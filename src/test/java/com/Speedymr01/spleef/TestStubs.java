package com.Speedymr01.spleef;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Utility to create lightweight test stubs for Bukkit interfaces using
 * java.lang.reflect.Proxy, avoiding the need for Mockito/ByteBuddy on JDK 25.
 */
public final class TestStubs {

    private TestStubs() {}

    // ── World ──────────────────────────────────────────────────────────

    /**
     * Creates a World stub. Each call to getBlockAt(Location) returns a new
     * block stub with the given default material.
     */
    public static World world(String name) {
        return world(name, Material.AIR);
    }

    /**
     * Creates a World stub whose blocks start with the given default material.
     */
    public static World world(String name, Material defaultBlockMaterial) {
        World proxy = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                new WorldHandler(name, defaultBlockMaterial)
        );
        return proxy;
    }

    /** Handler for World proxy — tracks all blocks created via getBlockAt(). */
    private static class WorldHandler implements java.lang.reflect.InvocationHandler {
        final String name;
        final Material defaultMaterial;
        final List<Block> blocksCreated = new ArrayList<>();

        WorldHandler(String name, Material defaultMaterial) {
            this.name = name;
            this.defaultMaterial = defaultMaterial;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getName":        return name;
                case "equals":         return proxy == args[0];
                case "hashCode":       return System.identityHashCode(proxy);
                case "toString":       return "World{" + name + "}";
                case "getBlockAt": {
                    Block b = blockStub(defaultMaterial);
                    blocksCreated.add(b);
                    return b;
                }
                default:
                    return defaultReturn(method);
            }
        }
    }

    /**
     * Returns all block stubs that were created by this world's getBlockAt().
     */
    public static List<Block> worldBlocks(World world) {
        java.lang.reflect.InvocationHandler h = Proxy.getInvocationHandler(world);
        if (h instanceof WorldHandler wh) {
            return List.copyOf(wh.blocksCreated);
        }
        return List.of();
    }

    /**
     * Counts how many times a given method was called across all blocks
     * created by this world.
     */
    public static long countWorldBlockCalls(World world, String methodName) {
        return worldBlocks(world).stream()
                .flatMap(b -> blockCalls(b).stream())
                .filter(methodName::equals)
                .count();
    }

    // ── Block ──────────────────────────────────────────────────────────

    /** Returns the recorded call list for a block stub. */
    public static List<String> blockCalls(Block block) {
        return _blockCalls.getOrDefault(block, List.of());
    }

    /** Returns the current Material type of a block stub. */
    public static Material blockType(Block block) {
        return _blockTypes.getOrDefault(block, Material.AIR);
    }

    public static Block blockStub() {
        return blockStub(Material.AIR);
    }

    public static Block blockStub(Material initialType) {
        List<String> calls = Collections.synchronizedList(new ArrayList<>());
        Block proxy = (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxyObj, method, args) -> {
                    calls.add(method.getName());
                    switch (method.getName()) {
                        case "getType":
                            return _blockTypes.getOrDefault(proxyObj, initialType);
                        case "setType":
                            _blockTypes.put(proxyObj, (Material) args[0]);
                            return null;
                        case "getState": {
                            BlockStateStub bss = _blockStates.computeIfAbsent(proxyObj,
                                    k -> new BlockStateStub());
                            return bss.proxy();
                        }
                        case "getLocation":
                            return _blockLocations.get(proxyObj);
                        case "equals":
                            return proxyObj == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxyObj);
                        case "toString":
                            return "BlockStub{" + _blockTypes.get(proxyObj) + "}";
                        default:
                            return defaultReturn(method);
                    }
                }
        );
        _blockCalls.put(proxy, calls);
        return proxy;
    }

    public static void bindLocation(Block block, Location loc) {
        _blockLocations.put(block, loc);
    }

    private static final Map<Object, List<String>> _blockCalls = new IdentityHashMap<>();
    private static final Map<Object, Material> _blockTypes = new IdentityHashMap<>();
    private static final Map<Object, Location> _blockLocations = new IdentityHashMap<>();
    private static final Map<Object, BlockStateStub> _blockStates = new IdentityHashMap<>();

    // ── BlockState ─────────────────────────────────────────────────────

    private static class BlockStateStub {
        private final List<String> calls = new ArrayList<>();

        BlockState proxy() {
            return (BlockState) Proxy.newProxyInstance(
                    BlockState.class.getClassLoader(),
                    new Class<?>[]{BlockState.class},
                    (proxy, method, args) -> {
                        calls.add(method.getName());
                        switch (method.getName()) {
                            case "update":
                                return true;
                            case "equals":
                                return proxy == args[0];
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            default:
                                return defaultReturn(method);
                        }
                    }
            );
        }
    }

    // ── Player ─────────────────────────────────────────────────────────

    public static Player player(String name, UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName":        return name;
                        case "getUniqueId":    return uuid;
                        case "equals":         return proxy == args[0];
                        case "hashCode":       return System.identityHashCode(proxy);
                        case "toString":       return "Player{" + name + "}";
                        default:               return defaultReturn(method);
                    }
                }
        );
    }

    public static Player player(String name) {
        return player(name, UUID.randomUUID());
    }

    // ── helpers ────────────────────────────────────────────────────────

    static Object defaultReturn(Method method) {
        Class<?> ret = method.getReturnType();
        if (!ret.isPrimitive()) return null;
        if (ret == boolean.class) return false;
        if (ret == int.class)     return 0;
        if (ret == long.class)    return 0L;
        if (ret == double.class)  return 0.0;
        if (ret == float.class)   return 0.0f;
        if (ret == short.class)   return (short) 0;
        if (ret == byte.class)    return (byte) 0;
        if (ret == char.class)    return (char) 0;
        return null;
    }
}

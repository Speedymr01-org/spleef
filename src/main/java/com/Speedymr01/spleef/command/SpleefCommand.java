package com.Speedymr01.spleef.command;

import com.Speedymr01.spleef.SpleefPlugin;
import com.Speedymr01.spleef.arena.Arena;
import com.Speedymr01.spleef.game.GameManager;
import com.Speedymr01.spleef.game.SpleefGame;
import com.Speedymr01.spleef.game.SpleefGame.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpleefCommand implements TabExecutor {

    private final SpleefPlugin plugin;
    private final GameManager gameManager;

    private static final List<String> SUB_COMMANDS = Arrays.asList("join", "leave", "start", "stop", "arena", "list", "info");

    public SpleefCommand(SpleefPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /spleef <join|leave|start|stop|arena|list|info>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                return handleJoin(sender, args);
            case "leave":
                return handleLeave(sender);
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "arena":
                return handleArena(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
                return true;
        }
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can join games!", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (gameManager.isPlayerInGame(player)) {
            player.sendMessage(Component.text("You are already in a game!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            // Find first available game or arena
            if (gameManager.getActiveGames().isEmpty()) {
                // Try to auto-start a game on the first arena
                if (gameManager.getArenas().isEmpty()) {
                    player.sendMessage(Component.text("No arenas available! An admin must set one up.", NamedTextColor.RED));
                    return true;
                }
                Arena arena = gameManager.getArenas().iterator().next();
                SpleefGame game = gameManager.createGame(arena);
                game.addPlayer(player);
                player.sendMessage(Component.text("Created new game on arena: " + arena.getName(), NamedTextColor.GREEN));
            } else {
                // Join the first available game
                SpleefGame game = gameManager.getActiveGames().iterator().next();
                game.addPlayer(player);
            }
            return true;
        }

        // Join specific arena
        String arenaName = args[1];
        gameManager.getArena(arenaName).ifPresentOrElse(arena -> {
            SpleefGame game = gameManager.getGame(arenaName).orElseGet(() -> gameManager.createGame(arena));
            game.addPlayer(player);
        }, () -> {
            player.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
        });

        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        gameManager.getPlayerGame(player).ifPresentOrElse(
                game -> game.removePlayer(player),
                () -> player.sendMessage(Component.text("You are not in a game!", NamedTextColor.RED))
        );
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spleef.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            // Start the first available game
            gameManager.getActiveGames().stream().findFirst().ifPresentOrElse(
                    SpleefGame::start,
                    () -> sender.sendMessage(Component.text("No active games to start!", NamedTextColor.RED))
            );
            return true;
        }

        String arenaName = args[1];
        gameManager.getGame(arenaName).ifPresentOrElse(
                SpleefGame::start,
                () -> sender.sendMessage(Component.text("No active game on arena '" + arenaName + "'!", NamedTextColor.RED))
        );
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spleef.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            gameManager.getActiveGames().forEach(SpleefGame::stop);
            sender.sendMessage(Component.text("All games stopped.", NamedTextColor.GREEN));
            return true;
        }

        String arenaName = args[1];
        gameManager.getGame(arenaName).ifPresentOrElse(
                SpleefGame::stop,
                () -> sender.sendMessage(Component.text("No active game on arena '" + arenaName + "'!", NamedTextColor.RED))
        );
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spleef.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /spleef arena <create|pos1|pos2|addspawn|type|players-needed> <name>", NamedTextColor.RED));
            return true;
        }

        String action = args[1];
        String arenaName = args.length > 2 ? args[2] : null;
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (action.equalsIgnoreCase("create")) {
            if (arenaName == null) {
                sender.sendMessage(Component.text("Usage: /spleef arena create <name>", NamedTextColor.RED));
                return true;
            }
            if (gameManager.createPendingArena(arenaName)) {
                sender.sendMessage(Component.text("Arena '" + arenaName + "' created!", NamedTextColor.GREEN));
                sender.sendMessage(Component.text("Now set its bounds: /spleef arena pos1 " + arenaName, NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Then: /spleef arena pos2 " + arenaName, NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("Arena '" + arenaName + "' already exists!", NamedTextColor.RED));
            }
            return true;
        }

        // All other actions (pos1, pos2, addspawn, players-needed) need a name
        if (arenaName == null) {
            sender.sendMessage(Component.text("Usage: /spleef arena " + action + " <name> [number]", NamedTextColor.RED));
            return true;
        }

        if (!gameManager.isArenaRegistered(arenaName)) {
            sender.sendMessage(Component.text("Arena '" + arenaName + "' not found! Use /spleef arena create " + arenaName + " first.", NamedTextColor.RED));
            return true;
        }

        // type can be set from console
        if (action.equalsIgnoreCase("type")) {
            if (args.length < 4) {
                sender.sendMessage(Component.text("Usage: /spleef arena type <name> <ffa|solos|duos|trios|quads>", NamedTextColor.RED));
                return true;
            }
            String type = args[3].toLowerCase();
            gameManager.getArena(arenaName).ifPresentOrElse(arena -> {
                if (arena.setGameType(type)) {
                    gameManager.saveArena(arena);
                    sender.sendMessage(Component.text("Arena '" + arenaName + "' set to " + type + " (" + arena.getMinPlayers() + " players needed).", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Invalid type! Valid types: ffa, solos, duos, trios, quads", NamedTextColor.RED));
                }
            }, () -> {
                sender.sendMessage(Component.text("Arena '" + arenaName + "' not fully set up yet! Set its bounds first.", NamedTextColor.RED));
            });
            return true;
        }

        // players-needed can be used from console
        if (action.equalsIgnoreCase("players-needed")) {
            if (args.length < 4) {
                sender.sendMessage(Component.text("Usage: /spleef arena players-needed <name> <number>", NamedTextColor.RED));
                return true;
            }
            gameManager.getArena(arenaName).ifPresentOrElse(arena -> {
                try {
                    int needed = Integer.parseInt(args[3]);
                    if (needed < 2) {
                        sender.sendMessage(Component.text("Minimum players needed is 2!", NamedTextColor.RED));
                        return;
                    }
                    int oldNeeded = arena.getMinPlayers();
                    arena.setMinPlayers(needed);
                    gameManager.saveArena(arena);
                    sender.sendMessage(Component.text("Players needed for '" + arenaName + "' changed from " + oldNeeded + " to " + needed + ".", NamedTextColor.GREEN));

                    // Warn if count doesn't match the game type
                    String type = arena.getGameType();
                    Integer typeMin = java.util.Map.of("ffa", 2, "solos", 2, "duos", 4, "trios", 6, "quads", 8).get(type);
                    if (typeMin != null && needed != typeMin) {
                        sender.sendMessage(Component.text("Warning: " + needed + " doesn't match " + type + " type (" + typeMin + ").", NamedTextColor.YELLOW));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number: " + args[3] + ". Use a positive number (minimum 2).", NamedTextColor.RED));
                }
            }, () -> {
                sender.sendMessage(Component.text("Arena '" + arenaName + "' not fully set up yet! Set its bounds first.", NamedTextColor.RED));
            });
            return true;
        }

        if (player == null) {
            sender.sendMessage(Component.text("Must be a player to use this command!", NamedTextColor.RED));
            return true;
        }

        if (action.equalsIgnoreCase("pos1")) {
            plugin.getConfig().set("selection." + arenaName + ".pos1", locationToString(player.getLocation()));
            plugin.saveConfig();
            player.sendMessage(Component.text("Position 1 set for arena '" + arenaName + "'.", NamedTextColor.GREEN));
            return true;
        }

        if (action.equalsIgnoreCase("pos2")) {
            plugin.getConfig().set("selection." + arenaName + ".pos2", locationToString(player.getLocation()));
            plugin.saveConfig();
            player.sendMessage(Component.text("Position 2 set for arena '" + arenaName + "'.", NamedTextColor.GREEN));

            if (plugin.getConfig().contains("selection." + arenaName + ".pos1")) {
                Location pos1 = stringToLocation(plugin.getConfig().getString("selection." + arenaName + ".pos1"));
                Location pos2 = player.getLocation();
                createArenaFromSelection(arenaName, pos1, pos2, sender);
                plugin.getConfig().set("selection." + arenaName, null);
                plugin.saveConfig();
            }
            return true;
        }

        if (action.equalsIgnoreCase("addspawn")) {
            gameManager.getArena(arenaName).ifPresentOrElse(arena -> {
                arena.addSpawnLocation(player.getLocation());
                gameManager.saveArena(arena);
                int count = arena.getSpawnLocations().size();
                int needed = arena.getMinPlayers();
                player.sendMessage(Component.text("Spawn location (" + count + "/" + needed + ") added to arena '" + arenaName + "'.", NamedTextColor.GREEN));
            }, () -> {
                sender.sendMessage(Component.text("Arena '" + arenaName + "' not fully set up yet! Set its bounds first.", NamedTextColor.RED));
            });
            return true;
        }

        sender.sendMessage(Component.text("Unknown arena action: " + action, NamedTextColor.RED));
        return true;
    }

    private void createArenaFromSelection(String name, Location pos1, Location pos2, CommandSender sender) {
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        Arena arena = new Arena(name, world, minX, minY, minZ, maxX, maxY, maxZ);
        gameManager.saveArena(arena);
        gameManager.removePending(name);
        sender.sendMessage(Component.text("Arena '" + name + "' bounds set!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Use /spleef arena addspawn " + name + " to add spawn points.", NamedTextColor.YELLOW));
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    @SuppressWarnings("deprecation")
    private Location stringToLocation(String str) {
        String[] parts = str.split(",");
        return new Location(
                plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
        );
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("=== Arenas ===", NamedTextColor.GOLD));

        boolean anyFound = false;

        // Pending arenas (bounds not set yet) - blue
        for (String name : gameManager.getPendingArenas()) {
            anyFound = true;
            sender.sendMessage(Component.text("- " + name + " [BOUNDS NOT SET]", NamedTextColor.BLUE));
        }

        // Fully created arenas
        for (Arena arena : gameManager.getArenas()) {
            anyFound = true;
            java.util.Optional<SpleefGame> optGame = gameManager.getGame(arena.getName());
            if (optGame.isPresent()) {
                SpleefGame game = optGame.get();
                GameState gs = game.getState();
                if (gs == GameState.ACTIVE) {
                    // Match in progress - red
                    sender.sendMessage(Component.text("- " + arena.getName() + " [MATCH IN PROGRESS]", NamedTextColor.RED)
                            .append(Component.text(" (" + game.getAlivePlayers().size() + "/" + game.getStartedPlayerCount() + " left)", NamedTextColor.GRAY))
                            .append(Component.text(" [" + arena.getGameType().toUpperCase() + "]", NamedTextColor.GRAY)));
                } else {
                    // Waiting for players - yellow
                    int needed = arena.getMinPlayers();
                    sender.sendMessage(Component.text("- " + arena.getName() + " [" + game.getPlayers().size() + "/" + needed + "]", NamedTextColor.YELLOW)
                            .append(Component.text(" [" + arena.getGameType().toUpperCase() + "]", NamedTextColor.GRAY)));
                }
            } else {
                sender.sendMessage(Component.text("- " + arena.getName() + " [READY]", NamedTextColor.GREEN)
                        .append(Component.text(" [" + arena.getGameType().toUpperCase() + "]", NamedTextColor.GRAY)));
            }
        }

        if (!anyFound) {
            sender.sendMessage(Component.text("No arenas configured.", NamedTextColor.GRAY));
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        String arenaName = args.length > 1 ? args[1] : null;

        if (arenaName == null) {
            sender.sendMessage(Component.text("Usage: /spleef info <arena>", NamedTextColor.RED));
            return true;
        }

        gameManager.getArena(arenaName).ifPresentOrElse(arena -> {
            sender.sendMessage(Component.text("=== Arena: " + arena.getName() + " ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("World: " + arena.getWorld().getName(), NamedTextColor.WHITE));
            sender.sendMessage(Component.text("Bounds: " + arena.getMinX() + "," + arena.getMinY() + "," + arena.getMinZ()
                    + " -> " + arena.getMaxX() + "," + arena.getMaxY() + "," + arena.getMaxZ(), NamedTextColor.WHITE));
            sender.sendMessage(Component.text("Spawn points: " + arena.getSpawnLocations().size(), NamedTextColor.WHITE));
        }, () -> {
            sender.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("arena")) {
                return Arrays.asList("create", "pos1", "pos2", "addspawn", "type", "players-needed").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info")) {
                return gameManager.getArenas().stream()
                        .map(Arena::getName)
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
                return gameManager.getActiveGames().stream()
                        .map(g -> g.getArena().getName())
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("arena")) {
            if (args[1].equalsIgnoreCase("create")) {
                return new ArrayList<>();
            }
            // Suggest arena names for pos1/pos2/addspawn/type/players-needed
            return gameManager.getArenas().stream()
                    .map(Arena::getName)
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("type")) {
            return Arrays.asList("ffa", "solos", "duos", "trios", "quads").stream()
                    .filter(s -> s.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

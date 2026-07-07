package com.tdm.spleef.command;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.arena.Arena;
import com.tdm.spleef.game.GameManager;
import com.tdm.spleef.game.SpleefGame;
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

    private static final List<String> SUB_COMMANDS = Arrays.asList("join", "leave", "start", "stop", "arena-create", "setarena", "list", "info");

    public SpleefCommand(SpleefPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /spleef <join|leave|start|stop|arena-create|setarena|list|info>", NamedTextColor.RED));
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
            case "arena-create":
                return handleArenaCreate(sender, args);
            case "setarena":
                return handleSetArena(sender, args);
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

    private boolean handleArenaCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spleef.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /spleef arena-create <name>", NamedTextColor.RED));
            return true;
        }

        String arenaName = args[1];
        if (gameManager.createPendingArena(arenaName)) {
            sender.sendMessage(Component.text("Arena '" + arenaName + "' created!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Now set its bounds: /spleef setarena " + arenaName + " pos1", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Then: /spleef setarena " + arenaName + " pos2", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Arena '" + arenaName + "' already exists!", NamedTextColor.RED));
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleSetArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spleef.admin")) {
            sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /spleef setarena <name> [pos1|pos2|addspawn]", NamedTextColor.RED));
            return true;
        }

        String arenaName = args[1];
        Player player = (sender instanceof Player) ? (Player) sender : null;

        // Must create the arena with arena-create first
        if (!gameManager.isArenaRegistered(arenaName)) {
            sender.sendMessage(Component.text("Arena '" + arenaName + "' not found! Use /spleef arena-create " + arenaName + " first.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 2) {
            sender.sendMessage(Component.text("Usage: /spleef setarena " + arenaName + " <pos1|pos2|addspawn>", NamedTextColor.RED));
            return true;
        }

        if (args[2].equalsIgnoreCase("pos1")) {
            if (player == null) { sender.sendMessage(Component.text("Must be a player!", NamedTextColor.RED)); return true; }
            plugin.getConfig().set("selection." + arenaName + ".pos1", locationToString(player.getLocation()));
            plugin.saveConfig();
            player.sendMessage(Component.text("Position 1 set for arena '" + arenaName + "'.", NamedTextColor.GREEN));
            return true;
        }

        if (args[2].equalsIgnoreCase("pos2")) {
            if (player == null) { sender.sendMessage(Component.text("Must be a player!", NamedTextColor.RED)); return true; }
            plugin.getConfig().set("selection." + arenaName + ".pos2", locationToString(player.getLocation()));
            plugin.saveConfig();
            player.sendMessage(Component.text("Position 2 set for arena '" + arenaName + "'.", NamedTextColor.GREEN));

            // Check if both positions are set, if so create the arena
            if (plugin.getConfig().contains("selection." + arenaName + ".pos1")) {
                Location pos1 = stringToLocation(plugin.getConfig().getString("selection." + arenaName + ".pos1"));
                Location pos2 = player.getLocation();
                createArenaFromSelection(arenaName, pos1, pos2, sender);
                plugin.getConfig().set("selection." + arenaName, null);
                plugin.saveConfig();
            }
            return true;
        }

        if (args[2].equalsIgnoreCase("addspawn")) {
            if (player == null) { sender.sendMessage(Component.text("Must be a player!", NamedTextColor.RED)); return true; }
            gameManager.getArena(arenaName).ifPresentOrElse(arena -> {
                arena.addSpawnLocation(player.getLocation());
                gameManager.saveArena(arena);
                player.sendMessage(Component.text("Spawn location added to arena '" + arenaName + "'.", NamedTextColor.GREEN));
            }, () -> {
                sender.sendMessage(Component.text("Arena '" + arenaName + "' not found!", NamedTextColor.RED));
            });
            return true;
        }

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
        sender.sendMessage(Component.text("Use /spleef setarena " + name + " addspawn to add spawn points.", NamedTextColor.YELLOW));
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
        if (gameManager.getArenas().isEmpty()) {
            sender.sendMessage(Component.text("No arenas configured.", NamedTextColor.GRAY));
        } else {
            for (Arena arena : gameManager.getArenas()) {
                boolean hasActive = gameManager.getGame(arena.getName()).isPresent();
                Component status = hasActive
                        ? Component.text(" [ACTIVE]", NamedTextColor.GREEN)
                        : Component.text(" [INACTIVE]", NamedTextColor.GRAY);
                sender.sendMessage(Component.text("- " + arena.getName(), NamedTextColor.WHITE).append(status));
            }
        }

        sender.sendMessage(Component.text("=== Active Games ===", NamedTextColor.GOLD));
        if (gameManager.getActiveGames().isEmpty()) {
            sender.sendMessage(Component.text("No active games.", NamedTextColor.GRAY));
        } else {
            for (SpleefGame game : gameManager.getActiveGames()) {
                sender.sendMessage(Component.text("- " + game.getArena().getName()
                        + " (" + game.getAlivePlayers().size() + "/" + game.getPlayers().size() + " players)",
                        NamedTextColor.WHITE));
            }
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
            if (args[0].equalsIgnoreCase("arena-create")) {
                return new ArrayList<>();
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
            if (args[0].equalsIgnoreCase("setarena")) {
                return Arrays.asList("pos1", "pos2", "addspawn").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}

package com.tdm.spleef.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a team in a Spleef game.
 */
public class GameTeam {

    private static final NamedTextColor[] TEAM_COLORS = {
            NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.GREEN,
            NamedTextColor.YELLOW, NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_AQUA,
            NamedTextColor.GOLD, NamedTextColor.DARK_GREEN
    };

    private static final String[] COLOR_NAMES = {
            "Red", "Blue", "Green", "Yellow", "Purple", "Cyan", "Gold", "Dark Green"
    };

    private static int nextId = 0;

    private final String name;
    private final NamedTextColor color;
    private final List<Player> members;
    private final List<Player> aliveMembers;

    public GameTeam(int index) {
        this.color = TEAM_COLORS[index % TEAM_COLORS.length];
        this.name = COLOR_NAMES[index % COLOR_NAMES.length];
        this.members = new ArrayList<>();
        this.aliveMembers = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public List<Player> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Player> getAliveMembers() {
        return Collections.unmodifiableList(aliveMembers);
    }

    public void addMember(Player player) {
        members.add(player);
        aliveMembers.add(player);
    }

    public void removeMember(Player player) {
        members.remove(player);
        aliveMembers.remove(player);
    }

    public boolean eliminateMember(Player player) {
        aliveMembers.remove(player);
        return aliveMembers.isEmpty(); // true = entire team eliminated
    }

    public boolean isAlive() {
        return !aliveMembers.isEmpty();
    }

    public int getAliveCount() {
        return aliveMembers.size();
    }

    /**
     * Returns the team name colored.
     */
    public Component getDisplayName() {
        return Component.text(name, color);
    }

    /**
     * Returns a player's name with their team color prefix.
     */
    public Component getPlayerDisplayName(Player player) {
        return Component.text(player.getName(), color);
    }

    /**
     * Build teams from a list of players based on team size.
     */
    public static List<GameTeam> createTeams(List<Player> players, int teamSize) {
        List<GameTeam> teams = new ArrayList<>();
        int teamIndex = 0;

        for (int i = 0; i < players.size(); i += teamSize) {
            GameTeam team = new GameTeam(teamIndex++);
            for (int j = i; j < i + teamSize && j < players.size(); j++) {
                team.addMember(players.get(j));
            }
            teams.add(team);
        }

        nextId = teamIndex;
        return teams;
    }

    /**
     * Gets the team a player belongs to.
     */
    public static GameTeam getPlayerTeam(List<GameTeam> teams, Player player) {
        for (GameTeam team : teams) {
            if (team.members.contains(player)) return team;
        }
        return null;
    }
}

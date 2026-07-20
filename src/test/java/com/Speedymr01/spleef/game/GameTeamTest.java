package com.Speedymr01.spleef.game;

import static org.junit.jupiter.api.Assertions.*;
import static com.Speedymr01.spleef.TestStubs.player;

import com.Speedymr01.spleef.TestStubs;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class GameTeamTest {

    private Player playerA;
    private Player playerB;
    private Player playerC;
    private Player playerD;
    private List<Player> players;

    @BeforeEach
    void setUp() {
        playerA = player("PlayerA");
        playerB = player("PlayerB");
        playerC = player("PlayerC");
        playerD = player("PlayerD");
        players = List.of(playerA, playerB, playerC, playerD);
    }

    @Test
    void createTeams_ffaSize0_returnsEmpty() {
        List<GameTeam> teams = GameTeam.createTeams(players, 0);
        assertTrue(teams.isEmpty(), "FFA/solos should produce no teams");
    }

    @Test
    void createTeams_duos_createsCorrectNumberOfTeams() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        assertEquals(2, teams.size(), "4 players in duos should create 2 teams");
    }

    @Test
    void createTeams_duos_assignsPlayersInOrder() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        assertEquals(2, teams.get(0).getMembers().size(), "First team should have 2 members");
        assertEquals(2, teams.get(1).getMembers().size(), "Second team should have 2 members");
        assertTrue(teams.get(0).getMembers().contains(playerA), "Team 0 should contain playerA");
        assertTrue(teams.get(0).getMembers().contains(playerB), "Team 0 should contain playerB");
        assertTrue(teams.get(1).getMembers().contains(playerC), "Team 1 should contain playerC");
        assertTrue(teams.get(1).getMembers().contains(playerD), "Team 1 should contain playerD");
    }

    @Test
    void createTeams_unevenPlayers_createsPartialTeam() {
        List<GameTeam> teams = GameTeam.createTeams(List.of(playerA, playerB, playerC, playerD), 3);
        assertEquals(2, teams.size(), "4 players in trios should create 2 teams (1 partial)");
        assertEquals(3, teams.get(0).getMembers().size());
        assertEquals(1, teams.get(1).getMembers().size());
    }

    @Test
    void getPlayerTeam_returnsCorrectTeam() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        assertEquals(teams.get(0), GameTeam.getPlayerTeam(teams, playerA));
        assertEquals(teams.get(0), GameTeam.getPlayerTeam(teams, playerB));
        assertEquals(teams.get(1), GameTeam.getPlayerTeam(teams, playerC));
        assertEquals(teams.get(1), GameTeam.getPlayerTeam(teams, playerD));
    }

    @Test
    void getPlayerTeam_unknownPlayer_returnsNull() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        Player unknown = TestStubs.player("Unknown");
        assertNull(GameTeam.getPlayerTeam(teams, unknown));
    }

    @Test
    void addMember_addsPlayerToTeam() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        GameTeam team = teams.get(0);
        assertEquals(2, team.getMembers().size());
        assertEquals(2, team.getAliveMembers().size());
    }

    @Test
    void removeMember_removesPlayerFromTeam() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        GameTeam team = teams.get(0);
        team.removeMember(playerA);
        assertFalse(team.getMembers().contains(playerA));
        assertFalse(team.getAliveMembers().contains(playerA));
        assertEquals(1, team.getMembers().size());
    }

    @Test
    void eliminateMember_marksPlayerDead() {
        List<GameTeam> teams = GameTeam.createTeams(players, 2);
        GameTeam team = teams.get(0);
        assertTrue(team.isAlive());
        team.eliminateMember(playerA);
        assertTrue(team.isAlive(), "Team with 1 alive should still be alive");
        assertEquals(1, team.getAliveCount());
    }

    @Test
    void eliminateMember_lastPlayer_returnsTrue() {
        GameTeam team = new GameTeam(0);
        team.addMember(playerA);
        boolean teamDead = team.eliminateMember(playerA);
        assertTrue(teamDead, "Last player eliminated should return true");
        assertFalse(team.isAlive());
        assertEquals(0, team.getAliveCount());
    }

    @Test
    void eliminateMember_notLastPlayer_returnsFalse() {
        GameTeam team = new GameTeam(0);
        team.addMember(playerA);
        team.addMember(playerB);
        boolean teamDead = team.eliminateMember(playerA);
        assertFalse(teamDead, "Team with remaining members should return false");
        assertTrue(team.isAlive());
    }

    @Test
    void isAlive_returnsCorrectState() {
        GameTeam team = new GameTeam(0);
        assertFalse(team.isAlive(), "Empty team should not be alive");
        team.addMember(playerA);
        assertTrue(team.isAlive(), "Team with members should be alive");
        team.eliminateMember(playerA);
        assertFalse(team.isAlive(), "Team with all dead should not be alive");
    }

    @Test
    void teamColors_areAssignedSequentially() {
        GameTeam team0 = new GameTeam(0);
        GameTeam team1 = new GameTeam(1);
        assertNotNull(team0.getColor());
        assertNotNull(team1.getColor());
        assertNotEquals(team0.getColor(), team1.getColor(), "Consecutive teams should have different colors");
    }

    @Test
    void getDisplayName_returnsColoredName() {
        GameTeam team = new GameTeam(0);
        assertNotNull(team.getDisplayName());
        assertTrue(team.getDisplayName().toString().contains(team.getName()));
    }

    @Test
    void getPlayerDisplayName_returnsColoredPlayerName() {
        GameTeam team = new GameTeam(0);
        team.addMember(playerA);
        assertNotNull(team.getPlayerDisplayName(playerA));
        assertTrue(team.getPlayerDisplayName(playerA).toString().contains("PlayerA"));
    }
}

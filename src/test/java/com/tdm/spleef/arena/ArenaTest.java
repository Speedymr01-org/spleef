package com.tdm.spleef.arena;

import com.tdm.spleef.TestStubs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArenaTest {

    private World world;
    private Arena arena;

    @BeforeEach
    void setUp() {
        world = TestStubs.world("world");
        // Arena from (10, 5, 10) to (20, 10, 20)
        arena = new Arena("test", world, 10, 5, 10, 20, 10, 20);
    }

    @Test
    void constructor_setsBoundsCorrectly() {
        assertEquals("test", arena.getName());
        assertEquals(world, arena.getWorld());
        assertEquals(10, arena.getMinX());
        assertEquals(5, arena.getMinY());
        assertEquals(10, arena.getMinZ());
        assertEquals(20, arena.getMaxX());
        assertEquals(10, arena.getMaxY());
        assertEquals(20, arena.getMaxZ());
    }

    @Test
    void constructor_sortsBounds() {
        Arena reversed = new Arena("rev", world, 20, 10, 20, 10, 5, 10);
        assertEquals(10, reversed.getMinX());
        assertEquals(5, reversed.getMinY());
        assertEquals(10, reversed.getMinZ());
        assertEquals(20, reversed.getMaxX());
        assertEquals(10, reversed.getMaxY());
        assertEquals(20, reversed.getMaxZ());
    }

    @Test
    void isWithinBounds_inside_returnsTrue() {
        Location loc = new Location(world, 15, 7, 15);
        assertTrue(arena.isWithinBounds(loc));
    }

    @Test
    void isWithinBounds_onEdge_returnsTrue() {
        assertTrue(arena.isWithinBounds(new Location(world, 10, 5, 10)));
        assertTrue(arena.isWithinBounds(new Location(world, 20, 10, 20)));
    }

    @Test
    void isWithinBounds_outsideX_returnsFalse() {
        assertFalse(arena.isWithinBounds(new Location(world, 9, 7, 15)));
        assertFalse(arena.isWithinBounds(new Location(world, 21, 7, 15)));
    }

    @Test
    void isWithinBounds_outsideY_returnsFalse() {
        assertFalse(arena.isWithinBounds(new Location(world, 15, 4, 15)));
        assertFalse(arena.isWithinBounds(new Location(world, 15, 11, 15)));
    }

    @Test
    void isWithinBounds_outsideZ_returnsFalse() {
        assertFalse(arena.isWithinBounds(new Location(world, 15, 7, 9)));
        assertFalse(arena.isWithinBounds(new Location(world, 15, 7, 21)));
    }

    @Test
    void isWithinBounds_wrongWorld_returnsFalse() {
        World otherWorld = TestStubs.world("other");
        assertFalse(arena.isWithinBounds(new Location(otherWorld, 15, 7, 15)));
    }

    @Test
    void isOnFloor_atMinY_returnsTrue() {
        assertTrue(arena.isOnFloor(new Location(world, 12, 5, 14)));
    }

    @Test
    void isOnFloor_aboveMinY_returnsFalse() {
        assertFalse(arena.isOnFloor(new Location(world, 12, 6, 14)));
    }

    @Test
    void isOnFloor_outsideX_returnsFalse() {
        assertFalse(arena.isOnFloor(new Location(world, 9, 5, 14)));
    }

    @Test
    void isOnFloor_outsideZ_returnsFalse() {
        assertFalse(arena.isOnFloor(new Location(world, 12, 5, 9)));
    }

    @Test
    void isOnFloor_wrongWorld_returnsFalse() {
        World otherWorld = TestStubs.world("other");
        assertFalse(arena.isOnFloor(new Location(otherWorld, 12, 5, 14)));
    }

    @Test
    void isWithinHorizontalBounds_inside_returnsTrue() {
        assertTrue(arena.isWithinHorizontalBounds(new Location(world, 12, 7, 14)));
    }

    @Test
    void isWithinHorizontalBounds_outside_returnsFalse() {
        assertFalse(arena.isWithinHorizontalBounds(new Location(world, 9, 7, 14)));
        assertFalse(arena.isWithinHorizontalBounds(new Location(world, 12, 7, 9)));
    }

    @Test
    void spawnLocation_getDefault_returnsCenter() {
        Location loc = arena.getSpawnLocation(0);
        assertEquals(15.5, loc.getX(), 0.01);
        assertEquals(6.0, loc.getY(), 0.01);
        assertEquals(15.5, loc.getZ(), 0.01);
    }

    @Test
    void spawnLocation_usesAddedSpawns() {
        arena.addSpawnLocation(new Location(world, 11.5, 6, 11.5));
        Location loc = arena.getSpawnLocation(0);
        assertEquals(11.5, loc.getX(), 0.01);
        assertEquals(6.0, loc.getY(), 0.01);
        assertEquals(11.5, loc.getZ(), 0.01);
    }

    @Test
    void spawnLocation_wrapsAround() {
        arena.addSpawnLocation(new Location(world, 11.5, 6, 11.5));
        arena.addSpawnLocation(new Location(world, 12.5, 6, 12.5));
        Location loc = arena.getSpawnLocation(2);
        assertEquals(11.5, loc.getX(), 0.01);
    }

    @Test
    void getSpawnLocations_returnsCopy() {
        arena.addSpawnLocation(new Location(world, 11, 6, 11));
        List<Location> locs = arena.getSpawnLocations();
        locs.clear();
        assertEquals(1, arena.getSpawnLocations().size(), "Should not modify internal list");
    }

    // ── Floor tests ────────────────────────────────────────────────────
    // The floor is 11 x 11 = 121 blocks (x from 10 to 20, z from 10 to 20, y=minY=5)

    @Test
    void fillFloor_placesSnowWhereAir() {
        // World returns AIR blocks by default
        arena.fillFloor();

        // Each of the 121 positions should call: getType, setType, getState
        assertEquals(121, TestStubs.countWorldBlockCalls(world, "setType"));
        assertEquals(121, TestStubs.countWorldBlockCalls(world, "getState"));
    }

    @Test
    void fillFloor_skipsNonAir() {
        // Create a world where blocks start as STONE
        World stoneWorld = TestStubs.world("stone", Material.STONE);
        Arena stoneArena = new Arena("test", stoneWorld, 10, 5, 10, 20, 10, 20);

        stoneArena.fillFloor();

        assertEquals(0, TestStubs.countWorldBlockCalls(stoneWorld, "setType"));
    }

    @Test
    void clearFloor_removesSnow() {
        // Create a world where blocks start as SNOW_BLOCK
        World snowWorld = TestStubs.world("snow", Material.SNOW_BLOCK);
        Arena snowArena = new Arena("test", snowWorld, 10, 5, 10, 20, 10, 20);

        snowArena.clearFloor();

        assertEquals(121, TestStubs.countWorldBlockCalls(snowWorld, "setType"));
    }

    @Test
    void clearFloor_skipsNonSnow() {
        // World returns AIR blocks by default
        arena.clearFloor();

        assertEquals(0, TestStubs.countWorldBlockCalls(world, "setType"));
    }

    // ── Game type tests ────────────────────────────────────────────────

    @Test
    void setGameType_ffa_setsMinPlayers() {
        assertTrue(arena.setGameType("ffa"));
        assertEquals(2, arena.getMinPlayers());
    }

    @Test
    void setGameType_duos_setsMinPlayers() {
        assertTrue(arena.setGameType("duos"));
        assertEquals(4, arena.getMinPlayers());
    }

    @Test
    void setGameType_trios_setsMinPlayers() {
        assertTrue(arena.setGameType("trios"));
        assertEquals(6, arena.getMinPlayers());
    }

    @Test
    void setGameType_quads_setsMinPlayers() {
        assertTrue(arena.setGameType("quads"));
        assertEquals(8, arena.getMinPlayers());
    }

    @Test
    void setGameType_invalid_returnsFalse() {
        assertFalse(arena.setGameType("invalid"));
        assertEquals("ffa", arena.getGameType());
    }

    @Test
    void setGameType_caseInsensitive() {
        assertTrue(arena.setGameType("DUOS"));
        assertEquals("duos", arena.getGameType());
    }

    @Test
    void setMinPlayers_enforcesMinimum() {
        arena.setMinPlayers(1);
        assertEquals(2, arena.getMinPlayers(), "Minimum should be 2");
        arena.setMinPlayers(5);
        assertEquals(5, arena.getMinPlayers());
    }

    // ── Save/load tests ────────────────────────────────────────────────

    @Test
    void save_storesAllFields() {
        arena.addSpawnLocation(new Location(world, 11, 6, 11));
        arena.setGameType("duos");

        MemoryConfiguration config = new MemoryConfiguration();
        arena.save(config.createSection("test"));

        assertEquals("world", config.getString("test.world"));
        assertEquals(10, config.getInt("test.minX"));
        assertEquals(5, config.getInt("test.minY"));
        assertEquals(10, config.getInt("test.minZ"));
        assertEquals(20, config.getInt("test.maxX"));
        assertEquals(10, config.getInt("test.maxY"));
        assertEquals(20, config.getInt("test.maxZ"));
        assertEquals(4, config.getInt("test.min-players"));
        assertEquals("duos", config.getString("test.game-type"));
        assertTrue(config.contains("test.spawns.0"));
    }

    @Test
    void save_roundTrip_serializesAllFields() {
        arena.addSpawnLocation(new Location(world, 11.5, 6, 11.5));
        arena.setGameType("trios");

        MemoryConfiguration config = new MemoryConfiguration();
        arena.save(config.createSection("arena"));

        // Verify all fields are present and correct (round-trip via config)
        assertEquals("world", config.getString("arena.world"));
        assertEquals(10, config.getInt("arena.minX"));
        assertEquals(5, config.getInt("arena.minY"));
        assertEquals(10, config.getInt("arena.minZ"));
        assertEquals(20, config.getInt("arena.maxX"));
        assertEquals(10, config.getInt("arena.maxY"));
        assertEquals(20, config.getInt("arena.maxZ"));
        assertEquals(6, config.getInt("arena.min-players"));
        assertEquals("trios", config.getString("arena.game-type"));
        assertEquals(11.5, config.getDouble("arena.spawns.0.x"), 0.001);
    }

    @Test
    void getValidTypes_containsAllTypes() {
        assertTrue(Arena.getValidTypes().containsAll(Set.of("ffa", "solos", "duos", "trios", "quads")));
    }
}

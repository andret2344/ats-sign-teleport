/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignTeleportServiceTest {
	@Mock
	private SignTeleportPlugin plugin;
	@Mock
	private Server server;
	@Mock
	private World world;
	@Mock
	private Chunk chunk;
	@Mock
	private YamlConfiguration yamlConfiguration;

	private SignTeleportService service;

	private NamespacedKey keyWorld;
	private NamespacedKey keyX;
	private NamespacedKey keyY;
	private NamespacedKey keyZ;
	private NamespacedKey keyYaw;
	private NamespacedKey keyPitch;

	@BeforeEach
	void setUp() {
		when(plugin.getName()).thenReturn("atssignteleport");
		service = new SignTeleportService(plugin);
		keyWorld = new NamespacedKey(plugin, "world");
		keyX = new NamespacedKey(plugin, "x");
		keyY = new NamespacedKey(plugin, "y");
		keyZ = new NamespacedKey(plugin, "z");
		keyYaw = new NamespacedKey(plugin, "yaw");
		keyPitch = new NamespacedKey(plugin, "pitch");
	}

	// ── getWorldName / isTeleportSign ─────────────────────────────────────────

	@Test
	void getWorldNameReturnsNull() {
		// given
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(pdc.get(keyWorld, PersistentDataType.STRING)).thenReturn(null);

		// when / then
		assertThat(service.getWorldName(pdc)).isNull();
	}

	@Test
	void getWorldNameReturnsName() {
		// given
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(pdc.get(keyWorld, PersistentDataType.STRING)).thenReturn("theworld");

		// when / then
		assertThat(service.getWorldName(pdc)).isEqualTo("theworld");
	}

	@Test
	void isTeleportSignFalse() {
		// given
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(pdc.has(keyWorld)).thenReturn(false);

		// when / then
		assertThat(service.isTeleportSign(pdc)).isFalse();
	}

	@Test
	void isTeleportSignTrue() {
		// given
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(pdc.has(keyWorld)).thenReturn(true);

		// when / then
		assertThat(service.isTeleportSign(pdc)).isTrue();
	}

	// ── buildLocation ─────────────────────────────────────────────────────────

	@Test
	void buildLocation() {
		// given
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld("theworld")).thenReturn(world);
		when(pdc.getOrDefault(keyX, PersistentDataType.DOUBLE, 0.0)).thenReturn(1.4);
		when(pdc.getOrDefault(keyY, PersistentDataType.DOUBLE, 0.0)).thenReturn(1.5);
		when(pdc.getOrDefault(keyZ, PersistentDataType.DOUBLE, 0.0)).thenReturn(1.6);
		when(pdc.getOrDefault(keyYaw, PersistentDataType.FLOAT, 0.0f)).thenReturn(45.0f);
		when(pdc.getOrDefault(keyPitch, PersistentDataType.FLOAT, 0.0f)).thenReturn(30.0f);

		// when
		final Location location = service.buildLocation("theworld", pdc);

		// then
		assertThat(location).isEqualTo(new Location(world, 1.4, 1.5, 1.6, 45.0f, 30.0f));
	}

	// ── buildEditorLines ──────────────────────────────────────────────────────

	@Test
	void buildEditorLines() {
		// given
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(pdc.getOrDefault(keyX, PersistentDataType.DOUBLE, 0.0)).thenReturn(1.0);
		when(pdc.getOrDefault(keyY, PersistentDataType.DOUBLE, 0.0)).thenReturn(2.0);
		when(pdc.getOrDefault(keyZ, PersistentDataType.DOUBLE, 0.0)).thenReturn(3.0);
		when(pdc.getOrDefault(keyYaw, PersistentDataType.FLOAT, 0.0f)).thenReturn(90.0f);
		when(pdc.getOrDefault(keyPitch, PersistentDataType.FLOAT, 0.0f)).thenReturn(45.0f);

		// when
		final String[] lines = service.buildEditorLines(pdc, "theworld");

		// then
		assertThat(lines).containsExactly("[TELEPORT]", "[theworld]", "[1.0, 2.0, 3.0]", "[90.0, 45.0]");
	}

	// ── parseSignData ─────────────────────────────────────────────────────────

	@Test
	void parseSignDataEmpty() {
		// when / then
		assertThat(service.parseSignData(new String[]{"", "", "", ""})).isNull();
	}

	@Test
	void parseSignDataWithNoWorld() {
		// when / then
		assertThat(service.parseSignData(new String[]{"[TELEPORT]", "xyz", "[1,1,1]", ""})).isNull();
	}

	@Test
	void parseSignDataWithWrongWorld() {
		// given
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(null);

		// when / then
		assertThat(service.parseSignData(new String[]{"[TELEPORT]", "[xyz]", "[1,1,1]", ""})).isNull();
	}

	@Test
	void parseSignDataWithWrongCoords() {
		// given
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);

		// when / then
		assertThat(service.parseSignData(new String[]{"[TELEPORT]", "[xyz]", "[1]", ""})).isNull();
	}

	@Test
	void parseSignDataFromOfflinePlayer() {
		// given
		when(plugin.getServer()).thenReturn(server);
		when(server.getPlayerExact("somePlayer")).thenReturn(null);

		// when / then
		assertThat(service.parseSignData(new String[]{"[TELEPORT]", "@somePlayer", "", ""})).isNull();
	}

	@Test
	void parseSignDataFromPlayerInNullWorld() {
		// given
		final Player targetPlayer = mock(Player.class);
		when(plugin.getServer()).thenReturn(server);
		when(server.getPlayerExact("somePlayer")).thenReturn(targetPlayer);
		when(targetPlayer.getLocation()).thenReturn(new Location(null, 100.0, 64.0, 200.0));

		// when / then
		assertThat(service.parseSignData(new String[]{"[TELEPORT]", "@somePlayer", "", ""})).isNull();
	}

	@Test
	void parseSignDataFromOnlinePlayer() {
		// given
		final Player targetPlayer = mock(Player.class);
		when(plugin.getServer()).thenReturn(server);
		when(server.getPlayerExact("somePlayer")).thenReturn(targetPlayer);
		when(targetPlayer.getLocation()).thenReturn(new Location(world, 100.3, 64.7, 200.1, 91.3f, 45.7f));
		when(world.getName()).thenReturn("theworld");

		// when
		final String[] result = service.parseSignData(new String[]{"[TELEPORT]", "@somePlayer", "", ""});

		// then — coords rounded to nearest 0.5, yaw/pitch stored as-is
		assertThat(result).isNotNull();
		assertThat(result[0]).isEqualTo("theworld");
		assertThat(result[1]).isEqualTo("100.5");
		assertThat(result[2]).isEqualTo("64.5");
		assertThat(result[3]).isEqualTo("200.0");
		assertThat(result[4]).isEqualTo("91.3");
		assertThat(result[5]).isEqualTo("45.7");
	}

	@Test
	void parseSignDataWithCoords() {
		// given
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);

		// when
		final String[] result = service.parseSignData(new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""});

		// then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(6);
		assertThat(result[0]).isEqualTo("xyz");
		assertThat(result[1]).isEqualTo("1.4");
		assertThat(result[2]).isEqualTo("1.5");
		assertThat(result[3]).isEqualTo("1.6");
		assertThat(result[4]).isEqualTo("0.0");
		assertThat(result[5]).isEqualTo("0.0");
	}

	@Test
	void parseSignDataWithYawAndPitchInLine2() {
		// given
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);

		// when
		final String[] result = service.parseSignData(new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6, 90.0, 45.0]", ""});

		// then
		assertThat(result).isNotNull();
		assertThat(result[4]).isEqualTo("90.0");
		assertThat(result[5]).isEqualTo("45.0");
	}

	@Test
	void parseSignDataWithYawAndPitchInLine3() {
		// given
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);

		// when
		final String[] result = service.parseSignData(new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", "[90.0, 45.0]"});

		// then
		assertThat(result).isNotNull();
		assertThat(result[4]).isEqualTo("90.0");
		assertThat(result[5]).isEqualTo("45.0");
	}

	// ── applySignData ─────────────────────────────────────────────────────────

	@Test
	void applySignData() {
		// given
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines"))
				.thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));
		final String[] data = {"xyz", "1.4", "1.5", "1.6", "0.0", "0.0"};

		// when
		final SignChangeEvent event = new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""}, Side.FRONT);
		service.applySignData(event, pdc, data);

		// then
		verify(pdc, times(1)).set(keyWorld, PersistentDataType.STRING, "xyz");
		verify(pdc, times(1)).set(keyX, PersistentDataType.DOUBLE, 1.4);
		verify(pdc, times(1)).set(keyY, PersistentDataType.DOUBLE, 1.5);
		verify(pdc, times(1)).set(keyZ, PersistentDataType.DOUBLE, 1.6);
		verify(pdc, times(1)).set(keyYaw, PersistentDataType.FLOAT, 0.0f);
		verify(pdc, times(1)).set(keyPitch, PersistentDataType.FLOAT, 0.0f);
		assertThat(event.getLine(0)).isEqualTo("[TELEPORT]");
		assertThat(event.getLine(2)).isEqualTo("xyz");
		assertThat(event.getLine(3)).isEqualTo("1.4, 1.5, 1.6");
	}

	@Test
	void applySignDataWithYawAndPitch() {
		// given
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines"))
				.thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));
		final String[] data = {"xyz", "1.4", "1.5", "1.6", "90.0", "45.0"};

		// when
		final SignChangeEvent event = new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6, 90.0, 45.0]", ""}, Side.FRONT);
		service.applySignData(event, pdc, data);

		// then
		verify(pdc, times(1)).set(keyYaw, PersistentDataType.FLOAT, 90.0f);
		verify(pdc, times(1)).set(keyPitch, PersistentDataType.FLOAT, 45.0f);
	}

	@Test
	void applySignDataWithTooFewConfigLines() {
		// given
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines")).thenReturn(List.of("[TELEPORT]", ""));
		final String[] data = {"xyz", "1.4", "1.5", "1.6", "0.0", "0.0"};

		// when — should not throw even if config has fewer than 4 lines
		final SignChangeEvent event = new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""}, Side.FRONT);
		service.applySignData(event, pdc, data);

		// then — pdc.set still called, but setLine not called
		verify(pdc, times(1)).set(keyWorld, PersistentDataType.STRING, "xyz");
		// lines remain unchanged from event constructor
		assertThat(event.getLine(0)).isEqualTo("[TELEPORT]");
	}

	// ── updateChunk / updateSigns ─────────────────────────────────────────────

	@Test
	void updateSignsWithTooFewConfigLines() {
		// given
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines")).thenReturn(List.of("[TELEPORT]"));

		// when — should not touch any world
		service.updateSigns();

		// then
		verify(plugin, never()).getServer();
	}

	@Test
	void updateSignsSkipsNonSigns() {
		// given
		final TileState nonSign = mock(TileState.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines"))
				.thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorlds()).thenReturn(List.of(world));
		when(world.getLoadedChunks()).thenReturn(new Chunk[]{chunk});
		when(chunk.getTileEntities()).thenReturn(new BlockState[]{nonSign});

		// when
		service.updateSigns();

		// then
		verify(nonSign, never()).getPersistentDataContainer();
	}

	@Test
	void updateSignsSkipsNonTeleportSigns() {
		// given
		final Sign sign = mock(Sign.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines"))
				.thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorlds()).thenReturn(List.of(world));
		when(world.getLoadedChunks()).thenReturn(new Chunk[]{chunk});
		when(chunk.getTileEntities()).thenReturn(new BlockState[]{sign});
		when(sign.getPersistentDataContainer()).thenReturn(pdc);
		when(pdc.has(keyWorld)).thenReturn(false);

		// when
		service.updateSigns();

		// then
		verify(sign, never()).update();
	}

	@Test
	void updateSignsUpdatesTeleportSigns() {
		// given
		final Sign sign = mock(Sign.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		final SignSide signSide = mock(SignSide.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines"))
				.thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));
		when(plugin.getServer()).thenReturn(server);
		when(server.getWorlds()).thenReturn(List.of(world));
		when(world.getLoadedChunks()).thenReturn(new Chunk[]{chunk});
		when(chunk.getTileEntities()).thenReturn(new BlockState[]{sign});
		when(sign.getPersistentDataContainer()).thenReturn(pdc);
		when(pdc.has(keyWorld)).thenReturn(true);
		when(pdc.get(keyWorld, PersistentDataType.STRING)).thenReturn("theworld");
		when(pdc.getOrDefault(keyX, PersistentDataType.DOUBLE, 0.0)).thenReturn(1.0);
		when(pdc.getOrDefault(keyY, PersistentDataType.DOUBLE, 0.0)).thenReturn(2.0);
		when(pdc.getOrDefault(keyZ, PersistentDataType.DOUBLE, 0.0)).thenReturn(3.0);
		when(pdc.getOrDefault(keyYaw, PersistentDataType.FLOAT, 0.0f)).thenReturn(0.0f);
		when(pdc.getOrDefault(keyPitch, PersistentDataType.FLOAT, 0.0f)).thenReturn(0.0f);
		when(sign.getSide(Side.FRONT)).thenReturn(signSide);

		// when
		service.updateSigns();

		// then
		verify(signSide, times(1)).setLine(0, "[TELEPORT]");
		verify(signSide, times(1)).setLine(1, "");
		verify(signSide, times(1)).setLine(2, "theworld");
		verify(signSide, times(1)).setLine(3, "1.0, 2.0, 3.0");
		verify(sign, times(1)).update();
	}

	@Test
	void updateChunkWithTooFewConfigLines() {
		// given
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines")).thenReturn(List.of("[TELEPORT]"));

		// when — config has fewer than 4 lines, no tile entities should be accessed
		service.updateChunk(chunk);

		// then
		verify(chunk, never()).getTileEntities();
	}

	@Test
	void updateChunkSkipsSignWithNullWorldName() {
		// given
		final Sign sign = mock(Sign.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(plugin.getConfig()).thenReturn(yamlConfiguration);
		when(yamlConfiguration.getStringList("lines"))
				.thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));
		when(chunk.getTileEntities()).thenReturn(new BlockState[]{sign});
		when(sign.getPersistentDataContainer()).thenReturn(pdc);
		when(pdc.has(keyWorld)).thenReturn(true);
		when(pdc.get(keyWorld, PersistentDataType.STRING)).thenReturn(null);

		// when
		service.updateChunk(chunk);

		// then
		verify(sign, never()).update();
	}
}

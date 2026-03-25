/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignTeleportListenerTest {
	@Mock
	private SignTeleportPlugin plugin;
	@Mock
	private SignTeleportService service;
	@Mock
	private Block block;
	@Mock
	private Player player;
	@Mock
	private Server server;
	@Mock
	private BukkitScheduler scheduler;
	@Mock
	private Chunk chunk;

	private SignTeleportListener listener;

	@BeforeEach
	void setUp() {
		listener = new SignTeleportListener(plugin, service);
	}

	private PersistentDataContainer mockBlockPdc() {
		final TileState blockState = mock(TileState.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(block.getState()).thenReturn(blockState);
		when(blockState.getPersistentDataContainer()).thenReturn(pdc);
		return pdc;
	}

	// ── breakBlock ────────────────────────────────────────────────────────────

	@Test
	void breakBlockNotTileState() {
		// given
		when(block.getState()).thenReturn(mock(BlockState.class));

		// when
		final BlockBreakEvent event = new BlockBreakEvent(block, player);
		listener.breakBlock(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void breakNonTeleportBlock() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		// service.isTeleportSign returns false by default

		// when
		final BlockBreakEvent event = new BlockBreakEvent(block, player);
		listener.breakBlock(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void breakTeleportSignWithPermission() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		when(service.isTeleportSign(pdc)).thenReturn(true);
		when(player.hasPermission("ats.signteleport.break")).thenReturn(true);

		// when
		final BlockBreakEvent event = new BlockBreakEvent(block, player);
		listener.breakBlock(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void breakTeleportSignWithoutPermission() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		when(service.isTeleportSign(pdc)).thenReturn(true);
		when(player.hasPermission("ats.signteleport.break")).thenReturn(false);

		// when
		final BlockBreakEvent event = new BlockBreakEvent(block, player);
		listener.breakBlock(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	// ── createSign ────────────────────────────────────────────────────────────

	@Test
	void createSignWhenParseReturnsNull() {
		// given — service.parseSignData returns null by default

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"", "", "", ""}, Side.FRONT));

		// then
		verify(block, never()).getState();
	}

	@Test
	void createSignWithoutPermission() {
		// given
		when(service.parseSignData(any())).thenReturn(new String[]{"xyz", "1.4", "1.5", "1.6", "0.0", "0.0"});
		when(player.hasPermission("ats.signteleport.create")).thenReturn(false);

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""}, Side.FRONT));

		// then
		verify(block, never()).getState();
	}

	@Test
	void createSignBlockNotTileState() {
		// given
		when(service.parseSignData(any())).thenReturn(new String[]{"xyz", "1.4", "1.5", "1.6", "0.0", "0.0"});
		when(player.hasPermission("ats.signteleport.create")).thenReturn(true);
		when(block.getState()).thenReturn(mock(BlockState.class));

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""}, Side.FRONT));

		// then
		verify(service, never()).applySignData(any(), any(), any());
	}

	@Test
	void createSign() {
		// given
		final TileState tileState = mock(TileState.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		when(block.getState()).thenReturn(tileState);
		when(tileState.getPersistentDataContainer()).thenReturn(pdc);
		final String[] data = {"xyz", "1.4", "1.5", "1.6", "0.0", "0.0"};
		when(service.parseSignData(any())).thenReturn(data);
		when(player.hasPermission("ats.signteleport.create")).thenReturn(true);

		// when
		final SignChangeEvent event = new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""}, Side.FRONT);
		listener.createSign(event);

		// then
		verify(service).applySignData(event, pdc, data);
		verify(tileState).update();
	}

	// ── chunkLoad ─────────────────────────────────────────────────────────────

	@Test
	void chunkLoadDelegatesToService() {
		// when
		listener.chunkLoad(new ChunkLoadEvent(chunk, false));

		// then
		verify(service).updateChunk(chunk);
	}

	// ── clickSign ─────────────────────────────────────────────────────────────

	@Test
	void clickSignWrongAction() {
		// when
		listener.clickSign(new PlayerInteractEvent(player, Event.Result.DEFAULT.equals(Event.Result.DEFAULT) ? org.bukkit.event.block.Action.LEFT_CLICK_AIR : org.bukkit.event.block.Action.LEFT_CLICK_AIR, null, null, BlockFace.EAST));

		// then
		verify(player, never()).teleport(any(Location.class));
	}

	@Test
	void clickNull() {
		// when
		listener.clickSign(new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, null, BlockFace.EAST));

		// then
		verify(player, never()).teleport(any(Location.class));
	}

	@Test
	void clickSignBlockNotTileState() {
		// given
		when(block.getState()).thenReturn(mock(BlockState.class));

		// when
		listener.clickSign(new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST));

		// then
		verify(player, never()).teleport(any(Location.class));
	}

	@Test
	void clickSignWithNoMetadata() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		// service.getWorldName returns null by default

		// when
		listener.clickSign(new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST));

		// then
		verify(player, never()).teleport(any(Location.class));
	}

	@Test
	void clickSignWithoutPermission() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		when(service.getWorldName(pdc)).thenReturn("xyz");
		when(player.hasPermission("ats.signteleport.use")).thenReturn(false);

		// when
		listener.clickSign(new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST));

		// then
		verify(player, never()).teleport(any(Location.class));
	}

	@Test
	void clickSignSneakingWithoutCreatePermission() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		when(service.getWorldName(pdc)).thenReturn("xyz");
		when(player.isSneaking()).thenReturn(true);
		when(player.hasPermission("ats.signteleport.create")).thenReturn(false);

		// when
		final PlayerInteractEvent event = new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST);
		listener.clickSign(event);

		// then
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.DENY);
		assertThat(event.useItemInHand()).isEqualTo(Event.Result.DENY);
		verify(server, never()).getScheduler();
	}

	@Test
	void clickSignSneakingNotASign() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		// mockBlockPdc() returns TileState which is NOT a Sign
		when(service.getWorldName(pdc)).thenReturn("xyz");
		when(player.isSneaking()).thenReturn(true);
		when(player.hasPermission("ats.signteleport.create")).thenReturn(true);

		// when — state is TileState, not Sign
		final PlayerInteractEvent event = new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST);
		listener.clickSign(event);

		// then
		verify(player, never()).openSign(any(), any());
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.DENY);
		assertThat(event.useItemInHand()).isEqualTo(Event.Result.DENY);
	}

	@Test
	void clickSignSneakingOpensEditor() {
		// given
		final Sign sign = mock(Sign.class);
		final PersistentDataContainer pdc = mock(PersistentDataContainer.class);
		final Location signLocation = new Location(null, 0, 0, 0);
		when(block.getState()).thenReturn(sign);
		when(sign.getPersistentDataContainer()).thenReturn(pdc);
		when(service.getWorldName(pdc)).thenReturn("theworld");
		when(player.isSneaking()).thenReturn(true);
		when(player.hasPermission("ats.signteleport.create")).thenReturn(true);
		when(plugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);
		final String[] editorLines = {"[TELEPORT]", "[theworld]", "[1.0, 2.0, 3.0, 0.0, 0.0]", ""};
		when(service.buildEditorLines(pdc, "theworld")).thenReturn(editorLines);
		when(sign.getLocation()).thenReturn(signLocation);
		final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

		// when
		final PlayerInteractEvent event = new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST);
		listener.clickSign(event);
		verify(scheduler).runTask(eq(plugin), taskCaptor.capture());
		taskCaptor.getValue().run();

		// then
		verify(player).sendSignChange(signLocation, editorLines);
		verify(player).openSign(sign, Side.FRONT);
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.DENY);
		assertThat(event.useItemInHand()).isEqualTo(Event.Result.DENY);
	}

	@Test
	void clickSignWithMetadata() {
		// given
		final PersistentDataContainer pdc = mockBlockPdc();
		final Location location = new Location(null, 1.4, 1.5, 1.6, 45.0f, 30.0f);
		when(service.getWorldName(pdc)).thenReturn("xyz");
		when(service.buildLocation("xyz", pdc)).thenReturn(location);
		when(player.hasPermission("ats.signteleport.use")).thenReturn(true);

		// when
		final PlayerInteractEvent event = new PlayerInteractEvent(player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST);
		listener.clickSign(event);

		// then
		verify(player).teleport(location);
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.DENY);
		assertThat(event.useItemInHand()).isEqualTo(Event.Result.DENY);
	}
}

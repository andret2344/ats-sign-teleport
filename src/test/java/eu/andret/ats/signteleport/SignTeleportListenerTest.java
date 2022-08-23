/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignTeleportListenerTest {
	private static final JSONObject JSON_OBJECT = new JSONObject()
			.put("world", "xyz")
			.put("x", 1.4)
			.put("y", 1.5)
			.put("z", 1.6);

	@Test
	void breakEventTest() {
		// given
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);

		// when
		listener.breakBlock(new BlockBreakEvent(block, player));

		// then
		verify(block, times(1)).removeMetadata(SignTeleportListener.TELEPORT, plugin);
	}

	@Test
	void createSignEmpty() {
		// given
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"", "", "", ""}));

		// then
		verify(block, times(0)).setMetadata(anyString(), any(MetadataValue.class));
	}

	@Test
	void createSignWithWrongWorld() {
		// given
		final Server server = mock(Server.class);
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);

		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(null);

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1,1,1]", ""}));

		// then
		verify(block, times(0)).setMetadata(anyString(), any(MetadataValue.class));
	}

	@Test
	void createSignWithNoWorld() {
		// given
		final Server server = mock(Server.class);
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);

		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(null);

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "xyz", "[1,1,1]", ""}));

		// then
		verify(block, times(0)).setMetadata(anyString(), any(MetadataValue.class));
	}

	@Test
	void createSignWithWrongCoords() {
		// given
		final Server server = mock(Server.class);
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		final World world = mock(World.class);

		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1]", ""}));

		// then
		verify(block, times(0)).setMetadata(anyString(), any(MetadataValue.class));
	}

	@Test
	void createSign() {
		// given
		final Server server = mock(Server.class);
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		final World world = mock(World.class);

		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);
		when(plugin.getLines()).thenReturn(List.of("[TELEPORT]", "", "%WORLD%", "%X%, %Y%, %Z%"));

		// when
		listener.createSign(new SignChangeEvent(block, player, new String[]{"[TELEPORT]", "[xyz]", "[1.4, 1.5, 1.6]", ""}));

		// then
		final String value = JSON_OBJECT.toString();
		verify(block, times(1)).setMetadata(eq(SignTeleportListener.TELEPORT),
				argThat(argument -> plugin.equals(argument.getOwningPlugin()) && value.equals(argument.asString())));
	}

	@Test
	void clickSignWrongAction() {
		// given
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Player player = mock(Player.class);

		// when
		listener.clickSign(new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, null, BlockFace.EAST));

		// then
		verify(player, times(0)).teleport(any(Location.class));
	}

	@Test
	void clickNull() {
		// given
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Player player = mock(Player.class);

		// when
		listener.clickSign(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, null, BlockFace.EAST));

		// then
		verify(player, times(0)).teleport(any(Location.class));
	}

	@Test
	void clickSignWithNoMetadata() {
		// given
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		when(block.getMetadata(SignTeleportListener.TELEPORT)).thenReturn(Collections.emptyList());

		// when
		listener.clickSign(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST));

		// then
		verify(player, times(0)).teleport(any(Location.class));
	}

	@Test
	void clickSignWithMetadata() {
		// given
		final Server server = mock(Server.class);
		final SignTeleportPlugin plugin = mock(SignTeleportPlugin.class);
		final SignTeleportListener listener = new SignTeleportListener(plugin);
		final Block block = mock(Block.class);
		final Player player = mock(Player.class);
		final World world = mock(World.class);

		when(plugin.getServer()).thenReturn(server);
		when(server.getWorld(anyString())).thenReturn(world);
		when(block.getMetadata(SignTeleportListener.TELEPORT))
				.thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, JSON_OBJECT)));

		// when
		listener.clickSign(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST));

		// then
		verify(player, times(1)).teleport(new Location(world, 1.4, 1.5, 1.6));
	}
}

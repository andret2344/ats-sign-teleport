/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class SignTeleportListener implements Listener {
	private static final Pattern LOCATION_PATTERN = Pattern.compile("\\[(-?\\d+(.\\d+)?),\\s*(-?\\d+(.\\d+)?),\\s*(-?\\d+(.\\d+)?)]");
	private static final Pattern WORLD_PATTERN = Pattern.compile("\\[(\\S+)]");

	public static final String TELEPORT = "teleport";
	private static final String KEY_WORLD = "world";
	private static final String KEY_X = "x";
	private static final String KEY_Y = "y";
	private static final String KEY_Z = "z";

	private final SignTeleportPlugin plugin;

	@EventHandler
	public void clickSign(@NotNull final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		final List<MetadataValue> metadata = event.getClickedBlock().getMetadata(TELEPORT);
		if (metadata.isEmpty()) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.hasPermission("ats.signteleport.use")) {
			return;
		}
		final JSONObject jsonObject = new JSONObject(metadata.get(0).asString());
		final Location location = new Location(
				plugin.getServer().getWorld(jsonObject.getString(KEY_WORLD)),
				jsonObject.getDouble(KEY_X),
				jsonObject.getDouble(KEY_Y),
				jsonObject.getDouble(KEY_Z));
		player.teleport(location);
	}

	@EventHandler
	public void breakBlock(@NotNull final BlockBreakEvent event) {
		event.getBlock().removeMetadata(TELEPORT, plugin);
	}

	@EventHandler
	public void createSign(@NotNull final SignChangeEvent event) {
		final String[] lines = event.getLines();
		final JSONObject json = createJson(lines);
		if (json == null) {
			return;
		}
		if (!event.getPlayer().hasPermission("ats.signteleport.create")) {
			return;
		}
		event.getBlock().setMetadata(TELEPORT, new FixedMetadataValue(plugin, json));
		final List<String> configLines = plugin.getConfig().getStringList("lines");
		List.of(0, 1, 2, 3)
				.forEach(i -> event.setLine(i, createLine(configLines.get(i), json)));
	}

	@NotNull
	private String createLine(@NotNull final String pattern, @NotNull final JSONObject jsonObject) {
		final String result = pattern.replace("%WORLD%", jsonObject.getString(KEY_WORLD))
				.replace("%X%", String.valueOf(jsonObject.getDouble(KEY_X)))
				.replace("%Y%", String.valueOf(jsonObject.getDouble(KEY_Y)))
				.replace("%Z%", String.valueOf(jsonObject.getDouble(KEY_Z)));
		return ChatColor.translateAlternateColorCodes('&', result);
	}

	@Nullable
	private JSONObject createJson(@NotNull final String[] lines) {
		if (!lines[0].equalsIgnoreCase("[TELEPORT]")) {
			return null;
		}
		final Matcher worldMatcher = WORLD_PATTERN.matcher(lines[1]);
		if (!worldMatcher.find()) {
			return null;
		}
		if (plugin.getServer().getWorld(worldMatcher.group(1)) == null) {
			return null;
		}
		final Matcher locationMatcher = LOCATION_PATTERN.matcher(lines[2]);
		if (!locationMatcher.find()) {
			return null;
		}
		return new JSONObject()
				.put(KEY_WORLD, worldMatcher.group(1))
				.put(KEY_X, Double.parseDouble(locationMatcher.group(1)))
				.put(KEY_Y, Double.parseDouble(locationMatcher.group(3)))
				.put(KEY_Z, Double.parseDouble(locationMatcher.group(5)));
	}
}

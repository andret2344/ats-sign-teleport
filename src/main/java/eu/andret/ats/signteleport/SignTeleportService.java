/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignTeleportService {
	private static final Pattern LOCATION_PATTERN = Pattern.compile("\\[(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)(,\\s*(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?))?]");
	private static final Pattern WORLD_PATTERN = Pattern.compile("\\[(\\S+)]");
	private static final Pattern YAW_PITCH_PATTERN = Pattern.compile("\\[(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)]");
	private static final String LINES = "lines";

	@NotNull
	private final SignTeleportPlugin plugin;
	private final NamespacedKey keyWorld;
	private final NamespacedKey keyX;
	private final NamespacedKey keyY;
	private final NamespacedKey keyZ;
	private final NamespacedKey keyYaw;
	private final NamespacedKey keyPitch;

	public SignTeleportService(@NotNull final SignTeleportPlugin plugin) {
		this.plugin = plugin;
		keyWorld = new NamespacedKey(plugin, "world");
		keyX = new NamespacedKey(plugin, "x");
		keyY = new NamespacedKey(plugin, "y");
		keyZ = new NamespacedKey(plugin, "z");
		keyYaw = new NamespacedKey(plugin, "yaw");
		keyPitch = new NamespacedKey(plugin, "pitch");
	}

	@Nullable
	public String getWorldName(@NotNull final PersistentDataContainer pdc) {
		return pdc.get(keyWorld, PersistentDataType.STRING);
	}

	public boolean isTeleportSign(@NotNull final PersistentDataContainer pdc) {
		return pdc.has(keyWorld);
	}

	@NotNull
	public Location buildLocation(@NotNull final String worldName, @NotNull final PersistentDataContainer pdc) {
		return new Location(
				plugin.getServer().getWorld(worldName),
				pdc.getOrDefault(keyX, PersistentDataType.DOUBLE, 0.0),
				pdc.getOrDefault(keyY, PersistentDataType.DOUBLE, 0.0),
				pdc.getOrDefault(keyZ, PersistentDataType.DOUBLE, 0.0),
				pdc.getOrDefault(keyYaw, PersistentDataType.FLOAT, 0.0f),
				pdc.getOrDefault(keyPitch, PersistentDataType.FLOAT, 0.0f));
	}

	@NotNull
	public String[] buildEditorLines(@NotNull final PersistentDataContainer pdc, @NotNull final String worldName) {
		final double x = pdc.getOrDefault(keyX, PersistentDataType.DOUBLE, 0.0);
		final double y = pdc.getOrDefault(keyY, PersistentDataType.DOUBLE, 0.0);
		final double z = pdc.getOrDefault(keyZ, PersistentDataType.DOUBLE, 0.0);
		final float yaw = pdc.getOrDefault(keyYaw, PersistentDataType.FLOAT, 0.0f);
		final float pitch = pdc.getOrDefault(keyPitch, PersistentDataType.FLOAT, 0.0f);
		return new String[]{
				"[TELEPORT]",
				"[" + worldName + "]",
				String.format("[%.1f, %.1f, %.1f]", x, y, z),
				String.format("[%.1f, %.1f]", yaw, pitch)
		};
	}

	@Nullable
	public String[] parseSignData(@NotNull final String[] lines) {
		if (!lines[0].equalsIgnoreCase("[TELEPORT]")) {
			return null;
		}
		if (lines[1].startsWith("@")) {
			return parsePlayerSignData(lines[1].substring(1));
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
		final boolean hasYawPitch = locationMatcher.group(7) != null;
		final String yaw;
		final String pitch;
		if (hasYawPitch) {
			yaw = locationMatcher.group(8);
			pitch = locationMatcher.group(10);
		} else {
			final Matcher yawPitchMatcher = YAW_PITCH_PATTERN.matcher(lines[3]);
			if (yawPitchMatcher.find()) {
				yaw = yawPitchMatcher.group(1);
				pitch = yawPitchMatcher.group(3);
			} else {
				yaw = "0.0";
				pitch = "0.0";
			}
		}
		return new String[]{
				worldMatcher.group(1),
				locationMatcher.group(1),
				locationMatcher.group(3),
				locationMatcher.group(5),
				yaw,
				pitch
		};
	}

	public void applySignData(@NotNull final SignChangeEvent event, @NotNull final PersistentDataContainer pdc, @NotNull final String[] data) {
		final double x = Double.parseDouble(data[1]);
		final double y = Double.parseDouble(data[2]);
		final double z = Double.parseDouble(data[3]);
		final float yaw = Float.parseFloat(data[4]);
		final float pitch = Float.parseFloat(data[5]);
		pdc.set(keyWorld, PersistentDataType.STRING, data[0]);
		pdc.set(keyX, PersistentDataType.DOUBLE, x);
		pdc.set(keyY, PersistentDataType.DOUBLE, y);
		pdc.set(keyZ, PersistentDataType.DOUBLE, z);
		pdc.set(keyYaw, PersistentDataType.FLOAT, yaw);
		pdc.set(keyPitch, PersistentDataType.FLOAT, pitch);
		final List<String> configLines = plugin.getConfig().getStringList(LINES);
		if (configLines.size() < 4) {
			return;
		}
		List.of(0, 1, 2, 3)
				.forEach(i -> event.setLine(i, createLine(configLines.get(i), data[0], x, y, z, yaw, pitch)));
	}

	public void updateChunk(@NotNull final Chunk chunk) {
		final List<String> configLines = plugin.getConfig().getStringList(LINES);
		if (configLines.size() < 4) {
			return;
		}
		for (final BlockState tileEntity : chunk.getTileEntities()) {
			if (!(tileEntity instanceof final Sign sign)) {
				continue;
			}
			final PersistentDataContainer pdc = sign.getPersistentDataContainer();
			if (!pdc.has(keyWorld)) {
				continue;
			}
			final String worldName = pdc.get(keyWorld, PersistentDataType.STRING);
			if (worldName == null) {
				continue;
			}
			final double x = pdc.getOrDefault(keyX, PersistentDataType.DOUBLE, 0.0);
			final double y = pdc.getOrDefault(keyY, PersistentDataType.DOUBLE, 0.0);
			final double z = pdc.getOrDefault(keyZ, PersistentDataType.DOUBLE, 0.0);
			final float yaw = pdc.getOrDefault(keyYaw, PersistentDataType.FLOAT, 0.0f);
			final float pitch = pdc.getOrDefault(keyPitch, PersistentDataType.FLOAT, 0.0f);
			final SignSide side = sign.getSide(Side.FRONT);
			for (int i = 0; i < 4; i++) {
				side.setLine(i, createLine(configLines.get(i), worldName, x, y, z, yaw, pitch));
			}
			sign.update();
		}
	}

	public void updateSigns() {
		final List<String> configLines = plugin.getConfig().getStringList(LINES);
		if (configLines.size() < 4) {
			return;
		}
		for (final World world : plugin.getServer().getWorlds()) {
			for (final Chunk chunk : world.getLoadedChunks()) {
				updateChunk(chunk);
			}
		}
	}

	@Nullable
	private String[] parsePlayerSignData(@NotNull final String playerName) {
		final Player target = plugin.getServer().getPlayerExact(playerName);
		if (target == null) {
			return null;
		}
		final Location loc = target.getLocation();
		if (loc.getWorld() == null) {
			return null;
		}
		return new String[]{
				loc.getWorld().getName(),
				String.valueOf(roundToHalf(loc.getX())),
				String.valueOf(roundToHalf(loc.getY())),
				String.valueOf(roundToHalf(loc.getZ())),
				String.valueOf(loc.getYaw()),
				String.valueOf(loc.getPitch())
		};
	}

	@NotNull
	private String createLine(@NotNull final String pattern, @NotNull final String world, final double x, final double y, final double z, final float yaw, final float pitch) {
		final String result = pattern.replace("%WORLD%", world)
				.replace("%X%", String.valueOf(x))
				.replace("%Y%", String.valueOf(y))
				.replace("%Z%", String.valueOf(z))
				.replace("%YAW%", String.valueOf(yaw))
				.replace("%PITCH%", String.valueOf(pitch));
		return ChatColor.translateAlternateColorCodes('&', result);
	}

	private static double roundToHalf(final double value) {
		return Math.round(value * 2) / 2.0;
	}
}

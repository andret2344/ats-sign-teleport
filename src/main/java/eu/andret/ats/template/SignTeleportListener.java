package eu.andret.ats.template;

import eu.andret.ats.template.entity.Teleport;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class SignTeleportListener implements Listener {
	private final SignTeleportPlugin plugin;

	@EventHandler
	public void clickSign(@NotNull final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		plugin.getTeleports().stream()
				.filter(teleport -> teleport.getSign().equals(event.getClickedBlock().getLocation()))
				.findAny()
				.map(Teleport::getTarget)
				.ifPresent(event.getPlayer()::teleport);
	}

	@EventHandler
	public void breakBlock(@NotNull final BlockBreakEvent event) {
		plugin.getTeleports().stream()
				.filter(teleport -> teleport.getSign().equals(event.getBlock().getLocation()))
				.findAny()
				.ifPresent(plugin.getTeleports()::remove);
	}

	@EventHandler
	public void createSign(@NotNull final SignChangeEvent event) {
		final String[] lines = event.getLines();
		final Location target = getLocationFromLines(lines[1], lines[2]);
		if (!lines[0].equalsIgnoreCase("[TELEPORT]") || target == null) {
			return;
		}
		plugin.getTeleports().add(new Teleport(event.getBlock().getLocation(), target));
		event.setLine(0, "");
		event.setLine(1, ChatColor.AQUA + "[TELEPORT]");
		event.setLine(2, "");
	}

	@Nullable
	private Location getLocationFromLines(@NotNull final String worldLine, @NotNull final String locationLine) {
		final Pattern worldPattern = Pattern.compile("\\[(\\S+)]");
		final Pattern locationPattern = Pattern.compile("\\[(-?\\d+(.\\d+)?),\\s*(-?\\d+(.\\d+)?),\\s*(-?\\d+(.\\d+)?)]");

		final Matcher worldMatcher = worldPattern.matcher(worldLine);
		if (!worldMatcher.find()) {
			return null;
		}

		final Matcher locationMatcher = locationPattern.matcher(locationLine);
		if (!locationMatcher.find()) {
			return null;
		}

		final World w = Bukkit.getServer().getWorld(worldMatcher.group(1));
		final double x = Double.parseDouble(locationMatcher.group(1));
		final double y = Double.parseDouble(locationMatcher.group(3));
		final double z = Double.parseDouble(locationMatcher.group(5));
		return new Location(w, x, y, z);
	}
}

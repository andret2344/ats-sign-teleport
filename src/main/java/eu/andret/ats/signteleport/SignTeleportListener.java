/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.jetbrains.annotations.NotNull;

public class SignTeleportListener implements Listener {
	@NotNull
	private final SignTeleportService service;

	public SignTeleportListener(@NotNull final SignTeleportService service) {
		this.service = service;
	}

	@EventHandler
	public void clickSign(@NotNull final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		final BlockState state = event.getClickedBlock().getState();
		if (!(state instanceof final PersistentDataHolder holder)) {
			return;
		}
		final PersistentDataContainer pdc = holder.getPersistentDataContainer();
		final String worldName = service.getWorldName(pdc);
		if (worldName == null) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.hasPermission("ats.signteleport.use")) {
			return;
		}
		player.teleport(service.buildLocation(worldName, pdc));
		event.setCancelled(true);
	}

	@EventHandler
	public void breakBlock(@NotNull final BlockBreakEvent event) {
		final BlockState state = event.getBlock().getState();
		if (!(state instanceof final PersistentDataHolder holder)) {
			return;
		}
		final PersistentDataContainer pdc = holder.getPersistentDataContainer();
		if (!service.isTeleportSign(pdc)) {
			return;
		}
		if (!event.getPlayer().hasPermission("ats.signteleport.break")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void createSign(@NotNull final SignChangeEvent event) {
		final String[] data = service.parseSignData(event.getLines());
		if (data == null) {
			return;
		}
		if (!event.getPlayer().hasPermission("ats.signteleport.create")) {
			return;
		}
		final BlockState state = event.getBlock().getState();
		if (!(state instanceof final PersistentDataHolder holder)) {
			return;
		}
		final PersistentDataContainer pdc = holder.getPersistentDataContainer();
		service.applySignData(event, pdc, data);
		state.update();
	}

	@EventHandler
	public void chunkLoad(@NotNull final ChunkLoadEvent event) {
		service.updateChunk(event.getChunk());
	}
}

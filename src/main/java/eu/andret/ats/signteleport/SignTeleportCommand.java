/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SignTeleportCommand implements CommandExecutor {
	@NotNull
	private final SignTeleportPlugin plugin;
	@NotNull
	private final SignTeleportService service;

	public SignTeleportCommand(@NotNull final SignTeleportPlugin plugin, @NotNull final SignTeleportService service) {
		this.plugin = plugin;
		this.service = service;
	}

	@Override
	public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			plugin.reloadConfig();
			service.updateSigns();
			sender.sendMessage("SignTeleport configuration reloaded.");
			return true;
		}
		return false;
	}
}

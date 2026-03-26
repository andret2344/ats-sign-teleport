/*
 * Copyright Andret Tools System (c) 2026. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.signteleport;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class SignTeleportPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		saveDefaultConfig();
		final SignTeleportService service = new SignTeleportService(this);
		final SignTeleportListener listener = new SignTeleportListener(service);
		getServer().getPluginManager().registerEvents(listener, this);
		service.updateSigns();
		getCommand("signteleport").setExecutor(new SignTeleportCommand(this, service));
		new Metrics(this, 16239);
	}
}

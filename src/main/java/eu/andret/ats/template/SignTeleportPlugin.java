package eu.andret.ats.template;

import eu.andret.ats.template.entity.Teleport;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class SignTeleportPlugin extends JavaPlugin {
	@Getter
	private final ArrayList<Teleport> teleports = new ArrayList<>();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new SignTeleportListener(this), this);
	}
}

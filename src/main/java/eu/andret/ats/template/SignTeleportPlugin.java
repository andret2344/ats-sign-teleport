package eu.andret.ats.template;

import eu.andret.ats.template.entity.Teleport;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SignTeleportPlugin extends JavaPlugin {
	@Getter
	private final List<Teleport> teleports = new ArrayList<>();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new SignTeleportListener(this), this);
	}
}

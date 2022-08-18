package eu.andret.ats.template.entity;

import lombok.Value;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Value
public class Teleport {
	@NotNull
	Location sign;
	@NotNull
	Location target;
}

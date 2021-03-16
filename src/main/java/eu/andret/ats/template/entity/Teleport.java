package eu.andret.ats.template.entity;

import lombok.Value;
import org.bukkit.Location;

@Value
public class Teleport {
	Location sign;
	Location target;
}

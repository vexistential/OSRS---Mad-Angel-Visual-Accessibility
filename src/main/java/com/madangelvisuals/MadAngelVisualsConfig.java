package com.madangelvisuals;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(MadAngelVisualsConfig.GROUP)
public interface MadAngelVisualsConfig extends Config
{
	String GROUP = "madangelvisuals";

	@ConfigItem(
		keyName = "remapShadow",
		name = "Amplify floor shadow",
		description = "Replace the Exploding Light floor telegraph with a brighter vanilla-style marker on the same tile.",
		position = 0
	)
	default boolean remapShadow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remapProjectile",
		name = "Remap light ball",
		description = "Replace the Exploding Light projectile with a brighter curated vanilla projectile.",
		position = 1
	)
	default boolean remapProjectile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "style",
		name = "Visual style",
		description = "Curated brighter appearance for the shadow marker and ball projectile.",
		position = 2
	)
	default LightStyle style()
	{
		return LightStyle.MAGENTA;
	}
}

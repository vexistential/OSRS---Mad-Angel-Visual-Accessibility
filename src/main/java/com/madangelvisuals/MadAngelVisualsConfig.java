package com.madangelvisuals;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(MadAngelVisualsConfig.GROUP)
public interface MadAngelVisualsConfig extends Config
{
	String GROUP = "mad-angel-visual-accessibility";

	@ConfigItem(
		keyName = "remapShadow",
		name = "Recolor floor shadow",
		description = "Recolor the Exploding Light floor shadow on player-stand destinations (not bounce-back to the boss).",
		position = 0
	)
	default boolean remapShadow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "style",
		name = "Shadow color",
		description = "High-contrast tint applied to the floor shadow.",
		position = 1
	)
	default LightStyle style()
	{
		return LightStyle.MAGENTA;
	}
}

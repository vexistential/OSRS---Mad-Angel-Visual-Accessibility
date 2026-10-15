package com.madangelvisuals;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Curated high-contrast colors for the floor shadow recolor.
 */
@Getter
@AllArgsConstructor
public enum LightStyle
{
	MAGENTA("Magenta", new Color(255, 0, 220)),
	CYAN("Cyan", new Color(0, 230, 255)),
	ORANGE("Orange", new Color(255, 140, 0)),
	LIME("Lime", new Color(120, 255, 0));

	private final String displayName;
	private final Color shadowColor;

	@Override
	public String toString()
	{
		return displayName;
	}
}

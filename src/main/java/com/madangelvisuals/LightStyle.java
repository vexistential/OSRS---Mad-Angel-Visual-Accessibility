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
	MAGENTA(new Color(255, 0, 220)),
	CYAN(new Color(0, 230, 255)),
	ORANGE(new Color(255, 140, 0)),
	LIME(new Color(120, 255, 0));

	private final Color shadowColor;
}

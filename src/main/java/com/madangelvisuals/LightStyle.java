package com.madangelvisuals;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Curated highlight colors for the floor telegraph. No free-form color picker.
 */
@Getter
@AllArgsConstructor
public enum LightStyle
{
	MAGENTA(new Color(255, 0, 220)),
	CYAN(new Color(0, 230, 255)),
	ORANGE(new Color(255, 140, 0)),
	LIME(new Color(120, 255, 0));

	/**
	 * Tint applied when recoloring the floor shadow spotanim.
	 */
	private final Color shadowColor;
}

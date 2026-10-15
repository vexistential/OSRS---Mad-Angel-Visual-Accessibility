package com.madangelvisuals;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Curated visual styles. Replacement projectile IDs are fixed vanilla spotanims
 * (same approach as Vardorvis Projectiles / Projectile Override). No free-form IDs.
 */
@Getter
@AllArgsConstructor
public enum LightStyle
{
	MAGENTA(new Color(255, 0, 220), 1341),
	CYAN(new Color(0, 230, 255), 1380),
	ORANGE(new Color(255, 140, 0), 1378),
	LIME(new Color(120, 255, 0), 1343);

	/**
	 * Tint applied to the amplified floor-shadow marker.
	 */
	private final Color shadowColor;

	/**
	 * Vanilla projectile spotanim used to replace the Exploding Light ball.
	 */
	private final int projectileSpotanimId;
}

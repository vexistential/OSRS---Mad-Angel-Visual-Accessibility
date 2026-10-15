package com.madangelvisuals;

import java.awt.Color;
import java.util.Arrays;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;

/**
 * Brightens an existing spotanim/GraphicsObject model in place (cosmetic recolor).
 */
final class ShadowRecolor
{
	private ShadowRecolor()
	{
	}

	static void apply(Model model, Color color)
	{
		if (model == null || color == null)
		{
			return;
		}

		short hsl = JagexColor.rgbToHSL(color.getRGB(), 1.0d);
		int hue = JagexColor.unpackHue(hsl);
		// Push sat/lum high so the shadow reads as an extremely bright tint.
		int sat = Math.min(7, Math.max(JagexColor.unpackSaturation(hsl), 6));
		int lum = Math.min(JagexColor.LUMINANCE_MAX, Math.max(JagexColor.unpackLuminance(hsl), 100));
		short bright = JagexColor.packHSL(hue, sat, lum);
		int brightInt = bright & 0xFFFF;

		short[] unlit = model.getUnlitFaceColors();
		if (unlit != null)
		{
			Arrays.fill(unlit, bright);
		}

		fill(model.getFaceColors1(), brightInt);
		fill(model.getFaceColors2(), brightInt);
		fill(model.getFaceColors3(), brightInt);

		// Invalidate GPU cache for this model once after mutation.
		model.setSceneId(-1);
	}

	private static void fill(int[] colors, int value)
	{
		if (colors != null)
		{
			Arrays.fill(colors, value);
		}
	}
}

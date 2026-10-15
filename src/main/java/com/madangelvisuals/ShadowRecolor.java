package com.madangelvisuals;

import java.awt.Color;
import java.util.Arrays;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;

/**
 * High-contrast in-place tint for an animated spotanim model.
 * Forces opaque faces and max sat/lum so the tint reads on dark floors.
 */
final class ShadowRecolor
{
	/** Brightness multiplier into Jagex HSL conversion (>1 pushes luminance up). */
	private static final double HSL_BRIGHTNESS = 1.5d;

	private ShadowRecolor()
	{
	}

	static void apply(Model model, Color color)
	{
		if (model == null || color == null)
		{
			return;
		}

		short hsl = JagexColor.rgbToHSL(color.getRGB(), HSL_BRIGHTNESS);
		int hue = JagexColor.unpackHue(hsl);
		short bright = JagexColor.packHSL(hue, JagexColor.SATURATION_MAX, JagexColor.LUMINANCE_MAX);
		int brightInt = bright & 0xFFFF;

		short[] unlit = model.getUnlitFaceColors();
		if (unlit != null)
		{
			Arrays.fill(unlit, bright);
		}

		fill(model.getFaceColors1(), brightInt);
		fill(model.getFaceColors2(), brightInt);
		fill(model.getFaceColors3(), brightInt);

		short[] textures = model.getFaceTextures();
		if (textures != null)
		{
			Arrays.fill(textures, (short) -1);
		}

		// 0 = opaque in Jagex face alpha. Shadows stay faint if left translucent.
		byte[] transparencies = model.getFaceTransparencies();
		if (transparencies != null)
		{
			Arrays.fill(transparencies, (byte) 0);
		}
	}

	private static void fill(int[] colors, int value)
	{
		if (colors != null)
		{
			Arrays.fill(colors, value);
		}
	}
}

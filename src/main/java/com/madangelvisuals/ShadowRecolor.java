package com.madangelvisuals;

import java.awt.Color;
import java.util.Arrays;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;

/**
 * Brightens an existing spotanim/GraphicsObject model in place (cosmetic recolor).
 * Clears face textures and transparency so the tint is as visible as untextured VFX.
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
		int sat = JagexColor.SATURATION_MAX;
		int lum = JagexColor.LUMINANCE_MAX;
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

		// Textured faces ignore face colors; strip textures so the tint shows.
		short[] textures = model.getFaceTextures();
		if (textures != null)
		{
			Arrays.fill(textures, (short) -1);
		}

		// Semi-transparent shadows stay faint even when recolored; force opaque faces.
		byte[] transparencies = model.getFaceTransparencies();
		if (transparencies != null)
		{
			Arrays.fill(transparencies, (byte) 0);
		}

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

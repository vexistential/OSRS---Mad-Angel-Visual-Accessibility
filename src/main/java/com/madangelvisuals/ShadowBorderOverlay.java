package com.madangelvisuals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.GraphicsObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines the tracked Exploding Light shadow GraphicsObject mesh itself
 * (not the whole tile).
 */
class ShadowBorderOverlay extends Overlay
{
	private static final int OUTLINE_WIDTH = 3;
	private static final int OUTLINE_FEATHER = 2;

	private final MadAngelVisualsPlugin plugin;
	private final MadAngelVisualsConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	ShadowBorderOverlay(
		MadAngelVisualsPlugin plugin,
		MadAngelVisualsConfig config,
		ModelOutlineRenderer modelOutlineRenderer)
	{
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.remapShadow())
		{
			return null;
		}

		Collection<GraphicsObject> shadows = plugin.getTrackedShadows();
		if (shadows.isEmpty())
		{
			return null;
		}

		Color color = config.style().getShadowColor();
		for (GraphicsObject shadow : shadows)
		{
			if (shadow.finished())
			{
				continue;
			}
			modelOutlineRenderer.drawOutline(shadow, OUTLINE_WIDTH, color, OUTLINE_FEATHER);
		}

		return null;
	}
}

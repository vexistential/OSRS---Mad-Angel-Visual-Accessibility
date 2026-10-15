package com.madangelvisuals;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Bright stroke-only floor outline around tracked shadow meshes, plus a floor-circle
 * fallback for active stand destinations that have no live shadow gfx yet.
 */
class ShadowBorderOverlay extends Overlay
{
	private static final float BORDER_WIDTH = 4f;

	private final Client client;
	private final MadAngelVisualsPlugin plugin;
	private final MadAngelVisualsConfig config;

	@Inject
	ShadowBorderOverlay(Client client, MadAngelVisualsPlugin plugin, MadAngelVisualsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
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
		Collection<LocalPoint> fallbackTiles = plugin.getFallbackOutlineTiles();
		if (shadows.isEmpty() && fallbackTiles.isEmpty())
		{
			return null;
		}

		Color style = config.style().getShadowColor();
		Color border = new Color(style.getRed(), style.getGreen(), style.getBlue(), 255);
		Stroke stroke = new BasicStroke(BORDER_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		Stroke previous = graphics.getStroke();
		graphics.setStroke(stroke);
		graphics.setColor(border);

		for (GraphicsObject shadow : shadows)
		{
			Shape hull = ShadowHull.of(client, shadow);
			if (hull != null)
			{
				graphics.draw(hull);
			}
		}

		int radius = plugin.getFallbackOutlineRadius();
		int plane = client.getTopLevelWorldView().getPlane();
		for (LocalPoint tile : fallbackTiles)
		{
			Shape circle = ShadowHull.floorCircle(client, tile, plane, radius);
			if (circle != null)
			{
				graphics.draw(circle);
			}
		}

		graphics.setStroke(previous);
		return null;
	}
}

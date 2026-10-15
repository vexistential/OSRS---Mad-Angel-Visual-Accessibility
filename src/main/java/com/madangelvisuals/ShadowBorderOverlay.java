package com.madangelvisuals;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Vibrant border (and light fill) around tracked Exploding Light shadow tiles
 * so the recolored shadow remains readable when the mesh itself stays faint.
 */
class ShadowBorderOverlay extends Overlay
{
	private static final int FILL_ALPHA = 90;
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

		Collection<LocalPoint> tiles = plugin.getTrackedShadowTiles();
		if (tiles.isEmpty())
		{
			return null;
		}

		Color style = config.style().getShadowColor();
		Color border = new Color(style.getRed(), style.getGreen(), style.getBlue(), 255);
		Color fill = new Color(style.getRed(), style.getGreen(), style.getBlue(), FILL_ALPHA);
		Stroke stroke = new BasicStroke(BORDER_WIDTH);

		for (LocalPoint lp : tiles)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, border, fill, stroke);
			}
		}

		return null;
	}
}

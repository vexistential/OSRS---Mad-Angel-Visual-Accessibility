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
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws a vibrant hull/outline around the tracked shadow GraphicsObject mesh
 * (not the whole tile). Uses canvas projection rather than ModelOutlineRenderer,
 * which fails to visibly outline flat floor shadows.
 */
class ShadowBorderOverlay extends Overlay
{
	private static final int FILL_ALPHA = 70;
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
		if (shadows.isEmpty())
		{
			return null;
		}

		Color style = config.style().getShadowColor();
		Color border = new Color(style.getRed(), style.getGreen(), style.getBlue(), 255);
		Color fill = new Color(style.getRed(), style.getGreen(), style.getBlue(), FILL_ALPHA);
		Stroke stroke = new BasicStroke(BORDER_WIDTH);

		for (GraphicsObject shadow : shadows)
		{
			Shape hull = ShadowHull.of(client, shadow);
			if (hull != null)
			{
				OverlayUtil.renderPolygon(graphics, hull, border, fill, stroke);
			}
		}

		return null;
	}
}

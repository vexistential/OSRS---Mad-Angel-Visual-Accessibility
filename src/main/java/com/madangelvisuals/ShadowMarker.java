package com.madangelvisuals;

import java.awt.Color;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.AnimationID;

/**
 * Bright ground marker placed on the same tile as a Mad Angel Exploding Light shadow
 * GraphicsObject. Uses the same lootbeam light model pattern as core Ground Items.
 */
final class ShadowMarker
{
	/**
	 * Enakh streaming light model used by core lootbeams — large and easy to recolor.
	 */
	private static final int LIGHT_MODEL_ID = 5809;
	private static final short LIGHT_RECOLOR_SRC = 6371;

	private final GraphicsObject source;
	private final RuneLiteObject runeLiteObject;

	ShadowMarker(Client client, GraphicsObject source, Color color)
	{
		this.source = source;
		this.runeLiteObject = client.createRuneLiteObject();

		Model model = client.loadModel(
			LIGHT_MODEL_ID,
			new short[]{LIGHT_RECOLOR_SRC},
			new short[]{JagexColor.rgbToHSL(color.getRGB(), 1.0d)}
		);
		Animation animation = client.loadAnimation(AnimationID.ENAKH_LIGHT_STREAMING);

		if (model != null)
		{
			runeLiteObject.setModel(model);
		}
		if (animation != null)
		{
			AnimationController controller = new AnimationController(client, animation.getId());
			controller.setOnFinished(AnimationController::loop);
			runeLiteObject.setAnimationController(controller);
		}

		LocalPoint lp = source.getLocation();
		if (lp != null)
		{
			runeLiteObject.setLocation(lp, source.getLevel());
		}

		runeLiteObject.setActive(true);
	}

	GraphicsObject getSource()
	{
		return source;
	}

	boolean isSourceFinished()
	{
		return source.finished();
	}

	void remove()
	{
		runeLiteObject.setActive(false);
	}
}

package com.madangelvisuals;

import java.awt.Color;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

/**
 * Hides the vanilla dark shadow draw and shows the same animated mesh recolored
 * bright via a {@link RuneLiteObject} (same shape, not a substitute prop).
 */
final class ShadowClone
{
	private final GraphicsObject source;
	private final RuneLiteObject replacement;

	ShadowClone(Client client, GraphicsObject source)
	{
		this.source = source;
		this.replacement = client.createRuneLiteObject();
		LocalPoint lp = source.getLocation();
		if (lp != null)
		{
			replacement.setLocation(lp, source.getLevel());
		}
		replacement.setActive(true);
	}

	GraphicsObject getSource()
	{
		return source;
	}

	boolean isSourceFinished()
	{
		return source.finished();
	}

	/**
	 * Pull the current animated frame from the (hidden) source, tint it, and
	 * display that same mesh on the replacement object.
	 */
	void update(Color color)
	{
		if (source.finished())
		{
			return;
		}

		Model model = source.getModel();
		if (model == null)
		{
			return;
		}

		ShadowRecolor.apply(model, color);

		LocalPoint lp = source.getLocation();
		if (lp != null)
		{
			replacement.setLocation(lp, source.getLevel());
		}
		replacement.setModel(model);
	}

	void remove()
	{
		replacement.setActive(false);
	}
}

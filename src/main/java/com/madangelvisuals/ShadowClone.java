package com.madangelvisuals;

import java.awt.Color;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

/**
 * Shows a high-contrast copy of the shadow mesh via {@link RuneLiteObject}.
 * The vanilla entity is only hidden after a recolored model is ready, so a failed
 * {@code getModel()} cannot erase the telegraph entirely.
 */
final class ShadowClone
{
	private final GraphicsObject source;
	private final RuneLiteObject replacement;
	private final Consumer<GraphicsObject> hideSource;
	private boolean sourceHidden;

	ShadowClone(Client client, GraphicsObject source, Consumer<GraphicsObject> hideSource)
	{
		this.source = source;
		this.hideSource = hideSource;
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
	 * Pull the current animated frame from the source, tint it, display it on the
	 * replacement, then hide the vanilla draw once a model is available.
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

		if (!sourceHidden)
		{
			hideSource.accept(source);
			sourceHidden = true;
		}
	}

	void remove()
	{
		replacement.setActive(false);
	}
}

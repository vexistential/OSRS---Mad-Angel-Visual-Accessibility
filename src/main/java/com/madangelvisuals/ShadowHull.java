package com.madangelvisuals;

import java.awt.Shape;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.model.Jarvis;

/**
 * Projects a GraphicsObject's model vertices to canvas and builds a convex hull
 * that follows the mesh silhouette (circular shadows stay round-ish).
 * <p>
 * Do not use {@link Perspective#getClickbox} here — it often returns an AABB,
 * which looks square on floor shadows.
 */
final class ShadowHull
{
	private ShadowHull()
	{
	}

	@Nullable
	static Shape of(Client client, GraphicsObject graphicsObject)
	{
		if (client == null || graphicsObject == null || graphicsObject.finished())
		{
			return null;
		}

		LocalPoint lp = graphicsObject.getLocation();
		Model model = graphicsObject.getModel();
		if (lp == null || model == null)
		{
			return null;
		}

		WorldView wv = graphicsObject.getWorldView();
		if (wv == null)
		{
			wv = client.getTopLevelWorldView();
		}

		int vertexCount = model.getVerticesCount();
		if (vertexCount < 3)
		{
			return null;
		}

		float[] vx = model.getVerticesX();
		float[] vy = model.getVerticesY();
		float[] vz = model.getVerticesZ();
		if (vx == null || vy == null || vz == null)
		{
			return null;
		}

		int z = graphicsObject.getZ() - graphicsObject.getAnimationHeightOffset();
		int[] x2d = new int[vertexCount];
		int[] y2d = new int[vertexCount];
		Perspective.modelToCanvas(client, wv, vertexCount, lp.getX(), lp.getY(), z, 0, vx, vy, vz, x2d, y2d);
		return Jarvis.convexHull(x2d, y2d);
	}
}

package com.madangelvisuals;

import java.awt.Shape;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.model.Jarvis;

/**
 * Floor-parallel outline of a shadow GraphicsObject.
 * <p>
 * Collapses model vertices onto the model's lowest Y (ground contact) before
 * projecting, so the convex hull lies on the floor plane instead of forming a
 * vertical billboard circle from the full 3D silhouette.
 * <p>
 * Do not use {@link Perspective#getClickbox} — it often returns an AABB.
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

		// Flatten onto the floor contact plane (min model Y) so the hull is
		// parallel to the ground, matching the looming shadow disc.
		float floorY = vy[0];
		for (int i = 1; i < vertexCount; i++)
		{
			if (vy[i] < floorY)
			{
				floorY = vy[i];
			}
		}
		float[] flatY = new float[vertexCount];
		Arrays.fill(flatY, floorY);

		int z = graphicsObject.getZ() - graphicsObject.getAnimationHeightOffset();
		int[] x2d = new int[vertexCount];
		int[] y2d = new int[vertexCount];
		Perspective.modelToCanvas(client, wv, vertexCount, lp.getX(), lp.getY(), z, 0, vx, flatY, vz, x2d, y2d);
		return Jarvis.convexHull(x2d, y2d);
	}
}

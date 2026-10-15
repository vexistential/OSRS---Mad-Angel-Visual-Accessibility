package com.madangelvisuals;

import java.awt.Shape;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.model.Jarvis;

/**
 * Builds a floor-parallel outline of a shadow GraphicsObject.
 * <p>
 * Finds the model's thin axis (disc normal), takes the other two axes as the
 * footprint, places those offsets on the tile plane, and projects with
 * {@link Perspective#localToCanvas}. That matches tile polygons visually —
 * lying flat — instead of a vertical 3D silhouette from {@code modelToCanvas}.
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

		// Disc normal = axis with least spread; the other two span the footprint.
		int normalAxis = thinnestAxis(vx, vy, vz, vertexCount);
		float[] axisU = axis(vx, vy, vz, (normalAxis + 1) % 3);
		float[] axisV = axis(vx, vy, vz, (normalAxis + 2) % 3);

		int plane = graphicsObject.getLevel();
		int worldViewId = wv.getId();
		int[] x2d = new int[vertexCount];
		int[] y2d = new int[vertexCount];
		int projected = 0;

		for (int i = 0; i < vertexCount; i++)
		{
			int sceneX = lp.getX() + Math.round(axisU[i]);
			int sceneY = lp.getY() + Math.round(axisV[i]);
			LocalPoint groundPoint = new LocalPoint(sceneX, sceneY, worldViewId);
			int groundHeight = Perspective.getTileHeight(client, groundPoint, plane);
			Point canvas = Perspective.localToCanvas(client, sceneX, sceneY, groundHeight);
			if (canvas != null)
			{
				x2d[projected] = canvas.getX();
				y2d[projected] = canvas.getY();
				projected++;
			}
		}

		if (projected < 3)
		{
			return null;
		}

		return Jarvis.convexHull(Arrays.copyOf(x2d, projected), Arrays.copyOf(y2d, projected));
	}

	private static int thinnestAxis(float[] vx, float[] vy, float[] vz, int count)
	{
		float spreadX = spread(vx, count);
		float spreadY = spread(vy, count);
		float spreadZ = spread(vz, count);

		int axis = 0;
		float min = spreadX;
		if (spreadY < min)
		{
			min = spreadY;
			axis = 1;
		}
		if (spreadZ < min)
		{
			axis = 2;
		}
		return axis;
	}

	private static float spread(float[] values, int count)
	{
		float min = values[0];
		float max = values[0];
		for (int i = 1; i < count; i++)
		{
			float v = values[i];
			if (v < min)
			{
				min = v;
			}
			if (v > max)
			{
				max = v;
			}
		}
		return max - min;
	}

	private static float[] axis(float[] vx, float[] vy, float[] vz, int index)
	{
		switch (index)
		{
			case 1:
				return vy;
			case 2:
				return vz;
			case 0:
			default:
				return vx;
		}
	}
}

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
 * Floor-parallel outlines for Exploding Light shadows.
 * <p>
 * Model footprints are placed on the tile with {@link Perspective#localToCanvas}
 * (not {@code modelToCanvas} silhouettes, which read as vertical circles).
 */
final class ShadowHull
{
	static final int DEFAULT_FALLBACK_RADIUS = Perspective.LOCAL_TILE_SIZE / 2;
	private static final int CIRCLE_SEGMENTS = 24;

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
			Point canvas = projectGround(client, sceneX, sceneY, plane, worldViewId);
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

	/**
	 * Floor-parallel circle used when a stand destination is pending but no live
	 * shadow GraphicsObject is available yet (e.g. already standing on the tile).
	 */
	@Nullable
	static Shape floorCircle(Client client, LocalPoint center, int plane, int radius)
	{
		if (client == null || center == null || radius <= 0)
		{
			return null;
		}

		int worldViewId = center.getWorldView();
		int[] x2d = new int[CIRCLE_SEGMENTS];
		int[] y2d = new int[CIRCLE_SEGMENTS];
		int projected = 0;

		for (int i = 0; i < CIRCLE_SEGMENTS; i++)
		{
			double angle = (Math.PI * 2 * i) / CIRCLE_SEGMENTS;
			int sceneX = center.getX() + (int) Math.round(Math.cos(angle) * radius);
			int sceneY = center.getY() + (int) Math.round(Math.sin(angle) * radius);
			Point canvas = projectGround(client, sceneX, sceneY, plane, worldViewId);
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

	/** Horizontal footprint radius from a shadow model (for fallback sizing). */
	static int footprintRadius(GraphicsObject graphicsObject)
	{
		if (graphicsObject == null || graphicsObject.finished())
		{
			return DEFAULT_FALLBACK_RADIUS;
		}

		Model model = graphicsObject.getModel();
		if (model == null)
		{
			return DEFAULT_FALLBACK_RADIUS;
		}

		int vertexCount = model.getVerticesCount();
		float[] vx = model.getVerticesX();
		float[] vy = model.getVerticesY();
		float[] vz = model.getVerticesZ();
		if (vertexCount < 1 || vx == null || vy == null || vz == null)
		{
			return DEFAULT_FALLBACK_RADIUS;
		}

		int normalAxis = thinnestAxis(vx, vy, vz, vertexCount);
		float[] axisU = axis(vx, vy, vz, (normalAxis + 1) % 3);
		float[] axisV = axis(vx, vy, vz, (normalAxis + 2) % 3);

		float maxR = 0f;
		for (int i = 0; i < vertexCount; i++)
		{
			float r = (float) Math.hypot(axisU[i], axisV[i]);
			if (r > maxR)
			{
				maxR = r;
			}
		}

		int radius = Math.round(maxR);
		return radius > 0 ? radius : DEFAULT_FALLBACK_RADIUS;
	}

	@Nullable
	private static Point projectGround(Client client, int sceneX, int sceneY, int plane, int worldViewId)
	{
		LocalPoint groundPoint = new LocalPoint(sceneX, sceneY, worldViewId);
		int groundHeight = Perspective.getTileHeight(client, groundPoint, plane);
		return Perspective.localToCanvas(client, sceneX, sceneY, groundHeight);
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

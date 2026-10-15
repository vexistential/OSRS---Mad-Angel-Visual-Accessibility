package com.madangelvisuals;

import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/**
 * Combat Mad Angel NPC IDs (quest + post-quest). Not all are present as gameval
 * constants in every client build, so they are listed explicitly.
 */
final class MadAngelNpcs
{
	static final int MAD_ANGEL = 16305;
	static final int MAD_ANGEL_ANIM = 16307;
	static final int MAD_ANGEL_QUEST = 16309;
	static final int MAD_ANGEL_ANIM_QUEST = 16311;

	private MadAngelNpcs()
	{
	}

	static boolean isMadAngel(NPC npc)
	{
		if (npc == null)
		{
			return false;
		}
		return isMadAngelId(npc.getId());
	}

	static boolean isMadAngelId(int id)
	{
		return id == MAD_ANGEL
			|| id == MAD_ANGEL_ANIM
			|| id == MAD_ANGEL_QUEST
			|| id == MAD_ANGEL_ANIM_QUEST;
	}

	/**
	 * True if {@code point} is inside the NPC's south-west tile + size footprint.
	 */
	static boolean isInsideFootprint(NPC npc, net.runelite.api.coords.WorldPoint point)
	{
		if (npc == null || point == null)
		{
			return false;
		}
		if (point.getPlane() != npc.getWorldLocation().getPlane())
		{
			return false;
		}

		net.runelite.api.coords.WorldPoint sw = npc.getWorldLocation();
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getComposition();
		}
		int size = composition != null ? composition.getSize() : 1;

		int x = point.getX();
		int y = point.getY();
		return x >= sw.getX() && x < sw.getX() + size
			&& y >= sw.getY() && y < sw.getY() + size;
	}
}

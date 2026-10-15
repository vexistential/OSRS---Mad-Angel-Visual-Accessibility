package com.madangelvisuals;

import net.runelite.api.NPC;

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
}

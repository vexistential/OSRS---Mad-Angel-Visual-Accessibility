package com.madangelvisuals;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MadAngelVisualsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MadAngelVisualsPlugin.class);
		RuneLite.main(args);
	}
}

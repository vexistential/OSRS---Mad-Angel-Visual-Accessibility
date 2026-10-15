package com.madangelvisuals;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Cosmetic remaps for Mad Angel Exploding Light visuals.
 * Amplifies the existing floor telegraph / ball projectile only — does not draw
 * stand-tile overlays or predict landing locations.
 */
@Slf4j
@PluginDescriptor(
	name = "Mad Angel Light Visuals",
	description = "Amplifies Mad Angel Exploding Light shadow and ball visuals for accessibility",
	tags = {"mad angel", "wyrmscraig", "pvm", "accessibility", "visuals"}
)
public class MadAngelVisualsPlugin extends Plugin implements RenderCallback
{
	/**
	 * Floor telegraph / impact spotanims from gameval. Confirm in-fight which ID(s)
	 * are the looming shadow vs bounce/explosion (see docs/ID_CAPTURE.md).
	 */
	private static final int[] SHADOW_SPOTANIMS = {
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_01,
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_02,
	};

	@Inject
	private Client client;

	@Inject
	private MadAngelVisualsConfig config;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	private final Set<Projectile> remappedProjectiles = new HashSet<>();
	private final Set<GraphicsObject> hiddenShadows = new HashSet<>();
	private final List<ShadowMarker> shadowMarkers = new ArrayList<>();

	@Provides
	MadAngelVisualsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MadAngelVisualsConfig.class);
	}

	@Override
	protected void startUp()
	{
		renderCallbackManager.register(this);
		log.debug("Mad Angel Light Visuals started");
	}

	@Override
	protected void shutDown()
	{
		renderCallbackManager.unregister(this);
		clearMarkers();
		remappedProjectiles.clear();
		hiddenShadows.clear();
		log.debug("Mad Angel Light Visuals stopped");
	}

	@Override
	public boolean addEntity(Renderable renderable, boolean ui)
	{
		if (renderable instanceof GraphicsObject && hiddenShadows.contains(renderable))
		{
			return false;
		}
		return true;
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (!config.remapProjectile())
		{
			return;
		}

		Projectile projectile = event.getProjectile();
		if (projectile.getId() != SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE)
		{
			return;
		}

		if (remappedProjectiles.contains(projectile))
		{
			return;
		}

		int replacementId = config.style().getProjectileSpotanimId();
		Projectile replacement = client.createProjectile(
			replacementId,
			projectile.getSourcePoint(),
			projectile.getStartHeight(),
			projectile.getSourceActor(),
			projectile.getTargetPoint(),
			projectile.getEndHeight(),
			projectile.getTargetActor(),
			projectile.getStartCycle(),
			projectile.getEndCycle(),
			projectile.getSlope(),
			projectile.getStartPos()
		);

		client.getProjectiles().addLast(replacement);
		remappedProjectiles.add(replacement);
		projectile.setEndCycle(0);

		log.debug("Remapped Exploding Light projectile {} -> {}", projectile.getId(), replacementId);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (!config.remapShadow())
		{
			return;
		}

		GraphicsObject graphicsObject = event.getGraphicsObject();
		if (!isShadowSpotanim(graphicsObject.getId()))
		{
			return;
		}

		hiddenShadows.add(graphicsObject);
		shadowMarkers.add(new ShadowMarker(client, graphicsObject, config.style().getShadowColor()));
		log.debug("Amplified Exploding Light shadow gfx {}", graphicsObject.getId());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		remappedProjectiles.removeIf(p -> p.getRemainingCycles() < 0);

		Iterator<ShadowMarker> it = shadowMarkers.iterator();
		while (it.hasNext())
		{
			ShadowMarker marker = it.next();
			if (marker.isSourceFinished())
			{
				hiddenShadows.remove(marker.getSource());
				marker.remove();
				it.remove();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING
			|| event.getGameState() == GameState.LOGGING_IN)
		{
			clearMarkers();
			remappedProjectiles.clear();
			hiddenShadows.clear();
		}
	}

	private void clearMarkers()
	{
		for (ShadowMarker marker : shadowMarkers)
		{
			marker.remove();
		}
		shadowMarkers.clear();
	}

	private static boolean isShadowSpotanim(int id)
	{
		for (int shadowId : SHADOW_SPOTANIMS)
		{
			if (shadowId == id)
			{
				return true;
			}
		}
		return false;
	}
}

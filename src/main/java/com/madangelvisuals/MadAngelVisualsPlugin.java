package com.madangelvisuals;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Recolors Mad Angel Exploding Light floor shadows on player-stand destinations.
 * Does not alter the ball projectile or draw tile-fill stand markers.
 */
@Slf4j
@PluginDescriptor(
	name = "Mad Angel Light Visuals",
	description = "Recolors Mad Angel Exploding Light floor shadows for accessibility",
	tags = {"mad angel", "wyrmscraig", "pvm", "accessibility", "visuals"}
)
public class MadAngelVisualsPlugin extends Plugin
{
	/** Prefer Mad Angel bomb impact/telegraph spotanims when present on the destination tile. */
	private static final int[] PREFERRED_SHADOW_SPOTANIMS = {
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_01,
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_02,
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW,
	};

	private static final int PENDING_EXTRA_TICKS = 10;

	@Inject
	private Client client;

	@Inject
	private MadAngelVisualsConfig config;

	private final Set<Projectile> seenBombProjectiles = new HashSet<>();
	private final Set<NPC> madAngels = new HashSet<>();
	/** Player-stand destinations awaiting / receiving shadow recolor. */
	private final List<PendingDestination> pendingDestinations = new ArrayList<>();
	/** GraphicsObjects we are actively recoloring until they finish. */
	private final List<GraphicsObject> recoloredShadows = new ArrayList<>();

	@Provides
	MadAngelVisualsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MadAngelVisualsConfig.class);
	}

	@Override
	protected void startUp()
	{
		scanMadAngels();
		log.debug("Mad Angel Light Visuals started");
	}

	@Override
	protected void shutDown()
	{
		clearState();
		log.debug("Mad Angel Light Visuals stopped");
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (MadAngelNpcs.isMadAngel(npc))
		{
			madAngels.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		madAngels.remove(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		madAngels.remove(npc);
		if (MadAngelNpcs.isMadAngel(npc))
		{
			madAngels.add(npc);
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		if (projectile.getId() != SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE)
		{
			return;
		}

		if (!seenBombProjectiles.add(projectile))
		{
			return;
		}

		WorldPoint target = projectile.getTargetPoint();
		log.debug("Bomb projectile seen target={} remaining={} targetActor={}",
			target, projectile.getRemainingCycles(), projectile.getTargetActor());

		if (!config.remapShadow())
		{
			return;
		}

		if (isBounceBackToBoss(projectile, target))
		{
			log.debug("Skipping shadow recolor; bounce-back toward Mad Angel");
			return;
		}

		if (target == null)
		{
			return;
		}

		LocalPoint lp = localFromWorld(client, target);
		if (lp == null)
		{
			log.debug("Could not resolve LocalPoint for bomb target {}", target);
			return;
		}

		pendingDestinations.add(new PendingDestination(projectile, target, lp));
		tryRecolorAtDestination(target, lp);
		log.debug("Pending shadow recolor at {}", target);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (!config.remapShadow() || pendingDestinations.isEmpty())
		{
			return;
		}

		GraphicsObject go = event.getGraphicsObject();
		LocalPoint lp = go.getLocation();
		if (lp == null)
		{
			return;
		}

		WorldPoint wp = WorldPoint.fromLocalInstance(client, lp);
		if (wp == null)
		{
			wp = WorldPoint.fromLocal(client, lp);
		}

		for (PendingDestination pending : pendingDestinations)
		{
			if (sameTile(pending.target, wp) || sameSceneTile(pending.localPoint, lp))
			{
				if (trackAndRecolor(go))
				{
					log.debug("Recolored shadow gfx id={} at {}", go.getId(), wp);
				}
				return;
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		seenBombProjectiles.removeIf(p -> p.getRemainingCycles() < 0);

		Iterator<PendingDestination> pendingIt = pendingDestinations.iterator();
		while (pendingIt.hasNext())
		{
			PendingDestination pending = pendingIt.next();
			if (pending.projectile.getRemainingCycles() < 0)
			{
				pending.ticksAfterProjectile++;
				if (pending.ticksAfterProjectile > PENDING_EXTRA_TICKS)
				{
					pendingIt.remove();
					continue;
				}
			}
			tryRecolorAtDestination(pending.target, pending.localPoint);
		}

		Iterator<GraphicsObject> shadowIt = recoloredShadows.iterator();
		while (shadowIt.hasNext())
		{
			GraphicsObject go = shadowIt.next();
			if (go.finished())
			{
				shadowIt.remove();
				continue;
			}
			ShadowRecolor.apply(go.getModel(), config.style().getShadowColor());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING
			|| event.getGameState() == GameState.LOGGING_IN)
		{
			clearState();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			scanMadAngels();
		}
	}

	private void tryRecolorAtDestination(WorldPoint target, LocalPoint localPoint)
	{
		GraphicsObject preferred = null;
		GraphicsObject fallback = null;

		for (GraphicsObject go : client.getTopLevelWorldView().getGraphicsObjects())
		{
			if (go.finished())
			{
				continue;
			}
			LocalPoint goLp = go.getLocation();
			if (goLp == null || !sameSceneTile(localPoint, goLp))
			{
				continue;
			}

			if (isPreferredShadowSpotanim(go.getId()))
			{
				preferred = go;
				break;
			}
			if (fallback == null)
			{
				fallback = go;
			}
		}

		GraphicsObject chosen = preferred != null ? preferred : fallback;
		if (chosen != null)
		{
			trackAndRecolor(chosen);
		}
	}

	private boolean trackAndRecolor(GraphicsObject go)
	{
		if (recoloredShadows.contains(go))
		{
			ShadowRecolor.apply(go.getModel(), config.style().getShadowColor());
			return true;
		}

		Model model = go.getModel();
		if (model == null)
		{
			// Model may not be ready this frame; keep pending and retry on tick.
			return false;
		}

		recoloredShadows.add(go);
		ShadowRecolor.apply(model, config.style().getShadowColor());
		return true;
	}

	private boolean isBounceBackToBoss(Projectile projectile, WorldPoint target)
	{
		Actor targetActor = projectile.getTargetActor();
		if (targetActor instanceof NPC && MadAngelNpcs.isMadAngel((NPC) targetActor))
		{
			return true;
		}

		if (target == null)
		{
			return false;
		}

		for (NPC npc : madAngels)
		{
			if (MadAngelNpcs.isInsideFootprint(npc, target))
			{
				return true;
			}
		}
		return false;
	}

	private void scanMadAngels()
	{
		madAngels.clear();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (MadAngelNpcs.isMadAngel(npc))
			{
				madAngels.add(npc);
			}
		}
	}

	private void clearState()
	{
		seenBombProjectiles.clear();
		madAngels.clear();
		pendingDestinations.clear();
		recoloredShadows.clear();
	}

	@Nullable
	static LocalPoint localFromWorld(Client client, WorldPoint worldPoint)
	{
		if (worldPoint == null)
		{
			return null;
		}
		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (lp == null)
		{
			lp = LocalPoint.fromWorld(client, worldPoint);
		}
		return lp;
	}

	private static boolean sameTile(WorldPoint a, WorldPoint b)
	{
		return a != null && b != null
			&& a.getX() == b.getX()
			&& a.getY() == b.getY()
			&& a.getPlane() == b.getPlane();
	}

	private static boolean sameSceneTile(LocalPoint a, LocalPoint b)
	{
		if (a == null || b == null)
		{
			return false;
		}
		return a.getSceneX() == b.getSceneX() && a.getSceneY() == b.getSceneY();
	}

	private static boolean isPreferredShadowSpotanim(int id)
	{
		for (int preferred : PREFERRED_SHADOW_SPOTANIMS)
		{
			if (preferred == id)
			{
				return true;
			}
		}
		return false;
	}

	private static final class PendingDestination
	{
		private final Projectile projectile;
		private final WorldPoint target;
		private final LocalPoint localPoint;
		private int ticksAfterProjectile;

		private PendingDestination(Projectile projectile, WorldPoint target, LocalPoint localPoint)
		{
			this.projectile = projectile;
			this.target = target;
			this.localPoint = localPoint;
		}
	}
}

package com.madangelvisuals;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
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
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Recolors Mad Angel Exploding Light floor shadows on player-stand destinations.
 * Mutates the shadow GraphicsObject model in place each frame and outlines the
 * shadow mesh footprint for readability.
 */
@Slf4j
@PluginDescriptor(
	name = "Mad Angel Light Visuals",
	description = "Recolors Mad Angel Exploding Light floor shadows for accessibility",
	tags = {"mad angel", "wyrmscraig", "pvm", "accessibility", "visuals"}
)
public class MadAngelVisualsPlugin extends Plugin
{
	/**
	 * Known Mad Angel bomb ground spotanims. Early destination gfx during a bomb
	 * window may also be tracked so an unlisted loom ID can still be remapped.
	 */
	private static final int[] SHADOW_SPOTANIMS = {
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW,
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_01,
		SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_IMPACT_02,
	};

	/** Client cycles (~20ms) after bomb spawn during which destination gfx are loom candidates. */
	private static final int EARLY_LOOM_CYCLES = 50;

	/** Keep scanning destination tiles briefly after the ball despawns. */
	private static final int PENDING_EXTRA_TICKS = 8;

	@Inject
	private Client client;

	@Inject
	private MadAngelVisualsConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ShadowBorderOverlay shadowBorderOverlay;

	private final List<PendingDestination> pendingDestinations = new ArrayList<>();
	private final Set<GraphicsObject> trackedShadows = Collections.newSetFromMap(new IdentityHashMap<>());
	private int lastFootprintRadius = ShadowHull.DEFAULT_FALLBACK_RADIUS;

	@Provides
	MadAngelVisualsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MadAngelVisualsConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(shadowBorderOverlay);
		log.debug("Mad Angel Light Visuals started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(shadowBorderOverlay);
		clearState();
		log.debug("Mad Angel Light Visuals stopped");
	}

	/**
	 * Live floor-shadow GraphicsObjects for model outline (not tile fill).
	 */
	Collection<GraphicsObject> getTrackedShadows()
	{
		if (trackedShadows.isEmpty())
		{
			return Collections.emptyList();
		}

		List<GraphicsObject> live = new ArrayList<>(trackedShadows.size());
		for (GraphicsObject go : trackedShadows)
		{
			if (!go.finished())
			{
				live.add(go);
			}
		}
		return live;
	}

	/**
	 * Stand destinations that need a floor-circle fallback because no live shadow
	 * gfx has appeared yet. Once a shadow was seen for that pending, fallback must
	 * not reappear after the gfx ends.
	 */
	Collection<LocalPoint> getFallbackOutlineTiles()
	{
		if (pendingDestinations.isEmpty())
		{
			return Collections.emptyList();
		}

		List<LocalPoint> tiles = new ArrayList<>();
		for (PendingDestination pending : pendingDestinations)
		{
			if (pending.sawShadow || hasLiveShadowOnTile(pending.localPoint))
			{
				continue;
			}
			tiles.add(pending.localPoint);
		}
		return tiles;
	}

	int getFallbackOutlineRadius()
	{
		return lastFootprintRadius > 0 ? lastFootprintRadius : ShadowHull.DEFAULT_FALLBACK_RADIUS;
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		if (projectile.getId() != SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE)
		{
			return;
		}

		if (!config.remapShadow())
		{
			return;
		}

		WorldPoint target = projectile.getTargetPoint();
		// Event position is the projectile target LocalPoint (instance-safe).
		LocalPoint lp = event.getPosition();
		if (lp == null)
		{
			lp = localFromWorld(client, target);
		}

		// Clear stand pendings on boss hops so a later stand destination can re-arm.
		if (isBounceBackToBoss(projectile))
		{
			clearAllPendings();
			log.debug("Skipping shadow recolor; bounce-back toward Mad Angel");
			return;
		}

		if (lp == null)
		{
			log.debug("Could not resolve LocalPoint for bomb target {}", target);
			return;
		}

		if (hasPendingAt(lp))
		{
			return;
		}

		armStandDestination(projectile, target, lp);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (!config.remapShadow())
		{
			return;
		}

		GraphicsObject go = event.getGraphicsObject();
		LocalPoint lp = go.getLocation();
		if (lp == null)
		{
			return;
		}

		// Known bomb shadows are authoritative for the stand tile. Snap the single
		// pending onto that gfx when ProjectileMoved lagged on a previous tile.
		if (isShadowSpotanim(go.getId()) && (hasLiveBombProjectile() || !pendingDestinations.isEmpty()))
		{
			PendingDestination pending = findPendingFor(lp, go.getLevel());
			if (pending == null)
			{
				Projectile bomb = findLiveBombProjectile();
				WorldPoint target = bomb != null ? bomb.getTargetPoint() : WorldPoint.fromLocalInstance(client, lp);
				armStandDestination(bomb, target, lp);
				pending = findPendingFor(lp, go.getLevel());
			}
			if (pending != null)
			{
				trackShadow(go, pending);
			}
			return;
		}

		if (pendingDestinations.isEmpty())
		{
			return;
		}

		PendingDestination pending = findPendingFor(lp, go.getLevel());
		if (pending != null && shouldTrack(go, pending))
		{
			trackShadow(go, pending);
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (!config.remapShadow())
		{
			return;
		}

		for (PendingDestination pending : pendingDestinations)
		{
			tryTrackShadowAt(pending.localPoint);
		}

		Iterator<GraphicsObject> it = trackedShadows.iterator();
		while (it.hasNext())
		{
			GraphicsObject go = it.next();
			if (go.finished())
			{
				it.remove();
				continue;
			}

			Model model = go.getModel();
			if (model == null)
			{
				continue;
			}
			// Mutate only for this frame's draw — do not store or reassign the model.
			ShadowRecolor.apply(model, config.style().getShadowColor());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Iterator<PendingDestination> pendingIt = pendingDestinations.iterator();
		while (pendingIt.hasNext())
		{
			PendingDestination pending = pendingIt.next();
			if (!hasLiveBombProjectile())
			{
				pending.ticksAfterProjectile++;
				if (pending.ticksAfterProjectile > PENDING_EXTRA_TICKS)
				{
					pendingIt.remove();
					continue;
				}
			}
			tryTrackShadowAt(pending.localPoint);
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
	}

	private void tryTrackShadowAt(LocalPoint localPoint)
	{
		for (GraphicsObject go : client.getTopLevelWorldView().getGraphicsObjects())
		{
			if (go.finished() || trackedShadows.contains(go))
			{
				continue;
			}
			LocalPoint goLp = go.getLocation();
			if (goLp == null || !sameSceneTile(localPoint, goLp))
			{
				continue;
			}

			PendingDestination pending = findPendingFor(goLp, go.getLevel());
			if (pending != null && shouldTrack(go, pending))
			{
				trackShadow(go, pending);
			}
		}
	}

	private boolean shouldTrack(GraphicsObject go, PendingDestination pending)
	{
		if (isShadowSpotanim(go.getId()))
		{
			return true;
		}

		// Accept unknown destination gfx only in the early window after arming so
		// later slash-dust on the stand tile is ignored.
		int age = client.getGameCycle() - pending.startCycle;
		return age >= 0 && age <= EARLY_LOOM_CYCLES;
	}

	private void trackShadow(GraphicsObject go, PendingDestination pending)
	{
		trackedShadows.add(go);
		pending.sawShadow = true;
		markSawShadowOnTile(go.getLocation());
		lastFootprintRadius = ShadowHull.footprintRadius(go);
		log.debug("Tracking floor shadow gfx id={}", go.getId());
	}

	private void markSawShadowOnTile(LocalPoint lp)
	{
		if (lp == null)
		{
			return;
		}
		for (PendingDestination pending : pendingDestinations)
		{
			if (sameSceneTile(pending.localPoint, lp))
			{
				pending.sawShadow = true;
			}
		}
	}

	private boolean hasPendingAt(LocalPoint lp)
	{
		for (PendingDestination pending : pendingDestinations)
		{
			if (sameSceneTile(pending.localPoint, lp))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Exactly one stand destination at a time. Clears prior hops so adjacent
	 * retargets cannot leave dual fallback circles.
	 */
	private void armStandDestination(@Nullable Projectile projectile, @Nullable WorldPoint target, LocalPoint lp)
	{
		clearAllPendings();
		pendingDestinations.add(new PendingDestination(projectile, target, lp, client.getGameCycle()));
		log.debug("Armed stand destination scene=({}, {})", lp.getSceneX(), lp.getSceneY());
		tryTrackShadowAt(lp);
	}

	private void clearAllPendings()
	{
		pendingDestinations.clear();
	}

	private boolean hasLiveBombProjectile()
	{
		return findLiveBombProjectile() != null;
	}

	@Nullable
	private Projectile findLiveBombProjectile()
	{
		for (Projectile projectile : client.getProjectiles())
		{
			if (projectile.getId() == SpotanimID.VFX_MAD_ANGEL_ATTACK_BOMB_THROW_PROJECTILE
				&& projectile.getRemainingCycles() >= 0)
			{
				return projectile;
			}
		}
		return null;
	}

	private boolean hasLiveShadowOnTile(LocalPoint lp)
	{
		for (GraphicsObject go : trackedShadows)
		{
			if (go.finished())
			{
				continue;
			}
			LocalPoint goLp = go.getLocation();
			if (goLp != null && sameSceneTile(lp, goLp))
			{
				return true;
			}
		}
		return false;
	}

	@Nullable
	private PendingDestination findPendingFor(LocalPoint lp, int level)
	{
		for (PendingDestination pending : pendingDestinations)
		{
			if (sameSceneTile(pending.localPoint, lp))
			{
				return pending;
			}
			if (pending.target != null)
			{
				WorldPoint goWp = WorldPoint.fromLocalInstance(client, lp);
				if (goWp == null)
				{
					goWp = WorldPoint.fromLocal(client, lp);
				}
				if (sameTile(pending.target, goWp) && (level < 0 || pending.target.getPlane() == level))
				{
					return pending;
				}
			}
		}
		return null;
	}

	/**
	 * True when the bomb is bouncing back toward the Mad Angel ({@code targetActor}).
	 * Footprint checks are not used — stand tiles can lie under the multi-tile NPC.
	 */
	private boolean isBounceBackToBoss(Projectile projectile)
	{
		Actor targetActor = projectile.getTargetActor();
		return targetActor instanceof NPC && MadAngelNpcs.isMadAngel((NPC) targetActor);
	}

	private void clearState()
	{
		trackedShadows.clear();
		pendingDestinations.clear();
		lastFootprintRadius = ShadowHull.DEFAULT_FALLBACK_RADIUS;
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

	private static boolean sameSceneTile(LocalPoint a, LocalPoint b)
	{
		if (a == null || b == null)
		{
			return false;
		}
		return a.getSceneX() == b.getSceneX() && a.getSceneY() == b.getSceneY();
	}

	private static boolean sameTile(WorldPoint a, WorldPoint b)
	{
		return a != null && b != null
			&& a.getX() == b.getX()
			&& a.getY() == b.getY()
			&& a.getPlane() == b.getPlane();
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

	private static final class PendingDestination
	{
		@Nullable
		private final Projectile projectile;
		@Nullable
		private final WorldPoint target;
		private final LocalPoint localPoint;
		private final int startCycle;
		private int ticksAfterProjectile;
		/** True once a live shadow gfx was tracked for this stand hop. */
		private boolean sawShadow;

		private PendingDestination(@Nullable Projectile projectile, @Nullable WorldPoint target,
			LocalPoint localPoint, int startCycle)
		{
			this.projectile = projectile;
			this.target = target;
			this.localPoint = localPoint;
			this.startCycle = startCycle;
		}
	}
}

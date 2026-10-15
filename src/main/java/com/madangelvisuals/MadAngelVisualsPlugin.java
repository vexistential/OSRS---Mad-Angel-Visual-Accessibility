package com.madangelvisuals;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
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
 * shadow mesh itself for readability.
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

	private final Set<NPC> madAngels = new HashSet<>();
	private final List<PendingDestination> pendingDestinations = new ArrayList<>();
	private final Set<GraphicsObject> trackedShadows = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Integer> loggedCaptureIds = new HashSet<>();
	/** Scene keys that were drawing fallback last render (transition logging). */
	private final Set<Long> fallbackActiveKeys = new HashSet<>();
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
		scanMadAngels();
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
	 * gfx has appeared yet (same-tile retarget / already standing). Once a shadow
	 * was seen for that pending, fallback must not reappear after the gfx ends.
	 */
	Collection<LocalPoint> getFallbackOutlineTiles()
	{
		Set<Long> nowActive = new HashSet<>();
		if (pendingDestinations.isEmpty())
		{
			logFallbackTransitions(nowActive);
			return Collections.emptyList();
		}

		List<LocalPoint> tiles = new ArrayList<>();
		for (PendingDestination pending : pendingDestinations)
		{
			// Fallback only before the first live shadow — never after it dies.
			if (pending.sawShadow || hasLiveShadowOnTile(pending.localPoint))
			{
				continue;
			}
			tiles.add(pending.localPoint);
			nowActive.add(sceneKey(pending.localPoint));
		}
		logFallbackTransitions(nowActive);
		return tiles;
	}

	int getFallbackOutlineRadius()
	{
		return lastFootprintRadius > 0 ? lastFootprintRadius : ShadowHull.DEFAULT_FALLBACK_RADIUS;
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

		WorldPoint target = projectile.getTargetPoint();
		// Event position is the projectile target LocalPoint (instance-safe).
		LocalPoint lp = event.getPosition();
		if (lp == null)
		{
			lp = localFromWorld(client, target);
		}

		if (!config.remapShadow())
		{
			return;
		}

		// Same projectile may hop stand→boss→stand. Clear stand pendings on boss
		// hops so a later stand destination (including the same tile) can re-arm.
		String bounceReason = bounceBackReason(projectile);
		if (bounceReason != null)
		{
			clearAllPendings("bounce");
			log.info("SKIP_BOUNCE reason={} target={} local={} targetActor={} proj={}",
				bounceReason, target, lp, projectile.getTargetActor(),
				System.identityHashCode(projectile));
			return;
		}

		if (lp == null)
		{
			log.info("Could not resolve LocalPoint for bomb target {}", target);
			return;
		}

		// Dedupe: only one stand tile at a time (ignore projectile identity — refs can churn).
		if (hasPendingAt(lp))
		{
			return;
		}

		armStandDestination(projectile, target, lp, "projectile");
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

		logCapture(go);

		// Known bomb shadows are authoritative for the stand tile. Snap the single
		// pending onto that gfx so adjacent hops still get a full outline even when
		// ProjectileMoved lagged on the previous tile.
		if (isShadowSpotanim(go.getId()) && (hasLiveBombProjectile() || !pendingDestinations.isEmpty()))
		{
			PendingDestination pending = findPendingFor(lp, go.getLevel());
			if (pending == null)
			{
				Projectile bomb = findLiveBombProjectile();
				WorldPoint target = bomb != null ? bomb.getTargetPoint() : WorldPoint.fromLocalInstance(client, lp);
				armStandDestination(bomb, target, lp, "shadow-snap");
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
		if (pending == null)
		{
			return;
		}

		if (shouldTrack(go, pending))
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
				LocalPoint lp = go.getLocation();
				log.info("DROP_SHADOW gfx={} scene=({}, {})",
					go.getId(),
					lp != null ? lp.getSceneX() : -1,
					lp != null ? lp.getSceneY() : -1);
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
					log.info("CLEAR_PENDING count=1 cause=ttl scene=({}, {})",
						pending.localPoint.getSceneX(),
						pending.localPoint.getSceneY());
					pendingIt.remove();
					continue;
				}
			}
			logCaptureScan(pending.localPoint);
			tryTrackShadowAt(pending.localPoint);
		}

		if (pendingDestinations.isEmpty())
		{
			loggedCaptureIds.clear();
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
			if (pending == null)
			{
				continue;
			}

			logCapture(go);

			if (shouldTrack(go, pending))
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

		// Loom usually spawns with the throw. Accept unknown destination gfx only
		// in that early window so later slash-dust on the stand tile is ignored.
		int age = client.getGameCycle() - pending.startCycle;
		return age >= 0 && age <= EARLY_LOOM_CYCLES;
	}

	private void trackShadow(GraphicsObject go, PendingDestination pending)
	{
		boolean added = trackedShadows.add(go);
		pending.sawShadow = true;
		markSawShadowOnTile(go.getLocation());
		lastFootprintRadius = ShadowHull.footprintRadius(go);
		if (!added)
		{
			return;
		}
		LocalPoint lp = go.getLocation();
		log.info("TRACK gfx={} scene=({}, {}) z={} radius={}",
			go.getId(),
			lp != null ? lp.getSceneX() : -1,
			lp != null ? lp.getSceneY() : -1,
			go.getZ(),
			lastFootprintRadius);
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
	 * retargets cannot leave dual fallback circles (projectile identity is not
	 * reliable across hops).
	 */
	private void armStandDestination(@Nullable Projectile projectile, @Nullable WorldPoint target,
		LocalPoint lp, String cause)
	{
		clearAllPendings("retarget-" + cause);
		pendingDestinations.add(new PendingDestination(projectile, target, lp, client.getGameCycle()));
		log.info("ARM cause={} target={} scene=({}, {}) remaining={} targetActor={} proj={} pendings={}",
			cause,
			target,
			lp.getSceneX(),
			lp.getSceneY(),
			projectile != null ? projectile.getRemainingCycles() : -1,
			projectile != null ? projectile.getTargetActor() : null,
			projectile != null ? System.identityHashCode(projectile) : 0,
			pendingDestinations.size());
		logCaptureScan(lp);
		tryTrackShadowAt(lp);
	}

	private void clearAllPendings(String cause)
	{
		int cleared = pendingDestinations.size();
		if (cleared == 0)
		{
			return;
		}
		pendingDestinations.clear();
		log.info("CLEAR_PENDING count={} cause={}", cleared, cause);
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

	private void logCaptureScan(LocalPoint localPoint)
	{
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
			logCapture(go);
		}
	}

	private void logCapture(GraphicsObject go)
	{
		if (!loggedCaptureIds.add(go.getId()))
		{
			return;
		}
		LocalPoint lp = go.getLocation();
		log.info("Shadow capture gfx id={} shadowCandidate={} scene=({}, {}) plane={} z={}",
			go.getId(),
			isShadowSpotanim(go.getId()),
			lp != null ? lp.getSceneX() : -1,
			lp != null ? lp.getSceneY() : -1,
			go.getLevel(),
			go.getZ());
	}

	/**
	 * Bounce-back toward the Mad Angel. Only {@code targetActor} counts — footprint
	 * checks false-positive when the stand tile lies under the multi-tile NPC
	 * (especially short adjacent hops near the angel).
	 *
	 * @return reason string for logging, or null if not a bounce-back
	 */
	@Nullable
	private String bounceBackReason(Projectile projectile)
	{
		Actor targetActor = projectile.getTargetActor();
		if (targetActor instanceof NPC && MadAngelNpcs.isMadAngel((NPC) targetActor))
		{
			return "targetActor";
		}
		return null;
	}

	private void logFallbackTransitions(Set<Long> nowActive)
	{
		for (Long key : nowActive)
		{
			if (!fallbackActiveKeys.contains(key))
			{
				log.info("FALLBACK_ON scene=({}, {})", (int) (key >> 32), key.intValue());
			}
		}
		for (Long key : fallbackActiveKeys)
		{
			if (!nowActive.contains(key))
			{
				log.info("FALLBACK_OFF scene=({}, {})", (int) (key >> 32), key.intValue());
			}
		}
		fallbackActiveKeys.clear();
		fallbackActiveKeys.addAll(nowActive);
	}

	private static long sceneKey(LocalPoint lp)
	{
		return (((long) lp.getSceneX()) << 32) | (lp.getSceneY() & 0xffffffffL);
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
		trackedShadows.clear();
		pendingDestinations.clear();
		madAngels.clear();
		loggedCaptureIds.clear();
		fallbackActiveKeys.clear();
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

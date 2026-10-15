# Significant fallback checkpoint

**Commit:** `a055e9d` — *Project shadow outline as a floor footprint, not a 3D silhouette.*

This is a known-good baseline for the Mad Angel Exploding Light assistive visual:

- Floor-parallel shadow outline (model footprint → `localToCanvas` at tile height → convex hull stroke)
- In-place shadow recolor
- Bounce-back skip
- No full-tile fill, no `ModelOutlineRenderer`, no shared-model `RuneLiteObject` clones

If later refinements regress visibility or orientation, reset behavior to this checkpoint before experimenting further.

**Same-tile follow-up (post-MVP):** Stand destinations must re-arm when the same projectile hops stand→boss→stand (including the same tile). Draw a floor-circle fallback for pending stand tiles that have no live shadow gfx yet.

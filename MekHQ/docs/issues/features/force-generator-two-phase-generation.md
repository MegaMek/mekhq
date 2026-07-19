# Force Generator: Two-Phase Generation (Combat -> Support-from-TOE)

**Status:** Design approved; ready to implement.
**Scope:** MekHQ (Command Generator dialog + generation pipeline). Reuses the MegaMek preview
include/exclude work already landed on `Implement-Force-Generator-in-MekHQ`.

## Problem

Today `CompanyGenerator.applyToCampaign` generates combat units and their support (personnel + support
vehicles) in a single pass on Accept. Two consequences:

1. There is no way to build a force incrementally or to add to an existing command later (e.g. drop in
   a Battle Armor company). A second run would bolt on a whole second force **and** a duplicate support
   command (second HQ, duplicate kitchens/medical/security).
2. Support is sized to a single generation pass rather than to the final committed force, so tuning
   (kitchen count, HR strain) is hard to reason about.

## Key enabling fact

`SupportPersonnelCalculator.compute(Campaign)` is already a **pure function over campaign state** - it
sizes support demand to the campaign's *current* force composition, not to the generation pass. The
support-vehicle capacity calculators (`SupportUnitGenerator`) also read `campaign` (active personnel,
combatant counts). So "generate support from the TOE" is essentially how the math already works; the
work is to **decouple** support generation into its own step and make it **reconcile against existing
support**.

## Approved decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Combat commit | **Accumulate** multiple rolls in the preview, then one **Assign** commits all included units |
| 2 | UI flow | **Repurpose Accept** into two steps: Assign-to-TOE, then a post-commit Support step |
| 3 | Replace scope | Wipe **only generator-created** units/personnel (tagged), never manually-added ones |
| 4 | Top-up model | **Category totals**: recompute total demand per category, subtract existing supply, add the shortfall |

## Workflow (player-facing)

1. **Build combat forces.** Roll a force into the preview tree; include/exclude nodes to pick what you
   want. Roll again to accumulate more (battalion, then a BA company, then a support lance).
2. **Assign to TOE.** One action commits the *included* combat units into the campaign TOE (formations
   + units + crews). No support is generated yet. Generated units are tagged as generator-created.
3. **Prompt: "Generate support forces from the TOE now?"** Yes runs the support step against the
   committed TOE; No leaves the command combat-only (support can be generated later).

Re-opening with forces already in the TOE, Assign offers **Add to existing / Replace / Cancel**. "Add"
is the normal additive path; "Replace" first removes previously generated (tagged) units + support.

## Architecture

Split `CompanyGenerator.applyToCampaign` into two independently-callable entry points; the existing
one-shot Accept becomes "call both in sequence":

- `commitCombatForces(campaign, descriptor, options)` - walk the filtered `ForceDescriptor`,
  materialize combat units/crews into the TOE (current Stage 4-7). Tags each created unit/person as
  generator-created. Stops before support.
- `generateSupportFromToe(campaign, options)` - current Stage 7e+ logic
  (`SupportPersonnelGenerator`, `SupportPersonnelToTOE`, `SupportUnitGenerator`) run against current
  campaign state, made **reconciling** (see below). Callable standalone.

### Support reconciliation (the one new algorithm)

`generateSupportFromToe` computes **demand - existing supply** per category:

- **Demand** = `SupportPersonnelCalculator.compute(campaign)` (techs, medics, admins/HR, astechs) plus
  the support-vehicle capacity calcs (canteens, MASH, salvage, logistics, security).
- **Supply** = count of support already present (existing techs, medics, admins, astechs; existing
  canteens, MASH trucks, etc.).
- **Generate** = `max(0, demand - supply)` per category.

This makes the step **idempotent and additive**: fresh command -> full support; run again after adding
a BA company -> only the shortfall (perhaps +1 canteen, a few techs, one HR admin). No duplicate HQ or
kitchens.

## Folding in the two tuning items

- **HR strain starts at 0 (approved):** admin demand is bumped until HR capacity >= headcount, i.e.
  until `RetirementDefectionTracker.getHRStrainModifier(campaign) == 0` for the committed TOE. Because
  support is computed from the final TOE, a newly generated command shows no HR-strain penalty.
- **Kitchen capacity (open tuning):** canteen count = `ceil(personnelNeedingKitchen / capacityPerCanteen)
  - existingCanteens`. Capacity-per-canteen rule still to be confirmed (candidate: 1 kitchen / 150 per
  canteen instead of the unit's 2 / 300, so the count reads more intuitively).

## Generator-created tagging

Both the combat-commit and support steps tag created units/personnel as generator-created (mechanism
TBD - a unit/person flag or a dedicated tag). Used by (a) Replace to remove only generated content, and
(b) support Supply counting to distinguish generated support from player-added.

## Incremental build order

Each step is independently shippable and testable:

1. **Split pipeline** into `commitCombatForces` + `generateSupportFromToe` (behavior-preserving; Accept
   calls both). Regression: one-shot Accept produces the same result as before.
2. **Reconciliation** (`demand - supply`) in the support step -> idempotent/additive. Regression:
   running support twice adds nothing the second time; adding combat then support tops up correctly.
3. **Tuning:** HR-to-zero; kitchen capacity rule.
4. **UI:** Assign -> support prompt; then the Add/Replace/Cancel warning on re-generation.

## Open / future considerations

- Generator-created tagging mechanism (flag vs tag) and save-game persistence.
- Replace: confirm it removes tagged support formations cleanly (empty-formation pruning already exists
  in `ForceDescriptorWalker`).
- Top-up currently by category totals; per-formation attribution is a possible future refinement.
- Interaction with the preview include/exclude: excluded combat nodes are already skipped at commit, so
  they never reach the support demand.

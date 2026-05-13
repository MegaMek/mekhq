/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.companyGeneration.ratgen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Single entry point for the ratgen-driven starting-force pipeline.
 *
 * <p>Composes the helpers in this package — {@link RulesetEngineBootstrap},
 * {@link ForceDescriptorWalker}, {@link MultiCrewAssembler}, {@link CrewDescriptorAdapter},
 * {@link RankAssigner} — into the 8-step pipeline described in
 * {@code docs/plans/force-generator-company-generation.md} (megamek repo). Phase 1 lands steps 1-7
 * (bootstrap → buildDescriptor → processRoot → walk → personnel → units → tree) but defers the
 * polish stage (parts, spares, finances, contracts, naming) until the static helpers are extracted
 * from {@link mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator}.</p>
 *
 * <p>This class is not yet wired into the public dialog. It is reachable only from the dev-gated
 * {@code RULESET_BASED} branch and from unit tests.</p>
 */
public final class CompanyGenerator {

    private static final MMLogger LOGGER = MMLogger.create(CompanyGenerator.class);

    // Single-thread daemon executor used by the addNewUnit watchdog. Scheduled tasks fire 5s after
    // each addNewUnit call begins; if addNewUnit returns first, the task is cancelled. If it hangs,
    // the task wins the race and dumps interesting thread stacks so the deadlock site is captured
    // in the log without the user having to grab a manual thread dump.
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CompanyGen-Watchdog");
        t.setDaemon(true);
        return t;
    });

    private CompanyGenerator() {
        // utility entry point
    }

    /**
     * Dumps stack traces for threads relevant to the force-generation pipeline (worker pool, EDT,
     * Swing Timer). Called by the {@link #WATCHDOG} when an {@code addNewUnit} call exceeds the
     * watchdog threshold; logs the worker thread (parked inside the hanging call) and the EDT
     * (which the worker may be waiting on) so a deadlock can be identified directly from the log.
     */
    private static void dumpInterestingThreads(String chassis, String model, long elapsedMs) {
        LOGGER.warn("[CompanyGen][Watchdog] addNewUnit hung >{}ms on chassis='{}' model='{}'; dumping interesting thread stacks",
              elapsedMs, chassis, model);
        Thread.getAllStackTraces().forEach((thread, frames) -> {
            String name = thread.getName();
            if (name.startsWith("SwingWorker") || name.startsWith("AWT-EventQueue") || name.contains("Timer")) {
                StringBuilder sb = new StringBuilder();
                sb.append("\n--- Thread '").append(name).append("' state=").append(thread.getState()).append(" ---");
                for (StackTraceElement frame : frames) {
                    sb.append("\n    at ").append(frame);
                }
                LOGGER.warn("[CompanyGen][Watchdog]{}", sb);
            }
        });
    }

    /**
     * Runs the ratgen pipeline end-to-end and applies the result to the given campaign. Returns the
     * descriptor tree the engine built so callers can inspect the structure (count leaves, log the
     * hierarchy, drive verification checks).
     *
     * @param campaign the target {@link Campaign}; receives the generated Formations, Units, and Persons
     * @param options  the user's {@link CompanyGenerationOptions}; the
     *                 {@link CompanyGenerationOptions#getForceDescriptorSnapshot()} block supplies the
     *                 ratgen inputs
     * @return the generated {@link ForceDescriptor} tree, or {@code null} if generation failed at the
     *         engine layer
     */
    public static ForceDescriptor generate(Campaign campaign, CompanyGenerationOptions options) {
        return generate(campaign, options, null);
    }

    /**
     * Same as {@link #generate(Campaign, CompanyGenerationOptions)} but accepts a
     * {@link Ruleset.ProgressListener} the engine and the rest of the pipeline use to surface status
     * updates. Pass {@code null} to suppress all progress callbacks (the default behavior of the
     * two-arg overload).
     *
     * <p>The listener is called from a background thread (typically a SwingWorker), so any UI work
     * triggered from it must be dispatched onto the EDT.</p>
     */
    public static ForceDescriptor generate(Campaign campaign, CompanyGenerationOptions options,
          Ruleset.ProgressListener listener) {
        long startedAt = System.currentTimeMillis();
        LOGGER.info("[CompanyGen][Pipeline]==================================================");
        LOGGER.info("[CompanyGen][Pipeline]CompanyGenerator.generate() START");
        if (listener != null) {
            listener.updateProgress(0.0, "Preparing generation parameters...");
        }
        ForceDescriptorSnapshot snap = options.getForceDescriptorSnapshot();

        // Stage 0: anchor inputs that the snapshot shouldn't be allowed to override.
        //
        // * Year is always the current campaign year. The user already chose the campaign date when
        //   they set up the campaign; asking again on a sub-panel risks divergence. Whatever the
        //   embedded ForceGeneratorOptionsView shows in its year field is informational only.
        //
        // * Faction falls back to the legacy CompanyGenerationOptions faction picker when the snapshot
        //   is still at its constructor default ("IS"). The embedded panel writes a non-default value
        //   on OK, in which case this is a no-op.
        snap.setYear(campaign.getGameYear());
        if ("IS".equals(snap.getFaction()) && options.getSpecifiedFaction() != null) {
            String legacyFactionCode = options.getSpecifiedFaction().getShortName();
            if (legacyFactionCode != null && !legacyFactionCode.isBlank()) {
                snap.setFaction(legacyFactionCode);
            }
        }
        LOGGER.info("[CompanyGen][Pipeline]Stage 0: anchored snapshot -> faction={} year={} (campaign year) echelon={} unitType={}",
              snap.getFaction(), snap.getYear(), snap.getEchelon(), snap.getUnitType());

        LOGGER.info("[CompanyGen][Pipeline]snapshot: faction={} year={} echelon={} unitType={} rating={} experience={} weightClass={} augmented={} sizeMod={} dropshipPct={} jumpshipPct={} cargo={} flags={} roles={}",
              snap.getFaction(), snap.getYear(), snap.getEchelon(), snap.getUnitType(),
              snap.getRating(), snap.getExperience(), snap.getWeightClass(),
              snap.isAugmented(), snap.getSizeMod(),
              snap.getDropshipPct(), snap.getJumpshipPct(), snap.getCargo(),
              snap.getFlags(), snap.getRoles());

        // 1. Bootstrap MegaMek-side state for the target year.
        LOGGER.info("[CompanyGen][Pipeline]Stage 1: bootstrap engine state");
        if (listener != null) {
            listener.updateProgress(0.0, "Loading factions and rulesets...");
        }
        RulesetEngineBootstrap.ensureLoaded(snap.getYear());

        // 2. Build a fresh ForceDescriptor from the snapshot. The Force Generator panel does this
        // server-side via buildForceDescriptor(); we mirror its inputs here so we never depend on the
        // panel being instantiated.
        LOGGER.info("[CompanyGen][Pipeline]Stage 2: build root ForceDescriptor from snapshot");
        ForceDescriptor fd = new ForceDescriptor();
        fd.setTopLevel(true);
        fd.setFaction(snap.getFaction());
        fd.setYear(snap.getYear());
        if (snap.getEchelon() != null) {
            fd.setEchelon(snap.getEchelon());
        }
        if (snap.getUnitType() != null) {
            fd.setUnitType(snap.getUnitType());
        }
        if (snap.getRating() != null) {
            fd.setRating(snap.getRating());
        }
        if (snap.getExperience() != null) {
            fd.setExperience(snap.getExperience());
        }
        if (snap.getWeightClass() != null) {
            fd.setWeightClass(snap.getWeightClass());
        }
        fd.setAugmented(snap.isAugmented());
        if (snap.getSizeMod() != null) {
            fd.setSizeMod(snap.getSizeMod());
        }
        fd.setDropshipPct(snap.getDropshipPct());
        LOGGER.info("[CompanyGen][Pipeline]  built fd: faction={} year={} echelon={} unitType={} rating={} weightClass={}",
              fd.getFaction(), fd.getYear(), fd.getEchelon(), fd.getUnitType(),
              fd.getRating(), fd.getWeightClass());

        // 3. Run the engine. Null listener is safe per Ruleset.processRoot's internal guards.
        LOGGER.info("[CompanyGen][Pipeline]Stage 3: Ruleset.processRoot()");
        if (listener != null) {
            listener.updateProgress(0.0, "Building force structure...");
        }
        long t0 = System.currentTimeMillis();
        Ruleset ruleset = Ruleset.findRuleset(fd);
        LOGGER.info("[CompanyGen][Pipeline]  Ruleset.findRuleset({}) resolved to ruleset for faction={}",
              fd.getFaction(), ruleset.getFaction());
        ruleset.processRoot(fd, listener);
        LOGGER.info("[CompanyGen][Pipeline]  Ruleset.processRoot() -> {}ms", System.currentTimeMillis() - t0);

        // 4-7. Walk the resulting tree; for each leaf, materialize a Unit, attach a crew, and place
        // the unit under the current Formation.
        LOGGER.info("[CompanyGen][Pipeline]Stage 4-7: walk tree, materialize Units + crews into Formations");
        if (listener != null) {
            listener.updateProgress(0.0, "Materializing units and crews...");
        }
        Formation root = campaign.getFormations();
        LOGGER.info("[CompanyGen][Pipeline]  campaign root Formation: id={} name={}",
              root == null ? "null" : root.getId(),
              root == null ? "null" : root.getName());
        int[] leafCount = { 0 };
        int[] skippedNoEntity = { 0 };
        int[] skippedAddFailed = { 0 };
        long[] stageStartNanos = { System.nanoTime() };
        ForceDescriptorWalker.walk(fd, campaign, root, (leaf, parent) -> {
            long leafStart = System.nanoTime();
            String parentInfo = parent == null ? "null"
                  : ("id=" + parent.getId() + " name='" + parent.getName() + "'");
            Entity entity = leaf.getEntity();
            if (entity == null) {
                LOGGER.warn("[CompanyGen][Leaf] SKIPPED (no entity): name={} unitType={} faction={} parent={}",
                      leaf.parseName(), leaf.getUnitType(), leaf.getFaction(), parentInfo);
                skippedNoEntity[0]++;
                return;
            }
            String entityChassis = entity.getChassis();
            String entityModel = entity.getModel();
            LOGGER.info("[CompanyGen][Leaf] ENTER chassis='{}' model='{}' unitType={} weight={} parent={} thread={}",
                  entityChassis, entityModel, entity.getUnitType(), entity.getWeight(),
                  parentInfo, Thread.currentThread().getName());

            LOGGER.info("[CompanyGen][Leaf][AddUnit] BEFORE campaign.addNewUnit chassis='{}' model='{}'",
                  entityChassis, entityModel);
            long addUnitStart = System.nanoTime();
            ScheduledFuture<?> watchdogTask = WATCHDOG.schedule(
                  () -> dumpInterestingThreads(entityChassis, entityModel,
                        (System.nanoTime() - addUnitStart) / 1_000_000),
                  5, TimeUnit.SECONDS);
            Unit unit;
            try {
                unit = campaign.addNewUnit(entity, false, 0);
            } finally {
                watchdogTask.cancel(false);
            }
            long afterAddUnitNanos = System.nanoTime();
            long addUnitMs = (afterAddUnitNanos - addUnitStart) / 1_000_000;
            LOGGER.info("[CompanyGen][Leaf][AddUnit] AFTER campaign.addNewUnit unit={} elapsed={}ms",
                  unit == null ? "null" : unit.getId(), addUnitMs);
            if (unit == null) {
                LOGGER.warn("[CompanyGen][Leaf] SKIPPED (addNewUnit failed): chassis='{}' model='{}'",
                      entityChassis, entityModel);
                skippedAddFailed[0]++;
                return;
            }

            LOGGER.info("[CompanyGen][Leaf][CrewAssemble] BEFORE MultiCrewAssembler.assemble unit={} crewDescriptor={}",
                  unit.getId(), leaf.getCo() == null ? "null" : "present");
            long assembleStart = System.nanoTime();
            java.util.List<Person> crew = MultiCrewAssembler.assemble(unit, leaf.getCo(), campaign,
                  /* overrideName */ true);
            long afterAssembleNanos = System.nanoTime();
            long assembleMs = (afterAssembleNanos - assembleStart) / 1_000_000;
            LOGGER.info("[CompanyGen][Leaf][CrewAssemble] AFTER MultiCrewAssembler.assemble crewSize={} elapsed={}ms",
                  crew.size(), assembleMs);

            if (!crew.isEmpty()) {
                LOGGER.info("[CompanyGen][Leaf][Rank] BEFORE RankAssigner.apply commander='{}'",
                      crew.get(0).getFullName());
                long rankStart = System.nanoTime();
                RankAssigner.apply(leaf.getCo(), crew.get(0));
                long rankMs = (System.nanoTime() - rankStart) / 1_000_000;
                LOGGER.info("[CompanyGen][Leaf][Rank] AFTER RankAssigner.apply elapsed={}ms", rankMs);
            }

            // Use the canonical Campaign API instead of parent.addUnit(uuid). The bare addUnit(uuid)
            // only updates the Formation's unit list; the Unit's formationId back-reference stays at
            // FORMATION_NONE, so UnitTableModel's Formation column and any caller of
            // Campaign.getFormation(unit.getFormationId()) sees nothing. addUnitToFormation sets
            // unit.setFormationId(id), pushes the assignment through AssignmentLogger, and fires
            // OrganizationChangedEvent — its subscribers (BriefingTab, TOETab) only do
            // ActionScheduler.schedule() which wraps Timer.restart() and is thread-safe.
            LOGGER.info("[CompanyGen][Leaf] BEFORE addUnitToFormation parent={} unit={}", parentInfo, unit.getId());
            campaign.addUnitToFormation(unit, parent.getId());
            LOGGER.info("[CompanyGen][Leaf] AFTER addUnitToFormation unit.formationId={}", unit.getFormationId());
            leafCount[0]++;
            long leafTotalMs = (System.nanoTime() - leafStart) / 1_000_000;
            // Warn on individual leaves that take more than 500ms — that's usually the sign of a
            // pathological RATGenerator selection or a slow Entity construction. Useful for spotting
            // hung-looking generation runs.
            if (leafTotalMs > 500) {
                LOGGER.warn("[CompanyGen][Leaf] leaf #{} SLOW: {}ms total (addUnit={}ms assemble={}ms) chassis='{}' model='{}'",
                      leafCount[0], leafTotalMs, addUnitMs, assembleMs, entityChassis, entityModel);
            }
            // Surface progress every 5 units to keep the dialog feeling alive AND to give the
            // log a heartbeat. Earlier batching at 25 was too coarse — a regiment with 36 leaves
            // would only get one mid-progress update, looking frozen to the user.
            if (listener != null && leafCount[0] % 5 == 0) {
                long elapsedSec = (System.nanoTime() - stageStartNanos[0]) / 1_000_000_000;
                LOGGER.info("[CompanyGen][Leaf][Progress] BEFORE listener.updateProgress count={} elapsedSec={}",
                      leafCount[0], elapsedSec);
                listener.updateProgress(0.0,
                      String.format("Materializing units and crews... (%d created, %ds elapsed)",
                            leafCount[0], elapsedSec));
                LOGGER.info("[CompanyGen][Leaf][Progress] AFTER listener.updateProgress");
            }
            LOGGER.info("[CompanyGen][Leaf] EXIT leaf #{} chassis='{}' model='{}' crew={} parent={} totalMs={}",
                  leafCount[0], entityChassis, entityModel, crew.size(), parentInfo, leafTotalMs);
        });

        LOGGER.info("[CompanyGen][Pipeline]Stage 4-7 summary: {} leaves placed, {} skipped (no entity), {} skipped (addNewUnit failed)",
              leafCount[0], skippedNoEntity[0], skippedAddFailed[0]);

        // 7b. Apply layered formation icons to every node in the campaign's Formation tree. Honors
        // the four formation-icon toggles on the options; bails cleanly if generation is disabled
        // or the formation-icon image directory is unavailable.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7b: apply layered formation icons");
        if (listener != null) {
            listener.updateProgress(0.0, "Applying formation icons...");
        }
        FormationIconBuilder.applyIcons(campaign.getFormations(), campaign, options);

        // 7c. Tree-aware rank assignment. Walks the Formation tree post-order and assigns each
        // node's commander the officer rank matching their FormationLevel (Lt → Lance, Capt →
        // Company, Major → Battalion, …). Non-officer combat crew get Sergeant-equivalent; support
        // crew get Corporal-equivalent. Gated on isAutomaticallyAssignRanks.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7c: tree-aware rank assignment");
        if (listener != null) {
            listener.updateProgress(0.0, "Assigning ranks...");
        }
        RulesetRankAssigner.apply(campaign, options);

        // 8. Polish stage (parts, spares, finances, contracts, naming) — wired into existing
        // AbstractCompanyGenerator helpers in a follow-up commit. Phase 1 stops here so the
        // tree-walk integration can be verified end-to-end against a Mek-only scenario.
        LOGGER.info("[CompanyGen][Pipeline]Stage 8: polish (parts/finance/contract) DEFERRED in Phase 1");

        LOGGER.info("[CompanyGen][Pipeline]CompanyGenerator.generate() DONE in {}ms",
              System.currentTimeMillis() - startedAt);
        LOGGER.info("[CompanyGen][Pipeline]==================================================");
        return fd;
    }
}

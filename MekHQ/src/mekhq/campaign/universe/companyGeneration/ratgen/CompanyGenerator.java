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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.market.PartsInUseManager;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
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

    /**
     * Result of a generation run: the engine's descriptor tree plus the flat list of {@link Person}s
     * the pipeline created during materialization. The list is used by post-generation steps that
     * need to iterate every fresh hire — e.g. setting the founder flag, generating callsigns, or
     * counting combatants for the alt-medical spare-personnel top-up.
     *
     * @param descriptor       the descriptor tree returned by {@link Ruleset#processRoot}, or
     *                         {@code null} if the engine layer failed
     * @param generatedPersons every Person added to the campaign by this generation, in the order
     *                         they were created (leaf order)
     */
    public record Result(@Nullable ForceDescriptor descriptor, List<Person> generatedPersons) {
    }

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
     * @return the generated {@link Result} bundling the descriptor tree and the flat list of Persons
     *         the pipeline created (the descriptor is {@code null} if the engine layer failed)
     */
    public static Result generate(Campaign campaign, CompanyGenerationOptions options) {
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
    public static Result generate(Campaign campaign, CompanyGenerationOptions options,
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
        String snapFactionBefore = snap.getFaction();
        String specifiedFactionCode = options.getSpecifiedFaction() == null
              ? "null"
              : options.getSpecifiedFaction().getShortName();
        String campaignFactionCode = campaign.getFaction() == null
              ? "null"
              : campaign.getFaction().getShortName();
        LOGGER.info("[CompanyGen][Pipeline][Faction] Stage 0 inputs: snap.faction='{}' options.specifiedFaction='{}' campaign.faction='{}' isUseSpecifiedFactionToAssignRanks={}",
              snapFactionBefore, specifiedFactionCode, campaignFactionCode,
              options.isUseSpecifiedFactionToAssignRanks());
        if ("IS".equals(snap.getFaction()) && options.getSpecifiedFaction() != null) {
            String legacyFactionCode = options.getSpecifiedFaction().getShortName();
            if (legacyFactionCode != null && !legacyFactionCode.isBlank()) {
                snap.setFaction(legacyFactionCode);
                LOGGER.info("[CompanyGen][Pipeline][Faction] Stage 0 swap: snap.faction '{}' -> '{}' (sourced from options.specifiedFaction)",
                      snapFactionBefore, legacyFactionCode);
            }
        } else {
            LOGGER.info("[CompanyGen][Pipeline][Faction] Stage 0 no swap (snap.faction='{}' specifiedFaction={})",
                  snap.getFaction(),
                  options.getSpecifiedFaction() == null ? "null" : specifiedFactionCode);
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
        // Flat accumulator of every Person the leaf walker creates. Stage 7d consumes this for
        // founder / callsign flags and the dialog hands it to processBonusUnitsBasedOnCampaignOptions
        // so the alt-medical spare-personnel branch can count combatants without re-walking the tree.
        List<Person> generatedPersons = new ArrayList<>();
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
            List<Person> crew = MultiCrewAssembler.assemble(unit, leaf.getCo(), campaign,
                  /* overrideName */ true);
            long afterAssembleNanos = System.nanoTime();
            long assembleMs = (afterAssembleNanos - assembleStart) / 1_000_000;
            LOGGER.info("[CompanyGen][Leaf][CrewAssemble] AFTER MultiCrewAssembler.assemble crewSize={} elapsed={}ms",
                  crew.size(), assembleMs);
            generatedPersons.addAll(crew);

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
        // Company, Major → Battalion, …). Non-officer combat crew get Sergeant-equivalent; any
        // support crew already attached to a Unit at this point get Corporal-equivalent. Gated on
        // isAutomaticallyAssignRanks.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7c: tree-aware rank assignment");
        if (listener != null) {
            listener.updateProgress(0.0, "Assigning ranks...");
        }
        Person rootCommander = RulesetRankAssigner.apply(campaign, options);

        // 7e. Support personnel: techs, doctors, administrators, plus astech and medic assistants.
        // Reads per-role coverage % and skill level from the Setup tab, scales the canonical CamOps
        // demand from SupportPersonnelCalculator, and creates the resulting Persons (or astech/medic
        // pool counts) via SupportPersonnelGenerator. Each generated support Person already has its
        // rank set by the generator — Stage 7c only ranks Persons in the Formation tree, and
        // support staff are free-floating campaign personnel.
        //
        // Runs before Stage 7d so the founder/callsign flags below sweep across combat AND support
        // staff in one pass.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7e: support personnel generation");
        if (listener != null) {
            listener.updateProgress(0.0, "Generating support personnel...");
        }
        SupportPersonnelGenerator.Result supportResult = SupportPersonnelGenerator.generate(campaign, options);
        generatedPersons.addAll(supportResult.generatedPersons());

        // 7e (continued). Assign techs to units using the Setup tab's three-slot sort grid (Pilot
        // Rank / Unit Weight / Pilot Skill, each with its own direction). Gated on
        // isAssignTechsToUnits; pulls only from the techs SupportPersonnelGenerator just created
        // so we don't steal a pre-existing campaign tech from another duty.
        SupportPersonnelAssigner.assign(campaign, options, supportResult);

        // 7d. Personnel flags driven by the Setup tab toggles: commander flag on the top-formation
        // officer, founder flag on every fresh hire (combat + support after the 7e merge above),
        // and random callsigns for non-Clan MekWarriors (support staff don't have the MEKWARRIOR
        // primary role so they're naturally skipped). These are pure Person-state mutations with
        // no algorithmic logic, so they live in the pipeline rather than in a dedicated helper.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7d: personnel flags");
        if (listener != null) {
            listener.updateProgress(0.0, "Applying personnel flags...");
        }
        applyPersonnelFlags(campaign, options, generatedPersons, rootCommander);

        // 8. Spare-parts warehouse stock-up. Uses the same PartsInUseManager the daily warehouse
        // and ongoing auto-logistics rely on, so the starting inventory is consistent with the
        // user's ongoing stocking policy: each part type's stocking percentage comes from the
        // CampaignOptions.getAutoLogistics*() values that the Spares tab writes into. Finance
        // and contract polish remain deferred.
        LOGGER.info("[CompanyGen][Pipeline]Stage 8: spare-parts warehouse stock-up");
        if (listener != null) {
            listener.updateProgress(0.0, "Stocking spare parts warehouse...");
        }
        stockSpareParts(campaign);

        LOGGER.info("[CompanyGen][Pipeline]CompanyGenerator.generate() DONE in {}ms",
              System.currentTimeMillis() - startedAt);
        LOGGER.info("[CompanyGen][Pipeline]==================================================");
        return new Result(fd, generatedPersons);
    }

    /**
     * Stage 7d: applies the Setup tab's personnel-flag toggles to the generation result.
     *
     * <ul>
     *   <li>{@code isAssignCompanyCommanderFlag} → {@link Person#setCommander(boolean)} on the
     *       commander {@link RulesetRankAssigner} promoted at the campaign-root formation.</li>
     *   <li>{@code isAssignFounderFlag} → {@link Person#setFounder(boolean)} on every Person this
     *       generation created.</li>
     *   <li>{@code isAssignMekWarriorsCallSigns} → {@link Person#setCallsign(String)} from
     *       {@link RandomCallsignGenerator} for every primary-role MekWarrior, skipped in Clan
     *       campaigns (Clan MekWarriors get their bloodname instead of a fixed-wing-style callsign).</li>
     * </ul>
     */
    private static void applyPersonnelFlags(Campaign campaign, CompanyGenerationOptions options,
          List<Person> generatedPersons, @Nullable Person rootCommander) {
        if (options.isAssignCompanyCommanderFlag() && rootCommander != null) {
            rootCommander.setCommander(true);
            LOGGER.info("[CompanyGen][Pipeline][Flags] commander flag set on '{}'", rootCommander.getFullName());
        }
        int founderCount = 0;
        int callsignCount = 0;
        boolean applyFounder = options.isAssignFounderFlag();
        boolean applyCallsigns = options.isAssignMekWarriorsCallSigns() && !campaign.isClanCampaign();
        RandomCallsignGenerator callsigns = applyCallsigns ? RandomCallsignGenerator.getInstance() : null;
        for (Person person : generatedPersons) {
            if (applyFounder) {
                person.setFounder(true);
                founderCount++;
            }
            if (applyCallsigns && person.getPrimaryRole() == PersonnelRole.MEKWARRIOR) {
                person.setCallsign(callsigns.generate());
                callsignCount++;
            }
        }
        LOGGER.info("[CompanyGen][Pipeline][Flags] founder={} callsigns={} (clanCampaign={})",
              founderCount, callsignCount, campaign.isClanCampaign());
    }

    /**
     * Stage 8: GM-stocks the warehouse with spare parts based on the campaign's auto-logistics
     * percentages. Mirrors what the WarehouseTab does on a daily refresh: builds a
     * {@link PartInUse} set for every part type the current force depends on, then asks
     * {@link PartsInUseManager#stockUpPartsInUseGM} to add enough of each to meet the per-type
     * target percentage. The percentages themselves come from {@code CampaignOptions.getAutoLogistics*}
     * — the same values that drive ongoing auto-logistics restocking during play, written by the
     * Spares tab's spinners.
     *
     * <p>Setting all percentages to 0 produces an empty warehouse with no shopping list churn,
     * matching the legacy {@code PartGenerationMethod.DISABLED} behavior.</p>
     */
    private static void stockSpareParts(Campaign campaign) {
        long start = System.nanoTime();
        PartsInUseManager partsInUseManager = new PartsInUseManager(campaign);
        // ignoreMothballedUnits=true matches WarehouseTab's daily refresh: at generation time
        // nothing is mothballed yet, but keep the call shape consistent with the rest of the
        // codebase. isResupply=false skips the resupply-specific prohibited-unit-type filter.
        // ignoreSparesUnderQuality=QUALITY_A accepts any quality already on hand as inventory
        // toward the target.
        Set<PartInUse> partsInUse = partsInUseManager.getPartsInUse(true, false, PartQuality.QUALITY_A);
        partsInUseManager.stockUpPartsInUseGM(partsInUse);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        LOGGER.info("[CompanyGen][Pipeline][Spares] reviewed {} distinct part types; elapsed={}ms",
              partsInUse.size(), elapsedMs);
    }
}

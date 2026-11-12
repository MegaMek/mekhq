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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nullable;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ratgenerator.C3NetworkConfigurator;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.ContractExpenseProjection;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialTerm;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.market.PartsInUseManager;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CargoShipGenerator;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.EnhancedImagingAugmentor;
import mekhq.campaign.universe.commandGeneration.LiftTopUp;
import mekhq.campaign.universe.commandGeneration.ManeiDominiAugmentor;
import mekhq.campaign.universe.commandGeneration.SupportCapability;
import mekhq.campaign.universe.commandGeneration.SupportCarrierReconciler;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.campaign.utilities.AutomatedTechAssignments;

/**
 * Single entry point for the ratgen-driven Command Generator pipeline.
 *
 * <p>Composes the helpers in this package — {@link RulesetEngineBootstrap},
 * {@link ForceDescriptorWalker}, {@link MultiCrewAssembler}, {@link CrewDescriptorAdapter},
 * {@link RankAssigner} — into the pipeline described in
 * {@code docs/plans/force-generator-company-generation.md} (megamek repo): bootstrap →
 * buildDescriptor → processRoot → walk → personnel → units → tree → spares → starting cash. The
 * contract polish stage remains deferred.</p>
 *
 * <p>The pipeline is split into a pure roll and campaign materialization, giving two consumer
 * patterns:</p>
 * <ul>
 *   <li><b>Player command</b> (the Command Designer): roll — via the embedded Force Generator
 *       preview or {@link #rollCommand} — then {@link #applyToCampaign} and
 *       {@link #generateSupportFromToe} materialize Units, Persons, and Formations into the
 *       campaign TOE.</li>
 *   <li><b>OpFor</b> (future): seed a {@link ForceDescriptorSnapshot} with the enemy faction and
 *       year, {@link #rollCommand}, then {@link #collectEntities} - the crewed entities feed a
 *       {@code BotForce} without creating any campaign state.</li>
 * </ul>
 */
public final class CommandGenerator {

    /**
     * Result of a generation run: the engine's descriptor tree plus the flat list of {@link Person}s
     * the pipeline created during materialization. The list is used by post-generation steps that
     * count the force's own people - e.g. the medical reserve, which is sized per combat role. Steps
     * that apply to everyone the build hired (founder flags, callsigns) use
     * {@link #personsAddedSince} instead, which also catches vehicle and ship crews.
     *
     * @param descriptor       the descriptor tree returned by {@link Ruleset#processRoot}, or
     *                         {@code null} if the engine layer failed
     * @param generatedPersons the combat crews (and, in a single-shot build, the support staff) this
     *                         generation created, in the order they were created (leaf order)
     */
    public record Result(@Nullable ForceDescriptor descriptor, List<Person> generatedPersons) {
    }

    private static final MMLogger LOGGER = MMLogger.create(CommandGenerator.class);

    /** A starting loan's terms: interest rate in percent, length in years, and collateral in percent. */
    private static final int STARTING_LOAN_RATE = 15;
    private static final int STARTING_LOAN_YEARS = 2;
    private static final int STARTING_LOAN_COLLATERAL = 100;
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CommandGenerator";

    /** How many undercrewed units the diagnostic names before it summarises the rest. */
    private static final int UNDERCREWED_UNITS_NAMED_IN_LOG = 20;

    // Single-thread daemon executor used by the addNewUnit watchdog. Scheduled tasks fire 5s after
    // each addNewUnit call begins; if addNewUnit returns first, the task is cancelled. If it hangs,
    // the task wins the race and dumps interesting thread stacks so the deadlock site is captured
    // in the log without the user having to grab a manual thread dump.
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CompanyGen-Watchdog");
        t.setDaemon(true);
        return t;
    });

    private CommandGenerator() {
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
                StringBuilder summary = new StringBuilder();
                summary.append("\n--- Thread '").append(name).append("' state=").append(thread.getState()).append(" ---");
                for (StackTraceElement frame : frames) {
                    summary.append("\n    at ").append(frame);
                }
                LOGGER.warn("[CompanyGen][Watchdog]{}", summary);
            }
        });
    }

    /**
     * Rolls a command from the given snapshot without touching any campaign: bootstraps the engine
     * for the snapshot's year, builds the root {@link ForceDescriptor}, and rolls the force via
     * {@link Ruleset#processRoot}. Each leaf of the returned tree carries a fully crewed
     * {@link Entity}.
     *
     * <p>This is the shared pure stage of the pipeline. The player path materializes the roll into
     * the campaign with {@link #applyToCampaign}; an OpFor consumer instead supplies an enemy
     * faction/year in the snapshot and harvests the roll with {@link #collectEntities} - it never
     * touches the campaign-mutating stages. Callers are responsible for anchoring the snapshot's
     * faction and year before rolling (the Command Designer seeds them from the campaign; an OpFor
     * caller seeds them from the scenario's enemy).</p>
     *
     * @param snapshot     the roll inputs (faction, year, echelon, unit type, rating, experience,
     *                 weight class, size modifier, dropship percentage)
     * @param listener progress listener for status updates, or {@code null} for none; called from
     *                 the rolling thread, so any UI work it triggers must be dispatched onto the EDT
     *
     * @return the rolled descriptor tree, its leaves carrying crewed entities
     */
    public static ForceDescriptor rollCommand(ForceDescriptorSnapshot snapshot,
          @Nullable Ruleset.ProgressListener listener) {
        if (listener != null) {
            listener.updateProgress(0.0, "Preparing generation parameters...");
        }
        LOGGER.info("[CompanyGen][Pipeline]snapshot: faction={} year={} echelon={} unitType={} rating={} experience={} weightClass={} augmented={} sizeMod={} dropshipPct={} jumpshipPct={} cargoPct={} flags={} roles={}",
              snapshot.getFaction(), snapshot.getYear(), snapshot.getEchelon(), snapshot.getUnitType(),
              snapshot.getRating(), snapshot.getExperience(), snapshot.getWeightClass(),
              snapshot.isAugmented(), snapshot.getSizeMod(),
              snapshot.getDropshipPct(), snapshot.getJumpshipPct(), snapshot.getCargoPct(),
              snapshot.getFlags(), snapshot.getRoles());

        // 1. Bootstrap MegaMek-side state for the target year.
        LOGGER.info("[CompanyGen][Pipeline]Stage 1: bootstrap engine state");
        if (listener != null) {
            listener.updateProgress(0.0, "Loading factions and rulesets...");
        }
        RulesetEngineBootstrap.ensureLoaded(snapshot.getYear());

        // 2. Build a fresh ForceDescriptor from the snapshot. The Force Generator panel does this
        // server-side via buildForceDescriptor(); we mirror its inputs here so we never depend on the
        // panel being instantiated.
        LOGGER.info("[CompanyGen][Pipeline]Stage 2: build root ForceDescriptor from snapshot");
        ForceDescriptor forceDescriptor = new ForceDescriptor();
        forceDescriptor.setTopLevel(true);
        forceDescriptor.setFaction(snapshot.getFaction());
        forceDescriptor.setYear(snapshot.getYear());
        if (snapshot.getEchelon() != null) {
            forceDescriptor.setEchelon(snapshot.getEchelon());
        }
        if (snapshot.getUnitType() != null) {
            forceDescriptor.setUnitType(snapshot.getUnitType());
        }
        if (snapshot.getRating() != null) {
            forceDescriptor.setRating(snapshot.getRating());
        }
        if (snapshot.getExperience() != null) {
            forceDescriptor.setExperience(snapshot.getExperience());
        }
        if (snapshot.getWeightClass() != null) {
            forceDescriptor.setWeightClass(snapshot.getWeightClass());
        }
        forceDescriptor.setInfantryClass(snapshot.getInfantryClass());
        forceDescriptor.setAugmented(snapshot.isAugmented());
        if (snapshot.getSizeMod() != null) {
            forceDescriptor.setSizeMod(snapshot.getSizeMod());
        }
        forceDescriptor.setDropshipPct(snapshot.getDropshipPct());
        LOGGER.info("[CompanyGen][Pipeline]  built forceDescriptor: faction={} year={} echelon={} unitType={} rating={} weightClass={}"
                    + " infantryClass={}",
              forceDescriptor.getFaction(), forceDescriptor.getYear(), forceDescriptor.getEchelon(), forceDescriptor.getUnitType(),
              forceDescriptor.getRating(), forceDescriptor.getWeightClass(), forceDescriptor.getInfantryClass());

        // 3. Run the engine. Null listener is safe per Ruleset.processRoot's internal guards.
        LOGGER.info("[CompanyGen][Pipeline]Stage 3: Ruleset.processRoot()");
        if (listener != null) {
            listener.updateProgress(0.0, "Building force structure...");
        }
        long processRootStartedAt = System.currentTimeMillis();
        Ruleset ruleset = Ruleset.findRuleset(forceDescriptor);
        LOGGER.info("[CompanyGen][Pipeline]  Ruleset.findRuleset({}) resolved to ruleset for faction={}",
              forceDescriptor.getFaction(), ruleset.getFaction());
        ruleset.processRoot(forceDescriptor, listener);
        LOGGER.info("[CompanyGen][Pipeline]  Ruleset.processRoot() -> {}ms", System.currentTimeMillis() - processRootStartedAt);
        return forceDescriptor;
    }

    /**
     * Harvests the crewed {@link Entity} objects from a rolled descriptor tree - every leaf that is
     * {@link ForceDescriptor#isIncluded() included} and carries an entity, in tree order (sub-forces
     * before attached forces at each node). Excluded leaves (struck out in the Command Designer
     * preview) are skipped, matching the commit walker's rule.
     *
     * <p>This is the OpFor-side consumer of {@link #rollCommand}: rolling with an enemy faction/year
     * and collecting the entities yields exactly the {@code List<Entity>} shape a
     * {@code BotForce} takes, with each entity already crewed by the engine - no campaign Person,
     * Unit, or Formation is created. The pipeline deliberately exposes these seams as plain static
     * methods rather than a materializer interface: the two consumers (campaign TOE commit, entity
     * harvest) share no state, only the rolled tree. If a configurable materialization policy is
     * ever needed, introduce an interface over the rolled descriptor - not an abstract class.</p>
     *
     * @param descriptor the rolled descriptor (or subtree) to harvest
     *
     * @return the included leaves' entities, in tree order; never {@code null}
     */
    public static List<Entity> collectEntities(ForceDescriptor descriptor) {
        List<Entity> entities = new ArrayList<>();
        collectEntitiesInto(descriptor, entities);
        return entities;
    }

    private static void collectEntitiesInto(ForceDescriptor descriptor, List<Entity> entities) {
        // A descriptor with an entity is a unit whether or not anything is nested under it: a carrier is
        // generated with the fighters it carries beneath it, and it is still a ship to harvest.
        if (descriptor.isIncluded() && descriptor.getEntity() != null) {
            entities.add(descriptor.getEntity());
        }
        for (ForceDescriptor child : descriptor.getSubForces()) {
            collectEntitiesInto(child, entities);
        }
        for (ForceDescriptor child : descriptor.getAttached()) {
            collectEntitiesInto(child, entities);
        }
    }

    /**
     * Stages 4-8: walk the rolled {@code descriptor}, materialize Units + crews into Formations, assign
     * ranks, generate support personnel and vehicles, apply formation icons and personnel flags, and
     * stock the spare-parts warehouse. Operates on a rolled descriptor (from the Command Designer
     * preview or {@link #rollCommand}), so the commit materializes the exact force the player saw.
     */
    public static Result applyToCampaign(Campaign campaign, CommandGenerationOptions options,
          ForceDescriptor forceDescriptor, Ruleset.ProgressListener listener) {
        return applyToCampaign(campaign, options, forceDescriptor, listener, true);
    }

    /**
     * As {@link #applyToCampaign(Campaign, CommandGenerationOptions, ForceDescriptor,
     * Ruleset.ProgressListener)}, but {@code generateSupport} controls whether support personnel and
     * vehicles are generated in the same pass. Pass {@code false} to commit only the combat force to
     * the TOE (phase one of two-phase generation); {@link #generateSupportFromToe} can then be run
     * later against the committed TOE.
     *
     * @param generateSupport {@code true} to also generate support (the one-shot behavior),
     *                        {@code false} to commit combat only
     */
    public static Result applyToCampaign(Campaign campaign, CommandGenerationOptions options,
          ForceDescriptor forceDescriptor, Ruleset.ProgressListener listener, boolean generateSupport) {
        // Snapshot the roster before anything is created so the founder flags go to only this build's people.
        Set<UUID> preExistingPersonIds = snapshotPersonnelIds(campaign);

        // 4-7. Walk the resulting tree; for each leaf, materialize a Unit, attach a crew, and place
        // the unit under the current Formation.
        LOGGER.info("[CompanyGen][Pipeline]Stage 4-7: walk tree, materialize Units + crews into Formations");
        if (listener != null) {
            listener.updateProgress(0.0, "Materializing units and crews...");
        }
        Formation root = campaign.getPlayerForce().getFormations();
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
        // Names for the mirrored Formations follow the generating faction's convention (declared in its
        // ruleset), with the player's Formation Naming Method supplying the alphabet wherever that
        // convention calls for one. Seeding the namer with every formation already in the campaign is
        // what stops a repeat build from reusing names the previous one took.
        List<String> existingFormationNames = campaign.getPlayerForce().getAllFormations().stream()
              .map(Formation::getName)
              .toList();
        FormationNamer namer = new FormationNamer(options.getForceNamingMethod(), existingFormationNames);
        namer.setAlwaysNumberRegiments(options.isAlwaysNumberRegiments());
        // The Unit built for each unit descriptor, so stage 7a can put the units the tree nests under a ship
        // aboard it once every unit exists.
        Map<ForceDescriptor, Unit> unitsByDescriptor = new IdentityHashMap<>();
        // Which descriptor each Formation mirrors, so the rank pass can read levels and commanders from the
        // roll rather than guessing them from depth.
        Map<Formation, ForceDescriptor> descriptorsByFormation = new IdentityHashMap<>();
        ForceDescriptorWalker.walk(forceDescriptor, campaign, root, namer, (leaf, parent) -> {
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
            LOGGER.debug("[CompanyGen][Leaf] ENTER chassis='{}' model='{}' unitType={} weight={} parent={} thread={}",
                  entityChassis, entityModel, entity.getUnitType(), entity.getWeight(),
                  parentInfo, Thread.currentThread().getName());

            LOGGER.debug("[CompanyGen][Leaf][AddUnit] BEFORE campaign.addNewUnit chassis='{}' model='{}'",
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
            LOGGER.debug("[CompanyGen][Leaf][AddUnit] AFTER campaign.addNewUnit unit={} elapsed={}ms",
                  unit == null ? "null" : unit.getId(), addUnitMs);
            if (unit == null) {
                LOGGER.warn("[CompanyGen][Leaf] SKIPPED (addNewUnit failed): chassis='{}' model='{}'",
                      entityChassis, entityModel);
                skippedAddFailed[0]++;
                return;
            }

            LOGGER.debug("[CompanyGen][Leaf][CrewAssemble] BEFORE MultiCrewAssembler.assemble unit={} crewDescriptor={}",
                  unit.getId(), leaf.getCo() == null ? "null" : "present");
            long assembleStart = System.nanoTime();
            List<Person> crew = MultiCrewAssembler.assemble(unit, leaf.getCo(), campaign,
                  /* overrideName */ true);
            long afterAssembleNanos = System.nanoTime();
            long assembleMs = (afterAssembleNanos - assembleStart) / 1_000_000;
            LOGGER.debug("[CompanyGen][Leaf][CrewAssemble] AFTER MultiCrewAssembler.assemble crewSize={} elapsed={}ms",
                  crew.size(), assembleMs);
            generatedPersons.addAll(crew);

            if (!crew.isEmpty()) {
                LOGGER.debug("[CompanyGen][Leaf][Rank] BEFORE RankAssigner.apply commander='{}'",
                      crew.get(0).getFullName());
                long rankStart = System.nanoTime();
                RankAssigner.apply(leaf.getCo(), crew.get(0));
                long rankMs = (System.nanoTime() - rankStart) / 1_000_000;
                LOGGER.debug("[CompanyGen][Leaf][Rank] AFTER RankAssigner.apply elapsed={}ms", rankMs);
            }

            // Use the canonical Campaign API instead of parent.addUnit(uuid). The bare addUnit(uuid)
            // only updates the Formation's unit list; the Unit's formationId back-reference stays at
            // FORMATION_NONE, so UnitTableModel's Formation column and any caller of
            // Campaign.getFormation(unit.getFormationId()) sees nothing. addUnitToFormation sets
            // unit.setFormationId(id), pushes the assignment through AssignmentLogger, and fires
            // OrganizationChangedEvent — its subscribers (BriefingTab, TOETab) only do
            // ActionScheduler.schedule() which wraps Timer.restart() and is thread-safe.
            LOGGER.debug("[CompanyGen][Leaf] BEFORE addUnitToFormation parent={} unit={}", parentInfo, unit.getId());
            // A leaf with no parent formation is a force of one unit with no structure above it. It is
            // filed under the root formation rather than dropped or left to throw.
            Formation targetFormation = parent;
            if (targetFormation == null) {
                LOGGER.warn("[CompanyGen][Leaf] unit {} has no parent formation; filing it under '{}'",
                      unit.getId(), root.getName());
                targetFormation = root;
            }
            campaign.getPlayerForce().addUnitToFormation(unit, targetFormation.getId(), campaign);
            LOGGER.debug("[CompanyGen][Leaf] AFTER addUnitToFormation unit.formationId={}", unit.getFormationId());
            unitsByDescriptor.put(leaf, unit);
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
                LOGGER.debug("[CompanyGen][Leaf][Progress] BEFORE listener.updateProgress count={} elapsedSec={}",
                      leafCount[0], elapsedSec);
                listener.updateProgress(0.0,
                      String.format("Materializing units and crews... (%d created, %ds elapsed)",
                            leafCount[0], elapsedSec));
                LOGGER.debug("[CompanyGen][Leaf][Progress] AFTER listener.updateProgress");
            }
            LOGGER.debug("[CompanyGen][Leaf] EXIT leaf #{} chassis='{}' model='{}' crew={} parent={} totalMs={}",
                  leafCount[0], entityChassis, entityModel, crew.size(), parentInfo, leafTotalMs);
        }, (descriptor, formation) -> descriptorsByFormation.put(formation, descriptor));

        LOGGER.info("[CompanyGen][Pipeline]Stage 4-7 summary: {} leaves placed, {} skipped (no entity), {} skipped (addNewUnit failed)",
              leafCount[0], skippedNoEntity[0], skippedAddFailed[0]);

        // 7a. Ship transport. Everything the tree nests under a ship - the fighter complement the Carried
        // Fighter Complement option adds - is assigned to that ship, so the TO&E shows it aboard and the
        // scenario launcher loads it in game.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7a: ship transport assignment");
        ShipTransportAssigner.assign(forceDescriptor, unitsByDescriptor);

        // 7b. Apply layered formation icons to every node in the campaign's Formation tree. Honors
        // the four formation-icon toggles on the options; bails cleanly if generation is disabled
        // or the formation-icon image directory is unavailable.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7b: apply layered formation icons");
        if (listener != null) {
            listener.updateProgress(0.0, "Applying formation icons...");
        }
        FormationIconBuilder.applyIcons(campaign.getPlayerForce().getFormations(),
              campaign.getPlayerForce().getFaction(), campaign::getUnit,
              campaign.getPlayerForce().getSupportCommandFormation(), campaign.getGameYear(), options);

        // 7b2. The most skilled pilots take the seats in the leading lances, when asked. Before ranks,
        // so the commanders chosen next are chosen from where the pilots will actually sit.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7b2: seating the most skilled pilots in the leading lances");
        PilotSkillSorter.apply(campaign, options, unitIdsOf(unitsByDescriptor.values()));

        // 7b3. Bloodnames, for the Clan warriors who earn one. Before the ranks, so the picks that follow can
        // put the Bloodnamed in command; the roll carries the force's calibre but no rank bonus, since nobody
        // holds a rank yet. This is the force's only roll: the one MekHQ makes when it creates a Clan warrior
        // is cleared by the crew adapter.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7b3: Bloodnames");
        assignBloodnames(campaign, options, generatedPersons);

        // 7c. Tree-aware rank assignment. Walks the Formation tree and assigns each node's commander
        // the officer rank matching their FormationLevel (Lt -> Lance, Capt -> Company, Major ->
        // Battalion, ...), choosing by skill when the Officer Selection options ask for it. Non-officer
        // combat crew get Sergeant-equivalent; any support crew already attached to a Unit at this
        // point get Corporal-equivalent. Gated on isAutomaticallyAssignRanks.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7c: tree-aware rank assignment");
        if (listener != null) {
            listener.updateProgress(0.0, "Assigning ranks...");
        }
        RulesetRankAssigner.Guidance guidance = rankGuidance(descriptorsByFormation, unitsByDescriptor);
        RulesetRankAssigner.Result ranks = RulesetRankAssigner.applyAndReport(campaign, options, guidance);
        Person rootCommander = ranks.rootCommander();

        // 7c2. Officers get the skills that come with the post, when Generate Captains is on.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7c2: officer skill increases");
        OfficerSkillBooster.apply(options, ranks);

        // 7e. Support: generate support personnel and standalone support vehicles sized to the
        // campaign's current force, and organize the support staff into the TOE. Extracted into
        // generateSupportFromToe so support can be (re)generated against the committed TOE, and gated
        // on generateSupport so combat can be committed on its own (phase one of two-phase generation).
        if (generateSupport) {
            // generateSupportFromToe applies TOE icons to the support formations it creates, so no
            // separate re-decoration pass is needed here.
            generatedPersons.addAll(generateSupportFromToe(campaign, options, listener));
        }

        // 7d. Command flags: commander flag on the top-formation officer and the second-in-command below
        // them. The roster-wide flags (founder, callsigns) wait for the end of the build, when every
        // person it hires - support staff, vehicle and ship crews included - exists.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7d: command flags");
        if (listener != null) {
            listener.updateProgress(0.0, "Applying personnel flags...");
        }
        applyCommandFlags(campaign, options, generatedPersons, rootCommander);

        // Manei Domini rank, class and cybernetics, for a Word of Blake Shadow Division. Runs here for
        // the same reason as bloodnames: implant availability is read off the person's rank.
        // Before either augmentation stage, since both read these and neither can be applied to
        // warriors afterwards.
        applyAugmentationRules(campaign, options);

        LOGGER.info("[CompanyGen][Pipeline] Stage 7f: Manei Domini augmentation (snapshot faction '{}')",
              options.getForceDescriptorSnapshot().getFaction());
        ManeiDominiAugmentor.augment(campaign,
              options.getForceDescriptorSnapshot().getFaction(), generatedPersons);

        // The Clans' own augmentation, unrelated to the Manei Domini and gated on its own rules. Runs
        // on the campaign's Persons rather than the descriptor's entity crews, because a unit's crew is
        // rebuilt from its people whenever it is reset and an implant written to the crew would be
        // lost with it.
        LOGGER.info("[CompanyGen][Pipeline] Stage 7f2: Clan enhanced imaging");
        EnhancedImagingAugmentor.augment(campaign,
              options.getForceDescriptorSnapshot().getFaction(), generatedPersons);

        // C3 and C3i networks. Generation picks units carrying the equipment but leaves them
        // unlinked, so a ComStar Level II arrived with six C3i sets and no network. The wiring is
        // MegaMek's, shared with its own force generator, and writes both the running net id and the
        // C3 UUIDs a campaign rebuilds from after a save. The units are the same Entity instances the
        // campaign wrapped - addNewUnit does not copy them - so wiring the descriptor wires the TOE.
        LOGGER.info("[CompanyGen][Pipeline] Stage 7g: C3 network configuration");
        C3NetworkConfigurator.configure(forceDescriptor);
        campaign.getPlayerForce().refreshNetworks(campaign.getGame());

        // 8-9. The build's tail - temporary crew, spares, cargo lift, roster flags and starting cash -
        // has to see everything the build adds, support included. The single-shot path has its support by
        // now; the two-phase Command Designer flow (generateSupport=false) runs the tail itself once its
        // support phase is done.
        if (generateSupport) {
            completeBuild(campaign, options, listener);
            List<Person> hired = personsAddedSince(campaign, preExistingPersonIds);
            applyRosterFlags(campaign, options, hired);
            processStartingCash(campaign, options);
        }

        LOGGER.info("[CompanyGen][Pipeline]CommandGenerator.applyToCampaign() DONE");
        return new Result(forceDescriptor, generatedPersons);
    }

    /**
     * The build's tail, run once every unit it creates exists: tops up the temporary crew pools, stocks
     * the spare-parts warehouse and provisions the cargo lift to haul it. Run after support generation,
     * so the support vehicles get their spares and the cargo ships are sized for them too.
     *
     * @param campaign the campaign being built into
     * @param options  the generation options, read for the cargo settings
     * @param listener optional progress listener; may be {@code null}
     */
    public static void completeBuild(Campaign campaign, CommandGenerationOptions options,
          @Nullable Ruleset.ProgressListener listener) {
        // 7f. Temporary crew. The assembler left the seats of any temporary-crew role empty behind a
        // single named crew member, so the pool has to be topped up to cover them or the generated
        // units arrive undercrewed.
        topUpTemporaryCrewPools(campaign);

        // 8. Spare-parts warehouse stock-up. Uses the same PartsInUseManager the daily warehouse
        // and ongoing auto-logistics rely on, so the starting inventory is consistent with the
        // user's ongoing stocking policy: each part type's stocking percentage comes from the
        // CampaignOptions.getAutoLogistics*() values that the Spares tab writes into.
        LOGGER.info("[CompanyGen][Pipeline]Stage 8: spare-parts warehouse stock-up");
        if (listener != null) {
            listener.updateProgress(0.0, "Stocking spare parts warehouse...");
        }
        stockSpareParts(campaign);

        // 8b. Cargo lift. Runs after the warehouse is stocked, because the tonnage the ships have to
        // haul is exactly what stocking just put in it, and before starting cash so the hulls' running
        // costs count toward it.
        LOGGER.info("[CompanyGen][Pipeline]Stage 8b: cargo lift");
        if (listener != null) {
            listener.updateProgress(0.0, "Provisioning cargo lift...");
        }
        generateCargoLift(campaign, options);
    }

    /**
     * Captures the IDs of everyone currently on the campaign's books. Take this snapshot before a build
     * starts so {@link #personsAddedSince} can find exactly the people that build hired.
     *
     * @param campaign the campaign whose roster is snapshotted
     *
     * @return the IDs of the people present before the build
     */
    public static Set<UUID> snapshotPersonnelIds(Campaign campaign) {
        Set<UUID> personIds = new HashSet<>();
        for (Person person : campaign.getPlayerForce().getHumanResources().getPersonnel()) {
            personIds.add(person.getId());
        }
        return personIds;
    }

    /**
     * Everyone on the books who was not there when {@code preExistingPersonIds} was taken: the combat
     * crews, the support staff, the support-vehicle and ship crews, and any spares a build hired.
     *
     * @param campaign             the campaign being built into
     * @param preExistingPersonIds the snapshot from {@link #snapshotPersonnelIds}, taken before the build
     *
     * @return the people the build added, in roster order
     */
    public static List<Person> personsAddedSince(Campaign campaign, Set<UUID> preExistingPersonIds) {
        List<Person> added = new ArrayList<>();
        for (Person person : campaign.getPlayerForce().getHumanResources().getPersonnel()) {
            if (!preExistingPersonIds.contains(person.getId())) {
                added.add(person);
            }
        }
        return added;
    }

    private static Set<UUID> unitIdsOf(Collection<Unit> units) {
        Set<UUID> unitIds = new HashSet<>();
        for (Unit unit : units) {
            unitIds.add(unit.getId());
        }
        return unitIds;
    }

    /**
     * Grants the support vehicles that are generated with their own crew rather than inside a support team. The
     * logistics convoy, canteen and security detail have no matching personnel section, so they are always granted
     * here. Recovery vehicles and MASH trucks join their section inside {@link SupportPersonnelToTOE#organize},
     * crewed from the generated staff, but only while support teams are switched on; with them off, no section is
     * built, so those vehicles are granted here too.
     *
     * @param campaign       the campaign the vehicles are granted to
     * @param supportFaction the faction the command is organised as: the campaign's own
     * @param autoAssignRanks whether the crews are given ranks, per Automatically Assign Ranks
     * @param namingMethod   the convention the command's formations are named with, so its support formations
     *                       read the same way its combat formations do
     */
    static void grantStandaloneSupportVehicles(Campaign campaign, Faction supportFaction, boolean autoAssignRanks,
          @Nullable ForceNamingMethod namingMethod) {
        for (SupportCapability capability : SupportCapability.values()) {
            if (!capability.isEnabled(campaign)) {
                LOGGER.info("[CompanyGen][SupportUnits] {}: switched off, nothing granted", capability);
                continue;
            }
            if (capability.joinsSection(campaign)) {
                LOGGER.info("[CompanyGen][SupportUnits] {}: joins the {} section, crewed from its own staff",
                      capability, capability.crewSection());
                continue;
            }
            LOGGER.info("[CompanyGen][SupportUnits] {}: generated standalone with its own crew", capability);
            SupportUnitGenerator.generate(capability, campaign, supportFaction, autoAssignRanks, namingMethod);
        }
    }

    /**
     * Stage 7e: generates support personnel (techs, doctors, administrators, plus astech and medic
     * assistants) and the standalone support vehicles a command gets for each enabled capability, and
     * organizes the support staff into the TOE. Sized to the campaign's current force composition via
     * {@link SupportPersonnelCalculator} and the {@link SupportUnitGenerator} capacity calculators, so
     * it can be run standalone against a committed TOE (phase two of two-phase generation).
     *
     * @param campaign the campaign whose current force the support is sized to
     * @param options  the generation options (per-role coverage %, skill levels, tech assignment)
     * @param listener optional progress listener; may be {@code null}
     *
     * @return the support {@link Person}s created (astech/medic pool hires are not individual Persons)
     */
    public static List<Person> generateSupportFromToe(Campaign campaign, CommandGenerationOptions options,
          Ruleset.ProgressListener listener) {
        LOGGER.info("[CompanyGen][Pipeline]Stage 7e: support personnel generation");
        if (listener != null) {
            listener.updateProgress(0.0, "Generating support personnel...");
        }
        // What the hangar holds before support is generated: only what this stage adds gets lift sized for it.
        Set<UUID> unitsBeforeSupport = snapshotHangarUnitIds(campaign);
        // The stage's own vehicles - flatbeds, canteens, recovery and MASH trucks - are generated below, after
        // the staff, but they need mechanics like any other vehicle. Count them into the demand now.
        Faction commandFaction = SupportPersonnelGenerator.resolveFaction(campaign);
        int vehiclesStillToCome = SupportUnitGenerator.vehiclesStillToGenerate(campaign, commandFaction);
        SupportPersonnelGenerator.Result supportResult =
              SupportPersonnelGenerator.generate(campaign, options, vehiclesStillToCome);

        // Organize the freshly generated support staff into the TOE. Each section (Maintenance /
        // Medical / Command) becomes infantry-style carrier units crewed by the staff, nested under a
        // Support Command formation. Crewing a carrier is separate from the setTech maintenance
        // assignment above, so techs still maintain the combat units.
        // Support teams are a campaign option: with it off the staff are still generated and hired, they simply stay
        // on the roster instead of being organized into carriers.
        if (SupportCarrierReconciler.isEnabled(campaign)) {
            SupportPersonnelToTOE.organize(campaign, supportResult.generatedPersons(),
                  commandFaction.isClan(), commandFaction);
        } else {
            LOGGER.info("[CompanyGen][SupportTOE] support teams are switched off; {} support person(s) stay unorganized",
                  supportResult.generatedPersons().size());
        }

        grantStandaloneSupportVehicles(campaign, commandFaction, options.isAutomaticallyAssignRanks(),
              options.getForceNamingMethod());

        // Assign techs to units through the same process as the Hangar's Quick Assign Maintenance Techs button.
        // Runs once every vehicle exists, the support stage's own included, so the flatbeds and the recovery
        // vehicles get their mechanics too.
        LOGGER.info("[CompanyGen][Pipeline][Assign] quick-assigning maintenance techs");
        AutomatedTechAssignments.handleTheAutomaticAssignmentOfUnmaintainedUnits(campaign, true);

        // Decorate the support formations created above with layered TOE icons. This must happen here
        // (not only at the tail of applyToCampaign) because the two-phase Command Designer flow calls
        // this method directly, after applyToCampaign already ran its icon pass with no support
        // formations in the tree - so without this call the support command commits with blank icons.
        // Appoint the senior staff - CMO, head technician, chief administrator. Runs
        // here rather than in stage 7d because the eligible people are exactly the support staff
        // generated above, and because the two-phase Command Designer flow reaches this method
        // directly; anywhere else and a design-then-generate-support build would commit with the posts
        // vacant.
        SeniorAppointmentAssigner.assign(campaign, supportResult.generatedPersons(),
              options.isAutomaticallyAssignRanks());

        // 7e2. The support sections are platoons and squads that need bays like any other unit, and no ship was
        // sized for them. Size lift for what this stage added, against the bays the hangar has free. Combat
        // units without a ship are left alone: their ship was struck out of the preview, and that stands.
        Set<UUID> unitsAddedBySupport = new HashSet<>(snapshotHangarUnitIds(campaign));
        unitsAddedBySupport.removeAll(unitsBeforeSupport);
        topUpLift(campaign, options, unitsAddedBySupport);

        LOGGER.info("[CompanyGen][Pipeline]Stage 7e: applying formation icons to support formations");
        FormationIconBuilder.applyIcons(campaign.getPlayerForce().getFormations(),
              campaign.getPlayerForce().getFaction(), campaign::getUnit,
              campaign.getPlayerForce().getSupportCommandFormation(), campaign.getGameYear(), options);

        logOrphanAudit(campaign);

        return supportResult.generatedPersons();
    }

    /**
     * Stage 7e2: adds the ships the command still needs once everything it will carry exists. Gated on the
     * DropShip percentage like the cargo lift: a command generated without DropShips hires its lift.
     */
    private static void topUpLift(Campaign campaign, CommandGenerationOptions options, Set<UUID> newUnitIds) {
        ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();
        if (snapshot == null) {
            return;
        }
        LOGGER.info("[CompanyGen][Pipeline]Stage 7e2: lift top-up for the support sections");
        Set<UUID> unitIdsBefore = snapshotHangarUnitIds(campaign);
        try {
            LiftTopUp.topUp(campaign, snapshot.getFaction(), snapshot.getYear(), snapshot.getRating(),
                  snapshot.getDropshipPct(), snapshot.getJumpshipPct(), newUnitIds);
        } catch (Exception exception) {
            LOGGER.error(exception, "[CompanyGen][LiftTopUp] lift top-up failed; the command keeps the ships it had");
        }
        rankShipsAddedSince(campaign, options, unitIdsBefore);
    }

    /**
     * Ranks the crews of the ships a stage added after the main rank pass (Stage 7c) has already run.
     *
     * @param campaign      the campaign the ships were added to
     * @param options       the generation options
     * @param unitIdsBefore the hangar's unit ids from before the stage ran
     */
    private static void rankShipsAddedSince(Campaign campaign, CommandGenerationOptions options,
          Set<UUID> unitIdsBefore) {
        List<Unit> added = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if (!unitIdsBefore.contains(unit.getId())) {
                added.add(unit);
            }
        }
        RulesetRankAssigner.rankLateShipCrews(campaign, options, added);
    }

    /**
     * Captures the IDs of every unit currently in the campaign's hangar, so a later stage can tell which units it
     * added.
     *
     * @param campaign the campaign whose hangar is snapshotted
     *
     * @return the IDs of the units present before the build
     */
    public static Set<UUID> snapshotHangarUnitIds(Campaign campaign) {
        Set<UUID> unitIds = new HashSet<>();
        for (Unit unit : campaign.getUnits()) {
            unitIds.add(unit.getId());
        }
        return unitIds;
    }

    /**
     * Stage 9 - starting cash: {@link CommandGenerationOptions#getStartingCashMonths()} months of the command's
     * running costs once it is built - salaries, maintenance, overhead, food and housing, and Hot Spots upkeep, each
     * only while the campaign charges it (see {@link ContractExpenseProjection#getMonthlyOffContractExpenses}) - and
     * never less than {@link CommandGenerationOptions#getMinimumStartingCash()}. The command itself is granted free.
     * The cash is credited as starting capital and reported in the Finances daily log.
     *
     * <p>With {@link CommandGenerationOptions#isStartingLoan()}, the starting cash is doubled: the command also takes
     * out a loan for the same amount, which it has to repay.</p>
     *
     * <p>The running costs are the whole campaign's, as they will be charged from the next day; in a new campaign
     * that is exactly the command just built.</p>
     *
     * @param campaign the campaign to credit
     * @param options  the generation options carrying the finance settings
     */
    public static void processStartingCash(Campaign campaign, CommandGenerationOptions options) {
        processStartingCash(campaign, options, ContractExpenseProjection.getMonthlyOffContractExpenses(campaign));
    }

    /**
     * As {@link #processStartingCash(Campaign, CommandGenerationOptions)}, with the command's monthly running costs
     * supplied rather than projected, so the cash arithmetic can be checked against a known figure.
     *
     * @param monthlyExpenses the command's running costs in an average month
     */
    static void processStartingCash(Campaign campaign, CommandGenerationOptions options, Money monthlyExpenses) {
        int months = options.getStartingCashMonths();
        Money monthlyCosts = monthlyExpenses.round();
        Money startingCash = monthlyCosts.multipliedBy(months).round();
        LOGGER.info("[CompanyGen][Pipeline]Stage 9: starting cash = {} month(s) of {} running costs -> {}",
              months, monthlyCosts.toAmountAndSymbolString(), startingCash.toAmountAndSymbolString());

        Money minimumStartingCash = Money.of(Math.max(0, options.getMinimumStartingCash()));
        boolean isFlooredToMinimum = !startingCash.isGreaterOrEqualThan(minimumStartingCash);
        if (isFlooredToMinimum) {
            startingCash = minimumStartingCash;
        }

        if (startingCash.isPositive()) {
            campaign.getPlayerForce().getFinances().credit(TransactionType.STARTING_CAPITAL,
                  campaign.getLocalDate(), startingCash,
                  getTextAt(RESOURCE_BUNDLE, "CommandGenerator.startingCapital.reason"));
        }

        if (isFlooredToMinimum) {
            campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CommandGenerator.startingCapital.reportMinimum",
                  startingCash.toAmountAndSymbolString()));
        } else {
            String basis = getFormattedTextAt(RESOURCE_BUNDLE, "CommandGenerator.startingCapital.basis.months",
                  months, monthlyCosts.toAmountAndSymbolString());
            campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CommandGenerator.startingCapital.report",
                  startingCash.toAmountAndSymbolString(), basis));
        }
        LOGGER.info("[CompanyGen][Pipeline]Stage 9: credited {} starting capital",
              startingCash.toAmountAndSymbolString());

        if (options.isStartingLoan() && startingCash.isPositive()) {
            // Campaign.addLoan credits the principal and reports the loan, so the cash doubles.
            campaign.addLoan(new Loan(startingCash, STARTING_LOAN_RATE, STARTING_LOAN_YEARS, FinancialTerm.MONTHLY,
                  STARTING_LOAN_COLLATERAL, campaign.getLocalDate()));
            LOGGER.info("[CompanyGen][Pipeline]Stage 9: starting loan of {} doubles the starting cash",
                  startingCash.toAmountAndSymbolString());
        }
    }

    /**
     * Post-generation diagnostic that finds members which never made it into the TOE, so a playtest
     * can locate them from megamek.log alone. Logs every hangar unit not attached to any formation (an
     * orphan unit, with its crew) and every active person not assigned to a unit (summarized by role,
     * since many - pooled astechs, unassigned admins - are legitimately unit-less). Purely diagnostic;
     * it mutates no state.
     *
     * @param campaign the campaign whose hangar and roster are audited
     */
    private static void logOrphanAudit(Campaign campaign) {
        List<Unit> orphanUnits = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if (unit.getFormationId() == Formation.FORMATION_NONE) {
                orphanUnits.add(unit);
            }
        }

        Map<PersonnelRole, Integer> unitlessByRole = new EnumMap<>(PersonnelRole.class);
        int unitlessCount = 0;
        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.getUnit() == null) {
                unitlessByRole.merge(person.getPrimaryRole(), 1, Integer::sum);
                unitlessCount++;
            }
        }

        LOGGER.info("[CompanyGen][OrphanAudit] {} unit(s) not in any formation; {} active person(s) not assigned to a unit",
              orphanUnits.size(), unitlessCount);

        for (Unit orphanUnit : orphanUnits) {
            List<String> crewNames = new ArrayList<>();
            for (Person crew : orphanUnit.getCrew()) {
                crewNames.add(crew.getFullName());
            }
            LOGGER.warn("[CompanyGen][OrphanAudit]   ORPHAN UNIT '{}' (id={}) attached to NO formation; crew=[{}]",
                  orphanUnit.getName(), orphanUnit.getId(),
                  crewNames.isEmpty() ? "none" : String.join(", ", crewNames));
        }
        if (!unitlessByRole.isEmpty()) {
            LOGGER.info("[CompanyGen][OrphanAudit]   unit-less active personnel by role (in the roster, not the TOE): {}",
                  unitlessByRole);
        }
    }

    /**
     * Stage 7d: flags the command's leadership. With {@code isAssignCompanyCommanderFlag} on, the
     * commander {@link RulesetRankAssigner} promoted at the campaign-root formation gets
     * {@link Person#setCommander(boolean)}, and the next in line the second-in-command flag.
     */
    private static void applyCommandFlags(Campaign campaign, CommandGenerationOptions options,
          List<Person> generatedPersons, @Nullable Person rootCommander) {
        if (options.isAssignCompanyCommanderFlag() && rootCommander != null) {
            rootCommander.setCommander(true);
            LOGGER.info("[CompanyGen][Pipeline][Flags] commander flag set on '{}'", rootCommander.getFullName());
            assignSecondInCommand(campaign, generatedPersons);
        }
    }

    /**
     * Applies the Setup tab's roster-wide toggles to everyone a build hired. Run once the build is
     * complete, so the support staff, the vehicle and ship crews and any medical reserve are included.
     *
     * <ul>
     *   <li>{@code isAssignFounderFlag} → {@link Person#setFounder(boolean)} on every person. Where the
     *       campaign also has {@code USE_FOUNDER_PLOT_ARMOR} set, each founder gains a point of Edge,
     *       matching what newborn founders receive in {@code AbstractProcreation}.</li>
     *   <li>{@code isAssignMekWarriorsCallSigns} → {@link Person#setCallsign(String)} from
     *       {@link RandomCallsignGenerator} for every primary-role MekWarrior who is not Clan
     *       personnel (Clan warriors carry a bloodname instead of a callsign). Judged per person rather
     *       than by the campaign's faction, so a Clan command built into a Mercenary campaign gets none.</li>
     * </ul>
     *
     * @param campaign the campaign being built into
     * @param options  the generation options carrying the toggles
     * @param hired    everyone the build hired, from {@link #personsAddedSince}
     */
    public static void applyRosterFlags(Campaign campaign, CommandGenerationOptions options, List<Person> hired) {
        int founderCount = 0;
        int callsignCount = 0;
        boolean applyFounder = options.isAssignFounderFlag();
        boolean applyCallsigns = options.isAssignMekWarriorsCallSigns();
        boolean applyFounderPlotArmor = campaign.getCampaignOptions().get(CampaignOption.USE_FOUNDER_PLOT_ARMOR);
        RandomCallsignGenerator callsigns = applyCallsigns ? RandomCallsignGenerator.getInstance() : null;
        for (Person person : hired) {
            if (applyFounder && !person.isFounder()) {
                person.setFounder(true);
                founderCount++;
                if (applyFounderPlotArmor) {
                    person.changeAttributeScore(SkillAttribute.EDGE, 1);
                }
            }
            if (applyCallsigns && (person.getPrimaryRole() == PersonnelRole.MEKWARRIOR)
                      && !person.isClanPersonnel()) {
                person.setCallsign(callsigns.generate());
                callsignCount++;
            }
        }
        LOGGER.info("[CompanyGen][Pipeline][Flags] founder={} callsigns={} across {} hired (founderPlotArmor={})",
              founderCount, callsignCount, hired.size(), applyFounderPlotArmor);
    }

    /**
     * Flags the executive officer of a generated command, so the campaign records a second-in-command
     * rather than re-deriving one every time it is asked.
     *
     * <p>Without the flag {@link ForceHumanResources#findTopCommanders} still produces an answer, but
     * it is inferred from whoever currently ranks highest - so it moves when people are promoted,
     * killed or hired. Flagging pins the appointment the way the commander's is pinned. The choice
     * itself is delegated to that same method so the generator does not invent a second, divergent
     * notion of who ranks next.</p>
     *
     * @param campaign         the campaign supplying the rank comparison rules
     * @param generatedPersons the people this generation created
     */
    private static void assignSecondInCommand(Campaign campaign, List<Person> generatedPersons) {
        Person existingSecondInCommand = campaign.getFlaggedSecondInCommand();
        if (existingSecondInCommand != null) {
            LOGGER.debug("[CompanyGen][Pipeline][Flags] second-in-command left as is; already held by '{}'",
                  existingSecondInCommand.getFullName());
            return;
        }

        Person[] topCommanders = ForceHumanResources.findTopCommanders(generatedPersons,
              campaign.getCampaignOptions(), campaign.getPlayerForce().isClanForce(), campaign.getLocalDate());
        Person secondInCommand = topCommanders[1];
        if (secondInCommand == null) {
            LOGGER.debug("[CompanyGen][Pipeline][Flags] second-in-command left vacant; the generated "
                  + "roster of {} offers no candidate below the commander", generatedPersons.size());
            return;
        }

        secondInCommand.setSecondInCommand(true);
        LOGGER.info("[CompanyGen][Pipeline][Flags] second-in-command flag set on '{}'",
              secondInCommand.getFullName());
    }

    /**
     * What the rank pass should know from the roll: each formation's level from its descriptor's echelon, and
     * the commander the engine designated for it, resolved to the person built for that crew.
     *
     * @param descriptorsByFormation the descriptor each built Formation mirrors
     * @param unitsByDescriptor      the unit built for each leaf descriptor
     *
     * @return the guidance for {@link RulesetRankAssigner#applyAndReport(Campaign, CommandGenerationOptions,
     *       RulesetRankAssigner.Guidance)}
     */
    static RulesetRankAssigner.Guidance rankGuidance(Map<Formation, ForceDescriptor> descriptorsByFormation,
          Map<ForceDescriptor, Unit> unitsByDescriptor) {
        Map<Formation, FormationLevel> levels = new IdentityHashMap<>();
        Map<Formation, Person> engineCommanders = new IdentityHashMap<>();
        for (Map.Entry<Formation, ForceDescriptor> entry : descriptorsByFormation.entrySet()) {
            Formation formation = entry.getKey();
            ForceDescriptor descriptor = entry.getValue();
            levels.put(formation, ForceDescriptorWalker.mapEchelonToFormationLevel(descriptor.getEchelon(),
                  descriptor.getFaction()));
            Person commander = builtCommanderOf(descriptor, unitsByDescriptor);
            if (commander != null) {
                engineCommanders.put(formation, commander);
            }
        }
        LOGGER.info("[CompanyGen][RankAssign] guidance from the roll: {} formation level(s), {} engine commander(s)",
              levels.size(), engineCommanders.size());
        return new RulesetRankAssigner.Guidance(levels, engineCommanders);
    }

    /**
     * The person built for the crew the engine designated as a formation's commander: the leaf beneath the
     * formation whose crew descriptor is the formation's own, resolved to its unit's commander.
     */
    private static @Nullable Person builtCommanderOf(ForceDescriptor formation,
          Map<ForceDescriptor, Unit> unitsByDescriptor) {
        CrewDescriptor designated = formation.getCo();
        if (designated == null) {
            return null;
        }
        ForceDescriptor leaf = leafCrewedBy(formation, designated);
        Unit unit = (leaf == null) ? null : unitsByDescriptor.get(leaf);
        return (unit == null) ? null : unit.getCommander();
    }

    private static @Nullable ForceDescriptor leafCrewedBy(ForceDescriptor node, CrewDescriptor crew) {
        boolean isLeaf = node.getSubForces().isEmpty() && node.getAttached().isEmpty();
        if (isLeaf) {
            return (node.getCo() == crew) ? node : null;
        }
        for (ForceDescriptor child : node.getSubForces()) {
            ForceDescriptor found = leafCrewedBy(child, crew);
            if (found != null) {
                return found;
            }
        }
        for (ForceDescriptor child : node.getAttached()) {
            ForceDescriptor found = leafCrewedBy(child, crew);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Awards the force its Bloodnames, as a share of its Clan warriors set by the force's calibre. The rule
     * and the numbers behind it are in {@link BloodnameQuota}; non-Clan personnel and anyone without a
     * phenotype are passed over, so support staff and Inner Sphere commands are untouched.
     *
     * @param campaign         the campaign the warriors belong to
     * @param options          the generation options, read for the force's experience level
     * @param generatedPersons every person this generation created
     */
    private static void assignBloodnames(Campaign campaign, CommandGenerationOptions options,
          List<Person> generatedPersons) {
        BloodnameQuota.award(campaign, options, generatedPersons);
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
     * <p>Setting all percentages to 0 produces an empty warehouse with no shopping list churn -
     * effectively disabling spare-part generation.</p>
     */
    /**
     * Stage 8b: provisions the cargo DropShips the command needs to haul its own warehouse, per the
     * Cargo Capacity percentage on the Force Generator tab.
     *
     * <p>Failures here are logged and swallowed: a command that cannot lift all of its spares is a
     * playable outcome the player can fix by buying a hull, whereas losing the whole build over it is
     * not.</p>
     */
    private static void generateCargoLift(Campaign campaign, CommandGenerationOptions options) {
        ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();
        if (snapshot == null) {
            LOGGER.debug("[CompanyGen][Cargo] no force snapshot; skipping cargo lift");
            return;
        }
        // A command generated with no DropShips owns no lift of any kind, so it gets no cargo hulls
        // either however high the cargo percentage is set. That is a real and playable starting
        // position - a unit that has to hire its transport - and the Cargo Summary still reports the
        // requirement, which now simply shows as a shortfall.
        if (snapshot.getDropshipPct() <= 0d) {
            LOGGER.info("[CompanyGen][Cargo] DropShip percentage is {}; generating no cargo hulls "
                        + "regardless of the {}% cargo setting (the command must hire its lift)",
                  snapshot.getDropshipPct(), snapshot.getCargoPct());
            return;
        }
        Set<UUID> unitIdsBefore = snapshotHangarUnitIds(campaign);
        try {
            CargoShipGenerator.Result result = CargoShipGenerator.generate(campaign,
                  snapshot.getFaction(), snapshot.getYear(), snapshot.getRating(),
                  snapshot.getCargoPct(), snapshot.getJumpshipPct());
            if (result.shortfallTons() > 0) {
                LOGGER.warn("[CompanyGen][Cargo] the command is {} tons short of hauling its own cargo",
                      result.shortfallTons());
            }
        } catch (Exception exception) {
            LOGGER.error(exception, "[CompanyGen][Cargo] cargo lift generation failed;"
                        + " the command keeps whatever cargo capacity it already had");
        }
        rankShipsAddedSince(campaign, options, unitIdsBefore);
    }

    /**
     * Writes the augmentation rules chosen on the Setup tab onto the campaign.
     *
     * <p>These are not generation settings - they belong to the campaign and to MegaMek's game
     * options - but they are chosen on the Setup tab because that is where the decision is made and
     * because neither rule can be applied to warriors after they are generated. Writing them here
     * makes the choice take effect for the generation that follows and, because both option sets are
     * saved with the campaign, hold for the saved game.</p>
     */
    // Package-private so the regression test can check the choice reaches the campaign.
    static void applyAugmentationRules(Campaign campaign, CommandGenerationOptions options) {
        if (!options.shouldApplyAugmentationRules()) {
            LOGGER.info("[CompanyGen][Pipeline] Stage 7e2: augmentation rules are hidden for the selected faction;"
                        + " leaving campaign settings unchanged");
            return;
        }
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean tracksImplants = options.isUseImplants();
        campaignOptions.set(CampaignOption.USE_IMPLANTS, tracksImplants);

        IOption neuralInterface = campaign.getGameOptions()
                                        .getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE);
        if (neuralInterface != null) {
            // With implants untracked there is nothing for the rule to act on, so it is written off
            // rather than left saying a rule is in play that cannot reach anyone.
            NeuralInterfaceMode mode = tracksImplants
                                             ? options.getNeuralInterfaceMode()
                                             : NeuralInterfaceMode.OFF;
            neuralInterface.setValue((mode == null) ? NeuralInterfaceMode.OFF.optionValue()
                                           : mode.optionValue());
        }
        LOGGER.info("[CompanyGen][Pipeline] Stage 7e2: augmentation rules written to the campaign -"
                    + " Use Implants={}, Pilot Implants='{}'",
              campaignOptions.get(CampaignOption.USE_IMPLANTS),
              (neuralInterface == null) ? "unavailable" : neuralInterface.stringValue());
    }

    /**
     * Stage 7f: crews the seats the assembler deliberately left empty on temporary-crew roles.
     *
     * <p>Where a role draws on the temporary crew pool the assembler puts one named person aboard and
     * leaves the rest of the seats empty, which is the roster the option exists to produce. Those seats
     * still have to be filled or the unit arrives undercrewed - a battle armour squad with one trooper
     * aboard reports four suits empty, and MegaMek marks the empty suits as having no one in them.</p>
     *
     * <p>Filling them takes both halves of MekHQ's own two-step: {@code fillTempCrewPoolForRole} sizes
     * the campaign pool to the shortfall, and {@code distributeTempCrewPoolToUnits} seats it. Sizing
     * alone leaves the crew in the pool and the units still empty.</p>
     *
     * <p>Which roles those are is asked of {@link ForceHumanResources#isBlobCrewEnabled}, the same
     * switch the rest of MekHQ reads, so a role added there is picked up here without change. Every
     * role is offered to it rather than only those already in the pool: a freshly generated campaign
     * has an empty pool, so reading the pool's keys finds nothing to fill and the units stay empty.</p>
     */
    // Package-private so the regression test can run the stage against a real campaign; nothing
    // outside this class calls it.
    static void topUpTemporaryCrewPools(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        int rolesFilled = 0;
        for (PersonnelRole role : PersonnelRole.values()) {
            if (!humanResources.isBlobCrewEnabled(role, campaignOptions)) {
                continue;
            }
            humanResources.fillTempCrewPoolForRole(campaign, campaignOptions, role);
            humanResources.distributeTempCrewPoolToUnits(campaign, campaignOptions, role);
            rolesFilled++;
            LOGGER.info("[CompanyGen][Pipeline][TempCrew] role {}: pool sized to {} and seated",
                  role, humanResources.getTempCrewPool(role));
        }
        if (rolesFilled == 0) {
            LOGGER.debug("[CompanyGen][Pipeline][TempCrew] no temporary crew roles enabled;"
                        + " every seat was filled with a named person");
        }
        reportUndercrewedUnits(campaign);
    }

    /**
     * Names any generated unit still short of a full crew, so a roster that looks wrong can be traced
     * from the log rather than by opening each unit.
     */
    private static void reportUndercrewedUnits(Campaign campaign) {
        List<String> undercrewed = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if ((unit.getEntity() != null) && !unit.isMothballed() && !unit.isFullyCrewed()) {
                undercrewed.add("%s (%d/%d)".formatted(unit.getName(), unit.getTotalCrewSize(),
                      unit.getFullCrewSize()));
            }
        }
        if (undercrewed.isEmpty()) {
            LOGGER.info("[CompanyGen][Pipeline][TempCrew] every generated unit is fully crewed");
            return;
        }
        // A whole command short of crew would otherwise put every unit on one line. The count is
        // always exact; only the naming is cut, and the line says by how much.
        int named = Math.min(undercrewed.size(), UNDERCREWED_UNITS_NAMED_IN_LOG);
        String tail = (named < undercrewed.size())
                            ? ", and %d more".formatted(undercrewed.size() - named) : "";
        LOGGER.warn("[CompanyGen][Pipeline][TempCrew] {} unit(s) left undercrewed: {}{}",
              undercrewed.size(), String.join(", ", undercrewed.subList(0, named)), tail);
    }

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

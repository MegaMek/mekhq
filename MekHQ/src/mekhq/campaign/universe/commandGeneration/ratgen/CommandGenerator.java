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

import java.time.LocalDate;
import java.util.ArrayList;
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

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ratgenerator.C3NetworkConfigurator;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.common.annotations.Nullable;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.enums.FinancialTerm;
import mekhq.campaign.market.PartsInUseManager;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CargoShipGenerator;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.EnhancedImagingAugmentor;
import mekhq.campaign.universe.commandGeneration.LiftTopUp;
import mekhq.campaign.universe.commandGeneration.ManeiDominiAugmentor;
import mekhq.campaign.universe.commandGeneration.SupportCarrierReconciler;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator;
import mekhq.campaign.universe.commandGeneration.TemporaryCrewRole;

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
     * need to iterate every fresh hire — e.g. setting the founder flag, generating callsigns, or
     * counting combatants for the alt-medical spare-personnel top-up.
     *
     * @param descriptor       the descriptor tree returned by {@link Ruleset#processRoot}, or
     *                         {@code null} if the engine layer failed
     * @param generatedPersons every Person added to the campaign by this generation, in the order
     *                         they were created (leaf order)
     * @param spareCosts       the value of the spare parts the build's warehouse stock-up added, for
     *                         the finance stage's pay-for debits
     * @param rolledUnitIds    the ids of the units the rolls produced - the combat units and the ships rolled
     *                         with them - which are the base of the percentage starting cash; what the build
     *                         adds afterwards (support vehicles, staff carriers, top-up and cargo ships) is not
     */
    public record Result(@Nullable ForceDescriptor descriptor, List<Person> generatedPersons,
          SpareCosts spareCosts, Set<UUID> rolledUnitIds) {
    }

    /**
     * The value of the spare parts the build's warehouse stock-up added, split by the categories the
     * pay-for finance toggles gate.
     *
     * @param parts      value of the general spare parts added
     * @param armour     value of the armor added
     * @param ammunition value of the ammunition added
     */
    public record SpareCosts(Money parts, Money armour, Money ammunition) {
        public static SpareCosts zero() {
            return new SpareCosts(Money.zero(), Money.zero(), Money.zero());
        }
    }

    private static final MMLogger LOGGER = MMLogger.create(CommandGenerator.class);
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
     * @param snap     the roll inputs (faction, year, echelon, unit type, rating, experience,
     *                 weight class, size modifier, dropship percentage)
     * @param listener progress listener for status updates, or {@code null} for none; called from
     *                 the rolling thread, so any UI work it triggers must be dispatched onto the EDT
     *
     * @return the rolled descriptor tree, its leaves carrying crewed entities
     */
    public static ForceDescriptor rollCommand(ForceDescriptorSnapshot snap,
          @Nullable Ruleset.ProgressListener listener) {
        if (listener != null) {
            listener.updateProgress(0.0, "Preparing generation parameters...");
        }
        LOGGER.info("[CompanyGen][Pipeline]snapshot: faction={} year={} echelon={} unitType={} rating={} experience={} weightClass={} augmented={} sizeMod={} dropshipPct={} jumpshipPct={} cargoPct={} flags={} roles={}",
              snap.getFaction(), snap.getYear(), snap.getEchelon(), snap.getUnitType(),
              snap.getRating(), snap.getExperience(), snap.getWeightClass(),
              snap.isAugmented(), snap.getSizeMod(),
              snap.getDropshipPct(), snap.getJumpshipPct(), snap.getCargoPct(),
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
        return fd;
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
          ForceDescriptor fd, Ruleset.ProgressListener listener) {
        return applyToCampaign(campaign, options, fd, listener, true);
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
          ForceDescriptor fd, Ruleset.ProgressListener listener, boolean generateSupport) {
        // Before the first unit is crewed: the crew assembler decides per unit whether a seat is a named
        // person or left for temporary crew, reading the campaign options at that moment.
        applyTemporaryCrewChoices(campaign, options);

        // Snapshot the hangar before any unit is created so the starting-cash stage can price only
        // the units this build adds (see processStartingCash).
        Set<UUID> preExistingUnitIds = snapshotHangarUnitIds(campaign);

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
        ForceDescriptorWalker.walk(fd, campaign, root, namer, (leaf, parent) -> {
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
            // A leaf with no parent formation is a force of one unit with no structure above it. It is
            // filed under the root formation rather than dropped or left to throw.
            Formation targetFormation = parent;
            if (targetFormation == null) {
                LOGGER.warn("[CompanyGen][Leaf] unit {} has no parent formation; filing it under '{}'",
                      unit.getId(), root.getName());
                targetFormation = root;
            }
            campaign.getPlayerForce().addUnitToFormation(unit, targetFormation.getId(), campaign);
            LOGGER.info("[CompanyGen][Leaf] AFTER addUnitToFormation unit.formationId={}", unit.getFormationId());
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
                LOGGER.info("[CompanyGen][Leaf][Progress] BEFORE listener.updateProgress count={} elapsedSec={}",
                      leafCount[0], elapsedSec);
                listener.updateProgress(0.0,
                      String.format("Materializing units and crews... (%d created, %ds elapsed)",
                            leafCount[0], elapsedSec));
                LOGGER.info("[CompanyGen][Leaf][Progress] AFTER listener.updateProgress");
            }
            LOGGER.info("[CompanyGen][Leaf] EXIT leaf #{} chassis='{}' model='{}' crew={} parent={} totalMs={}",
                  leafCount[0], entityChassis, entityModel, crew.size(), parentInfo, leafTotalMs);
        }, (descriptor, formation) -> descriptorsByFormation.put(formation, descriptor));

        LOGGER.info("[CompanyGen][Pipeline]Stage 4-7 summary: {} leaves placed, {} skipped (no entity), {} skipped (addNewUnit failed)",
              leafCount[0], skippedNoEntity[0], skippedAddFailed[0]);

        // 7a. Ship transport. Everything the tree nests under a ship - the fighter complement the Carried
        // Fighter Complement option adds - is assigned to that ship, so the TO&E shows it aboard and the
        // scenario launcher loads it in game.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7a: ship transport assignment");
        ShipTransportAssigner.assign(fd, unitsByDescriptor);

        // What the rolls produced, as the starting-cash stage prices it: the Spares and Finances tab shows the
        // percentage of exactly these units, so the build credits the percentage of exactly these units.
        Set<UUID> rolledUnitIds = snapshotHangarUnitIds(campaign);
        rolledUnitIds.removeAll(preExistingUnitIds);

        // 7b. Apply layered formation icons to every node in the campaign's Formation tree. Honors
        // the four formation-icon toggles on the options; bails cleanly if generation is disabled
        // or the formation-icon image directory is unavailable.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7b: apply layered formation icons");
        if (listener != null) {
            listener.updateProgress(0.0, "Applying formation icons...");
        }
        FormationIconBuilder.applyIcons(campaign.getPlayerForce().getFormations(), campaign, options);

        // 7b2. The most skilled pilots take the seats in the leading lances, when asked. Before ranks,
        // so the commanders chosen next are chosen from where the pilots will actually sit.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7b2: seating the most skilled pilots in the leading lances");
        PilotSkillSorter.apply(campaign, options);

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

        // 7d. Personnel flags driven by the Setup tab toggles: commander flag on the top-formation
        // officer, founder flag on every fresh hire (combat + support after the 7e merge above),
        // and random callsigns for non-Clan MekWarriors (support staff don't have the MEKWARRIOR
        // primary role so they're naturally skipped). These are pure Person-state mutations with
        // no algorithmic logic, so they live in the pipeline rather than in a dedicated helper.
        // Clan campaigns skip callsigns because their warriors carry a bloodname instead, which
        // assignBloodnames below awards.
        LOGGER.info("[CompanyGen][Pipeline]Stage 7d: personnel flags");
        if (listener != null) {
            listener.updateProgress(0.0, "Applying personnel flags...");
        }
        applyPersonnelFlags(campaign, options, generatedPersons, rootCommander);

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
        C3NetworkConfigurator.configure(fd);
        campaign.getPlayerForce().refreshNetworks(campaign.getGame());

        // 8. Spare-parts warehouse stock-up. Uses the same PartsInUseManager the daily warehouse
        // and ongoing auto-logistics rely on, so the starting inventory is consistent with the
        // user's ongoing stocking policy: each part type's stocking percentage comes from the
        // CampaignOptions.getAutoLogistics*() values that the Spares tab writes into. Contract
        // polish remains deferred.
        // 7f. Temporary crew. The assembler left the seats of any temporary-crew role empty behind a
        // single named crew member, so the pool has to be topped up to cover them or the generated
        // units arrive undercrewed.
        topUpTemporaryCrewPools(campaign);

        LOGGER.info("[CompanyGen][Pipeline]Stage 8: spare-parts warehouse stock-up");
        if (listener != null) {
            listener.updateProgress(0.0, "Stocking spare parts warehouse...");
        }
        SpareCosts spareCosts = stockSpareParts(campaign);

        // 8b. Cargo lift. Runs after the warehouse is stocked, because the tonnage the ships have to
        // haul is exactly what stocking just put in it, and before starting cash below so the hulls are
        // priced along with everything else the build produced rather than arriving free.
        LOGGER.info("[CompanyGen][Pipeline]Stage 8b: cargo lift");
        if (listener != null) {
            listener.updateProgress(0.0, "Provisioning cargo lift...");
        }
        generateCargoLift(campaign, options);

        // 9. Starting cash - single-shot path only: every unit the build created is new relative to
        // the pre-walk hangar snapshot, so this prices exactly this build's units. The two-phase
        // Command Designer flow (generateSupport=false) instead snapshots before its combat phase
        // and calls processStartingCash itself after support generation.
        if (generateSupport) {
            processStartingCash(campaign, options, preExistingUnitIds, rolledUnitIds, generatedPersons,
                  spareCosts);
        }

        LOGGER.info("[CompanyGen][Pipeline]CommandGenerator.applyToCampaign() DONE");
        return new Result(fd, generatedPersons, spareCosts, rolledUnitIds);
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
        int vehiclesStillToCome = SupportUnitGenerator.vehiclesStillToGenerate(campaign);
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
                  campaign.getPlayerForce().isClanForce());
        } else {
            LOGGER.info("[CompanyGen][SupportTOE] support teams are switched off; {} support person(s) stay unorganized",
                  supportResult.generatedPersons().size());
        }

        // Grant the standalone support vehicles a command gets for each enabled capability that has no
        // matching personnel section (logistics convoy, canteen, security). Salvage and medical
        // vehicles are handled inside SupportPersonnelToTOE.organize above, where they join their
        // section crewed from the generated staff (no double-generated personnel).
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Faction supportFaction = campaign.getPlayerForce().getFaction();
        if (campaignOptions.isUseStratCon()) {
            SupportUnitGenerator.generateLogisticsUnits(campaign, supportFaction, true);
        }
        if (campaignOptions.get(CampaignOption.USE_FATIGUE)) {
            SupportUnitGenerator.generateCommissaryUnits(campaign, supportFaction, true);
        }
        if (!campaignOptions.get(CampaignOption.PRISONER_CAPTURE_STYLE).isNone()) {
            SupportUnitGenerator.generateSecurityUnits(campaign, supportFaction, true);
        }

        // Assign techs to units using the Setup tab's three-slot sort grid (Pilot Rank / Unit Weight /
        // Pilot Skill, each with its own direction). Gated on isAssignTechsToUnits; pulls only from the
        // techs SupportPersonnelGenerator just created so we don't steal a pre-existing campaign tech.
        // Runs once every vehicle exists, the support stage's own included, so the flatbeds and the
        // recovery vehicles get their mechanics too.
        SupportPersonnelAssigner.assign(campaign, options, supportResult);

        // Decorate the support formations created above with layered TOE icons. This must happen here
        // (not only at the tail of applyToCampaign) because the two-phase Command Designer flow calls
        // this method directly, after applyToCampaign already ran its icon pass with no support
        // formations in the tree - so without this call the support command commits with blank icons.
        // Appoint the senior staff - CMO, head technician, chief administrator. Runs
        // here rather than in stage 7d because the eligible people are exactly the support staff
        // generated above, and because the two-phase Command Designer flow reaches this method
        // directly; anywhere else and a design-then-generate-support build would commit with the posts
        // vacant.
        SeniorAppointmentAssigner.assign(campaign, supportResult.generatedPersons());

        // 7e2. The support sections are platoons and squads that need bays like any other unit, and no ship was
        // sized for them. Size lift for what this stage added, against the bays the hangar has free. Combat
        // units without a ship are left alone: their ship was struck out of the preview, and that stands.
        Set<UUID> unitsAddedBySupport = new HashSet<>(snapshotHangarUnitIds(campaign));
        unitsAddedBySupport.removeAll(unitsBeforeSupport);
        topUpLift(campaign, options, unitsAddedBySupport);

        LOGGER.info("[CompanyGen][Pipeline]Stage 7e: applying formation icons to support formations");
        FormationIconBuilder.applyIcons(campaign.getPlayerForce().getFormations(), campaign, options);

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
        try {
            LiftTopUp.topUp(campaign, snapshot.getFaction(), snapshot.getYear(), snapshot.getRating(),
                  snapshot.getDropshipPct(), snapshot.getJumpshipPct(), newUnitIds);
        } catch (Exception exception) {
            LOGGER.error(exception, "[CompanyGen][LiftTopUp] lift top-up failed; the command keeps the ships it had");
        }
    }

    /**
     * Captures the IDs of every unit currently in the campaign's hangar. Take this snapshot before a
     * build starts so {@link #processStartingCash} can price only the units that build created - the
     * Command Designer takes it before its combat phase and passes it back after support generation.
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
     * Stage 9 - starting cash. When Process Finances is on, the base starting cash is either
     * {@link CommandGenerationOptions#getStartingCashPercent()} percent of the rolled units' total
     * purchase cost - the same units the Spares and Finances tab previews, so the build credits the
     * figure the tab showed - or - with Randomize Starting Cash - a roll of the configured number of
     * d6 in millions of C-Bills. If Pay for Initial Setup is on, the command's real generation costs
     * (personnel hiring at twice salary, unit purchase, and the stocked spare parts / armour /
     * ammunition, each gated by its own toggle) are then debited; cash floors at the Minimum
     * Starting Float, with the shortfall taken as a two-year 15% starting loan when Generate
     * Starting Loan is on. The result is credited as starting capital and reported in the Finances
     * daily log.
     *
     * <p>Units already in the hangar before the build (per {@code preExistingUnitIds}) are excluded
     * from both the percentage base and the unit-purchase debit, so building an additional command
     * into an existing campaign prices only the new units.</p>
     *
     * @param campaign           the campaign to credit
     * @param options            the generation options carrying the finance toggles
     * @param preExistingUnitIds hangar unit IDs captured by {@link #snapshotHangarUnitIds} before
     *                           the build; units with these IDs are not priced
     * @param rolledUnitIds      the units the rolls produced, the base of the percentage; the support
     *                           vehicles, staff carriers and ships the build added afterwards are priced
     *                           only in the pay-for-units debit
     * @param generatedPersons   every Person this build created, for the hiring-cost debit
     * @param spareCosts         the stocked spares' value by category, from the build's
     *                           {@link Result#spareCosts()}; {@code null} is treated as zero
     */
    public static void processStartingCash(Campaign campaign, CommandGenerationOptions options,
          Set<UUID> preExistingUnitIds, Set<UUID> rolledUnitIds, List<Person> generatedPersons,
          @Nullable SpareCosts spareCosts) {
        if (!options.isProcessFinances()) {
            LOGGER.info("[CompanyGen][Pipeline]Stage 9: finances disabled; no starting cash granted");
            return;
        }
        SpareCosts spares = (spareCosts == null) ? SpareCosts.zero() : spareCosts;

        // Price the units this build created. The rolled units are the base of the percentage model, as
        // the tab previewed; everything the build added after the rolls counts in the unit-purchase debit
        // only, so a top-up JumpShip does not double the starting cash.
        Money newUnitValue = Money.zero();
        Money rolledUnitValue = Money.zero();
        int pricedUnits = 0;
        int rolledUnits = 0;
        for (Unit unit : campaign.getUnits()) {
            if (preExistingUnitIds.contains(unit.getId())) {
                continue;
            }
            newUnitValue = newUnitValue.plus(unit.getBuyCost());
            pricedUnits++;
            if (rolledUnitIds.contains(unit.getId())) {
                rolledUnitValue = rolledUnitValue.plus(unit.getBuyCost());
                rolledUnits++;
            }
        }

        // Base cash: percentage of unit value, or the dice roll when randomized.
        int percent = options.getStartingCashPercent();
        Money startingCash;
        if (options.isRandomizeStartingCash()) {
            startingCash = Money.of(1_000_000)
                                 .multipliedBy(Utilities.dice(options.getRandomStartingCashDiceCount(), 6));
            LOGGER.info("[CompanyGen][Pipeline]Stage 9: randomized starting cash {}d6 million -> {}",
                  options.getRandomStartingCashDiceCount(), startingCash.toAmountAndSymbolString());
        } else {
            startingCash = rolledUnitValue.multipliedBy(percent).dividedBy(100).round();
            LOGGER.info("[CompanyGen][Pipeline]Stage 9: starting cash = {}% of {} rolled unit(s) worth {} -> {};"
                        + " {} unit(s) worth {} added by the build after the rolls are priced for setup costs only",
                  percent, rolledUnits, rolledUnitValue.toAmountAndSymbolString(),
                  startingCash.toAmountAndSymbolString(), pricedUnits - rolledUnits,
                  newUnitValue.minus(rolledUnitValue).toAmountAndSymbolString());
        }

        Money minimumStartingFloat = Money.of(options.getMinimumStartingFloat());
        Money loan = Money.zero();

        if (options.isPayForSetup()) {
            Money costs = Money.zero();
            if (options.isPayForPersonnel()) {
                Money hiringCosts = Money.zero();
                CampaignOptions campaignOptions = campaign.getCampaignOptions();
                boolean isClanForce = campaign.getPlayerForce().isClanForce();
                LocalDate today = campaign.getLocalDate();
                for (Person person : generatedPersons) {
                    hiringCosts = hiringCosts.plus(
                          person.getSalary(campaignOptions, isClanForce, today).multipliedBy(2));
                }
                costs = costs.plus(hiringCosts);
            }
            if (options.isPayForUnits()) {
                costs = costs.plus(newUnitValue);
            }
            if (options.isPayForParts()) {
                costs = costs.plus(spares.parts());
            }
            if (options.isPayForArmour()) {
                costs = costs.plus(spares.armour());
            }
            if (options.isPayForAmmunition()) {
                costs = costs.plus(spares.ammunition());
            }
            LOGGER.info("[CompanyGen][Pipeline]Stage 9: setup costs {} (personnel={} units={} parts={} armour={} ammo={})",
                  costs.toAmountAndSymbolString(), options.isPayForPersonnel(), options.isPayForUnits(),
                  options.isPayForParts(), options.isPayForArmour(), options.isPayForAmmunition());

            Money maximumPreLoanCosts = startingCash.minus(minimumStartingFloat);
            if (maximumPreLoanCosts.isGreaterOrEqualThan(costs)) {
                startingCash = startingCash.minus(costs);
            } else {
                // Cash floors at the minimum float; the shortfall becomes a loan if enabled.
                startingCash = minimumStartingFloat;
                if (options.isStartingLoan()) {
                    loan = costs.minus(maximumPreLoanCosts).round();
                }
            }
            startingCash = startingCash.round();
        } else {
            startingCash = startingCash.isGreaterOrEqualThan(minimumStartingFloat)
                                 ? startingCash : minimumStartingFloat;
        }

        if (startingCash.isPositive()) {
            campaign.getPlayerForce().getFinances().credit(TransactionType.STARTING_CAPITAL,
                  campaign.getLocalDate(), startingCash,
                  getTextAt(RESOURCE_BUNDLE, "CommandGenerator.startingCapital.reason"));
        }
        if (!loan.isZero()) {
            campaign.getPlayerForce().getFinances().addLoan(new Loan(loan, 15, 2, FinancialTerm.MONTHLY,
                  100, campaign.getLocalDate()));
        }

        if (loan.isZero()) {
            campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CommandGenerator.startingCapital.report",
                  startingCash.toAmountAndSymbolString(), percent));
        } else {
            campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE,
                  "CommandGenerator.startingCapital.reportWithLoan",
                  startingCash.toAmountAndSymbolString(), loan.toAmountAndSymbolString()));
        }
        LOGGER.info("[CompanyGen][Pipeline]Stage 9: credited {} starting capital, loan {}",
              startingCash.toAmountAndSymbolString(), loan.toAmountAndSymbolString());
    }

    /**
     * Estimates the purchase value of a set of rolled entities the way {@link Unit#getBuyCost()}
     * will price them once materialized: the entity cost (alternate cost for conventional infantry)
     * times the campaign's tech-base price multiplier. Used by the Command Designer's starting-cash
     * preview to price the design model before anything is committed.
     *
     * @param campaign the campaign supplying the price multipliers
     * @param entities the rolled entities to price (e.g. from {@link #collectEntities})
     *
     * @return the estimated total purchase value
     */
    public static Money estimateUnitValue(Campaign campaign, List<Entity> entities) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Money total = Money.zero();
        for (Entity entity : entities) {
            Money cost = Money.of((entity instanceof Infantry)
                                        ? entity.getAlternateCost()
                                        : entity.getCost(false));
            if (entity.isMixedTech()) {
                cost = cost.multipliedBy(campaignOptions.get(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER));
            } else if (entity.isClan()) {
                cost = cost.multipliedBy(campaignOptions.get(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER));
            } else {
                cost = cost.multipliedBy(campaignOptions.get(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER));
            }
            total = total.plus(cost);
        }
        return total;
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
     * Stage 7d: applies the Setup tab's personnel-flag toggles to the generation result.
     *
     * <ul>
     *   <li>{@code isAssignCompanyCommanderFlag} → {@link Person#setCommander(boolean)} on the
     *       commander {@link RulesetRankAssigner} promoted at the campaign-root formation.</li>
     *   <li>{@code isAssignFounderFlag} → {@link Person#setFounder(boolean)} on every Person this
     *       generation created. Where the campaign also has {@code USE_FOUNDER_PLOT_ARMOR} set, each
     *       founder gains a point of Edge, matching what newborn founders receive in
     *       {@code AbstractProcreation}.</li>
     *   <li>{@code isAssignMekWarriorsCallSigns} → {@link Person#setCallsign(String)} from
     *       {@link RandomCallsignGenerator} for every primary-role MekWarrior, skipped in Clan
     *       campaigns (Clan MekWarriors get their bloodname instead of a fixed-wing-style callsign).</li>
     * </ul>
     */
    private static void applyPersonnelFlags(Campaign campaign, CommandGenerationOptions options,
          List<Person> generatedPersons, @Nullable Person rootCommander) {
        if (options.isAssignCompanyCommanderFlag() && rootCommander != null) {
            rootCommander.setCommander(true);
            LOGGER.info("[CompanyGen][Pipeline][Flags] commander flag set on '{}'", rootCommander.getFullName());
            assignSecondInCommand(campaign, generatedPersons);
        }
        int founderCount = 0;
        int callsignCount = 0;
        boolean applyFounder = options.isAssignFounderFlag();
        boolean applyCallsigns = options.isAssignMekWarriorsCallSigns() && !campaign.getPlayerForce().isClanForce();
        boolean applyFounderPlotArmor = campaign.getCampaignOptions().get(CampaignOption.USE_FOUNDER_PLOT_ARMOR);
        RandomCallsignGenerator callsigns = applyCallsigns ? RandomCallsignGenerator.getInstance() : null;
        for (Person person : generatedPersons) {
            if (applyFounder) {
                person.setFounder(true);
                founderCount++;
                if (applyFounderPlotArmor) {
                    person.changeAttributeScore(SkillAttribute.EDGE, 1);
                }
            }
            if (applyCallsigns && person.getPrimaryRole() == PersonnelRole.MEKWARRIOR) {
                person.setCallsign(callsigns.generate());
                callsignCount++;
            }
        }
        LOGGER.info("[CompanyGen][Pipeline][Flags] founder={} callsigns={} (clanCampaign={}, founderPlotArmor={})",
              founderCount, callsignCount, campaign.getPlayerForce().isClanForce(), applyFounderPlotArmor);
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
    /**
     * Stage 3b: writes the designer's temporary-crew choices to the campaign.
     *
     * <p>Whether a unit's seats are filled with named people or left for temporary crew is decided by
     * {@link MultiCrewAssembler} as each unit is built, reading the campaign options at that moment. So the
     * choices have to reach the campaign before the first unit is crewed; written any later, every unit would
     * already hold named crew under the previous settings and the toggles would appear to do nothing.</p>
     *
     * @param campaign the campaign being generated into
     * @param options  the designer's choices
     */
    // Package-private so the regression test can check the choices reach the campaign.
    static void applyTemporaryCrewChoices(Campaign campaign, CommandGenerationOptions options) {
        Set<TemporaryCrewRole> chosenRoles = options.getTemporaryCrewRoles();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        for (TemporaryCrewRole role : TemporaryCrewRole.values()) {
            campaignOptions.set(role.getCampaignOption(), chosenRoles.contains(role));
        }
        LOGGER.info("[CompanyGen][Pipeline] Stage 3b: temporary crew written to the campaign - enabled for {}",
              chosenRoles.isEmpty() ? "no roles" : chosenRoles);
    }

    // Package-private so the regression test can check the choice reaches the campaign.
    static void applyAugmentationRules(Campaign campaign, CommandGenerationOptions options) {
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

    private static SpareCosts stockSpareParts(Campaign campaign) {
        long start = System.nanoTime();
        PartsInUseManager partsInUseManager = new PartsInUseManager(campaign);
        // Bracket the stock-up with warehouse category totals so the finance stage can price what
        // was added (the GM stock-up itself is free; the pay-for toggles decide whether the player
        // is billed for it).
        Money[] before = warehouseValueByCategory(campaign);
        // ignoreMothballedUnits=true matches WarehouseTab's daily refresh: at generation time
        // nothing is mothballed yet, but keep the call shape consistent with the rest of the
        // codebase. isResupply=false skips the resupply-specific prohibited-unit-type filter.
        // ignoreSparesUnderQuality=QUALITY_A accepts any quality already on hand as inventory
        // toward the target.
        Set<PartInUse> partsInUse = partsInUseManager.getPartsInUse(true, false, PartQuality.QUALITY_A);
        partsInUseManager.stockUpPartsInUseGM(partsInUse);
        Money[] after = warehouseValueByCategory(campaign);
        SpareCosts spareCosts = new SpareCosts(after[0].minus(before[0]), after[1].minus(before[1]),
              after[2].minus(before[2]));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        LOGGER.info("[CompanyGen][Pipeline][Spares] reviewed {} distinct part types; added value parts={} armour={} ammo={}; elapsed={}ms",
              partsInUse.size(), spareCosts.parts().toAmountAndSymbolString(),
              spareCosts.armour().toAmountAndSymbolString(),
              spareCosts.ammunition().toAmountAndSymbolString(), elapsedMs);
        return spareCosts;
    }

    /**
     * Sums the warehouse's spare-part value as {@code [parts, armour, ammunition]}, each part's
     * actual value times its stack quantity. Used to price the stock-up by delta.
     */
    private static Money[] warehouseValueByCategory(Campaign campaign) {
        Money parts = Money.zero();
        Money armour = Money.zero();
        Money ammunition = Money.zero();
        for (Part part : campaign.getPlayerForce().getWarehouse().getParts()) {
            Money value = part.getActualValue().multipliedBy(part.getQuantity());
            if (part instanceof Armor) {
                armour = armour.plus(value);
            } else if (part instanceof AmmoStorage) {
                ammunition = ammunition.plus(value);
            } else {
                parts = parts.plus(value);
            }
        }
        return new Money[] { parts, armour, ammunition };
    }
}

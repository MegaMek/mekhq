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
package mekhq.campaign.force;

import static java.lang.Math.max;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.force.Formation.FORMATION_NONE;
import static mekhq.campaign.force.Formation.FORMATION_ORIGIN;
import static mekhq.campaign.force.Formation.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.force.FormationType.STANDARD;
import static mekhq.campaign.mission.RandomFactionCamouflage.pickRandomCamouflage;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_A;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MINIMUM_TEMPORARY_CAPACITY;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;
import javax.swing.JOptionPane;

import megamek.client.ui.util.PlayerColour;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.camOpsReputation.ForceReputationController;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.events.NetworkChangedEvent;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.MASHCapacity;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTechProgression;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Base class holding the force-level state and logic extracted from {@link mekhq.campaign.Campaign}: faction identity
 * and appearance, finances, reputation/standing, the TO&amp;E, and force options.
 *
 * <p>The located resources (hangar, warehouse, personnel, location) are <em>not</em> owned by the force — they belong
 * to its {@link Detachment}(s). A force is not itself a location node; {@link #getDetachments()} is the
 * detachment-count-agnostic way to reach them, and the aggregate helpers ({@link #allUnits()}, {@link #allPersonnel()})
 * project across every detachment. Convenience accessors that assume a single detachment live on
 * {@link SingleDetachmentForce}; force-internal operations that still assume one go through
 * {@link #requireSingleDetachment()}.</p>
 *
 * <p>A force holds <em>no</em> reference back to its {@link mekhq.campaign.Campaign}. Campaign-level concerns — the
 * current date, and writing daily-report lines — are the campaign's responsibility: it passes values such as the date
 * in as method parameters and does its own reporting around the force's state changes.</p>
 */
public abstract class AbstractForce {

    private ForceHumanResources humanResources = new ForceHumanResources();
    private ForceShoppingList shoppingList = new ForceShoppingList();

    private final ForceOptions forceOptions;

    // Identity / appearance
    private String name;
    private RankSystem rankSystem;
    private Camouflage camouflage = pickRandomCamouflage(3025, "Root");
    private PlayerColour colour = PlayerColour.BLUE;
    private StandardFormationIcon unitIcon = new UnitIcon(null, null);
    private String retainerEmployerCode = null;
    private LocalDate retainerStartDate = null;
    private megamek.common.enums.Faction techFaction;

    private Finances finances;

    // Reputation / standing / crime / initiative
    private ForceReputationController reputation;
    private FactionStandings factionStandings;
    private int crimeRating = 0;
    private int crimePirateModifier = 0;
    private LocalDate dateOfLastCrime = null;
    private int initiativeBonus = 0;
    private int initiativeMaxBonus = 1;

    private final TreeMap<Integer, Formation> formationIds = new TreeMap<>();
    // Navigation toggles
    private boolean isAvoidingEmptySystems = true;
    private boolean isOverridingCommandCircuitRequirements = false;
    // Parts-in-use / restock options
    private boolean ignoreMothballed = true;
    private boolean topUpWeekly = false;
    private PartQuality ignoreSparesUnderQuality = QUALITY_A;
    // Capacity / facility state
    private int temporaryPrisonerCapacity = DEFAULT_TEMPORARY_CAPACITY;
    private int mashTheatreCapacity = 0;
    private boolean fieldKitchenWithinCapacity = false;
    private int repairBaysRented = 0;
    private List<UUID> automatedMothballUnits = new ArrayList<>();
    // Table of Organisation & Equipment (TO&E) and StratCon combat teams
    private Formation formations;
    private int lastFormationId;
    private Hashtable<Integer, CombatTeam> combatTeams = new Hashtable<>();

    protected AbstractForce(ForceOptions forceOptions, megamek.common.enums.Faction techFaction, RankSystem rankSystem,
          Finances finances, ForceReputationController reputation, FactionStandings factionStandings) {
        this.forceOptions = forceOptions;
        this.techFaction = techFaction;
        this.rankSystem = rankSystem;
        this.finances = finances;
        this.reputation = reputation;
        this.factionStandings = factionStandings;
    }

    /**
     * The {@link Detachment}s this force is currently split across. A {@link PlayerForce} tracks exactly one; the
     * located resources (hangar, warehouse, personnel, location) are owned by the detachment(s), not the force.
     */
    public abstract Collection<Detachment> getDetachments();

    /** All units across every {@link Detachment} of this force. Multi-detachment-safe. */
    public Collection<Unit> allUnits() {
        List<Unit> units = new ArrayList<>();
        for (Detachment detachment : getDetachments()) {
            units.addAll(detachment.getHangar().getUnits());
        }
        return units;
    }

    /** All personnel across every {@link Detachment} of this force. Multi-detachment-safe. */
    public Collection<Person> allPersonnel() {
        List<Person> personnel = new ArrayList<>();
        for (Detachment detachment : getDetachments()) {
            personnel.addAll(detachment.getPersonnel().values());
        }
        return personnel;
    }

    /**
     * The sole {@link Detachment}, asserting the single-detachment assumption. Throws {@link IllegalStateException} if
     * the force does not have exactly one detachment.
     *
     * <p>This is the internal counterpart to {@link SingleDetachmentForce}: grep {@code requireSingleDetachment} to
     * find the force-internal operations that are <em>not yet</em> multi-detachment-safe and must be reworked before a
     * multi-detachment force can use them.</p>
     */
    protected Detachment requireSingleDetachment() {
        Collection<Detachment> detachments = getDetachments();
        if (detachments.size() != 1) {
            throw new IllegalStateException("Operation assumes a single detachment but the force has "
                                                  + detachments.size());
        }
        return detachments.iterator().next();
    }

    public ForceHumanResources getHumanResources() {
        return humanResources;
    }

    public void setHumanResources(ForceHumanResources humanResources) {
        this.humanResources = humanResources;
    }

    public ForceShoppingList getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(ForceShoppingList shoppingList) {
        this.shoppingList = shoppingList;
    }

    public ForceOptions getForceOptions() {
        return forceOptions;
    }

    public Faction getFaction() {
        return forceOptions.getFaction();
    }

    public void setFaction(final Faction faction) {
        setFactionDirect(faction);
        updateTechFactionCode();
    }

    public void setFactionDirect(final Faction faction) {
        forceOptions.setFaction(faction);
    }

    public megamek.common.enums.Faction getTechFaction() {
        return techFaction;
    }

    public void updateTechFactionCode() {
        if (forceOptions.isFactionIntroDate()) {
            for (megamek.common.enums.Faction f : megamek.common.enums.Faction.values()) {
                if (f.equals(megamek.common.enums.Faction.NONE)) {
                    continue;
                }
                if (f.getCodeMM().equals(getFaction().getShortName())) {
                    techFaction = f;
                    UnitTechProgression.loadFaction(techFaction);
                    return;
                }
            }
            // If the tech progression data does not include the current faction, use a generic.
            if (getFaction().isClan()) {
                techFaction = megamek.common.enums.Faction.CLAN;
            } else if (getFaction().isPeriphery()) {
                techFaction = megamek.common.enums.Faction.PER;
            } else {
                techFaction = megamek.common.enums.Faction.IS;
            }
        } else {
            techFaction = megamek.common.enums.Faction.NONE;
        }
        // Unit tech level will be calculated if the code has changed.
        UnitTechProgression.loadFaction(techFaction);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(final @Nullable RankSystem rankSystem) {
        // If they are the same object, there hasn't been a change and thus don't need to process further
        if (Objects.equals(getRankSystem(), rankSystem)) {
            return;
        }

        // Then, we need to validate the rank system. Null isn't valid to be set but may be the result of a cancelled
        // load. However, validation will prevent that
        final RankValidator rankValidator = new RankValidator();
        if (!rankValidator.validate(rankSystem, false)) {
            return;
        }

        // We need to know the old rank system for personnel processing
        final RankSystem oldRankSystem = getRankSystem();

        // And with that, we can set the rank system
        setRankSystemDirect(rankSystem);

        // Finally, we fix all personnel ranks and ensure they are properly set
        allPersonnel().stream()
              .filter(person -> person.getRankSystem().equals(oldRankSystem))
              .forEach(person -> person.setRankSystem(rankValidator, rankSystem));
    }

    public void setRankSystemDirect(final RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public void setCamouflage(final Camouflage camouflage) {
        this.camouflage = camouflage;
    }

    public PlayerColour getColour() {
        return colour;
    }

    public void setColour(final PlayerColour colour) {
        this.colour = Objects.requireNonNull(colour, "Colour cannot be set to null");
    }

    public StandardFormationIcon getUnitIcon() {
        return unitIcon;
    }

    public void setUnitIcon(final StandardFormationIcon unitIcon) {
        this.unitIcon = unitIcon;
    }

    public String getRetainerEmployerCode() {
        return retainerEmployerCode;
    }

    public void setRetainerEmployerCode(String code) {
        retainerEmployerCode = code;
    }

    public LocalDate getRetainerStartDate() {
        return retainerStartDate;
    }

    public void setRetainerStartDate(LocalDate retainerStartDate) {
        this.retainerStartDate = retainerStartDate;
    }

    public Finances getFinances() {
        return finances;
    }

    public void setFinances(Finances finances) {
        this.finances = finances;
    }

    public Money getFunds() {
        return finances.getBalance();
    }

    /**
     * Credits {@code quantity} to this force's finances. The campaign is responsible for defaulting the description and
     * writing any daily-report line.
     */
    public void addFunds(final TransactionType type, final LocalDate date, final Money quantity,
          final String description) {
        finances.credit(type, date, quantity, description);
    }

    /**
     * Debits {@code quantity} from this force's finances. The campaign is responsible for defaulting the description
     * and writing any daily-report line.
     */
    public void removeFunds(final TransactionType type, final LocalDate date, final Money quantity,
          final String description) {
        finances.debit(type, date, quantity, description);
    }

    public ForceReputationController getReputation() {
        return reputation;
    }

    public void setReputation(ForceReputationController reputation) {
        this.reputation = reputation;
    }

    public FactionStandings getFactionStandings() {
        return factionStandings;
    }

    public void setFactionStandings(FactionStandings factionStandings) {
        this.factionStandings = factionStandings;
    }

    public int getRawCrimeRating() {
        return crimeRating;
    }

    public void setCrimeRating(int crimeRating) {
        this.crimeRating = crimeRating;
    }

    public void changeCrimeRating(int change) {
        this.crimeRating = Math.min(0, crimeRating + change);
    }

    public int getCrimePirateModifier() {
        return crimePirateModifier;
    }

    public void setCrimePirateModifier(int crimePirateModifier) {
        this.crimePirateModifier = crimePirateModifier;
    }

    public void changeCrimePirateModifier(int change) {
        this.crimePirateModifier = Math.min(0, crimePirateModifier + change);
    }

    public int getAdjustedCrimeRating() {
        return crimeRating + crimePirateModifier;
    }

    public @Nullable LocalDate getDateOfLastCrime() {
        return dateOfLastCrime;
    }

    public void setDateOfLastCrime(LocalDate dateOfLastCrime) {
        this.dateOfLastCrime = dateOfLastCrime;
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(int bonus) {
        initiativeBonus = bonus;
    }

    public void applyInitiativeBonus(int bonus) {
        if (bonus > initiativeMaxBonus) {
            initiativeMaxBonus = bonus;
        }
        if ((bonus + initiativeBonus) > initiativeMaxBonus) {
            initiativeBonus = initiativeMaxBonus;
        } else {
            initiativeBonus += bonus;
        }
    }

    public void initiativeBonusIncrement(boolean change) {
        if (change) {
            setInitiativeBonus(++initiativeBonus);
        } else {
            setInitiativeBonus(--initiativeBonus);
        }
        if (initiativeBonus > initiativeMaxBonus) {
            initiativeBonus = initiativeMaxBonus;
        }
    }

    public int getInitiativeMaxBonus() {
        return initiativeMaxBonus;
    }

    public void setInitiativeMaxBonus(int bonus) {
        initiativeMaxBonus = bonus;
    }

    public boolean isAvoidingEmptySystems() {
        return isAvoidingEmptySystems;
    }

    public void setIsAvoidingEmptySystems(boolean isAvoidingEmptySystems) {
        this.isAvoidingEmptySystems = isAvoidingEmptySystems;
    }

    public boolean isOverridingCommandCircuitRequirements() {
        return isOverridingCommandCircuitRequirements;
    }

    public void setIsOverridingCommandCircuitRequirements(boolean isOverridingCommandCircuitRequirements) {
        this.isOverridingCommandCircuitRequirements = isOverridingCommandCircuitRequirements;
    }

    public boolean getIgnoreMothballed() {
        return ignoreMothballed;
    }

    public void setIgnoreMothballed(boolean ignoreMothballed) {
        this.ignoreMothballed = ignoreMothballed;
    }

    public boolean getTopUpWeekly() {
        return topUpWeekly;
    }

    public void setTopUpWeekly(boolean topUpWeekly) {
        this.topUpWeekly = topUpWeekly;
    }

    public PartQuality getIgnoreSparesUnderQuality() {
        return ignoreSparesUnderQuality;
    }

    public void setIgnoreSparesUnderQuality(PartQuality ignoreSparesUnderQuality) {
        this.ignoreSparesUnderQuality = ignoreSparesUnderQuality;
    }

    public int getTemporaryPrisonerCapacity() {
        return temporaryPrisonerCapacity;
    }

    public void setTemporaryPrisonerCapacity(int temporaryPrisonerCapacity) {
        this.temporaryPrisonerCapacity = max(MINIMUM_TEMPORARY_CAPACITY, temporaryPrisonerCapacity);
    }

    /**
     * Adjusts the temporary prisoner capacity by {@code delta}, clamped to at least
     * {@link mekhq.campaign.randomEvents.prisoners.PrisonerEventManager#MINIMUM_TEMPORARY_CAPACITY}.
     */
    public void changeTemporaryPrisonerCapacity(int delta) {
        temporaryPrisonerCapacity = max(MINIMUM_TEMPORARY_CAPACITY, temporaryPrisonerCapacity + delta);
    }

    public int getCachedMashTheaterCapacity() {
        return mashTheatreCapacity;
    }

    public void setMashTheatreCapacity(int mashTheatreCapacity) {
        this.mashTheatreCapacity = mashTheatreCapacity;
    }

    /**
     * Whether the force's MASH theatre capacity covers its patients assigned to doctors. Off-contract or in transit
     * there is no cap. Takes a {@link Campaign} for the contract/location context the force does not hold.
     */
    public boolean getMashTheatresWithinCapacity(Campaign campaign) {
        if (campaign.isOnContractAndPlanetside()) {
            return true;
        }

        return calculateMASHTheaterCapacity(campaign) >= getHumanResources().getPatientsAssignedToDoctors().size();
    }

    public int calculateMASHTheaterCapacity(Campaign campaign) {
        List<Unit> unitsInTOE = getFormation(FORMATION_ORIGIN).getAllUnitsAsUnits(requireSingleDetachment().getHangar(),
              false);
        int baseCapacity = MASHCapacity.checkMASHCapacity(unitsInTOE,
              campaign.getCampaignOptions().getMASHTheatreCapacity());
        int rentedCapacity = FacilityRentals.getCapacityIncreaseFromRentals(campaign.getActiveContracts(),
              ContractRentalType.HOSPITAL_BEDS);
        return baseCapacity + rentedCapacity;
    }

    public boolean getFieldKitchenWithinCapacity() {
        return fieldKitchenWithinCapacity;
    }

    public void setFieldKitchenWithinCapacity(boolean fieldKitchenWithinCapacity) {
        this.fieldKitchenWithinCapacity = fieldKitchenWithinCapacity;
    }

    public int getRepairBaysRented() {
        return repairBaysRented;
    }

    public void setRepairBaysRented(int repairBaysRented) {
        this.repairBaysRented = repairBaysRented;
    }

    public void changeRepairBaysRented(int delta) {
        repairBaysRented = max(0, repairBaysRented + delta);
    }

    public List<UUID> getAutomatedMothballUnits() {
        return automatedMothballUnits;
    }

    public void setAutomatedMothballUnits(List<UUID> automatedMothballUnits) {
        this.automatedMothballUnits = automatedMothballUnits;
    }

    public Formation getFormations() {
        return formations;
    }

    public void setFormations(Formation formations) {
        this.formations = formations;
    }

    public List<Formation> getAllFormations() {
        return new ArrayList<>(formationIds.values());
    }

    public TreeMap<Integer, Formation> getFormationIds() {
        return formationIds;
    }

    public int getLastFormationId() {
        return lastFormationId;
    }

    /** The raw, unsanitized combat-team table (keyed by formation id); used for serialization and iteration. */
    public Hashtable<Integer, CombatTeam> getCombatTeamsMap() {
        return combatTeams;
    }

    @Nullable
    public Formation getFormation(int id) {
        return formationIds.get(id);
    }

    /**
     * Retrieves all units in the Table of Organization and Equipment (TOE).
     *
     * @param standardFormationsOnly if {@code true}, returns only units in {@link FormationType#STANDARD} formations;
     *                               if {@code false}, returns all units.
     *
     * @return a List of UUID objects representing all units in the TOE according to the specified filter
     */
    public List<UUID> getAllUnitsInTheTOE(boolean standardFormationsOnly) {
        return formations.getAllUnits(standardFormationsOnly);
    }

    public void addCombatTeam(CombatTeam combatTeam) {
        combatTeams.put(combatTeam.getFormationId(), combatTeam);
    }

    public void removeCombatTeam(final int formationId) {
        combatTeams.remove(formationId);
    }

    /**
     * Returns the {@link Hashtable} keyed by the combat team's {@code formationId}, after removing ineligible teams.
     * The sanitization removes the need for {@code isEligible()} checks whenever the table is fetched.
     */
    public Hashtable<Integer, CombatTeam> getCombatTeamsAsMap(Campaign campaign) {
        for (Formation formation : getAllFormations()) {
            int formationId = formation.getId();
            if (combatTeams.containsKey(formationId)) {
                CombatTeam combatTeam = combatTeams.get(formationId);

                if (combatTeam.isEligible(campaign)) {
                    continue;
                }
            } else {
                CombatTeam combatTeam = new CombatTeam(formationId, campaign);

                if (combatTeam.isEligible(campaign)) {
                    combatTeams.put(formationId, combatTeam);
                    continue;
                }
            }

            combatTeams.remove(formationId);
        }

        return combatTeams;
    }

    public ArrayList<CombatTeam> getCombatTeamsAsList(Campaign campaign) {
        ArrayList<CombatTeam> combatTeamsList = new ArrayList<>();
        for (CombatTeam combatTeam : getCombatTeamsAsMap(campaign).values()) {
            if (formationIds.containsKey(combatTeam.getFormationId())) {
                combatTeamsList.add(combatTeam);
            }
        }
        return combatTeamsList;
    }

    /**
     * Add formation to an existing superformation. This method will also assign the formation an id and place it in the
     * formationId hash.
     */
    public void addFormation(Formation formation, Formation superFormation, Campaign campaign) {
        int id = lastFormationId + 1;
        formation.setId(id);
        superFormation.addSubFormation(formation, true);
        formation.setScenarioId(superFormation.getScenarioId(), campaign);
        formationIds.put(id, formation);
        lastFormationId = id;

        formation.updateCommander(campaign);

        if (campaign.getCampaignOptions().isUseStratCon()) {
            recalculateCombatTeams(campaign);
        }
    }

    /**
     * Moves {@code formation} to sit directly under {@code superFormation} in the TOE, detaching it from its current
     * parent and inheriting the target's scenario assignment. Formation-type standardization is then applied per the
     * moved formation's {@link FormationType} (parents may be standardized, children may inherit), and formation levels
     * are repopulated across the TOE. No-ops if {@code formation} is {@code null} or equals {@code superFormation}. The
     * {@link Campaign} is supplied as a parameter for the scenario and TOE updates this drives.
     */
    public void moveFormation(Formation formation, Formation superFormation, Campaign campaign) {
        // Can't move a null formation under a subformation and can't move a formation under itself.
        if (formation == null || formation.equals(superFormation)) {
            return;
        }
        Formation parentFormation = formation.getParentFormation();

        if (null != parentFormation) {
            parentFormation.removeSubFormation(formation.getId());
        }

        superFormation.addSubFormation(formation, true);
        formation.setScenarioId(superFormation.getScenarioId(), campaign);

        FormationType formationType = formation.getFormationType();

        if (formationType.shouldStandardizeParents()) {
            for (Formation individualParentFormation : formation.getAllParents()) {
                individualParentFormation.setFormationType(STANDARD, false);
            }
        }

        if (formationType.shouldChildrenInherit()) {
            for (Formation childFormation : formation.getAllSubFormations()) {
                childFormation.setFormationType(formationType, false);
            }
        }

        // repopulate formation levels across the TO&E
        Formation.populateFormationLevelsFromOrigin(campaign);
    }

    /**
     * This is used by the XML loader. The id should already be set for this formation so don't increment.
     */
    public void importFormation(Formation formation) {
        lastFormationId = max(lastFormationId, formation.getId());
        formationIds.put(formation.getId(), formation);
    }

    public void addUnitToFormation(final @Nullable Unit unit, final Formation formation, Campaign campaign) {
        addUnitToFormation(unit, formation.getId(), campaign);
    }

    /**
     * Add unit to an existing formation. This method will also assign that formation's id to the unit.
     */
    public void addUnitToFormation(@Nullable Unit unit, int id, Campaign campaign) {
        if (unit == null) {
            return;
        }

        if (id == FORMATION_NONE) {
            Formation currentFormation = getFormation(unit.getFormationId());
            unit.setFormationId(FORMATION_NONE);
            unit.setScenarioId(NO_ASSIGNED_SCENARIO);
            MekHQ.triggerEvent(new OrganizationChangedEvent(campaign, currentFormation, unit));
            return;
        }

        Formation formation = formationIds.get(id);
        Formation prevFormation = formationIds.get(unit.getFormationId());
        boolean useTransfers = false;
        boolean transferLog = !campaign.getCampaignOptions().isUseTransfers();

        if (null != prevFormation) {
            if (null != prevFormation.getTechID()) {
                unit.removeTech();
            }
            // We log removal if we don't use transfers or if it can't be assigned to a new formation
            prevFormation.removeUnit(campaign, unit.getId(), transferLog || (formation == null));
            useTransfers = !transferLog;
            MekHQ.triggerEvent(new OrganizationChangedEvent(campaign, prevFormation, unit));
        }

        if (null != formation) {
            unit.setFormationId(id);
            unit.setScenarioId(formation.getScenarioId());
            if (null != formation.getTechID()) {
                final UUID id1 = formation.getTechID();
                Person formationTech = campaign.getPlayerForce().getHumanResources().getPerson(id1);
                if (formationTech.canTech(unit.getEntity())) {
                    if (null != unit.getTech()) {
                        unit.removeTech();
                    }

                    unit.setTech(formationTech);
                } else {
                    String cantTech = formationTech.getFullName() +
                                            " cannot maintain " +
                                            unit.getName() +
                                            '\n' +
                                            "You will need to assign a tech manually.";
                    JOptionPane.showMessageDialog(null, cantTech, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
            formation.addUnit(campaign, unit.getId(), useTransfers, prevFormation);
            MekHQ.triggerEvent(new OrganizationChangedEvent(campaign, formation, unit));
        }

        if (campaign.getCampaignOptions().isUseStratCon()) {
            recalculateCombatTeams(campaign);
        }
    }

    /**
     * Adds formation and all its sub-formations to the Combat Teams table.
     */
    public void addAllCombatTeams(Formation formation, Campaign campaign) {
        recalculateCombatTeams(campaign);

        for (Formation subFormation : formation.getSubFormations()) {
            addAllCombatTeams(subFormation, campaign);
        }
    }

    /**
     * Removes {@code formation} from the TOE: unassigns its units (clearing their scenario if it was deployed), removes
     * it from any scenario it was deployed to and from its parent formation, and clears any StratCon track assignments.
     * The {@link Campaign} is supplied as a parameter for the scenario, contract, and combat-team updates this drives.
     */
    public void removeFormation(Formation formation, Campaign campaign) {
        int formationId = formation.getId();
        formationIds.remove(formationId);
        // clear formationIds of all personnel with this formation
        for (UUID unitId : formation.getUnits()) {
            Unit unit = requireSingleDetachment().getHangar().getUnit(unitId);
            if (null == unit) {
                continue;
            }
            if (unit.getFormationId() == formationId) {
                unit.setFormationId(FORMATION_NONE);
                if (formation.isDeployed()) {
                    unit.setScenarioId(NO_ASSIGNED_SCENARIO);
                }
            }
        }

        // also remove this formation's id from any scenarios
        if (formation.isDeployed()) {
            Scenario scenario = campaign.getScenario(formation.getScenarioId());
            scenario.removeFormation(formationId);
        }

        if (null != formation.getParentFormation()) {
            formation.getParentFormation().removeSubFormation(formationId);
        }

        // clear out StratCon formation assignments
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (contract.getStratConCampaignState() != null) {
                for (StratConTrackState track : contract.getStratConCampaignState().getTracks()) {
                    track.unassignFormation(formationId);
                }
            }
        }

        if (campaign.getCampaignOptions().isUseStratCon()) {
            recalculateCombatTeams(campaign);
        }
    }

    /**
     * Removes {@code u} from its current formation, clearing its formation and scenario assignments and detaching it
     * from any C3 networks. When StratCon is enabled and this empties the formation, the formation is dropped from the
     * combat-teams table. The {@link Campaign} is supplied as a parameter for the game and combat-team updates this
     * drives.
     */
    public void removeUnitFromFormation(Unit unit, Campaign campaign) {
        Formation formation = getFormation(unit.getFormationId());
        if (null != formation) {
            formation.removeUnit(campaign, unit.getId(), true);
            unit.setFormationId(FORMATION_NONE);
            unit.setScenarioId(NO_ASSIGNED_SCENARIO);
            detachUnitFromC3Networks(unit, campaign.getGame());

            if (campaign.getCampaignOptions().isUseStratCon() && formation.getUnits().isEmpty()) {
                combatTeams.remove(formation.getId());
            }
        }
    }

    /**
     * @return the {@link Formation} {@code unit} is currently assigned to, or {@code null} if {@code unit} is
     *       {@code null} or unassigned
     */
    public @Nullable Formation getFormationFor(final @Nullable Unit unit) {
        return (unit == null) ? null : getFormation(unit.getFormationId());
    }

    /**
     * Resolves the {@link Formation} a person belongs to: the formation of their assigned unit if they crew one,
     * otherwise — for techs — the formation they are the assigned tech of.
     *
     * @return the person's formation, or {@code null} if none applies
     */
    public @Nullable Formation getFormationFor(final Person person) {
        final Unit unit = person.getUnit();
        if (unit != null) {
            return getFormationFor(unit);
        } else if (person.isTech()) {
            for (Formation formation : formationIds.values()) {
                if (person.getId().equals(formation.getTechID())) {
                    return formation;
                }
            }
        }

        return null;
    }

    /**
     * Rebuilds this force's C3, Naval C3, and C3i networks from the units' stored C3 UUIDs. The MegaMek {@link Game} is
     * supplied as a parameter (the force holds no game reference) because network membership is resolved against the
     * live game entities.
     */
    public void refreshNetworks(Game game) {
        for (Unit unit : allUnits()) {
            // we are going to rebuild the c3, nc3 and c3i networks based on the c3UUIDs
            Entity entity = unit.getEntity();
            if (null != entity && (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3())) {
                boolean C3iSet = false;
                boolean NC3Set = false;

                for (Entity e : game.getEntitiesVector()) {
                    // C3 Checks
                    if (entity.hasC3()) {
                        if ((entity.getC3MasterIsUUIDAsString() != null) &&
                                  entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                            entity.setC3Master(e, false);
                            break;
                        }
                    }
                    // Naval C3 checks
                    if (entity.hasNavalC3() && !NC3Set) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        // Well, they're the same value of 6...
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getNC3NextUUIDAsString(pos) != null) &&
                                      (e.getC3UUIDAsString() != null) &&
                                      entity.getNC3NextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                NC3Set = true;
                                break;
                            }

                            pos++;
                        }
                    }
                    // C3i Checks
                    if (entity.hasC3i() && !C3iSet) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getC3iNextUUIDAsString(pos) != null) &&
                                      (e.getC3UUIDAsString() != null) &&
                                      entity.getC3iNextUUIDAsString(pos).equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                C3iSet = true;
                                break;
                            }

                            pos++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Disbands the entire C3i/Naval C3 network that {@code u} belongs to, clearing the stored next-node UUIDs of every
     * unit on that network and refreshing networks against the supplied {@link Game}.
     */
    public void disbandNetworkOf(Unit u, Game game) {
        // collect all the other units on this network to rebuild the uuids
        Vector<Unit> networkedUnits = new Vector<>();
        for (Unit unit : allUnits()) {
            if (null != unit.getEntity().getC3NetId() &&
                      unit.getEntity().getC3NetId().equals(u.getEntity().getC3NetId())) {
                networkedUnits.add(unit);
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                if (nUnit.getEntity().hasNavalC3()) {
                    nUnit.getEntity().setNC3NextUUIDAsString(pos, null);
                } else {
                    nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                }
            }
        }
        refreshNetworks(game);
        MekHQ.triggerEvent(new NetworkChangedEvent(networkedUnits));
    }

    /**
     * Removes {@code removedUnits} from their shared C3i/Naval C3 network and rebuilds the remaining members' node
     * UUIDs so the network stays contiguous, refreshing networks against the supplied {@link Game}. All removed units
     * are assumed to share the first unit's network id.
     */
    public void removeUnitsFromNetwork(Vector<Unit> removedUnits, Game game) {
        // collect all the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<>();
        Vector<Unit> networkedUnits = new Vector<>();
        String network = removedUnits.getFirst().getEntity().getC3NetId();
        for (Unit unit : allUnits()) {
            if (removedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId() && unit.getEntity().getC3NetId().equals(network)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit u : removedUnits) {
                if (u.getEntity().hasNavalC3()) {
                    u.getEntity().setNC3NextUUIDAsString(pos, null);
                } else {
                    u.getEntity().setC3iNextUUIDAsString(pos, null);
                }
            }
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, uuids.get(pos));
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, uuids.get(pos));
                    }
                } else {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, null);
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                    }
                }
            }
        }
        refreshNetworks(game);
    }

    /**
     * Adds {@code addedUnits} to the C3i/Naval C3 network identified by {@code networkID}, rebuilding every member's
     * node UUIDs, and refreshes networks against the supplied {@link Game}.
     */
    public void addUnitsToNetwork(Vector<Unit> addedUnits, String networkID, Game game) {
        // collect all the other units on this network to rebuild the uuids
        Vector<String> uuids = new Vector<>();
        Vector<Unit> networkedUnits = new Vector<>();
        for (Unit u : addedUnits) {
            uuids.add(u.getEntity().getC3UUIDAsString());
            networkedUnits.add(u);
        }
        for (Unit unit : allUnits()) {
            if (addedUnits.contains(unit)) {
                continue;
            }
            if (null != unit.getEntity().getC3NetId() && unit.getEntity().getC3NetId().equals(networkID)) {
                networkedUnits.add(unit);
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
        }
        for (int pos = 0; pos < Entity.MAX_C3i_NODES; pos++) {
            for (Unit nUnit : networkedUnits) {
                if (pos < uuids.size()) {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, uuids.get(pos));
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, uuids.get(pos));
                    }
                } else {
                    if (nUnit.getEntity().hasNavalC3()) {
                        nUnit.getEntity().setNC3NextUUIDAsString(pos, null);
                    } else {
                        nUnit.getEntity().setC3iNextUUIDAsString(pos, null);
                    }
                }
            }
        }
        refreshNetworks(game);
        MekHQ.triggerEvent(new NetworkChangedEvent(addedUnits));
    }

    /**
     * @return one entry per C3i network in the TOE that has at least one free node; each entry is a
     *       {@code {networkId, freeNodeCount}} pair
     */
    public Vector<String[]> getAvailableC3iNetworks() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : allUnits()) {

            if (u.getFormationId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.hasC3i() && en.calculateFreeC3Nodes() <= 5 && en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[2];
                network[0] = en.getC3NetId();
                network[1] = "" + en.calculateFreeC3Nodes();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }
        return networks;
    }

    /**
     * @return one entry per Naval C3 network in the TOE that has at least one free node; each entry is a
     *       {@code {networkId, freeNodeCount}} pair. Naval C3 mirrors C3i, so this parallels
     *       {@link #getAvailableC3iNetworks()}.
     */
    public Vector<String[]> getAvailableNC3Networks() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : allUnits()) {

            if (u.getFormationId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.hasNavalC3() && en.calculateFreeC3Nodes() <= 5 && en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[2];
                network[0] = en.getC3NetId();
                network[1] = "" + en.calculateFreeC3Nodes();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }
        return networks;
    }

    /**
     * @return one entry per Nova CEWS network in the TOE that has at least one free node; each entry is a
     *       {@code {networkId, freeNodeCount}} pair. Nova CEWS networks hold at most three units.
     */
    public Vector<String[]> getAvailableNovaCEWSNetworks() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : allUnits()) {

            if (u.getFormationId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            // Nova CEWS max is 3 nodes, so unnetworked unit has 2 free nodes
            if (en.hasNovaCEWS() && en.calculateFreeC3Nodes() <= 2 && en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[2];
                network[0] = en.getC3NetId();
                network[1] = "" + en.calculateFreeC3Nodes();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }
        return networks;
    }

    /**
     * @return the C3 masters in the TOE that can accept a slave (have a free C3 node), excluding company-level masters
     *       whose free-node count is unreliable; each entry is a {@code {c3UUID, freeNodeCount, shortName}} triple
     */
    public Vector<String[]> getAvailableC3MastersForSlaves() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : allUnits()) {

            if (u.getFormationId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            // count of free c3 nodes for single company-level masters
            // will not be right so skip
            if (en.hasC3M() && !en.hasC3MM() && en.C3MasterIs(en)) {
                continue;
            }
            if (en.calculateFreeC3Nodes() > 0) {
                String[] network = new String[3];
                network[0] = en.getC3UUIDAsString();
                network[1] = "" + en.calculateFreeC3Nodes();
                network[2] = en.getShortName();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }

        return networks;
    }

    /**
     * @return the C3 masters in the TOE that can accept another master (have a free C3-M node); each entry is a
     *       {@code {c3UUID, freeMasterNodeCount, shortName}} triple
     */
    public Vector<String[]> getAvailableC3MastersForMasters() {
        Vector<String[]> networks = new Vector<>();
        Vector<String> networkNames = new Vector<>();

        for (Unit u : allUnits()) {

            if (u.getFormationId() < 0) {
                // only units currently in the TO&E
                continue;
            }
            Entity en = u.getEntity();
            if (null == en) {
                continue;
            }
            if (en.calculateFreeC3MNodes() > 0) {
                String[] network = new String[3];
                network[0] = en.getC3UUIDAsString();
                network[1] = "" + en.calculateFreeC3MNodes();
                network[2] = en.getShortName();
                if (!networkNames.contains(network[0])) {
                    networks.add(network);
                    networkNames.add(network[0]);
                }
            }
        }

        return networks;
    }

    /**
     * Detaches every unit slaved to {@code master} from it, clearing their C3 master reference and refreshing networks
     * against the supplied {@link Game}.
     */
    public void removeUnitsFromC3Master(Unit master, Game game) {
        List<Unit> removed = new ArrayList<>();
        for (Unit unit : allUnits()) {
            if (null != unit.getEntity().getC3MasterIsUUIDAsString() &&
                      unit.getEntity().getC3MasterIsUUIDAsString().equals(master.getEntity().getC3UUIDAsString())) {
                unit.getEntity().setC3MasterIsUUIDAsString(null);
                unit.getEntity().setC3Master(null, true);
                removed.add(unit);
            }
        }
        refreshNetworks(game);
        MekHQ.triggerEvent(new NetworkChangedEvent(removed));
    }

    /**
     * Detaches {@code u} from any C3 network it participates in (Naval C3, C3i, Nova CEWS, or a C3 master), refreshing
     * the affected networks against the supplied {@link Game}.
     */
    public void detachUnitFromC3Networks(Unit u, Game game) {
        if (u.getEntity().hasNavalC3() && u.getEntity().calculateFreeC3Nodes() < 5) {
            Vector<Unit> removedUnits = new Vector<>();
            removedUnits.add(u);
            removeUnitsFromNetwork(removedUnits, game);
            u.getEntity().setC3MasterIsUUIDAsString(null);
            u.getEntity().setC3Master(null, true);
            refreshNetworks(game);
        } else if (u.getEntity().hasC3i() && u.getEntity().calculateFreeC3Nodes() < 5) {
            Vector<Unit> removedUnits = new Vector<>();
            removedUnits.add(u);
            removeUnitsFromNetwork(removedUnits, game);
            u.getEntity().setC3MasterIsUUIDAsString(null);
            u.getEntity().setC3Master(null, true);
            refreshNetworks(game);
        } else if (u.getEntity().hasNovaCEWS() && u.getEntity().calculateFreeC3Nodes() < 2) {
            // Nova CEWS max is 3 nodes, so < 2 free means unit is networked
            Vector<Unit> removedUnits = new Vector<>();
            removedUnits.add(u);
            removeUnitsFromNetwork(removedUnits, game);
            u.getEntity().setC3MasterIsUUIDAsString(null);
            u.getEntity().setC3Master(null, true);
            refreshNetworks(game);
        }
        if (u.getEntity().hasC3M()) {
            removeUnitsFromC3Master(u, game);
            u.getEntity().setC3MasterIsUUIDAsString(null);
            u.getEntity().setC3Master(null, true);
        }
    }

}

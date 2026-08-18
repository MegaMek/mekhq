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
package mekhq.campaign.mission.newContract;

import static mekhq.campaign.mission.enums.ContractMoraleLevel.MAXIMUM_MORALE_LEVEL;
import static mekhq.campaign.mission.enums.ContractMoraleLevel.MINIMUM_MORALE_LEVEL;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.SupportPointNegotiation;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.ContractMoraleLevel;
import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.newContract.contractData.*;
import mekhq.campaign.mission.newContract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.newContract.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * This is the base template for all ChaosContract subclasses.
 *
 * <p>Note for reviewers: this is intended to be a pure data class, with no logic. The class that preceded it ended up
 * getting incredibly bloated with logic that ballooned the class into an unmanageable size. Please keep this class, and
 * its inheritors, as thin as possible.</p>
 */
public abstract class AbstractContract {
    /**
     * Support points negotiable per required combat team at the start of a contract. Sole definition of the rule - read
     * it through {@link #getMaximumSupportPoints()} rather than recomputing it.
     */
    public static final int INITIAL_SUPPORT_POINTS_PER_COMBAT_TEAM = 3;

    private UUID contractId;
    private String contractName;
    private @Nonnull String description = "";

    private EmployerData employerData;
    private EnemyData enemyData;

    private ContractTermsData contractTerms;
    private ContractObjectiveData objectiveData;
    private ContractFinanceData contractFinanceData;

    private @Nonnull Money salvagedByUnitValue = Money.zero();
    private @Nonnull Money salvagedByEmployerValue = Money.zero();

    private MissionStatus missionStatus;
    private ContractScheduleData scheduleData;
    private SystemsTargetData systemsTargetData;

    private RentedFacilitiesData rentedFacilitiesData;
    /**
     * Seeded with neutral morale so a freshly-constructed contract is always well-formed: generation performs its first
     * morale check (which reads the current level as its baseline) before any morale data is assigned.
     */
    private @Nonnull MoraleData moraleData = new MoraleData(ContractMoraleLevel.STALEMATE);
    private NegotiationData negotiationData;
    private Person playerNegotiator;
    /**
     * The id of {@link #playerNegotiator} as read from a save, held until it can be resolved.
     *
     * <p>Contracts are written inside {@code <info>}, which the loader parses before the personnel roster exists, so
     * the negotiator cannot be looked up while the contract itself is being read. The codec stashes the raw id here and
     * the loader resolves it in a post-load pass once the whole save has been read. Never serialized - the negotiator
     * is written from {@link #playerNegotiator}.</p>
     */
    private transient UUID pendingPlayerNegotiatorId;

    private StratConCampaignState stratConCampaignState;
    private int scale;
    private int requiredCombatElements;
    private int requiredVictoryPoints;
    private int trackCount; // TODO future proofing
    /** A "pity" contract: an easy top-up offer, surfaced in the market as a Proving Ground. */
    private boolean provingGround;

    private final List<Scenario> scenarios = new ArrayList<>();

    /*
     * This is a transient variable meant to keep track of a single jump path while the contract runs through initial
     * calculations, as the same jump path is referenced multiple times and calculating it each time is expensive. No
     * need to preserve it in save data.
     */
    private transient JumpPath cachedJumpPath;
    private transient int cachedContractDifficulty;

    public @Nonnull List<Scenario> getScenarios() {
        return scenarios;
    }

    /**
     * Don't use this method directly as it will not add an id to the added scenario. Use Campaign#AddScenario instead
     *
     * @param scenario the scenario to add this mission
     */
    public void addScenario(final Scenario scenario) {
        scenario.setMissionId(getId());
        getScenarios().add(scenario);
    }

    public List<Scenario> getVisibleScenarios() {
        List<Scenario> filteredScenarios = new ArrayList<>();

        for (Scenario scenario : getScenarios()) {
            if (!scenario.isCloaked()) {
                filteredScenarios.add(scenario);
            }
        }

        return filteredScenarios;
    }

    public List<Scenario> getCurrentScenarios() {
        List<Scenario> filteredScenarios = new ArrayList<>();

        for (Scenario scenario : getScenarios()) {
            if (scenario.getStatus().isCurrent()) {
                filteredScenarios.add(scenario);
            }
        }

        return filteredScenarios;
    }

    public List<Scenario> getCompletedScenarios() {
        List<Scenario> filteredScenarios = new ArrayList<>();

        for (Scenario scenario : getScenarios()) {
            if (!scenario.getStatus().isCurrent()) {
                filteredScenarios.add(scenario);
            }
        }

        return filteredScenarios;
    }

    public List<AtBScenario> getCurrentAtBScenarios() {
        List<AtBScenario> filteredScenarios = new ArrayList<>();

        for (Scenario scenario : getScenarios()) {
            if (scenario instanceof AtBScenario atBScenario && atBScenario.getStatus().isCurrent()) {
                filteredScenarios.add(atBScenario);
            }
        }

        return filteredScenarios;
    }

    public void clearScenarios() {
        scenarios.clear();
    }

    public UUID getId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public String getName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    /**
     * @return the contract's name, so UI components that render a contract directly (such as the Briefing Room's
     *       current-mission selector) show it by name rather than by object identity
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the name of this object as an HTML hyperlink.
     *
     * <p>The hyperlink is formatted with a "MISSION:" protocol prefix followed by the object's ID. This allows UI
     * components that support HTML to render the name as a clickable link, which can be used to navigate to or focus on
     * this specific object when clicked.</p>
     *
     * @return An HTML formatted string containing the object's name as a hyperlink with its ID
     *
     * @author Illiani
     * @since 0.50.05
     */
    public String getHyperlinkedName() {
        return String.format("<a href='MISSION:%s'>%s</a>", getId(), getName());
    }

    public @Nonnull String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EmployerData getEmployerData() {
        return employerData;
    }

    public void setEmployerData(EmployerData employerData) {
        this.employerData = employerData;
    }

    public EnemyData getEnemyData() {
        return enemyData;
    }

    public void setEnemyData(EnemyData enemyData) {
        this.enemyData = enemyData;
    }

    public ContractTermsData getContractTerms() {
        return contractTerms;
    }

    public void setContractTerms(ContractTermsData contractTerms) {
        this.contractTerms = contractTerms;
    }

    public ContractObjectiveData getObjectiveData() {
        return objectiveData;
    }

    public void setObjectiveData(ContractObjectiveData objectiveData) {
        this.objectiveData = objectiveData;
    }

    public ContractFinanceData getContractFinanceData() {
        return contractFinanceData;
    }

    public void setContractFinanceData(ContractFinanceData contractFinanceData) {
        this.contractFinanceData = contractFinanceData;
    }

    public void updateMonthlyPay(Money newMoney) {
        contractFinanceData = new ContractFinanceData(contractFinanceData, null, newMoney, null);
    }

    public void updateTransportPay(Money newMoney) {
        contractFinanceData = new ContractFinanceData(contractFinanceData, newMoney, null, null);
    }

    public void updateCombatPay(Money newMoney) {
        contractFinanceData = new ContractFinanceData(contractFinanceData, null, null, newMoney);
    }

    /**
     * @return this contract's status, or {@code null} for a market offer that has not been accepted - an offer is not a
     *       mission yet, and {@link ContractXmlCodec} persists the absent status as such
     */
    public @Nullable MissionStatus getStatus() {
        return missionStatus;
    }

    public void setStatus(MissionStatus missionStatus) {
        this.missionStatus = missionStatus;
    }

    public ContractScheduleData getScheduleData() {
        return scheduleData;
    }

    public void setScheduleData(ContractScheduleData scheduleData) {
        this.scheduleData = scheduleData;
    }

    public void updateScheduleData(@Nullable LocalDate newStartDate, @Nullable LocalDate newEndDate) {
        setScheduleData(new ContractScheduleData(scheduleData, newStartDate, newEndDate));
    }

    public boolean isActiveOn(LocalDate date) {
        return scheduleData.isActiveOn(date);
    }

    public SystemsTargetData getSystemsTargetData() {
        return systemsTargetData;
    }

    public void setSystemsTargetData(SystemsTargetData systemsTargetData) {
        this.systemsTargetData = systemsTargetData;
    }

    public RentedFacilitiesData getRentedFacilitiesData() {
        return rentedFacilitiesData;
    }

    public void setRentedFacilitiesData(RentedFacilitiesData rentedFacilitiesData) {
        this.rentedFacilitiesData = rentedFacilitiesData;
    }

    public @Nonnull MoraleData getMoraleData() {
        return moraleData;
    }

    public void setMoraleData(@Nonnull MoraleData moraleData) {
        this.moraleData = moraleData;
    }

    public void changeMorale(ContractMoraleLevel newMoraleLevel) {
        setMoraleData(new MoraleData(newMoraleLevel, null, Money.zero()));
    }

    public void changeMorale(ContractMoraleLevel newMoraleLevel, @Nullable LocalDate newRoutEndDate) {
        setMoraleData(new MoraleData(newMoraleLevel, newRoutEndDate, Money.zero()));
    }

    public void changeMorale(ContractMoraleLevel newMoraleLevel, @Nullable LocalDate newRoutEndDate,
          @Nonnull Money newRoutPayout) {
        setMoraleData(new MoraleData(newMoraleLevel, newRoutEndDate, newRoutPayout));
    }

    public void changeMorale(LocalDate newRoutEndDate) {
        setMoraleData(new MoraleData(moraleData.moraleLevel(), newRoutEndDate, Money.zero()));
    }

    public void changeMorale(LocalDate newRoutEndDate, Money newRoutPayout) {
        setMoraleData(new MoraleData(moraleData.moraleLevel(), newRoutEndDate, newRoutPayout));
    }

    /**
     * Adjusts the current {@link ContractMoraleLevel} by the specified delta and returns the resulting morale level.
     *
     * <p>The method computes a new integer morale value by adding the given {@code delta} to the unit's current
     * morale level, then clamps the result to the valid range defined by {@code MINIMUM_MORALE_LEVEL} and
     * {@code MAXIMUM_MORALE_LEVEL}. It then attempts to resolve the resulting value to a corresponding
     * {@link ContractMoraleLevel}.</p>
     *
     * <p>If the resolved morale level is valid (i.e., non-{@code null}), the unit's internal morale state is updated.
     * If no valid enum constant exists for the computed level, the method leaves the current morale unchanged and
     * returns the existing level.</p>
     *
     * <p><b>Note:</b> a positive delta improves the enemy morale, a negative delta decreases enemy morale.</p>
     *
     * @param delta the amount to adjust the current morale level by; may be positive or negative
     *
     * @return the new {@link ContractMoraleLevel} after applying the delta; if no corresponding morale level exists for
     *       the computed value, the current morale level is returned unchanged
     *
     * @author Illiani
     * @since 0.50.10
     */
    public ContractMoraleLevel changeMorale(final int delta) {
        int currentLevel = getMoraleLevel().getLevel();
        int newLevel = Math.clamp(currentLevel + delta, MINIMUM_MORALE_LEVEL, MAXIMUM_MORALE_LEVEL);

        ContractMoraleLevel newMoraleLevel = ContractMoraleLevel.parseFromLevel(newLevel);
        if (newMoraleLevel != null) {
            changeMorale(newMoraleLevel);
        }

        return newMoraleLevel != null ? newMoraleLevel : getMoraleLevel();
    }

    public ContractMoraleLevel getMoraleLevel() {
        return moraleData.moraleLevel();
    }

    public @Nullable LocalDate getRoutEndDate() {
        return moraleData.routEndDate();
    }

    public Money getRoutPayout() {
        return moraleData.routedPayout();
    }

    public @Nullable NegotiationData getNegotiationData() {
        return negotiationData;
    }

    public void setNegotiationData(NegotiationData negotiationData) {
        this.negotiationData = negotiationData;
    }

    public @Nullable Person getPlayerNegotiator() {
        return playerNegotiator;
    }

    public void setPlayerNegotiator(Person playerNegotiator) {
        this.playerNegotiator = playerNegotiator;
    }

    /**
     * @return the unresolved negotiator id stashed during a save load, or {@code null} when there is nothing pending
     */
    public @Nullable UUID getPendingPlayerNegotiatorId() {
        return pendingPlayerNegotiatorId;
    }

    /**
     * Stashes the negotiator id read from a save so the loader can resolve it once the roster is populated. Cleared by
     * that pass; not for general use.
     *
     * @param pendingPlayerNegotiatorId the id to resolve later, or {@code null} to clear
     */
    public void setPendingPlayerNegotiatorId(final @Nullable UUID pendingPlayerNegotiatorId) {
        this.pendingPlayerNegotiatorId = pendingPlayerNegotiatorId;
    }

    public @Nullable StratConCampaignState getStratConCampaignState() {
        return stratConCampaignState;
    }

    public void setStratConCampaignState(@Nullable StratConCampaignState stratConCampaignState) {
        this.stratConCampaignState = stratConCampaignState;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getRequiredCombatElements() {
        return requiredCombatElements;
    }

    public void setRequiredCombatElements(int requiredCombatElements) {
        this.requiredCombatElements = requiredCombatElements;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    /** @return {@code true} if this is a pity ("Proving Ground") contract - an easy top-up offer for a struggling force */
    public boolean isProvingGround() {
        return provingGround;
    }

    public void setProvingGround(boolean provingGround) {
        this.provingGround = provingGround;
    }

    public int getRequiredVictoryPoints() {
        return requiredVictoryPoints;
    }

    public void setRequiredVictoryPoints(int requiredVictoryPoints) {
        this.requiredVictoryPoints = requiredVictoryPoints;
    }

    /**
     * Generally you want to use {@link ContractUtilities#getJumpPath(Campaign, AbstractContract, AbstractLocation)}
     */
    public @Nullable JumpPath getCachedJumpPathDirect() {
        return cachedJumpPath;
    }

    public void setCachedJumpPath(JumpPath cachedJumpPath) {
        this.cachedJumpPath = cachedJumpPath;
    }

    public int getCachedContractDifficulty() {
        return cachedContractDifficulty;
    }

    public void setCachedContractDifficulty(int cachedContractDifficulty) {
        this.cachedContractDifficulty = cachedContractDifficulty;
    }

    public ChaosEmployerType getEmployerType() {
        return employerData.type();
    }

    public String getEmployerFactionCode() {
        return employerData.factionCode();
    }

    public Faction getEmployerFaction() {
        return employerData.getFaction();
    }

    public String getEmployerDisplayName() {
        return employerData.displayName();
    }

    public Person getEmployerNegotiator() {
        return employerData.negotiator();
    }

    public Person getEmployerLiaison() {
        return employerData.liaison();
    }

    public SkillLevel getEmployerForceSkill() {
        return employerData.forceSkill();
    }

    public int getEmployerEquipmentRating() {
        return employerData.equipmentRating();
    }

    public Camouflage getEmployerCamouflage() {
        return employerData.camouflage();
    }

    /** You almost always need {@link #getEmployerCamouflage()} */
    public PlayerColour getEmployerColor() {
        return employerData.color();
    }

    public String getEnemyFactionCode() {
        return enemyData.factionCode();
    }

    public Faction getEnemyFaction() {
        return enemyData.getFaction();
    }

    /**
     * @return the code of the covert patron bankrolling the <em>enemy</em>, or {@code null} if it has none. The
     *       employer may have a sponsor of its own; reach that through {@link #getEmployerData()}.
     */
    public @Nullable String getEnemySponsorFactionCode() {
        return enemyData.sponsorFactionCode();
    }

    /**
     * @return the covert patron bankrolling the <em>enemy</em>, or {@code null} if it has none. The employer may have a
     *       sponsor of its own; reach that through {@link #getEmployerData()}.
     */
    public @Nullable Faction getEnemySponsorFaction() {
        final String sponsorFactionCode = enemyData.sponsorFactionCode();
        // Factions.getFaction hands back a blank Faction rather than null for an unknown code, so guard here or
        // callers get an empty placeholder that passes their null check.
        return (sponsorFactionCode == null) ? null : Factions.getInstance().getFaction(sponsorFactionCode);
    }

    public String getEnemyDisplayName() {
        return enemyData.displayName();
    }

    public SkillLevel getEnemyForceSkill() {
        return enemyData.forceSkill();
    }

    public int getEnemyEquipmentRating() {
        return enemyData.equipmentRating();
    }

    public Camouflage getEnemyCamouflage() {
        return enemyData.camouflage();
    }

    /** You almost always need {@link #getEnemyCamouflage()} */
    public PlayerColour getEnemyColour() {
        return enemyData.color();
    }

    public boolean isBatchallAccepted() {
        return enemyData.batchallAccepted();
    }

    public int getRentedHospitalBeds() {
        return rentedFacilitiesData.hospitalBeds();
    }

    public int getRentedKitchens() {
        return rentedFacilitiesData.kitchens();
    }

    public int getRentedHoldingCells() {
        return rentedFacilitiesData.holdingCells();
    }

    /**
     * @return the id of the system this contract is fought in, or {@code null} when none is known
     */
    public @Nullable String getTargetSystemId() {
        return systemsTargetData.systemId();
    }

    public @Nullable PlanetarySystem getTargetSystem() {
        return systemsTargetData.getSystem();
    }

    public String getTargetSystemName(LocalDate currentDate) {
        return systemsTargetData.getSystemName(currentDate);
    }

    public String getTargetPlanetId() {
        return systemsTargetData.planetId();
    }

    public @Nullable Planet getTargetPlanet() {
        return systemsTargetData.getPlanet();
    }

    public @Nullable String getTargetPlanetName(LocalDate currentDate) {
        return systemsTargetData.getPlanetName(currentDate);
    }

    public Money getTransportPayment() {
        return contractFinanceData.transport();
    }

    public Money getMonthlyPayOut() {
        return contractFinanceData.monthlyPay();
    }

    public Money getTotalMonthlyPay() {
        int contractLengthInMonths = scheduleData.lengthInMonths();
        return contractFinanceData.getTotalMonthlyPay(contractLengthInMonths);
    }

    public Money getTotalPay() {
        int contractLengthInMonths = scheduleData.lengthInMonths();
        return contractFinanceData.getTotalPay(contractLengthInMonths);
    }

    public ContractObjectiveType getObjectiveType() {
        return objectiveData.playerObjectiveType();
    }

    public ContractObjectiveType getOpposingObjectiveType() {
        return objectiveData.opposingObjectiveType();
    }

    /**
     * @return the day this contract begins, or {@code null} when its schedule is not settled
     */
    public @Nullable LocalDate getStartDate() {
        return scheduleData.startDate();
    }

    /**
     * @return the day this contract ends, or {@code null} when it is open-ended
     */
    public @Nullable LocalDate getEndingDate() {
        return scheduleData.endDate();
    }

    public int getLengthInMonths() {
        return scheduleData.lengthInMonths();
    }

    public ChaosContractStepsTable getCommandRightsStep() {
        return contractTerms.commandRights();
    }

    public ContractCommandRights getCommandRights() {
        return getCommandRightsStep().getContractCommandRights();
    }

    public ChaosContractStepsTable getBasePayRateStep() {
        return contractTerms.payRate();
    }

    public double getBasePayMultiplier() {
        return getBasePayRateStep().getBasePayMultiplier();
    }

    public ChaosContractStepsTable getSupportStep() {
        return contractTerms.support();
    }

    public double getSupportMultiplier() {
        return getSupportStep().getStraightSupportMultiplier();
    }

    public double getBattlefieldLossMultiplier() {
        return getSupportStep().getBattlefieldLossMultiplier();
    }

    public ChaosContractStepsTable getTransportStep() {
        return contractTerms.transport();
    }

    public double getTransportMultiplier() {
        return getTransportStep().getTransportMultiplier();
    }

    public ChaosContractStepsTable getSalvageRightsStep() {
        return contractTerms.salvageRights();
    }

    public double getSalvageRightsMultiplier() {
        return getSalvageRightsStep().getSalvageMultiplier();
    }

    public boolean isSalvageExchange() {
        return getSalvageRightsStep().isExchangeSalvage();
    }

    public boolean canSalvage() {
        return getSalvageRightsMultiplier() > 0;
    }

    public boolean isPlayerAttacker() {
        return getObjectiveType().getChaosObjectiveType().isAttacker();
    }

    public @Nonnull Money getSalvagedByEmployerValue() {
        return salvagedByEmployerValue;
    }

    public void setSalvagedByEmployerValue(@Nonnull Money salvagedByEmployerValue) {
        this.salvagedByEmployerValue = salvagedByEmployerValue;
    }

    public void changeSalvagedByEmployerValue(Money delta) {
        salvagedByEmployerValue = salvagedByEmployerValue.plus(delta);
    }

    public @Nonnull Money getSalvagedByUnitValue() {
        return salvagedByUnitValue;
    }

    public void setSalvagedByUnitValue(@Nonnull Money salvagedByUnitValue) {
        this.salvagedByUnitValue = salvagedByUnitValue;
    }

    public void changeSalvagedByUnitValue(Money delta) {
        salvagedByUnitValue = salvagedByUnitValue.plus(delta);
    }

    /**
     * @param localDate the day to count from
     *
     * @return whole months from {@code localDate} until this contract ends, never negative, and {@code 0} when it has
     *       no end date. Every caller multiplies this by the monthly pay to settle outstanding escrow, so neither an
     *       unsettled schedule nor a date past the contract's end owes anything.
     */
    public long getMonthsLeft(LocalDate localDate) {
        final LocalDate endingDate = getEndingDate();
        if (endingDate == null) {
            return 0;
        }

        return Math.max(0, ChronoUnit.MONTHS.between(localDate, endingDate));
    }

    public boolean isPeaceful() {
        return getObjectiveType().isGarrisonType() && getMoraleLevel().isRouted();
    }

    public void setStartAndEndDate(LocalDate localDate) {
        setScheduleData(new ContractScheduleData(scheduleData, localDate, localDate.plusMonths(getLengthInMonths())));
    }

    /**
     * @return the contract's available support points, or 0 when it has no StratCon campaign state (the player opted
     *       out of StratCon for this contract, or it came from a save that never had one)
     */
    public int getCurrentSupportPoints() {
        if (stratConCampaignState == null) {
            return 0;
        }

        return stratConCampaignState.getSupportPoints();
    }

    /**
     * Returns the support-point reserve this contract can be negotiated up to, used as the "full reserves" reference
     * when displaying support points, and the cap applied during initial support-point negotiation - which calls this
     * rather than repeating the rule (see {@link SupportPointNegotiation}).
     *
     * @return the maximum support points the contract can hold in reserve
     */
    public int getMaximumSupportPoints() {
        return scale * INITIAL_SUPPORT_POINTS_PER_COMBAT_TEAM;
    }
}

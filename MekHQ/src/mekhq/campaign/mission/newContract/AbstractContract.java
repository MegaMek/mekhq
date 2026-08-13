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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.ContractMoraleLevel;
import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.newContract.contractData.*;
import mekhq.campaign.mission.newContract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
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
    private UUID contractId;
    private String contractName;
    private String description;

    private EmployerData employerData;
    private EnemyData enemyData;

    private ContractTermsData contractTerms;
    private ContractObjectiveData objectiveData;
    private ContractFinanceData contractFinanceData;

    private MissionStatus missionStatus;
    private ContractScheduleData scheduleData;
    private SystemsTargetData systemsTargetData;

    private RentedFacilitiesData rentedFacilitiesData;
    private MoraleData moraleData;
    private NegotiationData negotiationData;
    private Person playerNegotiator;

    private StratConCampaignState stratConCampaignState;
    private int scale;
    private int trackCount; // TODO future proofing

    private final List<Scenario> scenarios = new ArrayList<>();

    /*
     * This is a transient variable meant to keep track of a single jump path while the contract runs through initial
     * calculations, as the same jump path is referenced multiple times and calculating it each time is expensive. No
     * need to preserve it in save data.
     */
    private transient JumpPath cachedJumpPath;
    private transient int cachedContractDifficulty;

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getDescription() {
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

    public MissionStatus getMissionStatus() {
        return missionStatus;
    }

    public void setMissionStatus(MissionStatus missionStatus) {
        this.missionStatus = missionStatus;
    }

    public ContractScheduleData getScheduleData() {
        return scheduleData;
    }

    public void setScheduleData(ContractScheduleData scheduleData) {
        this.scheduleData = scheduleData;
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

    public MoraleData getMoraleData() {
        return moraleData;
    }

    public void setMoraleData(MoraleData moraleData) {
        this.moraleData = moraleData;
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

    public StratConCampaignState getStratConCampaignState() {
        return stratConCampaignState;
    }

    public void setStratConCampaignState(StratConCampaignState stratConCampaignState) {
        this.stratConCampaignState = stratConCampaignState;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    /** Generally you want to use {@link ContractUtilities#getJumpPath(Campaign, AbstractContract, CurrentLocation)} */
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

    public ContractMoraleLevel getEnemyMoraleLevel() {
        return moraleData.moraleLevel();
    }

    public @Nullable LocalDate getRoutEndDate() {
        return moraleData.routEndDate();
    }

    public Money getRoutPayout() {
        return moraleData.routedPayout();
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

    public String getTargetSystemId() {
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

    public Money getMonthlyPay() {
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

    public LocalDate getStartDate() {
        return scheduleData.startDate();
    }

    public LocalDate getEndDate() {
        return scheduleData.endDate();
    }

    public int getContractLengthInMonths() {
        return scheduleData.lengthInMonths();
    }

    public ChaosContractStepsTable getCommandRightsStep() {
        return contractTerms.commandRights();
    }

    public ContractCommandRights getContractCommandRights() {
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

    public boolean isExchangeSalvage() {
        return getSalvageRightsStep().isExchangeSalvage();
    }

    public boolean isPlayerAttacker() {
        return getObjectiveType().getChaosObjectiveType().isAttacker();
    }
}

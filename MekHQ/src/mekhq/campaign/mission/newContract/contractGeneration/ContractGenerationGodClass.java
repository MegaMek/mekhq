package mekhq.campaign.mission.newContract.contractGeneration;

import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.Nonnull;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.AbstractContractManager;
import mekhq.campaign.mission.newContract.AbstractContractObjective;
import mekhq.campaign.mission.newContract.NormalContractManager;
import mekhq.campaign.mission.newContract.NormalContractObjective;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.jspecify.annotations.Nullable;

public class ContractGenerationGodClass {
    private static final MMLogger LOGGER = MMLogger.create(ContractGenerationGodClass.class);

    public ContractGenerationGodClass() {
    }

    public void generateContract(Campaign campaign, double forceReputationFactor, AbstractLocation currentLocation,
          Person negotiator) {
        AbstractContractManager parentContractManager = new NormalContractManager();

        LocalDate currentDate = campaign.getLocalDate();

        PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        HiringHallLevel hiringHallLevel = currentSystem == null ?
                                                HiringHallLevel.NONE :
                                                currentSystem.getHiringHallLevel(currentDate);

        // Pick employer
        EmployerFactionSelection employerFactionSelectionData = generateEmployerFaction(campaign,
              forceReputationFactor,
              currentLocation,
              negotiator,
              currentDate,
              hiringHallLevel,
              parentContractManager);

        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            return;
        }

        // Pick contract objectives
        List<AtBContractType> objectives = generateContractObjectives(campaign,
              negotiator,
              hiringHallLevel,
              employerFactionSelectionData,
              parentContractManager);
        if (objectives.isEmpty()) {
            LOGGER.error("Contract generated with no objectives. No contract generated.");
            return;
        }
        AtBContractType firstObjective = objectives.getFirst();

        // Pick enemy
        Faction enemyFaction = generateEnemyFactionForContractObjectives(currentLocation,
              currentDate,
              employerFactionSelectionData,
              firstObjective,
              parentContractManager);

        // Pick location
        String targetSystemId = generateContractLocation(currentLocation,
              firstObjective,
              employerFactionSelectionData,
              enemyFaction,
              parentContractManager);
        if (targetSystemId == null) {
            LOGGER.error("Somehow failed to generate a contract without a viable location. This shouldn't be possible.");
            return;
        }
    }

    private static @Nullable String generateContractLocation(AbstractLocation currentLocation,
          AtBContractType firstObjective, EmployerFactionSelection employerFactionSelectionData, Faction enemyFaction,
          AbstractContractManager parentContractManager) {
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Null employer passed into generateContractLocation. This should have been vetted already");
            return null;
        }

        // TODO change from short name to faction object
        String targetSystemId = ContractLocationDetermination.determineContractLocation(firstObjective,
              true,
              employerFaction.getShortName(),
              enemyFaction.getShortName(),
              currentLocation);
        if (targetSystemId == null) {
            LOGGER.error("Could not find target system for contract generation. No contract generated.");
            return null;
        }

        parentContractManager.setTargetSystemId(targetSystemId);

        return targetSystemId;
    }

    private static Faction generateEnemyFactionForContractObjectives(AbstractLocation currentLocation,
          LocalDate currentDate, EmployerFactionSelection employerFactionSelectionData, AtBContractType firstObjective,
          AbstractContractManager parentContractManager) {
        // Only the first objective is used to determine the initial enemy, as this is used to influence contract
        // location.
        Faction enemyFaction = ObjectiveEnemyDetermination.generateEnemyFactionForObjective(currentLocation,
              currentDate, employerFactionSelectionData.employerFaction(), firstObjective);
        String enemyFactionCode = enemyFaction.getShortName();
        for (AbstractContractObjective contractObjective : parentContractManager.getContractAllObjectivesCopy()) {
            contractObjective.setEnemyFactionCode(enemyFactionCode);
        }

        return enemyFaction;
    }

    private static List<AtBContractType> generateContractObjectives(Campaign campaign, Person negotiator,
          HiringHallLevel hiringHallLevel, EmployerFactionSelection employerFactionSelectionData,
          AbstractContractManager parentContractManager) {
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Null employer passed into generateContractObjectives. This should have been vetted already");
            return List.of();
        }

        List<AtBContractType> objectives = MissionObjectiveTypeDetermination.getObjectiveType(campaign,
              hiringHallLevel,
              negotiator,
              employerFaction,
              employerFactionSelectionData.globalEmployerTableValue(),
              employerFactionSelectionData.independentEmployerTableValue());
        for (AtBContractType objective : objectives) {
            AbstractContractObjective contractObjective = new NormalContractObjective();
            contractObjective.setObjectiveType(objective);
            parentContractManager.addContractObjective(contractObjective);
        }

        return objectives;
    }

    private static @Nonnull EmployerFactionSelection generateEmployerFaction(Campaign campaign,
          double forceReputationFactor, AbstractLocation currentLocation, Person negotiator, LocalDate currentDate,
          HiringHallLevel hiringHallLevel, AbstractContractManager parentContractManager) {
        Faction campaignFaction = campaign.getFaction();
        int adjustedConnectionsLevel = negotiator.getAdjustedConnections(false);
        ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(adjustedConnectionsLevel);
        int connectionsEquipLevel = connectionsLevel.getEquipLevel();

        EmployerFactionSelection employerFactionSelectionData =
              ContractEmployerDetermination.getEmployerFactionSelectionData(currentLocation,
                    connectionsEquipLevel,
                    campaignFaction,
                    currentDate,
                    hiringHallLevel,
                    forceReputationFactor);
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Could not find employer for contract generation. No contract generated.");
            return employerFactionSelectionData;
        }

        parentContractManager.setEmployerFactionCode(employerFaction.getShortName());
        return employerFactionSelectionData;
    }
}

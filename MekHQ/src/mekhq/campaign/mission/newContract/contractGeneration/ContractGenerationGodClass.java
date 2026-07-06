package mekhq.campaign.mission.newContract.contractGeneration;

import java.time.LocalDate;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractGenerationGodClass {
    private static final MMLogger LOGGER = MMLogger.create(ContractGenerationGodClass.class);

    public ContractGenerationGodClass() {
    }

    public void generateContract(Campaign campaign, double forceReputationFactor, AbstractLocation currentLocation,
          Person negotiator) {
        LocalDate currentDate = campaign.getLocalDate();
        Faction campaignFaction = campaign.getFaction();

        PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        HiringHallLevel hiringHallLevel = currentSystem == null ?
                                                HiringHallLevel.NONE :
                                                currentSystem.getHiringHallLevel(currentDate);

        int adjustedConnectionsLevel = negotiator.getAdjustedConnections(false);

        // Pick employer
        EmployerFactionSelection employerFactionSelectionData =
              ContractEmployerDetermination.getEmployerFactionSelectionData(currentLocation,
                    adjustedConnectionsLevel,
                    campaignFaction,
                    currentDate,
                    hiringHallLevel,
                    forceReputationFactor);
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Could not find employer for contract generation. No contract generated.");
            return;
        }

        // Pick contract type
        List<AtBContractType> objectives = MissionObjectiveTypeDetermination.getObjectiveType(campaign,
              hiringHallLevel,
              negotiator,
              employerFaction,
              employerFactionSelectionData.globalEmployerTableValue(),
              employerFactionSelectionData.independentEmployerTableValue());
        AtBContractType firstObjective = objectives.getFirst();

        // Pick enemy
        Faction enemyFaction = ObjectiveEnemyDetermination.generateEnemyFactionForObjective(currentLocation,
              currentDate, employerFaction, firstObjective);


        // Pick location

    }
}

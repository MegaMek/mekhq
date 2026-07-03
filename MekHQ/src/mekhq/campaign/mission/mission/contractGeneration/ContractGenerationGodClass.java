package mekhq.campaign.mission.mission.contractGeneration;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractGenerationGodClass {
    private final Campaign campaign;
    private final LocalDate currentDate;
    private final Faction campaignFaction;
    private final HiringHallLevel hiringHallLevel;
    private final ContractEmployerDetermination contractEmployerDetermination;
    private final double forceReputationFactor;
    private final AbstractLocation currentLocation;

    public ContractGenerationGodClass(Campaign campaign, HiringHallLevel hiringHallLevel, int adjustedConnectionsLevel,
          AbstractLocation currentLocation) {
        this.campaign = campaign;
        this.currentDate = campaign.getLocalDate();
        this.campaignFaction = campaign.getFaction();
        this.forceReputationFactor = campaign.getReputation().getReputationFactor();
        this.currentLocation = currentLocation;
        this.hiringHallLevel = hiringHallLevel;

        contractEmployerDetermination = new ContractEmployerDetermination(currentDate,
              campaignFaction,
              hiringHallLevel,
              forceReputationFactor,
              adjustedConnectionsLevel,
              currentLocation);
    }

    public void generateContract(Person negotiator) {
        EmployerFactionSelection employerFactionSelection = contractEmployerDetermination.getContractEmployer();
        List<AtBContractType> objectiveType = getObjectives(negotiator, employerFactionSelection);
    }

    private List<AtBContractType> getObjectives(Person negotiator, EmployerFactionSelection employerFactionSelection) {
        MissionObjectiveTypeDetermination objectiveTypeDetermination = new MissionObjectiveTypeDetermination(campaign,
              hiringHallLevel, negotiator, employerFactionSelection.employerFaction(),
              employerFactionSelection.globalEmployerTableValue(),
              employerFactionSelection.independentEmployerTableValue());

        return objectiveTypeDetermination.getObjectiveType();
    }
}

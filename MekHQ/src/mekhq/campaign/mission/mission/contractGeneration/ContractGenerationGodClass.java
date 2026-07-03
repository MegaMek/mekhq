package mekhq.campaign.mission.mission.contractGeneration;

import static mekhq.campaign.mission.mission.contractGeneration.ObjectiveEnemyDetermination.getEnemyFaction;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.time.LocalDate;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.mission.NormalContractObjective;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractGenerationGodClass {
    private static final MMLogger LOGGER = MMLogger.create(ContractGenerationGodClass.class);

    private final Campaign campaign;
    private final CampaignTypeForContractDetermination campaignType;
    private final LocalDate currentDate;
    private final Faction campaignFaction;
    private final HiringHallLevel hiringHallLevel;
    private final ContractEmployerDetermination contractEmployerDetermination;
    private final double forceReputationFactor;
    private final AbstractLocation currentLocation;

    public ContractGenerationGodClass(Campaign campaign, CampaignTypeForContractDetermination campaignType,
          HiringHallLevel hiringHallLevel,
          int adjustedConnectionsLevel,
          AbstractLocation currentLocation) {
        this.campaign = campaign;
        this.campaignType = campaignType;
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

        Faction employerFaction = employerFactionSelection.employerFaction();

        for (AtBContractType objective : objectiveType) {
            NormalContractObjective contractObjective = new NormalContractObjective();
            Faction enemyFaction = getEnemyFaction(campaignType, objective, employerFaction, currentDate);
        }

        if (campaignType == CampaignTypeForContractDetermination.PIRATE) {
            // Under CamOps pirates don't generate employers they generate targets, so employer is always pirates,
            // CamOps pg 39 rev 5th edition
            employerFaction = Factions.getInstance().getFaction(PIRATE_FACTION_CODE);
        }
    }

    private List<AtBContractType> getObjectives(Person negotiator, EmployerFactionSelection employerFactionSelection) {
        MissionObjectiveTypeDetermination objectiveTypeDetermination = new MissionObjectiveTypeDetermination(campaign,
              hiringHallLevel, negotiator, employerFactionSelection.employerFaction(),
              employerFactionSelection.globalEmployerTableValue(),
              employerFactionSelection.independentEmployerTableValue());

        return objectiveTypeDetermination.getObjectiveType();
    }
}

package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.campaign.mission.newContract.contractGeneration.ObjectiveEnemyDetermination.getEnemyFaction;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.time.LocalDate;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.NormalContractObjective;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
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


        List<AtBContractType> objectiveTypes = getObjectives(negotiator, employerFactionSelection);

        Faction employerFaction = employerFactionSelection.employerFaction();
        AtBContractType firstObjective = objectiveTypes.getFirst();
        Faction enemyFaction = getEnemyFaction(campaignType, firstObjective, employerFaction, currentDate);

        String targetSystemId = DetermineContractLocation.generateContractLocation(employerFaction, enemyFaction);
        PlanetarySystem targetPlanetarySystem = Systems.getInstance().getSystemById(targetSystemId);

        // TODO Cache in contract object. We'll need to check if current location changed at any point
        JumpPath jumpPath = LocationUtils.planJumpPath(currentLocation.getCurrentSystem(), targetPlanetarySystem,
              campaign);

        for (AtBContractType objective : objectiveTypes) {
            NormalContractObjective contractObjective = new NormalContractObjective();
        }

        // Under CamOps pirates don't generate employers they generate targets, so employer is always pirates, CamOps
        // pg 39 rev 5th edition
        if (campaignType == CampaignTypeForContractDetermination.PIRATE) {
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

package mekhq.campaign.personnel.randomEvents.prisoners;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerEscapeeScenarioDialog;

import java.util.*;

import static megamek.common.Board.START_SW;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.personnel.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.randomEvents.prisoners.enums.MobType.HUGE;
import static mekhq.campaign.personnel.randomEvents.prisoners.enums.MobType.LARGE;
import static mekhq.campaign.personnel.randomEvents.prisoners.enums.MobType.MEDIUM;
import static mekhq.campaign.personnel.randomEvents.prisoners.enums.MobType.SMALL;
import static mekhq.campaign.stratcon.StratconContractInitializer.getUnoccupiedCoords;
import static mekhq.campaign.stratcon.StratconRulesManager.generateExternalScenario;
import static mekhq.campaign.stratcon.StratconRulesManager.getAvailableForceIDs;
import static mekhq.campaign.stratcon.StratconRulesManager.sortForcesByMapType;

public class PrisonEscapeScenario {
    private final Campaign campaign;
    private final AtBContract contract;
    private Set<Person> escapees;

    private final MMLogger logger = MMLogger.create(PrisonEscapeScenario.class);

    public PrisonEscapeScenario(Campaign campaign, AtBContract contract, Set<Person> escapees) {
        this.campaign = campaign;
        this.contract = contract;
        this.escapees = escapees;

        List<Unit> allMobs = findMobsForEscapees();
        createEscapeeScenario(allMobs);
    }


    private List<Unit> findMobsForEscapees() {
        List<Unit> mobs = new ArrayList<>();

        while (!escapees.isEmpty()) {
            int escapeeCount = escapees.size();

            Entity mobEntity = createMobEntity(escapeeCount);

            if (mobEntity == null) {
                logger.info("Failed to create mob");
                return mobs;
            }

            Unit mobUnit = new Unit(mobEntity, campaign);
            mobUnit.clearCrew();

            Set<Person> assignedEscapees = new HashSet<>();
            int maximumCrewSize = mobUnit.getFullCrewSize();
            for (Person escapee : escapees) {
                // If they don't have small arms, they're about to learn fast
                // We need to give them the skill, as it's required for the SOLDIER role, which is
                // required for serving in a CI unit, which mobs are.
                if (!escapee.hasSkill(S_SMALL_ARMS)) {
                    escapee.addSkill(S_SMALL_ARMS, 0, 0);
                }

                escapee.setPrimaryRole(campaign, SOLDIER);
                assignedEscapees.add(escapee);

                if (assignedEscapees.size() == maximumCrewSize) {
                    break;
                }
            }

            Crew crew = mobUnit.getEntity().getCrew();
            crew.setSize(assignedEscapees.size());

            for (Person escapee : assignedEscapees) {
                mobUnit.addPilotOrSoldier(escapee, null, false);
            }

            mobs.add(mobUnit);
            escapees.removeAll(assignedEscapees);
        }

        return mobs;
    }

    private Entity createMobEntity(int escapeeCount) {
        if (escapeeCount <= SMALL.getMaximum()) {
            return createMob(SMALL.getName());
        }

        if (escapeeCount <= MEDIUM.getMaximum()) {
            return createMob(MEDIUM.getName());
        }

        if (escapeeCount <= LARGE.getMaximum()) {
            return createMob(LARGE.getName());
        }

        return createMob(HUGE.getName());
    }

    public @Nullable Entity createMob(String mobName) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(mobName);
        if (mekSummary == null) {
            logger.error("Cannot find entry for {}", mobName);
            return null;
        }

        MekFileParser mekFileParser;

        try {
            mekFileParser = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName());
        } catch (Exception ex) {
            logger.error("Unable to load unit: {}", mekSummary.getEntryName(), ex);
            return null;
        }

        return mekFileParser.getEntity();
    }

    private void createEscapeeScenario(List<Unit> mobUnits) {
        final String DIRECTORY = "data/scenariotemplates/";
        final String GENERIC = DIRECTORY + "Intercept the Escapees.xml";

        ScenarioTemplate template = ScenarioTemplate.Deserialize(GENERIC);

        // If we've failed to deserialize the requested template, report the error and make the delivery.
        if (template == null) {
            logger.info(String.format("Failed to deserialize %s", GENERIC));
            return;
        }

        // Pick a random track where the interception will take place. If we fail to get a track,
        // we log an error and make the delivery, in the same manner as above.
        StratconTrackState track;
        try {
            final StratconCampaignState campaignState = contract.getStratconCampaignState();
            List<StratconTrackState> tracks = campaignState.getTracks();
            track = ObjectUtility.getRandomItem(tracks);
        } catch (NullPointerException e) {
            logger.info(String.format("Failed to fetch a track: %s", e.getMessage()));
            return;
        }

        StratconCoords coords = getUnoccupiedCoords(track, false);

        if (coords == null) {
            logger.info("Failed to fetch a free set of coords");
            return;
        }

        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract, false);
        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs, campaign);

        int randomForceIndex = randomInt(availableForceIDs.size());
        int randomForceID = availableForceIDs.get(randomForceIndex);

        // remove the force from the available lists, so we don't designate it as primary twice
        boolean autoAssignLances = contract.getCommandRights().isIntegrated();
        if (autoAssignLances) {
            availableForceIDs.removeIf(id -> id.equals(randomForceIndex));

            // we want to remove the actual int with the value, not the value at the index
            sortedAvailableForceIDs.get(AllGroundTerrain).removeIf(id -> id.equals(randomForceID));
        }

        StratconScenario scenario = generateExternalScenario(campaign, contract, track, coords,
            template, false, 0);

        if (scenario == null) {
            logger.info("Failed to generate a scenario");
            return;
        }

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, and announce the situation to the player
        AtBScenario backingScenario = scenario.getBackingScenario();

        for (BotForce botForce : backingScenario.getBotForces()) {
            if (botForce.getName().contains("Escapees")) {
                for (Unit mobUnit : mobUnits) {
                    botForce.addEntity(mobUnit.getEntity());
                }

                botForce.setRetreatEdge(START_SW);
                botForce.getBehaviorSettings().setAutoFlee(true);
            }
        }

        // Trigger a dialog to inform the user an interception has taken place
        new PrisonerEscapeeScenarioDialog(campaign, track, coords);
    }
}

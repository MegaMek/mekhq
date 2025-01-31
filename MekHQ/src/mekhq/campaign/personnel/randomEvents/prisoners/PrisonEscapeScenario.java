package mekhq.campaign.personnel.randomEvents.prisoners;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
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
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;

import java.util.*;

import static megamek.common.Board.START_SW;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.personnel.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.stratcon.StratconRulesManager.generateExternalScenario;
import static mekhq.campaign.stratcon.StratconRulesManager.getAvailableForceIDs;
import static mekhq.campaign.stratcon.StratconRulesManager.sortForcesByMapType;

public class PrisonEscapeScenario {
    private final MMLogger logger = MMLogger.create(PrisonEscapeScenario.class);

    // Mobs
    public enum MobType {
        SMALL("Mob (Small)", 1, 5),
        MEDIUM("Mob (Medium)", 6, 10),
        LARGE("Mob (Large)", 11, 20),
        HUGE("Mob (Huge)", 21, 30);

        private final String name;
        private final int minimum;
        private final int maximum;

        /**
         * Constructor for MobType, which assigns attributes to each enum constant.
         *
         * @param name    the name of the mob
         * @param minimum the minimum value associated with the mob
         * @param maximum the maximum value associated with the mob
         */
        MobType(String name, int minimum, int maximum) {
            this.name = name;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        /**
         * Gets the name of this mob type.
         *
         * @return the name of the mob
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the minimum value associated with this mob type.
         *
         * @return the minimum value
         */
        public int getMinimum() {
            return minimum;
        }

        /**
         * Gets the maximum value associated with this mob type.
         *
         * @return the maximum value
         */
        public int getMaximum() {
            return maximum;
        }

        @Override
        public String toString() {
            return String.format("%s (Min: %d, Max: %d)", name, minimum, maximum);
        }
    }

    public PrisonEscapeScenario(Campaign campaign, AtBContract contract, Set<Person> escapees) {
        List<Unit> allMobs = findMobsForEscapees(campaign, escapees);

        createEscapeeScenario(campaign, contract, allMobs);
    }


    private List<Unit> findMobsForEscapees(Campaign campaign, Set<Person> escapees) {
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
            for (Person escapee : escapees) {
                if (mobUnit.isFullyCrewed()) {
                    break;
                }

                // If they don't have small arms, they're about to learn fast
                if (!escapee.hasSkill(S_SMALL_ARMS)) {
                    escapee.addSkill(S_SMALL_ARMS, 0, 0);
                }

                escapee.setPrimaryRole(campaign, SOLDIER);

                mobUnit.addPilotOrSoldier(escapee, null, false);
                assignedEscapees.add(escapee);
            }

            // This helps us remove any crew that have been 'invented' during the entity creation process.
            for (Person crewMember : mobUnit.getCrew()) {
                if (!assignedEscapees.contains(crewMember)) {
                    mobUnit.remove(crewMember, false);
                }
            }

            mobUnit.resetPilotAndEntity();

            mobs.add(mobUnit);
            escapees.removeAll(assignedEscapees);
        }

        return mobs;
    }

    private Entity createMobEntity(int escapeeCount) {
        if (escapeeCount <= MobType.SMALL.getMaximum()) {
            return createMob(MobType.SMALL.getName());
        }

        if (escapeeCount <= MobType.MEDIUM.getMaximum()) {
            return createMob(MobType.MEDIUM.getName());
        }

        if (escapeeCount <= MobType.LARGE.getMaximum()) {
            return createMob(MobType.LARGE.getName());
        }

        return createMob(MobType.HUGE.getName());
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

    private static void createEscapeeScenario(Campaign campaign, AtBContract contract, List<Unit> mobUnits) {
        final String DIRECTORY = "data/scenariotemplates/";
        final String GENERIC = DIRECTORY + "Intercept the Escapees.xml";

        // Trigger a dialog to inform the user an interception has taken place
//        new DialogInterception(resupply, targetConvoy);

        // Determine which scenario template to use based on convoy state
        ScenarioTemplate template = ScenarioTemplate.Deserialize(GENERIC);

        // If we've failed to deserialize the requested template, report the error and make the delivery.
        // We report the error in this fashion, instead of hiding it in the log, as we want to
        // increase the likelihood the player is aware an error has occurred.
        if (template == null) {
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
            return;
        }


        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract, false);
        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs, campaign);

        int randomForceIndex = randomInt(availableForceIDs.size());
        int randomForceID = availableForceIDs.get(randomForceIndex);

        // remove the force from the available lists, so we don't designate it as primary
        // twice
        boolean autoAssignLances = contract.getCommandRights().isIntegrated();
        if (autoAssignLances) {
            availableForceIDs.removeIf(id -> id.equals(randomForceIndex));

            // we want to remove the actual int with the value, not the value at the index
            sortedAvailableForceIDs.get(AllGroundTerrain).removeIf(id -> id.equals(randomForceID));
        }

        StratconScenario scenario = generateExternalScenario(campaign, contract, track, null, template, false, 0);

        if (scenario == null) {
            return;
        }

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, including assigning the Resupply contents as loot and
        // assigning a player convoy (if appropriate)
        // Announce the situation to the player
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
    }
}

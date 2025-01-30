package mekhq.campaign.personnel.prisoners;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;

import java.util.*;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
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
        List<MobType> allMobs = findMobsForEscapees(escapees.size());
        List<Entity> mobEntities = new ArrayList<>();

        for (MobType mob : allMobs) {
            Entity entity = createMob(mob.getName());

            if (entity != null) {
                mobEntities.add(entity);
            }
        }

        Set<Person> assignedEscapees;
        List<Unit> mobUnits = new ArrayList<>();
        for (Entity mobEntity : mobEntities) {
            assignedEscapees = new HashSet<>();

            Unit unit = new Unit(mobEntity, campaign);

            for (Person escapee : escapees) {
                if (!escapee.hasSkill(SkillType.S_SMALL_ARMS)) {
                    // If they didn't have the skill before, they're about to learn fast
                    escapee.addSkill(SkillType.S_SMALL_ARMS, 0, 0);
                }

                escapee.setPrimaryRole(campaign, PersonnelRole.SOLDIER);

                unit.addPilotOrSoldier(escapee, null, false);
                assignedEscapees.add(escapee);

                if (unit.getCrewState().isFullyCrewed() || assignedEscapees.size() == escapees.size()) {
                    escapees.removeAll(assignedEscapees);
                    mobUnits.add(unit);
                    break;
                }
            }
        }

        for (Unit mobUnit : mobUnits) {
            mobUnit.resetPilotAndEntity();
        }

        createEscapeeScenario(campaign, contract, mobEntities);
    }


    private List<MobType> findMobsForEscapees(int escapees) {
        if (escapees < 1) {
            throw new IllegalArgumentException("Number must be greater than or equal to 1.");
        }

        List<MobType> selectedMobs = new ArrayList<>();

        double escapeeGroups = (double) escapees / 5;
        while (escapeeGroups > 0) {
            if (escapeeGroups <= 1) {
                escapeeGroups -= 1;
                selectedMobs.add(MobType.SMALL);
                continue;
            }

            if (escapeeGroups <= 2) {
                escapeeGroups -= 2;
                selectedMobs.add(MobType.MEDIUM);
                continue;
            }

            if (escapeeGroups <= 3) {
                escapeeGroups -= 3;
                selectedMobs.add(MobType.LARGE);
                continue;
            }

            escapeeGroups -= 4;
            selectedMobs.add(MobType.HUGE);
        }

        return selectedMobs;
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

    private static void createEscapeeScenario(Campaign campaign, AtBContract contract, List<Entity> mobEntities) {
        final String DIRECTORY = "data/scenariotemplates/";
        final String GENERIC = DIRECTORY + "Emergency Convoy Defense.xml";

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
        MMLogger logger = MMLogger.create(StratconCampaignState.class);
        logger.info(randomForceID);

        if (scenario == null) {
            return;
        }

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, including assigning the Resupply contents as loot and
        // assigning a player convoy (if appropriate)
        // Announce the situation to the player
//        BotForce botForce = new BotForce("Escapees", 2, 0, mobEntities);
//        scenario.getBackingScenario().addBotForce(botForce, campaign);
    }
}

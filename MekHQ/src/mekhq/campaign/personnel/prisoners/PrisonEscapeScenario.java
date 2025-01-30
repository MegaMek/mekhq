package mekhq.campaign.personnel.prisoners;

import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public PrisonEscapeScenario(Campaign campaign, Set<Person> escapees) {
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
    }

    /**
     * Finds the smallest combination of {@link MobInfo} to match the given number of escapees.
     * <p>
     * This method sorts {@code mobs} by ascending minimum values and tries to create the smallest
     * possible combination of mobs to match the target number. It favors larger {@link MobInfo}
     * whenever possible.
     * </p>
     *
     * @param escapees the target number to match
     * @return a {@link List} of {@link MobInfo} objects representing the smallest combination
     * @throws IllegalArgumentException if the number of escapees is less than 1
     */
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
}

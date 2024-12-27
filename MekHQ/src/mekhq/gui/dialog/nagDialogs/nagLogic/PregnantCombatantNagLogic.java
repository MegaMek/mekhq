package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class PregnantCombatantNagLogic {
    /**
     * Checks if the current campaign contains any personnel who are pregnant and actively assigned
     * to a force.
     *
     * <p>
     * This method iterates through all active personnel in the campaign to determine if any
     * are pregnant. If a pregnant person is assigned to a unit that belongs to a combat force
     * (i.e., a force with an ID other than {@link Force#FORCE_NONE}), this method returns {@code true}.
     * Otherwise, it returns {@code false}.
     * </p>
     *
     * <p>
     * If there are no active missions in the campaign, the method short-circuits and immediately
     * returns {@code false}.
     * </p>
     *
     * @return {@code true} if there are pregnant personnel actively assigned to a combat force,
     * {@code false} otherwise.
     */
    public static boolean hasActivePregnantCombatant(Campaign campaign) {
        if (campaign.getActiveMissions(false).isEmpty()) {
            return false;
        }

        // there is no reason to use a stream here, as there won't be enough iterations to warrant it
        for (Person person : campaign.getActivePersonnel()) {
            if (person.isPregnant()) {
                Unit unit = person.getUnit();

                if (unit != null) {
                    if (unit.getForceId() != Force.FORCE_NONE) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}

package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

public class UntreatedPersonnelNagLogic {
    /**
     * Checks whether the campaign has any untreated personnel with injuries.
     *
     * <p>
     * This method iterates over the campaign's active personnel and identifies individuals
     * who meet the following criteria:
     * <ul>
     *     <li>The individual requires treatment ({@link Person#needsFixing()}).</li>
     *     <li>The individual has not been assigned to a doctor.</li>
     *     <li>The individual is not currently classified as a prisoner.</li>
     * </ul>
     * If any personnel match these conditions, the method returns {@code true}.
     *
     * @return {@code true} if untreated injuries are present, otherwise {@code false}.
     */
    public static boolean campaignHasUntreatedInjuries(Campaign campaign) {
        for (Person person : campaign.getActivePersonnel()) {
            if (!person.getPrisonerStatus().isCurrentPrisoner()
                && person.needsFixing()
                && person.getDoctorId() == null) {
                return true;
            }
        }
        return false;
    }
}

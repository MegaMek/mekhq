package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;

import java.time.DayOfWeek;

public class DeploymentShortfallNagLogic {
    /**
     * Checks if the campaign's active contracts have deployment deficits that need to be addressed.
     *
     * <p>
     * The following conditions are evaluated to determine whether the requirements for short
     * deployments are met:
     * <ul>
     *     <li>The campaign must currently be located on a planet. If it is not, the dialog is skipped.</li>
     *     <li>The check is performed weekly, only on Sundays, to avoid spamming the user daily.</li>
     *     <li>If any active AtB contract has a deployment deficit, the method returns {@code true}.</li>
     * </ul>
     * If none of these conditions are met, the method returns {@code false}.
     *
     * @return {@code true} if there are unmet deployment requirements; otherwise, {@code false}.
     */
    public static boolean hasDeploymentShortfall(Campaign campaign) {
        if (!campaign.getLocation().isOnPlanet()) {
            return false;
        }

        // this prevents the nag from spamming daily
        if (campaign.getLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
            return false;
        }

        // There is no need to use a stream here, as the number of iterations doesn't warrant it.
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (campaign.getDeploymentDeficit(contract) > 0) {
                return true;
            }
        }

        return false;
    }
}

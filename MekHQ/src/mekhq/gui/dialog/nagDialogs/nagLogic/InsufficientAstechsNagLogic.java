package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;

public class InsufficientAstechsNagLogic {
    public static boolean hasAsTechsNeeded(Campaign campaign) {
        int asTechsNeeded = campaign.getAstechNeed();

        return asTechsNeeded > 0;
    }
}

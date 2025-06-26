package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.ADOPTION_OR_LANCE;

import mekhq.campaign.Campaign;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionAccoladeDialog;

public class FactionAccoladeEvent {
    public FactionAccoladeEvent(Campaign campaign, String factionCode, FactionAccoladeLevel accoladeLevel,
          boolean isSameFaction) {
        new FactionAccoladeDialog(campaign, factionCode, accoladeLevel, isSameFaction);

        if (accoladeLevel.is(ADOPTION_OR_LANCE)) {
            if (isSameFaction) {
                // TODO handle gifting of a lance
            } else {
                // TODO handle adoption
            }
        }
    }
}

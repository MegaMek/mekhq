package mekhq.campaign.personnel.prisoners;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

import static megamek.common.Compute.d6;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;

public class MonthlyPrisonerEventPicker {
    private static int DEFAULT_EVENT_CHANCE = STALEMATE.ordinal();

    public enum PrisonerEventType {
        RANSOM_EVENT();
    }

    public static void rollForMonthlyPrisonerEvent(Campaign campaign) {
        if (campaign.hasActiveContract()) {
            Contract contract = campaign.getActiveContracts().get(0);

            int eventChance = DEFAULT_EVENT_CHANCE;
            if (contract instanceof AtBContract) {
                eventChance = ((AtBContract) contract).getMoraleLevel().ordinal();
            }

            int roll = d6(2);

            if (roll <= eventChance) {
                boolean isFriendlyPOWs = false;

                if (!campaign.getFriendlyPrisoners().isEmpty()) {
                    isFriendlyPOWs = d6(1) <= 2;
                }

                new PrisonerRansomEvent(campaign, isFriendlyPOWs);
            }
        }
    }
}

package mekhq.campaign.personnel.prisoners;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

import static megamek.common.Compute.d6;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;

public class MonthlyPrisonerEventPicker {
    private static int DEFAULT_EVENT_CHANCE = STALEMATE.ordinal();

    public static void rollForMonthlyPrisonerEvent(Campaign campaign) {
        if (campaign.hasActiveContract()) {
            Contract contract = campaign.getActiveContracts().get(0);

            int ransomEventChance = DEFAULT_EVENT_CHANCE;
            if (contract instanceof AtBContract) {
                ransomEventChance = ((AtBContract) contract).getMoraleLevel().ordinal();
            }

            int roll = d6(2);
            if (roll <= ransomEventChance) {
                boolean isFriendlyPOWs = false;

                if (!campaign.getFriendlyPrisoners().isEmpty()) {
                    isFriendlyPOWs = d6(1) <= 2;
                }

                new PrisonerRansomEvent(campaign, isFriendlyPOWs);
            }
        }
    }
}

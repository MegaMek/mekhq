package mekhq.campaign.chaosCampaign;

import java.math.BigDecimal;
import java.math.RoundingMode;

import mekhq.campaign.finances.Money;

public class ChaosCampaignUtilities {
    public static int SUPPORT_POINTS_TO_MONEY_CONVERSION = 10_000; // Chaos Campaign pg 28

    public static Money getMoneyFromChaosSupportPoints(int supportPoints) {
        return Money.of(supportPoints * SUPPORT_POINTS_TO_MONEY_CONVERSION);
    }

    public static int getChaosSupportPointsFromMoney(Money money) {
        Money reducedMoney = money.dividedBy(SUPPORT_POINTS_TO_MONEY_CONVERSION);
        BigDecimal reducedMoneyAmount = reducedMoney.getAmount();

        return reducedMoneyAmount
                     .setScale(0, RoundingMode.HALF_UP)
                     .min(BigDecimal.valueOf(Integer.MAX_VALUE))
                     .max(BigDecimal.valueOf(Integer.MIN_VALUE))
                     .intValue();
    }
}

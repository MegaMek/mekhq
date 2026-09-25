package mekhq.campaign.mission.scenarios.salvage;

import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;

/**
 * A {@link SalvageSettlement} for salvage exchange rights: all salvage goes to the employer, and the player is paid
 * their share of its value in cash.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ExchangeSalvageSettlement extends SalvageSettlement {
    ExchangeSalvageSettlement(double playerShare) {
        super(playerShare);
    }

    @Override
    public boolean canKeepSalvage() {
        return false;
    }

    @Override
    public Money getCashShare(Money employerSalvageValue) {
        return employerSalvageValue.multipliedBy(getPlayerShare());
    }

    @Override
    TransactionType getCashShareTransactionType() {
        return TransactionType.SALVAGE_EXCHANGE;
    }

    @Override
    String getCashShareReasonKey() {
        return "CamOpsSalvageUtilities.exchange";
    }
}

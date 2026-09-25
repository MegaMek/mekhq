package mekhq.campaign.mission.scenarios.salvage;

import mekhq.campaign.finances.Money;

/**
 * A {@link SalvageSettlement} for salvage purchases: there is no cap on salvage. For each wreck, the player is paid
 * their share of its value in cash, unless they buy the unit by paying the employer their share.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PurchaseSalvageSettlement extends SalvageSettlement {
    PurchaseSalvageSettlement(double playerShare) {
        super(playerShare);
    }

    @Override
    public boolean canKeepSalvage() {
        return true;
    }

    @Override
    public boolean isKeptSalvageBought() {
        return true;
    }

    @Override
    public Money getPurchaseCost(Money keptSalvageValue) {
        return keptSalvageValue.multipliedBy(1.0 - getPlayerShare());
    }

    @Override
    public Money getCashShare(Money employerSalvageValue) {
        return employerSalvageValue.multipliedBy(getPlayerShare());
    }
}

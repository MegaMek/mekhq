package mekhq.campaign.mission.scenarios.salvage;

/**
 * A {@link SalvageSettlement} where the contract's salvage rights cap how much of the salvage the player may claim, by
 * value. Claimed salvage can be kept or sold; the rest goes to the employer.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CappedSalvageSettlement extends SalvageSettlement {
    CappedSalvageSettlement(double playerShare) {
        super(playerShare);
    }

    @Override
    public boolean isSalvageCapped() {
        return true;
    }

    @Override
    public boolean canKeepSalvage() {
        return true;
    }

    @Override
    public boolean canSellSalvage() {
        return true;
    }
}

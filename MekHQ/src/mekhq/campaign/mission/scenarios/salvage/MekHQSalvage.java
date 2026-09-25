package mekhq.campaign.mission.scenarios.salvage;

import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The {@link SalvageSystem#MEKHQ MekHQ} salvage system: an expanded version of Campaign Operations salvage.
 *
 * <p>This is {@link CamOpsRevisedSalvage CamOps (Revised)}, except that the salvage rights set the player's share of
 * each wreck's value rather than capping what they may claim. For each recovered wreck, the player receives their
 * share in cash unless they buy the unit from the employer (see {@link SalvageSettlement#forSalvagePurchases}).
 * Wrecks the salvage teams don't recover are lost.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class MekHQSalvage extends CamOpsRevisedSalvage {
    @Override
    public SalvageSettlement createSettlement(AbstractContract contract) {
        return SalvageSettlement.forSalvagePurchases(contract);
    }
}

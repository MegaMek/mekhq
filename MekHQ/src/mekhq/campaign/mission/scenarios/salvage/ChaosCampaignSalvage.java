package mekhq.campaign.mission.scenarios.salvage;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.gui.dialog.camOpsSalvage.SalvagePostScenarioPicker;

/**
 * The {@link SalvageSystem#CHAOS_CAMPAIGN Chaos Campaign} salvage system: a simplified version of Campaign Operations
 * salvage.
 *
 * <p>Salvage teams aren't used; the player recovers every wreck automatically. The salvage rights set the player's
 * share of each wreck's value, which they receive in cash unless they buy the unit from the employer (see
 * {@link SalvageSettlement#forSalvagePurchases}).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosCampaignSalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return false;
    }

    @Override
    public SalvageSettlement createSettlement(AbstractContract contract) {
        return SalvageSettlement.forSalvagePurchases(contract);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the player controls the battlefield, every wreck is recovered, and the post-scenario salvage picker only
     * asks which units the player wants to buy.</p>
     */
    @Override
    public void resolveScenarioSalvage(Campaign campaign, AbstractContract contract, Scenario scenario,
          boolean hasBattlefieldControl, List<TestUnit> claimedSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> unclaimedSalvage) {
        if (hasBattlefieldControl) {
            new SalvagePostScenarioPicker(campaign, this, contract, scenario, claimedSalvage, soldSalvage);
        }
    }
}

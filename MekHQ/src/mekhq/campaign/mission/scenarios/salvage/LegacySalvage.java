package mekhq.campaign.mission.scenarios.salvage;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.TestUnit;

/**
 * The {@link SalvageSystem#LEGACY Legacy} salvage system: the original MekHQ method, where salvage is claimed directly
 * in the resolve scenario wizard without salvage operations.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class LegacySalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return false;
    }

    @Override
    public boolean isSalvageClaimedInResolveWizard() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The player has already divided the salvage in the resolve scenario wizard, so it is settled as is.</p>
     */
    @Override
    public void resolveScenarioSalvage(Campaign campaign, AbstractContract contract, Scenario scenario,
          boolean hasBattlefieldControl, List<TestUnit> claimedSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> unclaimedSalvage) {
        CamOpsSalvageUtilities.resolveSalvage(campaign, contract, scenario, createSettlement(contract), claimedSalvage,
              soldSalvage, unclaimedSalvage);
    }
}

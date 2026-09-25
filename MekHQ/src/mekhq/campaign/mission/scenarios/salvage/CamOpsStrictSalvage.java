package mekhq.campaign.mission.scenarios.salvage;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.gui.dialog.camOpsSalvage.SalvagePostScenarioPicker;

/**
 * The {@link SalvageSystem#CAM_OPS_STRICT CamOps (Strict)} salvage system: salvage as written in Campaign Operations.
 *
 * <p>This is the baseline for every system that uses salvage operations: salvage formations and techs are assigned
 * before a scenario starts, and recover the wrecks afterward.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CamOpsStrictSalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The assigned salvage teams recover the wrecks in the post-scenario salvage picker. Their techs then risk
     * accidents (if risky salvage is enabled), and spend the time the recovery took.</p>
     *
     * <p>There is nothing to recover if no teams or techs were assigned, or if the player doesn't control the
     * battlefield.</p>
     */
    @Override
    public void resolveScenarioSalvage(Campaign campaign, AbstractContract contract, Scenario scenario,
          boolean hasBattlefieldControl, List<TestUnit> claimedSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> unclaimedSalvage) {
        boolean hasAssignedSalvageFormations = !scenario.getSalvageFormations().isEmpty();
        boolean hasAssignedSalvageTechs = !scenario.getSalvageTechs().isEmpty();
        if (!hasBattlefieldControl || !hasAssignedSalvageFormations || !hasAssignedSalvageTechs) {
            return;
        }

        SalvagePostScenarioPicker picker = new SalvagePostScenarioPicker(campaign, this, contract, scenario,
              claimedSalvage, soldSalvage);

        List<UUID> techUUIDs = scenario.getSalvageTechs();
        if (campaign.getCampaignOptions().get(CampaignOption.IS_USE_RISKY_SALVAGE)) {
            CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, techUUIDs, picker.getCountOfSalvageUnits());
        }

        CamOpsSalvageUtilities.depleteTechMinutes(campaign, techUUIDs, picker.getUsedSalvageTime());
    }
}

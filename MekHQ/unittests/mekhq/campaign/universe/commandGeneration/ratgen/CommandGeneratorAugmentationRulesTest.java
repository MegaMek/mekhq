/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers the augmentation rules chosen on the Setup tab reaching the campaign.
 *
 * <p>All three are off in a new campaign, and none can be applied to warriors after they are
 * generated, so a choice that did not reach the campaign before generation would be silently lost -
 * which is what a player reports as "I turned it on and nothing happened".</p>
 */
class CommandGeneratorAugmentationRulesTest {

    private static NeuralInterfaceMode neuralInterfaceOf(Campaign campaign) {
        return NeuralInterfaceMode.from(campaign.getGameOptions());
    }

    private static boolean maneiDominiOf(Campaign campaign) {
        return campaign.getGameOptions().booleanOption(OptionsConstants.RPG_MANEI_DOMINI);
    }

    /** The whole point: what is ticked on the tab is in force by the time generation reads it. */
    @Test
    void theChosenRulesAreWrittenToTheCampaign() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        assertFalse(campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS),
              "a fresh campaign tracks no implants, which is the situation being fixed");

        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setUseImplants(true);
        options.setUseManeiDomini(true);
        options.setNeuralInterfaceMode(NeuralInterfaceMode.FULL_TRACKING);

        CommandGenerator.applyAugmentationRules(campaign, options);

        assertTrue(campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS));
        assertTrue(maneiDominiOf(campaign));
        assertEquals(NeuralInterfaceMode.FULL_TRACKING, neuralInterfaceOf(campaign));
    }

    /**
     * With implants untracked there is nothing for either rule to act on, so neither is left saying it
     * is in play.
     */
    @Test
    void theRulesAreWrittenOffWhenImplantsAreNotTracked() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setUseImplants(false);
        options.setUseManeiDomini(true);
        options.setNeuralInterfaceMode(NeuralInterfaceMode.FULL_TRACKING);

        CommandGenerator.applyAugmentationRules(campaign, options);

        assertFalse(campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS));
        assertFalse(maneiDominiOf(campaign), "a rule with nothing to act on is not left switched on");
        assertEquals(NeuralInterfaceMode.OFF, neuralInterfaceOf(campaign));
    }

    /** Turning the rules back off must take effect too, not only turning them on. */
    @Test
    void theRulesCanBeTurnedBackOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setUseImplants(true);
        options.setUseManeiDomini(true);
        options.setNeuralInterfaceMode(NeuralInterfaceMode.PILOT_ABILITIES_ONLY);
        CommandGenerator.applyAugmentationRules(campaign, options);

        options.setUseManeiDomini(false);
        options.setNeuralInterfaceMode(NeuralInterfaceMode.OFF);
        CommandGenerator.applyAugmentationRules(campaign, options);

        assertTrue(campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS));
        assertFalse(maneiDominiOf(campaign));
        assertEquals(NeuralInterfaceMode.OFF, neuralInterfaceOf(campaign));
    }
}

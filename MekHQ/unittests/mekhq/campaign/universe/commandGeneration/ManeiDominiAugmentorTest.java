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
package mekhq.campaign.universe.commandGeneration;

import static megamek.common.options.OptionsConstants.MD_DERMAL_ARMOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.enums.ManeiDominiAugmentationRank;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.ManeiDominiRank;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateImplants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import testUtilities.MHQTestUtilities;

/**
 * Covers what stays on the MekHQ side of Manei Domini augmentation: which commands are Manei Domini at
 * all, and bridging MegaMek's augmentation rank to the campaign rank a {@code Person} carries.
 *
 * <p>The availability rules themselves - counts, level ceiling, superseded pairs, the neural interface
 * requirement - belong to MegaMek and are covered by {@code ManeiDominiImplantsTest}.</p>
 */
class ManeiDominiAugmentorTest {

    @Test
    void onlyTheShadowDivisionsAreManeiDomini() {
        assertTrue(ManeiDominiAugmentor.isShadowDivision("WOB.SD"));
        assertTrue(ManeiDominiAugmentor.isShadowDivision("wob.sd"), "the key is not case sensitive");
        assertFalse(ManeiDominiAugmentor.isShadowDivision("WOB"),
              "the Militia proper are not Manei Domini");
        assertFalse(ManeiDominiAugmentor.isShadowDivision("CS"));
        assertFalse(ManeiDominiAugmentor.isShadowDivision(null));
    }

    /**
     * The bridge is by name, so a rank added to one enum and not the other would silently become
     * {@code NONE} and strip the warrior's standing off the roster.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void everyAugmentationRankHasACampaignRankOfTheSameName(
          ManeiDominiAugmentationRank augmentationRank) {
        ManeiDominiRank campaignRank = ManeiDominiAugmentor.toCampaignRank(augmentationRank);
        assertNotEquals(ManeiDominiRank.NONE, campaignRank,
              augmentationRank + " has no campaign rank of the same name");
        assertEquals(augmentationRank.name(), campaignRank.name());
    }

    /**
     * A Shadow Division warrior issued dermal armour keeps it, in a campaign using Advanced Alternate
     * Medical.
     *
     * <p>That rule set works dermal armour out from the prosthetics a person carries and writes the answer
     * over the implant every new day, so the implant only survives if the prosthetics are on the record.</p>
     */
    @Test
    void dermalImplantsSurviveTheDailyEligibilityPass() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, true);
        Person warrior = new Person(campaign);
        warrior.getOptions().acquireAbility(PersonnelOptions.MD_ADVANTAGES, MD_DERMAL_ARMOR, true);

        int recorded = ManeiDominiAugmentor.recordDermalProsthetics(campaign, warrior,
              List.of(MD_DERMAL_ARMOR));

        assertEquals(4, recorded, "dermal armour covers both arms and both legs");

        AdvancedMedicalAlternateImplants.checkForDermalEligibility(warrior);
        assertTrue(warrior.getOptions().booleanOption(MD_DERMAL_ARMOR),
              "the implant should still be fitted after the daily pass");
    }

    /**
     * The other half of the test above: without the prosthetics on the record, the daily pass takes the
     * implant away. This is the failure the recording exists to prevent, so it is worth pinning down.
     */
    @Test
    void aDermalImplantWithNoProstheticsIsTakenAwayByTheDailyPass() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, true);
        Person warrior = new Person(campaign);
        warrior.getOptions().acquireAbility(PersonnelOptions.MD_ADVANTAGES, MD_DERMAL_ARMOR, true);

        AdvancedMedicalAlternateImplants.checkForDermalEligibility(warrior);

        assertFalse(warrior.getOptions().booleanOption(MD_DERMAL_ARMOR),
              "an implant with nothing underneath it does not survive the daily pass");
    }

    /**
     * A campaign not using Advanced Alternate Medical keeps the implant option as the only record, and
     * nothing rewrites it. Adding surgery to those warriors' medical history would be inventing history.
     */
    @Test
    void nothingIsRecordedWhenTheCampaignDoesNotUseAlternateMedical() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);
        Person warrior = new Person(campaign);
        warrior.getOptions().acquireAbility(PersonnelOptions.MD_ADVANTAGES, MD_DERMAL_ARMOR, true);

        int recorded = ManeiDominiAugmentor.recordDermalProsthetics(campaign, warrior,
              List.of(MD_DERMAL_ARMOR));

        assertEquals(0, recorded, "no prosthetics should be recorded");
        assertTrue(warrior.getProstheticInjuries().isEmpty(),
              "the warrior's medical history should be left alone");
    }

    /** An implant the alternate rule set does not model is left as an option, with nothing recorded. */
    @Test
    void anImplantWithNoProstheticEquivalentRecordsNothing() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, true);
        Person warrior = new Person(campaign);

        int recorded = ManeiDominiAugmentor.recordDermalProsthetics(campaign, warrior,
              List.of(OptionsConstants.MD_PAIN_SHUNT));

        assertEquals(0, recorded, "the pain shunt has no dermal prosthetics to record");
    }
}

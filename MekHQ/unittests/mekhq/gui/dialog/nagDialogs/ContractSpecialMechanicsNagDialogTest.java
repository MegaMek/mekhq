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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics.EscalationMode;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

/**
 * Tests for {@link ContractSpecialMechanicsNagDialog}: every contract type with special mechanics must have a briefing,
 * since the briefing is the only tutorial the player gets for them; when a briefing is due; and what it is made of.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ContractSpecialMechanicsNagDialogTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSpecialMechanicsNagDialog";
    private static final String KEY_PREFIX = "ContractSpecialMechanicsNagDialog.";

    private MockedStatic<MekHQ> mekHQ;
    private MHQOptions mhqOptions;

    @BeforeEach
    void setUp() {
        mhqOptions = mock(MHQOptions.class);
        when(mhqOptions.getNagDialogIgnore(anyString())).thenReturn(false);
        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);
    }

    @AfterEach
    void tearDown() {
        mekHQ.close();
    }

    /**
     * A campaign with the "Contracts Use Special Mechanics" option and mapless mode set as given.
     */
    private static Campaign campaign(boolean isContractsUseSpecialMechanics, boolean isMaplessMode) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)).thenReturn(isContractsUseSpecialMechanics);
        when(options.isUseStratConMaplessMode()).thenReturn(isMaplessMode);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    /**
     * A contract of the given type, run on a StratCon map or not.
     */
    private static AbstractContract contract(ContractObjectiveType objectiveType, boolean hasStratConState) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getStratConCampaignState()).thenReturn(hasStratConState ? new StratConCampaignState() : null);
        return contract;
    }

    // When a briefing is due

    @Test
    void aBriefingIsDueWithTheOptionOnForAContractOnAStratConMap() {
        assertTrue(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign(true, false),
              contract(ContractObjectiveType.ESPIONAGE, true)));
    }

    @Test
    void noBriefingIsDueWithTheOptionOff() {
        assertFalse(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign(false, false),
              contract(ContractObjectiveType.ESPIONAGE, true)));
    }

    @Test
    void noBriefingIsDueInMaplessMode() {
        assertFalse(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign(true, true),
              contract(ContractObjectiveType.ESPIONAGE, true)));
    }

    @Test
    void noBriefingIsDueForAContractOptedOutOfStratCon() {
        assertFalse(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign(true, false),
              contract(ContractObjectiveType.ESPIONAGE, false)));
    }

    @Test
    void noBriefingIsDueForAContractTypeWithoutSpecialMechanics() {
        assertFalse(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign(true, false),
              contract(ContractObjectiveType.UNDEFINED, true)));
    }

    @Test
    void suppressingOneContractTypesBriefingLeavesTheOthersDue() {
        when(mhqOptions.getNagDialogIgnore(
              ContractSpecialMechanicsNagDialog.getNagKey(ContractObjectiveType.ESPIONAGE))).thenReturn(true);
        Campaign campaign = campaign(true, false);

        assertFalse(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign,
              contract(ContractObjectiveType.ESPIONAGE, true)));
        assertTrue(ContractSpecialMechanicsNagDialog.isBriefingDue(campaign,
              contract(ContractObjectiveType.GARRISON_DUTY, true)));
    }

    // What a briefing is made of

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, mode = EnumSource.Mode.EXCLUDE, names = { "UNDEFINED" })
    void onlyEscalatingContractsAreBriefedOnTheSharedEscalationRules(ContractObjectiveType objectiveType) {
        String briefing = ContractSpecialMechanicsNagDialog.getBriefingText(objectiveType);
        String escalationRules = getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "escalation");
        boolean isEscalating = StratConContractMechanics.forObjectiveType(objectiveType).escalationMode()
                                     == EscalationMode.ESCALATING;

        assertEquals(isEscalating, briefing.contains(escalationRules),
              objectiveType + (isEscalating ? " escalates, so" : " does not escalate, so it") + " should "
                    + (isEscalating ? "" : "not ") + "carry the shared Escalation rules");
    }

    @Test
    void aGarrisonIsNeverBriefedOnTheSharedEscalationRules() {
        // Its Escalation falls rather than rises, so its own briefing explains it instead.
        assertFalse(ContractSpecialMechanicsNagDialog.getBriefingText(ContractObjectiveType.GARRISON_DUTY)
                          .contains(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "escalation")));
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class)
    void everyContractTypeWithSpecialMechanicsHasABriefing(ContractObjectiveType objectiveType) {
        StratConContractMechanics mechanics = StratConContractMechanics.forObjectiveType(objectiveType);
        if (!ContractSpecialMechanicsNagDialog.hasSpecialMechanics(mechanics)) {
            return;
        }

        String key = KEY_PREFIX + objectiveType.name();
        assertTrue(isResourceKeyValid(getTextAt(RESOURCE_BUNDLE, key)), key + " is missing");
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, mode = EnumSource.Mode.EXCLUDE, names = { "UNDEFINED" })
    void everyDefinedContractTypeHasSpecialMechanics(ContractObjectiveType objectiveType) {
        assertTrue(ContractSpecialMechanicsNagDialog.hasSpecialMechanics(
              StratConContractMechanics.forObjectiveType(objectiveType)));
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, names = { "UNDEFINED" })
    void undefinedContractTypeHasNoSpecialMechanics(ContractObjectiveType objectiveType) {
        assertFalse(ContractSpecialMechanicsNagDialog.hasSpecialMechanics(
              StratConContractMechanics.forObjectiveType(objectiveType)));
    }

    @Test
    void everyBriefedContractTypeIsSuppressedSeparately() {
        List<ContractObjectiveType> briefedContractTypes = ContractSpecialMechanicsNagDialog.getBriefedContractTypes();
        assertFalse(briefedContractTypes.contains(ContractObjectiveType.UNDEFINED));

        Set<String> nagKeys = new HashSet<>();
        for (ContractObjectiveType objectiveType : briefedContractTypes) {
            nagKeys.add(ContractSpecialMechanicsNagDialog.getNagKey(objectiveType));
        }
        assertEquals(briefedContractTypes.size(), nagKeys.size());
    }
}

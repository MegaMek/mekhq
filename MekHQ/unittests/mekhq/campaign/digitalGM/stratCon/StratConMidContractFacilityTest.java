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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.facility.StratConContractFacilityProfile;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests that a facility a random scenario brings onto the map partway through a contract is made like those placed
 * when the contract began: by the contract's facility profile, at the profile's tier, with a full garrison.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConMidContractFacilityTest {
    private Campaign campaign;
    private AbstractContract contract;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION))
              .thenReturn(false);
        contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.PLANETARY_ASSAULT);
        when(contract.getScale()).thenReturn(5);
    }

    private StratConFacility create(StratConContractDefinition contractDefinition, ForceAlignment owner) {
        try (MockedStatic<StratConContractDefinition> definitions = mockStatic(StratConContractDefinition.class)) {
            definitions.when(() -> StratConContractDefinition.getContractDefinition(
                  ContractObjectiveType.PLANETARY_ASSAULT)).thenReturn(contractDefinition);
            return StratConContractInitializer.createMidContractFacility(campaign, contract, owner);
        }
    }

    @Test
    void theContractProfilePicksTheTypeAndShiftsTheTier() {
        StratConContractFacilityProfile profile = new StratConContractFacilityProfile();
        Map<FacilityType, Integer> weights = new LinkedHashMap<>();
        weights.put(FacilityType.SensorPost, 1);
        profile.setTypeWeights(weights);
        profile.setTierModifier(1);
        StratConContractDefinition contractDefinition = mock(StratConContractDefinition.class);
        when(contractDefinition.getFacilityProfile()).thenReturn(profile);

        StratConFacility facility = create(contractDefinition, ForceAlignment.Opposing);

        assertNotNull(facility);
        assertEquals(FacilityType.SensorPost, facility.getFacilityType());
        assertEquals(ForceAlignment.Opposing, facility.getOwner());
        FacilityTier baseTier = StratConContractInitializer.getFacilityTier(5, false, false);
        assertEquals(profile.adjustTier(baseTier), facility.getTier());
        assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
    }

    @Test
    void withoutAProfileAnyFacilityOfThatSideWillDo() {
        StratConFacility facility = create(null, ForceAlignment.Allied);

        assertNotNull(facility);
        assertEquals(ForceAlignment.Allied, facility.getOwner());
        assertEquals(StratConContractInitializer.getFacilityTier(5, false, false), facility.getTier());
        assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
    }
}

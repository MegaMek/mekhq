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

import static mekhq.MHQConstants.NAG_STRATCON_FACILITY_BRIEFING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests when the one-time facility briefing is due: only with Facility Operations on, outside mapless play, on a
 * contract with at least one facility, and until the player asks not to see it again.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityBriefingNagDialogTest {
    private MockedStatic<MekHQ> mekHQ;
    private MHQOptions mhqOptions;
    private Campaign campaign;
    private CampaignOptions options;
    private AbstractContract contract;
    private StratConTrackState track;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        // Set up before MekHQ is mocked, so the options classes load their resources as they normally would.
        options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(true);
        when(options.isUseStratConMaplessMode()).thenReturn(false);
        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);

        mhqOptions = mock(MHQOptions.class);
        when(mhqOptions.getNagDialogIgnore(anyString())).thenReturn(false);
        when(mhqOptions.getLocale()).thenReturn(Locale.ENGLISH);
        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
    }

    @AfterEach
    void tearDown() {
        mekHQ.close();
    }

    private void placeFacility() {
        track.addFacility(new StratConCoords(1, 1), StratConTestData.facility(ForceAlignment.Opposing,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json"))));
    }

    @Test
    void aContractWithFacilitiesGetsTheBriefing() {
        placeFacility();

        assertTrue(StratConFacilityBriefingNagDialog.isBriefingDue(campaign, contract));
    }

    @Test
    void noBriefingWithFacilityOperationsOff() {
        placeFacility();
        when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(false);

        assertFalse(StratConFacilityBriefingNagDialog.isBriefingDue(campaign, contract));
    }

    @Test
    void noBriefingInMaplessPlay() {
        placeFacility();
        when(options.isUseStratConMaplessMode()).thenReturn(true);

        assertFalse(StratConFacilityBriefingNagDialog.isBriefingDue(campaign, contract));
    }

    @Test
    void noBriefingWithoutAnyFacility() {
        assertFalse(StratConFacilityBriefingNagDialog.isBriefingDue(campaign, contract));
    }

    @Test
    void noBriefingWithoutAStratConMap() {
        placeFacility();
        when(contract.getStratConCampaignState()).thenReturn(null);

        assertFalse(StratConFacilityBriefingNagDialog.isBriefingDue(campaign, contract));
    }

    @Test
    void noBriefingOnceThePlayerHasAskedNotToSeeItAgain() {
        placeFacility();
        when(mhqOptions.getNagDialogIgnore(NAG_STRATCON_FACILITY_BRIEFING)).thenReturn(true);

        assertFalse(StratConFacilityBriefingNagDialog.isBriefingDue(campaign, contract));
    }
}

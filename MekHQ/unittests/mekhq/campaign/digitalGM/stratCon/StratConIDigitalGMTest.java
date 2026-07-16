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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.IDigitalGM;
import mekhq.campaign.digitalGM.IFacilityStrategy;
import mekhq.campaign.events.NewDayEvent;
import org.junit.jupiter.api.Test;

/**
 * Verifies the digital-GM layer that replaced {@code StratConRulesManager.handleNewDay}: that exactly one GM activates
 * per StratCon play type, that each GM is configured with the strategies matching its legacy behaviour, and that the
 * shared dispatch only runs the enabled GM.
 *
 * @author Illiani
 */
class StratConIDigitalGMTest {

    private static Campaign campaignWithPlayType(StratConPlayType playType) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.getStratConPlayType()).thenReturn(playType);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    // region Exact play-type gating: mutually exclusive, so at most one GM acts per day

    @Test
    void normalPlayTypeEnablesOnlyStratConDigitalGM() {
        Campaign campaign = campaignWithPlayType(StratConPlayType.NORMAL);
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        assertTrue(new StratConDigitalGM().isEnabled(campaignOptions));
        assertFalse(new MaplessStratConGM().isEnabled(campaignOptions));
        assertFalse(new SinglesStratConGM().isEnabled(campaignOptions));
    }

    @Test
    void maplessPlayTypeEnablesOnlyMaplessStratConGM() {
        Campaign campaign = campaignWithPlayType(StratConPlayType.MAPLESS);
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        assertFalse(new StratConDigitalGM().isEnabled(campaignOptions));
        assertTrue(new MaplessStratConGM().isEnabled(campaignOptions));
        assertFalse(new SinglesStratConGM().isEnabled(campaignOptions));
    }

    @Test
    void singlesPlayTypeEnablesOnlySinglesStratConGM() {
        Campaign campaign = campaignWithPlayType(StratConPlayType.SINGLES);
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        assertFalse(new StratConDigitalGM().isEnabled(campaignOptions));
        assertFalse(new MaplessStratConGM().isEnabled(campaignOptions));
        assertTrue(new SinglesStratConGM().isEnabled(campaignOptions));
    }

    @Test
    void disabledPlayTypeEnablesNoGM() {
        Campaign campaign = campaignWithPlayType(StratConPlayType.DISABLED);
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        assertFalse(new StratConDigitalGM().isEnabled(campaignOptions));
        assertFalse(new MaplessStratConGM().isEnabled(campaignOptions));
        assertFalse(new SinglesStratConGM().isEnabled(campaignOptions));
    }

    // endregion

    // region Strategy configuration matches each play type's legacy behaviour

    @Test
    void normalAppliesFacilityEffectsAndFullCadence() {
        StratConDigitalGM gm = new StratConDigitalGM();

        assertInstanceOf(StratConFacilityStrategy.class, gm.getFacilityStrategy());
        assertFalse(gm.isSingleDropMode());
    }

    @Test
    void maplessSkipsFacilityEffectsButKeepsFullCadence() {
        MaplessStratConGM gm = new MaplessStratConGM();

        // Legacy: isUseStratConMapless -> facility effects skipped, but not single-drop
        assertInstanceOf(NoOpFacilityStrategy.class, gm.getFacilityStrategy());
        assertFalse(gm.isSingleDropMode());
    }

    @Test
    void singlesSkipsFacilityEffectsAndCapsToOneDrop() {
        SinglesStratConGM gm = new SinglesStratConGM();

        // Legacy: Singles implies Mapless (facility effects skipped) and single-drop pacing
        assertInstanceOf(NoOpFacilityStrategy.class, gm.getFacilityStrategy());
        assertTrue(gm.isSingleDropMode());
    }

    // endregion

    // region Dispatch gating: onNewDay runs handleNewDay only for the enabled GM

    @Test
    void onNewDayRunsHandleNewDayWhenEnabled() {
        StratConDigitalGM gm = spy(new StratConDigitalGM());
        doNothing().when(gm).handleNewDay(any());

        Campaign campaign = campaignWithPlayType(StratConPlayType.NORMAL);
        NewDayEvent event = mock(NewDayEvent.class);
        when(event.getCampaign()).thenReturn(campaign);

        gm.onNewDay(event);

        verify(gm, times(1)).handleNewDay(event);
    }

    @Test
    void onNewDaySkipsHandleNewDayWhenDisabled() {
        // A mapless GM must stay inert on a Normal-play campaign
        MaplessStratConGM gm = spy(new MaplessStratConGM());
        doNothing().when(gm).handleNewDay(any());

        Campaign campaign = campaignWithPlayType(StratConPlayType.NORMAL);
        NewDayEvent event = mock(NewDayEvent.class);
        when(event.getCampaign()).thenReturn(campaign);

        gm.onNewDay(event);

        verify(gm, never()).handleNewDay(any());
    }

    @Test
    void gmsExposeThemselvesAsDigitalGMs() {
        IDigitalGM normal = new StratConDigitalGM();
        IFacilityStrategy facility = new StratConDigitalGM().getFacilityStrategy();

        assertInstanceOf(IDigitalGM.class, normal);
        assertInstanceOf(IFacilityStrategy.class, facility);
    }

    // endregion
}

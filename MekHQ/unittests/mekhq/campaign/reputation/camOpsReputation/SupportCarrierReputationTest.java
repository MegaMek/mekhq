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
package mekhq.campaign.reputation.camOpsReputation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;
import java.util.Map;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Support carriers (support platoons and squads) are an organisational wrapper around support personnel. The
 * Reputation calculations must ignore the carrier unit and treat the people inside like any other support staff.
 *
 * @author Illiani
 * @since 0.51.01
 */
class SupportCarrierReputationTest {
    private static final int FULL_CREW_SIZE = 28;

    @Test
    void transportRequirements_ignoreSupportCarriers() {
        Campaign campaign = campaignWithUnits(List.of(supportCarrier(), infantryUnit()));

        Map<String, Integer> requirements = TransportationRating.calculateTransportRequirements(campaign);

        assertEquals(1, requirements.get("infantryCount"), "Only the non-carrier infantry unit should need a bay");
    }

    @Test
    void totalPersonnelCount_ignoresSupportCarriers() {
        Campaign campaign = campaignWithUnits(List.of(supportCarrier()));

        assertEquals(0, SupportRating.getTotalPersonnelCount(campaign, 0));
    }

    @Test
    void totalPersonnelCount_stillCountsOrdinaryInfantry() {
        Campaign campaign = campaignWithUnits(List.of(infantryUnit()));

        assertEquals(FULL_CREW_SIZE, SupportRating.getTotalPersonnelCount(campaign, 0));
    }

    private static Campaign campaignWithUnits(List<Unit> units) {
        Campaign campaign = mockCampaign();
        CampaignOptions campaignOptions = mock(CampaignOptions.class, RETURNS_DEEP_STUBS);
        when(campaignOptions.get(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION)).thenReturn(true);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getActiveUnits()).thenReturn(units);
        return campaign;
    }

    private static Unit supportCarrier() {
        Unit unit = infantryUnit();
        when(unit.isCarrier()).thenReturn(true);
        return unit;
    }

    private static Unit infantryUnit() {
        Entity entity = mock(Entity.class);
        when(entity.isInfantry()).thenReturn(true);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getFullCrewSize()).thenReturn(FULL_CREW_SIZE);
        return unit;
    }
}

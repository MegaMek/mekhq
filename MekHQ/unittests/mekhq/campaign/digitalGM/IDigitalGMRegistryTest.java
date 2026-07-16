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
package mekhq.campaign.digitalGM;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.MaplessStratConGM;
import mekhq.campaign.digitalGM.stratCon.SinglesStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConDigitalGM;
import mekhq.campaign.digitalGM.stratCon.StratConPlayType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link DigitalGMRegistry} returns the GM whose play type matches a campaign, and only that GM. This is
 * the lookup non-GM code uses to reach the active GM's strategies.
 *
 * @author Illiani
 */
class IDigitalGMRegistryTest {

    private final List<IDigitalGM> registered = new ArrayList<>();

    private void register(IDigitalGM IDigitalGM) {
        DigitalGMRegistry.register(IDigitalGM);
        registered.add(IDigitalGM);
    }

    @AfterEach
    void cleanup() {
        registered.forEach(DigitalGMRegistry::unregister);
        registered.clear();
    }

    private static Campaign campaignWith(StratConPlayType playType) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.getStratConPlayType()).thenReturn(playType);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    @Test
    void getActiveGmReturnsTheGmMatchingThePlayType() {
        StratConDigitalGM normal = new StratConDigitalGM();
        MaplessStratConGM mapless = new MaplessStratConGM();
        SinglesStratConGM singles = new SinglesStratConGM();
        register(normal);
        register(mapless);
        register(singles);

        assertSame(normal,
              DigitalGMRegistry.getActiveGM(campaignWith(StratConPlayType.NORMAL).getCampaignOptions()).orElseThrow());
        assertSame(mapless,
              DigitalGMRegistry.getActiveGM(campaignWith(StratConPlayType.MAPLESS).getCampaignOptions()).orElseThrow());
        assertSame(singles,
              DigitalGMRegistry.getActiveGM(campaignWith(StratConPlayType.SINGLES).getCampaignOptions()).orElseThrow());
    }

    @Test
    void getActiveGmIsEmptyWhenPlayIsDisabled() {
        register(new StratConDigitalGM());
        register(new MaplessStratConGM());
        register(new SinglesStratConGM());

        assertTrue(DigitalGMRegistry.getActiveGM(campaignWith(StratConPlayType.DISABLED).getCampaignOptions())
                         .isEmpty());
    }

    @Test
    void unregisterRemovesTheGm() {
        StratConDigitalGM gm = new StratConDigitalGM();
        DigitalGMRegistry.register(gm);
        DigitalGMRegistry.unregister(gm);

        assertTrue(DigitalGMRegistry.getActiveGM(campaignWith(StratConPlayType.NORMAL).getCampaignOptions()).isEmpty());
    }
}

/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

class BattleMekFactionGenerationMethodTest {
    // region Variable Declarations
    private static final BattleMekFactionGenerationMethod[] methods = BattleMekFactionGenerationMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.ORIGIN_FACTION.toolTipText"),
              BattleMekFactionGenerationMethod.ORIGIN_FACTION.getToolTipText());
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.SPECIFIED_FACTION.toolTipText"),
              BattleMekFactionGenerationMethod.SPECIFIED_FACTION.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsOriginFaction() {
        for (final BattleMekFactionGenerationMethod battleMekFactionGenerationMethod : methods) {
            if (battleMekFactionGenerationMethod == BattleMekFactionGenerationMethod.ORIGIN_FACTION) {
                assertTrue(battleMekFactionGenerationMethod.isOriginFaction());
            } else {
                assertFalse(battleMekFactionGenerationMethod.isOriginFaction());
            }
        }
    }

    @Test
    void testIsCampaignFaction() {
        for (final BattleMekFactionGenerationMethod battleMekFactionGenerationMethod : methods) {
            if (battleMekFactionGenerationMethod == BattleMekFactionGenerationMethod.CAMPAIGN_FACTION) {
                assertTrue(battleMekFactionGenerationMethod.isCampaignFaction());
            } else {
                assertFalse(battleMekFactionGenerationMethod.isCampaignFaction());
            }
        }
    }

    @Test
    void testIsSpecifiedFaction() {
        for (final BattleMekFactionGenerationMethod battleMekFactionGenerationMethod : methods) {
            if (battleMekFactionGenerationMethod == BattleMekFactionGenerationMethod.SPECIFIED_FACTION) {
                assertTrue(battleMekFactionGenerationMethod.isSpecifiedFaction());
            } else {
                assertFalse(battleMekFactionGenerationMethod.isSpecifiedFaction());
            }
        }
    }
    // region Boolean Comparison Methods

    @Test
    void testGenerateFaction() {
        final Person mockPerson = mock(Person.class);
        final Faction mockOriginFaction = mock(Faction.class);
        when(mockPerson.getOriginFaction()).thenReturn(mockOriginFaction);

        final Faction mockCampaignFaction = mock(Faction.class);
        final Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getFaction()).thenReturn(mockCampaignFaction);

        final Faction mockSpecifiedFaction = mock(Faction.class);

        assertEquals(
              BattleMekFactionGenerationMethod.ORIGIN_FACTION.generateFaction(mockPerson, mockCampaign,
                    mockSpecifiedFaction),
              mockOriginFaction);
        assertEquals(
              BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.generateFaction(mockPerson, mockCampaign,
                    mockSpecifiedFaction),
              mockCampaignFaction);
        assertEquals(
              BattleMekFactionGenerationMethod.SPECIFIED_FACTION.generateFaction(mockPerson, mockCampaign,
                    mockSpecifiedFaction),
              mockSpecifiedFaction);
    }

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.text"),
              BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.toString());
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.SPECIFIED_FACTION.text"),
              BattleMekFactionGenerationMethod.SPECIFIED_FACTION.toString());
    }
}

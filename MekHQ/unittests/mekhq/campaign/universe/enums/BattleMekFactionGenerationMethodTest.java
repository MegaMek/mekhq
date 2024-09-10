/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

public class BattleMekFactionGenerationMethodTest {
    // region Variable Declarations
    private static final BattleMekFactionGenerationMethod[] methods = BattleMekFactionGenerationMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.ORIGIN_FACTION.toolTipText"),
                BattleMekFactionGenerationMethod.ORIGIN_FACTION.getToolTipText());
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.SPECIFIED_FACTION.toolTipText"),
                BattleMekFactionGenerationMethod.SPECIFIED_FACTION.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    public void testIsOriginFaction() {
        for (final BattleMekFactionGenerationMethod battleMekFactionGenerationMethod : methods) {
            if (battleMekFactionGenerationMethod == BattleMekFactionGenerationMethod.ORIGIN_FACTION) {
                assertTrue(battleMekFactionGenerationMethod.isOriginFaction());
            } else {
                assertFalse(battleMekFactionGenerationMethod.isOriginFaction());
            }
        }
    }

    @Test
    public void testIsCampaignFaction() {
        for (final BattleMekFactionGenerationMethod battleMekFactionGenerationMethod : methods) {
            if (battleMekFactionGenerationMethod == BattleMekFactionGenerationMethod.CAMPAIGN_FACTION) {
                assertTrue(battleMekFactionGenerationMethod.isCampaignFaction());
            } else {
                assertFalse(battleMekFactionGenerationMethod.isCampaignFaction());
            }
        }
    }

    @Test
    public void testIsSpecifiedFaction() {
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
    public void testGenerateFaction() {
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
    public void testToStringOverride() {
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.text"),
                BattleMekFactionGenerationMethod.CAMPAIGN_FACTION.toString());
        assertEquals(resources.getString("BattleMekFactionGenerationMethod.SPECIFIED_FACTION.text"),
                BattleMekFactionGenerationMethod.SPECIFIED_FACTION.toString());
    }
}

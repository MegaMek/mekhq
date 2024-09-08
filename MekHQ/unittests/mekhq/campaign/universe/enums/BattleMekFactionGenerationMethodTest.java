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
    //region Variable Declarations
    private static final BattleMechFactionGenerationMethod[] methods = BattleMechFactionGenerationMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("BattleMechFactionGenerationMethod.ORIGIN_FACTION.toolTipText"),
                BattleMechFactionGenerationMethod.ORIGIN_FACTION.getToolTipText());
        assertEquals(resources.getString("BattleMechFactionGenerationMethod.SPECIFIED_FACTION.toolTipText"),
                BattleMechFactionGenerationMethod.SPECIFIED_FACTION.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsOriginFaction() {
        for (final BattleMechFactionGenerationMethod battleMechFactionGenerationMethod : methods) {
            if (battleMechFactionGenerationMethod == BattleMechFactionGenerationMethod.ORIGIN_FACTION) {
                assertTrue(battleMechFactionGenerationMethod.isOriginFaction());
            } else {
                assertFalse(battleMechFactionGenerationMethod.isOriginFaction());
            }
        }
    }

    @Test
    public void testIsCampaignFaction() {
        for (final BattleMechFactionGenerationMethod battleMechFactionGenerationMethod : methods) {
            if (battleMechFactionGenerationMethod == BattleMechFactionGenerationMethod.CAMPAIGN_FACTION) {
                assertTrue(battleMechFactionGenerationMethod.isCampaignFaction());
            } else {
                assertFalse(battleMechFactionGenerationMethod.isCampaignFaction());
            }
        }
    }

    @Test
    public void testIsSpecifiedFaction() {
        for (final BattleMechFactionGenerationMethod battleMechFactionGenerationMethod : methods) {
            if (battleMechFactionGenerationMethod == BattleMechFactionGenerationMethod.SPECIFIED_FACTION) {
                assertTrue(battleMechFactionGenerationMethod.isSpecifiedFaction());
            } else {
                assertFalse(battleMechFactionGenerationMethod.isSpecifiedFaction());
            }
        }
    }
    //region Boolean Comparison Methods

    @Test
    public void testGenerateFaction() {
        final Faction mockOriginFaction = mock(Faction.class);
        final Person mockPerson = mock(Person.class);
        when(mockPerson.getOriginFaction()).thenReturn(mockOriginFaction);

        final Faction mockCampaignFaction = mock(Faction.class);
        final Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getFaction()).thenReturn(mockCampaignFaction);

        final Faction mockSpecifiedFaction = mock(Faction.class);

        assertEquals(
                BattleMechFactionGenerationMethod.ORIGIN_FACTION.generateFaction(mockPerson, mockCampaign, mockSpecifiedFaction),
                mockOriginFaction);
        assertEquals(
                BattleMechFactionGenerationMethod.CAMPAIGN_FACTION.generateFaction(mockPerson, mockCampaign, mockSpecifiedFaction),
                mockCampaignFaction);
        assertEquals(
                BattleMechFactionGenerationMethod.SPECIFIED_FACTION.generateFaction(mockPerson, mockCampaign, mockSpecifiedFaction),
                mockSpecifiedFaction);
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("BattleMechFactionGenerationMethod.CAMPAIGN_FACTION.text"),
                BattleMechFactionGenerationMethod.CAMPAIGN_FACTION.toString());
        assertEquals(resources.getString("BattleMechFactionGenerationMethod.SPECIFIED_FACTION.text"),
                BattleMechFactionGenerationMethod.SPECIFIED_FACTION.toString());
    }
}

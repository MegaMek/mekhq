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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.client.bot.princess.CardinalEdge;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DestinationZoneMapper}.
 *
 * <p>These began (Phase 0.4) as characterization tests pinning a defect: the reverse mapping was the identity
 * function, so saving a force and reloading it into the editor mislabeled three of the four cardinal edges in a
 * three-way cycle (East&rarr;West, South&rarr;East, West&rarr;South). Phase 1.1 made the reverse mapping a true
 * inverse; the round-trip tests now assert that every destination-zone entry survives a save/load cycle unchanged.
 */
class DestinationZoneMapperTest {

    // Destination zone combo box indices, matching ScenarioForceTemplate.BOT_DESTINATION_ZONES ordering.
    private static final int COMBO_NORTH = 0;
    private static final int COMBO_EAST = 1;
    private static final int COMBO_SOUTH = 2;
    private static final int COMBO_WEST = 3;
    private static final int COMBO_NEAREST = 4;
    private static final int COMBO_NONE = 5;
    private static final int COMBO_OPPOSITE_DEPLOYMENT = 6;
    private static final int COMBO_RANDOM = 7;

    /**
     * Applies the forward mapping (save) then the reverse mapping (load), i.e. the combo index a force would show if it
     * were saved from {@code comboIndex} and immediately reloaded into the editor.
     */
    private static int roundTrip(int comboIndex) {
        return DestinationZoneMapper.storedZoneToComboIndex(DestinationZoneMapper.comboIndexToStoredZone(comboIndex));
    }

    // --- Forward mapping: correct and permanent ------------------------------------------------------------------

    @Test
    void forwardMappingMatchesCardinalEdgeIndicesAndSpecialConstants() {
        assertEquals(CardinalEdge.NORTH.getIndex(), DestinationZoneMapper.comboIndexToStoredZone(COMBO_NORTH));
        assertEquals(CardinalEdge.EAST.getIndex(), DestinationZoneMapper.comboIndexToStoredZone(COMBO_EAST));
        assertEquals(CardinalEdge.SOUTH.getIndex(), DestinationZoneMapper.comboIndexToStoredZone(COMBO_SOUTH));
        assertEquals(CardinalEdge.WEST.getIndex(), DestinationZoneMapper.comboIndexToStoredZone(COMBO_WEST));
        assertEquals(CardinalEdge.NEAREST.getIndex(), DestinationZoneMapper.comboIndexToStoredZone(COMBO_NEAREST));
        assertEquals(CardinalEdge.NONE.getIndex(), DestinationZoneMapper.comboIndexToStoredZone(COMBO_NONE));
        assertEquals(ScenarioForceTemplate.DESTINATION_EDGE_OPPOSITE_DEPLOYMENT,
              DestinationZoneMapper.comboIndexToStoredZone(COMBO_OPPOSITE_DEPLOYMENT));
        assertEquals(ScenarioForceTemplate.DESTINATION_EDGE_RANDOM,
              DestinationZoneMapper.comboIndexToStoredZone(COMBO_RANDOM));
    }

    // --- Round-trip: every entry must survive a save/load cycle unchanged (Phase 1.1 fix) ------------------------

    @Test
    void everyDestinationZoneRoundTripsUnchanged() {
        assertEquals(COMBO_NORTH, roundTrip(COMBO_NORTH), "North should round-trip unchanged");
        assertEquals(COMBO_EAST, roundTrip(COMBO_EAST), "East should round-trip unchanged (was mislabeled as West)");
        assertEquals(COMBO_SOUTH, roundTrip(COMBO_SOUTH), "South should round-trip unchanged (was mislabeled as East)");
        assertEquals(COMBO_WEST, roundTrip(COMBO_WEST), "West should round-trip unchanged (was mislabeled as South)");
        assertEquals(COMBO_NEAREST, roundTrip(COMBO_NEAREST), "Nearest should round-trip unchanged");
        assertEquals(COMBO_NONE, roundTrip(COMBO_NONE), "None should round-trip unchanged");
        assertEquals(COMBO_OPPOSITE_DEPLOYMENT, roundTrip(COMBO_OPPOSITE_DEPLOYMENT),
              "Opposite Deployment should round-trip unchanged");
        assertEquals(COMBO_RANDOM, roundTrip(COMBO_RANDOM), "Random should round-trip unchanged");
    }
}

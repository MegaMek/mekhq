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
package mekhq.gui.scenarioTemplateEditor;

import java.util.HashMap;
import java.util.Map;

import megamek.client.bot.princess.CardinalEdge;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;

/**
 * Translates between the "Destination Zone" combo box index shown in {@link ScenarioTemplateEditorDialog} and the
 * destination-zone value stored on a {@link ScenarioForceTemplate}. This logic was extracted from the dialog so it can
 * be exercised without constructing the Swing UI.
 *
 * <p>The combo box lists edges in the order North, East, South, West, whereas {@link CardinalEdge} indexes them
 * North(0), South(1), West(2), East(3); the two special destinations map to their own constants. Because the orderings
 * differ, {@link #storedZoneToComboIndex(int)} must be a genuine inverse of {@link #comboIndexToStoredZone(int)} rather
 * than the identity function. Both directions are derived from a single mapping table so they cannot drift apart.
 */
public final class DestinationZoneMapper {

    /**
     * Maps the destination-zone combo box index to the value stored on the force template: {@link CardinalEdge} indexes
     * for the cardinal/nearest/none entries, and the two special destination constants for the last two.
     */
    private static final Map<Integer, Integer> COMBO_INDEX_TO_STORED_ZONE = new HashMap<>();

    /**
     * The inverse of {@link #COMBO_INDEX_TO_STORED_ZONE}, derived from it so the two cannot fall out of sync. The
     * forward values are all distinct, so the inversion is well defined.
     */
    private static final Map<Integer, Integer> STORED_ZONE_TO_COMBO_INDEX = new HashMap<>();

    static {
        COMBO_INDEX_TO_STORED_ZONE.put(0, CardinalEdge.NORTH.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(1, CardinalEdge.EAST.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(2, CardinalEdge.SOUTH.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(3, CardinalEdge.WEST.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(4, CardinalEdge.NEAREST.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(5, CardinalEdge.NONE.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(6, ScenarioForceTemplate.DESTINATION_EDGE_OPPOSITE_DEPLOYMENT);
        COMBO_INDEX_TO_STORED_ZONE.put(7, ScenarioForceTemplate.DESTINATION_EDGE_RANDOM);

        for (Map.Entry<Integer, Integer> entry : COMBO_INDEX_TO_STORED_ZONE.entrySet()) {
            STORED_ZONE_TO_COMBO_INDEX.put(entry.getValue(), entry.getKey());
        }
    }

    private DestinationZoneMapper() {
    }

    /**
     * Forward mapping used when saving a force: the selected combo box index becomes the stored destination-zone
     * value.
     *
     * @param comboIndex the selected index in the destination zone combo box
     *
     * @return the destination-zone value to store on the force template
     */
    public static int comboIndexToStoredZone(int comboIndex) {
        return COMBO_INDEX_TO_STORED_ZONE.get(comboIndex);
    }

    /**
     * Reverse mapping used when loading a force into the editor: the stored destination-zone value becomes the combo
     * box index to select. This is the true inverse of {@link #comboIndexToStoredZone(int)}. An unrecognized value
     * (outside the mapped set) falls back to itself, matching the combo box's own bounds handling.
     *
     * @param storedZone the destination-zone value stored on the force template
     *
     * @return the combo box index to select
     */
    public static int storedZoneToComboIndex(int storedZone) {
        return STORED_ZONE_TO_COMBO_INDEX.getOrDefault(storedZone, storedZone);
    }
}

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
package mekhq.gui.dialog;

import java.util.HashMap;
import java.util.Map;

import megamek.client.bot.princess.CardinalEdge;
import mekhq.campaign.mission.ScenarioForceTemplate;

/**
 * Translates between the "Destination Zone" combo box index shown in {@link ScenarioTemplateEditorDialog} and the
 * destination-zone value stored on a {@link ScenarioForceTemplate}. This logic was extracted from the dialog so it can
 * be exercised without constructing the Swing UI.
 *
 * <p><strong>Known defect (behavior preserved on extraction):</strong> the reverse mapping is currently the identity
 * function, which is <em>not</em> the inverse of the forward mapping. The combo box lists edges in the order North,
 * East, South, West, whereas {@link CardinalEdge} indexes them North(0), South(1), West(2), East(3). As a result,
 * loading a saved force back into the editor mislabels three of the four cardinal edges: a stored East is shown as
 * West, a stored South as East, and a stored West as South (a three-way cycle among East, South and West). North,
 * Nearest, None and the two special destinations happen to survive because their stored value equals their combo index.
 * This class deliberately reproduces that long-standing behavior; the asymmetry is pinned by
 * {@code DestinationZoneMapperTest} and is scheduled to be corrected in Phase 1, at which point
 * {@link #storedZoneToComboIndex(int)} becomes a true inverse of {@link #comboIndexToStoredZone(int)}.
 */
public final class DestinationZoneMapper {

    /**
     * Maps the destination-zone combo box index to the value stored on the force template: {@link CardinalEdge} indexes
     * for the cardinal/nearest/none entries, and the two special destination constants for the last two.
     */
    private static final Map<Integer, Integer> COMBO_INDEX_TO_STORED_ZONE = new HashMap<>();

    static {
        COMBO_INDEX_TO_STORED_ZONE.put(0, CardinalEdge.NORTH.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(1, CardinalEdge.EAST.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(2, CardinalEdge.SOUTH.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(3, CardinalEdge.WEST.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(4, CardinalEdge.NEAREST.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(5, CardinalEdge.NONE.getIndex());
        COMBO_INDEX_TO_STORED_ZONE.put(6, ScenarioForceTemplate.DESTINATION_EDGE_OPPOSITE_DEPLOYMENT);
        COMBO_INDEX_TO_STORED_ZONE.put(7, ScenarioForceTemplate.DESTINATION_EDGE_RANDOM);
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
     * Reverse mapping used when loading a force into the editor.
     *
     * <p>Currently the identity function, which preserves the dialog's existing (defective) behavior described in the
     * class documentation. Do not "correct" this in isolation: the fix belongs to Phase 1 and flips the paired
     * assertions in {@code DestinationZoneMapperTest}.
     *
     * @param storedZone the destination-zone value stored on the force template
     *
     * @return the combo box index to select
     */
    public static int storedZoneToComboIndex(int storedZone) {
        return storedZone;
    }
}

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
package mekhq.gui.baseComponents.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A segmented tab strip in the style of the deployment wizard's mode selector: one cell per page, the selected cell
 * raised to the surface colour with the map's accent underline. Fires on mouse release and on space/enter.
 * {@code compact} draws a lighter, smaller strip for a second level of tabs.
 *
 * <p>Choosing a cell only notifies the owner, which selects it with {@link #setSelected(Object)}. Use
 * {@link HudModeSelector} when the pages are the values of an enum.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudTabStrip extends HudSegmentedControl<Integer> {
    /**
     * Builds a strip with one cell per label, in order.
     *
     * @param labels   the cell labels, left to right
     * @param compact  {@code true} for the smaller cell padding and font
     * @param onSelect called with the index of the cell the player picks
     */
    public HudTabStrip(List<String> labels, boolean compact, IntConsumer onSelect) {
        super(onSelect::accept, compact ? COMPACT_TABS : TABS);
        List<Segment<Integer>> segments = new ArrayList<>();
        for (int index = 0; index < labels.size(); index++) {
            segments.add(new Segment<>(index, labels.get(index), null, true));
        }
        setSegments(segments);
    }

    /**
     * Replaces a cell's label, for example to update a count.
     *
     * @param index the cell's position, from {@code 0}
     * @param label the new label
     */
    public void setLabel(int index, String label) {
        setSegmentTitle(index, label);
    }
}

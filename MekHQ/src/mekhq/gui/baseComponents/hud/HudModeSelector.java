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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A segmented page selector: one cell per value of an enum, with the interstellar-map tab's glowing accent underline on
 * the selected cell. Choosing a cell notifies the owner, which switches pages and selects the cell with
 * {@link #setSelected(Object)}.
 *
 * @param <E> the enum whose values are the pages
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudModeSelector<E extends Enum<E>> extends HudSegmentedControl<E> {
    /**
     * @param modeType the enum whose values are the pages, in the order they are shown
     * @param labeler  gives each page's label
     * @param onSelect called when the player chooses a page
     */
    public HudModeSelector(Class<E> modeType, Function<E, String> labeler, Consumer<E> onSelect) {
        super(onSelect, MODES);
        List<Segment<E>> segments = new ArrayList<>();
        for (E mode : modeType.getEnumConstants()) {
            segments.add(new Segment<>(mode, labeler.apply(mode), null, true));
        }
        setSegments(segments);
    }

    /**
     * Locks or unlocks a mode's cell. A locked cell is dimmed and ignores clicks and keys, so the page cannot be
     * selected.
     *
     * @param mode    the page to lock or unlock
     * @param enabled {@code true} to let the player choose the page
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setModeEnabled(E mode, boolean enabled) {
        setSegmentEnabled(mode, enabled);
    }

    /**
     * Replaces a page's label, for example to show a count.
     *
     * @param mode  the page whose label changes; does nothing if the selector has no such page
     * @param label the new label text
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setModeLabel(E mode, String label) {
        setSegmentTitle(mode, label);
    }
}

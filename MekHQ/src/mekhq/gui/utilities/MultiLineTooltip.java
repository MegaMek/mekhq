/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import megamek.common.util.StringUtil;

/**
 * This class splits a tooltip into multiple lines in order to wrap it.
 * <p>
 * Previously it enclosed the strings in HTML (<a
 * href="https://stackoverflow.com/questions/868651/multi-line-tooltips-in-java">Stack Overflow</a>), though it seems
 * like that is not necessary if the tooltip does not otherwise need formatting.
 *
 * @author Paul Taylor (adapted by Miguel Azevedo)
 */
public class MultiLineTooltip {
    private static final int DIALOG_TOOLTIP_MAX_SIZE = 85;
    private static final int SPACE_BUFFER = 10;

    /**
     * Wraps a string to a given length in characters, defaulting the size of a line to 85 characters.
     *
     * @param tip String to split
     *
     * @return Split string
     */
    public static String splitToolTip(String tip) {
        return splitToolTip(tip, DIALOG_TOOLTIP_MAX_SIZE);
    }

    /**
     * Wraps a string to a given line length in characters.
     *
     * @param tip    String to split
     * @param length Maximum characters that each line can have
     *
     * @return Split string
     */
    public static String splitToolTip(String tip, int length) {
        if (tip.length() <= length + SPACE_BUFFER) {
            return tip;
        }
        return StringUtil.wrapLines(tip, length);
    }
}

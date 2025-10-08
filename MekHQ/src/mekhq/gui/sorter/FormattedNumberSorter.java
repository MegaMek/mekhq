/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Comparator;

import megamek.logging.MMLogger;

/**
 * A comparator for numbers that have been formatted with DecimalFormat
 *
 * @author Jay Lawson
 */
public class FormattedNumberSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(FormattedNumberSorter.class);

    private static final String PLUS_SIGN = "+";
    private static final DecimalFormat FORMAT = new DecimalFormat();

    @Override
    public int compare(String s0, String s1) {
        // Cut off leading "+" sign if there
        if (s0.startsWith(PLUS_SIGN)) {
            s0 = s0.substring(1);
        }

        if (s1.startsWith(PLUS_SIGN)) {
            s1 = s1.substring(1);
        }
        // Empty cells are smaller than all numbers
        if (s0.isBlank() && s1.isBlank()) {
            return 0;
        } else if (s0.isBlank()) {
            return -1;
        } else if (s1.isBlank()) {
            return 1;
        }
        // let's find the weight class integer for each name
        long l0 = 0;
        try {
            l0 = FORMAT.parse(s0).longValue();
        } catch (ParseException e) {
            LOGGER.error("", e);
        }
        long l1 = 0;
        try {
            l1 = FORMAT.parse(s1).longValue();
        } catch (ParseException e) {
            LOGGER.error("", e);
        }
        return Long.compare(l0, l1);
    }
}

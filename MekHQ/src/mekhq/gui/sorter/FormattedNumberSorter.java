/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
    private static final MMLogger logger = MMLogger.create(FormattedNumberSorter.class);

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
        // lets find the weight class integer for each name
        long l0 = 0;
        try {
            l0 = FORMAT.parse(s0).longValue();
        } catch (ParseException e) {
            logger.error("", e);
        }
        long l1 = 0;
        try {
            l1 = FORMAT.parse(s1).longValue();
        } catch (ParseException e) {
            logger.error("", e);
        }
        return Long.compare(l0, l1);
    }
}

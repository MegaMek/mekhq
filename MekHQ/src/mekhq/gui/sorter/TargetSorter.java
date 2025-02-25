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

import java.util.Comparator;

import megamek.logging.MMLogger;

/**
 * A comparator for target numbers written as strings
 *
 * @author Jay Lawson
 */
public class TargetSorter implements Comparator<String> {
    private static final MMLogger logger = MMLogger.create(TargetSorter.class);

    @Override
    public int compare(String s0, String s1) {
        s0 = s0.replaceAll("\\+", "");
        s1 = s1.replaceAll("\\+", "");
        int r0;
        int r1;

        switch (s0) {
            case "Impossible":
                r0 = Integer.MAX_VALUE;
                break;
            case "Automatic Failure":
                r0 = Integer.MAX_VALUE - 1;
                break;
            case "Automatic Success":
                r0 = Integer.MIN_VALUE;
                break;
            default:
                try {
                    r0 = Integer.parseInt(s0);
                } catch (Exception e) {
                    logger.error("", e);
                    r0 = Integer.MAX_VALUE - 1;
                }
                break;
        }

        switch (s1) {
            case "Impossible":
                r1 = Integer.MAX_VALUE;
                break;
            case "Automatic Failure":
                r1 = Integer.MAX_VALUE - 1;
                break;
            case "Automatic Success":
                r1 = Integer.MIN_VALUE;
                break;
            default:
                try {
                    r1 = Integer.parseInt(s1);
                } catch (Exception e) {
                    logger.error("", e);
                    r1 = Integer.MAX_VALUE - 1;
                }
                break;
        }

        return Integer.compare(r0, r1);
    }
}

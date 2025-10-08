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

import java.util.Comparator;

import megamek.logging.MMLogger;

/**
 * A comparator for bonuses written as strings with "-" sorted to the bottom always
 *
 * @author Jay Lawson
 */
public class BonusSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(BonusSorter.class);

    @Override
    public int compare(String s0, String s1) {
        int i0, i1;

        if (s0.contains("/")) {
            String[] temp = s0.split("/");
            if (temp[0].contains("-") && temp[1].contains("-")) {
                i0 = 99;
            } else {
                int t0;
                try {
                    t0 = temp[0].contains("-") ? 0 : Integer.parseInt(temp[0]);
                } catch (Exception e) {
                    LOGGER.error("", e);
                    t0 = 0;
                }

                int t1;
                try {
                    t1 = temp[1].contains("-") ? 0 : Integer.parseInt(temp[1]);
                } catch (Exception e) {
                    LOGGER.error("", e);
                    t1 = 0;
                }
                i0 = t0 + t1;
            }
        } else {
            try {
                i0 = s0.equals("-") ? 90 : Integer.parseInt(s0);
            } catch (Exception e) {
                LOGGER.error("", e);
                i0 = 90;
            }
        }

        if (s1.contains("/")) {
            String[] temp = s1.split("/");
            if (temp[0].contains("-") && temp[1].contains("-")) {
                i1 = 99;
            } else {
                int t0;
                try {
                    t0 = temp[0].contains("-") ? 0 : Integer.parseInt(temp[0]);
                } catch (Exception e) {
                    LOGGER.error("", e);
                    t0 = 0;
                }

                int t1;
                try {
                    t1 = temp[1].contains("-") ? 0 : Integer.parseInt(temp[1]);
                } catch (Exception e) {
                    LOGGER.error("", e);
                    t1 = 0;
                }
                i1 = t0 + t1;
            }
        } else {
            try {
                i1 = s1.equals("-") ? 90 : Integer.parseInt(s1);
            } catch (Exception e) {
                LOGGER.error("", e);
                i1 = 90;
            }
        }

        return Integer.compare(i0, i1);
    }
}

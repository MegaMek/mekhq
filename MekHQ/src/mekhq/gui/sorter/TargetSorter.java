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
 * A comparator for target numbers written as strings
 *
 * @author Jay Lawson
 */
public class TargetSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(TargetSorter.class);

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
                    LOGGER.error("", e);
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
                    LOGGER.error("", e);
                    r1 = Integer.MAX_VALUE - 1;
                }
                break;
        }

        return Integer.compare(r0, r1);
    }
}

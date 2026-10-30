/*
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.logging.MMLogger;

/**
 * A comparator for target numbers written as strings
 *
 * @author Jay Lawson
 */
public class TargetSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(TargetSorter.class);
    private static final Pattern TARGET_NUMBER = Pattern.compile("-?\\d+");

    @Override
    public int compare(String target0, String target1) {
        return Integer.compare(toSortValue(target0), toSortValue(target1));
    }

    /**
     * @return the number a target string sorts by. A target the dice cannot reach, shown as {@code Impossible (13+)},
     *       sorts by its number: after every target that can be rolled and before the outright impossible ones.
     */
    private static int toSortValue(String target) {
        String targetWithoutPlus = target.replace("+", "");
        return switch (targetWithoutPlus) {
            case "Impossible" -> Integer.MAX_VALUE;
            case "Automatic Failure" -> Integer.MAX_VALUE - 1;
            case "Automatic Success" -> Integer.MIN_VALUE;
            default -> {
                Matcher targetNumber = TARGET_NUMBER.matcher(targetWithoutPlus);
                if (!targetNumber.find()) {
                    LOGGER.error("Cannot sort target number text: {}", target);
                    yield Integer.MAX_VALUE - 1;
                }
                try {
                    yield Integer.parseInt(targetNumber.group());
                } catch (NumberFormatException exception) {
                    LOGGER.error(exception, "Cannot sort target number text: {}", target);
                    yield Integer.MAX_VALUE - 1;
                }
            }
        };
    }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.personnel.enums.education.EducationLevel;

/**
 * A comparator for sorting Education Level {@link String} values.
 *
 * <p>This comparator relies on the {@link EducationLevel#fromString(String)} method to convert string
 * representations into {@link EducationLevel} objects, and then orders them based on their
 * {@link EducationLevel#getOrder()} value.</p>
 *
 * <p>If either string cannot be parsed into a valid {@code EducationLevel}, an error will be logged and the
 * malformed string will be placed after valid entries.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class EducationLevelSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(EducationLevelSorter.class);

    @Override
    public int compare(String firstString, String secondString) {
        try {
            int firstOrder = EducationLevel.fromString(firstString).getOrder();
            int secondOrder = EducationLevel.fromString(secondString).getOrder();
            return Integer.compare(firstOrder, secondOrder);
        } catch (Exception e) {
            LOGGER.error("Error comparing education levels: {} or {}", firstString, secondString, e);
            // malformed/non-matching strings go last
            return 1;
        }
    }
}

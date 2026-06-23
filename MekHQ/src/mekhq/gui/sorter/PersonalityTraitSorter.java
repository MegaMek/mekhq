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
package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.Objects;

import mekhq.campaign.randomEvents.personalities.PersonalityTrait;

public class PersonalityTraitSorter implements Comparator<PersonalityTrait> {

    public static final PersonalityTraitSorter INSTANCE = new PersonalityTraitSorter();

    @Override
    public int compare(PersonalityTrait first, PersonalityTrait second) {
        if (Objects.equals(first, second)) {
            return 0;
        } else if (first == null) {
            return -1;
        } else if (second == null) {
            return 1;
        } else if ((first.isNone() && second.isNone())) {
            return 0;
        } else {
            int firstIntScore = first.isTraitPositive() ? 1 : (first.isNone() ? 0 : -1);
            int secondIntScore = second.isTraitPositive() ? 1 : (second.isNone() ? 0 : -1);
            int scoreComparison = Integer.compare(firstIntScore, secondIntScore);
            if (scoreComparison != 0) {
                return scoreComparison;
            }
            int majorComparison = Boolean.compare(first.isTraitMajor(), second.isTraitMajor());
            if (!first.isTraitPositive()) {
                majorComparison = -majorComparison;
            }
            if (majorComparison != 0) {
                return majorComparison;
            }
            return first.toString().compareTo(second.toString());
        }
    }
}

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

package mekhq.campaign.personnel.backgrounds;

import megamek.common.Compute;
import mekhq.campaign.personnel.Person;

public class Toughness {
    /**
     * Generates the toughness attribute for a character.
     *
     * @param person The person for whom the toughness attribute is being generated.
     */
    public static void generateToughness(Person person) {
        int roll = Compute.d6(2);

        if (roll == 2) {
            person.setToughness(-1);
        } else if (roll == 12) {
            person.setToughness(1);
        }
    }
}

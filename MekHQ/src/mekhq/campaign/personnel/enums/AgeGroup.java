/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum AgeGroup {
    // region Enum Declarations
    ELDER("AgeGroup.ELDER.text", "AgeGroup.ELDER.toolTipText", 65),
    ADULT("AgeGroup.ADULT.text", "AgeGroup.ADULT.toolTipText", 20),
    TEENAGER("AgeGroup.TEENAGER.text", "AgeGroup.TEENAGER.toolTipText", 13),
    PRETEEN("AgeGroup.PRETEEN.text", "AgeGroup.PRETEEN.toolTipText", 10),
    CHILD("AgeGroup.CHILD.text", "AgeGroup.CHILD.toolTipText", 3),
    TODDLER("AgeGroup.TODDLER.text", "AgeGroup.TODDLER.toolTipText", 1),
    BABY("AgeGroup.BABY.text", "AgeGroup.BABY.toolTipText", -1);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final int groupLowerBound; // the lower bound of the age range for this age group, inclusive
    // endregion Variable Declarations

    // region Constructors
    AgeGroup(final String name, final String toolTipText, final int groupLowerBound) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.groupLowerBound = groupLowerBound;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public int getGroupLowerBound() {
        return groupLowerBound;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isElder() {
        return this == ELDER;
    }

    public boolean isAdult() {
        return this == ADULT;
    }

    public boolean isTeenager() {
        return this == TEENAGER;
    }

    public boolean isPreteen() {
        return this == PRETEEN;
    }

    public boolean isChild() {
        return this == CHILD;
    }

    public boolean isToddler() {
        return this == TODDLER;
    }

    public boolean isBaby() {
        return this == BABY;
    }
    // endregion Boolean Comparison Methods

    public static AgeGroup determineAgeGroup(final int age) {
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            if (age >= ageGroup.getGroupLowerBound()) {
                return ageGroup;
            }
        }

        MMLogger.create(AgeGroup.class).error("Illegal age of " + age + " entered for a person. Returning Adult");

        // This is a default return, which will only happen on error cases
        return ADULT;
    }

    @Override
    public String toString() {
        return name;
    }
}

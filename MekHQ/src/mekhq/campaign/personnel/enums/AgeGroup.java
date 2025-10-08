/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

        MMLogger.create(AgeGroup.class).error("Illegal age of {} entered for a person. Returning Adult", age);

        // This is a default return, which will only happen on error cases
        return ADULT;
    }

    @Override
    public String toString() {
        return name;
    }
}

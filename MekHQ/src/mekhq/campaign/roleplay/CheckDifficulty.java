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
package mekhq.campaign.roleplay;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * How hard the situation makes a check, shown as five named levels on the Checks page. The modifier is added to the
 * target number, so a positive value makes the check harder.
 */
public enum CheckDifficulty {
    VERY_EASY("VERY_EASY", -3),
    EASY("EASY", -1),
    NORMAL("NORMAL", 0),
    HARD("HARD", 2),
    VERY_HARD("VERY_HARD", 4);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final String label;
    private final int modifier;

    CheckDifficulty(String lookupName, int modifier) {
        this.label = getTextAt(RESOURCE_BUNDLE, "CheckDifficulty." + lookupName + ".label");
        this.modifier = modifier;
    }

    public String getLabel() {
        return label;
    }

    /**
     * @return the change to the target number; positive makes the check harder
     */
    public int getModifier() {
        return modifier;
    }

    /**
     * @return the modifier with its sign, such as "+2", "-1" or "+0"
     */
    public String getSignedModifier() {
        return signed(modifier);
    }

    /**
     * @param value a modifier
     *
     * @return the modifier with its sign, such as "+2", "-1" or "+0"
     */
    public static String signed(final int value) {
        return (value > 0) ? "+" + value : (value < 0) ? "-" + Math.abs(value) : "+0";
    }

    @Override
    public String toString() {
        return getLabel();
    }
}

/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.parts.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum RefitClass {
    NO_CHANGE(0,"RefitClass.NO_CHANGE.text"),
    OMNI_RECONFIG(1,"RefitClass.NO_CHANGE.text"),
    CLASS_A(2,"RefitClass.CLASS_A.text"),
    CLASS_B(3,"RefitClass.CLASS_B.text"),
    CLASS_C(4,"RefitClass.CLASS_C.text"),
    CLASS_D(5,"RefitClass.CLASS_D.text"),
    CLASS_E(6,"RefitClass.CLASS_E.text"),
    CLASS_F(7,"RefitClass.CLASS_F.text");

    private final int severity;
    private final String name;

    RefitClass(final int severity, final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
                MekHQ.getMHQOptions().getLocale());
        this.severity = severity;
        this.name = resources.getString(name);
    }

    /**
     * @return the translation's name for the refit class
     */
    public String getName() {
        return name;
    }

    /**
     * @param other - another RefitClass to compare to
     * @return the more difficult RefitClass of the two
     */
    public RefitClass keepHardest(RefitClass other) {
        if (other.severity > this.severity) {
            return other;
        } else {
            return this;
        }
    }

    /**
     * Gets the refit time multiplier for this refit class per CamOps 211
     * @param usingRefitKit - are we using a refit kit?
     * @return float time multipler
     */
    public float getTimeMultiplier(boolean usingRefitKit) {
        int mult = switch(this) {
            case NO_CHANGE -> 0;
            case OMNI_RECONFIG -> 1; // Omni reconfig takes base time on CamOps 205.
            case CLASS_A -> 2;
            case CLASS_B -> 3;
            case CLASS_C -> 5;
            case CLASS_D -> 8;
            case CLASS_E -> 9;
            case CLASS_F -> 10;
        };
        if (usingRefitKit) {
            mult *= 0.5;
        }
        return mult;
    }

    /**
     * Gets the refit difficulty modifier for this refit per CamOps 211.
     * The Refit Kit bonus needs to be handled elsewhere for the modifier summary to look good.
     * @return int difficulty modifier
     */
    public int getDifficultyModifier() {
        return switch (this) {
            case NO_CHANGE -> 0;
            case OMNI_RECONFIG -> -2; // Omni reconfiguartion has a skill of -2 per CamOps 205.
            case CLASS_A -> 2;
            case CLASS_B, CLASS_C -> 3;
            case CLASS_D, CLASS_E -> 4;
            case CLASS_F -> 5;
        };
    }

}
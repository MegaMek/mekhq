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

/**
 * Represents the class of a refit, and provides lookup functions for the difficulty and time
 * multipliers implied by that class. Based on Campaign Operations.
 */
public enum RefitClass {
    NO_CHANGE(0, "RefitClass.NO_CHANGE.text", "RefitClass.NO_CHANGE.shortText"),
    OMNI_RECONFIG(1, "RefitClass.OMNI_RECONFIG.text", "RefitClass.OMNI_RECONFIG.shortText"),
    CLASS_A(2, "RefitClass.CLASS_A.text", "RefitClass.CLASS_A.shortText"),
    CLASS_B(3, "RefitClass.CLASS_B.text", "RefitClass.CLASS_B.shortText"),
    CLASS_C(4, "RefitClass.CLASS_C.text", "RefitClass.CLASS_C.shortText"),
    CLASS_D(5, "RefitClass.CLASS_D.text", "RefitClass.CLASS_D.shortText"),
    CLASS_E(6, "RefitClass.CLASS_E.text", "RefitClass.CLASS_E.shortText"),
    CLASS_F(7, "RefitClass.CLASS_F.text", "RefitClass.CLASS_F.shortText"),
    PLEASE_REPAIR(8, "RefitClass.PLEASE_REPAIR.text", "RefitClass.PLEASE_REPAIR.shortText");

    private final int severity;
    private final String name;
    private final String shortName;

    RefitClass(final int severity, final String name, final String shortName) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
                MekHQ.getMHQOptions().getLocale());
        this.severity = severity;
        this.name = resources.getString(name);
        this.shortName = resources.getString(shortName);
    }

    /**
     * @return the translation's name for the refit class
     */
    public String toName() {
        return name;
    }

    public String toShortName() {
        return shortName;
    }

    /**
     * @return numeric class for XML
     */
    public int toNumeric() {
        return severity;
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
            case PLEASE_REPAIR -> 0;
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
            case NO_CHANGE, PLEASE_REPAIR -> 0;
            case OMNI_RECONFIG -> -2; // Omni reconfiguartion has a skill of -2 per CamOps 205.
            case CLASS_A -> 2;
            case CLASS_B, CLASS_C -> 3;
            case CLASS_D, CLASS_E -> 4;
            case CLASS_F -> 5;
        };
    }

    /**
     * @param numeric value for returning from XML
     */
    public static RefitClass fromNumeric(int numeric) {
        return switch(numeric) {
            case 0 -> NO_CHANGE;
            case 1 -> OMNI_RECONFIG;
            case 2 -> CLASS_A;
            case 3 -> CLASS_B;
            case 4 -> CLASS_C;
            case 5 -> CLASS_D;
            case 6 -> CLASS_E;
            case 7 -> CLASS_F;
            case 8 -> PLEASE_REPAIR;
            default -> throw new IllegalArgumentException("RefitClass.fromNumeric must be 0-8");
        };
    }

}
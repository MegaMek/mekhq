/*
 * PartQuality.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;

import java.util.*;

/**
 * Represents the quality of a Part. Quality is a scale that ranges from A to F. By the book, A
 * is bad and F is good, but there is an option that inverts this scale, hence the 'reverse'
 * options on the various functions available here.
 * 
 * Internally quality is represented by a number 0 to 5, bad to good.
 */
public enum PartQuality {
    A(0),
    B(1),
    C(2),
    D(3),
    E(4),
    F(5);

    public final int numericQuality;

    private PartQuality(int quality) {
        this.numericQuality = quality;
    }

    /**
     * @return numeric quality 0-5 bad-good
     */
    public int toNumeric() {
        return this.numericQuality;
    }

    /**
     * @param rawQuality - numeric quality 0-5 bad-good
     * @return corresponding PartQuality
     * @throws IllegalArgumentException
     */
    public static PartQuality fromNumeric(int rawQuality) {
        return switch (rawQuality) {
            case 0 -> A;
            case 1 -> B;
            case 2 -> C;
            case 3 -> D;
            case 4 -> E;
            case 5 -> F;
            default -> throw new IllegalArgumentException("rawQuality must be int 0-5");
        };
    }

    /**
     * @param reverse - are quality names reversed per the campaign option
     * @return String letter name for quality A-F bad-good (or good-bad if reversed)
     */
    public String toName(boolean reversed) {
        if (!reversed) {
            return switch(this) {
                case A -> "A";
                case B -> "B";
                case C -> "C";
                case D -> "D";
                case E -> "E";
                case F -> "F";
            };  
        } else {
            return switch(this) {
                case B -> "E";
                case A -> "F";
                case C -> "D";
                case D -> "C";
                case E -> "B";
                case F -> "A";
                default -> "?";
            };
        }
    }

  /**
   * @param code - one-character String name from A-F bad-good (or good-bad if reversed)
   * @param reverse - are quality names reversed per the campaign option
   * @return corresponding PartQuality
   * @throws IllegalArgumentException
   */
    public static PartQuality fromName(String code, boolean reverse) {
        if (!reverse) {
            return switch(code) {
                case "A" -> PartQuality.A;
                case "B" -> PartQuality.B;
                case "C" -> PartQuality.C;
                case "D" -> PartQuality.D;
                case "E" -> PartQuality.E;
                case "F" -> PartQuality.F;
                default -> throw new IllegalArgumentException("Expecting one-char string A to F");
            };
        } else {
            return switch(code) {
                case "F" -> PartQuality.A;
                case "E" -> PartQuality.B;
                case "D" -> PartQuality.C;
                case "C" -> PartQuality.D;
                case "B" -> PartQuality.E;
                case "A" -> PartQuality.F;
                default -> throw new IllegalArgumentException("Expecting one-char string A to F");
            };
        }
    }

    /**
     * @return modifier for repair rolls using a part of this quality
     */
    public int getRepairModifier(){
        return switch(this) {
            case A -> 3;
            case B -> 2;
            case C -> 1;
            case D -> 0;
            case E -> -1;
            case F -> -2;
        };
    }

    /**
     * @return Hex color code for coloring parts of this quality.
     */
    public String getHexColor() {
        return switch (this) {
            case A, B -> MekHQ.getMHQOptions().getFontColorNegativeHexColor();
            case C, D -> MekHQ.getMHQOptions().getFontColorWarningHexColor();
            case E, F -> MekHQ.getMHQOptions().getFontColorPositiveHexColor();
            
        };
    }

    /**
     * @return PartQuality that is one step better than this one, clamped
     */
    public PartQuality improveQuality() {
        if (this == F) {
            return this;
        } else {
            return fromNumeric(toNumeric() + 1);
        }
    }

    /**
     * @return PartQuality that is one step worse than this one, clamped
     */
    public PartQuality reduceQuality() {
        if (this == A) {
            return this;
        } else {
            return fromNumeric(toNumeric() - 1);
        }
    }

    /**
     * @return A list of PartQualities in order bad to good
     */
    public static List<PartQuality> allQualities() {
            return List.of(A,B,C,D,E,F);
    }
}
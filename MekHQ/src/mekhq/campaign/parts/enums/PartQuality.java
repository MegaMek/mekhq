/*
 * PartQuality.java
 *
 * Copyright (c) 2022-2024 - The MegaMek Team. All Rights Reserved.
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
    QUALITY_A(0),
    QUALITY_B(1),
    QUALITY_C(2),
    QUALITY_D(3),
    QUALITY_E(4),
    QUALITY_F(5);

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
            case 0 -> QUALITY_A;
            case 1 -> QUALITY_B;
            case 2 -> QUALITY_C;
            case 3 -> QUALITY_D;
            case 4 -> QUALITY_E;
            case 5 -> QUALITY_F;
            default -> throw new IllegalArgumentException("rawQuality must be int 0-5");
        };
    }

    /**
     * @param reversed - are quality names reversed per the campaign option
     * @return String letter name for quality A-F bad-good (or good-bad if reversed)
     */
    public String toName(boolean reversed) {
        if (!reversed) {
            return switch(this) {
                case QUALITY_A -> "A";
                case QUALITY_B -> "B";
                case QUALITY_C -> "C";
                case QUALITY_D -> "D";
                case QUALITY_E -> "E";
                case QUALITY_F -> "F";
            };  
        } else {
            return switch(this) {
                case QUALITY_B -> "E";
                case QUALITY_A -> "F";
                case QUALITY_C -> "D";
                case QUALITY_D -> "C";
                case QUALITY_E -> "B";
                case QUALITY_F -> "A";
                default -> "?";
            };
        }
    }

  /**
   * @param code - one-character String name from A-F bad-good (or good-bad if reversed)
   * @param reversed - are quality names reversed per the campaign option
   * @return corresponding PartQuality
   * @throws IllegalArgumentException
   */
    public static PartQuality fromName(String code, boolean reversed) {
        if (!reversed) {
            return switch(code) {
                case "A" -> PartQuality.QUALITY_A;
                case "B" -> PartQuality.QUALITY_B;
                case "C" -> PartQuality.QUALITY_C;
                case "D" -> PartQuality.QUALITY_D;
                case "E" -> PartQuality.QUALITY_E;
                case "F" -> PartQuality.QUALITY_F;
                default -> throw new IllegalArgumentException("Expecting one-char string A to F");
            };
        } else {
            return switch(code) {
                case "F" -> PartQuality.QUALITY_A;
                case "E" -> PartQuality.QUALITY_B;
                case "D" -> PartQuality.QUALITY_C;
                case "C" -> PartQuality.QUALITY_D;
                case "B" -> PartQuality.QUALITY_E;
                case "A" -> PartQuality.QUALITY_F;
                default -> throw new IllegalArgumentException("Expecting one-char string A to F");
            };
        }
    }

    /**
     * @return modifier for repair rolls using a part of this quality
     */
    public int getRepairModifier(){
        return switch(this) {
            case QUALITY_A -> 3;
            case QUALITY_B -> 2;
            case QUALITY_C -> 1;
            case QUALITY_D -> 0;
            case QUALITY_E -> -1;
            case QUALITY_F -> -2;
        };
    }

    /**
     * @return Hex color code for coloring parts of this quality.
     */
    public String getHexColor() {
        return switch (this) {
            case QUALITY_A, QUALITY_B -> MekHQ.getMHQOptions().getFontColorNegativeHexColor();
            case QUALITY_C, QUALITY_D -> MekHQ.getMHQOptions().getFontColorWarningHexColor();
            case QUALITY_E, QUALITY_F -> MekHQ.getMHQOptions().getFontColorPositiveHexColor();
            
        };
    }

    /**
     * @return PartQuality that is one step better than this one, clamped
     */
    public PartQuality improveQuality() {
        if (this == QUALITY_F) {
            return this;
        } else {
            return fromNumeric(toNumeric() + 1);
        }
    }

    /**
     * @return PartQuality that is one step worse than this one, clamped
     */
    public PartQuality reduceQuality() {
        if (this == QUALITY_A) {
            return this;
        } else {
            return fromNumeric(toNumeric() - 1);
        }
    }

    /**
     * @return A list of PartQualities in order bad to good
     */
    public static List<PartQuality> allQualities() {
            return List.of(QUALITY_A,QUALITY_B,QUALITY_C,QUALITY_D,QUALITY_E,QUALITY_F);
    }
}
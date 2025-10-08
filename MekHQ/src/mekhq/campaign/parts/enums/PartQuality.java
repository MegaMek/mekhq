/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.enums;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.enums.TechRating;
import mekhq.utilities.ReportingUtilities;

/**
 * Represents the quality of a Part. Quality is a scale that ranges from A to F. By the book, A is bad and F is good,
 * but there is an option that inverts this scale, hence the 'reverse' options on the various functions available here.
 * <p>
 * Internally quality is represented by a number 0 to 5, bad to good.
 */
public enum PartQuality {
    QUALITY_A(0, "A", "F", 3, TechRating.A),
    QUALITY_B(1, "B", "E", 2, TechRating.B),
    QUALITY_C(2, "C", "D", 1, TechRating.C),
    QUALITY_D(3, "D", "C", 0, TechRating.D),
    QUALITY_E(4, "E", "B", -1, TechRating.E),
    QUALITY_F(5, "F", "A", -2, TechRating.F);

    private final int index;
    private final String name;
    private final String reversedName;
    private final int repairModifier;
    private final TechRating techRating;
    private static final Map<Integer, PartQuality> INDEX_LOOKUP = new HashMap<>();
    private static final Map<String, PartQuality> NAME_LOOKUP = new HashMap<>();
    private static final Map<String, PartQuality> REVERSED_NAME_LOOKUP = new HashMap<>();

    static {
        for (PartQuality q : values()) {
            INDEX_LOOKUP.put(q.index, q);
            NAME_LOOKUP.put(q.name, q);
            REVERSED_NAME_LOOKUP.put(q.reversedName, q);
        }
    }

    PartQuality(int index, String name, String reversedName, int repairModifier, TechRating techRating) {
        this.index = index;
        this.name = name;
        this.reversedName = reversedName;
        this.repairModifier = repairModifier;
        this.techRating = techRating;
    }

    /**
     * @return numeric quality 0-5 bad-good
     */
    public int toNumeric() {
        return this.index;
    }

    /**
     * @return String letter name for quality A-F bad-good
     */
    public String getName() {
        return getName(false);
    }

    /**
     * @param reversed - are quality names reversed per the campaign option
     *
     * @return String letter name for quality A-F bad-good (or good-bad if reversed)
     */
    public String getName(boolean reversed) {
        if (reversed) {
            return this.reversedName;
        }
        return this.name;
    }

    /**
     * @return TechRating for this quality
     */
    public TechRating getTechRating() {
        return this.techRating;
    }

    /**
     * @param rawQuality - numeric quality 0-5 bad-good
     *
     * @return corresponding PartQuality
     *
     */
    public static PartQuality fromNumeric(int rawQuality) {
        try {
            return INDEX_LOOKUP.get(rawQuality);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("rawQuality must be int 0-5");
        }
    }

    /**
     * @param reversed - are quality names reversed per the campaign option
     *
     * @return String letter name for quality A-F bad-good (or good-bad if reversed)
     */
    public String toName(boolean reversed) {
        return getName(reversed);
    }

    /**
     * @param code     - one-character String name from A-F bad-good (or good-bad if reversed)
     * @param reversed - are quality names reversed per the campaign option
     *
     * @return corresponding PartQuality
     *
     */
    public static PartQuality fromName(String code, boolean reversed) {
        try {
            if (!reversed) {
                return NAME_LOOKUP.get(code);
            } else {
                return REVERSED_NAME_LOOKUP.get(code);
            }
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Expecting one-char string A to F");
        }
    }

    /**
     * @return modifier for repair rolls using a part of this quality
     */
    public int getRepairModifier() {
        return this.repairModifier;
    }

    /**
     * @return Hex color code for coloring parts of this quality.
     */
    public String getHexColor() {
        return switch (this) {
            case QUALITY_A, QUALITY_B -> ReportingUtilities.getNegativeColor();
            case QUALITY_C, QUALITY_D -> ReportingUtilities.getWarningColor();
            case QUALITY_E, QUALITY_F -> ReportingUtilities.getPositiveColor();

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
    public static List<PartQuality> allQualities(boolean reversed) {
        if (reversed) {
            return List.of(QUALITY_F, QUALITY_E, QUALITY_D, QUALITY_C, QUALITY_B, QUALITY_A);
        }
        return List.of(QUALITY_A, QUALITY_B, QUALITY_C, QUALITY_D, QUALITY_E, QUALITY_F);
    }
}

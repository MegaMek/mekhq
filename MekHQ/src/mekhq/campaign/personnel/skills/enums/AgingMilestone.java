/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.skills.enums;

import static java.lang.Integer.MAX_VALUE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NO_SKILL_ATTRIBUTE;
import static mekhq.utilities.MHQInternationalization.getTextAt;

public enum AgingMilestone {
    NONE(0, 25, 0, 0, 0, 0, 0, 0, 0, 0, false, false),
    TWENTY_FIVE(25, 31, 50, 50, 0, 50, 50, 50, 50, 0, false, false),
    THIRTY_ONE(31, 41, 50, 50, 0, 50, 50, 50, 0, 1, false, false),
    FORTY_ONE(41, 51, 0, 0, -50, 0, 0, 0, 0, 1, false, false),
    FIFTY_ONE(51, 61, 0, -100, 0, -100, 0, 0, -50, 2, false, false),
    SIXTY_ONE(61, 71, -100, -100, -100, 0, 50, 0, -50, 2, true, false),
    SEVENTY_ONE(71, 81, -100, -125, 0, -100, 0, -50, -75, 2, true, true),
    EIGHTY_ONE(81, 91, -150, -150, -100, -100, -100, -50, -100, 2, true, true),
    NINETY_ONE(91, 101, -150, -175, -150, -125, -150, -100, -100, 2, true, true),
    ONE_HUNDRED_ONE(101, MAX_VALUE, -200, -200, -200, -150, -200, -100, -150, 2, true, true);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AgingMilestone";

    public static final int CLAN_REPUTATION_MULTIPLIER = 150;
    public static final int STAR_CAPTAIN_RANK_INDEX = 34;
    public static final int STAR_CAPTAIN_REPUTATION_MULTIPLIER = 1;
    public static final int STAR_COLONEL_RANK_INDEX = 38;
    public static final int STAR_COLONEL_REPUTATION_MULTIPLIER = 2;

    // Attributes
    private final int milestone;
    private final int maximumAge;
    private final int strength;
    private final int body;
    private final int dexterity;
    private final int reflexes;
    private final int intelligence;
    private final int willpower;
    private final int charisma;
    private final int reputation;
    private final boolean slowLearner;
    private final boolean glassJaw;
    // Cumulative values
    private int cumulativeStrength;
    private int cumulativeBody;
    private int cumulativeDexterity;
    private int cumulativeReflexes;
    private int cumulativeIntelligence;
    private int cumulativeWillpower;
    private int cumulativeCharisma;

    // Static block to calculate cumulative modifiers
    static {
        int cumulativeStrength = 0;
        int cumulativeBody = 0;
        int cumulativeDexterity = 0;
        int cumulativeReflexes = 0;
        int cumulativeIntelligence = 0;
        int cumulativeWillpower = 0;
        int cumulativeCharisma = 0;

        for (AgingMilestone milestone : values()) {
            cumulativeStrength += milestone.strength;
            cumulativeBody += milestone.body;
            cumulativeDexterity += milestone.dexterity;
            cumulativeReflexes += milestone.reflexes;
            cumulativeIntelligence += milestone.intelligence;
            cumulativeWillpower += milestone.willpower;
            cumulativeCharisma += milestone.charisma;

            milestone.cumulativeStrength = cumulativeStrength;
            milestone.cumulativeBody = cumulativeBody;
            milestone.cumulativeDexterity = cumulativeDexterity;
            milestone.cumulativeReflexes = cumulativeReflexes;
            milestone.cumulativeIntelligence = cumulativeIntelligence;
            milestone.cumulativeWillpower = cumulativeWillpower;
            milestone.cumulativeCharisma = cumulativeCharisma;
        }
    }

    // Constructor
    AgingMilestone(int milestone, int maximumAge, int strength, int body, int dexterity, int reflexes, int intelligence,
          int willpower, int charisma, int reputation, boolean slowLearner, boolean glassJaw) {
        this.milestone = milestone;
        this.maximumAge = maximumAge;
        this.strength = strength;
        this.body = body;
        this.dexterity = dexterity;
        this.reflexes = reflexes;
        this.intelligence = intelligence;
        this.willpower = willpower;
        this.charisma = charisma;
        this.reputation = reputation;
        this.slowLearner = slowLearner;
        this.glassJaw = glassJaw;
    }

    /**
     * Retrieves the value of the specified skill attribute for this object.
     *
     * <p><b>Usage:</b> this exists to assist testing and should not be directly called. Use
     * {@link #getAttributeModifier(SkillAttribute)} instead.</p>
     *
     * @param attribute the {@link SkillAttribute} to retrieve; must not be {@code null}
     *
     * @return the integer value of the specified attribute, or {@code NO_SKILL_ATTRIBUTE} if
     *       {@code SkillAttribute.NONE} is provided
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getAttribute(SkillAttribute attribute) {
        return switch (attribute) {
            case NONE -> NO_SKILL_ATTRIBUTE;
            case STRENGTH -> strength;
            case BODY -> body;
            case DEXTERITY -> dexterity;
            case REFLEXES -> reflexes;
            case INTELLIGENCE -> intelligence;
            case WILLPOWER -> willpower;
            case CHARISMA -> charisma;
        };
    }

    // Getters

    /**
     * Use {@link #getMinimumAge()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getMilestone() {
        return milestone;
    }

    public int getMinimumAge() {
        return milestone;
    }

    public int getMaximumAge() {
        return maximumAge;
    }

    public int getAttributeModifier(SkillAttribute attribute) {
        return switch (attribute) {
            case NONE -> NO_SKILL_ATTRIBUTE;
            case STRENGTH -> cumulativeStrength;
            case BODY -> cumulativeBody;
            case DEXTERITY -> cumulativeDexterity;
            case REFLEXES -> cumulativeReflexes;
            case INTELLIGENCE -> cumulativeIntelligence;
            case WILLPOWER -> cumulativeWillpower;
            case CHARISMA -> cumulativeCharisma;
        };
    }

    public int getReputation() {
        return reputation;
    }

    public boolean isSlowLearner() {
        return slowLearner;
    }

    public boolean isGlassJaw() {
        return glassJaw;
    }

    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, "AgingMilestone." + name() + ".label");
    }
}

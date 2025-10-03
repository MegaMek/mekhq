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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.enums.SkillTypeNew.*;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import mekhq.MekHQ;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;

/**
 * Utility class providing static methods for working with experience levels and skill-related attributes.
 *
 * <p>This class offers methods for retrieving localized display names, formatting experience level names with color,
 * and obtaining color codes corresponding to specific experience levels. It supports both the raw experience level
 * integer values and {@code SkillLevel} objects.</p>
 *
 * <p>The methods in this class are intended for use throughout the application wherever experience levels or related
 * display information are needed.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SkillUtilities {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillUtilities";

    public static final int NO_SKILL = 0;
    public static final int DISABLED_SKILL_LEVEL = -1;
    public static final int MINIMUM_SKILL_LEVEL = 0;
    public static final int MAXIMUM_SKILL_LEVEL = 10;
    public static final int[] DEFAULT_SKILL_COSTS = new int[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

    public static final int SKILL_LEVEL_NONE = -1;
    public static final int SKILL_LEVEL_ULTRA_GREEN = 0;
    public static final int SKILL_LEVEL_GREEN = 1;
    public static final int SKILL_LEVEL_REGULAR = 2;
    public static final int SKILL_LEVEL_VETERAN = 3;
    public static final int SKILL_LEVEL_ELITE = 4;
    public static final int SKILL_LEVEL_HEROIC = 5;
    public static final int SKILL_LEVEL_LEGENDARY = 6;

    /**
     * Returns the localized display name for the specified experience level.
     *
     * <p>The method maps the given experience level integer constant to a key, retrieves the corresponding display
     * name from the resource bundle, and returns it. If the level does not match any predefined constant, a default
     * value is returned.</p>
     *
     * @param level the integer constant representing the experience level
     *
     * @return the localized display name for the specified experience level
     */
    public static String getExperienceLevelName(int level) {
        String key = switch (level) {
            case SKILL_LEVEL_ULTRA_GREEN -> "ultraGreen";
            case SKILL_LEVEL_GREEN -> "green";
            case SKILL_LEVEL_REGULAR -> "regular";
            case SKILL_LEVEL_VETERAN -> "veteran";
            case SKILL_LEVEL_ELITE -> "elite";
            case SKILL_LEVEL_HEROIC -> "heroic";
            case SKILL_LEVEL_LEGENDARY -> "legendary";
            case -1 -> "none";
            default -> "default";
        };

        return getTextAt(RESOURCE_BUNDLE, "SkillUtilities.skillLevel." + key);
    }

    /**
     * Returns the hexadecimal color code associated with the specified {@link SkillLevel}.
     *
     * <p>Retrieves the adjusted experience level value from the {@link SkillLevel} object and returns the color code
     * associated with that level.
     *
     * @param skillLevel the {@link SkillLevel} object representing the experience level
     *
     * @return the hex color code as a {@code String} for the given skill level
     */
    public static String getExperienceLevelColor(SkillLevel skillLevel) {
        int level = skillLevel.getAdjustedValue();
        return getExperienceLevelColor(level);
    }

    /**
     * Returns the hexadecimal color code associated with the specified experience level.
     *
     * <p>The method selects the appropriate color code for the provided experience level, as defined in the current
     * options. If the level does not match any known experience level, an empty string is returned.</p>
     *
     * @param level the integer constant representing the experience level
     *
     * @return the hex color code as a {@code String} for the given experience level, or an empty string if unspecified
     */
    public static String getExperienceLevelColor(int level) {
        return switch (level) {
            case SKILL_LEVEL_ULTRA_GREEN -> MekHQ.getMHQOptions().getFontColorSkillUltraGreenHexColor();
            case SKILL_LEVEL_GREEN -> MekHQ.getMHQOptions().getFontColorSkillGreenHexColor();
            case SKILL_LEVEL_REGULAR -> MekHQ.getMHQOptions().getFontColorSkillRegularHexColor();
            case SKILL_LEVEL_VETERAN -> MekHQ.getMHQOptions().getFontColorSkillVeteranHexColor();
            case SKILL_LEVEL_ELITE, SKILL_LEVEL_HEROIC, SKILL_LEVEL_LEGENDARY ->
                  MekHQ.getMHQOptions().getFontColorSkillEliteHexColor();
            default -> "";
        };
    }

    /**
     * Returns the experience level name with associated color formatting.
     *
     * <p>If a color is defined for the specified experience level, the name is wrapped with a span tag using the
     * corresponding hex color code. Otherwise, returns the unformatted name.</p>
     *
     * @param level the integer constant representing the experience level
     *
     * @return the experience level name, colored with its associated hex code if available; otherwise, the plain name
     */
    public static String getColoredExperienceLevelName(int level) {
        if (getExperienceLevelColor(level).isEmpty()) {
            return getExperienceLevelName(level);
        }

        return messageSurroundedBySpanWithColor(getExperienceLevelColor(level), getExperienceLevelName(level));
    }

    /**
     * Returns the experience level name with associated color formatting for the specified {@link SkillLevel}.
     *
     * <p>Retrieves the adjusted experience level from the {@link SkillLevel} object and returns the colored
     * experience level name, if available.</p>
     *
     * @param skillLevel the {@link SkillLevel} object representing the experience level
     *
     * @return the experience level name, colored with its associated hex code if available; otherwise, the plain name
     */
    public static String getColoredExperienceLevelName(SkillLevel skillLevel) {
        int level = skillLevel.getAdjustedValue();
        return getColoredExperienceLevelName(level);
    }

    /**
     * Determines the appropriate driving-related {@link SkillTypeNew} for the given entity.
     *
     * <p>The method inspects the type and movement mode of the provided {@link Entity} and returns the corresponding
     * driving skill type. Defaults to the 'Mech Piloting' skill if no other type matches.</p>
     *
     * @param entity the {@link Entity} whose driving skill is to be determined
     *
     * @return the corresponding driving-related {@link SkillTypeNew}
     */
    public static SkillTypeNew getDrivingSkillFor(Entity entity) {
        if (entity instanceof Tank) {
            return switch (entity.getMovementMode()) {
                case VTOL -> S_PILOT_VTOL;
                case NAVAL, HYDROFOIL, SUBMARINE -> S_PILOT_NVEE;
                default -> S_PILOT_GVEE;
            };
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return S_PILOT_SPACE;
        } else if (entity instanceof ConvFighter) {
            return S_PILOT_JET;
        } else if (entity instanceof Aero) {
            return S_PILOT_AERO;
        } else if (entity instanceof Infantry) {
            return S_ANTI_MEK;
        } else if (entity instanceof ProtoMek) {
            return S_GUN_PROTO;
        } else {
            return S_PILOT_MEK;
        }
    }

    /**
     * Determines the appropriate gunnery-related {@link SkillTypeNew} for the given entity.
     *
     * <p>The method evaluates the type of the provided {@code Entity} and returns the corresponding gunnery skill
     * type. Defaults to the 'Mech Gunnery' skill if no other type matches.</p>
     *
     * @param entity the {@link Entity} whose gunnery skill is to be determined
     *
     * @return the corresponding gunnery-related {@link SkillTypeNew}
     */
    public static SkillTypeNew getGunnerySkillFor(Entity entity) {
        if (entity instanceof Tank) {
            return S_GUN_VEE;
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return S_GUN_SPACE;
        } else if (entity instanceof ConvFighter) {
            return S_GUN_JET;
        } else if (entity instanceof Aero) {
            return S_GUN_AERO;
        } else if (entity instanceof Infantry) {
            if (entity instanceof BattleArmor) {
                return S_GUN_BA;
            } else {
                return S_SMALL_ARMS;
            }
        } else if (entity instanceof ProtoMek) {
            return S_GUN_PROTO;
        } else {
            return S_GUN_MEK;
        }
    }

    /**
     * Retrieves a curated list of roleplay {@link SkillTypeNew}s, sampling from multiple subtypes.
     *
     * <p>The method collects all skill types belonging to the general roleplay subtype and additionally samples one
     * random skill from each specialization (art, interest, science, security) if skills in those categories are
     * available. This ensures skill selection does not overly favor specialized subtypes.
     *
     * @return a list of selected roleplay-related {@link SkillTypeNew} values
     */
    public static List<SkillTypeNew> getRoleplaySkills() {
        List<SkillTypeNew> roleplaySkills = new ArrayList<>();
        List<SkillTypeNew> roleplaySkillsArt = new ArrayList<>();
        List<SkillTypeNew> roleplaySkillsInterest = new ArrayList<>();
        List<SkillTypeNew> roleplaySkillsScience = new ArrayList<>();
        List<SkillTypeNew> roleplaySkillsSecurity = new ArrayList<>();

        for (SkillTypeNew type : SkillTypeNew.values()) {
            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_GENERAL)) {
                roleplaySkills.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_ART)) {
                roleplaySkillsArt.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_INTEREST)) {
                roleplaySkillsInterest.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_SCIENCE)) {
                roleplaySkillsScience.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_SECURITY)) {
                roleplaySkillsSecurity.add(type);
            }
        }

        // These next few steps are so that we don't overweight skill specializations. Without this, the chances of
        // having a Science-related skill, for example, skyrocket and make those skills feel 'spammy'.
        if (!roleplaySkillsArt.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsArt));
        }

        if (!roleplaySkillsInterest.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsInterest));
        }

        if (!roleplaySkillsScience.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsScience));
        }

        if (!roleplaySkillsSecurity.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsSecurity));
        }

        return roleplaySkills;
    }
}

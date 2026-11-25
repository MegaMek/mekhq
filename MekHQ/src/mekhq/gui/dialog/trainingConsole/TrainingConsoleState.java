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
package mekhq.gui.dialog.trainingConsole;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.training.AutoTrainSettings;

/**
 * What the player has chosen in the Training Console: the page, the selection, the filters, and each page's own
 * settings. The selection is kept here rather than in the list, so it carries across filters and pages.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class TrainingConsoleState {
    /** How the people list is sorted. */
    enum Sort {
        /** Selected people first: the checkbox column. */
        SELECTED,
        NAME,
        ROLE,
        FORMATION,
        EXPERIENCE,
        XP
    }

    /**
     * Which side of the officer cut the people list shows, by whether each person's rank is an officer rank.
     *
     * @author Illiani
     * @since 0.51.01
     */
    enum RankGroup {
        OFFICERS,
        ENLISTED
    }

    /** Which skills the Skills page lists. */
    enum SkillGroup {
        HELD,
        GUNNERY,
        PILOTING,
        SUPPORT,
        UTILITY,
        ROLEPLAY,
        ALL
    }

    /** Which abilities the Abilities page lists. */
    enum AbilityGroup {
        COMBAT,
        MANEUVERING,
        UTILITY,
        CHARACTER,
        FLAWS,
        BUY_OFF
    }

    /** Which purchases the Attributes &amp; Traits page lists. */
    enum AttributeGroup {
        ATTRIBUTES,
        TRAITS,
        EDGE
    }

    /** The value of an ability choice that lets the console choose for each person. */
    static final String CHOOSE_FOR_EACH = "\u0000each";

    TrainingConsolePage page = TrainingConsolePage.SKILLS;

    /** The selected people, in the order they were selected. */
    final Set<Person> selected = new LinkedHashSet<>();

    // Filters
    String query = "";
    @Nullable PersonnelRole role;
    @Nullable RankGroup rankGroup;
    @Nullable String formation;
    @Nullable SkillLevel level;
    boolean isIncludePrisoners;
    boolean isOnlyStaged;
    boolean isHideExcluded;
    /** Selected people first, so whoever the console was opened for is at the top. */
    Sort sort = Sort.SELECTED;
    boolean isAscending = true;

    // Skills page
    SkillGroup skillGroup = SkillGroup.HELD;
    String skillQuery = "";
    @Nullable String openSkill;

    // Aptitudes page: the same skill groups as the Skills page, chosen separately
    SkillGroup aptitudeGroup = SkillGroup.HELD;
    String aptitudeQuery = "";
    @Nullable String openAptitude;
    final Map<String, Integer> trainTo = new HashMap<>();

    // Abilities page
    AbilityGroup abilityGroup = AbilityGroup.COMBAT;
    String abilityQuery = "";
    @Nullable String openAbility;
    final Map<String, String> abilityChoices = new HashMap<>();

    // Attributes & Traits page
    AttributeGroup attributeGroup = AttributeGroup.ATTRIBUTES;
    String attributeQuery = "";
    /** The open card: an attribute's or trait's enum name, or the Edge refill's key. */
    @Nullable String openAttribute;
    /** The score each attribute's "raise everyone to" is set to, by the attribute's enum name. */
    final Map<String, Integer> attributeTargets = new HashMap<>();

    // Auto-train page
    /** The auto-train settings being edited. */
    @Nullable AutoTrainSettings.Builder autoTrain;
    /** What the player has typed as a preset's name but not yet saved, kept across redraws. */
    @Nullable String presetNameText;
    @Nullable Integer openStep;
}

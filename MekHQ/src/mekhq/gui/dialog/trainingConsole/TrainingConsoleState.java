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
        NAME,
        ROLE,
        FORMATION,
        EXPERIENCE
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

    /** The value of an ability choice that lets the console choose for each person. */
    static final String CHOOSE_FOR_EACH = "\u0000each";

    TrainingConsolePage page = TrainingConsolePage.SKILLS;

    /** The selected people, in the order they were selected. */
    final Set<Person> selected = new LinkedHashSet<>();
    /** The person whose record is shown above the purchases, if any. */
    @Nullable Person focused;

    // Filters
    String query = "";
    @Nullable PersonnelRole role;
    @Nullable String formation;
    @Nullable SkillLevel level;
    boolean isIncludePrisoners;
    boolean isOnlyStaged;
    boolean isHideExcluded;
    Sort sort = Sort.NAME;
    boolean isAscending = true;

    // Skills page
    SkillGroup skillGroup = SkillGroup.HELD;
    String skillQuery = "";
    @Nullable String openSkill;
    final Map<String, Integer> trainTo = new HashMap<>();

    // Abilities page
    AbilityGroup abilityGroup = AbilityGroup.COMBAT;
    String abilityQuery = "";
    @Nullable String openAbility;
    final Map<String, String> abilityChoices = new HashMap<>();
    boolean isIncludePrerequisites;

    // Auto-train page
    @Nullable AutoTrainPage.Draft autoTrain;
    @Nullable Integer openStep;
}

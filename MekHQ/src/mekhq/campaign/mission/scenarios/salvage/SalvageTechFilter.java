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
package mekhq.campaign.mission.scenarios.salvage;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;

/**
 * The salvage planner's tech filters: hide injured techs, pregnant techs, or techs already assigned to units, show
 * only techs of one experience level, and hide techs that don't match a search.
 *
 * @param isHidingInjured   {@code true} to hide injured techs
 * @param isHidingPregnant  {@code true} to hide pregnant techs
 * @param isHidingUnitTechs {@code true} to hide techs assigned to units as their tech
 * @param skillLevel        the only experience level shown, or {@code null} for any
 * @param searchText        the search text; blank for no search
 *
 * @author Illiani
 * @since 0.51.01
 */
public record SalvageTechFilter(boolean isHidingInjured, boolean isHidingPregnant, boolean isHidingUnitTechs,
      @Nullable SkillLevel skillLevel, String searchText) {

    /** A filter that shows every tech. */
    public static final SalvageTechFilter SHOW_ALL = new SalvageTechFilter(false, false, false, null, "");

    /**
     * Why a tech is hidden. When several filters would hide a tech, the first reason in this order is given.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum HiddenReason {
        INJURED, PREGNANT, UNIT_TECH, OTHER_EXPERIENCE, NO_SEARCH_MATCH
    }

    /**
     * @param candidate the tech
     *
     * @return why the filter hides the tech, or {@code null} if it shows them
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable HiddenReason getHiddenReason(SalvageTechCandidate candidate) {
        if (isHidingInjured && candidate.isInjured()) {
            return HiddenReason.INJURED;
        }
        if (isHidingPregnant && candidate.isPregnant()) {
            return HiddenReason.PREGNANT;
        }
        if (isHidingUnitTechs && candidate.hasTechUnits()) {
            return HiddenReason.UNIT_TECH;
        }
        if ((skillLevel != null) && (candidate.skillLevel() != skillLevel)) {
            return HiddenReason.OTHER_EXPERIENCE;
        }
        if (!candidate.matches(searchText)) {
            return HiddenReason.NO_SEARCH_MATCH;
        }
        return null;
    }

    /**
     * @param candidate the tech
     *
     * @return {@code true} if the filter shows the tech
     */
    public boolean shows(SalvageTechCandidate candidate) {
        return getHiddenReason(candidate) == null;
    }

    /**
     * Counts how many techs each filter hides. Each hidden tech is counted once, under the first reason that hides
     * them.
     *
     * @param candidates the techs
     *
     * @return the number of techs hidden for each reason; reasons that hide nobody are left out
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Map<HiddenReason, Integer> countHidden(List<SalvageTechCandidate> candidates) {
        Map<HiddenReason, Integer> counts = new EnumMap<>(HiddenReason.class);
        for (SalvageTechCandidate candidate : candidates) {
            HiddenReason reason = getHiddenReason(candidate);
            if (reason != null) {
                counts.merge(reason, 1, Integer::sum);
            }
        }
        return counts;
    }
}

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
package mekhq.gui.dialog.markets.personnelMarket;

import static mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;

/**
 * One of the personnel market's role filters: every applicant, one of the broad groups (combat, support or civilian),
 * or a single profession. All go by an applicant's primary profession, the one their card leads with, so every
 * applicant belongs to exactly one group and the groups' counts add up.
 *
 * @param group      the group it lets through, or {@code null} if it is not a group's filter
 * @param profession the profession it lets through, or {@code null} if it is not a profession's filter
 *
 * @author Illiani
 * @since 0.51.01
 */
record ApplicantFilter(@Nullable PersonnelRoleSubType group, @Nullable PersonnelRole profession) {
    /** Lets every applicant through. */
    static final ApplicantFilter ALL = new ApplicantFilter(null, null);
    private static final String ALL_KEY = "ALL";

    static ApplicantFilter of(PersonnelRoleSubType group) {
        return new ApplicantFilter(group, null);
    }

    static ApplicantFilter of(PersonnelRole profession) {
        return new ApplicantFilter(null, profession);
    }

    /** @return the filters always on offer: every applicant, then each broad group */
    static List<ApplicantFilter> groups() {
        List<ApplicantFilter> filters = new ArrayList<>();
        filters.add(ALL);
        for (PersonnelRoleSubType group : PersonnelRoleSubType.values()) {
            filters.add(of(group));
        }
        return filters;
    }

    /**
     * The filters for single professions: one for each that at least one applicant has. Civilian professions have none
     * of their own, as there are too many to list; the civilian group covers them all.
     *
     * @param professions the applicants' primary professions, in any order and with any repeats
     * @param chosen      the filter in use, which stays on offer even if no applicant has its profession any more, so
     *                    the player can see what is narrowing the board
     *
     * @return the filters, in the professions' usual order
     */
    static List<ApplicantFilter> professions(Collection<PersonnelRole> professions, ApplicantFilter chosen) {
        TreeSet<PersonnelRole> offered = new TreeSet<>(professions);
        if (chosen.profession() != null) {
            offered.add(chosen.profession());
        }
        List<ApplicantFilter> filters = new ArrayList<>();
        for (PersonnelRole profession : offered) {
            if (!profession.isCivilian()) {
                filters.add(of(profession));
            }
        }
        return filters;
    }

    /**
     * @param primaryProfession an applicant's primary profession
     *
     * @return {@code true} if the filter lets the applicant through
     */
    boolean matches(PersonnelRole primaryProfession) {
        if (profession != null) {
            return primaryProfession == profession;
        }
        return group == null || primaryProfession.isSubType(group);
    }

    /** @return the filter's label on the rail */
    String label() {
        if (profession != null) {
            return profession.toString();
        }
        return group == null ? getTextAt(RESOURCE_BUNDLE, "rail.personnelMarket.all") : group.toString();
    }

    /** @return the text the filter is saved as; see {@link #fromKey} */
    String key() {
        if (profession != null) {
            return profession.name();
        }
        return group == null ? ALL_KEY : group.name();
    }

    /**
     * @param key the text a filter was saved as
     *
     * @return that filter; {@link #ALL} if there is no such filter any more, or if the save is from before the filters
     *       were saved by name
     */
    static ApplicantFilter fromKey(@Nullable String key) {
        for (ApplicantFilter filter : groups()) {
            if (filter.key().equals(key)) {
                return filter;
            }
        }
        for (PersonnelRole profession : PersonnelRole.values()) {
            if (profession.name().equals(key) && !profession.isCivilian()) {
                return of(profession);
            }
        }
        return ALL;
    }
}

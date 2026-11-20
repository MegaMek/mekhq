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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.function.Function;

import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.ApplicantFacts;

/**
 * The orders the personnel market's applicant board can be sorted in. Each has a natural direction, such as most
 * experienced first, and can be reversed. Every order falls back to the applicant's name, in alphabetical order
 * whichever way the order runs, so the board never shuffles between refreshes.
 *
 * @author Illiani
 * @since 0.51.01
 */
enum ApplicantSort {
    /** Most experienced first. */
    EXPERIENCE,
    /** Cheapest to hire first. */
    COST,
    /** Most special abilities first. */
    ABILITIES,
    /** Most flaws first. */
    FLAWS,
    /** Grouped by profession, the most experienced first within each. */
    ROLE,
    /** Alphabetical by full name. */
    NAME,
    /** Youngest first. */
    AGE;

    /** @return the sort's label on its chip */
    String label() {
        return getTextAt(RESOURCE_BUNDLE, "sort.personnelMarket." + name());
    }

    /** @return what the sort does, for its chip's tooltip */
    String toolTip() {
        return getTextAt(RESOURCE_BUNDLE, "sort.personnelMarket." + name() + ".tooltip");
    }

    /**
     * Builds the comparator for this order. The applicants' facts are looked up rather than worked out here, as they
     * are slow to compute and the board caches them.
     *
     * @param facts      each applicant's experience level, hiring cost and ability and flaw counts
     * @param today      the campaign's date, for ages
     * @param isReversed {@code true} to run the order the other way, such as least experienced first; ties still fall
     *                   back to alphabetical order
     *
     * @return the comparator
     *
     * @author Illiani
     * @since 0.51.01
     */
    Comparator<Person> comparator(Function<Person, ApplicantFacts> facts, LocalDate today, boolean isReversed) {
        Comparator<Person> byName = Comparator.comparing(Person::getFullName, String.CASE_INSENSITIVE_ORDER);
        Comparator<Person> mostExperienced = Comparator.comparingInt(
              (Person person) -> facts.apply(person).experienceLevel()).reversed();
        Comparator<Person> order = switch (this) {
            case EXPERIENCE -> mostExperienced;
            case COST -> Comparator.comparing((Person person) -> facts.apply(person).hiringCost());
            case ABILITIES -> Comparator.comparingInt((Person person) -> facts.apply(person).abilities()).reversed();
            case FLAWS -> Comparator.comparingInt((Person person) -> facts.apply(person).flaws()).reversed();
            case ROLE -> Comparator.comparing((Person person) -> person.getPrimaryRole().toString(),
                  String.CASE_INSENSITIVE_ORDER).thenComparing(mostExperienced);
            case NAME -> byName;
            case AGE -> Comparator.comparingInt((Person person) -> person.getAge(today));
        };
        return (isReversed ? order.reversed() : order).thenComparing(byName);
    }
}

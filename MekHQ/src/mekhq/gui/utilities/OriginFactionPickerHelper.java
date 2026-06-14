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
package mekhq.gui.utilities;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;

import megamek.common.universe.FactionTag;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Shared origin-faction picker logic for {@code CustomizePersonDialog} and
 * {@code CreateCharacterDialog}. Centralizes the strict lifespan filter and the
 * "Show All Factions" model variant so the two dialogs cannot drift.
 *
 * <p>See PR #8937 (issue #8929) for behavior context.</p>
 */
public final class OriginFactionPickerHelper {
    private OriginFactionPickerHelper() {}

    /**
     * {@code SPECIAL}-tagged meta-factions that remain valid as a deliberate, manually chosen
     * personal origin: Mercenary, Independent, and Pirate. These stay excluded from RANDOM origin
     * assignment (see {@code RangedFactionSelector}, the primary purpose of the {@code SPECIAL}
     * tag) but are legitimate choices in the manual picker. See issue #412.
     */
    private static final Set<String> ALLOWED_SPECIAL_ORIGINS = Set.of(
          Faction.MERCENARY_FACTION_CODE, "IND", Faction.PIRATE_FACTION_CODE);

    /**
     * @return {@code true} if {@code faction} is a meta-faction that should never appear in the
     *       origin picker regardless of era. This covers {@code HIDDEN} factions and
     *       {@code SPECIAL}-tagged meta-factions other than the handful that are valid personal
     *       origins ({@link #ALLOWED_SPECIAL_ORIGINS}). It excludes aggregates ({@code CLAN},
     *       {@code IS}, {@code Periphery}), dead/no-government codes ({@code ABN}, {@code UND},
     *       {@code NONE}), and administrative entities, while keeping Mercenary, Independent, and
     *       Pirate selectable.
     */
    private static boolean isNonOriginMetaFaction(Faction faction) {
        if (faction.is(FactionTag.HIDDEN)) {
            return true;
        }
        return faction.is(FactionTag.SPECIAL) && !ALLOWED_SPECIAL_ORIGINS.contains(faction.getShortName());
    }

    /**
     * @return {@code true} if {@code faction} would survive the strict lifespan filter on its
     *       own (independent of the "always include person's origin" exception). Used at dialog
     *       open to decide whether to auto-check "Show All Factions" so the dropdown actually
     *       reflects what's selected.
     *
     * @param faction     the faction to test, or {@code null} (returns {@code false})
     * @param person      the person whose lifespan window applies
     * @param currentYear the current campaign year (lifespan window upper bound)
     * @param endDate     the recruitment / joined-campaign date, or {@code null} if recruitment
     *                    is not tracked. The lifespan window upper bound is
     *                    {@code min(endDate.getYear(), currentYear)}.
     */
    public static boolean wouldStrictFilterAdmit(Faction faction, Person person, int currentYear,
          LocalDate endDate) {
        if (faction == null || isNonOriginMetaFaction(faction)) {
            return false;
        }
        int endYear = (endDate != null) ? Math.min(endDate.getYear(), currentYear) : currentYear;
        return faction.validBetween(person.getDateOfBirth().getYear(), endYear);
    }

    /**
     * Builds the origin-faction picker model.
     *
     * <p>Default ({@code showAllFactions == false}): only factions whose {@code yearsActive}
     * overlap the person's lifespan window {@code [birthYear .. min(endDate, currentYear)]}
     * appear, with non-origin meta-factions always excluded (see {@link #isNonOriginMetaFaction}:
     * {@code HIDDEN} and most {@code SPECIAL}-tagged factions, keeping Mercenary/Independent/Pirate).
     * This is the canonically correct behavior - a 3151-era character cannot be born in the long-
     * dissolved Federated Commonwealth.</p>
     *
     * <p>When {@code showAllFactions == true}: drops the lifespan filter entirely. Useful for
     * legitimate but unusual cases (long-lived characters whose origin is now-defunct, deliberate
     * "I want my Dark Age character to have a Word of Blake background" choices). Non-origin
     * meta-factions stay excluded ({@link #isNonOriginMetaFaction}): aggregates and dead/no-
     * government codes aren't legitimate origins regardless of era, but Mercenary, Independent,
     * and Pirate remain selectable.</p>
     *
     * <p>The person's existing origin faction is always included in both modes so editing a
     * character whose origin is filtered out by the lifespan check does not silently lose the
     * assignment.</p>
     *
     * @param person          the person whose origin is being picked
     * @param currentYear     the current campaign year (lifespan window upper bound)
     * @param endDate         the recruitment / joined-campaign date, or {@code null} if
     *                        recruitment is not tracked. The lifespan window upper bound is
     *                        {@code min(endDate.getYear(), currentYear)}.
     * @param showAllFactions whether to drop the lifespan filter
     */
    public static DefaultComboBoxModel<Faction> buildModel(Person person, int currentYear,
          LocalDate endDate, boolean showAllFactions) {
        List<Faction> orderedFactions = Factions.getInstance()
                                              .getFactions()
                                              .stream()
                                              .sorted((a, b) -> a.getFullName(currentYear)
                                                                      .compareToIgnoreCase(b.getFullName(currentYear)))
                                              .toList();

        DefaultComboBoxModel<Faction> factionsModel = new DefaultComboBoxModel<>();
        for (Faction faction : orderedFactions) {
            // Always include the person's origin faction
            if (faction.equals(person.getOriginFaction())) {
                factionsModel.addElement(faction);
                continue;
            }
            if (isNonOriginMetaFaction(faction)) {
                continue;
            }
            if (showAllFactions) {
                factionsModel.addElement(faction);
                continue;
            }

            int endYear = (endDate != null) ? Math.min(endDate.getYear(), currentYear) : currentYear;
            if (faction.validBetween(person.getDateOfBirth().getYear(), endYear)) {
                factionsModel.addElement(faction);
            }
        }

        return factionsModel;
    }
}

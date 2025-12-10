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
package mekhq.campaign.universe.factionHints;

import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.getFactionName;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.MHQConstants;
import mekhq.campaign.universe.Faction;

/**
 * Utility class for constructing and retrieving human-readable diplomacy event descriptions between factions based on
 * {@link FactionHint} data.
 *
 * <p>This class scans the campaign's faction relationship map and identifies any diplomacy changes that begin on a
 * given date according to {@link FactionHint#hintStartsToday(LocalDate)}. It ensures that each bilateral relationship
 * (e.g., Draconis Combine vs. Federated Suns) appears only once even if the faction map stores both directional
 * entries.</p>
 *
 * <p>The result is typically used for building UI labels, log entries, or report panels notifying the player of new
 * diplomatic developments.</p>
 *
 * @author Illiani
 * @since 0.50.11
 */
public final class DiplomacyLabel {
    /**
     * Scans all faction relationships and returns formatted diplomacy change descriptions for events that start on the
     * specified date.
     *
     * <p>The returned list is deduplicated so that bidirectional faction entries (A→B and B→A) only produce a single
     * event record. The formatting and labeling of events is delegated to the supplied {@link DiplomacyType}, which can
     * produce themed presentation strings appropriate for the diplomacy change.</p>
     *
     * @param today         the date being evaluated for diplomacy event start triggers; must not be {@code null}
     * @param relationships nested relationship structure mapping each faction to its known counterparts and associated
     *                      {@link FactionHint} lists
     * @param diplomacyType defines how the diplomacy changes should be converted to user-facing text (e.g., alliance,
     *                      war declaration); must not be {@code null}
     *
     * @return a list of formatted diplomacy event labels for all faction pairs whose hints begin on the given date;
     *       never {@code null}, ut may be empty
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static List<String> getDiplomacyEventsStartingOn(LocalDate today,
          Map<Faction, Map<Faction, List<FactionHint>>> relationships, DiplomacyType diplomacyType,
          boolean campaignFactionIsClanFaction) {
        boolean filterClanIntel = !MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS.isAfter(today);

        List<String> relevantRelationships = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (Map.Entry<Faction, Map<Faction, List<FactionHint>>> outerEntry : relationships.entrySet()) {
            Faction outerFaction = outerEntry.getKey();
            Map<Faction, List<FactionHint>> innerMap = outerEntry.getValue();

            for (Map.Entry<Faction, List<FactionHint>> innerEntry : innerMap.entrySet()) {
                Faction innerFaction = innerEntry.getKey();

                // Build an unordered key so (A:B) and (B:A) collapse to the same value
                String pairKey = buildUnorderedFactionKey(outerFaction, innerFaction);
                if (seenPairs.contains(pairKey)) {
                    continue;
                }

                if (filterClanIntel) {
                    if (outerFaction.isClan() != campaignFactionIsClanFaction) {
                        seenPairs.add(pairKey);
                        continue;
                    }
                    if (innerFaction.isClan() != campaignFactionIsClanFaction) {
                        seenPairs.add(pairKey);
                        continue;
                    }
                }

                for (FactionHint hint : innerEntry.getValue()) {
                    if (hint.hintStartsToday(today)) {
                        relevantRelationships.add(diplomacyType.getDisplayText(
                              getFactionName(innerFaction, today.getYear()),
                              getFactionName(outerFaction, today.getYear())));
                        seenPairs.add(pairKey); // Only one entry per faction pair
                        break;
                    }
                }
            }
        }

        return relevantRelationships;
    }

    /**
     * Produces an order-stable key representing a pair of factions without directionality, ensuring {@code A:B} and
     * {@code B:A} collapse to the same key.
     *
     * <p>The key is constructed lexicographically from the factions’ short names to guarantee consistent ordering
     * regardless of lookup order.</p>
     *
     * @param outerFaction the first faction; must not be {@code null}
     * @param innerFaction the second faction; must not be {@code null}
     *
     * @return a canonical, ordered string key used for deduplicating bidirectional relationships
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static String buildUnorderedFactionKey(Faction outerFaction, Faction innerFaction) {
        String a = outerFaction.getShortName();
        String b = innerFaction.getShortName();
        return (a.compareTo(b) <= 0) ? a + ":" + b : b + ":" + a;
    }
}

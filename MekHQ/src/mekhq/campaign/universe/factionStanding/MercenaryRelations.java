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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionStandings.DEFAULT_REGARD;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_ALLIED_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_ENEMY_FACTION_AT_WAR;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_ENEMY_FACTION_RIVAL;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_SAME_FACTION;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.StandingModifier.ABOVE_AVERAGE;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.StandingModifier.AVERAGE;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.StandingModifier.AWFUL;
import static mekhq.campaign.universe.factionStanding.MercenaryRelations.StandingModifier.BELOW_AVERAGE;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.Faction;

/**
 * Provides historical and contextual modifiers that represent how various factions perceive mercenary forces. The
 * modifiers are organized as a mapping from faction codes to chronologically ordered lists of {@link MercenaryRelation}
 * instances, each describing a standing modifier for a time period.
 *
 * <p>This class supplies query methods for retrieving the current relationship modifier based on a faction and year.
 * It also defines convenience constants for representing default or fallback values for the Inner Sphere and Clan
 * factions.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class MercenaryRelations {
    static final LocalDate NO_STARTING_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate NO_ENDING_DATE = LocalDate.MAX;
    static final double INNER_SPHERE_FALLBACK_VALUE = AVERAGE.getModifier();
    static final double CLAN_FALLBACK_VALUE = AWFUL.getModifier();

    /**
     * A mapping of faction codes to their respective lists of {@link MercenaryRelation} objects, describing how each
     * faction's relationship with mercenaries changes over various historical periods.
     *
     * <p>Each key is a faction code (e.g., "FS" for Federated Suns), and the value is a list detailing the standing
     * modifiers effective for specific date ranges, including explanatory context.</p>
     *
     * <p>If a faction code is not present in this map, lookups using {@link Map#get(Object)} will return
     * {@code null}.</p>
     *
     * <p><b>Usage:</b> Enter the faction code followed by a list of {@link MercenaryRelation} objects. These
     * objects should be ordered in chronological order oldest -> newest as the first valid modifier will be the
     * modifier returned. Ideally, an explanation of each modifier should be included so that future developers know the
     * logic behind the change.</p>
     *
     * @since 0.50.07
     */
    static final Map<String, List<MercenaryRelation>> CLIMATE_FACTION_STANDING_MODIFIERS = Map.ofEntries(
          // Federated Suns
          Map.entry("FS", List.of(
                // Professional, pragmatic approach via Department of Mercenary Relations (default standing)
                new MercenaryRelation(NO_STARTING_DATE, LocalDate.of(3062, 11, 15), ABOVE_AVERAGE),
                // FedCom Civil War Period: Internal conflict due to Civil War causing disruption
                new MercenaryRelation(LocalDate.of(3062, 11, 16), LocalDate.of(3067, 4, 20), BELOW_AVERAGE),
                // Slow regaining of stability following the Civil War
                new MercenaryRelation(LocalDate.of(3067, 4, 21), LocalDate.of(3069, 12, 31), AVERAGE),
                // Professional, pragmatic approach via Department of Mercenary Relations (default standing)
                new MercenaryRelation(LocalDate.of(3070, 1, 1), NO_ENDING_DATE, ABOVE_AVERAGE)
          )),
          // Draconis Combine
          Map.entry("DC", List.of(
                // Distrustful due to bushido culture viewing mercenaries as dishonorable (default standing)
                new MercenaryRelation(NO_STARTING_DATE, LocalDate.of(3029, 9, 3), BELOW_AVERAGE),
                // Death to Mercenaries edict
                new MercenaryRelation(LocalDate.of(3029, 9, 4), LocalDate.of(3039, 4, 15), AWFUL),
                // Death to Mercenaries starts to be relaxed, but not wholly removed
                new MercenaryRelation(LocalDate.of(3039, 4, 16), LocalDate.of(3054, 9, 14), BELOW_AVERAGE),
                // Theodore Kurita's reforms (new default)
                new MercenaryRelation(LocalDate.of(3054, 9, 15), NO_ENDING_DATE, AVERAGE)
          )),
          // Capellan Confederation
          Map.entry("CC", List.of(
                // Pragmatic but cautious (default standing)
                new MercenaryRelation(NO_STARTING_DATE, LocalDate.of(3036, 5, 11), BELOW_AVERAGE),
                // Romano Liao's Paranoid Purges: Extended suspicion and harsh treatment to mercenary contractors
                new MercenaryRelation(LocalDate.of(3036, 5, 12), LocalDate.of(3052, 5, 8), AWFUL),
                // Pragmatic but cautious (default standing)
                new MercenaryRelation(LocalDate.of(3052, 5, 9), NO_ENDING_DATE, BELOW_AVERAGE)
          )),
          // Lyran Commonwealth
          Map.entry("LA", List.of(
                // Bureaucratic but welcoming major employer (default standing)
                new MercenaryRelation(NO_STARTING_DATE, LocalDate.of(3062, 11, 15), ABOVE_AVERAGE),
                // FedCom Civil War Period: Internal conflict due to Civil War causing disruption
                new MercenaryRelation(LocalDate.of(3062, 11, 16), LocalDate.of(3067, 4, 20), BELOW_AVERAGE),
                // Slow regaining of stability following the Civil War
                new MercenaryRelation(LocalDate.of(3067, 4, 21), LocalDate.of(3069, 12, 31), AVERAGE),
                // Bureaucratic but welcoming major employer (default standing)
                new MercenaryRelation(LocalDate.of(3070, 1, 1), NO_ENDING_DATE, ABOVE_AVERAGE)
          )),
          // Free Worlds League (included so future developers don't think it was forgotten)
          Map.entry("FWL", List.of(
                // Utilitarian, often for internal balance (default standing)
                new MercenaryRelation(NO_STARTING_DATE, NO_ENDING_DATE, AVERAGE)
          )),
          // Free Rasalhague Republic
          Map.entry("FRR", List.of(
                // Ronin War mercenary betrayals created cultural bias against mercenaries
                new MercenaryRelation(LocalDate.of(3034, 5, 23), LocalDate.of(3035, 5, 23), AWFUL),
                // Lingering fallout from the betrayals (new default)
                new MercenaryRelation(LocalDate.of(3035, 5, 24), NO_ENDING_DATE, BELOW_AVERAGE)
          )),
          // Clan Diamond Shark
          Map.entry("CDS", List.of(
                // As the Merchant Clan they become more pragmatic about business relationships with mercenaries once
                // isolated from the Homeworlds
                new MercenaryRelation(LocalDate.of(3075, 12, 1), NO_ENDING_DATE, BELOW_AVERAGE)
          )),
          // Taurian Concordat
          Map.entry("TC", List.of(
                // Generally suspicious of mercenaries due to anti-Inner Sphere paranoia (default standing)
                new MercenaryRelation(NO_STARTING_DATE, LocalDate.of(3058, 8, 1), BELOW_AVERAGE),
                // Trinity Alliance Period: Cooperation with CC and MoC led to more professional mercenary relations
                new MercenaryRelation(LocalDate.of(3058, 8, 2), LocalDate.of(3067, 12, 31), AVERAGE),
                // Generally suspicious of mercenaries due to anti-Inner Sphere paranoia (default standing)
                new MercenaryRelation(LocalDate.of(3068, 1, 1), NO_ENDING_DATE, BELOW_AVERAGE)
          )),
          // Magistracy of Canopus
          Map.entry("MOC", List.of(
                // Pragmatic and welcoming due to open society values
                new MercenaryRelation(NO_STARTING_DATE, NO_ENDING_DATE, ABOVE_AVERAGE)
          )),
          // Outworlds Alliance
          Map.entry("OA", List.of(
                // Practical necessity due to weak military
                new MercenaryRelation(NO_STARTING_DATE, NO_ENDING_DATE, ABOVE_AVERAGE)
          ))
    );

    /**
     * Enumerates the possible standing modifiers for a faction's attitude toward mercenaries, with internal numeric
     * values representing their severity.
     *
     * @author Illiani
     * @since 0.50.07
     */
    enum StandingModifier {
        AWFUL(STARTING_REGARD_ENEMY_FACTION_AT_WAR),
        BELOW_AVERAGE(STARTING_REGARD_ENEMY_FACTION_RIVAL),
        AVERAGE(DEFAULT_REGARD),
        ABOVE_AVERAGE(STARTING_REGARD_ALLIED_FACTION),
        AMAZING(STARTING_REGARD_SAME_FACTION);

        private final double modifier;

        /**
         * Constructs a standing modifier with its associated numeric value.
         *
         * @param modifier the floating-point value representing the modifier.
         *
         * @author Illiani
         * @since 0.50.07
         */
        StandingModifier(double modifier) {
            this.modifier = modifier;
        }

        /**
         * Gets the numeric value assigned to this standing modifier.
         *
         * @return the standing modifier as a floating-point value.
         *
         * @author Illiani
         * @since 0.50.07
         */
        public double getModifier() {
            return modifier;
        }
    }

    /**
     * Represents the relationship between a faction and mercenaries during a specific historical period.
     *
     * @param startingDate     The first date this relationship applies (inclusive), or 01/01/2000 for no defined
     *                         start.
     * @param endingDate       The last date this relationship applies (inclusive), or {@link LocalDate#MAX} for no
     *                         defined end.
     * @param standingModifier The {@link StandingModifier} indicating the faction's attitude in this period.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public record MercenaryRelation(LocalDate startingDate, LocalDate endingDate, StandingModifier standingModifier) {
    }

    /**
     * Returns the standing modifier value for the specified faction and year, representing how strongly the faction
     * favors or disfavors mercenaries.
     *
     * <p>If no specific modifier is found for the faction and year, a fallback value is returned depending on
     * whether the faction is considered Inner Sphere or Clan.</p>
     *
     * @param faction the faction whose relationship to mercenaries is to be retrieved, or {@code null} to use fallback
     * @param today   the date to check for the relevant relationship
     *
     * @return the numeric standing modifier for how the faction treats mercenaries
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getMercenaryRelationsModifier(@Nullable Faction faction, LocalDate today) {
        if (faction == null) {
            return INNER_SPHERE_FALLBACK_VALUE;
        }

        double defaultModifier = faction.isClan() ? CLAN_FALLBACK_VALUE : INNER_SPHERE_FALLBACK_VALUE;

        String factionCode = faction.getShortName();
        List<MercenaryRelation> mercenaryRelations = CLIMATE_FACTION_STANDING_MODIFIERS.get(factionCode);
        if (mercenaryRelations == null) {
            return defaultModifier;
        }

        for (MercenaryRelation relation : mercenaryRelations) {
            if (isDateEqualOrBefore(relation.startingDate, today)
                      &&
                      (relation.endingDate.equals(NO_ENDING_DATE) || isDateEqualOrAfter(relation.endingDate, today))) {
                return relation.standingModifier.getModifier();
            }
        }

        return defaultModifier;
    }

    /**
     * Determines whether the first date is equal to or comes before the second date.
     *
     * @param firstDate  the first date to compare
     * @param secondDate the second date to compare against
     *
     * @return {@link true} if the first date is equal to or comes before the second date; {@link false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static boolean isDateEqualOrBefore(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.isBefore(secondDate) || firstDate.isEqual(secondDate);
    }

    /**
     * Determines whether the first date is equal to or comes after the second date.
     *
     * @param firstDate  the first date to compare
     * @param secondDate the second date to compare against
     *
     * @return {@link true} if the first date is equal to or comes after the second date; {@link false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static boolean isDateEqualOrAfter(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.isAfter(secondDate) || firstDate.isEqual(secondDate);
    }
}

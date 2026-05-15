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
package mekhq.campaign.universe.companyGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.ranks.Rank;
import org.junit.jupiter.api.Test;

/**
 * Pins the {@link FormationLevel} → rank-index mapping against {@code data/universe/ranks.xml}
 * slot conventions and {@code MekHQ/battletech_rank_command_mapping.xlsx}. If a future change
 * shifts an index, these tests fail with a clear "what changed" message rather than the user
 * silently seeing wrong ranks on generated commanders.
 *
 * <p>The mapping is family-aware: Inner Sphere / Clan / ComStar each occupy different slots for
 * the same canonical command size, so the test groups are organized by family.</p>
 */
class RulesetRankAssignerTest {

    // ===== Inner Sphere / Periphery =====

    @Test
    void rankIndex_lance_isLieutenantSlot() {
        // O3 — Lieutenant equivalent across IS rank XMLs (AFFS Leftenant, LCAF Leutnant, etc.)
        assertEquals(Rank.RWO_MAX + 3, RulesetRankAssigner.rankIndexForLevel(FormationLevel.LANCE));
    }

    @Test
    void rankIndex_company_isCaptainSlot() {
        assertEquals(Rank.RWO_MAX + 4, RulesetRankAssigner.rankIndexForLevel(FormationLevel.COMPANY));
    }

    @Test
    void rankIndex_battalion_isMajorSlot() {
        assertEquals(Rank.RWO_MAX + 5, RulesetRankAssigner.rankIndexForLevel(FormationLevel.BATTALION));
    }

    @Test
    void rankIndex_regiment_isColonelSlot() {
        // Pre-fix this returned RWO_MAX + 7 which is empty in every IS rank XML; the
        // setRankWithFallback walked DOWN to Major, leaving Regiment commanders with the wrong
        // rank. The correct slot is O8 (Colonel).
        assertEquals(Rank.RWO_MAX + 8, RulesetRankAssigner.rankIndexForLevel(FormationLevel.REGIMENT));
    }

    @Test
    void rankIndex_brigade_isBrigadierSlot() {
        assertEquals(Rank.RWO_MAX + 9, RulesetRankAssigner.rankIndexForLevel(FormationLevel.BRIGADE));
    }

    @Test
    void rankIndex_division_isMajorGeneralSlot() {
        assertEquals(Rank.RWO_MAX + 10, RulesetRankAssigner.rankIndexForLevel(FormationLevel.DIVISION));
    }

    @Test
    void rankIndex_corps_isGeneralSlot() {
        assertEquals(Rank.RWO_MAX + 11, RulesetRankAssigner.rankIndexForLevel(FormationLevel.CORPS));
    }

    @Test
    void rankIndex_army_isMarshalSlot() {
        assertEquals(Rank.RWO_MAX + 12, RulesetRankAssigner.rankIndexForLevel(FormationLevel.ARMY));
    }

    @Test
    void rankIndex_armyGroup_isFieldMarshalSlot() {
        assertEquals(Rank.RWO_MAX + 13, RulesetRankAssigner.rankIndexForLevel(FormationLevel.ARMY_GROUP));
    }

    // ===== Clan =====

    @Test
    void rankIndex_starOrNova_isNovaCommanderSlot() {
        // Star Commander is at O2 in the CLAN rank XML, Nova Commander at O3. FormationLevel
        // collapses Stars and Novas; we use Nova Commander as the default because it covers the
        // more elaborate "Nova" case and degrades cleanly to Star Commander semantics for pure
        // Stars (the Nova-Commander rank in the XML covers both).
        assertEquals(Rank.RWO_MAX + 3, RulesetRankAssigner.rankIndexForLevel(FormationLevel.STAR_OR_NOVA));
    }

    @Test
    void rankIndex_binaryOrTrinary_isStarCaptainSlot() {
        assertEquals(Rank.RWO_MAX + 4, RulesetRankAssigner.rankIndexForLevel(FormationLevel.BINARY_OR_TRINARY));
    }

    @Test
    void rankIndex_cluster_isStarColonelSlot() {
        // Pre-fix this returned RWO_MAX + 5 which is Nova Captain — wrong tier for a Cluster
        // (Cluster commander is Star Colonel at O8).
        assertEquals(Rank.RWO_MAX + 8, RulesetRankAssigner.rankIndexForLevel(FormationLevel.CLUSTER));
    }

    @Test
    void rankIndex_galaxy_isGalaxyCommanderSlot() {
        // Pre-fix this returned RWO_MAX + 7 which is empty in the CLAN rank XML.
        assertEquals(Rank.RWO_MAX + 9, RulesetRankAssigner.rankIndexForLevel(FormationLevel.GALAXY));
    }

    @Test
    void rankIndex_touman_isKhanSlot() {
        // Pre-fix this returned RWO_MAX + 8 (Star Colonel) — wrong tier for a Touman.
        assertEquals(Rank.RWO_MAX + 18, RulesetRankAssigner.rankIndexForLevel(FormationLevel.TOUMAN));
    }

    // ===== ComStar / Word of Blake =====

    @Test
    void rankIndex_levelII_isAdeptSlot() {
        assertEquals(Rank.RWO_MAX + 3, RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_II_OR_CHOIR));
    }

    @Test
    void rankIndex_levelIII_isDemiPrecentorSlot() {
        assertEquals(Rank.RWO_MAX + 4, RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_III));
    }

    @Test
    void rankIndex_levelIV_isPrecentorSlot() {
        // Pre-fix this returned RWO_MAX + 5 which is empty in the COMSTAR rank XML.
        assertEquals(Rank.RWO_MAX + 7, RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_IV));
    }

    @Test
    void rankIndex_levelV_isPrecentorSlot_sameAsLevelIV() {
        // Canon has no intermediate rank between Precentor (O7) and Precentor Martial (O12) —
        // LEVEL_V shares Precentor with LEVEL_IV. This is deliberate.
        assertEquals(Rank.RWO_MAX + 7, RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_V));
        assertEquals(RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_IV),
              RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_V));
    }

    @Test
    void rankIndex_levelVI_isPrecentorMartialSlot() {
        assertEquals(Rank.RWO_MAX + 12, RulesetRankAssigner.rankIndexForLevel(FormationLevel.LEVEL_VI));
    }

    // ===== Defensive =====

    @Test
    void rankIndex_null_returnsNegativeOne() {
        assertEquals(-1, RulesetRankAssigner.rankIndexForLevel(null));
    }
}

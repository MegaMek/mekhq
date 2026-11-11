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
package mekhq.campaign.finances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.UnitCostModel;
import mekhq.campaign.parts.Part;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link AcquisitionPriceMultipliers}: the Campaign Operations (CO p.14) acquisition price multipliers, set to
 * their Campaign Operations values unless a test says otherwise.
 */
class AcquisitionPriceMultipliersTest {
    private static final double DELTA = 1e-9;

    private CampaignOptions campaignOptions;
    private Faction innerSphere;
    private Faction clan;
    private Faction comStar;
    private Faction wordOfBlake;

    @BeforeEach
    void setUp() {
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.CLAN_TECH_INVASION_PRICE_MULTIPLIER, 5.0);
        campaignOptions.set(CampaignOption.CLAN_TECH_POST_INVASION_PRICE_MULTIPLIER, 4.0);
        campaignOptions.set(CampaignOption.SUCCESSION_WARS_ADVANCED_TECH_PRICE_MULTIPLIER, 5.0);
        campaignOptions.set(CampaignOption.TECH_RATING_E_PRICE_MULTIPLIER, 1.1);
        campaignOptions.set(CampaignOption.TECH_RATING_F_PRICE_MULTIPLIER, 1.25);

        innerSphere = mock(Faction.class);
        clan = mock(Faction.class);
        when(clan.isClan()).thenReturn(true);
        comStar = mock(Faction.class);
        when(comStar.isComStar()).thenReturn(true);
        when(comStar.isComStarOrWoB()).thenReturn(true);
        wordOfBlake = mock(Faction.class);
        when(wordOfBlake.isWoB()).thenReturn(true);
        when(wordOfBlake.isComStarOrWoB()).thenReturn(true);
    }

    private double multiplier(Faction faction, int year, boolean isClanTech, TechRating techRating) {
        return AcquisitionPriceMultipliers.getMultiplier(campaignOptions, faction, year, isClanTech, techRating);
    }

    /** Only the Total Warfare unit cost model uses the acquisition multipliers. */
    @ParameterizedTest
    @EnumSource(value = UnitCostModel.class, names = "TOTAL_WARFARE", mode = EnumSource.Mode.EXCLUDE)
    void otherUnitCostModelsIgnoreTheMultipliers(UnitCostModel unitCostModel) {
        campaignOptions.set(CampaignOption.UNIT_COST_MODEL, unitCostModel);

        assertEquals(1.0, AcquisitionPriceMultipliers.getMultiplier(campaignOptions, innerSphere, 3060, true,
              TechRating.F), DELTA);
    }

    /** Every multiplier defaults to ×1, so existing campaigns are priced as before. */
    @Test
    void defaultsLeavePricesUnchanged() {
        assertEquals(1.0, AcquisitionPriceMultipliers.getMultiplier(new CampaignOptions(), innerSphere, 3060, true,
              TechRating.F), DELTA);
    }

    @Nested
    class ClanTech {
        @ParameterizedTest
        @CsvSource({ "3048, 1.0", "3049, 5.0", "3070, 5.0", "3071, 4.0", "3150, 4.0" })
        void isPricedByYearForNonClanCampaigns(int year, double expected) {
            assertEquals(expected, multiplier(innerSphere, year, true, null), DELTA);
        }

        @Test
        void isNotSurchargedForClanCampaigns() {
            assertEquals(1.0, multiplier(clan, 3060, true, null), DELTA);
        }

        /** Without a faction, the campaign is treated as neither Clan nor ComStar. */
        @Test
        void isSurchargedWithoutAFaction() {
            assertEquals(5.0, multiplier(null, 3060, true, null), DELTA);
        }

        @Test
        void innerSphereTechIsNotSurcharged() {
            assertEquals(1.0, multiplier(innerSphere, 3060, false, null), DELTA);
        }
    }

    @Nested
    class TechRatings {
        @ParameterizedTest
        @CsvSource({ "2859, E, 1.1", "2860, E, 5.0", "3040, E, 5.0", "3041, E, 1.1",
                     "2859, F, 1.25", "2860, F, 5.0", "3040, F, 5.0", "3041, F, 1.25" })
        void areSurchargedDuringTheSuccessionWars(int year, TechRating techRating, double expected) {
            assertEquals(expected, multiplier(innerSphere, year, false, techRating), DELTA);
        }

        @ParameterizedTest
        @CsvSource({ "E, 1.1", "F, 1.25" })
        void comStarPaysTheUsualMultiplierDuringTheSuccessionWars(TechRating techRating, double expected) {
            assertEquals(expected, multiplier(comStar, 2950, false, techRating), DELTA);
        }

        /** The Word of Blake is treated as ComStar. */
        @ParameterizedTest
        @CsvSource({ "E, 1.1", "F, 1.25" })
        void wordOfBlakePaysTheUsualMultiplierDuringTheSuccessionWars(TechRating techRating, double expected) {
            assertEquals(expected, multiplier(wordOfBlake, 2950, false, techRating), DELTA);
        }

        @ParameterizedTest
        @CsvSource({ "E, 1.1", "F, 1.25" })
        void clansPayTheUsualMultiplierDuringTheSuccessionWars(TechRating techRating, double expected) {
            assertEquals(expected, multiplier(clan, 2950, false, techRating), DELTA);
        }

        @ParameterizedTest
        @CsvSource({ "A", "B", "C", "D" })
        void lowerRatingsAreUnaffected(TechRating techRating) {
            assertEquals(1.0, multiplier(innerSphere, 2950, false, techRating), DELTA);
        }

        @Test
        void anUnknownRatingIsUnaffected() {
            assertEquals(1.0, multiplier(innerSphere, 2950, false, null), DELTA);
        }
    }

    @Nested
    class Stacking {
        /** ×4 for Clan tech after 3070 and ×1.25 for tech rating F: 1 + 3 + 0.25. */
        @Test
        void clanTechAndTechRatingStackAdditively() {
            assertEquals(4.25, multiplier(innerSphere, 3080, true, TechRating.F), DELTA);
        }

        /** ×0.5 for Clan tech and ×0.5 for tech rating E: 0.5 × 0.5, where adding them would make it free. */
        @Test
        void discountsStackMultiplicatively() {
            campaignOptions.set(CampaignOption.CLAN_TECH_INVASION_PRICE_MULTIPLIER, 0.5);
            campaignOptions.set(CampaignOption.TECH_RATING_E_PRICE_MULTIPLIER, 0.5);

            assertEquals(0.25, multiplier(innerSphere, 3060, true, TechRating.E), DELTA);
        }

        /** Adding two ×0.1 discounts would go negative; multiplying them gives ×0.01. */
        @Test
        void discountsCanNotMakeAnItemFree() {
            campaignOptions.set(CampaignOption.CLAN_TECH_INVASION_PRICE_MULTIPLIER, 0.1);
            campaignOptions.set(CampaignOption.TECH_RATING_F_PRICE_MULTIPLIER, 0.1);

            assertEquals(0.01, multiplier(innerSphere, 3060, true, TechRating.F), DELTA);
        }

        /** ×5 for Clan tech before 3071, then ×0.5 for tech rating E: (1 + 4) × 0.5. */
        @Test
        void discountsApplyAfterTheSurcharges() {
            campaignOptions.set(CampaignOption.TECH_RATING_E_PRICE_MULTIPLIER, 0.5);

            assertEquals(2.5, multiplier(innerSphere, 3060, true, TechRating.E), DELTA);
        }
    }

    /** The unit and part entry points read the right details from the unit and part. */
    @Nested
    class EntryPoints {
        private static final int GAME_YEAR = 3060;

        /** ×5 for Clan tech before 3071 and ×1.1 for tech rating E: 1 + 4 + 0.1. */
        @Test
        void entityMultiplierCoversItsTechBaseAndTechRating() {
            Entity entity = mock(Entity.class);
            when(entity.isClan()).thenReturn(true);
            when(entity.getTechRating()).thenReturn(TechRating.E);

            assertEquals(5.1, AcquisitionPriceMultipliers.getUnitMultiplier(campaignOptions, innerSphere, GAME_YEAR,
                  entity), DELTA);
        }

        /** A mixed-tech unit on a Clan chassis only pays its mixed-tech unit price multiplier. */
        @Test
        void clanChassisMixedTechEntitiesDoNotPayTheClanTechMultiplier() {
            Entity entity = mock(Entity.class);
            when(entity.isClan()).thenReturn(true);
            when(entity.isMixedTech()).thenReturn(true);

            assertEquals(1.0, AcquisitionPriceMultipliers.getUnitMultiplier(campaignOptions, innerSphere, GAME_YEAR,
                  entity), DELTA);
        }

        /** An Inner Sphere unit carrying some Clan equipment is not priced as a Clan unit. */
        @Test
        void innerSphereChassisMixedTechEntitiesDoNotPayTheClanTechMultiplier() {
            Entity entity = mock(Entity.class);
            when(entity.isMixedTech()).thenReturn(true);

            assertEquals(1.0, AcquisitionPriceMultipliers.getUnitMultiplier(campaignOptions, innerSphere, GAME_YEAR,
                  entity), DELTA);
        }

        @Test
        void innerSphereChassisMixedTechSummariesDoNotPayTheClanTechMultiplier() {
            MekSummary summary = mock(MekSummary.class);
            when(summary.isMixedTech()).thenReturn(true);

            assertEquals(1.0, AcquisitionPriceMultipliers.getUnitMultiplier(campaignOptions, innerSphere, GAME_YEAR,
                  summary), DELTA);
        }

        /** Tech rating F during the Succession Wars. */
        @Test
        void summaryMultiplierMatchesTheEntity() {
            MekSummary summary = mock(MekSummary.class);
            when(summary.getTechRating()).thenReturn(TechRating.F);

            assertEquals(5.0, AcquisitionPriceMultipliers.getUnitMultiplier(campaignOptions, innerSphere, 3000,
                  summary), DELTA);
        }

        /** Parts only pay the multipliers when the unofficial option is on. */
        @Test
        void partsAreNotSurchargedByDefault() {
            Part part = mock(Part.class);
            when(part.getTechBase()).thenReturn(TechBase.CLAN);
            when(part.getTechRating()).thenReturn(TechRating.F);

            assertEquals(1.0, AcquisitionPriceMultipliers.getPartMultiplier(campaignOptions, innerSphere, GAME_YEAR,
                  part), DELTA);
        }

        /** ×5 for Clan tech before 3071 and ×1.25 for tech rating F: 1 + 4 + 0.25. */
        @Test
        void clanPartsPayTheClanTechAndTechRatingMultipliersWhenEnabled() {
            campaignOptions.set(CampaignOption.APPLY_ACQUISITION_MULTIPLIERS_TO_PARTS, true);
            Part part = mock(Part.class);
            when(part.getTechBase()).thenReturn(TechBase.CLAN);
            when(part.getTechRating()).thenReturn(TechRating.F);

            assertEquals(5.25, AcquisitionPriceMultipliers.getPartMultiplier(campaignOptions, innerSphere, GAME_YEAR,
                  part), DELTA);
        }
    }
}

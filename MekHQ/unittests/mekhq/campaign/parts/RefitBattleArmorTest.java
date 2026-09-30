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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testUtilities.parts.RefitKitPricing.expectedKitPrice;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import megamek.common.battleArmor.BattleArmor;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.missing.MissingBattleArmorSuit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A battle armor refit alters each trooper's suit rather than replacing it: suits keep their quality, a destroyed suit
 * stays destroyed, and battle armor is bought as battle armor (issue #10202).
 */
class RefitBattleArmorTest {
    private PartsScenario scenario;
    private Unit elementals;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        elementals = scenario.withUnit(UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER);
    }

    @Test
    void aRefitKeepsEachSuitsQuality() throws Exception {
        suitOf(1).setQuality(PartQuality.QUALITY_A);
        Refit refit = new Refit(elementals, UnitFixture.ELEMENTAL_BATTLE_ARMOR_FLAMER.loadEntity(), false, false,
              false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        assertEquals("[Flamer](Sqd5)", elementals.getEntity().getModel());
        assertEquals(PartQuality.QUALITY_A, suitOf(1).getQuality(), "Trooper 1 keeps its quality A suit");
        assertEquals(PartQuality.QUALITY_D, suitOf(2).getQuality());
        assertEquals(oneSuitPerTrooper(5), suitsPerTrooper());
    }

    @Test
    void aRefurbishmentKeepsADestroyedSuitDestroyed() {
        suitOf(2).remove(false);
        Refit refurbishment = new Refit(elementals, elementals.getEntity(), false, true, false);

        refurbishment.succeed();

        boolean isTrooperTwoStillDestroyed = false;
        for (Part part : elementals.getParts()) {
            if ((part instanceof MissingBattleArmorSuit) && (part.getLocation() == 2)) {
                isTrooperTwoStillDestroyed = true;
            }
        }
        assertTrue(isTrooperTwoStillDestroyed, "The destroyed suit does not come back");
        assertEquals(oneSuitPerTrooper(5), suitsPerTrooper(), "No trooper ends up with two suits");
    }

    @Test
    void aDestroyedSuitStaysDestroyedInThePointThatPlaysTheGame() {
        suitOf(2).remove(false);
        // A design loaded fresh from its file has all five troopers alive; the refit must carry the loss over to it. A
        // refit that adds anything to the destroyed trooper is refused, so the design here is the same one.
        BattleArmor freshDesign = (BattleArmor) UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER.loadEntity();
        assertEquals(5, freshDesign.getNumberActiveTroopers());
        Refit refit = new Refit(elementals, freshDesign, true, false, false);
        assertNull(refit.checkFixable(), "A refit that leaves the destroyed trooper alone may go ahead");

        refit.succeed();

        assertSame(freshDesign, elementals.getEntity());
        assertEquals(4, freshDesign.getNumberActiveTroopers(), "The point plays the game one trooper short");
        assertEquals(oneSuitPerTrooper(5), suitsPerTrooper(), "Each trooper slot still has exactly one suit");
    }

    @Test
    void aLargerSquadIsRefusedRatherThanGivenFreeSuits() {
        Refit refit = new Refit(elementals, UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER_SQUAD_OF_SIX.loadEntity(), false,
              false, false);

        assertNotNull(refit.checkFixable(), "Growing the squad is refused with a reason");
    }

    @Test
    void battleArmorIsBoughtAndPricedAsBattleArmor() {
        BattleArmor moreArmor = (BattleArmor) UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER.loadEntity();
        for (int trooper = BattleArmor.LOC_TROOPER_1; trooper < moreArmor.locations(); trooper++) {
            moreArmor.initializeArmor(moreArmor.getOArmor(trooper) + 2, trooper);
        }

        Refit refit = new Refit(elementals, moreArmor, false, false, false);

        assertInstanceOf(BAArmor.class, refit.getNewArmorSupplies());
        assertEquals(expectedKitPrice(refit), refit.getCost().round(), "The kit prices its armor by the point");
    }

    private BattleArmorSuit suitOf(int trooper) {
        for (Part part : new ArrayList<>(elementals.getParts())) {
            if ((part instanceof BattleArmorSuit suit) && (suit.getLocation() == trooper)) {
                return suit;
            }
        }
        throw new IllegalStateException("No suit for trooper " + trooper);
    }

    /** Counts each trooper's suits, intact or destroyed. */
    private SortedMap<Integer, Integer> suitsPerTrooper() {
        SortedMap<Integer, Integer> counts = new TreeMap<>();
        for (Part part : elementals.getParts()) {
            if ((part instanceof BattleArmorSuit) || (part instanceof MissingBattleArmorSuit)) {
                counts.merge(part.getLocation(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static SortedMap<Integer, Integer> oneSuitPerTrooper(int troopers) {
        SortedMap<Integer, Integer> counts = new TreeMap<>();
        for (int trooper = BattleArmor.LOC_TROOPER_1; trooper <= troopers; trooper++) {
            counts.put(trooper, 1);
        }
        return counts;
    }
}

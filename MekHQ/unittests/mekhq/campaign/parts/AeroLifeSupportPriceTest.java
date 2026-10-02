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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Entity;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingAeroLifeSupport;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A fighter's life support costs a flat 50,000 C-bills, as MegaMek prices it, while a spacecraft's costs 5,000 for
 * each crew member and passenger (issue #10264).
 */
class AeroLifeSupportPriceTest {
    private static AeroLifeSupport lifeSupportOf(UnitFixture fixture) {
        Unit unit = PartsScenario.create().withUnit(fixture);
        return PartsScenario.unitParts(unit, AeroLifeSupport.class).getFirst();
    }

    @Test
    void aFightersLifeSupportCostsAFlatFiftyThousand() {
        AeroLifeSupport lifeSupport = lifeSupportOf(UnitFixture.BATU_PRIME);
        assertTrue(lifeSupport.isForFighter());

        lifeSupport.calculateCost();

        assertEquals(Money.of(50000), lifeSupport.getStickerPrice());
    }

    @Test
    void aDropShipsLifeSupportCostsFiveThousandPerPersonAboard() {
        AeroLifeSupport lifeSupport = lifeSupportOf(UnitFixture.LEOPARD_DROPSHIP);
        assertFalse(lifeSupport.isForFighter());
        Entity leopard = lifeSupport.getUnit().getEntity();

        lifeSupport.calculateCost();

        assertEquals(Money.of(5000.0 * (leopard.getNCrew() + leopard.getNPassenger())),
              lifeSupport.getStickerPrice());
    }

    @Test
    void aFightersLifeSupportSavedAtTheOldPriceIsPricedRight() {
        AeroLifeSupport savedAtTheOldPrice = new AeroLifeSupport(50, Money.of(5000), true,
              lifeSupportOf(UnitFixture.BATU_PRIME).getCampaign());

        assertEquals(Money.of(50000), savedAtTheOldPrice.getStickerPrice());
    }

    @Test
    void aFightersMissingLifeSupportSavedAtTheOldPriceTakesANewOne() {
        AeroLifeSupport newLifeSupport = lifeSupportOf(UnitFixture.BATU_PRIME);
        newLifeSupport.calculateCost();
        MissingAeroLifeSupport missingAtTheOldPrice = new MissingAeroLifeSupport(50, Money.of(5000), true,
              newLifeSupport.getCampaign());

        assertTrue(missingAtTheOldPrice.isAcceptableReplacement(newLifeSupport, false));
    }

    @Test
    void aDropShipsMissingLifeSupportStillNeedsOneSizedForItsCrew() {
        AeroLifeSupport leopardLifeSupport = lifeSupportOf(UnitFixture.LEOPARD_DROPSHIP);
        leopardLifeSupport.calculateCost();
        MissingAeroLifeSupport missingForALargerCrew = new MissingAeroLifeSupport(1900,
              leopardLifeSupport.getStickerPrice().plus(5000), false, leopardLifeSupport.getCampaign());

        assertFalse(missingForALargerCrew.isAcceptableReplacement(leopardLifeSupport, false));
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refurbishment is paid for once, when it starts, and does not start at all when the force cannot pay (issue
 * #10195).
 *
 * <p>The Wolverine WVR-6R used here has fired some of its SRM 6 ammo. Before the fix, refurbishing a unit with ammo
 * bins ordered a refit kit on top of the refurbishment price, so the work stalled on procurement and was paid for
 * twice.</p>
 */
class RefitRefurbishmentTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private Unit wolverine;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_PARTS, true);
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        for (AmmoBin ammoBin : PartsScenario.unitParts(wolverine, AmmoBin.class)) {
            ammoBin.setShotsNeeded(5);
            ammoBin.updateConditionFromPart();
        }
    }

    private Refit newRefurbishment() {
        return new Refit(wolverine, wolverine.getEntity(), false, true, false);
    }

    private Money balance() {
        return campaign.getPlayerForce().getFinances().getBalance();
    }

    private void addFunds(Money amount) {
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.STARTING_CAPITAL, campaign.getLocalDate(), amount, "Test funds");
    }

    @Test
    void aRefurbishmentTheForceCannotAffordDoesNotStart() throws Exception {
        Refit refurbishment = newRefurbishment();
        Money price = campaign.getQuartermaster().getRefurbishmentCost(refurbishment);
        assertTrue(price.isPositive(), "The refurbishment has a price");
        addFunds(price.minus(Money.of(1)));
        Money fundsBefore = balance();

        boolean isStarted = refurbishment.begin();

        assertFalse(isStarted);
        assertNull(wolverine.getRefit(), "The unit is not being refurbished");
        assertEquals(fundsBefore, balance(), "Nothing is charged");
        assertEquals(List.of(), campaign.getPlayerForce().getShoppingList().getShoppingList());
    }

    @Test
    void aRefurbishmentIsPaidOnceWhenItStartsAndOrdersNoKit() throws Exception {
        addFunds(Money.of(10000000));
        Money fundsBefore = balance();
        Refit refurbishment = newRefurbishment();
        Money price = campaign.getQuartermaster().getRefurbishmentCost(refurbishment);

        boolean isStarted = refurbishment.begin();

        assertTrue(isStarted);
        assertSame(refurbishment, wolverine.getRefit());
        assertEquals(fundsBefore.minus(price), balance(), "Paid in full when it starts");
        assertEquals(List.of(), campaign.getPlayerForce().getShoppingList().getShoppingList(), "No kit is ordered");
        assertTrue(refurbishment.acquireParts(), "Nothing to wait for");

        refurbishment.succeed();

        assertNull(wolverine.getRefit());
        assertEquals(fundsBefore.minus(price), balance(), "Completing costs nothing further");
    }
}

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

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * While a refit waits for its kit, the Hangar says so instead of showing work time that is not passing, and the
 * warehouse still lists the kit's parts, which are set aside for the refit (issue #10583).
 */
class RefitWaitingForPartsTest {
    private static final int TRANSIT_DAYS = 3;

    private PartsScenario scenario;
    private Unit locust;
    private Refit refit;

    @BeforeEach
    void setUp() throws Exception {
        scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_PARTS, true);
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.STARTING_CAPITAL, campaign.getLocalDate(), Money.of(1000000), "Test funds");
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);
        refit.begin();
    }

    private void findTheKit() {
        refit.find(TRANSIT_DAYS, 1.0);
        refit.changeQuantity(-1);
        scenario.getCampaign().getPlayerForce().getShoppingList().removeZeroQuantityFromList();
    }

    private List<Part> kitPartsInTransit() {
        return scenario.getCampaign()
                     .getPlayerForce()
                     .getWarehouse()
                     .getHeldParts()
                     .stream()
                     .filter(part -> part.isReservedForRefit() && !part.isPresent())
                     .toList();
    }

    @Test
    void aKitNotYetFoundIsWaitedFor() {
        assertEquals(Refit.PARTS_NOT_FOUND, refit.getDaysUntilPartsArrive());
        assertEquals("Refit waiting for its kit", locust.getStatus());
    }

    @Test
    void aKitOnItsWayShowsWhenItArrives() {
        findTheKit();

        assertEquals(TRANSIT_DAYS, refit.getDaysUntilPartsArrive());
        assertEquals("Refit kit arrives in 3 days", locust.getStatus());
    }

    @Test
    void theKitsPartsAreHeldInTheWarehouseWhileOnTheirWay() {
        findTheKit();

        assertFalse(kitPartsInTransit().isEmpty(), "The kit's parts are listed while on their way");
        boolean isAnyListedAsSpare = kitPartsInTransit().stream().anyMatch(Part::isSpare);
        assertFalse(isAnyListedAsSpare, "They stay set aside for the refit, not spare");
    }

    @Test
    void partsFittedToAUnitAreNotHeld() {
        List<Part> heldParts = scenario.getCampaign().getPlayerForce().getWarehouse().getHeldParts();

        boolean isAnyFitted = heldParts.stream().anyMatch(part -> part.getUnit() != null);
        assertFalse(isAnyFitted, "The Locust's own parts are not warehouse stock");
    }

    @Test
    void onceTheKitArrivesTheWorkTimeIsShown() {
        findTheKit();
        for (Part part : kitPartsInTransit()) {
            part.setDaysToArrival(0);
        }
        if (refit.getNewArmorSupplies() != null) {
            refit.getNewArmorSupplies().setDaysToArrival(0);
        }

        assertEquals(0, refit.getDaysUntilPartsArrive());
        assertTrue(locust.getStatus().startsWith("Refitting"), locust.getStatus());
    }
}

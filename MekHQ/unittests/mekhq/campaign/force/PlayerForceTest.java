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
package mekhq.campaign.force;

import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.DOUBLE_ALL_COSTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.reputation.camOpsReputation.ForceReputationController;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PlayerForce#getPurchaseCostMultiplier}: the {@code DOUBLE_ALL_COSTS} special rule that doubles unit and
 * part purchase costs while an active contract carries it; and the force-wide helpers of {@link AbstractForce}, which
 * must see every detachment of a split force.
 */
class PlayerForceTest {

    private static PlayerForce playerForce() {
        return new PlayerForce(mock(Faction.class),
              megamek.common.enums.Faction.IS,
              null,
              mock(Finances.class),
              mock(ForceReputationController.class),
              0,
              mock(FactionStandings.class),
              mock(CampaignOptions.class));
    }

    private static AbstractContract contractUsingDoubleAllCosts(boolean usesRule) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.usesSpecialRule(DOUBLE_ALL_COSTS)).thenReturn(usesRule);
        return contract;
    }

    @Test
    void multiplierIsOneWithNoActiveContracts() {
        PlayerForce playerForce = playerForce();

        assertEquals(1.0, playerForce.getPurchaseCostMultiplier(Collections.emptyList()));
    }

    @Test
    void multiplierIsOneWhenNoActiveContractUsesTheRule() {
        PlayerForce playerForce = playerForce();
        List<AbstractContract> contracts = List.of(contractUsingDoubleAllCosts(false),
              contractUsingDoubleAllCosts(false));

        assertEquals(1.0, playerForce.getPurchaseCostMultiplier(contracts));
    }

    @Test
    void multiplierIsTwoWhenTheOnlyActiveContractUsesTheRule() {
        PlayerForce playerForce = playerForce();
        List<AbstractContract> contracts = List.of(contractUsingDoubleAllCosts(true));

        assertEquals(2.0, playerForce.getPurchaseCostMultiplier(contracts));
    }

    @Test
    void multiplierIsTwoWhenAnyActiveContractUsesTheRule() {
        PlayerForce playerForce = playerForce();
        // The rule-carrying contract is not the first in the list, so every contract must be checked.
        List<AbstractContract> contracts = List.of(contractUsingDoubleAllCosts(false),
              contractUsingDoubleAllCosts(true),
              contractUsingDoubleAllCosts(false));

        assertEquals(2.0, playerForce.getPurchaseCostMultiplier(contracts));
    }

    private static Unit unitWithId() {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        return unit;
    }

    /** A force split across the two given detachments. */
    private static PlayerForce splitForce(Detachment first, Detachment second) {
        PlayerForce playerForce = spy(playerForce());
        doReturn(List.of(first, second)).when(playerForce).getDetachments();
        return playerForce;
    }

    @Test
    void forceWideHelpersSeeEveryDetachment() {
        Detachment first = new Detachment();
        Detachment second = new Detachment();
        Unit firstUnit = unitWithId();
        Unit secondUnit = unitWithId();
        first.getHangar().addUnit(firstUnit);
        second.getHangar().addUnit(secondUnit);

        PlayerForce playerForce = splitForce(first, second);

        assertEquals(2, playerForce.allUnits().size());
        assertTrue(playerForce.allUnits().containsAll(List.of(firstUnit, secondUnit)));
        assertSame(firstUnit, playerForce.findUnit(firstUnit.getId()));
        assertSame(secondUnit, playerForce.findUnit(secondUnit.getId()));
        assertNull(playerForce.findUnit(UUID.randomUUID()));
        assertEquals(List.of(first.getWarehouse(), second.getWarehouse()), playerForce.allWarehouses());
    }

    @Test
    void forceWideHelpersMatchTheSingleDetachmentWhenNotSplit() {
        PlayerForce playerForce = playerForce();
        Unit unit = unitWithId();
        playerForce.getHangar().addUnit(unit);

        assertEquals(List.of(unit), List.copyOf(playerForce.allUnits()));
        assertSame(unit, playerForce.findUnit(unit.getId()));
        assertEquals(List.of(playerForce.getWarehouse()), playerForce.allWarehouses());
    }
}

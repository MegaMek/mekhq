/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ContractMarketMethodTest {
    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
          MekHQ.getMHQOptions().getLocale());

    @ParameterizedTest
    @EnumSource(ContractMarketMethod.class)
    void toolTipTextResolvesFromTheMarketBundle(ContractMarketMethod method) {
        assertEquals(resources.getString("ContractMarketMethod." + method.name() + ".toolTipText"),
              method.getToolTipText());
    }

    @ParameterizedTest
    @EnumSource(ContractMarketMethod.class)
    void toStringResolvesFromTheMarketBundle(ContractMarketMethod method) {
        assertEquals(resources.getString("ContractMarketMethod." + method.name() + ".text"), method.toString());
    }

    @ParameterizedTest
    @EnumSource(ContractMarketMethod.class)
    void isNoneIsTrueForExactlyTheNoneConstant(ContractMarketMethod method) {
        assertEquals(method == ContractMarketMethod.NONE, method.isNone());
    }

    @ParameterizedTest
    @EnumSource(ContractMarketMethod.class)
    void isChaosCampaignIsTrueForExactlyTheChaosCampaignConstant(ContractMarketMethod method) {
        assertEquals(method == ContractMarketMethod.CHAOS_CAMPAIGN, method.isChaosCampaign());
    }

    /**
     * Every method is either "no market at all" or one that generates contracts. The predicates are used as an
     * either/or throughout the campaign options, so a constant that answers {@code false} to both - or {@code true} to
     * both - would slip through those branches unnoticed.
     */
    @ParameterizedTest
    @EnumSource(ContractMarketMethod.class)
    void everyMethodIsExactlyOneOfNoneOrChaosCampaign(ContractMarketMethod method) {
        assertTrue(method.isNone() ^ method.isChaosCampaign(),
              method + " must answer true to exactly one of isNone()/isChaosCampaign()");
    }

    @Test
    void distinctConstantsHaveDistinctDisplayText() {
        assertFalse(ContractMarketMethod.NONE.toString().equals(ContractMarketMethod.CHAOS_CAMPAIGN.toString()),
              "the two market methods must be distinguishable in the campaign options dropdown");
    }
}

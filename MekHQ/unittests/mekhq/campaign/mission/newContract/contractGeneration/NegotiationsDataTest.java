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
package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.campaign.mission.newContract.contractGeneration.NegotiationTermsTables.SALVAGE_RIGHTS_EXCHANGE_MARKER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.enums.ContractCommandRights;
import org.junit.jupiter.api.Test;

class NegotiationsDataTest {
    private static NegotiationsData baseline() {
        return new NegotiationsData(ContractCommandRights.HOUSE, 0.5, 0.5, 0.5);
    }

    // ---- Accessors --------------------------------------------------------------------------

    @Test
    public void testAccessors() {
        NegotiationsData data = new NegotiationsData(ContractCommandRights.LIAISON, 0.25, 0.75, 1.0);

        assertEquals(ContractCommandRights.LIAISON, data.commandRights());
        assertEquals(0.25, data.salvageRights());
        assertEquals(0.75, data.supportRights());
        assertEquals(1.0, data.transportRights());
    }

    // ---- isExchange -------------------------------------------------------------------------

    @Test
    public void testIsExchange_WhenSalvageIsExchangeMarker() {
        NegotiationsData data = new NegotiationsData(ContractCommandRights.HOUSE,
              SALVAGE_RIGHTS_EXCHANGE_MARKER, 0.5, 0.5);

        assertTrue(data.isExchange());
    }

    @Test
    public void testIsExchange_WhenSalvageIsRegularValue() {
        assertFalse(baseline().isExchange());
    }

    @Test
    public void testIsExchange_WhenSalvageIsZero() {
        NegotiationsData data = new NegotiationsData(ContractCommandRights.HOUSE, 0.0, 0.5, 0.5);

        assertFalse(data.isExchange());
    }

    // ---- isBattleCompensation ---------------------------------------------------------------

    @Test
    public void testIsBattleCompensation_WhenSupportNegative() {
        NegotiationsData data = new NegotiationsData(ContractCommandRights.HOUSE, 0.5, -0.5, 0.5);

        assertTrue(data.isBattleCompensation());
    }

    @Test
    public void testIsBattleCompensation_WhenSupportZero() {
        NegotiationsData data = new NegotiationsData(ContractCommandRights.HOUSE, 0.5, 0.0, 0.5);

        assertFalse(data.isBattleCompensation());
    }

    @Test
    public void testIsBattleCompensation_WhenSupportPositive() {
        assertFalse(baseline().isBattleCompensation());
    }

    // ---- updateClause: happy paths ----------------------------------------------------------

    @Test
    public void testUpdateClause_CommandRights() {
        NegotiationsData data = baseline().updateClause(ContractNegotiationClause.COMMAND_RIGHTS,
              ContractCommandRights.INTEGRATED);

        assertEquals(ContractCommandRights.INTEGRATED, data.commandRights());
        // Other fields unchanged.
        assertEquals(0.5, data.salvageRights());
        assertEquals(0.5, data.supportRights());
        assertEquals(0.5, data.transportRights());
    }

    @Test
    public void testUpdateClause_SalvageRights() {
        NegotiationsData data = baseline().updateClause(ContractNegotiationClause.SALVAGE_RIGHTS, 0.9);

        assertEquals(0.9, data.salvageRights());
        assertEquals(ContractCommandRights.HOUSE, data.commandRights());
        assertEquals(0.5, data.supportRights());
        assertEquals(0.5, data.transportRights());
    }

    @Test
    public void testUpdateClause_SupportRights() {
        NegotiationsData data = baseline().updateClause(ContractNegotiationClause.SUPPORT_RIGHTS, 0.9);

        assertEquals(0.9, data.supportRights());
        assertEquals(ContractCommandRights.HOUSE, data.commandRights());
        assertEquals(0.5, data.salvageRights());
        assertEquals(0.5, data.transportRights());
    }

    @Test
    public void testUpdateClause_TransportRights() {
        NegotiationsData data = baseline().updateClause(ContractNegotiationClause.TRANSPORT_RIGHTS, 0.9);

        assertEquals(0.9, data.transportRights());
        assertEquals(ContractCommandRights.HOUSE, data.commandRights());
        assertEquals(0.5, data.salvageRights());
        assertEquals(0.5, data.supportRights());
    }

    @Test
    public void testUpdateClause_ReturnsNewInstance() {
        NegotiationsData original = baseline();

        assertNotSame(original, original.updateClause(ContractNegotiationClause.SALVAGE_RIGHTS, 0.9));
    }

    @Test
    public void testUpdateClause_SalvageToExchangeMarker_MakesIsExchangeTrue() {
        NegotiationsData data = baseline().updateClause(ContractNegotiationClause.SALVAGE_RIGHTS,
              SALVAGE_RIGHTS_EXCHANGE_MARKER);

        assertTrue(data.isExchange());
    }

    // ---- updateClause: type mismatches ------------------------------------------------------

    @Test
    public void testUpdateClause_CommandRightsWithWrongType_Throws() {
        NegotiationsData original = baseline();

        assertThrows(IllegalArgumentException.class,
              () -> original.updateClause(ContractNegotiationClause.COMMAND_RIGHTS, 0.9));
    }

    @Test
    public void testUpdateClause_SalvageRightsWithString_Throws() {
        NegotiationsData original = baseline();

        assertThrows(IllegalArgumentException.class,
              () -> original.updateClause(ContractNegotiationClause.SALVAGE_RIGHTS, "not a double"));
    }

    @Test
    public void testUpdateClause_DoubleClauseWithInteger_Throws() {
        // (double) newValue unboxes through Double, so an Integer cannot satisfy a double clause.
        NegotiationsData original = baseline();

        assertThrows(IllegalArgumentException.class,
              () -> original.updateClause(ContractNegotiationClause.TRANSPORT_RIGHTS, 3));
    }
}

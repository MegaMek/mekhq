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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.mission.contract.contractData.ActiveNegotiationData.Kind;
import mekhq.campaign.mission.contract.contractData.NegotiationStepMath.Term;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link ActiveNegotiationData}: the kind discriminator and the {@link Term}-to-delta mapping shared by both
 * outcomes.
 */
class ActiveNegotiationDataTest {

    @Test
    void haggleIsTaggedHaggleAndExposesPerTermDeltas() {
        // haggle(attempts, netMargin, pay, support, transport, salvage, command)
        ActiveNegotiationData data = ActiveNegotiationData.haggle(1, 2, 1, -1, 2, -2, 3);
        assertEquals(Kind.HAGGLE, data.kind());
        assertEquals(1, data.attempts());
        assertEquals(2, data.netMargin());
        assertEquals(1, data.deltaFor(Term.BASE_PAY));
        assertEquals(-1, data.deltaFor(Term.SUPPORT));
        assertEquals(2, data.deltaFor(Term.TRANSPORT));
        assertEquals(-2, data.deltaFor(Term.SALVAGE));
        assertEquals(3, data.deltaFor(Term.COMMAND_RIGHTS));
    }

    @Test
    void exceptionIsTaggedExceptionAndExposesPerTermLockChanges() {
        // exception(attempts, netMargin, pay, support, transport, salvage, command); +1 waived, -1 newly locked.
        ActiveNegotiationData data = ActiveNegotiationData.exception(2, 1, 1, 0, -1, 0, 0);
        assertEquals(Kind.EXCEPTION, data.kind());
        assertEquals(2, data.attempts());
        assertEquals(1, data.deltaFor(Term.BASE_PAY));
        assertEquals(0, data.deltaFor(Term.SUPPORT));
        assertEquals(-1, data.deltaFor(Term.TRANSPORT));
        assertEquals(0, data.deltaFor(Term.SALVAGE));
        assertEquals(0, data.deltaFor(Term.COMMAND_RIGHTS));
    }
}

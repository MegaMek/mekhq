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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.contract.utilities.NegotiationStepMath.Term;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verifies {@link NonNegotiableTermsData}: the {@link Term}-to-field mapping in {@code isLocked}, plus the {@code none}
 * and {@code anyLocked} helpers.
 */
class NonNegotiableTermsDataTest {

    @Test
    void noneLocksNothing() {
        NonNegotiableTermsData none = NonNegotiableTermsData.none();
        assertFalse(none.anyLocked());
        for (Term term : Term.values()) {
            assertFalse(none.isLocked(term), term + " must be unlocked in none()");
        }
    }

    @ParameterizedTest
    @EnumSource(Term.class)
    void isLockedReflectsOnlyTheMatchingTerm(final Term locked) {
        NonNegotiableTermsData data = new NonNegotiableTermsData(locked == Term.BASE_PAY, locked == Term.SUPPORT,
              locked == Term.TRANSPORT, locked == Term.SALVAGE, locked == Term.COMMAND_RIGHTS);
        assertTrue(data.anyLocked());
        for (Term term : Term.values()) {
            assertEquals(term == locked, data.isLocked(term),
                  "only " + locked + " should read as locked, but " + term + " disagreed");
        }
    }

    @ParameterizedTest
    @EnumSource(Term.class)
    void withUnlockedClearsOnlyThatTerm(final Term toUnlock) {
        NonNegotiableTermsData allLocked = new NonNegotiableTermsData(true, true, true, true, true);
        NonNegotiableTermsData result = allLocked.withUnlocked(toUnlock);
        for (Term term : Term.values()) {
            assertEquals(term != toUnlock, result.isLocked(term),
                  toUnlock + " should be the only term cleared, but " + term + " disagreed");
        }
    }
}

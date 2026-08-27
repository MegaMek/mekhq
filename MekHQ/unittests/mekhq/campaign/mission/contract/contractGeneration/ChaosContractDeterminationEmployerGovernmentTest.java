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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractDeterminationEmployer.EmployerFactions;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that a government (non-mercenary) contract is always issued by the player's own faction: no matter the
 * rolled employer type, the player faction is both the flavor (paying) employer and the territorial anchor, with no
 * covert sponsor and no themed or special employer.
 */
class ChaosContractDeterminationEmployerGovernmentTest {

    private static final LocalDate DATE = LocalDate.of(3050, 1, 1);

    private final ChaosContractDeterminationEmployerGovernment employer =
          new ChaosContractDeterminationEmployerGovernment();

    @ParameterizedTest
    @EnumSource(ChaosEmployerType.class)
    void theEmployerIsAlwaysThePlayerFactionForEveryRolledType(ChaosEmployerType type) {
        Faction playerFaction = mock(Faction.class);
        // The location and covert-viability are irrelevant to a government contract; the resolution never consults them.
        ILocation location = mock(ILocation.class);

        EmployerFactions result = employer.determineEmployerFactions(type, DATE, location, true, playerFaction);

        assertSame(playerFaction, result.flavor(), "the player's own faction is the employer");
        assertSame(playerFaction, result.anchor(), "the conflict anchors on the player's own faction");
        assertNull(result.sponsor(), "a government contract has no covert sponsor");
    }

    /**
     * The flavor/anchor resolution still runs for a government contract, but the overridden hooks force the player
     * faction whenever a faction is needed &mdash; independent of the rolled type, so no themed or regional lookup can
     * substitute a different faction.
     */
    @ParameterizedTest
    @EnumSource(ChaosEmployerType.class)
    void theFlavorAndAnchorHooksAlwaysReturnThePlayerFaction(ChaosEmployerType type) {
        Faction playerFaction = mock(Faction.class);
        ILocation location = mock(ILocation.class);

        assertSame(playerFaction, employer.resolveFlavorFaction(type, DATE, location, playerFaction),
              "the flavor faction is always the player faction");
        assertSame(playerFaction, employer.resolveAnchorFaction(type, DATE, location, playerFaction, playerFaction),
              "the anchor faction is always the player faction");
    }

    /**
     * The government determination overrides the shared special-employer hook to bar ComStar/Word of Blake outright, so
     * it returns {@code null} regardless of year or covert-viability &mdash; no faction singletons or dice are
     * consulted.
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void noSpecialEmployerIsEverGenerated(boolean covertViable) {
        assertNull(employer.checkForSpecialEmployer(3050, covertViable),
              "a government contract never fronts, is taken over by, or is covertly backed by a special employer");
    }
}

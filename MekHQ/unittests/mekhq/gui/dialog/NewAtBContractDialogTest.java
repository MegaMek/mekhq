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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the planet-name resolution helper extracted from
 * {@link NewAtBContractDialog#resolvePlanetName(String, LocalDate, Systems)}.
 *
 * <p>Guards against regressions in issues #5445 and #8547, where the manual contract
 * creation dialog rejected valid planet input because validation only consulted the
 * dropdown {@code cbPlanets} and ignored the suggest-field that is active when
 * "Show All Planets" is selected.</p>
 */
class NewAtBContractDialogTest {

    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);

    @ParameterizedTest
    @NullAndEmptySource
    void resolvePlanetName_nullOrEmptyReturnsNullWithoutConsultingRegistry(String input) {
        Systems registry = mock(Systems.class);

        PlanetarySystem result = NewAtBContractDialog.resolvePlanetName(input, DATE, registry);

        assertNull(result);
        // Empty/null input must short-circuit; never touch the registry.
        verify(registry, never()).getSystemByName(org.mockito.ArgumentMatchers.anyString(),
              org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    void resolvePlanetName_unknownNameReturnsNull() {
        Systems registry = mock(Systems.class);
        when(registry.getSystemByName("NotARealSystemXYZ", DATE)).thenReturn(null);

        PlanetarySystem result = NewAtBContractDialog.resolvePlanetName("NotARealSystemXYZ", DATE, registry);

        assertNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Terra", "Galatea" })
    void resolvePlanetName_knownNameReturnsRegistryResult(String name) {
        Systems registry = mock(Systems.class);
        PlanetarySystem expected = mock(PlanetarySystem.class);
        when(registry.getSystemByName(name, DATE)).thenReturn(expected);

        PlanetarySystem result = NewAtBContractDialog.resolvePlanetName(name, DATE, registry);

        assertSame(expected, result);
    }
}

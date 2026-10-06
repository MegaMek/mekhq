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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the {@link AbstractContractGeneration#forSearchType(ContractSearchType)} factory and the per-search-type
 * generator hierarchy: every search type resolves to a concrete generator that declares exactly that search type.
 *
 * @author Illiani
 * @since 0.51.01
 */
class AbstractContractGenerationTest {

    @ParameterizedTest
    @EnumSource(ContractSearchType.class)
    void forSearchTypeReturnsAGeneratorDeclaringThatSearchType(final ContractSearchType searchType) {
        AbstractContractGeneration generator = AbstractContractGeneration.forSearchType(searchType);

        assertNotNull(generator, "a generator exists for " + searchType);
        assertSame(searchType, generator.getSearchType(), "generator's search type for " + searchType);
    }

    @Test
    void forSearchTypeMapsEachSearchTypeToItsConcreteGenerator() {
        assertInstanceOf(MercenaryContractGeneration.class,
              AbstractContractGeneration.forSearchType(ContractSearchType.MERCENARY));
        assertInstanceOf(ActsOfPiracyContractGeneration.class,
              AbstractContractGeneration.forSearchType(ContractSearchType.PIRATE));
        assertInstanceOf(GovernmentContractGeneration.class,
              AbstractContractGeneration.forSearchType(ContractSearchType.GOVERNMENT));
        assertInstanceOf(TournamentContractGeneration.class,
              AbstractContractGeneration.forSearchType(ContractSearchType.TOURNAMENT));
    }
}

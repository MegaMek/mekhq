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
package mekhq.campaign.market.contractMarket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.EnemySelectionProfile;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link AbstractContractMarket#setEnemyCode} derives the {@link EnemySelectionProfile} from the contract's
 * type and passes it through to {@link RandomFactionGenerator}; the profiles' own selection behavior is tested directly
 * in {@code RandomFactionGeneratorTest}.
 */
class AbstractContractMarketEnemySelectionTest {

    @AfterEach
    void tearDown() {
        RandomFactionGenerator.setInstance(null);
    }

    @Test
    void setEnemyCodeDerivesProfileFromContractType() {
        Faction enemy = mock(Faction.class);
        when(enemy.getShortName()).thenReturn("ENEMY");
        // An aggregate enemy never rolls for mercenary substitution, keeping this test deterministic.
        when(enemy.isAggregate()).thenReturn(true);

        RandomFactionGenerator rfg = mock(RandomFactionGenerator.class);
        when(rfg.getRandomEnemy(any(), any(), any(), any(EnemySelectionProfile.class))).thenReturn(enemy);
        RandomFactionGenerator.setInstance(rfg);

        AtBContract contract = mock(AtBContract.class);
        when(contract.getContractType()).thenReturn(AtBContractType.GUERRILLA_WARFARE);
        when(contract.getEmployerCode()).thenReturn("EMPLOYER");
        Campaign campaign = mock(Campaign.class);

        new AtbMonthlyContractMarket().setEnemyCode(contract, campaign);

        verify(rfg).getRandomEnemy(any(), any(), any(), eq(EnemySelectionProfile.OCCUPYING_POWER));
        verify(contract).setEnemyCode("ENEMY");
    }
}

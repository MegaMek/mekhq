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
package mekhq.campaign.mission;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.UnitGeneratorParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

import java.io.File;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BotForceRandomizerTest {
    private Campaign mockCampaign;

    private void initializeTest() {
        // Set up a test campaign with a unit generator that produces only Griffin Sparkys.
        // There's definitely room for these tests to cover more, but this at least checks that it
        // doesn't throw when used in the way that Story Arcs do.
        EquipmentType.initializeTypes();

        MekSummary mockGeneratedMekSummary = mock(MekSummary.class);
        when(mockGeneratedMekSummary.getSourceFile()).thenReturn(new File(MHQTestUtilities.TEST_UNIT_DATA_DIR +
                                                                                "Griffin GRF-1E Sparky.mtf"));
        when(mockGeneratedMekSummary.getEntryName()).thenReturn(null);

        IUnitGenerator mockUnitGenerator = mock(IUnitGenerator.class);
        when(mockUnitGenerator.generate(any(UnitGeneratorParameters.class))).thenReturn(mockGeneratedMekSummary);

        mockCampaign = mock(Campaign.class);
        when(mockCampaign.getUnitGenerator()).thenReturn(mockUnitGenerator);
        when(mockCampaign.getPlayer()).thenReturn(new Player(1, "player"));
        when(mockCampaign.getCampaignOptions()).thenReturn(new CampaignOptions());
    }

    @Test
    public void testBotForceRandomizerAdjustedWeight() {
        initializeTest();

        Unit mockPlayerUnit = mock(Unit.class);
        Entity mockPlayerUnitEntity = mock(Entity.class);
        when(mockPlayerUnit.getEntity()).thenReturn(mockPlayerUnitEntity);

        when(mockPlayerUnitEntity.getWeight()).thenReturn(55.0);
        when(mockPlayerUnitEntity.getUnitType()).thenReturn(UnitType.MEK);

        List<Unit> playerUnits = List.of(mockPlayerUnit);
        List<Entity> fixedEntities = List.of();

        BotForceRandomizer randomizer = new BotForceRandomizer();
        randomizer.setBalancingMethod(BotForceRandomizer.BalancingMethod.WEIGHT_ADJ);
        randomizer.setForceMultiplier(2.0);

        List<Entity> generated = randomizer.generateForce(playerUnits, fixedEntities, mockCampaign);

        // There should be two units generated, as the player units score 55 adjusted tons, and a Griffin scores 55
        // adjusted tons.
        assertEquals(2, generated.size());
    }

    @Test
    public void testBotForceRandomizerBV() {
        initializeTest();

        Unit mockPlayerUnit = mock(Unit.class);
        Entity mockPlayerUnitEntity = mock(Entity.class);
        when(mockPlayerUnit.getEntity()).thenReturn(mockPlayerUnitEntity);
        when(mockPlayerUnitEntity.calculateBattleValue()).thenReturn(1449);

        List<Unit> playerUnits = List.of(mockPlayerUnit);
        List<Entity> fixedEntities = List.of();

        BotForceRandomizer randomizer = new BotForceRandomizer();
        randomizer.setBalancingMethod(BotForceRandomizer.BalancingMethod.BV);
        randomizer.setForceMultiplier(2.0);

        List<Entity> generated = randomizer.generateForce(playerUnits, fixedEntities, mockCampaign);

        // There should be two units generated, as the player units score 1449BV, the same as the
        // Sparky.
        assertEquals(2, generated.size());
    }
}

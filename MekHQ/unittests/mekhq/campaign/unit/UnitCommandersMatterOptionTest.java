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

package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.*;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UnitCommandersMatterOptionTest {

    // not static because we want @BeforeAll to run first
    private final List<Entity> allEntities = List.of(
          new AeroSpaceFighter(), new BattleArmor(), new BipedMek(), new CombatVehicleEscapePod(), new ConvFighter(),
          new ConvInfantry(), new Dropship(), new EjectedCrew(), new FighterSquadron(), new FixedWingSupport(),
          new Jumpship(), new LandAirMek(0, 0, 0), new LargeSupportTank(), new MekWarrior(), new ProtoMek(),
          new QuadMek(), new QuadVee(), new SmallCraft(), new SpaceStation(), new SuperHeavyTank(), new Tank(),
          new TripodMek(), new VTOL(), new Warship()
    );

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private void runTest(CampaignOptions options, Set<Class<? extends Entity>> expectedTrueClasses) {
        for (Entity entity : allEntities) {
            boolean expected = expectedTrueClasses.contains(entity.getClass());
            assertEquals(expected,
                  new Unit(entity, null).isOnlyCommandersMatter(options),
                  String.format("Unexpected isOnlyCommandersMatter for %s", entity.getClass().getSimpleName())
            );
        }
    }

    @Test
    public void testIsOnlyCommandersMatter_Vehicles() {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isOnlyCommandersMatterVehicles()).thenReturn(true);
        Set<Class<? extends Entity>> expectedTrue = Set.of(
              LargeSupportTank.class, SuperHeavyTank.class, Tank.class, VTOL.class);
        runTest(options, expectedTrue);
    }

    @Test
    public void testIsOnlyCommandersMatter_Infantry() {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isOnlyCommandersMatterInfantry()).thenReturn(true);
        Set<Class<? extends Entity>> expectedTrue = Set.of(ConvInfantry.class, EjectedCrew.class, MekWarrior.class);
        runTest(options, expectedTrue);
    }

    @Test
    public void testIsOnlyCommandersMatter_BA() {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isOnlyCommandersMatterBattleArmor()).thenReturn(true);
        Set<Class<? extends Entity>> expectedTrue = Set.of(BattleArmor.class);
        runTest(options, expectedTrue);
    }
}

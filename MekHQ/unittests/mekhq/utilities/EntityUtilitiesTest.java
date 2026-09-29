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
package mekhq.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Sensor;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests the sensor equipment checks used by StratCon advanced scouting.
 *
 * @author Illiani
 * @since 0.51.01
 */
class EntityUtilitiesTest {
    private static MiscMounted mockMisc(boolean isBeagleActiveProbe, String internalName, boolean isOperable) {
        MiscType type = mock(MiscType.class);
        when(type.hasFlag(MiscType.F_BAP)).thenReturn(isBeagleActiveProbe);
        when(type.getInternalName()).thenReturn(internalName);

        MiscMounted mounted = mock(MiscMounted.class);
        when(mounted.getType()).thenReturn(type);
        when(mounted.isOperable()).thenReturn(isOperable);
        return mounted;
    }

    private static Entity mockEntity(MiscMounted... equipment) {
        Entity entity = mock(Entity.class);
        when(entity.getMisc()).thenReturn(new ArrayList<>(List.of(equipment)));
        return entity;
    }

    @Nested
    class ImprovedSensors {
        @Test
        void workingInnerSphereImprovedSensors_counts() {
            Entity entity = mockEntity(mockMisc(true, Sensor.IS_IMPROVED, true));
            assertTrue(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void workingClanImprovedSensors_counts() {
            Entity entity = mockEntity(mockMisc(true, Sensor.CL_IMPROVED, true));
            assertTrue(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void damagedImprovedSensors_doNotCount() {
            Entity entity = mockEntity(mockMisc(true, Sensor.IS_IMPROVED, false));
            assertFalse(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void plainActiveProbe_isNotImprovedSensors() {
            Entity entity = mockEntity(mockMisc(true, "BeagleActiveProbe", true));
            assertFalse(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void improvedSensorsQuirk_countsWithoutEquipment() {
            Entity entity = mockEntity();
            when(entity.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)).thenReturn(true);
            assertTrue(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void improvedSensorsQuirk_countsEvenWhenSensorEquipmentIsDamaged() {
            Entity entity = mockEntity(mockMisc(true, Sensor.IS_IMPROVED, false));
            when(entity.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)).thenReturn(true);
            assertTrue(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void noEquipment_doesNotCount() {
            assertFalse(EntityUtilities.hasImprovedSensors(mockEntity()));
        }

        @Test
        void equipmentWithNullType_isSkipped() {
            MiscMounted mounted = mock(MiscMounted.class);
            when(mounted.getType()).thenReturn(null);
            Entity entity = mockEntity(mounted, mockMisc(true, Sensor.IS_IMPROVED, true));
            assertTrue(EntityUtilities.hasImprovedSensors(entity));
        }

        @Test
        void probeWithNullInternalName_isNotImprovedSensors() {
            Entity entity = mockEntity(mockMisc(true, null, true));
            assertFalse(EntityUtilities.hasImprovedSensors(entity));
        }
    }

    @Nested
    class ActiveProbe {
        @Test
        void workingActiveProbe_counts() {
            Entity entity = mockEntity(mockMisc(true, "BeagleActiveProbe", true));
            assertTrue(EntityUtilities.hasActiveProbe(entity));
        }

        @Test
        void damagedActiveProbe_doesNotCount() {
            Entity entity = mockEntity(mockMisc(true, "BeagleActiveProbe", false));
            assertFalse(EntityUtilities.hasActiveProbe(entity));
        }

        @Test
        void improvedSensors_areNotAnActiveProbe() {
            Entity entity = mockEntity(mockMisc(true, Sensor.IS_IMPROVED, true),
                  mockMisc(true, Sensor.CL_IMPROVED, true));
            assertFalse(EntityUtilities.hasActiveProbe(entity));
        }

        @Test
        void equipmentWithoutProbeFlag_doesNotCount() {
            Entity entity = mockEntity(mockMisc(false, "ISMediumLaser", true));
            assertFalse(EntityUtilities.hasActiveProbe(entity));
        }

        @Test
        void equipmentWithNullType_isSkipped() {
            MiscMounted mounted = mock(MiscMounted.class);
            when(mounted.getType()).thenReturn(null);
            assertFalse(EntityUtilities.hasActiveProbe(mockEntity(mounted)));
        }

        @Test
        void probeWithNullInternalName_countsAsActiveProbe() {
            Entity entity = mockEntity(mockMisc(true, null, true));
            assertTrue(EntityUtilities.hasActiveProbe(entity));
        }

        @Test
        void oneDamagedAndOneWorkingProbe_counts() {
            Entity entity = mockEntity(mockMisc(true, "BeagleActiveProbe", false),
                  mockMisc(true, "BloodhoundActiveProbe", true));
            assertTrue(EntityUtilities.hasActiveProbe(entity));
        }
    }

    @Nested
    class ReconCamera {
        @Test
        void workingMountedCamera_counts() {
            Entity entity = mock(Entity.class);
            when(entity.hasWorkingMisc(MiscType.F_RECON_CAMERA)).thenReturn(true);
            assertTrue(EntityUtilities.hasReconCamera(entity));
        }

        @Test
        void noMountedCameraOnNonBomber_doesNotCount() {
            Entity entity = mock(Entity.class);
            when(entity.hasWorkingMisc(MiscType.F_RECON_CAMERA)).thenReturn(false);
            assertFalse(EntityUtilities.hasReconCamera(entity));
        }

        @Test
        void cameraPodInBombLoadout_counts() {
            BombLoadout loadout = new BombLoadout();
            loadout.addBombs(BombTypeEnum.RECON_CAMERA, 1);
            Aero aero = mock(Aero.class);
            when(aero.getBombChoices()).thenReturn(loadout);
            assertTrue(EntityUtilities.hasReconCamera(aero));
        }

        @Test
        void bomberWithoutCameraPod_doesNotCount() {
            BombLoadout loadout = new BombLoadout();
            loadout.addBombs(BombTypeEnum.HE, 4);
            Aero aero = mock(Aero.class);
            when(aero.getBombChoices()).thenReturn(loadout);
            assertFalse(EntityUtilities.hasReconCamera(aero));
        }

        @Test
        void bomberWithMountedCamera_countsWithoutPod() {
            Aero aero = mock(Aero.class);
            when(aero.hasWorkingMisc(MiscType.F_RECON_CAMERA)).thenReturn(true);
            when(aero.getBombChoices()).thenReturn(new BombLoadout());
            assertTrue(EntityUtilities.hasReconCamera(aero));
        }
    }
}

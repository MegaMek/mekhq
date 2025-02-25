/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit.cleanup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;

class MovedAmmoBinTest {
    @Test
    void notAmmoBinEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentPart mockPart = mock(EquipmentPart.class);

        MovedAmmoBinStep step = new MovedAmmoBinStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    void noMatchingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        when(mockProposal.getEquipment()).thenReturn(Collections.emptySet());
        AmmoBin mockPart = mock(AmmoBin.class);

        MovedAmmoBinStep step = new MovedAmmoBinStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    void doesNotMatchDestroyedEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.isDestroyed()).thenReturn(true);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        AmmoBin mockPart = mock(AmmoBin.class);

        MovedAmmoBinStep step = new MovedAmmoBinStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    void doesNotMatchEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        AmmoBin mockPart = mock(AmmoBin.class);
        when(mockPart.getType()).thenReturn(mock(AmmoType.class));

        MovedAmmoBinStep step = new MovedAmmoBinStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    void doesNotMatchAmmoTypeTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(AmmoType.class));
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        AmmoBin mockPart = mock(AmmoBin.class);
        when(mockPart.getType()).thenReturn(mock(AmmoType.class));

        MovedAmmoBinStep step = new MovedAmmoBinStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    void mountMatchesEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        AmmoType mockType = mock(AmmoType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(1, mockMount).entrySet());
        AmmoBin mockPart = mock(AmmoBin.class);
        when(mockPart.getType()).thenReturn(mock(AmmoType.class));
        doReturn(true).when(mockPart).canChangeMunitions(mockType);

        MovedAmmoBinStep step = new MovedAmmoBinStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(1)).proposeMapping(mockPart, 1);
    }
}

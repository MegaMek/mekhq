/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.Aero;
import megamek.common.EquipmentType;
import megamek.common.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class MissingHeatSinkTest {
    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * https://github.com/MegaMek/mekhq/issues/2365
     */
    @Test
    public void missingHeatSinkSelectsCorrectPartDuringRepair() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());
        Mek mek = mock(Mek.class);
        when(mek.getWeight()).thenReturn(65.0);
        when(unit.getEntity()).thenReturn(mek);
        EquipmentType heatSinkType = mock(EquipmentType.class);

        // Create a missing heat sink on a unit
        int equipmentNum = 17;
        MissingHeatSink missingHeatSink = new MissingHeatSink(1, heatSinkType, equipmentNum, false, mockCampaign);
        missingHeatSink.setUnit(unit);
        warehouse.addPart(missingHeatSink);

        // Add an aero heat sink that isn't legit
        AeroHeatSink aeroHeatSink = new AeroHeatSink(1, Aero.HEAT_DOUBLE, false, mockCampaign);
        warehouse.addPart(aeroHeatSink);

        // Add an incorrect heat sink
        EquipmentType otherHeatSinkType = mock(EquipmentType.class);
        HeatSink otherHeatSink = new HeatSink(1, otherHeatSinkType, -1, false, mockCampaign);
        warehouse.addPart(otherHeatSink);

        // Add the correct heat sink
        HeatSink legitHeatSink = new HeatSink(1, heatSinkType, -1, false, mockCampaign);
        warehouse.addPart(legitHeatSink);

        missingHeatSink.fix();

        assertFalse(warehouse.getParts().contains(missingHeatSink));
        assertNull(missingHeatSink.getUnit());
        assertFalse(warehouse.getParts().contains(legitHeatSink));

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(partCaptor.capture());
        verify(unit, times(1)).removePart(eq(missingHeatSink));

        Part addedPart = partCaptor.getValue();
        assertInstanceOf(HeatSink.class, addedPart);

        HeatSink addedHeatSink = (HeatSink) addedPart;
        assertEquals(unit, addedHeatSink.getUnit());
        assertEquals(equipmentNum, addedHeatSink.getEquipmentNum());
        assertEquals(heatSinkType, addedHeatSink.getType());
    }
}

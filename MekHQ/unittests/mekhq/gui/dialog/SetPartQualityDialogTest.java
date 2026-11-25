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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class SetPartQualityDialogTest {

    private static Part part(String name, String location) {
        Part part = mock(Part.class);
        when(part.getName()).thenReturn(name);
        when(part.getLocationName()).thenReturn(location);
        return part;
    }

    @Test
    void getEditableParts_excludesMissingParts() {
        Part installed = part("Medium Laser", "Right Arm");
        MissingPart missing = mock(MissingPart.class);
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn("Wolverine WVR-6R");
        when(unit.getParts()).thenReturn(List.of(installed, missing));

        assertEquals(List.of(installed), SetPartQualityDialog.getEditableParts(unit));
    }

    @Test
    void getEditableParts_sortsByLocationThenName() {
        Part rightArmLaser = part("Medium Laser", "Right Arm");
        Part leftArmActuator = part("Lower Arm Actuator", "Left Arm");
        Part leftArmArmor = part("Armor", "Left Arm");
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn("Wolverine WVR-6R");
        when(unit.getParts()).thenReturn(List.of(rightArmLaser, leftArmActuator, leftArmArmor));

        assertEquals(List.of(leftArmArmor, leftArmActuator, rightArmLaser),
              SetPartQualityDialog.getEditableParts(unit));
    }

    @Test
    void getEditableParts_partWithoutLocationUsesUnitName() {
        // Unit-wide parts (engine, structure, etc.) report no location and are grouped under the unit's name
        Part engine = part("Fusion Engine", null);
        Part zuluLaser = part("Medium Laser", "Zulu");
        Part armLaser = part("Medium Laser", "Right Arm");
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn("Wolverine WVR-6R");
        when(unit.getParts()).thenReturn(List.of(zuluLaser, engine, armLaser));

        // "Right Arm" < "Wolverine WVR-6R" < "Zulu"
        assertEquals(List.of(armLaser, engine, zuluLaser), SetPartQualityDialog.getEditableParts(unit));
    }
}

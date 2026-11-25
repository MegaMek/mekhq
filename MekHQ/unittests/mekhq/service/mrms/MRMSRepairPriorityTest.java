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
package mekhq.service.mrms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * The order Mass Repair works through units: units with crew assigned come first, so damaged salvage or spares can't
 * take the techs' time ahead of the units the player means to field. Within each group the least damaged go first.
 *
 * @author Illiani
 * @since 0.51.01
 */
class MRMSRepairPriorityTest {

    private static Unit unit(boolean isCrewed, int damageState) {
        Unit unit = mock(Unit.class);
        List<Person> crew = new ArrayList<>();
        if (isCrewed) {
            crew.add(mock(Person.class));
        }
        when(unit.getCrew()).thenReturn(crew);
        when(unit.getDamageState()).thenReturn(damageState);
        return unit;
    }

    @Test
    void crewedUnitsComeBeforeUncrewedOnesHoweverDamaged() {
        Unit lightlyDamagedSalvage = unit(false, Entity.DMG_LIGHT);
        Unit heavilyDamagedLineUnit = unit(true, Entity.DMG_HEAVY);
        List<Unit> units = new ArrayList<>(List.of(lightlyDamagedSalvage, heavilyDamagedLineUnit));

        MRMSService.sortByRepairPriority(units);

        assertEquals(List.of(heavilyDamagedLineUnit, lightlyDamagedSalvage), units);
    }

    @Test
    void leastDamagedComeFirstWithinEachGroup() {
        Unit crippledCrewed = unit(true, Entity.DMG_CRIPPLED);
        Unit lightCrewed = unit(true, Entity.DMG_LIGHT);
        Unit heavyUncrewed = unit(false, Entity.DMG_HEAVY);
        Unit moderateUncrewed = unit(false, Entity.DMG_MODERATE);
        List<Unit> units = new ArrayList<>(List.of(heavyUncrewed, crippledCrewed, moderateUncrewed, lightCrewed));

        MRMSService.sortByRepairPriority(units);

        assertEquals(List.of(lightCrewed, crippledCrewed, moderateUncrewed, heavyUncrewed), units);
    }
}

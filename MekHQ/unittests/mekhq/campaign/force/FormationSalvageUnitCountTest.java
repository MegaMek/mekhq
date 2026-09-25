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
package mekhq.campaign.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.mission.scenarios.salvage.CamOpsStrictSalvage;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class FormationSalvageUnitCountTest {
    private final LocalHangar hangar = mock(LocalHangar.class);

    private static Unit unit(boolean canSalvage, boolean isDoomedInSpace) {
        Entity entity = mock(Tank.class);
        when(entity.doomedInSpace()).thenReturn(isDoomedInSpace);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.canSalvage(anyBoolean())).thenReturn(canSalvage);
        when(unit.isRepairable()).thenReturn(true);
        return unit;
    }

    private Formation formationWith(Unit... units) {
        Formation formation = spy(new Formation("Salvage Lance"));
        doReturn(List.of(units)).when(formation).getAllUnitsAsUnits(hangar, false);
        return formation;
    }

    @Test
    void onlyUnitsAvailableForSalvageAreCounted() {
        Formation formation = formationWith(unit(true, false), unit(false, false), unit(true, false));

        assertEquals(2, formation.getSalvageUnitCount(hangar, false, new CamOpsStrictSalvage()));
    }

    @Test
    void unitsThatCantSurviveInSpaceDontCountInSpace() {
        Formation formation = formationWith(unit(true, true), unit(true, false));

        assertEquals(1, formation.getSalvageUnitCount(hangar, true, new CamOpsStrictSalvage()));
    }

    @Test
    void unitsThatCantSurviveInSpaceStillCountOnTheGround() {
        Formation formation = formationWith(unit(true, true));

        assertEquals(1, formation.getSalvageUnitCount(hangar, false, new CamOpsStrictSalvage()));
    }

    @Test
    void emptyFormationHasNoSalvageUnits() {
        assertEquals(0, formationWith().getSalvageUnitCount(hangar, false, new CamOpsStrictSalvage()));
    }
}

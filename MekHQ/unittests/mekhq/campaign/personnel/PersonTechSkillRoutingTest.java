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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.skills.SkillType.S_TECH_AERO;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_BA;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VEHICLE;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VESSEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests the single source of truth mapping a unit type to the whole-unit "global" technician skill used by maintenance
 * and refit routing.
 */
class PersonTechSkillRoutingTest {

    private static Unit unitOf(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    @Test
    void meksProtoMeksAndHandheldWeaponsAreMaintainedByMekTechs() {
        assertEquals(S_TECH_MEK, Person.getGlobalTechSkillNameFor(unitOf(new BipedMek())));
        assertEquals(S_TECH_MEK, Person.getGlobalTechSkillNameFor(unitOf(new ProtoMek())));
        assertEquals(S_TECH_MEK, Person.getGlobalTechSkillNameFor(unitOf(new HandheldWeapon())));
    }

    @Test
    void battleArmorIsMaintainedByBattleArmorTechs() {
        assertEquals(S_TECH_BA, Person.getGlobalTechSkillNameFor(unitOf(new BattleArmor())));
    }

    @Test
    void aerospaceFightersAreMaintainedByAeroTechs() {
        assertEquals(S_TECH_AERO, Person.getGlobalTechSkillNameFor(unitOf(new AeroSpaceFighter())));
    }

    @Test
    void largeCraftAreMaintainedByVesselCrews() {
        assertEquals(S_TECH_VESSEL, Person.getGlobalTechSkillNameFor(unitOf(new Dropship())));
    }

    @Test
    void tanksAndInfantryAreMaintainedByMechanics() {
        assertEquals(S_TECH_VEHICLE, Person.getGlobalTechSkillNameFor(unitOf(new Tank())));
        assertEquals(S_TECH_VEHICLE, Person.getGlobalTechSkillNameFor(unitOf(new ConvInfantry())));
    }

    @Test
    void aNullUnitOrEntityMapsToNoSkill() {
        assertNull(Person.getGlobalTechSkillNameFor(null));
        assertNull(Person.getGlobalTechSkillNameFor(unitOf(null)));
    }
}

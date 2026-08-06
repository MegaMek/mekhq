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
package mekhq.campaign.personnel.skills;

import static megamek.common.units.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.units.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.units.EntityWeightClass.WEIGHT_SUPER_HEAVY;
import static mekhq.campaign.personnel.PersonnelOptions.SPECIALIST_CHOICE_MEK_LIGHT;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_MEK_AFFINITY_LIGHT;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_MEK_ANTIPATHY_LIGHT;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_UNIT_SPECIALIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.options.IOption;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.Test;

class TechChassisModifiersTest {
    /** A Mek of the given weight class. Family detection checks isMek/isFighter/isVehicle in that order. */
    private static Entity mekOfWeight(int weightClass) {
        Entity entity = mock(Entity.class);
        lenient().when(entity.isMek()).thenReturn(true);
        lenient().when(entity.getWeightClass()).thenReturn(weightClass);
        return entity;
    }

    /** A tech whose options carry the given Unit Specialist choice (or no specialist when {@code choice} is null). */
    private static Person techWithSpecialist(String choice) {
        PersonnelOptions options = mock(PersonnelOptions.class);
        IOption specialist = mock(IOption.class);
        lenient().when(specialist.booleanValue()).thenReturn(choice != null);
        lenient().when(specialist.stringValue()).thenReturn(choice);
        lenient().when(options.getOption(TECH_UNIT_SPECIALIST)).thenReturn(specialist);
        Person tech = mock(Person.class);
        lenient().when(tech.getOptions()).thenReturn(options);
        return tech;
    }

    @Test
    void testNullTechYieldsNoModifier() {
        assertEquals(0, TechChassisModifiers.getMaintenanceModifier(null, mekOfWeight(WEIGHT_LIGHT)));
        assertEquals(0, TechChassisModifiers.getRepairModifier(null, mekOfWeight(WEIGHT_LIGHT)));
    }

    @Test
    void testNullEntityYieldsNoModifier() {
        assertEquals(0,
              TechChassisModifiers.getMaintenanceModifier(techWithSpecialist(SPECIALIST_CHOICE_MEK_LIGHT), null));
        assertEquals(0, TechChassisModifiers.getRepairModifier(techWithSpecialist(SPECIALIST_CHOICE_MEK_LIGHT), null));
    }

    @Test
    void testMatchingSpecialistIsBonus() {
        Person tech = techWithSpecialist(SPECIALIST_CHOICE_MEK_LIGHT);
        Entity entity = mekOfWeight(WEIGHT_LIGHT);
        assertEquals(-1, TechChassisModifiers.getMaintenanceModifier(tech, entity));
        assertEquals(-1, TechChassisModifiers.getRepairModifier(tech, entity));
    }

    @Test
    void testMismatchedSpecialistIsPenalty() {
        // Light-Mek specialist working on an assault Mek: wrong specialty -> +1.
        Person tech = techWithSpecialist(SPECIALIST_CHOICE_MEK_LIGHT);
        Entity entity = mekOfWeight(WEIGHT_ASSAULT);
        assertEquals(1, TechChassisModifiers.getMaintenanceModifier(tech, entity));
        assertEquals(1, TechChassisModifiers.getRepairModifier(tech, entity));
    }

    /**
     * Regression for the specialist-null penalty bug: a super-heavy Mek has no specialty choice at all, so a specialist
     * tech must take neither a bonus nor the +1 "wrong specialty" penalty.
     */
    @Test
    void testSpecialistOnUnmappedWeightClassIsNeutral() {
        Person tech = techWithSpecialist(SPECIALIST_CHOICE_MEK_LIGHT);
        Entity entity = mekOfWeight(WEIGHT_SUPER_HEAVY);
        assertEquals(0, TechChassisModifiers.getMaintenanceModifier(tech, entity));
        assertEquals(0, TechChassisModifiers.getRepairModifier(tech, entity));
    }

    @Test
    void testAffinityImprovesMaintenanceOnly() {
        Person tech = techWithSpecialist(null);
        when(tech.getOptions().booleanOption(TECH_MEK_AFFINITY_LIGHT)).thenReturn(true);
        Entity entity = mekOfWeight(WEIGHT_LIGHT);
        // Affinity is the maintenance-only (piloting) analog; repair is unaffected.
        assertEquals(-1, TechChassisModifiers.getMaintenanceModifier(tech, entity));
        assertEquals(0, TechChassisModifiers.getRepairModifier(tech, entity));
    }

    @Test
    void testAntipathyWorsensMaintenanceOnly() {
        Person tech = techWithSpecialist(null);
        when(tech.getOptions().booleanOption(TECH_MEK_ANTIPATHY_LIGHT)).thenReturn(true);
        Entity entity = mekOfWeight(WEIGHT_LIGHT);
        assertEquals(1, TechChassisModifiers.getMaintenanceModifier(tech, entity));
        assertEquals(0, TechChassisModifiers.getRepairModifier(tech, entity));
    }
}

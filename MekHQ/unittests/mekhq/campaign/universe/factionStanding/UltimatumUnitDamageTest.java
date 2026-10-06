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
package mekhq.campaign.universe.factionStanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.damage.PreExistingDamageLevel;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for how {@link UltimatumUnitDamage} scales unit damage to the share of personnel who left.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UltimatumUnitDamageTest {
    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static Person buildPerson(PersonnelStatus status) {
        Person person = mock(Person.class);
        when(person.getStatus()).thenReturn(status);
        return person;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static Unit buildUnit(Entity entity, boolean isPresent, boolean isMothballed, boolean isRefitting) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.isPresent()).thenReturn(isPresent);
        when(unit.isMothballed()).thenReturn(isMothballed);
        when(unit.isRefitting()).thenReturn(isRefitting);
        return unit;
    }

    @Test
    @DisplayName("getDepartedShare counts personnel who left or died")
    void testGetDepartedShare() {
        List<Person> personnel = List.of(buildPerson(PersonnelStatus.ACTIVE),
              buildPerson(PersonnelStatus.ACTIVE),
              buildPerson(PersonnelStatus.DESERTED),
              buildPerson(PersonnelStatus.HOMICIDE));

        assertEquals(0.5, UltimatumUnitDamage.getDepartedShare(personnel), 0.0001);
    }

    @Test
    @DisplayName("getDepartedShare is zero with nobody to begin with")
    void testGetDepartedShareEmpty() {
        assertEquals(0.0, UltimatumUnitDamage.getDepartedShare(List.of()), 0.0001);
    }

    @Test
    @DisplayName("The damage level rises with the share of personnel who left")
    void testGetDamageLevel() {
        assertEquals(PreExistingDamageLevel.NONE, UltimatumUnitDamage.getDamageLevel(0.0));
        assertEquals(PreExistingDamageLevel.LIGHT, UltimatumUnitDamage.getDamageLevel(0.01));
        assertEquals(PreExistingDamageLevel.LIGHT, UltimatumUnitDamage.getDamageLevel(0.1499));
        assertEquals(PreExistingDamageLevel.MODERATE,
              UltimatumUnitDamage.getDamageLevel(UltimatumUnitDamage.MODERATE_DAMAGE_THRESHOLD));
        assertEquals(PreExistingDamageLevel.MODERATE, UltimatumUnitDamage.getDamageLevel(0.3499));
        assertEquals(PreExistingDamageLevel.HEAVY,
              UltimatumUnitDamage.getDamageLevel(UltimatumUnitDamage.HEAVY_DAMAGE_THRESHOLD));
        assertEquals(PreExistingDamageLevel.HEAVY, UltimatumUnitDamage.getDamageLevel(1.0));
    }

    @Test
    @DisplayName("The share of units damaged matches the share of personnel who left, rounded up")
    void testGetUnitsToDamage() {
        assertEquals(0, UltimatumUnitDamage.getUnitsToDamage(10, 0.0));
        assertEquals(1, UltimatumUnitDamage.getUnitsToDamage(10, 0.01));
        assertEquals(3, UltimatumUnitDamage.getUnitsToDamage(10, 0.25));
        assertEquals(10, UltimatumUnitDamage.getUnitsToDamage(10, 1.0));
        assertEquals(10, UltimatumUnitDamage.getUnitsToDamage(10, 1.5));
        assertEquals(0, UltimatumUnitDamage.getUnitsToDamage(0, 0.5));
    }

    @Test
    @DisplayName("Only present, in-service units of a supported type can be damaged")
    void testIsEligible() {
        BipedMek mek = mock(BipedMek.class);

        assertTrue(UltimatumUnitDamage.isEligible(buildUnit(mek, true, false, false)));
        assertFalse(UltimatumUnitDamage.isEligible(buildUnit(mek, false, false, false)));
        assertFalse(UltimatumUnitDamage.isEligible(buildUnit(mek, true, true, false)));
        assertFalse(UltimatumUnitDamage.isEligible(buildUnit(mek, true, false, true)));
        assertFalse(UltimatumUnitDamage.isEligible(buildUnit(mock(Infantry.class), true, false, false)));
        assertFalse(UltimatumUnitDamage.isEligible(buildUnit(null, true, false, false)));
    }

    @Test
    @DisplayName("A destroyed unit cannot be damaged")
    void testDestroyedUnitIsNotEligible() {
        BipedMek mek = mock(BipedMek.class);
        when(mek.isDestroyed()).thenReturn(true);

        assertFalse(UltimatumUnitDamage.isEligible(buildUnit(mek, true, false, false)));
    }

    @Test
    @DisplayName("Invalid shares never cause damage: negative or NaN mean none, above 1 damages every unit")
    void testInvalidSharesAreSafe() {
        assertEquals(PreExistingDamageLevel.NONE, UltimatumUnitDamage.getDamageLevel(Double.NaN));
        assertEquals(PreExistingDamageLevel.NONE, UltimatumUnitDamage.getDamageLevel(-0.5));
        assertEquals(0, UltimatumUnitDamage.getUnitsToDamage(10, Double.NaN));
        assertEquals(0, UltimatumUnitDamage.getUnitsToDamage(10, -0.5));
        assertEquals(0, UltimatumUnitDamage.getUnitsToDamage(-3, 0.5));
    }

    @Test
    @DisplayName("Everyone leaving damages every unit heavily")
    void testEveryoneLeaving() {
        List<Person> personnel = List.of(buildPerson(PersonnelStatus.DESERTED), buildPerson(PersonnelStatus.HOMICIDE));
        double share = UltimatumUnitDamage.getDepartedShare(personnel);

        assertEquals(1.0, share, 0.0001);
        assertEquals(PreExistingDamageLevel.HEAVY, UltimatumUnitDamage.getDamageLevel(share));
        assertEquals(7, UltimatumUnitDamage.getUnitsToDamage(7, share));
    }

    @Test
    @DisplayName("A single departure out of many still damages one unit, lightly")
    void testSingleDepartureAmongMany() {
        List<Person> personnel = new ArrayList<>();
        personnel.add(buildPerson(PersonnelStatus.DESERTED));
        for (int index = 0; index < 99; index++) {
            personnel.add(buildPerson(PersonnelStatus.ACTIVE));
        }
        double share = UltimatumUnitDamage.getDepartedShare(personnel);

        assertEquals(PreExistingDamageLevel.LIGHT, UltimatumUnitDamage.getDamageLevel(share));
        assertEquals(1, UltimatumUnitDamage.getUnitsToDamage(40, share));
    }
}

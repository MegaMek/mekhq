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
package mekhq.campaign.personnel.turnoverAndRetention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType;

/**
 * Tests for {@link RetirementDefectionTracker}, specifically verifying that
 * prosthetic injuries do not contribute to turnover penalties or medical
 * discharge determinations.
 *
 * @see <a href="https://github.com/MegaMek/mekhq/issues/8631">#8631</a>
 */
class RetirementDefectionTrackerTest {

    /**
     * Test InjuryType that allows setting the subtype for testing.
     */
    private static class TestInjuryType extends InjuryType {
        public TestInjuryType(InjurySubType subType, boolean isPermanent) {
            this.simpleName = "test_" + subType.name();
            this.recoveryTime = 10;
            this.fluffText = "Test injury";
            this.level = InjuryLevel.MINOR;
            this.allowedLocations = EnumSet.of(BodyLocation.GENERIC);
            this.injurySubType = subType;
            this.permanent = isPermanent;
        }
    }

    private Injury createInjury(InjurySubType subType, boolean permanent) {
        InjuryType type = new TestInjuryType(subType, permanent);
        return new Injury(10, "Test", BodyLocation.GENERIC, type, 1, LocalDate.now(), permanent);
    }

    private Person createPersonWithInjuries(Injury... injuries) {
        Person person = mock(Person.class);
        List<Injury> injuryList = List.of(injuries);

        when(person.getInjuries()).thenReturn(injuryList);

        return person;
    }

    // region Injury Modifier (Turnover Penalty)

    @Test
    void prostheticInjuriesAreExcludedFromTurnoverInjuryCount() {
        Person person = createPersonWithInjuries(
              createInjury(InjurySubType.NORMAL, true),
              createInjury(InjurySubType.PROSTHETIC_GENERIC, true),
              createInjury(InjurySubType.PROSTHETIC_MYOMER, true),
              createInjury(InjurySubType.IMPLANT_GENERIC, true),
              createInjury(InjurySubType.IMPLANT_VDNI, true));

        assertEquals(1, RetirementDefectionTracker.getInjuryTurnoverModifier(person),
              "Only the normal permanent injury should count toward turnover penalty");
    }

    @Test
    void onlyProstheticInjuriesResultsInZeroTurnoverPenalty() {
        Person person = createPersonWithInjuries(
              createInjury(InjurySubType.PROSTHETIC_GENERIC, true),
              createInjury(InjurySubType.PROSTHETIC_MYOMER, true),
              createInjury(InjurySubType.IMPLANT_VDNI, true));

        assertEquals(0, RetirementDefectionTracker.getInjuryTurnoverModifier(person),
              "A person with only prosthetics should have zero turnover injury penalty");
    }

    @Test
    void nonPermanentNormalInjuriesAreAlsoExcluded() {
        Person person = createPersonWithInjuries(
              createInjury(InjurySubType.NORMAL, false),
              createInjury(InjurySubType.NORMAL, true));

        assertEquals(1, RetirementDefectionTracker.getInjuryTurnoverModifier(person),
              "Only permanent non-prosthetic injuries should count");
    }

    // endregion Injury Modifier (Turnover Penalty)

    // region Medical Discharge (Payout)

    @Test
    void prostheticOnlyPersonIsNotMedicallyDischarged() {
        Person person = createPersonWithInjuries(
              createInjury(InjurySubType.PROSTHETIC_GENERIC, true),
              createInjury(InjurySubType.IMPLANT_GENERIC, true));

        assertFalse(RetirementDefectionTracker.hasMedicalDischargeInjuries(person),
              "A person with only prosthetics should not be medically discharged");
    }

    @Test
    void personWithRealPermanentInjuryIsMedicallyDischarged() {
        Person person = createPersonWithInjuries(
              createInjury(InjurySubType.PROSTHETIC_GENERIC, true),
              createInjury(InjurySubType.NORMAL, true));

        assertTrue(RetirementDefectionTracker.hasMedicalDischargeInjuries(person),
              "A person with a real permanent injury should still be medically discharged");
    }

    // endregion Medical Discharge (Payout)
}

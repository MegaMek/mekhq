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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.TargetRollModifier;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.ReportingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

class ActionCheckTest {

    private static class ConcreteActionCheck extends ActionCheck<ConcreteActionCheck> {
        private final boolean countUp;
        private final boolean naturalAptitude;
        private final String actionName;

        ConcreteActionCheck(Person person, TargetRoll targetNumber, boolean countUp,
              boolean naturalAptitude, String actionName) {
            super(person, targetNumber);
            this.countUp = countUp;
            this.naturalAptitude = naturalAptitude;
            this.actionName = actionName;
        }

        @Override
        protected ConcreteActionCheck getThis() {
            return this;
        }

        @Override
        protected boolean isCountUp() {
            return countUp;
        }

        @Override
        protected boolean hasNaturalAptitude() {
            return naturalAptitude;
        }

        @Override
        protected String getActionName() {
            return actionName;
        }
    }

    private ActionCheckResult resolveWithFixedRoll(ActionCheck<?> check, boolean useEdge,
          Integer firstRoll, Integer... otherRolls) {
        try (MockedStatic<Compute> utils = mockStatic(Compute.class)) {
            utils.when(Compute::d6).thenReturn(firstRoll, otherRolls);
            return check.resolve(useEdge, null);
        }
    }

    @Test
    void testGetTargetNumber() {
        TargetRoll target = new TargetRoll(6, "");
        ConcreteActionCheck check = new ConcreteActionCheck(mock(Person.class), target, false, false, "Action");

        assertEquals(target, check.getTargetNumber());
    }

    @Test
    void testWithExternalModifiers_AppliesModifiers() {
        TargetRoll target = new TargetRoll(5, "");
        ConcreteActionCheck check = new ConcreteActionCheck(mock(Person.class), target, false, false, "Action");
        check.withExternalModifiers(List.of(new TargetRollModifier(2, "Penalty"), new TargetRollModifier(-1, "Bonus")));

        assertEquals(6, check.getTargetNumber().getValue());
    }

    @Test
    void testWithMiscModifier_NotCountUp() {
        Person person = new Person("F", "L", null, "Faction");
        TargetRoll target = new TargetRoll(5, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        check.withMiscModifier(2);

        assertEquals(7, check.getTargetNumber().getValue());
    }

    @Test
    void testWithMiscModifier_CountUp() {
        TargetRoll target = new TargetRoll(5, "");
        ConcreteActionCheck check = new ConcreteActionCheck(mock(Person.class), target, true, false, "Action");
        check.withMiscModifier(2);

        assertEquals(3, check.getTargetNumber().getValue());
    }

    @Test
    void testResolve_SuccessFirstRoll() {
        Person person = new Person("F", "L", null, "Faction");
        TargetRoll target = new TargetRoll(7, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        ActionCheckResult result = resolveWithFixedRoll(check, true, 2, 6);

        assertTrue(result.isSuccess());
        assertFalse(result.hasUsedEdge());
        assertEquals(8, result.getRollResult());
        assertEquals("<a href='PERSON:link'>F L</a> <span color=\"positive\"><b>Passed</b></span> his <b>Action</b> " +
                           "check with a roll of <b>8</b> vs. a target number of <b>7</b>.",
              result.getReport(false).replace(person.getId().toString(), "link")
                    .replace(ReportingUtilities.getPositiveColor(), "positive"));
    }

    @Test
    void testResolve_FailureNoEdgeEnabled() {
        Person person = new Person("F", "L", null, "Faction");
        TargetRoll target = new TargetRoll(7, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        ActionCheckResult result = resolveWithFixedRoll(check, false, 1, 4);

        assertFalse(result.isSuccess());
        assertFalse(result.hasUsedEdge());
        assertEquals(5, result.getRollResult());
        assertEquals("<a href='PERSON:link'>F L</a> <span color=\"negative\"><b>Failed</b></span> his <b>Action</b> " +
                           "check with a roll of <b>5</b> vs. a target number of <b>7</b>.",
              result.getReport(false).replace(person.getId().toString(), "link")
                    .replace(ReportingUtilities.getNegativeColor(), "negative"));
    }

    @Test
    void testResolve_FailureNoAvailableEdge() {
        Person person = new Person("F", "L", null, "Faction");
        person.setCurrentEdge(0);
        TargetRoll target = new TargetRoll(7, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        ActionCheckResult result = resolveWithFixedRoll(check, true, 3, 2, 6, 6);

        assertFalse(result.isSuccess());
        assertFalse(result.hasUsedEdge());
        assertEquals("<a href='PERSON:link'>F L</a> <span color=\"negative\"><b>Failed</b></span> his <b>Action</b> " +
                           "check with a roll of <b>5</b> vs. a target number of <b>7</b>.",
              result.getReport(false).replace(person.getId().toString(), "link")
                    .replace(ReportingUtilities.getNegativeColor(), "negative"));
    }

    @Test
    void testResolve_FailureTargetGreaterThanTwelve() {
        Person person = new Person("F", "L", null, "Faction");
        TargetRoll target = new TargetRoll(13, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        ActionCheckResult result = resolveWithFixedRoll(check, true, 6, 6);

        assertFalse(result.isSuccess());
        assertFalse(result.hasUsedEdge());
    }

    @Test
    void testResolve_UsesEdgeAndSucceeds() {
        Person person = mock(Person.class);
        when(person.getHyperlinkedFullTitle()).thenReturn("Person");
        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.getCurrentEdge()).thenReturn(1);
        TargetRoll target = new TargetRoll(7, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        ActionCheckResult result = resolveWithFixedRoll(check, true, 4, 1, 3, 6);

        assertTrue(result.isSuccess());
        assertTrue(result.hasUsedEdge());
        assertEquals(9, result.getRollResult());
        verify(person).spendEdge();
        assertEquals("Person <span color=\"positive\"><b>Passed</b></span> her <b>Action</b> check with a roll of " +
                           "<b>9</b> vs. a target number of <b>7</b>. Used a point of <b>Edge</b>.",
              result.getReport(false).replace(ReportingUtilities.getPositiveColor(), "positive"));
    }

    @Test
    void testResolve_UsesEdgeAndFails() {
        Person person = mock(Person.class);
        when(person.getHyperlinkedFullTitle()).thenReturn("Person");
        when(person.getGender()).thenReturn(Gender.MALE);
        when(person.getCurrentEdge()).thenReturn(1);

        TargetRoll target = new TargetRoll(7, "");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, false, "Action");
        ActionCheckResult result = resolveWithFixedRoll(check, true, 2, 3, 4, 2);

        assertFalse(result.isSuccess());
        assertTrue(result.hasUsedEdge());
        assertEquals(6, result.getRollResult());
        verify(person).spendEdge();
        assertEquals("Person <span color=\"negative\"><b>Failed</b></span> his <b>Action</b> check with a roll of " +
                           "<b>6</b> vs. a target number of <b>7</b>. Used a point of <b>Edge</b>.",
              result.getReport(false).replace(ReportingUtilities.getNegativeColor(), "negative"));
    }

    @ParameterizedTest
    @CsvSource({ "true", "false" })
    void testResolve_PassesNaturalAptitudeToRoll(boolean naturalAptitude) {
        Person person = new Person("F", "L", null, "Faction");
        TargetRoll target = new TargetRoll(7, "Base");
        ConcreteActionCheck check = new ConcreteActionCheck(person, target, false, naturalAptitude, "Action");

        try (MockedStatic<Compute> utils = mockStatic(Compute.class)) {
            utils.when(Compute::d6).thenReturn(3, 5, 2);
            ActionCheckResult result = check.resolve(false, null);
            assertEquals(8, result.getRollResult());
            utils.verify(Compute::d6, times(naturalAptitude ? 3 : 2));
        }
    }
}

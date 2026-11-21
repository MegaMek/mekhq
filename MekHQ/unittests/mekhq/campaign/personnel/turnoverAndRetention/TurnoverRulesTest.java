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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the turnover rules in {@link RetirementDefectionTracker}: the family and shares modifiers, how a
 * departing character leaves, and when a departure can end in divorce.
 */
class TurnoverRulesTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 1, 1);

    // region Family Modifier

    private static Person mockRelative(PersonnelRole role, PersonnelStatus status, boolean isChild) {
        Person relative = mock(Person.class);
        when(relative.getPrimaryRole()).thenReturn(role);
        when(relative.getStatus()).thenReturn(status);
        when(relative.isChild(any())).thenReturn(isChild);
        return relative;
    }

    private static Person mockPerson(Person spouse, List<Person> children) {
        Genealogy genealogy = mock(Genealogy.class);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(genealogy.getChildren()).thenReturn(children);

        Person person = mock(Person.class);
        when(person.getGenealogy()).thenReturn(genealogy);
        return person;
    }

    @Test
    void testGetFamilyModifier_spouseAndAdultChildStack() {
        Person spouse = mockRelative(PersonnelRole.MEKWARRIOR, PersonnelStatus.ACTIVE, false);
        Person child = mockRelative(PersonnelRole.MEKWARRIOR, PersonnelStatus.ACTIVE, false);

        assertEquals(-2, RetirementDefectionTracker.getFamilyModifier(mockPerson(spouse, List.of(child)), TODAY));
    }

    @Test
    void testGetFamilyModifier_civilianSpouseDoesNotBlockChildBonus() {
        Person spouse = mockRelative(PersonnelRole.DEPENDENT, PersonnelStatus.ACTIVE, false);
        Person child = mockRelative(PersonnelRole.MEKWARRIOR, PersonnelStatus.ACTIVE, false);

        assertEquals(-1, RetirementDefectionTracker.getFamilyModifier(mockPerson(spouse, List.of(child)), TODAY));
    }

    @Test
    void testGetFamilyModifier_youngOrDepartedChildrenDoNotCount() {
        Person youngChild = mockRelative(PersonnelRole.NONE, PersonnelStatus.ACTIVE, true);
        Person departedChild = mockRelative(PersonnelRole.MEKWARRIOR, PersonnelStatus.RESIGNED, false);

        assertEquals(0, RetirementDefectionTracker.getFamilyModifier(mockPerson(null,
              List.of(youngChild, departedChild)), TODAY));
    }

    // endregion Family Modifier

    // region Shares Modifier

    private static AbstractContract mockContract(int sharesPercent) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getSharesPercent()).thenReturn(sharesPercent);
        return contract;
    }

    @Test
    void testGetSharesModifier_usesEndingContract() {
        assertEquals(-3, RetirementDefectionTracker.getSharesModifier(mockContract(50), new ArrayList<>()));
    }

    @Test
    void testGetSharesModifier_scheduledCheckUsesBestActiveContract() {
        List<AbstractContract> activeContracts = List.of(mockContract(30), mockContract(60), mockContract(40));

        assertEquals(-4, RetirementDefectionTracker.getSharesModifier(null, activeContracts));
    }

    @Test
    void testGetSharesModifier_noContractOrLowSharesGivesZero() {
        assertEquals(0, RetirementDefectionTracker.getSharesModifier(null, new ArrayList<>()));
        assertEquals(0, RetirementDefectionTracker.getSharesModifier(mockContract(20), new ArrayList<>()));
    }

    // endregion Shares Modifier

    // region Departure Status

    @ParameterizedTest
    @CsvSource({
          // medical, breakingContract, activeContract, retirementAge, defectionRoll, expected
          "true, true, true, false, true, RETIRED",
          "true, false, false, false, false, RETIRED",
          "false, true, false, true, false, RESIGNED",
          "false, true, true, false, true, DEFECTED",
          "false, false, false, true, false, RETIRED",
          "false, false, true, false, false, RESIGNED"
    })
    void testGetDepartureStatus(boolean hasMedicalDischargeInjuries, boolean isBreakingContract,
          boolean hasActiveContract, boolean isRetirementAge, boolean isDefectionRollSuccessful,
          PersonnelStatus expected) {
        assertEquals(expected, RetirementDefectionTracker.getDepartureStatus(hasMedicalDischargeInjuries,
              isBreakingContract, hasActiveContract, isRetirementAge, isDefectionRollSuccessful));
    }

    @Test
    void testGetDepartureStatus_breakingContractMidDeploymentStaysWithoutDefecting() {
        assertNull(RetirementDefectionTracker.getDepartureStatus(false, true, true, false, false));
    }

    // endregion Departure Status

    // region Departure Divorce

    @ParameterizedTest
    @CsvSource({
          "RESIGNED, true",
          "DEFECTED, true",
          "SACKED, true",
          "RETIRED, false",
          "ACTIVE, false",
          "KIA, false"
    })
    void testIsDepartureDivorceCheckRequired(PersonnelStatus status, boolean expected) {
        assertEquals(expected, RetirementDefectionTracker.isDepartureDivorceCheckRequired(status));
    }

    // endregion Departure Divorce
}

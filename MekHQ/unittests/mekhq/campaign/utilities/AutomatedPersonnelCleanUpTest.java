/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.HumanResources;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

class AutomatedPersonnelCleanUpTest {
    private Campaign testCampaign;
    private final LocalDate today = LocalDate.of(3151, 1, 1);

    @BeforeEach
    void setUp() {
        testCampaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_NoDepartedPersons() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person ineligiblePerson = new Person(testCampaign);
            ineligiblePerson.setStatus(PersonnelStatus.ACTIVE);

            humanResources.importPerson(ineligiblePerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_DeadPersonsButBeforeDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person ineligibleDeadPerson = new Person(testCampaign);
            ineligibleDeadPerson.setStatus(PersonnelStatus.DISEASE);
            ineligibleDeadPerson.setDateOfDeath(today.minusMonths(1));

            humanResources.importPerson(ineligibleDeadPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_DeadPersonsAfterDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleDeadPerson = new Person(testCampaign);
            eligibleDeadPerson.setStatus(PersonnelStatus.DISEASE);
            eligibleDeadPerson.setDateOfDeath(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleDeadPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_RelatedExemption_DeadPersonsAfterDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleDeadPerson = new Person(testCampaign);
            eligibleDeadPerson.setStatus(PersonnelStatus.DISEASE);
            eligibleDeadPerson.setDateOfDeath(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleDeadPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, true);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_UnrelatedExemption_DeadPersonsAfterDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleDeadPerson = new Person(testCampaign);
            eligibleDeadPerson.setStatus(PersonnelStatus.DISEASE);
            eligibleDeadPerson.setDateOfDeath(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleDeadPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, true, false);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_DeadPersonsAfterDateActiveGenealogy() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person personWhoDiedAfterThresholdButActiveGenealogy = new Person(testCampaign);
            personWhoDiedAfterThresholdButActiveGenealogy.setStatus(PersonnelStatus.DISEASE);
            personWhoDiedAfterThresholdButActiveGenealogy.setDateOfDeath(today.minusMonths(1).minusDays(i + 1));

            Genealogy genealogy = personWhoDiedAfterThresholdButActiveGenealogy.getGenealogy();

            Person activeParent = new Person(testCampaign);
            activeParent.setStatus(PersonnelStatus.ACTIVE);
            genealogy.addFamilyMember(FamilialRelationshipType.PARENT, activeParent);

            humanResources.importPerson(personWhoDiedAfterThresholdButActiveGenealogy);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_DeadPersonsAfterDateInactiveGenealogy() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person personWhoDiedAfterThresholdAndInactiveGenealogy = new Person(testCampaign);
            personWhoDiedAfterThresholdAndInactiveGenealogy.setStatus(PersonnelStatus.DISEASE);
            personWhoDiedAfterThresholdAndInactiveGenealogy.setDateOfDeath(today.minusMonths(1).minusDays(i + 1));

            Genealogy genealogy = personWhoDiedAfterThresholdAndInactiveGenealogy.getGenealogy();

            Person activeParent = new Person(testCampaign);
            activeParent.setStatus(PersonnelStatus.DISEASE);
            genealogy.addFamilyMember(FamilialRelationshipType.PARENT, activeParent);

            humanResources.importPerson(personWhoDiedAfterThresholdAndInactiveGenealogy);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_RetiredPersonsButBeforeDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person ineligibleRetiredPerson = new Person(testCampaign);
            ineligibleRetiredPerson.setStatus(PersonnelStatus.RETIRED);
            ineligibleRetiredPerson.setRetirement(today.minusMonths(1));

            humanResources.importPerson(ineligibleRetiredPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_DepartedPersonsAfterDateButNotRetired() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleRetiredPerson = new Person(testCampaign);
            eligibleRetiredPerson.setStatus(PersonnelStatus.LEFT);
            eligibleRetiredPerson.setRetirement(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleRetiredPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_RetiredPersonsAfterDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleRetiredPerson = new Person(testCampaign);
            eligibleRetiredPerson.setStatus(PersonnelStatus.LEFT);
            eligibleRetiredPerson.setRetirement(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleRetiredPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_RelatedExemption_RetiredPersonsAfterDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleRetiredPerson = new Person(testCampaign);
            eligibleRetiredPerson.setStatus(PersonnelStatus.RETIRED);
            eligibleRetiredPerson.setRetirement(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleRetiredPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, true, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_UnrelatedExemption_RetiredPersonsAfterDate() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleRetiredPerson = new Person(testCampaign);
            eligibleRetiredPerson.setStatus(PersonnelStatus.RETIRED);
            eligibleRetiredPerson.setRetirement(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleRetiredPerson);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, true);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_RetiredPersonsAfterDateActiveGenealogy() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person personWhoRetiredAfterThresholdButActiveGenealogy = new Person(testCampaign);
            personWhoRetiredAfterThresholdButActiveGenealogy.setStatus(PersonnelStatus.RETIRED);
            personWhoRetiredAfterThresholdButActiveGenealogy.setRetirement(today.minusMonths(1).minusDays(i + 1));

            Genealogy genealogy = personWhoRetiredAfterThresholdButActiveGenealogy.getGenealogy();

            Person activeParent = new Person(testCampaign);
            activeParent.setStatus(PersonnelStatus.ACTIVE);
            genealogy.addFamilyMember(FamilialRelationshipType.PARENT, activeParent);

            humanResources.importPerson(personWhoRetiredAfterThresholdButActiveGenealogy);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_RetiredPersonsAfterDateInactiveGenealogy() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person personWhoRetiredAfterThresholdWithInActiveGenealogy = new Person(testCampaign);
            personWhoRetiredAfterThresholdWithInActiveGenealogy.setStatus(PersonnelStatus.RETIRED);
            personWhoRetiredAfterThresholdWithInActiveGenealogy.setRetirement(today.minusMonths(1).minusDays(i + 1));

            Genealogy genealogy = personWhoRetiredAfterThresholdWithInActiveGenealogy.getGenealogy();

            Person activeParent = new Person(testCampaign);
            activeParent.setStatus(PersonnelStatus.DISEASE);
            genealogy.addFamilyMember(FamilialRelationshipType.PARENT, activeParent);

            humanResources.importPerson(personWhoRetiredAfterThresholdWithInActiveGenealogy);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_BackgroundPersonsAfterDate() {
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person eligibleDeadPerson = new Person(testCampaign);
            eligibleDeadPerson.setStatus(PersonnelStatus.BACKGROUND_CHARACTER);
            eligibleDeadPerson.setDateOfDeath(today.minusMonths(1).minusDays(i + 1));

            humanResources.importPerson(eligibleDeadPerson);
        }

        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        int expected = 10;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }

    @Test
    void testGetPersonnelToCleanUp_NoExemptions_BackgroundPersonsAfterDateActiveGenealogy() {
        // Setup
        HumanResources humanResources = testCampaign.getHumanResources();
        for (int i = 0; i < 10; i++) {
            Person personWhoRetiredAfterThresholdButActiveGenealogy = new Person(testCampaign);
            personWhoRetiredAfterThresholdButActiveGenealogy.setStatus(PersonnelStatus.BACKGROUND_CHARACTER);
            personWhoRetiredAfterThresholdButActiveGenealogy.setRetirement(today.minusMonths(1).minusDays(i + 1));

            Genealogy genealogy = personWhoRetiredAfterThresholdButActiveGenealogy.getGenealogy();

            Person activeParent = new Person(testCampaign);
            activeParent.setStatus(PersonnelStatus.ACTIVE);
            genealogy.addFamilyMember(FamilialRelationshipType.PARENT, activeParent);

            humanResources.importPerson(personWhoRetiredAfterThresholdButActiveGenealogy);
        }

        // Act
        AutomatedPersonnelCleanUp cleanUp = new AutomatedPersonnelCleanUp(humanResources, today, false, false);

        // Assert
        int expected = 0;
        int actual = cleanUp.getPersonnelToCleanUp().size();

        assertEquals(expected, actual);
    }
}

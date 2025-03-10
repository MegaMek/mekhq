/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import megamek.common.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.universe.Systems;
import mekhq.gui.dialog.nagDialogs.UntreatedPersonnelNagDialog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.campaignHasUntreatedInjuries;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class contains test methods for the {@link UntreatedPersonnelNagDialog} class.
 * It tests the different combinations of untreated personnel and verifies the behavior of the
 * {@code isUntreatedInjury()} method.
 */
class UntreatedPersonnelNagLogicTest {
    Campaign campaign;
    Person person;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     */
    @BeforeAll
    public static void setup() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        Ranks.initializeRankSystems();
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            MMLogger.create(UntreatedPersonnelNagLogicTest.class).error("", ex);
        }
    }

    /**
     * Initializes the campaign and creates a new person with the specified personnel role.
     * This person is assigned one hit.
     */
    @BeforeEach
    public void init() {
        campaign = new Campaign();
        person = campaign.newPerson(PersonnelRole.MEKWARRIOR);
        person.setHits(1);
    }

    // In the following tests the isUntreatedInjury() method is called, and its response is checked
    // against expected behavior

    @Test
    public void isUntreatedInjuryIncludesNonPrisonersTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.FREE, false);
        campaign.importPerson(person);
        assertTrue(campaignHasUntreatedInjuries(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesPrisonersTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.PRISONER, false);
        campaign.importPerson(person);
        assertFalse(campaignHasUntreatedInjuries(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesPrisonerDefectorsTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.PRISONER_DEFECTOR, false);
        campaign.importPerson(person);
        assertFalse(campaignHasUntreatedInjuries(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesBondsmenTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.BONDSMAN, false);
        campaign.importPerson(person);
        assertTrue(campaignHasUntreatedInjuries(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesInactivePersonnelTest() {
        person.setStatus(PersonnelStatus.AWOL);
        campaign.importPerson(person);
        assertFalse(campaignHasUntreatedInjuries(campaign));
    }
}

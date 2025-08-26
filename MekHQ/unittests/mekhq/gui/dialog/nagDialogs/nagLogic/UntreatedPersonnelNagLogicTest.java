/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.campaign.personnel.enums.PersonnelRole.DOCTOR;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.campaignHasUntreatedInjuries;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Systems;
import mekhq.gui.dialog.nagDialogs.UntreatedPersonnelNagDialog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class contains test methods for the {@link UntreatedPersonnelNagDialog} class. It tests the different
 * combinations of untreated personnel and verifies the behavior of the {@code isUntreatedInjury()} method.
 */
class UntreatedPersonnelNagLogicTest {
    Campaign campaign;
    Person injuredPerson;
    Person uninjuredPerson;
    Person doctor;

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
     * Initializes the campaign and creates a new person with the specified personnel role. This person is assigned one
     * hit.
     */
    @BeforeEach
    public void init() {
        campaign = mock(Campaign.class);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        injuredPerson = new Person(campaign);
        injuredPerson.setHits(1);
        uninjuredPerson = new Person(campaign);
        doctor = new Person(campaign);
        doctor.addSkill(S_SURGERY, 5, 0);
        doctor.setPrimaryRole(campaign, DOCTOR);
    }

    // In the following tests the isUntreatedInjury() method is called, and its response is checked
    // against expected behavior

    @Test
    public void isUntreatedInjuryTest() {
        assertTrue(campaignHasUntreatedInjuries(List.of(injuredPerson), 0));
    }

    @Test
    public void isNoUntreatedInjuryTest() {
        assertFalse(campaignHasUntreatedInjuries(List.of(uninjuredPerson), 0));
    }

    @Test
    public void isAboveDoctorThresholdTest() {
        MekHQ.getMHQOptions().setNewDayOptimizeMedicalAssignments(true);
        assertTrue(campaignHasUntreatedInjuries(List.of(injuredPerson), 0));
    }

    @Test
    public void isBelowDoctorThresholdTest() {
        MekHQ.getMHQOptions().setNewDayOptimizeMedicalAssignments(true);
        assertFalse(campaignHasUntreatedInjuries(List.of(injuredPerson, doctor), 25));
    }
}

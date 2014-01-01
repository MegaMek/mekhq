/*
 * FieldManualMercRevMrbcRating.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.rating;

import junit.framework.Assert;
import junit.framework.TestCase;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 9/23/2013
 */
@RunWith(JUnit4.class)
public class FieldManualMercRevDragoonsRatingTest {

    private Campaign mockCampaign = Mockito.mock(Campaign.class);

    private ArrayList<Person> mockPersonnnelList = new ArrayList<Person>();

    private Person mockDoctor = Mockito.mock(Person.class);
    private Person mockTech = Mockito.mock(Person.class);

    private Skill mockDoctorSkillRegular = Mockito.mock(Skill.class);
    private Skill mockDoctorSkillGreen = Mockito.mock(Skill.class);
    private Skill mockMedicSkill = Mockito.mock(Skill.class);
    private Skill mockMechTechSkillVeteran = Mockito.mock(Skill.class);
    private Skill mockMechTechSkillRegular = Mockito.mock(Skill.class);
    private Skill mockAstechSkill = Mockito.mock(Skill.class);

    @Before
    public void setUp() {
        // Set up the doctor.
        Mockito.when(mockDoctorSkillRegular.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);
        Mockito.when(mockDoctorSkillGreen.getExperienceLevel()).thenReturn(SkillType.EXP_GREEN);
        Mockito.when(mockDoctor.getPrimaryRole()).thenReturn(Person.T_DOCTOR);
        Mockito.when(mockDoctor.isDoctor()).thenReturn(true);
        Mockito.when(mockDoctor.isActive()).thenReturn(true);
        Mockito.when(mockDoctor.isDeployed()).thenReturn(false);
        Mockito.when(mockDoctor.getSkill(Mockito.eq(SkillType.S_DOCTOR))).thenReturn(mockDoctorSkillRegular);
        Mockito.when(mockDoctor.hasSkill(Mockito.eq(SkillType.S_DOCTOR))).thenReturn(true);
        Mockito.when(mockDoctor.getRankOrder()).thenReturn(5);

        // Set up the tech.
        Mockito.when(mockMechTechSkillVeteran.getExperienceLevel()).thenReturn(SkillType.EXP_VETERAN);
        Mockito.when(mockMechTechSkillRegular.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);
        Mockito.when(mockTech.getPrimaryRole()).thenReturn(Person.T_MECH_TECH);
        Mockito.when(mockTech.isTech()).thenReturn(true);
        Mockito.when(mockTech.isActive()).thenReturn(true);
        Mockito.when(mockTech.isDeployed()).thenReturn(false);
        Mockito.when(mockTech.getSkill(Mockito.eq(SkillType.S_TECH_MECH))).thenReturn(mockMechTechSkillVeteran);
        Mockito.when(mockTech.hasSkill(Mockito.eq(SkillType.S_TECH_MECH))).thenReturn(true);
        Mockito.when(mockTech.getRankOrder()).thenReturn(4);

        Mockito.when(mockMedicSkill.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);
        Mockito.when(mockAstechSkill.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);

        mockPersonnnelList.add(mockDoctor);
        mockPersonnnelList.add(mockTech);

        Mockito.when(mockCampaign.getPersonnel()).thenReturn(mockPersonnnelList);
        Mockito.when(mockCampaign.getNumberMedics()).thenCallRealMethod();
        Mockito.when(mockCampaign.getNumberAstechs()).thenCallRealMethod();
        Mockito.when(mockCampaign.getNumberPrimaryAstechs()).thenCallRealMethod();
        Mockito.when(mockCampaign.getNumberSecondaryAstechs()).thenCallRealMethod();
    }

    @Test
    public void testGetMedSupportAvailable() {

        // Test having 1 regular doctor with 4 temp medics.
        // Expected available support should be:
        // Regular Doctor = 40 hours.
        // + 4 Medics = 20 * 4 = 80 hours.
        // Total = 120 hours.
        FieldManualMercRevDragoonsRating testFieldManuMercRevDragoonsRating =
                new FieldManualMercRevDragoonsRating(mockCampaign);
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        int expectedHours = 120;
        Mockito.when(mockCampaign.getMedicPool()).thenReturn(4);
        TestCase.assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getMedicalSupportAvailable());

        // Add a mechwarrior who doubles as a back-up medic of Green skill.  This should add another 15 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockMechwarrior = Mockito.mock(Person.class);
        Mockito.when(mockMechwarrior.getPrimaryRole()).thenReturn(Person.T_MECHWARRIOR);
        Mockito.when(mockMechwarrior.getSecondaryRole()).thenReturn(Person.T_DOCTOR);
        Mockito.when(mockMechwarrior.isDoctor()).thenReturn(true);
        Mockito.when(mockMechwarrior.isActive()).thenReturn(true);
        Mockito.when(mockMechwarrior.isDeployed()).thenReturn(false);
        Mockito.when(mockMechwarrior.getSkill(Mockito.eq(SkillType.S_DOCTOR))).thenReturn(mockDoctorSkillGreen);
        Mockito.when(mockMechwarrior.hasSkill(Mockito.eq(SkillType.S_DOCTOR))).thenReturn(true);
        mockPersonnnelList.add(mockMechwarrior);
        expectedHours += 15;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        TestCase.assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getMedicalSupportAvailable());

        // Hire a full-time Medic.  This should add another 20 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockMedic = Mockito.mock(Person.class);
        Mockito.when(mockMedic.getPrimaryRole()).thenReturn(Person.T_MEDIC);
        Mockito.when(mockMedic.isDoctor()).thenReturn(false);
        Mockito.when(mockMedic.isActive()).thenReturn(true);
        Mockito.when(mockMedic.isDeployed()).thenReturn(false);
        Mockito.when(mockMedic.getSkill(Mockito.eq(SkillType.S_MEDTECH))).thenReturn(mockMedicSkill);
        Mockito.when(mockMedic.hasSkill(Mockito.eq(SkillType.S_MEDTECH))).thenReturn(true);
        mockPersonnnelList.add(mockMedic);
        expectedHours += 20;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        TestCase.assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getMedicalSupportAvailable());
    }

    @Test
    public void testGetTechSupportAvailable() {

        // Test having 1 veteran mech tech with 6 temp astechs.
        // Expected available support should be:
        // Regular Tech = 45 hours.
        // + 6 Astechs = 20 * 4 = 120 hours.
        // Total = 165 hours.
        FieldManualMercRevDragoonsRating testFieldManuMercRevDragoonsRating =
                new FieldManualMercRevDragoonsRating(mockCampaign);
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        int expectedHours = 165;
        Mockito.when(mockCampaign.getAstechPool()).thenReturn(6);
        TestCase.assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getTechSupportAvailable());

        // Add a mechwarrior who doubles as a back-up tech of Regular skill.  This should add another 20 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockMechwarrior = Mockito.mock(Person.class);
        Mockito.when(mockMechwarrior.getPrimaryRole()).thenReturn(Person.T_MECHWARRIOR);
        Mockito.when(mockMechwarrior.getSecondaryRole()).thenReturn(Person.T_MECH_TECH);
        Mockito.when(mockMechwarrior.isTech()).thenReturn(true);
        Mockito.when(mockMechwarrior.isTechSecondary()).thenReturn(true);
        Mockito.when(mockMechwarrior.isActive()).thenReturn(true);
        Mockito.when(mockMechwarrior.isDeployed()).thenReturn(false);
        Mockito.when(mockMechwarrior.getSkill(Mockito.eq(SkillType.S_TECH_MECH))).thenReturn(mockMechTechSkillRegular);
        Mockito.when(mockMechwarrior.hasSkill(Mockito.eq(SkillType.S_TECH_MECH))).thenReturn(true);
        mockPersonnnelList.add(mockMechwarrior);
        expectedHours += 20;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        TestCase.assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getTechSupportAvailable());

        // Hire a full-time Astech.  This should add another 20 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockAstech = Mockito.mock(Person.class);
        Mockito.when(mockAstech.getPrimaryRole()).thenReturn(Person.T_ASTECH);
        Mockito.when(mockAstech.isDoctor()).thenReturn(false);
        Mockito.when(mockAstech.isTech()).thenReturn(false);
        Mockito.when(mockAstech.isActive()).thenReturn(true);
        Mockito.when(mockAstech.isDeployed()).thenReturn(false);
        Mockito.when(mockAstech.getSkill(Mockito.eq(SkillType.S_ASTECH))).thenReturn(mockAstechSkill);
        Mockito.when(mockAstech.hasSkill(Mockito.eq(SkillType.S_ASTECH))).thenReturn(true);
        mockPersonnnelList.add(mockAstech);
        expectedHours += 20;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        TestCase.assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getTechSupportAvailable());
    }

    @Test
    public void testGetCommander() {

        // Test a campaign with the commander flagged.
        Person expectedCommander = Mockito.mock(Person.class);
        Mockito.when(mockCampaign.getFlaggedCommander()).thenReturn(expectedCommander);
        FieldManualMercRevDragoonsRating testRating = Mockito.spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        TestCase.assertEquals(expectedCommander, testRating.getCommander());

        // Test a campaign where the commander is not flagged, but there is a clear highest ranking officer.
        testRating = Mockito.spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        Mockito.when(expectedCommander.getRankOrder()).thenReturn(10);
        Person leftennant = Mockito.mock(Person.class);
        Mockito.when(leftennant.getRankOrder()).thenReturn(5);
        Person leftennant2 = Mockito.mock(Person.class);
        Mockito.when(leftennant2.getRankOrder()).thenReturn(5);
        List<Person> commandList = new ArrayList<Person>(3);
        commandList.add(leftennant);
        commandList.add(expectedCommander);
        commandList.add(leftennant2);
        Mockito.when(mockCampaign.getFlaggedCommander()).thenReturn(null);
        Mockito.doReturn(commandList).when(testRating).getCommanderList();
        TestCase.assertEquals(expectedCommander, testRating.getCommander());

        // Retire the old commander.  Give one leftennant more experience than the other.
        testRating = Mockito.spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        Mockito.when(mockCampaign.getFlaggedCommander()).thenReturn(null);
        Mockito.doReturn(commandList).when(testRating).getCommanderList();
        Mockito.when(expectedCommander.isActive()).thenReturn(false);
        Mockito.when(leftennant.getExperienceLevel(Mockito.anyBoolean())).thenReturn(SkillType.EXP_VETERAN);
        Mockito.when(leftennant.isActive()).thenReturn(true);
        Mockito.when(leftennant2.getExperienceLevel(Mockito.anyBoolean())).thenReturn(SkillType.EXP_REGULAR);
        Mockito.when(leftennant2.isActive()).thenReturn(true);
        TestCase.assertEquals(leftennant, testRating.getCommander());

        // Test a campaign with no flagged commander and where no ranks have been assigned.
        testRating = Mockito.spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        Mockito.when(mockCampaign.getFlaggedCommander()).thenReturn(null);
        Mockito.doReturn(null).when(testRating).getCommanderList();
        TestCase.assertNull(testRating.getCommander());
    }
}

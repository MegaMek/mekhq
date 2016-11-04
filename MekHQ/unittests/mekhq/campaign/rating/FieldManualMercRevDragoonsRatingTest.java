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

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.Bay;
import megamek.common.BipedMech;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Jumpship;
import megamek.common.MechBay;
import megamek.common.Tank;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 9/23/2013
 */
@RunWith(JUnit4.class)
public class FieldManualMercRevDragoonsRatingTest {

    private Campaign mockCampaign = mock(Campaign.class);

    private ArrayList<Person> mockPersonnnelList = new ArrayList<>();

    private Person mockDoctor = mock(Person.class);
    private Person mockTech = mock(Person.class);

    private Skill mockDoctorSkillRegular = mock(Skill.class);
    private Skill mockDoctorSkillGreen = mock(Skill.class);
    private Skill mockMedicSkill = mock(Skill.class);
    private Skill mockMechTechSkillVeteran = mock(Skill.class);
    private Skill mockMechTechSkillRegular = mock(Skill.class);
    private Skill mockAstechSkill = mock(Skill.class);

    @Before
    public void setUp() {
        // Set up the doctor.
        when(mockDoctorSkillRegular.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);
        when(mockDoctorSkillGreen.getExperienceLevel()).thenReturn(SkillType.EXP_GREEN);
        when(mockDoctor.getPrimaryRole()).thenReturn(Person.T_DOCTOR);
        when(mockDoctor.isDoctor()).thenReturn(true);
        when(mockDoctor.isActive()).thenReturn(true);
        when(mockDoctor.isDeployed()).thenReturn(false);
        when(mockDoctor.getSkill(eq(SkillType.S_DOCTOR))).thenReturn(mockDoctorSkillRegular);
        when(mockDoctor.hasSkill(eq(SkillType.S_DOCTOR))).thenReturn(true);
        when(mockDoctor.getRankNumeric()).thenReturn(5);

        // Set up the tech.
        when(mockMechTechSkillVeteran.getExperienceLevel()).thenReturn(SkillType.EXP_VETERAN);
        when(mockMechTechSkillRegular.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);
        when(mockTech.getPrimaryRole()).thenReturn(Person.T_MECH_TECH);
        when(mockTech.isTech()).thenReturn(true);
        when(mockTech.isActive()).thenReturn(true);
        when(mockTech.isDeployed()).thenReturn(false);
        when(mockTech.getSkill(eq(SkillType.S_TECH_MECH))).thenReturn(mockMechTechSkillVeteran);
        when(mockTech.hasSkill(eq(SkillType.S_TECH_MECH))).thenReturn(true);
        when(mockTech.getRankNumeric()).thenReturn(4);

        when(mockMedicSkill.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);
        when(mockAstechSkill.getExperienceLevel()).thenReturn(SkillType.EXP_REGULAR);

        mockPersonnnelList.add(mockDoctor);
        mockPersonnnelList.add(mockTech);

        when(mockCampaign.getPersonnel()).thenReturn(mockPersonnnelList);
        when(mockCampaign.getNumberMedics()).thenCallRealMethod();
        when(mockCampaign.getNumberAstechs()).thenCallRealMethod();
        when(mockCampaign.getNumberPrimaryAstechs()).thenCallRealMethod();
        when(mockCampaign.getNumberSecondaryAstechs()).thenCallRealMethod();

        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.useQuirks()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
    }

    @Test
    public void testInitValues() {
        FieldManualMercRevDragoonsRating spyRating = spy(new FieldManualMercRevDragoonsRating(mockCampaign));

        Unit mockWasp = mock(Unit.class);
        Entity mockWaspE = mock(BipedMech.class);
        doReturn(EntityMovementMode.BIPED).when(mockWaspE).getMovementMode();
        doReturn(mockWaspE).when(mockWasp).getEntity();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockWaspE).getTechLevel();
        doReturn(20.0).when(mockWaspE).getWeight();
        Person waspPilot = mock(Person.class);
        mockPersonnnelList.add(waspPilot);
        doReturn(waspPilot).when(mockWasp).getCommander();
        Crew waspCrew = mock(Crew.class);
        doReturn(4).when(waspCrew).getGunnery();
        doReturn(5).when(waspCrew).getPiloting();
        doReturn(waspCrew).when(mockWaspE).getCrew();

        Unit mockStinger = mock(Unit.class);
        Entity mockStingerE = mock(BipedMech.class);
        doReturn(EntityMovementMode.BIPED).when(mockStingerE).getMovementMode();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockStingerE).getTechLevel();
        doReturn(20.0).when(mockStingerE).getWeight();
        doReturn(mockStingerE).when(mockStinger).getEntity();
        Person stingerPilot = mock(Person.class);
        mockPersonnnelList.add(stingerPilot);
        doReturn(stingerPilot).when(mockStinger).getCommander();
        Crew stingerCrew = mock(Crew.class);
        doReturn(4).when(stingerCrew).getGunnery();
        doReturn(5).when(stingerCrew).getPiloting();
        doReturn(stingerCrew).when(mockStingerE).getCrew();

        Unit mockGriffin = mock(Unit.class);
        doReturn(true).when(mockGriffin).isMothballed();

        Unit mockThunderbolt = mock(Unit.class);
        Entity mockThunderboltE = mock(BipedMech.class);
        doReturn(EntityMovementMode.BIPED).when(mockThunderboltE).getMovementMode();
        doReturn(TechConstants.T_IS_TW_NON_BOX).when(mockThunderboltE).getTechLevel();
        doReturn(65.0).when(mockThunderboltE).getWeight();
        doReturn(mockThunderboltE).when(mockThunderbolt).getEntity();
        Person thunderboltPilot = mock(Person.class);
        mockPersonnnelList.add(thunderboltPilot);
        doReturn(thunderboltPilot).when(mockThunderbolt).getCommander();
        Crew thunderboltCrew = mock(Crew.class);
        doReturn(thunderboltCrew).when(mockThunderboltE).getCrew();
        doReturn(3).when(thunderboltCrew).getGunnery();
        doReturn(4).when(thunderboltCrew).getPiloting();

        Unit mockShrek = mock(Unit.class);
        Entity mockShrekE = mock(Tank.class);
        doReturn(EntityMovementMode.TRACKED).when(mockShrekE).getMovementMode();
        doReturn(80.0).when(mockShrekE).getWeight();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockShrekE).getTechLevel();
        doReturn(mockShrekE).when(mockShrek).getEntity();
        Person shrekCommander = mock(Person.class);
        mockPersonnnelList.add(shrekCommander);
        doReturn(shrekCommander).when(mockShrek).getCommander();
        Crew shrekCrew = mock(Crew.class);
        doReturn(shrekCrew).when(mockShrekE).getCrew();
        doReturn(4).when(shrekCrew).getGunnery();
        doReturn(4).when(shrekCrew).getPiloting();

        Unit mockShrek2 = mock(Unit.class);
        Entity mockShrek2E = mock(Tank.class);
        doReturn(EntityMovementMode.TRACKED).when(mockShrek2E).getMovementMode();
        doReturn(80.0).when(mockShrek2E).getWeight();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockShrek2E).getTechLevel();
        doReturn(mockShrek2E).when(mockShrek2).getEntity();
        Person shrek2Commander = mock(Person.class);
        mockPersonnnelList.add(shrek2Commander);
        doReturn(shrek2Commander).when(mockShrek2).getCommander();
        Crew shrek2Crew = mock(Crew.class);
        doReturn(shrek2Crew).when(mockShrek2E).getCrew();
        doReturn(3).when(shrek2Crew).getGunnery();
        doReturn(5).when(shrek2Crew).getPiloting();

        Unit mockHarasser = mock(Unit.class);
        Entity mockHarasserE = mock(Tank.class);
        doReturn(EntityMovementMode.HOVER).when(mockHarasserE).getMovementMode();
        doReturn(40.0).when(mockHarasserE).getWeight();
        doReturn(TechConstants.T_IS_TW_NON_BOX).when(mockHarasserE).getTechLevel();
        doReturn(mockHarasserE).when(mockHarasser).getEntity();
        Person harasserCommander = mock(Person.class);
        mockPersonnnelList.add(harasserCommander);
        doReturn(harasserCommander).when(mockHarasser).getCommander();
        Crew harasserCrew = mock(Crew.class);
        doReturn(harasserCrew).when(mockHarasserE).getCrew();
        doReturn(5).when(harasserCrew).getGunnery();
        doReturn(5).when(harasserCrew).getPiloting();

        Unit mockHarasser2 = mock(Unit.class);
        Entity mockHarasser2E = mock(Tank.class);
        doReturn(EntityMovementMode.HOVER).when(mockHarasser2E).getMovementMode();
        doReturn(40.0).when(mockHarasser2E).getWeight();
        doReturn(TechConstants.T_IS_TW_NON_BOX).when(mockHarasser2E).getTechLevel();
        doReturn(mockHarasser2E).when(mockHarasser2).getEntity();
        Person harasser2Commander = mock(Person.class);
        mockPersonnnelList.add(harasser2Commander);
        doReturn(harasser2Commander).when(mockHarasser2).getCommander();
        Crew harasser2Crew = mock(Crew.class);
        doReturn(harasser2Crew).when(mockHarasser2E).getCrew();
        doReturn(5).when(harasser2Crew).getGunnery();
        doReturn(6).when(harasser2Crew).getPiloting();

        Unit mockLightning = mock(Unit.class);
        Entity mockLightingE = mock(Aero.class);
        doReturn(EntityMovementMode.AEROSPACE).when(mockLightingE).getMovementMode();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockLightingE).getTechLevel();
        doReturn(50.0).when(mockLightingE).getWeight();
        doReturn(mockLightingE).when(mockLightning).getEntity();
        Person lightningPilot = mock(Person.class);
        mockPersonnnelList.add(lightningPilot);
        doReturn(lightningPilot).when(mockLightning).getCommander();
        Crew lightningCrew = mock(Crew.class);
        doReturn(lightningCrew).when(mockLightingE).getCrew();
        doReturn(4).when(lightningCrew).getGunnery();
        doReturn(5).when(lightningCrew).getPiloting();

        Unit mockLightning2 = mock(Unit.class);
        Entity mockLighting2E = mock(Aero.class);
        doReturn(EntityMovementMode.AEROSPACE).when(mockLighting2E).getMovementMode();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockLighting2E).getTechLevel();
        doReturn(50.0).when(mockLighting2E).getWeight();
        doReturn(mockLighting2E).when(mockLightning2).getEntity();
        Person lightning2Pilot = mock(Person.class);
        mockPersonnnelList.add(lightning2Pilot);
        doReturn(lightning2Pilot).when(mockLightning2).getCommander();
        Crew lightning2Crew = mock(Crew.class);
        doReturn(lightning2Crew).when(mockLighting2E).getCrew();
        doReturn(4).when(lightning2Crew).getGunnery();
        doReturn(3).when(lightning2Crew).getPiloting();

        Unit mockUnion = mock(Unit.class);
        Entity mockUnionE = mock(Dropship.class);
        doReturn(EntityMovementMode.SPHEROID).when(mockUnionE).getMovementMode();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockUnionE).getTechLevel();
        doReturn(3600.0).when(mockUnionE).getWeight();
        doReturn(mockUnionE).when(mockUnion).getEntity();
        Person unionCommander = mock(Person.class);
        mockPersonnnelList.add(unionCommander);
        doReturn(unionCommander).when(mockUnion).getCommander();
        Crew unionCrew = mock(Crew.class);
        doReturn(unionCrew).when(mockUnionE).getCrew();
        doReturn(4).when(unionCrew).getGunnery();
        doReturn(5).when(unionCrew).getPiloting();
        Vector<Bay> bays = new Vector<>(2);
        bays.add(new MechBay(12, 1, 1));
        bays.add(new ASFBay(2, 1, 2));
        doReturn(bays).when(mockUnionE).getTransportBays();

        Unit mockInvader = mock(Unit.class);
        Entity mockInvaderE = mock(Jumpship.class);
        doReturn(EntityMovementMode.AEROSPACE).when(mockLightingE).getMovementMode();
        doReturn(TechConstants.T_INTRO_BOXSET).when(mockInvaderE).getTechLevel();
        doReturn(152000.0).when(mockInvaderE).getWeight();
        doReturn(mockInvaderE).when(mockInvader).getEntity();
        Person invaderCommander = mock(Person.class);
        mockPersonnnelList.add(invaderCommander);
        doReturn(invaderCommander).when(mockInvader).getCommander();
        Crew invaderCrew = mock(Crew.class);
        doReturn(invaderCrew).when(mockInvaderE).getCrew();
        doReturn(4).when(invaderCrew).getGunnery();
        doReturn(5).when(invaderCrew).getPiloting();
        doReturn(new Vector<Bay>(0)).when(mockInvaderE).getTransportBays();

        ArrayList<Unit> unitList = new ArrayList<>(12);
        unitList.add(mockWasp);
        unitList.add(mockStinger);
        unitList.add(mockThunderbolt);
        unitList.add(mockGriffin);
        unitList.add(mockShrek);
        unitList.add(mockShrek2);
        unitList.add(mockHarasser);
        unitList.add(mockHarasser2);
        unitList.add(mockLightning);
        unitList.add(mockLightning2);
        unitList.add(mockUnion);
        unitList.add(mockInvader);
        doReturn(unitList).when(mockCampaign).getCopyOfUnits();

        spyRating.initValues();
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
        when(mockCampaign.getMedicPool()).thenReturn(4);
        assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getMedicalSupportAvailable());

        // Add a mechwarrior who doubles as a back-up medic of Green skill.  This should add another 15 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockMechwarrior = mock(Person.class);
        when(mockMechwarrior.getPrimaryRole()).thenReturn(Person.T_MECHWARRIOR);
        when(mockMechwarrior.getSecondaryRole()).thenReturn(Person.T_DOCTOR);
        when(mockMechwarrior.isDoctor()).thenReturn(true);
        when(mockMechwarrior.isActive()).thenReturn(true);
        when(mockMechwarrior.isDeployed()).thenReturn(false);
        when(mockMechwarrior.getSkill(eq(SkillType.S_DOCTOR))).thenReturn(mockDoctorSkillGreen);
        when(mockMechwarrior.hasSkill(eq(SkillType.S_DOCTOR))).thenReturn(true);
        mockPersonnnelList.add(mockMechwarrior);
        expectedHours += 15;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getMedicalSupportAvailable());

        // Hire a full-time Medic.  This should add another 20 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockMedic = mock(Person.class);
        when(mockMedic.getPrimaryRole()).thenReturn(Person.T_MEDIC);
        when(mockMedic.isDoctor()).thenReturn(false);
        when(mockMedic.isActive()).thenReturn(true);
        when(mockMedic.isDeployed()).thenReturn(false);
        when(mockMedic.getSkill(eq(SkillType.S_MEDTECH))).thenReturn(mockMedicSkill);
        when(mockMedic.hasSkill(eq(SkillType.S_MEDTECH))).thenReturn(true);
        mockPersonnnelList.add(mockMedic);
        expectedHours += 20;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getMedicalSupportAvailable());
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
        when(mockCampaign.getAstechPool()).thenReturn(6);
        assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getTechSupportHours());

        // Add a mechwarrior who doubles as a back-up tech of Regular skill.  This should add another 20 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockMechwarrior = mock(Person.class);
        when(mockMechwarrior.getPrimaryRole()).thenReturn(Person.T_MECHWARRIOR);
        when(mockMechwarrior.getSecondaryRole()).thenReturn(Person.T_MECH_TECH);
        when(mockMechwarrior.isTech()).thenReturn(true);
        when(mockMechwarrior.isTechSecondary()).thenReturn(true);
        when(mockMechwarrior.isActive()).thenReturn(true);
        when(mockMechwarrior.isDeployed()).thenReturn(false);
        when(mockMechwarrior.getSkill(eq(SkillType.S_TECH_MECH))).thenReturn(mockMechTechSkillRegular);
        when(mockMechwarrior.hasSkill(eq(SkillType.S_TECH_MECH))).thenReturn(true);
        mockPersonnnelList.add(mockMechwarrior);
        expectedHours += 20;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getTechSupportHours());

        // Hire a full-time Astech.  This should add another 20 hours.
        testFieldManuMercRevDragoonsRating = new FieldManualMercRevDragoonsRating(mockCampaign);
        Person mockAstech = mock(Person.class);
        when(mockAstech.getPrimaryRole()).thenReturn(Person.T_ASTECH);
        when(mockAstech.isDoctor()).thenReturn(false);
        when(mockAstech.isTech()).thenReturn(false);
        when(mockAstech.isActive()).thenReturn(true);
        when(mockAstech.isDeployed()).thenReturn(false);
        when(mockAstech.getSkill(eq(SkillType.S_ASTECH))).thenReturn(mockAstechSkill);
        when(mockAstech.hasSkill(eq(SkillType.S_ASTECH))).thenReturn(true);
        mockPersonnnelList.add(mockAstech);
        expectedHours += 20;
        testFieldManuMercRevDragoonsRating.updateAvailableSupport();
        assertEquals(expectedHours, testFieldManuMercRevDragoonsRating.getTechSupportHours());
    }

    @Test
    public void testGetCommander() {

        // Test a campaign with the commander flagged.
        Person expectedCommander = mock(Person.class);
        when(mockCampaign.getFlaggedCommander()).thenReturn(expectedCommander);
        FieldManualMercRevDragoonsRating testRating = spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        assertEquals(expectedCommander, testRating.getCommander());

        // Test a campaign where the commander is not flagged, but there is a clear highest ranking officer.
        testRating = spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        when(expectedCommander.getRankNumeric()).thenReturn(10);
        Person leftennant = mock(Person.class);
        when(leftennant.getRankNumeric()).thenReturn(5);
        Person leftennant2 = mock(Person.class);
        when(leftennant2.getRankNumeric()).thenReturn(5);
        List<Person> commandList = new ArrayList<>(3);
        commandList.add(leftennant);
        commandList.add(expectedCommander);
        commandList.add(leftennant2);
        when(mockCampaign.getFlaggedCommander()).thenReturn(null);
        doReturn(commandList).when(testRating).getCommanderList();
        assertEquals(expectedCommander, testRating.getCommander());

        // Retire the old commander.  Give one leftennant more experience than the other.
        testRating = spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        when(mockCampaign.getFlaggedCommander()).thenReturn(null);
        doReturn(commandList).when(testRating).getCommanderList();
        when(expectedCommander.isActive()).thenReturn(false);
        when(leftennant.getExperienceLevel(anyBoolean())).thenReturn(SkillType.EXP_VETERAN);
        when(leftennant.isActive()).thenReturn(true);
        when(leftennant2.getExperienceLevel(anyBoolean())).thenReturn(SkillType.EXP_REGULAR);
        when(leftennant2.isActive()).thenReturn(true);
        assertEquals(leftennant, testRating.getCommander());

        // Test a campaign with no flagged commander and where no ranks have been assigned.
        testRating = spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        when(mockCampaign.getFlaggedCommander()).thenReturn(null);
        doReturn(null).when(testRating).getCommanderList();
        assertNull(testRating.getCommander());
    }


    @Test
    public void testGetTransportationDetails() {
        FieldManualMercRevDragoonsRating testRating = spy(new FieldManualMercRevDragoonsRating(mockCampaign));
        testRating.initValues();
        doReturn(-10).when(testRating).getTransportValue();
        doReturn(BigDecimal.ZERO).when(testRating).getTransportPercent();
        doReturn(4).when(testRating).getHeavyVeeCount();
        doReturn(4).when(testRating).getLightVeeCount();
        String expected = "Transportation      -10\n" +
                          "    Dropship Capacity:       0%\n" +
                          "        #Mech Bays:                   0 needed /   0 available\n" +
                          "        #Fighter Bays:                0 needed /   0 available\n" +
                          "        #Small Craft Bays:            0 needed /   0 available\n" +
                          "        #Protomech Bays:              0 needed /   0 available\n" +
                          "        #Heavy Vehicle Bays:          4 needed /   0 available\n" +
                          "        #Light Vehicle Bays:          4 needed /   0 available (plus 0 excess heavy)\n" +
                          "        #BA Bays:                     0 needed /   0 available\n" +
                          "        #Infantry Bays:               0 needed /   0 available\n" +
                          "    Jumpship?                No\n" +
                          "    Warship w/out Collar?    No\n" +
                          "    Warship w/ Collar?       No";
        assertEquals(expected, testRating.getTransportationDetails());
        // Add some heavy vee bays.
        doReturn(0).when(testRating).getTransportValue();
        doReturn(BigDecimal.valueOf(100)).when(testRating).getTransportPercent();
        doReturn(8).when(testRating).getHeavyVeeBayCount();
        expected = "Transportation        0\n" +
                   "    Dropship Capacity:      100%\n" +
                   "        #Mech Bays:                   0 needed /   0 available\n" +
                   "        #Fighter Bays:                0 needed /   0 available\n" +
                   "        #Small Craft Bays:            0 needed /   0 available\n" +
                   "        #Protomech Bays:              0 needed /   0 available\n" +
                   "        #Heavy Vehicle Bays:          4 needed /   8 available\n" +
                   "        #Light Vehicle Bays:          4 needed /   0 available (plus 4 excess heavy)\n" +
                   "        #BA Bays:                     0 needed /   0 available\n" +
                   "        #Infantry Bays:               0 needed /   0 available\n" +
                   "    Jumpship?                No\n" +
                   "    Warship w/out Collar?    No\n" +
                   "    Warship w/ Collar?       No";
        assertEquals(expected, testRating.getTransportationDetails());
    }
}

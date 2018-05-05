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

import asserts.BigDecimalAssert;
import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.Bay;
import megamek.common.BipedMech;
import megamek.common.Crew;
import megamek.common.DockingCollar;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.MechBay;
import megamek.common.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.mission.Mission;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/28/13 11:20 AM
 */
@SuppressWarnings("FieldCanBeLocal")
@RunWith(JUnit4.class)
public class CampaignOpsReputationTest {

    private Campaign mockCampaign = mock(Campaign.class);
    private ArrayList<Unit> unitList = new ArrayList<>();
    private ArrayList<Person> personnelList = new ArrayList<>();
    private ArrayList<Person> activePersonnelList = new ArrayList<>();
    private ArrayList<Mission> missionList = new ArrayList<>();

    // Mothballed units.
    private Unit mockMechMothballed = mock(Unit.class);
    private Unit mockAeroMothballed = mock(Unit.class);
    private Unit mockTankMothballed = mock(Unit.class);

    // Mechs
    private Skill mockMechGunnery = mock(Skill.class);
    private Skill mockMechPilot = mock(Skill.class);
    private Skill mockLeader = mock(Skill.class);
    private Skill mockTactics = mock(Skill.class);
    private Skill mockStrategy = mock(Skill.class);
    private Skill mockNegotiation = mock(Skill.class);
    private BipedMech mockThunderbolt1 = mock(BipedMech.class);
    private Unit mockThunderboltUnit1 = mock(Unit.class);
    private Person mockThunderbolt1Pilot = mock(Person.class);
    private Person mockThunderbolt1Tech = mock(Person.class);
    private BipedMech mockThunderbolt2 = mock(BipedMech.class);
    private Unit mockThunderboltUnit2 = mock(Unit.class);
    private Person mockThunderbolt2Pilot = mock(Person.class);
    private Person mockThunderbolt2Tech = mock(Person.class);
    private BipedMech mockGrasshopper1 = mock(BipedMech.class);
    private Unit mockGrasshopperUnit1 = mock(Unit.class);
    private Person mockGrasshopper1Pilot = mock(Person.class);
    private Person mockGrasshopper1Tech = mock(Person.class);
    private BipedMech mockGrasshopper2 = mock(BipedMech.class);
    private Unit mockGrasshopperUnit2 = mock(Unit.class);
    private Person mockGrasshopper2Pilot = mock(Person.class);
    private Person mockGrasshopper2Tech = mock(Person.class);

    // Tanks
    private Skill mockTankGunnery = mock(Skill.class);
    private Skill mockTankPilot = mock(Skill.class);
    private Tank mockBulldog1 = mock(Tank.class);
    private Unit mockBulldogUnit1 = mock(Unit.class);
    private Person mockBulldog1Driver = mock(Person.class);
    private Person mockBulldog1Gunner1 = mock(Person.class);
    private Person mockBulldog1Gunner2 = mock(Person.class);
    private Person mockBulldog1Gunner3 = mock(Person.class);
    private Person mockBulldog1Tech = mock(Person.class);
    private Tank mockBulldog2 = mock(Tank.class);
    private Unit mockBulldogUnit2 = mock(Unit.class);
    private Person mockBulldog2Driver = mock(Person.class);
    private Person mockBulldog2Gunner1 = mock(Person.class);
    private Person mockBulldog2Gunner2 = mock(Person.class);
    private Person mockBulldog2Gunner3 = mock(Person.class);
    private Person mockBulldog2Tech = mock(Person.class);
    private Tank mockBulldog3 = mock(Tank.class);
    private Unit mockBulldogUnit3 = mock(Unit.class);
    private Person mockBulldog3Driver = mock(Person.class);
    private Person mockBulldog3Gunner1 = mock(Person.class);
    private Person mockBulldog3Gunner2 = mock(Person.class);
    private Person mockBulldog3Gunner3 = mock(Person.class);
    private Person mockBulldog3Tech = mock(Person.class);
    private Tank mockBulldog4 = mock(Tank.class);
    private Unit mockBulldogUnit4 = mock(Unit.class);
    private Person mockBulldog4Driver = mock(Person.class);
    private Person mockBulldog4Gunner1 = mock(Person.class);
    private Person mockBulldog4Gunner2 = mock(Person.class);
    private Person mockBulldog4Gunner3 = mock(Person.class);
    private Person mockBulldog4Tech = mock(Person.class);
    private Tank mockPackrat1 = mock(Tank.class);
    private Unit mockPackratUnit1 = mock(Unit.class);
    private Person mockPackrat1Driver = mock(Person.class);
    private Person mockPackrat1Gunner = mock(Person.class);
    private Person mockPackrat1Tech = mock(Person.class);
    private Tank mockPackrat2 = mock(Tank.class);
    private Unit mockPackratUnit2 = mock(Unit.class);
    private Person mockPackrat2Driver = mock(Person.class);
    private Person mockPackrat2Gunner = mock(Person.class);
    private Person mockPackrat2Tech = mock(Person.class);
    private Tank mockPackrat3 = mock(Tank.class);
    private Unit mockPackratUnit3 = mock(Unit.class);
    private Person mockPackrat3Driver = mock(Person.class);
    private Person mockPackrat3Gunner = mock(Person.class);
    private Person mockPackrat3Tech = mock(Person.class);
    private Tank mockPackrat4 = mock(Tank.class);
    private Unit mockPackratUnit4 = mock(Unit.class);
    private Person mockPackrat4Driver = mock(Person.class);
    private Person mockPackrat4Gunner = mock(Person.class);
    private Person mockPackrat4Tech = mock(Person.class);

    // Infantry
    private Skill mockInfantryGunnery = mock(Skill.class);
    private Infantry mockLaserPlatoon = mock(Infantry.class);
    private Unit mockLaserPlatoonUnit = mock(Unit.class);
    private Collection<Person> infantryPersonnel = new HashSet<>(28);

    // Fighters
    private Skill mockAeroGunnery = mock(Skill.class);
    private Skill mockAeroPilot = mock(Skill.class);
    private Aero mockCorsair1 = mock(Aero.class);
    private Unit mockCorsairUnit1 = mock(Unit.class);
    private Person mockCorsair1Pilot = mock(Person.class);
    private Person getMockCorsair1Tech = mock(Person.class);
    private Aero mockCorsair2 = mock(Aero.class);
    private Unit mockCorsairUnit2 = mock(Unit.class);
    private Person mockCorsair2Pilot = mock(Person.class);
    private Person getMockCorsair2Tech = mock(Person.class);

    // Dropships
    private Skill mockDropGunnery = mock(Skill.class);
    private Skill mockDropPilot = mock(Skill.class);
    private Dropship mockSeeker = mock(Dropship.class);
    private Unit mockSeekerUnit = mock(Unit.class);
    private Collection<Person> seekerCrew = new HashSet<>(20);

    // Jumpships
    private Skill mockJumpGunnery = mock(Skill.class);
    private Skill mockJumpPilot = mock(Skill.class);
    private Jumpship mockInvader = mock(Jumpship.class);
    private Unit mockInvaderUnit = mock(Unit.class);
    private Collection<Person> invaderCrew = new HashSet<>(24);

    // Techs
    private Skill mockMechTechSkillRegular = mock(Skill.class);
    private Skill mockMechTechSkillElite = mock(Skill.class);
    private Skill mockFighterTechSkill = mock(Skill.class);
    private Skill mockFighterTechSkillElite = mock(Skill.class);
    private Skill mockVeeTechSkill = mock(Skill.class);
    private Collection<Person> regularAdmins = new HashSet<>(10);

    // Finances
    private Finances mockFinances = mock(Finances.class);

    private CampaignOpsReputation spyReputation = spy(new CampaignOpsReputation(mockCampaign));

    @Before
    public void setUp() {
        int astechs = 0;
        mockCampaign = mock(Campaign.class);
        unitList = new ArrayList<>();
        personnelList = new ArrayList<>();
        activePersonnelList = new ArrayList<>();
        missionList = new ArrayList<>();
        infantryPersonnel = new HashSet<>(28);
        seekerCrew = new HashSet<>(20);
        invaderCrew = new HashSet<>(24);
        regularAdmins = new HashSet<>(10);
        spyReputation = spy(new CampaignOpsReputation(mockCampaign));

        when(mockMechMothballed.isMothballed()).thenReturn(true);

        when(mockAeroMothballed.isMothballed()).thenReturn(true);

        when(mockTankMothballed.isMothballed()).thenReturn(true);

        when(mockMechGunnery.getLevel()).thenReturn(4);
        when(mockMechPilot.getLevel()).thenReturn(5);
        when(mockTankGunnery.getLevel()).thenReturn(4);
        when(mockTankPilot.getLevel()).thenReturn(5);
        when(mockInfantryGunnery.getLevel()).thenReturn(4);
        when(mockAeroGunnery.getLevel()).thenReturn(4);
        when(mockAeroPilot.getLevel()).thenReturn(5);
        when(mockDropGunnery.getLevel()).thenReturn(4);
        when(mockDropPilot.getLevel()).thenReturn(5);
        when(mockJumpGunnery.getLevel()).thenReturn(4);
        when(mockJumpPilot.getLevel()).thenReturn(5);
        when(mockLeader.getLevel()).thenReturn(4);
        when(mockTactics.getLevel()).thenReturn(2);
        when(mockStrategy.getLevel()).thenReturn(2);
        when(mockNegotiation.getLevel()).thenReturn(5);
        when(mockMechTechSkillRegular.getLevel()).thenReturn(7);
        when(mockMechTechSkillElite.getLevel()).thenReturn(5);
        when(mockFighterTechSkill.getLevel()).thenReturn(7);
        when(mockFighterTechSkillElite.getLevel()).thenReturn(5);
        when(mockVeeTechSkill.getLevel()).thenReturn(7);

        when(mockThunderbolt1.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockThunderboltUnit1.getEntity()).thenReturn(mockThunderbolt1);
        when(mockThunderbolt1Pilot.isAdmin()).thenReturn(false);
        when(mockThunderbolt1Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockThunderbolt1Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockThunderbolt1Pilot);
        mockThunderboltUnit1.addPilotOrSoldier(mockThunderbolt1Pilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockThunderbolt1Pilot);
        when(mockThunderboltUnit1.getCrew()).thenReturn(crew);
        Crew mockThunderboltCrew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockThunderboltCrew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockThunderboltCrew).getGunnery();
        when(mockThunderbolt1.getCrew()).thenReturn(mockThunderboltCrew);
        when(mockThunderbolt1Tech.isAdmin()).thenReturn(false);
        when(mockThunderbolt1Tech.isTech()).thenReturn(true);
        when(mockThunderbolt1Tech.isActive()).thenReturn(true);
        when(mockThunderbolt1Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockThunderbolt1Tech);
        astechs += 6;

        when(mockThunderbolt2.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockThunderboltUnit2.getEntity()).thenReturn(mockThunderbolt2);
        when(mockThunderbolt2Pilot.isAdmin()).thenReturn(false);
        when(mockThunderbolt2Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockThunderbolt2Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockThunderbolt2Pilot);
        mockThunderboltUnit2.addPilotOrSoldier(mockThunderbolt2Pilot);
        crew = new ArrayList<>(1);
        crew.add(mockThunderbolt2Pilot);
        when(mockThunderboltUnit2.getCrew()).thenReturn(crew);
        Crew mockThunderbolt2Crew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockThunderbolt2Crew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockThunderbolt2Crew).getGunnery();
        when(mockThunderbolt2.getCrew()).thenReturn(mockThunderbolt2Crew);
        when(mockThunderbolt2Tech.isAdmin()).thenReturn(false);
        when(mockThunderbolt2Tech.isTech()).thenReturn(true);
        when(mockThunderbolt2Tech.isActive()).thenReturn(true);
        when(mockThunderbolt2Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockThunderbolt2Tech);
        astechs += 6;

        when(mockGrasshopper1.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockGrasshopperUnit1.getEntity()).thenReturn(mockGrasshopper1);
        when(mockGrasshopper1Pilot.isAdmin()).thenReturn(false);
        when(mockGrasshopper1Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockGrasshopper1Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockGrasshopper1Pilot);
        mockGrasshopperUnit1.addPilotOrSoldier(mockGrasshopper1Pilot);
        crew = new ArrayList<>(1);
        crew.add(mockGrasshopper1Pilot);
        when(mockGrasshopperUnit1.getCrew()).thenReturn(crew);
        Crew mockGrasshopperCrew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockGrasshopperCrew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockGrasshopperCrew).getGunnery();
        when(mockGrasshopper1.getCrew()).thenReturn(mockGrasshopperCrew);
        when(mockGrasshopper1Tech.isAdmin()).thenReturn(false);
        when(mockGrasshopper1Tech.isTech()).thenReturn(true);
        when(mockGrasshopper1Tech.isActive()).thenReturn(true);
        when(mockGrasshopper1Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockGrasshopper1Tech);
        astechs += 6;

        when(mockGrasshopper2.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockGrasshopperUnit2.getEntity()).thenReturn(mockGrasshopper2);
        when(mockGrasshopper2Pilot.isAdmin()).thenReturn(false);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_LEADER)).thenReturn(mockLeader);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_TACTICS)).thenReturn(mockTactics);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_STRATEGY)).thenReturn(mockStrategy);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_NEG)).thenReturn(mockNegotiation);
        personnelList.add(mockGrasshopper2Pilot);
        mockGrasshopperUnit2.addPilotOrSoldier(mockGrasshopper2Pilot);
        crew = new ArrayList<>(1);
        crew.add(mockGrasshopper2Pilot);
        when(mockGrasshopperUnit2.getCrew()).thenReturn(crew);
        Crew mockGrasshopper2Crew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockGrasshopper2Crew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockGrasshopper2Crew).getGunnery();
        when(mockGrasshopper2.getCrew()).thenReturn(mockGrasshopper2Crew);
        when(mockGrasshopper2Tech.isAdmin()).thenReturn(false);
        when(mockGrasshopper2Tech.isTech()).thenReturn(true);
        when(mockGrasshopper2Tech.isActive()).thenReturn(true);
        when(mockGrasshopper2Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillElite);
        personnelList.add(mockGrasshopper2Tech);
        astechs += 6;

        when(mockBulldog1.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldogUnit1.getEntity()).thenReturn(mockBulldog1);
        when(mockBulldog1Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog1Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Driver.isAdmin()).thenReturn(false);
        when(mockBulldog1Gunner1.isAdmin()).thenReturn(false);
        when(mockBulldog1Gunner2.isAdmin()).thenReturn(false);
        when(mockBulldog1Gunner3.isAdmin()).thenReturn(false);
        when(mockBulldog1Tech.isAdmin()).thenReturn(false);
        when(mockBulldog1Tech.isTech()).thenReturn(true);
        when(mockBulldog1Tech.isActive()).thenReturn(true);
        when(mockBulldog1Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog1Driver);
        personnelList.add(mockBulldog1Gunner1);
        personnelList.add(mockBulldog2Gunner1);
        personnelList.add(mockBulldog1Gunner3);
        personnelList.add(mockBulldog1Tech);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Driver);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner1);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner2);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner3);
        crew = new ArrayList<>(4);
        crew.add(mockBulldog1Driver);
        crew.add(mockBulldog1Gunner1);
        crew.add(mockBulldog1Gunner2);
        crew.add(mockBulldog1Gunner3);
        when(mockBulldogUnit1.getCrew()).thenReturn(crew);
        Crew mockBulldog1Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog1Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog1Crew).getGunnery();
        when(mockBulldog1.getCrew()).thenReturn(mockBulldog1Crew);
        astechs += 6;

        when(mockBulldog2.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog2Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog2Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog2Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog2Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldogUnit2.getEntity()).thenReturn(mockBulldog2);
        when(mockBulldog2Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Driver);
        when(mockBulldog2Gunner1.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Gunner1);
        when(mockBulldog2Gunner2.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Gunner1);
        when(mockBulldog2Gunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Gunner3);
        when(mockBulldog2Tech.isAdmin()).thenReturn(false);
        when(mockBulldog2Tech.isTech()).thenReturn(true);
        when(mockBulldog2Tech.isActive()).thenReturn(true);
        when(mockBulldog2Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog2Tech);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Driver);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner1);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner2);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner3);
        crew = new ArrayList<>(4);
        crew.add(mockBulldog2Driver);
        crew.add(mockBulldog2Gunner1);
        crew.add(mockBulldog2Gunner2);
        crew.add(mockBulldog2Gunner3);
        when(mockBulldogUnit2.getCrew()).thenReturn(crew);
        Crew mockBulldog2Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog2Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog2Crew).getGunnery();
        when(mockBulldog2.getCrew()).thenReturn(mockBulldog2Crew);
        astechs += 6;

        when(mockBulldog3.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog3Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog3Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog3Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog3Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog3Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldogUnit3.getEntity()).thenReturn(mockBulldog3);
        when(mockBulldog3Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Driver);
        when(mockBulldog3Gunner1.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Gunner1);
        when(mockBulldog3Gunner2.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Gunner1);
        when(mockBulldog3Gunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Gunner3);
        when(mockBulldog3Tech.isAdmin()).thenReturn(false);
        when(mockBulldog3Tech.isTech()).thenReturn(true);
        when(mockBulldog3Tech.isActive()).thenReturn(true);
        when(mockBulldog3Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog3Tech);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Driver);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner1);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner2);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner3);
        crew = new ArrayList<>(4);
        crew.add(mockBulldog3Driver);
        crew.add(mockBulldog3Gunner1);
        crew.add(mockBulldog3Gunner2);
        crew.add(mockBulldog3Gunner3);
        when(mockBulldogUnit3.getCrew()).thenReturn(crew);
        Crew mockBulldog3Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog3Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog3Crew).getGunnery();
        when(mockBulldog3.getCrew()).thenReturn(mockBulldog3Crew);
        astechs += 6;

        when(mockBulldog4.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog4Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog4Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog4Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog4Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog4Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldogUnit4.getEntity()).thenReturn(mockBulldog4);
        when(mockBulldog4Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Driver);
        when(mockBulldog4Gunner1.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Gunner1);
        when(mockBulldog4Gunner2.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Gunner1);
        when(mockBulldog4Gunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Gunner3);
        when(mockBulldog4Tech.isAdmin()).thenReturn(false);
        when(mockBulldog4Tech.isTech()).thenReturn(true);
        when(mockBulldog4Tech.isActive()).thenReturn(true);
        when(mockBulldog4Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog4Tech);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Driver);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner1);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner2);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner3);
        crew = new ArrayList<>(4);
        crew.add(mockBulldog4Driver);
        crew.add(mockBulldog4Gunner1);
        crew.add(mockBulldog4Gunner2);
        crew.add(mockBulldog4Gunner3);
        when(mockBulldogUnit4.getCrew()).thenReturn(crew);
        Crew mockBulldog4Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog4Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog4Crew).getGunnery();
        when(mockBulldog4.getCrew()).thenReturn(mockBulldog4Crew);
        astechs += 6;

        when(mockPackrat1.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat1Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat1Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat1Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit1.getEntity()).thenReturn(mockPackrat1);
        when(mockPackrat1Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat1Driver);
        when(mockPackrat1Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat1Gunner);
        when(mockPackrat1Tech.isAdmin()).thenReturn(false);
        when(mockPackrat1Tech.isTech()).thenReturn(true);
        when(mockPackrat1Tech.isActive()).thenReturn(true);
        when(mockPackrat1Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat1Tech);
        mockPackratUnit1.addPilotOrSoldier(mockPackrat1Driver);
        mockPackratUnit1.addPilotOrSoldier(mockPackrat1Gunner);
        crew = new ArrayList<>(2);
        crew.add(mockPackrat1Driver);
        crew.add(mockPackrat1Gunner);
        when(mockPackratUnit1.getCrew()).thenReturn(crew);
        Crew mockPackrat1Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat1Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat1Crew).getGunnery();
        when(mockPackrat1.getCrew()).thenReturn(mockPackrat1Crew);
        astechs += 6;

        when(mockPackrat2.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat2Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat2Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit2.getEntity()).thenReturn(mockPackrat2);
        when(mockPackrat2Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat2Driver);
        when(mockPackrat2Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat2Gunner);
        when(mockPackrat2Tech.isAdmin()).thenReturn(false);
        when(mockPackrat2Tech.isTech()).thenReturn(true);
        when(mockPackrat2Tech.isActive()).thenReturn(true);
        when(mockPackrat2Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat2Tech);
        mockPackratUnit2.addPilotOrSoldier(mockPackrat2Driver);
        mockPackratUnit2.addPilotOrSoldier(mockPackrat2Gunner);
        crew = new ArrayList<>(2);
        crew.add(mockPackrat2Driver);
        crew.add(mockPackrat2Gunner);
        when(mockPackratUnit2.getCrew()).thenReturn(crew);
        Crew mockPackrat2Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat2Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat2Crew).getGunnery();
        when(mockPackrat2.getCrew()).thenReturn(mockPackrat2Crew);
        astechs += 6;

        when(mockPackrat3.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat3Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat3Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat3Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit3.getEntity()).thenReturn(mockPackrat3);
        when(mockPackrat3Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat3Driver);
        when(mockPackrat3Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat3Gunner);
        when(mockPackrat3Tech.isAdmin()).thenReturn(false);
        when(mockPackrat3Tech.isTech()).thenReturn(true);
        when(mockPackrat3Tech.isActive()).thenReturn(true);
        when(mockPackrat3Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat3Tech);
        mockPackratUnit3.addPilotOrSoldier(mockPackrat3Driver);
        mockPackratUnit3.addPilotOrSoldier(mockPackrat3Gunner);
        crew = new ArrayList<>(2);
        crew.add(mockPackrat3Driver);
        crew.add(mockPackrat3Gunner);
        when(mockPackratUnit3.getCrew()).thenReturn(crew);
        Crew mockPackrat3Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat3Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat3Crew).getGunnery();
        when(mockPackrat3.getCrew()).thenReturn(mockPackrat3Crew);
        astechs += 6;

        when(mockPackrat4.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat4Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat4Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat4Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit4.getEntity()).thenReturn(mockPackrat4);
        when(mockPackrat4Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat4Driver);
        when(mockPackrat4Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat4Gunner);
        when(mockPackrat4Tech.isAdmin()).thenReturn(false);
        when(mockPackrat4Tech.isTech()).thenReturn(true);
        when(mockPackrat4Tech.isActive()).thenReturn(true);
        when(mockPackrat4Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat4Tech);
        mockPackratUnit4.addPilotOrSoldier(mockPackrat4Driver);
        mockPackratUnit4.addPilotOrSoldier(mockPackrat4Gunner);
        crew = new ArrayList<>(2);
        crew.add(mockPackrat4Driver);
        crew.add(mockPackrat4Gunner);
        when(mockPackratUnit4.getCrew()).thenReturn(crew);
        Crew mockPackrat4Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat4Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat4Crew).getGunnery();
        when(mockPackrat4.getCrew()).thenReturn(mockPackrat4Crew);
        astechs += 6;

        when(mockLaserPlatoon.getEntityType()).thenReturn(Entity.ETYPE_INFANTRY);
        when(mockLaserPlatoon.getSquadSize()).thenReturn(7);
        when(mockLaserPlatoon.getSquadN()).thenReturn(4);
        when(mockLaserPlatoonUnit.getEntity()).thenReturn(mockLaserPlatoon);
        crew = new ArrayList<>(28);
        for (int i = 0; i < 28; i++) {
            Person mockInfantry = mock(Person.class);
            when(mockInfantry.isAdmin()).thenReturn(false);
            when(mockInfantry.getSkill(SkillType.S_SMALL_ARMS)).thenReturn(mockInfantryGunnery);
            infantryPersonnel.add(mockInfantry);
            mockLaserPlatoonUnit.addPilotOrSoldier(mockInfantry);
            crew.add(mockInfantry);
        }
        when(mockLaserPlatoonUnit.getCrew()).thenReturn(crew);
        Crew mockLaserPlatoonCrew = mock(Crew.class);
        doReturn(mockInfantryGunnery.getLevel()).when(mockLaserPlatoonCrew).getGunnery();
        when(mockLaserPlatoon.getCrew()).thenReturn(mockLaserPlatoonCrew);
        personnelList.addAll(infantryPersonnel);

        when(mockCorsair1.getEntityType()).thenReturn(Entity.ETYPE_AERO);
        when(mockCorsairUnit1.getEntity()).thenReturn(mockCorsair1);
        when(mockCorsair1Pilot.isAdmin()).thenReturn(false);
        when(mockCorsair1Pilot.getSkill(SkillType.S_GUN_AERO)).thenReturn(mockAeroGunnery);
        when(mockCorsair1Pilot.getSkill(SkillType.S_PILOT_AERO)).thenReturn(mockAeroPilot);
        personnelList.add(mockCorsair1Pilot);
        mockCorsairUnit1.addPilotOrSoldier(mockCorsair1Pilot);
        crew = new ArrayList<>(1);
        crew.add(mockCorsair1Pilot);
        when(mockCorsairUnit1.getCrew()).thenReturn(crew);
        when(getMockCorsair1Tech.isAdmin()).thenReturn(false);
        when(getMockCorsair1Tech.isTech()).thenReturn(true);
        when(getMockCorsair1Tech.isActive()).thenReturn(true);
        when(getMockCorsair1Tech.getSkill(SkillType.S_TECH_AERO)).thenReturn(mockFighterTechSkill);
        personnelList.add(getMockCorsair1Tech);
        Crew mockCorsair1Crew = mock(Crew.class);
        doReturn(mockAeroPilot.getLevel()).when(mockCorsair1Crew).getPiloting();
        doReturn(mockAeroGunnery.getLevel()).when(mockCorsair1Crew).getGunnery();
        when(mockCorsair1.getCrew()).thenReturn(mockCorsair1Crew);
        astechs += 6;

        when(mockCorsair2.getEntityType()).thenReturn(Entity.ETYPE_AERO);
        when(mockCorsairUnit2.getEntity()).thenReturn(mockCorsair2);
        when(mockCorsair2Pilot.isAdmin()).thenReturn(false);
        when(mockCorsair2Pilot.getSkill(SkillType.S_GUN_AERO)).thenReturn(mockAeroGunnery);
        when(mockCorsair2Pilot.getSkill(SkillType.S_PILOT_AERO)).thenReturn(mockAeroPilot);
        personnelList.add(mockCorsair2Pilot);
        mockCorsairUnit2.addPilotOrSoldier(mockCorsair2Pilot);
        crew = new ArrayList<>(1);
        crew.add(mockCorsair2Pilot);
        when(mockCorsairUnit2.getCrew()).thenReturn(crew);
        when(getMockCorsair2Tech.isAdmin()).thenReturn(false);
        when(getMockCorsair2Tech.isTech()).thenReturn(true);
        when(getMockCorsair2Tech.isActive()).thenReturn(true);
        when(getMockCorsair2Tech.getSkill(SkillType.S_TECH_AERO)).thenReturn(mockFighterTechSkillElite);
        personnelList.add(getMockCorsair2Tech);
        Crew mockCorsair2Crew = mock(Crew.class);
        doReturn(mockAeroPilot.getLevel()).when(mockCorsair2Crew).getPiloting();
        doReturn(mockAeroGunnery.getLevel()).when(mockCorsair2Crew).getGunnery();
        when(mockCorsair2.getCrew()).thenReturn(mockCorsair2Crew);
        astechs += 6;

        when(mockSeeker.getEntityType()).thenReturn(Entity.ETYPE_DROPSHIP);
        when(mockSeekerUnit.getEntity()).thenReturn(mockSeeker);
        Bay transportBay;
        Vector<Bay> bayList = new Vector<>();
        transportBay = new MechBay(4, 1, 1);
        bayList.add(transportBay);
        transportBay = new ASFBay(2, 0, 2);
        bayList.add(transportBay);
        transportBay = new LightVehicleBay(22, 0, 3);
        bayList.add(transportBay);
        transportBay = new InfantryBay(4.0, 0, 4, InfantryBay.PlatoonType.FOOT);
        bayList.add(transportBay);
        when(mockSeeker.getTransportBays()).thenReturn(bayList);
        crew = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            Person mockCrew = mock(Person.class);
            when(mockCrew.isAdmin()).thenReturn(false);
            if (i == 0) {
                when(mockCrew.getSkill(SkillType.S_PILOT_SPACE)).thenReturn(mockDropPilot);
            } else {
                when(mockCrew.getSkill(SkillType.S_GUN_SPACE)).thenReturn(mockDropGunnery);
            }
            seekerCrew.add(mockCrew);
            mockSeekerUnit.addPilotOrSoldier(mockCrew);
            crew.add(mockCrew);
        }
        personnelList.addAll(seekerCrew);
        when(mockSeekerUnit.getCrew()).thenReturn(crew);
        when(mockSeekerUnit.getActiveCrew()).thenReturn(crew);
        when(mockSeekerUnit.getFullCrewSize()).thenReturn(20);
        Crew mockSeekerCrew = mock(Crew.class);
        doReturn(mockDropPilot.getLevel()).when(mockSeekerCrew).getPiloting();
        doReturn(mockDropGunnery.getLevel()).when(mockSeekerCrew).getGunnery();
        when(mockSeeker.getCrew()).thenReturn(mockSeekerCrew);

        when(mockInvader.getEntityType()).thenReturn(Entity.ETYPE_JUMPSHIP);
        when(mockInvaderUnit.getEntity()).thenReturn(mockInvader);
        DockingCollar collar;
        Vector<DockingCollar> collarList = new Vector<>(4);
        for (int i = 0; i < 4; i++) {
            collar = mock(DockingCollar.class);
            collarList.add(collar);
        }
        when(mockInvader.getTransportBays()).thenReturn(new Vector<>(0));
        when(mockInvader.getDockingCollars()).thenReturn(collarList);
        crew = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            Person mockCrew = mock(Person.class);
            when(mockCrew.isAdmin()).thenReturn(false);
            when(mockCrew.getSkill(SkillType.S_GUN_SPACE)).thenReturn(mockJumpGunnery);
            if (i == 0) {
                when(mockCrew.getSkill(SkillType.S_PILOT_SPACE)).thenReturn(mockJumpPilot);
            }
            invaderCrew.add(mockCrew);
            mockInvaderUnit.addPilotOrSoldier(mockCrew);
            crew.add(mockCrew);
        }
        personnelList.addAll(invaderCrew);
        when(mockInvaderUnit.getCrew()).thenReturn(crew);
        when(mockInvaderUnit.getActiveCrew()).thenReturn(crew);
        when(mockInvaderUnit.getFullCrewSize()).thenReturn(24);
        Crew mockInvaderCrew = mock(Crew.class);
        doReturn(mockJumpPilot.getLevel()).when(mockInvaderCrew).getPiloting();
        doReturn(mockJumpGunnery.getLevel()).when(mockInvaderCrew).getGunnery();
        when(mockInvader.getCrew()).thenReturn(mockInvaderCrew);

        unitList.add(mockThunderboltUnit1);
        unitList.add(mockThunderboltUnit2);
        unitList.add(mockGrasshopperUnit1);
        unitList.add(mockGrasshopperUnit2);
        unitList.add(mockBulldogUnit1);
        unitList.add(mockBulldogUnit2);
        unitList.add(mockBulldogUnit3);
        unitList.add(mockBulldogUnit4);
        unitList.add(mockPackratUnit1);
        unitList.add(mockPackratUnit2);
        unitList.add(mockPackratUnit3);
        unitList.add(mockPackratUnit4);
        unitList.add(mockLaserPlatoonUnit);
        unitList.add(mockCorsairUnit1);
        unitList.add(mockCorsairUnit2);
        unitList.add(mockSeekerUnit);
        unitList.add(mockInvaderUnit);

        for (Unit u : unitList) {
            when(u.isPresent()).thenReturn(true);
            when(u.hasPilot()).thenReturn(true);
        }

        for (int i = 0; i < 10; i++) {
            Person admin = mock(Person.class);
            when(admin.isAdmin()).thenReturn(true);
            when(admin.isActive()).thenReturn(true);
            regularAdmins.add(admin);
        }
        personnelList.addAll(regularAdmins);
        for (Person p : personnelList) {
            if (p.isActive()) { activePersonnelList.add(p); }
        }

        when(mockFinances.isInDebt()).thenReturn(false);

        doReturn(unitList).when(mockCampaign).getCopyOfUnits();
        doReturn(personnelList).when(mockCampaign).getPersonnel();
        doReturn(activePersonnelList).when(mockCampaign).getActivePersonnel();
        doReturn(astechs).when(mockCampaign).getAstechPool();
        doCallRealMethod().when(mockCampaign).getNumberAstechs();
        doCallRealMethod().when(mockCampaign).getNumberPrimaryAstechs();
        doCallRealMethod().when(mockCampaign).getNumberSecondaryAstechs();
        doCallRealMethod().when(mockCampaign).getTechs();
        doCallRealMethod().when(mockCampaign).getDoctors();
        doCallRealMethod().when(mockCampaign).getAdmins();
        doReturn(mockGrasshopper2Pilot).when(mockCampaign).getFlaggedCommander();
        doReturn(missionList).when(mockCampaign).getMissions();
        doReturn(mockFinances).when(mockCampaign).getFinances();
    }

    @Test
    public void testCalculateSupportNeeds() {

        // Test the example company.
        BigDecimal expectedTotalSkill = new BigDecimal("144.00");
        BigDecimal expectedAverageSkill = new BigDecimal("9.00");
        spyReputation.initValues();
        assertEquals(4, spyReputation.getMechCount());
        assertEquals(2, spyReputation.getFighterCount());
        assertEquals(0, spyReputation.getProtoCount());
        assertEquals(8, spyReputation.getVeeCount());
        assertEquals(0, spyReputation.getBattleArmorCount());
        assertEquals(28, spyReputation.getInfantryCount());
        assertEquals(98, spyReputation.getNonAdminPersonnelCount());
        assertEquals(1, spyReputation.getDropshipCount());
        BigDecimalAssert.assertEquals(expectedTotalSkill, spyReputation.getTotalSkillLevels(), 2);
        assertEquals(4, spyReputation.getMechTechTeamsNeeded());
        assertEquals(2, spyReputation.getAeroTechTeamsNeeded());
        assertEquals(8, spyReputation.getMechanicTeamsNeeded());
        assertEquals(0, spyReputation.getBattleArmorTechTeamsNeeded());
        assertEquals(10, spyReputation.getAdminsNeeded());
        assertEquals(expectedAverageSkill, spyReputation.calcAverageExperience());
        assertEquals(10, spyReputation.getExperienceValue());

        // Add a couple of mothballed units.
        unitList.add(mockMechMothballed);
        unitList.add(mockTankMothballed);
        unitList.add(mockAeroMothballed);
        assertEquals(4, spyReputation.getMechCount());
        assertEquals(2, spyReputation.getFighterCount());
        assertEquals(0, spyReputation.getProtoCount());
        assertEquals(8, spyReputation.getVeeCount());
        assertEquals(0, spyReputation.getBattleArmorCount());
        assertEquals(28, spyReputation.getInfantryCount());
        assertEquals(98, spyReputation.getNonAdminPersonnelCount());
        assertEquals(1, spyReputation.getDropshipCount());
        BigDecimalAssert.assertEquals(expectedTotalSkill, spyReputation.getTotalSkillLevels(), 2);
        assertEquals(4, spyReputation.getMechTechTeamsNeeded());
        assertEquals(2, spyReputation.getAeroTechTeamsNeeded());
        assertEquals(8, spyReputation.getMechanicTeamsNeeded());
        assertEquals(0, spyReputation.getBattleArmorTechTeamsNeeded());
        assertEquals(10, spyReputation.getAdminsNeeded());
        assertEquals(expectedAverageSkill, spyReputation.calcAverageExperience());
        assertEquals(10, spyReputation.getExperienceValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.getMechCount());
        assertEquals(0, spyReputation.getFighterCount());
        assertEquals(0, spyReputation.getProtoCount());
        assertEquals(0, spyReputation.getVeeCount());
        assertEquals(0, spyReputation.getBattleArmorCount());
        assertEquals(0, spyReputation.getInfantryCount());
        assertEquals(0, spyReputation.getNonAdminPersonnelCount());
        assertEquals(0, spyReputation.getDropshipCount());
        BigDecimalAssert.assertEquals(BigDecimal.ZERO, spyReputation.getTotalSkillLevels(), 2);
        assertEquals(0, spyReputation.getMechTechTeamsNeeded());
        assertEquals(0, spyReputation.getAeroTechTeamsNeeded());
        assertEquals(0, spyReputation.getMechanicTeamsNeeded());
        assertEquals(0, spyReputation.getBattleArmorTechTeamsNeeded());
        assertEquals(0, spyReputation.getAdminsNeeded());
        assertEquals(BigDecimal.ZERO, spyReputation.calcAverageExperience());
        assertEquals(0, spyReputation.getExperienceValue());
    }

    private void buildFreshCampaign() {
        doReturn(new ArrayList<Unit>(0)).when(mockCampaign).getCopyOfUnits();
        doReturn(new ArrayList<Person>(0)).when(mockCampaign).getPersonnel();
        doReturn(new ArrayList<Person>(0)).when(mockCampaign).getActivePersonnel();
        doReturn(new ArrayList<Person>(0)).when(mockCampaign).getAdmins();
        doReturn(new ArrayList<Person>(0)).when(mockCampaign).getTechs();
        doReturn(new ArrayList<Person>(0)).when(mockCampaign).getDoctors();
        doReturn(0).when(mockCampaign).getAstechPool();
        doReturn(0).when(mockCampaign).getNumberAstechs();
        doReturn(null).when(mockCampaign).getFlaggedCommander();
    }

    @Test
    public void testGetCommanderValue() {
        spyReputation.initValues();
        assertEquals(13, spyReputation.getCommanderValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.getCommanderValue());
    }

    @Test
    public void testGetCombatRecordValue() {
        // New company with no record.
        spyReputation.initValues();
        assertEquals(0, spyReputation.getCombatRecordValue());

        // Add a few missions.
        Mission winOne = mock(Mission.class);
        when(winOne.isActive()).thenReturn(false);
        when(winOne.getStatus()).thenReturn(Mission.S_SUCCESS);
        missionList.add(winOne);
        Mission winTwo = mock(Mission.class);
        when(winTwo.isActive()).thenReturn(false);
        when(winTwo.getStatus()).thenReturn(Mission.S_SUCCESS);
        missionList.add(winTwo);
        Mission winThree = mock(Mission.class);
        when(winThree.isActive()).thenReturn(false);
        when(winThree.getStatus()).thenReturn(Mission.S_SUCCESS);
        missionList.add(winThree);
        Mission lossOne = mock(Mission.class);
        when(lossOne.isActive()).thenReturn(false);
        when(lossOne.getStatus()).thenReturn(Mission.S_FAILED);
        missionList.add(lossOne);
        Mission active = mock(Mission.class);
        when(active.isActive()).thenReturn(true);
        when(active.getStatus()).thenReturn(Mission.S_ACTIVE);
        missionList.add(active);
        assertEquals(5, spyReputation.getCombatRecordValue());
    }

    @Test
    public void testGetTransportValue() {
        spyReputation.initValues();
        assertEquals(20, spyReputation.getTransportValue());

        // Test not having any dropships (though we still have a jumpship).
        doReturn(0).when(spyReputation).getDropshipCount();
        doReturn(0).when(spyReputation).getMechBayCount();
        doReturn(0).when(spyReputation).getInfantryBayCount();
        doReturn(0).when(spyReputation).getLightVeeBayCount();
        doReturn(0).when(spyReputation).getHeavyVeeBayCount();
        doReturn(0).when(spyReputation).getBaBayCount();
        doReturn(0).when(spyReputation).getFighterBayCount();
        doReturn(0).when(spyReputation).getProtoBayCount();
        doReturn(0).when(spyReputation).getSmallCraftBayCount();
        assertEquals(0, spyReputation.getTransportValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.getTransportValue());
    }

    @Test
    public void testGetSupportValue() {
        spyReputation.initValues();
        assertEquals(-5, spyReputation.getSupportValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.getSupportValue());
    }

    @Test
    public void testGetFinancialValue() {
        spyReputation.initValues();
        assertEquals(0, spyReputation.getFinancialValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.getFinancialValue());
    }

    @Test
    public void testCalculateUnitRatingScore() {
        spyReputation.initValues();
        assertEquals(38, spyReputation.calculateUnitRatingScore());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.calculateUnitRatingScore());
    }

    @Test
    public void testGetReputationModifier() {
        spyReputation.initValues();
        assertEquals(3, spyReputation.getModifier());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(0, spyReputation.getModifier());
    }

    @Test
    public void testGetAverageExperience() {
        spyReputation.initValues();
        assertEquals(SkillType.getExperienceLevelName(SkillType.EXP_VETERAN),
                     spyReputation.getAverageExperience());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(SkillType.getExperienceLevelName(-1),
                     spyReputation.getAverageExperience());
    }

    @Test
    public void testGetDetails() {
        String expectedDetails =
                "Unit Reputation:    38\n" +
                "    Method: Campaign Operations\n" +
                "\n" +
                "Experience:          10\n" +
                "    Average Experience:     Regular\n" +
                "        #Regular:                    16\n" +
                "\n" +
                "Commander:           13 (null)\n" +
                "    Leadership:               4\n" +
                "    Negotiation:              5\n" +
                "    Strategy:                 2\n" +
                "    Tactics:                  2\n" +
                "\n" +
                "Combat Record:        0\n" +
                "    Successful Missions:      0\n" +
                "    Failed Missions:          0\n" +
                "    Contract Breaches:        0\n" +
                "\n" +
                "Transportation:      20\n" +
                "    Mech Bays:                   4 needed /   4 available\n" +
                "    Fighter Bays:                2 needed /   2 available\n" +
                "    Small Craft Bays:            0 needed /   0 available\n" +
                "    Protomech Bays:              0 needed /   0 available\n" +
                "    Heavy Vehicle Bays:          0 needed /   0 available\n" +
                "    Light Vehicle Bays:          8 needed /  22 available (plus 0 excess heavy)\n" +
                "    BA Bays:                     0 needed /   0 available\n" +
                "    Infantry Bays:               1 needed /   4 available\n" +
                "    Docking Collars:             1 needed /   4 available\n" +
                "    Has Jumpships?             Yes\n" +
                "    Has Warships?               No\n" +
                "\n" +
                "Support:             -5\n" +
                "    Tech Support:\n" +
                "        Mech Techs:                   4 needed /    0 available\n" +
                "            NOTE: Protomechs and mechs use same techs.\n" +
                "        Aero Techs:                   2 needed /    0 available\n" +
                "        Mechanics:                    8 needed /    0 available\n" +
                "            NOTE: Vehicles and Infantry use the same mechanics.\n" +
                "        BA Techs:                     0 needed /    0 available\n" +
                "        Astechs:                     84 needed /   84 available\n" +
                "    Admin Support:                   10 needed /   10 available\n" +
                "    Large Craft Crew:\n" +
                "        All fully crewed.\n" +
                "\n" +
                "Financial           0\n" +
                "    In Debt?                 No\n" +
                "\n" +
                "Criminal Activity:  0 (MHQ does not currently track criminal activity.)\n" +
                "\n" +
                "Inactivity Modifier: 0 (MHQ does not track end dates for missions/contracts.)";
        spyReputation.initValues();
        assertEquals(expectedDetails, spyReputation.getDetails());

        // Test a brand new campaign.
        expectedDetails =
                "Unit Reputation:    0\n" +
                "    Method: Campaign Operations\n" +
                "\n" +
                "Experience:           0\n" +
                "    Average Experience:     Unknown\n" +
                "\n" +
                "\n" +
                "Commander:            0 \n" +
                "    Leadership:               0\n" +
                "    Negotiation:              0\n" +
                "    Strategy:                 0\n" +
                "    Tactics:                  0\n" +
                "\n" +
                "Combat Record:        0\n" +
                "    Successful Missions:      0\n" +
                "    Failed Missions:          0\n" +
                "    Contract Breaches:        0\n" +
                "\n" +
                "Transportation:       0\n" +
                "    Mech Bays:                   0 needed /   0 available\n" +
                "    Fighter Bays:                0 needed /   0 available\n" +
                "    Small Craft Bays:            0 needed /   0 available\n" +
                "    Protomech Bays:              0 needed /   0 available\n" +
                "    Heavy Vehicle Bays:          0 needed /   0 available\n" +
                "    Light Vehicle Bays:          0 needed /   0 available (plus 0 excess heavy)\n" +
                "    BA Bays:                     0 needed /   0 available\n" +
                "    Infantry Bays:               0 needed /   0 available\n" +
                "    Docking Collars:             0 needed /   0 available\n" +
                "    Has Jumpships?              No\n" +
                "    Has Warships?               No\n" +
                "\n" +
                "Support:              0\n" +
                "    Tech Support:\n" +
                "        Mech Techs:                   0 needed /    0 available\n" +
                "            NOTE: Protomechs and mechs use same techs.\n" +
                "        Aero Techs:                   0 needed /    0 available\n" +
                "        Mechanics:                    0 needed /    0 available\n" +
                "            NOTE: Vehicles and Infantry use the same mechanics.\n" +
                "        BA Techs:                     0 needed /    0 available\n" +
                "        Astechs:                      0 needed /    0 available\n" +
                "    Admin Support:                    0 needed /    0 available\n" +
                "    Large Craft Crew:\n" +
                "        All fully crewed.\n" +
                "\n" +
                "Financial           0\n" +
                "    In Debt?                 No\n" +
                "\n" +
                "Criminal Activity:  0 (MHQ does not currently track criminal activity.)\n" +
                "\n" +
                "Inactivity Modifier: 0 (MHQ does not track end dates for missions/contracts.)";
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(expectedDetails, spyReputation.getDetails());
    }

    @Test
    public void testCalcTechSupportValue() {
        assertEquals(0, spyReputation.calcTechSupportValue());

        // Test having techs and astechs without having any combat units.
        doReturn(12).when(mockCampaign).getNumberAstechs();
        doReturn(mockCampaign).when(spyReputation).getCampaign();
        ArrayList<Person> techs = new ArrayList<>(2);
        techs.add(mockThunderbolt1Tech);
        techs.add(mockThunderbolt2Tech);
        doReturn(techs).when(mockCampaign).getTechs();
        doReturn(0).when(spyReputation).getMechTechTeamsNeeded();
        doReturn(0).when(spyReputation).getMechanicTeamsNeeded();
        doReturn(0).when(spyReputation).getAeroTechTeamsNeeded();
        doReturn(0).when(spyReputation).getBattleArmorTechTeamsNeeded();
        assertEquals(0, spyReputation.calcTechSupportValue());

        // Test having techs without having any astechs.
        doReturn(0).when(mockCampaign).getNumberAstechs();
        doReturn(mockCampaign).when(spyReputation).getCampaign();
        techs = new ArrayList<>(2);
        techs.add(mockThunderbolt1Tech);
        techs.add(mockThunderbolt2Tech);
        doReturn(techs).when(mockCampaign).getTechs();
        doReturn(2).when(spyReputation).getMechTechTeamsNeeded();
        doReturn(0).when(spyReputation).getMechanicTeamsNeeded();
        doReturn(0).when(spyReputation).getAeroTechTeamsNeeded();
        doReturn(0).when(spyReputation).getBattleArmorTechTeamsNeeded();
        assertEquals(-5, spyReputation.calcTechSupportValue());

        // Test having astechs without having any techs.
        doReturn(12).when(mockCampaign).getNumberAstechs();
        doReturn(mockCampaign).when(spyReputation).getCampaign();
        doReturn(new ArrayList<>(0)).when(mockCampaign).getTechs();
        doReturn(2).when(spyReputation).getMechTechTeamsNeeded();
        doReturn(0).when(spyReputation).getMechanicTeamsNeeded();
        doReturn(0).when(spyReputation).getAeroTechTeamsNeeded();
        doReturn(0).when(spyReputation).getBattleArmorTechTeamsNeeded();
        assertEquals(-5, spyReputation.calcTechSupportValue());
    }

    @Test
    public void testGetTransportationDetails() {
        String expected = "Transportation:      20\n" +
                          "    Mech Bays:                   4 needed /   4 available\n" +
                          "    Fighter Bays:                2 needed /   2 available\n" +
                          "    Small Craft Bays:            0 needed /   0 available\n" +
                          "    Protomech Bays:              0 needed /   0 available\n" +
                          "    Heavy Vehicle Bays:          0 needed /   0 available\n" +
                          "    Light Vehicle Bays:          8 needed /  22 available (plus 0 excess heavy)\n" +
                          "    BA Bays:                     0 needed /   0 available\n" +
                          "    Infantry Bays:               1 needed /   4 available\n" +
                          "    Docking Collars:             1 needed /   4 available\n" +
                          "    Has Jumpships?             Yes\n" +
                          "    Has Warships?               No";
        spyReputation.initValues();
        assertEquals(expected, spyReputation.getTransportationDetails());

        // Add some heavy vehicles.
        expected = "Transportation:      10\n" +
                   "    Mech Bays:                   4 needed /   4 available\n" +
                   "    Fighter Bays:                2 needed /   2 available\n" +
                   "    Small Craft Bays:            0 needed /   0 available\n" +
                   "    Protomech Bays:              0 needed /   0 available\n" +
                   "    Heavy Vehicle Bays:          4 needed /   0 available\n" +
                   "    Light Vehicle Bays:          8 needed /  22 available (plus 0 excess heavy)\n" +
                   "    BA Bays:                     0 needed /   0 available\n" +
                   "    Infantry Bays:               1 needed /   4 available\n" +
                   "    Docking Collars:             1 needed /   4 available\n" +
                   "    Has Jumpships?             Yes\n" +
                   "    Has Warships?               No";
        doReturn(4).when(spyReputation).getHeavyVeeCount();
        assertEquals(expected, spyReputation.getTransportationDetails());

        // Add excess heavy vehicle bays.
        expected = "Transportation:      20\n" +
                   "    Mech Bays:                   4 needed /   4 available\n" +
                   "    Fighter Bays:                2 needed /   2 available\n" +
                   "    Small Craft Bays:            0 needed /   0 available\n" +
                   "    Protomech Bays:              0 needed /   0 available\n" +
                   "    Heavy Vehicle Bays:          4 needed /   8 available\n" +
                   "    Light Vehicle Bays:          8 needed /  22 available (plus 4 excess heavy)\n" +
                   "    BA Bays:                     0 needed /   0 available\n" +
                   "    Infantry Bays:               1 needed /   4 available\n" +
                   "    Docking Collars:             1 needed /   4 available\n" +
                   "    Has Jumpships?             Yes\n" +
                   "    Has Warships?               No";
        doReturn(8).when(spyReputation).getHeavyVeeBayCount();
        assertEquals(expected, spyReputation.getTransportationDetails());
    }
}

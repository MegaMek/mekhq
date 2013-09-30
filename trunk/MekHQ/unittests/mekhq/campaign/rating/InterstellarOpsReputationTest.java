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
import junit.framework.TestCase;
import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.Bay;
import megamek.common.BipedMech;
import megamek.common.DockingCollar;
import megamek.common.Dropship;
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
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/28/13 11:20 AM
 */
@RunWith(JUnit4.class)
public class InterstellarOpsReputationTest {

    // Test for is based off example of Arnold's force in Interstellar Ops Beta.

    Campaign mockCampaign = Mockito.mock(Campaign.class);
    ArrayList<Unit> unitList = new ArrayList<Unit>();
    ArrayList<Person> personnelList = new ArrayList<Person>();
    ArrayList<Mission> missionList = new ArrayList<Mission>();

    // Mothballed units.
    Unit mockMechMothballed = Mockito.mock(Unit.class);
    Unit mockAeroMothballed = Mockito.mock(Unit.class);
    Unit mockTankMothballed = Mockito.mock(Unit.class);

    // Mechs
    Skill mockMechGunnery = Mockito.mock(Skill.class);
    Skill mockMechPilot = Mockito.mock(Skill.class);
    Skill mockLeader = Mockito.mock(Skill.class);
    Skill mockTactics = Mockito.mock(Skill.class);
    Skill mockStrategy = Mockito.mock(Skill.class);
    Skill mockNegotiation = Mockito.mock(Skill.class);
    BipedMech mockThunderbolt1 = Mockito.mock(BipedMech.class);
    Unit mockThunderboltUnit1 = Mockito.mock(Unit.class);
    Person mockThunderbolt1Pilot = Mockito.mock(Person.class);
    Person mockThunderbolt1Tech = Mockito.mock(Person.class);
    BipedMech mockThunderbolt2 = Mockito.mock(BipedMech.class);
    Unit mockThunderboltUnit2 = Mockito.mock(Unit.class);
    Person mockThunderbolt2Pilot = Mockito.mock(Person.class);
    Person mockThunderbolt2Tech = Mockito.mock(Person.class);
    BipedMech mockGrasshopper1 = Mockito.mock(BipedMech.class);
    Unit mockGrasshopperUnit1 = Mockito.mock(Unit.class);
    Person mockGrasshopper1Pilot = Mockito.mock(Person.class);
    Person mockGrasshopper1Tech = Mockito.mock(Person.class);
    BipedMech mockGrasshopper2 = Mockito.mock(BipedMech.class);
    Unit mockGrasshopperUnit2 = Mockito.mock(Unit.class);
    Person mockGrasshopper2Pilot = Mockito.mock(Person.class);
    Person mockGrasshopper2Tech = Mockito.mock(Person.class);

    // Tanks
    Skill mockTankGunnery = Mockito.mock(Skill.class);
    Skill mockTankPilot = Mockito.mock(Skill.class);
    Tank mockBulldog1 = Mockito.mock(Tank.class);
    Unit mockBulldogUnit1 = Mockito.mock(Unit.class);
    Person mockBulldog1Driver = Mockito.mock(Person.class);
    Person mockBulldog1Gunner1 = Mockito.mock(Person.class);
    Person mockBulldog1Gunner2 = Mockito.mock(Person.class);
    Person mockBulldog1Gunner3 = Mockito.mock(Person.class);
    Person mockBulldog1Tech = Mockito.mock(Person.class);
    Tank mockBulldog2 = Mockito.mock(Tank.class);
    Unit mockBulldogUnit2 = Mockito.mock(Unit.class);
    Person mockBulldog2Driver = Mockito.mock(Person.class);
    Person mockBulldog2Gunner1 = Mockito.mock(Person.class);
    Person mockBulldog2Gunner2 = Mockito.mock(Person.class);
    Person mockBulldog2Gunner3 = Mockito.mock(Person.class);
    Person mockBulldog2Tech = Mockito.mock(Person.class);
    Tank mockBulldog3 = Mockito.mock(Tank.class);
    Unit mockBulldogUnit3 = Mockito.mock(Unit.class);
    Person mockBulldog3Driver = Mockito.mock(Person.class);
    Person mockBulldog3Gunner1 = Mockito.mock(Person.class);
    Person mockBulldog3Gunner2 = Mockito.mock(Person.class);
    Person mockBulldog3Gunner3 = Mockito.mock(Person.class);
    Person mockBulldog3Tech = Mockito.mock(Person.class);
    Tank mockBulldog4 = Mockito.mock(Tank.class);
    Unit mockBulldogUnit4 = Mockito.mock(Unit.class);
    Person mockBulldog4Driver = Mockito.mock(Person.class);
    Person mockBulldog4Gunner1 = Mockito.mock(Person.class);
    Person mockBulldog4Gunner2 = Mockito.mock(Person.class);
    Person mockBulldog4Gunner3 = Mockito.mock(Person.class);
    Person mockBulldog4Tech = Mockito.mock(Person.class);
    Tank mockPackrat1 = Mockito.mock(Tank.class);
    Unit mockPackratUnit1 = Mockito.mock(Unit.class);
    Person mockPackrat1Driver = Mockito.mock(Person.class);
    Person mockPackrat1Gunner = Mockito.mock(Person.class);
    Person mockPackrat1Tech = Mockito.mock(Person.class);
    Tank mockPackrat2 = Mockito.mock(Tank.class);
    Unit mockPackratUnit2 = Mockito.mock(Unit.class);
    Person mockPackrat2Driver = Mockito.mock(Person.class);
    Person mockPackrat2Gunner = Mockito.mock(Person.class);
    Person mockPackrat2Tech = Mockito.mock(Person.class);
    Tank mockPackrat3 = Mockito.mock(Tank.class);
    Unit mockPackratUnit3 = Mockito.mock(Unit.class);
    Person mockPackrat3Driver = Mockito.mock(Person.class);
    Person mockPackrat3Gunner = Mockito.mock(Person.class);
    Person mockPackrat3Tech = Mockito.mock(Person.class);
    Tank mockPackrat4 = Mockito.mock(Tank.class);
    Unit mockPackratUnit4 = Mockito.mock(Unit.class);
    Person mockPackrat4Driver = Mockito.mock(Person.class);
    Person mockPackrat4Gunner = Mockito.mock(Person.class);
    Person mockPackrat4Tech = Mockito.mock(Person.class);

    // Infantry
    Skill mockInfantryGunnery = Mockito.mock(Skill.class);
    Infantry mockLaserPlatoon = Mockito.mock(Infantry.class);
    Unit mockLaserPlatoonUnit = Mockito.mock(Unit.class);
    Collection<Person> infantryPersonnel = new HashSet<Person>(28);

    // Fighters
    Skill mockAeroGunnery = Mockito.mock(Skill.class);
    Skill mockAeroPilot = Mockito.mock(Skill.class);
    Aero mockCorsair1 = Mockito.mock(Aero.class);
    Unit mockCorsairUnit1 = Mockito.mock(Unit.class);
    Person mockCorsair1Pilot = Mockito.mock(Person.class);
    Person getMockCorsair1Tech = Mockito.mock(Person.class);
    Aero mockCorsair2 = Mockito.mock(Aero.class);
    Unit mockCorsairUnit2 = Mockito.mock(Unit.class);
    Person mockCorsair2Pilot = Mockito.mock(Person.class);
    Person getMockCorsair2Tech = Mockito.mock(Person.class);

    // Dropships
    Skill mockDropGunnery = Mockito.mock(Skill.class);
    Skill mockDropPilot = Mockito.mock(Skill.class);
    Dropship mockSeeker = Mockito.mock(Dropship.class);
    Unit mockSeekerUnit = Mockito.mock(Unit.class);
    Collection<Person> seekerCrew = new HashSet<Person>(20);

    // Jumpships
    Skill mockJumpGunnery = Mockito.mock(Skill.class);
    Skill mockJumpPilot = Mockito.mock(Skill.class);
    Jumpship mockInvader = Mockito.mock(Jumpship.class);
    Unit mockInvaderUnit = Mockito.mock(Unit.class);
    Collection<Person> invaderCrew = new HashSet<Person>(24);

    // Techs
    Skill mockMechTechSkillRegular = Mockito.mock(Skill.class);
    Skill mockMechTechSkillElite = Mockito.mock(Skill.class);
    Skill mockFighterTechSkill = Mockito.mock(Skill.class);
    Skill mockFighterTechSkillElite = Mockito.mock(Skill.class);
    Skill mockVeeTechSkill = Mockito.mock(Skill.class);
    Collection<Person> regularAdmins = new HashSet<Person>(10);

    // Finances
    Finances mockFinances = Mockito.mock(Finances.class);

    InterstellarOpsReputation spyReputation = Mockito.spy(new InterstellarOpsReputation(mockCampaign));

    @Before
    public void setUp() {
        int astechs = 0;
        mockCampaign = Mockito.mock(Campaign.class);
        unitList = new ArrayList<Unit>();
        personnelList = new ArrayList<Person>();
        missionList = new ArrayList<Mission>();
        infantryPersonnel = new HashSet<Person>(28);
        seekerCrew = new HashSet<Person>(20);
        invaderCrew = new HashSet<Person>(24);
        regularAdmins = new HashSet<Person>(10);
        spyReputation = Mockito.spy(new InterstellarOpsReputation(mockCampaign));

        Mockito.when(mockMechMothballed.isMothballed()).thenReturn(true);

        Mockito.when(mockAeroMothballed.isMothballed()).thenReturn(true);

        Mockito.when(mockTankMothballed.isMothballed()).thenReturn(true);

        Mockito.when(mockMechGunnery.getLevel()).thenReturn(4);
        Mockito.when(mockMechPilot.getLevel()).thenReturn(5);
        Mockito.when(mockTankGunnery.getLevel()).thenReturn(4);
        Mockito.when(mockTankPilot.getLevel()).thenReturn(5);
        Mockito.when(mockInfantryGunnery.getLevel()).thenReturn(4);
        Mockito.when(mockAeroGunnery.getLevel()).thenReturn(4);
        Mockito.when(mockAeroPilot.getLevel()).thenReturn(5);
        Mockito.when(mockDropGunnery.getLevel()).thenReturn(4);
        Mockito.when(mockDropPilot.getLevel()).thenReturn(5);
        Mockito.when(mockJumpGunnery.getLevel()).thenReturn(4);
        Mockito.when(mockJumpPilot.getLevel()).thenReturn(5);
        Mockito.when(mockLeader.getLevel()).thenReturn(4);
        Mockito.when(mockTactics.getLevel()).thenReturn(2);
        Mockito.when(mockStrategy.getLevel()).thenReturn(2);
        Mockito.when(mockNegotiation.getLevel()).thenReturn(5);
        Mockito.when(mockMechTechSkillRegular.getLevel()).thenReturn(7);
        Mockito.when(mockMechTechSkillElite.getLevel()).thenReturn(5);
        Mockito.when(mockFighterTechSkill.getLevel()).thenReturn(7);
        Mockito.when(mockFighterTechSkillElite.getLevel()).thenReturn(5);
        Mockito.when(mockVeeTechSkill.getLevel()).thenReturn(7);

        Mockito.when(mockThunderboltUnit1.getEntity()).thenReturn(mockThunderbolt1);
        Mockito.when(mockThunderbolt1Pilot.isAdmin()).thenReturn(false);
        Mockito.when(mockThunderbolt1Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        Mockito.when(mockThunderbolt1Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockThunderbolt1Pilot);
        mockThunderboltUnit1.addPilotOrSoldier(mockThunderbolt1Pilot);
        ArrayList<Person> crew = new ArrayList<Person>(1);
        crew.add(mockThunderbolt1Pilot);
        Mockito.when(mockThunderboltUnit1.getCrew()).thenReturn(crew);
        Mockito.when(mockThunderbolt1Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockThunderbolt1Tech.isTech()).thenReturn(true);
        Mockito.when(mockThunderbolt1Tech.isActive()).thenReturn(true);
        Mockito.when(mockThunderbolt1Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockThunderbolt1Tech);
        astechs += 6;

        Mockito.when(mockThunderboltUnit2.getEntity()).thenReturn(mockThunderbolt2);
        Mockito.when(mockThunderbolt2Pilot.isAdmin()).thenReturn(false);
        Mockito.when(mockThunderbolt2Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        Mockito.when(mockThunderbolt2Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockThunderbolt2Pilot);
        mockThunderboltUnit2.addPilotOrSoldier(mockThunderbolt2Pilot);
        crew = new ArrayList<Person>(1);
        crew.add(mockThunderbolt2Pilot);
        Mockito.when(mockThunderboltUnit2.getCrew()).thenReturn(crew);
        Mockito.when(mockThunderbolt2Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockThunderbolt2Tech.isTech()).thenReturn(true);
        Mockito.when(mockThunderbolt2Tech.isActive()).thenReturn(true);
        Mockito.when(mockThunderbolt2Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockThunderbolt2Tech);
        astechs += 6;

        Mockito.when(mockGrasshopperUnit1.getEntity()).thenReturn(mockGrasshopper1);
        Mockito.when(mockGrasshopper1Pilot.isAdmin()).thenReturn(false);
        Mockito.when(mockGrasshopper1Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        Mockito.when(mockGrasshopper1Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockGrasshopper1Pilot);
        mockGrasshopperUnit1.addPilotOrSoldier(mockGrasshopper1Pilot);
        crew = new ArrayList<Person>(1);
        crew.add(mockGrasshopper1Pilot);
        Mockito.when(mockGrasshopperUnit1.getCrew()).thenReturn(crew);
        Mockito.when(mockGrasshopper1Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockGrasshopper1Tech.isTech()).thenReturn(true);
        Mockito.when(mockGrasshopper1Tech.isActive()).thenReturn(true);
        Mockito.when(mockGrasshopper1Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockGrasshopper1Tech);
        astechs += 6;

        Mockito.when(mockGrasshopperUnit2.getEntity()).thenReturn(mockGrasshopper2);
        Mockito.when(mockGrasshopper2Pilot.isAdmin()).thenReturn(false);
        Mockito.when(mockGrasshopper2Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        Mockito.when(mockGrasshopper2Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        Mockito.when(mockGrasshopper2Pilot.getSkill(SkillType.S_LEADER)).thenReturn(mockLeader);
        Mockito.when(mockGrasshopper2Pilot.getSkill(SkillType.S_TACTICS)).thenReturn(mockTactics);
        Mockito.when(mockGrasshopper2Pilot.getSkill(SkillType.S_STRATEGY)).thenReturn(mockStrategy);
        Mockito.when(mockGrasshopper2Pilot.getSkill(SkillType.S_NEG)).thenReturn(mockNegotiation);
        personnelList.add(mockGrasshopper2Pilot);
        mockGrasshopperUnit2.addPilotOrSoldier(mockGrasshopper2Pilot);
        crew = new ArrayList<Person>(1);
        crew.add(mockGrasshopper2Pilot);
        Mockito.when(mockGrasshopperUnit2.getCrew()).thenReturn(crew);
        Mockito.when(mockGrasshopper2Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockGrasshopper2Tech.isTech()).thenReturn(true);
        Mockito.when(mockGrasshopper2Tech.isActive()).thenReturn(true);
        Mockito.when(mockGrasshopper2Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillElite);
        personnelList.add(mockGrasshopper2Tech);
        astechs += 6;

        Mockito.when(mockBulldogUnit1.getEntity()).thenReturn(mockBulldog1);
        Mockito.when(mockBulldog1Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockBulldog1Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog1Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog1Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog1Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog1Driver.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog1Gunner1.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog1Gunner2.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog1Gunner3.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog1Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog1Tech.isTech()).thenReturn(true);
        Mockito.when(mockBulldog1Tech.isActive()).thenReturn(true);
        Mockito.when(mockBulldog1Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog1Driver);
        personnelList.add(mockBulldog1Gunner1);
        personnelList.add(mockBulldog2Gunner1);
        personnelList.add(mockBulldog1Gunner3);
        personnelList.add(mockBulldog1Tech);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Driver);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner1);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner2);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner3);
        crew = new ArrayList<Person>(4);
        crew.add(mockBulldog1Driver);
        crew.add(mockBulldog1Gunner1);
        crew.add(mockBulldog1Gunner2);
        crew.add(mockBulldog1Gunner3);
        Mockito.when(mockBulldogUnit1.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockBulldog2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockBulldog2Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog2Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog2Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog2Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldogUnit2.getEntity()).thenReturn(mockBulldog2);
        Mockito.when(mockBulldog2Driver.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechGunnery);
        personnelList.add(mockBulldog2Driver);
        Mockito.when(mockBulldog2Gunner1.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Gunner1);
        Mockito.when(mockBulldog2Gunner2.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Gunner1);
        Mockito.when(mockBulldog2Gunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog2Gunner3);
        Mockito.when(mockBulldog2Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog2Tech.isTech()).thenReturn(true);
        Mockito.when(mockBulldog2Tech.isActive()).thenReturn(true);
        Mockito.when(mockBulldog2Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog2Tech);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Driver);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner1);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner2);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner3);
        crew = new ArrayList<Person>(4);
        crew.add(mockBulldog2Driver);
        crew.add(mockBulldog2Gunner1);
        crew.add(mockBulldog2Gunner2);
        crew.add(mockBulldog2Gunner3);
        Mockito.when(mockBulldogUnit2.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockBulldog3Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockBulldog3Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog3Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog3Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog3Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldogUnit3.getEntity()).thenReturn(mockBulldog3);
        Mockito.when(mockBulldog3Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Driver);
        Mockito.when(mockBulldog3Gunner1.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Gunner1);
        Mockito.when(mockBulldog3Gunner2.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Gunner1);
        Mockito.when(mockBulldog3Gunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog3Gunner3);
        Mockito.when(mockBulldog3Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog3Tech.isTech()).thenReturn(true);
        Mockito.when(mockBulldog3Tech.isActive()).thenReturn(true);
        Mockito.when(mockBulldog3Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog3Tech);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Driver);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner1);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner2);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner3);
        crew = new ArrayList<Person>(4);
        crew.add(mockBulldog3Driver);
        crew.add(mockBulldog3Gunner1);
        crew.add(mockBulldog3Gunner2);
        crew.add(mockBulldog3Gunner3);
        Mockito.when(mockBulldogUnit3.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockBulldog4Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockBulldog4Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog4Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog4Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldog4Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockBulldogUnit4.getEntity()).thenReturn(mockBulldog4);
        Mockito.when(mockBulldog4Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Driver);
        Mockito.when(mockBulldog4Gunner1.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Gunner1);
        Mockito.when(mockBulldog4Gunner2.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Gunner1);
        Mockito.when(mockBulldog4Gunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockBulldog4Gunner3);
        Mockito.when(mockBulldog4Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockBulldog4Tech.isTech()).thenReturn(true);
        Mockito.when(mockBulldog4Tech.isActive()).thenReturn(true);
        Mockito.when(mockBulldog4Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog4Tech);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Driver);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner1);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner2);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner3);
        crew = new ArrayList<Person>(4);
        crew.add(mockBulldog4Driver);
        crew.add(mockBulldog4Gunner1);
        crew.add(mockBulldog4Gunner2);
        crew.add(mockBulldog4Gunner3);
        Mockito.when(mockBulldogUnit4.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockPackrat1Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockPackrat1Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackrat1Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackratUnit1.getEntity()).thenReturn(mockPackrat1);
        Mockito.when(mockPackrat1Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat1Driver);
        Mockito.when(mockPackrat1Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat1Gunner);
        Mockito.when(mockPackrat1Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockPackrat1Tech.isTech()).thenReturn(true);
        Mockito.when(mockPackrat1Tech.isActive()).thenReturn(true);
        Mockito.when(mockPackrat1Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat1Tech);
        mockPackratUnit1.addPilotOrSoldier(mockPackrat1Driver);
        mockPackratUnit1.addPilotOrSoldier(mockPackrat1Gunner);
        crew = new ArrayList<Person>(2);
        crew.add(mockPackrat1Driver);
        crew.add(mockPackrat1Gunner);
        Mockito.when(mockPackratUnit1.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockPackrat2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockPackrat2Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackrat2Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackratUnit2.getEntity()).thenReturn(mockPackrat2);
        Mockito.when(mockPackrat2Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat2Driver);
        Mockito.when(mockPackrat2Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat2Gunner);
        Mockito.when(mockPackrat2Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockPackrat2Tech.isTech()).thenReturn(true);
        Mockito.when(mockPackrat2Tech.isActive()).thenReturn(true);
        Mockito.when(mockPackrat2Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat2Tech);
        mockPackratUnit2.addPilotOrSoldier(mockPackrat2Driver);
        mockPackratUnit2.addPilotOrSoldier(mockPackrat2Gunner);
        crew = new ArrayList<Person>(2);
        crew.add(mockPackrat2Driver);
        crew.add(mockPackrat2Gunner);
        Mockito.when(mockPackratUnit2.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockPackrat3Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockPackrat3Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackrat3Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackratUnit3.getEntity()).thenReturn(mockPackrat3);
        Mockito.when(mockPackrat3Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat3Driver);
        Mockito.when(mockPackrat3Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat3Gunner);
        Mockito.when(mockPackrat3Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockPackrat3Tech.isTech()).thenReturn(true);
        Mockito.when(mockPackrat3Tech.isActive()).thenReturn(true);
        Mockito.when(mockPackrat3Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat3Tech);
        mockPackratUnit3.addPilotOrSoldier(mockPackrat3Driver);
        mockPackratUnit3.addPilotOrSoldier(mockPackrat3Gunner);
        crew = new ArrayList<Person>(2);
        crew.add(mockPackrat3Driver);
        crew.add(mockPackrat3Gunner);
        Mockito.when(mockPackratUnit3.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockPackrat4Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockMechPilot);
        Mockito.when(mockPackrat4Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackrat4Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockMechGunnery);
        Mockito.when(mockPackratUnit4.getEntity()).thenReturn(mockPackrat4);
        Mockito.when(mockPackrat4Driver.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat4Driver);
        Mockito.when(mockPackrat4Gunner.isAdmin()).thenReturn(false);
        personnelList.add(mockPackrat4Gunner);
        Mockito.when(mockPackrat4Tech.isAdmin()).thenReturn(false);
        Mockito.when(mockPackrat4Tech.isTech()).thenReturn(true);
        Mockito.when(mockPackrat4Tech.isActive()).thenReturn(true);
        Mockito.when(mockPackrat4Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat4Tech);
        mockPackratUnit4.addPilotOrSoldier(mockPackrat4Driver);
        mockPackratUnit4.addPilotOrSoldier(mockPackrat4Gunner);
        crew = new ArrayList<Person>(2);
        crew.add(mockPackrat4Driver);
        crew.add(mockPackrat4Gunner);
        Mockito.when(mockPackratUnit4.getCrew()).thenReturn(crew);
        astechs += 6;

        Mockito.when(mockLaserPlatoon.getSquadSize()).thenReturn(7);
        Mockito.when(mockLaserPlatoon.getSquadN()).thenReturn(4);
        Mockito.when(mockLaserPlatoonUnit.getEntity()).thenReturn(mockLaserPlatoon);
        crew = new ArrayList<Person>(28);
        for (int i = 0; i < 28; i++) {
            Person mockInfantry = Mockito.mock(Person.class);
            Mockito.when(mockInfantry.isAdmin()).thenReturn(false);
            Mockito.when(mockInfantry.getSkill(SkillType.S_SMALL_ARMS)).thenReturn(mockInfantryGunnery);
            infantryPersonnel.add(mockInfantry);
            mockLaserPlatoonUnit.addPilotOrSoldier(mockInfantry);
            crew.add(mockInfantry);
        }
        Mockito.when(mockLaserPlatoonUnit.getCrew()).thenReturn(crew);
        personnelList.addAll(infantryPersonnel);

        Mockito.when(mockCorsairUnit1.getEntity()).thenReturn(mockCorsair1);
        Mockito.when(mockCorsair1Pilot.isAdmin()).thenReturn(false);
        Mockito.when(mockCorsair1Pilot.getSkill(SkillType.S_GUN_AERO)).thenReturn(mockAeroGunnery);
        Mockito.when(mockCorsair1Pilot.getSkill(SkillType.S_PILOT_AERO)).thenReturn(mockAeroPilot);
        personnelList.add(mockCorsair1Pilot);
        mockCorsairUnit1.addPilotOrSoldier(mockCorsair1Pilot);
        crew = new ArrayList<Person>(1);
        crew.add(mockCorsair1Pilot);
        Mockito.when(mockCorsairUnit1.getCrew()).thenReturn(crew);
        Mockito.when(getMockCorsair1Tech.isAdmin()).thenReturn(false);
        Mockito.when(getMockCorsair1Tech.isTech()).thenReturn(true);
        Mockito.when(getMockCorsair1Tech.isActive()).thenReturn(true);
        Mockito.when(getMockCorsair1Tech.getSkill(SkillType.S_TECH_AERO)).thenReturn(mockFighterTechSkill);
        personnelList.add(getMockCorsair1Tech);
        astechs += 6;

        Mockito.when(mockCorsairUnit2.getEntity()).thenReturn(mockCorsair2);
        Mockito.when(mockCorsair2Pilot.isAdmin()).thenReturn(false);
        Mockito.when(mockCorsair2Pilot.getSkill(SkillType.S_GUN_AERO)).thenReturn(mockAeroGunnery);
        Mockito.when(mockCorsair2Pilot.getSkill(SkillType.S_PILOT_AERO)).thenReturn(mockAeroPilot);
        personnelList.add(mockCorsair2Pilot);
        mockCorsairUnit2.addPilotOrSoldier(mockCorsair2Pilot);
        crew = new ArrayList<Person>(1);
        crew.add(mockCorsair2Pilot);
        Mockito.when(mockCorsairUnit2.getCrew()).thenReturn(crew);
        Mockito.when(getMockCorsair2Tech.isAdmin()).thenReturn(false);
        Mockito.when(getMockCorsair2Tech.isTech()).thenReturn(true);
        Mockito.when(getMockCorsair2Tech.isActive()).thenReturn(true);
        Mockito.when(getMockCorsair2Tech.getSkill(SkillType.S_TECH_AERO)).thenReturn(mockFighterTechSkillElite);
        personnelList.add(getMockCorsair2Tech);
        astechs += 6;

        Mockito.when(mockSeekerUnit.getEntity()).thenReturn(mockSeeker);
        Bay transportBay;
        Vector<Bay> bayList = new Vector<Bay>();
        for (int i = 0; i < 4; i++) {
            transportBay = new MechBay(100.0, 1, i);
            bayList.add(transportBay);
        }
        for (int i = 0; i < 2; i++) {
            transportBay = new ASFBay(100.0, 0, i);
            bayList.add(transportBay);
        }
        for (int i = 0; i < 22; i++) {
            transportBay = new LightVehicleBay(50.0, 0, i);
            bayList.add(transportBay);
        }
        for (int i = 0; i < 4; i++) {
            transportBay = new InfantryBay(4.0, 0, i);
            bayList.add(transportBay);
        }
        Mockito.when(mockSeeker.getTransportBays()).thenReturn(bayList);
        crew = new ArrayList<Person>(20);
        for (int i = 0; i < 20; i++) {
            Person mockCrew = Mockito.mock(Person.class);
            Mockito.when(mockCrew.isAdmin()).thenReturn(false);
            Mockito.when(mockCrew.getSkill(SkillType.S_GUN_SPACE)).thenReturn(mockDropGunnery);
            if (i == 0) {
                Mockito.when(mockCrew.getSkill(SkillType.S_PILOT_SPACE)).thenReturn(mockDropPilot);
            }
            seekerCrew.add(mockCrew);
            mockSeekerUnit.addPilotOrSoldier(mockCrew);
            crew.add(mockCrew);
        }
        personnelList.addAll(seekerCrew);
        Mockito.when(mockSeekerUnit.getCrew()).thenReturn(crew);
        Mockito.when(mockSeekerUnit.getActiveCrew()).thenReturn(crew);
        Mockito.when(mockSeekerUnit.getFullCrewSize()).thenReturn(20);

        Mockito.when(mockInvaderUnit.getEntity()).thenReturn(mockInvader);
        DockingCollar collar;
        Vector<DockingCollar> collarList = new Vector<DockingCollar>(4);
        for (int i = 0; i < 4; i++) {
            collar = Mockito.mock(DockingCollar.class);
            collarList.add(collar);
        }
        Mockito.when(mockInvader.getTransportBays()).thenReturn(new Vector<Bay>(0));
        Mockito.when(mockInvader.getDockingCollars()).thenReturn(collarList);
        crew = new ArrayList<Person>(24);
        for (int i = 0; i < 24; i++) {
            Person mockCrew = Mockito.mock(Person.class);
            Mockito.when(mockCrew.isAdmin()).thenReturn(false);
            Mockito.when(mockCrew.getSkill(SkillType.S_GUN_SPACE)).thenReturn(mockDropGunnery);
            if (i == 0) {
                Mockito.when(mockCrew.getSkill(SkillType.S_PILOT_SPACE)).thenReturn(mockDropPilot);
            }
            invaderCrew.add(mockCrew);
            mockInvaderUnit.addPilotOrSoldier(mockCrew);
            crew.add(mockCrew);
        }
        personnelList.addAll(invaderCrew);
        Mockito.when(mockInvaderUnit.getCrew()).thenReturn(crew);
        Mockito.when(mockInvaderUnit.getActiveCrew()).thenReturn(crew);
        Mockito.when(mockInvaderUnit.getFullCrewSize()).thenReturn(24);

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

        for (int i = 0; i < 10; i++) {
            Person admin = Mockito.mock(Person.class);
            Mockito.when(admin.isAdmin()).thenReturn(true);
            Mockito.when(admin.isActive()).thenReturn(true);
            regularAdmins.add(admin);
        }
        personnelList.addAll(regularAdmins);

        Mockito.when(mockFinances.isInDebt()).thenReturn(false);

        Mockito.doReturn(unitList).when(mockCampaign).getUnits();
        Mockito.doReturn(personnelList).when(mockCampaign).getPersonnel();
        Mockito.doReturn(astechs).when(mockCampaign).getAstechPool();
        Mockito.doCallRealMethod().when(mockCampaign).getNumberAstechs();
        Mockito.doCallRealMethod().when(mockCampaign).getNumberPrimaryAstechs();
        Mockito.doCallRealMethod().when(mockCampaign).getNumberSecondaryAstechs();
        Mockito.doCallRealMethod().when(mockCampaign).getTechs();
        Mockito.doCallRealMethod().when(mockCampaign).getDoctors();
        Mockito.doCallRealMethod().when(mockCampaign).getAdmins();
        Mockito.doReturn(mockGrasshopper2Pilot).when(mockCampaign).getFlaggedCommander();
        Mockito.doReturn(missionList).when(mockCampaign).getMissions();
        Mockito.doReturn(mockFinances).when(mockCampaign).getFinances();
    }

    @Test
    public void testCalculateSupportNeeds() {

        // Test the example company.
        BigDecimal expectedTotalSkill = new BigDecimal("138.0");
        BigDecimal expectedAverageSkill = new BigDecimal("8.63");
        spyReputation.initValues();
        TestCase.assertEquals(4, spyReputation.getMechCount());
        TestCase.assertEquals(2, spyReputation.getFighterCount());
        TestCase.assertEquals(0, spyReputation.getProtoCount());
        TestCase.assertEquals(8, spyReputation.getVeeCount());
        TestCase.assertEquals(0, spyReputation.getBattleArmorCount());
        TestCase.assertEquals(28, spyReputation.getInfantryCount());
        TestCase.assertEquals(200, spyReputation.getNonAdminPersonnelCount());
        TestCase.assertEquals(1, spyReputation.getDropshipCount());
        BigDecimalAssert.assertEquals(expectedTotalSkill, spyReputation.getTotalSkill(), 2);
        TestCase.assertEquals(4, spyReputation.getMechTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getProtoTechTeamsNeeded());
        TestCase.assertEquals(2, spyReputation.getFighterTechTeamsNeeded());
        TestCase.assertEquals(8, spyReputation.getVeeTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getBattleArmorTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getInfantryTechTeamsNeeded());
        TestCase.assertEquals(20, spyReputation.getAdminsNeeded());
        TestCase.assertEquals(expectedAverageSkill, spyReputation.calcAverageExperience());
        TestCase.assertEquals(10, spyReputation.getExperienceValue());

        // Add a couple of mothballed units.
        unitList.add(mockMechMothballed);
        unitList.add(mockTankMothballed);
        unitList.add(mockAeroMothballed);
        TestCase.assertEquals(4, spyReputation.getMechCount());
        TestCase.assertEquals(2, spyReputation.getFighterCount());
        TestCase.assertEquals(0, spyReputation.getProtoCount());
        TestCase.assertEquals(8, spyReputation.getVeeCount());
        TestCase.assertEquals(0, spyReputation.getBattleArmorCount());
        TestCase.assertEquals(28, spyReputation.getInfantryCount());
        TestCase.assertEquals(200, spyReputation.getNonAdminPersonnelCount());
        TestCase.assertEquals(1, spyReputation.getDropshipCount());
        BigDecimalAssert.assertEquals(expectedTotalSkill, spyReputation.getTotalSkill(), 2);
        TestCase.assertEquals(4, spyReputation.getMechTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getProtoTechTeamsNeeded());
        TestCase.assertEquals(2, spyReputation.getFighterTechTeamsNeeded());
        TestCase.assertEquals(8, spyReputation.getVeeTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getBattleArmorTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getInfantryTechTeamsNeeded());
        TestCase.assertEquals(20, spyReputation.getAdminsNeeded());
        TestCase.assertEquals(expectedAverageSkill, spyReputation.calcAverageExperience());
        TestCase.assertEquals(10, spyReputation.getExperienceValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getMechCount());
        TestCase.assertEquals(0, spyReputation.getFighterCount());
        TestCase.assertEquals(0, spyReputation.getProtoCount());
        TestCase.assertEquals(0, spyReputation.getVeeCount());
        TestCase.assertEquals(0, spyReputation.getBattleArmorCount());
        TestCase.assertEquals(0, spyReputation.getInfantryCount());
        TestCase.assertEquals(0, spyReputation.getNonAdminPersonnelCount());
        TestCase.assertEquals(0, spyReputation.getDropshipCount());
        BigDecimalAssert.assertEquals(BigDecimal.ZERO, spyReputation.getTotalSkill(), 2);
        TestCase.assertEquals(0, spyReputation.getMechTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getProtoTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getFighterTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getVeeTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getBattleArmorTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getInfantryTechTeamsNeeded());
        TestCase.assertEquals(0, spyReputation.getAdminsNeeded());
        TestCase.assertEquals(BigDecimal.ZERO, spyReputation.calcAverageExperience());
        TestCase.assertEquals(0, spyReputation.getExperienceValue());
    }

    private void buildFreshCampaign() {
        Mockito.doReturn(new ArrayList<Unit>(0)).when(mockCampaign).getUnits();
        Mockito.doReturn(new ArrayList<Person>(0)).when(mockCampaign).getPersonnel();
        Mockito.doReturn(new ArrayList<Person>(0)).when(mockCampaign).getAdmins();
        Mockito.doReturn(new ArrayList<Person>(0)).when(mockCampaign).getTechs();
        Mockito.doReturn(new ArrayList<Person>(0)).when(mockCampaign).getDoctors();
        Mockito.doReturn(0).when(mockCampaign).getAstechPool();
        Mockito.doReturn(0).when(mockCampaign).getNumberAstechs();
        Mockito.doReturn(null).when(mockCampaign).getFlaggedCommander();
    }

    @Test
    public void testGetCommanderValue() {
        spyReputation.initValues();
        TestCase.assertEquals(13, spyReputation.getCommanderValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getCommanderValue());
    }

    @Test
    public void testGetCombatRecordValue() {
        // New company with no record.
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getCombatRecordValue());

        // Add a few missions.
        Mission winOne = Mockito.mock(Mission.class);
        Mockito.when(winOne.isActive()).thenReturn(false);
        Mockito.when(winOne.getStatus()).thenReturn(Mission.S_SUCCESS);
        missionList.add(winOne);
        Mission winTwo = Mockito.mock(Mission.class);
        Mockito.when(winTwo.isActive()).thenReturn(false);
        Mockito.when(winTwo.getStatus()).thenReturn(Mission.S_SUCCESS);
        missionList.add(winTwo);
        Mission winThree = Mockito.mock(Mission.class);
        Mockito.when(winThree.isActive()).thenReturn(false);
        Mockito.when(winThree.getStatus()).thenReturn(Mission.S_SUCCESS);
        missionList.add(winThree);
        Mission lossOne = Mockito.mock(Mission.class);
        Mockito.when(lossOne.isActive()).thenReturn(false);
        Mockito.when(lossOne.getStatus()).thenReturn(Mission.S_FAILED);
        missionList.add(lossOne);
        Mission active = Mockito.mock(Mission.class);
        Mockito.when(active.isActive()).thenReturn(true);
        Mockito.when(active.getStatus()).thenReturn(Mission.S_ACTIVE);
        missionList.add(active);
        TestCase.assertEquals(5, spyReputation.getCombatRecordValue());
    }

    @Test
    public void testGetTransportValue() {
        spyReputation.initValues();
        TestCase.assertEquals(20, spyReputation.getTransportValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getTransportValue());
    }

    @Test
    public void testGetSupportValue() {
        spyReputation.initValues();
        TestCase.assertEquals(-5, spyReputation.getSupportValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getSupportValue());
    }

    @Test
    public void testGetFinancialValue() {
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getFinancialValue());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getFinancialValue());
    }

    @Test
    public void testCalculateUnitRatingScore() {
        spyReputation.initValues();
        TestCase.assertEquals(38, spyReputation.calculateUnitRatingScore());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.calculateUnitRatingScore());
    }

    @Test
    public void testGetReputationModifier() {
        spyReputation.initValues();
        TestCase.assertEquals(3, spyReputation.getReputationModifier());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        TestCase.assertEquals(0, spyReputation.getReputationModifier());
    }
}

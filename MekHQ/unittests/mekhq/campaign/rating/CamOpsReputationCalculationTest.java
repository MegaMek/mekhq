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

import megamek.common.BipedMech;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Franklin "Brains" Moody (fmoody at moodman dot org)
 * @since 12/18/17 11:57 AM
 */
@SuppressWarnings("FieldCanBeLocal")
@RunWith(JUnit4.class)
public class CamOpsReputationCalculationTest {

/*
    private Campaign mockCampaign;
    private ArrayList<Unit> unitList;
    private ArrayList<Person> personnelList;
    private ArrayList<Person> activePersonnelList;
    private ArrayList<Mission> missionList;
*/
    
    public Campaign setupCampaign(ArrayList<Unit> unitList,
                      ArrayList<Person> personnelList,
                      ArrayList<Person> activePersonnelList,
                      int astechs,
                      Person mockCommander,
                      ArrayList<Mission> missionList,
                      Finances mockFinances) {
        Campaign mockCampaign = mock(Campaign.class);

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
        doReturn(mockCommander).when(mockCampaign).getFlaggedCommander();
        doReturn(missionList).when(mockCampaign).getMissions();
        doReturn(mockFinances).when(mockCampaign).getFinances();
        
        return mockCampaign;
    }

    public void testCampaign(Campaign testcampaign, 
                             int expectedExperienceRating, int expectedCommanderRating,
                             int expectedCombatRecordRating, int expectedTransportRating,
                             int expectedSupportRating, int expectedFinancialRating,
                             int expectedCrimeRating, int expectedIdleRating,
                             int expectedCompleteRating){
        CampaignOpsReputation campaign_reputation = spy(new CampaignOpsReputation(testcampaign));

        campaign_reputation.initValues();
        assertEquals("Experience Rating", expectedExperienceRating,
                     campaign_reputation.getExperienceValue());
        assertEquals("Commander Rating",expectedCommanderRating,
                     campaign_reputation.getCommanderValue());
        assertEquals("Combat Record Rating",expectedCombatRecordRating,
                     campaign_reputation.getCombatRecordValue());
        assertEquals("Transport Rating", expectedTransportRating,
                     campaign_reputation.getTransportValue());
        assertEquals("Support Rating", expectedSupportRating,
                     campaign_reputation.getSupportValue());
        assertEquals("Financial Rating", expectedFinancialRating,
                     campaign_reputation.getFinancialValue());
        // TODO Crimes are not currently handled
        //assertEquals("Crimes", expectedCrimeRating, campaign1_reputation.getCrimesPenalty());
        // TODO Idle time is not currently handled
        //assertEquals("Idle Time", expectedIdleRating, campaign1_reputation.getIdleTimeModifier());
        assertEquals("Complete Rating", expectedCompleteRating,
                     campaign_reputation.calculateUnitRatingScore());
    }

    
    // Campaign 1 //
    // Empty Campaign //
    public Campaign setupCampaign1(){
        ArrayList<Unit> unitList;
        ArrayList<Person> personnelList;
        ArrayList<Person> activePersonnelList;
        ArrayList<Mission> missionList;

        unitList = new ArrayList<>();
        personnelList = new ArrayList<>();
        activePersonnelList = new ArrayList<>();
        missionList = new ArrayList<>();
        int mockAstechs = 0;
        //Person mockCommander = mock(Person.class);
        Person mockCommander = null;
        Finances mockFinances = mock(Finances.class);
        
        return setupCampaign(unitList, personnelList, activePersonnelList, mockAstechs, mockCommander, missionList, mockFinances);
    }

    @Test
    public void testCampaign1()    {
        // Empty Campaign,
        testCampaign(setupCampaign1(),
                     0,
                     0,
                     0,
                     0,
                     0,
                     0,
                     0,
                     0,
                     0
                     );
    }

    void generateMockMech(ArrayList<Unit> unitList, ArrayList<Person> personnelList, String unitName, int Piloting, int Gunnery){
        BipedMech mockMech = mock(BipedMech.class);
        Unit mockMechUnit = mock(Unit.class);
        Person mockMechPilot = mock(Person.class);
        Skill mockMechGunnery = mock(Skill.class);
        Skill mockMechPiloting = mock(Skill.class);

        // Assign a fluff name to make things easier to identify
        when(mockMechUnit.getFluffName()).thenReturn(unitName);
        
        // Assign the given skill levels
        when(mockMechGunnery.getLevel()).thenReturn(Gunnery);
        when(mockMechPiloting.getLevel()).thenReturn(Piloting);
        
        // Mock out the unit
        when(mockMech.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockMechUnit.getEntity()).thenReturn(mockMech);
        when(mockMechPilot.isAdmin()).thenReturn(false);
        when(mockMechPilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockMechPilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPiloting);
        
        // Add the pilot
        personnelList.add(mockMechPilot);
        mockMechUnit.addPilotOrSoldier(mockMechPilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockMechPilot);
        when(mockMechUnit.getCrew()).thenReturn(crew);
        Crew mockMechCrew = mock(Crew.class);
        doReturn(mockMechPiloting.getLevel()).when(mockMechCrew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockMechCrew).getGunnery();
        when(mockMech.getCrew()).thenReturn(mockMechCrew);
    }

    void generateMockTank(ArrayList<Unit> unitList, ArrayList<Person> personnelList, String unitName, int Piloting, int Gunnery){
        Tank mockVehicle = mock(Tank.class);
        Unit mockVehicleUnit = mock(Unit.class);
        Skill mockTankGunnery = mock(Skill.class);
        Skill mockTankPiloting = mock(Skill.class);

        Person mockVehicleDriver = mock(Person.class);
        Person mockVehicleGunner1 = mock(Person.class);
        Person mockVehicleGunner2 = mock(Person.class);
        Person mockVehicleGunner3 = mock(Person.class);

        // Assign a fluff name to make things easier to identify
        when(mockVehicleUnit.getFluffName()).thenReturn(unitName);

        // Assign the given skill levels
        when(mockTankPiloting.getLevel()).thenReturn(Piloting);
        when(mockTankGunnery.getLevel()).thenReturn(Gunnery);
        
        when(mockVehicle.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockVehicleUnit.getEntity()).thenReturn(mockVehicle);
        when(mockVehicleDriver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPiloting);
        when(mockVehicleGunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockVehicleGunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockVehicleGunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockVehicleDriver.isAdmin()).thenReturn(false);
        when(mockVehicleGunner1.isAdmin()).thenReturn(false);
        when(mockVehicleGunner2.isAdmin()).thenReturn(false);
        when(mockVehicleGunner3.isAdmin()).thenReturn(false);
        personnelList.add(mockVehicleDriver);
        personnelList.add(mockVehicleGunner1);
        personnelList.add(mockVehicleGunner2);
        personnelList.add(mockVehicleGunner3);
        mockVehicleUnit.addPilotOrSoldier(mockVehicleDriver);
        mockVehicleUnit.addPilotOrSoldier(mockVehicleGunner1);
        mockVehicleUnit.addPilotOrSoldier(mockVehicleGunner2);
        mockVehicleUnit.addPilotOrSoldier(mockVehicleGunner3);
        ArrayList<Person> crew = new ArrayList<>(4);
        crew.add(mockVehicleDriver);
        crew.add(mockVehicleGunner1);
        crew.add(mockVehicleGunner2);
        crew.add(mockVehicleGunner3);
        when(mockVehicleUnit.getCrew()).thenReturn(crew);
        Crew mockVehicleCrew = mock(Crew.class);
        doReturn(mockTankPiloting.getLevel()).when(mockVehicleCrew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockVehicleCrew).getGunnery();
        when(mockVehicle.getCrew()).thenReturn(mockVehicleCrew);
    }

    public Campaign setupCampaign2(){
        // CamOps Example Unit - Arnold's small House Davion Combined Arms Company
        ArrayList<Unit> unitList = new ArrayList<>();
        ArrayList<Person> personnelList = new ArrayList<>();
        ArrayList<Person> activePersonnelList = new ArrayList<>();
        int astechs_count = 0;
        Person commander = mock(Person.class);
        ArrayList<Mission> missionList = new ArrayList<>();
        Finances finances = mock(Finances.class);
                
        // TDR-5S Thunderbolt #1 (Regular)
        generateMockMech(unitList, personnelList, "TDR-5S Thunderbolt #1", 5, 4);
        // TDR-5S Thunderbolt #2 (Regular)
        generateMockMech(unitList, personnelList,"TDR-5S Thunderbolt #2",5, 4);
        // GHR-5H Grasshopper #1 (Regular)
        generateMockMech(unitList, personnelList,"GHR-5H Grasshopper #1",5, 4);
        // GHR-5H Grasshopper #2 (Regular)
        generateMockMech(unitList, personnelList,"GHR-5H Grasshopper #2",5, 4);
        
        // Bulldog #1 (Regular)
        generateMockTank(unitList, personnelList, "Bulldog #1", 5, 4);
        // Bulldog #2 (Regular)
        generateMockTank(unitList, personnelList, "Bulldog #2", 5, 4);
        // Bulldog #3 (Regular)
        generateMockTank(unitList, personnelList, "Bulldog #3", 5, 4);
        // Bulldog #4 (Regular)
        generateMockTank(unitList, personnelList, "Bulldog #4", 5, 4);
        
        // Foot Laser Platoon (Regular)

        // Packrat #1 (Regular)
        // Packrat #2 (Regular)
        // Packrat #3 (Regular)
        // Packrat #4 (Regular)
        // CSR-V12 Corsair #1 (Regular)
        // CSR-V12 Corsair #2 (Regular)
        // Seeker DropShip (Regular)
        // Invader JumpShip (Veteran)
        // Tech Teams (12 Regular Teams, 2 Elite Teams)
        // Administrators (10)


        return setupCampaign(unitList, personnelList, activePersonnelList, astechs_count, commander, missionList, finances);
    }
    
//    @Test
    public void testCampaign2(){
        // CamOps Example Unit - Arnold's small House Davion Combined Arms Company
        testCampaign(setupCampaign2(),
                     10,
                     13,
                     0,
                     23,
                     0,
                     0,
                     0,
                     0,
                     46
        );
    }

/*
    @Test
    public void testCampaign3(){
        // CamOps Example Unit - Hannah's ex-ComStar merc unit
        testCampaign(setupCampaign3(),
                     10,
                     23,
                     0,
                     5,
                     0,
                     0,
                     0,
                     0,
                     38
        );
    }

    @Test
    public void testCampaign4(){
        // CamOps Example Unit - Jason's pirate^Wextralegal liberation force
        // TODO Change this to a proper pirate unit...
        testCampaign(setupCampaign4(),
                     5,
                     9,
                     0,
                     -5,
                     0,
                     0,
                     0,
                     0,
                     9
        );
    }
*/

}


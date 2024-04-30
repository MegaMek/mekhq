/*
 * FieldManualMercRevMrbcRating.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.rating;

import asserts.BigDecimalAssert;
import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Hangar;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/28/13 11:20 AM
 */
@SuppressWarnings(value = "FieldCanBeLocal")
public class CampaignOpsReputationTest {

    private Campaign mockCampaign;
    private CampaignOptions mockCampaignOptions;
    private Hangar mockHangar;
    private List<Unit> unitList;
    private List<Person> personnelList;
    private List<Person> activePersonnelList;
    private List<Mission> missionList;
    private List<Mission> completedMissionList;

    // Mothballed units.
    private Unit mockMechMothballed;
    private Unit mockAeroMothballed;
    private Unit mockTankMothballed;

    // Mechs
    private Skill mockMechGunnery;
    private Skill mockMechPilot;
    private Skill mockLeader ;
    private Skill mockTactics;
    private Skill mockStrategy;
    private Skill mockNegotiation;
    private BipedMech mockThunderbolt1;
    private Unit mockThunderboltUnit1;
    private Person mockThunderbolt1Pilot;
    private Person mockThunderbolt1Tech;
    private BipedMech mockThunderbolt2;
    private Unit mockThunderboltUnit2;
    private Person mockThunderbolt2Pilot;
    private Person mockThunderbolt2Tech;
    private BipedMech mockGrasshopper1;
    private Unit mockGrasshopperUnit1;
    private Person mockGrasshopper1Pilot;
    private Person mockGrasshopper1Tech;
    private BipedMech mockGrasshopper2;
    private Unit mockGrasshopperUnit2;
    private Person mockGrasshopper2Pilot;
    private Person mockGrasshopper2Tech;

    // Tanks
    private Skill mockTankGunnery;
    private Skill mockTankPilot;
    private Tank mockBulldog1;
    private Unit mockBulldogUnit1;
    private Person mockBulldog1Driver;
    private Person mockBulldog1Gunner1;
    private Person mockBulldog1Gunner2;
    private Person mockBulldog1Gunner3;
    private Person mockBulldog1Tech;
    private Tank mockBulldog2;
    private Unit mockBulldogUnit2;
    private Person mockBulldog2Driver;
    private Person mockBulldog2Gunner1;
    private Person mockBulldog2Gunner2;
    private Person mockBulldog2Gunner3;
    private Person mockBulldog2Tech;
    private Tank mockBulldog3;
    private Unit mockBulldogUnit3;
    private Person mockBulldog3Driver;
    private Person mockBulldog3Gunner1;
    private Person mockBulldog3Gunner2;
    private Person mockBulldog3Gunner3;
    private Person mockBulldog3Tech;
    private Tank mockBulldog4;
    private Unit mockBulldogUnit4;
    private Person mockBulldog4Driver;
    private Person mockBulldog4Gunner1;
    private Person mockBulldog4Gunner2;
    private Person mockBulldog4Gunner3;
    private Person mockBulldog4Tech;
    private Tank mockPackrat1;
    private Unit mockPackratUnit1;
    private Person mockPackrat1Driver;
    private Person mockPackrat1Gunner;
    private Person mockPackrat1Tech;
    private Tank mockPackrat2;
    private Unit mockPackratUnit2;
    private Person mockPackrat2Driver;
    private Person mockPackrat2Gunner;
    private Person mockPackrat2Tech;
    private Tank mockPackrat3;
    private Unit mockPackratUnit3;
    private Person mockPackrat3Driver;
    private Person mockPackrat3Gunner;
    private Person mockPackrat3Tech;
    private Tank mockPackrat4;
    private Unit mockPackratUnit4;
    private Person mockPackrat4Driver;
    private Person mockPackrat4Gunner;
    private Person mockPackrat4Tech;

    // Infantry
    private Skill mockInfantryGunnery;
    private Infantry mockLaserPlatoon;
    private Unit mockLaserPlatoonUnit;
    private Collection<Person> infantryPersonnel;

    // Fighters
    private Skill mockAeroGunnery;
    private Skill mockAeroPilot;
    private AeroSpaceFighter mockCorsair1;
    private Unit mockCorsairUnit1;
    private Person mockCorsair1Pilot;
    private Person mockCorsair1Tech;
    private AeroSpaceFighter mockCorsair2;
    private Unit mockCorsairUnit2;
    private Person mockCorsair2Pilot;
    private Person mockCorsair2Tech;

    // DropShips
    private Skill mockDropGunnery;
    private Skill mockDropPilot;
    private Dropship mockSeeker;
    private Unit mockSeekerUnit;
    private Collection<Person> seekerCrew;

    // JumpShips
    private Skill mockJumpGunnery;
    private Skill mockJumpPilot;
    private Jumpship mockInvader;
    private Unit mockInvaderUnit;
    private Collection<Person> invaderCrew;

    // Techs
    private Skill mockMechTechSkillRegular;
    private Skill mockMechTechSkillElite;
    private Skill mockFighterTechSkill;
    private Skill mockFighterTechSkillElite;
    private Skill mockVeeTechSkill;
    private Collection<Person> regularAdmins;

    // Finances
    private Finances mockFinances;

    private CampaignOpsReputation spyReputation;

    @BeforeEach
    public void setUp() {
        mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.is(any())).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        mockHangar = mock(Hangar.class);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);

        mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.getManualUnitRatingModifier()).thenReturn(0);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        spyReputation = spy(new CampaignOpsReputation(mockCampaign));

        int astechs = 0;
        unitList = new ArrayList<>();
        personnelList = new ArrayList<>();
        activePersonnelList = new ArrayList<>();
        missionList = new ArrayList<>();
        completedMissionList = new ArrayList<>();

        mockMothballed();
        mockSkills();
        astechs += mockThunderbolt1();
        astechs += mockThunderbolt2();
        astechs += mockGrasshopper1();
        astechs += mockGrasshopper2();
        astechs += mockBulldog1();
        astechs += mockBulldog2();
        astechs += mockBulldog3();
        astechs += mockBulldog4();
        astechs += mockPackrat1();
        astechs += mockPackrat2();
        astechs += mockPackrat3();
        astechs += mockPackrat4();
        mockLaserPlatoon();
        astechs += mockCorsair1();
        astechs += mockCorsair2();
        mockSeeker();
        mockInvader();

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

        regularAdmins = new HashSet<>(20);
        for (int i = 0; i < 20; i++) {
            Person admin = mock(Person.class);
            when(admin.isAdministrator()).thenReturn(true);
            when(admin.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
            when(admin.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            doReturn(PersonnelStatus.ACTIVE).when(admin).getStatus();
            regularAdmins.add(admin);
        }

        personnelList.addAll(regularAdmins);

        for (Person p : personnelList) {
            if (p.getStatus().isActive()) {
                activePersonnelList.add(p);
            }
        }

        mockFinances = mock(Finances.class);
        when(mockFinances.isInDebt()).thenReturn(false);

        when(mockHangar.getUnits()).thenReturn(unitList);
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
        doReturn(completedMissionList).when(mockCampaign).getCompletedMissions();
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
        assertEquals(200, spyReputation.getNonAdminPersonnelCount());
        assertEquals(1, spyReputation.getDropShipCount());
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
        assertEquals(200, spyReputation.getNonAdminPersonnelCount());
        assertEquals(1, spyReputation.getDropShipCount());
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
        assertEquals(0, spyReputation.getDropShipCount());
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
        when(mockHangar.getUnits()).thenReturn(Collections.emptyList());
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
        when(winOne.getStatus()).thenReturn(MissionStatus.SUCCESS);
        missionList.add(winOne);
        completedMissionList.add(winOne);
        Mission winTwo = mock(Mission.class);
        when(winTwo.getStatus()).thenReturn(MissionStatus.SUCCESS);
        missionList.add(winTwo);
        completedMissionList.add(winTwo);
        Mission winThree = mock(Mission.class);
        when(winThree.getStatus()).thenReturn(MissionStatus.SUCCESS);
        missionList.add(winThree);
        completedMissionList.add(winThree);
        Mission lossOne = mock(Mission.class);
        when(lossOne.getStatus()).thenReturn(MissionStatus.FAILED);
        missionList.add(lossOne);
        completedMissionList.add(lossOne);
        Mission active = mock(Mission.class);
        when(active.getStatus()).thenReturn(MissionStatus.ACTIVE);
        missionList.add(active);
        assertEquals(5, spyReputation.getCombatRecordValue());
    }

    @Test
    public void testGetTransportValue() {
        spyReputation.initValues();
        assertEquals(20, spyReputation.getTransportValue());

        // Test not having any DropShips (though we still have a JumpShip).
        doReturn(0).when(spyReputation).getDropShipCount();
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
        assertEquals(SkillLevel.REGULAR, spyReputation.getAverageExperience());

        // Test a brand new campaign.
        buildFreshCampaign();
        spyReputation.initValues();
        assertEquals(SkillLevel.NONE, spyReputation.getAverageExperience());
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
                "    Partial Missions:         0\n" +
                "    Failed Missions:          0\n" +
                "    Contract Breaches:        0\n" +
                "\n" +
                "Transportation:      20\n" +
                "    BattleMech Bays:             4 needed /   4 available\n" +
                "    Fighter Bays:                2 needed /   2 available (plus 0 excess Small Craft)\n" +
                "    Small Craft Bays:            0 needed /   0 available\n" +
                "    ProtoMech Bays:              0 needed /   0 available\n" +
                "    Super Heavy Vehicle Bays:    0 needed /   0 available\n" +
                "    Heavy Vehicle Bays:          0 needed /   0 available (plus 0 excess Super Heavy)\n" +
                "    Light Vehicle Bays:          8 needed /  22 available (plus 0 excess Heavy and 0 excess Super Heavy)\n" +
                "    Battle Armor Bays:           0 needed /   0 available\n" +
                "    Infantry Bays:               1 needed /   4 available\n" +
                "    Docking Collars:             1 needed /   4 available\n" +
                "    Has JumpShips?             Yes\n" +
                "    Has WarShips?               No\n" +
                "\n" +
                "Support:             -5\n" +
                "    Tech Support:\n" +
                "        Mech Techs:                   4 needed /    0 available\n" +
                "            NOTE: ProtoMechs and BattleMechs use same techs.\n" +
                "        Aero Techs:                   2 needed /    0 available\n" +
                "        Mechanics:                    8 needed /    0 available\n" +
                "            NOTE: Vehicles and Infantry use the same mechanics.\n" +
                "        Battle Armor Techs:           0 needed /    0 available\n" +
                "        Astechs:                     84 needed /   84 available\n" +
                "    Admin Support:                   10 needed /   20 available\n" +
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
                "    Average Experience:     None\n" +
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
                "    Partial Missions:         0\n" +
                "    Failed Missions:          0\n" +
                "    Contract Breaches:        0\n" +
                "\n" +
                "Transportation:       0\n" +
                "    BattleMech Bays:             0 needed /   0 available\n" +
                "    Fighter Bays:                0 needed /   0 available (plus 0 excess Small Craft)\n" +
                "    Small Craft Bays:            0 needed /   0 available\n" +
                "    ProtoMech Bays:              0 needed /   0 available\n" +
                "    Super Heavy Vehicle Bays:    0 needed /   0 available\n" +
                "    Heavy Vehicle Bays:          0 needed /   0 available (plus 0 excess Super Heavy)\n" +
                "    Light Vehicle Bays:          0 needed /   0 available (plus 0 excess Heavy and 0 excess Super Heavy)\n" +
                "    Battle Armor Bays:           0 needed /   0 available\n" +
                "    Infantry Bays:               0 needed /   0 available\n" +
                "    Docking Collars:             0 needed /   0 available\n" +
                "    Has JumpShips?              No\n" +
                "    Has WarShips?               No\n" +
                "\n" +
                "Support:              0\n" +
                "    Tech Support:\n" +
                "        Mech Techs:                   0 needed /    0 available\n" +
                "            NOTE: ProtoMechs and BattleMechs use same techs.\n" +
                "        Aero Techs:                   0 needed /    0 available\n" +
                "        Mechanics:                    0 needed /    0 available\n" +
                "            NOTE: Vehicles and Infantry use the same mechanics.\n" +
                "        Battle Armor Techs:           0 needed /    0 available\n" +
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
        List<Person> techs = new ArrayList<>(2);
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
                          "    BattleMech Bays:             4 needed /   4 available\n" +
                          "    Fighter Bays:                2 needed /   2 available (plus 0 excess Small Craft)\n" +
                          "    Small Craft Bays:            0 needed /   0 available\n" +
                          "    ProtoMech Bays:              0 needed /   0 available\n" +
                          "    Super Heavy Vehicle Bays:    0 needed /   0 available\n" +
                          "    Heavy Vehicle Bays:          0 needed /   0 available (plus 0 excess Super Heavy)\n" +
                          "    Light Vehicle Bays:          8 needed /  22 available (plus 0 excess Heavy and 0 excess Super Heavy)\n" +
                          "    Battle Armor Bays:           0 needed /   0 available\n" +
                          "    Infantry Bays:               1 needed /   4 available\n" +
                          "    Docking Collars:             1 needed /   4 available\n" +
                          "    Has JumpShips?             Yes\n" +
                          "    Has WarShips?               No";
        spyReputation.initValues();
        assertEquals(expected, spyReputation.getTransportationDetails());

        // Add some heavy vehicles.
        expected = "Transportation:      10\n" +
                   "    BattleMech Bays:             4 needed /   4 available\n" +
                   "    Fighter Bays:                2 needed /   2 available (plus 0 excess Small Craft)\n" +
                   "    Small Craft Bays:            0 needed /   0 available\n" +
                   "    ProtoMech Bays:              0 needed /   0 available\n" +
                   "    Super Heavy Vehicle Bays:    0 needed /   0 available\n" +
                   "    Heavy Vehicle Bays:          4 needed /   0 available (plus 0 excess Super Heavy)\n" +
                   "    Light Vehicle Bays:          8 needed /  22 available (plus 0 excess Heavy and 0 excess Super Heavy)\n" +
                   "    Battle Armor Bays:           0 needed /   0 available\n" +
                   "    Infantry Bays:               1 needed /   4 available\n" +
                   "    Docking Collars:             1 needed /   4 available\n" +
                   "    Has JumpShips?             Yes\n" +
                   "    Has WarShips?               No";
        doReturn(4).when(spyReputation).getHeavyVeeCount();
        assertEquals(expected, spyReputation.getTransportationDetails());

        // Add excess heavy vehicle bays.
        expected = "Transportation:      20\n" +
                   "    BattleMech Bays:             4 needed /   4 available\n" +
                   "    Fighter Bays:                2 needed /   2 available (plus 0 excess Small Craft)\n" +
                   "    Small Craft Bays:            0 needed /   0 available\n" +
                   "    ProtoMech Bays:              0 needed /   0 available\n" +
                   "    Super Heavy Vehicle Bays:    0 needed /   0 available\n" +
                   "    Heavy Vehicle Bays:          4 needed /   8 available (plus 0 excess Super Heavy)\n" +
                   "    Light Vehicle Bays:          8 needed /  22 available (plus 4 excess Heavy and 0 excess Super Heavy)\n" +
                   "    Battle Armor Bays:           0 needed /   0 available\n" +
                   "    Infantry Bays:               1 needed /   4 available\n" +
                   "    Docking Collars:             1 needed /   4 available\n" +
                   "    Has JumpShips?             Yes\n" +
                   "    Has WarShips?               No";
        doReturn(8).when(spyReputation).getHeavyVeeBayCount();
        assertEquals(expected, spyReputation.getTransportationDetails());
    }

    private void mockMothballed() {
        mockMechMothballed = mock(Unit.class);
        mockAeroMothballed = mock(Unit.class);
        mockTankMothballed = mock(Unit.class);

        when(mockMechMothballed.isMothballed()).thenReturn(true);
        when(mockAeroMothballed.isMothballed()).thenReturn(true);
        when(mockTankMothballed.isMothballed()).thenReturn(true);
    }

    private void mockSkills() {
        mockMechGunnery = mock(Skill.class);
        mockMechPilot = mock(Skill.class);
        mockTankGunnery = mock(Skill.class);
        mockTankPilot = mock(Skill.class);
        mockInfantryGunnery = mock(Skill.class);
        mockAeroGunnery = mock(Skill.class);
        mockAeroPilot = mock(Skill.class);
        mockDropGunnery = mock(Skill.class);
        mockDropPilot = mock(Skill.class);
        mockJumpGunnery = mock(Skill.class);
        mockJumpPilot = mock(Skill.class);
        mockLeader = mock(Skill.class);
        mockTactics = mock(Skill.class);
        mockStrategy = mock(Skill.class);
        mockNegotiation = mock(Skill.class);
        mockMechTechSkillRegular = mock(Skill.class);
        mockMechTechSkillElite = mock(Skill.class);
        mockFighterTechSkill = mock(Skill.class);
        mockFighterTechSkillElite = mock(Skill.class);
        mockVeeTechSkill = mock(Skill.class);

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
    }

    private int mockThunderbolt1() {
        mockThunderbolt1 = mock(BipedMech.class);
        mockThunderboltUnit1 = mock(Unit.class);
        mockThunderbolt1Pilot = mock(Person.class);
        mockThunderbolt1Tech = mock(Person.class);

        when(mockThunderbolt1.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockThunderbolt1.getUnitType()).thenCallRealMethod();
        when(mockThunderboltUnit1.getEntity()).thenReturn(mockThunderbolt1);
        when(mockThunderbolt1Pilot.isAdministrator()).thenReturn(false);
        when(mockThunderbolt1Pilot.getPrimaryRole()).thenReturn(PersonnelRole.MECHWARRIOR);
        when(mockThunderbolt1Pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockThunderbolt1Pilot).getStatus();
        when(mockThunderbolt1Pilot.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
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
        when(mockThunderbolt1Tech.isAdministrator()).thenReturn(false);
        when(mockThunderbolt1Tech.isTech()).thenReturn(true);
        when(mockThunderbolt1Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockThunderbolt1Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(mockThunderbolt1Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        doReturn(PersonnelStatus.ACTIVE).when(mockThunderbolt1Tech).getStatus();
        when(mockThunderbolt1Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockThunderbolt1Tech);

        return 6; // astechs needed by Thunderbolt1
    }

    private int mockThunderbolt2() {
        mockThunderbolt2 = mock(BipedMech.class);
        mockThunderboltUnit2 = mock(Unit.class);
        mockThunderbolt2Pilot = mock(Person.class);
        mockThunderbolt2Tech = mock(Person.class);

        when(mockThunderbolt2.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockThunderbolt2.getUnitType()).thenCallRealMethod();
        when(mockThunderboltUnit2.getEntity()).thenReturn(mockThunderbolt2);
        when(mockThunderbolt2Pilot.isAdministrator()).thenReturn(false);
        when(mockThunderbolt2Pilot.getPrimaryRole()).thenReturn(PersonnelRole.MECHWARRIOR);
        when(mockThunderbolt2Pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockThunderbolt2Pilot).getStatus();
        when(mockThunderbolt2Pilot.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockThunderbolt2Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockThunderbolt2Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockThunderbolt2Pilot);
        mockThunderboltUnit2.addPilotOrSoldier(mockThunderbolt2Pilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockThunderbolt2Pilot);
        when(mockThunderboltUnit2.getCrew()).thenReturn(crew);
        Crew mockThunderbolt2Crew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockThunderbolt2Crew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockThunderbolt2Crew).getGunnery();
        when(mockThunderbolt2.getCrew()).thenReturn(mockThunderbolt2Crew);
        when(mockThunderbolt2Tech.isAdministrator()).thenReturn(false);
        when(mockThunderbolt2Tech.isTech()).thenReturn(true);
        when(mockThunderbolt2Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockThunderbolt2Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockThunderbolt2Tech).getStatus();
        when(mockThunderbolt2Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockThunderbolt2Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockThunderbolt2Tech);
        return 6; // astechs needed by Thunderbolt2
    }

    private int mockGrasshopper1() {
        mockGrasshopper1 = mock(BipedMech.class);
        mockGrasshopperUnit1 = mock(Unit.class);
        mockGrasshopper1Pilot = mock(Person.class);
        mockGrasshopper1Tech = mock(Person.class);

        when(mockGrasshopper1.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockGrasshopper1.getUnitType()).thenCallRealMethod();
        when(mockGrasshopperUnit1.getEntity()).thenReturn(mockGrasshopper1);
        when(mockGrasshopper1Pilot.isAdministrator()).thenReturn(false);
        when(mockGrasshopper1Pilot.getPrimaryRole()).thenReturn(PersonnelRole.MECHWARRIOR);
        when(mockGrasshopper1Pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockGrasshopper1Pilot).getStatus();
        when(mockGrasshopper1Pilot.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockGrasshopper1Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockGrasshopper1Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        personnelList.add(mockGrasshopper1Pilot);
        mockGrasshopperUnit1.addPilotOrSoldier(mockGrasshopper1Pilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockGrasshopper1Pilot);
        when(mockGrasshopperUnit1.getCrew()).thenReturn(crew);
        Crew mockGrasshopperCrew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockGrasshopperCrew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockGrasshopperCrew).getGunnery();
        when(mockGrasshopper1.getCrew()).thenReturn(mockGrasshopperCrew);
        when(mockGrasshopper1Tech.isAdministrator()).thenReturn(false);
        when(mockGrasshopper1Tech.isTech()).thenReturn(true);
        when(mockGrasshopper1Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockGrasshopper1Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockGrasshopper1Tech).getStatus();
        when(mockGrasshopper1Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockGrasshopper1Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillRegular);
        personnelList.add(mockGrasshopper1Tech);
        return 6; // astechs needed by Grasshopper1
    }

    private int mockGrasshopper2() {
        mockGrasshopper2 = mock(BipedMech.class);
        mockGrasshopperUnit2 = mock(Unit.class);
        mockGrasshopper2Pilot = mock(Person.class);
        mockGrasshopper2Tech = mock(Person.class);

        when(mockGrasshopper2.getEntityType()).thenReturn(Entity.ETYPE_MECH);
        when(mockGrasshopper2.getUnitType()).thenCallRealMethod();
        when(mockGrasshopperUnit2.getEntity()).thenReturn(mockGrasshopper2);
        when(mockGrasshopper2Pilot.isAdministrator()).thenReturn(false);
        when(mockGrasshopper2Pilot.getPrimaryRole()).thenReturn(PersonnelRole.MECHWARRIOR);
        when(mockGrasshopper2Pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockGrasshopper2Pilot).getStatus();
        when(mockGrasshopper2Pilot.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_GUN_MECH)).thenReturn(mockMechGunnery);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_PILOT_MECH)).thenReturn(mockMechPilot);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_LEADER)).thenReturn(mockLeader);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_TACTICS)).thenReturn(mockTactics);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_STRATEGY)).thenReturn(mockStrategy);
        when(mockGrasshopper2Pilot.getSkill(SkillType.S_NEG)).thenReturn(mockNegotiation);
        personnelList.add(mockGrasshopper2Pilot);
        mockGrasshopperUnit2.addPilotOrSoldier(mockGrasshopper2Pilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockGrasshopper2Pilot);
        when(mockGrasshopperUnit2.getCrew()).thenReturn(crew);
        Crew mockGrasshopper2Crew = mock(Crew.class);
        doReturn(mockMechPilot.getLevel()).when(mockGrasshopper2Crew).getPiloting();
        doReturn(mockMechGunnery.getLevel()).when(mockGrasshopper2Crew).getGunnery();
        when(mockGrasshopper2.getCrew()).thenReturn(mockGrasshopper2Crew);
        when(mockGrasshopper2Tech.isAdministrator()).thenReturn(false);
        when(mockGrasshopper2Tech.isTech()).thenReturn(true);
        when(mockGrasshopper2Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockGrasshopper2Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockGrasshopper2Tech).getStatus();
        when(mockGrasshopper2Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockGrasshopper2Tech.getSkill(SkillType.S_TECH_MECH)).thenReturn(mockMechTechSkillElite);
        personnelList.add(mockGrasshopper2Tech);
        return 6; // astechs needed by Grasshopper1
    }

    private int mockBulldog1() {
        mockBulldog1 = mock(Tank.class);
        mockBulldogUnit1 = mock(Unit.class);
        mockBulldog1Driver = mock(Person.class);
        mockBulldog1Gunner1 = mock(Person.class);
        mockBulldog1Gunner2 = mock(Person.class);
        mockBulldog1Gunner3 = mock(Person.class);
        mockBulldog1Tech = mock(Person.class);

        when(mockBulldog1.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog1.getUnitType()).thenCallRealMethod();
        when(mockBulldogUnit1.getEntity()).thenReturn(mockBulldog1);
        when(mockBulldog1Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog1Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog1Driver.isAdministrator()).thenReturn(false);
        when(mockBulldog1Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockBulldog1Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog1Driver).getStatus();
        when(mockBulldog1Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog1Gunner1.isAdministrator()).thenReturn(false);
        when(mockBulldog1Gunner1.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog1Gunner1.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog1Gunner1).getStatus();
        when(mockBulldog1Gunner1.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog1Gunner2.isAdministrator()).thenReturn(false);
        when(mockBulldog1Gunner2.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog1Gunner2.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog1Gunner2).getStatus();
        when(mockBulldog1Gunner2.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog1Gunner3.isAdministrator()).thenReturn(false);
        when(mockBulldog1Gunner3.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog1Gunner3.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog1Gunner3).getStatus();
        when(mockBulldog1Gunner3.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog1Tech.isAdministrator()).thenReturn(false);
        when(mockBulldog1Tech.isTech()).thenReturn(true);
        when(mockBulldog1Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockBulldog1Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog1Tech).getStatus();
        when(mockBulldog1Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog1Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog1Driver);
        personnelList.add(mockBulldog1Gunner1);
        personnelList.add(mockBulldog1Gunner2);
        personnelList.add(mockBulldog1Gunner3);
        personnelList.add(mockBulldog1Tech);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Driver);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner1);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner2);
        mockBulldogUnit1.addPilotOrSoldier(mockBulldog1Gunner3);
        ArrayList<Person> crew = new ArrayList<>(4);
        crew.add(mockBulldog1Driver);
        crew.add(mockBulldog1Gunner1);
        crew.add(mockBulldog1Gunner2);
        crew.add(mockBulldog1Gunner3);
        when(mockBulldogUnit1.getCrew()).thenReturn(crew);
        Crew mockBulldog1Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog1Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog1Crew).getGunnery();
        when(mockBulldog1.getCrew()).thenReturn(mockBulldog1Crew);
        return 6; // astechs needed by Bulldog1
    }

    private int mockBulldog2() {
        mockBulldog2 = mock(Tank.class);
        mockBulldogUnit2 = mock(Unit.class);
        mockBulldog2Driver = mock(Person.class);
        mockBulldog2Gunner1 = mock(Person.class);
        mockBulldog2Gunner2 = mock(Person.class);
        mockBulldog2Gunner3 = mock(Person.class);
        mockBulldog2Tech = mock(Person.class);

        when(mockBulldog2.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog2.getUnitType()).thenCallRealMethod();
        when(mockBulldog2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog2Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog2Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog2Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog2Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldogUnit2.getEntity()).thenReturn(mockBulldog2);
        when(mockBulldog2Driver.isAdministrator()).thenReturn(false);
        when(mockBulldog2Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockBulldog2Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog2Driver).getStatus();
        when(mockBulldog2Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog2Driver);
        when(mockBulldog2Gunner1.isAdministrator()).thenReturn(false);
        when(mockBulldog2Gunner1.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog2Gunner1.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog2Gunner1).getStatus();
        when(mockBulldog2Gunner1.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog2Gunner1);
        when(mockBulldog2Gunner2.isAdministrator()).thenReturn(false);
        when(mockBulldog2Gunner2.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog2Gunner2.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog2Gunner2).getStatus();
        when(mockBulldog2Gunner2.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog2Gunner2);
        when(mockBulldog2Gunner3.isAdministrator()).thenReturn(false);
        when(mockBulldog2Gunner3.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog2Gunner3.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog2Gunner3).getStatus();
        when(mockBulldog2Gunner3.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog2Gunner3);
        when(mockBulldog2Tech.isAdministrator()).thenReturn(false);
        when(mockBulldog2Tech.isTech()).thenReturn(true);
        when(mockBulldog2Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockBulldog2Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog2Tech).getStatus();
        when(mockBulldog2Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog2Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog2Tech);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Driver);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner1);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner2);
        mockBulldogUnit2.addPilotOrSoldier(mockBulldog2Gunner3);
        ArrayList<Person> crew = new ArrayList<>(4);
        crew.add(mockBulldog2Driver);
        crew.add(mockBulldog2Gunner1);
        crew.add(mockBulldog2Gunner2);
        crew.add(mockBulldog2Gunner3);
        when(mockBulldogUnit2.getCrew()).thenReturn(crew);
        Crew mockBulldog2Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog2Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog2Crew).getGunnery();
        when(mockBulldog2.getCrew()).thenReturn(mockBulldog2Crew);
        return 6; // astechs needed by Bulldog1
    }

    private int mockBulldog3() {
        mockBulldog3 = mock(Tank.class);
        mockBulldogUnit3 = mock(Unit.class);
        mockBulldog3Driver = mock(Person.class);
        mockBulldog3Gunner1 = mock(Person.class);
        mockBulldog3Gunner2 = mock(Person.class);
        mockBulldog3Gunner3 = mock(Person.class);
        mockBulldog3Tech = mock(Person.class);

        when(mockBulldog3.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog3.getUnitType()).thenCallRealMethod();
        when(mockBulldog3Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog3Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog3Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog3Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog3Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldogUnit3.getEntity()).thenReturn(mockBulldog3);
        when(mockBulldog3Driver.isAdministrator()).thenReturn(false);
        when(mockBulldog3Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockBulldog3Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog3Driver).getStatus();
        when(mockBulldog3Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog3Driver);
        when(mockBulldog3Gunner1.isAdministrator()).thenReturn(false);
        when(mockBulldog3Gunner1.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog3Gunner1.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog3Gunner1).getStatus();
        when(mockBulldog3Gunner1.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog3Gunner1);
        when(mockBulldog3Gunner2.isAdministrator()).thenReturn(false);
        when(mockBulldog3Gunner2.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog3Gunner2.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog3Gunner2).getStatus();
        when(mockBulldog3Gunner2.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog3Gunner2);
        when(mockBulldog3Gunner3.isAdministrator()).thenReturn(false);
        when(mockBulldog3Gunner3.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog3Gunner3.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog3Gunner3).getStatus();
        when(mockBulldog3Gunner3.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog3Gunner3);
        when(mockBulldog3Tech.isAdministrator()).thenReturn(false);
        when(mockBulldog3Tech.isTech()).thenReturn(true);
        when(mockBulldog3Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockBulldog3Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog3Tech).getStatus();
        when(mockBulldog3Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog3Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog3Tech);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Driver);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner1);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner2);
        mockBulldogUnit3.addPilotOrSoldier(mockBulldog3Gunner3);
        ArrayList<Person> crew = new ArrayList<>(4);
        crew.add(mockBulldog3Driver);
        crew.add(mockBulldog3Gunner1);
        crew.add(mockBulldog3Gunner2);
        crew.add(mockBulldog3Gunner3);
        when(mockBulldogUnit3.getCrew()).thenReturn(crew);
        Crew mockBulldog3Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog3Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog3Crew).getGunnery();
        when(mockBulldog3.getCrew()).thenReturn(mockBulldog3Crew);
        return 6; // astechs needed by Bulldog1
    }

    private int mockBulldog4() {
        mockBulldog4 = mock(Tank.class);
        mockBulldogUnit4 = mock(Unit.class);
        mockBulldog4Driver = mock(Person.class);
        mockBulldog4Gunner1 = mock(Person.class);
        mockBulldog4Gunner2 = mock(Person.class);
        mockBulldog4Gunner3 = mock(Person.class);
        mockBulldog4Tech = mock(Person.class);

        when(mockBulldog4.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockBulldog4.getUnitType()).thenCallRealMethod();
        when(mockBulldog4Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockBulldog4Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog4Gunner1.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog4Gunner2.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldog4Gunner3.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockBulldogUnit4.getEntity()).thenReturn(mockBulldog4);
        when(mockBulldog4Driver.isAdministrator()).thenReturn(false);
        when(mockBulldog4Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockBulldog4Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog4Driver).getStatus();
        when(mockBulldog4Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog4Driver);
        when(mockBulldog4Gunner1.isAdministrator()).thenReturn(false);
        when(mockBulldog4Gunner1.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog4Gunner1.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog4Gunner1).getStatus();
        when(mockBulldog4Gunner1.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog4Gunner1);
        when(mockBulldog4Gunner2.isAdministrator()).thenReturn(false);
        when(mockBulldog4Gunner2.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog4Gunner2.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog4Gunner2).getStatus();
        when(mockBulldog4Gunner2.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog4Gunner2);
        when(mockBulldog4Gunner3.isAdministrator()).thenReturn(false);
        when(mockBulldog4Gunner3.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockBulldog4Gunner3.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog4Gunner3).getStatus();
        when(mockBulldog4Gunner3.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockBulldog4Gunner3);
        when(mockBulldog4Tech.isAdministrator()).thenReturn(false);
        when(mockBulldog4Tech.isTech()).thenReturn(true);
        when(mockBulldog4Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockBulldog4Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockBulldog4Tech).getStatus();
        when(mockBulldog4Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockBulldog4Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockBulldog4Tech);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Driver);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner1);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner2);
        mockBulldogUnit4.addPilotOrSoldier(mockBulldog4Gunner3);
        ArrayList<Person> crew = new ArrayList<>(4);
        crew.add(mockBulldog4Driver);
        crew.add(mockBulldog4Gunner1);
        crew.add(mockBulldog4Gunner2);
        crew.add(mockBulldog4Gunner3);
        when(mockBulldogUnit4.getCrew()).thenReturn(crew);
        Crew mockBulldog4Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockBulldog4Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockBulldog4Crew).getGunnery();
        when(mockBulldog4.getCrew()).thenReturn(mockBulldog4Crew);
        return 6; // astechs needed by Bulldog1
    }

    private int mockPackrat1() {
        mockPackrat1 = mock(Tank.class);
        mockPackratUnit1 = mock(Unit.class);
        mockPackrat1Driver = mock(Person.class);
        mockPackrat1Gunner = mock(Person.class);
        mockPackrat1Tech = mock(Person.class);

        when(mockPackrat1.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat1.getUnitType()).thenCallRealMethod();
        when(mockPackrat1Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat1Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat1Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit1.getEntity()).thenReturn(mockPackrat1);
        when(mockPackrat1Driver.isAdministrator()).thenReturn(false);
        when(mockPackrat1Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockPackrat1Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat1Driver).getStatus();
        when(mockPackrat1Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat1Driver);
        when(mockPackrat1Gunner.isAdministrator()).thenReturn(false);
        when(mockPackrat1Gunner.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockPackrat1Gunner.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat1Gunner).getStatus();
        when(mockPackrat1Gunner.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat1Gunner);
        when(mockPackrat1Tech.isAdministrator()).thenReturn(false);
        when(mockPackrat1Tech.isTech()).thenReturn(true);
        when(mockPackrat1Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockPackrat1Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat1Tech).getStatus();
        when(mockPackrat1Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockPackrat1Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat1Tech);
        mockPackratUnit1.addPilotOrSoldier(mockPackrat1Driver);
        mockPackratUnit1.addPilotOrSoldier(mockPackrat1Gunner);
        ArrayList<Person> crew = new ArrayList<>(2);
        crew.add(mockPackrat1Driver);
        crew.add(mockPackrat1Gunner);
        when(mockPackratUnit1.getCrew()).thenReturn(crew);
        Crew mockPackrat1Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat1Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat1Crew).getGunnery();
        when(mockPackrat1.getCrew()).thenReturn(mockPackrat1Crew);
        return 6; // astechs needed for Packrat1
    }

    private int mockPackrat2() {
        mockPackrat2 = mock(Tank.class);
        mockPackratUnit2 = mock(Unit.class);
        mockPackrat2Driver = mock(Person.class);
        mockPackrat2Gunner = mock(Person.class);
        mockPackrat2Tech = mock(Person.class);

        when(mockPackrat2.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat2.getUnitType()).thenCallRealMethod();
        when(mockPackrat2Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat2Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat2Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit2.getEntity()).thenReturn(mockPackrat2);
        when(mockPackrat2Driver.isAdministrator()).thenReturn(false);
        when(mockPackrat2Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockPackrat2Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat2Driver).getStatus();
        when(mockPackrat2Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat2Driver);
        when(mockPackrat2Gunner.isAdministrator()).thenReturn(false);
        when(mockPackrat2Gunner.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockPackrat2Gunner.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat2Gunner).getStatus();
        when(mockPackrat2Gunner.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat2Gunner);
        when(mockPackrat2Tech.isAdministrator()).thenReturn(false);
        when(mockPackrat2Tech.isTech()).thenReturn(true);
        when(mockPackrat2Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockPackrat2Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat2Tech).getStatus();
        when(mockPackrat2Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockPackrat2Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat2Tech);
        mockPackratUnit2.addPilotOrSoldier(mockPackrat2Driver);
        mockPackratUnit2.addPilotOrSoldier(mockPackrat2Gunner);
        ArrayList<Person> crew = new ArrayList<>(2);
        crew.add(mockPackrat2Driver);
        crew.add(mockPackrat2Gunner);
        when(mockPackratUnit2.getCrew()).thenReturn(crew);
        Crew mockPackrat2Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat2Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat2Crew).getGunnery();
        when(mockPackrat2.getCrew()).thenReturn(mockPackrat2Crew);
        return 6; // astechs needed for Packrat2
    }

    private int mockPackrat3() {
        mockPackrat3 = mock(Tank.class);
        mockPackratUnit3 = mock(Unit.class);
        mockPackrat3Driver = mock(Person.class);
        mockPackrat3Gunner = mock(Person.class);
        mockPackrat3Tech = mock(Person.class);

        when(mockPackrat3.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat3.getUnitType()).thenCallRealMethod();
        when(mockPackrat3Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat3Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat3Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit3.getEntity()).thenReturn(mockPackrat3);
        when(mockPackrat3Driver.isAdministrator()).thenReturn(false);
        when(mockPackrat3Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockPackrat3Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat3Driver).getStatus();
        when(mockPackrat3Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat3Driver);
        when(mockPackrat3Gunner.isAdministrator()).thenReturn(false);
        when(mockPackrat3Gunner.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockPackrat3Gunner.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat3Gunner).getStatus();
        when(mockPackrat3Gunner.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat3Gunner);
        when(mockPackrat3Tech.isAdministrator()).thenReturn(false);
        when(mockPackrat3Tech.isTech()).thenReturn(true);
        when(mockPackrat3Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockPackrat3Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat3Tech).getStatus();
        when(mockPackrat3Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockPackrat3Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat3Tech);
        mockPackratUnit3.addPilotOrSoldier(mockPackrat3Driver);
        mockPackratUnit3.addPilotOrSoldier(mockPackrat3Gunner);
        ArrayList<Person> crew = new ArrayList<>(2);
        crew.add(mockPackrat3Driver);
        crew.add(mockPackrat3Gunner);
        when(mockPackratUnit3.getCrew()).thenReturn(crew);
        Crew mockPackrat3Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat3Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat3Crew).getGunnery();
        when(mockPackrat3.getCrew()).thenReturn(mockPackrat3Crew);
        return 6; // astechs needed for Packrat3
    }

    private int mockPackrat4() {
        mockPackrat4 = mock(Tank.class);
        mockPackratUnit4 = mock(Unit.class);
        mockPackrat4Driver = mock(Person.class);
        mockPackrat4Gunner = mock(Person.class);
        mockPackrat4Tech = mock(Person.class);

        when(mockPackrat4.getEntityType()).thenReturn(Entity.ETYPE_TANK);
        when(mockPackrat4.getUnitType()).thenCallRealMethod();
        when(mockPackrat4Driver.getSkill(SkillType.S_PILOT_GVEE)).thenReturn(mockTankPilot);
        when(mockPackrat4Driver.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackrat4Gunner.getSkill(SkillType.S_GUN_VEE)).thenReturn(mockTankGunnery);
        when(mockPackratUnit4.getEntity()).thenReturn(mockPackrat4);
        when(mockPackrat4Driver.isAdministrator()).thenReturn(false);
        when(mockPackrat4Driver.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        when(mockPackrat4Driver.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat4Driver).getStatus();
        when(mockPackrat4Driver.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat4Driver);
        when(mockPackrat4Gunner.isAdministrator()).thenReturn(false);
        when(mockPackrat4Gunner.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        when(mockPackrat4Gunner.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat4Gunner).getStatus();
        when(mockPackrat4Gunner.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockPackrat4Gunner);
        when(mockPackrat4Tech.isAdministrator()).thenReturn(false);
        when(mockPackrat4Tech.isTech()).thenReturn(true);
        when(mockPackrat4Tech.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        when(mockPackrat4Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockPackrat4Tech).getStatus();
        when(mockPackrat4Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockPackrat4Tech.getSkill(SkillType.S_TECH_MECHANIC)).thenReturn(mockVeeTechSkill);
        personnelList.add(mockPackrat4Tech);
        mockPackratUnit4.addPilotOrSoldier(mockPackrat4Driver);
        mockPackratUnit4.addPilotOrSoldier(mockPackrat4Gunner);
        ArrayList<Person> crew = new ArrayList<>(2);
        crew.add(mockPackrat4Driver);
        crew.add(mockPackrat4Gunner);
        when(mockPackratUnit4.getCrew()).thenReturn(crew);
        Crew mockPackrat4Crew = mock(Crew.class);
        doReturn(mockTankPilot.getLevel()).when(mockPackrat4Crew).getPiloting();
        doReturn(mockTankGunnery.getLevel()).when(mockPackrat4Crew).getGunnery();
        when(mockPackrat4.getCrew()).thenReturn(mockPackrat4Crew);
        return 6; // astechs needed for Packrat4
    }

    private void mockLaserPlatoon() {
        infantryPersonnel = new HashSet<>(28);
        mockLaserPlatoon = mock(Infantry.class);
        mockLaserPlatoonUnit = mock(Unit.class);

        when(mockLaserPlatoon.getEntityType()).thenReturn(Entity.ETYPE_INFANTRY);
        when(mockLaserPlatoon.getUnitType()).thenCallRealMethod();
        when(mockLaserPlatoon.getSquadSize()).thenReturn(7);
        when(mockLaserPlatoon.getSquadCount()).thenReturn(4);
        when(mockLaserPlatoonUnit.getEntity()).thenReturn(mockLaserPlatoon);
        ArrayList<Person> crew = new ArrayList<>(28);
        for (int i = 0; i < 28; i++) {
            Person mockInfantry = mock(Person.class);
            when(mockInfantry.isAdministrator()).thenReturn(false);
            when(mockInfantry.getPrimaryRole()).thenReturn(PersonnelRole.SOLDIER);
            when(mockInfantry.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            doReturn(PersonnelStatus.ACTIVE).when(mockInfantry).getStatus();
            when(mockInfantry.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
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
    }

    private int mockCorsair1() {
        mockCorsair1 = mock(AeroSpaceFighter.class);
        mockCorsairUnit1 = mock(Unit.class);
        mockCorsair1Pilot = mock(Person.class);
        mockCorsair1Tech = mock(Person.class);

        when(mockCorsair1.getEntityType()).thenReturn(Entity.ETYPE_AEROSPACEFIGHTER);
        when(mockCorsair1.getUnitType()).thenCallRealMethod();
        when(mockCorsairUnit1.getEntity()).thenReturn(mockCorsair1);
        when(mockCorsair1Pilot.isAdministrator()).thenReturn(false);
        when(mockCorsair1Pilot.getSkill(SkillType.S_GUN_AERO)).thenReturn(mockAeroGunnery);
        when(mockCorsair1Pilot.getSkill(SkillType.S_PILOT_AERO)).thenReturn(mockAeroPilot);
        when(mockCorsair1Pilot.getPrimaryRole()).thenReturn(PersonnelRole.AEROSPACE_PILOT);
        when(mockCorsair1Pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockCorsair1Pilot).getStatus();
        when(mockCorsair1Pilot.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockCorsair1Pilot);
        mockCorsairUnit1.addPilotOrSoldier(mockCorsair1Pilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockCorsair1Pilot);
        when(mockCorsairUnit1.getCrew()).thenReturn(crew);
        when(mockCorsair1Tech.isAdministrator()).thenReturn(false);
        when(mockCorsair1Tech.isTech()).thenReturn(true);
        when(mockCorsair1Tech.getPrimaryRole()).thenReturn(PersonnelRole.AERO_TECH);
        when(mockCorsair1Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockCorsair1Tech).getStatus();
        when(mockCorsair1Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockCorsair1Tech.getSkill(SkillType.S_TECH_AERO)).thenReturn(mockFighterTechSkill);
        personnelList.add(mockCorsair1Tech);
        Crew mockCorsair1Crew = mock(Crew.class);
        doReturn(mockAeroPilot.getLevel()).when(mockCorsair1Crew).getPiloting();
        doReturn(mockAeroGunnery.getLevel()).when(mockCorsair1Crew).getGunnery();
        when(mockCorsair1.getCrew()).thenReturn(mockCorsair1Crew);
        return 6; // astechs for Corsair1
    }

    private int mockCorsair2() {
        mockCorsair2 = mock(AeroSpaceFighter.class);
        mockCorsairUnit2 = mock(Unit.class);
        mockCorsair2Pilot = mock(Person.class);
        mockCorsair2Tech = mock(Person.class);

        when(mockCorsair2.getEntityType()).thenReturn(Entity.ETYPE_AEROSPACEFIGHTER);
        when(mockCorsair2.getUnitType()).thenCallRealMethod();
        when(mockCorsairUnit2.getEntity()).thenReturn(mockCorsair2);
        when(mockCorsair2Pilot.isAdministrator()).thenReturn(false);
        when(mockCorsair2Pilot.getSkill(SkillType.S_GUN_AERO)).thenReturn(mockAeroGunnery);
        when(mockCorsair2Pilot.getSkill(SkillType.S_PILOT_AERO)).thenReturn(mockAeroPilot);
        when(mockCorsair2Pilot.getPrimaryRole()).thenReturn(PersonnelRole.AEROSPACE_PILOT);
        when(mockCorsair2Pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockCorsair2Pilot).getStatus();
        when(mockCorsair2Pilot.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        personnelList.add(mockCorsair2Pilot);
        mockCorsairUnit2.addPilotOrSoldier(mockCorsair2Pilot);
        ArrayList<Person> crew = new ArrayList<>(1);
        crew.add(mockCorsair2Pilot);
        when(mockCorsairUnit2.getCrew()).thenReturn(crew);
        when(mockCorsair2Tech.isAdministrator()).thenReturn(false);
        when(mockCorsair2Tech.isTech()).thenReturn(true);
        when(mockCorsair2Tech.getPrimaryRole()).thenReturn(PersonnelRole.AERO_TECH);
        when(mockCorsair2Tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockCorsair2Tech).getStatus();
        when(mockCorsair2Tech.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockCorsair2Tech.getSkill(SkillType.S_TECH_AERO)).thenReturn(mockFighterTechSkillElite);
        personnelList.add(mockCorsair2Tech);
        Crew mockCorsair2Crew = mock(Crew.class);
        doReturn(mockAeroPilot.getLevel()).when(mockCorsair2Crew).getPiloting();
        doReturn(mockAeroGunnery.getLevel()).when(mockCorsair2Crew).getGunnery();
        when(mockCorsair2.getCrew()).thenReturn(mockCorsair2Crew);
        return 6; // astechs for Corsair1
    }

    private void mockSeeker() {
        seekerCrew = new HashSet<>(20);
        mockSeeker = mock(Dropship.class);
        mockSeekerUnit = mock(Unit.class);

        when(mockSeeker.getEntityType()).thenReturn(Entity.ETYPE_DROPSHIP);
        when(mockSeeker.getUnitType()).thenCallRealMethod();
        when(mockSeekerUnit.getEntity()).thenReturn(mockSeeker);
        Vector<Bay> bayList = new Vector<>();
        Bay transportBay = new MechBay(4, 1, 1);
        bayList.add(transportBay);
        transportBay = new ASFBay(2, 0, 2);
        bayList.add(transportBay);
        transportBay = new LightVehicleBay(22, 0, 3);
        bayList.add(transportBay);
        transportBay = new InfantryBay(4.0, 0, 4, PlatoonType.FOOT);
        bayList.add(transportBay);
        when(mockSeeker.getTransportBays()).thenReturn(bayList);
        ArrayList<Person> crew = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            Person mockCrew = mock(Person.class);
            when(mockCrew.isAdministrator()).thenReturn(false);
            when(mockCrew.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            doReturn(PersonnelStatus.ACTIVE).when(mockCrew).getStatus();
            when(mockCrew.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
            if (i == 0) {
                when(mockCrew.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_PILOT);
                when(mockCrew.getSkill(SkillType.S_PILOT_SPACE)).thenReturn(mockDropPilot);
            } else {
                when(mockCrew.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_GUNNER);
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
    }

    private void mockInvader() {
        invaderCrew = new HashSet<>(24);
        mockInvader = mock(Jumpship.class);
        mockInvaderUnit = mock(Unit.class);

        when(mockInvader.getEntityType()).thenReturn(Entity.ETYPE_JUMPSHIP);
        when(mockInvader.getUnitType()).thenCallRealMethod();
        when(mockInvaderUnit.getEntity()).thenReturn(mockInvader);
        DockingCollar collar;
        Vector<DockingCollar> collarList = new Vector<>(4);
        for (int i = 0; i < 4; i++) {
            collar = mock(DockingCollar.class);
            collarList.add(collar);
        }
        when(mockInvader.getTransportBays()).thenReturn(new Vector<>(0));
        when(mockInvader.getDockingCollars()).thenReturn(collarList);
        ArrayList<Person> crew = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            Person mockCrew = mock(Person.class);
            when(mockCrew.isAdministrator()).thenReturn(false);
            when(mockCrew.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            doReturn(PersonnelStatus.ACTIVE).when(mockCrew).getStatus();
            when(mockCrew.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
            if (i == 0) {
                when(mockCrew.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_PILOT);
                when(mockCrew.getSkill(SkillType.S_PILOT_SPACE)).thenReturn(mockJumpPilot);
            } else {
                when(mockCrew.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_GUNNER);
                when(mockCrew.getSkill(SkillType.S_GUN_SPACE)).thenReturn(mockJumpGunnery);
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
    }
}

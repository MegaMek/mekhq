/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.Personnel;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PersonnelTableModelColumnTest {
    //region Variable Declarations
    private static final PersonnelTableModelColumn[] columns = PersonnelTableModelColumn.values();

    @Test
    public void testGetWidth() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            switch (personnelTableModelColumn) {
                case RANK:
                case FIRST_NAME:
                case LAST_NAME:
                    // fixed size for the most important columns
                    assertNotNull(personnelTableModelColumn.getPreferredWidth());
                    break;
                case FORCE:
                case BODY:
                case AGGRESSION:
                case XP:
                case KILLS:
                case MEDTECH:
                    assertNull(personnelTableModelColumn.getPreferredWidth());
                    break;
            }
        }
    }

    @Test
    public void testGetAlignment() {
        // check overrides and a few columns of different types
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            switch (personnelTableModelColumn) {
                case RANK, SKILL_LEVEL ->
                    assertEquals(SwingConstants.LEFT, personnelTableModelColumn.getAlignment());
                case SALARY, TECH_MINUTES ->
                    assertEquals(SwingConstants.RIGHT, personnelTableModelColumn.getAlignment());
                case FIRST_NAME -> // String
                    assertEquals(SwingConstants.LEFT, personnelTableModelColumn.getAlignment());
                case AGE -> // Int
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                case VTOL -> // SkillPair
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                case BIRTHDAY -> // Date
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                case COMMANDER -> // Boolean
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                case HIGHEST_EDUCATION -> // field
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                case AGGRESSION -> // PersonalityTrait
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                case REFLEXES -> // SkillAttribute
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                default ->
                    assertTrue(SwingConstants.RIGHT != personnelTableModelColumn.getAlignment());
            }
        }
    }

    @Test
    public void testGetDefaultSortOrder() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            switch (personnelTableModelColumn) {
                case RANK:
                case FIRST_NAME:
                case LAST_NAME:
                case SKILL_LEVEL:
                    assertEquals(SortOrder.DESCENDING, personnelTableModelColumn.getDefaultSortOrder());
                    break;
                default:
                    assertNull(personnelTableModelColumn.getDefaultSortOrder());
                    break;
            }
        }
    }

    @Test
    public void testToStringOverride() {
        String bundle = "mekhq.resources.PersonnelTable";
        assertEquals(getTextAt(bundle, "Column.RANK.title"),
              PersonnelTableModelColumn.RANK.toString());
        assertEquals(getTextAt(bundle, "Column.PERSONNEL_STATUS.title"),
              PersonnelTableModelColumn.PERSONNEL_STATUS.toString());
        assertEquals(getTextAt(bundle, "Column.FORCE.title"),
              PersonnelTableModelColumn.FORCE.toString());
        assertEquals(getTextAt(bundle, "Column.TECH_MECHANIC.title"),
              PersonnelTableModelColumn.TECH_MECHANIC.toString());
        assertEquals(getTextAt(bundle, "Column.RECRUITMENT_DATE.title"),
              PersonnelTableModelColumn.RECRUITMENT_DATE.toString());
    }

    @Nested
    class LocationColumnCellValues {

        private static final LocalDate TODAY = LocalDate.of(3025, 1, 1);
        private static final String CAMPAIGN_NAME = "Test Mercs";

        private PersonnelMarket market;
        private Personnel mainForce;

        @BeforeEach
        void setUp() {
            market = mock(PersonnelMarket.class);
            mainForce = new Personnel();
        }

        private PlanetarySystem mockSystem(String sysName, String planetName) {
            PlanetarySystem sys = mock(PlanetarySystem.class);
            when(sys.getPrintableName(any())).thenReturn(sysName);
            Planet planet = mock(Planet.class);
            when(planet.getPrintableName(any())).thenReturn(planetName);
            when(sys.getPrimaryPlanet()).thenReturn(planet);
            return sys;
        }

        private Campaign mockCampaign() {
            Campaign campaign = mock(Campaign.class);
            CampaignOptions opts = mock(CampaignOptions.class);
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(campaign.getName()).thenReturn(CAMPAIGN_NAME);
            when(campaign.getMainForcePersonnel()).thenReturn(mainForce);
            when(campaign.getCampaignOptions()).thenReturn(opts);
            when(campaign.isOverridingCommandCircuitRequirements()).thenReturn(false);
            when(campaign.isGM()).thenReturn(false);
            when(opts.isUseFactionStandingCommandCircuitSafe()).thenReturn(false);
            when(campaign.getFutureAtBContracts()).thenReturn(List.of());
            return campaign;
        }

        private Person mockPerson() {
            return new Person("Test", "Person", null, "MERC");
        }

        private void wire(ILocation child, ILocation parent) {
            LocationNode.LocationManager.setLocation(child, parent);
        }

        @Test
        void noParent_allColumnsReturnDash() {
            Person person = mockPerson();
            Campaign campaign = mockCampaign();
            assertEquals("-", PersonnelTableModelColumn.LOCATION_SYSTEM.getCellValue(campaign, person));
            assertEquals("-", PersonnelTableModelColumn.LOCATION_PLANET.getCellValue(campaign, person));
            assertEquals(CAMPAIGN_NAME, PersonnelTableModelColumn.LOCATION_NAME.getCellValue(campaign, person));
            assertEquals("-",
                  PersonnelTableModelColumn.DESTINATION_SYSTEM.getCellValue(campaign, person));
            assertEquals("-",
                  PersonnelTableModelColumn.DESTINATION_PLANET.getCellValue(campaign, person));
            assertEquals("-",
                  PersonnelTableModelColumn.DESTINATION_NAME.getCellValue(campaign, person));
        }

        @Nested
        class MainForce {

            private PlanetarySystem mainSys;
            private CurrentLocation mainLoc;
            private Person person;
            private Campaign campaign;

            @BeforeEach
            void setUp() {
                mainSys = mockSystem("Galatea", "Galatea");
                mainLoc = new CurrentLocation(mainSys, 0.0);
                wire(mainForce, mainLoc);
                person = mockPerson();
                wire(person, mainForce);
                campaign = mockCampaign();
            }

            @Test
            void locationSystem_returnsCampaignSystem() {
                assertEquals("Galatea",
                      PersonnelTableModelColumn.LOCATION_SYSTEM.getCellValue(campaign, person));
            }

            @Test
            void locationPlanet_returnsCampaignPlanet() {
                assertEquals("Galatea",
                      PersonnelTableModelColumn.LOCATION_PLANET.getCellValue(campaign, person));
            }

            @Test
            void locationName_returnsCampaignName() {
                assertEquals(CAMPAIGN_NAME,
                      PersonnelTableModelColumn.LOCATION_NAME.getCellValue(campaign, person));
            }

            @Test
            void locationName_stillReturnsCampaignNameWhenTraveling() {
                PlanetarySystem destSys = mockSystem("Terra", "Terra");
                when(mainSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
                mainLoc.setJumpPath(new JumpPath(new ArrayList<>(List.of(mainSys, destSys))));

                assertEquals(CAMPAIGN_NAME,
                      PersonnelTableModelColumn.LOCATION_NAME.getCellValue(campaign, person));
            }

            @Test
            void destination_allDashWhenNotTraveling() {
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_SYSTEM.getCellValue(campaign, person));
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_PLANET.getCellValue(campaign, person));
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_NAME.getCellValue(campaign, person));
            }
        }

        @Nested
        class AcademyArrived {

            private AcademyCampusLocation campus;
            private Person person;
            private Campaign campaign;

            @BeforeEach
            void setUp() {
                PlanetarySystem academySys = mockSystem("New Avalon", "New Avalon");
                FixedLocation fixedLoc = new FixedLocation(academySys);
                campus = new AcademyCampusLocation("SLDFNaval", "SLDF Naval Academy");
                wire(campus, fixedLoc);
                CurrentLocation cl = new CurrentLocation(academySys, 0.0);
                wire(cl, campus);
                person = mockPerson();
                wire(person, cl);
                campaign = mockCampaign();
            }

            @Test
            void locationSystem_returnsAcademySystem() {
                assertEquals("New Avalon",
                      PersonnelTableModelColumn.LOCATION_SYSTEM.getCellValue(campaign, person));
            }

            @Test
            void locationPlanet_returnsAcademyPlanet() {
                assertEquals("New Avalon",
                      PersonnelTableModelColumn.LOCATION_PLANET.getCellValue(campaign, person));
            }

            @Test
            void locationName_returnsAcademyName() {
                assertEquals("SLDF Naval Academy",
                      PersonnelTableModelColumn.LOCATION_NAME.getCellValue(campaign, person));
            }

            @Test
            void destination_allDashWhenNotTraveling() {
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_SYSTEM.getCellValue(campaign, person));
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_PLANET.getCellValue(campaign, person));
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_NAME.getCellValue(campaign, person));
            }
        }

        @Nested
        class InTransit {

            private PlanetarySystem originSys;
            private PlanetarySystem academySys;
            private FixedLocation fixedLoc;
            private AcademyCampusLocation campus;
            private Campaign campaign;

            @BeforeEach
            void setUp() {
                originSys = mockSystem("Galatea", "Galatea");
                academySys = mockSystem("New Avalon", "New Avalon");
                fixedLoc = new FixedLocation(academySys);
                campus = new AcademyCampusLocation("SLDFNaval", "SLDF Naval Academy");
                wire(campus, fixedLoc);
                campaign = mockCampaign();
            }

            private Person buildTravelingPerson(CurrentLocation cl) {
                wire(cl, campus);
                Person person = mockPerson();
                wire(person, cl);
                return person;
            }

            @Test
            void destinationSystem_returnsLastSystemInPath() {
                when(originSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
                CurrentLocation cl = new CurrentLocation(originSys, 3.0);
                cl.setJumpPath(new JumpPath(new ArrayList<>(List.of(originSys, academySys))));
                Person person = buildTravelingPerson(cl);

                assertEquals("New Avalon", PersonnelTableModelColumn.DESTINATION_SYSTEM.getCellValue(campaign, person));
            }

            @Test
            void destinationPlanet_returnsLastSystemPrimaryPlanet() {
                when(originSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
                CurrentLocation cl = new CurrentLocation(originSys, 3.0);
                cl.setJumpPath(new JumpPath(new ArrayList<>(List.of(originSys, academySys))));
                Person person = buildTravelingPerson(cl);

                assertEquals("New Avalon", PersonnelTableModelColumn.DESTINATION_PLANET.getCellValue(campaign, person));
            }

            @Test
            void destinationName_outboundToAcademy_returnsAcademyName() {
                when(originSys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
                CurrentLocation cl = new CurrentLocation(originSys, 3.0);
                cl.setJumpPath(new JumpPath(new ArrayList<>(List.of(originSys, academySys))));
                Person person = buildTravelingPerson(cl);

                assertEquals("SLDF Naval Academy",
                      PersonnelTableModelColumn.DESTINATION_NAME.getCellValue(campaign, person));
            }

            @Test
            void destinationName_returnFromAcademy_returnsCampaignName() {
                PlanetarySystem homeSys = mockSystem("Galatea", "Galatea");
                when(academySys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
                when(academySys.getRechargeTime(any(), anyBoolean())).thenReturn(0.0);
                CurrentLocation cl = new CurrentLocation(academySys, 10.0);
                cl.setJumpPath(new JumpPath(new ArrayList<>(List.of(academySys, homeSys))));
                Person person = buildTravelingPerson(cl);

                assertEquals(CAMPAIGN_NAME,
                      PersonnelTableModelColumn.DESTINATION_NAME.getCellValue(campaign, person));
            }
        }

        @Nested
        class BaseArrived {

            private PlayerBase base;
            private Person person;
            private Campaign campaign;

            @BeforeEach
            void setUp() {
                PlanetarySystem baseSys = mockSystem("Outreach", "Outreach");
                FixedLocation fixedLoc = new FixedLocation(baseSys);
                base = new PlayerBase(fixedLoc);
                base.setDisplayName("Wolf's Dragoons HQ");
                person = mockPerson();
                wire(person, base.getBasePersonnel());
                campaign = mockCampaign();
            }

            @Test
            void locationName_returnsBaseName() {
                assertEquals("Wolf's Dragoons HQ",
                      PersonnelTableModelColumn.LOCATION_NAME.getCellValue(campaign, person));
            }

            @Test
            void locationSystem_returnsBaseSystem() {
                assertEquals("Outreach",
                      PersonnelTableModelColumn.LOCATION_SYSTEM.getCellValue(campaign, person));
            }

            @Test
            void locationPlanet_returnsBasePlanet() {
                assertEquals("Outreach",
                      PersonnelTableModelColumn.LOCATION_PLANET.getCellValue(campaign, person));
            }

            @Test
            void destination_allDashWhenNotTraveling() {
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_SYSTEM.getCellValue(campaign, person));
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_PLANET.getCellValue(campaign, person));
                assertEquals("-", PersonnelTableModelColumn.DESTINATION_NAME.getCellValue(campaign, person));
            }
        }
    }
}

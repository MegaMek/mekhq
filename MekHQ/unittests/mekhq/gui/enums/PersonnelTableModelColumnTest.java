/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.sorter.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PersonnelTableModelColumnTest {
    //region Variable Declarations
    private static final PersonnelTableModelColumn[] columns = PersonnelTableModelColumn.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsRank() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.RANK) {
                assertTrue(personnelTableModelColumn.isRank());
            } else {
                assertFalse(personnelTableModelColumn.isRank());
            }
        }
    }

    @Test
    public void testIsFirstName() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.FIRST_NAME) {
                assertTrue(personnelTableModelColumn.isFirstName());
            } else {
                assertFalse(personnelTableModelColumn.isFirstName());
            }
        }
    }

    @Test
    public void testIsLastName() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.LAST_NAME) {
                assertTrue(personnelTableModelColumn.isLastName());
            } else {
                assertFalse(personnelTableModelColumn.isLastName());
            }
        }
    }

    @Test
    public void testIsPreNominal() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.PRE_NOMINAL) {
                assertTrue(personnelTableModelColumn.isPreNominal());
            } else {
                assertFalse(personnelTableModelColumn.isPreNominal());
            }
        }
    }

    @Test
    public void testIsGivenName() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.GIVEN_NAME) {
                assertTrue(personnelTableModelColumn.isGivenName());
            } else {
                assertFalse(personnelTableModelColumn.isGivenName());
            }
        }
    }

    @Test
    public void testIsSurname() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.SURNAME) {
                assertTrue(personnelTableModelColumn.isSurname());
            } else {
                assertFalse(personnelTableModelColumn.isSurname());
            }
        }
    }

    @Test
    public void testIsBloodname() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.BLOODNAME) {
                assertTrue(personnelTableModelColumn.isBloodname());
            } else {
                assertFalse(personnelTableModelColumn.isBloodname());
            }
        }
    }

    @Test
    public void testIsPostNominal() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.POST_NOMINAL) {
                assertTrue(personnelTableModelColumn.isPostNominal());
            } else {
                assertFalse(personnelTableModelColumn.isPostNominal());
            }
        }
    }

    @Test
    public void testIsCallsign() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.CALLSIGN) {
                assertTrue(personnelTableModelColumn.isCallsign());
            } else {
                assertFalse(personnelTableModelColumn.isCallsign());
            }
        }
    }

    @Test
    public void testIsAge() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.AGE) {
                assertTrue(personnelTableModelColumn.isAge());
            } else {
                assertFalse(personnelTableModelColumn.isAge());
            }
        }
    }

    @Test
    public void testIsPersonnelStatus() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.PERSONNEL_STATUS) {
                assertTrue(personnelTableModelColumn.isPersonnelStatus());
            } else {
                assertFalse(personnelTableModelColumn.isPersonnelStatus());
            }
        }
    }

    @Test
    public void testIsGender() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.GENDER) {
                assertTrue(personnelTableModelColumn.isGender());
            } else {
                assertFalse(personnelTableModelColumn.isGender());
            }
        }
    }

    @Test
    public void testIsSkillLevel() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.SKILL_LEVEL) {
                assertTrue(personnelTableModelColumn.isSkillLevel());
            } else {
                assertFalse(personnelTableModelColumn.isSkillLevel());
            }
        }
    }

    @Test
    public void testIsPersonnelRole() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.PERSONNEL_ROLE) {
                assertTrue(personnelTableModelColumn.isPersonnelRole());
            } else {
                assertFalse(personnelTableModelColumn.isPersonnelRole());
            }
        }
    }

    @Test
    public void testIsUnitAssignment() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.UNIT_ASSIGNMENT) {
                assertTrue(personnelTableModelColumn.isUnitAssignment());
            } else {
                assertFalse(personnelTableModelColumn.isUnitAssignment());
            }
        }
    }

    @Test
    public void testIsForce() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.FORCE) {
                assertTrue(personnelTableModelColumn.isForce());
            } else {
                assertFalse(personnelTableModelColumn.isForce());
            }
        }
    }

    @Test
    public void testIsDeployed() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.DEPLOYED) {
                assertTrue(personnelTableModelColumn.isDeployed());
            } else {
                assertFalse(personnelTableModelColumn.isDeployed());
            }
        }
    }

    @Test
    public void testIsMek() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.MEK) {
                assertTrue(personnelTableModelColumn.isMek());
            } else {
                assertFalse(personnelTableModelColumn.isMek());
            }
        }
    }

    @Test
    public void testIsGroundVehicle() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.GROUND_VEHICLE) {
                assertTrue(personnelTableModelColumn.isGroundVehicle());
            } else {
                assertFalse(personnelTableModelColumn.isGroundVehicle());
            }
        }
    }

    @Test
    public void testIsNavalVehicle() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.NAVAL_VEHICLE) {
                assertTrue(personnelTableModelColumn.isNavalVehicle());
            } else {
                assertFalse(personnelTableModelColumn.isNavalVehicle());
            }
        }
    }

    @Test
    public void testIsVTOL() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.VTOL) {
                assertTrue(personnelTableModelColumn.isVTOL());
            } else {
                assertFalse(personnelTableModelColumn.isVTOL());
            }
        }
    }

    @Test
    public void testIsAerospace() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.AEROSPACE) {
                assertTrue(personnelTableModelColumn.isAerospace());
            } else {
                assertFalse(personnelTableModelColumn.isAerospace());
            }
        }
    }

    @Test
    public void testIsConventionalAircraft() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.CONVENTIONAL_AIRCRAFT) {
                assertTrue(personnelTableModelColumn.isConventionalAircraft());
            } else {
                assertFalse(personnelTableModelColumn.isConventionalAircraft());
            }
        }
    }

    @Test
    public void testIsVessel() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.VESSEL) {
                assertTrue(personnelTableModelColumn.isVessel());
            } else {
                assertFalse(personnelTableModelColumn.isVessel());
            }
        }
    }

    @Test
    public void testIsBattleArmour() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.BATTLE_ARMOUR) {
                assertTrue(personnelTableModelColumn.isBattleArmour());
            } else {
                assertFalse(personnelTableModelColumn.isBattleArmour());
            }
        }
    }

    @Test
    public void testIsSmallArms() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.SMALL_ARMS) {
                assertTrue(personnelTableModelColumn.isSmallArms());
            } else {
                assertFalse(personnelTableModelColumn.isSmallArms());
            }
        }
    }

    @Test
    public void testIsAntiMek() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.ANTI_MEK) {
                assertTrue(personnelTableModelColumn.isAntiMek());
            } else {
                assertFalse(personnelTableModelColumn.isAntiMek());
            }
        }
    }

    @Test
    public void testIsArtillery() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.ARTILLERY) {
                assertTrue(personnelTableModelColumn.isArtillery());
            } else {
                assertFalse(personnelTableModelColumn.isArtillery());
            }
        }
    }

    @Test
    public void testIsTactics() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TACTICS) {
                assertTrue(personnelTableModelColumn.isTactics());
            } else {
                assertFalse(personnelTableModelColumn.isTactics());
            }
        }
    }

    @Test
    public void testIsStrategy() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.STRATEGY) {
                assertTrue(personnelTableModelColumn.isStrategy());
            } else {
                assertFalse(personnelTableModelColumn.isStrategy());
            }
        }
    }

    @Test
    public void testIsLeadership() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.LEADERSHIP) {
                assertTrue(personnelTableModelColumn.isLeadership());
            } else {
                assertFalse(personnelTableModelColumn.isLeadership());
            }
        }
    }

    @Test
    public void testIsTechMek() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TECH_MEK) {
                assertTrue(personnelTableModelColumn.isTechMek());
            } else {
                assertFalse(personnelTableModelColumn.isTechMek());
            }
        }
    }

    @Test
    public void testIsTechAero() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TECH_AERO) {
                assertTrue(personnelTableModelColumn.isTechAero());
            } else {
                assertFalse(personnelTableModelColumn.isTechAero());
            }
        }
    }

    @Test
    public void testIsTechMechanic() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TECH_MECHANIC) {
                assertTrue(personnelTableModelColumn.isTechMechanic());
            } else {
                assertFalse(personnelTableModelColumn.isTechMechanic());
            }
        }
    }

    @Test
    public void testIsTechBA() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TECH_BA) {
                assertTrue(personnelTableModelColumn.isTechBA());
            } else {
                assertFalse(personnelTableModelColumn.isTechBA());
            }
        }
    }

    @Test
    public void testIsTechVessel() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TECH_VESSEL) {
                assertTrue(personnelTableModelColumn.isTechVessel());
            } else {
                assertFalse(personnelTableModelColumn.isTechVessel());
            }
        }
    }

    @Test
    public void testIsMedical() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.MEDICAL) {
                assertTrue(personnelTableModelColumn.isMedical());
            } else {
                assertFalse(personnelTableModelColumn.isMedical());
            }
        }
    }

    @Test
    public void testIsAdministration() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.ADMINISTRATION) {
                assertTrue(personnelTableModelColumn.isAdministration());
            } else {
                assertFalse(personnelTableModelColumn.isAdministration());
            }
        }
    }

    @Test
    public void testIsNegotiation() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.NEGOTIATION) {
                assertTrue(personnelTableModelColumn.isNegotiation());
            } else {
                assertFalse(personnelTableModelColumn.isNegotiation());
            }
        }
    }

    @Test
    public void testIsScrounge() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.SCROUNGE) {
                assertTrue(personnelTableModelColumn.isScrounge());
            } else {
                assertFalse(personnelTableModelColumn.isScrounge());
            }
        }
    }

    @Test
    public void testIsInjuries() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.INJURIES) {
                assertTrue(personnelTableModelColumn.isInjuries());
            } else {
                assertFalse(personnelTableModelColumn.isInjuries());
            }
        }
    }

    @Test
    public void testIsKills() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.KILLS) {
                assertTrue(personnelTableModelColumn.isKills());
            } else {
                assertFalse(personnelTableModelColumn.isKills());
            }
        }
    }

    @Test
    public void testIsSalary() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.SALARY) {
                assertTrue(personnelTableModelColumn.isSalary());
            } else {
                assertFalse(personnelTableModelColumn.isSalary());
            }
        }
    }

    @Test
    public void testIsXP() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.XP) {
                assertTrue(personnelTableModelColumn.isXP());
            } else {
                assertFalse(personnelTableModelColumn.isXP());
            }
        }
    }

    @Test
    public void testIsOriginFaction() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.ORIGIN_FACTION) {
                assertTrue(personnelTableModelColumn.isOriginFaction());
            } else {
                assertFalse(personnelTableModelColumn.isOriginFaction());
            }
        }
    }

    @Test
    public void testIsOriginPlanet() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.ORIGIN_PLANET) {
                assertTrue(personnelTableModelColumn.isOriginPlanet());
            } else {
                assertFalse(personnelTableModelColumn.isOriginPlanet());
            }
        }
    }

    @Test
    public void testIsBirthday() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.BIRTHDAY) {
                assertTrue(personnelTableModelColumn.isBirthday());
            } else {
                assertFalse(personnelTableModelColumn.isBirthday());
            }
        }
    }

    @Test
    public void testIsRecruitmentDate() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.RECRUITMENT_DATE) {
                assertTrue(personnelTableModelColumn.isRecruitmentDate());
            } else {
                assertFalse(personnelTableModelColumn.isRecruitmentDate());
            }
        }
    }

    @Test
    public void testIsLastRankChangeDate() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.LAST_RANK_CHANGE_DATE) {
                assertTrue(personnelTableModelColumn.isLastRankChangeDate());
            } else {
                assertFalse(personnelTableModelColumn.isLastRankChangeDate());
            }
        }
    }

    @Test
    public void testIsDueDate() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.DUE_DATE) {
                assertTrue(personnelTableModelColumn.isDueDate());
            } else {
                assertFalse(personnelTableModelColumn.isDueDate());
            }
        }
    }

    @Test
    public void testIsRetirementDate() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.RETIREMENT_DATE) {
                assertTrue(personnelTableModelColumn.isRetirementDate());
            } else {
                assertFalse(personnelTableModelColumn.isRetirementDate());
            }
        }
    }

    @Test
    public void testIsDeathDate() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.DEATH_DATE) {
                assertTrue(personnelTableModelColumn.isDeathDate());
            } else {
                assertFalse(personnelTableModelColumn.isDeathDate());
            }
        }
    }

    @Test
    public void testIsCommander() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.COMMANDER) {
                assertTrue(personnelTableModelColumn.isCommander());
            } else {
                assertFalse(personnelTableModelColumn.isCommander());
            }
        }
    }

    @Test
    public void testIsFounder() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.FOUNDER) {
                assertTrue(personnelTableModelColumn.isFounder());
            } else {
                assertFalse(personnelTableModelColumn.isFounder());
            }
        }
    }

    @Test
    public void testIsClanPersonnel() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.CLAN_PERSONNEL) {
                assertTrue(personnelTableModelColumn.isClanPersonnel());
            } else {
                assertFalse(personnelTableModelColumn.isClanPersonnel());
            }
        }
    }

    @Test
    public void testIsMarriageable() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.MARRIAGEABLE) {
                assertTrue(personnelTableModelColumn.isMarriageable());
            } else {
                assertFalse(personnelTableModelColumn.isMarriageable());
            }
        }
    }

    @Test
    public void testIsDivorceable() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.DIVORCEABLE) {
                assertTrue(personnelTableModelColumn.isDivorceable());
            } else {
                assertFalse(personnelTableModelColumn.isDivorceable());
            }
        }
    }

    @Test
    public void testIsTryingToConceive() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TRYING_TO_CONCEIVE) {
                assertTrue(personnelTableModelColumn.isTryingToConceive());
            } else {
                assertFalse(personnelTableModelColumn.isTryingToConceive());
            }
        }
    }

    @Test
    public void testIsImmortal() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.IMMORTAL) {
                assertTrue(personnelTableModelColumn.isImmortal());
            } else {
                assertFalse(personnelTableModelColumn.isImmortal());
            }
        }
    }

    @Test
    public void testIsToughness() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.TOUGHNESS) {
                assertTrue(personnelTableModelColumn.isToughness());
            } else {
                assertFalse(personnelTableModelColumn.isToughness());
            }
        }
    }

    @Test
    public void testIsEdge() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.EDGE) {
                assertTrue(personnelTableModelColumn.isEdge());
            } else {
                assertFalse(personnelTableModelColumn.isEdge());
            }
        }
    }

    @Test
    public void testIsSPACount() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.SPA_COUNT) {
                assertTrue(personnelTableModelColumn.isSPACount());
            } else {
                assertFalse(personnelTableModelColumn.isSPACount());
            }
        }
    }

    @Test
    public void testIsImplantCount() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.IMPLANT_COUNT) {
                assertTrue(personnelTableModelColumn.isImplantCount());
            } else {
                assertFalse(personnelTableModelColumn.isImplantCount());
            }
        }
    }

    @Test
    public void testIsPortraitPath() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            if (personnelTableModelColumn == PersonnelTableModelColumn.PORTRAIT_PATH) {
                assertTrue(personnelTableModelColumn.isPortraitPath());
            } else {
                assertFalse(personnelTableModelColumn.isPortraitPath());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetCellValue() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetDisplayText() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetToolTipText() {

    }

    @Test
    public void testGetWidth() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            switch (personnelTableModelColumn) {
                case PERSON:
                case UNIT_ASSIGNMENT:
                    assertEquals(125, personnelTableModelColumn.getWidth());
                    break;
                case RANK:
                case FIRST_NAME:
                case GIVEN_NAME:
                case DEPLOYED:
                    assertEquals(70, personnelTableModelColumn.getWidth());
                    break;
                case LAST_NAME:
                case SURNAME:
                case BLOODNAME:
                case CALLSIGN:
                case SKILL_LEVEL:
                case SALARY:
                    assertEquals(50, personnelTableModelColumn.getWidth());
                    break;
                case PERSONNEL_ROLE:
                case FORCE:
                    assertEquals(100, personnelTableModelColumn.getWidth());
                    break;
                default:
                    assertEquals(20, personnelTableModelColumn.getWidth());
                    break;
            }
        }
    }

    @Test
    public void testGetAlignment() {
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            switch (personnelTableModelColumn) {
                case PERSON:
                case RANK:
                case FIRST_NAME:
                case LAST_NAME:
                case PRE_NOMINAL:
                case GIVEN_NAME:
                case SURNAME:
                case BLOODNAME:
                case POST_NOMINAL:
                case CALLSIGN:
                case GENDER:
                case SKILL_LEVEL:
                case PERSONNEL_ROLE:
                case UNIT_ASSIGNMENT:
                case FORCE:
                case DEPLOYED:
                    assertEquals(SwingConstants.LEFT, personnelTableModelColumn.getAlignment());
                    break;
                case SALARY:
                    assertEquals(SwingConstants.RIGHT, personnelTableModelColumn.getAlignment());
                    break;
                default:
                    assertEquals(SwingConstants.CENTER, personnelTableModelColumn.getAlignment());
                    break;
            }
        }
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testIsVisible() {

    }

    @Test
    public void testGetComparator() {
        final Campaign mockCampaign = mock(Campaign.class);
        for (final PersonnelTableModelColumn personnelTableModelColumn : columns) {
            switch (personnelTableModelColumn) {
                case RANK:
                    assertInstanceOf(PersonRankStringSorter.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
                case AGE:
                case BIRTHDAY:
                case RECRUITMENT_DATE:
                case LAST_RANK_CHANGE_DATE:
                case DUE_DATE:
                case RETIREMENT_DATE:
                case DEATH_DATE:
                    assertInstanceOf(DateStringComparator.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
                case SKILL_LEVEL:
                    assertInstanceOf(LevelSorter.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
                case MEK:
                case GROUND_VEHICLE:
                case NAVAL_VEHICLE:
                case VTOL:
                case AEROSPACE:
                case CONVENTIONAL_AIRCRAFT:
                case VESSEL:
                case BATTLE_ARMOUR:
                case SMALL_ARMS:
                case ANTI_MEK:
                case ARTILLERY:
                case TACTICS:
                case STRATEGY:
                case LEADERSHIP:
                case TECH_MEK:
                case TECH_AERO:
                case TECH_MECHANIC:
                case TECH_BA:
                case TECH_VESSEL:
                case MEDICAL:
                case ADMINISTRATION:
                case NEGOTIATION:
                case SCROUNGE:
                    assertInstanceOf(BonusSorter.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
                case INJURIES:
                case KILLS:
                case XP:
                case TOUGHNESS:
                case EDGE:
                case SPA_COUNT:
                case IMPLANT_COUNT:
                    assertInstanceOf(IntegerStringSorter.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
                case SALARY:
                    assertInstanceOf(FormattedNumberSorter.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
                default:
                    assertInstanceOf(NaturalOrderComparator.class,
                            personnelTableModelColumn.getComparator(mockCampaign));
                    break;
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
        assertEquals(resources.getString("PersonnelTableModelColumn.RANK.text"),
                PersonnelTableModelColumn.RANK.toString());
        assertEquals(resources.getString("PersonnelTableModelColumn.PERSONNEL_STATUS.text"),
                PersonnelTableModelColumn.PERSONNEL_STATUS.toString());
        assertEquals(resources.getString("PersonnelTableModelColumn.FORCE.text"),
                PersonnelTableModelColumn.FORCE.toString());
        assertEquals(resources.getString("PersonnelTableModelColumn.TECH_MECHANIC.text"),
                PersonnelTableModelColumn.TECH_MECHANIC.toString());
        assertEquals(resources.getString("PersonnelTableModelColumn.RECRUITMENT_DATE.text"),
                PersonnelTableModelColumn.RECRUITMENT_DATE.toString());
        assertEquals(resources.getString("PersonnelTableModelColumn.PORTRAIT_PATH.text"),
                PersonnelTableModelColumn.PORTRAIT_PATH.toString());
    }
}

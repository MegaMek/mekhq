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
package mekhq.campaign.personnel.enums;

import mekhq.MekHQ;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfessionTest {
    //region Variable Declarations
    private static final Profession[] professions = Profession.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("Profession.MECHWARRIOR.toolTipText"),
                Profession.MECHWARRIOR.getToolTipText());
        assertEquals(resources.getString("Profession.ADMINISTRATOR.toolTipText"),
                Profession.ADMINISTRATOR.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsMechWarrior() {
        for (final Profession profession : professions) {
            if (profession == Profession.MECHWARRIOR) {
                assertTrue(profession.isMechWarrior());
            } else {
                assertFalse(profession.isMechWarrior());
            }
        }
    }

    @Test
    public void testIsAerospace() {
        for (final Profession profession : professions) {
            if (profession == Profession.AEROSPACE) {
                assertTrue(profession.isAerospace());
            } else {
                assertFalse(profession.isAerospace());
            }
        }
    }

    @Test
    public void testIsVehicle() {
        for (final Profession profession : professions) {
            if (profession == Profession.VEHICLE) {
                assertTrue(profession.isVehicle());
            } else {
                assertFalse(profession.isVehicle());
            }
        }
    }

    @Test
    public void testIsNaval() {
        for (final Profession profession : professions) {
            if (profession == Profession.NAVAL) {
                assertTrue(profession.isNaval());
            } else {
                assertFalse(profession.isNaval());
            }
        }
    }

    @Test
    public void testIsInfantry() {
        for (final Profession profession : professions) {
            if (profession == Profession.INFANTRY) {
                assertTrue(profession.isInfantry());
            } else {
                assertFalse(profession.isInfantry());
            }
        }
    }

    @Test
    public void testIsTech() {
        for (final Profession profession : professions) {
            if (profession == Profession.TECH) {
                assertTrue(profession.isTech());
            } else {
                assertFalse(profession.isTech());
            }
        }
    }

    @Test
    public void testIsMedical() {
        for (final Profession profession : professions) {
            if (profession == Profession.MEDICAL) {
                assertTrue(profession.isMedical());
            } else {
                assertFalse(profession.isMedical());
            }
        }
    }

    @Test
    public void testIsAdministrator() {
        for (final Profession profession : professions) {
            if (profession == Profession.ADMINISTRATOR) {
                assertTrue(profession.isAdministrator());
            } else {
                assertFalse(profession.isAdministrator());
            }
        }
    }

    @Test
    public void testIsCivilian() {
        for (final Profession profession : professions) {
            if (profession == Profession.CIVILIAN) {
                assertTrue(profession.isCivilian());
            } else {
                assertFalse(profession.isCivilian());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Disabled // FIXME : Windchild : Broken Test
    @Test
    public void testGetProfession() {
/*
        final Rank mockRankA = mock(Rank.class);
        when(mockRankA.getName(Profession.NAVAL)).thenReturn("Naval");
        when(mockRankA.isEmpty(Profession.NAVAL)).thenReturn(false);
        when(mockRankA.indicatesAlternativeSystem(Profession.NAVAL)).thenReturn(false);
        when(mockRankA.getName(Profession.AEROSPACE)).thenReturn("--NAVAL");
        when(mockRankA.isEmpty(Profession.AEROSPACE)).thenReturn(false);
        when(mockRankA.indicatesAlternativeSystem(Profession.AEROSPACE)).thenReturn(true);

        final Rank mockRankB = mock(Rank.class);
        when(mockRankB.getName(Profession.NAVAL)).thenReturn("-");
        when(mockRankB.isEmpty(Profession.NAVAL)).thenReturn(true);
        when(mockRankB.indicatesAlternativeSystem(Profession.NAVAL)).thenReturn(false);
        when(mockRankB.getName(Profession.AEROSPACE)).thenReturn("--NAVAL");
        when(mockRankB.isEmpty(Profession.AEROSPACE)).thenReturn(false);
        when(mockRankB.indicatesAlternativeSystem(Profession.AEROSPACE)).thenReturn(true);

        final List<Rank> ranks = new ArrayList<>();
        ranks.add(mockRankA);
        ranks.add(mockRankB);

        final RankSystem mockRankSystem = mock(RankSystem.class);
        when(mockRankSystem.getRanks()).thenReturn(ranks);

        assertEquals(Profession.NAVAL, Profession.NAVAL.getProfession(mockRankSystem, mockRankA));
        assertEquals(Profession.NAVAL, Profession.AEROSPACE.getProfession(mockRankSystem, mockRankA));
        assertEquals(Profession.NAVAL, Profession.NAVAL.getProfession(mockRankSystem, mockRankB));
        assertEquals(Profession.NAVAL, Profession.AEROSPACE.getProfession(mockRankSystem, mockRankB));
 */
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetProfessionFromBase() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetBaseProfession() {

    }

    @Test
    public void testIsEmptyProfessionMechWarrior() {
        final RankSystem mockRankSystem = mock(RankSystem.class);
        assertFalse(Profession.MECHWARRIOR.isEmptyProfession(mockRankSystem));
    }

    @Test
    public void testIsEmptyProfessionInitialRank() {
        final Map<Profession, String> mockRankNames = new HashMap<>();
        mockRankNames.put(Profession.NAVAL, "Naval");
        mockRankNames.put(Profession.AEROSPACE, "--NAVAL");

        final Rank mockRank = mock(Rank.class);
        when(mockRank.getRankNames()).thenReturn(mockRankNames);
        when(mockRank.getName(any())).thenCallRealMethod();
        when(mockRank.indicatesAlternativeSystem(any())).thenCallRealMethod();

        final List<Rank> ranks = new ArrayList<>();
        ranks.add(mockRank);

        final RankSystem mockRankSystem = mock(RankSystem.class);
        when(mockRankSystem.getRanks()).thenReturn(ranks);

        assertFalse(Profession.NAVAL.isEmptyProfession(mockRankSystem));
        assertTrue(Profession.AEROSPACE.isEmptyProfession(mockRankSystem));
    }

    @Test
    public void testIsEmptyProfessionEmptyByInvalidRedirect() {
        final Map<Profession, String> mockRankNamesA = new HashMap<>();
        mockRankNamesA.put(Profession.NAVAL, "Naval");
        mockRankNamesA.put(Profession.AEROSPACE, "--NAVAL");
        mockRankNamesA.put(Profession.CIVILIAN, "--ASF");
        mockRankNamesA.put(Profession.MEDICAL, "--NAVAL");

        final Rank mockRankA = mock(Rank.class);
        when(mockRankA.getRankNames()).thenReturn(mockRankNamesA);
        when(mockRankA.getName(any())).thenCallRealMethod();
        when(mockRankA.isEmpty(any())).thenCallRealMethod();
        when(mockRankA.indicatesAlternativeSystem(any())).thenCallRealMethod();

        final Map<Profession, String> mockRankNamesB = new HashMap<>();
        mockRankNamesB.put(Profession.NAVAL, "John");
        mockRankNamesB.put(Profession.AEROSPACE, "--NAVAL");
        mockRankNamesB.put(Profession.CIVILIAN, "-");
        mockRankNamesB.put(Profession.MEDICAL, "-");

        final Rank mockRankB = mock(Rank.class);
        when(mockRankB.getRankNames()).thenReturn(mockRankNamesB);
        when(mockRankB.getName(any())).thenCallRealMethod();
        when(mockRankB.isEmpty(any())).thenCallRealMethod();
        when(mockRankB.indicatesAlternativeSystem(any())).thenCallRealMethod();

        final List<Rank> ranks = new ArrayList<>();
        ranks.add(mockRankA);
        ranks.add(mockRankB);

        final RankSystem mockRankSystem = mock(RankSystem.class);
        when(mockRankSystem.getRanks()).thenReturn(ranks);

        assertFalse(Profession.NAVAL.isEmptyProfession(mockRankSystem));
        assertFalse(Profession.AEROSPACE.isEmptyProfession(mockRankSystem));
        assertTrue(Profession.CIVILIAN.isEmptyProfession(mockRankSystem));
        assertTrue(Profession.MEDICAL.isEmptyProfession(mockRankSystem));

        final Map<Profession, String> mockRankNamesC = new HashMap<>();
        mockRankNamesC.put(Profession.NAVAL, "-");
        mockRankNamesC.put(Profession.AEROSPACE, "-");
        mockRankNamesC.put(Profession.CIVILIAN, "-");
        mockRankNamesC.put(Profession.MEDICAL, "-");

        final Rank mockRankC = mock(Rank.class);
        when(mockRankC.getRankNames()).thenReturn(mockRankNamesC);
        when(mockRankC.getName(any())).thenCallRealMethod();
        when(mockRankC.isEmpty(any())).thenCallRealMethod();
        when(mockRankC.indicatesAlternativeSystem(any())).thenCallRealMethod();

        ranks.add(mockRankC);

        assertFalse(Profession.NAVAL.isEmptyProfession(mockRankSystem));
        assertFalse(Profession.AEROSPACE.isEmptyProfession(mockRankSystem));
        assertTrue(Profession.CIVILIAN.isEmptyProfession(mockRankSystem));
        assertTrue(Profession.MEDICAL.isEmptyProfession(mockRankSystem));

        final Map<Profession, String> mockRankNamesD = new HashMap<>();
        mockRankNamesD.put(Profession.NAVAL, "-");
        mockRankNamesD.put(Profession.AEROSPACE, "-");
        mockRankNamesD.put(Profession.CIVILIAN, "John");
        mockRankNamesD.put(Profession.MEDICAL, "-");

        final Rank mockRankD = mock(Rank.class);
        when(mockRankD.getRankNames()).thenReturn(mockRankNamesD);
        when(mockRankD.getName(any())).thenCallRealMethod();
        when(mockRankD.isEmpty(any())).thenCallRealMethod();
        when(mockRankD.indicatesAlternativeSystem(any())).thenCallRealMethod();

        ranks.add(mockRankD);

        assertFalse(Profession.NAVAL.isEmptyProfession(mockRankSystem));
        assertFalse(Profession.AEROSPACE.isEmptyProfession(mockRankSystem));
        assertFalse(Profession.CIVILIAN.isEmptyProfession(mockRankSystem));
        assertTrue(Profession.MEDICAL.isEmptyProfession(mockRankSystem));
    }

    @Test
    public void testGetAlternateProfessionRankSystem() {
        final Rank mockRank = mock(Rank.class);
        when(mockRank.getName(any())).thenReturn("--MW");

        final List<Rank> ranks = new ArrayList<>();
        ranks.add(mockRank);

        final RankSystem mockRankSystem = mock(RankSystem.class);
        when(mockRankSystem.getRanks()).thenReturn(ranks);

        assertEquals(Profession.MECHWARRIOR, Profession.AEROSPACE.getAlternateProfession(mockRankSystem));
    }

    @Test
    public void testGetAlternateProfessionRank() {
        final Rank mockRank = mock(Rank.class);

        // --MW
        when(mockRank.getName(any())).thenReturn("--MW");
        assertEquals(Profession.MECHWARRIOR, Profession.AEROSPACE.getAlternateProfession(mockRank));

        // --ADMIN
        when(mockRank.getName(any())).thenReturn("--ADMIN");
        assertEquals(Profession.ADMINISTRATOR, Profession.MECHWARRIOR.getAlternateProfession(mockRank));
    }

    @Test
    public void testGetAlternateProfessionString() {
        assertEquals(Profession.MECHWARRIOR, Profession.MECHWARRIOR.getAlternateProfession("--MW"));
        assertEquals(Profession.MECHWARRIOR, Profession.MECHWARRIOR.getAlternateProfession("--mw"));
        assertEquals(Profession.MECHWARRIOR, Profession.MECHWARRIOR.getAlternateProfession("--hi"));
        assertEquals(Profession.AEROSPACE, Profession.MECHWARRIOR.getAlternateProfession("--ASF"));
        assertEquals(Profession.VEHICLE, Profession.MECHWARRIOR.getAlternateProfession("--VEE"));
        assertEquals(Profession.NAVAL, Profession.MECHWARRIOR.getAlternateProfession("--NAVAL"));
        assertEquals(Profession.INFANTRY, Profession.MECHWARRIOR.getAlternateProfession("--INF"));
        assertEquals(Profession.TECH, Profession.MECHWARRIOR.getAlternateProfession("--TECH"));
        assertEquals(Profession.MEDICAL, Profession.MECHWARRIOR.getAlternateProfession("--MEDICAL"));
        assertEquals(Profession.ADMINISTRATOR, Profession.MECHWARRIOR.getAlternateProfession("--ADMIN"));
        assertEquals(Profession.CIVILIAN, Profession.MECHWARRIOR.getAlternateProfession("--CIVILIAN"));
    }

    @Test
    public void testGetProfessionFromPersonnelRole() {
        for (final PersonnelRole role : PersonnelRole.values()) {
            switch (role) {
                case MECHWARRIOR:
                case LAM_PILOT:
                case PROTOMECH_PILOT:
                    assertEquals(Profession.MECHWARRIOR, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case AEROSPACE_PILOT:
                case CONVENTIONAL_AIRCRAFT_PILOT:
                    assertEquals(Profession.AEROSPACE, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case GROUND_VEHICLE_DRIVER:
                case NAVAL_VEHICLE_DRIVER:
                case VTOL_PILOT:
                case VEHICLE_GUNNER:
                case VEHICLE_CREW:
                    assertEquals(Profession.VEHICLE, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case BATTLE_ARMOUR:
                case SOLDIER:
                    assertEquals(Profession.INFANTRY, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case VESSEL_PILOT:
                case VESSEL_CREW:
                case VESSEL_GUNNER:
                case VESSEL_NAVIGATOR:
                    assertEquals(Profession.NAVAL, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case MECH_TECH:
                case MECHANIC:
                case AERO_TECH:
                case BA_TECH:
                case ASTECH:
                    assertEquals(Profession.TECH, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case DOCTOR:
                case MEDIC:
                    assertEquals(Profession.MEDICAL, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case ADMINISTRATOR_COMMAND:
                case ADMINISTRATOR_LOGISTICS:
                case ADMINISTRATOR_HR:
                case ADMINISTRATOR_TRANSPORT:
                    assertEquals(Profession.ADMINISTRATOR, Profession.getProfessionFromPersonnelRole(role));
                    break;
                case DEPENDENT:
                case NONE:
                    assertEquals(Profession.CIVILIAN, Profession.getProfessionFromPersonnelRole(role));
                    break;
                default:
                    fail("Unknown Personnel Role of " + role.name());
                    break;
            }
        }
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("Profession.AEROSPACE.text"), Profession.AEROSPACE.toString());
        assertEquals(resources.getString("Profession.CIVILIAN.text"), Profession.CIVILIAN.toString());
    }
}

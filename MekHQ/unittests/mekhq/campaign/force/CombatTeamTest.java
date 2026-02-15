/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import static mekhq.campaign.force.FormationLevel.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.universe.Factions2;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CombatTeamTest {
    private static Factions2 testFactions2;

    @BeforeAll
    public static void setup() {
        testFactions2 = new Factions2("testresources/data/universe/factions");
        Factions.setInstance(Factions.loadDefault(true));
    }

    @SuppressWarnings("all") // get() without test; if it fails, test data is not loading; the test should fail
    private Faction getFaction(String code) {
        return new Faction(testFactions2.getFaction(code).get());
    }

    // Inner Sphere

    @Test
    public void testGetStandardFormationSize_InnerSphere_LanceDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LANCE.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_CompanyDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, COMPANY.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_BattalionDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BATTALION.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_RegimentDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, REGIMENT.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_BrigadeDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BRIGADE.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_DivisionDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, DIVISION.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_CorpsDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, CORPS.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_ArmyDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, ARMY.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_InnerSphere_ArmyGroupDepth() {
        // Setup
        Faction faction = getFaction("LA");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, ARMY_GROUP.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    // Clan

    @Test
    public void testGetStandardFormationSize_ClanFaction_LanceDepth() {
        // Setup
        Faction faction = getFaction("CBS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, STAR_OR_NOVA.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE, result);
    }

    @Test
    public void testGetStandardFormationSize_ClanFaction_CompanyDepth() {
        // Setup
        Faction faction = getFaction("CBS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BINARY_OR_TRINARY.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_ClanFaction_BattalionDepth() {
        // Setup
        Faction faction = getFaction("CBS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, CLUSTER.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_ClanFaction_RegimentDepth() {
        // Setup
        Faction faction = getFaction("CBS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, GALAXY.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_ClanFaction_BrigadeDepth() {
        // Setup
        Faction faction = getFaction("CBS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, TOUMAN.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3, result);
    }

    // Marian Hegemony

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_LanceDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LANCE.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_CompanyDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, COMPANY.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_BattalionDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BATTALION.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_RegimentDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, REGIMENT.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_BrigadeDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BRIGADE.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_DivisionDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, DIVISION.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_CorpsDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, CORPS.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_ArmyDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, ARMY.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_MarianHegemonyFaction_ArmyGroupDepth() {
        // Setup
        Faction faction = getFaction("MH");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, ARMY_GROUP.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    // ComStar

    @Test
    public void testGetStandardFormationSize_ComStarFaction_LanceDepth() {
        // Setup
        Faction faction = getFaction("CS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LEVEL_II_OR_CHOIR.getDepth());

        // Assert
        assertEquals(CombatTeam.LEVEL_II_SIZE, result);
    }

    @Test
    public void testGetStandardFormationSize_ComStarFaction_CompanyDepth() {
        // Setup
        Faction faction = getFaction("CS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LEVEL_III.getDepth());

        // Assert
        assertEquals(CombatTeam.LEVEL_II_SIZE * 6, result);
    }

    @Test
    public void testGetStandardFormationSize_ComStarFaction_BattalionDepth() {
        // Setup
        Faction faction = getFaction("CS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LEVEL_IV.getDepth());

        // Assert
        assertEquals(CombatTeam.LEVEL_II_SIZE * 6 * 6, result);
    }

    @Test
    public void testGetStandardFormationSize_ComStarFaction_RegimentDepth() {
        // Setup
        Faction faction = getFaction("CS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LEVEL_V.getDepth());

        // Assert
        assertEquals(CombatTeam.LEVEL_II_SIZE * 6 * 6 * 6, result);
    }

    @Test
    public void testGetStandardFormationSize_ComStarFaction_BrigadeDepth() {
        // Setup
        Faction faction = getFaction("CS");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LEVEL_VI.getDepth());

        // Assert
        assertEquals(CombatTeam.LEVEL_II_SIZE * 6 * 6 * 6 * 6, result);
    }

    // Fallback - Inner Sphere

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_LanceDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, LANCE.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_CompanyDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, COMPANY.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_BattalionDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BATTALION.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_RegimentDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, REGIMENT.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_BrigadeDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BRIGADE.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_DivisionDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, DIVISION.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_CorpsDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, CORPS.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_ArmyDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, ARMY.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackInnerSphere_ArmyGroupDepth() {
        // Setup
        var faction = getFaction("IS_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, ARMY_GROUP.getDepth());

        // Assert
        assertEquals(CombatTeam.LANCE_SIZE * 3 * 3 * 3 * 3 * 3 * 3 * 3 * 3, result);
    }

    // Fallback - Clan

    @Test
    public void testGetStandardFormationSize_FallbackClanFaction_LanceDepth() {
        // Setup
        var faction = getFaction("CLAN_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, STAR_OR_NOVA.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackClanFaction_CompanyDepth() {
        // Setup
        var faction = getFaction("CLAN_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, BINARY_OR_TRINARY.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackClanFaction_BattalionDepth() {
        // Setup
        var faction = getFaction("CLAN_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, CLUSTER.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackClanFaction_RegimentDepth() {
        // Setup
        var faction = getFaction("CLAN_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, GALAXY.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3, result);
    }

    @Test
    public void testGetStandardFormationSize_FallbackClanFaction_BrigadeDepth() {
        // Setup
        var faction = getFaction("CLAN_TAG");

        // Act
        int result = CombatTeam.getStandardFormationSize(faction, TOUMAN.getDepth());

        // Assert
        assertEquals(CombatTeam.STAR_SIZE * 3 * 3 * 3 * 3, result);
    }
}

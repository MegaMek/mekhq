/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

public class MysteryBoxTypeTest {
    //region Variable Declarations
    private static final MysteryBoxType[] types = MysteryBoxType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("MysteryBoxType.THIRD_SUCCESSION_WAR.toolTipText"),
              MysteryBoxType.THIRD_SUCCESSION_WAR.getToolTipText());
        assertEquals(resources.getString("MysteryBoxType.CLAN_EXPERIMENTAL.toolTipText"),
              MysteryBoxType.CLAN_EXPERIMENTAL.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsThirdSuccessionWar() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.THIRD_SUCCESSION_WAR) {
                assertTrue(mysteryBoxType.isThirdSuccessionWar());
            } else {
                assertFalse(mysteryBoxType.isThirdSuccessionWar());
            }
        }
    }

    @Test
    public void testIsStarLeagueRoyal() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.STAR_LEAGUE_ROYAL) {
                assertTrue(mysteryBoxType.isStarLeagueRoyal());
            } else {
                assertFalse(mysteryBoxType.isStarLeagueRoyal());
            }
        }
    }

    @Test
    public void testIsStarLeagueRegular() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.STAR_LEAGUE_REGULAR) {
                assertTrue(mysteryBoxType.isStarLeagueRegular());
            } else {
                assertFalse(mysteryBoxType.isStarLeagueRegular());
            }
        }
    }

    @Test
    public void testIsInnerSphereExperimental() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.INNER_SPHERE_EXPERIMENTAL) {
                assertTrue(mysteryBoxType.isInnerSphereExperimental());
            } else {
                assertFalse(mysteryBoxType.isInnerSphereExperimental());
            }
        }
    }

    @Test
    public void testIsClanKeshik() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.CLAN_KESHIK) {
                assertTrue(mysteryBoxType.isClanKeshik());
            } else {
                assertFalse(mysteryBoxType.isClanKeshik());
            }
        }
    }

    @Test
    public void testIsClanFrontLine() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.CLAN_FRONT_LINE) {
                assertTrue(mysteryBoxType.isClanFrontLine());
            } else {
                assertFalse(mysteryBoxType.isClanFrontLine());
            }
        }
    }

    @Test
    public void testIsClanSecondLine() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.CLAN_SECOND_LINE) {
                assertTrue(mysteryBoxType.isClanSecondLine());
            } else {
                assertFalse(mysteryBoxType.isClanSecondLine());
            }
        }
    }

    @Test
    public void testIsClanExperimental() {
        for (final MysteryBoxType mysteryBoxType : types) {
            if (mysteryBoxType == MysteryBoxType.CLAN_EXPERIMENTAL) {
                assertTrue(mysteryBoxType.isClanExperimental());
            } else {
                assertFalse(mysteryBoxType.isClanExperimental());
            }
        }
    }
    //region Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("MysteryBoxType.CLAN_KESHIK.text"), MysteryBoxType.CLAN_KESHIK.toString());
        assertEquals(resources.getString("MysteryBoxType.CLAN_SECOND_LINE.text"),
              MysteryBoxType.CLAN_SECOND_LINE.toString());
    }
}

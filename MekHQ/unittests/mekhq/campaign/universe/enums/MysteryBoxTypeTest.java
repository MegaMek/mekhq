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
package mekhq.campaign.universe.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MysteryBoxTypeTest {
    //region Variable Declarations
    private static final MysteryBoxType[] types = MysteryBoxType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
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
        assertEquals(resources.getString("MysteryBoxType.CLAN_SECOND_LINE.text"), MysteryBoxType.CLAN_SECOND_LINE.toString());
    }
}

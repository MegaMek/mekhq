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

import megamek.common.util.EncodeControl;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RankSystemTypeTest {
    //region Variable Declarations
    private static final RankSystemType[] types = RankSystemType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RankSystemType.DEFAULT.toolTipText"),
                RankSystemType.DEFAULT.getToolTipText());
        assertEquals(resources.getString("RankSystemType.USER_DATA.toolTipText"),
                RankSystemType.USER_DATA.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsDefault() {
        for (final RankSystemType rankSystemType : types) {
            if (rankSystemType == RankSystemType.DEFAULT) {
                assertTrue(rankSystemType.isDefault());
            } else {
                assertFalse(rankSystemType.isDefault());
            }
        }
    }

    @Test
    public void testIsUserData() {
        for (final RankSystemType rankSystemType : types) {
            if (rankSystemType == RankSystemType.USER_DATA) {
                assertTrue(rankSystemType.isUserData());
            } else {
                assertFalse(rankSystemType.isUserData());
            }
        }
    }

    @Test
    public void testIsCampaign() {
        for (final RankSystemType rankSystemType : types) {
            if (rankSystemType == RankSystemType.CAMPAIGN) {
                assertTrue(rankSystemType.isCampaign());
            } else {
                assertFalse(rankSystemType.isCampaign());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetFilePath() {
        assertEquals(MHQConstants.RANKS_FILE_PATH, RankSystemType.DEFAULT.getFilePath());
        assertEquals(MHQConstants.USER_RANKS_FILE_PATH, RankSystemType.USER_DATA.getFilePath());
        assertEquals("", RankSystemType.CAMPAIGN.getFilePath());
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RankSystemType.DEFAULT.text"), RankSystemType.DEFAULT.toString());
        assertEquals(resources.getString("RankSystemType.CAMPAIGN.text"), RankSystemType.CAMPAIGN.toString());
    }
}

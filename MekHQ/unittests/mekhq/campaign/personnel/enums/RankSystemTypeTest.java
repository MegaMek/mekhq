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
package mekhq.campaign.personnel.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

public class RankSystemTypeTest {
    //region Variable Declarations
    private static final RankSystemType[] types = RankSystemType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
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

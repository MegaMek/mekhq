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
package mekhq.gui.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

public class PersonnelTabViewTest {
    //region Variable Declarations
    private static final PersonnelTabView[] views = PersonnelTabView.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("PersonnelTabView.GRAPHIC.toolTipText"),
              PersonnelTabView.GRAPHIC.getToolTipText());
        assertEquals(resources.getString("PersonnelTabView.DATES.toolTipText"),
              PersonnelTabView.DATES.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsGraphic() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.GRAPHIC) {
                assertTrue(personnelTabView.isGraphic());
            } else {
                assertFalse(personnelTabView.isGraphic());
            }
        }
    }

    @Test
    public void testIsGeneral() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.GENERAL) {
                assertTrue(personnelTabView.isGeneral());
            } else {
                assertFalse(personnelTabView.isGeneral());
            }
        }
    }

    @Test
    public void testIsPilotGunnerySkills() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.PILOT_GUNNERY_SKILLS) {
                assertTrue(personnelTabView.isPilotGunnerySkills());
            } else {
                assertFalse(personnelTabView.isPilotGunnerySkills());
            }
        }
    }

    @Test
    public void testIsInfantrySkills() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.INFANTRY_SKILLS) {
                assertTrue(personnelTabView.isInfantrySkills());
            } else {
                assertFalse(personnelTabView.isInfantrySkills());
            }
        }
    }

    @Test
    public void testIsTacticalSkills() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.TACTICAL_SKILLS) {
                assertTrue(personnelTabView.isTacticalSkills());
            } else {
                assertFalse(personnelTabView.isTacticalSkills());
            }
        }
    }

    @Test
    public void testIsTechnicalSkills() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.TECHNICAL_SKILLS) {
                assertTrue(personnelTabView.isTechnicalSkills());
            } else {
                assertFalse(personnelTabView.isTechnicalSkills());
            }
        }
    }

    @Test
    public void testIsAdministrativeSkills() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.ADMINISTRATIVE_SKILLS) {
                assertTrue(personnelTabView.isAdministrativeSkills());
            } else {
                assertFalse(personnelTabView.isAdministrativeSkills());
            }
        }
    }

    @Test
    public void testIsBiographical() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.BIOGRAPHICAL) {
                assertTrue(personnelTabView.isBiographical());
            } else {
                assertFalse(personnelTabView.isBiographical());
            }
        }
    }

    @Test
    public void testIsFluff() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.FLUFF) {
                assertTrue(personnelTabView.isFluff());
            } else {
                assertFalse(personnelTabView.isFluff());
            }
        }
    }

    @Test
    public void testIsDates() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.DATES) {
                assertTrue(personnelTabView.isDates());
            } else {
                assertFalse(personnelTabView.isDates());
            }
        }
    }

    @Test
    public void testIsFlags() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.FLAGS) {
                assertTrue(personnelTabView.isFlags());
            } else {
                assertFalse(personnelTabView.isFlags());
            }
        }
    }

    @Test
    public void testIsPersonality() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.PERSONALITY) {
                assertTrue(personnelTabView.isPersonality());
            } else {
                assertFalse(personnelTabView.isPersonality());
            }
        }
    }

    @Test
    public void testIsOther() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.OTHER) {
                assertTrue(personnelTabView.isOther());
            } else {
                assertFalse(personnelTabView.isOther());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PersonnelTabView.GRAPHIC.text"), PersonnelTabView.GRAPHIC.toString());
        assertEquals(resources.getString("PersonnelTabView.TECHNICAL_SKILLS.text"),
              PersonnelTabView.TECHNICAL_SKILLS.toString());
        assertEquals(resources.getString("PersonnelTabView.OTHER.text"), PersonnelTabView.OTHER.toString());
    }
}

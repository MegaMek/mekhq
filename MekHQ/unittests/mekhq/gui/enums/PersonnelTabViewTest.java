/*
 * Copyright (c) 2022-2024 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(resources.getString("PersonnelTabView.TECHNICAL_SKILLS.text"), PersonnelTabView.TECHNICAL_SKILLS.toString());
        assertEquals(resources.getString("PersonnelTabView.OTHER.text"), PersonnelTabView.OTHER.toString());
    }
}

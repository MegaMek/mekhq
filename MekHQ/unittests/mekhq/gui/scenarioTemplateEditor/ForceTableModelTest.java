/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceGenerationMethod;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ForceTableModel}, covering the cell values including the columns intentionally left blank for player
 * and enemy/planet-owner forces.
 */
class ForceTableModelTest {

    private static ScenarioForceTemplate botForce() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ForceAlignment.Opposing.ordinal(),
              ForceGenerationMethod.BVScaled.ordinal(), 1.5, List.of(2, 4), 5, 40, UnitType.TANK);
        force.setForceName("OpFor");
        force.setGenerationOrder(3);
        force.setContributesToMapSize(true);
        return force;
    }

    private static ScenarioForceTemplate playerForce() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ForceAlignment.Player.ordinal(),
              ForceGenerationMethod.PlayerSupplied.ordinal(), 1.0, List.of(1), 5, 50,
              ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);
        force.setForceName("Player");
        return force;
    }

    @Test
    void botForceRowValues() {
        ForceTableModel model = new ForceTableModel();
        model.setForces(List.of(botForce()));

        assertEquals(1, model.getRowCount());
        assertEquals("3", model.getValueAt(0, ForceTableModel.COL_ORDER));
        assertEquals("OpFor", model.getValueAt(0, ForceTableModel.COL_FORCE_ID));
        assertEquals("1.5", model.getValueAt(0, ForceTableModel.COL_MULTIPLIER));
        assertEquals("40", model.getValueAt(0, ForceTableModel.COL_RETREAT));
        // enemy-bot force hides the contribution flags
        assertEquals("", model.getValueAt(0, ForceTableModel.COL_CONTRIBUTES_BV));
        assertEquals("", model.getValueAt(0, ForceTableModel.COL_CONTRIBUTES_UNIT_COUNT));
        assertEquals("Yes", model.getValueAt(0, ForceTableModel.COL_CONTRIBUTES_MAP_SIZE));
    }

    @Test
    void playerForceHidesMultiplierUnitTypeAndMaxWeight() {
        ForceTableModel model = new ForceTableModel();
        model.setForces(List.of(playerForce()));

        assertEquals("", model.getValueAt(0, ForceTableModel.COL_MULTIPLIER));
        assertEquals("", model.getValueAt(0, ForceTableModel.COL_UNIT_TYPE));
        assertEquals("", model.getValueAt(0, ForceTableModel.COL_MAX_WEIGHT));
    }

    @Test
    void fixedUnitCountShowsCountOrLance() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ForceAlignment.Opposing.ordinal(),
              ForceGenerationMethod.FixedUnitCount.ordinal(), 1.0, List.of(2), 5, 40, UnitType.TANK);
        force.setForceName("Fixed");
        force.setFixedUnitCount(4);

        ForceTableModel model = new ForceTableModel();
        model.setForces(List.of(force));
        assertEquals("4", model.getValueAt(0, ForceTableModel.COL_MULTIPLIER));

        force.setFixedUnitCount(-1);
        model.setForces(List.of(force));
        assertEquals("Lance", model.getValueAt(0, ForceTableModel.COL_MULTIPLIER));
    }

    @Test
    void getForceAtReturnsTheRowForce() {
        ScenarioForceTemplate force = botForce();
        ForceTableModel model = new ForceTableModel();
        model.setForces(List.of(force));
        assertEquals(force, model.getForceAt(0));
    }
}

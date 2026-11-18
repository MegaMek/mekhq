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
package mekhq.gui.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The Repair Bay works out which units need each salvaged part when it refreshes its task list, not every time a row
 * is drawn, so a salvage task's line follows the last refresh.
 */
class TaskTableModelSalvageDemandTest {
    private final PartsScenario scenario = PartsScenario.create();
    private final TaskTableModel model = new TaskTableModel(null, null);

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static String plainText(Object html) {
        return html.toString().replaceAll("<[^>]*>", "");
    }

    private static EquipmentPart mediumLaser(Unit unit) {
        return PartsScenario.unitParts(unit, EquipmentPart.class)
                     .stream()
                     .filter(equipmentPart -> "Medium Laser".equals(equipmentPart.getName()))
                     .findFirst()
                     .orElseThrow();
    }

    private void lasersOnTheirWay(Unit source, int quantity, int daysToArrive) {
        Part lasers = mediumLaser(source).clone();
        lasers.setCampaign(scenario.getCampaign());
        lasers.setQuantity(quantity);
        scenario.getCampaign().getQuartermaster().addPart(lasers, daysToArrive, false);
    }

    private Unit locustBeingStripped() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        locust.setSalvage(true);
        return locust;
    }

    @Test
    void aSalvageTaskCountsTheUnitsThatNeedItsPartOnItsOwnLine() {
        Unit stripped = locustBeingStripped();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        mediumLaser(wolverine).remove(false);

        model.setData(List.<IPartWork>of(mediumLaser(stripped)));

        String task = model.getValueAt(0, 0).toString();
        assertTrue(task.contains("<br>Needed by 1 unit<br>"), task);
    }

    @Test
    void theTooltipNamesTheUnits() {
        Unit stripped = locustBeingStripped();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        mediumLaser(wolverine).remove(false);
        IPartWork task = mediumLaser(stripped);

        model.setData(List.of(task));

        assertEquals("<html>Wolverine WVR-6R needs one</html>", TaskTableModel.getTaskTooltip(task));
    }

    @Test
    void theTooltipAddsWhenStockArrivesAboveTheUnits() {
        Unit stripped = locustBeingStripped();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        mediumLaser(wolverine).remove(false);
        lasersOnTheirWay(stripped, 4, 5);
        lasersOnTheirWay(stripped, 4, 2);
        IPartWork task = mediumLaser(stripped);

        model.setData(List.of(task));

        assertTrue(plainText(model.getValueAt(0, 0)).contains("Stock: 0 (8 coming)"));
        assertEquals("<html>8 due, the first in 2 days<br>Wolverine WVR-6R needs one</html>",
              TaskTableModel.getTaskTooltip(task));
    }

    @Test
    void aTaskNoUnitNeedsHasNoTooltip() {
        Unit stripped = locustBeingStripped();
        scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        IPartWork task = mediumLaser(stripped);

        model.setData(List.of(task));

        assertNull(TaskTableModel.getTaskTooltip(task));
    }

    @Test
    void theLineFollowsTheLastRefresh() {
        Unit stripped = locustBeingStripped();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        mediumLaser(wolverine).remove(false);
        List<IPartWork> tasks = List.of(mediumLaser(stripped));
        model.setData(tasks);

        // The Wolverine is stripped too, so it no longer waits for the laser
        wolverine.setSalvage(true);
        assertTrue(plainText(model.getValueAt(0, 0)).contains("Needed by 1 unit"));

        model.setData(tasks);
        String task = plainText(model.getValueAt(0, 0));
        assertFalse(task.contains("Needed by"), task);
    }

    @Test
    void repairTasksAreLeftAlone() {
        scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Unit damaged = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        mediumLaser(damaged).remove(false);
        MissingPart missingLaser = PartsScenario.unitParts(damaged, MissingPart.class).getFirst();

        model.setData(List.<IPartWork>of(missingLaser));

        String task = plainText(model.getValueAt(0, 0));
        assertFalse(task.contains("Needed by"), task);
        assertNull(TaskTableModel.getTaskTooltip(missingLaser));
    }
}

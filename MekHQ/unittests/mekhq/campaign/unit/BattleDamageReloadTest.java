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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.meks.MekGyro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Battle damage to a Mek's engine or gyro stays the same when the campaign is loaded again (issue #8690). In battle,
 * MegaMek marks a critical slot hit but not destroyed. Loading a campaign applies each part's hits to the unit again,
 * and that used to count only destroyed slots, so it hit fresh slots on top of the battle damage: a repairable engine
 * with three hits came back with all six slots hit and destroyed.
 */
class BattleDamageReloadTest {
    private Unit wolverine;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
    }

    /** The slot numbers of a system in a location, in slot order. */
    private static List<Integer> systemSlots(Entity entity, int system, int location) {
        List<Integer> slots = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < entity.getNumberOfCriticalSlots(location); slotIndex++) {
            CriticalSlot slot = entity.getCritical(location, slotIndex);
            if ((slot != null) && (slot.getType() == CriticalSlot.TYPE_SYSTEM) && (slot.getIndex() == system)) {
                slots.add(slotIndex);
            }
        }
        return slots;
    }

    /** Hits a slot the way a battle does: hit, not destroyed. */
    private void hitInBattle(int location, int slotIndex) {
        wolverine.getEntity().getCritical(location, slotIndex).setHit(true);
    }

    private int damagedSlots(int system) {
        Entity entity = wolverine.getEntity();
        int damaged = 0;
        for (int location = 0; location < entity.locations(); location++) {
            damaged += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, location);
        }
        return damaged;
    }

    /** What loading the campaign does to each unit once its parts are read. */
    private void reloadParts() {
        wolverine.initializeParts(true);
        wolverine.runDiagnostic(false);
    }

    private <T extends Part> T onlyPart(Class<T> partType) {
        List<T> parts = PartsScenario.unitParts(wolverine, partType);
        assertEquals(1, parts.size(), partType.getSimpleName());
        return parts.getFirst();
    }

    @Test
    void threeEngineHitsAwayFromTheFirstSlotsStayRepairableAfterAReload() {
        List<Integer> engineSlots = systemSlots(wolverine.getEntity(), Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO);
        hitInBattle(Mek.LOC_CENTER_TORSO, engineSlots.getFirst());
        hitInBattle(Mek.LOC_CENTER_TORSO, engineSlots.get(engineSlots.size() - 2));
        hitInBattle(Mek.LOC_CENTER_TORSO, engineSlots.getLast());
        wolverine.runDiagnostic(true);
        assertEquals(3, onlyPart(EnginePart.class).getHits(), "after the battle");

        reloadParts();

        assertEquals(3, onlyPart(EnginePart.class).getHits(), "after the reload");
        assertEquals(3, damagedSlots(Mek.SYSTEM_ENGINE));
    }

    @Test
    void threeEngineHitsOnTheFirstSlotsStayRepairableAfterAReload() {
        List<Integer> engineSlots = systemSlots(wolverine.getEntity(), Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO);
        for (int slotIndex : engineSlots.subList(0, 3)) {
            hitInBattle(Mek.LOC_CENTER_TORSO, slotIndex);
        }
        wolverine.runDiagnostic(true);

        reloadParts();

        assertEquals(3, onlyPart(EnginePart.class).getHits());
        assertEquals(3, damagedSlots(Mek.SYSTEM_ENGINE));
    }

    @Test
    void oneGyroHitOnTheLastGyroSlotIsStillOneHitAfterAReload() {
        List<Integer> gyroSlots = systemSlots(wolverine.getEntity(), Mek.SYSTEM_GYRO, Mek.LOC_CENTER_TORSO);
        hitInBattle(Mek.LOC_CENTER_TORSO, gyroSlots.getLast());
        wolverine.runDiagnostic(true);
        assertEquals(1, onlyPart(MekGyro.class).getHits(), "after the battle");

        reloadParts();

        assertEquals(1, onlyPart(MekGyro.class).getHits(), "after the reload");
        assertEquals(1, damagedSlots(Mek.SYSTEM_GYRO));
    }
}

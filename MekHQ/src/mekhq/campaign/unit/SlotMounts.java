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

import java.util.function.Consumer;

import megamek.common.CriticalSlot;
import megamek.common.equipment.Mounted;

/**
 * The items in a critical slot. A slot usually holds one item, but on a superheavy Mek one slot holds what two
 * standard slots would, so two one-slot items can share it. Anything done to "the item in a slot" must be
 * done to both.
 */
public final class SlotMounts {
    private SlotMounts() {}

    /**
     * Runs the action on each item in the slot: none, one, or two on a superheavy Mek.
     *
     * @param slot   the critical slot
     * @param action what to do to each item
     */
    public static void forEach(CriticalSlot slot, Consumer<Mounted<?>> action) {
        if (slot.getMount() != null) {
            action.accept(slot.getMount());
        }
        if (slot.getMount2() != null) {
            action.accept(slot.getMount2());
        }
    }
}

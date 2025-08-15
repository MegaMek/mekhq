/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public class EquipmentProposal {
    // region Variable Declarations
    protected final Unit unit;
    protected final Map<Integer, Mounted> equipment = new HashMap<>();
    protected final Map<Part, Integer> original = new HashMap<>();
    protected final Map<Part, Integer> mapped = new HashMap<>();
    // endregion Variable Declarations

    // region Constructors
    public EquipmentProposal(final Unit unit) {
        this.unit = Objects.requireNonNull(unit);
    }
    // endregion Constructors

    public Unit getUnit() {
        return unit;
    }

    public void consider(final Part part) {
        if (part instanceof EquipmentPart) {
            original.put(part, ((EquipmentPart) part).getEquipmentNum());
        } else if (part instanceof MissingEquipmentPart) {
            original.put(part, ((MissingEquipmentPart) part).getEquipmentNum());
        }
    }

    public void includeEquipment(final int equipmentNum, final Mounted<?> mount) {
        equipment.put(equipmentNum, Objects.requireNonNull(mount));
    }

    public void proposeMapping(final Part part, final int equipmentNum) {
        equipment.remove(equipmentNum);
        mapped.put(part, equipmentNum);
    }

    public Set<Part> getParts() {
        return Collections.unmodifiableSet(original.keySet());
    }

    public Set<Map.Entry<Integer, Mounted>> getEquipment() {
        return Collections.unmodifiableSet(equipment.entrySet());
    }

    public @Nullable Mounted<?> getEquipment(final int equipmentNum) {
        return equipment.get(equipmentNum);
    }

    public boolean hasProposal(final Part part) {
        return mapped.get(part) != null;
    }

    public int getOriginalMapping(final Part part) {
        final Integer originalEquipmentNum = original.get(part);
        return (originalEquipmentNum != null) ? originalEquipmentNum : -1;
    }

    public boolean isReduced() {
        for (final Part part : original.keySet()) {
            if (mapped.get(part) == null) {
                return false;
            }
        }

        return true;
    }

    public void apply() {
        for (final Part part : original.keySet()) {
            final int equipmentNum = (mapped.get(part) == null) ? -1 : mapped.get(part);

            if (part instanceof EquipmentPart) {
                ((EquipmentPart) part).setEquipmentNum(equipmentNum);
            } else if (part instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) part).setEquipmentNum(equipmentNum);
            }
        }
    }
}

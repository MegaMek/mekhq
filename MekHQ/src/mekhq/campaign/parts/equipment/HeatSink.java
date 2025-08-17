/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.equipment;

import java.util.StringJoiner;

import megamek.common.compute.Compute;
import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class HeatSink extends EquipmentPart {
    public HeatSink() {
        this(0, null, -1, false, null);
    }

    public HeatSink(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, 1.0, omniPodded, c);
    }

    @Override
    public HeatSink clone() {
        HeatSink clone = new HeatSink(getUnitTonnage(), getType(), getEquipmentNum(), omniPodded, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    /**
     * Copied from megamek.common.units.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     */
    @Override
    public Money getStickerPrice() {
        if (type.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) || type.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
            return Money.of(isOmniPodded() ? 7500 : 6000);
        } else {
            return Money.of(isOmniPodded() ? 2500 : 2000);
        }
    }

    @Override
    public MissingHeatSink getMissingPart() {
        return new MissingHeatSink(getUnitTonnage(), type, equipmentNum, omniPodded, campaign);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            int priorHits = hits;
            Mounted<?> mounted = unit.getEntity().getEquipment(equipmentNum);
            if (null != mounted) {
                if (mounted.isMissing()) {
                    remove(false);
                    return;
                }
                hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, equipmentNum,
                      mounted.getLocation());
            }
            if (checkForDestruction
                      && hits > priorHits
                      && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return isOmniPodded() ? 30 : 90;
        }
        return 120;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return isOmniPodded() ? -4 : -2;
        }
        return -1;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.HEAT_SINK;
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }

    /**
     * Gets a string containing details regarding the part, and optionally include information on its repair status.
     *
     * @param includeRepairDetails {@code true} if the details should include information such as the number of hits or
     *                             how much it would cost to repair the part.
     *
     * @return A string containing details regarding the part.
     */
    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringJoiner sj = new StringJoiner(", ");
        if (getName() != null && getName().equals("Double Heat Sink")) {
            sj.add(getTechBaseName());
        }

        if (!super.getDetails(includeRepairDetails).isEmpty()) {
            sj.add(super.getDetails(includeRepairDetails));
        }

        return sj.toString();
    }
}

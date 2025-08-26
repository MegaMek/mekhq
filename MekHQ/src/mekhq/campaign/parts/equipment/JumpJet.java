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

import megamek.common.CriticalSlot;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class JumpJet extends EquipmentPart {
    public JumpJet() {
        this(0, null, -1, false, null);
    }

    public JumpJet(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        // TODO : Memorize all entity attributes needed to calculate cost
        // TODO : As it is a part bought with one entity can be used on another entity
        // TODO : on which it would have a different price (only tonnage is taken into
        // TODO : account for compatibility)
        super(tonnage, et, equipNum, 1.0, omniPodded, c);
        this.unitTonnageMatters = true;
    }

    @Override
    public JumpJet clone() {
        JumpJet clone = new JumpJet(getUnitTonnage(), getType(), getEquipmentNum(), omniPodded, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        double ton;
        if (type.hasFlag(MiscType.F_PROTOMEK_EQUIPMENT)) {
            if (getUnitTonnage() <= 5) {
                ton = 0.05;
            } else if (getUnitTonnage() <= 9) {
                ton = 0.1;
            } else {
                ton = 0.15;
            }
        } else {
            if (getUnitTonnage() >= 90) {
                ton = 2.0;
            } else if (getUnitTonnage() >= 60) {
                ton = 1.0;
            } else {
                ton = 0.5;
            }
        }
        if (type.hasSubType(MiscType.S_IMPROVED)) {
            ton *= 2;
        }
        return ton;
    }

    /**
     * Copied from megamek.common.units.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     */
    @Override
    public Money getStickerPrice() {
        if (isOmniPodded()) {
            return Money.of(250 * getUnitTonnage());
        } else {
            return Money.of(200 * getUnitTonnage());
        }
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (null != unit) {
            return super.getDetails(includeRepairDetails);
        }
        return getUnitTonnage() + " ton unit";
    }

    @Override
    public MissingJumpJet getMissingPart() {
        return new MissingJumpJet(getUnitTonnage(), type, equipmentNum, omniPodded, campaign);
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
                hits = unit.getEntity().getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNum,
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
            return isOmniPodded() ? 30 : 60;
        }
        return 100;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        return -3;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }
}

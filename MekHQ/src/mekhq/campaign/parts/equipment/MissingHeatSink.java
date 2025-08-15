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

import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingHeatSink extends MissingEquipmentPart {
    public MissingHeatSink() {
        this(0, null, -1, false, null);
    }

    public MissingHeatSink(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, c, 1, 1.0, omniPodded);
    }

    @Override
    public int getBaseTime() {
        return isOmniPodded() ? 30 : 90;
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public HeatSink getNewPart() {
        return new HeatSink(getUnitTonnage(), type, -1, omniPodded, campaign);
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.HEAT_SINK;
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }
}

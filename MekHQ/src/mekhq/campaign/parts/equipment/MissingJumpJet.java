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

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingJumpJet extends MissingEquipmentPart {
    public MissingJumpJet() {
        this(0, null, -1, false, null);
    }

    public MissingJumpJet(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, c, 1, 1.0, omniPodded);
        this.unitTonnageMatters = true;
    }

    @Override
    public int getBaseTime() {
        return isOmniPodded() ? 30 : 60;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public JumpJet getNewPart() {
        return new JumpJet(getUnitTonnage(), type, -1, omniPodded, campaign);
    }

    @Override
    public double getTonnage() {
        double ton = 0.5;
        if (getUnitTonnage() >= 90) {
            ton = 2.0;
        } else if (getUnitTonnage() >= 60) {
            ton = 1.0;
        }
        if (type.hasSubType(MiscType.S_IMPROVED)) {
            ton *= 2;
        }
        return ton;
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }
}

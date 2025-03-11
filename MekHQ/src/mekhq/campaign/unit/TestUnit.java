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
 */

package mekhq.campaign.unit;

import megamek.common.Entity;
import mekhq.campaign.Campaign;

/**
 * This extension to units is for units that are not affiliated with the campaign and
 * so methods applied to them should not be allowed to affect the campaign structure.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class TestUnit extends Unit  {
    public TestUnit() {
        super(null, null);
    }

    public TestUnit(Entity en, Campaign c, boolean checkForDestruction) {
        super(en, c);
        initializeParts(false);
        runDiagnostic(checkForDestruction);
    }

    @Override
    public void initializeParts(boolean addParts) {
        //always return false
        super.initializeParts(false);
    }
}

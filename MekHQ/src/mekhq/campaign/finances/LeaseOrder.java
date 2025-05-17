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
package mekhq.campaign.finances;

import megamek.common.Entity;
import mekhq.campaign.Campaign;

public class LeaseOrder extends mekhq.campaign.unit.UnitOrder {
    private int daysToWait;
    private Campaign campaign;
    private Entity entity;

    /*
     * LeaseOrders is in the shopping list, it doesn't have a unit yet to attach to.
     */
    public LeaseOrder(megamek.common.Entity entity, mekhq.campaign.Campaign campaign) {
        super(entity, campaign);
        this.entity = entity;
        this.campaign = campaign;
    }

    @Override
    public void decrementDaysToWait() {
        daysToWait = daysToWait > 0 ? daysToWait-- : 0;
    }

    @Override
    public String find(int transitDays) {
        campaign.getQuartermaster().leaseUnit((megamek.common.Entity) getNewEquipment(), transitDays);
        return "<font color='" +
                     mekhq.MekHQ.getMHQOptions().getFontColorPositiveHexColor() +
                     "'><b> unit found for leasing</b>.</font> It will be delivered in " +
                     transitDays +
                     " days.";
    }

    @Override
    public String getAcquisitionName() {
        // This cannot be hyperlinked name due to the fact that we have a null unit ID
        // Also, the field this goes into does not currently support html, and would
        // need our listener attached
        // - Dylan
        return "Lease for " + getName();
    }

    // Leases don't actually COST anything...
    @Override
    public Money getTotalBuyCost() {
        return Money.zero();
    }

    @Override
    public Money getBuyCost() {
        return Money.zero();
    }
}


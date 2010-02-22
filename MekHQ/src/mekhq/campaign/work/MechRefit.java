/*
 * MechRefit.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.work;

import megamek.common.Entity;
import mekhq.campaign.Unit;

/**
 *
 * @author natit
 */
public class MechRefit extends Refit {

    public MechRefit(Unit unit, Entity target, int baseTime, int refitClass, char refitKitAvailability, int refitKitAvailabilityMod, int cost) {
        super(unit, target, baseTime, refitClass, refitKitAvailability, refitKitAvailabilityMod, cost);
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MechRefit
                && ((MechRefit) task).getUnitId() == this.getUnitId()
                && ((MechRefit) task).getTargetEntity().getModel().equals(this.getTargetEntity().getModel()));
    }
    
}

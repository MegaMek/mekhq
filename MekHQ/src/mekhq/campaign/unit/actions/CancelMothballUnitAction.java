/*
 * CancelMothballUnitAction.java
 *
 * Copyright (c) 2019 Megamek Team. All rights reserved.
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

package mekhq.campaign.unit.actions;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

/**
 * A unit action which cancels a pending mothball or activation work order.
 */
public class CancelMothballUnitAction implements IUnitAction {

    @Override
    public void Execute(Campaign campaign, Unit unit) {
        unit.cancelMothballOrActivation();
    }

}

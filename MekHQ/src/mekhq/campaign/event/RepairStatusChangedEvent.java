/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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

package mekhq.campaign.event;

import megamek.common.event.MMEvent;
import mekhq.campaign.unit.Unit;

/**
 * Triggered when a damaged unit has the repair location changed or is toggled between
 * salvage and repair. The repair site can be found with getUnit().getSite() and the
 * repair/salvage flag with getUnit().isSalvage().
 *
 */
public class RepairStatusChangedEvent extends MMEvent {

    private final Unit unit;

    public RepairStatusChangedEvent(Unit unit) {
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }
}

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
import mekhq.campaign.work.IAcquisitionWork;

/**
 * Triggered when a unit or part is added to the procurement list following a failed acquisition role.
 * A failed acquisition role should trigger a AcquisitionEvent.
 *
 */
public class ProcurementEvent extends MMEvent {

    private final IAcquisitionWork acquisition;
    
    public ProcurementEvent(IAcquisitionWork acquisition) {
        this.acquisition = acquisition;
    }
    
    public IAcquisitionWork getAcquisition() {
        return acquisition;
    }
}

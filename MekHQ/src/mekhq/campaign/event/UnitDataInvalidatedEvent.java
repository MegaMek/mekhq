/*
 * Copyright (c) 2017-2018 The MegaMek Team. All rights reserved.
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

/**
 * Triggered to signal that one/some unit/units may have been modified and that
 * therefore any derived data should be refreshed from the units list
 * in {@linkplain mekhq.campaign.Campaign}.
 */
public final class UnitDataInvalidatedEvent extends MMEvent {

    // If you plan to make singleton out of this, please make sure
    // that megamek.common.event.EventBus doesn't rely on object identity
    // for any internal workings (eg: that it never puts events into a Set at
    // some point) and also that this is documented in EventBus (least future
    // modifications may break the implementation here).

}

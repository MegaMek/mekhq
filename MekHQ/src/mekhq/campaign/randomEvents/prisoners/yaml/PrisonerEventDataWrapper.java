/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners.yaml;

import java.util.List;

import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;

/**
 * A wrapper class for managing a list of {@link PrisonerEventData}. This class provides getter and setter methods to
 * access and modify the list of prisoner events.
 */
public class PrisonerEventDataWrapper {
    private List<PrisonerEventData> events;

    /**
     * @return a {@link List} of {@link PrisonerEventData} objects representing the prisoner events.
     */
    public List<PrisonerEventData> getEvents() {
        return events;
    }

    /**
     * Sets the list of {@link PrisonerEventData} for this wrapper.
     *
     * @param events a {@link List} of {@link PrisonerEventData} objects to be associated with this wrapper.
     */
    public void setEvents(List<PrisonerEventData> events) {
        this.events = events;
    }
}

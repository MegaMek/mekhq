/*
 * Copyright (C) 2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.randomEvents.prisoners.yaml;

import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;

import java.util.List;

/**
 * A wrapper class for managing a list of {@link PrisonerEventData}.
 * This class provides getter and setter methods to access and modify the list
 * of prisoner events.
 */
public class PrisonerEventDataWrapper {
    private List<PrisonerEventData> events;

    /**
     * @return a {@link List} of {@link PrisonerEventData} objects representing
     * the prisoner events.
     */
    public List<PrisonerEventData> getEvents() {
        return events;
    }

    /**
     * Sets the list of {@link PrisonerEventData} for this wrapper.
     *
     * @param events a {@link List} of {@link PrisonerEventData} objects to be
     *               associated with this wrapper.
     */
    public void setEvents(List<PrisonerEventData> events) {
        this.events = events;
    }
}

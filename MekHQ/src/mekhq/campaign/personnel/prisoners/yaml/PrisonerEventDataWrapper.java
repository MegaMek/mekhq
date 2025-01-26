/*
 * Copyright (C) 2025 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.prisoners.yaml;

import mekhq.campaign.personnel.prisoners.PrisonerEventData;

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

/*
 * IAutosaveService.java
 *
 * Copyright (c) 2019 MekHQ Team. All rights reserved.
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

package mekhq.service;

import mekhq.campaign.Campaign;

import java.util.Calendar;

/**
 * Handles the possible auto-save situations
 * in MekHQ.
 */
public interface IAutosaveService {
    /**
     * Handles auto-saving when the day of the campaign advances.
     *
     * @param campaign Campaign to save
     * @param calendar the calendar to determine when to save
     */
    void requestDayAdvanceAutosave(Campaign campaign, Calendar calendar);

    /**
     * Handles auto-saving before a mission starts.
     *
     * @param campaign Campaign to save
     */
    void requestBeforeMissionAutosave(Campaign campaign);
}

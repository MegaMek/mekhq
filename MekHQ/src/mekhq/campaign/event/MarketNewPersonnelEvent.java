/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.event;

import java.util.Collections;
import java.util.List;

import megamek.common.event.MMEvent;
import mekhq.campaign.personnel.Person;

/**
 * Triggered when new potential recruits are available on the personnel market
 *
 * @author Neoancient
 *
 */
public class MarketNewPersonnelEvent extends MMEvent {

    private final List<Person> newPersonnel;

    public MarketNewPersonnelEvent(List<Person> newPersonnel) {
        this.newPersonnel = Collections.unmodifiableList(newPersonnel);
    }

    /**
     * @return An unmodifiable list of new recruits available
     */
    public List<Person> getPersonnel() {
        return newPersonnel;
    }

}

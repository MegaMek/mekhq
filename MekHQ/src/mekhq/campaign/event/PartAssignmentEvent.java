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

import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

/**
 * Triggered when a part is assigned to a tech for repair. If getTech() == null, the tech has
 * been removed from the assignment.
 *
 */
public class PartAssignmentEvent extends PartChangedEvent {
    
    private final Person tech;
    
    public PartAssignmentEvent(Part part, Person tech) {
        super(part);
        this.tech = tech;
    }
    
    public Person getTech() {
        return tech;
    }

}

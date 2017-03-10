/*
 * Copyright (C) 2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.event;

import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.personnel.Person;

/**
 * An event fired for every person who just came back from battle, dead or alive.
 * <p>
 * Event handlers can modify the status before its get passed on to MekHQ. This event
 * gets fired for both your own personnel as well as possible prisoners. If
 * an event handler sets the prisoner's status to "dead", they will not show
 * up in the list of prisoners made; this is a way to "filter out" specific
 * people who shouldn't be taken prisoner.
 */
public class PersonBattleFinishedEvent extends PersonChangedEvent {
    private PersonStatus status;
    
    public PersonBattleFinishedEvent(Person person, PersonStatus status) {
        super(person);
        this.status = status;
    }
    
    public PersonStatus getStatus() {
        return status;
    }
    
    public void setStatus(PersonStatus status) {
        this.status = status;
    }
}

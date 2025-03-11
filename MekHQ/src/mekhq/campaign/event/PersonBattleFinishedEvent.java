/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

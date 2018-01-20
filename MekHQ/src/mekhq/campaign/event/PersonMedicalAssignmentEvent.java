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

import mekhq.campaign.personnel.Person;

/**
 * Triggered when a patient is assigned to or removed from a doctor's care.
 *
 */
public class PersonMedicalAssignmentEvent extends PersonChangedEvent {

    private final Person patient;

    public PersonMedicalAssignmentEvent(Person doctor, Person patient) {
        super(doctor);
        this.patient = patient;
    }

    public Person getDoctor() {
        return getPerson();
    }

    public Person getPatient() {
        return patient;
    }

}

/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel;

import mekhq.MekHqXmlSerializable;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public abstract class AbstractPerson implements Serializable, MekHqXmlSerializable {
    //region Variable Declarations
    private static final long serialVersionUID = 2190101430016271321L;

    protected UUID id;

    protected String fullName;
    protected String givenName;
    protected String surname;
    protected String honorific;
    protected String maidenName;
    protected String callsign;

    protected int gender;
    protected PersonnelStatus status;

    protected LocalDate birthday;
    protected LocalDate dateOfDeath;

    protected String biography;
    protected Faction originFaction;
    protected Planet originPlanet;

    protected String portraitCategory;
    protected String portraitFile;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractPerson() {

    }
    //endregion Constructors
}

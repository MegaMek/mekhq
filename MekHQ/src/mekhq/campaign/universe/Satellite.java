/*
 * Satellite.java
 *
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This is an object for satellites (i.e. moons around a planet)
 *
 * @author Aaron Gullickson (aarongullickson at gmail.com)
 */
@XmlRootElement(name = "satellite")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Satellite {
    private String name;
    private String size;
    private String icon;

    public String getDescription() {
        return name + " (" + size + ")";
    }

    public String getIcon() {
        return icon;
    }
}

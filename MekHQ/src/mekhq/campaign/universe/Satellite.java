/*
 * Satellite.java
 *
 * Copyright (C) 2019 MegaMek team
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

package mekhq.campaign.universe;


import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is an object for satellites (i.e. moons around a planet)
 *
 * @author Aaron Gullickson <aarongullickson at gmail.com>
 */

@XmlRootElement(name="satellite")
@XmlAccessorType(XmlAccessType.FIELD)
public class Satellite implements Serializable {
	private static final long serialVersionUID = 8910811489755566896L;
	
	@XmlAttribute(name="size")
    private String size;

    @XmlValue
    private String name;
    
    public String getDescription() {
        return name + " (" + size + ")";
    }
}
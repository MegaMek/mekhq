/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mekhq.adapter.LocalDateAdapter;
import mekhq.campaign.universe.enums.HiringHallLevel;

import java.time.LocalDate;

@XmlRootElement(name = "hiringHall")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class HiringHallOverride {
    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    private LocalDate start;
    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    private LocalDate end;
    @XmlElement(name = "level")
    private HiringHallLevel level;


    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public HiringHallLevel getLevel() {
        return level;
    }

    public void setLevel(HiringHallLevel level) {
        this.level = level;
    }

    public boolean isActive(LocalDate date) {
        return date.isAfter(start) && date.isBefore(end);
    }
}

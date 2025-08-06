/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
@XmlRootElement(name = "newsItem")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class NewsItem {
    @XmlTransient
    private LocalDate date;
    @XmlTransient
    private Precision datePrecision;
    private String headline;
    @XmlElement(name = "desc")
    private String description;
    private String service;
    private String location;

    @XmlElement(name = "date")
    private String dateString;

    // ids will only be assigned when news is read in for the year
    transient private int id;

    public NewsItem() {
        this.headline = "None";
        this.location = null;
        this.date = null;
        this.description = null;
        this.service = null;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = ObjectUtility.nonNull(headline, this.headline);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        id = i;
    }

    public int getYear() {
        return date.getYear();
    }

    /**
     * Finalize this news item's date according to its precision.
     */
    public void finalizeDate() {
        if ((null == date) || (null == datePrecision) || (datePrecision == Precision.DAY)) {
            return;
        }

        int maxRandomDays;
        switch (datePrecision) {
            case MONTH:
                maxRandomDays = Math.toIntExact(ChronoUnit.DAYS.between(date, date.plus(1, ChronoUnit.MONTHS)));
                break;
            case YEAR:
                maxRandomDays = Math.toIntExact(ChronoUnit.DAYS.between(date, date.plus(1, ChronoUnit.YEARS)));
                break;
            case DECADE:
                maxRandomDays = Math.toIntExact(ChronoUnit.DAYS.between(date, date.plus(1, ChronoUnit.DECADES)));
                break;
            default:
                return;
        }
        date = date.plusDays(Compute.randomInt(maxRandomDays));
        datePrecision = Precision.DAY;
    }

    // Precision-aware year checker
    public boolean isInYear(int year) {
        if (null == date) {
            return false;
        }
        if (datePrecision == Precision.DECADE) {
            return ((year / 10) * 10 == date.getYear());
        }
        return year == date.getYear();
    }

    public String getPrefix() {
        String prefix = "";
        if (null != location) {
            prefix = location;
        }
        if (null != service) {
            if (!prefix.isEmpty()) {
                prefix += " ";
            }
            prefix += "[" + service + "]";
        }
        if (!prefix.isEmpty()) {
            prefix += " - ";
        }
        return prefix;
    }

    public String getHeadlineForReport() {
        String s = getPrefix() + "<b>" + getHeadline() + "</b>";
        if (null != description) {
            s += " [<a href='NEWS|" + getId() + "'>read more</a>]";
        }
        return s;
    }

    public String getFullDescription() {
        return "<h1>" + getHeadline() + "</h1>" + description;
    }

    // JAXB marshalling support

    @SuppressWarnings({ "unused" })
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (null != dateString) {
            dateString = dateString.trim().toUpperCase(Locale.ROOT);
            // Try to parse and set proper precision
            if (dateString.matches("^\\d\\d\\dX$")) {
                date = MHQXMLUtility.parseDate(dateString.substring(0, 3));
                datePrecision = Precision.DECADE;
            } else if (dateString.matches("^\\d\\d\\d\\d$")) {
                date = MHQXMLUtility.parseDate(dateString);
                datePrecision = Precision.YEAR;
            } else if (dateString.matches("^\\d\\d\\d\\d-\\d\\d$")) {
                date = MHQXMLUtility.parseDate(dateString);
                datePrecision = Precision.MONTH;
            } else {
                date = MHQXMLUtility.parseDate(dateString);
                datePrecision = Precision.DAY;
            }
        }
    }

    /** News precision enum */
    public enum Precision {DAY, MONTH, YEAR, DECADE}
}

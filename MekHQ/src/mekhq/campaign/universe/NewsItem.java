/*
 * NewsItem.java
 *
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.util.Locale;
import java.util.Random;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.chrono.GJChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import mekhq.Utilities;

/**
 * NewsItem
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
@XmlRootElement(name="newsItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class NewsItem {
    private final static DateTimeFormatter FORMATTER =
        DateTimeFormat.forPattern("yyyy-MM-dd").withChronology(GJChronology.getInstanceUTC());

    @XmlTransient
    private DateTime date;
    @XmlTransient
    private Precision datePrecision;
    private String headline;
    @XmlElement(name="desc")
    private String description;
    private String service;
    private String location;

    @XmlElement(name="date")
    private String dateString;

    //ids will only be assigned when news is read in for the year
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
        this.headline = Utilities.nonNull(headline, this.headline);
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

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
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

    /** Finalize this news item's date according to its precision, using the supplied seed */
    public void finalizeDate(long seed) {
        if((null == date) || (null == datePrecision) || (datePrecision == Precision.DAY)) {
            return;
        }
        Random rnd = new Random(seed + date.getMillis());
        int maxRandomDays = 0;
        switch(datePrecision) {
            case MONTH:
                maxRandomDays = Days.daysBetween(date, date.plusMonths(1)).getDays();
                break;
            case YEAR:
                maxRandomDays = Days.daysBetween(date, date.plusYears(1)).getDays();
                break;
            case DECADE:
                maxRandomDays = Days.daysBetween(date, date.plusYears(10)).getDays();
                break;
            default:
                return;
        }
        date = date.plusDays(rnd.nextInt(maxRandomDays));
        datePrecision = Precision.DAY;
    }

    // Precision-aware year checker
    public boolean isInYear(int year) {
        if(null == date) {
            return false;
        }
        if(datePrecision == Precision.DECADE) {
            return ((year / 10) * 10 == date.getYear());
        }
        return year == date.getYear();
    }

    public String getPrefix() {
        String prefix = "";
        if(null != location) {
            prefix = location;
        }
        if(null != service) {
            if(!prefix.isEmpty()) {
                prefix += " ";
            }
            prefix += "[" + service + "]";
        }
        if(!prefix.isEmpty()) {
            prefix += " - ";
        }
        return prefix;
    }

    public String getHeadlineForReport() {
        String s = getPrefix() + "<b>" + getHeadline() + "</b>";
        if(null != description) {
            s += " [<a href='NEWS|" + getId() + "'>read more</a>]";
        }
        return s;
    }

    public String getFullDescription() {
        String s = "<html><h1>" + getHeadline() + "</h1>(" + date.toString(FORMATTER) + ")<br><p>" + getPrefix() + description + "</p></html>";
        return s;
    }

    // JAXB marshalling support

    @SuppressWarnings({ "unused" })
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if(null != dateString) {
            dateString = dateString.trim().toUpperCase(Locale.ROOT);
            // Try to parse and set proper precision
            if(dateString.matches("^\\d\\d\\dX$")) {
                date = FORMATTER.parseDateTime(dateString.substring(0, 3) + "0-01-01");
                datePrecision = Precision.DECADE;
            } else if(dateString.matches("^\\d\\d\\d\\d$")) {
                date = FORMATTER.parseDateTime(dateString + "-01-01");
                datePrecision = Precision.YEAR;
            } else if(dateString.matches("^\\d\\d\\d\\d-\\d\\d$")) {
                date = FORMATTER.parseDateTime(dateString + "-01");
                datePrecision = Precision.MONTH;
            } else {
                date = FORMATTER.parseDateTime(dateString);
                datePrecision = Precision.DAY;
            }
        }
    }

    /** News precision enum */
    public static enum Precision { DAY, MONTH, YEAR, DECADE }
}

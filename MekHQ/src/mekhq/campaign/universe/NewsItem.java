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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;
import org.joda.time.chrono.GJChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import mekhq.Utilities;
import mekhq.adapter.DateAdapter;

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
    
    @XmlJavaTypeAdapter(DateAdapter.class)
    private DateTime date;
    private String headline;
    @XmlElement(name="desc")
    private String description;
    private String service;
    private String location;
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
}
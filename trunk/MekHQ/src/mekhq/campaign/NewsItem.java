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

package mekhq.campaign;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * NewsItem
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class NewsItem {
   
    private Date date;
    private String headline;
    private String description;
    private String service;
    private String location;
    
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
    
    public String getDescription() {
        return description;
    }
    
    public String getService() {
        return service;
    }

    public Date getDate() {
        return date;
    }
    
    public int getYear() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
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
            s += " [<a href='NEWS|" + getHeadline() + "|" + getFullDescription() + "'>read more</a>]";
        }
        return s;
    }
    
    public String getFullDescription() {
        String s = "<html><h1>" + getHeadline() + "</h1>(" + new SimpleDateFormat("d-MMMM-yyyy").format(date) + ")<br><p>" + getPrefix() + description + "</p></html>";
        return s;
    }
    
    public static NewsItem getNewsItemFromXML(Node wn) throws DOMException, ParseException {
        NewsItem retVal = new NewsItem();
        NodeList nl = wn.getChildNodes();
        
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("headline")) {
                retVal.headline = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("service")) {
                retVal.service = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                retVal.location = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                retVal.description = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                retVal.date = df.parse(wn2.getTextContent().trim());
            } 
        }
        return retVal;
    }
    
}
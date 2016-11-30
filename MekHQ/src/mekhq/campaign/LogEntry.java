/*
 * LogEntry.java
 * 
 * Copyright (C) 2009-2016 MegaMek team
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import mekhq.MekHQ;
import mekhq.MekHQOptions;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LogEntry implements MekHqXmlSerializable {
    private String type;
    private Date date;
    private String desc;
    private static final SimpleDateFormat DATE_FORMAT = MekHQOptions.getInstance().getDateFormatDataStorage();

    public LogEntry() {
        this(null, "", null);
    }
    
    public LogEntry(Date date, String desc) {
        this(date, desc, null);
    }
    
    public LogEntry(Date date, String desc, String type) {
        this.date = date;
        this.desc = Objects.requireNonNull(desc);
        this.type = type;
    }
    
    public void setDate(Date d) {
        this.date = d;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDesc(String d) {
        this.desc = Objects.requireNonNull(d);
    }
    
    public String getDesc() {
        return desc;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isType(String type) {
        return Objects.equals(this.type, type);
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(MekHqXmlUtil.indentStr(indent)).append("<logEntry>");
        if(null != date) {
            sb.append("<date>").append(DATE_FORMAT.format(date)).append("</date>");
        }
        sb.append("<desc>").append(MekHqXmlUtil.escape(desc)).append("</desc>");
        if(null != type) {
            sb.append("<type>").append(MekHqXmlUtil.escape(type)).append("</type>");
        }
        sb.append("</logEntry>");
        pw1.println(sb.toString());
    }
    
    public static LogEntry generateInstanceFromXML(Node wn) {
        LogEntry retVal = new LogEntry();
        
        try {    
            // Okay, now load fields!
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.desc = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    retVal.type = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    retVal.date = DATE_FORMAT.parse(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.logError(ex);
        }
        
        return retVal;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(null != date) {
            sb.append("[").append(DATE_FORMAT.format(date)).append("] ");
        }
        sb.append(desc);
        return sb.toString();
    }
    
    @Override
    public LogEntry clone() {
        return new LogEntry(getDate(), getDesc(), getType());
    }
}
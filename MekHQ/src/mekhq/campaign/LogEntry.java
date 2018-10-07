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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LogEntry implements Cloneable, MekHqXmlSerializable {

    private static final SimpleDateFormat dateFormat() {
        // LATER centralise date formatting so that every class doesn't have its own format and - possibly - switch to java.time
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
    }

    public LogEntry(Date date, String desc) {
        this(date, desc, null);
    }
    
    public LogEntry(Date date, String desc, String type) {
        this.date = date;
        this.desc = desc != null ? desc : ""; //$NON-NLS-1$
        this.type = type;
    }

    private Date date;
    private String desc; // non-null
    private String type;

    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public void setDesc(String desc) {
        this.desc = desc != null ? desc : ""; //$NON-NLS-1$
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void writeToXml(PrintWriter pw, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(MekHqXmlUtil.indentStr(indent)).append("<logEntry>"); //$NON-NLS-1$
        if (date != null)    sb.append("<date>").append(dateFormat().format(date)).append("</date>"); //$NON-NLS-1$ //$NON-NLS-2$
        assert desc != null; sb.append("<desc>").append(MekHqXmlUtil.escape(desc)).append("</desc>"); //$NON-NLS-1$ //$NON-NLS-2$
        if (type != null)    sb.append("<type>").append(MekHqXmlUtil.escape(type)).append("</type>");  //$NON-NLS-1$//$NON-NLS-2$
        sb.append("</logEntry>"); //$NON-NLS-1$
        pw.println(sb.toString());
    }

    public static LogEntry generateInstanceFromXML(Node wn) {
        final String METHOD_NAME = "generateInstanceFromXML(Node)"; //$NON-NLS-1$

        Date   date = null;
        String desc = null;
        String type = null;

        try {
            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node   node = nl.item(x);
                String nname = node.getNodeName();
                if (nname.equals("desc")) { //$NON-NLS-1$
                    desc = MekHqXmlUtil.unEscape(node.getTextContent());
                } else if (nname.equals("type")) { //$NON-NLS-1$
                    type = MekHqXmlUtil.unEscape(node.getTextContent());
                } else if (nname.equals("date")) { //$NON-NLS-1$
                    date = dateFormat().parse(node.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(LogEntry.class, METHOD_NAME, ex);
            return null;
        }

        return new LogEntry(date, desc, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (null != date) sb.append("[").append(dateFormat().format(date)).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append(desc);
        if (null != type) sb.append(" (").append(type).append(")");  //$NON-NLS-1$//$NON-NLS-2$
        return sb.toString();
    }

    @Override
    public LogEntry clone() {
        return new LogEntry(date, desc, type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, desc, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        LogEntry other = (LogEntry) obj;
        return Objects.equals(date, other.date)
            && desc.equals(other.desc)
            && Objects.equals(type, other.type);
    }

}

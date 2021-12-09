/*
 * Copyright (C) 2018 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.log;

import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Objects;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LogEntry implements Cloneable, MekHqXmlSerializable {
    private LocalDate date;
    private String desc; // non-null
    private LogEntryType type;

    // Keep protected so that only specific Log Entries, with a defined type, are created
    protected LogEntry(LocalDate date, String desc) {
        this(date, desc, null);
    }

    // Keep protected so that only specific Log Entries, with a defined type, are created
    protected LogEntry(LocalDate date, String desc, LogEntryType type) {
        this.date = date;
        this.desc = (desc != null) ? desc : "";
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = (desc != null) ? desc : "";
    }

    public LogEntryType getType() {
        return type;
    }

    public void setType(LogEntryType type) {
        this.type = type;
    }

    @Override
    public void writeToXml(PrintWriter pw, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(MekHqXmlUtil.indentStr(indent)).append("<logEntry>");
        if (date != null) {
            sb.append("<date>").append(MekHqXmlUtil.saveFormattedDate(date)).append("</date>");
        }
        sb.append("<desc>").append(MekHqXmlUtil.escape(desc)).append("</desc>");
        if (type != null) {
            sb.append("<type>").append(MekHqXmlUtil.escape(type.toString())).append("</type>");
        }
        sb.append("</logEntry>");
        pw.println(sb.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (null != date) {
            sb.append("[").append(date.toString()).append("] ");
        }
        sb.append(desc);
        if (null != type) {
            sb.append(" (").append(type).append(")");
        }
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
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }

        LogEntry other = (LogEntry) obj;
        return Objects.equals(date, other.date)
            && desc.equals(other.desc)
            && Objects.equals(type, other.type);
    }

    /**
     * This method is called when the log entry is edited via UI
     * @param originalDate the original date of the log entry
     * @param newDate the new date of the log entry
     * @param originalDesc the original description of the log entry
     * @param newDesc the new description of the log entry
     * @param person whose person this log entry belongs
     */
    public void onLogEntryEdited(LocalDate originalDate, LocalDate newDate, String originalDesc, String newDesc, Person person) {

    }
}

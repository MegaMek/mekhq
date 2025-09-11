/*
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
package mekhq.campaign.log;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Objects;

import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class LogEntry implements Cloneable {
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

    public void writeToXML(PrintWriter printWriter, int indent) {
        printWriter.println(MHQXMLUtility.indentStr(indent) + "<logEntry>");
        if (date != null) {
            printWriter.println(MHQXMLUtility.indentStr(indent + 1) + "<date><![CDATA[" + date + "]]></date>");
        }
        printWriter.println(MHQXMLUtility.indentStr(indent + 1) + "<desc><![CDATA[" + desc + "]]></desc>");
        if (type != null) {
            printWriter.println(MHQXMLUtility.indentStr(indent + 1) + "<type><![CDATA[" + type + "]]></type>");
        }
        printWriter.println(MHQXMLUtility.indentStr(indent) + "</logEntry>");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (null != date) {
            sb.append("[").append(date).append("] ");
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
        return Objects.equals(date, other.date) && desc.equals(other.desc) && Objects.equals(type, other.type);
    }

    /**
     * This method is called when the log entry is edited via UI
     *
     * @param originalDate the original date of the log entry
     * @param newDate      the new date of the log entry
     * @param originalDesc the original description of the log entry
     * @param newDesc      the new description of the log entry
     * @param person       whose person this log entry belongs
     */
    public void onLogEntryEdited(LocalDate originalDate, LocalDate newDate, String originalDesc, String newDesc,
          Person person) {

    }
}

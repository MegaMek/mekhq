/*
 * Version.java
 * 
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

package mekhq;


/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 * This is a very simple class that keeps track of versioning
 */
public class Version {
    
    private int snapshot;
    private int minor;
    private int major;
    
    public Version(String version) {
        if(null != version && version.length() > 0) {
            String[] temp = version.split("\\.");
            if(temp.length < 3) {
                return;
            }
            major = Integer.parseInt(temp[0]);
            minor = Integer.parseInt(temp[1]);
            String snap = temp[2].replace("-dev", "");
            if (snap.indexOf("-") > 0) {
                temp = snap.split("\\-");
                snap = temp[0];
            }
            snapshot = Integer.parseInt(snap);
        }
    }
    
    public int getSnapshot() {
        return snapshot;
    }
    
    public int getMinorVersion() {
        return minor;
    }
    
    public int getMajorVersion() {
        return major;
    }
}
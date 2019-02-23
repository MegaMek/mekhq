/*
 * Version.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2019 The MekHQ Team.
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
    private static final int NO_REVISION = -1;
    private int snapshot;
    private int minor;
    private int major;
    private int revision = NO_REVISION;
    
    public Version(String version) {
        if(null != version && version.length() > 0) {
            String[] temp = version.split("\\.");
            if(temp.length < 3) {
                return;
            }
            major = Integer.parseInt(temp[0]);
            minor = Integer.parseInt(temp[1]);
            String snap = temp[2].replace("-dev", "");
            
            // Check for other information embedded in the snapshot & handle it
            if (snap.indexOf("-") > 0) {
                // Create a new temporary array split by the hyphen
                temp = snap.split("\\-");
                // The snapshot is the first element of the new array
                snap = temp[0];
                
                // Loop over other elements searching for usable parameters
                for (int i = 1; i < temp.length; i++) {
                    // For now the only usable parameter is the revision
                    if (temp[i].matches("r[0-9]+")) {
                        String rev = temp[i].replace("r", "");
                        revision = Integer.parseInt(rev);
                    }
                }
            }
            
            // Finally, file the snapshot
            snapshot = Integer.parseInt(snap);
        }
    }

    public boolean hasRevision() {
        return revision != NO_REVISION;
    }
    
    public int getRevision() {
        return revision;
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
    
    /**
     * Compare Versions
     * 
     * Use this method to determine if this version is higher than
     * the version passed
     * 
     * @param other  The version we want to see if it is lower than this version.
     * 
     * @return true if this is higher than checkVersion
     */
    public boolean isHigherThan(String other) {
        return isHigherThan(new Version(other));
    }

    /**
     * Compare Versions
     *
     * Use this method to determine if the version passed is less than
     * this Version object.
     *
     * @param other  The version we want to see if is less than this version
     *
     * @return true if checkVersion is less than this Version object
     */
    public boolean isHigherThan(Version other) {
        return Version.compare(this, other) > 0;
    }

    /**
     * Compare Versions
     *
     * Use this method to determine if this version is lower than
     * the version passed
     *
     * @param other The version we want to see if it is higher than this version.
     *
     * @return true if this is lower than checkVersion
     */
    public boolean isLowerThan(String other) {
        return isLowerThan(new Version(other));
    }

    /**
     * Compare Versions
     *
     * Use this method to determine if this version is lower than
     * the version passed
     *
     * @param other The version we want to see if it is higher than this version.
     *
     * @return true if this is lower than checkVersion
     */
    public boolean isLowerThan(Version other) {
            return Version.compare(this, other) < 0;
    }

    /**
     * Compare versions
     * 
     * @param left  left version to compare
     * @param right right version to compare
     * 
     * @return a positive value if left is higher, a negative value if right is higher. 0 if they are equal.
     */
    private static int compare(Version left, Version right) {
        // Check Major version
        if (left.getMajorVersion() > right.getMajorVersion()) {
            return 1;
        } else if (left.getMajorVersion() < right.getMajorVersion()) {
            return -1;
        }

        // Major version is equal, try with Minor
        if (left.getMinorVersion() > right.getMinorVersion()) {
            return 1;
        } else if (left.getMinorVersion() < right.getMinorVersion()) {
            return -1;
        }

        // Minor version is also equal, try snapshot
        if (left.getSnapshot() > right.getSnapshot()) {
            return 1;
        } else if (left.getSnapshot() < right.getSnapshot()) {
            return -1;
        }

        // Snapshot is also equal, try revision if we have it
        if (left.hasRevision() && right.hasRevision()) {
            if (left.getRevision() > right.getRevision()) {
                return 1;
            } else if (left.getRevision() < right.getRevision()) {
                return -1;
            }
        }

        // If one side has a revision and the other does not, that side is higher
        if (left.hasRevision() && !right.hasRevision()) {
            return 1;
        } else if (!left.hasRevision() && right.hasRevision()) {
            return -1;
        }

        return 0;
    }
}

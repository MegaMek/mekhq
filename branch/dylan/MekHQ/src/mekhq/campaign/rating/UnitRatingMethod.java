/*
 * FieldManualMercRevMrbcRating.java
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
package mekhq.campaign.rating;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 9/24/2013
 */
public enum UnitRatingMethod {
    INTERSTELLAR_OPS("Interstellar Ops", new String[]{"Taharqa"}),
    FLD_MAN_MERCS_REV("FM: Mercenaries (rev)", new String[]{"FM: Mercenaries (rev)"});

    private String description;
    private String[] legacyDescriptions; // Old 'Taharqa' rating method renamed to Stellar Ops.  This property exists for backwards compatability.

    UnitRatingMethod(String description, String[] legacyDescriptions) {
        this.description = description;
        this.legacyDescriptions = legacyDescriptions;
    }

    public String getDescription() {
        return description;
    }

    public String[] getLegacyDescriptions() {
        return legacyDescriptions;
    }

    public boolean hasLegacyDescription(String description) {
        for (String s : getLegacyDescriptions()) {
            if (s.equalsIgnoreCase(description)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getUnitRatingMethodNames() {
        String[] methods = new String[values().length];
        for (int i = -0; i < values().length; i++) {
            methods[i] = values()[i].getDescription();
        }
        return methods;
    }

    public static UnitRatingMethod getUnitRatingMethod(String description) {
        for (UnitRatingMethod m : values()) {
            if (m.getDescription().equalsIgnoreCase(description)
                || m.hasLegacyDescription(description)) {
                return m;
            }
        }
        return null;
    }
}

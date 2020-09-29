/*
 * UnitRatingMethod.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.rating;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 9/24/2013
 */
public enum UnitRatingMethod {
    CAMPAIGN_OPS("Campaign Ops"),
    FLD_MAN_MERCS_REV("FM: Mercenaries (rev)");

    private final String description;

    UnitRatingMethod(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public static UnitRatingMethod parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        switch (text) {
            case "Campaign Ops":
            case "Taharqa":
            case "Interstellar Ops":
                return CAMPAIGN_OPS;
            case "FM: Mercenaries (rev)":
            default:
                return FLD_MAN_MERCS_REV;
        }
    }
}

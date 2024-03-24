/*
 * Copyright (c) 2016-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import megamek.common.planetaryconditions.Atmosphere;

public class PressureAdapter extends XmlAdapter<String, Integer> {

    @Override
    public Integer unmarshal(String v) throws Exception {
        switch (v) {
            case "Vacuum": return Atmosphere.VACUUM.ordinal();
            case "Trace": return Atmosphere.TRACE.ordinal();
            case "Thin":
            case "Low": return Atmosphere.THIN.ordinal();
            case "Standard":
            case "Normal": return Atmosphere.STANDARD.ordinal();
            case "High": return Atmosphere.HIGH.ordinal();
            case "Very High": return Atmosphere.VERY_HIGH.ordinal();
            default: return null;
        }
    }

    @Override
    public String marshal(Integer v) throws Exception {
        return Atmosphere.getAtmosphere(v).toString();
    }

}

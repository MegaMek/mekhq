/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.universe.Atmosphere;

public class AtmosphereAdapter extends XmlAdapter<String, Atmosphere> {
    @Override
    public Atmosphere unmarshal(String v) throws Exception {
        return Atmosphere.parseAtmosphere(v);
    }

    @Override
    public String marshal(Atmosphere v) throws Exception {
        return v.toString();
    }
}

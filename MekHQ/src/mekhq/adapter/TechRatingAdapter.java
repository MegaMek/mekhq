/*
 * Copyright (C) 2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import megamek.common.EquipmentType;

public class TechRatingAdapter extends XmlAdapter<String, Integer> {
    @Override
    public Integer unmarshal(String v) throws Exception {
        if(null == v) {
            return Integer.valueOf(EquipmentType.RATING_C);
        }
        return SocioIndustrialDataAdapter.convertRatingToCode(v);
    }

    @Override
    public String marshal(Integer v) throws Exception {
        if(null == v) {
            return null;
        }
        return SocioIndustrialDataAdapter.convertCodeToRating(v);
    }
}

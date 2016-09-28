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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import mekhq.Utilities;
import mekhq.campaign.material.MaterialUsage;

public class MaterialUsageSetAdapter extends XmlAdapter<String, EnumSet<MaterialUsage>>{
    @Override
    public EnumSet<MaterialUsage> unmarshal(String v) throws Exception {
        EnumSet<MaterialUsage> result = null;
        if((null != v) && !v.isEmpty()) {
            result = EnumSet.noneOf(MaterialUsage.class);
            for(String val : v.split(",", -1)) {
                try {
                    result.add(MaterialUsage.valueOf(val));
                } catch(IllegalArgumentException iaex) {
                    // Just ignore "wrong" data
                }
            }
        }
        return result;
    }

    @Override
    public String marshal(EnumSet<MaterialUsage> v) throws Exception {
        if(null == v) {
            return null;
        }
        List<String> values = new ArrayList<>(v.size());
        for(MaterialUsage matUsage : v) {
            values.add(matUsage.name());
        }
        return Utilities.combineString(values, ",");
    }
}

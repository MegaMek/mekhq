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
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class StringListAdapter extends XmlAdapter<String, List<String>> {
    private static final String SEPARATOR = ","; //$NON-NLS-1$

    @Override
    public List<String> unmarshal(String v) throws Exception {
        String[] values = v.split(SEPARATOR);
        List<String> result = new ArrayList<String>(values.length);
        for(String val : values) {
            result.add(val.trim());
        }
        return result;
    }

    @Override
    public String marshal(List<String> v) throws Exception {
        StringBuilder sb = new StringBuilder();
        boolean firstElement = true;
        for(String item : v) {
            if(firstElement) {
                firstElement = false;
            } else {
                sb.append(SEPARATOR);
            }
            sb.append(item.trim());
        }
        return sb.toString();
    }
}

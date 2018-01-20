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

public class BooleanValueAdapter extends XmlAdapter<String, Boolean> {
    @Override
    public Boolean unmarshal(String v) throws Exception {
        return v.equalsIgnoreCase("true") //$NON-NLS-1$
            || v.equalsIgnoreCase("yes") //$NON-NLS-1$
            || v.equals("1"); //$NON-NLS-1$
    }

    @Override
    public String marshal(Boolean v) throws Exception {
        return v.toString();
    }

}

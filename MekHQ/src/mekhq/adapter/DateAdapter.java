/*
 * Copyright (C) 2016 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHqXmlUtil;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;

public class DateAdapter extends XmlAdapter<String, LocalDate> {
    @Override
    public LocalDate unmarshal(final String xml) throws Exception {
        return MekHqXmlUtil.parseDate(xml);
    }

    @Override
    public String marshal(final LocalDate object) throws Exception {
        return MekHqXmlUtil.saveFormattedDate(object);
    }
}

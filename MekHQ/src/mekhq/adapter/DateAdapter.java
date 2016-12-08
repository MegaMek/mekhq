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

import org.joda.time.DateTime;
import org.joda.time.chrono.GJChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import mekhq.MekHQOptions;

public class DateAdapter extends XmlAdapter<String, DateTime> {
	private final static DateTimeFormatter FORMATTER =
			DateTimeFormat.forPattern(MekHQOptions.getInstance().getDateFormatDataStorage().toPattern())
			.withChronology(GJChronology.getInstanceUTC());	
	private final static DateTimeFormatter FORMATTER_FALLBACK =
			DateTimeFormat.forPattern("yyyy-MM-dd").withChronology(GJChronology.getInstanceUTC());	

	@Override
	public DateTime unmarshal(final String xml) throws Exception {
		DateTime result = null;
		
		if(xml.length() > 10) {
			result = FORMATTER.parseDateTime(xml);
		} else {
			result = FORMATTER_FALLBACK.parseDateTime(xml);
		}
		
		return result;
	}

	@Override
	public String marshal(final DateTime object) throws Exception {
		return object.toString(FORMATTER);
	}
}
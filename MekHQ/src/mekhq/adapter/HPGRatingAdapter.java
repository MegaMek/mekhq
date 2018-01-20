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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import megamek.common.EquipmentType;

public class HPGRatingAdapter extends XmlAdapter<String, Integer> {
    private final static Map<String, Integer> stringToEquipmentTypeMap = new HashMap<String, Integer>(6);
    private final static Map<Integer, String> equipmentTypeToStringMap = new HashMap<Integer, String>(6);
    static {
        stringToEquipmentTypeMap.put("A", EquipmentType.RATING_A); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("B", EquipmentType.RATING_B); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("C", EquipmentType.RATING_C); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("D", EquipmentType.RATING_D); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("X", EquipmentType.RATING_X); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_A, "A"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_B, "B"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_C, "C"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_D, "D"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_X, "X"); //$NON-NLS-1$
    }

    public static int convertRatingToCode(String rating) {
        Integer result = stringToEquipmentTypeMap.get(rating.toUpperCase(Locale.ROOT));
        return null != result ? result.intValue() : EquipmentType.RATING_X;
    }

    public static String convertCodeToRating(int code) {
        String result = equipmentTypeToStringMap.get(code);
        return null != result ? result : "?"; //$NON-NLS-1$
    }

    @Override
    public Integer unmarshal(String v) throws Exception {
        return HPGRatingAdapter.convertRatingToCode(v);
    }

    @Override
    public String marshal(Integer v) throws Exception {
        return HPGRatingAdapter.convertCodeToRating(v);
    }
}

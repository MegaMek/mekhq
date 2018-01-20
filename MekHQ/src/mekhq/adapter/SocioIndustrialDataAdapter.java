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
import mekhq.campaign.universe.Planet;

public class SocioIndustrialDataAdapter extends XmlAdapter<String, Planet.SocioIndustrialData> {
    private final static Map<String, Integer> stringToEquipmentTypeMap = new HashMap<String, Integer>(6);
    private final static Map<Integer, String> equipmentTypeToStringMap = new HashMap<Integer, String>(6);
    static {
        stringToEquipmentTypeMap.put("A", EquipmentType.RATING_A); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("B", EquipmentType.RATING_B); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("C", EquipmentType.RATING_C); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("D", EquipmentType.RATING_D); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("E", EquipmentType.RATING_E); //$NON-NLS-1$
        stringToEquipmentTypeMap.put("F", EquipmentType.RATING_F); //$NON-NLS-1$
        equipmentTypeToStringMap.put(-1, "ADV"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_A, "A"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_B, "B"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_C, "C"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_D, "D"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_E, "E"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_F, "F"); //$NON-NLS-1$
        equipmentTypeToStringMap.put(EquipmentType.RATING_X, "R"); //$NON-NLS-1$
    }
    private final String SEPARATOR = "-"; //$NON-NLS-1$

    public static int convertRatingToCode(String rating) {
        Integer result = stringToEquipmentTypeMap.get(rating.toUpperCase(Locale.ROOT));
        return null != result ? result.intValue() : EquipmentType.RATING_C;
    }

    public static String convertCodeToRating(int code) {
        String result = equipmentTypeToStringMap.get(code);
        return null != result ? result : "?"; //$NON-NLS-1$
    }

    @Override
    public Planet.SocioIndustrialData unmarshal(String v) throws Exception {
        String[] socio = v.split(SEPARATOR);
        Planet.SocioIndustrialData result = new Planet.SocioIndustrialData();
        if(socio.length >= 5) {
            result.tech = convertRatingToCode(socio[0]);
            if(result.tech == EquipmentType.RATING_C) {
                // Could be ADV or R too
                String techRating = socio[0].toUpperCase(Locale.ROOT);
                if(techRating.equals("ADV")) {
                    result.tech = -1;
                } else if(techRating.equals("R")) {
                    result.tech = EquipmentType.RATING_X;
                }
            }
            result.industry = convertRatingToCode(socio[1]);
            result.rawMaterials = convertRatingToCode(socio[2]);
            result.output = convertRatingToCode(socio[3]);
            result.agriculture = convertRatingToCode(socio[4]);
        }
        return result;
    }

    @Override
    public String marshal(Planet.SocioIndustrialData v) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(convertCodeToRating(v.tech)).append(SEPARATOR);
        sb.append(convertCodeToRating(v.industry)).append(SEPARATOR);
        sb.append(convertCodeToRating(v.rawMaterials)).append(SEPARATOR);
        sb.append(convertCodeToRating(v.output)).append(SEPARATOR);
        sb.append(convertCodeToRating(v.agriculture));
        return sb.toString();
    }
}

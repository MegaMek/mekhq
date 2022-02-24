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
import megamek.common.EquipmentType;
import mekhq.campaign.universe.SocioIndustrialData;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SocioIndustrialDataAdapter extends XmlAdapter<String, SocioIndustrialData> {
    private final static Map<String, Integer> stringToEquipmentTypeMap = new HashMap<>(6);
    private final static Map<Integer, String> equipmentTypeToStringMap = new HashMap<>(6);
    static {
        stringToEquipmentTypeMap.put("A", EquipmentType.RATING_A);
        stringToEquipmentTypeMap.put("B", EquipmentType.RATING_B);
        stringToEquipmentTypeMap.put("C", EquipmentType.RATING_C);
        stringToEquipmentTypeMap.put("D", EquipmentType.RATING_D);
        stringToEquipmentTypeMap.put("F", EquipmentType.RATING_F);
        stringToEquipmentTypeMap.put("X", EquipmentType.RATING_X);
        equipmentTypeToStringMap.put(-1, "ADV");
        equipmentTypeToStringMap.put(EquipmentType.RATING_A, "A");
        equipmentTypeToStringMap.put(EquipmentType.RATING_B, "B");
        equipmentTypeToStringMap.put(EquipmentType.RATING_C, "C");
        equipmentTypeToStringMap.put(EquipmentType.RATING_D, "D");
        equipmentTypeToStringMap.put(EquipmentType.RATING_F, "F");
        equipmentTypeToStringMap.put(EquipmentType.RATING_X, "X");
    }
    private final String SEPARATOR = "-";

    public static int convertRatingToCode(String rating) {
        Integer result = stringToEquipmentTypeMap.get(rating.toUpperCase(Locale.ROOT));
        return null != result ? result : EquipmentType.RATING_C;
    }

    public static String convertCodeToRating(int code) {
        String result = equipmentTypeToStringMap.get(code);
        return null != result ? result : "?";
    }

    @Override
    public SocioIndustrialData unmarshal(String v) throws Exception {
        String[] socio = v.split(SEPARATOR);
        SocioIndustrialData result = new SocioIndustrialData();
        if (socio.length >= 5) {
            result.tech = convertRatingToCode(socio[0]);
            if (result.tech == EquipmentType.RATING_C) {
                // Could be ADV or R too
                String techRating = socio[0].toUpperCase(Locale.ROOT);
                if (techRating.equals("ADV")) {
                    result.tech = -1;
                } else if (techRating.equals("R")) {
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
    public String marshal(SocioIndustrialData v) throws Exception {
        return convertCodeToRating(v.tech) + SEPARATOR + convertCodeToRating(v.industry) + SEPARATOR
                + convertCodeToRating(v.rawMaterials) + SEPARATOR + convertCodeToRating(v.output)
                + SEPARATOR + convertCodeToRating(v.agriculture);
    }
}

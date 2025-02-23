/*
 * SocioIndustrialData.java
 *
 * Copyright (C) 2011-2025 MegaMek team
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package mekhq.campaign.universe;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SocioIndustrialData {

    private final static Map<String, Integer> stringToEquipmentTypeMap = new HashMap<String, Integer>() {{
        put("A", EquipmentType.RATING_A);
        put("B", EquipmentType.RATING_B);
        put("C", EquipmentType.RATING_C);
        put("D", EquipmentType.RATING_D);
        put("F", EquipmentType.RATING_F);
        put("X", EquipmentType.RATING_X);
    }};

    private final static String SEPARATOR = "-";

    public static final SocioIndustrialData NONE = new SocioIndustrialData();
    static {
        NONE.tech = EquipmentType.RATING_X;
        NONE.industry = EquipmentType.RATING_X;
        NONE.rawMaterials = EquipmentType.RATING_X;
        NONE.output = EquipmentType.RATING_X;
        NONE.agriculture = EquipmentType.RATING_X;
    }

    public int tech;
    public int industry;
    public int rawMaterials;
    public int output;
    public int agriculture;

    public SocioIndustrialData() {
        this.tech = EquipmentType.RATING_X;
        this.industry = EquipmentType.RATING_X;
        this.rawMaterials = EquipmentType.RATING_X;
        this.output = EquipmentType.RATING_X;
        this.agriculture = EquipmentType.RATING_X;
    }

    public SocioIndustrialData(int t, int i, int r, int o, int a) {
        this.tech = t;
        this.industry = i;
        this.rawMaterials = r;
        this.output = o;
        this.agriculture = a;
    }

    @Override
    public String toString() {
        return ITechnology.getRatingName(tech)
                + "-" + ITechnology.getRatingName(industry)
                + "-" + ITechnology.getRatingName(rawMaterials)
                + "-" + ITechnology.getRatingName(output)
                + "-" + ITechnology.getRatingName(agriculture);
         }

    /** @return the USILR rating as a HTML description */
    public String getHTMLDescription() {
        // TODO: MHQInternationalization
        // TODO: Some way to encode "advanced" ultra-tech worlds (rating "AA" for technological sophistication)
        // TODO: Some way to encode "regressed" worlds
        // Note that rating "E" isn't used in official USILR codes, but we add them for completeness
        StringBuilder sb = new StringBuilder("<html>");
        switch (tech) {
            case -1:
                sb.append("Advanced: Ultra high-tech world<br>");
                break;
            case EquipmentType.RATING_A:
                sb.append("A: High-tech world<br>");
                break;
            case EquipmentType.RATING_B:
                sb.append("B: Advanced world<br>");
                break;
            case EquipmentType.RATING_C:
                sb.append("C: Moderately advanced world<br>");
                break;
            case EquipmentType.RATING_D:
                sb.append("D: Lower-tech world; about 21st- to 22nd-century level<br>");
                break;
            case EquipmentType.RATING_E:
                sb.append("E: Lower-tech world; about 20th century level<br>");
                break;
            case EquipmentType.RATING_F:
                sb.append("F: Primitive world<br>");
                break;
            case EquipmentType.RATING_X:
                sb.append("Regressed: Pre-industrial world<br>");
                break;
            default:
                sb.append("X: Technological sophistication unknown<br>");
                break;
        }

        switch (industry) {
            case EquipmentType.RATING_A:
                sb.append("A: Heavily industrialized<br>");
                break;
            case EquipmentType.RATING_B:
                sb.append("B: Moderately industrialized<br>");
                break;
            case EquipmentType.RATING_C:
                sb.append("C: Basic heavy industry; about 22nd century level<br>");
                break;
            case EquipmentType.RATING_D:
                sb.append("D: Low industrialization; about 20th century level<br>");
                break;
            case EquipmentType.RATING_E:
                sb.append("E: Very low industrialization; about 19th century level<br>");
                break;
            case EquipmentType.RATING_F:
                sb.append("F: No industrialization<br>");
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        switch (rawMaterials) {
            case EquipmentType.RATING_A:
                sb.append("A: Fully self-sufficient raw material production<br>");
                break;
            case EquipmentType.RATING_B:
                sb.append("B: Mostly self-sufficient raw material production<br>");
                break;
            case EquipmentType.RATING_C:
                sb.append("C: Limited raw material production<br>");
                break;
            case EquipmentType.RATING_D:
                sb.append("D: Production dependent on imports of raw materials<br>");
                break;
            case EquipmentType.RATING_E:
                sb.append("E: Production highly dependent on imports of raw materials<br>");
                break;
            case EquipmentType.RATING_F:
                sb.append("F: No economically viable local raw material production<br>");
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        switch (output) {
            case EquipmentType.RATING_A:
                sb.append("A: High industrial output<br>");
                break;
            case EquipmentType.RATING_B:
                sb.append("B: Good industrial output<br>");
                break;
            case EquipmentType.RATING_C:
                sb.append("C: Limited industrial output<br>"); // Bad for Ferengi
                break;
            case EquipmentType.RATING_D:
                sb.append("D: Negligible industrial output<br>");
                break;
            case EquipmentType.RATING_E:
                sb.append("E: Negligible industrial output<br>");
                break;
            case EquipmentType.RATING_F:
                sb.append("F: None<br>"); // Good for Ferengi
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        switch (agriculture) {
            case EquipmentType.RATING_A:
                sb.append("A: Breadbasket<br>");
                break;
            case EquipmentType.RATING_B:
                sb.append("B: Agriculturally abundant world<br>");
                break;
            case EquipmentType.RATING_C:
                sb.append("C: Modest agriculture<br>");
                break;
            case EquipmentType.RATING_D:
                sb.append("D: Poor agriculture<br>");
                break;
            case EquipmentType.RATING_E:
                sb.append("E: Very poor agriculture<br>");
                break;
            case EquipmentType.RATING_F:
                sb.append("F: Barren world<br>");
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        return sb.append("</html>").toString();
    }

    /**
     * This class is used to deserialize the SICs codes (e.g. "D-C-B-A-D") from a String into
     * a SocioIndustrialData object.
     */
    public static class SocioIndustrialDataDeserializer extends StdDeserializer<SocioIndustrialData> {

        public SocioIndustrialDataDeserializer() {
            this(null);
        }

        public SocioIndustrialDataDeserializer(final Class<?> vc) {
            super(vc);
        }

        private int convertRatingToCode(String rating) {
            Integer result = stringToEquipmentTypeMap.get(rating.toUpperCase(Locale.ROOT));
            return (null != result) ? result : EquipmentType.RATING_C;
        }
        @Override
        public SocioIndustrialData deserialize(final JsonParser jsonParser, final DeserializationContext context) {
            try {
                String[] socio = jsonParser.getText().split(SEPARATOR);
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
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}

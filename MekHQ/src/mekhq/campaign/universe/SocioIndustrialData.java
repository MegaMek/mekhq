/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.universe;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.EquipmentType;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SocioIndustrialData {

    private final static String SEPARATOR = "-";

    public static final SocioIndustrialData NONE = new SocioIndustrialData();
    static {
        NONE.tech = PlanetarySophistication.REGRESSED;
        NONE.industry = PlanetaryRating.F;
        NONE.rawMaterials = PlanetaryRating.F;
        NONE.output = PlanetaryRating.F;
        NONE.agriculture = PlanetaryRating.F;
    }

    public PlanetarySophistication tech;
    public PlanetaryRating industry;
    public PlanetaryRating rawMaterials;
    public PlanetaryRating output;
    public PlanetaryRating agriculture;

    public SocioIndustrialData() {
        this.tech = PlanetarySophistication.REGRESSED;
        this.industry = PlanetaryRating.F;
        this.rawMaterials = PlanetaryRating.F;
        this.output = PlanetaryRating.F;
        this.agriculture = PlanetaryRating.F;
    }

    public SocioIndustrialData(PlanetarySophistication t, PlanetaryRating i, PlanetaryRating r, PlanetaryRating o, PlanetaryRating a) {
        this.tech = t;
        this.industry = i;
        this.rawMaterials = r;
        this.output = o;
        this.agriculture = a;
    }

    @Override
    public String toString() {
        return tech.getName()
                + "-" + industry.getName()
                + "-" + rawMaterials.getName()
                + "-" + output.getName()
                + "-" + agriculture.getName();
        }

    /** @return the USILR rating as a HTML description */
    public String getHTMLDescription() {
        // TODO: MHQInternationalization
        // TODO: Some way to encode "advanced" ultra-tech worlds (rating "AA" for technological sophistication)
        // TODO: Some way to encode "regressed" worlds
        // Note that rating "E" isn't used in official USILR codes, but we add them for completeness
        StringBuilder sb = new StringBuilder("<html>");
        switch (tech) {
            case ADVANCED:
                sb.append("Advanced: Ultra high-tech world<br>");
                break;
            case A:
                sb.append("A: High-tech world<br>");
                break;
            case B:
                sb.append("B: Advanced world<br>");
                break;
            case C:
                sb.append("C: Moderately advanced world<br>");
                break;
            case D:
                sb.append("D: Lower-tech world; about 21st- to 22nd-century level<br>");
                break;
            case E:
                sb.append("E: Lower-tech world; about 20th century level<br>");
                break;
            case F:
                sb.append("F: Primitive world<br>");
                break;
            case REGRESSED:
                sb.append("Regressed: Pre-industrial world<br>");
                break;
            default:
                sb.append("X: Technological sophistication unknown<br>");
                break;
        }

        switch (industry) {
            case A:
                sb.append("A: Heavily industrialized<br>");
                break;
            case B:
                sb.append("B: Moderately industrialized<br>");
                break;
            case C:
                sb.append("C: Basic heavy industry; about 22nd century level<br>");
                break;
            case D:
                sb.append("D: Low industrialization; about 20th century level<br>");
                break;
            case E:
                sb.append("E: Very low industrialization; about 19th century level<br>");
                break;
            case F:
                sb.append("F: No industrialization<br>");
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        switch (rawMaterials) {
            case A:
                sb.append("A: Fully self-sufficient raw material production<br>");
                break;
            case B:
                sb.append("B: Mostly self-sufficient raw material production<br>");
                break;
            case C:
                sb.append("C: Limited raw material production<br>");
                break;
            case D:
                sb.append("D: Production dependent on imports of raw materials<br>");
                break;
            case E:
                sb.append("E: Production highly dependent on imports of raw materials<br>");
                break;
            case F:
                sb.append("F: No economically viable local raw material production<br>");
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        switch (output) {
            case A:
                sb.append("A: High industrial output<br>");
                break;
            case B:
                sb.append("B: Good industrial output<br>");
                break;
            case C:
                sb.append("C: Limited industrial output<br>"); // Bad for Ferengi
                break;
            case D:
                sb.append("D: Negligible industrial output<br>");
                break;
            case E:
                sb.append("E: Negligible industrial output<br>");
                break;
            case F:
                sb.append("F: None<br>"); // Good for Ferengi
                break;
            default:
                sb.append("X: None<br>");
                break;
        }

        switch (agriculture) {
            case A:
                sb.append("A: Breadbasket<br>");
                break;
            case B:
                sb.append("B: Agriculturally abundant world<br>");
                break;
            case C:
                sb.append("C: Modest agriculture<br>");
                break;
            case D:
                sb.append("D: Poor agriculture<br>");
                break;
            case E:
                sb.append("E: Very poor agriculture<br>");
                break;
            case F:
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

        private PlanetarySophistication getSophisticationFromString(String sophistication) {
            try {
                return PlanetarySophistication.fromName(sophistication.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // If the rating is not valid, return a default value
                return PlanetarySophistication.C;
            }
        }

        private PlanetaryRating getRatingFromString(String rating) {
            try {
                return PlanetaryRating.fromName(rating.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // If the rating is not valid, return a default value
                return PlanetaryRating.C;
            }
        }

        @Override
        public SocioIndustrialData deserialize(final JsonParser jsonParser, final DeserializationContext context) {
            try {
                String[] socio = jsonParser.getText().split(SEPARATOR);
                SocioIndustrialData result = new SocioIndustrialData();
                if (socio.length >= 5) {
                    result.tech = getSophisticationFromString(socio[0]);
                    if (result.tech == PlanetarySophistication.C) {
                        // Could be ADV or R too
                        String techRating = socio[0].toUpperCase(Locale.ROOT);
                        if (techRating.equals("ADV")) {
                            result.tech = PlanetarySophistication.ADVANCED;
                        } else if (techRating.equals("R")) {
                            result.tech = PlanetarySophistication.REGRESSED;
                        }
                    }
                    result.industry = getRatingFromString(socio[1]);
                    result.rawMaterials = getRatingFromString(socio[2]);
                    result.output = getRatingFromString(socio[3]);
                    result.agriculture = getRatingFromString(socio[4]);
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}

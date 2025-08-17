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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe;

import java.util.EnumMap;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.interfaces.ITechnology.TechRating;
import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;

public class SocioIndustrialData {

    private final static String SEPARATOR = "-";
    private static final EnumMap<PlanetarySophistication, TechRating> sophisticationToTechRating = new EnumMap<>(
          PlanetarySophistication.class);
    public static final SocioIndustrialData NONE = new SocioIndustrialData();

    static {
        sophisticationToTechRating.put(PlanetarySophistication.ADVANCED, TechRating.F);
        sophisticationToTechRating.put(PlanetarySophistication.A, TechRating.E);
        sophisticationToTechRating.put(PlanetarySophistication.B, TechRating.D);
        sophisticationToTechRating.put(PlanetarySophistication.C, TechRating.C);
        sophisticationToTechRating.put(PlanetarySophistication.D, TechRating.B);
        sophisticationToTechRating.put(PlanetarySophistication.F, TechRating.A);
        sophisticationToTechRating.put(PlanetarySophistication.REGRESSED,
              null); // Regressed worlds are regressed by any scale (CampaignOps p.51)
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
        // These are the default USILR values from CampaignOps p.126
        this.tech = PlanetarySophistication.C;
        this.industry = PlanetaryRating.D;
        this.rawMaterials = PlanetaryRating.B;
        this.output = PlanetaryRating.C;
        this.agriculture = PlanetaryRating.C;
    }

    public SocioIndustrialData(PlanetarySophistication t, PlanetaryRating i, PlanetaryRating r, PlanetaryRating o,
          PlanetaryRating a) {
        this.tech = t;
        this.industry = i;
        this.rawMaterials = r;
        this.output = o;
        this.agriculture = a;
    }

    @Override
    public String toString() {
        return tech.getName() +
                     "-" +
                     industry.getName() +
                     "-" +
                     rawMaterials.getName() +
                     "-" +
                     output.getName() +
                     "-" +
                     agriculture.getName();
    }

    /** @return the USILR rating as a HTML description */
    public String getHTMLDescription() {
        StringBuilder sb = new StringBuilder("<html>");
        switch (tech) {
            case ADVANCED:
                sb.append(
                      "Advanced: Ultra-Tech world. Hosts the most advanced research centers and universities in human space, equivalent to New Avalon or Strana Mechty.<br>");
                break;
            case A:
                sb.append(
                      "A: High-tech world. Advanced research centers and universities; best medical care; cutting-edge microelectronics industry.<br>");
                break;
            case B:
                sb.append(
                      "B: Advanced world. Access to many new technologies; hosts universities; good medical care available (though lacking in most cutting-edge medical tech); basic microelectronics industry.<br>");
                break;
            case C:
                sb.append(
                      "C: Moderately advanced world. Average local education and medical care; minimal microelectronics industry.<br>");
                break;
            case D:
                sb.append(
                      "D: Lower-tech world. Poor educational system; medical care equivalent to 21st- to 22nd-century levels; nonexistent microelectronics industry (excepting possible isolated regions run by private concerns).<br>");
                break;
            case F:
                sb.append(
                      "F: Primitive world. Inhabitants live without dependence on technology; no advanced education; medical care equivalent to twentieth-century level (at best).<br>");
                break;
            case REGRESSED:
                sb.append("Regressed: Pre-twentieth century technology, maybe Stone Age<br>");
                break;
        }

        switch (industry) {
            case A:
                sb.append("A: Heavily industrialized. Capable of manufacturing any and all complex products.<br>");
                break;
            case B:
                sb.append(
                      "B: Moderately industrialized. May produce a limited quantity and range of complex products.<br>");
                break;
            case C:
                sb.append("C: Basic heavy industry. Equivalent to roughly 22nd-century tech; fusion technology " +
                                "possible, but no complex products (including BattleMeks).<br>");
                break;
            case D:
                sb.append(
                      "D: Low industrialization. Roughly equivalent to mid-twentieth century level; fusion technology must be imported.<br>");
                break;
            case F:
                sb.append("F: No industrialization<br>");
                break;
        }

        switch (rawMaterials) {
            case A:
                sb.append(
                      "A: Fully self-sufficient. System produces all needed raw materials and may export in large quantities.<br>");
                break;
            case B:
                sb.append(
                      "B: Mostly self-sufficient. System produces all needed raw materials and may export a small surplus.<br>");
                break;
            case C:
                sb.append(
                      "C: Self-sustaining. System produces some of its needed raw materials and imports the rest.<br>");
                break;
            case D:
                sb.append(
                      "D: Dependent. System is poor in raw materials and must import most of its material needs.<br>");
                break;
            case F:
                sb.append(
                      "F: Heavy dependent. System utterly reliant on imported materials to maintain industry and population.<br>");
                break;
        }

        switch (output) {
            case A:
                sb.append(
                      "A: High output. World has wide industrial and commercial base capable of exporting most of its excess output, if sufficient space transport is available.<br>");
                break;
            case B:
                sb.append(
                      "B: Good output. World's industrial and commercial base sufficient for modest product export.<br>");
                break;
            case C:
                sb.append(
                      "C: Limited output. World has a small industrial base which limits exports; imported goods common.<br>"); // Bad for Ferengi
                break;
            case D:
                sb.append(
                      "D: Negligible output. World's industrial base insufficient for major exports; reliant on imported goods.<br>");
                break;
            case F:
                sb.append(
                      "F: No output. World must import most—if not all—of its heavy industrial and high-tech needs.<br>"); // Good for Ferengi
                break;
        }

        switch (agriculture) {
            case A:
                sb.append(
                      "A: Breadbasket. Planetary agro industries meet all local needs and sustain a thriving export trade, as allowed by available space transport.<br>");
                break;
            case B:
                sb.append(
                      "B: Abundant world. Rich agricultural environment sustains local needs and permits limited exports.<br>");
                break;
            case C:
                sb.append(
                      "C: Modest agriculture. Most food locally produced, though some agricultural needs rely on imports.<br>");
                break;
            case D:
                sb.append(
                      "D: Poor agriculture. Minimal agricultural output forces heavy reliance on off-world imports to sustain the local population.<br>");
                break;
            case F:
                sb.append(
                      "F: Barren world. World's agricultural output cannot sustain the local population without continuous off-world imports.<br>");
                break;
        }

        return sb.append("</html>").toString();
    }

    /**
     * Returns the equipment technology rating of the planet based on its sophistication. Using the USILR rating
     * conversion explained in the CampaignOps p.123 A USILR score of A corresponds to Tech Rating E, USILR B to Tech
     * Rating D, USILR C to Tech Rating C, USILR D to Tech Rating B, and USILR F to Tech Rating A. A Regressed world
     * remains regressed by any scale, while Advanced corresponds to Tech Rating F.
     */
    public @Nullable TechRating getEquipmentTechRating() {
        return sophisticationToTechRating.get(tech);
    }

    /**
     * This class is used to deserialize the SICs codes (e.g. "D-C-B-A-D") from a String into a SocioIndustrialData
     * object.
     */
    public static class SocioIndustrialDataDeserializer extends StdDeserializer<SocioIndustrialData> {

        public SocioIndustrialDataDeserializer() {
            this(null);
        }

        public SocioIndustrialDataDeserializer(final Class<?> vc) {
            super(vc);
        }

        private PlanetarySophistication getSophisticationFromString(String sophistication) {
            if (sophistication == null) {
                return PlanetarySophistication.C;
            }
            try {
                return PlanetarySophistication.fromName(sophistication.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // If the rating is not valid, return a default value but first let's evaluate 
                // some special cases to be retro compatible with the old codes
                return switch (sophistication.toUpperCase(Locale.ROOT)) {
                    case "ADV" -> PlanetarySophistication.ADVANCED;
                    case "R", "X" -> PlanetarySophistication.REGRESSED;
                    default -> PlanetarySophistication.C;
                };
            }
        }

        private PlanetaryRating getRatingFromString(String rating) {
            if (rating == null) {
                return PlanetaryRating.C;
            }
            try {
                return PlanetaryRating.fromName(rating.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // If the rating is not valid, return a default value but first let's evaluate 
                // some special cases to be retrocompatible with the old codes
                if (rating.toUpperCase(Locale.ROOT).equals("X")) {
                    return PlanetaryRating.F;
                }
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

/*
 * SocioIndustrialData.java
 *
 * Copyright (C) 2011-2016 MegaMek team
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

import megamek.common.EquipmentType;
import megamek.common.ITechnology;

public class SocioIndustrialData {
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
                + "-    " + ITechnology.getRatingName(output)
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
                sb.append("D: Negligable industrial output<br>");
                break;
            case EquipmentType.RATING_E:
                sb.append("E: Negligable industrial output<br>");
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
}

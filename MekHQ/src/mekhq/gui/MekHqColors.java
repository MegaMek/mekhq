/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.Color;

import javax.swing.UIManager;

import mekhq.MekHQ;
import mekhq.gui.preferences.ColorPreference;
import megamek.client.ui.preferences.PreferencesNode;

public class MekHqColors {

    //
    // General Colors
    //

    private static ColorPreference iconButtonColors;

    //
    // Force Colors
    //

    private static ColorPreference deployedColors;
    private static ColorPreference belowContractMinimumColors;

    //
    // Unit Colors
    //

    private static ColorPreference inTransitColors;
    private static ColorPreference refittingColors;
    private static ColorPreference mothballingColors;
    private static ColorPreference mothballedColors;
    private static ColorPreference notRepairableColors;
    private static ColorPreference nonfunctionalColors;
    private static ColorPreference needsPartsFixedColors;
    private static ColorPreference unmaintainedColors;
    private static ColorPreference uncrewedColors;

    //
    // Financial Colors
    //

    private static ColorPreference loanOverdueColors;

    //
    // Personnel Colors
    //

    private static ColorPreference injuredColors;
    private static ColorPreference healedInjuriesColors;
    private static ColorPreference paidRetirementColors;

    static {
        final PreferencesNode preferences = MekHQ.getPreferences().forClass(MekHqColors.class);

        iconButtonColors = new ColorPreference("iconButton", Color.LIGHT_GRAY, Color.BLACK);

        deployedColors = new ColorPreference("deployed", Color.LIGHT_GRAY, Color.BLACK);
        belowContractMinimumColors = new ColorPreference("belowContractMinimum", UIManager.getColor("Table.background"), Color.RED);

        inTransitColors = new ColorPreference("inTransit", Color.MAGENTA, Color.BLACK);
        refittingColors = new ColorPreference("refitting", Color.CYAN, Color.BLACK);
        mothballingColors = new ColorPreference("mothballing", new Color(153,153,255), Color.BLACK);
        mothballedColors = new ColorPreference("mothballed", new Color(204, 204, 255), Color.BLACK);
        notRepairableColors = new ColorPreference("notRepairable", new Color(190, 150, 55), Color.BLACK);
        nonfunctionalColors = new ColorPreference("nonfunctional", new Color(205, 92, 92), Color.BLACK);
        needsPartsFixedColors = new ColorPreference("needsPartsFixed", new Color(238, 238, 0), Color.BLACK);
        unmaintainedColors = new ColorPreference("unmaintainedColors", Color.ORANGE, Color.BLACK);
        uncrewedColors = new ColorPreference("uncrewed", new Color(218, 130, 255), Color.BLACK);

        loanOverdueColors = new ColorPreference("loanOverdue", Color.RED, Color.BLACK);

        injuredColors = new ColorPreference("injured", Color.RED, Color.BLACK);
        healedInjuriesColors = new ColorPreference("healed", new Color(0xee9a00), Color.BLACK);
        paidRetirementColors = new ColorPreference("paidRetirement", Color.LIGHT_GRAY, Color.BLACK);

        preferences.manage(iconButtonColors, deployedColors, belowContractMinimumColors,
                inTransitColors, refittingColors, mothballingColors, mothballedColors,
                notRepairableColors, nonfunctionalColors, needsPartsFixedColors, unmaintainedColors,
                uncrewedColors, loanOverdueColors, injuredColors, healedInjuriesColors,
                paidRetirementColors);
    }

    public ColorPreference getIconButton() {
        return iconButtonColors;
    }

    public ColorPreference getDeployed() {
        return deployedColors;
    }

    public ColorPreference getBelowContractMinimum() {
        return belowContractMinimumColors;
    }

    public ColorPreference getInTransit() {
        return inTransitColors;
    }

    public ColorPreference getRefitting() {
        return refittingColors;
    }

    public ColorPreference getMothballing() {
        return mothballingColors;
    }

    public ColorPreference getMothballed() {
        return mothballedColors;
    }

    public ColorPreference getNotRepairable() {
        return notRepairableColors;
    }

    public ColorPreference getNonFunctional() {
        return nonfunctionalColors;
    }

    public ColorPreference getNeedsPartsFixed() {
        return needsPartsFixedColors;
    }

    public ColorPreference getUnmaintained() {
        return unmaintainedColors;
    }

    public ColorPreference getUncrewed() {
        return uncrewedColors;
    }

    public ColorPreference getLoanOverdue() {
        return loanOverdueColors;
    }

    public ColorPreference getInjured() {
        return injuredColors;
    }

    public ColorPreference getHealedInjuries() {
        return healedInjuriesColors;
    }

    public ColorPreference getPaidRetirement() {
        return paidRetirementColors;
    }
}

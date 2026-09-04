/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.gui.commandGeneration.contents.SetupTab;
import mekhq.gui.commandGeneration.contents.SparesAndFinancesTab;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Every option a player can set in the Command Generator explains itself on hover (MekHQ issue 9938).
 *
 * <p>The tabs are built the way the dialog builds them and every control walked: check boxes, radio buttons,
 * drop-downs, spinners, text fields, sliders and buttons. Each must carry a tooltip that is not blank and is
 * not the {@code !key!} placeholder the bundle lookup returns for a missing key. The Force Generator tab's
 * controls come from MegaMek's own panel, which sets its tooltips from MegaMek's bundle, so it is covered
 * there.</p>
 */
class CommandGenerationTooltipsTest {

    private static Campaign campaign;

    @BeforeAll
    static void loadWhatTheTabsRead() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        Factions.setInstance(Factions.loadDefault(true));
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void everyControlOnThePersonnelAndOfficersTabHasATooltip() {
        JPanel tab = new SetupTab(campaign, new CommandGenerationOptions()).createTab();

        assertEveryControlExplainsItself(tab, "Personnel & Officers");
    }

    @Test
    void everyControlOnTheSparesAndFinancesTabHasATooltip() {
        JPanel tab = new SparesAndFinancesTab(campaign, new CommandGenerationOptions(), () -> null).createTab();

        assertEveryControlExplainsItself(tab, "Spares & Finances");
    }

    private static void assertEveryControlExplainsItself(Container tab, String tabName) {
        List<JComponent> controls = new ArrayList<>();
        collectControls(tab, controls);
        assertFalse(controls.isEmpty(), tabName + " built no controls; the walk found nothing to check");

        Set<String> missing = new TreeSet<>();
        for (JComponent control : controls) {
            String tooltip = control.getToolTipText();
            boolean isBlank = (tooltip == null) || tooltip.isBlank();
            boolean isPlaceholder = (tooltip != null) && tooltip.startsWith("!");
            if (isBlank || isPlaceholder) {
                missing.add(describe(control));
            }
        }
        assertTrue(missing.isEmpty(), tabName + ": " + missing.size() + " control(s) have no tooltip: " + missing);
    }

    /**
     * Gathers the controls a player operates. A spinner's own text field and a drop-down's arrow button are
     * parts of their control and are not listed on their own; the spinner or drop-down carries the tooltip.
     */
    private static void collectControls(Container container, List<JComponent> into) {
        for (Component component : container.getComponents()) {
            if (component instanceof JSpinner spinner) {
                into.add(spinner);
                continue;
            }
            if (component instanceof JComboBox<?> dropDown) {
                into.add(dropDown);
                continue;
            }
            boolean isControl = (component instanceof AbstractButton)
                  || (component instanceof JTextField)
                  || (component instanceof JSlider);
            if (isControl) {
                into.add((JComponent) component);
            }
            if (component instanceof Container child) {
                collectControls(child, into);
            }
        }
    }

    private static String describe(JComponent control) {
        String name = control.getName();
        if ((name != null) && !name.isBlank()) {
            return name;
        }
        if (control instanceof AbstractButton button) {
            return button.getClass().getSimpleName() + " '" + button.getText() + "'";
        }
        if (control instanceof JLabel label) {
            return "label '" + label.getText() + "'";
        }
        return control.getClass().getSimpleName() + " (unnamed)";
    }
}

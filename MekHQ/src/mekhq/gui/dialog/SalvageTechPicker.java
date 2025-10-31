/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class SalvageTechPicker extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageTechPicker";
    private static final int NUM_COLUMNS = 3;

    public final int SELECTION_CANCELLED = 0;
    public final int SELECTION_CONFIRMED = 1;

    private static Map<JCheckBox, Person> checkboxPersonMap;

    /**
     * Checks whether the user confirmed their tech selection.
     *
     * @return {@code true} if the user confirmed their selection, {@code false} if they canceled
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean wasConfirmed() {
        return getDialogChoice() == SELECTION_CONFIRMED;
    }

    /**
     * Retrieves the list of techs that were selected by the user.
     *
     * <p>This method examines all checkboxes in the dialog and returns a list of techs corresponding
     * to the checked checkboxes. If no checkboxes are selected or the dialog was canceled, an empty list is
     * returned.</p>
     *
     * @return a list of selected {@link Person} objects, or an empty list if none were selected
     *
     * @author Illiani
     * @since 0.50.10
     */
    public List<Person> getSelectedTechs() {
        List<Person> selectedTechs = new ArrayList<>();

        if (checkboxPersonMap != null) {
            for (Map.Entry<JCheckBox, Person> entry : checkboxPersonMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedTechs.add(entry.getValue());
                }
            }
        }

        return selectedTechs;
    }

    /**
     * Creates a new salvage tech picker dialog.
     *
     * @param campaign the current campaign
     * @param techs    the list of available techs that can perform salvage operations
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvageTechPicker(Campaign campaign, List<Person> techs) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getInCharacterMessage(campaign.getCommanderAddress(), !techs.isEmpty()),
              getButtons(!techs.isEmpty()),
              getOutOfCharacterMessage(),
              ImmersiveDialogWidth.LARGE.getWidth(),
              false,
              getSupplementalPanel(campaign, techs),
              null,
              true);
    }

    /**
     * Generates the in-character message displayed in the dialog.
     *
     * <p>The message varies depending on whether techs are available for deployment.</p>
     *
     * @param commanderAddress the formal address/title of the campaign commander
     * @param hasTechs         {@code true} if techs are available, {@code false} otherwise
     *
     * @return the formatted in-character message string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getInCharacterMessage(String commanderAddress, boolean hasTechs) {
        String key = "SalvageTechPicker.inCharacterMessage." + (hasTechs ? "normal" : "noTechs");
        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress);
    }


    /**
     * Generates the out-of-character message displayed in the dialog.
     *
     * @return the out-of-character message string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getOutOfCharacterMessage() {
        String key = "SalvageTechPicker.outOfCharacterMessage.salvage";
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * Creates the list of buttons to display in the dialog.
     *
     * <p>Always includes a Cancel button. If techs are available, also includes a Confirm button.</p>
     *
     * @param hasTechs {@code true} if techs are available for selection, {@code false} otherwise
     *
     * @return a list of button configurations for the dialog
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<ButtonLabelTooltipPair> getButtons(boolean hasTechs) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ButtonLabelTooltipPair(getText("Cancel.text"), null));

        if (hasTechs) {
            buttons.add(new ButtonLabelTooltipPair(getText("Confirm.text"), null));
        }

        return buttons;
    }

    /**
     * Creates the supplemental panel containing tech selection checkboxes.
     *
     * <p>This panel is displayed below the main dialog message and contains checkboxes arranged in three
     * columns. Each checkbox represents a tech that can be selected for salvage operations. The checkboxes are labeled
     * with the tech's name.</p>
     *
     * @param campaign the current campaign context
     * @param techs    the list of techs to display as checkboxes
     *
     * @return a {@link JPanel} containing the tech selection UI with checkboxes arranged in three columns
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static @Nullable JPanel getSupplementalPanel(Campaign campaign, List<Person> techs) {
        if (techs.isEmpty()) {
            return null;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTechs = new JLabel(getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.combo.label"));
        panel.add(lblTechs, constraints);

        // Create panel with three columns
        JPanel checkboxPanel = new JPanel(new GridLayout(1, NUM_COLUMNS, 10, 0));
        checkboxPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        JPanel middleColumn = new JPanel();
        middleColumn.setLayout(new BoxLayout(middleColumn, BoxLayout.Y_AXIS));
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));

        checkboxPersonMap = new LinkedHashMap<>();

        // Create checkboxes for each tech
        for (int i = 0; i < techs.size(); i++) {
            Person tech = techs.get(i);
            String techName = tech.getFullTitle();
            String skillLevel = tech.getSkillLevel(campaign, tech.getPrimaryRole().isTech()).toString();
            int minutesLeft = tech.getMinutesLeft();
            JCheckBox checkbox = new JCheckBox(techName + " (" + skillLevel + ", " + minutesLeft + "m)");
            checkbox.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            checkboxPersonMap.put(checkbox, tech);

            // Distribute checkboxes across three columns
            if (i % NUM_COLUMNS == 0) {
                leftColumn.add(checkbox);
            } else if (i % NUM_COLUMNS == 1) {
                middleColumn.add(checkbox);
            } else {
                rightColumn.add(checkbox);
            }
        }

        checkboxPanel.add(leftColumn);
        checkboxPanel.add(middleColumn);
        checkboxPanel.add(rightColumn);

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        constraints.gridy = 1;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, constraints);

        return panel;
    }
}

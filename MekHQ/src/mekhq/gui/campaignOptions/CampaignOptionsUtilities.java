/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.List;
import java.util.*;

public class CampaignOptionsUtilities {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);
    final static String IMAGE_DIRECTORY = "data/images/universe/factions/";

    /**
     * @return the image directory
     */
    public static String getImageDirectory() {
        return IMAGE_DIRECTORY;
    }

    /**
     * Creates a {@link GroupLayout} object for the specified {@link JPanel}.
     * <p>
     * Written to be paired with {@code CampaignOptionsStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GroupLayout} is created
     * @return the created {@link GroupLayout} object
     */
    public static GroupLayout createGroupLayout(JPanel panel) {
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        return layout;
    }

    /**
     * Creates a parent panel for the provided {@link JPanel}.
     *
     * @param panel the panel to be added to the parent panel
     * @param name the name of the parent panel
     * @return the created {@link JPanel}
     */
    public static JPanel createParentPanel(JPanel panel, String name) {
        // Create Panel
        final JPanel parentPanel = new CampaignOptionsStandardPanel(name);
        final GroupLayout parentLayout = createGroupLayout(parentPanel);

        // Layout
        parentPanel.setLayout(parentLayout);

        parentLayout.setVerticalGroup(
            parentLayout.createSequentialGroup()
                .addComponent(panel));

        parentLayout.setHorizontalGroup(
            parentLayout.createParallelGroup(Alignment.CENTER)
                .addComponent(panel));

        return parentPanel;
    }

    /**
     * Creates a new instance of {@link JTabbedPane} with the supplied panels as tabs.
     * <p>
     * The resource bundle reference for the individual tabs will be {@code panel.getName() + ".title"}
     *
     * @param panels a map containing the names of the panels as keys and the corresponding
     *              {@link JPanel} objects as values
     * @return a {@link JTabbedPane} with the supplied panels as tabs
     */
    static JTabbedPane createSubTabs(Map<String, JPanel> panels) {
        // We use a list here to ensure that the tabs always display in the same order,
        // and that order might as well be alphabetic.
        List<String> tabNames = new ArrayList<>(panels.keySet());
        tabNames.sort(String.CASE_INSENSITIVE_ORDER);

        // This is a special case handler to ensure 'general options' tabs always appear first
        int indexToMoveToFront = -1;
        for (int i=0; i<tabNames.size(); i++) {
            if (tabNames.get(i).contains("GeneralTab")) {
                indexToMoveToFront = i;
                break;
            }
        }

        if (indexToMoveToFront != -1) {
            String tabName = tabNames.remove(indexToMoveToFront);
            tabNames.add(0, tabName);
        }

        JTabbedPane tabbedPane = new JTabbedPane();

        for (String tabName : tabNames) {
            JPanel mainPanel = panels.get(tabName);

            // Create a panel for the quote
            JPanel quotePanel = new JPanel(new GridBagLayout());
            JLabel quote = new JLabel(String.format(
                "<html><i><div style='width: %s; text-align:center;'>%s</div></i></html>",
                UIUtil.scaleForGUI(mainPanel.getPreferredSize().width),
                resources.getString(tabName + ".border")));

            GridBagConstraints quoteConstraints = new GridBagConstraints();
            quoteConstraints.gridx = GridBagConstraints.RELATIVE;
            quoteConstraints.gridy = GridBagConstraints.RELATIVE;
            quotePanel.add(quote, quoteConstraints);

            // Create a BorderLayout panel for mainPanel
            JPanel mainPanelHolder = new JPanel(new GridBagLayout());
            GridBagConstraints mainConstraints = new GridBagConstraints();
            mainConstraints.gridx = GridBagConstraints.RELATIVE;
            mainConstraints.gridy = GridBagConstraints.RELATIVE;
            mainPanelHolder.add(mainPanel, mainConstraints);

            // Reorganize mainPanel to include quotePanel at bottom
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setName(tabName);
            contentPanel.add(mainPanelHolder, BorderLayout.CENTER);

            contentPanel.add(quotePanel, BorderLayout.SOUTH);

            // Create a wrapper panel for its easy alignment controls
            JPanel wrapperPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            wrapperPanel.add(contentPanel, gbc);

            tabbedPane.addTab(resources.getString(tabName + ".title"), wrapperPanel);
        }

        return tabbedPane;
    }

    /**
     * If a custom wrap size is provided, it is returned.
     * Otherwise, the default wrap size of 100 is returned.
     *
     * @param customWrapSize the maximum number of characters (including whitespaces) on each line
     *                      of the tooltip, or {@code null} if no custom wrap size is specified
     * @return the maximum number of characters on each line of a tooltip
     */
    public static int processWrapSize(@Nullable Integer customWrapSize) {
        return customWrapSize == null ? 100 : customWrapSize;
    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The {@code CampaignOptionsUtilities} class provides utility methods and constants
 * for managing, creating, and organizing user interface components related to the
 * campaign options dialog in the MekHQ application. This class focuses on assisting
 * in the creation and layout of panels, tabs, and other related components.
 *
 * <p>
 * This class is designed to be stateless and does not rely on any specific instance variables,
 * making its methods accessible in a static fashion.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <p>
 *   <li>Provides reusable methods to create and configure {@link JPanel} objects.</li>
 *   <li>Handles creation of organized, alphabetized tab groups with specialized handling for
 *       "general options" tabs.</li>
 *   <li>Offers UI utility methods for processing resource names, image directories,
 *       and dynamic content scaling.</li>
 *   <li>Supports internationalization through the {@link ResourceBundle} for localized strings.</li>
 * </p>
 */
public class CampaignOptionsUtilities {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);
    final static String IMAGE_DIRECTORY = "data/images/universe/factions/";

    /**
     * Retrieves the directory path for storing faction-related image resources.
     *
     * @return a {@link String} representing the path to the image directory.
     */
    public static String getImageDirectory() {
        return IMAGE_DIRECTORY;
    }

    /**
     * Creates a {@link GroupLayout} with default gap settings for the specified panel.
     *
     * <p>
     * The created {@link GroupLayout} automatically enables gaps between components
     * and containers for improved layout consistency and readability.
     * </p>
     *
     * @param panel the {@link JPanel} to which the {@link GroupLayout} will be applied.
     * @return the {@link GroupLayout} instance configured for the given panel.
     */
    public static GroupLayout createGroupLayout(JPanel panel) {
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        return layout;
    }

    /**
     * Creates a parent panel for the specified child panel and configures its layout.
     * The child panel is encapsulated in the parent {@link JPanel}, ensuring consistent spacing
     * and margins.
     *
     * @param panel the child {@link JPanel} that will be added to the parent panel.
     * @param name  the identifier name for the parent panel, used for UI tracking and debugging purposes.
     * @return a fully initialized {@link JPanel} configured as a parent container.
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
     * Dynamically creates a {@link JTabbedPane} from a map of tab names and panels.
     *
     * <p>
     * This method organizes the tabs in alphabetic order except for tabs whose
     * names contain "GeneralTab", which are moved to the front as a prioritized tab.
     * Each panel is wrapped in a custom layout that includes additional components,
     * such as quotes or additional spacing elements, for better visual formatting.
     * </p>
     *
     * <p>
     * Tabs are localized using the {@link ResourceBundle}, which maps tab names to
     * their corresponding displayed titles.
     * </p>
     *
     * @param panels a map where the key is the resource name of the tab, and the value
     *               is the {@link JPanel} displayed as the content of the tab.
     * @return a {@link JTabbedPane} containing the organized and formatted tabs.
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
     * Determines an appropriate wrap size for text rendering, using a default when
     * no value is specified.
     *
     * @param customWrapSize an optional {@link Integer} specifying the desired wrap size.
     *                       If this parameter is {@code null}, a default value of 100 is used.
     * @return the processed wrap size value; defaults to 100 if {@code customWrapSize} is null.
     */
    public static int processWrapSize(@Nullable Integer customWrapSize) {
        return customWrapSize == null ? 100 : customWrapSize;
    }
}

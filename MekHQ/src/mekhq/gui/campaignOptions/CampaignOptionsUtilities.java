/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel.getTipPanelName;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import megamek.Version;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code CampaignOptionsUtilities} class provides utility methods and constants for managing, creating, and
 * organizing user interface components related to the campaign options dialog in the MekHQ application. This class
 * focuses on assisting in the creation and layout of panels, tabs, and other related components.
 *
 * <p>
 * This class is designed to be stateless and does not rely on any specific instance variables, making its methods
 * accessible in a static fashion.
 * </p>
 *
 * <strong>Key Features:</strong>
 * <ul>
 *   <li>Provides reusable methods to create and configure {@link JPanel} objects.</li>
 *   <li>Handles creation of organized, alphabetized tab groups with specialized handling for
 *       "general options" tabs.</li>
 *   <li>Offers UI utility methods for processing resource names, image directories,
 *       and dynamic content scaling.</li>
 *   <li>Supports internationalization through the {@link ResourceBundle} for localized strings.</li>
 * </ul>
 */
public class CampaignOptionsUtilities {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsUtilities.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsDialog";
    final static String IMAGE_DIRECTORY = "data/images/universe/factions/";
    public final static int CAMPAIGN_OPTIONS_PANEL_WIDTH = scaleForGUI(950);

    /**
     * Version marker for campaign options that existed before the metadata system was implemented.
     * Use this for options added in version 0.50.10.
     */
    public static final Version VERSION_BEFORE_METADATA = new Version(0, 50, 10);

    /**
     * Current development version marker for campaign options added in the current version.
     * Use this for options added in version 0.50.12.
     */
    public static final Version VERSION_CURRENT = new Version(0, 50, 12);

    /**
     * Cache for reusing CampaignOptionMetadata instances to avoid creating duplicate objects.
     */
    private static final Map<String, CampaignOptionsMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    /**
     * Factory method to get or create a CampaignOptionMetadata instance with caching.
     * This ensures that identical metadata configurations reuse the same object instance.
     *
     * @param version the version when this option was added, or null for no version badge
     * @param flags   optional flags for this option (Custom, Important, Documented, Recommended)
     *
     * @return a CampaignOptionMetadata instance, either from cache or newly created
     */
    public static CampaignOptionsMetadata getMetadata(@Nullable Version version, CampaignOptionFlag... flags) {
        String key = buildMetadataKey(version, flags);
        return METADATA_CACHE.computeIfAbsent(key, k -> new CampaignOptionsMetadata(version, Set.of(flags)));
    }

    /**
     * Builds a unique cache key for a metadata configuration.
     *
     * @param version the version, or null
     * @param flags   the flags array
     *
     * @return a unique string key representing this configuration
     */
    private static String buildMetadataKey(@Nullable Version version, CampaignOptionFlag... flags) {
        String versionKey = (version == null) ? "null" : version.toString();
        String flagsKey = (flags == null || flags.length == 0) ? "none"
              : Arrays.stream(flags)
                    .sorted()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
        return versionKey + ":" + flagsKey;
    }

    public static String getCampaignOptionsResourceBundle() {
        return RESOURCE_BUNDLE;
    }

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
     * The created {@link GroupLayout} automatically enables gaps between components and containers for improved layout
     * consistency and readability.
     * </p>
     *
     * @param panel the {@link JPanel} to which the {@link GroupLayout} will be applied.
     *
     * @return the {@link GroupLayout} instance configured for the given panel.
     */
    public static GroupLayout createGroupLayout(JPanel panel) {
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        return layout;
    }

    /**
     * Creates a parent panel for the specified child panel and configures its layout. The child panel is encapsulated
     * in the parent {@link JPanel}, ensuring consistent spacing and margins.
     *
     * @param panel the child {@link JPanel} that will be added to the parent panel.
     * @param name  the identifier name for the parent panel, used for UI tracking and debugging purposes.
     *
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
     * This method organizes the tabs in alphabetic order except for tabs whose names contain "GeneralTab", which are
     * moved to the front as a prioritized tab. Each panel is wrapped in a custom layout that includes additional
     * components, such as quotes or additional spacing elements, for better visual formatting.
     * </p>
     *
     * <p>
     * Tabs are localized using the {@link ResourceBundle}, which maps tab names to their corresponding displayed
     * titles.
     * </p>
     *
     * @param panels a map where the key is the resource name of the tab, and the value is the {@link JPanel} displayed
     *               as the content of the tab.
     *
     * @return a {@link JTabbedPane} containing the organized and formatted tabs.
     */
    static JTabbedPane createSubTabs(Map<String, JPanel> panels) {
        // We use a list here to ensure that the tabs always display in the same order,
        // and that order might as well be alphabetic.
        List<String> tabNames = new ArrayList<>(panels.keySet());
        tabNames.sort(String.CASE_INSENSITIVE_ORDER);

        // This is a special case handler to ensure 'general options' tabs always appear first
        int indexToMoveToFront = -1;
        for (int i = 0; i < tabNames.size(); i++) {
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
                  "<html><div style='width: %s; text-align:center;'>%s</div></html>",
                  UIUtil.scaleForGUI(mainPanel.getPreferredSize().width),
                  getTextAt(RESOURCE_BUNDLE, tabName + ".border")));

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

            tabbedPane.addTab(getTextAt(RESOURCE_BUNDLE, tabName + ".title"), wrapperPanel);
        }

        return tabbedPane;
    }

    /**
     * Determines an appropriate wrap size for text rendering, using a default when no value is specified.
     *
     * @param customWrapSize an optional {@link Integer} specifying the desired wrap size. If this parameter is
     *                       {@code null}, a default value of 100 is used.
     *
     * @return the processed wrap size value; defaults to 100 if {@code customWrapSize} is null.
     */
    public static int processWrapSize(@Nullable Integer customWrapSize) {
        return customWrapSize == null ? 100 : customWrapSize;
    }

    /**
     * Creates a {@link MouseAdapter} that updates the text of a {@link JLabel} within the specified panel to display a
     * tip string when the mouse enters a related component.
     *
     * <p>
     * When the mouse enters a component with the specified name, this adapter retrieves a localized tip string
     * associated with that component. If the tip contains fewer than five HTML line break tags ({@code <br>}), extra
     * line breaks are appended to ensure a minimum number of lines. The formatted tip is then set as the text of a
     * {@link JLabel} within the provided panel, specifically targeting labels whose name matches the required pattern.
     * </p>
     *
     * @param associatedHeaderPanel   the {@link JPanel} containing the label to update
     * @param sourceComponentBaseName the name of the component whose tip string will be shown in the label
     *
     * @return a {@link MouseAdapter} instance that updates the label with formatted tip text on mouse enter
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static MouseAdapter createTipPanelUpdater(CampaignOptionsHeaderPanel associatedHeaderPanel,
          @Nullable String sourceComponentBaseName) {
        return createTipPanelUpdater(associatedHeaderPanel, sourceComponentBaseName, null);
    }

    /**
     * Creates a {@link MouseAdapter} that updates the text of a {@link JLabel} within the specified panel to display a
     * tip string when the mouse enters a related component.
     *
     * <p>
     * When the mouse enters a component with the specified name, this adapter retrieves a localized tip string
     * associated with that component. If the tip contains fewer than five HTML line break tags ({@code <br>}), extra
     * line breaks are appended to ensure a minimum number of lines. The formatted tip is then set as the text of a
     * {@link JLabel} within the provided panel, specifically targeting labels whose name matches the required pattern.
     * </p>
     *
     * @param associatedHeaderPanel   the {@link JPanel} containing the label to update
     * @param sourceComponentBaseName the name of the component whose tip string will be shown in the label
     * @param replacementText         the specific text to use, or {@code null} if the text should be dynamically
     *                                fetched from the source component.
     *
     * @return a {@link MouseAdapter} instance that updates the label with formatted tip text on mouse enter
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static MouseAdapter createTipPanelUpdater(CampaignOptionsHeaderPanel associatedHeaderPanel,
          @Nullable String sourceComponentBaseName, @Nullable String replacementText) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                String tipText = replacementText;
                if (replacementText == null) {
                    tipText = getTextAt(RESOURCE_BUNDLE, "lbl" + sourceComponentBaseName + ".tooltip");
                }

                if (tipText.isBlank()) {
                    return;
                }

                // This might seem really weird, and it is, but the wordWrap method uses '<br>' to create its new
                // lines. This allows us to more easily account for line width when counting instances of '<br>' in
                // the section below.
                tipText = wordWrap(tipText, 120);

                // We have to remove the opening tag so that the extra '<br>' we're adding can be factored into the
                // display
                tipText = tipText.replace("<html>", "");

                // These extra linebreaks are to ensure we have a relatively consistent number of lines. This stops the
                // options from 'bouncing' around too much as the tip resizes.
                int panelLineCount = associatedHeaderPanel.getTipPanelHeight();
                int missingLines = panelLineCount - tipLineCounter(tipText);
                if (missingLines > 0) {
                    String lineBreaks = "";
                    for (int missingLine = 0; missingLine < missingLines; missingLine++) {
                        lineBreaks += "<br>";
                    }

                    tipText = lineBreaks + tipText;
                } else if (missingLines < 0) {
                    LOGGER.warn("Tip panel for {} exceeds the maximum number of lines ({}). Line count should be " +
                                      "increased by {}",
                          associatedHeaderPanel.getName(),
                          panelLineCount,
                          Math.abs(missingLines));
                }

                // That out of the way, let's add the opening tag back in
                tipText = "<html>" + tipText;

                for (Component component : associatedHeaderPanel.getComponents()) {
                    if (component instanceof JLabel label) {
                        String labelName = label.getName();
                        if (labelName != null && labelName.contains(getTipPanelName())) {
                            label.setText(tipText);
                        }
                    }
                }
            }
        };
    }

    /**
     * Counts the number of occurrences of the HTML line break tag {@code "<br>"} in the given tip string.
     *
     * <p>
     * This method scans the provided string and returns the number of times the substring {@code "<br>"} appears. It is
     * useful for determining how many HTML line breaks are present in formatted tip text.
     * </p>
     *
     * @param tip the string to scan for {@code "<br>"} occurrences
     *
     * @return the number of {@code "<br>"} tags found in the string
     *
     * @author Illiani
     * @since 0.50.06
     */
    private static int tipLineCounter(String tip) {
        Pattern pattern = Pattern.compile("<br>");
        Matcher matcher = pattern.matcher(tip);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        return count;
    }

    // region Badge Formatting

    /**
     * Formats version and flag badges for campaign options based on metadata.
     *
     * <p>
     * The badges include:
     * <ul>
     *   <li>Special flag symbols (if any) - displayed first, uncolored</li>
     *   <li>Added since badge - colored star indicating when the option was added</li>
     * </ul>
     * </p>
     *
     * <p>
     * Development releases use a filled purple star (★), while milestone releases use a hollow green star (☆).
     * </p>
     *
     * @param metadata the campaign option metadata, or null if no badges should be shown
     *
     * @return an HTML-formatted string with all badges, or empty string if metadata is null
     */
    public static String formatBadges(@Nullable CampaignOptionsMetadata metadata) {
        if (metadata == null) {
            return "";
        }

        StringBuilder badges = new StringBuilder();

        // Add flag symbols first
        if (metadata.flags() != null && !metadata.flags().isEmpty()) {
            for (CampaignOptionFlag flag : metadata.flags()) {
                badges.append(" ").append(flag.getSymbol());
            }
        }

        // Add "added since" badge if specified
        badges.append(metadata.getAddedSinceBadgeHtml());

        return badges.toString();
    }

    // endregion Badge Formatting
}

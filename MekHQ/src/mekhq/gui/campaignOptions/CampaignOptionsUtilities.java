/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.Version;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.gui.baseComponents.MHQCollapsiblePanel;
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
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsDialog";
    final static String IMAGE_DIRECTORY = "data/images/universe/factions/";
    public final static int CAMPAIGN_OPTIONS_PANEL_WIDTH = scaleForGUI(950);
    private static final int QUOTE_TOP_PADDING = scaleForGUI(12);
    private static Consumer<String> tipTextConsumer;


    /**
     * Version marker for campaign options that existed before the metadata system was implemented and shouldn't have a
     * version badge.
     */
    public static final Version LEGACY_RULE_BEFORE_METADATA = null;

    /**
     * Version marker for campaign options that existed before the metadata system was implemented, but still since the
     * most recent milestone. This variable should be deprecated once the next milestone is declared.
     */
    public static final Version MILESTONE_BEFORE_METADATA = new Version(0, 50, 10);

    /**
     * Cache for reusing CampaignOptionMetadata instances to avoid creating duplicate objects.
     */
    private static final Map<String, CampaignOptionsMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    /**
     * Factory method to get or create a CampaignOptionMetadata instance with caching. This ensures that identical
     * metadata configurations reuse the same object instance.
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

    static void setTipTextConsumer(@Nullable Consumer<String> tipTextConsumer) {
        CampaignOptionsUtilities.tipTextConsumer = tipTextConsumer;
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
        return new CampaignOptionsPageWrapper(panel, name);
    }

    static Component createContentWithQuote(Component content, @Nullable String quoteResourceName) {
        if (quoteResourceName == null || !ResourceBundle.getBundle(RESOURCE_BUNDLE)
                                                 .containsKey(quoteResourceName + ".border")) {
            return content;
        }

        int quoteWidth = Math.max(1, Math.min(getQuoteReferenceWidth(content), CAMPAIGN_OPTIONS_PANEL_WIDTH));

        JPanel quotePanel = new JPanel(new GridBagLayout());
        quotePanel.setBorder(BorderFactory.createEmptyBorder(QUOTE_TOP_PADDING, 0, 0, 0));
        JLabel quote = new JLabel(String.format(
              "<html><div style='width: %spx; text-align:center;'>%s</div></html>",
              quoteWidth,
              getTextAt(RESOURCE_BUNDLE, quoteResourceName + ".border")));

        GridBagConstraints quoteConstraints = new GridBagConstraints();
        quoteConstraints.gridx = GridBagConstraints.RELATIVE;
        quoteConstraints.gridy = GridBagConstraints.RELATIVE;
        quotePanel.add(quote, quoteConstraints);

        JPanel quotedContent = new JPanel(new BorderLayout());
        quotedContent.setName("pnl" + quoteResourceName + "QuotedContent");
        quotedContent.add(content, BorderLayout.CENTER);
        quotedContent.add(quotePanel, BorderLayout.SOUTH);
        return quotedContent;
    }

    /**
     * Determines the width that the closing quote should be sized to.
     *
     * <p>
     * The page header reserves a fixed (wide) body text width, so sizing the quote to the whole content's preferred
     * width makes the quote noticeably wider than the option sections beneath it. To keep the quote aligned with the
     * visible sections, this method returns the width of the widest {@link MHQCollapsiblePanel} section found within the
     * content tree, falling back to the content's preferred width when no section is present.
     * </p>
     *
     * @param content the content whose section widths should be inspected
     *
     * @return the reference width to use for the quote
     */
    private static int getQuoteReferenceWidth(Component content) {
        int widestSection = findWidestSection(content);
        return widestSection > 0 ? widestSection : content.getPreferredSize().width;
    }

    private static int findWidestSection(Component component) {
        int widest = 0;
        if (component instanceof MHQCollapsiblePanel) {
            widest = component.getPreferredSize().width;
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                widest = Math.max(widest, findWidestSection(child));
            }
        }

        return widest;
    }

    private static class CampaignOptionsPageWrapper extends JPanel {
        private final Component content;

        private CampaignOptionsPageWrapper(Component content, String name) {
            super(null);
            this.content = content;
            setName("pnl" + name);
            add(content);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = content.getPreferredSize();
            return new Dimension(Math.min(preferredSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH), preferredSize.height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, content.getMinimumSize().height);
        }

        @Override
        public void doLayout() {
            Dimension preferredSize = content.getPreferredSize();
            int contentWidth = Math.min(preferredSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH);
            contentWidth = Math.min(contentWidth, getWidth());
            int x = Math.max(0, (getWidth() - contentWidth) / 2);
            content.setBounds(x, 0, contentWidth, preferredSize.height);
        }
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
    * Creates a {@link MouseAdapter} that updates the shared Campaign Options help surface when the mouse enters a
    * related component.
     *
     * <p>
    * When the mouse enters a component with the specified name, this adapter retrieves a localized tip string
    * associated with that component and sends the formatted text to the sticky help panel owned by the shell.
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
    * Creates a {@link MouseAdapter} that updates the shared Campaign Options help surface when the mouse enters a
    * related component.
     *
     * <p>
    * When the mouse enters a component with the specified name, this adapter retrieves a localized tip string
    * associated with that component and sends the formatted text to the sticky help panel owned by the shell.
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

                tipText = wordWrap(tipText, 120);
                if (!tipText.endsWith("</html>")) {
                    tipText += "</html>";
                }

                if (tipTextConsumer != null) {
                    tipTextConsumer.accept(tipText);
                }
            }
        };
    }

    // region Badge Formatting

    /**
     * Formats version and flag badges for campaign options based on metadata.
     * <p>
     * The badges include:
     * <ul>
     *   <li>Special flag symbols (if any) - displayed first, uncolored</li>
     *   <li>Added since badge - colored star indicating when the option was added</li>
     * </ul>
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

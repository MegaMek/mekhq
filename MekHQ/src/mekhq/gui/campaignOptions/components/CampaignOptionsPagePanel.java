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
package mekhq.gui.campaignOptions.components;

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.CAMPAIGN_OPTIONS_PANEL_WIDTH;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.formatBadges;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.MHQCollapsiblePanel;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;

/**
 * Standard page shell for Campaign Options screens, assembled through its
 * fluent {@link Builder}.
 *
 * <p>
 * A page stacks, from top to bottom: a {@link CampaignOptionsHeaderPanel}
 * header, an optional intro paragraph, a
 * stack of collapsible {@link MHQCollapsiblePanel} sections (with shared
 * expand/collapse-all controls), and an optional
 * quote footer. Arbitrary components can also be interleaved with the sections.
 * Every sectioned page is floored to a
 * shared width ({@link #UNIFORM_SECTION_STACK_WIDTH}) so the dialog's pages
 * render at a consistent width, while pages
 * whose content is naturally wider keep their size up to the page-width cap.
 * </p>
 *
 * <p>
 * Section titles and summaries are concatenated into
 * {@link #getSectionSearchText()} so the navigation filter can
 * match on a section heading, not only the page title.
 * </p>
 */
public class CampaignOptionsPagePanel extends JPanel {
    private static final int INTRO_HORIZONTAL_PADDING = UIUtil.scaleForGUI(24);
    private static final int QUOTE_TOP_PADDING = UIUtil.scaleForGUI(12);
    private static final int QUOTE_BOTTOM_PADDING = UIUtil.scaleForGUI(8);
    private static final int QUOTE_HORIZONTAL_PADDING = UIUtil.scaleForGUI(24);
    private static final int DEFAULT_HEADER_IMAGE_SIZE = 80;
    // Shared minimum width every sectioned page is floored to, so form pages render
    // at a consistent width across the
    // dialog instead of each page shrinking to its own widest section. Comfortably
    // covers a two-column form section
    // (label column plus a long right-column control or checkbox) and stays well
    // under the page width cap, so wider
    // table pages and the 950 cap are unaffected.
    private static final int UNIFORM_SECTION_STACK_WIDTH = UIUtil.scaleForGUI(640);

    private final JPanel pageBody;
    private final boolean showDetailsPanel;
    private final String sectionSearchText;

    private CampaignOptionsPagePanel(Builder builder) {
        super(null);
        setName("pnl" + builder.name + "Page");
        showDetailsPanel = builder.showDetailsPanel;

        pageBody = new JPanel(new BorderLayout());
        pageBody.setName("pnl" + builder.name + "PageBody");
        pageBody.setOpaque(false);

        // Render items preserve the order in which sections and raw components were added to the builder, so callers
        // can place arbitrary components above, between, or below the collapsible sections.
        List<Object> renderItems = new ArrayList<>();
        List<MHQCollapsiblePanel> sections = new ArrayList<>();
        StringBuilder searchTextBuilder = new StringBuilder();
        for (Object bodyItem : builder.bodyItems) {
            if (bodyItem instanceof Section section) {
                MHQCollapsiblePanel sectionPanel = createSection(section, builder.sectionsExpandedByDefault);
                sections.add(sectionPanel);
                renderItems.add(sectionPanel);
                appendSectionSearchText(searchTextBuilder, section);
            } else if (bodyItem instanceof JComponent component) {
                renderItems.add(component);
            }
        }
        sectionSearchText = searchTextBuilder.toString().trim();
        JPanel sectionControls = createSectionControls(sections);
        int sectionStackWidth = getPreferredSectionStackWidth(sections, sectionControls);

        JPanel contentPanel = createContentPanel(builder, renderItems, sections, sectionControls, sectionStackWidth);
        pageBody.add(contentPanel, BorderLayout.CENTER);
        int quoteWidth = sectionStackWidth > 0 ? sectionStackWidth : contentPanel.getPreferredSize().width;
        JComponent quotePanel = createQuotePanel(builder.quoteResourceName, quoteWidth);
        if (quotePanel != null) {
            pageBody.add(quotePanel, BorderLayout.SOUTH);
        }

        add(pageBody);
    }

    public static @Nonnull Builder builder(@Nonnull String name, @Nonnull String headerResourceName,
          @Nonnull String imageAddress) {
        return new Builder(name, headerResourceName, imageAddress);
    }

    public boolean shouldShowDetailsPanel() {
        return showDetailsPanel;
    }

    /**
     * Returns the concatenated, resolved title and summary text of every collapsible section on this page. This is used
     * by the navigation filter so a search can match a section title or summary, not only the page (tab) title.
     *
     * @return the section search text, or an empty string if the page has no sections
     */
    public @Nonnull String getSectionSearchText() {
        return sectionSearchText;
    }

    private static void appendSectionSearchText(StringBuilder builder, Section section) {
        appendResolvedText(builder, section.titleKey(), section.literal());
        appendResolvedText(builder, section.summaryKey(), section.literal());
    }

    private static void appendResolvedText(StringBuilder builder, @Nullable String key, boolean literal) {
        if (key == null) {
            return;
        }
        String text = literal ? key : getTextAt(getCampaignOptionsResourceBundle(), key);
        if (text != null && !text.isBlank()) {
            builder.append(' ').append(text);
        }
    }

    @Override
    public @Nonnull Dimension getPreferredSize() {
        Dimension preferredSize = pageBody.getPreferredSize();
        return new Dimension(Math.min(preferredSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH), preferredSize.height);
    }

    @Override
    public @Nonnull Dimension getMinimumSize() {
        return new Dimension(0, pageBody.getMinimumSize().height);
    }

    @Override
    public void doLayout() {
        Dimension preferredSize = pageBody.getPreferredSize();
        // Give the page body its full content width, even when that exceeds the viewport. Squeezing the inner
        // GridBagLayout below its content width makes the section stack collapse to its zero minimum width and the
        // header text wrap vertically, which wipes the whole page. Letting any overflow clip on the right keeps the
        // header on one line and the sections visible, which is a far better failure mode than a blank page.
        int contentWidth = preferredSize.width;
        int x = Math.max(0, (getWidth() - contentWidth) / 2);
        pageBody.setBounds(x, 0, contentWidth, preferredSize.height);
    }

    private JPanel createContentPanel(Builder builder, List<Object> renderItems, List<MHQCollapsiblePanel> sections,
          JPanel sectionControls, int sectionStackWidth) {
        CampaignOptionsHeaderPanel header = builder.headerPanel != null ? builder.headerPanel
              : new CampaignOptionsHeaderPanel(builder.headerResourceName,
                    builder.imageAddress,
                    builder.includeHeaderBodyText,
                    builder.headerImageSize,
                    builder.tintHeaderImage);

        JPanel panel = new CampaignOptionsStandardPanel(builder.name);
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.fill = GridBagConstraints.NONE;

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        layout.weightx = 0.0;
        layout.anchor = GridBagConstraints.CENTER;
        panel.add(header, layout);

        if (builder.introTextKey != null) {
            layout.gridy++;
            int introWidth = sectionStackWidth > 0 ? sectionStackWidth : header.getPreferredSize().width;
            panel.add(createIntroPanel(builder, introWidth), layout);
        }

        if (!renderItems.isEmpty()) {
            layout.gridy++;
            layout.anchor = GridBagConstraints.NORTHWEST;
            panel.add(createSectionStackPanel(renderItems, sections, sectionControls, sectionStackWidth), layout);
        }

        return panel;
    }

    private JPanel createSectionStackPanel(List<Object> renderItems, List<MHQCollapsiblePanel> sections,
          JPanel sectionControls, int stackWidth) {
        JPanel stackPanel = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension(Math.max(preferredSize.width, stackWidth), preferredSize.height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, super.getMinimumSize().height);
            }
        };
        stackPanel.setOpaque(false);

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = -1;
        layout.weightx = 1.0;
        layout.fill = GridBagConstraints.HORIZONTAL;

        boolean controlsAdded = false;
        for (Object renderItem : renderItems) {
            // The expand/collapse-all controls belong with the sections, so insert them immediately before the first
            // section. Components placed before any section therefore render above the controls (a top action area),
            // while components between sections stay inline.
            if (renderItem instanceof MHQCollapsiblePanel section) {
                if (!controlsAdded && !sections.isEmpty()) {
                    layout.gridy++;
                    layout.anchor = GridBagConstraints.EAST;
                    stackPanel.add(sectionControls, layout);
                    controlsAdded = true;
                }
                layout.gridy++;
                layout.anchor = GridBagConstraints.NORTHWEST;
                stackPanel.add(section, layout);
            } else if (renderItem instanceof JComponent component) {
                layout.gridy++;
                layout.anchor = GridBagConstraints.NORTHWEST;
                stackPanel.add(component, layout);
            }
        }

        return stackPanel;
    }

    private JPanel createIntroPanel(Builder builder, int textWidth) {
        int introHorizontalPadding = getIntroHorizontalPadding(textWidth);
        JPanel introPanel = new CampaignOptionsIntroPanel(builder.name + "Intro",
            getTextAt(getCampaignOptionsResourceBundle(), builder.introTextKey),
            textWidth - (introHorizontalPadding * 2));
        introPanel.setBorder(BorderFactory.createEmptyBorder(0,
            introHorizontalPadding,
            0,
            introHorizontalPadding));
        return introPanel;
    }

    private int getIntroHorizontalPadding(int introWidth) {
        return Math.min(INTRO_HORIZONTAL_PADDING, Math.max(0, (introWidth - 1) / 2));
    }

    private MHQCollapsiblePanel createSection(Section sectionDefinition, boolean expandedByDefault) {
        MHQCollapsiblePanel section = new MHQCollapsiblePanel(getSectionTitle(sectionDefinition),
              sectionDefinition.content);
        section.setSummary(getSectionSummary(sectionDefinition));
        section.setExpanded(expandedByDefault);

        // When the content exposes a header control (e.g. an enable toggle), mount it in the section header and mirror
        // its enabled state onto the title so the section can be read and toggled without expanding it.
        if (sectionDefinition.content instanceof SectionHeaderControlProvider provider) {
            section.setTrailingComponent(provider.getSectionHeaderControl());
            section.setTitleMuted(!provider.isSectionEnabled());
            provider.setSectionStateListener(() -> section.setTitleMuted(!provider.isSectionEnabled()));
        }

        return section;
    }

    private String getSectionTitle(Section sectionDefinition) {
        String title = sectionDefinition.literal ? sectionDefinition.titleKey
              : getTextAt(getCampaignOptionsResourceBundle(), sectionDefinition.titleKey);
        String badges = formatBadges(sectionDefinition.metadata);
        if (badges.isBlank()) {
            return title;
        }
        // <nobr> keeps the title (which becomes HTML once badges are present) on a single line. Without it an HTML
        // label only reserves the width of its longest word, so the title would wrap whenever the section content is
        // narrow, making the header width appear tied to the content width.
        return "<html><nobr>" + title + badges + "</nobr></html>";
    }

    private String getSectionSummary(Section sectionDefinition) {
        if (sectionDefinition.summaryKey == null) {
            return "";
        }
        return sectionDefinition.literal ? sectionDefinition.summaryKey
              : getTextAt(getCampaignOptionsResourceBundle(), sectionDefinition.summaryKey);
    }

    private int getPreferredSectionWidth(List<MHQCollapsiblePanel> sections) {
        int preferredWidth = 0;
        for (MHQCollapsiblePanel section : sections) {
            preferredWidth = Math.max(preferredWidth, section.getPreferredSize().width);
        }
        return preferredWidth;
    }

    private int getPreferredSectionStackWidth(List<MHQCollapsiblePanel> sections, JPanel sectionControls) {
        if (sections.isEmpty()) {
            return 0;
        }

        int contentWidth = Math.max(getPreferredSectionWidth(sections), sectionControls.getPreferredSize().width);
        // Floor every sectioned page to a shared width so form pages render at a
        // consistent width across the dialog
        // instead of each page shrinking to its own widest section. Pages whose content
        // is naturally wider than the
        // floor (e.g. table-based pages) keep their larger width; this only grows
        // narrower pages up to the floor, so it
        // never clips. Tune UNIFORM_SECTION_STACK_WIDTH to adjust the shared width.
        return Math.max(contentWidth, UNIFORM_SECTION_STACK_WIDTH);
    }

    private JPanel createSectionControls(List<MHQCollapsiblePanel> sections) {
        JButton expandAllButton = createSectionActionButton("btnExpandAll.text");
        expandAllButton.addActionListener(event -> setExpanded(true, sections));
        JButton collapseAllButton = createSectionActionButton("btnCollapseAll.text");
        collapseAllButton.addActionListener(event -> setExpanded(false, sections));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);
        controls.add(expandAllButton);
        controls.add(collapseAllButton);

        return controls;
    }

    private JButton createSectionActionButton(String resourceKey) {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), resourceKey));
        button.putClientProperty("JComponent.sizeVariant", "small");
        return button;
    }

    private void setExpanded(boolean expanded, List<MHQCollapsiblePanel> sections) {
        for (MHQCollapsiblePanel section : sections) {
            section.setExpanded(expanded);
        }
    }

    private @Nullable JComponent createQuotePanel(@Nullable String quoteResourceName, int contentWidth) {
        if (quoteResourceName == null || !ResourceBundle.getBundle(getCampaignOptionsResourceBundle())
                                           .containsKey(quoteResourceName + ".border")) {
            return null;
        }

        int quotePanelWidth = Math.max(1, Math.min(contentWidth, CAMPAIGN_OPTIONS_PANEL_WIDTH));
        int quoteHorizontalPadding = getQuoteHorizontalPadding(quotePanelWidth);
        int quoteTextWidth = quotePanelWidth - (quoteHorizontalPadding * 2);
        JPanel quotePanel = new JPanel(new GridBagLayout());
        quotePanel.setName("pnl" + quoteResourceName + "QuotePanel");
        quotePanel.setOpaque(false);
        quotePanel.setBorder(BorderFactory.createEmptyBorder(QUOTE_TOP_PADDING,
              quoteHorizontalPadding,
              QUOTE_BOTTOM_PADDING,
              quoteHorizontalPadding));

        JEditorPane quote = new JEditorPane("text/html",
              formatQuoteText(getTextAt(getCampaignOptionsResourceBundle(), quoteResourceName + ".border")));
        quote.setName("txt" + quoteResourceName + "Quote");
        quote.setEditable(false);
        quote.setFocusable(false);
        quote.setOpaque(false);
        quote.setBorder(BorderFactory.createEmptyBorder());
        quote.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        setFontScaling(quote, false, 1);

        Dimension quoteSize = getWrappedQuoteSize(quote, quoteTextWidth);
        quote.setPreferredSize(quoteSize);
        quote.setMinimumSize(quoteSize);

        GridBagConstraints quoteConstraints = new GridBagConstraints();
        quoteConstraints.gridx = GridBagConstraints.RELATIVE;
        quoteConstraints.gridy = GridBagConstraints.RELATIVE;
        quotePanel.add(quote, quoteConstraints);

        return quotePanel;
    }

    private int getQuoteHorizontalPadding(int quotePanelWidth) {
        return Math.min(QUOTE_HORIZONTAL_PADDING, Math.max(0, (quotePanelWidth - 1) / 2));
    }

    private Dimension getWrappedQuoteSize(JEditorPane quote, int quoteWidth) {
        quote.setSize(quoteWidth, Short.MAX_VALUE);
        Dimension preferredSize = quote.getPreferredSize();
        return new Dimension(quoteWidth, preferredSize.height);
    }

    private String formatQuoteText(String text) {
        return "<html><body style='margin: 0; padding: 0; text-align: center;'>" + text + "</body></html>";
    }

    public static class Builder {
        private final String name;
        private final String headerResourceName;
        private final String imageAddress;
        private final List<Object> bodyItems = new ArrayList<>();
        private CampaignOptionsHeaderPanel headerPanel;
        private String quoteResourceName;
        private String introTextKey;
        private boolean includeHeaderBodyText;
        private int headerImageSize = DEFAULT_HEADER_IMAGE_SIZE;
        private boolean tintHeaderImage = true;
        private boolean sectionsExpandedByDefault;
        private boolean showDetailsPanel = true;

        private Builder(String name, String headerResourceName, String imageAddress) {
            this.name = name;
            this.headerResourceName = headerResourceName;
            this.imageAddress = imageAddress;
        }

        public Builder includeHeaderBodyText() {
            includeHeaderBodyText = true;
            return this;
        }

        public Builder headerImageSize(int headerImageSize) {
            this.headerImageSize = headerImageSize;
            return this;
        }

        public Builder tintHeaderImage(boolean tintHeaderImage) {
            this.tintHeaderImage = tintHeaderImage;
            return this;
        }

        public Builder header(CampaignOptionsHeaderPanel headerPanel) {
            this.headerPanel = headerPanel;
            return this;
        }

        public Builder intro(String introTextKey) {
            this.introTextKey = introTextKey;
            return this;
        }

        public Builder quote(String quoteResourceName) {
            this.quoteResourceName = quoteResourceName;
            return this;
        }

        public Builder showDetailsPanel(boolean showDetailsPanel) {
            this.showDetailsPanel = showDetailsPanel;
            return this;
        }

        public Builder sectionsExpandedByDefault(boolean sectionsExpandedByDefault) {
            this.sectionsExpandedByDefault = sectionsExpandedByDefault;
            return this;
        }

        public Builder section(String titleKey, String summaryKey, JComponent content) {
            return section(titleKey, summaryKey, content, null);
        }

        public Builder section(String titleKey, String summaryKey, JComponent content,
              @Nullable CampaignOptionsMetadata metadata) {
            bodyItems.add(new Section(titleKey, summaryKey, content, metadata, false));
            return this;
        }

        /**
         * Adds a collapsible section whose title and summary are used verbatim rather than being resolved through the
         * campaign options resource bundle. Use this for sections whose titles are only known at runtime (for example,
         * one section per special ability).
         *
         * @param title   the literal section title
         * @param summary the literal summary shown when the section is collapsed, or {@code null} for none
         * @param content the section body
         */
        public Builder literalSection(String title, @Nullable String summary, JComponent content) {
            bodyItems.add(new Section(title, summary, content, null, true));
            return this;
        }

        /**
         * Adds an arbitrary component to the page body. The component is rendered in the order it was added relative to
         * sections, so it can sit above, between, or below them.
         *
         * @param component the component to add
         */
        public Builder component(JComponent component) {
            bodyItems.add(component);
            return this;
        }

        public CampaignOptionsPagePanel build() {
            return new CampaignOptionsPagePanel(this);
        }
    }

    private record Section(String titleKey, String summaryKey, JComponent content,
                           @Nullable CampaignOptionsMetadata metadata, boolean literal) {
    }
}

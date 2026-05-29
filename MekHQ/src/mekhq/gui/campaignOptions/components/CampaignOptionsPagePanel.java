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
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.gui.baseComponents.MHQCollapsiblePanel;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;

/**
 * Standard page shell for Campaign Options screens.
 */
public class CampaignOptionsPagePanel extends JPanel {
    private static final int QUOTE_TOP_PADDING = UIUtil.scaleForGUI(12);

    private final JPanel pageBody;
    private final boolean showDetailsPanel;

    private CampaignOptionsPagePanel(Builder builder) {
        super(null);
        setName("pnl" + builder.name + "Page");
        showDetailsPanel = builder.showDetailsPanel;

        pageBody = new JPanel(new BorderLayout());
        pageBody.setName("pnl" + builder.name + "PageBody");
        pageBody.setOpaque(false);

        JPanel contentPanel = createContentPanel(builder);
        pageBody.add(contentPanel, BorderLayout.CENTER);
        JComponent quotePanel = createQuotePanel(builder.quoteResourceName, contentPanel.getPreferredSize().width);
        if (quotePanel != null) {
            pageBody.add(quotePanel, BorderLayout.SOUTH);
        }

        add(pageBody);
    }

    public static Builder builder(String name, String headerResourceName, String imageAddress) {
        return new Builder(name, headerResourceName, imageAddress);
    }

    public boolean shouldShowDetailsPanel() {
        return showDetailsPanel;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = pageBody.getPreferredSize();
        return new Dimension(Math.min(preferredSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH), preferredSize.height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, pageBody.getMinimumSize().height);
    }

    @Override
    public void doLayout() {
        Dimension preferredSize = pageBody.getPreferredSize();
        int contentWidth = Math.min(preferredSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH);
        contentWidth = Math.min(contentWidth, getWidth());
        int x = Math.max(0, (getWidth() - contentWidth) / 2);
        pageBody.setBounds(x, 0, contentWidth, preferredSize.height);
    }

    private JPanel createContentPanel(Builder builder) {
        CampaignOptionsHeaderPanel header = builder.headerPanel != null ? builder.headerPanel
              : new CampaignOptionsHeaderPanel(builder.headerResourceName,
                    builder.imageAddress,
                    builder.includeHeaderBodyText,
                    false,
                    0);
        List<MHQCollapsiblePanel> sections = createSections(builder.sections, builder.sectionsExpandedByDefault);

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
            panel.add(createIntroPanel(builder, sections), layout);
        }

        if (!sections.isEmpty()) {
            layout.gridy++;
            layout.anchor = GridBagConstraints.NORTHWEST;
            panel.add(createSectionStackPanel(sections), layout);
        }

        return panel;
    }

    private JPanel createSectionStackPanel(List<MHQCollapsiblePanel> sections) {
        JPanel sectionControls = createSectionControls(sections);
        int stackWidth = Math.max(getPreferredSectionWidth(sections), sectionControls.getPreferredSize().width);
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
        layout.gridy = 0;
        layout.weightx = 1.0;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.anchor = GridBagConstraints.EAST;
        stackPanel.add(sectionControls, layout);

        layout.anchor = GridBagConstraints.NORTHWEST;
        for (MHQCollapsiblePanel section : sections) {
            layout.gridy++;
            stackPanel.add(section, layout);
        }

        return stackPanel;
    }

    private JPanel createIntroPanel(Builder builder, List<MHQCollapsiblePanel> sections) {
        return new CampaignOptionsIntroPanel(builder.name + "Intro",
              getTextAt(getCampaignOptionsResourceBundle(), builder.introTextKey),
              getPreferredSectionContentWidth(sections));
    }

    private List<MHQCollapsiblePanel> createSections(List<Section> sectionDefinitions,
          boolean sectionsExpandedByDefault) {
        List<MHQCollapsiblePanel> sections = new ArrayList<>();
        for (Section sectionDefinition : sectionDefinitions) {
            sections.add(createSection(sectionDefinition, sectionsExpandedByDefault));
        }
        return sections;
    }

    private MHQCollapsiblePanel createSection(Section sectionDefinition, boolean expandedByDefault) {
        MHQCollapsiblePanel section = new MHQCollapsiblePanel(getSectionTitle(sectionDefinition),
              sectionDefinition.content);
        section.setSummary(getTextAt(getCampaignOptionsResourceBundle(), sectionDefinition.summaryKey));
        section.setExpanded(expandedByDefault);
        return section;
    }

    private String getSectionTitle(Section sectionDefinition) {
        String title = getTextAt(getCampaignOptionsResourceBundle(), sectionDefinition.titleKey);
        String badges = formatBadges(sectionDefinition.metadata);
        if (badges.isBlank()) {
            return title;
        }
        return "<html>" + title + badges + "</html>";
    }

    private int getPreferredSectionContentWidth(List<MHQCollapsiblePanel> sections) {
        int preferredWidth = 0;
        for (MHQCollapsiblePanel section : sections) {
            preferredWidth = Math.max(preferredWidth, section.getContentPreferredWidth());
        }
        return preferredWidth;
    }

    private int getPreferredSectionWidth(List<MHQCollapsiblePanel> sections) {
        int preferredWidth = 0;
        for (MHQCollapsiblePanel section : sections) {
            preferredWidth = Math.max(preferredWidth, section.getPreferredSize().width);
        }
        return preferredWidth;
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

        int quoteWidth = Math.max(1, Math.min(contentWidth, CAMPAIGN_OPTIONS_PANEL_WIDTH));
        JPanel quotePanel = new JPanel(new GridBagLayout());
        quotePanel.setName("pnl" + quoteResourceName + "QuotePanel");
        quotePanel.setOpaque(false);
        quotePanel.setBorder(BorderFactory.createEmptyBorder(QUOTE_TOP_PADDING, 0, 0, 0));

        JLabel quote = new JLabel(String.format("<html><div style='width: %spx; text-align:center;'>%s</div></html>",
              quoteWidth,
              getTextAt(getCampaignOptionsResourceBundle(), quoteResourceName + ".border")));
        quote.setName("lbl" + quoteResourceName + "Quote");

        GridBagConstraints quoteConstraints = new GridBagConstraints();
        quoteConstraints.gridx = GridBagConstraints.RELATIVE;
        quoteConstraints.gridy = GridBagConstraints.RELATIVE;
        quotePanel.add(quote, quoteConstraints);

        return quotePanel;
    }

    public static class Builder {
        private final String name;
        private final String headerResourceName;
        private final String imageAddress;
        private final List<Section> sections = new ArrayList<>();
        private CampaignOptionsHeaderPanel headerPanel;
        private String quoteResourceName;
        private String introTextKey;
        private boolean includeHeaderBodyText;
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
            sections.add(new Section(titleKey, summaryKey, content, metadata));
            return this;
        }

        public CampaignOptionsPagePanel build() {
            return new CampaignOptionsPagePanel(this);
        }
    }

    private record Section(String titleKey, String summaryKey, JComponent content,
                           @Nullable CampaignOptionsMetadata metadata) {
    }
}
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

import static java.awt.Color.BLACK;
import static megamek.utilities.ImageUtilities.addTintToImageIcon;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.settingsBadges;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsTextProvider;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;

/**
 * MekHQ compatibility adapter for the shared {@link SettingsPagePanel}. It retains the existing Campaign Options
 * builder conventions while delegating page layout, collapsible sections, details policy, and section search to
 * MegaMek.
 */
public class CampaignOptionsPagePanel extends JPanel {
    private static final int DEFAULT_HEADER_IMAGE_SIZE = 80;
    private static final Map<String, Icon> HEADER_IMAGE_CACHE = new HashMap<>();

    private final SettingsPagePanel delegate;

    private CampaignOptionsPagePanel(Builder builder) {
        super(new BorderLayout());
        delegate = builder.buildDelegate();
        setName(delegate.getName());
        setOpaque(false);
        add(delegate, BorderLayout.CENTER);
    }

    public static @Nonnull Builder builder(@Nonnull String name, @Nonnull String headerResourceName,
          @Nonnull String imageAddress) {
        return new Builder(name, headerResourceName, imageAddress);
    }

    public boolean shouldShowDetailsPanel() {
        return delegate.shouldShowDetailsPanel();
    }

    public @Nonnull String getSectionSearchText() {
        return delegate.getSectionSearchText();
    }

    public boolean expandSectionsMatching(@Nonnull Predicate<String> sectionTextMatcher) {
        return delegate.expandSectionsMatching(sectionTextMatcher);
    }

    public void expandAllSections() {
        delegate.expandAllSections();
    }

    @Override
    public @Nonnull Dimension getPreferredSize() {
        return delegate.getPreferredSize();
    }

    @Override
    public @Nonnull Dimension getMinimumSize() {
        return delegate.getMinimumSize();
    }

    /** Source-compatible adapter for existing Campaign Options page declarations. */
    public static class Builder {
        private final String name;
        private final String headerResourceName;
        private final String imageAddress;
        private String resourceBundleName = getCampaignOptionsResourceBundle();
        private CampaignOptionsHeaderPanel headerPanel;
        private boolean includeHeaderBodyText;
        private int headerImageSize = DEFAULT_HEADER_IMAGE_SIZE;
        private boolean tintHeaderImage = true;
        private String introResourceName;
        private JComponent introComponent;
        private String quoteResourceName;
        private boolean sectionsExpandedByDefault;
        private boolean showDetailsPanel = true;
        private boolean standardContentWidth;
        private final java.util.List<BodyItem> bodyItems = new java.util.ArrayList<>();

        private Builder(String name, String headerResourceName, String imageAddress) {
            this.name = name;
            this.headerResourceName = headerResourceName;
            this.imageAddress = imageAddress;
        }

        public Builder includeHeaderBodyText() {
            includeHeaderBodyText = true;
            return this;
        }

        public Builder headerImageSize(int imageSize) {
            headerImageSize = imageSize;
            return this;
        }

        public Builder tintHeaderImage(boolean tint) {
            tintHeaderImage = tint;
            return this;
        }

        public Builder header(CampaignOptionsHeaderPanel header) {
            headerPanel = header;
            return this;
        }

        public Builder resourceBundle(String bundleName) {
            resourceBundleName = bundleName;
            return this;
        }

        public Builder intro(String resourceName) {
            introResourceName = resourceName;
            introComponent = null;
            return this;
        }

        public Builder introComponent(JComponent component) {
            introComponent = component;
            introResourceName = null;
            return this;
        }

        public Builder quote(String resourceName) {
            quoteResourceName = resourceName;
            return this;
        }

        public Builder showDetailsPanel(boolean show) {
            showDetailsPanel = show;
            return this;
        }

        public Builder standardContentWidth() {
            standardContentWidth = true;
            return this;
        }

        public Builder sectionsExpandedByDefault(boolean expanded) {
            sectionsExpandedByDefault = expanded;
            return this;
        }

        public Builder section(String titleKey, @Nullable String summaryKey, JComponent content) {
            return section(titleKey, summaryKey, content, null);
        }

        public Builder section(String titleKey, @Nullable String summaryKey, JComponent content,
              @Nullable CampaignOptionsMetadata metadata) {
            bodyItems.add(new SectionItem(titleKey, summaryKey, content, metadata, false));
            return this;
        }

        public Builder literalSection(String title, @Nullable String summary, JComponent content) {
            bodyItems.add(new SectionItem(title, summary, content, null, true));
            return this;
        }

        public Builder component(JComponent component) {
            bodyItems.add(new ComponentItem(component));
            return this;
        }

        public CampaignOptionsPagePanel build() {
            return new CampaignOptionsPagePanel(this);
        }

        private SettingsPagePanel buildDelegate() {
            SettingsTextProvider textProvider = CampaignOptionsComponentSupport.textProvider(resourceBundleName);
            SettingsPagePanel.Builder builder = SettingsPagePanel.builder(name, textProvider,
                  "lbl" + headerResourceName + ".text", headerPanel == null ? headerIcon() : null)
                  .showDetailsPanel(showDetailsPanel)
                  .sectionsExpandedByDefault(sectionsExpandedByDefault);
            if (headerPanel != null) {
                builder.header(headerPanel);
            }
            if (includeHeaderBodyText) {
                builder.headerBody("lbl" + headerResourceName + "Body.text");
            }
            if (introComponent != null) {
                builder.introComponent(introComponent);
            } else if (introResourceName != null) {
                builder.intro(introResourceName + ".intro");
            }
            if (quoteResourceName != null) {
                builder.quote(quoteResourceName + ".border");
            }
            if (standardContentWidth) {
                builder.standardContentWidth();
            }
            for (BodyItem bodyItem : bodyItems) {
                bodyItem.apply(builder);
            }
            return builder.build();
        }

        private Icon headerIcon() {
            String cacheKey = imageAddress + '|' + headerImageSize + '|' + tintHeaderImage;
            return HEADER_IMAGE_CACHE.computeIfAbsent(cacheKey, ignored -> {
                ImageIcon icon = scaleImageIcon(new ImageIcon(imageAddress), headerImageSize, true);
                return tintHeaderImage ? addTintToImageIcon(icon.getImage(), BLACK) : icon;
            });
        }
    }

    private sealed interface BodyItem permits SectionItem, ComponentItem {
        void apply(SettingsPagePanel.Builder builder);
    }

    private record SectionItem(String title, @Nullable String summary, JComponent content,
                               @Nullable CampaignOptionsMetadata metadata, boolean literal) implements BodyItem {
        @Override
        public void apply(SettingsPagePanel.Builder builder) {
            if (literal) {
                builder.literalSection(title, summary, content);
            } else {
                builder.section(title, summary, content, settingsBadges(metadata));
            }
        }
    }

    private record ComponentItem(JComponent component) implements BodyItem {
        @Override
        public void apply(SettingsPagePanel.Builder builder) {
            builder.component(component);
        }
    }

}

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
 * of The Topps Company Inc. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import megamek.common.annotations.Nullable;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * Owns the central Campaign Options content area and sticky contextual help surface.
 */
class CampaignOptionsContentHost extends JPanel {
    private static final int SCROLL_SPEED = 16;

    private final JPanel contentPanel;
    private final JScrollPane contentScrollPane;
    private final CampaignOptionsHelpPanel helpPanel;

    CampaignOptionsContentHost(Component content) {
        this(content, null, true);
    }

    CampaignOptionsContentHost(Component content, @Nullable String quoteResourceName, boolean showHelpPanel) {
        super(new BorderLayout());
        setName("campaignOptionsContentHost");

        helpPanel = new CampaignOptionsHelpPanel();
        CampaignOptionsUtilities.setTipTextConsumer(helpPanel::setHelpText);

        contentPanel = new CampaignOptionsContentPanel();
        contentPanel.setName("campaignOptionsContentPanel");

        contentScrollPane = new JScrollPane(contentPanel,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentScrollPane.setName("campaignOptionsContentScrollPane");
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);

        add(contentScrollPane, BorderLayout.CENTER);
        add(helpPanel, BorderLayout.SOUTH);
        setContent(content, quoteResourceName, showHelpPanel);
    }

    void setContent(Component content) {
        setContent(content, null);
    }

    void setContent(Component content, @Nullable String quoteResourceName) {
        setContent(content, quoteResourceName, true);
    }

    void setContent(Component content, @Nullable String quoteResourceName, boolean showHelpPanel) {
        helpPanel.clearHelpText();
        helpPanel.setVisible(shouldShowHelpPanel(content, showHelpPanel));
        contentPanel.removeAll();
        contentPanel.add(getDisplayContent(content, quoteResourceName), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        revalidate();
        repaint();
    }

    private boolean shouldShowHelpPanel(Component content, boolean defaultShowHelpPanel) {
        CampaignOptionsPagePanel pagePanel = findPagePanel(content);
        if (pagePanel != null) {
            return pagePanel.shouldShowDetailsPanel();
        }
        return defaultShowHelpPanel;
    }

    /**
     * Finds the {@link CampaignOptionsPagePanel} for the supplied content, whether it is the content itself or nested
     * inside a wrapper panel.
     *
     * <p>Most tabs hand their page panel in directly. The recursive lookup exists for {@code AbilitiesTab}, which
     * wraps its page in a {@link java.awt.BorderLayout} container so it can rebuild the page in place when the special
     * ability list changes (the tabbed pane keeps a stable container reference while the inner page is swapped).</p>
     *
     * @param content the content being displayed
     *
     * @return the page panel, or {@code null} if none is present
     */
    private static @Nullable CampaignOptionsPagePanel findPagePanel(Component content) {
        if (content instanceof CampaignOptionsPagePanel pagePanel) {
            return pagePanel;
        }
        if (content instanceof Container container) {
            for (Component child : container.getComponents()) {
                CampaignOptionsPagePanel pagePanel = findPagePanel(child);
                if (pagePanel != null) {
                    return pagePanel;
                }
            }
        }
        return null;
    }

    private Component getDisplayContent(Component content, @Nullable String quoteResourceName) {
        if (content instanceof CampaignOptionsPagePanel) {
            return content;
        }
        return CampaignOptionsUtilities.createContentWithQuote(content, quoteResourceName);
    }

    void resetScrollPosition() {
        contentScrollPane.getVerticalScrollBar().setValue(0);
    }

    @Override
    public void removeNotify() {
        CampaignOptionsUtilities.setTipTextConsumer(null);
        super.removeNotify();
    }

    private static class CampaignOptionsContentPanel extends JPanel implements Scrollable {
        private CampaignOptionsContentPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_SPEED;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
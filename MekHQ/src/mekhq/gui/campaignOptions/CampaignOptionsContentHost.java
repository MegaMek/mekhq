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
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import jakarta.annotation.Nullable;
import javax.swing.SwingUtilities;
import megamek.common.ui.FastJScrollPane;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsTextField;

/**
 * Owns the central Campaign Options content area and sticky contextual help surface.
 */
class CampaignOptionsContentHost extends JPanel {
    private static final int SCROLL_SPEED = 16;

    private final JPanel contentPanel;
    private final JScrollPane contentScrollPane;
    private final CampaignOptionsHelpPanel helpPanel;
    private final Consumer<String> tipConsumer;

    CampaignOptionsContentHost(Component content) {
        this(content, null, true);
    }

    CampaignOptionsContentHost(Component content, @Nullable String quoteResourceName, boolean showHelpPanel) {
        super(new BorderLayout());
        setName("campaignOptionsContentHost");

        helpPanel = new CampaignOptionsHelpPanel();
        tipConsumer = helpPanel::setHelpText;
        CampaignOptionsUtilities.setTipTextConsumer(tipConsumer);

        contentPanel = new CampaignOptionsContentPanel();
        contentPanel.setName("campaignOptionsContentPanel");

        contentScrollPane = new FastJScrollPane(contentPanel,
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
        boolean detailsPanelShown = shouldShowHelpPanel(content, showHelpPanel);
        helpPanel.setVisible(detailsPanelShown);
        contentPanel.removeAll();
        Component displayContent = getDisplayContent(content, quoteResourceName);
        contentPanel.add(displayContent, BorderLayout.CENTER);
        if (detailsPanelShown) {
            // The Option Details box already shows each control's help on hover, so drop the now-redundant floating
            // tooltips from the option inputs on this page. Pages that hide the box keep their tooltips as the only
            // hover help.
            clearRedundantOptionToolTips(displayContent);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
        revalidate();
        repaint();
        // Always show a freshly mounted page from the top. Deferred so it runs after the new content has been laid out
        // (notably for the initial page, which now opens with its sections expanded and is taller than the viewport).
        SwingUtilities.invokeLater(this::resetScrollPosition);
    }

    /**
     * Recursively clears the Swing tooltips on the option-input controls of a page that shows the Option Details box,
     * since that box already presents the same help text on hover. Buttons keep their tooltips, and pages that hide the
     * box are left untouched so their tooltips remain the only hover help.
     *
     * @param component the content subtree to process
     */
    private static void clearRedundantOptionToolTips(Component component) {
        if (component instanceof JComponent jComponent
                  && (component instanceof CampaignOptionsCheckBox
                  || component instanceof CampaignOptionsSpinner
                  || component instanceof CampaignOptionsLabel
                  || component instanceof CampaignOptionsTextField)) {
            jComponent.setToolTipText(null);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                clearRedundantOptionToolTips(child);
            }
        }
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
     * <p>Most pages hand their page panel in directly. The recursive lookup exists for {@code AbilitiesPages}, which
     * wraps its page in a {@link java.awt.BorderLayout} container so it can rebuild the page in place when the special
     * ability list changes (the content host keeps a stable container reference while the inner page is swapped).</p>
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
        CampaignOptionsUtilities.clearTipTextConsumer(tipConsumer);
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

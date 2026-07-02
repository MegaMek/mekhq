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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nonnull;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.model.RankTableModel;
import mekhq.gui.panes.RankSystemsPane;

/**
 * The {@code RankPage} class builds and manages the Rank leaf page of the Biography section of the Campaign Options
 * dialog. It owns the embedded {@link RankSystemsPane} that lets the user configure the campaign's rank systems.
 *
 * <p>This view is a sub-component of {@link BiographyPages}: the overall load/apply lifecycle still lives on
 * {@code BiographyPages}. Unlike the other Biography leaf pages, the Rank page does not participate in the shared
 * {@link BiographyOptionsModel}; the rank systems are stored and applied through the {@link RankSystemsPane} directly.
 * Because {@code BiographyPages} selects the active rank system during its load lifecycle - which can run before the
 * panel is ever shown - the {@code RankSystemsPane} is constructed eagerly when this view is created, and the page
 * exposes {@link #setSelectedRankSystem(RankSystem)} and {@link #applyToCampaign()} for the coordinator to drive the
 * rank-specific load and apply steps.</p>
 */
class RankPage {
    private static final int RANK_SYSTEMS_PANEL_WIDTH = 860;
    private static final int RANK_TABLE_HEIGHT = 420;
    private static final int RANK_RATE_COLUMN_WIDTH = 60;
    private static final int RANK_NAME_COLUMN_WIDTH = 100;
    private static final int RANK_OFFICER_COLUMN_WIDTH = 75;
    private static final int RANK_PAY_MULTIPLIER_COLUMN_WIDTH = 90;

    private final RankSystemsPane rankSystemsPane;

    /**
     * Constructs the Rank page and eagerly builds the embedded {@link RankSystemsPane} for the supplied campaign. The
     * pane is created up front because the coordinator's load lifecycle may select a rank system before the page panel
     * is built.
     *
     * @param campaign the campaign whose rank systems are configured by this page
     */
    RankPage(@Nonnull Campaign campaign) {
        rankSystemsPane = new RankSystemsPane(null, campaign);
    }

    /**
     * Builds and returns the Rank page panel, embedding the configured rank systems pane.
     *
     * @return a {@link JPanel} representing the Rank Page
     */
    @Nonnull JPanel createPanel() {
        // Header
        String imageAddress = getImageDirectory() + "logo_umayyad_caliphate.png";
        CampaignOptionsHeaderPanel headerPanel = new CampaignOptionsHeaderPanel("RankPage", imageAddress);

        // Contents
        JPanel rankSystemsPanel = createRankSystemsPanel();
        return CampaignOptionsPagePanel.builder("RankPage", "RankPage", imageAddress)
            .header(headerPanel)
            .showDetailsPanel(false)
            .intro("rankPage")
            .quote("rankPage")
            .section("lblRankPage.text", "lblRankPage.summary", rankSystemsPanel)
            .build();
    }

    private @Nonnull JPanel createRankSystemsPanel() {
        rankSystemsPane.applyToCampaign();
        configureEmbeddedRankSystemsPane();

        JPanel rankSystemsPanel = new JPanel(new BorderLayout());
        rankSystemsPanel.setName("pnlRankSystemsPanel");
        rankSystemsPanel.setOpaque(false);
        rankSystemsPanel.add(rankSystemsPane, BorderLayout.CENTER);

        return rankSystemsPanel;
    }

    private void configureEmbeddedRankSystemsPane() {
        rankSystemsPane.setBorder(BorderFactory.createEmptyBorder());
        rankSystemsPane.setViewportBorder(null);
        rankSystemsPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rankSystemsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        removeEmbeddedRankSystemsBorders(rankSystemsPane.getViewport().getView());

        JTable ranksTable = rankSystemsPane.getRanksTable();
        ranksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        ranksTable.setFillsViewportHeight(true);

        JTable ranksRowHeader = rankSystemsPane.getRanksRowHeaderTable();
        if (ranksRowHeader != null) {
            ranksRowHeader.setFillsViewportHeight(true);
        }

        // Single source of truth for embedded column widths. The pane re-applies this provider on every rank-system
        // switch and when the "Restore default column widths" header menu item is used, so there is no competing
        // width listener to fight (which previously let the wide standalone widths win).
        rankSystemsPane.setColumnWidthProvider(RankPage::getEmbeddedRankColumnWidth);

        JScrollPane tableScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class,
                ranksTable);
        if (tableScrollPane != null) {
            Dimension tableSize = new Dimension(RANK_SYSTEMS_PANEL_WIDTH, RANK_TABLE_HEIGHT);
            tableScrollPane.setPreferredSize(tableSize);
            tableScrollPane.setMinimumSize(new Dimension(0, RANK_TABLE_HEIGHT));
            tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

        Dimension rankSystemsSize = getEmbeddedRankSystemsSize();
        rankSystemsPane.setPreferredSize(rankSystemsSize);
        rankSystemsPane.setMinimumSize(new Dimension(0, rankSystemsSize.height));
    }

    private Dimension getEmbeddedRankSystemsSize() {
        Component view = rankSystemsPane.getViewport().getView();
        int viewHeight = view == null ? RANK_TABLE_HEIGHT : view.getPreferredSize().height;
        int horizontalScrollBarHeight = rankSystemsPane.getHorizontalScrollBar().getPreferredSize().height;

        return new Dimension(RANK_SYSTEMS_PANEL_WIDTH, viewHeight + horizontalScrollBarHeight);
    }

    private static int getEmbeddedRankColumnWidth(int modelIndex) {
        return switch (modelIndex) {
            case RankTableModel.COL_NAME_RATE -> RANK_RATE_COLUMN_WIDTH;
            case RankTableModel.COL_OFFICER -> RANK_OFFICER_COLUMN_WIDTH;
            case RankTableModel.COL_PAY_MULTI -> RANK_PAY_MULTIPLIER_COLUMN_WIDTH;
            default -> RANK_NAME_COLUMN_WIDTH;
        };
    }

    private void removeEmbeddedRankSystemsBorders(Component component) {
        if (component instanceof JPanel panel && ("rankSystemPanel".equals(panel.getName()) ||
                "rankSystemFileButtonsPanel".equals(panel.getName()))) {
            panel.setBorder(BorderFactory.createEmptyBorder());
        }

        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                removeEmbeddedRankSystemsBorders(child);
            }
        }
    }

    /**
     * Selects the given rank system in the embedded rank systems pane. Used by the coordinator's load lifecycle.
     *
     * @param rankSystem the rank system to select
     */
    void setSelectedRankSystem(RankSystem rankSystem) {
        rankSystemsPane.getComboRankSystems().setSelectedItem(rankSystem);
    }

    /**
     * Applies the rank systems pane's current state to the campaign. Used by the coordinator's apply lifecycle.
     */
    void applyToCampaign() {
        rankSystemsPane.applyToCampaign();
    }
}

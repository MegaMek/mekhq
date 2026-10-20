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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.dialog.markets.contractMarket.ChaosContractMarketDialog.RESOURCE_BUNDLE;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.assessmentDifficulty;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.marketName;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.objective;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.severityColor;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.signedCompactMoney;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.baseComponents.hud.HudRing;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.baseComponents.hud.HudTabStrip;
import mekhq.gui.dialog.markets.contractMarket.ChaosContractMarketDialog.MarketSort;
import mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.MarketBadge;
import mekhq.gui.utilities.WrapLayout;

/**
 * The market's Offers page: a board of offers on the left, filtered by search type and sortable by what matters to
 * the player, with each row showing the fight's difficulty (its ring) and the estimated net per month; the selected
 * offer's {@link ContractDossierPanel dossier} on the right.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class MarketOffersPage {
    /** The board's narrowest width; it grows past this when its controls need more room to sit on one line. */
    private static final int BOARD_WIDTH = 360;
    private static final int BOARD_PADDING = 12;
    /** The widest estimated-profit figure a row is sized for, a loss in the tens of millions. */
    private static final double WIDEST_FIGURE = -88_880_000;

    private final ChaosContractMarketDialog market;
    private final JPanel root = new JPanel(new BorderLayout());

    private final List<ContractSearchType> searchTypes;
    private final HudTabStrip searchStrip;
    private final Map<MarketSort, HudChip> sortChips = new EnumMap<>(MarketSort.class);
    private final DefaultListModel<AbstractContract> listModel = new DefaultListModel<>();
    private final JList<AbstractContract> list = new JList<>(listModel);
    private final HudConfirmButton clearBoard;
    private final JPanel dossierHolder = new JPanel(new BorderLayout());

    private AbstractContract shownInDossier;
    private boolean updatingList;
    private int boardWidth = scaleForGUI(BOARD_WIDTH);

    MarketOffersPage(ChaosContractMarketDialog market) {
        this.market = market;
        root.setOpaque(true);
        root.setBackground(GROUND);

        searchTypes = market.allowedSearchTypes();
        List<String> searchLabels = new ArrayList<>();
        for (ContractSearchType type : searchTypes) {
            searchLabels.add(ChaosContractMarketDialog.searchTypeLabel(type));
        }
        searchStrip = new HudTabStrip(searchLabels, true, index -> market.changeSearchType(searchTypes.get(index)));

        // The chips wrap onto a second line rather than running off the board's edge.
        JPanel sortRow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(2)));
        sortRow.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "board.contractMarket.sort")
                                            .toUpperCase(Locale.ROOT)));
        for (MarketSort sort : MarketSort.values()) {
            HudChip chip = new HudChip(sort.label(), () -> market.setSort(sort));
            chip.setToolTipText(sort.toolTip());
            sortChips.put(sort, chip);
            sortRow.add(chip);
        }

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.setCellRenderer(new OfferRenderer());
        // Rows stretch to the list's width; a narrow fixed width only stops the list asking for a wider board.
        list.setFixedCellWidth(scaleForGUI(200));
        // Names wrap to the list's width, so each row's height depends on it: re-measure when the width changes.
        list.addComponentListener(new ComponentAdapter() {
            private int measuredWidth = -1;

            @Override
            public void componentResized(ComponentEvent event) {
                if (list.getWidth() != measuredWidth) {
                    measuredWidth = list.getWidth();
                    list.setFixedCellHeight(1);
                    list.setFixedCellHeight(-1);
                }
            }
        });
        // Lets each row's tooltip (its difficulty) show; a list only asks its renderer once registered.
        ToolTipManager.sharedInstance().registerComponent(list);
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingList && (list.getSelectedValue() != null)) {
                market.select(list.getSelectedValue());
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, true);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        HudButton generate = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.generateNew")
                                                 .toUpperCase(Locale.ROOT), false, true);
        generate.setArmed(market.isGM());
        generate.setToolTipText(gmToolTip(market.isGM(), "button.contractMarket.generateNew.tooltip"));
        generate.addActionListener(event -> market.generateNewContracts());
        HudButton create = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.createNew")
                                               .toUpperCase(Locale.ROOT), false, true);
        create.setArmed(market.isGM());
        create.setToolTipText(gmToolTip(market.isGM(), "button.contractMarket.createNew.tooltip"));
        create.addActionListener(event -> market.createNewContract());
        clearBoard = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.deleteAll")
                                                .toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "button.contractMarket.deleteAll.confirm").toUpperCase(Locale.ROOT), true);
        clearBoard.addActionListener(event -> market.deleteAllContracts());

        // The buttons wrap onto a second line when the board is too narrow for all three.
        JPanel boardFoot = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(5), scaleForGUI(3)));
        boardFoot.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), 0, 0, 0));
        boardFoot.add(generate);
        boardFoot.add(create);
        boardFoot.add(clearBoard);

        JPanel boardTop = MarketHud.column();
        boardTop.add(HudStyle.leftAligned(searchStrip));
        boardTop.add(Box.createVerticalStrut(scaleForGUI(8)));
        boardTop.add(HudStyle.leftAligned(sortRow));
        boardTop.add(Box.createVerticalStrut(scaleForGUI(8)));

        JPanel board = new JPanel(new BorderLayout());
        board.setOpaque(true);
        board.setBackground(GROUND);
        board.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(BOARD_PADDING), scaleForGUI(10),
              scaleForGUI(BOARD_PADDING)));
        // Wide enough for the sort chips and the buttons to each sit on a single line, whatever the font and scale.
        boardWidth = Math.max(scaleForGUI(BOARD_WIDTH),
              Math.max(singleLineWidth(sortRow), singleLineWidth(boardFoot)) + 2 * scaleForGUI(BOARD_PADDING));
        board.setPreferredSize(new Dimension(boardWidth, 0));
        board.add(boardTop, BorderLayout.NORTH);
        board.add(listScroll, BorderLayout.CENTER);
        board.add(boardFoot, BorderLayout.SOUTH);

        dossierHolder.setOpaque(true);
        dossierHolder.setBackground(SURFACE_DEEP);
        dossierHolder.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));

        root.add(board, BorderLayout.WEST);
        root.add(dossierHolder, BorderLayout.CENTER);
    }

    JComponent getComponent() {
        return root;
    }

    /**
     * Rebuilds the board from the market's state, and the dossier when the selection changed or a rebuild is asked
     * for.
     *
     * @param rebuildDossier {@code true} to rebuild the dossier even if the same offer is still selected
     */
    void refresh(boolean rebuildDossier) {
        searchStrip.setSelected(searchTypes.indexOf(market.searchType()));
        for (Map.Entry<MarketSort, HudChip> entry : sortChips.entrySet()) {
            entry.getValue().setActive(entry.getKey() == market.sort());
        }

        List<AbstractContract> ordered = market.sortedContracts();
        updatingList = true;
        try {
            listModel.clear();
            ordered.forEach(listModel::addElement);
            AbstractContract selected = market.selected();
            if (selected != null) {
                list.setSelectedValue(selected, true);
            } else {
                list.clearSelection();
            }
        } finally {
            updatingList = false;
        }
        clearBoard.reset();
        clearBoard.setArmed(!ordered.isEmpty());

        AbstractContract selected = market.selected();
        if (rebuildDossier || (selected != shownInDossier)) {
            showDossier(selected);
        }
    }

    private void showDossier(AbstractContract contract) {
        shownInDossier = contract;
        dossierHolder.removeAll();
        if (contract == null) {
            dossierHolder.add(buildEmptyState(), BorderLayout.NORTH);
        } else {
            dossierHolder.add(new ContractDossierPanel(market.campaign(), contract, market.currentDate(),
                  market.estimate(contract), market.isContractTypeHidden(), market), BorderLayout.CENTER);
        }
        dossierHolder.revalidate();
        dossierHolder.repaint();
    }

    /**
     * The tooltip for a GM-only button: what it does when GM mode is on, or why it is unavailable when it is off.
     *
     * @param isGM          {@code true} if the campaign is in GM mode
     * @param toolTipKey    resource key of what the button does
     */
    static String gmToolTip(boolean isGM, String toolTipKey) {
        return getTextAt(RESOURCE_BUNDLE, isGM ? toolTipKey : "button.contractMarket.gmOnly.tooltip");
    }

    /**
     * The width a wrapping row of controls needs to keep every control on one line: their widths plus the layout's
     * gaps, one before each control and one after the last, as {@link WrapLayout} reserves them.
     */
    private static int singleLineWidth(JPanel row) {
        int gap = ((FlowLayout) row.getLayout()).getHgap();
        int width = row.getInsets().left + row.getInsets().right + gap;
        for (Component component : row.getComponents()) {
            width += component.getPreferredSize().width + gap;
        }
        // A pixel or two of slack, so rounding never tips the last control onto a second line.
        return width + scaleForGUI(2);
    }

    /**
     * The notice shown when the board is empty. When the market is disabled advancing the calendar will never
     * populate the board, so the standard "check back next month" advice would be misleading - point the player at
     * the option instead.
     */
    private JComponent buildEmptyState() {
        boolean marketDisabled = market.isMarketDisabled();
        String titleKey = marketDisabled ? "empty.contractMarket.disabled.title" : "empty.contractMarket.title";
        String bodyKey = marketDisabled ? "empty.contractMarket.disabled.body" : "empty.contractMarket.body";

        JPanel text = MarketHud.column();
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, titleKey).toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 0.95f, 0.12f));
        text.add(HudStyle.leftAligned(title));
        text.add(Box.createVerticalStrut(scaleForGUI(6)));
        text.add(HudStyle.leftAligned(MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE, bodyKey), TEXT_MUTED,
              Font.PLAIN, 0.92f)));

        JPanel notice = MarketHud.framedSurface(18);
        notice.add(text, BorderLayout.CENTER);

        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(16), scaleForGUI(16), scaleForGUI(16),
              scaleForGUI(16)));
        holder.add(notice, BorderLayout.CENTER);
        return holder;
    }

    /**
     * Draws each offer as a board card in the style of {@link HudCard}: the ring in the difficulty's colour, the name,
     * the employer, mission type and term beneath, any badges, and the estimated profit (combat pay included) on the
     * right. Unlike {@code HudCard}, the text wraps to the list's width, so long operation names are never cut off.
     */
    private final class OfferRenderer extends JPanel implements ListCellRenderer<AbstractContract> {
        /** Gaps and padding around the text: the card's insets, the accent bar, the ring, and the gaps between. */
        private static final int TEXT_CHROME = 10 + 10 + 3 + 12 + 9 + 9 + 6;

        private final HudRing ring = new HudRing();
        private final JLabel text = new JLabel();
        private final JLabel figure = new JLabel();
        private final JLabel figureUnit = new JLabel();
        private final int figureWidth;

        private OfferRenderer() {
            super(new BorderLayout(scaleForGUI(9), 0));
            text.setVerticalAlignment(SwingConstants.TOP);
            text.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));

            figure.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
            figure.setAlignmentX(Component.RIGHT_ALIGNMENT);
            // Measured with sample text: an empty label has no height, which would cut the figure off at the bottom.
            String widestFigure = signedCompactMoney(Money.of(WIDEST_FIGURE));
            figure.setText(widestFigure);
            figureUnit.setText(getTextAt(RESOURCE_BUNDLE, "board.contractMarket.profit"));
            figureUnit.setFont(hudFont(Font.PLAIN, 0.7f, 0.08f));
            figureUnit.setForeground(TEXT_FAINT);
            figureUnit.setAlignmentX(Component.RIGHT_ALIGNMENT);
            figureWidth = Math.max(getFontMetrics(figure.getFont()).stringWidth(widestFigure),
                  getFontMetrics(figureUnit.getFont()).stringWidth(figureUnit.getText()));

            JPanel figureColumn = Hud.transparentPanel(null);
            figureColumn.setLayout(new BoxLayout(figureColumn, BoxLayout.Y_AXIS));
            figureColumn.add(figure);
            figureColumn.add(figureUnit);
            figureColumn.setPreferredSize(new Dimension(figureWidth, figureColumn.getPreferredSize().height));

            JPanel ringHolder = Hud.transparentPanel(new BorderLayout());
            ringHolder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(2), 0, 0, 0));
            ringHolder.add(ring, BorderLayout.NORTH);
            JPanel figureHolder = Hud.transparentPanel(new BorderLayout());
            figureHolder.add(figureColumn, BorderLayout.NORTH);

            add(ringHolder, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(figureHolder, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AbstractContract> source,
              AbstractContract contract, int index, boolean isSelected, boolean cellHasFocus) {
            String separator = getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator");
            List<String> parts = new ArrayList<>();
            parts.add(contract.getEmployerMarketDisplayName());
            if (!market.isContractTypeHidden()) {
                parts.add(objective(contract, false));
            }
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "card.contractMarket.months",
                  contract.getLengthInMonths()));

            StringBuilder html = new StringBuilder("<html><b style='color:").append(hex(TEXT)).append("'>")
                                       .append(MarketHud.escape(marketName(contract))).append("</b><br>")
                                       .append("<span style='font-size:smaller; color:").append(hex(TEXT_MUTED))
                                       .append("'>").append(MarketHud.escape(String.join(separator, parts)))
                                       .append("</span>");
            List<String> badges = new ArrayList<>();
            for (MarketBadge badge : ContractMarketFormatting.badges(contract)) {
                badges.add("<span style='color:" + hex(badge.color()) + "'>"
                                 + MarketHud.escape(badge.text().toUpperCase(Locale.ROOT)) + "</span>");
            }
            if (!badges.isEmpty()) {
                html.append("<br><span style='font-size:smaller'><b>").append(String.join(separator, badges))
                      .append("</b></span>");
            }
            text.setText(html.append("</html>").toString());
            fitTextToWidth(textWidth(source));

            ring.setColor(severityColor(assessmentDifficulty(contract)), true);
            Money profit = market.estimate(contract).getNetWithCombatPay();
            figure.setText(signedCompactMoney(profit));
            figure.setForeground(profit.isNegative() ? DANGER : READY);

            setOpaque(true);
            setBackground(isSelected ? SURFACE : SURFACE_DEEP);
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                        BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(isSelected ? 7 : 10),
                              scaleForGUI(8), scaleForGUI(10)))));
            setToolTipText(ChaosContractMarketDialog.difficultyToolTip(contract));
            return this;
        }

        /**
         * Sizes the text to wrap at the given width. Swing's HTML view reports how tall the text is when laid out that
         * wide; a CSS width would not do, as Swing scales its {@code px} unit by about 1.3 and the text would overrun
         * the figure beside it.
         */
        private void fitTextToWidth(int width) {
            View view = (View) text.getClientProperty(BasicHTML.propertyKey);
            if (view == null) {
                text.setPreferredSize(null);
                return;
            }
            view.setSize(width, 0);
            int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
            text.setPreferredSize(new Dimension(width, height));
            text.setMinimumSize(new Dimension(0, height));
        }

        /**
         * The width the name and sub-line wrap to: the list's width less the ring, the figure and the card's padding.
         * Before the list is laid out it has no width, so the board's own width stands in for it.
         */
        private int textWidth(JList<?> source) {
            int listWidth = source.getWidth() > 0
                                  ? source.getWidth()
                                  : boardWidth - 2 * scaleForGUI(BOARD_PADDING) - scaleForGUI(2 + 10);
            return Math.max(scaleForGUI(80), listWidth - scaleForGUI(TEXT_CHROME) - figureWidth);
        }
    }
}

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
package mekhq.gui.dialog.markets.personnelMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.personnel.enums.GenderDescriptors.MALE_FEMALE_OTHER;
import static mekhq.gui.baseComponents.hud.Hud.escape;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.compactMoney;
import static mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.ApplicantFacts;
import mekhq.gui.utilities.WrapLayout;

/**
 * The applicant board in the middle of the personnel market: the sort chips, then one card per applicant the role
 * filter and search let through, then a count of what is shown and the GM's tools. Each card shows the applicant's
 * portrait, name, profession, age and gender, their experience and anything notable (abilities, flaws, a rare
 * profession), and what they cost to hire.
 *
 * <p>Several applicants can be picked at once with Ctrl- and Shift-click; the most recently clicked is the one the
 * dossier shows. Double-clicking an applicant opens their full personnel record.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ApplicantBoard {
    static final int BOARD_WIDTH = 430;
    private static final int PORTRAIT_SIZE = 40;
    /** The widest hiring cost a card's figure is sized for, so every card's figure column lines up. */
    private static final double WIDEST_FIGURE = 8_880_000;

    private final PersonnelMarketDialog market;
    private final JPanel root = new JPanel(new BorderLayout());
    private final Map<ApplicantSort, HudChip> sortChips = new EnumMap<>(ApplicantSort.class);
    private final DefaultListModel<Person> listModel = new DefaultListModel<>();
    private final JList<Person> list = new JList<>(listModel);
    private final CardLayout bodyLayout = new CardLayout();
    private final JPanel body = new JPanel(bodyLayout);
    private final JLabel emptyNotice = Hud.notice("");
    private final JLabel matches = Hud.hint("");
    private final Map<Person, Icon> portraits = new HashMap<>();
    private boolean updatingList;

    ApplicantBoard(PersonnelMarketDialog market) {
        this.market = market;
        root.setOpaque(true);
        root.setBackground(GROUND);
        root.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(10),
              scaleForGUI(14)));
        root.setPreferredSize(new Dimension(scaleForGUI(BOARD_WIDTH), 0));

        // The chips wrap onto a second line rather than running off the board's edge.
        JPanel sortRow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(2)));
        sortRow.add(keyLabel(getTextAt(RESOURCE_BUNDLE, "board.personnelMarket.sort").toUpperCase(Locale.ROOT)));
        for (ApplicantSort sort : ApplicantSort.values()) {
            HudChip chip = new HudChip(sort.label(), () -> market.setSort(sort));
            chip.setToolTipText(sort.toolTip());
            sortChips.put(sort, chip);
            sortRow.add(chip);
        }
        // Offset by the layout's own gap, so the row lines up with the list's edge.
        sortRow.setBorder(BorderFactory.createEmptyBorder(0, -scaleForGUI(4), scaleForGUI(6), 0));

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.setCellRenderer(new ApplicantRenderer());
        // Rows stretch to the list's width; a narrow fixed width only stops the list asking for a wider board.
        list.setFixedCellWidth(scaleForGUI(200));
        // Tags wrap to the list's width, so each row's height depends on it: re-measure when the width changes.
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
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingList) {
                market.selectionChanged();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = list.locationToIndex(event.getPoint());
                if (event.getClickCount() == 2 && index >= 0 && list.getCellBounds(index, index)
                                                                         .contains(event.getPoint())) {
                    market.showFullRecord(listModel.getElementAt(index));
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, true);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        emptyNotice.setVerticalAlignment(SwingConstants.TOP);
        emptyNotice.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(24), scaleForGUI(12), 0, scaleForGUI(12)));
        body.setOpaque(false);
        body.add(listScroll, "list");
        body.add(emptyNotice, "empty");

        HudButton addApplicant = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.add.gm")
                                                     .toUpperCase(Locale.ROOT), false, true);
        // A GM tool: armed only in GM mode, with a tooltip saying why when it is not.
        addApplicant.setArmed(market.isGM());
        String addToolTipKey = market.isGM() ? "button.personnelMarket.add.gm.tooltip"
                                     : "button.personnelMarket.gmOnly.tooltip";
        addApplicant.setToolTipText(Hud.wrapToolTip(getTextAt(RESOURCE_BUNDLE, addToolTipKey)));
        addApplicant.addActionListener(event -> {
            if (market.isGM()) {
                market.addApplicant();
            }
        });
        JPanel foot = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        foot.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), 0, 0, 0));
        foot.add(matches, BorderLayout.CENTER);
        foot.add(addApplicant, BorderLayout.EAST);

        root.add(sortRow, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(foot, BorderLayout.SOUTH);
    }

    JComponent getComponent() {
        return root;
    }

    /**
     * Refills the board, keeping as much of the selection as is still shown. If none of it is, the applicant now at
     * {@code fallbackIndex} is selected instead (or the last, if the board got shorter), so hiring someone moves the
     * selection on to the next applicant rather than back to the top.
     *
     * @param shown         the applicants to show, in board order
     * @param total         how many applicants the market has in all, for the count beneath the board
     * @param keep          the applicants to keep selected, if still shown
     * @param fallbackIndex the position to select if none of {@code keep} is shown
     * @param sort          the board's order, to mark its chip
     * @param isReversed    {@code true} if the order runs the other way from its natural direction
     * @param emptyText     the notice to show if no applicant is shown
     *
     * @author Illiani
     * @since 0.51.01
     */
    void show(List<Person> shown, int total, Collection<Person> keep, int fallbackIndex, ApplicantSort sort,
          boolean isReversed, String emptyText) {
        for (Map.Entry<ApplicantSort, HudChip> entry : sortChips.entrySet()) {
            ApplicantSort chipSort = entry.getKey();
            HudChip chip = entry.getValue();
            boolean isActive = chipSort == sort;
            chip.setActive(isActive);
            // The active chip shows which way the order runs; clicking it again reverses the order.
            chip.setText(isActive ? getFormattedTextAt(RESOURCE_BUNDLE,
                  isReversed ? "sort.personnelMarket.active.reversed" : "sort.personnelMarket.active.label",
                  chipSort.label()) : chipSort.label());
            chip.setToolTipText(isActive ? getFormattedTextAt(RESOURCE_BUNDLE, "sort.personnelMarket.active.tooltip",
                  chipSort.toolTip()) : chipSort.toolTip());
        }

        Person previousLead = lead();
        updatingList = true;
        try {
            listModel.clear();
            shown.forEach(listModel::addElement);
            List<Integer> indices = new ArrayList<>();
            for (Person person : keep) {
                int index = shown.indexOf(person);
                if (index >= 0) {
                    indices.add(index);
                }
            }
            if (indices.isEmpty() && !shown.isEmpty()) {
                indices.add(Math.clamp(fallbackIndex, 0, shown.size() - 1));
            }
            list.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
            // Restoring the selection makes its last row the lead; give the lead back to the applicant the player last
            // clicked, so the dossier keeps showing them. Re-adding a selected row only moves the lead.
            int leadIndex = previousLead == null ? -1 : shown.indexOf(previousLead);
            if (leadIndex >= 0 && indices.contains(leadIndex)) {
                list.addSelectionInterval(leadIndex, leadIndex);
            }
            if (!indices.isEmpty()) {
                list.ensureIndexIsVisible(indices.getFirst());
            }
        } finally {
            updatingList = false;
        }

        emptyNotice.setText("<html><div style='text-align:center'>" + escape(emptyText) + "</div></html>");
        bodyLayout.show(body, shown.isEmpty() ? "empty" : "list");
        matches.setText(getFormattedTextAt(RESOURCE_BUNDLE, "board.personnelMarket.matches", shown.size(), total));
    }

    /** @return the selected applicants, in board order */
    List<Person> selected() {
        return list.getSelectedValuesList();
    }

    /**
     * @return the applicant the dossier should show: the one most recently clicked if still selected, otherwise the
     *       first selected, or {@code null} if nobody is selected
     */
    @Nullable Person lead() {
        int lead = list.getLeadSelectionIndex();
        if (lead >= 0 && lead < listModel.size() && list.isSelectedIndex(lead)) {
            return listModel.getElementAt(lead);
        }
        List<Person> selected = selected();
        return selected.isEmpty() ? null : selected.getFirst();
    }

    /** @return the position of the first selected applicant, or {@code 0} if nobody is selected */
    int firstSelectedIndex() {
        return Math.max(0, list.getMinSelectionIndex());
    }

    /** Moves the keyboard focus to the board. */
    void focus() {
        list.requestFocusInWindow();
    }

    /** Forgets an applicant's cached portrait, once they have left the board. */
    void forget(Person person) {
        portraits.remove(person);
    }

    /**
     * Draws each applicant as a board card: the portrait, the name with the profession, age and gender beneath and the
     * notable tags below that, and the hiring cost on the right. The text wraps to the list's width, so long names and
     * many tags are never cut off.
     */
    private final class ApplicantRenderer extends JPanel implements ListCellRenderer<Person> {
        /** Gaps and padding around the text: the card's insets, the accent bar, the portrait, and the gaps between. */
        private static final int TEXT_CHROME = 10 + 10 + 3 + PORTRAIT_SIZE + 10 + 10 + 6;

        private final JLabel portrait = PersonnelMarketHud.portrait(null, scaleForGUI(PORTRAIT_SIZE));
        private final JLabel text = new JLabel();
        private final JLabel figure = new JLabel();
        private final JLabel figureKey = new JLabel();
        private final int figureWidth;

        private ApplicantRenderer() {
            super(new BorderLayout(scaleForGUI(10), 0));
            text.setVerticalAlignment(SwingConstants.TOP);
            text.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));

            figure.setFont(hudFont(Font.BOLD, 1.05f, 0.0f));
            figure.setAlignmentX(Component.RIGHT_ALIGNMENT);
            figureKey.setFont(hudFont(Font.BOLD, 0.66f, 0.12f));
            figureKey.setForeground(TEXT_FAINT);
            figureKey.setAlignmentX(Component.RIGHT_ALIGNMENT);
            // Measured with sample text: an empty label has no height, which would cut the figure off at the bottom.
            String widestFigure = compactMoney(Money.of(WIDEST_FIGURE));
            figure.setText(widestFigure);
            figureKey.setText(getTextAt(RESOURCE_BUNDLE, "board.personnelMarket.toHire").toUpperCase(Locale.ROOT));
            figureWidth = Math.max(getFontMetrics(figure.getFont()).stringWidth(widestFigure),
                  getFontMetrics(figureKey.getFont()).stringWidth(figureKey.getText()));

            JPanel figureColumn = Hud.transparentPanel(null);
            figureColumn.setLayout(new BoxLayout(figureColumn, BoxLayout.Y_AXIS));
            figureColumn.add(figure);
            figureColumn.add(figureKey);
            figureColumn.setPreferredSize(new Dimension(figureWidth, figureColumn.getPreferredSize().height));
            JPanel figureHolder = Hud.transparentPanel(new BorderLayout());
            figureHolder.add(figureColumn, BorderLayout.NORTH);

            add(Hud.topAligned(portrait), BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(figureHolder, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Person> source, Person person, int index,
              boolean isSelected, boolean cellHasFocus) {
            ApplicantFacts facts = market.facts(person);
            portrait.setIcon(portraits.computeIfAbsent(person,
                  key -> PersonnelMarketHud.portraitIcon(key, scaleForGUI(PORTRAIT_SIZE))));

            String separator = getTextAt(RESOURCE_BUNDLE, "value.personnelMarket.separator");
            StringBuilder html = new StringBuilder("<html><b style='color:").append(hex(TEXT)).append("'>")
                                       .append(escape(person.getFullName())).append("</b>");
            if (market.isRare(person)) {
                html.append(" <span style='color:").append(hex(AMBER)).append("'>&#9733;</span>");
            }
            List<String> sub = List.of(ApplicantDossier.roles(person),
                  getFormattedTextAt(RESOURCE_BUNDLE, "board.personnelMarket.age",
                        person.getAge(market.campaign().getLocalDate())),
                  MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender()));
            html.append("<br><span style='color:").append(hex(TEXT_FAINT)).append("'>")
                  .append(escape(String.join(separator, sub))).append("</span>");

            List<String> tags = new ArrayList<>();
            tags.add(tag(SkillType.getExperienceLevelName(facts.experienceLevel()),
                  ApplicantDossier.experienceColor(facts.experienceLevel())));
            // The counts are always shown, faint when zero, so every card can be compared at a glance.
            tags.add(tag(getFormattedTextAt(RESOURCE_BUNDLE, "tag.personnelMarket.abilities", facts.abilities()),
                  facts.abilities() > 0 ? READY : TEXT_FAINT));
            tags.add(tag(getFormattedTextAt(RESOURCE_BUNDLE, "tag.personnelMarket.flaws", facts.flaws()),
                  facts.flaws() > 0 ? DANGER : TEXT_FAINT));
            if (market.isRare(person)) {
                tags.add(tag(getTextAt(RESOURCE_BUNDLE, "tag.personnelMarket.rare"), AMBER));
            }
            html.append("<br><b>").append(String.join(separator, tags)).append("</b>");
            text.setText(html.append("</html>").toString());
            PersonnelMarketHud.fitHtmlToWidth(text, textWidth(source));

            Money cost = facts.hiringCost();
            figure.setText(cost.isPositive() ? compactMoney(cost) : getTextAt(RESOURCE_BUNDLE,
                  "board.personnelMarket.free"));
            figure.setForeground(market.balance().isLessThan(cost) ? DANGER : ACCENT_BRIGHT);

            setOpaque(true);
            setBackground(isSelected ? SURFACE : SURFACE_DEEP);
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                        BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(isSelected ? 7 : 10),
                              scaleForGUI(8), scaleForGUI(10)))));
            return this;
        }

        private static String tag(String text, Color color) {
            return "<span style='color:" + hex(color) + "'>" + escape(text.toUpperCase(Locale.ROOT)) + "</span>";
        }

        /** The width the text wraps to: the list's width less the portrait, the figure and the card's padding. */
        private int textWidth(JList<?> source) {
            int listWidth = source.getWidth() > 0 ? source.getWidth() : scaleForGUI(BOARD_WIDTH - 28);
            return Math.max(scaleForGUI(80), listWidth - scaleForGUI(TEXT_CHROME) - figureWidth);
        }
    }
}

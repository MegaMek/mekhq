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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.gui.roleplay.OracleConsole.escape;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.roleplay.Concepts;
import mekhq.campaign.roleplay.DiceExpression;
import mekhq.campaign.roleplay.Concepts.Concept;
import mekhq.campaign.roleplay.FateChart;
import mekhq.campaign.roleplay.FateChartAnswer;
import mekhq.campaign.roleplay.FateChartOdds;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.OracleActions.AskOutcome;
import mekhq.campaign.roleplay.OracleAnswer;
import mekhq.campaign.roleplay.OracleCharacter;
import mekhq.campaign.roleplay.OracleTable;
import mekhq.campaign.roleplay.PlotThreadStep;
import mekhq.campaign.roleplay.RandomEventFocus;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;

/**
 * The console's Ask page: the Fate Chart on the left (an optional question, the odds with their Yes thresholds at the
 * current chaos, and the Ask button) with Concepts beneath it; the answer, any random event, and the recent answers on
 * the right.
 */
class AskPage implements ConsoleSection {
    private static final int RECENT_ANSWERS = 5;
    private static final int ODDS_PER_ROW = 3;

    private final OracleConsole console;
    private final JPanel root = new JPanel(new BorderLayout());

    private final JPanel welcome;
    private final JTextField question = new JTextField();
    private final HudSegmentedControl<FateChartOdds> odds;
    private final HudVerdictBanner verdict = new HudVerdictBanner();
    private final JPanel eventSection = column();
    private final JPanel recent = column();

    private final JPanel conceptRows = column();
    private final List<ConceptRow> rows = new ArrayList<>();
    private final HudButton addConcept;
    private final JPanel conceptResults = new JPanel(new GridLayout(1, 1, scaleForGUI(1), 0));

    private static final List<String> QUICK_DICE = List.of("d6", "2d6", "d10", "d20", "d100");
    private final JTextField diceField = new JTextField();
    private final JLabel diceResult = new JLabel();

    AskPage(final OracleConsole console) {
        this.console = console;
        root.setOpaque(true);
        root.setBackground(GROUND);

        odds = new HudSegmentedControl<>(value -> { });
        odds.setColumns(ODDS_PER_ROW);
        List<Segment<FateChartOdds>> segments = new ArrayList<>();
        for (FateChartOdds value : FateChartOdds.values()) {
            segments.add(new Segment<>(value, value.getLabel(), "", true));
        }
        odds.setSegments(segments);
        odds.setSelected(FateChartOdds.FIFTY_FIFTY);

        Hud.styleField(question);
        question.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.question.toolTipText"));
        question.addActionListener(event -> ask());

        welcome = buildWelcome();

        HudButton askButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.button")
                                                  .toUpperCase(Locale.ROOT), true);
        askButton.addActionListener(event -> ask());

        addConcept = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.concepts.add").toUpperCase(Locale.ROOT),
              false, true);
        addConcept.addActionListener(event -> addConceptRow());
        HudButton rollConcepts = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.concepts.roll")
                                                     .toUpperCase(Locale.ROOT), true, true);
        rollConcepts.addActionListener(event -> rollConcepts());
        addConceptRow();
        conceptResults.setOpaque(true);
        conceptResults.setBackground(DIVIDER);
        conceptResults.setVisible(false);

        // Left column: the question, odds and Ask, then Concepts.
        JPanel left = column();
        left.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        left.add(leftAligned(welcome));
        left.add(Box.createVerticalStrut(scaleForGUI(12)));
        left.add(heading("OracleConsole.ask.question", "OracleConsole.ask.question.help"));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(question));
        left.add(Box.createVerticalStrut(scaleForGUI(14)));
        left.add(heading("OracleConsole.ask.odds", "OracleConsole.ask.odds.help"));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(odds));
        left.add(Box.createVerticalStrut(scaleForGUI(8)));
        left.add(leftAligned(rightButtonRow(Hud.hint(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.keys")),
              askButton)));
        left.add(Box.createVerticalStrut(scaleForGUI(20)));
        left.add(heading("OracleConsole.concepts", "OracleConsole.concepts.help"));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(conceptRows));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        JPanel conceptButtons = Hud.transparentPanel(null);
        conceptButtons.setLayout(new BoxLayout(conceptButtons, BoxLayout.X_AXIS));
        conceptButtons.add(addConcept);
        conceptButtons.add(Box.createHorizontalGlue());
        conceptButtons.add(rollConcepts);
        left.add(leftAligned(conceptButtons));
        left.add(Box.createVerticalStrut(scaleForGUI(10)));
        left.add(leftAligned(conceptResults));
        left.add(Box.createVerticalStrut(scaleForGUI(20)));
        left.add(heading("OracleConsole.dice", "OracleConsole.dice.help"));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(buildDiceRow()));
        left.add(Box.createVerticalStrut(scaleForGUI(6)));
        left.add(leftAligned(buildQuickDice()));
        left.add(Box.createVerticalStrut(scaleForGUI(8)));
        left.add(leftAligned(diceResult));

        // Right column: the answer, the random event, the recent answers.
        JPanel right = column();
        right.setOpaque(true);
        right.setBackground(SURFACE_DEEP);
        right.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14), scaleForGUI(16))));
        right.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.answer"))));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(verdict));
        right.add(Box.createVerticalStrut(scaleForGUI(14)));
        right.add(leftAligned(eventSection));
        right.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.recent"))));
        right.add(Box.createVerticalStrut(scaleForGUI(8)));
        right.add(leftAligned(recent));
        verdict.setVerdict(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.waiting"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.waiting.reason"), "–", ACCENT);

        JScrollPane leftScroll = scroll(left, GROUND);
        leftScroll.setPreferredSize(new Dimension(scaleForGUI(440), 0));
        root.add(leftScroll, BorderLayout.WEST);
        root.add(scroll(right, SURFACE_DEEP), BorderLayout.CENTER);
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    @Override
    public void refresh() {
        int chaos = console.roleplay().getChaosFactor();
        for (FateChartOdds value : FateChartOdds.values()) {
            odds.setSegmentSub(value, "≤ " + FateChart.getThresholds(value, chaos)[1]);
        }
        welcome.setVisible(!OracleGuidePreferences.getInstance().isWelcomeDismissed());
        refreshRecent();
    }

    /**
     * Selects odds by position, for the number-key shortcuts.
     *
     * @param index the odds' position, from 0 (Impossible) to 8 (Certain)
     */
    void selectOdds(final int index) {
        odds.setSelected(FateChartOdds.values()[index]);
    }

    // region Fate Chart

    private void ask() {
        FateChartOdds chosen = odds.getSelected();
        if (chosen == null) {
            return;
        }
        AskOutcome outcome = console.actions().ask(question.getText(), chosen);
        showAnswer(outcome.answer());
        showEvent(outcome);
        console.changed();
    }

    private void showAnswer(final OracleAnswer answer) {
        int[] thresholds = FateChart.getThresholds(answer.odds(), answer.chaos());
        List<String> ranges = new ArrayList<>();
        ranges.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.range.yes", thresholds[1]));
        if (thresholds[0] > 0) {
            ranges.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.range.exceptionalYes", thresholds[0]));
        }
        if (thresholds[2] <= 100) {
            ranges.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.range.exceptionalNo", thresholds[2]));
        }
        String reason = getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.reason", answer.odds().getLabel(),
              answer.chaos()) + " · " + String.join(" · ", ranges);
        if (!answer.question().isBlank()) {
            reason = "<i>“" + escape(answer.question()) + "”</i><br>" + reason;
        }
        verdict.setVerdict(answer.answer().getLabel(), "<html>" + reason + "</html>",
              getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.badge", answer.roll()), colorFor(answer.answer()));
    }

    private void showEvent(final AskOutcome outcome) {
        eventSection.removeAll();
        if (outcome.fate().hasRandomEvent()) {
            RandomEventFocus focus = outcome.fate().randomEventFocus();
            eventSection.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE,
                  "OracleConsole.ask.randomEvent"))));
            eventSection.add(Box.createVerticalStrut(scaleForGUI(8)));

            JPanel card = column();
            card.setOpaque(true);
            card.setBackground(SURFACE);
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
                  BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
                        scaleForGUI(12))));
            JLabel name = new JLabel(focus.getLabel().toUpperCase(Locale.ROOT) + "   ");
            name.setForeground(AMBER);
            name.setFont(hudFont(Font.BOLD, 1.0f, 0.1f));
            JLabel roll = Hud.hint(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.focusRoll",
                  outcome.fate().randomEventRoll()));
            JPanel top = Hud.transparentPanel(new BorderLayout());
            top.add(name, BorderLayout.WEST);
            top.add(roll, BorderLayout.EAST);
            card.add(leftAligned(top));
            card.add(Box.createVerticalStrut(scaleForGUI(4)));
            card.add(leftAligned(wrapped(focus.getDescription(), TEXT_MUTED)));
            JComponent effect = describeEffect(outcome, focus);
            if (effect != null) {
                card.add(Box.createVerticalStrut(scaleForGUI(8)));
                card.add(leftAligned(effect));
            }
            eventSection.add(leftAligned(card));
            eventSection.add(Box.createVerticalStrut(scaleForGUI(14)));
        }
        eventSection.revalidate();
        eventSection.repaint();
    }

    private @Nullable JComponent describeEffect(final AskOutcome outcome, final RandomEventFocus focus) {
        if (outcome.character() != null) {
            OracleCharacter character = outcome.character();
            JLabel show = Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.effect.character",
                  character.getName()), () -> console.showCharacter(character.getId()));
            Person person = character.isLinked() ? console.campaign().getPlayerForce().getHumanResources()
                                                         .getPerson(character.getPersonId()) : null;
            if (person == null) {
                return show;
            }
            JPanel links = column();
            links.add(leftAligned(show));
            links.add(Box.createVerticalStrut(scaleForGUI(4)));
            links.add(leftAligned(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.effect.check",
                  character.getName()), () -> console.showChecksFor(List.of(person), false))));
            return links;
        }
        if (outcome.thread() != null) {
            String name = outcome.thread().thread().getName();
            PlotThreadStep step = outcome.thread().revealed();
            String key = (step == null) ? "OracleConsole.ask.effect.threadLost" : "OracleConsole.ask.effect.threadProgress";
            JPanel effect = column();
            effect.add(leftAligned(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, key, name,
                  outcome.thread().stepNumber()), () -> console.showThread(outcome.thread().thread().getId()))));
            if (step != null) {
                effect.add(Box.createVerticalStrut(scaleForGUI(6)));
                effect.add(leftAligned(conceptTiles(step.concepts(), 2)));
            }
            return effect;
        }
        if (focus == RandomEventFocus.NEW_NPC) {
            return Hud.link(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.effect.generateNpc"), console::generateNpc);
        }
        if (outcome.isMissingCharacter()) {
            return wrapped(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.effect.noCharacter"), TEXT_FAINT);
        }
        if (outcome.isMissingThread()) {
            return wrapped(getTextAt(RESOURCE_BUNDLE, focus == RandomEventFocus.MOVE_TOWARD_A_THREAD
                                                            ? "OracleConsole.ask.effect.noThreadToProgress"
                                                            : "OracleConsole.ask.effect.noThreadToLose"), TEXT_FAINT);
        }
        return null;
    }

    private void refreshRecent() {
        recent.removeAll();
        List<JournalEntry> log = console.roleplay().getOracleLog();
        int shown = 0;
        for (int index = log.size() - 1; index >= 0 && shown < RECENT_ANSWERS; index--) {
            OracleAnswer answer = log.get(index).getAnswer();
            if (answer == null) {
                continue;
            }
            shown++;
            HudCard card = new HudCard().show(colorFor(answer.answer()), true, answer.answer().getLabel(), null,
                  List.of(), answer.question().isBlank() ? answer.odds().getLabel() : answer.question(),
                  Integer.toString(answer.roll()), OracleConsole.formatDate(log.get(index).getDate()), false);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.recent.toolTipText"));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    showAnswer(answer);
                    eventSection.removeAll();
                    eventSection.revalidate();
                }
            });
            recent.add(leftAligned(card));
        }
        if (shown == 0) {
            recent.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "OracleConsole.ask.recent.none"))));
        }
        recent.revalidate();
        recent.repaint();
    }

    /**
     * @param answer a Fate Chart answer
     *
     * @return its colour: green for Exceptional Yes, cyan for Yes, amber for No, red for Exceptional No
     */
    static Color colorFor(final FateChartAnswer answer) {
        return switch (answer) {
            case EXCEPTIONAL_YES -> READY;
            case NORMAL_YES -> ACCENT;
            case NORMAL_NO -> AMBER;
            case EXCEPTIONAL_NO -> DANGER;
        };
    }

    // endregion Fate Chart

    // region Welcome

    private JPanel buildWelcome() {
        JPanel banner = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        banner.setOpaque(true);
        banner.setBackground(SURFACE_HIGHLIGHT);
        banner.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_CYAN,
              scaleForGUI(1)), BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12))));
        JPanel text = column();
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleConsole.welcome.title").toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 0.95f, 0.12f));
        text.add(leftAligned(title));
        text.add(Box.createVerticalStrut(scaleForGUI(2)));
        JLabel intro = new JLabel("<html><div style='width:" + scaleForGUI(190) + "px'>"
                                        + escape(getTextAt(RESOURCE_BUNDLE, "OracleConsole.welcome.text"))
                                        + "</div></html>");
        intro.setForeground(TEXT_MUTED);
        intro.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        text.add(leftAligned(intro));

        HudButton notNow = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.welcome.dismiss")
                                               .toUpperCase(Locale.ROOT), false, true);
        notNow.addActionListener(event -> dismissWelcome());
        HudButton showMe = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.welcome.show")
                                               .toUpperCase(Locale.ROOT), true, true);
        showMe.addActionListener(event -> {
            dismissWelcome();
            console.openGuide(ConsolePage.ASK);
        });
        JPanel buttons = column();
        buttons.add(showMe);
        buttons.add(Box.createVerticalStrut(scaleForGUI(6)));
        buttons.add(notNow);

        banner.add(text, BorderLayout.CENTER);
        banner.add(buttons, BorderLayout.EAST);
        return banner;
    }

    private void dismissWelcome() {
        OracleGuidePreferences.getInstance().setWelcomeDismissed(true);
        welcome.setVisible(false);
    }

    // endregion Welcome

    // region Concepts

    /** One row of the Concepts picker: a category, a table in it, and a remove button. */
    private final class ConceptRow {
        private final JPanel panel = new JPanel(new BorderLayout(scaleForGUI(6), 0));
        private final JComboBox<String> category = new JComboBox<>(OracleTable.getCategories().toArray(new String[0]));
        private final JComboBox<OracleTable> table = new JComboBox<>();

        private ConceptRow() {
            panel.setOpaque(false);
            Hud.styleComboBox(category);
            Hud.styleComboBox(table);
            table.setRenderer(new DefaultListCellRenderer() {
                private final javax.swing.ListCellRenderer<Object> hud = Hud.comboRenderer();

                @Override
                @SuppressWarnings("unchecked")
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                      boolean isSelected, boolean cellHasFocus) {
                    Object display = (value instanceof OracleTable oracleTable) ? oracleTable.getTableLabel() : value;
                    return hud.getListCellRendererComponent((JList<Object>) list, display, index, isSelected,
                          cellHasFocus);
                }
            });
            category.setPreferredSize(new Dimension(scaleForGUI(130), category.getPreferredSize().height));
            category.addActionListener(event -> fillTables());
            fillTables();

            HudButton remove = new HudButton("×", false, true);
            remove.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.concepts.remove"));
            remove.addActionListener(event -> removeConceptRow(this));
            panel.add(category, BorderLayout.WEST);
            panel.add(table, BorderLayout.CENTER);
            panel.add(remove, BorderLayout.EAST);
        }

        private void fillTables() {
            String chosen = (String) category.getSelectedItem();
            table.setModel(new DefaultComboBoxModel<>(OracleTable.getTables(chosen).toArray(new OracleTable[0])));
        }
    }

    private void addConceptRow() {
        if (rows.size() >= Concepts.MAXIMUM_TABLES) {
            return;
        }
        ConceptRow row = new ConceptRow();
        rows.add(row);
        rebuildConceptRows();
    }

    private void removeConceptRow(final ConceptRow row) {
        if (rows.size() > 1) {
            rows.remove(row);
            rebuildConceptRows();
        }
    }

    private void rebuildConceptRows() {
        conceptRows.removeAll();
        for (ConceptRow row : rows) {
            conceptRows.add(leftAligned(row.panel));
            conceptRows.add(Box.createVerticalStrut(scaleForGUI(6)));
        }
        addConcept.setArmed(rows.size() < Concepts.MAXIMUM_TABLES);
        conceptRows.revalidate();
        conceptRows.repaint();
    }

    private void rollConcepts() {
        List<OracleTable> tables = new ArrayList<>();
        for (ConceptRow row : rows) {
            tables.add((OracleTable) row.table.getSelectedItem());
        }
        List<Concept> concepts = console.actions().rollConcepts(tables);
        conceptResults.removeAll();
        conceptResults.setLayout(new GridLayout(1, Math.max(1, concepts.size()), scaleForGUI(1), 0));
        for (JComponent tile : conceptTileList(concepts)) {
            conceptResults.add(tile);
        }
        conceptResults.setVisible(!concepts.isEmpty());
        conceptResults.revalidate();
        conceptResults.repaint();
        console.changed();
    }

    /**
     * @param concepts concepts to show
     * @param columns  how many tiles to a row
     *
     * @return the concepts as key/value tiles with 1px divider gaps
     */
    static JPanel conceptTiles(final List<Concept> concepts, final int columns) {
        int rows = Math.max(1, (concepts.size() + columns - 1) / columns);
        JPanel grid = new JPanel(new GridLayout(rows, columns, scaleForGUI(1), scaleForGUI(1)));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, scaleForGUI(1)));
        for (JComponent tile : conceptTileList(concepts)) {
            grid.add(tile);
        }
        for (int index = concepts.size(); index < rows * columns; index++) {
            JPanel filler = new JPanel();
            filler.setBackground(SURFACE_DEEP);
            grid.add(filler);
        }
        return grid;
    }

    private static List<JComponent> conceptTileList(final List<Concept> concepts) {
        List<JComponent> tiles = new ArrayList<>();
        for (Concept concept : concepts) {
            JPanel tile = column();
            tile.setOpaque(true);
            tile.setBackground(SURFACE_DEEP);
            tile.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(10), scaleForGUI(8),
                  scaleForGUI(10)));
            tile.add(leftAligned(Hud.eyebrow(concept.table().getCategory() + " · "
                                                   + concept.table().getTableLabel())));
            tile.add(Box.createVerticalStrut(scaleForGUI(2)));
            String meaning = (concept.meaning() == null) ? getTextAt(RESOURCE_BUNDLE, "OracleLog.noConcept")
                                   : concept.meaning();
            JLabel value = new JLabel(meaning);
            value.setForeground(TEXT);
            value.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
            tile.add(leftAligned(value));
            tiles.add(tile);
        }
        return tiles;
    }

    // endregion Concepts

    // region Dice

    private JPanel buildDiceRow() {
        Hud.styleField(diceField);
        diceField.setText("2d6");
        diceField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.field.toolTipText"));
        diceField.addActionListener(event -> rollDice(diceField.getText()));
        HudButton roll = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.roll").toUpperCase(Locale.ROOT),
              true, true);
        roll.addActionListener(event -> rollDice(diceField.getText()));
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
        row.add(diceField, BorderLayout.CENTER);
        row.add(roll, BorderLayout.EAST);
        diceResult.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        diceResult.setVisible(false);
        return row;
    }

    private JPanel buildQuickDice() {
        JPanel row = Hud.transparentPanel(null);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        for (String dice : QUICK_DICE) {
            if (row.getComponentCount() > 0) {
                row.add(Box.createHorizontalStrut(scaleForGUI(4)));
            }
            HudChip chip = new HudChip(dice, () -> {
                diceField.setText(dice);
                rollDice(dice);
            });
            chip.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.quick.toolTipText", dice));
            row.add(chip);
        }
        return row;
    }

    private void rollDice(final String expression) {
        diceResult.setVisible(true);
        if (!DiceExpression.isValid(expression)) {
            diceResult.setForeground(DANGER);
            diceResult.setText(wrappedHtml(getTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.invalid")));
            return;
        }
        DiceExpression.Roll roll = console.actions().rollDice(expression);
        diceResult.setForeground(TEXT_MUTED);
        diceResult.setText("<html><b><font color='" + hex(ACCENT_BRIGHT) + "' size='+2'>" + roll.total()
                                 + "</font></b>&nbsp;&nbsp;" + escape(roll.expression()) + "&nbsp;&nbsp;·&nbsp;&nbsp;"
                                 + escape(roll.describeWorking()) + "</html>");
        console.changed();
    }

    private static String wrappedHtml(final String text) {
        return "<html><div style='width:" + scaleForGUI(300) + "px'>" + escape(text) + "</div></html>";
    }

    // endregion Dice

    // region Layout helpers

    static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    static JScrollPane scroll(final JComponent content, final Color background) {
        JPanel holder = new WidthTrackingPanel();
        holder.setOpaque(true);
        holder.setBackground(background);
        holder.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, background, false);
        return scroll;
    }

    /** A scroll pane's content that always fits the pane's width, so only vertical scrolling is ever needed. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        private WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return scaleForGUI(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
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

    /**
     * A section heading with a hover-help mark beside it.
     */
    static JComponent heading(final String key, final String helpKey) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
        row.add(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, key)), BorderLayout.CENTER);
        row.add(Hud.helpMark(getTextAt(RESOURCE_BUNDLE, helpKey)), BorderLayout.EAST);
        return leftAligned(row);
    }

    /**
     * Gives a list of {@link HudCard} rows a fixed row size. Without one, Swing draws every row to measure the list,
     * which is slow for long lists.
     *
     * @param list the list
     */
    static void useFixedCardRows(final JList<?> list) {
        HudCard sample = new HudCard().show(ACCENT, false, "Sample", "Role",
              List.of(new HudCard.Tag("Tag", TEXT_MUTED)), "Sub-line", "00", "unit", false);
        list.setFixedCellHeight(sample.getPreferredSize().height);
        // Rows stretch to the list's width; this only keeps the list from measuring every row for its width.
        list.setFixedCellWidth(scaleForGUI(200));
    }

    static JLabel wrapped(final String text, final Color color) {
        JLabel label = new JLabel("<html><div style='width:" + scaleForGUI(250) + "px'>" + escape(text)
                                        + "</div></html>");
        label.setForeground(color);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

    static JPanel rightButtonRow(final @Nullable JComponent left, final JComponent... buttons) {
        JPanel row = Hud.transparentPanel(null);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        if (left != null) {
            row.add(left);
        }
        row.add(Box.createHorizontalGlue());
        for (int index = 0; index < buttons.length; index++) {
            if (index > 0) {
                row.add(Box.createHorizontalStrut(scaleForGUI(8)));
            }
            row.add(buttons[index]);
        }
        return row;
    }

    // endregion Layout helpers
}

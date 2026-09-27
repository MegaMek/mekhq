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
import static mekhq.gui.roleplay.AskPage.column;
import static mekhq.gui.roleplay.AskPage.conceptTiles;
import static mekhq.gui.roleplay.AskPage.rightButtonRow;
import static mekhq.gui.roleplay.AskPage.scroll;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.campaign.roleplay.PlotThreadLength;
import mekhq.campaign.roleplay.PlotThreadStep;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;
import mekhq.gui.baseComponents.hud.HudTrack;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;

/**
 * The console's Threads page: a board of plot threads on the left, filtered to active, concluded or all, with an inline
 * form for starting a thread; the selected thread's progress track and revealed steps on the right.
 */
class ThreadsPage implements ConsoleSection {
    private enum Filter { ACTIVE, CONCLUDED, ALL }

    private final OracleConsole console;
    private final JPanel root = new JPanel(new BorderLayout());

    private Filter filter = Filter.ACTIVE;
    private final List<HudChip> chips = new ArrayList<>();
    private final JTextField search = new JTextField();
    private final DefaultListModel<PlotThread> threadModel = new DefaultListModel<>();
    private final JList<PlotThread> threadList = new JList<>(threadModel);

    private final JPanel createForm = column();
    private final JTextField createName = new JTextField();
    private final HudSegmentedControl<PlotThreadLength> createLength = new HudSegmentedControl<>(value -> { });
    private final HudButton newThread;

    private final JPanel detail = column();
    private final JLabel title = new JLabel();
    private final JLabel status = new JLabel();
    private final JPanel renameRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
    private final JTextField renameField = new JTextField();
    private final HudTrack track = new HudTrack();
    private final HudVerdictBanner revelation = new HudVerdictBanner();
    private final JPanel steps = column();
    private final HudButton rename;
    private final HudConfirmButton delete;
    private final HudButton reveal;
    private final HudButton reroll;
    private final HudButton storyline;
    private final JLabel empty = Hud.notice("");

    /** The selected id, or {@code null} if nothing is selected. */
    private UUID selectedId;

    ThreadsPage(final OracleConsole console) {
        this.console = console;
        root.setOpaque(true);
        root.setBackground(GROUND);

        // Board
        JPanel chipRow = Hud.transparentPanel(null);
        chipRow.setLayout(new BoxLayout(chipRow, BoxLayout.X_AXIS));
        for (Filter value : Filter.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.filter." + value.name()),
                  () -> setFilter(value));
            chips.add(chip);
            chipRow.add(chip);
            chipRow.add(Box.createHorizontalStrut(scaleForGUI(6)));
        }
        Hud.styleField(search);
        search.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.search"));
        search.getDocument().addDocumentListener(onChange(this::refresh));

        threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadList.setBackground(SURFACE_DEEP);
        threadList.setCellRenderer(new ThreadRenderer());
        AskPage.useFixedCardRows(threadList);
        threadList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && threadList.getSelectedValue() != null) {
                selectedId = threadList.getSelectedValue().getId();
                revelation.setVisible(false);
                refreshDetail();
            }
        });
        JScrollPane listScroll = new JScrollPane(threadList);
        Hud.styleScroll(listScroll, SURFACE_DEEP, true);

        newThread = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.new").toUpperCase(Locale.ROOT),
              false, true);
        newThread.addActionListener(event -> showCreateForm(true));
        buildCreateForm();

        JPanel boardTop = column();
        boardTop.add(leftAligned(chipRow));
        boardTop.add(Box.createVerticalStrut(scaleForGUI(8)));
        boardTop.add(leftAligned(search));
        boardTop.add(Box.createVerticalStrut(scaleForGUI(8)));
        JPanel boardBottom = column();
        boardBottom.add(Box.createVerticalStrut(scaleForGUI(8)));
        boardBottom.add(leftAligned(rightButtonRow(null, newThread)));
        boardBottom.add(leftAligned(createForm));

        JPanel board = new JPanel(new BorderLayout());
        board.setOpaque(true);
        board.setBackground(GROUND);
        board.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        board.setPreferredSize(new Dimension(scaleForGUI(340), 0));
        board.add(boardTop, BorderLayout.NORTH);
        board.add(listScroll, BorderLayout.CENTER);
        board.add(boardBottom, BorderLayout.SOUTH);

        // Detail
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.2f, 0.12f));
        status.setForeground(TEXT_MUTED);
        status.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        Hud.styleField(renameField);
        renameField.addActionListener(event -> finishRename(true));
        HudButton saveName = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.save").toUpperCase(Locale.ROOT),
              true, true);
        saveName.addActionListener(event -> finishRename(true));
        HudButton cancelName = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cancel")
                                                   .toUpperCase(Locale.ROOT), false, true);
        cancelName.addActionListener(event -> finishRename(false));
        renameRow.add(renameField, BorderLayout.CENTER);
        renameRow.add(rightButtonRow(null, cancelName, saveName), BorderLayout.EAST);
        renameRow.setVisible(false);

        rename = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.rename").toUpperCase(Locale.ROOT), false,
              true);
        rename.addActionListener(event -> startRename());
        delete = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.delete").toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.confirmDelete").toUpperCase(Locale.ROOT), true);
        delete.addActionListener(event -> deleteSelected());
        storyline = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.readStoryline").toUpperCase(Locale.ROOT),
              false, true);
        storyline.addActionListener(event -> console.readStoryline(selectedId, null));
        reveal = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.reveal").toUpperCase(Locale.ROOT),
              true);
        reveal.addActionListener(event -> revealNext());
        reroll = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.reroll").toUpperCase(Locale.ROOT),
              false, true);
        reroll.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.reroll.toolTipText"));
        reroll.addActionListener(event -> rerollLatest());
        revelation.setVisible(false);

        JPanel header = Hud.transparentPanel(new BorderLayout());
        JPanel headerText = column();
        headerText.add(leftAligned(title));
        headerText.add(Box.createVerticalStrut(scaleForGUI(3)));
        headerText.add(leftAligned(status));
        header.add(headerText, BorderLayout.CENTER);

        detail.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        detail.add(leftAligned(header));
        detail.add(leftAligned(renameRow));
        detail.add(Box.createVerticalStrut(scaleForGUI(12)));
        detail.add(leftAligned(track));
        detail.add(Box.createVerticalStrut(scaleForGUI(12)));
        detail.add(leftAligned(rightButtonRow(null, rename, delete, storyline, reroll, reveal)));
        detail.add(Box.createVerticalStrut(scaleForGUI(12)));
        detail.add(leftAligned(revelation));
        detail.add(Box.createVerticalStrut(scaleForGUI(8)));
        detail.add(leftAligned(steps));

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(true);
        right.setBackground(SURFACE_DEEP);
        right.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        right.add(scroll(detail, SURFACE_DEEP), BorderLayout.CENTER);
        right.add(empty, BorderLayout.NORTH);

        root.add(board, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        // The console refreshes the page when it is shown, so only the chips are set up here.
        showFilter(Filter.ACTIVE);
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    /**
     * @return the id of the thread on screen, or {@code null} if none is selected
     */
    @Nullable UUID getSelectedThreadId() {
        return selectedId;
    }

    /**
     * Selects a thread, widening the filter if it is hidden.
     *
     * @param threadId the thread's id
     */
    void select(final UUID threadId) {
        selectedId = threadId;
        PlotThread thread = console.roleplay().getPlotThread(threadId);
        if (thread != null && filter == Filter.ACTIVE && thread.isComplete()) {
            setFilter(Filter.ALL);
        }
        search.setText("");
        refresh();
    }

    // region Board

    private void setFilter(final Filter value) {
        showFilter(value);
        refresh();
    }

    private void showFilter(final Filter value) {
        filter = value;
        for (int index = 0; index < chips.size(); index++) {
            chips.get(index).setActive(Filter.values()[index] == value);
        }
    }

    @Override
    public void refresh() {
        String text = search.getText().strip().toLowerCase(Locale.ROOT);
        List<PlotThread> shown = console.roleplay().getPlotThreads().stream()
                                       .filter(thread -> switch (filter) {
                                           case ACTIVE -> !thread.isComplete();
                                           case CONCLUDED -> thread.isComplete();
                                           case ALL -> true;
                                       })
                                       .filter(thread -> text.isEmpty()
                                                               || thread.getName().toLowerCase(Locale.ROOT)
                                                                        .contains(text))
                                       .toList();
        threadModel.clear();
        threadModel.addAll(shown);

        PlotThread selected = console.roleplay().getPlotThread(selectedId);
        if (selected == null || !shown.contains(selected)) {
            selected = shown.isEmpty() ? null : shown.get(0);
            selectedId = (selected == null) ? null : selected.getId();
        }
        if (selected != null) {
            threadList.setSelectedValue(selected, true);
        } else {
            threadList.clearSelection();
        }
        refreshDetail();
    }

    private static final class ThreadRenderer implements ListCellRenderer<PlotThread> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends PlotThread> list, PlotThread thread, int index,
              boolean isSelected, boolean cellHasFocus) {
            PlotThreadStep next = thread.getNextStep();
            Color ring = thread.isComplete() ? READY : (next != null && next.majorRevelation()) ? AMBER : ACCENT;
            return card.show(ring, thread.isComplete(), thread.getName(), null,
                  List.of(new Tag(shortLength(thread.getLength()), TEXT_MUTED)), describeProgress(thread),
                  thread.getRevealedSteps() + "/" + thread.getSteps().size(), null, isSelected);
        }
    }

    private static String shortLength(final PlotThreadLength length) {
        return getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.length.short." + length.name());
    }

    static String describeProgress(final PlotThread thread) {
        if (thread.isComplete()) {
            return getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.progress.concluded");
        }
        PlotThreadStep next = thread.getNextStep();
        if (next.conclusion()) {
            return getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.progress.conclusionNext");
        }
        if (next.majorRevelation()) {
            return getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.progress.flashpointNext");
        }
        int toFlashpoint = PlotThread.FLASHPOINT_INTERVAL - (thread.getRevealedSteps() % PlotThread.FLASHPOINT_INTERVAL);
        boolean nextMarkIsConclusion = thread.getRevealedSteps() + toFlashpoint >= thread.getSteps().size();
        return getFormattedTextAt(RESOURCE_BUNDLE, nextMarkIsConclusion ? "OracleConsole.threads.progress.toConclusion"
                                                           : "OracleConsole.threads.progress.toFlashpoint",
              thread.getSteps().size() - thread.getRevealedSteps(), toFlashpoint);
    }

    private void buildCreateForm() {
        createForm.setOpaque(true);
        createForm.setBackground(SURFACE);
        createForm.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_CYAN,
              scaleForGUI(1)), BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(10), scaleForGUI(10),
              scaleForGUI(10))));
        Hud.styleField(createName);
        createName.addActionListener(event -> create());
        List<Segment<PlotThreadLength>> lengths = new ArrayList<>();
        for (PlotThreadLength length : PlotThreadLength.values()) {
            lengths.add(new Segment<>(length, shortLength(length), getFormattedTextAt(RESOURCE_BUNDLE,
                  "OracleConsole.threads.length.steps", length.getSteps()), true));
        }
        createLength.setSegments(lengths);
        createLength.setSelected(PlotThreadLength.MEDIUM);

        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.cancel").toUpperCase(Locale.ROOT),
              false, true);
        cancel.addActionListener(event -> showCreateForm(false));
        HudButton create = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.create")
                                               .toUpperCase(Locale.ROOT), true, true);
        create.addActionListener(event -> create());

        createForm.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.new.eyebrow"))));
        createForm.add(Box.createVerticalStrut(scaleForGUI(6)));
        createForm.add(leftAligned(createName));
        createForm.add(Box.createVerticalStrut(scaleForGUI(6)));
        createForm.add(leftAligned(createLength));
        createForm.add(Box.createVerticalStrut(scaleForGUI(6)));
        createForm.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.new.hint"))));
        createForm.add(Box.createVerticalStrut(scaleForGUI(8)));
        createForm.add(leftAligned(rightButtonRow(null, cancel, create)));
        createForm.setVisible(false);
    }

    private void showCreateForm(final boolean show) {
        createForm.setVisible(show);
        newThread.setVisible(!show);
        createName.setText("");
        if (show) {
            createName.requestFocusInWindow();
        }
        root.revalidate();
    }

    private void create() {
        String name = createName.getText().strip();
        PlotThreadLength length = createLength.getSelected();
        if (name.isEmpty() || length == null) {
            createName.requestFocusInWindow();
            return;
        }
        PlotThread thread = console.actions().createThread(name, length);
        showCreateForm(false);
        selectedId = thread.getId();
        if (filter == Filter.CONCLUDED) {
            setFilter(Filter.ACTIVE);
        }
        console.changed();
    }

    // endregion Board

    // region Detail

    private void refreshDetail() {
        PlotThread thread = console.roleplay().getPlotThread(selectedId);
        boolean hasThread = thread != null;
        detail.setVisible(hasThread);
        empty.setVisible(!hasThread);
        empty.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(60), scaleForGUI(20), 0, scaleForGUI(20)));
        empty.setText(getTextAt(RESOURCE_BUNDLE, console.roleplay().getPlotThreads().isEmpty()
                                                       ? "OracleConsole.threads.empty"
                                                       : "OracleConsole.threads.noneShown"));
        if (!hasThread) {
            return;
        }

        title.setText(thread.getName().toUpperCase(Locale.ROOT));
        status.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.status",
              thread.getLength().getLabel(), thread.getRevealedSteps(), thread.getSteps().size(),
              describeProgress(thread)));
        track.setTrack(thread.getSteps().size(), thread.getRevealedSteps(),
              number -> PlotThread.isFlashpoint(number, thread.getLength()), number -> tooltipFor(thread, number),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.flashpoint"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.conclusion"));
        reveal.setArmed(!thread.isComplete());
        reroll.setVisible(thread.getRevealedSteps() > 0);
        reveal.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.reveal.toolTipText"));

        steps.removeAll();
        List<PlotThreadStep> revealed = thread.getRevealed();
        if (revealed.isEmpty()) {
            steps.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.nothingRevealed"))));
        }
        for (int index = revealed.size() - 1; index >= 0; index--) {
            PlotThreadStep step = revealed.get(index);
            steps.add(leftAligned(Hud.sectionHeading(stepHeading(thread, step))));
            steps.add(Box.createVerticalStrut(scaleForGUI(6)));
            steps.add(leftAligned(conceptTiles(step.concepts(), 2)));
            steps.add(Box.createVerticalStrut(scaleForGUI(12)));
        }
        detail.revalidate();
        detail.repaint();
    }

    private String stepHeading(final PlotThread thread, final PlotThreadStep step) {
        String key = step.conclusion() ? "OracleConsole.threads.step.conclusion"
                           : step.majorRevelation() ? "OracleConsole.threads.step.major"
                                   : "OracleConsole.threads.step";
        String heading = getFormattedTextAt(RESOURCE_BUNDLE, key, step.number());
        LocalDate date = thread.getRevealDate(step.number());
        return (date == null) ? heading : heading + "  ·  " + OracleConsole.formatDate(date);
    }

    private String tooltipFor(final PlotThread thread, final int number) {
        PlotThreadStep step = thread.getSteps().get(number - 1);
        String concepts = step.concepts().stream()
                                .map(concept -> OracleConsole.escape(concept.table().getTableLabel() + ": "
                                                                           + (concept.meaning() == null ? ""
                                                                                    : concept.meaning())))
                                .collect(Collectors.joining("<br>"));
        return "<html><b>" + OracleConsole.escape(stepHeading(thread, step)) + "</b><br>" + concepts + "</html>";
    }

    private void revealNext() {
        PlotThread thread = console.roleplay().getPlotThread(selectedId);
        if (thread == null) {
            return;
        }
        PlotThreadStep step = console.actions().revealNextStep(thread);
        if (step != null && (step.majorRevelation() || step.conclusion())) {
            revelation.setVerdict(getTextAt(RESOURCE_BUNDLE, step.conclusion() ? "OracleConsole.threads.banner.conclusion"
                                                                    : "OracleConsole.threads.banner.major"),
                  getFormattedTextAt(RESOURCE_BUNDLE, step.conclusion() ? "OracleConsole.threads.banner.conclusion.reason"
                                                            : "OracleConsole.threads.banner.major.reason",
                        OracleConsole.escape(thread.getName()), step.number()),
                  getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.threads.banner.badge", step.number()),
                  step.conclusion() ? READY : AMBER);
            revelation.setVisible(true);
        } else {
            revelation.setVisible(false);
        }
        console.changed();
    }

    private void rerollLatest() {
        PlotThread thread = console.roleplay().getPlotThread(selectedId);
        if (thread != null && console.actions().rerollLatestStep(thread) != null) {
            revelation.setVisible(false);
            console.changed();
        }
    }

    private void startRename() {
        PlotThread thread = console.roleplay().getPlotThread(selectedId);
        if (thread == null) {
            return;
        }
        renameField.setText(thread.getName());
        renameRow.setVisible(true);
        renameField.requestFocusInWindow();
        renameField.selectAll();
        detail.revalidate();
    }

    private void finishRename(final boolean save) {
        PlotThread thread = console.roleplay().getPlotThread(selectedId);
        if (save && thread != null && !renameField.getText().isBlank()) {
            thread.setName(renameField.getText().strip());
        }
        renameRow.setVisible(false);
        console.changed();
    }

    private void deleteSelected() {
        PlotThread thread = console.roleplay().getPlotThread(selectedId);
        if (thread != null) {
            console.actions().deleteThread(thread);
            selectedId = null;
            console.changed();
        }
    }

    // endregion Detail

    static DocumentListener onChange(final Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
    }
}

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
import static mekhq.gui.roleplay.AskPage.rightButtonRow;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.gui.roleplay.OracleConsole.escape;
import static mekhq.gui.roleplay.OracleConsole.formatDate;
import static mekhq.gui.roleplay.ThreadsPage.onChange;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.JournalEntryType;
import mekhq.campaign.roleplay.JournalExporter;
import mekhq.campaign.roleplay.JournalExporter.Format;
import mekhq.campaign.roleplay.JournalFilter;
import mekhq.campaign.roleplay.JournalText;
import mekhq.campaign.roleplay.OracleAnswer;
import mekhq.campaign.roleplay.OracleCharacter;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.baseComponents.hud.HudDateField;
import mekhq.gui.baseComponents.hud.HudRichTextEditor;

/**
 * The console's Journal page: every note and Oracle record in one list, grouped by month, narrowed by the filter column
 * (search, type, thread, cast member and dates). The right side reads the selected entry; a note opens straight into
 * the rich-text editor. The filtered journal can be exported as Markdown or plain text.
 */
class JournalPage implements ConsoleSection {
    private static final MMLogger LOGGER = MMLogger.create(JournalPage.class);

    /** The type chips: each stands for a group of entry types. */
    private enum TypeGroup {
        ALL(EnumSet.noneOf(JournalEntryType.class)),
        NOTES(EnumSet.of(JournalEntryType.NOTE)),
        ORACLE(EnumSet.of(JournalEntryType.FATE_CHART, JournalEntryType.RANDOM_EVENT, JournalEntryType.CONCEPTS)),
        THREADS(EnumSet.of(JournalEntryType.THREAD)),
        CHECKS(EnumSet.of(JournalEntryType.CHECK));

        private final Set<JournalEntryType> types;

        TypeGroup(Set<JournalEntryType> types) {
            this.types = types;
        }
    }

    private final OracleConsole console;
    private final JPanel root = new JPanel(new BorderLayout());

    // Filters
    private final JTextField search = new JTextField();
    private TypeGroup typeGroup = TypeGroup.ALL;
    private final Map<TypeGroup, HudChip> typeChips = new LinkedHashMap<>();
    private final JComboBox<Object> threadFilter = new JComboBox<>();
    private final JComboBox<Object> castFilter = new JComboBox<>();
    private final HudDateField from;
    private final HudDateField to;
    private boolean oldestFirst;
    private final HudChip newestChip;
    private final HudChip oldestChip;
    private boolean updatingFilters;

    // List
    private final DefaultListModel<Object> listModel = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(listModel);
    private final JLabel matches = Hud.hint("");

    // Reader
    private final JPanel reader = column();
    private final JLabel readerTitle = new JLabel();
    private final JLabel readerTags = new JLabel();
    private final HudButton tagsButton;
    private final HudRichTextEditor editor = new HudRichTextEditor();
    private final JLabel status = Hud.hint("");
    private final HudConfirmButton delete;
    private final JLabel empty = Hud.notice("");
    /** The entry in the reader, or {@code null} if none. */
    private JournalEntry shown;

    JournalPage(final OracleConsole console) {
        this.console = console;
        root.setOpaque(true);
        root.setBackground(GROUND);

        // Filter column
        Hud.styleField(search);
        search.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.search"));
        search.getDocument().addDocumentListener(onChange(this::filtersChanged));
        JPanel chipRow = Hud.transparentPanel(null);
        chipRow.setLayout(new BoxLayout(chipRow, BoxLayout.X_AXIS));
        for (TypeGroup group : TypeGroup.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.type." + group.name()),
                  () -> setTypeGroup(group));
            typeChips.put(group, chip);
            chipRow.add(chip);
            chipRow.add(Box.createHorizontalStrut(scaleForGUI(4)));
        }
        styleFilterCombo(threadFilter);
        styleFilterCombo(castFilter);
        from = new HudDateField(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.datePlaceholder"),
              this::filtersChanged);
        to = new HudDateField(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.datePlaceholder"),
              this::filtersChanged);
        from.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.date.toolTipText"));
        to.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.date.toolTipText"));
        newestChip = new HudChip(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.newest"), () -> setOldestFirst(false));
        oldestChip = new HudChip(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.oldest"), () -> setOldestFirst(true));
        JPanel sortRow = Hud.transparentPanel(null);
        sortRow.setLayout(new BoxLayout(sortRow, BoxLayout.X_AXIS));
        sortRow.add(newestChip);
        sortRow.add(Box.createHorizontalStrut(scaleForGUI(4)));
        sortRow.add(oldestChip);
        sortRow.add(Box.createHorizontalGlue());
        sortRow.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.clear"), this::clearFilters));

        JPanel filters = column();
        filters.add(leftAligned(search));
        filters.add(Box.createVerticalStrut(scaleForGUI(8)));
        filters.add(leftAligned(chipRow));
        filters.add(Box.createVerticalStrut(scaleForGUI(8)));
        filters.add(leftAligned(pair("OracleConsole.journal.thread", threadFilter, "OracleConsole.journal.cast",
              castFilter)));
        filters.add(Box.createVerticalStrut(scaleForGUI(6)));
        filters.add(leftAligned(pair("OracleConsole.journal.from", from, "OracleConsole.journal.to", to)));
        filters.add(Box.createVerticalStrut(scaleForGUI(8)));
        filters.add(leftAligned(sortRow));
        filters.add(Box.createVerticalStrut(scaleForGUI(8)));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.setCellRenderer(new EntryRenderer());
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                if (list.getSelectedValue() instanceof String) {
                    list.clearSelection();
                } else {
                    showEntry((JournalEntry) list.getSelectedValue());
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, true);

        JPanel board = new JPanel(new BorderLayout());
        board.setOpaque(true);
        board.setBackground(GROUND);
        board.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        board.setPreferredSize(new Dimension(scaleForGUI(360), 0));
        board.add(filters, BorderLayout.NORTH);
        board.add(listScroll, BorderLayout.CENTER);
        JPanel boardFoot = column();
        boardFoot.add(Box.createVerticalStrut(scaleForGUI(6)));
        boardFoot.add(leftAligned(matches));
        board.add(boardFoot, BorderLayout.SOUTH);

        // Reader
        readerTitle.setForeground(ACCENT_BRIGHT);
        readerTitle.setFont(hudFont(Font.BOLD, 1.05f, 0.12f));
        readerTags.setForeground(TEXT_MUTED);
        readerTags.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        tagsButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.tags").toUpperCase(Locale.ROOT),
              false, true);
        tagsButton.addActionListener(event -> showTagMenu());
        JPanel readerHead = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        JPanel headText = column();
        headText.add(leftAligned(readerTitle));
        headText.add(Box.createVerticalStrut(scaleForGUI(3)));
        headText.add(leftAligned(readerTags));
        readerHead.add(headText, BorderLayout.CENTER);
        JPanel tagsHolder = Hud.transparentPanel(new BorderLayout());
        tagsHolder.add(tagsButton, BorderLayout.NORTH);
        readerHead.add(tagsHolder, BorderLayout.EAST);

        editor.addFormattingButtons(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.format.bold"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.format.italic"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.format.underline"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.format.heading"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.format.headingText"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.format.bullets"));
        HudButton[] mention = new HudButton[1];
        mention[0] = editor.addToolbarButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.mention"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.mention.toolTipText"),
              () -> showMentionMenu(mention[0]));
        editor.addToolbarButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.quote"),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.quote.toolTipText"), this::quoteLastAnswer);
        editor.addChangeListener(this::saveEditor);
        editor.setPreferredSize(new Dimension(scaleForGUI(400), scaleForGUI(360)));

        delete = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.delete").toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.confirmDelete").toUpperCase(Locale.ROOT), true);
        delete.addActionListener(event -> deleteShown());
        HudButton export = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export")
                                               .toUpperCase(Locale.ROOT), false, true);
        export.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.toolTipText"));
        export.addActionListener(event -> export());
        HudButton newNote = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.new")
                                                .toUpperCase(Locale.ROOT), true, true);
        newNote.addActionListener(event -> newNote(selectedFilterThread(), selectedFilterCast(), null));

        reader.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        reader.add(leftAligned(readerHead));
        reader.add(Box.createVerticalStrut(scaleForGUI(10)));
        reader.add(leftAligned(editor));

        JPanel actions = Hud.transparentPanel(new BorderLayout());
        actions.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(16), scaleForGUI(12),
              scaleForGUI(16)));
        actions.add(rightButtonRow(status, delete, export, newNote), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(true);
        right.setBackground(SURFACE_DEEP);
        right.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        JPanel readerHolder = new JPanel(new BorderLayout());
        readerHolder.setOpaque(false);
        readerHolder.add(empty, BorderLayout.NORTH);
        readerHolder.add(reader, BorderLayout.CENTER);
        right.add(readerHolder, BorderLayout.CENTER);
        right.add(actions, BorderLayout.SOUTH);

        root.add(board, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);

        // The console refreshes the page when it is shown, so only the chips are set up here.
        updatingFilters = true;
        setTypeGroup(TypeGroup.ALL);
        setOldestFirst(false);
        updatingFilters = false;
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    // region Filters

    private JPanel pair(final String firstLabel, final JComponent first, final String secondLabel,
          final JComponent second) {
        JPanel pair = new JPanel(new GridLayout(1, 2, scaleForGUI(6), 0));
        pair.setOpaque(false);
        pair.add(labelled(firstLabel, first));
        pair.add(labelled(secondLabel, second));
        return pair;
    }

    private static JPanel labelled(final String key, final JComponent control) {
        JPanel panel = column();
        panel.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, key))));
        panel.add(Box.createVerticalStrut(scaleForGUI(2)));
        panel.add(leftAligned(control));
        return panel;
    }

    private void styleFilterCombo(final JComboBox<Object> comboBox) {
        Hud.styleComboBox(comboBox);
        ListCellRenderer<Object> hud = Hud.comboRenderer();
        comboBox.setRenderer((source, value, index, isSelected, cellHasFocus) -> hud.getListCellRendererComponent(
              source, (value == null) ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.any") : value, index,
              isSelected, cellHasFocus));
        comboBox.addActionListener(event -> filtersChanged());
    }

    private void setTypeGroup(final TypeGroup group) {
        typeGroup = group;
        typeChips.forEach((value, chip) -> chip.setActive(value == group));
        filtersChanged();
    }

    private void setOldestFirst(final boolean value) {
        oldestFirst = value;
        newestChip.setActive(!value);
        oldestChip.setActive(value);
        filtersChanged();
    }

    private void clearFilters() {
        updatingFilters = true;
        search.setText("");
        from.setText("");
        to.setText("");
        threadFilter.setSelectedItem(null);
        castFilter.setSelectedItem(null);
        updatingFilters = false;
        setTypeGroup(TypeGroup.ALL);
    }

    private @Nullable UUID selectedFilterThread() {
        return (threadFilter.getSelectedItem() instanceof PlotThread thread) ? thread.getId() : null;
    }

    private @Nullable UUID selectedFilterCast() {
        return (castFilter.getSelectedItem() instanceof OracleCharacter character) ? character.getId() : null;
    }

    private JournalFilter currentFilter() {
        return new JournalFilter(search.getText(), from.getDate(), to.getDate(), typeGroup.types,
              selectedFilterThread(), selectedFilterCast());
    }

    private void filtersChanged() {
        if (!updatingFilters) {
            refreshList(shown);
        }
    }

    /**
     * Shows one storyline from start to finish: clears the other filters, filters to the thread or character, and
     * sorts oldest first.
     *
     * @param threadId    the thread, or {@code null}
     * @param characterId the character, or {@code null}
     */
    void filterToStoryline(final @Nullable UUID threadId, final @Nullable UUID characterId) {
        refreshChoices();
        updatingFilters = true;
        search.setText("");
        from.setText("");
        to.setText("");
        threadFilter.setSelectedItem(console.roleplay().getPlotThread(threadId));
        castFilter.setSelectedItem(console.roleplay().getCharacter(characterId));
        updatingFilters = false;
        setOldestFirst(true);
        setTypeGroup(TypeGroup.ALL);
    }

    /**
     * Rebuilds the thread and cast filter choices, keeping the current selections.
     */
    private void refreshChoices() {
        updatingFilters = true;
        Object thread = threadFilter.getSelectedItem();
        Object cast = castFilter.getSelectedItem();
        DefaultComboBoxModel<Object> threads = new DefaultComboBoxModel<>();
        threads.addElement(null);
        console.roleplay().getPlotThreads().forEach(threads::addElement);
        threadFilter.setModel(threads);
        threadFilter.setSelectedItem(console.roleplay().getPlotThreads().contains(thread) ? thread : null);
        DefaultComboBoxModel<Object> characters = new DefaultComboBoxModel<>();
        characters.addElement(null);
        console.roleplay().getCharacters().forEach(characters::addElement);
        castFilter.setModel(characters);
        castFilter.setSelectedItem(console.roleplay().getCharacters().contains(cast) ? cast : null);
        updatingFilters = false;
    }

    // endregion Filters

    // region List

    @Override
    public void refresh() {
        refreshChoices();
        refreshList(shown);
    }

    private void refreshList(final @Nullable JournalEntry toSelect) {
        List<JournalEntry> entries = new ArrayList<>(console.roleplay().getTimeline(currentFilter()));
        if (!oldestFirst) {
            Collections.reverse(entries);
        }
        listModel.clear();
        String month = null;
        for (JournalEntry entry : entries) {
            String entryMonth = entry.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " "
                                      + entry.getDate().getYear();
            if (!entryMonth.equals(month)) {
                listModel.addElement(entryMonth);
                month = entryMonth;
            }
            listModel.addElement(entry);
        }

        int total = console.roleplay().getJournal().size() + console.roleplay().getOracleLog().size();
        matches.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.matches", entries.size(), total));

        JournalEntry selected = (toSelect != null && entries.contains(toSelect)) ? toSelect
                                      : entries.isEmpty() ? null : entries.get(0);
        if (selected != null) {
            list.setSelectedValue(selected, true);
        } else {
            list.clearSelection();
        }
        showEntry(selected);
    }

    /**
     * Draws one journal entry as a card: a type-coloured ring, a title, the type and tags, and the date.
     *
     * @param entry      the entry
     * @param isSelected {@code true} if it is selected
     *
     * @return the card
     */
    static HudCard entryCard(final JournalEntry entry, final boolean isSelected) {
        return new HudCard().show(colorFor(entry.getType()), !entry.isNote(), titleOf(entry),
              entry.getType().getLabel(), List.of(), describeBody(entry), formatDate(entry.getDate()), null,
              isSelected);
    }

    private static Color colorFor(final JournalEntryType type) {
        return switch (type) {
            case NOTE -> ACCENT_BRIGHT;
            case FATE_CHART -> ACCENT;
            case RANDOM_EVENT -> AMBER;
            case CONCEPTS -> TEXT_MUTED;
            case THREAD -> READY;
            case CHECK -> TEXT;
        };
    }

    private static String titleOf(final JournalEntry entry) {
        OracleAnswer answer = entry.getAnswer();
        if (answer != null) {
            return answer.answer().getLabel() + (answer.question().isBlank() ? "" : " · " + answer.question());
        }
        if (entry.getCheck() != null) {
            return ChecksPage.summarize(entry.getCheck());
        }
        String first = entry.getPlainText().strip().lines().findFirst().orElse("");
        return first.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.emptyNote") : first;
    }

    private static String describeBody(final JournalEntry entry) {
        if (entry.getCheck() != null) {
            return ChecksPage.describeSides(entry.getCheck());
        }
        List<String> lines = entry.getPlainText().strip().lines().skip(1).filter(line -> !line.isBlank()).toList();
        return lines.isEmpty() ? "" : lines.get(0).strip();
    }

    private final class EntryRenderer implements ListCellRenderer<Object> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<?> source, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            if (value instanceof String month) {
                return card.sectionHeader(month);
            }
            JournalEntry entry = (JournalEntry) value;
            // The ring's colour shows the type, so the row leaves the width to the title.
            return card.show(colorFor(entry.getType()), !entry.isNote(), titleOf(entry), null, List.of(),
                  describeTags(entry), String.format("%02d-%02d", entry.getDate().getMonthValue(),
                        entry.getDate().getDayOfMonth()), null, isSelected);
        }
    }

    // endregion List

    // region Reader

    private void showEntry(final @Nullable JournalEntry entry) {
        if (entry != null && entry == shown) {
            readerTags.setText(tagLine(entry));
            return;
        }
        shown = entry;
        delete.reset();
        reader.setVisible(entry != null);
        empty.setVisible(entry == null);
        empty.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(60), scaleForGUI(20), 0, scaleForGUI(20)));
        empty.setText(getTextAt(RESOURCE_BUNDLE, currentFilter().isActive() ? "OracleConsole.journal.noMatches"
                                                       : "OracleConsole.journal.empty"));
        status.setText("");
        if (entry == null) {
            return;
        }

        readerTitle.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.readerTitle",
              formatDate(entry.getDate()), entry.getType().getLabel()).toUpperCase(Locale.ROOT));
        readerTags.setText(tagLine(entry));
        editor.setHtml(entry.isNote() ? entry.getText() : JournalText.plainToHtml(entry.getText()));
        editor.setEditable(entry.isNote());
        editor.setToolbarVisible(entry.isNote());
        delete.setVisible(entry.isNote());
        reader.revalidate();
        reader.repaint();
    }

    private String tagLine(final JournalEntry entry) {
        String tags = describeTags(entry);
        return tags.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.noTags")
                     : getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.tagLine", tags);
    }

    private String describeTags(final JournalEntry entry) {
        List<String> names = new ArrayList<>();
        for (UUID id : entry.getThreads()) {
            String name = console.roleplay().getPlotThreadName(id);
            names.add(name == null ? getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.deletedThread") : name);
        }
        for (UUID id : entry.getCharacters()) {
            String name = console.roleplay().getCharacterName(id);
            if (name != null) {
                names.add(name);
            }
        }
        return String.join(", ", names);
    }

    private void saveEditor() {
        if (shown != null && shown.isNote()) {
            shown.setText(editor.getHtml());
            status.setText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.saved"));
            list.repaint();
        }
    }

    /**
     * Starts a new note dated today, tagged with a thread and a character and optionally quoting an answer.
     *
     * @param threadId    the thread to tag, or {@code null}
     * @param characterId the character to tag, or {@code null}
     * @param quote       an answer to quote at the top, or {@code null}
     */
    void newNote(final @Nullable UUID threadId, final @Nullable UUID characterId, final @Nullable OracleAnswer quote) {
        JournalEntry note = JournalEntry.newNote(console.campaign().getLocalDate());
        if (threadId != null) {
            note.getThreads().add(threadId);
        }
        if (characterId != null) {
            note.getCharacters().add(characterId);
        }
        if (quote != null) {
            note.setText("<html><body><blockquote>" + quoteHtml(quote) + "</blockquote><p></p></body></html>");
        }
        console.roleplay().getJournal().add(note);
        if (!currentFilter().matches(note)) {
            clearFilters();
        }
        refreshList(note);
        console.changed();
        editor.getEditorPane().requestFocusInWindow();
    }

    private static String quoteHtml(final OracleAnswer answer) {
        String text = getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.quoteText",
              answer.question().isBlank() ? answer.odds().getLabel() : answer.question(), answer.answer().getLabel(),
              answer.roll(), answer.odds().getLabel(), answer.chaos());
        return escape(text);
    }

    private void quoteLastAnswer() {
        JournalEntry latest = console.latestAnswer();
        if (latest == null) {
            status.setText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.quote.none"));
            return;
        }
        editor.insertQuote(quoteHtml(latest.getAnswer()));
        if (shown != null) {
            shown.getThreads().addAll(latest.getThreads());
            shown.getCharacters().addAll(latest.getCharacters());
            readerTags.setText(tagLine(shown));
        }
    }

    private void showMentionMenu(final JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();
        List<OracleCharacter> cast = console.roleplay().getActiveCharacters();
        if (cast.isEmpty()) {
            JMenuItem none = new JMenuItem(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.mention.none"));
            none.setEnabled(false);
            menu.add(none);
        }
        for (OracleCharacter character : cast) {
            JMenuItem item = new JMenuItem(character.getName());
            item.addActionListener(event -> {
                editor.insertText(character.getName());
                if (shown != null) {
                    shown.tagCharacter(character);
                    readerTags.setText(tagLine(shown));
                }
            });
            menu.add(item);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void showTagMenu() {
        if (shown == null) {
            return;
        }
        JournalEntry entry = shown;
        JPanel panel = new JPanel(new GridLayout(1, 2, scaleForGUI(14), 0));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12)));

        JPanel threads = column();
        threads.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.tags.threads"))));
        for (PlotThread thread : console.roleplay().getPlotThreads()) {
            threads.add(leftAligned(tagBox(thread.getName(), entry.getThreads().contains(thread.getId()), selected -> {
                if (selected) {
                    entry.getThreads().add(thread.getId());
                } else {
                    entry.getThreads().remove(thread.getId());
                }
            })));
        }
        JPanel cast = column();
        cast.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.tags.cast"))));
        for (OracleCharacter character : console.roleplay().getCharacters()) {
            if (!character.isActive() && !entry.getCharacters().contains(character.getId())) {
                continue;
            }
            cast.add(leftAligned(tagBox(character.getName(), entry.getCharacters().contains(character.getId()),
                  selected -> {
                      if (selected) {
                          entry.getCharacters().add(character.getId());
                      } else {
                          entry.getCharacters().remove(character.getId());
                      }
                  })));
        }
        panel.add(threads);
        panel.add(cast);

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)));
        popup.add(panel);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
                readerTags.setText(tagLine(entry));
                list.repaint();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
            }
        });
        popup.show(tagsButton, 0, tagsButton.getHeight());
    }

    private static HudCheckBox tagBox(final String label, final boolean selected,
          final Consumer<Boolean> onChange) {
        HudCheckBox box = new HudCheckBox(label);
        box.setSelected(selected);
        box.addActionListener(event -> onChange.accept(box.isSelected()));
        return box;
    }

    private void deleteShown() {
        if (shown != null && shown.isNote()) {
            console.roleplay().getJournal().remove(shown);
            shown = null;
            refreshList(null);
            console.changed();
        }
    }

    // endregion Reader

    // region Export

    private void export() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter markdown = new FileNameExtensionFilter(
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.markdown"), "md", "markdown");
        FileNameExtensionFilter text = new FileNameExtensionFilter(
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.text"), "txt");
        chooser.addChoosableFileFilter(markdown);
        chooser.addChoosableFileFilter(text);
        chooser.setFileFilter(markdown);
        chooser.setSelectedFile(new File(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.defaultName")
                                               + ".md"));
        if (chooser.showSaveDialog(root) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        Format format;
        if (file.getName().contains(".")) {
            format = Format.forFileName(file.getName());
        } else {
            format = (chooser.getFileFilter() == text) ? Format.TEXT : Format.MARKDOWN;
            file = new File(file.getParentFile(), file.getName() + '.' + format.getExtension());
        }

        JournalFilter filter = currentFilter();
        List<JournalEntry> entries = console.roleplay().getTimeline(filter);
        String document = JournalExporter.export(entries, format,
              getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.title"), describeFilter(filter),
              console.roleplay()::getPlotThreadName, console.roleplay()::getCharacterName, OracleConsole::formatDate);
        try {
            Files.writeString(file.toPath(), document, StandardCharsets.UTF_8);
            status.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.done", entries.size(),
                  file.getName()));
        } catch (Exception ex) {
            LOGGER.error("Failed to export the journal to {}", file, ex);
            status.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.export.failed",
                  ex.getMessage()));
        }
    }

    private @Nullable String describeFilter(final JournalFilter filter) {
        if (!filter.isActive()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (filter.text() != null && !filter.text().isBlank()) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.summary.text", filter.text().strip()));
        }
        if (!filter.types().isEmpty()) {
            parts.add(getTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.type." + typeGroup.name()));
        }
        if (filter.thread() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.summary.thread",
                  console.roleplay().getPlotThreadName(filter.thread())));
        }
        if (filter.character() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.summary.cast",
                  console.roleplay().getCharacterName(filter.character())));
        }
        LocalDate start = filter.from();
        if (start != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.summary.from", formatDate(start)));
        }
        LocalDate end = filter.to();
        if (end != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.journal.summary.to", formatDate(end)));
        }
        return String.join(", ", parts);
    }

    // endregion Export
}

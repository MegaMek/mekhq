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
package mekhq.gui.dialog.roleplay;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.JournalEntryType;
import mekhq.campaign.roleplay.JournalExporter;
import mekhq.campaign.roleplay.JournalExporter.Format;
import mekhq.campaign.roleplay.JournalFilter;
import mekhq.campaign.roleplay.JournalText;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.campaign.roleplay.Roleplay;

/**
 * The roleplay journal. A shared filter bar (search text, date range, entry type, plot thread and character) narrows
 * all three tabs:
 * <ul>
 *     <li><b>Journal</b>: the player's own rich-text notes, which can be tagged with threads and characters.</li>
 *     <li><b>Oracle Log</b>: the Oracle results recorded automatically, newest first.</li>
 *     <li><b>Timeline</b>: notes and Oracle results together, oldest first, so filtering by a thread or character
 *     reads that storyline start to finish.</li>
 * </ul>
 * The Timeline, as filtered, can be exported to a Markdown or plain-text file.
 */
public class JournalDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(JournalDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";
    private static final int PREVIEW_LENGTH = 40;

    private final Roleplay roleplay;
    private final LocalDate today;

    // Filters
    private JTextField txtSearch;
    private JComboBox<Object> cboType;
    private JComboBox<Object> cboThread;
    private JComboBox<Object> cboCharacter;
    private JTextField txtFrom;
    private JTextField txtTo;
    private boolean updatingFilters;

    // Journal tab
    private DefaultListModel<JournalEntry> entryModel;
    private JList<JournalEntry> lstEntries;
    private JLabel lblEntryDate;
    private JLabel lblTags;
    private JButton btnEditTags;
    private JButton btnDelete;
    private JEditorPane txtEntry;
    private final List<JButton> formatButtons = new ArrayList<>();
    private final DocumentListener editorListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent event) {
            saveEditor();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            saveEditor();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            saveEditor();
        }
    };
    private Document listenedDocument;
    private boolean updatingEditor;
    /** The note loaded in the editor, or {@code null} if none. */
    private JournalEntry currentEditorEntry;

    // Read-only tabs
    private JEditorPane txtOracleLog;
    private JEditorPane txtTimeline;

    public JournalDialog(final JDialog owner, final Roleplay roleplay, final LocalDate today) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "JournalDialog.title"), false);
        this.roleplay = roleplay;
        this.today = today;
        initialize();
        setSize(new Dimension(980, 640));
        setLocationRelativeTo(owner);
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tab.journal"), createJournalPanel());
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tab.oracleLog"), createOracleLogPanel());
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tab.timeline"), createTimelinePanel());

        JPanel pnlButtons = new JPanel(new BorderLayout());
        JButton btnExport = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.export"));
        btnExport.setToolTipText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.export.toolTipText"));
        btnExport.addActionListener(event -> export());
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.close"));
        btnClose.addActionListener(event -> dispose());
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlLeft.add(btnExport);
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlRight.add(btnClose);
        pnlButtons.add(pnlLeft, BorderLayout.WEST);
        pnlButtons.add(pnlRight, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createFilterPanel(), BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);

        // The Oracle stays usable while the journal is open, so pick up new results, threads and characters
        // whenever the journal comes back into focus.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent event) {
                refreshFilterChoices();
                refreshAll();
            }
        });

        refreshFilterChoices();
        refreshAll();
        // Open on the newest note, ready to read or continue.
        if (!roleplay.getJournal().isEmpty()) {
            refreshEntries(roleplay.getJournal().get(roleplay.getJournal().size() - 1));
        }
    }

    private static String formatDate(final LocalDate date) {
        return MekHQ.getMHQOptions().getDisplayFormattedDate(date);
    }

    // region Filters
    private JPanel createFilterPanel() {
        JPanel pnlFilters = new JPanel(new GridBagLayout());
        pnlFilters.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 4, 2, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        txtSearch = new JTextField(16);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshAll();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshAll();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshAll();
            }
        });

        cboType = new JComboBox<>();
        cboThread = new JComboBox<>();
        cboCharacter = new JComboBox<>();
        for (JComboBox<Object> comboBox : List.of(cboType, cboThread, cboCharacter)) {
            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                      boolean isSelected, boolean cellHasFocus) {
                    Object display = (value == null) ? getTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.any") : value;
                    return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
                }
            });
            comboBox.addActionListener(event -> {
                if (!updatingFilters) {
                    refreshAll();
                }
            });
        }
        cboType.addItem(null);
        for (JournalEntryType type : JournalEntryType.values()) {
            cboType.addItem(type);
        }

        txtFrom = createDateField();
        txtTo = createDateField();

        JButton btnClear = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.clear"));
        btnClear.addActionListener(event -> clearFilters());

        addFilter(pnlFilters, constraints, 0, 0, "JournalDialog.filter.search", txtSearch);
        addFilter(pnlFilters, constraints, 2, 0, "JournalDialog.filter.type", cboType);
        addFilter(pnlFilters, constraints, 4, 0, "JournalDialog.filter.from", txtFrom);
        addFilter(pnlFilters, constraints, 0, 1, "JournalDialog.filter.thread", cboThread);
        addFilter(pnlFilters, constraints, 2, 1, "JournalDialog.filter.character", cboCharacter);
        addFilter(pnlFilters, constraints, 4, 1, "JournalDialog.filter.to", txtTo);

        constraints.gridx = 6;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        pnlFilters.add(btnClear, constraints);
        return pnlFilters;
    }

    private void addFilter(final JPanel panel, final GridBagConstraints constraints, final int x, final int y,
          final String labelKey, final Component control) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = 0;
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey)), constraints);
        constraints.gridx = x + 1;
        constraints.weightx = 1;
        panel.add(control, constraints);
    }

    private JTextField createDateField() {
        JTextField field = new JTextField(9);
        field.setToolTipText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.date.toolTipText"));
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshAll();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshAll();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshAll();
            }
        });
        return field;
    }

    /**
     * Rebuilds the thread and character filter choices from the campaign, keeping the current selections.
     */
    private void refreshFilterChoices() {
        updatingFilters = true;
        Object selectedThread = cboThread.getSelectedItem();
        Object selectedCharacter = cboCharacter.getSelectedItem();

        cboThread.removeAllItems();
        cboThread.addItem(null);
        for (PlotThread thread : roleplay.getPlotThreads()) {
            cboThread.addItem(thread);
        }
        cboThread.setSelectedItem(selectedThread instanceof PlotThread thread
                                        && roleplay.getPlotThreads().contains(thread) ? thread : null);

        cboCharacter.removeAllItems();
        cboCharacter.addItem(null);
        for (String character : roleplay.getCharacters()) {
            cboCharacter.addItem(character);
        }
        cboCharacter.setSelectedItem(roleplay.getCharacters().contains(selectedCharacter) ? selectedCharacter : null);
        updatingFilters = false;
    }

    private void clearFilters() {
        updatingFilters = true;
        txtSearch.setText("");
        txtFrom.setText("");
        txtTo.setText("");
        cboType.setSelectedItem(null);
        cboThread.setSelectedItem(null);
        cboCharacter.setSelectedItem(null);
        updatingFilters = false;
        refreshAll();
    }

    /**
     * @return the filter described by the filter bar; an unreadable date is ignored and its field is marked
     */
    private JournalFilter currentFilter() {
        PlotThread thread = (cboThread.getSelectedItem() instanceof PlotThread selected) ? selected : null;
        return new JournalFilter(txtSearch.getText(), parseDate(txtFrom), parseDate(txtTo),
              (JournalEntryType) cboType.getSelectedItem(), thread == null ? null : thread.getId(),
              (String) cboCharacter.getSelectedItem());
    }

    private @Nullable LocalDate parseDate(final JTextField field) {
        String text = field.getText().strip();
        Color normal = UIManager.getColor("TextField.foreground");
        if (text.isEmpty()) {
            field.setForeground(normal);
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(text);
            field.setForeground(normal);
            return date;
        } catch (DateTimeParseException ex) {
            field.setForeground(Color.RED);
            return null;
        }
    }

    /**
     * @return a short description of the active filters, for the export header, or {@code null} if none are active
     */
    private @Nullable String describeFilter(final JournalFilter filter) {
        if (!filter.isActive()) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (filter.text() != null && !filter.text().isBlank()) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.summary.text", filter.text().strip()));
        }
        if (filter.type() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.summary.type",
                  filter.type().getLabel()));
        }
        if (filter.thread() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.summary.thread",
                  threadName(filter.thread())));
        }
        if (filter.character() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.summary.character",
                  filter.character()));
        }
        if (filter.from() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.summary.from",
                  formatDate(filter.from())));
        }
        if (filter.to() != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.filter.summary.to", formatDate(filter.to())));
        }
        return String.join(", ", parts);
    }

    private void refreshAll() {
        if (updatingFilters || lstEntries == null || txtOracleLog == null || txtTimeline == null) {
            return;
        }
        JournalEntry selected = lstEntries.getSelectedValue();
        refreshEntries(selected);
        refreshOracleLog();
        refreshTimeline();
    }
    // endregion Filters

    // region Journal
    private JPanel createJournalPanel() {
        entryModel = new DefaultListModel<>();
        lstEntries = new JList<>(entryModel);
        lstEntries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstEntries.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                JournalEntry entry = (JournalEntry) value;
                String text = formatDate(entry.getDate()) + " - " + preview(entry.getPlainText());
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        lstEntries.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedEntry();
            }
        });

        JPanel pnlList = new JPanel(new BorderLayout(5, 5));
        pnlList.add(new JScrollPane(lstEntries), BorderLayout.CENTER);
        JPanel pnlListButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton btnNew = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.new"));
        btnNew.addActionListener(event -> newEntry());
        btnDelete = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.delete"));
        btnDelete.addActionListener(event -> deleteEntry());
        pnlListButtons.add(btnNew);
        pnlListButtons.add(btnDelete);
        pnlList.add(pnlListButtons, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlList, createEditorPanel());
        splitPane.setDividerLocation(300);

        JPanel pnlJournal = new JPanel(new BorderLayout());
        pnlJournal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlJournal.add(splitPane, BorderLayout.CENTER);
        return pnlJournal;
    }

    private JPanel createEditorPanel() {
        lblEntryDate = new JLabel();
        lblEntryDate.setFont(lblEntryDate.getFont().deriveFont(Font.BOLD));
        lblTags = new JLabel();
        btnEditTags = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tags.edit"));
        btnEditTags.addActionListener(event -> editTags());

        JPanel pnlTags = new JPanel(new BorderLayout(10, 0));
        pnlTags.add(lblTags, BorderLayout.CENTER);
        pnlTags.add(btnEditTags, BorderLayout.EAST);

        txtEntry = new JEditorPane();
        txtEntry.setEditorKit(new HTMLEditorKit());
        txtEntry.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JPanel pnlToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        addFormatButton(pnlToolbar, "bold", new StyledEditorKit.BoldAction(), Font.BOLD);
        addFormatButton(pnlToolbar, "italic", new StyledEditorKit.ItalicAction(), Font.ITALIC);
        addFormatButton(pnlToolbar, "underline", new StyledEditorKit.UnderlineAction(), Font.PLAIN);
        addFormatButton(pnlToolbar, "heading", new HTMLEditorKit.InsertHTMLTextAction("heading",
              "<h3>" + getTextAt(RESOURCE_BUNDLE, "JournalDialog.format.heading") + "</h3>", HTML.Tag.BODY,
              HTML.Tag.H3), Font.PLAIN);
        addFormatButton(pnlToolbar, "bullets", new HTMLEditorKit.InsertHTMLTextAction("bullets",
              "<ul><li></li></ul>", HTML.Tag.BODY, HTML.Tag.UL), Font.PLAIN);

        JPanel pnlHeader = new JPanel(new GridLayout(3, 1, 0, 3));
        pnlHeader.add(lblEntryDate);
        pnlHeader.add(pnlTags);
        pnlHeader.add(pnlToolbar);

        JPanel pnlEditor = new JPanel(new BorderLayout(5, 5));
        pnlEditor.add(pnlHeader, BorderLayout.NORTH);
        pnlEditor.add(new JScrollPane(txtEntry), BorderLayout.CENTER);
        return pnlEditor;
    }

    private void addFormatButton(final JPanel toolbar, final String key, final Action action, final int fontStyle) {
        JButton button = new JButton(action);
        button.setText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.format." + key));
        button.setToolTipText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.format." + key + ".toolTipText"));
        button.setFont(button.getFont().deriveFont(fontStyle));
        // Keep the caret and selection in the editor when a button is clicked.
        button.setFocusable(false);
        formatButtons.add(button);
        toolbar.add(button);
    }

    private String preview(final String text) {
        String firstLine = text.strip().lines().findFirst().orElse("");
        if (firstLine.isEmpty()) {
            return getTextAt(RESOURCE_BUNDLE, "JournalDialog.empty");
        }
        return firstLine.length() > PREVIEW_LENGTH ? firstLine.substring(0, PREVIEW_LENGTH) + "..." : firstLine;
    }

    /**
     * Rebuilds the note list from the filtered journal, reselecting {@code toSelect} if it is still shown.
     */
    private void refreshEntries(final @Nullable JournalEntry toSelect) {
        JournalFilter filter = currentFilter();
        entryModel.clear();
        for (JournalEntry entry : roleplay.getJournal()) {
            if (filter.matches(entry)) {
                entryModel.addElement(entry);
            }
        }

        int index = (toSelect == null) ? -1 : entryModel.indexOf(toSelect);
        if (index >= 0) {
            if (lstEntries.getSelectedIndex() != index) {
                lstEntries.setSelectedIndex(index);
            }
            lstEntries.ensureIndexIsVisible(index);
        } else {
            lstEntries.clearSelection();
        }
        showSelectedEntry();
    }

    private void showSelectedEntry() {
        JournalEntry entry = lstEntries.getSelectedValue();
        if (entry != null && entry == currentEditorEntry) {
            refreshTagLabel(entry);
            return;
        }
        currentEditorEntry = entry;

        updatingEditor = true;
        if (entry == null) {
            lblEntryDate.setText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.noSelection"));
            txtEntry.setText("");
            txtEntry.setEditable(false);
        } else {
            lblEntryDate.setText(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.entryDate",
                  formatDate(entry.getDate())));
            txtEntry.setText(entry.getText());
            txtEntry.setCaretPosition(0);
            txtEntry.setEditable(true);
        }
        listenToEditorDocument();
        updatingEditor = false;

        refreshTagLabel(entry);
        btnDelete.setEnabled(entry != null);
        btnEditTags.setEnabled(entry != null);
        formatButtons.forEach(button -> button.setEnabled(entry != null));
    }

    /**
     * Loading HTML can replace the editor's document, so make sure the save listener is on the current one.
     */
    private void listenToEditorDocument() {
        Document document = txtEntry.getDocument();
        if (document != listenedDocument) {
            if (listenedDocument != null) {
                listenedDocument.removeDocumentListener(editorListener);
            }
            document.addDocumentListener(editorListener);
            listenedDocument = document;
        }
    }

    private void saveEditor() {
        JournalEntry entry = currentEditorEntry;
        if (updatingEditor || entry == null) {
            return;
        }
        entry.setText(txtEntry.getText());
        lstEntries.repaint();
    }

    private void refreshTagLabel(final @Nullable JournalEntry entry) {
        String tags = (entry == null) ? "" : describeTags(entry);
        lblTags.setText(tags.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "JournalDialog.tags.none")
                              : getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.tags", tags));
    }

    private void newEntry() {
        JournalEntry entry = JournalEntry.newNote(today);
        // Tag the new note with whatever storyline the player is currently reading.
        JournalFilter filter = currentFilter();
        if (filter.thread() != null) {
            entry.getThreads().add(filter.thread());
        }
        entry.tagCharacter(filter.character());
        roleplay.getJournal().add(entry);

        if (!filter.matches(entry)) {
            // A search or date filter would hide the new note; clear the filters so it can be written.
            clearFilters();
        }
        refreshEntries(entry);
        txtEntry.requestFocusInWindow();
    }

    private void deleteEntry() {
        JournalEntry entry = lstEntries.getSelectedValue();
        if (entry == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
              getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.delete.confirm", formatDate(entry.getDate())),
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.delete"), JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            int index = lstEntries.getSelectedIndex();
            roleplay.getJournal().remove(entry);
            refreshEntries(null);
            if (!entryModel.isEmpty()) {
                lstEntries.setSelectedIndex(Math.min(index, entryModel.size() - 1));
            }
            refreshTimeline();
        }
    }

    private void editTags() {
        JournalEntry entry = lstEntries.getSelectedValue();
        if (entry == null) {
            return;
        }

        // Threads: every current thread, plus any deleted thread the entry still names.
        DefaultListModel<Object> threadModel = new DefaultListModel<>();
        roleplay.getPlotThreads().forEach(threadModel::addElement);
        for (UUID id : entry.getThreads()) {
            if (roleplay.getPlotThread(id) == null) {
                threadModel.addElement(id);
            }
        }
        JList<Object> lstThreads = new JList<>(threadModel);
        lstThreads.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Object display = (value instanceof UUID)
                                       ? getTextAt(RESOURCE_BUNDLE, "JournalDialog.tags.deletedThread") : value;
                return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
            }
        });
        selectMatching(lstThreads, value -> entry.getThreads().contains(
              value instanceof PlotThread thread ? thread.getId() : value));

        // Characters: everyone on the Oracle list, plus any removed character the entry still names.
        DefaultListModel<String> characterModel = new DefaultListModel<>();
        roleplay.getCharacters().forEach(characterModel::addElement);
        entry.getCharacters().stream().filter(name -> !roleplay.getCharacters().contains(name))
              .forEach(characterModel::addElement);
        JList<String> lstCharacters = new JList<>(characterModel);
        selectMatching(lstCharacters, entry.getCharacters()::contains);

        JPanel pnlLists = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlLists.add(titledList("JournalDialog.tags.threads", lstThreads));
        pnlLists.add(titledList("JournalDialog.tags.characters", lstCharacters));
        JPanel pnlTagEditor = new JPanel(new BorderLayout(0, 8));
        pnlTagEditor.add(pnlLists, BorderLayout.CENTER);
        pnlTagEditor.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tags.help")), BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(this, pnlTagEditor,
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.tags.edit"), JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        entry.getThreads().clear();
        for (Object value : lstThreads.getSelectedValuesList()) {
            entry.getThreads().add(value instanceof PlotThread thread ? thread.getId() : (UUID) value);
        }
        entry.getCharacters().clear();
        lstCharacters.getSelectedValuesList().forEach(entry::tagCharacter);
        refreshAll();
    }

    private static <T> void selectMatching(final JList<T> list, final java.util.function.Predicate<T> selected) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (selected.test(list.getModel().getElementAt(i))) {
                indices.add(i);
            }
        }
        list.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
    }

    private static JPanel titledList(final String key, final JList<?> list) {
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(10);
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, key)), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(220, 200));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
    // endregion Journal

    // region Oracle Log and Timeline
    private JPanel createOracleLogPanel() {
        txtOracleLog = createReadOnlyPane();

        JButton btnClear = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.clear"));
        btnClear.addActionListener(event -> clearOracleLog());
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlButtons.add(btnClear);

        JPanel pnlLog = new JPanel(new BorderLayout(5, 5));
        pnlLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlLog.add(new JScrollPane(txtOracleLog), BorderLayout.CENTER);
        pnlLog.add(pnlButtons, BorderLayout.SOUTH);
        return pnlLog;
    }

    private JPanel createTimelinePanel() {
        txtTimeline = createReadOnlyPane();
        JPanel pnlTimeline = new JPanel(new BorderLayout());
        pnlTimeline.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlTimeline.add(new JScrollPane(txtTimeline), BorderLayout.CENTER);
        return pnlTimeline;
    }

    private static JEditorPane createReadOnlyPane() {
        JEditorPane pane = new JEditorPane("text/html", "");
        pane.setEditable(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        return pane;
    }

    private void refreshOracleLog() {
        JournalFilter filter = currentFilter();
        List<JournalEntry> entries = new ArrayList<>(roleplay.getOracleLog().stream().filter(filter::matches).toList());
        // Newest first, so the latest result is at the top.
        java.util.Collections.reverse(entries);
        String emptyKey = roleplay.getOracleLog().isEmpty() ? "JournalDialog.oracleLog.empty"
                                : "JournalDialog.noMatches";
        showEntries(txtOracleLog, entries, emptyKey);
    }

    private void refreshTimeline() {
        List<JournalEntry> entries = roleplay.getTimeline(currentFilter());
        boolean anything = !roleplay.getJournal().isEmpty() || !roleplay.getOracleLog().isEmpty();
        showEntries(txtTimeline, entries, anything ? "JournalDialog.noMatches" : "JournalDialog.timeline.empty");
    }

    private void showEntries(final JEditorPane pane, final List<JournalEntry> entries, final String emptyKey) {
        StringBuilder html = new StringBuilder("<html><body>");
        if (entries.isEmpty()) {
            html.append("<i>").append(JournalText.escapeHtml(getTextAt(RESOURCE_BUNDLE, emptyKey))).append("</i>");
        }
        for (JournalEntry entry : entries) {
            html.append("<p><b>").append(JournalText.escapeHtml(formatDate(entry.getDate()))).append(" - ")
                  .append(JournalText.escapeHtml(entry.getType().getLabel())).append("</b>");
            String tags = describeTags(entry);
            if (!tags.isEmpty()) {
                html.append(" <i>(").append(JournalText.escapeHtml(tags)).append(")</i>");
            }
            html.append("<br>").append(entry.getHtmlBody()).append("</p>");
        }
        pane.setText(html.append("</body></html>").toString());
        pane.setCaretPosition(0);
    }

    private void clearOracleLog() {
        int choice = JOptionPane.showConfirmDialog(this,
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.clear.confirm"),
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.clear"), JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            roleplay.getOracleLog().clear();
            refreshAll();
        }
    }
    // endregion Oracle Log and Timeline

    // region Export
    private void export() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter markdown = new FileNameExtensionFilter(
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.export.markdown"), "md", "markdown");
        FileNameExtensionFilter text = new FileNameExtensionFilter(
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.export.text"), "txt");
        chooser.addChoosableFileFilter(markdown);
        chooser.addChoosableFileFilter(text);
        chooser.setFileFilter(markdown);
        chooser.setSelectedFile(new File(getTextAt(RESOURCE_BUNDLE, "JournalDialog.export.defaultName") + ".md"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
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
        List<JournalEntry> entries = roleplay.getTimeline(filter);
        String document = JournalExporter.export(entries, format, getTextAt(RESOURCE_BUNDLE, "JournalDialog.title"),
              describeFilter(filter), roleplay::getPlotThreadName, JournalDialog::formatDate);
        try {
            Files.writeString(file.toPath(), document, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.export.done",
                  entries.size(), file.getAbsolutePath()));
        } catch (Exception ex) {
            LOGGER.error("Failed to export the journal to {}", file, ex);
            JOptionPane.showMessageDialog(this, getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.export.failed",
                  ex.getMessage()), getTextAt(RESOURCE_BUNDLE, "JournalDialog.export"), JOptionPane.ERROR_MESSAGE);
        }
    }
    // endregion Export

    private String describeTags(final JournalEntry entry) {
        List<String> names = new ArrayList<>();
        for (UUID id : entry.getThreads()) {
            names.add(threadName(id));
        }
        names.addAll(entry.getCharacters());
        return String.join(", ", names);
    }

    private String threadName(final UUID id) {
        String name = roleplay.getPlotThreadName(id);
        return (name == null) ? getTextAt(RESOURCE_BUNDLE, "JournalDialog.tags.deletedThread") : name;
    }
}

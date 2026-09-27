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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mekhq.MekHQ;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.Roleplay;

/**
 * The roleplay journal. The Journal tab holds the player's own dated notes; the Oracle Log tab shows the Oracle
 * results recorded automatically.
 */
public class JournalDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";
    private static final int PREVIEW_LENGTH = 40;

    private final Roleplay roleplay;
    private final LocalDate today;

    private DefaultListModel<JournalEntry> entryModel;
    private JList<JournalEntry> lstEntries;
    private JLabel lblEntryDate;
    private JTextArea txtEntry;
    private JButton btnDelete;
    private boolean updatingEditor;

    private JTextArea txtOracleLog;

    public JournalDialog(final JDialog owner, final Roleplay roleplay, final LocalDate today) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "JournalDialog.title"), false);
        this.roleplay = roleplay;
        this.today = today;
        initialize();
        setSize(new Dimension(760, 520));
        setLocationRelativeTo(owner);
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tab.journal"), createJournalPanel());
        tabs.addTab(getTextAt(RESOURCE_BUNDLE, "JournalDialog.tab.oracleLog"), createOracleLogPanel());
        // The log grows while the journal is open, so refresh it whenever it is shown.
        tabs.addChangeListener(event -> refreshOracleLog());

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.close"));
        btnClose.addActionListener(event -> dispose());
        pnlButtons.add(btnClose);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
    }

    private static String formatDate(final LocalDate date) {
        return MekHQ.getMHQOptions().getDisplayFormattedDate(date);
    }

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
                String text = formatDate(entry.getDate()) + " - " + preview(entry.getText());
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

        lblEntryDate = new JLabel();
        txtEntry = new JTextArea();
        txtEntry.setLineWrap(true);
        txtEntry.setWrapStyleWord(true);
        txtEntry.getDocument().addDocumentListener(new DocumentListener() {
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
        });

        JPanel pnlEditor = new JPanel(new BorderLayout(5, 5));
        pnlEditor.add(lblEntryDate, BorderLayout.NORTH);
        pnlEditor.add(new JScrollPane(txtEntry), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlList, pnlEditor);
        splitPane.setDividerLocation(280);

        JPanel pnlJournal = new JPanel(new BorderLayout());
        pnlJournal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlJournal.add(splitPane, BorderLayout.CENTER);

        refreshEntries(roleplay.getJournal().size() - 1);
        return pnlJournal;
    }

    private String preview(final String text) {
        String firstLine = text.strip().lines().findFirst().orElse("");
        if (firstLine.isEmpty()) {
            return getTextAt(RESOURCE_BUNDLE, "JournalDialog.empty");
        }
        return firstLine.length() > PREVIEW_LENGTH ? firstLine.substring(0, PREVIEW_LENGTH) + "..." : firstLine;
    }

    private void refreshEntries(final int selectedIndex) {
        entryModel.clear();
        entryModel.addAll(roleplay.getJournal());
        if (selectedIndex >= 0 && selectedIndex < entryModel.size()) {
            lstEntries.setSelectedIndex(selectedIndex);
            lstEntries.ensureIndexIsVisible(selectedIndex);
        }
        showSelectedEntry();
    }

    private void showSelectedEntry() {
        JournalEntry entry = lstEntries.getSelectedValue();
        updatingEditor = true;
        if (entry == null) {
            lblEntryDate.setText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.noSelection"));
            txtEntry.setText("");
            txtEntry.setEnabled(false);
        } else {
            lblEntryDate.setText(getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.entryDate",
                  formatDate(entry.getDate())));
            txtEntry.setText(entry.getText());
            txtEntry.setCaretPosition(0);
            txtEntry.setEnabled(true);
        }
        updatingEditor = false;
        btnDelete.setEnabled(entry != null);
    }

    private void saveEditor() {
        JournalEntry entry = lstEntries.getSelectedValue();
        if (updatingEditor || entry == null) {
            return;
        }
        entry.setText(txtEntry.getText());
        lstEntries.repaint();
    }

    private void newEntry() {
        List<JournalEntry> journal = roleplay.getJournal();
        journal.add(new JournalEntry(today, ""));
        refreshEntries(journal.size() - 1);
        txtEntry.requestFocusInWindow();
    }

    private void deleteEntry() {
        int index = lstEntries.getSelectedIndex();
        if (index < 0) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
              getFormattedTextAt(RESOURCE_BUNDLE, "JournalDialog.delete.confirm",
                    formatDate(roleplay.getJournal().get(index).getDate())),
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.delete"), JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            roleplay.getJournal().remove(index);
            refreshEntries(Math.min(index, roleplay.getJournal().size() - 1));
        }
    }
    // endregion Journal

    // region Oracle Log
    private JPanel createOracleLogPanel() {
        txtOracleLog = new JTextArea();
        txtOracleLog.setEditable(false);
        txtOracleLog.setLineWrap(true);
        txtOracleLog.setWrapStyleWord(true);

        JButton btnClear = new JButton(getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.clear"));
        btnClear.addActionListener(event -> clearOracleLog());
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlButtons.add(btnClear);

        JPanel pnlLog = new JPanel(new BorderLayout(5, 5));
        pnlLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlLog.add(new JScrollPane(txtOracleLog), BorderLayout.CENTER);
        pnlLog.add(pnlButtons, BorderLayout.SOUTH);

        refreshOracleLog();
        return pnlLog;
    }

    private void refreshOracleLog() {
        List<JournalEntry> log = roleplay.getOracleLog();
        if (log.isEmpty()) {
            txtOracleLog.setText(getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.empty"));
            return;
        }

        StringBuilder text = new StringBuilder();
        // Newest first, so the latest result is at the top.
        for (int i = log.size() - 1; i >= 0; i--) {
            JournalEntry entry = log.get(i);
            text.append(formatDate(entry.getDate())).append('\n').append(entry.getText()).append("\n\n");
        }
        txtOracleLog.setText(text.toString().strip());
        txtOracleLog.setCaretPosition(0);
    }

    private void clearOracleLog() {
        int choice = JOptionPane.showConfirmDialog(this,
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.clear.confirm"),
              getTextAt(RESOURCE_BUNDLE, "JournalDialog.oracleLog.clear"), JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            roleplay.getOracleLog().clear();
            refreshOracleLog();
        }
    }
    // endregion Oracle Log
}

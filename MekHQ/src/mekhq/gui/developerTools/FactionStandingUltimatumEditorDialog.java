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
package mekhq.gui.developerTools;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import jakarta.annotation.Nullable;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumData;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumManifest;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumSide;
import mekhq.gui.FileDialogs;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog;

/**
 * A developer tool for editing Faction Standing ultimatum files (JSON). Each ultimatum is its own file; this editor
 * exposes every persisted field with New / Load / Save / Add to Manifest, mirroring the point of interest editor.
 *
 * <p>An ultimatum's sides are edited as an ordered list: the picker shows them in this order, and the first side
 * delivers the opening offer.</p>
 *
 * <p>Changes take effect for campaigns loaded after saving, as each campaign builds its ultimatum library when it
 * loads.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class FactionStandingUltimatumEditorDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";
    private static final String ULTIMATUM_RESOURCE_BUNDLE = "mekhq.resources.FactionStandingUltimatumDialog";
    private static final String KEY_PREFIX = "ultimatumEditor.";

    /** Where the ultimatum dialog's text lives: the bundle the game reads it from. */
    static final String ULTIMATUM_TEXT_BUNDLE = ULTIMATUM_RESOURCE_BUNDLE;

    /**
     * One text key an ultimatum needs, for the Text Keys table.
     *
     * @param key       the resource key
     * @param purpose   what the text is used for, for the writer
     * @param isWritten {@code true} if the bundle already has an entry for the key
     *
     * @author Illiani
     * @since 0.51.01
     */
    record TextKeyRow(String key, String purpose, boolean isWritten) {}

    /** Ultimatum names and side IDs become resource keys and file names, so they're limited to upper-case IDs. */
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z0-9_]+");

    private final JFrame frame;
    // The file the current ultimatum was last loaded from or saved to; null until then. Registering with the manifest
    // needs a concrete file name, so the button stays disabled until this is set.
    private File currentFile;

    private final JTextField txtName = new JTextField(30);
    private final JTextField txtDate = new JTextField(12);
    private final JTextArea txtNotes = new JTextArea(4, 40);
    private final JTextArea txtAffectedFactionCodes = new JTextArea(3, 12);

    private final DefaultListModel<FactionStandingUltimatumSide> sideModel = new DefaultListModel<>();
    private final JList<FactionStandingUltimatumSide> lstSides = new JList<>(sideModel);
    // The list row the side fields currently show, so edits can be written back before the selection moves
    private int editingSideIndex = -1;
    private final JTextField txtSideId = new JTextField(10);
    private final JTextField txtSideName = new JTextField(30);
    private final JComboBox<PersonnelRole> cboSideRole = new JComboBox<>(PersonnelRole.values());
    private final JTextField txtSideFactionCode = new JTextField(8);

    private final JComboBox<String> cboDissenterPreference = new JComboBox<>();
    private final JCheckBox chkViolentTransition = new JCheckBox();
    private final JSpinner spnDivisiveness = new JSpinner(new SpinnerNumberModel(0,
          FactionStandingUltimatumData.MINIMUM_DIVISIVENESS, FactionStandingUltimatumData.MAXIMUM_DIVISIVENESS, 1));
    private final JButton btnAddToManifest = new JButton(getTextAt(RESOURCE_BUNDLE, "button.addToManifest"));

    /**
     * @param parent the owning frame
     *
     * @author Illiani
     * @since 0.51.01
     */
    public FactionStandingUltimatumEditorDialog(JFrame parent) {
        super(parent, true);
        this.frame = parent;
        setTitle(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "title"));
        setLayout(new BorderLayout());
        add(new FastJScrollPane(buildForm()), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
        clearForm();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 5, 3, 5);

        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        cboDissenterPreference.setEditable(true);

        lstSides.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstSides.setVisibleRowCount(4);
        lstSides.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                FactionStandingUltimatumSide side = (FactionStandingUltimatumSide) value;
                String shown = (index + 1) + ". " + nullToEmpty(side.id()) + " – " + nullToEmpty(side.name());
                return super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
            }
        });
        lstSides.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                commitSideFields();
                showSide(lstSides.getSelectedIndex());
            }
        });

        addRow(panel, constraints, "name", txtName);
        addRow(panel, constraints, "date", txtDate);
        addRow(panel, constraints, "notes", new FastJScrollPane(txtNotes));
        addRow(panel, constraints, "affectedFactionCodes", new FastJScrollPane(txtAffectedFactionCodes));
        addRow(panel, constraints, "sides", buildSidesPanel());
        addRow(panel, constraints, "sideId", txtSideId);
        addRow(panel, constraints, "sideName", txtSideName);
        addRow(panel, constraints, "sideRole", cboSideRole);
        addRow(panel, constraints, "sideFactionCode", txtSideFactionCode);
        addRow(panel, constraints, "dissenterPreference", cboDissenterPreference);
        addRow(panel, constraints, "isViolentTransition", chkViolentTransition);
        addRow(panel, constraints, "divisiveness", spnDivisiveness);
        return panel;
    }

    /**
     * The ordered list of sides with its Add / Remove / Move buttons.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildSidesPanel() {
        JButton btnAddSide = new JButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "sides.add"));
        btnAddSide.addActionListener(event -> {
            commitSideFields();
            sideModel.addElement(new FactionStandingUltimatumSide("", "", PersonnelRole.NOBLE, ""));
            lstSides.setSelectedIndex(sideModel.size() - 1);
            refreshDissenterChoices();
        });
        JButton btnRemoveSide = new JButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "sides.remove"));
        btnRemoveSide.addActionListener(event -> {
            int index = lstSides.getSelectedIndex();
            if (index < 0) {
                return;
            }
            editingSideIndex = -1;
            sideModel.remove(index);
            lstSides.setSelectedIndex(Math.min(index, sideModel.size() - 1));
            refreshDissenterChoices();
        });
        JButton btnMoveUp = new JButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "sides.up"));
        btnMoveUp.addActionListener(event -> moveSelectedSide(-1));
        JButton btnMoveDown = new JButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "sides.down"));
        btnMoveDown.addActionListener(event -> moveSelectedSide(1));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 3));
        buttons.add(btnAddSide);
        buttons.add(btnRemoveSide);
        buttons.add(btnMoveUp);
        buttons.add(btnMoveDown);

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.add(new FastJScrollPane(lstSides), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void moveSelectedSide(int step) {
        commitSideFields();
        int index = lstSides.getSelectedIndex();
        int target = index + step;
        if (index < 0 || target < 0 || target >= sideModel.size()) {
            return;
        }
        FactionStandingUltimatumSide side = sideModel.get(index);
        editingSideIndex = -1;
        sideModel.remove(index);
        sideModel.add(target, side);
        lstSides.setSelectedIndex(target);
    }

    /**
     * Writes the side fields back into the side they were showing.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void commitSideFields() {
        if (editingSideIndex < 0 || editingSideIndex >= sideModel.size()) {
            return;
        }
        sideModel.set(editingSideIndex, new FactionStandingUltimatumSide(
              txtSideId.getText().trim(),
              txtSideName.getText().trim(),
              (PersonnelRole) cboSideRole.getSelectedItem(),
              txtSideFactionCode.getText().trim()));
        refreshDissenterChoices();
    }

    /**
     * Shows a side in the side fields, or clears and disables them when no side is selected.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void showSide(int index) {
        editingSideIndex = index;
        boolean hasSide = index >= 0 && index < sideModel.size();
        FactionStandingUltimatumSide side = hasSide ? sideModel.get(index) : null;
        txtSideId.setText(side == null ? "" : nullToEmpty(side.id()));
        txtSideName.setText(side == null ? "" : nullToEmpty(side.name()));
        cboSideRole.setSelectedItem(side == null || side.role() == null ? PersonnelRole.NOBLE : side.role());
        txtSideFactionCode.setText(side == null ? "" : nullToEmpty(side.factionCode()));
        txtSideId.setEnabled(hasSide);
        txtSideName.setEnabled(hasSide);
        cboSideRole.setEnabled(hasSide);
        txtSideFactionCode.setEnabled(hasSide);
    }

    /**
     * Offers every side's ID, then going rogue, as the dissenter's preference, keeping the current value.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void refreshDissenterChoices() {
        Object current = cboDissenterPreference.getSelectedItem();
        cboDissenterPreference.removeAllItems();
        for (int index = 0; index < sideModel.size(); index++) {
            String sideId = sideModel.get(index).id();
            if (sideId != null && !sideId.isBlank()) {
                cboDissenterPreference.addItem(sideId);
            }
        }
        cboDissenterPreference.addItem(FactionStandingUltimatumData.ROGUE_PREFERENCE);
        cboDissenterPreference.setSelectedItem(current);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void addRow(JPanel panel, GridBagConstraints constraints, String fieldKey, Component control) {
        String labelKey = KEY_PREFIX + fieldKey;
        constraints.gridx = 0;
        constraints.gridy++;
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        DeveloperToolsUI.applyRowTooltip(RESOURCE_BUNDLE, labelKey, label, control);
        panel.add(label, constraints);
        constraints.gridx = 1;
        panel.add(control, constraints);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildButtonBar() {
        JPanel bar = new JPanel();
        JButton btnNew = new JButton(getTextAt(RESOURCE_BUNDLE, "button.new"));
        btnNew.addActionListener(event -> {
            currentFile = null;
            clearForm();
            updateManifestButtonState();
        });
        JButton btnLoad = new JButton(getTextAt(RESOURCE_BUNDLE, "button.load"));
        btnLoad.addActionListener(event -> loadFromFile());
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(event -> saveToFile());
        btnAddToManifest.addActionListener(event -> addToManifest());
        btnAddToManifest.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "addToManifest.tooltip")));
        JButton btnTextKeys = new JButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.button"));
        btnTextKeys.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.button.tooltip")));
        btnTextKeys.addActionListener(event -> showTextKeys());
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "button.close"));
        btnClose.addActionListener(event -> dispose());
        bar.add(btnNew);
        bar.add(btnLoad);
        bar.add(btnSave);
        bar.add(btnAddToManifest);
        bar.add(btnTextKeys);
        bar.add(btnClose);
        updateManifestButtonState();
        return bar;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void updateManifestButtonState() {
        btnAddToManifest.setEnabled(currentFile != null);
    }

    /**
     * Resets every field to a blank ultimatum.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void clearForm() {
        txtName.setText("");
        txtDate.setText("");
        txtNotes.setText("");
        txtAffectedFactionCodes.setText("");
        editingSideIndex = -1;
        sideModel.clear();
        showSide(-1);
        refreshDissenterChoices();
        cboDissenterPreference.setSelectedItem(null);
        chkViolentTransition.setSelected(false);
        spnDivisiveness.setValue(0);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void load(FactionStandingUltimatumData source) {
        txtName.setText(nullToEmpty(source.name()));
        txtDate.setText(nullToEmpty(source.date()));
        txtNotes.setText(nullToEmpty(source.notes()));
        txtAffectedFactionCodes.setText(String.join("\n", source.affectedFactionCodes()));

        editingSideIndex = -1;
        sideModel.clear();
        for (FactionStandingUltimatumSide side : source.sides()) {
            sideModel.addElement(side);
        }
        refreshDissenterChoices();
        cboDissenterPreference.setSelectedItem(source.dissenterPreference());
        if (sideModel.isEmpty()) {
            showSide(-1);
        } else {
            lstSides.setSelectedIndex(0);
        }

        chkViolentTransition.setSelected(source.isViolentTransition());
        // Clamped so a hand-edited file outside the supported range still loads into the spinner
        spnDivisiveness.setValue(Math.clamp(source.divisiveness(), FactionStandingUltimatumData.MINIMUM_DIVISIVENESS,
              FactionStandingUltimatumData.MAXIMUM_DIVISIVENESS));
    }

    /**
     * @return a new ultimatum built from the form's current values
     *
     * @author Illiani
     * @since 0.51.01
     */
    private FactionStandingUltimatumData buildFromForm() {
        commitSideFields();
        List<FactionStandingUltimatumSide> sides = new ArrayList<>();
        for (int index = 0; index < sideModel.size(); index++) {
            sides.add(sideModel.get(index));
        }

        Object dissenterPreference = cboDissenterPreference.getSelectedItem();
        return new FactionStandingUltimatumData(
              txtName.getText().trim(),
              txtDate.getText().trim(),
              emptyToNull(txtNotes.getText()),
              parseLines(txtAffectedFactionCodes.getText()),
              sides,
              dissenterPreference == null ? null : emptyToNull(dissenterPreference.toString()),
              chkViolentTransition.isSelected(),
              (int) spnDivisiveness.getValue());
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void loadFromFile() {
        File file = FileDialogs.openFactionStandingUltimatum(frame).orElse(null);
        if (file == null) {
            return;
        }

        FactionStandingUltimatumData loaded = FactionStandingUltimatumData.deserialize(file);
        if (loaded == null) {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "loadError.message"),
                  getTextAt(RESOURCE_BUNDLE, "loadError.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentFile = file;
        load(loaded);
        updateManifestButtonState();
    }

    /**
     * Validates the form and saves it. Missing dialog text is reported after saving, but doesn't block it, since the
     * text is usually written after the ultimatum.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void saveToFile() {
        String title = getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "title");
        FactionStandingUltimatumData ultimatum = buildFromForm();

        List<String> problemKeys = findProblems(ultimatum);
        if (!problemKeys.isEmpty()) {
            StringBuilder message = new StringBuilder(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "invalid.message"));
            for (String problemKey : problemKeys) {
                message.append("\n- ").append(getTextAt(RESOURCE_BUNDLE, problemKey));
            }
            JOptionPane.showMessageDialog(this, message.toString(), title, JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileDialogs.saveFactionStandingUltimatum(frame, ultimatum.name()).ifPresent(file -> {
            if (!ultimatum.serialize(file)) {
                JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "saveError.message"),
                      title, JOptionPane.ERROR_MESSAGE);
                return;
            }

            currentFile = file;
            updateManifestButtonState();
            warnAboutMissingText(ultimatum);
        });
    }

    /**
     * Tells the developer which of the ultimatum's dialog text keys have yet to be written.
     *
     * @param ultimatum the saved ultimatum
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void warnAboutMissingText(FactionStandingUltimatumData ultimatum) {
        List<String> missingKeys = new ArrayList<>();
        for (String key : FactionStandingUltimatumDialog.getRequiredTextKeys(ultimatum.name(), getSideIds(ultimatum))) {
            if (!isResourceKeyValid(getTextAt(ULTIMATUM_RESOURCE_BUNDLE, key))) {
                missingKeys.add(key);
            }
        }

        if (!missingKeys.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "missingText.message",
                        String.join("\n", missingKeys)),
                  getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "title"), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows every text key the ultimatum in the form needs, what each is for, whether it has been written, and the file
     * the entries belong in. Missing keys can be copied as ready-to-fill {@code key=} lines.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void showTextKeys() {
        String title = getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.title");
        FactionStandingUltimatumData ultimatum = buildFromForm();
        String name = ultimatum.name();
        if (name == null || !ID_PATTERN.matcher(name).matches()) {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.needsName"), title,
                  JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<TextKeyRow> rows = buildTextKeyRows(ultimatum);
        int missingCount = 0;
        DefaultTableModel tableModel = new DefaultTableModel(new Object[] {
              getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.column.key"),
              getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.column.status"),
              getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.column.purpose") }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (TextKeyRow row : rows) {
            if (!row.isWritten()) {
                missingCount++;
            }
            tableModel.addRow(new Object[] { row.key(),
                                             getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + (row.isWritten()
                                                                                            ? "textKeys.written"
                                                                                            : "textKeys.missing")),
                                             row.purpose() });
        }

        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(420);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(420);

        // The file path is a text field so it can be selected and copied
        JTextField txtFilePath = new JTextField(getBundleFilePath(ULTIMATUM_TEXT_BUNDLE));
        txtFilePath.setEditable(false);
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.summary", rows.size(),
              missingCount)), BorderLayout.NORTH);
        header.add(new JLabel(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.file")), BorderLayout.WEST);
        header.add(txtFilePath, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JDialog dialog = new JDialog(this, title, true);
        JButton btnCopyMissing = new JButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.copyMissing"));
        btnCopyMissing.setEnabled(missingCount > 0);
        btnCopyMissing.addActionListener(event -> Toolkit.getDefaultToolkit()
                                                        .getSystemClipboard()
                                                        .setContents(new StringSelection(
                                                              buildMissingKeysSnippet(name, rows)), null));
        JButton btnCloseKeys = new JButton(getTextAt(RESOURCE_BUNDLE, "button.close"));
        btnCloseKeys.addActionListener(event -> dialog.dispose());
        JPanel buttons = new JPanel();
        buttons.add(btnCopyMissing);
        buttons.add(btnCloseKeys);

        dialog.setLayout(new BorderLayout());
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(new FastJScrollPane(table), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(980, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Lists every text key an ultimatum needs, with what each is used for and whether it has been written. The order
     * matches the order the player meets the text: the three opening scenes, each side's card and news bulletin, then
     * the Mercenary and Pirate cards.
     *
     * @param ultimatum the ultimatum to list keys for
     *
     * @return one row per required key
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<TextKeyRow> buildTextKeyRows(FactionStandingUltimatumData ultimatum) {
        List<FactionStandingUltimatumSide> sides = ultimatum.sides();
        List<String> keys = FactionStandingUltimatumDialog.getRequiredTextKeys(ultimatum.name(), getSideIds(ultimatum));
        String firstLeader = sides.isEmpty() ? "" : nullToEmpty(sides.get(0).name());
        String dissenterPreference = nullToEmpty(ultimatum.dissenterPreference());

        List<String> purposes = new ArrayList<>();
        purposes.add(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.initialOffer", firstLeader));
        purposes.add(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.for", firstLeader));
        purposes.add(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.against",
              dissenterPreference));
        for (FactionStandingUltimatumSide side : sides) {
            String sideLabel = nullToEmpty(side.id());
            purposes.add(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.pitch", sideLabel));
            purposes.add(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.news", sideLabel));
        }
        purposes.add(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.mercenary"));
        purposes.add(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "textKeys.purpose.pirate"));

        List<TextKeyRow> rows = new ArrayList<>();
        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            boolean isWritten = isResourceKeyValid(getTextAt(ULTIMATUM_TEXT_BUNDLE, key));
            rows.add(new TextKeyRow(key, purposes.get(index), isWritten));
        }
        return rows;
    }

    /**
     * Builds the missing keys as {@code key=} lines, ready to paste into the properties file and fill in.
     *
     * @param ultimatumName the ultimatum's name, used as the block's comment heading
     * @param rows          the ultimatum's text key rows
     *
     * @return the snippet, or an empty string if nothing is missing
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String buildMissingKeysSnippet(String ultimatumName, List<TextKeyRow> rows) {
        StringBuilder snippet = new StringBuilder();
        for (TextKeyRow row : rows) {
            if (!row.isWritten()) {
                snippet.append(row.key()).append("=\n");
            }
        }
        if (snippet.isEmpty()) {
            return "";
        }
        return "# " + ultimatumName + "\n" + snippet;
    }

    /**
     * Turns a resource bundle name into the properties file that holds it, relative to the MekHQ repository.
     *
     * @param bundleName the bundle name, such as {@code mekhq.resources.FactionStandingUltimatumDialog}
     *
     * @return the file path, such as {@code MekHQ/resources/mekhq/resources/FactionStandingUltimatumDialog.properties}
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String getBundleFilePath(String bundleName) {
        return "MekHQ/resources/" + bundleName.replace('.', '/') + ".properties";
    }

    /**
     * @return the IDs of the ultimatum's sides, in order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<String> getSideIds(FactionStandingUltimatumData ultimatum) {
        List<String> sideIds = new ArrayList<>();
        for (FactionStandingUltimatumSide side : ultimatum.sides()) {
            sideIds.add(side.id());
        }
        return sideIds;
    }

    /**
     * Registers the current ultimatum's file name in the {@code ultimatummanifest.json} that sits alongside it, so the
     * game will load it. A manifest that exists but can't be read is left unchanged, as writing a new one over it would
     * drop every other entry.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void addToManifest() {
        if (currentFile == null) {
            return;
        }

        String fileName = currentFile.getName();
        File manifestFile = new File(currentFile.getParentFile(), FactionStandingUltimatumManifest.MANIFEST_FILE_NAME);
        String manifestTitle = getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "manifest.title");

        FactionStandingUltimatumManifest manifest;
        if (manifestFile.exists()) {
            manifest = FactionStandingUltimatumManifest.deserialize(manifestFile);
            if (manifest == null) {
                JOptionPane.showMessageDialog(this,
                      getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "manifest.loadError.message",
                            manifestFile.getPath()),
                      manifestTitle, JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            manifest = new FactionStandingUltimatumManifest();
        }

        if (manifest.ultimatumFileNames.contains(fileName)) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "manifest.alreadyPresent.message", fileName),
                  manifestTitle, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        manifest.ultimatumFileNames.add(fileName);
        if (manifest.serialize(manifestFile)) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "manifest.added.message", fileName),
                  manifestTitle, JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "manifest.error.message"),
                  manifestTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Checks an ultimatum for problems that would stop it working in game.
     *
     * @param ultimatum the ultimatum to check
     *
     * @return the resource keys describing each problem, in form order; empty if there are none
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<String> findProblems(FactionStandingUltimatumData ultimatum) {
        List<String> problemKeys = new ArrayList<>();

        String name = ultimatum.name();
        if (name == null || !ID_PATTERN.matcher(name).matches()) {
            problemKeys.add(KEY_PREFIX + "problem.name");
        }

        if (!isValidDate(ultimatum.date())) {
            problemKeys.add(KEY_PREFIX + "problem.date");
        }

        if (ultimatum.affectedFactionCodes().isEmpty()) {
            problemKeys.add(KEY_PREFIX + "problem.affectedFactionCodes");
        }

        List<FactionStandingUltimatumSide> sides = ultimatum.sides();
        if (sides.isEmpty()) {
            problemKeys.add(KEY_PREFIX + "problem.noSides");
        }

        boolean hasIncompleteSide = false;
        boolean hasDuplicateId = false;
        boolean hasDuplicateFaction = false;
        Set<String> sideIds = new HashSet<>();
        Set<String> factionCodes = new HashSet<>();
        for (FactionStandingUltimatumSide side : sides) {
            if (!isComplete(side)) {
                hasIncompleteSide = true;
                continue;
            }
            hasDuplicateId |= !sideIds.add(side.id());
            hasDuplicateFaction |= !factionCodes.add(side.factionCode());
        }
        if (hasIncompleteSide) {
            problemKeys.add(KEY_PREFIX + "problem.side");
        }
        if (hasDuplicateId) {
            problemKeys.add(KEY_PREFIX + "problem.duplicateSideId");
        }
        if (hasDuplicateFaction) {
            problemKeys.add(KEY_PREFIX + "problem.sameFaction");
        }

        String dissenterPreference = ultimatum.dissenterPreference();
        if (dissenterPreference == null || !(FactionStandingUltimatumData.ROGUE_PREFERENCE.equals(dissenterPreference)
                                                   || ultimatum.getSide(dissenterPreference) != null)) {
            problemKeys.add(KEY_PREFIX + "problem.dissenterPreference");
        }

        int divisiveness = ultimatum.divisiveness();
        if (divisiveness < FactionStandingUltimatumData.MINIMUM_DIVISIVENESS
                  || divisiveness > FactionStandingUltimatumData.MAXIMUM_DIVISIVENESS) {
            problemKeys.add(KEY_PREFIX + "problem.divisiveness");
        }

        return problemKeys;
    }

    /**
     * @return {@code true} if the text is a {@code yyyy-MM-dd} date
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isValidDate(@Nullable String date) {
        if (date == null) {
            return false;
        }

        try {
            LocalDate.parse(date);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    /**
     * @return {@code true} if the side has a valid ID, a name, a role, and a faction code
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isComplete(@Nullable FactionStandingUltimatumSide side) {
        return side != null
                     && side.id() != null && ID_PATTERN.matcher(side.id()).matches()
                     && side.name() != null && !side.name().isBlank()
                     && side.role() != null
                     && side.factionCode() != null && !side.factionCode().isBlank();
    }

    private static String nullToEmpty(@Nullable String value) {
        return (value == null) ? "" : value;
    }

    private static @Nullable String emptyToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }

    /**
     * @return each non-blank line of the text, trimmed
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<String> parseLines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                result.add(line.trim());
            }
        }
        return result;
    }
}

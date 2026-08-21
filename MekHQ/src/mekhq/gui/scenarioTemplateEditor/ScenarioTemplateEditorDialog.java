/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.scenarioTemplateEditor;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import megamek.client.ui.panels.abstractPanels.AbstractScrollablePanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import org.jspecify.annotations.NonNull;

/**
 * Handles editing, saving and loading of scenario template definitions.
 *
 * @author NickAragua
 */
public class ScenarioTemplateEditorDialog extends JDialog implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioTemplateEditorDialog.class);

    private final static String SAVE_TEMPLATE_COMMAND = "SAVE_TEMPLATE";
    private final static String LOAD_TEMPLATE_COMMAND = "LOAD_TEMPLATE";

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private final JFrame frame;

    TemplatePropertiesPanel templatePropertiesPanel;
    MapParametersPanel mapParametersPanel;
    ModifiersPanel modifiersPanel;
    ObjectivesPanel objectivesPanel;
    ForceEditorPanel forceEditorPanel;

    AbstractScrollablePanel globalPanel;

    final ForceTableModel forceTableModel = new ForceTableModel();
    JTable forceTable;
    JButton btnEditForce;
    JButton btnRemoveForce;
    JPanel forceListContainer;

    // the scenario template we're working on
    ScenarioTemplate scenarioTemplate = new ScenarioTemplate();

    // the ID of the force currently loaded into the force editor via "Edit", or null when adding a new force. Used to
    // update a force in place (and clean up its old key on rename) rather than orphaning or overwriting entries.
    String editedForceId = null;

    // serialized snapshot of the last saved/loaded editor state, for unsaved-changes detection.
    private String capturedState = "";

    /**
     * @param parent Creates a new instance of this dialog with the given parent JFrame.
     */
    public ScenarioTemplateEditorDialog(JFrame parent) {
        super(parent, true);
        frame = parent;
        initComponents();
        pack();
        validate();
        setUserPreferences();

        captureBaseline();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWithConfirmation();
            }
        });
    }

    /**
     * Initialize dialog components.
     */
    protected void initComponents() {
        this.setTitle(getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.title"));
        getContentPane().setLayout(new GridLayout());

        globalPanel = new DefaultMHQScrollablePanel(frame, "globalPanel", new GridBagLayout());

        JScrollPane globalScrollPane = new FastJScrollPane(globalPanel);
        globalScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        globalScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(globalScrollPane);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        setupTemplateProperties(constraints);
        setupObjectiveEditUI(constraints);
        setupForceEditorHeaders(constraints);
        setupForceEditor(constraints);
        initializeForceList(constraints);
        setupMapParameters(constraints);
        setupBottomButtons(constraints);

        renderForceList();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ScenarioTemplateEditorDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    /**
     * Sets up text entry boxes in the top - briefing, scenario name, labels.
     *
     */
    private void setupTemplateProperties(GridBagConstraints gridBagConstraints) {
        templatePropertiesPanel = new TemplatePropertiesPanel();
        templatePropertiesPanel.load(scenarioTemplate);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        globalPanel.add(templatePropertiesPanel, gridBagConstraints);
        gridBagConstraints.gridy++;
    }

    private void setupObjectiveEditUI(GridBagConstraints constraints) {
        objectivesPanel = new ObjectivesPanel();
        objectivesPanel.load(scenarioTemplate);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        globalPanel.add(objectivesPanel, constraints);
    }

    /**
     * Worker function that sets up top-level headers for the force template editor section.
     *
     */
    private void setupForceEditorHeaders(GridBagConstraints gridBagConstraints) {
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;

        JLabel lblForces = new JLabel(getTextAt(RESOURCE_BUNDLE,
              "ScenarioTemplateEditorDialog.participatingForces.label"));
        gridBagConstraints.gridy++;
        globalPanel.add(lblForces, gridBagConstraints);

        JButton btnHideShow = new JButton(getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.hideShow"));
        btnHideShow.addActionListener(evt -> toggleForcePanelVisibility());

        gridBagConstraints.gridx++;
        int previousAnchor = gridBagConstraints.anchor;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        globalPanel.add(btnHideShow, gridBagConstraints);

        JButton btnNewForce = new JButton(getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.newForce"));
        btnNewForce.addActionListener(evt -> newForce());
        gridBagConstraints.gridx++;
        globalPanel.add(btnNewForce, gridBagConstraints);
        gridBagConstraints.anchor = previousAnchor;
    }

    /**
     * Worker function that sets up UI elements for the force template editor.
     *
     */
    private void setupForceEditor(GridBagConstraints externalconstraints) {
        forceEditorPanel = new ForceEditorPanel(readMulFileNames());
        forceEditorPanel.setOnSave(this::commitForce);
        forceEditorPanel.setAvailableForceIds(scenarioTemplate.getScenarioForces().keySet());

        externalconstraints.gridx = 0;
        externalconstraints.gridy++;
        externalconstraints.gridwidth = GridBagConstraints.REMAINDER;
        globalPanel.add(forceEditorPanel, externalconstraints);
        externalconstraints.gridheight = 1;
    }

    /**
     * Reads the fixed-MUL file names from the MUL directory, to be offered by the force editor.
     */
    private static List<String> readMulFileNames() {
        List<String> names = new ArrayList<>();
        File mulDir = new File(MHQConstants.STRAT_CON_MUL_FILES_DIRECTORY);
        if (mulDir.exists() && mulDir.isDirectory()) {
            String[] muls = mulDir.list((d, s) -> s.toLowerCase().endsWith(".mul"));
            if (muls != null) {
                names.addAll(Arrays.asList(muls));
            }
        }
        return names;
    }

    /**
     * Commits the force currently in the editor into the template's roster (add or update), then refreshes the force
     * list. Invoked by the force editor's Save button.
     */
    private void commitForce() {
        String validationResult = forceEditorPanel.validateInput();
        if (!validationResult.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                  validationResult,
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.invalidForce.title"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        ScenarioForceTemplate forceTemplate = forceEditorPanel.buildForceTemplate();
        ForceRosterEditor.CommitResult commitResult = ForceRosterEditor.commit(scenarioTemplate,
              editedForceId,
              forceTemplate);
        if (!commitResult.committed()) {
            JOptionPane.showMessageDialog(this,
                  commitResult.errorMessage(),
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.invalidForce.title"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Back to "add new" mode once the edit has been committed.
        editedForceId = null;
        forceEditorPanel.setAvailableForceIds(scenarioTemplate.getScenarioForces().keySet());
        renderForceList();
        revalidate();
        repaint();
    }


    /**
     * Worker function called when initializing the dialog to place the force template list on the content pane.
     *
     * @param constraints Grid bag constraints.
     */
    private void initializeForceList(GridBagConstraints constraints) {
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = GridBagConstraints.REMAINDER;

        forceTable = new JTable(forceTableModel);
        forceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        forceTable.getSelectionModel().addListSelectionListener(e -> updateForceButtonState());
        // The model has 15 columns; with the default auto-resize they all get squeezed to a few unreadable pixels.
        // Turn auto-resize off and give each column a sensible width so the table scrolls horizontally instead.
        forceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] columnWidths = getColumnWidths();
        for (int column = 0; column < columnWidths.length && column < forceTable.getColumnCount(); column++) {
            forceTable.getColumnModel().getColumn(column).setPreferredWidth(columnWidths[column]);
        }

        btnEditForce = new JButton(getTextAt(RESOURCE_BUNDLE, "button.edit"));
        btnEditForce.setEnabled(false);
        btnEditForce.addActionListener(e -> editSelectedForce());
        btnRemoveForce = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));
        btnRemoveForce.setEnabled(false);
        btnRemoveForce.addActionListener(e -> removeSelectedForce());

        JPanel buttonRow = new JPanel();
        buttonRow.add(btnEditForce);
        buttonRow.add(btnRemoveForce);

        forceListContainer = new JPanel(new BorderLayout());
        forceListContainer.add(new FastJScrollPane(forceTable), BorderLayout.CENTER);
        forceListContainer.add(buttonRow, BorderLayout.SOUTH);
        forceListContainer.setVisible(false);

        globalPanel.add(forceListContainer, constraints);
    }

    private static int @NonNull [] getColumnWidths() {
        return new int[] { scaleForGUI(50), scaleForGUI(130), scaleForGUI(90), scaleForGUI(120), scaleForGUI(150),
                           scaleForGUI(150), scaleForGUI(100), scaleForGUI(70), scaleForGUI(120), scaleForGUI(100),
                           scaleForGUI(90), scaleForGUI(80), scaleForGUI(60), scaleForGUI(100), scaleForGUI(90) };
    }

    private void updateForceButtonState() {
        boolean hasSelection = forceTable.getSelectedRow() >= 0;
        btnEditForce.setEnabled(hasSelection);
        btnRemoveForce.setEnabled(hasSelection);
    }

    /**
     * Worker function called when initializing to place the map parameters.
     *
     */
    private void setupMapParameters(GridBagConstraints gridBagConstraints) {
        List<String> terrainTypeKeys = StratConBiomeManifest.getInstance()
                                             .getBiomeMapTypes()
                                             .keySet()
                                             .stream()
                                             .sorted()
                                             .toList();
        mapParametersPanel = new MapParametersPanel(terrainTypeKeys);
        mapParametersPanel.load(scenarioTemplate.mapParameters);

        JPanel pnlMapRow = new JPanel(new GridBagLayout());
        GridBagConstraints localConstraints = new GridBagConstraints();
        localConstraints.gridx = 0;
        localConstraints.gridy = 0;
        localConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlMapRow.add(mapParametersPanel, localConstraints);

        modifiersPanel = new ModifiersPanel(AtBScenarioModifier.getOrderedModifierKeys());
        modifiersPanel.load(scenarioTemplate.scenarioModifiers);
        localConstraints.gridx = 1;
        localConstraints.insets = new Insets(0, scaleForGUI(15), 0, 0);
        pnlMapRow.add(modifiersPanel, localConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        globalPanel.add(pnlMapRow, gridBagConstraints);
    }

    /**
     * Worker function that sets up the buttons on the bottom of the dialog
     *
     */
    private void setupBottomButtons(GridBagConstraints gridBagConstraints) {
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;

        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.setActionCommand(SAVE_TEMPLATE_COMMAND);
        btnSave.addActionListener(this);
        globalPanel.add(btnSave, gridBagConstraints);

        gridBagConstraints.gridx++;
        JButton btnLoad = new JButton(getTextAt(RESOURCE_BUNDLE, "button.load"));
        btnLoad.setActionCommand(LOAD_TEMPLATE_COMMAND);
        btnLoad.addActionListener(this);
        globalPanel.add(btnLoad, gridBagConstraints);

        gridBagConstraints.gridx++;
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "button.close"));
        btnClose.addActionListener(e -> closeWithConfirmation());
        globalPanel.add(btnClose, gridBagConstraints);
    }

    /**
     * Resets the force editor to "add a new force" state, so it does not retain the previously edited force's values.
     */
    private void newForce() {
        forceEditorPanel.reset();
        editedForceId = null;
    }

    /**
     * Closes the dialog, prompting first if there are unsaved changes.
     */
    private void closeWithConfirmation() {
        if (isDirty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.unsavedChanges.message"),
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.unsavedChanges.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    /**
     * Records the current editor state as the "clean" baseline. Called at construction and after a save or load.
     */
    private void captureBaseline() {
        capturedState = currentSerializedState();
    }

    /**
     * @return whether the editor state differs from the last captured baseline
     */
    private boolean isDirty() {
        return !currentSerializedState().equals(capturedState);
    }

    /**
     * Serializes a snapshot of the current editor state - the live template plus the not-yet-committed panel values -
     * so it can be compared against the baseline. Uses a clone so the working template is not mutated.
     */
    private String currentSerializedState() {
        ScenarioTemplate snapshot = scenarioTemplate.clone();
        if (templatePropertiesPanel != null) {
            templatePropertiesPanel.writeInto(snapshot);
            mapParametersPanel.writeInto(snapshot.mapParameters);
            modifiersPanel.writeInto(snapshot.scenarioModifiers);
        }
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            snapshot.Serialize(printWriter);
        }
        return stringWriter.toString();
    }


    /**
     * Re-reads the force roster into the force table and shows or hides the list based on whether any forces exist.
     */
    private void renderForceList() {
        forceTableModel.setForces(scenarioTemplate.getAllScenarioForces());
        if (forceListContainer != null) {
            forceListContainer.setVisible(!scenarioTemplate.getScenarioForces().isEmpty());
        }
    }

    /**
     * Loads the selected force into the force editor for editing.
     */
    private void editSelectedForce() {
        int row = forceTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        ScenarioForceTemplate forceTemplate = forceTableModel.getForceAt(row);
        forceEditorPanel.loadForce(forceTemplate);
        // Remember which force we are editing so committing updates it in place (and handles a rename) instead of
        // adding a duplicate.
        editedForceId = forceTemplate.getForceName();
    }

    /**
     * Removes the selected force from the roster.
     */
    private void removeSelectedForce() {
        int row = forceTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String forceId = forceTableModel.getForceAt(row).getForceName();
        scenarioTemplate.getScenarioForces().remove(forceId);

        // If we just removed the force being edited, drop back to "add new" mode so a later commit does not resurrect
        // it under a stale ID.
        if (forceId.equals(editedForceId)) {
            editedForceId = null;
        }

        forceEditorPanel.setAvailableForceIds(scenarioTemplate.getScenarioForces().keySet());
        renderForceList();
        revalidate();
        repaint();
    }

    /**
     * Event handler for the "Save" button.
     */
    private void saveTemplateButtonHandler() {
        // Validate the free-text map dimensions before mutating anything, so an invalid entry aborts the save cleanly
        // instead of throwing mid-way and leaving the template partially updated.
        String dimensionErrors = mapParametersPanel.validateInput();
        if (!dimensionErrors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                  dimensionErrors,
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.invalidMapParameters.title"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        templatePropertiesPanel.writeInto(scenarioTemplate);
        mapParametersPanel.writeInto(scenarioTemplate.mapParameters);
        modifiersPanel.writeInto(scenarioTemplate.scenarioModifiers);

        FileDialogs.saveScenarioTemplate((JFrame) getOwner(), scenarioTemplate)
              .ifPresent(file -> {
                  scenarioTemplate.Serialize(file);
                  captureBaseline();
              });
    }

    /**
     * Event handler for when the load button is cleared. Invokes deserialization functionality for user-selected file,
     * then reloads all UI elements.
     */
    private void loadTemplateButtonHandler() {
        File file = FileDialogs.openScenarioTemplate((JFrame) getOwner()).orElse(null);
        if (file == null) {
            return;
        }

        scenarioTemplate = ScenarioTemplate.Deserialize(file);

        if (scenarioTemplate == null) {
            JOptionPane.showMessageDialog(this,
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.loadError.message"),
                  getTextAt(RESOURCE_BUNDLE, "ScenarioTemplateEditorDialog.loadError.title"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // A freshly loaded template starts in "add new" mode; any prior edit context is stale.
        editedForceId = null;

        getContentPane().removeAll();
        globalPanel.removeAll();
        initComponents();
        pack();
        validate();
        // Do not re-register user preferences here. The window preference was registered once when the dialog was
        // constructed, and reloading a template rebuilds the contents but not the window itself; calling
        // setUserPreferences() again would double-manage the same JWindowPreference.
        captureBaseline();
    }

    /**
     * General event handler for button clicks on this dialog. Examines the action command and invokes appropriate
     * method.
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        if (SAVE_TEMPLATE_COMMAND.equals(evt.getActionCommand())) {
            saveTemplateButtonHandler();
        } else if (LOAD_TEMPLATE_COMMAND.equals(evt.getActionCommand())) {
            loadTemplateButtonHandler();
        }
    }

    /**
     * Helper method that hides or reveals the force editor section.
     */
    private void toggleForcePanelVisibility() {
        forceEditorPanel.setVisible(!forceEditorPanel.isVisible());
        forceListContainer.setVisible(!forceListContainer.isVisible() &&
                                            !scenarioTemplate.getScenarioForces().isEmpty());
    }

}

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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;

import megamek.client.ui.panels.abstractPanels.AbstractScrollablePanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.EntityWeightClass;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.digitalGM.stratCon.StratConBiomeManifest;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;

/**
 * Handles editing, saving and loading of scenario template definitions.
 *
 * @author NickAragua
 */
public class ScenarioTemplateEditorDialog extends JDialog implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioTemplateEditorDialog.class);

    private final static String SAVE_TEMPLATE_COMMAND = "SAVE_TEMPLATE";
    private final static String LOAD_TEMPLATE_COMMAND = "LOAD_TEMPLATE";

    private final JFrame frame;

    JPanel panForceList;
    TemplatePropertiesPanel templatePropertiesPanel;
    MapParametersPanel mapParametersPanel;
    ModifiersPanel modifiersPanel;
    ObjectivesPanel objectivesPanel;
    ForceEditorPanel forceEditorPanel;

    AbstractScrollablePanel globalPanel;

    JScrollPane forceScrollPane;

    // the scenario template we're working on
    ScenarioTemplate scenarioTemplate = new ScenarioTemplate();

    // the ID of the force currently loaded into the force editor via "Edit", or null when adding a new force. Used to
    // update a force in place (and clean up its old key on rename) rather than orphaning or overwriting entries.
    String editedForceId = null;

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
    }

    /**
     * Initialize dialog components.
     */
    protected void initComponents() {
        this.setTitle("Scenario Template Editor");
        getContentPane().setLayout(new GridLayout());

        globalPanel = new DefaultMHQScrollablePanel(frame, "globalPanel", new GridBagLayout());

        JScrollPane globalScrollPane = new FastJScrollPane(globalPanel);
        globalScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        globalScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(globalScrollPane);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupTemplateProperties(gbc);
        setupObjectiveEditUI(gbc);
        setupForceEditorHeaders(gbc);
        setupForceEditor(gbc);
        initializeForceList(gbc);
        setupMapParameters(gbc);
        setupBottomButtons(gbc);

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

    private void setupObjectiveEditUI(GridBagConstraints gbc) {
        objectivesPanel = new ObjectivesPanel();
        objectivesPanel.load(scenarioTemplate);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        globalPanel.add(objectivesPanel, gbc);
    }

    /**
     * Worker function that sets up top-level headers for the force template editor section.
     *
     */
    private void setupForceEditorHeaders(GridBagConstraints gridBagConstraints) {
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;

        JLabel lblForces = new JLabel("Participating Forces:");
        gridBagConstraints.gridy++;
        globalPanel.add(lblForces, gridBagConstraints);

        JButton btnHideShow = new JButton("Hide/Show");
        btnHideShow.addActionListener(evt -> toggleForcePanelVisibility());

        gridBagConstraints.gridx++;
        int previousAnchor = gridBagConstraints.anchor;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        globalPanel.add(btnHideShow, gridBagConstraints);
        gridBagConstraints.anchor = previousAnchor;
    }

    /**
     * Worker function that sets up UI elements for the force template editor.
     *
     */
    private void setupForceEditor(GridBagConstraints externalGBC) {
        forceEditorPanel = new ForceEditorPanel(readMulFileNames());
        forceEditorPanel.setOnSave(this::commitForce);
        forceEditorPanel.setAvailableForceIds(scenarioTemplate.getScenarioForces().keySet());

        externalGBC.gridx = 0;
        externalGBC.gridy++;
        externalGBC.gridwidth = GridBagConstraints.REMAINDER;
        globalPanel.add(forceEditorPanel, externalGBC);
        externalGBC.gridheight = 1;
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
                  "Invalid Force Configuration",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        ScenarioForceTemplate sft = forceEditorPanel.buildForceTemplate();
        ForceRosterEditor.CommitResult commitResult = ForceRosterEditor.commit(scenarioTemplate.getScenarioForces(),
              editedForceId,
              sft);
        if (!commitResult.committed()) {
            JOptionPane.showMessageDialog(this,
                  commitResult.errorMessage(),
                  "Invalid Force Configuration",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Back to "add new" mode once the edit has been committed.
        editedForceId = null;
        forceEditorPanel.setAvailableForceIds(scenarioTemplate.getScenarioForces().keySet());
        renderForceList();
        pack();
        repaint();
    }


    /**
     * Worker function called when initializing the dialog to place the force template list on the content pane.
     *
     * @param gbc Grid bag constraints.
     */
    private void initializeForceList(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        panForceList = new JPanel(new GridBagLayout());

        renderForceList();

        forceScrollPane = new FastJScrollPane(panForceList);
        forceScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        forceScrollPane.setVisible(false);

        globalPanel.add(forceScrollPane, gbc);
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
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        pnlMapRow.add(mapParametersPanel, localGbc);

        modifiersPanel = new ModifiersPanel(AtBScenarioModifier.getOrderedModifierKeys());
        modifiersPanel.load(scenarioTemplate.scenarioModifiers);
        localGbc.gridx = 1;
        localGbc.insets = new Insets(0, 15, 0, 0);
        pnlMapRow.add(modifiersPanel, localGbc);

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

        JButton btnSave = new JButton("Save");
        btnSave.setActionCommand(SAVE_TEMPLATE_COMMAND);
        btnSave.addActionListener(this);
        globalPanel.add(btnSave, gridBagConstraints);

        gridBagConstraints.gridx++;
        JButton btnLoad = new JButton("Load");
        btnLoad.setActionCommand(LOAD_TEMPLATE_COMMAND);
        btnLoad.addActionListener(this);
        globalPanel.add(btnLoad, gridBagConstraints);
    }

    /**
     * Worker function to re-draw the force template list.
     */
    private void renderForceList() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.ipadx = 5;
        gbc.ipady = 5;

        panForceList.removeAll();

        if (forceScrollPane != null) {
            forceScrollPane.setVisible(!scenarioTemplate.getScenarioForces().isEmpty());
        }

        gbc.gridy++;
        // headers
        JLabel lblGenerationOrder = new JLabel("Order");
        lblGenerationOrder.setBorder(new LineBorder(Color.GRAY));
        panForceList.add(lblGenerationOrder, gbc);

        JLabel lblForceNameHeader = new JLabel("Force ID");
        gbc.gridx++;
        lblForceNameHeader.setBorder(new LineBorder(Color.GRAY));
        panForceList.add(lblForceNameHeader, gbc);

        JLabel lblForceAlignmentHeader = new JLabel("Alignment");
        lblForceAlignmentHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblForceAlignmentHeader, gbc);

        JLabel lblGenerationMethodHeader = new JLabel("Generation");
        lblGenerationMethodHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblGenerationMethodHeader, gbc);

        JLabel lblMultiplierHeader = new JLabel("<html>Multiplier /<br/> Unit Count</html>");
        lblMultiplierHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblMultiplierHeader, gbc);

        JLabel lblDeploymentZonesHeader = new JLabel("Deployment");
        lblDeploymentZonesHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblDeploymentZonesHeader, gbc);

        JLabel lblDestinationZonesHeader = new JLabel("Destination");
        lblDestinationZonesHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblDestinationZonesHeader, gbc);

        JLabel lblRetreatThresholdHeader = new JLabel("Retreat %");
        lblRetreatThresholdHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblRetreatThresholdHeader, gbc);

        JLabel lblAllowedUnitTypesHeader = new JLabel("Unit Type");
        lblAllowedUnitTypesHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblAllowedUnitTypesHeader, gbc);

        JLabel lblWeightClassHeader = new JLabel("Max Wt Class");
        lblWeightClassHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblWeightClassHeader, gbc);

        JLabel lblArrivalTurnHeader = new JLabel("Arrival Turn");
        lblArrivalTurnHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblArrivalTurnHeader, gbc);

        JLabel lblReinforceLinkedHeader = new JLabel("Reinforce?");
        lblReinforceLinkedHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblReinforceLinkedHeader, gbc);

        JLabel lblContributesToBVHeader = new JLabel("+ BV?");
        lblContributesToBVHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblContributesToBVHeader, gbc);

        JLabel lblContributesToUnitCountHeader = new JLabel("+ Unit Count?");
        lblContributesToUnitCountHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblContributesToUnitCountHeader, gbc);

        JLabel lblMapSizeHeader = new JLabel("+ Map size?");
        lblMapSizeHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblMapSizeHeader, gbc);

        gbc.gridy++;
        gbc.gridx = 0;

        List<ScenarioForceTemplate> forceTemplateList = new ArrayList<>(scenarioTemplate.getAllScenarioForces());
        Collections.sort(forceTemplateList);

        for (ScenarioForceTemplate sft : forceTemplateList) {
            JLabel lblForceOrder = new JLabel(Integer.toString(sft.getGenerationOrder()));
            panForceList.add(lblForceOrder, gbc);

            JLabel lblForceName = new JLabel(sft.getForceName());
            gbc.gridx++;
            panForceList.add(lblForceName, gbc);

            JLabel lblForceAlignment = new JLabel(ScenarioForceTemplate.FORCE_ALIGNMENTS[sft.getForceAlignment()]);
            gbc.gridx++;
            panForceList.add(lblForceAlignment, gbc);

            JLabel lblGenerationMethod = new JLabel(ScenarioForceTemplate.FORCE_GENERATION_METHODS[sft.getGenerationMethod()]);
            gbc.gridx++;
            panForceList.add(lblGenerationMethod, gbc);

            JLabel lblMultiplier = new JLabel();
            gbc.gridx++;

            if (!sft.isPlayerForce() &&
                      (sft.getGenerationMethod() !=
                             ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {
                lblMultiplier.setText(((Double) sft.getForceMultiplier()).toString());
                panForceList.add(lblMultiplier, gbc);
            } else if (!sft.isPlayerForce() &&
                             (sft.getGenerationMethod() ==
                                    ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {

                if (sft.getFixedUnitCount() >= 0) {
                    lblMultiplier.setText(Integer.toString(sft.getFixedUnitCount()));
                } else {
                    lblMultiplier.setText("Lance");
                }
                panForceList.add(lblMultiplier, gbc);
            }

            JLabel lblDeploymentZones = getLblDeploymentZones(sft);
            gbc.gridx++;
            panForceList.add(lblDeploymentZones, gbc);

            JLabel lblDestinationZones = new JLabel(ScenarioForceTemplate.BOT_DESTINATION_ZONES[sft.getDestinationZone()]);
            gbc.gridx++;
            panForceList.add(lblDestinationZones, gbc);

            JLabel lblRetreatThreshold = new JLabel(Integer.toString(sft.getRetreatThreshold()));
            gbc.gridx++;
            panForceList.add(lblRetreatThreshold, gbc);

            JLabel lblAllowedUnitTypes = new JLabel(sft.getAllowedUnitTypeName());
            gbc.gridx++;
            if (!sft.isPlayerForce()) {
                panForceList.add(lblAllowedUnitTypes, gbc);
            }

            JLabel lblWeightClass = new JLabel(EntityWeightClass.getClassName(sft.getMaxWeightClass()));
            gbc.gridx++;
            if (!sft.isPlayerForce()) {
                panForceList.add(lblWeightClass, gbc);
            }

            JLabel lblArrivalTurn = new JLabel(sft.getArrivalTurn() < 0 ?
                                                     ScenarioForceTemplate.SPECIAL_ARRIVAL_TURNS.get(sft.getArrivalTurn()) :
                                                     Integer.toString(sft.getArrivalTurn()));
            gbc.gridx++;
            panForceList.add(lblArrivalTurn, gbc);

            JLabel lblReinforceLinked = new JLabel(sft.getCanReinforceLinked() ? "Yes" : "No");
            gbc.gridx++;
            panForceList.add(lblReinforceLinked, gbc);

            JLabel lblContributesToBV = new JLabel(sft.getContributesToBV() ? "Yes" : "No");
            gbc.gridx++;
            if (!(sft.isEnemyBotForce() || (sft.getForceAlignment() == ForceAlignment.PlanetOwner.ordinal()))) {
                panForceList.add(lblContributesToBV, gbc);
            }

            JLabel lblContributesToUnitCount = new JLabel(sft.getContributesToUnitCount() ? "Yes" : "No");
            gbc.gridx++;
            if (!(sft.isEnemyBotForce() || (sft.getForceAlignment() == ForceAlignment.PlanetOwner.ordinal()))) {
                panForceList.add(lblContributesToUnitCount, gbc);
            }

            JLabel lblMapSize = new JLabel(sft.getContributesToMapSize() ? "Yes" : "No");
            gbc.gridx++;
            panForceList.add(lblMapSize, gbc);

            JButton btnRemoveForce = new JButton("Remove");
            btnRemoveForce.setActionCommand(ForceListCommand.removeCommand(sft.getForceName()));
            btnRemoveForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnRemoveForce, gbc);

            JButton btnEditForce = new JButton("Edit");
            btnEditForce.setActionCommand(ForceListCommand.editCommand(sft.getForceName()));
            btnEditForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnEditForce, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
        }
    }

    private static JLabel getLblDeploymentZones(ScenarioForceTemplate sft) {
        JLabel lblDeploymentZones = new JLabel();
        StringBuilder dzBuilder = new StringBuilder();

        if (!sft.getDeploymentZones().isEmpty()) {
            dzBuilder.append("<html>");
            for (int zone : sft.getDeploymentZones()) {
                dzBuilder.append(ScenarioForceTemplate.DEPLOYMENT_ZONES[zone]);
                dzBuilder.append("<br/>");
            }
            dzBuilder.append("</html>");
        } else {
            dzBuilder.append(ScenarioForceTemplate.FORCE_DEPLOYMENT_SYNC_TYPES[sft.getSyncDeploymentType()
                                                                                     .ordinal()]);
            dzBuilder.append(" as ");
            dzBuilder.append(sft.getSyncedForceName());
        }

        lblDeploymentZones.setText(dzBuilder.toString());
        return lblDeploymentZones;
    }


    /**
     * Event handler for when the "Remove" button is pressed for a particular force template.
     *
     * @param command The command string containing the index of the force to remove.
     */
    private void deleteForceButtonHandler(String command) {
        String forceIndex = ForceListCommand.removeForceId(command);
        scenarioTemplate.getScenarioForces().remove(forceIndex);

        // If we just removed the force being edited, drop back to "add new" mode so a later commit does not resurrect
        // it under a stale ID.
        if (forceIndex.equals(editedForceId)) {
            editedForceId = null;
        }

        forceEditorPanel.setAvailableForceIds(scenarioTemplate.getScenarioForces().keySet());
        renderForceList();
        pack();
        repaint();
    }

    /**
     * Event handler for when the "Edit" button is pressed for a particular force template.
     *
     * @param command The command string containing the index of the force to edit.
     */
    private void editForceButtonHandler(String command) {
        String forceIndex = ForceListCommand.editForceId(command);
        forceEditorPanel.loadForce(scenarioTemplate.getScenarioForces().get(forceIndex));
        // Remember which force we are editing so committing updates it in place (and handles a rename) instead of
        // adding a duplicate.
        editedForceId = forceIndex;
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
                  "Invalid Map Parameters",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        templatePropertiesPanel.writeInto(scenarioTemplate);
        mapParametersPanel.writeInto(scenarioTemplate.mapParameters);
        modifiersPanel.writeInto(scenarioTemplate.scenarioModifiers);

        FileDialogs.saveScenarioTemplate((JFrame) getOwner(), scenarioTemplate)
              .ifPresent(file -> scenarioTemplate.Serialize(file));
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
                  "Error loading specified file. See log for details.",
                  "Load Error",
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
    }

    /**
     * General event handler for button clicks on this dialog. Examines the action command and invokes appropriate
     * method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ForceListCommand.isRemove(e.getActionCommand())) {
            deleteForceButtonHandler(e.getActionCommand());
        } else if (ForceListCommand.isEdit(e.getActionCommand())) {
            editForceButtonHandler(e.getActionCommand());
        } else if (SAVE_TEMPLATE_COMMAND.equals(e.getActionCommand())) {
            saveTemplateButtonHandler();
        } else if (LOAD_TEMPLATE_COMMAND.equals(e.getActionCommand())) {
            loadTemplateButtonHandler();
        }
    }

    /**
     * Helper method that hides or reveals the force editor section.
     */
    private void toggleForcePanelVisibility() {
        forceEditorPanel.setVisible(!forceEditorPanel.isVisible());
        forceScrollPane.setVisible(!forceScrollPane.isVisible() && !scenarioTemplate.getScenarioForces().isEmpty());
    }

}

/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;

/**
 * @author Taharqa
 */
public class AddOrEditKillEntryDialog extends JDialog {
    private static final int ADD_OPERATION  = 1;
    private static final int EDIT_OPERATION = 2;

    private final JFrame    frame;
    private final int       operationType;
    private       Kill      kill;
    private       LocalDate date;
    private final int       missionId;
    private final int       scenarioId;
    private final int       forceId;
    private final Campaign  campaign;

    private JTextField          txtKill;
    private JTextField          txtKiller;
    private JButton             btnDate;
    private MMComboBox<Mission> cboMissionId;
    private ArrayList<Integer>  missionIdList;
    private MMComboBox<String>  cboScenarioId;
    private ArrayList<Integer>  scenarioIdList;

    private static final MMLogger logger = MMLogger.create(AddOrEditKillEntryDialog.class);

    public AddOrEditKillEntryDialog(JFrame parent, boolean modal, UUID killerPerson, String killerUnit, LocalDate entryDate, Campaign campaign) {
        // We default missionId & scenarioId to 0 when adding new kills
        this(parent, modal, ADD_OPERATION, new Kill(killerPerson, "?", killerUnit, entryDate, 0, 0, -1, -1), campaign);
    }

    public AddOrEditKillEntryDialog(JFrame parent, boolean modal, Kill kill, Campaign campaign) {
        this(parent, modal, EDIT_OPERATION, kill, campaign);
    }

    private AddOrEditKillEntryDialog(JFrame parent, boolean modal, int operationType, Kill kill, Campaign c) {
        super(parent, modal);

        campaign = c;

        this.frame         = parent;
        this.kill          = Objects.requireNonNull(kill);
        this.date          = this.kill.getDate();
        this.missionId     = this.kill.getMissionId();
        this.scenarioId    = this.kill.getScenarioId();
        this.forceId       = this.kill.getForceId();
        this.operationType = operationType;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    public Optional<Kill> getKill() {
        return Optional.ofNullable(kill);
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddOrEditKillEntryDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        if (this.operationType == ADD_OPERATION) {
            setTitle(resourceMap.getString("dialogAdd.title"));
        } else {
            setTitle(resourceMap.getString("dialogEdit.title"));
        }
        getContentPane().setLayout(new GridBagLayout());

        JLabel lblKill = new JLabel();
        lblKill.setText(resourceMap.getString("lblKill.text"));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(lblKill, gridBagConstraints);

        txtKill = new JTextField();
        txtKill.setText(kill.getWhatKilled());
        txtKill.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(txtKill, gridBagConstraints);

        JLabel lblKiller = new JLabel();
        lblKiller.setText(resourceMap.getString("lblKiller.text"));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(lblKiller, gridBagConstraints);

        txtKiller = new JTextField();
        txtKiller.setText(kill.getKilledByWhat());
        txtKiller.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(txtKiller, gridBagConstraints);

        JLabel lblDate = new JLabel();
        lblDate.setText(resourceMap.getString("lblDate.text"));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(lblDate, gridBagConstraints);

        btnDate = new JButton();
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.setName("btnDate");
        btnDate.addActionListener(evt -> changeDate());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        getContentPane().add(btnDate, gridBagConstraints);

        JLabel lblMissionId = new JLabel();
        lblMissionId.setText(resourceMap.getString("lblMissionId.text"));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(lblMissionId, gridBagConstraints);

        cboMissionId  = new MMComboBox<>("cboMissionId");
        missionIdList = createIdList(true);
        for (int id : missionIdList) {
            if (id == 0) {
                cboMissionId.addItem(null);
            } else {
                cboMissionId.addItem(campaign.getMission(id));
            }
        }

        // if missionId is valid, default to the option matching missionId
        if (campaign.getMission(missionId) != null) {
            cboMissionId.setSelectedItem(campaign.getMission(missionId));
        }
        cboMissionId.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(cboMissionId, gridBagConstraints);

        JLabel lblScenarioId = new JLabel();
        lblScenarioId.setText(resourceMap.getString("lblScenarioId.text"));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(lblScenarioId, gridBagConstraints);

        cboScenarioId  = new MMComboBox<>("cboScenarioId");
        scenarioIdList = createIdList(false);
        for (int id : scenarioIdList) {
            if (id == 0) {
                cboScenarioId.addItem(null);
            } else {
                cboScenarioId.addItem("(" +
                                      campaign.getScenario(id).getDate() +
                                      ") " +
                                      campaign.getScenario(id).getName());
            }
        }

        // if scenarioId is valid, default to the option matching scenarioId
        if (campaign.getScenario(scenarioId) != null) {
            cboScenarioId.setSelectedItem("(" +
                                          campaign.getScenario(scenarioId).getDate() +
                                          ") " +
                                          campaign.getScenario(scenarioId).getName());
        }
        cboScenarioId.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(cboScenarioId, gridBagConstraints);

        JButton btnOK = new JButton();
        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.EAST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        JButton btnClose = new JButton();
        btnClose.setText(resourceMap.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(AddOrEditKillEntryDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        kill.setWhatKilled(txtKill.getText());
        kill.setKilledByWhat(txtKiller.getText());
        kill.setDate(date);

        // if an invalid mission or scenario was selected default to 0
        if (cboMissionId.getSelectedIndex() == -1) {
            kill.setMissionId(0);
        } else {
            kill.setMissionId(missionIdList.get(cboMissionId.getSelectedIndex()));
        }

        if (cboScenarioId.getSelectedIndex() == -1) {
            kill.setScenarioId(0);
        } else {
            kill.setScenarioId(scenarioIdList.get(cboScenarioId.getSelectedIndex()));
        }

        // we attempt to log the Force ID, but there is a lot of information that could
        // potentially be missing.
        if (forceId == -1) {
            try {
                kill.setForceId(campaign.getPerson(kill.getPilotId()).getUnit().getForceId());
            } catch (Exception ignored) {
            }
        }

        this.setVisible(false);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        kill = null;
        this.setVisible(false);
    }

    private void changeDate() {
        DateChooser dc = new DateChooser(frame, date);
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            date = dc.getDate();
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        }
    }

    private ArrayList<Integer> createIdList(boolean isMission) {
        ArrayList<Integer> idList = new ArrayList<>();

        if (isMission) {
            // the default value should be the first value
            idList.add(0);

            for (Mission mission : campaign.getSortedMissions()) {
                idList.add(mission.getId());
            }
        } else {
            for (Scenario scenario : campaign.getScenarios()) {
                idList.add(scenario.getId());
            }
            // this ensures the default value becomes the first value, when we reverse
            idList.add(0);
            Collections.reverse(idList);
        }

        return idList;
    }
}

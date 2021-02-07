/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.view.CompanyGenerationOptionsPanel;
import mekhq.preferences.PreferencesNode;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class CompanyGenerationDialog extends JDialog {
    //region Variable Declarations
    private JFrame frame;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "", ModalityType.APPLICATION_MODAL);
        setTitle(resources.getString("CompanyGenerationDialog.title"));
        setFrame(frame);
        initialize(campaign);
        managePreferences();
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public CompanyGenerationOptionsPanel getCompanyGenerationOptionsPanel() {
        return companyGenerationOptionsPanel;
    }

    public void setCompanyGenerationOptionsPanel(final CompanyGenerationOptionsPanel companyGenerationOptionsPanel) {
        this.companyGenerationOptionsPanel = companyGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize(final Campaign campaign) {
        setLayout(new BorderLayout());
        add(initializeCompanyGenerationOptionsPanel(campaign), BorderLayout.CENTER);
        add(initializeButtons(), BorderLayout.PAGE_END);

        setMinimumSize(new Dimension(480, 240));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private JPanel initializeCompanyGenerationOptionsPanel(final Campaign campaign) {
        setCompanyGenerationOptionsPanel(new CompanyGenerationOptionsPanel(getFrame(), campaign));
        return getCompanyGenerationOptionsPanel();
    }

    private JPanel initializeButtons() {
        JButton btnCancel = new JButton(resources.getString("Cancel"));
        btnCancel.addActionListener(evt -> setVisible(false));

        JButton btnExport = new JButton(resources.getString("Export"));
        btnExport.addActionListener(evt -> getCompanyGenerationOptionsPanel().exportOptionsToXML());

        JButton btnGenerate = new JButton(resources.getString("Generate"));
        btnGenerate.addActionListener(evt -> getCompanyGenerationOptionsPanel().generate());

        JButton btnRestore = new JButton(resources.getString("RestoreDefaults"));
        btnRestore.addActionListener(evt -> getCompanyGenerationOptionsPanel().setOptions(
                MekHQ.getMekHQOptions().getDefaultCompanyGenerationType()));

        JButton btnImport = new JButton(resources.getString("Import"));
        btnImport.addActionListener(evt -> getCompanyGenerationOptionsPanel().importOptionsFromXML());

        JButton btnApply = new JButton(resources.getString("Apply"));
        btnApply.addActionListener(evt -> {
            getCompanyGenerationOptionsPanel().apply();
            MekHQ.triggerEvent(new OrganizationChangedEvent(getCompanyGenerationOptionsPanel().getCampaign().getForces()));
            setVisible(false);
        });

        // Layout the UI
        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btnCancel)
                                .addComponent(btnExport)
                                .addComponent(btnGenerate, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btnRestore)
                                .addComponent(btnImport)
                                .addComponent(btnApply, GroupLayout.Alignment.TRAILING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(btnCancel)
                                .addComponent(btnExport)
                                .addComponent(btnGenerate))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(btnRestore)
                                .addComponent(btnImport)
                                .addComponent(btnApply))
        );

        return panel;
    }

    private void managePreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(getClass());

        preferences.manage(new JWindowPreference(this));
    }
    //endregion Initialization
}

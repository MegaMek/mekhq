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
package mekhq.gui.panes;

import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQSplitPane;

import javax.swing.*;
import java.awt.*;

public class UnitMarketPane extends AbstractMHQSplitPane {
    //region Variable Declarations
    private final Campaign campaign;

    //region Left Panel
    // Filters
    private JCheckBox chkShowMechs;
    private JCheckBox chkShowVehicles;
    private JCheckBox chkShowAerospace;
    private JCheckBox chkFilterByPercentageOfCost;
    private JSpinner spnCostPercentageThreshold;

    // Unit 
    //endregion Left Panel

    //region Right Panel
    private EntityViewPane entityViewPane;
    //endregion Right Panel
    //endregion Variable Declarations

    //region Constructors
    public UnitMarketPane(final JFrame frame, final Campaign campaign) {
        super(frame, "UnitMarketPane");
        this.campaign = campaign;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    //region Left Panel
    //region Filters
    public JCheckBox getChkShowMechs() {
        return chkShowMechs;
    }

    public void setChkShowMechs(final JCheckBox chkShowMechs) {
        this.chkShowMechs = chkShowMechs;
    }

    public JCheckBox getChkShowVehicles() {
        return chkShowVehicles;
    }

    public void setChkShowVehicles(final JCheckBox chkShowVehicles) {
        this.chkShowVehicles = chkShowVehicles;
    }

    public JCheckBox getChkShowAerospace() {
        return chkShowAerospace;
    }

    public void setChkShowAerospace(final JCheckBox chkShowAerospace) {
        this.chkShowAerospace = chkShowAerospace;
    }

    public JCheckBox getChkFilterByPercentageOfCost() {
        return chkFilterByPercentageOfCost;
    }

    public void setChkFilterByPercentageOfCost(final JCheckBox chkFilterByPercentageOfCost) {
        this.chkFilterByPercentageOfCost = chkFilterByPercentageOfCost;
    }

    public JSpinner getSpnCostPercentageThreshold() {
        return spnCostPercentageThreshold;
    }

    public void setSpnCostPercentageThreshold(final JSpinner spnCostPercentageThreshold) {
        this.spnCostPercentageThreshold = spnCostPercentageThreshold;
    }
    //endregion Filters
    //endregion Left Panel

    //region Right Panel
    public EntityViewPane getEntityViewPane() {
        return entityViewPane;
    }

    public void setEntityViewPane(final EntityViewPane entityViewPane) {
        this.entityViewPane = entityViewPane;
    }
    //endregion Right Panel
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Component createLeftComponent() {
        ???
    }

    @Override
    protected Component createRightComponent() {
        setEntityViewPane(new EntityViewPane(getFrame(), null));
        return getEntityViewPane();
    }

    private JPanel createFiltersPanel() {
        // Create Panel Components
        setChkShowMechs(new JCheckBox(resources.getString("chkShowMechs.text")));
        getChkShowMechs().setToolTipText(resources.getString("chkShowMechs.toolTipText"));
        getChkShowMechs().setName("chkShowMechs");

        setChkShowVehicles(new JCheckBox(resources.getString("chkShowVehicles.text")));
        getChkShowVehicles().setToolTipText(resources.getString("chkShowVehicles.toolTipText"));
        getChkShowVehicles().setName("chkShowVehicles");

        setChkShowAerospace(new JCheckBox(resources.getString("chkShowAerospace.text")));
        getChkShowAerospace().setToolTipText(resources.getString("chkShowAerospace.toolTipText"));
        getChkShowAerospace().setName("chkShowAerospace");

        setChkFilterByPercentageOfCost(new JCheckBox(resources.getString("chkFilterByPercentageOfCost.text")));
        getChkFilterByPercentageOfCost().setToolTipText(resources.getString("chkFilterByPercentageOfCost.toolTipText"));
        getChkFilterByPercentageOfCost().setName("chkFilterByPercentageOfCost");
        getChkFilterByPercentageOfCost().getAccessibleContext().setAccessibleDescription(resources.getString("chkFilterByPercentageOfCost.accessibleDescription"));

        setSpnCostPercentageThreshold(new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10)));
        getSpnCostPercentageThreshold().setToolTipText(resources.getString("spnFilterByPercentageOfCost.toolTipText"));
        getSpnCostPercentageThreshold().setName("spnCostPercentageThreshold");
        getSpnCostPercentageThreshold().getAccessibleContext().setAccessibleDescription(resources.getString("spnFilterByPercentageOfCost.accessibleDescription"));

        JLabel lblCostPercentageThreshold = new JLabel(resources.getString("lblCostPercentageThreshold.text"));
        lblCostPercentageThreshold.setToolTipText(resources.getString("spnFilterByPercentageOfCost.toolTipText"));
        lblCostPercentageThreshold.setName("lblCostPercentageThreshold");
        lblCostPercentageThreshold.getAccessibleContext().setAccessibleDescription(resources.getString("lblCostPercentageThreshold.accessibleDescription"));
        lblCostPercentageThreshold.setLabelFor(getSpnCostPercentageThreshold());

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelRandomizationPanel.title")));
        panel.setName("personnelRandomizationPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkShowMechs())
                                .addComponent(getChkShowVehicles())
                                .addComponent(getChkShowAerospace(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkFilterByPercentageOfCost())
                                .addComponent(getSpnCostPercentageThreshold())
                                .addComponent(lblCostPercentageThreshold, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkShowMechs())
                                .addComponent(getChkShowVehicles())
                                .addComponent(getChkShowAerospace()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkFilterByPercentageOfCost())
                                .addComponent(getSpnCostPercentageThreshold())
                                .addComponent(lblCostPercentageThreshold))
        );
        return panel;
    }

    private JPanel createUnitImagePanel() {
        // Create Panel Components


        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelRandomizationPanel.title")));
        panel.setName("personnelRandomizationPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkShowMechs())
                                .addComponent(getChkShowVehicles())
                                .addComponent(getChkShowAerospace(), GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkShowMechs())
                                .addComponent(getChkShowVehicles())
                                .addComponent(getChkShowAerospace()))
        );
        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        preferences.manage(new JToggleButtonPreference(getChkShowMechs()));
        preferences.manage(new JToggleButtonPreference(getChkShowVehicles()));
        preferences.manage(new JToggleButtonPreference(getChkShowAerospace()));
        preferences.manage(new JToggleButtonPreference(getChkFilterByPercentageOfCost()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnCostPercentageThreshold()));
        preferences.manage(new JTablePreference(tableUnits));
    }
    //endregion Initialization
}

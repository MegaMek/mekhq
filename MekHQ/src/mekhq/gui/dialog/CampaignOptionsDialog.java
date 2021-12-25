/*
 * Copyright (C) 2009-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.FileDialogs;
import mekhq.gui.SpecialAbilityPanel;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CampaignOptionsDialog extends AbstractMHQButtonDialog {

    //region Constructors
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog", new EncodeControl()),
                "CampaignOptionsDialog", "CampaignOptionsDialog.title");
        initialize();
        setOptions(campaign.getCampaignOptions(), campaign.getRandomSkillPreferences());
    }
    //endregion Constructors

    //region Initialization
    //region Center Pane
    @Override
    protected Container createCenterPane() {
        //region Variable Declaration and Initialisation
        GridBagConstraints gridBagConstraints;
        int gridy = 0;
        int gridx = 0;

        //endregion Variable Declaration and Initialisation


        //endregion Against the Bot Tab

        return getOptionsPane();
    }
    //endregion Center Pane

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 0));

        panel.add(new MMButton("btnOkay", resources, "btnOkay.text", null,
                this::okButtonActionPerformed));

        panel.add(new MMButton("btnSavePreset", resources, "btnSavePreset.text",
                "btnSavePreset.toolTipText", evt -> btnSaveActionPerformed()));

        panel.add(new MMButton("btnLoadPreset", resources, "btnLoadPreset.text",
                "btnLoadPreset.toolTipText", evt -> {
            final CampaignPresetSelectionDialog presetSelectionDialog = new CampaignPresetSelectionDialog(getFrame());
            if (presetSelectionDialog.showDialog().isConfirmed()) {
                applyPreset(presetSelectionDialog.getSelectedPreset());
            }
        }));

        panel.add(new MMButton("btnCancel", resources, "btnCancel.text", null,
                this::cancelActionPerformed));

        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getOptionsPane()));
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okButtonActionPerformed(final ActionEvent evt) {
        if (!txtName.getText().isBlank()) {
            updateOptions();
            setResult(DialogResult.CONFIRMED);
            setVisible(false);
        }
    }

    private void btnSaveActionPerformed() {
        if (txtName.getText().isBlank()) {
            return;
        }
        updateOptions();
        setResult(DialogResult.CONFIRMED);

        final CreateCampaignPresetDialog createCampaignPresetDialog
                = new CreateCampaignPresetDialog(getFrame(), getCampaign(), null);
        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            setVisible(false);
            return;
        }
        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            setVisible(false);
            return;
        }
        preset.writeToFile(getFrame(),
                FileDialogs.saveCampaignPreset(getFrame(), preset).orElse(null));
        setVisible(false);
    }
    //endregion Button Actions
}

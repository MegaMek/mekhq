/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class MekHQUnitSelectorDialog extends AbstractUnitSelectorDialog {
    //region Variable Declarations
    private Campaign campaign;
    private boolean addToCampaign;
    private UnitOrder selectedUnit = null;

    private static final String TARGET_UNKNOWN = "--";
    //endregion Variable Declarations

    public MekHQUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog,
                                   Campaign campaign, boolean addToCampaign) {
        super(frame, unitLoadingDialog);
        this.campaign = campaign;
        this.addToCampaign = addToCampaign;

        updateOptionValues();
        initialize();
        run();
    }

    @Override
    public void updateOptionValues() {
        gameOptions = campaign.getGameOptions();
        enableYearLimits = campaign.getCampaignOptions().limitByYear();
        allowedYear = campaign.getGameYear();
        canonOnly = campaign.getCampaignOptions().allowCanonOnly();
        gameTechLevel = campaign.getCampaignOptions().getTechLevel();

        if (campaign.getCampaignOptions().allowClanPurchases() && campaign.getCampaignOptions().allowISPurchases()) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
        } else if (campaign.getCampaignOptions().allowClanPurchases()) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_CLAN;
        } else {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS;
        }
    }

    //region Button Methods
    @Override
    protected JPanel createButtonsPanel() {
        JPanel panelButtons = new JPanel(new GridBagLayout());

        if (addToCampaign) {
            // This is used for the buy command in MekHQ, named buttonSelect because of how it is used elsewhere
            buttonSelect = new JButton(Messages.getString("MechSelectorDialog.Buy", TARGET_UNKNOWN));
            buttonSelect.setName("buttonBuy");
            buttonSelect.addActionListener(this);
            panelButtons.add(buttonSelect, new GridBagConstraints());

            if (campaign.isGM()) {
                // This is used as a GM add, the name is because of how it is used in MegaMek and MegaMekLab
                buttonSelectClose = new JButton(Messages.getString("MechSelectorDialog.AddGM"));
                buttonSelectClose.setName("buttonAddGM");
                buttonSelectClose.addActionListener(this);
                panelButtons.add(buttonSelectClose, new GridBagConstraints());
            }

            // This closes the dialog
            buttonClose = new JButton(Messages.getString("Close"));
            buttonClose.setName("buttonClose");
            buttonClose.addActionListener(this);
        } else {
            buttonSelectClose = new JButton(Messages.getString("MechSelectorDialog.Add"));
            buttonSelectClose.setName("buttonAdd");
            //the actual work will be done by whatever called this
            buttonSelectClose.addActionListener(evt -> setVisible(false));
            panelButtons.add(buttonSelectClose, new GridBagConstraints());

            // This closes the dialog
            buttonClose = new JButton(Messages.getString("Cancel"));
            buttonClose.setName("buttonCancel");
            buttonClose.addActionListener(evt -> {
                selectedUnit = null;
                setVisible(false);
            });
        }
        panelButtons.add(buttonClose, new GridBagConstraints());

        // This displays the BV of the selected unit
        buttonShowBV = new JButton(Messages.getString("MechSelectorDialog.BV"));
        buttonShowBV.setName("buttonShowBV");
        buttonShowBV.addActionListener(this);
        panelButtons.add(buttonShowBV, new GridBagConstraints());

        return panelButtons;
    }

    @Override
    protected void select(boolean isGM) {
        if (getSelectedEntity() != null) {
            if (isGM) {
                campaign.addUnit(selectedUnit.getEntity(), false, 0);
            } else {
                campaign.getShoppingList().addShoppingItem(selectedUnit, 1, campaign);
            }
        }
    }
    //endregion Button Methods

    /**
     * We need to override this to add some MekHQ specific functionality, namely changing button
     * names when the selected entity is chosen
     *
     * @return selectedEntity, or null if there isn't one
     */
    @Override
    public Entity getSelectedEntity() {
        Entity entity = super.getSelectedEntity();
        if (entity == null) {
            selectedUnit = null;
            buttonSelect.setEnabled(false);
            buttonSelect.setText(Messages.getString("MechSelectorDialog.Buy", TARGET_UNKNOWN));
            buttonSelect.setToolTipText(null);
        } else {
            selectedUnit = new UnitOrder(entity, campaign);
            buttonSelect.setEnabled(true);
            Person logisticsPerson = campaign.getLogisticsPerson();
            buttonSelect.setText(Messages.getString("MechSelectorDialog.Buy",
                    campaign.getTargetForAcquisition(selectedUnit, logisticsPerson, false)
                            .getValueAsString()));
            buttonSelect.setToolTipText(campaign.getTargetForAcquisition(selectedUnit,
                    logisticsPerson, false).getDesc());
        }
        return entity;
    }

    @Override
    protected Entity refreshUnitView() {
        return super.refreshUnitView();
    }

    @Override
    protected void filterUnits() {
        RowFilter<MechTableModel, Integer> unitTypeFilter;

        List<Integer> techLevels = new ArrayList<>();
        for (Integer selectedIdx : listTechLevel.getSelectedIndices()) {
            techLevels.add(techLevelListToIndex.get(selectedIdx));
        }
        final Integer[] nTypes = new Integer[techLevels.size()];
        techLevels.toArray(nTypes);

        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        final boolean checkSupportVee = Messages.getString("MechSelectorDialog.SupportVee")
                .equals(comboUnitType.getSelectedItem());
        //If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<MechTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends MechTableModel, ? extends Integer> entry) {
                    MechTableModel mechModel = entry.getModel();
                    MechSummary mech = mechModel.getMechSummary(entry.getIdentifier());
                    ITechnology tech = UnitTechProgression.getProgression(mech, campaign.getTechFaction(), true);
                    boolean techLevelMatch = false;
                    int type = enableYearLimits ? mech.getType(allowedYear) : mech.getType();
                    for (int tl : nTypes) {
                        if (type == tl) {
                            techLevelMatch = true;
                            break;
                        }
                    }
                    if (
                            /* year limits */
                            (!enableYearLimits || (mech.getYear() <= allowedYear))
                            /* Clan/IS limits */
                            && (campaign.getCampaignOptions().allowClanPurchases() || !TechConstants.isClan(mech.getType()))
                            && (campaign.getCampaignOptions().allowISPurchases() || TechConstants.isClan(mech.getType()))
                            /* Canon */
                            && (!canonOnly || mech.isCanon())
                            /* Weight */
                            && ((nClass == mech.getWeightClass()) || (nClass == EntityWeightClass.SIZE))
                            /* Technology Level */
                            && ((null != tech) && campaign.isLegal(tech))
                            && (techLevelMatch)
                            /* Support Vehicles */
                            && ((nUnit == -1)
                                    || (!checkSupportVee && mech.getUnitType().equals(UnitType.getTypeName(nUnit)))
                                    || (checkSupportVee && mech.isSupport()))
                            /* Advanced Search */
                            && ((searchFilter == null) || MechSearchFilter.isMatch(mech, searchFilter))
                    ) {
                        if (textFilter.getText().length() > 0) {
                            String text = textFilter.getText();
                            return mech.getName().toLowerCase().contains(text.toLowerCase());
                        }
                        return true;
                    }
                    return false;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
    }
}

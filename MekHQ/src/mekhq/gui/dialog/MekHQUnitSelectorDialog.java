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
import mekhq.campaign.parts.enums.PartQuality;
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
        enableYearLimits = campaign.getCampaignOptions().isLimitByYear();
        allowedYear = campaign.getGameYear();
        canonOnly = campaign.getCampaignOptions().isAllowCanonOnly();
        gameTechLevel = campaign.getCampaignOptions().getTechLevel();
        eraBasedTechLevel = campaign.getCampaignOptions().isVariableTechLevel();

        if (campaign.getCampaignOptions().isAllowClanPurchases() && campaign.getCampaignOptions().isAllowISPurchases()) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
        } else if (campaign.getCampaignOptions().isAllowClanPurchases()) {
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
            buttonSelect = new JButton(Messages.getString("MekSelectorDialog.Buy", TARGET_UNKNOWN));
            buttonSelect.setName("buttonBuy");
            buttonSelect.addActionListener(this);
            panelButtons.add(buttonSelect, new GridBagConstraints());

            if (campaign.isGM()) {
                // This is used as a GM add, the name is because of how it is used in MegaMek and MegaMekLab
                buttonSelectClose = new JButton(Messages.getString("MekSelectorDialog.AddGM"));
                buttonSelectClose.setName("buttonAddGM");
                buttonSelectClose.addActionListener(this);
                panelButtons.add(buttonSelectClose, new GridBagConstraints());
            }

            // This closes the dialog
            buttonClose = new JButton(Messages.getString("Close"));
            buttonClose.setName("buttonClose");
            buttonClose.addActionListener(this);
        } else {
            buttonSelectClose = new JButton(Messages.getString("MekSelectorDialog.Add"));
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
        buttonShowBV = new JButton(Messages.getString("MekSelectorDialog.BV"));
        buttonShowBV.setName("buttonShowBV");
        buttonShowBV.addActionListener(this);
        panelButtons.add(buttonShowBV, new GridBagConstraints());

        return panelButtons;
    }

    @Override
    protected void select(boolean isGM) {
        if (getSelectedEntity() != null) {
            if (isGM) {
                PartQuality quality = PartQuality.QUALITY_D;

                if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
                    quality = UnitOrder.getRandomUnitQuality(0);
                }

                campaign.addNewUnit(selectedUnit.getEntity(), false, 0, quality);
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
            if (addToCampaign) {
                buttonSelect.setEnabled(false);
                buttonSelect.setText(Messages.getString("MekSelectorDialog.Buy", TARGET_UNKNOWN));
                buttonSelect.setToolTipText(null);
            }
        } else {
            selectedUnit = new UnitOrder(entity, campaign);
            if (addToCampaign) {
                buttonSelect.setEnabled(true);
                final TargetRoll target = campaign.getTargetForAcquisition(selectedUnit);
                buttonSelect.setText(Messages.getString("MekSelectorDialog.Buy",
                        target.getValueAsString()));
                buttonSelect.setToolTipText(target.getDesc());
            }
        }

        return entity;
    }

    @Override
    protected Entity refreshUnitView() {
        Entity selectedEntity = super.refreshUnitView();
        if (selectedEntity != null) {
            labelImage.setIcon(new ImageIcon(selectedUnit.getImage(this)));
        } else {
            labelImage.setIcon(null);
        }

        return selectedEntity;
    }

    @Override
    protected void filterUnits() {
        RowFilter<MekTableModel, Integer> unitTypeFilter;

        List<Integer> techLevels = new ArrayList<>();
        for (Integer selectedIdx : listTechLevel.getSelectedIndices()) {
            techLevels.add(techLevelListToIndex.get(selectedIdx));
        }
        final Integer[] nTypes = new Integer[techLevels.size()];
        techLevels.toArray(nTypes);

        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex() - 1;
        final boolean checkSupportVee = Messages.getString("MekSelectorDialog.SupportVee")
                .equals(comboUnitType.getSelectedItem());
        // If the current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends MekTableModel, ? extends Integer> entry) {
                    MekTableModel mekModel = entry.getModel();
                    MekSummary mek = mekModel.getMekSummary(entry.getIdentifier());
                    ITechnology tech = UnitTechProgression.getProgression(mek, campaign.getTechFaction(), true);
                    boolean techLevelMatch = false;
                    int type = enableYearLimits ? mek.getType(allowedYear) : mek.getType();
                    for (int tl : nTypes) {
                        if (type == tl) {
                            techLevelMatch = true;
                            break;
                        }
                    }

                    if (
                            /* year limits */
                            (!enableYearLimits || (mek.getYear() <= allowedYear))
                            /* Clan/IS limits */
                            && (campaign.getCampaignOptions().isAllowClanPurchases() || !TechConstants.isClan(mek.getType()))
                            && (campaign.getCampaignOptions().isAllowISPurchases() || TechConstants.isClan(mek.getType()))
                            /* Canon */
                            && (!canonOnly || mek.isCanon())
                            /* Weight */
                            && ((nClass == mek.getWeightClass()) || (nClass == EntityWeightClass.SIZE))
                            /* Technology Level */
                            && ((null != tech) && campaign.isLegal(tech))
                            && (techLevelMatch)
                            /* Support Vehicles */
                            && ((nUnit == -1)
                                    || (!checkSupportVee && mek.getUnitType().equals(UnitType.getTypeName(nUnit)))
                                    || (checkSupportVee && mek.isSupport()))
                            /* Advanced Search */
                            && ((searchFilter == null) || MekSearchFilter.isMatch(mek, searchFilter))
                    ) {
                        if (!textFilter.getText().isBlank()) {
                            String text = textFilter.getText();
                            return mek.getName().toLowerCase().contains(text.toLowerCase());
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

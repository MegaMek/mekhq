/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.PatternSyntaxException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.RowFilter;

import megamek.client.ui.Messages;
import megamek.client.ui.advancedsearch.MekSearchFilter;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.ITechnology;
import megamek.common.MekSummary;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;

public class MekHQUnitSelectorDialog extends AbstractUnitSelectorDialog {
    private Campaign campaign;
    private boolean addToCampaign;
    private UnitOrder selectedUnit = null;
    private JButton buttonBuy;
    private JButton buttonAddGM;


    private static final String TARGET_UNKNOWN = "--";

    /**
     * This constructor creates the unit selector dialog for MekHQ. It loads the unit selector dialog in single-select
     * mode. These selectors are used for: <bl><li>Adding units to the campaign from the Purchase Unit dialog</li><li>
     * Adding units to the campaign from the 'Find Unit' dialog</li><li>Adding units to loot post-battle</li></bl>
     *
     * @param frame             The frame to load the unit dialog into.
     * @param unitLoadingDialog Display this frame instead while the unit dialog is loading (in case load is slow)
     * @param campaign          Used to fetch state variables from the campaign
     * @param addToCampaign     Used to determine if dialog should be in 'Buy/Add' or in 'Select for loot' mode
     */
    public MekHQUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog, Campaign campaign,
          boolean addToCampaign) {
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

        if (campaign.getCampaignOptions().isAllowClanPurchases() &&
                  campaign.getCampaignOptions().isAllowISPurchases()) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
        } else if (campaign.getCampaignOptions().isAllowClanPurchases()) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_CLAN;
        } else {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS;
        }
    }

    /**
     * This is the initialization function for all the buttons involved in ths panel.
     */
    @Override
    protected JPanel createButtonsPanel() {
        JPanel panelButtons = new JPanel(new GridBagLayout());
        //These buttons aren't always present - they all need to be initialized here to be manipulated in the state
        // machine below. They will be added to the panel only if they are present in the current view state.
        // addToCampaign and isGM control the view state.
        buttonSelect = new JButton();
        buttonSelectClose = new JButton();
        buttonClose = new JButton();
        buttonBuy = new JButton();
        buttonAddGM = new JButton();
        buttonShowBV = new JButton();

        if (addToCampaign) {
            //This branch is for purchases and adding to the hanger directly.
            buttonBuy.setText(Messages.getString("MekSelectorDialog.Buy", TARGET_UNKNOWN));
            buttonBuy.setName("buttonBuy");
            buttonBuy.addActionListener(evt -> buyUnit());
            buttonBuy.setEnabled(false);
            panelButtons.add(buttonBuy, new GridBagConstraints());

            if (campaign.isGM()) {
                // This is only displayed in GM mode.
                buttonAddGM.setText(Messages.getString("MekSelectorDialog.AddGM"));
                buttonAddGM.setName("buttonAddGM");
                buttonAddGM.addActionListener(evt -> addGM());
                buttonAddGM.setEnabled(false);
                panelButtons.add(buttonAddGM, new GridBagConstraints());
            }

            // This closes the dialog. Should always be around.
            buttonClose = new JButton(Messages.getString("Close"));
            buttonClose.setName("buttonClose");
            buttonClose.addActionListener(this);
        } else {
            // This branch is for adding units where they will not be going to the hanger.
            buttonSelect.setText(Messages.getString("MekSelectorDialog.Add"));
            buttonSelect.setName("buttonAdd");
            //the actual work will be done by whatever called this
            buttonSelect.addActionListener(evt -> select(campaign.isGM()));
            buttonSelect.setEnabled(true);
            panelButtons.add(buttonSelect, new GridBagConstraints());

            // This also closes the dialog. Different name in this state, though.
            buttonClose.setText(Messages.getString("Cancel"));
            buttonClose.setName("buttonCancel");
            buttonClose.addActionListener(evt -> {
                selectedUnit = null;
                setVisible(false);
            });
        }
        buttonClose.setEnabled(true);
        panelButtons.add(buttonClose, new GridBagConstraints());

        // This displays the BV of the selected unit.
        buttonShowBV.setText(Messages.getString("MekSelectorDialog.BV"));
        buttonShowBV.setName("buttonShowBV");
        buttonShowBV.addActionListener(this);
        panelButtons.add(buttonShowBV, new GridBagConstraints());

        return panelButtons;
    }

    /**
     * This function validates that we have a good unit for adding to the campaign.
     */
    private boolean isBadSelection() {
        if (getSelectedEntity() != null) {
            Entity entity = selectedUnit.getEntity();
            if (entity == null || isUnsupportedEntity(entity)) {
                final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
                      MekHQ.getMHQOptions().getLocale());

                String reason;
                if (entity == null) {
                    reason = MHQInternationalization.getTextAt(resources.getBaseBundleName(),
                          "mekSelectorDialog.unsupported.null");
                } else if (entity.getUnitType() == UnitType.GUN_EMPLACEMENT) {
                    reason = MHQInternationalization.getTextAt(resources.getBaseBundleName(),
                          "mekSelectorDialog.unsupported.gunEmplacement");
                } else {
                    reason = MHQInternationalization.getTextAt(resources.getBaseBundleName(),
                          "mekSelectorDialog.unsupported.droneOs");
                }
                campaign.addReport(String.format(reason,
                      spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                      CLOSING_SPAN_TAG));

                return true;
            }
            return false;
        }
        // In this case, getSelectedEntity() == null, and this selection is bad
        return true;
    }

    /**
     * Processes the event from the buy button.
     */
    private void buyUnit() {
        if (isBadSelection()) {
            return;
        }
        campaign.getShoppingList().addShoppingItem(selectedUnit, 1, campaign);
    }

    /**
     * This function processes the Add GM button's functions.
     */
    private void addGM() {

        if (isBadSelection()) {
            return;
        }

        PartQuality quality = PartQuality.QUALITY_D;
        if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
            quality = UnitOrder.getRandomUnitQuality(0);
        }

        campaign.addNewUnit(selectedUnit.getEntity(), false, 0, quality);
    }

    /**
     * Select processes the select button. This overrides a function in the AbstractUnitSelectorDialog.
     */
    @Override
    protected void select(boolean isGM) {
        if (isBadSelection()) {
            return;
        }
        PartQuality quality = PartQuality.QUALITY_D;

        if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
            quality = UnitOrder.getRandomUnitQuality(0);
        }
        campaign.addNewUnit(selectedUnit.getEntity(), false, 0, quality);
    }

    /**
     * We need to override this to add some MekHQ specific functionality, namely changing button names when the selected
     * entity is selected or unselected
     *
     * @return selectedEntity, or null if there isn't one
     */
    @Nullable
    @Override
    public Entity getSelectedEntity() {
        Entity entity = super.getSelectedEntity();
        if (entity == null) {
            selectedUnit = null;
            // If we are currently adding a unit to the campaign, we need to update the Buy and AddGM buttons.
            if (addToCampaign) {
                buttonBuy.setEnabled(false);
                buttonBuy.setText(Messages.getString("MekSelectorDialog.Buy", TARGET_UNKNOWN));
                buttonBuy.setToolTipText(null);
                buttonAddGM.setEnabled(false);
            }
        } else {
            selectedUnit = new UnitOrder(entity, campaign);
            // Here also, we need to update the Buy and AddGM buttons.
            if (addToCampaign) {
                buttonBuy.setEnabled(true);
                final TargetRoll target = campaign.getTargetForAcquisition(selectedUnit);
                buttonBuy.setText(Messages.getString("MekSelectorDialog.Buy", target.getValueAsString()));
                buttonBuy.setToolTipText(target.getDesc());
                buttonAddGM.setEnabled(true);
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

    private boolean isAllowedUnit(MekSummary mek, int nClass, ITechnology tech, boolean techLevelMatch,
          boolean checkSupportVee, int nUnit) {
        // If year limits are enabled, check that the mek is available now
        if (enableYearLimits && (mek.getYear() > allowedYear)) {
            return false;
        }
        // If a Clan mek, check that Clan meks are allowed to be purchased
        if (!(campaign.getCampaignOptions().isAllowClanPurchases()) && TechConstants.isClan(mek.getType())) {
            return false;
        }
        // if an IS mek, check that IS meks are allowed to be purchased
        if (!(campaign.getCampaignOptions().isAllowISPurchases()) && !TechConstants.isClan(mek.getType())) {
            return false;
        }
        // If canonOnly is set, is this mech Canon?
        if (canonOnly && !mek.isCanon()) {
            return false;
        }
        // Does weight match current weight class filter?
        if ((nClass != mek.getWeightClass()) && nClass != EntityWeightClass.SIZE) {
            return false;
        }
        // If the tech level is selected, does the selected tech level match?
        if ((tech == null) || !campaign.isLegal(tech)) {
            return false;
        }
        if (!techLevelMatch) {
            return false;
        }

        // Filter by unit type and support vehicles:
        // If a specific unit type is requested in the Unit Type dropdown (nUnit != -1):
        //     - if checkSupportVee then use "Support Vehicle" as this is not a default typeName from Megamek
        //     - If support vehicles should *not* be included, the unit must exactly match the requested type.
        //     - If support vehicles *should* be included, the unit must be a support vehicle (regardless of type).
        if (nUnit != -1) {
            String unitTypeName = checkSupportVee ? "Support Vehicle" : UnitType.getTypeName(nUnit);
            boolean isCorrectType = mek.getUnitType().equals(unitTypeName);
            boolean isSupport = mek.isSupport();
            if ((!checkSupportVee && !isCorrectType) || (checkSupportVee && !isSupport)) {
                return false;
            }
        }

        // if we have an advanced filter set, does it match that filter?
        if ((searchFilter != null) && !MekSearchFilter.isMatch(mek, searchFilter)) {
            return false;
        }

        // If a string is in the text filter, does the name match?
        if (!textFilter.getText().isBlank()) {
            String text = textFilter.getText();
            return mek.getName().toLowerCase().contains(text.toLowerCase());
        }
        // If all tests passed, then include this unit
        return true;

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
                    return isAllowedUnit(mek, nClass, tech, techLevelMatch, checkSupportVee, nUnit);
                    /*
                    if (
                        // year limits
                          (!enableYearLimits || (mek.getYear() <= allowedYear)) &&
                                // Clan/IS limits &&
                                (campaign.getCampaignOptions().isAllowClanPurchases() ||
                                       !TechConstants.isClan(mek.getType())) &&
                                (campaign.getCampaignOptions().isAllowISPurchases() ||
                                       TechConstants.isClan(mek.getType())) &&
                                // Canon
                                (!canonOnly || mek.isCanon()) &&
                                // Weight
                                ((nClass == mek.getWeightClass()) || (nClass == EntityWeightClass.SIZE)) &&
                                // Technology Level
                                ((null != tech) && campaign.isLegal(tech)) &&
                                (techLevelMatch) &&
                                //* Support Vehicles
                                ((nUnit == -1) ||
                                       (!checkSupportVee && mek.getUnitType().equals(UnitType.getTypeName(nUnit))) ||
                                       (checkSupportVee && mek.isSupport())) &&
                                // Advanced Search
                                ((searchFilter == null) || MekSearchFilter.isMatch(mek, searchFilter))) {
                        if (!textFilter.getText().isBlank()) {
                            String text = textFilter.getText();
                            return mek.getName().toLowerCase().contains(text.toLowerCase());
                        }
                        return true;
                    }
                    return false;*/
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
    }
}

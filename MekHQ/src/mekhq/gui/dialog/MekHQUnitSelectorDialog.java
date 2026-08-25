/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.RowFilter;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.AbstractUnitSelectorDialog;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MekSummary;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.UnitAcquisitionType;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import mekhq.campaign.campaignOptions.CampaignOption;

public class MekHQUnitSelectorDialog extends AbstractUnitSelectorDialog {
    private final Campaign campaign;
    private final boolean addToCampaign;
    private UnitOrder selectedUnit = null;
    private Entity selectedLootEntity = null;
    private boolean lootSelectionComplete = false;
    private JButton buttonBuy;
    private JButton buttonAddGM;


    private static final String TARGET_UNKNOWN = "--";

    /**
     * This constructor creates the unit selector dialog for MekHQ. It loads the unit selector dialog in single-select
     * mode. These selectors are used for: Adding units to the campaign from the Purchase Unit dialog. Adding units to
     * the campaign from the 'Find Unit' dialog. Adding units to post-battle loot.
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

        // MekHQ persists selections from both acquisition and loot modes as campaign units. Standalone Battlefield
        // Support Assets have no persistent Unit representation, while their linked base-unit rows remain valid.
        setUnitSelectionScopeFilter(MekHQUnitSelectorDialog::isPersistentCampaignUnitSummary);

        updateOptionValues();
        initialize();
        if (!addToCampaign) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            getRootPane().getActionMap().put(CLOSE_ACTION, new AbstractAction() {
                @Serial
                private static final long serialVersionUID = -5955140519962310618L;

                @Override
                public void actionPerformed(ActionEvent event) {
                    cancelLootSelection();
                }
            });
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    cancelLootSelection();
                }
            });
        }
        run();
    }

    @Override
    public void updateOptionValues() {
        gameOptions = campaign.getGameOptions();
        enableYearLimits = campaign.getCampaignOptions().get(CampaignOption.LIMIT_BY_YEAR);
        allowedYear = campaign.getGameYear();
        canonOnly = campaign.getCampaignOptions().get(CampaignOption.ALLOW_CANON_ONLY);
        gameTechLevel = campaign.getCampaignOptions().get(CampaignOption.TECH_LEVEL);
        eraBasedTechLevel = campaign.getCampaignOptions().get(CampaignOption.VARIABLE_TECH_LEVEL);

        if (campaign.getCampaignOptions().get(CampaignOption.ALLOW_CLAN_PURCHASES) &&
                  campaign.getCampaignOptions().get(CampaignOption.ALLOW_IS_PURCHASES)) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS_CLAN;
        } else if (campaign.getCampaignOptions().get(CampaignOption.ALLOW_CLAN_PURCHASES)) {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_CLAN;
        } else {
            techLevelDisplayType = TECH_LEVEL_DISPLAY_IS;
        }
    }

    /**
     * This is the initialization function for all the buttons involved in this panel.
     */
    @Override
    protected JPanel createButtonsPanel() {
        JPanel panelButtons = new JPanel(new GridBagLayout());

        buttonSelect = new JButton();
        buttonSelectClose = new JButton();
        buttonClose = new JButton();
        buttonBuy = new JButton();
        buttonAddGM = new JButton();
        buttonShowBV = new JButton();

        if (addToCampaign) {
            //This branch is for purchases and adding to the hangar directly.
            buttonBuy.setText(Messages.getString("MekSelectorDialog.Buy", TARGET_UNKNOWN));
            buttonBuy.setName("buttonBuy");
            buttonBuy.addActionListener(evt -> buyUnit());
            buttonBuy.setEnabled(false);
            panelButtons.add(buttonBuy, new GridBagConstraints());

            if (campaign.isGM()) {
                buttonAddGM.setText(Messages.getString("MekSelectorDialog.AddGM"));
                buttonAddGM.setName("buttonAddGM");
                buttonAddGM.addActionListener(evt -> addGM());
                buttonAddGM.setEnabled(false);
                panelButtons.add(buttonAddGM, new GridBagConstraints());
            }
            buttonClose = new JButton(Messages.getString("Close"));
            buttonClose.setName("buttonClose");
            buttonClose.addActionListener(this);
        } else {
            // This branch is for adding units where they will not be going to the hangar.
            buttonSelect.setText(Messages.getString("MekSelectorDialog.Add"));
            buttonSelect.setName("buttonAdd");
            buttonSelect.addActionListener(evt -> select(false));
            buttonSelect.setEnabled(true);
            panelButtons.add(buttonSelect, new GridBagConstraints());

            buttonClose.setText(Messages.getString("Cancel"));
            buttonClose.setName("buttonCancel");
            buttonClose.addActionListener(evt -> cancelLootSelection());
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
     * This function checks to see if this unit is invalid to add to the campaign.
     *
     * @return boolean True if invalid, false if valid.
     */
    private boolean isBadSelection(@Nullable Entity entity) {
        if (isCampaignAcquisitionCandidate(entity)) {
            return false;
        }
        if ((entity == null) || entity.isBattlefieldSupportAsset()) {
            return true;
        }

        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
              MekHQ.getMHQOptions().getLocale());
        String reason = MHQInternationalization.getTextAt(resources.getBaseBundleName(),
              (entity.getUnitType() == UnitType.GUN_EMPLACEMENT)
                    ? "mekSelectorDialog.unsupported.gunEmplacement"
                    : "mekSelectorDialog.unsupported.droneOs");
        campaign.addReport(ACQUISITIONS, String.format(reason,
              spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
              CLOSING_SPAN_TAG));
        return true;
    }

    /**
     * Processes the event from the buy button.
     */
    private void buyUnit() {
        if (isBadSelection(getSelectedEntity())) {
            return;
        }
        campaign.getPlayerForce().getShoppingList().addShoppingItem(selectedUnit, 1, campaign);
    }

    /**
     * This function processes the Add GM button's functions.
     */
    private void addGM() {
        if (isBadSelection(getSelectedEntity())) {
            return;
        }

        PartQuality quality = PartQuality.QUALITY_D;
        if (campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_UNIT_QUALITIES)) {
            quality = UnitOrder.getRandomUnitQuality(0);
        }

        campaign.addNewUnit(selectedUnit.getEntity(), false, 0, quality, UnitAcquisitionType.GM_ADDED);
    }

    /**
     * Select processes the select button. This overrides a function in the AbstractUnitSelectorDialog.
     */
    @Override
    protected void select(boolean ignored) {
        // Enter is bound to select() by the base dialog, but acquisition mode requires an explicit Buy or Add action.
        if (addToCampaign) {
            return;
        }
        Entity entity = getSelectedEntity();
        if (isBadSelection(entity)) {
            return;
        }
        selectedLootEntity = entity;
        lootSelectionComplete = true;
        setVisible(false);
    }

    private void cancelLootSelection() {
        selectedLootEntity = null;
        selectedUnit = null;
        lootSelectionComplete = true;
        setVisible(false);
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
        if (!addToCampaign && lootSelectionComplete) {
            return selectedLootEntity;
        }
        if (!isPersistentCampaignUnitSummary(getSelectedMekSummary())) {
            synchronizeSelectedUnit(null);
            return null;
        }

        Entity entity = super.getSelectedEntity();
        // Defend against stale or mismatched summary-cache data returning an asset despite the row-level filter.
        if ((entity != null) && entity.isBattlefieldSupportAsset()) {
            entity = null;
        }
        synchronizeSelectedUnit(entity);
        return entity;
    }

    private void synchronizeSelectedUnit(@Nullable Entity entity) {
        if (entity == null) {
            selectedUnit = null;
            if (addToCampaign) {
                buttonBuy.setEnabled(false);
                buttonBuy.setText(Messages.getString("MekSelectorDialog.Buy", TARGET_UNKNOWN));
                buttonBuy.setToolTipText(null);
                buttonAddGM.setEnabled(false);
            }
        } else {
            selectedUnit = new UnitOrder(entity, campaign);
            if (addToCampaign) {
                buttonBuy.setEnabled(true);
                buttonAddGM.setEnabled(true);
                TargetRoll target = campaign.checkAcquisition(selectedUnit).getTargetNumber();
                buttonBuy.setText(Messages.getString("MekSelectorDialog.Buy", target.getValueAsString()));
                buttonBuy.setToolTipText(target.getDesc());
            }
        }
    }

    @Override
    protected Entity refreshUnitView() {
        Entity selectedEntity = super.refreshUnitView();
        // The base selector previews standalone assets without calling getSelectedEntity(). Do not expose one as a
        // selectable MekHQ campaign unit if stale filtering leaves an asset row selected.
        if ((selectedEntity != null) && selectedEntity.isBattlefieldSupportAsset()) {
            selectedEntity = null;
            synchronizeSelectedUnit(null);
        }
        if (selectedUnit != null) {
            labelImage.setIcon(new ImageIcon(selectedUnit.getImage(this)));
        } else {
            labelImage.setIcon(null);
        }

        return selectedEntity;
    }

    static boolean isPersistentCampaignUnitSummary(@Nullable MekSummary unitSummary) {
        return (unitSummary != null) && !unitSummary.isBattlefieldSupportAsset();
    }

    static boolean isCampaignAcquisitionCandidate(@Nullable Entity entity) {
        return (entity != null) && !entity.isBattlefieldSupportAsset() && !isUnsupportedEntity(entity);
    }

    /**
     * This function is to simplify logic in filterUnits. It runs a series of checks to determine if a unit is valid
     * within the current filtering context.
     *
     * @param unitSummary              The unit being evaluated.
     * @param weightClassSelectorIndex The current weight class selection
     * @param tech                     The current tech selection
     * @param techLevelMatch           whether the current tech selection matches
     * @param unitTypeCode             the selected unit-type code
     *
     * @return true if the unit passes all filters and allowed, false otherwise
     */
    private boolean isAllowedUnit(MekSummary unitSummary, int weightClassSelectorIndex, ITechnology tech,
          boolean techLevelMatch, int unitTypeCode) {
        if (enableYearLimits && (unitSummary.getYear() > allowedYear)) {
            return false;
        }
        if (!(campaign.getCampaignOptions().get(CampaignOption.ALLOW_CLAN_PURCHASES)) && TechConstants.isClan(unitSummary.getType())) {
            return false;
        }
        if (!(campaign.getCampaignOptions().get(CampaignOption.ALLOW_IS_PURCHASES)) && !TechConstants.isClan(unitSummary.getType())) {
            return false;
        }
        if (canonOnly && !unitSummary.isCanon()) {
            return false;
        }
        if ((weightClassSelectorIndex != unitSummary.getWeightClass()) &&
                  weightClassSelectorIndex != EntityWeightClass.SIZE) {
            return false;
        }
        if ((tech == null) || !campaign.isLegal(tech)) {
            return false;
        }
        if (!techLevelMatch) {
            return false;
        }

        if (!matchesUnitTypeSelection(unitSummary, unitTypeCode)) {
            return false;
        }

        return true;
    }

    @Override
    protected void filterUnits() {
        // Preserve every shared selector predicate, including Alpha Strike/BFS advanced search and BFS card
        // title/subtitle text matching, then layer MekHQ's campaign legality checks over that result.
        super.filterUnits();
        final RowFilter<? super MekTableModel, ? super Integer> inheritedFilter = sorter.getRowFilter();
        List<Integer> techLevels = new ArrayList<>();
        for (Integer selectedIdx : listTechLevel.getSelectedIndices()) {
            techLevels.add(techLevelListToIndex.get(selectedIdx));
        }
        final Integer[] nTypes = new Integer[techLevels.size()];
        techLevels.toArray(nTypes);

        final int weightClassSelectorIndex = comboWeight.getSelectedIndex();
        // Use the base class's gap-robust mapping (the shared combo omits AERO, so a positional
        // selectedIndex - 1 would mismap types that follow it).
        final int unitTypeCode = unitTypeCodeForComboIndex(comboUnitType.getSelectedIndex());
        RowFilter<MekTableModel, Integer> unitTypeFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends MekTableModel, ? extends Integer> entry) {
                if ((inheritedFilter != null) && !inheritedFilter.include(entry)) {
                    return false;
                }
                MekTableModel mekModel = entry.getModel();
                MekSummary mek = mekModel.getMekSummary(entry.getIdentifier());
                ITechnology tech = UnitTechProgression.getProgression(mek, campaign.getTechFaction(), true);
                boolean techLevelMatch = false;
                int type = enableYearLimits ? mek.getType(allowedYear) : mek.getType();
                for (int techLevel : nTypes) {
                    if (type == techLevel) {
                        techLevelMatch = true;
                        break;
                    }
                }
                return isAllowedUnit(mek,
                      weightClassSelectorIndex,
                      tech,
                      techLevelMatch,
                      unitTypeCode);
            }
        };
        sorter.setRowFilter(unitTypeFilter);
    }
}

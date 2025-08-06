/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.buttons.MMButton;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panes.UnitMarketPane;

public class UnitMarketDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private final Campaign campaign;

    private UnitMarketPane unitMarketPane;

    // Buttons
    private JButton purchaseButton;
    private JButton addGMButton;
    private JButton removeButton;
    //endregion Variable Declarations

    //region Constructors
    public UnitMarketDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "UnitMarketDialog", "UnitMarketDialog.title");
        this.campaign = campaign;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public UnitMarketPane getUnitMarketPane() {
        return unitMarketPane;
    }

    public void setUnitMarketPane(final UnitMarketPane unitMarketPane) {
        this.unitMarketPane = unitMarketPane;
    }

    //region Buttons
    public JButton getPurchaseButton() {
        return purchaseButton;
    }

    public void setPurchaseButton(final JButton purchaseButton) {
        this.purchaseButton = purchaseButton;
    }

    public JButton getAddGMButton() {
        return addGMButton;
    }

    public void setAddGMButton(final JButton addGMButton) {
        this.addGMButton = addGMButton;
    }

    public JButton getRemoveButton() {
        return removeButton;
    }

    public void setRemoveButton(final JButton removeButton) {
        this.removeButton = removeButton;
    }
    //endregion Buttons
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setUnitMarketPane(new UnitMarketPane(getFrame(), getCampaign()));
        getUnitMarketPane().getMarketTable().getSelectionModel().addListSelectionListener(evt -> refreshView());
        return getUnitMarketPane();
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, getCampaign().isGM() ? 4 : 2));
        setPurchaseButton(new MMButton("btnPurchase", resources.getString("Purchase.text"),
              resources.getString("Purchase.toolTipText"), evt -> okAction()));
        panel.add(getPurchaseButton());

        if (getCampaign().isGM()) {
            setAddGMButton(new MMButton("btnAddGM", resources.getString("AddGM.text"),
                  resources.getString("AddGM.toolTipText"), evt -> getUnitMarketPane().addSelectedOffers()));
            panel.add(getAddGMButton());

            setRemoveButton(new MMButton("btnRemove", resources.getString("Remove.text"),
                  resources.getString("Remove.toolTipText"), evt -> getUnitMarketPane().removeSelectedOffers()));
            panel.add(getRemoveButton());
        }

        panel.add(new MMButton("btnCancel", resources.getString("Cancel.text"),
              resources.getString("Cancel.toolTipText"), this::cancelActionPerformed));
        return panel;
    }
    //endregion Initialization

    @Override
    protected void okAction() {
        getUnitMarketPane().purchaseSelectedOffers();
    }

    private void refreshView() {
        final boolean enabled = getUnitMarketPane().getSelectedEntity() != null;
        getPurchaseButton().setEnabled(enabled);
        if (getAddGMButton() != null) {
            getAddGMButton().setEnabled(enabled);
        }

        if (getRemoveButton() != null) {
            getRemoveButton().setEnabled(enabled);
        }
    }
}

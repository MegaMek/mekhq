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

import megamek.client.ui.baseComponents.MMButton;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panes.UnitMarketPane;

import javax.swing.*;
import java.awt.*;

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

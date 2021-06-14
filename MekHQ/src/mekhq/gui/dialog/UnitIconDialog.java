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
import megamek.client.ui.enums.DialogResult;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.gui.panels.UnitIconChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UnitIconDialog extends StandardForceIconDialog {
    //region Variable Declarations
    private AbstractIcon override;
    //endregion Variable Declarations

    //region Constructors
    public UnitIconDialog(final JFrame parent, final @Nullable AbstractIcon icon) {
        super(parent, "UnitIconDialog", "UnitIconDialog.title", new UnitIconChooser(icon));
        setOverride(null);
    }
    //endregion Constructors

    //region Getters
    @Override
    protected UnitIconChooser getChooser() {
        return (UnitIconChooser) super.getChooser();
    }

    public @Nullable AbstractIcon getOverride() {
        return override;
    }

    public void setOverride(final @Nullable AbstractIcon override) {
        this.override = override;
    }
    //endregion Getters

    //region Initialization
    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(new MMButton("btnOk", resources, "Ok.text", "Ok.toolTipText",
                this::okButtonActionPerformed));
        panel.add(new MMButton("btnNone", resources, "None.text",
                "UnitIconDialog.btnNone.toolTipText", this::noneActionPerformed));
        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
                this::cancelActionPerformed));
        panel.add(new MMButton("btnRefresh", resources, "refreshDirectory.text",
                "refreshDirectory.toolTipText", evt -> getChooser().refreshDirectory()));

        return panel;
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okButtonActionPerformed(final @Nullable ActionEvent evt) {
        okAction();
        setResult(((getChooser().getSelectedItem() == null) && (getOverride() == null))
                ? DialogResult.CANCELLED : DialogResult.CONFIRMED);
        setVisible(false);
    }

    private void noneActionPerformed(final ActionEvent evt) {
        setOverride(new UnitIcon(null, null));
        okButtonActionPerformed(null);
    }
    //endregion Button Actions
}

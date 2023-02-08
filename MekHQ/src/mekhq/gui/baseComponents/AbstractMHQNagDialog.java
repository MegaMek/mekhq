/*
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.baseComponents;

import megamek.client.ui.baseComponents.AbstractNagDialog;
import megamek.client.ui.enums.DialogResult;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;

import javax.swing.*;
import java.util.ResourceBundle;

public abstract class AbstractMHQNagDialog extends AbstractNagDialog {
    //region Variable Declarations
    private final Campaign campaign;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractMHQNagDialog(final JFrame frame, final String name, final String title,
                                   final String description, final Campaign campaign,
                                   final String key) {
        super(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale()), name, title, key);
        this.campaign = campaign;
        setShow(checkNag());
        setDescription(description.isBlank() ? description : resources.getString(description));
        if (isShow()) {
            initialize();
        } else {
            setResult(DialogResult.CONFIRMED);
        }
    }
    //endregion Constructors

    //region Getters
    public Campaign getCampaign() {
        return campaign;
    }
    //endregion Getters

    //region Button Actions
    @Override
    protected void okAction() {
        MekHQ.getMHQOptions().setNagDialogIgnore(getKey(), getChkIgnore().isSelected());
    }

    @Override
    protected void cancelAction() {
        MekHQ.getMHQOptions().setNagDialogIgnore(getKey(), getChkIgnore().isSelected());
    }
    //endregion Button Actions
}

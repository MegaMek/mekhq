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
package mekhq.gui.baseComponents;

import megamek.client.ui.baseComponents.AbstractValidationButtonDialog;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * This is the Base Dialog for a dialog with buttons where the Ok button requires data validation
 * in MegaMek. It extends Base Button Dialog, and adds a Validate button to the button panel. It
 * also includes an enum tracker for the result of the validation.
 *
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 * and validateAction().
 *
 * The resources associated with this dialog need to contain at least the following keys:
 * - "Ok.text" -> text for the Ok button
 * - "Ok.toolTipText" -> toolTipText for the Ok button
 * - "Validate.text" -> text for the Validate button
 * - "Validate.toolTipText" -> toolTipText for the Validate button
 * - "Cancel.text" -> text for the Cancel button
 * - "Cancel.toolTipText" -> toolTipText for the Cancel button
 *
 * This is directly tied to MegaMek's AbstractValidationButtonDialog, and any changes here MUST be
 * verified there.
 */
public abstract class AbstractMHQValidationButtonDialog extends AbstractValidationButtonDialog {
    //region Constructors
    /**
     * This creates a modal AbstractMHQValidationButtonDialog using the default MHQ resource bundle.
     * This is the normal constructor to use for an AbstractMHQValidationButtonDialog.
     */
    protected AbstractMHQValidationButtonDialog(final JFrame frame, final String name, final String title) {
        this(frame, true, name, title);
    }

    /**
     * This creates an AbstractMHQValidationButtonDialog using the default MHQ resource bundle. It
     * allows one to create non-modal button dialogs, which is not recommended by default.
     */
    protected AbstractMHQValidationButtonDialog(final JFrame frame, final boolean modal,
                                                final String name, final String title) {
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), name, title);
    }

    /**
     * This creates an AbstractMHQValidationButtonDialog using the specified resource bundle. This
     * is not recommended by default.
     */
    protected AbstractMHQValidationButtonDialog(final JFrame frame, final boolean modal,
                                                final ResourceBundle resources, final String name,
                                                final String title) {
        super(frame, modal, resources, name, title);
    }
    //endregion Constructors

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek
     */
    @Override
    protected void setPreferences() {
        setPreferences(MekHQ.getPreferences().forClass(getClass()));
    }
}

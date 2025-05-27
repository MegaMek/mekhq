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
 */
package mekhq.gui.baseComponents;

import megamek.client.ui.dialogs.buttonDialogs.AbstractValidationButtonDialog;
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
 * - "Ok.text" - text for the Ok button
 * - "Ok.toolTipText" - toolTipText for the Ok button
 * - "Validate.text" - text for the Validate button
 * - "Validate.toolTipText" - toolTipText for the Validate button
 * - "Cancel.text" - text for the Cancel button
 * - "Cancel.toolTipText" - toolTipText for the Cancel button
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
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale()), name, title);
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

    /**
     * This creates an AbstractMHQValidationButtonDialog using the specified resource bundle. This
     * is not recommended by default. Allows a JDialog to be specified as parent.
     */
    protected AbstractMHQValidationButtonDialog(final JDialog dialog, final JFrame frame, final boolean modal,
                                                final ResourceBundle resources, final String name,
                                                final String title) {
        super(dialog, frame, modal, resources, name, title);
    }
    //endregion Constructors

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     * @throws Exception if there's an issue initializing the preferences. Normally this means
     * a component has <strong>not</strong> had its name value set.
     */
    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
}

/*
 * BaseButtonDialog.java
 *
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

import megamek.common.util.EncodeControl;
import mekhq.gui.enums.DialogResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * This is the Base Dialog for a dialog with buttons in MekHQ. It extends Base Dialog, and adds a
 * button panel with base Ok and Cancel buttons. It also includes an enum tracker for the result of
 * the dialog.
 *
 * The resources associated with this dialog need to contain at least the following keys:
 * - "Ok" -> text for the ok button
 * - "Cancel" -> text for the cancel button
 */
public abstract class BaseButtonDialog extends BaseDialog {
    //region Variable Declarations
    private DialogResult result;
    //endregion Variable Declarations

    //region Constructors
    protected BaseButtonDialog(final JFrame frame, final String title) {
        this(frame, true, title);
    }

    protected BaseButtonDialog(final JFrame frame, final boolean modal, final String title) {
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), title);
    }

    protected BaseButtonDialog(final JFrame frame, final ResourceBundle resources, final String title) {
        this(frame, true, resources, title);
    }

    protected BaseButtonDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
                               final String title) {
        super(frame, modal, resources, title);
        setResult(DialogResult.CANCELLED); // Default result is cancelled
    }
    //endregion Constructors

    //region Getters/Setters
    public DialogResult getResult() {
        return result;
    }

    public void setResult(final DialogResult result) {
        this.result = result;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * Initializes the dialog's UI and preferences. Needs to be called by child classes for initial
     * setup.
     */
    @Override
    protected void initialize(final String name) {
        setName(name);
        setLayout(new BorderLayout());

        add(createCenterPane(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.PAGE_END);

        finalizeInitialization();
    }

    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));

        JButton okButton = new JButton(resources.getString("Ok"));
        okButton.setName("okButton");
        okButton.addActionListener(this::okButtonActionPerformed);
        panel.add(okButton);


        JButton cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(this::cancelActionPerformed);
        panel.add(cancelButton);

        return panel;
    }
    //endregion Initialization

    //region Button Actions
    protected void okButtonActionPerformed(final ActionEvent evt) {
        okAction();
        setResult(DialogResult.CONFIRMED);
        setVisible(false);
    }

    /**
     * Action performed when the Ok button is clicked.
     */
    protected void okAction() {

    }
    //endregion Button Actions

    /**
     * Sets the dialog to be visible, before returning the result
     * @return the result of showing the dialog
     */
    public DialogResult showDialog() {
        setVisible(true);
        return getResult();
    }
}

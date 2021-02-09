/*
 * BaseDialog.java
 *
 * Copyright (c) 2019 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/*
 * Base class for dialogs in MekHQ. This class handles setting the UI,
 * managing the Ok/Cancel buttons, managing the X button, and saving the dialog preferences.
 *
 * A class that inherits from this class needs to at least implement the method
 * setCustomUI which will create the custom UI dialog.
 * The methods setCustomPreferences, okAction, and cancelAction allow to customize
 * further the behavior of the dialog by child classes.
 *
 * The dialog will be constructed by the child class calling the initialize method.
 *
 * The resources associated with this dialog need to contain at least the following keys:
 * - dialog.text -> title of the dialog
 * - okButton.text -> text for the ok button
 * - cancelButton.text -> text for the cancel button
 */
public abstract class BaseDialog extends JDialog implements WindowListener {
    private ResourceBundle resources;
    private DialogResult result;

    protected BaseDialog(JFrame parent) {
        this(parent, false);
    }

    protected BaseDialog(JFrame parent, boolean isModal) {
        super(parent);

        this.addWindowListener(this);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(isModal);
    }

    /**
     * Result of closing the dialog.
     * @return
     */
    public DialogResult getResult() {
        return this.result;
    }

    /**
     * Initializes the dialog UI and preferences. Needs to be called by
     * child classes for initial setup.
     * @param resources the resources needed for this dialog
     */
    protected final void initialize(ResourceBundle resources) {
        assert resources != null;

        this.resources = resources;
        this.createUI();

        this.pack();
        this.setPreferences();
    }

    /**
     * Creates the custom UI (body) for the dialog.
     * This custom UI will be placed in the BorderLayout.CENTER.
     * @return
     */
    protected abstract Container createCustomUI();

    /**
     * Adds custom preferences to this dialog.
     *
     * By default, this dialog will track preferences related to the size
     * and position of the dialog. Other preferences should be added by overriding
     * this method.
     * @param preferences
     */
    protected void setCustomPreferences(PreferencesNode preferences) {
    }

    /**
     * Action performed when the ok button is clicked.
     */
    protected void okAction() {
    }

    /**
     * Action performed when the cancel button is clicked or
     * when the dialog is closed by the X button.
     */
    protected void cancelAction() {
    }

    private void createUI() {
        this.setTitle(resources.getString("dialog.text"));

        // Main body of the dialog
        this.getContentPane().add(this.createCustomUI(), BorderLayout.CENTER);

        // Ok/Cancel buttons
        JButton okButton = new JButton(resources.getString("okButton.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        okButton.requestFocus();

        JButton cancelButton = new JButton(resources.getString("cancelButton.text"));
        cancelButton.addActionListener(this::cancelButtonActionPerformed);

        // Layout buttons
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.add(buttonsPanel);
        this.getContentPane().add(bottom, BorderLayout.PAGE_END);
    }

    private void setPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(getClass());

        preferences.manage(new JWindowPreference(this));
        this.setCustomPreferences(preferences);
    }

    private void okButtonActionPerformed(ActionEvent evt) {
        this.okAction();
        this.result = DialogResult.OK;
        this.dispose();
    }

    /**
     * Notes: cancelling a dialog should always allow to close the dialog.
     */
    private void cancelButtonActionPerformed(ActionEvent evt) {
        try {
            this.cancelAction();
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
        finally {
            this.result = DialogResult.CANCEL;
            this.dispose();
        }
    }

    /**
     * Notes: closing the dialog should always allow to close the dialog.
     */
    @Override
    public void windowClosing(WindowEvent e) {
        try {
            this.cancelAction();
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }
        finally {
            this.result = DialogResult.CANCEL;
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e){
    }

    @Override
    public void windowDeiconified(WindowEvent e){
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}

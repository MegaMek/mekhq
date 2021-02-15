/*
 * BaseDialog.java
 *
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.MekHQ;
import mekhq.gui.enums.DialogResult;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/*
 * TODO : Windchild : Rewrite all comments
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
    //region Variable Declarations
    private JFrame frame;
    private DialogResult result;

    protected ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors
    protected BaseDialog(final JFrame frame, final String title) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), title);
    }

    protected BaseDialog(final JFrame frame, final boolean modal, final String title) {
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), title);
    }

    protected BaseDialog(final JFrame frame, final ResourceBundle resources, final String title) {
        this(frame, false, resources, title);
    }

    protected BaseDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
                         final String title) {
        super(frame, modal);
        setTitle(resources.getString(title));
        setFrame(frame);
        setResources(resources);
        setResult(DialogResult.CANCELLED); // Default result is cancelled
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    public void setFrame(final JFrame frame) {
        this.frame = frame;
    }

    public DialogResult getResult() {
        return result;
    }

    public void setResult(final DialogResult result) {
        this.result = result;
    }

    private void setResources(final ResourceBundle resources) {
        this.resources = resources;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * Initializes the dialog UI and preferences. Needs to be called by
     * child classes for initial setup.
     */
    protected void initialize(final String name) {
        setName(name);
        setLayout(new BorderLayout());

        add(createCenterPane(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.PAGE_END);

        pack();
        addWindowListener(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferences();
    }

    protected abstract Container createCenterPane();

    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));

        JButton okButton = new JButton(resources.getString("Ok"));
        okButton.setName("okButton");
        okButton.addActionListener(this::okButtonActionPerformed);
        panel.add(okButton);


        JButton cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        panel.add(cancelButton);

        return panel;
    }

    private void setPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(getClass());
        preferences.manage(new JWindowPreference(this));
        setCustomPreferences(preferences);
    }

    /**
     * Adds custom preferences to this dialog.
     *
     * By default, this dialog will track preferences related to the size
     * and position of the dialog. Other preferences should be added by overriding
     * this method.
     * @param preferences the preference node for this dialog
     */
    protected void setCustomPreferences(final PreferencesNode preferences) {

    }
    //endregion Initialization

    protected void okButtonActionPerformed(final ActionEvent evt) {
        okAction();
        setResult(DialogResult.CONFIRMED);
        setVisible(false);
    }

    /**
     * Action performed when the ok button is clicked.
     */
    protected abstract void okAction();

    /**
     * Notes: cancelling a dialog should always allow to close the dialog.
     */
    protected void cancelButtonActionPerformed(final ActionEvent evt) {
        try {
            cancelAction();
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        } finally {
            setVisible(false);
        }
    }

    /**
     * Action performed when the cancel button is clicked or
     * when the dialog is closed by the X button.
     */
    protected abstract void cancelAction();

    //region WindowEvents
    /**
     * Notes: closing the dialog should always allow you to close the dialog.
     */
    @Override
    public void windowClosing(final WindowEvent e) {
        try {
            cancelAction();
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }
    }

    @Override
    public void windowOpened(final WindowEvent e) {

    }

    @Override
    public void windowClosed(final WindowEvent e) {

    }

    @Override
    public void windowIconified(final WindowEvent e) {

    }

    @Override
    public void windowDeiconified(final WindowEvent e) {

    }

    @Override
    public void windowActivated(final WindowEvent e) {

    }

    @Override
    public void windowDeactivated(final WindowEvent e) {

    }
    //endregion WindowEvents
}

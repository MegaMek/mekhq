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
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * This is the base class for dialogs in MekHQ. This class handles setting the UI, managing the X
 * button, managing the escape key, and saving the dialog preferences.
 *
 * A class that inherits from this class needs to at least implement the method
 * createCenterPane to create the dialog's custom center pane.
 * The methods setCustomPreferences and cancelAction allow one to customize
 * further the behavior of the dialog by child classes.
 *
 * The dialog will be constructed by the child class calling the initialize method.
 */
public abstract class BaseDialog extends JDialog implements WindowListener {
    //region Variable Declarations
    private JFrame frame;

    protected static final String CLOSE_ACTION = "closeAction";

    protected ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors
    protected BaseDialog(final JFrame frame, final String title) {
        this(frame, false, title);
    }

    protected BaseDialog(final JFrame frame, final boolean modal, final String title) {
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), title);
    }

    protected BaseDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
                         final String title) {
        super(frame, modal);
        setTitle(resources.getString(title));
        setFrame(frame);
        setResources(resources);
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    public void setFrame(final JFrame frame) {
        this.frame = frame;
    }

    private void setResources(final ResourceBundle resources) {
        this.resources = resources;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * Initializes the dialog's UI and preferences. Needs to be called by child classes for initial
     * setup.
     */
    protected void initialize(final String name) {
        setName(name);
        setLayout(new BorderLayout());

        add(createCenterPane(), BorderLayout.CENTER);
        finalizeInitialization();
    }

    protected abstract Container createCenterPane();

    protected void finalizeInitialization() {
        pack();

        // Escape keypress
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        //getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escape, CLOSE_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, CLOSE_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, CLOSE_ACTION);
        getRootPane().getActionMap().put(CLOSE_ACTION, new AbstractAction() {
            private static final long serialVersionUID = 95171770700983453L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        addWindowListener(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferences();
    }

    private void setPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(getClass());
        preferences.manage(new JWindowPreference(this));
        setCustomPreferences(preferences);
    }

    /**
     * Adds custom preferences to the child dialog.
     *
     * By default, this dialog will track preferences related to the size
     * and position of the dialog. Other preferences can be added by overriding
     * this method.
     * @param preferences the preference node for this dialog
     */
    protected void setCustomPreferences(final PreferencesNode preferences) {

    }
    //endregion Initialization

    /**
     * Note: Cancelling a dialog should always allow one to close the dialog.
     */
    protected void cancelActionPerformed(final ActionEvent evt) {
        try {
            cancelAction();
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        } finally {
            setVisible(false);
        }
    }

    /**
     * Action performed when the Cancel button is clicked, the dialog is closed by the X button, or
     * the escape key is pressed
     */
    protected void cancelAction() {

    }

    //region WindowEvents
    /**
     * Note: Closing the dialog should always allow one to close the dialog.
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

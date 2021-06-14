/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panels.StandardForceIconChooser;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ResourceBundle;
import java.util.Set;

public class LayeredForceIconDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private StandardForceIcon originalForceIcon;
    private StandardForceIcon forceIcon;

    private boolean cancelled = false; // True when the user cancels the dialog

    private Deque<Set<String>> undoDeque = new ArrayDeque<>();
    private Deque<Set<String>> redoDeque = new ArrayDeque<>();

    //region GUI Elements
    private JTabbedPane tabbedPane;
    // Buttons
    private JButton btnUndo;
    private JButton btnRedo;
    private JButton btnRestore;
    //endregion GUI Elements
    //endregion Variable Declarations

    //region Constructors
    public LayeredForceIconDialog(JFrame parent, StandardForceIcon forceIcon) {
        super(parent, "LayeredForceIconDialog", "LayeredForceIconDialog.title");

        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return;
        }

        setForceIcon(forceIcon);

        initialize();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    //endregion Constructors

    //region Getters/Setters
    public StandardForceIcon getOriginalForceIcon() {
        return originalForceIcon;
    }

    public void setOriginalForceIcon(final StandardForceIcon originalForceIcon) {
        this.originalForceIcon = originalForceIcon;
    }

    public StandardForceIcon getForceIcon() {
        return forceIcon;
    }

    public void setForceIcon(final StandardForceIcon forceIcon) {
        this.forceIcon = forceIcon.clone();
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(resources.getString("StandardIconTab.title"), new StandardForceIconChooser(getForceIcon()));
        tabbedPane.addTab(resources.getString("LayeredIconTab.title"), initializeLayeredForceIconPanel());
        return tabbedPane;
    }

    private JPanel initializeLayeredForceIconPanel() {
        //region Button Graphical Components

        //endregion Button Graphical Components

        //region Layout
        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
/*
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(btnRestore)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnRestore)
        );
*/
        //endregion Layout

        return body;
    }

    private JPanel initializeIconDisplay() {
        //region Button Graphical Components

        //endregion Button Graphical Components

        //region Layout
        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
/*
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(btnRestore)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnRestore)
        );
*/
        //endregion Layout

        return body;
    }

    private JPanel initializeButtons() {
        //region Button Graphical Components
        JButton btnSelect = new JButton(resources.getString("Select.text"));
        btnSelect.setName("btnSelect");
        btnSelect.addActionListener(evt -> setVisible(false));

        btnUndo = new JButton(resources.getString("Undo.text"));
        btnUndo.setName("btnUndo");
        btnUndo.setEnabled(false);
        btnUndo.addActionListener(evt -> undo());

        btnRedo = new JButton(resources.getString("Redo.text"));
        btnRedo.setName("btnRedo");
        btnRedo.setEnabled(false);
        btnRedo.addActionListener(evt -> redo());

        btnRestore = new JButton(resources.getString("Restore.text"));
        btnRestore.setName("btnRestore");
        btnRestore.setEnabled(false);
        btnRestore.addActionListener(evt -> setForceIcon(getOriginalForceIcon()));

        JButton btnCancel = new JButton(resources.getString("Cancel.text"));
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(evt -> setVisible(false));

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(btnSelect)
                                .addComponent(btnUndo)
                                .addComponent(btnRedo)
                                .addComponent(btnRestore)
                                .addComponent(btnCancel))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(btnSelect)
                                .addComponent(btnUndo)
                                .addComponent(btnRedo)
                                .addComponent(btnRestore)
                                .addComponent(btnCancel))
        );

        return body;
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(LayeredForceIconDialog.class);
        preferences.manage(new JWindowPreference(this));
    }
    //endregion Initialization

    //region Event Handlers
    private void undo() {

    }

    private void redo() {

    }
    //endregion Event Handlers

    /**
     * Activates the dialog and returns if the user cancelled.
     */
 /*   public int showDialog() {
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return JOptionPane.CANCEL_OPTION;
        }
        setCancelled(false);
        setVisible(true);
        // After returning from the modal dialog, save settings the return whether it was cancelled or not...
        return isCancelled() ? JOptionPane.CANCEL_OPTION : JOptionPane.OK_OPTION;
    }*/
}

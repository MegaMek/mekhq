/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import mekhq.MekHQ;
import mekhq.gui.utilities.MarkdownEditorPanel;

/**
 * This dialog contains a MarkdownEditorPanel that the user can use to write markdown flavored text.
 *
 * @author Taharqa (Aaron Gullickson)
 */
public class MarkdownEditorDialog extends JDialog {
    private MarkdownEditorPanel mkEditor;
    private boolean changed;

    /**
     * Constructor
     *
     * @param title - a <code>String</code> for the title of the dialog
     * @param text  - a <code>String</code> for existing text to be placed in the editor when created.
     */
    public MarkdownEditorDialog(JFrame parent, boolean modal, String title, String text) {
        super(parent, modal);
        setTitle(title);

        initComponents();

        setPreferredSize(new Dimension(400, 500));

        pack();

        setLocationRelativeTo(parent);

        mkEditor.setText(text);
        changed = false;
    }

    private void initComponents() {

        mkEditor = new MarkdownEditorPanel();
        JButton btnOK = new JButton();
        JButton btnCancel = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.TextAreaDialog",
              MekHQ.getMHQOptions().getLocale());

        setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(mkEditor, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.addActionListener(evt -> btnOKActionPerformed());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        add(btnOK, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text"));
        btnCancel.addActionListener(evt -> setVisible(false));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        add(btnCancel, gridBagConstraints);
    }

    /**
     * Get the text currently in the editor
     *
     * @return <code>String</code> of the current text in the editor
     */
    public String getText() {
        return mkEditor.getText();
    }

    private void btnOKActionPerformed() {
        changed = true;
        setVisible(false);
    }

    /**
     * Was anything changed. Used to determine whether the user canceled the dialog of hit ok
     *
     * @return a <code>boolean</code> indicating whether anything was changed
     */
    public boolean wasChanged() {
        return changed;
    }
}

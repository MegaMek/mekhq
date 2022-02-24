/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
import mekhq.gui.utilities.MarkdownEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * This dialog contains a MarkdownEditorPanel that the user can use to write markdown flavored text.
 * @author Taharqa (Aaron Gullickson)
 */
public class MarkdownEditorDialog extends JDialog {
    private MarkdownEditorPanel mkEditor;
    private JButton btnOK;
    private JButton btnCancel;
    private boolean changed;

    /**
     * Constructor
     * @param parent
     * @param modal
     * @param title - a <code>String</code> for the title of the dialog
     * @param text - a <code>String</code> for existing text to be placed in the editor when created.
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
        java.awt.GridBagConstraints gridBagConstraints;

        mkEditor = new MarkdownEditorPanel();
        btnOK = new JButton();
        btnCancel = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.TextAreaDialog",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());

        setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(mkEditor, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.addActionListener(evt -> btnOKActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        add(btnOK, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text"));
        btnCancel.addActionListener(evt -> setVisible(false));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        add(btnCancel, gridBagConstraints);
    }

    /**
     * Get the text currently in the editor
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
     * @return a <code>boolean</code> indicating whether anything was changed
     */
    public boolean wasChanged() {
        return changed;
    }
}

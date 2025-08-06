/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultFormatter;

import mekhq.MekHQ;

/**
 * @author natit
 */
public class PopupValueChoiceDialog extends JDialog implements WindowListener {
    // Variable Declarations
    private JButton btnDone;
    private JSpinner value;
    private SpinnerNumberModel model;

    /**
     * This was originally set up as a text entry dialog, but there is really no reason to use it instead of the pre-fab
     * inputdialog that comes with java and it was actually causing problems because it uses a textpane instead of a
     * textfield. Since it is currently only called by the set xp command in MekHQView, I am going to refactor it into a
     * numeric value setter using a spinner.
     */
    public PopupValueChoiceDialog(final JFrame frame, final boolean modal, final String title,
          final int current, final int min) {
        super(frame, modal);
        model = new SpinnerNumberModel(current, min, null, 1);
        setTitle(title);
        initComponents();
        setLocationRelativeTo(frame);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
    }

    public PopupValueChoiceDialog(final JFrame parent, final boolean modal, final String title,
          final int current, final int min, final int max) {
        super(parent, modal);
        model = new SpinnerNumberModel(current, min, max, 1);
        setTitle(title);
        initComponents();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
    }

    private void initComponents() {
        JPanel pnlButton = new JPanel();
        btnDone = new JButton();
        JButton btnCancel = new JButton();
        value = new JSpinner(model);
        value.setEditor(new JSpinner.NumberEditor(value, "#")); //prevent digit grouping, e.g. 1,000
        JFormattedTextField jtf = ((JSpinner.DefaultEditor) value.getEditor()).getTextField();
        DefaultFormatter df = (DefaultFormatter) jtf.getFormatter();
        df.setCommitsOnValidEdit(true);

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PopupValueChoiceDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");

        btnDone.setText(resourceMap.getString("btnDone.text"));
        btnDone.setName("btnDone");
        btnDone.addActionListener(this::btnDoneActionPerformed);

        btnCancel.setText(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        pnlButton.setLayout(new GridLayout(0, 2));
        pnlButton.add(btnDone);
        pnlButton.add(btnCancel);

        value.setName("value");

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(value, BorderLayout.CENTER);
        getContentPane().add(pnlButton, BorderLayout.PAGE_END);
        pack();
    }

    private void btnDoneActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    private void btnCancelActionPerformed(ActionEvent evt) {
        value.getModel().setValue(-1);
        this.setVisible(false);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(new JFrame(), true, "Label", 0, 0, 1);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    System.exit(0);
                }
            });
            dialog.setVisible(true);
        });
    }

    public int getValue() {
        return (Integer) value.getValue();
    }

    @Override
    public void windowActivated(WindowEvent evt) {

    }

    @Override
    public void windowClosed(WindowEvent evt) {

    }

    @Override
    public void windowClosing(WindowEvent evt) {
        if (evt.getComponent() != this.btnDone) {
            value.getModel().setValue(-1);
            this.setVisible(false);
        }
    }

    @Override
    public void windowDeactivated(WindowEvent evt) {

    }

    @Override
    public void windowDeiconified(WindowEvent evt) {

    }

    @Override
    public void windowIconified(WindowEvent evt) {

    }

    @Override
    public void windowOpened(WindowEvent evt) {

    }
}

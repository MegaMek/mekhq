/*
 * Copyright (c) 2013 The MegaMek Team. All rights reserved.
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

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

/**
 * @author natit
 */
public class PopupValueChoiceDialog extends JDialog implements WindowListener {
    // Variable Declarations
    private JButton btnDone;
    private JSpinner value;
    private SpinnerNumberModel model;

    /**
	 * This was originally set up as a text entry dialog, but there is
	 * really no reason to use it instead of the pre-fab inputdialog that
	 * comes with java and it was actually causing problems because it uses
	 * a textpane instead of a textfield. Since it is currently only called by
	 * the set xp command in MekHQView, I am going to refactor it into a
	 * numeric value setter using a spinner.
	 */
	private static final long serialVersionUID = 8376874926997734492L;
	/** Creates new form */
	public PopupValueChoiceDialog(Frame parent, boolean modal, String title, int current, int min) {
		super(parent, modal);
        model = new SpinnerNumberModel(current, min, null, 1);
        setTitle(title);
        initComponents();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
	}

    public PopupValueChoiceDialog(Frame parent, boolean modal, String title, int current, int min, int max) {
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

		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PopupValueChoiceDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        btnDone.setText(resourceMap.getString("btnDone.text")); // NOI18N
        btnDone.setName("btnDone"); // NOI18N
        btnDone.addActionListener(this::btnDoneActionPerformed);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        pnlButton.setLayout(new GridLayout(0,2));
        pnlButton.add(btnDone);
        pnlButton.add(btnCancel);

        value.setName("value"); // NOI18N

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(value,BorderLayout.CENTER);
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
        java.awt.EventQueue.invokeLater(() -> {
            PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(new JFrame(), true, "Label", 0, 0, 1);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            dialog.setVisible(true);
        });
    }

    public int getValue() {
    	return (Integer)value.getValue();
    }

    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        if (arg0.getComponent() != this.btnDone) {
            value.getModel().setValue(-1);
            this.setVisible(false);
        }
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
    }
}

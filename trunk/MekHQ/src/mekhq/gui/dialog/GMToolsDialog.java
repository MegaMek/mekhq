/*
 * GM Tools Dialog
 * Added 2013/09/27
 */

package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.Utilities;

public class GMToolsDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 7724064095803583812L;
	private JButton dice;
	private JLabel diceResults;
	private JSpinner numDice;
	private JSpinner sizeDice;
	private JLabel d = new JLabel("d");

	private static final String GM_TOOL_DICE = "gmToolDice";

	public GMToolsDialog(Frame parent) {
		super(parent, false);
        setName("formGMTools"); // NOI18N
        setTitle("GM Tools");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        this.setPreferredSize(new Dimension(300,300));
        this.setMinimumSize(new Dimension(300,300));
		initComponents();
		setLocationRelativeTo(parent);
		pack();
	}

	private void initComponents() {
		GridBagConstraints gbc;

		JPanel dicePanel = new JPanel(new GridBagLayout());
		dicePanel.setBorder(BorderFactory.createTitledBorder("Dice Roller"));

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        numDice = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        ((JSpinner.DefaultEditor)numDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(numDice, gbc);

        gbc = new GridBagConstraints();
		gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        dicePanel.add(d, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        sizeDice = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        ((JSpinner.DefaultEditor)sizeDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(sizeDice, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = 3;
        dice = new JButton("Roll");
        dice.setActionCommand(GM_TOOL_DICE);
        dice.addActionListener(this);
        dicePanel.add(dice, gbc);

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = 3;
		diceResults = new JLabel(String.format("Result: %5d", 0));
		dicePanel.add(diceResults, gbc);

		// Finally, add the panel to the pane so we have content
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        getContentPane().add(dicePanel, gbc);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals(GM_TOOL_DICE)) {
			performDiceRoll();
		}
	}

	public void performDiceRoll() {
		diceResults.setText(String.format("Result: %5d", Utilities.dice((Integer)numDice.getValue(), (Integer)sizeDice.getValue())));
	}

}

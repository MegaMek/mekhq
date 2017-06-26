package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import megamek.common.util.StringUtil;

public class PartsInUseBulkActionDialog extends JDialog {
	private static final long serialVersionUID = 9114487716229826646L;

	public interface MODE {
		public static final int BUY = 0;
		public static final int ADD = 1;
	}

	public interface ACTION_TYPE {
		public static final int EXACT = 0;
		public static final int MINIMUM = 1;
		public static final int PERCENTAGE = 2;
	}

	private Frame parentFrame;
	private int mode;
	private int selectedCount;
	private boolean byTonnage;
	private Action callbackAction;

	private Font labelFont = UIManager.getFont("Label.font");
	
	public PartsInUseBulkActionDialog(Frame _parentFrame, boolean _modal, int _mode, int _selectedCount,
			boolean _byTonnage, Action _callbackAction) {
		super(_parentFrame, _modal);

		this.parentFrame = _parentFrame;
		this.mode = _mode;
		this.selectedCount = _selectedCount;
		this.byTonnage = _byTonnage;
		this.callbackAction = _callbackAction;

		initComponents();

		setLocationRelativeTo(_parentFrame);
	}

	private boolean isModeBuy() {
		return mode == MODE.BUY;
	}

	private boolean isModeAdd() {
		return mode == MODE.ADD;
	}

	private void initComponents() {
		setTitle(getActionString(false));

		final Container content = getContentPane();
		content.setLayout(new BorderLayout());

		JPanel pnlMain = new JPanel();
		pnlMain.setLayout(new GridBagLayout());

		int gridY = 0;
		pnlMain.add(createExactPanel(), createBaseConstraints(gridY++));
		pnlMain.add(createMinimumPanel(), createBaseConstraints(gridY++));
		pnlMain.add(createPercentagePanel(), createBaseConstraints(gridY++));

		content.add(pnlMain, BorderLayout.CENTER);
		content.add(createActionButtons(), BorderLayout.SOUTH);

		pack();
	}

	private Object createBaseConstraints(int gridY) {
		GridBagConstraints gridBagConstraints = null;
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridY;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;

		return gridBagConstraints;
	}

	private JPanel createExactPanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		pnl.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Exact Amount"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		JLabel lbl = new JLabel(String.format("%s exactly the %s specified below for %s.",
				getActionString(false, true), byTonnage ? "tonnage" : "amount", getSelectedPartDescription()));

		JTextField txtAmount = new JTextField();
		txtAmount.setMinimumSize(new java.awt.Dimension(200, 20));
		txtAmount.setPreferredSize(new java.awt.Dimension(200, 20));

		JButton btn = new JButton();
		btn.setText(getActionString(true));
		btn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDoBuyOrAddAction(ACTION_TYPE.EXACT, txtAmount);
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 0.1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gbc.insets = new java.awt.Insets(5, 5, 5, 5);

		pnl.add(lbl, gbc);

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		pnl.add(txtAmount, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = java.awt.GridBagConstraints.EAST;
		pnl.add(btn, gbc);

		return pnl;
	}

	private JPanel createMinimumPanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		pnl.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Minimum Amount"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		String txt = String.format("%s enough for %s so that the %s in storage and on order is at least as much as the amount specified below.",
				getActionString(false, true), getSelectedPartDescription(), byTonnage ? "tonnage" : "number");
		
		JTextField txtAmount = new JTextField();
		txtAmount.setMinimumSize(new java.awt.Dimension(200, 20));
		txtAmount.setPreferredSize(new java.awt.Dimension(200, 20));

		JButton btn = new JButton();
		btn.setText(getActionString(true));
		btn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDoBuyOrAddAction(ACTION_TYPE.MINIMUM, txtAmount);
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 0.1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gbc.insets = new java.awt.Insets(5, 5, 5, 5);

		pnl.add(createTextAreaLabel(txt), gbc);

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		pnl.add(txtAmount, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = java.awt.GridBagConstraints.EAST;
		pnl.add(btn, gbc);

		return pnl;
	}

	private JPanel createPercentagePanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		pnl.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Percentage Amount"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		String txt = String.format("%s enough for %s so that the %s in storage and on order is at least as much as the percentage specified below of the total %s of parts in use.\n\nThis option does not work for parts that are not in use.",
				getActionString(false, true), getSelectedPartDescription(), byTonnage ? "tonnage" : "number", byTonnage ? "tonnage" : "number");

		JTextField txtAmount = new JTextField();
		txtAmount.setMinimumSize(new java.awt.Dimension(200, 20));
		txtAmount.setPreferredSize(new java.awt.Dimension(200, 20));

		JButton btn = new JButton();
		btn.setText(getActionString(true));
		btn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDoBuyOrAddAction(ACTION_TYPE.PERCENTAGE, txtAmount);
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 0.1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gbc.insets = new java.awt.Insets(5, 5, 5, 5);

		pnl.add(createTextAreaLabel(txt), gbc);

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		pnl.add(txtAmount, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = java.awt.GridBagConstraints.EAST;
		pnl.add(btn, gbc);

		return pnl;
	}

	private String getSelectedPartDescription() {
		if (selectedCount == 1) {
			return "the selected part";
		}
		
		return String.format("each of the selected %s parts", selectedCount);
	}

	private JTextArea createTextAreaLabel(String txt) {
		JTextArea ta = new JTextArea(txt);
		ta.setColumns(80);
		ta.setEnabled(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setOpaque(false);
		ta.setFont(labelFont);
		ta.setDisabledTextColor(Color.BLACK);
		
		return ta;
	}
	
	private JPanel createActionButtons() {
		JPanel pnlButtons = new JPanel();

		int btnIdx = 0;

		GridBagConstraints gridBagConstraints = null;

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		JButton btnCancel = new JButton();
		btnCancel.setText("Cancel"); // NOI18N
		btnCancel.setName("btnCancel"); // NOI18N
		btnCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnDoCancelActionPerformed(evt);
			}
		});

		pnlButtons.add(btnCancel, gridBagConstraints);

		return pnlButtons;
	}

	private void btnDoBuyOrAddAction(int actionType, JTextField txtAmount) {
		if (!validateAmount(txtAmount)) {
			return;
		}

		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				String.format("%s:%s:%s", actionType, mode, txtAmount.getText().trim())); //$NON-NLS-1$
		callbackAction.actionPerformed(event);
		
		this.setVisible(false);
	}
	
	private void btnDoCancelActionPerformed(ActionEvent evt) {
		this.setVisible(false);
	}

	private String getActionString(boolean withGMModIfAppropriate) {
		return getActionString(withGMModIfAppropriate, false);
	}

	private String getActionString(boolean withGMModIfAppropriate, boolean shortForm) {
		String action = "Unknown Action";

		if (isModeBuy()) {
			action = "Buy"; // NOI18N
		} else if (isModeAdd()) {
			action = String.format("%sAdd", withGMModIfAppropriate ? "[GM] " : ""); // NOI18N
		}

		if (!shortForm) {
			action += " Parts";
		}

		return action;
	}

	private boolean validateAmount(JTextField txtFld) {
		String txtVal = txtFld.getText();

		if (StringUtil.isNullOrEmpty(txtVal)) {
			JOptionPane.showMessageDialog(parentFrame, "Please enter a value", "No value specified",
					JOptionPane.ERROR_MESSAGE);
			txtFld.requestFocus();
			return false;
		}

		txtVal = txtVal.trim();
		String errMsg = "";

		try {
			int val = Integer.parseInt(txtVal);
			
			if (!txtVal.equals(String.valueOf(val))) {
				errMsg = "Please enter a whole number greater than zero";
			} else if (val <= 0) {
				errMsg = "Please enter a whole number greater than zero";
			}
		} catch (Exception e) {
			errMsg = "Please enter a whole number greater than zero";
		}

		if (!StringUtil.isNullOrEmpty(errMsg)) {
			JOptionPane.showMessageDialog(parentFrame, errMsg, "Invalid value", JOptionPane.ERROR_MESSAGE);
			txtFld.requestFocus();
			return false;
		}

		return true;
	}
}

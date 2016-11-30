package mekhq.gui.dialog;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.MekHQOptions;

public class MekHQOptionsDialog extends JDialog {
	private static final long serialVersionUID = 5509865952125603676L;

	private JButton btnCancel;
	private JButton btnSave;

	private JLabel lblDateFormat;

	private JRadioButton rbtnDateFormatISO;
	private JRadioButton rbtnDateFormatLittleEndian;

	private ButtonGroup bgroupDateFormat;

	private JPanel pnlDateFormat;

	public MekHQOptionsDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		refreshOptions();
		setLocationRelativeTo(parent);
	}

	private void initComponents() {
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQOptionsDialog",
				new EncodeControl());

		setTitle(resourceMap.getString("title.text"));

		GridBagConstraints gridBagConstraints;
		getContentPane().setLayout(new GridBagLayout());

		btnCancel = new JButton();
		btnSave = new JButton();

		lblDateFormat = new JLabel();
		lblDateFormat.setText(resourceMap.getString("lblDateFormat.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new Insets(10, 10, 10, 0);
		getContentPane().add(lblDateFormat, gridBagConstraints);

		rbtnDateFormatISO = new JRadioButton();
		rbtnDateFormatISO.setText(resourceMap.getString("rbtnDateFormatISO.text"));

		rbtnDateFormatLittleEndian = new JRadioButton();
		rbtnDateFormatLittleEndian.setText(resourceMap.getString("rbtnDateFormatLittleEndian.text"));

		bgroupDateFormat = new ButtonGroup();
		bgroupDateFormat.add(rbtnDateFormatISO);
		bgroupDateFormat.add(rbtnDateFormatLittleEndian);

		pnlDateFormat = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnlDateFormat.add(rbtnDateFormatISO);
		pnlDateFormat.add(rbtnDateFormatLittleEndian);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new Insets(10, 0, 10, 10);
		getContentPane().add(pnlDateFormat, gridBagConstraints);

		btnSave.setText(resourceMap.getString("btnSave.text"));
		btnSave.setName("btnSave");
		btnSave.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSaveActionPerformed();
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
		getContentPane().add(btnSave, gridBagConstraints);

		btnCancel.setText(resourceMap.getString("btnCancel.text"));
		btnCancel.setName("btnCancel");
		btnCancel.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCancelActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		getContentPane().add(btnCancel, gridBagConstraints);

		pack();
	}

	protected void btnCancelActionPerformed(ActionEvent evt) {
		this.setVisible(false);
	}

	protected void btnSaveActionPerformed() {
		MekHQOptions options = MekHQOptions.getInstance();

		// initialize with default ISO
		SimpleDateFormat dateFormatShort = new SimpleDateFormat(MekHQOptions.DATE_PATTERN_ISO_SHORT);
		SimpleDateFormat dateFormatLong = new SimpleDateFormat(MekHQOptions.DATE_PATTERN_ISO_LONG);

		if (rbtnDateFormatISO.isSelected()) {
			dateFormatShort = new SimpleDateFormat(MekHQOptions.DATE_PATTERN_ISO_SHORT);
			dateFormatLong = new SimpleDateFormat(MekHQOptions.DATE_PATTERN_ISO_LONG);
		} else {
			dateFormatShort = new SimpleDateFormat(MekHQOptions.DATE_PATTERN_LITTLE_ENDIAN_SHORT);
			dateFormatLong = new SimpleDateFormat(MekHQOptions.DATE_PATTERN_LITTLE_ENDIAN_LONG);
		}

		options.setDateFormatShort(dateFormatShort);
		options.setDateFormatLong(dateFormatLong);

		try {
			options.save();
		} catch (FileNotFoundException e) {
			MekHQ.logError(e);
			JOptionPane.showMessageDialog(null,
					"For some reason MekHQ options could not be saved. (no write permissions in game directory?)",
					"Could not save MekHQ options", JOptionPane.ERROR_MESSAGE);
		}

		this.setVisible(false);
	}

	public void refreshOptions() {
		MekHQOptions options = MekHQOptions.getInstance();

		if (options.getDateFormatLong().toPattern() == MekHQOptions.DATE_PATTERN_ISO_LONG) {
			rbtnDateFormatISO.doClick();
		} else if (options.getDateFormatLong().toPattern() == MekHQOptions.DATE_PATTERN_LITTLE_ENDIAN_LONG) {
			rbtnDateFormatLittleEndian.doClick();
		} else {
			/// ? nothing selected?
		}
	}

}

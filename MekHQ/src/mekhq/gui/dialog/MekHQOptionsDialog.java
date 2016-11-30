package mekhq.gui.dialog;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import megamek.common.util.EncodeControl;
import mekhq.MekHQOptions;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

public class MekHQOptionsDialog extends JDialog {
	private static final long serialVersionUID = 5509865952125603676L;

	private JLabel lblDateFormat;

	private JRadioButton rbtnDateFormatISO;
	private JRadioButton rbtnDateFormatLittleEndian;

	private ButtonGroup bgroupDateFormat;

	private JPanel pnlDateFormat;

	public MekHQOptionsDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
		updateSelectedOptions();
		setLocationRelativeTo(parent);
	}

	private void initComponents() {
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQOptionsDialog",
				new EncodeControl());

		setTitle(resourceMap.getString("title.text"));

		GridBagConstraints gridBagConstraints;
		getContentPane().setLayout(new GridBagLayout());

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

		pack();
	}

	private void updateSelectedOptions() {
		MekHQOptions options = MekHQOptions.getInstance();

		if (options.getDateFormatLong().toPattern() == MekHQOptions.DATE_PATTERN_ISO_LONG) {
			rbtnDateFormatISO.setSelected(true);
		} else if (options.getDateFormatLong().toPattern() == MekHQOptions.DATE_PATTERN_LITTLE_ENDIAN_LONG) {
			rbtnDateFormatLittleEndian.setSelected(true);
		}
	}

}

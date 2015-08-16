/**
 * 
 */
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.universe.Era;
import mekhq.campaign.universe.Faction;

/**
 * @author Neoancient
 * 
 * Randomly selects an appropriate Bloodname based on Clan, phenotype, and year
 *
 */
public class BloodnameDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 120186090844572718L;
	
	public static final int CBS = 0;
	public static final int CB = 1;
	public static final int CCC = 2;
	public static final int CCO = 3;
	public static final int CDS = 4;
	public static final int CFM = 5;
	public static final int CGB = 6;
	public static final int CGS = 7;
	public static final int CHH = 8;
	public static final int CIH = 9;
	public static final int CJF = 10;
	public static final int CM = 11;
	public static final int CNC = 12;
	public static final int CSJ = 13;
	public static final int CSR = 14;
	public static final int CSA = 15;
	public static final int CSV = 16;
	public static final int CSL = 17;
	public static final int CWI = 18;
	public static final int CW = 19;
	public static final int CWIE = 20;
	public static final int CWOV	 = 21;
	public static final int C_NUM = 22;
	
	public static final Integer[][] terminus = {
		{2807, 3084},
		{2807, 3059},
		{2807, null},
		{2807, null},
		{2807, null},
		{2807, 3073},
		{2807, null},
		{2807, null},
		{2807, null},
		{2807, 3074},
		{2807, null},
		{2807, 2868},
		{2807, null},
		{2807, 3060},
		{2807, null},
		{2807, null},
		{2807, 3075},
		{3075, null},
		{2807, 2834},
		{2807, null},
		{3057, null},
		{2807, 2823}
	};
	
	public static final String[] clans = {
		"CBS", "CB", "CCC", "CCO", "CDS", "CFM", "CGB",
		"CGS", "CHH", "CIH", "CJF", "CMG", "CNC", "CSJ",
		"CSR", "CSA", "CSV", "CSL", "CWI", "CW", "CWIE", "CWOV"		
	};
	
	public static final String[] fullNames = {
		"Blood Spirit", "Burrock", "Cloud Cobra", "Coyote",
		"Diamond Shark/Sea Fox", "Fire Mandrill", "Ghost Bear",
		"Goliath Scorpion", "Hell's Horses", "Ice Hellion",
		"Jade Falcon", "Mongoose", "Nova Cat", "Smoke Jaguar",
		"Snow Raven", "Star Adder", "Steel Viper",
		"Stone Lion", "Widowmaker", "Wolf", "Wolf-in-Exile",
		"Wolverine"
	};
	
	public static Integer[] eras = {
		2807, 2825, 2850, 2900, 2950, 3000, 3050, 3060,
		3075, 3085, 3100
	};
	
	private JComboBox<String> cbClan;
	private JComboBox<Integer> cbEra;
	private JComboBox<String> cbPhenotype;
	private JButton btnGo;
	private JLabel lblName;
	private JLabel lblOrigClan;
	private JLabel lblPhenotype;
	private JTextArea txtWarning;

	public BloodnameDialog(JFrame parent) {
		super (parent, false);
		setTitle ("Bloodname Generator");
        getContentPane().setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(350,300));
        setMinimumSize(new Dimension(350,300));

		initComponents();

		setLocationRelativeTo(parent);
		pack();
		
	}
	
	private void initComponents() {		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		
		cbClan = new JComboBox<String>(fullNames);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		add(cbClan, gbc);
		cbClan.addActionListener(validateActionListener);
		
		cbEra = new JComboBox<Integer>(eras);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		add(cbEra, gbc);
		cbEra.addActionListener(validateActionListener);
		
		cbPhenotype = new JComboBox<String>();
		cbPhenotype.addItem("None");
		for (int i = 1; i < Bloodname.phenotypeNames.length; i++) {
			cbPhenotype.addItem(Bloodname.phenotypeNames[i]);
		}
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		add(cbPhenotype, gbc);
		cbPhenotype.addActionListener(validateActionListener);
		
		btnGo = new JButton("Go");
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		add(btnGo, gbc);
		btnGo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Bloodname n = Bloodname.randomBloodname
						(clans[cbClan.getSelectedIndex()],
								cbPhenotype.getSelectedIndex(),
								(Integer)cbEra.getSelectedItem());
				lblName.setText(n.getName() + " (" + n.getFounder() + ")");
				lblOrigClan.setText(Faction.getFaction(n.getOrigClan()).getFullName(Era.getEra((Integer)cbEra.getSelectedItem())));
				lblPhenotype.setText(Bloodname.phenotypeNames[n.getPhenotype()]);
			}
		});
		
		JLabel label = new JLabel("Result:");
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		add(label, gbc);

		lblName = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		add(lblName, gbc);

		label = new JLabel("Original Clan:");
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		add(label, gbc);

		lblOrigClan = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		add(lblOrigClan, gbc);

		label = new JLabel("Phenotype:");
		gbc.gridx = 0;
		gbc.gridy = 7;
		gbc.gridwidth = 1;
		add(label, gbc);

		lblPhenotype = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 7;
		gbc.gridwidth = 1;
		add(lblPhenotype, gbc);

		txtWarning = new JTextArea();
		txtWarning.setEditable(false);
		txtWarning.setLineWrap(true);
		txtWarning.setWrapStyleWord(true);
		gbc.gridx = 0;
		gbc.gridy = 8;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(txtWarning, gbc);
			}
	
	private ActionListener validateActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent ev) {
			btnGo.setEnabled(validateInput());
		}
	};
	
	private boolean validateInput() {
		int clan = cbClan.getSelectedIndex();
		int year = (Integer)cbEra.getSelectedItem();
		String phenotype = (String)cbPhenotype.getSelectedItem();
		
		if (clan < 0 || null == phenotype ||
				year < 2807 || year > 3150) {
			return false;
		}
		
		String txt = "";
		
		if (year < terminus[clan][0]) {
			for (int i = 0; i < eras.length; i++) {
				if (eras[i] >= terminus[clan][0]) {
					txt += fullNames[clan]
							+ " formed in "
							+ terminus[clan][0]
							+ ". Using " + eras[i] + ".\n";
					break;
				}
			}
		}
		if (null != terminus[clan][1] &&
				year > terminus[clan][1]) {
			for (int i = eras.length - 1; i >= 0; i--) {
				if (eras[i] <= terminus[clan][1]) {
					txt += fullNames[clan]
						+ " ceased to existed in "
						+ terminus[clan][1]
						+ ". Using " + eras[i] + ".\n";
					break;
				}
			}
		}
		if ("ProtoMech" == phenotype && year < 3060) {
			txt += "ProtoMechs did not exist in "
					+ year + ". Using Aerospace.\n";
		}
		if ("Naval" == phenotype && clan != CSR) {
			txt += "The Naval phenotype is unique to Clan Snow Raven. Using General.\n";
		}
		if ("TankWarrior" == phenotype && clan != CHH) {
			 txt += "The TankWarrior phenotype is unique to Clan Hell's Horses. Using General.\n";
		} else if ("TankWarrior" == phenotype && year < 3100) {
			txt += "The TankWarrior phenotype began development in the 32nd century. Using 3100.\n";
		}
		txtWarning.setText(txt);
		
		return true;
	};

	public void setFaction(String factionCode) {
		for (int i = 0; i < C_NUM; i++) {
			if (factionCode.equals(clans[i])) {
				cbClan.setSelectedIndex(i);
				return;
			}
		}
	}
	
	public void setYear(int year) {
		for (int i = eras.length - 1; i >= 0; i--) {
			if (eras[i] <= year) {
				cbEra.setSelectedIndex(i);
				return;
			}
		}
	}
}

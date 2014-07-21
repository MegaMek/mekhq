package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;

@SuppressWarnings("unused") // FIXME!
public class PlanetEditorGUI extends JPanel {
	private static final long serialVersionUID = -4988871070680951144L;
	private JPanel pnlGeneral = new JPanel();
	private JLabel lblSlot = new JLabel("Slot: ");
	private JComboBox<String> orbitalSlot;
	private JLabel lblType = new JLabel("Type: ");
	private JComboBox<String> type;
	private JLabel lblPressure = new JLabel("Pressure: ");
	private JComboBox<String> pressure;
	private JLabel lblLifezone = new JLabel("Lifezone: ");
	private JComboBox<String> lifezone;
	private JLabel lblClimate = new JLabel("Climate: ");
	private JComboBox<String> climate;
	private JLabel lblLifeforms = new JLabel("Lifeforms: ");
	private JComboBox<String> lifeforms;
	private JLabel lblDensity = new JLabel("Density: ");
	private JTextField density;
	private JLabel lblDay = new JLabel("Day Length: ");
	private JTextField dayLength;
	private JLabel lblYear = new JLabel("Year Length: ");
	private JTextField yearLength;
	private JLabel lblDiameter = new JLabel("Diameter: ");
	private JTextField diameter;
	private JLabel lblGravity = new JLabel("Gravity: ");
	private JTextField gravity;
	private JLabel lblPercWater = new JLabel("Percent Water: ");
	private JSpinner percentWater;
	private JLabel lblTemp = new JLabel("Temperature: ");
	private JSpinner temperature;
	private JLabel lblLandmasses = new JLabel("Land Masses: ");
	private JSpinner landmasses;
	
	// Habitability
	private JPanel pnlHabitability = new JPanel();
	private ButtonGroup habGroup;
	private JRadioButton habTrue;
	private JRadioButton habFalse;
	
	// Hyper-pulse Generators
	private JPanel pnlHPG = new JPanel();
	private ButtonGroup hpgGroup;
	private JRadioButton hpgClassA;
	private JRadioButton hpgClassB;
	private JRadioButton hpgClassC;
	
	// Zenith Recharge Station?
	private JPanel pnlZenith = new JPanel();
	private ButtonGroup zenithGroup;
	private JRadioButton zenithTrue;
	private JRadioButton zenithFalse;
	
	// Nadir Recharge Station?
	private JPanel pnlNadir = new JPanel();
	private ButtonGroup nadirGroup;
	private JRadioButton nadirTrue;
	private JRadioButton nadirFalse;
	
	// Socio-Industrial Ratings
	private JPanel pnlSocioIndi = new JPanel();
	private JSpinner socioIndustrial1;
	private JSpinner socioIndustrial2;
	private JSpinner socioIndustrial3;
	private JSpinner socioIndustrial4;
	private JSpinner socioIndustrial5;
	
	public PlanetEditorGUI(boolean fullEditor) {
		setLayout(new GridBagLayout());
		initializeComponents(fullEditor);
	}
	
	public void initializeComponents(boolean fullEditor) {
		GridBagConstraints gbc = new GridBagConstraints();
		
		// Start General Panel
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		pnlGeneral.add(lblSlot, gbc);
		gbc.gridx++;
		pnlGeneral.add(orbitalSlot, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblType, gbc);
		gbc.gridx++;
		pnlGeneral.add(type, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblPressure, gbc);
		gbc.gridx++;
		pnlGeneral.add(pressure, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblLifezone, gbc);
		gbc.gridx++;
		pnlGeneral.add(lifezone, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblClimate, gbc);
		gbc.gridx++;
		pnlGeneral.add(climate, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblLifeforms, gbc);
		gbc.gridx++;
		pnlGeneral.add(lifeforms, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblDensity, gbc);
		gbc.gridx++;
		pnlGeneral.add(density, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblDay, gbc);
		gbc.gridx++;
		pnlGeneral.add(dayLength, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblYear, gbc);
		gbc.gridx++;
		pnlGeneral.add(yearLength, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblDiameter, gbc);
		gbc.gridx++;
		pnlGeneral.add(diameter, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblGravity, gbc);
		gbc.gridx++;
		pnlGeneral.add(gravity, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblPercWater, gbc);
		gbc.gridx++;
		pnlGeneral.add(percentWater, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblTemp, gbc);
		gbc.gridx++;
		pnlGeneral.add(temperature, gbc);
		gbc.gridx--;
		gbc.gridy++;
		pnlGeneral.add(lblLandmasses, gbc);
		gbc.gridx++;
		pnlGeneral.add(landmasses, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		pnlGeneral.setBorder(BorderFactory.createTitledBorder("General"));
		add(pnlGeneral, gbc);
		// End General Panel
	}
}

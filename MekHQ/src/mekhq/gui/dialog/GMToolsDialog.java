/*
 * GM Tools Dialog
 * Added 2013/09/27
 */

package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.client.RandomUnitGenerator;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.UnitTableData;
import mekhq.campaign.universe.UnitTableData.FactionTables;
import mekhq.gui.CampaignGUI;

public class GMToolsDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 7724064095803583812L;
	private JButton dice;
	private JLabel diceResults;
	private JSpinner numDice;
	private JSpinner sizeDice;
	private JLabel d = new JLabel("d");
	
	private JComboBox<String> factionPicker;
	private JTextField yearPicker;
	private JComboBox<String> qualityPicker;
	private JComboBox<String> unitTypePicker;
	private JComboBox<String> unitWeightPicker;
	private JLabel unitPicked;
	
	private CampaignGUI gui;

	private static final String GM_TOOL_DICE = "gmToolDice";
	private static final String RAT_ROLLER_TOOL = "ratRollerTool";
	private static final String RAT_ADDER_TOOL = "ratAdderTool";

	public GMToolsDialog(Frame parent, CampaignGUI gui) {
		super(parent, false);
		this.gui = gui;
        setName("formGMTools"); // NOI18N
        setTitle("GM Tools");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        this.setPreferredSize(new Dimension(600,300));
        this.setMinimumSize(new Dimension(600,300));
		initComponents();
		setLocationRelativeTo(parent);
		pack();
	}

	private JPanel getDiceRoller() {
		JPanel dicePanel = new JPanel(new GridBagLayout());
		dicePanel.setBorder(BorderFactory.createTitledBorder("Dice Roller"));

        numDice = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        ((JSpinner.DefaultEditor)numDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(numDice, getGridBagConstraints(0,0));
        
        dicePanel.add(d, getGridBagConstraints(1,0));


        sizeDice = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        ((JSpinner.DefaultEditor)sizeDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(sizeDice, getGridBagConstraints(2,0));

        dice = new JButton("Roll");
        dice.setActionCommand(GM_TOOL_DICE);
        dice.addActionListener(this);
        dicePanel.add(dice, getGridBagConstraints(0,1,3,1));

		diceResults = new JLabel(String.format("Result: %5d", 0));
		dicePanel.add(diceResults, getGridBagConstraints(0,2,3,1));
		return dicePanel;
}

	/**
	 * Using mekhq.campaign.universe.UnitTableData for RAT access
	 */
private JPanel getRATRoller() {
	

	JPanel ratPanel = new JPanel(new GridBagLayout());
	ratPanel.setBorder(BorderFactory.createTitledBorder("RAT Roller"));
	
	factionPicker = new JComboBox<String>(Faction.getFactionList().toArray(new String[Faction.getFactionList().size()]));
	System.out.println(gui.getCampaign().getCalendar().get(Calendar.YEAR));
	yearPicker = new JTextField(5);
	yearPicker.setText(String.valueOf(gui.getCampaign().getCalendar().get(Calendar.YEAR)));
	qualityPicker = new JComboBox<String>(UnitTableData.qualityNames);
	unitTypePicker = new JComboBox<String>(UnitTableData.unitNames);
	unitWeightPicker = new JComboBox<String>(UnitTableData.weightNames);
	unitPicked = new JLabel();
	
	ratPanel.add(new JLabel("Year"), getGridBagConstraints(0,0));
	ratPanel.add(yearPicker, getGridBagConstraints(0,1));
	
	ratPanel.add(new JLabel("Faction"),getGridBagConstraints(1,0,2,1));
	ratPanel.add(factionPicker, getGridBagConstraints(1,1,2,1));
	
	ratPanel.add(new JLabel("Quality"),getGridBagConstraints(3,0));
	ratPanel.add(qualityPicker,getGridBagConstraints(3,1));
	
	ratPanel.add(new JLabel("Unit Type"),getGridBagConstraints(4,0));
	ratPanel.add(unitTypePicker,getGridBagConstraints(4,1));
	
	ratPanel.add(new JLabel("Weight"),getGridBagConstraints(5,0));
	ratPanel.add(unitWeightPicker,getGridBagConstraints(5,1));
	
	ratPanel.add(unitPicked,getGridBagConstraints(0,2,4,1));
	
	JButton roll = new JButton("Roll For RAT");
    roll.setActionCommand(RAT_ROLLER_TOOL);
    roll.addActionListener(this);
    ratPanel.add(roll,getGridBagConstraints(5,3));
    
    JButton roll2 = new JButton("Add Random Unit");
    if(!gui.getCampaign().isGM()) roll2.setEnabled(false);
    roll2.setActionCommand(RAT_ADDER_TOOL);
    roll2.addActionListener(this);
    ratPanel.add(roll2,getGridBagConstraints(6,3));
	return ratPanel;
}

private GridBagConstraints getGridBagConstraints(int x, int y){
	return getGridBagConstraints(x,y,1,1);
}
private GridBagConstraints getGridBagConstraints(int x, int y, int width, int height){
	return new GridBagConstraints(x,y,width,height,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0);
}
private void initComponents() {	
        getContentPane().add(getDiceRoller(), getGridBagConstraints(0,0));
        getContentPane().add(getRATRoller(), getGridBagConstraints(0,1));
}

@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals(GM_TOOL_DICE)) {
			performDiceRoll();
		}
		
		if (event.getActionCommand().equals(RAT_ROLLER_TOOL)){
			performRollRat();
		}
		
		if (event.getActionCommand().equals(RAT_ADDER_TOOL)){
			MechSummary ms = performRollRat();
			if(ms!=null){
				Entity e;
				try {
					e = new MechFileParser(ms.getSourceFile(),ms.getEntryName()).getEntity();
				gui.getCampaign().addUnit(e, false, 0);
		        gui.refreshUnitList();
		        gui.refreshServicedUnitList();
		        gui.refreshReport();
		        gui.refreshOverview();
				} catch (EntityLoadingException e1) {
					e1.printStackTrace();
					MekHQ.logError("Failed to load entity " + ms.getName() + " from " + ms.getSourceFile().toString());
					unitPicked.setText("Failed to load entity " + ms.getName());
				}
			}
		}
}


private MechSummary performRollRat() {
	try{
		UnitTableData unitTables = UnitTableData.getInstance();
		int unitType = Arrays.binarySearch(UnitTableData.unitNames,unitTypePicker.getSelectedItem().toString());
		int unitQuality = Arrays.binarySearch(UnitTableData.qualityNames,qualityPicker.getSelectedItem().toString());
		int unitWeight = Arrays.binarySearch(UnitTableData.weightNames,unitWeightPicker.getSelectedItem().toString());
		
		if(!unitTables.isInitialized()){
			unitPicked.setText("No Unit Tables Initialized.");
			return null;
		}
		FactionTables selection = unitTables.getBestRAT(gui.getCampaign().getCampaignOptions().getRATs(), Integer.parseInt(yearPicker.getText()),
				factionPicker.getSelectedItem().toString(), unitWeight);
		if( selection == null){
			unitPicked.setText("No Unit Table Avaliable for Selection");
			return null;
		}
		String rat = selection.getTable(unitType,unitWeight,unitQuality);
		MekHQ.logMessage("Selected: " + rat);
		if (rat == null){
			unitPicked.setText("No Unit Table Avaliable for Selection");
			return null;
		}
		Campaign campaign = gui.getCampaign();
		//Taken from mekhq.campaign.market.UnitMarket.addOffers();
		MechSummary ms = null;
		RandomUnitGenerator rug = RandomUnitGenerator.getInstance();
		if(!rug.isInitialized()) {
			unitPicked.setText("RAT tables are still loading");
			return null;
		}
		rug.setChosenRAT(rat);
		for(int i = 0;i < 10000;i++){ //This way we get a terminating condition to prevent locking if an option causes problems.
			ArrayList<MechSummary> msl = rug.generate(1);
			if (msl.size() > 0) {
				ms = msl.get(0);
				MekHQ.logMessage("picked "+ ms.getName() + ", determining if legal");
				if (campaign.getCampaignOptions().limitByYear() &&
						Integer.parseInt(yearPicker.getText()) < ms.getYear()) {
					continue;
				}
				if ((campaign.getCampaignOptions().allowClanPurchases() && ms.isClan())
						|| (campaign.getCampaignOptions().allowISPurchases() && !ms.isClan())) {
							//We have found a unit
							unitPicked.setText(ms.getName());
							return ms;
				}
			}	
		}
	}catch(NumberFormatException e){
		unitPicked.setText("Please enter a valid year");
		return null;
	}
	unitPicked.setText("No Unit Table Avaliable for Selection.\n The year is the suspect cause");
	return null;
		
}

public void performDiceRoll() {
		diceResults.setText(String.format("Result: %5d", Utilities.dice((Integer)numDice.getValue(), (Integer)sizeDice.getValue())));
}
}

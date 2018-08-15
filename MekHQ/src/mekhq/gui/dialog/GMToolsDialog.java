/*
 * GM Tools Dialog
 * Added 2013/09/27
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.gui.CampaignGUI;

public class GMToolsDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 7724064095803583812L;
    
    private JButton dice;
    private JLabel diceResults;
    private JSpinner numDice;
    private JSpinner sizeDice;
    private JLabel d = new JLabel("d");
    
    private JComboBox<FactionChoice> factionPicker;
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
        //this.setPreferredSize(new Dimension(600,300));
        //this.setMinimumSize(new Dimension(600,300));
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel getDiceRoller() {
        JPanel dicePanel = new JPanel(new GridBagLayout());
        dicePanel.setBorder(BorderFactory.createTitledBorder("Dice Roller"));

        numDice = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        ((JSpinner.DefaultEditor)numDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(numDice, newGridBagConstraints(0,0));
        
        dicePanel.add(d, newGridBagConstraints(1,0));

        sizeDice = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        ((JSpinner.DefaultEditor)sizeDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(sizeDice, newGridBagConstraints(2, 0));

        dice = new JButton("Roll");
        dice.setActionCommand(GM_TOOL_DICE);
        dice.addActionListener(this);
        dicePanel.add(dice, newGridBagConstraints(0, 1, 3, 1));

        diceResults = new JLabel(String.format("Result: %5d", 0));
        dicePanel.add(diceResults, newGridBagConstraints(0, 2, 3, 1));
        return dicePanel;
    }
    
    private JPanel getRATRoller() {
    	final String[] qualityNames = {"F", "D", "C", "B", "A", "A*"};
    	final String[] weightNames = {"Light", "Medium", "Heavy", "Assault"};
        JPanel ratPanel = new JPanel(new GridBagLayout());
        ratPanel.setBorder(BorderFactory.createTitledBorder("RAT Roller"));
        
        Collection<String> factionIds = Faction.getFactionList();
        List<FactionChoice> factionChoices = new ArrayList<>(factionIds.size());
        int year = gui.getCampaign().getGameYear();
        StringBuilder sb = new StringBuilder();
        for(String factionId : factionIds) {
            sb.setLength(0);
            sb.append(Faction.getFaction(factionId).getFullName(year)).append(" [").append(factionId).append("]");
            factionChoices.add(new FactionChoice(factionId, sb.toString()));
        }
        Collections.sort(factionChoices, new Comparator<FactionChoice>() {
            @Override
            public int compare(FactionChoice o1, FactionChoice o2) {
                if(o1 == o2 || o1.faction == o2.faction) {
                    return 0;
                }
                if(null == o1 || null == o1.faction) {
                    return -1;
                }
                if(null == o2 || null == o2.faction) {
                    return 1;
                }
                return o1.faction.compareTo(o2.faction);
            }
            
        });
        factionPicker = new JComboBox<FactionChoice>(factionChoices.toArray(new FactionChoice[factionChoices.size()]));
        System.out.println(gui.getCampaign().getGameYear());
        yearPicker = new JTextField(5);
        yearPicker.setText(String.valueOf(gui.getCampaign().getGameYear()));
        qualityPicker = new JComboBox<String>(qualityNames);
        unitTypePicker = new JComboBox<String>();
        for (int ut = 0; ut < UnitType.SIZE; ut++) {
            if (gui.getCampaign().getUnitGenerator().isSupportedUnitType(ut)) {
                unitTypePicker.addItem(UnitType.getTypeName(ut));
            }
        }
        unitWeightPicker = new JComboBox<String>(weightNames);
        unitPicked = new JLabel("-");
        unitTypePicker.addItemListener(ev ->
            unitWeightPicker.setEnabled(unitTypePicker.getSelectedItem().equals("Mek")
                    || unitTypePicker.getSelectedItem().equals("Tank")
                    || unitTypePicker.getSelectedItem().equals("Aero"))
        );
        
        ratPanel.add(new JLabel("Year"), newGridBagConstraints(0, 0));
        ratPanel.add(yearPicker, newGridBagConstraints(0, 1));
        
        ratPanel.add(new JLabel("Faction"), newGridBagConstraints(1, 0, 2, 1));
        ratPanel.add(factionPicker, newGridBagConstraints(1, 1, 2, 1));
        
        ratPanel.add(new JLabel("Quality"), newGridBagConstraints(3, 0));
        ratPanel.add(qualityPicker,newGridBagConstraints(3, 1));
        
        ratPanel.add(new JLabel("Unit Type"), newGridBagConstraints(4, 0));
        ratPanel.add(unitTypePicker, newGridBagConstraints(4, 1));
        
        ratPanel.add(new JLabel("Weight"), newGridBagConstraints(5, 0));
        ratPanel.add(unitWeightPicker, newGridBagConstraints(5, 1));
        
        ratPanel.add(unitPicked, newGridBagConstraints(0, 2, 4, 1));
        
        JButton roll = new JButton("Roll For RAT");
        roll.setActionCommand(RAT_ROLLER_TOOL);
        roll.addActionListener(this);
        ratPanel.add(roll, newGridBagConstraints(5, 3));
        
        JButton roll2 = new JButton("Add Random Unit");
        if(!gui.getCampaign().isGM()) roll2.setEnabled(false);
        roll2.setActionCommand(RAT_ADDER_TOOL);
        roll2.addActionListener(this);
        ratPanel.add(roll2, newGridBagConstraints(6, 3));
        return ratPanel;
    }
    
    private GridBagConstraints newGridBagConstraints(int x, int y) {
        return newGridBagConstraints(x, y, 1, 1);
    }
    
    private GridBagConstraints newGridBagConstraints(int x, int y, int width, int height) {
        return new GridBagConstraints(x, y, width, height, 1.0, 1.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 3, 0, 3), 0, 0);
    }
    
    private void initComponents() {    
        getContentPane().add(getDiceRoller(), newGridBagConstraints(0,0));
        getContentPane().add(getRATRoller(), newGridBagConstraints(0,1));
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        final String METHOD_NAME = "actionPerformed(ActionEvent)"; //$NON-NLS-1$

        if(event.getActionCommand().equals(GM_TOOL_DICE)) {
            performDiceRoll();
        }
        
        if(event.getActionCommand().equals(RAT_ROLLER_TOOL)) {
            performRollRat();
        }
        
        if(event.getActionCommand().equals(RAT_ADDER_TOOL)) {
            MechSummary ms = performRollRat();
            if(null != ms){
                Entity e = null;
                try {
                    e = new MechFileParser(ms.getSourceFile(),ms.getEntryName()).getEntity();
                    gui.getCampaign().addUnit(e, false, 0);
                } catch (EntityLoadingException e1) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            "Failed to load entity " + ms.getName() + " from " //$NON-NLS-1$
                                    + ms.getSourceFile().toString()); //$NON-NLS-1$
                    MekHQ.getLogger().error(getClass(), METHOD_NAME, e1);
                    unitPicked.setText("Failed to load entity " + ms.getName());
                }
            }
        }
    }
    
    private MechSummary performRollRat() {
        try{
            IUnitGenerator ug = gui.getCampaign().getUnitGenerator();
            int unitType = 0;
            for (int ut = 0; ut < UnitType.SIZE; ut++) {
                if (UnitType.getTypeName(ut).equals(unitTypePicker.getSelectedItem())) {
                    unitType = ut;
                    break;
                }
            }
            int unitQuality = qualityPicker.getSelectedIndex();
            int unitWeight = unitWeightPicker.getSelectedIndex() + EntityWeightClass.WEIGHT_LIGHT;
            if (!unitWeightPicker.isEnabled()) {
                unitWeight = -1;
            }
            
            int targetYear = Integer.parseInt(yearPicker.getText());
            
            Campaign campaign = gui.getCampaign();
            Predicate<MechSummary> test = ms ->
                	(!campaign.getCampaignOptions().limitByYear() || targetYear > ms.getYear())
                		&& (!ms.isClan() || campaign.getCampaignOptions().allowClanPurchases())
                		&& (ms.isClan() || campaign.getCampaignOptions().allowISPurchases());
            MechSummary ms = ug
            		.generate(((FactionChoice) factionPicker.getSelectedItem()).id,
            				unitType, unitWeight, targetYear, unitQuality, test);
            if (ms != null) {
                unitPicked.setText(ms.getName());
                return ms;
            }
        } catch(NumberFormatException e) {
            unitPicked.setText("Please enter a valid year");
            return null;
        }
        unitPicked.setText("No unit matching criteria and purchase restrictions.");
        return null;
    }

    public void performDiceRoll() {
            diceResults.setText(String.format("Result: %5d", Utilities.dice((Integer)numDice.getValue(), (Integer)sizeDice.getValue())));
    }
    
    private static class FactionChoice {
        public final String faction;
        public final String id;
        
        public FactionChoice(String id, String faction) {
            this.id = id;
            this.faction = faction;
        }
        
        @Override
        public String toString() {
            return faction;
        }
    }
}

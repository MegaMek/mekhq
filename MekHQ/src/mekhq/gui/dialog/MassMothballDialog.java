package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.UnitTableMouseAdapter;

public class MassMothballDialog extends JDialog implements ActionListener, ListSelectionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7435381378836891774L;
    
    Map<Integer, List<Unit>> unitsByType = new HashMap<>();
    Map<Integer, JList<Person>> techListsByUnitType = new HashMap<>(); 
    Map<Integer, JLabel> timeLabelsByUnitType = new HashMap<>();
    Campaign campaign;
    boolean activating;
    
    
    public MassMothballDialog(Frame parent, Unit[] units, Campaign campaign, boolean activate) {
        super(parent, "Mass Mothball/Activate");
        setLocationRelativeTo(parent);
        
        sortUnitsByType(units);
        this.campaign = campaign;
        
        getContentPane().setLayout(new GridLayout(5, 2, 2, 2));
        
        JLabel instructionLabel = new JLabel();
        instructionLabel.setText("Choose the techs to carry out mothball/reactivation operations on the displayed units.");
        getContentPane().add(instructionLabel);
        
        addTableHeaders();
        
        for(int unitType : unitsByType.keySet()) {
            addUnitTypePanel(unitType);
        }
        
        addExecuteButton(activate);
        
        //this.setPreferredSize(getContentPane().getPreferredSize());
        this.setResizable(false);
        this.pack();
        this.validate();
    }
    
    private void addTableHeaders() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.weightx = .3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        
        JLabel labelUnitNames = new JLabel();
        labelUnitNames.setText("Units to Process");
        headerPanel.add(labelUnitNames, gbc);
           
        JLabel labelTechs = new JLabel();
        labelTechs.setText("Available Techs");
        headerPanel.add(labelTechs, gbc);
        
        JLabel labelPlaceHolder = new JLabel();
        labelPlaceHolder.setText(" ");
        headerPanel.add(labelPlaceHolder, gbc);
        
        getContentPane().add(headerPanel);
    }
    
    private void addUnitTypePanel(int unitType) {
        JPanel unitTypePanel = new JPanel();
        unitTypePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.weightx = .3;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        
        for(Unit unit : unitsByType.get(unitType)) {
            JLabel unitLabel = new JLabel();
            unitLabel.setText(unit.getName());
            unitTypePanel.add(unitLabel, gbc);
            gbc.gridy++;
        }
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = GridBagConstraints.REMAINDER;

        JList<Person> techList = new JList<Person>();
        DefaultListModel<Person> listModel = new DefaultListModel<Person>();
        
        for(Person tech : campaign.getTechs()) {
            if(tech.canTech(unitsByType.get(unitType).get(0).getEntity())) {
                listModel.addElement(tech);
            }
        }
        
        techList.setModel(listModel);
        techList.addListSelectionListener(this);
        unitTypePanel.add(techList, gbc);
        techListsByUnitType.put(unitType, techList);
        
        gbc.gridx = 2;
        JLabel labelTotalTime = new JLabel();
        labelTotalTime.setText("<html>Completion Time: <span color='red'>Never</span></html>");
        timeLabelsByUnitType.put(unitType, labelTotalTime);
        unitTypePanel.add(labelTotalTime, gbc);
        
        getContentPane().add(unitTypePanel);
    }
    
    private void addExecuteButton(boolean activate) {
        JButton buttonExecute = new JButton();
        buttonExecute.setText(activate ? "Activate" : "Mothball");
        buttonExecute.setActionCommand(activate ? UnitTableMouseAdapter.COMMAND_ACTIVATE : UnitTableMouseAdapter.COMMAND_MOTHBALL);
        buttonExecute.addActionListener(this);
        getContentPane().add(buttonExecute);
    }
    
    private void sortUnitsByType(Unit[] units) {
        for(int x = 0; x < units.length; x++) {
            int unitType = UnitType.determineUnitTypeCode(units[x].getEntity());
            
            if(!unitsByType.containsKey(unitType)) {
                unitsByType.put(unitType, new ArrayList<>());
            }
            
            unitsByType.get(unitType).add(units[x]);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        if(e.getActionCommand() != UnitTableMouseAdapter.COMMAND_MOTHBALL &&
                e.getActionCommand() != UnitTableMouseAdapter.COMMAND_ACTIVATE) {
            return;
        }
        
        for(int unitType : unitsByType.keySet()) {
            List<Person> selectedTechs = techListsByUnitType.get(unitType).getSelectedValuesList();
            int techIndex = 0;
            
            // this is a "naive" approach, where we assign each of the selected techs
            // to approximately # units / # techs in mothball/reactivation tasks
            for(Unit unit : unitsByType.get(unitType)) {                
                unit.startMothballing(selectedTechs.get(techIndex).getId());
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
                
                if(techIndex == selectedTechs.size() - 1) {
                    techIndex = 0;
                } else {
                    techIndex++;
                }
            }
        }
        
        this.setVisible(false);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // TODO Auto-generated method stub
        JList<Person> techList = (JList<Person>) e.getSource();
        
        int unitType = -1;
        // this is mildly kludgy:
        // we scan the 'tech lists by unit type' dictionary to determine the unit type
        // since the number of tech lists is limited by the number of unit types, it shouldn't be too problematic for performance
        for(int key : techListsByUnitType.keySet()) {
            if(techListsByUnitType.get(key).equals(techList)) {
                unitType = key;
                break;
            }
        }
        
        // time to do the work is # units * 2 if mothballing * work day in minutes;
        int workTime = unitsByType.get(unitType).size() * (activating ? 1 : 2) * Unit.TECH_WORK_DAY;
        // a unit can only be mothballed by one tech, so it's pointless to assign more techs to the task
        int numTechs = Math.min(techList.getSelectedValuesList().size(), unitsByType.get(unitType).size());
        
        timeLabelsByUnitType.get(unitType).setText(getCompletionTimeText(workTime / numTechs));
    }
    
    private String getCompletionTimeText(int completionTime) {
        if(completionTime > 0) {
            return String.format("Completion Time: %d minutes", completionTime);
        } else {
            return "<html>Completion Time: <span color='red'>Never</span></html>";
        }
    }

    
}

package mekhq.gui.dialog;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import megamek.common.OffBoardDirection;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.gui.utilities.Java2sAutoTextField;

public class ObjectiveEditPanel extends JDialog {
    private JLabel lblShortDescription;
    private JTextArea txtShortDescription;
    private JLabel lblObjectiveType;
    private JComboBox<ObjectiveCriterion> cboObjectiveType;
    private JLabel lblDirection;
    private JComboBox<OffBoardDirection> cboDirection;
    private JLabel lblPercentage;
    private JTextField txtPercentage;
    private JLabel forceName;
    private Java2sAutoTextField txtForceName;
    
    private Scenario currentScenario;
    
    public ObjectiveEditPanel(Scenario scenario, JFrame parent) {
        currentScenario = scenario;
        
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initGUI() {
        lblShortDescription = new JLabel("Short Description:");
        lblObjectiveType = new JLabel("Objective Type:");
        lblPercentage = new JLabel("%%");
        
        txtShortDescription = new JTextArea();
        txtShortDescription.setColumns(40);
        txtShortDescription.setRows(5);
        
        cboObjectiveType = new JComboBox<>();
        for(ObjectiveCriterion objectiveType : ObjectiveCriterion.values()) {
            cboObjectiveType.addItem(objectiveType);
        }
        
        txtPercentage = new JTextField();
        txtPercentage.setColumns(4);
        
        cboDirection = new JComboBox<>();
        for(OffBoardDirection direction : OffBoardDirection.values()) {
            cboDirection.addItem(direction);
        }
        
        txtForceName = new Java2sAutoTextField(getAvailableForceNames());
        txtForceName.setColumns(40);
    }
    
    private List<String> getAvailableForceNames() {
        List<String> retVal = new ArrayList<>();
        
        if(currentScenario instanceof AtBScenario) {
            for(int x = 0; x < ((AtBScenario) currentScenario).getNumBots(); x++) {
                retVal.add(((AtBScenario) currentScenario).getBotForce(x).getName());
            }
        }
        
        //for()
        
        return retVal;
    }
}

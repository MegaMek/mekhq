package mekhq.gui.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;

/**
 * A table model for displaying scenarios
 */
public class ScenarioTableModel extends DataTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    Campaign campaign;
    
    private final static int COL_NAME       = 0;
    private final static int COL_STATUS     = 1;
    private final static int COL_DATE       = 2;
    private final static int COL_ASSIGN     = 3;
    private final static int N_COL          = 4;

    public ScenarioTableModel(Campaign c) {
        data = new ArrayList<Scenario>();
        campaign = c;
    }
    
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
        case COL_NAME:
            return "Scenario Name";
        case COL_STATUS:
            return "Resolution";
        case COL_DATE:
            return "Date";
        case COL_ASSIGN:
            return "# Units";
        default:
            return "?";
        }
    }

    public Object getValueAt(int row, int col) {
        Scenario scenario = getScenario(row);
        if(col == COL_NAME) {
            return scenario.getName();
        }
        if(col == COL_STATUS) {
            return scenario.getStatusName();
        }
        if(col == COL_DATE) {
            if(null == scenario.getDate()) {
                return "-";
            } else {
                SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                return shortDateFormat.format(scenario.getDate());
            }
        }
        if(col == COL_ASSIGN) {
            return scenario.getForces(getCampaign()).getAllUnits().size();
        }
        return "?";
    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_NAME:
            return 100;
        case COL_STATUS:
            return 50;
        default:
            return 20;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
        default:
            return SwingConstants.LEFT;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Scenario getScenario(int row) {
        return (Scenario)data.get(row);
    }
    
    private Campaign getCampaign() {
        return campaign;
    }
}

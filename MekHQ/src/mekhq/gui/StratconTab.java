package mekhq.gui;

import java.awt.BorderLayout;
import javax.swing.JScrollPane;

public class StratconTab extends CampaignGuiTab {
    /**
     * 
     */
    private static final long serialVersionUID = 8179754409939346465L;
    
    private StratconPanel stratconPanel;

    StratconTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        
        this.setLayout(new BorderLayout());
        stratconPanel = new StratconPanel(gui);
        JScrollPane scrollPane = new JScrollPane(stratconPanel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(StratconPanel.HEX_X_RADIUS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(StratconPanel.HEX_Y_RADIUS);
        this.add(scrollPane);
    }

    @Override
    public void initTab() {        
    }

    @Override
    public void refreshAll() {
        stratconPanel.repaint();
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.STRATCON;
    }
}
package mekhq.gui.panels;

import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryOutcome;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.model.StoryOutcomeModel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StoryPointEditorPanel extends AbstractMHQScrollablePanel {

    private StoryPoint storyPoint;

    // What do we need to track
    // name (done)
    // previous story points (done)
    // personality
    // splash image
    // StoryOutcomes and StoryTriggers
    private JTextField txtName;

    private JTable storyOutcomeTable;
    private StoryOutcomeModel storyOutcomeModel;

    public StoryPointEditorPanel(JFrame frame, String name, StoryPoint sp) {
        super(frame, name);
        storyPoint = sp;
        initialize();
    }

    @Override
    protected void initialize() {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><b>Story Point Name:</b></html>"), gbc);

        txtName = new JTextField(storyPoint.getName());
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><b>Linking Story Points:</b></html>"), gbc);

        StringBuilder sb = new StringBuilder();
        List<StoryPoint> linkedStoryPoints = storyPoint.getLinkingStoryPoints();
        String SEPARATOR = "";
        for (int i = 0; i < linkedStoryPoints.size(); i++) {
            sb.append(SEPARATOR);
            sb.append(linkedStoryPoints.get(i).getName());
            SEPARATOR = ", ";
        }
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel(sb.toString()), gbc);

        JPanel pnlOutcomes = new JPanel(new BorderLayout());
        pnlOutcomes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Story Outcomes and Triggers"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        storyOutcomeModel = new StoryOutcomeModel(storyPoint.getStoryOutcomes(), storyPoint.getStoryArc());
        // check for a default outcome or trigger
        if (storyPoint.getNextStoryPointId() != null || storyPoint.getStoryTriggers().size() > 0) {
            StoryOutcome defaultOutcome = new StoryOutcome();
            defaultOutcome.setResult("<DEFAULT>");
            if (storyPoint.getNextStoryPointId() != null) {
                defaultOutcome.setNextStoryPointId(storyPoint.getNextStoryPointId());
            }
            defaultOutcome.setStoryTriggers(storyPoint.getStoryTriggers());
            storyOutcomeModel.addOutcome(defaultOutcome);
        }
        storyOutcomeTable = new JTable(storyOutcomeModel);
        TableColumn column;
        for (int i = 0; i < StoryOutcomeModel.N_COL; i++) {
            column = storyOutcomeTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(storyOutcomeModel.getColumnWidth(i));
            column.setCellRenderer(storyOutcomeModel.getRenderer());
        }
        storyOutcomeTable.setIntercellSpacing(new Dimension(0, 0));
        storyOutcomeTable.setShowGrid(false);
        storyOutcomeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        pnlOutcomes.add(new JScrollPane(storyOutcomeTable), BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        add(pnlOutcomes, gbc);
    }
}

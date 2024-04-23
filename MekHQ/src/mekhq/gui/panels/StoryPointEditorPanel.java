package mekhq.gui.panels;

import mekhq.StoryPointHyperLinkListener;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryOutcome;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.gui.StoryArcEditorGUI;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.model.StoryOutcomeModel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StoryPointEditorPanel extends AbstractMHQScrollablePanel {

    private StoryArcEditorGUI editorGUI;
    private StoryPoint storyPoint;

    // What do we need to track
    // name (done)
    // previous story points (done)
    // personality
    // splash image
    // StoryOutcomes and StoryTriggers
    private JTextField txtName;
    private JPanel pnlOutcomes;

    private JTable storyOutcomeTable;
    private StoryOutcomeModel storyOutcomeModel;

    public StoryPointEditorPanel(JFrame frame, String name, StoryPoint sp, StoryArcEditorGUI gui) {
        super(frame, name);
        storyPoint = sp;
        this.editorGUI = gui;
        initialize();
    }

    @Override
    protected void initialize() {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><h2>" + storyPoint.getClass().getSimpleName() + "</h2></html>"), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
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
            sb.append(linkedStoryPoints.get(i).getHyperlinkedName());
            SEPARATOR = ", ";
        }
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextPane txtLinking = new JTextPane();
        txtLinking.setContentType("text/html");
        txtLinking.setEditable(false);
        txtLinking.setText(sb.toString());
        txtLinking.addHyperlinkListener(editorGUI.getStoryPointHLL());
        add(txtLinking, gbc);

        refreshOutcomesPanel();
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        add(pnlOutcomes, gbc);
    }

    private void refreshOutcomesPanel() {
        pnlOutcomes = new JPanel(new GridBagLayout());
        pnlOutcomes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Story Outcomes and Triggers"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        // I would prefer to do this with a JTable, but I can't properly use a HyperLinkListener in a JTable
        // so we need to create something else

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlOutcomes.add(new JLabel("<html><b>Result</b></html>"), gbc);
        gbc.gridx++;
        pnlOutcomes.add(new JLabel("<html><b>Next Story Point</b></html>"), gbc);
        gbc.gridx++;
        pnlOutcomes.add(new JLabel("<html><b>Story Triggers</b></html>"), gbc);

        for(StoryOutcome outcome : storyPoint.getStoryOutcomes()) {
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.gridy++;
            pnlOutcomes.add(new JLabel(outcome.getResult()), gbc);
            gbc.gridx++;
            String next = outcome.getNextStoryPointId() == null ? "" :
                    storyPoint.getStoryArc().getStoryPoint(outcome.getNextStoryPointId()).getHyperlinkedName();
            JTextPane txtNext = new JTextPane();
            txtNext.setContentType("text/html");
            txtNext.setEditable(false);
            txtNext.setText(next);
            txtNext.addHyperlinkListener(editorGUI.getStoryPointHLL());
            pnlOutcomes.add(txtNext, gbc);
            gbc.gridx++;
            pnlOutcomes.add(new JLabel(getStoryTriggerDescription(outcome.getStoryTriggers())), gbc);
            gbc.gridx++;
            gbc.weightx = 0.0;
            pnlOutcomes.add(new JButton("Edit Outcome"), gbc);
            gbc.gridx++;
            pnlOutcomes.add(new JButton("Delete Outcome"), gbc);
        }

        // check for a default outcome or trigger
        if (storyPoint.getNextStoryPointId() != null || storyPoint.getStoryTriggers().size() > 0) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 1.0;
            pnlOutcomes.add(new JLabel("<html><i>DEFAULT</i></html>"), gbc);
            gbc.gridx++;
            String next = storyPoint.getNextStoryPointId() == null ? "" :
                    storyPoint.getStoryArc().getStoryPoint(storyPoint.getNextStoryPointId()).getHyperlinkedName();
            JTextPane txtNext = new JTextPane();
            txtNext.setContentType("text/html");
            txtNext.setEditable(false);
            txtNext.setText(next);
            txtNext.addHyperlinkListener(editorGUI.getStoryPointHLL());
            pnlOutcomes.add(txtNext, gbc);
            gbc.gridx++;
            pnlOutcomes.add(new JLabel(getStoryTriggerDescription(storyPoint.getStoryTriggers())), gbc);
            gbc.gridx++;
            gbc.weightx = 0.0;
            pnlOutcomes.add(new JButton("Edit Outcome"), gbc);
            gbc.gridx++;
            pnlOutcomes.add(new JButton("Delete Outcome"), gbc);
        }
    }

    private String getStoryTriggerDescription(List<StoryTrigger> triggers) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        for(StoryTrigger trigger : triggers) {
            sb.append(trigger.getClass().getSimpleName());
            sb.append("<br>");
        }
        sb.append("</html>");
        return sb.toString();
    }
}

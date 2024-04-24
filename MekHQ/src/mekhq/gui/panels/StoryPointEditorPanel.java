package mekhq.gui.panels;

import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryOutcome;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.gui.StoryArcEditorGUI;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.dialog.CustomizeStoryOutcomeDialog;
import mekhq.gui.model.StoryOutcomeModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StoryPointEditorPanel extends AbstractMHQScrollablePanel {

    private StoryArcEditorGUI editorGUI;
    private StoryPoint storyPoint;

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
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><h2>" + storyPoint.getClass().getSimpleName() + "</h2></html>"), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("<html><b><nobr>Story Point Name:</nobr></b></html>"), gbc);

        txtName = new JTextField(storyPoint.getName());
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("<html><b><nobr>Linking Story Points:</nobr></b></html>"), gbc);

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

        pnlOutcomes = new JPanel(new GridBagLayout());
        pnlOutcomes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Story Outcomes and Triggers"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        add(pnlOutcomes, gbc);
        refreshOutcomesPanel();
    }

    private void refreshOutcomesPanel() {

        pnlOutcomes.removeAll();
        // I would prefer to do this with a JTable, but I can't properly use a HyperLinkListener in a JTable
        // so we need to create something else
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlOutcomes.add(new JLabel("<html><b><nobr>Result</nobr></b></html>"), gbc);
        gbc.gridx++;
        pnlOutcomes.add(new JLabel("<html><b><nobr>Next Story Point</nobr></b></html>"), gbc);
        gbc.gridx++;
        pnlOutcomes.add(new JLabel("<html><b><nobr>Story Triggers</nobr></b></html>"), gbc);

        List<String> currentOutcomes = new ArrayList<>();
        for(StoryOutcome outcome : storyPoint.getStoryOutcomes()) {
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.gridy++;
            JTextPane txtResult = new JTextPane();
            txtResult.setContentType("text/html");
            txtResult.setEditable(false);
            txtResult.setText(outcome.getResult());
            pnlOutcomes.add(txtResult, gbc);
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
            JTextPane txtTriggers = new JTextPane();
            txtTriggers.setContentType("text/html");
            txtTriggers.setEditable(false);
            txtTriggers.setText(getStoryTriggerDescription(outcome.getStoryTriggers()));
            txtTriggers.addHyperlinkListener(editorGUI.getStoryPointHLL());
            pnlOutcomes.add(txtTriggers, gbc);
            gbc.gridx++;
            gbc.weightx = 0.0;
            JButton btnEdit = new JButton("Edit Outcome");
            btnEdit.addActionListener(evt -> editOutcome(outcome.getResult()));
            pnlOutcomes.add(btnEdit, gbc);
            gbc.gridx++;
            JButton btnRemove = new JButton("Delete Outcome");
            btnRemove.addActionListener(evt -> removeOutcome(outcome.getResult()));
            pnlOutcomes.add(btnRemove, gbc);
            currentOutcomes.add(outcome.getResult());
        }

        // check for a default outcome or trigger
        if (storyPoint.getNextStoryPointId() != null || storyPoint.getStoryTriggers().size() > 0) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 1.0;
            JTextPane txtResult = new JTextPane();
            txtResult.setContentType("text/html");
            txtResult.setEditable(false);
            txtResult.setText("<i>" + StoryArc.defaultOutcome + "</i>");
            pnlOutcomes.add(txtResult, gbc);
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
            JTextPane txtTriggers = new JTextPane();
            txtTriggers.setContentType("text/html");
            txtTriggers.setEditable(false);
            txtTriggers.setText(getStoryTriggerDescription(storyPoint.getStoryTriggers()));
            txtTriggers.addHyperlinkListener(editorGUI.getStoryPointHLL());
            pnlOutcomes.add(txtTriggers, gbc);
            gbc.gridx++;
            gbc.weightx = 0.0;
            JButton btnEdit = new JButton("Edit Outcome");
            btnEdit.addActionListener(evt -> editOutcome(StoryArc.defaultOutcome));
            pnlOutcomes.add(btnEdit, gbc);
            gbc.gridx++;
            JButton btnRemove = new JButton("Delete Outcome");
            btnRemove.addActionListener(evt -> removeOutcome(StoryArc.defaultOutcome));
            pnlOutcomes.add(btnRemove, gbc);
            currentOutcomes.add(StoryArc.defaultOutcome);
        }

        //check for other possible outcomes
        JComboBox comboOutcomes = new JComboBox();
        List<String> possibleResults = storyPoint.getAllPossibleResults();
        possibleResults.removeAll(currentOutcomes);
        if(!possibleResults.isEmpty()) {
            for (String result : possibleResults) {
                comboOutcomes.addItem(result);
            }

            gbc.gridx = 0;
            gbc.gridy++;
            pnlOutcomes.add(new JButton("Add Outcome"), gbc);
            gbc.gridx = 1;
            gbc.gridwidth = 4;
            gbc.fill = GridBagConstraints.NONE;
            pnlOutcomes.add(comboOutcomes, gbc);
        }
    }

    private String getStoryTriggerDescription(List<StoryTrigger> triggers) {
        StringBuilder sb = new StringBuilder();
        for(StoryTrigger trigger : triggers) {
            sb.append(trigger.getDescription());
            sb.append("<br>");
        }
        return sb.toString();
    }

    public void removeOutcome(String result) {
        if(result.equals(StoryArc.defaultOutcome)) {
            storyPoint.removeDefaultOutcome();
        } else {
            storyPoint.removeStoryOutcome(result);
        }
        SwingUtilities.invokeLater(() -> refreshOutcomesPanel());
        SwingUtilities.invokeLater(() ->pnlOutcomes.revalidate());
    }

    public void editOutcome(String result) {
        CustomizeStoryOutcomeDialog csod = new CustomizeStoryOutcomeDialog(getFrame(), true, result, storyPoint);
        csod.setVisible(true);
        refreshOutcomesPanel();
        pnlOutcomes.revalidate();
    }
}

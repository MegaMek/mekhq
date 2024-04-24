package mekhq.gui.dialog;

import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryOutcome;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.utilities.JSuggestField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Vector;

public class CustomizeStoryOutcomeDialog extends JDialog {

    JFrame frame;
    StoryOutcome outcome;
    StoryPoint storyPoint;
    String result;
    boolean isNewOutcome;
    boolean isCustom;

    private JSuggestField suggestNext;
    private JTextField txtResult;

    public CustomizeStoryOutcomeDialog(JFrame parent, boolean modal, String result, StoryPoint sp, boolean isNew) {
        super(parent, modal);
        this.frame = parent;
        this.storyPoint = sp;
        this.isNewOutcome = isNew;
        this.result = result;
        if (this.result.equals(StoryPoint.DEFAULT_OUTCOME)) {
            outcome = new StoryOutcome();
            outcome.setNextStoryPointId(storyPoint.getNextStoryPointId());
            outcome.setStoryTriggers(storyPoint.getStoryTriggers());
        } else if (isNewOutcome) {
            outcome = new StoryOutcome();
        } else {
            outcome = storyPoint.getStoryOutcome(this.result);
        }
        initialize();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initialize() {
        setTitle("Customize Story Outcome");
        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridBagLayout());
        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);

        JButton btnOk = new JButton("OK");
        btnOk.addActionListener(this::done);
        panButtons.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this::cancel);
        panButtons.add(btnCancel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel("<html><b><nobr>Result:</nobr></b></html>"), gbc);
        gbc.gridy++;
        panMain.add(new JLabel("<html><b><nobr>Next Story Point:</nobr></b></html>"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        txtResult = new JTextField(result);
        txtResult.setEditable(result.equals(StoryPoint.CUSTOM_OUTCOME));
        panMain.add(txtResult, gbc);

        Vector<String> otherPoints = new Vector<String>();
        for (StoryPoint sp : storyPoint.getStoryArc().getStoryPoints()) {
            if (sp.getId().equals(storyPoint.getId())) {
                continue;
            }
            otherPoints.add(sp.getName());
        }
        suggestNext = new JSuggestField(this, otherPoints);

        if (outcome.getNextStoryPointId() != null) {
            suggestNext.setText(storyPoint.getStoryArc().getStoryPoint(outcome.getNextStoryPointId()).getName());
        }
        gbc.gridy++;
        panMain.add(suggestNext, gbc);
    }

    private void done(ActionEvent evt) {
        result = txtResult.getText();
        //set no next story point as default and then look for it
        outcome.setNextStoryPointId(null);
        // need to find by name which might not be unique
        for (StoryPoint sp : storyPoint.getStoryArc().getStoryPoints()) {
            if (suggestNext.getText().equals(sp.getName())) {
                outcome.setNextStoryPointId(sp.getId());
                break;
            }
        }

        // if this is the default or new, we need to do some additional stuff
        if (result.equals(StoryPoint.DEFAULT_OUTCOME)) {
            // if the default, we apply directly to the story point
            storyPoint.setNextStoryPointId(outcome.getNextStoryPointId());
        } else if(isNewOutcome) {
            // add a new outcome
            outcome.setResult(result);
            storyPoint.addStoryOutcome(result, outcome);
        }
        setVisible(false);
    }

    private void cancel(ActionEvent evt) {
        setVisible(false);
    }


}

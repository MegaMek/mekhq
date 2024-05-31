package mekhq.gui.dialog;

import mekhq.campaign.storyarc.StoryOutcome;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storytrigger.GameOverStoryTrigger;
import mekhq.gui.StoryArcEditorGUI;
import mekhq.gui.panels.storytriggerpanels.StoryTriggerPanel;
import mekhq.gui.utilities.JSuggestField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Vector;

public class CustomizeStoryOutcomeDialog extends JDialog {

    JFrame frame;
    private StoryOutcome outcome;
    private StoryPoint storyPoint;
    private String result;
    boolean isNewOutcome;
    private JSuggestField suggestNext;

    JComboBox<String> choiceAddTrigger;

    private ArrayList<StoryTriggerPanel> triggerPanels;
    private JPanel panTriggers;
    private JScrollPane scrTriggers;

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
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel("<html><b><nobr>Result:</nobr></b></html>"), gbc);
        gbc.gridy++;
        panMain.add(new JLabel("<html><b><nobr>Next Story Point:</nobr></b></html>"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panMain.add(new JLabel(result), gbc);

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

        choiceAddTrigger = new JComboBox<>();
        for(String s : StoryArcEditorGUI.availableTriggers.keySet()) {
            choiceAddTrigger.addItem(s);
        }
        JButton btnAddTrigger = new JButton("Add Story Trigger");
        btnAddTrigger.addActionListener(evt -> addTrigger());
        JPanel panAddTrigger = new JPanel(new BorderLayout());
        panAddTrigger.add(choiceAddTrigger, BorderLayout.CENTER);
        panAddTrigger.add(btnAddTrigger, BorderLayout.LINE_END);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panMain.add(panAddTrigger, gbc);

        panTriggers = new JPanel();
        panTriggers.setLayout(new BoxLayout(panTriggers, BoxLayout.Y_AXIS));

        triggerPanels = new ArrayList<>();
        for(StoryTrigger trigger : outcome.getStoryTriggers()) {
            StoryTriggerPanel panTrigger = trigger.getPanel(frame);
            panTrigger.getDeleteButton().addActionListener(evt -> removePanel(panTrigger));
            panTriggers.add(panTrigger);
            triggerPanels.add(panTrigger);
        }

        scrTriggers = new JScrollPane(panTriggers);
        scrTriggers.setMinimumSize(new Dimension(400, 200));
        //scrTriggers.setPreferredSize(new Dimension(400, 200));
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panMain.add(scrTriggers, gbc);
    }

    private void removePanel(StoryTriggerPanel panel) {
        triggerPanels.remove(panel);
        refreshTriggerPanels();
    }

    private void refreshTriggerPanels() {
        panTriggers = new JPanel();
        panTriggers.setLayout(new BoxLayout(panTriggers, BoxLayout.Y_AXIS));
        for(StoryTriggerPanel panel : triggerPanels) {
            panTriggers.add(panel);
        }
        scrTriggers.setViewportView(panTriggers);
    }

    private void done(ActionEvent evt) {
        //set no next story point as default and then look for it
        outcome.setNextStoryPointId(null);
        // need to find by name which might not be unique
        for (StoryPoint sp : storyPoint.getStoryArc().getStoryPoints()) {
            if (suggestNext.getText().equals(sp.getName())) {
                outcome.setNextStoryPointId(sp.getId());
                break;
            }
        }
        ArrayList<StoryTrigger> triggers = new ArrayList<>();
        for(StoryTriggerPanel panel : triggerPanels) {
            panel.updateStoryStrigger();
            triggers.add(panel.getStoryTrigger());
        }
        outcome.setStoryTriggers(triggers);
        // if this is the default or new, we need to do some additional stuff
        if (result.equals(StoryPoint.DEFAULT_OUTCOME)) {
            // if the default, we apply directly to the story point
            storyPoint.setNextStoryPointId(outcome.getNextStoryPointId());
            storyPoint.setStoryTriggers(outcome.getStoryTriggers());
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

    private JFrame getFrame() {
        return frame;
    }

    private void addTrigger() {
        String triggerName = (String) choiceAddTrigger.getSelectedItem();
        String className = StoryArcEditorGUI.availableTriggers.get(triggerName);
        StoryTrigger trigger = null;
        if(className != null) {
            try {
                trigger = (StoryTrigger) Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if(trigger != null) {
            trigger.setStoryArc(storyPoint.getStoryArc());
            StoryTriggerPanel panTrigger = trigger.getPanel(getFrame());
            panTrigger.getDeleteButton().addActionListener(evt -> removePanel(panTrigger));
            triggerPanels.add(panTrigger);
            refreshTriggerPanels();
        }
    }
}

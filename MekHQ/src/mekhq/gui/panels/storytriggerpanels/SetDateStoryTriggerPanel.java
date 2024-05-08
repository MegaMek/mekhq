package mekhq.gui.panels.storytriggerpanels;

import mekhq.MekHQ;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.campaign.storyarc.StoryTrigger;
import mekhq.campaign.storyarc.storypoint.CheckDateReachedStoryPoint;
import mekhq.campaign.storyarc.storytrigger.SetDateStoryTrigger;
import mekhq.gui.dialog.DateChooser;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetDateStoryTriggerPanel extends StoryTriggerPanel {

    private LocalDate date;
    private List<UUID> checkDateStoryPoints;
    private JComboBox<String> comboStoryPoints;
    private JSpinner spinDays;
    private JButton btnDate;
    private JCheckBox checkUseDate;

    public SetDateStoryTriggerPanel(JFrame frame, String name, SetDateStoryTrigger trigger) {
        super(frame, name, trigger);
    }

    @Override
    protected void createMainPanel() {
        comboStoryPoints = new JComboBox<>();
        checkDateStoryPoints = new ArrayList<>();
        for(StoryPoint storyPoint : getStoryTrigger().getStoryArc().getStoryPoints()) {
            if(storyPoint instanceof CheckDateReachedStoryPoint) {
                comboStoryPoints.addItem(storyPoint.getName());
                checkDateStoryPoints.add(storyPoint.getId());
            }
        }
        UUID currentStoryPointId = ((SetDateStoryTrigger) getStoryTrigger()).getStoryPointId();
        StoryPoint currentStoryPoint = null;
        if(currentStoryPointId != null) {
            currentStoryPoint = getStoryTrigger().getStoryArc().getStoryPoint(currentStoryPointId);
        }
        if(currentStoryPoint != null) {
            comboStoryPoints.setSelectedItem(currentStoryPoint.getName());
        }

        date = ((SetDateStoryTrigger) getStoryTrigger()).getDate();
        btnDate = new JButton("None");
        btnDate.setEnabled(false);
        if(date != null) {
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
            btnDate.setEnabled(true);
        }
        btnDate.addActionListener(evt -> changeDate());

        checkUseDate = new JCheckBox("Use Actual Date");
        checkUseDate.setSelected(date != null);
        checkUseDate.addActionListener(evt -> checkUseDate());

        spinDays = new JSpinner(new SpinnerNumberModel(((SetDateStoryTrigger) getStoryTrigger()).getFutureDays(),
                1, null, 1));
        spinDays.setEnabled(date == null);

        getMainPanel().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        getMainPanel().add(new JLabel("CheckDateReachedStoryPoint:"), gbc);
        gbc.gridy++;
        gbc.gridwidth = 2;
        getMainPanel().add(checkUseDate, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        JLabel lblActualDate = new JLabel("Actual Date:");
        lblActualDate.setToolTipText("Set a specific date.");
        getMainPanel().add(lblActualDate, gbc);
        gbc.gridy++;
        JLabel lblDays = new JLabel("Days in Future:");
        lblDays.setToolTipText("Set a date this many days in the future from when this trigger is executed.");
        getMainPanel().add(lblDays, gbc);

        gbc.gridy = 0;
        gbc.gridx++;
        gbc.weightx = 1.0;
        getMainPanel().add(comboStoryPoints, gbc);
        gbc.gridy++;
        gbc.gridy++;
        getMainPanel().add(btnDate, gbc);
        gbc.gridy++;
        getMainPanel().add(spinDays, gbc);
    }

    @Override
    public void updateStoryStrigger() {
        ((SetDateStoryTrigger) getStoryTrigger()).setStoryPointId(checkDateStoryPoints.get(comboStoryPoints.getSelectedIndex()));
        if(checkUseDate.isSelected()) {
            ((SetDateStoryTrigger) getStoryTrigger()).setDate(date);
        } else {
            ((SetDateStoryTrigger) getStoryTrigger()).setFutureDays((int) spinDays.getModel().getValue());
        }
    }

    private void changeDate() {
        // show the date chooser
        DateChooser dc = new DateChooser(getFrame(), date);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            date = dc.getDate();
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        }
    }

    private void checkUseDate() {
        if(checkUseDate.isSelected()) {
            date = LocalDate.of(3025, 1, 1);
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
            btnDate.setEnabled(true);
            spinDays.setEnabled(false);
        } else {
            date = null;
            btnDate.setText("None");
            btnDate.setEnabled(false);
            spinDays.setEnabled(true);
        }
    }
}

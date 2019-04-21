package mekhq.gui.dialog;

import mekhq.MekHQ;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

public class MekHqOptionsDialog extends JDialog {
    ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog");

    public MekHqOptionsDialog(JFrame parent) {
        super(parent);

        initComponents();
        setInitialState();
        setUserPreferences();
    }

    private void initComponents() {
        this.setTitle(resources.getString("dialog.text"));
        this.setName("dialog.name");

        JLabel labelSavedInfo = new JLabel("labelSavedInfo.text");
        labelSavedInfo.setName("labelSavedInfo.name");

        JRadioButton optionSaveDaily = new JRadioButton(resources.getString("optionSaveDaily.text"));
        optionSaveDaily.setName("optionSaveDaily.name");
        optionSaveDaily.setMnemonic(KeyEvent.VK_D);

        JRadioButton optionSaveWeekly = new JRadioButton(resources.getString("optionSaveWeekly.text"));
        optionSaveWeekly.setName("optionSaveWeekly.name");
        optionSaveWeekly.setMnemonic(KeyEvent.VK_W);

        ButtonGroup saveFrequencyGroup = new ButtonGroup();
        saveFrequencyGroup.add(optionSaveDaily);
        saveFrequencyGroup.add(optionSaveWeekly);

        JCheckBox checkSaveBeforeMissions = new JCheckBox(resources.getString("checkSaveBeforeMissions.text"));
        checkSaveBeforeMissions.setName("checkSaveBeforeMissions.name");
        checkSaveBeforeMissions.setMnemonic(KeyEvent.VK_S);

        JLabel labelSavedGamesCount = new JLabel("labelSavedGamesCount.text");
        labelSavedGamesCount.setName("labelSavedGamesCount.name");

        JSpinner spinnerSavedGamesCount = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        spinnerSavedGamesCount.setName("spinnerSavedGamesCount.name");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(labelSavedInfo)
                .addComponent(optionSaveDaily)
                .addComponent(optionSaveWeekly)
                .addComponent(checkSaveBeforeMissions)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(labelSavedGamesCount)
                    .addComponent(spinnerSavedGamesCount, GroupLayout.Alignment.TRAILING))
        );

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(labelSavedInfo)
                .addComponent(optionSaveDaily)
                .addComponent(optionSaveWeekly)
                .addComponent(checkSaveBeforeMissions)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(labelSavedGamesCount)
                    .addComponent(spinnerSavedGamesCount))
        );
    }

    private void setInitialState() {

    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(MekHqOptionsDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
}

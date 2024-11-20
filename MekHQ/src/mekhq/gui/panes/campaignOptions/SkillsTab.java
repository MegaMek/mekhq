package mekhq.gui.panes.campaignOptions;

import javax.swing.*;

public class SkillsTab {
    JFrame frame;
    String name;

    //start Combat Skills Tab
    //end Combat Skills Tab

    //start Support Skills Tab
    //end Support Skills Tab

    SkillsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeCombatSkillsTab();
        initializeSupportSkillsTab();
    }

    private void initializeCombatSkillsTab() {
    }
    private void initializeSupportSkillsTab() {
    }
}

package mekhq.gui.panes.campaignOptions.tabs;

import javax.swing.*;

public class FinancesTab {
    JFrame frame;
    String name;

    //start General Options
    //end General Options

    //start Price Multipliers
    //end Price Multipliers

    FinancesTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        createFinancesGeneralOptionsTab();
        createPriceMultipliersTab();
    }

    private void initializeGeneralOptionsTab() {
    }

    JPanel createFinancesGeneralOptionsTab() {

    }

    private void initializePriceMultipliersTab() {
    }

    JPanel createPriceMultipliersTab() {

    }
}

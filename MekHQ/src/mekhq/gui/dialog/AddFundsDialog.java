package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.gui.utilities.JMoneyTextField;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * @author natit
 */
public class AddFundsDialog extends JDialog implements FocusListener {
    private JButton btnAddFunds;
    private JMoneyTextField fundsQuantityField;
    private JFormattedTextField descriptionField;
    private MMComboBox<TransactionType> categoryCombo;
    private int closedType = JOptionPane.CLOSED_OPTION;
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddFundsDialog",
            MekHQ.getMHQOptions().getLocale());

    public AddFundsDialog(final JFrame frame, final boolean modal) {
        super(frame, modal);
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));

        btnAddFunds = new JButton();
        btnAddFunds.setText(resourceMap.getString("btnAddFunds.text"));
        btnAddFunds.setActionCommand(resourceMap.getString("btnAddFunds.actionCommand"));
        btnAddFunds.setName("btnAddFunds");
        btnAddFunds.addActionListener(this::btnAddFundsActionPerformed);

        getContentPane().add(buildFieldsPanel(), BorderLayout.NORTH);
        getContentPane().add(btnAddFunds, BorderLayout.PAGE_END);

        setLocationRelativeTo(getParent());
        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(AddFundsDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        fundsQuantityField = new JMoneyTextField();
        fundsQuantityField.setText(resourceMap.getString("fundsQuantityField.text"));
        fundsQuantityField.setToolTipText(resourceMap.getString("fundsQuantityField.toolTipText"));
        fundsQuantityField.setName("fundsQuantityField");
        fundsQuantityField.setColumns(10);
        panel.add(fundsQuantityField);

        categoryCombo = new MMComboBox<>("categoryCombo", TransactionType.values());
        categoryCombo.setSelectedItem(TransactionType.MISCELLANEOUS);
        categoryCombo.setToolTipText("The category the transaction falls into.");
        categoryCombo.setName("categoryCombo");
        panel.add(categoryCombo);

        descriptionField = new JFormattedTextField("Rich Uncle");
        descriptionField.addActionListener(x -> this.btnAddFundsActionPerformed(null));
        descriptionField.addFocusListener(this);
        descriptionField.setToolTipText("Description of the transaction.");
        descriptionField.setName("descriptionField");
        descriptionField.setColumns(20);
        panel.add(descriptionField);
        return panel;
    }

    public Money getFundsQuantityField() {
        return fundsQuantityField.getMoney();
    }

    public String getFundsDescription() {
        return descriptionField.getText();
    }

    public TransactionType getTransactionType() {
        return categoryCombo.getSelectedItem();
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (descriptionField.equals(e.getSource())) {
            SwingUtilities.invokeLater(() -> descriptionField.selectAll());
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        //not used
    }

    public int getClosedType() {
        return closedType;
    }

    private void btnAddFundsActionPerformed(ActionEvent evt) {
        this.closedType = JOptionPane.OK_OPTION;
        this.setVisible(false);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            AddFundsDialog dialog = new AddFundsDialog(new JFrame(), true);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            dialog.setVisible(true);
        });
    }
}

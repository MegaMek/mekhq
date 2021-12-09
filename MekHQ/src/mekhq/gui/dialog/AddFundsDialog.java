/*
 * AlertPopup.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */

package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.gui.utilities.JMoneyTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * @author natit
 */
public class AddFundsDialog extends JDialog implements FocusListener {
	private static final long serialVersionUID = -6946480787293179307L;

    private JButton btnAddFunds;
    private JMoneyTextField fundsQuantityField;
    private JFormattedTextField descriptionField;
    private MMComboBox<TransactionType> categoryCombo;
    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddFundsDialog", new EncodeControl()); //$NON-NLS-1$
    private int closedType = JOptionPane.CLOSED_OPTION;

	/** Creates new form AlertPopup */
    public AddFundsDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));

        btnAddFunds = new JButton();
        btnAddFunds.setText(resourceMap.getString("btnAddFunds.text")); // NOI18N
        btnAddFunds.setActionCommand(resourceMap.getString("btnAddFunds.actionCommand")); // NOI18N
        btnAddFunds.setName("btnAddFunds"); // NOI18N
        btnAddFunds.addActionListener(this::btnAddFundsActionPerformed);

        getContentPane().add(buildFieldsPanel(), BorderLayout.NORTH);
        getContentPane().add(btnAddFunds, BorderLayout.PAGE_END);

        setLocationRelativeTo(getParent());
        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(AddFundsDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        fundsQuantityField = new JMoneyTextField(() ->  btnAddFundsActionPerformed(null));
        fundsQuantityField.setText(resourceMap.getString("fundsQuantityField.text")); // NOI18N
        fundsQuantityField.setToolTipText(resourceMap.getString("fundsQuantityField.toolTipText")); // NOI18N
        fundsQuantityField.setName("fundsQuantityField"); // NOI18N
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

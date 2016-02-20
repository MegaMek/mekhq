/*
 * AlertPopup.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import mekhq.campaign.finances.Transaction;

/**
 *
 * @author natit
 */
public class AddFundsDialog extends JDialog implements FocusListener {
	private static final long serialVersionUID = -6946480787293179307L;

    private JFormattedTextField descriptionField;
    private JComboBox<String> categoryCombo;
    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddFundsDialog");
    private int closedType = JOptionPane.CLOSED_OPTION;

	/** Creates new form AlertPopup */
    public AddFundsDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    private void initComponents() {

        btnAddFunds = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        setTitle(resourceMap.getString("Form.title"));

        btnAddFunds.setText(resourceMap.getString("btnAddFunds.text")); // NOI18N
        btnAddFunds.setActionCommand(resourceMap.getString("btnAddFunds.actionCommand")); // NOI18N
        btnAddFunds.setName("btnAddFunds"); // NOI18N
        btnAddFunds.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnAddFundsActionPerformed(evt);
            }
        });

        getContentPane().add(buildFieldsPanel(), BorderLayout.NORTH);
        getContentPane().add(btnAddFunds, BorderLayout.PAGE_END);

        setLocationRelativeTo(getParent());
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        
        jFormattedTextFieldFundsQuantity = new JFormattedTextField();
        jFormattedTextFieldFundsQuantity.addFocusListener(this);
        jFormattedTextFieldFundsQuantity.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(NumberFormat.getIntegerInstance())));
        jFormattedTextFieldFundsQuantity.setText(resourceMap.getString("jFormattedTextFieldFundsQuantity.text")); // NOI18N
        jFormattedTextFieldFundsQuantity.setToolTipText(resourceMap.getString("jFormattedTextFieldFundsQuantity.toolTipText")); // NOI18N
        jFormattedTextFieldFundsQuantity.setName("jFormattedTextFieldFundsQuantity"); // NOI18N
        jFormattedTextFieldFundsQuantity.setColumns(10);
        panel.add(jFormattedTextFieldFundsQuantity);

        categoryCombo = new JComboBox<String>(Transaction.getCategoryList());
        categoryCombo.setSelectedItem(Transaction.getCategoryName(Transaction.C_MISC));
        categoryCombo.setToolTipText("The category the transaction falls into.");
        categoryCombo.setName("categoryCombo");
        panel.add(categoryCombo);

        descriptionField = new JFormattedTextField("Rich Uncle");
        descriptionField.addFocusListener(this);
        descriptionField.setToolTipText("Description of the transaction.");
        descriptionField.setName("descriptionField");
        descriptionField.setColumns(20);
        panel.add(descriptionField);

        return panel;
    }

    private void btnAddFundsActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddFundsActionPerformed
        this.closedType = JOptionPane.OK_OPTION;
        this.setVisible(false);
    }//GEN-LAST:event_btnAddFundsActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                AddFundsDialog dialog = new AddFundsDialog(new JFrame(), true);
                dialog.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    public long getFundsQuantity () {
        long fundsQuantity = ((Long) jFormattedTextFieldFundsQuantity.getValue()).longValue();
        return fundsQuantity;
    }

    public String getFundsDescription() {
        return descriptionField.getText();
    }

    public int getCategory() {
        return Transaction.getCategoryIndex((String)categoryCombo.getSelectedItem());
    }

    private JButton btnAddFunds;
    private JFormattedTextField jFormattedTextFieldFundsQuantity;

    @Override
    public void focusGained(FocusEvent e) {
        if (jFormattedTextFieldFundsQuantity.equals(e.getSource())) {
            selectAllTextInField(jFormattedTextFieldFundsQuantity);
        } else if (descriptionField.equals(e.getSource())) {
            selectAllTextInField(descriptionField);
        }
    }

    private void selectAllTextInField(final JFormattedTextField field) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                field.selectAll();
            }
        });
    }

    @Override
    public void focusLost(FocusEvent e) {
        //not used
    }
    
    public int getClosedType() {
        return closedType;
    }
}

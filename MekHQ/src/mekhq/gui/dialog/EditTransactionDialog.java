package mekhq.gui.dialog;

import mekhq.MekHQ;
import mekhq.campaign.finances.Transaction;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

public class EditTransactionDialog extends JDialog implements ActionListener, FocusListener, MouseListener {

    private final DateFormat LONG_DATE = DateFormat.getDateInstance(DateFormat.LONG);

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddFundsDialog");

    private Transaction oldTransaction;
    private Transaction newTransaction;
    private JFrame parent;

    private JFormattedTextField amountField;
    private JTextField descriptionField;
    private JButton dateButton;
    private JComboBox categoryCombo;

    private JButton saveButton;
    private JButton cancelButton;

    public EditTransactionDialog(Transaction transaction, JFrame parent, boolean modal) {
        super(parent, modal);
        oldTransaction = transaction;
        newTransaction = transaction;
        this.parent = parent;

        initGUI();
        setTitle("Edit Financial Transaction");
        setLocationRelativeTo(parent);
        pack();
    }

    private void initGUI() {
        try {
            setLayout(new BorderLayout());
            add(buildMainPanel(), BorderLayout.CENTER);
            add(buildButtonPanel(), BorderLayout.SOUTH);
        } catch (ParseException e) {
            MekHQ.logError(e);
        }
    }

    private JPanel buildMainPanel() throws ParseException {
        JPanel panel = new JPanel();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.BASELINE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(2,2,2,2);

        GridBagLayout l = new GridBagLayout();
        panel.setLayout(l);

        JLabel amountLabel = new JLabel("Amount");
        l.setConstraints(amountLabel, c);
        panel.add(amountLabel);

        c.gridx++;
        JLabel dateLabel = new JLabel("Date");
        l.setConstraints(dateLabel, c);
        panel.add(dateLabel);

        c.gridx++;
        JLabel categoryLabel = new JLabel("Category");
        l.setConstraints(categoryLabel, c);
        panel.add(categoryLabel);

        c.gridx++;
        JLabel descriptionLabel = new JLabel("Description");
        l.setConstraints(descriptionLabel, c);
        panel.add(descriptionLabel);

        c.gridx = 0;
        c.gridy++;
        amountField = new JFormattedTextField();
        amountField.addFocusListener(this);
        amountField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(NumberFormat.getIntegerInstance())));
        amountField.setText(amountField.getFormatter().valueToString(newTransaction.getAmount()));
        amountField.setToolTipText(resourceMap.getString("jFormattedTextFieldFundsQuantity.toolTipText"));
        amountField.setName("amountField");
        amountField.setColumns(10);
        l.setConstraints(amountField, c);
        panel.add(amountField);

        c.gridx++;
        dateButton = new JButton(LONG_DATE.format(newTransaction.getDate()));
        dateButton.addActionListener(this);
        l.setConstraints(dateButton, c);
        panel.add(dateButton);

        c.gridx++;
        categoryCombo = new JComboBox(Transaction.getCategoryList());
        categoryCombo.setSelectedItem(Transaction.getCategoryName(newTransaction.getCategory()));
        categoryCombo.setToolTipText("Category of the transaction");
        categoryCombo.setName("categoryCombo");
        l.setConstraints(categoryCombo, c);
        panel.add(categoryCombo);

        c.gridx++;
        descriptionField = new JTextField(newTransaction.getDescription());
        descriptionField.addFocusListener(this);
        descriptionField.setToolTipText("Description of the transaction.");
        descriptionField.setName("descriptionField");
        descriptionField.setColumns(10);
        l.setConstraints(descriptionField, c);
        panel.add(descriptionField);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        saveButton.setMnemonic('s');
        panel.add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic('c');
        panel.add(cancelButton);

        return panel;
    }

    public Transaction getOldTransaction() {
        return oldTransaction;
    }

    public Transaction getNewTransaction() {
        return newTransaction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (saveButton.equals(e.getSource())) {
            newTransaction.setAmount((Long)amountField.getValue());
            newTransaction.setCategory(Transaction.getCategoryIndex((String) categoryCombo.getSelectedItem()));
            newTransaction.setDescription(descriptionField.getText());
            try {
                newTransaction.setDate(LONG_DATE.parse(dateButton.getText()));
            } catch (ParseException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            newTransaction = oldTransaction;
            setVisible(false);
        } else if (dateButton.equals(e.getSource())) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(newTransaction.getDate());
            DateChooser chooser = new DateChooser(parent, calendar);
            chooser.showDateChooser();
            dateButton.setText(LONG_DATE.format(chooser.getDate().getTime()));
            chooser.dispose();
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (amountField.equals(e.getSource())) {
            selectAllTextInField(amountField);
        } else if (descriptionField.equals(e.getSource())) {
            selectAllTextInField(descriptionField);
        }
    }

    private void selectAllTextInField(final JTextField field) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                field.selectAll();
            }
        });
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

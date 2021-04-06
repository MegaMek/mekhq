/*
 * HireBulkPersonnel.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */
package mekhq.gui.dialog;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.Compute;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.gui.CampaignGUI;
import mekhq.gui.displayWrappers.RankDisplay;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Jay Lawson
 */
public class HireBulkPersonnelDialog extends JDialog {
    private static final long serialVersionUID = -6946480787293179307L;

    private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);
    private static final Insets DEFAULT_INSETS = new Insets(5, 5, 5, 5);

    private Campaign campaign;

    private JComboBox<PersonTypeItem> choiceType;
    private JComboBox<RankDisplay> choiceRanks;
    private DefaultComboBoxModel<RankDisplay> rankModel;
    private JSpinner spnNumber;
    private JTextField jtf;

    private JButton btnHire;
    private JButton btnClose;
    private JPanel panButtons;

    private JSpinner minAge;
    private JSpinner maxAge;

    private boolean useAge = false;
    private int minAgeVal = 19;
    private int maxAgeVal = 99;

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.HireBulkPersonnelDialog", new EncodeControl()); //$NON-NLS-1$

    public HireBulkPersonnelDialog(Frame parent, boolean modal, Campaign c) {
        super(parent, modal);
        this.campaign = c;
        initComponents();
        setLocationRelativeTo(getParent());
        setUserPreferences();
    }

    private static GridBagConstraints newConstraints(int xPos, int yPos) {
        return newConstraints(xPos, yPos, GridBagConstraints.NONE);
    }

    private static GridBagConstraints newConstraints(int xPos, int yPos, int fill) {
        GridBagConstraints result = new GridBagConstraints();
        result.gridx = xPos;
        result.gridy = yPos;
        result.fill = fill;
        result.anchor = GridBagConstraints.WEST;
        result.insets = DEFAULT_INSETS;
        return result;
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        choiceType = new JComboBox<>();
        choiceRanks = new JComboBox<>();

        btnHire = new JButton(resourceMap.getString("btnHire.text")); //$NON-NLS-1$
        btnClose = new JButton(resourceMap.getString("btnClose.text")); //$NON-NLS-1$
        panButtons = new JPanel(new GridBagLayout());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        setTitle(resourceMap.getString("Form.title")); //$NON-NLS-1$
        getContentPane().setLayout(new GridBagLayout());

        getContentPane().add(new JLabel(resourceMap.getString("lblType.text")), newConstraints(0, 0)); //$NON-NLS-1$

        DefaultComboBoxModel<PersonTypeItem> personTypeModel = new DefaultComboBoxModel<PersonTypeItem>();
        for(int i = 1; i < Person.T_NUM; i++) {
            personTypeModel.addElement(new PersonTypeItem(Person.getRoleDesc(i,campaign.getFaction().isClan()), i));
        }
        // Add "none" for generic AsTechs
        personTypeModel.addElement(new PersonTypeItem(Person.getRoleDesc(0, campaign.getFaction().isClan()), 0));
        choiceType.setModel(personTypeModel);
        choiceType.setName("choiceType"); // NOI18N
        gridBagConstraints = newConstraints(1, 0, GridBagConstraints.HORIZONTAL);
        gridBagConstraints.weightx = 1.0;
        choiceType.setSelectedIndex(0);
        choiceType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // If we change the type, we need to setup the ranks for that type
                refreshRanksCombo();
            }
        });
        getContentPane().add(choiceType, gridBagConstraints);

        getContentPane().add(new JLabel(resourceMap.getString("lblRank.text")), newConstraints(0, 1)); //$NON-NLS-1$

        rankModel = new DefaultComboBoxModel<>();
        choiceRanks.setModel(rankModel);
        choiceRanks.setName("choiceRanks");
        refreshRanksCombo();

        gridBagConstraints = newConstraints(1, 1, GridBagConstraints.HORIZONTAL);
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(choiceRanks, gridBagConstraints);

        int sn_min = 1;
        SpinnerNumberModel sn = new SpinnerNumberModel(1, sn_min, CampaignGUI.MAX_QUANTITY_SPINNER, 1);
        spnNumber = new JSpinner(sn);
        spnNumber.setEditor(new JSpinner.NumberEditor(spnNumber,"#")); //prevent digit grouping, e.g. 1,000
        jtf = ((JSpinner.DefaultEditor) spnNumber.getEditor()).getTextField();
        jtf.addKeyListener(new KeyListener() {
        	@Override
        	public void keyReleased(KeyEvent e) {
                try {
                    Integer newValue = Integer.valueOf(jtf.getText());
                    if (newValue > CampaignGUI.MAX_QUANTITY_SPINNER) {
                    	spnNumber.setValue(CampaignGUI.MAX_QUANTITY_SPINNER);
                    	jtf.setText(String.valueOf(CampaignGUI.MAX_QUANTITY_SPINNER));
                    } else if (newValue < sn_min) {
                    	spnNumber.setValue(sn_min);
                    	jtf.setText(String.valueOf(sn_min));
                    } else {
                    	spnNumber.setValue(newValue);
                    	jtf.setText(String.valueOf(newValue));
                    }
                } catch(NumberFormatException ex) {
                    //Not a number in text field
                	spnNumber.setValue(sn_min);
                	jtf.setText(String.valueOf(sn_min));
                }
        	}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
        });

        getContentPane().add(new JLabel(resourceMap.getString("lblNumber.text")), newConstraints(0, 2)); //$NON-NLS-1$

        gridBagConstraints = newConstraints(1, 2);
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(spnNumber, gridBagConstraints);

        int mainGridPos = 3;

        if(campaign.isGM()) {
            // GM tools
            JSeparator sep = new JSeparator();

            gridBagConstraints = newConstraints(0, mainGridPos, GridBagConstraints.HORIZONTAL);
            gridBagConstraints.gridwidth = 2;
            getContentPane().add(sep, gridBagConstraints);
            ++ mainGridPos;

            gridBagConstraints = newConstraints(0, mainGridPos);
            gridBagConstraints.weightx = 1.0;

            JCheckBox ageRangeCheck = new JCheckBox(resourceMap.getString("lblAgeRange.text")); //$NON-NLS-1$
            ageRangeCheck.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    useAge = ((JCheckBox)e.getSource()).isSelected();
                    minAge.setEnabled(useAge);
                    maxAge.setEnabled(useAge);
                }
            });
            getContentPane().add(ageRangeCheck, gridBagConstraints);

            gridBagConstraints = newConstraints(1, mainGridPos);
            gridBagConstraints.weightx = 1.0;

            JPanel ageRangePanel = new JPanel(new GridBagLayout());
            getContentPane().add(ageRangePanel, gridBagConstraints);

            minAge = new JSpinner(new SpinnerNumberModel(19, 0, 99, 1));
            ((JSpinner.DefaultEditor)minAge.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
            ((JSpinner.DefaultEditor)minAge.getEditor()).getTextField().setColumns(3);
            minAge.setEnabled(false);
            minAge.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    minAgeVal = (Integer)minAge.getModel().getValue();
                    if(minAgeVal > maxAgeVal) {
                        maxAge.setValue(minAgeVal);
                    }
                }
            });
            ageRangePanel.add(minAge, newConstraints(0, 0));

            ageRangePanel.add(new JLabel(resourceMap.getString("lblAgeRangeSeparator.text")), newConstraints(1, 0)); //$NON-NLS-1$

            maxAge = new JSpinner(new SpinnerNumberModel(99, 0, 99, 1));
            ((JSpinner.DefaultEditor)maxAge.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
            ((JSpinner.DefaultEditor)maxAge.getEditor()).getTextField().setColumns(3);
            maxAge.setEnabled(false);
            maxAge.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    maxAgeVal = (Integer)maxAge.getModel().getValue();
                    if(maxAgeVal < minAgeVal) {
                        minAge.setValue(maxAgeVal);
                    }
                }
            });
            //maxAge.setAlignmentY(CENTER_ALIGNMENT);
            ageRangePanel.add(maxAge, newConstraints(2, 0));

            ++ mainGridPos;
        }

        btnHire.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                hire();
            }
        });
        gridBagConstraints = newConstraints(0, 0);
        gridBagConstraints.insets = ZERO_INSETS;

        panButtons.add(btnHire, gridBagConstraints);
        gridBagConstraints.gridx++;

        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
        });
        panButtons.add(btnClose, gridBagConstraints);

        gridBagConstraints = newConstraints(0, mainGridPos, GridBagConstraints.HORIZONTAL);
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(panButtons, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(HireBulkPersonnelDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void hire() {
        int number = (Integer) spnNumber.getModel().getValue();
        PersonTypeItem selectedItem = (PersonTypeItem) choiceType.getSelectedItem();
        if (selectedItem == null) {
            MekHQ.getLogger().error("Attempted to bulk hire for null PersonnelType!");
            return;
        }


        LocalDate today = campaign.getLocalDate();
        LocalDate earliestBirthDate = today.minus(maxAgeVal + 1, ChronoUnit.YEARS)
                .plus(1, ChronoUnit.DAYS);
        final int days = Math.toIntExact(ChronoUnit.DAYS.between(earliestBirthDate,
                today.minus(minAgeVal, ChronoUnit.YEARS)));

        while (number > 0) {
            Person p = campaign.newPerson(((PersonTypeItem) choiceType.getSelectedItem()).id);
            p.setRank(((RankDisplay) Objects.requireNonNull(choiceRanks.getSelectedItem())).getRankNumeric());
            int age = p.getAge(today);
            if (useAge) {
                if ((age > maxAgeVal) || (age < minAgeVal)) {
                    LocalDate birthDay = earliestBirthDate.plus(Compute.randomInt(days), ChronoUnit.DAYS);
                    p.setBirthday(birthDay);
                    age = p.getAge(today);
                }
            }

            // Limit skills by age for children and adolescents
            if (age < 12) {
                p.removeAllSkills();
            } else if (age < 14) {
                p.limitSkills(0);
            } else if (age < 18) {
                p.limitSkills(age - 13);
            }

            if (!campaign.recruitPerson(p)) {
                number = 0;
            } else {
                number--;
            }
        }
    }

    private void refreshRanksCombo() {
        // Clear everything and start over! Wee!
        rankModel.removeAllElements();

        // Determine correct profession to pass into the loop
        int primaryRoleId = ((PersonTypeItem) Objects.requireNonNull(choiceType.getSelectedItem())).id;
        if (0 == primaryRoleId) {
            rankModel.addElement(new RankDisplay(0, campaign.getRankSystem().getRank(0).getName(Profession.MECHWARRIOR)));
        } else {
            rankModel.addAll(RankDisplay.getRankDisplaysForSystem(campaign.getRankSystem(),
                    Profession.getProfessionFromPersonnelRole(primaryRoleId)));
        }

        choiceRanks.setModel(rankModel);
        choiceRanks.setSelectedIndex(0);
    }

    private static class PersonTypeItem {
        public String name;
        public int id;

        public PersonTypeItem(String name, int id) {
            this.name = Objects.requireNonNull(name);
            this.id = id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

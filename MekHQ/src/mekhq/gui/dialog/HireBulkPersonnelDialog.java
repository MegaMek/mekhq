/*
 * Copyright (c) 2010-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.Compute;
import megamek.common.enums.SkillLevel;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.DefaultSpecialAbilityGenerator;
import mekhq.gui.CampaignGUI;
import mekhq.gui.displayWrappers.RankDisplay;

import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.ResourceBundle;

import static mekhq.campaign.personnel.SkillType.*;
import static mekhq.campaign.personnel.generator.AbstractSkillGenerator.addSkill;

/**
 * @author Jay Lawson
 */
public class HireBulkPersonnelDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(HireBulkPersonnelDialog.class);

    private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);
    private static final Insets DEFAULT_INSETS = new Insets(5, 5, 5, 5);

    private final Campaign campaign;

    private JComboBox<PersonTypeItem> choiceType;
    private JComboBox<RankDisplay> choiceRanks;
    private DefaultComboBoxModel<RankDisplay> rankModel;
    private JSpinner spnNumber;
    private JTextField jtf;

    private JSpinner minAge;
    private JSpinner maxAge;

    private MMComboBox<SkillLevel> skillLevel;

    private boolean useAge = false;
    private boolean useSkill = false;
    private int minAgeVal = 18;
    private int maxAgeVal = 99;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
            "mekhq.resources.HireBulkPersonnelDialog",
            MekHQ.getMHQOptions().getLocale());

    public HireBulkPersonnelDialog(final JFrame frame, final boolean modal, final Campaign campaign) {
        super(frame, modal);
        this.campaign = campaign;
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

        choiceType = new JComboBox<>();
        choiceRanks = new JComboBox<>();

        JButton btnHire = new JButton(resourceMap.getString("btnHire.text"));
        JButton btnGmHire = new JButton(resourceMap.getString("btnGmHire.text"));
        JButton btnClose = new JButton(resourceMap.getString("btnClose.text"));
        JPanel panButtons = new JPanel(new GridBagLayout());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");

        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new GridBagLayout());

        getContentPane().add(new JLabel(resourceMap.getString("lblType.text")), newConstraints(0, 0));

        DefaultComboBoxModel<PersonTypeItem> personTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelRole personnelRole : PersonnelRole.getPrimaryRoles()) {
            personTypeModel.addElement(
                    new PersonTypeItem(personnelRole.getName(campaign.getFaction().isClan()), personnelRole));
        }
        choiceType.setModel(personTypeModel);
        choiceType.setName("choiceType");
        GridBagConstraints gridBagConstraints = newConstraints(1, 0, GridBagConstraints.HORIZONTAL);
        gridBagConstraints.weightx = 1.0;
        choiceType.setSelectedIndex(0);
        choiceType.addActionListener(evt -> {
            // If we change the type, we need to set up the ranks for that type
            refreshRanksCombo();
        });
        getContentPane().add(choiceType, gridBagConstraints);

        getContentPane().add(new JLabel(resourceMap.getString("lblRank.text")), newConstraints(0, 1));

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
        spnNumber.setEditor(new NumberEditor(spnNumber, "#")); // prevent digit grouping, e.g. 1,000
        jtf = ((DefaultEditor) spnNumber.getEditor()).getTextField();
        jtf.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    int newValue = Integer.parseInt(jtf.getText());
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
                } catch (NumberFormatException ex) {
                    // Not a number in text field
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

        getContentPane().add(new JLabel(resourceMap.getString("lblNumber.text")), newConstraints(0, 2));

        gridBagConstraints = newConstraints(1, 2);
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(spnNumber, gridBagConstraints);

        int mainGridPos = 3;

        // GM tools
        if (campaign.isGM()) {
            // Age
            JSeparator sep = new JSeparator();

            gridBagConstraints = newConstraints(0, mainGridPos, GridBagConstraints.HORIZONTAL);
            gridBagConstraints.gridwidth = 2;
            getContentPane().add(sep, gridBagConstraints);
            ++mainGridPos;

            gridBagConstraints = newConstraints(0, mainGridPos);
            gridBagConstraints.weightx = 1.0;

            JCheckBox ageRangeCheck = new JCheckBox(resourceMap.getString("lblAgeRange.text"));
            ageRangeCheck.addActionListener(e -> {
                useAge = ((JCheckBox) e.getSource()).isSelected();
                minAge.setEnabled(useAge);
                maxAge.setEnabled(useAge);
            });
            getContentPane().add(ageRangeCheck, gridBagConstraints);

            gridBagConstraints = newConstraints(1, mainGridPos);
            gridBagConstraints.weightx = 1.0;

            JPanel ageRangePanel = new JPanel(new GridBagLayout());
            getContentPane().add(ageRangePanel, gridBagConstraints);

            minAge = new JSpinner(new SpinnerNumberModel(19, 0, 99, 1));
            ((DefaultEditor) minAge.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
            ((DefaultEditor) minAge.getEditor()).getTextField().setColumns(3);
            minAge.setEnabled(false);
            minAge.addChangeListener(e -> {
                minAgeVal = (Integer) minAge.getModel().getValue();
                if (minAgeVal > maxAgeVal) {
                    maxAge.setValue(minAgeVal);
                }
            });
            ageRangePanel.add(minAge, newConstraints(0, 0));

            ageRangePanel.add(new JLabel(resourceMap.getString("lblAgeRangeSeparator.text")), newConstraints(1, 0));

            maxAge = new JSpinner(new SpinnerNumberModel(99, 0, 99, 1));
            ((DefaultEditor) maxAge.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
            ((DefaultEditor) maxAge.getEditor()).getTextField().setColumns(3);
            maxAge.setEnabled(false);
            maxAge.addChangeListener(e -> {
                maxAgeVal = (Integer) maxAge.getModel().getValue();
                if (maxAgeVal < minAgeVal) {
                    minAge.setValue(maxAgeVal);
                }
            });
            ageRangePanel.add(maxAge, newConstraints(2, 0));

            ++mainGridPos;

            // Skill level
            gridBagConstraints = newConstraints(0, mainGridPos, GridBagConstraints.HORIZONTAL);
            gridBagConstraints.gridwidth = 2;
            ++mainGridPos;

            gridBagConstraints = newConstraints(0, mainGridPos);
            gridBagConstraints.weightx = 1.0;

            JCheckBox skillRangeCheck = new JCheckBox(resourceMap.getString("lblSkillLevel.text"));
            skillRangeCheck.addActionListener(e -> {
                useSkill = ((JCheckBox) e.getSource()).isSelected();
                skillLevel.setEnabled(useSkill);
            });
            getContentPane().add(skillRangeCheck, gridBagConstraints);

            gridBagConstraints = newConstraints(1, mainGridPos);
            gridBagConstraints.weightx = 1.0;

            JPanel skillRangePanel = new JPanel(new GridBagLayout());
            getContentPane().add(skillRangePanel, gridBagConstraints);

            skillLevel = new MMComboBox<>("comboSkillLevel", SkillLevel.values());
            skillLevel.setSelectedItem(SkillLevel.REGULAR);
            skillLevel.setEnabled(false);

            skillLevel.removeItem(SkillLevel.NONE);
            skillLevel.removeItem(SkillLevel.HEROIC);
            skillLevel.removeItem(SkillLevel.LEGENDARY);

            JLabel labelMinSkill = new JLabel("Minimum Skill:");
            labelMinSkill.setLabelFor(skillLevel);
            labelMinSkill.setEnabled(false);

            skillRangePanel.add(skillLevel, newConstraints(0, 0));

            ++mainGridPos;
        }

        btnHire.addActionListener(evt -> hire(false));
        gridBagConstraints = newConstraints(0, 0);
        gridBagConstraints.insets = ZERO_INSETS;

        panButtons.add(btnHire, gridBagConstraints);
        gridBagConstraints.gridx++;

        if (campaign.isGM()) {
            btnGmHire.addActionListener(evt -> hire(true));
            gridBagConstraints.insets = ZERO_INSETS;

            panButtons.add(btnGmHire, gridBagConstraints);
            gridBagConstraints.gridx++;
        }

        btnClose.addActionListener(evt -> setVisible(false));
        panButtons.add(btnClose, gridBagConstraints);

        gridBagConstraints = newConstraints(0, mainGridPos, GridBagConstraints.HORIZONTAL);
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(panButtons, gridBagConstraints);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(HireBulkPersonnelDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void hire(boolean isGmHire) {
        int number = (Integer) spnNumber.getModel().getValue();
        PersonTypeItem selectedItem = (PersonTypeItem) choiceType.getSelectedItem();
        if (selectedItem == null) {
            logger.error("Attempted to bulk hire for null PersonnelType!");
            return;
        }

        LocalDate today = campaign.getLocalDate();
        LocalDate earliestBirthDate = today.minusYears(maxAgeVal + 1).plusDays(1);
        final int days = Math.toIntExact(ChronoUnit.DAYS.between(earliestBirthDate, today.minusYears(minAgeVal)));

        while (number > 0) {
            Person person = campaign.newPerson(selectedItem.getRole());

            if ((useSkill) && (!selectedItem.getRole().isCivilian()) && (!selectedItem.getRole().isAssistant())) {
                overrideSkills(campaign, person, selectedItem.getRole(),
                    Objects.requireNonNull(skillLevel.getSelectedItem()).ordinal());
            }

            person.setRank(((RankDisplay) Objects.requireNonNull(choiceRanks.getSelectedItem())).getRankNumeric());

            int age = person.getAge(today);
            if (useAge) {
                if ((age > maxAgeVal) || (age < minAgeVal)) {
                    LocalDate birthDay = earliestBirthDate.plusDays(Compute.randomInt(days));
                    person.setDateOfBirth(birthDay);
                    age = person.getAge(today);
                }

                // Limit skills by age for children and adolescents
                if (age < 16) {
                    person.removeAllSkills();
                } else if (age < 18) {
                    person.limitSkills(0);
                }
            }

            int experienceLevel = person.getExperienceLevel(campaign, false);

            reRollLoyalty(campaign, person, experienceLevel);
            reRollAdvantages(campaign, person, experienceLevel);

            if (!campaign.recruitPerson(person, isGmHire)) {
                number = 0;
            } else {
                number--;
            }
        }
    }

    /**
     * Re-rolls the SPAs of a person based on their experience level.
     *
     * @param campaign       The current campaign.
     * @param person         The person whose advantages are being re-rolled.
     * @param experienceLevel The experience level of the person.
     */
    public static void reRollAdvantages(Campaign campaign, Person person, int experienceLevel) {
        Enumeration<IOption> options = new PersonnelOptions().getOptions(PersonnelOptions.LVL3_ADVANTAGES);

        for (IOption option : Collections.list(options)) {
            person.getOptions().getOption(option.getName()).clearValue();
        }

        if (experienceLevel > 0) {
            AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
            specialAbilityGenerator.setSkillPreferences(new RandomSkillPreferences());
            specialAbilityGenerator.generateSpecialAbilities(campaign, person, experienceLevel);
        }
    }

    /**
     * Re-rolls the loyalty of a person based on their experience level.
     *
     * @param campaign       The current campaign.
     * @param person         The person whose loyalty is being re-rolled.
     * @param experienceLevel The experience level of the person.
     */
    public static void reRollLoyalty(Campaign campaign, Person person, int experienceLevel) {
        if (experienceLevel <= 0) {
            person.setLoyalty(Compute.d6(3) + 2);
        } else if (experienceLevel == 1) {
            person.setLoyalty(Compute.d6(3) + 1);
        } else {
            person.setLoyalty(Compute.d6(3));
        }
    }

    /**
     * Replaces the skills for a {@link Person} based on their primary role and desired experience level.
     *
     * @param campaign    The current campaign
     * @param person      The {@link Person} to add default skills.
     * @param primaryRole The primary role of the person
     * @param expLvl      The experience level of the person (e.g.
     *                    {@link SkillType#EXP_GREEN}).
     */
    public static void overrideSkills(Campaign campaign, Person person, PersonnelRole primaryRole,
                                      int expLvl) {
        switch (primaryRole) {
            case MEKWARRIOR:
                addSkillFixedExperienceLevel(person, S_PILOT_MEK, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_MEK, expLvl);
                break;
            case LAM_PILOT:
                addSkillFixedExperienceLevel(person, S_PILOT_MEK, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_MEK, expLvl);
                addSkillFixedExperienceLevel(person, S_PILOT_AERO, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_AERO, expLvl);
                break;
            case GROUND_VEHICLE_DRIVER:
                addSkillFixedExperienceLevel(person, S_PILOT_GVEE, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_VEE, expLvl);
                break;
            case NAVAL_VEHICLE_DRIVER:
                addSkillFixedExperienceLevel(person, S_PILOT_NVEE, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_VEE, expLvl);
                break;
            case VTOL_PILOT:
                addSkillFixedExperienceLevel(person, S_PILOT_VTOL, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_VEE, expLvl);
                break;
            case VEHICLE_GUNNER:
                addSkillFixedExperienceLevel(person, S_GUN_VEE, expLvl);
                break;
            case VEHICLE_CREW, MECHANIC:
                addSkillFixedExperienceLevel(person, S_TECH_MECHANIC, expLvl);
                break;
            case AEROSPACE_PILOT:
                addSkillFixedExperienceLevel(person, S_PILOT_AERO, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_AERO, expLvl);
                break;
            case CONVENTIONAL_AIRCRAFT_PILOT:
                addSkillFixedExperienceLevel(person, S_PILOT_JET, expLvl);
                addSkillFixedExperienceLevel(person, S_GUN_JET, expLvl);
                break;
            case PROTOMEK_PILOT:
                addSkillFixedExperienceLevel(person, S_GUN_PROTO, expLvl);
                break;
            case BATTLE_ARMOUR:
                addSkillFixedExperienceLevel(person, S_GUN_BA, expLvl);
                addSkillFixedExperienceLevel(person, S_ANTI_MEK, expLvl);
                addSkillFixedExperienceLevel(person, S_SMALL_ARMS, expLvl);
                break;
            case SOLDIER:
                addSkillFixedExperienceLevel(person, S_SMALL_ARMS, expLvl);
                if (Utilities.rollProbability(new RandomSkillPreferences().getAntiMekProb())) {
                    addSkillFixedExperienceLevel(person, S_ANTI_MEK, expLvl);
                }
                break;
            case VESSEL_PILOT:
                addSkillFixedExperienceLevel(person, S_PILOT_SPACE, expLvl);
                break;
            case VESSEL_GUNNER:
                addSkillFixedExperienceLevel(person, S_GUN_SPACE, expLvl);
                break;
            case VESSEL_CREW:
                addSkillFixedExperienceLevel(person, S_TECH_VESSEL, expLvl);
                break;
            case VESSEL_NAVIGATOR:
                addSkillFixedExperienceLevel(person, S_NAV, expLvl);
                break;
            case MEK_TECH:
                addSkillFixedExperienceLevel(person, S_TECH_MEK, expLvl);
                break;
            case AERO_TEK:
                addSkillFixedExperienceLevel(person, S_TECH_AERO, expLvl);
                break;
            case BA_TECH:
                addSkillFixedExperienceLevel(person, S_TECH_BA, expLvl);
                break;
            case DOCTOR:
                addSkillFixedExperienceLevel(person, S_DOCTOR, expLvl);
                break;
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                addSkillFixedExperienceLevel(person, S_ADMIN, expLvl);

                if (campaign.getCampaignOptions().isAdminsHaveNegotiation()) {
                    addSkillFixedExperienceLevel(person, S_NEG, expLvl);
                }

                if (campaign.getCampaignOptions().isAdminsHaveScrounge()) {
                    addSkillFixedExperienceLevel(person, S_SCROUNGE, expLvl);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Adds a skill to a person with a fixed experience level.
     * If the person already has the specified skill, the bonus value will be
     * retained.
     *
     * @param person        The Person to add the skill to.
     * @param skill         The name of the skill to add.
     * @param experienceLvl The experience level for the skill.
     */
    private static void addSkillFixedExperienceLevel(Person person, String skill, int experienceLvl) {
        int bonus = 0;

        if (person.hasSkill(skill)) {
            bonus = person.getSkill(skill).getBonus();
        }

        addSkill(person, skill, experienceLvl, bonus);
    }

    private void refreshRanksCombo() {
        // Clear everything and start over! Wee!
        rankModel.removeAllElements();

        // Determine correct profession to pass into the loop
        final PersonnelRole role = ((PersonTypeItem) Objects.requireNonNull(choiceType.getSelectedItem())).getRole();
        rankModel.addAll(RankDisplay.getRankDisplaysForSystem(campaign.getRankSystem(),
                Profession.getProfessionFromPersonnelRole(role)));

        choiceRanks.setModel(rankModel);
        choiceRanks.setSelectedIndex(0);
    }

    private static class PersonTypeItem {
        private String name;
        private PersonnelRole role;

        public PersonTypeItem(String name, PersonnelRole role) {
            this.setName(Objects.requireNonNull(name));
            this.setRole(role);
        }

        public void setName(String name) {
            this.name = name;
        }

        public PersonnelRole getRole() {
            return role;
        }

        public void setRole(PersonnelRole role) {
            this.role = role;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

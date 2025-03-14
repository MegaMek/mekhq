/*
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
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
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.Skill;
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
import java.util.*;
import java.util.List;

import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.Compute.d6;
import static mekhq.campaign.personnel.SkillType.*;
import static mekhq.campaign.personnel.enums.PersonnelRole.*;
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
                if (skillLevel.getSelectedItem() != null) {
                    RandomSkillPreferences randomSkillPreferences = campaign.getRandomSkillPreferences();
                    boolean useExtraRandomness = randomSkillPreferences.randomizeSkill();

                    CampaignOptions campaignOptions = campaign.getCampaignOptions();
                    overrideSkills(campaignOptions.isAdminsHaveNegotiation(), campaignOptions.isAdminsHaveScrounge(),
                          useExtraRandomness, person, selectedItem.getRole(), skillLevel.getSelectedItem().ordinal());
                }
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

            reRollLoyalty(person, experienceLevel);
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
     * @param person         The person whose loyalty is being re-rolled.
     * @param experienceLevel The experience level of the person.
     */
    public static void reRollLoyalty(Person person, int experienceLevel) {
        if (experienceLevel <= 0) {
            person.setLoyalty(d6(3) + 2);
        } else if (experienceLevel == 1) {
            person.setLoyalty(d6(3) + 1);
        } else {
            person.setLoyalty(d6(3));
        }
    }

    /**
     * Overrides the skills of a person based on their role, experience level, and additional conditions.
     *
     * @param isAdminsHaveNegotiation whether administrators should have negotiation skills.
     * @param isAdminsHaveScrounge    whether administrators should have scrounge skills.
     * @param isUseExtraRandom        whether extra randomization should be applied to skill levels.
     * @param person                  the {@link Person} whose skills are being overridden.
     * @param primaryRole             the {@link PersonnelRole} of the person.
     * @param expLvl                  the experience level to which the skills should be set.
     */
    public static void overrideSkills(boolean isAdminsHaveNegotiation, boolean isAdminsHaveScrounge,
                                      boolean isUseExtraRandom, Person person, PersonnelRole primaryRole,
                                      int expLvl) {
        // Role-to-Skill Mapping
        Map<PersonnelRole, List<String>> roleSkills = Map.ofEntries(
              Map.entry(MEKWARRIOR, List.of(S_PILOT_MEK, S_GUN_MEK)),
              Map.entry(LAM_PILOT, List.of(S_PILOT_MEK, S_GUN_MEK, S_PILOT_AERO, S_GUN_AERO)),
              Map.entry(GROUND_VEHICLE_DRIVER, List.of(S_PILOT_GVEE, S_GUN_VEE)),
              Map.entry(NAVAL_VEHICLE_DRIVER, List.of(S_PILOT_NVEE, S_GUN_VEE)),
              Map.entry(VTOL_PILOT, List.of(S_PILOT_VTOL, S_GUN_VEE)),
              Map.entry(VEHICLE_GUNNER, List.of(S_GUN_VEE)),
              Map.entry(VEHICLE_CREW, List.of(S_TECH_MECHANIC)),
              Map.entry(MECHANIC, List.of(S_TECH_MECHANIC)),
              Map.entry(AEROSPACE_PILOT, List.of(S_PILOT_AERO, S_GUN_AERO)),
              Map.entry(CONVENTIONAL_AIRCRAFT_PILOT, List.of(S_PILOT_JET, S_GUN_JET)),
              Map.entry(PROTOMEK_PILOT, List.of(S_GUN_PROTO)),
              Map.entry(BATTLE_ARMOUR, List.of(S_GUN_BA, S_ANTI_MEK)),
              Map.entry(SOLDIER, List.of(S_SMALL_ARMS)),
              Map.entry(VESSEL_PILOT, List.of(S_PILOT_SPACE)),
              Map.entry(VESSEL_GUNNER, List.of(S_GUN_SPACE)),
              Map.entry(VESSEL_CREW, List.of(S_TECH_VESSEL)),
              Map.entry(VESSEL_NAVIGATOR, List.of(S_NAV)),
              Map.entry(MEK_TECH, List.of(S_TECH_MEK)),
              Map.entry(AERO_TEK, List.of(S_TECH_AERO)),
              Map.entry(BA_TECH, List.of(S_TECH_BA)),
              Map.entry(DOCTOR, List.of(S_DOCTOR))
        );

        // Add admin-specific logic
        if (primaryRole == ADMINISTRATOR_COMMAND || primaryRole == ADMINISTRATOR_LOGISTICS ||
              primaryRole == ADMINISTRATOR_TRANSPORT || primaryRole == ADMINISTRATOR_HR) {
            List<String> adminSkills = new ArrayList<>();

            adminSkills.add(S_ADMIN);
            if (isAdminsHaveNegotiation) {
                adminSkills.add(S_NEG);
            }
            if (isAdminsHaveScrounge) {
                adminSkills.add(S_SCROUNGE);
            }

            addSkillsAndRandomize(person, adminSkills, expLvl, isUseExtraRandom);
            return;
        }

        // Handle Normal Role Skills
        List<String> skills = roleSkills.getOrDefault(primaryRole, List.of());
        addSkillsAndRandomize(person, skills, expLvl, isUseExtraRandom);
    }

    /**
     * Adds the specified skills to a person and optionally applies randomization to those skills.
     *
     * @param person    the {@link Person} to whom the skills are to be added.
     * @param skills    a list of skill names to add.
     * @param expLvl    the experience level to which the skills are to be set.
     * @param randomize {@code true} if the skill levels should be randomized after being added;
     *                  {@code false} otherwise.
     */
    private static void addSkillsAndRandomize(Person person, List<String> skills, int expLvl, boolean randomize) {
        for (String skill : skills) {
            addSkillFixedExperienceLevel(person, skill, expLvl);
        }

        if (randomize) {
            randomizeSkills(person, skills);
        }
    }

    /**
     * Randomizes the skill levels for the given person within a specific range.
     * Skill levels may increase, decrease, or stay the same based on a dice roll.
     *
     * @param person the {@link Person} whose skills are being randomized.
     * @param skills a list of skill names that should be randomized.
     */
    private static void randomizeSkills(Person person, List<String> skills) {
        for (String skillName : skills) {
            Skill skill = person.getSkill(skillName);

            if (skill == null) {
                continue;
            }

            int roll = d6(); // Roll once for the skill
            int adjustedLevel = skill.getLevel() + (roll == 6 ? 1 : roll == 1 ? -1 : 0);
            skill.setLevel(clamp(adjustedLevel, 0, 10));
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

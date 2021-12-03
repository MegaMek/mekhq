/*
 * Copyright (c) 2016-2020 - The MegaMek Team. All Rights Reserved
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

import megamek.client.ui.preferences.*;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.PersonRankStringSorter;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.*;

public final class BatchXPDialog extends JDialog {
    private static final long serialVersionUID = -7897406116865495209L;

    private final Campaign campaign;
    private final PersonnelTableModel personnelModel;
    private final TableRowSorter<PersonnelTableModel> personnelSorter;
    private final PersonnelFilter personnelFilter;
    private boolean dataChanged = false;

    private JTable personnelTable;
    private JComboBox<PersonTypeItem> choiceType;
    private JComboBox<PersonTypeItem> choiceExp;
    private JComboBox<PersonTypeItem> choiceRank;
    private JCheckBox onlyOfficers;
    private JCheckBox noOfficers;
    private JComboBox<String> choiceSkill;
    private JSpinner skillLevel;
    private JCheckBox allowPrisoners;
    private JButton buttonSpendXP;

    // This is used for personnel outside of Clan factions
    private final List<Integer> personnelColumns = Arrays.asList(
            PersonnelTableModel.COL_RANK,
            PersonnelTableModel.COL_GIVEN_NAME,
            PersonnelTableModel.COL_SURNAME,
            PersonnelTableModel.COL_BLOODNAME,
            PersonnelTableModel.COL_AGE,
            PersonnelTableModel.COL_TYPE,
            PersonnelTableModel.COL_XP
    );

    // The Clans do not use Surnames, but they do have Bloodnames that should be listed
    private final List<Integer> personnelClanColumns = Arrays.asList(
            PersonnelTableModel.COL_RANK,
            PersonnelTableModel.COL_GIVEN_NAME,
            PersonnelTableModel.COL_BLOODNAME,
            PersonnelTableModel.COL_AGE,
            PersonnelTableModel.COL_TYPE,
            PersonnelTableModel.COL_XP
    );

    private JLabel matchedPersonnelLabel;

    private transient ResourceBundle resourceMap;
    private transient String choiceNoSkill;

    public BatchXPDialog(JFrame parent, Campaign campaign) {
        super(parent, "", true);

        this.resourceMap = ResourceBundle.getBundle("mekhq.resources.BatchXPDialog", new EncodeControl());

        setTitle(resourceMap.getString("MassTrainingDialog.title"));
        choiceNoSkill = resourceMap.getString("skill.choice.text");

        this.campaign = Objects.requireNonNull(campaign);
        this.personnelModel = new PersonnelTableModel(campaign);
        personnelModel.refreshData();
        personnelSorter = new TableRowSorter<>(personnelModel);
        personnelSorter.setSortsOnUpdates(true);
        personnelSorter.setComparator(PersonnelTableModel.COL_RANK, new PersonRankStringSorter(campaign));
        personnelSorter.setComparator(PersonnelTableModel.COL_AGE, new FormattedNumberSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_XP, new FormattedNumberSorter());
        personnelSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(1, SortOrder.ASCENDING)));
        personnelFilter = new PersonnelFilter();
        personnelSorter.setRowFilter(personnelFilter);

        initComponents();

        setUserPreferences();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        add(getPersonnelTable(), BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.WEST);

        pack();
        setLocationRelativeTo(getParent());
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(BatchXPDialog.class);

        choiceType.setName("primaryRole");
        preferences.manage(new JComboBoxPreference(choiceType));

        choiceExp.setName("experienceLevel");
        preferences.manage(new JComboBoxPreference(choiceExp));

        choiceRank.setName("rank");
        preferences.manage(new JComboBoxPreference(choiceRank));

        onlyOfficers.setName("onlyOfficers");
        preferences.manage(new JToggleButtonPreference(onlyOfficers));

        noOfficers.setName("noOfficers");
        preferences.manage(new JToggleButtonPreference(noOfficers));

        choiceSkill.setName("skill");
        preferences.manage(new JComboBoxPreference(choiceSkill));

        skillLevel.setName("skillLevel");
        preferences.manage(new JIntNumberSpinnerPreference(skillLevel));

        allowPrisoners.setName("allowPrisoners");
        preferences.manage(new JToggleButtonPreference(allowPrisoners));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private JComponent getPersonnelTable() {
        personnelTable = new JTable(personnelModel);
        personnelTable.setCellSelectionEnabled(false);
        personnelTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        for (int i = PersonnelTableModel.N_COL - 1; i >= 0 ; -- i) {
            TableColumn column = personnelTable.getColumnModel().getColumn(i);
            if (campaign.getFaction().isClan()) {
                if (personnelClanColumns.contains(i)) {
                    column.setPreferredWidth(personnelModel.getColumnWidth(i));
                    column.setCellRenderer(new MekHqTableCellRenderer());
                } else {
                    personnelTable.removeColumn(column);
                }
            } else {
                if (personnelColumns.contains(i)) {
                    column.setPreferredWidth(personnelModel.getColumnWidth(i));
                    column.setCellRenderer(new MekHqTableCellRenderer());
                } else {
                    personnelTable.removeColumn(column);
                }
            }
        }
        personnelTable.setIntercellSpacing(new Dimension(1, 0));
        personnelTable.setShowGrid(false);
        personnelTable.setRowSorter(personnelSorter);

        JScrollPane pane = new JScrollPane(personnelTable);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        return pane;
    }

    private JComponent getButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        choiceType = new JComboBox<>();
        choiceType.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) choiceType.getPreferredSize().getHeight()));
        DefaultComboBoxModel<PersonTypeItem> personTypeModel = new DefaultComboBoxModel<>();
        personTypeModel.addElement(new PersonTypeItem(resourceMap.getString("primaryRole.choice.text"), null));
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        for (PersonnelRole personnelRole : personnelRoles) {
            personTypeModel.addElement(new PersonTypeItem(personnelRole.getName(campaign.getFaction().isClan()), personnelRole.ordinal()));
        }
        choiceType.setModel(personTypeModel);
        choiceType.setSelectedIndex(0);
        choiceType.addActionListener(e -> {
            PersonTypeItem personTypeItem = (PersonTypeItem) Objects.requireNonNull(choiceType.getSelectedItem());
            personnelFilter.setPrimaryRole((personTypeItem.getId() == null) ? null : personnelRoles[personTypeItem.getId()]);
            updatePersonnelTable();
        });
        panel.add(choiceType);

        choiceExp = new JComboBox<>();
        choiceExp.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) choiceType.getPreferredSize().getHeight()));
        DefaultComboBoxModel<PersonTypeItem> personExpModel = new DefaultComboBoxModel<>();
        personExpModel.addElement(new PersonTypeItem(resourceMap.getString("experience.choice.text"), null));
        for (int i = 0; i < 5; ++ i) {
            personExpModel.addElement(new PersonTypeItem(SkillType.getExperienceLevelName(i), i));
        }
        choiceExp.setModel(personExpModel);
        choiceExp.setSelectedIndex(0);
        choiceExp.addActionListener(e -> {
            personnelFilter.setExpLevel(((PersonTypeItem) Objects.requireNonNull(choiceExp.getSelectedItem())).getId());
            updatePersonnelTable();
        });
        panel.add(choiceExp);

        choiceRank = new JComboBox<>();
        choiceRank.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) choiceType.getPreferredSize().getHeight()));
        DefaultComboBoxModel<PersonTypeItem> personRankModel = new DefaultComboBoxModel<>();
        personRankModel.addElement(new PersonTypeItem(resourceMap.getString("rank.choice.text"), null));

        final List<Rank> ranks = campaign.getRankSystem().getRanks();
        for (int i = 0; i < ranks.size(); i++) {
            personRankModel.addElement(new PersonTypeItem(ranks.get(i).getRankNamesAsString(", "), i));
        }
        choiceRank.setModel(personRankModel);
        choiceRank.setSelectedIndex(0);
        choiceRank.addActionListener(e -> {
            personnelFilter.setRank(((PersonTypeItem) Objects.requireNonNull(choiceRank.getSelectedItem())).getId());
            updatePersonnelTable();
        });
        panel.add(choiceRank);

        onlyOfficers = new JCheckBox(resourceMap.getString("onlyOfficers.text"));
        onlyOfficers.setHorizontalAlignment(SwingConstants.LEFT);
        onlyOfficers.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) onlyOfficers.getPreferredSize().getHeight()));
        onlyOfficers.addChangeListener(e -> {
            personnelFilter.setOnlyOfficers(onlyOfficers.isSelected());
            updatePersonnelTable();

            noOfficers.setEnabled(!onlyOfficers.isSelected());
        });
        JPanel onlyOfficersPanel = new JPanel(new GridLayout(1, 1));
        onlyOfficersPanel.setAlignmentY(JComponent.LEFT_ALIGNMENT);
        onlyOfficersPanel.add(onlyOfficers);
        onlyOfficersPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) onlyOfficersPanel.getPreferredSize().getHeight()));
        panel.add(onlyOfficersPanel);

        noOfficers = new JCheckBox(resourceMap.getString("noOfficers.text"));
        noOfficers.setHorizontalAlignment(SwingConstants.LEFT);
        noOfficers.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) noOfficers.getPreferredSize().getHeight()));
        noOfficers.addChangeListener(e -> {
            personnelFilter.setNoOfficers(noOfficers.isSelected());
            updatePersonnelTable();

            onlyOfficers.setEnabled(!noOfficers.isSelected());
        });
        JPanel noOfficersPanel = new JPanel(new GridLayout(1, 1));
        noOfficersPanel.setAlignmentY(JComponent.LEFT_ALIGNMENT);
        noOfficersPanel.add(noOfficers);
        noOfficersPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) noOfficersPanel.getPreferredSize().getHeight()));
        panel.add(noOfficersPanel);

        choiceSkill = new JComboBox<>();
        choiceSkill.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) choiceSkill.getPreferredSize().getHeight()));
        DefaultComboBoxModel<String> personSkillModel = new DefaultComboBoxModel<>();
        personSkillModel.addElement(choiceNoSkill);
        for (String skill : SkillType.getSkillList()) {
            personSkillModel.addElement(skill);
        }
        choiceSkill.setModel(personSkillModel);
        choiceSkill.setSelectedIndex(0);
        choiceSkill.addActionListener(e -> {
            if (choiceNoSkill.equals(choiceSkill.getSelectedItem())) {
                personnelFilter.setSkill(null);
                ((SpinnerNumberModel) skillLevel.getModel()).setMaximum(10);
                buttonSpendXP.setEnabled(false);
            } else {
                String skillName = (String) choiceSkill.getSelectedItem();
                personnelFilter.setSkill(skillName);
                int maxSkillLevel = SkillType.getType(skillName).getMaxLevel();
                int currentLevel = (Integer) skillLevel.getModel().getValue();
                ((SpinnerNumberModel) skillLevel.getModel()).setMaximum(maxSkillLevel);
                if (currentLevel > maxSkillLevel) {
                    skillLevel.getModel().setValue(maxSkillLevel);
                }
                buttonSpendXP.setEnabled(true);
            }
            updatePersonnelTable();
        });
        panel.add(choiceSkill);

        panel.add(Box.createRigidArea(new Dimension(10, 10)));
        panel.add(new JLabel(resourceMap.getString("targetSkillLevel.text")));

        skillLevel = new JSpinner(new SpinnerNumberModel(10, 0, 10, 1));
        skillLevel.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) skillLevel.getPreferredSize().getHeight()));
        skillLevel.addChangeListener(e -> {
            personnelFilter.setMaxSkillLevel((Integer) skillLevel.getModel().getValue());
            updatePersonnelTable();
        });
        panel.add(skillLevel);

        allowPrisoners = new JCheckBox(resourceMap.getString("allowPrisoners.text"));
        allowPrisoners.setHorizontalAlignment(SwingConstants.LEFT);
        allowPrisoners.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) allowPrisoners.getPreferredSize().getHeight()));
        allowPrisoners.addChangeListener(e -> {
            personnelFilter.setAllowPrisoners(allowPrisoners.isSelected());
            updatePersonnelTable();
        });
        JPanel allowPrisonersPanel = new JPanel(new GridLayout(1, 1));
        allowPrisonersPanel.setAlignmentY(JComponent.LEFT_ALIGNMENT);
        allowPrisonersPanel.add(allowPrisoners);
        allowPrisonersPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) allowPrisonersPanel.getPreferredSize().getHeight()));
        panel.add(allowPrisonersPanel);

        panel.add(Box.createVerticalGlue());

        matchedPersonnelLabel = new JLabel("");
        matchedPersonnelLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) matchedPersonnelLabel.getPreferredSize().getHeight()));
        panel.add(matchedPersonnelLabel);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) buttons.getPreferredSize().getHeight()));

        buttonSpendXP = new JButton(resourceMap.getString("spendXP.text"));
        buttonSpendXP.setEnabled(false);
        buttonSpendXP.addActionListener(e -> spendXP());
        buttons.add(buttonSpendXP);

        JButton button = new JButton(resourceMap.getString("close.text"));
        button.addActionListener(e -> setVisible(false));
        buttons.add(button);

        panel.add(buttons);

        panel.setMaximumSize(new Dimension((int) panel.getPreferredSize().getWidth(), Short.MAX_VALUE));
        panel.setMinimumSize(new Dimension((int) panel.getPreferredSize().getWidth(), 300));

        return panel;
    }

    protected void updatePersonnelTable() {
        personnelSorter.sort();
        if (!choiceNoSkill.equals(choiceSkill.getSelectedItem())) {
            int rows = personnelTable.getRowCount();
            matchedPersonnelLabel.setText(String.format(resourceMap.getString("eligible.format"), rows));
        } else {
            matchedPersonnelLabel.setText("");
        }
    }

    protected void spendXP() {
        String skillName = (String) choiceSkill.getSelectedItem();
        if (choiceNoSkill.equals(skillName) || (skillName == null)) {
            // This shouldn't happen, but guard against it anyway.
            return;
        }
        int rows = personnelTable.getRowCount();
        int improvedPersonnelCount = rows;
        while (rows > 0) {
            for (int i = 0; i < rows; ++ i) {
                Person p = personnelModel.getPerson(personnelTable.convertRowIndexToModel(i));
                int cost;
                if (p.hasSkill(skillName)) {
                    cost = p.getCostToImprove(skillName);
                } else {
                    cost = SkillType.getType(skillName).getCost(0);
                }
                int startingExperienceLevel = p.getExperienceLevel(false);

                // Improve the skill and deduce the cost
                p.improveSkill(skillName);
                p.spendXP(cost);
                PersonalLogger.improvedSkill(campaign, p, campaign.getLocalDate(),
                        p.getSkill(skillName).getType().getName(), p.getSkill(skillName).toString());

                // The next part is bollocks and doesn't belong here, but as long as we hardcode AtB ...
                if (campaign.getCampaignOptions().getUseAtB()) {
                    if (p.getPrimaryRole().isCombat() && !p.getPrimaryRole().isVesselCrew()
                            && (p.getExperienceLevel(false) > startingExperienceLevel)
                            && (startingExperienceLevel >= SkillType.EXP_REGULAR)) {
                        final SingleSpecialAbilityGenerator spaGenerator = new SingleSpecialAbilityGenerator();
                        final String spa = spaGenerator.rollSPA(p);
                        if (spa == null) {
                            if (campaign.getCampaignOptions().useEdge()) {
                                p.changeEdge(1);
                                p.changeCurrentEdge(1);
                                PersonalLogger.gainedEdge(campaign, p, campaign.getLocalDate());
                            }
                        } else {
                            PersonalLogger.gainedSPA(campaign, p, campaign.getLocalDate(), spa);
                        }
                    }
                }
                campaign.personUpdated(p);
            }
            // Refresh the filter and continue if we still have anyone available
            updatePersonnelTable();
            rows = personnelTable.getRowCount();
            dataChanged = true;
        }
        if (improvedPersonnelCount > 0) {
            campaign.addReport(String.format(resourceMap.getString("improvedSkills.format"), skillName, improvedPersonnelCount));
        }

    }

    public boolean hasDataChanged() {
        return dataChanged;
    }

    public static class PersonnelFilter extends RowFilter<PersonnelTableModel, Integer> {
        private PersonnelRole primaryRole = null;
        private Integer expLevel = null;
        private Integer rank = null;
        private boolean onlyOfficers = false;
        private boolean noOfficers = false;
        private String skill = null;
        private int maxSkillLevel = 10;
        private boolean prisoners = false;

        @Override
        public boolean include(RowFilter.Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
            Person p = entry.getModel().getPerson(entry.getIdentifier().intValue());
            if (!p.getStatus().isActive()) {
                return false;
            } else if (!prisoners && !p.getPrisonerStatus().isFree()) {
                return false;
            } else if ((null != primaryRole) && (p.getPrimaryRole() != primaryRole)) {
                return false;
            } else if ((null != expLevel) && (p.getExperienceLevel(false) != expLevel)) {
                return false;
            } else if (onlyOfficers && !p.getRank().isOfficer()) {
                return false;
            } else if (noOfficers && p.getRank().isOfficer()) {
                return false;
            } else if ((rank != null) && (p.getRankNumeric() != rank)) {
                return false;
            } else if (null != skill) {
                Skill s = p.getSkill(skill);
                if (null == s) {
                    int cost = SkillType.getType(skill).getCost(0);
                    return (cost >= 0) && (cost <= p.getXP());
                } else {
                    int cost = s.getCostToImprove();
                    return (s.getLevel() < maxSkillLevel) && (cost >= 0) && (cost <= p.getXP());
                }
            }
            return true;
        }

        public void setPrimaryRole(PersonnelRole primaryRole) {
            this.primaryRole = primaryRole;
        }

        public void setExpLevel(Integer level) {
            expLevel = level;
        }

        public void setRank(Integer rank) {
            this.rank = rank;
        }

        public void setOnlyOfficers(boolean onlyOfficers) {
            this.onlyOfficers = onlyOfficers;
        }

        public void setNoOfficers(boolean noOfficers) {
            this.noOfficers = noOfficers;
        }

        public void setSkill(String skillName) {
            skill = skillName;
        }

        public void setMaxSkillLevel(int level) {
            maxSkillLevel = level;
        }

        public void setAllowPrisoners(boolean allowPrisoners) {
            prisoners = allowPrisoners;
        }
    }

    private static class PersonTypeItem {
        private String name;
        private Integer id;

        public PersonTypeItem(String name, Integer id) {
            setName(Objects.requireNonNull(name));
            setId(id);
        }

        public String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        private void setId(Integer id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}

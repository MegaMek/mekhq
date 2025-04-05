/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.round;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.models.XTableColumnModel;
import megamek.codeUtilities.MathUtility;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.enums.PersonnelTableModelColumn;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public final class BatchXPDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(BatchXPDialog.class);

    private final Campaign campaign;
    private final PersonnelTableModel personnelModel;
    private TableRowSorter<PersonnelTableModel> personnelSorter;
    private PersonnelFilter personnelFilter;

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

    private final List<PersonnelTableModelColumn> batchXPColumns = List.of(PersonnelTableModelColumn.RANK,
          PersonnelTableModelColumn.FIRST_NAME,
          PersonnelTableModelColumn.LAST_NAME,
          PersonnelTableModelColumn.AGE,
          PersonnelTableModelColumn.PERSONNEL_ROLE,
          PersonnelTableModelColumn.XP);

    private JLabel matchedPersonnelLabel;

    private transient String choiceNoSkill;
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.BatchXPDialog",
          MekHQ.getMHQOptions().getLocale());

    public BatchXPDialog(JFrame parent, Campaign campaign) {
        super(parent, "", true);

        setTitle(resourceMap.getString("MassTrainingDialog.title"));
        choiceNoSkill = resourceMap.getString("skill.choice.text");

        this.campaign = Objects.requireNonNull(campaign);
        this.personnelModel = new PersonnelTableModel(campaign);
        personnelModel.refreshData();

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        add(getPersonnelTable(), BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.WEST);

        pack();
        setLocationRelativeTo(getParent());
    }

    private JComponent getPersonnelTable() {
        personnelTable = new JTable(personnelModel);
        personnelTable.setCellSelectionEnabled(false);
        personnelTable.setColumnModel(new XTableColumnModel());
        personnelTable.createDefaultColumnsFromModel();
        personnelTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        personnelTable.setIntercellSpacing(new Dimension(1, 0));
        personnelTable.setShowGrid(false);

        personnelSorter = new TableRowSorter<>(personnelModel);
        personnelSorter.setSortsOnUpdates(true);

        final XTableColumnModel columnModel = (XTableColumnModel) personnelTable.getColumnModel();
        final List<SortKey> sortKeys = new ArrayList<>();
        for (final PersonnelTableModelColumn column : PersonnelTableModel.PERSONNEL_COLUMNS) {
            final TableColumn tableColumn = columnModel.getColumnByModelIndex(column.ordinal());
            if (!batchXPColumns.contains(column)) {
                columnModel.setColumnVisible(tableColumn, false);
                continue;
            }

            tableColumn.setPreferredWidth(column.getWidth());
            tableColumn.setCellRenderer(getRenderer());
            columnModel.setColumnVisible(tableColumn, true);

            personnelSorter.setComparator(column.ordinal(), column.getComparator(campaign));
            final SortOrder sortOrder = column.getDefaultSortOrder();
            if (sortOrder != null) {
                sortKeys.add(new SortKey(column.ordinal(), sortOrder));
            }
        }
        personnelSorter.setSortKeys(sortKeys);
        personnelFilter = new PersonnelFilter(campaign);
        personnelSorter.setRowFilter(personnelFilter);
        personnelTable.setRowSorter(personnelSorter);

        final JScrollPane pane = new JScrollPaneWithSpeed(personnelTable);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        return pane;
    }

    private TableCellRenderer getRenderer() {
        return new Renderer();
    }

    private class Renderer extends MekHqTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int modelRow = table.convertRowIndexToModel(row);
            final PersonnelTableModelColumn personnelColumn = batchXPColumns.get(column);
            Person person = personnelModel.getPerson(modelRow);
            String displayText = personnelColumn.getDisplayText(campaign, person);
            if (displayText != null) {
                setText(displayText);
            }

            return this;
        }
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
            personTypeModel.addElement(new PersonTypeItem(personnelRole.getName(campaign.getFaction().isClan()),
                  personnelRole.ordinal()));
        }
        choiceType.setModel(personTypeModel);
        choiceType.setSelectedIndex(0);
        choiceType.addActionListener(e -> {
            PersonTypeItem personTypeItem = (PersonTypeItem) Objects.requireNonNull(choiceType.getSelectedItem());
            personnelFilter.setPrimaryRole((personTypeItem.getId() == null) ?
                                                 null :
                                                 personnelRoles[personTypeItem.getId()]);
            updatePersonnelTable();
        });
        panel.add(choiceType);

        choiceExp = new JComboBox<>();
        choiceExp.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) choiceType.getPreferredSize().getHeight()));
        DefaultComboBoxModel<PersonTypeItem> personExpModel = new DefaultComboBoxModel<>();
        personExpModel.addElement(new PersonTypeItem(resourceMap.getString("experience.choice.text"), null));
        for (int i = SkillLevel.ULTRA_GREEN.ordinal(); i <= SkillLevel.ELITE.ordinal(); i++) {
            final SkillLevel skillLevel = SkillLevel.parseFromInteger(i);
            personExpModel.addElement(new PersonTypeItem(skillLevel.toString(), skillLevel.getAdjustedValue()));
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
        onlyOfficersPanel.setAlignmentY(JComponent.TOP_ALIGNMENT);
        onlyOfficersPanel.add(onlyOfficers);
        onlyOfficersPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,
              (int) onlyOfficersPanel.getPreferredSize().getHeight()));
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
        noOfficersPanel.setAlignmentY(JComponent.TOP_ALIGNMENT);
        noOfficersPanel.add(noOfficers);
        noOfficersPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,
              (int) noOfficersPanel.getPreferredSize().getHeight()));
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
        choiceSkill.addActionListener(evt -> {
            if (choiceNoSkill.equals(choiceSkill.getSelectedItem())) {
                personnelFilter.setSkillName(null);
                ((SpinnerNumberModel) skillLevel.getModel()).setMaximum(10);
                skillLevel.setEnabled(false);
                buttonSpendXP.setEnabled(false);
            } else {
                final String skillName = (String) choiceSkill.getSelectedItem();
                final SkillType skillType = SkillType.getType(skillName);
                if (skillType == null) {
                    logger.error("Cannot mass train unknown skill type with name " + skillName);
                    return;
                }
                personnelFilter.setSkillName(skillName);
                int maxSkillLevel = SkillType.getType(skillName).getMaxLevel();
                if (maxSkillLevel == -1) {
                    skillLevel.setEnabled(false);
                    buttonSpendXP.setEnabled(false);
                } else {
                    skillLevel.setEnabled(true);
                    ((SpinnerNumberModel) skillLevel.getModel()).setMaximum(maxSkillLevel);
                    skillLevel.getModel()
                          .setValue(MathUtility.clamp((Integer) skillLevel.getModel().getValue(), 1, maxSkillLevel));
                    buttonSpendXP.setEnabled(true);
                }
            }
            updatePersonnelTable();
        });
        panel.add(choiceSkill);

        panel.add(Box.createRigidArea(new Dimension(10, 10)));
        panel.add(new JLabel(resourceMap.getString("targetSkillLevel.text")));

        skillLevel = new JSpinner(new SpinnerNumberModel(10, 1, 10, 1));
        skillLevel.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) skillLevel.getPreferredSize().getHeight()));
        skillLevel.addChangeListener(evt -> {
            personnelFilter.setMaxSkillLevel((Integer) skillLevel.getModel().getValue());
            updatePersonnelTable();
        });
        panel.add(skillLevel);

        allowPrisoners = new JCheckBox(resourceMap.getString("allowPrisoners.text"));
        allowPrisoners.setHorizontalAlignment(SwingConstants.LEFT);
        allowPrisoners.setMaximumSize(new Dimension(Short.MAX_VALUE,
              (int) allowPrisoners.getPreferredSize().getHeight()));
        allowPrisoners.addChangeListener(e -> {
            personnelFilter.setAllowPrisoners(allowPrisoners.isSelected());
            updatePersonnelTable();
        });
        JPanel allowPrisonersPanel = new JPanel(new GridLayout(1, 1));
        allowPrisonersPanel.setAlignmentY(JComponent.TOP_ALIGNMENT);
        allowPrisonersPanel.add(allowPrisoners);
        allowPrisonersPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,
              (int) allowPrisonersPanel.getPreferredSize().getHeight()));
        panel.add(allowPrisonersPanel);

        panel.add(Box.createVerticalGlue());

        matchedPersonnelLabel = new JLabel("");
        matchedPersonnelLabel.setMaximumSize(new Dimension(Short.MAX_VALUE,
              (int) matchedPersonnelLabel.getPreferredSize().getHeight()));
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

    private void updatePersonnelTable() {
        personnelSorter.sort();
        if (!choiceNoSkill.equals(choiceSkill.getSelectedItem())) {
            int rows = personnelTable.getRowCount();
            matchedPersonnelLabel.setText(String.format(resourceMap.getString("eligible.format"), rows));
        } else {
            matchedPersonnelLabel.setText("");
        }
    }

    private void spendXP() {
        String skillName = (String) choiceSkill.getSelectedItem();
        if (choiceNoSkill.equals(skillName) || (skillName == null)) {
            // This shouldn't happen, but guard against it anyway.
            return;
        }
        int rows = personnelTable.getRowCount();
        int improvedPersonnelCount = rows;
        while (rows > 0) {
            for (int i = 0; i < rows; ++i) {
                final CampaignOptions campaignOptions = campaign.getCampaignOptions();

                Person person = personnelModel.getPerson(personnelTable.convertRowIndexToModel(i));

                int cost = person.getCostToImprove(skillName, campaignOptions.isUseReasoningXpMultiplier());
                double costMultiplier = campaignOptions.getXpCostMultiplier();

                cost = (int) round(cost * costMultiplier);

                // Improve the skill and deduce the cost
                person.improveSkill(skillName);
                person.spendXP(cost);
                PersonalLogger.improvedSkill(campaign,
                      person,
                      campaign.getLocalDate(),
                      person.getSkill(skillName).getType().getName(),
                      person.getSkill(skillName).toString(person.getOptions(), person.getReputation()));
                campaign.personUpdated(person);
            }

            // Refresh the filter and continue if we still have anyone available
            updatePersonnelTable();
            rows = personnelTable.getRowCount();
        }

        if (improvedPersonnelCount > 0) {
            campaign.addReport(String.format(resourceMap.getString("improvedSkills.format"),
                  skillName,
                  improvedPersonnelCount));
        }
    }

    public static class PersonnelFilter extends RowFilter<PersonnelTableModel, Integer> {
        private final Campaign campaign;
        private PersonnelRole primaryRole = null;
        private Integer expLevel = null;
        private Integer rank = null;
        private boolean onlyOfficers = false;
        private boolean noOfficers = false;
        private String skillName = null;
        private int maxSkillLevel = 10;
        private boolean prisoners = false;

        public PersonnelFilter(final Campaign campaign) {
            this.campaign = campaign;
        }

        @Override
        public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
            Person person = entry.getModel().getPerson(entry.getIdentifier().intValue());
            if (!person.getStatus().isActive()) {
                return false;
            } else if (!prisoners && !person.getPrisonerStatus().isFree()) {
                return false;
            } else if ((null != primaryRole) && (person.getPrimaryRole() != primaryRole)) {
                return false;
            } else if ((null != expLevel) && (person.getExperienceLevel(campaign, false) != expLevel)) {
                return false;
            } else if (onlyOfficers && !person.getRank().isOfficer()) {
                return false;
            } else if (noOfficers && person.getRank().isOfficer()) {
                return false;
            } else if ((rank != null) && (person.getRankNumeric() != rank)) {
                return false;
            } else if (null != skillName) {
                final CampaignOptions campaignOptions = campaign.getCampaignOptions();
                final double xpCostMultiplier = campaignOptions.getXpCostMultiplier();
                Skill skill = person.getSkill(skillName);
                int cost = person.getCostToImprove(skillName, campaignOptions.isUseReasoningXpMultiplier());
                cost = (int) round(cost * xpCostMultiplier);

                if (null == skill) {
                    return (cost >= 0) && (cost <= person.getXP());
                } else {
                    return (skill.getLevel() < maxSkillLevel) && (cost >= 0) && (cost <= person.getXP());
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

        /**
         * @deprecated Use {@link #setSkillName(String)} instead.
         */
        @Deprecated(since = "0.50.05", forRemoval = true)
        public void setSkill(String skill) {
            this.skillName = skill;
        }

        /**
         * Sets the the name of the target skill.
         *
         * @param skillName the name of the skill to be set.
         */
        public void setSkillName(String skillName) {
            this.skillName = skillName;
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

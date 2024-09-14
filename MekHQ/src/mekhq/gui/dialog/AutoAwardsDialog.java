/*
 * Copyright (c) 2014-2024 - The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.models.XTableColumnModel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.gui.CampaignGUI;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.model.AutoAwardsTableModel;
import mekhq.gui.sorter.PersonRankStringSorter;

public class AutoAwardsDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(AutoAwardsDialog.class);

    final Campaign campaign;
    final CampaignGUI gui;

    private static final String PAN_AUTO_AWARDS = "PanAutoAwards";

    final private Map<Integer, Map<Integer, List<Object>>> allData;
    final private Map<Integer, List<Object>> data;
    final private int currentPageCount;

    private JComboBox<PersonnelFilter> cboPersonnelFilter;
    private JButton btnSelectAll;
    private JButton btnDeselectAll;
    private AutoAwardsTable personnelTable;
    private TableRowSorter<AutoAwardsTableModel> personnelSorter;

    private JButton btnSkip;
    private JButton btnSkipAll;
    private JButton btnDone;

    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AutoAwardsDialog",
            MekHQ.getMHQOptions().getLocale());

    public AutoAwardsDialog(Campaign c, Map<Integer, Map<Integer, List<Object>>> allAwardData, int ceremonyCount) {
        campaign = c;
        gui = campaign.getApp().getCampaigngui();
        allData = allAwardData;
        logger.info("attempting to extract a single page");
        data = allAwardData.get(ceremonyCount);
        logger.info("attempt successful");
        currentPageCount = ceremonyCount;

        setSize(new Dimension(800, 600));
        initComponents();
        setLocationRelativeTo(gui.getFrame());
    }

    private void initComponents() {
        setTitle(resourceMap.getString("AutoAwardsDialog.title"));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) (screenSize.getWidth() * 0.75);
        int screenHeight = (int) (screenSize.getHeight() * 0.94);

        setSize(screenWidth, screenHeight);

        setLayout(new BorderLayout());
        CardLayout cardLayout = new CardLayout();
        JPanel panMain = new JPanel(cardLayout);
        add(panMain, BorderLayout.CENTER);

        // we work with a combination image & instructions panel, as that allows us to
        // sit the image
        // right below the title, but above the instructions
        JPanel imageAndInstructionsPanel = new JPanel(new BorderLayout());

        Image image = new ImageIcon("data/images/awards/awardceremony.png")
                .getImage().getScaledInstance(screenWidth, (screenHeight / 7), Image.SCALE_FAST);
        JLabel lblImage = new JLabel(new ImageIcon(image));
        imageAndInstructionsPanel.add(lblImage, BorderLayout.CENTER);

        JTextArea txtInstructions = new JTextArea();
        txtInstructions.setEditable(false);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setLineWrap(true);
        txtInstructions.setText(resourceMap.getString("txtInstructions.text"));
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        imageAndInstructionsPanel.add(txtInstructions, BorderLayout.SOUTH);
        add(imageAndInstructionsPanel, BorderLayout.PAGE_START);

        JPanel autoAwardsPanel = new JPanel(new BorderLayout());

        cboPersonnelFilter = new JComboBox<>();
        cboPersonnelFilter.setMaximumSize(new Dimension(200, 20));

        for (PersonnelFilter filter : MekHQ.getMHQOptions().getPersonnelFilterStyle().getFilters(true)) {
            cboPersonnelFilter.addItem(filter);
        }

        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.X_AXIS));

        upperPanel.add(cboPersonnelFilter);
        upperPanel.add(Box.createHorizontalGlue());
        upperPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        btnDeselectAll = new JButton(resourceMap.getString("btnDeselectAll.text"));
        btnDeselectAll.addMouseListener(toggleAllListener);
        btnDeselectAll.setVisible(true);
        upperPanel.add(btnDeselectAll);

        btnSelectAll = new JButton(resourceMap.getString("btnSelectAll.text"));
        btnSelectAll.addMouseListener(toggleAllListener);
        btnSelectAll.setVisible(false);
        upperPanel.add(btnSelectAll);

        autoAwardsPanel.add(upperPanel, BorderLayout.PAGE_START);

        AutoAwardsTableModel model = new AutoAwardsTableModel(campaign);
        // This is where we insert the external data
        logger.info("Trying to pass data to AutoAwardsTableModel.java");
        logger.info("Data being passed: {}", data);
        model.setData(data);
        logger.info("Attempt successful");
        personnelTable = new AutoAwardsTable(model);
        personnelSorter = new TableRowSorter<>(model);
        personnelSorter.setComparator(AutoAwardsTableModel.COL_PERSON, new PersonRankStringSorter(campaign));
        personnelTable.setRowSorter(personnelSorter);
        ArrayList<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(AutoAwardsTableModel.COL_PERSON, SortOrder.DESCENDING));
        personnelSorter.setSortKeys(sortKeys);

        cboPersonnelFilter.addActionListener(evt -> filterPersonnel(personnelSorter, cboPersonnelFilter));

        TableColumn awardColumn = personnelTable.getColumnModel()
                .getColumn(personnelTable.convertColumnIndexToModel(AutoAwardsTableModel.COL_AWARD));

        DefaultCellEditor cellEditor = (DefaultCellEditor) awardColumn.getCellEditor();

        JCheckBox cbxAward = (JCheckBox) cellEditor.getComponent();
        cbxAward.addMouseListener(checkboxListener);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(personnelTable);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        autoAwardsPanel.add(scrollPane, BorderLayout.CENTER);

        panMain.add(autoAwardsPanel, PAN_AUTO_AWARDS);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        btnDone = new JButton(resourceMap.getString("btnDone.text"));
        btnDone.addActionListener(buttonListener);
        btnSkip = new JButton(resourceMap.getString("btnSkip.text"));
        btnSkip.addActionListener(buttonListener);
        btnSkipAll = new JButton(resourceMap.getString("btnSkipAll.text"));
        btnSkipAll.addActionListener(buttonListener);

        btnPanel.add(btnDone);
        btnPanel.add(btnSkip);
        btnPanel.add(btnSkipAll);

        add(btnPanel, BorderLayout.PAGE_END);
    }

    final private MouseListener checkboxListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            Checkbox checkbox = (Checkbox) e.getSource();
            boolean currentState = checkbox.getState();
            checkbox.setState(!currentState);
        }
    };

    final private MouseListener toggleAllListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            Object source = e.getSource();

            if (source instanceof JButton button) {
                AutoAwardsTableModel model = (AutoAwardsTableModel) personnelTable.getModel();

                if (button.equals(btnSelectAll)) {
                    btnSelectAll.setVisible(false);
                    btnDeselectAll.setVisible(true);

                    for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
                        model.setValueAt(true, rowIndex, AutoAwardsTableModel.COL_AWARD);
                    }
                } else if (button.equals(btnDeselectAll)) {
                    btnSelectAll.setVisible(true);
                    btnDeselectAll.setVisible(false);

                    for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
                        model.setValueAt(false, rowIndex, AutoAwardsTableModel.COL_AWARD);
                    }
                }
            }
        }
    };

    final private ActionListener buttonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getSource().equals(btnDone)) {
                for (int rowIndex = 0; rowIndex < personnelTable.getRowCount(); rowIndex++) {
                    if ((boolean) personnelTable.getValueAt(rowIndex, 3)) {
                        Person person = campaign.getPerson((UUID) data.get(rowIndex).get(0));
                        Award award = (Award) data.get(rowIndex).get(1);

                        List<Award> awardsForRemoval = new ArrayList<>();

                        if ((award.canBeAwarded(person))
                                && (award.getItem().equalsIgnoreCase("rank"))
                                && (award.getRange().equalsIgnoreCase("Promotion"))) {
                            for (Award existingAward : person.getAwardController().getAwards()) {
                                if ((!existingAward.getItem().equalsIgnoreCase("rank"))
                                        || (!existingAward.getRange().equalsIgnoreCase("promotion"))) {
                                    continue;
                                }

                                awardsForRemoval.add(existingAward);
                            }
                        }

                        if (!awardsForRemoval.isEmpty()) {
                            for (Award awardPendingRemoval : awardsForRemoval) {
                                person.getAwardController().removeAwardSilent(
                                        awardPendingRemoval.getSet(),
                                        awardPendingRemoval.getName(),
                                        null);
                            }
                        }

                        person.getAwardController().addAndLogAward(campaign, award.getSet(),
                                award.getName(), campaign.getLocalDate());
                    }
                }

                // this disables the current page
                setVisible(false);

                // if necessary, this initiates the next page
                if ((currentPageCount + 1) < allData.size()) {
                    AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(campaign, allData, (currentPageCount + 1));
                    autoAwardsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
                    autoAwardsDialog.setLocation(autoAwardsDialog.getLocation().x, 0);
                    autoAwardsDialog.setVisible(true);
                }
            } else if (event.getSource().equals(btnSkip)) {
                setVisible(false);

                if ((currentPageCount + 1) < allData.size()) {
                    AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(campaign, allData, (currentPageCount + 1));
                    autoAwardsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
                    autoAwardsDialog.setLocation(autoAwardsDialog.getLocation().x, 0);
                    autoAwardsDialog.setVisible(true);
                }
            } else if (event.getSource().equals(btnSkipAll)) {
                // we just need to disable the dialog if we're skipping all remaining pages
                setVisible(false);
            }
        }
    };

    private void filterPersonnel(TableRowSorter<AutoAwardsTableModel> sorter, JComboBox<PersonnelFilter> comboBox) {
        PersonnelFilter filter = (comboBox.getSelectedItem() == null)
                // this needs to be ALL, as we may have dead personnel in the table
                ? PersonnelFilter.ALL
                : (PersonnelFilter) comboBox.getSelectedItem();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends AutoAwardsTableModel, ? extends Integer> entry) {
                Person person = entry.getModel().getPerson(entry.getIdentifier());

                return filter.getFilteredInformation(person, campaign.getLocalDate());
            }
        });
    }
}

class AutoAwardsTable extends JTable {
    public AutoAwardsTable(AutoAwardsTableModel model) {
        super(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        XTableColumnModel columnModel = new XTableColumnModel();
        setColumnModel(columnModel);
        createDefaultColumnsFromModel();
        TableColumn column;
        for (int columnIndex = 0; columnIndex < AutoAwardsTableModel.N_COL; columnIndex++) {
            column = getColumnModel().getColumn(convertColumnIndexToView(columnIndex));
            column.setPreferredWidth(model.getColumnWidth(columnIndex));
            if (columnIndex != AutoAwardsTableModel.COL_AWARD) {
                column.setCellRenderer(model.getRenderer(columnIndex));
            }
        }

        setRowHeight(50);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        getColumnModel().getColumn(convertColumnIndexToView(AutoAwardsTableModel.COL_AWARD))
                .setCellEditor(new DefaultCellEditor(new JCheckBox()));
    }
}

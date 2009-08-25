/*
 * MekBayView.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq;

import gd.xml.ParseException;
import java.awt.Color;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.Campaign;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import megamek.client.ui.MechView;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.TargetRoll;
import megamek.common.XMLStreamParser;
import mekhq.campaign.Unit;
import mekhq.campaign.Utilities;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.PersonnelWorkItem;
import mekhq.campaign.work.ReloadItem;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.SalvageItem;
import mekhq.campaign.work.UnitWorkItem;
import mekhq.campaign.work.WorkItem;

/**
 * The application's main frame.
 */
public class MekHQView extends FrameView {

    
    private Campaign campaign = new Campaign();
    private TaskTableModel taskModel = new TaskTableModel();
    private MekTableModel unitModel = new MekTableModel();
    private TechTableModel techsModel = new TechTableModel();
    private PersonTableModel personnelModel = new PersonTableModel();
    private DocTableModel doctorsModel = new DocTableModel();
    private PartsTableModel partsModel = new PartsTableModel();
    private MekTableMouseAdapter unitMouseAdapter;
    private TaskTableMouseAdapter taskMouseAdapter;
    private int currentUnitId;
    private int currentTaskId;
    private int currentTechId;
    private int currentPersonId;
    private int currentDoctorId;
    private int currentPartsId;
    
    public MekHQView(SingleFrameApplication app) {
        super(app);
        
        unitMouseAdapter = new MekTableMouseAdapter();
        taskMouseAdapter = new TaskTableMouseAdapter();
        initComponents();

        refreshCalendar();
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = MekHQApp.getApplication().getMainFrame();
            aboutBox = new MekHQAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        MekHQApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
        tabMain = new javax.swing.JTabbedPane();
        panHangar = new javax.swing.JPanel();
        scrollTaskTable = new javax.swing.JScrollPane();
        TaskTable = new javax.swing.JTable();
        scrollUnitTable = new javax.swing.JScrollPane();
        UnitTable = new javax.swing.JTable();
        scrollTechTable = new javax.swing.JScrollPane();
        TechTable = new javax.swing.JTable();
        btnUnitPanel = new javax.swing.JPanel();
        btnDeployUnits = new javax.swing.JButton();
        btnRetrieveUnits = new javax.swing.JButton();
        panelDoTask = new javax.swing.JPanel();
        btnDoTask = new javax.swing.JButton();
        lblTarget = new javax.swing.JLabel();
        lblTargetNum = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        textTarget = new javax.swing.JTextArea();
        panSupplies = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        PartsTable = new javax.swing.JTable();
        panPersonnel = new javax.swing.JPanel();
        btnAssignDoc = new javax.swing.JButton();
        scrollPersonTable = new javax.swing.JScrollPane();
        PersonTable = new javax.swing.JTable();
        scrollDocTable = new javax.swing.JScrollPane();
        DocTable = new javax.swing.JTable();
        panFinances = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        txtPaneReport = new javax.swing.JTextPane();
        panelMasterButtons = new javax.swing.JPanel();
        btnAdvanceDay = new javax.swing.JButton();
        btnOvertime = new javax.swing.JToggleButton();
        btnGMMode = new javax.swing.JToggleButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        menuLoad = new javax.swing.JMenuItem();
        menuSave = new javax.swing.JMenuItem();
        menuOptions = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        miLoadForces = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        menuMarket = new javax.swing.JMenu();
        miPurchaseUnit = new javax.swing.JMenuItem();
        menuHire = new javax.swing.JMenu();
        miHirePilot = new javax.swing.JMenuItem();
        miHireTech = new javax.swing.JMenuItem();
        miHireDoctor = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setAutoscrolls(true);
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.GridBagLayout());

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(MekHQView.class);
        tabMain.setToolTipText(resourceMap.getString("tabMain.toolTipText")); // NOI18N
        tabMain.setMinimumSize(new java.awt.Dimension(600, 200));
        tabMain.setName("tabMain"); // NOI18N
        tabMain.setPreferredSize(new java.awt.Dimension(900, 300));

        panHangar.setFont(resourceMap.getFont("panHangar.font")); // NOI18N
        panHangar.setName("panHangar"); // NOI18N
        panHangar.setLayout(new java.awt.GridBagLayout());

        scrollTaskTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTaskTable.setName("scrollTaskTable"); // NOI18N
        scrollTaskTable.setPreferredSize(new java.awt.Dimension(300, 300));

        TaskTable.setModel(taskModel);
        TaskTable.setName("TaskTable"); // NOI18N
        TaskTable.setRowHeight(60);
        TaskTable.getColumnModel().getColumn(0).setCellRenderer(taskModel.getRenderer());
        TaskTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                TaskTableValueChanged(evt);
            }
        });
        TaskTable.addMouseListener(taskMouseAdapter);
        scrollTaskTable.setViewportView(TaskTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        panHangar.add(scrollTaskTable, gridBagConstraints);

        scrollUnitTable.setMinimumSize(new java.awt.Dimension(300, 200));
        scrollUnitTable.setName("scrollUnitTable"); // NOI18N
        scrollUnitTable.setPreferredSize(new java.awt.Dimension(300, 300));

        UnitTable.setModel(unitModel);
        UnitTable.setName("UnitTable"); // NOI18N
        UnitTable.setRowHeight(80);
        UnitTable.getColumnModel().getColumn(0).setCellRenderer(unitModel.getRenderer());
        UnitTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                UnitTableValueChanged(evt);
            }
        });
        UnitTable.addMouseListener(unitMouseAdapter);
        scrollUnitTable.setViewportView(UnitTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        panHangar.add(scrollUnitTable, gridBagConstraints);

        scrollTechTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTechTable.setName("scrollTechTable"); // NOI18N
        scrollTechTable.setPreferredSize(new java.awt.Dimension(300, 300));

        TechTable.setModel(techsModel);
        TechTable.setName("TechTable"); // NOI18N
        TechTable.setRowHeight(60);
        TechTable.getColumnModel().getColumn(0).setCellRenderer(techsModel.getRenderer());
        TechTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                TechTableValueChanged(evt);
            }
        });
        scrollTechTable.setViewportView(TechTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        panHangar.add(scrollTechTable, gridBagConstraints);

        btnUnitPanel.setMinimumSize(new java.awt.Dimension(300, 50));
        btnUnitPanel.setName("btnUnitPanel"); // NOI18N
        btnUnitPanel.setPreferredSize(new java.awt.Dimension(300, 50));
        btnUnitPanel.setLayout(new java.awt.GridBagLayout());

        btnDeployUnits.setText(resourceMap.getString("btnDeployUnits.text")); // NOI18N
        btnDeployUnits.setToolTipText(resourceMap.getString("btnDeployUnits.toolTipText")); // NOI18N
        btnDeployUnits.setName("btnDeployUnits"); // NOI18N
        btnDeployUnits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeployUnitsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        btnUnitPanel.add(btnDeployUnits, gridBagConstraints);

        btnRetrieveUnits.setText(resourceMap.getString("btnRetrieveUnits.text")); // NOI18N
        btnRetrieveUnits.setToolTipText(resourceMap.getString("btnRetrieveUnits.toolTipText")); // NOI18N
        btnRetrieveUnits.setName("btnRetrieveUnits"); // NOI18N
        btnRetrieveUnits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRetrieveUnitsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        btnUnitPanel.add(btnRetrieveUnits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        panHangar.add(btnUnitPanel, gridBagConstraints);

        panelDoTask.setMinimumSize(new java.awt.Dimension(300, 100));
        panelDoTask.setName("panelDoTask"); // NOI18N
        panelDoTask.setPreferredSize(new java.awt.Dimension(300, 100));
        panelDoTask.setLayout(new java.awt.GridBagLayout());

        btnDoTask.setText(resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTask.setEnabled(false);
        btnDoTask.setName("btnDoTask"); // NOI18N
        btnDoTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDoTaskActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panelDoTask.add(btnDoTask, gridBagConstraints);

        lblTarget.setText(resourceMap.getString("lblTarget.text")); // NOI18N
        lblTarget.setName("lblTarget"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        panelDoTask.add(lblTarget, gridBagConstraints);

        lblTargetNum.setFont(resourceMap.getFont("lblTargetNum.font")); // NOI18N
        lblTargetNum.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTargetNum.setText(resourceMap.getString("lblTargetNum.text")); // NOI18N
        lblTargetNum.setName("lblTargetNum"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        panelDoTask.add(lblTargetNum, gridBagConstraints);

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        textTarget.setBackground(resourceMap.getColor("textTarget.background")); // NOI18N
        textTarget.setColumns(20);
        textTarget.setEditable(false);
        textTarget.setFont(resourceMap.getFont("textTarget.font")); // NOI18N
        textTarget.setLineWrap(true);
        textTarget.setRows(5);
        textTarget.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTarget.setWrapStyleWord(true);
        textTarget.setBorder(null);
        textTarget.setName("textTarget"); // NOI18N
        jScrollPane6.setViewportView(textTarget);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTask.add(jScrollPane6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        panHangar.add(panelDoTask, gridBagConstraints);

        tabMain.addTab(resourceMap.getString("panHangar.TabConstraints.tabTitle"), panHangar); // NOI18N

        panSupplies.setName("panSupplies"); // NOI18N

        jScrollPane8.setName("jScrollPane8"); // NOI18N

        PartsTable.setModel(partsModel);
        PartsTable.setName("PartsTable"); // NOI18N
        PartsTable.setRowHeight(60);
        PartsTable.getColumnModel().getColumn(0).setCellRenderer(partsModel.getRenderer());
        PartsTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                PartsTableValueChanged(evt);
            }
        });
        jScrollPane8.setViewportView(PartsTable);

        org.jdesktop.layout.GroupLayout panSuppliesLayout = new org.jdesktop.layout.GroupLayout(panSupplies);
        panSupplies.setLayout(panSuppliesLayout);
        panSuppliesLayout.setHorizontalGroup(
            panSuppliesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSuppliesLayout.createSequentialGroup()
                .add(jScrollPane8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 428, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(634, Short.MAX_VALUE))
        );
        panSuppliesLayout.setVerticalGroup(
            panSuppliesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSuppliesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabMain.addTab(resourceMap.getString("panSupplies.TabConstraints.tabTitle"), panSupplies); // NOI18N

        panPersonnel.setName("panPersonnel"); // NOI18N
        panPersonnel.setLayout(new java.awt.GridBagLayout());

        btnAssignDoc.setText(resourceMap.getString("btnAssignDoc.text")); // NOI18N
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.setName("btnAssignDoc"); // NOI18N
        btnAssignDoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAssignDocActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        panPersonnel.add(btnAssignDoc, gridBagConstraints);

        scrollPersonTable.setMinimumSize(new java.awt.Dimension(300, 200));
        scrollPersonTable.setName("scrollPersonTable"); // NOI18N
        scrollPersonTable.setPreferredSize(new java.awt.Dimension(300, 300));

        PersonTable.setModel(personnelModel);
        PersonTable.setName("PersonTable"); // NOI18N
        PersonTable.setRowHeight(60);
        PersonTable.getColumnModel().getColumn(0).setCellRenderer(personnelModel.getRenderer());
        PersonTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                PersonTableValueChanged(evt);
            }
        });
        scrollPersonTable.setViewportView(PersonTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        panPersonnel.add(scrollPersonTable, gridBagConstraints);

        scrollDocTable.setMinimumSize(new java.awt.Dimension(300, 300));
        scrollDocTable.setName("scrollDocTable"); // NOI18N
        scrollDocTable.setPreferredSize(new java.awt.Dimension(300, 300));

        DocTable.setModel(doctorsModel);
        DocTable.setName("DocTable"); // NOI18N
        DocTable.setRowHeight(60);
        DocTable.getColumnModel().getColumn(0).setCellRenderer(doctorsModel.getRenderer());
        DocTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                DocTableValueChanged(evt);
            }
        });
        scrollDocTable.setViewportView(DocTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        panPersonnel.add(scrollDocTable, gridBagConstraints);

        tabMain.addTab(resourceMap.getString("panPersonnel.TabConstraints.tabTitle"), panPersonnel); // NOI18N

        panFinances.setName("panFinances"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        org.jdesktop.layout.GroupLayout panFinancesLayout = new org.jdesktop.layout.GroupLayout(panFinances);
        panFinances.setLayout(panFinancesLayout);
        panFinancesLayout.setHorizontalGroup(
            panFinancesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panFinancesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addContainerGap(877, Short.MAX_VALUE))
        );
        panFinancesLayout.setVerticalGroup(
            panFinancesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panFinancesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addContainerGap(546, Short.MAX_VALUE))
        );

        tabMain.addTab(resourceMap.getString("panFinances.TabConstraints.tabTitle"), panFinances); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(tabMain, gridBagConstraints);

        jScrollPane7.setMinimumSize(new java.awt.Dimension(800, 200));
        jScrollPane7.setName("jScrollPane7"); // NOI18N
        jScrollPane7.setPreferredSize(new java.awt.Dimension(800, 200));

        txtPaneReport.setContentType(resourceMap.getString("txtPaneReport.contentType")); // NOI18N
        txtPaneReport.setEditable(false);
        txtPaneReport.setFont(resourceMap.getFont("txtPaneReport.font")); // NOI18N
        txtPaneReport.setText(campaign.getCurrentReportHTML());
        txtPaneReport.setMinimumSize(new java.awt.Dimension(800, 200));
        txtPaneReport.setName("txtPaneReport"); // NOI18N
        txtPaneReport.setPreferredSize(new java.awt.Dimension(800, 200));
        jScrollPane7.setViewportView(txtPaneReport);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        mainPanel.add(jScrollPane7, gridBagConstraints);

        panelMasterButtons.setMinimumSize(new java.awt.Dimension(200, 200));
        panelMasterButtons.setName("panelMasterButtons"); // NOI18N
        panelMasterButtons.setPreferredSize(new java.awt.Dimension(200, 220));
        panelMasterButtons.setLayout(new java.awt.GridBagLayout());

        btnAdvanceDay.setText(resourceMap.getString("btnAdvanceDay.text")); // NOI18N
        btnAdvanceDay.setToolTipText(resourceMap.getString("btnAdvanceDay.toolTipText")); // NOI18N
        btnAdvanceDay.setName("btnAdvanceDay"); // NOI18N
        btnAdvanceDay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAdvanceDayActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        panelMasterButtons.add(btnAdvanceDay, gridBagConstraints);

        btnOvertime.setText(resourceMap.getString("btnOvertime.text")); // NOI18N
        btnOvertime.setName("btnOvertime"); // NOI18N
        btnOvertime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOvertimeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panelMasterButtons.add(btnOvertime, gridBagConstraints);

        btnGMMode.setText(resourceMap.getString("btnGMMode.text")); // NOI18N
        btnGMMode.setName("btnGMMode"); // NOI18N
        btnGMMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGMModeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panelMasterButtons.add(btnGMMode, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        mainPanel.add(panelMasterButtons, gridBagConstraints);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        menuLoad.setText(resourceMap.getString("menuLoad.text")); // NOI18N
        menuLoad.setName("menuLoad"); // NOI18N
        menuLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoadActionPerformed(evt);
            }
        });
        fileMenu.add(menuLoad);

        menuSave.setText(resourceMap.getString("menuSave.text")); // NOI18N
        menuSave.setName("menuSave"); // NOI18N
        menuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveActionPerformed(evt);
            }
        });
        fileMenu.add(menuSave);

        menuOptions.setText(resourceMap.getString("menuOptions.text")); // NOI18N
        menuOptions.setName("menuOptions"); // NOI18N
        menuOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOptionsActionPerformed(evt);
            }
        });
        fileMenu.add(menuOptions);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getActionMap(MekHQView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        miLoadForces.setText(resourceMap.getString("miLoadForces.text")); // NOI18N
        miLoadForces.setName("miLoadForces"); // NOI18N

        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        miLoadForces.add(jMenuItem1);

        menuBar.add(miLoadForces);

        menuMarket.setText(resourceMap.getString("menuMarket.text")); // NOI18N
        menuMarket.setName("menuMarket"); // NOI18N

        miPurchaseUnit.setText(resourceMap.getString("miPurchaseUnit.text")); // NOI18N
        miPurchaseUnit.setName("miPurchaseUnit"); // NOI18N
        miPurchaseUnit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miPurchaseUnitActionPerformed(evt);
            }
        });
        menuMarket.add(miPurchaseUnit);

        menuHire.setText(resourceMap.getString("menuHire.text")); // NOI18N
        menuHire.setName("menuHire"); // NOI18N

        miHirePilot.setText(resourceMap.getString("miHirePilot.text")); // NOI18N
        miHirePilot.setName("miHirePilot"); // NOI18N
        miHirePilot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miHirePilotActionPerformed(evt);
            }
        });
        menuHire.add(miHirePilot);

        miHireTech.setText(resourceMap.getString("miHireTech.text")); // NOI18N
        miHireTech.setName("miHireTech"); // NOI18N
        miHireTech.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miHireTechActionPerformed(evt);
            }
        });
        menuHire.add(miHireTech);

        miHireDoctor.setText(resourceMap.getString("miHireDoctor.text")); // NOI18N
        miHireDoctor.setName("miHireDoctor"); // NOI18N
        miHireDoctor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miHireDoctorActionPerformed(evt);
            }
        });
        menuHire.add(miHireDoctor);

        menuMarket.add(menuHire);

        menuBar.add(menuMarket);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(1104, 1104, 1104)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                    .add(statusPanelLayout.createSequentialGroup()
                        .add(statusMessageLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(statusAnimationLabel)
                        .addContainerGap())))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel)
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

private void btnRetrieveUnitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRetrieveUnitsActionPerformed

    try {
        loadListFile(false);
}//GEN-LAST:event_btnRetrieveUnitsActionPerformed
 catch (IOException ex) {
            Logger.getLogger(MekHQView.class.getName()).log(Level.SEVERE, null, ex);
        }
}                                           

private void btnDoTaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDoTaskActionPerformed
    //assign the task to the team here
    WorkItem task = campaign.getTask(currentTaskId);
    SupportTeam team = campaign.getTeam(currentTechId);
    int row = TaskTable.getSelectedRow();
    if(null != task && null != team) {
        boolean completed = campaign.processTask(task, team);
        refreshUnitList();
        refreshTaskList();
        refreshTechsList();
        refreshPartsList();
        refreshReport();
        if(!completed) {
            row++;
        }
        if(row >= TaskTable.getRowCount()) {
            row = 0;
        }
        TaskTable.setRowSelectionInterval(row, row); 
    }
}//GEN-LAST:event_btnDoTaskActionPerformed

private void TechTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = TechTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getTechTeams().size()) {
        currentTechId = campaign.getTechTeams().get(selected).getId();
    }
    else if(selected < 0) {
        currentTechId = -1;
    }
    updateAssignEnabled();
    updateTargetText();
}   

private void TaskTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = TaskTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getTasksForUnit(currentUnitId).size()) {
        currentTaskId = campaign.getTasksForUnit(currentUnitId).get(selected).getId();
    }
    else {
        currentTaskId = -1;
    }
    updateAssignEnabled();
    updateTargetText();
}

private void UnitTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = UnitTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getUnits().size()) {
        currentUnitId = campaign.getUnits().get(selected).getId();
    }
    else if(selected < 0) {
        currentUnitId = -1;
    }
    refreshTaskList();
}

private void PersonTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = PersonTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getPersonnel().size()) {
        currentPersonId = campaign.getPersonnel().get(selected).getId();
    }
    else if(selected < 0) {
        currentPersonId = -1;
    }
    updateAssignDoctorEnabled();
}

private void DocTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = DocTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getDoctors().size()) {
        currentDoctorId = campaign.getDoctors().get(selected).getId();
    }
    else if(selected < 0) {
        currentDoctorId = -1;
    }
    updateAssignDoctorEnabled();
}

private void PartsTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = PartsTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getParts().size()) {
        currentPartsId = campaign.getParts().get(selected).getId();
    }
    else if(selected < 0) {
        currentPartsId = -1;
    }
}


private void btnAdvanceDayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdvanceDayActionPerformed
    campaign.newDay();
    refreshUnitList();
    refreshTaskList();
    refreshTechsList();
    refreshPersonnelList();
    refreshDoctorsList();
    refreshCalendar();
    refreshReport();
}//GEN-LAST:event_btnAdvanceDayActionPerformed

private void btnDeployUnitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeployUnitsActionPerformed
    deployListFile();
}//GEN-LAST:event_btnDeployUnitsActionPerformed

private void btnAssignDocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAssignDocActionPerformed
    if(currentPersonId == -1) {
        return;
    }
    int row = PersonTable.getSelectedRow();
    Person p = campaign.getPerson(currentPersonId);
    if(null != p && null != p.getTask()) {
        p.getTask().setTeam(campaign.getTeam(currentDoctorId));
        row++;
    }
    refreshTechsList();
    refreshDoctorsList();
    refreshPersonnelList();
    if(row >= PersonTable.getRowCount()) {
            row = 0;
    }
    PersonTable.setRowSelectionInterval(row, row); 
}//GEN-LAST:event_btnAssignDocActionPerformed

private void miHirePilotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHirePilotActionPerformed
    NewPilotDialog npd = new NewPilotDialog(this.getFrame(), true);
    npd.setVisible(true);
    if(null != npd.getPilotPerson()) {
        campaign.addPerson(npd.getPilotPerson());
        refreshPersonnelList();
        refreshReport();
    }
}//GEN-LAST:event_miHirePilotActionPerformed

private void miHireTechActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHireTechActionPerformed
    NewTechTeamDialog ntd = new NewTechTeamDialog(this.getFrame(), true);
    ntd.setVisible(true);
    if(null != ntd.getTechTeam()) {
        campaign.addTeam(ntd.getTechTeam());
        refreshTechsList();
        refreshPersonnelList();
        refreshReport();
    }
}//GEN-LAST:event_miHireTechActionPerformed

private void miHireDoctorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHireDoctorActionPerformed
    NewMedicalTeamDialog nmd = new NewMedicalTeamDialog(this.getFrame(), true);
    nmd.setVisible(true);
    if(null != nmd.getMedicalTeam()) {
        campaign.addTeam(nmd.getMedicalTeam());
        refreshTechsList();
        refreshPersonnelList();
        refreshDoctorsList();
        refreshReport();
    }
}//GEN-LAST:event_miHireDoctorActionPerformed

private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveActionPerformed
    JFileChooser saveCpgn = new JFileChooser(".");
    saveCpgn.setDialogTitle("Save Campaign");
    saveCpgn.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File dir) {
            return ((dir.getName() != null) && dir.getName().endsWith(".cpn"));
        }

        @Override
        public String getDescription() {
            return "campaign file (*cpn)";
        }
    });
    saveCpgn.setSelectedFile(new File(campaign.getName() + campaign.getShortDateAsString() + ".cpn")); //$NON-NLS-1$
    int returnVal = saveCpgn.showSaveDialog(mainPanel);
    if ((returnVal != JFileChooser.APPROVE_OPTION) || (saveCpgn.getSelectedFile() == null)) {
       // I want a file, y'know!
       return;
    }
    File file = saveCpgn.getSelectedFile();   
    FileOutputStream fos = null;
    ObjectOutputStream out = null;
    try {
        fos = new FileOutputStream(file);
        out = new ObjectOutputStream(fos);
        out.writeObject(campaign);
        out.close();
    } catch(IOException ex) {
        ex.printStackTrace();
    }
}//GEN-LAST:event_menuSaveActionPerformed

private void menuLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLoadActionPerformed
    JFileChooser loadCpgn = new JFileChooser(".");
    loadCpgn.setDialogTitle("Load Campaign");
    loadCpgn.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File dir) {
            return ((dir.getName() != null) && dir.getName().endsWith(".cpn"));
        }

        @Override
        public String getDescription() {
            return "campaign file (*cpn)";
        }
    }); 
    int returnVal = loadCpgn.showOpenDialog(mainPanel);
    if ((returnVal != JFileChooser.APPROVE_OPTION) || (loadCpgn.getSelectedFile() == null)) {
       // I want a file, y'know!
       return;
    }      
    File file = loadCpgn.getSelectedFile();
    FileInputStream fis = null;
    ObjectInputStream in = null;
    try {
        fis = new FileInputStream(file);
        in = new ObjectInputStream(fis);
        campaign = (Campaign)in.readObject();
        campaign.restore();
        in.close();
    }
    catch(IOException ex) {
        ex.printStackTrace();
    }
    catch(ClassNotFoundException ex)
    {
        ex.printStackTrace();
    }
    refreshUnitList();
    refreshTaskList();
    refreshTechsList();
    refreshPersonnelList();
    refreshDoctorsList();
    refreshPartsList();
    refreshCalendar();
    refreshReport();
}//GEN-LAST:event_menuLoadActionPerformed

private void btnOvertimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOvertimeActionPerformed
    campaign.setOvertime(btnOvertime.isSelected());
    refreshTechsList();
    refreshTaskList();
}//GEN-LAST:event_btnOvertimeActionPerformed

private void btnGMModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGMModeActionPerformed
    campaign.setGMMode(btnGMMode.isSelected());
}//GEN-LAST:event_btnGMModeActionPerformed

private void menuOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsActionPerformed
    CampaignOptionsDialog cod = new CampaignOptionsDialog(this.getFrame(), true, campaign);
    cod.setVisible(true);
    refreshCalendar();
    
}//GEN-LAST:event_menuOptionsActionPerformed

private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try {
            loadListFile(true);//GEN-LAST:event_jMenuItem1ActionPerformed
        } catch (IOException ex) {
            Logger.getLogger(MekHQView.class.getName()).log(Level.SEVERE, null, ex);
        }
}

private void miPurchaseUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miPurchaseUnitActionPerformed
    UnitSelectorDialog usd = new UnitSelectorDialog(this.getFrame(), true);
    usd.setVisible(true);
    Entity en = usd.getSelectedEntity();
    if(null != en) {
        campaign.addUnit(en, false);
        refreshUnitList();
        refreshReport();
    }
}//GEN-LAST:event_miPurchaseUnitActionPerformed

protected void loadListFile(boolean allowNewPilots) throws IOException {
    JFileChooser loadList = new JFileChooser(".");
    loadList.setDialogTitle("Load Units");
    loadList.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File dir) {
            return ((dir.getName() != null) && dir.getName().endsWith(".mul"));
        }

        @Override
        public String getDescription() {
            return "MUL file";
        }
    });
    
    int returnVal = loadList.showOpenDialog(mainPanel);
    if ((returnVal != JFileChooser.APPROVE_OPTION) || (loadList.getSelectedFile() == null)) {
       // I want a file, y'know!
       return;
    }
        
    File unitFile = loadList.getSelectedFile();
    if (unitFile != null) {
        //I need to get the parser myself, because I want to pull both
        //entities and pilots from it
        // Create an empty parser.
        XMLStreamParser parser = new XMLStreamParser();

        // Open up the file.
        InputStream listStream = new FileInputStream(unitFile);

        // Read a Vector from the file.
        try {
        
            parser.parse(listStream);
            listStream.close();
        } catch (ParseException excep) {
            excep.printStackTrace(System.err);
            //throw new IOException("Unable to read from: " + unitFile.getName());
        }

        // Was there any error in parsing?
        if (parser.hasWarningMessage()) {
            System.out.println(parser.getWarningMessage());
        }
           
        // Add the units from the file.
        for (Entity entity : parser.getEntities()) {
            campaign.addUnit(entity, allowNewPilots);
        }
        //add any ejected pilots
        for(Pilot pilot : parser.getPilots()) {
            if(pilot.isEjected()) {
                campaign.addPilot(pilot, PilotPerson.T_MECH, false);
            }
        }
    }
    refreshUnitList();
    refreshPersonnelList();
    refreshReport();
}

protected void deployListFile() {
    if(UnitTable.getSelectedRow() == -1) {
        return;
    }
    
    ArrayList<Entity> chosen = new ArrayList<Entity>();
    ArrayList<Unit> toDeploy = new ArrayList<Unit>();
    StringBuffer undeployed = new StringBuffer();
    for(int i : UnitTable.getSelectedRows()) {
        Unit u = campaign.getUnits().get(i);
        if(null != u.getEntity()) {
            if(null == u.checkDeployment()) {
                chosen.add(u.getEntity());
                toDeploy.add(u);
            } else {
                undeployed.append("\n").append(u.getEntity().getDisplayName()).append(" (").append(u.checkDeployment()).append(")");
            }
        }
    }
    
    
    
    
    JFileChooser saveList = new JFileChooser(".");
    saveList.setDialogTitle("Deploy Units");
    saveList.setFileFilter(new FileFilter() {
        @Override
        public boolean accept(File dir) {
            return ((dir.getName() != null) && dir.getName().endsWith(".mul"));
        }

        @Override
        public String getDescription() {
            return "MUL file";
        }
    });
    saveList.setSelectedFile(new File(campaign.getName() + ".mul")); //$NON-NLS-1$
    int returnVal = saveList.showSaveDialog(mainPanel);
    if ((returnVal != JFileChooser.APPROVE_OPTION) || (saveList.getSelectedFile() == null)) {
       // I want a file, y'know!
       return;
    }
 
    File unitFile = saveList.getSelectedFile();
    if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(".mul") //$NON-NLS-1$
            || unitFile.getName().toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + ".mul"); //$NON-NLS-1$
                } catch (IOException ie) {
                    // nothing needs to be done here
                    return;
                }
            }
            try {
                // Save the player's entities to the file.
                //FIXME: this is not working
                EntityListFile.saveTo(unitFile, chosen);
                //set the unit and pilot as deployed
                for(Unit u: toDeploy) {
                    u.setDeployed(true);  
                }
                
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
            }
    }
    refreshUnitList();
    refreshPersonnelList();
    
    if(undeployed.length() > 0) {
        JOptionPane.showMessageDialog(this.getFrame(),"The following units could not be deployed:" + undeployed.toString(),"Could not deploy some units", JOptionPane.WARNING_MESSAGE);
    }
}
    
protected void refreshUnitList() {
    int selected = UnitTable.getSelectedRow();
    unitModel.setData(campaign.getUnits());
    if(selected > -1 && selected < campaign.getUnits().size()) {
        UnitTable.setRowSelectionInterval(selected, selected);
    }
}

protected void refreshTaskList() {
        taskModel.setData(campaign.getTasksForUnit(currentUnitId));
}

protected void refreshTechsList() {
    int selected = TechTable.getSelectedRow();
    techsModel.setData(campaign.getTechTeams());
    if(selected > -1 && selected < campaign.getTechTeams().size()) {
        TechTable.setRowSelectionInterval(selected, selected);
    }
}

protected void refreshDoctorsList() {
    int selected = DocTable.getSelectedRow();
    doctorsModel.setData(campaign.getDoctors());
    if(selected > -1 && selected < campaign.getDoctors().size()) {
        DocTable.setRowSelectionInterval(selected, selected);
    }
}

protected void refreshPersonnelList() {
    int selected = PersonTable.getSelectedRow();
    personnelModel.setData(campaign.getPersonnel());
    if(selected > -1 && selected < campaign.getPersonnel().size()) {
        PersonTable.setRowSelectionInterval(selected, selected);
    }
}

protected void refreshPartsList() {
    int selected = PartsTable.getSelectedRow();
    partsModel.setData(campaign.getParts());
    if(selected > -1 && selected < campaign.getParts().size()) {
        PartsTable.setRowSelectionInterval(selected, selected);
    }
}

protected void refreshCalendar() {
    getFrame().setTitle(campaign.getTitle());
}

protected void refreshReport() {
    txtPaneReport.setText(campaign.getCurrentReportHTML());
}

protected void updateAssignEnabled() {
    //must have a valid team and an unassigned task
    WorkItem curTask = campaign.getTask(currentTaskId);
    SupportTeam team = campaign.getTeam(currentTechId);
    if(null != curTask && null != team && team.getTargetFor(curTask).getValue() != TargetRoll.IMPOSSIBLE) {
        btnDoTask.setEnabled(true);
    } else {
        btnDoTask.setEnabled(false);
    }    
}

protected void updateAssignDoctorEnabled() {
    //must have a valid doctor and an unassigned task
    Person curPerson = campaign.getPerson(currentPersonId);
    SupportTeam team = campaign.getTeam(currentDoctorId);
    PersonnelWorkItem pw = null;
    if(null != curPerson) {
        pw = curPerson.getTask();
    }
    if(null != pw && null != team && !pw.isAssigned()
            && team.getTargetFor(pw).getValue() != TargetRoll.IMPOSSIBLE) {     
        btnAssignDoc.setEnabled(true);
    } else {
        btnAssignDoc.setEnabled(false);
    }    
}

protected void updateTargetText() {
    //must have a valid team and an unassigned task
    WorkItem task = campaign.getTask(currentTaskId);
    SupportTeam team = campaign.getTeam(currentTechId);
    if(null != task && null != team) {
        TargetRoll target = team.getTargetFor(task);
        textTarget.setText(target.getDesc());
        lblTargetNum.setText(target.getValueAsString());
    } else {
        textTarget.setText("");
        lblTargetNum.setText("-");
    }
}

/**
 * A table model for displaying work items
 */
public abstract class ArrayTableModel extends AbstractTableModel {
    
        protected String[] columnNames;
        protected ArrayList data;
        
        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
     
        //fill table with values
        public void setData(ArrayList array) {
            this.data = array;
            fireTableDataChanged();
        }
        
}

/**
 * A table model for displaying work items
 */
public class TaskTableModel extends ArrayTableModel {

        public TaskTableModel() {
            this.columnNames = new String[] {"Tasks"};
            this.data = new ArrayList<WorkItem>();
        }

        @Override
        public Object getValueAt(int row, int col) {
            return ((WorkItem)data.get(row)).getDescHTML();
        }  
        
        public WorkItem getTaskAt(int row) {
        return (WorkItem)data.get(row);
    }
        
        public TaskTableModel.Renderer getRenderer() {
        return new TaskTableModel.Renderer();
    }
    
    
    public class Renderer extends TaskInfo implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = this;
            WorkItem task = getTaskAt(row);
            setOpaque(true);          
            setText(getValueAt(row, column).toString());
            setToolTipText(task.getToolTip());
            if(isSelected) {
                select();
            } else {
                unselect();
            }
            
            if (null != task && task instanceof ReplacementItem && !((ReplacementItem)task).hasPart()) {
                    c.setBackground(Color.GRAY);
            } else {
                    c.setBackground(new Color(220, 220, 220));
            }
            return c;
        }
        
    }
}

public class TaskTableMouseAdapter extends MouseInputAdapter implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();
            WorkItem task = taskModel.getTaskAt(TaskTable.getSelectedRow());
            if (command.equalsIgnoreCase("REPLACE")) {
                if(task instanceof RepairItem) {
                    campaign.mutateTask(task, ((RepairItem)task).replace());
                } else if (task instanceof ReplacementItem) {
                    ((ReplacementItem)task).useUpPart();
                    task.setSkillMin(SupportTeam.EXP_GREEN);
                } else if (task instanceof SalvageItem) {
                    SalvageItem salvage = (SalvageItem)task;
                    salvage.scrap();
                }
                refreshTaskList();
            } else if (command.contains("SWAP_AMMO")) {
                if(task instanceof ReloadItem) {
                    ReloadItem reload = (ReloadItem)task;
                    Entity en = reload.getUnit().getEntity();
                    Mounted m = reload.getMounted();
                    if(null == m) {
                        return;
                    }
                    AmmoType curType = (AmmoType)m.getType();
                    String sel = command.split(":")[1];
                    int selType = Integer.parseInt(sel);
                    AmmoType newType = Utilities.getMunitionsFor(en, curType).get(selType);
                    reload.swapAmmo(newType);
                    refreshTaskList();
                }
            } else if (command.contains("CHANGE_MODE")) {
                String sel = command.split(":")[1];
                int selected = Integer.parseInt(sel);
                task.setMode(selected);
                refreshUnitList();
                refreshTaskList();
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        private void maybeShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                int row = TaskTable.rowAtPoint(e.getPoint());
                WorkItem task  = taskModel.getTaskAt(row);
                JMenuItem menuItem = null;
                JMenu menu = null;
                JCheckBoxMenuItem cbMenuItem = null;
                //**lets fill the pop up menu**//   
                if(task instanceof RepairItem 
                        || task instanceof ReplacementItem
                        || task instanceof SalvageItem) {
                    menu = new JMenu("Mode");
                    for(int i = 0; i < WorkItem.MODE_N; i++) {
                        cbMenuItem = new JCheckBoxMenuItem(WorkItem.getModeName(i));
                        if(task.getMode() == i) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.setActionCommand("CHANGE_MODE:" + i);
                            cbMenuItem.addActionListener(this);
                        }
                        menu.add(cbMenuItem);
                    }
                    popup.add(menu);
                    menuItem = new JMenuItem("Scrap component");
                    menuItem.setActionCommand("REPLACE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(((UnitWorkItem)task).canScrap());
                    popup.add(menuItem);
                }            
                if(task instanceof ReloadItem) {
                    ReloadItem reload = (ReloadItem)task;
                    Entity en = reload.getUnit().getEntity();
                    Mounted m = reload.getMounted();
                    menu = new JMenu("Swap Ammo");
                    int i = 0;
                    AmmoType curType = (AmmoType)m.getType();
                    for(AmmoType atype : Utilities.getMunitionsFor(en, curType)) {
                        cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
                        if(atype.equals(curType)) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.setActionCommand("SWAP_AMMO:" + i);
                            cbMenuItem.addActionListener(this);
                        }
                        menu.add(cbMenuItem);
                        i++;
                    }
                    popup.add(menu);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
}

/**
 * A table model for displaying units
 */
public class MekTableModel extends ArrayTableModel {

    public MekTableModel() {
        this.columnNames = new String[] {"Units"};
        this.data = new ArrayList<Unit>();
    }
  
    @Override
    public Object getValueAt(int row, int col) {
        return campaign.getUnitDesc(((Unit)data.get(row)).getId());
    }
    
    public Unit getUnitAt(int row) {
        return (Unit)data.get(row);
    }

    public MekTableModel.Renderer getRenderer() {
        return new MekTableModel.Renderer();
    }
    
    
    public class Renderer extends MekInfo implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = this;
            Unit u = getUnitAt(row);
            setOpaque(true);
            setUnit(u.getEntity());
            setText(getValueAt(row, column).toString());
            setToolTipText(campaign.getToolTipFor(u));
            if(isSelected) {
                select();
            } else {
                unselect();
            }
            
            if (null != u && u.isDeployed()) {
                c.setBackground(Color.GRAY);
            }
            else if(null != u && !u.isRepairable()) {
                    c.setBackground(new Color(190, 150, 55));
            }
            else if(null != u && !u.isFunctional()) {
                    c.setBackground(new Color(205,92,92));
            }
            else if(null != u && campaign.countTasksFor(u.getId()) > 0) {
                    c.setBackground(new Color(238, 238, 0));
            } 
            else {
                    c.setBackground(new Color(220, 220, 220));
            }
            return c;
        }
        
    }
}

public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {

        private ArrayList<PilotPerson> pilots;
    
        @Override
        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();
            Unit unit = unitModel.getUnitAt(UnitTable.getSelectedRow());
            if (command.equalsIgnoreCase("REMOVE_PILOT")) {
                unit.removePilot();
            } else if(command.contains("CHANGE_PILOT")) {
                String sel = command.split(":")[1];
                int selected = Integer.parseInt(sel);
                if(null != pilots && selected > -1 && selected < pilots.size()) {
                    campaign.changePilot(unit, pilots.get(selected));
                }
            } else if(command.equalsIgnoreCase("SELL")) {
                if(0 == JOptionPane.showConfirmDialog(null, "Do you really want to sell " + unit.getEntity().getDisplayName(), "Sell Unit?", JOptionPane.YES_NO_OPTION)) {
                    campaign.removeUnit(unit.getId());
                }
            } else if(command.equalsIgnoreCase("LOSS")) {
                if(0 == JOptionPane.showConfirmDialog(null, "Do you really want to consider " + unit.getEntity().getDisplayName() + " a combat loss?", "Remove Unit?", JOptionPane.YES_NO_OPTION)) {
                    campaign.removeUnit(unit.getId());
                }
            } else if(command.contains("ASSIGN_TECH")) {
                String sel = command.split(":")[1];
                int selected = Integer.parseInt(sel);
                if(selected > -1 && selected < campaign.getTechTeams().size()) {
                    SupportTeam team = campaign.getTechTeams().get(selected);
                    if(null != team) {
                        for(WorkItem task : campaign.getTasksForUnit(unit.getId())) {
                            if(team.getTargetFor(task).getValue() != TargetRoll.IMPOSSIBLE) {
                                campaign.processTask(task, team);
                            }
                        }
                    }
                }
                refreshUnitList();
                refreshTaskList();
                refreshTechsList();
                refreshReport();
            } else if(command.contains("SWAP_AMMO")) {
                String sel = command.split(":")[1];
                int selMount = Integer.parseInt(sel);
                Mounted m = unit.getEntity().getEquipment(selMount);
                if(null == m) {
                    return;
                }
                AmmoType curType = (AmmoType)m.getType();
                ReloadItem reload = campaign.getReloadWorkFor(m, unit);
                boolean newWork = false;
                if(null == reload) {
                    newWork = true;
                    reload = new ReloadItem(unit, m);
                }
                sel = command.split(":")[2];
                int selType = Integer.parseInt(sel);
                AmmoType newType = Utilities.getMunitionsFor(unit.getEntity(), curType).get(selType);
                reload.swapAmmo(newType);
                if(newWork) {
                    campaign.addWork(reload);
                }
                refreshTaskList();
                refreshUnitList();
            } else if(command.contains("CHANGE_SITE")) {
                String sel = command.split(":")[1];
                int selected = Integer.parseInt(sel);
                if(selected > -1 && selected < Unit.SITE_N) {
                    unit.setSite(selected);
                    refreshUnitList();
                    refreshTaskList();
                }
            } else if (command.equalsIgnoreCase("SALVAGE")) {
                unit.setSalvage(true);
                refreshUnitList();
            } else if (command.equalsIgnoreCase("REPAIR")) {
                unit.setSalvage(false);
                refreshUnitList();
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {

            if (e.getClickCount() == 2) {
                int row = UnitTable.rowAtPoint(e.getPoint());
                Unit unit = unitModel.getUnitAt(row);
                if(null != unit) {
                    MechView mv = new MechView(unit.getEntity(), false);
                    MekViewDialog mvd = new MekViewDialog(null, true, mv);
                    mvd.setVisible(true);
                }
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        
        private void maybeShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                int row = UnitTable.rowAtPoint(e.getPoint());
                Unit unit  = unitModel.getUnitAt(row);
                pilots = campaign.getEligiblePilotsFor(unit);
                JMenuItem menuItem = null;
                JMenu menu = null;
                JCheckBoxMenuItem cbMenuItem = null;
                //**lets fill the pop up menu**//    
                //change the location
                menu = new JMenu("Change site");
                int i = 0;
                for(i = 0; i < Unit.SITE_N; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
                    if(unit.getSite() == i) {
                            cbMenuItem.setSelected(true);
                    } else {
                        cbMenuItem.setActionCommand("CHANGE_SITE:" + i);
                        cbMenuItem.addActionListener(this);
                    }
                    menu.add(cbMenuItem);
                }
                menu.setEnabled(!unit.isDeployed());
                popup.add(menu);
                //assign all tasks to a certain tech
                menu = new JMenu("Assign all tasks");
                i = 0;
                for(SupportTeam tech : campaign.getTechTeams()) {
                    menuItem = new JMenuItem(tech.getDesc());
                    menuItem.setActionCommand("ASSIGN_TECH:" + i);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(tech.getMinutesLeft() > 0);
                    menu.add(menuItem);
                    i++;
                }
                menu.setEnabled(!unit.isDeployed());
                popup.add(menu);
                //swap ammo
                menu = new JMenu("Swap ammo");
                JMenu ammoMenu = null;
                for(Mounted m : unit.getEntity().getAmmo()) {
                    ammoMenu = new JMenu(m.getDesc());
                    i = 0;
                    AmmoType curType = (AmmoType)m.getType();
                    for(AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(), curType)) {
                        cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
                        if(atype.equals(curType)) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.setActionCommand("SWAP_AMMO:" + unit.getEntity().getEquipmentNum(m) + ":" + i);
                            cbMenuItem.addActionListener(this);
                        }
                        ammoMenu.add(cbMenuItem);
                        i++;
                    }
                    menu.add(ammoMenu);
                }
                menu.setEnabled(!unit.isDeployed());
                popup.add(menu);
                if(unit.isSalvage()) {
                    menuItem = new JMenuItem("Repair");
                    menuItem.setActionCommand("REPAIR");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(!unit.isDeployed() && unit.isRepairable());
                    popup.add(menuItem);
                } else {
                    menuItem = new JMenuItem("Salvage");
                    menuItem.setActionCommand("SALVAGE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(!unit.isDeployed());
                    popup.add(menuItem);
                }
                //remove pilot
                popup.addSeparator();
                menuItem = new JMenuItem("Remove pilot");
                menuItem.setActionCommand("REMOVE_PILOT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.hasPilot() && !unit.isDeployed());
                popup.add(menuItem);
                //switch pilot
                menu = new JMenu("Change pilot");              
                i = 0;
                for(PilotPerson pp : pilots) {
                    cbMenuItem = new JCheckBoxMenuItem(pp.getDesc());
                    if(unit.hasPilot()
                            && unit.getPilot().getId() == pp.getId()) {
                        cbMenuItem.setSelected(true);
                    }
                    cbMenuItem.setActionCommand("CHANGE_PILOT:" + i);
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    i++;
                }
                menu.setEnabled(!unit.isDeployed());
                popup.add(menu);
                popup.addSeparator();
                //sell unit
                menuItem = new JMenuItem("Sell Unit");
                menuItem.setActionCommand("SELL");
                menuItem.addActionListener(this);
                menuItem.setEnabled(!unit.isDeployed());
                popup.add(menuItem);
                //TODO: scrap unit
                //combat loss
                menuItem = new JMenuItem("Combat Loss");
                menuItem.setActionCommand("LOSS");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isDeployed());
                popup.add(menuItem);
                //TODO: add quirks?
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
}

/**
 * A table model for displaying work items
 */
public class TechTableModel extends ArrayTableModel {

        public TechTableModel() {
            this.columnNames = new String[] {"Techs"};
            this.data = new ArrayList<TechTeam>();
        }

        @Override
        public Object getValueAt(int row, int col) {
            return ((TechTeam)data.get(row)).getDescHTML();
        }  
        
        public TechTeam getTechAt(int row) {
        return (TechTeam)data.get(row);
    }
        
        public TechTableModel.Renderer getRenderer() {
        return new TechTableModel.Renderer();
    }
    
    
    public class Renderer extends TechInfo implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = this;
            setOpaque(true);          
            setText(getValueAt(row, column).toString());
            //setToolTipText(campaign.getToolTipFor(u));
            if(isSelected) {
                select();
            } else {
                unselect();
            }
            c.setBackground(new Color(220, 220, 220));
            return c;
        }
        
    }
}

/**
 * A table model for displaying personnel
 */
public class PersonTableModel extends ArrayTableModel {

        public PersonTableModel() {
            this.columnNames = new String[] {"Personnel"};
            this.data = new ArrayList<Person>();
        }

        @Override
        public Object getValueAt(int row, int col) {
            return ((Person)data.get(row)).getDescHTML();
        }  
        
        public Person getPersonAt(int row) {
        return (Person)data.get(row);
    }
        
        public PersonTableModel.Renderer getRenderer() {
        return new PersonTableModel.Renderer();
    }
    
    
    public class Renderer extends PersonInfo implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = this;
            setOpaque(true);          
            setText(getValueAt(row, column).toString());
            Person p = getPersonAt(row);
            //setToolTipText(campaign.getToolTipFor(u));
            if(isSelected) {
               select();
            } else {
               unselect();
            }
            if (null != p && p.isDeployed()) {
                    c.setBackground(Color.GRAY);
            } 
            else if(null != p && null != p.getTask()) {
                    c.setBackground(new Color(205,92,92));
            } else {
                    c.setBackground(new Color(220, 220, 220));
            }
            return c;
        }
        
    }
}

/**
 * A table model for displaying personnel
 */
public class DocTableModel extends ArrayTableModel {

        public DocTableModel() {
            this.columnNames = new String[] {"Doctors"};
            this.data = new ArrayList<MedicalTeam>();
        }

        @Override
        public Object getValueAt(int row, int col) {
            return ((MedicalTeam)data.get(row)).getDescHTML();
        }  
        
        public MedicalTeam getDoctorAt(int row) {
        return (MedicalTeam)data.get(row);
    }
        
        public DocTableModel.Renderer getRenderer() {
        return new DocTableModel.Renderer();
    }
    
    
    public class Renderer extends DoctorInfo implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = this;
            setOpaque(true);          
            setText(getValueAt(row, column).toString());
            setToolTipText(campaign.getToolTipFor(getDoctorAt(row)));
            if(isSelected) {
                select();
            } else {
                unselect();
            }
            c.setBackground(new Color(220, 220, 220));
            return c;
        }
        
    }
}

/**
 * A table model for displaying parts
 */
public class PartsTableModel extends ArrayTableModel {

        public PartsTableModel() {
            this.columnNames = new String[] {"Parts"};
            this.data = new ArrayList<Part>();
        }

        @Override
        public Object getValueAt(int row, int col) {
            return ((Part)data.get(row)).getDescHTML();
        }  
        
        public Part getPersonAt(int row) {
        return (Part)data.get(row);
    }
        
        public PartsTableModel.Renderer getRenderer() {
        return new PartsTableModel.Renderer();
    }
    
    
    public class Renderer extends PartInfo implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = this;
            setOpaque(true);          
            setText(getValueAt(row, column).toString());
            //setToolTipText(campaign.getToolTipFor(u));
            if(isSelected) {
                select();
            } else {
                unselect();
            }
            c.setBackground(new Color(220, 220, 220));
            return c;
        }
        
    }
}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable DocTable;
    private javax.swing.JTable PartsTable;
    private javax.swing.JTable PersonTable;
    private javax.swing.JTable TaskTable;
    private javax.swing.JTable TechTable;
    private javax.swing.JTable UnitTable;
    private javax.swing.JButton btnAdvanceDay;
    private javax.swing.JButton btnAssignDoc;
    private javax.swing.JButton btnDeployUnits;
    private javax.swing.JButton btnDoTask;
    private javax.swing.JToggleButton btnGMMode;
    private javax.swing.JToggleButton btnOvertime;
    private javax.swing.JButton btnRetrieveUnits;
    private javax.swing.JPanel btnUnitPanel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JLabel lblTarget;
    private javax.swing.JLabel lblTargetNum;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu menuHire;
    private javax.swing.JMenuItem menuLoad;
    private javax.swing.JMenu menuMarket;
    private javax.swing.JMenuItem menuOptions;
    private javax.swing.JMenuItem menuSave;
    private javax.swing.JMenuItem miHireDoctor;
    private javax.swing.JMenuItem miHirePilot;
    private javax.swing.JMenuItem miHireTech;
    private javax.swing.JMenu miLoadForces;
    private javax.swing.JMenuItem miPurchaseUnit;
    private javax.swing.JPanel panFinances;
    private javax.swing.JPanel panHangar;
    private javax.swing.JPanel panPersonnel;
    private javax.swing.JPanel panSupplies;
    private javax.swing.JPanel panelDoTask;
    private javax.swing.JPanel panelMasterButtons;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JScrollPane scrollDocTable;
    private javax.swing.JScrollPane scrollPersonTable;
    private javax.swing.JScrollPane scrollTaskTable;
    private javax.swing.JScrollPane scrollTechTable;
    private javax.swing.JScrollPane scrollUnitTable;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTabbedPane tabMain;
    private javax.swing.JTextArea textTarget;
    private javax.swing.JTextPane txtPaneReport;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}

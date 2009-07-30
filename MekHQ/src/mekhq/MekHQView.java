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

import java.awt.Color;
import java.awt.Component;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Vector;
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
import megamek.common.TargetRoll;
import mekhq.campaign.Unit;
import mekhq.campaign.Utilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.TechTeam;
import mekhq.campaign.work.PersonnelWorkItem;
import mekhq.campaign.work.ReloadItem;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.ReplacementItem;
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
    private MekTableMouseAdapter unitMouseAdapter;
    private TaskTableMouseAdapter taskMouseAdapter;
    private int currentUnitId;
    private int currentTaskId;
    private int currentTechId;
    private int currentPersonId;
    private int currentDoctorId;
    
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

        mainPanel = new javax.swing.JPanel();
        btnAdvanceDay = new javax.swing.JButton();
        tabMain = new javax.swing.JTabbedPane();
        panHangar = new javax.swing.JPanel();
        btnDeployUnits = new javax.swing.JButton();
        loadListBtn = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        btnDoTask = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        TaskTable = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        UnitTable = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        textTarget = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        TechTable = new javax.swing.JTable();
        lblTarget = new javax.swing.JLabel();
        lblTargetNum = new javax.swing.JLabel();
        panSupplies = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        panPersonnel = new javax.swing.JPanel();
        btnAssignDoc = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        PersonTable = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        DocTable = new javax.swing.JTable();
        panFinances = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        txtPaneReport = new javax.swing.JTextPane();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        menuLoad = new javax.swing.JMenuItem();
        menuSave = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
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

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(MekHQView.class);
        btnAdvanceDay.setText(resourceMap.getString("btnAdvanceDay.text")); // NOI18N
        btnAdvanceDay.setToolTipText(resourceMap.getString("btnAdvanceDay.toolTipText")); // NOI18N
        btnAdvanceDay.setName("btnAdvanceDay"); // NOI18N
        btnAdvanceDay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAdvanceDayActionPerformed(evt);
            }
        });

        tabMain.setToolTipText(resourceMap.getString("tabMain.toolTipText")); // NOI18N
        tabMain.setName("tabMain"); // NOI18N

        panHangar.setFont(resourceMap.getFont("panHangar.font")); // NOI18N
        panHangar.setName("panHangar"); // NOI18N

        btnDeployUnits.setText(resourceMap.getString("btnDeployUnits.text")); // NOI18N
        btnDeployUnits.setToolTipText(resourceMap.getString("btnDeployUnits.toolTipText")); // NOI18N
        btnDeployUnits.setName("btnDeployUnits"); // NOI18N
        btnDeployUnits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeployUnitsActionPerformed(evt);
            }
        });

        loadListBtn.setText(resourceMap.getString("loadListBtn.text")); // NOI18N
        loadListBtn.setToolTipText(resourceMap.getString("loadListBtn.toolTipText")); // NOI18N
        loadListBtn.setName("loadListBtn"); // NOI18N
        loadListBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadListBtnActionPerformed(evt);
            }
        });

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        btnDoTask.setText(resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTask.setEnabled(false);
        btnDoTask.setName("btnDoTask"); // NOI18N
        btnDoTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDoTaskActionPerformed(evt);
            }
        });

        jScrollPane4.setName("jScrollPane4"); // NOI18N

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
        jScrollPane4.setViewportView(TaskTable);

        jScrollPane5.setName("jScrollPane5"); // NOI18N

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
        jScrollPane5.setViewportView(UnitTable);

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        textTarget.setBackground(resourceMap.getColor("textTarget.background")); // NOI18N
        textTarget.setColumns(20);
        textTarget.setEditable(false);
        textTarget.setFont(resourceMap.getFont("textTarget.font")); // NOI18N
        textTarget.setLineWrap(true);
        textTarget.setRows(5);
        textTarget.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTarget.setBorder(null);
        textTarget.setName("textTarget"); // NOI18N
        jScrollPane6.setViewportView(textTarget);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        TechTable.setModel(techsModel);
        TechTable.setName("TechTable"); // NOI18N
        TechTable.setRowHeight(60);
        TechTable.getColumnModel().getColumn(0).setCellRenderer(techsModel.getRenderer());
        TechTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                TechTableValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(TechTable);

        lblTarget.setText(resourceMap.getString("lblTarget.text")); // NOI18N
        lblTarget.setName("lblTarget"); // NOI18N

        lblTargetNum.setFont(resourceMap.getFont("lblTargetNum.font")); // NOI18N
        lblTargetNum.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTargetNum.setText(resourceMap.getString("lblTargetNum.text")); // NOI18N
        lblTargetNum.setName("lblTargetNum"); // NOI18N

        org.jdesktop.layout.GroupLayout panHangarLayout = new org.jdesktop.layout.GroupLayout(panHangar);
        panHangar.setLayout(panHangarLayout);
        panHangarLayout.setHorizontalGroup(
            panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panHangarLayout.createSequentialGroup()
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panHangarLayout.createSequentialGroup()
                        .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 364, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(panHangarLayout.createSequentialGroup()
                                .add(19, 19, 19)
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(btnDoTask)
                                    .add(panHangarLayout.createSequentialGroup()
                                        .add(24, 24, 24)
                                        .add(lblTarget))
                                    .add(panHangarLayout.createSequentialGroup()
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(lblTargetNum, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 95, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jScrollPane6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(panHangarLayout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 299, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(panHangarLayout.createSequentialGroup()
                        .add(loadListBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btnDeployUnits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        panHangarLayout.setVerticalGroup(
            panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, panHangarLayout.createSequentialGroup()
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(panHangarLayout.createSequentialGroup()
                        .add(47, 47, 47)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE))
                    .add(panHangarLayout.createSequentialGroup()
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(loadListBtn)
                            .add(btnDeployUnits))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(panHangarLayout.createSequentialGroup()
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(panHangarLayout.createSequentialGroup()
                                        .add(btnDoTask)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(lblTarget)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(lblTargetNum))
                                    .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                .addContainerGap())
        );

        tabMain.addTab(resourceMap.getString("panHangar.TabConstraints.tabTitle"), panHangar); // NOI18N

        panSupplies.setName("panSupplies"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        org.jdesktop.layout.GroupLayout panSuppliesLayout = new org.jdesktop.layout.GroupLayout(panSupplies);
        panSupplies.setLayout(panSuppliesLayout);
        panSuppliesLayout.setHorizontalGroup(
            panSuppliesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSuppliesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addContainerGap(852, Short.MAX_VALUE))
        );
        panSuppliesLayout.setVerticalGroup(
            panSuppliesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSuppliesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addContainerGap(490, Short.MAX_VALUE))
        );

        tabMain.addTab(resourceMap.getString("panSupplies.TabConstraints.tabTitle"), panSupplies); // NOI18N

        panPersonnel.setName("panPersonnel"); // NOI18N

        btnAssignDoc.setText(resourceMap.getString("btnAssignDoc.text")); // NOI18N
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.setName("btnAssignDoc"); // NOI18N
        btnAssignDoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAssignDocActionPerformed(evt);
            }
        });

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        PersonTable.setModel(personnelModel);
        PersonTable.setName("PersonTable"); // NOI18N
        PersonTable.setRowHeight(60);
        PersonTable.getColumnModel().getColumn(0).setCellRenderer(personnelModel.getRenderer());
        PersonTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                PersonTableValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(PersonTable);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        DocTable.setModel(doctorsModel);
        DocTable.setName("DocTable"); // NOI18N
        DocTable.setRowHeight(60);
        DocTable.getColumnModel().getColumn(0).setCellRenderer(doctorsModel.getRenderer());
        DocTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                DocTableValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(DocTable);

        org.jdesktop.layout.GroupLayout panPersonnelLayout = new org.jdesktop.layout.GroupLayout(panPersonnel);
        panPersonnel.setLayout(panPersonnelLayout);
        panPersonnelLayout.setHorizontalGroup(
            panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panPersonnelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 356, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btnAssignDoc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 322, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(165, 165, 165))
        );
        panPersonnelLayout.setVerticalGroup(
            panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panPersonnelLayout.createSequentialGroup()
                .addContainerGap()
                .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, btnAssignDoc)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE))
                .addContainerGap())
        );

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
                .addContainerGap(852, Short.MAX_VALUE))
        );
        panFinancesLayout.setVerticalGroup(
            panFinancesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panFinancesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addContainerGap(490, Short.MAX_VALUE))
        );

        tabMain.addTab(resourceMap.getString("panFinances.TabConstraints.tabTitle"), panFinances); // NOI18N

        lblDate.setText(resourceMap.getString("lblDate.text")); // NOI18N
        lblDate.setName("lblDate"); // NOI18N

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        txtPaneReport.setContentType(resourceMap.getString("txtPaneReport.contentType")); // NOI18N
        txtPaneReport.setEditable(false);
        txtPaneReport.setFont(resourceMap.getFont("txtPaneReport.font")); // NOI18N
        txtPaneReport.setText(campaign.getCurrentReportHTML());
        txtPaneReport.setName("txtPaneReport"); // NOI18N
        jScrollPane7.setViewportView(txtPaneReport);

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(tabMain, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1058, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(mainPanelLayout.createSequentialGroup()
                        .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(lblDate, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 192, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnAdvanceDay))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 836, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(mainPanelLayout.createSequentialGroup()
                        .add(lblDate)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btnAdvanceDay))
                    .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 172, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(13, 13, 13)
                .add(tabMain, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 572, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getActionMap(MekHQView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        menuMarket.setText(resourceMap.getString("menuMarket.text")); // NOI18N
        menuMarket.setName("menuMarket"); // NOI18N

        miPurchaseUnit.setText(resourceMap.getString("miPurchaseUnit.text")); // NOI18N
        miPurchaseUnit.setEnabled(false);
        miPurchaseUnit.setName("miPurchaseUnit"); // NOI18N
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
                .add(1076, 1076, 1076)
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

private void loadListBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadListBtnActionPerformed
    loadListFile();
}//GEN-LAST:event_loadListBtnActionPerformed

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
    else if(selected < 0) {
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
    saveListFile();
}//GEN-LAST:event_btnDeployUnitsActionPerformed

private void btnAssignDocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAssignDocActionPerformed
    if(currentPersonId == -1) {
        return;
    }
    int row = PersonTable.getSelectedRow();
    Person p = campaign.getPerson(currentPersonId);
    if(null != p && null != p.getTask()) {
        campaign.assignTask(currentDoctorId, p.getTask().getId());
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
    NewPilotDialog npd = new NewPilotDialog(this.getFrame(), true, campaign);
    npd.setVisible(true);
    refreshPersonnelList();
}//GEN-LAST:event_miHirePilotActionPerformed

private void miHireTechActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHireTechActionPerformed
    NewTechTeamDialog ntd = new NewTechTeamDialog(this.getFrame(), true, campaign);
    ntd.setVisible(true);
    refreshTechsList();
    refreshPersonnelList();
}//GEN-LAST:event_miHireTechActionPerformed

private void miHireDoctorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHireDoctorActionPerformed
    NewMedicalTeamDialog nmd = new NewMedicalTeamDialog(this.getFrame(), true, campaign);
    nmd.setVisible(true);
    refreshTechsList();
    refreshPersonnelList();
    refreshDoctorsList();
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
    saveCpgn.setSelectedFile(new File("mycampaign.cpn")); //$NON-NLS-1$
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
    refreshCalendar();
    refreshReport();
}//GEN-LAST:event_menuLoadActionPerformed

protected void loadListFile() {
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
       try {
           // Read the units from the file.
           Vector<Entity> loadedUnits = EntityListFile.loadFrom(unitFile);
           // Add the units from the file.
           for (Entity entity : loadedUnits) {
              campaign.addUnit(entity);
           }
        } catch (IOException excep) {
            excep.printStackTrace(System.err);
        }
    }
    refreshUnitList();
    refreshPersonnelList();
}

protected void saveListFile() {
    if(UnitTable.getSelectedRow() == -1) {
        return;
    }
    
    ArrayList<Entity> chosen = new ArrayList<Entity>();
    ArrayList<Integer> toRemove = new ArrayList<Integer>();
    for(int i : UnitTable.getSelectedRows()) {
        Unit u = campaign.getUnits().get(i);
        if(u.hasPilot() && null != u.getEntity()) {
            chosen.add(u.getEntity());
            toRemove.add(u.getId());
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
    saveList.setSelectedFile(new File("myunits.mul")); //$NON-NLS-1$
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
                //clear the entities, so that if the user wants to read them back you wont get duplicates
                //The removeUnit method will also remove tasks and pilots associated with this unit
                for(int id: toRemove) {
                    campaign.removeUnit(id, true);
                }
                
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
            }
    }
    refreshUnitList();
    refreshPersonnelList();
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

protected void refreshCalendar() {
    lblDate.setText(campaign.getDateAsString());
    
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
            setOpaque(true);          
            setText(getValueAt(row, column).toString());
            //setToolTipText(campaign.getToolTipFor(u));
            if(isSelected) {
                //TODO: how do I set this to the user's default selection color?
                c.setBackground(new Color(253, 117, 28));
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
                    //TODO: destroy part once parts are implemented
                    task.setSkillMin(SupportTeam.EXP_GREEN);
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
                if(task instanceof RepairItem || task instanceof ReplacementItem) {
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
                }            
                menuItem = new JMenuItem("Scrap component");
                menuItem.setActionCommand("REPLACE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(task instanceof RepairItem || task instanceof ReplacementItem);              
                popup.add(menuItem);
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
                //TODO: how do I set this to the user's default selection color?
                c.setBackground(new Color(253, 117, 28));
            } else {
                if(null != u && campaign.countTasksFor(u.getId()) > 0) {
                    c.setBackground(Color.YELLOW);
                } else {
                    c.setBackground(new Color(220, 220, 220));
                }
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
                campaign.removePilotFrom(unit);
            } else if(command.contains("CHANGE_PILOT")) {
                String sel = command.split(":")[1];
                int selected = Integer.parseInt(sel);
                if(null != pilots && selected > -1 && selected < pilots.size()) {
                    campaign.changePilot(pilots.get(selected), unit);
                }
            } else if(command.equalsIgnoreCase("SELL")) {
                if(0 == JOptionPane.showConfirmDialog(null, "Do you really want to sell " + unit.getEntity().getDisplayName(), "Sell Unit?", JOptionPane.YES_NO_OPTION)) {
                    campaign.removeUnit(unit.getId(), false);
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
                popup.add(menu);
                //remove pilot
                popup.addSeparator();
                menuItem = new JMenuItem("Remove pilot");
                menuItem.setActionCommand("REMOVE_PILOT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.hasPilot());
                popup.add(menuItem);
                //switch pilot
                menu = new JMenu("Change pilot");              
                i = 0;
                for(PilotPerson pp : pilots) {
                    cbMenuItem = new JCheckBoxMenuItem(pp.getDesc());
                    if(null != unit.getEntity().getCrew()
                            && unit.getEntity().getCrew().equals(pp.getPilot())) {
                        cbMenuItem.setSelected(true);
                    }
                    cbMenuItem.setActionCommand("CHANGE_PILOT:" + i);
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    i++;
                }
                popup.add(menu);
                popup.addSeparator();
                //sell unit
                menuItem = new JMenuItem("Sell Unit");
                menuItem.setActionCommand("SELL");
                menuItem.addActionListener(this);
                popup.add(menuItem);
                //TODO: scrap unit
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
                //TODO: how do I set this to the user's default selection color?
                c.setBackground(new Color(253, 117, 28));
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
                //TODO: how do I set this to the user's default selection color?
                c.setBackground(new Color(253, 117, 28));
            } else {
                if(null != p && null != p.getTask() && !p.getTask().isAssigned()) {
                    c.setBackground(Color.RED);
                } else {
                    c.setBackground(new Color(220, 220, 220));
                }
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
                //TODO: how do I set this to the user's default selection color?
                c.setBackground(new Color(253, 117, 28));
            } else {
                c.setBackground(new Color(220, 220, 220));
            }
            return c;
        }
        
    }
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable DocTable;
    private javax.swing.JTable PersonTable;
    private javax.swing.JTable TaskTable;
    private javax.swing.JTable TechTable;
    private javax.swing.JTable UnitTable;
    private javax.swing.JButton btnAdvanceDay;
    private javax.swing.JButton btnAssignDoc;
    private javax.swing.JButton btnDeployUnits;
    private javax.swing.JButton btnDoTask;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblTarget;
    private javax.swing.JLabel lblTargetNum;
    private javax.swing.JButton loadListBtn;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu menuHire;
    private javax.swing.JMenuItem menuLoad;
    private javax.swing.JMenu menuMarket;
    private javax.swing.JMenuItem menuSave;
    private javax.swing.JMenuItem miHireDoctor;
    private javax.swing.JMenuItem miHirePilot;
    private javax.swing.JMenuItem miHireTech;
    private javax.swing.JMenuItem miPurchaseUnit;
    private javax.swing.JPanel panFinances;
    private javax.swing.JPanel panHangar;
    private javax.swing.JPanel panPersonnel;
    private javax.swing.JPanel panSupplies;
    private javax.swing.JProgressBar progressBar;
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

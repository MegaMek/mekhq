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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import megamek.client.ui.MechView;
import megamek.client.ui.swing.MechTileset;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.work.ReloadItem;
import mekhq.campaign.work.RepairItem;
import mekhq.campaign.work.WorkItem;

/**
 * The application's main frame.
 */
public class MekHQView extends FrameView {

    
    private Campaign campaign = new Campaign();
    private DefaultListModel unitsModel = new DefaultListModel();
    private DefaultListModel techsModel = new DefaultListModel();
    private DefaultListModel personnelModel = new DefaultListModel();
    private DefaultListModel doctorsModel = new DefaultListModel();
    private TaskTableModel taskModel = new TaskTableModel();
    private MekTableModel unitModel = new MekTableModel();
    private MekTableMouseAdapter unitMouseAdapter;
    private int currentUnitId;
    private int currentTaskId;
    private int currentTechId;
    private int currentPersonId;
    private int currentDoctorId;
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
    
    public MekHQView(SingleFrameApplication app) {
        super(app);
      
        unitMouseAdapter = new MekTableMouseAdapter();
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
        panFinances = new javax.swing.JTabbedPane();
        panHangar = new javax.swing.JPanel();
        lblTasks = new javax.swing.JLabel();
        btnDeployUnits = new javax.swing.JButton();
        loadListBtn = new javax.swing.JButton();
        ammoBtn = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        replaceBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        TeamsList = new javax.swing.JList();
        btnDoTask = new javax.swing.JButton();
        btnChangePilot = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        TaskTable = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        UnitTable = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        textTarget = new javax.swing.JTextArea();
        panSupplies = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        panPersonnel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        personnelList = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        DoctorsList = new javax.swing.JList();
        btnAssignDoc = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
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

        panFinances.setToolTipText(resourceMap.getString("panFinances.toolTipText")); // NOI18N
        panFinances.setName("panFinances"); // NOI18N

        panHangar.setName("panHangar"); // NOI18N

        lblTasks.setText(resourceMap.getString("lblTasks.text")); // NOI18N
        lblTasks.setName("lblTasks"); // NOI18N

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

        ammoBtn.setText(resourceMap.getString("ammoBtn.text")); // NOI18N
        ammoBtn.setToolTipText(resourceMap.getString("ammoBtn.toolTipText")); // NOI18N
        ammoBtn.setEnabled(false);
        ammoBtn.setName("ammoBtn"); // NOI18N
        ammoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ammoBtnActionPerformed(evt);
            }
        });

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        replaceBtn.setFont(resourceMap.getFont("replaceBtn.font")); // NOI18N
        replaceBtn.setText(resourceMap.getString("replaceBtn.text")); // NOI18N
        replaceBtn.setToolTipText(resourceMap.getString("replaceBtn.toolTipText")); // NOI18N
        replaceBtn.setEnabled(false);
        replaceBtn.setName("replaceBtn"); // NOI18N
        replaceBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceBtnActionPerformed(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        TeamsList.setModel(techsModel);
        TeamsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        TeamsList.setName("TeamsList"); // NOI18N
        TeamsList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                TeamsListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(TeamsList);

        btnDoTask.setText(resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTask.setEnabled(false);
        btnDoTask.setName("btnDoTask"); // NOI18N
        btnDoTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDoTaskActionPerformed(evt);
            }
        });

        btnChangePilot.setText(resourceMap.getString("btnChangePilot.text")); // NOI18N
        btnChangePilot.setName("btnChangePilot"); // NOI18N
        btnChangePilot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChangePilotActionPerformed(evt);
            }
        });

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        TaskTable.setModel(taskModel);
        TaskTable.setName("TaskTable"); // NOI18N
        TaskTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                TaskTableValueChanged(evt);
            }
        });
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
        textTarget.setRows(5);
        textTarget.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTarget.setBorder(null);
        textTarget.setName("textTarget"); // NOI18N
        jScrollPane6.setViewportView(textTarget);

        org.jdesktop.layout.GroupLayout panHangarLayout = new org.jdesktop.layout.GroupLayout(panHangar);
        panHangar.setLayout(panHangarLayout);
        panHangarLayout.setHorizontalGroup(
            panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, panHangarLayout.createSequentialGroup()
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(btnDeployUnits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(btnChangePilot, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(loadListBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 393, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panHangarLayout.createSequentialGroup()
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, panHangarLayout.createSequentialGroup()
                                .add(lblTasks, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 197, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(485, 485, 485))
                            .add(panHangarLayout.createSequentialGroup()
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(replaceBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                                    .add(ammoBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE))))
                        .add(286, 286, 286)
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, 0))
                    .add(panHangarLayout.createSequentialGroup()
                        .add(btnDoTask)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        panHangarLayout.setVerticalGroup(
            panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panHangarLayout.createSequentialGroup()
                .add(20, 20, 20)
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, panHangarLayout.createSequentialGroup()
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(panHangarLayout.createSequentialGroup()
                                .add(lblTasks)
                                .add(8, 8, 8)
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(panHangarLayout.createSequentialGroup()
                                        .add(ammoBtn)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(replaceBtn))
                                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 289, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(btnDoTask)))
                            .add(panHangarLayout.createSequentialGroup()
                                .add(loadListBtn)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnDeployUnits)
                                .add(18, 18, 18)
                                .add(btnChangePilot)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 232, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        panFinances.addTab(resourceMap.getString("panHangar.TabConstraints.tabTitle"), panHangar); // NOI18N

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
                .addContainerGap(1073, Short.MAX_VALUE))
        );
        panSuppliesLayout.setVerticalGroup(
            panSuppliesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSuppliesLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addContainerGap(645, Short.MAX_VALUE))
        );

        panFinances.addTab(resourceMap.getString("panSupplies.TabConstraints.tabTitle"), panSupplies); // NOI18N

        panPersonnel.setName("panPersonnel"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        personnelList.setModel(personnelModel);
        personnelList.setName("personnelList"); // NOI18N
        personnelList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                personnelListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(personnelList);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        DoctorsList.setModel(doctorsModel);
        DoctorsList.setName("DoctorsList"); // NOI18N
        DoctorsList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                DoctorsListValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(DoctorsList);

        btnAssignDoc.setText(resourceMap.getString("btnAssignDoc.text")); // NOI18N
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.setName("btnAssignDoc"); // NOI18N
        btnAssignDoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAssignDocActionPerformed(evt);
            }
        });

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        org.jdesktop.layout.GroupLayout panPersonnelLayout = new org.jdesktop.layout.GroupLayout(panPersonnel);
        panPersonnel.setLayout(panPersonnelLayout);
        panPersonnelLayout.setHorizontalGroup(
            panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panPersonnelLayout.createSequentialGroup()
                .addContainerGap()
                .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panPersonnelLayout.createSequentialGroup()
                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 384, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btnAssignDoc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 390, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel4)))
                    .add(jLabel3))
                .add(434, 434, 434))
        );
        panPersonnelLayout.setVerticalGroup(
            panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panPersonnelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)
                    .add(panPersonnelLayout.createSequentialGroup()
                        .add(jLabel4)
                        .add(3, 3, 3)
                        .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(btnAssignDoc)
                            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        panFinances.addTab(resourceMap.getString("panPersonnel.TabConstraints.tabTitle"), panPersonnel); // NOI18N

        jPanel4.setName("jPanel4"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addContainerGap(1073, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addContainerGap(645, Short.MAX_VALUE))
        );

        panFinances.addTab(resourceMap.getString("jPanel4.TabConstraints.tabTitle"), jPanel4); // NOI18N

        lblDate.setText(resourceMap.getString("lblDate.text")); // NOI18N
        lblDate.setName("lblDate"); // NOI18N

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(mainPanelLayout.createSequentialGroup()
                        .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(btnAdvanceDay)
                            .add(mainPanelLayout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(lblDate, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 284, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(1008, Short.MAX_VALUE))
                    .add(mainPanelLayout.createSequentialGroup()
                        .add(panFinances, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1279, Short.MAX_VALUE)
                        .add(20, 20, 20))))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(lblDate)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btnAdvanceDay)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panFinances, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 727, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        menuLoad.setText(resourceMap.getString("menuLoad.text")); // NOI18N
        menuLoad.setEnabled(false);
        menuLoad.setName("menuLoad"); // NOI18N
        fileMenu.add(menuLoad);

        menuSave.setText(resourceMap.getString("menuSave.text")); // NOI18N
        menuSave.setEnabled(false);
        menuSave.setName("menuSave"); // NOI18N
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
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1312, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 1116, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusAnimationLabel)
                .addContainerGap())
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
        refreshTeamsList();
        if(!completed) {
            row++;
        }
        if(row >= TaskTable.getRowCount()) {
            row = 0;
        }
        TaskTable.setRowSelectionInterval(row, row); 
    }
}//GEN-LAST:event_btnDoTaskActionPerformed

private void TeamsListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_TeamsListValueChanged
    int selected = TeamsList.getSelectedIndex();
    if(selected > -1 && selected < campaign.getTeams().size()) {
        currentTechId = campaign.getTechTeams().get(selected).getId();
    }
    else if(selected < 0) {
        currentTechId = -1;
    }
    updateAssignEnabled();
    updateReplaceEnabled();
    updateAmmoSwapEnabled();
}//GEN-LAST:event_TeamsListValueChanged

private void TaskTableValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
    int selected = TaskTable.getSelectedRow();
    if(selected > -1 && selected < campaign.getTasksForUnit(currentUnitId).size()) {
        currentTaskId = campaign.getTasksForUnit(currentUnitId).get(selected).getId();
    }
    else if(selected < 0) {
        currentTaskId = -1;
    }
    updateAssignEnabled();
    updateReplaceEnabled();
    updateAmmoSwapEnabled();
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

private void btnAdvanceDayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdvanceDayActionPerformed
    campaign.processDay();
    TaskReportDialog trd = new TaskReportDialog(this.getFrame(), true);
    trd.report(campaign);
    trd.setVisible(true);
    refreshUnitList();
    refreshTaskList();
    refreshTeamsList();
    refreshPersonnelList();
    refreshDoctorsList();
    refreshCalendar();
}//GEN-LAST:event_btnAdvanceDayActionPerformed

private void btnDeployUnitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeployUnitsActionPerformed
    saveListFile();
}//GEN-LAST:event_btnDeployUnitsActionPerformed

private void replaceBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceBtnActionPerformed
    WorkItem task = campaign.getTask(currentTaskId);
    if(task instanceof RepairItem) {
        campaign.mutateTask(task, ((RepairItem)task).replace());
        refreshTaskList();
    }
}//GEN-LAST:event_replaceBtnActionPerformed

private void ammoBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ammoBtnActionPerformed
    WorkItem task = campaign.getTask(currentTaskId);
    Unit unit = campaign.getUnit(currentUnitId);
    if(null != task && null != unit && task instanceof ReloadItem) {
        AmmoDialog ammod = new AmmoDialog(this.getFrame(), true, (ReloadItem)task, unit.getEntity());
        ammod.setVisible(true);
        refreshUnitList();
        refreshTaskList();
        refreshTeamsList();
    }
}//GEN-LAST:event_ammoBtnActionPerformed

private void btnChangePilotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChangePilotActionPerformed
    if(currentUnitId == -1) {
      return;
   }
    Unit u = campaign.getUnit(currentUnitId);
    Vector<PilotPerson> pilots = new Vector<PilotPerson>();
    for(Person p : campaign.getPersonnel()) {
        if(!(p instanceof PilotPerson)) {
            continue;
        }
        PilotPerson pp = (PilotPerson)p;
        if(pp.canPilot(u.getEntity())) {
            pilots.add(pp);
        }
    }
    if(pilots.size() > 0) {
        ChoosePilotDialog cpd = new ChoosePilotDialog(this.getFrame(), true, u, pilots);
        cpd.setVisible(true);
        refreshUnitList();
    }
}//GEN-LAST:event_btnChangePilotActionPerformed

private void btnAssignDocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAssignDocActionPerformed
    //assign the task to the team here
    Person p = campaign.getPerson(currentPersonId);
    if(null != p.getTask()) {
        campaign.assignTask(currentDoctorId, p.getTask().getId());
    }
    refreshTeamsList();
    refreshDoctorsList();
    refreshPersonnelList();
}//GEN-LAST:event_btnAssignDocActionPerformed

private void DoctorsListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_DoctorsListValueChanged
    int selected = DoctorsList.getSelectedIndex();
    if(selected > -1 && selected < campaign.getDoctors().size()) {
        currentDoctorId = campaign.getDoctors().get(selected).getId();
    }
    else if(selected < 0) {
        currentDoctorId = -1;
    }
    updateAssignDoctorEnabled();  
}//GEN-LAST:event_DoctorsListValueChanged

private void personnelListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_personnelListValueChanged
    int selected = personnelList.getSelectedIndex();
    if(selected > -1 && selected < campaign.getPersonnel().size()) {
        currentPersonId = campaign.getPersonnel().get(selected).getId();
    }
    else if(selected < 0) {
        currentPersonId = -1;
    }
    updateAssignDoctorEnabled();
}//GEN-LAST:event_personnelListValueChanged

private void miHirePilotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHirePilotActionPerformed
    NewPilotDialog npd = new NewPilotDialog(this.getFrame(), true, campaign);
    npd.setVisible(true);
    refreshPersonnelList();
}//GEN-LAST:event_miHirePilotActionPerformed

private void miHireTechActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHireTechActionPerformed
    NewTechTeamDialog ntd = new NewTechTeamDialog(this.getFrame(), true, campaign);
    ntd.setVisible(true);
    refreshTeamsList();
    refreshPersonnelList();
}//GEN-LAST:event_miHireTechActionPerformed

private void miHireDoctorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miHireDoctorActionPerformed
    NewMedicalTeamDialog nmd = new NewMedicalTeamDialog(this.getFrame(), true, campaign);
    nmd.setVisible(true);
    refreshTeamsList();
    refreshPersonnelList();
    refreshDoctorsList();
}//GEN-LAST:event_miHireDoctorActionPerformed

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
                    campaign.removeUnit(id);
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
    unitModel.setUnits(campaign.getUnits());
    unitModel.refreshModel();
    if(selected > -1 && selected < campaign.getUnits().size()) {
        UnitTable.setRowSelectionInterval(selected, selected);
    }
}

protected void refreshTaskList() {
        taskModel.fillTable(campaign.getTasksForUnit(currentUnitId));
}

protected void refreshTeamsList() {
    int selected = TeamsList.getSelectedIndex();
    techsModel.removeAllElements();
    int len = 0;
    for(SupportTeam team : campaign.getTechTeams()) {
        techsModel.addElement(team.getDesc());
        len++;
    }
    if(selected < len) {
        TeamsList.setSelectedIndex(selected);
    }
}

protected void refreshDoctorsList() {
    int selected = DoctorsList.getSelectedIndex();
    doctorsModel.removeAllElements();
    int len = 0;
    for(SupportTeam team : campaign.getDoctors()) {
        doctorsModel.addElement(team.getDesc());
        len++;
    }
    if(selected < len) {
        DoctorsList.setSelectedIndex(selected);
    }
}

protected void refreshPersonnelList() {
    int selected = personnelList.getSelectedIndex();
    personnelModel.removeAllElements();
    int len = 0;
    for(Person person : campaign.getPersonnel()) {
        personnelModel.addElement(person.getDesc());
        len++;
    }
    if(selected < len) {
        personnelList.setSelectedIndex(selected);
    }
}

protected void refreshCalendar() {
    lblDate.setText(dateFormat.format(campaign.calendar.getTime()));
    
}

protected void updateAssignEnabled() {
    //must have a valid team and an unassigned task
    WorkItem curTask = campaign.getTask(currentTaskId);
    SupportTeam team = campaign.getTeam(currentTechId);
    if(null != curTask && null != team && team.canDo(curTask)) {
        btnDoTask.setEnabled(true);
    } else {
        btnDoTask.setEnabled(false);
    }    
}

protected void updateAssignDoctorEnabled() {
    //must have a valid doctor and an unassigned task
    Person curPerson = campaign.getPerson(currentPersonId);
    SupportTeam team = campaign.getTeam(currentDoctorId);
    if(null != curPerson && null != curPerson.getTask() 
            && null != team && team.canDo(curPerson.getTask())) {     
        btnAssignDoc.setEnabled(true);
    } else {
        btnAssignDoc.setEnabled(false);
    }    
}

protected void updateReplaceEnabled() {
    //must have a valid team and an unassigned task
    WorkItem curTask = campaign.getTask(currentTaskId);
    if(null != curTask && curTask instanceof RepairItem) {
        replaceBtn.setEnabled(true);
    } else {
        replaceBtn.setEnabled(false);
    }    
}

protected void updateTargetText() {
    //must have a valid team and an unassigned task
    WorkItem task = campaign.getTask(currentTaskId);
    SupportTeam team = campaign.getTeam(currentTechId);
    if(null != task && null != team) {
        textTarget.setText(team.getTargetFor(task).getDesc());
    } else {
        textTarget.setText("");
    }
}

protected void updateAmmoSwapEnabled() {
    WorkItem curTask = campaign.getTask(currentTaskId);
    if(null != curTask && curTask instanceof ReloadItem) {
        ammoBtn.setEnabled(true);
    } else {
        ammoBtn.setEnabled(false);
    }    
}

/**
 * A table model for displaying work items
 */
public class TaskTableModel extends AbstractTableModel {

        public static final int NAME_INDEX = 0;
        public static final int TIME_INDEX = 1;
        public static final int SKILL_INDEX = 2;
        public static final int MOD_INDEX = 3;
        public static final int NUM_COLS = 4;
    
        private String[] columnNames = {"Name", "Time Left", "Skill", "Mod"};
        private ArrayList<WorkItem> tasks;
 
        public TaskTableModel() {
            tasks = new ArrayList<WorkItem>();
        }
        
        @Override
        public int getRowCount() {
            return tasks.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int row, int col) {
            WorkItem task = tasks.get(row);
            switch (col) {
             case NAME_INDEX:
                return task.getName();
             case TIME_INDEX:
                return task.getTime();
             case SKILL_INDEX:
                return SupportTeam.getRatingName(task.getSkillMin());
             case MOD_INDEX:
                return task.getAllMods().getValueAsString();
             default:
                return new Object();
         }
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
        public void fillTable(ArrayList<WorkItem> tasks) {
            this.tasks = tasks;
            fireTableDataChanged();
        }
        
}

public class MekTableModel extends AbstractTableModel {

    private String[] columnNames = {"Units"};
    private ArrayList<Unit> units = new ArrayList<Unit>();
    
    public void setUnits(ArrayList<Unit> u) {
        this.units = u;
    }
    
    
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    @Override
    public int getRowCount() {
        return units.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return campaign.getUnitDesc(units.get(row).getId());
    }
    
    public Unit getUnitAt(int row) {
        return units.get(row);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }
    
    public void refreshModel() {
        fireTableDataChanged();
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
                c.setBackground(new Color(220, 220, 220));
            }
            return c;
        }
        
    }
}

public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();
            Unit unit = unitModel.getUnitAt(UnitTable.getSelectedRow());
            if (command.equalsIgnoreCase("REMOVE_PILOT")) {
                unit.removePilot();
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
                JMenuItem menuItem = null;
                //**lets fill the pop up menu**//               
                //TODO: assign all tasks to a certain tech
                //remove pilot
                menuItem = new JMenuItem("Remove pilot");
                menuItem.setActionCommand("REMOVE_PILOT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.hasPilot());
                popup.add(menuItem);
                //TODO: switch pilot (should be its own menu)
                //TODO: scrap unit
                //TODO: sell unit
                //TODO: add quirks?
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList DoctorsList;
    private javax.swing.JTable TaskTable;
    private javax.swing.JList TeamsList;
    private javax.swing.JTable UnitTable;
    private javax.swing.JButton ammoBtn;
    private javax.swing.JButton btnAdvanceDay;
    private javax.swing.JButton btnAssignDoc;
    private javax.swing.JButton btnChangePilot;
    private javax.swing.JButton btnDeployUnits;
    private javax.swing.JButton btnDoTask;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblTasks;
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
    private javax.swing.JTabbedPane panFinances;
    private javax.swing.JPanel panHangar;
    private javax.swing.JPanel panPersonnel;
    private javax.swing.JPanel panSupplies;
    private javax.swing.JList personnelList;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton replaceBtn;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextArea textTarget;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}

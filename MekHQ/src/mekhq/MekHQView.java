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

import java.awt.Component;
import java.awt.Font;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.Campaign;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import megamek.client.ui.MechView;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Pilot;
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
    private int currentUnitId;
    private int currentTaskId;
    private int currentTechId;
    private int currentPersonId;
    private int currentDoctorId;
    
    private TaskReportDialog trd;
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
    
    public MekHQView(SingleFrameApplication app) {
        super(app);
      
        initComponents();

        refreshCalendar();
        
        trd = new TaskReportDialog(this.getFrame(), false);
        
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
        lblUnits = new javax.swing.JLabel();
        UnitsScroll = new javax.swing.JScrollPane();
        UnitList = new javax.swing.JList();
        lblTasks = new javax.swing.JLabel();
        btnViewUnit = new javax.swing.JButton();
        btnDeployUnits = new javax.swing.JButton();
        loadListBtn = new javax.swing.JButton();
        ammoBtn = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        replaceBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        TeamsList = new javax.swing.JList();
        assignBtn = new javax.swing.JButton();
        lblTeams = new javax.swing.JLabel();
        btnNewTeam = new javax.swing.JButton();
        btnPurchaseUnit = new javax.swing.JButton();
        btnSellUnit = new javax.swing.JButton();
        btnRemovePilot = new javax.swing.JButton();
        btnChangePilot = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        TaskTable = new javax.swing.JTable();
        btnOrganizeTask = new javax.swing.JButton();
        panSupplies = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        panPersonnel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        personnelList = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        btnHirePilot = new javax.swing.JButton();
        btnHireTech = new javax.swing.JButton();
        btnCustomizePerson = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        DoctorsList = new javax.swing.JList();
        btnAssignDoc = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        btnHireDoctor = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        menuLoad = new javax.swing.JMenuItem();
        menuSave = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
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

        lblUnits.setText(resourceMap.getString("lblUnits.text")); // NOI18N
        lblUnits.setName("lblUnits"); // NOI18N

        UnitsScroll.setName("UnitsScroll"); // NOI18N

        UnitList.setBackground(resourceMap.getColor("UnitList.background")); // NOI18N
        UnitList.setModel(unitsModel);
        UnitList.setName("UnitList"); // NOI18N
        UnitList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                UnitListValueChanged(evt);
            }
        });
        UnitsScroll.setViewportView(UnitList);

        lblTasks.setText(resourceMap.getString("lblTasks.text")); // NOI18N
        lblTasks.setName("lblTasks"); // NOI18N

        btnViewUnit.setText(resourceMap.getString("btnViewUnit.text")); // NOI18N
        btnViewUnit.setToolTipText(resourceMap.getString("btnViewUnit.toolTipText")); // NOI18N
        btnViewUnit.setName("btnViewUnit"); // NOI18N
        btnViewUnit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewUnitActionPerformed(evt);
            }
        });

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

        assignBtn.setText(resourceMap.getString("assignBtn.text")); // NOI18N
        assignBtn.setToolTipText(resourceMap.getString("assignBtn.toolTipText")); // NOI18N
        assignBtn.setEnabled(false);
        assignBtn.setName("assignBtn"); // NOI18N
        assignBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                assignBtnActionPerformed(evt);
            }
        });

        lblTeams.setText(resourceMap.getString("lblTeams.text")); // NOI18N
        lblTeams.setName("lblTeams"); // NOI18N

        btnNewTeam.setText(resourceMap.getString("btnNewTeam.text")); // NOI18N
        btnNewTeam.setToolTipText(resourceMap.getString("btnNewTeam.toolTipText")); // NOI18N
        btnNewTeam.setName("btnNewTeam"); // NOI18N
        btnNewTeam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewTeamActionPerformed(evt);
            }
        });

        btnPurchaseUnit.setText(resourceMap.getString("btnPurchaseUnit.text")); // NOI18N
        btnPurchaseUnit.setEnabled(false);
        btnPurchaseUnit.setName("btnPurchaseUnit"); // NOI18N

        btnSellUnit.setText(resourceMap.getString("btnSellUnit.text")); // NOI18N
        btnSellUnit.setEnabled(false);
        btnSellUnit.setName("btnSellUnit"); // NOI18N

        btnRemovePilot.setText(resourceMap.getString("btnRemovePilot.text")); // NOI18N
        btnRemovePilot.setName("btnRemovePilot"); // NOI18N
        btnRemovePilot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemovePilotActionPerformed(evt);
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

        btnOrganizeTask.setText(resourceMap.getString("btnOrganizeTask.text")); // NOI18N
        btnOrganizeTask.setName("btnOrganizeTask"); // NOI18N
        btnOrganizeTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOrganizeTaskActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout panHangarLayout = new org.jdesktop.layout.GroupLayout(panHangar);
        panHangar.setLayout(panHangarLayout);
        panHangarLayout.setHorizontalGroup(
            panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panHangarLayout.createSequentialGroup()
                .addContainerGap()
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panHangarLayout.createSequentialGroup()
                        .add(UnitsScroll, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 343, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(btnChangePilot, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnRemovePilot, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnSellUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnPurchaseUnit)
                            .add(btnViewUnit, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnDeployUnits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, loadListBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(lblTasks, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 197, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(assignBtn)
                            .add(panHangarLayout.createSequentialGroup()
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 475, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(lblTeams))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(btnOrganizeTask, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(btnNewTeam, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                                    .add(ammoBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(replaceBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .add(139, 139, 139)
                        .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(lblUnits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(0, 0, 0))
        );
        panHangarLayout.setVerticalGroup(
            panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panHangarLayout.createSequentialGroup()
                .addContainerGap()
                .add(lblUnits)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(UnitsScroll)
                    .add(panHangarLayout.createSequentialGroup()
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(panHangarLayout.createSequentialGroup()
                                .add(loadListBtn)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnDeployUnits)
                                .add(18, 18, 18)
                                .add(btnViewUnit)
                                .add(18, 18, 18)
                                .add(btnPurchaseUnit)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(btnSellUnit)
                                .add(18, 18, 18)
                                .add(btnRemovePilot)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(btnChangePilot))
                            .add(panHangarLayout.createSequentialGroup()
                                .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(panHangarLayout.createSequentialGroup()
                                        .add(lblTasks)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(panHangarLayout.createSequentialGroup()
                                                .add(ammoBtn)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                .add(replaceBtn))
                                            .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 224, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                .add(7, 7, 7)
                                .add(assignBtn)))
                        .add(11, 11, 11)
                        .add(lblTeams)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(panHangarLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(panHangarLayout.createSequentialGroup()
                                .add(btnNewTeam)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(btnOrganizeTask))
                            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 232, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(86, Short.MAX_VALUE))
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
                .addContainerGap(954, Short.MAX_VALUE))
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

        btnHirePilot.setText(resourceMap.getString("btnHirePilot.text")); // NOI18N
        btnHirePilot.setName("btnHirePilot"); // NOI18N
        btnHirePilot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHirePilotActionPerformed(evt);
            }
        });

        btnHireTech.setText(resourceMap.getString("btnHireTech.text")); // NOI18N
        btnHireTech.setName("btnHireTech"); // NOI18N
        btnHireTech.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHireTechActionPerformed(evt);
            }
        });

        btnCustomizePerson.setText(resourceMap.getString("btnCustomizePerson.text")); // NOI18N
        btnCustomizePerson.setEnabled(false);
        btnCustomizePerson.setName("btnCustomizePerson"); // NOI18N

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

        btnHireDoctor.setText(resourceMap.getString("btnHireDoctor.text")); // NOI18N
        btnHireDoctor.setName("btnHireDoctor"); // NOI18N
        btnHireDoctor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHireDoctorActionPerformed(evt);
            }
        });

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
                        .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(btnAssignDoc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnCustomizePerson, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnHirePilot, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(btnHireDoctor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, btnHireTech, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
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
                        .add(panPersonnelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(panPersonnelLayout.createSequentialGroup()
                                .add(btnHirePilot)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnHireTech)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnHireDoctor)
                                .add(27, 27, 27)
                                .add(btnCustomizePerson)
                                .add(18, 18, 18))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, panPersonnelLayout.createSequentialGroup()
                                .add(jLabel4)
                                .add(3, 3, 3)))
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
                .addContainerGap(954, Short.MAX_VALUE))
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
                    .add(panFinances, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1160, Short.MAX_VALUE)
                    .add(btnAdvanceDay)
                    .add(mainPanelLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lblDate, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 284, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
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
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1193, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 997, Short.MAX_VALUE)
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

private void btnNewTeamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewTeamActionPerformed
    NewTechTeamDialog ntd = new NewTechTeamDialog(this.getFrame(), true, campaign);
    ntd.setVisible(true);
    refreshTeamsList();
    refreshDoctorsList();
    refreshPersonnelList();
}//GEN-LAST:event_btnNewTeamActionPerformed

private void loadListBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadListBtnActionPerformed
    loadListFile();
}//GEN-LAST:event_loadListBtnActionPerformed

private void UnitListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_UnitListValueChanged
    int selected = UnitList.getSelectedIndex();
    if(selected > -1 && selected < campaign.getUnits().size()) {
        currentUnitId = campaign.getUnits().get(selected).getId();
    }
    else if(selected < 0) {
        currentUnitId = -1;
    }
    refreshTaskList();
}//GEN-LAST:event_UnitListValueChanged

private void assignBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_assignBtnActionPerformed
    //assign the task to the team here
    campaign.assignTask(currentTechId, currentTaskId);
    int next = TaskTable.getSelectedRow() + 1;
    refreshUnitList();
    refreshTaskList();
    if(next < TaskTable.getRowCount()) {
        TaskTable.setRowSelectionInterval(next, next);
    } else {
        TaskTable.setRowSelectionInterval(0,0);
    }
    refreshTeamsList();
    refreshDoctorsList();
}//GEN-LAST:event_assignBtnActionPerformed

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
}

private void btnAdvanceDayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdvanceDayActionPerformed
    campaign.processDay();
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

private void btnViewUnitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewUnitActionPerformed
  if(currentUnitId == -1) {
      return;
  }
  MechView mv = new MechView(campaign.getUnit(currentUnitId).getEntity(), false);
  MekViewDialog mvd = new MekViewDialog(this.getFrame(), true, mv);
  mvd.setVisible(true);
}//GEN-LAST:event_btnViewUnitActionPerformed

private void btnHireTechActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireTechActionPerformed
    NewTechTeamDialog ntd = new NewTechTeamDialog(this.getFrame(), true, campaign);
    ntd.setVisible(true);
    refreshTeamsList();
    refreshPersonnelList();
}//GEN-LAST:event_btnHireTechActionPerformed

private void btnHirePilotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHirePilotActionPerformed
    NewPilotDialog npd = new NewPilotDialog(this.getFrame(), true, campaign);
    npd.setVisible(true);
    refreshPersonnelList();
}//GEN-LAST:event_btnHirePilotActionPerformed

private void btnRemovePilotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemovePilotActionPerformed
   if(currentUnitId == -1) {
      return;
   }
   campaign.getUnit(currentUnitId).removePilot();
   refreshUnitList();
}//GEN-LAST:event_btnRemovePilotActionPerformed

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

private void btnHireDoctorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireDoctorActionPerformed
    NewMedicalTeamDialog nmd = new NewMedicalTeamDialog(this.getFrame(), true, campaign);
    nmd.setVisible(true);
    refreshTeamsList();
    refreshPersonnelList();
    refreshDoctorsList();
}//GEN-LAST:event_btnHireDoctorActionPerformed

private void btnOrganizeTaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOrganizeTaskActionPerformed
    if(currentTechId == -1) {
        return;
    }   
    OrganizeTasksDialog otd = new OrganizeTasksDialog(this.getFrame(), true, campaign.getTasksForTeam(currentTechId));
    otd.setVisible(true);
}//GEN-LAST:event_btnOrganizeTaskActionPerformed

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
    if(UnitList.getSelectedIndex() == -1) {
        return;
    }
    
    ArrayList<Entity> chosen = new ArrayList<Entity>();
    ArrayList<Integer> toRemove = new ArrayList<Integer>();
    for(int i : UnitList.getSelectedIndices()) {
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
    int selected = UnitList.getSelectedIndex();
    unitsModel.removeAllElements();
    int len = 0;
    for(Unit unit: campaign.getUnits()) {
        campaign.getTasksForUnit(unit.getId());
        unitsModel.addElement(unit.getEntity().getDisplayName() + " [" + unit.getPilotDesc() + "] " + campaign.getUnitTaskDesc(unit.getId()));
        len++;
    }
    if(selected < len) {
        UnitList.setSelectedIndex(selected);
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
    if(null != curTask && curTask.getTeamId() == WorkItem.TEAM_NONE && null != team && team.canDo(curTask)) {
        assignBtn.setEnabled(true);
    } else {
        assignBtn.setEnabled(false);
    }    
}

protected void updateAssignDoctorEnabled() {
    //must have a valid doctor and an unassigned task
    Person curPerson = campaign.getPerson(currentPersonId);
    SupportTeam team = campaign.getTeam(currentDoctorId);
    if(null != curPerson && null != curPerson.getTask() 
            && curPerson.getTask().getTeamId() == WorkItem.TEAM_NONE 
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
        public static final int ASSIGN_INDEX = 4;
        public static final int NUM_COLS = 5;
    
        private String[] columnNames = {"Name", "Time Left", "Skill", "Mod", "Assigned"};
        private ArrayList<WorkItem> tasks;
 
        public TaskTableModel() {
            tasks = new ArrayList<WorkItem>();
        }
        
        public int getRowCount() {
            return tasks.size();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

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
             case ASSIGN_INDEX:
                 if(task.isUnassigned()) {
                     return "none";
                 } else {
                    return campaign.getTeam(task.getTeamId()).getName();
                 }
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
            //allow editing of assignment column only
            if(col == ASSIGN_INDEX) {
                return true;
            } else {
                return false;
            }
        }
     
        //fill table with values
        public void fillTable(ArrayList<WorkItem> tasks) {
            this.tasks = tasks;
            fireTableDataChanged();
        }
        
}

/**
 * An extension of the JTable to allow for different comboboxes in 
 * each row
 * http://stackoverflow.com/questions/457463/putting-jcombobox-into-jtable
 */
public class ComboTable extends JTable {
    
    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        Object value = super.getValueAt(row, column);
        if(value != null) {
            if(value instanceof JComboBox) {
                return new DefaultCellEditor((JComboBox)value);
            }
            return getDefaultEditor(value.getClass());
        }
        return super.getCellEditor(row, column);
    }
  
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        Object value = super.getValueAt(row, column);
        if(value != null) {
            if(value instanceof JComboBox) {
              //  return new DefaultTableCellRenderer((JComboBox)value);
            }
            return getDefaultRenderer(value.getClass());
        }
        return super.getCellRenderer(row, column);
    }
}

public class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
        public MyComboBoxRenderer(String[] items) {
            super(items);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
    
            // Select the current value
            setSelectedItem(value);
            return this;
        }
    }
    
    public class MyComboBoxEditor extends DefaultCellEditor {
        public MyComboBoxEditor(String[] items) {
            super(new JComboBox(items));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList DoctorsList;
    private javax.swing.JTable TaskTable;
    private javax.swing.JList TeamsList;
    private javax.swing.JList UnitList;
    private javax.swing.JScrollPane UnitsScroll;
    private javax.swing.JButton ammoBtn;
    private javax.swing.JButton assignBtn;
    private javax.swing.JButton btnAdvanceDay;
    private javax.swing.JButton btnAssignDoc;
    private javax.swing.JButton btnChangePilot;
    private javax.swing.JButton btnCustomizePerson;
    private javax.swing.JButton btnDeployUnits;
    private javax.swing.JButton btnHireDoctor;
    private javax.swing.JButton btnHirePilot;
    private javax.swing.JButton btnHireTech;
    private javax.swing.JButton btnNewTeam;
    private javax.swing.JButton btnOrganizeTask;
    private javax.swing.JButton btnPurchaseUnit;
    private javax.swing.JButton btnRemovePilot;
    private javax.swing.JButton btnSellUnit;
    private javax.swing.JButton btnViewUnit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblTasks;
    private javax.swing.JLabel lblTeams;
    private javax.swing.JLabel lblUnits;
    private javax.swing.JButton loadListBtn;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem menuLoad;
    private javax.swing.JMenuItem menuSave;
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
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}

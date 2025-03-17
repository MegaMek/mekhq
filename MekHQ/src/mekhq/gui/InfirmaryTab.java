/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.TargetRoll;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.OptimizeInfirmaryAssignments;
import mekhq.campaign.event.MedicPoolChangedEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.PersonMedicalAssignmentEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.DocTableModel;
import mekhq.gui.model.PatientTableModel;
import mekhq.gui.sorter.PersonTitleSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.*;
import java.util.List;

/**
 * Shows injured and medical personnel
 */
public final class InfirmaryTab extends CampaignGuiTab {
    private JTable docTable;
    private JButton btnAssignDoc;
    private JButton btnUnassignDoc;
    private JButton btnOptimizeAssignments;
    private JList<Person> listAssignedPatient;
    private JList<Person> listUnassignedPatient;

    private PatientTableModel assignedPatientModel;
    private PatientTableModel unassignedPatientModel;
    private DocTableModel doctorsModel;

    private Image bgImage;

    //region Constructors
    public InfirmaryTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }
    //endregion Constructors

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
              MekHQ.getMHQOptions().getLocale());

        setLayout(new GridBagLayout());

        doctorsModel = new DocTableModel(getCampaign());
        docTable = new JTable(doctorsModel);
        docTable.setRowHeight(UIUtil.scaleForGUI(60));
        docTable.getColumnModel().getColumn(0).setCellRenderer(doctorsModel.getRenderer());
        docTable.getSelectionModel().addListSelectionListener(ev -> docTableValueChanged());
        docTable.setOpaque(false);
        JScrollPane scrollDocTable = new JScrollPaneWithSpeed(docTable);
        scrollDocTable.setMinimumSize(new Dimension(300, 300));
        scrollDocTable.setPreferredSize(new Dimension(300, 300));
        scrollDocTable.setOpaque(false);
        scrollDocTable.getViewport().setOpaque(false);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        add(scrollDocTable, gridBagConstraints);

        // Create buttons
        btnAssignDoc = new JButton(resourceMap.getString("btnAssignDoc.text"));
        btnAssignDoc.setToolTipText(resourceMap.getString("btnAssignDoc.toolTipText"));
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.addActionListener(ev -> assignDoctor());

        btnUnassignDoc = new JButton(resourceMap.getString("btnUnassignDoc.text"));
        btnUnassignDoc.setEnabled(false);
        btnUnassignDoc.addActionListener(ev -> unassignDoctor());

        btnOptimizeAssignments = new JButton(resourceMap.getString("btnOptimizeAssignments.text"));
        btnOptimizeAssignments.addActionListener(ev -> new OptimizeInfirmaryAssignments(getCampaign()));

        // Create a panel to group the buttons together horizontally
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(btnAssignDoc);
        buttonPanel.add(btnUnassignDoc);
        buttonPanel.add(btnOptimizeAssignments);

        // Add the button panel to the layout
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(buttonPanel, gridBagConstraints);

        assignedPatientModel = new PatientTableModel(getCampaign());
        listAssignedPatient = new JList<>(assignedPatientModel);
        listAssignedPatient.setCellRenderer(assignedPatientModel.getRenderer());
        listAssignedPatient.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listAssignedPatient.setVisibleRowCount(-1);
        listAssignedPatient.getSelectionModel().addListSelectionListener(ev -> updateAssignDoctorEnabled());
        listAssignedPatient.setOpaque(false);
        JScrollPane scrollAssignedPatient = new JScrollPaneWithSpeed(listAssignedPatient);
        scrollAssignedPatient.setMinimumSize(new Dimension(300, 360));
        scrollAssignedPatient.setPreferredSize(new Dimension(300, 360));
        scrollAssignedPatient.setOpaque(false);
        scrollAssignedPatient.getViewport().setOpaque(false);
        unassignedPatientModel = new PatientTableModel(getCampaign());
        listUnassignedPatient = new JList<>(unassignedPatientModel);
        listUnassignedPatient.setCellRenderer(unassignedPatientModel.getRenderer());
        listUnassignedPatient.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listUnassignedPatient.setVisibleRowCount(-1);
        listUnassignedPatient.getSelectionModel().addListSelectionListener(ev -> updateAssignDoctorEnabled());
        listUnassignedPatient.setOpaque(false);
        JScrollPane scrollUnassignedPatient = new JScrollPaneWithSpeed(listUnassignedPatient);
        scrollUnassignedPatient.setMinimumSize(new Dimension(300, 200));
        scrollUnassignedPatient.setPreferredSize(new Dimension(300, 300));
        scrollUnassignedPatient.setOpaque(false);
        scrollUnassignedPatient.getViewport().setOpaque(false);
        listAssignedPatient
                .setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panAssignedPatient.title")));
        listUnassignedPatient
                .setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panUnassignedPatient.title")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        add(scrollAssignedPatient, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(scrollUnassignedPatient, gridBagConstraints);
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshPatientList();
        refreshDoctorsList();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    public MHQTabType tabType() {
        return MHQTabType.INFIRMARY;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (null == bgImage) {
            return;
        }
        int size = Math.max(getWidth(), getHeight());
        g.drawImage(bgImage, 0, 0, size, size, (img, infoflags, x, y, width, height) -> {
            if ((infoflags & ImageObserver.ALLBITS) != 0) {
                repaint();
                return false;
            }
            return true;
        });
    }

    private Person getSelectedDoctor() {
        int row = docTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return doctorsModel.getDoctorAt(docTable.convertRowIndexToModel(row));
    }

    private ArrayList<Person> getSelectedAssignedPatients() {
        ArrayList<Person> patients = new ArrayList<>();
        int[] indices = listAssignedPatient.getSelectedIndices();
        if (assignedPatientModel.getSize() == 0) {
            return patients;
        }
        for (int idx : indices) {
            Person p = assignedPatientModel.getElementAt(idx);
            if (p == null) {
                continue;
            }
            patients.add(p);
        }
        return patients;
    }

    private ArrayList<Person> getSelectedUnassignedPatients() {
        ArrayList<Person> patients = new ArrayList<>();
        int[] indices = listUnassignedPatient.getSelectedIndices();
        if (unassignedPatientModel.getSize() == 0) {
            return patients;
        }
        for (int idx : indices) {
            Person p = unassignedPatientModel.getElementAt(idx);
            if (p == null) {
                continue;
            }
            patients.add(p);
        }
        return patients;
    }

    private void updateAssignDoctorEnabled() {
        Person doctor = getSelectedDoctor();
        btnAssignDoc.setEnabled((null != doctor) && (getCampaign().getPatientsFor(doctor) < 25)
                && (unassignedPatientModel.getSize() > 0));
        btnUnassignDoc.setEnabled(!getSelectedAssignedPatients().isEmpty());
    }

    private void docTableValueChanged() {
        refreshPatientList();
        updateAssignDoctorEnabled();
    }

    private void assignDoctor() {
        Person doctor = getSelectedDoctor();
        if (null == doctor) {
            return;
        }
        Collection<Person> selectedPatients = getSelectedUnassignedPatients();
        if (selectedPatients.isEmpty()) {
            // Pick the first in the list ... if there are any
            int patientSize = unassignedPatientModel.getSize();
            for (int i = 0; i < patientSize; ++i) {
                Person p = unassignedPatientModel.getElementAt(i);
                if ((null != p)
                        && (p.needsFixing()
                                || (getCampaign().getCampaignOptions().isUseAdvancedMedical() && p.needsAMFixing()))
                        && (getCampaign().getPatientsFor(doctor) < getCampaign().getCampaignOptions().getMaximumPatients())
                        && (getCampaign().getTargetFor(p, doctor).getValue() != TargetRoll.IMPOSSIBLE)) {
                    p.setDoctorId(doctor.getId(), getCampaign().getCampaignOptions().getHealingWaitingPeriod());
                    MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, p));
                    break;
                }
            }

        } else {
            for (Person p : selectedPatients) {
                if ((null != p)
                        && (p.needsFixing()
                                || (getCampaign().getCampaignOptions().isUseAdvancedMedical() && p.needsAMFixing()))
                        && (getCampaign().getPatientsFor(doctor) < getCampaign().getCampaignOptions().getMaximumPatients())
                        && (getCampaign().getTargetFor(p, doctor).getValue() != TargetRoll.IMPOSSIBLE)) {
                    p.setDoctorId(doctor.getId(), getCampaign().getCampaignOptions().getHealingWaitingPeriod());
                    MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, p));
                }
            }
        }
    }

    private void unassignDoctor() {
        Person doctor = getSelectedDoctor();
        for (Person p : getSelectedAssignedPatients()) {
            if ((null != p)) {
                p.setDoctorId(null, getCampaign().getCampaignOptions().getNaturalHealingWaitingPeriod());
                if (doctor != null) {
                    MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, p));
                }
            }
        }
    }

    public void refreshDoctorsList() {
        final int selected = docTable.getSelectedRow();
        final List<Person> doctors = getCampaign().getDoctors();
        doctors.sort(new PersonTitleSorter().reversed());
        doctorsModel.setData(doctors);
        if ((selected > -1) && (selected < doctors.size())) {
            docTable.setRowSelectionInterval(selected, selected);
        }
    }

    public void refreshPatientList() {
        Person doctor = getSelectedDoctor();
        ArrayList<Person> assigned = new ArrayList<>();
        ArrayList<Person> unassigned = new ArrayList<>();
        for (Person patient : getCampaign().getPatients()) {
            // Knock out inactive doctors
            if ((patient.getDoctorId() != null)
                    && (getCampaign().getPerson(patient.getDoctorId()) != null)
                    && !getCampaign().getPerson(patient.getDoctorId()).getStatus().isActive()) {
                patient.setDoctorId(null, getCampaign().getCampaignOptions().getNaturalHealingWaitingPeriod());
            }
            if (patient.getDoctorId() == null) {
                unassigned.add(patient);
            } else if ((doctor != null) && patient.getDoctorId().equals(doctor.getId())) {
                assigned.add(patient);
            }
        }
        List<Person> assignedPatients = getSelectedAssignedPatients();
        List<Person> unassignedPatients = getSelectedUnassignedPatients();
        int[] assignedIndices = new int[assignedPatients.size()];
        Arrays.fill(assignedIndices, Integer.MAX_VALUE);
        int[] unassignedIndices = new int[unassignedPatients.size()];
        Arrays.fill(unassignedIndices, Integer.MAX_VALUE);

        assignedPatientModel.setData(assigned);
        unassignedPatientModel.setData(unassigned);

        int i = 0;
        for (Person patient : assignedPatients) {
            int idx = assigned.indexOf(patient);
            assignedIndices[i] = (idx >= 0) ? idx : Integer.MAX_VALUE;
            ++i;
        }
        i = 0;
        for (Person patient : unassignedPatients) {
            int idx = unassigned.indexOf(patient);
            unassignedIndices[i] = (idx >= 0) ? idx : Integer.MAX_VALUE;
            ++i;
        }
        listAssignedPatient.setSelectedIndices(assignedIndices);
        listUnassignedPatient.setSelectedIndices(unassignedIndices);
    }

    private ActionScheduler doctorListScheduler = new ActionScheduler(this::refreshDoctorsList);
    private ActionScheduler patientListScheduler = new ActionScheduler(this::refreshPatientList);

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        doctorListScheduler.schedule();
        patientListScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonEvent ev) {
        doctorListScheduler.schedule();
        patientListScheduler.schedule();
    }

    @Subscribe
    public void handle(MedicPoolChangedEvent ev) {
        doctorListScheduler.schedule();
    }
}

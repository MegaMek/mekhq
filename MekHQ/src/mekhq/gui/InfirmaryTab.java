/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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

package mekhq.gui;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import megamek.common.TargetRoll;
import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Person;
import mekhq.gui.model.DocTableModel;
import mekhq.gui.model.PatientTableModel;

/**
 * Shows injured and medical personnel
 */

public final class InfirmaryTab extends CampaignGuiTab {

    private static final long serialVersionUID = 7558886712192449186L;

    private JTable docTable;
    private JButton btnAssignDoc;
    private JButton btnUnassignDoc;
    private JList<Person> listAssignedPatient;
    private JList<Person> listUnassignedPatient;

    private PatientTableModel assignedPatientModel;
    private PatientTableModel unassignedPatientModel;
    private DocTableModel doctorsModel;

    private Image bgImage;

    InfirmaryTab(CampaignGUI gui, String name) {
        super(gui, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        String bgImageFile = getIconPackage().getGuiElement("infirmary_background");
        if (null != bgImageFile && !bgImageFile.isEmpty()) {
            bgImage = Toolkit.getDefaultToolkit().createImage(bgImageFile);
        }

        doctorsModel = new DocTableModel(getCampaign());
        docTable = new JTable(doctorsModel);
        docTable.setRowHeight(60);
        docTable.getColumnModel().getColumn(0).setCellRenderer(doctorsModel.getRenderer(getIconPackage()));
        docTable.getSelectionModel().addListSelectionListener(ev -> docTableValueChanged());
        docTable.setOpaque(false);
        JScrollPane scrollDocTable = new JScrollPane(docTable);
        scrollDocTable.setMinimumSize(new java.awt.Dimension(300, 300));
        scrollDocTable.setPreferredSize(new java.awt.Dimension(300, 300));
        scrollDocTable.setOpaque(false);
        scrollDocTable.getViewport().setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        add(scrollDocTable, gridBagConstraints);

        btnAssignDoc = new JButton(resourceMap.getString("btnAssignDoc.text")); // NOI18N
        btnAssignDoc.setToolTipText(resourceMap.getString("btnAssignDoc.toolTipText")); // NOI18N
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.addActionListener(ev -> assignDoctor());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(btnAssignDoc, gridBagConstraints);

        btnUnassignDoc = new JButton(resourceMap.getString("btnUnassignDoc.text")); // NOI18N
        btnUnassignDoc.setEnabled(false);
        btnUnassignDoc.addActionListener(ev -> unassignDoctor());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(btnUnassignDoc, gridBagConstraints);

        assignedPatientModel = new PatientTableModel(getCampaign());
        listAssignedPatient = new JList<Person>(assignedPatientModel);
        listAssignedPatient.setCellRenderer(assignedPatientModel.getRenderer(getIconPackage()));
        listAssignedPatient.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listAssignedPatient.setVisibleRowCount(-1);
        listAssignedPatient.getSelectionModel().addListSelectionListener(ev -> updateAssignDoctorEnabled());
        listAssignedPatient.setOpaque(false);
        JScrollPane scrollAssignedPatient = new JScrollPane(listAssignedPatient);
        scrollAssignedPatient.setMinimumSize(new java.awt.Dimension(300, 360));
        scrollAssignedPatient.setPreferredSize(new java.awt.Dimension(300, 360));
        scrollAssignedPatient.setOpaque(false);
        scrollAssignedPatient.getViewport().setOpaque(false);
        unassignedPatientModel = new PatientTableModel(getCampaign());
        listUnassignedPatient = new JList<Person>(unassignedPatientModel);
        listUnassignedPatient.setCellRenderer(unassignedPatientModel.getRenderer(getIconPackage()));
        listUnassignedPatient.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listUnassignedPatient.setVisibleRowCount(-1);
        listUnassignedPatient.getSelectionModel().addListSelectionListener(ev -> updateAssignDoctorEnabled());
        listUnassignedPatient.setOpaque(false);
        JScrollPane scrollUnassignedPatient = new JScrollPane(listUnassignedPatient);
        scrollUnassignedPatient.setMinimumSize(new java.awt.Dimension(300, 200));
        scrollUnassignedPatient.setPreferredSize(new java.awt.Dimension(300, 300));
        scrollUnassignedPatient.setOpaque(false);
        scrollUnassignedPatient.getViewport().setOpaque(false);
        listAssignedPatient
                .setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panAssignedPatient.title")));
        listUnassignedPatient
                .setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panUnassignedPatient.title")));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        add(scrollAssignedPatient, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
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
    public GuiTabType tabType() {
        return GuiTabType.INFIRMARY;
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

    protected ArrayList<Person> getSelectedAssignedPatients() {
        ArrayList<Person> patients = new ArrayList<Person>();
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

    protected ArrayList<Person> getSelectedUnassignedPatients() {
        ArrayList<Person> patients = new ArrayList<Person>();
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
                                || (getCampaign().getCampaignOptions().useAdvancedMedical() && p.needsAMFixing()))
                        && (getCampaign().getPatientsFor(doctor) < 25)
                        && (getCampaign().getTargetFor(p, doctor).getValue() != TargetRoll.IMPOSSIBLE)) {
                    p.setDoctorId(doctor.getId(), getCampaign().getCampaignOptions().getHealingWaitingPeriod());
                    break;
                }
            }

        } else {
            for (Person p : selectedPatients) {
                if ((null != p)
                        && (p.needsFixing()
                                || (getCampaign().getCampaignOptions().useAdvancedMedical() && p.needsAMFixing()))
                        && (getCampaign().getPatientsFor(doctor) < 25)
                        && (getCampaign().getTargetFor(p, doctor).getValue() != TargetRoll.IMPOSSIBLE)) {
                    p.setDoctorId(doctor.getId(), getCampaign().getCampaignOptions().getHealingWaitingPeriod());
                }
            }
        }
        getCampaignGui().refreshTechsList();
        refreshDoctorsList();
        refreshPatientList();
    }

    private void unassignDoctor() {
        for (Person p : getSelectedAssignedPatients()) {
            if ((null != p)) {
                p.setDoctorId(null, getCampaign().getCampaignOptions().getNaturalHealingWaitingPeriod());
            }
        }

        getCampaignGui().refreshTechsList();
        refreshDoctorsList();
        refreshPatientList();
    }

    public void refreshDoctorsList() {
        int selected = docTable.getSelectedRow();
        doctorsModel.setData(getCampaign().getDoctors());
        if ((selected > -1) && (selected < getCampaign().getDoctors().size())) {
            docTable.setRowSelectionInterval(selected, selected);
        }
    }

    public void refreshPatientList() {
        Person doctor = getSelectedDoctor();
        ArrayList<Person> assigned = new ArrayList<Person>();
        ArrayList<Person> unassigned = new ArrayList<Person>();
        for (Person patient : getCampaign().getPatients()) {
            // Knock out inactive doctors
            if ((patient != null) && (patient.getDoctorId() != null)
                    && (getCampaign().getPerson(patient.getDoctorId()) != null)
                    && getCampaign().getPerson(patient.getDoctorId()).isInActive()) {
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

}

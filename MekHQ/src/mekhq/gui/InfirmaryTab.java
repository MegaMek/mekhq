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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.OptimizeInfirmaryAssignments;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.MedicPoolChangedEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.persons.PersonMedicalAssignmentEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.MedicalViewDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.DocTableModel;
import mekhq.gui.model.PatientTableModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.sorter.PersonTitleSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * Shows injured and medical personnel
 */
public final class InfirmaryTab extends CampaignGuiTab {
    private JTable docTable;
    private RoundedJButton btnAssignDoc;
    private RoundedJButton btnUnassignDoc;
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

        String bgImageFile = getIconPackage().getGuiElement("infirmary_background");
        if (null != bgImageFile && !bgImageFile.isEmpty()) {
            bgImage = Toolkit.getDefaultToolkit().createImage(bgImageFile);
        }

        doctorsModel = new DocTableModel(getCampaign());
        docTable = new JTable(doctorsModel);
        docTable.setRowHeight(UIUtil.scaleForGUI(60));
        docTable.getColumnModel().getColumn(0).setCellRenderer(doctorsModel.getRenderer());
        docTable.getSelectionModel().addListSelectionListener(ev -> docTableValueChanged());
        docTable.setOpaque(false);
        JScrollPane scrollDocTable = new JScrollPaneWithSpeed(docTable);
        scrollDocTable.setBorder(RoundedLineBorder.createRoundedLineBorder());
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
        btnAssignDoc = new RoundedJButton(resourceMap.getString("btnAssignDoc.text"));
        btnAssignDoc.setToolTipText(resourceMap.getString("btnAssignDoc.toolTipText"));
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.addActionListener(ev -> assignDoctor());

        btnUnassignDoc = new RoundedJButton(resourceMap.getString("btnUnassignDoc.text"));
        btnUnassignDoc.setEnabled(false);
        btnUnassignDoc.addActionListener(ev -> unassignDoctor());

        RoundedJButton btnOptimizeAssignments = new RoundedJButton(resourceMap.getString("btnOptimizeAssignments.text"));
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
        listAssignedPatient.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = listAssignedPatient.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        listAssignedPatient.setSelectedIndex(index);
                        Person selectedPatient = listAssignedPatient.getSelectedValue();
                        if (selectedPatient != null) {
                            MedicalViewDialog medicalViewDialog = new MedicalViewDialog(null,
                                  getCampaign(),
                                  selectedPatient);
                            medicalViewDialog.setVisible(true);
                        }
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Check for double-click with left mouse button
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = listAssignedPatient.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        listAssignedPatient.setSelectedIndex(index);
                        Person selectedPatient = listAssignedPatient.getSelectedValue();
                        if (selectedPatient != null) {
                            getCampaignGui().focusOnPerson(selectedPatient.getId());
                        }
                    }
                }
            }
        });

        JScrollPane scrollAssignedPatient = new JScrollPaneWithSpeed(listAssignedPatient);
        scrollAssignedPatient.setBorder(null);
        scrollAssignedPatient.setMinimumSize(new Dimension(300, 360));
        scrollAssignedPatient.setPreferredSize(new Dimension(300, 360));
        scrollAssignedPatient.setOpaque(false);
        scrollAssignedPatient.getViewport().setOpaque(false);
        listAssignedPatient.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
              "panAssignedPatient.title")));

        unassignedPatientModel = new PatientTableModel(getCampaign());
        listUnassignedPatient = new JList<>(unassignedPatientModel);
        listUnassignedPatient.setCellRenderer(unassignedPatientModel.getRenderer());
        listUnassignedPatient.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listUnassignedPatient.setVisibleRowCount(-1);
        listUnassignedPatient.getSelectionModel().addListSelectionListener(ev -> updateAssignDoctorEnabled());
        listUnassignedPatient.setOpaque(false);
        listUnassignedPatient.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = listUnassignedPatient.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        listUnassignedPatient.setSelectedIndex(index);
                        Person selectedPatient = listUnassignedPatient.getSelectedValue();
                        if (selectedPatient != null) {
                            MedicalViewDialog medicalViewDialog = new MedicalViewDialog(null,
                                  getCampaign(),
                                  selectedPatient);
                            medicalViewDialog.setVisible(true);
                        }
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Check for double-click with left mouse button
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = listUnassignedPatient.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        listUnassignedPatient.setSelectedIndex(index);
                        Person selectedPatient = listUnassignedPatient.getSelectedValue();
                        if (selectedPatient != null) {
                            getCampaignGui().focusOnPerson(selectedPatient.getId());
                        }
                    }
                }
            }
        });

        JScrollPane scrollUnassignedPatient = new JScrollPaneWithSpeed(listUnassignedPatient);
        scrollUnassignedPatient.setBorder(null);
        scrollUnassignedPatient.setMinimumSize(new Dimension(300, 200));
        scrollUnassignedPatient.setPreferredSize(new Dimension(300, 300));
        scrollUnassignedPatient.setOpaque(false);
        scrollUnassignedPatient.getViewport().setOpaque(false);
        listUnassignedPatient.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString(
              "panUnassignedPatient.title")));

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

        JPanel pnlTutorial = new TutorialHyperlinkPanel("infirmary");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(10, 5, 5, 5);
        add(pnlTutorial, gridBagConstraints);

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
        g.drawImage(bgImage, 0, 0, size, size, (img, infoFlags, x, y, width, height) -> {
            if ((infoFlags & ImageObserver.ALLBITS) != 0) {
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

    /**
     * Updates the enabled or disabled state of the doctor assignment-related buttons.
     *
     * <p>This method determines the current eligibility of the "Assign Doctor" and "Unassign Doctor"
     * buttons based on the following conditions:</p>
     * <ul>
     *   <li>If a doctor is selected, it calculates their medical capacity using the campaign
     *       options (e.g., maximum number of patients and administration usage). The "Assign Doctor"
     *       button is enabled if the doctor has available capacity and there are unassigned
     *       patients in the system.</li>
     *   <li>If no doctor is selected, the "Assign Doctor" button is disabled.</li>
     *   <li>The "Unassign Doctor" button is enabled if one or more assigned patients are selected.</li>
     * </ul>
     *
     * <p>This ensures that buttons in the UI reflect whether valid actions can be performed
     * based on the current application state.</p>
     */
    private void updateAssignDoctorEnabled() {
        Person doctor = getSelectedDoctor();

        if (doctor == null) {
            btnAssignDoc.setEnabled(false);
        } else {
            boolean canAssignToDoctor = canAssignToDoctor(doctor);
            btnAssignDoc.setEnabled(unassignedPatientModel.getSize() > 0 && canAssignToDoctor);
        }

        btnUnassignDoc.setEnabled(!getSelectedAssignedPatients().isEmpty());
    }

    /**
     * Determines if the given doctor can be assigned an additional patient.
     *
     * <p>This method checks whether assigning another patient to the specified doctor is within both the doctor's
     * individual capacity and the global MASH theatre capacity (if MASH theatres are being used). The doctor's capacity
     * is calculated based on campaign options and the doctor's qualifications. The global theatre constraint is only
     * considered if MASH theatres are enabled.</p>
     *
     * @param doctor the {@link Person} representing the doctor to check for assignment eligibility
     *
     * @return {@code true} if the doctor can be assigned another patient according to all capacity constraints
     *
     * @author Illiani
     * @since 0.50.10
     */
    private boolean canAssignToDoctor(Person doctor) {
        final CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        final int baseBedCount = campaignOptions.getMaximumPatients();
        final boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();

        final int doctorCapacity = doctor.getDoctorMedicalCapacity(isDoctorsUseAdministration, baseBedCount);
        final int patientsForDoctor = getCampaign().getPatientsFor(doctor);
        final boolean isWithinDoctorCapacity = doctorCapacity > patientsForDoctor;

        boolean useMASHTheatres = campaignOptions.isUseMASHTheatres();
        boolean isWithinTheatreCapacity = !useMASHTheatres;
        if (useMASHTheatres) {
            final int mashTheatreCapacity = getCampaign().getMashTheatreCapacity();
            final int patientsAssignedToDoctors = getCampaign().getPatientsAssignedToDoctors().size();
            isWithinTheatreCapacity = mashTheatreCapacity > patientsAssignedToDoctors;
        }

        return isWithinDoctorCapacity && isWithinTheatreCapacity;
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

        final CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        final int healingWaitingPeriod = campaignOptions.getHealingWaitingPeriod();

        boolean canAssignToDoctor = canAssignToDoctor(doctor);
        Collection<Person> selectedPatients = getSelectedUnassignedPatients();
        if (selectedPatients.isEmpty()) {
            // Pick the first in the list ... if there are any
            int patientSize = unassignedPatientModel.getSize();
            for (int i = 0; i < patientSize; ++i) {
                Person patient = unassignedPatientModel.getElementAt(i);

                if (null != patient && patient.needsFixing() && canAssignToDoctor) {
                    patient.setDoctorId(doctor.getId(), healingWaitingPeriod);
                    MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, patient));
                    break;
                }
            }
        } else {
            for (Person patient : selectedPatients) {
                if (null != patient && patient.needsFixing() && canAssignToDoctor) {
                    patient.setDoctorId(doctor.getId(), healingWaitingPeriod);
                    MekHQ.triggerEvent(new PersonMedicalAssignmentEvent(doctor, patient));
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
            if ((patient.getDoctorId() != null) &&
                      (getCampaign().getPerson(patient.getDoctorId()) != null) &&
                      !getCampaign().getPerson(patient.getDoctorId()).getStatus().isActiveFlexible()) {
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

    private final ActionScheduler doctorListScheduler = new ActionScheduler(this::refreshDoctorsList);
    private final ActionScheduler patientListScheduler = new ActionScheduler(this::refreshPatientList);

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

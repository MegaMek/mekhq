/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getFormattedText;

import java.awt.BorderLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import mekhq.campaign.personnel.Person;
import mekhq.gui.control.EditMedicalLogControl;

/**
 * A dialog for editing a person's medical log.
 *
 * <p>This dialog provides an interface for viewing and modifying the medical history of the provided character. It
 * uses the {@link EditMedicalLogControl} to handle the actual editing functionality.</p>
 *
 * @author Illiani
 * @since 0.50.05
 */
public class EditMedicalLogDialog extends JDialog {
    private final JFrame frame;
    private final Person person;
    private final LocalDate today;

    /**
     * Constructs a new dialog for editing a person's medical log.
     *
     * @param parent the parent frame for this dialog
     * @param person the person whose medical log is being edited
     * @param today  the current campaign date
     *
     * @author Illiani
     * @since 0.50.05
     */
    public EditMedicalLogDialog(JFrame parent, LocalDate today, Person person) {
        super(parent, true);

        this.frame = parent;
        this.today = today;
        this.person = person;

        initComponents();
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes the dialog components.
     *
     * <p>Sets up the dialog's basic properties, creates and adds the medical log editing control, and adds a
     * confirmation button to close the dialog.</p>
     *
     * @author Illiani
     * @since 0.50.05
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("EditPersonnelLogDialog");
        setTitle(getFormattedText("medicalLog.dialog.title", person.getFullTitle()));
        getContentPane().setLayout(new BorderLayout());

        EditMedicalLogControl editMedicalLogControl = new EditMedicalLogControl(frame, person, today);
        getContentPane().add(editMedicalLogControl, BorderLayout.CENTER);

        JButton btnOK = new JButton();
        btnOK.setName("btnOK");
        btnOK.setText(getFormattedText("medicalLog.btnOK.text"));
        btnOK.addActionListener(x -> this.dispose());
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }
}

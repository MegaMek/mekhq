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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.swing.util.UIUtil.scaleForGUI;
import static mekhq.MHQConstants.FORCE_ICON_PATH;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.utilities.ImageUtilities;
import mekhq.gui.utilities.RoundedLineBorder;

/**
 * A dialog that provides introductory information for new players starting the quickstart campaign. This dialog
 * displays an image, title, in-character information about "The Learning Ropes", a confirmation button, and
 * out-of-character guidance about the quickstart experience.
 *
 * <p>The dialog is modal and will block until the user confirms or closes it.</p>
 *
 * @author Illiani
 * @since 0.50.05
 */
public class NewPlayerQuickstartDialog extends JDialog {
    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    private static int PADDING = scaleForGUI(10);
    private static int DIALOG_WIDTH = scaleForGUI(750);
    private static int IMAGE_WIDTH = scaleForGUI(256);
    private static String NEW_PLAYER_QUICKSTART_ADDRESS = FORCE_ICON_PATH + "/Units/The Learning Ropes.png";

    /**
     * Creates a new dialog for providing information about the new player quickstart campaign. The dialog is modal and
     * will block until the user confirms or closes it.
     *
     * @param parent The parent frame that owns this dialog
     *
     * @author Illiani
     * @since 0.50.05
     */
    public NewPlayerQuickstartDialog(Frame parent) {
        super(parent, getFormattedTextAt("mekhq.resources.NewPlayerQuickstartDialog",
              "NewPlayerQuickstartDialog.header"), true);
        initComponents();
        setPreferredSize(new Dimension(DIALOG_WIDTH, (int) (getPreferredSize().height * 1.1)));
        setMinimumSize(new Dimension(DIALOG_WIDTH, (int) (getPreferredSize().height * 1.1)));
        pack();
        setLocationRelativeTo(parent);
        setResizable(true);
        setVisible(true);
    }

    /**
     * Initializes all the UI components of the dialog.
     *
     * <p>This includes:</p>
     * <ul>
     *     <li>Unit image at the top</li>
     *     <li>Title and in-character description</li>
     *     <li>Confirmation button</li>
     *     <li>Out-of-character information in an etched border box</li>
     * </ul>
     *
     * <p>Also sets up event listeners for dialog closing and button clicks.</p>
     *
     * @author Illiani
     * @since 0.50.05
     */
    private void initComponents() {
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        // Create panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Add the unit icon to the top
        ImageIcon icon = new ImageIcon(NEW_PLAYER_QUICKSTART_ADDRESS);
        icon = ImageUtilities.scaleImageIcon(icon, IMAGE_WIDTH, true);
        JLabel imageLabel = new JLabel(icon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(imageLabel);
        mainPanel.add(Box.createRigidArea(scaleForGUI(0, PADDING)));

        // Add In Character Text
        JLabel lblTitle = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "NewPlayerQuickstartDialog.title"));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblTitle);

        String fontStyle = "font-family: Noto Sans;";
        JLabel lblInCharacter = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "NewPlayerQuickstartDialog.inCharacter",
              DIALOG_WIDTH,
              fontStyle));
        setFontScaling(lblInCharacter, false, 1.1);
        lblInCharacter.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lblInCharacter);

        mainPanel.add(Box.createRigidArea(scaleForGUI(0, PADDING)));

        // Add Confirm button
        JButton confirmButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "NewPlayerQuickstartDialog.button"));
        confirmButton.setBorder(RoundedLineBorder.createRoundedLineBorder());
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(confirmButton);
        mainPanel.add(Box.createRigidArea(scaleForGUI(0, PADDING)));

        // Add OOC Text
        JLabel lblOutOfCharacter = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "NewPlayerQuickstartDialog.outOfCharacter",
              DIALOG_WIDTH));
        setFontScaling(lblOutOfCharacter, false, 1);
        lblOutOfCharacter.setBorder(RoundedLineBorder.createRoundedLineBorder());
        lblOutOfCharacter.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lblOutOfCharacter);

        add(mainPanel, BorderLayout.CENTER);

        // Handle dialog closing events
        confirmButton.addActionListener(e -> dispose());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }
}

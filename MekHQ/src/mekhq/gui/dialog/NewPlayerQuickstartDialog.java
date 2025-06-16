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

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.MHQConstants.FORCE_ICON_PATH;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.*;

import megamek.utilities.ImageUtilities;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

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

    private static final int PADDING = scaleForGUI(10);
    private static final int DIALOG_WIDTH = scaleForGUI(750);
    private static final Dimension IN_CHARACTER_TEXT_SIZE = scaleForGUI(750, 250);
    private static final int IMAGE_WIDTH = scaleForGUI(256);
    private static final Dimension BUTTON_SIZE = scaleForGUI(100, 30);
    private static final String NEW_PLAYER_QUICKSTART_ADDRESS = FORCE_ICON_PATH + "/Units/The Learning Ropes.png";

    private boolean wasCanceled = true;

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
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(DIALOG_WIDTH, (int) (getPreferredSize().height * 1.1)));
        pack();
        setLocationRelativeTo(parent);
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
        buildTopPanel();
        buildBottomPanel();
    }

    private void buildTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Add Unit Icon
        ImageIcon icon = new ImageIcon(NEW_PLAYER_QUICKSTART_ADDRESS);
        icon = ImageUtilities.scaleImageIcon(icon, IMAGE_WIDTH, true);
        JLabel imageLabel = new JLabel(icon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(imageLabel);
        topPanel.add(Box.createRigidArea(scaleForGUI(0, PADDING)));

        // Add In Character Text
        JLabel lblTitle = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "NewPlayerQuickstartDialog.title"));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(lblTitle);

        String fontStyle = "font-family: Noto Sans;";
        String htmlInCharacter = getFormattedTextAt(RESOURCE_BUNDLE,
              "NewPlayerQuickstartDialog.inCharacter",
              DIALOG_WIDTH,
              fontStyle);

        JTextPane txtInCharacter = new JTextPane();
        txtInCharacter.setContentType("text/html");
        txtInCharacter.setText(htmlInCharacter);
        txtInCharacter.setEditable(false);
        txtInCharacter.setOpaque(false);
        txtInCharacter.setBorder(BorderFactory.createEmptyBorder(0, PADDING, 0, PADDING));
        setFontScaling(txtInCharacter, false, 1.1);

        JScrollPane scrollPaneInCharacter = new JScrollPane(txtInCharacter);
        scrollPaneInCharacter.setBorder(null);
        scrollPaneInCharacter.setPreferredSize(IN_CHARACTER_TEXT_SIZE);
        scrollPaneInCharacter.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(scrollPaneInCharacter);

        add(topPanel, BorderLayout.CENTER);
    }

    private void buildBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, PADDING, 0, PADDING));

        // Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        RoundedJButton cancelButton = new RoundedJButton(getFormattedTextAt(RESOURCE_BUNDLE,
              "NewPlayerQuickstartDialog.button.cancel"));
        cancelButton.setPreferredSize(BUTTON_SIZE);
        cancelButton.addActionListener(e -> {
            wasCanceled = true;
            dispose();
        });
        buttonPanel.add(cancelButton);

        RoundedJButton confirmButton = new RoundedJButton(getFormattedTextAt(RESOURCE_BUNDLE,
              "NewPlayerQuickstartDialog.button.confirm"));
        confirmButton.setPreferredSize(BUTTON_SIZE);
        confirmButton.addActionListener(e -> {
            wasCanceled = false;
            dispose();
        });
        buttonPanel.add(confirmButton);

        bottomPanel.add(buttonPanel);

        // OOC label
        JLabel lblOutOfCharacter = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "NewPlayerQuickstartDialog.outOfCharacter",
              DIALOG_WIDTH));
        setFontScaling(lblOutOfCharacter, false, 1);
        lblOutOfCharacter.setBorder(
              BorderFactory.createCompoundBorder(
                    RoundedLineBorder.createRoundedLineBorder(),
                    BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
              )
        );
        lblOutOfCharacter.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottomPanel.add(Box.createRigidArea(new Dimension(0, PADDING)));
        bottomPanel.add(lblOutOfCharacter);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public boolean wasCanceled() {
        return wasCanceled;
    }
}

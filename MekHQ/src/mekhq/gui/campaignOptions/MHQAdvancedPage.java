/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import static megamek.client.ui.util.FontHandler.symbolIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.setSmallSizeVariant;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.client.ui.Messages;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.helpDialogs.HelpDialog;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The Advanced page of the MekHQ Client Options dialog: the user-data directory (with chooser and help buttons), the
 * game start-up delay/retry spinners, and the default company-generation method. Needs the hosting {@link JFrame} for
 * the directory chooser and help dialogs. Owns its controls and copies them back into the shared
 * {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQAdvancedPage extends MHQOptionsPage {
    private static final MMLogger LOGGER = MMLogger.create(MHQAdvancedPage.class);

    private final JFrame frame;

    // Advanced
    private JTextField userDirField;
    private CampaignOptionsSpinner spinnerStartGameDelay;
    private CampaignOptionsSpinner spinnerStartGameClientDelay;
    private CampaignOptionsSpinner spinnerStartGameClientRetryCount;
    private CampaignOptionsSpinner spinnerStartGameBotClientDelay;
    private CampaignOptionsSpinner spinnerStartGameBotClientRetryCount;
    private MMComboBox<CompanyGenerationMethod> comboDefaultCompanyGenerationMethod;

    MHQAdvancedPage(MHQOptionsModel model, JFrame frame) {
        super(model);
        this.frame = frame;
    }

    @Override
    Component createPage() {
        CampaignOptionsLabel userDirLabel = new CampaignOptionsLabel(RESOURCE_BUNDLE, "lblUserDir");
        userDirField = new JTextField(model.userDir, 20);
        userDirField.setName("txtUserDir");
        userDirField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "lblUserDir.toolTipText"));
        JButton userDirChooser = new JButton();
        userDirChooser.setName("btnUserDirChooser");
        userDirChooser.setToolTipText(getTextAt(RESOURCE_BUNDLE, "userDirChooser.title"));
        // Material Symbols "folder_open" glyph (https://fonts.google.com/icons), matching the icon buttons elsewhere in
        // the dialog, in place of a "..." text button. Sized a little above the label font so it fills the enlarged
        // square chooser button.
        userDirChooser.setIcon(symbolIcon(0xE2C8, userDirChooser.getFont().getSize() + UIUtil.scaleForGUI(2),
              userDirChooser.getForeground()));
        userDirChooser.addActionListener(evt -> CommonSettingsDialog.fileChooseUserDir(userDirField, frame));

        JButton userDirHelp = new JButton("Help");
        userDirHelp.setName("btnUserDirHelp");
        // MekHQ ships its own copy of the user-directory help under docs/Customization/MekHQ; MMConstants points at
        // MegaMek's docs/Customization/UserDir path, which is absent from MekHQ's working directory.
        String userDirHelpPath = "docs/Customization/MekHQ/UserDirHelp.html";
        try {
            String helpTitle = Messages.getString("UserDirHelpDialog.title");
            URL helpFile = new File(userDirHelpPath).toURI().toURL();
            userDirHelp.addActionListener(evt -> new HelpDialog(helpTitle, helpFile, frame).setVisible(true));
        } catch (MalformedURLException ex) {
            LOGGER.error("Could not find the user data directory help file at {}", userDirHelpPath);
        }

        // Render Help a touch more compact, and enlarge the icon-only chooser to a square as tall as the path field so
        // it is an easier click target.
        setSmallSizeVariant(userDirHelp);
        int chooserSide = userDirField.getPreferredSize().height;
        Dimension chooserSize = new Dimension(chooserSide, chooserSide);
        userDirChooser.setPreferredSize(chooserSize);
        userDirChooser.setMinimumSize(chooserSize);
        userDirChooser.setMaximumSize(chooserSize);

        // Let the path field fill the control column so its left edge lines up with the spinners below (and its right
        // edge, past the buttons, with theirs) instead of sitting at a fixed width nudged over by FlowLayout's leading
        // gap. The chooser and help buttons sit at the row's right end.
        JPanel userDirButtons = new JPanel();
        userDirButtons.setLayout(new BoxLayout(userDirButtons, BoxLayout.LINE_AXIS));
        userDirButtons.setOpaque(false);
        userDirButtons.add(userDirChooser);
        userDirButtons.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(6)));
        userDirButtons.add(userDirHelp);

        JPanel userDirControls = new JPanel(new BorderLayout(UIUtil.scaleForGUI(6), 0));
        userDirControls.setOpaque(false);
        userDirControls.add(userDirField, BorderLayout.CENTER);
        userDirControls.add(userDirButtons, BorderLayout.LINE_END);

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQAdvancedContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addRow(userDirLabel, userDirControls);
        spinnerStartGameDelay = advancedSpinner(panel, "lblStartGameDelay", 1000, 250, 2500, 25, model.startGameDelay);
        spinnerStartGameClientDelay =
              advancedSpinner(panel, "lblStartGameClientDelay", 50, 50, 2500, 25, model.startGameClientDelay);
        spinnerStartGameClientRetryCount = advancedSpinner(panel, "lblStartGameClientRetryCount", 1000, 100, 2500, 50,
              model.startGameClientRetryCount);
        spinnerStartGameBotClientDelay =
              advancedSpinner(panel, "lblStartGameBotClientDelay", 50, 50, 2500, 25, model.startGameBotClientDelay);
        spinnerStartGameBotClientRetryCount = advancedSpinner(panel, "lblStartGameBotClientRetryCount", 250, 100, 2500,
              50, model.startGameBotClientRetryCount);

        CampaignOptionsLabel companyGenerationLabel =
              new CampaignOptionsLabel(RESOURCE_BUNDLE, "lblDefaultCompanyGenerationMethod");
        comboDefaultCompanyGenerationMethod =
              new MMComboBox<>("comboDefaultCompanyGenerationMethod", CompanyGenerationMethod.values());
        comboDefaultCompanyGenerationMethod.setSelectedItem(model.defaultCompanyGenerationMethod);
        comboDefaultCompanyGenerationMethod.setToolTipText(
              getTextAt(RESOURCE_BUNDLE, "lblDefaultCompanyGenerationMethod.toolTipText"));
        comboDefaultCompanyGenerationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CompanyGenerationMethod method) {
                    list.setToolTipText(method.getToolTipText());
                }
                return this;
            }
        });
        panel.addRow(companyGenerationLabel, comboDefaultCompanyGenerationMethod);

        Component page = buildMHQPage("MHQAdvancedPage", "lblMHQAdvancedSection.text", "lblMHQAdvancedSection.summary",
              panel);
        created = true;
        return page;
    }

    /**
     * Creates a labelled integer spinner, initialises it to {@code value}, adds it to {@code panel} as a row, and
     * returns it so the caller can store it in a field for {@link #writeToModel()}.
     */
    private CampaignOptionsSpinner advancedSpinner(CampaignOptionsFormPanel panel, String labelKey, int defaultValue,
          int minimum, int maximum, int step, int value) {
        CampaignOptionsLabel label = new CampaignOptionsLabel(RESOURCE_BUNDLE, labelKey);
        CampaignOptionsSpinner spinner =
              new CampaignOptionsSpinner(RESOURCE_BUNDLE, labelKey, defaultValue, minimum, maximum, step);
        spinner.setValue(value);
        panel.addRow(label, spinner);
        return spinner;
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        model.userDir = userDirField.getText();
        model.startGameDelay = (int) spinnerStartGameDelay.getValue();
        model.startGameClientDelay = (int) spinnerStartGameClientDelay.getValue();
        model.startGameClientRetryCount = (int) spinnerStartGameClientRetryCount.getValue();
        model.startGameBotClientDelay = (int) spinnerStartGameBotClientDelay.getValue();
        model.startGameBotClientRetryCount = (int) spinnerStartGameBotClientRetryCount.getValue();
        model.defaultCompanyGenerationMethod =
              Objects.requireNonNull(comboDefaultCompanyGenerationMethod.getSelectedItem());
    }
}

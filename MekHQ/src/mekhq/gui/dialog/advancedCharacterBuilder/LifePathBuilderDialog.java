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
package mekhq.gui.dialog.advancedCharacterBuilder;

import static java.lang.Math.round;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import megamek.client.ui.util.JTextAreaWithCharacterLimit;
import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class LifePathBuilderDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";

    private static final int MINIMUM_COMPONENT_WIDTH = scaleForGUI(200);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(600);
    private static final Dimension SIDE_PANEL_MINIMUM_SIZE = new Dimension(MINIMUM_COMPONENT_WIDTH,
          MINIMUM_COMPONENT_HEIGHT);
    private static final Dimension MINIMUM_SIZE = new Dimension(MINIMUM_COMPONENT_WIDTH * 6,
          MINIMUM_COMPONENT_HEIGHT);
    private static final int PADDING = scaleForGUI(10);

    private static final int TOOLTIP_PANEL_WIDTH = (int) round(MINIMUM_SIZE.width * 0.95);
    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_COMPONENT_WIDTH * 0.95);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private JScrollPane scrollInstructions;
    private JEditorPane txtInstructions;

    private JLabel lblTooltipDisplay;

    private JScrollPane scrollProgress;
    private JEditorPane txtProgressBasic;

    private JTextArea txtName;
    private JTextArea txtFlavorText;
    private JSpinner spnAge;
    private JSpinner spnDiscount;
    private Set<ATOWLifeStage> lifeStages = new HashSet<>();
    private Set<LifePathCategory> categories = new HashSet<>();

    public LifePathBuilderDialog(Frame owner) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.title"), true);
        JPanel contents = initialize();

        SwingUtilities.invokeLater(() -> scrollInstructions.getVerticalScrollBar().setValue(0));
        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));

        setContentPane(contents);
        setMinimumSize(MINIMUM_SIZE);
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void setTxtInstructions(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH, newText);
        txtInstructions.setText(newTooltipText);

        SwingUtilities.invokeLater(() -> scrollInstructions.getVerticalScrollBar().setValue(0));
    }

    private void setTxtProgressBasic(String newText) {
        txtProgressBasic.setText(newText);
        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));
    }

    private void setLblTooltipDisplay(String newText) {
        String newTooltipText = String.format(PANEL_HTML_FORMAT, TOOLTIP_PANEL_WIDTH, newText);
        lblTooltipDisplay.setText(newTooltipText);
    }

    private void updateTxtProgressBasic() {
        String name = txtName.getText();
        String flavorText = txtFlavorText.getText();
        int age = (int) spnAge.getValue();
        int discount = (int) spnDiscount.getValue();

        StringBuilder lifeStageText = new StringBuilder();
        for (ATOWLifeStage lifeStage : lifeStages) {
            if (lifeStageText.toString().isBlank()) {
                lifeStageText = new StringBuilder(lifeStage.getDisplayName());
            } else {
                lifeStageText.append(", ").append(lifeStage.getDisplayName());
            }
        }

        StringBuilder categoriesText = new StringBuilder();
        for (LifePathCategory category : categories) {
            if (categoriesText.toString().isBlank()) {
                categoriesText = new StringBuilder(category.getDisplayName());
            } else {
                categoriesText.append(", ").append(category.getDisplayName());
            }
        }

        String newText = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.progress.basic", name,
              flavorText, age, discount, lifeStageText.toString(), categoriesText.toString());

        String newProgressText = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH, newText);
        setTxtProgressBasic(newProgressText);

        SwingUtilities.invokeLater(() -> scrollProgress.getVerticalScrollBar().setValue(0));
    }

    private JPanel initialize() {
        JPanel pnlInstructions = initializeInstructionsPanel();
        EnhancedTabbedPane tabMain = initializeMainPanel();
        JPanel pnlProgress = initializeProgressPanel();

        // Layout using GridBagLayout for a width ratio of 1:2:1
        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 1.0;

        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0; // Instructions panel (1x width)
        gridBagConstraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        container.add(pnlInstructions, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 2.0; // Main panel (2x width)
        gridBagConstraints.insets = new Insets(PADDING, 0, PADDING, 0);
        container.add(tabMain, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.weightx = 1.0; // Progress panel (1x width)
        gridBagConstraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        container.add(pnlProgress, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(0, PADDING, PADDING, PADDING);
        container.add(initializeControlPanel(), gridBagConstraints);

        return container;
    }

    private JPanel initializeInstructionsPanel() {
        JPanel pnlInstructions = new JPanel();

        // Border
        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.instructions");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        // Text Area
        txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.instructions.basic"));
        txtInstructions.setText(instructions);

        // Scroll Pane
        scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        // Final Touches
        pnlInstructions.add(scrollInstructions);
        pnlInstructions.setMinimumSize(SIDE_PANEL_MINIMUM_SIZE);

        return pnlInstructions;
    }

    private EnhancedTabbedPane initializeMainPanel() {
        EnhancedTabbedPane tabMain = new EnhancedTabbedPane();
        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.lifePath");
        tabMain.setBorder(RoundedLineBorder.createRoundedLineBorder(title));

        buildBasicInformationTab(tabMain);

        // Requirements
        JPanel tabRequirements = new JPanel();
        tabRequirements.setName("requirements");
        String titleRequirements = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.requirements");
        tabMain.addTab(titleRequirements, tabRequirements);
        tabRequirements.add(new JLabel("First Tab Content"));

        // TODO Add Requirement Group
        // TODO Remove Requirement Group
        // TODO Add Requirement
        // TODO Remove Requirement

        // Exclusions
        JPanel tabExclusions = new JPanel();
        tabExclusions.setName("exclusions");
        String titleExclusions = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.exclusions");
        tabMain.addTab(titleExclusions, tabExclusions);
        tabExclusions.add(new JLabel("First Tab Content"));

        // TODO Add Exclusion
        // TODO Remove Exclusion

        // Fixed XP
        JPanel tabFixedXP = new JPanel();
        tabFixedXP.setName("fixedXP");
        String titleFixedXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.fixedXP");
        tabMain.addTab(titleFixedXP, tabFixedXP);
        tabFixedXP.add(new JLabel("First Tab Content"));

        // TODO Add Award
        // TODO Remove Award

        // Flexible XP
        JPanel tabFlexibleXP = new JPanel();
        tabFlexibleXP.setName("flexibleXP");
        String titleFlexibleXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.flexibleXP");
        tabMain.addTab(titleFlexibleXP, tabFlexibleXP);
        tabFlexibleXP.add(new JLabel("First Tab Content"));

        // TODO Add Award Group
        // TODO Remove Award Group
        // TODO Add Award
        // TODO Remove Award

        // Add a listener to handle tab selection changes
        tabMain.addChangeListener(e -> {
            int selectedIndex = tabMain.getSelectedIndex();
            Component selectedTab = tabMain.getComponentAt(selectedIndex);
            String tabName = selectedTab.getName();

            String tabInstructionsKey = "LifePathBuilderDialog.tab.instructions." + tabName;
            String instructions = getTextAt(RESOURCE_BUNDLE, tabInstructionsKey);

            setTxtInstructions(instructions);
        });

        return tabMain;
    }

    private void buildBasicInformationTab(EnhancedTabbedPane tabMain) {
        JPanel tabBasicInformation = new JPanel();
        tabBasicInformation.setName("basic");
        String titleBasic = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.tab.title.basic");

        final int DERIVED_WIDTH = (int) round(MINIMUM_COMPONENT_WIDTH * 2 * 0.9);

        // Name
        final String titleName = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.name.label");
        final String tooltipName = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.name.tooltip");
        JLabel lblName = new JLabel(titleName);
        txtName = JTextAreaWithCharacterLimit.createLimitedTextArea(50, 1);
        JScrollPane nameScroll = new JScrollPane(txtName);
        nameScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        nameScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        int rowHeight = txtName.getFontMetrics(txtName.getFont()).getHeight();
        Dimension nameSize = new Dimension(DERIVED_WIDTH - lblName.getWidth(),
              scaleForGUI(rowHeight + 12));
        nameScroll.setPreferredSize(nameSize);
        nameScroll.setMaximumSize(nameSize);

        lblName.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipName)
        );
        txtName.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipName)
        );
        DocumentChangeListenerUtil.addChangeListener(
              txtName.getDocument(),
              this::updateTxtProgressBasic
        );

        // Flavor Text
        final String titleFlavorText = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.flavorText.label");
        final String tooltipFlavorText = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.flavorText.tooltip");
        JLabel lblFlavorText = new JLabel(titleFlavorText);
        txtFlavorText = JTextAreaWithCharacterLimit.createLimitedTextArea(500, 1);
        JScrollPane flavorScroll = new JScrollPane(txtFlavorText);
        flavorScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        flavorScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rowHeight = txtFlavorText.getFontMetrics(txtFlavorText.getFont()).getHeight();
        Dimension flavorSize = new Dimension(DERIVED_WIDTH - lblFlavorText.getWidth(),
              scaleForGUI(rowHeight * 10 + 12));
        flavorScroll.setPreferredSize(flavorSize);
        flavorScroll.setMaximumSize(flavorSize);

        lblFlavorText.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipFlavorText)
        );
        flavorScroll.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipFlavorText)
        );
        DocumentChangeListenerUtil.addChangeListener(
              txtFlavorText.getDocument(),
              this::updateTxtProgressBasic
        );

        // Age Modifier
        final String titleAge = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.age.label");
        final String tooltipAge = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.age.tooltip");
        JLabel lblAge = new JLabel(titleAge);
        spnAge = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        Dimension ageSize = new Dimension(DERIVED_WIDTH - lblAge.getWidth(),
              spnAge.getPreferredSize().height);
        spnAge.setPreferredSize(ageSize);
        spnAge.setMaximumSize(ageSize);

        lblAge.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAge)
        );
        spnAge.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipAge)
        );
        spnAge.addChangeListener(e -> this.updateTxtProgressBasic());

        // XP Discount
        final String titleDiscount = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.discount.label");
        final String tooltipDiscount = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.discount.tooltip");
        JLabel lblDiscount = new JLabel(titleDiscount);
        spnDiscount = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        Dimension discountSize = new Dimension(DERIVED_WIDTH - lblDiscount.getWidth(),
              spnAge.getPreferredSize().height);
        spnDiscount.setPreferredSize(discountSize);
        spnDiscount.setMaximumSize(discountSize);

        lblDiscount.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipDiscount)
        );
        spnDiscount.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipDiscount)
        );
        spnDiscount.addChangeListener(e -> this.updateTxtProgressBasic());

        // Manage Life Stages
        final String titleManageLifeStages = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageLifeStages.label");
        final String tooltipManageLifeStages = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageLifeStages.tooltip");
        RoundedJButton btnManageLifeStages = createButton(this, titleManageLifeStages,
              tooltipManageLifeStages);
        btnManageLifeStages.addActionListener(e -> {
            this.setVisible(false);
            LifePathStagePicker picker = new LifePathStagePicker(lifeStages);
            lifeStages = picker.getSelectedLifeStages();
            this.updateTxtProgressBasic();
            this.setVisible(true);
        });

        // Manage Categories
        final String titleManageCategories = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageCategories.label");
        final String tooltipManageCategories = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageCategories.tooltip");
        RoundedJButton btnManageCategories = createButton(this, titleManageCategories,
              tooltipManageCategories);
        btnManageCategories.addActionListener(e -> {
            this.setVisible(false);
            LifePathCategoryPicker picker = new LifePathCategoryPicker(categories);
            categories = picker.getSelectedCategories();
            this.updateTxtProgressBasic();
            this.setVisible(true);
        });

        // Layout
        GroupLayout layout = new GroupLayout(tabBasicInformation);
        tabBasicInformation.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
              layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblName)
                                    .addComponent(lblFlavorText)
                                    .addComponent(lblAge)
                                    .addComponent(lblDiscount)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(nameScroll)
                                    .addComponent(flavorScroll)
                                    .addGroup(layout.createSequentialGroup()
                                                    .addComponent(spnAge)
                                    )
                                    .addGroup(layout.createSequentialGroup()
                                                    .addComponent(spnDiscount)
                                    )
                                    .addGroup(layout.createSequentialGroup()
                                                    .addComponent(btnManageLifeStages)
                                                    .addGap(PADDING)
                                                    .addComponent(btnManageCategories)
                                    )
                    )
        );

        layout.setVerticalGroup(
              layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblName)
                                    .addComponent(nameScroll)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblFlavorText)
                                    .addComponent(flavorScroll)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblAge)
                                    .addComponent(spnAge)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblDiscount)
                                    .addComponent(spnDiscount)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnManageLifeStages)
                                    .addGap(PADDING)
                                    .addComponent(btnManageCategories)
                    )
        );

        tabMain.addTab(titleBasic, tabBasicInformation);
    }

    private static RoundedJButton createButton(LifePathBuilderDialog dialogInstance, String label, String tooltip) {
        RoundedJButton button = new RoundedJButton(label);

        button.setMinimumSize(button.getPreferredSize());
        button.setMaximumSize(button.getPreferredSize());
        button.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(dialogInstance::setLblTooltipDisplay, tooltip)
        );

        return button;
    }

    private JPanel initializeProgressPanel() {
        JPanel pnlProgress = new JPanel(new BorderLayout());

        String titleProgress = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.panel.title.progress");
        pnlProgress.setBorder(createRoundedLineBorder(titleProgress));

        txtProgressBasic = new JEditorPane();
        txtProgressBasic.setContentType("text/html");
        txtProgressBasic.setEditable(false);
        updateTxtProgressBasic();

        scrollProgress = new FastJScrollPane(txtProgressBasic);
        scrollProgress.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollProgress.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollProgress.setBorder(null);

        pnlProgress.add(scrollProgress, BorderLayout.CENTER);

        pnlProgress.setMinimumSize(SIDE_PANEL_MINIMUM_SIZE);

        return pnlProgress;
    }

    private JPanel initializeControlPanel() {
        JPanel pnlControls = new JPanel(new GridBagLayout());
        pnlControls.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JPanel pnlContents = new JPanel();
        pnlContents.setLayout(new BoxLayout(pnlContents, BoxLayout.Y_AXIS));
        pnlContents.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        setLblTooltipDisplay("");
        lblTooltipDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancel.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        btnCancel.addActionListener(e -> dispose());

        String titleSave = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.save");
        RoundedJButton btnSave = new RoundedJButton(titleSave);
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.setMargin(new Insets(PADDING, 0, PADDING, 0));

        String titleLoad = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.button.load");
        RoundedJButton btnLoad = new RoundedJButton(titleLoad);
        btnLoad.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLoad.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));

        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnLoad);
        pnlButtons.add(Box.createHorizontalStrut(PADDING));
        pnlButtons.add(btnSave);
        pnlButtons.add(Box.createHorizontalGlue());

        pnlContents.add(Box.createVerticalGlue());
        pnlContents.add(lblTooltipDisplay);
        pnlContents.add(Box.createVerticalStrut(PADDING));
        pnlContents.add(pnlButtons);
        pnlContents.add(Box.createVerticalGlue());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlControls.add(pnlContents, gridBagConstraints);

        return pnlControls;
    }

    static class TooltipMouseListenerUtil {
        /**
         * Returns a {@link MouseAdapter} that calls the given setter with the provided text when the mouse enters.
         *
         * @param setter The consumer to call (e.g., dialog::setLblTooltips)
         * @param text   The text to set
         *
         * @return a {@link MouseAdapter} for use with any {@link JComponent}
         */
        public static MouseAdapter forTooltip(Consumer<String> setter, String text) {
            return new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setter.accept(text);
                }
            };
        }
    }

    private static class DocumentChangeListenerUtil {
        /**
         * Adds a change callback to the given {@link Document}.
         *
         * @param document the {@link Document} to listen to
         * @param onChange a consumer or runnable callback, called when the document changes
         */
        public static void addChangeListener(Document document, Runnable onChange) {
            document.addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    onChange.run();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    onChange.run();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    onChange.run();
                }
            });
        }
    }
}

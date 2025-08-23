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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.round;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderMinimumComponentWidth;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.util.JTextAreaWithCharacterLimit;
import megamek.common.EnhancedTabbedPane;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.dialog.advancedCharacterBuilder.DocumentChangeListenerUtil;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathBuilderTabBasicInformation {
    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int MINIMUM_COMPONENT_WIDTH = getLifePathBuilderMinimumComponentWidth();
    private final static int PADDING = getLifePathBuilderPadding();

    private final JTextArea txtName;
    private final JTextArea txtSource;
    private final JTextArea txtFlavorText;
    private final JSpinner spnAge;
    private final JSpinner spnDiscount;
    private List<ATOWLifeStage> lifeStages = new ArrayList<>();
    private List<LifePathCategory> categories = new ArrayList<>();

    public String getName() {
        return txtName.getText();
    }

    public String getFlavorText() {
        return txtFlavorText.getText();
    }

    public String getSource() {
        return txtSource.getText();
    }

    public int getAge() {
        return (int) spnAge.getValue();
    }

    public int getDiscount() {
        return (int) spnDiscount.getValue();
    }

    public List<ATOWLifeStage> getLifeStages() {
        return lifeStages;
    }

    public List<LifePathCategory> getCategories() {
        return categories;
    }

    LifePathBuilderTabBasicInformation(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain) {
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
        FastJScrollPane nameScroll = new FastJScrollPane(txtName);
        nameScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        nameScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        int rowHeight = txtName.getFontMetrics(txtName.getFont()).getHeight();
        Dimension nameSize = new Dimension(DERIVED_WIDTH - lblName.getWidth(),
              scaleForGUI(rowHeight + 12));
        nameScroll.setPreferredSize(nameSize);
        nameScroll.setMaximumSize(nameSize);

        lblName.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipName)
        );
        txtName.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipName)
        );
        DocumentChangeListenerUtil.addChangeListener(
              txtName.getDocument(),
              parent::updateTxtProgress
        );

        // Source
        final String titleSource = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.source.label");
        final String tooltipSource = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.source.tooltip");
        JLabel lblSource = new JLabel(titleSource);
        txtSource = JTextAreaWithCharacterLimit.createLimitedTextArea(50, 1);
        FastJScrollPane sourceScroll = new FastJScrollPane(txtSource);
        sourceScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sourceScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sourceScroll.setPreferredSize(nameSize);
        sourceScroll.setMaximumSize(nameSize);
        lblSource.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipSource)
        );
        txtSource.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipSource)
        );
        DocumentChangeListenerUtil.addChangeListener(
              txtSource.getDocument(),
              parent::updateTxtProgress
        );

        // Flavor Text
        final String titleFlavorText = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.flavorText.label");
        final String tooltipFlavorText = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.flavorText.tooltip");
        JLabel lblFlavorText = new JLabel(titleFlavorText);
        txtFlavorText = JTextAreaWithCharacterLimit.createLimitedTextArea(500, 1);
        FastJScrollPane flavorScroll = new FastJScrollPane(txtFlavorText);
        flavorScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        flavorScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rowHeight = txtFlavorText.getFontMetrics(txtFlavorText.getFont()).getHeight();
        Dimension flavorSize = new Dimension(DERIVED_WIDTH - lblFlavorText.getWidth(),
              scaleForGUI(rowHeight * 10 + 12));
        flavorScroll.setPreferredSize(flavorSize);
        flavorScroll.setMaximumSize(flavorSize);

        lblFlavorText.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipFlavorText)
        );
        flavorScroll.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipFlavorText)
        );
        DocumentChangeListenerUtil.addChangeListener(
              txtFlavorText.getDocument(),
              parent::updateTxtProgress
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
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAge)
        );
        spnAge.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAge)
        );
        spnAge.addChangeListener(e -> parent.updateTxtProgress());

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
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipDiscount)
        );
        spnDiscount.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipDiscount)
        );
        spnDiscount.addChangeListener(e -> parent.updateTxtProgress());

        // Manage Life Stages
        final String titleManageLifeStages = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageLifeStages.label");
        final String tooltipManageLifeStages = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageLifeStages.tooltip");
        RoundedJButton btnManageLifeStages = createButton(parent, titleManageLifeStages,
              tooltipManageLifeStages);
        btnManageLifeStages.addActionListener(e -> {
            parent.setVisible(false);
            LifePathStagePicker picker = new LifePathStagePicker(
                  lifeStages);
            lifeStages = picker.getSelectedLifeStages();
            parent.updateTxtProgress();
            parent.setVisible(true);
        });

        // Manage Categories
        final String titleManageCategories = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageCategories.label");
        final String tooltipManageCategories = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.basic.manageCategories.tooltip");
        RoundedJButton btnManageCategories = createButton(parent, titleManageCategories,
              tooltipManageCategories);
        btnManageCategories.addActionListener(e -> {
            parent.setVisible(false);
            LifePathCategorySingletonPicker picker = new LifePathCategorySingletonPicker(
                  categories);
            categories = picker.getSelectedCategories();
            parent.updateTxtProgress();
            parent.setVisible(true);
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
                                    .addComponent(lblSource)
                                    .addComponent(lblAge)
                                    .addComponent(lblDiscount)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(nameScroll)
                                    .addComponent(flavorScroll)
                                    .addComponent(sourceScroll)
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
                                    .addComponent(lblSource)
                                    .addComponent(sourceScroll)
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
}

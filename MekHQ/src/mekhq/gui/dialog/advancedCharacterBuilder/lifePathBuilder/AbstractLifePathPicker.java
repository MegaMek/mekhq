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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Shared scaffolding for the Life Path wizard's picker dialogs.
 *
 * <p>All nine pickers are the same dialog wearing different options: instructions down the left, the options to the
 * right, and a help line above Cancel, Clear All and Confirm along the bottom. Everything but the options panel lived
 * in all nine copies before this class existed, and four defects lived in all nine copies with it.</p>
 *
 * <p>What the shared version fixes:</p>
 *
 * <ul>
 *     <li><b>Closing with the window X kept the edits.</b> The X behaved as Confirm, because nothing handled it.
 *     Here it runs the same restore-and-close as Cancel.</li>
 *     <li><b>The dialogs had no owner.</b> A {@link JDialog} built with no owner is not tied to the wizard, which is
 *     why every call site used to hide the wizard before opening a picker and show it again afterwards. That nests a
 *     modal event loop per click and is the usual cause of a dialog appearing behind the main window. Taking an owner
 *     lets the call sites drop the hide and show entirely.</li>
 *     <li><b>The title never said which group was being edited.</b> With several requirement groups open it was
 *     guesswork. The title now reads, for example, "Trait Picker - Requirement Group 2".</li>
 *     <li><b>There was no way to clear a group.</b> Undoing a selection meant unticking every box by hand.</li>
 * </ul>
 *
 * <p>A subclass supplies its bundle, its preferred size, the options panel, and what Cancel and Clear All mean for
 * the selection it holds. It calls {@link #buildAndShow()} once its own state is ready, because the options panel is
 * built during that call and cannot run before the subclass's fields are set.</p>
 *
 * @since 0.50.11
 */
abstract class AbstractLifePathPicker extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AbstractLifePathPicker.class);

    /** Width of the instructions column, which is the same in every picker. */
    protected static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);

    /** Space around and between components. */
    protected static final int PADDING = scaleForGUI(10);

    /** Wrapper that gives HTML text a fixed width, so it wraps instead of running off the dialog. */
    protected static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private final String resourceBundle;
    private final String bundlePrefix;
    private final int minimumMainWidth;
    private final int minimumComponentHeight;
    private final LifePathBuilderTabType tabType;
    private final Integer groupIndex;

    private JLabel lblTooltipDisplay;
    private RoundedJButton btnConfirm;
    private JPanel pnlMain;
    private JPanel pnlOptions;

    /**
     * @param owner                  the wizard this picker belongs to, so it cannot be lost behind it; may be
     *                               {@code null}
     * @param resourceBundle         the subclass's resource bundle
     * @param bundlePrefix           the prefix its keys share, which is its own simple class name
     * @param tabType                the section being edited, which decides the instructions and the title suffix
     * @param groupIndex             the group being edited, shown in the title; {@code null} for a picker that edits
     *                               no particular group
     * @param minimumMainWidth       how wide the options column needs to be
     * @param minimumComponentHeight how tall the dialog needs to be
     *
     * @since 0.50.11
     */
    protected AbstractLifePathPicker(@Nullable Window owner, String resourceBundle, String bundlePrefix,
          @Nullable LifePathBuilderTabType tabType, @Nullable Integer groupIndex, int minimumMainWidth,
          int minimumComponentHeight) {
        super(owner, ModalityType.APPLICATION_MODAL);

        this.resourceBundle = resourceBundle;
        this.bundlePrefix = bundlePrefix;
        this.tabType = tabType;
        this.groupIndex = groupIndex;
        this.minimumMainWidth = minimumMainWidth;
        this.minimumComponentHeight = minimumComponentHeight;
    }

    /**
     * Builds the options panel.
     *
     * <p>Called from {@link #buildAndShow()}, so a subclass must have set up its own state before calling that.</p>
     *
     * @return the panel holding this picker's options
     *
     * @since 0.50.11
     */
    protected abstract JPanel buildOptionsPanel();

    /**
     * Puts the selection back to what it was when the picker opened.
     *
     * <p>Run by Cancel and by the window X, which now mean the same thing.</p>
     *
     * @since 0.50.11
     */
    protected abstract void restoreStoredSelection();

    /**
     * Empties the selection.
     *
     * <p>Run by Clear All. This is not Cancel: the picker stays open and the cleared state is kept if the author then
     * confirms.</p>
     *
     * @since 0.50.11
     */
    protected abstract void clearSelection();

    /**
     * Reports whether the current selection may be confirmed.
     *
     * <p>The default allows anything. A subclass overrides this when an empty selection would produce a Life Path
     * nothing can use.</p>
     *
     * @return {@code true} when Confirm should be available
     *
     * @since 0.50.11
     */
    protected boolean isConfirmEnabled() {
        return true;
    }

    /**
     * Re-reads {@link #isConfirmEnabled()} and enables or disables Confirm to match.
     *
     * <p>Safe to call before the button exists, so a subclass can call it from a selection listener that also runs
     * while the dialog is still being built.</p>
     *
     * @since 0.50.11
     */
    protected void refreshConfirmEnabled() {
        if (btnConfirm != null) {
            btnConfirm.setEnabled(isConfirmEnabled());
        }
    }

    /**
     * Returns the width HTML text in the help line should wrap at.
     *
     * @return the wrap width in pixels
     *
     * @since 0.50.11
     */
    protected int getTooltipPanelWidth() {
        return (int) round(minimumMainWidth * 0.75);
    }

    /**
     * Shows a line of help text above the buttons.
     *
     * @param newText the text to show, which may contain HTML
     *
     * @since 0.50.11
     */
    protected void setLblTooltipDisplay(String newText) {
        if (lblTooltipDisplay != null) {
            lblTooltipDisplay.setText(String.format(PANEL_HTML_FORMAT, getTooltipPanelWidth(), newText));
        }
    }

    /**
     * Looks a key up in this picker's own bundle.
     *
     * @param keySuffix the part of the key after the class name
     *
     * @return the text
     *
     * @since 0.50.11
     */
    protected String getPickerText(String keySuffix) {
        return getTextAt(resourceBundle, bundlePrefix + '.' + keySuffix);
    }

    /**
     * Assembles the dialog and shows it.
     *
     * <p>Call this last in a subclass constructor. It builds the options panel, so everything that panel reads has to
     * exist first.</p>
     *
     * @since 0.50.11
     */
    protected final void buildAndShow() {
        setTitle(buildTitle());

        JPanel pnlInstructions = buildInstructionsPanel();
        pnlOptions = buildOptionsPanel();
        JPanel pnlControls = buildControlPanel();

        refreshConfirmEnabled();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        mainPanel.add(pnlInstructions, constraints);

        pnlMain = new JPanel(new BorderLayout());
        pnlMain.add(pnlOptions, BorderLayout.CENTER);
        pnlMain.add(pnlControls, BorderLayout.SOUTH);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        mainPanel.add(pnlMain, constraints);

        // The X is Cancel, not Confirm. Without this the dialog simply disposed and whatever had been edited was
        // kept, which is the opposite of what a Cancel button next to it implies.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                cancelAndDispose();
            }
        });

        setContentPane(mainPanel);
        setMinimumSize(new Dimension((int) round((MINIMUM_INSTRUCTIONS_WIDTH + minimumMainWidth) * 1.25),
              minimumComponentHeight));
        pack();
        setLocationRelativeTo(getOwner());
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Rebuilds the options panel in place.
     *
     * <p>Needed after the selection changes behind the controls' backs, which is what Clear All does: the checkboxes
     * and spinners already on screen still show the old selection until they are built again.</p>
     *
     * @since 0.50.11
     */
    protected final void rebuildOptions() {
        if (pnlMain == null || pnlOptions == null) {
            return;
        }

        pnlMain.remove(pnlOptions);
        pnlOptions = buildOptionsPanel();
        pnlMain.add(pnlOptions, BorderLayout.CENTER);
        pnlMain.revalidate();
        pnlMain.repaint();
    }

    /**
     * Returns the dialog title, naming the section and the group being edited when there is one.
     *
     * <p>The section name comes from the {@code picker.section} keys, which are plain text. The tabbed pane's
     * {@code tab.title} keys are HTML, and a window title does not render HTML, so they cannot be reused here.
     * Fixed XP and Exclusions only ever have one group, so their titles carry no group number.</p>
     *
     * @return the title
     *
     * @since 0.50.11
     */
    private String buildTitle() {
        String baseTitle = getPickerText("title");

        if (groupIndex == null || tabType == null) {
            return baseTitle;
        }

        String sectionName = getTextAt("mekhq.resources.LifePathBuilderDialog",
              "LifePathBuilderDialog.picker.section." + tabType.getLookupName());

        boolean hasSingleGroup = tabType == LifePathBuilderTabType.FIXED_XP
                                       || tabType == LifePathBuilderTabType.EXCLUSIONS;
        if (hasSingleGroup) {
            return getFormattedTextAt("mekhq.resources.LifePathBuilderDialog",
                  "LifePathBuilderDialog.picker.title.section", baseTitle, sectionName);
        }

        return getFormattedTextAt("mekhq.resources.LifePathBuilderDialog",
              "LifePathBuilderDialog.picker.title.group", baseTitle, sectionName, groupIndex);
    }

    /**
     * Builds the help line and the Cancel, Clear All and Confirm buttons.
     *
     * @return the controls panel
     *
     * @since 0.50.11
     */
    private JPanel buildControlPanel() {
        JPanel pnlControls = new JPanel();
        pnlControls.setLayout(new BoxLayout(pnlControls, BoxLayout.Y_AXIS));
        pnlControls.setBorder(RoundedLineBorder.createRoundedLineBorder());

        lblTooltipDisplay = new JLabel();
        lblTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        lblTooltipDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        setLblTooltipDisplay("");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedJButton btnCancel = new RoundedJButton(getPickerText("button.cancel"));
        btnCancel.addActionListener(actionEvent -> cancelAndDispose());

        RoundedJButton btnClear = new RoundedJButton(getTextAt("mekhq.resources.LifePathBuilderDialog",
              "LifePathBuilderDialog.picker.button.clear"));
        btnClear.addActionListener(actionEvent -> {
            clearSelection();
            refreshConfirmEnabled();
        });

        btnConfirm = new RoundedJButton(getPickerText("button.confirm"));
        btnConfirm.addActionListener(actionEvent -> dispose());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(btnCancel);
        buttonPanel.add(Box.createHorizontalStrut(PADDING));
        buttonPanel.add(btnClear);
        buttonPanel.add(Box.createHorizontalStrut(PADDING));
        buttonPanel.add(btnConfirm);
        buttonPanel.add(Box.createHorizontalGlue());

        pnlControls.add(lblTooltipDisplay);
        pnlControls.add(Box.createVerticalStrut(PADDING));
        pnlControls.add(buttonPanel);

        return pnlControls;
    }

    /**
     * Puts the selection back and closes.
     *
     * @since 0.50.11
     */
    private void cancelAndDispose() {
        restoreStoredSelection();
        dispose();
    }

    /**
     * Builds the instructions column.
     *
     * <p>The text is looked up per section where the subclass provides per-section keys, because what a picker means
     * changes between requiring something and awarding it.</p>
     *
     * @return the instructions panel
     *
     * @since 0.50.11
     */
    private JPanel buildInstructionsPanel() {
        JPanel pnlInstructions = new JPanel();
        pnlInstructions.setLayout(new BoxLayout(pnlInstructions, BoxLayout.Y_AXIS));
        pnlInstructions.setBorder(RoundedLineBorder.createRoundedLineBorder(getPickerText("instructions.label")));

        int textPanelWidth = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.75);
        String instructions = String.format(PANEL_HTML_FORMAT, textPanelWidth, getInstructionsText());

        JEditorPane txtInstructions = new JEditorPane("text/html", instructions);
        txtInstructions.setEditable(false);
        txtInstructions.setBorder(null);
        // An HTML pane keeps its own fixed font and colour otherwise, ignoring both the GUI scale and the theme, so
        // the text ends up small and dark on dark while the dialog around it scales correctly.
        txtInstructions.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        txtInstructions.setFont(UIManager.getFont("Label.font"));
        txtInstructions.setForeground(UIManager.getColor("Label.foreground"));

        FastJScrollPane scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        pnlInstructions.add(scrollInstructions);
        pnlInstructions.setMinimumSize(new Dimension(MINIMUM_INSTRUCTIONS_WIDTH, minimumComponentHeight));

        return pnlInstructions;
    }

    /**
     * Returns the instructions for the section being edited.
     *
     * @return the instructions text
     *
     * @since 0.50.11
     */
    private String getInstructionsText() {
        if (tabType == null) {
            return getPickerText("instructions.text");
        }

        return getPickerText("instructions.text." + tabType.getLookupName());
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(getClass());
            this.setName(getClass().getSimpleName());
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }
}

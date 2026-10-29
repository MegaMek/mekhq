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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.marketName;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;

/**
 * Chooses which of the board's offers to export, and how: as a contract file another campaign can import, or as a PDF
 * for players to read. Every offer starts ticked, so exporting the whole board is one click and exporting a single
 * offer is a matter of clearing the rest.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ContractExportDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = ChaosContractMarketDialog.RESOURCE_BUNDLE;

    /** What an export produces. */
    enum ExportFormat {
        /** A JSON contract file, which another campaign can import onto its board. */
        CONTRACT_FILE,
        /** A formatted PDF of the offers, for players to read. */
        PDF;

        String label() {
            return getTextAt(RESOURCE_BUNDLE, "export.contractMarket.format." + name());
        }

        String sub() {
            return getTextAt(RESOURCE_BUNDLE, "export.contractMarket.format." + name() + ".sub");
        }
    }

    private final Map<AbstractContract, HudCheckBox> checkBoxes = new LinkedHashMap<>();
    private final HudSegmentedControl<ExportFormat> formatControl = new HudSegmentedControl<>(format -> {});
    private HudButton exportButton;
    private boolean confirmed;

    /**
     * Shows the export choices for the given offers.
     *
     * @param owner  the market dialog
     * @param offers the board's offers, in the board's order
     *
     * @author Illiani
     * @since 0.51.01
     */
    ContractExportDialog(Window owner, List<AbstractContract> offers) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "export.contractMarket.title"), ModalityType.APPLICATION_MODAL);
        for (AbstractContract offer : offers) {
            HudCheckBox checkBox = new HudCheckBox(marketName(offer) + ContractReadout.separator()
                                                         + offer.getEmployerMarketDisplayName());
            checkBox.setSelected(true);
            checkBox.addActionListener(event -> refreshExportButton());
            checkBoxes.put(offer, checkBox);
        }
        initializeComponents();
    }

    /** @return {@code true} if the player chose to export, rather than cancelling */
    boolean wasConfirmed() {
        return confirmed;
    }

    /** @return the format the player chose */
    ExportFormat getSelectedFormat() {
        ExportFormat format = formatControl.getSelected();
        return format == null ? ExportFormat.CONTRACT_FILE : format;
    }

    /** @return the offers the player ticked, in the board's order */
    List<AbstractContract> getSelectedOffers() {
        List<AbstractContract> selected = new ArrayList<>();
        checkBoxes.forEach((offer, checkBox) -> {
            if (checkBox.isSelected()) {
                selected.add(offer);
            }
        });
        return selected;
    }

    // region Layout

    private void initializeComponents() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        content.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        refreshExportButton();
        setMinimumSize(scaleForGUI(520, 380));
        pack();
        setSize(scaleForGUI(600, 480));
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private JComponent buildBody() {
        JPanel body = MarketHud.column();
        body.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(14), scaleForGUI(10),
              scaleForGUI(14)));

        body.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "export.contractMarket.format"))));
        body.add(Box.createVerticalStrut(scaleForGUI(5)));
        List<Segment<ExportFormat>> segments = new ArrayList<>();
        for (ExportFormat format : ExportFormat.values()) {
            segments.add(new Segment<>(format, format.label(), format.sub(), true));
        }
        formatControl.setSegments(segments);
        formatControl.setSelected(ExportFormat.CONTRACT_FILE);
        body.add(leftAligned(formatControl));
        body.add(Box.createVerticalStrut(scaleForGUI(14)));

        JPanel offersHeading = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        offersHeading.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "export.contractMarket.offers")), BorderLayout.WEST);
        JPanel links = Hud.transparentPanel(null);
        links.setLayout(new BoxLayout(links, BoxLayout.X_AXIS));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "export.contractMarket.selectAll"), () -> setAll(true)));
        links.add(Box.createHorizontalStrut(scaleForGUI(10)));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "export.contractMarket.selectNone"), () -> setAll(false)));
        offersHeading.add(links, BorderLayout.EAST);
        body.add(leftAligned(offersHeading));
        body.add(Box.createVerticalStrut(scaleForGUI(5)));

        JPanel list = MarketHud.column();
        list.setOpaque(true);
        list.setBackground(SURFACE_DEEP);
        list.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(6),
              scaleForGUI(8)));
        for (HudCheckBox checkBox : checkBoxes.values()) {
            list.add(leftAligned(checkBox));
            list.add(Box.createVerticalStrut(scaleForGUI(3)));
        }
        JScrollPane scroll = new JScrollPane(MarketHud.topAligned(list));
        Hud.styleScroll(scroll, SURFACE_DEEP, true);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(body, BorderLayout.NORTH);
        holder.add(padded(scroll), BorderLayout.CENTER);
        return holder;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "export.contractMarket.cancel")
                                               .toUpperCase(Locale.ROOT), false);
        cancel.addActionListener(event -> dispose());
        exportButton = new HudButton("", true);
        exportButton.addActionListener(event -> {
            confirmed = true;
            dispose();
        });

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(exportButton);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void setAll(boolean selected) {
        checkBoxes.values().forEach(checkBox -> checkBox.setSelected(selected));
        refreshExportButton();
    }

    private void refreshExportButton() {
        int count = getSelectedOffers().size();
        exportButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "export.contractMarket.confirm", count)
                                   .toUpperCase(Locale.ROOT));
        exportButton.setArmed(count > 0);
    }

    private static JComponent padded(JComponent component) {
        JPanel padded = Hud.transparentPanel(new BorderLayout());
        padded.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(14), scaleForGUI(14), scaleForGUI(14)));
        padded.add(component, BorderLayout.CENTER);
        return padded;
    }

    // endregion Layout
}

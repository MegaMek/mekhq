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
package mekhq.gui.stratCon.deployment;

import static mekhq.gui.stratCon.deployment.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.unit.Unit;

/**
 * Renders a deployment board row as a HUD card: a leading readiness ring, the force or unit name, and a muted sub-line
 * (battle value and unit count for a formation; status and battle value for a unit). Selected rows get a brighter
 * surface and an accent left bar, matching the interstellar-map chrome.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DeploymentItemRenderer implements ListCellRenderer<Object> {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final transient Campaign campaign;

    private final JPanel card = new JPanel(new BorderLayout(UIUtil.scaleForGUI(10), 0));
    private final Ring ring = new Ring();
    private final JLabel nameLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();
    private final JLabel offBoardLabel = new JLabel();
    private final JLabel deployedLabel = new JLabel();
    private final JLabel subLabel = new JLabel();

    // Force IDs to flag as deploying off-board (used by the staged tray). Empty on the board renderer.
    private transient Set<Integer> offBoardForceIds = Collections.emptySet();

    // Force IDs that are already committed to the scenario (locked in the staged tray). Empty on the board renderer.
    private transient Set<Integer> lockedForceIds = Collections.emptySet();

    // Loose unit IDs already committed to the scenario (locked in the staged tray). Empty on the board renderer.
    private transient Set<UUID> lockedUnitIds = Collections.emptySet();

    // A separate cached component for section-divider rows in the staged tray (see SectionHeader).
    private final JPanel header = new JPanel(new BorderLayout());
    private final JLabel headerLabel = new JLabel();

    public DeploymentItemRenderer(Campaign campaign) {
        this.campaign = campaign;

        int pad = UIUtil.scaleForGUI(8);
        card.setBorder(BorderFactory.createEmptyBorder(pad, UIUtil.scaleForGUI(10), pad, UIUtil.scaleForGUI(10)));

        headerLabel.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        headerLabel.setForeground(TEXT_FAINT);
        header.setOpaque(true);
        header.setBackground(SURFACE_DEEP);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10), UIUtil.scaleForGUI(10),
                    UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(10))));
        header.add(headerLabel, BorderLayout.CENTER);

        nameLabel.setFont(hudFont(Font.BOLD, 1.0f, 0.02f));
        roleLabel.setFont(hudFont(Font.PLAIN, 0.82f, 0.06f));
        roleLabel.setForeground(TEXT_MUTED);
        offBoardLabel.setFont(hudFont(Font.BOLD, 0.72f, 0.12f));
        offBoardLabel.setForeground(AMBER);
        offBoardLabel.setText(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.offBoard.tag").toUpperCase(Locale.ROOT));
        offBoardLabel.setVisible(false);
        deployedLabel.setFont(hudFont(Font.BOLD, 0.72f, 0.12f));
        deployedLabel.setForeground(READY);
        deployedLabel.setText(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.deployed.tag").toUpperCase(Locale.ROOT));
        deployedLabel.setVisible(false);
        subLabel.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        subLabel.setForeground(TEXT_MUTED);

        // Name row: the force/unit name, its assigned combat role (formations only), a "deployed" flag when the force is
        // already committed to the scenario, and an off-board flag when deploying off-board.
        JPanel nameRow = new JPanel();
        nameRow.setOpaque(false);
        nameRow.setLayout(new BoxLayout(nameRow, BoxLayout.X_AXIS));
        nameRow.add(nameLabel);
        nameRow.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(8)));
        nameRow.add(roleLabel);
        nameRow.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(8)));
        nameRow.add(deployedLabel);
        nameRow.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(8)));
        nameRow.add(offBoardLabel);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(HudStyle.leftAligned(nameRow));
        text.add(HudStyle.leftAligned(subLabel));

        card.add(ring, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        if (value instanceof SectionHeader sectionHeader) {
            headerLabel.setText(sectionHeader.label().toUpperCase(Locale.ROOT));
            return header;
        }

        card.setOpaque(true);
        card.setBackground(isSelected ? SURFACE : SURFACE_DEEP);
        card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, UIUtil.scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                    BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
                          UIUtil.scaleForGUI(isSelected ? 7 : 10),
                          UIUtil.scaleForGUI(8),
                          UIUtil.scaleForGUI(10)))));

        if (value instanceof Formation formation) {
            configureFormation(formation);
            offBoardLabel.setVisible(offBoardForceIds.contains(formation.getId()));
            deployedLabel.setVisible(lockedForceIds.contains(formation.getId()));
        } else if (value instanceof Unit unit) {
            configureUnit(unit);
            offBoardLabel.setVisible(false);
            deployedLabel.setVisible(lockedUnitIds.contains(unit.getId()));
        } else {
            ring.setColor(TEXT_MUTED);
            nameLabel.setText("");
            roleLabel.setText("");
            subLabel.setText("");
            offBoardLabel.setVisible(false);
            deployedLabel.setVisible(false);
        }
        return card;
    }

    /**
     * Sets the force IDs to flag as deploying off-board. The staged tray passes the committed off-board selection here so
     * off-board forces show an indicator; the board renderer leaves this empty.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setOffBoardForceIds(Set<Integer> offBoardForceIds) {
        this.offBoardForceIds = (offBoardForceIds == null) ? Collections.emptySet() : offBoardForceIds;
    }

    /**
     * Sets the force IDs that are already committed to the scenario, so the staged tray flags them as "deployed" (and the
     * wizard blocks their removal). The board renderer leaves this empty.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setLockedForceIds(Set<Integer> lockedForceIds) {
        this.lockedForceIds = (lockedForceIds == null) ? Collections.emptySet() : lockedForceIds;
    }

    /**
     * Sets the loose unit IDs already committed to the scenario, so the staged tray marks those rows "deployed" (and the
     * wizard blocks their removal). The board renderer leaves this empty.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setLockedUnitIds(Set<UUID> lockedUnitIds) {
        this.lockedUnitIds = (lockedUnitIds == null) ? Collections.emptySet() : lockedUnitIds;
    }

    private void configureFormation(Formation formation) {
        List<OperationalStatus> statuses = formation.updateFormationIconOperationalStatus(campaign);
        ring.setColor(statuses.isEmpty() ? TEXT_MUTED : readinessColor(statuses.get(0)));
        nameLabel.setText(formation.getName());
        nameLabel.setForeground(TEXT);
        roleLabel.setText(formation.getCombatRoleInMemory().toString());
        subLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.row.formationSub",
              formation.getTotalBV(campaign, true), formation.getAllUnits(true).size()));
    }

    private void configureUnit(Unit unit) {
        ring.setColor(ACCENT);
        nameLabel.setText(unit.getName());
        nameLabel.setForeground(TEXT);
        roleLabel.setText("");
        int battleValue = (unit.getEntity() == null) ? 0 : unit.getEntity().calculateBattleValue(true, true);
        subLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.row.unitSub",
              unit.getStatus(), battleValue));
    }

    private static Color readinessColor(OperationalStatus status) {
        return switch (status) {
            case FULLY_OPERATIONAL, FACTORY_FRESH -> READY;
            case SUBSTANTIALLY_OPERATIONAL -> CAUTION;
            case MARGINALLY_OPERATIONAL, NOT_OPERATIONAL -> DANGER;
        };
    }

    /**
     * A non-selectable divider row in the staged tray, captioning the group of staged items that follows it (Primary,
     * Reinforcements, Auxiliaries, Utility, or already-deployed loose units), so a staged primary force reads apart from
     * staged reinforcements.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record SectionHeader(String label) {}

    /** A small hollow readiness ring drawn in the status colour. */
    private static final class Ring extends JComponent {
        private Color color = TEXT_MUTED;

        private Ring() {
            int size = UIUtil.scaleForGUI(16);
            Dimension dimension = new Dimension(size, size);
            setPreferredSize(dimension);
            setMinimumSize(dimension);
            setMaximumSize(dimension);
            setOpaque(false);
        }

        private void setColor(Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int stroke = UIUtil.scaleForGUI(3);
                g2.setStroke(new BasicStroke(stroke));
                g2.setColor(color);
                int inset = stroke;
                int diameter = Math.min(getWidth(), getHeight()) - (inset * 2);
                int y = (getHeight() - diameter) / 2;
                g2.drawOval(inset, y, diameter, diameter);
            } finally {
                g2.dispose();
            }
        }
    }
}

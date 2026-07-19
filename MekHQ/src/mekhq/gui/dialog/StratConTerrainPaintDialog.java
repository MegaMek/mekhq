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
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.gui.StratConPanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * GM tool: a non-modal terrain palette that puts the sector map into paint mode. Pick a terrain and a brush size, then
 * click or drag across the map to paint hexes; closing the palette leaves paint mode.
 *
 * <p>The dialog is deliberately modeless so the map stays usable underneath it - painting a coastline means many
 * strokes, not one.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConTerrainPaintDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    /** Brush radii offered, in hexes: a single hex, then the rings around it. */
    private static final int[] BRUSH_RADII = { 0, 1, 2 };

    private static final int ICON_SIZE = 24;

    private final transient StratConPanel stratConPanel;

    public StratConTerrainPaintDialog(StratConPanel stratConPanel) {
        super(SwingUtilities.getWindowAncestor(stratConPanel),
              getTextAt(RESOURCE_BUNDLE, "terrainPaint.title"),
              ModalityType.MODELESS);
        this.stratConPanel = stratConPanel;

        JList<String> terrainList = buildTerrainList();
        JComboBox<String> brushSelector = buildBrushSelector();

        setLayout(new BorderLayout());
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "terrainPaint.instructions")), BorderLayout.NORTH);
        add(new FastJScrollPane(terrainList), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(UIUtil.scaleForGUI(5), 0));
        footer.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "terrainPaint.brush.label")), BorderLayout.WEST);
        footer.add(brushSelector, BorderLayout.CENTER);

        RoundedJButton doneButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "terrainPaint.done.text"));
        doneButton.addActionListener(evt -> dispose());
        footer.add(doneButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5)));

        // Leaving the palette always leaves paint mode, however it is closed.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stratConPanel.exitPaintMode();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(UIUtil.scaleForGUI(280), UIUtil.scaleForGUI(420));
        setLocationRelativeTo(stratConPanel);
    }

    /**
     * @return the terrain palette, grouped by category so related ground sits together, with each entry showing the
     *       same sprite the map draws
     */
    private JList<String> buildTerrainList() {
        StratConBiomeManifest manifest = StratConBiomeManifest.getInstance();

        List<String> terrains = new ArrayList<>(manifest.getTerrainTypeNames());
        terrains.sort(Comparator.comparing((String terrain) -> manifest.getTerrainCategory(terrain).name())
                            .thenComparing(terrain -> terrain));

        JList<String> terrainList = new JList<>(terrains.toArray(new String[0]));
        terrainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        terrainList.setCellRenderer(new TerrainCellRenderer());
        terrainList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                stratConPanel.setPaintTerrain(terrainList.getSelectedValue());
            }
        });

        return terrainList;
    }

    private JComboBox<String> buildBrushSelector() {
        String[] labels = new String[BRUSH_RADII.length];
        for (int index = 0; index < BRUSH_RADII.length; index++) {
            labels[index] = getTextAt(RESOURCE_BUNDLE, "terrainPaint.brush." + BRUSH_RADII[index]);
        }

        JComboBox<String> brushSelector = new JComboBox<>(labels);
        brushSelector.addActionListener(evt -> stratConPanel.setPaintBrushRadius(BRUSH_RADII[brushSelector.getSelectedIndex()]));

        return brushSelector;
    }

    /** Draws each terrain with its map sprite, so the palette reads the way the sector does. */
    private class TerrainCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            BufferedImage sprite = stratConPanel.getTerrainImage((String) value);
            if (sprite != null) {
                int size = UIUtil.scaleForGUI(ICON_SIZE);
                label.setIcon(new ImageIcon(sprite.getScaledInstance(size, size, Image.SCALE_SMOOTH)));
            } else {
                label.setIcon(null);
            }

            return label;
        }
    }
}

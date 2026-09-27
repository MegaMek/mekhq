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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Font;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.roleplay.FateChart;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;

/**
 * The end-of-scene prompt behind the Chaos tile's "Scene ended" link. It asks one question, whether the scene was more
 * intense than the one before, and raises or lowers chaos by one. Only the player can judge a scene, so it never runs
 * by itself.
 */
final class ScenePrompt {
    private ScenePrompt() {}

    /**
     * @param chaos    the current chaos factor
     * @param onAnswer called with {@code true} for "more intense" and {@code false} for "less or the same"
     *
     * @return the prompt panel
     */
    static JComponent build(final int chaos, final Consumer<Boolean> onAnswer) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(12),
              scaleForGUI(14)));

        panel.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.eyebrow"))));
        panel.add(Box.createVerticalStrut(scaleForGUI(4)));
        JLabel question = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.question"));
        question.setForeground(TEXT);
        question.setFont(hudFont(Font.BOLD, 1.05f, 0.04f));
        panel.add(leftAligned(question));
        panel.add(Box.createVerticalStrut(scaleForGUI(6)));
        JLabel help = new JLabel("<html><div style='width:" + scaleForGUI(330) + "px'>"
                                       + getTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.help") + "</div></html>");
        help.setForeground(TEXT_MUTED);
        help.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        panel.add(leftAligned(help));
        panel.add(Box.createVerticalStrut(scaleForGUI(10)));

        HudButton less = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.less",
              Math.max(FateChart.MINIMUM_CHAOS_FACTOR, chaos - 1)).toUpperCase(Locale.ROOT), false, true);
        less.addActionListener(event -> onAnswer.accept(false));
        HudButton more = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.scene.more",
              Math.min(FateChart.MAXIMUM_CHAOS_FACTOR, chaos + 1)).toUpperCase(Locale.ROOT), true, true);
        more.addActionListener(event -> onAnswer.accept(true));
        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(less);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(more);
        panel.add(leftAligned(buttons));
        return panel;
    }
}

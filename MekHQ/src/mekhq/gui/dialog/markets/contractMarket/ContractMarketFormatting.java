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

import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * Shared, presentation-only formatting helpers for the Chaos contract market UI.
 *
 * <p>These turn raw contract data into the short, player-facing strings the card and dossier render. Kept out of the
 * data classes deliberately, so {@link AbstractContract} and its records stay logic-free.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractMarketFormatting {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final String SKULL_FULL_IMAGE = "data/images/misc/challenge_estimate_full.png";
    private static final String SKULL_HALF_IMAGE = "data/images/misc/challenge_estimate_half.png";
    private static final int DEFAULT_DIFFICULTY = 5;

    /**
     * The skull artwork, read from disk once and cached per requested size. A market listing renders these for every
     * offer it shows, so without this each card would re-read and re-scale both files.
     *
     * <p>Populated lazily from the EDT during rendering, so a plain map suffices. The cached icons are shared by all
     * the labels that use them, which is safe: an {@link ImageIcon} is a stateless renderer.</p>
     */
    private static final ImageIcon SKULL_FULL_BASE = new ImageIcon(SKULL_FULL_IMAGE);
    private static final ImageIcon SKULL_HALF_BASE = new ImageIcon(SKULL_HALF_IMAGE);
    private static final Map<Integer, ImageIcon> SCALED_FULL_SKULLS = new HashMap<>();
    private static final Map<Integer, ImageIcon> SCALED_HALF_SKULLS = new HashMap<>();

    private ContractMarketFormatting() {
    }

    /**
     * Renders the contract's cached difficulty as skulls: one full skull per two difficulty points, plus a half skull
     * for an odd point. This is the single source of truth for the difficulty display, so the card and the dossier
     * always agree. An unknown or uncomputed difficulty reads as an even fight.
     *
     * @param contract the contract to rate
     * @param size     the scaled pixel size for each skull
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JComponent difficultySkulls(AbstractContract contract, int size) {
        int difficulty = contract.getCachedContractDifficulty();
        if (difficulty < 1) {
            difficulty = DEFAULT_DIFFICULTY;
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);

        ImageIcon full = scaledSkull(SCALED_FULL_SKULLS, SKULL_FULL_BASE, size);
        ImageIcon half = scaledSkull(SCALED_HALF_SKULLS, SKULL_HALF_BASE, size);

        int fullSkulls = difficulty / 2;
        boolean halfSkull = (difficulty % 2) != 0;
        for (int i = 0; i < fullSkulls; i++) {
            panel.add(new JLabel(full));
        }
        if (halfSkull) {
            panel.add(new JLabel(half));
        }
        return panel;
    }

    /** Returns the skull artwork scaled to {@code size}, scaling and caching it on first request for that size. */
    private static ImageIcon scaledSkull(Map<Integer, ImageIcon> cache, ImageIcon base, int size) {
        return cache.computeIfAbsent(size, requested -> scaleImageIcon(base, requested, true));
    }

    /**
     * The clamped 1-10 difficulty used for both the skulls and the assessment word, so the two always agree. An unknown
     * or uncomputed difficulty reads as an even fight (5).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int assessmentDifficulty(AbstractContract contract) {
        int difficulty = contract.getCachedContractDifficulty();
        return difficulty < 1 ? DEFAULT_DIFFICULTY : difficulty;
    }

    /**
     * A plain-language read on the fight, keyed by the same 1-10 difficulty the skulls show. Difficulty 5 means the
     * player and enemy forces are evenly matched; lower is favorable to the player, higher is dangerous.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String assessmentLabel(AbstractContract contract) {
        return getTextAt(RESOURCE_BUNDLE, "value.contractMarket.assessment." + assessmentDifficulty(contract));
    }

    /**
     * Formats the opposing force as skill plus equipment letter, e.g. "Veteran / B-rated".
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String threat(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.threat",
              contract.getEnemyForceSkill().toString(),
              ratingLetter(contract.getEnemyEquipmentRating()));
    }

    /**
     * Formats the employer's own force as skill plus equipment letter, in the same style as {@link #threat}, e.g.
     * "Regular / C-rated".
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String alliedCommand(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.threat",
              contract.getEmployerForceSkill().toString(),
              ratingLetter(contract.getEmployerEquipmentRating()));
    }

    /**
     * Formats salvage terms as either "Exchange only" or a rounded percentage.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String salvage(AbstractContract contract) {
        if (contract.isSalvageExchange()) {
            return getTextAt(RESOURCE_BUNDLE, "value.contractMarket.salvage.exchange");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.salvage.percentage",
              asPercent(contract.getSalvageRightsMultiplier()));
    }

    public static String support(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.support",
              asPercent(contract.getSupportMultiplier()));
    }

    public static String transport(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.transport",
              asPercent(contract.getTransportMultiplier()));
    }

    private static int asPercent(double multiplier) {
        return (int) Math.round(multiplier * 100);
    }

    private static String ratingLetter(int equipmentRating) {
        String name = DragoonRating.fromRating(equipmentRating).name().replace("DRAGOON_", "");
        return name.equals("ASTAR") ? "A*" : name;
    }
}

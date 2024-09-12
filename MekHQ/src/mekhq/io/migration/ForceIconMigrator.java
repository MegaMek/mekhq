/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.io.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;

/**
 * This migrates Force icons from varied sources to Kailan's Pack
 * This migration occurred in 0.49.6, with additional changes migrated in
 * 0.49.7.
 */
public class ForceIconMigrator {
    private static final MMLogger logger = MMLogger.create(ForceIconMigrator.class);

    // region 0.49.7
    public static StandardForceIcon migrateForceIcon0496To0497(final @Nullable StandardForceIcon icon) {
        if (icon instanceof LayeredForceIcon) {
            return migrateLayeredForceIcon0497((LayeredForceIcon) icon);
        } else if (icon != null) {
            return migrateStandardForceIcon0497(icon);
        } else {
            logger.error("Attempted to migrate a null icon to 0.49.7, when AbstractIcon is non-nullable by design");
            return new StandardForceIcon();
        }
    }

    private static StandardForceIcon migrateLayeredForceIcon0497(final LayeredForceIcon icon) {
        if (icon.getPieces().containsKey(LayeredForceIconLayer.ALPHANUMERIC)) {
            for (final ForcePieceIcon piece : icon.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)) {
                if ("StratOps/".equals(piece.getCategory()) && "C3\302\240.png".equals(piece.getFilename())) {
                    piece.setFilename("C3.png");
                }
            }
        }

        if (icon.getPieces().containsKey(LayeredForceIconLayer.BACKGROUND)) {
            for (final ForcePieceIcon piece : icon.getPieces().get(LayeredForceIconLayer.BACKGROUND)) {
                if ("Periphery/".equals(piece.getCategory()) && "Auximite Providence.png".equals(piece.getFilename())) {
                    piece.setFilename("Axumite Providence.png");
                }
            }
        }

        if (icon.getPieces().containsKey(LayeredForceIconLayer.LOGO)) {
            for (final ForcePieceIcon piece : icon.getPieces().get(LayeredForceIconLayer.LOGO)) {
                switch (piece.getCategory()) {
                    case "Clan/":
                        switch (piece.getFilename()) {
                            case "Clan Fire Mandril.png":
                                piece.setFilename("Clan Fire Mandrill.png");
                                break;
                            case "Clan Hells Horses.png":
                                piece.setFilename("Clan Hell's Horses.png");
                                break;
                            case "Society.png":
                                piece.setFilename("The Society.png");
                                break;
                            default:
                                break;
                        }
                        break;
                    case "Inner Sphere/":
                        switch (piece.getFilename()) {
                            case "Federated Suns.png.png":
                                piece.setFilename("Federated Suns.png");
                                break;
                            case "Rim Worlds Republic.png":
                                piece.setCategory("Periphery/");
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (icon.getPieces().containsKey(LayeredForceIconLayer.TYPE)) {
            boolean hasLAM = false;

            for (final ForcePieceIcon piece : icon.getPieces().get(LayeredForceIconLayer.TYPE)) {
                switch (piece.getCategory()) {
                    case "NATO/":
                        if ("Vehicle (Wheeled Tracked).png".equals(piece.getFilename())) {
                            icon.setFilename("Vehicle (Tracked Wheeled).png");
                        }
                        break;
                    case "StratOps/":
                        switch (piece.getFilename()) {
                            case "LAM.png":
                                hasLAM = true;
                                break;
                            case "Vehicle (Hover Wheeled Armoured).png":
                                icon.setCategory("Pieces/Types/NATO/");
                                icon.setFilename("Vehicle (Mixed).png");
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }

            if (hasLAM) {
                icon.getPieces().get(LayeredForceIconLayer.TYPE).removeIf(
                        piece -> "StratOps/".equals(piece.getCategory()) && "LAM.png".equals(piece.getFilename()));
                icon.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                icon.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Right).png"));
            }
        }

        return icon;
    }

    private static StandardForceIcon migrateStandardForceIcon0497(final StandardForceIcon icon) {
        switch (icon.getCategory()) {
            case "Pieces/Alphanumerics/StratOps/":
                if ("C3\302\240.png".equals(icon.getFilename())) {
                    icon.setFilename("C3.png");
                }
                break;
            case "Pieces/Backgrounds/Periphery/":
                if ("Auximite Providence.png".equals(icon.getFilename())) {
                    icon.setFilename("Axumite Providence.png");
                }
                break;
            case "Pieces/Logos/Clan/":
                switch (icon.getFilename()) {
                    case "Clan Fire Mandril.png":
                        icon.setFilename("Clan Fire Mandrill.png");
                        break;
                    case "Clan Hells Horses.png":
                        icon.setFilename("Clan Hell's Horses.png");
                        break;
                    case "Society.png":
                        icon.setFilename("The Society.png");
                        break;
                    default:
                        break;
                }
                break;
            case "Pieces/Logos/Inner Sphere/":
                switch (icon.getFilename()) {
                    case "Federated Suns.png.png":
                        icon.setFilename("Federated Suns.png");
                        break;
                    case "Rim Worlds Republic.png":
                        icon.setCategory("Pieces/Logos/Periphery/");
                        break;
                    default:
                        break;
                }
                break;
            case "Pieces/Types/NATO/":
                if ("Vehicle (Wheeled Tracked).png".equals(icon.getFilename())) {
                    icon.setFilename("Vehicle (Tracked Wheeled).png");
                }
                break;
            case "Pieces/Types/StratOps/":
                switch (icon.getFilename()) {
                    case "LAM.png":
                        if (icon instanceof UnitIcon) {
                            return new UnitIcon(null, null);
                        } else {
                            final LayeredForceIcon migrated = new LayeredForceIcon();
                            migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                            migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                                    .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                            "BattleMech (Left).png"));
                            migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                                    .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                            "Aerospace (Right).png"));
                            return migrated;
                        }
                    case "Vehicle (Hover Wheeled Armoured).png":
                        icon.setCategory("Pieces/Types/NATO/");
                        icon.setFilename("Vehicle (Mixed).png");
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        return icon;
    }
    // endregion 0.49.7

    // region 0.49.6
    public static StandardForceIcon migrateForceIconToKailans(final @Nullable StandardForceIcon icon) {
        if (icon instanceof LayeredForceIcon) {
            return migrateLayeredForceIcon((LayeredForceIcon) icon);
        } else if (icon instanceof UnitIcon) {
            return migrateUnitIcon((UnitIcon) icon);
        } else if (icon != null) {
            return migrateStandardForceIcon(icon);
        } else {
            logger.error("Attempted to migrate a null icon, when AbstractIcon is non-nullable by design");
            return new StandardForceIcon();
        }
    }

    // region Layered Force Icon
    private static LayeredForceIcon migrateLayeredForceIcon(final LayeredForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        migrateAdjustments(icon, layered);
        migrateAlphanumerics(icon, layered);
        migrateBackgrounds(icon, layered);
        migrateFormations(icon, layered);
        migrateLogos(icon, layered);
        migrateSpecialModifiers(icon, layered);
        migrateTypes(icon, layered);
        return layered;
    }

    private static void migrateAdjustments(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.ADJUSTMENT,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "Air assault.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Air Mobile.png"));
                    break;
                case "Artillery_Missile.png":
                case "Artillery_Missile_small.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Artillery (Missile).png"));
                    break;
                case "Artillery_Missile_Multiple.png":
                case "Artillery_Missile_Multiple_small.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Artillery (Multiple Missile).png"));
                    break;
                case "Artillery_tube.png":
                case "Artillery_tube_small.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Artillery.png"));
                    break;
                case "Command and Control.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Command and Control.png"));
                    break;
                case "Drone Carrier.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Drone Carrier.png"));
                    break;
                case "Infantry_Motorized.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Motorized.png"));
                    break;
                case "Battle Armor Transport.png":
                case "Mechanized.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                    break;
                case "Mountaineers.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mountaineer.png"));
                    break;
                case "Paratrooper.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Paratrooper.png"));
                    break;
                case "Recon.png":
                    migrated.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Recon.png"));
                    break;
                case "Scuba.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Scuba.png"));
                    break;
                case "Supply.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Supply.png"));
                    break;
                case "Infantry_Jumppng":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/",
                                    "Jump Infantry (Large).png"));
                    break;
                case "Infantry_SpaceMarine.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Space.png"));
                    break;
                case "Omni.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Omni.png"));
                    break;

                default:
                    break;
            }
        }
    }

    private static void migrateAlphanumerics(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.ALPHANUMERIC,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "A I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "A.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "A II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "A.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "A III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "A.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "A IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "A.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "A V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "A.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "B I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "B.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "B II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "B.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "B III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "B.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "B IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "B.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "B V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "B.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "C I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "C.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "C II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "C.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "C III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "C.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "C IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "C.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "C V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "C.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "D I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "D.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "D II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "D.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "D III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "D.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "D IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "D.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "D V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "D.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "E I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "E.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "E II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "E.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "E III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "E.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "E IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "E.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "E V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "E.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "F I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "F.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "F II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "F.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "F III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "F.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "F IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "F.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "F V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "F.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "G I Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "G.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(01) Roman I.png"));
                    break;
                case "G II Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "G.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "G III Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "G.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "G IV Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "G.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "G V Low.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "G.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(05) Roman V.png"));
                    break;
                case "A Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "A.png"));
                    break;
                case "A Low.png":
                case "A.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "A.png"));
                    break;
                case "AP.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "AP.png"));
                    break;
                case "B Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "B.png"));
                    break;
                case "B Low.png":
                case "B.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "B.png"));
                    break;
                case "BB.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "BB.png"));
                    break;
                case "BT.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "BT.png"));
                    break;
                case "C Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "C.png"));
                    break;
                case "C Low.png":
                case "C.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "C.png"));
                    break;
                case "CA.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CA.png"));
                    break;
                case "CB.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CB.png"));
                    break;
                case "CH.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CH.png"));
                    break;
                case "CP.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CP.png"));
                    break;
                case "CT.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CT.png"));
                    break;
                case "CV.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CV.png"));
                    break;
                case "D Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "D.png"));
                    break;
                case "D Low.png":
                case "D.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "D.png"));
                    break;
                case "DCV.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "DCV.png"));
                    break;
                case "DD.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "DD.png"));
                    break;
                case "DH.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "DH.png"));
                    break;
                case "DS.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "DS.png"));
                    break;
                case "E Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "E.png"));
                    break;
                case "E Low.png":
                case "E.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "E.png"));
                    break;
                case "EP.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "EP.png"));
                    break;
                case "F Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "F.png"));
                    break;
                case "F Low.png":
                case "F.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "F.png"));
                    break;
                case "FR.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "FR.png"));
                    break;
                case "G Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "G.png"));
                    break;
                case "G Low.png":
                case "G.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "G.png"));
                    break;
                case "H Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "H.png"));
                    break;
                case "H Low.png":
                case "H.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "H.png"));
                    break;
                case "I Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "I.png"));
                    break;
                case "I Low.png":
                case "I.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "I.png"));
                    break;
                case "II Low.png":
                case "II.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(02) Roman II.png"));
                    break;
                case "III Low.png":
                case "III.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(03) Roman III.png"));
                    break;
                case "IV Low.png":
                case "IV.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(04) Roman IV.png"));
                    break;
                case "IX Low.png":
                case "IX.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(09) Roman IX.png"));
                    break;
                case "J Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "J.png"));
                    break;
                case "J Low.png":
                case "J.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "J.png"));
                    break;
                case "K Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "K.png"));
                    break;
                case "K Low.png":
                case "K.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "K.png"));
                    break;
                case "L Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "L.png"));
                    break;
                case "L Low.png":
                case "L.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "L.png"));
                    break;
                case "LB.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LB.png"));
                    break;
                case "LC.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LC.png"));
                    break;
                case "LG.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LG.png"));
                    break;
                case "LH.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LH.png"));
                    break;
                case "LM.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LM.png"));
                    break;
                case "LP.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LP.png"));
                    break;
                case "LT.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "LT.png"));
                    break;
                case "M Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "M.png"));
                    break;
                case "M Low.png":
                case "M.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "M.png"));
                    break;
                case "MY.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "MY.png"));
                    break;
                case "N Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "N.png"));
                    break;
                case "N Low.png":
                case "N.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "N.png"));
                    break;
                case "NF.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "NF.png"));
                    break;
                case "NX.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "NX.png"));
                    break;
                case "O Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "O.png"));
                    break;
                case "O Low.png":
                case "O.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "O.png"));
                    break;
                case "P Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "P.png"));
                    break;
                case "P Low.png":
                case "P.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "P.png"));
                    break;
                case "PC.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "PC.png"));
                    break;
                case "PS.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "PS.png"));
                    break;
                case "PT.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "PT.png"));
                    break;
                case "Q Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "Q.png"));
                    break;
                case "Q Low.png":
                case "Q.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "Q.png"));
                    break;
                case "R Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "R.png"));
                    break;
                case "R Low.png":
                case "R.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "R.png"));
                    break;
                case "S Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "S.png"));
                    break;
                case "S Low.png":
                case "S.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "S.png"));
                    break;
                case "SB.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "SB.png"));
                    break;
                case "SL.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "SL.png"));
                    break;
                case "SS.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "SS.png"));
                    break;
                case "ST.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "ST.png"));
                    break;
                case "T Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "T.png"));
                    break;
                case "T Low.png":
                case "T.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "T.png"));
                    break;
                case "U Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "U.png"));
                    break;
                case "U Low.png":
                case "U.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "U.png"));
                    break;
                case "V Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "V.png"));
                    break;
                case "V Low.png":
                case "V.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "V.png"));
                    break;
                case "VI Low.png":
                case "VI.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(06) Roman VI.png"));
                    break;
                case "VII Low.png":
                case "VII.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(07) Roman VII.png"));
                    break;
                case "VIII Low.png":
                case "VIII.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                    "(08) Roman VIII.png"));
                    break;
                case "W Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "W.png"));
                    break;
                case "W Low.png":
                case "W.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "W.png"));
                    break;
                case "X Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "X.png"));
                    break;
                case "X Low.png":
                case "X.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "X.png"));
                    break;
                case "Y Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "Y.png"));
                    break;
                case "Y Low.png":
                case "Y.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "Y.png"));
                    break;
                case "YC.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YC.png"));
                    break;
                case "YH.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YH.png"));
                    break;
                case "YLG.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YLG.png"));
                    break;
                case "YLH.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YLH.png"));
                    break;
                case "YLT.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YLT.png"));
                    break;
                case "YMY.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YMY.png"));
                    break;
                case "YNX.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YNX.png"));
                    break;
                case "YP.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YP.png"));
                    break;
                case "YR.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YR.png"));
                    break;
                case "YT.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YT.png"));
                    break;
                case "YX.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "YX.png"));
                    break;
                case "Z Low Left.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                    "Z.png"));
                    break;
                case "Z Low.png":
                case "Z.png":
                    migrated.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                    "Z.png"));
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateBackgrounds(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.BACKGROUND,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "CDS.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/",
                                    "Clan Diamond Shark.png"));
                    break;
                case "CGB.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/", "Clan Ghost Bear.png"));
                    break;
                case "CHH.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/",
                                    "Clan Hell's Horses.png"));
                    break;
                case "CJF.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/", "Clan Jade Falcon.png"));
                    break;
                case "CNC Alternate.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/",
                                    "Clan Nova Cat (Alternate).png"));
                    break;
                case "CNC.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/", "Clan Nova Cat.png"));
                    break;
                case "CSR Alternate.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/",
                                    "Clan Snow Raven (Alternate).png"));
                    break;
                case "CSR.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/", "Clan Snow Raven.png"));
                    break;
                case "CW.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Clan/", "Clan Wolf.png"));
                    break;
                case "Capellan Confederation.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Capellan Confederation.png"));
                    break;
                case "ComStar background.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/", "ComStar.png"));
                    break;
                case "Draconis Combine.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Draconis Combine.png"));
                    break;
                case "Federated Suns.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Federated Suns.png"));
                    break;
                case "Free Rasalhague Republic.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Free Rasalhague Republic.png"));
                    break;
                case "Free Worlds League.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Free Worlds League.png"));
                    break;
                case "Lyran Commonwealth.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Lyran Commonwealth.png"));
                    break;
                case "Republic of the sphere.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Inner Sphere/",
                                    "Republic of the Sphere.png"));
                    break;
                case "Magistracy of Canopus alternate.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Magistracy of Canopus (Alternate).png"));
                    break;
                case "Magistracy of Canopus.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Magistracy of Canopus.png"));
                    break;
                case "Marian Hegemony.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Marian Hegemony.png"));
                    break;
                case "Outworlds alliance alternate.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Outworlds Alliance (Alternate).png"));
                    break;
                case "Outworlds alliance.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Outworlds Alliance.png"));
                    break;
                case "Rim worlds Republic.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Rim Worlds Republic.png"));
                    break;
                case "Tarurian Concordat .png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Taurian Concordat.png"));
                    break;
                case "Tarurian Concordat alternate.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "Periphery/",
                                    "Taurian Concordat (Alternate).png"));
                    break;
                case "Merc.png":
                    migrated.getPieces().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.BACKGROUND)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND, "", "Mercenary.png"));
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateFormations(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.FORMATION,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "00 Installation.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(00) Installation.png"));
                    break;
                case "01 Fire Team.png":
                case "01 FireTeam.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(01) Fire Team.png"));
                    break;
                case "02 Individual.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(02) Individual.png"));
                    break;
                case "03 Team.png":
                case "03Team.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                    break;
                case "04 Lance.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(04) Lance.png"));
                    break;
                case "05 Lance Augmented.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(04A) Augmented Lance.png"));
                    break;
                case "06 Company.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(05) Company.png"));
                    break;
                case "07 Company Task Force.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(05A) Company Task Force.png"));
                    break;
                case "08 Battalion.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(06) Battalion.png"));
                    break;
                case "09 Battlegroup.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(06A) Battlegroup.png"));
                    break;
                case "10 Regiment.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(07) Regiment.png"));
                    break;
                case "11 Brigade.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(09) Brigade.png"));
                    break;
                case "12 Division.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(09) Division.png"));
                    break;
                case "13 Corps.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(10) Corps.png"));
                    break;
                case "14 Field Army.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(11) Field Army.png"));
                    break;
                case "15 Army Group.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(12) Army Group.png"));
                    break;
                case "Brigade Augmented.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(08A) Augmented Brigade.png"));
                    break;
                case "Corps Augmented.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(10A) Augmented Corps.png"));
                    break;
                case "Division Augmented.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(09A) Augmented Division.png"));
                    break;
                case "Field Army Augmented.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(11A) Augmented Field Army.png"));
                    break;
                case "Regiment Augmented.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                    "(07A) Augmented Regiment.png"));
                    break;
                case "16 Level I.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(01) Level I.png"));
                    break;
                case "17 Level II.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(02) Level II.png"));
                    break;
                case "18 Choir.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(03) Choir.png"));
                    break;
                case "19 Level III.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(04) Level III.png"));
                    break;
                case "20 Level IV.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(05) Level IV.png"));
                    break;
                case "21 Level V.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(06) Level V.png"));
                    break;
                case "22 Level VI.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "ComStar/", "(07) Level VI.png"));
                    break;
                case "23 Point.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(01) Point.png"));
                    break;
                case "24 Star.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(02) Star.png"));
                    break;
                case "25 Binary.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(04) Binary.png"));
                    break;
                case "26 Nova.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(03) Nova.png"));
                    break;
                case "27 Trinary.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(06) Trinary.png"));
                    break;
                case "28 SuperNova Binary.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/",
                                    "(05) SuperNova Binary.png"));
                    break;
                case "29 SuperNova Trinary.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/",
                                    "(07) SuperNova Trinary.png"));
                    break;
                case "30 Cluster.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(08) Cluster.png"));
                    break;
                case "31 Galaxy.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.FORMATION)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Clan/", "(09) Galaxy.png"));
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateLogos(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.LOGO,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "Capellan Confederation.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                    "Capellan Confederation.png"));
                    break;
                case "ComStar.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "ComStar.png"));
                    break;
                case "Draconis Combine.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                    "Draconis Combine.png"));
                    break;
                case "Federated Commonwealth.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                    "Federated Commonwealth.png"));
                    break;
                case "Federated Suns.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Federated Suns.png"));
                    break;
                case "Free Rasalhague Republic.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                    "Free Rasalhague Republic.png"));
                    break;
                case "Free Worlds League.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                    "Free Worlds League.png"));
                    break;
                case "Lyran Alliance.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Lyran Alliance.png"));
                    break;
                case "Lyran Commonwealth.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                    "Lyran Commonwealth.png"));
                    break;
                case "Rim Worlds Republic.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/",
                                    "Rim Worlds Republic.png"));
                    break;
                case "Star League.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Star League.png"));
                    break;
                case "Word of Blake.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Word of Blake.png"));
                    break;
                case "Clan Blood Spirit.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Blood Spirit.png"));
                    break;
                case "Clan Cloud Cobra.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Cloud Cobra.png"));
                    break;
                case "Clan Coyote.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Coyote.png"));
                    break;
                case "Clan Diamond Shark.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Diamond Shark.png"));
                    break;
                case "Clan Fire Mandril.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Fire Mandrill.png"));
                    break;
                case "Clan Ghost Bear.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Ghost Bear.png"));
                    break;
                case "Clan Goliath Scorpion.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Goliath Scorpion.png"));
                    break;
                case "Clan Hells Horses.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Hell's Horses.png"));
                    break;
                case "Clan Ice Hellion.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Ice Hellion.png"));
                    break;
                case "Clan Jade Falcon.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Jade Falcon.png"));
                    break;
                case "Clan Mongoose.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Mongoose.png"));
                    break;
                case "Clan Nova Cat.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Nova Cat.png"));
                    break;
                case "Clan Smoke Jaguar.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Smoke Jaguar.png"));
                    break;
                case "Clan Snow Raven.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Snow Raven.png"));
                    break;
                case "Clan Star Adder.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Star Adder.png"));
                    break;
                case "Clan Steel Viper.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Steel Viper.png"));
                    break;
                case "Clan Widowmaker.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Widowmaker.png"));
                    break;
                case "Clan Wolf in Exile.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Wolf-in-Exile.png"));
                    break;
                case "Clan Wolf.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Wolf.png"));
                    break;
                case "Clan Wolverine.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Wolverine.png"));
                    break;
                case "Circinus Federation.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/",
                                    "Circinus Federation.png"));
                    break;
                case "Magistracy of Canopus.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/",
                                    "Magistracy of Canopus.png"));
                    break;
                case "Marian Hegemony.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Marian Hegemony.png"));
                    break;
                case "Outworlds Alliance.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/",
                                    "Outworlds Alliance.png"));
                    break;
                case "Taurian Concordat.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Taurian Concordat.png"));
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateSpecialModifiers(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.SPECIAL_MODIFIER,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "C3.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "C3.png"));
                    break;
                case "C3i.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "C3i.png"));
                    break;
                case "HQ indicator.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom Right/", "HQ.png"));
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateTypes(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final ForcePieceIcon piece : original.getPieces().getOrDefault(LayeredForceIconLayer.TYPE,
                new ArrayList<>())) {
            switch (piece.getFilename()) {
                case "Aerospace Heavy.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                    break;
                case "Aerospace Light.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                    break;
                case "Aerospace Medium.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                    break;
                case "Aerospace.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace.png"));
                    break;
                case "Air Defense Artillery.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Air Defense.png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Artillery.png"));
                    break;
                case "Air Defense Capital Weaponry.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Air Defense.png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Space.png"));
                    break;
                case "Air Defense.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Air Defense.png"));
                    break;
                case "Airship.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Airship.png"));
                    break;
                case "Armoured_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Main Battle Tank.png"));
                    break;
                case "Artillery_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Artillery.png"));
                    break;
                case "Aviation VTOL.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                    "Aviation, Rotary Wing (Civilian).png"));
                    break;
                case "Battle Armor.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                    "Battle Armor (Extended).png"));
                    break;
                case "BattleMech Assault.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Assault.png"));
                    break;
                case "BattleMech Heavy.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                    break;
                case "BattleMech Light.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                    break;
                case "BattleMech Medium.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                    break;
                case "BattleMech Superheavy.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Superheavy.png"));
                    break;
                case "BattleMech.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                    "BattleMech (Center).png"));
                    break;
                case "Drone Hover Converted.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Drone (Converted).png"));
                    break;
                case "Drone Hover.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Drone (Military).png"));
                    break;
                case "Drone Tracked Converted.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Drone (Converted).png"));
                    break;
                case "Drone Tracked.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Drone (Military).png"));
                    break;
                case "Drone Wheeled Converted.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Drone (Converted).png"));
                    break;
                case "Drone Wheeled.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Drone (Military).png"));
                    break;
                case "Dropship.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "DropShip.png"));
                    break;
                case "Engineer.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Engineer.png"));
                    break;
                case "Fixed Wing.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                    "Aviation, Fixed Wing (Civilian).png"));
                    break;
                case "Headquarters.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                    "HQ (Headquarters).png"));
                    break;
                case "Industrial Mechs.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Industrial.png"));
                    break;
                case "Infantry Hover.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                    break;
                case "Infantry Jump.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/",
                                    "Jump Infantry (Large).png"));
                    break;
                case "Infantry Mechanized VTOL.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                    "Aviation, Rotary Wing (Military).png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                    break;
                case "Infantry Mechanized.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                    break;
                case "Infantry Space Marine.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Space.png"));
                    break;
                case "Infantry Tracked.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                    break;
                case "Infantry Wheeled.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled).png"));
                    break;
                case "Infantry.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    break;
                case "Infantry_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Infantry.png"));
                    break;
                case "Jumpship.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "JumpShip.png"));
                    break;
                case "LAM.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Right).png"));
                    break;
                case "Maintenance Alt.png":
                case "Maintenance Small.png":
                case "Maintenance.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Maintenance.png"));
                    break;
                case "medical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medical.png"));
                    break;
                case "Naval.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Naval.png"));
                    break;
                case "Naval_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Naval (Destroyer).png"));
                    break;
                case "Protomech.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "ProtoMech.png"));
                    break;
                case "Rail Pressurized.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Rail (Pressurized).png"));
                    break;
                case "Rail Unpressurized.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                    "Rail (Unpressurized).png"));
                    break;
                case "Satellite.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Satellite.png"));
                    break;
                case "Scuba.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Scuba (Bottom).png"));
                    break;
                case "Space Station.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Space Station.png"));
                    break;
                case "Transport Medium.png":
                case "Transport Small.png":
                case "Transport.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Transport.png"));
                    break;
                case "Transport_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Transport.png"));
                    break;
                case "Turret_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Turret (AA).png"));
                    break;
                case "UAV.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "UAV.png"));
                    break;
                case "Vehicle Assorted.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Mixed).png"));
                    break;
                case "Vehicle Hover Alt.png":
                case "Vehicle Hover Small.png":
                case "Vehicle Hover.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                    break;
                case "Vehicle Tracked Alt.png":
                case "Vehicle Tracked Small Artillery.png":
                case "Vehicle Tracked Small.png":
                case "Vehicle Tracked.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                    break;
                case "Vehicle Wheeled Alt.png":
                case "Vehicle Wheeled Small.png":
                case "Vehicle Wheeled.png":
                case "Vehicle_Wheeled.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled).png"));
                    break;
                case "VTOL_Graphical.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "VTOL.png"));
                    break;
                case "Warship.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                    break;
                case "WIGE.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                    "Aviation, WiGE (Military).png"));
                    break;
                case "Xenoplanetary.png":
                    migrated.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                    "Infantry (Xenoplanetary).png"));
                    break;
                default:
                    break;
            }
        }
    }
    // endregion Layered Force Icon

    // region Unit Icon
    private static UnitIcon migrateUnitIcon(final UnitIcon icon) {
        if (icon.hasDefaultFilename() || (icon.getFilename() == null)) {
            return new UnitIcon(null, null);
        }

        switch (icon.getCategory().toLowerCase(Locale.ENGLISH)) {
            case "units":
                return migrateUnitIconUnits(icon);
            case "pieces/logos":
                return migrateUnitIconLogos(icon);
            default:
                // Apply a hard reset, as they can reselect and it's almost certain this
                // originates
                // from user error
                return new UnitIcon(null, null);
        }
    }

    private static UnitIcon migrateUnitIconUnits(final UnitIcon icon) {
        switch (icon.getFilename()) {
            case "Arturanguards25th.jpg":
                return new UnitIcon(icon.getCategory(), "25th Arcturan Guards.jpg");
            case "Black_Widow_Company.jpg":
                return new UnitIcon(icon.getCategory(), "Black Widow Company.jpg");
            case "CapellanConfederation.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Capellan Confederation.png");
            case "CircinusFederation.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/",
                        "Circinus Federation.png");
            case "ClanCloudCobra.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Cloud Cobra.png");
            case "ClanCoyote.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Coyote.png");
            case "ClanDiamondShark.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Diamond Shark.png");
            case "ClanGhostBear.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Ghost Bear.png");
            case "ClanSteelViper.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Steel Viper.png");
            case "ClanWolverine.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Wolverine.png");
            case "ComStar.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "ComStar.png");
            case "DraconisCombine.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Draconis Combine.png");
            case "FederatedCommonwealth.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Federated Commonwealth.png");
            case "FederatedSuns.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Federated Suns.png");
            case "foxsteeth.png":
                return new UnitIcon(icon.getCategory(), "Fox's Teeth.png");
            case "FreeRasalhagueRepublic.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Free Rasalhague Republic.png");
            case "FreeWorldsLeague.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Free Worlds League.png");
            case "JadeFalconDelta.gif":
                return new UnitIcon(icon.getCategory(), "Jade Falcon Delta Galaxy.gif");
            case "LyranAlliance.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Lyran Alliance.png");
            case "MagistryOfCanopus.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/",
                        "Magistracy of Canopus.png");
            case "MarianHegemony.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/", "Marian Hegemony.png");
            case "Opacus_Venatori.jpg":
                return new UnitIcon(icon.getCategory(), "Opacus Venatori.jpg");
            case "OutworldsAlliance.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/", "Outworlds Alliance.png");
            case "RepublicOfTheSphere.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Republic of the Sphere.png");
            case "SorensonSabres.jpg":
                return new UnitIcon(icon.getCategory(), "Sorenson's Sabres.jpg");
            case "StarLeague.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Star League.png");
            case "TaurianConcordat.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/", "Taurian Concordat.png");
            case "WordOfBlake.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Word of Blake.png");
            default:
                return icon;
        }
    }

    private static UnitIcon migrateUnitIconLogos(final UnitIcon icon) {
        switch (icon.getFilename()) {
            case "Capellan Confederation.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Capellan Confederation.png");
            case "ComStar.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "ComStar.png");
            case "Draconis Combine.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Draconis Combine.png");
            case "Federated Commonwealth.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Federated Commonwealth.png");
            case "Federated Suns.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Federated Suns.png");
            case "Free Rasalhague Republic.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Free Rasalhague Republic.png");
            case "Free Worlds League.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Free Worlds League.png");
            case "Lyran Alliance.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Lyran Alliance.png");
            case "Lyran Commonwealth.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/",
                        "Lyran Commonwealth.png");
            case "Rim Worlds Republic.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/",
                        "Rim Worlds Republic.png");
            case "Star League.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Star League.png");
            case "Word of Blake.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Inner Sphere/", "Word of Blake.png");
            case "Clan Blood Spirit.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Blood Spirit.png");
            case "Clan Cloud Cobra.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Cloud Cobra.png");
            case "Clan Coyote.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Coyote.png");
            case "Clan Diamond Shark.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Diamond Shark.png");
            case "Clan Fire Mandril.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Fire Mandrill.png");
            case "Clan Ghost Bear.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Ghost Bear.png");
            case "Clan Goliath Scorpion.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Goliath Scorpion.png");
            case "Clan Hells Horses.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Hell's Horses.png");
            case "Clan Ice Hellion.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Ice Hellion.png");
            case "Clan Jade Falcon.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Jade Falcon.png");
            case "Clan Mongoose.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Mongoose.png");
            case "Clan Nova Cat.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Nova Cat.png");
            case "Clan Smoke Jaguar.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Smoke Jaguar.png");
            case "Clan Snow Raven.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Snow Raven.png");
            case "Clan Star Adder.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Star Adder.png");
            case "Clan Steel Viper.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Steel Viper.png");
            case "Clan Widowmaker.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Widowmaker.png");
            case "Clan Wolf in Exile.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Wolf-in-Exile.png");
            case "Clan Wolf.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Wolf.png");
            case "Clan Wolverine.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Clan/", "Clan Wolverine.png");
            case "Circinus Federation.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/",
                        "Circinus Federation.png");
            case "Magistracy of Canopus.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/",
                        "Magistracy of Canopus.png");
            case "Marian Hegemony.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/", "Marian Hegemony.png");
            case "Outworlds Alliance.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/", "Outworlds Alliance.png");
            case "Taurian Concordat.png":
                return new UnitIcon(LayeredForceIconLayer.LOGO.getLayerPath() + "Periphery/", "Taurian Concordat.png");
            default:
                return icon;
        }
    }
    // endregion Unit Icon

    // region Standard Force Icon
    private static StandardForceIcon migrateStandardForceIcon(final StandardForceIcon icon) {
        if (icon.hasDefaultCategory()) {
            if ("Book.png".equalsIgnoreCase(icon.getFilename())) {
                return new StandardForceIcon(StandardForceIcon.ROOT_CATEGORY,
                        StandardForceIcon.DEFAULT_FORCE_ICON_FILENAME);
            } else {
                return (icon instanceof LayeredForceIcon) ? new StandardForceIcon() : icon;
            }
        } else if (icon.getCategory().toLowerCase(Locale.ENGLISH).startsWith("pieces")) {
            LayeredForceIconLayer layer = Arrays.stream(LayeredForceIconLayer.values())
                    .filter(iconLayer -> icon.getCategory().equalsIgnoreCase(iconLayer.getLayerPath()))
                    .findFirst().orElse(null);

            if (layer == null) {
                return new StandardForceIcon();
            } else if (layer.isFrame()) {
                return new LayeredForceIcon();
            }

            final LayeredForceIcon parser = new LayeredForceIcon(LayeredForceIcon.LAYERED_CATEGORY,
                    icon.getFilename());
            final LayeredForceIcon parsed = new LayeredForceIcon();
            switch (layer) {
                case ADJUSTMENT:
                    migrateAdjustments(parser, parsed);
                    break;
                case ALPHANUMERIC:
                    migrateAlphanumerics(parser, parsed);
                    break;
                case BACKGROUND:
                    migrateBackgrounds(parser, parsed);
                    break;
                case FORMATION:
                    migrateFormations(parser, parsed);
                    break;
                case FRAME:
                    break;
                case LOGO:
                    migrateLogos(parser, parsed);
                    break;
                case SPECIAL_MODIFIER:
                    migrateSpecialModifiers(parser, parsed);
                    break;
                case TYPE:
                    migrateTypes(parser, parsed);
                    break;
            }

            return parsed;
        }

        switch (icon.getCategory().toLowerCase(Locale.ENGLISH)) {
            case "aerospace":
                return migrateStandardAerospace(icon);
            case "battle armor":
                return migrateStandardBattleArmor(icon);
            case "blue water naval":
                return migrateStandardBlueWaterNaval(icon);
            case "formations/alphabet/":
                return migrateStandardFormationsAlphabet(icon);
            case "formations/clan/":
                return migrateStandardFormationsClan(icon);
            case "formations/greek letters/":
                return migrateStandardFormationsGreekLetters(icon);
            case "formations/is/":
                return migrateStandardFormationsIS(icon);
            case "formation/numeric/":
                return migrateStandardFormationsNumeric(icon);
            case "formations/phoenetic/":
                return migrateStandardFormationsPhoenetic(icon);
            case "infantry":
                return migrateStandardInfantry(icon);
            case "mech":
                return migrateStandardMech(icon);
            case "military":
                return migrateStandardMilitary(icon);
            case "miscellaneous":
                return migrateStandardMiscellaneous(icon);
            case "names":
                return migrateStandardNames(icon);
            case "naval":
                return migrateStandardNaval(icon);
            case "units":
                return migrateStandardUnits(icon);
            case "vehicles":
                return migrateStandardVehicles(icon);
            case "vtol":
                return migrateStandardVTOL(icon);
            default:
                break;
        }

        return icon;
    }

    private static StandardForceIcon migrateStandardAerospace(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "aerospace_fighter.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Aerospace (M).png"));
                return layered;
            case "aerospacesquadron.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                return layered;
            case "afighter.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Fixed Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                return layered;
            case "afighterflight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Fixed Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                return layered;
            case "haerospace.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                return layered;
            case "haerospaceflight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "laerospace.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                return layered;
            case "laerospaceflight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "maerospace.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                return layered;
            case "maerospaceflight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardBattleArmor(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        if ("battlearmor.png".equals(icon.getFilename())) {
            layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
            layered.getPieces().get(LayeredForceIconLayer.TYPE)
                    .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Battle Armor (Extended).png"));
            layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
            layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                    .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "MVO.png"));
            return layered;
        } else {
            return icon;
        }
    }

    private static StandardForceIcon migrateStandardBlueWaterNaval(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "naval_vessel_submarine.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Naval (Submarine).png"));
                return layered;
            case "naval_vessel_surface.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Naval (Destroyer).png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsAlphabet(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "A.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "A.png"));
                return layered;
            case "B.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "B.png"));
                return layered;
            case "C.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "C.png"));
                return layered;
            case "D.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "D.png"));
                return layered;
            case "E.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "E.png"));
                return layered;
            case "F.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "F.png"));
                return layered;
            case "G.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "G.png"));
                return layered;
            case "H.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "H.png"));
                return layered;
            case "I.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "I.png"));
                return layered;
            case "J.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "J.png"));
                return layered;
            case "K.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "K.png"));
                return layered;
            case "L.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "L.png"));
                return layered;
            case "M.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "M.png"));
                return layered;
            case "N.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "N.png"));
                return layered;
            case "O.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "O.png"));
                return layered;
            case "P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "P.png"));
                return layered;
            case "Q.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "Q.png"));
                return layered;
            case "R.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "R.png"));
                return layered;
            case "S.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "S.png"));
                return layered;
            case "T.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "T.png"));
                return layered;
            case "U.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "U.png"));
                return layered;
            case "V.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "V.png"));
                return layered;
            case "W.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "W.png"));
                return layered;
            case "X.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "X.png"));
                return layered;
            case "Y.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "Y.png"));
                return layered;
            case "Z.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/English Letters/", "Z.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsClan(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Binary.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(04) Binary.png"));
                return layered;
            case "Cluster.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(08) Cluster.png"));
                return layered;
            case "Galaxy.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(09) Galaxy.png"));
                return layered;
            case "Nova.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(03) Nova.png"));
                return layered;
            case "Point.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(01) Point.png"));
                return layered;
            case "Star.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(02) Star.png"));
                return layered;
            case "Supernova Trinary.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/",
                                "(07) Supernova Trinary.png"));
                return layered;
            case "Supernova.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(05) Supernova.png"));
                return layered;
            case "Trinary.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Formation/Clan/", "(06) Trinary.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsGreekLetters(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Alpha.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(01) ALPHA.png"));
                return layered;
            case "Beta.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(02) BETA.png"));
                return layered;
            case "Chi.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(22) CHI.png"));
                return layered;
            case "Delta.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(04) DELTA.png"));
                return layered;
            case "Epsilon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(05) EPSILON.png"));
                return layered;
            case "Epsilon_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(05) Epsilon.png"));
                return layered;
            case "Eta.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(07) ETA.png"));
                return layered;
            case "Gamma.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(03) GAMMA.png"));
                return layered;
            case "Iota.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(09) IOTA.png"));
                return layered;
            case "Iota_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(09) Iota.png"));
                return layered;
            case "Kappa.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(10) KAPPA.png"));
                return layered;
            case "Lambda.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(11) LAMBDA.png"));
                return layered;
            case "Lambda_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(11) Lambda.png"));
                return layered;
            case "Mu.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(12) MU.png"));
                return layered;
            case "Mu_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(12) Mu.png"));
                return layered;
            case "Nu.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(13) NU.png"));
                return layered;
            case "Omega.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(24) OMEGA.png"));
                return layered;
            case "Omega_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(15) Omicron.png"));
                return layered;
            case "Omicron.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(15) OMICRON.png"));
                return layered;
            case "Phi.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(21) PHI.png"));
                return layered;
            case "Pi.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(16) PI.png"));
                return layered;
            case "Pi_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(16) Pi.png"));
                return layered;
            case "Psi.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(23) PSI.png"));
                return layered;
            case "Psi_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(23) Psi.png"));
                return layered;
            case "Rho.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(17) RHO.png"));
                return layered;
            case "Rho_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(17) Rho.png"));
                return layered;
            case "Sigma.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(18) SIGMA.png"));
                return layered;
            case "Tau.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(19) TAU.png"));
                return layered;
            case "Theta.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(08) THETA.png"));
                return layered;
            case "Theta_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(08) Theta.png"));
                return layered;
            case "Upsilon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(20) UPSILON.png"));
                return layered;
            case "Xi.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(14) XI.png"));
                return layered;
            case "Xi_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(14) Xi.png"));
                return layered;
            case "Zeta.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Alphabet/",
                                "(06) ZETA.png"));
                return layered;
            case "Zeta_Icon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Greek Letters/Upper Case/",
                                "(06) Zeta.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsIS(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "battalion.png":
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(08) Battalion.png"));
                return layered;
            case "Level I.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Roman Numerals/",
                                "(01) Roman I.png"));
                return layered;
            case "Level II.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Roman Numerals/",
                                "(02) Roman II.png"));
                return layered;
            case "Level III.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Roman Numerals/",
                                "(03) Roman III.png"));
                return layered;
            case "Level IV.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Roman Numerals/",
                                "(04) Roman IV.png"));
                return layered;
            case "regiment.png":
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(10) Regiment.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsNumeric(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "1.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "01.png"));
                return layered;
            case "2.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "02.png"));
                return layered;
            case "3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "03.png"));
                return layered;
            case "4.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "04.png"));
                return layered;
            case "5.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "05.png"));
                return layered;
            case "6.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "06.png"));
                return layered;
            case "7.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "07.png"));
                return layered;
            case "8.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "08.png"));
                return layered;
            case "9.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Numbers/", "09.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsPhoenetic(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Alpha.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "ALFA.png"));
                return layered;
            case "Bravo.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "BRAVO.png"));
                return layered;
            case "Charlie.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "CHARLIE.png"));
                return layered;
            case "Delta.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "DELTA.png"));
                return layered;
            case "Echo.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "ECHO.png"));
                return layered;
            case "Foxtrot.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "FOXTROT.png"));
                return layered;
            case "Golf.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "GOLF.png"));
                return layered;
            case "Hotel.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "HOTEL.png"));
                return layered;
            case "India.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "INDIA.png"));
                return layered;
            case "Juliet.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "JULIETT.png"));
                return layered;
            case "Kilo.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "KILO.png"));
                return layered;
            case "Lima.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "LIMA.png"));
                return layered;
            case "Mike.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "MIKE.png"));
                return layered;
            case "Novemeber.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "NOVEMBER.png"));
                return layered;
            case "Oscar.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "OSCAR.png"));
                return layered;
            case "Papa.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "PAPA.png"));
                return layered;
            case "Quebec.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "QUEBEC.png"));
                return layered;
            case "Romeo.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "ROMEO.png"));
                return layered;
            case "Sierra.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "SIERRA.png"));
                return layered;
            case "Tango.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "TANGO.png"));
                return layered;
            case "Uniform.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "UNIFORM.png"));
                return layered;
            case "Victor.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "VICTOR.png"));
                return layered;
            case "Whiskey.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "WHISKEY.png"));
                return layered;
            case "X-Ray.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "X-RAY.png"));
                return layered;
            case "Yankee.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "YANKEE.png"));
                return layered;
            case "Zulu.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/ICAO 1956/", "ZULU.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardInfantry(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "AirborneAssaultInfantryPlatoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Air Mobile.png"));
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Paratrooper.png"));
                return layered;
            case "infantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                return layered;
            case "infantrycompany.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                return layered;
            case "infantrylaserplatoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "LAS.png"));
                return layered;
            case "infantrylrmplatoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "LRM.png"));
                return layered;
            case "infantryplatoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "jinfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Jump Infantry.png"));
                return layered;
            case "jinfantrylrmplatoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Jump Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "LRM.png"));
                return layered;
            case "jinfantryplatoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Jump Infantry.png"));
                return layered;
            case "mechanizedinfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                return layered;
            case "mhinfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                return layered;
            case "mountaininfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mountaineer.png"));
                return layered;
            case "mtinfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                return layered;
            case "mvinfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                return layered;
            case "mwinfantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                return layered;
            case "Scuba Infantry Motorized.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Scuba (Bottom).png"));
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Motorized.png"));
                return layered;
            case "Scuba Infantry.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Scuba (Bottom).png"));
                return layered;
            case "Space Marines.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Infantry.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Space.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardMech(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "amech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Assault.png"));
                return layered;
            case "aomnimech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Assault.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Omni.png"));
                return layered;
            case "hmech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                return layered;
            case "hmechlance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "hmechlancereinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                return layered;
            case "hmechlancereinforcedaero.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                "A.png"));
                return layered;
            case "homnimech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Heavy.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Omni.png"));
                return layered;
            case "HQmech3elements.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Headquarters.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                return layered;
            case "HQmechlance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Headquarters.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "HQmechlancereinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Headquarters.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                return layered;
            case "HQmechlancereinforcedaero.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Headquarters.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                "A.png"));
                return layered;
            case "imech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Industrial.png"));
                return layered;
            case "lamech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Aerospace (Right).png"));
                return layered;
            case "lmech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                return layered;
            case "lmech3elements.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                return layered;
            case "lmechlance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "lmechlancereinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                return layered;
            case "lmechlancereinforcedaero.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                "A.png"));
                return layered;
            case "lomnimech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Light.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Omni.png"));
                return layered;
            case "mech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Center).png"));
                return layered;
            case "mechcompany.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Center).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                return layered;
            case "mmech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                return layered;
            case "mmechlance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "mmechlancereinforcedaero.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/English Letters/",
                                "A.png"));
                return layered;
            case "momnimech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medium.png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "StratOps/", "Omni.png"));
                return layered;
            case "nova.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Bottom).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                "Battle Armor (Extended).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "MVO.png"));
                return layered;
            case "pmech.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "BattleMech (Left).png"));
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "ProtoMech.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardMilitary(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Anti Air Arty.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "AAA (Anti-Aircraft Artillery).png"));
                return layered;
            case "Anti Air.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "AA (Anti-Aircraft).png"));
                return layered;
            case "Battalion.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "BN (Battalion).png"));
                return layered;
            case "Brigade.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "BDE (Brigade).png"));
                return layered;
            case "Close Air Support.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "CAS (Close Air Support).png"));
                return layered;
            case "Company.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "CO (Company).png"));
                return layered;
            case "Division.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "DIV (Division).png"));
                return layered;
            case "Field Arty.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "FA (Field Artillery).png"));
                return layered;
            case "Fire Support Team.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "FIRE (Fire Support).png"));
                return layered;
            case "Headquarters.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "HQ (Headquarters).png"));
                return layered;
            case "Lance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "LANCE.png"));
                return layered;
            case "Logistical Support.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "SPT (Support).png"));
                return layered;
            case "Main Operating Base.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "MOB (Mobile).png"));
                return layered;
            case "Platoon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "PLT (Platoon).png"));
                return layered;
            case "Recon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "RECON.png"));
                return layered;
            case "Regiment.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Order of Battle/",
                                "RGT (Regiment).png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardMiscellaneous(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "artillery.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "Artillery.png"));
                return layered;
            case "HQ.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "HQ (Headquarters).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Command and Control.png"));
                return layered;
            case "lmhq.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled HQ).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Command and Control.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "maintenance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Maintenance.png"));
                return layered;
            case "Medical Lance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medical.png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                return layered;
            case "mmhq.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled HQ).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Command and Control.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            case "paramedic.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "Medical.png"));
                return layered;
            case "recon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Recon.png"));
                return layered;
            case "specialforcessquad.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "SF (Special Forces).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                return layered;
            case "supply.png":
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Supply.png"));
                return layered;
            case "transport.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Transport.png"));
                return layered;
            case "xenoplanetary.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/",
                                "Infantry (Xenoplanetary).png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardNames(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Anvil.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/", "ANVIL.png"));
                return layered;
            case "Assault.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Weight/", "ASSAULT.png"));
                return layered;
            case "Battle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/", "BATTLE.png"));
                return layered;
            case "Cavarly.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "CAV (Cavalry).png"));
                return layered;
            case "Command.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "CMD (Command).png"));
                return layered;
            case "Fire.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/",
                                "FIRE (Fire Support).png"));
                return layered;
            case "Gun.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/", "GUN.png"));
                return layered;
            case "Hammer.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/", "HAMMER.png"));
                return layered;
            case "Heavy.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Weight/", "HEAVY.png"));
                return layered;
            case "Jump.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "JUMP.png"));
                return layered;
            case "Light.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Weight/", "LIGHT.png"));
                return layered;
            case "Medium.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Weight/", "MEDIUM.png"));
                return layered;
            case "Pursuit.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "PURSUIT.png"));
                return layered;
            case "Recon.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "RECON.png"));
                return layered;
            case "Scout.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Type/", "SCOUT.png"));
                return layered;
            case "Strike.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/", "STRIKE.png"));
                return layered;
            case "Striker.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/",
                                "STRIKER.png"));
                return layered;
            case "Sweep.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Alphanumeric/Lance Names/", "SWEEP.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardNaval(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "adropship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "DropShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                return layered;
            case "battlecruiser.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CB.png"));
                return layered;
            case "battleship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "BB.png"));
                return layered;
            case "bdropship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "DropShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "B.png"));
                return layered;
            case "carrier.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CV.png"));
                return layered;
            case "cdropship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "DropShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "C.png"));
                return layered;
            case "corvette.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "PC.png"));
                return layered;
            case "cruiser.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CA.png"));
                return layered;
            case "cvdropship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "DropShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CV.png"));
                return layered;
            case "destroyer.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "DD.png"));
                return layered;
            case "frigate.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "FF.png"));
                return layered;
            case "heavycruiser.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CH.png"));
                return layered;
            case "iiijumpship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "JumpShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                "(03) Roman III.png"));
                return layered;
            case "iijumpship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "JumpShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                "(02) Roman II.png"));
                return layered;
            case "ijumpship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "JumpShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                "(01) Roman I.png"));
                return layered;
            case "ivjumpship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "JumpShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/Roman Numerals/",
                                "(04) Roman IV.png"));
                return layered;
            case "jumpship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "JumpShip.png"));
                return layered;
            case "qdropship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "DropShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "Q.png"));
                return layered;
            case "survey.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "PS.png"));
                return layered;
            case "transportcruiser.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "StratOps/", "WarShip.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/", "CT.png"));
                return layered;
            case "warship.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "WarShip.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardUnits(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Arturanguards25th.jpg":
                return new StandardForceIcon(icon.getCategory(), "25th Arcturan Guards.jpg");
            case "Black_Widow_Company.jpg":
                return new StandardForceIcon(icon.getCategory(), "Black Widow Company.jpg");
            case "CapellanConfederation.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                "Capellan Confederation.png"));
                return layered;
            case "CircinusFederation.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Circinus Federation.png"));
                return layered;
            case "ClanCloudCobra.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Cloud Cobra.png"));
                return layered;
            case "ClanCoyote.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Coyote.png"));
                return layered;
            case "ClanDiamondShark.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Diamond Shark.png"));
                return layered;
            case "ClanGhostBear.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Ghost Bear.png"));
                return layered;
            case "ClanSteelViper.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Steel Viper.png"));
                return layered;
            case "ClanWolverine.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Clan/", "Clan Wolverine.png"));
                return layered;
            case "ComStar.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "ComStar.png"));
                return layered;
            case "DraconisCombine.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Draconis Combine.png"));
                return layered;
            case "FederatedCommonwealth.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                "Federated Commonwealth.png"));
                return layered;
            case "FederatedSuns.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Federated Suns.png"));
                return layered;
            case "foxsteeth.png":
                return new StandardForceIcon(icon.getCategory(), "Fox's Teeth.png");
            case "FreeRasalhagueRepublic.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                "Free Rasalhague Republic.png"));
                return layered;
            case "FreeWorldsLeague.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Free Worlds League.png"));
                return layered;
            case "JadeFalconDelta.gif":
                return new StandardForceIcon(icon.getCategory(), "Jade Falcon Delta Galaxy.gif");
            case "LyranAlliance.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Lyran Alliance.png"));
                return layered;
            case "MagistryOfCanopus.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Magistracy of Canopus.png"));
                return layered;
            case "MarianHegemony.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Marian Hegemony.png"));
                return layered;
            case "Opacus_Venatori.jpg":
                return new StandardForceIcon(icon.getCategory(), "Opacus Venatori.jpg");
            case "OutworldsAlliance.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Outworlds Alliance.png"));
                return layered;
            case "RepublicOfTheSphere.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/",
                                "Republic of the Sphere.png"));
                return layered;
            case "SorensonSabres.jpg":
                return new StandardForceIcon(icon.getCategory(), "Sorenson's Sabres.jpg");
            case "StarLeague.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Star League.png"));
                return layered;
            case "TaurianConcordat.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Periphery/", "Taurian Concordat.png"));
                return layered;
            case "WordOfBlake.png":
                layered.getPieces().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.LOGO)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO, "Inner Sphere/", "Word of Blake.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardVehicles(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "atvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                return layered;
            case "awvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                return layered;
            case "htrackedapc.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "H.png"));
                return layered;
            case "htvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "H.png"));
                return layered;
            case "htvehiclecompany.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "H.png"));
                return layered;
            case "htvehiclelance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "H.png"));
                return layered;
            case "hwvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "H.png"));
                return layered;
            case "hwvehiclelance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "H.png"));
                return layered;
            case "lhoverapc.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "lhvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "lhvehiclelance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "ltrackedapc.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "ltvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "lwheeledapc.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "lwvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "L.png"));
                return layered;
            case "mhoverapc.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            case "mhvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            case "mhvehiclelance.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Hover Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            case "mtrackedapc.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ADJUSTMENT)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ADJUSTMENT, "NATO/", "Mechanized.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            case "mtvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            case "mwvehicle.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/", "Vehicle (Tracked Wheeled).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "M.png"));
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardVTOL(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Basic Attack Vtol C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Attack Vtol C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Attack Vtol C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Attack Vtol C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "Basic Attack Vtol EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Attack Vtol EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Attack Vtol P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Attack Vtol.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "Basic Cargo Vtol C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Cargo Vtol C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Cargo Vtol C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Cargo Vtol C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "Basic Cargo Vtol EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Cargo Vtol EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Cargo Vtol P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Cargo Vtol.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "Basic Recon Vtol C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Recon Vtol C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Recon Vtol C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Recon Vtol C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "Basic Recon Vtol EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Recon Vtol EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Recon Vtol P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Recon Vtol.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "Basic Vtol C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Vtol C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Vtol C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Vtol C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                return layered;
            case "Basic Vtol EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Vtol EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "Basic Vtol P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "Basic Vtol.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                return layered;
            case "rvtol.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Civilian).png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "R.png"));
                return layered;
            case "rvtolattackcargoflight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Civilian).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom Right/English Letters/",
                                "C.png"));
                return layered;
            case "rvtolattackcargoflightreinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Civilian).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom Right/English Letters/",
                                "C.png"));
                return layered;
            case "VTOL Attack Element C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Element C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Element EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Element Reinforced C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element Reinforced C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element Reinforced C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Element Reinforced C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Element Reinforced EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element Reinforced EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Element Reinforced P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Element Reinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Element.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Flight C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Flight C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Flight EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Flight Reinforced C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight Reinforced C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight Reinforced C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Flight Reinforced C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Flight Reinforced EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight Reinforced EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Flight Reinforced P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Flight Reinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Flight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Single C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Single C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Single C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Single C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Single EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Single EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Single P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Single.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Squadron C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Squadron C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Squadron C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Squadron C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Squadron EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Squadron EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Squadron P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Squadron.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Wing C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Wing C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Wing C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Wing C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Attack Wing EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Wing EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Attack Wing P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Attack Wing.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "A.png"));
                return layered;
            case "VTOL Cargo Element C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Element C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Element EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Element EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Element Reinforced C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element Reinforced C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element Reinforced C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Element Reinforced C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Element Reinforced EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element Reinforced EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Element Reinforced P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Element Reinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Element.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Flight C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Flight C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Flight EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Flight Reinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Flight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Single C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Single C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Single C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Single C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Single EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Single EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Single P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Single.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Squadron C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Squadron C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Squadron C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Squadron C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Squadron EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Squadron EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Squadron P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Squadron.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Wing C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Wing C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Wing C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Wing C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Cargo Wing EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Wing EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Cargo Wing P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Cargo Wing.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "C.png"));
                return layered;
            case "VTOL Recon Attack Flight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Civilian).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "A.png"));
                return layered;
            case "VTOL Recon Element C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Element C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Element EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Element Reinforced C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element Reinforced C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element Reinforced C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Element Reinforced C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Element Reinforced EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element Reinforced EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Element Reinforced P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Element Reinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(03A) Augmented Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Element.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(03) Team.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Flight C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Flight C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Flight EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Flight Reinforced C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight Reinforced C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight Reinforced C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Flight Reinforced C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Flight Reinforced EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight Reinforced EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Flight Reinforced P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Flight Reinforced.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(04A) Augmented Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Flight.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(04) Lance.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Single C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Single C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Single C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Single C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Single EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Single EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Single P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Single.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(02) Individual.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Squadron C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Squadron C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Squadron C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Squadron C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Squadron EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Squadron EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Squadron P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Squadron.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/", "(05) Company.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Wing C3 EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Wing C3 EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Wing C3 P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Wing C3.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Left/", "C3.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "VTOL Recon Wing EW P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Wing EW.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Bottom/", "EW.png"));
                return layered;
            case "VTOL Recon Wing P.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top Right/English Letters/",
                                "P.png"));
                return layered;
            case "VTOL Recon Wing.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "NATO/",
                                "Aviation, Rotary Wing (Military).png"));
                layered.getPieces().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.FORMATION)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION, "Inner Sphere/",
                                "(06) Battalion.png"));
                layered.getPieces().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC, "Top/", "R.png"));
                return layered;
            case "vtol.png":
                layered.getPieces().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getPieces().get(LayeredForceIconLayer.TYPE)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE, "Graphical/", "VTOL.png"));
                return layered;
            default:
                return icon;
        }
    }
    // endregion Standard Force Icon

    // region Legacy Save Format
    public static void migrateLegacyIconMapNodes(final LayeredForceIcon icon, final Node wn) {
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            if ((wn2.getNodeType() != Node.ELEMENT_NODE) || !wn2.hasChildNodes()) {
                continue;
            }
            final String oldKey = wn2.getAttributes().getNamedItem("key").getTextContent();
            LayeredForceIconLayer key;
            if ("Pieces/Type/".equalsIgnoreCase(oldKey)) {
                key = LayeredForceIconLayer.TYPE;
            } else {
                key = Arrays.stream(LayeredForceIconLayer.values())
                        .filter(layer -> layer.getLayerPath().equalsIgnoreCase(oldKey))
                        .findFirst().orElse(null);
            }

            if (key == null) {
                continue;
            }
            final List<ForcePieceIcon> values = processIconMapSubNodes(wn2.getChildNodes(), key);
            if (!values.isEmpty()) {
                icon.getPieces().put(key, values);
            }
        }
    }

    private static List<ForcePieceIcon> processIconMapSubNodes(final NodeList nl, final LayeredForceIconLayer layer) {
        return IntStream.range(0, nl.getLength())
                .mapToObj(nl::item)
                .filter(wn2 -> wn2.getNodeType() == Node.ELEMENT_NODE)
                .map(wn2 -> wn2.getAttributes().getNamedItem("name").getTextContent())
                .filter(value -> (value != null) && !value.isEmpty())
                .map(value -> new ForcePieceIcon(layer, "", value))
                .collect(Collectors.toList());
    }
    // endregion Legacy Save Format
    // endregion 0.49.6
}

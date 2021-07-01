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
package mekhq.campaign.io.Migration;

import mekhq.MekHQ;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This migrates Force icons from varied sources to Kailan's Pack
 * This migration occurred in 0.49.3
 */
public class ForceIconMigrator {
    public static StandardForceIcon migrateForceIcon(final StandardForceIcon icon) {
        if (icon instanceof LayeredForceIcon) {
            return migrateLayeredForceIcon((LayeredForceIcon) icon);
        } else if (icon instanceof UnitIcon) {
            return icon; // Assume they properly set this value
        } else if (icon != null) {
            return migrateStandardForceIcon(icon);
        } else {
            MekHQ.getLogger().error("Attempted to migrate a null icon, when AbstractIcon is non-nullable by design");
            return new StandardForceIcon();
        }
    }

    //region Layered Force Icon
    private static LayeredForceIcon migrateLayeredForceIcon(final LayeredForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        migrateAdjustments(icon, layered);
        migrateAlphanumerics(icon, layered);
        migrateBackgrounds(icon, layered);
        migrateFormations(icon, layered);
        migrateFrames(icon, layered);
        migrateLogos(icon, layered);
        migrateSpecialModifiers(icon, layered);
        migrateTypes(icon, layered);
        return icon;
    }

    private static void migrateAdjustments(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>())) {
            switch (path) {
                case "Air assault.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Air Mobile.png");
                    break;
                case "Artillery_Missile.png":
                case "Artillery_Missile_small.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Artillery (Missile).png");
                    break;
                case "Artillery_Missile_Multiple.png":
                case "Artillery_Missile_Multiple_small.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Artillery (Multiple Missile).png");
                    break;
                case "Artillery_tube.png":
                case "Artillery_tube_small.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Artillery.png");
                    break;
                case "Command and Control.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Command and Control.png");
                    break;
                case "Drone Carrier.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone Carrier.png");
                    break;
                case "Infantry_Motorized.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Motorized.png");
                    break;
                case "Battle Armor Transport.png":
                case "Mechanized.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                    break;
                case "Mountaineers.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mountaineer.png");
                    break;
                case "Paratrooper.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Paratrooper.png");
                    break;
                case "Recon.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Recon.png");
                    break;
                case "Scuba.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Scuba.png");
                    break;
                case "Supply.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Supply.png");
                    break;
                case "Infantry_Jumppng":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Jump Infantry (Large).png");
                    break;
                case "Infantry_SpaceMarine.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Space.png");
                    break;
                case "Omni.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Omni.png");
                    break;

                default:
                    break;
            }
        }
    }

    private static void migrateAlphanumerics(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>())) {
            switch (path) {
                case "A I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "A II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "A III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "A IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "A V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "B I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/B.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "B II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/B.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "B III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/B.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "B IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/B.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "B V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/B.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "C I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/C.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "C II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/C.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "C III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/C.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "C IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/C.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "C V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/C.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "D I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/D.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "D II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/D.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "D III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/D.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "D IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/D.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "D V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/D.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "E I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/E.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "E II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/E.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "E III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/E.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "E IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/E.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "E V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/E.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "F I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/F.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "F II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/F.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "F III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/F.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "F IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/F.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "F V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/F.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "G I Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/G.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                    break;
                case "G II Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/G.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "G III Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/G.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "G IV Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/G.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "G V Low.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/G.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(05) Roman V.png");
                    break;
                case "A Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                    break;
                case "A Low.png":
                case "A.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                    break;
                case "AP.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/AP.png");
                    break;
                case "B Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/B.png");
                    break;
                case "B Low.png":
                case "B.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/B.png");
                    break;
                case "BB.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/BB.png");
                    break;
                case "BT.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/BT.png");
                    break;
                case "C Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/C.png");
                    break;
                case "C Low.png":
                case "C.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/C.png");
                    break;
                case "CA.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CA.png");
                    break;
                case "CB.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CB.png");
                    break;
                case "CH.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CH.png");
                    break;
                case "CP.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CP.png");
                    break;
                case "CT.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CT.png");
                    break;
                case "CV.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CV.png");
                    break;
                case "D Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/D.png");
                    break;
                case "D Low.png":
                case "D.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/D.png");
                    break;
                case "DCV.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/DCV.png");
                    break;
                case "DD.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/DD.png");
                    break;
                case "DH.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/DH.png");
                    break;
                case "DS.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/DS.png");
                    break;
                case "E Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/E.png");
                    break;
                case "E Low.png":
                case "E.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/E.png");
                    break;
                case "EP.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/EP.png");
                    break;
                case "F Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/F.png");
                    break;
                case "F Low.png":
                case "F.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/F.png");
                    break;
                case "FR.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/FR.png");
                    break;
                case "G Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/G.png");
                    break;
                case "G Low.png":
                case "G.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/G.png");
                    break;
                case "H Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/H.png");
                    break;
                case "H Low.png":
                case "H.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                    break;
                case "I Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/I.png");
                    break;
                case "I Low.png":
                case "I.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/I.png");
                    break;
                case "II Low.png":
                case "II.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                    break;
                case "III Low.png":
                case "III.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                    break;
                case "IV Low.png":
                case "IV.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                    break;
                case "IX Low.png":
                case "IX.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(09) Roman IX.png");
                    break;
                case "J Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/J.png");
                    break;
                case "J Low.png":
                case "J.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/J.png");
                    break;
                case "K Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/K.png");
                    break;
                case "K Low.png":
                case "K.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/K.png");
                    break;
                case "L Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/L.png");
                    break;
                case "L Low.png":
                case "L.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                    break;
                case "LB.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LB.png");
                    break;
                case "LC.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LC.png");
                    break;
                case "LG.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LG.png");
                    break;
                case "LH.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LH.png");
                    break;
                case "LM.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LM.png");
                    break;
                case "LP.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LP.png");
                    break;
                case "LT.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/LT.png");
                    break;
                case "M Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/M.png");
                    break;
                case "M Low.png":
                case "M.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                    break;
                case "MY.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/MY.png");
                    break;
                case "N Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/N.png");
                    break;
                case "N Low.png":
                case "N.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/N.png");
                    break;
                case "NF.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/NF.png");
                    break;
                case "NX.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/NX.png");
                    break;
                case "O Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/O.png");
                    break;
                case "O Low.png":
                case "O.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/O.png");
                    break;
                case "P Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/P.png");
                    break;
                case "P Low.png":
                case "P.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                    break;
                case "PC.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/PC.png");
                    break;
                case "PS.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/PS.png");
                    break;
                case "PT.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/PT.png");
                    break;
                case "Q Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/Q.png");
                    break;
                case "Q Low.png":
                case "Q.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/Q.png");
                    break;
                case "R Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/R.png");
                    break;
                case "R Low.png":
                case "R.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/R.png");
                    break;
                case "S Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/S.png");
                    break;
                case "S Low.png":
                case "S.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/S.png");
                    break;
                case "SB.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/SB.png");
                    break;
                case "SL.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/SL.png");
                    break;
                case "SS.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/SS.png");
                    break;
                case "ST.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/ST.png");
                    break;
                case "T Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/T.png");
                    break;
                case "T Low.png":
                case "T.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/T.png");
                    break;
                case "U Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/U.png");
                    break;
                case "U Low.png":
                case "U.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/U.png");
                    break;
                case "V Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/V.png");
                    break;
                case "V Low.png":
                case "V.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/V.png");
                    break;
                case "VI Low.png":
                case "VI.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(06) Roman VI.png");
                    break;
                case "VII Low.png":
                case "VII.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(07) Roman VII.png");
                    break;
                case "VIII Low.png":
                case "VIII.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(08) Roman VIII.png");
                    break;
                case "W Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/W.png");
                    break;
                case "W Low.png":
                case "W.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/W.png");
                    break;
                case "X Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/X.png");
                    break;
                case "X Low.png":
                case "X.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/X.png");
                    break;
                case "Y Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/Y.png");
                    break;
                case "Y Low.png":
                case "Y.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/Y.png");
                    break;
                case "YC.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YC.png");
                    break;
                case "YH.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YH.png");
                    break;
                case "YLG.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YLG.png");
                    break;
                case "YLH.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YLH.png");
                    break;
                case "YLT.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YLT.png");
                    break;
                case "YMY.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YMY.png");
                    break;
                case "YNX.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YNX.png");
                    break;
                case "YP.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YP.png");
                    break;
                case "YR.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YR.png");
                    break;
                case "YT.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YT.png");
                    break;
                case "YX.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/YX.png");
                    break;
                case "Z Low Left.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/Z.png");
                    break;
                case "Z Low.png":
                case "Z.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/Z.png");
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateBackgrounds(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.BACKGROUND, new ArrayList<>())) {
            switch (path) {
                case "CDS.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Diamond Shark.png");
                    break;
                case "CGB.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Ghost Bear.png");
                    break;
                case "CHH.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Hell's Horses.png");
                    break;
                case "CJF.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Jade Falcon.png");
                    break;
                case "CNC Alternate.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Nova Cat (Alternate).png");
                    break;
                case "CNC.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Nova Cat.png");
                    break;
                case "CSR Alternate.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Snow Raven (Alternate).png");
                    break;
                case "CSR.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Snow Raven.png");
                    break;
                case "CW.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Clan/Clan Wolf.png");
                    break;
                case "Capellan Confederation.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Capellan Confederation.png");
                    break;
                case "Comstar background.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/ComStar.png");
                    break;
                case "Draconis Combine.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Draconis Combine.png");
                    break;
                case "Federated Suns.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Federated Suns.png");
                    break;
                case "Free Rasalhague Republic.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Free Rasalhague Republic.png");
                    break;
                case "Free Worlds League.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Free Worlds League.png");
                    break;
                case "Lyran Commonwealth.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Lyran Commonwealth.png");
                    break;
                case "Republic of the sphere.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Inner Sphere/Republic of the Sphere.png");
                    break;
                case "Magistracy of Canopus alternate.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Magistracy of Canopus (Alternate).png");
                    break;
                case "Magistracy of Canopus.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Magistracy of Canopus.png");
                    break;
                case "Marian Hegemony.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Marian Hegemony.png");
                    break;
                case "Outworlds alliance alternate.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Outworlds Alliance (Alternate).png");
                    break;
                case "Outworlds alliance.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Outworlds Alliance.png");
                    break;
                case "Rim worlds Republic.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Rim Worlds Republic.png");
                    break;
                case "Tarurian Concordat .png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Tarurian Concordat.png");
                    break;
                case "Tarurian Concordat alternate.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Periphery/Tarurian Concordat (Alternate).png");
                    break;
                case "Merc.png":
                    migrated.getIconMap().put(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.BACKGROUND).add("Mercenary.png");
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateFormations(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.FORMATION, new ArrayList<>())) {
            switch (path) {
                case "00 Installation.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(00) Installation.png");
                    break;
                case "01 Fire Team.png":
                case "01 FireTeam.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(01) Fire Team.png");
                    break;
                case "02 Individual.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                    break;
                case "03 Team.png":
                case "03Team.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                    break;
                case "04 Lance.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                    break;
                case "05 Lance Augmented.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                    break;
                case "06 Company.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                    break;
                case "07 Company Task Force.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(08) Company Task Force.png");
                    break;
                case "08 Battalion.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                    break;
                case "09 Battlegroup.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(10) Battlegroup.png");
                    break;
                case "10 Regiment.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(11) Regiment.png");
                    break;
                case "11 Brigade.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(13) Brigade.png");
                    break;
                case "12 Division.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(15) Division.png");
                    break;
                case "13 Corps.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(17) Corps.png");
                    break;
                case "14 Field Army.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(19) Field Army.png");
                    break;
                case "15 Army Group.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(21) Army Group.png");
                    break;
                case "Brigade Augmented.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(14) Augmented Brigade.png");
                    break;
                case "Corps Augmented.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(18) Augmented Corps.png");
                    break;
                case "Division Augmented.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(16) Augmented Division.png");
                    break;
                case "Field Army Augmented.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(20) Augmented Field Army.png");
                    break;
                case "Regiment Augmented.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(12) Augmented Regiment.png");
                    break;
                case "16 Level I.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(01) Level I.png");
                    break;
                case "17 Level II.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(02) Level II.png");
                    break;
                case "18 Choir.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(03) Choir.png");
                    break;
                case "19 Level III.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(04) Level III.png");
                    break;
                case "20 Level IV.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(05) Level IV.png");
                    break;
                case "21 Level V.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(06) Level V.png");
                    break;
                case "22 Level VI.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("ComStar/(07) Level VI.png");
                    break;
                case "23 Point.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(01) Point.png");
                    break;
                case "24 Star.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(02) Star.png");
                    break;
                case "25 Binary.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(04) Binary.png");
                    break;
                case "26 Nova.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(03) Nova.png");
                    break;
                case "27 Trinary.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(06) Trinary.png");
                    break;
                case "28 SuperNova Binary.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(05) SuperNova Binary.png");
                    break;
                case "29 SuperNova Trinary.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(07) SuperNova Trinary.png");
                    break;
                case "30 Cluster.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(08) Cluster.png");
                    break;
                case "31 Galaxy.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Clan/(09) Galaxy.png");
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateFrames(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.FRAME, new ArrayList<>())) {
            switch (path) {
                case "Extended Frame.png":
                case "Frame.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.FRAME, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.FRAME).add("Frame.png");
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateLogos(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.LOGO, new ArrayList<>())) {
            switch (path) {
                case "Capellan Confederation.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Capellan Confederation.png");
                    break;
                case "ComStar.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/ComStar.png");
                    break;
                case "Draconis Combine.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Draconis Combine.png");
                    break;
                case "Federated Commonwealth.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Federated Commonwealth.png");
                    break;
                case "Federated Suns.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Federated Suns.png");
                    break;
                case "Free Rasalhague Republic.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Free Rasalhague Republic.png");
                    break;
                case "Free Worlds League.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Free Worlds League.png");
                    break;
                case "Lyran Alliance.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Lyran Alliance.png");
                    break;
                case "Lyran Commonwealth.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Lyran Commonwealth.png");
                    break;
                case "Rim Worlds Republic.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Rim Worlds Republic.png");
                    break;
                case "Star League.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Star League.png");
                    break;
                case "Word of Blake.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Word of Blake.png");
                    break;
                case "Clan Blood Spirit.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Blood Spirit.png");
                    break;
                case "Clan Cloud Cobra.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Cloud Cobra.png");
                    break;
                case "Clan Coyote.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Coyote.png");
                    break;
                case "Clan Diamond Shark.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Diamond Shark.png");
                    break;
                case "Clan Fire Mandril.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Fire Mandril.png");
                    break;
                case "Clan Ghost Bear.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Ghost Bear.png");
                    break;
                case "Clan Goliath Scorpion.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Goliath Scorpion.png");
                    break;
                case "Clan Hells Horses.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Hells Horses.png");
                    break;
                case "Clan Ice Hellion.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Ice Hellion.png");
                    break;
                case "Clan Jade Falcon.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Jade Falcon.png");
                    break;
                case "Clan Mongoose.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Mongoose.png");
                    break;
                case "Clan Nova Cat.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Nova Cat.png");
                    break;
                case "Clan Smoke Jaguar.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Smoke Jaguar.png");
                    break;
                case "Clan Snow Raven.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Snow Raven.png");
                    break;
                case "Clan Star Adder.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Star Adder.png");
                    break;
                case "Clan Steel Viper.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Steel Viper.png");
                    break;
                case "Clan Widowmaker.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Widowmaker.png");
                    break;
                case "Clan Wolf in Exile.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Wolf-in-Exile.png");
                    break;
                case "Clan Wolf.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Wolf.png");
                    break;
                case "Clan Wolverine.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Wolverine.png");
                    break;
                case "Circinus Federation.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Circinus Federation.png");
                    break;
                case "Magistracy of Canopus.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Magistracy of Canopus.png");
                    break;
                case "Marian Hegemony.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Marian Hegemony.png");
                    break;
                case "Outworlds Alliance.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Outworlds Alliance.png");
                    break;
                case "Taurian Concordat.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Taurian Concordat.png");
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateSpecialModifiers(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.SPECIAL_MODIFIER, new ArrayList<>())) {
            switch (path) {
                case "C3.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/C3.png");
                    break;
                case "C3i.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/C3i.png");
                    break;
                case "HQ indicator.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom Right/HQ.png");
                    break;
                default:
                    break;
            }
        }
    }

    private static void migrateTypes(final LayeredForceIcon original, final LayeredForceIcon migrated) {
        for (final String path : original.getIconMap().getOrDefault(LayeredForceIconLayer.TYPE, new ArrayList<>())) {
            switch (path) {
                case "Aerospace Heavy.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                    break;
                case "Aerospace Light.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                    break;
                case "Aerospace Medium.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                    break;
                case "Aerospace.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace.png");
                    break;
                case "Air Defense Artillery.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Air Defense.png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Artillery.png");
                    break;
                case "Air Defense Capital Weaponry.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Air Defense.png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Space.png");
                    break;
                case "Air Defense.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Air Defense.png");
                    break;
                case "Airship.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Airship.png");
                    break;
                case "Armoured_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Main Battle Tank.png");
                    break;
                case "Artillery_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Artillery.png");
                    break;
                case "Aviation VTOL.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Civilian).png");
                    break;
                case "Battle Armor.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Battle Armor (Extended).png");
                    break;
                case "BattleMech Assault.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Assault.png");
                    break;
                case "BattleMech Heavy.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                    break;
                case "BattleMech Light.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                    break;
                case "BattleMech Medium.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                    break;
                case "BattleMech Superheavy.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Superheavy.png");
                    break;
                case "BattleMech.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Center).png");
                    break;
                case "Drone Hover Converted.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone (Converted).png");
                    break;
                case "Drone Hover.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone (Military).png");
                    break;
                case "Drone Tracked Converted.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone (Converted).png");
                    break;
                case "Drone Tracked.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone (Military).png");
                    break;
                case "Drone Wheeled Converted.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone (Converted).png");
                    break;
                case "Drone Wheeled.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Drone (Military).png");
                    break;
                case "Dropship.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/DropShip.png");
                    break;
                case "Engineer.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Engineer.png");
                    break;
                case "Fixed Wing.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Fixed Wing (Civilian).png");
                    break;
                case "Headquarters.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/HQ (Headquarters).png");
                    break;
                case "Industrial Mechs.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Industrial.png");
                    break;
                case "Infantry Hover.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                    break;
                case "Infantry Jump.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Jump Infantry (Large).png");
                    break;
                case "Infantry Mechanized VTOL.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                    break;
                case "Infantry Mechanized.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                    break;
                case "Infantry Space Marine.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Space.png");
                    break;
                case "Infantry Tracked.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                    break;
                case "Infantry Wheeled.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled).png");
                    break;
                case "Infantry.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    break;
                case "Infantry_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Infantry.png");
                    break;
                case "Jumpship.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/JumpShip.png");
                    break;
                case "LAM.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Right).png");
                    break;
                case "Maintenance Alt.png":
                case "Maintenance Small.png":
                case "Maintenance.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Maintenance.png");
                    break;
                case "medical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medical.png");
                    break;
                case "Naval.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Naval.png");
                    break;
                case "Naval_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Naval (Destroyer).png");
                    break;
                case "Protomech.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/ProtoMech.png");
                    break;
                case "Rail Pressurized.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Rail (Pressurized).png");
                    break;
                case "Rail Unpressurized.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Rail (Unpressurized).png");
                    break;
                case "Satellite.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Satellite.png");
                    break;
                case "Scuba.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Scuba (Bottom).png");
                    break;
                case "Space Station.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Space Station.png");
                    break;
                case "Transport Medium.png":
                case "Transport Small.png":
                case "Transport.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Transport.png");
                    break;
                case "Transport_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Transport.png");
                    break;
                case "Turret_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Turret (AA).png");
                    break;
                case "UAV.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/UAV.png");
                    break;
                case "Vehicle Assorted.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Vehicle (Hover Wheeled Armoured).png");
                    break;
                case "Vehicle Hover Alt.png":
                case "Vehicle Hover Small.png":
                case "Vehicle Hover.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                    break;
                case "Vehicle Tracked Alt.png":
                case "Vehicle Tracked Small Artillery.png":
                case "Vehicle Tracked Small.png":
                case "Vehicle Tracked.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                    break;
                case "Vehicle Wheeled Alt.png":
                case "Vehicle Wheeled Small.png":
                case "Vehicle Wheeled.png":
                case "Vehicle_Wheeled.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled).png");
                    break;
                case "VTOL_Graphical.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/VTOL.png");
                    break;
                case "Warship.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                    break;
                case "WIGE.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, WiGE (Military).png");
                    break;
                case "Xenoplanetary.png":
                    migrated.getIconMap().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    migrated.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Infantry (Xenoplanetary).png");
                    break;
                default:
                    break;
            }
        }
    }
    //endregion Layered Force Icon

    //region Standard Force Icon
    private static StandardForceIcon migrateStandardForceIcon(final StandardForceIcon icon) {
        if (icon.hasDefaultCategory()) {
            if ("Book.png".equalsIgnoreCase(icon.getFilename())) {
                return new StandardForceIcon(StandardForceIcon.ROOT_CATEGORY,
                        StandardForceIcon.DEFAULT_FORCE_ICON_FILENAME);
            } else {
                return (icon instanceof LayeredForceIcon) ? new StandardForceIcon() : icon;
            }
        } else if (icon.getCategory().toLowerCase(Locale.ENGLISH).startsWith("pieces")) {
            LayeredForceIconLayer layer = null;
            for (final LayeredForceIconLayer iconLayer : LayeredForceIconLayer.values()) {
                if (icon.getCategory().equalsIgnoreCase(iconLayer.getLayerPath())) {
                    layer = iconLayer;
                    break;
                }
            }

            if (layer == null) {
                return new StandardForceIcon();
            }

            final LayeredForceIcon parser = new LayeredForceIcon();
            parser.getIconMap().putIfAbsent(layer, new ArrayList<>());
            final LayeredForceIcon parsed = new LayeredForceIcon();
            migrateLogos(parser, parsed);
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
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Aerospace (M).png");
                return layered;
            case "aerospacesquadron.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Company.png");
                return layered;
            case "afighter.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Fixed Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                return layered;
            case "afighterflight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Fixed Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                return layered;
            case "haerospace.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                return layered;
            case "haerospaceflight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "laerospace.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                return layered;
            case "laerospaceflight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "maerospace.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                return layered;
            case "maerospaceflight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardBattleArmor(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "battlearmor.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Battle Armor (Extended).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/MVO.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardBlueWaterNaval(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "naval_vessel_submarine.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Naval (Submarine).png");
                return layered;
            case "naval_vessel_surface.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Naval (Destroyer).png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsAlphabet(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "A.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/A.png");
                return layered;
            case "B.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/B.png");
                return layered;
            case "C.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/C.png");
                return layered;
            case "D.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/D.png");
                return layered;
            case "E.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/E.png");
                return layered;
            case "F.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/F.png");
                return layered;
            case "G.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/G.png");
                return layered;
            case "H.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/H.png");
                return layered;
            case "I.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/I.png");
                return layered;
            case "J.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/J.png");
                return layered;
            case "K.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/K.png");
                return layered;
            case "L.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/L.png");
                return layered;
            case "M.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/M.png");
                return layered;
            case "N.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/N.png");
                return layered;
            case "O.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/O.png");
                return layered;
            case "P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/P.png");
                return layered;
            case "Q.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/Q.png");
                return layered;
            case "R.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/R.png");
                return layered;
            case "S.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/S.png");
                return layered;
            case "T.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/T.png");
                return layered;
            case "U.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/U.png");
                return layered;
            case "V.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/V.png");
                return layered;
            case "W.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/W.png");
                return layered;
            case "X.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/X.png");
                return layered;
            case "Y.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/Y.png");
                return layered;
            case "Z.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/English Letters/Z.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsClan(final StandardForceIcon icon) {
        // TODO : Complete me
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "battlearmor.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Battle Armor (Extended).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/MVO.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsGreekLetters(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Alpha.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(01) ALPHA.png");
                return layered;
            case "Beta.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(02) BETA.png");
                return layered;
            case "Chi.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(22) CHI.png");
                return layered;
            case "Delta.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(04) DELTA.png");
                return layered;
            case "Epsilon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(05) EPSILON.png");
                return layered;
            case "Epsilon_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(05) Epsilon.png");
                return layered;
            case "Eta.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(07) ETA.png");
                return layered;
            case "Gamma.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(03) GAMMA.png");
                return layered;
            case "Iota.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(09) IOTA.png");
                return layered;
            case "Iota_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(09) Iota.png");
                return layered;
            case "Kappa.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(10) KAPPA.png");
                return layered;
            case "Lambda.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(11) LAMBDA.png");
                return layered;
            case "Lambda_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(11) Lambda.png");
                return layered;
            case "Mu.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(12) MU.png");
                return layered;
            case "Mu_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(12) Mu.png");
                return layered;
            case "Nu.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(13) NU.png");
                return layered;
            case "Omega.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(24) OMEGA.png");
                return layered;
            case "Omega_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(24) Omega.png");
                return layered;
            case "Omicron.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(15) OMICRON.png");
                return layered;
            case "Phi.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(21) PHI.png");
                return layered;
            case "Pi.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(16) PI.png");
                return layered;
            case "Pi_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(16) Pi.png");
                return layered;
            case "Psi.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(23) PSI.png");
                return layered;
            case "Psi_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(23) Psi.png");
                return layered;
            case "Rho.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(17) RHO.png");
                return layered;
            case "Rho_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(17) Rho.png");
                return layered;
            case "Sigma.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(18) SIGMA.png");
                return layered;
            case "Tau.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(19) TAU.png");
                return layered;
            case "Theta.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(08) THETA.png");
                return layered;
            case "Theta_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(08) Theta.png");
                return layered;
            case "Upsilon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(20) UPSILON.png");
                return layered;
            case "Xi.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(14) XI.png");
                return layered;
            case "Xi_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(14) Xi.png");
                return layered;
            case "Zeta.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Alphabet/(06) ZETA.png");
                return layered;
            case "Zeta_Icon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Greek Letters/(06) Zeta.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsIS(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "battalion.png":
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(08) Battalion.png");
                return layered;
            case "Level I.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Roman Numerals/(01) Roman I.png");
                return layered;
            case "Level II.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Roman Numerals/(02) Roman II.png");
                return layered;
            case "Level III.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Roman Numerals/(03) Roman III.png");
                return layered;
            case "Level IV.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Roman Numerals/(04) Roman IV.png");
                return layered;
            case "regiment.png":
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(10) Regiment.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsNumeric(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "1.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/01.png");
                return layered;
            case "2.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/02.png");
                return layered;
            case "3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/03.png");
                return layered;
            case "4.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/04.png");
                return layered;
            case "5.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/05.png");
                return layered;
            case "6.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/06.png");
                return layered;
            case "7.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/07.png");
                return layered;
            case "8.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/08.png");
                return layered;
            case "9.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Numbers/09.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardFormationsPhoenetic(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Alpha.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/ALFA.png");
                return layered;
            case "Bravo.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/BRAVO.png");
                return layered;
            case "Charlie.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/CHARLIE.png");
                return layered;
            case "Delta.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/DELTA.png");
                return layered;
            case "Echo.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/ECHO.png");
                return layered;
            case "Foxtrot.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/FOXTROT.png");
                return layered;
            case "Golf.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/GOLF.png");
                return layered;
            case "Hotel.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/HOTEL.png");
                return layered;
            case "India.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/INDIA.png");
                return layered;
            case "Juliet.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/JULIETT.png");
                return layered;
            case "Kilo.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/KILO.png");
                return layered;
            case "Lima.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/LIMA.png");
                return layered;
            case "Mike.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/MIKE.png");
                return layered;
            case "Novemeber.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/NOVEMBER.png");
                return layered;
            case "Oscar.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/OSCAR.png");
                return layered;
            case "Papa.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/PAPA.png");
                return layered;
            case "Quebec.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/QUEBEC.png");
                return layered;
            case "Romeo.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/ROMEO.png");
                return layered;
            case "Sierra.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/SIERRA.png");
                return layered;
            case "Tango.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/TANGO.png");
                return layered;
            case "Uniform.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/UNIFORM.png");
                return layered;
            case "Victor.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/VICTOR.png");
                return layered;
            case "Whiskey.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/WHISKEY.png");
                return layered;
            case "X-Ray.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/X-RAY.png");
                return layered;
            case "Yankee.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/YANKEE.png");
                return layered;
            case "Zulu.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/ICAO 1956/ZULU.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardInfantry(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "AirborneAssaultInfantryPlatoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Air Mobile.png");
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Paratrooper.png");
                return layered;
            case "infantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                return layered;
            case "infantrycompany.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Company.png");
                return layered;
            case "infantrylaserplatoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/LAS.png");
                return layered;
            case "infantrylrmplatoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/LRM.png");
                return layered;
            case "infantryplatoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "jinfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Jump Infantry.png");
                return layered;
            case "jinfantrylrmplatoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Jump Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/LRM.png");
                return layered;
            case "jinfantryplatoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Jump Infantry.png");
                return layered;
            case "mechanizedinfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                return layered;
            case "mhinfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                return layered;
            case "mountaininfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mountaineer.png");
                return layered;
            case "mtinfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                return layered;
            case "mvinfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                return layered;
            case "mwinfantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                return layered;
            case "Scuba Infantry Motorized.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Scuba (Bottom).png");
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Motorized.png");
                return layered;
            case "Scuba Infantry.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Scuba (Bottom).png");
                return layered;
            case "Space Marines.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Infantry.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Space.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardMech(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "amech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Assault.png");
                return layered;
            case "aomnimech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Assault.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Omni.png");
                return layered;
            case "hmech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                return layered;
            case "hmechlance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "hmechlancereinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                return layered;
            case "hmechlancereinforcedaero.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                return layered;
            case "homnimech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Heavy.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Omni.png");
                return layered;
            case "HQmech3elements.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Headquarters.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                return layered;
            case "HQmechlance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Headquarters.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "HQmechlancereinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Headquarters.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                return layered;
            case "HQmechlancereinforcedaero.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Headquarters.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                return layered;
            case "imech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Industrial.png");
                return layered;
            case "lamech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Aerospace (Right).png");
                return layered;
            case "lmech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                return layered;
            case "lmech3elements.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                return layered;
            case "lmechlance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "lmechlancereinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                return layered;
            case "lmechlancereinforcedaero.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                return layered;
            case "lomnimech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Light.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Omni.png");
                return layered;
            case "mech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Center).png");
                return layered;
            case "mechcompany.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Center).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Company.png");
                return layered;
            case "mmech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                return layered;
            case "mmechlance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "mmechlancereinforcedaero.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/English Letters/A.png");
                return layered;
            case "momnimech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medium.png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("StratOps/Omni.png");
                return layered;
            case "nova.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Bottom).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Battle Armor (Extended).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/MVO.png");
                return layered;
            case "pmech.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/BattleMech (Left).png");
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/ProtoMech.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardMilitary(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Anti Air Arty.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/AAA (Anti-Aircraft Artillery).png");
                return layered;
            case "Anti Air.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/AA (Anti-Aircraft).png");
                return layered;
            case "Battalion.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/BN (Battalion).png");
                return layered;
            case "Brigade.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/BDE (Brigade).png");
                return layered;
            case "Close Air Support.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/CAS (Close Air Support).png");
                return layered;
            case "Company.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/CO (Company).png");
                return layered;
            case "Division.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/DIV (Division).png");
                return layered;
            case "Field Arty.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/FA (Field Artillery).png");
                return layered;
            case "Fire Support Team.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/FIRE (Fire Support).png");
                return layered;
            case "Headquarters.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/HQ (Headquarters).png");
                return layered;
            case "Lance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/LANCE.png");
                return layered;
            case "Logistical Support.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/SPT (Support).png");
                return layered;
            case "Main Operating Base.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/MOB (Mobile).png");
                return layered;
            case "Platoon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/PLT (Platoon).png");
                return layered;
            case "Recon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/RECON.png");
                return layered;
            case "Regiment.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Order of Battle/RGT (Regiment).png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardMiscellaneous(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "artillery.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/Artillery.png");
                return layered;
            case "HQ.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/HQ (Headquarters).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Command and Control.png");
                return layered;
            case "lmhq.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled HQ).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Command and Control.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "maintenance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Maintenance.png");
                return layered;
            case "Medical Lance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medical.png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                return layered;
            case "mmhq.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled HQ).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Command and Control.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            case "paramedic.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Medical.png");
                return layered;
            case "recon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Recon.png");
                return layered;
            case "specialforcessquad.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/SF (Special Forces).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                return layered;
            case "supply.png":
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Supply.png");
                return layered;
            case "transport.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Transport.png");
                return layered;
            case "xenoplanetary.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/Infantry (Xenoplanetary).png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardNames(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Anvil.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/ANVIL.png");
                return layered;
            case "Assault.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Weight/ASSAULT.png");
                return layered;
            case "Battle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/BATTLE.png");
                return layered;
            case "Cavarly.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/CAV (Cavalry).png");
                return layered;
            case "Command.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/CMD (Command).png");
                return layered;
            case "Fire.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/FIRE (Fire Support).png");
                return layered;
            case "Gun.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/GUN.png");
                return layered;
            case "Hammer.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/HAMMER.png");
                return layered;
            case "Heavy.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Weight/HEAVY.png");
                return layered;
            case "Jump.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/JUMP.png");
                return layered;
            case "Light.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Weight/LIGHT.png");
                return layered;
            case "Medium.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Weight/MEDIUM.png");
                return layered;
            case "Pursuit.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/PURSUIT.png");
                return layered;
            case "Recon.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/RECON.png");
                return layered;
            case "Scout.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Type/SCOUT.png");
                return layered;
            case "Strike.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/STRIKE.png");
                return layered;
            case "Striker.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/STRIKER.png");
                return layered;
            case "Sweep.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Alphanumeric/Lance Names/SWEEP.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardNaval(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "adropship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/DropShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                return layered;
            case "battlecruiser.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CB.png");
                return layered;
            case "battleship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/BB.png");
                return layered;
            case "bdropship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/DropShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/B.png");
                return layered;
            case "carrier.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CV.png");
                return layered;
            case "cdropship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/DropShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/C.png");
                return layered;
            case "corvette.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/PC.png");
                return layered;
            case "cruiser.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CA.png");
                return layered;
            case "cvdropship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/DropShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CV.png");
                return layered;
            case "destroyer.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/DD.png");
                return layered;
            case "frigate.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/FF.png");
                return layered;
            case "heavycruiser.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/CH.png");
                return layered;
            case "iiijumpship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/JumpShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(03) Roman III.png");
                return layered;
            case "iijumpship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/JumpShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(02) Roman II.png");
                return layered;
            case "ijumpship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/JumpShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(01) Roman I.png");
                return layered;
            case "ivjumpship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/JumpShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/Roman Numerals/(04) Roman IV.png");
                return layered;
            case "jumpship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/JumpShip.png");
                return layered;
            case "qdropship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/DropShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/Q.png");
                return layered;
            case "survey.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/PS.png");
                return layered;
            case "transportcruiser.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("StratOps/WarShip.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/CT.png");
                return layered;
            case "warship.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/WarShip.png");
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
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Capellan Confederation.png");
                return layered;
            case "CircinusFederation.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Circinus Federation.png");
                return layered;
            case "ClanCloudCobra.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Cloud Cobra.png");
                return layered;
            case "ClanCoyote.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Coyote.png");
                return layered;
            case "ClanDiamondShark.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Diamond Shark.png");
                return layered;
            case "ClanGhostBear.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Ghost Bear.png");
                return layered;
            case "ClanSteelViper.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Steel Viper.png");
                return layered;
            case "ClanWolverine.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Clan/Clan Wolverine.png");
                return layered;
            case "Comstar.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/ComStar.png");
                return layered;
            case "DraconisCombine.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Draconis Combine.png");
                return layered;
            case "FederatedCommonwealth.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Federated Commonwealth.png");
                return layered;
            case "FederatedSuns.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Federated Suns.png");
                return layered;
            case "foxsteeth.png":
                return new StandardForceIcon(icon.getCategory(), "Fox's Teeth.png");
            case "FreeRasalhagueRepublic.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Free Rasalhague Republic.png");
                return layered;
            case "FreeWorldsLeague.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Free Worlds League.png");
                return layered;
            case "JadeFalconDelta.gif":
                return new StandardForceIcon(icon.getCategory(), "Jade Falcon Delta Galaxy.gif");
            case "LyranAlliance.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Lyran Alliance.png");
                return layered;
            case "MagistryOfCanopus.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Magistracy of Canopus.png");
                return layered;
            case "MarianHegemony.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Marian Hegemony.png");
                return layered;
            case "Opacus_Venatori.jpg":
                return new StandardForceIcon(icon.getCategory(), "Opacus Venatori.jpg");
            case "OutworldsAlliance.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Outworlds Alliance.png");
                return layered;
            case "RepublicOfTheSphere.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Republic of the Sphere.png");
                return layered;
            case "SorensonSabres.jpg":
                return new StandardForceIcon(icon.getCategory(), "Sorenson's Sabres.jpg");
            case "StarLeague.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Star League.png");
                return layered;
            case "TaurianConcordat.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Periphery/Taurian Concordat.png");
                return layered;
            case "WordOfBlake.png":
                layered.getIconMap().put(LayeredForceIconLayer.LOGO, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.LOGO).add("Inner Sphere/Word of Blake.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardVehicles(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "atvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                return layered;
            case "awvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                return layered;
            case "htrackedapc.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                return layered;
            case "htvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                return layered;
            case "htvehiclecompany.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                return layered;
            case "htvehiclelance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                return layered;
            case "hwvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                return layered;
            case "hwvehiclelance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/H.png");
                return layered;
            case "lhoverapc.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "lhvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "lhvehiclelance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "ltrackedapc.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "ltvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "lwheeledapc.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "lwvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/L.png");
                return layered;
            case "mhoverapc.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            case "mhvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            case "mhvehiclelance.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Hover Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            case "mtrackedapc.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ADJUSTMENT, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ADJUSTMENT).add("NATO/Mechanized.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            case "mtvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            case "mwvehicle.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Vehicle (Wheeled Tracked).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/M.png");
                return layered;
            default:
                return icon;
        }
    }

    private static StandardForceIcon migrateStandardVTOL(final StandardForceIcon icon) {
        final LayeredForceIcon layered = new LayeredForceIcon();
        switch (icon.getFilename()) {
            case "Basic Attack Vtol C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Attack Vtol C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Attack Vtol C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Attack Vtol C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "Basic Attack Vtol EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Attack Vtol EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Attack Vtol P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Attack Vtol.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "Basic Cargo Vtol C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Cargo Vtol C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Cargo Vtol C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Cargo Vtol C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "Basic Cargo Vtol EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Cargo Vtol EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Cargo Vtol P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Cargo Vtol.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "Basic Recon Vtol C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Recon Vtol C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Recon Vtol C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Recon Vtol C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "Basic Recon Vtol EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Recon Vtol EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Recon Vtol P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Recon Vtol.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "Basic Vtol C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Vtol C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Vtol C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Vtol C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                return layered;
            case "Basic Vtol EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Vtol EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "Basic Vtol P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "Basic Vtol.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                return layered;
            case "rvtol.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Civilian).png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/R.png");
                return layered;
            case "rvtolattackcargoflight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Civilian).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom Right/English Letters/C.png");
                return layered;
            case "rvtolattackcargoflightreinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Civilian).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom Right/English Letters/C.png");
                return layered;
            case "VTOL Attack Element C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Element C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Element EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Element Reinforced C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element Reinforced C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element Reinforced C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Element Reinforced C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Element Reinforced EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element Reinforced EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Element Reinforced P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Element Reinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Element.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Flight C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Flight C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Flight EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Flight Reinforced C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight Reinforced C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight Reinforced C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Flight Reinforced C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Flight Reinforced EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight Reinforced EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Flight Reinforced P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Flight Reinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Flight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Single C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Single C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Single C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Single C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Single EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Single EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Single P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Single.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Squadron C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Squadron C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Squadron C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Squadron C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Squadron EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Squadron EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Squadron P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Squadron.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Wing C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Wing C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Wing C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Wing C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Attack Wing EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Wing EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Attack Wing P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Attack Wing.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/A.png");
                return layered;
            case "VTOL Cargo Element C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Element C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Element EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Element EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Element Reinforced C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element Reinforced C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element Reinforced C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Element Reinforced C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Element Reinforced EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element Reinforced EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Element Reinforced P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Element Reinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Element.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Flight C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Flight C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Flight EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Flight Reinforced C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight Reinforced C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight Reinforced C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Flight Reinforced C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Flight Reinforced EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight Reinforced EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Flight Reinforced P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Flight Reinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Flight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Single C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Single C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Single C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Single C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Single EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Single EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Single P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Single.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Squadron C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Squadron C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Squadron C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Squadron C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Squadron EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Squadron EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Squadron P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Squadron.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Wing C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Wing C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Wing C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Wing C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Cargo Wing EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Wing EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Cargo Wing P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Cargo Wing.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/C.png");
                return layered;
            case "VTOL Recon Attack Flight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Civilian).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/A.png");
                return layered;
            case "VTOL Recon Element C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Element C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Element EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Element Reinforced C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element Reinforced C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element Reinforced C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Element Reinforced C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Element Reinforced EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element Reinforced EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Element Reinforced P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Element Reinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(04) Augmented Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Element.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(03) Team.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Flight C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Flight C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Flight EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Flight Reinforced C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight Reinforced C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight Reinforced C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Flight Reinforced C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Flight Reinforced EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight Reinforced EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Flight Reinforced P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Flight Reinforced.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(06) Augmented Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Flight.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(05) Lance.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Single C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Single C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Single C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Single C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Single EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Single EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Single P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Single.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(02) Individual.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Squadron C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Squadron C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Squadron C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Squadron C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Squadron EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Squadron EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Squadron P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Squadron.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(07) Company.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Wing C3 EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Wing C3 EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Wing C3 P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Wing C3.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Left/C3.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "VTOL Recon Wing EW P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Wing EW.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Bottom/EW.png");
                return layered;
            case "VTOL Recon Wing P.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top Right/English Letters/P.png");
                return layered;
            case "VTOL Recon Wing.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("NATO/Aviation, Rotary Wing (Military).png");
                layered.getIconMap().put(LayeredForceIconLayer.FORMATION, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.FORMATION).add("Inner Sphere/(09) Battalion.png");
                layered.getIconMap().put(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.ALPHANUMERIC).add("Top/R.png");
                return layered;
            case "vtol.png":
                layered.getIconMap().put(LayeredForceIconLayer.TYPE, new ArrayList<>());
                layered.getIconMap().get(LayeredForceIconLayer.TYPE).add("Graphical/VTOL.png");
                return layered;
            default:
                return icon;
        }
    }
    //endregion Standard Force Icon

    //region Legacy Save Format
    public static void migrateLegacyIconMapNodes(final LayeredForceIcon icon, final Node wn) {
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            if ((wn2.getNodeType() != Node.ELEMENT_NODE) || !wn2.hasChildNodes()) {
                continue;
            }
            final String oldKey = wn2.getAttributes().getNamedItem("key").getTextContent();
            LayeredForceIconLayer key = null;
            if ("Pieces/Type/".equalsIgnoreCase(oldKey)) {
                key = LayeredForceIconLayer.TYPE;
            } else {
                for (final LayeredForceIconLayer layer : LayeredForceIconLayer.values()) {
                    if (layer.getLayerPath().equalsIgnoreCase(oldKey)) {
                        key = layer;
                        break;
                    }
                }
            }

            if (key == null) {
                continue;
            }
            final List<String> values = processIconMapSubNodes(wn2.getChildNodes());
            icon.getIconMap().put(key, values);
        }
    }

    private static List<String> processIconMapSubNodes(final NodeList nl) {
        final List<String> values = new ArrayList<>();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String value = wn2.getAttributes().getNamedItem("name").getTextContent();
            if ((value != null) && !value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }
    //endregion Legacy Save Format
}

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

import megamek.common.icons.Camouflage;

/**
 * This migrates Camouflage from SeaBee's Pack to Deadborder's Pack.
 * This migration occurred in 0.49.3.
 */
public class CamouflageMigrator {
    public static void migrateCamouflage(final Camouflage camouflage) {
        camouflage.setCategory(migrateCategory(camouflage.getCategory()));
        camouflage.setFilename(migrateFilename(camouflage.getCategory(), camouflage.getFilename()));
        finalizeMigration(camouflage);
    }

    private static String migrateCategory(String text) {
        if (text.startsWith("Periphery/")) {
            text = text.replaceFirst("Periphery/", "");
        } else if (text.startsWith("Corporations/")) {
            text = text.replaceFirst("Corporations", "Misc");
        }

        if (text.startsWith("Clans/Hells Horses/")) {
            text = text.replaceFirst("Hells Horses", "Hell's Horses");
        } else if (text.startsWith("Clans/Wolf in Exile/")) {
            text = text.replaceFirst("Wolf in Exile", "Wolf-in-Exile");
        } else if (text.startsWith("Comstar/")) {
            text = text.replaceFirst("Comstar", "ComStar");
        } else if (text.startsWith("Kurita/")) {
            text = text.replaceFirst("Kurita", "Draconis Combine");
        } else if (text.startsWith("Davion/")) {
            text = text.replaceFirst("Davion", "Federated Suns");
        } else if (text.startsWith("Liao/")) {
            text = text.replaceFirst("Liao", "Capellan Confederation");
        } else if (text.startsWith("Marik/")) {
            text = text.replaceFirst("Marik", "Free Worlds League");
        } else if (text.startsWith("Mercs/Wolfs Dragoons/")) {
            text = text.replaceFirst("Wolfs Dragoons", "Wolf's Dragoons");
        } else if (text.startsWith("Mercs/Galatean Defence Force/")) {
            text = text.replaceFirst("Galatean Defence Force", "Galatean Defense Force");
        } else if (text.startsWith("Mercs/Hamptons Hessens/")) {
            text = text.replaceFirst("Hamptons Hessens", "Hampton's Hessens");
        } else if (text.startsWith("Mercs/Holts Hilltoppers/")) {
            text = text.replaceFirst("Holts Hilltoppers", "Holt's Hilltoppers");
        } else if (text.startsWith("Mercs/Medusans/")) {
            text = text.replaceFirst("Medusans", "The Medusans");
        } else if (text.startsWith("Outworlds Alliance/")) {
            text = "Outworlds Alliance/";
        } else if (text.startsWith("Taurian Concordat/")) {
            text = "Taurian Concordat/";
        } else if (text.startsWith("Nueva Castile/")) {
            text = "Minor Periphery/";
        } else if (text.startsWith("Umayyad Caliphate/")) {
            text = "Minor Periphery/";
        } else if (text.startsWith("Rasalhague/")) {
            text = text.replaceFirst("Rasalhague", "Free Rasalhague Republic");
        } else if (text.startsWith("Star League/")) {
            text = "Star League Defense Force/";
        } else if (text.startsWith("Steiner/")) {
            text = text.replaceFirst("Steiner", "Lyran Commonwealth");
        } else if (text.startsWith("Republic of the Sphere/Hastatis Sentinels/")) {
            text = "Republic of the Sphere/";
        } else if (text.startsWith("Republic of the Sphere/Principes Guard/")) {
            text = "Republic of the Sphere/";
        } else if (text.startsWith("Republic of the Sphere/Triarii Protectors/")) {
            text = "Republic of the Sphere/";
        } else if (text.startsWith("Rim Worlds Army/")) {
            text = "Rim Worlds Republic/";
        } else if (text.startsWith("Bloodwolf/Gradient/")) {
            text = text.replaceFirst("Bloodwolf", "Faction Camouflage");
        } else if (text.startsWith("Bloodwolf/Difference Clouds/")) {
            text = text.replaceFirst("Bloodwolf", "Faction Camouflage");
        } else if (text.startsWith("Standard Camouflage/Jelley Bean/")) {
            text = text.replaceFirst("Jelley Bean", "Jelly Bean");
        }

        if (text.startsWith("Free Worlds League/Free World Guard/")) {
            text = text.replaceFirst("Free World Guard", "Free World Guards");
        } else if (text.startsWith("Draconis Combine/An Ting Legion/")) {
            text = text.replaceFirst("An Ting Legion", "An Ting Legions");
        } else if (text.startsWith("Draconis Combine/Arkab Legion/")) {
            text = text.replaceFirst("Arkab Legion", "Arkab Legions");
        } else if (text.startsWith("Draconis Combine/Independent/")) {
            text = text.replaceFirst("Independent", "Independent Regiments");
        } else if (text.startsWith("Draconis Combine/Legion of Vega/")) {
            text = text.replaceFirst("Legion of Vega", "Legions of Vega");
        } else if (text.startsWith("Draconis Combine/Prosperina Hussars/")) {
            text = text.replaceFirst("Prosperina Hussars", "Proserpina Hussars");
        } else if (text.startsWith("Federated Suns/March Militias/")) {
            text = "Federated Suns/March Militias/";
        } else if (text.startsWith("Capellan Confederation/Citizens Honored/")) {
            text = text.replaceFirst("Citizens Honored", "Citizen's Honored");
        } else if (text.startsWith("Capellan Confederation/Liao Chang-Cheng (Liao Reserves)/")) {
            text = text.replaceFirst("Liao Chang-Cheng (Liao Reserves)", "Liao Cháng-Chéng");
        } else if (text.startsWith("Capellan Confederation/Victoria Commonality Regulars/")) {
            text = text.replaceFirst("Victoria Commonality Regulars", "Victoria Rangers");
        } else if (text.startsWith("Capellan Confederation/McCarrons Armored Cavalry/")) {
            text = text.replaceFirst("McCarrons Armored Cavalry", "McCarron's Armored Cavalry");
        } else if (text.startsWith("Capellan Confederation/Reserve Cavalry/")) {
            text = text.replaceFirst("Reserve Cavalry", "Capellan Reserve Cavalry");
        } else if (text.startsWith("Magistracy of Canopus/Chasseurs a Cheval/")) {
            text = text.replaceFirst("Chasseurs a Cheval", "Chasseurs á Cheval");
        } else if (text.startsWith("Lyran Commonwealth/Bolan Guard/")) {
            text = text.replaceFirst("Bolan Guard", "Commonwealth Guards");
        } else if (text.startsWith("Lyran Commonwealth/Buena Guard/")) {
            text = text.replaceFirst("Buena Guard", "Commonwealth Guards");
        } else if (text.startsWith("Lyran Commonwealth/Tikonov Republican/")) {
            text = text.replaceFirst("Tikonov Republican", "Republican Guards");
        }

        return text;
    }

    private static String migrateFilename(final String category, final String text) {
        switch (category) {
            case "Clans/Fire Mandrill/":
                return migrateClanFireMandrill(text);
            case "Clans/Ghost Bear/":
                return text.equalsIgnoreCase("Rasalhaugue Galaxy.jpg") ? "Rasalhague Galaxy.jpg" : text;
            case "Clans/Smoke Jaguar/":
                return text.equalsIgnoreCase("Jaguars Den Galaxy.jpg") ? "Jaguar's Den Galaxy.jpg" : text;
            case "ComStar/":
                return migrateComStar(text);
            case "Federated Suns/":
            case "Draconis Combine/":
            case "Word of Blake/":
                return text.equalsIgnoreCase("FleetAssets.jpg") ? "Fleet Assets.jpg" : text;
            case "Federated Suns/Arcadian Cuirassiers/":
                return text.equalsIgnoreCase("Arcadian Cuirassers.jpg") ? "Arcadian Cuirassiers.jpg" : text;
            case "Federated Suns/Chisholm's Raiders/":
                return migrateChisholmsRaiders(text);
            case "Federated Suns/Crucis Lancers/":
                return migrateCrucisLancers(text);
            case "Federated Suns/FedCom RCT/":
                return text.equalsIgnoreCase("3rd FedComRCT.jpg") ? "3rd FedCom RCT.jpg" : text;
            case "Federated Suns/Robinson Strikers/":
                return text.equalsIgnoreCase("5th Robinsons Strikers.jpg") ? "5th Robinson Strikers.jpg" : text;
            case "Federated Suns/Syrtis Fusiliers/":
                return migrateSyrtisFusiliers(text);
            case "Draconis Combine/Ghost Regiments/":
                return migrateGhostRegiments(text);
            case "Draconis Combine/Ryuken/":
                return migrateRyuken(text);
            case "Capellan Confederation/Capellan Brigade/":
                return migrateCapellanBrigade(text);
            case "Capellan Confederation/Citizen's Honored/":
                return migrateCitizensHonored(text);
            case "Capellan Confederation/Free Capella/":
                return text.equalsIgnoreCase("Borodins Vindicators.jpg") ? "Borodin's Vindicators.jpg" : text;
            case "Capellan Confederation/Liao Cháng-Chéng/":
                return migrateLiaoChangCheng(text);
            case "Capellan Confederation/McCarron's Armored Cavalry/":
                return migrateMcCarronsArmoredCavalry(text);
            case "Capellan Confederation/St. Ives/":
                return migrateStIves(text);
            case "Capellan Confederation/Victoria Rangers/":
                return migrateVictoriaRangers(text);
            case "Capellan Confederation/Warrior Houses/":
                return migrateWarriorHouses(text);
            case "Free Worlds League/Fusiliers of Oriente/":
                return migrateFusiliersOfOriente(text);
            case "Mercs/DropShip Irregulars/":
                return migrateDropShipIrregulars(text);
            case "Mercs/Galatean Defense Force/":
                return migrateGalateanDefenseForce(text);
            case "Mercs/Hampton's Hessens/":
                return migrateHamptonsHessens(text);
            case "Mercs/Holt's Hilltoppers/":
                return migrateHoltsHilltoppers(text);
            case "Mercs/The Medusans/":
                return migrateTheMedusans(text);
            case "Mercs/Northwind Highlanders/":
                return text.equalsIgnoreCase("First Kearny.jpg") ? "First Kearny Highlanders.jpg" : text;
            case "Mercs/":
                return migrateMercs(text);
            case "Magistracy of Canopus/Magestrix Royal Guard/":
                return migrateMagistracyOfCanopus(text);
            case "Marian Hegemony/":
                return migrateMarianHegemony(text);
            case "Minor Periphery/":
                return text.equalsIgnoreCase("1st Umayyad Corps.jpg") ? "Umayyad 1st Corps.jpg" : text;
            case "Pirates/":
                return migratePirates(text);
            case "Republic of the Sphere/":
                return migrateRepublicOfTheSphere(text);
            case "Rim Worlds Republic/":
                return text.equalsIgnoreCase("21st Rim Worlders.jpg") ? "21st Rim Worlds.jpg" : text;
            case "Solaris Stables/":
                return migrateSolarisStables(text);
            case "Star League Defence Force/":
                return migrateStarLeagueDefenceForce(text);
            case "Lyran Commonwealth/Arcturan Guards/":
                return text.equalsIgnoreCase("20th Arcturan Guard.jpg") ? "20th Arcturan Guards.jpg" : text;
            case "Lyran Commonwealth/Commonwealth Guards/":
                return migrateCommonwealthGuards(text);
            case "Faction Camouflage/Difference Clouds/":
                return migrateDifferenceClouds(text);
            case "Faction Camouflage/Gradients/":
                return migrateGradients(text);
            case "Standard Camouflage/Jelly Bean/":
                return migrateJellyBean(text);
            default:
                return text;
        }
    }

    private static String migrateClanFireMandrill(final String text) {
        switch (text) {
            case "Kindraa Faraday-Tanga.jpg":
                return "Kindraa Faraday-Tanaga.jpg";
            case "Kindraa Mattila-Carol.jpg":
                return "Kindraa Mattila-Carrol.jpg";
            case "Matilla-Carrol Fleet Assets.jpg":
                return "Mattila-Carrol Fleet Assets.jpg";
            default:
                return text;
        }
    }

    private static String migrateComStar(final String text) {
        switch (text) {
            case "7th V-Iota Army.jpg":
                return "7th Army V-Iota.jpg";
            case "8th V-Pi Army.jpg":
                return "8th Army V-Pi.jpg";
            case "9th V-Lambda Army.jpg":
                return "9th Army V-Lambda.jpg";
            case "10th V-nu Army.jpg":
                return "10th Army V-Nu.jpg";
            case "11th V-eta Army.jpg":
                return "11th Army V-Eta.jpg";
            case "12th V-Beta Army.jpg":
                return "12th Army V-Beta.jpg";
            default:
                return text;
        }
    }

    private static String migrateChisholmsRaiders(final String text) {
        switch (text) {
            case "1st Chisholms Raiders.jpg":
                return "1st Chisholm's Raiders.jpg";
            case "2nd Chisholms Raiders.jpg":
                return "2nd Chisholm's Raiders.jpg";
            default:
                return text;
        }
    }

    private static String migrateCrucisLancers(final String text) {
        switch (text) {
            case "1st Crucis Lancers RCT.jpg":
                return "1st Crucis Lancers.jpg";
            case "2nd Crucis Lancers RCT.jpg":
                return "2nd Crucis Lancers.jpg";
            case "3rd Crucis Lancers RCT.jpg":
                return "3rd Crucis Lancers.jpg";
            case "4th Crucis Lancers RCT.jpg":
                return "4th Crucis Lancers.jpg";
            case "5th Crucis Lancers RCT.jpg":
                return "5th Crucis Lancers.jpg";
            case "6th Crucis Lancers RCT.jpg":
                return "6th Crucis Lancers.jpg";
            case "7th Crucis Lancers RCT.jpg":
                return "7th Crucis Lancers.jpg";
            case "8th Crucis Lancers RCT.jpg":
                return "8th Crucis Lancers.jpg";
            default:
                return text;
        }
    }

    private static String migrateSyrtisFusiliers(final String text) {
        switch (text) {
            case "2nd Syrtis Fusiliers RCT.jpg":
                return "2nd Syrtis Fusiliers.jpg";
            case "8th Syrtis Fusiliers RCT.jpg":
                return "8th Syrtis Fusiliers.jpg";
            default:
                return text;
        }
    }

    private static String migrateGhostRegiments(final String text) {
        switch (text) {
            case "1st Ghost Regiment.jpg":
                return "1st Ghost.jpg";
            case "3rd Ghost Regiment.jpg":
                return "3rd Ghost";
            case "5th Ghost Regiment.jpg":
                return "5th Ghost";
            case "6th Ghost Regiment.jpg":
                return "6th Ghost";
            case "7th Ghost Regiment.jpg":
                return "7th Ghost";
            case "10th Ghost Regiment.jpg":
                return "10th Ghost";
            case "11th Ghost Regiment.jpg":
                return "11th Ghost";
            case "12th Ghost Regiment.jpg":
                return "12th Ghost";
            default:
                return text;
        }
    }

    private static String migrateRyuken(final String text) {
        switch (text) {
            case "Ryuken-Go.jpg":
                return "Ryuken-go.jpg";
            case "Ryuken-Hachi.jpg":
                return "Ryuken-hachi.jpg";
            case "Ryuken-Ni.jpg":
                return "Ryuken-ni.jpg";
            case "Ryuken-Roku.jpg":
                return "Ryuken-roku.jpg";
            case "Ryuken-San.jpg":
                return "Ryuken-san.jpg";
            case "Ryuken-Yon.jpg":
                return "Ryuken-yon.jpg";
            default:
                return text;
        }
    }

    private static String migrateCapellanBrigade(final String text) {
        switch (text) {
            case "Ambermarles Highlanders.jpg":
                return "Ambermarle's Highlanders.jpg";
            case "Marshigamas Legionnaires.jpg":
                return "Marshigama's Legionnaires.jpg";
            case "St Cyrs Armoured Hussars.jpg":
                return "St. Cyr's Armored Hussars.jpg";
            default:
                return text;
        }
    }

    private static String migrateCitizensHonored(final String text) {
        switch (text) {
            case "15 Dracon.jpg":
                return "15th Dracon.jpg";
            case "Laurels Legion.jpg":
                return "Laurel's Legion.jpg";
            default:
                return text;
        }
    }

    private static String migrateLiaoChangCheng(final String text) {
        switch (text) {
            case "Renshields Dragoons.jpg":
                return "Renshield's Dragoons.jpg";
            case "Syns Hussars.jpg":
                return "Syn's Hussars.jpg";
            case "Vongs Grenadiers.jpg":
                return "Vong's Grenadiers.jpg";
            default:
                return text;
        }
    }

    private static String migrateMcCarronsArmoredCavalry(final String text) {
        switch (text) {
            case "1st McCarrons Armored Cavalry.jpg":
                return "1st McCarron's Armored Cavalry.jpg";
            case "2nd McCarrons Armored Cavalry.jpg":
                return "2nd McCarron's Armored Cavalry.jpg";
            case "3rd McCarrons Armored Cavalry.jpg":
                return "3rd McCarron's Armored Cavalry.jpg";
            case "4th McCarrons Armored Cavalry.jpg":
                return "4th McCarron's Armored Cavalry.jpg";
            case "5th McCarrons Armored Cavalry.jpg":
                return "5th McCarron's Armored Cavalry.jpg";
            default:
                return text;
        }
    }

    private static String migrateStIves(final String text) {
        switch (text) {
            case "Marcellas Armoured Infantry.jpg":
                return "Marcella's Armored Infantry.jpg";
            case "Romans Mounted Fusiliers.jpg":
                return "Roman's Mounted Fusiliers.jpg";
            default:
                return text;
        }
    }

    private static String migrateVictoriaRangers(final String text) {
        switch (text) {
            case "Kingstons Rangers.jpg":
                return "Kingston's Rangers.jpg";
            case "Sungs Rangers.jpg":
                return "Sung's Rangers.jpg";
            default:
                return text;
        }
    }

    private static String migrateWarriorHouses(final String text) {
        switch (text) {
            case "House LuSann.jpg":
                return "House Lu Sann.jpg";
            case "House Matsukai.jpg":
                return "House Ma-Tsu Kai.jpg";
            case "House White Tigers.jpg":
                return "House White Tiger.jpg";
            default:
                return text;
        }
    }

    private static String migrateFusiliersOfOriente(final String text) {
        switch (text) {
            case "5th Oriente Fusilier.jpg":
                return "5th Oriente Fusiliers.jpg";
            case "Oriente Fusilier Ducal Guard.jpg":
                return "Ducal Guard.jpg";
            default:
                return text;
        }
    }

    private static String migrateDropShipIrregulars(final String text) {
        switch (text) {
            case "1st Dropship Irregulars.jpg":
                return "DropShip Irregulars (1st Battalion).jpg";
            case "2nd Dropship Irregulars.jpg":
                return "DropShip Irregulars (2nd Battalion).jpg";
            default:
                return text;
        }
    }

    private static String migrateGalateanDefenseForce(final String text) {
        switch (text) {
            case "1st Galatean Defence Force.jpg":
                return "1st Galatean Defense Force.jpg";
            case "2nd Galatean Defence Force.jpg":
                return "2nd Galatean Defense Force.jpg";
            default:
                return text;
        }
    }

    private static String migrateHamptonsHessens(final String text) {
        switch (text) {
            case "1st Hampton Irregulars.jpg":
                return "1st New Hessen Irregulars.jpg";
            case "2nd Hampton Irregulars.jpg":
                return "2nd New Hessen Irregulars.jpg";
            case "Hamptons Hessens.jpg":
                return "New Hessen Armored Scouts.jpg";
            default:
                return text;
        }
    }

    private static String migrateHoltsHilltoppers(final String text) {
        switch (text) {
            case "Holts Hilltoppers 1st company.jpg":
                return "Holt's Hilltoppers (1st Company).jpg";
            case "Holts Hilltoppers 2nd company.jpg":
                return "Holt's Hilltoppers (2nd Company).jpg";
            case "Holts Hilltoppers.jpg":
                return "Holt's Hilltoppers.jpg";
            default:
                return text;
        }
    }

    private static String migrateTheMedusans(final String text) {
        switch (text) {
            case "1st Medusans.jpg":
                return "Command Squadron.jpg";
            case "2nd Medusans.jpg":
                    return "Hydra Assault Squadron.jpg";
            case "3rd Medusans.jpg":
                return "Basilisk Assault Squadron.jpg";
            default:
                return text;
        }
    }

    private static String migrateMercs(final String text) {
        switch (text) {
            case "Ables Aces.jpg":
                return "Able's Aces.jpg";
            case "Aces Darwin Whipits.jpg":
                return "Ace Darwin's Whip-Its (Ace).jpg";
            case "Avantis Angels.jpg":
                return "Avanti's Angels.jpg";
            case "Bad Dreams.jpg":
                return "Bad Dream.jpg";
            case "Barretts Fusiliers.jpg":
                return "Barrett's Fusiliers.jpg";
            case "Battlemagic.jpg":
                return "Battle Magic.jpg";
            case "BlackHearts.jpg":
                return "Blackheart.jpg";
            case "Brions Legion.jpg":
                return "Brion's Legion.jpg";
            case "Brocks Buccaneers.jpg":
                return "Brock's Buccaneers.jpg";
            case "Bronsons Horde.jpg":
                return "Bronson's Horde.jpg";
            case "Bullards Armored Cavalry.jpg":
                return "Bullard's Armored Cavalry.jpg";
            case "Burrs Black Cobras.jpg":
                return "Burr's Black Cobras.jpg";
            case "Burtons Brigade.jpg":
                return "Burton's Brigade.jpg";
            case "Caesars Cohorts.jpg":
                return "Caesar's Cohorts.jpg";
            case "Camachos Caballeros.jpg":
                return "Camacho's Caballeros.jpg";
            case "Carsons Renegades.jpg":
                return "Carson's Renegades.jpg";
            case "Cliftons Rangers.jpg":
                return "Clifton's Rangers.jpg";
            case "Cunningham's commando's.jpg":
                return "Cunningham's Commandos.jpg";
            case "Dantes Detective.jpg":
                return "Dante's Detective.jpg";
            case "Deliahs Gauntlet.jpg":
                return "Deliah's Gauntlet.jpg";
            case "Devils Advocates.jpg":
                return "Devil's Advocates.jpg";
            case "Devils Brigade.jpg":
                return "Devil's Brigade.jpg";
            case "Dragon Slayers.jpg":
                return "Dragonslayers.jpg";
            case "Dragons Breath.jpg":
                return "Dragon's Breath.jpg";
            case "Dredericksons Devils.jpg":
                return "Dedrickson's Devils.jpg";
            case "Erikssons Einherjar.jpg":
                return "Eriksson's Einherjar.jpg";
            case "Gabharts Carabineers.jpg":
                return "Gabhardt's Carabineers.jpg";
            case "Gaels Grinders.jpg":
                return "Gael's Grinders.jpg";
            case "Gale force.jpg":
                return "Gale Force.jpg";
            case "Gannons Cannons.jpg":
                return "Gannon's Cannons.jpg";
            case "Gordons Armoured Cavalry.jpg":
                return "Gordon's Armored Cavalry.jpg";
            case "Grandin's crusaders.jpg":
                return "Grandin's Crusaders.jpg";
            case "Gravewalkers.jpg":
                return "Grave Walkers.jpg";
            case "Grays Ghosts.jpg":
                return "Gray's Ghosts.jpg";
            case "Greenburgs Godzillas.jpg":
                return "Greenburg's Godzillas.jpg";
            case "Greggs Long Striders.jpg":
                return "Gregg's Long Striders.jpg";
            case "Griffin's pride.jpg":
                return "Griffin's Pride.jpg";
            case "Hannibals Hermits.jpg":
                return "Hannibal's Hermits.jpg";
            case "Hansens Roughriders.jpg":
                return "Hansen's Roughriders.jpg";
            case "Harcourts Destructors.jpg":
                return "Harcourt's Destructors.jpg";
            case "Harlocks Warriors.jpg":
                return "Harlock's Warriors.jpg";
            case "Heavy Hell Raisers.jpg":
                return "HeavyHell Raisers.jpg";
            case "Hells Black Aces.jpg":
                return "Hell's Black Aces.jpg";
            case "IllicianLancers.jpg":
                return "Illician Lancers.jpg";
            case "Jacobs Juggernauts.jpg":
                return "Jacob's Juggernauts.jpg";
            case "Khasparovs Knights.jpg":
                return "Khasparov's Knights.jpg";
            case "Khorsakovs Cossacks.jpg":
                return "Khorsakov's Cossacks.jpg";
            case "Kirkpatricks Invaders.jpg":
                return "Kirkpatrick's Invaders.jpg";
            case "Knights of St Cameron.jpg":
                return "Knights of St. Cameron.jpg";
            case "Lindons Battalion.jpg":
                return "Lindon's Battalion.jpg";
            case "Little Richards Panzer Brigade.jpg":
                return "Little Richard's Panzer Brigade.jpg";
            case "Longwoods Bluecoats.jpg":
                return "Longwood's Bluecoats.jpg";
            case "Marksons Marauders.jpg":
                return "Markson's Marauders.jpg";
            case "McFaddens Skyriders.jpg":
                return "McFadden's Skyriders.jpg";
            case "Micks Blue Sky Rangers.jpg":
                return "Mick's Blue Sky Rangers.jpg";
            case "Narhals Raiders.jpg":
                return "Narhal's Raiders.jpg";
            case "Nelsons Longbows.jpg":
                return "Nelson's Longbows.jpg";
            case "Olsons Rangers.jpg":
                return "Olson's Rangers.jpg";
            case "Pandoras Box.jpg":
                return "Pandora's Box.jpg";
            case "Preys Divisionals.jpg":
                return "Prey's Divisionals.jpg";
            case "Quints Olympian Ground Pounders.jpg":
                return "Quint's Olympian Ground Pounders.jpg";
            case "Ramilies Raiders.jpg":
                return "Ramilie's Raiders.jpg";
            case "Reeds Brew.jpg":
                return "Reed's Brew.jpg";
            case "Romanovs Crusaders.jpg":
                return "Romanov's Crusaders.jpg";
            case "Rubinsky's light horse.jpg":
                return "Rubinsky's Light Horse.jpg";
            case "Simonsons Cutthroats.jpg":
                return "Simonson's Cutthroats.jpg";
            case "Smithsons Chinese Bandits.jpg":
                return "Smithson's Chinese Bandits.jpg";
            case "Storms Metal Thunder.jpg":
                return "Storm's Metal Thunder.jpg";
            case "Summers Storm.jpg":
                return "Summer's Storm.jpg";
            case "Swanns Cavaliers.jpg":
                return "Swann's Cavaliers.jpg";
            case "The fourty-eighth.jpg":
                return "The 48th.jpg";
            case "Thors Hammers.jpg":
                return "Thor's Hammers.jpg";
            case "TigerSharks.jpg":
                return "Tiger Sharks.jpg";
            case "Vandelays Valkyries.jpg":
                return "Vandelay's Valkyries.jpg";
            case "Vinsons Vigilantes.jpg":
                return "Vinson's Vigilantes.jpg";
            case "Wannamakers Widowmakers.jpg":
                return "Wannamaker's Widowmakers.jpg";
            case "Wilsons Hussars.jpg":
                return "Wilson's Hussars.jpg";
            case "Winfields Regiment.jpg":
                return "Winfield's Regiment.jpg";
            case "Zeus Thunderbolt.jpg":
                return "Zeus' Thunderbolts.jpg";
            default:
                return text;
        }
    }

    private static String migrateMagistracyOfCanopus(final String text) {
        switch (text) {
            case "1st Canopian Cuirassers.jpg":
                return "1st Canopian Cuirassiers.jpg";
            case "2nd Canopian Cuirassers.jpg":
                return "2nd Canopian Cuirassiers.jpg";
            case "Raventhirs Iron Hand.jpg":
                return "Raventhir's Iron Hand.jpg";
            default:
                return text;
        }
    }

    private static String migrateMarianHegemony(final String text) {
        switch (text) {
            case "Legio Martia Victrix.jpg":
                return "I Legio Martia Victrix.jpg";
            case "Legio Cataphracti.jpg":
                return "II Legio Cataphracti.jpg";
            case "Legio Limitanei.jpg":
                return "III Legio Limitanei.jpg";
            case "IV Legio Comitantentis.jpg":
            case "Legio Comitatensis.jpg":
                return "IV Legio Comitatensis.jpg";
            case "Legio Ripariensis.jpg":
                return "V Legio Ripariensis.jpg";
            case "Legio Ripariensis2.jpg":
                return "VI Legio Ripariensis.jpg";
            default:
                return text;
        }
    }

    private static String migratePirates(final String text) {
        switch (text) {
            case "Band of The Damned.jpg":
                return "Band of the Damned.jpg";
            case "Deaths Consorts.jpg":
                return "Death's Consorts.jpg";
            case "Morrisons Extractors.jpg":
                return "Morrison's Extractors.jpg";
            case "Shen se Tian.jpg":
                return "Shen-sè Tian.jpg";
            default:
                return text;
        }
    }

    private static String migrateRepublicOfTheSphere(final String text) {
        switch (text) {
            case "Hastatis Sentinels.jpg":
                return "Hastati Sentinels.jpg";
            case "Principes Guard.jpg":
                return "Principes Guards.jpg";
            default:
                return text;
        }
    }

    private static String migrateSolarisStables(final String text) {
        switch (text) {
            case "BlackLions.jpg":
                return "Black Lions.jpg";
            case "BlueFist.jpg":
                return "Blue Fist.jpg";
            case "LionCity.jpg":
                return "Lion City.jpg";
            case "SkyeTigers.jpg":
                return "Skye Tigers.jpg";
            default:
                return text;
        }
    }

    private static String migrateStarLeagueDefenceForce(final String text) {
        switch (text) {
            case "1st Royal Battlemech Regiment.jpg":
                return "1st Royal BattleMech Regiment.jpg";
            case "SLDF drab.jpg":
                return "SLDF Drab.jpg";
            default:
                return text;
        }
    }

    private static String migrateCommonwealthGuards(final String text) {
        switch (text) {
            case "1st Bolan Guard.jpg":
                return "1st Bolan Guards.jpg";
            case "2nd Bolan Guard.jpg":
                return "2nd Bolan Guards.jpg";
            case "1st Buena Guard.jpg":
                return "1st Buena Guards.jpg";
            case "2nd Buena Guard.jpg":
                return "2nd Buena Guards.jpg";
            default:
                return text;
        }
    }

    private static String migrateDifferenceClouds(final String text) {
        switch (text) {
            case "Clan Sea Fox, Diamond Shark.png":
                return "Clan Diamond Shark.png";
            case "Clan Widow Maker.png":
                return "Clan Widowmaker.png";
            case "Comstar.png":
                return "ComStar.png";
            case "Federated Suns, Commonwealth.png":
                return "Federated Suns.png";
            case "Lyran Commonwealth, Alliance.png":
                return "Lyran Commonwealth.png";
            default:
                return text;
        }
    }

    private static String migrateGradients(final String text) {
        switch (text) {
            case "Clan Sea Fox, Diamond Shark.png":
                return "Clan Diamond Shark.png";
            case "Clan Widow Maker.png":
                return "Clan Widowmaker.png";
            case "Comstar.png":
                return "ComStar.png";
            case "Federated Suns, Commonwealth.png":
                return "Federated Suns.png";
            case "Lyran Commonwealth, Alliance.png":
                return "Lyran Commonwealth.png";
            default:
                return text;
        }
    }

    private static String migrateJellyBean(final String text) {
        switch (text) {
            case "Jelly Bean .jpg":
                return "Green.jpg";
            case "Jelly Bean Desert.jpg":
                return "Desert.jpg";
            case "Jelly Bean Ocean.jpg":
                return "Ocean.jpg";
            case "Jelly Bean Opfor.jpg":
                return "Red.jpg";
            case "Jelly Bean Urban.jpg":
                return "Urban.jpg";
            default:
                return text;
        }
    }

    /**
     * This handles anything that cannot be done in the above normal migration, like filename-based category swaps
     */
    private static void finalizeMigration(final Camouflage camouflage) {
        switch (camouflage.getCategory()) {
            case "Star League Defense Force/":
                if (camouflage.getFilename().equalsIgnoreCase("Eridani Light Horse.jpg")) {
                    camouflage.setCategory("Mercs/");
                }
                break;
            case "Fiefdom of Randis/":
                if (camouflage.getFilename().equalsIgnoreCase("Brotherhood of Randis.jpg")) {
                    camouflage.setCategory("Mercs/");
                }
                break;
            case "Mercs/Battle Corps/":
                if (camouflage.getFilename().equalsIgnoreCase("1st Battle Corps.jpg")) {
                    camouflage.setCategory("Mercs/");
                    camouflage.setFilename("The Battle Corps.jpg");
                }
                break;
            case "Mercs/Mobile Fire/":
                if (camouflage.getFilename().equalsIgnoreCase("1st Mobile Fire.jpg")) {
                    camouflage.setCategory("Mercs/");
                    camouflage.setFilename("Mobile Fire.jpg");
                }
                break;
            default:
                break;
        }
    }
}

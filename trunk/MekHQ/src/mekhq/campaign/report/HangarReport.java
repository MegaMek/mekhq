/*
 * RatingReport.java
 *
 * Copyright (c) 2013 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.report;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SpaceStation;
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.Warship;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;



/**
 * @author Jay Lawson
 * @version %I% %G%
 * @since 3/12/2012
 */
public class HangarReport extends Report {
  
    
    public HangarReport(Campaign c) {
        super(c);
    }
    
    public String getTitle() {
        return "Hangar Breakdown";
    }
      
    public JTextPane getReport() {
        JTextPane txtReport = new JTextPane();
        txtReport.setFont(new Font("Courier New", Font.PLAIN, 12));
        
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Hangar");
        JTree overviewHangarTree = new JTree(top);
 
        // BattleMechs
        boolean expandMechs = false;
        int countMechs = 0;
        int countBattleMechs = 0;
        int countOmniMechs = 0;
        int colossalMech = 0;
        int assaultMech = 0;
        int heavyMech = 0;
        int mediumMech = 0;
        int lightMech = 0;
        int ultralightMech = 0;
        int colossalOmniMech = 0;
        int assaultOmniMech = 0;
        int heavyOmniMech = 0;
        int mediumOmniMech = 0;
        int lightOmniMech = 0;
        int ultralightOmniMech = 0;
        
        // ASF
        boolean expandASF = false;
        int countASF = 0;
        int countStandardASF = 0;
        int countOmniASF = 0;
        int countLightASF = 0;
        int countMediumASF = 0;
        int countHeavyASF = 0;
        int countOmniLightASF = 0;
        int countOmniMediumASF = 0;
        int countOmniHeavyASF = 0;
        
        // Vehicles
        boolean expandVees = false;
        int countVees = 0;
        int countStandardVees = 0;
        int countOmniVees = 0;
        int countVTOL = 0;
        int countVTOLLight = 0;
        int countSub = 0;
        int countSubColossal = 0;
        int countSubAssault = 0;
        int countSubHeavy = 0;
        int countSubMedium = 0;
        int countSubLight = 0;
        int countNaval = 0;
        int countNavalColossal = 0;
        int countNavalAssault = 0;
        int countNavalHeavy = 0;
        int countNavalMedium = 0;
        int countNavalLight = 0;
        int countWiGE = 0;
        int countWiGEAssault = 0;
        int countWiGEHeavy = 0;
        int countWiGEMedium = 0;
        int countWiGELight = 0;
        int countTracked = 0;
        int countTrackedColossal = 0;
        int countTrackedAssault = 0;
        int countTrackedHeavy = 0;
        int countTrackedMedium = 0;
        int countTrackedLight = 0;
        int countWheeled = 0;
        int countWheeledAssault = 0;
        int countWheeledHeavy = 0;
        int countWheeledMedium = 0;
        int countWheeledLight = 0;
        int countHover = 0;
        int countHoverMedium = 0;
        int countHoverLight = 0;
        int countHydrofoil = 0;
        int countHydrofoilAssault = 0;
        int countHydrofoilHeavy = 0;
        int countHydrofoilMedium = 0;
        int countHydrofoilLight = 0;
        int countOmniVTOL = 0;
        int countOmniVTOLLight = 0;
        int countOmniSub = 0;
        int countOmniSubColossal = 0;
        int countOmniSubAssault = 0;
        int countOmniSubHeavy = 0;
        int countOmniSubMedium = 0;
        int countOmniSubLight = 0;
        int countOmniNaval = 0;
        int countOmniNavalColossal = 0;
        int countOmniNavalAssault = 0;
        int countOmniNavalHeavy = 0;
        int countOmniNavalMedium = 0;
        int countOmniNavalLight = 0;
        int countOmniWiGE = 0;
        int countOmniWiGEAssault = 0;
        int countOmniWiGEHeavy = 0;
        int countOmniWiGEMedium = 0;
        int countOmniWiGELight = 0;
        int countOmniTracked = 0;
        int countOmniTrackedColossal = 0;
        int countOmniTrackedAssault = 0;
        int countOmniTrackedHeavy = 0;
        int countOmniTrackedMedium = 0;
        int countOmniTrackedLight = 0;
        int countOmniWheeled = 0;
        int countOmniWheeledAssault = 0;
        int countOmniWheeledHeavy = 0;
        int countOmniWheeledMedium = 0;
        int countOmniWheeledLight = 0;
        int countOmniHover = 0;
        int countOmniHoverMedium = 0;
        int countOmniHoverLight = 0;
        int countOmniHydrofoil = 0;
        int countOmniHydrofoilAssault = 0;
        int countOmniHydrofoilHeavy = 0;
        int countOmniHydrofoilMedium = 0;
        int countOmniHydrofoilLight = 0;
        
        // Battle Armor and Infantry
        boolean expandInfantry = false;
        int countInfantry = 0;
        int countFootInfantry = 0;
        int countJumpInfantry = 0;
        int countMotorizedInfantry = 0;
        int countMechanizedInfantry = 0;
        int countBA = 0;
        int countBAPAL = 0;
        int countBALight = 0;
        int countBAMedium = 0;
        int countBAHeavy = 0;
        int countBAAssault = 0;
        
        // Jumpships, Warships, Dropships, and SmallCraft
        boolean expandSpace = false;
        int countSpace = 0;
        int countJumpships = 0;
        int countWarships = 0;
        int countLargeWS = 0;
        int countSmallWS = 0;
        int countDropships = 0;
        int countLargeDS = 0;
        int countMediumDS = 0;
        int countSmallDS = 0;
        int countSmallCraft = 0;
        
        // Conventional Fighters
        int countConv = 0;
        
        // Support Vees
        /*boolean expandSupportVees = false;
        int countSupportVees = 0;
        int countSupportStandardVees = 0;
        int countSupportOmniVees = 0;
        int countSupportVTOL = 0;
        int countSupportVTOLLight = 0;
        int countSupportSub = 0;
        int countSupportSubColossal = 0;
        int countSupportSubAssault = 0;
        int countSupportSubHeavy = 0;
        int countSupportSubMedium = 0;
        int countSupportSubLight = 0;
        int countSupportNaval = 0;
        int countSupportNavalColossal = 0;
        int countSupportNavalAssault = 0;
        int countSupportNavalHeavy = 0;
        int countSupportNavalMedium = 0;
        int countSupportNavalLight = 0;
        int countSupportWiGE = 0;
        int countSupportWiGEAssault = 0;
        int countSupportWiGEHeavy = 0;
        int countSupportWiGEMedium = 0;
        int countSupportWiGELight = 0;
        int countSupportTracked = 0;
        int countSupportTrackedColossal = 0;
        int countSupportTrackedAssault = 0;
        int countSupportTrackedHeavy = 0;
        int countSupportTrackedMedium = 0;
        int countSupportTrackedLight = 0;
        int countSupportWheeled = 0;
        int countSupportWheeledAssault = 0;
        int countSupportWheeledHeavy = 0;
        int countSupportWheeledMedium = 0;
        int countSupportWheeledLight = 0;
        int countSupportHover = 0;
        int countSupportHoverMedium = 0;
        int countSupportHoverLight = 0;
        int countSupportHydrofoil = 0;
        int countSupportHydrofoilAssault = 0;
        int countSupportHydrofoilHeavy = 0;
        int countSupportHydrofoilMedium = 0;
        int countSupportHydrofoilLight = 0;
        int countSupportOmniVTOL = 0;
        int countSupportOmniVTOLLight = 0;
        int countSupportOmniSub = 0;
        int countSupportOmniSubColossal = 0;
        int countSupportOmniSubAssault = 0;
        int countSupportOmniSubHeavy = 0;
        int countSupportOmniSubMedium = 0;
        int countSupportOmniSubLight = 0;
        int countSupportOmniNaval = 0;
        int countSupportOmniNavalColossal = 0;
        int countSupportOmniNavalAssault = 0;
        int countSupportOmniNavalHeavy = 0;
        int countSupportOmniNavalMedium = 0;
        int countSupportOmniNavalLight = 0;
        int countSupportOmniWiGE = 0;
        int countSupportOmniWiGEAssault = 0;
        int countSupportOmniWiGEHeavy = 0;
        int countSupportOmniWiGEMedium = 0;
        int countSupportOmniWiGELight = 0;
        int countSupportOmniTracked = 0;
        int countSupportOmniTrackedColossal = 0;
        int countSupportOmniTrackedAssault = 0;
        int countSupportOmniTrackedHeavy = 0;
        int countSupportOmniTrackedMedium = 0;
        int countSupportOmniTrackedLight = 0;
        int countSupportOmniWheeled = 0;
        int countSupportOmniWheeledAssault = 0;
        int countSupportOmniWheeledHeavy = 0;
        int countSupportOmniWheeledMedium = 0;
        int countSupportOmniWheeledLight = 0;
        int countSupportOmniHover = 0;
        int countSupportOmniHoverMedium = 0;
        int countSupportOmniHoverLight = 0;
        int countSupportOmniHydrofoil = 0;
        int countSupportOmniHydrofoilAssault = 0;
        int countSupportOmniHydrofoilHeavy = 0;
        int countSupportOmniHydrofoilMedium = 0;
        int countSupportOmniHydrofoilLight = 0;*/
        
        // Turrets
        int countGE = 0;
        
        // Protomechs
        boolean expandProtos = false;
        int countProtos = 0;
        int countLightProtos = 0;
        int countMediumProtos = 0;
        int countHeavyProtos = 0;
        int countAssaultProtos = 0;
        
        
        // Gather data and load it into the tree
        for (Unit u : getCampaign().getUnits()) {
            Entity e = u.getEntity();
            if (e instanceof Mech) {
                countMechs++;
                if (e.isOmni()) {
                    countOmniMechs++;
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                        ultralightOmniMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        lightOmniMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        mediumOmniMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        heavyOmniMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                        assaultOmniMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                        colossalOmniMech++;
                    }
                } else {
                    countBattleMechs++;
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                        ultralightMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        lightMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        mediumMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        heavyMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                        assaultMech++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                        colossalMech++;
                    }
                }
            } else if (e instanceof ConvFighter) {
                countConv++;
            } else if (e instanceof SpaceStation) {
                continue;
            } else if (e instanceof Warship) {
                countSpace++;
                countWarships++;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_WAR) {
                    countSmallWS++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_WAR) {
                    countLargeWS++;
                }
            } else if (e instanceof Jumpship) {
                countSpace++;
                countJumpships++;
            } else if (e instanceof Dropship) {
                countSpace++;
                countDropships++;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_DROP) {
                    countSmallDS++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_DROP) {
                    countMediumDS++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_DROP) {
                    countLargeDS++;
                }
            } else if (e instanceof SmallCraft) {
                countSpace++;
                countSmallCraft++;
            } else if (e instanceof Aero) {
                countASF++;
                if (e.isOmni()) {
                    countOmniASF++;
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        countOmniLightASF++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        countOmniMediumASF++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        countOmniHeavyASF++;
                    }
                } else {
                    countStandardASF++;
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        countLightASF++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        countMediumASF++;
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        countHeavyASF++;
                    }
                }
            } else if (e instanceof Protomech) {
                countProtos++;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    countLightProtos++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    countMediumProtos++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    countHeavyProtos++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    countAssaultProtos++;
                }
            } else if (e instanceof GunEmplacement) {
                countGE++;
            } else if (e instanceof SupportTank || e instanceof SupportVTOL) {
                continue;
            } else if (e instanceof Tank) {
                countVees++;
                if (e.isOmni()) {
                    countOmniVees++;
                    if (e instanceof VTOL) {
                        countOmniVTOL++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniVTOLLight++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countOmniTracked++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniTrackedLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniTrackedMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countOmniTrackedHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countOmniTrackedAssault++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            countOmniTrackedColossal++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countOmniWheeled++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniWheeledLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniWheeledMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countOmniWheeledHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countOmniWheeledAssault++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countOmniHover++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniHoverLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniHoverMedium++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countOmniWiGE++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniWiGELight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniWiGEMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countOmniWiGEHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countOmniWiGEAssault++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countOmniNaval++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniNavalLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniNavalMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countOmniNavalHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countOmniNavalAssault++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            countOmniNavalColossal++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countOmniSub++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniSubLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniSubMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countOmniSubHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countOmniSubAssault++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            countOmniSubColossal++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniHydrofoilLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countOmniHydrofoilMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countOmniHydrofoilHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countOmniHydrofoilAssault++;
                        }
                    }
                } else {
                    countStandardVees++;
                    if (e instanceof VTOL) {
                        countVTOL++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countVTOLLight++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countTracked++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countTrackedLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countTrackedMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countTrackedHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countTrackedAssault++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            countTrackedColossal++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countWheeled++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countWheeledLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countWheeledMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countWheeledHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countWheeledAssault++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countHover++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countHoverLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countHoverMedium++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countWiGE++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countWiGELight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countWiGEMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countWiGEHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countWiGEAssault++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countNaval++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countNavalLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countNavalMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countNavalHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countNavalAssault++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            countNavalColossal++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countSub++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countSubLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countSubMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countSubHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countSubAssault++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            countSubColossal++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countHydrofoilLight++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            countHydrofoilMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            countHydrofoilHeavy++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            countHydrofoilAssault++;
                        }
                    }
                }
            } else if (e instanceof BattleArmor) {
                countBA++;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                    countBAPAL++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    countBALight++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    countBAMedium++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    countBAHeavy++;
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    countBAAssault++;
                }
            } else if (e instanceof Infantry) {
                countInfantry++;
                if (((Infantry) e).isMechanized()) {
                    countMechanizedInfantry++;
                } else if (e.getMovementMode() == EntityMovementMode.INF_JUMP) {
                    countJumpInfantry++;
                } else if (e.getMovementMode() == EntityMovementMode.INF_LEG) {
                    countFootInfantry++;
                } else if (e.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                    countMotorizedInfantry++;
                }
            }
        }
        
        // Mech Nodes
        final DefaultMutableTreeNode mechs = new DefaultMutableTreeNode("'Mechs: "+countMechs);
        DefaultMutableTreeNode battlemechs = new DefaultMutableTreeNode("BattleMechs: "+countBattleMechs);
        DefaultMutableTreeNode omnis = new DefaultMutableTreeNode("OmniMechs: "+countOmniMechs);
        mechs.add(battlemechs);
        mechs.add(omnis);
        DefaultMutableTreeNode colossalmechs = new DefaultMutableTreeNode("Colossal: "+colossalMech);
        battlemechs.add(colossalmechs);
        DefaultMutableTreeNode assaultmechs = new DefaultMutableTreeNode("Assault: "+assaultMech);
        battlemechs.add(assaultmechs);
        DefaultMutableTreeNode heavymechs = new DefaultMutableTreeNode("Heavy: "+heavyMech);
        battlemechs.add(heavymechs);
        DefaultMutableTreeNode mediummechs = new DefaultMutableTreeNode("Medium: "+mediumMech);
        battlemechs.add(mediummechs);
        DefaultMutableTreeNode lightmechs = new DefaultMutableTreeNode("Light: "+lightMech);
        battlemechs.add(lightmechs);
        DefaultMutableTreeNode ultralightmechs = new DefaultMutableTreeNode("Ultralight: "+ultralightMech);
        battlemechs.add(ultralightmechs);
        DefaultMutableTreeNode colossalomnis = new DefaultMutableTreeNode("Colossal: "+colossalOmniMech);
        omnis.add(colossalomnis);
        DefaultMutableTreeNode assaultomnis = new DefaultMutableTreeNode("Assault: "+assaultOmniMech);
        omnis.add(assaultomnis);
        DefaultMutableTreeNode heavyomnis = new DefaultMutableTreeNode("Heavy: "+heavyOmniMech);
        omnis.add(heavyomnis);
        DefaultMutableTreeNode mediumomnis = new DefaultMutableTreeNode("Medium: "+mediumOmniMech);
        omnis.add(mediumomnis);
        DefaultMutableTreeNode lightomnis = new DefaultMutableTreeNode("Light: "+lightOmniMech);
        omnis.add(lightomnis);
        DefaultMutableTreeNode ultralightomnis = new DefaultMutableTreeNode("Ultralight: "+ultralightOmniMech);
        omnis.add(ultralightomnis);
        top.add(mechs);
        
        // ASF Nodes
        final DefaultMutableTreeNode ASF = new DefaultMutableTreeNode("'Aerospace Fighters: "+countASF);
        DefaultMutableTreeNode sASF = new DefaultMutableTreeNode("Standard Fighters: "+countStandardASF);
        DefaultMutableTreeNode oASF = new DefaultMutableTreeNode("OmniFighters: "+countOmniASF);
        ASF.add(sASF);
        ASF.add(oASF);
        DefaultMutableTreeNode sHeavyASF = new DefaultMutableTreeNode("Heavy: "+countHeavyASF);
        sASF.add(sHeavyASF);
        DefaultMutableTreeNode sMediumASF = new DefaultMutableTreeNode("Medium: "+countMediumASF);
        sASF.add(sMediumASF);
        DefaultMutableTreeNode sLightASF = new DefaultMutableTreeNode("Light: "+countLightASF);
        sASF.add(sLightASF);
        DefaultMutableTreeNode oHeavyASF = new DefaultMutableTreeNode("Heavy: "+countOmniHeavyASF);
        oASF.add(oHeavyASF);
        DefaultMutableTreeNode oMediumASF = new DefaultMutableTreeNode("Medium: "+countOmniMediumASF);
        oASF.add(oMediumASF);
        DefaultMutableTreeNode oLightASF = new DefaultMutableTreeNode("Light: "+countOmniLightASF);
        oASF.add(oLightASF);
        top.add(ASF);
        
        // Vee Nodes
        final DefaultMutableTreeNode vees = new DefaultMutableTreeNode("Vehicles: "+countVees);
        DefaultMutableTreeNode sVees = new DefaultMutableTreeNode("Standard: "+countStandardVees);
        DefaultMutableTreeNode oVees = new DefaultMutableTreeNode("OmniVees: "+countOmniVees);
        vees.add(sVees);
        vees.add(oVees);
        DefaultMutableTreeNode sTracked = new DefaultMutableTreeNode("Tracked: "+countTracked);
        sVees.add(sTracked);
        DefaultMutableTreeNode sTrackedColossal = new DefaultMutableTreeNode("Super Heavy: "+countTrackedColossal);
        sTracked.add(sTrackedColossal);
        DefaultMutableTreeNode sTrackedAssault = new DefaultMutableTreeNode("Assault: "+countTrackedAssault);
        sTracked.add(sTrackedAssault);
        DefaultMutableTreeNode sTrackedHeavy = new DefaultMutableTreeNode("Heavy: "+countTrackedHeavy);
        sTracked.add(sTrackedHeavy);
        DefaultMutableTreeNode sTrackedMedium = new DefaultMutableTreeNode("Medium: "+countTrackedMedium);
        sTracked.add(sTrackedMedium);
        DefaultMutableTreeNode sTrackedLight = new DefaultMutableTreeNode("Light: "+countTrackedLight);
        sTracked.add(sTrackedLight);
        DefaultMutableTreeNode oTracked = new DefaultMutableTreeNode("Tracked: "+countOmniTracked);
        oVees.add(oTracked);
        DefaultMutableTreeNode oTrackedColossal = new DefaultMutableTreeNode("Super Heavy: "+countOmniTrackedColossal);
        oTracked.add(oTrackedColossal);
        DefaultMutableTreeNode oTrackedAssault = new DefaultMutableTreeNode("Assault: "+countOmniTrackedAssault);
        oTracked.add(oTrackedAssault);
        DefaultMutableTreeNode oTrackedHeavy = new DefaultMutableTreeNode("Heavy: "+countOmniTrackedHeavy);
        oTracked.add(oTrackedHeavy);
        DefaultMutableTreeNode oTrackedMedium = new DefaultMutableTreeNode("Medium: "+countOmniTrackedMedium);
        oTracked.add(oTrackedMedium);
        DefaultMutableTreeNode oTrackedLight = new DefaultMutableTreeNode("Light: "+countOmniTrackedLight);
        oTracked.add(oTrackedLight);
        DefaultMutableTreeNode sWheeled = new DefaultMutableTreeNode("Wheeled: "+countWheeled);
        sVees.add(sWheeled);
        DefaultMutableTreeNode sWheeledAssault = new DefaultMutableTreeNode("Assault: "+countWheeledAssault);
        sWheeled.add(sWheeledAssault);
        DefaultMutableTreeNode sWheeledHeavy = new DefaultMutableTreeNode("Heavy: "+countWheeledHeavy);
        sWheeled.add(sWheeledHeavy);
        DefaultMutableTreeNode sWheeledMedium = new DefaultMutableTreeNode("Medium: "+countWheeledMedium);
        sWheeled.add(sWheeledMedium);
        DefaultMutableTreeNode sWheeledLight = new DefaultMutableTreeNode("Light: "+countWheeledLight);
        sWheeled.add(sWheeledLight);
        DefaultMutableTreeNode oWheeled = new DefaultMutableTreeNode("Wheeled: "+countOmniWheeled);
        oVees.add(oWheeled);
        DefaultMutableTreeNode oWheeledAssault = new DefaultMutableTreeNode("Assault: "+countOmniWheeledAssault);
        oWheeled.add(oWheeledAssault);
        DefaultMutableTreeNode oWheeledHeavy = new DefaultMutableTreeNode("Heavy: "+countOmniWheeledHeavy);
        oWheeled.add(oWheeledHeavy);
        DefaultMutableTreeNode oWheeledMedium = new DefaultMutableTreeNode("Medium: "+countOmniWheeledMedium);
        oWheeled.add(oWheeledMedium);
        DefaultMutableTreeNode oWheeledLight = new DefaultMutableTreeNode("Light: "+countOmniWheeledLight);
        oWheeled.add(oWheeledLight);
        DefaultMutableTreeNode sHover = new DefaultMutableTreeNode("Hover: "+countHover);
        sVees.add(sHover);
        DefaultMutableTreeNode sHoverMedium = new DefaultMutableTreeNode("Medium: "+countHoverMedium);
        sHover.add(sHoverMedium);
        DefaultMutableTreeNode sHoverLight = new DefaultMutableTreeNode("Light: "+countHoverLight);
        sHover.add(sHoverLight);
        DefaultMutableTreeNode oHover = new DefaultMutableTreeNode("Hover: "+countOmniHover);
        oVees.add(oHover);
        DefaultMutableTreeNode oHoverMedium = new DefaultMutableTreeNode("Medium: "+countOmniHoverMedium);
        oHover.add(oHoverMedium);
        DefaultMutableTreeNode oHoverLight = new DefaultMutableTreeNode("Light: "+countOmniHoverLight);
        oHover.add(oHoverLight);
        DefaultMutableTreeNode sVTOL = new DefaultMutableTreeNode("VTOL: "+countVTOL);
        sVees.add(sVTOL);
        DefaultMutableTreeNode sVTOLLight = new DefaultMutableTreeNode("Light: "+countVTOLLight);
        sVTOL.add(sVTOLLight);
        DefaultMutableTreeNode oVTOL = new DefaultMutableTreeNode("VTOL: "+countOmniVTOL);
        oVees.add(oVTOL);
        DefaultMutableTreeNode oVTOLLight = new DefaultMutableTreeNode("Light: "+countOmniVTOLLight);
        oVTOL.add(oVTOLLight);
        DefaultMutableTreeNode sWiGE = new DefaultMutableTreeNode("WiGE: "+countWiGE);
        sVees.add(sWiGE);
        DefaultMutableTreeNode sWiGEAssault = new DefaultMutableTreeNode("Assault: "+countWiGEAssault);
        sWiGE.add(sWiGEAssault);
        DefaultMutableTreeNode sWiGEHeavy = new DefaultMutableTreeNode("Heavy: "+countWiGEHeavy);
        sWiGE.add(sWiGEHeavy);
        DefaultMutableTreeNode sWiGEMedium = new DefaultMutableTreeNode("Medium: "+countWiGEMedium);
        sWiGE.add(sWiGEMedium);
        DefaultMutableTreeNode sWiGELight = new DefaultMutableTreeNode("Light: "+countWiGELight);
        sWiGE.add(sWiGELight);
        DefaultMutableTreeNode oWiGE = new DefaultMutableTreeNode("WiGE: "+countOmniWiGE);
        oVees.add(oWiGE);
        DefaultMutableTreeNode oWiGEAssault = new DefaultMutableTreeNode("Assault: "+countOmniWiGEAssault);
        oWiGE.add(oWiGEAssault);
        DefaultMutableTreeNode oWiGEHeavy = new DefaultMutableTreeNode("Heavy: "+countOmniWiGEHeavy);
        oWiGE.add(oWiGEHeavy);
        DefaultMutableTreeNode oWiGEMedium = new DefaultMutableTreeNode("Medium: "+countOmniWiGEMedium);
        oWiGE.add(oWiGEMedium);
        DefaultMutableTreeNode oWiGELight = new DefaultMutableTreeNode("Light: "+countOmniWiGELight);
        oWiGE.add(oWiGELight);
        DefaultMutableTreeNode sNaval = new DefaultMutableTreeNode("Naval: "+countNaval);
        sVees.add(sNaval);
        DefaultMutableTreeNode sNavalColossal = new DefaultMutableTreeNode("Super Heavy: "+countNavalColossal);
        sNaval.add(sNavalColossal);
        DefaultMutableTreeNode sNavalAssault = new DefaultMutableTreeNode("Assault: "+countNavalAssault);
        sNaval.add(sNavalAssault);
        DefaultMutableTreeNode sNavalHeavy = new DefaultMutableTreeNode("Heavy: "+countNavalHeavy);
        sNaval.add(sNavalHeavy);
        DefaultMutableTreeNode sNavalMedium = new DefaultMutableTreeNode("Medium: "+countNavalMedium);
        sNaval.add(sNavalMedium);
        DefaultMutableTreeNode sNavalLight = new DefaultMutableTreeNode("Light: "+countNavalLight);
        sNaval.add(sNavalLight);
        DefaultMutableTreeNode oNaval = new DefaultMutableTreeNode("Naval: "+countOmniNaval);
        oVees.add(oNaval);
        DefaultMutableTreeNode oNavalColossal = new DefaultMutableTreeNode("Super Heavy: "+countOmniNavalColossal);
        oNaval.add(oNavalColossal);
        DefaultMutableTreeNode oNavalAssault = new DefaultMutableTreeNode("Assault: "+countOmniNavalAssault);
        oNaval.add(oNavalAssault);
        DefaultMutableTreeNode oNavalHeavy = new DefaultMutableTreeNode("Heavy: "+countOmniNavalHeavy);
        oNaval.add(oNavalHeavy);
        DefaultMutableTreeNode oNavalMedium = new DefaultMutableTreeNode("Medium: "+countOmniNavalMedium);
        oNaval.add(oNavalMedium);
        DefaultMutableTreeNode oNavalLight = new DefaultMutableTreeNode("Light: "+countOmniNavalLight);
        oNaval.add(oNavalLight);
        DefaultMutableTreeNode sSub = new DefaultMutableTreeNode("Sub: "+countSub);
        sVees.add(sSub);
        DefaultMutableTreeNode sSubColossal = new DefaultMutableTreeNode("Super Heavy: "+countSubColossal);
        sSub.add(sSubColossal);
        DefaultMutableTreeNode sSubAssault = new DefaultMutableTreeNode("Assault: "+countSubAssault);
        sSub.add(sSubAssault);
        DefaultMutableTreeNode sSubHeavy = new DefaultMutableTreeNode("Heavy: "+countSubHeavy);
        sSub.add(sSubHeavy);
        DefaultMutableTreeNode sSubMedium = new DefaultMutableTreeNode("Medium: "+countSubMedium);
        sSub.add(sSubMedium);
        DefaultMutableTreeNode sSubLight = new DefaultMutableTreeNode("Light: "+countSubLight);
        sSub.add(sSubLight);
        DefaultMutableTreeNode oSub = new DefaultMutableTreeNode("Sub: "+countOmniSub);
        oVees.add(oSub);
        DefaultMutableTreeNode oSubColossal = new DefaultMutableTreeNode("Super Heavy: "+countOmniSubColossal);
        oSub.add(oSubColossal);
        DefaultMutableTreeNode oSubAssault = new DefaultMutableTreeNode("Assault: "+countOmniSubAssault);
        oSub.add(oSubAssault);
        DefaultMutableTreeNode oSubHeavy = new DefaultMutableTreeNode("Heavy: "+countOmniSubHeavy);
        oSub.add(oSubHeavy);
        DefaultMutableTreeNode oSubMedium = new DefaultMutableTreeNode("Medium: "+countOmniSubMedium);
        oSub.add(oSubMedium);
        DefaultMutableTreeNode oSubLight = new DefaultMutableTreeNode("Light: "+countOmniSubLight);
        oSub.add(oSubLight);
        DefaultMutableTreeNode sHydrofoil = new DefaultMutableTreeNode("Hydrofoil: "+countHydrofoil);
        sVees.add(sHydrofoil);
        DefaultMutableTreeNode sHydrofoilAssault = new DefaultMutableTreeNode("Assault: "+countHydrofoilAssault);
        sHydrofoil.add(sHydrofoilAssault);
        DefaultMutableTreeNode sHydrofoilHeavy = new DefaultMutableTreeNode("Heavy: "+countHydrofoilHeavy);
        sHydrofoil.add(sHydrofoilHeavy);
        DefaultMutableTreeNode sHydrofoilMedium = new DefaultMutableTreeNode("Medium: "+countHydrofoilMedium);
        sHydrofoil.add(sHydrofoilMedium);
        DefaultMutableTreeNode sHydrofoilLight = new DefaultMutableTreeNode("Light: "+countHydrofoilLight);
        sHydrofoil.add(sHydrofoilLight);
        DefaultMutableTreeNode oHydrofoil = new DefaultMutableTreeNode("Hydrofoil: "+countOmniHydrofoil);
        oVees.add(oHydrofoil);
        DefaultMutableTreeNode oHydrofoilAssault = new DefaultMutableTreeNode("Assault: "+countOmniHydrofoilAssault);
        oHydrofoil.add(oHydrofoilAssault);
        DefaultMutableTreeNode oHydrofoilHeavy = new DefaultMutableTreeNode("Heavy: "+countOmniHydrofoilHeavy);
        oHydrofoil.add(oHydrofoilHeavy);
        DefaultMutableTreeNode oHydrofoilMedium = new DefaultMutableTreeNode("Medium: "+countOmniHydrofoilMedium);
        oHydrofoil.add(oHydrofoilMedium);
        DefaultMutableTreeNode oHydrofoilLight = new DefaultMutableTreeNode("Light: "+countOmniHydrofoilLight);
        oHydrofoil.add(oHydrofoilLight);
        top.add(vees);
        
        // Conventional Fighters
        final DefaultMutableTreeNode conv = new DefaultMutableTreeNode("Conventional Fighters: "+countConv);
        top.add(conv);
        
        // Infantry Nodes
        int allInfantry = (countInfantry+countBA);
        final DefaultMutableTreeNode inf = new DefaultMutableTreeNode("Infantry: "+allInfantry);
        DefaultMutableTreeNode cInf = new DefaultMutableTreeNode("Conventional: "+countInfantry);
        DefaultMutableTreeNode BAInf = new DefaultMutableTreeNode("Battle Armor: "+countBA);
        inf.add(cInf);
        inf.add(BAInf);
        DefaultMutableTreeNode infFoot = new DefaultMutableTreeNode("Foot Platoons: "+countFootInfantry);
        cInf.add(infFoot);
        DefaultMutableTreeNode infJump = new DefaultMutableTreeNode("Jump Platoons: "+countJumpInfantry);
        cInf.add(infJump);
        DefaultMutableTreeNode infMechanized = new DefaultMutableTreeNode("Mechanized Platoons: "+countMechanizedInfantry);
        cInf.add(infMechanized);
        DefaultMutableTreeNode infMotorized = new DefaultMutableTreeNode("Motorized Platoons: "+countMotorizedInfantry);
        cInf.add(infMotorized);
        DefaultMutableTreeNode baPAL = new DefaultMutableTreeNode("PAL/Exoskeleton: "+countBAPAL);
        BAInf.add(baPAL);
        DefaultMutableTreeNode baLight = new DefaultMutableTreeNode("Light: "+countBALight);
        BAInf.add(baLight);
        DefaultMutableTreeNode baMedium = new DefaultMutableTreeNode("Medium: "+countBAMedium);
        BAInf.add(baMedium);
        DefaultMutableTreeNode baHeavy = new DefaultMutableTreeNode("Heavy: "+countBAHeavy);
        BAInf.add(baHeavy);
        DefaultMutableTreeNode baAssault = new DefaultMutableTreeNode("Assault: "+countBAAssault);
        BAInf.add(baAssault);
        top.add(inf);
        
        // Protomechs
        final DefaultMutableTreeNode protos = new DefaultMutableTreeNode("Protomechs: "+countProtos);
        DefaultMutableTreeNode plight = new DefaultMutableTreeNode("Light: "+countLightProtos);
        protos.add(plight);
        DefaultMutableTreeNode pmedium = new DefaultMutableTreeNode("Medium: "+countMediumProtos);
        protos.add(pmedium);
        DefaultMutableTreeNode pheavy = new DefaultMutableTreeNode("Heavy: "+countHeavyProtos);
        protos.add(pheavy);
        DefaultMutableTreeNode passault = new DefaultMutableTreeNode("Assault: "+countAssaultProtos);
        protos.add(passault);
        top.add(protos);
        
        // Turrets
        final DefaultMutableTreeNode ge = new DefaultMutableTreeNode("Gun Emplacements: "+countGE);
        top.add(ge);
        
        // Space
        final DefaultMutableTreeNode space = new DefaultMutableTreeNode("Spacecraft: "+countSpace);
        DefaultMutableTreeNode ws = new DefaultMutableTreeNode("Warships: "+countWarships);
        space.add(ws);
        DefaultMutableTreeNode js = new DefaultMutableTreeNode("Jumpships: "+countJumpships);
        space.add(js);
        DefaultMutableTreeNode ds = new DefaultMutableTreeNode("Dropships: "+countDropships);
        space.add(ds);
        DefaultMutableTreeNode sc = new DefaultMutableTreeNode("Small Craft: "+countSmallCraft);
        space.add(sc);
        DefaultMutableTreeNode smws = new DefaultMutableTreeNode("Small Warships: "+countSmallWS);
        ws.add(smws);
        DefaultMutableTreeNode lgws = new DefaultMutableTreeNode("Large Warships: "+countLargeWS);
        ws.add(lgws);
        DefaultMutableTreeNode smds = new DefaultMutableTreeNode("Small Dropships: "+countSmallDS);
        ds.add(smds);
        DefaultMutableTreeNode mdds = new DefaultMutableTreeNode("Medium Dropships: "+countMediumDS);
        ds.add(mdds);
        DefaultMutableTreeNode lgds = new DefaultMutableTreeNode("Large Dropships: "+countLargeDS);
        ds.add(lgds);
        top.add(space);
        
        for (Unit u : getCampaign().getUnits()) {
            Entity e = u.getEntity();
            if (e instanceof Mech) {
                expandMechs = true;
                if (e.isOmni()) {
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                        ultralightomnis.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        lightomnis.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        mediumomnis.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        heavyomnis.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                        assaultomnis.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                        colossalomnis.add(new DefaultMutableTreeNode(u.getName()));
                    }
                } else {
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                        ultralightmechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        lightmechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        mediummechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        heavymechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                        assaultmechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                        colossalmechs.add(new DefaultMutableTreeNode(u.getName()));
                    }
                }
            } else if (e instanceof ConvFighter) {
                conv.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof Warship) {
                expandSpace = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_WAR) {
                    smws.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_WAR) {
                    lgws.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof Jumpship) {
                expandSpace = true;
                js.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof Dropship) {
                expandSpace = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_DROP) {
                    smds.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_DROP) {
                    mdds.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_DROP) {
                    lgds.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof SmallCraft) {
                expandSpace = true;
                sc.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof Aero) {
                expandASF = true;
                if (e.isOmni()) {
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        oLightASF.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        oMediumASF.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        oHeavyASF.add(new DefaultMutableTreeNode(u.getName()));
                    }
                } else {
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        sLightASF.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        sMediumASF.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        sHeavyASF.add(new DefaultMutableTreeNode(u.getName()));
                    }
                }
            } else if (e instanceof Protomech) {
                expandProtos = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    plight.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    pmedium.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    pheavy.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    passault.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof GunEmplacement) {
                ge.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof SupportTank || e instanceof SupportVTOL) {
                continue;
            } else if (e instanceof Tank) {
                expandVees = true;
                if (e.isOmni()) {
                    if (e instanceof VTOL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oVTOLLight.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oTrackedLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oTrackedMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oTrackedHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oTrackedAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            oTrackedColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oWheeledLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oWheeledMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oWheeledHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oWheeledAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oHoverLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oHoverMedium.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oWiGELight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oWiGEMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oWiGEHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oWiGEAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oNavalLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oNavalMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oNavalHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oNavalAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            oNavalColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSubLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSubMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSubHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSubAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            oSubColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oHydrofoilLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oHydrofoilMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oHydrofoilHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oHydrofoilAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    }
                } else {
                    if (e instanceof VTOL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sVTOLLight.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sTrackedLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sTrackedMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sTrackedHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sTrackedAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            sTrackedColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sWheeledLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sWheeledMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sWheeledHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sWheeledAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sHoverLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sHoverMedium.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sWiGELight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sWiGEMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sWiGEHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sWiGEAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sNavalLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sNavalMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sNavalHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sNavalAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            sNavalColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSubLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSubMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSubHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSubAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            sSubColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sHydrofoilLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sHydrofoilMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sHydrofoilHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sHydrofoilAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    }
                }
            } else if (e instanceof BattleArmor) {
                expandInfantry = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                    baPAL.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    baLight.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    baMedium.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    baHeavy.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    baAssault.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof Infantry) {
                expandInfantry = true;
                if (((Infantry) e).isMechanized()) {
                    infMechanized.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getMovementMode() == EntityMovementMode.INF_JUMP) {
                    infJump.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getMovementMode() == EntityMovementMode.INF_LEG) {
                    infFoot.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                    infMotorized.add(new DefaultMutableTreeNode(u.getName()));
                }
            }
        }
        
        // Reset our UI
        final boolean expandMechsFinal = expandMechs;
        final boolean expandASFFinal = expandASF;
        final boolean expandVeesFinal = expandVees;
        final boolean expandInfantryFinal = expandInfantry; 
        final boolean expandSpaceFinal = expandSpace;
        final boolean expandProtosFinal = expandProtos;
        
        overviewHangarTree.setSelectionPath(null);
        overviewHangarTree.expandPath(new TreePath(top.getPath()));
        /*
        if (expandMechsFinal) {
            overviewHangarTree.expandPath(new TreePath(mechs.getPath()));
        }
        if (expandASFFinal) {
            overviewHangarTree.expandPath(new TreePath(ASF.getPath()));
        }
        if (expandVeesFinal) {
            overviewHangarTree.expandPath(new TreePath(vees.getPath()));
        }
        if (expandInfantryFinal) {
            overviewHangarTree.expandPath(new TreePath(inf.getPath()));
        }
        if (expandSpaceFinal) {
            overviewHangarTree.expandPath(new TreePath(space.getPath()));
        }
        if (expandProtosFinal) {
            overviewHangarTree.expandPath(new TreePath(protos.getPath()));
        }*/
        
        int countInTransit = 0;
        int countPresent = 0;
        int countDamaged = 0;
        int countDeployed = 0;
        for (Unit u : getCampaign().getUnits()) {
            if (u.isPresent()) {
                countPresent++;
            } else {
                countInTransit++;
            }
            if (u.isDamaged()) {
                countDamaged++;
            }
            if (u.isDeployed()) {
                countDeployed++;
            }
        }
        
        txtReport.setAlignmentY(1.0f);

        txtReport.setText("Total Units: "+getCampaign().getUnits().size()+
                "\n  Present: "+countPresent+
                "\n  In Transit: "+countInTransit+
                "\n  Damaged: "+countDamaged+
                "\n  Deployed: "+countDeployed + "\n\n\n");
        txtReport.insertComponent(overviewHangarTree);
      
        return txtReport;
    }
   
}
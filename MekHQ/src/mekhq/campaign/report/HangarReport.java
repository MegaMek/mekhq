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

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTextPane;
import javax.swing.JTree;
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

    public JTree getHangarTree() {
        //region Variable Declarations
        // BattleMechs
        // boolean expandMechs = false;
        int countMechs = 0;

        int countBattleMechs = 0;
        int colossalMech = 0;
        int assaultMech = 0;
        int heavyMech = 0;
        int mediumMech = 0;
        int lightMech = 0;
        int ultralightMech = 0;

        int countOmniMechs = 0;
        int colossalOmniMech = 0;
        int assaultOmniMech = 0;
        int heavyOmniMech = 0;
        int mediumOmniMech = 0;
        int lightOmniMech = 0;
        int ultralightOmniMech = 0;

        // ASF
        //boolean expandASF = false;
        int countASF = 0;

        int countStandardASF = 0;
        int countHeavyASF = 0;
        int countMediumASF = 0;
        int countLightASF = 0;

        int countOmniASF = 0;
        int countOmniHeavyASF = 0;
        int countOmniMediumASF = 0;
        int countOmniLightASF = 0;

        // Vehicles
        //boolean expandVees = false;
        int countVees = 0;

        int countStandardVees = 0;

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

        int countVTOL = 0;
        int countVTOLLight = 0;

        int countWiGE = 0;
        int countWiGEAssault = 0;
        int countWiGEHeavy = 0;
        int countWiGEMedium = 0;
        int countWiGELight = 0;

        int countNaval = 0;
        int countNavalColossal = 0;
        int countNavalAssault = 0;
        int countNavalHeavy = 0;
        int countNavalMedium = 0;
        int countNavalLight = 0;

        int countSub = 0;
        int countSubColossal = 0;
        int countSubAssault = 0;
        int countSubHeavy = 0;
        int countSubMedium = 0;
        int countSubLight = 0;

        int countHydrofoil = 0;
        int countHydrofoilAssault = 0;
        int countHydrofoilHeavy = 0;
        int countHydrofoilMedium = 0;
        int countHydrofoilLight = 0;

        int countOmniVees = 0;

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

        int countOmniVTOL = 0;
        int countOmniVTOLLight = 0;

        int countOmniWiGE = 0;
        int countOmniWiGEAssault = 0;
        int countOmniWiGEHeavy = 0;
        int countOmniWiGEMedium = 0;
        int countOmniWiGELight = 0;

        int countOmniNaval = 0;
        int countOmniNavalColossal = 0;
        int countOmniNavalAssault = 0;
        int countOmniNavalHeavy = 0;
        int countOmniNavalMedium = 0;
        int countOmniNavalLight = 0;

        int countOmniSub = 0;
        int countOmniSubColossal = 0;
        int countOmniSubAssault = 0;
        int countOmniSubHeavy = 0;
        int countOmniSubMedium = 0;
        int countOmniSubLight = 0;

        int countOmniHydrofoil = 0;
        int countOmniHydrofoilAssault = 0;
        int countOmniHydrofoilHeavy = 0;
        int countOmniHydrofoilMedium = 0;
        int countOmniHydrofoilLight = 0;

        // Support Vees
        //boolean expandSupportVees = false;
        int countSupportVees = 0;
        int countSupportStandardVees = 0;
        int countSupportOmniVees = 0;

        int countSupportWheeled = 0;
        int countSupportWheeledSmall = 0;
        int countSupportWheeledMedium = 0;
        int countSupportWheeledLarge = 0;

        int countSupportTracked = 0;
        int countSupportTrackedSmall = 0;
        int countSupportTrackedMedium = 0;
        int countSupportTrackedLarge = 0;

        int countSupportHover = 0;
        int countSupportHoverSmall = 0;
        int countSupportHoverMedium = 0;
        int countSupportHoverLarge = 0;

        int countSupportVTOL = 0;
        int countSupportVTOLSmall = 0;
        int countSupportVTOLMedium = 0;
        int countSupportVTOLLarge = 0;

        int countSupportWiGE = 0;
        int countSupportWiGESmall = 0;
        int countSupportWiGEMedium = 0;
        int countSupportWiGELarge = 0;

        int countSupportAirship = 0;
        int countSupportAirshipSmall = 0;
        int countSupportAirshipMedium = 0;
        int countSupportAirshipLarge = 0;

        int countSupportFixedWing = 0;
        int countSupportFixedWingSmall = 0;
        int countSupportFixedWingMedium = 0;
        int countSupportFixedWingLarge = 0;

        int countSupportNaval = 0;
        int countSupportNavalSmall = 0;
        int countSupportNavalMedium = 0;
        int countSupportNavalLarge = 0;
        int countSupportSub = 0;
        int countSupportSubSmall = 0;
        int countSupportSubMedium = 0;
        int countSupportSubLarge = 0;
        int countSupportHydrofoil = 0;
        int countSupportHydrofoilSmall = 0;
        int countSupportHydrofoilMedium = 0;
        int countSupportHydrofoilLarge = 0;

        int countSupportOmniTracked = 0;
        int countSupportOmniTrackedSmall = 0;
        int countSupportOmniTrackedMedium = 0;
        int countSupportOmniTrackedLarge = 0;

        int countSupportOmniWheeled = 0;
        int countSupportOmniWheeledSmall = 0;
        int countSupportOmniWheeledMedium = 0;
        int countSupportOmniWheeledLarge = 0;

        int countSupportOmniHover = 0;
        int countSupportOmniHoverSmall = 0;
        int countSupportOmniHoverMedium = 0;
        int countSupportOmniHoverLarge = 0;

        int countSupportOmniVTOL = 0;
        int countSupportOmniVTOLSmall = 0;
        int countSupportOmniVTOLMedium = 0;
        int countSupportOmniVTOLLarge = 0;

        int countSupportOmniWiGE = 0;
        int countSupportOmniWiGESmall = 0;
        int countSupportOmniWiGEMedium = 0;
        int countSupportOmniWiGELarge = 0;

        int countSupportOmniAirship = 0;
        int countSupportOmniAirshipSmall = 0;
        int countSupportOmniAirshipMedium = 0;
        int countSupportOmniAirshipLarge = 0;

        int countSupportOmniFixedWing = 0;
        int countSupportOmniFixedWingSmall = 0;
        int countSupportOmniFixedWingMedium = 0;
        int countSupportOmniFixedWingLarge = 0;

        int countSupportOmniNaval = 0;
        int countSupportOmniNavalSmall = 0;
        int countSupportOmniNavalMedium = 0;
        int countSupportOmniNavalLarge = 0;
        int countSupportOmniSub = 0;
        int countSupportOmniSubSmall = 0;
        int countSupportOmniSubMedium = 0;
        int countSupportOmniSubLarge = 0;
        int countSupportOmniHydrofoil = 0;
        int countSupportOmniHydrofoilSmall = 0;
        int countSupportOmniHydrofoilMedium = 0;
        int countSupportOmniHydrofoilLarge = 0;

        // Battle Armor and Infantry
        //boolean expandInfantry = false;
        int countInfantry = 0;

        int countFootInfantry = 0;
        int countMotorizedInfantry = 0;
        int countJumpInfantry = 0;
        int countMechanizedInfantry = 0;

        int countBA = 0;
        int countBAAssault = 0;
        int countBAHeavy = 0;
        int countBAMedium = 0;
        int countBALight = 0;
        int countBAPAL = 0;

        // Conventional Fighter
        int countConv = 0;

        // Protomechs
        //boolean expandProtos = false;
        int countProtos = 0;
        int countAssaultProtos = 0;
        int countHeavyProtos = 0;
        int countMediumProtos = 0;
        int countLightProtos = 0;

        // Turrets
        int countGE = 0;

        // Jumpships, Warships, Dropships, and SmallCraft
        //boolean expandSpace = false;
        int countSpace = 0;

        int countSmallCraft = 0;

        int countDropships = 0;
        int countLargeDS = 0;
        int countMediumDS = 0;
        int countSmallDS = 0;

        int countJumpships = 0;

        int countWarships = 0;
        int countLargeWS = 0;
        int countSmallWS = 0;

        // Space Stations
        int countSpaceStations = 0;

        //endregion Variable Declarations

        //region Tree Creation
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Hangar");
        JTree overviewHangarTree = new JTree(top);

        // Mech Nodes
        final DefaultMutableTreeNode mechs = new DefaultMutableTreeNode();

        DefaultMutableTreeNode battlemechs = new DefaultMutableTreeNode();
        mechs.add(battlemechs);
        DefaultMutableTreeNode colossalMechs = new DefaultMutableTreeNode();
        battlemechs.add(colossalMechs);
        DefaultMutableTreeNode assaultMechs = new DefaultMutableTreeNode();
        battlemechs.add(assaultMechs);
        DefaultMutableTreeNode heavyMechs = new DefaultMutableTreeNode();
        battlemechs.add(heavyMechs);
        DefaultMutableTreeNode mediumMechs = new DefaultMutableTreeNode();
        battlemechs.add(mediumMechs);
        DefaultMutableTreeNode lightMechs = new DefaultMutableTreeNode();
        battlemechs.add(lightMechs);
        DefaultMutableTreeNode ultralightMechs = new DefaultMutableTreeNode();
        battlemechs.add(ultralightMechs);

        DefaultMutableTreeNode omnis = new DefaultMutableTreeNode();
        mechs.add(omnis);
        DefaultMutableTreeNode colossalOmniMechs = new DefaultMutableTreeNode();
        omnis.add(colossalOmniMechs);
        DefaultMutableTreeNode assaultOmniMechs = new DefaultMutableTreeNode();
        omnis.add(assaultOmniMechs);
        DefaultMutableTreeNode heavyOmniMechs = new DefaultMutableTreeNode();
        omnis.add(heavyOmniMechs);
        DefaultMutableTreeNode mediumOmniMechs = new DefaultMutableTreeNode();
        omnis.add(mediumOmniMechs);
        DefaultMutableTreeNode lightOmniMechs = new DefaultMutableTreeNode();
        omnis.add(lightOmniMechs);
        DefaultMutableTreeNode ultralightOmniMechs = new DefaultMutableTreeNode();
        omnis.add(ultralightOmniMechs);

        top.add(mechs);

        // ASF Nodes
        final DefaultMutableTreeNode ASF = new DefaultMutableTreeNode();

        DefaultMutableTreeNode sASF = new DefaultMutableTreeNode();
        ASF.add(sASF);
        DefaultMutableTreeNode sHeavyASF = new DefaultMutableTreeNode();
        sASF.add(sHeavyASF);
        DefaultMutableTreeNode sMediumASF = new DefaultMutableTreeNode();
        sASF.add(sMediumASF);
        DefaultMutableTreeNode sLightASF = new DefaultMutableTreeNode();
        sASF.add(sLightASF);

        DefaultMutableTreeNode oASF = new DefaultMutableTreeNode();
        ASF.add(oASF);
        DefaultMutableTreeNode oHeavyASF = new DefaultMutableTreeNode();
        oASF.add(oHeavyASF);
        DefaultMutableTreeNode oMediumASF = new DefaultMutableTreeNode();
        oASF.add(oMediumASF);
        DefaultMutableTreeNode oLightASF = new DefaultMutableTreeNode();
        oASF.add(oLightASF);

        top.add(ASF);

        // Vee Nodes
        final DefaultMutableTreeNode vees = new DefaultMutableTreeNode();

        DefaultMutableTreeNode sVees = new DefaultMutableTreeNode();
        vees.add(sVees);

        DefaultMutableTreeNode sTracked = new DefaultMutableTreeNode();
        sVees.add(sTracked);
        DefaultMutableTreeNode sTrackedColossal = new DefaultMutableTreeNode();
        sTracked.add(sTrackedColossal);
        DefaultMutableTreeNode sTrackedAssault = new DefaultMutableTreeNode();
        sTracked.add(sTrackedAssault);
        DefaultMutableTreeNode sTrackedHeavy = new DefaultMutableTreeNode();
        sTracked.add(sTrackedHeavy);
        DefaultMutableTreeNode sTrackedMedium = new DefaultMutableTreeNode();
        sTracked.add(sTrackedMedium);
        DefaultMutableTreeNode sTrackedLight = new DefaultMutableTreeNode();
        sTracked.add(sTrackedLight);

        DefaultMutableTreeNode sWheeled = new DefaultMutableTreeNode();
        sVees.add(sWheeled);
        DefaultMutableTreeNode sWheeledAssault = new DefaultMutableTreeNode();
        sWheeled.add(sWheeledAssault);
        DefaultMutableTreeNode sWheeledHeavy = new DefaultMutableTreeNode();
        sWheeled.add(sWheeledHeavy);
        DefaultMutableTreeNode sWheeledMedium = new DefaultMutableTreeNode();
        sWheeled.add(sWheeledMedium);
        DefaultMutableTreeNode sWheeledLight = new DefaultMutableTreeNode();
        sWheeled.add(sWheeledLight);

        DefaultMutableTreeNode sHover = new DefaultMutableTreeNode();
        sVees.add(sHover);
        DefaultMutableTreeNode sHoverMedium = new DefaultMutableTreeNode( );
        sHover.add(sHoverMedium);
        DefaultMutableTreeNode sHoverLight = new DefaultMutableTreeNode();
        sHover.add(sHoverLight);

        DefaultMutableTreeNode sVTOL = new DefaultMutableTreeNode();
        sVees.add(sVTOL);
        DefaultMutableTreeNode sVTOLLight = new DefaultMutableTreeNode();
        sVTOL.add(sVTOLLight);

        DefaultMutableTreeNode sWiGE = new DefaultMutableTreeNode();
        sVees.add(sWiGE);
        DefaultMutableTreeNode sWiGEAssault = new DefaultMutableTreeNode();
        sWiGE.add(sWiGEAssault);
        DefaultMutableTreeNode sWiGEHeavy = new DefaultMutableTreeNode();
        sWiGE.add(sWiGEHeavy);
        DefaultMutableTreeNode sWiGEMedium = new DefaultMutableTreeNode();
        sWiGE.add(sWiGEMedium);
        DefaultMutableTreeNode sWiGELight = new DefaultMutableTreeNode();
        sWiGE.add(sWiGELight);

        DefaultMutableTreeNode sNaval = new DefaultMutableTreeNode();
        sVees.add(sNaval);
        DefaultMutableTreeNode sNavalColossal = new DefaultMutableTreeNode();
        sNaval.add(sNavalColossal);
        DefaultMutableTreeNode sNavalAssault = new DefaultMutableTreeNode();
        sNaval.add(sNavalAssault);
        DefaultMutableTreeNode sNavalHeavy = new DefaultMutableTreeNode();
        sNaval.add(sNavalHeavy);
        DefaultMutableTreeNode sNavalMedium = new DefaultMutableTreeNode();
        sNaval.add(sNavalMedium);
        DefaultMutableTreeNode sNavalLight = new DefaultMutableTreeNode();
        sNaval.add(sNavalLight);

        DefaultMutableTreeNode sSub = new DefaultMutableTreeNode();
        sVees.add(sSub);
        DefaultMutableTreeNode sSubColossal = new DefaultMutableTreeNode();
        sSub.add(sSubColossal);
        DefaultMutableTreeNode sSubAssault = new DefaultMutableTreeNode();
        sSub.add(sSubAssault);
        DefaultMutableTreeNode sSubHeavy = new DefaultMutableTreeNode();
        sSub.add(sSubHeavy);
        DefaultMutableTreeNode sSubMedium = new DefaultMutableTreeNode();
        sSub.add(sSubMedium);
        DefaultMutableTreeNode sSubLight = new DefaultMutableTreeNode();
        sSub.add(sSubLight);

        DefaultMutableTreeNode sHydrofoil = new DefaultMutableTreeNode();
        sVees.add(sHydrofoil);
        DefaultMutableTreeNode sHydrofoilAssault = new DefaultMutableTreeNode();
        sHydrofoil.add(sHydrofoilAssault);
        DefaultMutableTreeNode sHydrofoilHeavy = new DefaultMutableTreeNode();
        sHydrofoil.add(sHydrofoilHeavy);
        DefaultMutableTreeNode sHydrofoilMedium = new DefaultMutableTreeNode();
        sHydrofoil.add(sHydrofoilMedium);
        DefaultMutableTreeNode sHydrofoilLight = new DefaultMutableTreeNode();
        sHydrofoil.add(sHydrofoilLight);

        DefaultMutableTreeNode oVees = new DefaultMutableTreeNode();
        vees.add(oVees);

        DefaultMutableTreeNode oTracked = new DefaultMutableTreeNode();
        oVees.add(oTracked);
        DefaultMutableTreeNode oTrackedColossal = new DefaultMutableTreeNode();
        oTracked.add(oTrackedColossal);
        DefaultMutableTreeNode oTrackedAssault = new DefaultMutableTreeNode();
        oTracked.add(oTrackedAssault);
        DefaultMutableTreeNode oTrackedHeavy = new DefaultMutableTreeNode();
        oTracked.add(oTrackedHeavy);
        DefaultMutableTreeNode oTrackedMedium = new DefaultMutableTreeNode();
        oTracked.add(oTrackedMedium);
        DefaultMutableTreeNode oTrackedLight = new DefaultMutableTreeNode();
        oTracked.add(oTrackedLight);

        DefaultMutableTreeNode oWheeled = new DefaultMutableTreeNode();
        oVees.add(oWheeled);
        DefaultMutableTreeNode oWheeledAssault = new DefaultMutableTreeNode();
        oWheeled.add(oWheeledAssault);
        DefaultMutableTreeNode oWheeledHeavy = new DefaultMutableTreeNode();
        oWheeled.add(oWheeledHeavy);
        DefaultMutableTreeNode oWheeledMedium = new DefaultMutableTreeNode();
        oWheeled.add(oWheeledMedium);
        DefaultMutableTreeNode oWheeledLight = new DefaultMutableTreeNode();
        oWheeled.add(oWheeledLight);

        DefaultMutableTreeNode oHover = new DefaultMutableTreeNode();
        oVees.add(oHover);
        DefaultMutableTreeNode oHoverMedium = new DefaultMutableTreeNode();
        oHover.add(oHoverMedium);
        DefaultMutableTreeNode oHoverLight = new DefaultMutableTreeNode();
        oHover.add(oHoverLight);

        DefaultMutableTreeNode oVTOL = new DefaultMutableTreeNode();
        oVees.add(oVTOL);
        DefaultMutableTreeNode oVTOLLight = new DefaultMutableTreeNode();
        oVTOL.add(oVTOLLight);

        DefaultMutableTreeNode oWiGE = new DefaultMutableTreeNode();
        oVees.add(oWiGE);
        DefaultMutableTreeNode oWiGEAssault = new DefaultMutableTreeNode();
        oWiGE.add(oWiGEAssault);
        DefaultMutableTreeNode oWiGEHeavy = new DefaultMutableTreeNode();
        oWiGE.add(oWiGEHeavy);
        DefaultMutableTreeNode oWiGEMedium = new DefaultMutableTreeNode();
        oWiGE.add(oWiGEMedium);
        DefaultMutableTreeNode oWiGELight = new DefaultMutableTreeNode();
        oWiGE.add(oWiGELight);

        DefaultMutableTreeNode oNaval = new DefaultMutableTreeNode();
        oVees.add(oNaval);
        DefaultMutableTreeNode oNavalColossal = new DefaultMutableTreeNode();
        oNaval.add(oNavalColossal);
        DefaultMutableTreeNode oNavalAssault = new DefaultMutableTreeNode();
        oNaval.add(oNavalAssault);
        DefaultMutableTreeNode oNavalHeavy = new DefaultMutableTreeNode();
        oNaval.add(oNavalHeavy);
        DefaultMutableTreeNode oNavalMedium = new DefaultMutableTreeNode();
        oNaval.add(oNavalMedium);
        DefaultMutableTreeNode oNavalLight = new DefaultMutableTreeNode();
        oNaval.add(oNavalLight);

        DefaultMutableTreeNode oSub = new DefaultMutableTreeNode();
        oVees.add(oSub);
        DefaultMutableTreeNode oSubColossal = new DefaultMutableTreeNode();
        oSub.add(oSubColossal);
        DefaultMutableTreeNode oSubAssault = new DefaultMutableTreeNode();
        oSub.add(oSubAssault);
        DefaultMutableTreeNode oSubHeavy = new DefaultMutableTreeNode();
        oSub.add(oSubHeavy);
        DefaultMutableTreeNode oSubMedium = new DefaultMutableTreeNode();
        oSub.add(oSubMedium);
        DefaultMutableTreeNode oSubLight = new DefaultMutableTreeNode();
        oSub.add(oSubLight);

        DefaultMutableTreeNode oHydrofoil = new DefaultMutableTreeNode();
        oVees.add(oHydrofoil);
        DefaultMutableTreeNode oHydrofoilAssault = new DefaultMutableTreeNode();
        oHydrofoil.add(oHydrofoilAssault);
        DefaultMutableTreeNode oHydrofoilHeavy = new DefaultMutableTreeNode();
        oHydrofoil.add(oHydrofoilHeavy);
        DefaultMutableTreeNode oHydrofoilMedium = new DefaultMutableTreeNode();
        oHydrofoil.add(oHydrofoilMedium);
        DefaultMutableTreeNode oHydrofoilLight = new DefaultMutableTreeNode();
        oHydrofoil.add(oHydrofoilLight);

        top.add(vees);

        // Support Vee Nodes
        final DefaultMutableTreeNode supportVees = new DefaultMutableTreeNode();

        // Standard Support Vees
        DefaultMutableTreeNode sSupportVees = new DefaultMutableTreeNode();
        supportVees.add(sSupportVees);

        DefaultMutableTreeNode sSupportWheeled = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportWheeled);
        DefaultMutableTreeNode sSupportWheeledLarge = new DefaultMutableTreeNode();
        sSupportWheeled.add(sSupportWheeledLarge);
        DefaultMutableTreeNode sSupportWheeledMedium = new DefaultMutableTreeNode();
        sSupportWheeled.add(sWheeledMedium);
        DefaultMutableTreeNode sSupportWheeledSmall = new DefaultMutableTreeNode();
        sSupportWheeled.add(sSupportWheeledSmall);

        DefaultMutableTreeNode sSupportTracked = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportTracked);
        DefaultMutableTreeNode sSupportTrackedLarge = new DefaultMutableTreeNode();
        sSupportTracked.add(sSupportTrackedLarge);
        DefaultMutableTreeNode sSupportTrackedMedium = new DefaultMutableTreeNode();
        sSupportTracked.add(sSupportTrackedMedium);
        DefaultMutableTreeNode sSupportTrackedSmall = new DefaultMutableTreeNode();
        sSupportTracked.add(sSupportTrackedSmall);

        DefaultMutableTreeNode sSupportHover = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportHover);
        DefaultMutableTreeNode sSupportHoverLarge = new DefaultMutableTreeNode();
        sSupportHover.add(sSupportHoverLarge);
        DefaultMutableTreeNode sSupportHoverMedium = new DefaultMutableTreeNode();
        sSupportHover.add(sSupportHoverMedium);
        DefaultMutableTreeNode sSupportHoverSmall = new DefaultMutableTreeNode();
        sSupportHover.add(sSupportHoverSmall);

        DefaultMutableTreeNode sSupportVTOL = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportVTOL);
        DefaultMutableTreeNode sSupportVTOLLarge = new DefaultMutableTreeNode();
        sSupportVTOL.add(sSupportVTOLLarge);
        DefaultMutableTreeNode sSupportVTOLMedium = new DefaultMutableTreeNode();
        sSupportVTOL.add(sSupportVTOLMedium);
        DefaultMutableTreeNode sSupportVTOLSmall = new DefaultMutableTreeNode();
        sSupportVTOL.add(sSupportVTOLSmall);

        DefaultMutableTreeNode sSupportWiGE = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportWiGE);
        DefaultMutableTreeNode sSupportWiGELarge = new DefaultMutableTreeNode();
        sSupportWiGE.add(sSupportWiGELarge);
        DefaultMutableTreeNode sSupportWiGEMedium = new DefaultMutableTreeNode();
        sSupportWiGE.add(sSupportWiGEMedium);
        DefaultMutableTreeNode sSupportWiGESmall = new DefaultMutableTreeNode();
        sSupportWiGE.add(sSupportWiGESmall);

        DefaultMutableTreeNode sSupportAirship = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportAirship);
        DefaultMutableTreeNode sSupportAirshipLarge = new DefaultMutableTreeNode();
        sSupportAirship.add(sSupportAirshipLarge);
        DefaultMutableTreeNode sSupportAirshipMedium = new DefaultMutableTreeNode();
        sSupportAirship.add(sSupportAirshipMedium);
        DefaultMutableTreeNode sSupportAirshipSmall = new DefaultMutableTreeNode();
        sSupportAirship.add(sSupportAirshipSmall);

        DefaultMutableTreeNode sSupportFixedWing = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportFixedWing);
        DefaultMutableTreeNode sSupportFixedWingLarge = new DefaultMutableTreeNode();
        sSupportFixedWing.add(sSupportFixedWingLarge);
        DefaultMutableTreeNode sSupportFixedWingMedium = new DefaultMutableTreeNode();
        sSupportFixedWing.add(sSupportFixedWingMedium);
        DefaultMutableTreeNode sSupportFixedWingSmall = new DefaultMutableTreeNode();
        sSupportFixedWing.add(sSupportFixedWingSmall);

        DefaultMutableTreeNode sSupportNaval = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportNaval);
        DefaultMutableTreeNode sSupportNavalLarge = new DefaultMutableTreeNode();
        sSupportNaval.add(sSupportNavalLarge);
        DefaultMutableTreeNode sSupportNavalMedium = new DefaultMutableTreeNode();
        sSupportNaval.add(sSupportNavalMedium);
        DefaultMutableTreeNode sSupportNavalSmall = new DefaultMutableTreeNode();
        sSupportNaval.add(sSupportNavalSmall);

        DefaultMutableTreeNode sSupportSub = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportSub);
        DefaultMutableTreeNode sSupportSubLarge = new DefaultMutableTreeNode();
        sSupportSub.add(sSupportSubLarge);
        DefaultMutableTreeNode sSupportSubMedium = new DefaultMutableTreeNode();
        sSupportSub.add(sSupportSubMedium);
        DefaultMutableTreeNode sSupportSubSmall = new DefaultMutableTreeNode();
        sSupportSub.add(sSupportSubSmall);

        DefaultMutableTreeNode sSupportHydrofoil = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportHydrofoil);
        DefaultMutableTreeNode sSupportHydrofoilLarge = new DefaultMutableTreeNode();
        sSupportHydrofoil.add(sSupportHydrofoilLarge);
        DefaultMutableTreeNode sSupportHydrofoilMedium = new DefaultMutableTreeNode();
        sSupportHydrofoil.add(sSupportHydrofoilMedium);
        DefaultMutableTreeNode sSupportHydrofoilSmall = new DefaultMutableTreeNode();
        sSupportHydrofoil.add(sSupportHydrofoilSmall);

        // Omni Support Vees
        DefaultMutableTreeNode oSupportVees = new DefaultMutableTreeNode();
        supportVees.add(oSupportVees);

        DefaultMutableTreeNode oSupportWheeled = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportWheeled);
        DefaultMutableTreeNode oSupportWheeledLarge = new DefaultMutableTreeNode();
        oSupportWheeled.add(oSupportWheeledLarge);
        DefaultMutableTreeNode oSupportWheeledMedium = new DefaultMutableTreeNode();
        oSupportWheeled.add(oSupportWheeledMedium);
        DefaultMutableTreeNode oSupportWheeledSmall = new DefaultMutableTreeNode();
        oSupportWheeled.add(oSupportWheeledSmall);

        DefaultMutableTreeNode oSupportTracked = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportTracked);
        DefaultMutableTreeNode oSupportTrackedLarge = new DefaultMutableTreeNode();
        oSupportTracked.add(oSupportTrackedLarge);
        DefaultMutableTreeNode oSupportTrackedMedium = new DefaultMutableTreeNode();
        oSupportTracked.add(oSupportTrackedMedium);
        DefaultMutableTreeNode oSupportTrackedSmall = new DefaultMutableTreeNode();
        oSupportTracked.add(oSupportTrackedSmall);

        DefaultMutableTreeNode oSupportHover = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportHover);
        DefaultMutableTreeNode oSupportHoverLarge = new DefaultMutableTreeNode();
        oSupportHover.add(oSupportHoverLarge);
        DefaultMutableTreeNode oSupportHoverMedium = new DefaultMutableTreeNode();
        oSupportHover.add(oSupportHoverMedium);
        DefaultMutableTreeNode oSupportHoverSmall = new DefaultMutableTreeNode();
        oSupportHover.add(oSupportHoverSmall);

        DefaultMutableTreeNode oSupportVTOL = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportVTOL);
        DefaultMutableTreeNode oSupportVTOLLarge = new DefaultMutableTreeNode();
        oSupportVTOL.add(oSupportVTOLLarge);
        DefaultMutableTreeNode oSupportVTOLMedium = new DefaultMutableTreeNode();
        oSupportVTOL.add(oSupportVTOLMedium);
        DefaultMutableTreeNode oSupportVTOLSmall = new DefaultMutableTreeNode();
        oSupportVTOL.add(oSupportVTOLSmall);

        DefaultMutableTreeNode oSupportWiGE = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportWiGE);
        DefaultMutableTreeNode oSupportWiGELarge = new DefaultMutableTreeNode();
        oSupportWiGE.add(oSupportWiGELarge);
        DefaultMutableTreeNode oSupportWiGEMedium = new DefaultMutableTreeNode();
        oSupportWiGE.add(oSupportWiGEMedium);
        DefaultMutableTreeNode oSupportWiGESmall = new DefaultMutableTreeNode();
        oSupportWiGE.add(oSupportWiGESmall);

        DefaultMutableTreeNode oSupportAirship = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportAirship);
        DefaultMutableTreeNode oSupportAirshipLarge = new DefaultMutableTreeNode();
        oSupportAirship.add(oSupportAirshipLarge);
        DefaultMutableTreeNode oSupportAirshipMedium = new DefaultMutableTreeNode();
        oSupportAirship.add(oSupportAirshipMedium);
        DefaultMutableTreeNode oSupportAirshipSmall = new DefaultMutableTreeNode();
        oSupportAirship.add(oSupportAirshipSmall);

        DefaultMutableTreeNode oSupportFixedWing = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportFixedWing);
        DefaultMutableTreeNode oSupportFixedWingLarge = new DefaultMutableTreeNode();
        oSupportFixedWing.add(oSupportFixedWingLarge);
        DefaultMutableTreeNode oSupportFixedWingMedium = new DefaultMutableTreeNode();
        oSupportFixedWing.add(oSupportFixedWingMedium);
        DefaultMutableTreeNode oSupportFixedWingSmall = new DefaultMutableTreeNode();
        oSupportFixedWing.add(oSupportFixedWingSmall);

        DefaultMutableTreeNode oSupportNaval = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportNaval);
        DefaultMutableTreeNode oSupportNavalLarge = new DefaultMutableTreeNode();
        oSupportNaval.add(oSupportNavalLarge);
        DefaultMutableTreeNode oSupportNavalMedium = new DefaultMutableTreeNode();
        oSupportNaval.add(oSupportNavalMedium);
        DefaultMutableTreeNode oSupportNavalSmall = new DefaultMutableTreeNode();
        oSupportNaval.add(oSupportNavalSmall);

        DefaultMutableTreeNode oSupportSub = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportSub);
        DefaultMutableTreeNode oSupportSubLarge = new DefaultMutableTreeNode();
        oSupportSub.add(oSupportSubLarge);
        DefaultMutableTreeNode oSupportSubMedium = new DefaultMutableTreeNode();
        oSupportSub.add(oSupportSubMedium);
        DefaultMutableTreeNode oSupportSubSmall = new DefaultMutableTreeNode();
        oSupportSub.add(oSupportSubSmall);

        DefaultMutableTreeNode oSupportHydrofoil = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportHydrofoil);
        DefaultMutableTreeNode oSupportHydrofoilLarge = new DefaultMutableTreeNode();
        oSupportHydrofoil.add(oSupportHydrofoilLarge);
        DefaultMutableTreeNode oSupportHydrofoilMedium = new DefaultMutableTreeNode();
        oSupportHydrofoil.add(oSupportHydrofoilMedium);
        DefaultMutableTreeNode oSupportHydrofoilSmall = new DefaultMutableTreeNode();
        oSupportHydrofoil.add(oSupportHydrofoilSmall);

        top.add(supportVees);

        // Infantry Nodes
        final DefaultMutableTreeNode inf = new DefaultMutableTreeNode();

        DefaultMutableTreeNode cInf = new DefaultMutableTreeNode();
        inf.add(cInf);
        DefaultMutableTreeNode infFoot = new DefaultMutableTreeNode();
        cInf.add(infFoot);
        DefaultMutableTreeNode infMotorized = new DefaultMutableTreeNode();
        cInf.add(infMotorized);
        DefaultMutableTreeNode infJump = new DefaultMutableTreeNode();
        cInf.add(infJump);
        DefaultMutableTreeNode infMechanized = new DefaultMutableTreeNode();
        cInf.add(infMechanized);

        DefaultMutableTreeNode BAInf = new DefaultMutableTreeNode();
        inf.add(BAInf);
        DefaultMutableTreeNode baAssault = new DefaultMutableTreeNode();
        BAInf.add(baAssault);
        DefaultMutableTreeNode baHeavy = new DefaultMutableTreeNode();
        BAInf.add(baHeavy);
        DefaultMutableTreeNode baMedium = new DefaultMutableTreeNode();
        BAInf.add(baMedium);
        DefaultMutableTreeNode baLight = new DefaultMutableTreeNode();
        BAInf.add(baLight);
        DefaultMutableTreeNode baPAL = new DefaultMutableTreeNode();
        BAInf.add(baPAL);

        top.add(inf);

        // Conventional Fighters
        final DefaultMutableTreeNode conv = new DefaultMutableTreeNode();

        top.add(conv);

        // ProtoMechs
        final DefaultMutableTreeNode protos = new DefaultMutableTreeNode();
        DefaultMutableTreeNode pAssault = new DefaultMutableTreeNode();
        protos.add(pAssault);
        DefaultMutableTreeNode pHeavy = new DefaultMutableTreeNode();
        protos.add(pHeavy);
        DefaultMutableTreeNode pMedium = new DefaultMutableTreeNode();
        protos.add(pMedium);
        DefaultMutableTreeNode pLight = new DefaultMutableTreeNode();
        protos.add(pLight);

        top.add(protos);

        // Turrets
        final DefaultMutableTreeNode ge = new DefaultMutableTreeNode();

        top.add(ge);

        // Space
        final DefaultMutableTreeNode space = new DefaultMutableTreeNode();

        DefaultMutableTreeNode sc = new DefaultMutableTreeNode();
        space.add(sc);

        DefaultMutableTreeNode ds = new DefaultMutableTreeNode();
        space.add(ds);
        DefaultMutableTreeNode lgds = new DefaultMutableTreeNode();
        ds.add(lgds);
        DefaultMutableTreeNode mdds = new DefaultMutableTreeNode();
        ds.add(mdds);
        DefaultMutableTreeNode smds = new DefaultMutableTreeNode();
        ds.add(smds);

        DefaultMutableTreeNode js = new DefaultMutableTreeNode();
        space.add(js);

        DefaultMutableTreeNode ws = new DefaultMutableTreeNode();
        space.add(ws);
        DefaultMutableTreeNode lgws = new DefaultMutableTreeNode();
        ws.add(lgws);
        DefaultMutableTreeNode smws = new DefaultMutableTreeNode();
        ws.add(smws);

        top.add(space);

        // Space Stations
        final DefaultMutableTreeNode spaceStation = new DefaultMutableTreeNode();

        top.add(spaceStation);
        //endregion Node Creation

        //region UnitList Processing
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
                countSpaceStations++;
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
            } else if (e.isSupportVehicle()){
                countSupportVees++;
                if (e.isOmni()) {
                    countSupportOmniVees++;

                    if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countSupportOmniWheeled++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniWheeledSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniWheeledMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniWheeledLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countSupportOmniTracked++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniTrackedSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniTrackedMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniTrackedLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countSupportOmniHover++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniHoverSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniHoverMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniHoverLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.VTOL) {
                        countSupportOmniVTOL++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniVTOLSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniVTOLMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniVTOLLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countSupportOmniWiGE++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniWiGESmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniWiGEMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniWiGELarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.AIRSHIP) {
                        countSupportOmniAirship++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniAirshipSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniAirshipMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniAirshipLarge++;
                        }
                    } else if (false) { //TODO : Add Fixed Wing-Support
                        countSupportOmniFixedWing++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniFixedWingSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniFixedWingMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniFixedWingLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countSupportOmniNaval++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniNavalSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniNavalMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniNavalLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countSupportOmniSub++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniSubSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniSubMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniSubLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        countSupportOmniHydrofoil++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportOmniHydrofoilSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportOmniHydrofoilMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportOmniHydrofoilLarge++;
                        }
                    }
                } else {
                    countSupportStandardVees++;

                    if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countSupportWheeled++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportWheeledSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportWheeledMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportWheeledLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countSupportTracked++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportTrackedSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportTrackedMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportTrackedLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countSupportHover++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportHoverSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportHoverMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportHoverLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.VTOL) {
                        countSupportVTOL++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportVTOLSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportVTOLMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportVTOLLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countSupportWiGE++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportWiGESmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportWiGEMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportWiGELarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.AIRSHIP) {
                        countSupportAirship++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportAirshipSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportAirshipMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportAirshipLarge++;
                        }
                    } else if (false) { //TODO : Add Fixed-Wing Support
                        countSupportFixedWing++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportFixedWingSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportFixedWingMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportFixedWingLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countSupportNaval++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportNavalSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportNavalMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportNavalLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countSupportSub++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportSubSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportSubMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportSubLarge++;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        countSupportHydrofoil++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
                            countSupportHydrofoilSmall++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                            countSupportHydrofoilMedium++;
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                            countSupportHydrofoilLarge++;
                        }
                    }
                }
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
        //endregion UnitList Processing

        //region Tree Description Assignment
        // Mech Nodes
        mechs.setUserObject("'Mechs: " + countMechs);

        battlemechs.setUserObject("BattleMechs: " + countBattleMechs);
        colossalMechs.setUserObject("Super Heavy: " + colossalMech);
        assaultMechs.setUserObject("Assault: " + assaultMech);
        heavyMechs.setUserObject("Heavy: " + heavyMech);
        mediumMechs.setUserObject("Medium: " + mediumMech);
        lightMechs.setUserObject("Light: " + lightMech);
        ultralightMechs.setUserObject("Ultralight: " + ultralightMech);

        omnis.setUserObject("OmniMechs: " + countOmniMechs);
        colossalOmniMechs.setUserObject("Super Heavy: " + colossalOmniMech);
        assaultOmniMechs.setUserObject("Assault: " + assaultOmniMech);
        heavyOmniMechs.setUserObject("Heavy: " + heavyOmniMech);
        mediumOmniMechs.setUserObject("Medium: " + mediumOmniMech);
        lightOmniMechs.setUserObject("Light: " + lightOmniMech);
        ultralightOmniMechs.setUserObject("Ultralight: " + ultralightOmniMech);

        // ASF Nodes
        ASF.setUserObject("'Aerospace Fighters: " + countASF);

        sASF.setUserObject("Standard Fighters: " + countStandardASF);
        sHeavyASF.setUserObject("Heavy: " + countHeavyASF);
        sMediumASF.setUserObject("Medium: " + countMediumASF);
        sLightASF.setUserObject("Light: " + countLightASF);

        oASF.setUserObject("OmniFighters: " + countOmniASF);
        oHeavyASF.setUserObject("Heavy: " + countOmniHeavyASF);
        oMediumASF.setUserObject("Medium: " + countOmniMediumASF);
        oLightASF.setUserObject("Light: " + countOmniLightASF);

        // Vee Nodes
        vees.setUserObject("Vehicles: " + countVees);

        sVees.setUserObject("Standard: " + countStandardVees);

        DefaultMutableTreeNode sTracked = new DefaultMutableTreeNode("Tracked: " + countTracked);
        sVees.add(sTracked);
        DefaultMutableTreeNode sTrackedColossal = new DefaultMutableTreeNode("Super Heavy: " + countTrackedColossal);
        sTracked.add(sTrackedColossal);
        DefaultMutableTreeNode sTrackedAssault = new DefaultMutableTreeNode("Assault: " + countTrackedAssault);
        sTracked.add(sTrackedAssault);
        DefaultMutableTreeNode sTrackedHeavy = new DefaultMutableTreeNode("Heavy: " + countTrackedHeavy);
        sTracked.add(sTrackedHeavy);
        DefaultMutableTreeNode sTrackedMedium = new DefaultMutableTreeNode("Medium: " + countTrackedMedium);
        sTracked.add(sTrackedMedium);
        DefaultMutableTreeNode sTrackedLight = new DefaultMutableTreeNode("Light: " + countTrackedLight);
        sTracked.add(sTrackedLight);

        DefaultMutableTreeNode sWheeled = new DefaultMutableTreeNode("Wheeled: " + countWheeled);
        sVees.add(sWheeled);
        DefaultMutableTreeNode sWheeledAssault = new DefaultMutableTreeNode("Assault: " + countWheeledAssault);
        sWheeled.add(sWheeledAssault);
        DefaultMutableTreeNode sWheeledHeavy = new DefaultMutableTreeNode("Heavy: " + countWheeledHeavy);
        sWheeled.add(sWheeledHeavy);
        DefaultMutableTreeNode sWheeledMedium = new DefaultMutableTreeNode("Medium: " + countWheeledMedium);
        sWheeled.add(sWheeledMedium);
        DefaultMutableTreeNode sWheeledLight = new DefaultMutableTreeNode("Light: " + countWheeledLight);
        sWheeled.add(sWheeledLight);

        DefaultMutableTreeNode sHover = new DefaultMutableTreeNode("Hover: " + countHover);
        sVees.add(sHover);
        DefaultMutableTreeNode sHoverMedium = new DefaultMutableTreeNode("Medium: " + countHoverMedium);
        sHover.add(sHoverMedium);
        DefaultMutableTreeNode sHoverLight = new DefaultMutableTreeNode("Light: " + countHoverLight);
        sHover.add(sHoverLight);

        DefaultMutableTreeNode sVTOL = new DefaultMutableTreeNode("VTOL: " + countVTOL);
        sVees.add(sVTOL);
        DefaultMutableTreeNode sVTOLLight = new DefaultMutableTreeNode("Light: " + countVTOLLight);
        sVTOL.add(sVTOLLight);

        DefaultMutableTreeNode sWiGE = new DefaultMutableTreeNode("WiGE: " + countWiGE);
        sVees.add(sWiGE);
        DefaultMutableTreeNode sWiGEAssault = new DefaultMutableTreeNode("Assault: " + countWiGEAssault);
        sWiGE.add(sWiGEAssault);
        DefaultMutableTreeNode sWiGEHeavy = new DefaultMutableTreeNode("Heavy: " + countWiGEHeavy);
        sWiGE.add(sWiGEHeavy);
        DefaultMutableTreeNode sWiGEMedium = new DefaultMutableTreeNode("Medium: " + countWiGEMedium);
        sWiGE.add(sWiGEMedium);
        DefaultMutableTreeNode sWiGELight = new DefaultMutableTreeNode("Light: " + countWiGELight);
        sWiGE.add(sWiGELight);

        DefaultMutableTreeNode sNaval = new DefaultMutableTreeNode("Naval: " + countNaval);
        sVees.add(sNaval);
        DefaultMutableTreeNode sNavalColossal = new DefaultMutableTreeNode("Super Heavy: " + countNavalColossal);
        sNaval.add(sNavalColossal);
        DefaultMutableTreeNode sNavalAssault = new DefaultMutableTreeNode("Assault: " + countNavalAssault);
        sNaval.add(sNavalAssault);
        DefaultMutableTreeNode sNavalHeavy = new DefaultMutableTreeNode("Heavy: " + countNavalHeavy);
        sNaval.add(sNavalHeavy);
        DefaultMutableTreeNode sNavalMedium = new DefaultMutableTreeNode("Medium: " + countNavalMedium);
        sNaval.add(sNavalMedium);
        DefaultMutableTreeNode sNavalLight = new DefaultMutableTreeNode("Light: " + countNavalLight);
        sNaval.add(sNavalLight);

        DefaultMutableTreeNode sSub = new DefaultMutableTreeNode("Sub: " + countSub);
        sVees.add(sSub);
        DefaultMutableTreeNode sSubColossal = new DefaultMutableTreeNode("Super Heavy: " + countSubColossal);
        sSub.add(sSubColossal);
        DefaultMutableTreeNode sSubAssault = new DefaultMutableTreeNode("Assault: " + countSubAssault);
        sSub.add(sSubAssault);
        DefaultMutableTreeNode sSubHeavy = new DefaultMutableTreeNode("Heavy: " + countSubHeavy);
        sSub.add(sSubHeavy);
        DefaultMutableTreeNode sSubMedium = new DefaultMutableTreeNode("Medium: " + countSubMedium);
        sSub.add(sSubMedium);
        DefaultMutableTreeNode sSubLight = new DefaultMutableTreeNode("Light: " + countSubLight);
        sSub.add(sSubLight);

        DefaultMutableTreeNode sHydrofoil = new DefaultMutableTreeNode("Hydrofoil: " + countHydrofoil);
        sVees.add(sHydrofoil);
        DefaultMutableTreeNode sHydrofoilAssault = new DefaultMutableTreeNode("Assault: " + countHydrofoilAssault);
        sHydrofoil.add(sHydrofoilAssault);
        DefaultMutableTreeNode sHydrofoilHeavy = new DefaultMutableTreeNode("Heavy: " + countHydrofoilHeavy);
        sHydrofoil.add(sHydrofoilHeavy);
        DefaultMutableTreeNode sHydrofoilMedium = new DefaultMutableTreeNode("Medium: " + countHydrofoilMedium);
        sHydrofoil.add(sHydrofoilMedium);
        DefaultMutableTreeNode sHydrofoilLight = new DefaultMutableTreeNode("Light: " + countHydrofoilLight);
        sHydrofoil.add(sHydrofoilLight);

        DefaultMutableTreeNode oVees = new DefaultMutableTreeNode("OmniVees: " + countOmniVees);
        vees.add(oVees);

        DefaultMutableTreeNode oTracked = new DefaultMutableTreeNode("Tracked: " + countOmniTracked);
        oVees.add(oTracked);
        DefaultMutableTreeNode oTrackedColossal = new DefaultMutableTreeNode("Super Heavy: " + countOmniTrackedColossal);
        oTracked.add(oTrackedColossal);
        DefaultMutableTreeNode oTrackedAssault = new DefaultMutableTreeNode("Assault: " + countOmniTrackedAssault);
        oTracked.add(oTrackedAssault);
        DefaultMutableTreeNode oTrackedHeavy = new DefaultMutableTreeNode("Heavy: " +countOmniTrackedHeavy);
        oTracked.add(oTrackedHeavy);
        DefaultMutableTreeNode oTrackedMedium = new DefaultMutableTreeNode("Medium: " + countOmniTrackedMedium);
        oTracked.add(oTrackedMedium);
        DefaultMutableTreeNode oTrackedLight = new DefaultMutableTreeNode("Light: " + countOmniTrackedLight);
        oTracked.add(oTrackedLight);

        DefaultMutableTreeNode oWheeled = new DefaultMutableTreeNode("Wheeled: " + countOmniWheeled);
        oVees.add(oWheeled);
        DefaultMutableTreeNode oWheeledAssault = new DefaultMutableTreeNode("Assault: " + countOmniWheeledAssault);
        oWheeled.add(oWheeledAssault);
        DefaultMutableTreeNode oWheeledHeavy = new DefaultMutableTreeNode("Heavy: " + countOmniWheeledHeavy);
        oWheeled.add(oWheeledHeavy);
        DefaultMutableTreeNode oWheeledMedium = new DefaultMutableTreeNode("Medium: " + countOmniWheeledMedium);
        oWheeled.add(oWheeledMedium);
        DefaultMutableTreeNode oWheeledLight = new DefaultMutableTreeNode("Light: " + countOmniWheeledLight);
        oWheeled.add(oWheeledLight);

        DefaultMutableTreeNode oHover = new DefaultMutableTreeNode("Hover: " + countOmniHover);
        oVees.add(oHover);
        DefaultMutableTreeNode oHoverMedium = new DefaultMutableTreeNode("Medium: " + countOmniHoverMedium);
        oHover.add(oHoverMedium);
        DefaultMutableTreeNode oHoverLight = new DefaultMutableTreeNode("Light: " + countOmniHoverLight);
        oHover.add(oHoverLight);

        DefaultMutableTreeNode oVTOL = new DefaultMutableTreeNode("VTOL: " + countOmniVTOL);
        oVees.add(oVTOL);
        DefaultMutableTreeNode oVTOLLight = new DefaultMutableTreeNode("Light: " + countOmniVTOLLight);
        oVTOL.add(oVTOLLight);

        DefaultMutableTreeNode oWiGE = new DefaultMutableTreeNode("WiGE: " + countOmniWiGE);
        oVees.add(oWiGE);
        DefaultMutableTreeNode oWiGEAssault = new DefaultMutableTreeNode("Assault: " + countOmniWiGEAssault);
        oWiGE.add(oWiGEAssault);
        DefaultMutableTreeNode oWiGEHeavy = new DefaultMutableTreeNode("Heavy: " + countOmniWiGEHeavy);
        oWiGE.add(oWiGEHeavy);
        DefaultMutableTreeNode oWiGEMedium = new DefaultMutableTreeNode("Medium: " + countOmniWiGEMedium);
        oWiGE.add(oWiGEMedium);
        DefaultMutableTreeNode oWiGELight = new DefaultMutableTreeNode("Light: " + countOmniWiGELight);
        oWiGE.add(oWiGELight);

        DefaultMutableTreeNode oNaval = new DefaultMutableTreeNode("Naval: " + countOmniNaval);
        oVees.add(oNaval);
        DefaultMutableTreeNode oNavalColossal = new DefaultMutableTreeNode("Super Heavy: " + countOmniNavalColossal);
        oNaval.add(oNavalColossal);
        DefaultMutableTreeNode oNavalAssault = new DefaultMutableTreeNode("Assault: " + countOmniNavalAssault);
        oNaval.add(oNavalAssault);
        DefaultMutableTreeNode oNavalHeavy = new DefaultMutableTreeNode("Heavy: " + countOmniNavalHeavy);
        oNaval.add(oNavalHeavy);
        DefaultMutableTreeNode oNavalMedium = new DefaultMutableTreeNode("Medium: " + countOmniNavalMedium);
        oNaval.add(oNavalMedium);
        DefaultMutableTreeNode oNavalLight = new DefaultMutableTreeNode("Light: " + countOmniNavalLight);
        oNaval.add(oNavalLight);

        DefaultMutableTreeNode oSub = new DefaultMutableTreeNode("Sub: " + countOmniSub);
        oVees.add(oSub);
        DefaultMutableTreeNode oSubColossal = new DefaultMutableTreeNode("Super Heavy: " + countOmniSubColossal);
        oSub.add(oSubColossal);
        DefaultMutableTreeNode oSubAssault = new DefaultMutableTreeNode("Assault: " + countOmniSubAssault);
        oSub.add(oSubAssault);
        DefaultMutableTreeNode oSubHeavy = new DefaultMutableTreeNode("Heavy: " + countOmniSubHeavy);
        oSub.add(oSubHeavy);
        DefaultMutableTreeNode oSubMedium = new DefaultMutableTreeNode("Medium: " + countOmniSubMedium);
        oSub.add(oSubMedium);
        DefaultMutableTreeNode oSubLight = new DefaultMutableTreeNode("Light: " + countOmniSubLight);
        oSub.add(oSubLight);

        DefaultMutableTreeNode oHydrofoil = new DefaultMutableTreeNode("Hydrofoil: " + countOmniHydrofoil);
        oVees.add(oHydrofoil);
        DefaultMutableTreeNode oHydrofoilAssault = new DefaultMutableTreeNode("Assault: " + countOmniHydrofoilAssault);
        oHydrofoil.add(oHydrofoilAssault);
        DefaultMutableTreeNode oHydrofoilHeavy = new DefaultMutableTreeNode("Heavy: " + countOmniHydrofoilHeavy);
        oHydrofoil.add(oHydrofoilHeavy);
        DefaultMutableTreeNode oHydrofoilMedium = new DefaultMutableTreeNode("Medium: " + countOmniHydrofoilMedium);
        oHydrofoil.add(oHydrofoilMedium);
        DefaultMutableTreeNode oHydrofoilLight = new DefaultMutableTreeNode("Light: " + countOmniHydrofoilLight);
        oHydrofoil.add(oHydrofoilLight);

        // Support Vee Nodes
        supportVees.setUserObject("Support Vehicles: " + countSupportVees);

        // Standard Support Vees
        DefaultMutableTreeNode sSupportVees = new DefaultMutableTreeNode("Standard: " + countSupportStandardVees);
        supportVees.add(sSupportVees);

        DefaultMutableTreeNode sSupportWheeled = new DefaultMutableTreeNode("Wheeled: " + countSupportWheeled);
        sSupportVees.add(sSupportWheeled);
        DefaultMutableTreeNode sSupportWheeledLarge = new DefaultMutableTreeNode("Large: " + countSupportWheeledLarge);
        sSupportWheeled.add(sSupportWheeledLarge);
        DefaultMutableTreeNode sSupportWheeledMedium = new DefaultMutableTreeNode("Medium: " + countSupportWheeledMedium);
        sSupportWheeled.add(sWheeledMedium);
        DefaultMutableTreeNode sSupportWheeledSmall = new DefaultMutableTreeNode("Small: " + countSupportWheeledSmall);
        sSupportWheeled.add(sSupportWheeledSmall);

        DefaultMutableTreeNode sSupportTracked = new DefaultMutableTreeNode("Tracked: " + countSupportTracked);
        sSupportVees.add(sSupportTracked);
        DefaultMutableTreeNode sSupportTrackedLarge = new DefaultMutableTreeNode("Large: " + countSupportTrackedLarge);
        sSupportTracked.add(sSupportTrackedLarge);
        DefaultMutableTreeNode sSupportTrackedMedium = new DefaultMutableTreeNode("Medium: " + countSupportTrackedMedium);
        sSupportTracked.add(sSupportTrackedMedium);
        DefaultMutableTreeNode sSupportTrackedSmall = new DefaultMutableTreeNode("Small: " + countSupportTrackedSmall);
        sSupportTracked.add(sSupportTrackedSmall);

        DefaultMutableTreeNode sSupportHover = new DefaultMutableTreeNode("Hover: " + countSupportHover);
        sSupportVees.add(sSupportHover);
        DefaultMutableTreeNode sSupportHoverLarge = new DefaultMutableTreeNode("Large: " + countSupportHoverLarge);
        sSupportHover.add(sSupportHoverLarge);
        DefaultMutableTreeNode sSupportHoverMedium = new DefaultMutableTreeNode("Medium: " + countSupportHoverMedium);
        sSupportHover.add(sSupportHoverMedium);
        DefaultMutableTreeNode sSupportHoverSmall = new DefaultMutableTreeNode("Small: " + countSupportHoverSmall);
        sSupportHover.add(sSupportHoverSmall);

        DefaultMutableTreeNode sSupportVTOL = new DefaultMutableTreeNode("VTOL: " + countSupportVTOL);
        sSupportVees.add(sSupportVTOL);
        DefaultMutableTreeNode sSupportVTOLLarge = new DefaultMutableTreeNode("Large: " + countSupportVTOLLarge);
        sSupportVTOL.add(sSupportVTOLLarge);
        DefaultMutableTreeNode sSupportVTOLMedium = new DefaultMutableTreeNode("Medium: " + countSupportVTOLMedium);
        sSupportVTOL.add(sSupportVTOLMedium);
        DefaultMutableTreeNode sSupportVTOLSmall = new DefaultMutableTreeNode("Small: " + countSupportVTOLSmall);
        sSupportVTOL.add(sSupportVTOLSmall);

        DefaultMutableTreeNode sSupportWiGE = new DefaultMutableTreeNode("WiGE: " + countSupportWiGE);
        sSupportVees.add(sSupportWiGE);
        DefaultMutableTreeNode sSupportWiGELarge = new DefaultMutableTreeNode("Large: " + countSupportWiGELarge);
        sSupportWiGE.add(sSupportWiGELarge);
        DefaultMutableTreeNode sSupportWiGEMedium = new DefaultMutableTreeNode("Medium: " + countSupportWiGEMedium);
        sSupportWiGE.add(sSupportWiGEMedium);
        DefaultMutableTreeNode sSupportWiGESmall = new DefaultMutableTreeNode("Small: " + countSupportWiGESmall);
        sSupportWiGE.add(sSupportWiGESmall);

        DefaultMutableTreeNode sSupportAirship = new DefaultMutableTreeNode("Airship: " + countSupportAirship);
        sSupportVees.add(sSupportAirship);
        DefaultMutableTreeNode sSupportAirshipLarge = new DefaultMutableTreeNode("Large: " + countSupportAirshipLarge);
        sSupportAirship.add(sSupportAirshipLarge);
        DefaultMutableTreeNode sSupportAirshipMedium = new DefaultMutableTreeNode("Medium: " + countSupportAirshipMedium);
        sSupportAirship.add(sSupportAirshipMedium);
        DefaultMutableTreeNode sSupportAirshipSmall = new DefaultMutableTreeNode("Small: " + countSupportAirshipSmall);
        sSupportAirship.add(sSupportAirshipSmall);

        DefaultMutableTreeNode sSupportFixedWing = new DefaultMutableTreeNode("Fixed-Wing: " + countSupportFixedWing);
        sSupportVees.add(sSupportFixedWing);
        DefaultMutableTreeNode sSupportFixedWingLarge = new DefaultMutableTreeNode("Large: " + countSupportFixedWingLarge);
        sSupportFixedWing.add(sSupportFixedWingLarge);
        DefaultMutableTreeNode sSupportFixedWingMedium = new DefaultMutableTreeNode("Medium: " + countSupportFixedWingMedium);
        sSupportFixedWing.add(sSupportFixedWingMedium);
        DefaultMutableTreeNode sSupportFixedWingSmall = new DefaultMutableTreeNode("Small: " + countSupportFixedWingSmall);
        sSupportFixedWing.add(sSupportFixedWingSmall);

        DefaultMutableTreeNode sSupportNaval = new DefaultMutableTreeNode("Naval: " + countSupportNaval);
        sSupportVees.add(sSupportNaval);
        DefaultMutableTreeNode sSupportNavalLarge = new DefaultMutableTreeNode("Large: " + countSupportNavalLarge);
        sSupportNaval.add(sSupportNavalLarge);
        DefaultMutableTreeNode sSupportNavalMedium = new DefaultMutableTreeNode("Medium: " + countSupportNavalMedium);
        sSupportNaval.add(sSupportNavalMedium);
        DefaultMutableTreeNode sSupportNavalSmall = new DefaultMutableTreeNode("Small: " + countSupportNavalSmall);
        sSupportNaval.add(sSupportNavalSmall);

        DefaultMutableTreeNode sSupportSub = new DefaultMutableTreeNode("Sub: " + countSupportSub);
        sSupportVees.add(sSupportSub);
        DefaultMutableTreeNode sSupportSubLarge = new DefaultMutableTreeNode("Large: " + countSupportSubLarge);
        sSupportSub.add(sSupportSubLarge);
        DefaultMutableTreeNode sSupportSubMedium = new DefaultMutableTreeNode("Medium: " + countSupportSubMedium);
        sSupportSub.add(sSupportSubMedium);
        DefaultMutableTreeNode sSupportSubSmall = new DefaultMutableTreeNode("Small: " + countSupportSubSmall);
        sSupportSub.add(sSupportSubSmall);

        DefaultMutableTreeNode sSupportHydrofoil = new DefaultMutableTreeNode("Hydrofoil: " + countSupportHydrofoil);
        sSupportVees.add(sSupportHydrofoil);
        DefaultMutableTreeNode sSupportHydrofoilLarge = new DefaultMutableTreeNode("Large: " + countSupportHydrofoilLarge);
        sSupportHydrofoil.add(sSupportHydrofoilLarge);
        DefaultMutableTreeNode sSupportHydrofoilMedium = new DefaultMutableTreeNode("Medium: " + countSupportHydrofoilMedium);
        sSupportHydrofoil.add(sSupportHydrofoilMedium);
        DefaultMutableTreeNode sSupportHydrofoilSmall = new DefaultMutableTreeNode("Small: " + countSupportHydrofoilSmall);
        sSupportHydrofoil.add(sSupportHydrofoilSmall);

        // Omni Support Vees
        DefaultMutableTreeNode oSupportVees = new DefaultMutableTreeNode("OmniVees: " + countSupportOmniVees);
        supportVees.add(oSupportVees);

        DefaultMutableTreeNode oSupportWheeled = new DefaultMutableTreeNode("Wheeled: " + countSupportOmniWheeled);
        oSupportVees.add(oSupportWheeled);
        DefaultMutableTreeNode oSupportWheeledLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniWheeledLarge);
        oSupportWheeled.add(oSupportWheeledLarge);
        DefaultMutableTreeNode oSupportWheeledMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniWheeledMedium);
        oSupportWheeled.add(oSupportWheeledMedium);
        DefaultMutableTreeNode oSupportWheeledSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniWheeledSmall);
        oSupportWheeled.add(oSupportWheeledSmall);

        DefaultMutableTreeNode oSupportTracked = new DefaultMutableTreeNode("Tracked: " + countSupportOmniTracked);
        oSupportVees.add(oSupportTracked);
        DefaultMutableTreeNode oSupportTrackedLarge = new DefaultMutableTreeNode("Large: " +countSupportOmniTrackedLarge);
        oSupportTracked.add(oSupportTrackedLarge);
        DefaultMutableTreeNode oSupportTrackedMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniTrackedMedium);
        oSupportTracked.add(oSupportTrackedMedium);
        DefaultMutableTreeNode oSupportTrackedSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniTrackedSmall);
        oSupportTracked.add(oSupportTrackedSmall);

        DefaultMutableTreeNode oSupportHover = new DefaultMutableTreeNode("Hover: " + countSupportOmniHover);
        oSupportVees.add(oSupportHover);
        DefaultMutableTreeNode oSupportHoverLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniHoverLarge);
        oSupportHover.add(oSupportHoverLarge);
        DefaultMutableTreeNode oSupportHoverMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniHoverMedium);
        oSupportHover.add(oSupportHoverMedium);
        DefaultMutableTreeNode oSupportHoverSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniHoverSmall);
        oSupportHover.add(oSupportHoverSmall);

        DefaultMutableTreeNode oSupportVTOL = new DefaultMutableTreeNode("VTOL: " + countSupportOmniVTOL);
        oSupportVees.add(oSupportVTOL);
        DefaultMutableTreeNode oSupportVTOLLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniVTOLLarge);
        oSupportVTOL.add(oSupportVTOLLarge);
        DefaultMutableTreeNode oSupportVTOLMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniVTOLMedium);
        oSupportVTOL.add(oSupportVTOLMedium);
        DefaultMutableTreeNode oSupportVTOLSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniVTOLSmall);
        oSupportVTOL.add(oSupportVTOLSmall);

        DefaultMutableTreeNode oSupportWiGE = new DefaultMutableTreeNode("WiGE: " + countSupportOmniWiGE);
        oSupportVees.add(oSupportWiGE);
        DefaultMutableTreeNode oSupportWiGELarge = new DefaultMutableTreeNode("Large: " + countSupportOmniWiGELarge);
        oSupportWiGE.add(oSupportWiGELarge);
        DefaultMutableTreeNode oSupportWiGEMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniWiGEMedium);
        oSupportWiGE.add(oSupportWiGEMedium);
        DefaultMutableTreeNode oSupportWiGESmall = new DefaultMutableTreeNode("Small: " + countSupportOmniWiGESmall);
        oSupportWiGE.add(oSupportWiGESmall);

        DefaultMutableTreeNode oSupportAirship = new DefaultMutableTreeNode("Airship: " + countSupportOmniAirship);
        oSupportVees.add(oSupportAirship);
        DefaultMutableTreeNode oSupportAirshipLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniAirshipLarge);
        oSupportAirship.add(oSupportAirshipLarge);
        DefaultMutableTreeNode oSupportAirshipMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniAirshipMedium);
        oSupportAirship.add(oSupportAirshipMedium);
        DefaultMutableTreeNode oSupportAirshipSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniAirshipSmall);
        oSupportAirship.add(oSupportAirshipSmall);

        DefaultMutableTreeNode oSupportFixedWing = new DefaultMutableTreeNode("Fixed-Wing: " + countSupportOmniFixedWing);
        oSupportVees.add(oSupportFixedWing);
        DefaultMutableTreeNode oSupportFixedWingLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniFixedWingLarge);
        oSupportFixedWing.add(oSupportFixedWingLarge);
        DefaultMutableTreeNode oSupportFixedWingMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniFixedWingMedium);
        oSupportFixedWing.add(oSupportFixedWingMedium);
        DefaultMutableTreeNode oSupportFixedWingSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniFixedWingSmall);
        oSupportFixedWing.add(oSupportFixedWingSmall);

        DefaultMutableTreeNode oSupportNaval = new DefaultMutableTreeNode("Naval: " + countSupportOmniNaval);
        oSupportVees.add(oSupportNaval);
        DefaultMutableTreeNode oSupportNavalLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniNavalLarge);
        oSupportNaval.add(oSupportNavalLarge);
        DefaultMutableTreeNode oSupportNavalMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniNavalMedium);
        oSupportNaval.add(oSupportNavalMedium);
        DefaultMutableTreeNode oSupportNavalSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniNavalSmall);
        oSupportNaval.add(oSupportNavalSmall);

        DefaultMutableTreeNode oSupportSub = new DefaultMutableTreeNode("Sub: " + countSupportOmniSub);
        oSupportVees.add(oSupportSub);
        DefaultMutableTreeNode oSupportSubLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniSubLarge);
        oSupportSub.add(oSupportSubLarge);
        DefaultMutableTreeNode oSupportSubMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniSubMedium);
        oSupportSub.add(oSupportSubMedium);
        DefaultMutableTreeNode oSupportSubSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniSubSmall);
        oSupportSub.add(oSupportSubSmall);

        DefaultMutableTreeNode oSupportHydrofoil = new DefaultMutableTreeNode("Hydrofoil: " + countSupportOmniHydrofoil);
        oSupportVees.add(oSupportHydrofoil);
        DefaultMutableTreeNode oSupportHydrofoilLarge = new DefaultMutableTreeNode("Large: " + countSupportOmniHydrofoilLarge);
        oSupportHydrofoil.add(oSupportHydrofoilLarge);
        DefaultMutableTreeNode oSupportHydrofoilMedium = new DefaultMutableTreeNode("Medium: " + countSupportOmniHydrofoilMedium);
        oSupportHydrofoil.add(oSupportHydrofoilMedium);
        DefaultMutableTreeNode oSupportHydrofoilSmall = new DefaultMutableTreeNode("Small: " + countSupportOmniHydrofoilSmall);
        oSupportHydrofoil.add(oSupportHydrofoilSmall);

        top.add(supportVees);

        // Infantry Nodes
        int allInfantry = (countInfantry + countBA);
        inf.setUserObject("Infantry: " + allInfantry);

        cInf.setUserObject("Conventional: " + countInfantry);
        infFoot.setUserObject("Foot Platoons: " + countFootInfantry);
        infMotorized.setUserObject("Motorized Platoons: " + countMotorizedInfantry);
        infJump.setUserObject("Jump Platoons: " + countJumpInfantry);
        infMechanized.setUserObject("Mechanized Platoons: " + countMechanizedInfantry);

        BAInf.setUserObject("Battle Armor: " + countBA);
        baAssault.setUserObject("Assault: " + countBAAssault);
        baHeavy.setUserObject("Heavy: " + countBAHeavy);
        baMedium.setUserObject("Medium: " + countBAMedium);
        baLight.setUserObject("Light: " + countBALight);
        baPAL.setUserObject("PAL/Exoskeleton: " + countBAPAL);

        // Conventional Fighters
        conv.setUserObject("Conventional Fighters: " + countConv);

        // ProtoMechs
        protos.setUserObject("ProtoMechs: " + countProtos);
        pAssault.setUserObject("Assault: " + countAssaultProtos);
        pHeavy.setUserObject("Heavy: " + countHeavyProtos);
        pMedium.setUserObject("Medium: " + countMediumProtos);
        pLight.setUserObject("Light: " + countLightProtos);

        // Turrets
        ge.setUserObject("Gun Emplacements: " + countGE);

        // Space
        space.setUserObject("Spacecraft: " + countSpace);

        sc.setUserObject("Small Craft: " + countSmallCraft);

        ds.setUserObject("DropShips: " + countDropships);
        lgds.setUserObject("Large DropShips: " + countLargeDS);
        mdds.setUserObject("Medium DropShips: " + countMediumDS);
        smds.setUserObject("Small DropShips: " + countSmallDS);

        js.setUserObject("JumpShips: " + countJumpships);

        ws.setUserObject("WarShips: " + countWarships);
        lgws.setUserObject("Large WarShips: " + countLargeWS);
        smws.setUserObject("Small WarShips: " + countSmallWS);

        //Space Stations
        spaceStation.setUserObject("Space Station: " + countSpaceStations);

        //endregion Tree Description Assignment

        for (Unit u : getCampaign().getUnits()) {
            Entity e = u.getEntity();
            if (e instanceof Mech) {
                //expandMechs = true;
                if (e.isOmni()) {
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                        ultralightOmniMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        lightOmniMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        mediumOmniMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        heavyOmniMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                        assaultOmniMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                        colossalOmniMechs.add(new DefaultMutableTreeNode(u.getName()));
                    }
                } else {
                    if (e.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                        ultralightMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        lightMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        mediumMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        heavyMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                        assaultMechs.add(new DefaultMutableTreeNode(u.getName()));
                    } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                        colossalMechs.add(new DefaultMutableTreeNode(u.getName()));
                    }
                }
            } else if (e instanceof ConvFighter) {
                conv.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof SpaceStation) {
                spaceStation.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof Warship) {
                //expandSpace = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_WAR) {
                    smws.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_WAR) {
                    lgws.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof Jumpship) {
                //expandSpace = true;
                js.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof Dropship) {
                //expandSpace = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_DROP) {
                    smds.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_DROP) {
                    mdds.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_DROP) {
                    lgds.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof SmallCraft) {
                //expandSpace = true;
                sc.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e instanceof Aero) {
                //expandASF = true;
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
                //expandProtos = true;
                if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    pLight.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    pMedium.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    pHeavy.add(new DefaultMutableTreeNode(u.getName()));
                } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    pAssault.add(new DefaultMutableTreeNode(u.getName()));
                }
            } else if (e instanceof GunEmplacement) {
                ge.add(new DefaultMutableTreeNode(u.getName()));
            } else if (e.isSupportVehicle()) {
                if (e.isOmni()) {
                    if (e instanceof VTOL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportVTOLLight.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportTrackedLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportTrackedMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSupportTrackedHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSupportTrackedAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            oSupportTrackedColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportWheeledLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportWheeledMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSupportWheeledHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSupportWheeledAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportHoverLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportHoverMedium.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportWiGELight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportWiGEMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSupportWiGEHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSupportWiGEAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportNavalLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportNavalMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSupportNavalHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSupportNavalAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            oSupportNavalColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportSubLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportSubMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSupportSubHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSupportSubAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            oSupportSubColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            oSupportHydrofoilLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            oSupportHydrofoilMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            oSupportHydrofoilHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            oSupportHydrofoilAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    }
                } else {
                    if (e instanceof VTOL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportVTOLLight.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportTrackedLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportTrackedMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSupportTrackedHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSupportTrackedAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            sSupportTrackedColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportWheeledLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportWheeledMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSupportWheeledHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSupportWheeledAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportHoverLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportHoverMedium.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportWiGELight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportWiGEMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSupportWiGEHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSupportWiGEAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportNavalLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportNavalMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSupportNavalHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSupportNavalAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            sSupportNavalColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportSubLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportSubMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSupportSubHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSupportSubAssault.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_COLOSSAL) {
                            sSupportSubColossal.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            sSupportHydrofoilLight.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            sSupportHydrofoilMedium.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                            sSupportHydrofoilHeavy.add(new DefaultMutableTreeNode(u.getName()));
                        } else if (e.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                            sSupportHydrofoilAssault.add(new DefaultMutableTreeNode(u.getName()));
                        }
                    }
                }
            } else if (e instanceof Tank) {
                //expandVees = true;
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
                //expandInfantry = true;
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
                //expandInfantry = true;
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
        /*
        final boolean expandMechsFinal = expandMechs;
        final boolean expandASFFinal = expandASF;
        final boolean expandVeesFinal = expandVees;
        final boolean expandInfantryFinal = expandInfantry;
        final boolean expandSpaceFinal = expandSpace;
        final boolean expandProtosFinal = expandProtos;
        */

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

        return overviewHangarTree;
    }

    public String getHangarTotals() {
    	int countInTransit = 0;
        int countPresent = 0;
        int countDamaged = 0;
        int countDeployed = 0;
        int total = 0;
        for (Unit u : getCampaign().getUnits()) {
            total++;
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

        return "Total Units: "      + total +
                "\n  Present: "     + countPresent +
                "\n  In Transit: "  + countInTransit +
                "\n  Damaged: "     + countDamaged +
                "\n  Deployed: "    + countDeployed;
    }

    public JTextPane getReport() {
        JTextPane txtReport = new JTextPane();
        txtReport.setMinimumSize(new Dimension(800, 500));
        txtReport.setFont(new Font("Courier New", Font.PLAIN, 12));

        txtReport.setAlignmentY(1.0f);

        txtReport.setText(getHangarTotals() + "\n\n\n");
        txtReport.insertComponent(getHangarTree());

        return txtReport;
    }
}

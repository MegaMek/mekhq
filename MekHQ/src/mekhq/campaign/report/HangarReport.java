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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import megamek.common.Warship;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

/**
 * @author Jay Lawson
 * @version %I% %G%
 * @since 3/12/2012
 */
public class HangarReport extends Report {
    //Private Variables
    private int totalNumberOfUnits = 0;
    private int countUnitsInTransit = 0;
    private int countUnitsPresent = 0;
    private int countDamagedUnits = 0;
    private int countDeployedUnits = 0;

    public HangarReport(Campaign c) {
        super(c);
    }

    public String getTitle() {
        return "Hangar Breakdown";
    }

    public JTree getHangarTree() {
        //region Variable Declarations
        //region BattleMechs
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
        //endregion BattleMechs

        //region ASF
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
        //endregion ASF

        //region Vehicles
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
        //endregion Vehicles

        //region Support Vehicles
        //boolean expandSupportVees = false;
        int countSupportVees = 0;

        int countSupportStandardVees = 0;

        int countSupportWheeled = 0;
        int countSupportWheeledLarge = 0;
        int countSupportWheeledMedium = 0;
        int countSupportWheeledSmall = 0;

        int countSupportTracked = 0;
        int countSupportTrackedLarge = 0;
        int countSupportTrackedMedium = 0;
        int countSupportTrackedSmall = 0;


        int countSupportHover = 0;
        int countSupportHoverLarge = 0;
        int countSupportHoverMedium = 0;
        int countSupportHoverSmall = 0;

        int countSupportVTOL = 0;
        int countSupportVTOLLarge = 0;
        int countSupportVTOLMedium = 0;
        int countSupportVTOLSmall = 0;

        int countSupportWiGE = 0;
        int countSupportWiGELarge = 0;
        int countSupportWiGEMedium = 0;
        int countSupportWiGESmall = 0;

        int countSupportAirship = 0;
        int countSupportAirshipLarge = 0;
        int countSupportAirshipMedium = 0;
        int countSupportAirshipSmall = 0;

        int countSupportFixedWing = 0;
        int countSupportFixedWingLarge = 0;
        int countSupportFixedWingMedium = 0;
        int countSupportFixedWingSmall = 0;

        int countSupportNaval = 0;
        int countSupportNavalLarge = 0;
        int countSupportNavalMedium = 0;
        int countSupportNavalSmall = 0;
        int countSupportSub = 0;
        int countSupportSubLarge = 0;
        int countSupportSubMedium = 0;
        int countSupportSubSmall = 0;
        int countSupportHydrofoil = 0;
        int countSupportHydrofoilLarge = 0;
        int countSupportHydrofoilMedium = 0;
        int countSupportHydrofoilSmall = 0;

        int countSupportSatellite = 0;
        int countSupportSatelliteLarge = 0;
        int countSupportSatelliteMedium = 0;
        int countSupportSatelliteSmall = 0;

        int countSupportRail = 0;
        int countSupportRailLarge = 0;
        int countSupportRailMedium = 0;
        int countSupportRailSmall = 0;

        int countSupportMaglev = 0;
        int countSupportMaglevLarge = 0;
        int countSupportMaglevMedium = 0;
        int countSupportMaglevSmall = 0;

        int countSupportOmniVees = 0;

        int countSupportOmniTracked = 0;
        int countSupportOmniTrackedLarge = 0;
        int countSupportOmniTrackedMedium = 0;
        int countSupportOmniTrackedSmall = 0;

        int countSupportOmniWheeled = 0;
        int countSupportOmniWheeledLarge = 0;
        int countSupportOmniWheeledMedium = 0;
        int countSupportOmniWheeledSmall = 0;

        int countSupportOmniHover = 0;
        int countSupportOmniHoverLarge = 0;
        int countSupportOmniHoverMedium = 0;
        int countSupportOmniHoverSmall = 0;

        int countSupportOmniVTOL = 0;
        int countSupportOmniVTOLLarge = 0;
        int countSupportOmniVTOLMedium = 0;
        int countSupportOmniVTOLSmall = 0;

        int countSupportOmniWiGE = 0;
        int countSupportOmniWiGELarge = 0;
        int countSupportOmniWiGEMedium = 0;
        int countSupportOmniWiGESmall = 0;

        int countSupportOmniAirship = 0;
        int countSupportOmniAirshipLarge = 0;
        int countSupportOmniAirshipMedium = 0;
        int countSupportOmniAirshipSmall = 0;

        int countSupportOmniFixedWing = 0;
        int countSupportOmniFixedWingLarge = 0;
        int countSupportOmniFixedWingMedium = 0;
        int countSupportOmniFixedWingSmall = 0;

        int countSupportOmniNaval = 0;
        int countSupportOmniNavalLarge = 0;
        int countSupportOmniNavalMedium = 0;
        int countSupportOmniNavalSmall = 0;
        int countSupportOmniSub = 0;
        int countSupportOmniSubLarge = 0;
        int countSupportOmniSubMedium = 0;
        int countSupportOmniSubSmall = 0;
        int countSupportOmniHydrofoil = 0;
        int countSupportOmniHydrofoilLarge = 0;
        int countSupportOmniHydrofoilMedium = 0;
        int countSupportOmniHydrofoilSmall = 0;

        int countSupportOmniSatellite = 0;
        int countSupportOmniSatelliteLarge = 0;
        int countSupportOmniSatelliteMedium = 0;
        int countSupportOmniSatelliteSmall = 0;

        int countSupportOmniRail = 0;
        int countSupportOmniRailLarge = 0;
        int countSupportOmniRailMedium = 0;
        int countSupportOmniRailSmall = 0;

        int countSupportOmniMaglev = 0;
        int countSupportOmniMaglevLarge = 0;
        int countSupportOmniMaglevMedium = 0;
        int countSupportOmniMaglevSmall = 0;
        //endregion Vehicles

        //region Battle Armor and Infantry
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
        //endregion Battle Armor and Infantry

        // Conventional Fighter
        int countConv = 0;

        //region ProtoMechs
        //boolean expandProtos = false;
        int countProtos = 0;
        int countAssaultProtos = 0;
        int countHeavyProtos = 0;
        int countMediumProtos = 0;
        int countLightProtos = 0;
        //endregion ProtoMechs

        // Turrets
        int countGE = 0;

        //region JumpShips, WarShips, DropShips, and SmallCraft
        //boolean expandSpace = false;
        int countSpace = 0;

        int countSmallCraft = 0;

        int countDropships = 0;
        int countLargeDS = 0;
        int countMediumDS = 0;
        int countSmallDS = 0;

        int countJumpShips = 0;

        int countWarShips = 0;
        int countLargeWS = 0;
        int countSmallWS = 0;
        //endregion JumpShips, WarShips, DropShips, and SmallCraft

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

        DefaultMutableTreeNode sSupportSatellite = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportSatellite);
        DefaultMutableTreeNode sSupportSatelliteLarge = new DefaultMutableTreeNode();
        sSupportSatellite.add(sSupportSatelliteLarge);
        DefaultMutableTreeNode sSupportSatelliteMedium = new DefaultMutableTreeNode();
        sSupportSatellite.add(sSupportSatelliteMedium);
        DefaultMutableTreeNode sSupportSatelliteSmall = new DefaultMutableTreeNode();
        sSupportSatellite.add(sSupportSatelliteSmall);

        DefaultMutableTreeNode sSupportRail = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportRail);
        DefaultMutableTreeNode sSupportRailLarge = new DefaultMutableTreeNode();
        sSupportRail.add(sSupportRailLarge);
        DefaultMutableTreeNode sSupportRailMedium = new DefaultMutableTreeNode();
        sSupportRail.add(sSupportRailMedium);
        DefaultMutableTreeNode sSupportRailSmall = new DefaultMutableTreeNode();
        sSupportRail.add(sSupportRailSmall);

        DefaultMutableTreeNode sSupportMaglev = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportMaglev);
        DefaultMutableTreeNode sSupportMaglevLarge = new DefaultMutableTreeNode();
        sSupportMaglev.add(sSupportMaglevLarge);
        DefaultMutableTreeNode sSupportMaglevMedium = new DefaultMutableTreeNode();
        sSupportMaglev.add(sSupportMaglevMedium);
        DefaultMutableTreeNode sSupportMaglevSmall = new DefaultMutableTreeNode();
        sSupportMaglev.add(sSupportMaglevSmall);


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

        DefaultMutableTreeNode oSupportSatellite = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportSatellite);
        DefaultMutableTreeNode oSupportSatelliteLarge = new DefaultMutableTreeNode();
        oSupportSatellite.add(oSupportSatelliteLarge);
        DefaultMutableTreeNode oSupportSatelliteMedium = new DefaultMutableTreeNode();
        oSupportSatellite.add(oSupportSatelliteMedium);
        DefaultMutableTreeNode oSupportSatelliteSmall = new DefaultMutableTreeNode();
        oSupportSatellite.add(oSupportSatelliteSmall);

        DefaultMutableTreeNode oSupportRail = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportRail);
        DefaultMutableTreeNode oSupportRailLarge = new DefaultMutableTreeNode();
        oSupportRail.add(oSupportRailLarge);
        DefaultMutableTreeNode oSupportRailMedium = new DefaultMutableTreeNode();
        oSupportRail.add(oSupportRailMedium);
        DefaultMutableTreeNode oSupportRailSmall = new DefaultMutableTreeNode();
        oSupportRail.add(oSupportRailSmall);

        DefaultMutableTreeNode oSupportMaglev = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportMaglev);
        DefaultMutableTreeNode oSupportMaglevLarge = new DefaultMutableTreeNode();
        oSupportMaglev.add(oSupportMaglevLarge);
        DefaultMutableTreeNode oSupportMaglevMedium = new DefaultMutableTreeNode();
        oSupportMaglev.add(oSupportMaglevMedium);
        DefaultMutableTreeNode oSupportMaglevSmall = new DefaultMutableTreeNode();
        oSupportMaglev.add(oSupportMaglevSmall);

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
        List<Unit> unitList = new ArrayList<>(getCampaign().getUnits());
        unitList.sort(Comparator.comparing(Unit::getName, new NaturalOrderComparator()));
        for (Unit u : unitList) {
            Entity e = u.getEntity();

            // Create general stats
            totalNumberOfUnits++;
            if (u.isPresent()) {
                countUnitsPresent++;
            } else {
                countUnitsInTransit++;
            }
            if (u.isDamaged()) {
                countDamagedUnits++;
            }
            if (u.isDeployed()) {
                countDeployedUnits++;
            }

            //Determine what type of unit, and add it to the proper subtree
            if (e instanceof Mech) {
                countMechs++;
                if (e.isOmni()) {
                    countOmniMechs++;
                    switch(e.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_COLOSSAL:
                            colossalOmniMech++;
                            colossalOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_ASSAULT:
                            assaultOmniMech++;
                            assaultOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_HEAVY:
                            heavyOmniMech++;
                            heavyOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_MEDIUM:
                            mediumOmniMech++;
                            mediumOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_LIGHT:
                            lightOmniMech++;
                            lightOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                            ultralightOmniMech++;
                            ultralightOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                    }
                } else {
                    countBattleMechs++;
                    switch (e.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_COLOSSAL:
                            colossalMech++;
                            colossalMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_ASSAULT:
                            assaultMech++;
                            assaultMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_HEAVY:
                            heavyMech++;
                            heavyMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_MEDIUM:
                            mediumMech++;
                            mediumMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_LIGHT:
                            lightMech++;
                            lightMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                            ultralightMech++;
                            ultralightMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                    }
                }
            } else if (e.isSupportVehicle()) {
                // this needs to be near or at the top because some of the units that
                // should have been caught by this will otherwise be selected for other unit types
                countSupportVees++;
                if (e.isOmni()) {
                    countSupportOmniVees++;
                    if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countSupportOmniWheeled++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniWheeledLarge++;
                                oSupportWheeledLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniWheeledMedium++;
                                oSupportWheeledMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniWheeledSmall++;
                                oSupportWheeledSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countSupportOmniTracked++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniTrackedLarge++;
                                oSupportTrackedLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniTrackedSmall++;
                                oSupportTrackedSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniTrackedMedium++;
                                oSupportTrackedMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countSupportOmniHover++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniHoverLarge++;
                                oSupportHoverLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniHoverMedium++;
                                oSupportHoverMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniHoverSmall++;
                                oSupportHoverSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.VTOL) {
                        countSupportOmniVTOL++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniVTOLLarge++;
                                oSupportVTOLLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniVTOLMedium++;
                                oSupportVTOLMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniVTOLSmall++;
                                oSupportVTOLSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countSupportOmniWiGE++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniWiGELarge++;
                                oSupportWiGELarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniWiGEMedium++;
                                oSupportWiGEMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniWiGESmall++;
                                oSupportWiGESmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.AIRSHIP) {
                        countSupportOmniAirship++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniAirshipLarge++;
                                oSupportAirshipLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniAirshipMedium++;
                                oSupportAirshipMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniAirshipSmall++;
                                oSupportAirshipSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.isFighter()) {
                        countSupportOmniFixedWing++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniFixedWingLarge++;
                                oSupportFixedWingLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniFixedWingMedium++;
                                oSupportFixedWingMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniFixedWingSmall++;
                                oSupportFixedWingSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countSupportOmniNaval++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniNavalLarge++;
                                oSupportNavalLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniNavalMedium++;
                                oSupportNavalMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniNavalSmall++;
                                oSupportNavalSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countSupportOmniSub++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniSubLarge++;
                                oSupportSubLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniSubMedium++;
                                oSupportSubMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniSubSmall++;
                                oSupportSubSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        countSupportOmniHydrofoil++;
                        switch(e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniHydrofoilLarge++;
                                oSupportHydrofoilLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniHydrofoilMedium++;
                                oSupportHydrofoilMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniHydrofoilSmall++;
                                oSupportHydrofoilSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.STATION_KEEPING) {
                        countSupportOmniSatellite++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniSatelliteLarge++;
                                oSupportSatelliteLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniSatelliteMedium++;
                                oSupportSatelliteMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniSatelliteSmall++;
                                oSupportSatelliteSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.RAIL) {
                        countSupportOmniRail++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniRailLarge++;
                                oSupportRailLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniRailMedium++;
                                oSupportRailMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniRailSmall++;
                                oSupportRailSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.MAGLEV) {
                        countSupportOmniMaglev++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniMaglevLarge++;
                                oSupportMaglevLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniMaglevMedium++;
                                oSupportMaglevMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniMaglevSmall++;
                                oSupportMaglevSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    }
                } else {
                    countSupportStandardVees++;
                    if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countSupportWheeled++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportWheeledLarge++;
                                sSupportWheeledLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportWheeledMedium++;
                                sSupportWheeledMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportWheeledSmall++;
                                sSupportWheeledSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countSupportTracked++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportTrackedLarge++;
                                sSupportTrackedLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportTrackedMedium++;
                                sSupportTrackedMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportTrackedSmall++;
                                sSupportTrackedSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countSupportHover++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportHoverLarge++;
                                sSupportHoverLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportHoverMedium++;
                                sSupportHoverMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportHoverSmall++;
                                sSupportHoverSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.VTOL) {
                        countSupportVTOL++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportVTOLLarge++;
                                sSupportVTOLLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportVTOLMedium++;
                                sSupportVTOLMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportVTOLSmall++;
                                sSupportVTOLSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countSupportWiGE++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportWiGELarge++;
                                sSupportWiGELarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportWiGEMedium++;
                                sSupportWiGEMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportWiGESmall++;
                                sSupportWiGESmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.AIRSHIP) {
                        countSupportAirship++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportAirshipLarge++;
                                sSupportAirshipLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportAirshipSmall++;
                                sSupportAirshipSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportAirshipMedium++;
                                sSupportAirshipMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.isFighter()) {
                        countSupportFixedWing++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportFixedWingLarge++;
                                sSupportFixedWingLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportFixedWingMedium++;
                                sSupportFixedWingMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportFixedWingSmall++;
                                sSupportFixedWingSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countSupportNaval++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportNavalSmall++;
                                sSupportNavalSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportNavalMedium++;
                                sSupportNavalMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportNavalLarge++;
                                sSupportNavalLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countSupportSub++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportSubLarge++;
                                sSupportSubLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportSubMedium++;
                                sSupportSubMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportSubSmall++;
                                sSupportSubSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        countSupportHydrofoil++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportHydrofoilLarge++;
                                sSupportHydrofoilLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportHydrofoilMedium++;
                                sSupportHydrofoilMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportHydrofoilSmall++;
                                sSupportHydrofoilSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.STATION_KEEPING) {
                        countSupportSatellite++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportSatelliteLarge++;
                                sSupportSatelliteLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportSatelliteMedium++;
                                sSupportSatelliteMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportSatelliteSmall++;
                                sSupportSatelliteSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.RAIL) {
                        countSupportRail++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportRailLarge++;
                                sSupportRailLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportRailMedium++;
                                sSupportRailMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportRailSmall++;
                                sSupportRailSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.MAGLEV) {
                        countSupportMaglev++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportMaglevLarge++;
                                sSupportMaglevLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportMaglevMedium++;
                                sSupportMaglevMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportMaglevSmall++;
                                sSupportMaglevSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    }
                }
            } else if (e instanceof ConvFighter) {
                countConv++;
                conv.add(new DefaultMutableTreeNode(createNodeName(u)));
            } else if (e instanceof SpaceStation) {
                countSpaceStations++;
                spaceStation.add(new DefaultMutableTreeNode(createNodeName(u)));
            } else if (e instanceof Warship) {
                countSpace++;
                countWarShips++;
                switch(e.getWeightClass()){
                    case EntityWeightClass.WEIGHT_LARGE_WAR:
                        countLargeWS++;
                        lgws.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_SMALL_WAR:
                        countSmallWS++;
                        smws.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                }
            } else if (e instanceof Jumpship) {
                countSpace++;
                countJumpShips++;
                js.add(new DefaultMutableTreeNode(createNodeName(u)));
            } else if (e instanceof Dropship) {
                countSpace++;
                countDropships++;
                switch(e.getWeightClass()) {
                    case EntityWeightClass.WEIGHT_LARGE_DROP:
                        countLargeDS++;
                        lgds.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_MEDIUM_DROP:
                        countMediumDS++;
                        mdds.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_SMALL_DROP:
                        countSmallDS++;
                        smds.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                }
            } else if (e instanceof SmallCraft) {
                countSpace++;
                countSmallCraft++;
                sc.add(new DefaultMutableTreeNode(createNodeName(u)));
            } else if (e instanceof Aero) {
                countASF++;
                if (e.isOmni()) {
                    countOmniASF++;
                    switch(e.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_HEAVY:
                            countOmniHeavyASF++;
                            oHeavyASF.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_MEDIUM:
                            countOmniMediumASF++;
                            oMediumASF.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_LIGHT:
                            countOmniLightASF++;
                            oLightASF.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                    }
                } else {
                    countStandardASF++;
                    switch(e.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_HEAVY:
                            countHeavyASF++;
                            sHeavyASF.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_MEDIUM:
                            countMediumASF++;
                            sMediumASF.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_LIGHT:
                            countLightASF++;
                            sLightASF.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                    }
                }
            } else if (e instanceof Protomech) {
                countProtos++;
                switch(e.getWeightClass()) {
                    case EntityWeightClass.WEIGHT_ASSAULT:
                        countAssaultProtos++;
                        pAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_HEAVY:
                        countHeavyProtos++;
                        pHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_MEDIUM:
                        countMediumProtos++;
                        pMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_LIGHT:
                        countLightProtos++;
                        pLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                }
            } else if (e instanceof GunEmplacement) {
                countGE++;
                ge.add(new DefaultMutableTreeNode(createNodeName(u)));
            } else if (e instanceof Tank) {
                countVees++;
                if (e.isOmni()) {
                    countOmniVees++;
                    if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countOmniTracked++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_COLOSSAL:
                                countOmniTrackedColossal++;
                                oTrackedColossal.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countOmniTrackedAssault++;
                                oTrackedAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countOmniTrackedHeavy++;
                                oTrackedHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniTrackedMedium++;
                                oTrackedMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniTrackedLight++;
                                oTrackedLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countOmniWheeled++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countOmniWheeledAssault++;
                                oWheeledAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countOmniWheeledHeavy++;
                                oWheeledHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniWheeledMedium++;
                                oWheeledMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniWheeledLight++;
                                oWheeledLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countOmniHover++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniHoverMedium++;
                                oHoverMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniHoverLight++;
                                oHoverLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.VTOL) {
                        countOmniVTOL++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countOmniVTOLLight++;
                            oVTOLLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countOmniWiGE++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countOmniWiGEAssault++;
                                oWiGEAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countOmniWiGEHeavy++;
                                oWiGEHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniWiGEMedium++;
                                oWiGEMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniWiGELight++;
                                oWiGELight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countOmniNaval++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_COLOSSAL:
                                countOmniNavalColossal++;
                                oNavalColossal.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countOmniNavalAssault++;
                                oNavalAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countOmniNavalHeavy++;
                                oNavalHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniNavalMedium++;
                                oNavalMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniNavalLight++;
                                oNavalLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countOmniSub++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_COLOSSAL:
                                countOmniSubColossal++;
                                oSubColossal.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countOmniSubAssault++;
                                oSubAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countOmniSubHeavy++;
                                oSubHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniSubMedium++;
                                oSubMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniSubLight++;
                                oSubLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        countOmniHydrofoil++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countOmniHydrofoilAssault++;
                                oHydrofoilAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countOmniHydrofoilHeavy++;
                                oHydrofoilHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countOmniHydrofoilMedium++;
                                oHydrofoilMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniHydrofoilLight++;
                                oHydrofoilLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    }
                } else {
                    countStandardVees++;
                    if (e.getMovementMode() == EntityMovementMode.TRACKED) {
                        countTracked++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_COLOSSAL:
                                countTrackedColossal++;
                                sTrackedColossal.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countTrackedAssault++;
                                sTrackedAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countTrackedHeavy++;
                                sTrackedHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countTrackedMedium++;
                                sTrackedMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countTrackedLight++;
                                sTrackedLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WHEELED) {
                        countWheeled++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countWheeledAssault++;
                                sWheeledAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countWheeledHeavy++;
                                sWheeledHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countWheeledMedium++;
                                sWheeledMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countWheeledLight++;
                                sWheeledLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HOVER) {
                        countHover++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countHoverMedium++;
                                sHoverMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countHoverLight++;
                                sHoverLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.VTOL) {
                        countVTOL++;
                        if (e.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                            countVTOLLight++;
                            sVTOLLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countWiGE++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countWiGEAssault++;
                                sWiGEAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countWiGEHeavy++;
                                sWiGEHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countWiGEMedium++;
                                sWiGEMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countWiGELight++;
                                sWiGELight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.NAVAL) {
                        countNaval++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_COLOSSAL:
                                countNavalColossal++;
                                sNavalColossal.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countNavalAssault++;
                                sNavalAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countNavalHeavy++;
                                sNavalHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countNavalMedium++;
                                sNavalMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countNavalLight++;
                                sNavalLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.SUBMARINE) {
                        countSub++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_COLOSSAL:
                                countSubColossal++;
                                sSubColossal.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countSubAssault++;
                                sSubAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countSubHeavy++;
                                sSubHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countSubMedium++;
                                sSubMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countSubLight++;
                                sSubLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.HYDROFOIL) {
                        countHydrofoil++;
                        switch(e.getWeightClass()){
                            case EntityWeightClass.WEIGHT_ASSAULT:
                                countHydrofoilAssault++;
                                sHydrofoilAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_HEAVY:
                                countHydrofoilHeavy++;
                                sHydrofoilHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM:
                                countHydrofoilMedium++;
                                sHydrofoilMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countHydrofoilLight++;
                                sHydrofoilLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    }
                }
            } else if (e instanceof BattleArmor) {
                countBA++;
                switch(e.getWeightClass()){
                    case EntityWeightClass.WEIGHT_ASSAULT:
                        countBAAssault++;
                        baAssault.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_HEAVY:
                        countBAHeavy++;
                        baHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_MEDIUM:
                        countBAMedium++;
                        baMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_LIGHT:
                        countBALight++;
                        baLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                    case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                        countBAPAL++;
                        baPAL.add(new DefaultMutableTreeNode(createNodeName(u)));
                        break;
                }
            } else if (e instanceof Infantry) {
                countInfantry++;
                if (((Infantry) e).isMechanized()) {
                    countMechanizedInfantry++;
                    infMechanized.add(new DefaultMutableTreeNode(createNodeName(u)));
                } else if (e.getMovementMode() == EntityMovementMode.INF_JUMP) {
                    countJumpInfantry++;
                    infJump.add(new DefaultMutableTreeNode(createNodeName(u)));
                } else if (e.getMovementMode() == EntityMovementMode.INF_LEG) {
                    countFootInfantry++;
                    infFoot.add(new DefaultMutableTreeNode(createNodeName(u)));
                } else if (e.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                    countMotorizedInfantry++;
                    infMotorized.add(new DefaultMutableTreeNode(createNodeName(u)));
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
        ASF.setUserObject("Aerospace Fighters: " + countASF);

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

        sTracked.setUserObject("Tracked: " + countTracked);
        sTrackedColossal.setUserObject("Super Heavy: " + countTrackedColossal);
        sTrackedAssault.setUserObject("Assault: " + countTrackedAssault);
        sTrackedHeavy.setUserObject("Heavy: " + countTrackedHeavy);
        sTrackedMedium.setUserObject("Medium: " + countTrackedMedium);
        sTrackedLight.setUserObject("Light: " + countTrackedLight);

        sWheeled.setUserObject("Wheeled: " + countWheeled);
        sWheeledAssault.setUserObject("Assault: " + countWheeledAssault);
        sWheeledHeavy.setUserObject("Heavy: " + countWheeledHeavy);
        sWheeledMedium.setUserObject("Medium: " + countWheeledMedium);
        sWheeledLight.setUserObject("Light: " + countWheeledLight);

        sHover.setUserObject("Hover: " + countHover);
        sHoverMedium.setUserObject("Medium: " + countHoverMedium);
        sHoverLight.setUserObject("Light: " + countHoverLight);

        sVTOL.setUserObject("VTOL: " + countVTOL);
        sVTOLLight.setUserObject("Light: " + countVTOLLight);

        sWiGE.setUserObject("WiGE: " + countWiGE);
        sWiGEAssault.setUserObject("Assault: " + countWiGEAssault);
        sWiGEHeavy.setUserObject("Heavy: " + countWiGEHeavy);
        sWiGEMedium.setUserObject("Medium: " + countWiGEMedium);
        sWiGELight.setUserObject("Light: " + countWiGELight);

        sNaval.setUserObject("Naval: " + countNaval);
        sNavalColossal.setUserObject("Super Heavy: " + countNavalColossal);
        sNavalAssault.setUserObject("Assault: " + countNavalAssault);
        sNavalHeavy.setUserObject("Heavy: " + countNavalHeavy);
        sNavalMedium.setUserObject("Medium: " + countNavalMedium);
        sNavalLight.setUserObject("Light: " + countNavalLight);

        sSub.setUserObject("Sub: " + countSub);
        sSubColossal.setUserObject("Super Heavy: " + countSubColossal);
        sSubAssault.setUserObject("Assault: " + countSubAssault);
        sSubHeavy.setUserObject("Heavy: " + countSubHeavy);
        sSubMedium.setUserObject("Medium: " + countSubMedium);
        sSubLight.setUserObject("Light: " + countSubLight);

        sHydrofoil.setUserObject("Hydrofoil: " + countHydrofoil);
        sHydrofoilAssault.setUserObject("Assault: " + countHydrofoilAssault);
        sHydrofoilHeavy.setUserObject("Heavy: " + countHydrofoilHeavy);
        sHydrofoilMedium.setUserObject("Medium: " + countHydrofoilMedium);
        sHydrofoilLight.setUserObject("Light: " + countHydrofoilLight);

        oVees.setUserObject("OmniVees: " + countOmniVees);

        oTracked.setUserObject("Tracked: " + countOmniTracked);
        oTrackedColossal.setUserObject("Super Heavy: " + countOmniTrackedColossal);
        oTrackedAssault.setUserObject("Assault: " + countOmniTrackedAssault);
        oTrackedHeavy.setUserObject("Heavy: " +countOmniTrackedHeavy);
        oTrackedMedium.setUserObject("Medium: " + countOmniTrackedMedium);
        oTrackedLight.setUserObject("Light: " + countOmniTrackedLight);

        oWheeled.setUserObject("Wheeled: " + countOmniWheeled);
        oWheeledAssault.setUserObject("Assault: " + countOmniWheeledAssault);
        oWheeledHeavy.setUserObject("Heavy: " + countOmniWheeledHeavy);
        oWheeledMedium.setUserObject("Medium: " + countOmniWheeledMedium);
        oWheeledLight.setUserObject("Light: " + countOmniWheeledLight);

        oHover.setUserObject("Hover: " + countOmniHover);
        oHoverMedium.setUserObject("Medium: " + countOmniHoverMedium);
        oHoverLight.setUserObject("Light: " + countOmniHoverLight);

        oVTOL.setUserObject("VTOL: " + countOmniVTOL);
        oVTOLLight.setUserObject("Light: " + countOmniVTOLLight);

        oWiGE.setUserObject("WiGE: " + countOmniWiGE);
        oWiGEAssault.setUserObject("Assault: " + countOmniWiGEAssault);
        oWiGEHeavy.setUserObject("Heavy: " + countOmniWiGEHeavy);
        oWiGEMedium.setUserObject("Medium: " + countOmniWiGEMedium);
        oWiGELight.setUserObject("Light: " + countOmniWiGELight);

        oNaval.setUserObject("Naval: " + countOmniNaval);
        oNavalColossal.setUserObject("Super Heavy: " + countOmniNavalColossal);
        oNavalAssault.setUserObject("Assault: " + countOmniNavalAssault);
        oNavalHeavy.setUserObject("Heavy: " + countOmniNavalHeavy);
        oNavalMedium.setUserObject("Medium: " + countOmniNavalMedium);
        oNavalLight.setUserObject("Light: " + countOmniNavalLight);

        oSub.setUserObject("Sub: " + countOmniSub);
        oSubColossal.setUserObject("Super Heavy: " + countOmniSubColossal);
        oSubAssault.setUserObject("Assault: " + countOmniSubAssault);
        oSubHeavy.setUserObject("Heavy: " + countOmniSubHeavy);
        oSubMedium.setUserObject("Medium: " + countOmniSubMedium);
        oSubLight.setUserObject("Light: " + countOmniSubLight);

        oHydrofoil.setUserObject("Hydrofoil: " + countOmniHydrofoil);
        oHydrofoilAssault.setUserObject("Assault: " + countOmniHydrofoilAssault);
        oHydrofoilHeavy.setUserObject("Heavy: " + countOmniHydrofoilHeavy);
        oHydrofoilMedium.setUserObject("Medium: " + countOmniHydrofoilMedium);
        oHydrofoilLight.setUserObject("Light: " + countOmniHydrofoilLight);

        // Support Vee Nodes
        supportVees.setUserObject("Support Vehicles: " + countSupportVees);

        // Standard Support Vees
        sSupportVees.setUserObject("Standard: " + countSupportStandardVees);

        sSupportWheeled.setUserObject("Wheeled: " + countSupportWheeled);
        sSupportWheeledLarge.setUserObject("Large: " + countSupportWheeledLarge);
        sSupportWheeledMedium.setUserObject("Medium: " + countSupportWheeledMedium);
        sSupportWheeledSmall.setUserObject("Small: " + countSupportWheeledSmall);

        sSupportTracked.setUserObject("Tracked: " + countSupportTracked);
        sSupportTrackedLarge.setUserObject("Large: " + countSupportTrackedLarge);
        sSupportTrackedMedium.setUserObject("Medium: " + countSupportTrackedMedium);
        sSupportTrackedSmall.setUserObject("Small: " + countSupportTrackedSmall);

        sSupportHover.setUserObject("Hover: " + countSupportHover);
        sSupportHoverLarge.setUserObject("Large: " + countSupportHoverLarge);
        sSupportHoverMedium.setUserObject("Medium: " + countSupportHoverMedium);
        sSupportHoverSmall.setUserObject("Small: " + countSupportHoverSmall);

        sSupportVTOL.setUserObject("VTOL: " + countSupportVTOL);
        sSupportVTOLLarge.setUserObject("Large: " + countSupportVTOLLarge);
        sSupportVTOLMedium.setUserObject("Medium: " + countSupportVTOLMedium);
        sSupportVTOLSmall.setUserObject("Small: " + countSupportVTOLSmall);

        sSupportWiGE.setUserObject("WiGE: " + countSupportWiGE);
        sSupportWiGELarge.setUserObject("Large: " + countSupportWiGELarge);
        sSupportWiGEMedium.setUserObject("Medium: " + countSupportWiGEMedium);
        sSupportWiGESmall.setUserObject("Small: " + countSupportWiGESmall);

        sSupportAirship.setUserObject("Airship: " + countSupportAirship);
        sSupportAirshipLarge.setUserObject("Large: " + countSupportAirshipLarge);
        sSupportAirshipMedium.setUserObject("Medium: " + countSupportAirshipMedium);
        sSupportAirshipSmall.setUserObject("Small: " + countSupportAirshipSmall);

        sSupportFixedWing.setUserObject("Fixed-Wing: " + countSupportFixedWing);
        sSupportFixedWingLarge.setUserObject("Large: " + countSupportFixedWingLarge);
        sSupportFixedWingMedium.setUserObject("Medium: " + countSupportFixedWingMedium);
        sSupportFixedWingSmall.setUserObject("Small: " + countSupportFixedWingSmall);

        sSupportNaval.setUserObject("Naval: " + countSupportNaval);
        sSupportNavalLarge.setUserObject("Large: " + countSupportNavalLarge);
        sSupportNavalMedium.setUserObject("Medium: " + countSupportNavalMedium);
        sSupportNavalSmall.setUserObject("Small: " + countSupportNavalSmall);

        sSupportSub.setUserObject("Sub: " + countSupportSub);
        sSupportSubLarge.setUserObject("Large: " + countSupportSubLarge);
        sSupportSubMedium.setUserObject("Medium: " + countSupportSubMedium);
        sSupportSubSmall.setUserObject("Small: " + countSupportSubSmall);

        sSupportHydrofoil.setUserObject("Hydrofoil: " + countSupportHydrofoil);
        sSupportHydrofoilLarge.setUserObject("Large: " + countSupportHydrofoilLarge);
        sSupportHydrofoilMedium.setUserObject("Medium: " + countSupportHydrofoilMedium);
        sSupportHydrofoilSmall.setUserObject("Small: " + countSupportHydrofoilSmall);

        sSupportSatellite.setUserObject("Satellite: " + countSupportSatellite);
        sSupportSatelliteLarge.setUserObject("Large: " + countSupportSatelliteLarge);
        sSupportSatelliteMedium.setUserObject("Medium: " + countSupportSatelliteMedium);
        sSupportSatelliteSmall.setUserObject("Small: " + countSupportSatelliteSmall);

        sSupportRail.setUserObject("Rail: " + countSupportRail);
        sSupportRailLarge.setUserObject("Large: " + countSupportRailLarge);
        sSupportRailMedium.setUserObject("Medium: " + countSupportRailMedium);
        sSupportRailSmall.setUserObject("Small: " + countSupportRailSmall);

        sSupportMaglev.setUserObject("Maglev: " + countSupportMaglev);
        sSupportMaglevLarge.setUserObject("Large: " + countSupportMaglevLarge);
        sSupportMaglevMedium.setUserObject("Medium: " + countSupportMaglevMedium);
        sSupportMaglevSmall.setUserObject("Small: " + countSupportMaglevSmall);

        // Omni Support Vees
        oSupportVees.setUserObject("OmniVees: " + countSupportOmniVees);

        oSupportWheeled.setUserObject("Wheeled: " + countSupportOmniWheeled);
        oSupportWheeledLarge.setUserObject("Large: " + countSupportOmniWheeledLarge);
        oSupportWheeledMedium.setUserObject("Medium: " + countSupportOmniWheeledMedium);
        oSupportWheeledSmall.setUserObject("Small: " + countSupportOmniWheeledSmall);

        oSupportTracked.setUserObject("Tracked: " + countSupportOmniTracked);
        oSupportTrackedLarge.setUserObject("Large: " +countSupportOmniTrackedLarge);
        oSupportTrackedMedium.setUserObject("Medium: " + countSupportOmniTrackedMedium);
        oSupportTrackedSmall.setUserObject("Small: " + countSupportOmniTrackedSmall);

        oSupportHover.setUserObject("Hover: " + countSupportOmniHover);
        oSupportHoverLarge.setUserObject("Large: " + countSupportOmniHoverLarge);
        oSupportHoverMedium.setUserObject("Medium: " + countSupportOmniHoverMedium);
        oSupportHoverSmall.setUserObject("Small: " + countSupportOmniHoverSmall);

        oSupportVTOL.setUserObject("VTOL: " + countSupportOmniVTOL);
        oSupportVTOLLarge.setUserObject("Large: " + countSupportOmniVTOLLarge);
        oSupportVTOLMedium.setUserObject("Medium: " + countSupportOmniVTOLMedium);
        oSupportVTOLSmall.setUserObject("Small: " + countSupportOmniVTOLSmall);

        oSupportWiGE.setUserObject("WiGE: " + countSupportOmniWiGE);
        oSupportWiGELarge.setUserObject("Large: " + countSupportOmniWiGELarge);
        oSupportWiGEMedium.setUserObject("Medium: " + countSupportOmniWiGEMedium);
        oSupportWiGESmall.setUserObject("Small: " + countSupportOmniWiGESmall);

        oSupportAirship.setUserObject("Airship: " + countSupportOmniAirship);
        oSupportAirshipLarge.setUserObject("Large: " + countSupportOmniAirshipLarge);
        oSupportAirshipMedium.setUserObject("Medium: " + countSupportOmniAirshipMedium);
        oSupportAirshipSmall.setUserObject("Small: " + countSupportOmniAirshipSmall);

        oSupportFixedWing.setUserObject("Fixed-Wing: " + countSupportOmniFixedWing);
        oSupportFixedWingLarge.setUserObject("Large: " + countSupportOmniFixedWingLarge);
        oSupportFixedWingMedium.setUserObject("Medium: " + countSupportOmniFixedWingMedium);
        oSupportFixedWingSmall.setUserObject("Small: " + countSupportOmniFixedWingSmall);

        oSupportNaval.setUserObject("Naval: " + countSupportOmniNaval);
        oSupportNavalLarge.setUserObject("Large: " + countSupportOmniNavalLarge);
        oSupportNavalMedium.setUserObject("Medium: " + countSupportOmniNavalMedium);
        oSupportNavalSmall.setUserObject("Small: " + countSupportOmniNavalSmall);

        oSupportSub.setUserObject("Sub: " + countSupportOmniSub);
        oSupportSubLarge.setUserObject("Large: " + countSupportOmniSubLarge);
        oSupportSubMedium.setUserObject("Medium: " + countSupportOmniSubMedium);
        oSupportSubSmall.setUserObject("Small: " + countSupportOmniSubSmall);

        oSupportHydrofoil.setUserObject("Hydrofoil: " + countSupportOmniHydrofoil);
        oSupportHydrofoilLarge.setUserObject("Large: " + countSupportOmniHydrofoilLarge);
        oSupportHydrofoilMedium.setUserObject("Medium: " + countSupportOmniHydrofoilMedium);
        oSupportHydrofoilSmall.setUserObject("Small: " + countSupportOmniHydrofoilSmall);

        oSupportSatellite.setUserObject("Satellite: " + countSupportOmniSatellite);
        oSupportSatelliteLarge.setUserObject("Large: " + countSupportOmniSatelliteLarge);
        oSupportSatelliteMedium.setUserObject("Medium: " + countSupportOmniSatelliteMedium);
        oSupportSatelliteSmall.setUserObject("Small: " + countSupportOmniSatelliteSmall);

        oSupportRail.setUserObject("Rail: " + countSupportOmniRail);
        oSupportRailLarge.setUserObject("Large: " + countSupportOmniRailLarge);
        oSupportRailMedium.setUserObject("Medium: " + countSupportOmniRailMedium);
        oSupportRailSmall.setUserObject("Small: " + countSupportOmniRailSmall);

        oSupportMaglev.setUserObject("Maglev: " + countSupportOmniMaglev);
        oSupportMaglevLarge.setUserObject("Large: " + countSupportOmniMaglevLarge);
        oSupportMaglevMedium.setUserObject("Medium: " + countSupportOmniMaglevMedium);
        oSupportMaglevSmall.setUserObject("Small: " + countSupportOmniMaglevSmall);

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

        js.setUserObject("JumpShips: " + countJumpShips);

        ws.setUserObject("WarShips: " + countWarShips);
        lgws.setUserObject("Large WarShips: " + countLargeWS);
        smws.setUserObject("Small WarShips: " + countSmallWS);

        //Space Stations
        spaceStation.setUserObject("Space Station: " + countSpaceStations);

        //endregion Tree Description Assignment

        overviewHangarTree.setSelectionPath(null);
        overviewHangarTree.expandPath(new TreePath(top.getPath()));

        // TODO : Implement an automatically expanding path for the paths that have units, although preferably with a
        // TODO : way to enable or disable it as desired
        /*
        // Reset our UI
        final boolean expandMechsFinal = expandMechs;
        final boolean expandASFFinal = expandASF;
        final boolean expandVeesFinal = expandVees;
        final boolean expandInfantryFinal = expandInfantry;
        final boolean expandSpaceFinal = expandSpace;
        final boolean expandProtosFinal = expandProtos;

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
        }
        */

        return overviewHangarTree;
    }

    private String createNodeName(Unit u) {
        String name = u.getName();

        if (u.isMothballed()) {
            name += " (Mothballed)";
        }

        return name;
    }

    public String getHangarTotals() {
        return String.format("Total Units: %d\n  Present: %d\n  In Transit: %d\n  Damaged: %d\n  Deployed: %d",
                totalNumberOfUnits, countUnitsPresent, countUnitsInTransit, countDamagedUnits, countDeployedUnits);
    }

    public JTextPane getReport() {
        JTextPane txtReport = new JTextPane();
        txtReport.setMinimumSize(new Dimension(800, 500));
        txtReport.setFont(new Font("Courier New", Font.PLAIN, 12));

        txtReport.setAlignmentY(1.0f);

        JTree hangarTree = getHangarTree();

        txtReport.setText(getHangarTotals() + "\n\n\n");
        txtReport.insertComponent(hangarTree);

        return txtReport;
    }
}

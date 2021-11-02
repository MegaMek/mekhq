/*
 * Copyright (c) 2013 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.report;

import megamek.common.*;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Jay Lawson
 * @since 3/12/2012
 */
public class HangarReport extends AbstractReport {
    //region Constructors
    public HangarReport(final Campaign campaign) {
        super(campaign);
    }
    //endregion Constructors

    public JTree getHangarTree() {
        //region Variable Declarations
        //region BattleMechs
        int countMechs = 0;

        int countBattleMechs = 0;
        int superHeavyMech = 0;
        int assaultMech = 0;
        int heavyMech = 0;
        int mediumMech = 0;
        int lightMech = 0;
        int ultralightMech = 0;

        int countOmniMechs = 0;
        int superHeavyOmniMech = 0;
        int assaultOmniMech = 0;
        int heavyOmniMech = 0;
        int mediumOmniMech = 0;
        int lightOmniMech = 0;
        int ultralightOmniMech = 0;

        int countIndustrialMechs = 0;
        int superHeavyIndustrialMech = 0;
        int assaultIndustrialMech = 0;
        int heavyIndustrialMech = 0;
        int mediumIndustrialMech = 0;
        int lightIndustrialMech = 0;
        //endregion BattleMechs

        //region ASF
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
        int countVees = 0;

        int countStandardVees = 0;

        int countTracked = 0;
        int countTrackedSuperHeavy = 0;
        int countTrackedAssault = 0;
        int countTrackedHeavy = 0;
        int countTrackedMedium = 0;
        int countTrackedLight = 0;

        int countWheeled = 0;
        int countWheeledSuperHeavy = 0;
        int countWheeledAssault = 0;
        int countWheeledHeavy = 0;
        int countWheeledMedium = 0;
        int countWheeledLight = 0;

        int countHover = 0;
        int countHoverSuperHeavy = 0;
        int countHoverMedium = 0;
        int countHoverLight = 0;

        int countVTOL = 0;
        int countVTOLSuperHeavy = 0;
        int countVTOLLight = 0;

        int countWiGE = 0;
        int countWiGESuperHeavy = 0;
        int countWiGEAssault = 0;
        int countWiGEHeavy = 0;
        int countWiGEMedium = 0;
        int countWiGELight = 0;

        int countNaval = 0;
        int countNavalSuperHeavy = 0;
        int countNavalAssault = 0;
        int countNavalHeavy = 0;
        int countNavalMedium = 0;
        int countNavalLight = 0;

        int countSub = 0;
        int countSubSuperHeavy = 0;
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
        int countOmniTrackedSuperHeavy = 0;
        int countOmniTrackedAssault = 0;
        int countOmniTrackedHeavy = 0;
        int countOmniTrackedMedium = 0;
        int countOmniTrackedLight = 0;

        int countOmniWheeled = 0;
        int countOmniWheeledSuperHeavy = 0;
        int countOmniWheeledAssault = 0;
        int countOmniWheeledHeavy = 0;
        int countOmniWheeledMedium = 0;
        int countOmniWheeledLight = 0;

        int countOmniHover = 0;
        int countOmniHoverSuperHeavy = 0;
        int countOmniHoverMedium = 0;
        int countOmniHoverLight = 0;

        int countOmniVTOL = 0;
        int countOmniVTOLSuperHeavy = 0;
        int countOmniVTOLLight = 0;

        int countOmniWiGE = 0;
        int countOmniWiGESuperHeavy = 0;
        int countOmniWiGEAssault = 0;
        int countOmniWiGEHeavy = 0;
        int countOmniWiGEMedium = 0;
        int countOmniWiGELight = 0;

        int countOmniNaval = 0;
        int countOmniNavalSuperHeavy = 0;
        int countOmniNavalAssault = 0;
        int countOmniNavalHeavy = 0;
        int countOmniNavalMedium = 0;
        int countOmniNavalLight = 0;

        int countOmniSub = 0;
        int countOmniSubSuperHeavy = 0;
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

        int countSupportMagLev = 0;
        int countSupportMagLevLarge = 0;
        int countSupportMagLevMedium = 0;
        int countSupportMagLevSmall = 0;

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

        int countSupportOmniMagLev = 0;
        int countSupportOmniMagLevLarge = 0;
        int countSupportOmniMagLevMedium = 0;
        int countSupportOmniMagLevSmall = 0;
        //endregion Vehicles

        //region Battle Armor and Infantry
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
        int countProtos = 0;
        int countAssaultProtos = 0;
        int countHeavyProtos = 0;
        int countMediumProtos = 0;
        int countLightProtos = 0;
        //endregion ProtoMechs

        // Turrets
        int countGE = 0;

        //region JumpShips, WarShips, DropShips, and SmallCraft
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
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(resources.getString("HangarReport.Origin"));
        JTree overviewHangarTree = new JTree(top);

        // Mech Nodes
        final DefaultMutableTreeNode mechs = new DefaultMutableTreeNode();

        DefaultMutableTreeNode battleMechs = new DefaultMutableTreeNode();
        mechs.add(battleMechs);
        DefaultMutableTreeNode superHeavyMechs = new DefaultMutableTreeNode();
        battleMechs.add(superHeavyMechs);
        DefaultMutableTreeNode assaultMechs = new DefaultMutableTreeNode();
        battleMechs.add(assaultMechs);
        DefaultMutableTreeNode heavyMechs = new DefaultMutableTreeNode();
        battleMechs.add(heavyMechs);
        DefaultMutableTreeNode mediumMechs = new DefaultMutableTreeNode();
        battleMechs.add(mediumMechs);
        DefaultMutableTreeNode lightMechs = new DefaultMutableTreeNode();
        battleMechs.add(lightMechs);
        DefaultMutableTreeNode ultralightMechs = new DefaultMutableTreeNode();
        battleMechs.add(ultralightMechs);

        DefaultMutableTreeNode omnis = new DefaultMutableTreeNode();
        mechs.add(omnis);
        DefaultMutableTreeNode superHeavyOmniMechs = new DefaultMutableTreeNode();
        omnis.add(superHeavyOmniMechs);
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

        DefaultMutableTreeNode industrialMechs = new DefaultMutableTreeNode();
        mechs.add(industrialMechs);
        DefaultMutableTreeNode superHeavyIndustrialMechs = new DefaultMutableTreeNode();
        industrialMechs.add(superHeavyIndustrialMechs);
        DefaultMutableTreeNode assaultIndustrialMechs = new DefaultMutableTreeNode();
        industrialMechs.add(assaultIndustrialMechs);
        DefaultMutableTreeNode heavyIndustrialMechs = new DefaultMutableTreeNode();
        industrialMechs.add(heavyIndustrialMechs);
        DefaultMutableTreeNode mediumIndustrialMechs = new DefaultMutableTreeNode();
        industrialMechs.add(mediumIndustrialMechs);
        DefaultMutableTreeNode lightIndustrialMechs = new DefaultMutableTreeNode();
        industrialMechs.add(lightIndustrialMechs);

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
        DefaultMutableTreeNode sTrackedSuperHeavy = new DefaultMutableTreeNode();
        sTracked.add(sTrackedSuperHeavy);
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
        DefaultMutableTreeNode sWheeledSuperHeavy = new DefaultMutableTreeNode();
        sWheeled.add(sWheeledSuperHeavy);
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
        DefaultMutableTreeNode sHoverSuperHeavy = new DefaultMutableTreeNode( );
        sHover.add(sHoverSuperHeavy);
        DefaultMutableTreeNode sHoverMedium = new DefaultMutableTreeNode( );
        sHover.add(sHoverMedium);
        DefaultMutableTreeNode sHoverLight = new DefaultMutableTreeNode();
        sHover.add(sHoverLight);

        DefaultMutableTreeNode sVTOL = new DefaultMutableTreeNode();
        sVees.add(sVTOL);
        DefaultMutableTreeNode sVTOLSuperHeavy = new DefaultMutableTreeNode();
        sVTOL.add(sVTOLSuperHeavy);
        DefaultMutableTreeNode sVTOLLight = new DefaultMutableTreeNode();
        sVTOL.add(sVTOLLight);

        DefaultMutableTreeNode sWiGE = new DefaultMutableTreeNode();
        sVees.add(sWiGE);
        DefaultMutableTreeNode sWiGESuperHeavy = new DefaultMutableTreeNode();
        sWiGE.add(sWiGESuperHeavy);
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
        DefaultMutableTreeNode sNavalSuperHeavy = new DefaultMutableTreeNode();
        sNaval.add(sNavalSuperHeavy);
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
        DefaultMutableTreeNode sSubSuperHeavy = new DefaultMutableTreeNode();
        sSub.add(sSubSuperHeavy);
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
        DefaultMutableTreeNode oTrackedSuperHeavy = new DefaultMutableTreeNode();
        oTracked.add(oTrackedSuperHeavy);
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
        DefaultMutableTreeNode oWheeledSuperHeavy = new DefaultMutableTreeNode();
        oWheeled.add(oWheeledSuperHeavy);
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
        DefaultMutableTreeNode oHoverSuperHeavy = new DefaultMutableTreeNode();
        oHover.add(oHoverSuperHeavy);
        DefaultMutableTreeNode oHoverMedium = new DefaultMutableTreeNode();
        oHover.add(oHoverMedium);
        DefaultMutableTreeNode oHoverLight = new DefaultMutableTreeNode();
        oHover.add(oHoverLight);

        DefaultMutableTreeNode oVTOL = new DefaultMutableTreeNode();
        oVees.add(oVTOL);
        DefaultMutableTreeNode oVTOLSuperHeavy = new DefaultMutableTreeNode();
        oVTOL.add(oVTOLSuperHeavy);
        DefaultMutableTreeNode oVTOLLight = new DefaultMutableTreeNode();
        oVTOL.add(oVTOLLight);

        DefaultMutableTreeNode oWiGE = new DefaultMutableTreeNode();
        oVees.add(oWiGE);
        DefaultMutableTreeNode oWiGESuperHeavy = new DefaultMutableTreeNode();
        oWiGE.add(oWiGESuperHeavy);
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
        DefaultMutableTreeNode oNavalSuperHeavy = new DefaultMutableTreeNode();
        oNaval.add(oNavalSuperHeavy);
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
        DefaultMutableTreeNode oSubSuperHeavy = new DefaultMutableTreeNode();
        oSub.add(oSubSuperHeavy);
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
        sSupportWheeled.add(sSupportWheeledMedium);
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

        DefaultMutableTreeNode sSupportMagLev = new DefaultMutableTreeNode();
        sSupportVees.add(sSupportMagLev);
        DefaultMutableTreeNode sSupportMagLevLarge = new DefaultMutableTreeNode();
        sSupportMagLev.add(sSupportMagLevLarge);
        DefaultMutableTreeNode sSupportMagLevMedium = new DefaultMutableTreeNode();
        sSupportMagLev.add(sSupportMagLevMedium);
        DefaultMutableTreeNode sSupportMagLevSmall = new DefaultMutableTreeNode();
        sSupportMagLev.add(sSupportMagLevSmall);


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

        DefaultMutableTreeNode oSupportMagLev = new DefaultMutableTreeNode();
        oSupportVees.add(oSupportMagLev);
        DefaultMutableTreeNode oSupportMagLevLarge = new DefaultMutableTreeNode();
        oSupportMagLev.add(oSupportMagLevLarge);
        DefaultMutableTreeNode oSupportMagLevMedium = new DefaultMutableTreeNode();
        oSupportMagLev.add(oSupportMagLevMedium);
        DefaultMutableTreeNode oSupportMagLevSmall = new DefaultMutableTreeNode();
        oSupportMagLev.add(oSupportMagLevSmall);

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
        List<Unit> unitList = new ArrayList<>(getCampaign().getHangar().getUnits());
        unitList.sort(Comparator.comparing(Unit::getName, new NaturalOrderComparator()));
        for (Unit u : unitList) {
            Entity e = u.getEntity();

            // Determine what type of unit, and add it to the proper subtree
            if (e instanceof Mech) {
                countMechs++;
                if (((Mech) e).isIndustrial()) {
                    countIndustrialMechs++;
                    switch (e.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                            superHeavyIndustrialMech++;
                            superHeavyIndustrialMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_ASSAULT:
                            assaultIndustrialMech++;
                            assaultIndustrialMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_HEAVY:
                            heavyIndustrialMech++;
                            heavyIndustrialMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_MEDIUM:
                            mediumIndustrialMech++;
                            mediumIndustrialMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                        case EntityWeightClass.WEIGHT_LIGHT:
                            lightIndustrialMech++;
                            lightIndustrialMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
                            break;
                    }
                } else if (e.isOmni()) {
                    countOmniMechs++;
                    switch (e.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                            superHeavyOmniMech++;
                            superHeavyOmniMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                            superHeavyMech++;
                            superHeavyMechs.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
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
                        countSupportOmniMagLev++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportOmniMagLevLarge++;
                                oSupportMagLevLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportOmniMagLevMedium++;
                                oSupportMagLevMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportOmniMagLevSmall++;
                                oSupportMagLevSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        countSupportMagLev++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_LARGE_SUPPORT:
                                countSupportMagLevLarge++;
                                sSupportMagLevLarge.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_MEDIUM_SUPPORT:
                                countSupportMagLevMedium++;
                                sSupportMagLevMedium.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_SMALL_SUPPORT:
                                countSupportMagLevSmall++;
                                sSupportMagLevSmall.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                switch (e.getWeightClass()) {
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
                switch (e.getWeightClass()) {
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
                    switch (e.getWeightClass()) {
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
                    switch (e.getWeightClass()) {
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
                switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniTrackedSuperHeavy++;
                                oTrackedSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniWheeledSuperHeavy++;
                                oWheeledSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniHoverSuperHeavy++;
                                oHoverSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniVTOLSuperHeavy++;
                                oVTOLSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countOmniVTOLLight++;
                                oVTOLLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countOmniWiGE++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniWiGESuperHeavy++;
                                oWiGESuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniNavalSuperHeavy++;
                                oNavalSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countOmniSubSuperHeavy++;
                                oSubSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countTrackedSuperHeavy++;
                                sTrackedSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countWheeledSuperHeavy++;
                                sWheeledSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countHoverSuperHeavy++;
                                sHoverSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countVTOLSuperHeavy++;
                                sVTOLSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                            case EntityWeightClass.WEIGHT_LIGHT:
                                countVTOLLight++;
                                sVTOLLight.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
                        }
                    } else if (e.getMovementMode() == EntityMovementMode.WIGE) {
                        countWiGE++;
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countWiGESuperHeavy++;
                                sWiGESuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
                                break;
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countNavalSuperHeavy++;
                                sNavalSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
                            case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                                countSubSuperHeavy++;
                                sSubSuperHeavy.add(new DefaultMutableTreeNode(createNodeName(u)));
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
                        switch (e.getWeightClass()) {
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
                switch (e.getWeightClass()) {
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
        mechs.setUserObject(resources.getString("HangarReport.Mechs") + " " + countMechs);

        battleMechs.setUserObject(resources.getString("HangarReport.BattleMechs") + " " + countBattleMechs);
        superHeavyMechs.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + superHeavyMech);
        assaultMechs.setUserObject(resources.getString("HangarReport.Assault") + " " + assaultMech);
        heavyMechs.setUserObject(resources.getString("HangarReport.Heavy") + " " + heavyMech);
        mediumMechs.setUserObject(resources.getString("HangarReport.Medium") + " " + mediumMech);
        lightMechs.setUserObject(resources.getString("HangarReport.Light") + " " + lightMech);
        ultralightMechs.setUserObject(resources.getString("HangarReport.Ultralight") + " " + ultralightMech);

        omnis.setUserObject(resources.getString("HangarReport.OmniMechs") + " " + countOmniMechs);
        superHeavyOmniMechs.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + superHeavyOmniMech);
        assaultOmniMechs.setUserObject(resources.getString("HangarReport.Assault") + " " + assaultOmniMech);
        heavyOmniMechs.setUserObject(resources.getString("HangarReport.Heavy") + " " + heavyOmniMech);
        mediumOmniMechs.setUserObject(resources.getString("HangarReport.Medium") + " " + mediumOmniMech);
        lightOmniMechs.setUserObject(resources.getString("HangarReport.Light") + " " + lightOmniMech);
        ultralightOmniMechs.setUserObject(resources.getString("HangarReport.Ultralight") + " " + ultralightOmniMech);

        industrialMechs.setUserObject(resources.getString("HangarReport.IndustrialMechs") + " " + countIndustrialMechs);
        superHeavyIndustrialMechs.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + superHeavyIndustrialMech);
        assaultIndustrialMechs.setUserObject(resources.getString("HangarReport.Assault") + " " + assaultIndustrialMech);
        heavyIndustrialMechs.setUserObject(resources.getString("HangarReport.Heavy") + " " + heavyIndustrialMech);
        mediumIndustrialMechs.setUserObject(resources.getString("HangarReport.Medium") + " " + mediumIndustrialMech);
        lightIndustrialMechs.setUserObject(resources.getString("HangarReport.Light") + " " + lightIndustrialMech);

        // ASF Nodes
        ASF.setUserObject(resources.getString("HangarReport.AerospaceFighters") + " " + countASF);

        sASF.setUserObject(resources.getString("HangarReport.StandardFighters") + " " + countStandardASF);
        sHeavyASF.setUserObject(resources.getString("HangarReport.Heavy") + " " + countHeavyASF);
        sMediumASF.setUserObject(resources.getString("HangarReport.Medium") + " " + countMediumASF);
        sLightASF.setUserObject(resources.getString("HangarReport.Light") + " " + countLightASF);

        oASF.setUserObject(resources.getString("HangarReport.OmniFighters") + " " + countOmniASF);
        oHeavyASF.setUserObject(resources.getString("HangarReport.Heavy") + " " + countOmniHeavyASF);
        oMediumASF.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniMediumASF);
        oLightASF.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniLightASF);

        // Vee Nodes
        vees.setUserObject(resources.getString("HangarReport.Vehicles") + " " + countVees);

        sVees.setUserObject(resources.getString("HangarReport.Standard") + " " + countStandardVees);

        sTracked.setUserObject(resources.getString("HangarReport.Tracked") + " " + countTracked);
        sTrackedSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countTrackedSuperHeavy);
        sTrackedAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countTrackedAssault);
        sTrackedHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countTrackedHeavy);
        sTrackedMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countTrackedMedium);
        sTrackedLight.setUserObject(resources.getString("HangarReport.Light") + " " + countTrackedLight);

        sWheeled.setUserObject(resources.getString("HangarReport.Wheeled") + " " + countWheeled);
        sWheeledSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countWheeledSuperHeavy);
        sWheeledAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countWheeledAssault);
        sWheeledHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countWheeledHeavy);
        sWheeledMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countWheeledMedium);
        sWheeledLight.setUserObject(resources.getString("HangarReport.Light") + " " + countWheeledLight);

        sHover.setUserObject(resources.getString("HangarReport.Hover") + " " + countHover);
        sHoverSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countHoverSuperHeavy);
        sHoverMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countHoverMedium);
        sHoverLight.setUserObject(resources.getString("HangarReport.Light") + " " + countHoverLight);

        sVTOL.setUserObject(resources.getString("HangarReport.VTOL") + " " + countVTOL);
        sVTOLSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countVTOLSuperHeavy);
        sVTOLLight.setUserObject(resources.getString("HangarReport.Light") + " " + countVTOLLight);

        sWiGE.setUserObject(resources.getString("HangarReport.WiGE") + " " + countWiGE);
        sWiGESuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countWiGESuperHeavy);
        sWiGEAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countWiGEAssault);
        sWiGEHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countWiGEHeavy);
        sWiGEMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countWiGEMedium);
        sWiGELight.setUserObject(resources.getString("HangarReport.Light") + " " + countWiGELight);

        sNaval.setUserObject(resources.getString("HangarReport.Naval") + " " + countNaval);
        sNavalSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countNavalSuperHeavy);
        sNavalAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countNavalAssault);
        sNavalHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countNavalHeavy);
        sNavalMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countNavalMedium);
        sNavalLight.setUserObject(resources.getString("HangarReport.Light") + " " + countNavalLight);

        sSub.setUserObject(resources.getString("HangarReport.Sub") + " " + countSub);
        sSubSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countSubSuperHeavy);
        sSubAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countSubAssault);
        sSubHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countSubHeavy);
        sSubMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSubMedium);
        sSubLight.setUserObject(resources.getString("HangarReport.Light") + " " + countSubLight);

        sHydrofoil.setUserObject(resources.getString("HangarReport.Hydrofoil") + " " + countHydrofoil);
        sHydrofoilAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countHydrofoilAssault);
        sHydrofoilHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countHydrofoilHeavy);
        sHydrofoilMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countHydrofoilMedium);
        sHydrofoilLight.setUserObject(resources.getString("HangarReport.Light") + " " + countHydrofoilLight);

        oVees.setUserObject(resources.getString("HangarReport.OmniVees") + " " + countOmniVees);

        oTracked.setUserObject(resources.getString("HangarReport.Tracked") + " " + countOmniTracked);
        oTrackedSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniTrackedSuperHeavy);
        oTrackedAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countOmniTrackedAssault);
        oTrackedHeavy.setUserObject(resources.getString("HangarReport.Heavy") +countOmniTrackedHeavy);
        oTrackedMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniTrackedMedium);
        oTrackedLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniTrackedLight);

        oWheeled.setUserObject(resources.getString("HangarReport.Wheeled") + " " + countOmniWheeled);
        oWheeledSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniWheeledSuperHeavy);
        oWheeledAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countOmniWheeledAssault);
        oWheeledHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countOmniWheeledHeavy);
        oWheeledMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniWheeledMedium);
        oWheeledLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniWheeledLight);

        oHover.setUserObject(resources.getString("HangarReport.Hover") + " " + countOmniHover);
        oHoverSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniHoverSuperHeavy);
        oHoverMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniHoverMedium);
        oHoverLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniHoverLight);

        oVTOL.setUserObject(resources.getString("HangarReport.VTOL") + " " + countOmniVTOL);
        oVTOLSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniVTOLSuperHeavy);
        oVTOLLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniVTOLLight);

        oWiGE.setUserObject(resources.getString("HangarReport.WiGE") + " " + countOmniWiGE);
        oWiGESuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniWiGESuperHeavy);
        oWiGEAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countOmniWiGEAssault);
        oWiGEHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countOmniWiGEHeavy);
        oWiGEMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniWiGEMedium);
        oWiGELight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniWiGELight);

        oNaval.setUserObject(resources.getString("HangarReport.Naval") + " " + countOmniNaval);
        oNavalSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniNavalSuperHeavy);
        oNavalAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countOmniNavalAssault);
        oNavalHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countOmniNavalHeavy);
        oNavalMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniNavalMedium);
        oNavalLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniNavalLight);

        oSub.setUserObject(resources.getString("HangarReport.Sub") + " " + countOmniSub);
        oSubSuperHeavy.setUserObject(resources.getString("HangarReport.SuperHeavy") + " " + countOmniSubSuperHeavy);
        oSubAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countOmniSubAssault);
        oSubHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countOmniSubHeavy);
        oSubMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniSubMedium);
        oSubLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniSubLight);

        oHydrofoil.setUserObject(resources.getString("HangarReport.Hydrofoil") + " " + countOmniHydrofoil);
        oHydrofoilAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countOmniHydrofoilAssault);
        oHydrofoilHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countOmniHydrofoilHeavy);
        oHydrofoilMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countOmniHydrofoilMedium);
        oHydrofoilLight.setUserObject(resources.getString("HangarReport.Light") + " " + countOmniHydrofoilLight);

        // Support Vee Nodes
        supportVees.setUserObject(resources.getString("HangarReport.SupportVehicles") + " " + countSupportVees);

        // Standard Support Vees
        sSupportVees.setUserObject(resources.getString("HangarReport.Standard") + " " + countSupportStandardVees);

        sSupportWheeled.setUserObject(resources.getString("HangarReport.Wheeled") + " " + countSupportWheeled);
        sSupportWheeledLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportWheeledLarge);
        sSupportWheeledMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportWheeledMedium);
        sSupportWheeledSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportWheeledSmall);

        sSupportTracked.setUserObject(resources.getString("HangarReport.Tracked") + " " + countSupportTracked);
        sSupportTrackedLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportTrackedLarge);
        sSupportTrackedMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportTrackedMedium);
        sSupportTrackedSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportTrackedSmall);

        sSupportHover.setUserObject(resources.getString("HangarReport.Hover") + " " + countSupportHover);
        sSupportHoverLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportHoverLarge);
        sSupportHoverMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportHoverMedium);
        sSupportHoverSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportHoverSmall);

        sSupportVTOL.setUserObject(resources.getString("HangarReport.VTOL") + " " + countSupportVTOL);
        sSupportVTOLLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportVTOLLarge);
        sSupportVTOLMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportVTOLMedium);
        sSupportVTOLSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportVTOLSmall);

        sSupportWiGE.setUserObject(resources.getString("HangarReport.WiGE") + " " + countSupportWiGE);
        sSupportWiGELarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportWiGELarge);
        sSupportWiGEMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportWiGEMedium);
        sSupportWiGESmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportWiGESmall);

        sSupportAirship.setUserObject(resources.getString("HangarReport.Airship") + " " + countSupportAirship);
        sSupportAirshipLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportAirshipLarge);
        sSupportAirshipMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportAirshipMedium);
        sSupportAirshipSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportAirshipSmall);

        sSupportFixedWing.setUserObject(resources.getString("HangarReport.FixedWing") + " " + countSupportFixedWing);
        sSupportFixedWingLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportFixedWingLarge);
        sSupportFixedWingMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportFixedWingMedium);
        sSupportFixedWingSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportFixedWingSmall);

        sSupportNaval.setUserObject(resources.getString("HangarReport.Naval") + " " + countSupportNaval);
        sSupportNavalLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportNavalLarge);
        sSupportNavalMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportNavalMedium);
        sSupportNavalSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportNavalSmall);

        sSupportSub.setUserObject(resources.getString("HangarReport.Sub") + " " + countSupportSub);
        sSupportSubLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportSubLarge);
        sSupportSubMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportSubMedium);
        sSupportSubSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportSubSmall);

        sSupportHydrofoil.setUserObject(resources.getString("HangarReport.Hydrofoil") + " " + countSupportHydrofoil);
        sSupportHydrofoilLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportHydrofoilLarge);
        sSupportHydrofoilMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportHydrofoilMedium);
        sSupportHydrofoilSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportHydrofoilSmall);

        sSupportSatellite.setUserObject(resources.getString("HangarReport.Satellite") + " " + countSupportSatellite);
        sSupportSatelliteLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportSatelliteLarge);
        sSupportSatelliteMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportSatelliteMedium);
        sSupportSatelliteSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportSatelliteSmall);

        sSupportRail.setUserObject(resources.getString("HangarReport.Rail") + " " + countSupportRail);
        sSupportRailLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportRailLarge);
        sSupportRailMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportRailMedium);
        sSupportRailSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportRailSmall);

        sSupportMagLev.setUserObject(resources.getString("HangarReport.MagLev") + " " + countSupportMagLev);
        sSupportMagLevLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportMagLevLarge);
        sSupportMagLevMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportMagLevMedium);
        sSupportMagLevSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportMagLevSmall);

        // Omni Support Vees
        oSupportVees.setUserObject(resources.getString("HangarReport.OmniVees") + " " + countSupportOmniVees);

        oSupportWheeled.setUserObject(resources.getString("HangarReport.Wheeled") + " " + countSupportOmniWheeled);
        oSupportWheeledLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniWheeledLarge);
        oSupportWheeledMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniWheeledMedium);
        oSupportWheeledSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniWheeledSmall);

        oSupportTracked.setUserObject(resources.getString("HangarReport.Tracked") + " " + countSupportOmniTracked);
        oSupportTrackedLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniTrackedLarge);
        oSupportTrackedMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniTrackedMedium);
        oSupportTrackedSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniTrackedSmall);

        oSupportHover.setUserObject(resources.getString("HangarReport.Hover") + " " + countSupportOmniHover);
        oSupportHoverLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniHoverLarge);
        oSupportHoverMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniHoverMedium);
        oSupportHoverSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniHoverSmall);

        oSupportVTOL.setUserObject(resources.getString("HangarReport.VTOL") + " " + countSupportOmniVTOL);
        oSupportVTOLLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniVTOLLarge);
        oSupportVTOLMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniVTOLMedium);
        oSupportVTOLSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniVTOLSmall);

        oSupportWiGE.setUserObject(resources.getString("HangarReport.WiGE") + " " + countSupportOmniWiGE);
        oSupportWiGELarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniWiGELarge);
        oSupportWiGEMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniWiGEMedium);
        oSupportWiGESmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniWiGESmall);

        oSupportAirship.setUserObject(resources.getString("HangarReport.Airship") + " " + countSupportOmniAirship);
        oSupportAirshipLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniAirshipLarge);
        oSupportAirshipMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniAirshipMedium);
        oSupportAirshipSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniAirshipSmall);

        oSupportFixedWing.setUserObject(resources.getString("HangarReport.FixedWing") + " " + countSupportOmniFixedWing);
        oSupportFixedWingLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniFixedWingLarge);
        oSupportFixedWingMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniFixedWingMedium);
        oSupportFixedWingSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniFixedWingSmall);

        oSupportNaval.setUserObject(resources.getString("HangarReport.Naval") + " " + countSupportOmniNaval);
        oSupportNavalLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniNavalLarge);
        oSupportNavalMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniNavalMedium);
        oSupportNavalSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniNavalSmall);

        oSupportSub.setUserObject(resources.getString("HangarReport.Sub") + " " + countSupportOmniSub);
        oSupportSubLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniSubLarge);
        oSupportSubMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniSubMedium);
        oSupportSubSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniSubSmall);

        oSupportHydrofoil.setUserObject(resources.getString("HangarReport.Hydrofoil") + " " + countSupportOmniHydrofoil);
        oSupportHydrofoilLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniHydrofoilLarge);
        oSupportHydrofoilMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniHydrofoilMedium);
        oSupportHydrofoilSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniHydrofoilSmall);

        oSupportSatellite.setUserObject(resources.getString("HangarReport.Satellite") + " " + countSupportOmniSatellite);
        oSupportSatelliteLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniSatelliteLarge);
        oSupportSatelliteMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniSatelliteMedium);
        oSupportSatelliteSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniSatelliteSmall);

        oSupportRail.setUserObject(resources.getString("HangarReport.Rail") + " " + countSupportOmniRail);
        oSupportRailLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniRailLarge);
        oSupportRailMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniRailMedium);
        oSupportRailSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniRailSmall);

        oSupportMagLev.setUserObject(resources.getString("HangarReport.MagLev") + " " + countSupportOmniMagLev);
        oSupportMagLevLarge.setUserObject(resources.getString("HangarReport.Large") + " " + countSupportOmniMagLevLarge);
        oSupportMagLevMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countSupportOmniMagLevMedium);
        oSupportMagLevSmall.setUserObject(resources.getString("HangarReport.Small") + " " + countSupportOmniMagLevSmall);

        // Infantry Nodes
        int allInfantry = (countInfantry + countBA);
        inf.setUserObject(resources.getString("HangarReport.Infantry") + " " + allInfantry);

        cInf.setUserObject(resources.getString("HangarReport.Conventional") + " " + countInfantry);
        infFoot.setUserObject(resources.getString("HangarReport.FootPlatoons") + " " + countFootInfantry);
        infMotorized.setUserObject(resources.getString("HangarReport.MotorizedPlatoons") + " " + countMotorizedInfantry);
        infJump.setUserObject(resources.getString("HangarReport.JumpPlatoons") + " " + countJumpInfantry);
        infMechanized.setUserObject(resources.getString("HangarReport.MechanizedPlatoons") + " " + countMechanizedInfantry);

        BAInf.setUserObject(resources.getString("HangarReport.BattleArmor") + " " + countBA);
        baAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countBAAssault);
        baHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countBAHeavy);
        baMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countBAMedium);
        baLight.setUserObject(resources.getString("HangarReport.Light") + " " + countBALight);
        baPAL.setUserObject(resources.getString("HangarReport.PAL_Exoskeleton") + " " + countBAPAL);

        // Conventional Fighters
        conv.setUserObject(resources.getString("HangarReport.ConventionalFighters") + " " + countConv);

        // ProtoMechs
        protos.setUserObject(resources.getString("HangarReport.ProtoMechs") + " " + countProtos);
        pAssault.setUserObject(resources.getString("HangarReport.Assault") + " " + countAssaultProtos);
        pHeavy.setUserObject(resources.getString("HangarReport.Heavy") + " " + countHeavyProtos);
        pMedium.setUserObject(resources.getString("HangarReport.Medium") + " " + countMediumProtos);
        pLight.setUserObject(resources.getString("HangarReport.Light") + " " + countLightProtos);

        // Turrets
        ge.setUserObject(resources.getString("HangarReport.GunEmplacements") + " " + countGE);

        // Space
        space.setUserObject(resources.getString("HangarReport.Spacecraft") + " " + countSpace);

        sc.setUserObject(resources.getString("HangarReport.SmallCraft") + " " + countSmallCraft);

        ds.setUserObject(resources.getString("HangarReport.DropShips") + " " + countDropships);
        lgds.setUserObject(resources.getString("HangarReport.Large") + " " + countLargeDS);
        mdds.setUserObject(resources.getString("HangarReport.Medium") + " " + countMediumDS);
        smds.setUserObject(resources.getString("HangarReport.Small") + " " + countSmallDS);

        js.setUserObject(resources.getString("HangarReport.JumpShips") + " " + countJumpShips);

        ws.setUserObject(resources.getString("HangarReport.WarShips") + " " + countWarShips);
        lgws.setUserObject(resources.getString("HangarReport.Large") + " " + countLargeWS);
        smws.setUserObject(resources.getString("HangarReport.Small") + " " + countSmallWS);

        //Space Stations
        spaceStation.setUserObject(resources.getString("HangarReport.SpaceStations") + " " + countSpaceStations);
        //endregion Tree Description Assignment

        overviewHangarTree.setSelectionPath(null);
        overviewHangarTree.expandPath(new TreePath(top.getPath()));

        return overviewHangarTree;
    }

    private String createNodeName(final Unit unit) {
        return unit.getName() + (unit.isMothballed() ? resources.getString("HangarReport.Mothballed") : "");
    }
}

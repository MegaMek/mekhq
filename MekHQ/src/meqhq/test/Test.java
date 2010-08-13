/*
 * Test.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package meqhq.test;

import common.EquipmentFactory;
import components.abPlaceable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import mekhq.campaign.Campaign;
import mekhq.campaign.Faction;
import mekhq.campaign.SSWLibHelper;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekEngine;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.SalvageItem;
import mekhq.campaign.work.WorkItem;

/**
 *
 * @author natit
 */
public class Test {

    public static final void main (String [] args) {
        try {
            testGetName();
        } catch (Exception ex) {
            System.out.println(ex);
        }

        System.out.println("End");
    }

    /**
     * Tests the SSWLibHelper.getAbPlaceableByName method
     */
    @SuppressWarnings("rawtypes")
	private static final void testGetName () {
        ArrayList<Integer> partTypes = new ArrayList<Integer>();
        ArrayList<Class> classes = new ArrayList<Class>();
        int nbMaxMechsToLoad = 1000;

        // With first 1000 mechs
        // EquipmentPart : 34 not ok
        classes.add(EquipmentPart.class);

        // Ammo : 24 not ok
        partTypes.add(new Integer(Part.PART_TYPE_AMMO));

        // Weapon : 8 not ok
        partTypes.add(new Integer(Part.PART_TYPE_WEAPON));

        // Equipment : 2 not ok
        partTypes.add(new Integer(Part.PART_TYPE_EQUIPMENT_PART));

        // Armor : 0 not ok
        classes.add(Armor.class);

        // MekActuator : 0 not ok
        classes.add(MekActuator.class);

        // MekEngine : 0 not ok
        classes.add(MekEngine.class);

        // MekGyro : 0 not ok
        classes.add(MekGyro.class);

        // MekLifeSupport : 0 not ok
        classes.add(MekLifeSupport.class);

        // MekLocation : 0 not ok
        classes.add(MekLocation.class);

        // MekSensor : 0 not ok
        classes.add(MekSensor.class);

        testGetName(nbMaxMechsToLoad, classes, partTypes);
    }

     /**
     * Tests the SSWLibHelper.getAbPlaceableByName method.
     * Called by testGetName()
     */
    @SuppressWarnings("rawtypes")
	private static final void testGetName (int nbMaxMechsToLoad, ArrayList<Class> classes, ArrayList<Integer> partTypes) {

        Logger.getLogger(Test.class.getName()).log(Level.OFF, null, "Logging off");

        Campaign campaign = new Campaign();

        MechSummary [] entities = MechSummaryCache.getInstance().getAllMechs();
        
        int cpt = 0;

        for (int i=0;i<entities.length;i++) {
            MechSummary ms = entities[i];
            
            Entity entity = null;
            try {
                entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                if (MechSummary.determineUnitType(entity).equals("Mek") && cpt<nbMaxMechsToLoad) {
                    campaign.addUnit(entity, true);
                    // System.out.println(entity.getDisplayName());
                    cpt++;
                }
            } catch (EntityLoadingException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        ArrayList<String> sswNamesNotOk = new ArrayList<String>();
        int nbNotOk = 0;

        for (Unit unit : campaign.getUnits()) {
            unit.setSalvage(true);
            int unitId = unit.getId();
            ArrayList<WorkItem> workItems = campaign.getAllTasksForUnit(unitId);
            for (WorkItem workItem : workItems) {
                if (workItem instanceof SalvageItem) {
                    SalvageItem salvageItem = (SalvageItem) workItem;
                    Part part = salvageItem.getPart();

                    boolean testPart = false;
                    if (part instanceof EquipmentPart && classes.contains(EquipmentPart.class) && partTypes.contains(new Integer ( ((EquipmentPart) part).getPartType() )))
                        testPart = true;
                    else if (!(part instanceof EquipmentPart) && classes.contains(part.getClass()))
                        testPart = true;

                    if (testPart) {
                        ArrayList<String> sswNames = part.getPotentialSSWNames(Faction.F_COMSTAR);

                        EquipmentFactory sswEquipmentFactory = Campaign.getSswEquipmentFactory();
                        components.Mech sswMech = Campaign.getSswMech();

                        abPlaceable placeable = null;
                        for (String sswName : sswNames) {
                            placeable = SSWLibHelper.getAbPlaceableByName(sswEquipmentFactory, sswMech, sswName);
                            if (placeable != null)
                                break;
                        }

                        if (placeable == null) {

                            boolean addToNotOk = false;
                            for (String sswName : sswNames) {
                                if (!sswNamesNotOk.contains(sswName)) {
                                    sswNamesNotOk.add(sswName);
                                    addToNotOk = true;
                                }
                            }

                            if (addToNotOk)
                                nbNotOk++;
                        } else {
                            System.out.println(sswNames.get(0) + "; " + placeable.ActualName() + "; " + placeable.GetAvailability().toString());
                        }
                    }
                }
            }
        }

        System.out.println("nb not ok : " + nbNotOk);
        for (String sswName : sswNamesNotOk) {
            System.out.println("sswName : " + sswName);
        }
    }
}

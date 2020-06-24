/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import mekhq.MekHQ;

/**
 * This class handles functionality related to loading and stratcon facility definitions.
 * @author NickAragua
 *
 */
public class StratconFacilityFactory {
    // loaded facility definitions
    private static List<StratconFacility> stratconFacilityList = new ArrayList<>();
    
    static {
        reloadFacilities();
    }
    
    public static void reloadFacilities() {
        stratconFacilityList.clear();
        
        // load dynamic scenarios
        StratconFacilityManifest facilityManifest = StratconFacilityManifest.Deserialize("./data/stratconfacilities/facilitymanifest.xml");
        
        // load user-specified scenario list
        StratconFacilityManifest userManifest = StratconFacilityManifest.Deserialize("./data/stratconfacilities/userfacilitymanifest.xml");
        
        if(facilityManifest != null) {
            loadFacilitiesFromManifest(facilityManifest);
        }
        
        if(userManifest != null) {
            loadFacilitiesFromManifest(userManifest);
        }
    }
    
    /**
     * Helper function that loads scenario templates from the given manifest.
     * @param manifest The manifest to process
     */
    private static void loadFacilitiesFromManifest(StratconFacilityManifest manifest) {
        if(manifest == null) {
            return;
        }
        
        for(String fileName : manifest.facilityFileNames) {
            String filePath = String.format("./data/stratconfacilities/%s", fileName.trim());
            
            try {
                StratconFacility facility = StratconFacility.Deserialize(filePath);
                
                if(facility != null) {
                    stratconFacilityList.add(facility);
                }
            } catch(Exception e) {
                MekHQ.getLogger().error(StratconFacilityFactory.class, "loadFacilitiesFromManifest", 
                        String.format("Error loading file: %s", filePath), e);
            }
        }
    }
    
    /**
     * Retrieves a random facility
     */
    public static StratconFacility getRandomFacility() {
        int facilityIndex = Compute.randomInt(stratconFacilityList.size());
        return (StratconFacility) stratconFacilityList.get(facilityIndex).clone();
    }
}

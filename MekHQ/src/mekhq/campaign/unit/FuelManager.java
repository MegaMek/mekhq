/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.unit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.Aero;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.material.Material;
import mekhq.campaign.material.MaterialStorage;
import mekhq.campaign.material.MaterialUsage;
import mekhq.campaign.material.Materials;

public final class FuelManager {
    private static boolean initialized = false;
    
    private static Material natural_gas = null;
    private static Material petrochemicals = null;
    private static Material hydrogen = null;
    private static Material alcohol = null;
    
    private static synchronized void initialize() {
        if(!initialized) {
            try(InputStream is = FuelManager.class.getClassLoader().getResourceAsStream("mekhq/campaign/unit/fuel.xml")) { //$NON-NLS-1$
                Materials.loadMaterials(is);
                
                natural_gas = Materials.getMaterial("FUEL_GAS"); //$NON-NLS-1$
                petrochemicals = Materials.getMaterial("FUEL_ICE"); //$NON-NLS-1$
                hydrogen = Materials.getMaterial("HYDROGEN"); //$NON-NLS-1$
                alcohol = Materials.getMaterial("FUEL_ALCOHOL"); //$NON-NLS-1$
                
                initialized = true;
            } catch (IOException ioex) {
                MekHQ.logError(ioex);
            }
        }
    }
    
    private static Entity getBaseEntity(Entity e) {
        MechSummaryCache msc = MechSummaryCache.getInstance();
        MechSummary summary = msc.getMech((e.getChassis() + " " + e.getModel()).trim()); //$NON-NLS-1$
        if(null != summary) {
            try {
                return new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
            } catch (EntityLoadingException elex) {
                MekHQ.logError(elex);
            }
        } else {
            MekHQ.logError("Couldn't load base entity definition for " + e.getChassis() + " " + e.getModel()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }
    
    // getFuelCapability(...) - return the totality of all fuel tanks inside the unit or list of units
    
    /** @return the internal fuel storage of the given entity */
    public static Collection<MaterialStorage> getFuelCapability(Entity e) {
        if(null == e) {
            return null;
        }
        initialize();
        List<MaterialStorage> result = new ArrayList<>();
        if(e instanceof Aero) {
            Aero base = (Aero) getBaseEntity(e);
            if(null != base) {
                Engine engine = base.getEngine();
                if(engine.hasFlag(Engine.COMBUSTION_ENGINE)) {
                    result.add(new MaterialStorage(petrochemicals, base.getFuelTonnage()));
                } else {
                    result.add(new MaterialStorage(hydrogen, base.getFuelTonnage()));
                }
            }
        }
        // TODO: Other unit types, where applicable
        return result;
    }
    
    /** @return the internal fuel storage of the given unit */
    public static Collection<MaterialStorage> getFuelCapability(Unit u) {
        return (null == u) ? null : getFuelCapability(u.getEntity());
    }
    
    /** @return the internal fuel storage of the given unit collection */
    public static Collection<MaterialStorage> getFuelCapability(Collection<Unit> units) {
        if(null == units) {
            return null;
        }
        
        Map<Material, MaterialStorage> result = new HashMap<>();
        for(Unit u : units) {
            if((null != u) && (null != u.getEntity())) {
                Collection<MaterialStorage> unitFuel = getFuelCapability(u.getEntity());
                for(MaterialStorage unitFuelStorage : unitFuel) {
                    MaterialStorage currentStorage = result.get(unitFuelStorage.getMaterial());
                    if(null == currentStorage) {
                        currentStorage = unitFuelStorage;
                        result.put(unitFuelStorage.getMaterial(), unitFuelStorage);
                    } else {
                        currentStorage.changeAmount(unitFuelStorage.getAmount());
                    }
                }
            }
        }
        
        return result.values();
    }
    
    /** @return the internal fuel storages of the given force and campaign */
    public static Collection<MaterialStorage> getFuelCapability(Campaign c, Force f) {
        if((null == c) || (null == f)) {
            return null;
        }
        
        Collection<UUID> uuids = f.getAllUnits();
        List<Unit> units = new ArrayList<>(uuids.size());
        for(UUID unitId : uuids) {
            Unit u = c.getUnit(unitId);
            if((null != u) && (null != u.getEntity())) {
                units.add(u);
            }
        }
        
        return getFuelCapability(units);
    }
    
    /** @return the internal fuel storages of all the player units <i>and</i> the player warehouse in the given campaign */
    public static Collection<MaterialStorage> getFuelCapability(Campaign c) {
        if(null == c) {
            return null;
        }
        
        return getFuelCapability(c.getUnits());
    }
    
    // getFuelTonnage(...) - return a specific unit's maximum fuel tonnage for the specific fuel
    
    /** @return the entity's maximum fuel tonnage for the specific fuel material */
    public static double getFuelTonnage(Entity e, Material m) {
        if((null == e) || (null == m) || !m.hasUsage(MaterialUsage.FUEL)) {
            return 0.0;
        }
        
        for(MaterialStorage storage : getFuelCapability(e)) {
            if(m.equals(storage.getMaterial())) {
                return storage.getAmount();
            }
        }
        
        return 0.0;
    }
    
    /** @return the entity's maximum fuel tonnage for the specific fuel material id */
    public static double getFuelTonnage(Entity e, String materialId) {
        return getFuelTonnage(e, Materials.getMaterial(materialId));
    }

    /** @return the unit's maximum fuel tonnage for the specific fuel material */
    public static double getFuelTonnage(Unit u, Material m) {
        if(null == u) {
            return 0.0;
        }
        
        return getFuelTonnage(u.getEntity(), m);
    }
    
    /** @return the unit's maximum fuel tonnage for the specific fuel material */
    public static double getFuelTonnage(Unit u, String materialId) {
        return getFuelTonnage(u, Materials.getMaterial(materialId));
    }
    
    /** @return the unit collection's maximum fuel tonnage for the specific fuel material id */
    public static double getFuelTonnage(Collection<Unit> units, Material m) {
        if((null == units) || units.isEmpty() || (null == m) || !m.hasUsage(MaterialUsage.FUEL)) {
            return 0.0;
        }
        
        double result = 0.0;
        for(Unit u : units) {
            result += getFuelTonnage(u, m);
        }
        
        return result;
    }
    
    /** @return the unit collection's maximum fuel tonnage for the specific fuel material id */
    public static double getFuelTonnage(Collection<Unit> units, String materialId) {
        return getFuelTonnage(units, Materials.getMaterial(materialId));
    }
    
    /** @return the given force's maximum fuel tonnage for the specific fuel material */
    public static double getFuelTonnage(Campaign c, Force f, Material m) {
        if((null == c) || (null == f) || (null == m) || !m.hasUsage(MaterialUsage.FUEL)) {
            return 0.0;
        }
        
        Collection<UUID> uuids = f.getAllUnits();
        List<Unit> units = new ArrayList<>(uuids.size());
        for(UUID unitId : f.getAllUnits()) {
            Unit u = c.getUnit(unitId);
            if((null != u) && (null != u.getEntity())) {
                units.add(u);
            }
        }
        
        return getFuelTonnage(units, m);
    }
    
    /** @return the given force's maximum fuel tonnage for the specific fuel material id */
    public static double getFuelTonnage(Campaign c, Force f, String materialId) {
        return getFuelTonnage(c, f, Materials.getMaterial(materialId));
    }
    
    // fill(...) - fill the specific units' internal fuel tank

    /** Fill the entity's internal tanks */
    public static void fill(Entity e) {
        if(null == e) {
            return;
        }
        initialize();
        if(e instanceof Aero) {
            Aero base = (Aero) getBaseEntity(e);
            if(null != base) {
                ((Aero) e).setFuelTonnage(base.getFuelTonnage());
            }
        }
    }
    
    /** Fill the unit's internal tanks */
    public static void fill(Unit u) {
        if(null != u) {
            fill(u.getEntity());
        }
    }
    
    /** Fill the collection of unit's internal tanks */
    public static void fill(Collection<Unit> units) {
        if(null != units) {
            for(Unit u : units) {
                fill(u);
            }
        }
    }
    
    /** Fill the specified force's internal tanks */
    public static void fill(Campaign c, Force f) {
        if((null != c) && (null != f)) {
            for(UUID unitId : f.getAllUnits()) {
                fill(c.getUnit(unitId));
            }
        }
    }
    
    /** Fill all units in the given campaign's internal tanks. */
    public static void fill(Campaign c) {
        if(null != c) {
            fill(c.getUnits());
        }
    }
}

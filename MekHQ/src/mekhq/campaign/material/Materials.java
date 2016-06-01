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
package mekhq.campaign.material;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;

import mekhq.MekHQ;

public class Materials {
    private final static Object LOADING_LOCK = new Object[0];
    
    private static final ConcurrentMap<String, Material> MATERIALS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<MaterialUsage, Set<Material>> MATERIAL_USAGES = new ConcurrentHashMap<>();
    
    // Marshaller / unmarshaller instances
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(MaterialList.class, Material.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            // unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch(JAXBException e) {
            MekHQ.logError(e);
        }
    }

    public static Material getMaterial(String id) {
        return MATERIALS.get(id);
    }
    
    public static Set<Material> getMaterials(MaterialUsage usage) {
        Set<Material> materials = MATERIAL_USAGES.get(usage);
        return (null != materials) ? new HashSet<>(materials) : Collections.<Material>emptySet();
    }
    
    public static boolean registerMaterial(Material mat) {
        boolean registered = (MATERIALS.putIfAbsent(mat.getId(), mat) == null);
        if(registered && !mat.isTemplate()) {
            for(MaterialUsage usage : mat.getUsage()) {
                Set<Material> materials = MATERIAL_USAGES.get(usage);
                if(null == materials) {
                    materials = new HashSet<Material>();
                    MATERIAL_USAGES.put(usage, materials);
                }
                materials.add(mat);
            }
        }
        return registered;
    }
    
    public static void loadMaterials(InputStream source) {
        synchronized(LOADING_LOCK) {
            // JAXB unmarshaller closes the stream it doesn't own. Bad JAXB. BAD.
            try(InputStream is = new FilterInputStream(source) {
                    @Override
                    public void close() { /* ignore */ }
                }) {
                // The materials register themselves during unmarshalling
                unmarshaller.unmarshal(new StreamSource(is), MaterialList.class).getValue();
            } catch (JAXBException e) {
                MekHQ.logError(e);
            } catch(IOException e) {
                MekHQ.logError(e);
            }
        }
    }
    
    public static void writeMaterial(Material mat, OutputStream os) {
        try {
            marshaller.marshal(mat, os);
        } catch(JAXBException e) {
            MekHQ.logError(e);
        }
    }
    
    @XmlRootElement(name="materials")
    private static class MaterialList {
        @XmlElement(name="material")
        private List<Material> materials;
    }
}

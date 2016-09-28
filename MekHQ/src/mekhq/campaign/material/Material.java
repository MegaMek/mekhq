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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import megamek.common.EquipmentType;
import mekhq.Utilities;
import mekhq.adapter.BooleanValueAdapter;
import mekhq.adapter.MaterialUsageSetAdapter;
import mekhq.adapter.StringListAdapter;
import mekhq.adapter.TechRatingAdapter;

@XmlRootElement(name="material")
@XmlAccessorType(XmlAccessType.FIELD)
public class Material {
    @XmlAttribute(required=true)
    private String id;
    private String name;
    @XmlJavaTypeAdapter(BooleanValueAdapter.class)
    private Boolean template;
    private String base;
    private String icon;
    /** Value of the raw material, per ton */
    private Double value;
    private Double valueMultiplier;
    @XmlElement(name="storage")
    private StorageType storageType;
    @XmlJavaTypeAdapter(MaterialUsageSetAdapter.class)
    private EnumSet<MaterialUsage> usage;
    @XmlElement(name="tr")
    @XmlJavaTypeAdapter(TechRatingAdapter.class)
    private Integer techRating;
    /** For custom use */
    @XmlJavaTypeAdapter(StringListAdapter.class)
    private List<String> tags;
    // TODO: Availability
    
    // Fluff
    @XmlElement(name="desc")
    private String description;
    
    private transient Material baseMaterial;
    
    /** @return the internal ID */
    public String getId() {
        return id;
    }
    
    /** @return the human-readable name */
    public String getName() {
        return name;
    }
    
    /** @return the file name if an icon representing this material, or <code>null</code> */
    public String getIcon() {
        return icon;
    }
    
    /** @return <code>true</code> if this is a material template for a set of materials */
    public boolean isTemplate() {
        return (null != template) && template.booleanValue();
    }
    
    /** @return the base value of a ton of the material, in C-bills */
    public double getValue() {
        double baseValue = 0.0;
        
        // Use our value if we have one, else pull the one from the base template
        if(null != value) {
            baseValue = value.doubleValue();
        } else if(null != baseMaterial) {
            baseValue = baseMaterial.getValue();
        }
        
        if(null != valueMultiplier) {
            baseValue *= valueMultiplier.doubleValue();
        }
        
        return baseValue;
    }
    
    /** @return the {@link StorageType} of the material, defaulting to <code>StorageType.BULK</code> */
    public StorageType getStorageType() {
        StorageType result = storageType;
        if((null == result) && (null != baseMaterial)) {
            result = baseMaterial.getStorageType();
        }
        return Utilities.nonNull(result, StorageType.BULK);
    }
    
    /** @return the set of (typical) {@link MaterialUsage} of the material */
    public EnumSet<MaterialUsage> getUsage() {
        EnumSet<MaterialUsage> result = usage;
        if((null == result) && (null != baseMaterial)) {
            result = baseMaterial.getUsage();
        }
        return Utilities.nonNull(result, EnumSet.noneOf(MaterialUsage.class));
    }
    
    /** @return <code>true</code> if the material is used for this specific {@link MaterialUsage} */
    public boolean hasUsage(MaterialUsage u) {
        return getUsage().contains(u);
    }

    /** @return the technology rating required to manufacture the material; defaults to <code>EquipmentType.RATING_C</code> */
    public int getTechRating() {
        return (null != techRating) ? techRating.intValue() : EquipmentType.RATING_C;
    }
    
    /** @return the free-form "tags" set for this material (for example {"pressure", "ultracool"} for HYDROGEN) */
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }
    
    /** @return the human-readable description of the material; defaults to an empty string */
    public String getDescription() {
        return Utilities.nonNull(description, "");
    }
    
    // JAXB marshalling support
    
    @SuppressWarnings("unused")
    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if(null == name) {
            name = id;
        }
        id = id.toUpperCase(Locale.ROOT);
        if(null == tags) {
            tags = new ArrayList<>();
        } else {
            List<String> filteredTags = new ArrayList<>(tags.size());
            for(String tag : tags) {
                if((null != tag) && !tag.isEmpty()) {
                    filteredTags.add(tag.toLowerCase(Locale.ROOT));
                }
            }
            tags = filteredTags;
        }
        if(null != base) {
            base = base.toUpperCase(Locale.ROOT);
            baseMaterial = Materials.getMaterial(base);
            if(null == baseMaterial) {
                throw new RuntimeException(String.format("Base material '%s' undefined.", base));
            }
            if(!baseMaterial.isTemplate()) {
                throw new RuntimeException(String.format("Base material '%s' is not a material template.", base));
            }
        }
        if(!Materials.registerMaterial(this)) {
            throw new RuntimeException(String.format("Material '%s' was already defined.", id));
        }
    }
    
    @SuppressWarnings("unused")
    private boolean beforeMarshal(Marshaller marshaller) {
        if(null != baseMaterial) {
            base = baseMaterial.id;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final Material other = (Material) obj;
        return Objects.equals(id, other.id);
    }
}

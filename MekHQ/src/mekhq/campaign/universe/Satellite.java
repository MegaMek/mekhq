/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.universe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is an object for satellites (i.e. moons around a planet)
 *
 * @author Aaron Gullickson (aarongullickson at gmail.com)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Satellite {
    @JsonProperty("name")
    private SourceableValue<String> name;
    @JsonProperty("size")
    private SourceableValue<String> size;
    @JsonProperty("icon")
    private String icon;

    public SourceableValue getSourcedName() {
        return name;
    }

    public String getSize() {
        if(null == size) {
            return "medium";
        }
        return size.getValue();
    }

    public String getIcon() {
        return icon;
    }
}

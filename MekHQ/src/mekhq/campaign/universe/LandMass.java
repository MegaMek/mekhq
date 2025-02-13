/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is an object for landMasses of a planet
 *
 * @author Aaron Gullickson (aarongullickson at gmail.com)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class LandMass {

    @JsonProperty("name")
    private SourceableValue<String> name;
    @JsonProperty("capital")
    private SourceableValue<String> capital;

    public String getDescription() {
        if(null != capital && null != name) {
            return name.getValue() + " (" + capital.getValue() + ")";
        } else if (null == name && null != capital) {
            return "(" + capital.getValue() + ")";
        } else if (null != name && null == capital) {
            return name.getValue();
        } else {
            return "Unknown";
        }
    }
}

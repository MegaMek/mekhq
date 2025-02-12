/*
 * SourceableValue.java
 *
 * Copyright (c) 2011-2025 - The MegaMek team. All Rights Reserved.
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
 * This generic class is designed to hold an absract value and a string
 * that indicates the source of that value. It is designed primarily to work
 * with planetary information, but could be used for other in-universe
 * sourceable information.
**/
@JsonIgnoreProperties(ignoreUnknown=true)
public class SourceableValue<T> {

    @JsonProperty("source")
    private String source;

    @JsonProperty("value")
    private T value;

    public String getSource() {
        return source;
    }

    public T getValue() {
        return value;
    }

    public boolean isCanon() {
        return (null != source);
    }

}

/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.parts.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

/**
 * Represents the type of one step of a refit operation, so that some refit operation classes can
 * cover more than one possible step each more easily.
 */
public enum RefitStepType {
    LEAVE("RefitStepType.LEAVE.text", "LEAVE"),
    REMOVE("RefitStepType.REMOVE.text", "REMOVE"),
    CHANGE_FACING("RefitStepType.CHANGE_FACING.text", "CHANGE_FACING"),
    REMOVE_ENGINE_SINKS("RefitStepType.REMOVE_ENGINE_SINKS.text", "REMOVE_ENGINE_SINKS"),
    ADD_ENGINE_SINKS("RefitStepType.ADD_ENGINE_SINKS.text", "ADD_ENGINE_SINKS"),
    ADD("RefitStepType.ADD.text", "ADD"),
    MOVE("RefitStepType.MOVE.text", "MOVE"),
    UNLOAD("RefitStepType.UNLOAD.text", "UNLOAD"),
    LOAD("RefitStepType.LOAD.text", "LOAD"),
    REMOVE_ARMOR("RefitStepType.REMOVE_ARMOR.text", "REMOVE_ARMOR"),
    ADD_ARMOR("RefitStepType.ADD_ARMOR.text", "ADD_ARMOR"),
    CHANGE_ARMOR_TYPE("RefitStepType.CHANGE_ARMOR_TYPE.text", "CHANGE_ARMOR_TYPE"),
    REMOVE_CASE("RefitStepType.REMOVE_CASE.text", "REMOVE_CASE"),
    ADD_CASE("RefitStepType.ADD_CASE.text", "ADD_CASE"),
    CHANGE_STRUCTURE_TYPE("RefitStepType.CHANGE_STRUCTURE_TYPE.text", "CHANGE_STRUCTURE_TYPE"),
    // Myomer Type and Structure Type live together
    DETACH_OMNIPOD("RefitStepType.DETATCH_OMNIPOD.text", "DETACH_OMNIPOD"),
    ATTACH_OMNIPOD("RefitStepType.ATTATCH_OMNIPOD.text", "ATTACH_OMNIPOD");
    
    private final String name;
    private final String xmlName;

    RefitStepType(final String name, final String xmlName) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.xmlName = xmlName;
    }
    
    /**
     * @return the translation's name for the refit step
     */
    public String toName() {
        return name;
    }
    
    /**
     * @return a name sutable for encoding this step in XML
     */
    public String toXmlName() {
        return xmlName;
    }

    /**
     * @param xmlName - the encoded name of a step
     * @return the corresponding RefitStepType
     * @throws IllegalArgumentException
     */
    public static RefitStepType fromXmlName(String xmlName) throws IllegalArgumentException {
        return switch (xmlName) {
            case "LEAVE" -> LEAVE;
            case "REMOVE" -> REMOVE;
            case "CHANGE_FACING" -> CHANGE_FACING;
            case "REMOVE_ENGINE_SINKS" -> REMOVE_ENGINE_SINKS;
            case "ADD_ENGINE_SINKS" -> ADD_ENGINE_SINKS;
            case "ADD" -> ADD;
            case "MOVE" -> MOVE;
            case "UNLOAD" -> UNLOAD;
            case "LOAD" -> LOAD;
            case "REMOVE_ARMOR" -> REMOVE_ARMOR;
            case "ADD_ARMOR" -> ADD_ARMOR;
            case "CHANGE_ARMOR_TYPE" -> CHANGE_ARMOR_TYPE;
            case "REMOVE_CASE" -> REMOVE_CASE;
            case "ADD_CASE" -> ADD_CASE;
            case "CHANGE_STRUCTURE_TYPE" -> CHANGE_STRUCTURE_TYPE;
            case "DETACH_OMNIPOD" -> DETACH_OMNIPOD;
            case "ATTACH_OMNIPOD" -> ATTACH_OMNIPOD;
            default -> throw new IllegalArgumentException("Invalid RefitStepType");
        };
    }
}
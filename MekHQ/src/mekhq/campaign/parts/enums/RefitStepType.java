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
    ADD("RefitStepType.ADD.text", "ADD"),
    MOVE("RefitStepType.MOVE.text", "MOVE"),
    CHANGE("RefitSTepType.CHANGE.text", "CHANGE"),
    UNLOAD("RefitStepType.UNLOAD.text", "UNLOAD"),
    LOAD("RefitStepType.LOAD.text", "LOAD"),
    MOVE_AMMO("RefitStepType.MOVE_AMMO.text", "MOVE_AMMO"),
    REMOVE_ARMOR("RefitStepType.REMOVE_ARMOR.text", "REMOVE_ARMOR"),
    ADD_ARMOR("RefitStepType.ADD_ARMOR.text", "ADD_ARMOR"),
    CHANGE_ARMOR_TYPE("RefitStepType.CHANGE_ARMOR_TYPE.text", "CHANGE_ARMOR_TYPE"),
    REMOVE_UNTRACKED_SINKS("RefitStepType.REMOVE_UNTRACKED_SINKS.text", "REMOVE_UNTRACKED_SINKS"),
    ADD_UNTRACKED_SINKS("RefitStepType.ADD_UNTRACKED_SINKS.text", "ADD_UNTRACKED_SINKS"),
    CHANGE_UNTRACKED_SINKS("RefitStepType.CHANGE_UNTRACKED_SINKS.text", "CHANGE_UNTRACKED_SINKS"),
    REMOVE_SCCS_SINKS("RefitStepType.REMOVE_SCCS_SINKS.text", "REMOVE_SCCS_SINKS"),
    ADD_SCCS_SINKS("RefitStepType.ADD_SCCS_SINKS.text", "ADD_SCCS_SINKS"),
    CHANGE_SCCS_SINKS("RefitStepType.CHANGE_SCCS_SINKS.text", "CHANGE_SCCS_SINKS"),
    REMOVE_CASE("RefitStepType.REMOVE_CASE.text", "REMOVE_CASE"),
    ADD_CASE("RefitStepType.ADD_CASE.text", "ADD_CASE"),
    REMOVE_TURRET("RefitStepType.REMOVE_TURRET.text", "REMOVE_TURRET"),
    ADD_TURRET("RefitStepType.ADD_TURRET.text", "ADD_TURRET"),
    CHANGE_STRUCTURE_TYPE("RefitStepType.CHANGE_STRUCTURE_TYPE.text", "CHANGE_STRUCTURE_TYPE"),
    // Myomer Type and Structure Type live together
    DETACH_OMNIPOD("RefitStepType.DETACH_OMNIPOD.text", "DETACH_OMNIPOD"),
    ATTACH_OMNIPOD("RefitStepType.ATTACH_OMNIPOD.text", "ATTACH_OMNIPOD"),
    MOVE_OMNIPOD("RefitStepType.MOVE_OMNIPOD.text", "MOVE_OMNIPOD"),
    DETACH_AMMOPOD("RefitStepType.DETACH_AMMOPOD.text", "DETACH_AMMOPOD"),
    ATTACH_AMMOPOD("RefitStepType.ATTACH_AMMOPOD.text", "ATTACH_AMMOPOD"),
    MOVE_AMMOPOD("RefitStepType.MOVE_AMMOPOD.text", "MOVE_AMMOPOD"),
    META("RefitStepType.META.text", "META"),
    ERROR("RefitStepType.ERROR.text", "ERROR");
    
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
     * @return is this an additive operation for purposes of bad location filtering?
     */
    public boolean isAdditive() {
        return switch (this) {
            case CHANGE_FACING -> true;
            case ADD -> true;
            case LOAD -> true;
            case MOVE -> true;
            case MOVE_AMMO -> true;
            case MOVE_OMNIPOD -> true;
            case ATTACH_OMNIPOD -> true;
            case ADD_ARMOR -> true;
            case CHANGE_ARMOR_TYPE -> true;
            case ADD_CASE -> true;

            default -> false;
        };
    }

    /**
     * Get the omni equivilant of this action.
     */
    public RefitStepType omnify() {
        return switch (this) {
            case REMOVE -> DETACH_OMNIPOD;
            case CHANGE_FACING -> MOVE_OMNIPOD;
            case MOVE -> MOVE_OMNIPOD;
            case ADD -> ATTACH_OMNIPOD;
            case UNLOAD -> DETACH_AMMOPOD;
            case LOAD -> ATTACH_AMMOPOD;
            case MOVE_AMMO -> MOVE_AMMOPOD;
            case ADD_CASE -> ATTACH_OMNIPOD;
            case REMOVE_CASE -> DETACH_OMNIPOD;
            default -> this;
        };
    }

    public boolean isOmniType() {
        return switch (this) {
            case ATTACH_OMNIPOD, DETACH_OMNIPOD, MOVE_OMNIPOD -> true;
            case ATTACH_AMMOPOD, DETACH_AMMOPOD, MOVE_AMMOPOD -> true;
            default -> false;
        };
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
            case "ADD" -> ADD;
            case "MOVE" -> MOVE;
            case "CHANGE" -> CHANGE;
            case "UNLOAD" -> UNLOAD;
            case "LOAD" -> LOAD;
            case "MOVE_AMMO" -> MOVE_AMMO;
            case "REMOVE_ARMOR" -> REMOVE_ARMOR;
            case "ADD_ARMOR" -> ADD_ARMOR;
            case "CHANGE_ARMOR_TYPE" -> CHANGE_ARMOR_TYPE;
            case "REMOVE_UNTRACKED_SINKS" -> REMOVE_UNTRACKED_SINKS;
            case "ADD_UNTRACKED_SINKS" -> ADD_UNTRACKED_SINKS;
            case "CHANGE_UNTRACKED_SINKS" -> CHANGE_UNTRACKED_SINKS;
            case "REMOVE_SCCS_SINKS" -> REMOVE_SCCS_SINKS;
            case "ADD_SCCS_SINKS" -> ADD_SCCS_SINKS;
            case "CHANGE_SCCS_SINKS" -> CHANGE_SCCS_SINKS;
            case "REMOVE_CASE" -> REMOVE_CASE;
            case "ADD_CASE" -> ADD_CASE;
            case "REMOVE_TURRET" -> REMOVE_TURRET;
            case "ADD_TURRET" -> ADD_TURRET;
            case "CHANGE_STRUCTURE_TYPE" -> CHANGE_STRUCTURE_TYPE;
            case "DETACH_OMNIPOD" -> DETACH_OMNIPOD;
            case "ATTACH_OMNIPOD" -> ATTACH_OMNIPOD;
            case "MOVE_OMNIPOD" -> MOVE_OMNIPOD;
            case "DETACH_AMMOPOD" -> DETACH_AMMOPOD;
            case "ATTACH_AMMOPOD" -> ATTACH_AMMOPOD;
            case "MOVE_AMMOPOD" -> MOVE_AMMOPOD;
            case "META" -> META;
            case "ERROR" -> ERROR;
            default -> throw new IllegalArgumentException("Invalid RefitStepType");
        };
    }
}
/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved
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
package mekhq.gui.enums;

import megamek.common.util.EncodeControl;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public enum LayeredForceIcon {
    //region Enum Declarations
    TYPE("LayeredForceIcon.types.text", "Pieces/Type/", "tableTypes", ListSelectionModel.SINGLE_SELECTION),
    FORMATION("LayeredForceIcon.formations.text", "Pieces/Formations/", "tableFormations", ListSelectionModel.SINGLE_SELECTION),
    ADJUSTMENT("LayeredForceIcon.adjustments.text", "Pieces/Adjustments/", "tableAdjustments", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    ALPHANUMERIC("LayeredForceIcon.alphanumerics.text", "Pieces/Alphanumerics/", "tableAlphanumerics", ListSelectionModel.SINGLE_SELECTION),
    SPECIAL_MODIFIER("LayeredForceIcon.special.text", "Pieces/Special Modifiers/", "tableSpecialModifiers", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    BACKGROUND("LayeredForceIcon.backgrounds.text", "Pieces/Backgrounds/", "tableBackgrounds", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    FRAME("LayeredForceIcon.frame.text", "Pieces/Frames/", "tableFrames", ListSelectionModel.SINGLE_SELECTION),
    LOGO("LayeredForceIcon.logos.text", "Pieces/Logos/", "tableLogos", ListSelectionModel.SINGLE_SELECTION);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name; // The name of the tab
    private final String layerPath; // The String containing the individual layer's path
    private final String tableName; // The String used in JTable::setName for accessibility purposes
    private final int listSelectionModel; // The int used to determine how the selection

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    LayeredForceIcon(String name, String layerPath, String tableName, int listSelectionModel) {
        this.name = resources.getString(name);
        this.layerPath = layerPath;
        this.tableName = tableName;
        this.listSelectionModel = listSelectionModel;
    }
    //endregion Constructors

    //region Getters
    public String getLayerPath() {
        return layerPath;
    }

    public String getTableName() {
        return tableName;
    }

    public int getListSelectionModel() {
        return listSelectionModel;
    }
    //endregion Getters

    /**
     * @return the layered force icon enum values in the order they are drawn in
     */
    public static List<LayeredForceIcon> getInDrawOrder() {
        List<LayeredForceIcon> drawOrder = new ArrayList<>();
        drawOrder.add(BACKGROUND);
        drawOrder.add(FRAME);
        drawOrder.add(TYPE);
        drawOrder.add(FORMATION);
        drawOrder.add(ADJUSTMENT);
        drawOrder.add(ALPHANUMERIC);
        drawOrder.add(SPECIAL_MODIFIER);
        drawOrder.add(LOGO);
        return drawOrder;
    }

    @Override
    public String toString() {
        return name;
    }
}

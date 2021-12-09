/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.MekHqConstants;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public enum LayeredForceIcon {
    //region Enum Declarations
    TYPE("LayeredForceIcon.TYPE.text", "LayeredForceIcon.TYPE.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_TYPE_PATH, "tableTypes", ListSelectionModel.SINGLE_SELECTION),
    FORMATION("LayeredForceIcon.FORMATION.text", "LayeredForceIcon.FORMATION.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_FORMATION_PATH, "tableFormations", ListSelectionModel.SINGLE_SELECTION),
    ADJUSTMENT("LayeredForceIcon.ADJUSTMENT.text", "LayeredForceIcon.ADJUSTMENT.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_ADJUSTMENT_PATH, "tableAdjustments", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    ALPHANUMERIC("LayeredForceIcon.ALPHANUMERIC.text", "LayeredForceIcon.ALPHANUMERIC.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_ALPHANUMERIC_PATH, "tableAlphanumerics", ListSelectionModel.SINGLE_SELECTION),
    SPECIAL_MODIFIER("LayeredForceIcon.SPECIAL_MODIFIER.text", "LayeredForceIcon.SPECIAL_MODIFIER.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_SPECIAL_MODIFIER_PATH, "tableSpecialModifiers", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    BACKGROUND("LayeredForceIcon.BACKGROUND.text", "LayeredForceIcon.BACKGROUND.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_BACKGROUND_PATH, "tableBackgrounds", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    FRAME("LayeredForceIcon.FRAME.text", "LayeredForceIcon.FRAME.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_FRAME_PATH, "tableFrames", ListSelectionModel.SINGLE_SELECTION),
    LOGO("LayeredForceIcon.LOGO.text", "LayeredForceIcon.LOGO.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_LOGO_PATH, "tableLogos", ListSelectionModel.SINGLE_SELECTION);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String layerPath;
    private final String tableName;
    private final int listSelectionModel;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    LayeredForceIcon(final String name, final String toolTipText, final String layerPath,
                     final String tableName, final int listSelectionModel) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.layerPath = layerPath;
        this.tableName = tableName;
        this.listSelectionModel = listSelectionModel;
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

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

    //region Boolean Comparison Methods
    public boolean isType() {
        return this == TYPE;
    }

    public boolean isFormation() {
        return this == FORMATION;
    }

    public boolean isAdjustment() {
        return this == ADJUSTMENT;
    }

    public boolean isAlphanumeric() {
        return this == ALPHANUMERIC;
    }

    public boolean isSpecialModifier() {
        return this == SPECIAL_MODIFIER;
    }

    public boolean isBackground() {
        return this == BACKGROUND;
    }

    public boolean isFrame() {
        return this == FRAME;
    }

    public boolean isLogo() {
        return this == LOGO;
    }
    //endregion Boolean Comparison Methods

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

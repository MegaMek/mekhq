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
package mekhq.campaign.icons.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHqConstants;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public enum LayeredForceIconLayer {
    //region Enum Declarations
    TYPE("LayeredForceIconLayer.TYPE.text", "LayeredForceIconLayer.TYPE.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_TYPE_PATH, "tableTypes", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    FORMATION("LayeredForceIconLayer.FORMATION.text", "LayeredForceIconLayer.FORMATION.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_FORMATION_PATH, "tableFormations", ListSelectionModel.SINGLE_SELECTION),
    ADJUSTMENT("LayeredForceIconLayer.ADJUSTMENT.text", "LayeredForceIconLayer.ADJUSTMENT.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_ADJUSTMENT_PATH, "tableAdjustments", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    ALPHANUMERIC("LayeredForceIconLayer.ALPHANUMERIC.text", "LayeredForceIconLayer.ALPHANUMERIC.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_ALPHANUMERIC_PATH, "tableAlphanumerics", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    SPECIAL_MODIFIER("LayeredForceIconLayer.SPECIAL_MODIFIER.text", "LayeredForceIconLayer.SPECIAL_MODIFIER.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_SPECIAL_MODIFIER_PATH, "tableSpecialModifiers", ListSelectionModel.SINGLE_SELECTION),
    BACKGROUND("LayeredForceIconLayer.BACKGROUND.text", "LayeredForceIconLayer.BACKGROUND.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_BACKGROUND_PATH, "tableBackgrounds", ListSelectionModel.SINGLE_SELECTION),
    FRAME("LayeredForceIconLayer.FRAME.text", "LayeredForceIconLayer.FRAME.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_FRAME_PATH, "tableFrames", ListSelectionModel.SINGLE_SELECTION),
    LOGO("LayeredForceIconLayer.LOGO.text", "LayeredForceIconLayer.LOGO.toolTipText",
            MekHqConstants.LAYERED_FORCE_ICON_LOGO_PATH, "tableLogos", ListSelectionModel.SINGLE_SELECTION);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String layerPath;
    private final String tableName;
    private final int listSelectionMode;
    //endregion Variable Declarations

    //region Constructors
    LayeredForceIconLayer(final String name, final String toolTipText, final String layerPath,
                          final String tableName, final int listSelectionMode) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign", new EncodeControl());

        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.layerPath = layerPath;
        this.tableName = tableName;
        this.listSelectionMode = listSelectionMode;
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

    public int getListSelectionMode() {
        return listSelectionMode;
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
    public static List<LayeredForceIconLayer> getInDrawOrder() {
        return Arrays.asList(BACKGROUND, FRAME, TYPE, FORMATION, ADJUSTMENT, ALPHANUMERIC, SPECIAL_MODIFIER, LOGO);
    }

    @Override
    public String toString() {
        return name;
    }
}

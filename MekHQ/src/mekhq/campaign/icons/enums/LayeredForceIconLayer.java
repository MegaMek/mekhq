/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.icons.enums;

import java.util.List;
import java.util.ResourceBundle;
import javax.swing.ListSelectionModel;

import mekhq.MHQConstants;
import mekhq.MekHQ;

/**
 * This contains the individual layers of a LayeredForceIcon, which are also the potential header folders within the
 * Pieces category of the Force Icon Directory.
 */
public enum LayeredForceIconLayer {
    //region Enum Declarations
    TYPE("LayeredForceIconLayer.TYPE.text", "LayeredForceIconLayer.TYPE.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_TYPE_PATH, "tableTypes", ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    FORMATION("LayeredForceIconLayer.FORMATION.text", "LayeredForceIconLayer.FORMATION.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_FORMATION_PATH, "tableFormations", ListSelectionModel.SINGLE_SELECTION),
    ADJUSTMENT("LayeredForceIconLayer.ADJUSTMENT.text",
          "LayeredForceIconLayer.ADJUSTMENT.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_ADJUSTMENT_PATH,
          "tableAdjustments",
          ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    ALPHANUMERIC("LayeredForceIconLayer.ALPHANUMERIC.text",
          "LayeredForceIconLayer.ALPHANUMERIC.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_ALPHANUMERIC_PATH,
          "tableAlphanumerics",
          ListSelectionModel.MULTIPLE_INTERVAL_SELECTION),
    SPECIAL_MODIFIER("LayeredForceIconLayer.SPECIAL_MODIFIER.text",
          "LayeredForceIconLayer.SPECIAL_MODIFIER.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_SPECIAL_MODIFIER_PATH,
          "tableSpecialModifiers",
          ListSelectionModel.SINGLE_SELECTION),
    BACKGROUND("LayeredForceIconLayer.BACKGROUND.text", "LayeredForceIconLayer.BACKGROUND.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_BACKGROUND_PATH, "tableBackgrounds", ListSelectionModel.SINGLE_SELECTION),
    FRAME("LayeredForceIconLayer.FRAME.text", "LayeredForceIconLayer.FRAME.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_FRAME_PATH, "tableFrames", ListSelectionModel.SINGLE_SELECTION),
    LOGO("LayeredForceIconLayer.LOGO.text", "LayeredForceIconLayer.LOGO.toolTipText",
          MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH, "tableLogos", ListSelectionModel.SINGLE_SELECTION);
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
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
              MekHQ.getMHQOptions().getLocale());
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
        return List.of(BACKGROUND, FRAME, TYPE, FORMATION, ADJUSTMENT, ALPHANUMERIC, SPECIAL_MODIFIER, LOGO);
    }

    @Override
    public String toString() {
        return name;
    }
}

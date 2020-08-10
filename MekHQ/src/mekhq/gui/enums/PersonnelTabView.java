/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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

import java.util.ResourceBundle;

public enum PersonnelTabView {
    GRAPHIC("PersonnelTabView.GRAPHIC"),
    GENERAL("PersonnelTabView.GENERAL"),
    PILOT_GUNNERY_SKILLS("PersonnelTabView.PILOT_GUNNERY_SKILLS"),
    INFANTRY_SKILLS("PersonnelTabView.INFANTRY_SKILLS"),
    TACTICAL_SKILLS("PersonnelTabView.TACTICAL_SKILLS"),
    TECHNICAL_SKILLS("PersonnelTabView.TECHNICAL_SKILLS"),
    ADMINISTRATIVE_SKILLS("PersonnelTabView.ADMINISTRATIVE_SKILLS"),
    BIOGRAPHICAL("PersonnelTabView.BIOGRAPHICAL"),
    FLUFF("PersonnelTabView.FLUFF");

    private final String name;

    PersonnelTabView(String name) {
        this.name = ResourceBundle.getBundle("mekhq.resources.GUIEnums", new EncodeControl())
                .getString(name);
    }

    @Override
    public String toString() {
        return name;
    }
}

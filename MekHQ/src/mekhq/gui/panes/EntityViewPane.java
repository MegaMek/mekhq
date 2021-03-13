/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panes;

import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Entity;
import megamek.common.MechView;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TROView;
import mekhq.MekHQ;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;

import javax.swing.*;

public class EntityViewPane extends AbstractMHQTabbedPane {
    //region Variable Declarations
    private MechViewPanel entityPanel;
    private MechViewPanel troPanel;
    //endregion Variable Declarations

    //region Constructors
    public EntityViewPane(final JFrame frame, final @Nullable Entity entity) {
        super(frame, "EntityViewPane");
        initialize();
        updateDisplayedEntity(entity);
    }
    //endregion Constructors

    //region Getters/Setters
    public MechViewPanel getEntityPanel() {
        return entityPanel;
    }

    public void setEntityPanel(final MechViewPanel entityPanel) {
        this.entityPanel = entityPanel;
    }

    public MechViewPanel getTROPanel() {
        return troPanel;
    }

    public void setTROPanel(final MechViewPanel troPanel) {
        this.troPanel = troPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        setEntityPanel(new MechViewPanel());
        getEntityPanel().setName("entityPanel");
        addTab("Summary", getEntityPanel());

        setTROPanel(new MechViewPanel());
        getTROPanel().setName("troPanel");
        addTab("TRO", getTROPanel());

        setPreferences();
    }
    //endregion Initialization

    /**
     * This updates the pane's currently displayed entity
     * @param entity the entity to update to, or null if the panels are to be reset.
     */
    public void updateDisplayedEntity(final @Nullable Entity entity) {
        // null entity, which means to reset the panels
        if (entity == null) {
            getEntityPanel().reset();
            getTROPanel().reset();
            return;
        }

        try {
            getEntityPanel().setMech(entity, new MechView(entity, false));
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            getEntityPanel().reset();
        }

        try {
            getTROPanel().setMech(entity, TROView.createView(entity, true));
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            getTROPanel().reset();
        }
    }
}

/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.location.IPlace;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.model.LocationDisplay;

/**
 * Detail panel for a single {@link IPlace}. Shows the place's name, type, current location status, system, planet, and
 * (when in transit) its destination.
 *
 * <p>Follows MVC: {@link LocationPlacePanelModel} computes display strings, {@link
 * LocationPlacePanelView} owns the widgets, and {@link LocationPlacePanelController} drives the single
 * populate-on-construction update.</p>
 */
public class LocationPlacePanel extends JScrollablePanel {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    public LocationPlacePanel(IPlace place, Campaign campaign) {
        LocationPlacePanelModel model = new LocationPlacePanelModel(place, campaign);
        LocationPlacePanelView view = new LocationPlacePanelView();
        new LocationPlacePanelController(model, view);

        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);
    }

    // =========================================================================
    // Model
    // =========================================================================

    private static final class LocationPlacePanelModel {

        private final String name;
        private final String placeType;
        private final String system;
        private final String planet;
        private final String status;
        private final boolean inTransit;
        private final String destinationName;
        private final String destinationSystem;
        private final String destinationPlanet;

        LocationPlacePanelModel(IPlace place, Campaign campaign) {
            LocalDate today = campaign.getLocalDate();
            name = LocationDisplay.getLocationName(place, campaign, today);
            placeType = resolveType(place);
            system = LocationDisplay.getLocationSystem(place, today, campaign);
            planet = LocationDisplay.getLocationPlanet(place, today, campaign);
            status = resolveStatus(place);
            inTransit = place.isInTransit();
            destinationName = LocationDisplay.getDestinationName(place, campaign, today);
            destinationSystem = LocationDisplay.getDestinationSystem(place, today);
            destinationPlanet = LocationDisplay.getDestinationPlanet(place, today);
        }

        private static String resolveType(IPlace place) {
            if (place instanceof Campaign) {
                return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.type.mainForce");
            }
            if (place instanceof AbstractBase base) {
                String displayType = base.getDisplayType();
                return (displayType != null && !displayType.isBlank())
                             ? displayType
                             : getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.type.base");
            }
            return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.type.location");
        }

        private static String resolveStatus(IPlace place) {
            if (place.isInTransit()) {
                return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.status.inTransit");
            }
            if (place.isAtJumpPoint()) {
                return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.status.atJumpPoint");
            }
            if (place.isOnPlanet()) {
                return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.status.onPlanet");
            }
            return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.status.unknown");
        }

        String getName() {return name;}

        String getPlaceType() {return placeType;}

        String getSystem() {return system;}

        String getPlanet() {return planet;}

        String getStatus() {return status;}

        boolean isInTransit() {return inTransit;}

        String getDestinationName() {return destinationName;}

        String getDestinationSystem() {return destinationSystem;}

        String getDestinationPlanet() {return destinationPlanet;}
    }

    // =========================================================================
    // View
    // =========================================================================

    private static final class LocationPlacePanelView extends JPanel {

        private static final int PADDING = UIUtil.scaleForGUI(10);

        private final JLabel lblName = new JLabel();
        private final JLabel lblType = new JLabel();
        private final JLabel lblStatusValue = new JLabel();
        private final JLabel lblSystemValue = new JLabel();
        private final JLabel lblPlanetValue = new JLabel();
        private final JLabel lblDestLabel;
        private final JLabel lblDestValue = new JLabel();
        private final JLabel lblDestSystemLabel;
        private final JLabel lblDestSystemValue = new JLabel();
        private final JLabel lblDestPlanetLabel;
        private final JLabel lblDestPlanetValue = new JLabel();

        LocationPlacePanelView() {
            lblDestLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.label.destination"));
            lblDestSystemLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.label.destinationSystem"));
            lblDestPlanetLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.label.destinationPlanet"));

            lblName.setFont(lblName.getFont().deriveFont(Font.BOLD, lblName.getFont().getSize2D() + 2f));
            lblType.setFont(lblType.getFont().deriveFont(Font.ITALIC));

            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(PADDING / 4, PADDING / 4, PADDING / 4, PADDING / 4);

            // Name spanning both columns
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            add(lblName, gbc);

            // Type spanning both columns
            gbc.gridy = 1;
            add(lblType, gbc);

            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;

            addRow(2, getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.label.status"), lblStatusValue, gbc);
            addRow(3, getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.label.system"), lblSystemValue, gbc);
            addRow(4, getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.label.planet"), lblPlanetValue, gbc);
            addPairRow(5, lblDestLabel, lblDestValue, gbc);
            addPairRow(6, lblDestSystemLabel, lblDestSystemValue, gbc);
            addPairRow(7, lblDestPlanetLabel, lblDestPlanetValue, gbc);

            gbc.gridx = 0;
            gbc.gridy = 8;
            gbc.gridwidth = 2;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            add(new JPanel(), gbc);
        }

        private void addRow(int row, String labelText, JLabel value, GridBagConstraints gbc) {
            addPairRow(row, new JLabel(labelText), value, gbc);
        }

        private void addPairRow(int row, JLabel label, JLabel value, GridBagConstraints gbc) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            add(label, gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            add(value, gbc);
        }

        void update(LocationPlacePanelModel model) {
            lblName.setText(model.getName());
            lblType.setText(model.getPlaceType());
            lblStatusValue.setText(model.getStatus());
            lblSystemValue.setText(model.getSystem());
            lblPlanetValue.setText(model.getPlanet());

            boolean inTransit = model.isInTransit();
            lblDestLabel.setVisible(inTransit);
            lblDestValue.setVisible(inTransit);
            lblDestSystemLabel.setVisible(inTransit);
            lblDestSystemValue.setVisible(inTransit);
            lblDestPlanetLabel.setVisible(inTransit);
            lblDestPlanetValue.setVisible(inTransit);

            if (inTransit) {
                lblDestValue.setText(model.getDestinationName());
                lblDestSystemValue.setText(model.getDestinationSystem());
                lblDestPlanetValue.setText(model.getDestinationPlanet());
            }
        }
    }

    // =========================================================================
    // Controller
    // =========================================================================

    private static final class LocationPlacePanelController {

        LocationPlacePanelController(LocationPlacePanelModel model, LocationPlacePanelView view) {
            view.update(model);
        }
    }
}

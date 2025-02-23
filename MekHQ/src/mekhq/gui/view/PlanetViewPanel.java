/*
 * Copyright (C) 2009-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.PlanetaryType;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.MarkdownRenderer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A custom panel that gets filled in with goodies from a Planet record
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class PlanetViewPanel extends JScrollablePanel {
    private PlanetarySystem system;
    private Campaign campaign;
    private int planetPos;

    private Image planetIcon = null;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel",
            MekHQ.getMHQOptions().getLocale());

    public PlanetViewPanel(PlanetarySystem s, Campaign c) {
        this(s, c, 0);
    }

    public PlanetViewPanel(PlanetarySystem s, Campaign c, int p) {
        super();
        this.system = s;
        this.campaign = c;
        this.planetPos = p;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel pnlSystem = getSystemPanel();
        pnlSystem.setBorder(BorderFactory.createTitledBorder(system.getPrintableName(campaign.getLocalDate()) + ' ' + resourceMap.getString("system.text")));
        add(pnlSystem);

        Planet planet = system.getPlanet(planetPos);
        if (null == planet) {
            //try the primary - but still could be null
            planet = system.getPrimaryPlanet();
        }
        if (null != planet) {
            JPanel pnlPlanet = getPlanetPanel(planet);
            pnlPlanet.setBorder(BorderFactory.createTitledBorder(planet.getPrintableName(campaign.getLocalDate())));
            add(pnlPlanet);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        if (null != planetIcon) {
            Graphics2D gfx = (Graphics2D) g;
            final int width = getWidth();
            final int offset = 6;
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Arc2D.Double arc = new Arc2D.Double();
            gfx.setPaint(Color.BLACK);
            arc.setArcByCenter(width - 32 - offset, 32 + offset, 35, 0, 360, Arc2D.OPEN);
            gfx.fill(arc);
            gfx.setPaint(Color.WHITE);
            arc.setArcByCenter(width - 32 - offset, 32 + offset, 34, 0, 360, Arc2D.OPEN);
            gfx.fill(arc);
            gfx.drawImage(planetIcon, width - 64 - offset, offset, 64, 64, null);
        }
    }

    private JPanel getPlanetPanel(Planet planet) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        LocalDate currentDate = campaign.getLocalDate();

        JLabel lblOwner = new JLabel("<html><nobr><i>" + planet.getFactionDesc(campaign.getLocalDate()) + "</i></nobr></html>");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(lblOwner, gridBagConstraints);

        //Set up grid bag constraints
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.fill = GridBagConstraints.HORIZONTAL;
        gbcLabel.anchor = GridBagConstraints.NORTHWEST;
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 1;
        gbcText.weightx = 1.0;
        gbcText.insets = new Insets(0, 10, 0, 0);
        gbcText.fill = GridBagConstraints.BOTH;
        gbcText.anchor = GridBagConstraints.NORTHWEST;
        int infoRow = 1;

        //Planet type
        JLabel lblPlanetType = new JLabel(resourceMap.getString("lblPlanetaryType1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblPlanetType, gbcLabel);
        JLabel txtPlanetType = new JLabel(planet.getPlanetType().toString());
        gbcText.gridy = infoRow;
        panel.add(txtPlanetType, gbcText);
        ++ infoRow;

        //System Position
        if (planet.getPlanetType() != PlanetaryType.ASTEROID_BELT) {
            JLabel lblDiameter = new JLabel(resourceMap.getString("lblDiameter.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDiameter, gbcLabel);
            JLabel txtDiameter = new JLabel( String.format("%.1f km",
                    planet.getDiameter()));
            gbcText.gridy = infoRow;
            panel.add(txtDiameter, gbcText);
            ++ infoRow;
        }

        //System Position
        if ((null != planet.getSystemPosition()) || (null != planet.getOrbitRadius())) {
            JLabel lblPosition = new JLabel(resourceMap.getString("lblPosition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPosition, gbcLabel);
            JLabel txtPosition = getTxtPosition(planet);
            gbcText.gridy = infoRow;
            panel.add(txtPosition, gbcText);
            ++ infoRow;
        }

        //Time to Jump point
        JLabel lblJumpPoint = new JLabel(resourceMap.getString("lblJumpPoint1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblJumpPoint, gbcLabel);
        JLabel txtJumpPoint = new JLabel(Math.round(100 * planet.getTimeToJumpPoint(1)) / 100.0 + " days");
        gbcText.gridy = infoRow;
        panel.add(txtJumpPoint, gbcText);
        ++ infoRow;

        //Year length
        if (null != planet.getYearLength()) {
            JLabel lblYear = new JLabel(resourceMap.getString("lblYear1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblYear, gbcLabel);
            JLabel txtYear = new JLabel(planet.getYearLength() + " Terran years");
            gbcText.gridy = infoRow;
            panel.add(txtYear, gbcText);
            ++ infoRow;
        }

        //day length
        if (null != planet.getDayLength(currentDate)) {
            JLabel lblDay = new JLabel(resourceMap.getString("lblDay1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDay, gbcLabel);
            JLabel txtDay = new JLabel(planet.getDayLength(currentDate) + " hours");
            gbcText.gridy = infoRow;
            panel.add(txtDay, gbcText);
            ++ infoRow;
        }

        //Gravity
        if (null != planet.getGravity()) {
            JLabel lblGravity = new JLabel(resourceMap.getString("lblGravity1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblGravity, gbcLabel);
            JLabel txtGravity = new JLabel(planet.getGravityText());
            gbcText.gridy = infoRow;
            panel.add(txtGravity, gbcText);
            ++ infoRow;
        }

        //Atmosphere
        if (null != planet.getAtmosphere(currentDate)) {
            JLabel lblAtmosphere = new JLabel(resourceMap.getString("lblAtmosphere.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAtmosphere, gbcLabel);
            JLabel txtAtmosphere = new JLabel(planet.getAtmosphereName(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtAtmosphere, gbcText);
            ++ infoRow;
        }

        //Atmospheric Pressure
        if (null != planet.getPressure(currentDate)) {
            JLabel lblPressure = new JLabel(resourceMap.getString("lblPressure1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPressure, gbcLabel);
            JLabel txtPressure = new JLabel(planet.getPressureName(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtPressure, gbcText);
            ++ infoRow;
        }

        //Atmospheric composition
        if (null != planet.getComposition(currentDate)) {
            JLabel lblComposition = new JLabel(resourceMap.getString("lblComposition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblComposition, gbcLabel);
            JLabel txtComposition = new JLabel("<html>" + planet.getComposition(currentDate) + "</html>");
            gbcText.gridy = infoRow;
            panel.add(txtComposition, gbcText);
            ++ infoRow;
        }

        //Temperature
        if ((null != planet.getTemperature(currentDate))) {
            JLabel lblTemp = new JLabel(resourceMap.getString("lblTemp1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblTemp, gbcLabel);
            //Using Unicode for the degree symbol as it is required for proper display on certain systems
            JLabel txtTemp = new JLabel(planet.getTemperature(currentDate) + "°" + 'C');
            gbcText.gridy = infoRow;
            panel.add(txtTemp, gbcText);
            ++ infoRow;
        }

        //Water
        if (null != planet.getPercentWater(currentDate)) {
            JLabel lblWater = new JLabel(resourceMap.getString("lblWater1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblWater, gbcLabel);
            JLabel txtWater = new JLabel(planet.getPercentWater(currentDate) + " percent");
            gbcText.gridy = infoRow;
            panel.add(txtWater, gbcText);
            ++ infoRow;
        }

        //native life forms
        if (null != planet.getLifeForm(currentDate)) {
            JLabel lblAnimal = new JLabel(resourceMap.getString("lblAnimal1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAnimal, gbcLabel);
            JLabel txtAnimal = new JLabel(planet.getLifeFormName(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtAnimal, gbcText);
            ++ infoRow;
        }

        //satellites
        if (null != planet.getSatellites() || planet.getSmallMoons()>0) {
            JLabel lblSatellite = new JLabel(resourceMap.getString("lblSatellite1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSatellite, gbcLabel);
            JLabel txtSatellite = new JLabel("<html>" + planet.getSatelliteDescription() + "</html>");
            gbcText.gridy = infoRow;
            panel.add(txtSatellite, gbcText);
            ++ infoRow;
        }

        //landmasses
        if (null != planet.getLandMasses()) {
            JLabel lblLandMass = new JLabel(resourceMap.getString("lblLandMass1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblLandMass, gbcLabel);
            JLabel txtLandMass = new JLabel("<html>" + planet.getLandMassDescription() + "</html>");
            gbcText.gridy = infoRow;
            panel.add(txtLandMass, gbcText);
            ++ infoRow;
        }

        //Population
        if (null != planet.getPopulation(currentDate)) {
            JLabel lblPopulation = new JLabel(resourceMap.getString("lblPopulation.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPopulation, gbcLabel);
            JLabel txtPopulation = new JLabel(NumberFormat.getNumberInstance(Locale.getDefault()).format(planet.getPopulation(currentDate)));
            gbcText.gridy = infoRow;
            panel.add(txtPopulation, gbcText);
            ++ infoRow;
        }

        //SIC codes
        if (null != planet.getSocioIndustrial(currentDate)) {
            JLabel lblSocioIndustrial = new JLabel(resourceMap.getString("lblSocioIndustrial1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSocioIndustrial, gbcLabel);
            SocioIndustrialData sid = planet.getSocioIndustrial(currentDate);
            String sidText = (null == sid) ? "" : sid.getHTMLDescription();
            JLabel txtSocioIndustrial = new JLabel(sidText);
            gbcText.gridy = infoRow;
            panel.add(txtSocioIndustrial, gbcText);
            ++ infoRow;
        }

        //HPG status
        if (null != planet.getHPGClass(currentDate)) {
            JLabel lblHPG = new JLabel(resourceMap.getString("lblHPG1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblHPG, gbcLabel);
            JLabel txtHPG = new JLabel(planet.getHPGClass(currentDate));
            gbcText.gridy = infoRow;
            panel.add(txtHPG, gbcText);
            ++ infoRow;
        }

        //Hiring Hall Level
        JLabel lblHiringHall = new JLabel(resourceMap.getString("lblHiringHall.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblHiringHall, gbcLabel);
        JLabel textHiringHall = new JLabel(StringUtils.capitalize(
            planet.getHiringHallLevel(currentDate)
            .name()
            .toLowerCase()));
        gbcText.gridy = infoRow;
        panel.add(textHiringHall, gbcText);
        ++ infoRow;

        // Academies
        List<Academy> filteredAcademies = system.getFilteredAcademies(campaign);

        if (!filteredAcademies.isEmpty()) {
            ++infoRow;
            JLabel lblAcademies = new JLabel(resourceMap.getString("lblAcademies.text"));
            gbcLabel.gridx = 0;
            gbcLabel.gridy = infoRow;
            panel.add(lblAcademies, gbcLabel);

            JTextPane txtAcademies = new JTextPane();
            txtAcademies.setEditable(false);
            txtAcademies.setContentType("text/html");
            txtAcademies.setText(MarkdownRenderer.getRenderedHtml(system.getAcademiesForSystem(filteredAcademies)));
            ((DefaultCaret) txtAcademies.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panel.add(txtAcademies, gridBagConstraints);
            ++infoRow;
        }

        if (null != planet.getDescription()) {
            JTextPane txtDesc = new JTextPane();
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(planet.getDescription()));
            ((DefaultCaret) txtDesc.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panel.add(txtDesc, gridBagConstraints);
        }

        return panel;
    }

    private static JLabel getTxtPosition(Planet planet) {
        String text;
        if (null != planet.getOrbitRadius()) {
            if (planet.getPlanetType() == PlanetaryType.ASTEROID_BELT) {
                text = String.format("%.3f AU",
                        planet.getOrbitRadius());
            } else {
                text = String.format("%s (%.3f AU)",
                        planet.getDisplayableSystemPosition(), planet.getOrbitRadius());
            }
        } else {
            text = planet.getDisplayableSystemPosition();
        }
        JLabel txtPosition = new JLabel(text);
        return txtPosition;
    }

    private JPanel getSystemPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        LocalDate currentDate = campaign.getLocalDate();

        //Set up grid bag constraints
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.fill = GridBagConstraints.NONE;
        gbcLabel.anchor = GridBagConstraints.NORTHWEST;
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 1;
        gbcText.weightx = 0.5;
        gbcText.insets = new Insets(0, 10, 0, 0);
        gbcText.fill = GridBagConstraints.HORIZONTAL;
        gbcText.anchor = GridBagConstraints.WEST;
        int infoRow = 0;

        //Star Type
        JLabel lblStarType = new JLabel(resourceMap.getString("lblStarType1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblStarType, gbcLabel);
        JLabel txtStarType = new JLabel(system.getStar().toString() + " (" + system.getRechargeTimeText(currentDate) + ')');
        gbcText.gridy = infoRow;
        panel.add(txtStarType, gbcText);
        ++ infoRow;

        //Recharge Stations
        JLabel lblRecharge = new JLabel(resourceMap.getString("lblRecharge1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblRecharge, gbcLabel);
        JLabel txtRecharge = new JLabel(system.getRechargeStationsText(currentDate));
        gbcText.gridy = infoRow;
        panel.add(txtRecharge, gbcText);

        //TODO: maybe some other summary information, like best HPG and number of planetary systems

        return panel;
    }
}

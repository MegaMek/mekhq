/*
 * PlanetViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;

import org.joda.time.DateTime;

import megamek.common.util.EncodeControl;
import megamek.common.util.ImageUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.StarUtil;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * A custom panel that gets filled in with goodies from a Planet record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PlanetViewPanel extends JPanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private PlanetarySystem system;
    private Campaign campaign;
    
    private JPanel pnlSystem;
    private JPanel pnlPrimary;
    
    private Image planetIcon = null;
    
    public PlanetViewPanel(PlanetarySystem s, Campaign c) {
        this.system = s;
        this.campaign = c;
        initComponents();
    }
    
    private void initComponents() {

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);

        pnlSystem = getSystemPanel();
        pnlSystem.setBorder(BorderFactory.createTitledBorder(system.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar())) + " " + resourceMap.getString("system.text")));
        pnlSystem.setBackground(Color.WHITE);
        add(pnlSystem);
        
        Planet primary = system.getPrimaryPlanet();
        if(null != primary) {
            pnlPrimary = getPlanetPanel(primary);
            pnlPrimary.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2),
                    "<html><b>" + primary.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()))  + " " + resourceMap.getString("primaryPlanet.text") + "</b></html>"));
            pnlPrimary.setBackground(Color.WHITE);
            add(pnlPrimary);
        };
        
        for(int pos : system.getPlanetPositions()) {
            if(pos == system.getPrimaryPlanetPosition()) {
                continue;
            }
            Planet p = system.getPlanet(pos);
            JPanel planetPanel = getPlanetPanel(p);
            planetPanel.setBorder(BorderFactory.createTitledBorder(p.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar()))));
            planetPanel.setBackground(Color.WHITE);
            add(planetPanel);
        }
        //planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(planet));
    }
    
    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
    
        if(null != planetIcon) {
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
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1
        JPanel panel = new JPanel();        
        panel.setLayout(new GridBagLayout());
        DateTime currentDate = Utilities.getDateTimeDay(campaign.getCalendar());
        
        JLabel lblOwner = new JLabel("<html><i>" + planet.getFactionDesc(currentDate) + "</i></html>");
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
        gbcLabel.fill = GridBagConstraints.NONE;
        gbcLabel.anchor = GridBagConstraints.NORTHWEST;        
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 1;
        gbcText.weightx = 0.5;
        gbcText.insets = new Insets(0, 10, 0, 0);
        gbcText.fill = GridBagConstraints.HORIZONTAL;
        gbcText.anchor = GridBagConstraints.WEST;  
        int infoRow = 1;
        
        //Planet type
        JLabel lblPlanetType = new JLabel(resourceMap.getString("lblPlanetaryType1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblPlanetType, gbcLabel);        
        JTextArea txtPlanetType = new JTextArea(planet.getPlanetType());
        txtPlanetType.setEditable(false);
        txtPlanetType.setLineWrap(true);
        txtPlanetType.setWrapStyleWord(true);
        gbcText.gridy = infoRow;
        panel.add(txtPlanetType, gbcText);
        ++ infoRow;
        
        //System Position
        if((null != planet.getSystemPosition()) || (null != planet.getOrbitRadius())) {
            JLabel lblPosition = new JLabel(resourceMap.getString("lblPosition.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPosition, gbcLabel);            
            String text;
            if(null != planet.getOrbitRadius()) {
                text = String.format("%s (%.3f AU)", //$NON-NLS-1$
                    planet.getSystemPositionText(), planet.getOrbitRadius());
            } else {
                text = planet.getSystemPositionText();
            }
            JTextArea txtPosition = new JTextArea(text);
            txtPosition.setEditable(false);
            txtPosition.setLineWrap(true);
            txtPosition.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtPosition, gbcText);
            ++ infoRow;
        }
        
        //Time to Jump point
        JLabel lblJumpPoint = new JLabel(resourceMap.getString("lblJumpPoint1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblJumpPoint, gbcLabel);        
        JTextArea txtJumpPoint = new JTextArea(Double.toString(Math.round(100 * planet.getTimeToJumpPoint(1))/100.0) + " days");
        txtJumpPoint.setEditable(false);
        txtJumpPoint.setLineWrap(true);
        txtJumpPoint.setWrapStyleWord(true);
        gbcText.gridy = infoRow;
        panel.add(txtJumpPoint, gbcText);
        ++ infoRow;
        
        //Year length
        if(null != planet.getYearLength()) {
            JLabel lblYear = new JLabel(resourceMap.getString("lblYear1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblYear, gbcLabel);        
            JTextArea txtYear = new JTextArea(Double.toString(planet.getYearLength()) + " Terran years");
            txtYear.setEditable(false);
            txtYear.setLineWrap(true);
            txtYear.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtYear, gbcText);
            ++ infoRow;
        }
        
        //day length
        if(null != planet.getDayLength()) {
            JLabel lblDay = new JLabel(resourceMap.getString("lblDay1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblDay, gbcLabel);        
            JTextArea txtDay = new JTextArea(Double.toString(planet.getDayLength()) + " hours");
            txtDay.setEditable(false);
            txtDay.setLineWrap(true);
            txtDay.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtDay, gbcText);
            ++ infoRow;
        }
        
        /*
           * TODO: fix satelllite information
        lblSatellite.setName("lblSatellite"); // NOI18N
        lblSatellite.setText(resourceMap.getString("lblSatellite1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = infoRow;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSatellite, gridBagConstraints);
        
        txtSatellite.setName("lblSatellite2"); // NOI18N
        txtSatellite.setText(planet.getSatelliteDescription());
        txtSatellite.setEditable(false);
        txtSatellite.setLineWrap(true);
        txtSatellite.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = infoRow;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtSatellite, gridBagConstraints);
        
        ++ infoRow;
        */
        
        //Gravity
        if(null != planet.getGravity()) {
            JLabel lblGravity = new JLabel(resourceMap.getString("lblGravity1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblGravity, gbcLabel);        
            JTextArea txtGravity = new JTextArea(planet.getGravityText());
            txtGravity.setEditable(false);
            txtGravity.setLineWrap(true);
            txtGravity.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtGravity, gbcText);
            ++ infoRow;
        }
        
        //Atmospheric Pressure
        if(null != planet.getPressure(currentDate)) {
            JLabel lblPressure = new JLabel(resourceMap.getString("lblPressure1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPressure, gbcLabel);        
            JTextArea txtPressure = new JTextArea(planet.getPressureName(currentDate));
            txtPressure.setEditable(false);
            txtPressure.setLineWrap(true);
            txtPressure.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtPressure, gbcText);
            ++ infoRow;
        }
        
        //Temperature
        if((null != planet.getTemperature(currentDate))) {
            JLabel lblTemp = new JLabel(resourceMap.getString("lblTemp1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblTemp, gbcLabel);        
            JTextArea txtTemp = new JTextArea(planet.getTemperature(currentDate) + "°C");
            txtTemp.setEditable(false);
            txtTemp.setLineWrap(true);
            txtTemp.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtTemp, gbcText);
            ++ infoRow;
        }
        
        //Water
        if(null != planet.getPercentWater(currentDate)) {
            JLabel lblWater = new JLabel(resourceMap.getString("lblWater1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblWater, gbcLabel);        
            JTextArea txtWater = new JTextArea(planet.getPercentWater(currentDate) + " percent");
            txtWater.setEditable(false);
            txtWater.setLineWrap(true);
            txtWater.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtWater, gbcText);
            ++ infoRow;
        }
        
        //native life forms
        if(null != planet.getLifeForm(currentDate)) {
            JLabel lblAnimal = new JLabel(resourceMap.getString("lblAnimal1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblAnimal, gbcLabel);        
            JTextArea txtAnimal = new JTextArea(planet.getLifeFormName(currentDate));
            txtAnimal.setEditable(false);
            txtAnimal.setLineWrap(true);
            txtAnimal.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtAnimal, gbcText);
            ++ infoRow;
        }
        
        //landmasses
        if(null != planet.getLandMasses()) {
            JLabel lblLandMass = new JLabel(resourceMap.getString("lblLandMass1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblLandMass, gbcLabel);        
            JTextArea txtLandMass = new JTextArea(planet.getLandMassDescription());
            txtLandMass.setEditable(false);
            txtLandMass.setLineWrap(true);
            txtLandMass.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtLandMass, gbcText);
            ++ infoRow;
        }
        
        //Population
        if(null != planet.getPopulation(currentDate)) {
            JLabel lblPopulation = new JLabel(resourceMap.getString("lblPopulation.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblPopulation, gbcLabel);
            JTextArea txtPopulation = new JTextArea(NumberFormat.getNumberInstance(Locale.getDefault()).format(planet.getPopulation(currentDate)));
            txtPopulation.setEditable(false);
            txtPopulation.setLineWrap(true);
            txtPopulation.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtPopulation, gbcText);
            ++ infoRow;
        }
        
        //SIC codes
        if(null != planet.getSocioIndustrial(currentDate)) {
            JLabel lblSocioIndustrial = new JLabel(resourceMap.getString("lblSocioIndustrial1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblSocioIndustrial, gbcLabel);      
            SocioIndustrialData sid = planet.getSocioIndustrial(currentDate);
            String sidText = (null == sid) ? "" : sid.getHTMLDescription();
            JTextPane txtSocioIndustrial = new JTextPane();
            txtSocioIndustrial.setContentType("text/html");
            txtSocioIndustrial.setText(sidText);
            txtSocioIndustrial.setEditable(false);
            gbcText.gridy = infoRow;
            panel.add(txtSocioIndustrial, gbcText);
            ++ infoRow;
        }
        
        //HPG status
        if(null != planet.getHPGClass(currentDate)) {
            JLabel lblHPG = new JLabel(resourceMap.getString("lblHPG1.text"));
            gbcLabel.gridy = infoRow;
            panel.add(lblHPG, gbcLabel);        
            JTextArea txtHPG = new JTextArea(planet.getHPGClass(currentDate));
            txtHPG.setEditable(false);
            txtHPG.setLineWrap(true);
            txtHPG.setWrapStyleWord(true);
            gbcText.gridy = infoRow;
            panel.add(txtHPG, gbcText);
            ++ infoRow;
        }

        if(null != planet.getDescription()) {
            JTextArea txtDesc = new JTextArea(planet.getDescription());
            ((DefaultCaret) txtDesc.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            txtDesc.setEditable(false);
            txtDesc.setLineWrap(true);
            txtDesc.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panel.add(txtDesc, gridBagConstraints);
        }
        
        return panel;
    }
    
    private JPanel getSystemPanel() {
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1$
        JPanel panel = new JPanel();               
        panel.setLayout(new GridBagLayout());
        DateTime currentDate = Utilities.getDateTimeDay(campaign.getCalendar());
        
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
        JTextArea txtStarType = new JTextArea(system.getSpectralTypeText() + " (" + system.getRechargeTimeText(currentDate) + ")");
        txtStarType.setEditable(false);
        txtStarType.setLineWrap(true);
        txtStarType.setWrapStyleWord(true);
        gbcText.gridy = infoRow;
        panel.add(txtStarType, gbcText);        
        ++ infoRow;
        
        //Recharge Stations
        JLabel lblRecharge = new JLabel(resourceMap.getString("lblRecharge1.text"));
        gbcLabel.gridy = infoRow;
        panel.add(lblRecharge, gbcLabel);        
        JTextArea txtRecharge = new JTextArea(system.getRechargeStationsText(currentDate));
        txtRecharge.setEditable(false);
        txtRecharge.setLineWrap(true);
        txtRecharge.setWrapStyleWord(true);
        gbcText.gridy = infoRow;
        panel.add(txtRecharge, gbcText);
        
        //TODO: maybe some other summary information, like best HPG and number of planetary systems
        
        return panel;
    }
}
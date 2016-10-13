/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.JPanel;

import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

public class MedicalViewPanel extends JPanel {
    private static final long serialVersionUID = 6178230374580087883L;
    
    private final Campaign campaign;
    private final Person person;
    private final IconPackage iconPackage;
    private ResourceBundle resourceMap = null;

    private Image defaultMaleDoll;
    private Image defaultFemaleDoll;
    
    public MedicalViewPanel(Campaign c, Person p, IconPackage ip) {
        super();
        this.campaign = Objects.requireNonNull(c);
        this.person = Objects.requireNonNull(p);
        this.iconPackage = Objects.requireNonNull(ip);
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonViewPanel", new EncodeControl()); //$NON-NLS-1$
        
        // Preload default paperdolls
        String doll = ip.getGuiElement("default_male_paperdoll");
        if((null != doll) && !doll.isEmpty()) {
            defaultMaleDoll = Toolkit.getDefaultToolkit().createImage(doll);
        }
        doll = ip.getGuiElement("default_female_paperdoll");
        if((null != doll) && !doll.isEmpty()) {
            defaultFemaleDoll = Toolkit.getDefaultToolkit().createImage(doll);
        }
        setMinimumSize(new Dimension(1024, 768));
        initComponents();
    }
    
    private void initComponents() {
        
    }
}

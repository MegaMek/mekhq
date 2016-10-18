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
package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.gui.view.Paperdoll;

public class MedicalViewDialog extends JDialog {
    private static final long serialVersionUID = 6178230374580087883L;
    
    private final Campaign campaign;
    private final Person person;
    private final IconPackage iconPackage;
    private ResourceBundle resourceMap = null;

    private Paperdoll defaultMaleDoll;
    private Paperdoll defaultFemaleDoll;
    
    public MedicalViewDialog(Frame parent, Campaign c, Person p, IconPackage ip) {
        super();
        this.campaign = Objects.requireNonNull(c);
        this.person = Objects.requireNonNull(p);
        this.iconPackage = Objects.requireNonNull(ip);
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonViewPanel", new EncodeControl()); //$NON-NLS-1$
        
        // Preload default paperdolls
        defaultMaleDoll = new Paperdoll(ip.getGuiElement("default_male_paperdoll"));
        defaultFemaleDoll = new Paperdoll(ip.getGuiElement("default_female_paperdoll"));
        
        setMinimumSize(new Dimension(1024, 768));
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        initComponents();
    }
    
    private void initComponents() {
        getContentPane().setBackground(Color.WHITE);
        getContentPane().setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 5;
        
        Paperdoll testDoll = new Paperdoll("data/images/misc/paperdoll/female.png");
        person.getInjuries().stream().forEach(inj ->
        {
            Color col;
            switch(inj.getLevel()) {
                case CHRONIC:
                    col =  new Color(255, 204, 255);
                    break;
                case DEADLY:
                    col = Color.RED;
                    break;
                case MAJOR:
                    col = Color.ORANGE;
                    break;
                case MINOR:
                    col = Color.YELLOW;
                    break;
                case NONE:
                    col = Color.WHITE;
                    break;
                default:
                    col = Color.WHITE;
                    break;
                
            }
            
            testDoll.setLocColor(inj.getLocation(), col);
        });
        JPanel testDollWrapper = new JPanel(null);
        testDollWrapper.setLayout(new BoxLayout(testDollWrapper, BoxLayout.LINE_AXIS));
        testDollWrapper.add(testDoll);
        testDollWrapper.setMaximumSize(new Dimension(256, Integer.MAX_VALUE));
        
        getContentPane().add(testDollWrapper, gbc);

        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        
        getContentPane().add(genBaseDataPanel(campaign, person), gbc);
        
        gbc.gridy = 1;
        
        getContentPane().add(genMedicalHistory(campaign, person), gbc);
        
        pack();
    }
    
    private JPanel genBaseDataPanel(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridLayout(10, 2));
        panel.setBorder(BorderFactory.createMatteBorder(3, 3, 0, 3, Color.BLACK));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        String name = p.getFullName();
        String[] nameParts = name.split(" ", -1);
        
        String familyName = "-";
        String givenNames = "";
        if(nameParts.length < 2) {
            givenNames = nameParts[0];
        } else {
            familyName = nameParts[nameParts.length - 1];
            givenNames = String.join(" ", Arrays.copyOf(nameParts, nameParts.length - 1));
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        GregorianCalendar birthday = p.getBirthday();
        dateFormat.setCalendar(birthday);
        String birthdayString = dateFormat.format(birthday.getTime());
        GregorianCalendar now = c.getCalendar();
        int ageInMonths = (now.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)) * 12
            + now.get(Calendar.MONTH) - birthday.get(Calendar.MONTH);
        
        String phenotype = (p.getPhenotype() != Person.PHENOTYPE_NONE) ? p.getPhenotypeName() : "baseline";
        
        Force f = c.getForceFor(p);
        String force = (null != f) ? f.getFullName() : "-";
        
        Person doc = c.getPerson(p.getDoctorId());
        String doctor = "none";
        if((null != doc) && doc.isActive()) {
            doctor = doc.getFullTitle();
        }
        panel.add(genLabel("Family name"));
        panel.add(genLabel("Given name(s)"));
        panel.add(genWrittenText(familyName, true));
        panel.add(genWrittenText(givenNames, true));
        panel.add(genLabel("Date of birth"));
        panel.add(genLabel("Age yrs., mons."));
        panel.add(genWrittenText(birthdayString, true));
        panel.add(genWrittenText(String.format("%d, %d", ageInMonths / 12, ageInMonths % 12), true));
        panel.add(genLabel("Gender"));
        panel.add(genLabel("Phenotype"));
        panel.add(genWrittenText(p.isMale() ? "M" : "F", true));
        panel.add(genWrittenText(phenotype, true));
        panel.add(genLabel("Assigned to unit"));
        panel.add(genLabel(""));
        panel.add(genWrittenText(force, true));
        panel.add(genWrittenText("", true));
        panel.add(genLabel("Assigned medical staff"));
        panel.add(genLabel("Last check-up"));
        panel.add(genWrittenText(doctor, true));
        panel.add(genWrittenText("", true));
        return panel;
    }
    
    private JPanel genMedicalHistory(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel("Past medical history"));
        panel.add(genWrittenText("", false));
        
        return panel;
    }
    
    private JLabel genLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Courier New", Font.PLAIN, 16));
        label.setForeground(Color.GRAY);
        return label;
    }
    
    private JLabel genWrittenText(String text, boolean withUnderline) {
        JLabel label = new JLabel("    " + text);
        label.setFont(new Font("Bradley Hand ITC", Font.BOLD, 20));
        if(withUnderline) {
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Color.BLACK));
        }
        return label;
    }
    
}

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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.joda.time.chrono.GJChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ExtraData;
import mekhq.campaign.LogEntry;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryLevel;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.gui.view.Paperdoll;

public class MedicalViewDialog extends JDialog {
    private static final long serialVersionUID = 6178230374580087883L;
    private static final String MENU_CMD_SEPARATOR = ","; //$NON-NLS-1$
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    private final static DateTimeFormatter DATE_FORMATTER =
        DateTimeFormat.forPattern("yyyy-MM-dd").withChronology(GJChronology.getInstanceUTC()); //$NON-NLS-1$

    private static final ExtraData.Key<String> DOCTOR_NOTES = new ExtraData.StringKey("doctor_notes"); //$NON-NLS-1$
    // TODO: Custom paper dolls
    @SuppressWarnings("unused")
    private static final ExtraData.Key<String> PAPERDOLL = new ExtraData.StringKey("paperdoll_xml_file"); //$NON-NLS-1$

    private final Campaign campaign;
    private final Person person;
    private ResourceBundle resourceMap = null;

    private Paperdoll defaultMaleDoll;
    private Paperdoll defaultFemaleDoll;
    private JPanel dollWrapper;
    private Paperdoll doll;
    private JPanel injuryPanel;
    private JTextArea notesArea;

    private ActionListener dollActionListener;

    private transient Font labelFont;
    private transient Font handwritingFont;
    private transient Color labelColor;
    private transient ImageIcon healImageIcon;
    
    private boolean gmMode;

    public MedicalViewDialog(Window parent, Campaign c, Person p, IconPackage ip) {
        super();
        this.campaign = Objects.requireNonNull(c);
        this.person = Objects.requireNonNull(p);
        //this.iconPackage = Objects.requireNonNull(ip);
        resourceMap = ResourceBundle.getBundle("mekhq.resources.MedicalViewDialog", new EncodeControl()); //$NON-NLS-1$
        
        // Preload default paperdolls
        try(InputStream fis = new FileInputStream(ip.getGuiElement("default_male_paperdoll"))) { //$NON-NLS-1$
            defaultMaleDoll = new Paperdoll(fis);
        } catch(IOException e) {
            MekHQ.logError(e);
        }
        try(InputStream fis = new FileInputStream(ip.getGuiElement("default_female_paperdoll"))) { //$NON-NLS-1$
            defaultFemaleDoll = new Paperdoll(fis);
        } catch(IOException e) {
            MekHQ.logError(e);
        }
        
        setPreferredSize(new Dimension(1024, 840));
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        
        labelFont = UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.PLAIN, 16); //$NON-NLS-1$
        try(InputStream fis = new FileInputStream("data/fonts/angelina.TTF")) { //$NON-NLS-1$
            handwritingFont = Font.createFont(Font.TRUETYPE_FONT, fis).deriveFont(Font.PLAIN, 22);
        } catch (FontFormatException | IOException e) {
            handwritingFont = null;
        }
        labelColor = new Color(170, 170, 170);
        healImageIcon = new ImageIcon(new ImageIcon("data/images/misc/medical.png").getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)); //$NON-NLS-1$
        
        dollActionListener = ae -> {
            final BodyLocation loc = BodyLocation.of(ae.getActionCommand());
            final boolean locationPicked = !loc.readableName.isEmpty();
            Point mousePos = doll.getMousePosition();
            JPopupMenu popup = new JPopupMenu();
            if(locationPicked) {
                JLabel header = new JLabel(Utilities.capitalize(loc.readableName));
                header.setFont(UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.BOLD)); //$NON-NLS-1$
                popup.add(header);
                popup.addSeparator();
            }
            if(locationPicked) {
                ActionListener addActionListener = addEvent -> {
                    String[] commands = addEvent.getActionCommand().split(MENU_CMD_SEPARATOR, 2);
                    InjuryType addIType = InjuryType.byKey(commands[0]);
                    int severity = Integer.valueOf(commands[1]);
                    person.addInjury(addIType.newInjury(campaign, person, loc, severity));
                    revalidate();
                };
                JMenu addMenu = new JMenu(resourceMap.getString("menuAdd.text")); //$NON-NLS-1$
                InjuryType.getAllTypes().stream().filter(it -> it.isValidInLocation(loc))
                    .sorted((it1, it2) -> it1.getSimpleName().compareToIgnoreCase(it2.getSimpleName()))
                    .forEach(it -> {
                        IntStream.range(1, it.getMaxSeverity() + 1).forEach(severity -> {
                        JMenuItem add = new JMenuItem(resourceMap.getString("menuMore.text") + it.getSimpleName(severity)); //$NON-NLS-1$
                        add.setActionCommand(it.getKey() + MENU_CMD_SEPARATOR + severity);
                        add.addActionListener(addActionListener);
                        addMenu.add(add);
                    });
                });
                popup.add(addMenu);
            } else {
                JMenuItem edit = new JMenuItem(resourceMap.getString("menuNewInjury.text"), //$NON-NLS-1$
                    UIManager.getIcon("FileView.fileIcon")); //$NON-NLS-1$
                popup.add(edit);
            }
            JMenuItem remove = new JMenuItem(loc.readableName.isEmpty() ? resourceMap.getString("menuHealAll.text") : resourceMap.getString("menuHeal.text"), healImageIcon); //$NON-NLS-1$ //$NON-NLS-2$
            if(locationPicked && p.getInjuriesByLocation(loc).isEmpty()) {
                remove.setEnabled(false);
            } else {
                remove.addActionListener(rae -> {
                    person.getInjuries().stream().filter(inj -> !locationPicked || (inj.getLocation() == loc))
                        .forEach(inj -> person.removeInjury(inj));
                    revalidate();
                });
            }
            popup.add(remove);
            Dimension popupSize = popup.getPreferredSize();
            popup.show(doll, (int) (mousePos.getX() - popupSize.getWidth()) + 10, (int) mousePos.getY() - 10);

        };
        
        setBackground(Color.WHITE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        Container scrollPanel = new JPanel();
        getContentPane().add(new JScrollPane(scrollPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        initComponents(scrollPanel);
        
        JButton okayButton = new JButton(resourceMap.getString("buttonDone.text")); //$NON-NLS-1$
        okayButton.addActionListener(ae -> {
            if(!notesArea.getText().isEmpty()) {
                p.getExtraData().set(DOCTOR_NOTES, notesArea.getText());
            }
            setVisible(false);
        });
        okayButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        getContentPane().add(okayButton);
        pack();
    }
    
    private void initComponents(Container cont) {
        cont.setBackground(Color.WHITE);
        cont.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 6;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        
        dollWrapper = new JPanel(null);
        dollWrapper.setLayout(new BoxLayout(dollWrapper, BoxLayout.Y_AXIS));
        dollWrapper.setMinimumSize(new Dimension(256, 768));
        dollWrapper.setMaximumSize(new Dimension(256, Integer.MAX_VALUE));
        dollWrapper.setOpaque(false);
        dollWrapper.setAlignmentY(Component.TOP_ALIGNMENT);
        cont.add(dollWrapper, gbc);

        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        
        cont.add(genBaseData(campaign, person), gbc);
        
        gbc.gridy = 1;
        
        cont.add(genMedicalHistory(campaign, person), gbc);
        
        gbc.gridy = 2;
        
        cont.add(genAllergies(campaign, person), gbc);

        gbc.gridy = 3;
        
        cont.add(genIllnesses(campaign, person), gbc);

        gbc.gridy = 4;
        
        cont.add(injuryPanel = new JPanel(), gbc);

        gbc.gridy = 5;
        
        cont.add(genNotes(campaign, person), gbc);
    }

    @Override
    public void validate() {
        dollWrapper.setVisible(false);
        fillDoll(dollWrapper, campaign, person);
        dollWrapper.setVisible(true);
        injuryPanel.setVisible(false);
        fillInjuries(injuryPanel, campaign, person);
        injuryPanel.setVisible(true);
        super.validate();
    }
    
    private JPanel fillDoll(JPanel panel, Campaign c, Person p) {
        panel.removeAll();
        
        if(null != doll) {
            doll.removeActionListener(dollActionListener);
        }
        doll = person.isMale() ? defaultMaleDoll : defaultFemaleDoll;
        doll.clearLocColors();
        doll.clearLocTags();
        doll.setHighlightColor(new Color(170, 170, 255));
        Arrays.stream(BodyLocation.values())
            .filter(bl -> p.hasInjury(bl))
            .forEach(bl -> {
                if(person.isLocationMissing(bl) && !person.isLocationMissing(bl.parent)) {
                    doll.setLocTag(bl, "lost"); //$NON-NLS-1$
                } else if(!person.isLocationMissing(bl)) {
                    InjuryLevel level = getMaxInjuryLevel(person, bl);
                    Color col;
                    switch(level) {
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
                    doll.setLocColor(bl, col);
                }
            });
        if(isGMMode()) {
            doll.addActionListener(dollActionListener);
        }
        panel.add(doll);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel genBaseData(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridLayout(10, 2));
        panel.setBorder(BorderFactory.createMatteBorder(3, 3, 0, 3, Color.BLACK));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        String name = p.getFullName();
        String[] nameParts = name.split(" ", -1); //$NON-NLS-1$
        
        String familyName = "-"; //$NON-NLS-1$
        String givenNames = ""; //$NON-NLS-1$
        if(nameParts.length < 2) {
            givenNames = nameParts[0];
        } else {
            familyName = nameParts[nameParts.length - 1];
            givenNames = String.join(" ", Arrays.copyOf(nameParts, nameParts.length - 1)); //$NON-NLS-1$
        }
        
        GregorianCalendar birthday = (GregorianCalendar) p.getBirthday().clone();
        DATE_FORMAT.setCalendar(birthday);
        String birthdayString = DATE_FORMAT.format(birthday.getTime());
        GregorianCalendar now = (GregorianCalendar) c.getCalendar().clone();
        int ageInMonths = (now.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)) * 12
            + now.get(Calendar.MONTH) - birthday.get(Calendar.MONTH);
        
        String phenotype = (p.getPhenotype() != Person.PHENOTYPE_NONE) ? p.getPhenotypeName() : resourceMap.getString("baselinePhenotype.text"); //$NON-NLS-1$
        
        Force f = c.getForceFor(p);
        String force = (null != f) ? f.getFullName() : "-"; //$NON-NLS-1$
        
        Person doc = c.getPerson(p.getDoctorId());
        String doctor = resourceMap.getString("none.text"); //$NON-NLS-1$
        if((null != doc) && doc.isActive()) {
            doctor = doc.getFullTitle();
        }
        panel.add(genLabel(resourceMap.getString("familyName.text"))); //$NON-NLS-1$
        panel.add(genLabel(resourceMap.getString("givenNames.text"))); //$NON-NLS-1$
        panel.add(genWrittenPanel(familyName));
        panel.add(genWrittenPanel(givenNames));
        panel.add(genLabel(resourceMap.getString("birthDate.text"))); //$NON-NLS-1$
        panel.add(genLabel(resourceMap.getString("age.text"))); //$NON-NLS-1$
        panel.add(genWrittenPanel(birthdayString));
        panel.add(genWrittenPanel(String.format(resourceMap.getString("age.format"), ageInMonths / 12, ageInMonths % 12))); //$NON-NLS-1$
        panel.add(genLabel(resourceMap.getString("gender.text"))); //$NON-NLS-1$
        panel.add(genLabel(resourceMap.getString("phenotype.text"))); //$NON-NLS-1$
        panel.add(genWrittenPanel(p.isMale() ? resourceMap.getString("genderMale.text") : resourceMap.getString("genderFemale.text"))); //$NON-NLS-1$ //$NON-NLS-2$
        panel.add(genWrittenPanel(phenotype));
        panel.add(genLabel(resourceMap.getString("assignedTo.text"))); //$NON-NLS-1$
        panel.add(genLabel("")); //$NON-NLS-1$
        panel.add(genWrittenPanel(force));
        panel.add(genWrittenPanel("")); //$NON-NLS-1$
        panel.add(genLabel(resourceMap.getString("assignedDoctor.text"))); //$NON-NLS-1$
        panel.add(genLabel(resourceMap.getString("lastCheckup.text"))); //$NON-NLS-1$
        panel.add(genWrittenPanel(doctor));
        panel.add(genWrittenPanel("")); //$NON-NLS-1$
        return panel;
    }
    
    private JPanel genMedicalHistory(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("medicalHistory.text"))); //$NON-NLS-1$
        Map<String, List<LogEntry>> groupedEntries = p.getPersonnelLog().stream()
            .filter(entry -> entry.isType(Person.LOGTYPE_MEDICAL))
            .sorted((entry1, entry2) -> entry1.getDate().compareTo(entry2.getDate()))
            .collect(Collectors.groupingBy(entry -> DATE_FORMAT.format(entry.getDate())));
        groupedEntries.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
            .forEachOrdered(e -> {
                if(e.getValue().size() > 1) {
                    panel.add(genWrittenText(e.getKey()));
                    e.getValue().forEach(entry -> {
                        JPanel wrapper = new JPanel();
                        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
                        wrapper.setOpaque(false);
                        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                        wrapper.add(Box.createHorizontalStrut(60));
                        wrapper.add(genWrittenText(String.format(resourceMap.getString("historyText.format"), entry.getDesc()))); //$NON-NLS-1$
                        panel.add(wrapper);
                    });
                } else {
                    panel.add(genWrittenText(String.format(resourceMap.getString("historyDateAndText.format"), e.getKey(), e.getValue().get(0).getDesc()))); //$NON-NLS-1$
                }
            });
        return panel;
    }
    
    private JPanel genAllergies(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("allergies.text"))); //$NON-NLS-1$
        panel.add(genWrittenText("")); //$NON-NLS-1$
        
        return panel;
    }
    
    private JPanel genIllnesses(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("illnesses.text"))); //$NON-NLS-1$
        panel.add(genWrittenText("")); //$NON-NLS-1$
        
        return panel;
    }
    
    /** Get the maximum injury level in the specified location */
    private InjuryLevel getMaxInjuryLevel(Person p, BodyLocation loc) {
        return p.getInjuries().stream().filter(inj -> !inj.isHidden() && (inj.getLocation() == loc))
            .sorted((inj1, inj2) -> Integer.compare(inj2.getLevel().ordinal(), inj1.getLevel().ordinal()))
            .findFirst().map(Injury::getLevel).orElse(InjuryLevel.NONE);
    }
    
    /** Compiles a list of body locations stream ordered by the maximum injury level in that location */ 
    private Stream<BodyLocation> maxInjurylevelLocationStream(Person p) {
        Map<BodyLocation, InjuryLevel> levelMap = new HashMap<>();
        Arrays.stream(BodyLocation.values())
            .filter(bl -> p.hasInjury(bl))
            .forEach(bl -> {
                levelMap.put(bl, getMaxInjuryLevel(p, bl));
            });
        return levelMap.entrySet().stream().sorted((entry1, entry2) ->
            Integer.compare(entry2.getValue().ordinal(), entry1.getValue().ordinal())
        ).map(Map.Entry::getKey);
    }
    
    private JPanel fillInjuries(JPanel panel, Campaign c, Person p) {
        panel.removeAll();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(genLabel(resourceMap.getString("injuries.text"))); //$NON-NLS-1$
        
        maxInjurylevelLocationStream(p).forEachOrdered(bl -> {
            JPanel blWrapper = new JPanel();
            blWrapper.setLayout(new BoxLayout(blWrapper, BoxLayout.X_AXIS));
            blWrapper.setOpaque(false);
            blWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            blWrapper.add(Box.createHorizontalStrut(30));
            blWrapper.add(genWrittenText(Utilities.capitalize(bl.readableName)));
            panel.add(blWrapper);
            
            p.getInjuriesByLocation(bl).stream()
                .sorted((inj1, inj2) -> Integer.compare(inj2.getLevel().ordinal(), inj1.getLevel().ordinal()))
                .forEachOrdered(inj -> {
                    JLabel injLabel = null;
                    if(inj.getType().isPermanent()) {
                        injLabel = genWrittenText(String.format(resourceMap.getString("injuriesText.format"), //$NON-NLS-1$
                            inj.getType().getSimpleName(), inj.getStart().toString(DATE_FORMATTER)));
                    } else if(inj.isPermanent() || (inj.getTime() <= 0)) {
                        injLabel = genWrittenText(String.format(resourceMap.getString("injuriesPermanent.format"), //$NON-NLS-1$
                            inj.getType().getSimpleName(), inj.getStart().toString(DATE_FORMATTER)));
                    } else {
                        injLabel = genWrittenText(String.format(resourceMap.getString("injuriesTextAndDuration.format"), //$NON-NLS-1$
                            inj.getType().getSimpleName(), inj.getStart().toString(DATE_FORMATTER), genTimePeriod(inj.getTime())));
                    }
                    if(isGMMode()) {
                        injLabel.addMouseListener(new InjuryLabelMouseAdapter(injLabel, p, inj));
                    }
                    
                    JPanel wrapper = new JPanel();
                    wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
                    wrapper.setOpaque(false);
                    wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                    wrapper.add(Box.createHorizontalStrut(60));
                    wrapper.add(injLabel);
                    
                    panel.add(wrapper);
                });
        });
        
        return panel;
    }
    
    private JPanel genNotes(Campaign c, Person p) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(genLabel(resourceMap.getString("doctorsNotes.text"))); //$NON-NLS-1$
        
        String notes = p.getExtraData().get(DOCTOR_NOTES, ""); //$NON-NLS-1$
        notesArea = new JTextArea(notes);
        notesArea.setEditable(true);
        notesArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesArea.setFont(handwritingFont);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.X_AXIS));
        notesPanel.setOpaque(false);
        notesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesPanel.add(Box.createHorizontalStrut(30));
        notesPanel.add(notesArea);
        
        panel.add(notesPanel);
        
        return panel;
    }

    private JLabel genLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(labelFont);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(labelColor);
        return label;
    }
    
    private JPanel genWrittenPanel(String text) {
        JLabel label = genWrittenText(text);
        
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(Box.createHorizontalStrut(30));
        wrapper.add(label);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Color.BLACK));
        
        return wrapper;
    }
    
    private JLabel genWrittenText(String text) {
        JLabel label = new JLabel(text);
        label.setFont(handwritingFont);
        return label;
    }
    
    private String genTimePeriod(int days) {
        if(days <= 1) {
            return resourceMap.getString("durationOneDay.text"); //$NON-NLS-1$
        } else if(days < 21) {
            return String.format(resourceMap.getString("durationDays.format"), days); //$NON-NLS-1$
        } else if(days <= 12 * 7) {
            return String.format(resourceMap.getString("durationWeeks.format"), days * 1.0 / 7.0); //$NON-NLS-1$
        } else if(days <= 2 * 365) {
            return String.format(resourceMap.getString("durationMonths.format"), days * 12.0 / 365.0); //$NON-NLS-1$
        } else {
            return String.format(resourceMap.getString("durationYears.format"), days * 1.0 / 365.0); //$NON-NLS-1$
        }
    }
    
    public boolean isGMMode() {
        return gmMode;
    }
    
    public void setGMMode(boolean gmMode) {
        this.gmMode = gmMode;
    }
    
    private static class InjuryLabelMouseAdapter extends MouseAdapter {
        private final JLabel label;
        private final Person person;
        private final Injury injury;
        private ImageIcon healImageIcon;
        private ResourceBundle resourceMap = null;

        public InjuryLabelMouseAdapter(JLabel label, Person person, Injury injury) {
            this.label = label;
            this.person = person;
            this.injury = injury;
            this.healImageIcon = new ImageIcon(new ImageIcon("data/images/misc/medical.png").getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)); //$NON-NLS-1$
            this.resourceMap = ResourceBundle.getBundle("mekhq.resources.MedicalViewDialog", new EncodeControl()); //$NON-NLS-1$
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            label.setBackground(Color.LIGHT_GRAY);
            label.setOpaque(true);
            label.invalidate();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setBackground(Color.WHITE);
            label.setOpaque(false);
            label.invalidate();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getButton() == MouseEvent.BUTTON1) {
                JPopupMenu popup = new JPopupMenu();
                JLabel header = new JLabel(injury.getFluff());
                header.setFont(UIManager.getDefaults().getFont("Menu.font").deriveFont(Font.BOLD)); //$NON-NLS-1$
                popup.add(header);
                popup.addSeparator();
                JMenuItem edit = new JMenuItem(resourceMap.getString("menuEdit.text"), //$NON-NLS-1$
                    UIManager.getIcon("FileView.fileIcon")); //$NON-NLS-1$
                popup.add(edit);
                JMenuItem remove = new JMenuItem(resourceMap.getString("menuRemove.text"), healImageIcon); //$NON-NLS-1$
                remove.addActionListener(ae -> {
                    person.removeInjury(injury);
                    label.getRootPane().getParent().revalidate();
                });
                popup.add(remove);
                Dimension popupSize = popup.getPreferredSize();
                popup.show(e.getComponent(), e.getX() - (int) popupSize.getWidth() + 10, e.getY() - 10);
            }
        }
    }
}

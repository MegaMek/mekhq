package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.Kill;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.FileDialogs;

public class CampaignExportWizard extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = -7171621116865584010L;
    
    private JList<Force> forceList;
    private JList<Person> personList;
    private JList<Unit> unitList;
    private JList<Part> partList;
    private JCheckBox chkExportSettings = new JCheckBox();
    private JCheckBox chkExportContractOffers = new JCheckBox();
    private JCheckBox chkDestructiveExport = new JCheckBox();
    //private JCheckBox chkExportAssignedTechs = new JCheckBox();
    private ResourceBundle resourceMap;
    
    private Campaign sourceCampaign;
    private Campaign destinationCampaign;
    
    private Optional<File> destinationCampaignFile;
    
    public enum CampaignExportWizardState {
        ForceSelection,
        PersonSelection,
        UnitSelection,
        PartSelection,
        MiscellaneousSelection,
        DestinationFileSelection // this should always be last
    }
    
    public CampaignExportWizard(Campaign c) {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignExportWizard", new EncodeControl());
        chkExportSettings.setText(resourceMap.getString("chkExportSettings.text"));
        chkExportContractOffers.setText(resourceMap.getString("chkExportContractOffers.text"));
        chkDestructiveExport.setText(resourceMap.getString("chkDestructiveExport.text"));
        //chkExportAssignedTechs.setText(resourceMap.getString("chkExportAssignedTechs.text"));
        
        sourceCampaign = c;
        setupForceList();
        setupPersonList();
        setupUnitList();
        setupPartList();
        chkDestructiveExport.setToolTipText(resourceMap.getString("chkDestructiveExport.tooltip"));
    }
    
    public void display(CampaignExportWizardState state) {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        JLabel lblInstructions = new JLabel();
        getContentPane().add(lblInstructions, gbc);
        
        gbc.gridy++;
        
        JScrollPane scrollPane = new JScrollPane();
        switch(state) {
        case ForceSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.ForceSelection.text"));
            scrollPane.setViewportView(forceList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PersonSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.PersonSelection.text"));
            scrollPane.setViewportView(personList);
            getContentPane().add(scrollPane, gbc);
            break;
        case UnitSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.UnitSelection.text"));
            scrollPane.setViewportView(unitList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PartSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.PartSelection.text"));
            scrollPane.setViewportView(partList);
            getContentPane().add(scrollPane, gbc);
            break;
        case MiscellaneousSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.MiscSelection.text"));
            getContentPane().add(chkExportSettings, gbc);
            gbc.gridy++;
            getContentPane().add(chkExportContractOffers, gbc);
            gbc.gridy++;
            getContentPane().add(chkDestructiveExport, gbc);
            //gbc.gridy++;
            //getContentPane().add(chkExportAssignedTechs, gbc);
            break;
        case DestinationFileSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.Finalize.text"));
            JButton btnNewCampaign = new JButton(resourceMap.getString("btnNewCampaign.text"));
            btnNewCampaign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destinationCampaignFile = FileDialogs.saveCampaign(null, sourceCampaign);
                    if(destinationCampaignFile.isPresent()) {
                        exportToCampaign(destinationCampaignFile.get());
                        setVisible(false);
                    }
                }
            });
            getContentPane().add(btnNewCampaign, gbc);
            gbc.gridx++;
            
            JButton btnExistingCampaign = new JButton(resourceMap.getString("btnExistingCampaign.text"));
            btnExistingCampaign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destinationCampaignFile = FileDialogs.openCampaign(null);
                    if(destinationCampaignFile.isPresent()) {
                        exportToCampaign(destinationCampaignFile.get());
                        setVisible(false);
                    }
                }
            });
            getContentPane().add(btnExistingCampaign, gbc);
            gbc.gridx--;
        }
        
        gbc.gridy++;
        
        if(state != CampaignExportWizardState.DestinationFileSelection) {
            JButton btnNext = new JButton(resourceMap.getString("btnNext.text"));
            btnNext.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    nextButtonHandler(state);
                }
            });
            
            getContentPane().add(btnNext, gbc);
        }
        
        validate();
        pack();
        setLocationRelativeTo(getParent());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setVisible(true);
    }
    
    private void setupForceList() {
        forceList = new JList<>();
        DefaultListModel<Force> forceListModel = new DefaultListModel<>();
        for(Force force : sourceCampaign.getAllForces()) {
            forceListModel.addElement(force);
        }
        forceList.setModel(forceListModel);    
    }
    
    private void setupPersonList() {
        personList = new JList<>();
        DefaultListModel<Person> personListModel = new DefaultListModel<>();
        for(Person person : sourceCampaign.getActivePersonnel()) {
            personListModel.addElement(person);
        }
        personList.setModel(personListModel);
        personList.setCellRenderer(new PersonListCellRenderer());
    }
    
    private void setupUnitList() {
        unitList = new JList<>();
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        for(Unit unit : sourceCampaign.getUnits()) {
            unitListModel.addElement(unit);
        }
        unitList.setModel(unitListModel);    
        unitList.setCellRenderer(new UnitListCellRenderer());
    }
    
    private void setupPartList() {
        partList = new JList<>();
        DefaultListModel<Part> partListModel = new DefaultListModel<>();
        for(Part part : sourceCampaign.getSpareParts()) {
            // if the part isn't part of some other activity
            if(!part.isReservedForRefit() &&
                    !part.isReservedForReplacement() &&
                    !part.isBeingWorkedOn() &&
                    part.isPresent()) {
                partListModel.addElement(part);
            }
        }
        partList.setModel(partListModel);    
    }
    
    /**
     * updates the person list based on changes to the force selection list
     * and unit selection list
     */
    private void updatePersonList() {
        List<Integer> selectedIndices = Arrays.stream(personList.getSelectedIndices())
                .boxed()
                .collect(Collectors.toList()); 
        
        for(Force force : forceList.getSelectedValuesList()) {
            for(UUID unitID : force.getAllUnits()) {
                Unit unit = sourceCampaign.getUnit(unitID);
                
                for(Person person : unit.getActiveCrew()) {
                    // this approach recurs throughout the class, and I
                    // couldn't find any better way to select multiple items in a JList
                    personList.setSelectedValue(person, false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
                
                if(unit.getTech() != null) {
                    personList.setSelectedValue(unit.getTech(), false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
            }
            
            if(force.getTechID() != null) {
                personList.setSelectedValue(sourceCampaign.getPerson(force.getTechID()), false);
                selectedIndices.add(personList.getSelectedIndex());
            }
        }
        
        for(Unit unit : unitList.getSelectedValuesList()) { 
            for(Person person : unit.getActiveCrew()) {
                personList.setSelectedValue(person, false);
                selectedIndices.add(personList.getSelectedIndex());
            }
            
            if(unit.getTech() != null) {
                personList.setSelectedValue(unit.getTech(), false);
                selectedIndices.add(personList.getSelectedIndex());
            }
        }
        
        // somewhat awkward syntax but the person list expects an int array
        // and all we have is a list
        personList.setSelectedIndices(selectedIndices.stream().mapToInt(i->i).toArray());
    }
    
    /**
     * updates the unit list based on changes to the force selection list
     * and person selection list, without losing existing selections
     */
    private void updateUnitList() {
        List<Integer> selectedIndices = Arrays.stream(unitList.getSelectedIndices())
                .boxed()
                .collect(Collectors.toList()); 
        
        for(Force force : forceList.getSelectedValuesList()) {
            for(UUID unitID : force.getAllUnits()) {
                Unit unit = sourceCampaign.getUnit(unitID);
                
                unitList.setSelectedValue(unit, false);
                selectedIndices.add(unitList.getSelectedIndex());
            }
        }
        
        for(Person person : personList.getSelectedValuesList()) { 
            if(person.getUnitId() != null) {
                Unit unit = sourceCampaign.getUnit(person.getUnitId());
                
                unitList.setSelectedValue(unit, false);
                selectedIndices.add(unitList.getSelectedIndex());
            }
        }
        
        // somewhat awkward syntax but the person list expects an int array
        // and all we have is a list
        unitList.setSelectedIndices(selectedIndices.stream().mapToInt(i->i).toArray());
    }
    
    private void nextButtonHandler(CampaignExportWizardState state) {
        switch(state) {
        case ForceSelection:
            updatePersonList();
            updateUnitList();
            break;
        case PersonSelection:
            updateUnitList();
            break;
        case UnitSelection:
            updatePersonList();
            break;
        }
        
        display(CampaignExportWizardState.values()[state.ordinal() + 1]);
    }
    
    /**
     * Carry out the campaign export.
     * @param file Destination file.
     * @return Whether or not the operation succeeded.
     */
    private boolean exportToCampaign(File file) {
        boolean newCampaign = !file.exists();
        
        if(newCampaign) {
            destinationCampaign = new Campaign();
            destinationCampaign.setApp(sourceCampaign.getApp());
        } else {
            try {
                FileInputStream fis = new FileInputStream(file);
                destinationCampaign = CampaignFactory.newInstance(sourceCampaign.getApp()).createCampaign(fis);
                // Restores all transient attributes from serialized objects
                destinationCampaign.restore();
                destinationCampaign.cleanUp();
                fis.close();
            } catch (NullEntityException ex) {
                MekHQ.getLogger().error(this.getClass(), "exportToCampaign", 
                        "The following units could not be loaded by the campaign:\n" + ex.getError() + "\n\nPlease be sure to copy over any custom units before starting a new version of MekHQ.\nIf you believe the units listed are not customs, then try deleting the file data/mechfiles/units.cache and restarting MekHQ.\nIt is also possible that unit chassi and model names have changed across versions of MegaMek. You can check this by\nopening up MegaMek and searching for the units. Chassis and models can be edited in your MekHQ save file with a text editor.");
                return false;
            } catch (Exception ex) {
                MekHQ.getLogger().error(this.getClass(), "exportToCampaign", 
                        "The campaign file could not be loaded.\nPlease check the log file for details.");
                return false;
            } catch(OutOfMemoryError e) {
                MekHQ.getLogger().error(this.getClass(), "exportToCampaign", 
                        "MekHQ ran out of memory attempting to load the campaign file. \nTry increasing the memory allocated to MekHQ and reloading.\nSee the FAQ at http://megamek.org for details.");
                return false;
            }
        }
        
        if(chkExportSettings.isSelected()) {
            destinationCampaign.setCampaignOptions(sourceCampaign.getCampaignOptions());
        }
        
        if(chkExportContractOffers.isSelected()) {
            for(Contract contract : sourceCampaign.getContractMarket().getContracts()) {
                destinationCampaign.getContractMarket().getContracts().add(contract);
            }
        }
        
        // forces aren't moved/copied over, we just use the force selection to pre-populate the list of people and units 
        // to be exported
        
        for(Unit unit : unitList.getSelectedValuesList()) {
            if(destinationCampaign.getUnit(unit.getId()) != null) {
                destinationCampaign.removeUnit(unit.getId());
            }
            
            destinationCampaign.importUnit(unit);
        }
        
        // overwrite any people with the same ID.
        for(Person person : personList.getSelectedValuesList()) {
            if(destinationCampaign.getPerson(person.getId()) != null) {
                destinationCampaign.removePerson(person.getId());
            }
            
            destinationCampaign.importPerson(person);
            for(Kill kill : sourceCampaign.getKillsFor(person.getId())) {
                destinationCampaign.importKill(kill);
            }
        }
        
        // there's just no way to overwrite parts
        for(Part part : partList.getSelectedValuesList()) {
            destinationCampaign.importPart(part);
            
            if(chkDestructiveExport.isSelected()) {
                sourceCampaign.removePart(part);
            }
        }
        
        boolean saved = CampaignGUI.saveCampaign(null, destinationCampaign, file);
        
        // having saved the destination campaign, we can now get rid of stuff in the source
        // campaign, if we're doing a destructive export
        // don't do it if we failed to save for some reason.
        if(saved && chkDestructiveExport.isSelected()) {
            for(Unit unit : unitList.getSelectedValuesList()) {            
                sourceCampaign.removeUnit(unit.getId());
            }
            
            for(Person person : personList.getSelectedValuesList()) {
                sourceCampaign.removePerson(person.getId(), true);
            }
            
            for(Part part : partList.getSelectedValuesList()) {
                sourceCampaign.removePart(part);
            }
        }

        return saved;
    }
    
    private class UnitListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JLabel) cmp).setText(((Unit) value).getName());
            return cmp;
        }
    }
    
    private class PersonListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Person person = (Person) value;
            String cellValue = String.format("%s (%s)", person.getFullName(), person.getPrimaryRoleDesc());
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
}

package mekhq.gui.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.Kill;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.FileDialogs;

public class CampaignExportWizard extends JDialog {
    private JList<Force> forceList;
    private JList<Person> personList;
    private JList<Unit> unitList;
    private JList<Part> partList;
    private JList<AtBContract> contractOfferList;
    private JCheckBox chkExportSettings = new JCheckBox("Export Campaign Settings");
    private JCheckBox chkExportContractOffers = new JCheckBox("Export Current Contract Offers");
    private JCheckBox chkDestructiveExport = new JCheckBox("Destructive Export");
    
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
        sourceCampaign = c;
        setupForceList();
        setupPersonList();
        setupUnitList();
        setupPartList();
        chkDestructiveExport.setToolTipText("Personnel, units and parts selected will be removed from current campaign.");
    }
    
    public void display(CampaignExportWizardState state) {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        JLabel lblInstructions = new JLabel(state.toString());
        getContentPane().add(lblInstructions, gbc);
        
        gbc.gridy++;
        
        JScrollPane scrollPane = new JScrollPane();
        switch(state) {
        case ForceSelection:
            scrollPane.setViewportView(forceList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PersonSelection:
            scrollPane.setViewportView(personList);
            getContentPane().add(scrollPane, gbc);
            break;
        case UnitSelection:
            scrollPane.setViewportView(unitList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PartSelection:
            scrollPane.setViewportView(partList);
            getContentPane().add(scrollPane, gbc);
            break;
        case MiscellaneousSelection:
            getContentPane().add(chkExportSettings, gbc);
            gbc.gridy++;
            getContentPane().add(chkExportContractOffers, gbc);
            gbc.gridy++;
            getContentPane().add(chkDestructiveExport, gbc);
            // then:
            // chkExportAssignedTechs
            break;
        case DestinationFileSelection:
            JTextArea txtDestinationCampaignFile = new JTextArea();
            txtDestinationCampaignFile.setEditable(false);
            
            getContentPane().add(txtDestinationCampaignFile, gbc);
            gbc.gridy++;
            
            JButton btnNewCampaign = new JButton("Export to New Campaign");
            btnNewCampaign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destinationCampaignFile = FileDialogs.saveCampaign(null, sourceCampaign);
                    if(destinationCampaignFile.isPresent()) {
                        exportToCampaign(destinationCampaignFile.get());
                        //setVisible(false);
                    }
                }
            });
            getContentPane().add(btnNewCampaign, gbc);
            gbc.gridx++;
            
            JButton btnExistingCampaign = new JButton("Export to Existing Campaign");
            btnExistingCampaign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destinationCampaignFile = FileDialogs.openCampaign(null);
                    if(destinationCampaignFile.isPresent()) {
                        exportToCampaign(destinationCampaignFile.get());
                        //setVisible(false);
                    }
                }
            });
            getContentPane().add(btnExistingCampaign, gbc);
            gbc.gridx--;
        }
        
        gbc.gridy++;
        
        if(state != CampaignExportWizardState.DestinationFileSelection) {
            JButton btnNext = new JButton("Next");
            btnNext.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updatePersonList();
                    updateUnitList();
                    display(CampaignExportWizardState.values()[state.ordinal() + 1]);
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
    }
    
    private void setupUnitList() {
        unitList = new JList<>();
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        for(Unit unit : sourceCampaign.getUnits()) {
            unitListModel.addElement(unit);
        }
        unitList.setModel(unitListModel);    
    }
    
    private void setupPartList() {
        partList = new JList<>();
        DefaultListModel<Part> partListModel = new DefaultListModel<>();
        for(Part part : sourceCampaign.getSpareParts()) {
            partListModel.addElement(part);
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
                    personList.setSelectedValue(person, false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
                
                if(unit.getTech() != null) {
                    personList.setSelectedValue(unit.getTech(), false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
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
        
        // overwrite any people with the same ID.
        for(Person person : personList.getSelectedValuesList()) {
            if(destinationCampaign.getPerson(person.getId()) != null) {
                destinationCampaign.removePerson(person.getId());
            }
            
            destinationCampaign.importPerson(person);
            for(Kill kill : sourceCampaign.getKillsFor(person.getId())) {
                destinationCampaign.importKill(kill);
            }
            
            if(chkDestructiveExport.isSelected()) {
                sourceCampaign.removePerson(person.getId(), true);
            }
        }
        
        for(Unit unit : unitList.getSelectedValuesList()) {
            if(destinationCampaign.getUnit(unit.getId()) != null) {
                destinationCampaign.removeUnit(unit.getId());
            }
            
            destinationCampaign.importUnit(unit);
            
            if(chkDestructiveExport.isSelected()) {
                sourceCampaign.removeUnit(unit.getId());
            }
        }
        
        // there's just no way to overwrite parts
        for(Part part : partList.getSelectedValuesList()) {
            destinationCampaign.importPart(part);
            
            if(chkDestructiveExport.isSelected()) {
                sourceCampaign.removePart(part);
            }
        }
        
        return CampaignGUI.saveCampaign(null, destinationCampaign, file);
    }
}

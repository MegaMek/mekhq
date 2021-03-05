package mekhq.gui.view;

import megamek.common.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator;

import java.util.List;

public class CompanyGenerationPanel {
    //region Variable Declarations
    private AbstractCompanyGenerator companyGenerator = null;

    // Data
    private List<Person> combatPersonnel;
    private List<Person> supportPersonnel;
    private List<Entity> entities;
    //endregion Variable Declarations

    //region Getters/Setters
    public AbstractCompanyGenerator getCompanyGenerator() {
        return companyGenerator;
    }

    public void setCompanyGenerator(AbstractCompanyGenerator companyGenerator) {
        this.companyGenerator = companyGenerator;
    }

    //region Data
    public List<Person> getCombatPersonnel() {
        return combatPersonnel;
    }

    public void setCombatPersonnel(List<Person> combatPersonnel) {
        this.combatPersonnel = combatPersonnel;
    }

    public List<Person> getSupportPersonnel() {
        return supportPersonnel;
    }

    public void setSupportPersonnel(List<Person> supportPersonnel) {
        this.supportPersonnel = supportPersonnel;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }
    //endregion Data
    //endregion Getters/Setters
/**

    public void generate() {
        if ((getCompanyGenerator() != null) && (JOptionPane.showConfirmDialog(getFrame(),
                resources.getString("CompanyGenerationPanel.OverwriteGenerationWarning.text"),
                resources.getString("CompanyGenerationPanel.OverwriteGenerationWarning.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)) {
            return;
        } else if (!validateOptions()) {
            return;
        }

        setCompanyGenerator(getCompanyGenerationMethod().getGenerator(getCampaign(), createOptionsFromPanel()));

        setCombatPersonnel(getCompanyGenerator().generateCombatPersonnel(getCampaign()));
        setSupportPersonnel(getCompanyGenerator().generateSupportPersonnel(getCampaign()));
        setEntities(getCompanyGenerator().generateUnits(getCampaign(), getCombatPersonnel()));
    }

    public void apply() {
        if (getCompanyGenerator() == null) {
            if (JOptionPane.showConfirmDialog(getFrame(),
                    resources.getString("CompanyGenerationPanel.ImmediateApplicationWarning.text"),
                    resources.getString("CompanyGenerationPanel.ImmediateApplicationWarning.title"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                generate();
                // Catch statement for bad data
                if (getCompanyGenerator() == null) {
                    return;
                }
            } else {
                return;
            }
        }

        getCompanyGenerator().applyToCampaign(getCampaign(), getCombatPersonnel(),
                getSupportPersonnel(), getEntities());
    }
 */
}

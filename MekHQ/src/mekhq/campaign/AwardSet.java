package mekhq.campaign;

import mekhq.campaign.personnel.Award;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class represents an award set.
 * @author Miguel Azevedo
 *
 */
@XmlRootElement(name="awards")
public class AwardSet{

    public AwardSet(){}

    @XmlElement(name = "award")
    private ArrayList<Award> awards;

    public List<Award> getAwards(){
        return Collections.unmodifiableList(awards);
    }
}

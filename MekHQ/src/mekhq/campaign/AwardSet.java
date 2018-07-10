package mekhq.campaign;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement(name="awards")
public class AwardSet{

    public AwardSet(){}

    @XmlElement(name = "award")
    private Set<Award> awards;

    public Set<Award> getAwards(){
        return awards;
    }
}

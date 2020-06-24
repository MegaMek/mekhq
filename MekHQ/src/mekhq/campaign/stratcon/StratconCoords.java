package mekhq.campaign.stratcon;

import megamek.common.Coords;
import megamek.common.util.HashCodeUtil;

public class StratconCoords extends Coords {
    private static final long serialVersionUID = 2660132431077309812L;

    public StratconCoords(int x, int y) {
        super(x, y);
    }
    
    public StratconCoords() {
        super(0, 0);
    }

    public StratconCoords translate(int direction) {
        Coords coords = translated(direction);
        int y = coords.getY();
        
        if(isXOdd() && coords.getX() != getX()) {
            y--;
        } else if (!isXOdd() && coords.getX() != getX()) {
            y++;
        }
        
        return new StratconCoords(coords.getX(), y);
    }
    
    /**
     * Get the hash code for these coords.
     * 
     * @return The <code>int</code> hash code for these coords.
     */
    @Override
    public int hashCode() {
        return (HashCodeUtil.hash1(getX() + 1337) ^ HashCodeUtil.hash1(getY() + 97331)) & 0x7FFFFFFF;
    }
    
    /**
     * Coords are equal if their x and y components are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        StratconCoords other = (StratconCoords) object;
        return other.getX() == this.getX() && other.getY() == this.getY();
    }
}

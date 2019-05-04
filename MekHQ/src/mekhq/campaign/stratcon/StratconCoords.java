package mekhq.campaign.stratcon;

import megamek.common.Coords;
import megamek.common.util.HashCodeUtil;

public class StratconCoords {
    private int x;
    private int y;
    
    public StratconCoords(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
    
    public StratconCoords() {
        setX(0);
        setY(0);
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }
    
    /**
     * Get the hash code for these coords.
     * 
     * @return The <code>int</code> hash code for these coords.
     */
    @Override
    public int hashCode() {
        return (HashCodeUtil.hash1(x + 1337) ^ HashCodeUtil.hash1(y + 97331)) & 0x7FFFFFFF;
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

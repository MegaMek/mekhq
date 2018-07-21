package mekhq.campaign.universe;

import static org.junit.Assert.*;

import org.junit.Test;

public class RandomFactionGeneratorTest {

    @Test
    public void testWeightedMap() {
        RandomFactionGenerator.WeightedMap<Integer> map = new RandomFactionGenerator.WeightedMap<>();
        int total = 0;
        for (int i = 0; i < 6; i++) {
            map.add(i, i);
            total += i;
        }
        
        assertEquals(map.size(), 5);
        assertEquals(map.lastKey().intValue(), total);
        assertEquals(map.ceilingEntry(1).getValue().intValue(), 1);
        assertEquals(map.ceilingEntry(2).getValue().intValue(), 2);
        assertEquals(map.ceilingEntry(10).getValue().intValue(), 4);
        assertEquals(map.ceilingEntry(11).getValue().intValue(), 5);
        assertEquals(map.ceilingEntry(15).getValue().intValue(), 5);
    }

}

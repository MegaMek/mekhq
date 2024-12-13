package mekhq.campaign.autoResolve;

import mekhq.campaign.autoResolve.helper.BalancedConsolidateForces;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BalancedConsolidateForcesTest {
    @Test
    void balanceTwoTeams() {
        var input = Arrays.asList(
            new BalancedConsolidateForces.ForceRepresentation(1, 1, new int[0], new int[] {2}),
            new BalancedConsolidateForces.ForceRepresentation(2, 1, new int[]{1, 2, 3, 4}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(3, 2, new int[] {5}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(4, 2, new int[] {6, 7}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(5, 2, new int[] {8, 9}, new int[0])
        );

        List<BalancedConsolidateForces.Container> result = BalancedConsolidateForces.balancedLists(input);
        // Check if balanced: all teams same top-level count

        var expected = List.of(
            new BalancedConsolidateForces.Container(2, 1, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(1, 1, new int[] {1, 2, 3, 4}, new BalancedConsolidateForces.Container[0])
            }),
            new BalancedConsolidateForces.Container(4, 2, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(3, 2, new int[] {5, 6, 7, 8, 9}, new BalancedConsolidateForces.Container[0])
            })
        );

        System.out.println("Balanced: " + BalancedConsolidateForces.isBalanced(result));

        assertTrue(BalancedConsolidateForces.isBalanced(result), "Not balanced");

        for (int i = 0; i < result.size(); i++) {
            // check topLevel
            assertEquals(expected.get(i).teamId(), result.get(i).teamId());
            assertEquals(expected.get(i).uid(), result.get(i).uid());
            assertTrue(result.get(i).isTop());

            for (int j = 0; j < result.get(i).subs().length; j++) {
                // check subLevel
                assertEquals(expected.get(i).subs()[j].teamId(), result.get(i).subs()[j].teamId());
                assertEquals(expected.get(i).subs()[j].uid(), result.get(i).subs()[j].uid());
                assertTrue(result.get(i).subs()[j].isLeaf());
            }
        }
    }

    @Test
    void balanceTwoVeryDifferentSizedTeams() {
        var input = Arrays.asList(
            new BalancedConsolidateForces.ForceRepresentation(1, 1, new int[0], new int[] {2}),
            new BalancedConsolidateForces.ForceRepresentation(2, 1, new int[]{1, 2, 3, 4}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(3, 2, new int[] {5}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(4, 2, new int[] {6, 7}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(5, 2, new int[] {8, 9}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(6, 2, new int[] {10, 11}, new int[] {7}),
            new BalancedConsolidateForces.ForceRepresentation(7, 2, new int[] {13, 14, 15, 16, 17, 18, 19, 20, 21}, new int[8]),
            new BalancedConsolidateForces.ForceRepresentation(8, 2, new int[] {22, 23, 24, 25, 26, 27, 28, 29, 30}, new int[0])
        );

        List<BalancedConsolidateForces.Container> result = BalancedConsolidateForces.balancedLists(input);
        // Check if balanced: all teams same top-level count

        var expected = List.of(
            new BalancedConsolidateForces.Container(2, 1, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(1, 1, new int[] {1, 2}, new BalancedConsolidateForces.Container[0])
            }),
            new BalancedConsolidateForces.Container(4, 1, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(3, 1, new int[] {3, 4}, new BalancedConsolidateForces.Container[0])
            }),
            new BalancedConsolidateForces.Container(8, 2, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(5, 2,
                    new int[] {5, 6, 7, 8, 9, 10},
                    new BalancedConsolidateForces.Container[0]),
                new BalancedConsolidateForces.Container(6, 2,
                    new int[] {11, 13, 14, 15, 16, 17},
                    new BalancedConsolidateForces.Container[0]),
                new BalancedConsolidateForces.Container(7, 2,
                    new int[] {18},
                    new BalancedConsolidateForces.Container[0]),
            }),
            new BalancedConsolidateForces.Container(11, 2, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(9, 2,
                    new int[] {19, 20, 21, 22, 23, 24},
                    new BalancedConsolidateForces.Container[0]),
                new BalancedConsolidateForces.Container(10, 2,
                    new int[] {25, 26, 27, 28, 29, 30},
                    new BalancedConsolidateForces.Container[0])
            })
        );

        System.out.println("Balanced: " + BalancedConsolidateForces.isBalanced(result));

        assertTrue(BalancedConsolidateForces.isBalanced(result), "Not balanced");

        for (int i = 0; i < result.size(); i++) {
            // check topLevel
            assertEquals(expected.get(i).teamId(), result.get(i).teamId());
            assertEquals(expected.get(i).uid(), result.get(i).uid());
            assertTrue(result.get(i).isTop());

            for (int j = 0; j < result.get(i).subs().length; j++) {
                // check subLevel
                assertEquals(expected.get(i).subs()[j].teamId(), result.get(i).subs()[j].teamId());
                assertEquals(expected.get(i).subs()[j].uid(), result.get(i).subs()[j].uid());
                assertTrue(result.get(i).subs()[j].isLeaf());
            }
        }
    }


    @Test
    void balanceTwoVeryUnbalancedSizes() {
        var input = Arrays.asList(
            new BalancedConsolidateForces.ForceRepresentation(1, 1, new int[0], new int[] {2}),
            new BalancedConsolidateForces.ForceRepresentation(2, 1, new int[]{1, 2, 3, 4}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(3, 2, new int[] {5}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(4, 2, new int[] {6, 7}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(5, 2, new int[] {8, 9}, new int[0]),
            new BalancedConsolidateForces.ForceRepresentation(6, 2, new int[] {10, 11}, new int[] {7}),
            new BalancedConsolidateForces.ForceRepresentation(7, 2, new int[] {13, 14, 15, 16, 17, 18, 19, 20, 21}, new int[8]),
            new BalancedConsolidateForces.ForceRepresentation(8, 2, new int[] {22, 23, 24, 25}, new int[0])
        );

        List<BalancedConsolidateForces.Container> result = BalancedConsolidateForces.balancedLists(input);
        // Check if balanced: all teams same top-level count

        var expected = List.of(
            new BalancedConsolidateForces.Container(2, 1, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(1, 1, new int[] {1, 2, 3, 4}, new BalancedConsolidateForces.Container[0])
            }),
            new BalancedConsolidateForces.Container(7, 2, new int[0], new BalancedConsolidateForces.Container[] {
                new BalancedConsolidateForces.Container(3, 2,
                    new int[] {5, 6, 7, 8, 9, 10},
                    new BalancedConsolidateForces.Container[0]),
                new BalancedConsolidateForces.Container(4, 2,
                    new int[] {11, 13, 14, 15, 16, 17},
                    new BalancedConsolidateForces.Container[0]),
                new BalancedConsolidateForces.Container(5, 2,
                    new int[] {18, 19, 20, 21, 22, 23},
                    new BalancedConsolidateForces.Container[0]),
                new BalancedConsolidateForces.Container(6, 2,
                    new int[] {24, 25},
                    new BalancedConsolidateForces.Container[0]),
            })
        );

        System.out.println("Balanced: " + BalancedConsolidateForces.isBalanced(result));

        assertTrue(BalancedConsolidateForces.isBalanced(result), "Not balanced");

        for (int i = 0; i < result.size(); i++) {
            // check topLevel
            assertEquals(expected.get(i).teamId(), result.get(i).teamId());
            assertEquals(expected.get(i).uid(), result.get(i).uid());
            assertTrue(result.get(i).isTop());

            for (int j = 0; j < result.get(i).subs().length; j++) {
                // check subLevel
                assertEquals(expected.get(i).subs()[j].teamId(), result.get(i).subs()[j].teamId());
                assertEquals(expected.get(i).subs()[j].uid(), result.get(i).subs()[j].uid());
                assertTrue(result.get(i).subs()[j].isLeaf());
            }
        }
    }
}

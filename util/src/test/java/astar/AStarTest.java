package astar;

import astar.model.SpaceProblem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AStarTest {

    @Test
    public void SearchNodeTest2D() {
        List<Solution> path = new AStarGoalFind(
                SpaceProblem.SpaceFind.PROBLEM,
                SpaceProblem.at(1, 1),
                SpaceProblem.at(3, 3)
        ).plan;

        assertEquals("[1,1, 2,1, 2,2, 3,2, 3,3]", path.toString());
        assertEquals(path.size(), 5);
    }













}


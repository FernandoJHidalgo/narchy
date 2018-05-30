package astar;

import astar.impl.HashPriorityQueue;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DatastructuresTest {

	@Test
	public void hashPriorityQueueTest() {

		
		
		class InconsistentComparator implements Comparator<Integer> {
			public int compare(Integer x, Integer y) {
				return 0;
			}
		}
		HashPriorityQueue<Integer, Integer> Q = new HashPriorityQueue<Integer, Integer>(
				new InconsistentComparator());

		Q.add(0, 0);
		Q.add(1, 1);
		Q.add(2, 2);
		Q.add(3, 3);

		
		
		Q.remove(1, 1);
		assertEquals(true, Q.contains(0));
		assertEquals(false, Q.contains(1));
		assertEquals(true, Q.contains(2));
		assertEquals(true, Q.contains(3));
		Q.remove(0, 0);
		assertEquals(false, Q.contains(0));
		assertEquals(true, Q.contains(2));
		assertEquals(true, Q.contains(3));
		Q.remove(3, 3);
		assertEquals(true, Q.contains(2));
		assertEquals(false, Q.contains(3));

		Q.clear();
		Q.add(0, 0);
		Q.add(1, 1);
		Q.add(2, 2);
		Q.add(3, 3);

		int x = Q.poll();
		
		
		assertEquals(0, Q.size());

		
		
		
		
		
		
		

	}

}

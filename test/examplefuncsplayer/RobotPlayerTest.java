package examplefuncsplayer;

import static org.junit.Assert.*;

import alpha.RubbleUtil;
import org.junit.Test;

public class RobotPlayerTest {

	@Test
	public void testSanity() {
		assertEquals(2, 1+1);
	}

	@Test
	public void testRubbleClearing() {
		assertEquals(0, RubbleUtil.getRoundsToMakeMovable(5));
		assertEquals(RubbleUtil.TOO_MANY_ROUNDS, RubbleUtil.getRoundsToMakeMovable(10000000));
		assertEquals(53, RubbleUtil.getRoundsToMakeMovable(4200));
		assertEquals(63, RubbleUtil.getRoundsToMakeMovable(7100));
		assertEquals(35, RubbleUtil.getRoundsToMakeMovable(1516));
	}

}

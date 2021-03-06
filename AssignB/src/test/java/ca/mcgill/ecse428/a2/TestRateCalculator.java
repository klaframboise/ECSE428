package ca.mcgill.ecse428.a2;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.mcgill.ecse428.a2.*;


public class TestRateCalculator {
	
	RateCalculator calc;
	static String from = "j5c 1t2";
	static String to = "g0m 1m0";
	static float length = 10f;
	static float width = 10f;
	static float height = 10f;
	static float weight = 1;
	static float delta = 0.005f;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		calc = new RateCalculator();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRegularRate() {
		float actual = calc.getRate(from, to, length, width, height, weight, RateCalculator.REGULAR);
		float expected = 18.14f;
		
		assertEquals(expected, actual, delta);
	}
	
	@Test
	public void testXpressRate() {
		float actual = calc.getRate(from, to, length, width, height, weight, RateCalculator.XPRESS);
		float expected = 23.41f;
		
		assertEquals(expected, actual, delta);
	}
	
	@Test
	public void testPriorityRate() {
		float actual = calc.getRate(from, to, length, width, height, weight, RateCalculator.PRIORITY);
		float expected = 40.87f;
		
		assertEquals(expected, actual, delta);
	}
	
	@Test
	public void testInvalidFromPostalCode() {
		
		try {
			calc.getRate("jjjjjj", to, length, width, height, weight, RateCalculator.REGULAR);
			fail("No exception caught");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			fail("Caught an unexpected exception");
		}

	}
	
	@Test
	public void testInvalidToPostalCodes() {
		
		try {
			calc.getRate(from, "gggggg", length, width, height, weight, RateCalculator.REGULAR);
			fail("No exception caught");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			fail("Caught an unexpected exception: " + e.getMessage());
		}
		
	}

	@Test
	/**
	 * Max length + girth = 300 cm
	 * girth = 2*height + 2*width
	 */
	public void testMaxLengthPlusGirth() {
		
		try {
			calc.getRate(from, to, 199, 30, 30, weight, RateCalculator.XPRESS);
			fail("No exception caught");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			fail("Caught an unexpected exception: " + e.getMessage());
		}
		
	}
	
	@Test
	/**
	 * Max length = 200 cm
	 */
	public void testMaxLength() {
		
		try {
			calc.getRate(from, to, 225, 1, 1, weight, RateCalculator.XPRESS);
			fail("No exception caught");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			fail("Caught an unexpected exception: " + e.getMessage());
		}
		
	}
	
	@Test
	/**
	 * Max length = 30 kg
	 */
	public void testMaxWeight() {
		
		try {
			calc.getRate(from, to, length, width, height, 31, RateCalculator.XPRESS);
			fail("No exception caught");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			fail("Caught an unexpected exception: " + e.getMessage());
		}
		
	}
	
	@Test
	/**
	 * Min measure accepted by api = 0.1 cm
	 */
	public void testMinMeasurement() {
		
		try {
			calc.getRate(from, to, .09f, 1f, 1f, weight, RateCalculator.XPRESS);
			fail("No exception caught");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			fail("Caught an unexpected exception: " + e.getMessage());
		}
		
	}
	
}

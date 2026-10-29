package com.dozingcatsoftware.dodge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterstitialHelperTest {
    private static final long MIN = 60_000L;

    @Test
    public void gapBetweenAds() {
        long shown = 20 * MIN;
        assertFalse(InterstitialHelper.capAllows(shown + (3 * MIN) - 1, shown, 0));
        assertTrue(InterstitialHelper.capAllows(shown + 3 * MIN, shown, 0));
    }

    @Test
    public void noAdInFirstThreeMinutes() {
        long never = -InterstitialHelper.MIN_INTERVAL_MS;
        assertFalse(InterstitialHelper.capAllows(2 * MIN, never, 0));
        assertTrue(InterstitialHelper.capAllows(3 * MIN, never, 0));
    }
}

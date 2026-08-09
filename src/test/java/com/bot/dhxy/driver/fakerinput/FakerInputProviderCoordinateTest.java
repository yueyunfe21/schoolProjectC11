package com.bot.dhxy.driver.fakerinput;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakerInputProviderCoordinateTest {

    @Test
    void primaryScreenPixelsMapToFakerInputAbsoluteRange() {
        assertEquals(0, FakerInputProvider.normalizeAbsoluteCoordinate(0, 2560));
        assertEquals(Short.MAX_VALUE, FakerInputProvider.normalizeAbsoluteCoordinate(2559, 2560));
        assertEquals(8451, FakerInputProvider.normalizeAbsoluteCoordinate(660, 2560));
    }

    @Test
    void offScreenTargetsFailClosedInsteadOfClampingToAnUnrelatedPixel() {
        assertThrows(IllegalArgumentException.class,
                () -> FakerInputProvider.normalizeAbsoluteCoordinate(-1, 2560));
        assertThrows(IllegalArgumentException.class,
                () -> FakerInputProvider.normalizeAbsoluteCoordinate(2560, 2560));
    }
}

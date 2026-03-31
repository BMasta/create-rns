package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import com.bmaster.createrns.util.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeCodecTest {
    @Test
    void flexibleCodecParsesSingleValueAsCollapsedRange() {
        var range = CodecAssertions.assertParses(Range.FLEXIBLE_CODEC, TestRegistryContexts.json(), "5");

        assertEquals(new Range(5, 5), range);
    }

    @Test
    void flexibleCodecRejectsInvertedRange() {
        CodecAssertions.assertFails(Range.FLEXIBLE_CODEC, TestRegistryContexts.json(), """
                {
                  "min": 7,
                  "max": 3
                }
                """, "Range min must not exceed max");
    }
}

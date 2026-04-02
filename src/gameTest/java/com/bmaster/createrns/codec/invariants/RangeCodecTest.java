package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.codec.Range;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class RangeCodecTest {
    @GameTest(template = "empty16x16")
    public void flexibleCodecParsesSingleValueAsCollapsedRange(GameTestHelper helper) {
        var range = CodecHelper.assertParses(helper, Range.FLEXIBLE_CODEC, CodecHelper.json(), "5",
                "collapsed range");

        helper.assertValueEqual(range, new Range(5, 5), "collapsed range value");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void flexibleCodecRejectsInvertedRange(GameTestHelper helper) {
        CodecHelper.assertFails(helper, Range.FLEXIBLE_CODEC, CodecHelper.json(), """
                {
                  "min": 7,
                  "max": 3
                }
                """, "Range min must not exceed max");
        helper.succeed();
    }
}

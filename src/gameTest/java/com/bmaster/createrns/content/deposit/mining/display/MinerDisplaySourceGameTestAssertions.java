package com.bmaster.createrns.content.deposit.mining.display;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

final class MinerDisplaySourceGameTestAssertions {
    static void assertTwoColumnFlapText(
            GameTestHelper helper, DisplayLinkContext context, List<List<MutableComponent>> text,
            List<String> expectedLines, String description
    ) {
        helper.assertTrue(context.flapDisplayContext == Boolean.TRUE,
                description + " should activate the two-column flap layout");
        helper.assertValueEqual(text.size(), expectedLines.size(), description + " flap row count");
        for (int row = 0; row < expectedLines.size(); row++) {
            String expectedLine = expectedLines.get(row);
            int separator = expectedLine.indexOf(": ");
            helper.assertTrue(separator >= 0, "Expected a key/value separator in " + expectedLine);

            var sections = text.get(row);
            helper.assertValueEqual(sections.size(), 2, description + " flap section count at row " + row);
            helper.assertValueEqual(sections.getFirst().getString(), expectedLine.substring(0, separator),
                    description + " flap key at row " + row);
            helper.assertValueEqual(sections.getLast().getString(), expectedLine.substring(separator + 2),
                    description + " flap value at row " + row);
        }
    }

    private MinerDisplaySourceGameTestAssertions() {
    }
}

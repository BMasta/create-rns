package com.bmaster.createrns.compat.aeronautics;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSItems;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class ScannerNavigationTargetPhysicsGameTest {
    @GameTest(template = "empty16x16")
    public void scannerHasNavigationTarget(GameTestHelper helper) {
        var target = NavigationTarget.ofStack(RNSItems.DEPOSIT_SCANNER.asStack());
        helper.assertTrue(target instanceof ScannerNavigationTarget,
                "Deposit scanner should use the scanner navigation target");
        helper.succeed();
    }
}

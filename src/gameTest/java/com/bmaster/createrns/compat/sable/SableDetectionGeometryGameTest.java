package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.CreateRNS;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class SableDetectionGeometryGameTest {
    @GameTest(template = "empty16x16")
    public void rotatedDetectionAreaRejectsEnclosingBoundsCorner(GameTestHelper helper) {
        var detectionArea = new AABB(-0.1, -0.5, -2, 0.1, 0.5, 2);
        var operatorPose = new Pose3d(
                new Vector3d(),
                new Quaterniond().rotationY(Math.PI / 4),
                new Vector3d(),
                new Vector3d(1)
        );

        helper.assertTrue(SableOperatingSublevelAdapter.intersectsDetectionArea(
                        detectionArea, BlockPos.ZERO, operatorPose, null),
                "A block crossing the rotated detection area should be detected");
        helper.assertFalse(SableOperatingSublevelAdapter.intersectsDetectionArea(
                        detectionArea, new BlockPos(-2, 0, 1), operatorPose, null),
                "A block only inside the enclosing bounds should not be detected");
        helper.succeed();
    }
}

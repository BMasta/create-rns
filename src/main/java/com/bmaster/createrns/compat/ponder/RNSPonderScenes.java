package com.bmaster.createrns.compat.ponder;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class RNSPonderScenes {
    protected static float calcAngle(float rpm, float dur) {
        return Math.round(rpm * dur / 60 / 20) * 360;
    }

    public static void mining(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mining", "Mining Deposits");
        scene.scaleSceneView(0.9f);

        BlockPos bearing = util.grid().at(2, 3, 2);
        BlockPos bearingNewPos = util.grid().at(2, 5, 2);
        BlockPos drill = util.grid().at(2, 2, 2);
        Vec3 underDrill = drill.getBottomCenter();
        Selection kinetics = util.select().position(2, 4, 2);
        Selection resonators = util.select().fromTo(2, 3, 2, 2, 4, 2);
        BlockPos depositUnderBearing = util.grid().at(2, 1, 2);

        BlockPos bearing2 = util.grid().at(1, 3, 2);
        BlockPos drill2 = util.grid().at(1, 2, 2);
        Selection minerTop2 = util.select().fromTo(1, 3, 2, 1, 4, 2);
        BlockPos depositTopRight = util.grid().at(0, 1, 4);
        BlockPos depositBottomRight = util.grid().at(0, 1, 0);

        Selection deposits = util.select().fromTo(0, 1, 0, 4, 1, 4);
        Selection depositsClaimableByMiner2 = util.select().fromTo(0, 1, 0, 3, 1, 4);

        var depStack = new ItemStack(RNSBlocks.IRON_DEPOSIT_BLOCK.get().asItem());
        ParticleEmitter depositParticle = scene.effects().particleEmitterWithinBlockSpace(
                new ItemParticleOption(ParticleTypes.ITEM, depStack), Vec3.ZERO);

        int rpm, dur;

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(deposits, Direction.NORTH);
        scene.idle(15);

        scene.overlay().showText(100)
                .pointAt(util.vector().topOf(depositUnderBearing))
                .attachKeyFrame()
                .placeNearTarget()
                .sharedText(ServerConfig.infiniteDeposits ? "mining_infinite" : "mining_finite");
        scene.idle(100);

        scene.world().showSection(util.select().position(bearing), Direction.NORTH);
        scene.idle(10);

        scene.overlay().showText(50)
                .pointAt(util.vector().blockSurface(bearing, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Miner bearings create contraptions used for mining");
        scene.idle(60);

        var drillLink = scene.world().showIndependentSection(util.select().position(drill), Direction.NORTH);
        scene.idle(20);
        scene.overlay().showText(100)
                .pointAt(util.vector().blockSurface(drill, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Each miner contraption must have exactly one drill head directly underneath in order to function");
        scene.idle(120);

        scene.overlay().showText(60)
                .pointAt(util.vector().blockSurface(drill, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("When assembled, the miner claims deposit blocks below in a certain area");
        scene.idle(70);

        scene.overlay().showControls(util.vector().topOf(bearing).subtract(.5, 0, 0), Pointing.DOWN, 10)
                .rightClick();
        scene.idle(10);

        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), deposits, 60);
        rpm = 100;
        dur = 300;
        scene.world().rotateBearing(bearing, calcAngle(rpm, dur), dur);
        scene.world().rotateSection(drillLink, 0, calcAngle(rpm, dur), 0, dur);
        scene.world().setKineticSpeed(util.select().position(bearing), rpm);
        scene.effects().emitParticles(underDrill, depositParticle, 2, dur);
        scene.idle(60);

        scene.world().setKineticSpeed(kinetics, rpm);
        scene.world().setKineticSpeed(minerTop2, -rpm);
        scene.world().showSection(kinetics, Direction.EAST);
        scene.world().showSection(minerTop2, Direction.EAST);
        var drillLink2 = scene.world().showIndependentSection(util.select().position(drill2), Direction.EAST);
        scene.idle(10);

        scene.overlay().showText(110)
                .pointAt(util.vector().blockSurface(depositTopRight, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Each deposit block can only be claimed by a single miner");
        scene.idle(40);

        scene.overlay().showControls(util.vector().topOf(bearing2).subtract(.5, 0, 0), Pointing.DOWN, 10)
                .rightClick();
        scene.idle(5);

        scene.overlay().showOutline(PonderPalette.RED, new Object(), depositsClaimableByMiner2, 75);
        rpm = -100;
        dur = 190;
        scene.world().rotateBearing(bearing2, calcAngle(rpm, dur), dur);
        scene.world().rotateSection(drillLink2, 0, calcAngle(rpm, dur), 0, dur);
        scene.world().setKineticSpeed(minerTop2, rpm);
        scene.idle(20);

        scene.overlay().showControls(util.vector().centerOf(depositBottomRight).subtract(1, 0, 0), Pointing.LEFT, 50)
                .showing(AllIcons.I_MTD_CLOSE);
        scene.idle(60);
        scene.world().hideIndependentSection(drillLink2, Direction.WEST);
        scene.world().hideSection(minerTop2, Direction.WEST);
        scene.idle(30);

        var cobblestone = new ItemStack(Items.COBBLESTONE);
        var cobblePos = drill.getCenter().add(new Vec3(-1.2, -0.2, 1.5));
        var nugget = new ItemStack(Items.IRON_NUGGET);
        var nuggetPos = drill.getCenter().add(new Vec3(-1.5, -0.2, 1.5));
        scene.world().createItemEntity(cobblePos, Vec3.ZERO, cobblestone);
        scene.idle(3);
        scene.world().createItemEntity(nuggetPos, Vec3.ZERO, nugget);
        scene.idle(10);

        scene.overlay().showText(375)
                .pointAt(nuggetPos)
                .attachKeyFrame()
                .placeNearTarget()
                .text("While some items can be mined without additional attachments...")
                .colored(PonderPalette.BLUE);
        scene.idle(70);

        scene.world().hideSection(resonators.add(util.select().position(bearingNewPos)), Direction.SOUTH);
        scene.idle(15);

        scene.world().setBlocks(resonators, RNSBlocks.RESONATOR_BLOCK.getDefaultState(), false);
        scene.world().setBlock(bearingNewPos, RNSBlocks.MINER_BEARING_BLOCK.getDefaultState().setValue(MinerBearingBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(util.select().position(bearingNewPos), Direction.NORTH);
        var resonatorsLink = scene.world().showIndependentSection(resonators, Direction.NORTH);
        scene.idle(15);

        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), resonators, 175);
        scene.idle(20);

        rpm = 100;
        dur = 1200;
        scene.world().setKineticSpeed(util.select().position(bearingNewPos), rpm);
        scene.world().rotateBearing(bearingNewPos, calcAngle(rpm, dur), dur);
        scene.world().rotateSection(resonatorsLink, 0, calcAngle(rpm, dur), 0, dur);
        scene.world().rotateSection(drillLink, 0, calcAngle(rpm, dur), 0, dur);
        scene.effects().emitParticles(underDrill, depositParticle, 2, dur);
        scene.idle(45);

        var rawIron = new ItemStack(Items.RAW_IRON);
        var rawIronPos = drill.getCenter().add(new Vec3(-1.5, -0.2, -1.5));
        scene.world().createItemEntity(rawIronPos, Vec3.ZERO, rawIron);
        scene.idle(10);

        scene.overlay().showText(200)
                .pointAt(rawIronPos)
                .attachKeyFrame()
                .placeNearTarget()
                .text("Others require an active catalyst")
                .colored(PonderPalette.GREEN);
        scene.idle(200);
    }
}

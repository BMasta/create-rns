package com.bmaster.createrns.compat.ponder;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlock;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlockEntity;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder.WorldInstructions;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class RNSPonderScenes {
    protected static int calcAngle(float rpm, float dur) {
        return Math.round(rpm * dur / 60 / 20) * 360;
    }

    protected static float calcRpm(int angle, float dur) {
        return angle / dur * 60 * 20 / 360;
    }

    public static void mining(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mining", "Mining Deposits");
        scene.scaleSceneView(0.9f);

        BlockPos bearing = util.grid().at(2, 2, 2);
        BlockPos bearingNewPos = util.grid().at(2, 4, 2);
        BlockPos drill = util.grid().at(2, 1, 2);
        Vec3 underDrill = drill.getBottomCenter();
        Selection kinetics = util.select().fromTo(2, 3, 2, 2, 4, 2)
                .add(util.select().fromTo(2, 5, 2, 4, 5, 2))
                .add(util.select().fromTo(4, 1, 2, 4, 4, 2));
        Selection sourceCog = util.select().position(5, 0, 2);
        Selection resonators = util.select().fromTo(2, 2, 2, 2, 3, 2);
        BlockPos depositUnderBearing = util.grid().at(2, 0, 2);

        BlockPos bearing2 = util.grid().at(1, 2, 2);
        BlockPos drill2 = util.grid().at(1, 1, 2);
        Selection minerTop2 = util.select().fromTo(1, 2, 2, 1, 3, 2);
        BlockPos depositTopRight = util.grid().at(0, 0, 4);
        BlockPos depositBottomRight = util.grid().at(0, 0, 0);

        Selection deposits = util.select().fromTo(0, 0, 0, 4, 0, 4);
        Selection depositsClaimableByMiner2 = util.select().fromTo(0, 0, 0, 3, 0, 4);

        var depStack = new ItemStack(RNSBlocks.IRON_DEPOSIT_BLOCK.get().asItem());
        ParticleEmitter depositParticle = scene.effects().particleEmitterWithinBlockSpace(
                new ItemParticleOption(ParticleTypes.ITEM, depStack), Vec3.ZERO);

        int rpm, dur;

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
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
        scene.idle(80);
        scene.world().showSection(kinetics, Direction.WEST);
        scene.world().showSection(sourceCog, Direction.WEST);
        scene.idle(40);

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
        scene.world().setKineticSpeed(kinetics, rpm);
        scene.world().setKineticSpeed(sourceCog, -rpm);
        scene.world().setKineticSpeed(minerTop2, -rpm);
        scene.world().rotateBearing(bearing, calcAngle(rpm, dur), dur);
        scene.world().rotateSection(drillLink, 0, calcAngle(rpm, dur), 0, dur);
        scene.world().setKineticSpeed(util.select().position(bearing), rpm);
        scene.effects().emitParticles(underDrill, depositParticle, 2, dur);
        scene.idle(60);

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

        scene.overlay().showControls(util.vector().centerOf(depositBottomRight).subtract(1, 0, 0), Pointing.LEFT, 40)
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

        scene.overlay().showText(365)
                .pointAt(nuggetPos)
                .attachKeyFrame()
                .placeNearTarget()
                .text("While some items can be mined without additional attachments...")
                .colored(PonderPalette.BLUE);
        scene.idle(55);

        scene.overlay().showControls(util.vector().topOf(bearing).subtract(.5, 0, 0), Pointing.DOWN, 10)
                .rightClick();
        scene.idle(15);

        scene.world().hideSection(resonators.add(util.select().position(bearingNewPos)), Direction.SOUTH);
        scene.idle(15);

        scene.world().setBlocks(resonators, RNSBlocks.RESONATOR_BLOCK.getDefaultState(), false);
        scene.world().setBlock(bearingNewPos, RNSBlocks.MINER_BEARING_BLOCK.getDefaultState().setValue(MinerBearingBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(util.select().position(bearingNewPos), Direction.NORTH);
        var resonatorsLink = scene.world().showIndependentSection(resonators, Direction.NORTH);
        scene.idle(15);

        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), resonators, 270);
        scene.idle(15);

        scene.overlay().showControls(util.vector().blockSurface(bearingNewPos, Direction.WEST), Pointing.LEFT, 10)
                .rightClick();
        scene.idle(5);

        rpm = 100;
        dur = Short.MAX_VALUE - 1;
        scene.world().setKineticSpeed(util.select().position(bearingNewPos), rpm);
        scene.world().rotateBearing(bearingNewPos, calcAngle(rpm, dur), dur);
        scene.world().rotateSection(resonatorsLink, 0, calcAngle(rpm, dur), 0, dur);
        scene.world().rotateSection(drillLink, 0, calcAngle(rpm, dur), 0, dur);
        scene.effects().emitParticles(underDrill, depositParticle, 2, dur);
        scene.idle(35);

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

    public static void extracting(SceneBuilder builder, SceneBuildingUtil util) {
        SelectionUtil sel = util.select();
        PositionUtil pos = util.grid();
        VectorUtil vec = util.vector();

        Selection contraption = sel.fromTo(3, 1, 2, 5, 5, 4);
        BlockPos bearing = pos.at(4, 6, 3);
        BlockPos drill = pos.at(4, 1, 3);
        BlockPos outerInterface = pos.at(7, 5, 3);
        BlockPos contInterface = pos.at(5, 5, 3);
        Selection outerInterfaceSystem = sel.fromTo(7, 1, 3, 7, 5, 3);
        Selection interfacePair = sel.position(outerInterface).add(sel.position(contInterface));
        var psiClass = PortableItemInterfaceBlockEntity.class;
        Selection kineticsTop = sel.fromTo(1, 7, 3, 4, 7, 3);
        Selection kineticsSide = sel.fromTo(1, 1, 3, 1, 6, 3);
        Selection sourceCog = sel.position(0, 0, 3);
        BlockPos timer = pos.at(7, 5, 1);
        Selection timerSel = sel.position(timer);
        BlockPos extender = pos.at(7, 5, 2);
        Selection extenderSel = sel.position(extender);
        Selection redstoneStructure = sel.fromTo(7, 1, 1, 7, 5, 2);

        Consumer<CompoundTag> psiOn = nbt -> {
            nbt.putFloat("Distance", 1);
            nbt.putFloat("Timer", 4);
        };
        Consumer<CompoundTag> psiOff = nbt -> nbt.putFloat("Timer", 1);

        float rpm = 100;

        CreateSceneBuilder s = new CreateSceneBuilder(builder);
        WorldInstructions w = s.world();
        OverlayInstructions o = s.overlay();
        s.title("exchanging", "Extracting mined resources");
        s.scaleSceneView(0.65f);
        s.setSceneOffsetY(-0.8f);
        MinerContraption miner = new MinerContraption(s, util, bearing, drill, contraption, RNSBlocks.IRON_DEPOSIT_BLOCK.get())
                .syncKinetics(kineticsTop, false)
                .syncKinetics(kineticsSide, false)
                .syncKinetics(sourceCog, false);

        w.setKineticSpeed(sourceCog, rpm);
        w.setKineticSpeed(kineticsTop.add(kineticsSide), rpm);
        miner.setBearingKinetics(rpm);

        s.configureBasePlate(1, 0, 7);
        s.showBasePlate();
        w.showSection(sourceCog, Direction.UP);
        s.idle(10);

        miner.showBearing(Direction.DOWN);
        s.idle(2);

        w.showSection(kineticsTop, Direction.DOWN);
        s.idle(2);

        w.showSection(kineticsSide, Direction.EAST);
        s.idle(2);

        miner.showContraption(Direction.NORTH);
        s.idle(8);

        o.showControls(vec.blockSurface(bearing, Direction.NORTH).add(0, 0.2, 0), Pointing.RIGHT, 10)
                .rightClick();
        s.idle(6);

        miner.mine(rpm, 175);
        s.idle(35);

        o.showText(60)
                .pointAt(vec.blockSurface(bearing, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Miners work best when running continuously without interruptions");
        s.idle(70);

        w.showSection(outerInterfaceSystem, Direction.WEST);
        o.showText(60)
                .pointAt(vec.blockSurface(outerInterface, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Adding a portable interface can significantly slow them down");
        s.idle(70);

        for (int i = 0; i < 3; ++i) {
            miner.mine(360, 12);
            s.idle(12);

            w.modifyBlockEntityNBT(interfacePair, psiClass, psiOn);
            s.idle(10);

            w.modifyBlockEntityNBT(interfacePair, psiClass, psiOff);
            s.idle(2);
        }
        miner.mine(360, 12);
        s.idle(8);
        // 80

        o.showControls(vec.blockSurface(bearing, Direction.NORTH).add(0, 0.2, 0), Pointing.RIGHT, 10)
                .rightClick();
        s.idle(40);

        w.modifyBlockEntityNBT(timerSel, BrassDiodeBlockEntity.class, nbt -> nbt.putInt("ScrollValue", 160));
        w.modifyBlockEntityNBT(extenderSel, BrassDiodeBlockEntity.class, nbt -> nbt.putInt("ScrollValue", 40));
        w.modifyBlock(extender, bs -> bs.setValue(BrassDiodeBlock.INVERTED, true), false);

        UnaryOperator<BlockState> setPoweringOff = bs -> bs.setValue(BrassDiodeBlock.POWERING, false);
        UnaryOperator<BlockState> setPoweringOn = bs -> bs.setValue(BrassDiodeBlock.POWERING, true);
        UnaryOperator<BlockState> setPoweredOff = bs -> bs.setValue(BrassDiodeBlock.POWERED, false);
        UnaryOperator<BlockState> setPoweredOn = bs -> bs.setValue(BrassDiodeBlock.POWERED, true);

        for (int i = 0; i < 50; ++i) {
            if (i == 0) {
                w.showSection(redstoneStructure, Direction.SOUTH);
                s.idle(10);

                o.showText(56)
                        .pointAt(timer.getCenter().subtract(0, 0.3, 0))
                        .attachKeyFrame()
                        .placeNearTarget()
                        .text("Controlling interfaces with redstone is key to maximize efficiency");
                s.idle(66);

                s.addKeyframe();
                o.showControls(vec.blockSurface(bearing, Direction.NORTH).add(0, 0.2, 0), Pointing.RIGHT, 10)
                        .rightClick();
                s.idle(4);
            } else {
                w.cycleBlockProperty(timer, BrassDiodeBlock.POWERING);          // Timer pulse on
                s.idle(1);

                w.cycleBlockProperty(timer, BrassDiodeBlock.POWERING);          // Timer pulse off
                w.modifyBlock(extender, setPoweredOn, false);      // Extender input on
                s.idle(1);

                w.modifyBlock(extender, setPoweredOff, false);     // Extender input off
                w.modifyBlock(extender, setPoweringOn, false);     // Extender output on
                s.idle(3);

                w.modifyBlockEntityNBT(interfacePair, psiClass, psiOn);         // Extend interfaces
                s.idle(37);

                w.modifyBlock(extender, setPoweringOff, false);    // Extender output off
                s.idle(2);

                w.modifyBlockEntityNBT(interfacePair, psiClass, psiOff);        // Retract interfaces
                miner.mine(rpm, 36);
                s.idle(13);

                if (i == 1) s.markAsFinished();
                s.idle(23);
            }

            miner.mine(rpm, 85);
            s.idle(80);
        }
    }

    protected static class MinerContraption {
        protected final CreateSceneBuilder scene;
        protected final SceneBuildingUtil util;
        protected final BlockPos bearing;
        protected final BlockPos drill;
        protected final Selection contraption;
        protected final ParticleEmitter particle;
        protected ElementLink<WorldSectionElement> contraptionLink = null;
        protected Map<Selection, Boolean> syncedKinetics = new Object2ObjectOpenHashMap<>();

        protected MinerContraption(CreateSceneBuilder scene, SceneBuildingUtil util,
                                   BlockPos bearing, BlockPos drill, Selection contraption, DepositBlock depBlock) {
            this.scene = scene;
            this.util = util;
            this.bearing = bearing;
            this.drill = drill;
            this.contraption = contraption;
            this.particle = scene.effects().particleEmitterWithinBlockSpace(
                    new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(depBlock)), Vec3.ZERO);
        }

        public MinerContraption syncKinetics(Selection kinetics, boolean reverse) {
            this.syncedKinetics.put(kinetics, reverse);
            return this;
        }

        public void hideContraption(Direction direction) {
            if (contraptionLink == null) return;
            scene.world().hideIndependentSection(contraptionLink, direction);
        }

        public void showContraption(Direction direction) {
            if (contraptionLink != null) hideContraption(direction);
            contraptionLink = scene.world().showIndependentSection(contraption, direction);
        }

        public void hideBearing(Direction direction) {
            scene.world().hideSection(util.select().position(bearing), direction);
        }

        public void showBearing(Direction direction) {
            scene.world().showSection(util.select().position(bearing), direction);
        }

        public void setBearingKinetics(float rpm) {
            scene.world().setKineticSpeed(util.select().position(bearing), rpm);
        }

        public float rotate(int angle, int dur) {
            if (contraptionLink == null) return 0f;
            float rpm = calcRpm(angle, dur);
            setBearingKinetics(rpm);
            scene.world().rotateBearing(bearing, angle, dur);
            scene.world().rotateSection(contraptionLink, 0, angle, 0, dur);
            for (var k : syncedKinetics.entrySet()) {
                scene.world().setKineticSpeed(k.getKey(), k.getValue() ? -rpm : rpm);
            }
            return rpm;
        }

        public float rotate(float rpm, int dur) {
            if (contraptionLink == null) return 0f;
            int angle = calcAngle(rpm, dur);
            float newRpm = calcRpm(angle, dur);
            scene.world().rotateBearing(bearing, angle, dur);
            scene.world().rotateSection(contraptionLink, 0, angle, 0, dur);
            setBearingKinetics(newRpm);
            for (var k : syncedKinetics.entrySet()) {
                scene.world().setKineticSpeed(k.getKey(), k.getValue() ? -newRpm : newRpm);
            }
            return newRpm;
        }

        public float mine(int angle, int dur) {
            if (contraptionLink == null) return 0f;
            float newRpm = rotate(angle, dur);
            scene.effects().emitParticles(drill.getBottomCenter(), particle, 2, dur);
            return newRpm;
        }

        public float mine(float rpm, int dur) {
            if (contraptionLink == null) return 0f;
            float newRpm = rotate(rpm, dur);
            scene.effects().emitParticles(drill.getBottomCenter(), particle, 2, dur);
            return newRpm;
        }
    }
}

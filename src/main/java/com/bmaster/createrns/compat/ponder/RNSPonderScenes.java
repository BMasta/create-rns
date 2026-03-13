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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSPonderScenes {
    protected static int calcAngle(float rpm, float dur) {
        return Math.round(rpm * dur / 60 / 20) * 360;
    }

    protected static float calcRpm(int angle, float dur) {
        return angle / dur * 60 * 20 / 360;
    }

    public static void mining(SceneBuilder builder, SceneBuildingUtil util) {
        var sel = util.select();
        var pos = util.grid();
        var vec = util.vector();

        var s = new CreateSceneBuilder(builder);
        var w = s.world();
        var o = s.overlay();

        s.title("mining", "Mining Deposits");
        s.scaleSceneView(0.9f);

        // Positions
        var bearing = pos.at(2, 2, 2);
        var bearingNewPos = pos.at(2, 4, 2);
        var drill = pos.at(2, 1, 2);
        var bearing2 = pos.at(1, 2, 2);
        var drill2 = pos.at(1, 1, 2);
        var depositUnderBearing = pos.at(2, 0, 2);
        var depositTopRight = pos.at(0, 0, 4);
        var depositBottomRight = pos.at(0, 0, 0);

        // Selections
        var sourceCog = sel.position(5, 0, 2);
        var nearBearingKinetics = sel.fromTo(2, 3, 2, 2, 4, 2);
        var chainDriveKinetics = sel.fromTo(2, 5, 2, 4, 5, 2);
        var verticalShaftKinetics = sel.fromTo(4, 1, 2, 4, 4, 2);
        var resonators = sel.fromTo(2, 2, 2, 2, 3, 2);
        var miner2Cog = sel.fromTo(1, 3, 2, 1, 3, 2);
        var deposits = sel.fromTo(0, 0, 0, 4, 0, 4);
        var depositsClaimableByMiner2 = sel.fromTo(0, 0, 0, 3, 0, 4);

        // Other
        float rpm = 100;
        var cobblestone = new ItemStack(Items.COBBLESTONE);
        var nugget = new ItemStack(Items.IRON_NUGGET);
        var rawIron = new ItemStack(Items.RAW_IRON);
        var cobblePos = drill.getCenter().add(new Vec3(-1.2, -0.2, 1.5));
        var nuggetPos = drill.getCenter().add(new Vec3(-1.5, -0.2, 1.5));
        var rawIronPos = drill.getCenter().add(new Vec3(-1.5, -0.2, -1.5));
        var bearingFacingDown = RNSBlocks.MINER_BEARING.getDefaultState().setValue(MinerBearingBlock.FACING, Direction.DOWN);
        var miner = new MinerContraption(s, util, bearing, drill, sel.position(drill), RNSBlocks.IRON_DEPOSIT.get())
                .syncKinetics(nearBearingKinetics, false)
                .syncKinetics(chainDriveKinetics, false)
                .syncKinetics(verticalShaftKinetics, false)
                .syncKinetics(sourceCog, true);
        var miner2 = new MinerContraption(s, util, bearing2, drill2, sel.position(drill2), RNSBlocks.IRON_DEPOSIT.get())
                .syncKinetics(miner2Cog, false);
        var minerAfter = new MinerContraption(s, util, bearingNewPos, drill, resonators.add(sel.position(drill)), RNSBlocks.IRON_DEPOSIT.get())
                .syncKinetics(chainDriveKinetics, false)
                .syncKinetics(verticalShaftKinetics, false)
                .syncKinetics(sourceCog, true);

        // Initial kinetics setup
        w.setKineticSpeed(sourceCog, -rpm);
        w.setKineticSpeed(nearBearingKinetics, rpm);
        w.setKineticSpeed(chainDriveKinetics, rpm);
        w.setKineticSpeed(verticalShaftKinetics, rpm);
        w.setKineticSpeed(miner2Cog, -rpm);

        s.configureBasePlate(0, 0, 5);
        s.showBasePlate();
        w.showSection(sourceCog, Direction.UP);
        s.idle(15);

        o.showText(100)
                .pointAt(vec.topOf(depositUnderBearing))
                .attachKeyFrame()
                .placeNearTarget()
                .sharedText(ServerConfig.INFINITE_DEPOSITS.get() ? "mining_infinite" : "mining_finite");
        s.idle(100);

        miner.showBearing(Direction.NORTH);
        s.idle(10);

        o.showText(50)
                .pointAt(vec.blockSurface(bearing, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Miner bearings create contraptions used for mining");
        s.idle(60);

        miner.showContraption(Direction.NORTH);
        s.idle(20);

        o.showText(100)
                .pointAt(vec.blockSurface(drill, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Each miner contraption must have exactly one drill head directly underneath in order to function");
        s.idle(80);

        w.showSection(verticalShaftKinetics, Direction.WEST);
        s.idle(2);

        w.showSection(chainDriveKinetics, Direction.UP);
        s.idle(2);

        w.showSection(nearBearingKinetics, Direction.NORTH);
        s.idle(10);

        miner.setBearingKinetics(rpm);
        s.idle(20);

        o.showText(60)
                .pointAt(vec.blockSurface(drill, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("When assembled, the miner claims deposit blocks below in a certain area");
        s.idle(70);

        o.showControls(vec.topOf(bearing).subtract(.5, 0, 0), Pointing.DOWN, 10)
                .rightClick();
        s.idle(10);

        o.showOutline(PonderPalette.GREEN, new Object(), deposits, 60);
        miner.mine(rpm, 300);
        s.idle(60);

        w.showSection(miner2Cog, Direction.EAST);
        miner2.setBearingKinetics(-rpm);
        miner2.showBearing(Direction.EAST);
        miner2.showContraption(Direction.EAST);
        s.idle(10);

        o.showText(110)
                .pointAt(vec.blockSurface(depositTopRight, Direction.WEST))
                .attachKeyFrame()
                .placeNearTarget()
                .text("Each deposit block can only be claimed by a single miner");
        s.idle(40);

        o.showControls(vec.topOf(bearing2).subtract(.5, 0, 0), Pointing.DOWN, 10)
                .rightClick();
        s.idle(5);

        o.showOutline(PonderPalette.RED, new Object(), depositsClaimableByMiner2, 75);
        miner2.rotate(-rpm, 190);
        s.idle(20);

        o.showControls(vec.centerOf(depositBottomRight).subtract(1, 0, 0), Pointing.LEFT, 40)
                .showing(AllIcons.I_MTD_CLOSE);
        s.idle(60);

        miner2.hideBearing(Direction.WEST);
        miner2.hideContraption(Direction.WEST);
        w.hideSection(miner2Cog, Direction.WEST);
        s.idle(30);

        w.createItemEntity(cobblePos, Vec3.ZERO, cobblestone);
        s.idle(3);
        w.createItemEntity(nuggetPos, Vec3.ZERO, nugget);
        s.idle(10);

        o.showText(365)
                .pointAt(nuggetPos)
                .attachKeyFrame()
                .placeNearTarget()
                .text("While some items can be mined without additional attachments...")
                .colored(PonderPalette.BLUE);
        s.idle(55);

        o.showControls(vec.topOf(bearing).subtract(.5, 0, 0), Pointing.DOWN, 10)
                .rightClick();
        s.idle(15);

        miner.hideBearing(Direction.SOUTH);
        miner.hideContraption(Direction.SOUTH);
        w.hideSection(nearBearingKinetics, Direction.SOUTH);
        s.idle(15);

        w.setBlocks(resonators, RNSBlocks.RESONATOR.getDefaultState(), false);
        w.setBlock(bearingNewPos, bearingFacingDown, false);
        minerAfter.setBearingKinetics(rpm);
        minerAfter.showBearing(Direction.NORTH);
        minerAfter.showContraption(Direction.NORTH);
        s.idle(15);

        o.showOutline(PonderPalette.GREEN, new Object(), resonators, 270);
        s.idle(15);

        o.showControls(vec.blockSurface(bearingNewPos, Direction.WEST), Pointing.LEFT, 10).rightClick();
        s.idle(5);

        minerAfter.mine(rpm, Short.MAX_VALUE - 1);
        s.idle(35);

        w.createItemEntity(rawIronPos, Vec3.ZERO, rawIron);
        s.idle(10);

        o.showText(200)
                .pointAt(rawIronPos)
                .attachKeyFrame()
                .placeNearTarget()
                .text("Others require an active catalyst")
                .colored(PonderPalette.GREEN);
        s.idle(200);
    }

    public static void extracting(SceneBuilder builder, SceneBuildingUtil util) {
        var sel = util.select();
        var pos = util.grid();
        var vec = util.vector();

        var s = new CreateSceneBuilder(builder);
        var w = s.world();
        var o = s.overlay();
        s.title("extracting", "Extracting mined resources");
        s.scaleSceneView(0.65f);
        s.setSceneOffsetY(-0.8f);

        // Positions
        var bearing = pos.at(4, 6, 3);
        var drill = pos.at(4, 1, 3);
        var outerInterface = pos.at(7, 5, 3);
        var contInterface = pos.at(5, 5, 3);
        var timer = pos.at(7, 5, 1);
        var extender = pos.at(7, 5, 2);

        // Selections
        var contraption = sel.fromTo(3, 1, 2, 5, 5, 4);
        var outerInterfaceSystem = sel.fromTo(7, 1, 3, 7, 5, 3);
        var interfacePair = sel.position(outerInterface).add(sel.position(contInterface));
        var kineticsTop = sel.fromTo(1, 7, 3, 4, 7, 3);
        var kineticsSide = sel.fromTo(1, 1, 3, 1, 6, 3);
        var sourceCog = sel.position(0, 0, 3);
        var timerSel = sel.position(timer);
        var extenderSel = sel.position(extender);
        var redstoneStructure = sel.fromTo(7, 1, 1, 7, 5, 2);

        // Other
        float rpm = 100;
        UnaryOperator<BlockState> setPoweringOff = bs -> bs.setValue(BrassDiodeBlock.POWERING, false);
        UnaryOperator<BlockState> setPoweringOn = bs -> bs.setValue(BrassDiodeBlock.POWERING, true);
        UnaryOperator<BlockState> setPoweredOff = bs -> bs.setValue(BrassDiodeBlock.POWERED, false);
        UnaryOperator<BlockState> setPoweredOn = bs -> bs.setValue(BrassDiodeBlock.POWERED, true);
        var psiClass = PortableItemInterfaceBlockEntity.class;
        Consumer<CompoundTag> psiOn = nbt -> {
            nbt.putFloat("Distance", 1);
            nbt.putFloat("Timer", 4);
        };
        Consumer<CompoundTag> psiOff = nbt -> nbt.putFloat("Timer", 1);
        MinerContraption miner = new MinerContraption(s, util, bearing, drill, contraption, RNSBlocks.IRON_DEPOSIT.get())
                .syncKinetics(kineticsTop, false)
                .syncKinetics(kineticsSide, false)
                .syncKinetics(sourceCog, false);

        // Initial kinetics setup
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

        // First pass of redstone-assisted miner loop
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

        miner.mine(rpm, 85);
        s.idle(80);

        // Loop the same thing, but without decorations
        for (int i = 0; i < 50; ++i) {
            w.cycleBlockProperty(timer, BrassDiodeBlock.POWERING);          // Timer pulse on
            s.idle(1);

            w.cycleBlockProperty(timer, BrassDiodeBlock.POWERING);          // Timer pulse off
            w.modifyBlock(extender, setPoweredOn, false);      // Extender input on
            s.idle(1);

            w.modifyBlock(extender, setPoweredOff, false);     // Extender input off
            w.modifyBlock(extender, setPoweringOn, false);     // Extender output on
            s.idle(3);

            w.modifyBlockEntityNBT(interfacePair, psiClass, psiOn);         // Extend interfaces | Mining off
            s.idle(37);

            w.modifyBlock(extender, setPoweringOff, false);    // Extender output off
            s.idle(2);

            w.modifyBlockEntityNBT(interfacePair, psiClass, psiOff);        // Retract interfaces | Mining on
            miner.mine(rpm, 121);
            s.idle(116);

            if (i == 0) s.markAsFinished();
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

        public void rotate(int angle, int dur) {
            if (contraptionLink == null) return;
            float rpm = calcRpm(angle, dur);
            setBearingKinetics(rpm);
            scene.world().rotateBearing(bearing, angle, dur);
            scene.world().rotateSection(contraptionLink, 0, angle, 0, dur);
            for (var k : syncedKinetics.entrySet()) {
                scene.world().setKineticSpeed(k.getKey(), k.getValue() ? -rpm : rpm);
            }
        }

        public void rotate(float rpm, int dur) {
            if (contraptionLink == null) return;
            rotate(calcAngle(rpm, dur), dur);
        }

        public void mine(int angle, int dur) {
            if (contraptionLink == null) return;
            rotate(angle, dur);
            scene.effects().emitParticles(drill.getCenter().subtract(0, 0.5, 0), particle, 2, dur);
        }

        public void mine(float rpm, int dur) {
            if (contraptionLink == null) return;
            rotate(rpm, dur);
            scene.effects().emitParticles(drill.getCenter().subtract(0, 0.5, 0), particle, 2, dur);
        }
    }
}

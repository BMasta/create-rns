//package com.bmaster.createrns.compat.ponder;
//
//import com.bmaster.createrns.RNSContent;
//import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
//import net.createmod.ponder.api.ParticleEmitter;
//import net.createmod.ponder.api.PonderPalette;
//import net.createmod.ponder.api.element.ElementLink;
//import net.createmod.ponder.api.element.WorldSectionElement;
//import net.createmod.ponder.api.scene.SceneBuilder;
//import net.createmod.ponder.api.scene.SceneBuildingUtil;
//import net.createmod.ponder.api.scene.Selection;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.particles.ItemParticleOption;
//import net.minecraft.core.particles.ParticleTypes;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.phys.Vec3;
//
//public class RNSPonderScenes {
//    public static void mining(SceneBuilder builder, SceneBuildingUtil util) {
//        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
//        scene.title("mining", "Mining Deposits");
//        scene.setSceneOffsetY(-4f);
//        scene.scaleSceneView(0.9f);
//
//        Selection basePlate = util.select().fromTo(0, 4, 0, 6, 4, 6);
//        Selection deposit1 = util.select().fromTo(3, 5, 2, 5, 5, 4);
//        Selection deposit1Edge = util.select().position(3, 5, 2);
//        Selection deposit2 = util.select().fromTo(1, 5, 2, 2, 5, 4);
//        Selection deposit3 = util.select().fromTo(3, 0, 2, 5, 3, 4);
//        Selection deposit13Outline = util.select().fromTo(3, 1, 2, 5, 5, 4);
//        Selection miner1 = util.select().position(4, 6, 3);
//        BlockPos miner1Above = new BlockPos(4, 7, 3);
//        Selection miner2 = util.select().position(2, 6, 3);
//        Selection belt = util.select().fromTo(0, 7, 3, 6, 7, 3);
//        Selection kineticsTop = util.select().fromTo(3, 7, 4, 3, 7, 5);
//        Selection kineticsBottom = util.select().fromTo(3, 5, 5, 3, 5, 7);
//        Selection nearMinersCog = util.select().position(3, 6, 4);
//        Selection originCog = util.select().position(2, 4, 7);
//        ElementLink<WorldSectionElement> bpLink = scene.world().showIndependentSection(basePlate, Direction.DOWN);
//
//        scene.world().setKineticSpeed(miner1, -100);
//        scene.world().setKineticSpeed(miner2, -100);
//        scene.world().setKineticSpeed(nearMinersCog, 50);
//        scene.world().setKineticSpeed(belt, 50);
//        scene.world().setKineticSpeed(kineticsTop, 50);
//        scene.world().setKineticSpeed(kineticsBottom, -50);
//        scene.world().setKineticSpeed(originCog, 25);
//
//        scene.idle(10);
//        scene.world().showSection(deposit1, Direction.DOWN);
//        scene.idle(10);
//        scene.world().showSection(miner1, Direction.DOWN);
//        scene.idle(10);
//        var depItem = new ItemStack(RNSContent.IRON_DEPOSIT_BLOCK.get().asItem());
//        var impureIron = new ItemStack(RNSContent.IMPURE_IRON_ORE.get());
//        var impureCopper = new ItemStack(RNSContent.IMPURE_COPPER_ORE.get());
//        ParticleEmitter depositParticle = scene.effects().particleEmitterWithinBlockSpace(
//                new ItemParticleOption(ParticleTypes.ITEM, depItem), util.vector().of(0, 0, 0));
//        scene.effects().emitParticles(miner1.getCenter().subtract(0, 0.5, 0),
//                depositParticle, 2, Integer.MAX_VALUE);
//
//        scene.world().showSection(nearMinersCog, Direction.NORTH);
//        scene.idle(5);
//        scene.world().showSection(belt, Direction.DOWN);
//        scene.idle(5);
//        scene.world().showSection(kineticsTop, Direction.NORTH);
//        scene.world().showSection(kineticsBottom, Direction.NORTH);
//        scene.idle(5);
//        scene.world().showSection(originCog, Direction.NORTH);
//
//        scene.idle(20);
//        scene.overlay().showText(50)
//                .attachKeyFrame()
//                .text("Miners can extract materials from deposits scattered around the world")
//                .pointAt(miner1.getCenter())
//                .placeNearTarget();
//
//        scene.idle(70);
//        scene.overlay().showOutline(PonderPalette.WHITE, new Object(), deposit1, 390);
//
//        scene.idle(20);
//        scene.overlay().showText(50)
//                .attachKeyFrame()
//                .text("Miner Mk.1 can claim deposit blocks in a 3x3x1 area below it")
//                .pointAt(deposit1Edge.getCenter())
//                .placeNearTarget();
//
//        scene.idle(70);
//        scene.overlay().showText(90)
//                .attachKeyFrame()
//                .text("Its throughput and yield types depend on which deposit blocks it claimed")
//                .pointAt(util.select().position(miner1Above).getCenter())
//                .placeNearTarget();
//
//        scene.idle(90);
//        scene.world().createItemOnBelt(miner1Above, Direction.UP, impureIron);
//
//        scene.idle(20);
//        scene.world().createItemOnBelt(miner1Above, Direction.UP, impureIron);
//
//        scene.idle(20);
//        scene.world().createItemOnBelt(miner1Above, Direction.UP, impureCopper);
//
//        scene.idle(10);
//        scene.world().showSection(deposit2, Direction.EAST);
//        scene.world().showSection(miner2, Direction.EAST);
//        scene.idle(5);
//        scene.effects().emitParticles(miner2.getCenter().subtract(0, 0.5, 0),
//                depositParticle, 2, 115);
//
//        scene.idle(15);
//        scene.overlay().showOutline(PonderPalette.WHITE, new Object(), deposit2, 100);
//        scene.idle(10);
//        scene.overlay().showText(70)
//                .attachKeyFrame()
//                .text("A deposit block cannot be claimed by more than one miner at a time")
//                .pointAt(deposit1Edge.getCenter())
//                .placeNearTarget();
//
//        scene.idle(90);
//        scene.world().hideSection(deposit2, Direction.WEST);
//        scene.world().hideSection(miner2, Direction.WEST);
//
//        scene.idle(40);
//        scene.world().replaceBlocks(deposit1Edge, RNSContent.REDSTONE_DEPOSIT_BLOCK.getDefaultState(), true);
//        scene.overlay().showOutline(PonderPalette.WHITE, new Object(), deposit1.substract(deposit1Edge), 90);
//
//        scene.idle(20);
//        scene.overlay().showText(50)
//                .attachKeyFrame()
//                .text("Miner Mk.1 can only mine basic deposits")
//                .pointAt(deposit1Edge.getCenter())
//                .placeNearTarget();
//
//        scene.idle(70);
//        scene.world().moveSection(bpLink, new Vec3(0, -4, 0), 20);
//
//        scene.idle(20);
//        scene.world().setBlocks(deposit3, RNSContent.IRON_DEPOSIT_BLOCK.getDefaultState(), false);
//        ElementLink<WorldSectionElement> deposit3Link = scene.world().showIndependentSection(deposit3, Direction.NORTH);
//        scene.world().moveSection(deposit3Link, new Vec3(0, 1, 0), 0);
//
//        scene.idle(10);
//        scene.world().replaceBlocks(miner1, RNSContent.MINER_MK2_BLOCK.get().defaultBlockState(), true);
//        scene.world().setKineticSpeed(miner1, -100);
//        scene.overlay().showOutline(PonderPalette.WHITE, new Object(), deposit13Outline, Integer.MAX_VALUE);
//        scene.idle(20);
//        scene.overlay().showText(70)
//                .attachKeyFrame()
//                .text("Miner Mk.2 can mine all types of deposits in a 3x3x5 area")
//                .pointAt(miner1.getCenter())
//                .placeNearTarget();
//    }
//}

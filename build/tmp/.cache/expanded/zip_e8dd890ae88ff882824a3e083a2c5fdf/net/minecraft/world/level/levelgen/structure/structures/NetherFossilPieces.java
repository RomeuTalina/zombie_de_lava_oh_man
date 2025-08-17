package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class NetherFossilPieces {
    private static final ResourceLocation[] FOSSILS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_1"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_2"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_3"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_4"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_5"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_6"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_7"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_8"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_9"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_10"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_11"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_12"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_13"),
        ResourceLocation.withDefaultNamespace("nether_fossils/fossil_14")
    };

    public static void addPieces(StructureTemplateManager pStructureManager, StructurePieceAccessor pPieces, RandomSource pRandom, BlockPos pPos) {
        Rotation rotation = Rotation.getRandom(pRandom);
        pPieces.addPiece(new NetherFossilPieces.NetherFossilPiece(pStructureManager, Util.getRandom(FOSSILS, pRandom), pPos, rotation));
    }

    public static class NetherFossilPiece extends TemplateStructurePiece {
        public NetherFossilPiece(StructureTemplateManager pStructureManager, ResourceLocation pLocation, BlockPos pPos, Rotation pRotation) {
            super(StructurePieceType.NETHER_FOSSIL, 0, pStructureManager, pLocation, pLocation.toString(), makeSettings(pRotation), pPos);
        }

        public NetherFossilPiece(StructureTemplateManager pStructureManager, CompoundTag pTag) {
            super(StructurePieceType.NETHER_FOSSIL, pTag, pStructureManager, p_391079_ -> makeSettings(pTag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow()));
        }

        private static StructurePlaceSettings makeSettings(Rotation pRotation) {
            return new StructurePlaceSettings().setRotation(pRotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228558_, CompoundTag p_228559_) {
            super.addAdditionalSaveData(p_228558_, p_228559_);
            p_228559_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        }

        @Override
        protected void handleDataMarker(String p_228561_, BlockPos p_228562_, ServerLevelAccessor p_228563_, RandomSource p_228564_, BoundingBox p_228565_) {
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228548_,
            StructureManager p_228549_,
            ChunkGenerator p_228550_,
            RandomSource p_228551_,
            BoundingBox p_228552_,
            ChunkPos p_228553_,
            BlockPos p_228554_
        ) {
            BoundingBox boundingbox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
            p_228552_.encapsulate(boundingbox);
            super.postProcess(p_228548_, p_228549_, p_228550_, p_228551_, p_228552_, p_228553_, p_228554_);
            this.placeDriedGhast(p_228548_, p_228551_, boundingbox, p_228552_);
        }

        private void placeDriedGhast(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pTemplateBox, BoundingBox pBox) {
            RandomSource randomsource = RandomSource.create(pLevel.getSeed()).forkPositional().at(pTemplateBox.getCenter());
            if (randomsource.nextFloat() < 0.5F) {
                int i = pTemplateBox.minX() + randomsource.nextInt(pTemplateBox.getXSpan());
                int j = pTemplateBox.minY();
                int k = pTemplateBox.minZ() + randomsource.nextInt(pTemplateBox.getZSpan());
                BlockPos blockpos = new BlockPos(i, j, k);
                if (pLevel.getBlockState(blockpos).isAir() && pBox.isInside(blockpos)) {
                    pLevel.setBlock(blockpos, Blocks.DRIED_GHAST.defaultBlockState().rotate(Rotation.getRandom(randomsource)), 2);
                }
            }
        }
    }
}
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public abstract class BlockEntity extends net.minecraftforge.common.capabilities.CapabilityProvider.BlockEntities implements net.minecraftforge.common.extensions.IForgeBlockEntity {
    private static final Codec<BlockEntityType<?>> TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockEntityType<?> type;
    @Nullable
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;

    public BlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super();
        this.type = pType;
        this.worldPosition = pPos.immutable();
        this.validateBlockState(pBlockState);
        this.blockState = pBlockState;
        this.gatherCapabilities();
    }

    private void validateBlockState(BlockState pState) {
        if (!this.isValidBlockState(pState)) {
            throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + this.worldPosition + ", got " + pState);
        }
    }

    public boolean isValidBlockState(BlockState pState) {
        return this.getType().isValid(pState);
    }

    public static BlockPos getPosFromTag(ChunkPos pChunkPos, CompoundTag pTag) {
        int i = pTag.getIntOr("x", 0);
        int j = pTag.getIntOr("y", 0);
        int k = pTag.getIntOr("z", 0);
        int l = SectionPos.blockToSectionCoord(i);
        int i1 = SectionPos.blockToSectionCoord(k);
        if (l != pChunkPos.x || i1 != pChunkPos.z) {
            LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", pTag, pChunkPos);
            i = pChunkPos.getBlockX(SectionPos.sectionRelative(i));
            k = pChunkPos.getBlockZ(SectionPos.sectionRelative(k));
        }

        return new BlockPos(i, j, k);
    }

    @Nullable
    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level pLevel) {
        this.level = pLevel;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    protected void loadAdditional(ValueInput pInput) {
        if (getCapabilities() != null) pInput.read("ForgeCaps", CompoundTag.CODEC).ifPresent(caps -> deserializeCaps(pInput.lookup(), caps));
    }

    public final void loadWithComponents(ValueInput pInput) {
        this.loadAdditional(pInput);
        this.components = pInput.read("components", DataComponentMap.CODEC).orElse(DataComponentMap.EMPTY);
    }

    public final void loadCustomOnly(ValueInput pInput) {
        this.loadAdditional(pInput);
    }

    protected void saveAdditional(ValueOutput pOutput) {
        if (getCapabilities() != null) pOutput.storeNullable("ForgeCaps", CompoundTag.CODEC, serializeCaps(this.level.registryAccess()));
    }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, pRegistries);
            this.saveWithFullMetadata(tagvalueoutput);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    public void saveWithFullMetadata(ValueOutput pOutput) {
        this.saveWithoutMetadata(pOutput);
        this.saveMetadata(pOutput);
    }

    public void saveWithId(ValueOutput pOutput) {
        this.saveWithoutMetadata(pOutput);
        this.saveId(pOutput);
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, pRegistries);
            this.saveWithoutMetadata(tagvalueoutput);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    public void saveWithoutMetadata(ValueOutput pOutput) {
        this.saveAdditional(pOutput);
        pOutput.store("components", DataComponentMap.CODEC, this.components);
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, pRegistries);
            this.saveCustomOnly(tagvalueoutput);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    public void saveCustomOnly(ValueOutput pOutput) {
        this.saveAdditional(pOutput);
    }

    private void saveId(ValueOutput pOutput) {
        addEntityType(pOutput, this.getType());
    }

    public static void addEntityType(ValueOutput pOutput, BlockEntityType<?> pEntityType) {
        pOutput.store("id", TYPE_CODEC, pEntityType);
    }

    private void saveMetadata(ValueOutput pOutput) {
        this.saveId(pOutput);
        pOutput.putInt("x", this.worldPosition.getX());
        pOutput.putInt("y", this.worldPosition.getY());
        pOutput.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static BlockEntity loadStatic(BlockPos pPos, BlockState pState, CompoundTag pTag, HolderLookup.Provider pRegistries) {
        BlockEntityType<?> blockentitytype = pTag.read("id", TYPE_CODEC).orElse(null);
        if (blockentitytype == null) {
            LOGGER.error("Skipping block entity with invalid type: {}", pTag.get("id"));
            return null;
        } else {
            BlockEntity blockentity;
            try {
                blockentity = blockentitytype.create(pPos, pState);
            } catch (Throwable throwable2) {
                LOGGER.error("Failed to create block entity {} for block {} at position {} ", blockentitytype, pPos, pState, throwable2);
                return null;
            }

            try {
                BlockEntity blockentity1;
                try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(blockentity.problemPath(), LOGGER)) {
                    blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, pRegistries, pTag));
                    blockentity1 = blockentity;
                }

                return blockentity1;
            } catch (Throwable throwable1) {
                LOGGER.error("Failed to load data for block entity {} for block {} at position {}", blockentitytype, pPos, pState, throwable1);
                return null;
            }
        }
    }

    public void setChanged() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }
    }

    protected static void setChanged(Level pLevel, BlockPos pPos, BlockState pState) {
        pLevel.blockEntityChanged(pPos);
        if (!pState.isAir()) {
            pLevel.updateNeighbourForOutputSignal(pPos, pState.getBlock());
        }
    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return new CompoundTag();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
        this.invalidateCaps();
        requestModelDataUpdate();
    }

    @Override
    public void onChunkUnloaded() {
        this.invalidateCaps();
    }

    public void clearRemoved() {
        this.remove = false;
    }

    public void preRemoveSideEffects(BlockPos pPos, BlockState pState) {
        if (this instanceof Container container && this.level != null) {
            Containers.dropContents(this.level, pPos, container);
        }
    }

    public boolean triggerEvent(int pId, int pType) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory pReportCategory) {
        pReportCategory.setDetail("Name", this::getNameForReporting);
        pReportCategory.setDetail("Cached block", this.getBlockState()::toString);
        if (this.level == null) {
            pReportCategory.setDetail("Block location", () -> this.worldPosition + " (world missing)");
        } else {
            pReportCategory.setDetail("Actual block", this.level.getBlockState(this.worldPosition)::toString);
            CrashReportCategory.populateBlockLocationDetails(pReportCategory, this.level, this.worldPosition);
        }
    }

    public String getNameForReporting() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Deprecated
    public void setBlockState(BlockState pBlockState) {
        this.validateBlockState(pBlockState);
        this.blockState = pBlockState;
    }

    protected void applyImplicitComponents(DataComponentGetter pComponentGetter) {
    }

    public final void applyComponentsFromItemStack(ItemStack pStack) {
        this.applyComponents(pStack.getPrototype(), pStack.getComponentsPatch());
    }

    public final void applyComponents(DataComponentMap pComponents, DataComponentPatch pPatch) {
        final Set<DataComponentType<?>> set = new HashSet<>();
        set.add(DataComponents.BLOCK_ENTITY_DATA);
        set.add(DataComponents.BLOCK_STATE);
        final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(pComponents, pPatch);
        this.applyImplicitComponents(new DataComponentGetter() {
            @Nullable
            @Override
            public <T> T get(DataComponentType<? extends T> p_335233_) {
                set.add(p_335233_);
                return datacomponentmap.get(p_335233_);
            }

            @Override
            public <T> T getOrDefault(DataComponentType<? extends T> p_334887_, T p_333244_) {
                set.add(p_334887_);
                return datacomponentmap.getOrDefault(p_334887_, p_333244_);
            }
        });
        DataComponentPatch datacomponentpatch = pPatch.forget(set::contains);
        this.components = datacomponentpatch.split().added();
    }

    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
    }

    @Deprecated
    public void removeComponentsFromTag(ValueOutput pOutput) {
    }

    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
        datacomponentmap$builder.addAll(this.components);
        this.collectImplicitComponents(datacomponentmap$builder);
        return datacomponentmap$builder.build();
    }

    public DataComponentMap components() {
        return this.components;
    }

    public void setComponents(DataComponentMap pComponents) {
        this.components = pComponents;
    }

    @Nullable
    public static Component parseCustomNameSafe(ValueInput pInput, String pCustomName) {
        return pInput.read(pCustomName, ComponentSerialization.CODEC).orElse(null);
    }

    public ProblemReporter.PathElement problemPath() {
        return new BlockEntity.BlockEntityPathElement(this);
    }

    record BlockEntityPathElement(BlockEntity blockEntity) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return this.blockEntity.getNameForReporting() + "@" + this.blockEntity.getBlockPos();
        }
    }
}

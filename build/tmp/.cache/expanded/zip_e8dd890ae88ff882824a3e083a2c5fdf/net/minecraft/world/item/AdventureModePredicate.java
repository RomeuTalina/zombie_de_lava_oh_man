package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public class AdventureModePredicate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<AdventureModePredicate> CODEC = ExtraCodecs.compactListCodec(
            BlockPredicate.CODEC, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())
        )
        .xmap(AdventureModePredicate::new, p_329117_ -> p_329117_.predicates);
    public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(
        BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p_333442_ -> p_333442_.predicates, AdventureModePredicate::new
    );
    public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
    public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
    private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
    private final List<BlockPredicate> predicates;
    @Nullable
    private List<Component> cachedTooltip;
    @Nullable
    private BlockInWorld lastCheckedBlock;
    private boolean lastResult;
    private boolean checksBlockEntity;

    public AdventureModePredicate(List<BlockPredicate> pPredicates) {
        this.predicates = pPredicates;
    }

    private static boolean areSameBlocks(BlockInWorld pFirst, @Nullable BlockInWorld pSecond, boolean pCheckNbt) {
        if (pSecond == null || pFirst.getState() != pSecond.getState()) {
            return false;
        } else if (!pCheckNbt) {
            return true;
        } else if (pFirst.getEntity() == null && pSecond.getEntity() == null) {
            return true;
        } else if (pFirst.getEntity() != null && pSecond.getEntity() != null) {
            boolean flag;
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                RegistryAccess registryaccess = pFirst.getLevel().registryAccess();
                CompoundTag compoundtag = saveBlockEntity(pFirst.getEntity(), registryaccess, problemreporter$scopedcollector);
                CompoundTag compoundtag1 = saveBlockEntity(pSecond.getEntity(), registryaccess, problemreporter$scopedcollector);
                flag = Objects.equals(compoundtag, compoundtag1);
            }

            return flag;
        } else {
            return false;
        }
    }

    private static CompoundTag saveBlockEntity(BlockEntity pBlockEntity, RegistryAccess pRegistryAccess, ProblemReporter pProblemReporter) {
        TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(pProblemReporter.forChild(pBlockEntity.problemPath()), pRegistryAccess);
        pBlockEntity.saveWithId(tagvalueoutput);
        return tagvalueoutput.buildResult();
    }

    public boolean test(BlockInWorld pBlock) {
        if (areSameBlocks(pBlock, this.lastCheckedBlock, this.checksBlockEntity)) {
            return this.lastResult;
        } else {
            this.lastCheckedBlock = pBlock;
            this.checksBlockEntity = false;

            for (BlockPredicate blockpredicate : this.predicates) {
                if (blockpredicate.matches(pBlock)) {
                    this.checksBlockEntity = this.checksBlockEntity | blockpredicate.requiresNbt();
                    this.lastResult = true;
                    return true;
                }
            }

            this.lastResult = false;
            return false;
        }
    }

    private List<Component> tooltip() {
        if (this.cachedTooltip == null) {
            this.cachedTooltip = computeTooltip(this.predicates);
        }

        return this.cachedTooltip;
    }

    public void addToTooltip(Consumer<Component> pTooltipAdder) {
        this.tooltip().forEach(pTooltipAdder);
    }

    private static List<Component> computeTooltip(List<BlockPredicate> pPredicates) {
        for (BlockPredicate blockpredicate : pPredicates) {
            if (blockpredicate.blocks().isEmpty()) {
                return List.of(UNKNOWN_USE);
            }
        }

        return pPredicates.stream()
            .flatMap(p_333785_ -> p_333785_.blocks().orElseThrow().stream())
            .distinct()
            .map(p_335858_ -> (Component)p_335858_.value().getName().withStyle(ChatFormatting.DARK_GRAY))
            .toList();
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther instanceof AdventureModePredicate adventuremodepredicate ? this.predicates.equals(adventuremodepredicate.predicates) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.predicates.hashCode();
    }

    @Override
    public String toString() {
        return "AdventureModePredicate{predicates=" + this.predicates + "}";
    }
}
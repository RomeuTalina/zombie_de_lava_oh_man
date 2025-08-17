package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
    @Override
    public Codec<ItemUsedOnLocationTrigger.TriggerInstance> codec() {
        return ItemUsedOnLocationTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pStack) {
        ServerLevel serverlevel = pPlayer.level();
        BlockState blockstate = serverlevel.getBlockState(pPos);
        LootParams lootparams = new LootParams.Builder(serverlevel)
            .withParameter(LootContextParams.ORIGIN, pPos.getCenter())
            .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
            .withParameter(LootContextParams.BLOCK_STATE, blockstate)
            .withParameter(LootContextParams.TOOL, pStack)
            .create(LootContextParamSets.ADVANCEMENT_LOCATION);
        LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
        this.trigger(pPlayer, p_286596_ -> p_286596_.matches(lootcontext));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ItemUsedOnLocationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_325222_ -> p_325222_.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::player),
                    ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::location)
                )
                .apply(p_325222_, ItemUsedOnLocationTrigger.TriggerInstance::new)
        );

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block pBlock) {
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock).build());
            return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... pConditions) {
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
                Arrays.stream(pConditions).map(LootItemCondition.Builder::build).toArray(LootItemCondition[]::new)
            );
            return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
        }

        public static <T extends Comparable<T>> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
            Block pBlock, Property<T> pProperty, String pValue
        ) {
            StatePropertiesPredicate.Builder statepropertiespredicate$builder = StatePropertiesPredicate.Builder.properties().hasProperty(pProperty, pValue);
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
                LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock).setProperties(statepropertiespredicate$builder).build()
            );
            return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(Block pBlock, Property<Boolean> pProperty, boolean pValue) {
            return placedBlockWithProperties(pBlock, pProperty, String.valueOf(pValue));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(Block pBlock, Property<Integer> pProperty, int pValue) {
            return placedBlockWithProperties(pBlock, pProperty, String.valueOf(pValue));
        }

        public static <T extends Comparable<T> & StringRepresentable> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
            Block pBlock, Property<T> pProperty, T pValue
        ) {
            return placedBlockWithProperties(pBlock, pProperty, pValue.getSerializedName());
        }

        private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
            ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
                LocationCheck.checkLocation(pLocation).build(), MatchTool.toolMatches(pTool).build()
            );
            return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
            return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
        }

        public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
            return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
        }

        public boolean matches(LootContext pContext) {
            return this.location.isEmpty() || this.location.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator p_310790_) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(p_310790_);
            this.location.ifPresent(p_357626_ -> p_310790_.validate(p_357626_, LootContextParamSets.ADVANCEMENT_LOCATION, "location"));
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return this.player;
        }
    }
}
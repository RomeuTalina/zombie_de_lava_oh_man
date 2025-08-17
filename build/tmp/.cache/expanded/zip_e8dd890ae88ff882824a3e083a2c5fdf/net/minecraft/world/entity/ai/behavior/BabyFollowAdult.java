package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
    public static OneShot<LivingEntity> create(UniformInt pFollowRange, float pSpeedModifier) {
        return create(pFollowRange, p_147421_ -> pSpeedModifier, MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
    }

    public static OneShot<LivingEntity> create(
        UniformInt pFollowRange, Function<LivingEntity, Float> pSpeedModifier, MemoryModuleType<? extends LivingEntity> pNearestVisibleAdult, boolean pTargetEyeHeight
    ) {
        return BehaviorBuilder.create(
            p_405320_ -> p_405320_.group(
                    p_405320_.present(pNearestVisibleAdult), p_405320_.registered(MemoryModuleType.LOOK_TARGET), p_405320_.absent(MemoryModuleType.WALK_TARGET)
                )
                .apply(
                    p_405320_,
                    (p_405313_, p_405314_, p_405315_) -> (p_258326_, p_408904_, p_258328_) -> {
                        if (!p_408904_.isBaby()) {
                            return false;
                        } else {
                            LivingEntity livingentity = p_405320_.get(p_405313_);
                            if (p_408904_.closerThan(livingentity, pFollowRange.getMaxValue() + 1) && !p_408904_.closerThan(livingentity, pFollowRange.getMinValue())) {
                                WalkTarget walktarget = new WalkTarget(
                                    new EntityTracker(livingentity, pTargetEyeHeight, pTargetEyeHeight), pSpeedModifier.apply(p_408904_), pFollowRange.getMinValue() - 1
                                );
                                p_405314_.set(new EntityTracker(livingentity, true, pTargetEyeHeight));
                                p_405315_.set(walktarget);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                )
        );
    }
}
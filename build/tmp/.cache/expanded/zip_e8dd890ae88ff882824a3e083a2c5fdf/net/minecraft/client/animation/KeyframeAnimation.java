package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimation {
    private final AnimationDefinition definition;
    private final List<KeyframeAnimation.Entry> entries;
    private final Vector3f scratchVector = new Vector3f();

    private KeyframeAnimation(AnimationDefinition pDefinition, List<KeyframeAnimation.Entry> pEntries) {
        this.definition = pDefinition;
        this.entries = pEntries;
    }

    static KeyframeAnimation bake(ModelPart pRoot, AnimationDefinition pDefinition) {
        List<KeyframeAnimation.Entry> list = new ArrayList<>();
        Function<String, ModelPart> function = pRoot.createPartLookup();

        for (Map.Entry<String, List<AnimationChannel>> entry : pDefinition.boneAnimations().entrySet()) {
            String s = entry.getKey();
            List<AnimationChannel> list1 = entry.getValue();
            ModelPart modelpart = function.apply(s);
            if (modelpart == null) {
                throw new IllegalArgumentException("Cannot animate " + s + ", which does not exist in model");
            }

            for (AnimationChannel animationchannel : list1) {
                list.add(new KeyframeAnimation.Entry(modelpart, animationchannel.target(), animationchannel.keyframes()));
            }
        }

        return new KeyframeAnimation(pDefinition, List.copyOf(list));
    }

    public void applyStatic() {
        this.apply(0L, 1.0F);
    }

    public void applyWalk(float pWalkAnimationPos, float pWalkAnimationSpeed, float pTimeMultiplier, float pSpeedMultiplier) {
        long i = (long)(pWalkAnimationPos * 50.0F * pTimeMultiplier);
        float f = Math.min(pWalkAnimationSpeed * pSpeedMultiplier, 1.0F);
        this.apply(i, f);
    }

    public void apply(AnimationState pAnimationState, float pAgeInTicks) {
        this.apply(pAnimationState, pAgeInTicks, 1.0F);
    }

    public void apply(AnimationState pAnimationState, float pAgeInTicks, float pSpeedMultiplier) {
        pAnimationState.ifStarted(p_408975_ -> this.apply((long)((float)p_408975_.getTimeInMillis(pAgeInTicks) * pSpeedMultiplier), 1.0F));
    }

    public void apply(long pTimeInMillis, float pScale) {
        float f = this.getElapsedSeconds(pTimeInMillis);

        for (KeyframeAnimation.Entry keyframeanimation$entry : this.entries) {
            keyframeanimation$entry.apply(f, pScale, this.scratchVector);
        }
    }

    private float getElapsedSeconds(long pTimeInMillis) {
        float f = (float)pTimeInMillis / 1000.0F;
        return this.definition.looping() ? f % this.definition.lengthInSeconds() : f;
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
        public void apply(float pElapsedSeconds, float pScale, Vector3f pScratchVector) {
            int i = Math.max(0, Mth.binarySearch(0, this.keyframes.length, p_406117_ -> pElapsedSeconds <= this.keyframes[p_406117_].timestamp()) - 1);
            int j = Math.min(this.keyframes.length - 1, i + 1);
            Keyframe keyframe = this.keyframes[i];
            Keyframe keyframe1 = this.keyframes[j];
            float f = pElapsedSeconds - keyframe.timestamp();
            float f1;
            if (j != i) {
                f1 = Mth.clamp(f / (keyframe1.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
            } else {
                f1 = 0.0F;
            }

            keyframe1.interpolation().apply(pScratchVector, f1, this.keyframes, i, j, pScale);
            this.target.apply(this.part, pScratchVector);
        }
    }
}
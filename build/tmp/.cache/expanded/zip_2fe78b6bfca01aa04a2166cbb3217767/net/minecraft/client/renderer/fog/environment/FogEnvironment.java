package net.minecraft.client.renderer.fog.environment;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class FogEnvironment {
    public abstract void setupFog(FogData p_407408_, Entity p_406747_, BlockPos p_406076_, ClientLevel p_409746_, float p_406048_, DeltaTracker p_410425_);

    public boolean providesColor() {
        return true;
    }

    public int getBaseColor(ClientLevel p_408834_, Camera p_407835_, int p_410065_, float p_407737_) {
        return -1;
    }

    public boolean modifiesDarkness() {
        return false;
    }

    public float getModifiedDarkness(LivingEntity p_408428_, float p_405830_, float p_410252_) {
        return p_405830_;
    }

    public abstract boolean isApplicable(@Nullable FogType p_409760_, Entity p_408188_);

    public void onNotApplicable() {
    }
}
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
    public abstract void setupFog(FogData pFogData, Entity pEntity, BlockPos pPos, ClientLevel pLevel, float pRenderDistance, DeltaTracker pDeltaTracker);

    public boolean providesColor() {
        return true;
    }

    public int getBaseColor(ClientLevel pLevel, Camera pCamera, int pRenderDistance, float pPartialTick) {
        return -1;
    }

    public boolean modifiesDarkness() {
        return false;
    }

    public float getModifiedDarkness(LivingEntity pEntity, float pDarkness, float pPartialTick) {
        return pDarkness;
    }

    public abstract boolean isApplicable(@Nullable FogType pFogType, Entity pEntity);

    public void onNotApplicable() {
    }
}
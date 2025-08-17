package com.mojang.blaze3d;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GpuOutOfMemoryException extends RuntimeException {
    public GpuOutOfMemoryException(String pMessage) {
        super(pMessage);
    }
}
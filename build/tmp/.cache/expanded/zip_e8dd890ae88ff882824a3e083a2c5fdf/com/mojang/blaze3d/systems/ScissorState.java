package com.mojang.blaze3d.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScissorState {
    private boolean enabled;
    private int x;
    private int y;
    private int width;
    private int height;

    public void enable(int pX, int pY, int pWidth, int pHeight) {
        this.enabled = true;
        this.x = pX;
        this.y = pY;
        this.width = pWidth;
        this.height = pHeight;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}
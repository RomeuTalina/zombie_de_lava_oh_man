package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ImageWidget extends AbstractWidget {
    ImageWidget(int pX, int pY, int pWidth, int pHeight) {
        super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY);
    }

    public static ImageWidget texture(int pWidth, int pHeight, ResourceLocation pTexture, int pTextureWidth, int pTextureHeight) {
        return new ImageWidget.Texture(0, 0, pWidth, pHeight, pTexture, pTextureWidth, pTextureHeight);
    }

    public static ImageWidget sprite(int pWidth, int pHeight, ResourceLocation pSprite) {
        return new ImageWidget.Sprite(0, 0, pWidth, pHeight, pSprite);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_275454_) {
    }

    @Override
    public void playDownSound(SoundManager p_297959_) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    public abstract void updateResource(ResourceLocation pResource);

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent p_298071_) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    static class Sprite extends ImageWidget {
        private ResourceLocation sprite;

        public Sprite(int pX, int pY, int pWidth, int pHeight, ResourceLocation pSprite) {
            super(pX, pY, pWidth, pHeight);
            this.sprite = pSprite;
        }

        @Override
        public void renderWidget(GuiGraphics p_298082_, int p_297761_, int p_298881_, float p_300382_) {
            p_298082_.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }

        @Override
        public void updateResource(ResourceLocation p_408597_) {
            this.sprite = p_408597_;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Texture extends ImageWidget {
        private ResourceLocation texture;
        private final int textureWidth;
        private final int textureHeight;

        public Texture(int pX, int pY, int pWidth, int pHeight, ResourceLocation pTexture, int pTextureWidth, int pTextureHeight) {
            super(pX, pY, pWidth, pHeight);
            this.texture = pTexture;
            this.textureWidth = pTextureWidth;
            this.textureHeight = pTextureHeight;
        }

        @Override
        protected void renderWidget(GuiGraphics p_301123_, int p_301197_, int p_299250_, float p_300781_) {
            p_301123_.blit(
                RenderPipelines.GUI_TEXTURED,
                this.texture,
                this.getX(),
                this.getY(),
                0.0F,
                0.0F,
                this.getWidth(),
                this.getHeight(),
                this.textureWidth,
                this.textureHeight
            );
        }

        @Override
        public void updateResource(ResourceLocation p_406146_) {
            this.texture = p_406146_;
        }
    }
}
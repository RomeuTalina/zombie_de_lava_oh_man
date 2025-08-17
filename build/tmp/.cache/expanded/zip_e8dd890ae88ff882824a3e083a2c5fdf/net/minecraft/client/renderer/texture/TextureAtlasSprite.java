package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureAtlasSprite {
    private final ResourceLocation atlasLocation;
    private final SpriteContents contents;
    private final boolean animated;
    final int x;
    final int y;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;

    protected TextureAtlasSprite(ResourceLocation pAtlasLocation, SpriteContents pContents, int pOriginX, int pOriginY, int pX, int pY) {
        this.atlasLocation = pAtlasLocation;
        this.contents = pContents;
        this.animated = pContents.metadata().getSection(AnimationMetadataSection.TYPE).isPresent();
        this.x = pX;
        this.y = pY;
        this.u0 = (float)pX / pOriginX;
        this.u1 = (float)(pX + pContents.width()) / pOriginX;
        this.v0 = (float)pY / pOriginY;
        this.v1 = (float)(pY + pContents.height()) / pOriginY;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public float getU0() {
        return this.u0;
    }

    public float getU1() {
        return this.u1;
    }

    public SpriteContents contents() {
        return this.contents;
    }

    public boolean isAnimated() {
        return this.animated;
    }

    @Nullable
    public TextureAtlasSprite.Ticker createTicker() {
        final SpriteTicker spriteticker = this.contents.createTicker();
        return spriteticker != null ? new TextureAtlasSprite.Ticker() {
            @Override
            public void tickAndUpload(GpuTexture p_393158_) {
                spriteticker.tickAndUpload(TextureAtlasSprite.this.x, TextureAtlasSprite.this.y, p_393158_);
            }

            @Override
            public void close() {
                spriteticker.close();
            }
        } : null;
    }

    public float getU(float pU) {
        float f = this.u1 - this.u0;
        return this.u0 + f * pU;
    }

    public float getUOffset(float pOffset) {
        float f = this.u1 - this.u0;
        return (pOffset - this.u0) / f;
    }

    public float getV0() {
        return this.v0;
    }

    public float getV1() {
        return this.v1;
    }

    public float getV(float pV) {
        float f = this.v1 - this.v0;
        return this.v0 + f * pV;
    }

    public float getVOffset(float pOffset) {
        float f = this.v1 - this.v0;
        return (pOffset - this.v0) / f;
    }

    public ResourceLocation atlasLocation() {
        return this.atlasLocation;
    }

    @Override
    public String toString() {
        return "TextureAtlasSprite{contents='"
            + this.contents
            + "', u0="
            + this.u0
            + ", u1="
            + this.u1
            + ", v0="
            + this.v0
            + ", v1="
            + this.v1
            + "}";
    }

    public void uploadFirstFrame(GpuTexture pTexture) {
        this.contents.uploadFirstFrame(this.x, this.y, pTexture);
    }

    private float atlasSize() {
        float f = this.contents.width() / (this.u1 - this.u0);
        float f1 = this.contents.height() / (this.v1 - this.v0);
        return Math.max(f1, f);
    }

    public float uvShrinkRatio() {
        return 4.0F / this.atlasSize();
    }

    public VertexConsumer wrap(VertexConsumer pConsumer) {
        return new SpriteCoordinateExpander(pConsumer, this);
    }

    @OnlyIn(Dist.CLIENT)
    public interface Ticker extends AutoCloseable {
        void tickAndUpload(GpuTexture pTexture);

        @Override
        void close();
    }

    public int getPixelRGBA(int frameIndex, int x, int y) {
        if (this.contents.animatedTexture != null) {
            x += this.contents.animatedTexture.getFrameX(frameIndex) * this.contents.width;
            y += this.contents.animatedTexture.getFrameY(frameIndex) * this.contents.height;
        }

        return this.contents.getOriginalImage().getPixel(x, y);
    }
}

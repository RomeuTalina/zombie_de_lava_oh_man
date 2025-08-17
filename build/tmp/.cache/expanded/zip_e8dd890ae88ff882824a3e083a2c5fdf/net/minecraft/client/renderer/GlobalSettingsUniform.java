package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class GlobalSettingsUniform implements AutoCloseable {
    public static final int UBO_SIZE = new Std140SizeCalculator().putVec2().putFloat().putFloat().putInt().get();
    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

    public void update(int pWindowWidth, int pWindowHeight, double pGlintStrength, long pGameTime, DeltaTracker pDeltaTracker, int pMenuBackgroundBlurriness) {
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, UBO_SIZE)
                .putVec2(pWindowWidth, pWindowHeight)
                .putFloat((float)pGlintStrength)
                .putFloat(((float)(pGameTime % 24000L) + pDeltaTracker.getGameTimeDeltaPartialTick(false)) / 24000.0F)
                .putInt(pMenuBackgroundBlurriness)
                .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
        }

        RenderSystem.setGlobalSettingsUniform(this.buffer);
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
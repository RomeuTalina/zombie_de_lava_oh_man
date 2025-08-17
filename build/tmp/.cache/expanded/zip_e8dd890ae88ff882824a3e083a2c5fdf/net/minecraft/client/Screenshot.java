package net.minecraft.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SCREENSHOT_DIR = "screenshots";

    public static void grab(File pGameDirectory, RenderTarget pRenderTarget, Consumer<Component> pMessageConsumer) {
        grab(pGameDirectory, null, pRenderTarget, 1, pMessageConsumer);
    }

    public static void grab(File pGameDirectory, @Nullable String pFilename, RenderTarget pRenderTarget, int pDownscaleFactor, Consumer<Component> pMessageConsumer) {
        takeScreenshot(
            pRenderTarget,
            pDownscaleFactor,
            p_389141_ -> {
                File file1 = new File(pGameDirectory, "screenshots");
                file1.mkdir();
                File file2;
                if (pFilename == null) {
                    file2 = getFile(file1);
                } else {
                    file2 = new File(file1, pFilename);
                }

                var event = new net.minecraftforge.client.event.ScreenshotEvent(p_389141_, file2);
                if (net.minecraftforge.client.event.ScreenshotEvent.BUS.post(event)) {
                    pMessageConsumer.accept(event.getCancelMessage());
                    return;
                }
                final File target = event.getScreenshotFile();

                Util.ioPool()
                    .execute(
                        () -> {
                            try {
                                NativeImage $$4x = p_389141_;

                                try {
                                    p_389141_.writeToFile(target);
                                    Component component = Component.literal(target.getName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(p_389149_ -> p_389149_.withClickEvent(new ClickEvent.OpenFile(file2.getAbsoluteFile())));
                                    if (event.getResultMessage() != null)
                                        pMessageConsumer.accept(event.getResultMessage());
                                    else
                                    pMessageConsumer.accept(Component.translatable("screenshot.success", component));
                                } catch (Throwable throwable1) {
                                    if (p_389141_ != null) {
                                        try {
                                            $$4x.close();
                                        } catch (Throwable throwable) {
                                            throwable1.addSuppressed(throwable);
                                        }
                                    }

                                    throw throwable1;
                                }

                                if (p_389141_ != null) {
                                    p_389141_.close();
                                }
                            } catch (Exception exception) {
                                LOGGER.warn("Couldn't save screenshot", (Throwable)exception);
                                pMessageConsumer.accept(Component.translatable("screenshot.failure", exception.getMessage()));
                            }
                        }
                    );
            }
        );
    }

    public static void takeScreenshot(RenderTarget pRenderTarget, Consumer<NativeImage> pWriter) {
        takeScreenshot(pRenderTarget, 1, pWriter);
    }

    public static void takeScreenshot(RenderTarget pRenderTarget, int pDownscaleFactor, Consumer<NativeImage> pWriter) {
        int i = pRenderTarget.width;
        int j = pRenderTarget.height;
        GpuTexture gputexture = pRenderTarget.getColorTexture();
        if (gputexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else if (i % pDownscaleFactor == 0 && j % pDownscaleFactor == 0) {
            GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, i * j * gputexture.getFormat().pixelSize());
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToBuffer(
                    gputexture,
                    gpubuffer,
                    0,
                    () -> {
                        try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
                            int k = j / pDownscaleFactor;
                            int l = i / pDownscaleFactor;
                            NativeImage nativeimage = new NativeImage(l, k, false);

                            for (int i1 = 0; i1 < k; i1++) {
                                for (int j1 = 0; j1 < l; j1++) {
                                    if (pDownscaleFactor == 1) {
                                        int i3 = gpubuffer$mappedview.data().getInt((j1 + i1 * i) * gputexture.getFormat().pixelSize());
                                        nativeimage.setPixelABGR(j1, j - i1 - 1, i3 | 0xFF000000);
                                    } else {
                                        int k1 = 0;
                                        int l1 = 0;
                                        int i2 = 0;

                                        for (int j2 = 0; j2 < pDownscaleFactor; j2++) {
                                            for (int k2 = 0; k2 < pDownscaleFactor; k2++) {
                                                int l2 = gpubuffer$mappedview.data()
                                                    .getInt((j1 * pDownscaleFactor + j2 + (i1 * pDownscaleFactor + k2) * i) * gputexture.getFormat().pixelSize());
                                                k1 += ARGB.red(l2);
                                                l1 += ARGB.green(l2);
                                                i2 += ARGB.blue(l2);
                                            }
                                        }

                                        int j3 = pDownscaleFactor * pDownscaleFactor;
                                        nativeimage.setPixelABGR(j1, k - i1 - 1, ARGB.color(255, k1 / j3, l1 / j3, i2 / j3));
                                    }
                                }
                            }

                            pWriter.accept(nativeimage);
                        }

                        gpubuffer.close();
                    },
                    0
                );
        } else {
            throw new IllegalArgumentException("Image size is not divisible by downscale factor");
        }
    }

    private static File getFile(File pGameDirectory) {
        String s = Util.getFilenameFormattedDateTime();
        int i = 1;

        while (true) {
            File file1 = new File(pGameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            i++;
        }
    }
}

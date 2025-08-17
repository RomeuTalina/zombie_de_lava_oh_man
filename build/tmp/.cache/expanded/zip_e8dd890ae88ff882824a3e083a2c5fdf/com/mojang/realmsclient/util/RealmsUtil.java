package com.mojang.realmsclient.util;

import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
    private static final int MINUTES = 60;
    private static final int HOURS = 3600;
    private static final int DAYS = 86400;

    public static Component convertToAgePresentation(long pMillis) {
        if (pMillis < 0L) {
            return RIGHT_NOW;
        } else {
            long i = pMillis / 1000L;
            if (i < 60L) {
                return Component.translatable("mco.time.secondsAgo", i);
            } else if (i < 3600L) {
                long l = i / 60L;
                return Component.translatable("mco.time.minutesAgo", l);
            } else if (i < 86400L) {
                long k = i / 3600L;
                return Component.translatable("mco.time.hoursAgo", k);
            } else {
                long j = i / 86400L;
                return Component.translatable("mco.time.daysAgo", j);
            }
        }
    }

    public static Component convertToAgePresentationFromInstant(Date pDate) {
        return convertToAgePresentation(System.currentTimeMillis() - pDate.getTime());
    }

    public static void renderPlayerFace(GuiGraphics pGuiGraphics, int pX, int pY, int pSize, UUID pPlayerUuid) {
        Minecraft minecraft = Minecraft.getInstance();
        ProfileResult profileresult = minecraft.getMinecraftSessionService().fetchProfile(pPlayerUuid, false);
        PlayerSkin playerskin = profileresult != null ? minecraft.getSkinManager().getInsecureSkin(profileresult.profile()) : DefaultPlayerSkin.get(pPlayerUuid);
        PlayerFaceRenderer.draw(pGuiGraphics, playerskin, pX, pY, pSize);
    }

    public static <T> CompletableFuture<T> supplyAsync(RealmsUtil.RealmsIoFunction<T> pAction, @Nullable Consumer<RealmsServiceException> pOnError) {
        return CompletableFuture.supplyAsync(() -> {
            RealmsClient realmsclient = RealmsClient.getOrCreate();

            try {
                return pAction.apply(realmsclient);
            } catch (Throwable throwable) {
                if (throwable instanceof RealmsServiceException realmsserviceexception) {
                    if (pOnError != null) {
                        pOnError.accept(realmsserviceexception);
                    }
                } else {
                    LOGGER.error("Unhandled exception", throwable);
                }

                throw new RuntimeException(throwable);
            }
        }, Util.nonCriticalIoPool());
    }

    public static CompletableFuture<Void> runAsync(RealmsUtil.RealmsIoConsumer pAction, @Nullable Consumer<RealmsServiceException> pOnError) {
        return supplyAsync(pAction, pOnError);
    }

    public static Consumer<RealmsServiceException> openScreenOnFailure(Function<RealmsServiceException, Screen> pScreenSupplier) {
        Minecraft minecraft = Minecraft.getInstance();
        return p_410171_ -> minecraft.execute(() -> minecraft.setScreen(pScreenSupplier.apply(p_410171_)));
    }

    public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(Function<RealmsServiceException, Screen> pScreenSupplier, String pErrorMessage) {
        return openScreenOnFailure(pScreenSupplier).andThen(p_408019_ -> LOGGER.error(pErrorMessage, (Throwable)p_408019_));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface RealmsIoConsumer extends RealmsUtil.RealmsIoFunction<Void> {
        void accept(RealmsClient pClient) throws RealmsServiceException;

        default Void apply(RealmsClient p_407565_) throws RealmsServiceException {
            this.accept(p_407565_);
            return null;
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface RealmsIoFunction<T> {
        T apply(RealmsClient pClient) throws RealmsServiceException;
    }
}
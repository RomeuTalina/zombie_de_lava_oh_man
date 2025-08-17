package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableBoolean;

@OnlyIn(Dist.CLIENT)
public class ToastManager {
    private static final int SLOT_COUNT = 5;
    private static final int ALL_SLOTS_OCCUPIED = -1;
    final Minecraft minecraft;
    private final List<ToastManager.ToastInstance<?>> visibleToasts = new ArrayList<>();
    private final BitSet occupiedSlots = new BitSet(5);
    private final Deque<Toast> queued = Queues.newArrayDeque();
    private final Set<SoundEvent> playedToastSounds = new HashSet<>();
    @Nullable
    private ToastManager.ToastInstance<NowPlayingToast> nowPlayingToast;

    public ToastManager(Minecraft pMinecraft, Options pOptions) {
        this.minecraft = pMinecraft;
        if (pOptions.showNowPlayingToast().get()) {
            this.createNowPlayingToast();
        }
    }

    public void update() {
        MutableBoolean mutableboolean = new MutableBoolean(false);
        this.visibleToasts.removeIf(p_389308_ -> {
            Toast.Visibility toast$visibility = p_389308_.visibility;
            p_389308_.update();
            if (p_389308_.visibility != toast$visibility && mutableboolean.isFalse()) {
                mutableboolean.setTrue();
                p_389308_.visibility.playSound(this.minecraft.getSoundManager());
            }

            if (p_389308_.hasFinishedRendering()) {
                this.occupiedSlots.clear(p_389308_.firstSlotIndex, p_389308_.firstSlotIndex + p_389308_.occupiedSlotCount);
                return true;
            } else {
                return false;
            }
        });
        if (!this.queued.isEmpty() && this.freeSlotCount() > 0) {
            this.queued.removeIf(p_389309_ -> {
                int i = p_389309_.occcupiedSlotCount();
                int j = this.findFreeSlotsIndex(i);
                if (j == -1) {
                    return false;
                } else {
                    this.visibleToasts.add(new ToastManager.ToastInstance<>(p_389309_, j, i));
                    this.occupiedSlots.set(j, j + i);
                    SoundEvent soundevent = p_389309_.getSoundEvent();
                    if (soundevent != null && this.playedToastSounds.add(soundevent)) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundevent, 1.0F, 1.0F));
                    }

                    return true;
                }
            });
        }

        this.playedToastSounds.clear();
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.update();
        }
    }

    public void render(GuiGraphics pGuiGraphics) {
        if (!this.minecraft.options.hideGui) {
            int i = pGuiGraphics.guiWidth();
            if (!this.visibleToasts.isEmpty()) {
                pGuiGraphics.nextStratum();
            }

            for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
                toastinstance.render(pGuiGraphics, i);
            }

            if (this.minecraft.options.showNowPlayingToast().get()
                && this.nowPlayingToast != null
                && (this.minecraft.screen == null || !(this.minecraft.screen instanceof PauseScreen))) {
                this.nowPlayingToast.render(pGuiGraphics, i);
            }
        }
    }

    private int findFreeSlotsIndex(int pSlots) {
        if (this.freeSlotCount() >= pSlots) {
            int i = 0;

            for (int j = 0; j < 5; j++) {
                if (this.occupiedSlots.get(j)) {
                    i = 0;
                } else if (++i == pSlots) {
                    return j + 1 - i;
                }
            }
        }

        return -1;
    }

    private int freeSlotCount() {
        return 5 - this.occupiedSlots.cardinality();
    }

    @Nullable
    public <T extends Toast> T getToast(Class<? extends T> pToastClass, Object pToken) {
        for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
            if (toastinstance != null
                && pToastClass.isAssignableFrom(toastinstance.getToast().getClass())
                && toastinstance.getToast().getToken().equals(pToken)) {
                return (T)toastinstance.getToast();
            }
        }

        for (Toast toast : this.queued) {
            if (pToastClass.isAssignableFrom(toast.getClass()) && toast.getToken().equals(pToken)) {
                return (T)toast;
            }
        }

        return null;
    }

    public void clear() {
        this.occupiedSlots.clear();
        this.visibleToasts.clear();
        this.queued.clear();
    }

    public void addToast(Toast pToast) {
        if (net.minecraftforge.client.event.ForgeEventFactoryClient.onToastAdd(pToast)) return;
        this.queued.add(pToast);
    }

    public void showNowPlayingToast() {
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.resetToast();
            this.nowPlayingToast.getToast().showToast(this.minecraft.options);
        }
    }

    public void hideNowPlayingToast() {
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.getToast().setWantedVisibility(Toast.Visibility.HIDE);
        }
    }

    public void createNowPlayingToast() {
        this.nowPlayingToast = new ToastManager.ToastInstance<>(new NowPlayingToast(), 0, 0);
    }

    public void removeNowPlayingToast() {
        this.nowPlayingToast = null;
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public double getNotificationDisplayTimeMultiplier() {
        return this.minecraft.options.notificationDisplayTime().get();
    }

    @OnlyIn(Dist.CLIENT)
    class ToastInstance<T extends Toast> {
        private static final long SLIDE_ANIMATION_DURATION_MS = 600L;
        private final T toast;
        final int firstSlotIndex;
        final int occupiedSlotCount;
        private long animationStartTime;
        private long becameFullyVisibleAt;
        Toast.Visibility visibility;
        private long fullyVisibleFor;
        private float visiblePortion;
        protected boolean hasFinishedRendering;

        ToastInstance(final T pToast, final int pFirstSlotIndex, final int pOccupiedSlotCount) {
            this.toast = pToast;
            this.firstSlotIndex = pFirstSlotIndex;
            this.occupiedSlotCount = pOccupiedSlotCount;
            this.resetToast();
        }

        public T getToast() {
            return this.toast;
        }

        public void resetToast() {
            this.animationStartTime = -1L;
            this.becameFullyVisibleAt = -1L;
            this.visibility = Toast.Visibility.HIDE;
            this.fullyVisibleFor = 0L;
            this.visiblePortion = 0.0F;
            this.hasFinishedRendering = false;
        }

        public boolean hasFinishedRendering() {
            return this.hasFinishedRendering;
        }

        private void calculateVisiblePortion(long pVisibilityTime) {
            float f = Mth.clamp((float)(pVisibilityTime - this.animationStartTime) / 600.0F, 0.0F, 1.0F);
            f *= f;
            if (this.visibility == Toast.Visibility.HIDE) {
                this.visiblePortion = 1.0F - f;
            } else {
                this.visiblePortion = f;
            }
        }

        public void update() {
            long i = Util.getMillis();
            if (this.animationStartTime == -1L) {
                this.animationStartTime = i;
                this.visibility = Toast.Visibility.SHOW;
            }

            if (this.visibility == Toast.Visibility.SHOW && i - this.animationStartTime <= 600L) {
                this.becameFullyVisibleAt = i;
            }

            this.fullyVisibleFor = i - this.becameFullyVisibleAt;
            this.calculateVisiblePortion(i);
            this.toast.update(ToastManager.this, this.fullyVisibleFor);
            Toast.Visibility toast$visibility = this.toast.getWantedVisibility();
            if (toast$visibility != this.visibility) {
                this.animationStartTime = i - (int)((1.0F - this.visiblePortion) * 600.0F);
                this.visibility = toast$visibility;
            }

            boolean flag = this.hasFinishedRendering;
            this.hasFinishedRendering = this.visibility == Toast.Visibility.HIDE && i - this.animationStartTime > 600L;
            if (this.hasFinishedRendering && !flag) {
                this.toast.onFinishedRendering();
            }
        }

        public void render(GuiGraphics pGuiGraphics, int pGuiWidth) {
            if (!this.hasFinishedRendering) {
                pGuiGraphics.pose().pushMatrix();
                pGuiGraphics.pose().translate(this.toast.xPos(pGuiWidth, this.visiblePortion), this.toast.yPos(this.firstSlotIndex));
                this.toast.render(pGuiGraphics, ToastManager.this.minecraft.font, this.fullyVisibleFor);
                pGuiGraphics.pose().popMatrix();
            }
        }
    }
}

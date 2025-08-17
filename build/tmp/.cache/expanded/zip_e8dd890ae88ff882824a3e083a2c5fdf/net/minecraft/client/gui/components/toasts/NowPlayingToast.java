package net.minecraft.client.gui.components.toasts;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NowPlayingToast implements Toast {
    private static final ResourceLocation NOW_PLAYING_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/now_playing");
    private static final ResourceLocation MUSIC_NOTES_SPRITE = ResourceLocation.parse("icon/music_notes");
    private static final int PADDING = 7;
    private static final int MUSIC_NOTES_SIZE = 16;
    private static final int HEIGHT = 30;
    private static final int MUSIC_NOTES_SPACE = 30;
    private static final int VISIBILITY_DURATION = 5000;
    private static final int TEXT_COLOR = DyeColor.LIGHT_GRAY.getTextColor();
    private static final long MUSIC_COLOR_CHANGE_FREQUENCY_MS = 25L;
    private static int musicNoteColorTick;
    private static long lastMusicNoteColorChange;
    private static int musicNoteColor = -1;
    private boolean updateToast;
    private double notificationDisplayTimeMultiplier;
    @Nullable
    private static String currentSong;
    private final Minecraft minecraft;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public NowPlayingToast() {
        this.minecraft = Minecraft.getInstance();
    }

    public static void renderToast(GuiGraphics pGuiGraphics, Font pFont) {
        if (currentSong != null) {
            pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOW_PLAYING_BACKGROUND_SPRITE, 0, 0, getWidth(currentSong, pFont), 30);
            int i = 7;
            pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MUSIC_NOTES_SPRITE, 7, 7, 16, 16, musicNoteColor);
            pGuiGraphics.drawString(pFont, getNowPlayingString(currentSong), 30, 15 - 9 / 2, TEXT_COLOR);
        }
    }

    public static void tickMusicNotes() {
        currentSong = Minecraft.getInstance().getMusicManager().getCurrentMusicTranslationKey();
        if (currentSong != null) {
            long i = System.currentTimeMillis();
            if (i > lastMusicNoteColorChange + 25L) {
                musicNoteColorTick++;
                lastMusicNoteColorChange = i;
                musicNoteColor = ColorLerper.getLerpedColor(ColorLerper.Type.MUSIC_NOTE, musicNoteColorTick);
            }
        }
    }

    private static Component getNowPlayingString(@Nullable String pCurrentSong) {
        return pCurrentSong == null ? Component.empty() : Component.translatable(pCurrentSong.replace("/", "."));
    }

    public void showToast(Options pOptions) {
        this.updateToast = true;
        this.notificationDisplayTimeMultiplier = pOptions.notificationDisplayTime().get();
        this.setWantedVisibility(Toast.Visibility.SHOW);
    }

    @Override
    public void update(ToastManager p_410553_, long p_408175_) {
        if (this.updateToast) {
            this.wantedVisibility = p_408175_ < 5000.0 * this.notificationDisplayTimeMultiplier ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
            tickMusicNotes();
        }
    }

    @Override
    public void render(GuiGraphics p_405968_, Font p_406720_, long p_408292_) {
        renderToast(p_405968_, p_406720_);
    }

    @Override
    public void onFinishedRendering() {
        this.updateToast = false;
    }

    @Override
    public int width() {
        return getWidth(currentSong, this.minecraft.font);
    }

    private static int getWidth(@Nullable String pText, Font pFont) {
        return 30 + pFont.width(getNowPlayingString(pText)) + 7;
    }

    @Override
    public int height() {
        return 30;
    }

    @Override
    public float xPos(int p_407715_, float p_407970_) {
        return this.width() * p_407970_ - this.width();
    }

    @Override
    public float yPos(int p_408258_) {
        return 0.0F;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    public void setWantedVisibility(Toast.Visibility pWantedVisibility) {
        this.wantedVisibility = pWantedVisibility;
    }
}
package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CHAT_HISTORY = 100;
    private static final int MESSAGE_NOT_FOUND = -1;
    private static final int MESSAGE_INDENT = 4;
    private static final int MESSAGE_TAG_MARGIN_LEFT = 4;
    private static final int BOTTOM_MARGIN = 40;
    private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
    private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    private final Minecraft minecraft;
    private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
    private final List<GuiMessage> allMessages = Lists.newArrayList();
    private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
    private int chatScrollbarPos;
    private boolean newMessageSinceScroll;
    private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

    public ChatComponent(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
        this.recentChat.addAll(pMinecraft.commandHistory().history());
    }

    public void tick() {
        if (!this.messageDeletionQueue.isEmpty()) {
            this.processMessageDeletionQueue();
        }
    }

    private int forEachLine(int pLinesPerPage, int pTickCount, boolean pFocused, int pBottomY, ChatComponent.LineConsumer pAction) {
        int i = this.getLineHeight();
        int j = 0;

        for (int k = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, pLinesPerPage) - 1; k >= 0; k--) {
            int l = k + this.chatScrollbarPos;
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(l);
            if (guimessage$line != null) {
                int i1 = pTickCount - guimessage$line.addedTime();
                float f = pFocused ? 1.0F : (float)getTimeFactor(i1);
                if (f > 1.0E-5F) {
                    j++;
                    int j1 = pBottomY - k * i;
                    int k1 = j1 - i;
                    pAction.accept(0, k1, j1, guimessage$line, k, f);
                }
            }
        }

        return j;
    }

    public void render(GuiGraphics pGuiGraphics, int pTickCount, int pMouseX, int pMouseY, boolean pFocused) {
        if (!this.isChatHidden()) {
            int i = this.getLinesPerPage();
            int j = this.trimmedMessages.size();
            if (j > 0) {
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("chat");
                float f = (float)this.getScale();
                int k = Mth.ceil(this.getWidth() / f);
                int l = pGuiGraphics.guiHeight();
                pGuiGraphics.pose().pushMatrix();
                pGuiGraphics.pose().scale(f, f);
                pGuiGraphics.pose().translate(4.0F, 0.0F);
                int i1 = Mth.floor((l - 40) / f);
                int j1 = this.getMessageEndIndexAt(this.screenToChatX(pMouseX), this.screenToChatY(pMouseY));
                float f1 = this.minecraft.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
                float f2 = this.minecraft.options.textBackgroundOpacity().get().floatValue();
                double d0 = this.minecraft.options.chatLineSpacing().get();
                int k1 = (int)Math.round(-8.0 * (d0 + 1.0) + 4.0 * d0);
                this.forEachLine(i, pTickCount, pFocused, i1, (p_404827_, p_404828_, p_404829_, p_404830_, p_404831_, p_404832_) -> {
                    pGuiGraphics.fill(p_404827_ - 4, p_404828_, p_404827_ + k + 4 + 4, p_404829_, ARGB.color(p_404832_ * f2, -16777216));
                    GuiMessageTag guimessagetag = p_404830_.tag();
                    if (guimessagetag != null) {
                        int l4 = ARGB.color(p_404832_ * f1, guimessagetag.indicatorColor());
                        pGuiGraphics.fill(p_404827_ - 4, p_404828_, p_404827_ - 2, p_404829_, l4);
                        if (p_404831_ == j1 && guimessagetag.icon() != null) {
                            int i5 = this.getTagIconLeft(p_404830_);
                            int j5 = p_404829_ + k1 + 9;
                            this.drawTagIcon(pGuiGraphics, i5, j5, guimessagetag.icon());
                        }
                    }
                });
                int l1 = this.forEachLine(i, pTickCount, pFocused, i1, (p_404836_, p_404837_, p_404838_, p_404839_, p_404840_, p_404841_) -> {
                    int l4 = p_404838_ + k1;
                    pGuiGraphics.drawString(this.minecraft.font, p_404839_.content(), p_404836_, l4, ARGB.color(p_404841_ * f1, -1));
                });
                long i2 = this.minecraft.getChatListener().queueSize();
                if (i2 > 0L) {
                    int j2 = (int)(128.0F * f1);
                    int k2 = (int)(255.0F * f2);
                    pGuiGraphics.pose().pushMatrix();
                    pGuiGraphics.pose().translate(0.0F, i1);
                    pGuiGraphics.fill(-2, 0, k + 4, 9, k2 << 24);
                    pGuiGraphics.drawString(this.minecraft.font, Component.translatable("chat.queue", i2), 0, 1, ARGB.color(j2, -1));
                    pGuiGraphics.pose().popMatrix();
                }

                if (pFocused) {
                    int j4 = this.getLineHeight();
                    int k4 = j * j4;
                    int l2 = l1 * j4;
                    int i3 = this.chatScrollbarPos * l2 / j - i1;
                    int j3 = l2 * l2 / k4;
                    if (k4 != l2) {
                        int k3 = i3 > 0 ? 170 : 96;
                        int l3 = this.newMessageSinceScroll ? 13382451 : 3355562;
                        int i4 = k + 4;
                        pGuiGraphics.fill(i4, -i3, i4 + 2, -i3 - j3, ARGB.color(k3, l3));
                        pGuiGraphics.fill(i4 + 2, -i3, i4 + 1, -i3 - j3, ARGB.color(k3, 13421772));
                    }
                }

                pGuiGraphics.pose().popMatrix();
                profilerfiller.pop();
            }
        }
    }

    private void drawTagIcon(GuiGraphics pGuiGraphics, int pLeft, int pBottom, GuiMessageTag.Icon pTagIcon) {
        int i = pBottom - pTagIcon.height - 1;
        pTagIcon.draw(pGuiGraphics, pLeft, i);
    }

    private int getTagIconLeft(GuiMessage.Line pLine) {
        return this.minecraft.font.width(pLine.content()) + 4;
    }

    private boolean isChatHidden() {
        return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
    }

    private static double getTimeFactor(int pCounter) {
        double d0 = pCounter / 200.0;
        d0 = 1.0 - d0;
        d0 *= 10.0;
        d0 = Mth.clamp(d0, 0.0, 1.0);
        return d0 * d0;
    }

    public void clearMessages(boolean pClearSentMsgHistory) {
        this.minecraft.getChatListener().clearQueue();
        this.messageDeletionQueue.clear();
        this.trimmedMessages.clear();
        this.allMessages.clear();
        if (pClearSentMsgHistory) {
            this.recentChat.clear();
            this.recentChat.addAll(this.minecraft.commandHistory().history());
        }
    }

    public void addMessage(Component pChatComponent) {
        this.addMessage(pChatComponent, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
    }

    public void addMessage(Component pChatComponent, @Nullable MessageSignature pHeaderSignature, @Nullable GuiMessageTag pTag) {
        GuiMessage guimessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), pChatComponent, pHeaderSignature, pTag);
        this.logChatMessage(guimessage);
        this.addMessageToDisplayQueue(guimessage);
        this.addMessageToQueue(guimessage);
    }

    private void logChatMessage(GuiMessage pMessage) {
        String s = pMessage.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
        String s1 = Optionull.map(pMessage.tag(), GuiMessageTag::logTag);
        if (s1 != null) {
            LOGGER.info("[{}] [CHAT] {}", s1, s);
        } else {
            LOGGER.info("[CHAT] {}", s);
        }
    }

    private void addMessageToDisplayQueue(GuiMessage pMessage) {
        int i = Mth.floor(this.getWidth() / this.getScale());
        GuiMessageTag.Icon guimessagetag$icon = pMessage.icon();
        if (guimessagetag$icon != null) {
            i -= guimessagetag$icon.width + 4 + 2;
        }

        List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(pMessage.content(), i, this.minecraft.font);
        boolean flag = this.isChatFocused();

        for (int j = 0; j < list.size(); j++) {
            FormattedCharSequence formattedcharsequence = list.get(j);
            if (flag && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }

            boolean flag1 = j == list.size() - 1;
            this.trimmedMessages.add(0, new GuiMessage.Line(pMessage.addedTime(), formattedcharsequence, pMessage.tag(), flag1));
        }

        while (this.trimmedMessages.size() > 100) {
            this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
        }
    }

    private void addMessageToQueue(GuiMessage pMessage) {
        this.allMessages.add(0, pMessage);

        while (this.allMessages.size() > 100) {
            this.allMessages.remove(this.allMessages.size() - 1);
        }
    }

    private void processMessageDeletionQueue() {
        int i = this.minecraft.gui.getGuiTicks();
        this.messageDeletionQueue.removeIf(p_250713_ -> i >= p_250713_.deletableAfter() ? this.deleteMessageOrDelay(p_250713_.signature()) == null : false);
    }

    public void deleteMessage(MessageSignature pMessageSignature) {
        ChatComponent.DelayedMessageDeletion chatcomponent$delayedmessagedeletion = this.deleteMessageOrDelay(pMessageSignature);
        if (chatcomponent$delayedmessagedeletion != null) {
            this.messageDeletionQueue.add(chatcomponent$delayedmessagedeletion);
        }
    }

    @Nullable
    private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature pMessageSignature) {
        int i = this.minecraft.gui.getGuiTicks();
        ListIterator<GuiMessage> listiterator = this.allMessages.listIterator();

        while (listiterator.hasNext()) {
            GuiMessage guimessage = listiterator.next();
            if (pMessageSignature.equals(guimessage.signature())) {
                int j = guimessage.addedTime() + 60;
                if (i >= j) {
                    listiterator.set(this.createDeletedMarker(guimessage));
                    this.refreshTrimmedMessages();
                    return null;
                }

                return new ChatComponent.DelayedMessageDeletion(pMessageSignature, j);
            }
        }

        return null;
    }

    private GuiMessage createDeletedMarker(GuiMessage pMessage) {
        return new GuiMessage(pMessage.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
    }

    public void rescaleChat() {
        this.resetChatScroll();
        this.refreshTrimmedMessages();
    }

    private void refreshTrimmedMessages() {
        this.trimmedMessages.clear();

        for (GuiMessage guimessage : Lists.reverse(this.allMessages)) {
            this.addMessageToDisplayQueue(guimessage);
        }
    }

    public ArrayListDeque<String> getRecentChat() {
        return this.recentChat;
    }

    public void addRecentChat(String pMessage) {
        if (!pMessage.equals(this.recentChat.peekLast())) {
            if (this.recentChat.size() >= 100) {
                this.recentChat.removeFirst();
            }

            this.recentChat.addLast(pMessage);
        }

        if (pMessage.startsWith("/")) {
            this.minecraft.commandHistory().addCommand(pMessage);
        }
    }

    public void resetChatScroll() {
        this.chatScrollbarPos = 0;
        this.newMessageSinceScroll = false;
    }

    public void scrollChat(int pPosInc) {
        this.chatScrollbarPos += pPosInc;
        int i = this.trimmedMessages.size();
        if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
            this.chatScrollbarPos = i - this.getLinesPerPage();
        }

        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
        }
    }

    public boolean handleChatQueueClicked(double pMouseX, double pMouseY) {
        if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
            ChatListener chatlistener = this.minecraft.getChatListener();
            if (chatlistener.queueSize() == 0L) {
                return false;
            } else {
                double d0 = pMouseX - 2.0;
                double d1 = this.minecraft.getWindow().getGuiScaledHeight() - pMouseY - 40.0;
                if (d0 <= Mth.floor(this.getWidth() / this.getScale()) && d1 < 0.0 && d1 > Mth.floor(-9.0 * this.getScale())) {
                    chatlistener.acceptNextDelayedMessage();
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    @Nullable
    public Style getClickedComponentStyleAt(double pMouseX, double pMouseY) {
        double d0 = this.screenToChatX(pMouseX);
        double d1 = this.screenToChatY(pMouseY);
        int i = this.getMessageLineIndexAt(d0, d1);
        if (i >= 0 && i < this.trimmedMessages.size()) {
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(i);
            return this.minecraft.font.getSplitter().componentStyleAtWidth(guimessage$line.content(), Mth.floor(d0));
        } else {
            return null;
        }
    }

    @Nullable
    public GuiMessageTag getMessageTagAt(double pMouseX, double pMouseY) {
        double d0 = this.screenToChatX(pMouseX);
        double d1 = this.screenToChatY(pMouseY);
        int i = this.getMessageEndIndexAt(d0, d1);
        if (i >= 0 && i < this.trimmedMessages.size()) {
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(i);
            GuiMessageTag guimessagetag = guimessage$line.tag();
            if (guimessagetag != null && this.hasSelectedMessageTag(d0, guimessage$line, guimessagetag)) {
                return guimessagetag;
            }
        }

        return null;
    }

    private boolean hasSelectedMessageTag(double pX, GuiMessage.Line pLine, GuiMessageTag pTag) {
        if (pX < 0.0) {
            return true;
        } else {
            GuiMessageTag.Icon guimessagetag$icon = pTag.icon();
            if (guimessagetag$icon == null) {
                return false;
            } else {
                int i = this.getTagIconLeft(pLine);
                int j = i + guimessagetag$icon.width;
                return pX >= i && pX <= j;
            }
        }
    }

    private double screenToChatX(double pX) {
        return pX / this.getScale() - 4.0;
    }

    private double screenToChatY(double pY) {
        double d0 = this.minecraft.getWindow().getGuiScaledHeight() - pY - 40.0;
        return d0 / (this.getScale() * this.getLineHeight());
    }

    private int getMessageEndIndexAt(double pMouseX, double pMouseY) {
        int i = this.getMessageLineIndexAt(pMouseX, pMouseY);
        if (i == -1) {
            return -1;
        } else {
            while (i >= 0) {
                if (this.trimmedMessages.get(i).endOfEntry()) {
                    return i;
                }

                i--;
            }

            return i;
        }
    }

    private int getMessageLineIndexAt(double pMouseX, double pMouseY) {
        if (this.isChatFocused() && !this.isChatHidden()) {
            if (!(pMouseX < -4.0) && !(pMouseX > Mth.floor(this.getWidth() / this.getScale()))) {
                int i = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
                if (pMouseY >= 0.0 && pMouseY < i) {
                    int j = Mth.floor(pMouseY + this.chatScrollbarPos);
                    if (j >= 0 && j < this.trimmedMessages.size()) {
                        return j;
                    }
                }

                return -1;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public boolean isChatFocused() {
        return this.minecraft.screen instanceof ChatScreen;
    }

    public int getWidth() {
        return getWidth(this.minecraft.options.chatWidth().get());
    }

    public int getHeight() {
        return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
    }

    public double getScale() {
        return this.minecraft.options.chatScale().get();
    }

    public static int getWidth(double pWidth) {
        int i = 320;
        int j = 40;
        return Mth.floor(pWidth * 280.0 + 40.0);
    }

    public static int getHeight(double pHeight) {
        int i = 180;
        int j = 20;
        return Mth.floor(pHeight * 160.0 + 20.0);
    }

    public static double defaultUnfocusedPct() {
        int i = 180;
        int j = 20;
        return 70.0 / (getHeight(1.0) - 20);
    }

    public int getLinesPerPage() {
        return this.getHeight() / this.getLineHeight();
    }

    private int getLineHeight() {
        return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
    }

    public ChatComponent.State storeState() {
        return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
    }

    public void restoreState(ChatComponent.State pState) {
        this.recentChat.clear();
        this.recentChat.addAll(pState.history);
        this.messageDeletionQueue.clear();
        this.messageDeletionQueue.addAll(pState.delayedMessageDeletions);
        this.allMessages.clear();
        this.allMessages.addAll(pState.messages);
        this.refreshTrimmedMessages();
    }

    @OnlyIn(Dist.CLIENT)
    record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface LineConsumer {
        void accept(int pX, int pStartY, int pEndY, GuiMessage.Line pLine, int pIndex, float pFade);
    }

    @OnlyIn(Dist.CLIENT)
    public static class State {
        final List<GuiMessage> messages;
        final List<String> history;
        final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

        public State(List<GuiMessage> pMessages, List<String> pHistory, List<ChatComponent.DelayedMessageDeletion> pDelayedMessageDeletions) {
            this.messages = pMessages;
            this.history = pHistory;
            this.delayedMessageDeletions = pDelayedMessageDeletions;
        }
    }
}
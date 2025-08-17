package net.minecraft.client.gui.screens.dialog.body;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DialogBodyHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MapCodec<? extends DialogBody>, DialogBodyHandler<?>> HANDLERS = new HashMap<>();

    private static <B extends DialogBody> void register(MapCodec<B> pCodec, DialogBodyHandler<? super B> pHandler) {
        HANDLERS.put(pCodec, pHandler);
    }

    @Nullable
    private static <B extends DialogBody> DialogBodyHandler<B> getHandler(B pBody) {
        return (DialogBodyHandler<B>)HANDLERS.get(pBody.mapCodec());
    }

    @Nullable
    public static <B extends DialogBody> LayoutElement createBodyElement(DialogScreen<?> pScreen, B pBody) {
        DialogBodyHandler<B> dialogbodyhandler = getHandler(pBody);
        if (dialogbodyhandler == null) {
            LOGGER.warn("Unrecognized dialog body {}", pBody);
            return null;
        } else {
            return dialogbodyhandler.createControls(pScreen, pBody);
        }
    }

    public static void bootstrap() {
        register(PlainMessage.MAP_CODEC, new DialogBodyHandlers.PlainMessageHandler());
        register(ItemBody.MAP_CODEC, new DialogBodyHandlers.ItemHandler());
    }

    static void runActionOnParent(DialogScreen<?> pScreen, @Nullable Style pStyle) {
        if (pStyle != null) {
            ClickEvent clickevent = pStyle.getClickEvent();
            if (clickevent != null) {
                pScreen.runAction(Optional.of(clickevent));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ItemHandler implements DialogBodyHandler<ItemBody> {
        public LayoutElement createControls(DialogScreen<?> p_409486_, ItemBody p_410433_) {
            if (p_410433_.description().isPresent()) {
                PlainMessage plainmessage = p_410433_.description().get();
                LinearLayout linearlayout = LinearLayout.horizontal().spacing(2);
                linearlayout.defaultCellSetting().alignVerticallyMiddle();
                ItemDisplayWidget itemdisplaywidget = new ItemDisplayWidget(
                    Minecraft.getInstance(),
                    0,
                    0,
                    p_410433_.width(),
                    p_410433_.height(),
                    CommonComponents.EMPTY,
                    p_410433_.item(),
                    p_410433_.showDecorations(),
                    p_410433_.showTooltip()
                );
                linearlayout.addChild(itemdisplaywidget);
                linearlayout.addChild(
                    new FocusableTextWidget(plainmessage.width(), plainmessage.contents(), p_409486_.getFont(), false, false, 4)
                        .configureStyleHandling(true, p_407925_ -> DialogBodyHandlers.runActionOnParent(p_409486_, p_407925_))
                );
                return linearlayout;
            } else {
                return new ItemDisplayWidget(
                    Minecraft.getInstance(),
                    0,
                    0,
                    p_410433_.width(),
                    p_410433_.height(),
                    p_410433_.item().getHoverName(),
                    p_410433_.item(),
                    p_410433_.showDecorations(),
                    p_410433_.showTooltip()
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class PlainMessageHandler implements DialogBodyHandler<PlainMessage> {
        public LayoutElement createControls(DialogScreen<?> p_405971_, PlainMessage p_408125_) {
            return new FocusableTextWidget(p_408125_.width(), p_408125_.contents(), p_405971_.getFont(), false, false, 4)
                .configureStyleHandling(true, p_409257_ -> DialogBodyHandlers.runActionOnParent(p_405971_, p_409257_))
                .setCentered(true);
        }
    }
}
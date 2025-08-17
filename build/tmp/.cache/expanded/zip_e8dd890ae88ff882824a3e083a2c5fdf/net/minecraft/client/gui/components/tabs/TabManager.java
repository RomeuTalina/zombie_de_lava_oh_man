package net.minecraft.client.gui.components.tabs;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabManager {
    private final Consumer<AbstractWidget> addWidget;
    private final Consumer<AbstractWidget> removeWidget;
    private final Consumer<Tab> onSelected;
    private final Consumer<Tab> onDeselected;
    @Nullable
    private Tab currentTab;
    @Nullable
    private ScreenRectangle tabArea;

    public TabManager(Consumer<AbstractWidget> pAddWidget, Consumer<AbstractWidget> pRemoveWidget) {
        this(pAddWidget, pRemoveWidget, p_406722_ -> {}, p_408604_ -> {});
    }

    public TabManager(Consumer<AbstractWidget> pAddWidget, Consumer<AbstractWidget> pRemoveWidget, Consumer<Tab> pOnSelected, Consumer<Tab> pOnDeselected) {
        this.addWidget = pAddWidget;
        this.removeWidget = pRemoveWidget;
        this.onSelected = pOnSelected;
        this.onDeselected = pOnDeselected;
    }

    public void setTabArea(ScreenRectangle pTabArea) {
        this.tabArea = pTabArea;
        Tab tab = this.getCurrentTab();
        if (tab != null) {
            tab.doLayout(pTabArea);
        }
    }

    public void setCurrentTab(Tab pTab, boolean pPlayClickSound) {
        if (!Objects.equals(this.currentTab, pTab)) {
            if (this.currentTab != null) {
                this.currentTab.visitChildren(this.removeWidget);
            }

            Tab tab = this.currentTab;
            this.currentTab = pTab;
            pTab.visitChildren(this.addWidget);
            if (this.tabArea != null) {
                pTab.doLayout(this.tabArea);
            }

            if (pPlayClickSound) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            this.onDeselected.accept(tab);
            this.onSelected.accept(this.currentTab);
        }
    }

    @Nullable
    public Tab getCurrentTab() {
        return this.currentTab;
    }
}
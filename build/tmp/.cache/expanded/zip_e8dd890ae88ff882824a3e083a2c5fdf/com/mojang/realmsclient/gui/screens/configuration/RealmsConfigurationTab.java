package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface RealmsConfigurationTab {
    void updateData(RealmsServer pServer);

    default void onSelected(RealmsServer pServer) {
    }

    default void onDeselected(RealmsServer pServer) {
    }
}
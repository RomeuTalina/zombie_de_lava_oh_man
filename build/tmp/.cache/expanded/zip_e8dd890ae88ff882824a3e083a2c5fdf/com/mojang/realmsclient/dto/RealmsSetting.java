package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RealmsSetting(@SerializedName("name") String name, @SerializedName("value") String value) implements ReflectionBasedSerialization {
    public static RealmsSetting hardcoreSetting(boolean pHardcore) {
        return new RealmsSetting("hardcore", Boolean.toString(pHardcore));
    }

    public static boolean isHardcore(List<RealmsSetting> pSettings) {
        for (RealmsSetting realmssetting : pSettings) {
            if (realmssetting.name().equals("hardcore")) {
                return Boolean.parseBoolean(realmssetting.value());
            }
        }

        return false;
    }
}
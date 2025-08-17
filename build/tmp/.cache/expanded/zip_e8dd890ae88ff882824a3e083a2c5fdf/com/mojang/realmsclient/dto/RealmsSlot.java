package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RealmsSlot implements ReflectionBasedSerialization {
    @SerializedName("slotId")
    public int slotId;
    @SerializedName("options")
    @JsonAdapter(RealmsSlot.RealmsWorldOptionsJsonAdapter.class)
    public RealmsWorldOptions options;
    @SerializedName("settings")
    public List<RealmsSetting> settings;

    public RealmsSlot(int pSlotId, RealmsWorldOptions pOptions, List<RealmsSetting> pSettings) {
        this.slotId = pSlotId;
        this.options = pOptions;
        this.settings = pSettings;
    }

    public static RealmsSlot defaults(int pSlotId) {
        return new RealmsSlot(pSlotId, RealmsWorldOptions.createEmptyDefaults(), List.of(RealmsSetting.hardcoreSetting(false)));
    }

    public RealmsSlot clone() {
        return new RealmsSlot(this.slotId, this.options.clone(), new ArrayList<>(this.settings));
    }

    public boolean isHardcore() {
        return RealmsSetting.isHardcore(this.settings);
    }

    @OnlyIn(Dist.CLIENT)
    static class RealmsWorldOptionsJsonAdapter extends TypeAdapter<RealmsWorldOptions> {
        private RealmsWorldOptionsJsonAdapter() {
        }

        public void write(JsonWriter pWriter, RealmsWorldOptions pOptions) throws IOException {
            pWriter.jsonValue(new GuardedSerializer().toJson(pOptions));
        }

        public RealmsWorldOptions read(JsonReader pReader) throws IOException {
            String s = pReader.nextString();
            return RealmsWorldOptions.parse(new GuardedSerializer(), s);
        }
    }
}
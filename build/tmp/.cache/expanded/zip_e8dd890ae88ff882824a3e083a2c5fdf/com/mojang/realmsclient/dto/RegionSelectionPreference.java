package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public enum RegionSelectionPreference {
    AUTOMATIC_PLAYER(0, "realms.configuration.region_preference.automatic_player"),
    AUTOMATIC_OWNER(1, "realms.configuration.region_preference.automatic_owner"),
    MANUAL(2, "");

    public static final RegionSelectionPreference DEFAULT_SELECTION = AUTOMATIC_PLAYER;
    public final int id;
    public final String translationKey;

    private RegionSelectionPreference(final int pId, final String pTranslationKey) {
        this.id = pId;
        this.translationKey = pTranslationKey;
    }

    @OnlyIn(Dist.CLIENT)
    public static class RegionSelectionPreferenceJsonAdapter extends TypeAdapter<RegionSelectionPreference> {
        private static final Logger LOGGER = LogUtils.getLogger();

        public void write(JsonWriter pWriter, RegionSelectionPreference pRegionSelectionPreference) throws IOException {
            pWriter.value((long)pRegionSelectionPreference.id);
        }

        public RegionSelectionPreference read(JsonReader pReader) throws IOException {
            int i = pReader.nextInt();

            for (RegionSelectionPreference regionselectionpreference : RegionSelectionPreference.values()) {
                if (regionselectionpreference.id == i) {
                    return regionselectionpreference;
                }
            }

            LOGGER.warn("Unsupported RegionSelectionPreference {}", i);
            return RegionSelectionPreference.DEFAULT_SELECTION;
        }
    }
}
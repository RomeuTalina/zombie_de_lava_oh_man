package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

public class DetectedVersion {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final WorldVersion BUILT_IN = createFromConstants();

    private static WorldVersion createFromConstants() {
        return new WorldVersion.Simple(
            UUID.randomUUID().toString().replaceAll("-", ""), "1.21.8", new DataVersion(4440, "main"), SharedConstants.getProtocolVersion(), 64, 81, new Date(), true
        );
    }

    private static WorldVersion createFromJson(JsonObject pJson) {
        JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "pack_version");
        return new WorldVersion.Simple(
            GsonHelper.getAsString(pJson, "id"),
            GsonHelper.getAsString(pJson, "name"),
            new DataVersion(GsonHelper.getAsInt(pJson, "world_version"), GsonHelper.getAsString(pJson, "series_id", "main")),
            GsonHelper.getAsInt(pJson, "protocol_version"),
            GsonHelper.getAsInt(jsonobject, "resource"),
            GsonHelper.getAsInt(jsonobject, "data"),
            Date.from(ZonedDateTime.parse(GsonHelper.getAsString(pJson, "build_time")).toInstant()),
            GsonHelper.getAsBoolean(pJson, "stable")
        );
    }

    public static WorldVersion tryDetectVersion() {
        try {
            WorldVersion worldversion;
            try (InputStream inputstream = DetectedVersion.class.getResourceAsStream("/version.json")) {
                if (inputstream == null) {
                    LOGGER.warn("Missing version information!");
                    return BUILT_IN;
                }

                try (InputStreamReader inputstreamreader = new InputStreamReader(inputstream)) {
                    worldversion = createFromJson(GsonHelper.parse(inputstreamreader));
                }
            }

            return worldversion;
        } catch (JsonParseException | IOException ioexception) {
            throw new IllegalStateException("Game version information is corrupt", ioexception);
        }
    }
}
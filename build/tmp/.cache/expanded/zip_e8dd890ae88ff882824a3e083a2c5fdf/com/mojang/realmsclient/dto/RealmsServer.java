package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.util.UUIDTypeAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServer extends ValueObject implements ReflectionBasedSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_VALUE = -1;
    public static final Component WORLD_CLOSED_COMPONENT = Component.translatable("mco.play.button.realm.closed");
    @SerializedName("id")
    public long id = -1L;
    @Nullable
    @SerializedName("remoteSubscriptionId")
    public String remoteSubscriptionId;
    @Nullable
    @SerializedName("name")
    public String name;
    @SerializedName("motd")
    public String motd = "";
    @SerializedName("state")
    public RealmsServer.State state = RealmsServer.State.CLOSED;
    @Nullable
    @SerializedName("owner")
    public String owner;
    @SerializedName("ownerUUID")
    @JsonAdapter(UUIDTypeAdapter.class)
    public UUID ownerUUID = Util.NIL_UUID;
    @SerializedName("players")
    public List<PlayerInfo> players = Lists.newArrayList();
    @SerializedName("slots")
    private List<RealmsSlot> slotList = createEmptySlots();
    @Exclude
    public Map<Integer, RealmsSlot> slots = new HashMap<>();
    @SerializedName("expired")
    public boolean expired;
    @SerializedName("expiredTrial")
    public boolean expiredTrial = false;
    @SerializedName("daysLeft")
    public int daysLeft;
    @SerializedName("worldType")
    public RealmsServer.WorldType worldType = RealmsServer.WorldType.NORMAL;
    @SerializedName("isHardcore")
    public boolean isHardcore = false;
    @SerializedName("gameMode")
    public int gameMode = -1;
    @SerializedName("activeSlot")
    public int activeSlot = -1;
    @Nullable
    @SerializedName("minigameName")
    public String minigameName;
    @SerializedName("minigameId")
    public int minigameId = -1;
    @Nullable
    @SerializedName("minigameImage")
    public String minigameImage;
    @SerializedName("parentWorldId")
    public long parentRealmId = -1L;
    @Nullable
    @SerializedName("parentWorldName")
    public String parentWorldName;
    @SerializedName("activeVersion")
    public String activeVersion = "";
    @SerializedName("compatibility")
    public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
    @Nullable
    @SerializedName("regionSelectionPreference")
    public RegionSelectionPreferenceDto regionSelectionPreference;

    public String getDescription() {
        return this.motd;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getMinigameName() {
        return this.minigameName;
    }

    public void setName(String pName) {
        this.name = pName;
    }

    public void setDescription(String pMotd) {
        this.motd = pMotd;
    }

    public static RealmsServer parse(GuardedSerializer pSerializer, String pJson) {
        try {
            RealmsServer realmsserver = pSerializer.fromJson(pJson, RealmsServer.class);
            if (realmsserver == null) {
                LOGGER.error("Could not parse McoServer: {}", pJson);
                return new RealmsServer();
            } else {
                finalize(realmsserver);
                return realmsserver;
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServer: {}", exception.getMessage());
            return new RealmsServer();
        }
    }

    public static void finalize(RealmsServer pServer) {
        if (pServer.players == null) {
            pServer.players = Lists.newArrayList();
        }

        if (pServer.slotList == null) {
            pServer.slotList = createEmptySlots();
        }

        if (pServer.slots == null) {
            pServer.slots = new HashMap<>();
        }

        if (pServer.worldType == null) {
            pServer.worldType = RealmsServer.WorldType.NORMAL;
        }

        if (pServer.activeVersion == null) {
            pServer.activeVersion = "";
        }

        if (pServer.compatibility == null) {
            pServer.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
        }

        if (pServer.regionSelectionPreference == null) {
            pServer.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
        }

        sortInvited(pServer);
        finalizeSlots(pServer);
    }

    private static void sortInvited(RealmsServer pServer) {
        pServer.players
            .sort(
                (p_87502_, p_87503_) -> ComparisonChain.start()
                    .compareFalseFirst(p_87503_.getAccepted(), p_87502_.getAccepted())
                    .compare(p_87502_.getName().toLowerCase(Locale.ROOT), p_87503_.getName().toLowerCase(Locale.ROOT))
                    .result()
            );
    }

    private static void finalizeSlots(RealmsServer pServer) {
        pServer.slotList.forEach(p_404754_ -> pServer.slots.put(p_404754_.slotId, p_404754_));

        for (int i = 1; i <= 3; i++) {
            if (!pServer.slots.containsKey(i)) {
                pServer.slots.put(i, RealmsSlot.defaults(i));
            }
        }
    }

    private static List<RealmsSlot> createEmptySlots() {
        List<RealmsSlot> list = new ArrayList<>();
        list.add(RealmsSlot.defaults(1));
        list.add(RealmsSlot.defaults(2));
        list.add(RealmsSlot.defaults(3));
        return list;
    }

    public boolean isCompatible() {
        return this.compatibility.isCompatible();
    }

    public boolean needsUpgrade() {
        return this.compatibility.needsUpgrade();
    }

    public boolean needsDowngrade() {
        return this.compatibility.needsDowngrade();
    }

    public boolean shouldPlayButtonBeActive() {
        boolean flag = !this.expired && this.state == RealmsServer.State.OPEN;
        return flag && (this.isCompatible() || this.needsUpgrade() || this.isSelfOwnedServer());
    }

    private boolean isSelfOwnedServer() {
        return Minecraft.getInstance().isLocalPlayer(this.ownerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
    }

    @Override
    public boolean equals(Object pOther) {
        if (pOther == null) {
            return false;
        } else if (pOther == this) {
            return true;
        } else if (pOther.getClass() != this.getClass()) {
            return false;
        } else {
            RealmsServer realmsserver = (RealmsServer)pOther;
            return new EqualsBuilder()
                .append(this.id, realmsserver.id)
                .append(this.name, realmsserver.name)
                .append(this.motd, realmsserver.motd)
                .append(this.state, realmsserver.state)
                .append(this.owner, realmsserver.owner)
                .append(this.expired, realmsserver.expired)
                .append(this.worldType, this.worldType)
                .isEquals();
        }
    }

    public RealmsServer clone() {
        RealmsServer realmsserver = new RealmsServer();
        realmsserver.id = this.id;
        realmsserver.remoteSubscriptionId = this.remoteSubscriptionId;
        realmsserver.name = this.name;
        realmsserver.motd = this.motd;
        realmsserver.state = this.state;
        realmsserver.owner = this.owner;
        realmsserver.players = this.players;
        realmsserver.slotList = this.slotList.stream().map(RealmsSlot::clone).toList();
        realmsserver.slots = this.cloneSlots(this.slots);
        realmsserver.expired = this.expired;
        realmsserver.expiredTrial = this.expiredTrial;
        realmsserver.daysLeft = this.daysLeft;
        realmsserver.worldType = this.worldType;
        realmsserver.isHardcore = this.isHardcore;
        realmsserver.gameMode = this.gameMode;
        realmsserver.ownerUUID = this.ownerUUID;
        realmsserver.minigameName = this.minigameName;
        realmsserver.activeSlot = this.activeSlot;
        realmsserver.minigameId = this.minigameId;
        realmsserver.minigameImage = this.minigameImage;
        realmsserver.parentWorldName = this.parentWorldName;
        realmsserver.parentRealmId = this.parentRealmId;
        realmsserver.activeVersion = this.activeVersion;
        realmsserver.compatibility = this.compatibility;
        realmsserver.regionSelectionPreference = this.regionSelectionPreference != null ? this.regionSelectionPreference.clone() : null;
        return realmsserver;
    }

    public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> pSlots) {
        Map<Integer, RealmsSlot> map = Maps.newHashMap();

        for (Entry<Integer, RealmsSlot> entry : pSlots.entrySet()) {
            map.put(entry.getKey(), new RealmsSlot(entry.getKey(), entry.getValue().options.clone(), entry.getValue().settings));
        }

        return map;
    }

    public boolean isSnapshotRealm() {
        return this.parentRealmId != -1L;
    }

    public boolean isMinigameActive() {
        return this.worldType == RealmsServer.WorldType.MINIGAME;
    }

    public String getWorldName(int pSlot) {
        return this.name == null
            ? this.slots.get(pSlot).options.getSlotName(pSlot)
            : this.name + " (" + this.slots.get(pSlot).options.getSlotName(pSlot) + ")";
    }

    public ServerData toServerData(String pIp) {
        return new ServerData(Objects.requireNonNullElse(this.name, "unknown server"), pIp, ServerData.Type.REALM);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Compatibility {
        UNVERIFIABLE,
        INCOMPATIBLE,
        RELEASE_TYPE_INCOMPATIBLE,
        NEEDS_DOWNGRADE,
        NEEDS_UPGRADE,
        COMPATIBLE;

        public boolean isCompatible() {
            return this == COMPATIBLE;
        }

        public boolean needsUpgrade() {
            return this == NEEDS_UPGRADE;
        }

        public boolean needsDowngrade() {
            return this == NEEDS_DOWNGRADE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class McoServerComparator implements Comparator<RealmsServer> {
        private final String refOwner;

        public McoServerComparator(String pRefOwner) {
            this.refOwner = pRefOwner;
        }

        public int compare(RealmsServer pFirst, RealmsServer pSecond) {
            return ComparisonChain.start()
                .compareTrueFirst(pFirst.isSnapshotRealm(), pSecond.isSnapshotRealm())
                .compareTrueFirst(pFirst.state == RealmsServer.State.UNINITIALIZED, pSecond.state == RealmsServer.State.UNINITIALIZED)
                .compareTrueFirst(pFirst.expiredTrial, pSecond.expiredTrial)
                .compareTrueFirst(Objects.equals(pFirst.owner, this.refOwner), Objects.equals(pSecond.owner, this.refOwner))
                .compareFalseFirst(pFirst.expired, pSecond.expired)
                .compareTrueFirst(pFirst.state == RealmsServer.State.OPEN, pSecond.state == RealmsServer.State.OPEN)
                .compare(pFirst.id, pSecond.id)
                .result();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum State {
        CLOSED,
        OPEN,
        UNINITIALIZED;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum WorldType {
        NORMAL,
        MINIGAME,
        ADVENTUREMAP,
        EXPERIENCE,
        INSPIRATION;
    }
}
package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.VillageSectionsDebugRenderer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.GameEventDebugPayload;
import net.minecraft.network.protocol.common.custom.GameEventListenerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.common.custom.NeighborUpdatesDebugPayload;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiAddedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiRemovedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiTicketCountDebugPayload;
import net.minecraft.network.protocol.common.custom.RaidsDebugPayload;
import net.minecraft.network.protocol.common.custom.RedstoneWireOrientationsDebugPayload;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.network.protocol.common.custom.VillageSectionsDebugPayload;
import net.minecraft.network.protocol.common.custom.WorldGenAttemptDebugPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Crypt;
import net.minecraft.util.HashOps;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientPacketListener extends ClientCommonPacketListenerImpl implements ClientGamePacketListener, TickablePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
    private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
    private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
    private static final Component RECONFIGURE_SCREEN_MESSAGE = Component.translatable("connect.reconfiguring");
    private static final Component BAD_CHAT_INDEX = Component.translatable("multiplayer.disconnect.bad_chat_index");
    private static final Component COMMAND_SEND_CONFIRM_TITLE = Component.translatable("multiplayer.confirm_command.title");
    private static final int PENDING_OFFSET_THRESHOLD = 64;
    public static final int TELEPORT_INTERPOLATION_THRESHOLD = 64;
    public static final ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> COMMAND_NODE_BUILDER = new ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider>() {
        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> createLiteral(String p_409225_) {
            return LiteralArgumentBuilder.literal(p_409225_);
        }

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> createArgument(String p_410024_, ArgumentType<?> p_408738_, @Nullable ResourceLocation p_410100_) {
            RequiredArgumentBuilder<ClientSuggestionProvider, ?> requiredargumentbuilder = RequiredArgumentBuilder.argument(p_410024_, p_408738_);
            if (p_410100_ != null) {
                requiredargumentbuilder.suggests(SuggestionProviders.getProvider(p_410100_));
            }

            return requiredargumentbuilder;
        }

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> configure(
            ArgumentBuilder<ClientSuggestionProvider, ?> p_407529_, boolean p_408694_, boolean p_408103_
        ) {
            if (p_408694_) {
                p_407529_.executes(p_407908_ -> 0);
            }

            if (p_408103_) {
                p_407529_.requires(ClientSuggestionProvider::allowsRestrictedCommands);
            }

            return p_407529_;
        }
    };
    private final GameProfile localGameProfile;
    private ClientLevel level;
    private ClientLevel.ClientLevelData levelData;
    private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
    private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet<>();
    private final ClientAdvancements advancements;
    private final ClientSuggestionProvider suggestionsProvider;
    private final ClientSuggestionProvider restrictedSuggestionsProvider;
    private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
    private int serverChunkRadius = 3;
    private int serverSimulationDistance = 3;
    private final RandomSource random = RandomSource.createThreadSafe();
    public CommandDispatcher<ClientSuggestionProvider> commands = new CommandDispatcher<>();
    private ClientRecipeContainer recipes = new ClientRecipeContainer(Map.of(), SelectableRecipe.SingleInputSet.empty());
    private final UUID id = UUID.randomUUID();
    private Set<ResourceKey<Level>> levels;
    private final RegistryAccess.Frozen registryAccess;
    private final FeatureFlagSet enabledFeatures;
    private final PotionBrewing potionBrewing;
    private FuelValues fuelValues;
    private final HashedPatchMap.HashGenerator decoratedHashOpsGenerator;
    private OptionalInt removedPlayerVehicleId = OptionalInt.empty();
    @Nullable
    private LocalChatSession chatSession;
    private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
    private int nextChatIndex;
    private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
    private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
    @Nullable
    private CompletableFuture<Optional<ProfileKeyPair>> keyPairFuture;
    @Nullable
    private ClientInformation remoteClientInformation;
    private final ChunkBatchSizeCalculator chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
    private final PingDebugMonitor pingDebugMonitor;
    private final DebugSampleSubscriber debugSampleSubscriber;
    @Nullable
    private LevelLoadStatusManager levelLoadStatusManager;
    private boolean serverEnforcesSecureChat;
    private boolean seenInsecureChatWarning = false;
    private volatile boolean closed;
    private final Scoreboard scoreboard = new Scoreboard();
    private final ClientWaypointManager waypointManager = new ClientWaypointManager();
    private final SessionSearchTrees searchTrees = new SessionSearchTrees();
    private final List<WeakReference<CacheSlot<?, ?>>> cacheSlots = new ArrayList<>();

    public ClientPacketListener(Minecraft p_253924_, Connection p_253614_, CommonListenerCookie p_298329_) {
        super(p_253924_, p_253614_, p_298329_);
        this.localGameProfile = p_298329_.localGameProfile();
        this.registryAccess = p_298329_.receivedRegistries();
        RegistryOps<HashCode> registryops = this.registryAccess.createSerializationContext(HashOps.CRC32C_INSTANCE);
        this.decoratedHashOpsGenerator = p_389339_ -> p_389339_.encodeValue(registryops)
            .getOrThrow(p_389341_ -> new IllegalArgumentException("Failed to hash " + p_389339_ + ": " + p_389341_))
            .asInt();
        this.enabledFeatures = p_298329_.enabledFeatures();
        this.advancements = new ClientAdvancements(p_253924_, this.telemetryManager);
        this.suggestionsProvider = new ClientSuggestionProvider(this, p_253924_, true);
        this.restrictedSuggestionsProvider = new ClientSuggestionProvider(this, p_253924_, false);
        this.pingDebugMonitor = new PingDebugMonitor(this, p_253924_.getDebugOverlay().getPingLogger());
        this.debugSampleSubscriber = new DebugSampleSubscriber(this, p_253924_.getDebugOverlay());
        if (p_298329_.chatState() != null) {
            p_253924_.gui.getChat().restoreState(p_298329_.chatState());
        }

        this.potionBrewing = PotionBrewing.bootstrap(this.enabledFeatures);
        this.fuelValues = FuelValues.vanillaBurnTimes(p_298329_.receivedRegistries(), this.enabledFeatures);
    }

    public ClientSuggestionProvider getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    public void close() {
        this.closed = true;
        this.clearLevel();
        this.telemetryManager.onDisconnect();
    }

    public void clearLevel() {
        this.clearCacheSlots();
        this.level = null;
        this.levelLoadStatusManager = null;
    }

    private void clearCacheSlots() {
        for (WeakReference<CacheSlot<?, ?>> weakreference : this.cacheSlots) {
            CacheSlot<?, ?> cacheslot = weakreference.get();
            if (cacheslot != null) {
                cacheslot.clear();
            }
        }

        this.cacheSlots.clear();
    }

    public RecipeAccess recipes() {
        return this.recipes;
    }

    @Override
    public void handleLogin(ClientboundLoginPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
        CommonPlayerSpawnInfo commonplayerspawninfo = pPacket.commonPlayerSpawnInfo();
        List<ResourceKey<Level>> list = Lists.newArrayList(pPacket.levels());
        Collections.shuffle(list);
        this.levels = Sets.newLinkedHashSet(list);
        ResourceKey<Level> resourcekey = commonplayerspawninfo.dimension();
        Holder<DimensionType> holder = commonplayerspawninfo.dimensionType();
        this.serverChunkRadius = pPacket.chunkRadius();
        this.serverSimulationDistance = pPacket.simulationDistance();
        boolean flag = commonplayerspawninfo.isDebug();
        boolean flag1 = commonplayerspawninfo.isFlat();
        int i = commonplayerspawninfo.seaLevel();
        ClientLevel.ClientLevelData clientlevel$clientleveldata = new ClientLevel.ClientLevelData(Difficulty.NORMAL, pPacket.hardcore(), flag1);
        this.levelData = clientlevel$clientleveldata;
        this.level = new ClientLevel(
            this,
            clientlevel$clientleveldata,
            resourcekey,
            holder,
            this.serverChunkRadius,
            this.serverSimulationDistance,
            this.minecraft.levelRenderer,
            flag,
            commonplayerspawninfo.seed(),
            i
        );
        this.minecraft.setLevel(this.level, ReceivingLevelScreen.Reason.OTHER);
        if (this.minecraft.player == null) {
            this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
            this.minecraft.player.setYRot(-180.0F);
            if (this.minecraft.getSingleplayerServer() != null) {
                this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
            }
        }

        this.minecraft.debugRenderer.clear();
        this.minecraft.player.resetPos();
        net.minecraftforge.client.event.ForgeEventFactoryClient.firePlayerLogin(this.minecraft.gameMode, this.minecraft.player, this.minecraft.getConnection().connection);
        this.minecraft.player.setId(pPacket.playerId());
        this.level.addEntity(this.minecraft.player);
        this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
        this.minecraft.cameraEntity = this.minecraft.player;
        this.startWaitingForNewLevel(this.minecraft.player, this.level, ReceivingLevelScreen.Reason.OTHER);
        this.minecraft.player.setReducedDebugInfo(pPacket.reducedDebugInfo());
        this.minecraft.player.setShowDeathScreen(pPacket.showDeathScreen());
        this.minecraft.player.setDoLimitedCrafting(pPacket.doLimitedCrafting());
        this.minecraft.player.setLastDeathLocation(commonplayerspawninfo.lastDeathLocation());
        this.minecraft.player.setPortalCooldown(commonplayerspawninfo.portalCooldown());
        this.minecraft.gameMode.setLocalMode(commonplayerspawninfo.gameType(), commonplayerspawninfo.previousGameType());
        this.minecraft.options.setServerRenderDistance(pPacket.chunkRadius());
        this.chatSession = null;
        this.signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
        this.nextChatIndex = 0;
        this.lastSeenMessages = new LastSeenMessagesTracker(20);
        this.messageSignatureCache = MessageSignatureCache.createDefault();
        if (this.connection.isEncrypted()) {
            this.prepareKeyPair();
        }

        this.telemetryManager.onPlayerInfoReceived(commonplayerspawninfo.gameType(), pPacket.hardcore());
        this.minecraft.quickPlayLog().log(this.minecraft);
        this.serverEnforcesSecureChat = pPacket.enforcesSecureChat();
        if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
            SystemToast systemtoast = SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST);
            this.minecraft.getToastManager().addToast(systemtoast);
            this.seenInsecureChatWarning = true;
        }
    }

    @Override
    public void handleAddEntity(ClientboundAddEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == pPacket.getId()) {
            this.removedPlayerVehicleId = OptionalInt.empty();
        }

        Entity entity = this.createEntityFromPacket(pPacket);
        if (entity != null) {
            entity.recreateFromPacket(pPacket);
            this.level.addEntity(entity);
            this.postAddEntitySoundInstance(entity);
        } else {
            LOGGER.warn("Skipping Entity with id {}", pPacket.getType());
        }
    }

    @Nullable
    private Entity createEntityFromPacket(ClientboundAddEntityPacket pPacket) {
        EntityType<?> entitytype = pPacket.getType();
        if (entitytype == EntityType.PLAYER) {
            PlayerInfo playerinfo = this.getPlayerInfo(pPacket.getUUID());
            if (playerinfo == null) {
                LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", pPacket.getUUID());
                return null;
            } else {
                return new RemotePlayer(this.level, playerinfo.getProfile());
            }
        } else {
            return entitytype.create(this.level, EntitySpawnReason.LOAD);
        }
    }

    private void postAddEntitySoundInstance(Entity pEntity) {
        if (pEntity instanceof AbstractMinecart abstractminecart) {
            this.minecraft.getSoundManager().play(new MinecartSoundInstance(abstractminecart));
        } else if (pEntity instanceof Bee bee) {
            boolean flag = bee.isAngry();
            BeeSoundInstance beesoundinstance;
            if (flag) {
                beesoundinstance = new BeeAggressiveSoundInstance(bee);
            } else {
                beesoundinstance = new BeeFlyingSoundInstance(bee);
            }

            this.minecraft.getSoundManager().queueTickingSound(beesoundinstance);
        }
    }

    @Override
    public void handleSetEntityMotion(ClientboundSetEntityMotionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            entity.lerpMotion(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
        }
    }

    @Override
    public void handleSetEntityData(ClientboundSetEntityDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.id());
        if (entity != null) {
            entity.getEntityData().assignValues(pPacket.packedItems());
        }
    }

    @Override
    public void handleEntityPositionSync(ClientboundEntityPositionSyncPacket p_364334_) {
        PacketUtils.ensureRunningOnSameThread(p_364334_, this, this.minecraft);
        Entity entity = this.level.getEntity(p_364334_.id());
        if (entity != null) {
            Vec3 vec3 = p_364334_.values().position();
            entity.getPositionCodec().setBase(vec3);
            if (!entity.isLocalInstanceAuthoritative()) {
                float f = p_364334_.values().yRot();
                float f1 = p_364334_.values().xRot();
                boolean flag = entity.position().distanceToSqr(vec3) > 4096.0;
                if (this.level.isTickingEntity(entity) && !flag) {
                    entity.moveOrInterpolateTo(vec3, f, f1);
                } else {
                    entity.snapTo(vec3, f, f1);
                }

                if (!entity.isInterpolating() && entity.hasIndirectPassenger(this.minecraft.player)) {
                    entity.positionRider(this.minecraft.player);
                    this.minecraft.player.setOldPosAndRot();
                }

                entity.setOnGround(p_364334_.onGround());
            }
        }
    }

    @Override
    public void handleTeleportEntity(ClientboundTeleportEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.id());
        if (entity == null) {
            if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == pPacket.id()) {
                LOGGER.debug(
                    "Trying to teleport entity with id {}, that was formerly player vehicle, applying teleport to player instead", pPacket.id()
                );
                setValuesFromPositionPacket(pPacket.change(), pPacket.relatives(), this.minecraft.player, false);
                this.connection
                    .send(
                        new ServerboundMovePlayerPacket.PosRot(
                            this.minecraft.player.getX(),
                            this.minecraft.player.getY(),
                            this.minecraft.player.getZ(),
                            this.minecraft.player.getYRot(),
                            this.minecraft.player.getXRot(),
                            false,
                            false
                        )
                    );
            }
        } else {
            boolean flag = pPacket.relatives().contains(Relative.X)
                || pPacket.relatives().contains(Relative.Y)
                || pPacket.relatives().contains(Relative.Z);
            boolean flag1 = this.level.isTickingEntity(entity) || !entity.isLocalInstanceAuthoritative() || flag;
            boolean flag2 = setValuesFromPositionPacket(pPacket.change(), pPacket.relatives(), entity, flag1);
            entity.setOnGround(pPacket.onGround());
            if (!flag2 && entity.hasIndirectPassenger(this.minecraft.player)) {
                entity.positionRider(this.minecraft.player);
                this.minecraft.player.setOldPosAndRot();
                if (entity.isLocalInstanceAuthoritative()) {
                    this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
                }
            }
        }
    }

    @Override
    public void handleTickingState(ClientboundTickingStatePacket p_311347_) {
        PacketUtils.ensureRunningOnSameThread(p_311347_, this, this.minecraft);
        if (this.minecraft.level != null) {
            TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
            tickratemanager.setTickRate(p_311347_.tickRate());
            tickratemanager.setFrozen(p_311347_.isFrozen());
        }
    }

    @Override
    public void handleTickingStep(ClientboundTickingStepPacket p_309537_) {
        PacketUtils.ensureRunningOnSameThread(p_309537_, this, this.minecraft);
        if (this.minecraft.level != null) {
            TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
            tickratemanager.setFrozenTicksToRun(p_309537_.tickSteps());
        }
    }

    @Override
    public void handleSetHeldSlot(ClientboundSetHeldSlotPacket p_365551_) {
        PacketUtils.ensureRunningOnSameThread(p_365551_, this, this.minecraft);
        if (Inventory.isHotbarSlot(p_365551_.slot())) {
            this.minecraft.player.getInventory().setSelectedSlot(p_365551_.slot());
        }
    }

    @Override
    public void handleMoveEntity(ClientboundMoveEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            if (entity.isLocalInstanceAuthoritative()) {
                VecDeltaCodec vecdeltacodec1 = entity.getPositionCodec();
                Vec3 vec31 = vecdeltacodec1.decode(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
                vecdeltacodec1.setBase(vec31);
            } else {
                if (pPacket.hasPosition()) {
                    VecDeltaCodec vecdeltacodec = entity.getPositionCodec();
                    Vec3 vec3 = vecdeltacodec.decode(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
                    vecdeltacodec.setBase(vec3);
                    if (pPacket.hasRotation()) {
                        entity.moveOrInterpolateTo(vec3, pPacket.getYRot(), pPacket.getXRot());
                    } else {
                        entity.moveOrInterpolateTo(vec3, entity.getYRot(), entity.getXRot());
                    }
                } else if (pPacket.hasRotation()) {
                    entity.moveOrInterpolateTo(entity.position(), pPacket.getYRot(), pPacket.getXRot());
                }

                entity.setOnGround(pPacket.isOnGround());
            }
        }
    }

    @Override
    public void handleMinecartAlongTrack(ClientboundMoveMinecartPacket p_364082_) {
        PacketUtils.ensureRunningOnSameThread(p_364082_, this, this.minecraft);
        if (p_364082_.getEntity(this.level) instanceof AbstractMinecart abstractminecart) {
            if (abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
                newminecartbehavior.lerpSteps.addAll(p_364082_.lerpSteps());
            }
        }
    }

    @Override
    public void handleRotateMob(ClientboundRotateHeadPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            entity.lerpHeadTo(pPacket.getYHeadRot(), 3);
        }
    }

    @Override
    public void handleRemoveEntities(ClientboundRemoveEntitiesPacket p_182633_) {
        PacketUtils.ensureRunningOnSameThread(p_182633_, this, this.minecraft);
        p_182633_.getEntityIds().forEach((int p_357779_) -> {
            Entity entity = this.level.getEntity(p_357779_);
            if (entity != null) {
                if (entity.hasIndirectPassenger(this.minecraft.player)) {
                    LOGGER.debug("Remove entity {}:{} that has player as passenger", entity.getType(), p_357779_);
                    this.removedPlayerVehicleId = OptionalInt.of(p_357779_);
                }

                this.level.removeEntity(p_357779_, Entity.RemovalReason.DISCARDED);
            }
        });
    }

    @Override
    public void handleMovePlayer(ClientboundPlayerPositionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        if (!player.isPassenger()) {
            setValuesFromPositionPacket(pPacket.change(), pPacket.relatives(), player, false);
        }

        this.connection.send(new ServerboundAcceptTeleportationPacket(pPacket.id()));
        this.connection
            .send(
                new ServerboundMovePlayerPacket.PosRot(
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false, false
                )
            );
    }

    private static boolean setValuesFromPositionPacket(PositionMoveRotation pPositionMoveRotation, Set<Relative> pRelatives, Entity pEntity, boolean pLerp) {
        PositionMoveRotation positionmoverotation = PositionMoveRotation.of(pEntity);
        PositionMoveRotation positionmoverotation1 = PositionMoveRotation.calculateAbsolute(positionmoverotation, pPositionMoveRotation, pRelatives);
        boolean flag = positionmoverotation.position().distanceToSqr(positionmoverotation1.position()) > 4096.0;
        if (pLerp && !flag) {
            pEntity.moveOrInterpolateTo(positionmoverotation1.position(), positionmoverotation1.yRot(), positionmoverotation1.xRot());
            pEntity.setDeltaMovement(positionmoverotation1.deltaMovement());
            return true;
        } else {
            pEntity.setPos(positionmoverotation1.position());
            pEntity.setDeltaMovement(positionmoverotation1.deltaMovement());
            pEntity.setYRot(positionmoverotation1.yRot());
            pEntity.setXRot(positionmoverotation1.xRot());
            PositionMoveRotation positionmoverotation2 = new PositionMoveRotation(pEntity.oldPosition(), Vec3.ZERO, pEntity.yRotO, pEntity.xRotO);
            PositionMoveRotation positionmoverotation3 = PositionMoveRotation.calculateAbsolute(positionmoverotation2, pPositionMoveRotation, pRelatives);
            pEntity.setOldPosAndRot(positionmoverotation3.position(), positionmoverotation3.yRot(), positionmoverotation3.xRot());
            return false;
        }
    }

    @Override
    public void handleRotatePlayer(ClientboundPlayerRotationPacket p_367721_) {
        PacketUtils.ensureRunningOnSameThread(p_367721_, this, this.minecraft);
        Player player = this.minecraft.player;
        player.setYRot(p_367721_.yRot());
        player.setXRot(p_367721_.xRot());
        player.setOldRot();
        this.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), false, false));
    }

    @Override
    public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        pPacket.runUpdates((p_284633_, p_284634_) -> this.level.setServerVerifiedBlockState(p_284633_, p_284634_, 19));
    }

    @Override
    public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket p_194241_) {
        PacketUtils.ensureRunningOnSameThread(p_194241_, this, this.minecraft);
        int i = p_194241_.getX();
        int j = p_194241_.getZ();
        this.updateLevelChunk(i, j, p_194241_.getChunkData());
        ClientboundLightUpdatePacketData clientboundlightupdatepacketdata = p_194241_.getLightData();
        this.level.queueLightUpdate(() -> {
            this.applyLightData(i, j, clientboundlightupdatepacketdata, false);
            LevelChunk levelchunk = this.level.getChunkSource().getChunk(i, j, false);
            if (levelchunk != null) {
                this.enableChunkLight(levelchunk, i, j);
                this.minecraft.levelRenderer.onChunkReadyToRender(levelchunk.getPos());
            }
        });
    }

    @Override
    public void handleChunksBiomes(ClientboundChunksBiomesPacket p_275437_) {
        PacketUtils.ensureRunningOnSameThread(p_275437_, this, this.minecraft);

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata : p_275437_.chunkBiomeData()) {
            this.level
                .getChunkSource()
                .replaceBiomes(
                    clientboundchunksbiomespacket$chunkbiomedata.pos().x,
                    clientboundchunksbiomespacket$chunkbiomedata.pos().z,
                    clientboundchunksbiomespacket$chunkbiomedata.getReadBuffer()
                );
        }

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata1 : p_275437_.chunkBiomeData()) {
            this.level
                .onChunkLoaded(
                    new ChunkPos(
                        clientboundchunksbiomespacket$chunkbiomedata1.pos().x, clientboundchunksbiomespacket$chunkbiomedata1.pos().z
                    )
                );
        }

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata2 : p_275437_.chunkBiomeData()) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = this.level.getMinSectionY(); k <= this.level.getMaxSectionY(); k++) {
                        this.minecraft
                            .levelRenderer
                            .setSectionDirty(
                                clientboundchunksbiomespacket$chunkbiomedata2.pos().x + i,
                                k,
                                clientboundchunksbiomespacket$chunkbiomedata2.pos().z + j
                            );
                    }
                }
            }
        }
    }

    private void updateLevelChunk(int pX, int pZ, ClientboundLevelChunkPacketData pData) {
        this.level.getChunkSource().replaceWithPacketData(pX, pZ, pData.getReadBuffer(), pData.getHeightmaps(), pData.getBlockEntitiesTagsConsumer(pX, pZ));
    }

    private void enableChunkLight(LevelChunk pChunk, int pX, int pZ) {
        LevelLightEngine levellightengine = this.level.getChunkSource().getLightEngine();
        LevelChunkSection[] alevelchunksection = pChunk.getSections();
        ChunkPos chunkpos = pChunk.getPos();

        for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            int j = this.level.getSectionYFromSectionIndex(i);
            levellightengine.updateSectionStatus(SectionPos.of(chunkpos, j), levelchunksection.hasOnlyAir());
        }

        this.level.setSectionRangeDirty(pX - 1, this.level.getMinSectionY(), pZ - 1, pX + 1, this.level.getMaxSectionY(), pZ + 1);
    }

    @Override
    public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getChunkSource().drop(pPacket.pos());
        this.queueLightRemoval(pPacket);
    }

    private void queueLightRemoval(ClientboundForgetLevelChunkPacket pPacket) {
        ChunkPos chunkpos = pPacket.pos();
        this.level.queueLightUpdate(() -> {
            LevelLightEngine levellightengine = this.level.getLightEngine();
            levellightengine.setLightEnabled(chunkpos, false);

            for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
                SectionPos sectionpos = SectionPos.of(chunkpos, i);
                levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, null);
                levellightengine.queueSectionData(LightLayer.SKY, sectionpos, null);
            }

            for (int j = this.level.getMinSectionY(); j <= this.level.getMaxSectionY(); j++) {
                levellightengine.updateSectionStatus(SectionPos.of(chunkpos, j), true);
            }
        });
    }

    @Override
    public void handleBlockUpdate(ClientboundBlockUpdatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.setServerVerifiedBlockState(pPacket.getPos(), pPacket.getBlockState(), 19);
    }

    @Override
    public void handleConfigurationStart(ClientboundStartConfigurationPacket p_298839_) {
        PacketUtils.ensureRunningOnSameThread(p_298839_, this, this.minecraft);
        this.minecraft.getChatListener().clearQueue();
        this.sendChatAcknowledgement();
        ChatComponent.State chatcomponent$state = this.minecraft.gui.getChat().storeState();
        this.minecraft.clearClientLevel(new ServerReconfigScreen(RECONFIGURE_SCREEN_MESSAGE, this.connection));
        this.connection
            .setupInboundProtocol(
                ConfigurationProtocols.CLIENTBOUND,
                new ClientConfigurationPacketListenerImpl(
                    this.minecraft,
                    this.connection,
                    new CommonListenerCookie(
                        this.localGameProfile,
                        this.telemetryManager,
                        this.registryAccess,
                        this.enabledFeatures,
                        this.serverBrand,
                        this.serverData,
                        this.postDisconnectScreen,
                        this.serverCookies,
                        chatcomponent$state,
                        this.customReportDetails,
                        this.serverLinks()
                    )
                )
            );
        this.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
    }

    @Override
    public void handleTakeItemEntity(ClientboundTakeItemEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getItemId());
        LivingEntity livingentity = (LivingEntity)this.level.getEntity(pPacket.getPlayerId());
        if (livingentity == null) {
            livingentity = this.minecraft.player;
        }

        if (entity != null) {
            if (entity instanceof ExperienceOrb) {
                this.level
                    .playLocalSound(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.1F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F,
                        false
                    );
            } else {
                this.level
                    .playLocalSound(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        0.2F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F,
                        false
                    );
            }

            this.minecraft.particleEngine.add(new ItemPickupParticle(this.minecraft.getEntityRenderDispatcher(), this.level, entity, livingentity));
            if (entity instanceof ItemEntity itementity) {
                ItemStack itemstack = itementity.getItem();
                if (!itemstack.isEmpty()) {
                    itemstack.shrink(pPacket.getAmount());
                }

                if (itemstack.isEmpty()) {
                    this.level.removeEntity(pPacket.getItemId(), Entity.RemovalReason.DISCARDED);
                }
            } else if (!(entity instanceof ExperienceOrb)) {
                this.level.removeEntity(pPacket.getItemId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void handleSystemChat(ClientboundSystemChatPacket p_233708_) {
        PacketUtils.ensureRunningOnSameThread(p_233708_, this, this.minecraft);
        this.minecraft.getChatListener().handleSystemMessage(p_233708_.content(), p_233708_.overlay());
    }

    @Override
    public void handlePlayerChat(ClientboundPlayerChatPacket p_233702_) {
        PacketUtils.ensureRunningOnSameThread(p_233702_, this, this.minecraft);
        int i = this.nextChatIndex++;
        if (p_233702_.globalIndex() != i) {
            LOGGER.error("Missing or out-of-order chat message from server, expected index {} but got {}", i, p_233702_.globalIndex());
            this.connection.disconnect(BAD_CHAT_INDEX);
        } else {
            Optional<SignedMessageBody> optional = p_233702_.body().unpack(this.messageSignatureCache);
            if (optional.isEmpty()) {
                LOGGER.error("Message from player with ID {} referenced unrecognized signature id", p_233702_.sender());
                this.connection.disconnect(INVALID_PACKET);
            } else {
                this.messageSignatureCache.push(optional.get(), p_233702_.signature());
                UUID uuid = p_233702_.sender();
                PlayerInfo playerinfo = this.getPlayerInfo(uuid);
                if (playerinfo == null) {
                    LOGGER.error("Received player chat packet for unknown player with ID: {}", uuid);
                    this.minecraft.getChatListener().handleChatMessageError(uuid, p_233702_.signature(), p_233702_.chatType());
                } else {
                    RemoteChatSession remotechatsession = playerinfo.getChatSession();
                    SignedMessageLink signedmessagelink;
                    if (remotechatsession != null) {
                        signedmessagelink = new SignedMessageLink(p_233702_.index(), uuid, remotechatsession.sessionId());
                    } else {
                        signedmessagelink = SignedMessageLink.unsigned(uuid);
                    }

                    PlayerChatMessage playerchatmessage = new PlayerChatMessage(
                        signedmessagelink, p_233702_.signature(), optional.get(), p_233702_.unsignedContent(), p_233702_.filterMask()
                    );
                    playerchatmessage = playerinfo.getMessageValidator().updateAndValidate(playerchatmessage);
                    if (playerchatmessage != null) {
                        this.minecraft.getChatListener().handlePlayerChatMessage(playerchatmessage, playerinfo.getProfile(), p_233702_.chatType());
                    } else {
                        this.minecraft.getChatListener().handleChatMessageError(uuid, p_233702_.signature(), p_233702_.chatType());
                    }
                }
            }
        }
    }

    @Override
    public void handleDisguisedChat(ClientboundDisguisedChatPacket p_251920_) {
        PacketUtils.ensureRunningOnSameThread(p_251920_, this, this.minecraft);
        this.minecraft.getChatListener().handleDisguisedChatMessage(p_251920_.message(), p_251920_.chatType());
    }

    @Override
    public void handleDeleteChat(ClientboundDeleteChatPacket p_241325_) {
        PacketUtils.ensureRunningOnSameThread(p_241325_, this, this.minecraft);
        Optional<MessageSignature> optional = p_241325_.messageSignature().unpack(this.messageSignatureCache);
        if (optional.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
        } else {
            this.lastSeenMessages.ignorePending(optional.get());
            if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue(optional.get())) {
                this.minecraft.gui.getChat().deleteMessage(optional.get());
            }
        }
    }

    @Override
    public void handleAnimate(ClientboundAnimatePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            if (pPacket.getAction() == 0) {
                LivingEntity livingentity = (LivingEntity)entity;
                livingentity.swing(InteractionHand.MAIN_HAND);
            } else if (pPacket.getAction() == 3) {
                LivingEntity livingentity1 = (LivingEntity)entity;
                livingentity1.swing(InteractionHand.OFF_HAND);
            } else if (pPacket.getAction() == 2) {
                Player player = (Player)entity;
                player.stopSleepInBed(false, false);
            } else if (pPacket.getAction() == 4) {
                this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
            } else if (pPacket.getAction() == 5) {
                this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
            }
        }
    }

    @Override
    public void handleHurtAnimation(ClientboundHurtAnimationPacket p_265581_) {
        PacketUtils.ensureRunningOnSameThread(p_265581_, this, this.minecraft);
        Entity entity = this.level.getEntity(p_265581_.id());
        if (entity != null) {
            entity.animateHurt(p_265581_.yaw());
        }
    }

    @Override
    public void handleSetTime(ClientboundSetTimePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.setTimeFromServer(pPacket.gameTime(), pPacket.dayTime(), pPacket.tickDayTime());
        this.telemetryManager.setTime(pPacket.gameTime());
    }

    @Override
    public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket p_105084_) {
        PacketUtils.ensureRunningOnSameThread(p_105084_, this, this.minecraft);
        this.minecraft.level.setDefaultSpawnPos(p_105084_.getPos(), p_105084_.getAngle());
    }

    @Override
    public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getVehicle());
        if (entity == null) {
            LOGGER.warn("Received passengers for unknown entity");
        } else {
            boolean flag = entity.hasIndirectPassenger(this.minecraft.player);
            entity.ejectPassengers();

            for (int i : pPacket.getPassengers()) {
                Entity entity1 = this.level.getEntity(i);
                if (entity1 != null) {
                    entity1.startRiding(entity, true);
                    if (entity1 == this.minecraft.player) {
                        this.removedPlayerVehicleId = OptionalInt.empty();
                        if (!flag) {
                            if (entity instanceof AbstractBoat) {
                                this.minecraft.player.yRotO = entity.getYRot();
                                this.minecraft.player.setYRot(entity.getYRot());
                                this.minecraft.player.setYHeadRot(entity.getYRot());
                            }

                            Component component = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
                            this.minecraft.gui.setOverlayMessage(component, false);
                            this.minecraft.getNarrator().saySystemNow(component);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getSourceId()) instanceof Leashable leashable) {
            leashable.setDelayedLeashHolderId(pPacket.getDestId());
        }
    }

    private static ItemStack findTotem(Player pPlayer) {
        for (InteractionHand interactionhand : InteractionHand.values()) {
            ItemStack itemstack = pPlayer.getItemInHand(interactionhand);
            if (itemstack.has(DataComponents.DEATH_PROTECTION)) {
                return itemstack;
            }
        }

        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    @Override
    public void handleEntityEvent(ClientboundEntityEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            switch (pPacket.getEventId()) {
                case 21:
                    this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
                    break;
                case 35:
                    int i = 40;
                    this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
                    this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0F, 1.0F, false);
                    if (entity == this.minecraft.player) {
                        this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
                    }
                    break;
                case 63:
                    this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
                    break;
                default:
                    entity.handleEntityEvent(pPacket.getEventId());
            }
        }
    }

    @Override
    public void handleDamageEvent(ClientboundDamageEventPacket p_270800_) {
        PacketUtils.ensureRunningOnSameThread(p_270800_, this, this.minecraft);
        Entity entity = this.level.getEntity(p_270800_.entityId());
        if (entity != null) {
            entity.handleDamageEvent(p_270800_.getSource(this.level));
        }
    }

    @Override
    public void handleSetHealth(ClientboundSetHealthPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.player.hurtTo(pPacket.getHealth());
        this.minecraft.player.getFoodData().setFoodLevel(pPacket.getFood());
        this.minecraft.player.getFoodData().setSaturation(pPacket.getSaturation());
    }

    @Override
    public void handleSetExperience(ClientboundSetExperiencePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.player.setExperienceValues(pPacket.getExperienceProgress(), pPacket.getTotalExperience(), pPacket.getExperienceLevel());
    }

    @Override
    public void handleRespawn(ClientboundRespawnPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        CommonPlayerSpawnInfo commonplayerspawninfo = pPacket.commonPlayerSpawnInfo();
        ResourceKey<Level> resourcekey = commonplayerspawninfo.dimension();
        Holder<DimensionType> holder = commonplayerspawninfo.dimensionType();
        LocalPlayer localplayer = this.minecraft.player;
        ResourceKey<Level> resourcekey1 = localplayer.level().dimension();
        boolean flag = resourcekey != resourcekey1;
        ReceivingLevelScreen.Reason receivinglevelscreen$reason = this.determineLevelLoadingReason(localplayer.isDeadOrDying(), resourcekey, resourcekey1);
        if (flag) {
            Map<MapId, MapItemSavedData> map = this.level.getAllMapData();
            boolean flag1 = commonplayerspawninfo.isDebug();
            boolean flag2 = commonplayerspawninfo.isFlat();
            int i = commonplayerspawninfo.seaLevel();
            ClientLevel.ClientLevelData clientlevel$clientleveldata = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), flag2);
            this.levelData = clientlevel$clientleveldata;
            this.level = new ClientLevel(
                this,
                clientlevel$clientleveldata,
                resourcekey,
                holder,
                this.serverChunkRadius,
                this.serverSimulationDistance,
                this.minecraft.levelRenderer,
                flag1,
                commonplayerspawninfo.seed(),
                i
            );
            this.level.addMapData(map);
            this.minecraft.setLevel(this.level, receivinglevelscreen$reason);
        }

        this.minecraft.cameraEntity = null;
        if (localplayer.hasContainerOpen()) {
            localplayer.closeContainer();
        }

        LocalPlayer localplayer1;
        if (pPacket.shouldKeep((byte)2)) {
            localplayer1 = this.minecraft
                .gameMode
                .createPlayer(this.level, localplayer.getStats(), localplayer.getRecipeBook(), localplayer.getLastSentInput(), localplayer.isSprinting());
        } else {
            localplayer1 = this.minecraft.gameMode.createPlayer(this.level, localplayer.getStats(), localplayer.getRecipeBook());
        }

        this.startWaitingForNewLevel(localplayer1, this.level, receivinglevelscreen$reason);
        localplayer1.setId(localplayer.getId());
        this.minecraft.player = localplayer1;
        if (flag) {
            this.minecraft.getMusicManager().stopPlaying();
        }

        this.minecraft.cameraEntity = localplayer1;
        if (pPacket.shouldKeep((byte)2)) {
            List<SynchedEntityData.DataValue<?>> list = localplayer.getEntityData().getNonDefaultValues();
            if (list != null) {
                localplayer1.getEntityData().assignValues(list);
            }

            localplayer1.setDeltaMovement(localplayer.getDeltaMovement());
            localplayer1.setYRot(localplayer.getYRot());
            localplayer1.setXRot(localplayer.getXRot());
        } else {
            localplayer1.resetPos();
            localplayer1.setYRot(-180.0F);
        }

        if (pPacket.shouldKeep((byte)1)) {
            localplayer1.getAttributes().assignAllValues(localplayer.getAttributes());
        } else {
            localplayer1.getAttributes().assignBaseValues(localplayer.getAttributes());
        }

        localplayer1.updateSyncFields(localplayer); // Forge: fix MC-10657
        net.minecraftforge.client.event.ForgeEventFactoryClient.firePlayerRespawn(this.minecraft.gameMode, localplayer, localplayer1, localplayer1.connection.connection);
        this.level.addEntity(localplayer1);
        localplayer1.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(localplayer1);
        localplayer1.setReducedDebugInfo(localplayer.isReducedDebugInfo());
        localplayer1.setShowDeathScreen(localplayer.shouldShowDeathScreen());
        localplayer1.setLastDeathLocation(commonplayerspawninfo.lastDeathLocation());
        localplayer1.setPortalCooldown(commonplayerspawninfo.portalCooldown());
        localplayer1.portalEffectIntensity = localplayer.portalEffectIntensity;
        localplayer1.oPortalEffectIntensity = localplayer.oPortalEffectIntensity;
        if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
            this.minecraft.setScreen(null);
        }

        this.minecraft.gameMode.setLocalMode(commonplayerspawninfo.gameType(), commonplayerspawninfo.previousGameType());
    }

    private ReceivingLevelScreen.Reason determineLevelLoadingReason(boolean pDying, ResourceKey<Level> pSpawnDimension, ResourceKey<Level> pCurrentDimension) {
        ReceivingLevelScreen.Reason receivinglevelscreen$reason = ReceivingLevelScreen.Reason.OTHER;
        if (!pDying) {
            if (pSpawnDimension == Level.NETHER || pCurrentDimension == Level.NETHER) {
                receivinglevelscreen$reason = ReceivingLevelScreen.Reason.NETHER_PORTAL;
            } else if (pSpawnDimension == Level.END || pCurrentDimension == Level.END) {
                receivinglevelscreen$reason = ReceivingLevelScreen.Reason.END_PORTAL;
            }
        }

        return receivinglevelscreen$reason;
    }

    @Override
    public void handleExplosion(ClientboundExplodePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Vec3 vec3 = pPacket.center();
        this.minecraft
            .level
            .playLocalSound(
                vec3.x(),
                vec3.y(),
                vec3.z(),
                pPacket.explosionSound().value(),
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (this.minecraft.level.random.nextFloat() - this.minecraft.level.random.nextFloat()) * 0.2F) * 0.7F,
                false
            );
        this.minecraft.level.addParticle(pPacket.explosionParticle(), vec3.x(), vec3.y(), vec3.z(), 1.0, 0.0, 0.0);
        pPacket.playerKnockback().ifPresent(this.minecraft.player::addDeltaMovement);
    }

    @Override
    public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getEntityId()) instanceof AbstractHorse abstracthorse) {
            LocalPlayer localplayer = this.minecraft.player;
            int i = pPacket.getInventoryColumns();
            SimpleContainer simplecontainer = new SimpleContainer(AbstractHorse.getInventorySize(i));
            HorseInventoryMenu horseinventorymenu = new HorseInventoryMenu(pPacket.getContainerId(), localplayer.getInventory(), simplecontainer, abstracthorse, i);
            localplayer.containerMenu = horseinventorymenu;
            this.minecraft.setScreen(new HorseInventoryScreen(horseinventorymenu, localplayer.getInventory(), abstracthorse, i));
        }
    }

    @Override
    public void handleOpenScreen(ClientboundOpenScreenPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        MenuScreens.create(pPacket.getType(), this.minecraft, pPacket.getContainerId(), pPacket.getTitle());
    }

    @Override
    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        ItemStack itemstack = pPacket.getItem();
        int i = pPacket.getSlot();
        this.minecraft.getTutorial().onGetItem(itemstack);
        boolean flag;
        if (this.minecraft.screen instanceof CreativeModeInventoryScreen creativemodeinventoryscreen) {
            flag = !creativemodeinventoryscreen.isInventoryOpen();
        } else {
            flag = false;
        }

        if (pPacket.getContainerId() == 0) {
            if (InventoryMenu.isHotbarSlot(i) && !itemstack.isEmpty()) {
                ItemStack itemstack1 = player.inventoryMenu.getSlot(i).getItem();
                if (itemstack1.isEmpty() || itemstack1.getCount() < itemstack.getCount()) {
                    itemstack.setPopTime(5);
                }
            }

            player.inventoryMenu.setItem(i, pPacket.getStateId(), itemstack);
        } else if (pPacket.getContainerId() == player.containerMenu.containerId && (pPacket.getContainerId() != 0 || !flag)) {
            player.containerMenu.setItem(i, pPacket.getStateId(), itemstack);
        }

        if (this.minecraft.screen instanceof CreativeModeInventoryScreen) {
            player.inventoryMenu.setRemoteSlot(i, itemstack);
            player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public void handleSetCursorItem(ClientboundSetCursorItemPacket p_369171_) {
        PacketUtils.ensureRunningOnSameThread(p_369171_, this, this.minecraft);
        this.minecraft.getTutorial().onGetItem(p_369171_.contents());
        if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
            this.minecraft.player.containerMenu.setCarried(p_369171_.contents());
        }
    }

    @Override
    public void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket p_368912_) {
        PacketUtils.ensureRunningOnSameThread(p_368912_, this, this.minecraft);
        this.minecraft.getTutorial().onGetItem(p_368912_.contents());
        this.minecraft.player.getInventory().setItem(p_368912_.slot(), p_368912_.contents());
    }

    @Override
    public void handleContainerContent(ClientboundContainerSetContentPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        if (pPacket.containerId() == 0) {
            player.inventoryMenu.initializeContents(pPacket.stateId(), pPacket.items(), pPacket.carriedItem());
        } else if (pPacket.containerId() == player.containerMenu.containerId) {
            player.containerMenu.initializeContents(pPacket.stateId(), pPacket.items(), pPacket.carriedItem());
        }
    }

    @Override
    public void handleOpenSignEditor(ClientboundOpenSignEditorPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        BlockPos blockpos = pPacket.getPos();
        if (this.level.getBlockEntity(blockpos) instanceof SignBlockEntity signblockentity) {
            this.minecraft.player.openTextEdit(signblockentity, pPacket.isFrontText());
        } else {
            LOGGER.warn("Ignoring openTextEdit on an invalid entity: {} at pos {}", this.level.getBlockEntity(blockpos), blockpos);
        }
    }

    @Override
    public void handleBlockEntityData(ClientboundBlockEntityDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        BlockPos blockpos = pPacket.getPos();
        this.minecraft.level.getBlockEntity(blockpos, pPacket.getType()).ifPresent(p_404890_ -> {
            ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_404890_.problemPath(), LOGGER);

            try {
                p_404890_.onDataPacket(connection, TagValueInput.create(problemreporter$scopedcollector, this.registryAccess, pPacket.getTag()), this.registryAccess);
            } catch (Throwable throwable1) {
                try {
                    problemreporter$scopedcollector.close();
                } catch (Throwable throwable) {
                    throwable1.addSuppressed(throwable);
                }

                throw throwable1;
            }

            problemreporter$scopedcollector.close();
            if (p_404890_ instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
                ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
            }
        });
    }

    @Override
    public void handleContainerSetData(ClientboundContainerSetDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        if (player.containerMenu != null && player.containerMenu.containerId == pPacket.getContainerId()) {
            player.containerMenu.setData(pPacket.getId(), pPacket.getValue());
        }
    }

    @Override
    public void handleSetEquipment(ClientboundSetEquipmentPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (this.level.getEntity(pPacket.getEntity()) instanceof LivingEntity livingentity) {
            pPacket.getSlots().forEach(p_325480_ -> livingentity.setItemSlot(p_325480_.getFirst(), p_325480_.getSecond()));
        }
    }

    @Override
    public void handleContainerClose(ClientboundContainerClosePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.player.clientSideCloseContainer();
    }

    @Override
    public void handleBlockEvent(ClientboundBlockEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.level.blockEvent(pPacket.getPos(), pPacket.getBlock(), pPacket.getB0(), pPacket.getB1());
    }

    @Override
    public void handleBlockDestruction(ClientboundBlockDestructionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.level.destroyBlockProgress(pPacket.getId(), pPacket.getPos(), pPacket.getProgress());
    }

    @Override
    public void handleGameEvent(ClientboundGameEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        ClientboundGameEventPacket.Type clientboundgameeventpacket$type = pPacket.getEvent();
        float f = pPacket.getParam();
        int i = Mth.floor(f + 0.5F);
        if (clientboundgameeventpacket$type == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
            player.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.START_RAINING) {
            this.level.getLevelData().setRaining(true);
            this.level.setRainLevel(0.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.STOP_RAINING) {
            this.level.getLevelData().setRaining(false);
            this.level.setRainLevel(1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
            this.minecraft.gameMode.setLocalMode(GameType.byId(i));
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.WIN_GAME) {
            this.minecraft.setScreen(new WinScreen(true, () -> {
                this.minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                this.minecraft.setScreen(null);
            }));
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.DEMO_EVENT) {
            Options options = this.minecraft.options;
            Component component = null;
            if (f == 0.0F) {
                this.minecraft.setScreen(new DemoIntroScreen());
            } else if (f == 101.0F) {
                component = Component.translatable(
                    "demo.help.movement", options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage()
                );
            } else if (f == 102.0F) {
                component = Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage());
            } else if (f == 103.0F) {
                component = Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage());
            } else if (f == 104.0F) {
                component = Component.translatable("demo.day.6", options.keyScreenshot.getTranslatedKeyMessage());
            }

            if (component != null) {
                this.minecraft.gui.getChat().addMessage(component);
                this.minecraft.getNarrator().saySystemQueued(component);
            }
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND) {
            this.level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            this.level.setRainLevel(f);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            this.level.setThunderLevel(f);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.PUFFER_FISH_STING) {
            this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
            this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
            if (i == 1) {
                this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
            this.minecraft.player.setShowDeathScreen(f == 0.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.LIMITED_CRAFTING) {
            this.minecraft.player.setDoLimitedCrafting(f == 1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START && this.levelLoadStatusManager != null) {
            this.levelLoadStatusManager.loadingPacketsReceived();
        }
    }

    private void startWaitingForNewLevel(LocalPlayer pPlayer, ClientLevel pLevel, ReceivingLevelScreen.Reason pReason) {
        this.levelLoadStatusManager = new LevelLoadStatusManager(pPlayer, pLevel, this.minecraft.levelRenderer);
        this.minecraft.setScreen(new ReceivingLevelScreen(this.levelLoadStatusManager::levelReady, pReason));
    }

    @Override
    public void handleMapItemData(ClientboundMapItemDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        MapId mapid = pPacket.mapId();
        MapItemSavedData mapitemsaveddata = this.minecraft.level.getMapData(mapid);
        if (mapitemsaveddata == null) {
            mapitemsaveddata = MapItemSavedData.createForClient(pPacket.scale(), pPacket.locked(), this.minecraft.level.dimension());
            this.minecraft.level.overrideMapData(mapid, mapitemsaveddata);
        }

        pPacket.applyToMap(mapitemsaveddata);
        this.minecraft.getMapTextureManager().update(mapid, mapitemsaveddata);
    }

    @Override
    public void handleLevelEvent(ClientboundLevelEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.isGlobalEvent()) {
            this.minecraft.level.globalLevelEvent(pPacket.getType(), pPacket.getPos(), pPacket.getData());
        } else {
            this.minecraft.level.levelEvent(pPacket.getType(), pPacket.getPos(), pPacket.getData());
        }
    }

    @Override
    public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.advancements.update(pPacket);
    }

    @Override
    public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ResourceLocation resourcelocation = pPacket.getTab();
        if (resourcelocation == null) {
            this.advancements.setSelectedTab(null, false);
        } else {
            AdvancementHolder advancementholder = this.advancements.get(resourcelocation);
            this.advancements.setSelectedTab(advancementholder, false);
        }
    }

    @Override
    public void handleCommands(ClientboundCommandsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        var context = CommandBuildContext.simple(this.registryAccess, this.enabledFeatures);
        this.commands = new CommandDispatcher<>(pPacket.getRoot(context, COMMAND_NODE_BUILDER));
        this.commands = net.minecraftforge.client.ClientCommandHandler.mergeServerCommands(this.commands, context);
    }

    @Override
    public void handleStopSoundEvent(ClientboundStopSoundPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.getSoundManager().stop(pPacket.getName(), pPacket.getSource());
    }

    @Override
    public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.suggestionsProvider.completeCustomSuggestions(pPacket.id(), pPacket.toSuggestions());
    }

    @Override
    public void handleUpdateRecipes(ClientboundUpdateRecipesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.recipes = new ClientRecipeContainer(pPacket.itemSets(), pPacket.stonecutterRecipes());
    }

    @Override
    public void handleLookAt(ClientboundPlayerLookAtPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Vec3 vec3 = pPacket.getPosition(this.level);
        if (vec3 != null) {
            this.minecraft.player.lookAt(pPacket.getFromAnchor(), vec3);
        }
    }

    @Override
    public void handleTagQueryPacket(ClientboundTagQueryPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (!this.debugQueryHandler.handleResponse(pPacket.getTransactionId(), pPacket.getTag())) {
            LOGGER.debug("Got unhandled response to tag query {}", pPacket.getTransactionId());
        }
    }

    @Override
    public void handleAwardStats(ClientboundAwardStatsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        for (Entry<Stat<?>> entry : pPacket.stats().object2IntEntrySet()) {
            Stat<?> stat = entry.getKey();
            int i = entry.getIntValue();
            this.minecraft.player.getStats().setValue(this.minecraft.player, stat, i);
        }

        if (this.minecraft.screen instanceof StatsScreen statsscreen) {
            statsscreen.onStatsUpdated();
        }
    }

    @Override
    public void handleRecipeBookAdd(ClientboundRecipeBookAddPacket p_365432_) {
        PacketUtils.ensureRunningOnSameThread(p_365432_, this, this.minecraft);
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();
        if (p_365432_.replace()) {
            clientrecipebook.clear();
        }

        for (ClientboundRecipeBookAddPacket.Entry clientboundrecipebookaddpacket$entry : p_365432_.entries()) {
            clientrecipebook.add(clientboundrecipebookaddpacket$entry.contents());
            if (clientboundrecipebookaddpacket$entry.highlight()) {
                clientrecipebook.addHighlight(clientboundrecipebookaddpacket$entry.contents().id());
            }

            if (clientboundrecipebookaddpacket$entry.notification()) {
                RecipeToast.addOrUpdate(this.minecraft.getToastManager(), clientboundrecipebookaddpacket$entry.contents().display());
            }
        }

        this.refreshRecipeBook(clientrecipebook);
    }

    @Override
    public void handleRecipeBookRemove(ClientboundRecipeBookRemovePacket p_364792_) {
        PacketUtils.ensureRunningOnSameThread(p_364792_, this, this.minecraft);
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();

        for (RecipeDisplayId recipedisplayid : p_364792_.recipes()) {
            clientrecipebook.remove(recipedisplayid);
        }

        this.refreshRecipeBook(clientrecipebook);
    }

    @Override
    public void handleRecipeBookSettings(ClientboundRecipeBookSettingsPacket p_365706_) {
        PacketUtils.ensureRunningOnSameThread(p_365706_, this, this.minecraft);
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();
        clientrecipebook.setBookSettings(p_365706_.bookSettings());
        this.refreshRecipeBook(clientrecipebook);
    }

    private void refreshRecipeBook(ClientRecipeBook pRecipeBook) {
        pRecipeBook.rebuildCollections();
        this.searchTrees.updateRecipes(pRecipeBook, this.level);
        if (this.minecraft.screen instanceof RecipeUpdateListener recipeupdatelistener) {
            recipeupdatelistener.recipesUpdated();
        }
        net.minecraftforge.client.event.ForgeEventFactoryClient.onRecipesUpdated(pRecipeBook);
    }

    @Override
    public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getEntityId());
        if (entity instanceof LivingEntity) {
            Holder<MobEffect> holder = pPacket.getEffect();
            MobEffectInstance mobeffectinstance = new MobEffectInstance(
                holder, pPacket.getEffectDurationTicks(), pPacket.getEffectAmplifier(), pPacket.isEffectAmbient(), pPacket.isEffectVisible(), pPacket.effectShowsIcon(), null
            );
            if (!pPacket.shouldBlend()) {
                mobeffectinstance.skipBlending();
            }

            ((LivingEntity)entity).forceAddEffect(mobeffectinstance, null);
        }
    }

    private <T> Registry.PendingTags<T> updateTags(ResourceKey<? extends Registry<? extends T>> pRegistryKey, TagNetworkSerialization.NetworkPayload pPayload) {
        Registry<T> registry = this.registryAccess.lookupOrThrow(pRegistryKey);
        return registry.prepareTagReload(pPayload.resolve(registry));
    }

    @Override
    public void handleUpdateTags(ClientboundUpdateTagsPacket p_298004_) {
        PacketUtils.ensureRunningOnSameThread(p_298004_, this, this.minecraft);
        List<Registry.PendingTags<?>> list = new ArrayList<>(p_298004_.getTags().size());
        boolean flag = this.connection.isMemoryConnection();
        p_298004_.getTags().forEach((p_357782_, p_357783_) -> {
            if (!flag || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_357782_)) {
                list.add(this.updateTags((ResourceKey<? extends Registry<?>>)p_357782_, p_357783_));
            }
        });
        list.forEach(Registry.PendingTags::apply);
        this.fuelValues = FuelValues.vanillaBurnTimes(this.registryAccess, this.enabledFeatures);
        List<ItemStack> list1 = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
        this.searchTrees.updateCreativeTags(list1);

        net.minecraftforge.event.ForgeEventFactory.onTagsUpdated(this.registryAccess, true, this.connection.isMemoryConnection());
    }

    @Override
    public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket p_171771_) {
    }

    @Override
    public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket p_171773_) {
    }

    @Override
    public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket p_171775_) {
        PacketUtils.ensureRunningOnSameThread(p_171775_, this, this.minecraft);
        Entity entity = this.level.getEntity(p_171775_.playerId());
        if (entity == this.minecraft.player) {
            if (this.minecraft.player.shouldShowDeathScreen()) {
                this.minecraft.setScreen(new DeathScreen(p_171775_.message(), this.level.getLevelData().isHardcore()));
            } else {
                this.minecraft.player.respawn();
            }
        }
    }

    @Override
    public void handleChangeDifficulty(ClientboundChangeDifficultyPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.levelData.setDifficulty(pPacket.difficulty());
        this.levelData.setDifficultyLocked(pPacket.locked());
    }

    @Override
    public void handleSetCamera(ClientboundSetCameraPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity != null) {
            this.minecraft.setCameraEntity(entity);
        }
    }

    @Override
    public void handleInitializeBorder(ClientboundInitializeBorderPacket p_171767_) {
        PacketUtils.ensureRunningOnSameThread(p_171767_, this, this.minecraft);
        WorldBorder worldborder = this.level.getWorldBorder();
        worldborder.setCenter(p_171767_.getNewCenterX(), p_171767_.getNewCenterZ());
        long i = p_171767_.getLerpTime();
        if (i > 0L) {
            worldborder.lerpSizeBetween(p_171767_.getOldSize(), p_171767_.getNewSize(), i);
        } else {
            worldborder.setSize(p_171767_.getNewSize());
        }

        worldborder.setAbsoluteMaxSize(p_171767_.getNewAbsoluteMaxSize());
        worldborder.setWarningBlocks(p_171767_.getWarningBlocks());
        worldborder.setWarningTime(p_171767_.getWarningTime());
    }

    @Override
    public void handleSetBorderCenter(ClientboundSetBorderCenterPacket p_171781_) {
        PacketUtils.ensureRunningOnSameThread(p_171781_, this, this.minecraft);
        this.level.getWorldBorder().setCenter(p_171781_.getNewCenterX(), p_171781_.getNewCenterZ());
    }

    @Override
    public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket p_171783_) {
        PacketUtils.ensureRunningOnSameThread(p_171783_, this, this.minecraft);
        this.level.getWorldBorder().lerpSizeBetween(p_171783_.getOldSize(), p_171783_.getNewSize(), p_171783_.getLerpTime());
    }

    @Override
    public void handleSetBorderSize(ClientboundSetBorderSizePacket p_171785_) {
        PacketUtils.ensureRunningOnSameThread(p_171785_, this, this.minecraft);
        this.level.getWorldBorder().setSize(p_171785_.getSize());
    }

    @Override
    public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket p_171789_) {
        PacketUtils.ensureRunningOnSameThread(p_171789_, this, this.minecraft);
        this.level.getWorldBorder().setWarningBlocks(p_171789_.getWarningBlocks());
    }

    @Override
    public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket p_171787_) {
        PacketUtils.ensureRunningOnSameThread(p_171787_, this, this.minecraft);
        this.level.getWorldBorder().setWarningTime(p_171787_.getWarningDelay());
    }

    @Override
    public void handleTitlesClear(ClientboundClearTitlesPacket p_171765_) {
        PacketUtils.ensureRunningOnSameThread(p_171765_, this, this.minecraft);
        this.minecraft.gui.clearTitles();
        if (p_171765_.shouldResetTimes()) {
            this.minecraft.gui.resetTitleTimes();
        }
    }

    @Override
    public void handleServerData(ClientboundServerDataPacket p_233704_) {
        PacketUtils.ensureRunningOnSameThread(p_233704_, this, this.minecraft);
        if (this.serverData != null) {
            this.serverData.motd = p_233704_.motd();
            p_233704_.iconBytes().map(ServerData::validateIcon).ifPresent(this.serverData::setIconBytes);
            ServerList.saveSingleServer(this.serverData);
        }
    }

    @Override
    public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket p_240832_) {
        PacketUtils.ensureRunningOnSameThread(p_240832_, this, this.minecraft);
        this.suggestionsProvider.modifyCustomCompletions(p_240832_.action(), p_240832_.entries());
    }

    @Override
    public void setActionBarText(ClientboundSetActionBarTextPacket p_171779_) {
        PacketUtils.ensureRunningOnSameThread(p_171779_, this, this.minecraft);
        this.minecraft.gui.setOverlayMessage(p_171779_.text(), false);
    }

    @Override
    public void setTitleText(ClientboundSetTitleTextPacket p_171793_) {
        PacketUtils.ensureRunningOnSameThread(p_171793_, this, this.minecraft);
        this.minecraft.gui.setTitle(p_171793_.text());
    }

    @Override
    public void setSubtitleText(ClientboundSetSubtitleTextPacket p_171791_) {
        PacketUtils.ensureRunningOnSameThread(p_171791_, this, this.minecraft);
        this.minecraft.gui.setSubtitle(p_171791_.text());
    }

    @Override
    public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket p_171795_) {
        PacketUtils.ensureRunningOnSameThread(p_171795_, this, this.minecraft);
        this.minecraft.gui.setTimes(p_171795_.getFadeIn(), p_171795_.getStay(), p_171795_.getFadeOut());
    }

    @Override
    public void handleTabListCustomisation(ClientboundTabListPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.getTabList().setHeader(pPacket.header().getString().isEmpty() ? null : pPacket.header());
        this.minecraft.gui.getTabList().setFooter(pPacket.footer().getString().isEmpty() ? null : pPacket.footer());
    }

    @Override
    public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.getEntity(this.level) instanceof LivingEntity livingentity) {
            livingentity.removeEffectNoUpdate(pPacket.effect());
        }
    }

    @Override
    public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket p_248731_) {
        PacketUtils.ensureRunningOnSameThread(p_248731_, this, this.minecraft);

        for (UUID uuid : p_248731_.profileIds()) {
            this.minecraft.getPlayerSocialManager().removePlayer(uuid);
            PlayerInfo playerinfo = this.playerInfoMap.remove(uuid);
            if (playerinfo != null) {
                this.listedPlayers.remove(playerinfo);
            }
        }
    }

    @Override
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket p_250115_) {
        PacketUtils.ensureRunningOnSameThread(p_250115_, this, this.minecraft);

        for (ClientboundPlayerInfoUpdatePacket.Entry clientboundplayerinfoupdatepacket$entry : p_250115_.newEntries()) {
            PlayerInfo playerinfo = new PlayerInfo(Objects.requireNonNull(clientboundplayerinfoupdatepacket$entry.profile()), this.enforcesSecureChat());
            if (this.playerInfoMap.putIfAbsent(clientboundplayerinfoupdatepacket$entry.profileId(), playerinfo) == null) {
                this.minecraft.getPlayerSocialManager().addPlayer(playerinfo);
            }
        }

        for (ClientboundPlayerInfoUpdatePacket.Entry clientboundplayerinfoupdatepacket$entry1 : p_250115_.entries()) {
            PlayerInfo playerinfo1 = this.playerInfoMap.get(clientboundplayerinfoupdatepacket$entry1.profileId());
            if (playerinfo1 == null) {
                LOGGER.warn(
                    "Ignoring player info update for unknown player {} ({})", clientboundplayerinfoupdatepacket$entry1.profileId(), p_250115_.actions()
                );
            } else {
                for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : p_250115_.actions()) {
                    this.applyPlayerInfoUpdate(clientboundplayerinfoupdatepacket$action, clientboundplayerinfoupdatepacket$entry1, playerinfo1);
                }
            }
        }
    }

    private void applyPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.Action pAction, ClientboundPlayerInfoUpdatePacket.Entry pEntry, PlayerInfo pPlayerInfo) {
        switch (pAction) {
            case INITIALIZE_CHAT:
                this.initializeChatSession(pEntry, pPlayerInfo);
                break;
            case UPDATE_GAME_MODE:
                if (pPlayerInfo.getGameMode() != pEntry.gameMode()
                    && this.minecraft.player != null
                    && this.minecraft.player.getUUID().equals(pEntry.profileId())) {
                    this.minecraft.player.onGameModeChanged(pEntry.gameMode());
                }

                pPlayerInfo.setGameMode(pEntry.gameMode());
                break;
            case UPDATE_LISTED:
                if (pEntry.listed()) {
                    this.listedPlayers.add(pPlayerInfo);
                } else {
                    this.listedPlayers.remove(pPlayerInfo);
                }
                break;
            case UPDATE_LATENCY:
                pPlayerInfo.setLatency(pEntry.latency());
                break;
            case UPDATE_DISPLAY_NAME:
                pPlayerInfo.setTabListDisplayName(pEntry.displayName());
                break;
            case UPDATE_HAT:
                pPlayerInfo.setShowHat(pEntry.showHat());
                break;
            case UPDATE_LIST_ORDER:
                pPlayerInfo.setTabListOrder(pEntry.listOrder());
        }
    }

    private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry pEntry, PlayerInfo pPlayerInfo) {
        GameProfile gameprofile = pPlayerInfo.getProfile();
        SignatureValidator signaturevalidator = this.minecraft.getProfileKeySignatureValidator();
        if (signaturevalidator == null) {
            LOGGER.warn("Ignoring chat session from {} due to missing Services public key", gameprofile.getName());
            pPlayerInfo.clearChatSession(this.enforcesSecureChat());
        } else {
            RemoteChatSession.Data remotechatsession$data = pEntry.chatSession();
            if (remotechatsession$data != null) {
                try {
                    RemoteChatSession remotechatsession = remotechatsession$data.validate(gameprofile, signaturevalidator);
                    pPlayerInfo.setChatSession(remotechatsession);
                } catch (ProfilePublicKey.ValidationException profilepublickey$validationexception) {
                    LOGGER.error("Failed to validate profile key for player: '{}'", gameprofile.getName(), profilepublickey$validationexception);
                    pPlayerInfo.clearChatSession(this.enforcesSecureChat());
                }
            } else {
                pPlayerInfo.clearChatSession(this.enforcesSecureChat());
            }
        }
    }

    private boolean enforcesSecureChat() {
        return this.minecraft.canValidateProfileKeys() && this.serverEnforcesSecureChat;
    }

    @Override
    public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Player player = this.minecraft.player;
        player.getAbilities().flying = pPacket.isFlying();
        player.getAbilities().instabuild = pPacket.canInstabuild();
        player.getAbilities().invulnerable = pPacket.isInvulnerable();
        player.getAbilities().mayfly = pPacket.canFly();
        player.getAbilities().setFlyingSpeed(pPacket.getFlyingSpeed());
        player.getAbilities().setWalkingSpeed(pPacket.getWalkingSpeed());
    }

    @Override
    public void handleSoundEvent(ClientboundSoundPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft
            .level
            .playSeededSound(
                this.minecraft.player,
                pPacket.getX(),
                pPacket.getY(),
                pPacket.getZ(),
                pPacket.getSound(),
                pPacket.getSource(),
                pPacket.getVolume(),
                pPacket.getPitch(),
                pPacket.getSeed()
            );
    }

    @Override
    public void handleSoundEntityEvent(ClientboundSoundEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getId());
        if (entity != null) {
            this.minecraft
                .level
                .playSeededSound(
                    this.minecraft.player,
                    entity,
                    pPacket.getSound(),
                    pPacket.getSource(),
                    pPacket.getVolume(),
                    pPacket.getPitch(),
                    pPacket.getSeed()
                );
        }
    }

    @Override
    public void handleBossUpdate(ClientboundBossEventPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.minecraft.gui.getBossOverlay().update(pPacket);
    }

    @Override
    public void handleItemCooldown(ClientboundCooldownPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.duration() == 0) {
            this.minecraft.player.getCooldowns().removeCooldown(pPacket.cooldownGroup());
        } else {
            this.minecraft.player.getCooldowns().addCooldown(pPacket.cooldownGroup(), pPacket.duration());
        }
    }

    @Override
    public void handleMoveVehicle(ClientboundMoveVehiclePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.minecraft.player.getRootVehicle();
        if (entity != this.minecraft.player && entity.isLocalInstanceAuthoritative()) {
            Vec3 vec3 = pPacket.position();
            Vec3 vec31;
            if (entity.isInterpolating()) {
                vec31 = entity.getInterpolation().position();
            } else {
                vec31 = entity.position();
            }

            if (vec3.distanceTo(vec31) > 1.0E-5F) {
                if (entity.isInterpolating()) {
                    entity.getInterpolation().cancel();
                }

                entity.absSnapTo(vec3.x(), vec3.y(), vec3.z(), pPacket.yRot(), pPacket.xRot());
            }

            this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
        }
    }

    @Override
    public void handleOpenBook(ClientboundOpenBookPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ItemStack itemstack = this.minecraft.player.getItemInHand(pPacket.getHand());
        BookViewScreen.BookAccess bookviewscreen$bookaccess = BookViewScreen.BookAccess.fromItem(itemstack);
        if (bookviewscreen$bookaccess != null) {
            this.minecraft.setScreen(new BookViewScreen(bookviewscreen$bookaccess));
        }
    }

    @Override
    public void handleCustomPayload(CustomPacketPayload p_300286_) {
        if (p_300286_ instanceof PathfindingDebugPayload pathfindingdebugpayload) {
            this.minecraft
                .debugRenderer
                .pathfindingRenderer
                .addPath(pathfindingdebugpayload.entityId(), pathfindingdebugpayload.path(), pathfindingdebugpayload.maxNodeDistance());
        } else if (p_300286_ instanceof NeighborUpdatesDebugPayload neighborupdatesdebugpayload) {
            this.minecraft.debugRenderer.neighborsUpdateRenderer.addUpdate(neighborupdatesdebugpayload.time(), neighborupdatesdebugpayload.pos());
        } else if (p_300286_ instanceof RedstoneWireOrientationsDebugPayload redstonewireorientationsdebugpayload) {
            this.minecraft.debugRenderer.redstoneWireOrientationsRenderer.addWireOrientations(redstonewireorientationsdebugpayload);
        } else if (p_300286_ instanceof StructuresDebugPayload structuresdebugpayload) {
            this.minecraft
                .debugRenderer
                .structureRenderer
                .addBoundingBox(structuresdebugpayload.mainBB(), structuresdebugpayload.pieces(), structuresdebugpayload.dimension());
        } else if (p_300286_ instanceof WorldGenAttemptDebugPayload worldgenattemptdebugpayload) {
            ((WorldGenAttemptRenderer)this.minecraft.debugRenderer.worldGenAttemptRenderer)
                .addPos(
                    worldgenattemptdebugpayload.pos(),
                    worldgenattemptdebugpayload.scale(),
                    worldgenattemptdebugpayload.red(),
                    worldgenattemptdebugpayload.green(),
                    worldgenattemptdebugpayload.blue(),
                    worldgenattemptdebugpayload.alpha()
                );
        } else if (p_300286_ instanceof PoiTicketCountDebugPayload poiticketcountdebugpayload) {
            this.minecraft.debugRenderer.brainDebugRenderer.setFreeTicketCount(poiticketcountdebugpayload.pos(), poiticketcountdebugpayload.freeTicketCount());
        } else if (p_300286_ instanceof PoiAddedDebugPayload poiaddeddebugpayload) {
            BrainDebugRenderer.PoiInfo braindebugrenderer$poiinfo = new BrainDebugRenderer.PoiInfo(
                poiaddeddebugpayload.pos(), poiaddeddebugpayload.poiType(), poiaddeddebugpayload.freeTicketCount()
            );
            this.minecraft.debugRenderer.brainDebugRenderer.addPoi(braindebugrenderer$poiinfo);
        } else if (p_300286_ instanceof PoiRemovedDebugPayload poiremoveddebugpayload) {
            this.minecraft.debugRenderer.brainDebugRenderer.removePoi(poiremoveddebugpayload.pos());
        } else if (p_300286_ instanceof VillageSectionsDebugPayload villagesectionsdebugpayload) {
            VillageSectionsDebugRenderer villagesectionsdebugrenderer = this.minecraft.debugRenderer.villageSectionsDebugRenderer;
            villagesectionsdebugpayload.villageChunks().forEach(villagesectionsdebugrenderer::setVillageSection);
            villagesectionsdebugpayload.notVillageChunks().forEach(villagesectionsdebugrenderer::setNotVillageSection);
        } else if (p_300286_ instanceof GoalDebugPayload goaldebugpayload) {
            this.minecraft.debugRenderer.goalSelectorRenderer.addGoalSelector(goaldebugpayload.entityId(), goaldebugpayload.pos(), goaldebugpayload.goals());
        } else if (p_300286_ instanceof BrainDebugPayload braindebugpayload) {
            this.minecraft.debugRenderer.brainDebugRenderer.addOrUpdateBrainDump(braindebugpayload.brainDump());
        } else if (p_300286_ instanceof BeeDebugPayload beedebugpayload) {
            this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateBeeInfo(beedebugpayload.beeInfo());
        } else if (p_300286_ instanceof HiveDebugPayload hivedebugpayload) {
            this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateHiveInfo(hivedebugpayload.hiveInfo(), this.level.getGameTime());
        } else if (p_300286_ instanceof GameTestAddMarkerDebugPayload gametestaddmarkerdebugpayload) {
            this.minecraft
                .debugRenderer
                .gameTestDebugRenderer
                .addMarker(
                    gametestaddmarkerdebugpayload.pos(),
                    gametestaddmarkerdebugpayload.color(),
                    gametestaddmarkerdebugpayload.text(),
                    gametestaddmarkerdebugpayload.durationMs()
                );
        } else if (p_300286_ instanceof GameTestClearMarkersDebugPayload) {
            this.minecraft.debugRenderer.gameTestDebugRenderer.clear();
        } else if (p_300286_ instanceof RaidsDebugPayload raidsdebugpayload) {
            this.minecraft.debugRenderer.raidDebugRenderer.setRaidCenters(raidsdebugpayload.raidCenters());
        } else if (p_300286_ instanceof GameEventDebugPayload gameeventdebugpayload) {
            this.minecraft.debugRenderer.gameEventListenerRenderer.trackGameEvent(gameeventdebugpayload.gameEventType(), gameeventdebugpayload.pos());
        } else if (p_300286_ instanceof GameEventListenerDebugPayload gameeventlistenerdebugpayload) {
            this.minecraft.debugRenderer.gameEventListenerRenderer.trackListener(gameeventlistenerdebugpayload.listenerPos(), gameeventlistenerdebugpayload.listenerRange());
        } else if (p_300286_ instanceof BreezeDebugPayload breezedebugpayload) {
            this.minecraft.debugRenderer.breezeDebugRenderer.add(breezedebugpayload.breezeInfo());
        } else {
            this.handleUnknownCustomPayload(p_300286_);
        }
    }

    private void handleUnknownCustomPayload(CustomPacketPayload pPacket) {
        LOGGER.warn("Unknown custom packet payload: {}", pPacket.type().id());
    }

    @Override
    public void handleAddObjective(ClientboundSetObjectivePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.getObjectiveName();
        if (pPacket.getMethod() == 0) {
            this.scoreboard.addObjective(s, ObjectiveCriteria.DUMMY, pPacket.getDisplayName(), pPacket.getRenderType(), false, pPacket.getNumberFormat().orElse(null));
        } else {
            Objective objective = this.scoreboard.getObjective(s);
            if (objective != null) {
                if (pPacket.getMethod() == 1) {
                    this.scoreboard.removeObjective(objective);
                } else if (pPacket.getMethod() == 2) {
                    objective.setRenderType(pPacket.getRenderType());
                    objective.setDisplayName(pPacket.getDisplayName());
                    objective.setNumberFormat(pPacket.getNumberFormat().orElse(null));
                }
            }
        }
    }

    @Override
    public void handleSetScore(ClientboundSetScorePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.objectiveName();
        ScoreHolder scoreholder = ScoreHolder.forNameOnly(pPacket.owner());
        Objective objective = this.scoreboard.getObjective(s);
        if (objective != null) {
            ScoreAccess scoreaccess = this.scoreboard.getOrCreatePlayerScore(scoreholder, objective, true);
            scoreaccess.set(pPacket.score());
            scoreaccess.display(pPacket.display().orElse(null));
            scoreaccess.numberFormatOverride(pPacket.numberFormat().orElse(null));
        } else {
            LOGGER.warn("Received packet for unknown scoreboard objective: {}", s);
        }
    }

    @Override
    public void handleResetScore(ClientboundResetScorePacket p_312811_) {
        PacketUtils.ensureRunningOnSameThread(p_312811_, this, this.minecraft);
        String s = p_312811_.objectiveName();
        ScoreHolder scoreholder = ScoreHolder.forNameOnly(p_312811_.owner());
        if (s == null) {
            this.scoreboard.resetAllPlayerScores(scoreholder);
        } else {
            Objective objective = this.scoreboard.getObjective(s);
            if (objective != null) {
                this.scoreboard.resetSinglePlayerScore(scoreholder, objective);
            } else {
                LOGGER.warn("Received packet for unknown scoreboard objective: {}", s);
            }
        }
    }

    @Override
    public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        String s = pPacket.getObjectiveName();
        Objective objective = s == null ? null : this.scoreboard.getObjective(s);
        this.scoreboard.setDisplayObjective(pPacket.getSlot(), objective);
    }

    @Override
    public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        ClientboundSetPlayerTeamPacket.Action clientboundsetplayerteampacket$action = pPacket.getTeamAction();
        PlayerTeam playerteam;
        if (clientboundsetplayerteampacket$action == ClientboundSetPlayerTeamPacket.Action.ADD) {
            playerteam = this.scoreboard.addPlayerTeam(pPacket.getName());
        } else {
            playerteam = this.scoreboard.getPlayerTeam(pPacket.getName());
            if (playerteam == null) {
                LOGGER.warn(
                    "Received packet for unknown team {}: team action: {}, player action: {}",
                    pPacket.getName(),
                    pPacket.getTeamAction(),
                    pPacket.getPlayerAction()
                );
                return;
            }
        }

        Optional<ClientboundSetPlayerTeamPacket.Parameters> optional = pPacket.getParameters();
        optional.ifPresent(p_389337_ -> {
            playerteam.setDisplayName(p_389337_.getDisplayName());
            playerteam.setColor(p_389337_.getColor());
            playerteam.unpackOptions(p_389337_.getOptions());
            playerteam.setNameTagVisibility(p_389337_.getNametagVisibility());
            playerteam.setCollisionRule(p_389337_.getCollisionRule());
            playerteam.setPlayerPrefix(p_389337_.getPlayerPrefix());
            playerteam.setPlayerSuffix(p_389337_.getPlayerSuffix());
        });
        ClientboundSetPlayerTeamPacket.Action clientboundsetplayerteampacket$action1 = pPacket.getPlayerAction();
        if (clientboundsetplayerteampacket$action1 == ClientboundSetPlayerTeamPacket.Action.ADD) {
            for (String s : pPacket.getPlayers()) {
                this.scoreboard.addPlayerToTeam(s, playerteam);
            }
        } else if (clientboundsetplayerteampacket$action1 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            for (String s1 : pPacket.getPlayers()) {
                this.scoreboard.removePlayerFromTeam(s1, playerteam);
            }
        }

        if (clientboundsetplayerteampacket$action == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            this.scoreboard.removePlayerTeam(playerteam);
        }
    }

    @Override
    public void handleParticleEvent(ClientboundLevelParticlesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        if (pPacket.getCount() == 0) {
            double d0 = pPacket.getMaxSpeed() * pPacket.getXDist();
            double d2 = pPacket.getMaxSpeed() * pPacket.getYDist();
            double d4 = pPacket.getMaxSpeed() * pPacket.getZDist();

            try {
                this.level
                    .addParticle(
                        pPacket.getParticle(),
                        pPacket.isOverrideLimiter(),
                        pPacket.alwaysShow(),
                        pPacket.getX(),
                        pPacket.getY(),
                        pPacket.getZ(),
                        d0,
                        d2,
                        d4
                    );
            } catch (Throwable throwable1) {
                LOGGER.warn("Could not spawn particle effect {}", pPacket.getParticle());
            }
        } else {
            for (int i = 0; i < pPacket.getCount(); i++) {
                double d1 = this.random.nextGaussian() * pPacket.getXDist();
                double d3 = this.random.nextGaussian() * pPacket.getYDist();
                double d5 = this.random.nextGaussian() * pPacket.getZDist();
                double d6 = this.random.nextGaussian() * pPacket.getMaxSpeed();
                double d7 = this.random.nextGaussian() * pPacket.getMaxSpeed();
                double d8 = this.random.nextGaussian() * pPacket.getMaxSpeed();

                try {
                    this.level
                        .addParticle(
                            pPacket.getParticle(),
                            pPacket.isOverrideLimiter(),
                            pPacket.alwaysShow(),
                            pPacket.getX() + d1,
                            pPacket.getY() + d3,
                            pPacket.getZ() + d5,
                            d6,
                            d7,
                            d8
                        );
                } catch (Throwable throwable) {
                    LOGGER.warn("Could not spawn particle effect {}", pPacket.getParticle());
                    return;
                }
            }
        }
    }

    @Override
    public void handleUpdateAttributes(ClientboundUpdateAttributesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        Entity entity = this.level.getEntity(pPacket.getEntityId());
        if (entity != null) {
            if (!(entity instanceof LivingEntity)) {
                throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + entity + ")");
            } else {
                AttributeMap attributemap = ((LivingEntity)entity).getAttributes();

                for (ClientboundUpdateAttributesPacket.AttributeSnapshot clientboundupdateattributespacket$attributesnapshot : pPacket.getValues()) {
                    AttributeInstance attributeinstance = attributemap.getInstance(clientboundupdateattributespacket$attributesnapshot.attribute());
                    if (attributeinstance == null) {
                        LOGGER.warn(
                            "Entity {} does not have attribute {}", entity, clientboundupdateattributespacket$attributesnapshot.attribute().getRegisteredName()
                        );
                    } else {
                        attributeinstance.setBaseValue(clientboundupdateattributespacket$attributesnapshot.base());
                        attributeinstance.removeModifiers();

                        for (AttributeModifier attributemodifier : clientboundupdateattributespacket$attributesnapshot.modifiers()) {
                            attributeinstance.addTransientModifier(attributemodifier);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        AbstractContainerMenu abstractcontainermenu = this.minecraft.player.containerMenu;
        if (abstractcontainermenu.containerId == pPacket.containerId()) {
            if (this.minecraft.screen instanceof RecipeUpdateListener recipeupdatelistener) {
                recipeupdatelistener.fillGhostRecipe(pPacket.recipeDisplay());
            }
        }
    }

    @Override
    public void handleLightUpdatePacket(ClientboundLightUpdatePacket p_194243_) {
        PacketUtils.ensureRunningOnSameThread(p_194243_, this, this.minecraft);
        int i = p_194243_.getX();
        int j = p_194243_.getZ();
        ClientboundLightUpdatePacketData clientboundlightupdatepacketdata = p_194243_.getLightData();
        this.level.queueLightUpdate(() -> this.applyLightData(i, j, clientboundlightupdatepacketdata, true));
    }

    private void applyLightData(int pX, int pZ, ClientboundLightUpdatePacketData pData, boolean pUpdate) {
        LevelLightEngine levellightengine = this.level.getChunkSource().getLightEngine();
        BitSet bitset = pData.getSkyYMask();
        BitSet bitset1 = pData.getEmptySkyYMask();
        Iterator<byte[]> iterator = pData.getSkyUpdates().iterator();
        this.readSectionList(pX, pZ, levellightengine, LightLayer.SKY, bitset, bitset1, iterator, pUpdate);
        BitSet bitset2 = pData.getBlockYMask();
        BitSet bitset3 = pData.getEmptyBlockYMask();
        Iterator<byte[]> iterator1 = pData.getBlockUpdates().iterator();
        this.readSectionList(pX, pZ, levellightengine, LightLayer.BLOCK, bitset2, bitset3, iterator1, pUpdate);
        levellightengine.setLightEnabled(new ChunkPos(pX, pZ), true);
    }

    @Override
    public void handleMerchantOffers(ClientboundMerchantOffersPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        AbstractContainerMenu abstractcontainermenu = this.minecraft.player.containerMenu;
        if (pPacket.getContainerId() == abstractcontainermenu.containerId && abstractcontainermenu instanceof MerchantMenu merchantmenu) {
            merchantmenu.setOffers(pPacket.getOffers());
            merchantmenu.setXp(pPacket.getVillagerXp());
            merchantmenu.setMerchantLevel(pPacket.getVillagerLevel());
            merchantmenu.setShowProgressBar(pPacket.showProgress());
            merchantmenu.setCanRestock(pPacket.canRestock());
        }
    }

    @Override
    public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.serverChunkRadius = pPacket.getRadius();
        this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
        this.level.getChunkSource().updateViewRadius(pPacket.getRadius());
    }

    @Override
    public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket p_194245_) {
        PacketUtils.ensureRunningOnSameThread(p_194245_, this, this.minecraft);
        this.serverSimulationDistance = p_194245_.simulationDistance();
        this.level.setServerSimulationDistance(this.serverSimulationDistance);
    }

    @Override
    public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        this.level.getChunkSource().updateViewCenter(pPacket.getX(), pPacket.getZ());
    }

    @Override
    public void handleBlockChangedAck(ClientboundBlockChangedAckPacket p_233698_) {
        PacketUtils.ensureRunningOnSameThread(p_233698_, this, this.minecraft);
        this.level.handleBlockChangedAck(p_233698_.sequence());
    }

    @Override
    public void handleBundlePacket(ClientboundBundlePacket p_265195_) {
        PacketUtils.ensureRunningOnSameThread(p_265195_, this, this.minecraft);

        for (Packet<? super ClientGamePacketListener> packet : p_265195_.subPackets()) {
            packet.handle(this);
        }
    }

    @Override
    public void handleProjectilePowerPacket(ClientboundProjectilePowerPacket p_330827_) {
        PacketUtils.ensureRunningOnSameThread(p_330827_, this, this.minecraft);
        if (this.level.getEntity(p_330827_.getId()) instanceof AbstractHurtingProjectile abstracthurtingprojectile) {
            abstracthurtingprojectile.accelerationPower = p_330827_.getAccelerationPower();
        }
    }

    @Override
    public void handleChunkBatchStart(ClientboundChunkBatchStartPacket p_297740_) {
        this.chunkBatchSizeCalculator.onBatchStart();
    }

    @Override
    public void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket p_300262_) {
        this.chunkBatchSizeCalculator.onBatchFinished(p_300262_.batchSize());
        this.send(new ServerboundChunkBatchReceivedPacket(this.chunkBatchSizeCalculator.getDesiredChunksPerTick()));
    }

    @Override
    public void handleDebugSample(ClientboundDebugSamplePacket p_333240_) {
        this.minecraft.getDebugOverlay().logRemoteSample(p_333240_.sample(), p_333240_.debugSampleType());
    }

    @Override
    public void handlePongResponse(ClientboundPongResponsePacket p_329147_) {
        this.pingDebugMonitor.onPongReceived(p_329147_);
    }

    @Override
    public void handleTestInstanceBlockStatus(ClientboundTestInstanceBlockStatus p_393618_) {
        PacketUtils.ensureRunningOnSameThread(p_393618_, this, this.minecraft);
        if (this.minecraft.screen instanceof TestInstanceBlockEditScreen testinstanceblockeditscreen) {
            testinstanceblockeditscreen.setStatus(p_393618_.status(), p_393618_.size());
        }
    }

    @Override
    public void handleWaypoint(ClientboundTrackedWaypointPacket p_407680_) {
        PacketUtils.ensureRunningOnSameThread(p_407680_, this, this.minecraft);
        p_407680_.apply(this.waypointManager);
    }

    private void readSectionList(
        int pX,
        int pZ,
        LevelLightEngine pLightEngine,
        LightLayer pLightLayer,
        BitSet pSkyYMask,
        BitSet pEmptySkyYMask,
        Iterator<byte[]> pSkyUpdates,
        boolean pUpdate
    ) {
        for (int i = 0; i < pLightEngine.getLightSectionCount(); i++) {
            int j = pLightEngine.getMinLightSection() + i;
            boolean flag = pSkyYMask.get(i);
            boolean flag1 = pEmptySkyYMask.get(i);
            if (flag || flag1) {
                pLightEngine.queueSectionData(
                    pLightLayer, SectionPos.of(pX, j, pZ), flag ? new DataLayer((byte[])pSkyUpdates.next().clone()) : new DataLayer()
                );
                if (pUpdate) {
                    this.level.setSectionDirtyWithNeighbors(pX, j, pZ);
                }
            }
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected() && !this.closed;
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return this.listedPlayers;
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return this.playerInfoMap.values();
    }

    public Collection<UUID> getOnlinePlayerIds() {
        return this.playerInfoMap.keySet();
    }

    @Nullable
    public PlayerInfo getPlayerInfo(UUID pUniqueId) {
        return this.playerInfoMap.get(pUniqueId);
    }

    @Nullable
    public PlayerInfo getPlayerInfo(String pName) {
        for (PlayerInfo playerinfo : this.playerInfoMap.values()) {
            if (playerinfo.getProfile().getName().equals(pName)) {
                return playerinfo;
            }
        }

        return null;
    }

    public GameProfile getLocalGameProfile() {
        return this.localGameProfile;
    }

    public ClientAdvancements getAdvancements() {
        return this.advancements;
    }

    public CommandDispatcher<ClientSuggestionProvider> getCommands() {
        return this.commands;
    }

    public ClientLevel getLevel() {
        return this.level;
    }

    public DebugQueryHandler getDebugQueryHandler() {
        return this.debugQueryHandler;
    }

    public UUID getId() {
        return this.id;
    }

    public Set<ResourceKey<Level>> levels() {
        return this.levels;
    }

    public RegistryAccess.Frozen registryAccess() {
        return this.registryAccess;
    }

    public void markMessageAsProcessed(MessageSignature pSignature, boolean pAcknowledged) {
        if (this.lastSeenMessages.addPending(pSignature, pAcknowledged) && this.lastSeenMessages.offset() > 64) {
            this.sendChatAcknowledgement();
        }
    }

    private void sendChatAcknowledgement() {
        int i = this.lastSeenMessages.getAndClearOffset();
        if (i > 0) {
            this.send(new ServerboundChatAckPacket(i));
        }
    }

    public void sendChat(String pMessage) {
        pMessage = net.minecraftforge.client.ForgeHooksClient.onClientSendMessage(pMessage);
        if (pMessage.isEmpty()) return;
        Instant instant = Instant.now();
        long i = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update lastseenmessagestracker$update = this.lastSeenMessages.generateAndApplyUpdate();
        MessageSignature messagesignature = this.signedMessageEncoder.pack(new SignedMessageBody(pMessage, instant, i, lastseenmessagestracker$update.lastSeen()));
        this.send(new ServerboundChatPacket(pMessage, instant, i, messagesignature, lastseenmessagestracker$update.update()));
    }

    public void sendCommand(String pCommand) {
        if (net.minecraftforge.client.ClientCommandHandler.runCommand(pCommand)) return;
        SignableCommand<ClientSuggestionProvider> signablecommand = SignableCommand.of(this.commands.parse(pCommand, this.suggestionsProvider));
        if (signablecommand.arguments().isEmpty()) {
            this.send(new ServerboundChatCommandPacket(pCommand));
        } else {
            Instant instant = Instant.now();
            long i = Crypt.SaltSupplier.getLong();
            LastSeenMessagesTracker.Update lastseenmessagestracker$update = this.lastSeenMessages.generateAndApplyUpdate();
            ArgumentSignatures argumentsignatures = ArgumentSignatures.signCommand(signablecommand, p_247875_ -> {
                SignedMessageBody signedmessagebody = new SignedMessageBody(p_247875_, instant, i, lastseenmessagestracker$update.lastSeen());
                return this.signedMessageEncoder.pack(signedmessagebody);
            });
            this.send(new ServerboundChatCommandSignedPacket(pCommand, instant, i, argumentsignatures, lastseenmessagestracker$update.update()));
        }
    }

    public void sendUnattendedCommand(String pCommand, @Nullable Screen pPreviousScreen) {
        if (net.minecraftforge.client.ClientCommandHandler.runCommand(pCommand)) return;
        switch (this.verifyCommand(pCommand)) {
            case NO_ISSUES:
                this.send(new ServerboundChatCommandPacket(pCommand));
                this.minecraft.setScreen(pPreviousScreen);
                break;
            case PARSE_ERRORS:
                this.openCommandSendConfirmationWindow(pCommand, "multiplayer.confirm_command.parse_errors", pPreviousScreen);
                break;
            case SIGNATURE_REQUIRED:
                LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", pCommand);
                break;
            case PERMISSIONS_REQUIRED:
                this.openCommandSendConfirmationWindow(pCommand, "multiplayer.confirm_command.permissions_required", pPreviousScreen);
        }
    }

    private ClientPacketListener.CommandCheckResult verifyCommand(String pCommand) {
        ParseResults<ClientSuggestionProvider> parseresults = this.commands.parse(pCommand, this.suggestionsProvider);
        if (!isValidCommand(parseresults)) {
            return ClientPacketListener.CommandCheckResult.PARSE_ERRORS;
        } else if (SignableCommand.hasSignableArguments(parseresults)) {
            return ClientPacketListener.CommandCheckResult.SIGNATURE_REQUIRED;
        } else {
            ParseResults<ClientSuggestionProvider> parseresults1 = this.commands.parse(pCommand, this.restrictedSuggestionsProvider);
            return !isValidCommand(parseresults1) ? ClientPacketListener.CommandCheckResult.PERMISSIONS_REQUIRED : ClientPacketListener.CommandCheckResult.NO_ISSUES;
        }
    }

    private static boolean isValidCommand(ParseResults<?> pParseResults) {
        return !pParseResults.getReader().canRead() && pParseResults.getExceptions().isEmpty() && pParseResults.getContext().getLastChild().getCommand() != null;
    }

    private void openCommandSendConfirmationWindow(String pCommand, String pTitleKey, @Nullable Screen pPreviousScreen) {
        Screen screen = this.minecraft.screen;
        this.minecraft.setScreen(new ConfirmScreen(p_404888_ -> {
            if (p_404888_) {
                this.send(new ServerboundChatCommandPacket(pCommand));
            }

            if (p_404888_) {
                this.minecraft.setScreen(pPreviousScreen);
            } else {
                this.minecraft.setScreen(screen);
            }
        }, COMMAND_SEND_CONFIRM_TITLE, Component.translatable(pTitleKey, Component.literal(pCommand).withStyle(ChatFormatting.YELLOW))));
    }

    public void broadcastClientInformation(ClientInformation pInformation) {
        if (!pInformation.equals(this.remoteClientInformation)) {
            this.send(new ServerboundClientInformationPacket(pInformation));
            this.remoteClientInformation = pInformation;
        }
    }

    @Override
    public void tick() {
        if (this.chatSession != null && this.minecraft.getProfileKeyPairManager().shouldRefreshKeyPair()) {
            this.prepareKeyPair();
        }

        if (this.keyPairFuture != null && this.keyPairFuture.isDone()) {
            this.keyPairFuture.join().ifPresent(this::setKeyPair);
            this.keyPairFuture = null;
        }

        this.sendDeferredPackets();
        if (this.minecraft.getDebugOverlay().showNetworkCharts()) {
            this.pingDebugMonitor.tick();
        }

        this.debugSampleSubscriber.tick();
        this.telemetryManager.tick();
        if (this.levelLoadStatusManager != null) {
            this.levelLoadStatusManager.tick();
            if (this.levelLoadStatusManager.levelReady() && !this.minecraft.player.hasClientLoaded()) {
                this.connection.send(new ServerboundPlayerLoadedPacket());
                this.minecraft.player.setClientLoaded(true);
            }
        }
    }

    public void prepareKeyPair() {
        this.keyPairFuture = this.minecraft.getProfileKeyPairManager().prepareKeyPair();
    }

    private void setKeyPair(ProfileKeyPair pKeyPair) {
        if (this.minecraft.isLocalPlayer(this.localGameProfile.getId())) {
            if (this.chatSession == null || !this.chatSession.keyPair().equals(pKeyPair)) {
                this.chatSession = LocalChatSession.create(pKeyPair);
                this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.getId());
                this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
            }
        }
    }

    @Override
    protected DialogConnectionAccess createDialogAccess() {
        return new DialogConnectionAccess() {
            @Override
            public void disconnect(Component p_408484_) {
                ClientPacketListener.this.getConnection().disconnect(p_408484_);
            }

            @Override
            public void runCommand(String p_406542_, @Nullable Screen p_410460_) {
                ClientPacketListener.this.sendUnattendedCommand(p_406542_, p_410460_);
            }

            @Override
            public void openDialog(Holder<Dialog> p_405822_, @Nullable Screen p_408171_) {
                ClientPacketListener.this.showDialog(p_405822_, this, p_408171_);
            }

            @Override
            public void sendCustomAction(ResourceLocation p_410464_, Optional<Tag> p_409454_) {
                ClientPacketListener.this.send(new ServerboundCustomClickActionPacket(p_410464_, p_409454_));
            }

            @Override
            public ServerLinks serverLinks() {
                return ClientPacketListener.this.serverLinks();
            }
        };
    }

    @Nullable
    public ServerData getServerData() {
        return this.serverData;
    }

    public FeatureFlagSet enabledFeatures() {
        return this.enabledFeatures;
    }

    public boolean isFeatureEnabled(FeatureFlagSet pEnabledFeatures) {
        return pEnabledFeatures.isSubsetOf(this.enabledFeatures());
    }

    public Scoreboard scoreboard() {
        return this.scoreboard;
    }

    public PotionBrewing potionBrewing() {
        return this.potionBrewing;
    }

    public FuelValues fuelValues() {
        return this.fuelValues;
    }

    public void updateSearchTrees() {
        this.searchTrees.rebuildAfterLanguageChange();
    }

    public SessionSearchTrees searchTrees() {
        return this.searchTrees;
    }

    public void registerForCleaning(CacheSlot<?, ?> pCacheSlot) {
        this.cacheSlots.add(new WeakReference<>(pCacheSlot));
    }

    public HashedPatchMap.HashGenerator decoratedHashOpsGenenerator() {
        return this.decoratedHashOpsGenerator;
    }

    public ClientWaypointManager getWaypointManager() {
        return this.waypointManager;
    }

    @OnlyIn(Dist.CLIENT)
    static enum CommandCheckResult {
        NO_ISSUES,
        PARSE_ERRORS,
        SIGNATURE_REQUIRED,
        PERMISSIONS_REQUIRED;
    }
}

package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class DataPackCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PACK = new DynamicCommandExceptionType(
        p_308647_ -> Component.translatableEscape("commands.datapack.unknown", p_308647_)
    );
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_ENABLED = new DynamicCommandExceptionType(
        p_308646_ -> Component.translatableEscape("commands.datapack.enable.failed", p_308646_)
    );
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_DISABLED = new DynamicCommandExceptionType(
        p_308645_ -> Component.translatableEscape("commands.datapack.disable.failed", p_308645_)
    );
    private static final DynamicCommandExceptionType ERROR_CANNOT_DISABLE_FEATURE = new DynamicCommandExceptionType(
        p_326235_ -> Component.translatableEscape("commands.datapack.disable.failed.feature", p_326235_)
    );
    private static final Dynamic2CommandExceptionType ERROR_PACK_FEATURES_NOT_ENABLED = new Dynamic2CommandExceptionType(
        (p_308643_, p_308644_) -> Component.translatableEscape("commands.datapack.enable.failed.no_flags", p_308643_, p_308644_)
    );
    private static final DynamicCommandExceptionType ERROR_PACK_INVALID_NAME = new DynamicCommandExceptionType(
        p_405152_ -> Component.translatableEscape("commands.datapack.create.invalid_name", p_405152_)
    );
    private static final DynamicCommandExceptionType ERROR_PACK_INVALID_FULL_NAME = new DynamicCommandExceptionType(
        p_405146_ -> Component.translatableEscape("commands.datapack.create.invalid_full_name", p_405146_)
    );
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_EXISTS = new DynamicCommandExceptionType(
        p_405145_ -> Component.translatableEscape("commands.datapack.create.already_exists", p_405145_)
    );
    private static final Dynamic2CommandExceptionType ERROR_PACK_METADATA_ENCODE_FAILURE = new Dynamic2CommandExceptionType(
        (p_405147_, p_405148_) -> Component.translatableEscape("commands.datapack.create.metadata_encode_failure", p_405147_, p_405148_)
    );
    private static final DynamicCommandExceptionType ERROR_PACK_IO_FAILURE = new DynamicCommandExceptionType(
        p_405149_ -> Component.translatableEscape("commands.datapack.create.io_failure", p_405149_)
    );
    private static final SuggestionProvider<CommandSourceStack> SELECTED_PACKS = (p_136848_, p_136849_) -> SharedSuggestionProvider.suggest(
        p_136848_.getSource().getServer().getPackRepository().getSelectedIds().stream().map(StringArgumentType::escapeIfRequired), p_136849_
    );
    private static final SuggestionProvider<CommandSourceStack> UNSELECTED_PACKS = (p_248113_, p_248114_) -> {
        PackRepository packrepository = p_248113_.getSource().getServer().getPackRepository();
        Collection<String> collection = packrepository.getSelectedIds();
        FeatureFlagSet featureflagset = p_248113_.getSource().enabledFeatures();
        return SharedSuggestionProvider.suggest(
            packrepository.getAvailablePacks()
                .stream()
                .filter(p_248116_ -> p_248116_.getRequestedFeatures().isSubsetOf(featureflagset))
                .map(Pack::getId)
                .filter(p_250072_ -> !collection.contains(p_250072_))
                .map(StringArgumentType::escapeIfRequired),
            p_248114_
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pBuildContext) {
        pDispatcher.register(
            Commands.literal("datapack")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.literal("enable")
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .suggests(UNSELECTED_PACKS)
                                .executes(
                                    p_136876_ -> enablePack(
                                        p_136876_.getSource(),
                                        getPack(p_136876_, "name", true),
                                        (p_180059_, p_180060_) -> p_180060_.getDefaultPosition().insert(p_180059_, p_180060_, Pack::selectionConfig, false)
                                    )
                                )
                                .then(
                                    Commands.literal("after")
                                        .then(
                                            Commands.argument("existing", StringArgumentType.string())
                                                .suggests(SELECTED_PACKS)
                                                .executes(
                                                    p_136880_ -> enablePack(
                                                        p_136880_.getSource(),
                                                        getPack(p_136880_, "name", true),
                                                        (p_180056_, p_180057_) -> p_180056_.add(
                                                            p_180056_.indexOf(getPack(p_136880_, "existing", false)) + 1, p_180057_
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("before")
                                        .then(
                                            Commands.argument("existing", StringArgumentType.string())
                                                .suggests(SELECTED_PACKS)
                                                .executes(
                                                    p_136878_ -> enablePack(
                                                        p_136878_.getSource(),
                                                        getPack(p_136878_, "name", true),
                                                        (p_180046_, p_180047_) -> p_180046_.add(
                                                            p_180046_.indexOf(getPack(p_136878_, "existing", false)), p_180047_
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("last")
                                        .executes(p_136874_ -> enablePack(p_136874_.getSource(), getPack(p_136874_, "name", true), List::add))
                                )
                                .then(
                                    Commands.literal("first")
                                        .executes(
                                            p_136882_ -> enablePack(
                                                p_136882_.getSource(),
                                                getPack(p_136882_, "name", true),
                                                (p_180052_, p_180053_) -> p_180052_.add(0, p_180053_)
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("disable")
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .suggests(SELECTED_PACKS)
                                .executes(p_136870_ -> disablePack(p_136870_.getSource(), getPack(p_136870_, "name", false)))
                        )
                )
                .then(
                    Commands.literal("list")
                        .executes(p_136864_ -> listPacks(p_136864_.getSource()))
                        .then(Commands.literal("available").executes(p_136846_ -> listAvailablePacks(p_136846_.getSource())))
                        .then(Commands.literal("enabled").executes(p_136811_ -> listEnabledPacks(p_136811_.getSource())))
                )
                .then(
                    Commands.literal("create")
                        .requires(Commands.hasPermission(4))
                        .then(
                            Commands.argument("id", StringArgumentType.string())
                                .then(
                                    Commands.argument("description", ComponentArgument.textComponent(pBuildContext))
                                        .executes(
                                            p_405151_ -> createPack(
                                                p_405151_.getSource(),
                                                StringArgumentType.getString(p_405151_, "id"),
                                                ComponentArgument.getResolvedComponent(p_405151_, "description")
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int createPack(CommandSourceStack pSource, String pId, Component pDescription) throws CommandSyntaxException {
        Path path = pSource.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
        if (!FileUtil.isValidStrictPathSegment(pId)) {
            throw ERROR_PACK_INVALID_NAME.create(pId);
        } else if (!FileUtil.isPathPartPortable(pId)) {
            throw ERROR_PACK_INVALID_FULL_NAME.create(pId);
        } else {
            Path path1 = path.resolve(pId);
            if (Files.exists(path1)) {
                throw ERROR_PACK_ALREADY_EXISTS.create(pId);
            } else {
                PackMetadataSection packmetadatasection = new PackMetadataSection(
                    pDescription, SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA), Optional.empty()
                );
                DataResult<JsonElement> dataresult = PackMetadataSection.CODEC.encodeStart(JsonOps.INSTANCE, packmetadatasection);
                Optional<Error<JsonElement>> optional = dataresult.error();
                if (optional.isPresent()) {
                    throw ERROR_PACK_METADATA_ENCODE_FAILURE.create(pId, optional.get().message());
                } else {
                    JsonObject jsonobject = new JsonObject();
                    jsonobject.add(PackMetadataSection.TYPE.name(), dataresult.getOrThrow());

                    try {
                        Files.createDirectory(path1);
                        Files.createDirectory(path1.resolve(PackType.SERVER_DATA.getDirectory()));

                        try (
                            BufferedWriter bufferedwriter = Files.newBufferedWriter(path1.resolve("pack.mcmeta"), StandardCharsets.UTF_8);
                            JsonWriter jsonwriter = new JsonWriter(bufferedwriter);
                        ) {
                            jsonwriter.setSerializeNulls(false);
                            jsonwriter.setIndent("  ");
                            GsonHelper.writeValue(jsonwriter, jsonobject, null);
                        }
                    } catch (IOException ioexception) {
                        LOGGER.warn("Failed to create pack at {}", path.toAbsolutePath(), ioexception);
                        throw ERROR_PACK_IO_FAILURE.create(pId);
                    }

                    pSource.sendSuccess(() -> Component.translatable("commands.datapack.create.success", pId), true);
                    return 1;
                }
            }
        }
    }

    private static int enablePack(CommandSourceStack pSource, Pack pPack, DataPackCommand.Inserter pPriorityCallback) throws CommandSyntaxException {
        PackRepository packrepository = pSource.getServer().getPackRepository();
        List<Pack> list = Lists.newArrayList(packrepository.getSelectedPacks());
        pPriorityCallback.apply(list, pPack);
        pSource.sendSuccess(() -> Component.translatable("commands.datapack.modify.enable", pPack.getChatLink(true)), true);
        ReloadCommand.reloadPacks(list.stream().map(Pack::getId).collect(Collectors.toList()), pSource);
        return list.size();
    }

    private static int disablePack(CommandSourceStack pSource, Pack pPack) {
        PackRepository packrepository = pSource.getServer().getPackRepository();
        List<Pack> list = Lists.newArrayList(packrepository.getSelectedPacks());
        list.remove(pPack);
        pSource.sendSuccess(() -> Component.translatable("commands.datapack.modify.disable", pPack.getChatLink(true)), true);
        ReloadCommand.reloadPacks(list.stream().map(Pack::getId).collect(Collectors.toList()), pSource);
        return list.size();
    }

    private static int listPacks(CommandSourceStack pSource) {
        return listEnabledPacks(pSource) + listAvailablePacks(pSource);
    }

    private static int listAvailablePacks(CommandSourceStack pSource) {
        PackRepository packrepository = pSource.getServer().getPackRepository();
        packrepository.reload();
        Collection<Pack> collection = packrepository.getSelectedPacks();
        Collection<Pack> collection1 = packrepository.getAvailablePacks();
        FeatureFlagSet featureflagset = pSource.enabledFeatures();
        List<Pack> list = collection1.stream().filter(p_248121_ -> !collection.contains(p_248121_) && p_248121_.getRequestedFeatures().isSubsetOf(featureflagset)).toList();
        if (list.isEmpty()) {
            pSource.sendSuccess(() -> Component.translatable("commands.datapack.list.available.none"), false);
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                    "commands.datapack.list.available.success", list.size(), ComponentUtils.formatList(list, p_136844_ -> p_136844_.getChatLink(false))
                ),
                false
            );
        }

        return list.size();
    }

    private static int listEnabledPacks(CommandSourceStack pSource) {
        PackRepository packrepository = pSource.getServer().getPackRepository();
        packrepository.reload();
        Collection<? extends Pack> collection = packrepository.getSelectedPacks();
        if (collection.isEmpty()) {
            pSource.sendSuccess(() -> Component.translatable("commands.datapack.list.enabled.none"), false);
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                    "commands.datapack.list.enabled.success", collection.size(), ComponentUtils.formatList(collection, p_136807_ -> p_136807_.getChatLink(true))
                ),
                false
            );
        }

        return collection.size();
    }

    private static Pack getPack(CommandContext<CommandSourceStack> pContext, String pName, boolean pEnabling) throws CommandSyntaxException {
        String s = StringArgumentType.getString(pContext, pName);
        PackRepository packrepository = pContext.getSource().getServer().getPackRepository();
        Pack pack = packrepository.getPack(s);
        if (pack == null) {
            throw ERROR_UNKNOWN_PACK.create(s);
        } else {
            boolean flag = packrepository.getSelectedPacks().contains(pack);
            if (pEnabling && flag) {
                throw ERROR_PACK_ALREADY_ENABLED.create(s);
            } else if (!pEnabling && !flag) {
                throw ERROR_PACK_ALREADY_DISABLED.create(s);
            } else {
                FeatureFlagSet featureflagset = pContext.getSource().enabledFeatures();
                FeatureFlagSet featureflagset1 = pack.getRequestedFeatures();
                if (!pEnabling && !featureflagset1.isEmpty() && pack.getPackSource() == PackSource.FEATURE) {
                    throw ERROR_CANNOT_DISABLE_FEATURE.create(s);
                } else if (!featureflagset1.isSubsetOf(featureflagset)) {
                    throw ERROR_PACK_FEATURES_NOT_ENABLED.create(s, FeatureFlags.printMissingFlags(featureflagset, featureflagset1));
                } else {
                    return pack;
                }
            }
        }
    }

    interface Inserter {
        void apply(List<Pack> pCurrentPacks, Pack pPack) throws CommandSyntaxException;
    }
}
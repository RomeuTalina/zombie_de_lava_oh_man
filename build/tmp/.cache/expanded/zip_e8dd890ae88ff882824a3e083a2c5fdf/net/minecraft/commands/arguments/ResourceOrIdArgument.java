package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.ResourceLocationParseRule;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ResourceOrIdArgument<T> implements ArgumentType<Holder<T>> {
    private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
    public static final DynamicCommandExceptionType ERROR_FAILED_TO_PARSE = new DynamicCommandExceptionType(
        p_334248_ -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", p_334248_)
    );
    public static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ELEMENT = new Dynamic2CommandExceptionType(
        (p_405039_, p_405040_) -> Component.translatableEscape("argument.resource_or_id.no_such_element", p_405039_, p_405040_)
    );
    public static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private final HolderLookup.Provider registryLookup;
    private final Optional<? extends HolderLookup.RegistryLookup<T>> elementLookup;
    private final Codec<T> codec;
    private final Grammar<ResourceOrIdArgument.Result<T, Tag>> grammar;
    private final ResourceKey<? extends Registry<T>> registryKey;

    protected ResourceOrIdArgument(CommandBuildContext pRegistryLookup, ResourceKey<? extends Registry<T>> pRegistryKey, Codec<T> pCodec) {
        this.registryLookup = pRegistryLookup;
        this.elementLookup = pRegistryLookup.lookup(pRegistryKey);
        this.registryKey = pRegistryKey;
        this.codec = pCodec;
        this.grammar = createGrammar(pRegistryKey, OPS);
    }

    public static <T, O> Grammar<ResourceOrIdArgument.Result<T, O>> createGrammar(ResourceKey<? extends Registry<T>> pRegistryKey, DynamicOps<O> pOps) {
        Grammar<O> grammar = SnbtGrammar.createParser(pOps);
        Dictionary<StringReader> dictionary = new Dictionary<>();
        Atom<ResourceOrIdArgument.Result<T, O>> atom = Atom.of("result");
        Atom<ResourceLocation> atom1 = Atom.of("id");
        Atom<O> atom2 = Atom.of("value");
        dictionary.put(atom1, ResourceLocationParseRule.INSTANCE);
        dictionary.put(atom2, grammar.top().value());
        NamedRule<StringReader, ResourceOrIdArgument.Result<T, O>> namedrule = dictionary.put(
            atom, Term.alternative(dictionary.named(atom1), dictionary.named(atom2)), p_405044_ -> {
                ResourceLocation resourcelocation = p_405044_.get(atom1);
                if (resourcelocation != null) {
                    return new ResourceOrIdArgument.ReferenceResult<>(ResourceKey.create(pRegistryKey, resourcelocation));
                } else {
                    O o = p_405044_.getOrThrow(atom2);
                    return new ResourceOrIdArgument.InlineResult<>(o);
                }
            }
        );
        return new Grammar<>(dictionary, namedrule);
    }

    public static ResourceOrIdArgument.LootTableArgument lootTable(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.LootTableArgument(pContext);
    }

    public static Holder<LootTable> getLootTable(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return getResource(pContext, pName);
    }

    public static ResourceOrIdArgument.LootModifierArgument lootModifier(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.LootModifierArgument(pContext);
    }

    public static Holder<LootItemFunction> getLootModifier(CommandContext<CommandSourceStack> pContext, String pName) {
        return getResource(pContext, pName);
    }

    public static ResourceOrIdArgument.LootPredicateArgument lootPredicate(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.LootPredicateArgument(pContext);
    }

    public static Holder<LootItemCondition> getLootPredicate(CommandContext<CommandSourceStack> pContext, String pName) {
        return getResource(pContext, pName);
    }

    public static ResourceOrIdArgument.DialogArgument dialog(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.DialogArgument(pContext);
    }

    public static Holder<Dialog> getDialog(CommandContext<CommandSourceStack> pContext, String pName) {
        return getResource(pContext, pName);
    }

    private static <T> Holder<T> getResource(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, Holder.class);
    }

    @Nullable
    public Holder<T> parse(StringReader pReader) throws CommandSyntaxException {
        return this.parse(pReader, this.grammar, OPS);
    }

    @Nullable
    private <O> Holder<T> parse(StringReader pReader, Grammar<ResourceOrIdArgument.Result<T, O>> pGrammar, DynamicOps<O> pOps) throws CommandSyntaxException {
        ResourceOrIdArgument.Result<T, O> result = pGrammar.parseForCommands(pReader);
        return this.elementLookup.isEmpty()
            ? null
            : result.parse(pReader, this.registryLookup, pOps, this.codec, (HolderLookup.RegistryLookup<T>)this.elementLookup.get());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        return SharedSuggestionProvider.listSuggestions(pContext, pBuilder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class DialogArgument extends ResourceOrIdArgument<Dialog> {
        protected DialogArgument(CommandBuildContext pContext) {
            super(pContext, Registries.DIALOG, Dialog.DIRECT_CODEC);
        }
    }

    public record InlineResult<T, O>(O value) implements ResourceOrIdArgument.Result<T, O> {
        @Override
        public Holder<T> parse(
            ImmutableStringReader p_409546_,
            HolderLookup.Provider p_410228_,
            DynamicOps<O> p_410382_,
            Codec<T> p_408251_,
            HolderLookup.RegistryLookup<T> p_406267_
        ) throws CommandSyntaxException {
            return Holder.direct(
                p_408251_.parse(p_410228_.createSerializationContext(p_410382_), this.value)
                    .getOrThrow(p_408685_ -> ResourceOrIdArgument.ERROR_FAILED_TO_PARSE.createWithContext(p_409546_, p_408685_))
            );
        }
    }

    public static class LootModifierArgument extends ResourceOrIdArgument<LootItemFunction> {
        protected LootModifierArgument(CommandBuildContext pContext) {
            super(pContext, Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC);
        }
    }

    public static class LootPredicateArgument extends ResourceOrIdArgument<LootItemCondition> {
        protected LootPredicateArgument(CommandBuildContext pContext) {
            super(pContext, Registries.PREDICATE, LootItemCondition.DIRECT_CODEC);
        }
    }

    public static class LootTableArgument extends ResourceOrIdArgument<LootTable> {
        protected LootTableArgument(CommandBuildContext pContext) {
            super(pContext, Registries.LOOT_TABLE, LootTable.DIRECT_CODEC);
        }
    }

    public record ReferenceResult<T, O>(ResourceKey<T> key) implements ResourceOrIdArgument.Result<T, O> {
        @Override
        public Holder<T> parse(
            ImmutableStringReader p_410510_,
            HolderLookup.Provider p_406672_,
            DynamicOps<O> p_409410_,
            Codec<T> p_410626_,
            HolderLookup.RegistryLookup<T> p_406658_
        ) throws CommandSyntaxException {
            return p_406658_.get(this.key)
                .orElseThrow(() -> ResourceOrIdArgument.ERROR_NO_SUCH_ELEMENT.createWithContext(p_410510_, this.key.location(), this.key.registry()));
        }
    }

    public sealed interface Result<T, O> permits ResourceOrIdArgument.InlineResult, ResourceOrIdArgument.ReferenceResult {
        Holder<T> parse(
            ImmutableStringReader pReader,
            HolderLookup.Provider pRegistryLookup,
            DynamicOps<O> pOps,
            Codec<T> pCodec,
            HolderLookup.RegistryLookup<T> pElementLookup
        ) throws CommandSyntaxException;
    }
}
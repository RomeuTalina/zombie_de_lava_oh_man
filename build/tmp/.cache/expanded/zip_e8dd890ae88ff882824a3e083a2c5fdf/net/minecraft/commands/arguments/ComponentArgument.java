package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.entity.Entity;

public class ComponentArgument extends ParserBasedArgument<Component> {
    private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "'hello world'", "\"\"", "{text:\"hello world\"}", "[\"\"]");
    public static final DynamicCommandExceptionType ERROR_INVALID_COMPONENT = new DynamicCommandExceptionType(
        p_308346_ -> Component.translatableEscape("argument.component.invalid", p_308346_)
    );
    private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(OPS);

    private ComponentArgument(HolderLookup.Provider pRegistries) {
        super(TAG_PARSER.withCodec(pRegistries.createSerializationContext(OPS), TAG_PARSER, ComponentSerialization.CODEC, ERROR_INVALID_COMPONENT));
    }

    public static Component getRawComponent(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, Component.class);
    }

    public static Component getResolvedComponent(CommandContext<CommandSourceStack> pContext, String pName, @Nullable Entity pEntity) throws CommandSyntaxException {
        return ComponentUtils.updateForEntity(pContext.getSource(), getRawComponent(pContext, pName), pEntity, 0);
    }

    public static Component getResolvedComponent(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return getResolvedComponent(pContext, pName, pContext.getSource().getEntity());
    }

    public static ComponentArgument textComponent(CommandBuildContext pContext) {
        return new ComponentArgument(pContext);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
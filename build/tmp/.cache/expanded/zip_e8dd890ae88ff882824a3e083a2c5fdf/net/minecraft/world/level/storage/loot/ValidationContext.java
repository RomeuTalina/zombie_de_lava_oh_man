package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;

/**
 * Context for validating loot tables. Loot tables are validated recursively by checking that all functions, conditions,
 * etc. (implementing {@link LootContextUser}) are valid according to their LootTable's {@link LootContextParamSet}.
 */
public class ValidationContext {
    private final ProblemReporter reporter;
    private final ContextKeySet contextKeySet;
    private final Optional<HolderGetter.Provider> resolver;
    private final Set<ResourceKey<?>> visitedElements;

    public ValidationContext(ProblemReporter pReporter, ContextKeySet pContextKeySet, HolderGetter.Provider pResolver) {
        this(pReporter, pContextKeySet, Optional.of(pResolver), Set.of());
    }

    public ValidationContext(ProblemReporter pReporter, ContextKeySet pContextKeySet) {
        this(pReporter, pContextKeySet, Optional.empty(), Set.of());
    }

    private ValidationContext(ProblemReporter pReporter, ContextKeySet pContextKeySet, Optional<HolderGetter.Provider> pResolver, Set<ResourceKey<?>> pVisitedElements) {
        this.reporter = pReporter;
        this.contextKeySet = pContextKeySet;
        this.resolver = pResolver;
        this.visitedElements = pVisitedElements;
    }

    public ValidationContext forChild(ProblemReporter.PathElement pChild) {
        return new ValidationContext(this.reporter.forChild(pChild), this.contextKeySet, this.resolver, this.visitedElements);
    }

    public ValidationContext enterElement(ProblemReporter.PathElement pElement, ResourceKey<?> pKey) {
        Set<ResourceKey<?>> set = ImmutableSet.<ResourceKey<?>>builder().addAll(this.visitedElements).add(pKey).build();
        return new ValidationContext(this.reporter.forChild(pElement), this.contextKeySet, this.resolver, set);
    }

    public boolean hasVisitedElement(ResourceKey<?> pKey) {
        return this.visitedElements.contains(pKey);
    }

    public void reportProblem(ProblemReporter.Problem pProblem) {
        this.reporter.report(pProblem);
    }

    public void validateContextUsage(LootContextUser pUser) {
        Set<ContextKey<?>> set = pUser.getReferencedContextParams();
        Set<ContextKey<?>> set1 = Sets.difference(set, this.contextKeySet.allowed());
        if (!set1.isEmpty()) {
            this.reporter.report(new ValidationContext.ParametersNotProvidedProblem(set1));
        }
    }

    public HolderGetter.Provider resolver() {
        return this.resolver.orElseThrow(() -> new UnsupportedOperationException("References not allowed"));
    }

    public boolean allowsReferences() {
        return this.resolver.isPresent();
    }

    public ValidationContext setContextKeySet(ContextKeySet pContextKeySet) {
        return new ValidationContext(this.reporter, pContextKeySet, this.resolver, this.visitedElements);
    }

    public ProblemReporter reporter() {
        return this.reporter;
    }

    public record MissingReferenceProblem(ResourceKey<?> referenced) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Missing element " + this.referenced.location() + " of type " + this.referenced.registry();
        }
    }

    public record ParametersNotProvidedProblem(Set<ContextKey<?>> notProvided) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Parameters " + this.notProvided + " are not provided in this context";
        }
    }

    public record RecursiveReferenceProblem(ResourceKey<?> referenced) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return this.referenced.location() + " of type " + this.referenced.registry() + " is recursively called";
        }
    }

    public record ReferenceNotAllowedProblem(ResourceKey<?> referenced) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Reference to " + this.referenced.location() + " of type " + this.referenced.registry() + " was used, but references are not allowed";
        }
    }
}
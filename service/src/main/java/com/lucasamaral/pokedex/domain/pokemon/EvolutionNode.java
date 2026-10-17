package com.lucasamaral.pokedex.domain.pokemon;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requireNonBlank;
import static com.lucasamaral.pokedex.domain.pokemon.Preconditions.requirePositive;

/**
 * One species in an evolution chain, with the species it can evolve into.
 * <p>
 * Composite: the chain is a tree, so a linear line (Bulbasaur) and a branching one (Eevee) share one shape.
 *
 * @param spriteUrl sprite of the species, or {@code null} when unknown
 * @param condition how the previous stage evolves into this one; {@code null} for the base form
 */
public record EvolutionNode(
        int speciesId,
        String name,
        String spriteUrl,
        EvolutionCondition condition,
        List<EvolutionNode> evolvesTo) {

    public EvolutionNode {
        requirePositive(speciesId, "Species id must be positive");
        requireNonBlank(name, "Species name must not be blank");
        evolvesTo = List.copyOf(Objects.requireNonNull(evolvesTo, "evolvesTo is required"));
    }

    public boolean isFinalStage() {
        return evolvesTo.isEmpty();
    }

    /** Every species in the tree, depth first, starting with this one. */
    public List<String> names() {
        return stages().map(EvolutionNode::name).toList();
    }

    private Stream<EvolutionNode> stages() {
        return Stream.concat(Stream.of(this), evolvesTo.stream().flatMap(EvolutionNode::stages));
    }
}

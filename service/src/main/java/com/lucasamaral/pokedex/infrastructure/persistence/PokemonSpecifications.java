package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the collection query from the filters that are present, so absent filters never reach SQL as nulls.
 */
final class PokemonSpecifications {

    private static final char ESCAPE = '\\';

    private PokemonSpecifications() {
    }

    static Specification<PokemonEntity> matching(CollectionFilter filter) {
        Specification<PokemonEntity> spec = Specification.unrestricted();
        if (filter.name() != null) {
            spec = spec.and(anyNameContains(filter.name()));
        }
        if (filter.tag() != null) {
            spec = spec.and(hasTag(filter.tag()));
        }
        if (filter.pokeapiId() != null) {
            spec = spec.and(hasPokeapiId(filter.pokeapiId()));
        }
        return spec;
    }

    /** Name, nickname or any localized name contains the text; % and _ typed by the user are matched literally. */
    private static Specification<PokemonEntity> anyNameContains(String text) {
        var pattern = "%" + escapeLikeWildcards(text) + "%";
        return (root, query, cb) -> {
            var localized = query.subquery(Integer.class);
            var correlated = localized.correlate(root);
            var names = correlated.<PokemonEntity, String, String>joinMap("localizedNames");
            localized.select(cb.literal(1)).where(cb.like(cb.lower(names.value()), pattern, ESCAPE));
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, ESCAPE),
                    cb.like(cb.lower(root.get("nickname")), pattern, ESCAPE),
                    cb.exists(localized));
        };
    }

    private static Specification<PokemonEntity> hasTag(String tag) {
        return (root, query, cb) -> cb.isMember(tag, root.get("tags"));
    }

    private static Specification<PokemonEntity> hasPokeapiId(int pokeapiId) {
        return (root, query, cb) -> cb.equal(root.get("pokeapiId"), pokeapiId);
    }

    static String escapeLikeWildcards(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

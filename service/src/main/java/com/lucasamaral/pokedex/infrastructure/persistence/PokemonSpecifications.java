package com.lucasamaral.pokedex.infrastructure.persistence;

import com.lucasamaral.pokedex.application.collection.CollectionFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the collection query from the filters that are present, so absent filters never reach SQL as nulls.
 */
final class PokemonSpecifications {

    private PokemonSpecifications() {
    }

    static Specification<PokemonEntity> matching(CollectionFilter filter) {
        Specification<PokemonEntity> spec = Specification.unrestricted();
        if (filter.name() != null) {
            spec = spec.and(nameOrNicknameContains(filter.name()));
        }
        if (filter.tag() != null) {
            spec = spec.and(hasTag(filter.tag()));
        }
        return spec;
    }

    private static Specification<PokemonEntity> nameOrNicknameContains(String text) {
        var pattern = "%" + text + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("nickname")), pattern));
    }

    private static Specification<PokemonEntity> hasTag(String tag) {
        return (root, query, cb) -> cb.isMember(tag, root.get("tags"));
    }
}

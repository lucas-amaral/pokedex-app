-- The local Pokémon collection (US03): source data copied from PokeAPI plus proprietary fields.

CREATE TABLE pokemon (
    id           UUID          PRIMARY KEY,
    pokeapi_id   INTEGER       NOT NULL CONSTRAINT uk_pokemon_pokeapi_id UNIQUE,
    -- Source data, refreshed on every sync
    name         VARCHAR(100)  NOT NULL,
    species_name VARCHAR(100)  NOT NULL,
    category     VARCHAR(100),
    description  VARCHAR(1000),
    sprite_url   VARCHAR(500),
    artwork_url  VARCHAR(500),
    height_dm    INTEGER       NOT NULL CHECK (height_dm >= 0),
    weight_hg    INTEGER       NOT NULL CHECK (weight_hg >= 0),
    -- Proprietary fields, never touched by a sync
    nickname     VARCHAR(30),
    notes        VARCHAR(500),
    region       VARCHAR(50),
    -- Bookkeeping
    synced_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version      BIGINT        NOT NULL
);

CREATE TABLE pokemon_type (
    pokemon_id UUID        NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    slot       INTEGER     NOT NULL,
    name       VARCHAR(30) NOT NULL,
    PRIMARY KEY (pokemon_id, slot)
);

CREATE TABLE pokemon_ability (
    pokemon_id UUID        NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    position   INTEGER     NOT NULL,
    name       VARCHAR(50) NOT NULL,
    hidden     BOOLEAN     NOT NULL,
    PRIMARY KEY (pokemon_id, position)
);

CREATE TABLE pokemon_stat (
    pokemon_id UUID        NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    position   INTEGER     NOT NULL,
    name       VARCHAR(30) NOT NULL,
    base_value INTEGER     NOT NULL CHECK (base_value BETWEEN 1 AND 255),
    PRIMARY KEY (pokemon_id, position)
);

CREATE TABLE pokemon_localized_name (
    pokemon_id UUID        NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    language   VARCHAR(16) NOT NULL,
    name       VARCHAR(50) NOT NULL,
    PRIMARY KEY (pokemon_id, language)
);

CREATE TABLE pokemon_tag (
    pokemon_id UUID        NOT NULL REFERENCES pokemon (id) ON DELETE CASCADE,
    tag        VARCHAR(30) NOT NULL,
    PRIMARY KEY (pokemon_id, tag)
);

CREATE INDEX ix_pokemon_tag_tag ON pokemon_tag (tag);

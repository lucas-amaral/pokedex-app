package com.lucasamaral.pokedex.domain.pokemon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EvolutionNodeTest {

    private static final EvolutionCondition LEVEL_16 = new EvolutionCondition("level-up", 16, null);
    private static final EvolutionCondition LEVEL_32 = new EvolutionCondition("level-up", 32, null);

    private final EvolutionNode bulbasaurLine = node(1, "bulbasaur", null,
            node(2, "ivysaur", LEVEL_16,
                    node(3, "venusaur", LEVEL_32)));

    private final EvolutionNode eeveeLine = node(133, "eevee", null,
            node(134, "vaporeon", new EvolutionCondition("use-item", null, "water-stone")),
            node(135, "jolteon", new EvolutionCondition("use-item", null, "thunder-stone")),
            node(196, "espeon", new EvolutionCondition("level-up", null, null)));

    @Test
    void listsEveryStageFromTheBaseForm() {
        assertThat(bulbasaurLine.names()).containsExactly("bulbasaur", "ivysaur", "venusaur");
    }

    @Test
    void listsBranchesInOrder() {
        assertThat(eeveeLine.names()).containsExactly("eevee", "vaporeon", "jolteon", "espeon");
        assertThat(eeveeLine.evolvesTo()).hasSize(3);
    }

    @Test
    void knowsWhetherItIsAFinalStage() {
        assertThat(bulbasaurLine.isFinalStage()).isFalse();
        assertThat(bulbasaurLine.evolvesTo().getFirst().evolvesTo().getFirst().isFinalStage()).isTrue();
    }

    @Test
    void theBaseFormHasNoEvolutionCondition() {
        assertThat(bulbasaurLine.condition()).isNull();
        assertThat(bulbasaurLine.evolvesTo().getFirst().condition()).isEqualTo(LEVEL_16);
    }

    @Test
    void isNotAffectedByChangesToTheOriginalChildList() {
        var children = new ArrayList<>(List.of(node(2, "ivysaur", LEVEL_16)));
        var root = new EvolutionNode(1, "bulbasaur", null, null, children);

        children.clear();

        assertThat(root.evolvesTo()).hasSize(1);
    }

    @Test
    void rejectsNonPositiveSpeciesId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> node(0, "bulbasaur", null))
                .withMessageContaining("id");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> node(1, "", null))
                .withMessageContaining("name");
    }

    private static EvolutionNode node(int id, String name, EvolutionCondition condition, EvolutionNode... evolvesTo) {
        return new EvolutionNode(id, name, null, condition, List.of(evolvesTo));
    }
}

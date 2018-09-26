package org.mage.test.cards.abilities.keywords;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Jump-start is found on instants and sorceries. You can cast a card with
 * jump-start from your graveyard by paying all its regular costs and one
 * additional cost: discarding a card from your hand.
 *
 * @author LevelX2
 */
public class JumpStartTest extends CardTestPlayerBase {

    @Test
    public void testNormalUse() {
        // Direct Current deals 2 damage to any target.
        // Jump-start
        addCard(Zone.HAND, playerA, "Direct Current", 1); // Sorcery {1}{R}{R}
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 6);
        addCard(Zone.HAND, playerA, "Disenchant", 1);

        addCard(Zone.BATTLEFIELD, playerB, "Silvercoat Lion", 1); // 2/2

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Direct Current", "Silvercoat Lion");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Direct Current with jump-start", playerB);

        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Silvercoat Lion", 1);
        assertHandCount(playerA, 0); // 1 from sacrificed Clue and 1 from draw of turn 3
        assertExileCount(playerA, "Direct Current", 1);

        assertLife(playerA, 20);
        assertLife(playerB, 18);

    }

    @Test
    public void testCastFromGraveyardCountered() {
        // Direct Current deals 2 damage to any target.
        // Jump-start
        addCard(Zone.HAND, playerA, "Direct Current", 1); // Sorcery {1}{R}{R}
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 6);
        addCard(Zone.HAND, playerA, "Disenchant", 1);

        addCard(Zone.BATTLEFIELD, playerB, "Silvercoat Lion", 1); // 2/2
        addCard(Zone.HAND, playerB, "Counterspell", 1);
        addCard(Zone.BATTLEFIELD, playerB, "Island", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Direct Current", "Silvercoat Lion");
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Direct Current with jump-start", playerB);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerB, "Counterspell", "Direct Current");

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Silvercoat Lion", 1);
        assertGraveyardCount(playerB, "Counterspell", 1);
        assertHandCount(playerA, 0); // 1 from sacrificed Clue and 1 from draw of turn 3
        assertGraveyardCount(playerA, "Direct Current", 0);
        assertExileCount(playerA, "Direct Current", 1);

        assertLife(playerA, 20);
        assertLife(playerB, 20);

    }
}

/*
 *  Copyright 2010 BetaSteward_at_googlemail.com. All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 * 
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 * 
 *  THIS SOFTWARE IS PROVIDED BY BetaSteward_at_googlemail.com ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BetaSteward_at_googlemail.com OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and should not be interpreted as representing official policies, either expressed
 *  or implied, of BetaSteward_at_googlemail.com.
 */

package mage.sets.magic2010;

import java.util.UUID;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Rarity;
import mage.abilities.effects.common.continious.GainAbilityTargetEffect;
import mage.abilities.effects.common.continious.GainControlTargetEffect;
import mage.abilities.effects.common.UntapTargetEffect;
import mage.abilities.keyword.HasteAbility;
import mage.cards.CardImpl;
import mage.target.common.TargetCreaturePermanent;

/**
 *
 * @author BetaSteward_at_googlemail.com
 */
public class ActOfTreason extends CardImpl<ActOfTreason> {

    public ActOfTreason(UUID ownerId) {
        super(ownerId, 124, "Act of Treason", Rarity.UNCOMMON, new CardType[]{CardType.SORCERY}, "{2}{R}");
        this.expansionSetCode = "M10";
        this.color.setRed(true);

        // Gain control of target creature until end of turn. Untap that creature.
        // It gains haste until end of turn. (It can attack and {T} this turn.)
        this.getSpellAbility().addTarget(new TargetCreaturePermanent(true));
        this.getSpellAbility().addEffect(new GainControlTargetEffect(Duration.EndOfTurn));
        this.getSpellAbility().addEffect(new UntapTargetEffect());
        this.getSpellAbility().addEffect(new GainAbilityTargetEffect(HasteAbility.getInstance(), Duration.EndOfTurn));

    }

    public ActOfTreason(final ActOfTreason card) {
        super(card);
    }

    @Override
    public ActOfTreason copy() {
        return new ActOfTreason(this);
    }

}

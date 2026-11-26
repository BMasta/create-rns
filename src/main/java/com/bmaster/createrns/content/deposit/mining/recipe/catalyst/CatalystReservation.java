package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import java.util.Collection;

public abstract class CatalystReservation {
    public abstract CatalystReservation copy();

    public abstract boolean consume(Collection<Catalyst> catalysts);
}

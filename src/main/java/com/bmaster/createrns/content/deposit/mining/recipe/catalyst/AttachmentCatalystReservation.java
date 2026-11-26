package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import java.util.Collection;

public class AttachmentCatalystReservation extends CatalystReservation {
    @Override
    public AttachmentCatalystReservation copy() {
        return new AttachmentCatalystReservation();
    }

    @Override
    public boolean consume(Collection<Catalyst> catalysts) {
        return true;
    }
}

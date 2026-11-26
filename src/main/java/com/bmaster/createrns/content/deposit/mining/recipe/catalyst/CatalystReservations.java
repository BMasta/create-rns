package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CatalystReservations {
    private final HashMap<CatalystRequirementType<?, ?>, CatalystReservation> reservationMap;

    public CatalystReservations() {
        reservationMap = CatalystRequirementType.BY_NAME.values().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        CatalystRequirementType::createReservation,
                        (a, b) -> a,
                        HashMap::new
                ));
    }

    private CatalystReservations(HashMap<CatalystRequirementType<?, ?>, CatalystReservation> reservationMap) {
        HashMap<CatalystRequirementType<?, ?>, CatalystReservation> resMapTmp = HashMap.newHashMap(reservationMap.size());
        for (var e : reservationMap.entrySet()) {
            resMapTmp.put(e.getKey(), e.getValue().copy());
        }
        this.reservationMap = resMapTmp;
    }

    @SuppressWarnings("unchecked")
    public <R extends CatalystReservation> R getForType(CatalystRequirementType<?, R> type) {
        return (R) reservationMap.get(type);
    }

    public CatalystReservations copy() {
        return new CatalystReservations(reservationMap);
    }

    public void update(CatalystReservations newReservations) {
        reservationMap.clear();
        for (var e : newReservations.reservationMap.entrySet()) {
            reservationMap.put(e.getKey(), e.getValue().copy());
        }
    }

    public boolean consume(Collection<Catalyst> catalysts) {
        for (var e : reservationMap.entrySet()) {
            if (!e.getValue().consume(catalysts)) return false;
        }
        return true;
    }
}

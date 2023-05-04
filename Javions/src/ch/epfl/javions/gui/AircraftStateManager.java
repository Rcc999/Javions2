package ch.epfl.javions.gui;

import ch.epfl.javions.adsb.AircraftStateAccumulator;
import ch.epfl.javions.adsb.Message;
import ch.epfl.javions.aircraft.AircraftDatabase;
import ch.epfl.javions.aircraft.IcaoAddress;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public final class AircraftStateManager {

    public static final long MINUTE_AGO_PURGE = 60 * 1_000_000_000L;
    private final Map<IcaoAddress, AircraftStateAccumulator<ObservableAircraftState>> associativeMap;
    private final ObservableSet<ObservableAircraftState> observableAircraftStates;
    private final ObservableSet<ObservableAircraftState> unmodifiableObservableAircraftStates;
    private final AircraftDatabase aircraftDatabase;
    private  long currentTimeStampNs = 0;


    public AircraftStateManager(AircraftDatabase aircraftDataBase) {
        this.aircraftDatabase = aircraftDataBase;
        this.associativeMap = new HashMap<>();
        this.observableAircraftStates=  FXCollections.observableSet();
        this.unmodifiableObservableAircraftStates = FXCollections.unmodifiableObservableSet(observableAircraftStates);
    }

    public void purge(){
        for (IcaoAddress icaoAddress : associativeMap.keySet()) {
            if(associativeMap.get(icaoAddress).stateSetter().getPosition() == null)
                continue;
            AircraftStateAccumulator<ObservableAircraftState> states = associativeMap.get(icaoAddress);
            ObservableAircraftState stateToRemove = states.stateSetter();
            if (currentTimeStampNs - stateToRemove.getLastMessageTimeStampNs() >= MINUTE_AGO_PURGE) {
                observableAircraftStates.remove(stateToRemove);
                associativeMap.remove(icaoAddress);
            }
        }
    }

    public void updateWithMessage(Message message) throws IOException {
        IcaoAddress key = message.icaoAddress();
        if (associativeMap.get(key) == null) {
            ObservableAircraftState newState = new ObservableAircraftState(key, aircraftDatabase.get(key));
            associativeMap.put(key, new AircraftStateAccumulator<>(newState));
        }
        associativeMap.get(key).update(message);

        if(associativeMap.get(key).stateSetter().getPosition() != null)
            observableAircraftStates.add(associativeMap.get(key).stateSetter());

        currentTimeStampNs = message.timeStampNs();
    }

    public ObservableSet<ObservableAircraftState> states() {
        return unmodifiableObservableAircraftStates;
    }

}

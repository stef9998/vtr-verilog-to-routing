import java.util.Objects;

/**
 * Representation for a 6T(ransistor)2R(esistor) memory cell.
 * Calculates the fault of the memory cell with given memristors.
 * 6T2R is two 4T1R cells chained, but the middle transistors used for both memristors.
 * @author Stefan Reichel
 */
public class MemCell6T2R implements MemCell{

    final private Resistor firstMemristor;
    final private Resistor secondMemristor;
    final private Fault memCellFault;

    public MemCell6T2R(FaultRates faultRates){
        firstMemristor = new Resistor(faultRates);
        secondMemristor = new Resistor(faultRates);
        memCellFault = Objects.requireNonNull(calcCellFault(), "Memristor has nondefined Fault-State");
    }

    private Fault calcCellFault(){
        if (firstMemristorContainsFault() || secondMemristorContainsFault()){
            switch (firstMemristorFault()) {
                case FF:
                    switch (secondMemristorFault()){
                        case FF:
                        case SA1:
                            return Fault.FF;
                        case SA0:
                        case UD:
                            return Fault.SA0;
                        default:
                            return null;
                    }
                case SA1:
                    return secondMemristorFault();
                case SA0:
                    return Fault.SA0;
                case UD:
                    switch (secondMemristorFault()){
                        case FF:
                        case SA0:
                            return Fault.SA0;
                        case SA1:
                        case UD:
                            return Fault.UD;
                        default:
                            return null;
                    }
                default:
                    return null;
            }
        }
        return Fault.FF;
    }

    @Override
    public Fault getCellFault() {
        return memCellFault;
    }

    private Resistor getFirstMemristor() { //TODO see if needed
        return firstMemristor;
    }

    private Resistor getSecondMemristor() { //TODO see if needed
        return secondMemristor;
    }

    public boolean firstMemristorContainsFault() {
        return firstMemristor.containsFault();
    }

    public boolean secondMemristorContainsFault() {
        return secondMemristor.containsFault();
    }

    public Fault firstMemristorFault(){
        return firstMemristor.getFault();
    }

    public Fault secondMemristorFault(){
        return secondMemristor.getFault();
    }
}

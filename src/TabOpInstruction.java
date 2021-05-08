import java.util.HashMap;

public class TabOpInstruction {
    private String mnemonic;
    private HashMap<String, Entry> entries;
    private String relativeForm;

    public TabOpInstruction(String mnemonic) {
        this.mnemonic = mnemonic;
        this.relativeForm = "";
        entries = new HashMap<>();
    }

    public String getRelativeForm() {
        return relativeForm;
    }

    public void setRelativeForm(String relativeForm) {
        this.relativeForm = relativeForm;
    }

    public void addEntry(String operand, String addressingMode, int length, String opCode) {
        entries.put(operand, new Entry(operand, addressingMode, length, opCode));
    }

    public Entry getEntryMatchingOperand(String operand) {
        return entries.get(operand);
    }

    class Entry {
        String operand;
        String addressingMode;
        int length;
        String opCode;

        public Entry(String operand, String addressingMode, int length, String opCode) {
            this.operand = operand;
            this.addressingMode = addressingMode;
            this.length = length;
            this.opCode = opCode;
        }
    }
}

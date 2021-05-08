import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Assembly {
    HashMap<String, TabOpInstruction> instructions;
    HashMap<String, String> tabsim;
    List<LSTInstruction> lstLines;
    List<Integer> branchInstructions;

    public Assembly() {
        this.instructions = new HashMap<>();
        this.tabsim = new HashMap<>();
        this.lstLines = new ArrayList<>();
        this.branchInstructions = new ArrayList<>();
    }

    public void LSTFirstPass(String instructionLine, FileWriter tabSimWriter) throws IOException {
        // Identify instruction fields
        Instruction instruction = identifyFields(instructionLine);
        LSTInstruction lstLine = new LSTInstruction();
        String opCode = "";
        String opCodeOperand = "";

        // Write to TABSIM if there is a label
        if (!instruction.label.equals("")) {
            writeToTabSim(instruction, tabSimWriter);
        }

        // Write contLoc and instruction to LST
        lstLine.contLoc = Directive.getContLoc();
        lstLine.instruction = instruction;

        TabOpInstruction tabOpInstruction = instructions.get(instruction.mnemonic);
        if (tabOpInstruction != null) {
            String operandForm = "";
            // Get operand form
            if (!tabOpInstruction.getRelativeForm().equals("")) {
                lstLine.relativeForm = tabOpInstruction.getRelativeForm();
                operandForm = tabOpInstruction.getRelativeForm();
                branchInstructions.add(lstLines.size());
            }
            else {
                String[] operands = instruction.operand.split(",");
                if (operands.length == 1 && instruction.mnemonic.equals("JMP")) {
                    operandForm = "opr16a";
                    branchInstructions.add(lstLines.size());
                }
                else {
                    opCodeOperand = operandToHexadecimal(instruction.operand);
                    if (opCodeOperand != null) {
                        operandForm = operandToOperandForm(instruction.operand);
                    }
                }
            }
            TabOpInstruction.Entry entry = tabOpInstruction.getEntryMatchingOperand(operandForm);

            if (entry != null) {
                opCode = entry.opCode + opCodeOperand;
                lstLine.opCode = opCode;
                lstLine.addressingMode = entry.addressingMode;
            }
            else {
                lstLine.instruction.comment += " ; Invalid instruction";
            }
        }
        else {
            if (Directive.isDirective(instruction.mnemonic)) {
                opCode = Directive.getOpCode(instruction);
                lstLine.opCode = opCode;
            }
            else {
                lstLine.instruction.comment += " ; Unknown instruction";
            }
        }
        lstLines.add(lstLine);

        // Get length of previous instruction and increase contLoc
        int instructionLength = opCode.length() / 2;
        Directive.increaseContLoc(instructionLength);
    }

    public void LSTSecondPass() {
        for (int i : branchInstructions) {
            LSTInstruction branchInstruction = lstLines.get(i);
            // Check if instruction is relative or JMP
            if (!branchInstruction.relativeForm.equals("")) {
                String firstPartOpCode;
                String finalOpCode = relativeOpCode(branchInstruction.relativeForm, branchInstruction.contLoc, branchInstruction.instruction.operand, branchInstruction.instruction.mnemonic);
                if (branchInstruction.relativeForm.equals("rel16")) {
                    firstPartOpCode = branchInstruction.opCode.substring(0, 4);
                }
                else {
                    // rel 8 and rel9
                    firstPartOpCode = branchInstruction.opCode.substring(0, 2);
                }
                branchInstruction.opCode = firstPartOpCode + finalOpCode;
            }
            else {
                // Instruction is JMP
                int operandValue = getOperandValue(branchInstruction.instruction.operand);
                // TODO: Is this check alright ? This probably has to be padded to four zeros regardless of number
                if (operandValue > 255) {
                    String finalOpCode = zeroPaddedHex(operandValue);
                    branchInstruction.opCode = branchInstruction.opCode.substring(0, 2) + finalOpCode;
                }
                else {
                    branchInstruction.opCode = "; Invalid branch";
                }
            }
        }
    }

    private String relativeOpCode(String relativeForm, String contLoc, String operand, String mnemonic) {
        // Get contLoc integer value
        int iContLoc = operandToInt("$" + contLoc);
        if (relativeForm.equals("rel8")) {
            iContLoc += 2;

            int iOperand = getOperandValue(operand);

            // Get subtraction value
            int subtraction = iOperand - iContLoc;

            // Validate subtraction range
            if (subtraction >= -128 && subtraction <= 127) {
                return signedZeroPaddedHex(subtraction, 8);
            }
        }
        else if (relativeForm.equals("rel16")) {
            iContLoc += 4;

            int iOperand = getOperandValue(operand);

            // Get subtraction value
            int subtraction = iOperand - iContLoc;

            // Validate subtraction range
            if (subtraction >= -32768 && subtraction <= 32767) {
                return signedZeroPaddedHex(subtraction, 16);
            }
        }
        else if (relativeForm.equals("abdxys,rel9")) {
            iContLoc += 3;

            String[] operands = operand.split(",");

            int iOperand = getOperandValue(operands[1]);

            // Get subtraction value
            int subtraction = iOperand - iContLoc;
            if (subtraction >= -256 && subtraction <= 255) {
                String lb = "";
                String rr = signedZeroPaddedHex(subtraction, 8);
                switch (operands[0]) {
                    case "A":
                        if (mnemonic.equals("IBEQ"))
                            lb = subtraction < 0 ? "90" : "80";
                        else if (mnemonic.equals("IBNE"))
                            lb = subtraction < 0 ? "B0" : "A0";
                        break;
                    case "B":
                        if (mnemonic.equals("IBEQ"))
                            lb = subtraction < 0 ? "91" : "81";
                        else if (mnemonic.equals("IBNE"))
                            lb = subtraction < 0 ? "B1" : "A1";
                        break;
                    case "D":
                        if (mnemonic.equals("IBEQ"))
                            lb = subtraction < 0 ? "94" : "84";
                        else if (mnemonic.equals("IBNE"))
                            lb = subtraction < 0 ? "B4" : "A4";
                        break;
                    case "X":
                        if (mnemonic.equals("IBEQ"))
                            lb = subtraction < 0 ? "95" : "85";
                        else if (mnemonic.equals("IBNE"))
                            lb = subtraction < 0 ? "B5" : "A5";
                        break;
                    case "Y":
                        if (mnemonic.equals("IBEQ"))
                            lb = subtraction < 0 ? "96" : "86";
                        else if (mnemonic.equals("IBNE"))
                            lb = subtraction < 0 ? "B6" : "A6";
                        break;
                    case "SP":
                        if (mnemonic.equals("IBEQ"))
                            lb = subtraction < 0 ? "97" : "87";
                        else if (mnemonic.equals("IBNE"))
                            lb = subtraction < 0 ? "B7" : "A7";
                        break;
                    default:
                        rr = " ; Invalid Register";
                }
                return lb + rr;
            }
        }
        return " ; Invalid branch";
    }

    private int getOperandValue(String operand) {
        int iOperand;
        String labelValue = tabsim.get(operand);
        if (labelValue != null)
            iOperand = operandToInt("$" + labelValue);
        else
            iOperand = operandToInt(operand);
        return iOperand;
    }

    public void writeLstToFile(FileWriter fileWriter) throws IOException {
        for (LSTInstruction i : lstLines) {
            fileWriter.write(i.toString() + "\n");
        }
    }

    private void writeToTabSim(Instruction instruction, FileWriter tabSimWriter) throws IOException {
        // Get label and labelValue
        String labelValue = Directive.getContLoc();
        if (instruction.mnemonic.equals("EQU")) {
            labelValue = operandToHexadecimal(instruction.operand);
        }
        String label = instruction.label.substring(0, instruction.label.length() - 1);

        // Write entry to tabsim in memory
        tabsim.put(label, labelValue);

        // Write entry to tabsim in file
        tabSimWriter.write(label + " " + labelValue + "\n");
    }

    private Instruction identifyFields(String instructionLine) {
        Instruction instruction = new Instruction();
        Scanner scanner = new Scanner(instructionLine);

        // Label and mnemonic
        String token = scanner.next();
        if (token.charAt(token.length() - 1) == ':') {
            instruction.label = token;
            token = scanner.next();
        }
        instruction.mnemonic = token;
        
        // Operand(s)
        if (scanner.hasNext()) {
            token = scanner.next();
            instruction.operand = token;
        }

        // Comment
        if (scanner.hasNext()) {
            token = scanner.nextLine().strip();
            instruction.comment = token;
        }

        return instruction;
    }

    public void readTabOp(File tableOp) {
        try (Scanner scanner = new Scanner(tableOp)) {
            // Read header
            scanner.nextLine();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                addInstruction(line);
            }
        } catch (FileNotFoundException e) {
            Main.printLine("File not found.");
        }
    }

    private void addInstruction(String entry) {
        // Read instruction data
        Scanner scanner = new Scanner(entry);
        String mnemonic = scanner.next();
        String operand = scanner.next();
        String addressingMode = scanner.next();
        int length = scanner.nextInt();
        String opCode = scanner.next();

        // Add entry
        if (!instructions.containsKey(mnemonic)) {
            instructions.put(mnemonic, new TabOpInstruction(mnemonic));
        }
        instructions.get(mnemonic).addEntry(operand, addressingMode, length, opCode);

        // Check if is relative
        if (addressingMode.equals("REL")) {
            instructions.get(mnemonic).setRelativeForm(operand);
        }
    }

    private String operandToOperandForm(String operand) {
        // No operand
        if (operand.equals("")) {
            return "none";
        }

        String[] operands = operand.split(",");

        if (operands.length == 1) {
            int numericValue = operandToInt(operand);

            // # Immediate modes
            if (operand.charAt(0) == '#') {
                if (numericValue > 255) {
                    return "#opr16i";
                } else {
                    return "#opr8i";
                }
            }

            // Last check, just a numeric value
            if (numericValue > 255) {
                return "opr16a";
            } else {
                return "opr8a";
            }
        }
        else {
            // There are two operands, Indexed mode
            return "oprx,xysp";
        }
    }

    private String operandToHexadecimal(String operand) {
        if (operand.equals("")) {
            return null;
        }

        String[] operands = operand.split(",");

        if (operands.length == 1) {
            int numericValue = operandToInt(operand);

            return zeroPaddedHex(numericValue);
        }
        else {
            // There are two operands, Indexed mode

            if (operand.charAt(0) == '[' && operand.charAt(operand.length() - 1) == ']') {
                String register = operands[1].substring(0, operands[1].length() - 1);
                // Formula 6
                if (operand.charAt(1) == 'D') {
                    return formula6(register);
                }
                else if (Character.isLetter(operand.charAt(1))) {
                    return null;
                }
                // 16 bit offset indexed indirect (Formula 3)
                else {
                    int constant = operandToInt(operands[0].substring(1));
                    return formula3(constant, register);
                }
            }
            else if (operands[0].matches("(A|B|D)")) {
                return formula5(operands[0], operands[1]);
            }
            else {
                int constant = operandToInt(operands[0]);
                String register = operands[1];

                // Formula 4
                if (register.contains("+") || register.contains("-")) {
                    return formula4(constant, register);
                }
                // Constant offset indexed (Formulas 1 and 2)
                else {
                    return constantOffsetIndexed(constant, register);
                }
            }
        }
    }

    // Formulas 1 and 2
    private String constantOffsetIndexed(int constant, String register) {
        String binaryConstant = Integer.toBinaryString(constant);
        String rr = getRegisterCode(register);

        if (rr == null)
            return null;

        // 5 bit
        if (constant >= -16 && constant <= 15) {
            String fiveBitConstant = ("00000" + binaryConstant).substring(binaryConstant.length());
            String xb = rr + "0" + fiveBitConstant;
            return xbToHexByte(xb);
        }
        else {
            String z;
            String s = constant < 0 ? "1" : "0";
            String operandOpCode = zeroPaddedHex(constant);

            // 9 bit
            if (constant >= -256 && constant <= 255) {
                z = "0";
            }
            // 16 bit
            else {
                z = "1";
            }

            String xb = "111" + rr + "0" + z + s;
            return xbToHexByte(xb) + operandOpCode;
        }
    }

    private String formula3(int constant, String register) {
        String rr = getRegisterCode(register);
        if (rr == null || constant < 0) {
            return null;
        }

        String xb = "111" + rr + "011";
        String eeff = signedZeroPaddedHex(constant, 16);
        return xbToHexByte(xb) + eeff;
    }

    private String formula4(int constant, String signedRegister) {
        char p;
        char sign;
        String register;
        if (signedRegister.charAt(0) == '+' || signedRegister.charAt(0) == '-') {
            sign = signedRegister.charAt(0);
            register = signedRegister.substring(1);
            p = '0';
        }
        else {
            sign = signedRegister.charAt(signedRegister.length() - 1);
            register = signedRegister.substring(0, signedRegister.length() - 1);
            p = '1';
        }

        String rr = getRegisterCode(register);
        if (rr == null || constant < 1 || constant > 8 || register.equals("PC")) {
            return null;
        }

        if (sign == '+') {
            constant--;
        }
        else {
            constant = -constant;
        }

        String binaryConstant = Integer.toBinaryString(constant);
        String fourBitConstant = ("0000" + binaryConstant).substring(binaryConstant.length());
        String xb = rr + "1" + p + fourBitConstant;
        return xbToHexByte(xb);
    }

    private String formula5(String reg1, String reg2) {
        String aa;
        switch (reg1) {
            case "A":
                aa = "00";
                break;
            case "B":
                aa = "01";
                break;
            case "D":
                aa = "10";
                break;
            default:
                aa = null;
        }

        String rr = getRegisterCode(reg2);
        if (aa == null || rr == null) {
            return null;
        }

        String xb = "111" + rr + "1" + aa;
        return xbToHexByte(xb);
    }

    private String formula6(String register) {
        String rr = getRegisterCode(register);
        if (rr == null)
            return null;

        String xb = "111" + rr + "111";
        return xbToHexByte(xb);
    }

    private String xbToHexByte(String xb) {
        int xbValue = Integer.parseUnsignedInt(xb, 2);
        return signedZeroPaddedHex(xbValue, 8);
    }

    private String getRegisterCode(String register) {
        String rr;
        switch (register) {
            case "X":
                rr = "00";
                break;
            case "Y":
                rr = "01";
                break;
            case "SP":
                rr = "10";
                break;
            case "PC":
                rr = "11";
                break;
            default:
                rr = null;
        }
        return rr;
    }

    public static int operandToInt(String operand) {
        // Make sure you only have the numeric string
        String realOperand;
        if (operand.charAt(0) == '#') {
            realOperand = operand.substring(1);
        }
        else {
            realOperand = operand;
        }

        // Make conversion and return the number
        if (realOperand.charAt(0) == '$') {
            return Integer.parseInt(realOperand.substring(1), 16);
        }
        else if (realOperand.charAt(0) == '@') {
            return Integer.parseInt(realOperand.substring(1), 8);
        }
        else if (realOperand.charAt(0) == '%') {
            return Integer.parseInt(realOperand.substring(1), 2);
        }
        else {
            return Integer.parseInt(realOperand);
        }
    }

    public static String zeroPaddedHex(int numericValue) {
        String hexValue = Integer.toHexString(numericValue).toUpperCase();
        if (numericValue > 255) {
            return ("0000" + hexValue).substring(hexValue.length());
        }
        else {
            return ("00" + hexValue).substring(hexValue.length());
        }
    }

    public static String signedZeroPaddedHex(int numericValue, int length) {
        int negativeValueOffset = length == 8 ? 6 : 4;
        if (numericValue < 0) {
            return Integer.toHexString(numericValue).toUpperCase().substring(negativeValueOffset);
        }
        else {
            String zeroPaddedHex = zeroPaddedHex(numericValue);
            if (length == 16 && zeroPaddedHex.length() == 2) {
                return "00" + zeroPaddedHex;
            }
            return zeroPaddedHex;
        }
    }

    class Instruction {
        String label;
        String mnemonic;
        String operand;
        String comment;

        public Instruction() {
            this.label = "";
            this.mnemonic = "";
            this.operand = "";
            this.comment = "";
        }

        @Override
        public String toString() {
            String rawInstruction = label + " " + mnemonic + " " + operand + " " + comment;
            return rawInstruction.strip();
        }
    }

    class LSTInstruction {
        String contLoc;
        Instruction instruction;
        String opCode;
        String addressingMode;
        String relativeForm;

        LSTInstruction() {
            this.contLoc = "";
            this.instruction = null;
            this.opCode = "";
            this.addressingMode = "";
            this.relativeForm = "";
        }

        @Override
        public String toString() {
            String rawInstruction = contLoc + " " + instruction.toString() + " " + addressingMode + " " + opCode;
            return rawInstruction.strip();
        }
    }
}

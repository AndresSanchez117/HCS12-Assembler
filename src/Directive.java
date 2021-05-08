public class Directive {
    private static int contLoc = 0;

    public static String getContLoc() {
        String cont = Assembly.zeroPaddedHex(contLoc);
        if (cont.length() == 2)
            cont = "00" + cont;
        return cont;
    }

    public static void increaseContLoc(int n) {
        contLoc += n;
    }

    public static boolean isDirective(String instruction) {
        String[] directives = {"ORG", "END", "START", "EQU", "BSZ", "FCB", "FCC", "FILL", "DC.B", "DC.W"};
        for (String directive : directives) {
            if (directive.equals(instruction)) {
                return true;
            }
        }
        return false;
    }

    public static String getOpCode(Assembly.Instruction instruction) {
        String opCode = "";
        switch (instruction.mnemonic) {
            case "ORG":
                contLoc = Assembly.operandToInt(instruction.operand);
                opCode = "";
                break;
            case "START":
                contLoc = 0;
                opCode = "";
                break;
            case "BSZ":
                int blockSize = Assembly.operandToInt(instruction.operand);
                for (int i = 0; i < blockSize; i++) {
                    opCode += "00";
                }
                break;
            case "DC.B": // TODO: Support text with DC.B in it's own case
            case "FCB":
                String[] fcbOperands = instruction.operand.split(",");
                for (int i = 0; i < fcbOperands.length; i++) {
                    int operandValue = Assembly.operandToInt(fcbOperands[i]);
                    String byteString = Assembly.zeroPaddedHex(operandValue);
                    opCode += byteString;
                }
                break;
            case "FCC":
                String asciiString = instruction.operand.substring(1, instruction.operand.length() - 1);
                for (int i = 0; i < asciiString.length(); i++) {
                    String hexValue = Assembly.zeroPaddedHex(asciiString.charAt(i));
                    opCode += hexValue;
                }
                break;
            case "FILL":
                String[] fillOperands = instruction.operand.split(",");
                int fillValue = Assembly.operandToInt(fillOperands[0]);
                int n = Assembly.operandToInt(fillOperands[1]);

                String fillString = Assembly.zeroPaddedHex(fillValue);
                for (int i = 0; i < n; i++) {
                    opCode += fillString;
                }
                break;
            case "DC.W":
                // TODO: Support text strings in DC.B and DC.W
                String[] dcWOperands = instruction.operand.split(",");
                for (String op : dcWOperands) {
                    int operandValue = Assembly.operandToInt(op);
                    String wordString = Assembly.signedZeroPaddedHex(operandValue, 16);
                    opCode += wordString;
                }
                break;
        }
        return opCode;
    }
}

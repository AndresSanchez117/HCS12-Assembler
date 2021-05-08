import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

// Sanchez Salcedo Andres
public class Main {

    public static void main(String[] args) throws IOException {
        Assembly assembly = new Assembly();

        // Read tabOp
        File tabOpFile = new File("/home/andres/Ing/5-SeminarioDeTraductores-I/Projects/TABOP.txt");
        assembly.readTabOp(tabOpFile);

        File inFile = new File("/home/andres/Ing/5-SeminarioDeTraductores-I/Projects/Practica_8/P8.asm");

        try (Scanner scanner = new Scanner(inFile)) {
            // Fist Pass
            FileWriter tabSimWriter = new FileWriter("TABSIM.txt");
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                assembly.LSTFirstPass(line, tabSimWriter);
                //lstWriter.write("\n");
            }
            tabSimWriter.close();

            // Second Pass
            assembly.LSTSecondPass();

            // Write LST to File
            FileWriter lstWriter = new FileWriter("P8.LST");
            assembly.writeLstToFile(lstWriter);
            lstWriter.close();
        } catch (FileNotFoundException e) {
           printLine("File not found: ");
        }
    }

    static void printLine(String s) {
        System.out.println(s);
    }
}

/**************************************************************************************************
 * Compilation: javac Assembler.java
 * Execution: java Assembler fileName
 * Dependencies: none
 * 
 * An assembler than converts assembly language programs to machine language for the Mano Basic Computer
 * (found in Morris Mano's Copmuter System Architecture). The file to be converted is specified as a command
 * line argument which then gets compiled to a.txt as output. This program implements a two pass assembler.
 * **************************************************************************************************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.Scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;

public class Assembler {

    // Symbols tables to store instruction opcodes and their machine code
    Set<String> pseudoInstructionSet;
    Map<String, String> memoryInstructionTable;
    Map<String, String> nonMemoryInstructionTable;
    Map<String, Integer> addressSymbolTable;

    // The final output as a list of strings
    List<String> outputCode;

    /**
     * The constructor that initializes all of the symbol tables and assembles the input program
     * specified as a list of strings into machine code output.
     *
     * @param program the program to assemble as a list of strings
     */
    public Assembler(List<String> program) {
        // initialize all of the tables
        pseudoInstructionSet = new HashSet<>();
        pseudoInstructionSet.add("ORG");
        pseudoInstructionSet.add("END");
        pseudoInstructionSet.add("HEX");
        pseudoInstructionSet.add("DEC");

        memoryInstructionTable = new HashMap<>();
        memoryInstructionTable.put("AND", "000");
        memoryInstructionTable.put("ADD", "001");
        memoryInstructionTable.put("LDA", "010");
        memoryInstructionTable.put("STA", "011");
        memoryInstructionTable.put("BUN", "100");
        memoryInstructionTable.put("BSA", "101");
        memoryInstructionTable.put("ISZ", "110");

        nonMemoryInstructionTable = new HashMap<>();
        nonMemoryInstructionTable.put("CLA", "0111100000000000");
        nonMemoryInstructionTable.put("CLE", "0111010000000000");
        nonMemoryInstructionTable.put("CMA", "0111001000000000");
        nonMemoryInstructionTable.put("CME", "0111000100000000");
        nonMemoryInstructionTable.put("CIR", "0111000010000000");
        nonMemoryInstructionTable.put("CIL", "0111000001000000");
        nonMemoryInstructionTable.put("INC", "0111000000100000");
        nonMemoryInstructionTable.put("SPA", "0111000000010000");
        nonMemoryInstructionTable.put("SNA", "0111000000001000");
        nonMemoryInstructionTable.put("SZA", "0111000000000100");
        nonMemoryInstructionTable.put("SZE", "0111000000000010");
        nonMemoryInstructionTable.put("HLT", "0111000000000001");
        nonMemoryInstructionTable.put("INP", "1111100000000000");
        nonMemoryInstructionTable.put("OUT", "1111010000000000");
        nonMemoryInstructionTable.put("SKI", "1111001000000000");
        nonMemoryInstructionTable.put("SKO", "1111000100000000");
        nonMemoryInstructionTable.put("ION", "1111000010000000");
        nonMemoryInstructionTable.put("IOF", "1111000001000000");

        addressSymbolTable = new HashMap<>();
        outputCode = new ArrayList<>();

        // assemble the program in two passes, output is impicitly stored in outputCode
        firstPass(program);
        secondPass(program);
    }

    /**
     * Get the assembled output as a list of strings
     * @return the assembled machine code
     */
    public List<String> getOutput() {
        return outputCode;
    }

    // run the first pass of the assembler
    // associate all of the labels with their memory addresses
    private void firstPass(List<String> program) {
        int locationCounter = 0;

        for (String instruction : program) {
            if (hasLabel(instruction)) {
                addressSymbolTable.put(getLabel(instruction), locationCounter);
                locationCounter++;
            } else {
                String operation = getOperation(instruction);
                if (operation.equals("ORG")) {
                    locationCounter = Integer.parseInt(getOrgOperand(instruction));
                } else if (operation.equals("END")) {
                    break;
                } else {
                    locationCounter++;
                }
            }
        }
    }

    // does this instruction contain a label as its address?
    private boolean hasLabel(String instruction) {
        instruction = instruction.trim(); 

        // the labels are required to be of length 3 followed by a comma
        if (instruction.length() < 3 || instruction.length() == 3 || instruction.charAt(3) != ',') {
            return false;
        }

        return true;
    }

    // assumes a label exists
    private String getLabel(String instruction) {
        return instruction.substring(0, 3);
    }

    // assumes instruction has no label (operations are of length 3)
    private String getOperation(String instruction) {
        instruction = instruction.trim();
        return instruction.substring(0, 3);
    }

    // assumes instruction is ORG (as only called in that case)
    // all parts divided by a space
    private String getOrgOperand(String instruction) {
        String[] instructionInParts = instruction.split(" ");
        return instructionInParts[1];
    }

    // run the second pass of the two pass assembler
    // write the machine code to outputCode
    private void secondPass(List<String> program) {
        int locationCounter = 0;

        for (String instruction : program) {
            // -1 denotes that END instruction has been encountered
            if (locationCounter == -1) return;

            // disect the string into different tokens
            Map<String, String> instructionTokens = disectInstruction(instruction, locationCounter);

            // translate the instruction and write to output, simultaneously updating locationCounter
            locationCounter = translateInstruction(instructionTokens, locationCounter);
        }
    }

    // translate instructions and write to output
    // returns the location counter for the next instruction
    private int translateInstruction(Map<String, String> instructionTokens, int locationCounter) {
        
        String type = instructionTokens.get("type");

        switch (type) {
            case "MRI": //MRI -> Memory Reference Insructions
                return translateMRI(instructionTokens, locationCounter);

            case "NON_MRI": // NON_MRI -> Register, IO and other instructions
                return translateNonMRI(instructionTokens, locationCounter);

            case "PSEUDO":  // Assembler directives (pseudo instructions)
                return translatePseudo(instructionTokens, locationCounter);

            default:    // unreachable as program is assumed to be valid
                return -1;
        }

    }

    // translate MRI Instructions
    // return address of next location
    private int translateMRI(Map<String, String> instructionTokens, int locationCounter) {
        StringBuilder machineCode = new StringBuilder();

        // add to machineCode part by part
        machineCode.append(instructionTokens.get("location") + " ");
        machineCode.append(instructionTokens.get("addressingMode"));
        machineCode.append(memoryInstructionTable.get(instructionTokens.get("operation")));
        machineCode.append(getBinaryString(
                addressSymbolTable.get(instructionTokens.get("operand")), 12));

        // write to output
        outputCode.add(machineCode.toString());

        // the next instruction is at the next location
        return locationCounter + 1;
    }

    // translate non MRI instructions
    // return address of next location
    private int translateNonMRI(Map<String, String> instructionTokens, int locationCounter) {
        StringBuilder machineCode = new StringBuilder();

        // add to machineCode part by part
        machineCode.append(instructionTokens.get("location") + " ");
        machineCode.append(nonMemoryInstructionTable.get(instructionTokens.get("operation")));

        // write to output
        outputCode.add(machineCode.toString());

        // the next instruction is at the next location
        return locationCounter + 1;
    }

    // translate pseudo instructions
    // update locationCounter in case of ORG and END
    // return address of next location
    private int translatePseudo(Map<String, String> instructionTokens, int locationCounter) {
        String operation = instructionTokens.get("operation");

        if (operation.equals("ORG")) {
            return Integer.parseInt(instructionTokens.get("operand"));
        } else if (operation.equals("END")) {
            return -1;
        } else if (operation.equals("DEC")) {
            String machineCode = instructionTokens.get("location") + " " +
                    getBinaryString(Integer.parseInt(instructionTokens.get("operand")), 16);
            outputCode.add(machineCode.toString());
            return locationCounter + 1;
        } else if (operation.equals("HEX")) {
            String machineCode = instructionTokens.get("location") + " " +
                    getBinaryString(Integer.parseInt(instructionTokens.get("operand"), 16), 16);
            outputCode.add(machineCode.toString());
            return locationCounter + 1;
        } else {
            // should not happen
            throw new RuntimeException("Invalid instruction");
        }
    }

    // disect the instruction into parts
    // return the instruction tokens as a Map with these fields and corresponding values
    // valid fields are
    // a.) location -> the memory address the instruction will be at
    // b.) operation -> the operation part of the instruction
    // c.) type -> if the instruction is MRI, NON_MRI or PSEUDO
    // d.) operand -> the operand of the instruction (if applicable)
    // e.) addressingMode -> the addressingMode of the instruction (direct - 0, indirect - 1)
    private Map<String, String> disectInstruction(String instruction, int locationCounter) {
        Map<String, String> disectedInstruction = new HashMap<>();

        instruction = instruction.trim(); 

        if (instruction.length() < 3) {
            new RuntimeException("Invalid Instruction");
        }

        // the location of the instruction in memory
        if (instruction.length() == 3 || instruction.charAt(3) != ',') {
            disectedInstruction.put("location", Integer.toBinaryString(locationCounter));
        } else {
            String label = instruction.substring(0, 3);
            disectedInstruction.put("location", Integer.toBinaryString(addressSymbolTable.get(label)));
            instruction = instruction.substring(5, instruction.length());
        }

        // split the instruction about the spaces
        String[] instructionParts = instruction.split(" ");

        // the operation (op-code) of the instruction
        String opcode = instructionParts[0];
        disectedInstruction.put("operation", opcode);

        // the type of the instruction
        if (memoryInstructionTable.containsKey(opcode)) {
            disectedInstruction.put("type", "MRI");
        } else if (nonMemoryInstructionTable.containsKey(opcode)) {
            disectedInstruction.put("type", "NON_MRI");
        } else if (pseudoInstructionSet.contains(opcode)) {
            disectedInstruction.put("type", "PSEUDO");
        } else {
            throw new RuntimeException("Invalid Instruction at opcode : " + opcode);
        }

        // check if there is an operand and if there is store it
        if (instructionParts.length > 1) {
            if (!instructionParts[1].startsWith("/")) {
                disectedInstruction.put("operand", instructionParts[1]);
            }
        }

        // check if Indirect memory addressing mode is specified
        if (instructionParts.length > 2) {
            if (!instructionParts[1].startsWith("/") && !instructionParts[2].startsWith("/")) {
                if (instructionParts[2].equals("I")) {
                    disectedInstruction.put("addressingMode", "1");
                } else {
                    disectedInstruction.put("addressingMode", "0");    
                }
            } else {
                disectedInstruction.put("addressingMode", "0");
            }
        } else {
            disectedInstruction.put("addressingMode", "0");
        }

        return disectedInstruction;
    }

    // get a (fixed length) binary string for the specified number
    // params: decimal -> the number to convert
    // length -> the length of the binary string
    private String getBinaryString(int decimal, int length) {
        // bitwise or (|) with (1 << length) to get binary string of length - (length + 1)
        // the suffix is the required binary string
        if (decimal >= 0)
            return Integer.toBinaryString(1 << length | decimal).substring(1);

        // in case of negative numbers the bainry string will be of 32 bits, we need only the
        // last (length) bits
        return Integer.toBinaryString(decimal).substring(32 - length, 32);
    }

    /**
     * The main method that is the entry point of execution. It reads a file as a command line argument
     * and then assembles it using an Assembler object. It then packages the output to a txt file for viewing.
     * 
     * @param args The command line arguments. args[0] should specify the file to be compiled.
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

        // read file
        String directory = System.getProperty("user.dir");
        File file = new File(directory + File.separator + args[0]);
    
        Scanner in = new Scanner(file);
       
        List<String> program = new ArrayList<>();
        while (in.hasNext()) {
            program.add(in.nextLine());
        }

        // pass into an assembler object
        Assembler assembler = new Assembler(program);

        // get the output from the assembler object
        List<String> output = assembler.getOutput();

        // package into a file
        PrintWriter writer = new PrintWriter("a.txt", "UTF-8");
        for (String instruction : output) {
            writer.println(instruction);
        }   
        writer.close();
    }
}
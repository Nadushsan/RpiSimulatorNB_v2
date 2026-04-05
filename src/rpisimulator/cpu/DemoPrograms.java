package rpisimulator.cpu;

import java.util.ArrayList;
import java.util.List;

/**
 * Programmes ARM de démonstration prêts à exécuter
 */
public class DemoPrograms {

    public static class Step {
        private final String instruction;
        private final Runnable action;
        private final String description;

        public Step(String instruction, Runnable action, String description) {
            this.instruction = instruction;
            this.action = action;
            this.description = description;
        }

        public String instruction()   { return instruction; }
        public Runnable action()      { return action; }
        public String description()   { return description; }
    }

    /** Programme 1 : Addition simple */
    public static List<Step> addition(CPU cpu) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("MOV R0, #10",   () -> cpu.MOV(0, 10),       "Charge 10 dans R0"));
        steps.add(new Step("MOV R1, #25",   () -> cpu.MOV(1, 25),       "Charge 25 dans R1"));
        steps.add(new Step("ADD R2, R0, R1",() -> cpu.ADD(2, 0, 1),     "R2 = R0 + R1 = 35"));
        steps.add(new Step("STR R2, [0]",   () -> cpu.STR(2, 0),        "Sauvegarde R2 en mémoire"));
        return steps;
    }

    /** Programme 2 : Opérations logiques */
    public static List<Step> logique(CPU cpu) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("MOV R0, #0xFF", () -> cpu.MOV(0, 0xFF),     "Charge 0xFF dans R0"));
        steps.add(new Step("MOV R1, #0x0F", () -> cpu.MOV(1, 0x0F),     "Charge 0x0F dans R1"));
        steps.add(new Step("AND R2, R0, R1",() -> cpu.AND(2, 0, 1),     "R2 = R0 AND R1 = 0x0F"));
        steps.add(new Step("ORR R3, R0, R1",() -> cpu.ORR(3, 0, 1),     "R3 = R0 OR  R1 = 0xFF"));
        steps.add(new Step("EOR R4, R0, R1",() -> cpu.EOR(4, 0, 1),     "R4 = R0 XOR R1 = 0xF0"));
        return steps;
    }

    /** Programme 3 : Décalages de bits */
    public static List<Step> decalages(CPU cpu) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("MOV R0, #1",    () -> cpu.MOV(0, 1),        "Charge 1 dans R0"));
        steps.add(new Step("LSL R1, R0, #4",() -> cpu.LSL(1, 0, 4),     "R1 = R0 << 4 = 0x10"));
        steps.add(new Step("LSL R2, R0, #8",() -> cpu.LSL(2, 0, 8),     "R2 = R0 << 8 = 0x100"));
        steps.add(new Step("LSR R3, R2, #4",() -> cpu.LSR(3, 2, 4),     "R3 = R2 >> 4 = 0x10"));
        steps.add(new Step("MOV R0, #0xFF", () -> cpu.MOV(0, 0xFF),     "Recharge 0xFF"));
        steps.add(new Step("LSR R4, R0, #4",() -> cpu.LSR(4, 0, 4),     "R4 = R0 >> 4 = 0x0F"));
        return steps;
    }

    /** Programme 4 : Multiplication et CMP */
    public static List<Step> multiplication(CPU cpu) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("MOV R0, #6",    () -> cpu.MOV(0, 6),        "Charge 6 dans R0"));
        steps.add(new Step("MOV R1, #7",    () -> cpu.MOV(1, 7),        "Charge 7 dans R1"));
        steps.add(new Step("MUL R2, R0, R1",() -> cpu.MUL(2, 0, 1),     "R2 = R0 * R1 = 42"));
        steps.add(new Step("MOV R3, #42",   () -> cpu.MOV(3, 42),       "Charge 42 dans R3"));
        steps.add(new Step("CMP R2, R3",    () -> cpu.CMP(2, 3),        "Compare R2 et R3 → Z=1"));
        steps.add(new Step("SUB R4, R2, R3",() -> cpu.SUB(4, 2, 3),     "R4 = R2 - R3 = 0"));
        return steps;
    }

    /** Programme 5 : Appel de fonction (BL / BX LR) */
    public static List<Step> appelFonction(CPU cpu) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("MOV R0, #5",    () -> cpu.MOV(0, 5),        "Argument : R0 = 5"));
        steps.add(new Step("MOV R1, #3",    () -> cpu.MOV(1, 3),        "Argument : R1 = 3"));
        steps.add(new Step("BL 0x100",      () -> cpu.BL(0x100),        "Appel fonction @ 0x100, LR sauvé"));
        steps.add(new Step("ADD R2,R0,R1",  () -> cpu.ADD(2, 0, 1),     "Corps fonction: R2 = R0+R1"));
        steps.add(new Step("BX LR",         () -> cpu.BX_LR(),          "Retour : PC = LR"));
        return steps;
    }
}

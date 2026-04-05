package rpisimulator.cpu;

/**
 * Simulateur de registres ARM Cortex-A (Raspberry Pi)
 * Registres : R0-R12, SP (R13), LR (R14), PC (R15)
 * Flags CPSR : N, Z, C, V
 */
public class CPU {

    // Registres généraux R0-R12
    private int[] r = new int[13];

    // Registres spéciaux
    private int sp;   // R13 - Stack Pointer
    private int lr;   // R14 - Link Register
    private int pc;   // R15 - Program Counter

    // Flags CPSR (Current Program Status Register)
    private boolean flagN; // Negative
    private boolean flagZ; // Zero
    private boolean flagC; // Carry
    private boolean flagV; // Overflow

    // Mémoire simulée (1 KB)
    private int[] memory = new int[256];

    // Dernière adresse mémoire écrite (pour surbrillance UI)
    private int lastWrittenAddr = -1;

    // Historique des instructions exécutées
    private StringBuilder log = new StringBuilder();

    public CPU() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < 13; i++) r[i] = 0;
        sp = 0x1000; // Stack commence à 0x1000
        lr = 0;
        pc = 0;
        flagN = false;
        flagZ = false;
        flagC = false;
        flagV = false;
        log = new StringBuilder();
        lastWrittenAddr = -1;
        log.append("[CPU] Réinitialisation — ARM Cortex-A\n");
    }

    // ─── Instructions ARM basiques ───────────────────────────────────────────

    /** MOV Rd, #imm  → Rd = imm */
    public void MOV(int rd, int imm) {
        checkReg(rd);
        setReg(rd, imm);
        updateFlagsNZ(imm);
        pc += 4;
        log.append(String.format("MOV R%d, #0x%X  → R%d = 0x%X\n", rd, imm, rd, imm));
    }

    /** ADD Rd, Rn, Rm  → Rd = Rn + Rm */
    public void ADD(int rd, int rn, int rm) {
        checkReg(rd); checkReg(rn); checkReg(rm);
        long result = (long) getReg(rn) + (long) getReg(rm);
        setReg(rd, (int) result);
        updateFlagsADD(getReg(rn), getReg(rm), (int) result);
        pc += 4;
        log.append(String.format("ADD R%d, R%d, R%d  → R%d = 0x%X\n", rd, rn, rm, rd, getReg(rd)));
    }

    /** SUB Rd, Rn, Rm  → Rd = Rn - Rm */
    public void SUB(int rd, int rn, int rm) {
        checkReg(rd); checkReg(rn); checkReg(rm);
        int result = getReg(rn) - getReg(rm);
        setReg(rd, result);
        updateFlagsSUB(getReg(rn), getReg(rm), result);
        pc += 4;
        log.append(String.format("SUB R%d, R%d, R%d  → R%d = 0x%X\n", rd, rn, rm, rd, getReg(rd)));
    }

    /** MUL Rd, Rn, Rm  → Rd = Rn * Rm */
    public void MUL(int rd, int rn, int rm) {
        checkReg(rd); checkReg(rn); checkReg(rm);
        int result = getReg(rn) * getReg(rm);
        setReg(rd, result);
        updateFlagsNZ(result);
        pc += 4;
        log.append(String.format("MUL R%d, R%d, R%d  → R%d = 0x%X\n", rd, rn, rm, rd, getReg(rd)));
    }

    /** AND Rd, Rn, Rm  → Rd = Rn & Rm */
    public void AND(int rd, int rn, int rm) {
        checkReg(rd); checkReg(rn); checkReg(rm);
        int result = getReg(rn) & getReg(rm);
        setReg(rd, result);
        updateFlagsNZ(result);
        pc += 4;
        log.append(String.format("AND R%d, R%d, R%d  → R%d = 0x%X\n", rd, rn, rm, rd, getReg(rd)));
    }

    /** ORR Rd, Rn, Rm  → Rd = Rn | Rm */
    public void ORR(int rd, int rn, int rm) {
        checkReg(rd); checkReg(rn); checkReg(rm);
        int result = getReg(rn) | getReg(rm);
        setReg(rd, result);
        updateFlagsNZ(result);
        pc += 4;
        log.append(String.format("ORR R%d, R%d, R%d  → R%d = 0x%X\n", rd, rn, rm, rd, getReg(rd)));
    }

    /** EOR Rd, Rn, Rm  → Rd = Rn ^ Rm (XOR) */
    public void EOR(int rd, int rn, int rm) {
        checkReg(rd); checkReg(rn); checkReg(rm);
        int result = getReg(rn) ^ getReg(rm);
        setReg(rd, result);
        updateFlagsNZ(result);
        pc += 4;
        log.append(String.format("EOR R%d, R%d, R%d  → R%d = 0x%X\n", rd, rn, rm, rd, getReg(rd)));
    }

    /** LSL Rd, Rn, #shamt  → Rd = Rn << shamt */
    public void LSL(int rd, int rn, int shamt) {
        checkReg(rd); checkReg(rn);
        int result = getReg(rn) << shamt;
        setReg(rd, result);
        updateFlagsNZ(result);
        pc += 4;
        log.append(String.format("LSL R%d, R%d, #%d  → R%d = 0x%X\n", rd, rn, shamt, rd, getReg(rd)));
    }

    /** LSR Rd, Rn, #shamt  → Rd = Rn >>> shamt */
    public void LSR(int rd, int rn, int shamt) {
        checkReg(rd); checkReg(rn);
        int result = getReg(rn) >>> shamt;
        setReg(rd, result);
        updateFlagsNZ(result);
        pc += 4;
        log.append(String.format("LSR R%d, R%d, #%d  → R%d = 0x%X\n", rd, rn, shamt, rd, getReg(rd)));
    }

    /** CMP Rn, Rm  → Met à jour les flags sans stocker le résultat */
    public void CMP(int rn, int rm) {
        checkReg(rn); checkReg(rm);
        int result = getReg(rn) - getReg(rm);
        updateFlagsSUB(getReg(rn), getReg(rm), result);
        pc += 4;
        log.append(String.format("CMP R%d, R%d  → flags: N=%b Z=%b C=%b V=%b\n",
                rn, rm, flagN, flagZ, flagC, flagV));
    }

    /** STR Rd, [addr]  → memory[addr] = Rd */
    public void STR(int rd, int addr) {
        checkReg(rd);
        if (addr >= 0 && addr < memory.length) {
            memory[addr] = getReg(rd);
            lastWrittenAddr = addr;
            log.append(String.format("STR R%d, [0x%X]  → mem[0x%X] = 0x%X\n", rd, addr, addr, getReg(rd)));
        } else {
            lastWrittenAddr = -1;
            log.append(String.format("STR ERREUR: adresse 0x%X hors limites\n", addr));
        }
        pc += 4;
    }

    /** LDR Rd, [addr]  → Rd = memory[addr] */
    public void LDR(int rd, int addr) {
        checkReg(rd);
        if (addr >= 0 && addr < memory.length) {
            setReg(rd, memory[addr]);
            log.append(String.format("LDR R%d, [0x%X]  → R%d = 0x%X\n", rd, addr, rd, getReg(rd)));
        } else {
            log.append(String.format("LDR ERREUR: adresse 0x%X hors limites\n", addr));
        }
        pc += 4;
    }

    /** BL label  → LR = PC+4, PC = label */
    public void BL(int targetPc) {
        lr = pc + 4;
        pc = targetPc;
        log.append(String.format("BL 0x%X  → LR = 0x%X, PC = 0x%X\n", targetPc, lr, pc));
    }

    /** BX LR  → PC = LR (retour de fonction) */
    public void BX_LR() {
        pc = lr;
        log.append(String.format("BX LR  → PC = 0x%X\n", pc));
    }

    // ─── Mise à jour des flags ────────────────────────────────────────────────

    private void updateFlagsNZ(int result) {
        flagN = (result < 0);
        flagZ = (result == 0);
    }

    private void updateFlagsADD(int a, int b, int result) {
        flagN = (result < 0);
        flagZ = (result == 0);
        flagC = (((long) a & 0xFFFFFFFFL) + ((long) b & 0xFFFFFFFFL)) > 0xFFFFFFFFL;
        flagV = ((a > 0 && b > 0 && result < 0) || (a < 0 && b < 0 && result > 0));
    }

    private void updateFlagsSUB(int a, int b, int result) {
        flagN = (result < 0);
        flagZ = (result == 0);
        flagC = (((long) a & 0xFFFFFFFFL) >= ((long) b & 0xFFFFFFFFL));
        flagV = ((a > 0 && b < 0 && result < 0) || (a < 0 && b > 0 && result > 0));
    }

    private void checkReg(int r) {
        if (r < 0 || r > 12) throw new IllegalArgumentException("Registre invalide: R" + r);
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int getReg(int index) {
        if (index < 0 || index > 12) throw new IllegalArgumentException("R" + index);
        return r[index];
    }

    public void setReg(int index, int value) {
        if (index < 0 || index > 12) throw new IllegalArgumentException("R" + index);
        r[index] = value;
    }

    public int getSP()  { return sp; }
    public int getLR()  { return lr; }
    public int getPC()  { return pc; }
    public void setSP(int v) { sp = v; }
    public void setLR(int v) { lr = v; }
    public void setPC(int v) { pc = v; }

    public boolean isFlagN() { return flagN; }
    public boolean isFlagZ() { return flagZ; }
    public boolean isFlagC() { return flagC; }
    public boolean isFlagV() { return flagV; }

    public int getCPSR() {
        return (flagN ? 1 << 31 : 0) | (flagZ ? 1 << 30 : 0)
             | (flagC ? 1 << 29 : 0) | (flagV ? 1 << 28 : 0);
    }

    public String getLog() { return log.toString(); }
    public void clearLog() { log = new StringBuilder(); }

    // ─── Accès mémoire ───────────────────────────────────────────────────────

    public int getLastWrittenAddr() { return lastWrittenAddr; }
    public void clearLastWritten() { lastWrittenAddr = -1; }
    public int getMemorySize() { return memory.length; }
    public int getMemoryAt(int addr) {
        if (addr >= 0 && addr < memory.length) return memory[addr];
        return 0;
    }
    public int[] getMemoryCopy() { return memory.clone(); }
    public void resetMemory() {
        for (int i = 0; i < memory.length; i++) memory[i] = 0;
    }
}

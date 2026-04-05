# ARM Cortex-A Register Simulator — Raspberry Pi
**Projet NetBeans | JavaFX | Systèmes Embarqués**

---

## Description

Simulateur de registres ARM Cortex-A (architecture de la Raspberry Pi 4).
L'application reproduit le fonctionnement interne du processeur : registres,
flags CPSR et cycle Fetch-Decode-Execute, avec interface graphique JavaFX dark theme.

---

## Architecture du projet

```
RpiSimulator/
├── src/rpisimulator/
│   ├── cpu/
│   │   ├── CPU.java            ← Cœur : registres ARM + instructions
│   │   └── DemoPrograms.java   ← Programmes de démonstration
│   └── ui/
│       ├── MainApp.java        ← Interface JavaFX principale
│       └── style.css           ← Thème dark GitHub-inspired
└── README.md
```

---

## Registres simulés

| Registre | Alias  | Rôle                              |
|----------|--------|-----------------------------------|
| R0–R12   | —      | Registres généraux                |
| R13      | SP     | Stack Pointer (pile = 0x1000)     |
| R14      | LR     | Link Register (adresse de retour) |
| R15      | PC     | Program Counter (+4 par instruction)|
| CPSR     | —      | Flags N, Z, C, V                  |

---

## Instructions ARM implémentées

| Instruction         | Opération                  |
|---------------------|----------------------------|
| `MOV Rd, #imm`      | Rd = imm                   |
| `ADD Rd, Rn, Rm`    | Rd = Rn + Rm               |
| `SUB Rd, Rn, Rm`    | Rd = Rn - Rm               |
| `MUL Rd, Rn, Rm`    | Rd = Rn × Rm               |
| `AND Rd, Rn, Rm`    | Rd = Rn & Rm               |
| `ORR Rd, Rn, Rm`    | Rd = Rn \| Rm              |
| `EOR Rd, Rn, Rm`    | Rd = Rn ^ Rm (XOR)        |
| `LSL Rd, Rn, #sh`   | Rd = Rn << sh              |
| `LSR Rd, Rn, #sh`   | Rd = Rn >>> sh             |
| `CMP Rn, Rm`        | Met à jour flags (Rn - Rm) |
| `STR Rd, [addr]`    | mem[addr] = Rd             |
| `LDR Rd, [addr]`    | Rd = mem[addr]             |
| `BL addr`           | LR = PC+4, PC = addr       |
| `BX LR`             | PC = LR (retour fonction)  |

---

## Flags CPSR

| Flag | Nom      | Activé quand                     |
|------|----------|----------------------------------|
| N    | Negative | Résultat négatif (bit 31 = 1)   |
| Z    | Zero     | Résultat = 0                     |
| C    | Carry    | Débordement non signé            |
| V    | Overflow | Débordement signé                |

---

## Programmes de démonstration

1. **Addition simple** — MOV, ADD, STR
2. **Opérations logiques** — AND, ORR, EOR
3. **Décalages de bits** — LSL, LSR
4. **Multiplication & CMP** — MUL, CMP, SUB
5. **Appel de fonction** — BL, BX LR (sauvegarde LR)

---

## Comment ouvrir dans NetBeans

1. File → Open Project → sélectionner le dossier `RpiSimulator`
2. Ajouter JavaFX au module path (SDK JavaFX 17+)
3. Dans Project Properties → Run → VM Options :
   ```
   --module-path /chemin/vers/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```
4. Main class : `rpisimulator.ui.MainApp`
5. Run (F6)

---

## Prérequis

- Java JDK 17+
- JavaFX SDK 17+ (https://openjfx.io)
- NetBeans 17+

---

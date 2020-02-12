package emulator;

import asm.AlphaQAsm;
import asm.Native;
import asm.parsing.CodeException;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	private static final Object runLock = new Object();
	private static final Set<String> validOnStopCommands = Set.of(
			"pi", "px", "ps", "pb", "pu",
			"d", "df",
			"sep", "clear", "cmd");
	private static HashMap<String, Integer> symbolTable = new LinkedHashMap<>();
	private static Set<Breakpoint> breakpoints = new HashSet<>();
	private static Emulator emulator = new Emulator();
	private static volatile boolean running = false;
	private static Thread runningThread;
	private static byte[] buffer = new byte[131072];
	private static short input = 0;
	private static Collection<String[]> onStopCommands = new ArrayList<>();
	private static Pattern symbolPattern;
	private static JFrame keyboardInput;
	private static boolean enableTerminal = false;
	private static short[] keyPressed = new short[] {0};
	private static short emulatorPreviousOut = 0;

	public static int getInput() {
		if (keyboardInput.isVisible()) {
			input = 0;
			synchronized (keyPressed) {
				return keyPressed[0];
			}
		}
		return input;
	}

	public static String getAddressString(int address) {
		String symbol = findSymbolByAddress(address);
		return symbol == null ? String.format("%04x", address) : String.format("%s(%04x)", symbol, address);
	}

	public static String findSymbolByAddress(int address) {
		for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
			if (address == entry.getValue())
				return entry.getKey();
		}

		return null;
	}

	private static Breakpoint checkBreakpoints() {
		for (Breakpoint breakpoint : breakpoints) {
			if (breakpoint.name.equals("on-out")) {
				if (emulator.outputTripped)
					return breakpoint;
			} else if (breakpoint.name.equals("@sout")) {
				if (emulator.previousInstruction != null && emulator.previousInstruction.getOpcode() == 0x06 &&
						emulator.previousInstruction.getArg1() == 0x07 && emulator.previousInstructionWasTaken &&
						Short.toUnsignedInt(emulator.sp) < Short.toUnsignedInt(breakpoint.value)) {
					return breakpoint;
				}
			} else if (emulator.get(breakpoint.name) == breakpoint.value) {
				return breakpoint;
			}
		}

		return null;
	}

	private static void runAsync() {
		while (true) {
			try {
				try {
					synchronized (runLock) {
						runLock.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				do {
					emulatorPreviousOut = emulator.out;
					emulator.executeInstruction();


					if (enableTerminal) {
						if (emulatorPreviousOut == 0 && emulator.out != 0) {
							System.out.print((char) emulator.out);
						}
					}

					if (emulator.error) {
						System.err.printf("Error: Invalid instruction at %04x\nPrevious instruction -> %04x: %s\n",
								emulator.ip, emulator.previousInstruction.address, emulator.previousInstruction.toString());
						running = false;
					}

					Breakpoint breakpoint = checkBreakpoints();
					if (breakpoint != null) {
						running = false;

						System.out.printf("Breakpoint reached:\n%s\n", breakpoint.toString());
					}
				} while (running);

				for (String[] commands : onStopCommands) {
					executeCommand(commands, null);
				}

				synchronized (runLock) {
					runLock.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static String formatShortX(short value) {
		return String.format("%04x", Short.toUnsignedInt(value));
	}

	private static String formatShortI(short value) {
		return String.format("%6d", value);
	}

	private static String formatShortU(short value) {
		return String.format("%5d", Short.toUnsignedInt(value));
	}

	private static String formatShortB(short value) {
		return String.format("%16s", Integer.toBinaryString(Short.toUnsignedInt(value))).replace(' ', '0');
	}

	private static int findSymbol(String find) {
		return symbolTable.getOrDefault(find, -1);

	}

	private static void loadRam(String fullName) {
		File file = new File(fullName);
		if (!file.exists()) {
			System.err.println(fullName + " doesn't exist");
			return;
		} else if (file.isDirectory()) {
			System.err.println(fullName + " is a directory");
			return;
		} else if (file.length() > 65536 * 2) {
			System.err.println("File too large");
			return;
		}

		try (FileInputStream in = new FileInputStream(file)) {
			int bytesRead;
			bytesRead = in.read(buffer, 0, buffer.length);
			Arrays.fill(emulator.memory, (short) 0);

			for (int i = 0; i < bytesRead; i += 2) {
				emulator.memory[i >> 1] = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << Byte.SIZE));
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static void loadSymbols(String fullName, Scanner scanner) {
		File file = new File(fullName);
		if (!file.exists()) {
			System.err.println(fullName + " doesn't exist");
			return;
		} else if (file.isDirectory()) {
			System.err.println(fullName + " is a directory");
			return;
		}

		if (symbolTable.size() > 0) {
			System.out.print("Clear existing symbols? (Y/N) ");
			String line = scanner.nextLine().trim().toLowerCase();

			if (line.equals("y"))
				symbolTable.clear();
			else if (!line.equals("n")) {
				System.err.println("Unexpected response");
				return;
			}
		}

		try (Scanner fileScanner = new Scanner(new File(fullName))) {

			while (fileScanner.hasNextLine()) {
				String symbolEntry = fileScanner.nextLine().trim();
				Matcher symbol = symbolPattern.matcher(symbolEntry);

				if (symbol.matches()) {
					symbolTable.put(symbol.group(1), Integer.valueOf(symbol.group(2)));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static boolean executeCommand(String[] parts, Scanner scanner) {
		if (parts[0].equals("exit")) {
			return true;
		} else if (parts[0].equals("pause")) {
			if (running) {
				running = false;
				synchronized (runLock) {
					try {
						runLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else {
				System.err.println("Not currently running");
			}
			return false;
		} else if (parts[0].equals("kb")) {
			keyboardInput.setVisible(!keyboardInput.isVisible());
			return false;
		} else if (parts[0].equals("term")) {
			enableTerminal = !enableTerminal;
			return false;
		} else if (parts[0].equals("cmd")) {
			String[] cmd = new String[parts.length + 1];
			cmd[0] = "cmd";
			cmd[1] = "/c";

			for (int i = 1; i < parts.length; i++) {
				cmd[i + 1] = parts[1];
			}

			try {
				new ProcessBuilder().command(cmd).inheritIO().start().waitFor();
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}

			return false;
		}


		if (!running) {
			if (parts[0].equals("load-all")) {
				if (parts.length < 2) {
					System.err.println("Expected file name");
					return false;
				}

				StringBuilder fullNameBuilder = new StringBuilder(parts[1]);
				for (int i = 2; i < parts.length; i++) {
					fullNameBuilder.append(" ").append(parts[i]);
				}
				String fullName = fullNameBuilder.toString();

				loadRam(fullName);
				loadSymbols(fullName + ".symbols", scanner);
			} else if (parts[0].equals("load-ram")) {
				if (parts.length < 2) {
					System.err.println("Expected file name");
					return false;
				}

				StringBuilder fullNameBuilder = new StringBuilder(parts[1]);
				for (int i = 2; i < parts.length; i++) {
					fullNameBuilder.append(" ").append(parts[i]);
				}
				String fullName = fullNameBuilder.toString();

				loadRam(fullName);

				return false;
			} else if (parts[0].equals("load-symbols")) {
				if (parts.length < 2) {
					System.out.println("Expected symbol file");
				}

				StringBuilder fullNameBuilder = new StringBuilder(parts[1]);
				for (int i = 2; i < parts.length; i++) {
					fullNameBuilder.append(" ").append(parts[i]);
				}
				String fullName = fullNameBuilder.toString();

				loadSymbols(fullName, scanner);

				return false;
			} else if (parts[0].equals("clr-symbols")) {
				symbolTable.clear();
			} else if (parts[0].equals("load-asm")) {
				if (parts.length < 2) {
					System.err.println("Expected file name");
					return false;
				}

				StringBuilder fullNameBuilder = new StringBuilder(parts[1]);
				for (int i = 2; i < parts.length; i++) {
					fullNameBuilder.append(" ").append(parts[i]);
				}
				String fullName = fullNameBuilder.toString();

				File file = new File(fullName);
				if (!file.exists()) {
					System.err.println(fullName + " doesn't exist");
					return false;
				} else if (file.isDirectory()) {
					System.err.println(fullName + " is a directory");
					return false;
				}

				try {
					AlphaQAsm assembler = new AlphaQAsm(file);

					emulator.memory = assembler.getProgram();
					symbolTable = assembler.getSymbolTable();
				} catch (IOException e) {
					System.err.println("Failed to read file");
					e.printStackTrace();
				} catch (CodeException e) {
					System.err.println("Failed to parse file");
					e.printStackTrace();
				}

				return false;
			} else if (parts[0].equals("clear")) {
				System.out.print("\u001b[2J\u001b[H");
			} else if (parts[0].equals("set")) {
				if (parts.length < 3) {
					System.err.println("Expected variable and value");
				}

				short value;

				int symbol = findSymbol(parts[2]);
				if (symbol != -1) {
					value = (short) symbol;
				} else {


					if (!parts[2].matches("[0-9A-Fa-f]+")) {
						System.err.println("Expected value");
						return false;
					}

					try {
						value = (short) Integer.parseInt(parts[2], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected number for value");
						return false;
					}
				}

				if (parts[1].equals("a")) {
					emulator.a = value;
				} else if (parts[1].equals("b")) {
					emulator.b = value;
				} else if (parts[1].equals("c")) {
					emulator.c = value;
				} else if (parts[1].equals("d")) {
					emulator.d = value;
				} else if (parts[1].equals("e")) {
					emulator.e = value;
				} else if (parts[1].equals("fp")) {
					emulator.fp = value;
				} else if (parts[1].equals("sp")) {
					emulator.sp = value;
				} else if (parts[1].equals("ip")) {
					emulator.ip = value;
				} else if (parts[1].equals("in")) {
					if (keyboardInput.isVisible()) {
						System.err.println("Input already in use by keyboard");
						return false;
					}

					input = value;
				} else if (parts[1].equals("tpat")) {
					emulator.tpat = value;
				} else if (parts[1].equals("fpat")) {
					emulator.fpat = value;
				} else if (parts[1].equals("flags")) {
					emulator.zf = (value & 1) != 0;
					emulator.sf = (value & 2) != 0;
					emulator.cf = (value & 4) != 0;
					emulator.of = (value & 8) != 0;
				} else if (parts[1].matches("&[0-9A-Fa-f]+")) {
					int address = Integer.parseInt(parts[1].substring(1), 16);

					if (address < 0 || address >= 65536) {
						System.err.println("Address out of range");
						return false;
					}

					emulator.memory[address] = value;
				} else if (parts[1].startsWith("&")) {
					int address = findSymbol(parts[1].substring(1));

					if (address == -1) {
						System.err.println("Symbol not found");
						return false;
					}

					emulator.memory[address] = value;
				} else {
					System.err.println("Unknown value to set");
				}
			} else if (parts[0].equals("bp")) {
				if (parts.length < 2) {
					System.err.println("Expected breakpoint location and value");
					return false;
				}

				if (parts[1].equals("on-out")) {
					breakpoints.add(new Breakpoint(parts[1], (short) 0));
					return false;
				}

				if (parts.length < 3) {
					System.err.println("Expected breakpoint location and value");
					return false;
				}

				short value;

				int symbol = findSymbol(parts[2]);
				if (symbol != -1) {
					value = (short) symbol;
				} else {


					if (!parts[2].matches("[0-9A-Fa-f]+")) {
						System.err.println("Expected value");
						return false;
					}

					try {
						value = (short) Integer.parseInt(parts[2], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected number for value");
						return false;
					}
				}

				if (parts[1].matches("[abcde]") || parts[1].matches("[fsi]p") || parts[1].matches("[zsco]f") || parts[1].equals("flags") || parts[1].equals("out")) {
					breakpoints.add(new Breakpoint(parts[1], value));
				} else if (parts[1].matches("&[0-9A-Fa-f]+")) {
					try {
						int address = Integer.parseInt(parts[1].substring(1), 16);

						if (address < 0 || address >= 65536) {
							System.err.println("Address out of range");
							return false;
						}


						breakpoints.add(new Breakpoint(String.format("&%04x", address), value));
					} catch (NumberFormatException e) {
						System.err.println("Expected address to be number");
						return false;
					}
				} else if (parts[1].startsWith("&")) {
					int address = findSymbol(parts[1].substring(1));

					if (address == -1) {
						System.err.println("Symbol not found");
						return false;
					}

					breakpoints.add(new Breakpoint(String.format("&%04x", address), value));
				} else {
					System.err.println("Invalid break point name");
				}
			} else if (parts[0].equals("-bp")) {
				if (parts.length < 2) {
					System.err.println("Expected breakpoint location and value");
					return false;
				}

				if (parts[1].equals("on-out")) {
					breakpoints.remove(new Breakpoint(parts[1], (short) 0));
					return false;
				}

				if (parts.length < 3) {
					System.err.println("Expected breakpoint location and value");
					return false;
				}

				short value;

				int symbol = findSymbol(parts[2]);
				if (symbol != -1) {
					value = (short) symbol;
				} else {


					if (!parts[2].matches("[0-9A-Fa-f]+")) {
						System.err.println("Expected value");
						return false;
					}

					try {
						value = (short) Integer.parseInt(parts[2], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected number for value");
						return false;
					}
				}

				if (parts[1].matches("[abcde]") || parts[1].matches("[fsi]p") || parts[1].matches("[zsco]f") || parts[1].equals("flags") || parts[1].equals("out")) {
					breakpoints.remove(new Breakpoint(parts[1], value));
				} else if (parts[1].matches("&[0-9A-Fa-f]+")) {
					try {
						int address = Integer.parseInt(parts[1].substring(1), 16);

						if (address < 0 || address >= 65536) {
							System.err.println("Address out of range");
							return false;
						}

						breakpoints.remove(new Breakpoint(String.format("%04x", address), value));
					} catch (NumberFormatException e) {
						System.err.println("Expected address to be number");
						return false;
					}
				} else if (parts[1].startsWith("&")) {
					int address = findSymbol(parts[1].substring(1));

					if (address == -1) {
						System.err.println("Symbol not found");
						return false;
					}

					breakpoints.remove(new Breakpoint(String.format("%04x", address), value));
				} else {
					System.err.println("Invalid break point name");
				}
			} else if (parts[0].equals("clr-bp")) {
				breakpoints.clear();
			} else if (parts[0].equals("d")) {
				if (parts.length < 3) {
					System.err.println("Expected number of instructions and address");
					return false;
				}

				int address;

				if (parts[1].matches("[0-9A-Fa-f]+")) {
					try {
						address = Integer.parseInt(parts[1], 16);

						if (address < 0 || address >= 65535) {
							System.err.println("Address out of range");
							return false;
						}
					} catch (NumberFormatException e) {
						System.err.println("Expected address");
						return false;
					}
				} else if (parts[1].equals("ip")) {
					address = emulator.ip;
				} else if (parts[1].equals("prev")) {
					address = Short.toUnsignedInt(emulator.previousInstruction.address);
				} else {
					address = findSymbol(parts[1]);
					if (address == -1) {
						System.err.println("Symbol " + parts[1] + " not found");
						return false;
					}
				}

				if (parts[2].matches("[0-9A-Fa-f]+")) {
					int ip = address;

					int count;
					try {
						count = Integer.parseInt(parts[2], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected number for intsruction count");
						return false;
					}

					for (int i = 0; i < count && ip < 65536; i++) {
						Instruction instruction = emulator.instructionAt(ip);
						String symbol = findSymbolByAddress(ip);

						if (symbol != null) {
							System.out.printf("%04x: %s%s; (%s)\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction, symbol);
						} else {
							System.out.printf("%04x: %s%s\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction);
						}
						ip += instruction.length();
					}
				} else {
					System.err.println("Expected number of instructions to print");
				}
			} else if (parts[0].equals("df")) {
				if (parts.length < 2) {
					parts = new String[] {"df", "ip"};
				}

				int length = 65536;

				if (parts.length == 3) {
					try {
						length = Integer.parseInt(parts[2], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected maximum length");
						return false;
					}
				}

				int address;

				if (parts[1].matches("[0-9A-Fa-f]+")) {
					try {
						address = Integer.parseInt(parts[1], 16);

						if (address < 0 || address >= 65535) {
							System.err.println("Address out of range");
							return false;
						}
					} catch (NumberFormatException e) {
						System.err.println("Expected address");
						return false;
					}
				} else if (parts[1].equals("ip")) {
					address = emulator.ip;
				} else if (parts[1].equals("prev")) {
					address = Short.toUnsignedInt(emulator.previousInstruction.address);
				} else {
					address = findSymbol(parts[1]);
					if (address == -1) {
						System.err.println("Symbol " + parts[1] + " not found");
						return false;
					}
				}

				SortedMap<Short, Instruction> insns = new TreeMap<>(Comparator.comparingInt(Short::toUnsignedInt));
				Set<Short> leaders = new HashSet<>();

				Stack<Short> addressesToSearch = new Stack<>();

				addressesToSearch.push((short) address);

				while (!addressesToSearch.isEmpty()) {
					int ip = Short.toUnsignedInt(addressesToSearch.pop());
					if (insns.containsKey((short) ip)) continue;


					Instruction insn = emulator.instructionAt(ip);
					insns.put((short) ip, insn);

					int nextIp = ip + insn.length();

					if (insn.getOpcode() == 0x01 && insn.getArg1() == 7) {
						if (insn.getArg2() >= 4) {
							int target = 0x1234;

							switch (insn.getArg2()) {
								case 4:
									target = 0;
									break;
								case 5:
									target = 1;
									break;
								case 6:
									target = 0xFFFF;
									break;
								case 7:
									target = insn.getImmediate();
									break;
							}

							addressesToSearch.push((short) target);
							leaders.add((short) target);
							if (insn.getCondition() != 0) {
								addressesToSearch.push((short) nextIp);
								leaders.add((short) nextIp);
							}
						} else {
							addressesToSearch.push((short) nextIp);
						}
					} else if (insn.getOpcode() == 0x06 && insn.getArg1() == 7) {
						if (insn.getCondition() != 0) {
							addressesToSearch.push((short) nextIp);
							leaders.add((short) nextIp);
						}
					} else if (insn.getOpcode() == 0x38 && insn.getArg1() == 7) {
						if (insn.getArg2() >= 4) {
							int target = 0x1234;

							switch (insn.getArg2()) {
								case 4:
									target = nextIp;
									break;
								case 5:
									target = nextIp + 1;
									break;
								case 6:
									target = nextIp + 0xFFFF;
									break;
								case 7:
									target = nextIp + insn.getImmediate();
									break;
							}

							addressesToSearch.push((short) target);
							leaders.add((short) target);
							if (insn.getCondition() != 0) {
								addressesToSearch.push((short) nextIp);
								leaders.add((short) nextIp);
							}
						} else {
							addressesToSearch.push((short) nextIp);
						}
					} else if (insn.getOpcode() == 0x3A && insn.getArg1() == 7) {
						if (insn.getArg2() >= 4) {
							int target = 0x1234;

							switch (insn.getArg2()) {
								case 4:
									target = nextIp;
									break;
								case 5:
									target = nextIp - 1;
									break;
								case 6:
									target = nextIp - 0xFFFF;
									break;
								case 7:
									target = nextIp - insn.getImmediate();
									break;
							}


							addressesToSearch.push((short) target);
							leaders.add((short) target);
							if (insn.getCondition() != 0) {
								addressesToSearch.push((short) nextIp);
								leaders.add((short) nextIp);
							}
						} else {
							addressesToSearch.push((short) nextIp);
						}
					} else if (!insn.isInvalid()) {
						addressesToSearch.push((short) nextIp);
					}
				}

				Instruction prev = null;

				var it = insns.entrySet().iterator();
				var insn = it.next();

				for (int i = 0; i < length; i++) {
					if (prev != null && insn.getKey() != prev.address + prev.length()) {
						System.out.printf("[%04x - %04x]\n", Short.toUnsignedInt((short) (prev.address + prev.length())), Short.toUnsignedInt((short) (insn.getKey() - 1)));
					}

					int ip = Short.toUnsignedInt(insn.getKey());
					Instruction instruction = insn.getValue();
					String symbol = findSymbolByAddress(ip);

					if (it.hasNext()) {
						var next = it.next();
						if (leaders.contains(insn.getKey()))
							System.out.println();

						if (next.getKey() == (short) (address + 1) && instruction.getArg2() == 7) {

							if (symbol != null) {
								System.out.printf("%04x: %s%s; (%s)    ", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction, symbol);
							} else {
								System.out.printf("%04x: %s%s    ", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction);
							}

							ip = Short.toUnsignedInt(next.getKey());
							instruction = next.getValue();
							symbol = findSymbolByAddress(ip);

							if (symbol != null) {
								System.out.printf("%04x: %s%s; (%s)\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction, symbol);
							} else {
								System.out.printf("%04x: %s%s\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction);
							}

							prev = next.getValue();
							if (it.hasNext()) insn = it.next();
						} else {
							if (symbol != null) {
								System.out.printf("%04x: %s%s; (%s)\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction, symbol);
							} else {
								System.out.printf("%04x: %s%s\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction);
							}

							prev = insn.getValue();
							insn = next;
						}
					} else {
						if (leaders.contains(insn.getKey()))
							System.out.println();

						if (symbol != null) {
							System.out.printf("%04x: %s%s; (%s)\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction, symbol);
						} else {
							System.out.printf("%04x: %s%s\n", ip, ip == Short.toUnsignedInt(emulator.ip) ? ">" : "", instruction);
						}

						break;
					}

				}
			} else if (parts[0].equals("dall")) {
				if (parts.length != 2) {
					System.out.println("Expected file name");
				}

				var out = System.out;

				try (PrintStream ps = new PrintStream(new File(parts[1]))) {
					System.setOut(ps);

					for (String symbol : symbolTable.keySet()) {
						if (!symbol.startsWith("@")) {
							executeCommand(new String[] {"df", symbol}, scanner);
							System.out.println();
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} finally {
					System.setOut(out);
				}
			} else if (parts[0].equals("pi")) {
				print(parts, Main::formatShortI);
			} else if (parts[0].equals("px")) {
				print(parts, Main::formatShortX);
			} else if (parts[0].equals("pu")) {
				print(parts, Main::formatShortU);
			} else if (parts[0].equals("pb")) {
				print(parts, Main::formatShortB);
			} else if (parts[0].equals("ps")) {
				if (parts.length != 2) {
					System.err.println("Expected address");
					return false;
				}

				int address = findSymbol(parts[1]);
				if (address == -1) {
					if (parts[1].matches("[0-9A-Fa-f]+")) {
						try {
							address = Integer.parseInt(parts[1], 16);
						} catch (NumberFormatException e) {
							System.err.println("Expected number for address");
							return false;
						}
					} else {
						System.err.println("Symbol not found");
						return false;
					}
				}

				if (address < 0 || address >= 65536) {
					System.err.println("Address out of range");
					return false;
				}

				while (address < 65536 && emulator.memory[address] != 0) {
					System.out.print((char) emulator.memory[address]);
					address++;
				}
			} else if (parts[0].equals("s")) {
				synchronized (runLock) {
					runLock.notify();
					try {
						runLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else if (parts[0].equals("so")) {
				Breakpoint bp = new Breakpoint("ip", (short) (emulator.ip + emulator.instructionAt(emulator.ip).length()));

				boolean needToRemove = breakpoints.add(bp);
				running = true;

				synchronized (runLock) {
					runLock.notify();
					try {
						runLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (needToRemove) breakpoints.remove(bp);
			} else if (parts[0].equals("rt")) {
				if (parts.length < 2) {
					System.err.println("Expected address to run to");
					return false;
				}

				int address = findSymbol(parts[1]);

				if (address == -1) {
					try {
						address = Integer.parseUnsignedInt(parts[1], 16);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}

				if (address < 0 || address >= 65536) {
					System.err.println("Address out of range");
					return false;
				}

				Breakpoint bp = new Breakpoint("ip", (short) address);

				boolean needToRemove = breakpoints.add(bp);
				running = true;

				synchronized (runLock) {
					runLock.notify();
					try {
						runLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (needToRemove) breakpoints.remove(bp);
			} else if (parts[0].equals("sout")) {
				Breakpoint bp = new Breakpoint("@sout", emulator.sp);

				boolean needToRemove = breakpoints.add(bp);
				running = true;

				synchronized (runLock) {
					runLock.notify();
					try {
						runLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (needToRemove) breakpoints.remove(bp);
			} else if (parts[0].equals("run")) {
				running = true;

				synchronized (runLock) {
					runLock.notify();
				}
			} else if (parts[0].equals("symbols")) {
				for (Map.Entry<String, Integer> symbol : symbolTable.entrySet()) {
					System.out.printf("%s: %04x; %s\n", symbol.getKey(), symbol.getValue(), emulator.instructionAt(symbol.getValue()));
				}
			} else if (parts[0].equals("symbol")) {
				if (parts.length < 2) {
					System.out.println("Symbol name expected");
					return false;
				}

				Integer address = symbolTable.get(parts[1]);

				if (address == null)
					System.out.println("Symbol not found");
				else
					System.out.printf("%04x; %s\n", address, emulator.instructionAt(address));

			} else if (parts[0].equals("find-symbol")) {
				if (parts.length < 2) {
					System.out.println("Symbol address expected");
					return false;
				}

				Integer num;
				try {
					num = Integer.valueOf(parts[1], 16);
				} catch (NumberFormatException e) {
					System.out.println("Expected an address");
					return false;
				}

				String symbol = findSymbolByAddress(num);

				System.out.println(symbol == null ? "Not found" : symbol);
			} else if (parts[0].equals("symbol-before")) {

				if (parts.length < 2) {
					System.out.println("Symbol address expected");
					return false;
				}

				Integer num;
				try {
					num = Integer.valueOf(parts[1], 16);
				} catch (NumberFormatException e) {
					System.out.println("Expected an address");
					return false;
				}

				String symbolBefore = null;
				int symbolBeforeAddress = Integer.MIN_VALUE;

				for (Map.Entry<String, Integer> symbol : symbolTable.entrySet()) {
					if (symbol.getValue() < num && num > symbolBeforeAddress) {
						symbolBefore = symbol.getKey();
						symbolBeforeAddress = symbol.getValue();
					}
				}

				if (symbolBefore == null)
					System.out.println("Not found");
				else
					System.out.printf("%s: %04x; %s\n", symbolBefore, symbolBeforeAddress, emulator.instructionAt(symbolBeforeAddress));
			} else if (parts[0].equals("reset")) {
				emulator.resetState();
				emulatorPreviousOut = 0;
				input = 0;
			} else if (parts[0].equals("clr-ram")) {
				emulator.resetMemory();
			} else if (parts[0].equals("on-stop-default")) {
				onStopCommands.clear();

				onStopCommands.add(new String[] {"sep"});
				onStopCommands.add(new String[] {"px", "stack"});
				onStopCommands.add(new String[] {"sep"});
				onStopCommands.add(new String[] {"d", "prev", "1"});
				onStopCommands.add(new String[] {"df", "ip", "10"});
				onStopCommands.add(new String[] {"sep"});
				onStopCommands.add(new String[] {"px", "regs"});
				onStopCommands.add(new String[] {"sep"});
			} else if (parts[0].equals("on-stop")) {
				onStopCommands.clear();

				while (true) {
					String _command = scanner.nextLine().trim().replaceAll("\\s+", " ");
					String[] _parts = _command.split(" ");

					if (_parts[0].equals("done"))
						break;
					else if (validOnStopCommands.contains(_parts[0]))
						onStopCommands.add(_parts);
					else
						System.err.println("Cannot execute this command on stop");
				}
			} else if (parts[0].equals("clr-on-stop")) {
				onStopCommands.clear();
			} else if (parts[0].equals("sep")) {
				System.out.println("------------------------------------------------");
			} else if (parts[0].equals("cycles")) {
				System.out.println(emulator.cycleCount);
			} else {
				System.err.println("Unknown command");
			}
		} else {
			System.err.println("This command cannot be executed while the emulator is running");
		}

		return false;
	}

	public static void main(String[] args) {
		System.loadLibrary("EPODWin");
		Native.SetConsoleMode(-11, Native.GetConsoleMode(-11) | 4);
		symbolPattern = Pattern.compile("^([^:]+)\\s*:\\s*(\\d+)$");
		keyboardInput = new JFrame("Keyboard");
		keyboardInput.addKeyListener(new Keyboard(keyPressed));
		keyboardInput.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		emulator.input = Main::getInput;

		runningThread = new Thread(Main::runAsync);
		runningThread.setDaemon(true);
		runningThread.start();

		while (true) {
			Scanner scanner = new Scanner(System.in);

			String command = scanner.nextLine().trim();
			command.replaceAll("\\s+", " ");

			String[] parts = command.split(" ");

			if (executeCommand(parts, scanner))
				break;
		}

		keyboardInput.dispose();
	}

	private static void print(String[] parts, PrintFormat numberFormat) {
		if (parts.length < 2) {
			System.out.println("Expected print command");
			return;
		}

		if (parts[1].equals("regs")) {
			System.out.printf(
					"| a: %s |  b: %s |  c: %s |  d: %s |\n" +
							"| e: %s | fp: %s | sp: %s | ip: %s |\n",
					numberFormat.formatShort(emulator.a), numberFormat.formatShort(emulator.b), numberFormat.formatShort(emulator.c), numberFormat.formatShort(emulator.d),
					numberFormat.formatShort(emulator.e), numberFormat.formatShort(emulator.fp), numberFormat.formatShort(emulator.sp), numberFormat.formatShort(emulator.ip));
		} else if (parts[1].equals("stack")) {
			if (Short.toUnsignedInt(emulator.fp) >= Short.toUnsignedInt(emulator.sp)) {
				System.out.println("No stack frame found");
				return;
			}


			for (int i = Short.toUnsignedInt(emulator.fp); i < Short.toUnsignedInt(emulator.sp); i++) {
				System.out.printf("%04x: %s\n", i, numberFormat.formatShort(emulator.memory[i]));
			}

		} else if (parts[1].equals("state")) {
			System.out.printf(
					"tpat: %s fpat: %s out: %s\n" +
							"zf: %d sf: %d cf: %d of: %d\n",
					numberFormat.formatShort(emulator.tpat), numberFormat.formatShort(emulator.fpat), numberFormat.formatShort(emulator.out),
					emulator.zf ? 1 : 0, emulator.sf ? 1 : 0, emulator.cf ? 1 : 0, emulator.of ? 1 : 0);
		} else if (parts[1].equals("ram")) {
			int length;
			int address;

			if (parts.length == 3) {
				length = 1;
			} else if (parts.length >= 4) {
				if (parts[3].matches("[0-9A-Fa-f]+")) {
					try {
						length = Integer.parseInt(parts[3], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected number for length");
						return;
					}
				} else {
					System.err.println("Expected number for length");
					return;
				}
			} else {
				System.err.println("Expected memory location and size");
				return;
			}

			address = findSymbol(parts[2]);
			if (address == -1) {
				if (parts[2].matches("[0-9A-Fa-f]+")) {
					try {
						address = Integer.parseInt(parts[2], 16);
					} catch (NumberFormatException e) {
						System.err.println("Expected number for address");
						return;
					}
				} else {
					System.err.println("Symbol not found");
					return;
				}
			}

			if (address < 0 || address + length > 65536) {
				System.err.println("Address out of range");
				return;
			}

			for (int i = address; i < address + length; i++) {
				System.out.printf("%04x: %s\n", i, numberFormat.formatShort(emulator.memory[i]));
			}
		}
	}

	private interface PrintFormat {
		String formatShort(short value);
	}

	private static class Keyboard extends KeyAdapter {
		static final short NEWLINE_KEY = 128;
		static final short BACKSPACE_KEY = 129;
		static final short LEFT_KEY = 130;
		static final short UP_KEY = 131;
		static final short RIGHT_KEY = 132;
		static final short DOWN_KEY = 133;
		static final short HOME_KEY = 134;
		static final short END_KEY = 135;
		static final short PAGE_UP_KEY = 136;
		static final short PAGE_DOWN_KEY = 137;
		static final short INSERT_KEY = 138;
		static final short DELETE_KEY = 139;
		static final short ESC_KEY = 140;
		static final short F1_KEY = 141;
		static final short F2_KEY = 142;
		static final short F3_KEY = 143;
		static final short F4_KEY = 144;
		static final short F5_KEY = 145;
		static final short F6_KEY = 146;
		static final short F7_KEY = 147;
		static final short F8_KEY = 148;
		static final short F9_KEY = 149;
		static final short F10_KEY = 150;
		static final short F11_KEY = 151;
		static final short F12_KEY = 152;
		private static short[] actionKeyCodes;

		static {
			initKeyCodes();
		}

		private final short[] keyPressed;

		Keyboard(short[] keyPressed) {
			this.keyPressed = keyPressed;
		}

		private static void initKeyCodes() {
			actionKeyCodes = new short[255];
			actionKeyCodes[KeyEvent.VK_PAGE_UP] = PAGE_UP_KEY;
			actionKeyCodes[KeyEvent.VK_PAGE_DOWN] = PAGE_DOWN_KEY;
			actionKeyCodes[KeyEvent.VK_END] = END_KEY;
			actionKeyCodes[KeyEvent.VK_HOME] = HOME_KEY;
			actionKeyCodes[KeyEvent.VK_LEFT] = LEFT_KEY;
			actionKeyCodes[KeyEvent.VK_UP] = UP_KEY;
			actionKeyCodes[KeyEvent.VK_RIGHT] = RIGHT_KEY;
			actionKeyCodes[KeyEvent.VK_DOWN] = DOWN_KEY;
			actionKeyCodes[KeyEvent.VK_F1] = F1_KEY;
			actionKeyCodes[KeyEvent.VK_F2] = F2_KEY;
			actionKeyCodes[KeyEvent.VK_F3] = F3_KEY;
			actionKeyCodes[KeyEvent.VK_F4] = F4_KEY;
			actionKeyCodes[KeyEvent.VK_F5] = F5_KEY;
			actionKeyCodes[KeyEvent.VK_F6] = F6_KEY;
			actionKeyCodes[KeyEvent.VK_F7] = F7_KEY;
			actionKeyCodes[KeyEvent.VK_F8] = F8_KEY;
			actionKeyCodes[KeyEvent.VK_F9] = F9_KEY;
			actionKeyCodes[KeyEvent.VK_F10] = F10_KEY;
			actionKeyCodes[KeyEvent.VK_F11] = F11_KEY;
			actionKeyCodes[KeyEvent.VK_F12] = F12_KEY;
			actionKeyCodes[KeyEvent.VK_INSERT] = INSERT_KEY;
		}

		short getKeyCode(KeyEvent e) {
			short key;
			int letter = (int) e.getKeyChar();
			short code = (short) e.getKeyCode();

			if (letter == KeyEvent.CHAR_UNDEFINED)
				key = actionKeyCodes[code];
			else {
				key = (short) letter;
			}

			return key;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			short key = getKeyCode(e);

			if (key > 0) {
				synchronized (keyPressed) {
					keyPressed[0] = key;
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			synchronized (keyPressed) {
				keyPressed[0] = 0;
			}
		}
	}

	private static class Breakpoint {
		private String name;
		private short value;

		Breakpoint(String name, short value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, value);
		}

		@Override
		public String toString() {
			if (name.equals("ip"))
				return String.format("ip = %s", getAddressString(value));
			else if (name.matches("&[0-9A-F]{4}")) {
				return String.format("&%s = %04x", getAddressString(Integer.parseInt(name.substring(1), 16)), value);
			} else if (name.equals("on-out")) {
				return name;
			}

			return String.format("%s = %04x", name, value);
		}

		String getName() {
			return name;
		}

		short getValue() {
			return value;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (!(obj instanceof Breakpoint)) return false;

			Breakpoint bp = (Breakpoint) obj;

			return name.equals(bp.getName()) && value == bp.getValue();
		}
	}
}

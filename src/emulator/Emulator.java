package emulator;

import java.util.Arrays;
import java.util.function.IntSupplier;

public class Emulator {

	public short[] memory = new short[65536];

	public short a, b, c, d, e, fp, sp, ip;

	public short out;

	public boolean zf, sf, cf, of;

	public short tpat = 1, fpat;

	public boolean error;

	public long instructionsRetired = 0;
	public long cycleCount;
	public Instruction previousInstruction = new Instruction((short) 0);

	public boolean outputTripped = false;

	public IntSupplier input = null;
	public boolean previousInstructionWasTaken = false;

	public void executeInstruction() {
		cycleCount++;

		if (error)
			return;

		Instruction instruction = new Instruction(ip);

		instruction.instruction = memory[Short.toUnsignedInt(ip)];

		if (instruction.isInvalid()) {
			error = true;
			return;
		}

		instructionsRetired++;

		ip++;

		if (instruction.hasImmediate()) {
			instruction.immediate = memory[Short.toUnsignedInt(ip)];
			ip++;
			cycleCount++;
		}

		previousInstruction = instruction;
		outputTripped = false;

		previousInstructionWasTaken = conditionMet(instruction);

		if (!previousInstructionWasTaken)
			return;

		int opcode = instruction.getOpcode();
		short arg1 = getArg1(instruction);
		short arg2 = getArg2(instruction);
		if (opcode == 0x01) {
			setArg1(instruction, arg2);
		} else if (opcode == 0x02) {
			memory[Short.toUnsignedInt(sp)] = arg1;
			sp++;
			cycleCount++;
		} else if (opcode == 0x03) {
			memory[Short.toUnsignedInt(sp)] = arg2;
			sp++;
			cycleCount++;
		} else if (opcode == 0x04) {
			memory[Short.toUnsignedInt(arg2)] = arg1;
			cycleCount++;
		} else if (opcode == 0x05) {
			memory[Short.toUnsignedInt(arg1)] = arg2;
			cycleCount++;
		} else if (opcode == 0x06) {
			setArg1(instruction, memory[Short.toUnsignedInt((short) (sp - 1))]);
			sp--;
			cycleCount++;
		} else if (opcode == 0x07) {
			memory[Short.toUnsignedInt(sp)] = arg1;
			sp++;
			setArg1(instruction, arg2);
			cycleCount++;
		} else if (opcode == 0x08) {
			setArg1(instruction, e);
		} else if (opcode == 0x09) {
			setArg1(instruction, fp);
		} else if (opcode == 0x0A) {
			setArg1(instruction, sp);
		} else if (opcode == 0x0B) {
			setArg1(instruction, ip);
		} else if (opcode == 0x0C) {
			short flags = 0;
			if (zf)
				flags |= 1;
			if (sf)
				flags |= 2;
			if (cf)
				flags |= 4;
			if (of)
				flags |= 8;

			setArg1(instruction, flags);
		} else if (opcode == 0x0D) {
			memory[Short.toUnsignedInt((short) (fp + arg2))] = arg1;
			cycleCount++;
		} else if (opcode == 0x0E) {
			short flags = arg2;

			zf = (flags & 1) != 0;
			sf = (flags & 2) != 0;
			cf = (flags & 4) != 0;
			of = (flags & 8) != 0;
		} else if (opcode == 0x0F) {
			memory[Short.toUnsignedInt((short) (sp + arg2))] = arg1;
			cycleCount++;
		} else if (opcode == 0x10) {
			out = arg2;
			outputTripped = true;
		} else if (opcode == 0x12) {
			short temp = memory[Short.toUnsignedInt((short) (sp + arg2))];
			memory[Short.toUnsignedInt((short) (sp + arg2))] = arg1;

			setArg1(instruction, temp);
			cycleCount++;
		} else if (opcode == 0x14) {
			if ((arg2 & 1) == 0)
				fpat = arg1;
			else
				tpat = arg1;
		} else if (opcode == 0x15) {
			switch (instruction.getArg2()) {
				case 0:
					short temp = a;
					a = arg1;
					setArg1(instruction, temp);
					break;
				case 1:
					temp = b;
					b = arg1;
					setArg1(instruction, temp);
					break;
				case 2:
					temp = c;
					c = arg1;
					setArg1(instruction, temp);
					break;
				case 3:
					temp = d;
					d = arg1;
					setArg1(instruction, temp);
					break;
				case 4:
					temp = e;
					e = arg1;
					setArg1(instruction, temp);
					break;
				case 5:
					temp = fp;
					fp = arg1;
					setArg1(instruction, temp);
					break;
				case 6:
					temp = sp;
					sp = arg1;
					setArg1(instruction, temp);
					break;
				case 7:
					temp = ip;
					ip = arg1;
					setArg1(instruction, temp);
					break;
			}
		} else if (opcode == 0x16) {
			setArg1(instruction, QueryDevice.get(Short.toUnsignedInt(arg2)));
		} else if (opcode == 0x17) {
			setArg1(instruction, memory[Short.toUnsignedInt((short) (sp + arg2))]);
			cycleCount++;
		} else if (opcode == 0x18) {
			setArg1SfZf(instruction, arg1 == arg2 ? tpat : fpat);
		} else if (opcode == 0x19) {
			setArg1SfZf(instruction, (short) (arg1 & arg2));
		} else if (opcode == 0x1A) {
			setSfZf((short) (arg1 & arg2));
		} else if (opcode == 0x1B) {
			setArg1SfZf(instruction, (short) ~(arg1 & arg2));
		} else if (opcode == 0x1C) {
			setArg1SfZf(instruction, (short) (arg1 | arg2));
		} else if (opcode == 0x1D) {
			setArg1SfZf(instruction, (short) ~(arg1 | arg2));
		} else if (opcode == 0x1E) {
			setArg1SfZf(instruction, (short) (arg1 ^ arg2));
		} else if (opcode == 0x1F) {
			setArg1SfZf(instruction, (short) ~(arg1 ^ arg2));
		} else if (opcode == 0x20) {
			setArg1SfZf(instruction, rotateLeft(arg1, arg2));
		} else if (opcode == 0x21) {
			setArg1SfZf(instruction, rotateRight(arg1, arg2));
		} else if (opcode == 0x22) {
			short temp = memory[Short.toUnsignedInt(arg2)];

			memory[Short.toUnsignedInt(arg2)] = arg1;
			setArg1SfZf(instruction, temp);
			cycleCount++;
		} else if (opcode == 0x23) {
			setArg1SfZf(instruction, (short) (input == null ? 0 : input.getAsInt()));
		} else if (opcode == 0x24) {
			setArg1SfZf(instruction, arg1 != arg2 ? tpat : fpat);
		} else if (opcode == 0x25) {
			setArg1SfZf(instruction, memory[Short.toUnsignedInt((short) (fp + arg2))]);
			cycleCount++;
		} else if (opcode == 0x26) {
			setArg1SfZf(instruction, memory[Short.toUnsignedInt((short) (sp + arg2))]);
			cycleCount++;
		} else if (opcode == 0x27) {
			setArg1SfZf(instruction, memory[Short.toUnsignedInt(arg2)]);
			cycleCount++;
		} else if (opcode == 0x28) {
			int temp = Short.toUnsignedInt(arg1);
			temp <<= (arg2 & 0xF);
			cf = (temp & 0x10000) != 0;
			setArg1SfZf(instruction, (short) temp);
		} else if (opcode == 0x29) {
			int temp = Short.toUnsignedInt(arg1);
			temp <<= 1;
			if (cf)
				temp |= 1;
			temp <<= (arg2 & 0xF);
			temp >>>= 1;
			cf = (temp & 0x10000) != 0;
			setArg1SfZf(instruction, (short) temp);
		} else if (opcode == 0x2A) {
			int temp = Short.toUnsignedInt(arg1);
			temp <<= 1;
			temp >>>= (arg2 & 0xF);
			cf = (temp & 0x1) != 0;
			temp >>>= 1;
			setArg1SfZf(instruction, (short) temp);
		} else if (opcode == 0x2B) {
			int temp = Short.toUnsignedInt(arg1);
			if (cf)
				temp |= 0x10000;
			temp <<= 1;
			temp >>>= (arg2 & 0xF);
			cf = (temp & 0x1) != 0;
			temp >>>= 1;
			setArg1SfZf(instruction, (short) temp);
		} else if (opcode == 0x2C) {
			int temp = arg1;
			temp <<= 1;
			temp >>= (arg2 & 0xF);
			cf = (temp & 0x1) != 0;
			temp >>= 1;
			setArg1SfZf(instruction, (short) temp);
		} else if (opcode == 0x38) {
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);

			cf = (result & 0x10000) != 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setArg1SfZf(instruction, (short) result);
		} else if (opcode == 0x39) {
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);
			if (cf)
				result++;

			cf = (result & 0x10000) != 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setArg1SfZf(instruction, (short) result);
		} else if (opcode == 0x3A) {
			arg2 = (short) -arg2;
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);

			cf = (result & 0x10000) == 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setArg1SfZf(instruction, (short) result);
		} else if (opcode == 0x3B) {
			arg2 = (short) -arg2;
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);
			if (cf)
				result--;

			cf = (result & 0x10000) == 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setArg1SfZf(instruction, (short) result);
		} else if (opcode == 0x3C) {
			arg2 = (short) -arg2;
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);

			cf = (result & 0x10000) == 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setSfZf((short) result);
		} else if (opcode == 0x3D) {
			arg1 = (short) -arg1;
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);
			if (cf)
				result--;

			cf = (result & 0x10000) == 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setArg1SfZf(instruction, (short) result);
		} else if (opcode == 0x3E) {
			arg1 = (short) -arg1;
			int result = Short.toUnsignedInt(arg1) + Short.toUnsignedInt(arg2);

			cf = (result & 0x10000) == 0;
			of = (arg1 < 0 && arg2 < 0 && result >= 0) || (arg1 >= 0 && arg2 >= 0 && result < 0);

			setArg1SfZf(instruction, (short) result);
		} else {
			error = true;
		}
	}

	private short rotateLeft(short a, short b) {
		b &= 0xF;
		return (short) ((a << b) | (Short.toUnsignedInt(a) >>> (16 - b)));
	}

	private short rotateRight(short a, short b) {
		b &= 0xF;
		return (short) ((a >>> b) | (a << (16 - b)));
	}

	private boolean conditionMet(Instruction instruction) {
		switch (instruction.getCondition()) {
			case 0:
				return true;
			case 1:
				return !(cf | zf);
			case 2:
				return !cf;
			case 3:
				return cf;
			case 4:
				return (cf | zf);
			case 5:
				return zf;
			case 6:
				return !zf;
			case 7:
				return !(zf | (sf ^ of));
			case 8:
				return sf == of;
			case 9:
				return sf ^ of;
			case 10:
				return (zf | (sf ^ of));
			case 11:
				return of;
			case 12:
				return !of;
			case 13:
				return sf;
			case 14:
				return !sf;
			case 15:
				return c != 0;
		}

		return true;
	}

	private short getArg1(Instruction instruction) {
		switch (instruction.getArg1()) {
			case 0:
				return a;
			case 1:
				return b;
			case 2:
				return c;
			case 3:
				return d;
			case 4:
				return e;
			case 5:
				return fp;
			case 6:
				return sp;
			case 7:
				return ip;
		}

		return 0;
	}

	private short getArg2(Instruction instruction) {
		switch (instruction.getArg2()) {
			case 0:
				return a;
			case 1:
				return b;
			case 2:
				return c;
			case 3:
				return d;
			case 4:
				return 0;
			case 5:
				return 1;
			case 6:
				return -1;
			case 7:
				return instruction.immediate;
		}

		return 0;
	}

	private void setArg1(Instruction instruction, short value) {
		switch (instruction.getArg1()) {
			case 0:
				a = value;
				break;
			case 1:
				b = value;
				break;
			case 2:
				c = value;
				break;
			case 3:
				d = value;
				break;
			case 4:
				e = value;
				break;
			case 5:
				fp = value;
				break;
			case 6:
				sp = value;
				break;
			case 7:
				ip = value;
				break;
		}
	}

	private void setSfZf(short value) {
		zf = value == 0;
		sf = value < 0;
	}

	private void setArg1SfZf(Instruction instruction, short value) {
		setSfZf(value);

		switch (instruction.getArg1()) {
			case 0:
				a = value;
				break;
			case 1:
				b = value;
				break;
			case 2:
				c = value;
				break;
			case 3:
				d = value;
				break;
			case 4:
				e = value;
				break;
			case 5:
				fp = value;
				break;
			case 6:
				sp = value;
				break;
			case 7:
				ip = value;
				break;
		}
	}

	public void resetState() {
		a = 0;
		b = 0;
		c = 0;
		d = 0;
		e = 0;
		fp = 0;
		sp = 0;
		ip = 0;

		previousInstruction = new Instruction((short) 0);

		out = 0;

		outputTripped = false;

		zf = false;
		sf = false;
		cf = false;
		of = false;

		tpat = 1;
		fpat = 0;
		error = false;
		cycleCount = 0;
		instructionsRetired = 0;
	}

	public void resetMemory() {
		Arrays.fill(memory, (short) 0);
	}

	public Instruction instructionAt(int pointer) {
		Instruction instruction = new Instruction((short) pointer);

		instruction.instruction = memory[pointer];
		if (instruction.hasImmediate())
			instruction.immediate = memory[Short.toUnsignedInt((short) (pointer + 1))];

		return instruction;
	}

	public short get(String name) {
		if (name.equals("a"))
			return a;
		else if (name.equals("b"))
			return b;
		else if (name.equals("c"))
			return c;
		else if (name.equals("d"))
			return d;
		else if (name.equals("e"))
			return e;
		else if (name.equals("fp"))
			return fp;
		else if (name.equals("sp"))
			return sp;
		else if (name.equals("ip"))
			return ip;
		else if (name.equals("out"))
			return out;
		else if (name.matches("&[0-9A-Fa-f]{4}")) {
			return memory[Integer.parseInt(name.substring(1), 16)];
		} else if (name.equals("zf"))
			return (short) (zf ? 1 : 0);
		else if (name.equals("sf"))
			return (short) (sf ? 1 : 0);
		else if (name.equals("cf"))
			return (short) (cf ? 1 : 0);
		else if (name.equals("of"))
			return (short) (of ? 1 : 0);
		else if (name.equals("flags")) {
			short flags = 0;
			if (zf)
				flags |= 1;
			if (sf)
				flags |= 2;
			if (cf)
				flags |= 4;
			if (of)
				flags |= 8;

			return flags;
		}

		return 0;
	}
}

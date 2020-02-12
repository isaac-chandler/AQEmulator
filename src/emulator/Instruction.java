package emulator;

public class Instruction {
	public short instruction;
	public short immediate;
	public short address;

	public Instruction(short address) {
		this.address = address;
	}

	public int length() {
		return hasImmediate() ? 2 : 1;
	}

	public short getImmediate() {
		return immediate;
	}

	public int getOpcode() {
		return (instruction >>> 10) & 0x3F;
	}

	public int getArg1() {
		return (instruction >>> 7) & 0x7;
	}

	public int getArg2() {
		return (instruction >>> 4) & 0x7;
	}

	public int getCondition() {
		return instruction & 0xF;
	}

	public boolean hasImmediate() {
		return getArg2() == 0x7;
	}

	public String getArg1Name() {
		switch (getArg1()) {
			case 0:
				return "a";
			case 1:
				return "b";
			case 2:
				return "c";
			case 3:
				return "d";
			case 4:
				return "e";
			case 5:
				return "fp";
			case 6:
				return "sp";
			case 7:
				return "ip";
		}

		return "UNKOWN ARG1";
	}

	public String getArg2Name() {
		if (getOpcode() != 0x15) {
			switch (getArg2()) {
				case 0:
					return "a";
				case 1:
					return "b";
				case 2:
					return "c";
				case 3:
					return "d";
				case 4:
					return "0000";
				case 5:
					return "0001";
				case 6:
					return "FFFF";
				case 7:
					return String.format("%04x", Short.toUnsignedInt(immediate));
			}
		} else {
			switch (getArg2()) {
				case 0:
					return "a";
				case 1:
					return "b";
				case 2:
					return "c";
				case 3:
					return "d";
				case 4:
					return "e";
				case 5:
					return "fp";
				case 6:
					return "sp";
				case 7:
					return "ip";
			}
		}

		return "UNKOWN ARG2";
	}

	public String getConditionName() {
		switch (getCondition()) {
			case 0:
				return "";
			case 1:
				return ":a";
			case 2:
				return ":ae";
			case 3:
				return ":b";
			case 4:
				return ":be";
			case 5:
				return ":z";
			case 6:
				return ":nz";
			case 7:
				return ":g";
			case 8:
				return ":ge";
			case 9:
				return ":l";
			case 10:
				return ":le";
			case 11:
				return ":o";
			case 12:
				return ":no";
			case 13:
				return ":s";
			case 14:
				return ":ns";
			case 15:
				return ":cnz";
		}

		return "UNKNOWN CONDITION";
	}

	public String getOpcodeName() {
		switch (getOpcode()) {
			case 0x01:
			case 0x04:
			case 0x05:
			case 0x08:
			case 0x09:
			case 0x0A:
			case 0x0B:
			case 0x0D:
			case 0x0F:
			case 0x25:
			case 0x26:
			case 0x27:
				return "set";
			case 0x02:
			case 0x03:
				return "push";
			case 0x06:
				return "pop";
			case 0x07:
				return "pshs";
			case 0x0C:
				return "getf";
			case 0x0E:
				return "setf";
			case 0x10:
				return "out";
			case 0x14:
				return "bpat";
			case 0x15:
			case 0x22:
			case 0x12:
				return "swap";
			case 0x16:
				return "qery";
			case 0x17:
				return "fset";
			case 0x18:
				return "steq";
			case 0x19:
				return "and";
			case 0x1A:
				return "test";
			case 0x1B:
				return "nand";
			case 0x1C:
				return "or";
			case 0x1D:
				return "nor";
			case 0x1E:
				return "xor";
			case 0x1F:
				return "xnor";
			case 0x20:
				return "rotl";
			case 0x21:
				return "rotr";
			case 0x23:
				return "in";
			case 0x24:
				return "stnq";
			case 0x28:
				return "shl";
			case 0x29:
				return "shlc";
			case 0x2A:
				return "shr";
			case 0x2B:
				return "shrc";
			case 0x2C:
				return "shra";
			case 0x38:
				return "add";
			case 0x39:
				return "addc";
			case 0x3A:
				return "sub";
			case 0x3B:
				return "subb";
			case 0x3C:
				return "comp";
			case 0x3D:
				return "sbbr";
			case 0x3E:
				return "subr";
			default:
				return INVALID_OPCODE;
		}
	}

	public static final String INVALID_OPCODE = "INVALID OPCODE";

	public boolean isInvalid() {
		return getOpcodeName().equals(INVALID_OPCODE);
	}

	@Override
	public String toString() {
		String opcodeName = getOpcodeName();
		if (isInvalid())
			return opcodeName;

		int opcode = getOpcode();

		if (opcode == 0x01) {
			if (getArg1() == 7)
				return "jump" + getConditionName() + " " + formatMemoryArg2();
		} else if (opcode == 0x02 || opcode == 0x0C || opcode == 0x23) {
			return opcodeName + getConditionName() + " " + getArg1Name();
		} else if (opcode == 0x03 || opcode == 0x10) {
			return opcodeName + getConditionName() + " " + getArg2Name();
		} else if (opcode == 0x04) {
			return opcodeName + getConditionName() + " &" + formatMemoryArg2() + ", " + getArg1Name();
		} else if (opcode == 0x05) {
			return opcodeName + getConditionName() + " &" + getArg1Name() + ", " + getArg2Name();
		} else if (opcode == 0x06) {
			if (getArg1() == 7)
				return "ret" + getConditionName();
			else
				return opcodeName + " " + getArg1Name();
		} else if (opcode == 0x07) {
			if (getArg1() == 7)
				return "call"  + getConditionName() + " " + formatMemoryArg2();
		} else if (opcode == 0x08) {
			if (getArg2() == 7)
				return "jump" + getConditionName() + " e";
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", e";
		} else if (opcode == 0x09) {
			if (getArg2() == 7)
				return "jump" + getConditionName() + " fp";
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", fp";
		} else if (opcode == 0x0A) {
			if (getArg2() == 7)
				return "jump" + getConditionName() + " sp";
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", sp";
		} else if (opcode == 0x0B) {
			if (getArg2() == 7)
				return "jump" + getConditionName() + " ip";
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", ip";
		} else if (opcode == 0x0D) {
			return opcodeName + getConditionName() + " " + formatOffsetAccess("fp") + ", " + getArg1Name();
		} else if (opcode == 0x0E) {
			if (getArg2() == 4)
				return "clrf" + getConditionName();
			else
				return opcodeName + getConditionName() + " " + getArg2Name();
		} else if (opcode == 0x0F) {
			return opcodeName + getConditionName() + " " + formatOffsetAccess("sp") + ", " + getArg1Name();
		} else if (opcode == 0x12) {
			if (getArg1() == 0x7) {
				return "scal" + getConditionName() + " " + getArg2();
			}
			return "sswp" + getConditionName() + " " + getArg1Name() + ", " + formatOffsetAccess("sp");
		} else if (opcode == 0x14) {
			if (getArg2() == 4)
				return "fpat" + getConditionName() + " " + getArg1Name();
			else if (getArg2() == 5)
				return "tpat" + getConditionName() + " " + getArg1Name();
		} else if (opcode == 0x17) {
			return opcodeName + getConditionName() + " " + getArg1Name() + ", " + formatOffsetAccess("sp");
		} else if (opcode == 0x18) {
			if (getArg2() == 4)
				return "flip" + getConditionName() + " " + getArg1Name();
		} else if (opcode == 0x22) {
			return opcodeName + getConditionName() + " " + getArg1Name() + ", &" + formatMemoryArg2();
		} else if (opcode == 0x25) {
			if (getArg1() == 7)
				return "jump" + getConditionName() + " " + formatOffsetAccess("fp");
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", " + formatOffsetAccess("fp");
		} else if (opcode == 0x26) {
			if (getArg1() == 7)
				return "jump" + getConditionName() + " " + formatOffsetAccess("sp");
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", " + formatOffsetAccess("sp");
		} else if (opcode == 0x27) {
			if (getArg1() == 7)
				return "jump" + getConditionName() + " &" + formatMemoryArg2();
			else
				return opcodeName + getConditionName() + " " + getArg1Name() + ", &" + formatMemoryArg2();
		} else if (opcode == 0x38) {
			if (getArg2() == 5)
				return "inc" + getConditionName() + " " + getArg1Name();
		} else if (opcode == 0x3A) {
			if (getArg2() == 5 && getArg1() == 7)
				return "spin" + getConditionName();
			else if (getArg2() == 5)
				return "dec" + getConditionName() + " " + getArg1Name();
		} else if (opcode == 0x3C) {
			if (getArg2() == 4)
				return "chck" + getConditionName() + " " + getArg1Name();
		} else if (opcode == 0x3E) {
			if (getArg2() == 4)
				return "neg" + getConditionName() + " " + getArg1Name();
		}

		return opcodeName + getConditionName() + " " + getArg1Name() + ", " + getArg2Name();
	}

	private String formatMemoryArg2() {
		int arg2 = getArg2();

		short address = 0;

		if (arg2 < 4)
			return getArg2Name();
		else if (arg2 == 4)
			address = 0;
		else if (arg2 == 5)
			address = 1;
		else if (arg2 == 6)
			address = -1;
		else if (arg2 == 7)
			address = immediate;

		return Main.getAddressString(Short.toUnsignedInt(address));
	}

	private String formatOffsetAccess(String register) {
		int arg2 = getArg2();

		if (arg2 == 0)
			return "&(" + register + "+a)";
		else if (arg2 == 1)
			return "&(" + register + "+b)";
		else if (arg2 == 2)
			return "&(" + register + "+c)";
		else if (arg2 == 3)
			return "&(" + register + "+d)";
		else if (arg2 == 4)
			return "&" + register;
		else if (arg2 == 5)
			return "&(" + register + "+0001)";
		else if (arg2 == 6)
			return "&(" + register + "-0001)";
		else if (arg2 == 7)
			if (immediate >= 0) {
				return String.format("&(%s+%04x)", register, immediate);
			} else if (immediate < 0) {
				return String.format("&(%s-%04x)", register, -immediate);
			}

		return "";
	}
}

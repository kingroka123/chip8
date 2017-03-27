import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import org.apache.commons.io.IOUtils;

public class Chip8 implements KeyListener {

	private short opcode;
	private char[] memory = new char[4096];
	private char[] chip8_fontset = { 0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
			0x20, 0x60, 0x20, 0x20, 0x70, // 1
			0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
			0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
			0x90, 0x90, 0xF0, 0x10, 0x10, // 4
			0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
			0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
			0xF0, 0x10, 0x20, 0x40, 0x40, // 7
			0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
			0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
			0xF0, 0x90, 0xF0, 0x90, 0x90, // A
			0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
			0xF0, 0x80, 0x80, 0x80, 0xF0, // C
			0xE0, 0x90, 0x90, 0x90, 0xE0, // D
			0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
			0xF0, 0x80, 0xF0, 0x80, 0x80 // F 
	};

	private char[] V = new char[16];
	private short I;
	private short pc;
	private char delay_timer;
	private short[] stack = new short[16];// stack
	private short sp; // stack pointer
	public char[] gfx = new char[64 * 32];
	private char[] keys = new char[16];
	private char sound_timer;
	public boolean drawFlag;
	private Random random;

	public Chip8() {
		random = new Random();
	}

	public void initialize() {
		// Initialize registers and memory once
		pc = 0x200; // Program counter starts at 0x200
		opcode = 0; // Reset current opcode
		I = 0; // Reset index register
		sp = 0; // Reset stack pointer

		// Clear display
		for(int i = 0; i < 2048; ++i)
			gfx[i] = 0;
		// Clear stack
		for(int i = 0; i < 16; ++i)
			stack[i] = 0;
		// Clear registers V0-VF
		for(int i = 0; i < 16; ++i)
			keys[i] = V[i] = 0;
		// Clear memory
		for(int i = 0; i < 4096; ++i)
			memory[i] = 0;
		// Load fontset
		for (int i = 0; i < 80; ++i) {
			memory[i] = chip8_fontset[i];
		}

		for (int i = 0; i < 0xF; i++) {
			keys[i] = 0;
		}
		// Reset timers
		delay_timer = 0;
		sound_timer = 0;
	}

	private void log(Object o) {
		System.out.println(o);
	}

	public void loadGame(String path) throws IOException {
		File initialFile = new File(path);
		InputStream targetStream = new FileInputStream(initialFile);

		byte[] buffer = IOUtils.toByteArray(targetStream);
		for (int i = 0; i < buffer.length; i++) {
			memory[i + 512] = (char) (buffer[i] & 0xFF);
			// System.out.println(Integer.toHexString(memory[i + 512]));
		}
	}

	public void emulateCycle() {
		// Fetch Opcode
		opcode = (short) (memory[pc] << 8 | memory[pc + 1]);
		// Decode Opcode
		// log("read: "+Integer.toHexString(opcode));
		// Execute Opcode
		switch ((int) opcode & 0xF000) {
		case 0x0000: {
			switch (opcode & 0x000F) {
			case 0x0000: {
				clearDisplay();
				break;
			}
			case 0x000E: {
				returnFromSubroutine();
				break;
			}
			}
			break;
		}

		case 0x1000: {
			goToAddress();
			break;

		}
		case 0x2000: {
			callSubroutine();
			break;

		}
		case 0x3000: {
			skipIfEqual();
			break;

		}
		case 0x4000: {
			skipIfNotEqual();
			break;

		}
		case 0x5000: {
			skipIfEqual2();
			break;

		}
		case 0x6000: {
			set();
			break;

		}
		case 0x7000: {
			add();
			break;

		}
		case 0x8000: {

			switch (opcode & 0x000F) {
			case 0x0000: {
				set2();
				break;
			}
			case 0x0001: {
				orSet();
				break;
			}
			case 0x0002: {
				andSet();
				break;
			}
			case 0x0003: {
				xorSet();
				break;
			}

			case 0x0004: {
				carryAdd();

				break;
			}
			case 0x0005: {
				borrowSub();
				break;

			}
			case 0x0006: {
				shiftRight();
				break;
			}
			case 0x0007: {
				notBorrowSub();
				break;
			}
			case 0x000E: {
				shiftLeft();
				break;
			}
			}
			break;

		}
		case 0x9000: {
			skipIfNotEqual2();
			break;
		}
		case 0xA000: {
			setI();
			break;
		}
		case 0xB000: {
			jump();
			break;
		}
		case 0xC000: {
			rand();
			break;
		}
		case 0xD000: {
			draw();
			break;
		}
		case 0xE000: {
			switch (opcode & 0x00FF) {
			case 0x009E: { // EX9E - Skips the next instruction if the key stored
							// in
							// VX is pressed
				if (keys[V[(opcode & 0x0F00) >> 8]] != 0) {
					pc += 4;
				} else {
					pc += 2;
				}
				break;
			}
			case 0x00A1: { // EXA1 - Skips the next instruction if the key stored
							// in
							// VX isn't pressed
				if (keys[V[(opcode & 0x0F00) >> 8]] == 0)
					pc += 4;
				else
					pc += 2;
				break;
			}
			}
			break;
		}
		case 0xF000: {
			switch (opcode & 0x00FF) {
			case 0x0007: {
				setRegisterToDelayTimer();
				break;
			}
			case 0x000A: {// FX0A - A key press is awaited, and then stored in VX
				boolean picked = false;
				System.out.println("waiting for input");
				while (!picked) {
					for (int i = 0; i < keys.length; i++) {
						char key = keys[i];
						if (key != 0) {
							picked = true;
							V[(opcode & 0x0F00 >> 8)] = (char) i;
							pc+=2;
							break;
						}
					}
				}
				break;
			}
			case 0x0015: {
				setDelayTimer();
				break;
			}
			case 0x0018: {
				setSoundTimer();
				break;
			}
			case 0x001E: {
				addToI();
				break;
			}
			case 0x0029: {
				setIToSpriteLoc();
				break;
			}
			case 0x0033: {
				binaryStore();
				break;

			}
			case 0x0055: {
				store();
				break;
			}
			case 0x0065: {
				fill();
				break;
			}
			}
			break;
		}
		default:
			System.out.printf("Unknown opcode: 0x%X\n", opcode);
			break;
		}

		// Update timers
		if (delay_timer > 0)
			--delay_timer;

		if (sound_timer > 0) {
			if (sound_timer == 1)
				System.out.println("BEEP!\n");
			sound_timer--;
		}
		try {
			Thread.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//debugRender();
	}


	private void debugRender()
	{
		// Draw
		for(int y = 0; y < 32; ++y)
		{
			for(int x = 0; x < 64; ++x)
			{
				if(gfx[(y*64) + x] == 0) 
					System.out.print("O");
				else 
					System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	private void clearDisplay() {
		// 00E0 - clear display
		gfx = new char[64 * 32];
		pc += 2;
		drawFlag = true;
		log("cleared display");
	}

	private void returnFromSubroutine() {
		// 00EE - return; returns from a subroutine
		sp--;
		pc = stack[sp];
		pc += 2;
		log("returned");
	}

	private void goToAddress() {
		// 1NNN - goto address NNN
		pc = (short) (opcode & 0x0FFF);
		log("pc set to " + pc);
	}

	private void callSubroutine() {
		// 2NNN - calls subroutine at NNN
		stack[sp] = pc;
		sp++;
		pc = (short) (opcode & 0x0FFF);
		log("called subroutine at " + pc);
	}

	private void skipIfEqual() {
		// 3XNN - Skips the next instruction if VX equals NN
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short nn = (short) (opcode & 0x00FF);
		if (V[xx] == nn) {
			pc += 4;
			log("skipped because V" + xx + " and " + nn + " were equal");
		} else {
			pc += 2;
		}
	}

	private void skipIfNotEqual() {
		// 4XNN - Skip the next instruction if VX doesnt equal NN
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short nn = (short) (opcode & 0x00FF);
		if (V[xx] != nn) {
			pc += 4;
			log("skipped because V" + xx + " and " + nn + " were not equal");
		} else {
			pc += 2;
		}
	}

	private void skipIfEqual2() {
		// 5XY0 - Skips the next instruction if VX equals VY
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		if (V[xx] == V[yy]) {
			pc += 4;
			log("skipped because V" + xx + " and " + "V" + yy + " were equal");
		} else {
			pc += 2;
		}
	}

	private void set() {
		// 6XNN - Sets VX to NN
		short xx = (short) ((opcode & 0x0F00) >> 8);
		char val = (char) (opcode & 0x00FF);
		V[xx] = val;
		log("set V" + xx + " to: 0x" + Integer.toHexString(val));
		pc += 2;
	}

	private void add() {
		// 7XNN - Adds NN to VX
		short xx = (short) ((opcode & 0x0F00) >> 8);
		char val = (char) (opcode & 0x00FF);
		V[xx] += val;
		log("added " + Integer.toHexString(val) + " to V" + xx);
		pc += 2;
	}

	private void set2() {
		// 8XY0 - Sets VX to the value of VY
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		V[xx] = V[yy];
		log("set V" + xx + " to: 0x" + Integer.toHexString(V[yy]));
		pc += 2;

	}

	private void orSet() {
		// 8XY1 - Sets VX to VX or VY
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		V[xx] |= V[yy];
		log("OR set V" + xx + " to: 0x" + Integer.toHexString(V[xx]));
		pc += 2;
	}

	private void andSet() {
		// 8XY2 - Sets VX to VX and VY
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		V[xx] &= V[yy];
		log("AND set V" + xx + " to: 0x" + Integer.toHexString(V[xx]));
		pc += 2;
	}

	private void xorSet() {
		// 8XY3 - Sets VX to VX xor VY; VF is reset to 0
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		V[xx] ^= V[yy];
		log("XOR set V" + xx + " to: 0x" + Integer.toHexString(V[xx]));
		pc += 2;
	}

	private void carryAdd() {
		// 8XY4 - Adds VY to VX; VF is set to 1 when there's
		// a carry, and to 0 when there isn't
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		int i = V[xx] + V[yy];
		if (V[yy] > (0xFF - V[xx])) {
			V[0xF] = 1;
		} else {
			V[0xF] = 0;
		}
		V[xx] = (char) (V[yy] + V[xx]);
		log("carry added " + V[yy] + " to V" + xx);
		pc += 2;
	}

	private void borrowSub() {
		// 8XY5 - VY is subtracted from VX; VF is set to 0 when
		// there's a borrow, and 1 when there isn't
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		if (V[xx] > V[yy]) {
			V[0xF] = 1;
		} else {
			V[0xF] = 0;
		}
		V[xx] = (char) (V[xx] - V[yy]);
		log("borrow subtracted " + V[yy] + " from V" + xx);
		pc += 2;
	}

	private void shiftRight() {
		// 8XY6 - Shifts VX right by one; VF is set to the value
		// of the least significant bit of VX before the shift
		short xx = (short) ((opcode & 0x0F00) >> 8);
		V[0xF] = (char) (V[xx] & 0x1);
		V[xx] >>= 0x1;
		log("shifted V" + xx + "to the right");
		pc += 2;
	}

	private void notBorrowSub() {
		// 8XY7 - Sets VX to VY minus VX; VF is set to 0 when
		// there's a borrow, and 1 when there isn't
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		if (V[yy] > V[xx]) {
			V[0xF] = 1;
		} else {
			V[0xF] = 0;
		}
		V[xx] = (char) (V[yy] - V[xx]);
		log("!borrow subtracted " + V[yy] + " from V" + xx);
		pc += 2;
	}

	private void shiftLeft() {
		// 8XYE - Shifts VX left by one; VF is set to the value
		// of the most significant bit of VX before the shift
		short xx = (short) ((opcode & 0x0F00) >> 8);
		V[0xF] = (char) (V[xx] >> 7);
		V[xx] <<= 1;
		log("shifted V" + xx + "to the left");
		pc += 2;
	}

	private void skipIfNotEqual2() {
		// 9XY0 - Skips the next instruction if VX doesn't equal VY
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short yy = (short) ((opcode & 0x00F0) >> 4);
		if (V[xx] != V[yy]) {
			pc += 4;
			log("skipped because V" + xx + " and V" + yy + " were not equal");
		} else {
			pc += 2;
		}

	}

	private void setI() {
		// ANNN - Sets I to the address NNN
		I = (short) (opcode & 0x0FFF);
		log("set I to " + Integer.toHexString(I));
		pc += 2;
	}

	private void jump() {
		// BNNN - Jumps to the address NNN plus V0
		short pos = (short) (opcode & 0x0FFF);
		pc = (short) (pos + V[0]);
		log("jumped to " + pc);
	}

	private void rand() {
		// CXNN - Sets VX to the result of a bitwise and
		// operation
		// on a random number (Typically: 0 to 255) and NN.
		short xx = (short) ((opcode & 0x0F00) >> 8);
		short nn = (short) ((opcode & 0x00FF));
		short rand = (short) random.nextInt(256);

		V[xx] = (char) (rand % 0xFF & nn);
		pc += 2;
	}

	private void draw() {
		short x = (short) V[(opcode & 0x0F00) >> 8];
		short y = (short) V[(opcode & 0x00F0) >> 4];
		short height = (short) (opcode & 0x000F);
		short pixel;

		V[0xF] = 0;
		for (int yline = 0; yline < height; yline++) {
			pixel = (short) memory[I + yline];
			for (int xline = 0; xline < 8; xline++) {
				if ((pixel & (0x80 >> xline)) != 0) {
					if (((x + xline) + ((y + yline) * 64)) >= 2048) {
						System.out.println("X: " + x);
						System.out.println("Y: " + y);
						System.out.println("X Line: " + xline);
						System.out.println("Y Line: " + yline);
						System.out.println("Total: " + ((x + xline) + ((y + yline) * 64)));
					}
					if (gfx[x + xline + (y + yline) * 64] == 1) {
						V[0xF] = 1;
					}

					gfx[x + xline + ((y + yline) * 64)] ^= 1;
					log("put pixel at " + (x + xline) + ", " + (y + yline));
				}
			}
		}

		drawFlag = true;
		pc += 2;
	}

	private void setRegisterToDelayTimer() {
		// Set delay timer
		short xx = (short) ((opcode & 0x0F00) >> 8);
		V[xx] = delay_timer;
		log("Set V" + xx + " timer to " + delay_timer);
		pc += 2;
	}

	private void setDelayTimer() {
		// FX15 - Sets the delay timer to VX
		short xx = (short) ((opcode & 0x0F00) >> 8);
		delay_timer = V[xx];
		log("Set delay timer to " + delay_timer);
		pc += 2;
	}

	private void setSoundTimer() {
		// FX18 - Sets the sound timer to VX
		short xx = (short) ((opcode & 0x0F00) >> 8);
		sound_timer = V[xx];
		pc += 2;
	}

	private void addToI() {
		// FX1E - Adds VX to I
		short xx = (short) ((opcode & 0x0F00) >> 8);
		if (I + V[xx] > 0xFFF)
			V[0xF] = 1;
		else
			V[0xF] = 0;
		I += V[xx];
		log("set I to " + I);
		pc += 2;
	}

	private void setIToSpriteLoc() {
		// FX29 - Sets I to the location of the sprite for the character in VX
		short xx = (short) ((opcode & 0x0F00) >> 8);
		I = (short) (V[xx] * 0x5);
		log("set I to " + I);
		pc += 2;
	}

	private void binaryStore() {
		// FX33 - Stores the binary-coded decimal representation of VX
		memory[I] = (char) (V[(opcode & 0x0F00) >> 8] / 100);
		memory[I + 1] = (char) ((V[(opcode & 0x0F00) >> 8] / 10) % 10);
		memory[I + 2] = (char) ((V[(opcode & 0x0F00) >> 8] % 100) % 10);
		pc += 2;
		log("stored I");
	}

	private void store() {
		// FX55 - Stores V0 to VX (including VX) in memory
		// starting
		// at address I
		short xx = (short) ((opcode & 0x0F00) >> 8);
		for (int i = I; i <= xx; i++) {
			memory[I + i] = V[i];
		}
		I += (xx) + 1;
		log("stored from 0 to " + xx);
		pc += 2;
	}

	private void fill() {
		// FX65 - Fills V0 to VX (including VX) with values
		// from
		// memory starting at address I
		short xx = (short) ((opcode & 0x0F00) >> 8);
		for (int i = 0; i < xx; i++) {
			V[i] = memory[I + i];
		}
		I += (xx) + 1;
		log("filled from 0 to " + xx);
		pc += 2;

	}

	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_1) {
			keys[1] = 1;
		}
		if (key == KeyEvent.VK_2) {
			keys[2] = 1;

		}
		if (key == KeyEvent.VK_3) {
			keys[3] = 1;

		}
		if (key == KeyEvent.VK_4) {
			keys[0xC] = 1;

		}
		if (key == KeyEvent.VK_Q) {
			keys[4] = 1;

		}
		if (key == KeyEvent.VK_W) {
			keys[5] = 1;

		}
		if (key == KeyEvent.VK_E) {
			keys[6] = 1;

		}
		if (key == KeyEvent.VK_R) {
			keys[0xD] = 1;

		}
		if (key == KeyEvent.VK_A) {
			keys[7] = 1;

		}
		if (key == KeyEvent.VK_S) {
			keys[8] = 1;

		}
		if (key == KeyEvent.VK_D) {
			keys[9] = 1;

		}
		if (key == KeyEvent.VK_F) {
			keys[0xE] = 1;
		}
		if (key == KeyEvent.VK_Z) {
			keys[0xA] = 1;
		}
		if (key == KeyEvent.VK_X) {
			keys[0x0] = 1;
		}
		if (key == KeyEvent.VK_C) {
			keys[0xB] = 1;
		}
		if (key == KeyEvent.VK_V) {
			keys[0xF] = 0x1;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_1) {
			keys[1] = 0;
		}
		if (key == KeyEvent.VK_2) {
			keys[2] = 0;

		}
		if (key == KeyEvent.VK_3) {
			keys[3] = 0;

		}
		if (key == KeyEvent.VK_4) {
			keys[0xC] = 0;

		}
		if (key == KeyEvent.VK_Q) {
			keys[4] = 0;

		}
		if (key == KeyEvent.VK_W) {
			keys[5] = 0;

		}
		if (key == KeyEvent.VK_E) {
			keys[6] = 0;

		}
		if (key == KeyEvent.VK_R) {
			keys[0xD] = 0;

		}
		if (key == KeyEvent.VK_A) {
			keys[7] = 0;

		}
		if (key == KeyEvent.VK_S) {
			keys[8] = 0;

		}
		if (key == KeyEvent.VK_D) {
			keys[9] = 0;

		}
		if (key == KeyEvent.VK_F) {
			keys[0xE] = 0;
		}
		if (key == KeyEvent.VK_Z) {
			keys[0xA] = 0;
		}
		if (key == KeyEvent.VK_X) {
			keys[0x0] = 0;
		}
		if (key == KeyEvent.VK_C) {
			keys[0xB] = 0x0;
		}
		if (key == KeyEvent.VK_V) {
			keys[0xF] = 0x0;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

}

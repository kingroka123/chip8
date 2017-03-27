import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main extends JPanel {
	private static final long serialVersionUID = 1L;
	static Chip8 myChip8;
	static JFrame frame = new JFrame("Chip8");
	static Main main;

	public Main() {

	}

	public static void main(String[] args) {
		main = new Main();
		frame.setSize(64 * 12 + 10, 64 * 10 + 10);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(main);
		frame.setVisible(true);

		myChip8 = new Chip8();
		frame.addKeyListener(myChip8);
		// Set up render system and register input callbacks
		// setupGraphics();
		// setupInput();

		// Initialize the Chip8 system and load the game into the memory
		myChip8.initialize();
		try {
			myChip8.loadGame("src/programs/invaders.c8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Emulation loop
		for (;;) {
			// Emulate one cycle
			myChip8.emulateCycle();

			// If the draw flag is set, update the screen
			if (myChip8.drawFlag) {
				// drawGraphics();
			}

			// Store key press state (Press and Release)
			// myChip8.setKeys();
			
			main.repaint();
		}

	}

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.black);
		g2d.drawRect(64-1, 64, 64*10+1,32*10 );
		if (myChip8 != null) {
			
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 32; y++) {
					char c = myChip8.gfx[x + y * 64];
					if (c == 0b1) {
						g2d.setColor(Color.black);
						g2d.fillRect(64+x * 10, y * 10 + 64, 10, 10);
					}else{
						g2d.setColor(Color.white);
						g2d.fillRect(64+x * 10, y * 10 +64, 10, 10);

					}
				}
			}
		}
	}

	static int multiply(int a, int b) {
		int aa = a;
		int bb = b;
		int result = 0;

		while (bb != 0) {
			if ((bb & 0b1) == 0b1) {
				result = result + aa;
			}
			aa <<= 1;// multiply by 2
			bb >>= 1;// divide by 2
		}

		return result;
	}

	static int divide(int a, int b) {
		int i = log(Integer.highestOneBit(a), 2) - log(Integer.highestOneBit(b), 2);
		int N = a;
		int D = b << i;
		int t = (N - D);
		int Q = 0;

		while (t >= 0) {
			Q |= 0b1;
			N = t;

			N <<= 1;
			Q <<= 1;

			t = (N - D);
		}
		return Q >> i;
	}

	static int log(int x, int base) {
		return (int) (Math.log(x) / Math.log(base));
	}
}

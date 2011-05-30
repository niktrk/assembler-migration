package main;

/**
 * Main application class.
 * 
 * @author Nikola Trkulja
 * @author Igor Let
 */
public class App {

	public static void main(String[] args) throws Exception {

		// start remove
		args = new String[1];
		args[0] = "src/asm/mov.asm";
		// end remove

		if (args.length != 0) {
			String filename = args[0];
			if (filename.endsWith(".asm")) {
				String outputFilename = filename.substring(0, filename.lastIndexOf('.')).concat(
						".wsl");
				Scanner scanner = new Scanner(filename);
				Parser parser = new Parser(scanner);
				// PrintWriter output = new PrintWriter(new FileWriter(outputFilename));
				// output.print(parser.parse());
				// output.close();

				// start remove
				System.out.println(parser.parse());
				// end remove

			} else {
				System.out.println("Input file extension must be '.asm'.");
			}
		} else {
			System.out.println("No input file specified.");
		}
	}

}

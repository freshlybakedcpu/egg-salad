package io.github.freshlybakedcpu.egg;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class Main {
    private static String[] samples;
    private static String[] supportedFiles = new String[]{"txt", "json"};

    public static void main(String[] args) {
        // if (Arrays.stream(args).anyMatch(""::equals))
        File inputFile;
        Scanner scanner = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter file path or title of sample file: ");
        String userInput = scanner.nextLine();
        inputFile = new File("samples/" + userInput.toLowerCase().replace(" ", "-") + ".txt");  // Read user input
        if (!inputFile.exists()) // Checks if file exists in "input" folder
            inputFile = new File(userInput);
        if (inputFile.exists()) {
            System.out.println("\nFile path: " + inputFile.getAbsolutePath());
            System.out.println("File name: " + inputFile.getName());
            System.out.println("Writeable: " + inputFile.canWrite());
            System.out.println("Readable: " + inputFile.canRead());
            System.out.println("File size in bytes: " + inputFile.length());
        } else {
            System.out.println("The provided file path \"" + inputFile.getAbsolutePath() +"\" does not exist.");
            return;
        }
        System.out.println("\nWhat file type would you like the data saved in? (Available: " + Arrays.toString(supportedFiles) + ")");
        String fileType = scanner.nextLine();
        if(!Arrays.stream(supportedFiles).anyMatch(fileType::equals)) {
          System.out.println("\nProvided file type \"" + fileType + "\" is not supported.");
          return;
        }

        try {
            // Turns file into String
            String content = readFile(inputFile.getPath(), StandardCharsets.UTF_8);

            // Removes chapter headers
            System.out.println("\nRemoving chapter headers...");
            // Roman numeral regex: (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])

            // Great Expectations: "Chapter I. "
            content = content.replaceAll("Chapter (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])[.]\\R", "");

            // A Tale of Two Cities: "I. "
            // content = content.replaceAll("(?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])([.]\\s+.+\\R)", "");

            // War and Peace: "CHAPTER I  "
            content = content.replaceAll("CHAPTER (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])\\R", "");

            // Removes all newline characters.
            System.out.println("Removing all newline characters...");
            content = content.replace("\r\n", " ").replace("\n", " ");
            content = content.trim().replaceAll("\\s+", " "); // Ensures there are no duplicate newline characters.

            // Loads sentence detector model
            System.out.println("Loading sentence detector model...");
            InputStream inputStream = new FileInputStream("models/en-sent.bin");
            SentenceModel model = new SentenceModel(inputStream);

            // Instantiates the SentenceDetectorME class
            SentenceDetectorME detector = new SentenceDetectorME(model);

            // Detecting the sentence
            System.out.println("Beginning sentence detection...");
            String[] sentences = detector.sentDetect(content);

            System.out.println("Writing to file...");
            FileWriter writer = new FileWriter(String.format("output/%s_en-sent.%s", inputFile.getName().replaceAll("[.]txt", ""), fileType));
            if(fileType.equals("txt")) {
              for (String s : sentences) {
                writer.write(s + "\n");
              }
              writer.close();
            }
            else if(fileType.equals("json")) {
              writer.write("{\n");
              writer.write("\t\"" + inputFile.getName().replaceAll("[.]txt", "") + "\": [\n");
              boolean firstTime = true;
              for (String s : sentences) {
                if(firstTime) {
                  writer.write("\t\t\"" + s + "\"");
                  firstTime = false;
                }
                else {
                  writer.write(",\n\t\t\"" + s + "\"");
                }
              }
              writer.write("\n\t]\n}");
              writer.close();
            }
            else {
              System.out.println("\nThis shouldn't have happened. Please report this error!\n[Error]: fileType did not meet conditional requirements during file writing.\nfileType = " + fileType);
              return;
            }
            System.out.println("Done!");
        } catch (IOException e) {
            System.out.println("Error.");
            e.printStackTrace();
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}

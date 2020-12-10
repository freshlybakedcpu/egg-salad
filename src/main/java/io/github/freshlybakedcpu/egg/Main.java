package io.github.freshlybakedcpu.egg;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;

import com.google.common.base.CaseFormat;

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
        
        System.out.println("\nWhat file type would you like the data saved in? (Available: " + Arrays.toString(supportedFiles).replace("[", "").replace("]", "") + ")");
        String fileType = scanner.nextLine();
        if(Arrays.stream(supportedFiles).noneMatch(fileType::equals)) {
          System.out.println("\nProvided file type \"" + fileType + "\" is not supported.");
          return;
        }
        
        System.out.println("\nWould you like chapter headings? (y/n)");
        String chapterHeadings = scanner.nextLine();
        String chapterCode = "";
        if (!chapterHeadings.matches("[yn]")) {
          System.out.println("\nYour input \"" + chapterHeadings + "\" was not one of the provided options.");
          return;
        }
        else if(chapterHeadings.matches("y")) {
            // Calculates chapter code
            System.out.println("\nCalculating unique chapter code...");
            int[] codePoints = new int[20];
            for (int i = 0; i < 20; i++) {
                // codePoints[i] = (int) Math.floor(Math.random() * (126 - 33 + 1)) + 33;
                codePoints[i] = (int) Math.floor(Math.random() * (90 - 65 + 1)) + 65;
            }
            chapterCode = new String(codePoints, 0, codePoints.length);
        }
        try {
            // Turns file into String
            String content = readFile(inputFile.getPath(), StandardCharsets.UTF_8);

            // Removes chapter headers
            System.out.println("\nRemoving chapter headers...");
            // Roman numeral regex: (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])
            if (chapterHeadings.matches("y")) {
                // Great Expectations: "Chapter I. "
                content = content.replaceAll("Chapter (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])[.]\\R", chapterCode);

                // A Tale of Two Cities: "I. [chapter name]"
                // content = content.replaceAll("(?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])([.]\\s+.+\\R)", chapterCode);

                // War and Peace: "CHAPTER I  "
                // Note: Book headings (e.g. BOOK TWO: 1805) also need to be removed.
                content = content.replaceAll("CHAPTER (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])\\R", chapterCode);

                // Replaces all normal quotes with curly quotes
                System.out.println("Replacing all straight quotes...");
                content = content.replaceAll("\"\\S", "“").replaceAll("\\S\"", "”");

                // 1984: Chapter 1
                // Note: Part headings (e.g.  PART ONE) also need to be removed
                System.out.println("\nSeparating text by chapter...");
                content = content.replaceAll("Chapter\\s^[0-9]+$\\R", chapterCode);

                String[] textByChapter = content.split(chapterCode);

                // Loads sentence detector model
                System.out.println("Loading sentence detector model...");
                InputStream inputStream = new FileInputStream("models/en-sent.bin");
                SentenceModel model = new SentenceModel(inputStream);

                // Instantiates the SentenceDetectorME class
                SentenceDetectorME detector = new SentenceDetectorME(model);

                System.out.println("Removing newline characters and detecting sentences for each chapter (" + (textByChapter.length - 1) + " total)");
                String[][] sentences = new String[textByChapter.length - 1][];
                for (int i = 1; i < textByChapter.length; i++) {
                    System.out.print("\tChapter no." + i + "...");
                    // Removes all newline characters.

                    textByChapter[i] = textByChapter[i].replace("\r\n", " ").replace("\n", " ");
                    textByChapter[i] = textByChapter[i].trim().replaceAll("\\s+", " "); // Ensures there are no duplicate newline characters.

                    // Detecting the sentence
                    sentences[i-1] = detector.sentDetect(textByChapter[i]);
                    System.out.println("done!");
                }

                System.out.println("Writing to file...");
                FileWriter writer = new FileWriter(String.format("output/%s_chapterHeaders_en-sent.%s", inputFile.getName().replaceAll("[.]txt", ""), fileType));
                if(fileType.equals("txt")) {
                    for(int i = 0; i < sentences.length; i++) {
                        System.out.println("\tChapter " + (i+1) + "...");
                        writer.write("Chapter " + (i+1) + "\n");
                        int sentenceNo = 1;
                        for (String s : sentences[i]) {
                            System.out.println("\t\tSentence no. " + sentenceNo);
                            writer.write(s);
                            if (sentenceNo < sentences[i].length) writer.write("\n");
                            sentenceNo++;
                        }
                        if (i + 1 < sentences.length) writer.write("\n\n");
                    }
                    writer.close();
                }
                else if(fileType.equals("json")) {
                    writer.write("{\n");
                    String arrayName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, inputFile.getName().replaceAll("[.]txt", ""));
                    writer.write("\t\"" + arrayName + "\": {\n");
                    boolean firstTime = true;
                    for (int i = 0; i < sentences.length; i++) {
                        System.out.println("\tChapter " + (i + 1) + "...");
                        writer.write("\t\t\"chapter" + (i + 1) + "\": [\n");
                        int sentenceNo = 1;
                        for (String s : sentences[i]) {
                            System.out.println("\t\tSentence no. " + sentenceNo);
                            writer.write("\t\t\t\"" + s + "\"");
                            if (sentenceNo < sentences[i].length) writer.write(",\n");
                            else writer.write("\n");
                            sentenceNo++;
                        }
                        if (i + 1 < sentences.length) writer.write("\t\t],\n");
                        else writer.write("\t\t]\n");
                    }
                    writer.write("\t}\n}");
                    writer.close();
                }
                else {
                    System.out.println("\nThis shouldn't have happened. Please report this error!\n[Error]: fileType did not meet conditional requirements during file writing.\nfileType = " + fileType);
                    return;
                }

                System.out.println("Done!");
            }
            else if (chapterHeadings.matches("n")) {
                // Great Expectations: "Chapter I. "
                content = content.replaceAll("Chapter (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])[.]\\R", "");

                // A Tale of Two Cities: "I. "
                // content = content.replaceAll("(?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])([.]\\s+.+\\R)", "");

                // War and Peace: "CHAPTER I  "
                content = content.replaceAll("CHAPTER (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])\\R", "");

                // 1984: Chapter 1
                // Note: Part headings (e.g.  PART ONE) also need to be removed
                System.out.println("\nSeparating text by chapter...");
                content = content.replaceAll("Chapter\\s[0-9]+\\R", "");

                // Removes all newline characters.
                System.out.println("Removing all newline characters...");
                content = content.replace("\r\n", " ").replace("\n", " ");
                content = content.trim().replaceAll("\\s+", " "); // Ensures there are no duplicate newline characters.

                // Replaces all normal quotes with curly quotes
                System.out.println("Replacing all straight quotes...");
                content = content.replaceAll("\"\\S", "“").replaceAll("\\S\"", "”");

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
                    String arrayName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, inputFile.getName().replaceAll("[.]txt", ""));
                    writer.write("\t\"" + arrayName + "\": [\n");
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
            }
            else {
                System.out.println("\nThis shouldn't have happened. Please report this error!\n[Error]: chapterHeadings did not meet conditional requirements while removing chapter headings.\nchapterHeadings = " + chapterHeadings);
                // 'return' is unnecessary as the last statement in a 'void' method
            }
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

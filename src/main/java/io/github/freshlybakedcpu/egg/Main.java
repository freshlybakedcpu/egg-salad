package io.github.freshlybakedcpu.egg;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class Main {
    public static void main(String[] args) {
        File myObj;
        Scanner myObject = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter file path or title of file in \"input\" folder: ");
        String input = myObject.nextLine();
        myObj = new File("input/"+input.toLowerCase().replace(" ", "-")+".txt");  // Read user input
        if (!myObj.exists()) // Checks if file exists in "input" folder
            myObj = new File(input);
        if (myObj.exists()) {
            System.out.println("File path: " + myObj.getAbsolutePath());
            System.out.println("File name: " + myObj.getName());
            System.out.println("Writeable: " + myObj.canWrite());
            System.out.println("Readable: " + myObj.canRead());
            System.out.println("File size in bytes: " + myObj.length());
        } else {
            System.out.println("Provided file path: " + myObj.getAbsolutePath());
            System.out.println("The file does not exist.");
            return;
        }

        try {
            // Turns file into String
            String content = readFile(myObj.getPath(), StandardCharsets.UTF_8);

            // Removes chapter headers
            System.out.println("\nRemoving chapter headers...");
            // Roman numeral regex: (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])

            // Great Expectations: "Chapter I. "
            content = content.replaceAll("Chapter (?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])[.]\\R", "");

            // A Tale of Two Cities: "I. "
            content = content.replaceAll("(?<![A-Z])(M*(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3}))(?![A-Z])([.]\\s+.+\\R)", "");

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
            FileWriter writer = new FileWriter(String.format("output/%s_en-sent.txt", myObj.getName()));
            for (String s : sentences) {
                writer.write(s + "\n");
            }
            writer.close();
            System.out.println("Done!");
        }
        catch(IOException e) {
            System.out.println("Error.");
            e.printStackTrace();
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
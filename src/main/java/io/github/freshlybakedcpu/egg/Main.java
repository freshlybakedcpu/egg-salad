package io.github.freshlybakedcpu.egg;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class Main {
    public static void main(String[] args) throws Exception {
        File myObj;
        // if(!ArrayUtils.isEmpty(arr)) {
        Scanner myObject = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter file path: ");
        String input = myObject.nextLine();
        myObj = new File(input);  // Read user input
    /*
    }
    else {
      myObj = new File(args[1]);
    }
    */

        if (myObj.exists()) {
            System.out.println("File name: " + myObj.getName());
            System.out.println("Writeable: " + myObj.canWrite());
            System.out.println("Readable: " + myObj.canRead());
            System.out.println("File size in bytes: " + myObj.length());
        } else {
            System.out.println("The file does not exist.");
            return;
        }

        try {
            // Turns file into String
            String content = readFile(input, StandardCharsets.UTF_8);

            // Removes all newline characters.
            content = content.replace("\r\n", " ").replace("\n", " ");

            // Loading sentence detector model
            InputStream inputStream = new FileInputStream("models/en-sent.bin");
            SentenceModel model = new SentenceModel(inputStream);

            // Instantiating the SentenceDetectorME class
            SentenceDetectorME detector = new SentenceDetectorME(model);

            // Detecting the sentence
            String sentences[] = detector.sentDetect(content);

            FileWriter writer = new FileWriter(String.format("output/%s_en-sent.txt", myObj.getName()));
            for (int i = 0; i < sentences.length; i++) {
                writer.write(sentences[i] + "\n\n");
            }
            writer.close();
        }
        catch(IOException e) {
            System.out.println("Error.");
            e.printStackTrace();
            return;
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
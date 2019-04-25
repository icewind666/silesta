package com.silesta;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.giaybac.traprange.PDFTableExtractor;
import com.giaybac.traprange.entity.Table;
import com.silesta.models.BankOperation;
import com.silesta.runtime.CommandRunner;
import com.silesta.util.Command;
import com.silesta.voice.SilestaVoice;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
//import sun.util.logging.PlatformLogger;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.neuroph.contrib.graphml.ExampleNetworXOR;
import org.neuroph.contrib.graphml.GraphmlExport;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.events.NeuralNetworkEvent;
import org.neuroph.core.events.NeuralNetworkEventListener;
import org.neuroph.core.learning.IterativeLearning;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.Perceptron;
import org.neuroph.nnet.learning.PerceptronLearning;
import org.neuroph.nnet.learning.ResilientPropagation;
import org.neuroph.util.TransferFunctionType;
import org.neuroph.util.data.norm.MaxMinNormalizer;
import org.neuroph.util.data.norm.ZeroMeanNormalizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());
    private static String startingLineRegex = "^([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) ?([^\\/][а-яa-zА-ЯA-Z\\s\\-\\@0-9\\*\\.]+)\\s+([\\+]?[0-9]+\\s?[0-9]+\\,[0-9][0-9])$";
    private static String extDescRegex = "^[^\\+\\-/][а-яА-Яa-zA-Z]+$";
    private static String categoryRegex = "^[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} \\/ ([0-9]{5,7}) ([а-яА-Яa-zA-Z ]+)";
    private static String endingLineRegex = "^([\\+]?[0-9]+\\s?[0-9]+\\,[0-9][0-9]) ([0-9]+\\s?[0-9]+\\,[0-9][0-9])";



    private static void extractOperationsFromPdf(String pdfFilePath) {
        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            //document.getClass();
            if (!document.isEncrypted()) {
                // Pre compile patterns
                Pattern startingPattern = Pattern.compile(startingLineRegex);
                Pattern extPattern = Pattern.compile(extDescRegex);
                Pattern categoryPattern = Pattern.compile(categoryRegex);

                // Load PDF file
                PDFTextStripper tStripper = new PDFTextStripper();
                tStripper.setSortByPosition(true);
                String pdfFileInText = tStripper.getText(document);
                String[] lines = pdfFileInText.split("\\r?\\n");

                boolean isOperationOpened = false;
                BankOperation currentOperation = new BankOperation();
                List<BankOperation> operations = new ArrayList<>();

                for (String line : lines) {
                    Matcher startingMatcher = startingPattern.matcher(line);
                    if (startingMatcher.find()) {
                        if(isOperationOpened) {
                            // close previous operation
                            System.out.print("Closing previous operation..");
                            operations.add((BankOperation) currentOperation.clone());
                        }

                        System.out.print("Opening operation..");
                        System.out.println(line.substring(startingMatcher.start(), startingMatcher.end()));
                        String dateStr = startingMatcher.group(0);
                        currentOperation = new BankOperation();

                        // Parse date
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
                        try {
                            Date date = formatter.parse(dateStr);
                            System.out.println(formatter.format(date));
                            currentOperation.setOpDate(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        currentOperation.setDescription(startingMatcher.group(1));

                        if (startingMatcher.groupCount() > 2) {
                            String amountStr = startingMatcher.group(2);
                            if (amountStr.startsWith("+")) {
                                currentOperation.setIncome(true);
                                String resultAmountStr = amountStr.substring(1).replaceAll(" ", "")
                                        .replaceAll(",",".");
                                double amountCasted = Double.parseDouble(resultAmountStr);
                                currentOperation.setAmount(amountCasted);
                            }
                        }
                        isOperationOpened = true;

                        continue;
                    }

                    if(isOperationOpened) {
                        Matcher extMatcher = extPattern.matcher(line);
                        if (extMatcher.find()) {
                            System.out.print("Ext string: ");
                            System.out.println(line.substring(extMatcher.start(), extMatcher.end()));
                            continue;
                        }
                        Matcher catMatcher = categoryPattern.matcher(line);
                        if (catMatcher.find()) {
                            System.out.print("Cat string: ");
                            System.out.println(line.substring(catMatcher.start(), catMatcher.end()));
                            continue;
                        }
                    }
                    //System.out.println(line);
                }

                System.out.println("DONE");

//                PDFTableExtractor extractor = new PDFTableExtractor();
//                List<Table> tables = extractor.setSource(pdfFilePath)
//                        .addPage(0)
//                        .addPage(1)
//                        .exceptLine(new int[] {0,1,2,3,4,5,6}) //the first line in each page
//                        .extract();
//                String html = tables.get(0).toHtml();//table in html format
//                String csv = tables.get(0).toString();//table in csv format using semicolon as a delimiter


            }

        } catch (InvalidPasswordException e) {
            e.printStackTrace();
        } catch (IOException | CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args) {
        log.info("S.I.L.E.S.T.A project is starting up, ...");
        boolean RECALCULATE = true;

        extractOperationsFromPdf("D:\\silesta\\docs\\Document-2019-04-24-124711.pdf");

//
//
//        if(RECALCULATE) {
//
//            NeuralNetwork neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,3, 50, 1);
//            neuralNetwork.setLearningRule(new ResilientPropagation());
//            DataSet trainingSet;
//            trainingSet = DataSet.createFromFile("data.csv", 3, 1, ",", true);
//
//            // norm = new ZeroMeanNormalizer();
//            MaxMinNormalizer norm = new MaxMinNormalizer();
//            norm.normalize(trainingSet);
//
//            ResilientPropagation rule = (ResilientPropagation) neuralNetwork.getLearningRule();
//            //rule.setMaxIterations(10000000);
//            rule.setMaxError(0.05);
//
//            Runnable runnable = () -> neuralNetwork.learn(trainingSet);
//            Thread t = new Thread(runnable);
//            t.start();
//
//            while (!rule.isStopped()) {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.printf("iteration: %s\n", rule.getCurrentIteration());
//                System.out.printf("Prev error : %s\n", rule.getPreviousEpochError());
//                System.out.printf("Overall error: %s\n", rule.getTotalNetworkError());
//            }
//
//            // save the trained network into file
//            neuralNetwork.save("net.nnet");
//            GraphmlExport ge = new GraphmlExport(neuralNetwork);
//            ge.parse();
//            ge.writeToFile("graphml.xml");
//
//        }
//
//        // load the saved network
//        NeuralNetwork neuralNetworkDone = NeuralNetwork.createFromFile("net.nnet");
//        // set network input
//        neuralNetworkDone.setInput(0, 1, 3);
//        neuralNetworkDone.calculate();
//        double[] networkOutput = neuralNetworkDone.getOutput();
//        System.out.printf("%s", Arrays.toString(networkOutput));
//
//        neuralNetworkDone.setInput(1, 1, 3);
//        neuralNetworkDone.calculate();
//        double[] networkOutput2 = neuralNetworkDone.getOutput();
//        System.out.printf("%s", Arrays.toString(networkOutput2));
//
//        neuralNetworkDone.setInput(1, 2, 3);
//        neuralNetworkDone.calculate();
//        double[] networkOutput3 = neuralNetworkDone.getOutput();
//        System.out.printf("%s", Arrays.toString(networkOutput3));

    }
}

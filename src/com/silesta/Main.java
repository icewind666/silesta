package com.silesta;

import com.silesta.models.Tables;
import com.silesta.models.tables.BankOperations;
import com.silesta.models.tables.records.BankOperationsRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.sql.*;
import java.util.stream.Collectors;

import org.jooq.*;
import org.jooq.impl.DSL;

import static com.silesta.models.Tables.BANK_OPERATIONS;


public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());
    private static String startingLineRegex = "^([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) ?([^\\/][а-яa-zА-ЯA-Z\\s\\w\\-\\@0-9\\*\\.]+) (.*)$";
    private static String extDescRegex = "^([^\\+\\-/][а-яА-Яa-zA-Z\\@\\w]+)$";
    private static String categoryRegex = "^[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} \\/ ([0-9\\-\\s]{1,7}) ([а-яА-Яa-zA-Z ]+)";
    private static String endingLineRegex = "(\\+?.*[0-9]+\\,[0-9][0-9]) (.*[0-9]+\\,[0-9][0-9])";
    private static String periodLineRegex = "([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) - ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})";
    private static String cardLineRegex = "Выписка по счету дебетовой карты (.*)";


    public static DSLContext CTX;
    private static String CARD_SOURCE;


    private static List<BankOperationsRecord> extractOperationsFromPdf(String pdfFilePath) {
        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            if (!document.isEncrypted()) {
                // Pre compile patterns
                Pattern startingPattern = Pattern.compile(startingLineRegex);
                Pattern extPattern = Pattern.compile(extDescRegex);
                Pattern categoryPattern = Pattern.compile(categoryRegex);
                Pattern amountsPattern = Pattern.compile(endingLineRegex);
                Pattern periodPattern = Pattern.compile(periodLineRegex);
                Pattern cardPattern = Pattern.compile(cardLineRegex);

                // Load PDF file
                String[] lines = loadPdfLines(document);
                java.sql.Date periodStart;
                java.sql.Date periodEnd;

                boolean isOperationOpened = false;
                BankOperationsRecord currentOperation = CTX.newRecord(BANK_OPERATIONS);
                currentOperation.setSource(CARD_SOURCE);
                List<BankOperationsRecord> operations = new ArrayList<>();

                for (String line : lines) {
                    Matcher cardMatcher = cardPattern.matcher(line);
                    if (cardMatcher.find()) {
                        CARD_SOURCE = cardMatcher.group(1);
                        continue;
                    }

                    // Checking period of pdf doc operations.
                    // Operations in given source are replaced by new ones
                    Matcher periodMatcher = periodPattern.matcher(line);
                    if (periodMatcher.find()) {
                        String dateStartStr = periodMatcher.group(1);
                        String dateEndStr = periodMatcher.group(2);
                        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                        try {
                            Date dateStart = formatter.parse(dateStartStr);
                            Date dateEnd = formatter.parse(dateEndStr);
                            periodStart = new java.sql.Date(dateStart.getTime());
                            periodEnd = new java.sql.Date(dateEnd.getTime());

                            // clear period in database only with this source
                            CTX.deleteFrom(BANK_OPERATIONS).where(
                                    BANK_OPERATIONS.OP_DATE.greaterOrEqual(periodStart)
                                    .and(BANK_OPERATIONS.OP_DATE.lessOrEqual(periodEnd))
                                    .and(BANK_OPERATIONS.SOURCE.eq(CARD_SOURCE)));
                            continue;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    Matcher startingMatcher = startingPattern.matcher(line);
                    if (startingMatcher.find()) {
                        if(isOperationOpened) {
                            operations.add(currentOperation);
                        }
                        isOperationOpened = true;
                        currentOperation = CTX.newRecord(BANK_OPERATIONS);
                        currentOperation.setSource(CARD_SOURCE);
                        processDateInOp(currentOperation, startingMatcher);
                        currentOperation.setDesc(startingMatcher.group(2));

                        if (startingMatcher.groupCount() > 2) {
                            String amountStr = startingMatcher.group(3);
                            Matcher amountMatcher = amountsPattern.matcher(amountStr);
                            if (amountMatcher.find()) {
                                processAmountInOpLine(currentOperation, amountMatcher);
                            }
                        }
                        continue;
                    }

                    if(isOperationOpened) {
                        if (matchExtDescription(extPattern, currentOperation, line)) continue;
                        if (matchCategory(categoryPattern, currentOperation, line)) continue;
                        if(matchAmount(amountsPattern, currentOperation, operations, line)) {
                            isOperationOpened = false;
                        }
                    }
                }
                System.out.println("DONE");
                System.out.println(String.format("Parsed %s operations.", operations.size()));
                return operations;

            }
        } catch (IOException | CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String[] loadPdfLines(PDDocument document) throws IOException {
        PDFTextStripper tStripper = new PDFTextStripper();
        tStripper.setSortByPosition(true);
        String pdfFileInText = tStripper.getText(document);
        return pdfFileInText.split("\\r?\\n");
    }

    private static void processDateInOp(BankOperationsRecord currentOperation, Matcher startingMatcher) {
        String dateStr = startingMatcher.group(1);
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        try {
            Date date = formatter.parse(dateStr);
            currentOperation.setOpDate(new java.sql.Date(date.getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static void processAmountInOpLine(BankOperationsRecord currentOperation, Matcher amountMatcher) {
        String amountString = amountMatcher.group(1);
        String resultAmountStr;
        if (amountString.startsWith("+")) {
            currentOperation.setIsIncome(true);
            resultAmountStr = amountString.substring(1);
        }
        else {
            resultAmountStr = amountString;
        }
        resultAmountStr = resultAmountStr.replaceAll("[^0-9,]", "");
        resultAmountStr = resultAmountStr.replaceAll(",",".");
        double amountCasted = Double.parseDouble(resultAmountStr);
        currentOperation.setAmount((float)amountCasted);
    }

    private static boolean matchAmount(Pattern amountsPattern, BankOperationsRecord currentOperation, List<BankOperationsRecord> operations, String line) throws CloneNotSupportedException {
        Matcher amountMatcher = amountsPattern.matcher(line);
        boolean result = false;
        if (amountMatcher.find()) {
            String amountString = amountMatcher.group(1);
            if (amountString.startsWith("+")) {
                currentOperation.setIsIncome(true);
                String resultAmountStr = amountString.substring(1).replaceAll(" ", "")
                        .replaceAll(",",".");
                double amountCasted = Double.parseDouble(resultAmountStr);
                currentOperation.setAmount((float)amountCasted);
                result = true;
                operations.add(currentOperation);
            }
        }
        return result;
    }

    private static boolean matchCategory(Pattern categoryPattern, BankOperationsRecord currentOperation, String line) {
        Matcher catMatcher = categoryPattern.matcher(line);
        if (catMatcher.find()) {
            currentOperation.setCatName(catMatcher.group(2));
            long cat_id = 0;
            if(!catMatcher.group(1).equals("-")) {
                cat_id = Long.parseLong(catMatcher.group(1));
            }
            currentOperation.setExtCatId(cat_id);
            return true;
        }
        return false;
    }

    private static boolean matchExtDescription(Pattern extPattern, BankOperationsRecord currentOperation, String line) {
        Matcher extMatcher = extPattern.matcher(line);
        if (extMatcher.find()) {
            currentOperation.setDesc(currentOperation.getDesc().concat(" " + extMatcher.group(1)));
            return true;
        }
        return false;
    }




    public static void main(String[] args) {
        log.info("S.I.L.E.S.T.A project is starting up, ...");
        boolean RECALCULATE = true;

        try  {
            String userName = "silesta";
            String password = "123";
            String url = "jdbc:postgresql://localhost:5432/silestadb";
            Connection conn = DriverManager.getConnection(url, userName, password);
            CTX = DSL.using(conn, SQLDialect.POSTGRES);

            List<Path> filesInFolder = Files.walk(Paths.get("D:\\silesta\\docs\\"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            for (Path pdfFile : filesInFolder) {
                List<BankOperationsRecord> allOperations = extractOperationsFromPdf(pdfFile.toString());
                for (BankOperationsRecord op : allOperations) {
                    op.store();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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

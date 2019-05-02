package com.silesta;

import com.silesta.extractors.SberbankExtractorConfig;
import com.silesta.interfaces.ExtractorConfiguration;
import com.silesta.interfaces.IExtractor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.logging.Logger;


public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());

    //TODO: move this to config file
    private static String POSTGRESQL_DB = "jdbc:postgresql://localhost:5432/silestadb";
    private static String USERNAME = "silesta";
    private static String PASSWORD = "123";
    private static String EXTRACTOR_PATH = "D:\\silesta\\docs\\9430";
    private static DSLContext CTX;


    public static void main(String[] args) {
        log.info("S.I.L.E.S.T.A project is starting up, ...");

        try  {
            Connection conn = DriverManager.getConnection(POSTGRESQL_DB, USERNAME, PASSWORD);
            CTX = DSL.using(conn, SQLDialect.POSTGRES);

            HashMap<String, String> props = new HashMap<>();
            props.put("path", EXTRACTOR_PATH);
            props.put("card_source", "Sberbank Debit");

            ExtractorConfiguration config = new SberbankExtractorConfig(props);
            config.setCTX(CTX);
            IExtractor extr = new BankCardDataExtractor();
            extr.init(config);
            extr.extract();
            log.info("Extraction done");

        }
        catch (Exception e) {
            e.printStackTrace();
        }

//
//         boolean RECALCULATE = true;
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

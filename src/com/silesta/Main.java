package com.silesta;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.silesta.runtime.CommandRunner;
import com.silesta.util.Command;
import com.silesta.voice.SilestaVoice;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
//import sun.util.logging.PlatformLogger;

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
import org.neuroph.util.data.norm.ZeroMeanNormalizer;


public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("S.I.L.E.S.T.A project is starting up, ...");

// create new perceptron network
        NeuralNetwork neuralNetwork = new Perceptron(3,1);
        neuralNetwork.setLearningRule(new ResilientPropagation());
// create training set
        DataSet trainingSet;
// add training data to training set (logical OR function)

//        trainingSet.addRow(new DataSetRow (new double[]{0, 0},
//                new double[]{0}));
//        trainingSet.addRow(new DataSetRow (new double[]{0, 1},
//                new double[]{1}));
//        trainingSet.addRow(new DataSetRow (new double[]{1, 0},
//                new double[]{1}));
//        trainingSet.addRow(new DataSetRow(new double[]{1, 1},
//                new double[]{1}));

        trainingSet = DataSet.createFromFile("data.csv",3,1, ",", true);
        // learn the training set
        ZeroMeanNormalizer norm = new ZeroMeanNormalizer();
        norm.normalize(trainingSet);

        //NeuralNetworkEventListener listener = neuralNetworkEvent -> System.out.println(neuralNetworkEvent.getEventType().name());
        ResilientPropagation rule = (ResilientPropagation)neuralNetwork.getLearningRule();
        rule.setMaxError(0.22);
        Runnable runnable =
                () -> { neuralNetwork.learn(trainingSet); };
        Thread t = new Thread(runnable);
        t.start();
//        neuralNetwork.addListener(listener);
//        neuralNetwork.learn(trainingSet);

        while(!rule.isStopped()) {
            if(rule.getCurrentIteration() % 100000 == 0) {
                System.out.printf("iteration: %s\n", rule.getCurrentIteration());
                System.out.printf("Prev error : %s\n", rule.getPreviousEpochError());
                System.out.printf("Overall error: %s\n", rule.getTotalNetworkError());
            }
        }

        // save the trained network into file
        neuralNetwork.save("net.nnet");
        GraphmlExport ge = new GraphmlExport( neuralNetwork );
        ge.parse();
        ge.writeToFile("graphml.xml");


        // load the saved network
//        NeuralNetwork neuralNetworkDone = NeuralNetwork.createFromFile("or_perceptron.nnet");
//        // set network input
//        neuralNetworkDone.setInput(0, 1, 2);
//        // calculate network
//        neuralNetworkDone.calculate();
//        // get network output
//        double[] networkOutput = neuralNetworkDone.getOutput();
//
//        System.out.printf("%s", Arrays.toString(networkOutput));
//
//
//        SilestaVoice voice = new SilestaVoice();
//
//        if (args.length >= 1) {
//            //user specified a config file
//            Properties p = new Properties();
//
//            try {
//                p.load(new FileInputStream(args[0]));
//                String commandToRun = args[1];
//                CommandRunner runner = new CommandRunner(commandToRun, p);
//                runner.go();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                log.severe("Error: I cant load properties file");
//                log.log(Level.SEVERE, "Exception: ", e);
//            }
//
//        }
    }
}

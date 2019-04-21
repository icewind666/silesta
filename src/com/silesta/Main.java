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
import org.neuroph.util.TransferFunctionType;
import org.neuroph.util.data.norm.MaxMinNormalizer;
import org.neuroph.util.data.norm.ZeroMeanNormalizer;


public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("S.I.L.E.S.T.A project is starting up, ...");
        boolean RECALCULATE = true;



        if(RECALCULATE) {

            NeuralNetwork neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,3, 50, 1);
            neuralNetwork.setLearningRule(new ResilientPropagation());
            DataSet trainingSet;
            trainingSet = DataSet.createFromFile("data.csv", 3, 1, ",", true);

            // norm = new ZeroMeanNormalizer();
            MaxMinNormalizer norm = new MaxMinNormalizer();
            norm.normalize(trainingSet);

            ResilientPropagation rule = (ResilientPropagation) neuralNetwork.getLearningRule();
            //rule.setMaxIterations(10000000);
            rule.setMaxError(0.05);

            Runnable runnable = () -> neuralNetwork.learn(trainingSet);
            Thread t = new Thread(runnable);
            t.start();

            while (!rule.isStopped()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.printf("iteration: %s\n", rule.getCurrentIteration());
                System.out.printf("Prev error : %s\n", rule.getPreviousEpochError());
                System.out.printf("Overall error: %s\n", rule.getTotalNetworkError());
            }

            // save the trained network into file
            neuralNetwork.save("net.nnet");
            GraphmlExport ge = new GraphmlExport(neuralNetwork);
            ge.parse();
            ge.writeToFile("graphml.xml");

        }

        // load the saved network
        NeuralNetwork neuralNetworkDone = NeuralNetwork.createFromFile("net.nnet");
        // set network input
        neuralNetworkDone.setInput(0, 1, 3);
        neuralNetworkDone.calculate();
        double[] networkOutput = neuralNetworkDone.getOutput();
        System.out.printf("%s", Arrays.toString(networkOutput));

        neuralNetworkDone.setInput(1, 1, 3);
        neuralNetworkDone.calculate();
        double[] networkOutput2 = neuralNetworkDone.getOutput();
        System.out.printf("%s", Arrays.toString(networkOutput2));

        neuralNetworkDone.setInput(1, 2, 3);
        neuralNetworkDone.calculate();
        double[] networkOutput3 = neuralNetworkDone.getOutput();
        System.out.printf("%s", Arrays.toString(networkOutput3));

    }
}

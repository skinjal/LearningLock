package io.alstonlin.learninglock;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

public class LockScreenML {

    private static final double OUTPUT_THRESHOLD = 0.5;
    private static final double TRAIN_CONVERGENCE_THRESHOLD = 0.01;
    private BasicNetwork network;
    private int numNeurons;

    private LockScreenML(int numNeurons){
        this.numNeurons = numNeurons;
        this.network = new BasicNetwork();
        this.network.addLayer(new BasicLayer(new ActivationSigmoid(), true, numNeurons));
        this.network.addLayer(new BasicLayer(new ActivationSigmoid(), true, numNeurons));
        this.network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));
        this.network.getStructure().finalizeStructure();
        this.network.reset();
    }

    /**
     * Takes in data to train the Neural Network.
     *
     * @param validTimes Inputs of times that would be valid and should unlock. Should be in a
     *                   n * numNeurons double array of times
     * @param invalidTimes Similar to the first parameter, but with data that should not result in
     *                     an unlock
     */
    private void train(double[][] validTimes, double[][] invalidTimes){
        // Creates input and output arrays
        int total = validTimes.length + invalidTimes.length;
        double[][] output = new double[total][1];
        double[][] input = new double[total][numNeurons];
        for (int i = 0; i < validTimes.length; i++){
            input[i] = validTimes[i];
            output[i][0] = 1;
        }
        for (int i = 0; i < invalidTimes.length; i++){
            input[i + validTimes.length] = invalidTimes[i];
            output[i][0] = 0;
        }
        NeuralDataSet trainingSet = new BasicNeuralDataSet(input, output);
        // Starts training
        final Train train = new ResilientPropagation(network, trainingSet);
        do {
            train.iteration();
        } while(train.getError() > TRAIN_CONVERGENCE_THRESHOLD);
    }

    /**
     * Uses the given time data to predict if they're the real owner or not
     * @param times The time data
     */
    private boolean predict(double[] times){
        MLData input = new BasicMLData(times);
        MLData output =  network.compute(input);
        return output.getData()[0] >= OUTPUT_THRESHOLD;
    }
}

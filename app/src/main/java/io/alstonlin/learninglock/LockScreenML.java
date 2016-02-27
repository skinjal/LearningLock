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
import org.encog.persist.EncogDirectoryPersistence;

import java.io.File;
import java.util.ArrayList;

public class LockScreenML {
    // Constants
    public static final String FILENAME = "learning_lock_saved.eg";
    private static final double OUTPUT_THRESHOLD = 0.5;
    private static final double TRAIN_CONVERGENCE_THRESHOLD = 0.01;
    // Fields
    private ArrayList<double[]> valid;
    private ArrayList<double[]> invalid;
    private BasicNetwork network;
    private int inputLayerCount;

    private LockScreenML(int inputLayerCount){
        this.inputLayerCount = inputLayerCount;
        this.network = new BasicNetwork();
        this.valid = new ArrayList<>();
        this.invalid = new ArrayList<>();
        this.network.addLayer(new BasicLayer(new ActivationSigmoid(), true, inputLayerCount));
        this.network.addLayer(new BasicLayer(new ActivationSigmoid(), true, inputLayerCount));
        this.network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));
        this.network.getStructure().finalizeStructure();
        this.network.reset();
    }

    /**
     * Attemps to load from file if exists.
     * @return The loaded object from file, or null if does not exist
     */
    public static LockScreenML loadFromFile(){
        try {
            BasicNetwork network = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(FILENAME));
            LockScreenML loaded = new LockScreenML(network.getLayerTotalNeuronCount(0));
            loaded.network = network;
            return loaded;
        } catch(Exception e){
            return null;
        }
    }

    /**
     * Adds an entry to the System, which will retrain it and add it to the saved inputs.
     * @param data The data (pattern cell times) of the user as input
     * @param validity If the data was valid, the output
     */
    public void addEntry(double[] data, boolean validity){
        ArrayList<double[]> list = validity ? valid : invalid;
        list.add(data);
        train((double[][])valid.toArray(),(double[][])invalid.toArray());
    }

    /**
     * Uses the given time data to predict if they're the real owner or not
     * @param times The time data
     */
    public boolean predict(double[] times){
        MLData input = new BasicMLData(times);
        MLData output =  network.compute(input);
        return output.getData()[0] >= OUTPUT_THRESHOLD;
    }

    /**
     * Takes in data to train the Neural Network.
     *
     * @param validTimes Inputs of times that would be valid and should unlock. Should be in a
     *                   n * inputLayerCount double array of times
     * @param invalidTimes Similar to the first parameter, but with data that should not result in
     *                     an unlock
     */
    private void train(double[][] validTimes, double[][] invalidTimes){
        // Creates input and output arrays
        int total = validTimes.length + invalidTimes.length;
        double[][] output = new double[total][1];
        double[][] input = new double[total][inputLayerCount];
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
        // Saves everything this is trained
        EncogDirectoryPersistence.saveObject(new File(FILENAME), network);
    }


}

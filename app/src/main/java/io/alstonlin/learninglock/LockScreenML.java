package io.alstonlin.learninglock;

import android.content.Context;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class LockScreenML {
    // Constants
    public static final String FILENAME = "learning_lock_saved.eg";
    private static final double OUTPUT_THRESHOLD = 0.5;
    private static final double TRAIN_CONVERGENCE_THRESHOLD = 0.01;
    // Fields
    private transient Context context;
    private ArrayList<double[]> valid;
    private ArrayList<double[]> invalid;
    private BasicNetwork network;
    private int inputLayerCount;

    private LockScreenML(Context context, int inputLayerCount){
        this.context = context;
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
    public static LockScreenML loadFromFile(Context context){
        FileInputStream fis = null;
        ObjectInputStream is = null;
        LockScreenML result = null;
        try {
            fis = context.openFileInput(FILENAME);
            is = new ObjectInputStream(fis);
            LockScreenML loaded = (LockScreenML) is.readObject();
            loaded.context = context;
            result = loaded;
        } catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (fis != null) fis.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        return result;
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
        save();
    }

    private void save() {
        FileOutputStream fos = null;
        ObjectOutputStream os = null;
        try {
            fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(this);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
                if (fos != null) fos.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }


}

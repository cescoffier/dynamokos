package de.akquinet.gomobile.dynamokos.oracle;

import java.util.Random;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import de.akquinet.gomobile.dynamokos.prediction.Prediction;

/**
 * Oracle implementation.
 * 
 * This class implements the prediction service.
 * It will be exposed as an OSGi service thanks to iPOJO
 * (@Provides annotation)
 * 
 * The instance of this component implementation is created 
 * in the metadata.xml file
 * 
 * This component implementation will be exposed remotely. But
 * this MUST not impact the code ! To achieve that, we just
 * enable the iPOJO property propagation.
 */
@Component(propagation=true)
@Provides
public class Oracle implements Prediction {

    /**
     * The Oracle secret...
     */
    private Random random = new Random();

    /**
     * Uses the oracle to get a prediction.
     * @return a prediction "randomly" choose.
     * @see de.akquinet.gomobile.dynamokos.prediction.Prediction#getPrediction()
     */
    public String getPrediction() {
        int index = random.nextInt(predictions.length - 1);
        return predictions[index];
    }

    /**
     * Uses the oracle to get a prediction for the given question.
     * @param question the question
     * @return a prediction "randomly" choose.
     * @see de.akquinet.gomobile.dynamokos.prediction.Prediction#getPrediction(java.lang.String)
     */
    public String getPrediction(String question) {
        return getPrediction();
    }
}

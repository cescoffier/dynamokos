package de.akquinet.gomobile.dynamokos.prediction;

/**
 * Prediction service use to interact with the Oracle.
 */
public interface Prediction {
    
    /**
     * Possible answers.
     */
    public String[] predictions = new String[] {
            "As I see it, yes",
            "Ask again later",
            "Better not tell you now",
            "Cannot predict now",
            "Concentrate and ask again",
            "Don't count on it",
            "It is certain",
            "It is decidedly so",
            "Most likely",
            "My reply is no",
            "My sources say no",
            "Outlook good",
            "Outlook not so good",
            "Reply hazy, try again",
            "Signs point to yes",
            "Very doubtful",
            "Without a doubt",
            "Yes",
            "Yes - definitely",
            "You may rely on it"
    };

    /**
     * @return a prediction. No question is required as the Oracle has also telepathic skills.
     */
    public String getPrediction();

    /**
     * @param question the question to ask to the oracle.
     * @return a prediction.
     */
    public String getPrediction(String question);

}

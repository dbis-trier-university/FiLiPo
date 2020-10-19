import RecordLinkage.AlignmentProcessor;
import Utils.Loader.ConfigurationLoader;

import java.util.Scanner;

public class FiLiPoConsole {
    private static int initRequests = ConfigurationLoader.getCandidateRequests();
    private static int simRequests = ConfigurationLoader.getSimilarityRequests();
    private static double strSim = ConfigurationLoader.getStringSimilarity();
    private static double recSim = ConfigurationLoader.getRecordSimilarity();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("---------------");
        System.out.println("| FiLiPo");
        System.out.println("---------------");

        System.out.print("Knowledge Base: ");
        String dbName = sc.nextLine().trim();

        System.out.print("API: ");
        String apiName = sc.nextLine().trim();

        System.out.print("Similarity Requests (>= "+initRequests+") ["+simRequests+"]: ");
        String tmp = sc.nextLine();
        int similarityRequests;
        if(tmp.equals("")){
            similarityRequests = Math.max(simRequests, initRequests);
        } else {
            similarityRequests = Math.max(Integer.parseInt(tmp), initRequests);
        }

        double stringSimThreshold, recordSimThreshold;
        if(ConfigurationLoader.getMode() == 1){
            System.out.print("String Similarity Threshold ["+ConfigurationLoader.getStringSimilarity()+"]: ");
            tmp = sc.nextLine();
            stringSimThreshold = tmp.equalsIgnoreCase("") ? strSim : Double.parseDouble(tmp);

            System.out.print("Record Similarity Threshold ["+ConfigurationLoader.getRecordSimilarity()+"]: ");
            tmp = sc.nextLine();
            recordSimThreshold = tmp.equalsIgnoreCase("") ? recSim : Double.parseDouble(tmp);
            System.out.println("---------------");
        } else {
            stringSimThreshold = strSim;
            recordSimThreshold = recSim;
        }

        final long timeStart = System.currentTimeMillis();
        AlignmentProcessor detector = new AlignmentProcessor(apiName, dbName, initRequests,
                similarityRequests, stringSimThreshold, recordSimThreshold);

        detector.startRelationAligning(ConfigurationLoader.isInSupportMode());

        final long timeEnd = System.currentTimeMillis();
        long time = ((timeEnd - timeStart) / 1000);
        long minutes = (time / 60);
        long seconds = (time % 60);
        System.out.println("Done: " + minutes + ":" + (seconds < 10 ? "0" + seconds : seconds) + " Minutes");
    }
}

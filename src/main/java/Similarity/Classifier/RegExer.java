package Similarity.Classifier;

import java.util.Objects;

public class RegExer {
    public static boolean isEqual(String id1, String id2, String filter){
        if(filter != null && filter.contains("/i")){
            filter = filter.replace("/i","");
            return id1.replaceAll(filter,"").equalsIgnoreCase(id2.replaceAll(filter,""));
        } else if(filter != null && filter.contains("/f")) {
            return true;
        } else {
            return id1.replaceAll(Objects.requireNonNull(filter),"").equals(id2.replaceAll(filter,""));
        }
    }
}

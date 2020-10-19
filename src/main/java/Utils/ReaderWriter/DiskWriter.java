package Utils.ReaderWriter;

import com.opencsv.CSVWriter;
import org.apache.commons.lang3.tuple.Triple;

import java.io.*;
import java.util.List;

public class DiskWriter<L,M,R> {
    String path;

    public DiskWriter(String path){
        this.path = path;
    }

    public static void initPathWithReset(String path){
        File yourFile;
        if (path.lastIndexOf("/") > 0) {
            yourFile = new File(path.substring(0,path.lastIndexOf("/")));
            yourFile.mkdirs();
            yourFile = new File(path);
            yourFile.delete();
            try {
                yourFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            yourFile = new File(path);
            yourFile.delete();
            try {
                yourFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static BufferedWriter init(String path) throws FileNotFoundException {
        new File(path.substring(0,path.substring(4).indexOf("/") + 4)).mkdirs();

        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);

        return new BufferedWriter(new OutputStreamWriter(fos));
    }

    public static void writeLineToFile(String path, String line) throws IOException {
        initPathWithReset(path);
        File fout = new File(path);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(line);
        bw.flush();
        bw.close();
    }

    public static void writeCsv(String path, List<String[]> list){
        try {
            File file = new File(path.substring(0,path.lastIndexOf("/")));
            file.mkdirs();
            FileWriter outputFile = new FileWriter(path);
            CSVWriter writer = new CSVWriter(outputFile);

            for(String[] line : list){
                writer.writeNext(line);
            }

            writer.close();
            outputFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


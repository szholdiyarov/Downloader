import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;

public class DownloadMain {
    private static String fileDir;

    public static  void openChooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = chooser.showDialog(null, "Открыть файл");
        if (ret == JFileChooser.APPROVE_OPTION) {
            fileDir = chooser.getSelectedFile().toString();
            System.out.println(fileDir);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(10);

        openChooseDirectory(); // Specify directory
        Download download = new Download("https://vk.com/feed?section=updates", fileDir,false);

        /* 2 versions */
        pool.submit(download);
        pool.submit(new  Download("http://zholdiyarov.zz.mu/img/avstr.gif", fileDir,false));

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }
}

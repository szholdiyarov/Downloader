import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Download implements Runnable {
    private static final int BUFFER_SIZE = 4096;
    private String fileURL;
    private String saveDir;
    private String saveFilePath;
    private URL url;
    private HttpURLConnection httpConn;
    private int responseCode;
    private int contentLength;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    private int bytesRead;
    private byte[] buffer;
    private long downloadedFileSize;
    private double currentProgress;

    public Download(String fileURL, String saveDir) {
        this.fileURL = fileURL;
        this.saveDir = saveDir;
    }

    private void print(String message) {
        System.out.println("System message : " + message);
    }

    private void printExceptionMessage(String... exception) {
        print("!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!");
        print("EXCEPTION IN METHOD  '" + exception[0] + "'");
        print("EXCEPTION IS '" + exception[1] + "'");
        print("!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!");
    }

    public void downloadFile() throws IOException {
        print("start downloading " + fileURL);
        url = new URL(fileURL);
        httpConn = (HttpURLConnection) url.openConnection();
        responseCode = httpConn.getResponseCode();

        /** check HTTP response code **/
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            contentLength = httpConn.getContentLength();

            if (disposition != null) { // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
                if (fileName.contains("?")) {
                    print("couldn't get file name. File name is now set to unknown for "
                            + fileURL);
                    fileName = "uknown";
                }
            }

            /** opens input stream from the HTTP connection **/
            inputStream = httpConn.getInputStream();
            saveFilePath = saveDir + File.separator + fileName;

            /** opens an output stream to save into file **/
            outputStream = new FileOutputStream(saveFilePath);

            bytesRead = -1;
            buffer = new byte[BUFFER_SIZE];
            downloadedFileSize = 0;
            currentProgress = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                downloadedFileSize += bytesRead;
                currentProgress = (double) downloadedFileSize
                        / (double) httpConn.getContentLengthLong() * 100;
                print("Current progress "
                        + String.format("%.1f", currentProgress) + "%");
                outputStream.write(buffer, 0, bytesRead);
            }

            try {

                MessageDigest md = MessageDigest.getInstance("SHA1");
                FileInputStream fis = new FileInputStream(saveFilePath);
                byte[] dataBytes = new byte[1024];

                int nread = 0;

                while ((nread = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                }
                ;

                byte[] mdbytes = md.digest();

                // convert the byte to hex format
                StringBuffer sb = new StringBuffer("");
                for (int i = 0; i < mdbytes.length; i++) {
                    sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
                            .substring(1));
                }


                PrintWriter writer = new PrintWriter(saveFilePath.substring(0, saveFilePath.length() - fileName.length()) + fileName + ".check", "UTF-8");

                writer.append(sb);
                writer.close();

            } catch (NoSuchAlgorithmException e) {
                printExceptionMessage("downloadFile(...)", e.getMessage());
            }

            outputStream.close();
            inputStream.close();

            print("File downloaded");

        } else {
            print("No file to download. Server replied HTTP code: "
                    + responseCode);
        }
        httpConn.disconnect();
    }

    @Override
    public void run() {
        try {
            downloadFile();
        } catch (IOException e) {
            printExceptionMessage("run(...)", e.getMessage());
        }

    }
}

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * created by szholdiyarov on 22.05.2015
 * PURPOSE: This class is used for downloading any file to your specified directory.
 * Taken parameters are : url of the file or web page and specified directory.
 * <p>
 * © All rights reserved 2015.
 * DISCLAIMER: You can use this code "as is". I have no responsibilities on what you are doing and how.
 * This code is for demonstration purposes only.
 */

public class Download implements Runnable {

    /* Declaring all the variables */
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
    private boolean isProxyAllowed;

    /* Getters and setters(might be useful) */
    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    /* Constructor with parameters */
    public Download(String fileURL, String saveDir, boolean isProxyAllowed) {
        this.fileURL = fileURL;
        this.saveDir = saveDir;
        this.isProxyAllowed = isProxyAllowed;
    }

    /* Used to print any messages */
    private void print(String... message) {
        if (message.length > 1) {
            System.out.println(message[0] + " : " + message[1]);
        } else {
            System.out.println("System message : " + message[0]);
        }
    }

    /* Used to print any exception messages */
    private void printExceptionMessage(String... exception) {
        print("!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!");
        print("EXCEPTION IN METHOD  '" + exception[0] + "'");
        print("EXCEPTION IS '" + exception[1] + "'");
        print("!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!=!");
    }

    /* Download file */
    public void downloadFile() throws IOException {
        if (isProxyAllowed) { // check if proxy is specified
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("pro", 9999));
            httpConn = (HttpURLConnection) url.openConnection(proxy);
            startDownload();
        } else {
            url = new URL(fileURL);
            httpConn = (HttpURLConnection) url.openConnection();
            startDownload();
        }

    }

    /* Actual download file */
    private void startDownload() throws IOException{
        print("start downloading " + fileURL);
        url = new URL(fileURL);

        responseCode = httpConn.getResponseCode();

        /* check HTTP response code */
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
                /* extracts file name from URL */
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
                /* If name is not in good format */
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
                /* Calculate progress */
                downloadedFileSize += bytesRead;
                currentProgress = (double) downloadedFileSize
                        / (double) httpConn.getContentLengthLong() * 100;
                print(fileURL, "current progress "
                        + String.format("%.1f", currentProgress) + "%" + "! " + downloadedFileSize + "bytes out of " + httpConn.getContentLength() + "bytes.");
                outputStream.write(buffer, 0, bytesRead);
            }

            /* calculate checksum of the downloaded file */
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                FileInputStream fis = new FileInputStream(saveFilePath);
                byte[] dataBytes = new byte[1024];

                int nread = 0;

                while ((nread = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                }
                byte[] mdbytes = md.digest();

                /* convert the byte to hex format */
                StringBuffer sb = new StringBuffer("");

                for (int i = 0; i < mdbytes.length; i++) {
                    sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
                            .substring(1));
                }

                /* Save checksum to file */
                PrintWriter writer = new PrintWriter(saveFilePath.substring(0, saveFilePath.length() - fileName.length()) + fileName + ".check", "UTF-8");

                writer.append(sb);
                writer.close();

            } catch (NoSuchAlgorithmException e) {
                printExceptionMessage("downloadFile(...)", e.getMessage());
            }

            outputStream.close();
            inputStream.close();

            print(fileURL, "File downloaded");

        } else { // response code is not ok
            printExceptionMessage("download()", "No file to download. Server replied HTTP code: "
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

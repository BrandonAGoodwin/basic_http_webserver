import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class JavaHTTPServer implements Runnable {

    static final File WEB_ROOT = new File (".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";

    // Port to listen connection
    static final int PORT = 8080;

    // Verbose mode
    static final boolean verbose = true;

    // Client Connection Socket
    private Socket socket;
    
    public JavaHTTPServer(Socket _socket) {
        socket = _socket;
    }

    public static void main (String [] args){
        try{
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started. \nListening for connections on port: " + PORT + "...\n");

            while(true) {
                JavaHTTPServer myServer = new JavaHTTPServer(serverSocket.accept());

                if(verbose){
                    System.out.println("Connection opened. (" + new Date() + ")");
                }

                Thread thread = new Thread(myServer);
                thread.start();

            }
        }catch(IOException e){
            System.err.println("Server connection error: " + e.getMessage());   
        }
    }

    @Override
    public void run() {
    
        // Read characters via the input stream
        BufferedReader fromServer = null;
        // Create character output stream to client (for headers)
        PrintWriter toServer = null;
        // Get binary output stream to client (for requested data)
        BufferedOutputStream dataToServer = null;

        String fileRequested = null;

        try {
            fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toServer = new PrintWriter(socket.getOutputStream());
            dataToServer = new BufferedOutputStream(socket.getOutputStream());

            String input = fromServer.readLine();

            // Remove this
            /* while(!input.isEmpty()){
                System.out.println(input);
                input = fromServer.readLine();
            } */

            // Breaks down string into individual words
            StringTokenizer parse = new StringTokenizer(input);
            // Get the HTTP method of the client
            String method = parse.nextToken().toUpperCase(); 
            // We get the file requested
            fileRequested = parse.nextToken().toLowerCase();

            // We support only GET and HEAD methods, we check
            if(!method.equals("GET") &&  !method.equals("HEAD")){
                if(verbose){
                    System.out.println("501 Not implemented: " + method);
                }

                // We return the not supported file to the client
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                // Set type of file.length() to int as it is of long type
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                // Read content to return to client
                byte [] fileData = readFileData(file, fileLength);

                // We send HTTP headers with data to client
                toServer.println("HTTP/1.1 501 Not Implemented");
                toServer.println("Server: Java HTTP Server");
                toServer.println("Date: " + new Date());
                toServer.println("Content-type: " + contentMimeType);
                toServer.println("Content-length: " + fileLength);
                toServer.println(); // Blank line inbetween headers and content
                toServer.flush();
                // File
                dataToServer.write(fileData, 0, fileLength);
                dataToServer.flush();

                return;
            }else{
                // GET or HEAD method
                if(fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }

                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);

                if(method.equals("GET")) { // Get method so we return content
                    byte[] fileData = readFileData(file, fileLength);

                    // Send HTTP Headers
                    toServer.println("HTTP/1.1 200 OK");
                    toServer.println("Server: Java HTTP Server");
                    toServer.println("Date: " + new Date());
                    toServer.println("Content-type: " + content);
                    toServer.println("Content-length: " + fileLength);
                    toServer.println(); // Blank line inbetween headers and content
                    toServer.flush();

                    dataToServer.write(fileData, 0, fileLength);
                    dataToServer.flush();
                }

                if (verbose) {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }

            }

        } catch (FileNotFoundException fnfe) {
            try {
                fileNotFound(toServer, dataToServer, fileRequested);
            } catch(IOException ioe){
                System.err.println("Error with the file not found exception: " + ioe.getMessage());
            } 

        } catch (IOException e) {
            System.err.println("Server error: " + e); 
        } finally {
            try{
                fromServer.close(); // Close character input stream
                toServer.close();
                dataToServer.close(); // Try implement try-resource-catch
                socket.close();// We clsoe the socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream: " + e.getMessage());
            }

            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
    
        byte[] fileData = new byte[fileLength];

        try(FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(fileData);
        }

        return fileData;
    }

    // Get supported MIME types
    private String getContentType(String fileRequested){
        if(fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")){
            return "text/html";
        }else{
            return "text/plain";
        }
    }

    private void fileNotFound(PrintWriter toServer, OutputStream dataToServer, String fileRequested) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);

        toServer.println("HTTP/1.1 404 Not FileNotFound");
        toServer.println("Server: Java HTTP Server");
        toServer.println("Date: " + new Date());
        toServer.println("Content-type: " + content);
        toServer.println("Content-length: " + fileLength);
        toServer.println(); // Blank line inbetween headers and content
        toServer.flush();

        dataToServer.write(fileData, 0, fileLength);
        dataToServer.flush();
        
        if(verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }
    
}
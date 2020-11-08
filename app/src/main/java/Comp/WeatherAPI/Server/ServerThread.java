package Comp.WeatherAPI.Server;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class ServerThread extends Thread {

    protected DataInputStream inputStream;
    protected DataOutputStream outputStream;
    protected Socket socket;
    private String lines = "";
    private Authentication auth;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    // TODO: Server Thread. Run. IO Error/ Client Thread-1 terminated abruptly
    public void run() {
        try {
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("IO successfully initiated");
        } catch (IOException e) {
            System.err.println("Server Thread. Run. IO error in server thread");
        }

        try {
            auth = Authentication.getInstance();

            String username = TCP.readAuthUsernameOrAnswer(inputStream);
            System.out.println(username);
            assert username != null;

            if (!auth.validateUser(username)) {
                TCP.writeAuthMessage(outputStream, AuthenticationMessages.Auth_Fail, "User doesn't exist.");
                return;
            }
            Authentication.AuthenticationResult authRes = auth.authorize(username, this);
            boolean authenticated = authRes.authenticated;
            TCP.writeAuthMessage(outputStream, authRes.result, authRes.customMessage); // Sending token or fail depends on auth
            //If auth successful, move on
            if (authenticated) {

                Socket dataSocket;
                //From now on we should only receive query requests
                ServerSocket dataServerSocket = new ServerSocket(0); // Available random port
                TCP.passDataSocketInfo(outputStream, dataServerSocket.getLocalPort());
                dataSocket = dataServerSocket.accept();

                System.out.println("A data connection was established with client " + dataSocket.getRemoteSocketAddress() + " at port " + dataSocket.getLocalPort());
                // A while can be added here
                Query query = TCP.readQuery(inputStream);
                if(query == null){
                    outputStream.writeInt(-1);
                }else{
                    File response = query.sendQuery();
                    outputStream.writeInt(response.hashCode());
                    outputStream.flush();
                    FileInputStream fileInputStream = new FileInputStream(response);
                    byte[] fileData = new byte[(int) response.length()];
                    fileInputStream.read(fileData);
                    DataOutputStream dataOutputStream = new DataOutputStream(dataSocket.getOutputStream());
                    dataOutputStream.write(fileData);
                    dataOutputStream.flush();
                    // outputStream.writeInt(response.hashCode()); Dosya adini yolla

                }

            }
        } catch (IOException e) {

            System.err.println("Server Thread. Run. IO Error/ Client " + this.getName() + " terminated abruptly");
            System.err.println(e);
        } catch (NullPointerException e) {
            System.err.println("Server Thread. Run.Client " + this.getName() + " Closed");
        } finally {
            try {
                // TODO: End session
                System.out.println("Closing the connection");
                if (inputStream != null) {
                    inputStream.close();
                    System.err.println(" Socket Input Stream Closed");
                }
                if (outputStream != null) {
                    outputStream.close();
                    System.err.println("Socket Out Closed");
                }
                if (socket != null) {
                    socket.close();
                    System.err.println("Socket Closed");
                }
            } catch (IOException ie) {
                System.err.println("Socket Close Error");
            }
        }
    }
}

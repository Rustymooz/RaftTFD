import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.util.List;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Main {
    private static final String path = "replicas.txt";

    List<String> file;
    private int line;

    private InetAddress addr;
    private int port;

    private Socket sckt;
    private ServerSocket srvr;
    private DataInputStream inpt;
    private DataOutputStream oupt;

    public Main(String[] args) {

        try {
            line = Integer.parseInt(args[0]);
            file = Files.readAllLines(Paths.get(path));

            //TODO: line format is: IP space PORT
            String[] splt = file.get(line).split("\s+");

            addr = InetAddress.getByName(splt[0]);
            port = Integer.parseInt(splt[1]);

            srvr = new ServerSocket(port);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Argument count is zero");
        }

        Main raft = new Main(args);

        ServerLeader serverLeader = new ServerLeader(1026, "Berlindes");

        Replicas replica1 = new Replicas("Berlindes");
        Replicas replica2 = new Replicas("Berlindolas");
    }

}
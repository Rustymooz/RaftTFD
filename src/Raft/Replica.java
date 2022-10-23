package Raft;


import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executor;

public class Replica{

    private static final String path = "replicas.txt";

    List<String> file;
    private int line;

    private InetAddress addr;
    private int port;

    private Executor exe;

    public Replica(String[] args) {

        try {

            int port = getPort(args[0]);

            System.out.println(line);

            ClientRMI clientRMI = new ClientRMI(port, Integer.toString(line));

            Scanner in = new Scanner(System.in);
            while(true){
                System.out.println("Input > ");
                String line = in.nextLine();

                String[] split = line.split("\s+");

                String id = split[0];
                String requestLabel = split[1];
                String requestData = split[2];



                if(line.equalsIgnoreCase("exit")) break;

                String result = clientRMI.Invoke(id, requestLabel, requestData);
                System.out.println("Response of replica " + id + ":" + result);
            }

            //ClientRMI clientRMI = new ClientRMI(port);
            //clientRMI.Invoke(identifier, "GET", "N");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)  {

        if (args.length == 0) {
            System.err.println("Argument count is zero");
            System.exit(1);
        }

        Replica raft = new Replica(args);
    }

    public int getPort(String line) throws IOException {

        this.line = Integer.parseInt(line);
        file = Files.readAllLines(Paths.get(path));

        //TODO: line format is: IP space PORT
        String[] splt = file.get(this.line).split("\s+");

        addr = InetAddress.getByName(splt[0]);
        port = Integer.parseInt(splt[1]);

        return port;
    }
}
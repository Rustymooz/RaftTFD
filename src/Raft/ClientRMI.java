package Raft;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class ClientRMI implements Handler {

    private static final String path = "replicas.txt";
    private Registry registry;
    private Handler stub;

    private int port;

    public ClientRMI(int port, String id) throws RemoteException, NotBoundException {
        ServerRMI serverRMI = new ServerRMI(port, id);
        this.port = port;
        registry = LocateRegistry.getRegistry(this.port);

    }

    public String Invoke(String id, String requestLabel, String requestData) throws RemoteException, NotBoundException {

        registry = LocateRegistry.getRegistry(getPort(id));
        stub = (Handler) registry.lookup(id);
        String result = stub.Invoke(id, requestLabel, requestData);
        System.out.println(result);

        return result;
    }

    // Codigo forte
    public int getPort(String line) {
        try {
            int file_line;
            int porta;

            file_line = Integer.parseInt(line);
            List<String> file = Files.readAllLines(Paths.get(path));

            //TODO: line format is: IP space PORT
            String[] splt = file.get(file_line).split("\s+");

            porta = Integer.parseInt(splt[1]);

            return porta;
        }catch (Exception e){
            System.out.println(e.toString());
        }
        return 0;
    }
}

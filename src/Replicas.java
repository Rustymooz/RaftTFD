import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Replicas implements Handler{

    public Replicas(String identifier) {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1026);
            Handler stub =  (Handler) registry.lookup(identifier);
            for (String list: registry.list()) {
                System.out.println("LISTAS: " + list);
            }
            registry.bind("Wagoogus", stub);
            String response = stub.sayHello();
            System.out.println("response: " + response);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public String sayHello() throws RemoteException {
        return "Wagooguer";
    }
}
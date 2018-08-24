package ugcs.ucsHub;


import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.client.ClientSession;

public class ClientSessionEx extends ClientSession {
    public ClientSessionEx(Client client) {
        super(client);
    }

    public int getClientId() {
        return super.clientId;
    }
}

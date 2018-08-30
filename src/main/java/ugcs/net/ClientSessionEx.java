package ugcs.net;


import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.client.ClientSession;

class ClientSessionEx extends ClientSession {
    ClientSessionEx(Client client) {
        super(client);
    }

    int getClientId() {
        return super.clientId;
    }
}

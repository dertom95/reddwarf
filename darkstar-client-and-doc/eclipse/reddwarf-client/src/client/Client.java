package client;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.util.Properties;

import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.ClientChannelListener;
import com.sun.sgs.client.simple.SimpleClient;
import com.sun.sgs.client.simple.SimpleClientListener;

public class Client implements SimpleClientListener {

    public final SimpleClient _simpleClient = new SimpleClient(this); 
    public static void main(String[] args) {
            // TODO Auto-generated method stub
            final Client client = new Client();
            
            final Properties properties = new Properties( System.getProperties() );
            
            properties.setProperty("port","62964");
            properties.setProperty("host","localhost");
            
            try {
                    client._simpleClient.login(properties);
                    try {
                            synchronized (client) {
                                    client.wait(10000L);
                            }
                    } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                    }
            } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
    }
	@Override
	public void disconnected(boolean arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ClientChannelListener joinedChannel(ClientChannel arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void receivedMessage(ByteBuffer arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void reconnected() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void reconnecting() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		// TODO Auto-generated method stub
		return new PasswordAuthentication("Thomas", new char[]{'a'});
	}
	@Override
	public void loggedIn() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void loginFailed(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
}

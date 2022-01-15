package test;

import java.io.Serializable;
import java.util.Properties;

import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;

public class Test implements AppListener, Serializable{

	@Override
	public void initialize(Properties arg0) {
		// TODO Auto-generated method stub
		System.out.println("INIT SUPERDUPER TEST!");
	}

	@Override
	public ClientSessionListener loggedIn(ClientSession arg0) {
		System.out.println("LOGGED IN!");

		return null;
	}

}

package ch.fhnw.modeller.webservice;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

@Path("/")
public class ServiceRoot extends Application {
	
	@GET
	public String getMsg() {
		return "Hello World !!";
	}

}
package br.com.uerj.rest;

import java.util.LinkedList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import br.com.uerj.DAO.DAO_Dash;

@Path("/processo")
public class ProcessoService {

	@Path("/abertos")
	@GET
	@Produces(MediaType.APPLICATION_FORM_URLENCODED)
	
	public LinkedList<String> getProcessosAbertos(){
		LinkedList<String> lprocessos = DAO_Dash.ListarProcessosValidos();
		
		return lprocessos;
	}
}

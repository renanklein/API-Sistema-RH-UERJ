package br.com.uerj.rest;

import java.util.LinkedList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import br.com.uerj.DAO.DAO_Dash;
import br.com.uerj.model.Vaga;

@Path("/vaga")
public class VagaService {
	@Path("/listar")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	
	public LinkedList<Vaga> vagasAbertas(){
		LinkedList<Vaga> lvagas = DAO_Dash.ListarVagasAbertas();
		
		return lvagas;
	}
}

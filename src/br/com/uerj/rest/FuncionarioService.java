package br.com.uerj.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import br.com.uerj.DAO.DAO_RH1;
import br.com.uerj.DAO.DAO_Util;
import br.com.uerj.config.ConexaoBD;
import br.com.uerj.model.Candidato;
import br.com.uerj.model.Funcionario;
@Path("/funcionario")
public class FuncionarioService {
	
	private static final String CHARSET_UTF8 = ";charset=utf-8";
	
	@GET
	@Path("/getfunc/{matricula}")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF8)
	public Response getOperacao(@PathParam("matricula") String matricula) {
		
		Funcionario func = DAO_RH1.consultaFunc(matricula);
		
		if(func == null) {
			return Response.status(Response.Status.NOT_FOUND).entity("Funcionário de matricula " + matricula + " não encontrado").build();
		}
		else if(!func.isStatus()) {
			return Response.status(Response.Status.BAD_REQUEST).entity("O funcionário de matricula "+ matricula + " já foi exonerado").build();
		}
		
		
		Gson toJson = new Gson();
		
		String json = toJson.toJson(func);
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
		
		//A opcao de operacao do Funcionário deverá ser determinada no front-end
	}
	
	@POST
	@Path("/alterFunc")
	@Consumes(MediaType.APPLICATION_JSON + CHARSET_UTF8)
	public Response processaFunc(String json) {
		Gson gson = new Gson();
		Funcionario func = gson.fromJson(json, Funcionario.class);
		
		//O valor da string opcao e o redirecionamento será determinado pelo vue
		//Antes de disparar essa função
		int id_vaga = DAO_RH1.exoneraFunc(func);
		if(id_vaga < 0 ) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Não foi possivel exonerar o funcionário").build();
		}
		else {
			if(DAO_Util.isConcursoValido(func.getProcesso())) {
				ArrayList<Candidato> lcands = DAO_Util.ProximoCand(func);
				if(lcands.size() == 0) {
					return Response.status(Response.Status.OK).entity("Funcionário exonerado mas não há candidatos disponíveis").build();
				}else if(lcands.size() > 1) {
					
					return Response.status(Response.Status.OK).entity("Ocorreu um empate").build();
				}
				else {
					Candidato ca = lcands.get(0);
					DAO_Util.setStatusVaga(2, func.getIdVaga(), ca.getCPF());
					try {
						ConexaoBD a = new ConexaoBD();
						a.iniciaBd();
						Connection c = a.getConexao();
						c.setAutoCommit(false);
						PreparedStatement ps1 = (PreparedStatement) c.prepareStatement("UPDATE concurso_especialidade "
								+ "SET nu_vacancia = nu_vacancia - 1,nu_banco_restante = nu_banco_restante - 1 where id_concurso_especialidade = ?");
						ps1.setInt(1, ca.getId_espec());
						ps1.executeUpdate();
						ps1 = (PreparedStatement) c.prepareStatement("UPDATE concurso_candidato SET id_situacao = ? WHERE cd_chave_candidato = ?;");
						
						ps1.setInt(1, 2);
						ps1.setString(2,ca.getCPF());
						
						ps1.executeUpdate();
						
						System.out.println("Opa, mudou");
						lcands.get(0).setId_vaga(2);
						lcands.get(0).setEspecialidade("Convocado");
						
						ps1 = (PreparedStatement) c.prepareStatement("INSERT INTO concurso_candidato_historico (id_concurso_especialidade,cd_chave_candidato,id_situacao_antiga,id_situacao_nova,dt_mudanca_situacao) VALUES (?,?,?,?,?);");
						ps1.setInt(1, ca.getId_espec());
						ps1.setString(2, ca.getCPF());
						ps1.setInt(3,ca.getId_situacao());
						ps1.setInt(4,2);
						Calendar cal = Calendar.getInstance();
						java.util.Date d = cal.getTime();
						ps1.setDate(5, new java.sql.Date(d.getTime()));
						
						ps1.executeUpdate();
						
						c.commit();
						
						ps1.close();
						c.close();
						a.fechaBd();
						
						return Response.status(Response.Status.OK).entity("O devido candidato foi selecionado !").build();
					}catch(SQLException s) {
						s.printStackTrace();
						
						return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ocorreu um erro inexperado").build();
					}
					
					
				}
				
			}
			else {
				DAO_Util.setStatusVaga(5, id_vaga, func.getChave());
				return Response.status(Response.Status.OK).entity("Funcionário exonerado mas o concurso expirou").build();
			}
		}
		
		
	}
	
	
	
}


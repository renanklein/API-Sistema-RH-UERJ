package br.com.uerj.rest;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.uerj.DAO.DAO_Dash;
import br.com.uerj.DAO.DAO_Util;
import br.com.uerj.DAO.DAO_mCand;
import br.com.uerj.model.Candidato;


@Path("/candidato")
public class CandidatoService {
	
	//private static final String CHARSET_UTF8 = ";charset=utf-8";
	
	
	public static String ListToJson(LinkedList<Candidato> lcand) {

		Gson Tojson = new Gson();
		
		return Tojson.toJson(lcand,new TypeToken<LinkedList<Candidato>>() {}.getClass());
		
	}
	
	@Path("/convocados")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public LinkedList<Candidato> candidatosConvocados() {
		LinkedList<Candidato> lcands = DAO_Dash.ListarCandidatosConvocados();
		
		
		return lcands;
	}
	
	@Path("/processo")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public LinkedList<Candidato> candidatosProcesso(@QueryParam("cd_processo") String cdProcesso) {
		
		LinkedList<Candidato> lcands = DAO_Dash.ListarCandidatosProcesso(cdProcesso);
		
		
		return lcands;
		
	}
	
	@Path("/aptos")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	
	public LinkedList<Candidato> candidatosAptos() {
		LinkedList<Candidato> lcands = DAO_Dash.ListarCandidatosAptos();
		
		return lcands;
	}
	
	@Path("/empatados/{id_concurso}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	
	public  LinkedList<Candidato> candidatosEmpatados(@PathParam("id_concurso") String id) {
		LinkedList<Candidato> lcands = DAO_Dash.ListarCandidatosEmpatados(Integer.parseInt(id));
		
		
		return lcands;
		
	}
	
	@Path("/selecionar")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	
	public Response selecionarCandidato(@FormParam("idVaga") String id_vaga, @FormParam("idConcurso")
	String id_conc, @FormParam("processo") String processo) 
	{
		String unidade="", lotacao="", localiz="";
		int idVaga = Integer.parseInt(id_vaga);
		int idConcurso = Integer.parseInt(id_conc);
		Calendar ca = Calendar.getInstance();
		java.util.Date d = ca.getTime();
		Date hoje = new java.sql.Date(d.getTime());
		//new SimpleDateFormat("yyyy-MM-dd").format(getTime());
		Date validade = DAO_mCand.SelecionarValidadeProcesso(processo);
		boolean hist;
		if(validade.before(hoje)){
			hist = DAO_mCand.alterarStatusVaga(idVaga, 5);
			return Response.status(Response.Status.BAD_REQUEST).entity("Não foi possível selecionar candidado - Concurso expirado!")
					.build();
		}
		else if(DAO_mCand.sepecionarBanco(idConcurso) < 1){
			hist = DAO_mCand.alterarStatusVaga(idVaga, 5);
			return 	Response.status(Response.Status.BAD_REQUEST).entity("Não foi possível selecionar candidado - Banco Esgotado!")
					.build();
		}
		else{
			Candidato cand = DAO_mCand.SelecionarCandidato(idConcurso);
			if(cand.getEmpate() > 0){
				hist = DAO_mCand.alterarStatusVaga(idVaga, 3);
			    return Response.status(Response.Status.BAD_REQUEST).entity("Não foi possível selecionar candidado - Necessário realizar desempate!")
			    		.build();
			} else
				try {
					if(DAO_mCand.confirmaSelecao(idConcurso, cand.getCPF(), idVaga, unidade, lotacao, localiz)){
						hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 2, 3, "");
						hist = DAO_mCand.escreverHistoricoVaga(idVaga, cand.getCPF(), 3);
						hist = DAO_mCand.alterarStatusVaga(idVaga, 2);
						
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			return Response.status(Response.Status.OK).entity("O candidato "+cand.getNome()+" foi selecionado para a vaga!")
					.build();
		}
	}
	
	@Path("/aptoNomeado")
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	
	public Response setAptoNomeado(@FormParam("json") String candJson,@FormParam("data") String data,@FormParam("portaria") String portaria,
			@FormParam("unidade") String unidade, @FormParam("lotacao") String lotacao, @FormParam("localizacao") String localizacao,
			@FormParam("idVaga") String idv, @FormParam("idConcurso") String idc,@FormParam("matricula") String matricula) 
	{
		Gson toCandidato = new Gson();
		Candidato cand = toCandidato.fromJson(candJson, Candidato.class);
		int idVaga = Integer.parseInt(idv);
		int idConcurso = Integer.parseInt(idc);
		
			if(data != null){
				if(DAO_mCand.nomearFuncionario(cand.getCPF(), portaria, data,unidade,
						lotacao, localizacao)){
					boolean hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 8, 4, portaria);
					hist = DAO_mCand.escreverHistoricoVaga(idVaga, cand.getCPF(), 4);
					hist = DAO_mCand.alterarStatusVaga(idVaga, 1);
					hist = DAO_mCand.atualizarBancoNomeacao(idConcurso);
					return Response.status(Response.Status.OK).entity("Nomeação concluída com sucesso!").build();				
				}
				else{
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Não foi possível conectar ao banco de dados!\n"+
						"Tente novamente, se o problema persistir entre em contato com o suporte.").build();
				}
				
	       }
			else if(matricula != null){
				if(DAO_mCand.alterarEliminadoApto(cand.getCPF(), 8, matricula, idVaga)){
					boolean hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 2, 8, "");
					hist = DAO_mCand.escreverHistoricoVaga(idVaga, cand.getCPF(), 8);
					return Response.status(Response.Status.OK).entity("Alteração realizada com sucesso!")
							.build();
				}
				else{
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Não foi possível conectar ao banco de dados!\n"+
							"Tente novamente, se o problema persistir entre em contato com o suporte.").build();
				}
				
			}
			else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Não foi possivel realizar a operação").build();
			}
	}
	
	@Path("/fimDeFila")
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response setFimDeFila(@FormParam("json") String candJson,@FormParam("idVaga") String idv, @FormParam("idConcurso") String idc,
			@FormParam("processo") String processo, @FormParam("unidade") String unidade, @FormParam("lotacao") String lotacao,
			@FormParam("localizacao") String localizacao) 
	{
		Gson toCandidato = new Gson();
		Candidato cand = toCandidato.fromJson(candJson, Candidato.class);
		int idVaga = Integer.parseInt(idv);
		int idConcurso = Integer.parseInt(idc);
		if(DAO_mCand.processaFimFila(cand.getCPF())){
			boolean hist = DAO_mCand.escreverHistoricoVaga(idVaga, cand.getCPF(), 5);
			if(cand.getStatus().equals("Candidato")){
				hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 1, 5, "");
				return Response.status(Response.Status.OK).entity("Situação alterada com sucesso!").build();
			}
			else{
				hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 2, 5, "");
				hist = DAO_mCand.alterarStatusVaga(idVaga, 4);
				
				//INCLUIR O METODO DE SELECAO DE CANDIDATO
				Calendar ca = Calendar.getInstance();
				java.util.Date d = ca.getTime();
				Date hoje = new java.sql.Date(d.getTime());
				Date validade = DAO_mCand.SelecionarValidadeProcesso(processo);
				if(validade.before(hoje)){
					hist = DAO_mCand.alterarStatusVaga(idVaga, 5);
					return Response.status(Response.Status.BAD_REQUEST).entity("Não foi possível selecionar candidado - Concurso expirado!")
							.build();
				}
				else if(DAO_mCand.sepecionarBanco(idConcurso) < 1){
					hist = DAO_mCand.alterarStatusVaga(idVaga, 5);
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Não foi possível selecionar candidado - Banco Esgotado!")
							.build();
				}
				else{
					Candidato candSelect = DAO_mCand.SelecionarCandidato(idConcurso);
					if(candSelect.getEmpate() > 0){
						hist = DAO_mCand.alterarStatusVaga(idVaga, 3);
						return Response.status(Response.Status.BAD_REQUEST).entity("Não foi possível selecionar candidado - Necessário realizar desempate!")
								.build();
					} else {
						this.selecionarCandidato(idv, idc, processo);
					}
					
				}
			}
		}
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Naõ foi possivel selecionar o candidato - Ocorreu um erro inesperado")
				.build();
			
	}
	
	@Path("/eliminar")
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	
	public Response setEliminado(@FormParam("json") String candJson,@FormParam("idVaga") String idv,
			@FormParam("idConcurso") String idc) 
	{
		int idVaga = Integer.parseInt(idv);
		int idConcurso = Integer.parseInt(idc);
		Gson toCandidato = new Gson();
		Candidato cand = toCandidato.fromJson(candJson, Candidato.class);
		if(DAO_mCand.alterarEliminadoApto(cand.getCPF(), 7, null, 0)){
			boolean hist = DAO_mCand.escreverHistoricoVaga(idVaga, cand.getCPF(), 7);
			hist = DAO_mCand.alterarStatusVaga(idVaga, 4);
			hist = DAO_mCand.atualizarBancoEliminacao(idConcurso);
			if(cand.getStatus().equals("Convocado")){
				hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 2, 7, "");
			}
			else if(cand.getStatus().equals("Apto")){
				hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 8, 5, "");
			}
			this.selecionarCandidato(idv, idc, cand.getProcesso());
			return Response.status(Response.Status.OK).entity("Situação alterada com sucesso!").build();
		}
		else {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Naõ foi possivel selecionar o candidato - Ocorreu um erro inesperado")
					.build();
		}
	}
	
	@Path("/convocar")
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	
	public Response setConvocado(@FormParam("json") String candJson,@FormParam("idVaga") String idv, @FormParam("idConcurso") String idc) {
		int idVaga = Integer.parseInt(idv);
		int idConcurso = Integer.parseInt(idc);
		Gson toCandidato = new Gson();
		Candidato cand = toCandidato.fromJson(candJson, Candidato.class);
		if(DAO_mCand.alterarStatusCandidato(cand.getCPF(), 2)){
			boolean hist = DAO_mCand.escreverHistoricoCand(idConcurso, cand.getCPF(), 3, 2, "");
			hist = DAO_mCand.escreverHistoricoVaga(idVaga, cand.getCPF(), 2);
			return Response.status(Response.Status.OK).entity("Situação alterada com sucesso!").build();	
		}
		else{
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Não foi possível conectar ao banco de dados!\n"+
					"Tente novamente, se o problema persistir entre em contato com o suporte.").build();
		}
	}
	
}
	


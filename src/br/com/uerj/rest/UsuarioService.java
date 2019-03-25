package br.com.uerj.rest;

import java.util.LinkedList;


import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import br.com.uerj.DAO.DAO_Dash;
import br.com.uerj.DAO.DAO_user;
import br.com.uerj.model.Usuario;

@Path("/user")
public class UsuarioService {
	
	
	
	@Path("/post")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addUser(@FormParam("nomelogin") String username, @FormParam("senhalogin") String password,
	@FormParam("senhalogincf") String confirmPassword, @FormParam("permissaolog") String permission)
	{
		Usuario criar = new Usuario();
		
		int aux_permission = 0;
		if(permission.equals("1")) {
			aux_permission = 1;
		}else if(permission.equals("2")) {
			aux_permission = 2;
		}
		
		
		if(password.equals(confirmPassword)) {
			criar.setUsuario(username);
			criar.setSenha(password);
			criar.setPermissao(aux_permission);
			DAO_user.inserir(criar);
			
			return Response.status(Response.Status.OK).entity("Usuário inserido com sucesso").build();
		}
		else {
			return Response.status(Response.Status.BAD_REQUEST).entity("Confirmação diferente da senha digitada").build();
		}
	}
	
	@Path("/change/{username}")
	@PUT
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response alterUser(@PathParam("username") String username, @FormParam("nova_senha") String newPassword, 
	@FormParam("confirma_senha") String confirmNewPassword) 
	{ 
		//String do parametro é um json
		
		//Convertendo o json em objeto Usuario
		Usuario user = DAO_user.consultaUser(username);
		if(newPassword.equals(confirmNewPassword)) {
			user.setSenha(newPassword);
			//persistindo a nova senha no bd ... 
			DAO_user.updateSenha(user);
			
			return Response.status(Response.Status.OK).entity("Senha alterado com sucesso").build();
			
		}else {
			return Response.status(Response.Status.BAD_REQUEST).entity("Confirmação diferente da senha passada").build();
			
		}
		
	}
	@Path("/delete/{username}")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteUser(String userJson,@PathParam("username") String username) {
		Gson gson = new Gson();
		
		Usuario user = gson.fromJson(userJson, Usuario.class);
		
		if(user != null) {
			DAO_user.delete(user);
			
			return Response.status(Response.Status.OK).entity("Usuário deletado com sucesso").build();
			
		}else {
			
			return Response.status(Response.Status.BAD_REQUEST).entity("Usuário inexistente").build();
		}
	}
	
	@Path("/list")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public LinkedList<Usuario> listUsers() {
		LinkedList<Usuario> lusers = DAO_Dash.ListarUsuarios();
		
	
		return lusers;
	}
	
}


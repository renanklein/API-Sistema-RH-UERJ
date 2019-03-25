package br.com.uerj.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConexaoBD {
	private Connection c;
	
	public void iniciaBd(){
		try {
			String url = "jdbc:mysql://127.0.0.1:3306/rh2?serverTimezone=GMT-3";
			String user = "root";
			String password = "12345";
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			c = (Connection) DriverManager.getConnection(url, user, password);
			
			
		}catch(Exception s) {
			s.printStackTrace();
		}
		
	}
	
	public Connection getConexao() {
		return c;
	}
	
	public void fechaBd() {
		try {
			c.close();
		}catch(Exception e) {
			e.getMessage();
		}
	}
}

/*
 * @Author KeXu
 * SQL connection tool
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class SqlConn										//Establish a connection to SQL database
{
	private String ip;
	private String username;
	private String password;
	private ResultSet rset;
	private Connection conn;
	private Statement stmt;
	public void setSql(String serverip,String orclUsername,String orclPassword)
	{
		ip = serverip;
		username = orclUsername;
		password = orclPassword;
	}
	public void tryConn()
	{
		try{
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
			conn = DriverManager.getConnection(ip,username,password);
			stmt = conn.createStatement();
			System.out.println("Connection successful!");
		}
		catch (Exception e)
		{	e.printStackTrace();}
	}
	public ResultSet getResult(String sql)
	{
		try{
			rset=stmt.executeQuery(sql);
			return rset;
		}
		catch(SQLException sqle){
			System.out.println(sqle.toString());
			return null;
		}
	}
	public boolean updateSql(String strSQL)
	{
		try{
			stmt.executeUpdate(strSQL);
			conn.commit();
			return true;
		}
		catch(SQLException sqle){
			System.out.println(sqle.toString());
			return false;
		}
	}
	public void closeConnection()
	{
		try{
			stmt.close();
			conn.close();
		}
		catch(SQLException sqle){
			System.out.println(sqle.toString());
		}
	}
}
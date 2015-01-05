/*
 * @Author KeXu
 * Storage the constants
 */

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class Constant														
{
	final String serverIp = "127.0.0.1";
	//final String serverIp = "192.168.1.101";
	//final String serverIp = "42.122.224.220";
	final int serverPort = 55555;
	final boolean isUseDatabase = false;							
	final int maxTables  = 100;										
	final int maxUsers  = 2 * maxTables;							
	final String STATEND = "stateEnd";								
	final int BUFFER_SIZE = 1024;									
	final int MaxImageLength = (int)(1024000 * 10);
	final double multiple = 1.0;									
	final int wsizex = m(512);										
	final int wsizey = m(364);										
	final boolean isShowUser = true;								
	final String imagepath = "image/";
	final String SysImgpath = "sys_image/";
	final Color chatColor = new Color(232,232,232);
	final int ADD = 2;												
	final int MINUS = -2;											
	final int ESCAPEMINUS = -4;										
	final boolean debug = true;

	public int m(int i){
		return (int)(multiple * i); 
	}

	ByteBuffer sendbuffer;
	public void sendInforBack(SocketChannel client,String message)	
	{
		try{
			message += STATEND;										
			byte[] sendBytes = message.getBytes();					
			sendbuffer = ByteBuffer.allocate(sendBytes.length);		
			sendbuffer.put(sendBytes);								
			sendbuffer.flip();										
			client.write(sendbuffer);								
			System.out.println("Send£º" + message);
		}
		catch (Exception e) {
			System.out.println("Error");
			if(debug){	e.printStackTrace();}
		} 
	}
}
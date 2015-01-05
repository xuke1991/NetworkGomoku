/*
 * @Author KeXu
 * Server end of the application 
 */

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;  
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.ResultSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.Color;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JLabel;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.FileReader;

class Server extends JFrame	implements ActionListener, MouseListener, MouseMotionListener 	
{
	Constant c;
	private ImgTemp imgTemps[];										
	private SqlConn sqlConn;										
	private GameTable Tables[];										
	private UAS Users[];																
	private int userSum = 0;										
	private Selector selector;										
	private ServerSocketChannel ssc;								
	private ServerSocket ss;										
	private InetSocketAddress address;								
	JTextArea showUsers = new JTextArea("");
	JScrollPane	showUsersScroll = new JScrollPane(showUsers);
	Image playerImage;
	JLabel viewersInfors[];
	String viewersInforsID[];

	Server(Constant cc)												
	{
		super("User");												
		c = cc;														
		imgTemps = new ImgTemp[c.maxUsers];
		viewersInfors = new JLabel[c.maxUsers];
		viewersInforsID = new String[c.maxUsers];

		Tables = new GameTable[c.maxTables];
		for(int i = 0;i < Tables.length;i++){						
			Tables[i] = new GameTable(c);
			Tables[i].setUsers(Users);
			Tables[i].setServer(this);
			if(c.isUseDatabase){									
				Tables[i].setSqlConn(sqlConn);
			}
		}

		Users = new UAS[c.maxUsers];								
		for(int i = 0;i < Users.length;i++){
			Users[i] = new UAS();	
		}		

		for(int i = 0;i < imgTemps.length;i++){
			imgTemps[i] = new ImgTemp();
		}

		if(c.isShowUser){
			showUser();
		}

		if(c.isUseDatabase){											
			String connectString = "jdbc:oracle:thin:@"+ c.serverIp +":1521:orcl";
			String orclUsername = "scott";
			String orclPassword = "scott";
			sqlConn = new SqlConn();								
			sqlConn.setSql(connectString,orclUsername,orclPassword);
		}

		try {
            selector = Selector.open();
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);							
            ss = ssc.socket();
            address = new InetSocketAddress(c.serverPort);
            ss.bind(address);										
            ssc.register(selector, SelectionKey.OP_ACCEPT);			
			System.out.println("********* Gomoko Server End ********");
            System.out.println("Socket registered");
		}
		catch (Exception e){	
			System.out.println("Can not start server");
			if(c.debug){	e.printStackTrace();}
			System.exit(0);
		}
		try {
			int userNumber = -1;									
            while(true)												
			{
                int shijian = selector.select();					
                if(shijian == 0)	{	continue;	}
                ByteBuffer echoBuffer = ByteBuffer.allocate(c.BUFFER_SIZE);			
                for (SelectionKey key : selector.selectedKeys())					
				{
                    if(key.isAcceptable())							
					{
                        ServerSocketChannel server = (ServerSocketChannel)key.channel();	
                        SocketChannel client = server.accept();						
                        client.configureBlocking(false);							
                        client.register(selector, SelectionKey.OP_READ);			
			System.out.println("Has a new connection");
                        //System.out.println("Has a new connection" + client);
                    }
                    else if(key.isReadable())						
					{
                        SocketChannel client = (SocketChannel)key.channel();		
                        echoBuffer.clear();											
						int readInt = 0;											
                        try {
                            while ((readInt = client.read(echoBuffer)) > 0){
								//if(readInt == 0 )	{ continue;}					
								byte[] readByte = new byte[readInt];				
								for(int i = 0;i < readInt;i++){						
									readByte[i]=echoBuffer.get(i);
								}
								echoBuffer.clear();									
								client.register(selector, SelectionKey.OP_READ);
								userNumber = getUserNum(client);
								prepareParse(readByte,client,userNumber);
                            }
							if(readInt < 0)	{						
								//System.out.println("Conncetion stopped: " + client);
								System.out.println("Conncetion stopped");
								userExit(client);
								client.close();
							}
                        }
						catch(Exception e){							
							//System.out.println("Conncetion error: " + client);
							System.out.println("Conncetion error");
							if(c.debug){	e.printStackTrace();}
							userExit(client);
							key.cancel();
                            break;
                        }
                    }
                }
				selector.selectedKeys().removeAll(selector.selectedKeys());	
            }
        }
        catch (Exception e){	
			System.out.println(e);
			if(c.debug){	e.printStackTrace();}
		}
	}

	public void prepareParse(byte[] readByte, SocketChannel client,int userNumber)		
	{	
		String dirtyResult = new String(readByte);
		if(userNumber >= 0){
			if(!Users[userNumber].getIsFileSended()){
				imageSaveControl(readByte,client,userNumber);
			}
		}

		String results[];
		try{
			results = dirtyResult.split(c.STATEND);
		}
		catch(Exception e){	
			if(c.debug){	e.printStackTrace();}
			return; 
		}

		if(userNumber < 0 || (userNumber >= 0 && !Users[userNumber].getIsFile())) {	
			for(int i = 0;i < results.length;i++){
				System.out.println("Parse: " + results[i]);
				Parse(results[i],client,userNumber);	
			}
		}
	}

	public void imageSaveControl(byte[] readByte, SocketChannel client,int userNumber){	/
		try{
			String dirtyResult = new String(readByte,"ISO-8859-1");
			if(dirtyResult.contains("ULMImg")){						
				System.out.println("userNumber = " + userNumber);
				Users[userNumber].setIsFile(true);
				int l = ("ULMImg" + c.STATEND).length();
				int position = dirtyResult.indexOf("ULMImg");
				String remain = dirtyResult.substring(position + l);
				if(remain.length() > 0){
					System.out.println("Image error correction begins");
					byte [] remains = remain.getBytes("ISO-8859-1");
					saveImage(userNumber,remains);
					byte another[] = dirtyResult.substring(0,position).getBytes("ISO-8859-1");
					if(another.length > 0){
						prepareParse(another,client,userNumber);
					}
				}
			}
			else if(dirtyResult.contains("SMImgD")){				
				Users[userNumber].setIsFile(false);
				Users[userNumber].setIsFileSended(true);
				int l = ("SMImgD" + c.STATEND).length();
				int position = dirtyResult.indexOf("SMImgD");
				String remain = dirtyResult.substring(0,position);
				if(remain.length() > 0){
					System.out.println("Image error correction stops");
					byte [] remains = remain.getBytes("ISO-8859-1");
					saveImage(userNumber,remains);
					byte another[] = dirtyResult.substring(position + l).getBytes("ISO-8859-1");
					if(another.length > 0){
						prepareParse(another,client,userNumber);
					}
				}
				if(Users[userNumber].imgTempNum != -1){						
					System.out.println("File size:" + imgTemps[Users[userNumber].imgTempNum].getTempLength());
					try{													
						File fWrite = new File(c.imagepath + Users[userNumber].getId() + ".PNG");
						FileOutputStream out = new FileOutputStream(fWrite);
						out.write(imgTemps[Users[userNumber].imgTempNum].getNewFiles(),0,imgTemps[Users[userNumber].imgTempNum].getTempLength());
						out.close();
					}
					catch(Exception e){
						System.out.println(e);
						if(c.debug){	e.printStackTrace();}
					}
					imgTemps[Users[userNumber].imgTempNum].userNumber = -1;
					imgTemps[Users[userNumber].imgTempNum].newFiles = null;
					imgTemps[Users[userNumber].imgTempNum].setTempLength(0);
				}
				Users[userNumber].setIsFile(false);
				Users[userNumber].setIsFileSended(true);
				String result = "setIsFileSended;true";
				c.sendInforBack(client,result);
			}

			if(Users[userNumber].getIsFile()){								
				if((!dirtyResult.contains("ULMImg")) && (!dirtyResult.contains("SMImgD"))) {
					saveImage(userNumber,readByte);
				}
			}
		}
		catch(Exception e){
			System.out.println(e);
			if(c.debug){	e.printStackTrace();}
		}
	}

	public void saveImage(int userNumber, byte[] readByte){			
		if(readByte.length <= 0)return;
		for(int j = 0; j < c.maxUsers; j++){
			if(Users[userNumber].imgTempNum == -1){					
				for(int k = 0; k < c.maxUsers; k++){
					if(imgTemps[k].userNumber == -1){
						imgTemps[k].userNumber = userNumber;
						Users[userNumber].imgTempNum = k;
						imgTemps[k].newFiles = new byte[c.MaxImageLength];
						System.out.println("k = "+k);
						break;
					}
				}
			}
		}
		if(imgTemps[Users[userNumber].imgTempNum].newFiles == null){
			imgTemps[Users[userNumber].imgTempNum].newFiles = new byte[c.MaxImageLength];
		}
		imgTemps[Users[userNumber].imgTempNum].setNewFiles(readByte,readByte.length);
	}
	
	public void Parse(String str, SocketChannel client ,int userNumber)		
	{
		if(str.equals("") || str == null) return;
		boolean isok = false;
		String [] message = null;
		try{
			message = str.split(";");
		}
		catch(Exception e){	
			if(c.debug){	e.printStackTrace();}
			return; 
		}
		if(message.length > 0){

			if(message[0].equals("login")&&message.length == 3)		
			{
				//Type£º"login;userName;password"
				doUserLogin(message,client);
			}
			else if(message[0].equals("regist"))					
			{
				//Type£º"regist;id;name;password"
				regist(client,message[1],message[2],message[3]);
			}
			else if(message[0].equals("sitdown"))					
			{
				//Type£º"sitdown;Table No."
				int deskNumber = Integer.parseInt(message[1]);
				int i = getUserNum(client);
				Users[i].setDeskNumber(deskNumber);
				System.out.println("isStart = " + Tables[deskNumber].getIsStart());
				if(Tables[deskNumber].getIsStart()){
					String result = "AreadyStart";
					c.sendInforBack(client,result);
				}
			}
			else if(message[0].equals("sitdown2"))					
			{
				//Type£º"sitdown;Table No;Seat No;Portrait No"
				int deskNumber = Integer.parseInt(message[1]);
				int chairNumber = Integer.parseInt(message[2]);

				GameTable t = Tables[deskNumber];

				if(!t.seat[chairNumber].userId.equals("")&&!t.getIsStart()){
					String result = "NoSit;";
					c.sendInforBack(client,result);
					return;											
				}
				
				int i = getUserNum(client);

				if(deskNumber < 0)									
				{	t.setTableNumber(deskNumber);}
				UAS	Viewers[] = t.getViewers();
				Seat[] seat = t.getSeat();

				if(!t.getIsStart()){								
					for(int j = 0;j < Viewers.length; j++)			
					{
						if(Viewers[j].getId().equals("")){			
							t.seat[chairNumber].set(Users[i].getId(),Users[i].getName(),message[3]); 	
							broadcast(seatState());					
							Viewers[j] = Users[i];
							String result = "ackSit;" + deskNumber + ";" + "NotStart";
							StepRecord stepRecords[] = t.getStepRecords();
							c.sendInforBack(client,result);			
							System.out.println("User "+ Viewers[j].getName() + " enters No." + deskNumber + " room");
							break;
						}
					}
				}
				else if(t.getIsStart()){							
					for(int j = 0;j < Viewers.length; j++)			//Record online user id,name,SocketChannel
					{
						if(Viewers[j].getId().equals("")){			
							Viewers[j] = Users[i];
							String result = "ackSit;" + deskNumber + ";";
							StepRecord stepRecords[] = t.getStepRecords();	
							int step = t.getStep();
							result += "AreadyStart;" + step + ";";
							for(int k = 0;k < step;k++){
								result += stepRecords[k].getX() + "," + stepRecords[k].getY() + "," + stepRecords[k].getColor() + ";";
							}
							c.sendInforBack(client,result);			

							System.out.println("User "+Viewers[j].getName()+" enters No."+deskNumber+" room");
							break;
						}
					}
				}

				t.setViewerSum(t.getViewerSum() + 1);				
				t.refreshViewersInfor();							
				Users[i].setDeskNumber(deskNumber,chairNumber);
			}
			else if(message[0].equals("initGameHallOk"))			
			{
				refreshGameHallPlayers();
				c.sendInforBack(client,seatState());	
			}
			else if(message[0].equals("viewPlayersInfor"))			
			{
				String imageName =c.imagepath + message[1]+".PNG";
				String from = "";
				if(message[2].equals("GameHall")){
					from = "0";
				}
				else{
					from = "1";
				}
				int n = 0;
				try{
					c.sendInforBack(client,"DLPImg" + from);

					File file =new File(imageName);
					System.out.println(imageName + " File size:" + file.length());
					if(file.length() > c.MaxImageLength){
						System.out.println("File too large");//
						c.sendInforBack(client,"ImgIllegal" + from);
						return;
					}
					FileInputStream  fr = new FileInputStream (file);
					byte[] b = new byte[1024];
					ByteBuffer sendbuffer; 
					while ((n = fr.read(b)) > 0) {	
						sendbuffer = ByteBuffer.wrap(b,0,n);  
						client.write(sendbuffer);
						sendbuffer.flip();
						Thread.sleep(3);	//Thread.sleep(3);
					}
					fr.close();
					c.sendInforBack(client,"LPImgD");
				}
				catch(Exception e){
					System.out.println(e);
					if(c.debug){	e.printStackTrace();}
					System.out.println("Data failed to send");
					c.sendInforBack(client,"ImgIllegal" + from);
				}
			}
			else if(message[0].equals("setMyPortrait"))				
			{
				int i = getUserNum(client);
				Users[i].setPortrait(message[1]);
			}
			else if(message[0].equals("saveDone"))					
			{
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].refreshGamersInforPartly(client);	
				Tables[deskNumber].refreshViewersInforPartly(client);
				c.sendInforBack(client,seatState());
			}
			else if(message[0].equals("initOppoPictOk"))			
			{
				refreshGameHallPlayersPartly(client);
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].setIOPS(Tables[deskNumber].getIOPS() + 1);	
			}
			else if(message[0].equals("getSeatState"))				
			{
				c.sendInforBack(client,seatState());	
			}
			else if(message[0].equals("started"))					
			{
				//Type£º"started;table No.;X-coordinate;Y-coordinate;color"
				int deskNumber = Integer.parseInt(message[1]);
				int i = getUserNum(client);
				Users[i].setDeskNumber(deskNumber);
				//System.out.println(Users[i].getDeskNumber());
				if(Tables[deskNumber].checkSetDown(message,client)){	
					broadcast(seatState());	
				}
			}
			else if(message[0].equals("ready"))						
			{
				//Type£º"ready;table No."
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].doReadyGame(client);
				if(Tables[deskNumber].getIsStart()){				
					broadcast(seatState());	
				}
			}
			else if(message[0].equals("userMessage"))				
			{
				//Type£º"userMessage;table No.;content"
				int deskNumber = Integer.parseInt(message[1]);
				int index = str.indexOf(";");
				index = str.indexOf(";", index + 1);
				String mInfor = str.substring(index + 1);
				if(mInfor.equals(""))	return;
				Tables[deskNumber].doChatting(mInfor,client);
			}
			else if(message[0].equals("userBroadcastMessage"))		
			{
				//Type£º"userBroadcastMessage;content"
				int index = str.indexOf(";");
				String mInfor = str.substring(index + 1);
				if(mInfor.equals(""))	return; 
				doBroadcastChatting(mInfor,client);
			}
			else if(message[0].equals("userSeparateChatMessage"))	
			{
				//Type£º"userBroadcastMessage;userId;content"
				String userId = message[1];
				int index = str.indexOf(";");
				index = str.indexOf(";", index + 1);
				String mInfor = str.substring(index + 1);
				if(mInfor.equals(""))	return; 
				doBroadcastChatting(mInfor,client,userId);
			}
			else if(message[0].equals("closeTable"))				
			{
				//Type£º"closeTable;table No"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].viewerExit(client);
				resetUserSeat(client);
				broadcast(seatState());								
			}
			else if(message[0].equals("rollbackRequest"))			
			{
				//Type£º"rollbackRequest;table No;step"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].rollbackForward(message[2],client);
			}
			else if(message[0].equals("replyRBForward"))			
			{
				//Type£º"replyRBForward;table No;reply;step"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].doRollback(Integer.parseInt(message[2]),Integer.parseInt(message[3]),client);
			}
			else if(message[0].equals("refreshGamersInforPartly"))	
			{
				//Type£º"refreshGamersInforPartly;table No"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].refreshGamersInforPartly(client);
			}
			else if(message[0].equals("refreshViewersInforPartly"))	
			{
				//Type£º"refreshViewersInforPartly;table No"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].refreshViewersInforPartly(client);
			}
			else if(message[0].equals("admitLose"))					
			{
				//Type£º"admitLose;table No;color"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].admitLose(message[2]);
				broadcast(seatState());
			}
			else if(message[0].equals("drawRequest"))				
			{
				//Type£º"drawRequest;table No;"
				int deskNumber = Integer.parseInt(message[1]);
				Tables[deskNumber].drawRequest(client);
			}
			else if(message[0].equals("drawRequestReply"))			
			{
				//Type£º"drawRequestReply;table No;reply"
				int deskNumber = Integer.parseInt(message[1]);
				if(Tables[deskNumber].doDraw(client , Integer.parseInt(message[2]))){
					broadcast(seatState());
				}
			}
			else if(message[0].equals("addFriendRequest"))			
			{
				//Type£º"addFriendRequest;userId"
				int i = getUserNum(client);
				int j = getUserNumByUserId(message[1]);
				if(isExsitFriend(Users[i].getId(),Users[i].getName(),Users[j].getId(),Users[j].getName())){
					c.sendInforBack(Users[i].getUserChannel(),"friendExsit;");
					return;
				}
				c.sendInforBack(Users[j].getUserChannel(),"addFriendRequest;" +Users[i].getId() + ";"+ Users[i].getName());
			}
			else if(message[0].equals("agreeAddFriend"))			
			{
				//Type£º"agreeAddFriend;userId"
				int j = getUserNum(client);
				int i = getUserNumByUserId(message[1]);
				addFriend(Users[i].getId(),Users[i].getName(),Users[j].getId(),Users[j].getName());
				c.sendInforBack(Users[j].getUserChannel(),"addFriendAgree;" +Users[i].getId() + ";"+ Users[i].getName());
				c.sendInforBack(Users[i].getUserChannel(),"addFriendAgree;" +Users[j].getId() + ";"+ Users[j].getName());
			}
			else if(message[0].equals("viewFriends"))				
			{
				//Type£º"viewFriends;"
				int i = getUserNum(client);
				c.sendInforBack(client,getFriends(Users[i].getId()));
			}
			else if(message[0].equals("delFriend"))					
			{
				//Type£º"delFriend;userId;userName"
				int i = getUserNum(client);
				int j = getUserNumByUserId(message[1]);
				delFriend(Users[i].getId(),Users[i].getName(),message[1],message[2]);
				c.sendInforBack(client,"delSuccess;");
				if(j >= 0){
					c.sendInforBack(Users[j].getUserChannel(),"delSuccess;" + Users[i].getName());
				}
			}
		}
	}

	public void doUserLogin(String []message,SocketChannel client)	
	{
		boolean isTrueUser = false;
		boolean isAlreadyLogin = false;
		String userId = message[1];
		String pswd = message[2];
		String userName = "";
		String Score = "0";

		if(c.isUseDatabase){	
			sqlConn.tryConn();
			String sql = "select * from login where userId='" + userId + "'and upassword='" + pswd + "'";
			ResultSet rs=sqlConn.getResult(sql);
			try{
				while (rs.next()){
					userId = rs.getString(1);
					userName = rs.getString(2);
					Score = rs.getInt(4) + "";
					isTrueUser = true;
				}
				sqlConn.closeConnection();
			}
			catch(Exception e){
				System.out.println(e);
				if(c.debug){	e.printStackTrace();}
			}
		}

		else{													
			try{
				FileReader filein = new FileReader("src/users.txt");
				BufferedReader br = new BufferedReader(filein);
				String temp = "";
				while((temp = br.readLine()) != null) {
					try{
						//System.out.println("Config file:" + temp);
						String dlls[] = temp.split(";");
						for(int i = 0 ; i < dlls.length ; i++){
							dlls[i] = dlls[i].trim();
						}
						//id;name;password;socre;
						if(dlls[0].equals(userId)){
							if(dlls[2].equals(pswd)){
								Score = dlls[3];
								userName = dlls[1];
								isTrueUser = true;
								break;
							}
						}
					}
					catch(Exception e){
						continue;
					}
				}
			}
			catch (Exception ee){	
				System.out.println(ee);
				if(c.debug){	ee.printStackTrace();}
			}

			String strRegex = "[\u4e00-\u9fa5a-zA-Z0-9]*";			
			Pattern p = Pattern.compile(strRegex);
			Matcher m = p.matcher(userId); 
			if(!userId.matches(strRegex)){
				isTrueUser = false;
			} 

		
		}

		if(userId != null){
			String result = "";
			if(isTrueUser){ 
				result = "ack;" + userId + "," + userName + "," + Score;
				for(int i = 0;i < Users.length;i++){
					if(Users[i].getId().equals(userId)){
						result="nak_reLogin";
						isAlreadyLogin = true;
						System.out.println("Can not login again");
					}
				}
				if(!isAlreadyLogin){
					for(int i = 0; i < Users.length;i++){
						if(Users[i].getId().equals("")){
							Users[i].setAll(userId,userName,Integer.parseInt(Score),client);	//Record user id,name and SocketChannel
							System.out.println("User "+Users[i].getName() + " logged in" + "  Id:" + userId + " score:" + Score);
							break;
						}
					}
					userSum++;
				}
				if(c.isShowUser){
					refreshShowUser();
				}
			}
			else{													
				result = "nak";
				System.out.println("Login failed");
			}
			c.sendInforBack(client,result);			
		}
	}

	public void regist(SocketChannel client,String id, String name ,String password){
		if(c.isUseDatabase){}
		else{
			try{
				File txt = new File("src/users.txt");				
				FileReader filein = new FileReader("src/users.txt");
				BufferedReader br = new BufferedReader(filein);
				String temp = null;
				
				while((temp = br.readLine()) != null) {
					String dlls[] = temp.split(";");
						for(int i = 0 ; i < dlls.length ; i++){
							dlls[i] = dlls[i].trim();
						}
						//id;password;socre;name
						if(dlls[0].equals(id)){
							c.sendInforBack(client,"idRepeat;");
							return;
						}
				}
				String record = id + ";" + name + ";" + password + ";0;" + "\r\n";
				byte []contents = (record).getBytes();
			
				FileOutputStream out = new FileOutputStream(txt,true);		
				out.write(contents);
				out.close();
				System.out.println("New user registered");
				c.sendInforBack(client,"registSuccess;");
			}
			catch (Exception e){	
				System.out.println(e);
				if(c.debug){	e.printStackTrace();}
			}
		}
	}

	public String getFriends(String userId){
		String friends = "Myfriends;";
		if(c.isUseDatabase){}
		else{
			try{
				File txt = new File("src/friends.txt");				
				FileReader filein = new FileReader("src/friends.txt");
				BufferedReader br = new BufferedReader(filein);
				String temp = null;
				
				while((temp = br.readLine()) != null) {
					String dlls[] = temp.split(";");
					for(int i = 0 ; i < dlls.length ; i++){
						dlls[i] = dlls[i].trim();
					}
					if(dlls[0].equals(userId)){
						friends += dlls[1]+";";
					}
				}
			}
			catch (Exception e){	
				System.out.println(e);
				if(c.debug){	e.printStackTrace();}
			}
		}
		return friends;
	}

	public void delFriend(String iId,String iName, String jId ,String jName){
		if(c.isUseDatabase){}
		else{
			String record = "";
			try{
				FileReader filein = new FileReader("src/friends.txt");
				BufferedReader br = new BufferedReader(filein);
				String temp = null;
				
				while((temp = br.readLine()) != null) {
					if(temp.equals(iId + ";" + jId +","+ jName)||temp.equals(jId + ";" + iId  +","+ iName)){
					}
					else{
						record += temp + "\r\n";
					}
				}
				File txt = new File("src/friends.txt");				
				FileOutputStream out = new FileOutputStream(txt);	
				byte []contents = (record).getBytes();
				out.write(contents);
				out.close();
				System.out.println("Delete friend");
			}
			catch (IOException ee)
			{	System.out.println(ee);}
		}
	}


	public boolean isExsitFriend(String iId,String iName, String jId ,String jName){
		try{
			FileReader filein = new FileReader("src/friends.txt");
			BufferedReader br = new BufferedReader(filein);
			String temp = null;
			while((temp = br.readLine()) != null) {
				String dlls[] = temp.split(";");
				for(int i = 0 ; i < dlls.length ; i++){
					dlls[i] = dlls[i].trim();
				}
				if(dlls[0].equals(iId)&&dlls[1].equals(jId + "," + jName)){
					return true;
				}
			}
		}
		catch (IOException ee)
		{	System.out.println(ee);}
		return false;
	}

	public void addFriend(String iId,String iName, String jId ,String jName){
		if(c.isUseDatabase){}
		else{

			if(isExsitFriend(iId, iName, jId, jName)){
				return ;
			}
			try{
				File txt = new File("src/friends.txt");				
				FileOutputStream out = new FileOutputStream(txt,true);		
				String record = iId + ";" + jId +","+ jName + "\r\n" + jId + ";" + iId  +","+ iName + "\r\n";
				byte []contents = (record).getBytes();
				out.write(contents);
				out.close();
				System.out.println("Add a friend");
			}
			catch (IOException ee)
			{	System.out.println(ee);}
		}
	}

	public void userExit(SocketChannel client)						
	{
		int i = getUserNum(client);
		int deskNumber = -1;
		int chairNumber = -1;
		if(i >= 0){
			deskNumber = Users[i].getDeskNumber();
			chairNumber = Users[i].getChairNumber();
		}
		else{
			System.out.println("Leave as a viewer");
			return;
		}
		if(deskNumber >= 0){	
			System.out.println("Escape room No.:"+deskNumber);
			Tables[deskNumber].viewerExit(client);
		}
		if(chairNumber >= 0){
			if(Tables[deskNumber].seat[chairNumber].userId.equals(Users[i].getId())){
				Tables[deskNumber].seat[chairNumber].clear();
			}
		}

		System.out.println("User "+Users[i].getName()+" logged off");
		Users[i].clear();
		userSum--;
		broadcast(seatState());										
		refreshGameHallPlayers();
		if(c.isShowUser){
			refreshShowUser();
		}
	}

	public void refreshShowUser(){									
		showUsers.setText("");
		showUsers.removeAll();
		for(int i = 0,sum = 0; i < Users.length;i++){
			if(!Users[i].getId().equals("")){
				int perLength = 10;									
				String userFormatId = Users[i].getId();
				int idLenth = userFormatId.getBytes().length;
				if(idLenth > 8){
					userFormatId = userFormatId.substring(0,3)+"¡­";
					idLenth = userFormatId.getBytes().length;
				}
				String idPos = "";
				for(int j= 0;j < perLength - idLenth;j++){
					idPos += "  ";
				}
				String infor = "    ID " + userFormatId + idPos  + "name " + Users[i].getName() ;
				viewersInforsID[sum] = userFormatId;
				viewersInfors[sum] = new JLabel(infor);
				viewersInfors[sum].setBounds(0,18 * sum,217,18);
				showUsers.add(viewersInfors[sum]);
				showUsers.append("\r\n");
				viewersInfors[sum].addMouseListener(this);
				sum ++;
			}
		}
		JScrollBar bar = showUsersScroll.getVerticalScrollBar();
		bar.setValue(bar.getMaximum());
	}

	public void resetUserSeat(SocketChannel client){				
		int i = getUserNum(client);
		int deskNumber = -1;
		int chairNumber = -1;
		if(i >= 0){
			deskNumber = Users[i].getDeskNumber();
			chairNumber = Users[i].getChairNumber();
		}
		if(deskNumber >= 0&&chairNumber >= 0){	
			if(Tables[deskNumber].seat[chairNumber].userId.equals(Users[i].getId())){
				Tables[deskNumber].seat[chairNumber].clear();
			}
		}
	}

	public String seatState(){										
		//System.out.println("Create seat information");
		String seatState = "seatState;";
		for(int j = 0; j < Tables.length; j ++){
			for(int k = 0 ; k < 2 ; k++)
			if(!Tables[j].seat[k].userId.equals("")){
				seatState = seatState + j + "¡ý";
				seatState = seatState + k + "¡ý";
				seatState = seatState + Tables[j].seat[k].userId + "¡ý";
				seatState = seatState + Tables[j].seat[k].userName + "¡ý";
				seatState = seatState + Tables[j].seat[k].pictName + "¡ý";
				seatState = seatState + Tables[j].getIsStart() + "¡ü";
			}
		}
		return seatState;
	}

	public int getUserNum(SocketChannel client)						
	{
		for(int i = 0;i < Users.length;i++)		{
			if(!Users[i].getId().equals("")){
				if(Users[i].getUserChannel().isConnected()){
					if(Users[i].getUserChannel().equals(client)){
						return i;
					}
				}
			}
		}
		return -1;
	}

	public void doBroadcastChatting(String infor , SocketChannel client){	
		System.out.println("Chat in game hall:");
		String userName = "";
		String userId = "";
		int userNum = getUserNum(client);							
		userName = Users[userNum].getName();
		userId = Users[userNum].getId();
		String userMessage = "userBroadcastMessage;1;"+ userId + ";" + userName + ";" +infor;
		for(int i = 0;i < Users.length; i++) {						//Visit all SocketChannels£¬send message to available SocketChannel
			if(!Users[i].getId().equals("")){
				if(Users[i].getUserChannel().isConnected()){		
					c.sendInforBack(Users[i].getUserChannel(),userMessage);
				}
				else																		
				{	Users[i].clear();	}
			}
		}
	}

	public void doBroadcastChatting(String infor , SocketChannel client, String targetUserId){	
		System.out.println("Private chat:");
		String userName = "";
		String userId = "";
		int userNum = getUserNum(client);							
		userName = Users[userNum].getName();
		userId = Users[userNum].getId();
		String userMessage = "userBroadcastMessage;2;"+ userId + ";" + userName + ";" +infor;
		for(int i = 0;i < Users.length; i++) {						
			if(Users[i].getId().equals(targetUserId)){
				if(Users[i].getUserChannel().isConnected()){		
					c.sendInforBack(Users[i].getUserChannel(),userMessage);
					break;
				}
				else																		
				{	Users[i].clear();	}
			}
		}
		c.sendInforBack(client,userMessage);							
	}

	public void refreshGameHallPlayers()								
	{
		broadcast(createGameHallPlayersInfor());
	}

	public void refreshGameHallPlayersPartly(SocketChannel client)	
	{
		c.sendInforBack(client,createGameHallPlayersInfor());
	}

	public String createGameHallPlayersInfor(){						//Create userId,usreName,color,score
		String uId = "";
		String uName = "";
		String infor = "refreshGameHallPlayers;";
		int ucolor = -1;
		String userPortrait = "";
		for(int i = 0;i < Users.length; i++){
			if(!Users[i].getId().equals("")){
				uId = Users[i].getId();
				uName = Users[i].getName();
				ucolor = Users[i].getUColor();
				int score = Users[i].getScore();
				userPortrait = Users[i].getPortrait();
				infor += uId + "¡ý" + uName + "¡ý" + ucolor + "¡ý" + score + "¡ý" + userPortrait + "¡ü";
			}
		}
		return infor;
	}

	public int getScoresByUserId(String Id)							
	{
		int userId = Integer.parseInt(Id);
		int userScores = 0;


		if(c.isUseDatabase){										
			sqlConn.tryConn();
			String sql = "select scores from login where userid='" + userId + "'";
			ResultSet rs = sqlConn.getResult(sql);
			try{
				while (rs.next())
				{	userScores = rs.getInt(1);	}
				sqlConn.closeConnection();
			}
			catch(Exception e){
				System.out.println(e);
				if(c.debug){	e.printStackTrace();}
			}
		}

		return userScores;
	}

	public int getUserNumByUserId(String Id)						
	{
		for(int i = 0;i < Users.length; i++){
			if(Users[i].getId().equals(Id)){
				return i;
			}
		}
		return -1;
	}

	public void broadcast(String infor)								
	{
		System.out.println("Hall boardcast:");
		for(int i = 0;i < Users.length; i++)						
		{
			if(!Users[i].getId().equals("")){
				//System.out.println(Users[i].getId());
				if(Users[i].getUserChannel().isConnected()){
					c.sendInforBack(Users[i].getUserChannel(),infor);
				}
				else												
				{	Users[i].clear();	}
			}
		}
	}

	public void showUser(){											

		try {
			String src = c.SysImgpath + "default.png";		
			Image image=ImageIO.read(this.getClass().getResource(src));
			this.setIconImage(image);								
		}
		catch (Exception e) {
			System.out.println(e);
			if(c.debug){	e.printStackTrace();}
		}

		ImageIcon img = new ImageIcon(c.SysImgpath + "bg5.jpg");
		JLabel bgLabel = new JLabel(img);
		bgLabel.setBounds(0,0,c.wsizex,c.wsizey);
		this.getLayeredPane().add(bgLabel, new Integer(Integer.MIN_VALUE));
		((JPanel)getContentPane()).setOpaque(false);

		setLayout(null);
		setResizable(false);
		setVisible(true);

		showUsersScroll.setBounds(c.m(30), c.m(50), c.m(220), c.m(260));	
		add(showUsersScroll);
		showUsers.setOpaque(true);
		showUsers.setBackground(c.chatColor);
		showUsers.setEditable(false); 

		this.setBounds(160,0,c.wsizex,c.wsizey);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				try{
					dispose();
				}
				catch(Exception ee){
				}
			}
		});
	}

	public void setImage(String userId){							
		ImageIcon icon = new ImageIcon("image/" + userId + ".png");
		icon.setImage(icon.getImage().getScaledInstance(icon.getIconWidth(),
		icon.getIconHeight(), Image.SCALE_DEFAULT));
		playerImage = icon.getImage();
		paint(this.getGraphics());
	}

	public void actionPerformed(ActionEvent e){}

	public void mouseClicked(MouseEvent e)				
	{
		if(e.getModifiers() != 16) return;							
		for(int i = 0 ; i < viewersInfors.length ; i++){
			if (e.getSource() == viewersInfors[i]){
				setImage(viewersInforsID[i]);
			}
		}
	}
	public void mouseEntered(MouseEvent e){ 
		for(int i=0 ; i<viewersInfors.length ; i++){
			if (e.getSource() == viewersInfors[i]){
				viewersInfors[i].setOpaque(true);
				viewersInfors[i].setBackground(new Color(48,117,174)); 
			}
		}
	}
	public void mouseExited(MouseEvent e) {
		for(int i=0 ; i<viewersInfors.length ; i++){
			if (e.getSource() == viewersInfors[i]){
				 viewersInfors[i].setBackground(c.chatColor);		
			}
		}
	} 
	public void mouseReleased(MouseEvent e){ }
	public void mouseDragged(MouseEvent e){ }
	public void mouseMoved(MouseEvent e){ }
	public void mousePressed(MouseEvent e) { } 
	public void paint(Graphics g)									
	{
		super.paintComponents(g);
		g.drawImage(playerImage,300 ,80 ,90 ,130 ,this); 
	}
}

class ImgTemp														
{
	int userNumber = -1;
	byte []newFiles;												
	int tempLength = 0;												

	public void setNewFiles(byte[] readByte, int length){
		for (int i= tempLength; i< tempLength + length; i++ ){
			newFiles[i] = readByte[i - tempLength];
		}
		tempLength += length;
	}

	public byte[] getNewFiles(){
		return newFiles;
	}

	public int getTempLength()
	{	return tempLength; }

	public void setTempLength(int templ)
	{	tempLength = templ; }
}
/*
 * 作者:	 韩旭滨
 * QQ:	 	 714670841
 * 邮箱:	 714670841@qq.com
 * 开发工具:EditPlus
 * Copyright 2014 韩旭滨 
 * 本作品只用于个人学习、研究或欣赏，转发请注明出处。
 */

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Graphics;
import java.awt.FlowLayout;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

class PlayerInfor extends JFrame implements ActionListener, MouseListener, MouseMotionListener 
{
	private Constant c;
	Image playerImage;
	private JLabel playerID;	
	private JLabel playerName;	
	private JLabel playerScore;	
	boolean isHavePlayerImage = false;

	PlayerInfor(Constant cc){
		super("Player profile");									
		c = cc;
	}

	public void setInfor(String playersInforsID,String playersInforsName, String playersInforsScore , String playersPortrait)
	{		
		try {
			String src = c.portrait_path + playersPortrait;		
			Image image = ImageIO.read(this.getClass().getResource(src));
			this.setIconImage(image);						
		} 
		catch (Exception e) {
			System.out.println(e);
		}  
		setLayout(null);
		setResizable(false);
		ImageIcon img = new ImageIcon(c.SysImgpath + "bg4.jpg");
		JLabel bgLabel = new JLabel(img);
		bgLabel.setBounds(0,0,c.m(150),c.m(200));
		this.getLayeredPane().add(bgLabel, new Integer(Integer.MIN_VALUE));
		((JPanel)getContentPane()).setOpaque(false);
		playerName = new JLabel("<html><font size = 5 color = blue>" + playersInforsName + "(" + playersInforsID + ")</font></html>");
		add(playerName);
		playerName.setBounds(c.m(60), c.m(-15), c.m(150), c.m(50));
		playerScore = new JLabel("积分:" + playersInforsScore);
		add(playerScore);
		playerScore.setBounds(c.m(20), c.m(60), c.m(100), c.m(50)); 
	}
	public void setImage(byte [] newPlayerFiles){	
		ImageIcon icon = new ImageIcon(newPlayerFiles);
		icon.setImage(icon.getImage().getScaledInstance(icon.getIconWidth(),
		icon.getIconHeight(), Image.SCALE_DEFAULT));
		playerImage = icon.getImage();
		isHavePlayerImage = true;
		this.setSize(c.m(150),c.m(200));
		this.setLocationRelativeTo(null);
		setVisible(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				dispose();
			}
		});
		paint(this.getGraphics());
	}
	public void open(){}
	public void actionPerformed(ActionEvent e){ }
	public void mouseClicked(MouseEvent e)   {}
	public void mouseEntered(MouseEvent e){		}
	public void mouseExited(MouseEvent e) { 		} 
	public void mouseReleased(MouseEvent e){ }
	public void mouseDragged(MouseEvent e){ }
	public void mouseMoved(MouseEvent e){ }
	public void mousePressed(MouseEvent e) { } 
	public void paint(Graphics g)
	{
		super.paintComponents(g);
		if(isHavePlayerImage){
			g.drawImage(playerImage,c.m(10) ,c.m(20) ,c.m(45) ,c.m(65) ,this); 
		}
	} 
} 
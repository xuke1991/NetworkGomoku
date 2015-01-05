NetworkGomoku
=============
/*
 * @Author KeXu
 * Gomoku (Network version)
 * Network version of Gomoku
 * Contains the following functions:
 * Register a new player and select figures and protraits
 * Record of points earned, punishment for escaping during a game
 * Add friend; public broadcast; private chat
 * Saving game for reviewing
 * Allow bystanders to watch the game
 *
 */


Descriptions:

Content of the application include two parts.

Folder A:

Codes for server end. Application entrance is A.java. 

Server.java implements the network connection with the client.

GameTable.java contains objects of the tables. Each contains several bystanders and two players.

UAS.java (User and Socket Channel) contains users' parameters.

SqlConn.java is the conncetion to SQL database. Driver of SQL has to be set before using. 


Folder B:

Codes for the client. Application  entrance is B.java.

InforChange.java is the main program and implements the network connection to the server end.

LoginJFrame.java implements the login function for existing users.

RegistJFrame.java implements the register function for new users.

Wuziqi.java contains game layout.

Wuziqi_review.java implements the function of reviewing saved games.

PlayerInfor.java displays the information of users.


Some pictures are collected from the Internet and I would like to extend my sincere gratitude to those.


Network version of Gomoku

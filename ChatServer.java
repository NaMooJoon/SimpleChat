// https://github.com/NaMooJoon/SimpleChat.git
// 21800179 김준현

import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap<String, PrintWriter> hm = new HashMap<String, PrintWriter>();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id, " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){
				if(line.equals("/quit"))
					break;
				if(line.indexOf("/to ") == 0){
					System.out.println("> " + id + " request the whispher...");
					sendmsg(line);
				}else if(line.equals("/userlist")){
					System.out.println("> " + id + " request the userlist...");
					send_userlist();
				}else {
					if(isCuss(line)) // line에 욕설이 포함되어 있지 않다면, broadcast 실행
						broadcast(id, " : " + line);
					else			// 욕설이 포함 되어있다면, sender에게만 Warnning message 보내기
						sendmsg("/to " + id + " >>> Warnning < Cuss Words >"); // 기존 함수 활용
				}
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			// 기존 > broadcast(id + " exited.");
			broadcast(id, " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(id + " whisphered : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg

	public void send_userlist() {									// userlist를 요청한 user에게 현재 Userlist를 보내주는 함수
		synchronized(hm) {											// HashMap 충돌 방지
			int userCount = 0;
			PrintWriter receiver = (PrintWriter)hm.get(id);			// userlist를 요청한 user의 PrintWriter 받기
			receiver.println(">>> Userlist <<<");
			Set<String> users = hm.keySet();
			for(String user : users) {								// hm의 key값들(ID들) 모두 출력
				receiver.println(" - " + user);
				userCount ++;
			}
			receiver.println("\n>>> The number of users: " + userCount);
			receiver.flush();
		} // synchronized
	} // send_userlist

	public void broadcast(String sender, String msg){		// sender과 msg를 따로 파라미터로 받음.
		synchronized(hm){
			HashMap newMap = (HashMap)hm.clone();			// 기존 해쉬맵을 복제
			newMap.remove(sender);							// 복제된 해쉬맵에서 sender key 삭제하기. (sender/자기 자신 제외)

			Collection collection = newMap.values();		// 복제된 해쉬맵에 있는 모든 키값을 Collection화 하기.
			Iterator iter = collection.iterator();
			while(iter.hasNext()){
				PrintWriter pw = (PrintWriter)iter.next();
				pw.println(sender + msg);
			} // while
			pw.flush();
		} // synchronized
	} // broadcast

	// message에 욕설이 포함되어 있는 지 아닌 지 값을 반환해주는 함수.
	public boolean isCuss(String msg) {
		String[] cuss = { "fuck", "shit","시발", "좆", "새끼"};	  // 욕설 들
		msg = msg.toLowerCase();									// word 비교를 위해서 msg 소문자로 바꾸기.
		for(String word : cuss)					// cuss 배열 전체 검색
			if(msg.contains(word))				// 만일 msg가 word와 같은 단어가 포함된다면 false
				return false;
		return true;							// 욕설들이 없다면 true
	}
}

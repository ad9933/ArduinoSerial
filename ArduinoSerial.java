import com.pi4j.io.serial.*;
import com.pi4j.util.CommandArgumentParser;
import com.pi4j.util.Console;

import java.io.IOException;
import java.util.*;

import java.net.*;

class ArduinoSerial {
	
	static final int PORT_NUM = 7777;
	
	static final Serial serial = SerialFactory.createInstance();
	Scanner scanner = new Scanner(System.in);
	
	SerialConfig config = null;
	
	DatagramSocket socket = null;
	
	//초기화 구문
	public ArduinoSerial() {
		
		//UDP 소켓 초기화
		try {
			socket = new DatagramSocket(PORT_NUM);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		//시리얼 포트로 데이터가 들어왔을때 취할 행동 설정
		serial.addListener(new SerialDataEventListener() {
			
			public void dataReceived(SerialDataEvent event) {
				try {
					System.out.println("[*]Serial Input : " + event.getAsciiString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		//시리얼 통신 환경 설정
		config = new SerialConfig();
		
		try {
			config.device(SerialPort.getDefaultPort())
                  .baud(Baud._38400)		//아두이노쪽에서 보드레이트 38400으로 설정할것.
                  .dataBits(DataBits._8)
                  .parity(Parity.NONE)
                  .stopBits(StopBits._1)
                  .flowControl(FlowControl.NONE);
		
			//시리얼 통신 시작
			serial.open(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//초기화 끝난 후 활동시작
		System.out.println("[*]Serial port and Socket has been initialized.");
		actions();
		
	}
	
	public static void main(String[] args) {
		System.out.println("[*]Starting...");
		new ArduinoSerial();
		
	}
	
	//초기화 후 수행할 코드
	void actions() {
		
		//UDP 정보 중계 스레드 시작
		UdpReceiveThread recv = new UdpReceiveThread(socket);
		recv.start();
		
		//키보드로 정보 입력받을시 시리얼로 전송함
		while(true) {
			String input = scanner.nextLine();
			try {
				serial.write(input.getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("[*]" + input.length() + " of data sent to Serial");
		}
		
	}
	
	//UDP로 받은 정보를 시리얼로 전송해줄 스레드
	static class UdpReceiveThread extends Thread {
		
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] data = null;
		
		static final int DATA_LEN = 256;	//받을 패킷 길이 설정
		
		public boolean stop = false;		//스레드를 멈춰야 할 경우 stop를 true로 바꾸면 된다.
		
		public UdpReceiveThread(DatagramSocket socket) {
			this.socket = socket;
		}
		
		//수신해서 데이터를 처리하는 내용
		public void run() {
			
			while(!stop) {
				
				//데이터 수신
				data = new byte[DATA_LEN];
				packet = new DatagramPacket(data, data.length);
				
				try {
					socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//시리얼 포트로 데이터 송신
				try {
					ArduinoSerial.serial.write(data);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				System.out.println("[*]" + packet.getLength() + " bytes of data sent to Serial");
				
			}
		}
	}
	
}
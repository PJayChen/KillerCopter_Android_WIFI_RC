package app.udp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Ctrl_Signal extends Activity implements View.OnClickListener{

	private udpthread udpSocket = null;
	private String IP;
	private int TarPort, LocPort;
	private EditText editRx, editThr;
	private Button btnThr, btnRise, btnFall;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ctrl_signal);
		
		setView();
		
		setSocket();
		
	}
	
	private void setSocket(){
		udpSocket = new udpthread(editRx);
		//get data from IP_setting.java
		Bundle bd = getIntent().getExtras();
		IP = bd.getString("IP");
		TarPort = Integer.parseInt(bd.getString("TarPort"));
		LocPort = Integer.parseInt(bd.getString("LocPort"));
		
		udpSocket.setRemoteIP(IP);
		udpSocket.setRemotePort(TarPort);
		udpSocket.setLocalPort(LocPort);
		
		//display connect information
		StringBuilder sb = new StringBuilder();
		sb.append(editRx.getText().toString().trim());
		sb.append("\n");
		sb.append("IP: ");
		sb.append(IP);
		sb.append("\nPort: ");
		sb.append(TarPort);
		sb.append("\nLocal Port");
		sb.append(LocPort);
		editRx.setText(sb.toString().trim());
		sb.delete(0, sb.length());
		sb = null;
		
		//connect, if success all button can be clickable
		if(udpSocket.ConnectSocket())
			setUIState(true);
	}
	
	private void setUIState(boolean state){
		btnThr.setClickable(state);
		btnRise.setClickable(state);
		btnFall.setClickable(state);
	}
	
	public void setView(){
		editRx = (EditText) findViewById(R.id.editRx);
		editThr = (EditText) findViewById(R.id.editThr);
		btnThr = (Button) findViewById(R.id.btn_thr);
		btnRise = (Button) findViewById(R.id.btn_rise);
		btnFall = (Button) findViewById(R.id.btn_fall);
		
		setUIState(false);
		editThr.setText("800");
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}

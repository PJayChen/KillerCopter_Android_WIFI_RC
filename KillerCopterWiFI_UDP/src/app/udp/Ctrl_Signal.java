package app.udp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Ctrl_Signal extends Activity implements View.OnClickListener{

	private udpthread udpSocket = null;
	private String IP;
	private int TarPort, LocPort;
	private EditText editRx, editThr;
	private Button btnThr, btnRise, btnFall, btnLand;
	private TextView txtCurrThr;
	private int currThr;
	StringBuilder sb = null;
	private String SendData;
	
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
		sb = new StringBuilder();
		sb.append(editRx.getText().toString().trim());
		sb.append("\n");
		sb.append("IP: ");
		sb.append(IP);
		sb.append("\nPort: ");
		sb.append(TarPort);
		sb.append("\nLocal Port: ");
		sb.append(LocPort);
		editRx.setText(sb.toString().trim());
		sb.delete(0, sb.length());
		sb = null;
		
		//connect, if success all button can be clickable
		udpSocket.ConnectSocket();
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
		btnLand = (Button) findViewById(R.id.btn_land);
		txtCurrThr = (TextView) findViewById(R.id.text_currThrust);
		
		btnThr.setOnClickListener(this);
		btnRise.setOnClickListener(this);
		btnFall.setOnClickListener(this);
		btnLand.setOnClickListener(this);
		
		setUIState(false);
		editThr.setText("800");
		txtCurrThr.setText("Current Thrust: 800");
		//store current Thrust value
		currThr = Integer.parseInt(editThr.getText().toString());
	}
	
	private void sendThrust(){
		
		sb = new StringBuilder();
		sb.append("pwm ");
		sb.append(String.valueOf(currThr));
		sb.append("\n");
		//send out commend "pwm xxxx" 
		SendData = sb.toString();
		udpSocket.SendData(SendData);
		SendData = null;			
		sb.delete(0, sb.length());
		
		//Display send thrust on TextView
		sb.append("Current Thrust: ");
		sb.append(String.valueOf(currThr));
		txtCurrThr.setText(sb.toString());
		
		sb.delete(0, sb.length());
		sb = null;
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub		
		switch(v.getId()){
		case R.id.btn_thr:
			
			//get the input thrust value type in EditText
			currThr = Integer.parseInt(editThr.getText().toString().trim());
			sendThrust();
			
			break;
			
		case R.id.btn_rise:
			
			currThr += 50;
			sendThrust();
			
			break;
			
		case R.id.btn_fall:
			
			currThr -= 50;
			sendThrust();
			
			break;
			
		case R.id.btn_land:
			
			currThr = 800;
			sendThrust();
			
			break;
		default:;
		
		}
	}

}

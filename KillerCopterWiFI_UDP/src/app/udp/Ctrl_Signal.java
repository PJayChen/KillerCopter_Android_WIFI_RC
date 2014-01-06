package app.udp;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Ctrl_Signal extends Activity implements View.OnClickListener, SensorEventListener{

	
	//View -----------------------------
	private EditText editRx, editThr;
	private Button btnThr, btnRise, btnFall, btnLand, btnAcce;
	private TextView txtCurrThr;
	private int currThr;  //store current thrust value

	//UDP ------------------------------
	private String IP;
	private int TarPort, LocPort;
	private udpthread udpSocket = null;
	StringBuilder sb = null;
	private String SendData;
	
	//Sensor ---------------------------
	private Sensor acce; //accelerometer
	private SensorManager mySensorManager; //the manager of all sensor
	private boolean accFlag = true; //enable accelerometer or not
	private float x, y;
	
	//Thread ---------------------------	
	private static final int STR_ACC = 1;
	//UI(main) thread handler
	private Handler UI_Handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			
			switch(msg.what){
				case STR_ACC:
					btnAcce.setText(msg.getData().getString("pitch_SP") + msg.getData().getString("roll_SP")); 
					udpSocket.SendData(msg.getData().getString("pitch_SP_udp"));
					udpSocket.SendData(msg.getData().getString("roll_SP_udp"));
					this.removeMessages(STR_ACC);					
					System.out.println("STR_ACC running!!!!");
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	};
	
	//A thread for determine Setpoint  ------------------------------------
	private Thread t_acce;
	private boolean t_run_flag = true;
	private Runnable run_updateAcc = new Runnable(){
		public void run(){
			
			System.out.println("mThread is working!!!!");
						
			String pitch_SP, roll_SP; //setting point of pitch and roll
			
			while(t_run_flag){
				try {
					System.out.println("mThread delay!!!!");
					
					Thread.sleep(200);
					
					Bundle dataBd = new Bundle();
					
					if(x > 5){
						pitch_SP = "pitch p";
					}else if(x < -5){
						pitch_SP = "pitch n";
					}else{
						pitch_SP = "pitch";
					}
					
					dataBd.putString("pitch_SP", pitch_SP + "(" + String.valueOf(x) + "), " );
					//btnAcce.setText(pitch_SP + "(" + String.valueOf(x) + "), " );
					pitch_SP = pitch_SP + "\n";
					dataBd.putString("pitch_SP_udp", pitch_SP);
					//udpSocket.SendData(pitch_SP);
					
					if(y > 5){
						roll_SP = "roll p";
					}else if(y < -5){
						roll_SP = "roll n";
					}else{
						roll_SP = "roll";
					}
					
					dataBd.putString("roll_SP", roll_SP + "(" + String.valueOf(y) + ")");
					//btnAcce.setText( btnAcce.getText() + roll_SP + "(" + String.valueOf(y) + ") ->");
					roll_SP = roll_SP + "\n";
					dataBd.putString("roll_SP_udp", roll_SP);
					//udpSocket.SendData(roll_SP);
					
					Message msg = new Message();
					msg.what = STR_ACC;
					msg.setData(dataBd);
					UI_Handler.sendMessage(msg);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}//End of while
			
		}//End of run()
	};
	
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
		btnAcce = (Button) findViewById(R.id.btnAcc);
		txtCurrThr = (TextView) findViewById(R.id.text_currThrust);
		
		btnThr.setOnClickListener(this);
		btnRise.setOnClickListener(this);
		btnFall.setOnClickListener(this);
		btnLand.setOnClickListener(this);
		btnAcce.setOnClickListener(this);
		
		btnAcce.setText("Accelerometer OFF");
		setUIState(false);
		editThr.setText("800");
		txtCurrThr.setText("Current Thrust: 800");
		//store current Thrust value
		currThr = Integer.parseInt(editThr.getText().toString());
	}
	
	private void sendThrust(){
		
		sb = new StringBuilder();

		if(currThr > 800){
			sb.append("pwm ");
			sb.append(String.valueOf(currThr));
			sb.append("\n");
		}else {
			sb.append("\n");
		}
		
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
			
			currThr = (currThr >= 1800)?1800:currThr + 50;
			sendThrust();
			
			break;
			
		case R.id.btn_fall:
			
			currThr = (currThr <= 800)?800:currThr - 50;
			sendThrust();
			
			break;
			
		case R.id.btn_land:
			
			currThr = 800;
			sendThrust();

			break;
			
		case R.id.btnAcc:
			if(accFlag){
				mySensorManager.registerListener(this, acce, SensorManager.SENSOR_DELAY_NORMAL);
				accFlag = false; //acce will turn off while push down the button next time 
				
				//Create a thread for Tx Setpoint and start it ------
				t_run_flag = true;
				t_acce = new Thread(run_updateAcc);		
				t_acce.start();
				
			}else{
				mySensorManager.unregisterListener(this, acce);
				accFlag = true; //acce will turn on while push down the button next time
				
				//Stop the thread for Tx Setpoint ------
				t_run_flag = false;
				t_acce.interrupt();
				t_acce = null;
				
				btnAcce.setText("Accelerometer OFF");
			}
			udpSocket.SendData("pitch\n");
			udpSocket.SendData("roll\n");
			break;
		default:;
		
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		x = event.values[0];
		y = event.values[1];
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ctrl_signal);
		
		setView();
		setSocket();
		
		mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		acce = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
	}
	
	protected void onDestroy(){
		super.onDestroy();
		//Stop Thread
		if(t_acce != null){
			t_run_flag = false;
			t_acce.interrupt();
			t_acce = null;
		}
		
	}
	
}

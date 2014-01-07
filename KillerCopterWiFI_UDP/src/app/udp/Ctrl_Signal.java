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
	private EditText editRx, editThr, editScale;
	private Button btnThr, btnRise, btnFall, btnLand, btnAcce, btnShutdown, btnScale;
	private TextView txtCurrThr;
	//Variable -------------------------
	private int currThr;  //store current thrust value
	private int ThrScale;

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
	private static final int T_ACC = 1, T_LANDING = 2;
	//UI(main) thread handler
	private Handler UI_Handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			
			switch(msg.what){
				case T_ACC:
					btnAcce.setText(msg.getData().getString("pitch_SP") + msg.getData().getString("roll_SP")); 
					udpSocket.SendData(msg.getData().getString("pitch_SP_udp"));
					udpSocket.SendData(msg.getData().getString("roll_SP_udp"));
					this.removeMessages(T_ACC);				
					System.out.println("Accelermeter send data!!!!");
					if(t_run_flag == false){
						btnAcce.setText("Accelerometer OFF");
						udpSocket.SendData("pitch\n");
						udpSocket.SendData("roll\n");
					}
					break;
					
				case T_LANDING:
					sendThrust();
					this.removeMessages(T_LANDING);
					System.out.println("Landing!!!!");
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	};
	
	//Stop thread by set the runnable flag to FLASE 
	private boolean stopThread(Thread t){
		//boolean flag = true;
		if(t != null){
			//flag = false;
			t.interrupt();
			System.out.printf("Thread %s Stop\n", t.getName());
			t = null;						
		}
		return false;
	}
	//A thread for determine Setpoint  ------------------------------------
	private Thread t_acce;
	private boolean t_run_flag = true, flag_send = true;
	//private static final int S0_RST = 0, S1_GREATER = 1, S2_LESS = 2, S3_MID = 3, S4_IDLE = 4;
	//private int counter = 0, curr_state, next_state;
	//private float x_last = 0, y_last;
	private Runnable run_updateAcc = new Runnable(){
		public void run(){
			
			System.out.println("mThread is working!!!!");
						
			String pitch_SP = null, roll_SP = null; //setting point of pitch and roll
			
			//curr_state = 0;//reset to S0
			//next_state = 0;
			
			while(t_run_flag){
				try {			
					/*
					curr_state = next_state;
					
					System.out.printf("curr_state = %d\n", curr_state);
					
					switch(curr_state){
						case S0_RST:
								if(x > 5) next_state = S1_GREATER;
								else if(x < -5) next_state = S2_LESS;
								else next_state = S4_IDLE;
							break;
						case S1_GREATER:
								if(counter >= 2) next_state = S4_IDLE;
							break;
						case S2_LESS:
								if(counter >= 2) next_state = S4_IDLE;
							break;
						case S3_MID:
								if(counter >= 2) next_state = S4_IDLE;
							break;
						case S4_IDLE:
								if(x_last < 5 && x > 5) next_state = S1_GREATER;
								else if(x_last > -5 && x < -5) next_state = S2_LESS;
								else if( (x_last > 5 || x_last < -5) && (x < 5 || x < -5) ) next_state = S3_MID;
								else next_state = S4_IDLE;
							break;
					}//End of switch(curr_state)
					
					Bundle dataBd = new Bundle();
					switch(curr_state){
						case S0_RST:
								counter = 0;
								pitch_SP = "pitch";
								flag_send = false;
							break;
						case S1_GREATER:
								x_last = (x > 5)?x:x_last;
								Thread.sleep(800);								
								pitch_SP = "pitch p";
								counter++;
								flag_send = true;
							break;
						case S2_LESS:
								x_last = (x < -5)?x:x_last;
								Thread.sleep(800);
								pitch_SP = "pitch n";
								counter++;
								flag_send = true;
							break;
						case S3_MID:
								if(x < 5 || x > -5) x_last = x;
								Thread.sleep(800);
								pitch_SP = "pitch";
								counter++;
								flag_send = true;
							break;
						case S4_IDLE:
								Thread.sleep(800);
								counter = 0;
								flag_send = true;
							break;
					}
					
					dataBd.putString("pitch_SP", pitch_SP + "(" + String.valueOf(x) + "), " );
					//pitch_SP = pitch_SP + "\n";
					dataBd.putString("pitch_SP_udp", pitch_SP + "\n");
					*/
					
					Thread.sleep(1000);
					
					Bundle dataBd = new Bundle();
					//Pitch part ------------------------------------------------
					
					if(x > 5){
						pitch_SP = "pitch p";
					}else if(x < -5){
						pitch_SP = "pitch n";
					}else{
						pitch_SP = "pitch";
					}
					
					dataBd.putString("pitch_SP", pitch_SP + "(" + String.valueOf(x) + "), " );
					pitch_SP = pitch_SP + "\n";
					dataBd.putString("pitch_SP_udp", pitch_SP);
					
					
					//Roll part -------------------------------------------------
					if(y > 5){
						roll_SP = "roll p";
					}else if(y < -5){
						roll_SP = "roll n";
					}else{
						roll_SP = "roll";
					}
					
					dataBd.putString("roll_SP", roll_SP + "(" + String.valueOf(y) + ")");
					roll_SP = roll_SP + "\n";
					dataBd.putString("roll_SP_udp", roll_SP);
					
					//Send data to UI handler
					if(flag_send){
						Message msg = new Message();
						msg.what = T_ACC;
						msg.setData(dataBd);
						UI_Handler.sendMessage(msg);
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}//End of while
			
		}//End of run()
	};
	
	private Runnable run_landing = new Runnable(){
		public void run(){
			
			try {
				if(currThr > 800){
					if(currThr > 1200) currThr = 1200;
					Message msg = new Message();
					msg.what = T_LANDING;
					UI_Handler.sendMessage(msg);
					
					while(currThr > 800){
						Thread.sleep(1000);
						currThr -= 50;
						currThr = (currThr < 800)?800:currThr;
						
						msg = new Message();
						msg.what = T_LANDING;
						UI_Handler.sendMessage(msg);						
					}
					
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
		editScale = (EditText) findViewById(R.id.editScale);
		
		btnThr = (Button) findViewById(R.id.btn_thr);
		btnRise = (Button) findViewById(R.id.btn_rise);
		btnFall = (Button) findViewById(R.id.btn_fall);
		btnLand = (Button) findViewById(R.id.btn_land);
		btnAcce = (Button) findViewById(R.id.btnAcc);
		btnShutdown = (Button) findViewById(R.id.btn_shutdown);
		btnScale = (Button) findViewById(R.id.btn_scale);
		
		txtCurrThr = (TextView) findViewById(R.id.text_currThrust);
		
		btnThr.setOnClickListener(this);
		btnRise.setOnClickListener(this);
		btnFall.setOnClickListener(this);
		btnLand.setOnClickListener(this);
		btnAcce.setOnClickListener(this);
		btnShutdown.setOnClickListener(this);
		btnScale.setOnClickListener(this);
		
		//Initial Text ----------------------------
		btnAcce.setText("Accelerometer OFF");
		setUIState(false);
		editThr.setText("800");
		txtCurrThr.setText("Current Thrust: 800");
		//store current Thrust value
		editScale.setText("50");
		ThrScale = Integer.parseInt(editScale.getText().toString());
		btnScale.setText(String.valueOf(ThrScale));
		currThr = Integer.parseInt(editThr.getText().toString());
		
		btnLand.setClickable(false);
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
			currThr = (currThr >= 1800)?1800:currThr + ThrScale;
			sendThrust();			
			break;
			
		case R.id.btn_fall:			
			currThr = (currThr <= 800)?800:currThr - ThrScale;
			sendThrust();		
			break;
			
		case R.id.btn_land:
			Thread t_landing = new Thread(run_landing);
			t_landing.start();
			break;
			
		case R.id.btnAcc:
			if(accFlag){
				mySensorManager.registerListener(this, acce, SensorManager.SENSOR_DELAY_NORMAL);
				accFlag = false; //acce will turn off while push down the button next time 
				
				//Create a thread for Tx Setpoint and start it ------
				t_run_flag = true;
				t_acce = new Thread(run_updateAcc);
				t_acce.setName("t_acce");
				t_acce.start();
				System.out.printf("Thread %s Start\n", t_acce.getName());
				
			}else{
				mySensorManager.unregisterListener(this, acce);
				accFlag = true; //acce will turn on while push down the button next time
				
				//Stop the thread for Tx Setpoint ------
				t_run_flag = stopThread(t_acce);
				t_acce = null;
				
				btnAcce.setText("Accelerometer OFF");
				udpSocket.SendData("pitch\n");
				udpSocket.SendData("roll\n");
			}
			break;
		case R.id.btn_shutdown:
			currThr = 800;
			sendThrust();
			break;
		case R.id.btn_scale:
			ThrScale = Integer.valueOf(editScale.getText().toString());
			ThrScale = (ThrScale > 50)?50:( (ThrScale < 15)?15:ThrScale );
			btnScale.setText(String.valueOf(ThrScale));
			break;
		default:;
		
		}//End of switch
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
		t_run_flag = stopThread(t_acce);
		t_acce = null;
		
	}
	
}

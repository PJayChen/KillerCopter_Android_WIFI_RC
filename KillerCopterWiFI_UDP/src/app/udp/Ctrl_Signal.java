package app.udp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Ctrl_Signal extends Activity implements View.OnClickListener, SensorEventListener{

	
	//View -----------------------------
	private EditText editRx, editThr, editScale;
	private Button btnThr, btnRise, btnFall, btnLand, btnAcce, btnShutdown, btnScale, btnLock, btnTakeOff, btnRise25;
	private TextView txtCurrThr;
	//Variable -------------------------
	private int currThr;  //store current thrust value
	private int ThrScale;

	
	Vibrator vibrator = null;
	  
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
	private static final int T_ACC_PITCH = 1, T_ACC_ROLL = 2, T_LANDING = 3, T_ACC_OFF = 4;
	private boolean LandBtnColor = true;
	//UI(main) thread handler
	private Handler UI_Handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			
			switch(msg.what){
				case T_ACC_PITCH:
					btnAcce.setText(msg.getData().getString("pitch_SP") + msg.getData().getString("roll_SP")); 
					udpSocket.SendData(msg.getData().getString("pitch_SP_udp"));
					//udpSocket.SendData(msg.getData().getString("roll_SP_udp"));
					this.removeMessages(T_ACC_PITCH);				
					System.out.println("Accelermeter send data!!!!");
					if(t_run_flag == false){
						btnAcce.setText("Accelerometer OFF");
						//udpSocket.SendData("pitch\n");
						//udpSocket.SendData("roll\n");
					}
					break;
				case T_ACC_ROLL:
					udpSocket.SendData(msg.getData().getString("roll_SP_udp"));
					this.removeMessages(T_ACC_ROLL);
					
					//if(t_run_flag == false){
						//btnAcce.setText("Accelerometer OFF");
						//udpSocket.SendData("pitch\n");
						//udpSocket.SendData("roll\n");
					//}
					break;
				case T_LANDING:
					sendThrust();
					if(currThr <= 800) btnLand.setBackgroundColor(Color.RED);
					else if(LandBtnColor){
						btnLand.setBackgroundColor(Color.RED);
						LandBtnColor = !LandBtnColor;
					}else{
						btnLand.setBackgroundColor(Color.GREEN);
						LandBtnColor = !LandBtnColor;
					}
					
					this.removeMessages(T_LANDING);
					System.out.println("Landing!!!!");
					break;
				case T_ACC_OFF:
					udpSocket.SendData(msg.getData().getString("outdata"));
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
	private boolean t_run_flag = true;
	private Runnable run_updateAcc = new Runnable(){
		public void run(){
			
			System.out.println("run_updateAcc is working!!!!");
						
			String pitch_SP = null, roll_SP = null; //setting point of pitch and roll			
				try {			
					while(t_run_flag){
						Thread.sleep(500);
						
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
						pitch_SP = null;
						
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
						roll_SP = null;
						
						//Send data to UI handler
						//if(flag_send){
						Message msg = new Message();
						msg.what = T_ACC_PITCH;
						msg.setData(dataBd);
						UI_Handler.sendMessage(msg);
						
						Thread.sleep(500);
						
						msg = new Message();
						msg.what = T_ACC_ROLL;
						msg.setData(dataBd);
						UI_Handler.sendMessage(msg);
						//}
					}//End of while
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
		}//End of run()
	};
	
	private Runnable turnOFF_Acc = new Runnable(){
		public void run(){
			try {
				Bundle dataBd = new Bundle();
				
				Message msg = new Message();
				msg.what = T_ACC_OFF;
				dataBd.putString("outdata", "pitch\n");
				msg.setData(dataBd);
				UI_Handler.sendMessage(msg);
				Thread.sleep(200);
				
				msg = new Message();
				msg.what = T_ACC_OFF;
				dataBd.putString("outdata", "roll\n");
				msg.setData(dataBd);
				UI_Handler.sendMessage(msg);
				Thread.sleep(200);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("turnOFF_Acc Finish");
		}
	};
	
	private Runnable run_landing = new Runnable(){
		public void run(){
			
			try {
				if(currThr > 800){
					if(currThr > 1400) currThr = 1400;
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
		
	}
	
	private boolean btnState;
	private void setBtnState(boolean state){
		btnThr.setClickable(state);
		btnRise.setClickable(state);
		btnFall.setClickable(state);
		btnLand.setClickable(state);
		btnAcce.setClickable(state);
		btnShutdown.setClickable(state);
		btnRise25.setClickable(state);
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
		btnLock = (Button) findViewById(R.id.btnLock);
		btnTakeOff = (Button) findViewById(R.id.btn_takeOff);
		btnRise25 = (Button) findViewById(R.id.btnRise25);
		
		txtCurrThr = (TextView) findViewById(R.id.text_currThrust);
		
		btnThr.setOnClickListener(this);
		btnRise.setOnClickListener(this);
		btnFall.setOnClickListener(this);
		btnLand.setOnClickListener(this);
		btnAcce.setOnClickListener(this);
		btnShutdown.setOnClickListener(this);
		btnScale.setOnClickListener(this);
		btnLock.setOnClickListener(this);
		btnTakeOff.setOnClickListener(this);
		btnRise25.setOnClickListener(this);
		
		btnTakeOff.setClickable(false);
		
		btnLand.setBackgroundColor(Color.RED);
		btnShutdown.setBackgroundColor(Color.YELLOW);
		//Initial Text ----------------------------
		btnAcce.setText("Accelerometer OFF");
		btnState = false;
		setBtnState(btnState);
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
		vibrator.vibrate(100);
		switch(v.getId()){
		case R.id.btn_thr:			
			//get the input thrust value type in EditText
			currThr = Integer.parseInt(editThr.getText().toString().trim());
			if(currThr > 1400 ) currThr = 1400;
			else if(currThr < 800) currThr = 800;
			sendThrust();			
			break;
			
		case R.id.btn_rise:			
			currThr = (currThr >= 1400)?1400:currThr + ThrScale;
			sendThrust();			
			break;
			
		case R.id.btn_fall:			
			currThr = (currThr <= 800)?800:currThr - ThrScale;
			sendThrust();		
			break;
			
		case R.id.btn_land:
			//Thread t_landing = new Thread(run_landing);
			//t_landing.start();
			udpSocket.SendData("land\n");
			currThr = 800;
			btnLand.setClickable(false);
					
			//Display send thrust on TextView
			sb = new StringBuilder();
			sb.append("Current Thrust: ");
			sb.append(String.valueOf(currThr));
			txtCurrThr.setText(sb.toString());
			
			btnTakeOff.setClickable(false);
			btnTakeOff.setBackgroundColor(Color.DKGRAY);
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
				
				t_acce = new Thread(turnOFF_Acc); //One-short thread used to turn accelerm.
				t_acce.start();
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
		case R.id.btnLock:
			btnState = !btnState;
			setBtnState(btnState);
			if(btnState) btnLock.setText("unlock");
			else btnLock.setText("lock");
			break;
		case R.id.btn_takeOff:
			if(currThr > 1300){
				currThr = 1375;
				sendThrust();
			}
			break;
		case R.id.btnRise25:
			currThr = (currThr >= 1400)?1400:currThr + 25;
			sendThrust();
			break;
		default:;
		
		}//End of switch
		if(currThr > 1300 && v.getId() == R.id.btnRise25){
			btnTakeOff.setClickable(true);
			btnTakeOff.setBackgroundColor(Color.GREEN);
		}else{
			btnTakeOff.setClickable(false);
			btnTakeOff.setBackgroundColor(Color.DKGRAY);
		}
		
		if(currThr > 800 && btnState){
			btnLand.setClickable(true);
			btnLand.setBackgroundColor(Color.GREEN);
		}else{
			btnLand.setClickable(false);
			btnLand.setBackgroundColor(Color.RED);
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
		
		vibrator = (Vibrator) getApplication().getSystemService(Context.VIBRATOR_SERVICE);
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

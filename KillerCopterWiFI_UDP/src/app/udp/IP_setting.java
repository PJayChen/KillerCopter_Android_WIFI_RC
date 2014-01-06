package app.udp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class IP_setting extends Activity implements View.OnClickListener{

	private EditText editIP;
	private EditText editTarPort;
	private EditText editLocPort;
	private Button btnConnect;
	private Button btnST_MW;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ip_setting);
		
		setView();		
	}
	
	public void setView(){
		editIP = (EditText) findViewById(R.id.editThr);
		editTarPort = (EditText) findViewById(R.id.editTargetPort);
		editLocPort = (EditText) findViewById(R.id.editLocPort);
		btnConnect = (Button) findViewById(R.id.btn_thr);
		btnST_MW = (Button) findViewById(R.id.btnudp);
		
		btnConnect.setOnClickListener(this);
		btnST_MW.setOnClickListener(this);
		
		//editIP.setText("192.168.0.10");
		//editTarPort.setText("8080");
		editIP.setText("10.42.0.1");
		editTarPort.setText("55056");
		
		editLocPort.setText("55056");
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		switch(v.getId()){
		case R.id.btn_thr:
			Intent intent = new Intent();
			intent.setClass(IP_setting.this, Ctrl_Signal.class);
			
			//The information for Ctrl_Signal
			intent.putExtra("IP", editIP.getText().toString().trim());
			intent.putExtra("TarPort", editTarPort.getText().toString().trim());
			intent.putExtra("LocPort", editLocPort.getText().toString().trim());
			
			startActivity(intent);
			//IP_setting.this.finish();
			break;
		
		case R.id.btnudp:
			Intent intent_UDP = new Intent();
			intent_UDP.setClass(IP_setting.this, udpmainatt.class);
			
			startActivity(intent_UDP);
			IP_setting.this.finish();
			
			break;
		default:;
		}//End of switch(v.getId())
	}//End of onClick(View v)

}

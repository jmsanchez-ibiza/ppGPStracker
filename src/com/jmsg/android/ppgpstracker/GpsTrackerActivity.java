package com.jmsg.android.ppgpstracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class GpsTrackerActivity extends Activity {
	
	private static final String tag = "GpsTrackerActivity";
	private static final boolean mostrarToast = false;

	// Variables para acceso al UI
	Button btnRegistro, btnIntensivo, btnReiniciar;
	TextView txtStatus, txtDateTime, txtLastLocationInfo,
		txtLastLocationInfoExtra;
	
	// Variables para enlace con el servicio
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnRegistro = (Button)findViewById(R.id.buttonRegistro);
        btnIntensivo = (Button)findViewById(R.id.buttonIntensivo);
        btnReiniciar = (Button)findViewById(R.id.buttonReiniciar);
        txtStatus = (TextView)findViewById(R.id.txtStatus);
        txtDateTime = (TextView)findViewById(R.id.txtDateTime);
        txtLastLocationInfo = (TextView)findViewById(R.id.txtLastLocationInfo);
        txtLastLocationInfoExtra = (TextView)findViewById(R.id.txtLastLocationInfoExtra);
        
        btnRegistro.setOnClickListener(btnRegistroListener);
        btnIntensivo.setOnClickListener(btnIntensivoListener);
        btnReiniciar.setOnClickListener(btnReiniciarListener);

        // restoreMe(savedInstanceState);
        
        // Al crear la actividad, si no está corriendo el servicio lo iniciamos automáticamente
        if (!GpsService.isRunning()){
        	startAndBind();
        }
        
        if (mostrarToast)
        	Toast.makeText(getBaseContext(),
				"onCreate",
				Toast.LENGTH_SHORT)
				.show();
        
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	if (mostrarToast)
    		Toast.makeText(getBaseContext(),
				"onResume",
				Toast.LENGTH_SHORT)
				.show();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// Hay que enlazar el UI con el servicio por si acaso
    	doBindService();
    	
    	if (mostrarToast)
    		Toast.makeText(getBaseContext(),
				"onResume",
				Toast.LENGTH_SHORT)
				.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e(tag, "Fallo al intentar desenlazar del servicio", t);
        }
        
        if (mostrarToast)
        	Toast.makeText(getBaseContext(),
				"onDestroy",
				Toast.LENGTH_SHORT)
				.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Grabamos los datos que sean necesarios
        outState.putString("txtStatusCaption", txtStatus.getText().toString());
        outState.putString("txtDateTimeCaption", txtDateTime.getText().toString());
        outState.putString("txtLastLocationInfoCaption", txtLastLocationInfo.getText().toString());
        outState.putString("txtLastLocationInfoExtraCaption", txtLastLocationInfoExtra.getText().toString());
        outState.putString("btnRegistrar", btnRegistro.getText().toString());
        
        //if (mostrarToast)
        	Toast.makeText(getBaseContext(),
				"onSaveInstanceState",
				Toast.LENGTH_SHORT)
				.show();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        if (state!=null) {
        	// Restauramos los datos que fueron guardados.
        	txtStatus.setText(state.getString("txtStatusCaption"));
        	txtDateTime.setText(state.getString("txtDateTimeCaption"));
        	txtLastLocationInfo.setText(state.getString("txtLastLocationInfoCaption"));
        	txtLastLocationInfoExtra.setText(state.getString("txtLastLocationInfoExtraCaption"));
        	String estadoBoton= state.getString("btnRegistrar");
        	
        	if(estadoBoton.equals(R.string.btnCaptionRegistroIniciar)){
        		// Si el botón está con el caption "iniciar" es que está parado
        		actualizarBotones(false);
        	} else actualizarBotones(true);
        	
        	// Al entrar aquí se ha de enlazar de nuevo el UI con el servicio
        	doBindService();
        	
       		//if (mostrarToast)
       			Toast.makeText(getBaseContext(),
    				"onRestoreInstanceState",
    				Toast.LENGTH_SHORT)
    				.show();       			
        }
    }
    
    private void restoreMe(Bundle state) {
        if (state!=null) {
        	// Restauramos los datos que fueron guardados.
        	txtStatus.setText(state.getString("txtStatusCaption"));
        	txtDateTime.setText(state.getString("txtDateTimeCaption"));
        	txtLastLocationInfo.setText(state.getString("txtLastLocationInfoCaption"));
        	txtLastLocationInfoExtra.setText(state.getString("txtLastLocationInfoExtraCaption"));
        	
       		//if (mostrarToast)
       			Toast.makeText(getBaseContext(),
    				"restoreMe",
    				Toast.LENGTH_SHORT)
    				.show();
        }
    }
    
    // Métodos de respuesta al click en los botones
	private OnClickListener btnRegistroListener = new OnClickListener() {
		public void onClick(View v) {
			if(GpsService.isRunning()){
				Log.d(tag, "preClick: Running is true");
			} else Log.d(tag, "preClick: Running is false");
			
			/**
			 * JMSG 04/09/2013, 20:21:34
			 * Hay dos estados, según si esta el servicio corriendo o no
			 */
			if (GpsService.isRunning()) {
				stopAndUnbind();
	            
			} else {
				// El servicio está desactivado, pues vamos a activarlo
				startAndBind();
			}
			
			if(GpsService.isRunning()){
				Log.d(tag, "postClick: Running is true");
			} else Log.d(tag, "postClick: Running is false");
			
		}
	};
	
	private void startAndBind(){

		// El servicio está desactivado, pues vamos a activarlo
		startService(new Intent(GpsTrackerActivity.this, GpsService.class));
		doBindService();
		
		// Al inicia de nuevo el servicio, forzamos el modo NORMAL
		sendMessageToService(GpsService.MSG_SET_MODO_NORMAL);

		actualizarBotones(true);
	}
	
	private void stopAndUnbind() {
		// Antes de desactivar el servicio, forzamos el modo NORMAL
		sendMessageToService(GpsService.MSG_SET_MODO_NORMAL);
		
		// El servicio está activado, por lo tanto hay que desactivarlo
		doUnbindService();
        stopService(new Intent(GpsTrackerActivity.this, GpsService.class));
        
        actualizarBotones(false);
	}
	
	private void actualizarBotones(boolean isRunning) {
		
		// Poner el caption de usoIntensivo
		btnIntensivo.setText(
				GpsService.isModoIntensivo()?
						R.string.btnCaptionUsoNormal:R.string.btnCaptionUsoIntensivo);
		
		// No sé porqué pero esto va al revés para que funcione
		if (isRunning){
			// EL servicio esta corriendo
			btnRegistro.setText(R.string.btnCaptionRegistroDetener);
			
			btnIntensivo.setEnabled(true);
			btnReiniciar.setEnabled(true);
			
		} else {
			// El servicio NO está corriendo
			btnRegistro.setText(R.string.btnCaptionRegistroIniciar);
		
			btnIntensivo.setEnabled(false);
			btnReiniciar.setEnabled(false);
		}
		
		if (isRunning) {
			Log.d(tag, "Actualizar Botones isRunning = true");
        } else {
			Log.d(tag, "Actualizar Botones isRunning = false");
        }
		Log.d(tag, 
				"btnRegistro: "+btnRegistro.getText()+
				" btnIntensivo: "+btnIntensivo.getText()+
				" btnReiniciar: "+btnReiniciar.getText()
				);
	}
	
	private OnClickListener btnIntensivoListener  = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			/**
			 * JMSG 04/09/2013, 20:21:34
			 * Hay dos estados, según el valor de GpsService.isModoIntensivo
			 *
			 */
			if (GpsService.isModoIntensivo()) {
				// Estamos trabajando en modo intensivo, cambiar al modo normal
				sendMessageToService(GpsService.MSG_SET_MODO_NORMAL);
				Toast.makeText(getBaseContext(),
						"Ha seleccionado USO NORMAL",
						Toast.LENGTH_LONG)
						.show();
				// Cambiar el caption del boton
				btnIntensivo.setText(R.string.btnCaptionUsoIntensivo);
			} else {
				// Estamos en modo normal, cambiar al modo intensivo
				sendMessageToService(GpsService.MSG_SET_MODO_INTENSIVO);
				Toast.makeText(getBaseContext(),
						"Ha seleccionado USO INTENSIVO",
						Toast.LENGTH_LONG)
						.show();
				// Cambiar el caption del boton
				btnIntensivo.setText(R.string.btnCaptionUsoNormal);
			}
									
			// TODO, que despues de un tiempo "prudencial" se desconecte el modo
			// intensivo automáticamente y vuelva al modo normal, para evitar olvidos
			// y consumo excesivo de batería.
		}
	};

	private OnClickListener btnReiniciarListener  = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// Reiniciar el servicio, parando y después iniciándolo
			stopAndUnbind();
			// SLEEP 2 SECONDS HERE ...
		    Handler handler = new Handler(); 
		    handler.postDelayed(new Runnable() { 
		         public void run() {
		        	 // Volver a iniciar
		        	 startAndBind(); 
		         } 
		    }, 2000); 
			Toast.makeText(getBaseContext(),
						"Servicio GPS reiniciado.",
						Toast.LENGTH_SHORT)
						.show();
		}
	};
	
//    private OnClickListener btnStartListener = new OnClickListener() {
//        public void onClick(View v){
//            startService(new Intent(GpsTrackerActivity.this, GpsService.class));
//        }
//    };
//    private OnClickListener btnStopListener = new OnClickListener() {
//        public void onClick(View v){
//            doUnbindService();
//            stopService(new Intent(GpsTrackerActivity.this, GpsService.class));
//        }
//    };
//    private OnClickListener btnBindListener = new OnClickListener() {
//        public void onClick(View v){
//            doBindService();
//        }
//    };
//    private OnClickListener btnUnbindListener = new OnClickListener() {
//        public void onClick(View v){
//            doUnbindService();
//        }
//    };
//    private OnClickListener btnUpby1Listener = new OnClickListener() {
//        public void onClick(View v){
//            sendMessageToService(1);
//        }
//    };
//    private OnClickListener btnUpby10Listener = new OnClickListener() {
//        public void onClick(View v){
//            sendMessageToService(10);
//        }
//    };
    
    
    /**
     * Aquí recibimos los mensajes desde el servicio
     * @author josemsg
     *
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case GpsService.MSG_SET_VALUE:
  
            	// Obtener los datos recibidos
				String strDateTime = msg.getData().getString("DateTime");
				String strLatitude = msg.getData().getString("Latitude");
				String strLongitude = msg.getData().getString("Longitude");
				String strAccuracy = msg.getData().getString("Accuracy");
				String strAltitude = msg.getData().getString("Altitude");
				String strSpeed = msg.getData().getString("Speed");
				String strSatellites = msg.getData().getString("Satellites");
				String strContadores = String.format("%d (%d) -%d db:%d", 
						msg.getData().getInt("cntObtenidos", 0),
						msg.getData().getInt("cntEnviados", 0),
						msg.getData().getInt("cntRechazados", 0),
						msg.getData().getInt("cntDbPointCounter", 0)
						);
				
				Log.i(tag,"IncomingHandler:" + strDateTime+" "+strLatitude+" "+ strLongitude +
						" " + strAccuracy+ " "+ strAltitude+" "+strSpeed+
						" " + strSatellites + " " + strContadores);
				
				// Actualizar el UI
				txtStatus.setText("Status");
				txtDateTime.setText(strDateTime+ " Puntos: "+strContadores);
				txtLastLocationInfo.setText("Lat: "+strLatitude+" Lon: "+strLongitude);
				txtLastLocationInfoExtra.setText("Acc: "+strAccuracy+" Alt: "+strAltitude+
						" Vel: "+strSpeed+" Sat:"+strSatellites 	);
				break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            txtStatus.setText(R.string.statusConectado);
            try {
                Message msg = Message.obtain(null, GpsService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            	// Si estamos aquí es que ni si quiera heos podido iniciar el servicio
            }
        }

        public void onServiceDisconnected(ComponentName className) {
        	// Este método es llamado cuando la conexion con el servicio ha sido inesperadamente desconectado
            mService = null;
            txtStatus.setText(R.string.statusDesconectado);
        }
    };

    
	// Enviar mensaje al servicio
    private void sendMessageToService(int msgID) {
        if (mIsBound) {
            if (mService != null) {
                try {
                	Message msg = Message.obtain(null, msgID, 0, 0);
					msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }

    /**
     * Enlazar esta actividad y UI al servicio
     */
    void doBindService() {
        bindService(new Intent(this, GpsService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        txtStatus.setText("Enlazando con el servicio.");
    }
    
    /**
     * Desenlazar esta actividad del servicio
     */
    void doUnbindService() {
        if (mIsBound) {
        	// Si hemos recibido el servicio, y por lo tanto estábamos registrado, ahora toca desregistrarse.
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, GpsService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                	// No podemos nada en especial si el servicio ha petado
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Desenlazarnos de la conexión existente.
            unbindService(mConnection);
            mIsBound = false;
            txtStatus.setText("Desconectado.");
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gps_tracker, menu);
		return true;
	}

}

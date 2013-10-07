package com.jmsg.android.ppgpstracker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

//import com.example.ejemplo_service.MainActivity;
//import com.example.ejemplo_service.R;
//import com.example.ejemplo_service.MyService.IncomingHandler;

public class GpsService extends Service {
	
	// Etiqueta para LOG
	private static final String tag = "GpsService";

	// Variables y objetos para usar en toda la clase del servicio
	private NotificationManager nm;
	private int cntObtenidos = 0, cntRechazados = 0, cntEnviados = 0;
	private static boolean isRunning = false;
	private static boolean isLocationRunning = false;
	private static boolean isModoIntensivo = false;
	
	ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Llevamos un
																// registro de
																// todos los
																// clientes
																// registrados
																// hasta el
																// momento.
	int mValue = 0; // Guarda el último valor establecido por un cliente.

	// Mensajes utilizados entre el servicio y el UI
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_SET_VALUE = 3;
	static final int MSG_SET_MODO_NORMAL = 4;
	static final int MSG_SET_MODO_INTENSIVO = 5;
	final Messenger mMessenger = new Messenger(new IncomingHandler()); // Utilizado
																		// para
																		// que
																		// desde
																		// los
																		// clientes
																		// se
																		// envien
																		// mensajes
																		// a
																		// IncomingHandler.

	// Para la base de datos
	private DatabaseHandler db;
	
	// Para el servicio de localización
	private LocationManager lm;
	private LocationListener locationListener;
	private int minTimeMillis = 10000,	// 3 seg mientras probamos, luego 30 segundos
			minDistanceMeters = 10,		// 10 mts
			minAccuracyMeters = 50,		// 50 mts
			maxNumRegistrosEnDb = 30; 	// 5 reg mientras probamos, luego 2 x min x 60 min = 120 mas menos por hora 
	// TODO hacer que minTimeMillis = 30000, 30 seg
	
	// Datos de la última localización obtenida 
	private String lastDateTime,
					lastLatitude,
					lastLongitude,
					lastAccuracy,
					lastAltitude,
					lastSpeed,
					lastSatellites;
	private int lastPointCounter = 0;
	
	private Location lastLoc;
	
	// Datos de conexión a Open GTS
	private String serverName = "213.96.91.211:8080/",
					gprmcName = "gprmc/Data?",
					accountName = "piscinaspepe",
					deviceName = "josemsg";
	// TODO, leer estos campos desde las preferencias

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	/**
	 * JMSG 04/09/2013, 20:55:45 Gestionar los mensajes que entran al servicio
	 */
	class IncomingHandler extends Handler {
		// Handler of incoming messages from clients.
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SET_MODO_NORMAL:
				// Vemos cual es el tiempo de intervalo que tenemos seleccinado y ponerlo
				// En el caso de que el servicio esté corriendo
				
				// TODO de momento pongo 20000 milis (20 seg), pero tendría que usar el valor 
				// almacenado en preferencias
				actualizarLoggerService(LocationManager.GPS_PROVIDER, minTimeMillis,
						minDistanceMeters, locationListener);
				isModoIntensivo = false;
				break;
			case MSG_SET_MODO_INTENSIVO:
				// Establecer el intervalo en el valor mínimo -> 0
				// En el caso de que el servicio esté corriendo
				actualizarLoggerService(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				isModoIntensivo = true;
				break;
				
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * JMSG 04/09/2013, 20:58:15 Desde aquí enviamos los datos a la actividad UI
	 * en todos los mClients registrados
	 */
	private void sendMessageToUI() {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				// Enviar datos al UI
				Bundle b = new Bundle();
				b.putString("DateTime", lastDateTime);
				b.putString("Latitude", lastLatitude);
				b.putString("Longitude", lastLongitude);
				b.putString("Accuracy", lastAccuracy);
				b.putString("Altitude", lastAltitude);
				b.putString("Speed", lastSpeed);
				b.putString("Satellites", lastSatellites);
				b.putInt("cntEnviados", cntEnviados);
				b.putInt("cntObtenidos", cntObtenidos);
				b.putInt("cntRechazados", cntRechazados);
				b.putInt("cntDbPointCounter", lastPointCounter);
				
				Message msg = Message.obtain(null, MSG_SET_VALUE);
				msg.setData(b);
				mClients.get(i).send(msg);

			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going
				// through the list from back to front so this is safe to do
				// inside the loop.
				mClients.remove(i);
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(tag, "GpsService Iniciado.");

		startLoggerService(); // Iniciar el servicio de localización por GPS
		showNotification(); // Crear y mostrar la notificación
		isRunning = true; // El servicio está corriendo
		
		// Cargar valores almacenados en preferencias para la configuración del servicio
		// TODO
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		shutdownLoggerService(); // Al destruir el servicio también tenemos que
									// detener el servicio de localización

		// if (timer != null) {timer.cancel();}

		// Resetear loa contadores
		cntObtenidos = 0;
		cntRechazados = 0;
		cntEnviados = 0;

		nm.cancel(R.string.service_started); // Cancelar la notificación
		Log.i(tag, "GpsService detenido.");
		isRunning = false;

	}

	/**
	 * JMSG 05/09/2013, 18:19:59 Inicia el servicio de localización, llamado
	 * desde onCreate
	 */
	private void startLoggerService() {
		// Inicializar el LocationManager y el locationListener
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMillis,
				minDistanceMeters, locationListener);

		// Registrar que el servicio de localización está corriendo.
		isLocationRunning = true;

		// Inicializar la base de datos
		db = new DatabaseHandler(this);
        
	}
	
	/**
	 * JMSG 07/09/2013, 11:15:10
	 * Actualiza el servicio de localización
	 * @param provider, proveedor de localización
	 * @param minTime, tiempo mínimo en milisegundos entre cada intento de obtener localización
	 * @param minDistance, distancia mínima en metros para que se actualice la posición
	 * @param listener, el LocationListener encargado de obtener la localización
	 */
	private void actualizarLoggerService(String provider, long minTime, float minDistance, LocationListener listener) {
		// Actualizar las actualizaciones del Servicio logger de GPS
		lm.requestLocationUpdates(provider, minTime, minDistance, listener);
	}

	public class MyLocationListener implements LocationListener {
		public void onLocationChanged(Location loc) {
			/**
			 * Hemos obtenido una nueva localización
			 * 1. Comprobar si es una localización válida
			 * 1.1. Que no sea null.
			 * 1.2. Que tenga precisión suficiente.
			 * 2. Si la localización es válida.
			 * 2.1. Enviar la localización al servidor OPEN GTS
			 * 2.2. Graba la posición en la base de datos.
			 * 2.3. Comprobar el número de puntos en la db y si es superior a maxNumRegistrosEnDb
			 *      entonces hay que exportar los puntos al fichero de texto de la SDcard y 
			 *      luego borrar los registros de la base de datos
			 * 3. Actualizar  variables de "lastLocation" y 
			 *    los contadores de puntos obtenidos, puntos enviados, puntos rechazados y 
			 *    puntos en la db
			 * 4. Enviar los datos a los clientes UI registrados.
			 */

			// Para poder obtener la hora

			GregorianCalendar greg = new GregorianCalendar(
					TimeZone.getTimeZone("Europe/Madrid"));
			DateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String fechaHora = timestampFormat.format(greg.getTime());
				// La fecha y hora del sistema en el momento que hemos la posición
			
			// 1. Comprobar si es una localización válida
			// 1.1. Que no sea null.
			if (loc != null) {
				boolean pointIsRecorded = false;
				int cuantosRegistros;

				try {
					// 1.2. Que tenga precisión suficiente.
					
					// TODO, determinar si programamos que al estar en modo intensivo
					// la precisión (accuracy) se tenga que comprobar o cualquiera
					// nos vale
					// 1.2.1 Si estamos en modo intensivo, cualquier accuracy nos vale
					// De momento no pedimos que tenga al menos minAccuracyMeters
					
					
					// TODO, hay que deja el IF como sigue y quita el if (true)
					// if (loc.hasAccuracy()
					//		&& loc.getAccuracy() <= minAccuracyMeters) {
						
					// Para hacer debug en el emulador dejamos pasar todos los puntos
					if (true) {	
						pointIsRecorded = true;
						
						// 2. Si la localización es válida.
						// 2.1. Enviar la localización al servidor OPEN GTS
						String url = "http://" + getServerName()
								+ getGprmcName() + "acct=" + getAccountName()
								+ "&" + "dev=" + getDeviceName() + "&"
								+ "gprmc=" + OpenGTSutils.GPRMCEncode(loc);

						Log.i(tag, "URL: " + url);

						AsyncHttpClient client = new AsyncHttpClient();						
						client.get(url, new AsyncHttpResponseHandler(){
							@Override
							public void onStart() {
								Log.i(tag, "AsyncHttpClient onStart");
							}
							@Override
							public void onSuccess(String response) {
								Log.i(tag,"AsyncHttpClient SUCCESS "+response);
								cntEnviados++;
							}
							@Override
						    public void onFailure(Throwable e, String response) {
								Log.i(tag,"AsyncHttpClient FAILURE "+response);
							}
							
							@Override
							public void onFinish() {
								Log.i(tag,"AsyncHttpClient onFinish");
							}
						});

						// 2.2. Graba la posición en la base de datos.
						DbPointsManager punto = new DbPointsManager(
								timestampFormat.format(loc.getTime()), 
								loc.getLatitude(), 
								loc.getLongitude(), 
								loc.hasAltitude() ? loc.getAltitude() : 0.0, 
								loc.hasAccuracy() ? loc.getAccuracy() : 0.0, 
								loc.hasSpeed() ? loc.getSpeed() : 0.0, 
								loc.hasBearing() ? loc.getBearing() : 0.0, 
								false);
								// TODO, de momento el ultimo campo "puntoEnviado", sera false, hasta que
								// incorporemos un sistema para poder controlar que puntos se han podido enviar
								// y cuales no.
						
						db.addDbPoint(punto);
						
						// Saber cuantos registros hay en la base de datos
						cuantosRegistros = db.getDbPointsCount();
						
						Log.d(tag,"Reg #: "+cuantosRegistros+" Max #: "+maxNumRegistrosEnDb);
						
						// * 2.3. Comprobar el número de puntos en la db y si es superior a maxNumRegistrosEnDb
						// *      entonces hay que exportar los puntos al fichero de texto de la SDcard y 
						// *      luego borrar los registros de la base de datos
						if (cuantosRegistros > maxNumRegistrosEnDb) {
							if(db.doDbExport(deviceName)>0) {
								// * 2.3.1 Se ha exportado los datos pues ahora borrar el
								// * 	   contenido de la tabla "points" de la base de datos
								int i;
								i = db.deleteDbPointAll();
								Log.d(tag,"Se borraron "+i+"PUNTOS");
							}
							
						}
						
						Log.i(tag, "addDbPoint: " + punto.toString());
						
					}

				} catch (Exception e) {
					Log.e(tag, e.toString());
				} finally {
					// Si estamos aqui algo ha ido mal así que ...
					/* Este código en principio no es necesario
					if (db.isOpen())
						db.close();
					*/
				}

				// Toast
				if (pointIsRecorded) {
					cntObtenidos++;
					/* TODO
					if (showingDebugToast)
						Toast.makeText(
								getBaseContext(),
								"Location stored: \nLat: "
										+ sevenSigDigits.format(loc
												.getLatitude())
										+ " \nLon: "
										+ sevenSigDigits.format(loc
												.getLongitude())
										+ " \nAlt: "
										+ (loc.hasAltitude() ? loc
												.getAltitude() + "m" : "?")
										+ "\nVel: "
										+ (loc.hasSpeed() ? loc.getSpeed()
												+ "m" : "?")
										+ " \nAcc: "
										+ (loc.hasAccuracy() ? loc
												.getAccuracy() + "m" : "?"),
								Toast.LENGTH_LONG).show();
					*/
				} else {
					cntRechazados++;
					/* TODO
					if (showingDebugToast)
						Toast.makeText(
								getBaseContext(),
								"Location not accurate enough: \nLat: "
										+ sevenSigDigits.format(loc
												.getLatitude())
										+ " \nLon: "
										+ sevenSigDigits.format(loc
												.getLongitude())
										+ " \nAlt: "
										+ (loc.hasAltitude() ? loc
												.getAltitude() + "m" : "?")
										+ "\nVel: "
										+ (loc.hasSpeed() ? loc.getSpeed()
												+ "m" : "?")
										+ " \nAcc: "
										+ (loc.hasAccuracy() ? loc
												.getAccuracy() + "m" : "?"),
								Toast.LENGTH_LONG).show();
					*/
				}
			}

			// 3. Actualizar  variables de "lastLocation" y 
			//    los contadores de puntos obtenidos, puntos enviados, puntos rechazados
			
			// Poner la fecha en YYMMDD HH:MM:SS
			StringBuffer buf = new StringBuffer(timestampFormat.format(loc.getTime()));
			buf.insert(4, '-');
			buf.insert(7, '-');
			buf.insert(10, ' ');
			buf.insert(13, ':');
			buf.insert(16, ':');
			
			// buf.append('Z');
			
			setLastDateTime(buf.toString());
			
			setLastLatitude(String.format("%1.6f", loc.getLatitude()));
			
			setLastLongitude(String.format("%1.6f", loc.getLongitude()));
			
			setLastAccuracy((loc.hasAccuracy() ? 
					String.format("%1.0f", loc.getAccuracy()) + "m" : "?"));
			
			setLastAltitude((loc.hasAltitude() ? 
					String.format("%1.0f", loc.getAltitude()) + "m" : "?"));
			
			setLastSpeed((loc.hasSpeed() ? String.format("%1.0f", loc.getSpeed()) + "m/s" : "?"));
			
			setLastSpeed(getLastSpeed() + " "
					+ (loc.hasSpeed() ? String.format("%1.0f", loc.getSpeed() * 3.6) + "km/s" : "?"));

			Bundle extras = loc.getExtras();
			if (extras != null) {
				setLastSatellites(extras.containsKey("satellites") ? extras
						.get("satellites").toString() : "0");
				Log.d(tag, "sat: " + getLastSatellites());
			} else
				Log.d(tag, "sat: extras es null");
			
			// Guardar el objeto de la última localización
			setLastLoc(loc);
			
			// Guardar el número de registros en la base de datos para luego actualizar el UI
			setLastPointCounter(db.getDbPointsCount());
			
			// 4. Enviar los datos a los clientes UI registrados.
			sendMessageToUI();

		}

		public void onProviderDisabled(String provider) {
			/* TODO
			 * if (showingDebugToast)
				Toast.makeText(getBaseContext(),
						"onProviderDisabled: " + provider, Toast.LENGTH_SHORT)
						.show();
			*/

		}

		public void onProviderEnabled(String provider) {
			/* TODO
			 * if (showingDebugToast)
				Toast.makeText(getBaseContext(),
						"onProviderEnabled: " + provider, Toast.LENGTH_SHORT)
						.show();
			*/

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			/* TODO
			String showStatus = "GPS estado desconocido";
			
			if (status == LocationProvider.AVAILABLE)
				showStatus = "GPS Disponible";
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				showStatus = "GPS Inaccesible temporalmente";
			if (status == LocationProvider.OUT_OF_SERVICE)
				showStatus = "GPS fuera de servicio";
			if (status != lastGpsStatus && showingDebugToast) {
				Toast.makeText(getBaseContext(),
						"Provider: " + provider + " GPS: " + showStatus,
						Toast.LENGTH_SHORT).show();
			}

			if (status != lastGpsStatus)
				currentStatus = showStatus;

			lastGpsStatus = status;

			if (extras != null) {
				currentSatellites = extras.containsKey("satellites") ? extras
						.get("satellites").toString() : "0";
				Log.d(tag, "sat: " + currentSatellites);
			} else
				Log.d(tag, "extras es null");
			*/
		}

	}

	private void showNotification() {
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.service_started);
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, GpsTrackerActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_label),
				text, contentIntent);

		/**
		 * JMSG 03/09/2013, 19:53:13 Hacer que no se pueda borrar la
		 * notificación
		 */
		notification.flags |= Notification.FLAG_NO_CLEAR;

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		nm.notify(R.string.service_started, notification);
	}

	/**
	 * JMSG 05/09/2013, 18:28:05 Detenemos el servicio de localización
	 */
	private void shutdownLoggerService() {
		isLocationRunning = false; // Informamos que el servicio de localización
									// se ha detenido
		lm.removeUpdates(locationListener); // Detenemos el servicio de
											// localización
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(tag, "GpsService: Recibido startID " + startId + ": " + intent);
		return START_STICKY; // run until explicitly stopped.
	}

	/**
	 * JMSG 05/09/2013, 18:49:17 Nos dice si el servicio está corriendo
	 * 
	 * @return boolean
	 */
	public static boolean isRunning() {
		return isRunning;
	}

	/**
	 * JMSG 05/09/2013, 18:49:47 Nos dice si el servicio de localización está
	 * corriendo
	 * 
	 * @return boolean
	 */
	public static boolean isLocationRunning() {
		return isLocationRunning;
	}
	
	/**
	 * JMSG 07/09/2013, 12:48:02
	 * Nos dice si el servicio de localización está
	 * en modo intensivo o normal
	 * 
	 * @return boolean
	 */
	public static boolean isModoIntensivo() {
		return isModoIntensivo;
	}

	// private void onTimerTick() {
	// Log.i("TimerTick", "Timer doing work." + counter);
	// try {
	// counter += incrementby;
	// sendMessageToUI(counter);
	//
	// } catch (Throwable t) { //you should always ultimately catch all
	// exceptions in timer tasks.
	// Log.e("TimerTick", "Timer Tick Failed.", t);
	// }
	// }

	/**
	 * Getters y Setters
	 * @return
	 */

	public String getLastDateTime() {
		return lastDateTime;
	}
	
	public void setLastDateTime(String lastDateTime) {
		this.lastDateTime = lastDateTime;
	}

	public String getLastLatitude() {
		return lastLatitude;
	}

	public void setLastLatitude(String lastLatitude) {
		this.lastLatitude = lastLatitude;
	}

	public String getLastLongitude() {
		return lastLongitude;
	}

	public void setLastLongitude(String lastLongitude) {
		this.lastLongitude = lastLongitude;
	}

	public String getLastAccuracy() {
		return lastAccuracy;
	}

	public void setLastAccuracy(String lastAccuracy) {
		this.lastAccuracy = lastAccuracy;
	}

	public String getLastAltitude() {
		return lastAltitude;
	}

	public void setLastAltitude(String lastAltitude) {
		this.lastAltitude = lastAltitude;
	}

	public String getLastSpeed() {
		return lastSpeed;
	}

	public void setLastSpeed(String lastSpeed) {
		this.lastSpeed = lastSpeed;
	}

	public String getLastSatellites() {
		return lastSatellites;
	}

	public void setLastSatellites(String lastSatellites) {
		this.lastSatellites = lastSatellites;
	}

	public Location getLastLoc() {
		return lastLoc;
	}

	public void setLastLoc(Location lastLoc) {
		this.lastLoc = lastLoc;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getGprmcName() {
		return gprmcName;
	}

	public void setGprmcName(String gprmcName) {
		this.gprmcName = gprmcName;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public int getLastPointCounter() {
		return lastPointCounter;
	}

	public void setLastPointCounter(int lastPointCounter) {
		this.lastPointCounter = lastPointCounter;
	}	
}

package com.jmsg.android.ppgpstracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {
	 
    // Variables Static
	private static final String tag = "DatabaseHandler";
    // Versión de la base de datos
    private static final int DATABASE_VERSION = 1;
 
    // Nombre de la base de datos
    private static final String DATABASE_NAME = "DB_pointsManager";
 
    // Nombre de la tabla de puntos de localización
    private static final String TABLE_POINTS = "points";
 
    // Nombres de las columnas de la tabla de puntos
    private static final String KEY_ID = "id";
    private static final String KEY_GMTTIMESTAMP = "gmttimestamp";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "lontitude";
    private static final String KEY_ALTITUDE = "altitude";
    private static final String KEY_ACCURACY = "accuracy";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_BEARING = "bearing";
    private static final String KEY_PUNTOENVIADO = "puntoenviado";
 
    // Constructor
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    // Crear tablas
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_POINTS_TABLE = "CREATE TABLE " + TABLE_POINTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
        		+ KEY_GMTTIMESTAMP + " VARCHAR,"
        		+ KEY_LATITUDE + " REAL,"
        		+ KEY_LONGITUDE + " REAL,"
        		+ KEY_ALTITUDE + " REAL,"
        		+ KEY_ACCURACY + " REAL,"
        		+ KEY_SPEED + " REAL,"
                + KEY_BEARING + " REAL,"
        		+ KEY_PUNTOENVIADO + " INTEGER" + ")";
        db.execSQL(CREATE_POINTS_TABLE);
    }
 
    // Actualizando la base de datos
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	
    	/**
    	 * JMSG 11/09/2013, 20:58:54
    	 * TODO
    	 * Por el momento optamos por borrar la tabla de puntos y volver a crearla,
    	 * pero lo normal sería hacer las modificaciones oportunas según la versión
    	 * mediante instrucciones SQL "ALTER TABLE" ...
    	 */
        // Borrar la tabla si existe
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POINTS);
 
        // Crear las tablas de nuevo
        onCreate(db);
    }
    
    /**
     * Los métodos para (Altas, Bajas, Modificaciones, Consultas, etc ...
     * 
     */
    // Añadir un nuevo punto
    void addDbPoint(DbPointsManager point) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_GMTTIMESTAMP, point.getGmttimestamp());
        values.put(KEY_LATITUDE, point.getLatitude());
        values.put(KEY_LONGITUDE, point.getLongitude());
        values.put(KEY_ALTITUDE, point.getAltitude());
        values.put(KEY_ACCURACY, point.getAccuracy());
        values.put(KEY_SPEED, point.getSpeed());
        values.put(KEY_BEARING, point.getBearing());
        values.put(KEY_PUNTOENVIADO, point.getPuntoEnviado()? 1 : 0);
        
        // Insertar el registro
        db.insert(TABLE_POINTS, null, values);
        db.close(); // cerrar la conexión con la base de datos
    }
 
    // Obtener un punto
    DbPointsManager getDbPoint(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        Cursor cursor = db.query(TABLE_POINTS, 
        		new String[] { KEY_ID, KEY_GMTTIMESTAMP, KEY_LATITUDE, KEY_LONGITUDE,
        						KEY_ALTITUDE, KEY_ACCURACY, KEY_SPEED, KEY_BEARING, KEY_PUNTOENVIADO },
        		KEY_ID + "=?",
                new String[] { String.valueOf(id) },
                null, null, null, null);
        
        if (cursor != null)
            cursor.moveToFirst();
 
        DbPointsManager point = new DbPointsManager(
        			Integer.parseInt(cursor.getString(0)), cursor.getString(1),
        			cursor.getDouble(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getDouble(4),
        			cursor.getDouble(5), cursor.getDouble(6), cursor.getInt(7) == 1);
        
        // Cerramos la base de datos
        if (db.isOpen()) {
			db.close();
		}
        
        // retornar Punto
        return point;
    }
     
    // Obtener una lista con todos los Puntos
    public List<DbPointsManager> getAllDbPoints() {
        List<DbPointsManager> pointList = new ArrayList<DbPointsManager>();
        
        // Seleccionar ALL Query
        String selectQuery = "SELECT  * FROM " + TABLE_POINTS;
 
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
 
        // Recorrer todo el cursor y añadir a la lista
        if (cursor.moveToFirst()) {
            do {
                DbPointsManager point = new DbPointsManager();
                point.setID(Integer.parseInt(cursor.getString(0)));
                point.setGmttimestamp(cursor.getString(1));
                point.setLatitude(cursor.getDouble(1));
                point.setLongitude(cursor.getDouble(2));
                point.setAltitude(cursor.getDouble(3));
                point.setAccuracy(cursor.getDouble(4));
                point.setSpeed(cursor.getDouble(5));
                point.setBearing(cursor.getDouble(6));
                point.setPuntoEnviado(cursor.getInt(7) == 1);
                
                // Añadir el punto a la lista
                pointList.add(point);
            } while (cursor.moveToNext());
        }
 
        // Cerramos la base de datos
        if (db.isOpen()) {
			db.close();
		}
        // retornar la lista de puntos
        return pointList;
    }
 
    // Actualizar un punto
    public int updateDbPoint(DbPointsManager point) {
    	int ret = 0;
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_GMTTIMESTAMP, point.getGmttimestamp());
        values.put(KEY_LATITUDE, point.getLatitude());
        values.put(KEY_LONGITUDE, point.getLongitude());
        values.put(KEY_ALTITUDE, point.getAltitude());
        values.put(KEY_ACCURACY, point.getAccuracy());
        values.put(KEY_SPEED, point.getSpeed());
        values.put(KEY_BEARING, point.getBearing());
        values.put(KEY_PUNTOENVIADO, point.getPuntoEnviado()? 1 : 0);
        
        // Actalizar registro
        ret = db.update(TABLE_POINTS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(point.getID()) });
        
        // Cerramos la base de datos
        if (db.isOpen()) {
			db.close();
		}
        return ret;
    }
 
    // Borrar un punto
    public void deleteDbPoint(DbPointsManager point) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_POINTS, KEY_ID + " = ?",
                new String[] { String.valueOf(point.getID()) });
        // Cerramos la base de datos
        if (db.isOpen()) {
			db.close();
		}
    }
    
    // Borrar todos los puntos
    public int deleteDbPointAll() {
    	int ret = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        
        ret = db.delete(TABLE_POINTS, null, null);
        
        // Cerramos la base de datos
        if (db.isOpen()) {
			db.close();
		}
        
        return ret;
    }
 
 
    // Obtener el número de puntos que hay en la tabla
    public int getDbPointsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_POINTS;
        int ret = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
         
        // Devolver el count de la tabla
        ret = cursor.getCount();
        cursor.close();
        
        // Cerramos la base de datos
        if (db.isOpen()) {
			db.close();
		}
        
        return ret;
    }
    
    /**
     * doDbExport
     * Exporta el contenido de la base de datos en un fichero txt
     *
     * @return: número de registros exportados
     */
    public int doDbExport(String userName) {
    	int ret = 0;
    	StringBuffer fileBuf = new StringBuffer();
    	SQLiteDatabase db = this.getReadableDatabase();
    	Cursor cursor = null;
    	String myQuery = "", fileName = "";
    	
        try {
        	
        	// Vamos a leer todos los registros de la tabla y creando el Buffer
        	myQuery = "SELECT * FROM "+TABLE_POINTS+" ORDER BY "+KEY_GMTTIMESTAMP+" ASC";
        	cursor = db.rawQuery(myQuery, null);
        	
        	if (cursor.moveToFirst()) {
        		String gmtTimestamp = null;
        		String beginTimestamp = null;
        		
        		// Vamos a ir leyendo
        		do {        	        
        	        gmtTimestamp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GMTTIMESTAMP));
					if (beginTimestamp == null) {
						beginTimestamp = gmtTimestamp;
					}
					double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE));
					double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE));
					double altitude = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_ALTITUDE));
							// TODO + this.getAltitudeCorrectionMeters();
					double accuracy = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_ACCURACY));
					double speed = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_SPEED));
					int punto_enviado = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PUNTOENVIADO));
					
					// Añadir al buffer
					Locale locale = Locale.US;
					fileBuf.append( 
					 (char)34 + OpenGTSutils.zuluFormat(gmtTimestamp) + (char)34 + ", " +
					 (char)34 + latitude + (char)34 + ", " +
					 (char)34 + longitude + (char)34 + ", " +
					 (char)34 + altitude + (char)34 + ", " +
					 (char)34 + accuracy + (char)34 + ", " +
					 (char)34 + speed + (char)34 + ", " +
					 (char)34 + punto_enviado + (char)34 + "\n");
					
					// Sumo un registro
					ret++;
					
        		} while (cursor.moveToNext());
        		
        		// Pasamos el buffer a un string para escribir en el fichero
				String fileContents = fileBuf.toString();
				//Log.d(tag, fileContents);
				
				// Escribimos en el archivo
				// Componer el nombre del fichero, que será: nombre-de-usuario_fecha-del-dia-en-curso.txt
		    	fileName = userName+"_"+OpenGTSutils.fechaActual()+".txt";
		    	String dirPath = Environment.getExternalStorageDirectory().toString()
						+ "/_GPS" ;
		    	
		    	Log.d(tag, "DB_EXPORT -> Dir: "+dirPath+" Filename: "+fileName);
		    	
				File sdDir = new File(dirPath);
				
				if(sdDir.mkdirs()) {
					Log.d(tag,"OK Dir: "+dirPath);
				} else {
					Log.d(tag,"FAIL Dir: "+dirPath);
				}
				File file = new File(dirPath+"/" + fileName);
				if(file.exists()) {
					Log.d(tag, "OK file EXIST");
				} else {
					// Si el fichero no existe
					fileContents = 
	        				(char)34 + "fecha-hora" + (char)34 + "," +
	        				(char)34 + "latitud" + (char)34 + "," +
	        				(char)34 + "longitud" + (char)34 + "," +
							(char)34 + "altitud" + (char)34 + "," +
							(char)34 + "precision" + (char)34 + "," +
							(char)34 + "velocidad" + (char)34 + "," +
							(char)34 + "rumbo" + (char)34 + "," +
							(char)34 + "punto-enviado" + (char)34 + "\n" +
							fileContents;
					
					Log.d(tag,"file NOT EXIST, NEW file");
				}

				FileWriter sdWriter = new FileWriter(file, true);
				Log.d(tag,"new FileWriter");
				sdWriter.write(fileContents);
				Log.d(tag, "write fileContents");
				sdWriter.close();
				Log.d(tag,"close file");
				
				
				
				/*
				 try {
				 	FileOutputStream fOut = new FileOutputStream(file, true);
			        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			        myOutWriter.append(fileContents);
			        myOutWriter.close();
			        fOut.close();
			        Log.d(tag,"fOut.CLOSE");
			        
					} catch (Exception e) {
					  e.printStackTrace();
					  ret = 0;
					}
				*/
				
				
		        // Ahora tenemos que borrar la base de datos
		        db.close();
		        
		        
        	} else {
        		// No había ningún registro para escribir
        	}
        	
        } catch (Exception e) {
              Log.e("ERRR", "Could not create file",e);
              ret = 0;
        }
        return ret;
    }
}
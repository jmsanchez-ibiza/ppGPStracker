package com.jmsg.android.ppgpstracker;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.location.Location;

public class OpenGTSutils {
	/***
	 * JMSG 31/08/2013, 10:29:45 Funciones para crear la cadena de datos GPRMC
	 */
	/**
	 * Encode a location as GPRMC string data.
	 * <p/>
	 * For details check org.opengts.util.Nmea0183#_parse_GPRMC(String) (OpenGTS
	 * source)
	 * 
	 * @param loc
	 *            location
	 * @return GPRMC data
	 */
	public static final String GPRMCEncode(Location loc) {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
		DecimalFormat f = new DecimalFormat("0.000000", dfs);

		String gprmc = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,,",
				"$GPRMC", NMEAGPRMCTime(new Date(loc.getTime())), "A",
				NMEAGPRMCCoord(Math.abs(loc.getLatitude())),
				(loc.getLatitude() >= 0) ? "N" : "S",
				NMEAGPRMCCoord(Math.abs(loc.getLongitude())),
				(loc.getLongitude() >= 0) ? "E" : "W",
				f.format(MetersPerSecondToKnots(loc.getSpeed())),
				f.format(loc.getBearing()),
				NMEAGPRMCDate(new Date(loc.getTime())));

		gprmc += "*" + NMEACheckSum(gprmc);

		return gprmc;
	}

	public static String NMEAGPRMCTime(Date dateToFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat("HHmmss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(dateToFormat);
	}

	public static String NMEAGPRMCDate(Date dateToFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(dateToFormat);
	}

	public static String NMEAGPRMCCoord(double coord) {
		int degrees = (int) coord;
		double minutes = (coord - degrees) * 60;

		DecimalFormat df = new DecimalFormat("00.00000",
				new DecimalFormatSymbols(Locale.US));
		StringBuilder rCoord = new StringBuilder();
		rCoord.append(degrees);
		rCoord.append(df.format(minutes));

		return rCoord.toString();
	}

	public static String NMEACheckSum(String msg) {
		int chk = 0;
		for (int i = 1; i < msg.length(); i++) {
			chk ^= msg.charAt(i);
		}
		String chk_s = Integer.toHexString(chk).toUpperCase();
		while (chk_s.length() < 2) {
			chk_s = "0" + chk_s;
		}
		return chk_s;
	}

	/**
	 * Converts given meters/second to nautical mile/hour.
	 * 
	 * @param mps
	 *            meters per second
	 * @return knots
	 */
	public static double MetersPerSecondToKnots(double mps) {
		// Google "meters per second to knots"
		return mps * 1.94384449;
	}

	/**
	 * Convierte un string con formato de fecha proveniente del locationManager
	 * ej: 20081215135500 en 2008-12-15 13:55:00
	 * 
	 * @param beginTimestamp
	 * @return StringBuffer con la fecha
	 */
	public static String zuluFormat(String beginTimestamp) {
		// turn 20081215135500 into 2008-12-15 13:55:00
		StringBuffer buf = new StringBuffer(beginTimestamp);
		buf.insert(4, '-');
		buf.insert(7, '-');
		buf.insert(10, ' ');
		buf.insert(13, ':');
		buf.insert(16, ':');
		// buf.append('Z');
		return buf.toString();
	}
	
	/**
	 * Devuelve la fecha actual con el formato yyyy-mm-dd
	 * 
	 * @return
	 */
	public static String fechaActual() {
		Date fActual = new Date();
		return String.format("%04d", fActual.getYear()+1900)
			   + "-" +
			   String.format("%02d", fActual.getMonth()+1)
			   + "-" +
			   String.format("%02d", fActual.getDate());
		
	}

}

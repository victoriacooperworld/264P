import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/******************************************************************************************************************
* File:MiddleFilter.java
* Project: Lab 1
* Copyright:
*   Copyright (c) 2020 University of California, Irvine
*   Copyright (c) 2003 Carnegie Mellon University
* Versions:
*   1.1 January 2020 - Revision for SWE 264P: Distributed Software Architecture, Winter 2020, UC Irvine.
*   1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
* Parameters: None
* Internal Methods: None
******************************************************************************************************************/

public class MiddleFilter extends FilterFramework
{
	public static double last_altitude1=-1;
	public static double last_altitude2=-1;
	public static boolean wildjump;

	List<String> jump =new ArrayList<>();
;	public void run()
    {
		int bytesread = 0;					// Number of bytes read from the input file.
		int byteswritten = 0;				// Number of bytes written to the stream.
		byte databyte = 0;					// The byte of data read from the file
		int id;
		long measurement;

		// Next we write a message to the terminal to let the world know we are alive...
		System.out.print( "\n" + this.getName() + "::Middle Reading ");
		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss");
		double velocity = 0.0;
		double altitude = 0.0;
		double pressure = 0.0;
		double temperature = 0.0;
		double oldAltitude = 0.0;
		while (true)
		{
			// Here we read a byte and write a byte
			try {

				//check the id
				id = 0;
				for (int i = 0; i < 4; i++) {
					databyte = ReadFilterInputPort();    // This is where we read the byte from the stream...
					id = id | (databyte & 0xFF);        // We append the byte on to ID...
					if (i != 4 - 1)                // If this is not the last byte, then slide the
					{                                    // previously appended byte to the left by one byte
						id = id << 8;                    // to make room for the next byte we append to the ID
					}
					bytesread++;                        // Increment the byte count
					WriteFilterOutputPort(databyte);
				}

				measurement = 0;
				for (int i=0; i<8; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...
					if (i != 8-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
						// measurement
					}
					bytesread++;									// Increment the byte count
					if(id!=2){
						WriteFilterOutputPort(databyte);
					}
				}

				if ( id == 0 )
				{
					TimeStamp.setTimeInMillis(measurement);
				}
				else if ( id == 1 )
				{
					velocity=Double.longBitsToDouble(measurement);
				}
				else if ( id == 2 )
				{
					altitude=Double.longBitsToDouble(measurement);
					oldAltitude=altitude;
					if ((last_altitude1== -1 && last_altitude2==-1)){   //the first case
						wildjump=false;
					}else{
						if (Math.abs(last_altitude1-altitude)>100){
							wildjump=true;

						}
					}
					if (wildjump){
						if (last_altitude2==-1){
							altitude=last_altitude1;
						}else{
							altitude=(last_altitude1+last_altitude2)/2;   //push back the average
						}
					}
					//push the data to the next filter
					ByteBuffer b = ByteBuffer.allocate(Long.BYTES);
					b.putLong(Double.doubleToLongBits(altitude));
					byte[] newaltibyte = b.array();
					for (int i =0; i<8;i++){			//write back the data
						databyte = newaltibyte[i];
						WriteFilterOutputPort(databyte);
					}
					if(wildjump){
						WriteFilterOutputPort((byte)1);		//write the byte back
					}else {
						WriteFilterOutputPort((byte)0);
					}

					last_altitude2=last_altitude1;
					last_altitude1=altitude;


				}
				else if ( id == 3 )
				{
					pressure=Double.longBitsToDouble(measurement);
				}
				else if ( id == 4 ) {
					temperature = Double.longBitsToDouble(measurement);
					if(wildjump){
						jump.add(outputCSV(TimeStampFormat.format(TimeStamp.getTime()), velocity, oldAltitude, pressure, temperature));  //write to csv file
						wildjump=false;
					}

				}
			}
			catch (EndOfStreamException e)
			{
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;
			}
		}
		writeCSV(jump);
   }
	private String outputCSV(String time, double velocity, double altitude, double pressure, double temperature){
		String data = "";
		data=time+','+String.format("%3.5f", velocity)+','+String.format("%3.5f", altitude)+','+String.format("%3.5f", pressure)+','+String.format("%3.5f", temperature)+'\n';
		return data;
	}

	public static void writeCSV(List<String> line) {
		try (PrintWriter writer = new PrintWriter("Wildpoints.csv")) {
			writer.write("Time,Velocity,Altitude,Pressure,Temperature\n");
			for(String d : line){
				writer.write(d);
			}
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}

	}
}
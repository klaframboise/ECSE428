package ca.mcgill.ecse428.a2;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import ca.canadapost.cpcdp.rating.generated.messages.Messages;
import ca.canadapost.cpcdp.rating.generated.rating.*;
import ca.canadapost.cpcdp.rating.generated.rating.MailingScenario.ParcelCharacteristics;
import ca.canadapost.cpcdp.rating.generated.rating.MailingScenario.Destination;
import ca.canadapost.cpcdp.rating.generated.rating.MailingScenario.Destination.Domestic;
import ca.canadapost.cpcdp.rating.generated.rating.MailingScenario.ParcelCharacteristics.Dimensions;

public class RateCalculator {

	public static final String REGULAR = "Regular Parcel";
	public static final String XPRESS = "Xpresspost";
	public static final String PRIORITY = "Priority";
	public static final int MAX_GIRTH_PLUS_LENGTH = 300;
	public static final int MAX_LENGTH = 200;
	public static final int MAX_WEIGHT = 30;
	public static final float MIN_MEASUREMENT = 0.1f;
	
	private APIConnection api;

	public RateCalculator() {
		api = new APIConnection();
	}

	/**
	 * Gets the postal rate for a parcel with the given characteristics at the given service level.
	 * @param from origin postal code, case and whitespace insensitive
	 * @param to destination postal code, case and whitespace insensitive
	 * @param length measure of the longest side, in cm
	 * @param width width in cm
	 * @param height height in cm
	 * @param weight weight in kg
	 * @param mode one of {@link #REGULAR}, {@link #XPRESS}, or {@link #PRIORITY}
	 * @return the rate, float
	 * @throws IllegalArgumentException if any argument is invalid
	 */
	public float getRate(String from, String to, float length, float width, float height, float weight, String mode) throws IllegalArgumentException {

		/* Create mailing scenario */
		MailingScenario scenario = new MailingScenario();
		scenario.setQuoteType("counter");

		/* Create parcel dimensions */
		Dimensions dim = new Dimensions();
		dim.setLength(new BigDecimal(length));
		dim.setWidth(new BigDecimal(width));
		dim.setHeight(new BigDecimal(height));

		/* Assign parcel characteristics */
		ParcelCharacteristics charac = new ParcelCharacteristics();
		charac.setDimensions(dim);
		charac.setWeight(new BigDecimal(weight));
		scenario.setParcelCharacteristics(charac);

		/* Set origin and destination */
		scenario.setOriginPostalCode(from.replaceAll(" ", "").toUpperCase());
		Domestic domestic = new Domestic();
		domestic.setPostalCode(to.replaceAll(" ", "").toUpperCase());
		Destination destination = new Destination();
		destination.setDomestic(domestic);
		scenario.setDestination(destination);

		/* Execute rate request */
		ClientResponse resp = api.createMailingScenario(scenario);
		InputStream respIS = resp.getEntityInputStream();

		System.out.println("HTTP Response Status: " + resp.getStatus() + " " + resp.getStatusInfo());

		/* Parse XML */
		JAXBContext jc;
		Object entity = null;
		try {
			jc = JAXBContext.newInstance(PriceQuotes.class, Messages.class);
			entity = jc.createUnmarshaller().unmarshal(respIS);
		} catch (JAXBException e) {
			e.printStackTrace();
			api.close();
			return -1;
		}

		/* Find corresponding price for service */
		if (entity instanceof PriceQuotes) {	//check if request was succesful
			PriceQuotes priceQuotes = (PriceQuotes) entity;
			for (Iterator<PriceQuotes.PriceQuote> iter = priceQuotes.getPriceQuotes().iterator(); iter.hasNext();) { 
				PriceQuotes.PriceQuote aPriceQuote = (PriceQuotes.PriceQuote) iter.next();                	
				if(aPriceQuote.getServiceName().equals(mode)) {
					return aPriceQuote.getPriceDetails().getDue().floatValue();
				}
			}
		} else if (entity != null) {
			/* Assume Error Schema */
			Messages messageData = (Messages) entity;
			for (Iterator<Messages.Message> iter = messageData.getMessage().iterator(); iter.hasNext();) {
				Messages.Message aMessage = (Messages.Message) iter.next();
				api.close();
				throw (IllegalArgumentException)parseErrorMessage(aMessage.getCode(), aMessage.getDescription(), from, to, length, width, height, weight, mode);
			}
		}
		
		return -1;
	}

	/**
	 * Parses the server error message and returns an IllegalArgumentException with a more
	 * meaningful message
	 * @param code error code from server
	 * @param msg error message
	 * @param from
	 * @param to
	 * @param length
	 * @param width
	 * @param height
	 * @param weight
	 * @param mode
	 * @return IllegalArgumentException if the exception is known to have been caused by an illegal argument
	 * or Exception otherwise
	 */
	private Exception parseErrorMessage(String code, String msg, String from, String to, float length, float width, float height, float weight, String mode) {
		System.out.println("Received msg: " + code + "::" + msg);
		if(msg.contains("origin-postal-code")) 
			return new IllegalArgumentException("Origin postal code is invalid");
		else if(msg.matches(".*(?<!origin-)postal-code.*")) 
			return new IllegalArgumentException("Destination postal code is invalid");
		else if(msg.contains("DimensionMeasurementType")) 
			return new IllegalArgumentException(String.format("Value %.3f is invalid. Minimum allowed: %.3f", Float.parseFloat(msg.split("'")[1]), MIN_MEASUREMENT));
		else if(msg.equals("No services are appropriate for the shipment of the defined parcel.  Validate the parcel criteria against product specifications.")) 
			return diagnoseMessage(from, to, length, width, height, weight, mode);
		else 
			return new Exception(code + "::" + msg);
	}

	/**
	 * Use when server returns "No service appropriate" message.
	 * @param from
	 * @param to
	 * @param length
	 * @param width
	 * @param height
	 * @param weight
	 * @param mode
	 * @return IllegalArgumentException if the exception is known to have been caused by an illegal argument
	 * or Exception otherwise
	 */
	private Exception diagnoseMessage(String from, String to, float length, float width, float height, float weight, String mode) {
		
		/* Check girth + length */
		if(length + 2*width + 2*height > MAX_GIRTH_PLUS_LENGTH) return new IllegalArgumentException("Girth + length exceeds maximum allowed (" + MAX_GIRTH_PLUS_LENGTH + " cm)");
		else if(length > MAX_LENGTH) return new IllegalArgumentException("Length exceeds maximum allowed (" + MAX_LENGTH + " cm)");
		else if(weight > MAX_WEIGHT) return new IllegalArgumentException("Weight exceeds maximum allowed (" + MAX_WEIGHT + " kg)");
		return new Exception("Unexpected server exception");
	}

}

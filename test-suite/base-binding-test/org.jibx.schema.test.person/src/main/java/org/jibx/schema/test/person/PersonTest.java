package org.jibx.schema.test.person;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.jibx.sampleschema.person.Person;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.jibx.schema.codegen.*;

/**
 * Unit test for simple App.
 */
public class PersonTest 
    implements  BundleActivator
{
    public final static String STRING_ENCODING = "UTF8";
    public final static String URL_ENCODING = "UTF-8";

    public static final void main(String[] args)
    {
    	PersonTest test = new PersonTest();
    	test.start(null);
    }
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PersonTest()
    {
    }
    /**
     * Test it!
     */
    public void start(BundleContext context)
    {
	    System.out.println("starting");
    	Object message = createMessage();
    	String xml = marshalObject(message);
	    System.out.println(xml);
	    
	    message = unmarshalMessage(xml);
	    this.checkMessage(message);
    }
    public void stop(BundleContext context)
    {
    	
    }
    /**
     * Create a default ping message.
     * @return
     */
    public Object createMessage()
    {
    	Person person = new Person();
    	person.setFirstName("Don");
    	person.setLastName("Corley");
        return person;
    }
    public static final int FLIGHT_NO = 5;
    /**
     * Print the payload of this ping message.
     * @param message
     */
    public void checkMessage(Object message)
    {
    	Person person = (Person)message;
        if (( "Don".equals(person.getFirstName())) &&
        		( "Corley".equals(person.getLastName())))
        	System.out.println ("Test ran great!");
        else
        	System.out.println ("Test failed");
    }
    /**
     * Marshal this message to xml .
     * @param message
     * @param system
     * @return
     */
//    static String PACKAGE_NAME = "org.jibx.schema.person";
    static String BINDING_NAME = "binding";
    public String marshalObject(Object message)
    {
		try {
//			IBindingFactory jc = BindingDirectory.getFactory(BINDING_NAME, PACKAGE_NAME);	// OSGi does not like this
			IBindingFactory jc = BindingDirectory.getFactory(Person.class);
			IMarshallingContext marshaller = jc.createMarshallingContext();
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
			marshaller.marshalDocument(message, URL_ENCODING, null, out);
			String xml = out.toString(STRING_ENCODING);
			return xml;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JiBXException e) {
			e.printStackTrace();
		}
		return null;
    }
    /**
     * Unmarshal this xml Message to an object.
     * @param xml
     * @param system
     * @return
     */
    public Object unmarshalMessage(String xml)
    {        
		try {
//			IBindingFactory jc = BindingDirectory.getFactory(BINDING_NAME, PACKAGE_NAME);
			IBindingFactory jc = BindingDirectory.getFactory(Person.class);
			IUnmarshallingContext unmarshaller = jc.createUnmarshallingContext();
	        Reader inStream = new StringReader(xml);
			Object message = unmarshaller.unmarshalDocument( inStream, BINDING_NAME);
			return message;
		} catch (JiBXException e) {
			e.printStackTrace();
		}
    	return null;
    }
}

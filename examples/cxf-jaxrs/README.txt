/*
  This example was copied from the cxf-jaxrs example. Thanks Apache!
  
  See that example in the servicemix distribution (apache-servicemix-4.4.1/examples/cxf/cxf-jaxrs)
  for more details.
 */

Purpose
-------
Create a RESTful JAX-RS web service using CXF and expose it with the 
OSGi HTTP Service.

Running the Example
-------------------

1. Start ServiceMix by running the following command:

  <servicemix_home>/bin/servicemix          (on UNIX)
  <servicemix_home>\bin\servicemix          (on Windows)
  


Running a Client
----------------

You can browse WSDL at:

http://localhost:8181/cxf/crm/customerservice?_wadl&_type=xml

or

http://localhost:8181/cxf/crm?_wadl&_type=xml

The latter URI can be used to see the desription of multiple root
resource classes.

You can see the services listing at http://localhost:8181/cxf.

You can make invocations on the web service in several ways, including
using a web client, using a Java client and using a command-line
utility such a curl or Wget. See below for more details.

(a) To run a web client:
    -------------------
Open a browser and go to the following URL:

   http://localhost:8181/cxf/crm/customerservice/customers/123

It should display an XML representation for customer 123.

Note, if you use Safari, right click the window and select 'Show Source'.

(b) To run a command-line utility:
    -----------------------------
You can use a command-line utility, such as curl or Wget, to make 
the invocations. For example, try using curl as follows:

- Open a command prompt and change directory to
  <servicemix_home>/examples/cxf-jaxrs.
  
- Run the following curl commands:

  # Create a customer
  #
  #
  curl -X POST -T src/main/resources/org/apache/servicemix/examples/cxf/jaxrs/client/add_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers

  # Retrieve the customer instance with id 123
  #
  curl http://localhost:8181/cxf/crm/customerservice/customers/123
 
  # Update the customer instance with id 123
  #
  curl -X PUT -T src/main/resources/org/apache/servicemix/examples/cxf/jaxrs/client/update_customer.xml -H "Content-Type: text/xml" http://localhost:8181/cxf/crm/customerservice/customers
  
  # Delete the customer instance with id 123
  #
  curl -X DELETE http://localhost:8181/cxf/crm/customerservice/customers/123


Changing /cxf servlet alias
---------------------------
By default CXF Servlet is assigned a '/cxf' alias. You can
change it in a couple of ways

a. Add org.apache.cxf.osgi.cfg to the /etc directory and set the
   'org.apache.cxf.servlet.context' property, for example:
   
   org.apache.cxf.servlet.context=/custom

b. Use shell config commands, for example:

     config:edit org.apache.cxf.osgi   
     config:propset org.apache.cxf.servlet.context /super
     config:update

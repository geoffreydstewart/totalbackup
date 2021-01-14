package org.gds.totalbackup;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import java.net.ServerSocket;
import java.net.Socket;

/** Tests ports for various scenarios
 *
 *  Sets the properties: portInUse, portAvailable
 *  Expects the property: port_number
 *
 * org.gds.totalbackup.CaPortTester
 */
public class CaPortTester extends Task
{

	protected String _portNumber;
	protected String _hostName;
	protected String _connectionType;

    public void setPortNumber(String portNumber)
    {
    	_portNumber = portNumber;
    }

    public void setHostName(String hostName)
    {
    	_hostName = hostName;
	}

	public void setConnectionType(String connectionType)
	{
		_connectionType = connectionType;
	}

    protected void verifyPortNumberSet() throws BuildException
    {
		if( _portNumber==null )
			throw new BuildException("You must specify a port number");
	}

    protected void verifyHostNameSet() throws BuildException
    {
		if( _hostName==null )
			throw new BuildException("You must specify a host name");
	}

	protected void verifyConnectionType() throws BuildException
	{
		if ((_connectionType==null) || ((!_connectionType.equals("client")) && (!_connectionType.equals("server"))))
			throw new BuildException("You must specify a connection type of either 'client' or 'server'");
	}

    protected void testServerSocket() throws BuildException
    {
		ServerSocket serverSock;

		log("Creating a server connection socket on port: " + _portNumber);

		try
    	{
      		// Connection socket creation
      		serverSock = new ServerSocket (Integer.parseInt(_portNumber));
    	}
    	catch (Exception excep)
    	{
    		log("Unable to create server connection socket");

    		throw new BuildException("The port requested is already in use");
    	}
    	try
    	{
      		serverSock.close();
    	}
    	catch (Exception excep)
    	{

			throw new BuildException("Unable to close connection socket");
    	}
    	log("Port '" + _portNumber + "' available for use by Server. Test successful") ;
    	return;
	}

    protected void testClientSocket() throws BuildException
    {
		Socket sock;

		log("Creating a client connection socket on host: " + _hostName + " using port: " + _portNumber);

		try
    	{
      		// Connection socket creation
      		sock = new Socket (_hostName, Integer.parseInt(_portNumber));
    	}
    	catch (Exception excep)
    	{
    		log("Unable to create client connection socket");

      		throw new BuildException("There was no server listening on " + _hostName + " using port: " + _portNumber);
    	}
    	try
    	{
      		sock.close();
    	}
    	catch (Exception excep)
    	{
    		log("Unable to close connection socket");

      		throw new BuildException("Unable to close connection socket");
    	}
    	log("Port '" + _portNumber + "' available for client connections. Test successful") ;
    	return;
	}

    public void execute() throws BuildException
    {
		verifyPortNumberSet();
		verifyHostNameSet();
		verifyConnectionType();

        if (_connectionType.equals("client"))
        {
			testClientSocket();
		}
		else
		{
			testServerSocket();
		}

		//getProject().setProperty("server.port.available", server_Port_Available);
		//getProject().setProperty("client.port.available", client_Port_Available);
    }
}


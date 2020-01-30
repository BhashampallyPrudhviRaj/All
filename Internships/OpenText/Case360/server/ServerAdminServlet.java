package com.eistream.sonora.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eistream.sonora.cachemanager.CacheConstants;
import com.eistream.sonora.cachemanager.CacheSend;
import com.eistream.sonora.csrfguard.CSRFGuardTokenResolver;
import com.eistream.sonora.exceptions.SonoraApplicationException;
import com.eistream.sonora.exceptions.SonoraException;
import com.eistream.sonora.layout.ThemeResolver;
import com.eistream.sonora.security.SecurityCache;
import com.eistream.sonora.users.UserElement;
import com.eistream.sonora.util.Base64UrlSafe;
import com.eistream.sonora.util.ContextAdapter;
import com.eistream.sonora.util.CurrentUser;
import com.eistream.sonora.util.HtmlUtil;
import com.eistream.sonora.util.StringUtil;
import com.eistream.sonora.util.UtilRequest;
import com.eistream.utilities.StringVar;
import com.eistream.utilities.StringVarResolverHashMap;
import com.eistream.utilities.TimestampUtil;
import com.eistream.utilities.calendar.CalendarFactory;
import com.eistream.utilities.calendar.ISimpleDateFormat;

public class ServerAdminServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	  
    String externalName = "ServerAdminServlet";
    StringVarResolverHashMap ex = new StringVarResolverHashMap();
    StringVar sv = new StringVar(ex);

    private final int    tDisp              = 1;
    private final int    tDelete            = 2;
    private final int    tConfigurationProperties = 4;
    private final int    tConfigurationCreateUserProp = 5;
    private final int    tConfigurationUpdateUserProp = 6;
    private final int    tConfigurationDeleteUserProp = 7;
    private final int    tUpdate = 8;
    

    public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
  	}

	/**
	 */
	public void destroy()
	{
		super.destroy();
	}

	public String getServletInfo()
	{
		return "Server Administration application";
	}

	/**
	 * This is the standard HttpServlet doGet method called when someone does a get on this servlet.
	 * <hr>
	 * The following paramaters are used: <br>
	 * Paramater    Description<br>
	 * op           the operation to perform.<br>
	 * <br>
	 * <hr>
	 * The following operations are supported:<br>
	 * op	[optional] (required)<br>
	 * <hr>
	 * p	display the License property page.<br>
	 * <br>
	 *
	 * @param req HttpServletRequest
	 * @param resp HttpServletResponse
	 * @exception ServletException
	 * @exception java.io.IOException
	 * @exception javax.servlet.ServletException
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
     
		String cancel = UtilRequest.getParameter(req, "cancel");
		if (cancel != null)
			return;

		// indicate what transaction was being performed when an error occurred
	    // i.e., displaying servers, deleting a server etc.
	    int pendingTransaction = 0;
	    
		// Create the HTML output Writer

		String charEncoding = null;

		// Create the HTML output Writer
		resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
        charEncoding = UtilRequest.getCharEncoding();

		ByteArrayOutputStream serverBytes = new ByteArrayOutputStream(4096);
	    PrintWriter  out = new PrintWriter(new OutputStreamWriter(serverBytes, charEncoding), true);

		// Disable cache
		resp.setHeader("Pragma", "no-cache");
		resp.setHeader("Expires", "-1");

		// HTML code from Resource Bundle
   
		// Find command line parameters
		String szOperation = UtilRequest.getParameter(req, "op");        // Operation
        if (szOperation == null || szOperation.length() == 0)
            szOperation = "g";

		// Process Command Line arguments
		try
		{

            switch(szOperation.charAt(0))
            {
    	        case 'g':
    	            pendingTransaction = tDisp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                        SecurityCache.SYSTEM_ACL_NAME))
                        serverProperties(req, out);
                    else
                    {
                        ResourceBundle myResources= ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
                        out.println(myResources.getString("SERVER_BLANK"));
                    }
                   	break;

                case 'd':
                    pendingTransaction = tDelete;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                        SecurityCache.SYSTEM_ACL_NAME))
                    {

                        serverDelete(req, out);
                        serverProperties(req, out);
                    }
                    else
                    {
                        ResourceBundle myResources= ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
                        out.println(myResources.getString("SERVER_BLANK"));
                    }
                    break;
            
                case 'p':
                    pendingTransaction = tConfigurationProperties;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                        SecurityCache.SYSTEM_ACL_NAME))
                    {
                        String category = UtilRequest.getStringParameter(req, "category", null, UtilRequest.DEFAULT_SanityCheck);   // category
                        if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                        {
                            ocrConfigurationProperties(req, out, category);
                        }
                        else if(category.indexOf(SystemApplicationProperty.LOG_CATEGORY) != -1)
                        {
                            logConfigurationProperties(req, out, category);
                        }
                        else if(category.indexOf(SystemApplicationProperty.BPME_CATEGORY) != -1)
                        {
                            bpmeConfigurationProperties(req, out, category);
                        }
                        else
                        	serverConfigurationProperties(req, out, category);
                    }
                    break;

                case 'c':
                    pendingTransaction = tConfigurationCreateUserProp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                    SecurityCache.SYSTEM_ACL_NAME))
                    {
                        String category = UtilRequest.getParameter(req, "category");   // category
                        if (category.indexOf(SystemApplicationProperty.CUSTOM_CATEGORY) != -1)
                        	createUserProperty(req, out);
                        else if (category.indexOf(SystemApplicationProperty.CAPTURE_CATEGORY) != -1)
                          	createCaptureClassProperty(req, out);
                        else if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                          	createOcrConnectionPool(req, out);
                        else if (category.indexOf(SystemApplicationProperty.BPME_CATEGORY) != -1)
                            createOrUpdateBPMEServer(req, out, true);
                    }
                    break;

                case 'o':
                    pendingTransaction = tConfigurationCreateUserProp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                    SecurityCache.SYSTEM_ACL_NAME))
                    {
                        String category = UtilRequest.getParameter(req, "category");   // category
                        String itemName = UtilRequest.getParameter(req, "name");                    	
                        if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                            ocrConnectionPoolProperties(req, out, itemName);
                        else if (category.indexOf(SystemApplicationProperty.BPME_CATEGORY) != -1)
                            createOrUpdateBPMEServer(req, out, false);
                    }
                    break;
  	
                case 'x':
                    pendingTransaction = tConfigurationDeleteUserProp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                    SecurityCache.SYSTEM_ACL_NAME))
                    {
                        String category = UtilRequest.getParameter(req, "category");   // category
                        String propName = UtilRequest.getParameter(req, "propName");                        
                        if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                        {
                          	deleteOcrPool(req, out, propName);
                        }
                        else if (category.indexOf(SystemApplicationProperty.BPME_CATEGORY) != -1)
                        {
                            deleteBPMEServer(req, out, propName);
                        }
                        else

                        {
                        	deleteProperty(propName,category);
                        	serverConfigurationProperties(req, out, category);
                        }
                    }
                    break;

                // delete a connection from a ocr connection pool    
                case 'y':
                	 pendingTransaction = tConfigurationDeleteUserProp;
                	 if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                         SecurityCache.SYSTEM_ACL_NAME))
                	 {
                		 String propName = UtilRequest.getParameter(req, "propName");
                		 String host = UtilRequest.getParameter(req, "host");   // category
                		 String port = UtilRequest.getParameter(req, "port");   // category
                		 deleteOcrPoolConnection(req, out, propName, host, port);
                	 }
                	 break;
                
                case 'u':
                    pendingTransaction = tUpdate;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                        SecurityCache.SYSTEM_ACL_NAME))
                    {

                        serverUpdate(req, out);
                    }
                    else
                    {
                        ResourceBundle myResources= ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
                        out.println(myResources.getString("SERVER_BLANK"));
                    }
                    break;

                case 'a':
                    pendingTransaction =  tConfigurationUpdateUserProp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                    SecurityCache.SYSTEM_ACL_NAME))
                    {
                    	String propName = UtilRequest.getParameter(req, "name");
                    	String displayName = UtilRequest.getParameter(req, "displayName");
                        logCategoryProperties(req, out, propName, displayName);
                                
                    }
                    break;
  	
                case 'r':
                    pendingTransaction =  tConfigurationUpdateUserProp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                    SecurityCache.SYSTEM_ACL_NAME))
                    {
                    	String propName = UtilRequest.getParameter(req, "name");
                    	String displayName = UtilRequest.getParameter(req, "displayName");
                        logUserCategoryProperties(req, out, propName, displayName);
                                
                    }
                    break;
  	    
                    
                case 't':
                    pendingTransaction = tConfigurationCreateUserProp;
                    if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                    SecurityCache.SYSTEM_ACL_NAME))
                    {
                        logCreateCategory(req, out);
                                
                    }
                    break;
  	  	
                case 'b':
               	 	pendingTransaction = tConfigurationDeleteUserProp;
               	 	if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                                     SecurityCache.SYSTEM_ACL_NAME))
               	 	{
               	 		String propName = UtilRequest.getParameter(req, "name");
               	 		logDeleteCategory(req, out, propName);
               	 	}
            	 break;
   
                default:
                {
		            // Get HTML "Unknown Command" Resource
                    ResourceBundle myResources= ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
                   	if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
                   		out.println(myResources.getString("UNKNOWN_COMMAND_DISPLAY"));
   		            break;
   		        }
            }
		}

		catch (Exception error)
		{
	        doError(req, error, out, pendingTransaction);
        }

 	    finally
	    {
	        if (serverBytes.size() != 0)
	        {
	            resp.setContentLength(serverBytes.size() + CSRFGuardTokenResolver.getScriptTagLength());
	            serverBytes.writeTo(resp.getOutputStream());
	        }

	        //out.close();
	    }
	}

	// Handle the Post Request
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String cancel = UtilRequest.getParameter(req, "cancel");
		if (cancel != null)
			return;

		// indicate what transaction was being performed when an error occurred
        // i.e., displaying servers, deleting a server etc.
        int pendingTransaction = 0;

		// Create the HTML output Writer

	    String charEncoding = null;

  	    resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
        charEncoding = UtilRequest.getCharEncoding();

		ByteArrayOutputStream licenseBytes = new ByteArrayOutputStream(4096);
	    PrintWriter  out = new PrintWriter(new OutputStreamWriter(licenseBytes, charEncoding), true);

	    // Find command line parameters
        String szOperation = UtilRequest.getParameter(req, "op");    // Operation

        // Process Command Line arguments
        try
        {
            // Make sure "op" exists, then go to specified operation
            if (szOperation != null)
            {
                switch(szOperation.charAt(0))
                {

                    case 'p':
                        pendingTransaction =  tConfigurationUpdateUserProp;
                        if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req),
                            SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                            SecurityCache.SYSTEM_ACL_NAME))
                        {
                            String category = UtilRequest.getParameter(req, "category");   // category
                            updateServerAppProperties(req, out, category);
                            serverConfigurationProperties(req, out, category);

                        }
                        break;

                    case 'c':
                        pendingTransaction = tConfigurationCreateUserProp;
                        if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req),
                            SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                            SecurityCache.SYSTEM_ACL_NAME))
                        {
                            String category = UtilRequest.getParameter(req, "category"); // category
                            if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                            {
                            	createOcrConnectionPropertyPost(req, out, category);
                            	break;
                            } else if (SystemApplicationProperty.BPME_CATEGORY.equalsIgnoreCase(category))
                            {
                                createOrUpdateBPMEServerPost(req, out, true);
                                break;
                            }

                            if (category.indexOf(SystemApplicationProperty.CUSTOM_CATEGORY) != -1)
                            	createUserPropertyPost(req, out, category);
                            if (category.indexOf(SystemApplicationProperty.CAPTURE_CATEGORY) != -1)
                            	createCaptureClassPropertyPost(req, out, category);
                            serverConfigurationProperties(req, out, category);

                        }
                        break;

                    case 'o':
                    	pendingTransaction = tConfigurationCreateUserProp;
                    	if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req),
                                SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                SecurityCache.SYSTEM_ACL_NAME))
                    	{
                            String category = UtilRequest.getParameter(req, "category"); // category
                            String itemName = UtilRequest.getParameter(req, "name");
                            if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                                updateOcrConnectionsPost(req, out, itemName);
                            else if (category.indexOf(SystemApplicationProperty.BPME_CATEGORY) != -1)
                                createOrUpdateBPMEServerPost(req, out, false);
                    	}
                    	break;
                        
                    case 'u':
                        pendingTransaction = tDelete;
                        if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                            SecurityCache.SYSTEM_ACL_NAME))
                        {

                            serverUpdatePost(req, out);
                            serverProperties(req, out);
                        }
                        else
                        {
                            ResourceBundle myResources= ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
                            out.println(myResources.getString("SERVER_BLANK"));
                        }
                        break;
                        
                    case 'l':
                    	logUpdateGeneralProperties(req, out);
                    	break;
                    	
                    case 'm':
                    	pendingTransaction = tConfigurationUpdateUserProp;
                    	logUpdateCategory(req, out);
                    	break;
                    	
                    case 'n':
                    	pendingTransaction = tConfigurationProperties;
                    	logConfigurationProperties(req, out, "Log");
                    	break;
                    	
                    case 'q':
                        pendingTransaction = tConfigurationCreateUserProp;
                        if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req),
                            SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                            SecurityCache.SYSTEM_ACL_NAME))
                        {
                        	logCreateCategoryPost(req, out);
                        }
                        break;
	
                    case 'x':
                    	// cancel operation from a post
                        pendingTransaction = tConfigurationProperties;
                        if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
                                            SecurityCache.SYSTEM_ACL_NAME))
                        {
                            String category = UtilRequest.getParameter(req, "category");   // category
                            if (category.indexOf(SystemApplicationProperty.OCR_CATEGORY) != -1)
                            {
                                ocrConfigurationProperties(req, out, category);
                            }
                            else if(category.indexOf(SystemApplicationProperty.LOG_CATEGORY) != -1)
                            {
                                logConfigurationProperties(req, out, category);
                            }
                            else if(category.indexOf(SystemApplicationProperty.BPME_CATEGORY) != -1)
                            {
                                bpmeConfigurationProperties(req, out, category);
                            } else 
                                serverConfigurationProperties(req, out, category);
                        }
                        break;

                    
                    default:
                    {
                       	if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
                       	{
                       		ResourceBundle myResources = ResourceBundle.getBundle(
                            "com.eistream.sonora.licensemanager.LicenseAdminServletResource",
                            UserElement.getUserLocale(req));
                       		out.println(myResources.getString("UNKNOWN_COMMAND_DISPLAY"));
                       	}
                        break;
                    }
   		        }
            }

            // Otherwise report a usage error
            else
            {
   		    }
        }

	    catch (Exception error)
		{
            doError(req, error, out, pendingTransaction);

        }

	    finally
	    {
	        if (licenseBytes.size() != 0)
	        {
	            resp.setContentLength(licenseBytes.size() + CSRFGuardTokenResolver.getScriptTagLength());
	            licenseBytes.writeTo(resp.getOutputStream());
	        }

	        //out.close();
	    }
	}

    public void serverDelete(HttpServletRequest req, PrintWriter out) throws Exception
    {
        try
        {
            String hostName = null;
            byte[] hostNameB = Base64UrlSafe.decode(UtilRequest.getParameter(req,"hostName"));
            if (hostNameB != null)
            {
                hostName = new String(hostNameB, "UTF-8");
                SystemSessionEJB systemSession = ((SystemSessionEJBHome) ContextAdapter.getHome(ContextAdapter.SYSTEMSESSION)).create();
                systemSession.deleteServer(hostName);
            }
        }
        catch (Exception e)
        {
            SonoraApplicationException sex = new SonoraApplicationException(
                SonoraException.EXPTN_INVALID_LICENSE, null);
            throw sex;
        }

    }

    public void serverUpdate(HttpServletRequest req, PrintWriter out) throws Exception
    {
        try
        {
            String hostName = null;
            String hostNameI = UtilRequest.getStringParameter(req,"hostName", null, UtilRequest.DEFAULT_SanityCheck);
            byte[] hostNameB = Base64UrlSafe.decode(UtilRequest.getParameter(req,"hostName"));
            if (hostNameB != null)
            {
                hostName = new String(hostNameB, "UTF-8");
            }
            String capacity = UtilRequest.getStringParameter(req,"capacity", null, UtilRequest.DEFAULT_SanityCheck);
            ResourceBundle myResources = ResourceBundle.getBundle(
                    "com.eistream.sonora.system.ServerAdminServletResource",
                    UserElement.getUserLocale(req));

            // Expression support
            StringVarResolverHashMap svrh = new StringVarResolverHashMap();
            StringVar svServer = new StringVar(svrh);

            svrh.setVariable("HOSTNAME", hostName);
            svrh.setVariable("CAPACITY", capacity);
            svrh.setVariable("HOSTNAMEI", hostNameI);
                
            out.println(svServer.resolveNames(myResources.getString(
            "SERVER_UPDATE")));

        }
        catch (Exception e)
        {
            SonoraApplicationException sex = new SonoraApplicationException(
                SonoraException.EXPTN_INVALID_LICENSE, null);
            throw sex;
        }

    }

    public void serverUpdatePost(HttpServletRequest req, PrintWriter out) throws Exception
    {
        try
        {
            String hostName = null;
            byte[] hostNameB = Base64UrlSafe.decode(UtilRequest.getParameter(req,"hostName"));
            if (hostNameB != null)
            {
                hostName = new String(hostNameB, "UTF-8");
            }
            String capacity = UtilRequest.getParameter(req,"capacity");
            Integer serverCapacity = null;
            if (capacity == null || capacity.length() == 0)
            {
            	serverCapacity = new Integer(0);
            }
            else
            {
            	try
            	{
            		serverCapacity = new Integer(capacity);
            	}
            	catch(Exception e)
            	{
            		return;
            	}
            }
                  	
            ServerDAO server = new ServerDAO();
            server.updateCapacity(hostName, serverCapacity.intValue());
            
        }
        catch (Exception e)
        {
            SonoraApplicationException sex = new SonoraApplicationException(
                SonoraException.EXPTN_INVALID_LICENSE, null);
            throw sex;
        }

    }

    public void serverProperties(HttpServletRequest req, PrintWriter out) throws Exception
    {
        ResourceBundle myResources= ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
        String RESERVED_CLUSTER_NAME = "*CLUSTER*";

        // Expression support
        StringVarResolverHashMap svrh = new StringVarResolverHashMap();
        StringVar svServer = new StringVar(svrh);

        try
        {
            SystemSessionEJB systemSession = ((SystemSessionEJBHome) ContextAdapter.getHome(ContextAdapter.SYSTEMSESSION)).create();
            ServerTO[] servers = systemSession.getServerList();

            String hostName = null;

            hostName = ContextAdapter.getHostandPort();

            out.println(svServer.resolveNames(myResources.getString("SERVER_PROPERTIES_START")));

            if (servers != null)
            {
                for (int i = 0; i < servers.length; i++)
                {
                    svrh.setVariable("DELETE", "");
                    // skip the cluster
                    if (!RESERVED_CLUSTER_NAME.equals(servers[i].getHostName()))
                    {
                        String encodedHostName = Base64UrlSafe.encodeBytes(servers[i].getHostName().getBytes("UTF-8"));
                        svrh.setVariable("HOSTNAMEI", encodedHostName);
                 
                        // if the server is not this server allow deletion
                        if (hostName != null)
                        {
                            if (!hostName.equals(servers[i].getHostName()))
                            {
                                String deleteURL = svServer.resolveNames(myResources.getString("SERVER_DELETE_URL"));
                                svrh.setVariable("DELETE", deleteURL);
                            }
                        }
                    
                        svrh.setVariable("HOSTNAME", servers[i].getHostName());
                        svrh.setVariable("IPADDRESS", servers[i].getIpAddress());

                        if (servers[i].getServerActivity() != null)
                        {
                            String dateExpires = null;
                            ISimpleDateFormat formatDate = CalendarFactory.getISimpleDateFormatInstance(TimestampUtil.
                                getFormatSpecFromPattern(TimestampUtil.DATETIME_FORMAT,
                                        UserElement.getUserLocale(req), true), UserElement.getUserLocale(req));
                            java.util.Date sDate = new java.util.Date(servers[i].getServerActivity().longValue());
                            dateExpires = formatDate.format(sDate);
                            svrh.setVariable("SERVERACTIVITY", dateExpires);
                        }
                        else
                            svrh.setVariable("SERVERACTIVITY", "");

                        svrh.setVariable("WFLOADFACTOR", Integer.toString(servers[i].getWorkflowLoadFactor()));
                        svrh.setVariable("WFACTIVE", Boolean.valueOf(servers[i].getWorkflowActiveState()));
                        svrh.setVariable("S_VERSIONLABEL",  servers[i].getVersion()==null ? myResources.getString("SERVER_PROPERTIES_UNKNOWN_VERSION") : servers[i].getVersion());
                        
                        svrh.setVariable("TABINDEX", String.valueOf(i));
                                      
                        out.println(svServer.resolveNames(myResources.getString("SERVER_PROPERTIES")));
                    }

                }
            }
            out.println(svServer.resolveNames(myResources.getString("SERVER_PROPERTIES_END")));

        }
        catch (Exception e)
        {
            throw e;
        }

    }
  
    public void serverConfigurationProperties(HttpServletRequest req, PrintWriter out, String category)
        throws Exception
    {
        ResourceBundle myResources = ResourceBundle.getBundle(
            "com.eistream.sonora.system.ServerAdminServletResource",
            UserElement.getUserLocale(req));

        // Expression support
        StringVarResolverHashMap svrh = new StringVarResolverHashMap();
        StringVar svServer = new StringVar(svrh);


        try
        {
            // Search for the category
            int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES, category);

            if (index < 0)
                throw new Exception("invalid category " + category);

            svrh.setVariable("CATEGORY", category);

            if (category.equals(SystemApplicationProperty.ALERTS_CATEGORY) == true)
            	svrh.setVariable("categorytitle", myResources.getString("alerts_TEXT"));
            else if (category.equals(SystemApplicationProperty.CACHE_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("cache_TEXT"));
            else if (category.equals(SystemApplicationProperty.CAPTURE_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("capture_TEXT"));
            else if (category.equals(SystemApplicationProperty.CUSTOM_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("custom_TEXT"));
            else if (category.equals(SystemApplicationProperty.FILESTORE_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("filestore_TEXT"));
            else if (category.equals(SystemApplicationProperty.HISTORY_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("history_TEXT"));
            else if (category.equals(SystemApplicationProperty.IMPORT_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("import_TEXT"));
            else if (category.equals(SystemApplicationProperty.LOG_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("log_TEXT"));
            else if (category.equals(SystemApplicationProperty.OCR_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("ocr_TEXT"));
            else if (category.equals(SystemApplicationProperty.PROCESS_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("process_TEXT"));
            else if (category.equals(SystemApplicationProperty.QUERY_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("query_TEXT"));
            else if (category.equals(SystemApplicationProperty.SYSTEM_CATEGORY) == true)
              	svrh.setVariable("categorytitle", myResources.getString("general_TEXT"));
            else if (category.equals(SystemApplicationProperty.BPME_CATEGORY) == true)
                svrh.setVariable("categorytitle", myResources.getString("bpme_TEXT"));
              
            
            boolean userCategory = false;
            boolean captureCategory = false;
          
            if (category.indexOf(SystemApplicationProperty.CUSTOM_CATEGORY) != -1)
            {
                userCategory = true;
                svrh.setVariable("CREATE_PROP", myResources.getString("SYSTEM_CONFIGURATION_CREATE_USER_PROP"));
            }
            else if (category.indexOf(SystemApplicationProperty.CAPTURE_CATEGORY) != -1)
            {
            	captureCategory = true;
                svrh.setVariable("CREATE_PROP", myResources.getString("SYSTEM_CONFIGURATION_CREATE_CAPTURE_PROP"));
            }
            else
                svrh.setVariable("CREATE_PROP", "");

            svrh.setVariable("CONFIGURATION_ACTIONS_MENU", svServer.resolveNames(myResources.getString("CONFIGURATION_ACTIONS_MENU")));
            svrh.setVariable("SUBMIT", "");
           
            out.println(svServer.resolveNames(myResources.getString(
                "SYSTEM_APP_PROPERTIES_START")));

            SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

            ArrayList appProps = appProperties.findByCategory(category);
            StringBuffer label = null;
            
            if (appProps != null && appProps.size() > 0)
            {
            	svrh.setVariable("SUBMIT", myResources.getString("SYSTEM_CONFIGURATION_PROP_SUBMIT"));

            	String[] currentThemeNames = ThemeResolver.getCurrentThemeNames();
            	
            	if (currentThemeNames == null)
            	    currentThemeNames = ThemeResolver.DEFAULT_THEMES;
            	
            	svrh.setVariable("THEME_LIST", Arrays.toString(currentThemeNames).replaceAll("[\\[\\]]", ""));
            	
                for (int i = 0; i < appProps.size(); i++)
                {
                	SystemApplicationProperty appProp = (SystemApplicationProperty)appProps.get(i);

                	// BZ 61507.  Just hide this deprecated property.  The current value is ALLOW_DUPLICATE_SCRIPTNAME
                	if (appProp.getPropName().equals("CHECK_DUPLICATE_SCRIPTNAMES"))
                	    continue;
                	
                	// check for hidden capture properties
                	if (captureCategory)
                	{
                		if (SystemApplicationProperty.HIDDENPROPERTIES.indexOf(appProp.getPropName()) != -1)
                			continue;
                	}
                
                	if (appProp.getPropValue() == null)
                        svrh.setVariable("VALUE", "");
                    else
                        svrh.setVariable("VALUE", HtmlUtil.toSafeHtml(appProp.getPropValue()));

                    svrh.setVariable("NAME", appProp.getPropName());
                 
                    svrh.setVariable("PROPNAME", appProp.getPropName());
                    
                    if (userCategory)
                    {
                  		String deletePropURL = svServer.resolveNames(myResources.getString("SYSTEM_CONFIGURATION_DELETE_USER_PROP"));
                		svrh.setVariable("DELETE_USER_PROP", deletePropURL);
              
                        svrh.setVariable("TOOLTIP", appProp.getPropToolTip());
                        if (appProp.getPropLabel() == null || appProp.getPropLabel().length() == 0)
                            svrh.setVariable("LABEL", appProp.getPropName());
                        else             	
                        	svrh.setVariable("LABEL", appProp.getPropLabel());
                    }
                    else
                    {
              
                        // only delete user defined props or user defined capture class props
                        if (captureCategory)
                        {
                        	if (appProp.getReadOnly()== false)
                        	{
                        		String deletePropURL = svServer.resolveNames(myResources.getString("SYSTEM_CONFIGURATION_DELETE_CAPTURE_CLASS_PROP"));
                        		svrh.setVariable("DELETE_USER_PROP", deletePropURL);
                        	}
                        	else
                                svrh.setVariable("DELETE_USER_PROP", "");
                        	// capture property exceptions
                        	if(appProp.getPropName().equals("CAPTURE_IGNORE_BLANK_SETS"))
                                svrh.setVariable("DELETE_USER_PROP", "");
                          	if(appProp.getPropName().equals("COUNT_SUBMITTED_FILES"))
                                svrh.setVariable("DELETE_USER_PROP", "");
                          	if(appProp.getPropName().equals("SOLE_CAPTURE_SERVER"))
                                svrh.setVariable("DELETE_USER_PROP", "");
                        }
                     	else
                            svrh.setVariable("DELETE_USER_PROP", "");
               
                    	String toolTip = "";
                        label = new StringBuffer();
                        
                        if (SystemApplicationProperty.RESTART_PROPERTIES.indexOf(appProp.getPropName()) != -1)
                        {
                        	label.append(myResources.getString("PROP_RESTART") + " ");
                        }
                        try
                        {
                        	toolTip = myResources.getString(appProp.getPropName() + "_TOOLTIP");
                        	toolTip = svServer.resolveNames(toolTip);
                        }
                        catch(Exception e)
                        {
                        	toolTip="";
                        }
                        try
                        {
                        	label.append(myResources.getString(appProp.getPropName() + "_LABEL"));
                        }
                        catch(Exception e)
                        {
                        	//label=null;
                           	label.append(appProp.getPropName());
                        }
                     	svrh.setVariable("TOOLTIP", toolTip);
                     	
                     	if(label!=null)
                     	{
                    		svrh.setVariable("LABEL", label.toString());
                     	}
                     	else
                     	{
                    		svrh.setVariable("LABEL", "");
                     	}                                                 
                    }
                    if (appProp.getReadOnly()== true)
                    	out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_READONLY_PROPERTY")));
                    else if("BOS_WS_PASSWORD".equals(appProp.getPropName()))
                    	out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PASSWORD_PROPERTY")));
                    else if("BOS_SERVICE_WS_PASSWORD".equals(appProp.getPropName()))
                    	out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PASSWORD_PROPERTY")));
                    else if("SMTP_PASSWORD".equals(appProp.getPropName()))
                    {
                    	if(appProp.getPropValue() != null && appProp.getPropValue().length() > 0)
                    		svrh.setVariable("VALUE", "{{PASSWORDPROTECTED}}");
                    	out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PASSWORD_PROPERTY")));
                    }
                    else
                   		out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PROPERTY")));
                         	
                }
                out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PROPERTIES_END")));
        
            }
            else
            {
            	String noProp = "";
            	
            	if (category.equals(SystemApplicationProperty.CUSTOM_CATEGORY) == true)
            	{
            		noProp = myResources.getString("SYSTEM_APP_NO_CUSTOM_PROPS");
            	}
             	svrh.setVariable("NOPROP", noProp);
                
            	out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PROPERTY_NONE")));
                out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_NO_PROPERTIES_END")));
            }
        }
        catch (Exception e)
        {
            throw e;
        }

    }
    
    public void ocrConfigurationProperties(HttpServletRequest req, PrintWriter out, String category)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	
	    try
	    {
	        // Search for the category
	        int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES, category);
	
	        if (index < 0)
	            throw new Exception("invalid category" + category);
	
	        svrh.setVariable("CATEGORY", category);
	
	        svrh.setVariable("categorytitle", myResources.getString("ocr_TEXT"));
	        svrh.setVariable("CREATE_PROP", myResources.getString("SYSTEM_CONFIGURATION_CREATE_OCR_POOL"));
            svrh.setVariable("CONFIGURATION_ACTIONS_MENU", svServer.resolveNames(myResources.getString("CONFIGURATION_ACTIONS_MENU")));
	     
	        svrh.setVariable("SUBMIT",
	                             myResources.getString("SYSTEM_CONFIGURATION_PROP_SUBMIT"));
	        
	        out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_OCR_CONNECTION_POOLS_START")));
	
	        OnValidateOcrConnectionPool ocr = new OnValidateOcrConnectionPool();
	        ArrayList poolNames = ocr.getConnectionPools();
	        
	        if (poolNames != null && poolNames.size() > 0)
	        {
	        	for (int i = 0; i < poolNames.size(); i++)
	        	{
	        		String poolName = (String)poolNames.get(i);
	        		svrh.setVariable("NAME", poolName);
	        		SystemApplicationProperty displayName = ocr.getDisplayName(poolName);
	        		String deleteURL = svServer.resolveNames(myResources.getString("SYSTEM_DELETE_OCR_CONNECTION_POOL"));
	        		svrh.setVariable("SYSTEM_DELETE_OCR_CONNECTION_POOL", deleteURL);
	        		if (displayName != null && displayName.getPropValue() != null && displayName.getPropValue().length() > 0)
	        			svrh.setVariable("DISPLAYNAME", displayName.getPropValue());
	        		else
	        			svrh.setVariable("DISPLAYNAME", poolName);
	        		out.println(svServer.resolveNames(myResources.getString("SYSTEM_OCR_CONNECTION_POOL_NAME")));
 	         	
	        	
	        	}
	        }
	        else
	        {
	        	String noProp = "";
            	
	        	noProp = myResources.getString("SYSTEM_APP_NO_OCR_PROPS");
             	svrh.setVariable("NOPROP", noProp);
                
            	out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PROPERTY_NONE")));
   
	        }
	        
	        out.println(svServer.resolveNames(myResources.getString(
	           "SYSTEM_OCR_CONNECTION_POOLS_END")));
	
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
	    

    public void logConfigurationProperties(HttpServletRequest req, PrintWriter out, String category)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	
	    try
	    {
	        // Search for the category
	        int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES, category);
	
	        if (index < 0)
	            throw new Exception("invalid category" + category);
	
	        svrh.setVariable("CATEGORY", category);
	
	        svrh.setVariable("categorytitle", myResources.getString("log_TEXT"));
            svrh.setVariable("CREATE_PROP", myResources.getString("SYSTEM_LOG_CREATE_CATEGORY_ACTION"));
            svrh.setVariable("CONFIGURATION_ACTIONS_MENU", svServer.resolveNames(myResources.getString("CONFIGURATION_ACTIONS_MENU")));

	        SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

            SystemApplicationProperty prop = appProperties.find("APP_LOG_DIRECTORY");
            svrh.setVariable("APP_LOG_DIRECTORY_PROPNAME", prop.getPropName());
            svrh.setVariable("APP_LOG_DIRECTORY_PROPVALUE", prop.getPropValue());
        	String toolTip = "";
            try
            {
            	toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
            }
            catch(Exception e)
            {
            }
         	svrh.setVariable("APP_LOG_DIRECTORY_TOOLTIP", toolTip);
     
         	prop = appProperties.find("VERBOSELOGGING");
         	svrh.setVariable("VERBOSELOGGING_PROPNAME", prop.getPropName());
         	svrh.setVariable("VERBOSELOGGING_PROPVALUE", prop.getPropValue());
         	toolTip = "";
         	try
         	{
         		toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
         	}
         	catch(Exception e)
         	{
         	}
          	svrh.setVariable("VERBOSELOGGING_TOOLTIP", toolTip);
                  	
          	prop = appProperties.find("FORCEMETHODNAMES");
         	svrh.setVariable("FORCEMETHODNAMES_PROPNAME", prop.getPropName());
         	svrh.setVariable("FORCEMETHODNAMES_PROPVALUE", prop.getPropValue());
         	toolTip = "";
         	try
         	{
         		toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
         	}
         	catch(Exception e)
         	{
         	}
          	svrh.setVariable("FORCEMETHODNAMES_TOOLTIP", toolTip);
     
        	prop = appProperties.find("LOGBEANEXCEPTIONS");
         	svrh.setVariable("LOGBEANEXCEPTIONS_PROPNAME", prop.getPropName());
         	svrh.setVariable("LOGBEANEXCEPTIONS_PROPVALUE", prop.getPropValue());
         	toolTip = "";
         	try
         	{
         		toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
         	}
         	catch(Exception e)
         	{
         	}
          	svrh.setVariable("LOGBEANEXCEPTIONS_TOOLTIP", toolTip);
     
          	prop = appProperties.find("LOGRAWMESSAGES");
         	svrh.setVariable("LOGRAWMESSAGES_PROPNAME", prop.getPropName());
         	svrh.setVariable("LOGRAWMESSAGES_PROPVALUE", prop.getPropValue());
         	toolTip = "";
         	try
         	{
         		toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
         	}
         	catch(Exception e)
         	{
         	}
          	svrh.setVariable("LOGRAWMESSAGES_TOOLTIP", toolTip);
     
          	prop = appProperties.find("LOGFULLSTACK");
         	svrh.setVariable("LOGFULLSTACK_PROPNAME", prop.getPropName());
         	svrh.setVariable("LOGFULLSTACK_PROPVALUE", prop.getPropValue());
         	toolTip = "";
         	try
         	{
         		toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
         	}
         	catch(Exception e)
         	{
         	}
          	svrh.setVariable("LOGFULLSTACK_TOOLTIP", toolTip);
     
         	prop = appProperties.find("LOGFORMATTER");
         	svrh.setVariable("LOGFORMATTER_PROPNAME", prop.getPropName());
         	svrh.setVariable("LOGFORMATTER_PROPVALUE", prop.getPropValue());
         	toolTip = "";
         	try
         	{
         		toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
         	}
         	catch(Exception e)
         	{
         	}
          	svrh.setVariable("LOGFORMATTER_TOOLTIP", toolTip);

          	prop = appProperties.find("LICENSE_LOG_FILE");
            svrh.setVariable("LICENSE_LOG_FILE_PROPNAME", prop.getPropName());
            if (prop.getPropValue() == null)
            	svrh.setVariable("LICENSE_LOG_FILE_PROPVALUE", "");
            else
            	svrh.setVariable("LICENSE_LOG_FILE_PROPVALUE", prop.getPropValue());
            toolTip = "";
            try
            {
                toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
            }
            catch(Exception e)
            {
            }
            svrh.setVariable("LICENSE_LOG_FILE_TOOLTIP", toolTip);

            prop = appProperties.find("LOGSECURITYFAILURES");
            svrh.setVariable("LOGSECURITYFAILURES_PROPNAME", prop.getPropName());
            svrh.setVariable("LOGSECURITYFAILURES_PROPVALUE", prop.getPropValue());
            toolTip = "";
            try
            {
                toolTip = myResources.getString(prop.getPropName() + "_TOOLTIP");
            }
            catch(Exception e)
            {
            }
            svrh.setVariable("LOGSECURITYFAILURES_TOOLTIP", toolTip);
         	out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_LOG_PROPERTIES_START")));
	
         	ArrayList catNames = appProperties.findLikeProp("Log", "%_CATNAME");
        	ArrayList logLevels = appProperties.findLikeProp("Log", "%_LOGLEVEL");
            	
         	if (catNames != null)
         	{
         		for (int i=0; i < catNames.size(); i++)
         		{
         			SystemApplicationProperty catName = (SystemApplicationProperty)catNames.get(i);
         			if (i < logLevels.size())
         			{
         				SystemApplicationProperty logLevel = (SystemApplicationProperty)logLevels.get(i);
         				svrh.setVariable("LOGLEVEL", convertLogLevel(logLevel.getPropValue(),myResources));
         			}
         			else
         				svrh.setVariable("LOGLEVEL", "");
             		svrh.setVariable("NAME", catName.getPropName());
         			svrh.setVariable("DISPLAYNAME", catName.getPropValue());
         			if (catName.getReadOnly())
         			{
         				svrh.setVariable("SYSTEM_DELETE_LOG_CATEGORY", "");
         		   	    out.println(svServer.resolveNames(myResources.getString("SYSTEM_LOG_CATEGORY_NAME")));
         			}
         			else
         			{
         	            String deleteURL = svServer.resolveNames(myResources.getString("SYSTEM_DELETE_LOG_CATEGORY"));
                        svrh.setVariable("SYSTEM_DELETE_LOG_CATEGORY", deleteURL);
                  	    out.println(svServer.resolveNames(myResources.getString("SYSTEM_USER_LOG_CATEGORY_NAME")));
         			}
         		}
         	}
            out.println(svServer.resolveNames(myResources.getString(
	           "SYSTEM_LOG_PROPERTIES_END")));
	
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
    
    public void bpmeConfigurationProperties(HttpServletRequest req, PrintWriter out, String category) throws Exception {
        ResourceBundle myResources = ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource",
                UserElement.getUserLocale(req));

        // Expression support
        StringVarResolverHashMap svrh = new StringVarResolverHashMap();
        StringVar svServer = new StringVar(svrh);

        try {
            // Search for the category
            int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES, category);

            if (index < 0)
                throw new Exception("invalid category" + category);

            svrh.setVariable("CATEGORY", category);

            svrh.setVariable("categorytitle", myResources.getString("bpme_TEXT"));
            svrh.setVariable("CREATE_PROP", myResources.getString("SYSTEM_CONFIGURATION_CREATE_BPME_SERVER"));
            svrh.setVariable("CONFIGURATION_ACTIONS_MENU",
                    svServer.resolveNames(myResources.getString("CONFIGURATION_ACTIONS_MENU")));

            svrh.setVariable("SUBMIT", myResources.getString("SYSTEM_CONFIGURATION_PROP_SUBMIT"));

            out.println(svServer.resolveNames(myResources.getString("SYSTEM_BPME_SERVERS_START")));

            OnValidateBPMEServer bpmeConfig = new OnValidateBPMEServer();
            List<String> idList = bpmeConfig.getBPMEServerIDs();

            if (!idList.isEmpty()) {
                for (String serverId : idList) {
                    svrh.setVariable("ID", serverId);
                    svrh.setVariable("DISPLAY_NAME", bpmeConfig.getDisplayName(serverId));
                    String deleteURL = svServer.resolveNames(myResources.getString("SYSTEM_DELETE_BPME_SERVER"));
                    svrh.setVariable("SYSTEM_DELETE_BPME_SERVER", deleteURL);

                    out.println(svServer.resolveNames(myResources.getString("SYSTEM_BPME_SERVER_NAME")));
                }
            } else {
                String noProp = myResources.getString("SYSTEM_APP_NO_BPME_SERVERS");
                svrh.setVariable("NOPROP", noProp);

                out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_PROPERTY_NONE")));
            }

            out.println(svServer.resolveNames(myResources.getString("SYSTEM_BPME_SERVERS_END")));

        } catch (Exception e) {
            throw e;
        }
    }
    
    public void logCategoryProperties(HttpServletRequest req, PrintWriter out, String propName, String displayName)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	
	    try
	    {
	       
	      
	        SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

	        svrh.setVariable("NAME", propName);
            svrh.setVariable("DISPLAYNAME", displayName);
          
            // get the category
            SystemApplicationProperty catProp = appProperties.find(propName);
            
            // convert the dotted categories name to the internal saved name which uses a hyphen
            // property names should not contain .
            displayName = displayName.replace('.', '-');
            SystemApplicationProperty prop = appProperties.find(displayName + "_LOGLEVEL");
            
            svrh.setVariable("SEVERESELECTED", "");
            svrh.setVariable("WARNINGSELECTED", "");
            svrh.setVariable("INFOSELECTED", "");
            svrh.setVariable("CONFIGSELECTED", "");
            
    		svrh.setVariable("Disabled", "");
            
            // dont allow the level to be changed on the root levels unless it is Sonora
            if (catProp.getReadOnly() == true)
            {
            	if (displayName.equals("Sonora") == true)
            		svrh.setVariable("Disabled", "");
            	else if(displayName.equals("Jal") == true)
              		svrh.setVariable("Disabled", "");
                else if(displayName.equals("JalStackTrace") == true)
                    svrh.setVariable("Disabled", "");
            	else if(displayName.equals("Utilities") == true)
              		svrh.setVariable("Disabled", "");
             	else
            		svrh.setVariable("Disabled", "Disabled");
            }
             	
            int level = new Integer(prop.getPropValue()).intValue();
            if (level == Level.SEVERE.intValue())
            	 svrh.setVariable("SEVERESELECTED", "SELECTED");
            else if (level == Level.WARNING.intValue())
            	 svrh.setVariable("WARNINGSELECTED", "SELECTED");
            else if (level == Level.INFO.intValue())
             	 svrh.setVariable("INFOSELECTED", "SELECTED");
            else if (level == Level.CONFIG.intValue())
             	 svrh.setVariable("CONFIGSELECTED", "SELECTED");
            
            
            prop = appProperties.find(displayName + "_MAXSIZE");
            svrh.setVariable("MAXSIZEVALUE", prop.getPropValue());
            svrh.setVariable("MAXSIZEPROPNAME", "");
            
        	prop = appProperties.find("APP_LOG_DIRECTORY");
        	svrh.setVariable("APP_LOG_DIRECTORY", prop.getPropValue());
         	
        	prop = appProperties.find(displayName + "_LOGNAME");
        	svrh.setVariable("LOGNAMEVALUE", prop.getPropValue());
        	svrh.setVariable("LOGNAMEPROPNAME", prop.getPropName());
        
          	prop = appProperties.find(displayName + "_NUMFILES");
          	svrh.setVariable("NUMFILESVALUE", prop.getPropValue());
          	svrh.setVariable("NUMFILESPROPNAME", prop.getPropName());
         	
         	out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_LOG_CATEGORY_PROPERTIES")));
	
         
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
    
    public void logUserCategoryProperties(HttpServletRequest req, PrintWriter out, String propName, String displayName)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	
	    try
	    {
	       
	      
	        SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

	        svrh.setVariable("NAME", propName);
            svrh.setVariable("DISPLAYNAME", displayName);
            
            // convert the dotted categories name to the internal saved name which uses a hyphen
            // property names should not contain .
            displayName = displayName.replace('.', '-');
            SystemApplicationProperty prop = appProperties.find(displayName + "_LOGLEVEL");
            
            svrh.setVariable("SEVERESELECTED", "");
            svrh.setVariable("WARNINGSELECTED", "");
            svrh.setVariable("INFOSELECTED", "");
            svrh.setVariable("CONFIGSELECTED", "");
            
    		svrh.setVariable("Disabled", "");
             	
            int level = new Integer(prop.getPropValue()).intValue();
            if (level == Level.SEVERE.intValue())
            	 svrh.setVariable("SEVERESELECTED", "SELECTED");
            else if (level == Level.WARNING.intValue())
            	 svrh.setVariable("WARNINGSELECTED", "SELECTED");
            else if (level == Level.INFO.intValue())
             	 svrh.setVariable("INFOSELECTED", "SELECTED");
            else if (level == Level.CONFIG.intValue())
             	 svrh.setVariable("CONFIGSELECTED", "SELECTED");
            
            
         	
         	out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_USER_LOG_CATEGORY_PROPERTIES")));
	
         
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}


    public void logUpdateGeneralProperties(HttpServletRequest req, PrintWriter out)
    throws Exception
	{
	   
	    try
	    {
	    	
	        String propValue = UtilRequest.getParameter(req, "APP_LOG_DIRECTORY");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("APP_LOG_DIRECTORY", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
            SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();
            SystemApplicationProperty prop = appProperties.find("APP_LOG_DIRECTORY");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());

            propValue = UtilRequest.getParameter(req, "VERBOSELOGGING");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("VERBOSELOGGING", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("VERBOSELOGGING");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());
          
            propValue = UtilRequest.getParameter(req, "FORCEMETHODNAMES");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("FORCEMETHODNAMES", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("FORCEMETHODNAMES");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());

            propValue = UtilRequest.getParameter(req, "LOGBEANEXCEPTIONS");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("LOGBEANEXCEPTIONS", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("LOGBEANEXCEPTIONS");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());

            propValue = UtilRequest.getParameter(req, "LOGRAWMESSAGES");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("LOGRAWMESSAGES", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("LOGRAWMESSAGES");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());

            propValue = UtilRequest.getParameter(req, "LOGFULLSTACK");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("LOGFULLSTACK", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("LOGFULLSTACK");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());

            propValue = UtilRequest.getParameter(req, "LOGFORMATTER");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("LOGFORMATTER", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("LOGFORMATTER");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());
         
            propValue = UtilRequest.getParameter(req, "LICENSE_LOG_FILE");
	        try
	        {
	        	OnValidateLog vLog = new OnValidateLog();
	        	vLog.onChange("LICENSE_LOG_FILE", propValue);
	        }
	        catch (SonoraException se)
	        {
	        	throw se;
	        }
	        appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("LICENSE_LOG_FILE");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());
            
            propValue = UtilRequest.getParameter(req, "LOGSECURITYFAILURES");
            try
            {
                OnValidateLog vLog = new OnValidateLog();
                vLog.onChange("LOGSECURITYFAILURES", propValue);
            }
            catch (SonoraException se)
            {
                throw se;
            }
            appProperties = new SystemApplicationPropertyDAO();
            prop = appProperties.find("LOGSECURITYFAILURES");
            prop.setPropValue(propValue);
            appProperties.update(prop.getPropName(), prop.getPropValue());

            CacheSend flushcache = new CacheSend();
     
            // flush app props
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);

            // dynamically configure logs on all servers
            flushcache.sendCommand(CacheConstants.LOGCONFIGCACHE);
            
            logConfigurationProperties(req, out, "Log");
            
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
    
    public void logUpdateCategory(HttpServletRequest req, PrintWriter out)
    throws Exception
	{
	   
	    try
	    {
	    	
	        //String propName = UtilRequest.getParameter(req, "name");
	        
	        String displayName = UtilRequest.getParameter(req, "displayName");
	        
	        // replace dotted prop name with hyphen the internal prop name
	        displayName = displayName.replace('.', '-');
	        
	        SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

	        String level = UtilRequest.getParameter(req, "LOGLEVEL");
		     
	        SystemApplicationProperty prop = null;
	        if (level != null)
	        {
	        	prop = appProperties.find(displayName + "_LOGLEVEL");
                        
	        	prop.setPropValue(level);
            
	        	appProperties.update(prop.getPropName(), prop.getPropValue());
	        }
            String logName = UtilRequest.getParameter(req, "LOGNAME");
		       
            if (logName != null)
            {
            	prop = appProperties.find(displayName + "_LOGNAME");
                        
            	prop.setPropValue(logName);
            
            	appProperties.update(prop.getPropName(), prop.getPropValue());
            }
            String maxSize = UtilRequest.getParameter(req, "MAXSIZE");
		       
            if (maxSize != null)
            {
            	try
            	{
            		new Integer(maxSize);
            	}
            	catch (Exception e)
            	{
                    SonoraException ex = new SonoraException(SonoraException.INVALID_LOG_MAXSIZE, e.getMessage());
                    throw ex;
            	}
            	prop = appProperties.find(displayName + "_MAXSIZE");
                        
            	prop.setPropValue(maxSize);
            
            	appProperties.update(prop.getPropName(), prop.getPropValue());
            }
            
            String numFiles = UtilRequest.getParameter(req, "NUMFILES");
		    
            if (numFiles != null)
            {
            	try
            	{
            		new Integer(numFiles);
                    
            	}
            	catch(Exception e)
            	{
                    SonoraException ex = new SonoraException(SonoraException.INVALID_LOG_NUMFILES, e.getMessage());
                    throw ex;
            	}
            	prop = appProperties.find(displayName + "_NUMFILES");
                        
            	prop.setPropValue(numFiles);
            
            	appProperties.update(prop.getPropName(), prop.getPropValue());
            }
            
            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);

            flushcache.sendCommand(CacheConstants.LOGCONFIGCACHE);
            
            logConfigurationProperties(req, out, "Log");
            
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
    
    public void logCreateCategory(HttpServletRequest req, PrintWriter out)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	
	    try
	    {
	       
	      
	         
	        svrh.setVariable("CATNAMEVALUE", "Sonora.");
                
        	String toolTip = "";
            
            try
            {
            	toolTip = myResources.getString("CATNAME_TOOLTIP");
            }
            catch(Exception e)
            {
            }
         	svrh.setVariable("CATNAME_TOOLTIP", toolTip);
         
       
         	out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_LOG_NEW_CATEGORY_PROPERTIES")));
	
         
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
  
    public void logCreateCategoryPost(HttpServletRequest req, PrintWriter out)
    throws Exception
	{
	   
	    try
	    {
	    	
	        String dottedCatName = UtilRequest.getParameter(req, "CATNAME");
	    
	        int rootIndex = dottedCatName.indexOf('.');
	        if (rootIndex == -1)
	        {
	        	SonoraException ex = new SonoraException(SonoraException.INVALID_CATEGORY_NAME, null);
	        	throw ex;
	        }
	        
	        String rootCategory = dottedCatName.substring(0, rootIndex);
	        boolean validRootCategory = false;
	        
	        if (rootCategory.equals("Sonora"))
	        	validRootCategory = true;
	        else if(rootCategory.equals("Jal"))
	        	validRootCategory = true;
	        else if(rootCategory.equals("Utilities"))
	        	validRootCategory = true;
	        if (!validRootCategory)
	        {
	        	SonoraException ex = new SonoraException(SonoraException.INVALID_CATEGORY_NAME, null);
	        	throw ex;
	        }
	        
	        
	        
	        String catName = dottedCatName.replace('.', '-');
	        
	        SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

	        SystemApplicationProperty prop = new SystemApplicationProperty();
	        
	        prop.setPropValue(dottedCatName);
	        prop.setPropName(catName+"_CATNAME");
	        prop.setPropCategory(SystemApplicationProperty.LOG_CATEGORY);
	        prop.setReadOnly(false);
	       
	        appProperties.create(prop);
	        
	        String logLevel = UtilRequest.getParameter(req, "LOGLEVEL");
	        
	        prop.setPropValue(logLevel);
	        prop.setPropName(catName+"_LOGLEVEL");
	        prop.setPropCategory(SystemApplicationProperty.LOG_CATEGORY);
		    prop.setReadOnly(false);
	       
	        appProperties.create(prop);
	        
	        prop.setPropValue(null);
	        prop.setPropName(catName+"_LOGNAME");
	        prop.setPropCategory(SystemApplicationProperty.LOG_CATEGORY);
		    prop.setReadOnly(false);
	       
	        appProperties.create(prop);
	   
	        prop.setPropValue(null);
	        prop.setPropName(catName+"_MAXSIZE");
	        prop.setPropCategory(SystemApplicationProperty.LOG_CATEGORY);
		    prop.setReadOnly(false);
	       
	        appProperties.create(prop);
	     
	        prop.setPropValue(null);
	        prop.setPropName(catName+"_NUMFILES");
	        prop.setPropCategory(SystemApplicationProperty.LOG_CATEGORY);
		    prop.setReadOnly(false);
	       
	        appProperties.create(prop);
	        
            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);

            flushcache.sendCommand(CacheConstants.LOGCONFIGCACHE);
            
            logConfigurationProperties(req, out, "Log");
            
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}
 
    public void logDeleteCategory(HttpServletRequest req, PrintWriter out, String name)
    throws Exception
	{
	   
	    try
	    {
	        
	        SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

	        //replace dotted displayname with internal prop name thatuses a hyphen
	        // prop names can not contain .
	        name = name.replace('.','-');
	        appProperties.delete(name+"_CATNAME");
	        appProperties.delete(name+"_LOGLEVEL");
	        appProperties.delete(name+"_LOGNAME");
	        appProperties.delete(name+"_MAXSIZE");
	        appProperties.delete(name+"_NUMFILES");
		      
	                 
            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);

            flushcache.sendCommand(CacheConstants.LOGCONFIGCACHE);
            
            logConfigurationProperties(req, out, "Log");
            
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}

    public void updateServerAppProperties(HttpServletRequest req,
                                          PrintWriter out, String category)
        throws Exception
    {

        try
        {
            int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES, category);

            if (index < 0)
                throw new Exception("invalid category" + category);

            SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

            ArrayList appProps = appProperties.findByCategory(category);

            if (appProps != null)
            {
                for (int i = 0; i < appProps.size(); i++)
                {
                	SystemApplicationProperty appProp = (SystemApplicationProperty)
                        appProps.get(i);

                    String propValue = UtilRequest.getParameter(req, appProp.getPropName());   // get the property
             
                    if (propValue != null)
                    {
                        if (category.indexOf(SystemApplicationProperty.CAPTURE_CATEGORY) != -1)
                        {    
                        	try
                        	{
                        		OnValidateCapture valCap = new OnValidateCapture();
                        		valCap.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                 
                        if (category.indexOf(SystemApplicationProperty.ALERTS_CATEGORY) != -1)
                        {    
                        	try
                        	{
                        		OnValidateAlerts valAlerts = new OnValidateAlerts();
                        		valAlerts.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                 
                        if (category.indexOf(SystemApplicationProperty.CACHE_CATEGORY) != -1)
                        {    
                        	try
                        	{
                        		OnValidateCache valCache = new OnValidateCache();
                        		valCache.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                        
                        if (category.indexOf(SystemApplicationProperty.CUSTOM_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateCustom valCustom = new OnValidateCustom();
                        		valCustom.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                
                        if (category.indexOf(SystemApplicationProperty.SYSTEM_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateGeneral valSystem = new OnValidateGeneral();
                        		valSystem.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                
                        if (category.indexOf(SystemApplicationProperty.FILESTORE_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateFilestore valFilestore = new OnValidateFilestore();
                        		valFilestore.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                
                        if (category.indexOf(SystemApplicationProperty.HISTORY_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateHistory valHistory = new OnValidateHistory();
                        		valHistory.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
                
                        if (category.indexOf(SystemApplicationProperty.IMPORT_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateImport valImport = new OnValidateImport();
                        		valImport.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
 
                        if (category.indexOf(SystemApplicationProperty.PROCESS_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateProcess valProcess = new OnValidateProcess();
                        		valProcess.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
 
                        if (category.indexOf(SystemApplicationProperty.QUERY_CATEGORY) != -1)
                        {    
                      
                        	try
                        	{
                         		OnValidateQuery valQuery = new OnValidateQuery();
                        		valQuery.onChange(appProp.getPropName(), propValue);
                        	}
                        	catch (SonoraException se)
                        	{
                        		throw se;
                        	}
                        }
 
                        if (propValue.compareTo("{{PASSWORDPROTECTED}}") != 0)
                        	appProperties.update(appProp.getPropName(), propValue);
                    }

                }
               
          
                CacheSend flushcache = new CacheSend();
                flushcache.sendCommand(CacheConstants.APPPROPCACHE);

                if (category.indexOf(SystemApplicationProperty.CACHE_CATEGORY) != -1)
                {
                    SystemSessionEJB systemSession = ( (SystemSessionEJBHome)
                        ContextAdapter.getHome(ContextAdapter.SYSTEMSESSION)).
                        create();
                    systemSession.flushAllCache();
                }
                
            }

        }
        catch (Exception e)
        {
            throw e;
        }

    }

    public void createCaptureClassProperty(HttpServletRequest req, PrintWriter out)
    throws Exception
    {
    	ResourceBundle myResources = ResourceBundle.getBundle(
        "com.eistream.sonora.system.ServerAdminServletResource",
        UserElement.getUserLocale(req));

    	// Expression support
    	StringVarResolverHashMap svrh = new StringVarResolverHashMap();
    	StringVar svServer = new StringVar(svrh);

    	try
    	{

    		out.println(svServer.resolveNames(myResources.getString(
            	"SYSTEM_APP_CREATE_CAPTURE_CLASS_PROP")));

    	}
    	catch (Exception e)
    	{
    		throw e;
    	}

    }

    public void createUserProperty(HttpServletRequest req, PrintWriter out)
           throws Exception
       {
           ResourceBundle myResources = ResourceBundle.getBundle(
               "com.eistream.sonora.system.ServerAdminServletResource",
               UserElement.getUserLocale(req));

           // Expression support
           StringVarResolverHashMap svrh = new StringVarResolverHashMap();
           StringVar svServer = new StringVar(svrh);

           try
           {

               out.println(svServer.resolveNames(myResources.getString(
                   "SYSTEM_APP_CREATE_USER_PROP")));

           }
           catch (Exception e)
           {
               throw e;
           }

    }

    public void createOcrConnectionPool(HttpServletRequest req, PrintWriter out)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	    try
	    {
	
	        out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_APP_CREATE_OCR_CONNECTION_POOL")));
	
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}

    public void createOrUpdateBPMEServer(HttpServletRequest req, PrintWriter out, boolean isCreate) throws Exception {
        ResourceBundle myResources = ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource",
                UserElement.getUserLocale(req));

        // Expression support
        StringVarResolverHashMap svrh = new StringVarResolverHashMap();
        StringVar svServer = new StringVar(svrh);

        try {
            if (isCreate) {
                svrh.setVariable("BPME_SERVER_FORM_ACTION", "ServerAdmin?op=c&category=BPME");
                svrh.setVariable("ID_READONLY", "");
            } else {
                svrh.setVariable("BPME_SERVER_FORM_ACTION", "ServerAdmin?op=o&category=BPME");
                svrh.setVariable("ID_READONLY", "readonly");
            }

            String testConnection = UtilRequest.getParameter(req, "Test");
            if (!StringUtil.isBlank(testConnection)) {
                // if test connection button pressed than populate svhr from request
                String reqSSL = UtilRequest.getParameter(req, "SSL");
                String reqServerName = UtilRequest.getParameter(req, "SERVER_NAME");
                String reqServerPort = UtilRequest.getParameter(req, "SERVER_PORT");
                String reqCommunity = UtilRequest.getParameter(req, "COMMUNITY_NAME");
                String testURL = OnValidateBPMEServer.sslToProtocol(reqSSL) + "://" + reqServerName + ":" + reqServerPort + "/" + reqCommunity;
                
                boolean passed = doTest(testURL);
                
                svrh.setVariable("ID", UtilRequest.getParameter(req, "ID"));
                svrh.setVariable("DISPLAY_NAME", UtilRequest.getParameter(req, "DISPLAY_NAME"));
                svrh.setVariable("SSL", reqSSL);
                svrh.setVariable("SERVER_NAME", reqServerName);
                svrh.setVariable("SERVER_PORT", reqServerPort);
                svrh.setVariable("COMMUNITY_NAME", reqCommunity);
                svrh.setVariable("TEST_RESULTS", passed ? "Connection established with<br>" + testURL : "Failed to connect to<br>" + testURL);
            } else if (isCreate) { 
                // set default values
                svrh.setVariable("ID", "");
                svrh.setVariable("DISPLAY_NAME", "");
                svrh.setVariable("SSL", "0");
                svrh.setVariable("SERVER_NAME", "");
                svrh.setVariable("SERVER_PORT", "");
                svrh.setVariable("COMMUNITY_NAME", "");
                svrh.setVariable("TEST_RESULTS", "Testing could take up to 15 seconds.");
            } else {
                // get values from DB
                OnValidateBPMEServer bpmeConfig = new OnValidateBPMEServer();
                String bpmeId = UtilRequest.getParameter(req, "ID");
                svrh.setVariable("ID", bpmeId);
                svrh.setVariable("DISPLAY_NAME", bpmeConfig.getDisplayName(bpmeId));
                svrh.setVariable("SSL", bpmeConfig.getSSL(bpmeId));
                svrh.setVariable("SERVER_NAME", bpmeConfig.getServerName(bpmeId));
                svrh.setVariable("SERVER_PORT", bpmeConfig.getServerPort(bpmeId));
                svrh.setVariable("COMMUNITY_NAME", bpmeConfig.getCommunity(bpmeId));
                svrh.setVariable("TEST_RESULTS", "Testing could take up to 15 seconds.");
            }

            out.println(svServer.resolveNames(myResources.getString("SYSTEM_APP_BPME_SERVER_DETAILS")));
        } catch (Exception e) {
            throw e;
        }

    }

    private boolean doTest(String testUrl) {
        try {
            URL url = new URL(testUrl);//"https://bps-adfsfed01.opentext.com/adfs/services/trust/13/UsernameMixed");
            HttpURLConnection  conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            int timeout = 15000;
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.connect();
            while (conn.getResponseCode() == -1 && timeout > 0) {
                try {
                    timeout -= 200;
                    Thread.sleep(200);
                } catch (Exception ex) {
                    timeout = -1; 
                }
            }
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            // do nothing, connection failed but it's OK
        }
        return false;
    }
    
    public void createUserPropertyPost(HttpServletRequest req,
                                       PrintWriter out, String category)
        throws Exception
    {

        try
        {
            int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES,
                                            category);

            if (index < 0)
                throw new Exception("invalid category" + category);

            SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

            SystemApplicationProperty userProp = new SystemApplicationProperty();

            userProp.setPropValue(UtilRequest.getParameter(req, "VALUE"));
            userProp.setPropName(UtilRequest.getParameter(req, "NAME"));
            userProp.setPropLabel(UtilRequest.getParameter(req, "LABEL"));
            userProp.setPropToolTip(UtilRequest.getParameter(req, "TOOLTIP"));
            userProp.setPropCategory(category);
          
            try
            {
            	OnValidateCustom valCustom = new OnValidateCustom();
            	valCustom.onCreate(userProp.getPropName(), userProp.getPropValue());
            }
            catch (SonoraException se)
            {
            	throw se;
            }
    
            appProperties.create(userProp);
            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);


        }
        catch (Exception e)
        {
            throw e;
        }

    }

    
    public void createOcrConnectionPropertyPost(HttpServletRequest req, PrintWriter out, String category)
	throws Exception
	{
	
		try
		{
			int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES,
			                 category);
			
			if (index < 0)
				throw new Exception("invalid category" + category);
			
			
			String connectionName = UtilRequest.getParameter(req, "NAME");
			String connectionDisplayName = UtilRequest.getParameter(req, "DISPLAYNAME");
			String connectionIdleTimeOut = UtilRequest.getParameter(req, "IDLETIMEOUT");
				
			OnValidateOcrConnectionPool validatePool = new OnValidateOcrConnectionPool();
			validatePool.onCreateConnectionPool(connectionName, connectionDisplayName, connectionIdleTimeOut);
			
			String addConnection = UtilRequest.getParameter(req, "addConnection");
			if (addConnection != null && addConnection.length() > 0)
			{
				validatePool.addConnection(connectionName);
				
			}
			
			CacheSend flushcache = new CacheSend();
			flushcache.sendCommand(CacheConstants.APPPROPCACHE);
			if (addConnection != null && addConnection.length() > 0)
				ocrConnectionPoolProperties(req, out,connectionName);
			else
				ocrConfigurationProperties(req, out, category);
			  	
		
		}
		catch (Exception e)
		{
		throw e;
		}
		
	}
    
    public void createOrUpdateBPMEServerPost(HttpServletRequest req, PrintWriter out, boolean isCreate) throws Exception {
        try {
            String bpmeId = UtilRequest.getParameter(req, "ID");
            String bpmeDisplayName = UtilRequest.getParameter(req, "DISPLAY_NAME");
            String bpmeSSL = UtilRequest.getParameter(req, "SSL");
            String bpmeServerName = UtilRequest.getParameter(req, "SERVER_NAME");
            String bpmeServerPort = UtilRequest.getParameter(req, "SERVER_PORT");
            String bpmeCommunity = UtilRequest.getParameter(req, "COMMUNITY_NAME");

            OnValidateBPMEServer bpmeConfig = new OnValidateBPMEServer();

            String testConnection = UtilRequest.getParameter(req, "Test");
            if (!StringUtil.isBlank(testConnection)) {
                // if test connection button pressed than don't commit changes and just redirect to createOrUpdateBPMEServer
                createOrUpdateBPMEServer(req, out, isCreate);
                return;
            } else if (isCreate) {
                bpmeConfig.onCreateBPMEServer(bpmeId, bpmeDisplayName, bpmeSSL, bpmeServerName, bpmeServerPort,
                        bpmeCommunity);
            } else {
                bpmeConfig.onUpdateBPMEServer(bpmeId, bpmeDisplayName, bpmeSSL, bpmeServerName, bpmeServerPort, bpmeCommunity);
            }

            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);
            
            bpmeConfigurationProperties(req, out, SystemApplicationProperty.BPME_CATEGORY);

        } catch (Exception e) {
            throw e;
        }
    }    

    public void updateOcrConnectionsPost(HttpServletRequest req, PrintWriter out, String connectionPoolName)
	throws Exception
	{
	
		try
		{
	
			SystemApplicationProperty ocrProp = null;

			OnValidateOcrConnectionPool ocrPool = new OnValidateOcrConnectionPool();

			ocrProp = ocrPool.getSize(connectionPoolName);
		
			int connections = new Integer(ocrProp.getPropValue()).intValue();
		
			ArrayList ports = ocrPool.getPorts(req, connectionPoolName, ocrProp.getPropValue());
			ArrayList hosts = ocrPool.getHosts(req, connectionPoolName, ocrProp.getPropValue());
		
			
			
			SystemApplicationProperty portProp = null;
			SystemApplicationProperty hostProp = null;
			SystemApplicationPropertyDAO sysProp = new SystemApplicationPropertyDAO();
			
			for (int i = 0; i < connections; i++)
			{
				portProp = (SystemApplicationProperty)ports.get(i);
				try
				{
					new Integer(portProp.getPropValue());
				}
				catch(NumberFormatException ne)
				{
					SonoraException ex = new SonoraException(SonoraException.INVALID_OCR_PORT, null);
					throw ex;	       
				}
				sysProp.update(portProp.getPropName(), portProp.getPropValue());
				hostProp = (SystemApplicationProperty)hosts.get(i);
				if (hostProp.getPropValue() == null || hostProp.getPropValue().length() == 0)
				{
					SonoraException ex = new SonoraException(SonoraException.INVALID_OCR_HOST, null);
					throw ex;	       
				}
				sysProp.update(hostProp.getPropName(), hostProp.getPropValue());
				
			}
			
			ocrProp = ocrPool.getIdleTimeOut(req,connectionPoolName);
			
			if (ocrProp.getPropValue() != null && ocrProp.getPropValue().length() > 0)
			{
				try
				{
					int idleTimeOutIntValue = Integer.parseInt(ocrProp.getPropValue());
					
					if(idleTimeOutIntValue < 0)
					{
						throw new SonoraException(SonoraException.INVALID_IDLE_TIMEOUT, null);
					}
				}
				catch(NumberFormatException ne)
				{
				    SonoraException ex = new SonoraException(SonoraException.INVALID_IDLE_TIMEOUT, null);
			        throw ex;	       
				}
			}
			
			sysProp.update(ocrProp.getPropName(), ocrProp.getPropValue());
		
			ocrProp = ocrPool.getDisplayName(req,connectionPoolName);
			sysProp.update(ocrProp.getPropName(), ocrProp.getPropValue());
		
			
			String addConnection = UtilRequest.getParameter(req, "addConnection");
			if (addConnection != null && addConnection.length() > 0)
			{
				ocrPool.addConnection(connectionPoolName);
			}
		
			CacheSend flushcache = new CacheSend();
			flushcache.sendCommand(CacheConstants.APPPROPCACHE);
			if (addConnection != null && addConnection.length() > 0)
				ocrConnectionPoolProperties(req, out, connectionPoolName);
			else
				ocrConfigurationProperties(req, out, SystemApplicationProperty.OCR_CATEGORY);
		}
		catch (Exception e)
		{
		throw e;
		}
		
	}

    public void ocrConnectionPoolProperties(HttpServletRequest req, PrintWriter out, String connectionName)
    throws Exception
	{
	    ResourceBundle myResources = ResourceBundle.getBundle(
	        "com.eistream.sonora.system.ServerAdminServletResource",
	        UserElement.getUserLocale(req));
	
	    // Expression support
	    StringVarResolverHashMap svrh = new StringVarResolverHashMap();
	    StringVar svServer = new StringVar(svrh);
	
	    try
	    {
	        svrh.setVariable("NAME", connectionName);
	    	
	        SystemApplicationProperty ocrProp = null;

	        OnValidateOcrConnectionPool ocrPool = new OnValidateOcrConnectionPool();
	        ocrProp = ocrPool.getDisplayName(connectionName);
	        if (ocrProp != null)
	        	svrh.setVariable("DISPLAYNAME", ocrProp.getPropValue());
	        else
	        	svrh.setVariable("DISPLAYNAME", "");
		
	        ocrProp = ocrPool.getIdleTimeOut(connectionName);
	        if (ocrProp != null)
	        	svrh.setVariable("IDLETIMEOUT", ocrProp.getPropValue());
	        else
	        	svrh.setVariable("IDLETIMEOUT", null);
	     
	        ocrProp = ocrPool.getSize(connectionName);
		     
	        svrh.setVariable("CONNECTIONS", ocrProp.getPropValue());
	    	
	        out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_APP_OCR_CONNECTION_POOL_PROPERTIES_START")));
	
	        // get the hosts and port for each connection
	        ArrayList hosts = ocrPool.getHosts(connectionName, ocrProp.getPropValue());
	        ArrayList ports = ocrPool.getPorts(connectionName, ocrProp.getPropValue());
	        
	        SystemApplicationProperty ocrHostProp = null;
	        SystemApplicationProperty ocrPortProp = null;

	        
	        
	        for (int i = 0; i < hosts.size(); i++)
	        {
	        	ocrHostProp = (SystemApplicationProperty)hosts.get(i);
	        	ocrPortProp = (SystemApplicationProperty)ports.get(i);
	            svrh.setVariable("connectionNumber", Integer.toString(i+1));
	            svrh.setVariable("indexNumber", Integer.toString(i));
	            if (ocrHostProp.getPropValue() == null)
	            	svrh.setVariable("HOST", "");
	            else
	            	svrh.setVariable("HOST", ocrHostProp.getPropValue());
		        svrh.setVariable("PORT", ocrPortProp.getPropValue());
	       
		        if (i == (hosts.size() - 1))
	            {
	            	String deleteURL = svServer.resolveNames(myResources.getString("SYSTEM_DELETE_OCR_CONNECTION"));
	            	svrh.setVariable("SYSTEM_DELETE_OCR_CONNECTION", deleteURL);
	            }
	            else
	              	svrh.setVariable("SYSTEM_DELETE_OCR_CONNECTION", "");
		        
		        out.println(svServer.resolveNames(myResources.getString(
	            "SYSTEM_APP_OCR_CONNECTION_POOL_PROPERTIES_CONNECTIONS")));
	                	
	        }
	        out.println(svServer.resolveNames(myResources.getString(
            "SYSTEM_APP_OCR_CONNECTION_POOL_PROPERTIES_END")));

	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}

    public void createCaptureClassPropertyPost(HttpServletRequest req,
            PrintWriter out, String category)
	throws Exception
	{

    	try
    	{
    		int index = Arrays.binarySearch(SystemApplicationProperty.CATEGORIES,
                 category);

    		if (index < 0)
    			throw new Exception("invalid category" + category);

    		SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

    		SystemApplicationProperty userProp = new SystemApplicationProperty();

    		userProp.setPropValue(UtilRequest.getParameter(req, "VALUE"));
    		userProp.setPropName(UtilRequest.getParameter(req, "NAME"));
    		userProp.setPropLabel(UtilRequest.getParameter(req, "NAME"));
    		userProp.setPropToolTip(UtilRequest.getParameter(req, "VALUE"));
    		userProp.setPropCategory(category);
    		userProp.setReadOnly(false);
    		userProp.setRelatedProp("CAPTURE_CLASSES");
    		
    		try
			{
				Class eventHandler = Class.forName("com.eistream.sonora.system.OnValidateCapture");
				OnValidateCapture handler = (OnValidateCapture)eventHandler.newInstance();
				handler.onCreate(userProp.getPropName(), userProp.getPropValue());
			}
			catch (ClassNotFoundException ce)
			{
			}
			catch (SonoraException se)
			{
				throw se;
			}
	
    		
    		appProperties.create(userProp);
    		
    		userProp = appProperties.find("CAPTURE_CLASSES");
    		// check if related property contains the new property value
    		if (userProp.getPropValue().indexOf(UtilRequest.getParameter(req, "NAME")) == -1)
    		{
    			appProperties.update("CAPTURE_CLASSES", userProp.getPropValue()+","+ UtilRequest.getParameter(req, "NAME"));
    		}
    		CacheSend flushcache = new CacheSend();
    		flushcache.sendCommand(CacheConstants.APPPROPCACHE);

    	}
    	catch (Exception e)
    	{
    		throw e;
    	}

	}
    public void deleteProperty(String propName,String category)
        throws Exception
    {

        try
        {
        	SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();

            appProperties.delete(propName);
            
            if (category.indexOf(SystemApplicationProperty.CAPTURE_CATEGORY) != -1)
            {    
            	// remove the capture class property from the related property
            	SystemApplicationProperty userProp = appProperties.find("CAPTURE_CLASSES");
            	Pattern p = Pattern.compile(","+propName);
//            	 Create a matcher with an input string
                Matcher m = p.matcher(userProp.getPropValue());
                StringBuffer sb = new StringBuffer();
                boolean result = m.find();
                // Loop through and create a new String 
                // with the replacements
                while(result)
                {
                    m.appendReplacement(sb, "");
                    result = m.find();
                }
                // Add the last segment of input to 
                // the new String
                m.appendTail(sb);
       			appProperties.update("CAPTURE_CLASSES", sb.toString());
            }
    		CacheSend flushcache = new CacheSend();
    		flushcache.sendCommand(CacheConstants.APPPROPCACHE);

     
        }
        catch (Exception e)
        {
            throw e;
        }

    }


    public void deleteOcrPool(HttpServletRequest req, PrintWriter out, String poolName)
    throws Exception
    {

	    try
	    {
	    	SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();
	
//		 	We can't do a wild-card delete because they might be deleting XYZ and it would match all XYZ2 occurrences 
//	        appProperties.deleteLike(SystemApplicationProperty.OCR_CATEGORY,poolName+"%");
			OnValidateOcrConnectionPool validatePool = new OnValidateOcrConnectionPool();
			validatePool.onDeleteConnectionPool(poolName);
	
	    	// remove the pool name from the NAMES property
	    	SystemApplicationProperty namesProp = appProperties.find("NAMES");
	    	
	    	// break the list up into individual names
	    	StringTokenizer st = new StringTokenizer(namesProp.getPropValue(), ",");
	    	StringBuffer sb = new StringBuffer();
	    	while (st.hasMoreElements())
	    	{
	    		String oneName = st.nextToken();
	    		// Copy all but the one we want to remove
	    		if (!oneName.equals(poolName))
	    		{
	    			// If this isn't the first one then add a ','
	    			if (sb.length() > 0)
	    				sb.append(",");
	    			
	    			// This one's a keeper so add it
	    			sb.append(oneName);
	    		}
	    	}

	    	// If nothing was dropped then don't bother with the update
	    	if (sb.length() != namesProp.getPropValue().length())
	    		appProperties.update("NAMES", sb.length() == 0 ? null : sb.toString());
	      
	    	CacheSend flushcache = new CacheSend();
			flushcache.sendCommand(CacheConstants.APPPROPCACHE);
			
			ocrConfigurationProperties(req, out, SystemApplicationProperty.OCR_CATEGORY);
		 
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}

    
    public void deleteOcrPoolConnection(HttpServletRequest req, PrintWriter out, String poolName, String host, String port)
    throws Exception
    {

	    try
	    {
	    	SystemApplicationPropertyDAO appProperties = new SystemApplicationPropertyDAO();
	
	        appProperties.delete(host);
	        appProperties.delete(port);
		
	        OnValidateOcrConnectionPool ocrPool = new OnValidateOcrConnectionPool();
	        ocrPool.subConnection(poolName);
	        
	    	CacheSend flushcache = new CacheSend();
			flushcache.sendCommand(CacheConstants.APPPROPCACHE);
			
			ocrConnectionPoolProperties(req, out, poolName);
		 
	    }
	    catch (Exception e)
	    {
	        throw e;
	    }
	
	}

    public void deleteBPMEServer(HttpServletRequest req, PrintWriter out, String bpmeId) throws Exception {
        try {
            OnValidateBPMEServer bpmeConfig = new OnValidateBPMEServer();
            bpmeConfig.onDeleteBPMEServer(bpmeId);

            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.APPPROPCACHE);

            bpmeConfigurationProperties(req, out, SystemApplicationProperty.BPME_CATEGORY);

        } catch (Exception e) {
            throw e;
        }
    }
    
    private void doError(HttpServletRequest req, Exception error, PrintWriter out, int pendingTransaction)
    {
        SonoraException ex = SonoraException.getFromException(error);

        ResourceBundle myResources = ResourceBundle.getBundle("com.eistream.sonora.system.ServerAdminServletResource", UserElement.getUserLocale(req));
        String tranDesc = null;
		try
		{
    		tranDesc = myResources.getString("TRAN" + String.valueOf(pendingTransaction));
		}
		catch(MissingResourceException mrex)
		{
		}

	    String user = null;
	  	try
	  	{
	  		user = CurrentUser.currentServletUser(req);
	  	}
	  	catch(Exception e)
	  	{
	  	}

		out.println(SonoraException.buildErrorPageFromException(ex, tranDesc, UserElement.getUserLocale(req), user));
    }

    private String convertLogLevel(String propValue, ResourceBundle myResources)
    {
    	
    	try
    	{
    		return myResources.getString(propValue);
    	}
    	catch(Exception e)
    	{
    		return "";
    	}
    }
}

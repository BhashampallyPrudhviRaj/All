package com.eistream.sonora.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.eistream.sonora.cachemanager.CacheConstants;
import com.eistream.sonora.cachemanager.CacheRegister;
import com.eistream.sonora.cachemanager.CacheSend;
import com.eistream.sonora.csrfguard.CSRFGuardTokenResolver;
import com.eistream.sonora.events.OnApplication;
import com.eistream.sonora.exceptions.CalendarAlreadyExistsException;
import com.eistream.sonora.exceptions.SonoraException;
import com.eistream.sonora.fields.FmsCache;
import com.eistream.sonora.fields.formats.FieldFormatsCache;
import com.eistream.sonora.filestore.FileExcludeListCache;
import com.eistream.sonora.filestore.FileExtensionMapCache;
import com.eistream.sonora.filestore.StorageManagerCache;
import com.eistream.sonora.filestore.TemporaryFileCache;
import com.eistream.sonora.forms.FormsCache;
import com.eistream.sonora.forms.SonoraForm;
import com.eistream.sonora.licensemanager.LicenseCache;
import com.eistream.sonora.nbd.NBDSessionEJB;
import com.eistream.sonora.nbd.NBDSessionEJBHome;
import com.eistream.sonora.nbd.WCCalculatedExcept;
import com.eistream.sonora.nbd.WCDay;
import com.eistream.sonora.nbd.WCExcept;
import com.eistream.sonora.nbd.WCExceptions;
import com.eistream.sonora.nbd.WCWeek;
import com.eistream.sonora.nbd.WorkCalendar;
import com.eistream.sonora.nbd.WorkCalendarCache;
import com.eistream.sonora.nbd.WorkCalendarElement;
import com.eistream.sonora.objects.ObjectsElement;
import com.eistream.sonora.property.AclProperty;
import com.eistream.sonora.query.QueryCache;
import com.eistream.sonora.recordsmanager.ComponentDefinition;
import com.eistream.sonora.recordsmanager.RecordsManagerCache;
import com.eistream.sonora.repositories.RepositoryCache;
import com.eistream.sonora.security.RoleDefCache;
import com.eistream.sonora.security.SecurityCache;
import com.eistream.sonora.system.ApplicationsCache;
import com.eistream.sonora.system.MenuCache;
import com.eistream.sonora.system.ScriptCache;
import com.eistream.sonora.system.SystemApplicationProperties;
import com.eistream.sonora.system.SystemSessionEJB;
import com.eistream.sonora.system.SystemSessionEJBHome;
import com.eistream.sonora.system.TransformerCache;
import com.eistream.sonora.templates.TemplatesCache;
import com.eistream.sonora.users.UserCache;
import com.eistream.sonora.users.UserElement;
import com.eistream.sonora.util.Base64UrlSafe;
import com.eistream.sonora.util.BlobOverflow;
import com.eistream.sonora.util.ContextAdapter;
import com.eistream.sonora.util.CurrentUser;
import com.eistream.sonora.util.ExpressionCache;
import com.eistream.sonora.util.HtmlUtil;
import com.eistream.sonora.util.LogCategoryEx;
import com.eistream.sonora.util.NaturalComparator;
import com.eistream.sonora.util.StatisticsCache;
import com.eistream.sonora.util.StringUtil;
import com.eistream.sonora.util.UtilRequest;
import com.eistream.sonora.util.XmlUtil;
import com.eistream.sonora.workflow.workflows.WorkflowsCache;
import com.eistream.sonora.workflow.workpump.GetNextController;
import com.eistream.utilities.StringVar;
import com.eistream.utilities.StringVarResolverHashMap;
import com.eistream.utilities.TimestampUtil;
import com.eistream.utilities.calendar.CalendarFactory;
import com.eistream.utilities.calendar.ICalendar;
import com.eistream.utilities.calendar.IDateFormat;
import com.eistream.utilities.calendar.IFactory;
import com.eistream.utilities.calendar.ISimpleDateFormat;
import com.eistream.utilities.tiff.TiffTocCache;

public class AdminServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	  
    private LogCategoryEx  m_Log;

	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
       
        try
        {
            m_Log = new LogCategoryEx("Sonora.admin.AdminServlet");
        }
        catch (Exception e)
        {
        }
	}

	/**
	 */
	public void destroy()
	{
		super.destroy();
	}

	public String getServletInfo()
	{
		return "Administration application";
	}

	/**
	 * This is the standard HttpServlet doGet method called when someone does a get on this servlet.
	 * <hr>
	 * The following paramaters are used: <br>
	 * Paramater    Description<br>
	 * op           the operation to perform.<br>
	 * fs           the FrameSet to use.<br>
	 * <br>
	 * <hr>
	 * The following operations are supported:<br>
	 * op	[optional] (required)<br>
	 * <hr>
	 * l	display the Admin frameset.<br>
	 * g	display the Admin menu.<br>
	 * b	display a blank Screen.<br>
	 * p	displays the Work Calendar property page.<br>
	 * c	flush all caches.<br>
	 * C	display the Cache manager page.<br>
	 * d	deletes the specifed work calendar.<br>
	 * I	displays the named scripts frame.<br>
	 * i	displays the named scripts page.<br>
	 * a	flush the ACL cache.<br>
	 * f	flush the field cache.<br>
	 * h	flush the forms cache.<br>
	 * n	flush the Metrics cache.<br>
	 * o	flush the Licence cache.<br>
	 * r	flush the Repository cache.<br>
	 * s	flush the System table of contents cache.<br>
	 * t	flush the tiff table of contents cache.<br>
	 * u	flush the User cache.<br>
	 * w	flush the Workflow caches.<br>
	 * N	flush the GetNext queues cache.<br>
	 * x	display the cache manager page.<br>
	 * y	flush the Templates cache.
	 * q	flush pool cache.<br>
	 * z	flush data manager cache.<br>
	 * T	flush temporary file cache.<br>
	 * U	flush the file extension map cache.<br>
	 * Z	flush the file exclude list cache.<br>
	 * B	flush the business work calendar cache.<br>
     * m    flush the menu cache.<br>
     * e	flush the expression cache.<br>
	 * 
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

		String  MethodName="doGet";

		// Create the HTML output Writer

		String charEncoding = null;

		// Create the HTML output Writer
		resp.setContentType("text/html; charset=" + "UTF-8");
		charEncoding = "UTF8";
		PrintWriter out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), charEncoding), true);


		// Disable cache
		resp.setHeader("Pragma", "no-cache");
		resp.setHeader("Expires", "-1");

		// Find command line parameters
		String szOperation = UtilRequest.getParameter(req, "op");		 // Operation
		String szFrameSet = UtilRequest.getParameter(req, "fs");		 // Frameset name
		String menuName = UtilRequest.getParameter(req, "mu");			// Menu name (only for op=g)
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));


		// Process Command Line arguments
		try
		{
			String user = CurrentUser.currentServletUser(req);
			SecurityCache.checkStrictAdminPermission(user);
			
			String onApplicationClass = null;
			onApplicationClass =  ContextAdapter.getAdapterValue("APPLICATIONEVENTCLASS","");

			if ((onApplicationClass != null) && (onApplicationClass.length() > 0))
			{
				try
				{
					Class eventHandler = Class.forName("com.eistream.sonora.events.OnApplication" + onApplicationClass);
					OnApplication handler = (OnApplication)eventHandler.newInstance();
					handler.onUserStart(CurrentUser.currentServletUser(req), req, resp);
				}
				catch (Exception e)
				{
				}
			}

			// Command line defaults
			if (szOperation == null || szOperation.length() == 0)
				szOperation = "l";

            if (szOperation.equals("gc"))
            {
                int MEG = 1024 * 1024;
                Runtime rt = Runtime.getRuntime();
                long freeMemoryBeforeGC = rt.freeMemory()/MEG;
                rt.gc();
                long freeMemoryAfterGC = rt.freeMemory()/MEG;
                
                StringVarResolverHashMap svrhm = new StringVarResolverHashMap();
                StringVar sv = new StringVar(svrhm);
                svrhm.setVariable("totalMemory", new Long(rt.totalMemory()/MEG));
                svrhm.setVariable("maxMemory", rt.maxMemory()==Long.MAX_VALUE ? (Object) "Unlimited" : new Long(rt.maxMemory()/MEG));
                svrhm.setVariable("freeMemoryBeforeGC", new Long(freeMemoryBeforeGC));
                svrhm.setVariable("freeMemoryAfterGC", new Long(freeMemoryAfterGC));
                svrhm.setVariable("availableProcessors", new Integer(rt.availableProcessors()));
                
                out.println(sv.resolveNames(MyResources.getString("ADMIN_GC_SUMMARY")));
            }
            else if (szOperation.equals("prop"))
            {
                if (!SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S, SecurityCache.SYSTEM_ACL_NAME) ||
                        !ContextAdapter.getBooleanValue("ALLOW_PROP_DUMP", false))
                {
                    out.println(MyResources.getString("ADMIN_BLANK"));
                }
                else
                {
                    String prop = UtilRequest.getParameter(req, "prop");
    
                    // Get HTML "Unknown Command" Resource
                    if (StringUtil.isBlank(prop) || !Character.isJavaIdentifierStart(prop.charAt(0)))
                    {
                        // Get HTML "Unknown Command" Resource
                        if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
                            out.println(MyResources.getString("UNKNOWN_COMMAND_DISPLAY"));
                    }
                    else
                    {
                        ResourceBundle rb = ResourceBundle.getBundle(prop, UserElement.getUserLocale(req));
                        
                        StringVarResolverHashMap svrhm = new StringVarResolverHashMap();
                        StringVar sv = new StringVar(svrhm);

                        sv.setPattern(sv.resolveNames(MyResources.getString("ADMIN_PROP_ROW")));
                        StringBuffer sb = new StringBuffer();
                        
                        List<String> keyList = Collections.list(rb.getKeys());
                        Collections.sort(keyList, new NaturalComparator());
                        Enumeration<String> sortedEnum = Collections.enumeration(keyList); 
                        
                        while (sortedEnum.hasMoreElements())
                        {
                            String key = sortedEnum.nextElement();
                            String value = HtmlUtil.toSafeHtml(rb.getString(key));
                            
                            value = value.replaceAll("\r*\n", "<br />");
                            
                            svrhm.setVariable("key", key);
                            svrhm.setVariable("value", value);
                            sv.resolveNames(sb);
                        }
                        
                        svrhm.setVariable("rows", sb.toString());
                        svrhm.setVariable("prop", HtmlUtil.toSafeHtml(prop));
                        out.println(sv.resolveNames(MyResources.getString("ADMIN_PROP_SUMMARY")));
                    }
                }
            }
			else switch (szOperation.charAt(0))
			{
			case 'l':
				  boolean isIE = true;
                  String browser = req.getHeader("user-agent");
                  if (browser != null && browser.length() > 0)
                  {
                  	if (browser.indexOf("IE") != -1)
                  	   isIE = true;
                  	else
                         isIE = false;
                  }
                  else
                  	isIE = true;

				// Get admin window's frameset
				if (szFrameSet == null)
				{
					if (isIE)
						szFrameSet = "FRAMESET_default_IE";
					else
						szFrameSet = "FRAMESET_default_MOZILLA";
				}
				else
					szFrameSet = "FRAMESET_" + szFrameSet;
				// Get the HTML template for the given frameset from the resource bundle
				out.println(com.eistream.sonora.util.Version.resolveVersionAndCopyright(MyResources.getString(szFrameSet)));
				break;

			case 'g':
				// Display the toolbox menu
				displayMenu(req, MyResources, menuName, out);
				break;

			case 'b':
				// Display Blank screen
				out.println(MyResources.getString("ADMIN_BLANK"));
				break;

			case 'p':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					workCalendarList(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;

			case 'P':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					workCalendarProperties(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;
				
			case 'I':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					scriptsFrame(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;

			case 'i':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					scriptsPage(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;

			case 'd':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
				{
					deleteWorkCalendar(req, out);
					
					// redisplay list of calendars
					workCalendarList(req, out);
				}
				else
				{
					out.println(MyResources.getString("ADMIN_BLANK"));
				}
				break;

			case 'C':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					cacheManage(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;

			case 'c':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					flushAllCache(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;

			case 'a':
				flushCache(req, CacheConstants.ACLCACHE, out);
				break;
            case 'D':
                flushCache(req, CacheConstants.ROLEDEFCACHE, out);
                break;                
			case 'f':
				flushCache(req, CacheConstants.FIELDCACHE, out);
				break;
			case 'h':
				flushCache(req, CacheConstants.FORMSCACHE, out);
				break;
			case 'n':
				flushCache(req, CacheConstants.STATISTICSCACHE, out);
				break;
			case 'o':
				flushCache(req, CacheConstants.LICENSECACHE, out);
				break;
			case 'r':
				flushCache(req, CacheConstants.REPOSITORYCACHE, out);
				break;
			case 's':
				flushCache(req, CacheConstants.SERVERCACHE, out);
				break;
			case 't':
				flushCache(req, CacheConstants.TIFFTOCCACHE, out);
				break;
			case 'u':
				flushCache(req, CacheConstants.USERCACHE, out);
				break;
			case 'w':
				flushCache(req, CacheConstants.WORKFLOWCACHES, out);
				break;
			case 'N':
				flushCache(req, CacheConstants.GETNEXTCACHE, out);
				break;
			case 'x':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
												SecurityCache.SYSTEM_ACL_NAME))
					cacheManager(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;
			case 'y':
				flushCache(req, CacheConstants.TEMPLATESCACHE, out);
				break;
			case 'q':
				flushCache(req, CacheConstants.POOLCACHE, out);
				break;
			case 'z':
				flushCache(req, CacheConstants.DATAMANAGERCACHE, out);
				break;
			case 'T':
				flushCache(req, CacheConstants.TEMPORARYFILECACHE, out);
				break;
			case 'U':
				flushCache(req, CacheConstants.FILEEXTENSIONMAPCACHE, out);
				break;
			case 'Z':
				flushCache(req, CacheConstants.FILEEXCLUDELISTCACHE, out);
				break;
			case 'B':
				flushCache(req, CacheConstants.NBDCACHE, out);
				break;
			case 'V':
				flushCache(req, CacheConstants.APPPROPCACHE, out);
				break;
			case 'Q':
				flushCache(req, CacheConstants.QUERYCACHE, out);
				break;
			case 'R':
				flushCache(req, CacheConstants.FMSROWCACHE, out);
				break;
			case 'S':
				flushCache(req, CacheConstants.FIELDSSTYLECACHE, out);
				break;                
            case 'm':
                flushCache(req, CacheConstants.MENUCACHE, out);
                break;                
            case 'e':
                flushCache(req, CacheConstants.EXPRESSIONSCACHE, out);
                break;
            case 'E':
                flushCache(req, CacheConstants.SCRIPTSCACHE, out);
                break;
            case 'X':
            	flushCache(req, CacheConstants.TRANSFORMERCACHE, out);	// XML Transformer templates cache
            	break;
			case '@':
				if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S, SecurityCache.SYSTEM_ACL_NAME))
					dumpObjects(req, out);
				else
					out.println(MyResources.getString("ADMIN_BLANK"));
				break;
            case '$':
                flushCache(req, CacheConstants.RECORDSMANAGERCACHE, out);
                break;                
            case 'A':
                flushCache(req, CacheConstants.APPLICATIONSCACHE, out);
                break;
                
                
			default:
				// Get HTML "Unknown Command" Resource
				if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
					out.println(MyResources.getString("UNKNOWN_COMMAND_DISPLAY"));
				break;
			}
		}
		
		catch (Exception error)
		{
			m_Log.error(MethodName, "E_EXCEPTION", error);
	        doErrorEx(req, error, out, MethodName, szOperation);
		}

		finally
		{
		   if(!CSRFGuardTokenResolver.isCsrfGuardEnabled())
		       out.close();
		}
	}

	// Handle the Post Request
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		String cancel = UtilRequest.getParameter(req, "cancel");
		if (cancel != null)
			return;

		// Create the HTML output Writer

		String charEncoding = null;
		resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
		charEncoding = UtilRequest.getCharEncoding();
		PrintWriter out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), charEncoding), true);


		// Find command line parameters
		String szOperation = UtilRequest.getParameter(req, "op");	 // Operation

		// Process Command Line arguments
		try
		{
			String user = CurrentUser.currentServletUser(req);
			SecurityCache.checkStrictAdminPermission(user);
			
			// Make sure "op" exists, then go to specified operation
			if (szOperation != null)
			{
				switch (szOperation.charAt(0))
				{

				case 'W':
					// Update the calendar if not a cancel - note the "Cancel" is case sensitive so
					// its not found in the earlier check for "cancel"
					String sCancel = UtilRequest.getParameter(req, "Cancel");
					if (sCancel == null)
						workCalendarUpdate(req, out);
					
					// redisplay list of calendars if no errors
					workCalendarList(req, out);
					break;

				default:
					// Get HTML "Unknown Command" Resource
					ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
					if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
						out.println(MyResources.getString("UNKNOWN_COMMAND_DISPLAY"));
					break;
				}
			}

			// Otherwise report a usage error
			else
			{
				// Get HTML "Unknown Command" Resource
				ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
				if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
					out.println(MyResources.getString("UNKNOWN_COMMAND_DISPLAY"));
			}
		}

		catch (Exception error)
		{
			// Log Error (fix this section later)
			String  MethodName = "doPost";
			m_Log.error(MethodName, "E_EXCEPTION", error);
			
	        doErrorEx(req, error, out, MethodName, szOperation);
		}

		finally
		{
		    if(!CSRFGuardTokenResolver.isCsrfGuardEnabled())
		        out.close();
		}
	}

	/**
	 * This method builds and returns the Admin toobox menu.
	 */
	private void displayMenu(HttpServletRequest req, ResourceBundle resources, String menuName, PrintWriter out) throws Exception
	{
		// Get the menu definition (they can specify alternate menus with a mu= parameter -- just like the menu servlet)
		if (menuName == null)
			menuName = "TOOLBOX";
		String menu = resources.getString("MENU_" + menuName);

		// Get the constant portions of the menu HTML.
		String menuStart    = resources.getString("ADMIN_MENU_START");
		String menuMiddle   = resources.getString("ADMIN_MENU_MIDDLE");
		String menuEnd      = resources.getString("ADMIN_MENU_END");

		// These are where we build the menuEntries and menuReset strings
		StringBuffer menuEntries = new StringBuffer();
		StringBuffer menuReset   = new StringBuffer();

		// Loop through the menu entries in the menu definition string
		boolean isLastOneBlank = true;
		int indexStart = 0;
		int indexEnd;
		for (int i = 0;  indexStart != -1; ++i)
		{
			// Pick the entries off the menu string one by one
			indexEnd = menu.indexOf(';', indexStart);
			if (indexEnd == -1)
				break;
			String menuEntry = menu.substring(indexStart, indexEnd).trim();
			indexStart = indexEnd + 1;

			// Get the ACL and Permission to be tested
			String acl          = resources.getString("MENU_ACL_"  + menuEntry).trim();
			String permission   = resources.getString("MENU_PERM_" + menuEntry).trim();

			// Test this user against the specified ACL and Permission

			// Note that the permission can be blank (to unconditionally show the menu item),
			// can be a single permission, or can be permissions logically combined with
			// "&&" or "||".
			boolean mayI;
			int logicalOpStart;
			if ( permission.length() == 0 )
			{
				// The permission is blank ... it's unconditionally included.
				// But if it is the "BLANK" menu entry, then don't put two of
				// them in a row.
				if ( isLastOneBlank && menuEntry.equals("BLANK") )
					mayI = false;
				else
					mayI = true;
			}
			else if ( -1 != (logicalOpStart = permission.indexOf("&&"))
						|| -1 != (logicalOpStart = permission.indexOf("||")) )
			{
				// The permissions are a logical combination
				String strOp = permission.substring(logicalOpStart, logicalOpStart + 2);
				ArrayList listPermissions = new ArrayList();
				int start = 0;

				while ( logicalOpStart != -1 )
				{
					listPermissions.add(permission.substring(start, logicalOpStart).trim());
					start = logicalOpStart + 2;
					logicalOpStart = permission.indexOf(strOp, start);
				}
				listPermissions.add(permission.substring(start).trim());

				String[] arrayPermissions = new String[listPermissions.size()];
				arrayPermissions = (String[]) listPermissions.toArray(arrayPermissions);

				if ( strOp.equals("&&") )
					mayI = SecurityCache.hasPermissionAND(
							        CurrentUser.currentServletUser(req),
									arrayPermissions,
									acl,
									null, null, false);
				else
					mayI = SecurityCache.hasPermissionOR(
							CurrentUser.currentServletUser(req),
							arrayPermissions,
							acl,
							null, null, false);
			}
			else
			{
				// The permission is a single permission
				mayI = SecurityCache.hasPermission(CurrentUser.currentServletUser(req), permission, acl);
			}

			// If they are allowed to see this entry, add it to the html and reset strings
			if (mayI)
			{
				menuEntries.append(resources.getString("MENU_HTML_" + menuEntry));
				menuReset  .append(resources.getString("MENU_RESET_" + menuEntry));
				isLastOneBlank = menuEntry.equals("BLANK");
			}
		}

		// Send the menu
		out.println(menuStart + menuReset +menuMiddle + menuEntries + menuEnd);
	}

	private void flushCache(HttpServletRequest req, String cacheName, PrintWriter out)
	{
		try
		{
			ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
			if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S,
											SecurityCache.SYSTEM_ACL_NAME))
			{
				SystemSessionEJB systemSession =
				((SystemSessionEJBHome) ContextAdapter.getHome(ContextAdapter.SYSTEMSESSION)).create();
				systemSession.flushCache(cacheName);
				if (req.getParameter("manager") !=null)
					cacheManager(req, out);
				else
					cacheManage(req,out);
			}
			else
				out.println(MyResources.getString("ADMIN_BLANK"));
		}
		catch (Exception e)
		{
			m_Log.error("flushCache", "E_EXCEPTION", e);

		}
	}

	private void flushAllCache(HttpServletRequest req, PrintWriter out)
	{
		try
		{
			ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
			if (SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONMANAGEOBJECTS_S, SecurityCache.SYSTEM_ACL_NAME))
			{
				SystemSessionEJB systemSession = ((SystemSessionEJBHome) ContextAdapter.getHome(ContextAdapter.SYSTEMSESSION)).create();

				systemSession.flushAllCache();
				if (req.getParameter("manager") !=null)
					cacheManager(req, out);
				else
					out.println(MyResources.getString("CACHE_CLEARED"));
			}
			else
				out.println(MyResources.getString("ADMIN_BLANK"));
		}
		catch (Exception e)
		{
			m_Log.error("flushCache", "E_EXCEPTION", e);

		}
	}

	private void workCalendarList(HttpServletRequest req, PrintWriter out) throws Exception
	{
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
		// Expression support
		StringVarResolverHashMap eResolver = new StringVarResolverHashMap();
		StringVar eTable = new StringVar(eResolver);
		
		// Get Home Interface from the HTTP Session Object
		NBDSessionEJBHome NBDHome = (NBDSessionEJBHome) ContextAdapter.getHome(ContextAdapter.NBDSESSION);

		NBDSessionEJB NBD_ejb = NBDHome.create();
		
		// get list of calendars from NBDSessionEJB
		ArrayList calendarArray = NBD_ejb.getWorkCalendarElements();
		if (calendarArray == null)
			throw new Exception("Unable to get work calendar list");

		int numCalendars = calendarArray.size();
  	    ResourceBundle MyWorkCalendarResources = ResourceBundle.getBundle("com.eistream.sonora.admin.CalendarNames", UserElement.getUserLocale(req));
		
		// load list of calendars
		StringBuffer calendarList = new StringBuffer(0);
		
		String sTitle = MyResources.getString("WC_CALENDAR_PROPS");
		String sDeleteWorkCalendar = MyResources.getString("WC_DELETE_CALENDAR");

		String calendarName = null;
		for (int i=0; i < numCalendars; i++)
		{
			// returns a WorkCalendarElement
			WorkCalendarElement wceName = (WorkCalendarElement)calendarArray.get(i);
			if (wceName == null)
				throw new Exception("Unable to get work calendar element");

			calendarName = wceName.getName();
			if (calendarName != null && calendarName.length() != 0)
			{
				// create WorkCalendar object from NBDSessionEJB
				WorkCalendar wcCalendar = NBD_ejb.getWorkCalendar(calendarName);
				if (wcCalendar == null)
					throw new Exception("Unable to get work calendar");

		        boolean hasPermission = SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONDELETE_S,
		        		wcCalendar.getAcl());

				String displayCalendarName = checkResourceCalendarName(calendarName, MyWorkCalendarResources);
                String encodedCalendar = Base64UrlSafe.encodeBytes(calendarName.getBytes("UTF-8"));
				String encodedCalendarName = URLEncoder.encode(encodedCalendar,"UTF-8");

				if (calendarName.compareTo(WorkCalendar.SYSTEM_CALENDAR) == 0 || hasPermission == false)
				{
					// don't allow deleting of system calendar or read-only calendars
					calendarList.append("<tr class='lineshaded'><td></td>");
				}
				else
				{
					// put the calendar name in the table with post for deleting
					String sDeletePrompt = MyResources.getString("WC_DELETE_CALENDAR_PROMPT");
					String sDeleteDefinition = "<A title=\"" + sDeleteWorkCalendar + "\" href=\"javascript: if(window.confirm('" + sDeletePrompt + "'))(location.assign('Admin?op=d&wc=" + encodedCalendarName + "'))"; 
					sDeleteDefinition += "\"><img src=\"Images/act-del.gif\" border=\"0\"></A>";
					calendarList.append("<tr class='lineshaded'><td>" + sDeleteDefinition + "</td>");
				}

				calendarList.append("<td><A title=\"" + sTitle);
				calendarList.append("\" href = \"Admin?op=P&wc=" + encodedCalendarName);
				calendarList.append("\"><img src=\"Images/properties.gif\" border=\"0\">");
				calendarList.append(HtmlUtil.toSafeHtml(displayCalendarName) + "</a></td>");
				
				// set the inherited week in the table
				String inheritWorkWeekName = wcCalendar.getInheritedWeek();
				if (inheritWorkWeekName == null)
					inheritWorkWeekName = "";
				else
					inheritWorkWeekName = checkResourceCalendarName(inheritWorkWeekName, MyWorkCalendarResources);
				calendarList.append("<td valign=\"bottom\">" + HtmlUtil.toSafeHtml(inheritWorkWeekName) + "</td>");
				
				// set the inherited exceptions (holidays) in the table
				String inheritExceptionName = wcCalendar.getInheritedExceptions();
				if (inheritExceptionName == null)
					inheritExceptionName = "";
				else
					inheritExceptionName = checkResourceCalendarName(inheritExceptionName, MyWorkCalendarResources);
				calendarList.append("<td valign=\"bottom\">" + HtmlUtil.toSafeHtml(inheritExceptionName) + "</td>");

            	calendarList.append("<td></td></tr>");
			}
		} // end for

		// set table rows in HTML
		eResolver.setVariable("calendarNames", calendarList.toString());

		out.println(eTable.resolveNames(MyResources.getString("WORKCALENDAR_LIST")));
		out.close();
	}

	private String checkResourceCalendarName(String calendarName, ResourceBundle MyWorkCalendarResources)
	{
		// see if calendar is defined in resource file and if so use resource name
		if (calendarName.startsWith("@"))
		{
	        String sName = MyWorkCalendarResources.getString(calendarName);
	        if (sName != null)
	        	return sName;
		}
		return calendarName;
	}

	private void deleteWorkCalendar(HttpServletRequest req, PrintWriter out) throws Exception
	{
		// Get Home Interface from the HTTP Session Object
		NBDSessionEJBHome NBDHome = (NBDSessionEJBHome) ContextAdapter.getHome(ContextAdapter.NBDSESSION);
		NBDSessionEJB NBD_ejb = NBDHome.create();

		// get work calendar name
		String workCalendarName = UtilRequest.getParameter(req, "wc");	// work calendar name
		if (workCalendarName != null && workCalendarName.length() != 0)
		{
            byte[] byteCalendarName = Base64UrlSafe.decode(workCalendarName);
            String decodedCalendarName = new String(byteCalendarName, "UTF-8");
			NBD_ejb.removeWorkCalendar(decodedCalendarName);
				
			CacheSend flushcache = new CacheSend();
			flushcache.sendCommand(CacheConstants.NBDCACHE);
		}
	}

	private void workCalendarProperties(HttpServletRequest req, PrintWriter out) throws Exception
	{
		Locale userLocale = UserElement.getUserLocale(req);
		
        TimeZone userTz = UtilRequest.getTimeZone(req);
		
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", userLocale);
		
		// Expression support
		StringVarResolverHashMap eResolver = new StringVarResolverHashMap();
		StringVar eTable = new StringVar(eResolver);
		
		// Get Home Interface from the HTTP Session Object
		NBDSessionEJBHome NBDHome = (NBDSessionEJBHome) ContextAdapter.getHome(ContextAdapter.NBDSESSION);
		NBDSessionEJB NBD_ejb = NBDHome.create();

		// get work calendar name
		String workCalendarName = UtilRequest.getParameter(req, "wc");	// work calendar name

		WorkCalendar wcCalendar;
		if (workCalendarName == null || workCalendarName.length() == 0)
		{
			// none specified so create new calendar
			wcCalendar = new WorkCalendar();
			wcCalendar.setName("");
		}
		else
		{
			// create WorkCalendar object from NBDSessionEJB
            byte[] byteCalendarName = Base64UrlSafe.decode(workCalendarName);
            String decodedCalendarName = new String(byteCalendarName, "UTF-8");
			wcCalendar = NBD_ejb.getWorkCalendar(decodedCalendarName);
		}

        boolean hasPermission = SecurityCache.hasPermission(CurrentUser.currentServletUser(req), SecurityCache.SECURITY_PERMISSIONDELETE_S,
        		wcCalendar.getAcl());
    	// set param for javascript function to make the form readonly if necessary
        if (!hasPermission)
        	eResolver.setVariable("display", "true");
        else
        	eResolver.setVariable("display", "false");

		// set work calendar name in the HTML
		String wcName = wcCalendar.getName();
		eResolver.setVariable("calendarName", HtmlUtil.toSafeHtml(wcName));

		// put work calendar name in hidden field in case the calendar is saved to a new name
		eResolver.setVariable("oldCalendarName", HtmlUtil.toSafeHtml(wcName));
		
		// make strings used in javascript available as hidden fields in HTML
  	    String sThisCalendar = MyResources.getString("WC_THISCALENDAR");
		eResolver.setVariable("stringThisCalendar", sThisCalendar);
  	    String sInherited = MyResources.getString("WC_INHERITED");
		eResolver.setVariable("stringInherited", sInherited);
  	    String sDelete = MyResources.getString("WC_DELETE");
		eResolver.setVariable("stringDelete", sDelete);
  	    String sWorkday = MyResources.getString("WC_WORKDAY");
		eResolver.setVariable("stringWorkday", sWorkday);
  	    String sNonWorkday = MyResources.getString("WC_NON_WORKDAY");
		eResolver.setVariable("stringNonWorkday", sNonWorkday);
  	    String sNoEmptyExpr = MyResources.getString("WC_EXPR_CANNOT_BE_EMPTY");
		eResolver.setVariable("stringNoEmptyExpression", sNoEmptyExpr);
  	    String sNoEmptyDate = MyResources.getString("WC_DATE_CANNOT_BE_EMPTY");
		eResolver.setVariable("stringNoEmptyDate", sNoEmptyDate);
  	    String sNoEmptyCalendarName = MyResources.getString("WC_NAME_CANNOT_BE_EMPTY");
		eResolver.setVariable("stringNoEmptyCalendarName", sNoEmptyCalendarName);
	    String sExceptionProperties = MyResources.getString("WC_EXCEPTION_PROPERTIES");
		eResolver.setVariable("stringExceptionProps", sExceptionProperties);

		// save date format spec in HTML so javascript routines can validate dates
		String dateFormatSpec = TimestampUtil.getPattern(TimestampUtil.DATE_FORMAT, userLocale);
		eResolver.setVariable("stringDateFormatSpec", dateFormatSpec);
		String dateFormatLabel = TimestampUtil.getPatternLabel(TimestampUtil.DATE_FORMAT, userLocale);
		eResolver.setVariable("stringDateFormatLabel", dateFormatLabel);
		
		// set work calendar application in HTML
		String wcApplication = wcCalendar.getApplicationName();
		if (wcApplication == null)
			wcApplication = "";
		eResolver.setVariable("applicationName", HtmlUtil.toSafeHtml(wcApplication));

  	    String sNone = MyResources.getString("WC_NONE");

  	    // get locale list from users servlet resource
  	    ResourceBundle MyUserResources = ResourceBundle.getBundle("com.eistream.sonora.users.UsersServletResource", userLocale);
        StringBuffer szPreferredLocale_Options = new StringBuffer(0);
        // add the none option
        szPreferredLocale_Options.append("<OPTION VALUE=\"" + sNone + "\">" + sNone + "</OPTION>");
        szPreferredLocale_Options.append(MyUserResources.getString("PREFERRED_LOCALE"));

        String allPreferredValues = szPreferredLocale_Options.toString();
        // get locale from WorkCalendar if there
        String sLocale = wcCalendar.getLocale();
        if (sLocale == null)
			sLocale = sNone;
 
        // set the preferred Locale default
        int preferredLocaleIndex = allPreferredValues.indexOf("\"" + sLocale + "\"");
        if (preferredLocaleIndex != -1)
        {
            preferredLocaleIndex += sLocale.length()+1;
            try
            {
                 szPreferredLocale_Options.insert(preferredLocaleIndex+1, " SELECTED ");
            }
            catch(Exception e)
            {
            }
        }
        eResolver.setVariable("PreferredLocaleOptions", szPreferredLocale_Options);

		// set time zone
  	    StringBuffer szTimeZone_Options;
  	    
        // Process Time Zone Options String
		if ("1".equals(ContextAdapter.getAdapterValue("TIMEZONE_RESOURCE")))
		{
		    szTimeZone_Options = new StringBuffer(MyUserResources.getString("USER_TIMEZONE"));
		    String userTimeZone = wcCalendar.getTimeZone();
		    if (userTimeZone != null)
		    {
		    	// try and match with an entry in resource file and select it if found
		    	int pos = szTimeZone_Options.indexOf(userTimeZone);
		    	if (pos != -1)
		    		szTimeZone_Options = szTimeZone_Options.insert(pos-1, " selected");
		    }
		}
        else
        {
        	String selectedTz = wcCalendar.getTimeZone();
        	if ( selectedTz == null || selectedTz.length() == 0 )
        		selectedTz = sNone;
        	
        	szTimeZone_Options = HtmlUtil.buildTimeZoneOptions(sNone, selectedTz);
        }
        
        // set the Time Zone drop down in the HTML
        eResolver.setVariable("TimeZoneOptions", szTimeZone_Options);

        // get Acl from WorkCalendar and pass into AclProperty constructor
		AclProperty aclProp = new AclProperty(wcCalendar.getAcl());
		String inputAttributes = " tabindex=\"2\"";	// do we need more attributes then this?
		String sACLHtml = aclProp.toHtmlEditField(inputAttributes, userLocale, userTz);
        eResolver.setVariable("calendarACL", sACLHtml);
		
        String inheritedWeek = wcCalendar.getInheritedWeek();
		if (inheritedWeek == null || inheritedWeek.length() == 0)
		{
			// disable 'Inherit work week' and enable 'Define work week'
	        eResolver.setVariable("inheritchecked", "");
	        eResolver.setVariable("defineWeekchecked", "checked");
	        
	    	// set param for javascript function to enable and disable work week radio buttons
	        eResolver.setVariable("inheritOn", "false");
		}
		else
		{
			// enable 'Inherit work week' and disable 'Define work week'
	        eResolver.setVariable("inheritchecked", "checked");
	        eResolver.setVariable("defineWeekchecked", "");

	    	// set param for javascript function to enable or disable work week radio buttons
	        eResolver.setVariable("inheritOn", "true");
		}

  	    ResourceBundle MyWorkCalendarResources = ResourceBundle.getBundle("com.eistream.sonora.admin.CalendarNames", userLocale);

		// load the calendar in the "Inherit work week from" drop down list
		loadCalendarList(wcName, NBD_ejb, "inheritList", eResolver, false, inheritedWeek, MyResources, MyWorkCalendarResources);

		// set the work days and work times
		setWorkDayandWorkTimes(wcCalendar, NBD_ejb, eResolver, userLocale, MyResources);

        String inheritedExceptions = wcCalendar.getInheritedExceptions();
        
	    // load the "Inherit exceptions from" drop down list
		loadCalendarList(wcName, NBD_ejb, "hinheritfrom", eResolver, true, inheritedExceptions, MyResources, MyWorkCalendarResources);

        // set the example of a specific date in the HTML
	    eResolver.setVariable("specificDatePattern", dateFormatLabel);

	    // get all the recurring dates
		ResourceBundle myRecurringResources = ResourceBundle.getBundle("com.eistream.sonora.admin.CalendarRecurringDates", userLocale);
		String sRecurringDates = myRecurringResources.getString("WC_RECURRING_DATES");
		
  	    StringBuffer szRecurringDates = new StringBuffer(0);
  	    
  	    String[] arrayDates = sRecurringDates.split("\n");
  	    int beginIndex = 0;
  	    while ( beginIndex + 1 < arrayDates.length )
  	    {
            // select 1st entry
            if (beginIndex == 0)
            	szRecurringDates.append("<OPTION value=\"" + arrayDates[beginIndex + 1] + "\" selected>" + arrayDates[beginIndex] + "</OPTION>");
            else
            	szRecurringDates.append("<OPTION value=\"" + arrayDates[beginIndex + 1] + "\">" + arrayDates[beginIndex] + "</OPTION>");
  	    	
            beginIndex += 3;
  	    }
	    
        // set the recurring date in the HTML
		eResolver.setVariable("recurring", szRecurringDates);
		    
		// fill exceptions table
		loadExceptionsTable(wcCalendar, NBD_ejb, userLocale, eResolver, MyResources, !hasPermission);

		// Initial HTML for calculated exceptions table
		String sCalculatedExceptions = "<tbody>";

   		// set the patterns for getting day and month
		IFactory calFactory = CalendarFactory.getIFactory(wcCalendar.getLocaleInstance());
		ISimpleDateFormat dayFormat = calFactory.getISimpleDateFormatInstance("EEEEEEEEE", userLocale);
	   	ISimpleDateFormat yearFormat = calFactory.getISimpleDateFormatInstance("yyyy", userLocale);
		IDateFormat monthFormat = calFactory.getIDateFormatDateInstance(DateFormat.LONG, userLocale);

	   	// variable used to display the years in the table
	   	String sDisplayYear = "";
	   	
		// set the numbers of years for calculated exception range (7 years)
 		ICalendar start = calFactory.getICalendarInstance();
		ICalendar end = calFactory.getICalendarInstance();
		String curYear = yearFormat.format(start.getTime());
	    Integer iCurYear = new Integer(curYear);
		start.set(iCurYear.intValue(), 0, 1);	// Month is 0-based, 0 for January
		end.set(iCurYear.intValue()+6, 11, 31);	// 7 year range

		// get calculated exceptions for the specified date range
		ArrayList list = wcCalendar.getExceptions().getCalculatedExceptions(start, end);
		if (list != null)
		{
			for ( int index = 0; index < list.size(); ++index)
			{
				WCCalculatedExcept ce = (WCCalculatedExcept)list.get(index);
				
   				TimeZone tz = ce.getCalendar().getJavaTimeZone();
				// get a date object for this exception
				Date exceptionDate = ce.getCalendar().getTime();
				
				// set the time zone before getting info
				dayFormat.setTimeZone(tz);
				monthFormat.setTimeZone(tz);
				yearFormat.setTimeZone(tz);

				// get formatted day, month and year
				String sDay = dayFormat.format(exceptionDate);
				String sMonth = monthFormat.format(exceptionDate);
				String sYear = yearFormat.format(exceptionDate);
				
				// add the year row just once for each year of exceptions
				if (sDisplayYear.compareTo(sYear) != 0)
				{
					sDisplayYear = sYear;
     				sCalculatedExceptions += "<tr><td><b>" + sDisplayYear + "</b></td><td></td><td></td></tr>";
     			}
				
				// add the exception data as part of the HTML
 				sCalculatedExceptions += "<tr><td>" + sDay + "&nbsp;&nbsp;&nbsp;</td><td>" + sMonth + "&nbsp;&nbsp;&nbsp;</td>";
				sCalculatedExceptions +="<td>" + HtmlUtil.toSafeHtml(ce.getExcept().getDescription()) + "</td></tr>";
			} // end for
		}

       	sCalculatedExceptions += "</tbody>";
		eResolver.setVariable("calculatedExceptions", sCalculatedExceptions);
		
		// generate final HTML
		out.println(eTable.resolveNames(MyResources.getString("WORKCALENDAR_PROPERTIES")));
		out.close();
	}

	private boolean workCalendarUpdate(HttpServletRequest req, PrintWriter out) throws Exception
	{
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));

		// Get Home Interface from the HTTP Session Object
		NBDSessionEJBHome NBDHome = (NBDSessionEJBHome) ContextAdapter.getHome(ContextAdapter.NBDSESSION);
		NBDSessionEJB NBD_ejb = NBDHome.create();

		WorkCalendar wcCalendar = null;
		boolean bNewCalendar = false;
			
		// get calendar name from user
		String sCalendarName = UtilRequest.getParameter(req, "calName");

		// get original work calendar name in case they changed the name
		String workOldCalendarName = UtilRequest.getParameter(req, "oldCalendar");	// work calendar name

		// if no old calendar name then we are creating a new one
		if (workOldCalendarName.compareTo("") == 0)
		{
			WorkCalendar wcExistingCalendar = null;
			try
			{
				// make sure new calendar name does not already exist
				wcExistingCalendar = NBD_ejb.getWorkCalendar(sCalendarName);
			}
			catch (Exception err)
			{
			}
			
			if (wcExistingCalendar != null)
			{
				throw new CalendarAlreadyExistsException(sCalendarName);
			}
			
			bNewCalendar = true;
			wcCalendar = new WorkCalendar();
		}
		else
		{
			// make sure old one exists
			WorkCalendar wcExistingCalendar = NBD_ejb.getWorkCalendar(workOldCalendarName);
					
			// get a mutable instance of the calendar so we can make changes
			wcCalendar = wcExistingCalendar.getMutableInstance();
			if (wcCalendar == null)
				throw new Exception("Unable to get mutable instance of a work calendar");
		}

		// set the calendar name - it could be a new one
		wcCalendar.setName(sCalendarName);

		// get application name - might not need this
			
		// get time zone
		String sTimeZone = UtilRequest.getParameter(req, "TimeZone");
  	    String sNone = MyResources.getString("WC_NONE");
  	    if (sTimeZone.compareTo(sNone) == 0)
			wcCalendar.setTimeZone(null);
  	    else
  	    	wcCalendar.setTimeZone(sTimeZone);
			
		String sPreferredLocale = UtilRequest.getParameter(req, "PreferredLocale");
		Locale locale = UserElement.getUserLocale(req);
		if (sPreferredLocale.compareTo(sNone) == 0)
			wcCalendar.setLocale(null);
		else
			wcCalendar.setLocale(sPreferredLocale);

		// set the security
		AclProperty aclProp = new AclProperty(wcCalendar.getAcl());
		aclProp.fromHttp(req, locale, wcCalendar.getTimeZoneInstance());
		wcCalendar.setAcl((String)aclProp.getValue());

  	    String sRangeSeparator = MyResources.getString("WC_RANGE_SEPARATOR");
  	    String sListSeparator = MyResources.getString("WC_LIST_SEAPATOR");

		String sInheritedWorkWeekCalendar = "";		// default to no inherited work week
		// see if user has inherited a work week
		String sInheritWorkWeek = UtilRequest.getParameter(req, "wwinherit");
		if (sInheritWorkWeek != null && sInheritWorkWeek.compareTo("yes") == 0)
		{
			sInheritedWorkWeekCalendar = UtilRequest.getParameter(req, "wwinheritfrom");
			wcCalendar.setInheritedWeek(sInheritedWorkWeekCalendar);
		}
		else
		{
			wcCalendar.setInheritedWeek(null);

			// specify formats for work day delimters
		    WCWeek workCalendarWeek = wcCalendar.getWeek();

	  	    try
	  	    {
		  	    // add work days
				WCDay daySunday = workCalendarWeek.getDay(Calendar.SUNDAY);
				String sSundayCheckBox = UtilRequest.getParameter(req, "Sunday");
				if (sSundayCheckBox != null && sSundayCheckBox.compareTo("ON") == 0)
					daySunday.setWorkDay(true);
				else
					// set to not a work day
					daySunday.setWorkDay(false);
					
				// set work time if value entered
				String sSundayWorkTime = UtilRequest.getParameter(req, "SundayHours");
				if (sSundayWorkTime != null && sSundayWorkTime.length() > 0)
					daySunday.setWorkTimeParse(sSundayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					daySunday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
					
				WCDay dayMonday = workCalendarWeek.getDay(Calendar.MONDAY);
				String sMondayCheckBox = UtilRequest.getParameter(req, "Monday");
				if (sMondayCheckBox != null && sMondayCheckBox.compareTo("ON") == 0)
					dayMonday.setWorkDay(true);
				else
					dayMonday.setWorkDay(false);
		    		
				String sMondayWorkTime = UtilRequest.getParameter(req, "MondayHours");
				if (sMondayWorkTime != null && sMondayWorkTime.length() > 0)
					dayMonday.setWorkTimeParse(sMondayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					dayMonday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
					
				WCDay dayTuesday = workCalendarWeek.getDay(Calendar.TUESDAY);
				String sTuesdayCheckBox = UtilRequest.getParameter(req, "Tuesday");
				if (sTuesdayCheckBox != null && sTuesdayCheckBox.compareTo("ON") == 0)
					dayTuesday.setWorkDay(true);
				else
					dayTuesday.setWorkDay(false);
	
				String sTuesdayWorkTime = UtilRequest.getParameter(req, "TuesdayHours");
				if (sTuesdayWorkTime != null && sTuesdayWorkTime.length() > 0)
					dayTuesday.setWorkTimeParse(sTuesdayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					dayTuesday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
					
				WCDay dayWednesday = workCalendarWeek.getDay(Calendar.WEDNESDAY);
				String sWednesdayCheckBox = UtilRequest.getParameter(req, "Wednesday");
				if (sWednesdayCheckBox != null && sWednesdayCheckBox.compareTo("ON") == 0)
					dayWednesday.setWorkDay(true);
				else
					dayWednesday.setWorkDay(false);
	
				String sWednesdayWorkTime = UtilRequest.getParameter(req, "WednesdayHours");
				if (sWednesdayWorkTime != null && sWednesdayWorkTime.length() > 0)
					dayWednesday.setWorkTimeParse(sWednesdayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					dayWednesday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
	
				WCDay dayThursday = workCalendarWeek.getDay(Calendar.THURSDAY);
				String sThursdayCheckBox = UtilRequest.getParameter(req, "Thursday");
				if (sThursdayCheckBox != null && sThursdayCheckBox.compareTo("ON") == 0)
					dayThursday.setWorkDay(true);
				else
					dayThursday.setWorkDay(false);
					
				String sThursdayWorkTime = UtilRequest.getParameter(req, "ThursdayHours");
				if (sThursdayWorkTime != null && sThursdayWorkTime.length() > 0)
					dayThursday.setWorkTimeParse(sThursdayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					dayThursday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
	
				WCDay dayFriday = workCalendarWeek.getDay(Calendar.FRIDAY);
				String sFridayCheckBox = UtilRequest.getParameter(req, "Friday");
				if (sFridayCheckBox != null && sFridayCheckBox.compareTo("ON") == 0)
					dayFriday.setWorkDay(true);
				else
					dayFriday.setWorkDay(false);
	
				String sFridayWorkTime = UtilRequest.getParameter(req, "FridayHours");
				if (sFridayWorkTime != null && sFridayWorkTime.length() > 0)
					dayFriday.setWorkTimeParse(sFridayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					dayFriday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
	
				WCDay daySaturday = workCalendarWeek.getDay(Calendar.SATURDAY);
				String sSaturdayCheckBox = UtilRequest.getParameter(req, "Saturday");
				if (sSaturdayCheckBox != null && sSaturdayCheckBox.compareTo("ON") == 0)
					daySaturday.setWorkDay(true);
				else
					daySaturday.setWorkDay(false);
					
				String sSaturdayWorkTime = UtilRequest.getParameter(req, "SaturdayHours");
				if (sSaturdayWorkTime != null && sSaturdayWorkTime.length() > 0)
					daySaturday.setWorkTimeParse(sSaturdayWorkTime, locale, sRangeSeparator, sListSeparator);
				else
					daySaturday.setWorkTimeParse("", locale, sRangeSeparator, sListSeparator);
	  	    }
	  	    catch (Exception err)
	  	    {
	  	    	throw new Exception("Invalid work time specified");
	  	    }
		}

		// see if user has inherited exceptions
		String sInheritedExceptions = UtilRequest.getParameter(req, "hinheritfrom");
		if (sInheritedExceptions.compareTo("") != 0)
			wcCalendar.setInheritedExceptions(sInheritedExceptions);
		else
			wcCalendar.setInheritedExceptions(null);

		// always rebuild the entire set of exceptions
		WCExceptions wcExceptions = wcCalendar.getExceptions();
		wcExceptions.clear();

		String [] arrExceptionData  = UtilRequest.getParameterValues(req, "exceptionData");
		if (arrExceptionData != null)
		{
			IFactory calFactory = CalendarFactory.getIFactory(wcCalendar.getLocaleInstance());
			String formatSpec = TimestampUtil.getFormatSpecFromPattern(TimestampUtil.DATE_FORMAT, locale, false);				
	   		ISimpleDateFormat dayFormat = calFactory.getISimpleDateFormatInstance(formatSpec, locale);
			dayFormat.setTimeZone(wcCalendar.getTimeZoneInstance());
			dayFormat.setLenient(false);

			// iterate thru all the items - the source is a hidden multiple select comobo box 
			for (int i=0;i<arrExceptionData.length;i++)
			{
				// this is in XML
				String sExceptionItem = arrExceptionData[i];
				
				// encode the description before the DOM parses it
				int startPos = sExceptionItem.indexOf("Description='");
				if (startPos != -1)
				{
					int endPos = sExceptionItem.indexOf("Info='");
					if (endPos != -1)
					{
						String sEndString = sExceptionItem.substring(endPos);
						String sDescription = sExceptionItem.substring(startPos+13, endPos-2);
						String sNewDescription = XmlUtil.encodeString(sDescription, true);
						sExceptionItem = sExceptionItem.substring(0, startPos) + "Description='" + sNewDescription + "' " + sEndString;

						startPos = sExceptionItem.indexOf("Info='",endPos);
						endPos = sExceptionItem.indexOf("Type='",startPos);
						if (endPos != -1)
						{
							sEndString = sExceptionItem.substring(endPos);
							String sInfo = sExceptionItem.substring(startPos+6, endPos-2);
							String sNewInfo = XmlUtil.encodeString(sInfo, true);
							sExceptionItem = sExceptionItem.substring(0, startPos) + "Info='" + sNewInfo + "' " + sEndString;
						}				
					}
				}

				Document document;	
				try
				{
					// load the DOM
					DOMParser parser = new DOMParser();
					parser.parse(new InputSource(new BufferedReader(new StringReader(sExceptionItem))));
					document =  parser.getDocument();
				}
				catch (Exception err)
				{
					throw new Exception("Invalid exception XML");
				}

				NamedNodeMap exceptionItemAttributes;
				try
				{
					NodeList nodeListExceptions = document.getElementsByTagName("ExceptionItem");
					exceptionItemAttributes = nodeListExceptions.item(0).getAttributes();
				}
				catch (Exception err)
				{
					throw new Exception("XML for exceptions is missing exception items or attributes");
				}

				// get all the attributes - this is the exception data
				String sType;
				String sInfo;
				String sDescription;
				String sWorktime;
				boolean bWorkday = false;
				try
				{
					// get the description (or name) of the exception item
					Node nodeDescription = exceptionItemAttributes.getNamedItem("Description");
					sDescription = nodeDescription.getNodeValue();
						
					// get the expression or the specific date of the exception item
					Node nodeInfo = exceptionItemAttributes.getNamedItem("Info");
					sInfo = nodeInfo.getNodeValue();
						
					// get the type of the info - expression or specific date
					Node nodeType = exceptionItemAttributes.getNamedItem("Type");
					sType = nodeType.getNodeValue();
	
					// get the workday
					Node nodeWorkday = exceptionItemAttributes.getNamedItem("Workday");
					String sWorkday = nodeWorkday.getNodeValue();
					if (sWorkday.compareTo("1") == 0)
						bWorkday = true;
						
					// get the work time
					Node nodeWorktime = exceptionItemAttributes.getNamedItem("WorkTime");
					sWorktime = nodeWorktime.getNodeValue();
				}
				catch (Exception err)
				{
					throw new Exception("Missing exception attribute");
				}

	    		// add exception data based on type
				WCExcept exceptionDay;
				if (sType.compareTo("Expression") == 0)
		    		exceptionDay = new WCExcept(sInfo);
				else
		    		exceptionDay = new WCExcept(dayFormat.parse(sInfo));
					
	    		exceptionDay.setDescription(sDescription);
	    		WCDay day = new WCDay();
	    		if (sWorktime != null && sWorktime.length() != 0)
	    			day.setWorkTimeParse(sWorktime, locale, sRangeSeparator, sListSeparator);
				day.setWorkDay(bWorkday);
				exceptionDay.setDay(day);
	    		wcExceptions.add(exceptionDay);
			} // end for
		}
			
		// create new work calendar if specified otherwise save existing one
		if (bNewCalendar)
			NBD_ejb.createWorkCalendar(wcCalendar);
		else
			NBD_ejb.setWorkCalendar(wcCalendar);
			
		CacheSend flushcache = new CacheSend();
		flushcache.sendCommand(CacheConstants.NBDCACHE);
		return true;
	}

	private void loadCalendarList(String wcName, NBDSessionEJB NBD_ejb, String htmlElementName, StringVarResolverHashMap eResolver,
					boolean addNoneOption, String sSelected, ResourceBundle MyResources, ResourceBundle MyWorkCalendarResources) throws Exception
	{
		// load drop down with list of calendars - do not include your self
		StringBuffer calendarList = new StringBuffer(0);

		boolean foundSelected = false;
		// get list of calendars from NBDSessionEJB
		ArrayList calendarArray = NBD_ejb.getWorkCalendarElements();
		if (calendarArray != null)
		{
			int numCalendars = calendarArray.size();
			String calendarName = null;
			for (int i=0; i < numCalendars; i++)
			{
				// returns a WorkCalendarElement
				WorkCalendarElement wceName = (WorkCalendarElement)calendarArray.get(i);
				if (wceName == null)
					throw new Exception("Unable to get work calendar element");

				calendarName = wceName.getName();
				// don't add the current calendar to list
				if ( !wcName.equalsIgnoreCase(calendarName) )
				{
					String displayCalendarName = checkResourceCalendarName(calendarName, MyWorkCalendarResources);
	
					calendarList.append("<option value=\"");
					calendarList.append(HtmlUtil.toSafeHtml(calendarName) + "\"");
					if (sSelected != null && sSelected.length() != 0 && sSelected.compareTo(calendarName) == 0)
					{
			           	calendarList.append(" selected");
			           	foundSelected = true;
					}
	            	calendarList.append(">");
	            	calendarList.append(HtmlUtil.toSafeHtml(displayCalendarName) + "</option>");
				}
			} // end for
		}
		
		if (sSelected != null && sSelected.length() != 0 && !foundSelected)
		{
			String displayCalendarName = checkResourceCalendarName(sSelected, MyWorkCalendarResources);
			
			calendarList.append("<option value=\"");
			calendarList.append(HtmlUtil.toSafeHtml(sSelected)).append("\" selected>");
        	calendarList.append(HtmlUtil.toSafeHtml(displayCalendarName) + "</option>");
           	foundSelected = true;
		}

		// inherited exceptions drop down
		if (addNoneOption)
		{
	  	    String sNone = MyResources.getString("WC_NONE");
			calendarList.append("<option value=\"\"");
			if (foundSelected == false)
				calendarList.append(" selected");
        	calendarList.append(">");
        	calendarList.append(HtmlUtil.toSafeHtml(sNone) + "</option>");
		}
		
		// set value in HTML for inheritOn drop down list
	    eResolver.setVariable(htmlElementName, calendarList.toString());
	}
	
	private void setWorkDayandWorkTimes(WorkCalendar wcCalendar, NBDSessionEJB NBD_ejb, StringVarResolverHashMap eResolver, Locale locale, ResourceBundle MyResources) throws Exception
	{
		WCWeek workCalendarWeek = null;

		// get inherited work week calendar if specified
        String inheritedWeek = wcCalendar.getInheritedWeek();
        if (inheritedWeek != null && inheritedWeek.length() != 0)
        {
        	WorkCalendar wcInheritedCalendar;
        	try
        	{
        		// create WorkCalendar object from NBDSessionEJB
        		wcInheritedCalendar = NBD_ejb.getWorkCalendar(inheritedWeek);
         	}
        	catch (Exception err)
        	{
        		throw new Exception("Inherited work week calendar does not exist");
        	}
           	workCalendarWeek = wcInheritedCalendar.getWeek();
        }
        else
        {
        	workCalendarWeek = wcCalendar.getWeek();
        }

        if (workCalendarWeek == null)
        	throw new Exception("Unable to get work calendar week");

		// get time range separator and list separator
	    String rangeSeparator = MyResources.getString("WC_RANGE_SEPARATOR");
	    String listSeparator = MyResources.getString("WC_LIST_SEAPATOR");

        // set the example of a work time in the HTML. Use a U.S. example and convert to current locale
		WCDay day = new WCDay();
		WorkCalendar tempCalendar = new WorkCalendar();
		tempCalendar.setLocale("en-US");
		day.setWorkTimeParse("08:30-12:00,13:00-17:30");
		String exampleWorkTime = day.getWorkTimeString(locale, rangeSeparator, listSeparator);
	    eResolver.setVariable("exampleWorkTime", HtmlUtil.toSafeHtml(exampleWorkTime));

		// set Work Days and associated work times
	    for (int indexDay = 0; indexDay < 7; indexDay++)
	    {
	    	if (indexDay == 0)	// sunday - is there a define?
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.SUNDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
	    			eResolver.setVariable("sundayChecked", "checked");
	    		else
				    eResolver.setVariable("sundayChecked", "");

	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("sundayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("sundayWorkTime", "");
	    	}
	    	else if (indexDay == 1)		// monday
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.MONDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
				    eResolver.setVariable("mondayChecked", "checked");
	    		else
				    eResolver.setVariable("mondayChecked", "");
				    
	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
 	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("mondayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("mondayWorkTime", "");
	    	}
	    	else if (indexDay == 2)		// tuesday
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.TUESDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
				    eResolver.setVariable("tuesdayChecked", "checked");
	    		else
				    eResolver.setVariable("tuesdayChecked", "");
				    
	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
 	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("tuesdayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("tuesdayWorkTime", "");
	    	}
	    	else if (indexDay == 3)		// wednesday
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.WEDNESDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
				    eResolver.setVariable("wednesdayChecked", "checked");
	    		else
				    eResolver.setVariable("wednesdayChecked", "");
				    
	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
 	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("wednesdayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("wednesdayWorkTime", "");
	    	}		    		
	    	else if (indexDay == 4)		// thursday
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.THURSDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
				    eResolver.setVariable("thursdayChecked", "checked");
	    		else
				    eResolver.setVariable("thursdayChecked", "");
	    		
	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("thursdayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("thursdayWorkTime", "");
	    	}
	    	else if (indexDay == 5)		// friday
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.FRIDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
				    eResolver.setVariable("fridayChecked", "checked");
	    		else
				    eResolver.setVariable("fridayChecked", "");
				    
	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("fridayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("fridayWorkTime", "");
	    	}		    	
	    	else if (indexDay == 6)		// saturday
	    	{
	    		WCDay workCalendarDay = workCalendarWeek.getDay(Calendar.SATURDAY);
	    		if ( workCalendarDay.isWorkDay() == true)
	    			eResolver.setVariable("saturdayChecked", "checked");
	    		else
	    			eResolver.setVariable("saturdayChecked", "");
				    
	    		String sWorkTime = workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator);
	    		if (sWorkTime != null && sWorkTime.length() > 0)
	    			eResolver.setVariable("saturdayWorkTime", workCalendarDay.getWorkTimeString(locale, rangeSeparator, listSeparator));
	    		else
				    eResolver.setVariable("saturdayWorkTime", "");
	    	}		    
	    } // end for
	}

	private void loadExceptionsTable(WorkCalendar wcCalendar, NBDSessionEJB NBD_ejb, Locale locale, StringVarResolverHashMap eResolver, ResourceBundle MyResources, boolean display) throws Exception
	{
		StringBuffer szExceptionTableData = new StringBuffer(0);
		
		// load non-inherited exceptions first
		boolean inherited = false;
		
		// flags used to add 2 rows - 1 for main calendar exceptions ("This calendar:") and 
		// one for inherited calendar exceptions ("Inherited:")  
		boolean addedThisCalendar = false;
		boolean addedInherited = false;

		boolean moreExceptions = false;

		// string which holds hidden drop down list;
		String sExceptionDataItem = "";

	    String rangeSeparator = MyResources.getString("WC_RANGE_SEPARATOR");
	    String listSeparator = MyResources.getString("WC_LIST_SEAPATOR");

		// create temp calendar for getting inherited exceptions
		WorkCalendar currentCalendar = wcCalendar;
		
		// tableRowId is used to identify rows for deletion
		int tableRowId = 0;

	    String sThisCalendar = MyResources.getString("WC_THISCALENDAR");
	    String sInherited = MyResources.getString("WC_INHERITED");
	    String sDelete = MyResources.getString("WC_DELETE");
	    String sWorkDay = MyResources.getString("WC_WORKDAY");
	    String sNonWorkDay = MyResources.getString("WC_NON_WORKDAY");
	    String sExceptionProperties = MyResources.getString("WC_EXCEPTION_PROPERTIES");

		// get array list of exceptions
		WCExceptions wcExceptions = currentCalendar.getExceptions();
		if (wcExceptions != null)
			moreExceptions = true;
		
		while (moreExceptions)
		{
			// 	get num exceptions
			WCExcept[] arrayExceptions = wcExceptions.toArray();
			int numInheritedExc = 0;
			if (arrayExceptions != null)
				numInheritedExc = arrayExceptions.length;

			// put "This calendar:" in table at beginning only
			if (addedThisCalendar == false)
			{
				tableRowId++;
				szExceptionTableData.append("<tr><td><a ><img src=\"Images/blank.gif\" align=\"right\"></a></td>");
				szExceptionTableData.append("<td nowrap><a ><img src=\"Images/blank.gif\"></a><b>" + sThisCalendar + "</b></td>");
				szExceptionTableData.append("<td nowrap></td><td nowrap></td><td nowrap></td></tr>");
				addedThisCalendar = true;
			}
			else if (addedInherited == false)
			{
				tableRowId++;
				szExceptionTableData.append("<tr><td><a ><img src=\"Images/blank.gif\" align=\"right\"></a></td>");
				szExceptionTableData.append("<td nowrap><a ><img src=\"Images/blank.gif\"></a><b>" + sInherited + "</b></td>");
				szExceptionTableData.append("<td nowrap></td><td nowrap></td><td nowrap></td></tr>");
				addedInherited = true;
			}

			// fill table with exceptions
			for (int i = 0; i < numInheritedExc; i++)
			{
				szExceptionTableData.append("<tr>");
	
				// if not inherited and not read only then allow delete
				if (!inherited && !display)
				{
					String sTableRowId = "" + tableRowId++;
					// defined exceptions can be deleted so set onClick handler with table row id
					szExceptionTableData.append("<td><a href=\"javascript:removeException(" + sTableRowId + ")\" tabindex=\"53\" title=\"" + sDelete + "\"><img src=\"Images/act-del.gif\" align=\"right\"></a></td>");
					
					// defined exceptions can be edited so call javascript handler for editing
					szExceptionTableData.append("<td nowrap><a href=\"javascript:editException(" + sTableRowId + ")\" tabindex=\"53\" title=\"" + sExceptionProperties + "\"><img src=\"Images/properties.gif\"></a>" + HtmlUtil.toSafeHtml(arrayExceptions[i].getDescription()) + "&nbsp;&nbsp;&nbsp;</td>");
				}
				else
				{
					// inherited exceptions cannot be deleted so put a blank image in
					szExceptionTableData.append("<td><a ><img src=\"Images/blank.gif\" align=\"right\"></a></td>");
					
					szExceptionTableData.append("<td nowrap><a ><img src=\"Images/blank.gif\"></a>" + HtmlUtil.toSafeHtml(arrayExceptions[i].getDescription()) + "&nbsp;&nbsp;&nbsp;</td>");
				}
	  
				Date specificDate = arrayExceptions[i].getSpecificDate();
				String sSpecificDate = "";
				if (specificDate != null)
				{
					String formatSpec = TimestampUtil.getFormatSpecFromPattern(TimestampUtil.DATE_FORMAT, locale, false);				
			   		ISimpleDateFormat dayFormat = CalendarFactory.getISimpleDateFormatInstance(formatSpec, locale);
					dayFormat.setTimeZone(wcCalendar.getTimeZoneInstance());
					dayFormat.setLenient(false);
					sSpecificDate = dayFormat.format(specificDate);
					szExceptionTableData.append("<td nowrap valign=\"bottom\">" + HtmlUtil.toSafeHtml(sSpecificDate) + "&nbsp;&nbsp;&nbsp;</td>");
				}
				else
				{
					szExceptionTableData.append("<td nowrap valign=\"bottom\">" + HtmlUtil.toSafeHtml(arrayExceptions[i].getExpression()) + "&nbsp;&nbsp;&nbsp;</td>");
				}
				
		  	    String stringWorkDay = sNonWorkDay;
		  	    boolean bWorkDay = false;
				String sWorkTime = "";
				WCDay day = arrayExceptions[i].getDay();
				if (day != null)
				{
					if (day.isWorkDay())
					{
						stringWorkDay = sWorkDay;
						bWorkDay = true;
					}
					sWorkTime = day.getWorkTimeString(locale, rangeSeparator, listSeparator);
				}

				szExceptionTableData.append("<td nowrap valign=\"bottom\">" + stringWorkDay + "&nbsp;&nbsp;&nbsp;</td>");
				szExceptionTableData.append("<td nowrap valign=\"bottom\">" + sWorkTime + "</td>");
				szExceptionTableData.append("</tr>");

				// add exception data for non-inherited calendar into hidden drop down
				if (inherited == false)
				{
					// example - <ExceptionItem Description='Memorial Day' Info='date(y,6,1) - 1 mondays' Type='Expression' Workday='1' WorkTime='8:30 AM - 12:00 PM, 1:00 PM - 5:00 PM'></ExceptionItem>
					String xmlExceptionItem = "<ExceptionItem Description='" + HtmlUtil.toSafeHtml(arrayExceptions[i].getDescription()) + "'";
					if (specificDate != null)
						xmlExceptionItem += " Info='" + HtmlUtil.toSafeHtml(sSpecificDate) + "' Type='SpecificDate'";
					else
						xmlExceptionItem += " Info='" + HtmlUtil.toSafeHtml(arrayExceptions[i].getExpression()) + "' Type='Expression'";

					xmlExceptionItem += " Workday='";
					if (bWorkDay == true)
						xmlExceptionItem += "1'";
					else
						xmlExceptionItem += "0'";

					xmlExceptionItem += " WorkTime='" + sWorkTime + "'></ExceptionItem>";
					sExceptionDataItem += "<option value=\"" + xmlExceptionItem + "\"></option>";
				}
			} // end for
		
			// get inherited calendar to see if we need to add these into the table
			String  sCalendarInheritedExceptions = currentCalendar.getInheritedExceptions();
			if (sCalendarInheritedExceptions != null && sCalendarInheritedExceptions.length() > 0)
			{
				inherited = true;
				
				// create WorkCalendar object from NBDSessionEJB
				currentCalendar = NBD_ejb.getWorkCalendar(sCalendarInheritedExceptions);
				
	    		// get the inherited exceptions
				wcExceptions = currentCalendar.getExceptions();
			}
			else
				moreExceptions = false;
		}  // end while
	
       // set the rows for the exceptions table in the HTML
		if (wcExceptions != null)
			eResolver.setVariable("rowExceptions", szExceptionTableData.toString());
		
		// set hidden drop down with xml exception data into HTML
		eResolver.setVariable("exceptionDataItems", sExceptionDataItem);
 	}

	public void cacheManage(HttpServletRequest req, PrintWriter out)
	{
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
		try
		{
			StringVarResolverHashMap eResolver = new StringVarResolverHashMap();
			StringVar eTable = new StringVar(eResolver);
			StringBuffer sKeys = new StringBuffer();
			// only display values if param is set
			boolean bValues = false;
			if (UtilRequest.getParameter(req, "Values") !=null)
				bValues=true;
			eResolver.setVariable("UserCacheLength",String.valueOf(UserCache.getCacheLength()));
			Object[] keys;
			if (bValues)
			{
				keys = UserCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("UserCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("ACLCacheLength",String.valueOf(SecurityCache.getCacheLength()));
			if (bValues)
			{
				keys = SecurityCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("ACLCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("RoleDefCacheLength",String.valueOf(RoleDefCache.getCacheLength()));
			if (bValues)
			{
				keys = RoleDefCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("RoleDefCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("FieldCacheLength",String.valueOf(FmsCache.getFieldsCacheLength()));
			if (bValues)
			{
				keys = FmsCache.getFieldsCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("FieldCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("FormCacheLength",String.valueOf(FormsCache.getCacheLength()));
			if (bValues)
			{
				keys = FormsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("FormCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("RepositoryCacheLength",String.valueOf(RepositoryCache.getCacheLength()));
			if (bValues)
			{
				keys = RepositoryCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("RepositoryCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("LicenseCacheLength",String.valueOf(LicenseCache.getCacheLength()));
			if (bValues)
			{
				keys = LicenseCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("LicenseCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("MetricCacheLength",String.valueOf(StatisticsCache.getCacheLength()));
			if (bValues)
			{
				keys = StatisticsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("MetricCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("TiffCacheLength",String.valueOf(TiffTocCache.getCacheLength()));
			if (bValues)
			{
				keys = TiffTocCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("TiffCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("SystemCacheLength",String.valueOf(CacheRegister.getCacheLength()));
			if (bValues)
			{
				keys = CacheRegister.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("SystemCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("WorkflowCacheLength",String.valueOf(WorkflowsCache.getCacheLength()));
			if (bValues)
			{
				keys = WorkflowsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("WorkflowCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("TemplatesCacheLength",String.valueOf(TemplatesCache.getCacheLength()));
			if (bValues)
			{
				keys = TemplatesCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("TemplatesCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("RolesCacheLength",String.valueOf(SecurityCache.getRolesCacheLength()));
			if (bValues)
			{
				keys = SecurityCache.getRolesCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("RolesCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("PoolCacheLength",String.valueOf(StorageManagerCache.getPoolCacheLength()));
			if (bValues)
			{
				keys = StorageManagerCache.getPoolCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("PoolCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("DataManagerCacheLength",String.valueOf(StorageManagerCache.getDataManagerCacheLength()));
			if (bValues)
			{
				keys = StorageManagerCache.getDataManagerCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("DataManagerCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("GetNextCacheLength",String.valueOf(GetNextController.getCacheLength()));
			if (bValues)
			{
				keys = GetNextController.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("GetNextCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("TemporaryFileCacheLength", "" + TemporaryFileCache.length());
			if (bValues)
			{
				keys = TemporaryFileCache.getKeys();
				int count = keys.length;
				for (int ii = 0; ii < count; ii++)
				{
					sKeys.append(keys[ii].toString()+ "; ");
				}
			}
			eResolver.setVariable("TemporaryFileCacheString", sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("NBDCacheLength",String.valueOf(WorkCalendarCache.getCacheLength()));
			if (bValues)
			{
				keys = WorkCalendarCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("NBDCacheString",sKeys.toString());
			sKeys.setLength(0);

			
			eResolver.setVariable("AppPropCacheLength",String.valueOf(SystemApplicationProperties.getCacheLength()));
			if (bValues)
			{
				keys = SystemApplicationProperties.getCacheKeys();
				if (keys != null)
				{
					for (int i = 0; i < keys.length; i++)
					{
						sKeys.append(keys[i].toString()+"; ");
					}
					eResolver.setVariable("ApplicationPropertyString",sKeys.toString());
				}
				else
				{
					eResolver.setVariable("ApplicationPropertyString","");
				}
			}
			sKeys.setLength(0);
			
			
			eResolver.setVariable("FmsRowCacheSize", String.valueOf(TemplatesCache.getRowCacheSize()));
			if (bValues)
			{
				keys = TemplatesCache.getRowCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("FMSRowCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("FieldFormatsCacheSize", String.valueOf(FieldFormatsCache.getCacheSize()));
			if (bValues)
			{
				keys = FieldFormatsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("FieldFormatsCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("ExpressionCacheSize", String.valueOf(ExpressionCache.getCacheLength()));
			if (bValues)
			{
				keys = ExpressionCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("ExpressionCacheString",sKeys.toString());
			sKeys.setLength(0);
			
			eResolver.setVariable("ScriptCacheLength",String.valueOf(ScriptCache.getCacheLength()));
			if (bValues)
			{
				keys = ScriptCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("ScriptCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("TransformerCacheSize", String.valueOf(TransformerCache.getCacheLength()));
			if (bValues)
			{
				keys = TransformerCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"; ");
				}
			}
			eResolver.setVariable("TransformerCacheString",sKeys.toString());
			sKeys.setLength(0); 
            
            ArrayList components = RecordsManagerCache.getRecordDefinitionList(null);
            if (components == null)
                components = new ArrayList();

            int count = components.size();
            eResolver.setVariable("RecordsManagerCacheSize", "" + count);
            if (bValues)
            {
                for (int ii = 0; ii < count; ii++)
                {
                    ComponentDefinition cd = (ComponentDefinition)components.get(ii);
                    sKeys.append(cd.getComponentName()).append(";");                        
                }
            }
            eResolver.setVariable("RecordsManagerCacheString",sKeys.toString());
            sKeys.setLength(0);
            
         
            eResolver.setVariable("ApplicationsCacheLength",String.valueOf(ApplicationsCache.getCacheLength()));
            if (bValues)
            {
                keys = ApplicationsCache.getCacheKeys();
                for (int i = 0; i < keys.length; i++)
                {
                    sKeys.append(keys[i].toString()+"; ");
                }
            }
            eResolver.setVariable("ApplicationsCacheString",sKeys.toString());
            sKeys.setLength(0);
            
 			
			out.println(eTable.resolveNames(MyResources.getString("CACHE_MANAGE")));
			out.close();
		}
		catch (Exception err)
		{
			// Log Error (fix this section later)
			String  MethodName = "CacheManage";
			m_Log.error(MethodName, "E_EXCEPTION", err);
		}
	}
	public void cacheManager(HttpServletRequest req, PrintWriter out)
	{
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
		try
		{
			StringVarResolverHashMap eResolver = new StringVarResolverHashMap();
			StringVar eTable = new StringVar(eResolver);
			StringBuffer sKeys = new StringBuffer();
			// only display values if param is set
			boolean bValues = true;
			eResolver.setVariable("UserCacheLength",String.valueOf(UserCache.getCacheLength()));
			Object[] keys;
			if (bValues)
			{
				keys = UserCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("UserCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("ACLCacheLength",String.valueOf(SecurityCache.getCacheLength()));
			if (bValues)
			{
				keys = SecurityCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("ACLCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("RoleDefCacheLength",String.valueOf(RoleDefCache.getCacheLength()));
			if (bValues)
			{
				keys = RoleDefCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("RoleDefCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("FieldCacheLength",String.valueOf(FmsCache.getFieldsCacheLength()));
			if (bValues)
			{
				keys = FmsCache.getFieldsCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("FieldCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("FormCacheLength",String.valueOf(FormsCache.getCacheLength()));
			if (bValues)
			{
				keys = FormsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					SonoraForm form = FormsCache.getForm(keys[i].toString());
					if (form.getRawString().length() > 0)
						sKeys.append(keys[i].toString()+"\n");
					else sKeys.append("(").append(keys[i].toString()).append(")\n");
				}
			}
			eResolver.setVariable("FormCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("RepositoryCacheLength",String.valueOf(RepositoryCache.getCacheLength()));
			if (bValues)
			{
				keys = RepositoryCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("RepositoryCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("LicenseCacheLength",String.valueOf(LicenseCache.getCacheLength()));
			if (bValues)
			{
				keys = LicenseCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("LicenseCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("MetricCacheLength",String.valueOf(StatisticsCache.getCacheLength()));
			if (bValues)
			{
				keys = StatisticsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("MetricCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("TiffCacheLength",String.valueOf(TiffTocCache.getCacheLength()));
			if (bValues)
			{
				keys = TiffTocCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("TiffCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("SystemCacheLength",String.valueOf(CacheRegister.getCacheLength()));
			if (bValues)
			{
				keys = CacheRegister.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("SystemCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("WorkflowCacheLength",String.valueOf(WorkflowsCache.getCacheLength()));
			if (bValues)
			{
				keys = WorkflowsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("WorkflowCacheString",sKeys.toString());
			sKeys.setLength(0);


			eResolver.setVariable("TemplatesCacheLength",String.valueOf(TemplatesCache.getCacheLength()));
			if (bValues)
			{
				keys = TemplatesCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("TemplatesCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("RolesCacheLength",String.valueOf(SecurityCache.getRolesCacheLength()));
			if (bValues)
			{
				keys = SecurityCache.getRolesCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+"\n");
				}
			}
			eResolver.setVariable("RolesCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("PoolCacheLength",String.valueOf(StorageManagerCache.getPoolCacheLength()));
			if (bValues)
			{
				keys = StorageManagerCache.getPoolCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					String poolId = (String) keys[i];
					String name = StorageManagerCache.getPoolElement(poolId).getName();
					sKeys.append(name + "\n");
				}
			}
			eResolver.setVariable("PoolCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("DataManagerCacheLength",String.valueOf(StorageManagerCache.getDataManagerCacheLength()));
			if (bValues)
			{
				keys = StorageManagerCache.getDataManagerCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					String dataManagerId = (String) keys[i];
					String name = StorageManagerCache.getDataManagerElement(dataManagerId).getName();
					sKeys.append(name + "\n");
				}
			}
			eResolver.setVariable("DataManagerCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("GetNextCacheLength",String.valueOf(GetNextController.getCacheLength()));
			if (bValues)
			{
				keys = GetNextController.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString() + "\n");
				}
			}
			eResolver.setVariable("GetNextCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("TemporaryFileCacheLength", "" + TemporaryFileCache.length());
			if (bValues)
			{
				keys = TemporaryFileCache.getKeys();
				int count = keys.length;
				for (int ii = 0; ii < count; ii++)
				{
					String name = (String) keys[ii];
					sKeys.append(name + "\n");
				}
			}
			eResolver.setVariable("TemporaryFileCacheString",sKeys.toString());
			sKeys.setLength(0);


			int len = FileExtensionMapCache.getSize();
			if (bValues)
			{
				if (len != 0)
				{
					keys = FileExtensionMapCache.getKeys();
					int count = keys.length;
					for (int i = 0; i < count; i++)
					{
						sKeys.append(FileExtensionMapCache.getExtensionMapsAsString((String) keys[i])).append("\n");
					}
				}
			}
			eResolver.setVariable("FileExtensionMapCacheLength", "" + len);
			eResolver.setVariable("FileExtensionMapCacheString",sKeys.toString());
			sKeys.setLength(0);


			len = FileExcludeListCache.getSize();
			if (bValues)
			{
				if (len != 0)
				{
					keys = FileExcludeListCache.getKeys();
					int count = keys.length;
					for (int i = 0; i < count; i++)
					{
						String ExcludeList = FileExcludeListCache.getExcludeList((String) keys[i]);
						sKeys.append((String) keys[i]).append(": ").append(ExcludeList).append("\n");
					}
				}
			}
			eResolver.setVariable("FileExcludeListCacheLength", "" + len);
			eResolver.setVariable("FileExcludeListCacheString",sKeys.toString());
			sKeys.setLength(0);


			eResolver.setVariable("NBDCacheLength",String.valueOf(WorkCalendarCache.getCacheLength()));
			if (bValues)
			{
				keys = WorkCalendarCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString() + "\n");
				}
			}
			eResolver.setVariable("NBDCacheString",sKeys.toString());
			sKeys.setLength(0);

			eResolver.setVariable("AppPropCacheLength",String.valueOf(SystemApplicationProperties.getCacheLength()));
			if (bValues)
			{
				keys = SystemApplicationProperties.getCacheKeys();
				if (keys != null)
				{
					for (int i = 0; i < keys.length; i++)
					{
						sKeys.append(keys[i].toString() + "\n");
					}
					eResolver.setVariable("ApplicationPropertyString",sKeys.toString());
				}
				else
				{
					eResolver.setVariable("ApplicationPropertyString","");
				}
			}
			sKeys.setLength(0);

			eResolver.setVariable("QueryCacheLength",String.valueOf(QueryCache.getCacheLength()));
			if (bValues)
			{
				keys = QueryCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString() + "\n");
				}
			}
			
			eResolver.setVariable("QueryCacheString",sKeys.toString());
			sKeys.setLength(0);
			
			
			eResolver.setVariable("FmsRowCacheSize", String.valueOf(TemplatesCache.getRowCacheSize()));
			if (bValues)
			{
				keys = TemplatesCache.getRowCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+ "\n");
				}
			}
			eResolver.setVariable("FMSRowCacheString",sKeys.toString());
			sKeys.setLength(0);
			
			eResolver.setVariable("FieldFormatsCacheSize", String.valueOf(FieldFormatsCache.getCacheSize()));
			if (bValues)
			{
				keys = FieldFormatsCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString()+ "\n");
				}
			}
			eResolver.setVariable("FieldFormatsCacheString",sKeys.toString());
			sKeys.setLength(0);

            eResolver.setVariable("MenuCacheLength",String.valueOf(MenuCache.getCacheLength()));
            if (bValues)
            {
                keys = MenuCache.getCacheKeys();
                for (int i = 0; i < keys.length; i++)
                {
                    sKeys.append(keys[i].toString() + "\n");
                }
            }
            eResolver.setVariable("MenuCacheString",sKeys.toString());
            sKeys.setLength(0);

            eResolver.setVariable("ExpressionCacheSize",String.valueOf(ExpressionCache.getCacheLength()));
            if (bValues)
            {
                keys = ExpressionCache.getCacheKeys();
                for (int i = 0; i < keys.length; i++)
                {
                    sKeys.append(keys[i].toString() + "\n");
                }
            }
            eResolver.setVariable("ExpressionCacheString",sKeys.toString());
            sKeys.setLength(0);

			eResolver.setVariable("ScriptCacheLength",String.valueOf(ScriptCache.getCacheLength()));
			if (bValues)
			{
				keys = ScriptCache.getCacheKeys();
				for (int i = 0; i < keys.length; i++)
				{
					sKeys.append(keys[i].toString() + "\n");
				}
			}
			eResolver.setVariable("ScriptCacheString",sKeys.toString());
			sKeys.setLength(0);

            eResolver.setVariable("TransformerCacheSize",String.valueOf(TransformerCache.getCacheLength()));
            if (bValues)
            {
                keys = TransformerCache.getCacheKeys();
                for (int i = 0; i < keys.length; i++)
                {
                    sKeys.append(keys[i].toString() + "\n");
                }
            }
            eResolver.setVariable("TransformerCacheString",sKeys.toString());
            sKeys.setLength(0);
            
            ArrayList components = RecordsManagerCache.getRecordDefinitionList(null);
            if (components == null)
                components = new ArrayList();

            int count = components.size();
            eResolver.setVariable("RecordsManagerCacheSize", "" + count);
            if (bValues)
            {
                for (int ii = 0; ii < count; ii++)
                {
                    ComponentDefinition cd = (ComponentDefinition)components.get(ii);
                    sKeys.append(cd.getComponentName()).append("\n");                        
                }
            }
            eResolver.setVariable("RecordsManagerCacheString",sKeys.toString());
            sKeys.setLength(0);
            

            eResolver.setVariable("ApplicationsCacheLength",String.valueOf(ApplicationsCache.getCacheLength()));
            if (bValues)
            {
                keys = ApplicationsCache.getCacheKeys();
                for (int i = 0; i < keys.length; i++)
                {
                    sKeys.append(keys[i].toString()).append("\n");
                }
            }            
            eResolver.setVariable("ApplicationsCacheString",sKeys.toString());
            sKeys.setLength(0);

			out.println(eTable.resolveNames(MyResources.getString("CACHE_MANAGER")));
			out.close();
		}
		catch (Exception err)
		{
			// Log Error (fix this section later)
			String  MethodName = "CacheManage";
			m_Log.error(MethodName, "E_EXCEPTION", err);
		}
	}
	
	private void dumpObjects(HttpServletRequest req, PrintWriter out) throws Exception
	{
		final int MAX_DATA = 10 * 1024;
		
		String key = UtilRequest.getParameter(req, "key");		 // Operation
		
		if (key == null)
			key = "%";
		
		ArrayList<String> objects = BlobOverflow.listKeys(key);
		
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
		StringVarResolverHashMap svrhm = new StringVarResolverHashMap();
		StringVar sv = new StringVar(svrhm);

		StringBuffer sb = new StringBuffer();

		sv.setPattern(MyResources.getString("ADMIN_TABLE_ROW"));
		for (int i=0; i<objects.size(); i++)
		{
			String name = (String) objects.get(i);

			// Don't even show Admin users the digital signatures!!
			if (name.startsWith(ObjectsElement.DIGITAL_SIGNATURES) ||
				name.startsWith(ObjectsElement.LICENSE))
				continue;

			String sData = BlobOverflow.getFromObjectsTableAsString(name);
			String htmlData = HtmlUtil.toSafeHtml(sData.substring(0,Math.min(MAX_DATA,sData.length())));
			
			svrhm.setVariable("name", name);
			svrhm.setVariable("data", htmlData);
			sv.resolveNames(sb);
		}
		svrhm.setVariable("rows", sb);
		
		out.println(sv.resolveNames(MyResources.getString("ADMIN_TABLE")));
	}
	
    private void doErrorEx(HttpServletRequest req, Exception error, PrintWriter out, String function, String op)
    {
    	// Handle the error as best we can, we always log exceptions.
    	m_Log.error(function, "E_EXCEPTION", error);
  	        
 		ResourceBundle myResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", UserElement.getUserLocale(req));
  	        
    	// Get a descriptive name for the operation we attempted, based on the op= parameter.  If we
    	// can't find the appropriate resource, just use the operation name, it's better than nothing.
    	String tranDesc = null;
    	try
    	{
    		tranDesc = myResources.getString("OPERATION_" + op);
    	}
  	  	catch (MissingResourceException mrex)
  	  	{
  	  		tranDesc = function;
  	  	}

  	  	String user = null;
  	  	try
  	  	{
  	  		user = CurrentUser.currentServletUser(req);
  	  	}
  	  	catch(Exception e)
  	  	{
  	  	}
  	  
  	  	out.println(SonoraException.buildErrorPageFromException(error, tranDesc, UserElement.getUserLocale(req),user));
    }
    
	private void scriptsFrame(HttpServletRequest req, PrintWriter out)
	{
		Locale userLocale = UserElement.getUserLocale(req);
		
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", userLocale);

		StringVarResolverHashMap eResolver = new StringVarResolverHashMap();
		StringVar eTable = new StringVar(eResolver);

		out.println(CSRFGuardTokenResolver.addCsrfTokenToResponse(eTable.resolveNames(MyResources.getString("SCRIPT_FRAMESET")), "frame", "src", req));
		out.close();	// ?
	}

	private void scriptsPage(HttpServletRequest req, PrintWriter out) throws Exception
	{
		Locale userLocale = UserElement.getUserLocale(req);
		
		ResourceBundle MyResources = ResourceBundle.getBundle("com.eistream.sonora.admin.AdminServletResource", userLocale);

  	  	String user = null;
  	  	try
  	  	{
  	  		user = CurrentUser.currentServletUser(req);
  	  	}
  	  	catch(Exception e)
  	  	{
  	  	}

		StringVarResolverHashMap eResolver = new StringVarResolverHashMap();
		StringVar eTable = new StringVar(eResolver);

		// build the tree
		ScriptTreeBuilder scriptTree = new ScriptTreeBuilder(user);
		eResolver.setVariable("scriptTree", scriptTree.toString(null));

		// render the form with the tree
		out.println(eTable.resolveNames(MyResources.getString("SCRIPT_PAGE")));
		if(!CSRFGuardTokenResolver.isCsrfGuardEnabled())
		    out.close();	// ?
	}
}

package com.eistream.sonora.filestore;

import static com.eistream.sonora.filestore.AppletsOptionsHelper.ALL_APPLETS_DISABLED;
import static com.eistream.sonora.filestore.AppletsOptionsHelper.ALL_APPLETS_ENABLED;
import static com.eistream.sonora.filestore.AppletsOptionsHelper.ONLY_IFORWEB_APPLET_ENABLE;
import static com.eistream.sonora.filestore.AppletsOptionsHelper.ONLY_UPLOAD_DOWNLOAD_APLLET_ENABLE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.ejb.FinderException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.eistream.sonora.cachemanager.CacheConstants;
import com.eistream.sonora.cachemanager.CacheSend;
import com.eistream.sonora.csrfguard.CSRFGuardTokenResolver;
import com.eistream.sonora.events.OnApplication;
import com.eistream.sonora.exceptions.DocumentLockedByAnotherUserException;
import com.eistream.sonora.exceptions.DocumentRecordNotAccessibleException;
import com.eistream.sonora.exceptions.FileExtensionMapAlreadyExistsException;
import com.eistream.sonora.exceptions.InvalidFileTransformClassException;
import com.eistream.sonora.exceptions.NegativeObjectIdException;
import com.eistream.sonora.exceptions.NoAccessException;
import com.eistream.sonora.exceptions.NoDocumentLockException;
import com.eistream.sonora.exceptions.NoUserPermissionsException;
import com.eistream.sonora.exceptions.NotFoundException;
import com.eistream.sonora.exceptions.RecordNotFoundException;
import com.eistream.sonora.exceptions.SonoraEventException;
import com.eistream.sonora.exceptions.SonoraException;
import com.eistream.sonora.exceptions.SonoraExceptionEx;
import com.eistream.sonora.exceptions.SonoraExceptionMessageStrings;
import com.eistream.sonora.exceptions.SonoraIllegalArgumentException;
import com.eistream.sonora.fields.FieldPropertiesTOArrayUtil;
import com.eistream.sonora.fields.html.ConflictResolutionPage;
import com.eistream.sonora.forms.SonoraForm;
import com.eistream.sonora.history.HistoryRecord;
import com.eistream.sonora.history.HistorySessionEJB;
import com.eistream.sonora.history.HistorySessionEJBHome;
import com.eistream.sonora.layout.uxtags.UXThemeResolver;
import com.eistream.sonora.property.ImmutableProperty;
import com.eistream.sonora.property.Property;
import com.eistream.sonora.property.PropertyAsStringResolver;
import com.eistream.sonora.recordsmanager.DeclarationManager;
import com.eistream.sonora.recordsmanager.FilePlanComponent;
import com.eistream.sonora.recordsmanager.FilePlanComponents;
import com.eistream.sonora.recordsmanager.FilePlanNavigator;
import com.eistream.sonora.recordsmanager.FilePlanView;
import com.eistream.sonora.recordsmanager.HostRecordId;
import com.eistream.sonora.recordsmanager.LoginManager;
import com.eistream.sonora.recordsmanager.RecordDefinition;
import com.eistream.sonora.recordsmanager.RecordOperations;
import com.eistream.sonora.recordsmanager.RecordsUtil;
import com.eistream.sonora.repositories.ArchiveInfo;
import com.eistream.sonora.repositories.RepositoryElement;
import com.eistream.sonora.repositories.RepositoryKey;
import com.eistream.sonora.repositories.RepositoryManager;
import com.eistream.sonora.repository.util.RepositoryProviderWebFacade;
import com.eistream.sonora.repository.util.RepositoryProviderWebProxy;
import com.eistream.sonora.security.SecurityCache;
import com.eistream.sonora.system.SystemApplicationProperties;
import com.eistream.sonora.templates.TemplatesCache;
import com.eistream.sonora.users.UserCache;
import com.eistream.sonora.users.UserContext;
import com.eistream.sonora.users.UserElement;
import com.eistream.sonora.util.Base64UrlSafe;
import com.eistream.sonora.util.CompareUtils;
import com.eistream.sonora.util.ContextAdapter;
import com.eistream.sonora.util.CurrentUser;
import com.eistream.sonora.util.DatabaseServer;
import com.eistream.sonora.util.HtmlUtil;
import com.eistream.sonora.util.L10nUtils;
import com.eistream.sonora.util.LogCategoryEx;
import com.eistream.sonora.util.MimeTypeEx;
import com.eistream.sonora.util.MkDirs;
import com.eistream.sonora.util.ResolvedResourceBundle;
import com.eistream.sonora.util.SonoraConstants;
import com.eistream.sonora.util.SonoraExceptionUtil;
import com.eistream.sonora.util.SonoraMessageInterpreter;
import com.eistream.sonora.util.StatisticsCache;
import com.eistream.sonora.util.StringUtil;
import com.eistream.sonora.util.UtilRequest;
import com.eistream.sonora.util.XmlUtil;
import com.eistream.utilities.Contract;
import com.eistream.utilities.StringVar;
import com.eistream.utilities.StringVarResolver;
import com.eistream.utilities.StringVarResolverHashMap;
import com.eistream.utilities.StringVarResolverServletParms;
import com.eistream.utilities.TimestampUtil;
import com.eistream.utilities.calendar.CalendarFactory;
import com.eistream.utilities.calendar.ISimpleDateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * FileStore Servlet This class is responsible for the Filestore user interface
 */
public class FileStoreServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private LogCategoryEx m_Log;
    private static final int BUFFER_SIZE = 100 * 1024;

    private static final int MAXPROFILENAME_LEN = 128;

    private static final String PROFILENAME_REGEX = "^[a-zA-Z]{1}[a-zA-Z0-9_ ]{0," + (MAXPROFILENAME_LEN - 1) + "}$";

    //
    // These are the standard HttpServlet methods
    //
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {

        String cancel = UtilRequest.getParameter(req, "cancel");
        if (cancel != null)
            return;

        dispatchOperation(req, resp, false);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String cancel = UtilRequest.getParameter(req, "cancel");
        if (cancel != null)
            return;

        dispatchOperation(req, resp, true);
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        try
        {
            m_Log = new LogCategoryEx("Sonora.filestore.FileStoreServlet");
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    @Override
    public String getServletInfo()
    {
        return "FileStore Servlet";
    }

    private void sendSuccessPageToApplet(PrintWriter out)
    {
        out.println("<html><body>outcome=success</body></html");
    }

    private void sendErrorPageToApplet(PrintWriter out, String error, String description, String eventMessage)
    {
        String methodName = "sendErrorPageToApplet";

        try
        {
            out.println("<html><body>outcome=error?error=" + error + "&description=" + description + "&eventMessage=" + eventMessage + "</body></html");

        }
        catch (Exception e)
        {
            m_Log.error(methodName, "E_EXCEPTION", error);

            // Eat the exception - not much we can do.
        }
    }

    private void setDirectDocURL(HttpServletRequest req)
    {
        Collection c = Collections.synchronizedList(Collections.list(req.getParameterNames()));

        String url = "";
        String paramValue;
        String paramName;

        synchronized (c)
        {
            Iterator i = c.iterator();
            while (i.hasNext())
            {
                Object nameObj = i.next();
                if (nameObj != null)
                {
                    paramName = (String) nameObj;
                    paramValue = UtilRequest.getParameter(req, paramName);

                    if (url.length() > 0)
                    {
                        url += "&";
                    }
                    url += paramName + "=" + paramValue;
                }
            }
        }

        url = "FileStore?directDoc=1&" + url;
        req.setAttribute("docUrl", url);
    }

    /**
     * This is used by both doGet and doPost to dispatch the appropriate
     * opFunction - it is shared because the two code paths are virtually
     * identical.
     */
    private void dispatchOperation(HttpServletRequest req, HttpServletResponse resp, boolean isPost) throws ServletException, IOException
    {
        String methodName = "dispatchOperation";
        String function = methodName;
        PrintWriter out = null;
        OpContext p = null;

        // This try block ensures we log the exiting message, unfortunately if
        // we get an exception here, there is
        // not much we can do for the user because we haven't opened the
        // PrintWriter to talk to the browser yet.
        try
        {
            // Trigger the OnApplicaiton event if this is a doGet
            if (!isPost)
            {
                String onApplicationClass = null;
                onApplicationClass = ContextAdapter.getAdapterValue("APPLICATIONEVENTCLASS", "");

                if ((onApplicationClass != null) && (onApplicationClass.length() > 0))
                {
                    try
                    {
                        Class eventHandler = Class.forName("com.eistream.sonora.events.OnApplication" + onApplicationClass);
                        OnApplication handler = (OnApplication) eventHandler.newInstance();
                        handler.onUserStart(CurrentUser.currentServletUser(req), req, resp);
                    }
                    catch (Exception e)
                    {
                    }
                }
            }

            // Get the various request parameters
            p = new OpContext(req, resp, null, "m");
            String id = p.sDocumentId;
            if (null != id)
            {
                if (id.indexOf('.') > -1)
                    id = id.substring(id.indexOf('.') + 1);

                if (Integer.parseInt(id) < 0)
                    throw new NegativeObjectIdException("Object ID can't be negative");
            }
            String genericapplet = UtilRequest.getParameter(req, "genericapplet");
            if (genericapplet != null && genericapplet.equals("true"))
            {
                resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
                resp.setHeader("Pragma", "no-cache"); // HTTP 1.0
                resp.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
                out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), UtilRequest.getCharEncoding()), true);
                p.out = out;
                String griid = req.getParameter("riid");
                String gdid = req.getParameter("id");
                if (griid != null && gdid != null)
                {
                    if (griid.indexOf(gdid) > 0)
                    {
                        p.sDocumentId = gdid;
                        p.iDocumentId = new BigDecimal(gdid);
                    }
                }
                Docinfo docinfo = FileStoreNew.execute(req, resp, p.out, p.getFileStoreSessionEJB(), p.getFileStoreElement(), p.op, p.iDocumentId);
                if (docinfo.isdownload())
                {
                    try
                    {
                        opGetFile(p);
                    }
                    catch (Exception e)
                    {
                        docinfo.failed();

                    }
                }
                docinfo.sendResponse();
                return;
            }
            // Set the function code for error reporting
            function = p.op;

            // Disable cache unless viewing files (or download since SSL chokes
            // otherwise)
             if (p.opIs("view") || p.opIs("y") || p.opIs("open") || p.opIs("download") || p.opIs("getContent") || p.opIs("getContentPages") || p.opIs("doCreate") || "cf".equals(UtilRequest.getParameter(p.req, "src"))) // we
                                                                                                                                                                                                                         // just
                                                                                                                                                                                                                         // need
                                                                                                                                                                                                                         // just
                                                                                                                                                                                                                         // send
                                                                                                                                                                                                                         // an
                                                                                                                                                                                                                         // HTML
                                                                                                                                                                                                                         // to
                                                                                                                                                                                                                         // the
                                                                                                                                                                                                                         // custom
                                                                                                                                                                                                                         // frame
            {
                if (UtilRequest.isInternetExplorer(req))
                {
                    if (ContextAdapter.getBooleanValue("FIX_IE_FILE_CACHE", false))
                    {
                        resp.setHeader("Pragma", "private");
                        resp.setHeader("Cache-Control", "private, must-revalidate");
                    }
                    else
                    {
                        resp.setHeader("Pragma", "no-cache"); // HTTP 1.0
                        resp.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
                    }
                }
                if(!p.isJNLPEnables()) // dont initialize output stream when using the jnlp
                    out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream()), true);
            }
            else
            {
                UserContext userContext = new UserContext(p.req);
                ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
                String persona = (String) prop.get("persona").getValue();
                boolean hasPersona = persona != null && persona.length() != 0;
                String includeContentFrame = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");

                if ("1".equals(includeContentFrame) || ("2".equals(includeContentFrame) && hasPersona))
                {
                    if ((p.opIs("m") || p.opIs("manage") || p.opIs("create") || (p.opIs("upload") && p.redisplay != null && p.redisplay.equals("manage"))) && req.getParameter("fs") == null)
                    {
                        forwardToFilestoreDocumentViewJsp(p);
                        return;
                    }
                    if (p.opIs("restore"))
                    {
                        opRestore(p);
                        return;
                    }
                    if (hasPersona)
                    {
                        if ((p.opIs("e") || p.opIs("g")) && UtilRequest.getParameter(req, "directDoc") == null)
                        {
                            ArrayList versions = p.getVersionList();

                            if (versions.isEmpty())
                            {
                                // just perform manage operation instead of open
                                // empty screen with two buttons
                                forwardToFilestoreDocumentViewJsp(p);
                                return;
                            }

                            setDirectDocURL(req);
                            forwardToOpenDocumentViewJsp(p);
                            return;
                        }
                    }
                }
                if (p.isJNLPEnables())
                {
                    if (p.opIs("edit"))
                    {
                        opEdit(p);
                        return;
                    }
                    else if (p.opIs("checkout") || p.opIs("k") || p.opIs("l"))
                    {
                        opCheckout(p);
                        return;
                    }
                    else if (p.opIs("EVCheckoutForm"))
                    {
                        opPostEVCheckoutForm(p);
                        return;
                    }

                }
                resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
                resp.setHeader("Pragma", "no-cache"); // HTTP 1.0
                resp.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
                out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), UtilRequest.getCharEncoding()), true);

            }

            p.out = out;

            // Every request requires either a document ID or an archive key
            if (p.sDocumentId == null && p.sKey == null)
            {
                out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), UtilRequest.getCharEncoding()), true);
                if (!p.opIs("doCreate") && !p.opIs(RepositoryProviderWebProxy.OP_CREATE) && !p.opIs(RepositoryProviderWebProxy.OP_DELETE) && !p.opIs(RepositoryProviderWebProxy.OP_GETCONTENT) && !p.opIs(RepositoryProviderWebProxy.OP_CREATECONTENT) && !p.opIs(RepositoryProviderWebProxy.OP_DOACTION) && !p.opIs(RepositoryProviderWebProxy.OP_DOACTIONGETDATA) && !p.opIs(RepositoryProviderWebProxy.OP_DOACTIONPOSTDATA) && !p.opIs(RepositoryProviderWebProxy.OP_COPY) && !p.opIs(RepositoryProviderWebProxy.OP_GET) && !p.opIs(RepositoryProviderWebProxy.OP_QUERY) && !p.opIs(RepositoryProviderWebProxy.OP_SET))
                {
                    if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
                    {
                        out.println(p.resources.getString("FILESTORE_USAGE_DISPLAY"));
                        return;
                    }
                    else
                    {
                        String queryPart = ((req.getQueryString() == null) ? "" : "?" + req.getQueryString());
                        String URL = req.getRequestURL() + queryPart;
                        m_Log.error(methodName, "Invalid document Id : " + p.sDocumentId);
                        m_Log.error(methodName, "The user : " + req.getUserPrincipal().getName() + " made this request : " + URL);
                        // set this to status 400 , it is a bad request.
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }
            }
            p.out = out;
            // For the get operations: "g" "e" "open", default the redisplay to
            // close to maintain URL backwards compatibility
            if ((p.opIs("open") || p.opIs("g") || p.opIs("e")) && (p.redisplay == null))
                p.redisplay = "close";

            // Make sure there is no collision between get and post operations
            if (isPost)
                p.op = "post" + p.op;

            if (p.opIs("vlist"))
            {
                FileStoreElement fs = p.getFileStoreElement();
                if (fs.getIsLocked() && p.isLockedByCurrentUser() && fs.getCurrentVersion() == 0)
                {
                    p.unlockDocument();
                }
            }

            // Dispatch to the appropriate filestore opFunction. None of these
            // methods does its own logging
            // or exception handling (if an exception is thrown, it is reported
            // here using the function code).
            if (p.opIs("vlist"))
                opVList(p);
            else if (p.opIs("view") || p.opIs("y"))
                opView(p);
            else if (p.opIs("edit"))
                opEdit(p);
            else if (p.opIs("open") || p.opIs("g") || p.opIs("e"))
                opOpen(p);
            else if (p.opIs("checkout") || p.opIs("k") || p.opIs("l"))
                opCheckout(p);
            else if (p.opIs("checkin") || p.opIs("i"))
                opCheckin(p);
            else if (p.opIs("lock") || p.opIs("w"))
                opLock(p);
            else if (p.opIs("unlock") || p.opIs("u"))
                opUnlock(p);
            else if (p.opIs("upload") || p.opIs("r"))
                opUpload(p);
            else if (p.opIs("upscan"))
                opUpScan(p);
            else if (p.opIs("upblank"))
                opUpBlank(p);
            else if (p.opIs("postUpload"))
                opPostUpload(p);
            else if (p.opIs("download"))
                opDownload(p);
            else if (p.opIs("downloadEdit"))
                opDownloadEdit(p);
            else if (p.opIs("getFile"))
                opGetFile(p); // Called by download applet to retrieve a file
            else if (p.opIs("manage") || p.opIs("m") || p.opIs("f"))
                opManage(p); // Manage (display the document frameset)
            else if (p.opIs("addRendition"))
                opAddRendition(p);

            else if (p.opIs("publish"))
                opPublish(p);
            else if (p.opIs("unpublish"))
                opUnpublish(p);
            else if (p.opIs("EVDetails"))
                opEVDetails(p, null);
            else if (p.opIs("postEVDetailsForm"))
                opPostEVDetailsForm(p);
            else if (p.opIs("postEVCheckoutForm"))
                opPostEVCheckoutForm(p);
            else if (p.opIs("postEVCheckinForm"))
                opPostEVCheckinForm(p);
            else if (p.opIs("signature"))
                opSignature(p);
            else if (p.opIs("properties") || p.opIs("p"))
                opProperties(p, null);
            else if (p.opIs("postProperties"))
                opPostProperties(p, out); // Update properties request
            else if (p.opIs("makeTemplate") || p.opIs("t"))
                opMakeTemplate(p);

            else if (p.opIs("delVersion"))
                opDelVersion(p);
            else if (p.opIs("delRendition"))
                opDelRendition(p);
            else if (p.opIs("delete") || p.opIs("x") || p.opIs("d"))
                opDeleteDocument(p);
            else if (p.opIs("create"))
                opManage(p);

            else if (p.opIs("archiveTemplateCreate") || p.opIs("postArchiveTemplateCreate"))
                opArchiveTemplateCreate(p);
            else if (p.opIs("archiveTemplateDisplay"))
                opArchiveTemplateDisplay(p);
            else if (p.opIs("archive"))
                opArchive(p);
            else if (p.opIs("restore"))
                opRestore(p);

            else if (p.opIs("extensionMaps"))
                opExtensionMaps(p);

            else if (p.opIs("imagingOptions"))
                opImagingOptions(p, null);
            else if (p.opIs("postImagingOptions"))
                opPostImagingOptions(p);
            else if (p.opIs("deleteScanningProfile") || p.opIs("postdeleteScanningProfile"))
                opDeleteScanningProfile(p);

            else if (p.opIs("newExtension"))
                opEditExtensionMap(p, true);
            else if (p.opIs("deleteExtensionMap"))
                opDeleteExtensionMap(p);
            else if (p.opIs("postEditExtension"))
                opEditExtensionMap(p, false);
            else if (p.opIs("postCreateExclusionList"))
                opPostCreateExclusionList(p);
            else if (p.opIs("postUpdateExtensionMap"))
                opPostUpdateExtensionMap(p);

            else if (p.opIs("declare"))
                opDeclare(p);
            else if (p.opIs("supersede"))
                opSupersede(p);
            else if (p.opIs("addSupporting"))
                opAddSupporting(p);
            else if (p.opIs("showViewList"))
                opViewListForm(p);
            else if (p.opIs("navigate"))
                opNavigateFilePlanForm(p);
            else if (p.opIs("filePlan"))
                opFilePlan(p);
            else if (p.opIs("postRMViewList"))
                opPostViewListForm(p);
            else if (p.opIs("postNavigate"))
                opPostNavigateFilePlanForm(p);
            else if (p.opIs("postRegister"))
                opPostDeclarationForm(p);
            else if (p.opIs("postSupersede"))
                opPostSupersede(p);
            else if (p.opIs("postAddSupporting"))
                opPostAddSupporting(p);
            else if (p.opIs("postAutoClassify"))
                opPostAutoClassify(p);

            else if (p.opIs("filePlan"))
                opFilePlan(p);
            else if (p.opIs("postFilePlan"))
                opPostFilePlan(p);
            else if (p.opIs("browseDocument"))
                opPostFilePlan(p);
            else if (p.opIs("postBrowseDocument"))
                opPostBrowseDocument(p);
            else if (p.opIs("close") || p.opIs("c"))
                opClose(p);
            else if (p.opIs("fixDocumentCopies"))
                opFixDocumentCopies(p);
            else if (p.opIs("imageMap"))
                opOutputImageMap(p);

            /*
             * Methods added for external filestore repository support
             */
            else if (p.opIs("getContent"))
                opGetContent(p);
            else if (p.opIs("getContentPages"))
                opGetContentPages(p);
            else if (p.opIs("postdoCreate"))
                opCreateObject(p);
            else if (p.opIs("post" + RepositoryProviderWebProxy.OP_COPY))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.copy(req, resp);
            }
            else if (p.opIs("post" + RepositoryProviderWebProxy.OP_CREATE))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.create(req, resp);
            }
            else if (p.opIs("post" + RepositoryProviderWebProxy.OP_CREATECONTENT))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.createContent(req, resp);
            }
            else if (p.opIs(RepositoryProviderWebProxy.OP_DELETE))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.delete(req, resp);
            }
            else if (p.opIs(RepositoryProviderWebProxy.OP_GETCONTENT))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.getContent(req, resp);
            }
            else if (p.opIs(RepositoryProviderWebProxy.OP_DOACTION))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.doAction(req, resp);
            }
            else if (p.opIs(RepositoryProviderWebProxy.OP_DOACTIONGETDATA))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.doActionGetData(req, resp);
            }
            else if (p.opIs("post" + RepositoryProviderWebProxy.OP_DOACTIONPOSTDATA))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.doActionPostData(req, resp);
            }
            else if (p.opIs(RepositoryProviderWebProxy.OP_GET))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.get(req, resp);
            }
            else if (p.opIs("post" + RepositoryProviderWebProxy.OP_SET))
            {
                p.sentPage = true;
                RepositoryProviderWebFacade.set(req, resp);
            }

            // The null operation simply falls through to perform the specified
            // redisplay
            else if (p.opIs("null"))
                ;

            // Invalid operation
            else
            {
                if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
                {
                    out.println(p.resources.getString("FILESTORE_USAGE_DISPLAY"));
                }
                return;
            }

            // If the operation method has not already returned a page, return
            // the page specified by the redisplay parameter or the redirect Url
            // If a redirect Url parameter is specified, we send a redirect to
            // that Url. Otherwise if a redisplay parameter is specified,
            // redisplay
            // the requested page. If neither is specfied, redisplay the version
            // list page.
            if (!p.sentPage)
            {
                String rurl = p.getParameter("rurl");
                if (rurl != null && rurl.length() != 0)
                {
                    try
                    {
                        rurl = UtilRequest.rUrlDecode(rurl);
                        String pop = p.getParameter("pop");
                        if (pop != null)
                        {
                            StringBuilder szPassAlong = new StringBuilder();
                            String[] op = req.getParameterValues("op");
                            if (op != null && op.length > 1)
                                szPassAlong.append("&op=").append(op[1]);
                            String target = p.getParameter(req, "Target");
                            if (target != null)
                                szPassAlong.append("&Target=").append(target);
                            String category = p.getParameter(req, "Category");
                            if (category != null)
                                szPassAlong.append("&Category=").append(category);
                            resp.sendRedirect(rurl + "&pop=" + pop + szPassAlong.toString());
                        }
                        else
                            resp.sendRedirect(rurl);
                    }
                    catch (Exception err)
                    {
                    }
                }
                else
                {
                    // If we were called by an aplet set it the successful
                    // status page. If something went wrong,
                    // Exception will be thrown and we won't reach this point
                    if (p.calledByApplet)
                    {
                        sendSuccessPageToApplet(out);
                    }
                    else if ("_ajax".equals(p.target))
                    {
                        // send message-body of zero-length
                    }
                    // Otherwise redisplay the page they've asked for with the
                    // redisplay= parameter. Note that both the
                    // VList and Close operations suppport the onLoad parameter
                    // in OpContext to specify an onLoad event
                    // handler. This is used when we need to update a page as
                    // well as launch a new window to do something.
                    else if (p.redisplay == null || p.redisplay.equalsIgnoreCase("VList"))
                        opVList(p);
                    else if (p.redisplay.equalsIgnoreCase("close"))
                        opClose(p);
                    else if (p.redisplay.equalsIgnoreCase("manage"))
                        opManage(p);
                }
            }
        }
        catch (Exception error)
        {
            if (out == null)
            {
                resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
                out = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), UtilRequest.getCharEncoding()), true);
                resp.setHeader("Pragma", "no-cache");
            }

            Throwable t = SonoraExceptionUtil.unwrap(error);
            if (t instanceof Exception)
            {
                error = (Exception) t;
            }
            else
            {
                error = new Exception(t);
                error.setStackTrace(t.getStackTrace());
            }

            // for handling NoUserPermissionsException from the
            // FileStoreSessionEJBBean.getElement(BigDecimal,Boolean), we throw
            // FinderException
            // with message "NoUserPermissionsException" and check that here and
            // don't log the message as error because it
            // is already logged in the bean , or else log the error for other
            // exception cases.
            boolean isNoUserPermissionsException = ((error != null) && (error instanceof FinderException) && ("NoUserPermissionsException".equals(error.getMessage())));

            // for handling RecordNotFoundException from the
            // FileStoreSessionEJBBean.getElement(BigDecimal,Boolean), we throw
            // FinderException
            // with message "RecordNotFoundException" and check that here and
            // don't log the message as error because it
            // is already logged in the bean , or else log the error for other
            // exception cases.
            String errorMsg = error.getMessage();
            boolean isRecordNotFoundException = ((error != null) && (error instanceof FinderException) && (errorMsg.contains("; RecordNotFoundException ;")));
            String errMsg = "";
            if (!isRecordNotFoundException && !isNoUserPermissionsException)
            {
                // Handle the error as best we can, we always log exceptions.
                m_Log.error(function, "E_EXCEPTION", error);
            }
            else if (isRecordNotFoundException && !isNoUserPermissionsException)
            {
                errMsg = errorMsg.replace("; RecordNotFoundException ;", "");
                FinderException err = new FinderException(errMsg);
                err.setStackTrace(error.getStackTrace());
                ;
                m_Log.error(function, "E_EXCEPTION", err);
            }
            // We will need access to the error resources
            Locale userLocale = UserElement.getUserLocale(req);
            ResourceBundle errorResources = ResourceBundle.getBundle("com.eistream.sonora.util.SonoraErrorBundle", userLocale);

            // if this error is being returned to an applet, it must be
            // formatted such that the applet will understand. We know we
            // were called by an applet because all the Applet URLs include the
            // parameter applet=1 which is parsed out by OpContext.
            if (p != null && p.calledByApplet)
            {
                // Get the information we'll need from the exception
                SonoraException ex = SonoraException.getFromException(error);
                String exceptionMessage = ex.getMessage();
                int errorCode = 0;
                String errorTitle = null;
                String errorDesc = null;

                if (error instanceof SonoraExceptionEx)
                {
                    SonoraExceptionMessageStrings sonoraExceptionMessageStrings = SonoraMessageInterpreter.interpretMessage(error, userLocale);
                    errorTitle = sonoraExceptionMessageStrings.getTitle();
                    errorDesc = sonoraExceptionMessageStrings.getDescription();
                }
                if (error instanceof SonoraEventException)
                {
                    // this case for custom error messages(errorEvent function
                    // from event handler scripts)
                    out.println(exceptionMessage);
                    return;
                }
                else
                {
                    errorCode = ex.getErrorCodeFromExceptionMessage();

                    // Get the error title. Try getting it using the error code,
                    // but if we can't use the generic title
                    try
                    {
                        errorTitle = errorResources.getString("ERROR" + errorCode);
                    }
                    catch (MissingResourceException mrex)
                    {
                        errorTitle = errorResources.getString("ERROR0");
                    }

                    // Get the error description. Try getting it using the error
                    // code, but if we can't, try using the
                    // error title for the specific error, if that fails, use
                    // the generic error description.
                    try
                    {
                        errorDesc = errorResources.getString("DESCRIPTION" + errorCode);
                    }
                    catch (MissingResourceException mrex)
                    {
                        try
                        {
                            errorDesc = errorResources.getString("ERROR" + errorCode);
                        }
                        catch (MissingResourceException mex)
                        {
                            errorDesc = errorResources.getString("DESCRIPTION0");
                        }
                    }
                }

                // Get the exception message, use the event exception message if
                // we have it, otherwise the exception text
                String eventMessage = SonoraException.unwrapEventExceptionMessage(exceptionMessage).trim();
                if (eventMessage.length() == 0)
                    eventMessage = SonoraException.convertExceptionText(exceptionMessage);

                String errorHeading = "";

                if (errorCode != 0)
                {
                    errorHeading += "Error " + String.valueOf(errorCode) + " -- " + errorTitle;
                }
                else
                {
                    errorHeading = errorTitle;
                    if (CompareUtils.compare(errorDesc, exceptionMessage))
                    {
                        errorDesc = eventMessage; // Avoid the same message
                                                  // displayed twice
                        eventMessage = "";
                    }
                }

                // Send the error message to the applet.
                sendErrorPageToApplet(out, errorHeading, errorDesc, eventMessage);
            }
            // The error is being returned to a user to see...
            else
            {
                // Get a descriptive name for the operation we attempted, based
                // on the op= parameter. If we
                // can't find the appropriate resource, just use the operation
                // name, it's better than nothing.
                String tranDesc = null;
                if (p != null)
                {
                    try
                    {
                        tranDesc = p.resources.getString("OPERATION_" + function.toLowerCase());
                    }
                    catch (MissingResourceException mrex)
                    {
                        tranDesc = function;
                    }
                }
                else
                {
                    tranDesc = function;
                }
                // return it to the user.
                // build the error page for NoUserPermissionsException
                String user = (p != null ? p.user : "");
                error = (isNoUserPermissionsException ? new NoUserPermissionsException(user) : error);

                if (isRecordNotFoundException)
                {
                    // checking whether the request is for CaseFolder object or
                    // FileStore object.
                    // If it is for CaseFolder object ,the request is redirected
                    // from CaseFolderServlet in case it is not found in
                    // CaseFolder Repository.
                    // So, then isRedirect attribute will be true.
                    // Otherwise, it would return null.
                    Boolean isRedirect = (Boolean) req.getAttribute("isRedirect");
                    if (isRedirect == null)
                        error = new RecordNotFoundException(error, SonoraConstants.FILESTORE_REPOSITORY + "." + p.sDocumentId);
                    else if (isRedirect == true)
                        error = new RecordNotFoundException(error, SonoraConstants.CASEFOLDER_REPOSITORY + "." + p.sDocumentId);
                }

                out.println(SonoraException.buildErrorPageFromException(error, tranDesc, userLocale, user));
            }
        }
        finally
        {
            // Make sure the PrintWriter is closed
            if (null != out && !CSRFGuardTokenResolver.isCsrfGuardEnabled())
                out.close();
        }
    }

    // ***************************************** Service Methods
    // ***************************************************

    // This method returns the View Url for the specified version - specified
    // because this is called from
    // opVList for each version file in turn so we can't use the version
    // parameter in the OpContext.
    private String getViewURL(OpContext p, FSVersionElement fsv) throws Exception
    {
        FileStoreElement fs = p.getFileStoreElement();

        String result = null;
        if (fs.isNewStyleDocument())
        {
            // Get the document's template (the current document could itself be
            // a template)
            FileStoreElement template = fs.getTemplateId() == null ? fs : (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());

            // Get the Viewer URL for this file type (allows users to plug in
            // different viewers)
            String viewerURLTemplate = FileExtensionMapCache.getViewerUrl(template.getDocumentID(), fsv.getFileName());
            if (viewerURLTemplate != null)
            {
                StringVarResolverHashMap hashTable = new StringVarResolverHashMap();
                StringVar stringVar = new StringVar(hashTable);

                hashTable.setVariable("documentId", fs.getDocumentID().toString());
                hashTable.setVariable("version", "" + fsv.getVersionNumber());
                hashTable.setVariable("elId", fsv.getStorageElement().getStorageId().toString());
                result = stringVar.resolveNames(viewerURLTemplate);
            }
        }

        // If we don't have a custom viewerURL, use the standard built-in
        // operation
        if (result == null)
        {
            StorageElement se = fsv.getStorageElement();
            String documentId = fsv.getDocumentID().toString();
            int versionNumber = fsv.getVersionNumber();

            UserContext userContext = new UserContext(p.req);
            ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
            String persona = (String) prop.get("persona").getValue();
            boolean hasPersona = persona != null && persona.length() != 0;
            String includeContentFrame = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");

            // document content should be opened in the same window while using
            // the new Filestore frameset
            if ("1".equals(includeContentFrame) || ("2".equals(includeContentFrame) && hasPersona))
            {
                if (CSRFGuardTokenResolver.isCsrfGuardEnabled())
                {
                    result = CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "href=\"FileStore?op=view&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString()))) + "\" target=\"" + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + "\"";
                }
                else
                {
                    result = "href=\"FileStore?op=view&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "\" target=\"" + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + "\"";
                }
            }
            else
            {
                // If we are using applets, and the file is an image, launch the
                // WIE in a new window.
                if (p.useApplets() && se != null && !se.isRendition() && p.isWIEFile(versionNumber))
                {
                    result = "href=\"javascript:void(0);\" onclick=\"window.open('FileStore?op=view&redisplay=vlist&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "','_blank','toolbar=0,location=0,menubar=0,scrollbars=0,status=1,resizable=1', false)\"";
                }
                else
                {
                    result = "href=\"FileStore?op=view&redisplay=vlist&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + " \" target=\"_blank\"";
                }
            }
        }
        else
            result = "href=\"" + result + "\" target=\"_blank\"";

        return result;
    }

    // This method returns the Edit Url for the specified version - specified
    // because this is called from
    // opVList for each version file in turn so we can't use the version
    // parameter in the OpContext.
    private String getEditURL(OpContext p, FSVersionElement fsv) throws Exception
    {
        UserContext userContext = new UserContext(p.req);
        ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
        String persona = (String) prop.get("persona").getValue();
        boolean hasPersona = persona != null && persona.length() != 0;
        String includeContentFrame = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");
        StorageElement se = fsv.getStorageElement();
        String documentId = fsv.getDocumentID().toString();
        int versionNumber = fsv.getVersionNumber();

        // If we are using applets, and the file is an image, launch the WIE in
        // a new window.
        if (p.useApplets() && p.isWIEFile(fsv.getVersionNumber()))
        {
            if ("1".equals(includeContentFrame) || ("2".equals(includeContentFrame) && hasPersona))
            {
                return "href=\"FileStore?op=edit&redisplay=vlist&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "\" target=\"" + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + "\"";
            }
            else
            {
                return "href=\"javascript:void(0);\" onclick=\"window.open('FileStore?op=edit&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "','_blank', 'toolbar=0,location=0,menubar=0,scrollbars=0,status=1,resizable=1', false)\"";
            }
        }
        else
        {
            return "href=\"FileStore?op=edit&redisplay=vlist&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "\"";
        }
    }

    // This method returns an icon for a given version's extension, it verifies
    // that the icon it
    // returns is actually a supported icon by checking in the property
    // SUPPORTED_ICONS.
    String getIcon(String supported, FileStoreElement fs, FSVersionElement fsv)
    {
        if (fsv == null || fsv.getExtensions() == null)
            return fs.IconName != null ? fs.IconName : "ext-filestore.gif";

        // Force the extension to lower case so we can make all the icon
        // filenames lower case
        String extension = fsv.getExtensions().toLowerCase();
        if (supported.indexOf(extension) != -1)
            return "ext-" + extension + ".gif";
        return "ext-unk.gif";
    }

    // This method returns the given page, marshalling all the appropriate
    // resolvers so the page can access the document's properties
    public void returnPage(OpContext p, String page) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        PropertyAsStringResolver fsPropResolver;
        // Don't hit the database if filestore element is loaded already(For
        // better performance). Just reuse filestore element load
        // from the OpContext.
        // Why don't i just call p.getFileStoreElement() instead of performing
        // complicated if condition . There are two reasons
        // 1. This method( returnPage ) called from multiple callers(multiple
        // context). In some context( especialy OpRestore() ) the filestore
        // element is not loaded and expects that way.
        // 2. During error reporting, PropertyAsStringResolver not realy used
        // even though we are chaining the resolvers. So let the
        // resolver decide to load the element.
        if (p.iDocumentId != null && p.fs != null && CompareUtils.compare(p.iDocumentId, p.fs.DocumentID))
        {
            // Resolve any variables in the frameset using the document's
            // properties, the Version resolver, and any servlet parameters
            fsPropResolver = new PropertyAsStringResolver(new FileStoreProperties(p.fs));
        }
        else
        {
            fsPropResolver = new PropertyAsStringResolver(new FileStoreProperties(p.iDocumentId));
        }

        StringVarResolverHashMap versionVars = new StringVarResolverHashMap(new com.eistream.sonora.util.Version());
        StringVarResolverServletParms vars = new StringVarResolverServletParms(p.req);

        // Chain the resolvers
        StringVar htmlResolver = new StringVar(fsPropResolver);
        fsPropResolver.chainResolvers(p.stringVars);
        p.stringVars.chainResolvers(versionVars);
        versionVars.chainResolvers(vars);

        // If this is a template, add the templateName property
        if (p.getFileStoreElement().IsTemplate != FileStoreElement.TYPE_INSTANCE)
        {
            // Create another hashmap resolver
            StringVarResolverHashMap templateVars = new StringVarResolverHashMap();
            vars.chainResolvers(templateVars);

            // Get the template name from the document element
            templateVars.setVariable("templateName", p.getFileStoreElement().TemplateName);
        }

        // Always make the DocumentID directly available
        versionVars.setVariable("DocumentID", p.sDocumentId);

        Locale locale = UserElement.getUserLocale(p.req);
        String output = htmlResolver.resolveNames(page, locale, p.getTimeZone());
        String result = CSRFGuardTokenResolver.addCsrfTokenToResponse(output, "frame", "src", p.req);
        p.out.println(L10nUtils.localizeTokens(result, locale));
    }

    // Returns true if the document is not a record or if it is a record and the
    // user
    // has access to all its classifications.
    // This is an expensive test and should be deferred to the last possible
    // moment.
    private boolean isAccessible(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            boolean isAccessible = true;

            if (p.getFileStoreElement().isRecordsManagementEnabled() && p.getVersion() != p.fs.getNextVersion().intValue())
            {
                DocVersionElement dve = p.getFileStoreSessionEJB().getDocumentVersionElement(p.fs, p.getVersion());
                if (dve.isRecord())
                {
                    loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
                    isAccessible = RecordsUtil.isAccessible(loginManager, p.fs.getDocumentID(), p.getVersion());
                }
            }
            return isAccessible;
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    // This method downloads the specified document, version, rendition. In
    // general, when it is called from an opMethod,
    // it decides if it should use an applet to do this, or if it has to send
    // the file to the browser. Note that even if
    // we run an applet, the applet will make a request to get the file which
    // will call this method again to send the
    // file to the applet.
    //
    // doDownload supports the mode parameter to specify if the file should be
    // downloaded into the current window,
    // a new window, or into the current window after it has been redisplayed.

    // The available modes are listed below.
    private static final int MODE_DEFAULT = 0; // no mode specified (defaults to
                                               // immediate)
    private static final int MODE_IMMEDIATE = 1; // download the file into the
                                                 // current window
    private static final int MODE_DEFERRED = 2; // perform specified redisplay,
                                                // then download file into
                                                // current window
    private static final int MODE_NEWWINDOW = 3; // perform specified redisplay,
                                                 // then download file into new
                                                 // window

    // The action codes are listed below.
    private static final int ACTION_VIEW = 0; // send file to browser to display
                                              // (if image+applets launch WIE)
    private static final int ACTION_DOWNLOAD = 1; // send file to browser to
                                                  // save - do not launch the
                                                  // application
    private static final int ACTION_DOWNLOAD_EDIT = 2; // download file and
                                                       // launch application
    private static final int ACTION_EDIT = 3; // edit an existing file on the
                                              // client system (only for
                                              // applets)
    private static final int ACTION_GET = 4; // used by the download applet to
                                             // get the file
    private static final int ACTION_OPEN = 5; // send file to browser to display
                                              // and maybe edit (if
                                              // image+applets launch WIE)
    private static final int ACTION_SCAN = 6; // Tell applet to start in
                                              // scanning mode (if image+applets
                                              // launch WIE)
    private static final int ACTION_BLANK = 7; // Tell applet to start in
                                               // scanning mode (if
                                               // image+applets launch WIE)
    private static final String actionOp[] =
    { "view", "download", "downloadEdit", "edit", "get", "open", "scan", "blank" };

    private void doDownload(OpContext p, int action, int mode) throws ServletException, IOException, Exception
    {
    	
        // Make sure user has the right to access the document if it is a
        // record.
        if (isAccessible(p) == false)
        {
            throw new DocumentRecordNotAccessibleException(SecurityCache.parseDomain(p.req.getRemoteUser()));
        }

        boolean isIE = UtilRequest.isInternetExplorer(p.req);
        boolean isIE11 = UtilRequest.isInternetExplorer11(p.req);

        // Fix IE problem with cache control headers and SSL (Bug 64530)
        if (isIE && ContextAdapter.getBooleanValue("FIX_IE_FILE_CACHE", false))
        {
            p.resp.setHeader("Pragma", " "); // HTTP 1.0
            p.resp.setHeader("Cache-Control", "must-revalidate"); // HTTP 1.1
        }

        // If HTTP response contains a document that cannot be opened inline in
        // IE,
        // IE obtains the file name correctly only in simplest cases, e.g.
        // <a href="URL"> element. If HTTP response is get as a result
        // of window.open() or URL is typed in the IE address bar, IE fails.
        // See e.g. TFS 5304
        // We don't use FIX_IE_FILE_CACHE option here because
        // IE_HTTP_CACHE_REVALIDATION
        // is supposed to be true for all IE download actions, but
        // FIX_IE_FILE_CACHE
        // is used in many other places and it is supposed to be false by
        // default.
        if (isIE && ContextAdapter.getBooleanValue("IE_HTTP_CACHE_REVALIDATION", true))
        {
            p.resp.setHeader("Pragma", " "); // HTTP 1.0
            p.resp.setHeader("Cache-Control", "must-revalidate"); // HTTP 1.1
        }

        // If requested, defer the requested download operation or do it in a
        // new window. The mode is ignored
        // when using the download applet. This is done by using the onLoad
        // feature.
        if (mode == MODE_DEFAULT)
            mode = MODE_IMMEDIATE;

        int currentVersion = p.getFileStoreElement().getCurrentVersion();
        if (mode != MODE_IMMEDIATE && !(p.useApplets() && !p.isWIEFile(currentVersion)) && p.isIForWebAppletEnable())
        {
            FSVersionElement fsv = p.getVersionElement(currentVersion);
            StorageElement se = fsv.getStorageElement();
            String documentId = fsv.getDocumentID().toString();
            int versionNumber = fsv.getVersionNumber();

            UserContext userContext = new UserContext(p.req);
            ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
            String persona = (String) prop.get("persona").getValue();
            boolean hasPersona = persona != null && persona.length() != 0;
            String includeContentFrame = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");
            if ("1".equals(includeContentFrame) || ("2".equals(includeContentFrame) && hasPersona))
            {
                if (isNativeExtension(fsv.getExtensions()))
                {
                    p.onLoad = "if (window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + " != null) {" + "window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + ".location='FileStore?op=" + actionOp[action] + "&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "';}";
                }
                else
                {
                    p.onLoad = "var contentFrame = window.parent.document.getElementById('" + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + "');" + "if (contentFrame != null) {contentFrame.src='';}";
                }
            }
            else
            {
                String url = "'FileStore?mode=immediate" + "&op=" + actionOp[action] + "&id=" + documentId + "&version=" + versionNumber + (se == null ? "" : ("&elId=" + se.getStorageId().toString())) + "'";

                if (mode == MODE_NEWWINDOW)
                {
                    p.onLoad = "window.open(" + url + ",'_blank','toolbar=0,location=0,menubar=0,scrollbars=0,status=1,resizable=1', false);";
                }
                else if (mode == MODE_DEFERRED)
                {
                    p.onLoad = "window.location=" + url + ";";
                }
            }

            // Return for the dispatcher to send the requested redisplay page
            return;
        }

        // Once we get here, we're returning a page
        p.sentPage = true;

        // Just in case we are downloading a rendition.
        // String elementId = p.getParameter(p.req, "elId");

        // Fetch the FileStoreElement and the specified version's
        // VersionElement.
        FSVersionElement fsv = null;
        FileStoreElement fs = p.getFileStoreElement();

        boolean bPublishedRequired = (action == ACTION_OPEN) && fs.isEnhancedDocumentVersioningEnabled() && fs.isEnhancedDocumentVersioningViewPublishedEnabled() && p.sVersion == null;

        /*
         * Get the latest published if they have asked for the current version,
         * if they ask for a specific one give them that version even though
         * they have latest published set.
         */
        if (bPublishedRequired)
            fsv = p.getFileStoreSessionEJB().getLatestPublishedDVE(fs.getDocumentID());
        else
            fsv = p.getVersionElement(OpContext.VER_SPECIFIED);

        if (fsv == null && (action != ACTION_SCAN && action != ACTION_BLANK))
        {
            UserContext userContext = new UserContext(p.req);
            ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
            String persona = (String) prop.get("persona").getValue();

            if (bPublishedRequired)
            {
                if (persona != null && persona.length() > 0)
                    returnPage(p, p.resources.getString("NO_PUBLISHED_VERSION_PERSONA_BASED"));

                // The requested doucment has no published versions and the
                // template designer selected
                // the 'View Published Only' button
                else
                    returnPage(p, p.resources.getString("NO_PUBLISHED_VERSION"));
            }
            else
            {
                // if the document has no versions - just perform Manage
                // operation
                StringBuilder manageUrl = new StringBuilder("FileStore?op=m&id=");
                manageUrl.append(p.sDocumentId);
                p.resp.sendRedirect(manageUrl.toString());
            }
            return;
        }

        // Download the file with the download applet or WIE if we can,
        // otherwise just pump it to the browser
        if ((fsv == null && (action == ACTION_SCAN || action == ACTION_BLANK)) || ((fsv.getStorageElement() == null || !fsv.getStorageElement().isRendition()) && p.useApplets()))
        {
            // Are we going to invoke the WIE instead? (only for view or edit of
            // images on Windows clients)
        	
        	/******* modified if condition by (mode==1 || mode==5)|| *********/  
            if ((mode==1 || mode==5)||((p.isIForWebAppletEnable()) && ((action == ACTION_DOWNLOAD_EDIT || action == ACTION_VIEW || action == ACTION_EDIT || action == ACTION_OPEN || action == ACTION_SCAN || action == ACTION_BLANK) && ((action == ACTION_SCAN || action == ACTION_BLANK) || p.isWIEFile(OpContext.VER_SPECIFIED)))))
            {
                p.resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());

                // For I4Web DOWNLOAD_EDIT is the same as EDIT (we only have
                // VIEW, OPEN, and EDIT)
                if (action == ACTION_DOWNLOAD_EDIT)
                    action = ACTION_EDIT;

                // if it is locked to someone else, it doesn't matter what they
                // asked for, they can't edit.
                if (fs.getIsLocked() && !p.isLockedByCurrentUser())
                    action = ACTION_VIEW;

                // If they are opening it and they have it locked, they are in
                // edit mode
                if (action == ACTION_OPEN && p.isLockedByCurrentUser())
                    action = ACTION_EDIT;

                // What version do we want
                int version = p.getVersion();
                if (fsv != null && fsv.VersionNumber > 0)
                    version = fsv.VersionNumber;

                // Launch the WIE applet to edit the image
                StringVarResolver contractResolver = Contract.getAsStringVarResolver();
                StringVar varResolver = new StringVar(contractResolver);

                StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
                contractResolver.chainResolvers(stringVars);

                // Pass authorization string to help solve the multiple login
                // screens for the applets when using WebSphere.
                String authorization = p.req.getHeader("Authorization");
                stringVars.setVariable("authorization", authorization);

                String jsessionid = "";
                if (ContextAdapter.getBooleanValue("ENABLE_HTTP_ONLY", false))
                {
                    Cookie cookies[] = p.req.getCookies();
                    if (cookies != null)
                    {
                        for (int i = 0; i < cookies.length; i++)
                        {
                            if (cookies[i].getName().equalsIgnoreCase("JSESSIONID"))
                            {
                                jsessionid = cookies[i].getValue();
                                break;
                            }
                        }
                    }
                }
                stringVars.setVariable("jsessionid", jsessionid);
                stringVars.setVariable("startScanning", Boolean.toString(action == ACTION_SCAN));
                String scanType = p.getParameter(p.req, "scan");
                if (scanType != null && scanType.equalsIgnoreCase("new"))
                    stringVars.setVariable("scannewversion", Boolean.TRUE.toString());
                else
                    stringVars.setVariable("scannewversion", Boolean.FALSE.toString());

                stringVars.setVariable("createblank", Boolean.toString(action == ACTION_BLANK));
                if (ContextAdapter.getBooleanValue("I4WEB_SEPARATE_VM", true))
                    stringVars.setVariable("separate_jvm", "true");
                else
                    stringVars.setVariable("separate_jvm", "false");

                // For I4Web SCAN is the same as EDIT (we only have VIEW, xOPEN,
                // and EDIT)
                if (action == ACTION_SCAN || action == ACTION_BLANK)
                    action = ACTION_EDIT;

                stringVars.setVariable("space", " ");

                // Get the permissions string
                String permissions = p.getFileStoreSessionEJB().getPermissions(fs);

                // If viewing and using WEI then don't allow checkout
                if ((action == ACTION_VIEW && p.isWIEFile(OpContext.VER_SPECIFIED)) && (version >= fs.CurrentVersion) && fs.IsLocked)
                {
                    if (permissions.length() > 0)
                        permissions += ",";
                    permissions += Contract.PERM_DISABLE_CHECKOUT;
                }

                // Fix for Bug 64399 - Unable to view TIF images using imaging
                // for the Web when logged in with a user name that has an apos
                String user = SecurityCache.parseDomain(p.req.getRemoteUser()).replaceAll("\u0027", "/\\u0027");
                String requestedAt = ""; // requestedAt = new
                                         // java.sql.Timestamp(System.currentTimeMillis()).toString();
                /*
                 * Don't want to slow the performance either a. Constructing
                 * logCategoryEx object and checking is log enabled. or b.
                 * System.currentTimeMillis()
                 */
                stringVars.setVariable("mode", action == ACTION_EDIT ? Contract.P_MODE_RW : Contract.P_MODE_READ); // mode
                                                                                                                   // for
                                                                                                                   // WIE
                                                                                                                   // used
                                                                                                                   // to
                                                                                                                   // indicate
                                                                                                                   // read-only
                stringVars.setVariable("id", p.sDocumentId);
                stringVars.setVariable("version", "" + version);
                stringVars.setVariable("permissions", permissions);
                stringVars.setVariable("userName", user);
                stringVars.setVariable("templateId", fs.TemplateId == null ? fs.DocumentID : fs.TemplateId);
                stringVars.setVariable("requestedAt", requestedAt);

                ImagingOptions imagingOptions = ImagingOptions.fromXml(fs.getImagingOptions());
                ScanningProfile scanningProfiles[];
                StringBuffer sbO = new StringBuffer();
                StringBuffer sbE = new StringBuffer();

                if (imagingOptions != null && (scanningProfiles = imagingOptions.getScanningProfiles()) != null && scanningProfiles.length > 0)
                {
                    String profile = p.getParameter("profile");
                    for (int i = 0; i < imagingOptions.getScanningProfiles().length; i++)
                    {
                        ScanningProfileProperties scanningProfileProperties = new ScanningProfileProperties(null, scanningProfiles[i], p.resources);

                        ImmutableProperty propScanData = scanningProfileProperties.get(ScanningProfileProperties.SCANCUSTOMDATA);

                        if (propScanData != null)
                        {
                            String scanData = (String) propScanData.getValue();
                            if (scanData != null && scanData.length() > 0)
                            {
                                String encodeData = URLEncoder.encode(scanData, "UTF-8");
                                scanningProfileProperties.set(ScanningProfileProperties.SCANCUSTOMDATA, encodeData);
                            }
                        }

                        stringVars.chainResolvers(new PropertyAsStringResolver(scanningProfileProperties));

                        stringVars.setVariable("profileNum", new Integer(i + 1));
                        stringVars.setVariable("scanTemplateData", varResolver.resolveNames(p.resources.getString("RUN_WIE_APPLET_SCAN_TEMPLATE_DATA")));

                        if (profile != null && profile.length() > 0 && profile.equalsIgnoreCase(scanningProfiles[i].getProfileName()))
                        {
                            sbO = new StringBuffer();
                            sbE = new StringBuffer();
                            stringVars.setVariable("profileNum", new Integer(1));
                            sbO.append(varResolver.resolveNames(p.resources.getString("RUN_WIE_APPLET_SCAN_TEMPLATES_O")));
                            sbE.append(varResolver.resolveNames(p.resources.getString("RUN_WIE_APPLET_SCAN_TEMPLATES_E")));
                            break;
                        }

                        sbO.append(varResolver.resolveNames(p.resources.getString("RUN_WIE_APPLET_SCAN_TEMPLATES_O")));
                        sbE.append(varResolver.resolveNames(p.resources.getString("RUN_WIE_APPLET_SCAN_TEMPLATES_E")));
                    }
                }

                if (sbO.length() == 0)
                {
                    stringVars.setVariable("useScanTemplates", "false");
                    stringVars.setVariable("scanTemplates_o", "");
                    stringVars.setVariable("scanTemplates_e", "");
                }
                else
                {
                    stringVars.setVariable("useScanTemplates", "true");
                    stringVars.setVariable("scanTemplates_o", sbO);
                    stringVars.setVariable("scanTemplates_e", sbE);
                }

                if (imagingOptions != null)
                {
                    stringVars.setVariable("startPage", new Integer(imagingOptions.getStartPage()));
                    stringVars.setVariable("initialView", new Integer(imagingOptions.getStartView()));
                    stringVars.setVariable("initialZoom", new Integer(imagingOptions.getStartZoom()));
                }
                else
                {
                    stringVars.setVariable("startPage", new Integer(1));
                    stringVars.setVariable("initialView", new Integer(Contract.VIEW_DEFAULT));
                    stringVars.setVariable("initialZoom", new Integer(Contract.ZOOM_DEFAULT));
                }

                // Add the version numbers for the jar files that we download
                // with/for the applet.
                stringVars.setVariable("jai-i4webVersion", com.global360.webimageviewer.Version.Version + "." + com.global360.webimageviewer.Version.Build);
                stringVars.setVariable("i4WebVersion", com.global360.sonora.applets.webimageapplet.Version.Version + "." + com.global360.sonora.applets.webimageapplet.Version.Build);
                stringVars.setVariable("UtiltiesVersion", com.eistream.utilities.Version.Version + "." + com.eistream.utilities.Version.Build);

                /**
                 * The I4WEB_USE_JAI flag is only intended for developers to be
                 * able to test the applet without having to have a non-windows
                 * client. It is not intended as a customer available flag or
                 * configuration
                 */
                String htmlPage = null;
                String nextUrl;
                UserContext userContext = new UserContext(p.req);
                ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
                String persona = (String) prop.get("persona").getValue();
                boolean hasPersona = persona != null && persona.length() != 0;
                String includeContentFrame = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");
                if ("1".equals(includeContentFrame) || ("2".equals(includeContentFrame) && hasPersona))
                {
                    nextUrl = "FileStore?op=null&id=" + p.sDocumentId + "&redisplay=vlist";
                }
                else
                {
                    nextUrl = "";
                }
                stringVars.setVariable("nextUrl", nextUrl);
                String forceUseJavaI4Web = ContextAdapter.getFileAdapterValue("I4WEB_USE_JAI", "0");
                if (!UtilRequest.isWindowsClient(p.req) || forceUseJavaI4Web.equalsIgnoreCase("1"))
                    htmlPage = varResolver.resolveNames(p.resources.getString("RUN_WIE_JAVA_A"));
                else
                {
                    // It's windows and we're using the old WIE - See if it's
                    // Windows 6.0 (A.k.a. Vista) or greater. If so do special
                    // install piece.
                    // wjs 2011-01-21 setting this Vista thing is useless. It
                    // screws up IE8 because the width is 1280. Could make it
                    // smaller but also leaves a
                    // visual artifact on the screen (small thin bar above menu
                    // before resize.
                    // stringVars.setVariable("VistaInstall",
                    // !UtilRequest.isWindowsClient6Plus(p.req) ? "" :
                    // p.resources.getString("RUN_WIE_APPLET_VISTA_INSTALL"));
                	
                	String clientLocation1 = null;
                    FSVersionElement fsv1 = p.getVersionElement(OpContext.VER_CURRENT);
                    if (fsv1 != null)
                        clientLocation1 = fsv1.getClientLocation();
                    if (clientLocation1 == null)
                    	//clientLocation1 = "";
                        clientLocation1 = "C:\\Prudhvi\\OpenText\\Intern\\Case360\\file_example_TIFF_1MB.tiff";
                	String cid =  Bravahelper.createId(clientLocation1+"");
//                  System.out.println(cid);
                	stringVars.setVariable("cid",cid);
                    //htmlPage = varResolver.resolveNames(p.resources.getString("RUN_WIE_APPLET"));
                	htmlPage = varResolver.resolveNames(p.resources.getString("RUN_UPLOAD_TEST"));
                }

                // Generate the HTML to invoke the down applet
                if (m_Log.isDebugEnabled())
                    m_Log.debug("doDownload", "HTML to launch I4Web: " + htmlPage);
                returnPage(p, htmlPage);
                return;
            }
            else if ((p.isJNLPEnables()) && (action == ACTION_EDIT || action == ACTION_DOWNLOAD_EDIT || (action == ACTION_OPEN && p.isLockedByCurrentUser())))
            {
                p.sentPage = true;
                p.req.setAttribute("codebasejnlp", UtilRequest.getBaseUrl(p.req));
                HashMap<String, String> arguments = new HashMap<String, String>(30, 0.9f);

                // Pass authorization string to help solve the multiple login
                // screens for the applets when using WebSphere.
                arguments.put("authorization", encode(p.req.getHeader("Authorization")));
                String jsessionid = "";
                if (ContextAdapter.getBooleanValue("ENABLE_HTTP_ONLY", false))
                {
                    Cookie cookies[] = p.req.getCookies();
                    if (cookies != null)
                    {
                        for (int i = 0; i < cookies.length; i++)
                        {
                            if (cookies[i].getName().equalsIgnoreCase("JSESSIONID"))
                            {
                                jsessionid = cookies[i].getValue();
                                break;
                            }
                        }
                    }
                }
                arguments.put("jsessionid", encode(jsessionid));
                //p.req.setAttribute("JESSIONID",jsessionid );
                // If the client location is null (this happens when a file is
                // created by capture), use the file name instead.
                String clientFileName = fsv.getClientLocation();
                if (clientFileName == null)
                    clientFileName = fsv.getFileName();

                arguments.put("mode", encode((action != ACTION_DOWNLOAD_EDIT) ? "edit" : "rw")); // mode!=download_edit
                                                                                                 // launches
                                                                                                 // file
                                                                                                 // already
                                                                                                 // on
                                                                                                 // client
                                                                                                 // mode=rw
                                                                                                 // causes
                                                                                                 // a
                                                                                                 // download
                                                                                                 // and
                                                                                                 // then
                                                                                                 // launch
                arguments.put("location", encode((URLEncoder.encode(clientFileName, "UTF-8"))));
                arguments.put("operation", encode( "FileStore?op=getFile&id=" + p.sDocumentId + "&applet=1"));

                // Where we go next depends on the redisplay option, the op code
                // null simply falls through to perform the specified redisplay
                String redisp = p.redisplay == null ? "vlist" : p.redisplay;
                arguments.put("nextUrl", encode("FileStore?op=null&id=" + p.sDocumentId + "&redisplay=" + redisp));
                arguments.put("actionurl",encode(CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "FileStore?genericapplet=true&id=" + p.sDocumentId)));
                arguments.put("exitUrl", encode("filestore/AppletReturn.jsp?op=vlist&id=" + p.sDocumentId));
                arguments.put("bgcolor", encode("6699CC"));
                // Set the url to go to if the user cancels the download from
                // the applet.
                arguments.put("cancelUrl", encode("FileStore?op=unlock&id=" + p.sDocumentId + "&redisplay=" + redisp));

                // for OfficeIntegration needs
                arguments.put("base_server_uri", encode(UtilRequest.getBaseUrl(p.req)));
                arguments.put("update_version_on_save", encode("false"));

                // for EnhancedVersioning processing in OfficeIntegration
                DocVersionElement currentDocVersionElement = p.getDocVersionElement(p.fs.CurrentVersion);
                boolean isEnhancedDocumentVersioningEnable = p.fs.isEnhancedDocumentVersioningEnabled();
                String currentVersionLabel = isEnhancedDocumentVersioningEnable ? currentDocVersionElement.getVersionLabel() : StringUtil.EMPTY;
                String proposedNextVersionLabel = isEnhancedDocumentVersioningEnable ? generateNextVersionLable(currentDocVersionElement).toString() : StringUtil.EMPTY;
                arguments.put("enhanced_versioning", encode((String.valueOf(isEnhancedDocumentVersioningEnable))));
                arguments.put("current_version_label", encode(currentVersionLabel));
                arguments.put("proposed_version_label", encode(proposedNextVersionLabel));

                arguments.put("version", encode(String.valueOf(p.fs.CurrentVersion)));
                arguments.put("document_id", encode(constructRepositoryKey(p.fs)));
                arguments.put("document_type", encode("Case360"));

                arguments.put("op", encode(p.op));
                arguments.put("pending_save_changes", encode("false"));

                arguments.put("codeBase", encode(UtilRequest.getBaseUrl(p.req)));
                arguments.put("documentBase", encode(UtilRequest.getBaseUrl(p.req) + "/FileStore?op=" + p.op + "&id=" + p.sDocumentId + "&redisplay=" + redisp));
                arguments.put("app", encode("updownex"));

                p.req.setAttribute("id", p.sDocumentId);
                updateCase360jnlp(arguments, p.req);
                String jsp = "/filestore/LaunchWebStart.jsp"; 
                // Generate the HTML to invoke the down applet (note: the whole
                // errorUrl mechanism is now useless as we invoke the applet in
                // a hidden iframe)
                // returnPage(p,
                // varResolver.resolveNames(p.resources.getString("JNLP_APP")));
                getServletContext().getRequestDispatcher(jsp).forward(p.req, p.resp);
                return;

            }
            else if ((p.isUploadDownloadAppletsEnables()) && (action == ACTION_EDIT || action == ACTION_DOWNLOAD_EDIT || (action == ACTION_OPEN && p.isLockedByCurrentUser())))
            {
                p.resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());

                // If they are opening it and they have it locked, they are in
                // edit mode
                if (action == ACTION_OPEN && p.isLockedByCurrentUser())
                    action = ACTION_EDIT;

                // Download the file with the download applet
                StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
                StringVar varResolver = new StringVar(stringVars);

                // Pass authorization string to help solve the multiple login
                // screens for the applets when using WebSphere.
                String authorization = p.req.getHeader("Authorization");
                stringVars.setVariable("authorization", authorization);
                String jsessionid = "";
                if (ContextAdapter.getBooleanValue("ENABLE_HTTP_ONLY", false))
                {
                    Cookie cookies[] = p.req.getCookies();
                    if (cookies != null)
                    {
                        for (int i = 0; i < cookies.length; i++)
                        {
                            if (cookies[i].getName().equalsIgnoreCase("JSESSIONID"))
                            {
                                jsessionid = cookies[i].getValue();
                                break;
                            }
                        }
                    }
                }
                stringVars.setVariable("jsessionid", jsessionid);

                // If the client location is null (this happens when a file is
                // created by capture), use the file name instead.
                String clientFileName = fsv.getClientLocation();
                if (clientFileName == null)
                    clientFileName = fsv.getFileName();

                stringVars.setVariable("mode", (action != ACTION_DOWNLOAD_EDIT) ? "edit" : "rw"); // mode!=download_edit
                                                                                                  // launches
                                                                                                  // file
                                                                                                  // already
                                                                                                  // on
                                                                                                  // client
                                                                                                  // mode=rw
                                                                                                  // causes
                                                                                                  // a
                                                                                                  // download
                                                                                                  // and
                                                                                                  // then
                                                                                                  // launch
                stringVars.setVariable("file", URLEncoder.encode(clientFileName, "UTF-8"));
                stringVars.setVariable("operation", "FileStore?op=getFile&id=" + p.sDocumentId + "&applet=1") ;

                // Where we go next depends on the redisplay option, the op code
                // null simply falls through to perform the specified redisplay
                String redisp = p.redisplay == null ? "vlist" : p.redisplay;
                stringVars.setVariable("nextUrl","FileStore?op=null&id=" + p.sDocumentId + "&redisplay=" + redisp);
                stringVars.setVariable("actionurl", CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "FileStore?genericapplet=true&id=" + p.sDocumentId));
                String rootKey=p.req.getParameter("shistrootkey");
                if(null!=rootKey){
                   rootKey=rootKey.substring(rootKey.lastIndexOf(".")+1);
                stringVars.setVariable("exitUrl",CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "filestore/AppletReturn.jsp?op=vlist&id=" + p.sDocumentId+"&caseKey="+rootKey));
                }
                else
                     stringVars.setVariable("exitUrl",CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "filestore/AppletReturn.jsp?op=vlist&id=" + p.sDocumentId));
                // Set the url to go to if the user cancels the download from
                // the applet.
                stringVars.setVariable("cancelUrl",CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "FileStore?op=unlock&id=" + p.sDocumentId + "&redisplay=" + redisp));

                // for OfficeIntegration needs
                stringVars.setVariable("base_server_uri", UtilRequest.getBaseUrl(p.req));

                // for EnhancedVersioning processing in OfficeIntegration
                DocVersionElement currentDocVersionElement = p.getDocVersionElement(p.fs.CurrentVersion);
                boolean isEnhancedDocumentVersioningEnable = p.fs.isEnhancedDocumentVersioningEnabled();
                String currentVersionLabel = isEnhancedDocumentVersioningEnable ? currentDocVersionElement.getVersionLabel() : StringUtil.EMPTY;
                String proposedNextVersionLabel = isEnhancedDocumentVersioningEnable ? generateNextVersionLable(currentDocVersionElement).toString() : StringUtil.EMPTY;
                stringVars.setVariable("enhanced_versioning", isEnhancedDocumentVersioningEnable);
                stringVars.setVariable("current_version_label", currentVersionLabel);
                stringVars.setVariable("proposed_version_label", proposedNextVersionLabel);

                stringVars.setVariable("version", p.fs.CurrentVersion);
                stringVars.setVariable("document_id", constructRepositoryKey(p.fs));
                stringVars.setVariable("op", p.op);
                // Generate the HTML to invoke the down applet (note: the whole
                // errorUrl mechanism is now useless as we invoke the applet in
                // a hidden iframe)
                returnPage(p, varResolver.resolveNames(p.resources.getString("RUN_DOWNLOAD_APPLET")));
                return;
            }

            // If we did not return an applet, fall through to the code below to
            // send the actual file
            // either to the browser, or to an applet we invoked the last time
            // we were here.

        }

        // If the template defines an extension map for this file then get the
        // view URL, resolve {{}},
        // and hen redirect to that URL
        if (fsv != null && (action == ACTION_VIEW || action == ACTION_OPEN))
        {
            FileExtensionMap fem = FileExtensionMapCache.getExtensionMap(fs.getTemplateId(), fsv.getExtensions());
            if (fem != null && !StringUtil.isBlank(fem.get("VIEWERURL")))
            {
                StringVarResolverHashMap hashTable = new StringVarResolverHashMap();
                StringVar stringVar = new StringVar(hashTable);
                hashTable.setVariable("documentId", fs.getDocumentID().toString());
                hashTable.setVariable("version", "" + p.getVersion());
                hashTable.setVariable("elId", fsv.getStorageElement().getStorageId().toString());
                String result = stringVar.resolveNames(fem.get("VIEWERURL"));

                p.resp.sendRedirect(result);
                return;
            }
        }

        // If the request for the file came from the download applet, it may
        // tell us where the user saved
        // the file on their system, save this it in the database so we can
        // default it on checkin.
        // Get the parameter unencoded so we can use URLDecoder to get the
        // correct value
        String location = p.getParameter("location", false);
        if (location != null && location.length() != 0)
        {
            // UploadFrontEx.java does URLEncoder.encode so we need to decode
            // here or characters such as Swedish "וצה" are messed up
            location = java.net.URLDecoder.decode(location, "UTF8");
            fsv.setClientLocation(location);
            if (fs.isNewStyleDocument())
                p.getFileStoreSessionEJB().updateClientLocation(fsv.getStorageElement());
            else
                p.getVersionsSessionEJB().setElement(fsv);
        }

        if (fs.isOldStyleDocument())
        {
            p.resp.setHeader("Content-Disposition", UtilRequest.getContentDisposition(calculateIsInlineOrAttachmentFlagForContentDisposition(action, fsv.getFileName()), p.req, fsv.getFileName()));

            // Filename and Document Type
            String fileName = fs.getFileName(fsv);

            // Set content type for reply, so browser will know what it is
            String documentType = MimeTypeEx.getMimeType(fileName);
            p.resp.setContentType(documentType);

            ServletOutputStream stream = null;
            BufferedInputStream docfile = null;
            byte[] ioBuf = new byte[BUFFER_SIZE];
            try
            {
                // get the resources we need for file transfer
                stream = p.resp.getOutputStream();
                docfile = new BufferedInputStream(new FileInputStream(fileName));
                int numRead;

                // read/write til done
                while ((numRead = docfile.read(ioBuf, 0, BUFFER_SIZE)) != -1)
                {
                    stream.write(ioBuf, 0, numRead);
                }

                docfile.close();
                stream.flush();
                stream.close();
            }
            catch (FileNotFoundException fe)
            {
                p.resp.setHeader("Content-Disposition", "");
                p.resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                throw (fe);
            }
            catch (Exception e)
            {
                String methodName = "doDownload";
                m_Log.debug(methodName, "Failed to download the file.");
                m_Log.error(methodName, "E_EXCEPTION", e);
                p.resp.setHeader("Content-Disposition", "");
                p.resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                throw (e);
            }
            finally
            {
                ioBuf = null;
                if (docfile != null)
                    docfile.close();
                if (stream != null)
                    stream.close();
            }
            return;
        }

        // We have a new style document. If it is an image document, we must
        // first assemble it in a temporary file,
        // then download that file. Get the storage element for this version. It
        // would be a map for an image document.
        if (fsv != null)
        {
            StorageElement se = fsv.getStorageElement();
            if (se.isMapElement())
            {
                String fileToDelete = null;
                SystemPool sp = new SystemPool(true);

                try
                {
                    // Create an image document so we can assemble it to a file.
                    ImageDocumentNew imageDocument = new ImageDocumentNew(fs, fsv.getVersionNumber());

                    // Build last node of file name. Use the document name as a
                    // basis.
                    String documentName = imageDocument.getDocumentName();
                    String documentNameU = documentName.toUpperCase();
                    // Make sure the filename ends with a valid TIFF file
                    // extension
                    if (!documentNameU.endsWith(".TIFF") && !documentNameU.endsWith(".TIF"))
                        documentName = documentName + ".tif";

                    StorageElement tmp = new StorageElement(se);
                    tmp.setFileName(documentName);

                    // Get a temporary file.
                    StorageElement nSe = sp.getWorkFile(tmp);
                    String fullFileName = nSe.getWorkFileName();

                    // Put the image document together.
                    imageDocument.assemble(fullFileName);

                    // At this point we have created a file that we MUST delete
                    // regardless of the outcome.
                    fileToDelete = fullFileName;

                    // Set the disposition.
                    p.resp.setHeader("Content-Disposition", UtilRequest.getContentDisposition(calculateIsInlineOrAttachmentFlagForContentDisposition(action, documentName), p.req, documentName));

                    // Get the data. Note that we cannot call sm.download()
                    // because this element is not and CANNOT be persisted.
                    MagneticDataManager.download(nSe, fileToDelete, p.resp);

                    // Delete the temporary file.
                    sp.deleteFile(fileToDelete);
                    fileToDelete = null;
                }
                catch (Exception e)
                {
                    p.resp.setHeader("Content-Disposition", "");
                    p.resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                    throw e;
                }
                finally
                {
                    if (fileToDelete != null)
                        sp.deleteFile(fileToDelete);
                }
            }
            else
            {
                try
                {
                    // We have a normal (non-image) document or a standard
                    // versioning document. Ask the storage manager to send it
                    se = fsv.getStorageElement();
                    StorageManager sm = new StorageManager(false);

                    p.resp.setHeader("Content-Disposition", UtilRequest.getContentDisposition(calculateIsInlineOrAttachmentFlagForContentDisposition(action, fsv.getFileName()), p.req, fsv.getFileName()));
                    sm.download(se, p.resp);
                }
                catch (RecordNotFoundException rnfe)
                {
                    p.resp.setHeader("Content-Disposition", "");
                    p.resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    throw rnfe;
                }
                catch (Exception e)
                {
                    p.resp.setHeader("Content-Disposition", "");
                    p.resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                    throw e;
                }
            }

            // Add history record for download if required
            if ("0".equals(ContextAdapter.getAdapterValue("CREATE_HISTORY", "1")) == false)
            {
                // Check to see if the 'Log Read Only Actions' bit is set at the
                // template

                // Check with template to see if it is OK to write view history
                if (action == ACTION_VIEW)
                {
                    FileStoreElement template = (fs.IsTemplate != FileStoreElement.TYPE_INSTANCE) ? fs : (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());
                    if (template.isReadOnlyHistoryEnabled())
                    {
                        HistoryRecord history = new HistoryRecord();
                        history.setObjectId(fs.getDocumentID());
                        history.setComponentType(0);
                        history.setComponentId(null);

                        if (fs.isEnhancedDocumentVersioningEnabled())
                        {
                            DocVersionElement dve = fsv.getDocVersionElement();
                            if (fsv.getStorageElement().isRendition())
                                history.setAction("FILESTORE_EDV_REVISION_VIEWED");
                            else
                                history.setAction("FILESTORE_EDV_VERSION_VIEWED");

                            history.addVariable("versionLabel", "" + dve.getVersionLabel());
                        }
                        else
                        {
                            if (fsv.getStorageElement().isRendition())
                                history.setAction("FILESTORE_REVISION_VIEWED");
                            else
                                history.setAction("FILESTORE_VERSION_VIEWED");
                        }
                        history.addVariable("versionNumber", "" + se.getVersionNumber());

                        history.setModifiedDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
                        history.setModifiedBy(SecurityCache.parseDomain(p.req.getRemoteUser()));
                        history.setACL(fs.getACL());

                        HistorySessionEJB history_ejb = ((HistorySessionEJBHome) ContextAdapter.getHome(ContextAdapter.HISTORYSESSION)).create();
                        history_ejb.createHistory(SonoraConstants.FILESTORE_REPOSITORY, null, history);
                        history_ejb = null;
                    }
                }
                if (action == ACTION_DOWNLOAD)
                {
                    FileStoreElement template = (fs.IsTemplate != FileStoreElement.TYPE_INSTANCE) ? fs : (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());
                    if (template.isReadOnlyHistoryEnabled())
                    {
                        HistoryRecord history = new HistoryRecord();
                        history.setObjectId(fs.getDocumentID());
                        history.setComponentType(0);
                        history.setComponentId(null);

                        if (fs.isEnhancedDocumentVersioningEnabled())
                        {
                            DocVersionElement dve = fsv.getDocVersionElement();
                            if (fsv.getStorageElement().isRendition())
                                history.setAction("FILESTORE_EDV_REVISION_DOWNLOAD");
                            else
                                history.setAction("FILESTORE_EDV_VERSION_DOWNLOAD");

                            history.addVariable("versionLabel", "" + dve.getVersionLabel());
                        }
                        else
                        {
                            if (fsv.getStorageElement().isRendition())
                                history.setAction("FILESTORE_REVISION_DOWNLOAD");
                            else
                                history.setAction("FILESTORE_VERSION_DOWNLOAD");
                        }
                        history.addVariable("versionNumber", "" + se.getVersionNumber());

                        history.setModifiedDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
                        history.setModifiedBy(SecurityCache.parseDomain(p.req.getRemoteUser()));
                        history.setACL(fs.getACL());

                        HistorySessionEJB history_ejb = ((HistorySessionEJBHome) ContextAdapter.getHome(ContextAdapter.HISTORYSESSION)).create();
                        history_ejb.createHistory(SonoraConstants.FILESTORE_REPOSITORY, null, history);
                        history_ejb = null;
                    }
                }
            }
        }
    }

    private void updateCase360jnlp(Map<String, String> arguments, HttpServletRequest req) throws Exception
    {
        String contextPath = getServletContext().getRealPath("/");
        File jnlpfile;
        if (CSRFGuardTokenResolver.isCsrfGuardEnabled()|| ContextAdapter.getBooleanValue("FORM_AUTHENTICATION", false))
        {
            String[] path = contextPath.split(".war");
            contextPath = path[0] + "API.war";
            jnlpfile = new File(contextPath + "/filestore/Case360.jnlp");
        }
        else
        {
            jnlpfile = new File(contextPath + "/filestore/Case360.jnlp");
        }
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(jnlpfile);

        NodeList jnlpTag = doc.getElementsByTagName("jnlp");

        if (jnlpTag.getLength() > 0)
        {
            Node jnlpnode = jnlpTag.item(0);
            NamedNodeMap attributes = jnlpnode.getAttributes();
            Node codebaseattribute = attributes.getNamedItem("codebase");
            if (null != codebaseattribute)
                codebaseattribute.setTextContent(decode(arguments.get("codeBase")));
        }

        // Get the argument element
        NodeList argumentList = doc.getElementsByTagName("argument");
        int length = argumentList.getLength();
        Iterator<String> iterator = arguments.keySet().iterator();
        for (int i = 0; i < length && iterator.hasNext(); i++)
        {
            String key = iterator.next();
            String value = arguments.get(key);
            String argvalue = key + ":" + value;
            Node argnode = argumentList.item(i);
            argnode.setTextContent(argvalue);
        }
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(jnlpfile);
        transformer.transform(source, result);
    }

    //
    // These are the operations that can download a file to the browser. They
    // all just pass on the request to
    // doDownload with an op parameter telling it the requested operation.
    //
    private void opEdit(OpContext p) throws Exception
    {
        doDownload(p, ACTION_EDIT, MODE_IMMEDIATE);
    }

    // Handle get/download request open for editing or viewing of the current
    // version
    private void opOpen(OpContext p) throws ServletException, IOException, Exception
    {
        // Edit if it is locked by the current user, otherwise, view.
        doDownload(p, ACTION_OPEN, MODE_IMMEDIATE);
    }

    private void opView(OpContext p) throws Exception
    {
        doDownload(p, ACTION_VIEW, MODE_IMMEDIATE);
    }

    // This is called by the download applet to fetch a file.
    private void opGetFile(OpContext p) throws Exception
    {
        doDownload(p, ACTION_GET, MODE_IMMEDIATE);
    }

    private void opDownload(OpContext p) throws Exception
    {
        doDownload(p, ACTION_DOWNLOAD, MODE_IMMEDIATE);
    }

    private void opUpScan(OpContext p) throws Exception
    {
        doDownload(p, ACTION_SCAN, MODE_IMMEDIATE);
    }

    private void opUpBlank(OpContext p) throws Exception
    {
        doDownload(p, ACTION_BLANK, MODE_IMMEDIATE);
    }

    private void opDownloadEdit(OpContext p) throws Exception
    {
        doDownload(p, ACTION_DOWNLOAD_EDIT, MODE_IMMEDIATE);
    }

    private void forwardToFilestoreDocumentViewJsp(OpContext p) throws Exception
    {
        UserContext userContext = new UserContext(p.req);
        userContext.setTz(p.getTimeZone());
        userContext.setLocale(UserElement.getUserLocale(p.req));
        userContext.setResources(p.resources);

        // The filestore element for this document
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            // Tell the user there is no such document
            p.out.println(p.resources.getString("FILESTORE_MISSING"));
            return;
        }

        // If we have a template, the user better have manage template rights.
        if (fs.getTemplateId() == null && !SecurityCache.hasPermission(CurrentUser.currentServletUser(p.req), SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, SecurityCache.SYSTEM_ACL_NAME))
        {
            throw new NoUserPermissionsException("FileStore Manage - " + SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S + " access denied user " + CurrentUser.currentServletUser(p.req));
        }

        FileStoreElement template = (fs.IsTemplate != FileStoreElement.TYPE_INSTANCE) ? fs : (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());

        if ((p.opIs("create") && template.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_UPLOAD) || (p.opIs("upload") && p.redisplay.equals("manage")))
        {
            p.req.setAttribute("topOp", "upload");
        }
        else if (p.opIs("create") && template.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_SCAN && p.useApplets())
        {
            p.req.setAttribute("topOp", "upscan");
        }
        else if (p.opIs("create") && template.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_BLANK && p.useApplets())
        {
            p.req.setAttribute("topOp", "upblank");
        }

        ArrayList versions = p.getVersionList();

        p.req.setAttribute("repository", String.valueOf(SonoraConstants.FILESTORE_REPOSITORY));
        if (!versions.isEmpty())
        {
            String documentSrc;

            FSVersionElement fsve = (FSVersionElement) versions.get(versions.size() - 1);

            int intVersion = fsve.getVersionNumber();
            String versionNumber = String.valueOf(intVersion);
            String storageId = fsve.getStorageElement() == null ? "" : fsve.getStorageElement().getStorageId().toString();
            p.req.setAttribute("documentVersion", versionNumber);

            // storageId attribute is used only for opening the document content
            // if it doesn't exist the Content panel is empty
            if (isNativeExtension(fsve.getExtensions()))
            {
                p.req.setAttribute("storageId", storageId);
            }

            if (fs.getIsLocked() && p.isLockedByCurrentUser())
            {
                if (p.useApplets() && p.isWIEFile(intVersion))
                {
                    documentSrc = "FileStore?op=edit&id=" + fsve.getDocumentID() + "&version=" + intVersion + (storageId.length() != 0 ? "&elId=" + storageId : "");
                    p.req.setAttribute("documentSrc", documentSrc);
                }
            }
        }

        p.req.setAttribute("userContext", userContext);

        RequestDispatcher disp = null;

        String jsp = UtilRequest.getParameter(p.req, "jsp");
        p.req.setAttribute("id", fs.getDocumentID().toString());

        if (StringUtil.isBlank(jsp))
        {
            disp = p.req.getRequestDispatcher(SonoraConstants.FILESTORE_DOCUMENT_VIEW_JSP);
        }
        else
        {
            disp = p.req.getRequestDispatcher("/filestore/" + jsp);
        }
        if (disp != null)
        {
            disp.forward(p.req, p.resp);
        }
    }

    private void forwardToOpenDocumentViewJsp(OpContext p) throws Exception
    {
        UserContext userContext = new UserContext(p.req);
        userContext.setTz(p.getTimeZone());
        userContext.setLocale(UserElement.getUserLocale(p.req));
        userContext.setResources(p.resources);

        ArrayList versions = p.getVersionList();

        p.req.setAttribute("repository", String.valueOf(SonoraConstants.FILESTORE_REPOSITORY));

        if (!versions.isEmpty())
        {
            String versionNumber = String.valueOf(((FSVersionElement) versions.get(versions.size() - 1)).getVersionNumber());
            p.req.setAttribute("documentVersion", versionNumber);
            p.req.setAttribute("storageId", String.valueOf(p.getVersionElement(versions.size()).getStorageElement().getStorageId()));
        }

        p.req.setAttribute("userContext", userContext);

        RequestDispatcher disp = null;
        String jsp = UtilRequest.getParameter(p.req, "jsp");
        if (StringUtil.isBlank(jsp))
        {
            disp = p.req.getRequestDispatcher(SonoraConstants.FILESTORE_OPEN_DOCUMENT_VIEW_JSP);
        }
        else
        {
            disp = p.req.getRequestDispatcher("/filestore/" + jsp);
        }
        if (disp != null)
        {
            disp.forward(p.req, p.resp);
        }
    }

    private boolean isNativeExtension(String extension)
    {
        // only file with native extension is tried to be opened on the Content
        // frame
        String nativeExtensionsProp = SystemApplicationProperties.getValue("NATIVE_FILE_TYPES", "");
        String[] nativeExtensions = nativeExtensionsProp.split(",", -1);
        if (nativeExtensions != null && nativeExtensions.length != 0)
        {
            for (int i = 0; i < nativeExtensions.length; i++)
            {
                if (nativeExtensions[i].trim().equalsIgnoreCase(extension))
                {
                    return true;
                }
            }
            return false;
        }
        else
        {
            return false;
        }
    }

    private boolean calculateIsInlineOrAttachmentFlagForContentDisposition(int action, String fileName)
    {
        // We do inline only if the operation is View and it is not a file type
        // that want to force to be saved
        // as a file. Otherwise do attachment to force the browser to save it as
        // a file.
        return ((action == ACTION_VIEW || action == ACTION_OPEN) && !MimeTypeEx.downLoadAsAttachment(fileName));
    }

    /**
     * Reconstruct document repository key from FileStoreElement.
     * 
     * @param fse
     *            FileStoreElement
     * @return document repository key
     */
    private String constructRepositoryKey(FileStoreElement fse)
    {
        String repositoryKey = String.valueOf(SonoraConstants.FILESTORE_REPOSITORY) + '.';
        if (fse.getIsTemplate() == FileStoreElement.TYPE_INSTANCE)
            repositoryKey = repositoryKey + fse.getTemplateId().toString() + '.';
        repositoryKey = repositoryKey + fse.getDocumentID().toString();

        return repositoryKey;
    }

    private VersionLabel generateNextVersionLable(DocVersionElement priorDocumentVersion)
    {

        VersionLabel versionLabel;

        // We should never get a null priorDve, but make sure something
        // reasonable happens if we do
        if (priorDocumentVersion != null)
        {
            versionLabel = new VersionLabel(priorDocumentVersion.getVersionLabel());
            versionLabel.increment();
        }
        else
        {
            versionLabel = new VersionLabel(); // Default Version String
        }

        return versionLabel;
    }

    /**
     * This little class represents a potential entry in the popup menu. If
     * every bit in the mask is set in the mask we are given, then this entry
     * will be included in the final pop-up menu.
     */
    static class MenuEntry
    {
        String url;
        int mask;
        boolean needView; // True if we have to compute include {{viewUrl}}
                          // string variable
        boolean needEdit; // True if we have to compute include {{editUrl}}
                          // string variable

        MenuEntry(String url, int mask)
        {
            this.url = url;
            this.mask = mask;
            this.needView = false;
            this.needEdit = false;
        }

        MenuEntry(String url, int mask, boolean needView, boolean needEdit)
        {
            this.url = url;
            this.mask = mask;
            this.needView = needView;
            this.needEdit = needEdit;
        }
    }

    // These values specify the various types of entries that appear in the
    // version list - it is used to select
    // the array of potential menu entries that apply.
    static final int MMT_EMPTY = 0; // empty document
    static final int MMT_INPROG = 1; // in progress version
    static final int MMT_TOP = 2; // topmost version in an unlocked document
    static final int MMT_TOPLOCK = 3; // topmost version in a locked document
    static final int MMT_PRIMARY = 4; // primary rendition
    static final int MMT_RENDITION = 5; // secondary rendition

    // These values are used as bitwise masks to select the menu entries to be
    // included for the given version (MM is short for Menu Mask)
    // All of the bits in the state mask must match (the state specifies all the
    // conditions which must be true for the menu to show)
    static final int MMS_PERM_UP = 0x00800000; // upload permission
    static final int MMS_PERM_ED = 0x00400000; // edit permission (or annotate
                                               // permission)
    static final int MMS_PERM_RD = 0x00200000; // read permission
    static final int MMS_PERM_CI = 0x00100000; // check in permission
    static final int MMS_PERM_UN = 0x00080000; // unlock permission (will be
                                               // false if the document is
                                               // locked by someone else and
                                               // current user does not have
                                               // manage object permission)
    static final int MMS_PERM_LK = 0x00040000; // lock permission (will be false
                                               // if the document is locked by
                                               // someone else)
    static final int MMS_PERM_DV = 0x00020000; // delete version permission
                                               // (different than delete
                                               // document permission)
    static final int MMS_PERM_AN = 0x00010000; // annotate permission
    static final int MMS_RES1 = 0x00008000; // reserved
    static final int MMS_RES2 = 0x00004000; // reserved
    static final int MMS_RES3 = 0x00002000; // reserved
    static final int MMS_RMAVAIL = 0x00001000; // records management is
                                               // installed and available for
                                               // this document
    static final int MMS_LOCKCU = 0x00000800; // document is locked to the
                                              // current user
    static final int MMS_ISARCHIVE = 0x00000400; // document is an archive of
                                                 // another object
    static final int MMS_NOTARCHIVE = 0x00000200; // document is not an archive
    static final int MMS_NEWSTYLE = 0x00000100; // new style document (uses
                                                // storage controllers/storage
                                                // elements)
    static final int MMS_VERDETAILS = 0x00000080; // document supports enhanced
                                                  // versioning or field
                                                  // versioning
    static final int MMS_ENHVER = 0x00000040; // document supports enhanced
                                              // versioning
    static final int MMS_RES4 = 0x00000020; // reserved
    static final int MMS_ISREC = 0x00000010; // this version is declared as a
                                             // record
    static final int MMS_NOTREC = 0x00000008; // this version is not declared as
                                              // a record
    static final int MMS_ISPUB = 0x00000004; // this version is published
    static final int MMS_NOTPUB = 0x00000002; // this version is not published
    static final int MMS_RES5 = 0x00000001; // reserved

    // This value is used to clear all the version specific bits in the state
    static final int MMS_RESET = ~(MMS_ISREC + MMS_NOTREC + MMS_ISPUB + MMS_NOTPUB);

    // This defines the lists of potential menu entries - the outer array is
    // indexed by the type of
    // entry in the version list, the inner array lists the menu entries. The
    // order of the list is
    // significant as it determines the order in which entries in the final menu
    // will appear.
    MenuEntry[][] menuEntries =
    {
            // Menu entries for an empty document (all they can do is upload a
            // new file) (will never exist for an archive object)
            { new MenuEntry("MENTRY_UPLOADFIRST", MMS_PERM_UP), new MenuEntry("MENTRY_UPSCANFIRST", MMS_PERM_UP), new MenuEntry("MENTRY_UPBLANKFIRST", MMS_PERM_UP), },

            // Menu entries for an in progress version (the document is locked)
            // (will never exist for an archive object)
            { new MenuEntry("MENTRY_EDIT", MMS_PERM_ED + MMS_LOCKCU, false, true), new MenuEntry("MENTRY_VDETAILS", MMS_PERM_RD + MMS_VERDETAILS), new MenuEntry("MENTRY_CHECKIN", MMS_PERM_CI + MMS_LOCKCU), new MenuEntry("MENTRY_UNLOCK", MMS_PERM_UN), },

            // Menu entries for the topmost version of an unlocked document
            // (this is the only version they can lock/checkout)
            { new MenuEntry("MENTRY_RESTORE", MMS_PERM_RD + MMS_ISARCHIVE), new MenuEntry("MENTRY_VIEW", MMS_PERM_RD + MMS_NOTARCHIVE, true, false), new MenuEntry("MENTRY_CHECKOUT", MMS_PERM_ED + MMS_PERM_LK + MMS_NOTARCHIVE), new MenuEntry("MENTRY_LOCK", MMS_PERM_LK + MMS_NOTARCHIVE), new MenuEntry("MENTRY_UPLOAD", MMS_PERM_UP + MMS_NOTARCHIVE), new MenuEntry("MENTRY_UPSCAN", MMS_PERM_UP + MMS_NOTARCHIVE), new MenuEntry("MENTRY_UPBLANK", MMS_PERM_UP + MMS_NOTARCHIVE), new MenuEntry("MENTRY_DOWNLOAD", MMS_PERM_RD + MMS_NOTARCHIVE), new MenuEntry("MENTRY_ADDRENDITION", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_NEWSTYLE), new MenuEntry("MENTRY_DECLARE", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL), new MenuEntry("MENTRY_SUPERSEDE", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL + MMS_ISREC), new MenuEntry("MENTRY_ADDSUPPORTING", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL + MMS_ISREC), new MenuEntry("MENTRY_PUBLISH", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_ENHVER + MMS_NOTPUB),
                    new MenuEntry("MENTRY_UNPUBLISH", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_ENHVER + MMS_ISPUB), new MenuEntry("MENTRY_VDETAILS", MMS_PERM_RD + MMS_VERDETAILS), new MenuEntry("MENTRY_SIGNATURE", MMS_PERM_RD + MMS_NEWSTYLE), new MenuEntry("MENTRY_DELVERSION", MMS_PERM_DV + MMS_NOTREC), },

            // Menu entries for the topmost version of a locked document
            // (similar to any other version, except they can't delete it)
            { new MenuEntry("MENTRY_RESTORE", MMS_PERM_RD + MMS_ISARCHIVE), new MenuEntry("MENTRY_VIEW", MMS_PERM_RD + MMS_NOTARCHIVE, true, false), new MenuEntry("MENTRY_DOWNLOAD", MMS_PERM_RD + MMS_NOTARCHIVE), new MenuEntry("MENTRY_ADDRENDITION", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_NEWSTYLE), new MenuEntry("MENTRY_DECLARE", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL), new MenuEntry("MENTRY_SUPERSEDE", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL + MMS_ISREC), new MenuEntry("MENTRY_ADDSUPPORTING", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL + MMS_ISREC), new MenuEntry("MENTRY_PUBLISH", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_ENHVER + MMS_NOTPUB), new MenuEntry("MENTRY_UNPUBLISH", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_ENHVER + MMS_ISPUB), new MenuEntry("MENTRY_VDETAILS", MMS_PERM_RD + MMS_VERDETAILS), new MenuEntry("MENTRY_SIGNATURE", MMS_PERM_RD + MMS_NEWSTYLE), },

            // Menu entries for a version's primary rendition
            { new MenuEntry("MENTRY_RESTORE", MMS_PERM_RD + MMS_ISARCHIVE), new MenuEntry("MENTRY_VIEW", MMS_PERM_RD + MMS_NOTARCHIVE, true, false), new MenuEntry("MENTRY_DOWNLOAD", MMS_PERM_RD + MMS_NOTARCHIVE), new MenuEntry("MENTRY_ADDRENDITION", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_NEWSTYLE), // You
                                                                                                                                                                                                                                                                                                        // can
                                                                                                                                                                                                                                                                                                        // declare
                                                                                                                                                                                                                                                                                                        // a
                                                                                                                                                                                                                                                                                                        // version
                                                                                                                                                                                                                                                                                                        // as
                                                                                                                                                                                                                                                                                                        // a
                                                                                                                                                                                                                                                                                                        // record
                                                                                                                                                                                                                                                                                                        // more
                                                                                                                                                                                                                                                                                                        // than
                                                                                                                                                                                                                                                                                                        // once!
                    new MenuEntry("MENTRY_DECLARE", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL), new MenuEntry("MENTRY_SUPERSEDE", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL + MMS_ISREC), new MenuEntry("MENTRY_ADDSUPPORTING", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_RMAVAIL + MMS_ISREC), new MenuEntry("MENTRY_PUBLISH", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_ENHVER + MMS_NOTPUB), new MenuEntry("MENTRY_UNPUBLISH", MMS_PERM_UP + MMS_NOTARCHIVE + MMS_ENHVER + MMS_ISPUB), new MenuEntry("MENTRY_VDETAILS", MMS_PERM_RD + MMS_VERDETAILS), new MenuEntry("MENTRY_SIGNATURE", MMS_PERM_RD + MMS_NEWSTYLE), new MenuEntry("MENTRY_DELVERSION", MMS_PERM_DV + MMS_NOTREC), },

            // Menu entries for a version's alternate renditions (there won't be
            // any on archive objects)
            { new MenuEntry("MENTRY_VIEW", MMS_PERM_RD + MMS_NOTARCHIVE, true, false), new MenuEntry("MENTRY_DOWNLOAD", MMS_PERM_RD + MMS_NOTARCHIVE), new MenuEntry("MENTRY_DELRENDITION", MMS_PERM_DV + MMS_NOTREC), // TODO:
                                                                                                                                                                                                                       // confirm
                                                                                                                                                                                                                       // that
                                                                                                                                                                                                                       // delete
                                                                                                                                                                                                                       // rendition
                                                                                                                                                                                                                       // should
                                                                                                                                                                                                                       // be
                                                                                                                                                                                                                       // disabled
                                                                                                                                                                                                                       // if
                                                                                                                                                                                                                       // it
                                                                                                                                                                                                                       // is
                                                                                                                                                                                                                       // a
                                                                                                                                                                                                                       // record
            }, };

    String getMenu(OpContext p, FSVersionElement fsv, int type, int state, StringVarResolverHashMap stringVars, StringVar stringVar) throws Exception
    {
        // Build the menu entries here
        StringBuilder menu = new StringBuilder();

        // Get a reference to the appropriate menu for this version list type
        MenuEntry entries[] = menuEntries[type];

        // Test if this entry is valid for these conditions we use the
        // StringBuffer to keep a list
        // of the entries (by their resource names) we've added so we don't add
        // one twice.
        String entry;
        for (int i = 0; i < entries.length; ++i)
        {
            if (!p.isIForWebAppletEnable() && entries[i].url != null && (entries[i].url.startsWith("MENTRY_UPSCAN") || entries[i].url.startsWith("MENTRY_UPBLANK")))
            {
                continue;
            }

            // The document must meet ALL of this entry's conditions for it to
            // appear
            if ((entries[i].mask & state) == entries[i].mask)
            {
                // Add the entry to the menu - if this is not an empty document,
                // generate the view and edit Urls for this element.
                // These Urls are more complex to generate as they need to vary
                // depending if the version is an image and I4Web is enabled.
                if (fsv != null)
                {
                    if (entries[i].needView)
                        stringVars.setVariable("viewUrl", getViewURL(p, fsv));
                    if (entries[i].needEdit)
                        stringVars.setVariable("editUrl", getEditURL(p, fsv));
                }

                UserContext userContext = new UserContext(p.req);
                ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
                String persona = (String) prop.get("persona").getValue();
                boolean hasPersona = persona != null && persona.length() != 0;
                String includeContentFrame = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");
                if ("1".equals(includeContentFrame) || ("2".equals(includeContentFrame) && hasPersona))
                {
                    if ("MENTRY_RESTORE".equals(entries[i].url))
                    {
                        entry = p.resources.getString("MENTRY_RESTORE_SELF");
                    }
                    else if ("MENTRY_UPSCANFIRST".equals(entries[i].url))
                    {
                        entry = p.resources.getString("MENTRY_UPSCANFIRST_CONTENT");
                    }
                    else if ("MENTRY_UPSCAN".equals(entries[i].url))
                    {
                        entry = p.resources.getString("MENTRY_UPSCAN_CONTENT");
                    }
                    else if ("MENTRY_UPBLANKFIRST".equals(entries[i].url))
                    {
                        entry = p.resources.getString("MENTRY_UPBLANKFIRST_CONTENT");
                    }
                    else if ("MENTRY_UPBLANK".equals(entries[i].url))
                    {
                        entry = p.resources.getString("MENTRY_UPBLANK_CONTENT");
                    }
                    else
                    {
                        entry = p.resources.getString(entries[i].url);
                    }
                }
                else
                {
                    entry = p.resources.getString(entries[i].url);
                }

                entry = L10nUtils.localizeTokens(stringVar.resolveNames(entry), userContext.getLocale());
                menu.append(entry);
            }
        }

        return menu.toString();
    }

    // Handle new version list request
    private void opVList(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        // Expression support
        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
        StringVar stringVar = new StringVar(stringVars);
        // This provides the caller with the ability to set an onLoad event
        // handler to cause something
        // to happen when the version list is displayed - this is used in a
        // number of operations to open
        // a new window to display something.
        stringVars.setVariable("onLoad", "onload=\"placeFocus();maybeClose();" + (p.onLoad == null ? "" : p.onLoad) + "\"");

        // Get the utilities string resource file for date/time conversion
        Locale locale = UserElement.getUserLocale(p.req);
        ResourceBundle myRes = ResourceBundle.getBundle("com.eistream.utilities.StringResource", locale);
        String pattern = myRes.getString("OUTDATETIMEPATTERN");

        // strip percents
        StringTokenizer st = new StringTokenizer(pattern, "%");
        StringBuilder newPattern = new StringBuilder();

        while (st.hasMoreTokens())
        {
            newPattern.append(st.nextToken());
        }
        pattern = newPattern.toString();
        ISimpleDateFormat sdf = CalendarFactory.getISimpleDateFormatInstance(pattern, locale);
        sdf.setTimeZone(p.getTimeZone());

        // Get current FileStore Element
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            p.out.println(p.resources.getString("FILESTORE_BLANK"));
            return;
        }

        // Note that we do not use PERM_DELETE because a template that has
        // instances does not allow deletion.
        // The deletion permission does not apply to contents but to the
        // enclosing object
        // (I do not think this is quite right - jpb).
        // boolean canDelete =
        // fs.Capabilities.indexOf(FileStoreElement.PERM_UPDATE) != -1;

        // Get the version list in version number order.
        ArrayList versions = p.getVersionList();
        int count = versions.size();

        // If the document is not empty and we get an empty list this means the
        // current user has no access to any of the versions. Put up an error
        // instead of a blank list.
        if (count == 0 && fs.getCurrentVersion() != 0)
        {
            returnPage(p, p.resources.getString("NO_PUBLISHED_IN_VERSION_LIST"));
            return;
        }

        FSVersionElement fsv = null;

        if (fs.isNewStyleDocument())
        {
            // Sort the list so the primary rendition appears last.
            Comparator comparator = new SortByRenditionFileName();
            Collections.sort(versions, comparator);
        }

        // Construct the document state mask with the information that does not
        // vary between entries in the vlist
        int state = 0;
        if (p.isLockedByCurrentUser())
        {
            state += MMS_LOCKCU;
        }
        if (fs.isEnhancedDocumentVersioningEnabled())
        {
            state += MMS_ENHVER;
        }
        if (fs.isEnhancedDocumentVersioningEnabled() || fs.isFieldVersioningEnabled())
        {
            state += MMS_VERDETAILS;
        }
        if (fs.isRecordsManagementEnabled())
        {
            state += MMS_RMAVAIL;
        }
        if (fs.isNewStyleDocument())
        {
            state += MMS_NEWSTYLE;
        }

        state += (fs.getRepositoryDocId() == null) ? MMS_NOTARCHIVE : MMS_ISARCHIVE;

        if (StringUtil.isBlank(fs.Capabilities))
        {
            fs.Capabilities = p.getFileStoreSessionEJB().getCapabilities(fs);
        }

        // Add in the permissions (note that edit is special - if the user does
        // not have edit, but has annotate
        // then we set the edit bit for any version that is an image (whew! what
        // a mess). We use three separate bits
        // in the state - MSS_PERM_ED which indicates that the user has real
        // edit permission, MSS_PERM_AN which
        // indicates if the user has real annotate permission, and MSS_PERM_EDOK
        // which is set for each version.
        String permissions = p.getPermissions();

        if (fs.Capabilities.indexOf("ED") != -1 || permissions.indexOf(Contract.PERM_WRITE) != -1) // Edit
        {
            state += MMS_PERM_ED;
        }
        if (fs.Capabilities.indexOf("LK") != -1) // Edit
        {
            state += MMS_PERM_LK;
        }
        if (fs.Capabilities.indexOf("AN") != -1) // Annotate
        {
            state += MMS_PERM_AN;
        }
        if (fs.Capabilities.indexOf("UP") != -1) // Upload
        {
            state += MMS_PERM_UP;
        }
        if (fs.Capabilities.indexOf("RD") != -1) // Read
        {
            state += MMS_PERM_RD;
        }
        if (fs.Capabilities.indexOf("CI") != -1) // Check in
        {
            state += MMS_PERM_CI;
        }
        if (fs.Capabilities.indexOf("UN") != -1) // Unlock
        {
            state += MMS_PERM_UN;
        }
        if (fs.Capabilities.indexOf("DV") != -1) // Delete version
        {
            state += MMS_PERM_DV;
        }

        stringVars.setVariable("documentID", p.sDocumentId);

        if (fs.Capabilities.indexOf("UP") != -1)
        {
            String uploadUrlResolved = stringVar.resolveNames(p.resources.getString("UPLOAD_ELEMENT"));
            uploadUrlResolved = L10nUtils.localizeTokens(uploadUrlResolved, locale);
            stringVars.setVariable("uploader", uploadUrlResolved);
        }
        else
        {
            stringVars.setVariable("uploader", "&nbsp");
        }

        // Determine if the user is allowed to lock the topmost version - there
        // is a special case if the top
        // version is an image, in which case they are allowed to lock it if
        // they have only annotate permission.
        // The session bean grants lock to any document if the user has annotate
        // permission, we must remove it
        // if the topmost version is not an image.
        // This is of interest only if the document is not currently locked.
        if (fs.Capabilities.indexOf("AN") != -1)
        {
            // If the user does not have write permission but they have Annotate
            // ...
            if (permissions.indexOf(Contract.PERM_WRITE) == -1 && permissions.indexOf(Contract.PERM_ANNOTATE) != -1)
            {
                // ... then if the topmost version is NOT an image (or if there
                // are no accessible versions), don't add edit
                boolean isImage = false;
                if (count > 0)
                {
                    // Get the topmost version from the version list
                    fsv = (FSVersionElement) versions.get(count - 1);
                    // If it is new-style, ask the storage element
                    if (fs.isOldStyleDocument())
                    {
                        isImage = fsv.isWIEFile();
                    }
                    else
                    {
                        StorageElement se = fsv.getStorageElement();
                        isImage = se.isWIEFile() || se.isMapElement();
                    }

                    // Turn on the lock permission only if it is an image
                    if (isImage)
                    {
                        state += MMS_PERM_ED;
                    }
                }
            }
        }

        // Set up the resolver with all of the document's properties.
        stringVars.setVariable("title", fs.Title != null ? HtmlUtil.toSafeHtml(fs.getTitle()) : "");
        stringVars.setVariable("titleHtml", fs.Title != null ? stringVar.resolveNames(p.resources.getString("LIST_VX_TITLE")) : "");

        // The code below builds two string buffers: the menus and the
        // versionList
        StringBuilder menus = new StringBuilder();
        StringBuilder versionList = new StringBuilder();

        // We write a separator before each version except the first one, we use
        // lastVersion to
        // track when we get a new version and comment is obtained from the
        // primary rendition of
        // a version, but only written just before the separator.
        boolean firstRow = true;
        int lastVersion = -1;
        String comment = "";

        // This is the index of the row we are working with, this is separate
        // from the loop variable
        // below since we sometimes synthesize a version and skip some of the
        // version elements.
        int index = 0;
        stringVars.setVariable("index", "" + index);

        // This string lists the supported extensions for use by getIcon
        String supportedExtensions = p.resources.getString("SUPPORTED_EXTENSIONS");

        // ***********************************************************************************
        // Display the "in progress" version
        //
        // If the document is locked, manufacture the first, in progress, row.
        // Note that image
        // documents will actually have a "real" in-progress version. Users
        // without write
        // permission do not see the inprogress version at all (it is implicitly
        // not published).
        //
        // We checked locked before checking for empty as it is possible to have
        // an empty, locked
        // document (create document, upload a version, lock it, then delete the
        // topmost version).
        if (fs.getIsLocked())
        {
            String lockedBy = fs.setUserIdOrDisplayName(fs.LockedBy);
            stringVars.setVariable("lockedBy", fs.LockedBy != null ? lockedBy : "");
            stringVars.setVariable("icon", HtmlUtil.toSafeHtml(getIcon(supportedExtensions, fs, null)));
            stringVars.setVariable("versionNumber", "");

            // We will need access to the topmost version to get information to
            // be displayed in the list.
            // Be aware if the user has only read permission and the topmost
            // version is not published, we
            // won't be able to get the version element.
            fsv = p.getVersionElement(fs.CurrentVersion);
            String versionListRow;
            if (p.isLockedByCurrentUser())
            {
                // If it is locked, but empty, there is nothing to edit!
                if (count > 0)
                {
                    stringVars.setVariable("editUrl", getEditURL(p, (FSVersionElement) versions.get(count - 1)));
                    versionListRow = p.resources.getString("LIST_VX_VERSION_INPROG_CURRENT_USER");
                }
                else
                {
                    versionListRow = p.resources.getString("LIST_VX_VERSION_INPROG_CURRENT_USER_EMPTY");
                }
            }
            else
            {
                versionListRow = p.resources.getString("LIST_VX_VERSION_INPROG_OTHER_USER");
            }
            versionList.append(L10nUtils.localizeTokens(stringVar.resolveNames(versionListRow), locale));

            if (fs.isEnhancedDocumentVersioningEnabled())
            {
                // The comment for this version row, is the checkout comment
                // from the current topmost version
                if (fsv != null)
                {
                    DocVersionElement dve = fsv.getDocVersionElement();
                    if (dve != null)
                    {
                        comment = dve.getCheckoutComment();
                    }
                }
                else
                {
                    comment = "";
                }
            }

            stringVars.setVariable("version", fsv == null ? "" : "" + fsv.getVersionNumber());

            // Build the menu for this row (if the user has access to it)
            stringVars.setVariable("actions", stringVar.resolveNames(getMenu(p, fsv, MMT_INPROG, state, stringVars, stringVar)));
            menus.append(stringVar.resolveNames(p.resources.getString("LIST_VX_MENU")));
            firstRow = false;
            ++index;
        }

        // *************************************************************************************
        // Display the empty document version
        //
        // We special case the first row for empty documents and in progress
        // (locked) documents
        else if (fs.CurrentVersion == 0)
        {
            stringVars.setVariable("dateCreated", sdf.format(fs.getCreatedDateTime()));
            stringVars.setVariable("createdBy", fs.CreatedBy);
            String versionListRow = stringVar.resolveNames(p.resources.getString("LIST_VX_VERSION_EMPTY"));
            versionList.append(L10nUtils.localizeTokens(versionListRow, locale));

            // Build the menu for this row
            stringVars.setVariable("actions", stringVar.resolveNames(getMenu(p, fsv, MMT_EMPTY, state, stringVars, stringVar)));
            menus.append(stringVar.resolveNames(p.resources.getString("LIST_VX_MENU")));
            firstRow = false;
            ++index;
        }

        // Set up information needed by the record manager functions. We want to
        // get the parent id
        // only once since it is the same for all versions and it is an
        // expensive operation.
        long parentId = 0;
        if (fs.isRecordsManagementEnabled())
        {
            LoginManager loginManager = null;

            try
            {
                FilePlanComponent fpc = null;
                if (fs.getComponentId() != null && fs.getComponentId().longValue() != 0)
                {
                    loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), locale);

                    // Get current component's grandparent.
                    FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
                    fpc = fpn.getParentComponent(fs.getFilePlanViewId().longValue(), fs.getComponentId().longValue());
                    if (fpc != null)
                    {
                        fpc = fpn.getParentComponent(fs.getFilePlanViewId().longValue(), fpc.getComponentId());
                        if (fpc != null)
                        {
                            parentId = fpc.getComponentId();
                        }
                    }
                }
            }
            finally
            {
                if (loginManager != null)
                {
                    loginManager.logout();
                }
            }
        }

        // ****************************************************************************************************
        //
        // Loop through all the versions of this document
        //
        for (int ii = 0; ii < count; ++ii, ++index)
        {
            // Get version element, skip it if we get null (should not happen)
            fsv = (FSVersionElement) versions.get(count - 1 - ii);
            if (fsv == null)
            {
                continue;
            }

            // We have to special case the first version row if this is an empty
            // document or a locked document
            if (ii == 0)
            {
                // If we have a new style image document and the document is
                // locked and the version of the
                // last element is the same as the nextVersion in the file store
                // element, do not display
                // this element (it has already been done).
                if (ii == 0 && fs.isNewStyleDocument() && (fsv.getStorageElement().isMapElement() || fs.isStandardVersioning()) && fs.getIsLocked() && fs.getNextVersion().intValue() == fsv.getVersionNumber() && !fsv.getStorageElement().isRendition())
                {
                    // Skip the currently checked out element.
                    continue;
                }
            }

            // If this is a new version - write out the comments and separator
            // from the previous version
            if (lastVersion != fsv.getVersionNumber())
            {
                if (!firstRow)
                {
                    if (comment != null && comment.length() != 0)
                    {
                        stringVars.setVariable("comment", HtmlUtil.toSafeHtml(comment));
                        versionList.append(stringVar.resolveNames(p.resources.getString("LIST_VX_VERSION_COMMENT")));
                    }
                    versionList.append(p.resources.getString("LIST_VX_VERSION_SEPARATOR"));
                }
                else
                {
                    firstRow = false;
                }

                // Update the current version number
                lastVersion = fsv.getVersionNumber();
            }

            // Add this version to the versionList, the HTML template expects
            // the following variables
            // to be available to the StringVar resolver: index, version,
            // fileName, published, date and user.
            stringVars.setVariable("index", "" + index);
            String version = "";
            String fileName = "";
            String published = "";
            String record = "";
            String date = "";
            String user = "";
            String elementID = "";

            // Each time through the loop, we reset the bits that are set
            // specific to the current version.
            state &= MMS_RESET;
            int type;

            // This specifies the name of the property with the StringVar
            // template needed for this type of row.
            String versionProperty;

            // Set up variables needed by the declare record function.
            if (fs.isRecordsManagementEnabled())
            {
                stringVars.setVariable("parentId", "" + parentId);
                stringVars.setVariable("viewId", fs.getFilePlanViewId().toString());
                stringVars.setVariable("componentDefId", fs.getComponentDefId().toString());
            }

            if (fs.isEnhancedDocumentVersioningEnabled())
            {
                // Get the specified version (or the current version if one was
                // not specified)
                DocVersionElement dve = fsv.getDocVersionElement();

                if (dve != null)
                {
                    version = dve.getVersionLabel();
                    comment = dve.getCheckInComment();
                    state += dve.isPublished() ? MMS_ISPUB : MMS_NOTPUB;

                    // The published icon should only appear for users who have
                    // permission to update the document
                    if ((state & MMS_PERM_UP) != 0)
                    {
                        published = p.resources.getString(dve.isPublished() ? "LIST_VX_VERSION_PUBLISHED_TRUE" : "LIST_VX_VERSION_PUBLISHED_FALSE");
                    }
                    state += dve.isRecord() ? MMS_ISREC : MMS_NOTREC;
                    record = p.resources.getString(dve.isRecord() ? "LIST_VX_VERSION_RECORD" : "LIST_VX_VERSION_NORECORD");
                }
                else
                {
                    version = "" + fsv.getVersionNumber();
                    comment = "";
                    state += MMS_NOTREC;
                }
            }
            else
            {
                version = "" + fsv.getVersionNumber();
                published = "";
                comment = "";
                state += MMS_NOTREC;
            }

            // Set up the variables needed to generate the version row based on
            // the type of this version.
            date = sdf.format(fsv.CreatedDateTime);
            user = fs.setUserIdOrDisplayName(fsv.CreatedBy);

            if (fs.isOldStyleDocument())
            {
                fileName = fsv.getFileName();
                comment = "";
                versionProperty = "LIST_VX_VERSION";

                // The entry type determines which menu is displayed
                type = (fsv.VersionNumber == fs.getCurrentVersion()) ? (fs.getIsLocked() ? MMT_TOPLOCK : MMT_TOP) : MMT_PRIMARY;
            }
            else
            {
                // If this is an image document (with page versioning, get the
                // filename from the page map
                StorageElement se = fsv.getStorageElement();
                elementID = "&elId=" + se.getStorageId().toString();

                if (se.isMapElement())
                {
                    DocumentMap documentMap = new DocumentMap(fs, se);
                    fileName = documentMap.getDocumentName();
                }
                else
                {
                    fileName = se.getFileName();
                }

                if (!se.isRendition())
                {
                    versionProperty = "LIST_VX_VERSION";
                    type = (fsv.VersionNumber == fs.getCurrentVersion()) ? (fs.getIsLocked() ? MMT_TOPLOCK : MMT_TOP) : MMT_PRIMARY;
                }
                else
                {
                    versionProperty = "LIST_VX_RENDITION";
                    type = MMT_RENDITION;
                }
            }

            // Set the variables needed by the version row and menu entries
            stringVars.setVariable("versionLabel", HtmlUtil.toSafeHtml(version));
            stringVars.setVariable("versionNumber", "" + fsv.getVersionNumber());
            stringVars.setVariable("fileName", HtmlUtil.toSafeHtml(fileName));
            stringVars.setVariable("elementID", elementID);
            stringVars.setVariable("published", L10nUtils.localizeTokens(stringVar.resolveNames(published), locale));
            stringVars.setVariable("record", L10nUtils.localizeTokens(stringVar.resolveNames(record), locale));
            stringVars.setVariable("date", date);
            stringVars.setVariable("user", user);
            stringVars.setVariable("icon", HtmlUtil.toSafeHtml(getIcon(supportedExtensions, fs, fsv)));
            stringVars.setVariable("viewUrl", getViewURL(p, fsv));

            // Construct the version row and add it to the list.
            String versionListRow = stringVar.resolveNames(p.resources.getString(versionProperty));
            versionList.append(L10nUtils.localizeTokens(versionListRow, locale));

            // Construct the menu for this row
            stringVars.setVariable("actions", stringVar.resolveNames(getMenu(p, fsv, type, state, stringVars, stringVar)));
            menus.append(stringVar.resolveNames(p.resources.getString("LIST_VX_MENU")));
        }

        UserContext userContext = new UserContext(p.req);
        ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
        String persona = (String) prop.get("persona").getValue();
        boolean hasPersona = persona != null && persona.length() != 0;
        String includeContentFrameProp = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");

        // some actions (like uploading a new version) should cause the Content
        // frame refreshing
        if ("1".equals(includeContentFrameProp) || ("2".equals(includeContentFrameProp) && hasPersona))
        {
            if (count > 0)
            {
                FSVersionElement fsve = (FSVersionElement) versions.get(versions.size() - 1);
                int intVersion = fsve.getVersionNumber();
                String documentId = fsve.getDocumentID().toString();
                String storageId = fsve.getStorageElement() == null ? "" : fsve.getStorageElement().getStorageId().toString();

                if ((p.opIs("null") || p.opIs("delVersion") || p.opIs("unlock") || (p.opIs("postUpload") && p.isOnlyIForWebAppletEnable())) && p.onLoad == null && "vlist".equals(p.redisplay))
                {
                    String onLoadPart;
                    if (isNativeExtension(fsve.getExtensions()))
                    {   if(CSRFGuardTokenResolver.isCsrfGuardEnabled())
                       {
                        String url = CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, ".location='FileStore?op=view&id=" + documentId + "&version=" + intVersion + (storageId.length() != 0 ? "&elId=" + storageId : "")); 
                        onLoadPart = "if (window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + " != null) " + "{window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID +url + "'}";                       
                       }else {
                           onLoadPart = "if (window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + " != null) " + "{window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID +".location='FileStore?op=view&id=" + documentId + "&version=" + intVersion + (storageId.length() != 0 ? "&elId=" + storageId : "")+ "'}";
                       }
                    
                    }
                    else
                    {
                        onLoadPart = "var contentFrame = window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + ";" + "if (contentFrame != null) {contentFrame.location='about:blank'}";
                    }

                    stringVars.setVariable("onLoad", "onload=\"placeFocus();maybeClose();" + onLoadPart + "\"");
                }
            }
            else
            {
                if ((p.opIs("delVersion") || p.opIs("unlock")) && p.onLoad == null && "vlist".equals(p.redisplay))
                {
                    String onLoadPart = "var contentFrame = window.parent." + SonoraConstants.FILESTORE_CONTENT_FRAME_ID + ";" + "if (contentFrame != null) {contentFrame.location='about:blank'}";
                    stringVars.setVariable("onLoad", "onload=\"placeFocus();maybeClose();" + onLoadPart + "\"");
                }
            }
        }

        // Don't forget to tack on the comment for the last version (if there is
        // one)
        if (comment != null && comment.length() != 0)
        {
            stringVars.setVariable("comment", HtmlUtil.toSafeHtml(comment));
            versionList.append(stringVar.resolveNames(p.resources.getString("LIST_VX_VERSION_COMMENT")));
        }

        // Merge the menus and versionList with the HTML body of the page
        stringVars.setVariable("menus", menus.toString());
        stringVars.setVariable("versionList", versionList.toString());

        if ("Legacy".equals(UXThemeResolver.getTheme()))
        {
            stringVars.setVariable("theadClassname", "");
        }
        else
        {
            stringVars.setVariable("theadClassname", " class=\"dialog\"");
        }

        String localJS = L10nUtils.getLocaleJS(locale);
        stringVars.setVariable("LOCALE_JS", localJS);

        p.out.println(L10nUtils.localizeTokens(stringVar.resolveNames(p.resources.getString("LIST_VX")), locale));
    }

    // Handle the manage document request (for either instances or templates),
    // this returns the appropriate frameset
    // For templates we always return the built-in template, for instances we
    // can get custom templates.
    private void opManage(OpContext p) throws Exception
    {
        // The filestore element for this document
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            // Tell the user there is no such document
            p.out.println(p.resources.getString("FILESTORE_MISSING"));
            return;
        }

        // If we have a template, the user better have manage template rights.
        if (fs.getTemplateId() == null && !SecurityCache.hasPermission(CurrentUser.currentServletUser(p.req), SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, SecurityCache.SYSTEM_ACL_NAME))
        {
            throw new NoUserPermissionsException("FileStore Manage - " + SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S + " access denied user " + CurrentUser.currentServletUser(p.req));
        }

        // Before doing anything else, log a read only history event (if that's
        // been configured in the template).
        // Get the document's template element (which is itself if it is a
        // template)
        FileStoreElement template = (fs.IsTemplate != FileStoreElement.TYPE_INSTANCE) ? fs : (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());
        if (template.isReadOnlyHistoryEnabled())
        {
            HistoryRecord history = new HistoryRecord();
            history.setObjectId(fs.getDocumentID());
            history.setComponentType(0);
            history.setComponentId(null);
            history.setAction("FSDOC_ACCESSED");
            history.setModifiedDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
            history.setModifiedBy(SecurityCache.parseDomain(p.req.getRemoteUser()));
            history.setACL(fs.getACL());

            HistorySessionEJB history_ejb = ((HistorySessionEJBHome) ContextAdapter.getHome(ContextAdapter.HISTORYSESSION)).create();
            history_ejb.createHistory(SonoraConstants.FILESTORE_REPOSITORY, null, history);
            history_ejb = null;
        }

        if (fs.getTemplateName() != null)
            StatisticsCache.bumpCounter(String.valueOf(SonoraConstants.FILESTORE_REPOSITORY) + "." + fs.getTemplateName() + "." + FileStoreElement.FSCOUNTER_VIEWINGS);

        // Get the frameset to be used, templates always use the default
        // frameset
        String frameset = null;
        if (fs.IsTemplate == FileStoreElement.TYPE_INSTANCE)
        {
            // If they've specified an fs= parameter, that's the frameset we'll
            // use
            String framesetName = p.getParameter(p.req, "fs");

            // If the frameset name didn't resolve for some reason, we can't use
            // it
            if (framesetName != null && framesetName.equalsIgnoreCase("{{frameset}}"))
                framesetName = null;

            if (framesetName != null)
                frameset = p.resources.getString("FRAMESET_" + framesetName);

            // If we don't have a frameset, ask the session bean to get the
            // frameset
            if (frameset == null)
            {
                SonoraForm framesetForm = p.getFileStoreSessionEJB().getFrameset(fs, UserElement.getUserLocale(p.req));
                if (framesetForm != null)
                    frameset = framesetForm.getRawString();
            }

            // If we still don't have a frameset, use the default frameset
            if (frameset == null)
            {
                frameset = p.resources.getString("FRAMESET_default");
            }
        }
        else if ((fs.IsTemplate == FileStoreElement.TYPE_TEMPLATE || fs.IsTemplate == FileStoreElement.TYPE_PUBLISHED_TEMPLATE) && fs.repositoryDocId != null)
        {
            frameset = p.resources.getString("FRAMESET_archivetemplate");
        }
        else
        {
            frameset = CSRFGuardTokenResolver.addCsrfTokenToResponse(p.resources.getString("FRAMESET_template"), "frame", "src", p.req);
        }
        if (!p.opIs("create") || template.getInitialAction() == FileStoreElement.INITIAL_ACTION_NONE)
            p.stringVars.setVariable("topOp", "vlist");
        else if (template.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_UPLOAD)
            p.stringVars.setVariable("topOp", "upload");
        else if (template.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_SCAN)
            p.stringVars.setVariable("topOp", "upscan");
        else if (template.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_BLANK)
            p.stringVars.setVariable("topOp", "upblank");

        // Return the frameset with the appropriate variable resolution
        returnPage(p, frameset);
    }

    // Create a new FileStore template from the specified document
    private void opMakeTemplate(OpContext p) throws Exception
    {
        // Create template, update the request parameters to reference the new
        // template
        p.iDocumentId = p.getFileStoreSessionEJB().makeTemplate(p.iDocumentId);
        p.sDocumentId = p.iDocumentId.toString();

        // Flush the affected caches.
        CacheSend flushcache = new CacheSend();
        flushcache.sendCommand(CacheConstants.FIELDCACHE);
        flushcache.sendCommand(CacheConstants.TEMPLATESCACHE);

        // Open the new document
        opManage(p);
    }

    // Return the Filestore properties page
    private void opProperties(OpContext p, FileStoreProperties filestoreProperties) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        // Get the document's FileStore element, just display a blank page if we
        // can't get it.
        FileStoreElement fs = p.getFileStoreElement();

        if (fs == null)
        {
            p.out.println(p.resources.getString("FILESTORE_BLANK"));
            return;
        }

        // Set the createdBy parameter according to GENERAL_USE_DISPLAYNAME
        fs.CreatedBy = fs.setUserIdOrDisplayName(fs.CreatedBy);
        // Setup a StringVar resolver and add the document's property page to
        // the resolver chain
        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
        StringVar varResolver = new StringVar(stringVars);

        if (filestoreProperties == null)
            filestoreProperties = new FileStoreProperties(fs);
        PropertyAsStringResolver filestorePropResolver = new PropertyAsStringResolver(filestoreProperties);
        stringVars.chainResolvers(filestorePropResolver);

        // Check if the user is allowed to update the document
        if (StringUtil.isBlank(fs.Capabilities))
        {
            // For one single check(PERM_UPDATE) do we need to call
            // getCapabilities??.
            fs.Capabilities = p.getFileStoreSessionEJB().getCapabilities(fs);
        }
        boolean canUpdate = fs.Capabilities.indexOf(FileStoreElement.PERM_UPDATE) != -1;

        // Set the DocumentID
        stringVars.setVariable("DocumentID", p.sDocumentId);

        stringVars.setVariable("DISABLED", "DISABLED");

        if (fs.IsTemplate == 1)
            stringVars.setVariable("DISABLED", "");

        Locale locale = UserElement.getUserLocale(p.req);
        ResourceBundle rb = ResourceBundle.getBundle("com.eistream.sonora.filestore.StorageServletResource", locale);
        PoolCandidates poolCandidates = new PoolCandidates(rb);

        if (fs.isNewStyleDocument())
        {
            int poolId = fs.getPoolId().intValue();

            stringVars.setVariable("masterPoolIdoriginal", fs.getPoolId().toString());

            String masterSelectionList = null;
            stringVars.setVariable("pathPrefix", "");
            stringVars.setVariable("typeIsPool", p.resources.getString("TYPE_POOL"));
            stringVars.setVariable("typeIsPath", "");

            if (canUpdate)
            {
                masterSelectionList = poolCandidates.getStorageControllerCandidates(poolId);
            }
            else
            {
                PoolElement pe = StorageManagerCache.getPoolElement(poolId);
                masterSelectionList = pe.getName();
            }

            stringVars.setVariable("masterCandidates", masterSelectionList);

            // No conversion is necessary.
            stringVars.setVariable("convert", "");
        }
        else
        {
            // We have an old fashioned document or template.
            stringVars.setVariable("masterPoolIdoriginal", "");
            stringVars.setVariable("masterCandidates", "");
            stringVars.setVariable("typeIsPath", p.resources.getString("TYPE_PATH"));
            stringVars.setVariable("typeIsPool", "");
            stringVars.setVariable("versioningType", "");
            stringVars.setVariable("fullTextType", "");

            if (fs.getTemplateId() == null)
            {
                // We have a template. See if conversion is already in progress
                // for this template, if it is not
                // add the conversion button.
                ConversionRequest cr = new ConversionRequest(fs);
                if (cr.exists())
                {
                    stringVars.setVariable("convert", "");
                }
                else
                {
                    int poolId = StorageManagerCache.getDefaultPoolElement().getPoolId();
                    String masterSelectionList = poolCandidates.getStorageControllerCandidates(poolId, "convertPoolId");
                    stringVars.setVariable("convertControllerCandidates", masterSelectionList);
                    stringVars.setVariable("convert", varResolver.resolveNames(p.resources.getString("CONVERT_DOCUMENTS")));
                }
            }
        }

        BigDecimal viewId = fs.getFilePlanViewId();
        if (viewId == null)
        {
            viewId = new BigDecimal("0");
        }
        stringVars.setVariable(FileStoreProperties.VIEWID + "original", viewId.toString());

        // See if we have a template.
        if (fs.getTemplateId() == null)
        {
            // Set the enhanced document versioning level.
            String enhancedDocumentVersioningType = varResolver.resolveNames(p.resources.getString("ENHANCED_DOCUMENT_VERSIONING_TYPE"));
            stringVars.setVariable("enhancedDocumentVersioningType", L10nUtils.localizeTokens(enhancedDocumentVersioningType, locale));
            String readonlyViewPublished = varResolver.resolveNames(p.resources.getString("EDV_READONLY_VIEW_PUBLISHED"));
            stringVars.setVariable("edvReadOnlyViewPublishedType", L10nUtils.localizeTokens(readonlyViewPublished, locale));

            // Set the records management selection but only if records
            // management is available.
            if (RecordsUtil.isRecordsManagerAvailable())
            {
                LoginManager loginManager = null;
                try
                {
                    String user = SecurityCache.parseDomain(p.req.getRemoteUser());
                    loginManager = RecordsUtil.getLoginManager(user, locale);

                    if (RecordsUtil.isAutoComponentDefinitionEnabled())
                    {
                        // Prompt for the view so we can create the
                        // record component definition
                        FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
                        String filePlanViewList = fpn.getFilePlanViewSelectionList(fs.getFilePlanViewId(), FileStoreProperties.VIEWID, locale, p.resources);

                        stringVars.setVariable("recordsManagementType", filePlanViewList);
                    }
                    else
                    {
                        // Ask user to select an existing component definition.
                        RecordDefinition rd = new RecordDefinition(loginManager);
                        String componentList = rd.getRecordDefinitionSelectionList(fs.getComponentDefId(), FileStoreProperties.COMPONENTDEFID, locale, p.resources);
                        stringVars.setVariable(FileStoreProperties.COMPONENTDEFID, componentList);

                        stringVars.setVariable("recordsManagementType", componentList);
                    }
                }
                finally
                {
                    if (loginManager != null)
                        loginManager.logout();
                }
            }
            else
            {
                stringVars.setVariable("recordsManagementType", "");
            }

            if (fs.isNewStyleDocument())
            {
                // Set the image page level versioning pick (this is not
                // available if the document has any versions because
                // if there is an image file checked in using page level
                // versioning, changing to regular version will loose
                // data (we'll reference only the map file, and won't be able to
                // get to the pages)).
                String versioningType = varResolver.resolveNames(p.resources.getString("VERSIONING_TYPE"));
                stringVars.setVariable("versioningType", L10nUtils.localizeTokens(versioningType, locale));

                // Set the full text pick.
                String fullTextType = "";
                // Put full text pick if supported for the database and the
                // rules allow it.
                if (DatabaseServer.isFullTextIndexingSupported())
                {
                    // Currently full text is supported only for Oracle.
                    int dbms = ContextAdapter.getDBType();
                    if (dbms == ContextAdapter.ORACLE)
                    {
                        // Primary pool must be magnetic.
                        PoolElement pe = StorageManagerCache.getPoolElement(p.getFileStoreElement().getPoolId().intValue());
                        if (StorageManagerCache.isMagneticDataManager(pe.getDataManagerId()))
                        {
                            String fullTextTypeResource = varResolver.resolveNames(p.resources.getString("FULL_TEXT_TYPE"));
                            fullTextType = L10nUtils.localizeTokens(fullTextTypeResource, locale);
                        }
                    }
                }

                stringVars.setVariable("fullTextType", fullTextType);

                // Set the encryption option but only if there is an encryption
                // class defined.
                if (isCryptAvailable())
                {
                    String encryptionType = varResolver.resolveNames(p.resources.getString("ENCRYPTION_TYPE"));
                    stringVars.setVariable("encryptionType", L10nUtils.localizeTokens(encryptionType, locale));
                }
                else
                {
                    stringVars.setVariable("encryptionType", "");
                }
            }
            else
            {
                stringVars.setVariable("versioningType", "");
                stringVars.setVariable("fullTextType", "");
                stringVars.setVariable("encryptionType", "");
            }

            stringVars.setVariable("initialActionNone", fs.getInitialAction() == FileStoreElement.INITIAL_ACTION_NONE ? "selected" : "");
            stringVars.setVariable("strActionNone", p.resources.getString("INITIAL_ACTION_NONE"));
            stringVars.setVariable("initialActionUpload", fs.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_UPLOAD ? "selected" : "");
            stringVars.setVariable("strActionUpload", p.resources.getString("INITIAL_ACTION_AUTOUPLOAD"));
            stringVars.setVariable("initialActionScan", fs.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_SCAN ? "selected" : "");
            stringVars.setVariable("strActionScan", p.resources.getString("INITIAL_ACTION_AUTOSCAN"));
            stringVars.setVariable("initialActionBlank", fs.getInitialAction() == FileStoreElement.INITIAL_ACTION_AUTO_BLANK ? "selected" : "");
            stringVars.setVariable("strActionBlank", p.resources.getString("INITIAL_ACTION_AUTOBLANK"));
            stringVars.setVariable("initialAction", L10nUtils.localizeTokens(varResolver.resolveNames(p.resources.getString("INITIAL_ACTION")), locale));

            String readOnlyHistory = varResolver.resolveNames(p.resources.getString("READ_ONLY_HISTORY"));
            stringVars.setVariable("readOnlyHistory", L10nUtils.localizeTokens(readOnlyHistory, locale));

            // Set field versioning option.
            String fieldVersioningType = varResolver.resolveNames(p.resources.getString("FIELD_VERSIONING_TYPE"));
            stringVars.setVariable("fieldVersioningType", L10nUtils.localizeTokens(fieldVersioningType, locale));

            // Set the archive options.
            String archiveOptions = varResolver.resolveNames(p.resources.getString("ARCHIVE_OPTIONS"));
            stringVars.setVariable("archiveOptions", L10nUtils.localizeTokens(archiveOptions, locale));
        }
        else
        {
            // We have an document instance, not a template - all these options
            // are unavailable in instances
            stringVars.setVariable("versioningType", "");
            stringVars.setVariable("fullTextType", "");
            stringVars.setVariable("fieldVersioningType", "");
            stringVars.setVariable("enhancedDocumentVersioningType", "");
            stringVars.setVariable("edvReadOnlyViewPublishedType", "");
            stringVars.setVariable("recordsManagementType", "");
            stringVars.setVariable("archiveOptions", "");
            stringVars.setVariable("initialAction", "");
            stringVars.setVariable("encryptionType", "");
        }

        // Add the make template button if the user has appropriate permissions
        boolean canMakeTemplate = fs.Capabilities.indexOf(FileStoreElement.PERM_MAKETEMPLATE) != -1;
        String mtButton = canMakeTemplate ? varResolver.resolveNames(p.resources.getString("MAKETEMPLATE_BUTTON")) : "";
        stringVars.setVariable("MtButton", L10nUtils.localizeTokens(mtButton, locale));

        // Get the approprate HTML template from the resource bundle (it is
        // different for templates vs instances
        // Also get the appropriate submit button
        String pageName = null;
        String buttonName = null;
        if (fs.IsTemplate != FileStoreElement.TYPE_INSTANCE)
        {
            // Get the HTML template for "Template Properties" (it is different
            // for an archive template vs a standard template)
            if (fs.getRepositoryDocId() == null)
                pageName = "TEMPLATE_PROPERTIES_DISPLAY";
            else
                pageName = "ARCHIVE_TEMPLATE_PROPERTIES_DISPLAY";
            buttonName = "SUBMIT_BUTTON_TEMPLATE";
        }
        else
        {
            // Get HTML Resources for "Instance Properties"
            pageName = "INSTANCE_PROPERTIES_DISPLAY";
            buttonName = "SUBMIT_BUTTON_INSTANCE";
        }

        // Add the submit button
        stringVars.setVariable("submitButton", canUpdate ? L10nUtils.localizeTokens(p.resources.getString(buttonName), locale) : "");

        String localeJS = L10nUtils.getLocaleJS(locale);
        stringVars.setVariable("LOCALE_JS", localeJS);

        // Get the appropriate properties page form, fill it in and send it to
        // the client

        String output = CSRFGuardTokenResolver.addCsrfTokenToResponse(varResolver.resolveNames(p.resources.getString(pageName), locale, p.getTimeZone()), "form", "action", p.req);
        p.out.println(L10nUtils.localizeTokens(output, locale));
    }

    // returns true if there is a class that implements the IFilestoreCrypto
    // interface and false if there is not.
    private boolean isCryptAvailable()
    {
        try
        {
            String className = ContextAdapter.getAdapterValue("FILESTORECRYPTOCLASS", null);
            if (StringUtil.isBlank(className))
            {
                return false;
            }

            Class c = Class.forName(className);

            if (Class.forName("com.eistream.sonora.filestore.IFilestoreCrypto").isAssignableFrom(c))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // Update a Document
    private void opPostProperties(OpContext p, PrintWriter out) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        TimeZone tz = p.getTimeZone();

        FileStoreProperties filestoreProperties = new FileStoreProperties(p.getFileStoreElement());

        String convert = p.getParameter(p.req, "Convert");
        // String exitHow = p.getParameter(p.req, "exitHow");

        boolean isValid = filestoreProperties.fromHttp(p.req, UserElement.getUserLocale(p.req), tz);
        if (!isValid && filestoreProperties.getInvalidMessage() != null)
        {
            Property[] props = filestoreProperties.getAllProperties();
            if (props != null)
            {
                for (int i = 0; i < props.length; i++)
                {
                    Property prop = props[i];
                    if (prop.getInvalidMessage() != null && prop.isValid() == false)
                    {
                        throw new SonoraIllegalArgumentException(SonoraIllegalArgumentException.PARAM_PROPERTY, prop.getName() + " : " + prop.getLastUserEntry() + " - " + prop.getInvalidMessage());
                    }
                }
            }
        }

        FileStoreElement fs = filestoreProperties.getFileStoreElement();
        filestoreProperties.SetUser(SecurityCache.parseDomain(p.req.getRemoteUser()));
        filestoreProperties.setLocale(UserElement.getUserLocale(p.req));

        // Update FileStore Element...
        ArrayList conflicts = new ArrayList();
        if (!filestoreProperties.saveChanges(conflicts))
        {
            ConflictResolutionPage conflictsPage = new ConflictResolutionPage(conflicts);

            // Process and output HTML and return back here
            p.out.println(conflictsPage.getHtml("FileStore?op=properties&id=" + p.sDocumentId, UserElement.getUserLocale(p.req), tz));
            return;
        }
        else // No Conflicts
        {
            // Convert document if required.
            if (convert != null)
            {
                // We have been asked to convert all the documents based on this
                // template to the new (storage manager) structure.
                // See if conversion is already in progress for this template.
                // Note that while the form was up, someone else may
                // have started a conversion, so we better check again.
                boolean useStandardVersioning;
                String versioningLevel = p.getParameter(p.req, "versioningLevel");
                if (versioningLevel == null)
                    useStandardVersioning = true;
                else
                    useStandardVersioning = false;

                String pool = p.getParameter(p.req, "convertPoolId");
                int poolId;
                if (pool == null)
                {
                    poolId = StorageManagerCache.getDefaultPoolElement().getPoolId();
                }
                else
                {
                    poolId = Integer.parseInt(pool);
                }

                fs = filestoreProperties.getFileStoreElement();
                ConversionRequest cr = new ConversionRequest(fs, poolId, useStandardVersioning);

                // Queue conversion request if there is not one already queued.
                cr.queueIfAbsent();
            }
        }
        opProperties(p, null);
    }

    // Process a document delete request.
    private void opDeleteDocument(OpContext p) throws Exception
    {
        try
        {
            if (p.sVersion == null || p.sVersion.equals("0"))
            {
                // Delete the whole document
                p.getFileStoreSessionEJB().deleteElement(p.iDocumentId);
            }
            else
            {
                // Delete just the requested version.
                opDelVersion(p);
            }
        }
        catch (Exception e)
        {
            String norurl = p.getParameter(p.req, "norurl");
            if (!StringUtil.isBlank(norurl))
                p.resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    // This displays the extension map page, allowing users to manage the
    // template's extension map
    private void opExtensionMaps(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        FileStoreElement fs = p.getFileStoreElement();

        // Create the StringVar objects
        StringVarResolverHashMap hashTable = new StringVarResolverHashMap();
        StringVar stringVar = new StringVar(hashTable);

        // Only users with manage template permissions can modify the extension
        // map.
        boolean readOnly = !SecurityCache.hasPermission(p.user, SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, fs.ACL, fs, false);

        // Get the list of extension map rules
        ArrayList rules = FileExtensionMapCache.getExtensionMaps(p.iDocumentId);

        // Build the extension map list
        StringBuilder extensionList = new StringBuilder();
        String extensionMapTemplate = p.resources.getString(readOnly ? "EXTENSION_MAP_READ_ONLY" : "EXTENSION_MAP");

        // Make sure the templateId (the documentId) is in the resolver
        // variables
        hashTable.setVariable("templateId", p.sDocumentId);

        int count = rules.size();
        for (int ii = 0; ii < count; ii++)
        {
            // Get the extension map rules
            FileExtensionMap fr = (FileExtensionMap) rules.get(ii);
            String transformClass = fr.get("transformClass");
            String viewerURL = fr.get("viewerURL");
            String ix = fr.get("indexRendition");

            // Setup the StringVar resolver variables from the rules (note that
            // DELETE_EXTENSION_MAP needs templateId and extension to be set up
            // first)
            hashTable.setVariable("index", "" + (ii + 1));
            hashTable.setVariable("extension", fr.getExtension());
            hashTable.setVariable("deleteExtensionMapButton", stringVar.resolveNames(p.resources.getString("DELETE_EXTENSION_MAP")));
            hashTable.setVariable("transformClass", transformClass == null ? "" : transformClass);
            hashTable.setVariable("viewerURL", viewerURL == null ? "" : viewerURL);
            hashTable.setVariable("checked", (ix != null && !ix.equals("0")) ? "checked" : "");

            // Get the Transform class options
            String transformClassOptions = null;
            if (transformClass != null && transformClass.length() > 0)
            {
                try
                {
                    FileTransform ft = FileExtensionMapCache.getTransformClass(p.iDocumentId, fr.getExtension());
                    transformClassOptions = ft.getOptionsAsHtmlDisplay(UserElement.getUserLocale(p.req), p.getTimeZone());
                }
                catch (Exception e)
                {
                    m_Log.error("opExtensionMaps", "E_EXCEPTION", e);
                    transformClassOptions = XmlUtil.encodeString(e.getLocalizedMessage(), true);
                }
                hashTable.setVariable("OptionsDisplayStyle", "");
            }
            else
            {
                hashTable.setVariable("OptionsDisplayStyle", "style=\"display:none;\"");
            }
            hashTable.setVariable("transformClassOptions", transformClassOptions == null ? "&nbsp;" : transformClassOptions);

            // Generate the html for this extension
            String map = stringVar.resolveNames(extensionMapTemplate);
            extensionList.append(map);
        }

        // Set up the resolver variables needed to build the page that contains
        // this extension list
        hashTable.setVariable("extensionList", extensionList.toString());
        hashTable.setVariable("newExtensionButton", stringVar.resolveNames(p.resources.getString("MAKE_NEW_EXTENSION_BUTTON")));

        if (ContextAdapter.getDBType() == ContextAdapter.ORACLE)
        {
            String excludeList = FileExcludeListCache.getExcludeList(p.sDocumentId);
            hashTable.setVariable("excludeList", excludeList == null ? "" : excludeList.trim());
            hashTable.setVariable("excludeList", stringVar.resolveNames(p.resources.getString(readOnly ? "EXCLUDE_LIST_READ_ONLY" : "EXCLUDE_LIST")));
        }
        else
            hashTable.setVariable("excludeList", "");

        // Fill in the HTML template and write it to the browser.
        String localeJS = L10nUtils.getLocaleJS(UserElement.getUserLocale(p.req));
        hashTable.setVariable("LOCALE_JS", localeJS);

        p.out.println(stringVar.resolveNames(p.resources.getString(readOnly ? "EXTENSION_MAPS_READ_ONLY" : "EXTENSION_MAPS")));
    }

    // This displays the Imaging Options page, allowing users to manage the
    // template's imaging options
    private void opPostImagingOptions(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        FileStoreElement fs = p.getFileStoreElement();

        ImagingOptions imagingOptions = ImagingOptions.fromXml(fs.getImagingOptions());
        ImagingOptionsProperties imagingOptionsProperties = new ImagingOptionsProperties(p.iDocumentId, imagingOptions, new ResolvedResourceBundle(p.resources, new StringVar(Contract.getAsStringVarResolver())));

        boolean valid = true;
        boolean bSuccess = imagingOptionsProperties.fromHttp(p.req, UserElement.getUserLocale(p.req), p.getTimeZone());

        if (!bSuccess) // Properties did not validate - send it back
        {
            opImagingOptions(p, imagingOptionsProperties);
            return;
        }

        imagingOptionsProperties.saveChanges(null);

        // Check to see that the profile names are unique and valid
        if (imagingOptions.getScanningProfiles().length > 1)
        {
            for (int i = 0; i < imagingOptions.getScanningProfiles().length; i++)
            {
                ScanningProfile scanningProfile = imagingOptions.getScanningProfiles()[i];
                // Verify characters in name
                valid &= scanningProfile.getProfileName().matches(PROFILENAME_REGEX);

                // Verify uniqueness
                valid &= unique(imagingOptions.getScanningProfiles(), scanningProfile.getProfileName(), i);
            }
        }

        FileStoreElement newFs = new FileStoreElement(fs);

        if (p.getParameter(p.req, "addProfile") != null)
            opPostAddProfile(p, imagingOptions);

        newFs.setImagingOptions(imagingOptions.toXml());

        if (valid)
            p.storeSession.setElement(fs, newFs, true);

        p.fs = newFs;

        opImagingOptions(p, null);
    }

    // Appends a new profile line onto the end
    private void opPostAddProfile(OpContext p, ImagingOptions imagingOptions) throws Exception
    {
        // Get the current set
        ScanningProfile[] scanningProfiles = imagingOptions.getScanningProfiles();

        // Count them
        int profiles = scanningProfiles == null ? 0 : scanningProfiles.length;

        // Copy them to a new, bigger, array
        ScanningProfile[] newScanningProfiles = new ScanningProfile[profiles + 1];

        System.arraycopy(scanningProfiles, 0, newScanningProfiles, 0, profiles);

        // Find a unique name
        String newProfileName = p.resources.getString("SCANNING_OPTIONS_NEWPROFILENAME_DEFAULT");
        while (!unique(scanningProfiles, newProfileName, -1))
        {
            newProfileName = "_" + newProfileName;
        }

        // Fill in the new element at the end of the bigger array
        ScanningProfile newScanningProfile = new ScanningProfile(p.resources);
        newScanningProfile.setProfileName(newProfileName);
        newScanningProfiles[profiles] = newScanningProfile;

        // Stick the new longer array back into the imaging options
        imagingOptions.setScanningProfiles(newScanningProfiles);
    }

    // Checks to see that the proposed name is unique among existing
    // ScanningProfile's
    private boolean unique(ScanningProfile[] scanningProfiles, String newProfileName, int me)
    {
        if (scanningProfiles == null)
            return true;

        for (int i = 0; i < scanningProfiles.length; i++)
            if (i != me && scanningProfiles[i].getProfileName().equalsIgnoreCase(newProfileName))
                return false;

        return true;
    }

    private void opDeleteScanningProfile(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        FileStoreElement fs = p.getFileStoreElement();

        ImagingOptions imagingOptions = ImagingOptions.fromXml(fs.getImagingOptions());

        ImagingOptionsProperties imagingOptionsProperties = new ImagingOptionsProperties(p.iDocumentId, imagingOptions, new ResolvedResourceBundle(p.resources, new StringVar(Contract.getAsStringVarResolver())));

        boolean bSuccess = imagingOptionsProperties.fromHttp(p.req, UserElement.getUserLocale(p.req), p.getTimeZone());

        if (!bSuccess) // Property validation failed - send it back
        {
            opImagingOptions(p, imagingOptionsProperties);
            return;
        }

        imagingOptionsProperties.saveChanges(null);

        int profileOffset = UtilRequest.getIntegerParameter(p.req, "profileOffset");
        String profileName = new String(Base64UrlSafe.decode(p.getParameter(p.req, "profileName")), "UTF8");

        // Get the current set
        ScanningProfile[] scanningProfiles = imagingOptions.getScanningProfiles();

        // Count them
        int profiles = scanningProfiles == null ? 0 : scanningProfiles.length;

        // Copy them (minus the one we are deleting, of course) to a new,
        // smaller , array
        ScanningProfile[] newScanningProfiles = new ScanningProfile[profiles - 1];
        for (int i = 0, j = 0; i < profiles; i++)
        {
            try
            {
                if (i != profileOffset || !scanningProfiles[i].getProfileName().equals(profileName))
                    newScanningProfiles[j++] = scanningProfiles[i];
            }
            catch (Exception e)
            {
                // If anything goes wrong, just display what we have and bail.
                opImagingOptions(p, null);
                return;
            }
        }

        // Stick the new shorter array back into the imaging options
        imagingOptions.setScanningProfiles(newScanningProfiles);

        // Get an "original" filestore element
        FileStoreElement newFs = new FileStoreElement(fs);

        // Stuff our new and improved imaging options into the filestore element
        newFs.setImagingOptions(imagingOptions.toXml());

        // Write out our updated filestore element
        p.storeSession.setElement(fs, newFs, true);

        // Slip the new one back into the OpContext object so we will pick it up
        // right away
        p.fs = newFs;

        // Show off the new and improved imaging options.
        opImagingOptions(p, null);
    }

    // This displays the Imaging Options page, allowing users to manage the
    // template's imaging options
    private void opImagingOptions(OpContext p, ImagingOptionsProperties imagingOptionsProperties) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        FileStoreElement fs = p.getFileStoreElement();

        ImagingOptions imagingOptions = null;

        imagingOptions = ImagingOptions.fromXml(fs.getImagingOptions());

        if (imagingOptions == null)
            imagingOptions = new ImagingOptions();

        // Create the StringVar objects
        StringVarResolverHashMap hashTable = new StringVarResolverHashMap();

        if (null == imagingOptionsProperties) // create a new one, otherwise use
                                              // the one passed in
            imagingOptionsProperties = new ImagingOptionsProperties(fs.DocumentID, imagingOptions, new ResolvedResourceBundle(p.resources, new StringVar(Contract.getAsStringVarResolver())));

        StringVarResolver imagingOptionsResolver = new PropertyAsStringResolver(imagingOptionsProperties);
        imagingOptionsResolver.chainResolvers(Contract.getAsStringVarResolver());
        hashTable.chainResolvers(imagingOptionsResolver);
        StringVar stringVar = new StringVar(hashTable);
        stringVar.setEnableRecursion(true);

        StringVar svDel = new StringVar(hashTable);
        svDel.setEnableRecursion(true);

        // Only users with manage template permissions can modify the extension
        // map.
        boolean readOnly = !SecurityCache.hasPermission(p.user, SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, fs.ACL, fs, false);

        // Make sure the templateId (the documentId) is in the resolver
        // variables
        hashTable.setVariable("templateId", p.sDocumentId);

        // Set up the resolver variables needed to build the page that contains
        // this extension list

        StringBuffer sb = new StringBuffer();
        stringVar.setPattern(p.resources.getString("SCANNING_PROFILE_ROW"));

        StringBuffer sbScanningOptions = new StringBuffer();

        if (imagingOptions.getScanningProfiles() != null)
        {

            for (int i = 0; i < imagingOptions.getScanningProfiles().length; i++)
            {
                // Verify characters in name
                ScanningProfile scanningProfile = imagingOptions.getScanningProfiles()[i];
                String propName = ImagingOptionsProperties.SCANNINGPROFILES + "." + i + "." + ScanningProfileProperties.PROFILENAME;
                Property prop = ((Property) imagingOptionsProperties.get(propName));
                if (!scanningProfile.getProfileName().matches(PROFILENAME_REGEX))
                {
                    prop.setInvalid(p.resources.getString("SCANNING_PROFILE_INVALID_NAME"));
                    prop.setLastUserEntry(scanningProfile.getProfileName());
                }
                else if (!unique(imagingOptions.getScanningProfiles(), scanningProfile.getProfileName(), i))
                {
                    prop.setInvalid(p.resources.getString("SCANNING_PROFILE_DUPLICATE_NAME"));
                    prop.setLastUserEntry(scanningProfile.getProfileName());
                }

                hashTable.setVariable("profileOffset", new Integer(i));

                sbScanningOptions.append(" enableScanningOptions('").append(i).append("');");

                hashTable.setVariable("deleteButton", svDel.resolveNames(p.resources.getString(imagingOptions.getScanningProfiles().length > 1 ? "SCANNING_PROFILE_ROW_DELETE_BUTTON" : "SCANNING_PROFILE_ROW_DELETE_BUTTON_GRAY")));

                stringVar.resolveNames(sb, UserElement.getUserLocale(p.req), p.getTimeZone());
            }
        }
        hashTable.setVariable("enableScanningOptions", sbScanningOptions);

        hashTable.setVariable("ScanningProfiles", sb.toString());

        // Fill in the HTML template and write it to the browser.
        p.out.println(stringVar.resolveNames(p.resources.getString(readOnly ? "IMAGING_TEMPLATE_DISPLAY_READ_ONLY" : "IMAGING_TEMPLATE_DISPLAY")));
    }

    // This handles the posting of a new exclusion list
    private void opPostCreateExclusionList(OpContext p) throws Exception
    {
        // Make sure the user has manage template permission to this template
        FileStoreElement fs = p.getFileStoreElement();
        boolean readOnly = !SecurityCache.hasPermission(p.user, SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, fs.ACL, fs, false);

        // Get the new exclusion list and save it
        if (!readOnly)
        {
            String excludeList = p.getParameter(p.req, "excludeList");
            FileExcludeListCache.putExcludeList(p.iDocumentId, excludeList);
        }

        // Display the new extension map
        opExtensionMaps(p);
    }

    // Show form for an extension map.
    private void opEditExtensionMap(OpContext p, boolean bNew) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        // If this is an existing extension, get current values
        String extension = null;
        String transformClass = null;
        String viewerURL = null;
        String transformClassOptions = null;
        String ix = null;
        String isNew;
        if (bNew)
            isNew = "true";
        else
        {
            isNew = p.getParameter("isNew");
        }

        if (!bNew)
        {
            // There are two ways we could reach here ...
            // 1) from the Edit button, from a particular existing extension,
            // from the list of all extensions.
            // 2) from a currently displayed Edit form for a particular (or new)
            // extension, when the user enters or changes the Transform
            // Class and presses "Update Options" button, when the recgonition
            // Pool(only in case of Global360 FileTransformOCR)
            // is changed.
            // In the second case, the entered values from the current form
            // are used to regenerate the new form, with new Transform Class
            // options filled in for the new Transform class
            if (p.req.getParameter("GetOptions") == null && p.req.getParameter("RefreshPoolOptions") == null)
            {
                // We got here from the "Edit" button of a particular existing
                // extension
                extension = p.getParameter("extension");
                if (!StringUtil.isBlank(extension))
                    extension = extension.trim();
                FileExtensionMap fr = FileExtensionMapCache.getExtensionMap(p.iDocumentId, extension);

                transformClass = fr.get("transformClass");
                viewerURL = fr.get("viewerURL");
                ix = fr.get("indexRendition");

                if (!StringUtil.isBlank(transformClass))
                {
                    try
                    {
                        FileTransform ft = FileExtensionMapCache.getTransformClass(p.iDocumentId, extension);
                        transformClassOptions = ft.getOptionsAsHtmlEditField(UserElement.getUserLocale(p.req), p.getTimeZone());
                    }
                    catch (Exception e)
                    {
                        m_Log.error("opEditExtensionMap", "E_EXCEPTION", e);
                        transformClassOptions = XmlUtil.encodeString(e.getLocalizedMessage(), true);
                    }
                }
            }
            else
            {
                // Get the parameters from the form
                extension = p.getParameter(p.req, "extension");
                transformClass = p.getParameter(p.req, "transformClass");
                viewerURL = p.getParameter(p.req, "viewerURL");
                String indexRendition = p.getParameter(p.req, "indexRendition");

                ix = (indexRendition == null ? "0" : "1");
                transformClassOptions = null;

                if (!StringUtil.isBlank(transformClass))
                {
                    transformClass = transformClass.trim();
                    FileTransform ft = isClassValid(transformClass);

                    if (ft == null)
                    {
                        transformClassOptions = "<br/><table border=\"0\"><colgroup><col width=\"5%\"><col width=\"*\"></colgroup>" + "<tr><td/><td>" + p.resources.getString("EXTENSION_MAP_INVALID_CLASS") + "</td></tr></table><br/>";
                    }
                    else
                    {
                        // The transform class is legal
                        try
                        {
                            if (p.req.getParameter("RefreshPoolOptions") != null)
                            {
                                ft.setOptionsFromHttp(p.req, UserElement.getUserLocale(p.req), p.getTimeZone());
                            }
                            transformClassOptions = ft.getOptionsAsHtmlEditField(UserElement.getUserLocale(p.req), p.getTimeZone());
                        }
                        catch (Exception e)
                        {
                            m_Log.error("opEditExtensionMap", "E_EXCEPTION", e);
                            transformClassOptions = XmlUtil.encodeString(e.getLocalizedMessage(), true);
                        }
                    }
                }
            }
        }

        StringVarResolverHashMap hashTable = new StringVarResolverHashMap();
        StringVar stringVar = new StringVar(hashTable);

        hashTable.setVariable("index", "0");
        hashTable.setVariable("isNew", isNew == null ? "" : isNew);
        hashTable.setVariable("templateId", p.sDocumentId);
        hashTable.setVariable("extension", extension == null ? "" : extension.trim());
        hashTable.setVariable("transformClass", transformClass == null ? "" : transformClass.trim());
        hashTable.setVariable("viewerURL", viewerURL == null ? "" : viewerURL.trim());
        hashTable.setVariable("checked", (ix != null && !ix.equals("0")) ? "checked" : "");
        hashTable.setVariable("transformClassOptions", transformClassOptions == null ? "&nbsp;" : transformClassOptions);

        hashTable.setVariable("OptionsEmptyDisplayStyle", StringUtil.isBlank(transformClass) ? "" : "style='display:none;'");
        hashTable.setVariable("OptionsFormDisplayStyle", StringUtil.isBlank(transformClass) ? "style='display:none;'" : "");

        p.out.println(stringVar.resolveNames(p.resources.getString("EDIT_EXTENSION_MAP")));
    }

    // Delete the specified extension from the extension map
    private void opDeleteExtensionMap(OpContext p) throws Exception
    {
        // Check if the user has manage template permission
        FileStoreElement fs = p.getFileStoreElement();

        boolean isReadOnly = !SecurityCache.hasPermission(p.user, SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, fs.ACL, fs, false);

        // If they do, then delete the specified extension
        if (!isReadOnly)
        {
            String extension = p.getParameter(p.req, "extension");
            FileExtensionMapCache.deleteExtensionMap(p.iDocumentId, extension);
        }

        // Redisplay the extension map list
        opExtensionMaps(p);
    }

    // This checks to see if the given class name can be instantiated on the
    // server.
    private FileTransform isClassValid(String className)
    {
        try
        {
            if (StringUtil.isBlank(className))
                return null;

            Class c = Class.forName(className);
            if (Class.forName("com.eistream.sonora.filestore.FileTransform").isAssignableFrom(c))
            {
                return (FileTransform) c.newInstance();
            }
            return null;
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }
        catch (NoClassDefFoundError e)
        {
            return null;
        }
        catch (IllegalAccessException e)
        {
            return null;
        }
        catch (InstantiationException e)
        {
            return null;
        }
    }

    private void opPostUpdateExtensionMap(OpContext p) throws Exception
    {
        // Check if the OK button was pressed
        if (p.req.getParameter("OK") != null)
        {
            // Check if the user has manage template permissions
            FileStoreElement fs = p.getFileStoreElement();
            boolean readOnly = !SecurityCache.hasPermission(p.user, SecurityCache.SECURITY_PERMISSIONMANAGETEMPLATES_S, fs.ACL, fs, false);

            if (!readOnly)
            {
                // Get the parameters from the form
                String extension = p.getParameter(p.req, "extension");
                String extensionOrig = p.getParameter(p.req, "extensionOriginal");
                String transformClass = p.getParameter(p.req, "transformClass");
                String transformClassOrig = p.getParameter(p.req, "transformClassOriginal");
                String viewerURL = p.getParameter(p.req, "viewerURL");
                String indexRendition = p.getParameter(p.req, "indexRendition");
                String isNew = p.getParameter("isNew");
                String transformClassOptions = null;

                if (!StringUtil.isBlank(extension))
                {
                    extension = extension.trim();
                }

                if (!StringUtil.isBlank(extensionOrig))
                {
                    if (StringUtil.isBlank(isNew))
                        extensionOrig = extensionOrig.trim();
                    else
                        extensionOrig = StringUtil.EMPTY;
                }

                // If a transform class was specified, make sure it is valid
                if (!StringUtil.isBlank(transformClass))
                {
                    transformClass = transformClass.trim();
                    FileTransform ft = isClassValid(transformClass);
                    if (ft == null)
                        // TODO: we should return a proper error page instead of
                        // an exception for this
                        throw new InvalidFileTransformClassException(transformClass);

                    if (StringUtil.isBlank(transformClassOrig))
                        transformClassOrig = transformClassOrig.trim();

                    // The transform class is legal, get the transform class
                    // options
                    // (but only if the form was showing the options for this
                    // transform class)
                    if (transformClass.equals(transformClassOrig) || Boolean.valueOf(isNew).booleanValue())
                    {
                        ft.setOptionsFromHttp(p.req, UserElement.getUserLocale(p.req), p.getTimeZone());
                        transformClassOptions = ft.getOptions();

                        // The options string, within the FileExtensionMap, is a
                        // UTF-8 string
                        // encoded as base 64. So we encode it.
                        if (transformClassOptions != null)
                            transformClassOptions = Base64UrlSafe.encodeBytes(transformClassOptions.getBytes("UTF-8"));
                    }
                }

                if (!StringUtil.isBlank(transformClass) || !StringUtil.isBlank(viewerURL) || !StringUtil.isBlank(indexRendition))
                {
                    if (!StringUtil.isBlank(extensionOrig))
                    {
                        extensionOrig = extensionOrig.trim();
                    }

                    boolean isNewFileMapExtension = true;
                    // If the extension changed, delete the old one
                    if (!extension.equalsIgnoreCase(extensionOrig) && extensionOrig.length() > 0)
                    {
                        FileExtensionMap extensionMap = FileExtensionMapCache.getExtensionMap(p.iDocumentId, extension);
                        if (extensionMap != null)
                            throw new FileExtensionMapAlreadyExistsException(extension);
                        FileExtensionMapCache.deleteExtensionMap(p.iDocumentId, extensionOrig);
                    }

                    if (extension.equalsIgnoreCase(extensionOrig))
                    {
                        isNewFileMapExtension = false;
                    }

                    // build the data
                    String data = "transformClass=" + (transformClass == null ? "" : transformClass) + ",viewerURL=" + (viewerURL == null ? "" : viewerURL.trim()) + ",indexRendition=" + (indexRendition == null ? "0" : "1");
                    if (transformClassOptions != null && transformClassOptions.length() > 0)
                        data += ",transformOptions=" + transformClassOptions;

                    // Construct the new extension map
                    FileExtensionMap fr = new FileExtensionMap(p.iDocumentId, extension, data);
                    if (isNewFileMapExtension)
                    {
                        FileExtensionMapCache.addExtensionMap(fr);
                    }
                    else
                    {
                        FileExtensionMapCache.updateExtensionMap(fr);
                    }
                }
            }

            // Now re-display the extension map
            opExtensionMaps(p);
        }

        // Check if the "Get Options" button was pressed
        else if (p.req.getParameter("GetOptions") != null || p.req.getParameter("RefreshPoolOptions") != null)
        {
            // Redisplay the edit extension page
            opEditExtensionMap(p, false);
        }

        // Otherwise the "Cancel" button was pressed
        else
            // Re-display the extension map
            opExtensionMaps(p);
    }

    private void opViewListForm(OpContext p) throws Exception
    {
        LoginManager loginManager = null;
        ArrayList fpvl = null;

        try
        {
            // This operation always returns a page
            p.sentPage = true;

            // Get the file plan views.
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
            FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
            fpvl = fpn.getFilePlanViewList();
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }

        StringVarResolverHashMap hashMap = new StringVarResolverHashMap();
        StringVar varResolver = new StringVar(hashMap);

        String header = p.resources.getString("RM_VIEW_LIST_HEADER");

        String id = p.getParameter(p.req, "id");
        String szComponentDefId = p.getParameter(p.req, "componentDefId");
        hashMap.setVariable("componentDefId", szComponentDefId);
        hashMap.setVariable("documentId", id);
        hashMap.setVariable("id", id);
        hashMap.setVariable("version", p.getParameter(p.req, "version"));
        hashMap.setVariable("operation", p.getParameter(p.req, "operation"));
        hashMap.setVariable("navType", p.getParameter(p.req, "navType"));
        hashMap.setVariable("dispAddressType", p.getParameter(p.req, "dispAddressType"));

        p.out.println(varResolver.resolveNames(header, UserElement.getUserLocale(p.req), p.getTimeZone()));
        hashMap.clearVariables();

        String listEntry = p.resources.getString("RM_VIEW_LIST_ENTRY");
        int count = fpvl.size();
        for (int ii = 0; ii < count; ii++)
        {
            FilePlanView fpv = (FilePlanView) fpvl.get(ii);
            hashMap.setVariable("viewId", "" + fpv.getViewId());
            hashMap.setVariable("viewName", fpv.getViewName());
            hashMap.setVariable("viewType", fpv.getViewTypeAsString(p.resources));
            String shading = "";
            if (ii % 2 != 0)
                shading = "class=\"shaded\"";
            hashMap.setVariable("shading", shading);

            p.out.println(varResolver.resolveNames(listEntry, UserElement.getUserLocale(p.req), p.getTimeZone()));
            hashMap.clearVariables();
        }

        String trailer = p.resources.getString("RM_VIEW_LIST_TRAILER");
        p.out.println(trailer);
    }

    private void opPostViewListForm(OpContext p) throws Exception
    {
        String id = p.getParameter(p.req, "id");
        String version = p.getParameter(p.req, "version");
        String viewId = p.getParameter(p.req, "viewId");
        String operation = p.getParameter(p.req, "operation");
        String navType = p.getParameter(p.req, "navType");
        String dispAddressType = p.getParameter(p.req, "dispAddressType");
        String szComponentDefId = p.getParameter(p.req, "componentDefId");

        p.resp.sendRedirect("FileStore?op=navigate&id=" + id + "&version=" + version + "&viewId=" + viewId + "&parentId=0&how=navigateDown" + "&operation=" + operation + "&navType=" + navType + "&dispAddressType=" + dispAddressType + "&componentDefId=" + szComponentDefId);
    }

    private void opNavigateFilePlanForm(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            // This operation always returns a page
            p.sentPage = true;

            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));

            String sViewId = p.getParameter(p.req, "viewId");
            long viewId = new Long(sViewId).longValue();

            String sParentId = p.getParameter(p.req, "parentId");
            long parentId = new Long(sParentId).longValue();

            String navType = p.getParameter(p.req, "navType");
            boolean containersOnly = navType.equals("1") ? true : false;

            String dispAddressType = p.getParameter(p.req, "dispAddressType");

            // Get the file plan components at this level.
            FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
            FilePlanComponents fpComponents = fpn.getFilePlanComponents(viewId, parentId, containersOnly);

            // Get the object information.
            String version = p.getParameter(p.req, "version");

            String id = p.getParameter(p.req, "id");

            String szComponentDefId = p.getParameter(p.req, "componentDefId");
            long componentDefId = Long.parseLong(szComponentDefId);

            StringVarResolverHashMap hashMap = new StringVarResolverHashMap();
            StringVar varResolver = new StringVar(hashMap);

            String path;
            int dualNamingOption = Integer.parseInt(dispAddressType);
            if (parentId == 0)
                path = "//" + fpn.getViewName(viewId);
            else
                path = RecordsUtil.getPathById(loginManager, parentId, viewId, dualNamingOption);
            String componentPattern = p.resources.getString("RM_PATH_LOCATOR_FOR_DECLARE");
            String locator = fpn.getPathLocator(path, viewId, dualNamingOption, componentPattern, UserElement.getUserLocale(p.req), p.getTimeZone());

            hashMap.setVariable("documentId", id);
            hashMap.setVariable("id", id);
            hashMap.setVariable("version", version);
            hashMap.setVariable("parentId", sParentId);
            hashMap.setVariable("viewId", sViewId);
            hashMap.setVariable("navType", navType);
            hashMap.setVariable("dispAddressType", dispAddressType);
            hashMap.setVariable("componentDefId", szComponentDefId);
            hashMap.setVariable("operation", p.getParameter(p.req, "operation"));
            hashMap.setVariable("locator", locator);

            String header = p.resources.getString("RM_FILE_PLAN_HEADER");
            p.out.println(varResolver.resolveNames(header, UserElement.getUserLocale(p.req), p.getTimeZone()));
            hashMap.clearVariables();

            ArrayList fpca = fpComponents.getComponentList();
            int count = fpca.size();
            for (int ii = 0; ii < count; ii++)
            {
                FilePlanComponent fpc = (FilePlanComponent) fpca.get(ii);

                hashMap.setVariable("viewId", "" + fpc.getViewId());
                hashMap.setVariable("componentName", fpc.getComponentName());
                hashMap.setVariable("componentType", fpc.getComponentDefinitionName());
                hashMap.setVariable("componentId", "" + fpc.getComponentId());
                hashMap.setVariable("componentDefId", "" + fpc.getComponentDefinitionId());
                hashMap.setVariable("code", fpc.getCode());
                hashMap.setVariable("componentTitle", fpc.getComponentTitle());
                hashMap.setVariable("viewName", fpc.getViewName());
                hashMap.setVariable("fullPath", fpc.getFullPathByName());
                hashMap.setVariable("pathByName", fpc.getFullPathByName());
                hashMap.setVariable("pathByCode", fpc.getFullPathByCode());
                hashMap.setVariable("parentId", sParentId);
                hashMap.setVariable("idx", "" + (ii + 1));
                hashMap.setVariable("shading", ii % 2 == 0 ? "" : "class=\"shaded\"");

                // Add the navigation buttons if the node has children
                hashMap.setVariable("navigationButton", varResolver.resolveNames(p.resources.getString(fpc.getHasTargets() != 0 ? "RM_NAVIGATION_BUTTON" : "RM_NO_NAVIGATION_BUTTON")));

                // Put selection button but only if entry qualifies.
                String listEntry = p.resources.getString((fpc.isContainer() && (!fpc.canContain(loginManager, componentDefId)) || (fpc.isContainer() ^ containersOnly)) ? "RM_FILE_PLAN_ENTRY_NOT_SELECTABLE" : "RM_FILE_PLAN_ENTRY");
                p.out.println(varResolver.resolveNames(listEntry, UserElement.getUserLocale(p.req), p.getTimeZone()));
                hashMap.clearVariables();
            }

            // Set the trailer.
            hashMap.setVariable("parentId", sParentId);
            hashMap.setVariable("viewId", sViewId);

            String trailer = p.resources.getString("RM_FILE_PLAN_TRAILER");
            p.out.println(varResolver.resolveNames(trailer, UserElement.getUserLocale(p.req), p.getTimeZone()));
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opPostNavigateFilePlanForm(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        String id = p.getParameter(p.req, "id");
        String version = p.getParameter(p.req, "version");
        String sViewId = p.getParameter(p.req, "viewId");
        String sComponentId = p.getParameter(p.req, "componentId");
        String sComponentDefId = p.getParameter(p.req, "componentDefId");
        String operation = p.getParameter(p.req, "operation");
        String how = p.getParameter(p.req, "how");
        String navType = p.getParameter(p.req, "navType");
        String dispAddressType = p.getParameter(p.req, "dispAddressType");

        long viewId = Long.parseLong(sViewId);
        long componentId = Long.parseLong(sComponentId);

        // If we are done navigating the file plan, we can invoke the next
        // operation.
        if (how.equalsIgnoreCase("done"))
            p.resp.sendRedirect("FileStore?op=" + operation + "&id=" + id + "&version=" + version + "&viewId=" + sViewId + "&componentId=" + sComponentId + "&componentDefId=" + sComponentDefId);
        else if (how.equalsIgnoreCase("navigateUp"))
        {
            // Back to the top.
            if (componentId == 0)
            {
                // Back to the view list.
                p.resp.sendRedirect("FileStore?op=showViewList&id=" + id + "&version=" + version + "&viewId=" + viewId + "&parentId=" + componentId + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType + "&componentDefId=" + sComponentDefId);
            }
            else
            {
                LoginManager loginManager = null;
                try
                {
                    // We have to get the parent component and then drill down.
                    // Get the grandparent so we can get its children.
                    loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
                    FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
                    FilePlanComponent fpc = fpn.getParentComponent(viewId, componentId);

                    p.resp.sendRedirect("FileStore?op=navigate&id=" + id + "&version=" + version + "&viewId=" + viewId + "&parentId=" + fpc.getComponentId() + "&componentDefId=" + sComponentDefId + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
                }
                finally
                {
                    if (loginManager != null)
                        loginManager.logout();
                }
            }
        }
        else if (how.equalsIgnoreCase("navigateTo"))
        {
            if (viewId == 0)
            {
                // Back to the view list.
                p.resp.sendRedirect("FileStore?op=showViewList&id=" + id + "&version=" + version + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType + "&componentDefId=" + sComponentDefId);
            }
            else
            {
                p.resp.sendRedirect("FileStore?op=navigate&id=" + id + "&version=" + version + "&viewId=" + viewId + "&parentId=" + sComponentId + "&componentDefId=" + sComponentDefId + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
            }
        }
        else
        {
            // Keep traversing the file plan.
            p.resp.sendRedirect("FileStore?op=navigate&id=" + id + "&version=" + version + "&viewId=" + viewId + "&parentId=" + componentId + "&componentDefId=" + sComponentDefId + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
        }
    }

    private void opFilePlan(OpContext p) throws Exception
    {
        LoginManager loginManager = null;
        ArrayList fpvl = null;

        try
        {
            // This operation always returns a page
            p.sentPage = true;

            // Get the file plan views.
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
            FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
            fpvl = fpn.getFilePlanViewList();
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }

        StringVarResolverHashMap hashMap = new StringVarResolverHashMap();
        StringVar varResolver = new StringVar(hashMap);

        String listEntry = p.resources.getString("RM_VIEW_LIST_ENTRY");
        StringBuilder selectionList = new StringBuilder();

        int count = fpvl.size();
        for (int ii = 0; ii < count; ii++)
        {
            FilePlanView fpv = (FilePlanView) fpvl.get(ii);
            hashMap.setVariable("viewId", "" + fpv.getViewId());
            hashMap.setVariable("viewName", fpv.getViewName());
            hashMap.setVariable("viewType", fpv.getViewTypeAsString(p.resources));
            String shading = "";
            if (ii % 2 != 0)
                shading = "class=\"shaded\"";
            hashMap.setVariable("shading", shading);
            selectionList.append(varResolver.resolveNames(listEntry, UserElement.getUserLocale(p.req), p.getTimeZone()));
            hashMap.clearVariables();
        }

        p.out.println(varResolver.resolveNames(p.resources.getString("RM_BROWSE_FILE_PLAN_HEADER"), UserElement.getUserLocale(p.req), p.getTimeZone()));
        p.out.println(selectionList.toString());
        p.out.println(p.resources.getString("RM_BROWSE_FILE_PLAN_TRAILER"));
    }

    private void opPostFilePlan(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            // This operation always returns a page
            p.sentPage = true;

            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));

            String sViewId = p.getParameter(p.req, "viewId");
            long viewId = new Long(sViewId).longValue();

            String sParentId = p.getParameter(p.req, "parentId");
            long parentId = new Long(sParentId).longValue();

            String navType = p.getParameter(p.req, "navType");
            boolean containersOnly = navType.equals("1") ? true : false;

            String dispAddressType = p.getParameter(p.req, "dispAddressType");

            // Get the file plan components at this level.
            FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
            FilePlanComponents fpComponents = fpn.getFilePlanComponents(viewId, parentId, containersOnly);

            StringVarResolverHashMap hashMap = new StringVarResolverHashMap();
            StringVar varResolver = new StringVar(hashMap);

            String path;
            int dualNamingOption = Integer.parseInt(dispAddressType);
            if (parentId == 0)
                path = "//" + fpn.getViewName(viewId);
            else
                path = RecordsUtil.getPathById(loginManager, parentId, viewId, dualNamingOption);
            String componentPattern = p.resources.getString("RM_PATH_LOCATOR_FOR_BROWSE");
            String locator = fpn.getPathLocator(path, viewId, dualNamingOption, componentPattern, UserElement.getUserLocale(p.req), p.getTimeZone());

            hashMap.setVariable("documentId", "0");
            hashMap.setVariable("id", "0");
            hashMap.setVariable("version", "1");
            hashMap.setVariable("locator", locator);
            hashMap.setVariable("parentId", sParentId);
            hashMap.setVariable("viewId", sViewId);
            hashMap.setVariable("navType", navType);
            hashMap.setVariable("dispAddressType", dispAddressType);
            hashMap.setVariable("operation", p.getParameter(p.req, "operation"));

            String header = p.resources.getString("RM_BROWSE_DOCUMENT_HEADER");
            p.out.println(varResolver.resolveNames(header, UserElement.getUserLocale(p.req), p.getTimeZone()));
            hashMap.clearVariables();

            ArrayList fpca = fpComponents.getComponentList();
            int count = fpca.size();
            for (int ii = 0; ii < count; ii++)
            {
                FilePlanComponent fpc = (FilePlanComponent) fpca.get(ii);

                hashMap.setVariable("viewId", "" + fpc.getViewId());
                hashMap.setVariable("componentName", fpc.getComponentName());
                hashMap.setVariable("componentType", fpc.getComponentDefinitionName());
                hashMap.setVariable("componentId", "" + fpc.getComponentId());
                hashMap.setVariable("componentDefId", "" + fpc.getComponentDefinitionId());
                hashMap.setVariable("code", fpc.getCode());
                hashMap.setVariable("componentTitle", fpc.getComponentTitle());
                hashMap.setVariable("viewName", fpc.getViewName());
                hashMap.setVariable("fullPath", fpc.getFullPathByName());
                hashMap.setVariable("pathByName", fpc.getFullPathByName());
                hashMap.setVariable("pathByCode", fpc.getFullPathByCode());
                hashMap.setVariable("parentId", sParentId);
                hashMap.setVariable("idx", "" + (ii + 1));
                hashMap.setVariable("shading", ii % 2 == 0 ? "" : "class=\"shaded\"");

                // Add the navigation buttons if the node has children
                hashMap.setVariable("navigationButton", varResolver.resolveNames(p.resources.getString(fpc.getHasTargets() != 0 ? "RM_DOCUMENT_BROWSE_NAVIGATION_BUTTON" : "RM_DOCUMENT_BROWSE_NO_NAVIGATION_BUTTON")));

                // Use the property template for the list entry - put selection
                // button but only if entry qualifies.
                String listEntry = p.resources.getString(fpc.isContainer() ^ containersOnly ? "RM_FILE_PLAN_ENTRY_NOT_SELECTABLE" : "RM_DOCUMENT_BROWSE_ENTRY");
                p.out.println(varResolver.resolveNames(listEntry, UserElement.getUserLocale(p.req), p.getTimeZone()));
                hashMap.clearVariables();
            }

            hashMap.setVariable("parentId", sParentId);
            hashMap.setVariable("viewId", sViewId);

            String trailer = p.resources.getString("RM_BROWSE_DOCUMENT_TRAILER");
            p.out.println(varResolver.resolveNames(trailer, UserElement.getUserLocale(p.req), p.getTimeZone()));
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opPostBrowseDocument(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        String sViewId = p.getParameter(p.req, "viewId");
        String sComponentId = p.getParameter(p.req, "componentId");
        String operation = p.getParameter(p.req, "operation");
        String how = p.getParameter(p.req, "how");
        String navType = p.getParameter(p.req, "navType");
        String dispAddressType = p.getParameter(p.req, "dispAddressType");

        long viewId = Long.parseLong(sViewId);
        long componentId = Long.parseLong(sComponentId);

        // If we are done navigating the file plan, we can invoke the next
        // operation.
        if (how.equalsIgnoreCase("done"))
        {
            LoginManager loginManager = null;

            try
            {
                // Get document information.
                loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));

                HostRecordId hId = RecordsUtil.getHostRecordId(loginManager, componentId);

                if (hId == null)
                    throw new NotFoundException(NotFoundException.MSG_ID_NO_RECORD_IN_CM, String.valueOf(componentId));

                BigDecimal id = new BigDecimal(hId.getId());
                int versionNumber = hId.getVersionNumber();

                if (!RecordsUtil.isAccessible(loginManager, id, versionNumber))
                    throw new NoAccessException(id.toString() + " " + Integer.toString(versionNumber));

                if (versionNumber == 0)
                {
                    // We have a case folder.
                    p.resp.sendRedirect("Case?op=f&id=" + id);
                }
                else
                {
                    p.resp.sendRedirect("FileStore?op=" + operation + "&id=" + id + "&version=" + versionNumber);
                }
            }
            finally
            {
                if (loginManager != null)
                    loginManager.logout();
            }

        }
        else if (how.equalsIgnoreCase("navigateUp"))
        {
            // Back to the top.
            if (componentId == 0)
            {
                // Back to the view list.
                p.resp.sendRedirect("FileStore?op=filePlan&id=0&version=1" + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
            }
            else
            {
                LoginManager loginManager = null;
                try
                {
                    // We have to get the parent component and then drill down.
                    // Get the grandparent so we can get its children.
                    loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
                    FilePlanNavigator fpn = new FilePlanNavigator(loginManager);
                    FilePlanComponent fpc = fpn.getParentComponent(viewId, componentId);

                    p.resp.sendRedirect("FileStore?op=browseDocument&id=" + p.sDocumentId + "&version=" + p.getVersionString() + "&viewId=" + viewId + "&parentId=" + fpc.getComponentId() + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
                }
                finally
                {
                    if (loginManager != null)
                        loginManager.logout();
                }
            }
        }
        else if (how.equalsIgnoreCase("navigateTo"))
        {
            if (viewId == 0)
            {
                // Back to the view list.
                p.resp.sendRedirect("FileStore?op=filePlan&id=0&version=1" + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
            }
            else
            {
                p.resp.sendRedirect("FileStore?op=browseDocument&id=" + p.sDocumentId + "&version=" + p.getVersionString() + "&viewId=" + viewId + "&parentId=" + sComponentId + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
            }
        }
        else
        {
            // Keep traversing the file plan.
            p.resp.sendRedirect("FileStore?op=browseDocument&id=" + p.sDocumentId + "&version=" + p.getVersionString() + "&viewId=" + viewId + "&parentId=" + componentId + "&navType=" + navType + "&operation=" + operation + "&dispAddressType=" + dispAddressType);
        }
    }

    private void opSupersede(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            // This operation always returns a page
            p.sentPage = true;

            Locale locale = UserElement.getUserLocale(p.req);
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), locale);
            String sParentId = p.getParameter(p.req, "parentId");
            FileStoreElement fs = p.getFileStoreElement();

            RecordOperations ro = new RecordOperations(loginManager);
            String form = ro.getSupersedeForm(fs, p.getVersion(), sParentId, p.resources, locale, p.getTimeZone());

            p.out.println(form);
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opPostSupersede(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
            RecordOperations ro = new RecordOperations(loginManager);
            ro.processSupersedeForm(p.req, true);
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opAddSupporting(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        LoginManager loginManager = null;

        try
        {
            Locale locale = UserElement.getUserLocale(p.req);
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), locale);
            RecordOperations ro = new RecordOperations(loginManager);

            String sParentId = p.getParameter(p.req, "parentId");

            FileStoreElement fs = p.getFileStoreElement();
            String form = ro.getAddSupportingForm(fs, p.getVersion(), sParentId, p.resources, locale, p.getTimeZone());

            p.out.println(form);
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opPostAddSupporting(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
            RecordOperations ro = new RecordOperations(loginManager);
            ro.processAddSupportingForm(p.req, true);
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opPostAutoClassify(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        LoginManager loginManager = null;

        try
        {
            Locale locale = UserElement.getUserLocale(p.req);
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), locale);
            DeclarationManager dm = new DeclarationManager(loginManager);
            FileStoreElement fs = p.getFileStoreElement();
            dm.autoClassify(fs, p.req, p.resp, p.resources, locale, p.getTimeZone());
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opDeclare(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        LoginManager loginManager = null;

        try
        {
            // Log in to the records manager.
            Locale locale = UserElement.getUserLocale(p.req);
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), locale);

            String sParentId = p.getParameter(p.req, "parentId");

            DeclarationManager dm = new DeclarationManager(loginManager);
            FileStoreElement fs = p.getFileStoreElement();
            String form = dm.getForm(fs, p.getVersion(), sParentId, p.resources, locale, p.getTimeZone());

            p.out.println(form);
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    private void opPostDeclarationForm(OpContext p) throws Exception
    {
        LoginManager loginManager = null;

        try
        {
            loginManager = RecordsUtil.getLoginManager(SecurityCache.parseDomain(p.req.getRemoteUser()), UserElement.getUserLocale(p.req));
            DeclarationManager dm = new DeclarationManager(loginManager);
            FileStoreElement fs = p.getFileStoreElement();
            dm.processForm(fs, p.req);
        }
        finally
        {
            if (loginManager != null)
                loginManager.logout();
        }
    }

    // Make an Archive Template for an Object Template
    private void doArchiveTemplate(OpContext p, boolean create) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        // Command line defaults
        if ((p.sKey == null) || (p.sKey.startsWith("{{")))
            p.sKey = p.sDocumentId;

        ArchiveInfo archiveInfo = RepositoryManager.getObjectAsXmlForArchive(null, p.sKey, null, null);

        // add all strings that are in the param list and the
        StringVarResolverHashMap eSupportResolver = new StringVarResolverHashMap(new StringVarResolverServletParms(p.req));
        StringVar sv = new StringVar(eSupportResolver);
        eSupportResolver.setVariable("archiveTemplateID", archiveInfo.archiveTemplateId);
        eSupportResolver.setVariable("displayName", archiveInfo.displayName);

        // Get file store session bean.
        FileStoreSessionEJB storeSession = p.getFileStoreSessionEJB();

        FileStoreElement fs = null;

        if (!create) // If we aren't creating one then one must already exist or
                     // we'll offer to create one
        {
            if (archiveInfo.archiveTemplateId == null)
            {
                p.out.println(sv.resolveNames(p.resources.getString("NO_ARCHIVE_TEMPLATE")));
                return;
            }

            try
            {
                fs = storeSession.getElement(archiveInfo.archiveTemplateId);
            }
            catch (FinderException fe)
            {
                p.out.println(sv.resolveNames(p.resources.getString("MISSING_ARCHIVE_TEMPLATE")));
                return;
            }
        }

        if (fs == null)
        {
            // Create template.
            archiveInfo.archiveTemplateId = storeSession.makeTemplate(new BigDecimal(0));
            eSupportResolver.setVariable("archiveTemplateID", archiveInfo.archiveTemplateId);

            // Set the repository key in the new Archive Template to that of
            // this Object
            fs = storeSession.getElement(archiveInfo.archiveTemplateId);
            FileStoreElement old_fs = fs.duplicate();
            fs.setRepositoryDocId(p.sKey);
            fs.setTemplateName(sv.resolveNames(p.resources.getString("ARCHIVE_TEMPLATE_DISPLAYNAME_PREFIX")));
            fs.setTitle(sv.resolveNames(p.resources.getString("ARCHIVE_TEMPLATE_DISPLAYNAME_PREFIX")));
            fs.setIconName("archive.gif");
            fs.setIsTemplate(FileStoreElement.TYPE_PUBLISHED_TEMPLATE);
            fs.setFieldVersioningLevel(true);
            fs.setHidden(true);
            storeSession.setElement(old_fs, fs, true);

            RepositoryManager.setArchiveTemplateForTemplate(p.sKey, archiveInfo.archiveTemplateId);

            // Flush the affected caches.
            CacheSend flushcache = new CacheSend();
            flushcache.sendCommand(CacheConstants.FIELDCACHE);
            flushcache.sendCommand(CacheConstants.TEMPLATESCACHE);
        }

        p.out.println(CSRFGuardTokenResolver.addCsrfTokenToResponse(sv.resolveNames(p.resources.getString("ARCHIVE_TEMPLATE_FRAMESET")), "iframe", "src", p.req));

    }

    private void opArchiveTemplateCreate(OpContext p) throws Exception
    {
        doArchiveTemplate(p, true);
    }

    private void opArchiveTemplateDisplay(OpContext p) throws Exception
    {
        doArchiveTemplate(p, false);
    }

    private void opArchive(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        // set the response content type TODO: why do we do this here??? this
        // should be generic
        p.resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
        p.resp.sendRedirect("FileStore?op=f&id=" + p.getFileStoreSessionEJB().archiveObject(p.sDocumentId, SecurityCache.parseDomain(p.req.getRemoteUser())));
    }

    private void opRestore(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        boolean repositorySpecified = p.sDocumentId.indexOf(RepositoryKey.ID_SEP) > -1;

        FileStoreElement fs = null;
        try
        {
            String id = p.sDocumentId;

            // If repositorySpecified then this isn't a Filestore ID, but rather
            // an Archive ID
            if (repositorySpecified)
            {
                id = RepositoryManager.normalizeObjectId(id);
                fs = p.getFileStoreSessionEJB().getElement(id, false);

                if (fs == null)
                {
                    throw new FinderException();
                }

                // Since we overloaded the ID format, tell OpContext what ID and
                // what Filestore object to use
                p.iDocumentId = fs.DocumentID;
                p.fs = fs;
            }
            else
            {
                fs = p.getFileStoreSessionEJB().getElement(new BigDecimal(id));
            }
        }
        catch (FinderException fe)
        {
            throw new RecordNotFoundException(fe, p.sDocumentId);
        }

        // See if the object to be restored already exists
        ArchiveInfo archiveInfo = RepositoryManager.getObjectAsXmlForArchive(null, fs.repositoryDocId, null, null);

        boolean overwrite = false;
        boolean rearchive = false;
        UserContext userContext = new UserContext(p.req);
        ImmutableProperty prop = UserCache.getUserElementInst(userContext.getUser());
        String persona = (String) prop.get("persona").getValue();
        boolean hasPersona = persona != null && persona.length() != 0;
        String includeContentProperty = SystemApplicationProperties.getValue("INCLUDE_CONTENT_FRAME");
        boolean includeContentFrame = "1".equals(includeContentProperty) || ("2".equals(includeContentProperty) && hasPersona);

        FileStoreElement fsTemplate = (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());

        PrintWriter out = null;
        if (p.out == null)
        {
            p.resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
            out = new PrintWriter(new OutputStreamWriter(p.resp.getOutputStream(), UtilRequest.getCharEncoding()), true);
            p.resp.setHeader("Pragma", "no-cache");
        }

        if (fsTemplate.isArchivePromptBeforeAutoRestoreEnabled() && p.getParameter(p.req, "RestoreOK") == null)
        {
            String restoreConfirm;

            if (includeContentFrame)
            {
                restoreConfirm = "ARCHIVE_OK_TO_RESTORE_CONTENT_FRAME";
            }
            else
            {
                restoreConfirm = "ARCHIVE_OK_TO_RESTORE";
            }

            if (p.out == null)
            {
                p.out = out;
            }
            returnPage(p, p.resources.getString(restoreConfirm));
            return;
        }

        if (archiveInfo != null)
        {
            if (fsTemplate.isArchivePromptBeforeRestoreOverwriteEnabled() && p.getParameter(p.req, "overwrite") == null)
            {
                String restoreConfirm;

                if (includeContentFrame)
                {
                    restoreConfirm = "ARCHIVE_OK_TO_OVERWRITE_CONTENT_FRAME";
                }
                else
                {
                    restoreConfirm = "ARCHIVE_OK_TO_OVERWRITE";
                }

                if (p.out == null)
                {
                    p.out = out;
                }
                returnPage(p, p.resources.getString(restoreConfirm));
                return;
            }

            overwrite = true;

            if (fsTemplate.isArchiveRearchiveModifiedEnabled() && RepositoryManager.modifiedSinceRestore(fs.repositoryDocId))
            {
                String rearchiveParm = p.getParameter(p.req, "rearchive");
                if (fsTemplate.isArchivePromptBeforeRearchiveEnabled() && (rearchiveParm == null || rearchiveParm.length() == 0))
                {
                    String restoreConfirm;

                    if (includeContentFrame)
                    {
                        restoreConfirm = "ARCHIVE_OK_TO_REARCHIVE_CONTENT_FRAME";
                    }
                    else
                    {
                        restoreConfirm = "ARCHIVE_OK_TO_REARCHIVE";
                    }

                    if (p.out == null)
                    {
                        p.out = out;
                    }
                    returnPage(p, p.resources.getString(restoreConfirm));
                    return;
                }

                rearchive = Boolean.valueOf(rearchiveParm).booleanValue();
            }
        }

        if (rearchive)
        {
            p.getFileStoreSessionEJB().archiveObject(fs.repositoryDocId, SecurityCache.parseDomain(p.req.getRemoteUser()));
        }
        else if (overwrite)
        {
            RepositoryManager.delete(fs.repositoryDocId);
        }

        p.getFileStoreSessionEJB().restoreArchivedObject(fs, p.getVersion(), SecurityCache.parseDomain(p.req.getRemoteUser()));

        // If the caller did not specify an rurl parameter, redirect to the
        // newly created object
        String rurl = p.getParameter("rurl");
        if (rurl == null || rurl.startsWith(StringVar.OPEN_DELIM))
        {
            RepositoryKey repKey = RepositoryKey.fromString(fs.repositoryDocId);
            String url = RepositoryManager.getRepositoryInterface(repKey.repositoryId.intValue()).getURL("open", repKey.objectKey);

            p.resp.sendRedirect(url);
            return;
        }

        p.resp.sendRedirect(UtilRequest.rUrlDecode(rurl));
    }

    private void opPostEVCheckoutForm(OpContext p) throws Exception
    {
        String action = p.getParameter(p.req, "action");

        // If they pressed the Cancel button
        if (p.getParameter(p.req, "submitCancel") != null)
        {
            // Don't forget to unlock the document!
            p.unlockDocument();
            return;
        }

        DocVersionProperties dvp = new DocVersionProperties(p.iDocumentId, p.getVersion());
        TimeZone tz = p.getTimeZone();

        boolean bSuccess = dvp.fromHttp(p.req, UserElement.getUserLocale(p.req), tz);
        if (!bSuccess) // Properties validation failed - send it back
        {
            doEVCheckoutForm(p, dvp, action);
            return;
        }

        // Update document version element and handle any conflicts that are
        // detected, if there is conflict
        // generate and return the conflict page (we'll come back here when the
        // user submits the conflict form)
        ArrayList conflicts = new ArrayList();
        if (!dvp.saveChanges(conflicts))
        {
            ConflictResolutionPage conflictsPage = new ConflictResolutionPage(conflicts);
            p.out.println(conflictsPage.getHtml("FileStore?op=checkout&id=" + p.sDocumentId + "&version=" + p.getVersionString(), UserElement.getUserLocale(p.req), tz));
        }

        // If the action is checkout, now is the time to actually download the
        // file, otherwise it was lock
        // and we're done (the dispatcher will redisplay the appropriate page
        // when we return).
        if (action.equalsIgnoreCase("checkout"))
        {
            // If we were invoked from the version list, we need to redisplay
            // the vlist before editing the file
            int mode = MODE_IMMEDIATE;
            if (p.redisplay == null || p.redisplay.compareToIgnoreCase("VList") == 0)
            {
                mode = MODE_DEFERRED;
                // If we are invoking an image document, the mode is new window
                if (p.useApplets() && p.isWIEFile(OpContext.VER_SPECIFIED))
                    mode = MODE_NEWWINDOW;
            }
            doDownload(p, ACTION_DOWNLOAD_EDIT, mode);
        }
    }

    // Return the extended versioning checkout form. This is used for both
    // checkout and lock, the action
    // parameter specifies the operation (what we do when this form is posted is
    // different).
    private void doEVCheckoutForm(OpContext p, DocVersionProperties dvp, String action) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        FileStoreElement fs = p.getFileStoreElement();

        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
        StringVar varResolver = new StringVar(stringVars);

        // Add the document version properties to the resolver chain
        if (null == dvp)
            dvp = new DocVersionProperties(fs.getDocumentID(), fs.getCurrentVersion());
        PropertyAsStringResolver propResolver = new PropertyAsStringResolver(dvp);
        stringVars.chainResolvers(propResolver);

        stringVars.setVariable("documentId", p.sDocumentId);
        stringVars.setVariable("version", "" + fs.getCurrentVersion());
        stringVars.setVariable("title", fs.getTitle() == null ? "" : HtmlUtil.toSafeHtml(fs.getTitle()));
        stringVars.setVariable("action", action);
        stringVars.setVariable("redisplay", p.redisplay == null ? "vlist" : p.redisplay);

        // Process and output HTML
        Locale locale = UserElement.getUserLocale(p.req);
        String output = varResolver.resolveNames(p.resources.getString("DOCVERSION_CHECKOUT_FORM"), locale, p.getTimeZone());
        if (p.out == null)
        {
            p.resp.setContentType("text/html; charset=" + UtilRequest.getCharSet());
            p.resp.setHeader("Pragma", "no-cache"); // HTTP 1.0
            p.resp.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
            p.out = new PrintWriter(new OutputStreamWriter(p.resp.getOutputStream(), UtilRequest.getCharEncoding()), true);
        }
        p.out.println(L10nUtils.localizeTokens(output, locale));
    }

    // Checkout starts by locking the document to the current user. Then we test
    // to see if enhanced
    // versioning is enabled. If it is, we return the enhanced versioning
    // checkout form. If enhanced
    // versioning is not enabled, we download the file for edit - this is a
    // little different from the
    // edit operation that is available after the checkout - if applets are
    // enabled,
    private void opCheckout(OpContext p) throws Exception
    {
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            p.out.println(p.resources.getString("FILESTORE_BLANK"));
            return;
        }

        // Start by locking the document, if this document is using enhanced
        // versioning, this ensures
        // that the current user has the document locked (and is able to lock
        // it) before the version
        // form is displayed - this avoids a possible error if another user
        // locks it while the form is
        // displayed. It also means, however, that we have to unlock the
        // document if the user clicks
        // the Cancel button on the checkout form.
        p.lockDocument();

        // If enhanced versioning, return the checkout form, otherwise, download
        // the file.
        if (fs.getEnhancedDocumentVersioningLevel())
            doEVCheckoutForm(p, null, "checkout");
        else
        {
            // If we were invoked from the version list, we need to redisplay
            // the vlist before editing the file
            int mode = MODE_IMMEDIATE;
            if (p.redisplay == null || p.redisplay.compareToIgnoreCase("VList") == 0)
            {
                mode = MODE_DEFERRED;
                // If we are invoking an image document, the mode is new window
                if (p.useApplets() && p.isWIEFile(OpContext.VER_SPECIFIED))
                    mode = MODE_NEWWINDOW;
            }
            doDownload(p, ACTION_DOWNLOAD_EDIT, mode);
        }
    }

    /******************************
     * Various "Uploading" operations
     *****************************************************/

    // The upload applet and the upload form both invoke this operation to
    // actually upload a file. The upload
    // action may be triggered by an upload, checkin, or addRendition request.
    // For upload and checkin all we
    // need to know is the document id, for addRendition, we also need to know
    // the version number.
    private void opPostUpload(OpContext p) throws Exception
    {
        // The action parameter tells us whether we are doing an "upload",
        // "checkin", or "addRendition"
        String action = p.getParameter(p.req, "action");

        // The lockme parameter currently used only by the External FileStore
        // Repository.
        // Lock the document before upload the file
        String lockMeParamValue = p.getParameter(p.req, "lockme");
        if ((lockMeParamValue != null) && ("1".compareToIgnoreCase(lockMeParamValue) == 0))
        {
            p.lockDocument();
        }

        // Get the document element
        FileStoreElement fs = p.getFileStoreElement();
        FileStoreElement fsOld = new FileStoreElement(fs);

        // Perform the appropriate action
        if (action.equalsIgnoreCase("addRendition"))
        {
            StorageElement se = new StorageElement(fs, p.getVersion(), "");

            // Set an indication that this is a rendition.
            se.setRenditionLevel(true);

            // Save client location if one has been provided.
            // The location field cannot be 'sanitized.'
            // Get the parameter unencoded so we can use URLDecoder to get the
            // correct value
            String location = p.getParameter("location", false);
            if (location != null && location.length() != 0)
            {
                // UploadFrontEx.java does URLEncoder.encode so we need to
                // decode here or characters such as Swedish "וצה" are messed up
                location = java.net.URLDecoder.decode(location, "UTF8");
                se.setClientLocation(location);
            }

            // Set pool id for the element.
            se.setPoolId(fs.getPoolId().intValue());

            // Upload the rendition
            StorageManager sm = new StorageManager(false);
            sm.upload(fs, se, p.req);

            // Record version deletion if possible
            if ("0".equals(ContextAdapter.getAdapterValue("CREATE_HISTORY", "1")) == false)
            {
                HistoryRecord history = new HistoryRecord();
                history.setAction("FILESTORE_REVISION_ADDED");

                if (fs.isEnhancedDocumentVersioningEnabled())
                {
                    FSVersionElement fsv = p.getVersionElement(se.getVersionNumber());
                    DocVersionElement dve = fsv.getDocVersionElement();
                    history.addVariable("versionNumber", "" + dve.getVersionLabel());
                }
                else
                    history.addVariable("versionNumber", "" + se.getVersionNumber());
                history.setObjectId(fs.getDocumentID());
                history.setComponentType(0);
                history.setComponentId(null);
                history.setModifiedDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
                history.setModifiedBy(SecurityCache.parseDomain(p.req.getRemoteUser()));
                history.setACL(fs.getACL());

                HistorySessionEJB history_ejb = ((HistorySessionEJBHome) ContextAdapter.getHome(ContextAdapter.HISTORYSESSION)).create();
                history_ejb.createHistory(SonoraConstants.FILESTORE_REPOSITORY, null, history);
                history_ejb = null;
            }
        }
        else
        {
            // We allow a checkin only if the document is locked by the current
            // user, or it is not locked and is empty.
            if (action.equalsIgnoreCase("checkin"))
            {
                if (fs.IsLocked && !p.isLockedByCurrentUser())
                    throw new DocumentLockedByAnotherUserException();
                else if (!fs.IsLocked && !p.isEmpty())
                    throw new NoDocumentLockException();
            }

            // See if we have a new format document.
            if (fs.isNewStyleDocument())
            {
                StorageElement se = new StorageElement(fs, fs.getPoolId().intValue(), fs.getNextVersion().intValue(), "");

                // Save client location if one has been provided.
                // The location field cannot be 'sanitized.'
                // Get the parameter unencoded so we can use URLDecoder to get
                // the correct value
                String location = p.getParameter("location", false);
                if (location != null && location.length() != 0)
                {
                    // UploadFrontEx.java does URLEncoder.encode so we need to
                    // decode here or characters such as Swedish "וצה" are
                    // messed up
                    location = java.net.URLDecoder.decode(location, "UTF8");
                    se.setClientLocation(location);
                }
                // Set pool id for the element.
                se.setPoolId(fs.getPoolId().intValue());

                try
                {
                    // Upload and check in the file.
                    StorageManager sm = new StorageManager(false);
                    StorageElement rSe = sm.upload(fs, se, p.req);
                    if (rSe == null)
                    {
                        // Nothing was uploaded, thismeans the user cancelled
                        // the upload dialog.
                        // We must unlock the document if we are doing an upload
                        // but
                        // not if we are doing a checkin.
                        if (action.equalsIgnoreCase("upload"))
                            p.unlockDocument();
                    }
                }
                catch (Exception ex)
                {
                    try
                    {
                        if (action.equalsIgnoreCase("upload"))
                            p.unlockDocument();
                    }
                    catch (Exception e)
                    {
                        m_Log.error("opPostUpload", "E_EXCEPTION", e);
                    }
                    throw ex;
                }
            }
            else
            {
                // These are the current and new version elements. If there were
                // none before, then they are also the first (fsv == fsvnew)
                FSVersionElement fsv = null;
                FSVersionElement fsvNew = null;

                if (fs.CurrentVersion != 0)
                    fsv = p.getVersionsSessionEJB().getElement(p.iDocumentId, fs.CurrentVersion);

                if (fsv == null)
                {
                    fsv = new FSVersionElement();
                    fsv.DocumentID = fs.DocumentID;
                    fs.setCurrentVersion(fs.getNextVersion().intValue());
                    fs.setNextVersion(new Integer(fs.getNextVersion().intValue() + 1));
                    fsv.VersionNumber = fs.CurrentVersion;
                    fsvNew = fsv;
                }
                else
                {
                    // We must create a new version element for the incoming
                    // file.
                    fsvNew = new FSVersionElement(fsv);
                    fs.setCurrentVersion(fs.getNextVersion().intValue());
                    fs.setNextVersion(new Integer(fs.getNextVersion().intValue() + 1));
                    fsvNew.VersionNumber = fs.CurrentVersion;
                }

                // TODO: I'm not sure this is doing the right thing - it should
                // always pickup the path from the template!
                // Make sure we use the latest path for a template.
                if (fs.TemplateId != null)
                    fsvNew.PathPrefix = ((FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId())).PathPrefix;
                else
                    fsvNew.PathPrefix = fs.PathPrefix;

                // Save client location if one has been provided.
                // The location field cannot be 'sanitized.'
                // Get the parameter unencoded so we can use URLDecoder to get
                // the correct value
                String location = p.getParameter("location", false);
                if (location != null && location.length() != 0)
                {
                    // UploadFrontEx.java does URLEncoder.encode so we need to
                    // decode here or characters such as Swedish "וצה" are
                    // messed up
                    location = java.net.URLDecoder.decode(location, "UTF8");
                    fsvNew.ClientLocation = location;
                }
                String fullFilePath = fs.getFilePath(fsvNew);
                MkDirs.mkdirs(fullFilePath);

                // Get the content file(s).
                String fileName;
                try
                {
                    MultipartRequest multi = new MultipartRequest(p.req, fullFilePath, null, "c");
                    Enumeration files = multi.getFileNames();
                    String name = (String) files.nextElement();
                    fileName = multi.getFileName(name);
                }
                catch (Exception ex)
                {
                    try
                    {
                        if (action.equalsIgnoreCase("upload"))
                            p.unlockDocument();
                    }
                    catch (Exception e)
                    {
                        m_Log.error("opPostUpload", "E_EXCEPTION", e);
                    }
                    throw ex;
                }

                // No file name given, this means the user cancelled from the
                // upload dialog.
                if (StringUtil.isBlank(fileName))
                {
                    // Nothing was uploaded, we must unlock the document if we
                    // are doing an upload but
                    // not if we are doing a checkin.
                    if (action.equalsIgnoreCase("upload"))
                        p.unlockDocument();
                }
                else
                {
                    // Do the actual check in.
                    try
                    {
                        p.getFileStoreSessionEJB().checkIn(fsOld, fs, fsvNew, fileName);
                    }
                    catch (Exception e)
                    {
                        p.getFileStoreSessionEJB().deleteDocumentVersion(fs, fs.getCurrentVersion() + 1);
                        throw e;
                    }
                }
            }
        }

        if (!p.isUploadDownloadAppletsEnables())
        {
            p.flush();
        }
    }

    // This method is used to TRIGGER the file upload for checkin, upload and
    // addRendition. This may be called
    // directly by the actions methods, or indirectly by opPostEVCheckoutForm
    // after the user clicks OK on the
    // enhanced version checkout form. It normally invokes the upload applet, or
    // returns a non-applet upload
    // file HTML form. However it special cases the checkin of an image being
    // edited with WIE (which isn't
    // really on the client's machine at all and therefore does not need to be
    // uploaded).
    // The action parameter specifies what this method is to do: upload, checkin
    // or addRendition.

    private void triggerFileUpload(OpContext p, String action) throws Exception
    {
        // The behavior of checkin of a WIE file is somewhat different than the
        // other cases: we do NOT
        // upload a file at all, instead, we check in the working copy that is
        // stored on the server.
        if (p.useApplets() && p.isWIEFile(OpContext.VER_CURRENT) && action.equalsIgnoreCase("checkin"))
        {
            FileStoreElement fs = p.getFileStoreElement();

            // Allow a checkin only if the document is locked by the current
            // user, or it is not locked and is empty.
            // Error reporting for these errors isn't terribly important because
            // users will not get the Checkin
            // operation on a menu unless it is valid.
            if (fs.IsLocked && !p.isLockedByCurrentUser())
                throw new DocumentLockedByAnotherUserException();
            else if (!fs.IsLocked && fs.getCurrentVersion() != 0)
                throw new NoDocumentLockException();

            // Do the checkin of the image file cached on the server
            p.getFileStoreSessionEJB().checkInWIE(p.iDocumentId);

            // Force refetching the filestore element and version list now that
            // things have changed
            p.flush();
            return;
        }

        // In the cases below, this operation returns a page
        p.sentPage = true;

        String redisplay = ((p.redisplay != null) && (!p.redisplay.equalsIgnoreCase(""))) ? p.redisplay : "vlist";

        // Trigger the upload the file by running the upload applet or returning
        // the upload form as specified by the "use applets" flag.
        if (p.isUploadDownloadAppletsEnables())
        {
            // This method returns the HTML page that invokes the upload applet,
            // the action parameter indicates
            // the action to be performed when the file is sent to the server
            // ("upload", "checkin", or "addRendition").
            StringVarResolverHashMap resolver = new StringVarResolverHashMap();
            StringVar sv = new StringVar(resolver);

            // Get the client location (if this is a checkin, this is where the
            // file was checked out to). Note if the
            // document is empty, there will be no version record yet.
            String clientLocation = null;
            FSVersionElement fsv = p.getVersionElement(OpContext.VER_CURRENT);
            if (fsv != null)
                clientLocation = fsv.getClientLocation();
            if (clientLocation == null)
                clientLocation = "";
            String rurlParamValue = p.getParameter("rurl");
            // Pass authorization string to help solve the multiple login
            // screens for the applets when using WebSphere.
            String authorization = p.req.getHeader("Authorization");
            resolver.setVariable("authorization", authorization);
            String jsessionid = "";
            if (ContextAdapter.getBooleanValue("ENABLE_HTTP_ONLY", false))
            {
                Cookie cookies[] = p.req.getCookies();
                if (cookies != null)
                {
                    for (int i = 0; i < cookies.length; i++)
                    {
                        if (cookies[i].getName().equalsIgnoreCase("JSESSIONID"))
                        {
                            jsessionid = cookies[i].getValue();
                            break;
                        }
                    }
                }
            }
            resolver.setVariable("jsessionid", jsessionid);
            resolver.setVariable("file", URLEncoder.encode(clientLocation, "UTF-8"));
            resolver.setVariable("nextUrl",  CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req,"FileStore?op=null&id=" + p.sDocumentId + "&redisplay=" + redisplay + (!StringUtil.isBlank(rurlParamValue) ? "&rurl=" + rurlParamValue : "")));
            resolver.setVariable("operation", CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req, "FileStore?op=upload&applet=1&id=" + p.sDocumentId + "&action=" + action + "&redisplay=" + redisplay + "&version=" + p.getVersionString()));
            resolver.setVariable("errorUrl", "FileStore?op=unlock&id=" + p.sDocumentId + "&redisplay=" + redisplay);

            // Set the url to go to if the user cancels the upload from the
            // applet.
            // Note that we do not want to pass a cancel url for checkin.
            if (action.equalsIgnoreCase("upload"))
                resolver.setVariable("cancelUrl",  CSRFGuardTokenResolver.appendURLWithCSRFToken(p.req,"FileStore?op=unlock&id=" + p.sDocumentId + "&redisplay=" + redisplay + (!StringUtil.isBlank(rurlParamValue) ? "&rurl=" + rurlParamValue : "")));
            else
                resolver.setVariable("cancelUrl", "");


  String html = sv.resolveNames(p.resources.getString("RUN_UPLOAD_APPLET"));
  returnPage(p, html);
            
        }
        else
        {
            // This method returns the HTML upload form page, the action
            // parameter indicates
            // the action to be performed when the file is sent to the server
            // ("upload", "checkin", or "addRendition").
            StringVarResolverHashMap resolver = new StringVarResolverHashMap();
            StringVar sv = new StringVar(resolver);

            // Fetch the FileStore element in question (to get its title)
            FileStoreElement fs = p.getFileStoreElement();

            // Fetch the version element (to get a default filename)
            FSVersionElement fsv = p.getVersionElement(OpContext.VER_CURRENT);

            // Setup the variables in the StringVar resolver
            resolver.setVariable("Title", fs.Title == null ? "" : HtmlUtil.toSafeHtml(fs.getTitle()));
            resolver.setVariable("Filename", fsv == null || fsv.FileName == null ? "" : fsv.FileName);
            resolver.setVariable("DocumentID", p.sDocumentId);
            String rurlParamValue = p.getParameter("rurl");
            resolver.setVariable("operation", "FileStore?op=upload&id=" + p.sDocumentId + "&action=" + action + "&redisplay=" + redisplay + (p.isVersionSpecified() ? "&version=" + p.getVersionString() : "") + (!StringUtil.isBlank(rurlParamValue) ? "&rurl=" + rurlParamValue : ""));

            // Process and output HTML
            String output = CSRFGuardTokenResolver.addCsrfTokenToResponse(sv.resolveNames(p.resources.getString("UPLOAD_FORM")), "form", "action", p.req);
            p.out.println(L10nUtils.localizeTokens(output, UserElement.getUserLocale(p.req)));
        }
    }

    // This method is invoked when the Enhanced Versioning Checkout form is
    // posted back to the server, it
    // saves the extended versioning fields (potentially returning a conflict
    // page which when posted
    // repeats this post) and then performs the action(s) appropriate for the
    // original
    // operation, prompting to upload the file.
    private void opPostEVCheckinForm(OpContext p) throws Exception
    {
        // Get the action the form was displayed for (checkin or upload)
        String action = p.getParameter(p.req, "action");

        // If the user clicked the cancel button, just return (the dispatcher
        // will redisplay the version list) - no harm done
        if (p.getParameter(p.req, "submitCancel") != null)
        {
            // If the form was displayed as part of an upload operation, we
            // locked the document
            // in opUpload, now that they've canceled the upload, don't forget
            // to unlock the document.
            if (action.equalsIgnoreCase("upload"))
                p.unlockDocument();
            return;
        }

        // Update document version element.
        ArrayList conflicts = new ArrayList();
        TimeZone tz = p.getTimeZone();

        // check for duplicate labels
        if (action.equalsIgnoreCase("checkin"))
        {
            // Version label is a form variable, we cannot 'sanitize' it.
            String versionLabel = UtilRequest.getParameter(p.req, "versionLabel");
            if (versionLabel.length() == 0) // we do not allow empty version
                                            // labels either
            {
                // Set the versionLabelerror text
                doEVCheckinForm(p, "checkin", null, "DOCVERSION_LABEL_ERR_EMPTY");
                return;
            }
            else if (versionLabel.length() > VersionLabel.MAX_VERSION_LABEL_SIZE)
            {
                // Set the versionLabelerror text
                doEVCheckinForm(p, "checkin", null, "DOCVERSION_LABEL_ERR_TOLONG");
                return;
            }
        }

        DocVersionProperties dvp = new DocVersionProperties(p.iDocumentId, p.getVersion());
        boolean bSuccess = dvp.fromHttp(p.req, UserElement.getUserLocale(p.req), tz);
        if (!bSuccess)
        {
            doEVCheckinForm(p, action, dvp, "");
            return;
        }

        // If there was a conflict, return the conflict page which when posted
        // will return back here
        if (!dvp.saveChanges(conflicts))
        {
            ConflictResolutionPage conflictsPage = new ConflictResolutionPage(conflicts);
            p.out.println(conflictsPage.getHtml("FileStore?op=newVersion&id=" + p.sDocumentId + "&version=" + p.getVersionString(), UserElement.getUserLocale(p.req), tz));
            return;
        }

        // Complete the upload/checkin operation. What we do depends on the
        // original action that caused this form
        // to be displayed (this is stored in the hidden field named "action").
        // Either "upload" or "checkin".
        // triggerFileUpload(p, action, "vlist");
        triggerFileUpload(p, action);
    }

    private void doEVCheckinForm(OpContext p, String action) throws Exception
    {
        doEVCheckinForm(p, action, null, "");
    }

    // This method returns the enhanced versioning checkin form. When this form
    // is posted, a
    // DocVersionElement will be created for the new version with the values
    // entered in the form.
    // This method is used by both checkin and upload. The action parameter is
    // used to differentiate
    // between "upload" and "checkin". This is written into the hidden field
    // named "action" on the
    // form so the doPostCreateVersionForm method knows what to do when the form
    // is posted.
    private void doEVCheckinForm(OpContext p, String action, DocVersionProperties dvp, String errRes) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        FileStoreElement fs = p.getFileStoreElement();
        int parentVersion = fs.getCurrentVersion();

        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
        StringVar varResolver = new StringVar(stringVars);

        DocVersionElement priorDve = null;

        // Generate a version label for the new version based on the label on
        // the current version
        VersionLabel versionLabel = null;
        if (parentVersion > 0)
        {
            priorDve = p.getDocVersionElement(parentVersion);

            versionLabel = generateNextVersionLable(priorDve);
        }
        else
            versionLabel = new VersionLabel(); // Default Version String

        // See if the next version record already exists (the user may have
        // locked, then unlocked the document)
        DocVersionElement newDve = p.getDocVersionElement(fs.getNextVersion().intValue());
        if (newDve == null)
        {
            newDve = new DocVersionElement(fs, fs.getNextVersion().intValue());
            newDve.setVersionLabel(versionLabel.toString());
            newDve = p.getFileStoreSessionEJB().createDocumentVersionElement(newDve);
        }
        else
            newDve.setVersionLabel(versionLabel.toString());

        // Make all the properties of the version record available to the form
        if (null == dvp)
            dvp = new DocVersionProperties(newDve);
        PropertyAsStringResolver propResolver = new PropertyAsStringResolver(dvp);
        stringVars.chainResolvers(propResolver);

        String rurlParamValue = p.getParameter("rurl");

        // Add in the standard document properties
        stringVars.setVariable("documentId", p.sDocumentId);
        stringVars.setVariable("version", "" + newDve.getVersionNumber());
        stringVars.setVariable("title", fs.getTitle() == null ? "" : HtmlUtil.toSafeHtml(fs.getTitle()));
        stringVars.setVariable("action", action);
        stringVars.setVariable("redisplay", p.redisplay == null ? "vlist" : p.redisplay);
        stringVars.setVariable("rurl", StringUtil.isBlank(rurlParamValue) ? "" : rurlParamValue);
        stringVars.setVariable("labelError", errRes.length() > 0 ? p.resources.getString(errRes) : "");

        Locale locale = UserElement.getUserLocale(p.req);
        // Add in any properties from the previous version record (that's where
        // the checkout comment etc were stored).
        if (priorDve != null)
        {
            String strCheckoutDate = "";
            if (priorDve.getCheckoutDate() != null)
            {
                String pattern = TimestampUtil.getFormatSpecFromPattern(TimestampUtil.DATETIME_FORMAT, locale, true);
                ISimpleDateFormat sdf = CalendarFactory.getISimpleDateFormatInstance(pattern, locale);
                sdf.setTimeZone(p.getTimeZone());

                strCheckoutDate = sdf.format(priorDve.getCheckoutDate());
            }

            stringVars.setVariable("checkoutComment", priorDve.getCheckoutComment() == null ? "" : HtmlUtil.toSafeHtml(priorDve.getCheckoutComment()));
            stringVars.setVariable("checkoutDate", strCheckoutDate);
            stringVars.setVariable("checkedOutBy", priorDve.getCheckedOutBy() == null ? "" : priorDve.getCheckedOutBy());
            stringVars.setVariable("showCheckout", (priorDve.getCheckoutComment() == null ? "style=\"display:none\"" : ""));
        }
        else
            stringVars.setVariable("showCheckout", "style=\"display:none\"");

        // Process and output HTML
        String output = varResolver.resolveNames(p.resources.getString("DOCVERSION_CHECKIN_FORM"), locale, p.getTimeZone());
        p.out.println(L10nUtils.localizeTokens(output, locale));
    }

    // The upload new rendition operation, this is really pretty simple as it
    // just triggers a file upload
    private void opAddRendition(OpContext p) throws Exception
    {
        triggerFileUpload(p, "addRendition");
    }

    // The upload new version operation
    private void opUpload(OpContext p) throws Exception
    {
        // Start by locking the document, if this document is using enhanced
        // versioning, this ensures
        // that the current user has the document locked (and is able to lock
        // it) before the version
        // form is displayed - this avoids a possible error if another user
        // locks it while the form is
        // displayed. If the document is not using enhanced versioning, it
        // ensures nobody else can get
        // to the document while the applet or upload form is being displayed.
        // It also means, however,
        // that we have to unlock the document if the user clicks the Cancel
        // button on the checkout form.
        p.lockDocument();

        // If enhanced versioning is enabled, we have to start by displaying the
        // checkin form
        // Otherwise we upload a new file
        if (p.getFileStoreElement().getEnhancedDocumentVersioningLevel())
        {
            doEVCheckinForm(p, "upload");
        }
        else
        {
            triggerFileUpload(p, "upload");

        }
    }

    // The checkin new version operation
    private void opCheckin(OpContext p) throws Exception
    {
        FileStoreElement fs = p.getFileStoreElement();

        // Allow a checkin only if the document is locked by the current user,
        // or it is not locked and is empty.
        // Error reporting for these errors isn't terribly important because
        // users will not get the Checkin
        // operation on a menu unless it is valid.
        if (fs.IsLocked && !p.isLockedByCurrentUser())
            throw new DocumentLockedByAnotherUserException();
        else if (!fs.IsLocked && fs.getCurrentVersion() != 0)
            throw new NoDocumentLockException();

        // If enhanced versioning is enabled, we have to start by displaying the
        // checkin form
        // Otherwise we upload a new file
        if (!fs.getEnhancedDocumentVersioningLevel())
        {
            triggerFileUpload(p, "checkin");
        }
        else
            doEVCheckinForm(p, "checkin");
    }

    private void opUnlock(OpContext p) throws Exception
    {
        // Unlock the document
        p.unlockDocument();
    }

    private void opLock(OpContext p) throws Exception
    {
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            p.out.println(p.resources.getString("FILESTORE_BLANK"));
            return;
        }

        // Start by locking the document, if this document is using enhanced
        // versioning, this ensures
        // that the current user has the document locked (and is able to lock
        // it) before the version
        // form is displayed - this avoids a possible error if another user
        // locks it while the form is
        // displayed. It also means, however, that we have to unlock the
        // document if the user clicks
        // the Cancel button on the checkout form.
        p.lockDocument();

        // Flush our cached version list so we get the in-progress version (with
        // images and applets)
        p.flush();

        // If enhanced versioning, return the checkout form, otherwise, download
        // the file.
        if (fs.getEnhancedDocumentVersioningLevel())
            doEVCheckoutForm(p, null, "lock");
    }

    private void opPublish(OpContext p) throws Exception
    {
        FileStoreSessionEJB storeSession = p.getFileStoreSessionEJB();
        DocVersionElement dve = storeSession.getDocumentVersionElement(p.iDocumentId, p.getVersion());

        dve.setPublished(true);
        storeSession.updateDocumentVersionElement(dve, dve, true);
    }

    private void opUnpublish(OpContext p) throws Exception
    {
        FileStoreSessionEJB storeSession = p.getFileStoreSessionEJB();
        DocVersionElement dve = storeSession.getDocumentVersionElement(p.iDocumentId, p.getVersion());

        dve.setPublished(false);
        storeSession.updateDocumentVersionElement(dve, dve, true);
    }

    // Handle the post-back of the version details form.
    private void opPostEVDetailsForm(OpContext p) throws Exception
    {
        // What we do depends on which button they clicked on
        if (p.getParameter(p.req, "Submit") != null)
        {
            // The only version we let you change is the "in-progress" version,
            // we can't use the version parameter
            // on the Url since the in-progress version doesn't really exist.
            DocVersionProperties dvp = new DocVersionProperties(p.iDocumentId, p.getFileStoreElement().getCurrentVersion());

            TimeZone tz = p.getTimeZone();
            boolean bSuccess = dvp.fromHttp(p.req, UserElement.getUserLocale(p.req), tz);

            if (!bSuccess) // Property validation errors occured, send it back
            {
                opEVDetails(p, dvp);
                return;
            }

            // Update document version element, check for conflicts and return a
            // conflict page if necessary
            ArrayList conflicts = new ArrayList();
            if (!dvp.saveChanges(conflicts))
            {
                ConflictResolutionPage conflictsPage = new ConflictResolutionPage(conflicts);
                p.out.println(conflictsPage.getHtml("FileStore?op=EVDetails&id=" + p.sDocumentId + "&version=" + p.getVersionString(), UserElement.getUserLocale(p.req), tz));
            }
        }
        else if (p.getParameter(p.req, "submitNext") != null)
        {
            p.setVersion(p.getNextVersion());
            opEVDetails(p, null);
        }
        else if (p.getParameter(p.req, "submitPrev") != null)
        {
            p.setVersion(p.getPrevVersion());
            opEVDetails(p, null);
        }
        // Otherwise they clicked cancel, just return the redisplay page
    }

    // Return the version details form.
    private void opEVDetails(OpContext p, DocVersionProperties dvp) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
        StringVar varResolver = new StringVar(stringVars);

        FileStoreElement fs = p.getFileStoreElement();

        // If they did not specify a version number, and the document is locked,
        // they want the version details
        // for the "in-progress" version. Also, if they specify a version number
        // greater than the current version,
        // they want the "in-progress" version. These are actually stored in the
        // DocVersionElement for the
        // most recent (current) version, it is just that we present them
        // differently.
        boolean showInProgress = (!p.isVersionSpecified()) && fs.getIsLocked() || (p.getVersion() > fs.getCurrentVersion());
        String detailsForm;
        Locale locale = UserElement.getUserLocale(p.req);
        // If we can access the document version element, we use the enhanced
        // versioning page
        if (fs.isEnhancedDocumentVersioningEnabled())
        {
            if (null == dvp)
            {
                DocVersionElement dve = p.getDocVersionElement(showInProgress ? OpContext.VER_CURRENT : OpContext.VER_SPECIFIED);

                // Add the document version element property manager to the
                // resolver chain (if we can get it)
                if (dve != null)
                    dvp = new DocVersionProperties(dve);
            }

            if (null != dvp)
            {
                PropertyAsStringResolver propResolver = new PropertyAsStringResolver(dvp);
                stringVars.chainResolvers(propResolver);
            }

            // Add various standard variables
            stringVars.setVariable("templateId", fs.getTemplateId() == null ? p.sDocumentId : fs.getTemplateId().toString());
            stringVars.setVariable("DocumentID", "" + p.iDocumentId);
            stringVars.setVariable("documentId", p.sDocumentId);
            stringVars.setVariable("version", "" + (showInProgress ? fs.getCurrentVersion() + 1 : p.getVersion()));
            stringVars.setVariable("title", fs.getTitle() == null ? "" : HtmlUtil.toSafeHtml(fs.getTitle()));

            // Disable the next and previous version buttons as needed (we
            // disable them rather than hide them so
            // the buttons don't move around as the user clicks on next/prev).
            stringVars.setVariable("disableNext", p.getNextVersion() == -1 ? "disabled" : "");
            if (showInProgress)
                stringVars.setVariable("disablePrev", "");
            else
                stringVars.setVariable("disablePrev", p.getPrevVersion() == -1 ? "disabled" : "");

            if (showInProgress)
            {
                // Use the proper form for the version (the in-progress version
                // can update, others cannot)
                detailsForm = p.resources.getString("DOCVERSION_UPDATE_FORM");
            }
            else
            {
                if (fs.isFieldVersioningEnabled())
                {
                    // Use the proper form for the version (the in-progress
                    // version can update, others cannot)
                    detailsForm = p.resources.getString("DOC_DISPLAY_FORM_VERSION_AND_FIELDS");
                }
                else
                {
                    // Use the proper form for the version (the in-progress
                    // version can update, others cannot)
                    detailsForm = p.resources.getString("DOCVERSION_DISPLAY_FORM");
                }
            }

        }
        else
        {
            // Add various standard variables
            stringVars.setVariable("templateId", fs.getTemplateId() == null ? p.sDocumentId : fs.getTemplateId().toString());
            stringVars.setVariable("DocumentID", "" + p.iDocumentId);
            stringVars.setVariable("documentId", p.sDocumentId);
            stringVars.setVariable("versionLabel", "" + (p.getVersion()));
            stringVars.setVariable("version", "" + (showInProgress ? fs.getCurrentVersion() + 1 : (p.getVersion())));

            stringVars.setVariable("title", fs.getTitle() == null ? "" : HtmlUtil.toSafeHtml(fs.getTitle()));

            // Disable the next and previous version buttons as needed (we
            // disable them rather than hide them so
            // the buttons don't move around as the user clicks on next/prev).

            int prevVersion = p.getPrevVersion();
            int nextVersion = p.getNextVersion();

            // if the next/prev version is greater then the versions then next
            // version is the
            // In progress version.

            if (showInProgress)
            {
                stringVars.setVariable("disablePrev", "");
                stringVars.setVariable("disableNext", "disabled");
                // Use the proper form for the version (the in-progress version
                // can update, others cannot)
                detailsForm = p.resources.getString("DOC_DISPLAY_FORM_INPROGRESS_FIELDS");
            }
            else
            {
                stringVars.setVariable("disablePrev", prevVersion == -1 ? "disabled" : "");
                stringVars.setVariable("disableNext", nextVersion == -1 ? "disabled" : "");

                // Use the proper form for the version (the in-progress version
                // can update, others cannot)
                detailsForm = p.resources.getString("DOC_DISPLAY_FORM_FIELDS");
            }
        }

        detailsForm = varResolver.resolveNames(detailsForm, locale, p.getTimeZone());
        p.out.println(L10nUtils.localizeTokens(detailsForm, locale));
    }

    private void opSignature(OpContext p) throws Exception
    {
        // This operation always returns a page
        p.sentPage = true;

        // The element ID is required for this request
        if (p.sElementId == null)
        {
            if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
            {
                p.out.println(p.resources.getString("FILESTORE_USAGE_DISPLAY"));
            }
            return;
        }

        // Verify the signature
        DigitalSignature sig = new DigitalSignature();
        int result = sig.verify(p.iElementId);

        // Report the results of the verification
        String vfyResult = null;
        Locale locale = UserElement.getUserLocale(p.req);
        ResourceBundle historyResources = ResourceBundle.getBundle("com.eistream.sonora.history.HistoryResource", locale);
        switch (result)
        {
        case DigitalSignature.SIGNATURE_VALID:
            p.out.println(L10nUtils.localizeTokens(p.resources.getString("VERIFY_VALID"), locale));
            vfyResult = historyResources.getString("DGSG_VERIFY_VALID");
            break;
        case DigitalSignature.SIGNATURE_INVALID:
            p.out.println(L10nUtils.localizeTokens(p.resources.getString("VERIFY_INVALID"), locale));
            vfyResult = historyResources.getString("DGSG_VERIFY_INVALID");
            break;
        case DigitalSignature.SIGNATURE_NONE:
            p.out.println(L10nUtils.localizeTokens(p.resources.getString("VERIFY_NONE"), locale));
            vfyResult = historyResources.getString("DGSG_VERIFY_NONE");
            break;
        case DigitalSignature.SIGNATURE_MISSING:
            p.out.println(L10nUtils.localizeTokens(p.resources.getString("VERIFY_MISSING"), locale));
            vfyResult = historyResources.getString("DGSG_VERIFY_MISSING");
            break;
        case DigitalSignature.DATA_FILE_MISSING:
            p.out.println(L10nUtils.localizeTokens(p.resources.getString("VERIFY_FILE_MISSING"), locale));
            vfyResult = historyResources.getString("DGSG_VERIFY_FILE_MISSING");
            break;
        default:
            vfyResult = "Unknown result";
            break;
        }

        // Add history record for download if required
        if ("0".equals(ContextAdapter.getAdapterValue("CREATE_HISTORY", "1")) == false)
        {
            FileStoreElement fs = p.getFileStoreElement();
            FileStoreElement template = (fs.IsTemplate != FileStoreElement.TYPE_INSTANCE) ? fs : (FileStoreElement) TemplatesCache.getTemplate(TemplatesCache.FILESTORE_TEMPLATES, fs.getTemplateId());
            if (template.isReadOnlyHistoryEnabled())
            {
                FSVersionElement fsv = p.getVersionElement(p.getVersion());
                StorageElement se = fsv.getStorageElement();

                HistoryRecord history = new HistoryRecord();
                history.setObjectId(fs.getDocumentID());
                history.setComponentType(0);
                history.setComponentId(null);

                history.addVariable("result", "" + vfyResult);

                if (se.isRendition())
                {
                    if (fs.isEnhancedDocumentVersioningEnabled())
                    {
                        DocVersionElement dve = fsv.getDocVersionElement();
                        history.setAction("FILESTORE_EDV_REVISION_VFYDGSG");
                        history.addVariable("versionLabel", "" + dve.getVersionLabel());
                    }
                    else
                    {
                        history.setAction("FILESTORE_REVISION_VFY_DGSG");
                        history.addVariable("versionNumber", "" + se.getVersionNumber());
                    }
                }
                else
                {
                    if (fs.isEnhancedDocumentVersioningEnabled())
                    {
                        DocVersionElement dve = fsv.getDocVersionElement();
                        history.setAction("FILESTORE_EDV_VERSION_VFYDGSG");
                        history.addVariable("versionLabel", "" + dve.getVersionLabel());
                    }
                    else
                    {
                        history.setAction("FILESTORE_VERSION_VFY_DGSG");
                        history.addVariable("versionNumber", "" + se.getVersionNumber());
                    }
                }

                history.setModifiedDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
                history.setModifiedBy(SecurityCache.parseDomain(p.req.getRemoteUser()));
                history.setACL(fs.getACL());

                HistorySessionEJB history_ejb = ((HistorySessionEJBHome) ContextAdapter.getHome(ContextAdapter.HISTORYSESSION)).create();
                history_ejb.createHistory(SonoraConstants.FILESTORE_REPOSITORY, null, history);
                history_ejb = null;
            }
        }
    }

    private void opDelVersion(OpContext p) throws Exception
    {
        // Get current FileStore Element
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            p.out.println(p.resources.getString("FILESTORE_BLANK"));
            return;
        }

        p.getFileStoreSessionEJB().deleteDocumentVersion(fs, p.getVersion());

        // The version list is no longer valid.
        p.flush();

    }

    private void opDelRendition(OpContext p) throws Exception
    {
        // This request requires the element id.
        if (p.sElementId == null)
        {
            if (!ContextAdapter.getBooleanValue("STRICT_SECURITY", false))
            {
                p.out.println(p.resources.getString("FILESTORE_USAGE_DISPLAY"));
            }
            return;
        }

        p.getFileStoreSessionEJB().deleteRendition(p.iElementId);

        // The version list is no longer valid.
        p.flush();
    }

    // Return a page that closes the current window.
    private void opClose(OpContext p) throws Exception
    {
        p.sentPage = true;

        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();
        StringVar stringVar = new StringVar(stringVars);
        stringVars.setVariable("onLoad", p.onLoad == null ? "" : p.onLoad);

        p.out.println(stringVar.resolveNames(p.resources.getString("FILESTORE_CLOSE")));
    }

    private void opGetContent(OpContext p) throws Exception
    {
        p.sentPage = true;
        p.calledByApplet = true; // Hack - instuct this servlet to send the
                                 // short description of error during exception.
        byte[] content = RepositoryManager.getContent(p.sDocumentId);
        if (content == null)
            content = new byte[0];
        p.resp.getOutputStream().write(content);
    }

    private void opGetContentPages(OpContext p) throws Exception
    {
        p.sentPage = true;
        p.calledByApplet = true; // Hack - instuct this servlet to send the
                                 // short description of error during exception
        byte[] content = RepositoryManager.getContent(p.sDocumentId, p.getParameter("Pages"));
        if (content == null)
            content = new byte[0];
        p.resp.getOutputStream().write(content);
    }

    private void opCreateObject(OpContext p) throws Exception
    {
        try
        {
            p.sentPage = true;
            String objectId = RepositoryManager.createEx(p.getParameter("template"), FieldPropertiesTOArrayUtil.fromXML(p.getParameter("fieldProperties")));
            p.resp.getOutputStream().write(objectId.getBytes());
        }
        catch (Exception e)
        {
            String message = SonoraExceptionUtil.unwrap(e).getLocalizedMessage();
            p.resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
            throw e;
        }
    }

    private void opFixDocumentCopies(OpContext p) throws Exception
    {
        try
        {
            p.sentPage = true;
            FileStoreElement fileStoreElement = p.getFileStoreElement();
            FixDocumentRequest aRequest;
            if (fileStoreElement.getTemplateId() == null)
            {
                aRequest = new FixDocumentRequest(fileStoreElement);
            }
            else
            {
                aRequest = new FixDocumentRequest(p.getFileStoreSessionEJB().getElement(fileStoreElement.getTemplateId()));
            }

            aRequest.queueIfAbsent();
            p.resp.getOutputStream().write("Fix Document copies request is queued".getBytes());
        }
        catch (Exception e)
        {
            p.resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    private void opOutputImageMap(OpContext p) throws Exception
    {
        p.sentPage = true;

        // Get current FileStore Element
        FileStoreElement fs = p.getFileStoreElement();
        if (fs == null)
        {
            p.out.println(p.resources.getString("FILESTORE_BLANK"));
            return;
        }

        DocumentMap map = new DocumentMap(fs, p.getVersion());
        String html = map.toHtml(UserElement.getUserLocale(p.req));

        p.out.println(html);
    }

    /**
     * This class provides access to all of the parameters needed to process
     * Filestore requests
     */
    static class OpContext
    {
        HttpServletRequest req;
        HttpServletResponse resp;
        PrintWriter out;

        boolean getParametersSetup = false;
        String qs;
        boolean isMultipartForm;

        String sDocumentId;
        String op;
        BigDecimal iDocumentId;
        String sVersion;
        private int iVersion;
        String sElementId;
        long iElementId;
        boolean calledByApplet = false;
        String redisplay;
        String sKey;
        String target;
        ResourceBundle resources;
        int mode;
        StringVarResolverHashMap stringVars = new StringVarResolverHashMap();

        // This tells the dispatcher if the operation method already sent a
        // response page or not.
        boolean sentPage = false;

        // This is passed on to VList by the dispatcher, specifying an onLoad
        // event handler (used to launch the WIE in a new window)
        String onLoad = null;

        FileStoreSessionEJB storeSession = null;
        FileStoreElement fs = null;

        FSVersionsSessionEJB versionsSession = null;
        FSVersionElement currentFSVersion = null;
        FSVersionElement thisFSVersion = null;

        DocVersionElement currentDocVersion = null;
        DocVersionElement thisDocVersion = null;

        ArrayList versionList = null;

        String user = null;

        // Constants used by the getFSVersion and getDocVerson methods
        public static final int VER_CURRENT = -1;
        public static final int VER_SPECIFIED = -2;
        public static final int VER_NEXT = -3;
        public static final int VER_PREV = -4;

        // Gets a sanitized parameter.
        String getParameter(HttpServletRequest req, String name) throws Exception
        {
            String value = UtilRequest.getParameter(req, name);
            if (!StringUtil.isBlank(value))
            {
                // Included other characters to prevent cross site script attack
                value = value.replaceAll("[<>%\\[\\](){}]", "");
            }

            // String value = UtilRequest.getStringParameter(req, name, null,
            // UtilRequest.DEFAULT_SanityCheck);
            return value;
        }

        // Get a parameter value from the query string (used to work around a
        // WebSphere bug)
        String getParameter(String param) throws Exception
        {
            return getParameter(param, true);
        }

        // Get a parameter value from the query string (used to work around a
        // WebSphere bug (new interface need to get URLencoded location)
        String getParameter(String param, boolean bCheckMultiPart) throws Exception
        {
            // Setup member varibles we need (we need the content type to see if
            // we are dealing with a post of a multipart/forms)
            if (!getParametersSetup)
            {
                qs = req.getQueryString();
                String contentType = req.getContentType();
                isMultipartForm = (contentType != null && contentType.lastIndexOf("multipart/form") != -1);
                getParametersSetup = true;
            }

            // If it is not a multipart/form just get the parameters from the
            // request
            if (bCheckMultiPart && !isMultipartForm)
                return getParameter(req, param);

            // If we are responding to a file upload post (multipart/form) get
            // the parameter values using the query string because WebSphere
            // does not handle multipart/forms
            param += "=";
            String value = null;

            // If we don't have a query string return null
            if (qs != null)
            {
                // Find the parameter in the string
                int index = qs.lastIndexOf(param);
                if (index != -1)
                {
                    int length = param.length();

                    // Get the param value (to the end of the string)
                    String temp = qs.substring(index + length);
                    if (temp != null)
                    {
                        // The parameter value is terminated by the end of
                        // string or the separator '&'
                        int end = temp.indexOf('&');
                        if (end == -1)
                            value = temp;
                        else
                            value = temp.substring(0, end);
                    }
                }
            }
            return value;
        }

        OpContext(HttpServletRequest req, HttpServletResponse resp, PrintWriter out, String defaultOp) throws Exception
        {
            this.req = req;
            this.resp = resp;
            this.out = out;

            String sCalledByApplet;

            // Get the standard parameters to the servlet
            op = getParameter("op");
            sDocumentId = getParameter("id");
            sVersion = getParameter("version");
            sElementId = getParameter("elId");
            sCalledByApplet = getParameter("applet");
            redisplay = getParameter("redisplay");
            sKey = getParameter("key");
            String sMode = getParameter("mode"); // download mode
                                                 // (immediate,deferred,newwindow)
            user = CurrentUser.currentServletUser(req);
            target = getParameter("target");

            // Special case test to see if the version number starts with {{ -
            // if it does we don't have a version number
            // This occurs with some of the archive operations and also for
            // non-field versioned documents.
            if (sVersion != null)
                sVersion = sVersion.trim();

            if (sVersion != null && (sVersion.startsWith("{{") || sVersion.trim().equals("0") || sVersion.compareToIgnoreCase("") == 0))
                sVersion = null;

            // Convert the parameters to numeric values
            if (sDocumentId != null)
            {
                if (sDocumentId.indexOf(RepositoryKey.ID_SEP) > -1)
                    sDocumentId = RepositoryManager.normalizeObjectId(sDocumentId);
                try
                {
                    iDocumentId = (sDocumentId == null) ? null : new BigDecimal(sDocumentId);
                }
                catch (NumberFormatException nfe)
                {
                    RepositoryKey rk = RepositoryKey.fromString(sDocumentId);
                    if (RepositoryElement.isNativeRepository(rk.repositoryId.intValue()) && (rk.repositoryId.intValue() != SonoraConstants.PROCESS_REPOSITORY))
                        throw nfe;
                }
            }
            if(sElementId !=null)
            {
            sElementId = sElementId.replace(" ", "");
            }
            iElementId = (sElementId == null) ? -1 : Long.parseLong(sElementId);
            iVersion = (sVersion == null) ? -1 : Integer.parseInt(sVersion);

            // Determine whether called by an applet or not.
            calledByApplet = sCalledByApplet != null && !sCalledByApplet.equalsIgnoreCase("0");

            // Set the download mode option (defaults to MODE_DEFAULT)
            mode = MODE_DEFAULT;
            if (sMode != null)
            {
                if (sMode.compareToIgnoreCase("immediate") == 0)
                    mode = MODE_IMMEDIATE;
                if (sMode.compareToIgnoreCase("deferred") == 0)
                    mode = MODE_DEFERRED;
                if (sMode.compareToIgnoreCase("newwindow") == 0)
                    mode = MODE_NEWWINDOW;
            }

            // Set the default operation code if we didn't get one
            if (op == null)
                op = defaultOp;

            // Get access to our resources
            resources = ResourceBundle.getBundle("com.eistream.sonora.filestore.FileStoreServletResource", UserElement.getUserLocale(req));
        }

        // This method flushes the cached elements, this is necessary after a
        // lock/unlock because they
        // would retain the incorrect lock state.
        void flush()
        {
            fs = null;
            currentFSVersion = null;
            thisFSVersion = null;
            currentDocVersion = null;
            thisDocVersion = null;
            versionList = null;
            sElementId = null; // if the string is null, nobody uses the long
                               // value
        }

        // Shortcut to test the operation
        boolean opIs(String test)
        {
            return op.equalsIgnoreCase(test);
        }

        // Get the timezone to be used
        TimeZone getTimeZone()
        {
            TimeZone tz = TimeZone.getDefault();
            try
            {
                ImmutableProperty userProps = UserCache.getUserElementInst(user);
                if (userProps != null)
                    tz = TimeZone.getTimeZone((String) (userProps.get("timeZone").getValue()));
            }
            catch (Exception err)
            {
                // do nothing use default timezone
            }
            return tz;
        }

        FileStoreSessionEJB getFileStoreSessionEJB() throws Exception
        {
            // If we haven't already gotten the Filestore session bean, get it
            // now
            if (storeSession == null)
            {
                FileStoreSessionEJBHome fileStoreHome = (FileStoreSessionEJBHome) ContextAdapter.getHome(ContextAdapter.FILESTORESESSION);
                storeSession = fileStoreHome.create();
            }
            return storeSession;
        }

        FileStoreElement getFileStoreElement() throws Exception
        {
            if (fs == null)
                fs = getFileStoreSessionEJB().getElement(iDocumentId, false);
            return fs;
        }

        FSVersionsSessionEJB getVersionsSessionEJB() throws Exception
        {
            if (versionsSession == null)
            {
                FSVersionsSessionEJBHome FSVersionsHome = (FSVersionsSessionEJBHome) ContextAdapter.getHome(ContextAdapter.FSVERSIONSSESSION);
                versionsSession = FSVersionsHome.create();
            }
            return versionsSession;
        }

        // Return the requested FSVersionElement for this document. There are
        // some special values that may be
        // specified: VER_CURRENT to get the element for the current document
        // version and VER_SPECIFIED to
        // get the version specified by the version= parameter to the servlet.
        // These two specific versions are
        // cached so it doesn't cost anything to get them repeatedly.
        FSVersionElement getVersionElement(int version) throws Exception
        {
            // If they've asked for the specified version, but no version was
            // specified, use the current version
            if (version == VER_SPECIFIED && sVersion == null)
                version = VER_CURRENT;

            int saveVersion = version;
            if (version == VER_CURRENT)
            {
                if (currentFSVersion != null)
                    return currentFSVersion;
                version = getFileStoreElement().getCurrentVersion();
            }
            else if (version == VER_SPECIFIED)
            {
                if (thisFSVersion != null)
                    return thisFSVersion;
                version = iVersion;
            }

            // If this is an empty document, return null
            if (version <= 0)
                return null;

            // Get the version element
            FSVersionElement fsv = null;
            if (fs.isOldStyleDocument())
                fsv = getVersionsSessionEJB().getElement(iDocumentId, version);
            else
            {
                if (sElementId != null)
                {
                    // A non-null sElementId always refers to a rendition.
                    // Note that this will throw if the user has no access
                    // rights.
                    fsv = getFileStoreSessionEJB().getRenditionElement(iDocumentId.longValue(), version, iElementId);
                }
                else
                {
                    try
                    {
                        // Get the least expensive element. Note that if we have
                        // an image document, we will get back the least
                        // expensive map (unless we are doing standard
                        // versioning in which case we'll get back the tiff
                        // file.)
                        // Note that this will throw if the user has no access
                        // rights.
                        fsv = getFileStoreSessionEJB().getVersionElement(fs, version);
                    }
                    catch (Exception e)
                    {
                        // We assume the user has no access to the requested
                        // version. Give
                        // him access to the first published version he is
                        // entitled to (if any).
                        ArrayList versions = getVersionList();
                        int count = versions.size();
                        if (count == 0)
                        {
                            // There are no accessible versions.
                            throw e;
                        }

                        // Sort the list so the primary renditions appears last.
                        Comparator comparator = new SortByRenditionFileName();
                        Collections.sort(versions, comparator);

                        fsv = (FSVersionElement) versions.get(count - 1);
                    }
                }
            }

            // Save away the current and specified version records to make them
            // cheap to get again.
            if (saveVersion == VER_CURRENT)
                currentFSVersion = fsv;
            else if (saveVersion == VER_SPECIFIED)
                thisFSVersion = fsv;

            return fsv;
        }

        // Return the requested DocVersionElement for this document. There are
        // two special values that may be
        // specified: VER_CURRENT to get the element for the current document
        // version and VER_SPECIFIED to
        // get the version specified by the version= parameter to the servlet.
        // These two specific versions are
        // cached so it doesn't cost anything to get them repeatedly. In
        // addition, the values VER_NEXT and VER_PREV
        // are supported to get the next and previous version elements (relative
        // to the currently specified version).
        // All of these return a null if the version does not exist.
        DocVersionElement getDocVersionElement(int version) throws Exception
        {
            // If they've asked for the specified version, but no version was
            // specified, use the current version
            if (version == VER_SPECIFIED && sVersion == null)
                version = VER_CURRENT;

            int saveVersion = version;
            if (version == VER_CURRENT)
            {
                if (currentDocVersion != null)
                    return currentDocVersion;
                version = getFileStoreElement().getCurrentVersion();
            }
            else if (version == VER_SPECIFIED)
            {
                if (thisDocVersion != null)
                    return thisDocVersion;
                version = iVersion;
            }
            else if (version == VER_NEXT)
                version = iVersion + 1;
            else if (version == VER_PREV)
                version = iVersion - 1;

            // If this is an empty document, or if they asked for the version
            // previous to the first version there is no version element
            if (version <= 0)
                return null;

            // Get the version element
            DocVersionElement docVersion = null;
            try
            {
                docVersion = getFileStoreSessionEJB().getDocumentVersionElement(iDocumentId, version);
            }
            catch (FinderException error)
            {
                // Ignore FinderExceptions, just return null
            }

            if (saveVersion == VER_CURRENT)
                currentDocVersion = docVersion;
            else if (saveVersion == VER_SPECIFIED)
                thisDocVersion = docVersion;

            return docVersion;
        }

        // This method returns the specified version number, if this is being
        // called the caller wants
        // a version number so if one wasn't specified on the Url, get the
        // current version number.
        public int getVersion() throws Exception
        {
            return (sVersion != null) ? iVersion : getFileStoreElement().getCurrentVersion();
        }

        public String getVersionString() throws Exception
        {
            return "" + getVersion();
        }

        public void setVersion(int version)
        {
            iVersion = version;
            sVersion = "" + version;
            flush();
        }

        public boolean isVersionSpecified()
        {
            return sVersion != null;
        }

        // Get the document's version list
        public ArrayList getVersionList() throws Exception
        {
            // If we haven't already, get the version list in version number
            // order.
            if (versionList == null)
                versionList = getFileStoreSessionEJB().getDocumentElements(getFileStoreElement(), true);
            return versionList;
        }

        // Return the number of the next version after the currently specified
        // version. If the document is
        // locked and the current version is the top version, we synthesize a
        // current+1 version number to
        // represent the "in-progress" version. Note that we have to call
        // getVersion to handle the case
        // where the Url did not specify a version number and we default to the
        // current version.
        public int getNextVersion() throws Exception
        {
            int version = getVersion();
            getFileStoreElement();
            if (version == fs.getCurrentVersion() && fs.getIsLocked())
                return version + 1;

            // Get the version list in version number order.
            ArrayList versions = getVersionList();

            // Loop through all the versions of this document (from newest to
            // oldest)
            int nextVersion = -1;
            for (int i = versions.size() - 1; i > 0; --i)
            {
                // Get version element
                FSVersionElement fsv = (FSVersionElement) versions.get(i);
                if (fsv == null)
                    continue;

                if (fs.isNewStyleDocument() && fsv.getStorageElement().isRendition())
                    continue;

                if (fsv.VersionNumber <= version)
                    break;

                if (fsv.VersionNumber > version)
                    nextVersion = fsv.VersionNumber;
            }

            // There is no next version
            return nextVersion;
        }

        public int getPrevVersion() throws Exception
        {
            int version = getVersion();
            getFileStoreElement();
            if (version > fs.getCurrentVersion())
                return fs.getCurrentVersion();

            // Get the version list in version number order.
            ArrayList versions = getVersionList();

            // Loop through all the versions of this document (from newest to
            // oldest)
            boolean foundVersion = false;
            for (int i = versions.size() - 1; i >= 0; --i)
            {
                // Get version element
                FSVersionElement fsv = (FSVersionElement) versions.get(i);
                if (fsv == null)
                    continue;

                if (fs.isNewStyleDocument() && fsv.getStorageElement().isRendition())
                    continue;

                if (foundVersion)
                    return fsv.VersionNumber;

                if (fsv.VersionNumber == version)
                    foundVersion = true;
            }

            // There is no next version
            return -1;
        }

        // Is the specified version an image document?
        public boolean isWIEFile(int version) throws Exception
        {
            if (ContextAdapter.getBooleanValue("OPEN_WIE_WITH_LOCAL_OS_APPLICATION", false))
            {
                return false;
            }
            if (version == VER_CURRENT)
                version = getFileStoreElement().getCurrentVersion();
            else if (version == VER_SPECIFIED)
                version = iVersion;

            // If the document is empty, then it isn't an image
            if (version == 0)
                return false;

            // If it is old-style, ask the version element
            if (getFileStoreElement().isOldStyleDocument())
                return getVersionElement(version).isWIEFile();

            // If it is new-style, ask the storage element
            FSVersionElement element = getVersionElement(version);
            if(element != null)
            {
                StorageElement se = element.getStorageElement();
                return se.isWIEFile() || se.isMapElement();
            }
            return false;
        }

        boolean lockDocument() throws Exception
        {
            flush();
            return getFileStoreSessionEJB().lock(iDocumentId);
        }

        boolean unlockDocument() throws Exception
        {
            flush();
            return getFileStoreSessionEJB().unlock(iDocumentId);
        }

        // Return true if this document is locked by the current user, false if
        // it is not
        // locked or locked by someone else.
        boolean isLockedByCurrentUser() throws Exception
        {
            return getFileStoreElement().IsLocked && getFileStoreSessionEJB().isLockedByCurrentUser(fs.LockedBy);
        }

        // Return true if this document is empty
        boolean isEmpty() throws Exception
        {
            return getFileStoreElement().getCurrentVersion() != 0;
        }

        // Return true if we are to use applets, otherwise false
        boolean useApplets() throws Exception
        {
            int temp = Integer.parseInt(ContextAdapter.getAdapterValue("ALLOW_APPLETS", "1"));

            // Determine if applets are enabled in the context adapter
            boolean useApplet = useAppletsConditionCalculator(temp);

            // If they are disabled, see if this user is configured to use them
            // anyway
            if (!useApplet)
            {
                ImmutableProperty userProps = UserCache.getUserElementInst(user);
                if (userProps != null)
                {
                    temp = ((Integer) (userProps.get("allowApplets").getValue())).intValue();
                    useApplet = useAppletsConditionCalculator(temp);
                }
            }
            return useApplet;
        }

        private boolean useAppletsConditionCalculator(int appletOptionValue)
        {
            return (appletOptionValue != ALL_APPLETS_DISABLED);
        }

        // Return true if we are to use upload/download applet, otherwise false
        boolean isUploadDownloadAppletsEnables() throws Exception
        {
            int temp = Integer.parseInt(ContextAdapter.getAdapterValue("ALLOW_APPLETS", "1"));

            // Determine if UploadDownload applet is enabled in the context
            // adapter
            boolean useUploadDownloadApplet = isUploadDownloadAppletsEnablesConditionCalculator(temp);

            // If it is disabled, see if this user is configured to use it
            // anyway
            if (temp == ALL_APPLETS_DISABLED)
            {
                ImmutableProperty userProps = UserCache.getUserElementInst(user);
                if (userProps != null)
                {
                    temp = ((Integer) (userProps.get("allowApplets").getValue())).intValue();
                    useUploadDownloadApplet = isUploadDownloadAppletsEnablesConditionCalculator(temp);
                }
            }
            return useUploadDownloadApplet;
        }

        boolean isJNLPEnables() throws Exception
        {
            int temp = Integer.parseInt(ContextAdapter.getAdapterValue("ALLOW_APPLETS", "0"));

            return temp == AppletsOptionsHelper.JNLP_ENABLE;
        }

        private boolean isUploadDownloadAppletsEnablesConditionCalculator(int appletOptionValue)
        {
            return ((appletOptionValue == ALL_APPLETS_ENABLED) || (appletOptionValue == ONLY_UPLOAD_DOWNLOAD_APLLET_ENABLE));
        }

        // Return true if we are to use IForWeb applet, otherwise false
        boolean isIForWebAppletEnable() throws Exception
        {
            int temp = Integer.parseInt(ContextAdapter.getAdapterValue("ALLOW_APPLETS", "1"));

            // Determine if IForWeb applet is enabled in the context adapter
            boolean useIForWebApplet = isIForWebAppletEnableConditionCalculator(temp);

            // If it disabled, see if this user is configured to use it anyway
            if (temp == ALL_APPLETS_DISABLED)
            {
                ImmutableProperty userProps = UserCache.getUserElementInst(user);
                if (userProps != null)
                {
                    temp = ((Integer) (userProps.get("allowApplets").getValue())).intValue();
                    useIForWebApplet = isIForWebAppletEnableConditionCalculator(temp);
                }
            }
            return useIForWebApplet;
        }

        private boolean isIForWebAppletEnableConditionCalculator(int appletOptionValue)
        {
            return ((appletOptionValue == ALL_APPLETS_ENABLED) || (appletOptionValue == ONLY_IFORWEB_APPLET_ENABLE));
        }

        /**
         * Checks is only IForWebApplet is enabled. This function return "true"
         * ONLY if "ALLOW_APPLETS" filestore configuration option is set to
         * 3(see "Configuring Filestore" topic of Case360 help for additional
         * details about filestore configuration options)
         * 
         * @return true if only IForWebApplet is enabled
         * @throws Exception
         */
        boolean isOnlyIForWebAppletEnable() throws Exception
        {
            return (isIForWebAppletEnable() && !isUploadDownloadAppletsEnables());
        }

        /**
         * Checks is only Upload/Download applet is enabled. This function
         * return "true" ONLY if "ALLOW_APPLETS" filestore configuration option
         * is set to 2(see "Configuring Filestore" topic of Case360 help for
         * additional details about filestore configuration options)
         * 
         * @return true if only Upload/Download applet is enabled
         * @throws Exception
         */
        boolean isOnlyUploadDownloadAppletsEnables() throws Exception
        {
            return (isUploadDownloadAppletsEnables() && !isIForWebAppletEnable());
        }

        String getPermissions() throws Exception
        {
            StringBuilder permissions = new StringBuilder();

            if (SecurityCache.hasPermission(user, SecurityCache.SECURITY_PERMISSIONWRITE_S, fs.ACL, fs, false))
            {
                permissions.append(Contract.PERM_WRITE);
                permissions.append(",").append(Contract.PERM_INSERTPAGES);
                permissions.append(",").append(Contract.PERM_DELETEPAGES);
                permissions.append(",").append(Contract.PERM_SCAN);
                permissions.append(",").append(Contract.PERM_DESPECKLE);
                permissions.append(",").append(Contract.PERM_DESKEW);
                permissions.append(",").append(Contract.PERM_CROP);
                permissions.append(",").append(Contract.PERM_CHECKIN);
                permissions.append(",").append(Contract.PERM_CHECKOUT);
                permissions.append(",").append(Contract.PERM_UNLOCK);
                permissions.append(",").append(Contract.PERM_ANNOTATE);
            }
            else if (SecurityCache.hasPermission(user, SecurityCache.SECURITY_PERMISSIONANNOTATE_S, fs.ACL, fs, false))
            {
                permissions.append(Contract.PERM_CHECKIN);
                permissions.append(",").append(Contract.PERM_CHECKOUT);
                permissions.append(",").append(Contract.PERM_UNLOCK);
                permissions.append(",").append(Contract.PERM_ANNOTATE);
            }

            return permissions.toString();
        }
    }

    private static String encode(String str)
    {
        String aa = str;
        if (str == null || str.trim().equals(""))
        {
            aa = " ";
        }
        byte[] array = aa.getBytes(Charset.forName("UTF-8"));
        return DatatypeConverter.printHexBinary(array);
    }

    public static String decode(String str)
    {
        if (str == null)
        {
            return "";
        }
        return new String(DatatypeConverter.parseHexBinary(str), Charset.forName("UTF-8"));
    }
}

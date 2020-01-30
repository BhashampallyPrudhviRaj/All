

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.igc.be.api.client.FileView;
import com.igc.be.api.client.html.HtmlConfig;
import com.igc.be.api.client.html.HtmlSignedConfigResponse;
import com.igc.be.api.client.html.parameters.Application;
import com.igc.be.api.client.rest.SignedConfigError;
import com.igc.be.api.client.ui.TaskBar;
import com.igc.be.api.core.BravaConnection;
import com.igc.be.api.core.Providers;
import com.igc.be.api.core.cache.CacheFile;
import com.igc.be.api.core.cache.UploadableFileCacheQuery;
import com.igc.be.api.core.cache.UploadableFileCacheResponse;
import com.igc.be.api.core.rendition.Composition;
import com.igc.be.api.core.rendition.CompositionResponse;
import com.igc.be.api.core.rendition.FileRendition;
import com.igc.be.api.core.rendition.RenditionRequest;
import com.igc.be.api.core.rendition.RenditionResponse;
import com.igc.be.api.core.rendition.RenditionType;

/**
 * Servlet implementation class sample
 */
@WebServlet("/sample")
public class sample extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private BravaConnection conn = null;
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	    String compositionId;
		try {
			compositionId = request.getParameter("compositionId");								

			HtmlConfig htmlConfig = getBravaConn().createHtmlConfig(compositionId);
			    String clientId = request.getParameter("ClientID");			    
				
			    htmlConfig.set(Application.CLIENT_ID, clientId);
			    htmlConfig.set(Application.USER_NAME, "John Doe");
			    System.out.println(request.getRequestURL());
			    htmlConfig.setVisible(TaskBar.DOWNLOAD_ORIGINAL, false);
			 
			    FileView fileView = Providers.ClientConfig().createFileView();
			    fileView.setTitle("My First Document");
			    htmlConfig.getDocumentConfig(compositionId).addDocumentFragmentView(fileView);		 
			    
			    try {    
			    	  HtmlSignedConfigResponse signedConfigResponse = htmlConfig.getRequest().getResponse();
			    	  response.getWriter().print(signedConfigResponse.getResponseString());
		    	} catch (Exception e) {
			    	  e.printStackTrace();
			    	  SignedConfigError error = Providers.ClientConfig().createSignedConfigError(e.getMessage());
			    	  
		    	}
		} catch (Exception e2) {
					// TODO Auto-generated catch block
			e2.printStackTrace();
		}		
	}

	private BravaConnection getBravaConn() throws Exception {
		if(conn == null) {
			conn =Providers.Connection().create("http://10.96.44.66:8080");
		} 
		return conn; 
	}   
}
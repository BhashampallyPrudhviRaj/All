<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="java.io.File, java.io.FileInputStream, java.io.IOException, java.io.InputStream, javax.servlet.ServletException, javax.servlet.annotation.WebServlet, javax.servlet.http.HttpServlet, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, com.igc.be.api.client.FileView, com.igc.be.api.client.html.HtmlConfig, com.igc.be.api.client.html.HtmlSignedConfigResponse, com.igc.be.api.client.html.parameters.Application, com.igc.be.api.client.rest.SignedConfigError, com.igc.be.api.client.ui.TaskBar, com.igc.be.api.core.BravaConnection, com.igc.be.api.core.Providers, com.igc.be.api.core.cache.CacheFile, com.igc.be.api.core.cache.UploadableFileCacheQuery, com.igc.be.api.core.cache.UploadableFileCacheResponse, com.igc.be.api.core.rendition.Composition, com.igc.be.api.core.rendition.CompositionResponse, com.igc.be.api.core.rendition.FileRendition, com.igc.be.api.core.rendition.RenditionRequest, com.igc.be.api.core.rendition.RenditionResponse, com.igc.be.api.core.rendition.RenditionType" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
<script src="http://10.96.44.66:8080/BravaSDK/html-clients/html-clients.js"></script>
<script src="http://10.96.44.66:8080/BravaSDK/HTML/viewer/viewer.js"></script>
    
<%!
private BravaConnection conn = null;
private String copy() throws Exception {		  
    InputStream initialStream = new FileInputStream(new File("C:\\Program Files (x86)\\OpenText\\Brava! Enterprise\\Tomcat\\webapps\\BravaSDK\\sampleFiles\\Sample W4.pdf"));
    CacheFile cache = cacheUploadableFile("sample1","sample12",initialStream);
    return createRendition(cache);
}
private BravaConnection getBravaConn() throws Exception {
	if(conn == null) {
		conn =Providers.Connection().create("http://10.96.44.66:8080");
	} 
	return conn; 
}

CacheFile cacheUploadableFile(String fileid, String filename, InputStream fileis) throws Exception {
	getBravaConn();
    CacheFile cacheFile = Providers.Cache().createCacheFile(fileid);
    cacheFile.setFilename(filename);
 
    UploadableFileCacheQuery query = conn.createUploadableCacheQuery(cacheFile);
 
    UploadableFileCacheResponse queryresponse = query.getResponse();
    if (!queryresponse.isUploaded())
        queryresponse.getUploader().upload(fileis);
 
    return cacheFile;
}

public String createRendition(CacheFile file) throws Exception {
	RenditionRequest rendition = conn.createRenditionRequest();
    Composition composition = Providers.Rendition().createComposition(RenditionType.HTML);

    FileRendition fileRendition = Providers.Rendition().createFileRendition(file);
    composition.getFragmentSequence().addFileRendition(fileRendition);

    rendition.getCompositions().addComposition(composition);

    RenditionResponse rendresponse = rendition.getResponse();

    CompositionResponse compositionResponse = rendresponse.getCompositionResponse(composition);
    return compositionResponse.getId();

} 
%>
	<script>
	
	    window.onload = function() { 
            brava.htmlClients.create("viewer", {
                path: "http://10.96.44.66:8080/BravaSDK/HTML/viewer",
                container: "htmlviewer",
                clientType: "2D",
                height: "50%",
                width: "75%",
                signedConfigUrl: "http://10.96.44.66:8080/Project/sample?compositionId=<%= (String) copy() %>",                		
                xmlPostType: "text/xml"
            }); 
        };
    </script>
  </head>

  <body>  	
    <div id="htmlviewer"></div>    
  </body>
</html>
package com.eistream.sonora.filestore;

 

import java.io.FileInputStream;
import java.io.InputStream;

 import com.igc.be.api.core.BravaConnection; 
 import com.igc.be.api.core.Providers;
 import com.igc.be.api.core.cache.CacheFile;
 import com.igc.be.api.core.cache.UploadableFileCacheQuery;
 import com.igc.be.api.core.cache.UploadableFileCacheResponse; 
 import com.igc.be.api.core.exception.BravaResponseException; 
 import com.igc.be.api.core.rendition.Composition;
 import com.igc.be.api.core.rendition.CompositionResponse; 
 import com.igc.be.api.core.rendition.FileRendition;
 import com.igc.be.api.core.rendition.RenditionRequest;
 import com.igc.be.api.core.rendition.RenditionResponse; 
 import com.igc.be.api.core.rendition.RenditionType; 
 

 

public class Bravahelper  {
    public static String createId(String location) {
        try{
            InputStream inputstream = new FileInputStream(location);
            
            
            return createRendition(Providers.Connection().create("http://10.96.44.66:8080"),cacheUploadableFile(Providers.Connection().create("http://10.96.44.66:8080"),location,"sample_name",inputstream));
        }
        catch(Exception e)
        {
            //System.out.println("exception "+e);
            e.printStackTrace();
            return e.toString();
        }
        
    }
    static CacheFile cacheUploadableFile(  BravaConnection conn,String fileid, String filename,InputStream fileis) throws BravaResponseException {
           
        CacheFile file = Providers.Cache().createCacheFile(fileid);
        file.setFilename(filename);
     
        UploadableFileCacheQuery query = conn.createUploadableCacheQuery(file);
     
        UploadableFileCacheResponse queryresponse = query.getResponse();
        if (!queryresponse.isUploaded())
            queryresponse.getUploader().upload(fileis);
     
        return file;
    }
public static String createRendition(BravaConnection conn, CacheFile file) throws Exception {
 
RenditionRequest rendition = conn.createRenditionRequest();
Composition composition = Providers.Rendition().createComposition(RenditionType.HTML);

 

FileRendition fileRendition = Providers.Rendition().createFileRendition(file);
composition.getFragmentSequence().addFileRendition(fileRendition);

 

rendition.getCompositions().addComposition(composition);

 

RenditionResponse rendresponse = rendition.getResponse();

 

CompositionResponse compositionResponse = rendresponse.getCompositionResponse(composition);

 

return compositionResponse.getId();
}

 

}
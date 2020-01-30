package com.eistream.sonora.filestore;

import java.util.*;
import java.math.BigDecimal;
import com.eistream.utilities.*;
import com.eistream.sonora.util.*;

/**
 * This class represents a version of the contents of a document.  Except as noted, every member variable in this class
 * represents a column in the SONORAFSVERSIONS table.  The primary key of this table is (DOCUMENTID, VERSIONNUMBER).
 * <p>
 * This class is not used to keep track of the content or versions of DataManager managed documents.
 * The StorageElement class is used for this purpose.
 * @author Copyright © 2001-2004 eiStream Technologies, Inc.
 */
public class FSVersionElement implements java.io.Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The system assigned identifier for the document.
	 */
	public  BigDecimal    			DocumentID;
	/**
	 * The version number of this version element.
	 */
	public  int                     VersionNumber;

	/**
	 * This is the identifier of the user who created this version element.
	 */
	public  String                  CreatedBy;

	/**
	 * The date when this version element was created.
	 */
	public  java.sql.Timestamp      CreatedDateTime;

	/**
	 * The last node of the file name minus the extension.
	 */
	public  String                  FileName;

	/**
	 * A list of extensions for this file.  Currently only one extension is supported.
	 */
	public  String                  Extensions;

	/**
	 * The location in a client machine where the content corresponding to this version element was last stored.
	 */
	public  String                  ClientLocation;

	/**
	 * Currently not used since versions cannot be modified.
	 */
	public  boolean                 IsLocked;

	/**
	 * Currently not used since versions cannot be modified.
	 */
	public  String                  LockedBy;

	/**
	 * The leading part of the directory name where the file is stored.
	 */
	public  String                  PathPrefix;

	/**
	 * The storage element associated with this document and version.
	 */
	private StorageElement          storageElement;


    /**
     * The enhanced document version element (if any) associated with this document and version.     *
     */
	private DocVersionElement       docVersionElement;

    public DocVersionElement getDocVersionElement()
    {
        return docVersionElement;
    }

   public void setDocVersionElement(DocVersionElement docVersionElement)
   {
       this.docVersionElement = docVersionElement;
   }

	/**
	 * Default constructor.
	 */
	public FSVersionElement()
	{
		FileName = null;
		Extensions = null;
		ClientLocation = null;

		// Currently we do not lock versions.
		IsLocked = false;
		LockedBy = null;

		PathPrefix = null;

		storageElement = null;

        docVersionElement = null;
	}

	/**
	 * Create a version element out of the passed values.
	 * @param documentId the document identifier.
	 * @param versionNumber the version number.
	 * @param pathPrefix the path prefix.
	 * @param fileName only the last node of the file name is used (with its extension if any).
	 */
	public FSVersionElement(BigDecimal documentId, int versionNumber, String pathPrefix, String fileName)
	{
		this.DocumentID = documentId;
		this.VersionNumber = versionNumber;
		putFileName(fileName);
		ClientLocation = null;
		IsLocked = false;
		LockedBy = null;
		this.PathPrefix = pathPrefix;
		storageElement = null;
        docVersionElement = null;
	}

	/**
	 * Copy constructor.
	 * @param fsv Source version element.
	 */
	public FSVersionElement(FSVersionElement fsv)
	{
		DocumentID              = fsv.DocumentID;
		VersionNumber           = fsv.VersionNumber;
		CreatedBy               = fsv.CreatedBy;
		CreatedDateTime         = fsv.CreatedDateTime;
		FileName                = fsv.FileName;

		// space separated list of file name extensions.
		Extensions              = fsv.Extensions;
		ClientLocation          = fsv.ClientLocation;
		IsLocked                = fsv.IsLocked;
		LockedBy                = fsv.LockedBy;
		PathPrefix              = fsv.PathPrefix;
		storageElement          = null;
        docVersionElement = null;
	}

	/**
	 * Create a version element out of a result set.
	 * @param rs the result set.
	 *
	 * @exception java.sql.SQLException
	 */
	public FSVersionElement(java.sql.ResultSet rs)
	throws java.sql.SQLException
	{
		DocumentID              = SqlUtil.getBigDecimal(rs,"DocumentID", 0);
		VersionNumber           = rs.getInt("VersionNumber");
		CreatedBy               = rs.getString("CreatedBy");
		CreatedDateTime         = rs.getTimestamp("CreatedDateTime");
		FileName                = rs.getString("FileName");
		Extensions              = rs.getString("Extensions");
		ClientLocation          = rs.getString("ClientLocation");
		IsLocked                = rs.getBoolean("IsLocked");
		LockedBy                = rs.getString("LockedBy");
		PathPrefix              = rs.getString("PathPrefix");
		storageElement          = null;
        docVersionElement       = null;

	}

	/**
	 * Creates a copy of this version element.  Only the element is duplicated and not the underlying content.
	 */
	public FSVersionElement duplicate()
	{
		FSVersionElement versionElement = new FSVersionElement();
		versionElement.DocumentID       =   DocumentID;
		versionElement.VersionNumber    =   VersionNumber;
		versionElement.CreatedBy        =   CreatedBy;
		versionElement.CreatedDateTime  =   CreatedDateTime;
		versionElement.FileName         =   FileName;

		// space separated list of file name extensions.
		versionElement.Extensions       =   Extensions;
		versionElement.ClientLocation   =   ClientLocation;
		versionElement.IsLocked         =   IsLocked;
		versionElement.LockedBy         =   LockedBy;
		versionElement.PathPrefix       =   PathPrefix;
		versionElement.setStorageElement(storageElement);

		return versionElement;

	}

	/**
	 * Create a version element given a storage element.
	 * @param storageElement the source storage element.
	 */
	public FSVersionElement(StorageElement storageElement)
	{
		this.storageElement = storageElement;
		DocumentID = storageElement.getDocumentId();
		VersionNumber = storageElement.getVersionNumber();
		CreatedBy = storageElement.getCreatedBy();
		CreatedDateTime = storageElement.getCreatedDateTime();
		putFileName(storageElement.getFileName());
		ClientLocation = storageElement.getClientLocation();
		IsLocked = false;
		LockedBy = null;
		PathPrefix = storageElement.getLocation();
        docVersionElement = null;
	}


	/**
	 * Returns the mime type of a file given its name.
	 * @param fileName the file name.
	 * @return the mime type.
	 */
	public String getMimeType(String fileName)
	{
		return MimeTypeEx.getMimeType(fileName);
	}

	/**
	 * Returns the first file name in this version.
	 * @return the file name.
	 */
	public String getFileName()
	{

		// If we have a storage element, return its file name (last node).
		if (storageElement != null)
		{
			return storageElement.getFileName();
		}
		// Note that for this cut, we just return the catenation of the fileName and the
		// first extension (if any).
		String extension = null;
		if (Extensions != null)
		{
			StringTokenizer parser = new StringTokenizer(Extensions);
			extension = parser.nextToken();
		}
		if (FileName != null)
		{
			if (extension != null)
			{
				return FileName + "." + extension;
			}
			else
			{
				return FileName;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * Stores the last node of the file name.
	 * @param fullFileName can be a fully qualified file name or just the last node of a file name.
	 */
	public void putFileName(String fullFileName)
	{
		// We'll split the file name into its base name and extension.  We
		// store the base name in the FileName variable and we add the extension
		// to the Extensions variable if it is not already present.
		fullFileName = PathUtil.normalize(fullFileName);

		int fileNameStart = fullFileName.lastIndexOf(PathUtil.FILE_SEPARATOR);
		String justFileName = fullFileName.substring(fileNameStart + 1);
		int extensionStart = justFileName.lastIndexOf('.');
		String extension = null;
		if (extensionStart > 0)
		{
			extension = justFileName.substring(extensionStart + 1);
			FileName = justFileName.substring(0, extensionStart);
		}
		else
		{
			FileName = justFileName;
		}

		// for now.
		Extensions = extension;
	}
	/**
	 * Indicates if this file is going to be handled by the Web Image Editor (WIE) or not.
	 * Currently only files with a 'tiff' or 'tif' extension are handled by the WIE.
	 * @return true if this file is going to be handled by the WIE and false if it is not.
	 */
	public boolean isWIEFile()
	{
		//Removing (Extensions.equalsIgnoreCase("tiff") || Extensions.equalsIgnoreCase("tif") to support any type of extension
		//Changes made on Jan 8, 2020
		//if (FileName != null && Extensions != null &&				
		//		 (storageElement != null && storageElement.getFileName().compareToIgnoreCase(DocumentID.toString() + ".map") == 0))
			
			
		if (FileName != null && Extensions != null &&
			(Extensions.equalsIgnoreCase("tiff") || Extensions.equalsIgnoreCase("tif") ||
			 (storageElement != null && storageElement.getFileName().compareToIgnoreCase(DocumentID.toString() + ".map") == 0)))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Sets the document identifier.  f there is an associated storage element, its document identifier is also set to this value.
	 * @param documentID the document identifier.
	 */
	public void setDocumentID(BigDecimal documentID)
	{
		this.DocumentID = documentID;
		if (storageElement != null)
		{
			storageElement.setDocumentId(documentID);
		}

	}
	/**
	 * Gets the document identifier.
	 * @return the document identifier.
	 */
	public BigDecimal getDocumentID()
	{
		return DocumentID;
	}

	/**
	 * Sets the version number.  If there is an associated storage element, its version number is also set to this value.
	 * @param versionNumber the version number.
	 */
	public void setVersionNumber(int versionNumber)
	{
		this.VersionNumber = versionNumber;
		if (storageElement != null)
		{
			storageElement.setVersionNumber(versionNumber);
		}
	}

	/**
	 * Gets the version number.
	 * @return the version number.
	 */
	public int getVersionNumber()
	{
		return VersionNumber;
	}


	/**
	 * Sets the client location.  This is a directory on a client machine were the content associated with this
	 * version element was last stored.  f there is an associated storage element, its client location is also set to this value.
	 * @param clientLocation the client location.
	 */
	public void setClientLocation(String clientLocation)
	{
		this.ClientLocation = clientLocation;
		if (storageElement != null)
		{
			storageElement.setClientLocation(clientLocation);
		}
	}

	/**
	 * Gets the client location
	 * @return the client location.
	 */
	public String getClientLocation()
	{
		return ClientLocation;
	}

	/**
	 * Sets the FileName member variable.
	 * @param fileName the last node of the file name (minus the extension).
	 */
	public void setFileNameField(String fileName)
	{
		this.FileName = fileName;
	}

	/**
	 * Gets the last node of the file name minus its extension.
	 * @return the last node of the file name minus its extension.
	 */
	public String getFileNameField()
	{
		return FileName;
	}

	/**
	 * Sets the extsions associated with this element.  This is a comma separated list.
	 * Currently, only one extension is supported.
	 * @param Extensions the extensions associated with this element.
	 */
	public void setExtensions(String Extensions)
	{
		this.Extensions = Extensions;
	}

	/**
	 * Gets the extsions associated with this element.
	 * Currently, only one extension is supported.
	 * @return the extensions associated with this element.
	 */
	public String  getExtensions()
	{
		return Extensions;
	}

	/**
	 * Sets the leading part of the directory where the file is stored.
	 * @param PathPrefix the leading part of the directory where the file is stored.
	 */
	public void setPathPrefix(String PathPrefix)
	{
		this.PathPrefix = PathPrefix;
	}
	/**
	 * Gets the leading part of the directory where the file is stored.
	 * @return the leading part of the directory where the file is stored.
	 */
	public String  getPathPrefix()
	{
		return PathPrefix;
	}

	/**
	 * Gets the associated storage element.  Note that if there is one present, this indicates the document
	 * resides in a storage pool.
	 * @return the associated storage element or null if there is no associated element.
	 */
	public StorageElement getStorageElement()
	{
		return storageElement;
	}

	/**
	 * Sets the associated storage element.
	 * @param storageElement the associated storage element.
	 */
	public void setStorageElement(StorageElement storageElement)
	{
		this.storageElement = storageElement;
		DocumentID = storageElement.getDocumentId();
		VersionNumber = storageElement.getVersionNumber();
		CreatedBy = storageElement.getCreatedBy();
		CreatedDateTime = storageElement.getCreatedDateTime();
		putFileName(storageElement.getFileName());
		ClientLocation = storageElement.getClientLocation();
		IsLocked = false;
		LockedBy = null;
		PathPrefix = "/";
	}

	/**
	 * Gets the transfer object matching this version element.
	 * @return the version transfer object.
	 */
    public VersionTO getVersionTO()
    {
        VersionTO versionTO = new VersionTO();
        versionTO.setClientLocation(ClientLocation);
        versionTO.setCreatedBy(CreatedBy);
        versionTO.setCreatedDateTime(CalTimestampUtil.TimestampToCalendar(CreatedDateTime));
        versionTO.setDocumentID(DocumentID);
        versionTO.setExtensions(Extensions);
        versionTO.setFileName(FileName);
        versionTO.setLocked(IsLocked);
        versionTO.setLockedBy(LockedBy);
        versionTO.setPathPrefix(PathPrefix);
        versionTO.setVersionNumber(VersionNumber);

        return versionTO;
    }

    public FSVersionElementTO getFSVersionElementTO()
    {
        FSVersionElementTO fsVersionTO = new FSVersionElementTO();
        fsVersionTO.setClientLocation(ClientLocation);
        fsVersionTO.setDocumentID(DocumentID);
        fsVersionTO.setExtensions(Extensions);
        fsVersionTO.setFileName(getFileNameField());
        fsVersionTO.setPathPrefix(PathPrefix);
        fsVersionTO.setVersionNumber(VersionNumber);
        fsVersionTO.setCreatedBy(CreatedBy);
        fsVersionTO.setCreatedDateTime(CalTimestampUtil.TimestampToCalendar(CreatedDateTime));
        fsVersionTO.setLocked(IsLocked);
        fsVersionTO.setLockedBy(LockedBy);

        if (getStorageElement() != null)
        {
            StorageElementTO storageTO = new StorageElementTO();
            storageTO.setClientLocation(getStorageElement().getClientLocation());
            storageTO.setCreatedBy(getStorageElement().getCreatedBy());
            storageTO.setCreatedDateTime(CalTimestampUtil.TimestampToCalendar(getStorageElement().getCreatedDateTime()));
            storageTO.setDocumentId(getStorageElement().getDocumentId());
            storageTO.setDuplicateFile(getStorageElement().getIsDuplicateFile());
            storageTO.setFileName(getStorageElement().getFileName());
            storageTO.setImageComponent(getStorageElement().isImageComponent());
            storageTO.setInProgress(getStorageElement().getInProgress());
            storageTO.setItemSize(getStorageElement().getItemSize());
            storageTO.setLocation(getStorageElement().getLocation());
            storageTO.setMapElement(getStorageElement().isMapElement());
            storageTO.setMirrorFamilyId(getStorageElement().getMirrorFamilyId());
            storageTO.setPoolId(getStorageElement().getPoolId());
            storageTO.setRendition(getStorageElement().isRendition());
            storageTO.setRenditionLevel(getStorageElement().getRenditionLevel());
            storageTO.setSignaturePoolId(getStorageElement().getSignaturePoolId());
            storageTO.setStorageId(getStorageElement().getStorageId());
            storageTO.setTemporaryFileIndication(getStorageElement().getTemporaryFileIndication());
            storageTO.setVersionNumber(getStorageElement().getVersionNumber());
            storageTO.setWIEFile(getStorageElement().isWIEFile());
            storageTO.setWorkFileName(getStorageElement().getWorkFileName());
            fsVersionTO.setStorageElement(storageTO);
        }
        else
            fsVersionTO.setStorageElement(null);

        if (getDocVersionElement() != null)
        {
            DocVersionElementTO docVersion = new DocVersionElementTO();
            docVersion.setCheckedInBy(getDocVersionElement().getCheckedInBy());
            docVersion.setCheckedInDate(CalTimestampUtil.TimestampToCalendar(getDocVersionElement().getCheckedInDate()));
            docVersion.setCheckedOutBy(getDocVersionElement().getCheckedOutBy());
            docVersion.setCheckInComment(getDocVersionElement().getCheckInComment());
            docVersion.setCheckoutComment(getDocVersionElement().getCheckoutComment());
            docVersion.setCheckoutDate(CalTimestampUtil.TimestampToCalendar(getDocVersionElement().getCheckoutDate()));
            docVersion.setChildVersionNumber(getDocVersionElement().getChildVersionNumber());
            docVersion.setDocumentId(getDocVersionElement().getDocumentId());
            docVersion.setFlags(getDocVersionElement().getFlags());
            docVersion.setInProgress(getDocVersionElement().isInProgress());
            docVersion.setLocked(getDocVersionElement().isLocked());
            docVersion.setPublished(getDocVersionElement().isPublished());
            docVersion.setRecord(getDocVersionElement().isRecord());
            docVersion.setVersionLabel(getDocVersionElement().getVersionLabel());
            docVersion.setVersionNumber(getDocVersionElement().getVersionNumber());
            fsVersionTO.setDocVersionElement(docVersion);

        }
        else
            fsVersionTO.setDocVersionElement(null);

        return fsVersionTO;

    }

	/**
	 * @return Returns the createdBy.
	 */
	public String getCreatedBy()
	{
		return CreatedBy;
	}

	/**
	 * @param createdBy The createdBy to set.
	 */
	public void setCreatedBy(String createdBy)
	{
		CreatedBy = createdBy;
	}

	/**
	 * @return Returns the createdDateTime.
	 */
	public java.sql.Timestamp getCreatedDateTime()
	{
		return CreatedDateTime;
	}

	/**
	 * @param createdDateTime The createdDateTime to set.
	 */
	public void setCreatedDateTime(java.sql.Timestamp createdDateTime)
	{
		CreatedDateTime = createdDateTime;
	}

	/**
	 * @return Returns the isLocked.
	 */
	public boolean isLocked()
	{
		return IsLocked;
	}

	/**
	 * @param isLocked The isLocked to set.
	 */
	public void setLocked(boolean isLocked)
	{
		IsLocked = isLocked;
	}

	/**
	 * @return Returns the lockedBy.
	 */
	public String getLockedBy()
	{
		return LockedBy;
	}

	/**
	 * @param lockedBy The lockedBy to set.
	 */
	public void setLockedBy(String lockedBy)
	{
		LockedBy = lockedBy;
	}

	/**
	 * @param fileName The fileName to set.
	 */
	public void setFileName(String fileName)
	{
		FileName = fileName;
	}
}

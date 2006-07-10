/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.tool.podcasts;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.fileupload.FileItem;
import org.sakaiproject.api.app.podcasts.PodcastService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;


public class podHomeBean {
	
	/**
	 * Stores the properties of a specific podcast to be displayed
	 * on the main page.
	 * 
	 * @author josephrodriguez
	 *
	 */
	public class DecoratedPodcastBean {
		private String resourceId;
		private String filename;
		private long fileSize;
		private String displayDate;
		private String title;
		private String description;
		private String size;
		private String type;
		private String postedTime;
		private String postedDate;
		private String author;
		
		public DecoratedPodcastBean() {
			
		}
		
		public DecoratedPodcastBean(String resourceId, String filename, String displayDate, String title, String description, String size, String type) {
			this.resourceId = resourceId;
			this.filename = filename;
			this.displayDate = displayDate;
			this.title = title;
			this.description = description;
			this.size = size;
			this.type = type;
		}
		
		public String getDescription() {
			return description;
		}
		public void setDescription(String decsription) {
			this.description = decsription;
		}
		public String getDisplayDate() {
			return displayDate;
		}

		public void setDisplayDate(String displayDate) {
			this.displayDate = displayDate;
		}
		public String getFilename() {
			return filename;
		}
		public void setFilename(String filename) {
			this.filename = filename;
		}
		public String getSize() {
			return size;
		}
		public void setSize(String size) {
			this.size = size;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}

		public String getPostedTime() {
			return postedTime;
		}

		public void setPostedTime(String postedTime) {
			this.postedTime = postedTime;
		}

		public String getPostedDate() {
			return postedDate;
		}

		public void setPostedDate(String postedDate) {
			this.postedDate = postedDate;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getResourceId() {
			return resourceId;
		}

		public void setResourceId(String resourceId) {
			this.resourceId = resourceId;
		}

		public long getFileSize() {
			return fileSize;
		}

		public void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}

	}

	// podHomeBean constants
	private static final String DOT = ".";
	private static final String NO_RESOURCES_ERR_MSG = "To use the Podcasts tool, you must first add the Resources tool.";
	private static final String RESOURCEID = "resourceId";
	
	// podHomeBean member variables
	private boolean resourceToolExists;
	private boolean podcastFolderExists;
	private boolean actPodcastsExist;
	private boolean podcastResourceCheckFirstTry;
	private PodcastService podcastService;
	private List contents;
	private String URL;
	private DecoratedPodcastBean selectedPodcast;

	// used by podAdd.jsp for adding a podcast
	private String filename;
	private Date date;
	private String title;
	private String description;
	private String email;
	private long fileSize;
    BufferedInputStream fileAsStream;

	private SelectItem [] emailItems = {
		new SelectItem("none", "None - No notification"),
		new SelectItem("low", "Low - Only participants who have opted in"),
		new SelectItem("high", "High - All participants")
	};
	
	public podHomeBean() {
		resourceToolExists=false;
		podcastFolderExists = false;
		actPodcastsExist = false;
		podcastResourceCheckFirstTry=true;
	}

	/**
     *   Determines if Resource tool part of the site. Needed to store podcasts.
     *   Since multiple ui items need to be removed, set boolean variable so 
     *   only need to check actual resource once 
     *  
     * @return true if Resource tool exists so entire page can display
     *         false if does not exist so just error message displays
     */
	public boolean getResourceToolExists() {
		if (podcastResourceCheckFirstTry) {
			podcastResourceCheckFirstTry=false;

			try
			{
				Site thisSite = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
				List pageList = thisSite.getPages();
				Iterator iterator = pageList.iterator();

				while(iterator.hasNext())
				{
					SitePage pgelement = (SitePage) iterator.next();

					if (pgelement.getTitle().equals("Resources"))
					{
						resourceToolExists = true;
						break;
					}
				}
			}
			catch(Exception e)
			{
				return resourceToolExists;
			}
	    
			if(!resourceToolExists)
			{
				setErrorMessage(NO_RESOURCES_ERR_MSG);
			}
		}
		
	    return resourceToolExists;
	}

	public void setResourseToolExists(boolean resourceToolExists) {
		this.resourceToolExists = resourceToolExists;
	}
	
	private void setErrorMessage(String errorMsg)
	{
		FacesContext.getCurrentInstance().addMessage(null,
		      new FacesMessage("Alert: " + errorMsg));
	}
	  
	public boolean getPodcastFolderExists() {
		podcastFolderExists=false;
		  
		  if (resourceToolExists) {
			  // we know resources tool exists, but need to know if podcast folder does
			  podcastFolderExists = podcastService.checkPodcastFolder();
		  }
		  
		  return podcastFolderExists;
	  }
	  
	  public void setPodcastFolderExists(boolean podcastFolderExists) {
		  this.podcastFolderExists = podcastFolderExists;
	  }
	  
	  public String getURL() {
		  URL = ServerConfigurationService.getServerUrl() + Entity.SEPARATOR + "podcasts/site/" 
		         + podcastService.getSiteId();
		  return URL;
	  }
	  
	  public void setURL(String URL) {
		  this.URL = URL;
	  }

	public PodcastService getPodcastService() {
		return podcastService;
	}

	public void setPodcastService(PodcastService podcastService) {
		this.podcastService = podcastService;
	}
	
	public DecoratedPodcastBean getAPodcast(ResourceProperties podcastProperties, String resourceId)
		throws EntityPropertyNotDefinedException, EntityPropertyTypeException {
		DecoratedPodcastBean podcastInfo = new DecoratedPodcastBean();
		
		// fill up the decorated bean
		
		// store resourceId
		podcastInfo.setResourceId(resourceId);
		
		// store Title and Description
		podcastInfo.setTitle(podcastProperties.getPropertyFormatted(ResourceProperties.PROP_DISPLAY_NAME));
		podcastInfo.setDescription(podcastProperties.getPropertyFormatted(ResourceProperties.PROP_DESCRIPTION));

		// store Display date
		// to format the date as: DAY_OF_WEEK  DAY MONTH_NAME YEAR
		SimpleDateFormat formatter = new SimpleDateFormat ("EEEEEE',' dd MMMMM yyyy" );
		Date tempDate = new Date(podcastProperties.getProperty(PodcastService.DISPLAY_DATE));
		podcastInfo.setDisplayDate(formatter.format(tempDate));
							
		// store actual filename (for revision/deletion purposes?)
		// put in local variable to use to get MIME type
		String filename = podcastProperties.getProperty(ResourceProperties.PROP_ORIGINAL_FILENAME);
		podcastInfo.setFilename(filename);

		// store actual and formatted file size
		// determine whether to display filesize as bytes or MB
		long size = Long.parseLong(podcastProperties.getProperty(ResourceProperties.PROP_CONTENT_LENGTH));
		podcastInfo.setFileSize(size);
		
		double sizeMB = size / (1024.0*1024.0); 
		DecimalFormat df = new DecimalFormat("#.#");
		String sizeString;
		if ( sizeMB >  0.3) {
			sizeString = df.format(sizeMB) + "MB";
		}
		else {
			df.applyPattern("#,###");
			sizeString = "" + df.format(size) + " bytes";
		}
		podcastInfo.setSize(sizeString);

		// TODO: figure out how to determine/store content type
		// this version, just capitalize extension
		// ie, MP3, AVI, etc
		if (filename.indexOf(DOT) == -1)
			podcastInfo.setType("unknown");
		else {
			String extension = filename.substring(filename.indexOf(DOT) + 1);
			podcastInfo.setType( extension.toUpperCase() );
		}

		// get and format last modified time
		formatter.applyPattern("hh:mm a z" );
		tempDate = new Date(podcastProperties.getTimeProperty(ResourceProperties.PROP_MODIFIED_DATE).getTime());
		podcastInfo.setPostedTime(formatter.format(tempDate));

		// get and format last modified date
		formatter.applyPattern("MM/dd/yyyy" );
		tempDate = new Date(podcastProperties.getTimeProperty(ResourceProperties.PROP_MODIFIED_DATE).getTime());
		podcastInfo.setPostedDate(formatter.format(tempDate));

		// get author
		podcastInfo.setAuthor(podcastProperties.getPropertyFormatted(ResourceProperties.PROP_CREATOR));
		
		return podcastInfo;
	}

	public List getContents() {
		contents = podcastService.getPodcasts();

		// create local List of DecoratedBeans
		ArrayList decoratedPodcasts = new ArrayList();

		if (contents != null) {
			Iterator podcastIter = contents.iterator();
		
			// for each bean
			while (podcastIter.hasNext() ) {
				try {
					// get its properties from ContentHosting
					ContentResource podcastResource = (ContentResource) podcastIter.next();
					ResourceProperties podcastProperties = podcastResource.getProperties();

					// Create a new decorated bean to store the info
					DecoratedPodcastBean podcastInfo = getAPodcast(podcastProperties, podcastResource.getId());

					// add it to the List to send to the page
					decoratedPodcasts.add(podcastInfo);
			
					// get the next podcast if it exists
				}
				catch (EntityPropertyNotDefinedException ende) {
					
				}
				catch (EntityPropertyTypeException epte) {
					
				}

			}
		
		}

		// when done:
		// TODO: sort the list
		return decoratedPodcasts; //new decorated list 
	}

	public void setContents(List contents) {
		this.contents = contents;
	}

	/**
	 * Resources/podcasts exists, but are there any actual podcasts
	 * 
	 * @return true if there are podcasts, false otherwise
	 */
	public boolean getActPodcastsExist() {
		if (!getPodcastFolderExists()) {
			// if for some reason there is not a podcast folder
			// for example, was renamed in Resources
			actPodcastsExist = false;
		}
		else  {
			// ask the service if there is anything in the podcast folder
			actPodcastsExist = podcastService.checkForActualPodcasts();
		}

		return actPodcastsExist;
	}

	public void setActPodcastsExist(boolean actPodcastsExist) {
		this.actPodcastsExist = actPodcastsExist;
	}
	
	public void podMainListener(ActionEvent e) {
	    FacesContext context = FacesContext.getCurrentInstance();
	    Map requestParams = context.getExternalContext().getRequestParameterMap();
	    String resourceId = (String) requestParams.get(RESOURCEID);

	    setPodcastSelected(resourceId);
	}
	
	public void setPodcastSelected(String resourceId) {
		Iterator podcastIter = contents.iterator();
		
		// for each bean
		while (podcastIter.hasNext() ) {
			try {

				// get its properties from ContentHosting
				ContentResource podcastResource = (ContentResource) podcastIter.next();

				if (podcastResource.getId().equals(resourceId))
				{
					selectedPodcast = getAPodcast(podcastResource.getProperties(), podcastResource.getId());
					break;
				}

			} catch (EntityPropertyNotDefinedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EntityPropertyTypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	public DecoratedPodcastBean getSelectedPodcast() {
		return selectedPodcast;
	}

	public void setSelectedPodcast(DecoratedPodcastBean selectedPodcast) {
		this.selectedPodcast = selectedPodcast;
	}

	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
	    this.filename = filename;
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
	    this.date = date;
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public SelectItem [] getEmailItems() {
		return emailItems;
	}
	
	public String getemail() {
		return email;
	}
	
	public void setemail(String email) {
		this.email = email;
	}
	
	public void processFileUpload (ValueChangeEvent event)
            throws AbortProcessingException
    {
	   UIComponent component = event.getComponent();

	    Object newValue = event.getNewValue();
        Object oldValue = event.getOldValue();
        PhaseId phaseId = event.getPhaseId();
        Object source = event.getSource();
        System.out.println("processFileUpload() event: " + event + " component: "
                + component + " newValue: " + newValue + " oldValue: " + oldValue
                + " phaseId: " + phaseId + " source: " + source);

        if (newValue instanceof String) return;
        if (newValue == null) return;

        // must be a FileItem
        try
        {
            FileItem item = (FileItem) event.getNewValue();
	        String fieldName = item.getFieldName();
	        filename = item.getName();
	        // TODO: 1. save this as a property?
	        //       2. also save the type of file?
	        fileSize = item.getSize();
	        System.out.println("processFileUpload(): item: " + item + " fieldname: " + fieldName + " filename: " + filename + " length: " + fileSize);

	        // Read the file as a stream (may be more memory-efficient)
	        fileAsStream = new BufferedInputStream(item.getInputStream());

	        // Read the contents as a byte array
	        // Just need to upload in preparation for depositing into Resources
	        //fileContents = item.get();

        }
        catch (Exception ex)
        {
            // handle exception
            System.out.println("Houston, we have a problem.");
            ex.printStackTrace();
        }
    }
	
	/**
	 * This attempts to add a podcast
	 */
	public String processAdd() {
		byte[] fileContents = new byte[(int) fileSize];
		
		try {
			fileAsStream.read(fileContents);
		}
		catch (IOException ioe) {
			System.out.println("What happened to the fileStream?");
		}
		
		podcastService.addPodcast(title, date, description, fileContents, filename);

		title="";
		date = null;
		description="";
		fileAsStream = null;
		filename="";
		return "cancel";
	}
	
	public String processCancelAdd() {
		date = null;
		title="";
		description="";
		fileAsStream = null;
		filename="";
		return "cancel";
	}
	
	public String processRevisePodcast() {
		String displayDate;
		byte[] fileContents = null;
		// TODO: validate here?
		
		// If file has changed, change it in the resource
		if (filename != null) {
			if (! filename.equals("") ) {
				selectedPodcast.filename = filename;

				if (fileAsStream != null) {
					fileContents = new byte[(int) fileSize];

				}
				else {
					fileContents = new byte[(int) selectedPodcast.fileSize];
				}
			
				try {
					fileAsStream.read(fileContents);
				}
				catch (IOException ioe) {
					System.out.println("What happened to the fileStream?");
				}
			
			}
		}
		
		if ( date == null ) {
			displayDate = selectedPodcast.displayDate;
		}
		else {
			displayDate = date.toString();
		}
			
			podcastService.revisePodcast(selectedPodcast.resourceId, selectedPodcast.title, displayDate,
				selectedPodcast.description, fileContents, selectedPodcast.filename);
		
		date = null;
		title="";
		description="";
		fileAsStream = null;
		filename="";
		return "cancel";
	}
	
	public String processCancelRevise() {
		selectedPodcast = null;
		return "cancel";
	}
    
	public String processDeletePodcast() {
		try {
			podcastService.removePodcast(selectedPodcast.getResourceId());
			return "cancel";
		}
		catch (PermissionException e) {
			setErrorMessage("You do not have permission to delete the selected file.");
		}
		catch (Exception e) {
			setErrorMessage("An internal error prevented the podcast from being deleted. Please try again.");
		}
		return "";
	}
	
	public String processCancelDelete() {
		selectedPodcast = null;
		return "cancel";
	}
}

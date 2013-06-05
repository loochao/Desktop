package org.docear.plugin.services.features.user;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.event.DocearEvent;
import org.docear.plugin.core.event.IDocearEventListener;
import org.docear.plugin.services.ADocearServiceFeature;
import org.docear.plugin.services.DocearServiceException;
import org.docear.plugin.services.ServiceController;
import org.docear.plugin.services.features.io.DocearConnectionProvider;
import org.docear.plugin.services.features.user.view.WorkspaceDocearServiceConnectionBar;
import org.docear.plugin.services.features.user.view.WorkspaceDocearServiceConnectionBar.CONNECTION_STATE;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.user.IUserAccountChangeListener;
import org.freeplane.core.user.UserAccountChangeEvent;
import org.freeplane.core.user.UserAccountController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.components.TreeView;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class DocearUserController extends ADocearServiceFeature {
	
	public static DocearUser LOCAL_USER = new DocearLocalUser();
	private PropertyChangeListener userPropertyListener;
	private AccountRegisterer registerer = new AccountRegisterer();

	
	public final static String DOCEAR_CONNECTION_USERNAME_PROPERTY = "docear.service.connect.username";
	public final static String DOCEAR_CONNECTION_TOKEN_PROPERTY = "docear.service.connect.token";
	
	private final WorkspaceDocearServiceConnectionBar connectionBar = new WorkspaceDocearServiceConnectionBar();

	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/
	public DocearUserController() {
		initListeners();
		String name = ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_USERNAME_PROPERTY);
		DocearUser user = loadUser(name);
		user.activate();
	}
	
	public boolean createUserAccount(DocearUser user) throws DocearServiceException {
		if(user != null) {
			registerer.createRegisteredUser(user.getUsername(), user.getPassword(), user.getEmail(), user.isNewsletterEnabled());
			return true;
		}
		return false;
	}
	

	public void loginUser(DocearUser user) throws DocearServiceException {
		if(user == null || LOCAL_USER.equals(user)) {
			return;
		}
		if(user.isValid()) {
			user.setOnline(true);
			return;
		}
		MultivaluedMap<String, String> formParams = new MultivaluedMapImpl();
		formParams.add("password", user.getPassword());
		Status status = null;
		
		WebResource webRes = ServiceController.getConnectionController().getServiceResource().path("/authenticate/" + user.getName());
		ClientResponse response;
		try {
			response = ServiceController.getConnectionController().post(webRes, formParams);
		}
		catch (Exception e) {
			throw new DocearServiceException(e.getMessage());
		}
		
		try {
			status = response.getClientResponseStatus();
		
			if (Status.OK.equals(status)) {
				String token = response.getHeaders().getFirst("accessToken");
				DocearConnectionProvider.readResponseContent(response.getEntityInputStream());
				user.setEnabled(true);
				user.setAccessToken(token);
				user.setOnline(true);
			}
			else {
				throw new DocearServiceException(DocearConnectionProvider.getErrorMessageString(response));
			}
		} catch (IOException e) {
			throw new DocearServiceException(e.getMessage());
		} 
		finally {
			response.close();
		}
		
	}

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	private void adjustInfoBarConnectionState(DocearUser user) {
		if (user.getAccessToken() != null && user.getAccessToken().trim().length() > 0) {
			connectionBar.setUsername(user.getName());
			connectionBar.setEnabled(true);
			if (user.isTransmissionEnabled()) {
				connectionBar.setConnectionState(CONNECTION_STATE.CONNECTED);
			}
			else {
				connectionBar.setConnectionState(CONNECTION_STATE.DISABLED);
			}
		}
		else {
			connectionBar.setUsername("");
			connectionBar.setConnectionState(CONNECTION_STATE.NO_CREDENTIALS);
			connectionBar.setEnabled(false);
		}
	}
	
	public static DocearUser getActiveUser() {
		if(UserAccountController.getController().getActiveUser() instanceof DocearUser) {
			return (DocearUser) UserAccountController.getController().getActiveUser();
		}
		else {
			if(UserAccountController.getController().getActiveUser() == null) {
				return LOCAL_USER;
			}
			return new DocearUser(UserAccountController.getController().getActiveUser());
		}
	}
	
	private void initListeners() {
		DocearController.getController().addDocearEventListener(new IDocearEventListener() {

			public void handleEvent(DocearEvent event) {
				if (event.getSource().equals(connectionBar) && WorkspaceDocearServiceConnectionBar.ACTION_COMMAND_TOGGLE_CONNECTION_STATE.equals(event.getEventObject())) {
					DocearUser user = getActiveUser();
					user.toggleTransmissionEnabled();
				}
				else if(WorkspaceDocearServiceConnectionBar.CONNECTION_BAR_CLICKED.equals(event.getSource()) ) {
//					DocearAllowUploadChooserAction.showDialog(false);
					LogUtils.info("CALL SETTINGS WIZARD");
				}
			}
		});
		
		UserAccountController.getController().addUserAccountChangeListener(new IUserAccountChangeListener() {
			
			public void activated(UserAccountChangeEvent event) {
				if(event.getUser() instanceof DocearUser) {
					event.getUser().addPropertyChangeListener(getUserPropertyChangeListener());
					try {
						loginUser((DocearUser) event.getUser());
					} catch (DocearServiceException e) {
						e.printStackTrace();
					}
				}
				else {
					getActiveUser().activate();
				}
			}
			
			public void aboutToDeactivate(UserAccountChangeEvent event) {
				if(event.getUser() == null) {
					return;
				}
				if(event.getUser() instanceof DocearUser) {
					//DOCEAR - ToDo: clear up all user account links
					((DocearUser)event.getUser()).setOnline(false);
				}
				event.getUser().removePropertyChangeListener(getUserPropertyChangeListener());
			}
		});
	}
	
	private PropertyChangeListener getUserPropertyChangeListener() {
		if(userPropertyListener == null) {
			userPropertyListener = new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					DocearUser user = getActiveUser();
					if (DocearUser.USERNAME_PROPERTY.equals(evt.getPropertyName())) {
						connectionBar.setUsername(String.valueOf(evt.getNewValue()));
					}
					else if (DocearUser.TRANSMISSION_PROPERTY.equals(evt.getPropertyName())) {
						connectionBar.allowTransmission(user.isTransmissionEnabled());
					}
					adjustInfoBarConnectionState(user);
				}
			};
		}
		return userPropertyListener;
	}

	public void installView(ModeController modeController) {
		setupView(modeController);
	}
	
	private void setupView(ModeController modeController) {
		TreeView view = ((TreeView)WorkspaceController.getModeExtension(modeController).getView());
		if(view != null) {
			view.addToolBar(connectionBar, TreeView.BOTTOM_TOOLBAR_STACK);
		}
	}

	public DocearUser loadUser(String name) {
		if(name == null) {
			return LOCAL_USER;
		}
		else {
			DocearUser user = new DocearUser();
			user.setUsername(name);
			user.setEnabled(true);
			String token = ResourceController.getResourceController().getProperty(DOCEAR_CONNECTION_TOKEN_PROPERTY);
			user.setAccessToken(token);
			return user;
		}
	}


	@Override
	protected void installDefaults(ModeController modeController) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}
	
	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	
}
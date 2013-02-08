package org.freeplane.plugin.workspace.nodes;

import java.awt.Component;
import java.net.URI;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.actions.NodeRefreshAction;
import org.freeplane.plugin.workspace.actions.NodeRemoveAction;
import org.freeplane.plugin.workspace.actions.NodeRenameAction;
import org.freeplane.plugin.workspace.actions.PhysicalFolderSortOrderAction;
import org.freeplane.plugin.workspace.actions.WorkspaceNewProjectAction;
import org.freeplane.plugin.workspace.components.menu.WorkspacePopupMenu;
import org.freeplane.plugin.workspace.components.menu.WorkspacePopupMenuBuilder;
import org.freeplane.plugin.workspace.event.IWorkspaceNodeActionListener;
import org.freeplane.plugin.workspace.event.WorkspaceActionEvent;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

public final class WorkspaceRoot extends AFolderNode implements IWorkspaceNodeActionListener {

	private static final long serialVersionUID = 1L;
	private static Icon DEFAULT_ICON = new ImageIcon(
			FolderLinkNode.class.getResource("/images/16x16/preferences-desktop-filetype-association.png"));
	private static WorkspacePopupMenu popupMenu;

	public WorkspaceRoot() {
		super(null);
	}

	public final String getTagName() {
		return null;
	}

	public void handleAction(WorkspaceActionEvent event) {
		if (event.getType() == WorkspaceActionEvent.MOUSE_RIGHT_CLICK) {
			showPopup((Component) event.getBaggage(), event.getX(), event.getY());
		}
	}
	
	public boolean isSystem() {
		return true;
	}

	public boolean setIcons(DefaultTreeCellRenderer renderer) {
		renderer.setOpenIcon(DEFAULT_ICON);
		renderer.setClosedIcon(DEFAULT_ICON);
		renderer.setLeafIcon(DEFAULT_ICON);
		return true;
	}

	public void initializePopup() {
		if (popupMenu == null) {
			Controller controller = Controller.getCurrentController();
//			controller.addAction(new WorkspaceExpandAction());
//			controller.addAction(new WorkspaceCollapseAction());
			controller.addAction(new WorkspaceNewProjectAction());
//			controller.addAction(new WorkspaceHideAction());
//			controller.addAction(new NodeNewFolderAction());
//			controller.addAction(new NodeNewLinkAction());
//			controller.addAction(new NodeEnableMonitoringAction());
//			controller.addAction(new NodeOpenLocationAction());
			
			//FIXME: #332
//			controller.addAction(new NodeCutAction());
//			controller.addAction(new NodeCopyAction());
//			controller.addAction(new NodePasteAction());
			controller.addAction(new NodeRenameAction());
			controller.addAction(new NodeRemoveAction());
			controller.addAction(new NodeRefreshAction());
//			
//			controller.addAction(new FileNodeNewMindmapAction());
//			controller.addAction(new FileNodeNewFileAction());
//			controller.addAction(new FileNodeDeleteAction());
			
			controller.addAction(new PhysicalFolderSortOrderAction());
			
			popupMenu = new WorkspacePopupMenu();
			WorkspacePopupMenuBuilder.addActions(popupMenu, new String[] {
					WorkspacePopupMenuBuilder.createSubMenu(TextUtils.getRawText("workspace.action.new.label")),
					WorkspaceNewProjectAction.KEY,
//					"workspace.action.node.new.folder",
//					"workspace.action.node.new.link",
					WorkspacePopupMenuBuilder.endSubMenu(),
					WorkspacePopupMenuBuilder.SEPARATOR,
//					"workspace.action.location.change",
//					"workspace.action.node.open.location",
//					//"workspace.action.hide",
//					WorkspacePopupMenuBuilder.SEPARATOR,
//					"workspace.action.node.cut",
//					"workspace.action.node.copy",						
//					"workspace.action.node.paste",
//					WorkspacePopupMenuBuilder.SEPARATOR, 
					"workspace.action.all.expand",
					"workspace.action.all.collapse",					 
					WorkspacePopupMenuBuilder.SEPARATOR,
					NodeRefreshAction.KEY					
			});
		}
	}

	protected AWorkspaceTreeNode clone(WorkspaceRoot node) {
		return super.clone(node);
	}

	public AWorkspaceTreeNode clone() {
		WorkspaceRoot node = new WorkspaceRoot();
		return clone(node);
	}

	public WorkspacePopupMenu getContextMenu() {
		if (popupMenu == null) {
			initializePopup();
		}
		return popupMenu;
	}
	
	public String getName() {
		return "default's workspace"; 
	}
	
	public void refresh() {
		getModel().reload(this);
	}
	
	public URI getPath() {
		// not needed for workspace root
		return null;
	}
	
	public boolean isLeaf() {
		return false;
	}

	public AWorkspaceTreeNode getChildAt(int childIndex) {
		return (AWorkspaceTreeNode) WorkspaceController.getCurrentModel().getChild(this, childIndex);
	}

	public int getChildCount() {		
		return WorkspaceController.getCurrentModel().getProjects().size();
	}

	public int getIndex(TreeNode node) {
		return WorkspaceController.getCurrentModel().getIndexOfChild(this, node);
	}

	public boolean getAllowsChildren() {
		return true;
	}

	public Enumeration<AWorkspaceTreeNode> children() {
		return new Enumeration<AWorkspaceTreeNode>() {
		    int count = 0;
		    
		    public boolean hasMoreElements() {
		    	return count < getChildCount();
		    }

		    public AWorkspaceTreeNode nextElement() {
				if (count < getChildCount()) {
				  	return getChildAt(count++);
				}				
				throw new NoSuchElementException("WorkspaceRoot Enumeration");
		    }
		};
	}
	
	
}

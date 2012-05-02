package ca.elmonline.util.swing;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import ca.elmonline.util.HashKeyGenerator;

public class TreeExpansionMgr implements TreeExpansionListener
{
    private JTree tree;
    private HashKeyGenerator keygen;
    private Collection<Object> expanded;

    private static HashKeyGenerator defaultKeyGenerator = new DefaultHashKeyGenerator();
    
    public TreeExpansionMgr(JTree tree)
    {
        this( tree, defaultKeyGenerator );
    }
    
    public TreeExpansionMgr(JTree tree, HashKeyGenerator keygen)
    {
        this.tree = tree;
        this.keygen = keygen;
        
        expanded = new HashSet<Object>();
        
        tree.addTreeExpansionListener( this );
    }
    
    public void storeExpansion()
    {
        Object root = tree.getModel().getRoot();
        if ( root == null )
            return;
        else
            storeExpansion( new TreePath(root) );
    }
    
    private void storeExpansion(TreePath parentPath)
    {
        Enumeration<TreePath> e = tree.getExpandedDescendants( parentPath );
        if (e != null)
            while ( e.hasMoreElements() ) {
                TreePath path = e.nextElement();
                Object key = keygen.getHashKey( path.getLastPathComponent() );
                expanded.add( key );
            }
    }
    
    public void restoreExpansion()
    {
        Object root = tree.getModel().getRoot();
        if ( root == null )
            return;
        else
            restoreExpansion( new TreePath(root) );
    }
    
    private void restoreExpansion(TreePath parentPath)
    {
        Object parent = parentPath.getLastPathComponent();
        if ( expanded.contains( keygen.getHashKey( parent ) ) )
            tree.expandPath( parentPath );
        for ( int i=0; i<tree.getModel().getChildCount( parent ); ++i )
            restoreExpansion( parentPath.pathByAddingChild( tree.getModel().getChild( parent, i ) ) );
    }

    /* (non-Javadoc)
     * @see javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event.TreeExpansionEvent)
     */
    public void treeCollapsed( TreeExpansionEvent event )
    {
        Object key = keygen.getHashKey( event.getPath().getLastPathComponent() );
        expanded.remove( key );
    }

    /* (non-Javadoc)
     * @see javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event.TreeExpansionEvent)
     */
    public void treeExpanded( TreeExpansionEvent event )
    {
        Object key = keygen.getHashKey( event.getPath().getLastPathComponent() );
        expanded.add( key );
    }
    
    private static class DefaultHashKeyGenerator implements HashKeyGenerator
    {
        public Object getHashKey(Object data) { return data; }
    }
}

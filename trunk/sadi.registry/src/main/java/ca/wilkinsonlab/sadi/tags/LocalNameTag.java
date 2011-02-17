package ca.wilkinsonlab.sadi.tags;

import java.io.IOException;
import java.net.URI;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;

/**
 * A JSP tag to return the local name of a URI.
 */
@SuppressWarnings("serial")
public class LocalNameTag extends TagSupport
{
	private String uri;
	private boolean withNamespace; 
	
	public void setUri(String uri)
	{
		this.uri = uri;
	}
	
	public void setWithNamespace(String ns)
	{
		withNamespace = Boolean.valueOf(ns);
	}
	
	/**
	 * Return an appropriate local name for a URI.
	 * @param uri the URI
	 * @return an appropriate local name
	 */
	private String getLocalName()
	{
		URI u = URI.create(uri);
		String fragment = StringUtils.substringBefore( u.getFragment(), "?" );
		String lastPathElement = StringUtils.substringAfterLast(u.getPath(), "/");
		if (fragment != null) {
			return withNamespace ? String.format("%s#%s", lastPathElement, fragment) : fragment;
		} else {
			return lastPathElement;
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.jsp.tagext.Tag#doStartTag()
	 */
	@Override
	public int doStartTag() throws JspException
	{
		try {
			pageContext.getOut().print( getLocalName() );
		} catch (IOException e) {
			throw new JspException( String.format("error %s while writing to client", e) );
		}
		return SKIP_BODY;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.jsp.tagext.Tag#doEndTag()
	 */
	@Override
	public int doEndTag() throws JspException
	{
		return EVAL_PAGE;
	}

//	/* (non-Javadoc)
//	 * @see javax.servlet.jsp.tagext.Tag#release()
//	 */
//	@Override
//	public void release()
//	{
//		// TODO Auto-generated method stub
//		
//	}
//
//	/* (non-Javadoc)
//	 * @see javax.servlet.jsp.tagext.Tag#setPageContext(javax.servlet.jsp.PageContext)
//	 */
//	@Override
//	public void setPageContext(PageContext pc)
//	{
//		// TODO Auto-generated method stub
//		
//	}
//
//	/* (non-Javadoc)
//	 * @see javax.servlet.jsp.tagext.Tag#getParent()
//	 */
//	@Override
//	public Tag getParent()
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	/* (non-Javadoc)
//	 * @see javax.servlet.jsp.tagext.Tag#setParent(javax.servlet.jsp.tagext.Tag)
//	 */
//	@Override
//	public void setParent(Tag t)
//	{
//		// TODO Auto-generated method stub
//		
//	}
}

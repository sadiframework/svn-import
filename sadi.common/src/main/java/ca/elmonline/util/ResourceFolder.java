package ca.elmonline.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ResourceFolder
{
    private URL folderUrl;
    private List<URL> resources;
    
    public ResourceFolder( URL folderUrl )
    {
        if ( isFolderUrl( folderUrl ) ) {
            this.folderUrl = folderUrl;
        } else {
            try {
                this.folderUrl = makeFolderUrl( folderUrl );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( "Unable to create folder URL from " + folderUrl );
            }
        }
        resources = getResourceList( this.folderUrl );
    }
    
    public List<URL> getResources( )
    {
        return resources;
    }
    
    public void refresh()
    {
        resources = getResourceList( this.folderUrl );
    }
    
    private static String PATH_SEPARATOR = "/";
    
    private static boolean isFolderUrl( URL url )
    {
        return url.getPath().endsWith( PATH_SEPARATOR );
    }
    
    private static URL makeFolderUrl( URL url ) throws MalformedURLException
    {
        if ( isFolderUrl( url ) )
            return url;
        else
            return new URL( url, new File( url.getPath() ).getName().concat( PATH_SEPARATOR ) );
    }
    
    private static List<URL> getResourceList( URL folderUrl )
    {
        List<URL> resources = new ArrayList<URL>();
        try {
            BufferedReader reader = new BufferedReader( new InputStreamReader(
                    (InputStream)folderUrl.getContent() ) );
            for ( String line = reader.readLine() ; line != null ; line = reader.readLine() ) {
                resources.add( new URL( folderUrl, line ) );
            }
        } catch ( IOException e ) {
            // TODO log the exception...
        }
        return resources;
    }
}

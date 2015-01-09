package net.md_5.scriptus;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;

/**
 * Mojo to set a user defined property to the current commit hash, in a user
 * defined format.
 */
@Mojo(name = "describe", defaultPhase = LifecyclePhase.INITIALIZE)
public class DescribeMojo extends AbstractMojo
{

    /**
     * Maven project we are invoking.
     */
    @Parameter(name = "${project}", readonly = true)
    private MavenProject project;
    /**
     * Format used to set describe property. This first string argument will be
     * the abbreviated HEAD commit hash.
     */
    @Parameter(defaultValue = "git-${project.name}-%s")
    private String format;
    /**
     * The property to set, useful it we want to stack describes.
     */
    @Parameter(defaultValue = "describe")
    private String descriptionProperty;
    /**
     * Directory of the .git folder. Normally the current directory will be
     * sufficient.
     */
    @Parameter(property = "maven.changeSet.scmDirectory")
    private File scmDirectory;
    /**
     * Hash to use if we fail to get the Git info.
     */
    @Parameter(defaultValue = "unknown")
    private String failHash;
    /**
     * Whether or not to fail the build when we cannot get info.
     */
    @Parameter(defaultValue = "false")
    private boolean fail;
    /**
     * Length of the abbreviated hash.
     */
    @Parameter(defaultValue = "7")
    private int hashLength;

    @SuppressWarnings("UseSpecificCatch")
    public void execute() throws MojoExecutionException
    {
        String gitHash = null;

        try
        {
            Git git = Git.open( scmDirectory );

            try
            {
                ObjectId head = git.getRepository().resolve( Constants.HEAD );

                if ( head != null )
                {
                    ObjectReader reader = git.getRepository().newObjectReader();

                    try
                    {
                        gitHash = reader.abbreviate( head, hashLength ).name();
                    } finally
                    {
                        reader.release();
                    }
                } else
                {
                    getLog().warn( "Warning: Repository has no commits!" );
                }
            } finally
            {
                git.close();
            }
        } catch ( Exception ex )
        {
            if ( fail )
            {
                throw new MojoExecutionException( "Exception reading Git repo", ex );
            }
            getLog().warn( "Failed to get HEAD commit hash: " + ex.getClass().getName() + ":" + ex.getMessage() );
        }

        String formatted = String.format( format, ( gitHash == null ) ? failHash : gitHash );

        project.getProperties().put( descriptionProperty, formatted );
        getLog().info( String.format( "Set property \"%s\" to \"%s\"", descriptionProperty, formatted ) );
    }
}

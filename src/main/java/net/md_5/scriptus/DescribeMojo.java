package net.md_5.scriptus;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Mojo to set a user defined property to the current commit hash, in a user
 * defined format.
 */
@Mojo(name = "describe", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class DescribeMojo extends AbstractMojo
{

    /**
     * Maven project we are invoking.
     */
    @Parameter(property = "project", readonly = true)
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
     * Whether or not to override the existing description / time property.
     */
    @Parameter(defaultValue = "false")
    private boolean override;
    /**
     * Directory of the .git folder. Normally the current directory will be
     * sufficient.
     */
    @Parameter(property = "maven.changeSet.scmDirectory", defaultValue = "${project.basedir}")
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
    /**
     * Time property to set with the commit time.
     */
    @Parameter(defaultValue = "project.build.outputTimestamp")
    private String timeProperty;

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void execute() throws MojoExecutionException
    {
        String gitHash = null;
        int commitTime = -1;

        try
        {
            Repository repository = new FileRepositoryBuilder().findGitDir( scmDirectory ).build();

            try
            {
                ObjectId head = repository.resolve( Constants.HEAD );

                if ( head != null )
                {
                    ObjectReader reader = repository.newObjectReader();

                    try
                    {
                        gitHash = reader.abbreviate( head, hashLength ).name();

                        RevWalk walk = new RevWalk( reader );
                        try
                        {
                            commitTime = walk.parseCommit( head ).getCommitTime();
                        } finally
                        {
                            walk.close();
                        }
                    } finally
                    {
                        reader.close();
                    }
                } else
                {
                    getLog().warn( "Warning: Repository has no commits!" );
                }
            } finally
            {
                repository.close();
            }
        } catch ( RepositoryNotFoundException ex )
        {
            if ( fail )
            {
                throw new MojoExecutionException( "Could not find Git repository", ex );
            }
            getLog().warn( "Could not find Git repository in " + scmDirectory );
        } catch ( Exception ex )
        {
            if ( fail )
            {
                throw new MojoExecutionException( "Exception reading Git repository", ex );
            }
            getLog().warn( "Failed to get HEAD commit hash: " + ex.getClass().getName() + ":" + ex.getMessage() );
        }

        setProperty( descriptionProperty, String.format( format, ( gitHash == null ) ? failHash : gitHash ), override );

        boolean overrideTime = true;
        if ( project.getProperties().containsKey( timeProperty ) )
        {
            try
            {
                commitTime = Math.max( commitTime, Integer.parseInt( project.getProperties().getProperty( timeProperty ) ) );
            } catch ( NumberFormatException ex )
            {
                overrideTime = override;
            }
        }
        setProperty( timeProperty, Integer.toString( ( commitTime == -1 ) ? (int) ( System.currentTimeMillis() / 1000L ) : commitTime ), overrideTime );
    }

    private void setProperty(String property, String value, boolean override)
    {
        if ( property == null || property.isEmpty() )
        {
            return;
        }

        if ( !override && project.getProperties().containsKey( property ) )
        {
            getLog().warn( String.format( "Property \"%s\" already set to \"%s\"", property, project.getProperties().getProperty( property ) ) );
            return;
        }

        project.getProperties().put( property, value );
        getLog().info( String.format( "Set property \"%s\" to \"%s\"", property, value ) );
    }
}

package hpi;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.model.Jenkins.MasterComputer;

public class MasterToSlaves extends Notifier{

	private String folderPath;
	private boolean makeFilesExecutable;
	private String[] copiedFiles;
	private String destFolderPath;
	
	private static final Logger logger = Logger.getLogger(MasterToSlaves.class.getName()); 
	
	@DataBoundConstructor
    public MasterToSlaves(String folderPath, boolean makeFilesExecutable, boolean deleteFilesAfterBuild) {
		this.folderPath = folderPath;
		this.makeFilesExecutable = makeFilesExecutable;
    }

    public String getFolderPath() {
		return folderPath;
	}

	public boolean getMakeFilesExecutable() {
		return makeFilesExecutable;
	}
	
	public String getDestFolderPath() {
		return destFolderPath;
	}
	
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());
		Jenkins jenkins = Hudson.getActiveInstance();
		FilePath hudsonRoot = jenkins.getRootPath();

        if(Computer.currentComputer() instanceof MasterComputer) {
    		FilePath copyFrom = new FilePath(hudsonRoot, folderPath);
    		Computer [] computers = jenkins.getComputers();
    	    for(Computer c : computers) {
    	    	if(c instanceof SlaveComputer) {
    	    		FilePath copyTo = new FilePath(hudsonRoot, destFolderPath);
    	    		logger.info("Copying data from " + copyFrom.toURI()+ " to " + copyTo.toURI());
    	    		listener.getLogger().println("Copying data from, "+copyFrom.toURI()+" to " + copyTo.toURI());
    	    		copyFrom.copyRecursiveTo(copyTo);
    	    		logger.info("Saving file Names");
    	    		saveNames(copyFrom);
    	    		logger.info("Making executable");
    	    		if (makeFilesExecutable) {
    	    			seeFolder(copyTo);
    	    		}
    	    	}
    	    }
    		return true;	
        }
		return false;   	
    }

    
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(MasterToSlaves.class);
        }
        
        public FormValidation doCheckFolderPath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
            	return FormValidation.error("Empty source folder path");
            }
        	return FormValidation.ok();
        }
        
        public FormValidation doCheckDestFolderPath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (0 == value.length()) {
            	return FormValidation.error("Empty destination folder path");
            }
        	return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Copy data to slaves";
        }

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
		}

    }
    
    void seeFolder(FilePath path) throws IOException, InterruptedException {
    	List<FilePath> children = path.list();
		for (FilePath child : children) {
			if (child.isDirectory()) {
				seeFolder(child);
			} else {
				child.chmod(0755);
			}
		}
    }
    
    void saveNames(FilePath path) throws IOException, InterruptedException {
    	List<FilePath> children = path.list();
    	this.copiedFiles = new String[children.size()];
    	int i = 0;
		for (FilePath child : children) {
			copiedFiles[i] = child.getName(); 
			i++;
			logger.info("Saving name = " + child.getName());
		}
    }

}

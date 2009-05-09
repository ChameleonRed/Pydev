package org.python.pydev.navigator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;

public class PythonPathNatureStub implements IPythonPathNature{

    private HashSet<String> projectSourcePathSet;

    public PythonPathNatureStub(HashSet<String> projectSourcePathSet) {
        this.projectSourcePathSet = projectSourcePathSet; 
    }

    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        throw new RuntimeException("Not impl");
        
    }

    public String getOnlyProjectPythonPathStr() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public String getProjectExternalSourcePath(boolean resolve) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public String getProjectSourcePath(boolean resolve) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public Set<String> getProjectSourcePathSet(boolean resolve) throws CoreException {
        return projectSourcePathSet;
    }

    public void setProject(IProject project, IPythonNature nature) {
        throw new RuntimeException("Not impl");
        
    }

    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setProjectSourcePath(String newSourcePath) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void clearCaches() {
    }

    public void setVariableSubstitution(Map<String, String> variableSubstitution){
        throw new RuntimeException("Not impl");
    }

    public Map<String, String> getVariableSubstitution() throws CoreException{
        throw new RuntimeException("Not implemented");
    }

}

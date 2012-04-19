package org.python.pydev.debug.newconsole;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.InterpreterResponse;

/**
 * This class allows console to communicate with python backend by using the existing
 * debug connection.
 * 
 * @author hussain.bohra
 * 
 */
public class PydevDebugConsoleCommunication implements
		IScriptConsoleCommunication {


	private EvaluateDebugConsoleExpression evaluateConsoleExpression;

	private int TIMEOUT = PydevConsoleConstants.CONSOLE_TIMEOUT;
	private boolean isConsoleInitialized = false;

	String EMPTY = (String) StringUtils.EMPTY;
	
    /**
     * Signals that the next command added should be sent as an input to the server.
     */
    private volatile boolean waitingForInput;
    
    /**
     * Input that should be sent to the server (waiting for raw_input)
     */
    private volatile String inputReceived;

    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock = new Object();

    /**
     * Response that should be sent back to the shell.
     */
    private volatile InterpreterResponse nextResponse;
    
	public PydevDebugConsoleCommunication(){
        
		this.evaluateConsoleExpression = new EvaluateDebugConsoleExpression();
	}
	

	/**
	 * Initialize the console
	 * 
	 */
	public EvaluateDebugConsoleExpression.PydevDebugConsoleMessage intializeConsole(){
		isConsoleInitialized = false;
		EvaluateDebugConsoleExpression.PydevDebugConsoleMessage consoleMessage = null;
		this.evaluateConsoleExpression.initializeConsole();
		String result = this.evaluateConsoleExpression.waitForCommand();
		if (result == null) {
			// Timeout occured
			return consoleMessage;
		}
		try {
			consoleMessage = XMLUtils.getConsoleMessage(result);
			isConsoleInitialized = true;
		} catch (CoreException e) {
			Log.log(e);
		}
		return consoleMessage;
	}
	
	public void execInterpreter(final String command,
			final ICallback<Object, InterpreterResponse> onResponseReceived,
			final ICallback<Object, Tuple<String, String>> onContentsReceived) {
		
		nextResponse = null;
		if (waitingForInput) {
			inputReceived = command;
			waitingForInput = false;
			// the thread that we started in the last exec is still alive if we were waiting for an input.
		} else {
			// create a thread that'll keep locked until an answer is received from the server.
			Job job = new Job("PyDev Debug Console Communication") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					PyStackFrame frame = evaluateConsoleExpression.getLastSelectedFrame();
					if (frame != null){
						if (!isConsoleInitialized) {
							isConsoleInitialized = false;
							EvaluateDebugConsoleExpression.PydevDebugConsoleMessage consoleMessage = intializeConsole();
							if (consoleMessage == null) {
								nextResponse = new InterpreterResponse(EMPTY,
										"Error: Unable to launch console for the selected frame: " + frame.getId() + "\n", false, false);
								return Status.CANCEL_STATUS;
							} else {
								if (consoleMessage.getOutputMessage().toString() != EMPTY){
									nextResponse = new InterpreterResponse(consoleMessage.getOutputMessage().toString(),
											consoleMessage.getErrorMessage().toString(), consoleMessage.isMore(), false);
									return Status.CANCEL_STATUS;
								} else {
									consoleMessage.getErrorMessage().toString();
									nextResponse = new InterpreterResponse(consoleMessage.getOutputMessage().toString(), 
											consoleMessage.getErrorMessage().toString(), consoleMessage.isMore(), false);
									isConsoleInitialized = true;
								}
							}
						}
					} else {
						nextResponse = new InterpreterResponse(EMPTY,
								"[Invalid Frame]: Please select frame to connect the console."
										+ "\n", false, false);
						return Status.CANCEL_STATUS;
					}
                    evaluateConsoleExpression.resetPayload();
                    evaluateConsoleExpression.executeCommand(command);
                    String result = evaluateConsoleExpression.waitForCommand();
                    try {
                        if(result.length() == 0){
                            //timed out
                            nextResponse = new InterpreterResponse(result, EMPTY, false, false);
                            return Status.CANCEL_STATUS;
                            
                        }else{
    						EvaluateDebugConsoleExpression.PydevDebugConsoleMessage consoleMessage = XMLUtils
    						        .getConsoleMessage(result);
    	                    nextResponse = new InterpreterResponse(consoleMessage.getOutputMessage().toString(),
    	                    		consoleMessage.getErrorMessage().toString(), consoleMessage.isMore(), false);
                        }
					} catch (CoreException e) {
						Log.log(e);
	                    nextResponse = new InterpreterResponse(result, EMPTY, false, false);
	                    return Status.CANCEL_STATUS;
					}
					
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}

        int timeOut = TIMEOUT; //only get contents each 500 millis...
		// busy loop until we have a response
		while (nextResponse == null) {
			synchronized (lock) {
				try {
					lock.wait(20);
				} catch (InterruptedException e) {
				}
			}
			timeOut -= 20;

			if (timeOut <= 0 && nextResponse == null) {
				timeOut = TIMEOUT/2; // after the first, get it each 250 millis
			}
		}
		onResponseReceived.call(nextResponse);
	}

	public ICompletionProposal[] getCompletions(String text, String actTok,
			int offset) throws Exception {
		ICompletionProposal[] receivedCompletions = {};
		if (waitingForInput) {
			return new ICompletionProposal[0];
		}
		String result = evaluateConsoleExpression
				.getCompletions(actTok, offset);
		if (result.length() > 0) {
			List<Object[]> fromServer = XMLUtils.XMLToCompletions(result);
			List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();
			PydevConsoleCommunication.convertToICompletions(text, actTok,
					offset, fromServer, ret);
			receivedCompletions = ret.toArray(new ICompletionProposal[ret
					.size()]);
		}
		return receivedCompletions;
	}

	public String getDescription(String text) throws Exception {
		return null;
	}

	public void close() throws Exception {
		evaluateConsoleExpression.close();
	}

	/**
	 * Enable/Disable linking of the debug console with the suspended frame.
	 */
	public void linkWithDebugSelection(boolean isLinkedWithDebug) {
		evaluateConsoleExpression.linkWithDebugSelection(isLinkedWithDebug);
	}

}

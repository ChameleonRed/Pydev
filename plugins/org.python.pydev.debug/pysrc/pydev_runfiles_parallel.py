import unittest
import Queue
from pydevd_constants import * #@UnusedWildImport
import pydev_runfiles_xml_rpc
import time
import os

#=======================================================================================================================
# FlattenTestSuite
#=======================================================================================================================
def FlattenTestSuite(test_suite, ret):
    if isinstance(test_suite, unittest.TestSuite):
        for t in test_suite._tests:
            FlattenTestSuite(t, ret)
            
    elif isinstance(test_suite, unittest.TestCase):
        ret.append(test_suite)


#=======================================================================================================================
# ExecuteTestsInParallel
#=======================================================================================================================
def ExecuteTestsInParallel(tests, jobs, split, verbosity):
    '''
    @param tests: list(PydevTestSuite)
        A list with the suites to be run
        
    @param split: str
        Either 'module' or the number of tests that should be run in each batch
    '''
    #This queue will receive the tests to be run. Each entry in a queue is a list with the tests to be run together When
    #split == 'tests', each list will have a single element, when split == 'module', each list will have all the tests
    #from a given module.
    tests_queue = []
    
    queue_elements = []
    if split == 'module':
        module_to_tests = {}
        for test in tests:
            lst = []
            FlattenTestSuite(test, lst)
            for test in lst:
                key = (test.__pydev_pyfile__, test.__pydev_module_name__)
                module_to_tests.setdefault(key, []).append(test)
        
        for key, tests in module_to_tests.items():
            queue_elements.append(tests)
    
    elif split == 'tests':
        for test in tests:
            lst = []
            FlattenTestSuite(test, lst)
            for test in lst:
                queue_elements.append([test])
    
    else:
        raise AssertionError('Do not know how to handle: %s' % (split,))
    
    for test_cases in queue_elements:
        test_queue_elements = []
        for test_case in test_cases:
            try:
                test_name = test_case.__class__.__name__+"."+test_case._testMethodName
            except AttributeError:
                #Support for jython 2.1 (__testMethodName is pseudo-private in the test case)
                test_name = test_case.__class__.__name__+"."+test_case._TestCase__testMethodName

            test_queue_elements.append(test_case.__pydev_pyfile__+'|'+test_name)
        
        tests_queue.append(test_queue_elements)
        
    
    
    queue = Queue.Queue()
    for item in tests_queue:
        queue.put(item, block=False)

    
    providers = []
    clients = []
    for i in range(jobs):
        test_cases_provider = CommunicationThread(queue)
        providers.append(test_cases_provider)
        
        test_cases_provider.start()
        port = test_cases_provider.port
        
        clients.append(ClientThread(i, port, verbosity))
        
    for client in clients:
        client.start()

    client_alive = True
    while client_alive:    
        client_alive = False
        for client in clients:
            #Wait for all the clients to exit.
            if not client.finished:
                client_alive = True
                time.sleep(.2)
                break
    
    for provider in providers:
        provider.shutdown()
    
    
    
#=======================================================================================================================
# CommunicationThread
#=======================================================================================================================
class CommunicationThread(threading.Thread):
    
    def __init__(self, tests_queue):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.queue = tests_queue
        self.finished = False
        from pydev_imports import SimpleXMLRPCServer
        
        
        # This is a hack to patch slow socket.getfqdn calls that
        # BaseHTTPServer (and its subclasses) make.
        # See: http://bugs.python.org/issue6085
        # See: http://www.answermysearches.com/xmlrpc-server-slow-in-python-how-to-fix/2140/
        try:
            import BaseHTTPServer
            def _bare_address_string(self):
                host, port = self.client_address[:2]
                return '%s' % host
            BaseHTTPServer.BaseHTTPRequestHandler.address_string = _bare_address_string
            
        except:
            pass
        # End hack.


        # Create server
        
        import pydev_localhost
        server = SimpleXMLRPCServer((pydev_localhost.get_localhost(), 0), logRequests=False)
        server.register_function(self.GetTestsToRun)
        server.register_function(self.notifyStartTest)
        server.register_function(self.notifyTest)
        server.register_function(self.notifyCommands)
        self.port = server.socket.getsockname()[1]
        self.server = server
        
        
    def GetTestsToRun(self, job_id):
        '''
        @param job_id:
        
        @return: list(str)
            Each entry is a string in the format: filename|Test.testName 
        '''
        try:
            ret = self.queue.get(block=False)
            return ret
        except: #Any exception getting from the queue (empty or not) means we finished our work on providing the tests.
            self.finished = True
            return []


    def notifyCommands(self, job_id, commands):
        #Batch notification.
        for command in commands:
            getattr(self, command[0])(job_id, *command[1], **command[2])
            
        return True

    def notifyStartTest(self, job_id, *args, **kwargs):
        pydev_runfiles_xml_rpc.notifyStartTest(*args, **kwargs)
        return True
    
    
    def notifyTest(self, job_id, *args, **kwargs):
        pydev_runfiles_xml_rpc.notifyTest(*args, **kwargs)
        return True
    
    def shutdown(self):
        if hasattr(self.server, 'shutdown'):
            self.server.shutdown()
        else:
            self._shutdown = True
    
    def run(self):
        if hasattr(self.server, 'shutdown'):
            self.server.serve_forever()
        else:
            self._shutdown = False
            while not self._shutdown:
                self.server.handle_request()
        
    
    
#=======================================================================================================================
# Client
#=======================================================================================================================
class ClientThread(threading.Thread):
    
    def __init__(self, job_id, port, verbosity):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.port = port
        self.job_id = job_id
        self.verbosity = verbosity
        self.finished = False


    def _reader_thread(self, pipe, target):
        while True:
            target.write(pipe.read(1))
            
        
    def run(self):
        try:
            import pydev_runfiles_parallel_client
            args = [
                sys.executable, 
                pydev_runfiles_parallel_client.__file__, 
                str(self.job_id), 
                str(self.port), 
                str(self.verbosity), 
            ]
            if False:
                proc = subprocess.Popen(args, env=os.environ, shell=False, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                
                stdout_thread = threading.Thread(target=self._reader_thread,args=(proc.stdout, sys.stdout))
                stdout_thread.setDaemon(True)
                stdout_thread.start()
    
                stderr_thread = threading.Thread(target=self._reader_thread,args=(proc.stderr, sys.stderr))
                stderr_thread.setDaemon(True)
                stderr_thread.start()
            else:
                import subprocess
                proc = subprocess.Popen(args, env=os.environ, shell=False)
                proc.wait()

        finally:
            self.finished = True


package edu.ualr.oyster.core;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ualr.oyster.ErrorFormatter;
import edu.ualr.oyster.OysterExplanationFormatter;
import edu.ualr.oyster.data.ClusterRecord;
import edu.ualr.oyster.data.ClusterTypes;
import edu.ualr.oyster.data.RecordTypes;
import edu.ualr.oyster.er.OysterAssertionEngine;
import edu.ualr.oyster.er.OysterClusterEngine;
import edu.ualr.oyster.er.OysterMergeConsolidationEngine;
import edu.ualr.oyster.er.OysterMergeEngine;
import edu.ualr.oyster.er.OysterResolutionEngine;
import edu.ualr.oyster.index.NullIndex;
import edu.ualr.oyster.index.TalburtZhouInvertedIndex;
import edu.ualr.oyster.io.AttributesParser;
import edu.ualr.oyster.io.OysterDelimitedReader;
import edu.ualr.oyster.io.RunScriptParser;
import edu.ualr.oyster.io.SourceDescriptorParser;
import edu.ualr.oyster.kb.DBEntityMap;
import edu.ualr.oyster.kb.EntityMap;
import edu.ualr.oyster.kb.OysterIdentityRepository;

public class OysterServiceMain extends OysterMain {
	private boolean sorted = false;
	private boolean stats = false;
	private boolean keepPreviousDBTable = false;
	private boolean error = false;

	private int sortValue = 1;
	private int gc = -1;
	private int topStart = -1;
	private int topStop = -1;
	private int errorCode = 0;

	// UCD - Added source and output files
	private String logFile = "";
	private String rsFile = "";
	private String debugFile = "";
	private String srcFile = "", outFile = "";
	private Level logLevel = Level.OFF;
	private static final int SUCCESSFUL_INITIALIZATION = 0;
	
	@Override
	public int process(String[] args) {
		throw new UnsupportedOperationException(
				"OysterServiceMain does not support batch processing.");
	}

	public OysterServiceMain(String configurationFile) {
		super();
		rsFile = configurationFile;
		
		try {

			int initializationOutcome = initialize();
			
			if (SUCCESSFUL_INITIALIZATION != initializationOutcome) {
				throw new OysterInitializationException("Initialization failed in initialize method with an error code of " + initializationOutcome);
			}
			
		} catch (Exception e) {
			throw new OysterInitializationException("Initialization failed with exception " + e, e);
		}

		
	}

	public synchronized String attemptMatch(String matchString) {

		// reset the rules Map for each run
		repository.setRuleMap(new HashMap<String, LinkedHashSet<String>>());

		openOysterOutputs(keepPreviousDBTable);

	    // From about line 740.  Get a Reader for the reference files.  For this web service it will be a a File Delimited type reader.
        OysterDelimitedReader osr = new OysterDelimitedReader(getSource().getSourcePath(), getSource().getDelimiter(), getSource().getQualifer(), getSource().isLabel(), getSource().getReferenceItems(), logger);
       
        osr.open(matchString);
        getSource().setSourceReader(osr);
        
        
        engine.setClusterRecord(getSource().getSourceReader().getClusterRecord());

        // Another method with a side effect.  This actually reads in the reference file.
        source.getSourceReader().getNextReference();
        
        // About line 791
        // Ask ResolutionEngine to resolve reference
        ((OysterResolutionEngine) engine).integrateSource(sorted, source.getSourceReader().getRecordCount());

        osr.close();

        // About line 858:
       	getSource().getSourceReader().close();      

       	ByteArrayOutputStream matchOutput = new ByteArrayOutputStream();
       	
       	// from setOysterOutputs
        try {
			repository.setLinkMapWriter(new PrintWriter(new OutputStreamWriter(matchOutput, "UTF8")));
		} catch (UnsupportedEncodingException e) {
			// Ignored
		}
       	// About line 976:
       	repository.close(runScript.isChangeReportDetail(), rsFile, runScript.getName(), OysterIdentityRepository.OUTPUT_TYPE.SITE);
       	
       	return matchOutput.toString();
	}
	
	private int initialize() throws Exception {
        Level logLevel = Level.OFF;
        Map<String, Long> totals = new LinkedHashMap<String, Long>();
        Map<String, Long> rswooshs = new LinkedHashMap<String, Long>();

        try {
            // read run script (XML)
            RunScriptParser rsParser = new RunScriptParser();
            setRunScript(rsParser.parse(rsFile));

            // UCD - Added output file
            if (!outFile.isEmpty()) {
                runScript.setLinkOutputLocation(outFile);
            }
            
            // initialize logger
            logger = Logger.getLogger(getClass().getName());

            // remove root handlers and disable any references to root handlers
            logger.setUseParentHandlers(false);
            Logger globalLogger = Logger.getLogger("global");
            Handler[] handlers = globalLogger.getHandlers();
            for (Handler handler : handlers) {
                globalLogger.removeHandler(handler);
            }

            // add handlers
            if (runScript.getLogFile() != null) {
                logFile = runScript.getLogFile();
                if (runScript.getLogFileNum() > 1) {
                	logFile = formatLogFile(logFile);
                }
                fileHandler = new FileHandler(logFile, runScript.getLogFileSize(), runScript.getLogFileNum());
                fileHandler.setEncoding("UTF8");
                logger.addHandler(fileHandler);
                OysterExplanationFormatter formatter = new OysterExplanationFormatter();
                fileHandler.setFormatter(formatter);

            } else {
            	logger.addHandler(new ConsoleHandler());
            	logger.info("Set logger to console");
            }

            if (runScript.isDebug() && runScript.isExplanation()) {
                logLevel = Level.FINEST;
            } else if (runScript.isDebug() && !runScript.isExplanation()) {
                logLevel = Level.FINE;
            } else if (!runScript.isDebug() && runScript.isExplanation()) {
                logLevel = Level.INFO;
            } else if (!runScript.isDebug() && !runScript.isExplanation()) {
                logLevel = Level.SEVERE;
            }

            // set Garbage Collection
            gc = runScript.getGc();

            // set level and formatter
            logger.info("Setting logging level to " + logLevel);
            logger.setLevel(logLevel);

            StringBuilder sb = new StringBuilder(150);

            startTime = now();

            // validate RunScript
            if (validateRunScript(rsFile)) {
                // create a single instance of OysterAttributes
                AttributesParser aParser = new AttributesParser();
                setAttributes(aParser.parse(runScript.getAttributeLocation()));

                stats = runScript.isSystemStats();

                // Validate Item Names in Identity Rules against Item Names in Reference Items
                if (validateAttributes(attributes)) {
                    // Instantiate Comparators
                    initializeComparators();

                    // validate rule matchCodes against Compatators
                    if (validateRuleMatchCodes(attributes) && 
                        validateRuleNumbering(attributes) && 
                        validateIndexingRules(attributes) &&
                        validateIndexingScanRules(attributes) &&
                        validateIndexingRuleNumbering(attributes)) {

                        //==========================================================================
                        // Initializing the Index
                        //==========================================================================
                        if (attributes.getIndexingRules() != null && !attributes.getIndexingRules().isEmpty()){
                        	TalburtZhouInvertedIndex tzIndex = new TalburtZhouInvertedIndex();
                        	tzIndex.setRules(attributes.getIndexingRules());
                        	index = tzIndex;
                        } else {
                        	index = new NullIndex();
                        }
                        
                        index.setPassThruAttributes(passThruAttributes);

                        //==========================================================================                    
                        // Initialize the Record & Cluster Types
                        // These are hardcoded right now but should become dynamic in the future
                        //==========================================================================
                        // OysterIdentityRecord Type
                        recordType = RecordTypes.MAP;
                        clusterType = ClusterTypes.MAP;
                        
                        //==========================================================================                    
                        // Initialize the EntityMap
                        //==========================================================================
                        if (runScript.getEntityMapType() == null) {
                            entityMap = new EntityMap(new LinkedHashMap<String, ClusterRecord>(), recordType);
                        } else if (runScript.getEntityMapType().equalsIgnoreCase("EntityMap")) {
                            entityMap = new EntityMap(new LinkedHashMap<String, ClusterRecord>(), recordType);
                        } else if (runScript.getEntityMapType().equalsIgnoreCase("DBEntityMap")) {
                            DBEntityMap dbEntityMap = new DBEntityMap(new LinkedHashMap<String, ClusterRecord>(), runScript.getEntityMapCType(), runScript.getEntityMapServer(), runScript.getEntityMapPort(), runScript.getEntityMapSID(), runScript.getEntityMapUserID(), runScript.getEntityMapPasswd(), recordType);
                            if (dbEntityMap.isConnected(keepPreviousDBTable)) {
                                entityMap = dbEntityMap;
                            } else {
                                return (-1000);
                            }
                        } else {
                            try {
                                Class<?> comp = Class.forName(runScript.getEntityMapType());
                                entityMap = (EntityMap) comp.newInstance();
                            } catch (Exception ex) {
                                entityMap = new EntityMap(new LinkedHashMap<String, ClusterRecord>(), recordType);
                            }
                        }

                        // open the outputs for this mode
                        if (openOysterOutputs(keepPreviousDBTable)) {
                            if (this.validateSourceDescriptorNames(runScript.getSources())) {
                                //==================================================================
                                // Create the ER engine
                                //==================================================================
                                if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_MERGE_PURGE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_RECORD_LINKAGE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_CAPTURE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_RESOLVE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_UPDATE)) {
                                    if (runScript.getEngineType() != null && runScript.getEngineType().equalsIgnoreCase("RSwooshStandard")) {
                                        setEngine(new OysterMergeEngine(logger, recordType));
                                    } else if (runScript.getEngineType() != null && runScript.getEngineType().equalsIgnoreCase("RSwooshEnhanced")) {
                                        setEngine(new OysterMergeConsolidationEngine(logger, recordType));
                                    } else if (runScript.getEngineType() != null && runScript.getEngineType().equalsIgnoreCase("FSCluster")) {
                                        setEngine(new OysterClusterEngine(logger, recordType));
                                    } else {
                                        setEngine(new OysterMergeEngine(logger, recordType));
                                    }

                                    // set the rule list for the engine
                                    ((OysterResolutionEngine) engine).setRuleList(attributes.getIdentityRules());
                                    ((OysterResolutionEngine) engine).setAttributes(attributes);
                                    ((OysterResolutionEngine) engine).setRuleFreq(ruleFreq);
                                    ((OysterResolutionEngine) engine).setCompleteRuleFiring(completeRuleFiring);
                                } else {
                                    setEngine(new OysterAssertionEngine(logger, "@"+runScript.getRunMode(), recordType));
                                }

                                // set Repository Metadata
                                repository.setDate(now());
                                repository.setRunScriptName(runScript.getName());
                                repository.setOysterVersion(version);

                                // set the ER engine parameters
                                engine.setRepository(repository);
                                engine.setDebug(runScript.isDebug());

                                // do I need to preload the index?
                                if (runScript.isPreLoad()) {
                                    preloadIndex();
                                }

                                if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_MERGE_PURGE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_RECORD_LINKAGE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_CAPTURE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_RESOLVE) ||
                                    runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_UPDATE)) {
                                    ((OysterResolutionEngine) engine).setByPassFilter(runScript.isBypassFilter());

                                    // deterimine the least common rule denominator
                                    if (!((OysterResolutionEngine) engine).isByPassFilter()) {
                                        if (runScript.getLcrd() != null && runScript.getLcrd().size() > 0) {
                                            ((OysterResolutionEngine) engine).setPrimaryFilter(runScript.getLcrd());
                                            ((OysterResolutionEngine) engine).setSecondaryFilter(lcrd(attributes.getIdentityRules()));
                                        } else {
                                            ((OysterResolutionEngine) engine).setPrimaryFilter(lcrd(attributes.getIdentityRules()));
                                        }
                                    } else {
                                        logger.info("Bypassing Least Common Rule filter");
                                    }
                                    
                                    for (Iterator<String> it = runScript.getSources().iterator(); it.hasNext();) {
                                        String file = it.next();
                                        if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_MERGE_PURGE) ||
                                            runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_RECORD_LINKAGE) ||
                                            runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_CAPTURE) ||
                                            runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_IDENT_UPDATE)) {
                                            engine.setCapture(true);
                                        } else {
                                            engine.setCapture(false);
                                        }

                                        // read sourceDescriptor script (XML)
                                        SourceDescriptorParser sdParser = new SourceDescriptorParser();
                                        setSource(sdParser.parse(file));
                                        
                                        // UCD - Added source file  
                                        getSource().setSourcePath(srcFile);
                                        

                                        // Validate the attribute names in the metadata for Reference Items
                                        if (validateReferences(getSource(), attributes)) {
                                            // sync the oir metadata
                                            syncOysterIdentityRecord();
                                            // open the reference source
                                            OysterDelimitedReader osr = new OysterDelimitedReader(getSource().getSourcePath(), getSource().getDelimiter(), getSource().getQualifer(), getSource().isLabel(), getSource().getReferenceItems(), logger);
                                            osr.open("");
                                            getSource().setSourceReader(osr);

                                            // Create Optimized Rule Matrix
                                            ((OysterResolutionEngine) engine).createMatrix();
                                            ((OysterResolutionEngine) engine).populateMasks();
                                            
                                            // get the system stats before reading current source
                                            if (stats) {
                                                logger.info("System stats before reading current source");
                                                systemStats();
                                            }

                                            String start, stop;
                                            start = now();
                                            while (source.getSourceReader().getNextReference() > 0 && !die) {
                                                if (debugRecords.contains(getSource().getSourceReader().getClusterRecord().getMergedRecord().get("@RefID"))){
                                                    logger.setLevel(Level.FINEST);
                                                }

                                            
                                                // turn off
                                                if (debugRecords.contains(getSource().getSourceReader().getClusterRecord().getMergedRecord().get("@RefID"))){
                                                    logger.setLevel(logLevel);
                                                }
                                            }

                                            if (((OysterResolutionEngine) engine).hasPostConsolidation()) {
                                                // get the system stats Pre RSwoosh
                                                if (stats) {
                                                    logger.info("System stats Pre RSwoosh");
                                                    systemStats();
                                                }

                                                ((OysterResolutionEngine) engine).postConsolidation(sorted, getSource().getSourceReader().getRecordCount(), getSource().getSourceReader().getCountPoint());
                                            }

                                            // get the system stats post RSwoosh
                                            if (stats) {
                                                if (((OysterResolutionEngine) engine).hasPostConsolidation()) {
                                                    logger.info("System stats Post RSwoosh");
                                                } else {
                                                    logger.info("System stats After Run");
                                                }
                                                systemStats();
                                            }

                                            // close the source
                                            getSource().getSourceReader().close();

                                            totals.put(file, getSource().getSourceReader().getRecordCount() - 1);
                                            rswooshs.put(file, ((OysterResolutionEngine) engine).getTempCounter());
                                            totalRecords += getSource().getSourceReader().getRecordCount() - 1;
                                            totalRSwooshs += ((OysterResolutionEngine) engine).getTempCounter();
                                            ((OysterResolutionEngine) engine).setTempCounter(0);
                                        } else {
                                            logger.severe("##ERROR: Reference Items and Attributes do not match.");
                                            error = true;
                                        }
                                    }
                                    for (Iterator<Entry<String, Long>> it = totals.entrySet().iterator(); it.hasNext();) {
                                        Entry<String, Long> entry = it.next();
                                        long count = entry.getValue();
                                        long rswoosh = rswooshs.get(entry.getKey());
                                        sb = new StringBuilder(100);
                                        sb.append("Records processed for ")
                                          .append(entry.getKey())
                                          .append(": ")
                                          .append(count)
                                          .append("(")
                                          .append(rswoosh)
                                          .append(")")
                                          .append(System.getProperty("line.separator"));
                                        logger.info(sb.toString());
                                    }
                                    sb = new StringBuilder(100);
                                    sb.append("# of Consolidation Steps: ")
                                      .append(totalRSwooshs)
                                      .append(System.getProperty("line.separator"));
                                    logger.info(sb.toString());
                                } else {
                                    // read sourceDescriptor script (XML)
                                    SourceDescriptorParser sdParser = new SourceDescriptorParser();
                                    setSource(sdParser.parse(runScript.getAssertionInputLocation()));

                                    // Validate the attribute names in the metadata for Reference Items
                                    if (validateReferences(getSource(), attributes)) {
                                        // sync the oir metadata
                                        syncOysterIdentityRecord();
                                    
                                        sb = new StringBuilder(100);
                                        sb.append("Source: ")
                                          .append(getSource().getSourcePath())
                                          .append(System.getProperty("line.separator"));
                                        logger.info(sb.toString());

                                        // Ignored two
//                                        OysterDelimitedReader osr = new OysterDelimitedReader(getSource().getSourcePath(), getSource().getDelimiter(), getSource().getQualifer(), getSource().isLabel(), getSource().getReferenceItems(), logger);
//                                        osr.open();
////                                        osr.setCountPoint(1000);
//                                        getSource().setSourceReader(osr);
//======================================
// CAN I MOVE THIS OUTSIDE THE LOOP
//======================================
                                        engine.setCurrentSourceName(getSource().getSourceName());
                                        getSource().getSourceReader().setSource(getSource().getSourceName());

                                        // set engine options for this sources run
                                        getSource().getSourceReader().getClusterRecord().setCurrentRunID(repository.getMid());
                                        engine.setClusterRecord(getSource().getSourceReader().getClusterRecord());
//======================================
//======================================
                                        // get the system stats before reading current source
                                        if (stats) {
                                            logger.info("System stats before reading current source");
                                            systemStats();
                                        }
                                    
                                        while (getSource().getSourceReader().getNextReference() > 0 && !((OysterAssertionEngine) engine).isBadAssert() && !die) {
                                            if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_ASSERT_REF2REF)) {
                                                ((OysterAssertionEngine) engine).assertRefToRef(getSource().getSourceReader().getRecordCount());
                                            } else if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_ASSERT_REF2STR)) {
                                                ((OysterAssertionEngine) engine).assertRefToStr(getSource().getSourceReader().getRecordCount());
                                            } else if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_ASSERT_STR2STR)) {
                                                ((OysterAssertionEngine) engine).assertStrToStr(getSource().getSourceReader().getRecordCount());
                                            } else if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_ASSERT_SPLIT_STR)) {
                                                ((OysterAssertionEngine) engine).assertSplitStr(getSource().getSourceReader().getRecordCount());
                                            }
                                        }
                                    
                                        if (((OysterAssertionEngine) engine).isBadAssert()) {
                                            error = true;
                                            errorCode = ((OysterAssertionEngine) engine).getAssertCode();
                                        } else {
                                            // handle the last record group
                                            if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_ASSERT_STR2STR)) {
                                                ((OysterAssertionEngine) engine).postAssertStrToStr(getSource().getSourceReader().getRecordCount());
                                            } else if (runScript.getRunMode().equalsIgnoreCase(OysterKeywords.RUNMODE_ASSERT_SPLIT_STR)) {
                                                ((OysterAssertionEngine) engine).postAssertNegStr(getSource().getSourceReader().getRecordCount());
                                                ((OysterAssertionEngine) engine).postNeg();
                                    
                                                if (((OysterAssertionEngine) engine).isBadAssert()) {
                                                    error = true;
                                                    errorCode = ((OysterAssertionEngine) engine).getAssertCode();
                                                }
                                            }
                                        }
                                    } else {
                                        logger.severe("##ERROR: Reference Items and Attributes do not match.");
                                        error = true;
                                        errorCode = -2000;
                                    }
                                }
                                //==========================================================================
                                // Close files
                                //==========================================================================
                                groups = repository.getEntityMap().getSize();
//                               rsf=rsFile;
                            
                                if (!error) {
                                    repository.close(runScript.isChangeReportDetail(), rsFile, runScript.getName());
                                }
                            } else {
                                logger.severe("##ERROR: SourceDescriptor Names are not Unique.");
                                error = true;
                                errorCode = -2300;
                            }
                        } else {
                            logger.severe("##ERROR: Unable to open Oyster outputs.");
                            error = true;
                            errorCode = -1000;
                        }
                    } else {
                        logger.severe("##ERROR: Comparator and MatchCodes do not match.");
                        error = true;
                        errorCode = -2100;
                    }
                } else {
                    logger.severe("##ERROR: Reference Items and Rules do not match.");
                    error = true;
                    errorCode = -2200;
                }
            } else {
                logger.severe("##ERROR: Invalid RunScript.");
                error = true;
                errorCode = -1100;
            }
        } catch (Exception ex) {
            Logger.getLogger(OysterMain.class.getName()).log(Level.SEVERE, ErrorFormatter.format(ex), ex);
        }
        stopTime = now();

        // get the system stats at the end
        if (stats) {
            logger.info("System stats at the end");
            systemStats();
        }

        String elapsedTime = "";
        if (startTime != null && stopTime != null) {
            elapsedTime = elapsedTime();
        } else {
            error = true;
        }

        //==========================================================================
        // Output reports and statistics
        //==========================================================================
        if (error) {
            logger.severe("Process ended with Errors! Please check the log file.");
            logger.severe("Error Code = " + errorCode);
        } else {
            outputStats(elapsedSecs(startTime, stopTime));
        }
        
        StringBuilder sb = new StringBuilder(100);
        sb.append("Time process started at ")
          .append(startTime)
          .append(System.getProperty("line.separator"));
        sb.append("Time process ended at ")
          .append(stopTime)
          .append(System.getProperty("line.separator"));
        sb.append("Total elapsed time ")
          .append(elapsedTime)
          .append(System.getProperty("line.separator"));
        logger.info(sb.toString());

        return errorCode;
    }

	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	public boolean isStats() {
		return stats;
	}

	public void setStats(boolean stats) {
		this.stats = stats;
	}

	public boolean isKeepPreviousDBTable() {
		return keepPreviousDBTable;
	}

	public void setKeepPreviousDBTable(boolean keepPreviousDBTable) {
		this.keepPreviousDBTable = keepPreviousDBTable;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public int getSortValue() {
		return sortValue;
	}

	public void setSortValue(int sortValue) {
		this.sortValue = sortValue;
	}

	public int getGc() {
		return gc;
	}

	public void setGc(int gc) {
		this.gc = gc;
	}

	public int getTopStart() {
		return topStart;
	}

	public void setTopStart(int topStart) {
		this.topStart = topStart;
	}

	public int getTopStop() {
		return topStop;
	}

	public void setTopStop(int topStop) {
		this.topStop = topStop;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public String getRsFile() {
		return rsFile;
	}

	public void setRsFile(String rsFile) {
		this.rsFile = rsFile;
	}

	public String getDebugFile() {
		return debugFile;
	}

	public void setDebugFile(String debugFile) {
		this.debugFile = debugFile;
	}

	public String getSrcFile() {
		return srcFile;
	}

	public void setSrcFile(String srcFile) {
		this.srcFile = srcFile;
	}

	public String getOutFile() {
		return outFile;
	}

	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

	public Level getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * 
	 * @return
	 */
	private String reportCommandLineArguments() {
		StringBuilder sb = new StringBuilder();
		sb.append("Command Line Arguments:\n");
		sb.append(String.format("\t%-20s: %b\n", "stats", stats));
		sb.append(String.format("\t%-20s: %b\n", "keepPreviousDBTable",
				keepPreviousDBTable));
		sb.append(String.format("\t%-20s: %b\n", "error", error));

		sb.append(String.format("\t%-20s: %3d\n", "sortValue", sortValue));
		sb.append(String.format("\t%-20s: %3d\n", "gc", gc));
		sb.append(String.format("\t%-20s: %3d\n", "topStart", topStart));
		sb.append(String.format("\t%-20s: %3d\n", "topStop", topStop));
		sb.append(String.format("\t%-20s: %3d\n", "errorCode", errorCode));

		sb.append(String.format("\t%-20s: %s\n", "logFile", logFile));
		sb.append(String.format("\t%-20s: %s\n", "rsFile", rsFile));
		sb.append(String.format("\t%-20s: %s\n", "debugFile", debugFile));
		sb.append(String.format("\t%-20s: %s\n", "srcFile", srcFile));
		sb.append(String.format("\t%-20s: %s\n", "logLevel", logLevel));

		return sb.toString();
	}

	private String runFileParameters() {
		StringBuilder sb = new StringBuilder();
		sb.append("Derived from Run Script:\n");
		sb.append(String.format("\t%-20s: %s\n", "attributes",
				runScript.getAttributeLocation()));

		return sb.toString();
	}

	@Override
	public String toString() {
		return reportCommandLineArguments() + runFileParameters();
	}

	private static class OysterInitializationException extends RuntimeException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OysterInitializationException(Exception e) {
			super(e);
		}

		public OysterInitializationException(String s) {
			super(s);
		}

		public OysterInitializationException(String string, Exception e) {
			super(string, e);
		}
	}
}
